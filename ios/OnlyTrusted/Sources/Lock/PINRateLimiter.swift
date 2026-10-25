// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation
import Security

/// Rate limits PIN unlock attempts to defeat trivial brute-force.
///
/// The vault's AES-256-GCM master key is protected by the system Keychain's
/// `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` ACL, which means a locked
/// device won't release the key at all. But once past the OS lock, the OnlyTrusted
/// PIN sheet was previously unlimited — an attacker with the device unlocked and
/// our lock gate open could hammer PIN guesses with no slowdown.
///
/// Policy (keep in sync with Android `PINRateLimiter.kt`):
///   - 5 consecutive failures → 60 s lockout (first offense)
///   - 10 consecutive failures → 5 min lockout
///   - 15 consecutive failures → 15 min lockout
///   - 20+ consecutive failures → 60 min lockout, repeats
///   - Any successful unlock resets the counter to 0
///
/// Storage: Keychain so a force-stop + relaunch can't reset the counter.
/// `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` so the record survives
/// reboot but is unreadable while the OS is locked.
enum PINRateLimiter {

    private static let service = "com.onlytrusted.app"
    private static let accountFailures = "pin.rate.failures.v1"
    private static let accountLockedUntil = "pin.rate.locked_until.v1"

    /// Returns the remaining lockout window in seconds, or 0 if the user may try now.
    static func remainingLockout(now: Date = Date()) -> TimeInterval {
        guard let lockedUntilEpoch = loadDouble(account: accountLockedUntil) else { return 0 }
        let remaining = lockedUntilEpoch - now.timeIntervalSince1970
        return max(0, remaining)
    }

    /// Call on a wrong PIN. Returns the new lockout window (0 if under threshold).
    @discardableResult
    static func recordFailure(now: Date = Date()) -> TimeInterval {
        let failures = (loadInt(account: accountFailures) ?? 0) + 1
        store(int: failures, account: accountFailures)

        let penalty = penaltySeconds(for: failures)
        if penalty > 0 {
            let until = now.timeIntervalSince1970 + penalty
            store(double: until, account: accountLockedUntil)
        }
        return penalty
    }

    /// Call on a successful PIN entry. Resets failure counter + clears lockout.
    static func recordSuccess() {
        delete(account: accountFailures)
        delete(account: accountLockedUntil)
    }

    /// Visible for tests. Returns the lockout duration triggered by the Nth
    /// consecutive failure. Below the first threshold returns 0.
    static func penaltySeconds(for failures: Int) -> TimeInterval {
        switch failures {
        case ..<5: return 0
        case 5..<10: return 60              // 1 min
        case 10..<15: return 5 * 60         // 5 min
        case 15..<20: return 15 * 60        // 15 min
        default: return 60 * 60             // 60 min, repeats
        }
    }

    // Visible for tests. Wipes everything.
    static func reset() {
        delete(account: accountFailures)
        delete(account: accountLockedUntil)
    }

    // MARK: - Keychain backing

    private static func baseQuery(account: String) -> [CFString: Any] {
        [
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: service,
            kSecAttrAccount: account,
            kSecAttrAccessible: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]
    }

    private static func store(int value: Int, account: String) {
        store(data: String(value).data(using: .utf8) ?? Data(), account: account)
    }
    private static func store(double value: Double, account: String) {
        store(data: String(value).data(using: .utf8) ?? Data(), account: account)
    }
    private static func store(data: Data, account: String) {
        var add = baseQuery(account: account)
        add[kSecValueData] = data
        let status = SecItemAdd(add as CFDictionary, nil)
        if status == errSecDuplicateItem {
            let query = baseQuery(account: account)
            let attrs: [CFString: Any] = [kSecValueData: data]
            SecItemUpdate(query as CFDictionary, attrs as CFDictionary)
        }
    }

    private static func loadInt(account: String) -> Int? {
        loadString(account: account).flatMap(Int.init)
    }
    private static func loadDouble(account: String) -> Double? {
        loadString(account: account).flatMap(Double.init)
    }
    private static func loadString(account: String) -> String? {
        var query = baseQuery(account: account)
        query[kSecReturnData] = true
        query[kSecMatchLimit] = kSecMatchLimitOne
        var out: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &out) == errSecSuccess,
              let data = out as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private static func delete(account: String) {
        SecItemDelete(baseQuery(account: account) as CFDictionary)
    }
}
