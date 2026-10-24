// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation
import CallKit
import SwiftUI

/// Wangiri (one-ring hangup) scam detector for iOS.
///
/// A wangiri call rings for well under 2 seconds and then disconnects before the owner
/// can answer. The scam relies on the owner curiously calling the number back; the
/// callback routes through a premium-rate international number and the owner pays a
/// hefty per-minute charge.
///
/// Detection uses `CXCallObserver`:
///   - `callChanged` fires with `hasConnected == false` + `hasEnded == true` for an
///     incoming call that was never answered.
///   - If the elapsed time between the first observation (ringing) and the end state
///     is `< wangiriThreshold`, we classify as wangiri.
///
/// iOS limitation: `CXCall` does not surface the actual phone number for privacy
/// reasons. We can **count** wangiri hangups, surface them in the UI, and log an event,
/// but we cannot auto-add the specific number to `BlockListStore` on iOS. The
/// Settings screen states this clearly. On Android we have `TelephonyManager`/`PhoneStateListener`
/// which does expose the number, so the Android path is a true auto-block.
@MainActor
final class WangiriWatcher: NSObject, ObservableObject {

    static let shared = WangiriWatcher()

    /// Any ring-to-end delta shorter than this counts as wangiri. 2 s is the agreed
    /// threshold with the Android implementation.
    private let wangiriThreshold: TimeInterval = 2.0

    private let observer = CXCallObserver()

    /// Tracks the first time we saw a given `CXCall.uuid` so we can diff on end.
    private var ringStartByCallID: [UUID: Date] = [:]

    @Published private(set) var detectedThisMonth: Int = 0
    @Published private(set) var recentBlocks: [WangiriBlockRecord] = []

    private let d = UserDefaults.standard
    private enum Keys {
        static let enabled = "wangiri.v1.enabled"
        static let monthlyCount = "wangiri.v1.monthly_count"
        static let monthlyKey = "wangiri.v1.monthly_key"
        static let recent = "wangiri.v1.recent_json"
    }

    /// Default ON. User can disable from Settings → Wangiri Protection.
    var isEnabled: Bool {
        get { d.object(forKey: Keys.enabled) as? Bool ?? true }
        set {
            d.set(newValue, forKey: Keys.enabled)
            if newValue { startIfNeeded() } else { stop() }
        }
    }

    private var isObserving = false

    private override init() {
        super.init()
        reloadMonthlyCount()
        reloadRecent()
    }

    func startIfNeeded() {
        guard isEnabled else { return }
        guard !isObserving else { return }
        observer.setDelegate(self, queue: nil)
        isObserving = true
    }

    func stop() {
        guard isObserving else { return }
        observer.setDelegate(nil, queue: nil)
        isObserving = false
        ringStartByCallID.removeAll()
    }

    // MARK: - Internal

    fileprivate func noteRinging(_ callUUID: UUID) {
        if ringStartByCallID[callUUID] == nil {
            ringStartByCallID[callUUID] = Date()
        }
    }

    fileprivate func noteEnded(_ callUUID: UUID, hasConnected: Bool) {
        guard let start = ringStartByCallID.removeValue(forKey: callUUID) else { return }
        // Only an incoming call that never connected counts.
        guard !hasConnected else { return }
        let elapsed = Date().timeIntervalSince(start)
        guard elapsed > 0 && elapsed < wangiriThreshold else { return }
        recordWangiri(elapsed: elapsed)
    }

    private func recordWangiri(elapsed: TimeInterval) {
        let record = WangiriBlockRecord(id: UUID(), at: Date(), elapsedSeconds: elapsed)
        recentBlocks.insert(record, at: 0)
        if recentBlocks.count > 20 { recentBlocks = Array(recentBlocks.prefix(20)) }
        persistRecent()
        incrementMonthlyCount()
        EventStore.shared.append(
            type: SecurityEventRecord.Kind.wangiriBlocked,
            detail: String(format: "wangiri hangup after %.2fs", elapsed)
        )
    }

    private func reloadMonthlyCount() {
        let currentKey = monthKey(for: Date())
        let savedKey = d.string(forKey: Keys.monthlyKey)
        if savedKey == currentKey {
            detectedThisMonth = d.integer(forKey: Keys.monthlyCount)
        } else {
            detectedThisMonth = 0
            d.set(currentKey, forKey: Keys.monthlyKey)
            d.set(0, forKey: Keys.monthlyCount)
        }
    }

    private func incrementMonthlyCount() {
        reloadMonthlyCount()
        detectedThisMonth += 1
        d.set(detectedThisMonth, forKey: Keys.monthlyCount)
    }

    private func monthKey(for date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM"
        return f.string(from: date)
    }

    private func reloadRecent() {
        guard
            let data = d.data(forKey: Keys.recent),
            let decoded = try? JSONDecoder().decode([WangiriBlockRecord].self, from: data)
        else {
            recentBlocks = []
            return
        }
        recentBlocks = decoded
    }

    private func persistRecent() {
        guard let data = try? JSONEncoder().encode(recentBlocks) else { return }
        d.set(data, forKey: Keys.recent)
    }
}

extension WangiriWatcher: CXCallObserverDelegate {
    nonisolated func callObserver(_ callObserver: CXCallObserver, callChanged call: CXCall) {
        let id = call.uuid
        let outgoing = call.isOutgoing
        let hasConnected = call.hasConnected
        let hasEnded = call.hasEnded
        Task { @MainActor [weak self] in
            guard let self else { return }
            if outgoing { return }
            if !hasEnded {
                // Ringing / connecting — remember the first time we saw this id.
                self.noteRinging(id)
            } else {
                self.noteEnded(id, hasConnected: hasConnected)
            }
        }
    }
}

struct WangiriBlockRecord: Codable, Identifiable, Hashable {
    let id: UUID
    let at: Date
    let elapsedSeconds: TimeInterval
}
