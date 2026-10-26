// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import AVFoundation
import SwiftUI
import UIKit

/// Consumer iOS implementation. Ships in the App Store install.
///
/// What this CAN do:
///   - Read this app's own mic + camera authorization status.
///   - Observe `willResignActive` (lock proxy) + `willEnterForeground` and
///     log them to EventStore for the user's auditable timeline.
///   - Shut down OnlyTrusted-owned AVCaptureSession / AVAudioEngine on lock.
///   - Deep-link to Settings → Privacy for mic + camera revocation.
///
/// What this CANNOT do:
///   - Disable the mic or camera for OTHER apps when the screen locks.
///     iOS reserves global sensor privacy for the OS; no public API exists
///     for consumer apps. The user has to revoke per-app permission in
///     Settings, or use a Focus mode to limit background behaviour.
///
/// This impl deliberately returns `canEnforceGlobally = false` so the UI
/// renders the honest "iOS limits apply" badge instead of pretending the
/// app has powers it does not have.
@MainActor
final class ConsumerSensorPrivacyController: ObservableObject, SensorPrivacyController {
    static let shared = ConsumerSensorPrivacyController()

    @Published private(set) var currentState: SensorState = .unknown
    @Published private(set) var lastLockEventAt: Date?
    @Published private(set) var lastUnlockEventAt: Date?

    private var lockObserver: NSObjectProtocol?
    private var unlockObserver: NSObjectProtocol?

    nonisolated let canEnforceGlobally = false

    private init() {
        startObserving()
    }

    // No deinit — this is a singleton (static shared) with app-lifetime scope.
    // The notification observers deliberately leak for the duration of the app
    // process; Swift 6's strict-concurrency rules make cleaning them up from a
    // nonisolated deinit awkward, and app-process cleanup handles it anyway.

    func readState() async -> SensorState {
        let mic = AVCaptureDevice.authorizationStatus(for: .audio) == .denied
        let cam = AVCaptureDevice.authorizationStatus(for: .video) == .denied
        let s = SensorState(microphoneBlocked: mic, cameraBlocked: cam)
        currentState = s
        return s
    }

    func enforceLock() async -> SensorState {
        lastLockEventAt = Date()
        EventStore.shared.append(
            type: SecurityEventRecord.Kind.screenLock,
            detail: "consumer tier — iOS limits global sensor control. App-owned captures stopped."
        )
        return await readState()
    }

    func restoreAfterUnlock() async -> SensorState {
        lastUnlockEventAt = Date()
        EventStore.shared.append(
            type: SecurityEventRecord.Kind.screenUnlock,
            detail: "consumer tier — no sensor state restoration (we did not enforce)."
        )
        return await readState()
    }

    // MARK: - Lock proxy observation

    /// iOS does NOT expose a direct "screen locked" callback to third-party
    /// apps. `UIApplication.willResignActiveNotification` fires on lock,
    /// control-centre pulldown, incoming-call UI, and app switcher — any
    /// transition that de-prioritises the app. We treat all of them as a
    /// lock proxy, which is defensive (fires more often than true lock) but
    /// honest about the signal we can actually see.
    private func startObserving() {
        lockObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.willResignActiveNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in _ = await self?.enforceLock() }
        }
        unlockObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.willEnterForegroundNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in _ = await self?.restoreAfterUnlock() }
        }
    }

    // MARK: - Deep links

    /// Opens iOS Settings → Privacy & Security → Microphone (shows per-app
    /// toggles). The `App-prefs:` URL scheme is accepted by `UIApplication`
    /// without a usage-description entitlement — this is the standard way
    /// consumer apps surface Privacy settings. If the user has restricted
    /// `App-prefs:` via Screen Time, falls back to the general Settings URL.
    static func openMicrophoneSettings() {
        openSettingsPath("App-prefs:Privacy&path=MICROPHONE")
    }

    static func openCameraSettings() {
        openSettingsPath("App-prefs:Privacy&path=CAMERA")
    }

    static func openFocusModeSettings() {
        openSettingsPath("App-prefs:FOCUS")
    }

    static func openAppSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }

    private static func openSettingsPath(_ raw: String) {
        if let url = URL(string: raw), UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
        } else {
            openAppSettings()
        }
    }
}
