// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI

/// Settings → "Mic & Camera Lock". Honest UI that surfaces what the Consumer
/// tier CAN do (read + log + deep-link) and explicitly names what it CANNOT
/// do (globally disable sensors). See docs/SENSOR_PRIVACY_ARCHITECTURE.md.
struct SensorPrivacyScreen: View {
    @StateObject private var controller = ConsumerSensorPrivacyController.shared

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 20) {
                statusCard
                honestLimitsCard
                deepLinksCard
                recentEventsCard
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 20)
        }
        .background(Color.appScreenBackground.ignoresSafeArea())
        .navigationTitle("Mic & Camera Lock")
        .navigationBarTitleDisplayMode(.inline)
        .task { _ = await controller.readState() }
    }

    // MARK: - Status

    private var statusCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("CURRENT STATE")
                .font(.system(size: 11, weight: .heavy))
                .tracking(2)
                .foregroundStyle(Color.appAccent)
            HStack(spacing: 14) {
                sensorChip(name: "Microphone",
                           symbol: "mic.slash.fill",
                           blocked: controller.currentState.microphoneBlocked)
                sensorChip(name: "Camera",
                           symbol: "video.slash.fill",
                           blocked: controller.currentState.cameraBlocked)
            }
            Text("These show whether YOU have revoked mic/camera access for OnlyTrusted itself. iOS does not let us read or control what other apps are doing.")
                .font(.system(size: 13))
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.appAccent.opacity(0.2), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func sensorChip(name: String, symbol: String, blocked: Bool?) -> some View {
        let tint: Color = blocked == true ? .green : (blocked == false ? .orange : Color.appSecondaryText)
        let state: String = blocked == true ? "Blocked" : (blocked == false ? "Allowed" : "Unknown")
        return VStack(alignment: .leading, spacing: 6) {
            Image(systemName: symbol)
                .font(.system(size: 20))
                .foregroundStyle(tint)
            Text(name)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color.appPrimaryText)
            Text(state)
                .font(.system(size: 12))
                .foregroundStyle(tint)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.appScreenBackground.opacity(0.55))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    // MARK: - Honest limits

    private var honestLimitsCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("HONEST LIMITS")
                .font(.system(size: 11, weight: .heavy))
                .tracking(2)
                .foregroundStyle(Color.appAccent)
            Text("What this screen can do")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color.appPrimaryText)
            bullet("Shows OnlyTrusted's own mic + camera authorization.")
            bullet("Logs every screen-lock + unlock event to your Events log.")
            bullet("Shuts down OnlyTrusted's own captures when the screen locks.")
            bullet("Links you to the exact Settings page to revoke mic/camera access from any app.")

            Text("What this screen cannot do")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color.appPrimaryText)
                .padding(.top, 6)
            bullet("Prevent Instagram / WhatsApp / Zoom / any other app from using the mic or camera.")
            bullet("Flip a global \"everything off while locked\" switch — iOS does not expose that to any consumer app.")
            bullet("Change what the orange / green dot in your status bar means. Those are Apple's indicators, not ours.")
        }
        .padding(16)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.appSecondaryText.opacity(0.14), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func bullet(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Circle().fill(Color.appAccent).frame(width: 5, height: 5).padding(.top, 7)
            Text(text)
                .font(.system(size: 13))
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    // MARK: - Deep links

    private var deepLinksCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("DO IT YOURSELF")
                .font(.system(size: 11, weight: .heavy))
                .tracking(2)
                .foregroundStyle(Color.appAccent)
            deepLinkRow(symbol: "mic.fill", title: "Microphone — per-app toggles",
                        action: ConsumerSensorPrivacyController.openMicrophoneSettings)
            deepLinkRow(symbol: "video.fill", title: "Camera — per-app toggles",
                        action: ConsumerSensorPrivacyController.openCameraSettings)
            deepLinkRow(symbol: "moon.fill", title: "Focus — limit calls + notifications when sleeping",
                        action: ConsumerSensorPrivacyController.openFocusModeSettings)
        }
        .padding(16)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.appSecondaryText.opacity(0.14), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func deepLinkRow(symbol: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: symbol)
                    .font(.system(size: 15))
                    .foregroundStyle(Color.appAccent)
                    .frame(width: 24)
                Text(title)
                    .font(.system(size: 14))
                    .foregroundStyle(Color.appPrimaryText)
                Spacer()
                Image(systemName: "arrow.up.right.square")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appSecondaryText)
            }
        }
        .buttonStyle(.plain)
    }

    // MARK: - Recent events

    private var recentEventsCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("RECENT")
                .font(.system(size: 11, weight: .heavy))
                .tracking(2)
                .foregroundStyle(Color.appAccent)
            if let last = controller.lastLockEventAt {
                Text("Last screen-lock: \(formatted(last))")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appSecondaryText)
            }
            if let last = controller.lastUnlockEventAt {
                Text("Last unlock: \(formatted(last))")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appSecondaryText)
            }
            if controller.lastLockEventAt == nil && controller.lastUnlockEventAt == nil {
                Text("Lock + unlock events will appear here once the screen locks for the first time while OnlyTrusted is running.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appSecondaryText)
            }
        }
        .padding(16)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.appSecondaryText.opacity(0.14), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func formatted(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateStyle = .short
        f.timeStyle = .medium
        return f.string(from: date)
    }
}
