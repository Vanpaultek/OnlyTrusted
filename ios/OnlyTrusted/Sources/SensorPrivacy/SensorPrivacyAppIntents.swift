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

/// "Shield apps on lock" — fires the Family-Controls shield on the user's
/// selected app list. Designed to be the action of a Shortcuts Automation
/// triggered by "When iPhone is Locked" so the shield auto-applies on lock.
///
/// Requires:
///   - `com.apple.developer.family-controls` entitlement (declared in
///     ios/Entitlements/OnlyTrusted.entitlements)
///   - User has granted Family Controls authorization in-app
///   - User has selected apps via FamilyActivityPicker in AppShieldSetup
///
/// Does nothing silently if any precondition is missing — safer than
/// crashing in the Shortcuts runtime.
@available(iOS 16.0, *)
struct ShieldAppsOnLockIntent: AppIntent {
    static let title: LocalizedStringResource = "Shield apps on lock"
    static let description = IntentDescription(
        "Blocks the apps you've picked in OnlyTrusted \u{2192} App Shield from opening. Pair with a Shortcuts Automation 'When iPhone is Locked' so the shield auto-applies when you lock the phone."
    )
    static let openAppWhenRun: Bool = false

    func perform() async throws -> some IntentResult & ProvidesDialog {
        let applied = await MainActor.run { AppShieldManager.applyShield() }
        let count = await MainActor.run { AppShieldState.shared.selection.applicationTokens.count }
        if applied {
            return .result(dialog: "Shield applied to \(count) app\(count == 1 ? "" : "s").")
        }
        // Honest, specific failure messages so Shortcuts shows the real reason.
        let authorized = await MainActor.run { AppShieldState.shared.authorization == .approved }
        if !authorized {
            return .result(dialog: "Could not apply shield: OnlyTrusted does not have Family Controls authorization. Open OnlyTrusted \u{2192} Mic & Camera Lock \u{2192} Shield apps on lock to grant it.")
        }
        if count == 0 {
            return .result(dialog: "Could not apply shield: no apps are selected. Open OnlyTrusted \u{2192} App Shield to pick which apps to shield.")
        }
        return .result(dialog: "Could not apply shield. This may be the iOS Simulator, or the Family Controls entitlement is not yet approved for this build.")
    }
}

/// Inverse of [ShieldAppsOnLockIntent] — removes the shield so the apps can
/// open again. Pair with Shortcuts "When iPhone is Unlocked" so it fires
/// automatically on unlock.
@available(iOS 16.0, *)
struct UnshieldAppsOnUnlockIntent: AppIntent {
    static let title: LocalizedStringResource = "Unshield apps on unlock"
    static let description = IntentDescription(
        "Removes OnlyTrusted's App Shield so the selected apps can open again. Pair with a Shortcuts Automation 'When iPhone is Unlocked'."
    )
    static let openAppWhenRun: Bool = false

    func perform() async throws -> some IntentResult & ProvidesDialog {
        await MainActor.run { AppShieldManager.clearShield() }
        return .result(dialog: "Shield removed. Your selected apps can open again.")
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
        AppShortcut(
            intent: ShieldAppsOnLockIntent(),
            phrases: [
                "Shield my apps with \(.applicationName)",
                "\(.applicationName) shield on",
            ],
            shortTitle: "Shield apps",
            systemImageName: "lock.app.dashed",
        )
        AppShortcut(
            intent: UnshieldAppsOnUnlockIntent(),
            phrases: [
                "Unshield my apps with \(.applicationName)",
                "\(.applicationName) shield off",
            ],
            shortTitle: "Unshield apps",
            systemImageName: "lock.open",
        )
    }
}
