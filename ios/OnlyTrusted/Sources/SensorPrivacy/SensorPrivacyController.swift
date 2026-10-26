// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation

/// Snapshot of mic + camera authorization state as this app sees it.
///
/// `nil` for a sensor means "unknown / platform doesn't expose a reliable
/// read" — the UI should render that as "Unknown" rather than guessing.
/// See docs/SENSOR_PRIVACY_ARCHITECTURE.md for the full contract.
struct SensorState: Equatable {
    let microphoneBlocked: Bool?
    let cameraBlocked: Bool?

    static let unknown = SensorState(microphoneBlocked: nil, cameraBlocked: nil)
}

/// Shared contract across iOS implementations. The consumer impl ships in the
/// App Store install and is read-only + deep-link based. An MDM-managed
/// variant could implement a true `canEnforceGlobally = true` version in a
/// separate enterprise product — not in scope for this build.
protocol SensorPrivacyController {
    /// Returns the current authorization state for mic + camera as visible
    /// to this app. Global cross-app state is not exposed by iOS and must
    /// return `nil`.
    func readState() async -> SensorState

    /// Called on screen-lock proxy (`willResignActive`). Consumer impl shuts
    /// down OnlyTrusted-owned captures and logs the event. Does NOT touch
    /// other apps' permissions — iOS forbids that.
    func enforceLock() async -> SensorState

    /// Called on `willEnterForeground`. Consumer impl logs the unlock and
    /// restarts captures the user had running.
    func restoreAfterUnlock() async -> SensorState

    /// `false` for Consumer iOS. A hypothetical MDM-enrolled build could
    /// return `true`. The UI uses this flag to decide whether to render a
    /// "we actually enforce this" badge or an "iOS limits apply" honest-note.
    var canEnforceGlobally: Bool { get }
}
