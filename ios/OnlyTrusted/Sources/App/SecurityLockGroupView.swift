// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI

/// Reusable grouping of the three "lock-related" controls that affect what
/// happens when the phone (or app) goes to sleep:
///
///   1. Auto-lock after inactivity   — re-lock the OnlyTrusted app
///   2. Inactivity window            — how long before auto-lock fires
///                                     (conditional: visible only when toggle on)
///   3. Mic & Camera Lock            — mute mic + cut camera when the
///                                     device screen locks (independent of #1)
///
/// Used in two places so iOS and the user see identical UX:
///   - Settings screen, inline inside the security card
///   - Dashboard, inside a modal sheet triggered by the Security Lock card
///
/// All state is owned by the caller (AppGate / UserDefaults) so swapping
/// between the inline and sheet presentation is lossless.
struct SecurityLockGroupView: View {
    @Binding var appLockToggle: Bool
    @Binding var autoLockToggle: Bool
    @Binding var autoLockSeconds: Int
    let canUseAuth: Bool
    let onAppLockChanged: (Bool) -> Void
    let onAutoLockToggleChanged: (Bool) -> Void
    let onAutoLockSecondsPicker: () -> Void
    let onLockDenied: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // App Lock
            Toggle(isOn: $appLockToggle) {
                row(icon: "lock.shield.fill",
                    title: "App Lock",
                    subtitle: "Require biometric or passcode to open.")
            }
            .tint(Color.appAccent)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .onChange(of: appLockToggle) { _, now in
                if now && !canUseAuth {
                    appLockToggle = false
                    onLockDenied()
                    return
                }
                onAppLockChanged(now)
            }

            SettingsDivider()

            // Auto-lock after inactivity
            Toggle(isOn: $autoLockToggle) {
                row(icon: "clock.fill",
                    title: "Auto-lock after inactivity",
                    subtitle: autoLockToggle
                        ? "Re-lock after \(AutoLockPrefs.label(for: autoLockSeconds)) in the background."
                        : "Only lock when you choose Panic lock.")
            }
            .tint(Color.appAccent)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .onChange(of: autoLockToggle) { _, now in
                onAutoLockToggleChanged(now)
            }

            // Inactivity window — only visible while Auto-lock is ON.
            if autoLockToggle {
                SettingsDivider()
                Button(action: onAutoLockSecondsPicker) {
                    SettingsNavRow(icon: "timer", iconTint: Color.appAccent,
                                   label: "Inactivity window",
                                   value: AutoLockPrefs.label(for: autoLockSeconds))
                }
                .buttonStyle(.plain)
            }

            // Mic & Camera Lock — always visible, independent of the two above.
            SettingsDivider()
            NavigationLink { SensorPrivacyScreen() } label: {
                SettingsNavRow(icon: "mic.slash.fill", iconTint: Color.appAccent,
                               label: "Mic & Camera Lock",
                               value: "iOS limits apply")
            }
            .buttonStyle(.plain)
        }
    }

    private func row(icon: String, title: String, subtitle: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color.appAccent)
                .frame(width: 32, height: 32)
                .background(Color.appAccent.opacity(0.16))
                .clipShape(RoundedRectangle(cornerRadius: 8))
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.appPrimaryText)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(Color.appSecondaryText)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

/// Sheet wrapper for the Dashboard-triggered presentation. Takes a dismiss
/// handler so the dashboard can close it on CTA tap.
struct SecurityLockGroupSheet: View {
    @Binding var appLockToggle: Bool
    @Binding var autoLockToggle: Bool
    @Binding var autoLockSeconds: Int
    let canUseAuth: Bool
    let onAppLockChanged: (Bool) -> Void
    let onAutoLockToggleChanged: (Bool) -> Void
    let onAutoLockSecondsPicker: () -> Void
    let onLockDenied: () -> Void
    let onDone: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Set how OnlyTrusted locks itself and how the mic + camera behave when your phone's screen locks. All three work together — change any of them here or in Settings; they stay in sync.")
                        .font(.footnote)
                        .foregroundStyle(Color.appSecondaryText)
                        .padding(.horizontal, 18)

                    PremiumCard {
                        SecurityLockGroupView(
                            appLockToggle: $appLockToggle,
                            autoLockToggle: $autoLockToggle,
                            autoLockSeconds: $autoLockSeconds,
                            canUseAuth: canUseAuth,
                            onAppLockChanged: onAppLockChanged,
                            onAutoLockToggleChanged: onAutoLockToggleChanged,
                            onAutoLockSecondsPicker: onAutoLockSecondsPicker,
                            onLockDenied: onLockDenied,
                        )
                    }
                }
                .padding(.vertical, 20)
            }
            .background(Color.appScreenBackground.ignoresSafeArea())
            .navigationTitle("Security Lock")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done", action: onDone)
                        .tint(Color.appAccent)
                }
            }
        }
    }
}
