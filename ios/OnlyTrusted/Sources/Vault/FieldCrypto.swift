// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation
import CryptoKit
import LocalAuthentication
import Security

/// Keychain-backed AES-GCM encryption for free-text fields (address, notes).
/// Equivalent of Android's FieldCrypto; uses Keychain to store a 256-bit symmetric key.
enum FieldCrypto {

    private static let service = "com.onlytrusted.app"
    private static let account = "field-key.v1"

    enum CryptoError: Error {
        case keychainStoreFailed(OSStatus)
        case keychainLoadFailed(OSStatus)
        case invalidCiphertext
    }

    static func encrypt(_ plaintext: String?) throws -> String? {
        guard let plaintext, !plaintext.isEmpty else { return nil }
        let key = try getOrCreateKey()
        let box = try AES.GCM.seal(Data(plaintext.utf8), using: key)
        guard let combined = box.combined else { throw CryptoError.invalidCiphertext }
        return combined.base64EncodedString()
    }

    static func decrypt(_ encoded: String?) throws -> String? {
        guard let encoded, !encoded.isEmpty else { return nil }
        guard let data = Data(base64Encoded: encoded) else { throw CryptoError.invalidCiphertext }
        let key = try getOrCreateKey()
        let box = try AES.GCM.SealedBox(combined: data)
        let plain = try AES.GCM.open(box, using: key)
        return String(data: plain, encoding: .utf8)
    }

    // MARK: - Key management

    private static let keyLock = NSLock()
    nonisolated(unsafe) private static var cached: SymmetricKey?

    /// Called by `AppLockGate` after a successful biometric unlock. Pre-caches the
    /// vault key via the Secure Enclave passkey path so later encrypt/decrypt calls
    /// don't re-prompt. Pass the same `LAContext` used for the lock-screen prompt to
    /// avoid a second biometric prompt within the same session.
    static func primeCache(from passkeyContext: LAContext) {
        guard BiometricPasskey.isEnrolled else { return }
        if let key = try? BiometricPasskey.unwrapVaultKey(context: passkeyContext) {
            keyLock.lock(); defer { keyLock.unlock() }
            cached = key
        }
    }

    /// Called on sign-out / app background to clear the in-memory copy. The wrapped
    /// blob stays on disk; next unlock re-primes.
    static func clearCache() {
        keyLock.lock(); defer { keyLock.unlock() }
        cached = nil
    }

    private static func getOrCreateKey() throws -> SymmetricKey {
        keyLock.lock()
        defer { keyLock.unlock() }
        if let cached { return cached }

        // Phase 2: if the Secure Enclave passkey is enrolled, that path is primed by
        // AppLockGate via primeCache() right after the lock-screen biometric prompt.
        // We don't attempt to unwrap here on-demand, because that would fire a second
        // biometric prompt from an unrelated code path (background sync, widget, etc.)
        // and surprise the user. Instead we fall through to the legacy Keychain copy,
        // which still works for the first release and keeps PIN-only mode safe.

        if let keyData = try loadKeyData() {
            let key = SymmetricKey(data: keyData)
            cached = key
            return key
        }

        let key = SymmetricKey(size: .bits256)
        try storeKeyData(key.withUnsafeBytes { Data($0) })
        cached = key
        return key
    }

    /// Called from AppLockGate once a session is authenticated and the user has
    /// biometric unlock enabled. Wraps the current vault key with the Secure Enclave
    /// passkey (idempotent — no-op if the wrap already exists). Keychain copy is
    /// retained so PIN-only unlocks still work; a future release can delete the
    /// legacy Keychain copy once dual-path coverage is proven in the wild.
    static func enrollPasskeyIfNeeded() {
        guard BiometricPasskey.isAvailable, !BiometricPasskey.isEnrolled else { return }
        do {
            let key = try getOrCreateKey()
            try BiometricPasskey.enrollIfNeeded(wrapping: key)
        } catch {
            // Enrollment failures leave the user on the legacy Keychain path — no
            // data loss, degraded security only. Logged for diagnostics.
            NSLog("[Passkey] enroll failed: \\(error)")
        }
    }

    private static func baseQuery() -> [CFString: Any] {
        [
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: service,
            kSecAttrAccount: account,
            kSecAttrAccessible: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]
    }

    private static func loadKeyData() throws -> Data? {
        var query = baseQuery()
        query[kSecReturnData] = true
        query[kSecMatchLimit] = kSecMatchLimitOne

        var out: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &out)
        switch status {
        case errSecSuccess:
            return out as? Data
        case errSecItemNotFound:
            return nil
        default:
            throw CryptoError.keychainLoadFailed(status)
        }
    }

    private static func storeKeyData(_ data: Data) throws {
        var query = baseQuery()
        query[kSecValueData] = data

        let status = SecItemAdd(query as CFDictionary, nil)
        if status != errSecSuccess && status != errSecDuplicateItem {
            throw CryptoError.keychainStoreFailed(status)
        }
    }
}
