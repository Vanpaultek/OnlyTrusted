// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI
import UIKit

/// iPhone Sensor Privacy Center — the honest version of what the Mic & Camera
/// Lock story can look like inside a consumer App Store install.
///
/// iOS forbids any third-party app from reading or modifying other apps'
/// microphone / camera access. This screen explicitly does NOT claim to:
///   - enumerate every app on the phone with mic / camera access
///   - verify whether an app's permission is actually revoked
///   - block any other app's recording after the screen locks
///
/// What it DOES, honestly:
///   1. Shows OnlyTrusted's own mic + camera authorization.
///   2. Deep-links to iOS Settings → Privacy → Microphone / Camera where
///      the user can review every app's state (Apple's authoritative source).
///   3. Lets the user maintain a personal "Always Restricted" checklist —
///      a notepad of apps the user has deliberately denied, labelled
///      "self-reported" so no OS verification is implied.
///   4. Explains Apple's orange / green indicator dots so the user knows
///      what to look for.
///   5. Points at the iOS Shortcuts automation + the Apple Feedback form
///      for people who want to push for a native lock-screen mic/cam toggle.
///   6. Confirms OnlyTrusted's own captures (voice bio, face lock, QR
///      scanning) stop immediately when the screen locks.
///
/// See docs/SENSOR_PRIVACY_ARCHITECTURE.md § "Tier 1 Consumer (iOS)" for
/// the full honest-claims rationale.
struct SensorPrivacyScreen: View {
    @StateObject private var controller = ConsumerSensorPrivacyController.shared
    @StateObject private var restricted = AlwaysRestrictedAppsStore.shared
    @State private var newAppName: String = ""
    @State private var newEntryMic: Bool = true
    @State private var newEntryCamera: Bool = true

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 20) {
                heroCard
                statusCard
                permissionReviewCards
                restrictedChecklistCard
                indicatorEducationCard
                captureProtectionCard
                requestAppleCard
                honestLimitsCard
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 20)
        }
        .background(Color.appScreenBackground.ignoresSafeArea())
        .navigationTitle("Mic & Camera Lock")
        .navigationBarTitleDisplayMode(.inline)
        .task { _ = await controller.readState() }
    }

    // MARK: - Hero

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("IPHONE SENSOR PRIVACY CENTER")
                .font(.system(size: 11, weight: .heavy))
                .tracking(2)
                .foregroundStyle(Color.appAccent)
            Text("The honest version")
                .font(.system(size: 24, weight: .bold, design: .rounded))
                .foregroundStyle(Color.appPrimaryText)
            Text("iOS forbids any app from touching another app's mic or camera. OnlyTrusted does not pretend otherwise. What this screen does: help you lock other apps down yourself, remind you how iOS already warns you, and keep our own recordings honest.")
                .font(.system(size: 13))
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.appAccent.opacity(0.22), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    // MARK: - Status (OnlyTrusted's own permissions)

    private var statusCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("ONLYTRUSTED'S OWN PERMISSIONS")
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
            Text("Status refers only to what YOU have granted OnlyTrusted. iOS does not share other apps' permission state with us — see the next cards for how to review those yourself.")
                .font(.system(size: 13))
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.appSecondaryText.opacity(0.14), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func sensorChip(name: String, symbol: String, blocked: Bool?) -> some View {
        let tint: Color = blocked == true ? .green : (blocked == false ? .orange : Color.appSecondaryText)
        let state: String = blocked == true ? "Blocked" : (blocked == false ? "Allowed" : "Unknown")
        return VStack(alignment: .leading, spacing: 6) {
            Image(systemName: symbol)
                .font(.system(size: 20))
                .foregroundStyle(tint)
            Text(name).font(.system(size: 13, weight: .semibold)).foregroundStyle(Color.appPrimaryText)
            Text(state).font(.system(size: 12)).foregroundStyle(tint)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.appScreenBackground.opacity(0.55))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    // MARK: - Permission review (Apple-authoritative deep links)

    private var permissionReviewCards: some View {
        VStack(spacing: 12) {
            reviewCard(
                icon: "mic.fill",
                title: "Mic Permission Review",
                body: "Open iOS Settings → Privacy & Security → Microphone to see every app that has asked for microphone access and revoke anything you don't want listening.",
                buttonLabel: "Open Microphone Settings",
                action: ConsumerSensorPrivacyController.openMicrophoneSettings,
            )
            reviewCard(
                icon: "video.fill",
                title: "Camera Permission Review",
                body: "Open iOS Settings → Privacy & Security → Camera and audit the full list. Revoke anything that doesn't need video.",
                buttonLabel: "Open Camera Settings",
                action: ConsumerSensorPrivacyController.openCameraSettings,
            )
        }
    }

    private func reviewCard(icon: String, title: String, body: String,
                            buttonLabel: String, action: @escaping () -> Void) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.appAccent)
                Text(title).font(.system(size: 15, weight: .semibold)).foregroundStyle(Color.appPrimaryText)
            }
            Text(body).font(.system(size: 13)).foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
            Button(action: action) {
                HStack {
                    Text(buttonLabel).font(.system(size: 14, weight: .semibold))
                    Image(systemName: "arrow.up.right.square").font(.system(size: 13))
                }
                .foregroundStyle(Color.appAccent)
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Color.appSecondaryText.opacity(0.14), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Always-restricted checklist (self-reported)

    private var restrictedChecklistCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("ALWAYS RESTRICTED — YOUR CHECKLIST")
                .font(.system(size: 11, weight: .heavy))
                .tracking(2)
                .foregroundStyle(Color.appAccent)
            Text("A personal notepad of apps you've denied mic / camera access. This list is **self-reported** — OnlyTrusted does not verify iOS state. Use it as a memory aid for your own audits.")
                .font(.system(size: 13))
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)

            // Add row
            HStack(spacing: 8) {
                TextField("App name (e.g. Instagram)", text: $newAppName)
                    .textFieldStyle(.roundedBorder)
                    .font(.system(size: 14))
                Button {
                    restricted.add(name: newAppName,
                                   micRestricted: newEntryMic,
                                   cameraRestricted: newEntryCamera)
                    newAppName = ""
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 24))
                        .foregroundStyle(Color.appAccent)
                }
                .buttonStyle(.plain)
                .disabled(newAppName.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            HStack(spacing: 14) {
                Toggle(isOn: $newEntryMic) {
                    Label("Mic revoked", systemImage: "mic.slash")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.appSecondaryText)
                }
                .tint(Color.appAccent)
                .toggleStyle(.switch)
                .controlSize(.mini)
                Toggle(isOn: $newEntryCamera) {
                    Label("Cam revoked", systemImage: "video.slash")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.appSecondaryText)
                }
                .tint(Color.appAccent)
                .toggleStyle(.switch)
                .controlSize(.mini)
            }

            Divider().background(Color.appSecondaryText.opacity(0.14))

            if restricted.entries.isEmpty {
                Text("No apps added yet. Add the apps you deliberately denied mic / cam access so you can audit them later.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.appSecondaryText)
                    .padding(.vertical, 6)
            } else {
                ForEach(restricted.entries) { entry in
                    restrictedRow(entry)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.appSecondaryText.opacity(0.14), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func restrictedRow(_ entry: RestrictedAppEntry) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "app.badge.checkmark")
                .font(.system(size: 15))
                .foregroundStyle(Color.appAccent)
            VStack(alignment: .leading, spacing: 2) {
                Text(entry.displayName)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.appPrimaryText)
                HStack(spacing: 10) {
                    if entry.micRestricted {
                        Label("mic", systemImage: "mic.slash.fill")
                            .font(.system(size: 11))
                            .foregroundStyle(.green)
                    }
                    if entry.cameraRestricted {
                        Label("cam", systemImage: "video.slash.fill")
                            .font(.system(size: 11))
                            .foregroundStyle(.green)
                    }
                    if !entry.micRestricted && !entry.cameraRestricted {
                        Text("neither restricted")
                            .font(.system(size: 11))
                            .foregroundStyle(Color.appSecondaryText)
                    }
                }
            }
            Spacer()
            Button {
                restricted.remove(id: entry.id)
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 16))
                    .foregroundStyle(Color.appSecondaryText.opacity(0.6))
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 6)
    }

    // MARK: - Indicator education

    private var indicatorEducationCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("WATCH FOR APPLE'S INDICATORS")
                .font(.system(size: 11, weight: .heavy))
                .tracking(2)
                .foregroundStyle(Color.appAccent)
            indicatorRow(color: .orange, label: "Orange dot = mic in use")
            indicatorRow(color: .green, label: "Green dot = camera in use")
            Text("If you see these when you aren't using a known mic / camera app, swipe down for Control Center — iOS names the app at the top. These dots are Apple's authoritative signal, not OnlyTrusted's.")
                .font(.system(size: 13))
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.appSecondaryText.opacity(0.14), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func indicatorRow(color: Color, label: String) -> some View {
        HStack(spacing: 10) {
            Circle().fill(color).frame(width: 10, height: 10)
            Text(label).font(.system(size: 13)).foregroundStyle(Color.appPrimaryText)
        }
    }

    // MARK: - OnlyTrusted's own capture protection

    private var captureProtectionCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("ONLYTRUSTED'S OWN CAPTURES")
                .font(.system(size: 11, weight: .heavy))
                .tracking(2)
                .foregroundStyle(Color.appAccent)
            HStack(spacing: 10) {
                Image(systemName: "checkmark.shield.fill")
                    .font(.system(size: 18))
                    .foregroundStyle(.green)
                Text("Active")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.appPrimaryText)
            }
            Text("When OnlyTrusted is itself using the mic or camera (Voice Biometric enrollment, Face-Change Lock watcher, QR scanning), we immediately stop the recording the moment the screen locks or the app goes to the background.")
                .font(.system(size: 13))
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
            if let last = controller.lastLockEventAt {
                Text("Last screen-lock seen: \(formatted(last))")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.appSecondaryText)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.appSecondaryText.opacity(0.14), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    // MARK: - Request Apple

    private var requestAppleCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("ASK APPLE TO BUILD IT NATIVELY")
                .font(.system(size: 11, weight: .heavy))
                .tracking(2)
                .foregroundStyle(Color.appAccent)
            Text("The only way to get a true \"lock = all apps lose mic + camera\" behaviour on iPhone is for Apple to add it to iOS itself. Submit a Feedback request supporting the feature.")
                .font(.system(size: 13))
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
            Button {
                if let url = URL(string: "https://feedbackassistant.apple.com") {
                    UIApplication.shared.open(url)
                }
            } label: {
                HStack {
                    Text("Open Feedback Assistant").font(.system(size: 14, weight: .semibold))
                    Image(systemName: "arrow.up.right.square").font(.system(size: 13))
                }
                .foregroundStyle(Color.appAccent)
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Color.appSecondaryText.opacity(0.14), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Honest limits

    private var honestLimitsCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("HONEST LIMITS")
                .font(.system(size: 11, weight: .heavy))
                .tracking(2)
                .foregroundStyle(Color.appAccent)
            bullet("OnlyTrusted cannot read other apps' microphone or camera authorization status — iOS does not expose it.")
            bullet("OnlyTrusted cannot block another app's recording after the screen locks. No consumer App Store app can.")
            bullet("Microphone gain tricks, blank camera overlays, and silent audio sessions do NOT produce real system-wide protection — we deliberately avoid them.")
            bullet("The \"Always Restricted\" checklist is a personal notepad. OnlyTrusted does not verify what iOS actually enforces — only Apple's Settings → Privacy pages do.")
            bullet("MDM-enrolled (supervised) devices have stronger controls via Apple Configuration profiles. That's an enterprise path, not a consumer App Store feature.")
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appCardBackground)
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.appSecondaryText.opacity(0.14), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func bullet(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Circle().fill(Color.appAccent).frame(width: 5, height: 5).padding(.top, 7)
            Text(text).font(.system(size: 13)).foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func formatted(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateStyle = .short
        f.timeStyle = .medium
        return f.string(from: date)
    }
}
