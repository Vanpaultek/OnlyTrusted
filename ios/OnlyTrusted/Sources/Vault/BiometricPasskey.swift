// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation
import CryptoKit
import LocalAuthentication
import Security

/// Phase 2 of the authentication model: a hardware-bound EC256 keypair living in the
/// Secure Enclave, used to wrap the FieldCrypto AES-256-GCM key. The private key never
/// leaves the Secure Enclave; the OS kernel refuses to perform ECDH with it unless the
/// user authenticates with Face ID / Touch ID / device passcode in the same unlock
/// window. See `docs/AUTH_MODEL.md` for the full architecture rationale.
///
/// Wrap format for FieldCrypto's SymmetricKey:
///     [ephemeral public key (65 bytes, X9.63 uncompressed)]
///     [AES-GCM sealed box of the SymmetricKey.rawRepresentation]
///
/// Unwrap:
///   1. Decode the ephemeral public key.
///   2. Perform ECDH between the ephemeral public key and the Secure Enclave private
///      key (biometric prompt fires here).
///   3. HKDF-SHA256 the shared secret into a 32-byte AES key.
///   4. AES-GCM open the sealed box to recover the SymmetricKey bytes.
enum BiometricPasskey {

    private static let keyTag = "com.onlytrusted.app.passkey.v2"
    private static let wrappedBlobService = "com.onlytrusted.app"
    private static let wrappedBlobAccount = "field-key.wrapped.v2"
    private static let hkdfInfo = Data("onlytrusted-passkey-wrap-v2".utf8)

    enum PasskeyError: Error {
        case notAvailable               // No Secure Enclave on this device (sim without SEP)
        case keyGenerationFailed(Error?)
        case keyNotFound
        case keychainStoreFailed(OSStatus)
        case keychainLoadFailed(OSStatus)
        case decryptFailed(Error?)
        case invalidWrappedBlob
    }

    // MARK: - Availability

    /// Secure Enclave is present on all shipping iPhones with Face ID / Touch ID. iOS
    /// Simulator exposes a software Secure Enclave stub on arm64 Macs but not on x86.
    /// Call this before enrolling to decide whether to show the Passkey upgrade prompt.
    static var isAvailable: Bool {
        SecureEnclave.isAvailable
    }

    /// True once a Secure Enclave key exists for this app install. Checked via the
    /// Keychain query; the private key material itself never touches the Keychain.
    static var isEnrolled: Bool {
        let query: [CFString: Any] = [
            kSecClass: kSecClassKey,
            kSecAttrApplicationTag: Data(keyTag.utf8),
            kSecAttrKeyType: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef: true
        ]
        var out: CFTypeRef?
        return SecItemCopyMatching(query as CFDictionary, &out) == errSecSuccess
    }

    // MARK: - Enrollment (first-time setup after biometric is enabled)

    /// Generates the Secure Enclave keypair (if missing) and wraps the supplied vault
    /// key with it. Safe to call repeatedly — no-op if a wrapped blob already exists.
    /// Pass the existing FieldCrypto key as `wrapping` so the migration can replace
    /// the plain Keychain copy once the wrap is verified.
    static func enrollIfNeeded(wrapping vaultKey: SymmetricKey) throws {
        if try loadWrappedBlob() != nil { return }
        guard isAvailable else { throw PasskeyError.notAvailable }

        let seKey = try getOrCreateSecureEnclaveKey()
        guard let sePublic = SecKeyCopyPublicKey(seKey) else {
            throw PasskeyError.keyGenerationFailed(nil)
        }
        let wrapped = try wrap(vaultKey: vaultKey, with: sePublic)
        try storeWrappedBlob(wrapped)
    }

    /// Unwraps the vault key using the Secure Enclave keypair. Fires a biometric prompt
    /// via the supplied `LAContext`; passing a context that was already evaluated for
    /// `.deviceOwnerAuthentication` within the last 10 seconds avoids a double prompt.
    static func unwrapVaultKey(context: LAContext) throws -> SymmetricKey {
        guard let blob = try loadWrappedBlob() else { throw PasskeyError.keyNotFound }
        let seKey = try loadSecureEnclaveKey(context: context)
        return try unwrap(blob: blob, with: seKey)
    }

    /// Deletes the wrapped blob + Secure Enclave key. Used when the user disables
    /// biometric unlock from Settings — the vault falls back to the plain Keychain
    /// `field-key.v1` path.
    static func unenroll() throws {
        let deleteBlob: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: wrappedBlobService,
            kSecAttrAccount: wrappedBlobAccount
        ]
        SecItemDelete(deleteBlob as CFDictionary)

        let deleteKey: [CFString: Any] = [
            kSecClass: kSecClassKey,
            kSecAttrApplicationTag: Data(keyTag.utf8),
            kSecAttrKeyType: kSecAttrKeyTypeECSECPrimeRandom
        ]
        SecItemDelete(deleteKey as CFDictionary)
    }

    // MARK: - Secure Enclave keypair

    private static func getOrCreateSecureEnclaveKey() throws -> SecKey {
        if let existing = try? loadSecureEnclaveKey(context: nil) { return existing }

        // The private key is biometry-current-set: enrolling a new face or finger
        // invalidates it. That is intentional — it defeats attackers who register
        // their own biometric after physically acquiring the device.
        var accessError: Unmanaged<CFError>?
        guard let access = SecAccessControlCreateWithFlags(
            kCFAllocatorDefault,
            kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
            [.privateKeyUsage, .biometryCurrentSet],
            &accessError
        ) else {
            throw PasskeyError.keyGenerationFailed(accessError?.takeRetainedValue())
        }

        let attributes: [CFString: Any] = [
            kSecAttrKeyType: kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits: 256,
            kSecAttrTokenID: kSecAttrTokenIDSecureEnclave,
            kSecPrivateKeyAttrs: [
                kSecAttrIsPermanent: true,
                kSecAttrApplicationTag: Data(keyTag.utf8),
                kSecAttrAccessControl: access
            ] as CFDictionary
        ]

        var error: Unmanaged<CFError>?
        guard let privateKey = SecKeyCreateRandomKey(attributes as CFDictionary, &error) else {
            throw PasskeyError.keyGenerationFailed(error?.takeRetainedValue())
        }
        return privateKey
    }

    private static func loadSecureEnclaveKey(context: LAContext?) throws -> SecKey {
        var query: [CFString: Any] = [
            kSecClass: kSecClassKey,
            kSecAttrApplicationTag: Data(keyTag.utf8),
            kSecAttrKeyType: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef: true
        ]
        if let context {
            query[kSecUseAuthenticationContext] = context
        }

        var out: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &out)
        guard status == errSecSuccess, let ref = out else {
            throw PasskeyError.keychainLoadFailed(status)
        }
        return ref as! SecKey
    }

    // MARK: - Wrap / unwrap

    private static func wrap(vaultKey: SymmetricKey, with sePublicKey: SecKey) throws -> Data {
        // Fresh ephemeral keypair for one-shot ECDH. Private half is discarded; public
        // half is prepended to the ciphertext so unwrap can recompute the shared secret.
        let ephemeralPrivate = P256.KeyAgreement.PrivateKey()
        let ephemeralPublicRaw = ephemeralPrivate.publicKey.x963Representation

        // Convert ephemeralPrivate to a SecKey so SecKeyCopyKeyExchangeResult can do the
        // ECDH against the Secure Enclave public half.
        guard
            let sePubData = SecKeyCopyExternalRepresentation(sePublicKey, nil) as Data?,
            let sePubAsP256 = try? P256.KeyAgreement.PublicKey(x963Representation: sePubData)
        else { throw PasskeyError.keyGenerationFailed(nil) }

        let sharedSecret = try ephemeralPrivate.sharedSecretFromKeyAgreement(with: sePubAsP256)
        let wrappingKey = sharedSecret.hkdfDerivedSymmetricKey(
            using: SHA256.self, salt: Data(), sharedInfo: hkdfInfo, outputByteCount: 32
        )

        let vaultKeyData = vaultKey.withUnsafeBytes { Data($0) }
        let sealed = try AES.GCM.seal(vaultKeyData, using: wrappingKey)
        guard let combined = sealed.combined else { throw PasskeyError.decryptFailed(nil) }

        return ephemeralPublicRaw + combined
    }

    private static func unwrap(blob: Data, with sePrivateKey: SecKey) throws -> SymmetricKey {
        // X9.63 uncompressed P-256 public key is exactly 65 bytes (0x04 || X || Y).
        let pubKeyLength = 65
        guard blob.count > pubKeyLength else { throw PasskeyError.invalidWrappedBlob }
        let ephemeralPubRaw = blob.prefix(pubKeyLength)
        let sealed = blob.suffix(from: pubKeyLength)

        guard let ephemeralPubKey = try? makeSecKey(fromP256Raw: ephemeralPubRaw) else {
            throw PasskeyError.invalidWrappedBlob
        }

        // Perform ECDH on the Secure Enclave — this is where the biometric prompt fires.
        var error: Unmanaged<CFError>?
        let params: [CFString: Any] = [:]
        guard let sharedSecretData = SecKeyCopyKeyExchangeResult(
            sePrivateKey,
            .ecdhKeyExchangeStandard,
            ephemeralPubKey,
            params as CFDictionary,
            &error
        ) as Data? else {
            throw PasskeyError.decryptFailed(error?.takeRetainedValue())
        }

        // Recreate the HKDF the same way wrap() did.
        let wrappingKey = HKDF<SHA256>.deriveKey(
            inputKeyMaterial: SymmetricKey(data: sharedSecretData),
            salt: Data(),
            info: hkdfInfo,
            outputByteCount: 32
        )

        let box = try AES.GCM.SealedBox(combined: sealed)
        let vaultKeyData = try AES.GCM.open(box, using: wrappingKey)
        return SymmetricKey(data: vaultKeyData)
    }

    private static func makeSecKey(fromP256Raw raw: Data) throws -> SecKey {
        let attrs: [CFString: Any] = [
            kSecAttrKeyType: kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeyClass: kSecAttrKeyClassPublic,
            kSecAttrKeySizeInBits: 256
        ]
        var error: Unmanaged<CFError>?
        guard let key = SecKeyCreateWithData(raw as CFData, attrs as CFDictionary, &error) else {
            throw PasskeyError.keyGenerationFailed(error?.takeRetainedValue())
        }
        return key
    }

    // MARK: - Wrapped-blob Keychain storage

    private static func loadWrappedBlob() throws -> Data? {
        let query: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: wrappedBlobService,
            kSecAttrAccount: wrappedBlobAccount,
            kSecReturnData: true,
            kSecMatchLimit: kSecMatchLimitOne
        ]
        var out: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &out)
        switch status {
        case errSecSuccess: return out as? Data
        case errSecItemNotFound: return nil
        default: throw PasskeyError.keychainLoadFailed(status)
        }
    }

    private static func storeWrappedBlob(_ data: Data) throws {
        let add: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: wrappedBlobService,
            kSecAttrAccount: wrappedBlobAccount,
            kSecAttrAccessible: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData: data
        ]
        let status = SecItemAdd(add as CFDictionary, nil)
        if status == errSecDuplicateItem {
            let update: [CFString: Any] = [
                kSecClass: kSecClassGenericPassword,
                kSecAttrService: wrappedBlobService,
                kSecAttrAccount: wrappedBlobAccount
            ]
            let attrs: [CFString: Any] = [kSecValueData: data]
            let updateStatus = SecItemUpdate(update as CFDictionary, attrs as CFDictionary)
            if updateStatus != errSecSuccess {
                throw PasskeyError.keychainStoreFailed(updateStatus)
            }
        } else if status != errSecSuccess {
            throw PasskeyError.keychainStoreFailed(status)
        }
    }
}
