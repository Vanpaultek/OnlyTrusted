// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI
import UIKit

/// Settings screen for the Distress Signal feature — the shake-trigger toggle +
/// WhatsApp group link configuration + the on-screen walkthrough for creating the
/// group manually.
///
/// iOS version is slightly smaller than Android's because iOS doesn't support the
/// volume-down 4× trigger (iOS sandboxes volume keys outside the app's own view
/// hierarchy — see Distress/ShakeDetector.swift header for the full note).
struct DistressSettingsScreen: View {

    @Environment(\.dismiss) private var dismiss
    @State private var shakeEnabled: Bool = ShakeTriggerPrefs.isEnabled
    @State private var waGroupURL: String = WhatsAppDistressPrefs.groupInviteURL ?? ""
    @State private var waSaveError: String?
    @State private var waSaveOk: Bool = false

    var body: some View {
        ScrollView(.vertical) {
            VStack(alignment: .leading, spacing: 16) {
                introCard
                triggersCard
                whatsAppCard
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
        }
        .background(Color.appScreenBackground.ignoresSafeArea())
        .navigationTitle("Distress Signal")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Back") { dismiss() }
                    .tint(Color.appBrightCyan)
            }
        }
    }

    // MARK: - Cards

    private var introCard: some View {
        card {
            HStack {
                Image(systemName: "info.circle.fill")
                    .foregroundStyle(Color.appAccent)
                Text("How it works")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.appPrimaryText)
            }
            Text(
                "Fires an SMS with your current location to EVERY trusted contact at once, "
                + "opens the phone dialer with your first contact pre-dialed, and (if "
                + "WhatsApp is installed) opens WhatsApp with the same message pre-filled "
                + "so you can one-tap-send to your emergency group."
            )
            .font(.system(size: 12))
            .foregroundStyle(Color.appSecondaryText)
            Text(
                "OnlyTrusted sends zero traffic itself — everything flows through your own "
                + "carrier (SMS over GSM, voice over PSTN) and your own WhatsApp account."
            )
            .font(.system(size: 11))
            .foregroundStyle(Color.appSecondaryText)
        }
    }

    private var triggersCard: some View {
        card {
            Text("Triggers")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color.appPrimaryText)
            Toggle(isOn: Binding(
                get: { shakeEnabled },
                set: { shakeEnabled = $0; ShakeTriggerPrefs.setEnabled($0) }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Shake phone to trigger")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.appPrimaryText)
                    Text("5 shakes in 3 seconds. Foreground only — no battery cost when the app is closed.")
                        .font(.system(size: 11))
                        .foregroundStyle(Color.appSecondaryText)
                }
            }
            .tint(Color.appBrightCyan)
            Text(
                "iOS does not expose volume keys or power-button taps to third-party apps "
                + "outside their own view, so volume-down 4× is Android-only. Use the "
                + "SOS button on the Dashboard or shake the phone instead."
            )
            .font(.system(size: 11))
            .foregroundStyle(Color.appSecondaryText)
        }
    }

    private var whatsAppCard: some View {
        card {
            Text("WhatsApp emergency group")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color.appPrimaryText)
            Text(
                "WhatsApp blocks third-party apps from creating groups or sending to a specific "
                + "group chat. One-time manual setup instead:\n\n"
                + "1. Open WhatsApp. Tap New Chat (pencil icon) → New Group.\n"
                + "2. Add every emergency contact; name the group (e.g. \"OnlyTrusted SOS\").\n"
                + "3. Open the group → tap group name → Invite via link → Share link → copy.\n"
                + "4. Paste the link below.\n\n"
                + "When you fire SOS, the link is copied to your clipboard automatically and "
                + "WhatsApp opens with the distress text pre-filled — pick the group, tap "
                + "Send, then tap the voice-call icon for a group call."
            )
            .font(.system(size: 12))
            .foregroundStyle(Color.appSecondaryText)
            TextField("https://chat.whatsapp.com/…", text: $waGroupURL)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.URL)
                .autocapitalization(.none)
                .onChange(of: waGroupURL) { _, _ in waSaveError = nil; waSaveOk = false }
            if let err = waSaveError {
                Text(err)
                    .font(.system(size: 12))
                    .foregroundStyle(Color.appDanger)
            }
            if waSaveOk {
                Text("Saved.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.appAccent)
            }
            HStack(spacing: 10) {
                Button {
                    let ok = WhatsAppDistressPrefs.setGroupInviteURL(waGroupURL)
                    if ok { waSaveOk = true; waSaveError = nil }
                    else {
                        waSaveError = "Must start with https://chat.whatsapp.com/ or https://wa.me/."
                        waSaveOk = false
                    }
                } label: {
                    Text("Save")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(RoundedRectangle(cornerRadius: 10).fill(Color.appAccent))
                }
                .buttonStyle(.plain)
                Button {
                    if let url = URL(string: "whatsapp://") {
                        UIApplication.shared.open(url)
                    }
                } label: {
                    Text("Open WhatsApp")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.appBrightCyan)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color.appElevatedSurface))
    }
}
