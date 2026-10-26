// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation
import CryptoKit

/// A single audit-log row. The hash chain mirrors Android's SecurityRepository.log():
/// each row's `rowHash` = SHA-256(prevHash | timestamp | type | detail). A tamper-check
/// walks the chain and fails on the first break.
struct SecurityEventRecord: Codable, Identifiable {
    let id: UUID
    let timestampMillis: Int64
    let type: String
    let detail: String?
    let prevHash: String?
    let rowHash: String

    enum Kind {
        static let callBlocked = "CALL_BLOCKED"
        static let callAllowed = "CALL_ALLOWED"
        static let smsFlagged = "SMS_FLAGGED"
        static let callDirectoryReloaded = "CALL_DIRECTORY_RELOADED"
        static let panicLock = "PANIC_LOCK"
        static let vaultEdit = "VAULT_EDIT"
        static let wipe = "WIPE"
        /// Fired when the SIM-Swap Alarm detects a different carrier identity than the
        /// baseline captured on Pro activation. Pro-tier.
        static let simSwap = "SIM_SWAP"
        /// Fired by the Geo-fence Auto-Lock (Elite) when the device exits every trusted
        /// region. Triggers an auto-lock and a prepared SMS to the chosen contact.
        static let geofenceExit = "GEOFENCE_EXIT"
        /// Fired by SensorPrivacyController on screen-lock proxy (iOS: willResignActive;
        /// Android: ACTION_SCREEN_OFF). Detail carries the pre-lock SensorState snapshot.
        /// See docs/SENSOR_PRIVACY_ARCHITECTURE.md.
        static let screenLock = "SCREEN_LOCK"
        static let screenUnlock = "SCREEN_UNLOCK"
        /// Fired when SensorPrivacyController successfully changes a sensor's state.
        /// Only ever fires on tiers that `canEnforceGlobally` (Android Device Owner +
        /// Privileged OS build). Consumer tiers never fire this.
        static let sensorPrivacyEnforced = "SENSOR_PRIVACY_ENFORCED"
        static let sensorPrivacyRestored = "SENSOR_PRIVACY_RESTORED"
        /// Fired when the controller attempted to change a sensor state but the OS
        /// refused (missing permission). Auditable record that enforcement was NOT
        /// applied — important for incident review.
        static let sensorPrivacyRefused = "SENSOR_PRIVACY_REFUSED"
    }
}

/// Append-only event store, backed by the App Group container.
///
/// Keep this file small: cap at 10k rows (matches Android retention). Writes are
/// serialised via a dedicated queue so concurrent appends from the main app and
/// extensions don't corrupt the file.
final class EventStore: @unchecked Sendable {
    static let shared = EventStore()

    private let queue = DispatchQueue(label: "com.onlytrusted.app.events", qos: .utility)
    private let filename = "events.v1.json"
    private let maxRows = 10_000
    private let trimEveryNWrites = 50
    private var writesSinceTrim = 0

    private var fileURL: URL? { try? SharedContainer.fileURL(named: filename) }

    func allRecent(limit: Int = 500) -> [SecurityEventRecord] {
        queue.sync {
            Array(loadRaw().suffix(limit).reversed())
        }
    }

    func append(type: String, detail: String? = nil) {
        queue.async { [weak self] in
            guard let self else { return }
            var rows = self.loadRaw()
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            let prev = rows.last?.rowHash
            let hash = self.hash(prev: prev, timestamp: now, type: type, detail: detail)
            let record = SecurityEventRecord(
                id: UUID(), timestampMillis: now, type: type, detail: detail,
                prevHash: prev, rowHash: hash
            )
            rows.append(record)
            self.writesSinceTrim += 1
            if self.writesSinceTrim >= self.trimEveryNWrites, rows.count > self.maxRows {
                rows = Array(rows.suffix(self.maxRows))
                self.writesSinceTrim = 0
            }
            try? self.persist(rows)
        }
    }

    func wipe() {
        queue.sync { try? persist([]) }
    }

    func verifyChain() -> Bool {
        queue.sync {
            let rows = loadRaw()
            var prev: String? = nil
            for row in rows {
                if row.prevHash != prev { return false }
                let expected = hash(prev: prev, timestamp: row.timestampMillis,
                                    type: row.type, detail: row.detail)
                if row.rowHash != expected { return false }
                prev = row.rowHash
            }
            return true
        }
    }

    // MARK: - Internals

    private func loadRaw() -> [SecurityEventRecord] {
        guard let fileURL,
              let data = try? Data(contentsOf: fileURL),
              let rows = try? JSONDecoder.events.decode([SecurityEventRecord].self, from: data)
        else { return [] }
        return rows
    }

    private func persist(_ rows: [SecurityEventRecord]) throws {
        guard let fileURL else { throw SharedContainer.ContainerError.missingAppGroup }
        let data = try JSONEncoder.events.encode(rows)
        try data.write(to: fileURL, options: [.atomic, .completeFileProtectionUnlessOpen])
    }

    private func hash(prev: String?, timestamp: Int64, type: String, detail: String?) -> String {
        let material = "\(prev ?? "")|\(timestamp)|\(type)|\(detail ?? "")"
        let digest = SHA256.hash(data: Data(material.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}

private extension JSONEncoder {
    static let events: JSONEncoder = {
        let e = JSONEncoder()
        e.outputFormatting = [.sortedKeys]
        return e
    }()
}

private extension JSONDecoder {
    static let events: JSONDecoder = {
        JSONDecoder()
    }()
}
