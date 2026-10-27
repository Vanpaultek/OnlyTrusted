// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI
import UIKit

/// iPhone Sensor Privacy Center — honest permission-review hub.
///
/// Rewritten for density: fewer cards on first paint, denser information
/// grouped together, education + legal moved behind a "Learn more"
/// disclosure. Core actions (Review iOS Settings, Always Restricted
/// checklist) are above the fold.
///
/// See docs/SENSOR_PRIVACY_ARCHITECTURE.md § "Tier 1 Consumer (iOS)".
struct SensorPrivacyScreen: View {
    @StateObject private var controller = ConsumerSensorPrivacyController.shared
    @StateObject private var restricted = AlwaysRestrictedAppsStore.shared
    @State private var showAddSheet = false
    @State private var showLearnMore = false

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 16) {
                statusCard
                reviewCard
                restrictedCard
                learnMoreDisclosure
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 20)
        }
        .background(Color.appScreenBackground.ignoresSafeArea())
        .navigationTitle("Mic & Camera Lock")
        .navigationBarTitleDisplayMode(.inline)
        .task { _ = await controller.readState() }
        .sheet(isPresented: $showAddSheet) {
            AddRestrictedAppSheet { name, mic, cam in
                restricted.add(name: name, micRestricted: mic, cameraRestricted: cam)
            }
        }
    }

    // MARK: - Status

    private var statusCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("ONLYTRUSTED'S OWN PERMISSIONS")
                .eyebrow()
            HStack(spacing: 10) {
                sensorChip(name: "Microphone", symbol: "mic.fill",
                           blocked: controller.currentState.microphoneBlocked)
                sensorChip(name: "Camera", symbol: "video.fill",
                           blocked: controller.currentState.cameraBlocked)
            }
            Text("iOS does not reveal other apps' permission state to third-party apps.")
                .cardBodyMuted()
        }
        .padding(14)
        .card()
    }

    private func sensorChip(name: String, symbol: String, blocked: Bool?) -> some View {
        let tint: Color = blocked == true ? .green : (blocked == false ? .orange : Color.appSecondaryText)
        let state: String = blocked == true ? "Blocked" : (blocked == false ? "Allowed" : "Unknown")
        return VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                Image(systemName: symbol).font(.system(size: 13)).foregroundStyle(tint)
                Text(name).font(.system(size: 12, weight: .semibold)).foregroundStyle(Color.appPrimaryText)
            }
            Text(state).font(.system(size: 11, weight: .semibold)).foregroundStyle(tint)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(Color.appScreenBackground.opacity(0.55))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    // MARK: - Review (merged mic + camera)

    private var reviewCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("REVIEW IN IOS SETTINGS")
                .eyebrow()
            Text("iOS owns the per-app list. Tap below to open it directly.")
                .cardBodyMuted()
            VStack(spacing: 8) {
                reviewRow(symbol: "mic.fill", label: "Microphone — all apps",
                          action: ConsumerSensorPrivacyController.openMicrophoneSettings)
                reviewRow(symbol: "video.fill", label: "Camera — all apps",
                          action: ConsumerSensorPrivacyController.openCameraSettings)
            }
        }
        .padding(14)
        .card()
    }

    private func reviewRow(symbol: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: symbol)
                    .font(.system(size: 14))
                    .foregroundStyle(Color.appAccent)
                    .frame(width: 24)
                Text(label).font(.system(size: 14)).foregroundStyle(Color.appPrimaryText)
                Spacer()
                Image(systemName: "arrow.up.right.square")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appAccent)
            }
            .padding(10)
            .background(Color.appScreenBackground.opacity(0.5))
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Always Restricted (compact)

    private var restrictedCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("ALWAYS RESTRICTED").eyebrow()
                Spacer()
                Button { showAddSheet = true } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "plus")
                        Text("Add")
                    }
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Color.appAccent)
                }
                .buttonStyle(.plain)
            }
            Text("Your personal checklist — self-reported, not OS-verified.")
                .cardBodyMuted()
            if restricted.entries.isEmpty {
                Text("No apps yet. Tap Add to log an app you've denied mic / cam access.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.appSecondaryText)
                    .padding(.vertical, 6)
            } else {
                VStack(spacing: 6) {
                    ForEach(restricted.entries) { entry in
                        restrictedRow(entry)
                    }
                }
            }
        }
        .padding(14)
        .card()
    }

    private func restrictedRow(_ entry: RestrictedAppEntry) -> some View {
        HStack(spacing: 10) {
            Text(entry.displayName)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color.appPrimaryText)
            Spacer()
            if entry.micRestricted {
                Image(systemName: "mic.slash.fill").font(.system(size: 12)).foregroundStyle(.green)
            }
            if entry.cameraRestricted {
                Image(systemName: "video.slash.fill").font(.system(size: 12)).foregroundStyle(.green)
            }
            Button { restricted.remove(id: entry.id) } label: {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.appSecondaryText.opacity(0.6))
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 4)
        .padding(.horizontal, 8)
        .background(Color.appScreenBackground.opacity(0.4))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - Learn more disclosure

    private var learnMoreDisclosure: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) { showLearnMore.toggle() }
            } label: {
                HStack {
                    Text("Learn more about iOS limits")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color.appAccent)
                    Spacer()
                    Image(systemName: showLearnMore ? "chevron.up" : "chevron.down")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(Color.appAccent)
                }
            }
            .buttonStyle(.plain)

            if showLearnMore {
                VStack(spacing: 12) {
                    indicatorMiniCard
                    captureProtectionMiniCard
                    requestAppleMiniCard
                    honestLimitsMiniCard
                }
            }
        }
        .padding(14)
        .card()
    }

    private var indicatorMiniCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Apple's indicator dots").miniHeading()
            HStack(spacing: 10) {
                Circle().fill(.orange).frame(width: 8, height: 8)
                Text("Orange = mic in use").font(.system(size: 12)).foregroundStyle(Color.appPrimaryText)
                Spacer()
                Circle().fill(.green).frame(width: 8, height: 8)
                Text("Green = camera in use").font(.system(size: 12)).foregroundStyle(Color.appPrimaryText)
            }
            Text("Swipe down for Control Center — iOS names the app at the top.")
                .cardBodyMuted()
        }
        .padding(12)
        .miniCard()
    }

    private var captureProtectionMiniCard: some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.shield.fill")
                .font(.system(size: 16)).foregroundStyle(.green)
            VStack(alignment: .leading, spacing: 2) {
                Text("Our own captures: Active").miniHeading()
                Text("Voice bio, face lock, QR scanning all stop on screen lock.")
                    .cardBodyMuted()
            }
        }
        .padding(12)
        .miniCard()
    }

    private var requestAppleMiniCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Ask Apple to build it natively").miniHeading()
            Text("A true \"lock = all apps lose mic/cam\" needs iOS itself. Submit feedback.")
                .cardBodyMuted()
            Button {
                if let url = URL(string: "https://feedbackassistant.apple.com") {
                    UIApplication.shared.open(url)
                }
            } label: {
                HStack(spacing: 4) {
                    Text("Open Feedback Assistant").font(.system(size: 12, weight: .semibold))
                    Image(systemName: "arrow.up.right.square").font(.system(size: 11))
                }
                .foregroundStyle(Color.appAccent)
            }
            .buttonStyle(.plain)
        }
        .padding(12)
        .miniCard()
    }

    private var honestLimitsMiniCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Honest limits").miniHeading()
            miniBullet("Can't read other apps' mic/cam permissions.")
            miniBullet("Can't stop other apps' recordings when the phone locks.")
            miniBullet("No gain tricks, overlays, or private APIs — we deliberately avoid them.")
            miniBullet("MDM-supervised devices have stronger controls (enterprise only).")
        }
        .padding(12)
        .miniCard()
    }

    private func miniBullet(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Circle().fill(Color.appAccent).frame(width: 4, height: 4).padding(.top, 6)
            Text(text).font(.system(size: 12)).foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

// MARK: - Add-restricted sheet

private struct AddRestrictedAppSheet: View {
    let onAdd: (String, Bool, Bool) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name: String = ""
    @State private var mic: Bool = true
    @State private var cam: Bool = true

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("App name (e.g. Instagram)", text: $name)
                    Toggle(isOn: $mic) {
                        Label("Mic revoked", systemImage: "mic.slash")
                    }
                    Toggle(isOn: $cam) {
                        Label("Camera revoked", systemImage: "video.slash")
                    }
                } footer: {
                    Text("Tracked locally only. OnlyTrusted does not verify what iOS actually enforces for this app — review in iOS Settings.")
                }
            }
            .navigationTitle("Add restricted app")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onAdd(name, mic, cam)
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }
}

// MARK: - Styling helpers

private extension Text {
    func eyebrow() -> some View {
        self.font(.system(size: 10, weight: .heavy))
            .tracking(1.8)
            .foregroundStyle(Color.appAccent)
    }
    func cardBodyMuted() -> some View {
        self.font(.system(size: 12))
            .foregroundStyle(Color.appSecondaryText)
            .fixedSize(horizontal: false, vertical: true)
    }
    func miniHeading() -> some View {
        self.font(.system(size: 13, weight: .semibold))
            .foregroundStyle(Color.appPrimaryText)
    }
}

private extension View {
    func card() -> some View {
        self.frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.appCardBackground)
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.appSecondaryText.opacity(0.14), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 14))
    }
    func miniCard() -> some View {
        self.frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.appScreenBackground.opacity(0.4))
            .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}
