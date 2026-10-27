// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import AppIntents
import SwiftUI

/// Exposes OnlyTrusted's sensor-privacy actions to the Shortcuts app so users
/// can wire them into their own Automations ("When iPhone is locked → run
/// OnlyTrusted Lock-Mic", or Siri "Hey Siri, lock my mic").
///
/// iOS has no public API for a third-party app to disable mic / camera for
/// OTHER apps — these intents therefore only touch OnlyTrusted's own captures
/// and the Events log. See docs/SENSOR_PRIVACY_ARCHITECTURE.md § "Tier 4 iOS
/// Managed Device" for why a true global cutoff is enterprise-only.

/// "Lock OnlyTrusted mic + log event" — triggers the consumer-tier
/// enforceLock() path. Appears in Shortcuts under OnlyTrusted → actions.
@available(iOS 16.0, *)
struct LockMicAppIntent: AppIntent {
    static let title: LocalizedStringResource = "Lock OnlyTrusted Mic"
    static let description = IntentDescription(
        "Shuts down any OnlyTrusted-owned captures and logs a lock event. iOS forbids consumer apps from touching other apps' mic / camera — pair with Focus mode for broader quiet-hours behaviour."
    )
    static let openAppWhenRun: Bool = false

    func perform() async throws -> some IntentResult {
        _ = await ConsumerSensorPrivacyController.shared.enforceLock()
        return .result()
    }
}

/// "Restore OnlyTrusted mic" — the inverse: runs restoreAfterUnlock().
@available(iOS 16.0, *)
struct RestoreMicAppIntent: AppIntent {
    static let title: LocalizedStringResource = "Restore OnlyTrusted Mic"
    static let description = IntentDescription(
        "Reverses the OnlyTrusted mic lock and logs the restore. Does not change other apps' mic / camera access."
    )
    static let openAppWhenRun: Bool = false

    func perform() async throws -> some IntentResult {
        _ = await ConsumerSensorPrivacyController.shared.restoreAfterUnlock()
        return .result()
    }
}

/// "Open Microphone Settings" — jumps straight into iOS Settings → Privacy
/// → Microphone where the user manages per-app mic access. Handy as a
/// Shortcut shortcut.
@available(iOS 16.0, *)
struct OpenMicrophoneSettingsAppIntent: AppIntent {
    static let title: LocalizedStringResource = "Open iOS Mic Settings"
    static let description = IntentDescription(
        "Opens iOS Settings → Privacy → Microphone so you can review which apps have mic access."
    )
    static let openAppWhenRun: Bool = true

    func perform() async throws -> some IntentResult {
        await MainActor.run { ConsumerSensorPrivacyController.openMicrophoneSettings() }
        return .result()
    }
}

/// "Open Camera Settings" — same pattern for camera.
@available(iOS 16.0, *)
struct OpenCameraSettingsAppIntent: AppIntent {
    static let title: LocalizedStringResource = "Open iOS Camera Settings"
    static let description = IntentDescription(
        "Opens iOS Settings → Privacy → Camera for per-app camera access review."
    )
    static let openAppWhenRun: Bool = true

    func perform() async throws -> some IntentResult {
        await MainActor.run { ConsumerSensorPrivacyController.openCameraSettings() }
        return .result()
    }
}

/// Groups the four intents under the OnlyTrusted umbrella in Shortcuts.
/// The user can drag any of these into their own Automation (e.g.
/// Shortcuts → Automation → Create → When iPhone is Locked → add
/// "Lock OnlyTrusted Mic").
@available(iOS 16.0, *)
struct OnlyTrustedAppShortcutsProvider: AppShortcutsProvider {
    @AppShortcutsBuilder static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: LockMicAppIntent(),
            phrases: [
                "Lock mic in \(.applicationName)",
                "\(.applicationName) mic off",
            ],
            shortTitle: "Lock Mic",
            systemImageName: "mic.slash.fill",
        )
        AppShortcut(
            intent: RestoreMicAppIntent(),
            phrases: [
                "Restore mic in \(.applicationName)",
                "\(.applicationName) mic on",
            ],
            shortTitle: "Restore Mic",
            systemImageName: "mic.fill",
        )
        AppShortcut(
            intent: OpenMicrophoneSettingsAppIntent(),
            phrases: ["Open mic settings via \(.applicationName)"],
            shortTitle: "iOS Mic Settings",
            systemImageName: "mic.circle",
        )
        AppShortcut(
            intent: OpenCameraSettingsAppIntent(),
            phrases: ["Open camera settings via \(.applicationName)"],
            shortTitle: "iOS Camera Settings",
            systemImageName: "camera.circle",
        )
    }
}
