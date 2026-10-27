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
    @StateObject private var appShield = AppShieldState.shared
    @State private var newAppName: String = ""
    @State private var newEntryMic: Bool = true
    @State private var newEntryCam: Bool = true
    @State private var showLearnMore = false

    // Post-return prompt — fires when the user comes back to OnlyTrusted
    // after tapping a "Review in iOS Settings" button. See
    // docs/SENSOR_PRIVACY_ARCHITECTURE.md § "Tier 1 Consumer (iOS)".
    @State private var pendingReturnSensor: ReturnSensor?
    @State private var pendingReturnAt: Date?
    @State private var showPostReturnPrompt = false
    @State private var showBrowseSheet = false

    fileprivate enum ReturnSensor { case mic, cam }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 16) {
                statusCard
                shieldOnLockCard
                restrictedCard
                learnMoreDisclosure
                reviewCard
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 20)
        }
        .background(Color.appScreenBackground.ignoresSafeArea())
        .navigationTitle("Mic & Camera Lock")
        .navigationBarTitleDisplayMode(.inline)
        .task { _ = await controller.readState() }
        .onReceive(NotificationCenter.default.publisher(
            for: UIApplication.willEnterForegroundNotification)
        ) { _ in
            maybeShowPostReturnPrompt()
        }
        .sheet(isPresented: $showPostReturnPrompt) {
            PostReturnPromptSheet(
                sensor: pendingReturnSensor ?? .mic,
                existingLower: Set(restricted.entries.map { $0.displayName.lowercased() }),
                onPick: { name in
                    restricted.add(name: name,
                                   micRestricted: pendingReturnSensor == .mic,
                                   cameraRestricted: pendingReturnSensor == .cam)
                },
            )
        }
        .sheet(isPresented: $showBrowseSheet) {
            SuggestedAppsBrowseSheet(
                allApps: SuggestedApps.commonMicCamApps,
                restricted: restricted,
                defaultMic: newEntryMic,
                defaultCam: newEntryCam,
            )
        }
    }

    private func maybeShowPostReturnPrompt() {
        guard pendingReturnSensor != nil,
              let openedAt = pendingReturnAt,
              Date().timeIntervalSince(openedAt) < 5 * 60
        else {
            pendingReturnSensor = nil
            pendingReturnAt = nil
            return
        }
        showPostReturnPrompt = true
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
                reviewRow(symbol: "mic.fill", label: "Microphone — all apps") {
                    pendingReturnSensor = .mic
                    pendingReturnAt = Date()
                    ConsumerSensorPrivacyController.openMicrophoneSettings()
                }
                reviewRow(symbol: "video.fill", label: "Camera — all apps") {
                    pendingReturnSensor = .cam
                    pendingReturnAt = Date()
                    ConsumerSensorPrivacyController.openCameraSettings()
                }
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

    // MARK: - Shield on lock (FamilyControls + Shortcuts combo)

    /// The closest iOS lets us get to Android's auto-mute-on-lock: shield
    /// (= block from opening) the user-picked apps using the Family Controls
    /// framework, triggered by a user-created Shortcuts Automation.
    ///
    /// Opens the existing AppShieldSetupScreen for the app picker + authorization,
    /// then provides a step-by-step guide to wire up the "When iPhone is Locked"
    /// Automation that calls our ShieldAppsOnLockIntent.
    private var shieldOnLockCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("SHIELD APPS ON LOCK").eyebrow()
                Spacer()
                Text("Pro").miniTierChip()
            }
            Text("The closest iOS allows to Android's auto-mute-on-lock. Shielded apps are blocked from opening (and from using mic/cam) while the shield is active.")
                .cardBodyMuted()

            // Live status row — shows authorization + selection + shield state so
            // the user knows exactly where they are in the setup.
            shieldStatusRow

            NavigationLink { AppShieldSetupScreen() } label: {
                HStack(spacing: 10) {
                    Image(systemName: "lock.app.dashed")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.appAccent)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Pick apps + grant Family Controls")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color.appPrimaryText)
                        Text("Opens App Shield setup — authorize + choose the apps to shield.")
                            .font(.system(size: 11))
                            .foregroundStyle(Color.appSecondaryText)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color.appSecondaryText)
                }
                .padding(10)
                .background(Color.appScreenBackground.opacity(0.5))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
            .buttonStyle(.plain)

            // Shortcuts automation guide.
            VStack(alignment: .leading, spacing: 6) {
                Text("STEP 2 — SHORTCUTS AUTOMATION")
                    .font(.system(size: 9, weight: .heavy))
                    .tracking(1.2)
                    .foregroundStyle(Color.appSecondaryText)
                shortcutStep(num: "1", text: "Open iOS Shortcuts app → Automation tab")
                shortcutStep(num: "2", text: "+ → Create Personal Automation → When iPhone is Locked → Next")
                shortcutStep(num: "3", text: "Add Action → search \"OnlyTrusted\" → tap Shield apps")
                shortcutStep(num: "4", text: "Toggle OFF \"Ask Before Running\" → Done")
                shortcutStep(num: "5", text: "Create a second Automation: When iPhone is Unlocked → OnlyTrusted → Unshield apps")
                Button {
                    if let url = URL(string: "shortcuts://create-workflow") {
                        UIApplication.shared.open(url)
                    }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.up.right.square").font(.system(size: 11))
                        Text("Open Shortcuts app").font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundStyle(Color.appAccent)
                }
                .buttonStyle(.plain)
                .padding(.top, 4)
            }
            .padding(10)
            .background(Color.appScreenBackground.opacity(0.4))
            .clipShape(RoundedRectangle(cornerRadius: 10))

            legalDisclosureBanner
        }
        .padding(14)
        .card()
    }

    /// Live status row: shows auth state + how many apps selected + whether
    /// the shield is currently active. Updates reactively via AppShieldState.
    private var shieldStatusRow: some View {
        let authLabel: String
        let authTint: Color
        switch appShield.authorization {
        case .approved:      authLabel = "Family Controls authorized"; authTint = .green
        case .notDetermined: authLabel = "Family Controls not requested yet"; authTint = Color.appSecondaryText
        case .denied:        authLabel = "Family Controls denied"; authTint = .orange
        }
        return VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Circle().fill(authTint).frame(width: 6, height: 6)
                Text(authLabel)
                    .font(.system(size: 11))
                    .foregroundStyle(Color.appPrimaryText)
            }
            HStack(spacing: 6) {
                Image(systemName: appShield.shieldedCount > 0 ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 10))
                    .foregroundStyle(appShield.shieldedCount > 0 ? .green : Color.appSecondaryText)
                Text("\(appShield.shieldedCount) app\(appShield.shieldedCount == 1 ? "" : "s") selected to shield")
                    .font(.system(size: 11))
                    .foregroundStyle(Color.appPrimaryText)
            }
            HStack(spacing: 6) {
                Image(systemName: appShield.enabled ? "lock.fill" : "lock.open")
                    .font(.system(size: 10))
                    .foregroundStyle(appShield.enabled ? .green : Color.appSecondaryText)
                Text(appShield.enabled ? "Shield currently ACTIVE (apps blocked)" : "Shield currently OFF (apps unblocked)")
                    .font(.system(size: 11))
                    .foregroundStyle(Color.appPrimaryText)
            }
        }
        .padding(10)
        .background(Color.appScreenBackground.opacity(0.4))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    /// Legal / transparency banner — the "what Family Controls actually
    /// means" copy that Apple's App Review team expects to see visible to
    /// users (not buried in a Privacy Policy PDF).
    private var legalDisclosureBanner: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("WHAT FAMILY CONTROLS DOES")
                .font(.system(size: 9, weight: .heavy))
                .tracking(1.2)
                .foregroundStyle(Color.subGold)
            miniBullet("OnlyTrusted asks iOS to shield apps YOU pick. We never see which apps you picked — Apple gives us opaque tokens only.")
            miniBullet("Shielded apps are blocked from opening entirely while the shield is active. They cannot deliver notifications, mic/cam access, or any other activity.")
            miniBullet("Family Controls authorization also exposes aggregate Screen Time data to OnlyTrusted. OnlyTrusted does NOT read, store, or transmit it — code path proof is on GitHub (AGPL-3.0).")
            miniBullet("Requires Apple to approve the \"com.apple.developer.family-controls\" entitlement on our developer team. If it isn't approved yet, this card shows the setup UI but applying the shield silently fails.")
            miniBullet("Revoke anytime: iOS Settings \u{2192} Screen Time \u{2192} See All App & Website Activity \u{2192} App Limits (or just remove OnlyTrusted's Family Controls authorization).")
        }
        .padding(10)
        .background(Color.subGold.opacity(0.08))
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.subGold.opacity(0.3), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func shortcutStep(num: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Text(num)
                .font(.system(size: 10, weight: .bold, design: .monospaced))
                .foregroundStyle(Color.appAccent)
                .frame(width: 14, height: 14)
                .background(Circle().fill(Color.appAccent.opacity(0.18)))
            Text(text)
                .font(.system(size: 11))
                .foregroundStyle(Color.appPrimaryText)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    // MARK: - Always Restricted (compact)

    private var restrictedCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("ALWAYS RESTRICTED — YOUR CHECKLIST").eyebrow()
            Text("Your personal notepad — self-reported, not OS-verified.")
                .cardBodyMuted()

            // Inline add form.
            HStack(spacing: 8) {
                TextField("App name (e.g. Instagram)", text: $newAppName)
                    .textFieldStyle(.roundedBorder)
                    .font(.system(size: 13))
                Button {
                    restricted.add(name: newAppName,
                                   micRestricted: newEntryMic,
                                   cameraRestricted: newEntryCam)
                    newAppName = ""
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 24))
                        .foregroundStyle(newAppName.trimmingCharacters(in: .whitespaces).isEmpty
                                         ? Color.appSecondaryText.opacity(0.4)
                                         : Color.appAccent)
                }
                .buttonStyle(.plain)
                .disabled(newAppName.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            HStack(spacing: 14) {
                Toggle(isOn: $newEntryMic) {
                    HStack(spacing: 4) {
                        Image(systemName: "mic.slash").font(.system(size: 11))
                        Text("Mic revoked").font(.system(size: 11))
                    }
                    .foregroundStyle(Color.appSecondaryText)
                }
                .tint(Color.appAccent)
                .toggleStyle(.switch)
                .controlSize(.mini)
                Toggle(isOn: $newEntryCam) {
                    HStack(spacing: 4) {
                        Image(systemName: "video.slash").font(.system(size: 11))
                        Text("Cam revoked").font(.system(size: 11))
                    }
                    .foregroundStyle(Color.appSecondaryText)
                }
                .tint(Color.appAccent)
                .toggleStyle(.switch)
                .controlSize(.mini)
            }

            // Browse suggestions — opens a searchable sheet. iOS does not let
            // OnlyTrusted enumerate what's actually installed (Apple blocks
            // app enumeration), so this is a curated list of ~50 common
            // mic/cam apps.
            Button {
                showBrowseSheet = true
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.appAccent)
                    Text("Browse suggested apps")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color.appPrimaryText)
                    Spacer()
                    let picked = restricted.entries.filter { entry in
                        SuggestedApps.commonMicCamApps.contains {
                            $0.caseInsensitiveCompare(entry.displayName) == .orderedSame
                        }
                    }.count
                    Text("\(picked) / \(SuggestedApps.commonMicCamApps.count)")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color.appAccent)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color.appSecondaryText)
                }
                .padding(10)
                .background(Color.appScreenBackground.opacity(0.5))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
            .buttonStyle(.plain)

            // Currently restricted apps — compact combined view (suggested + custom).
            if restricted.entries.isEmpty {
                Text("No apps yet. Tap Browse above or type a name.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.appSecondaryText)
                    .padding(.vertical, 4)
            } else {
                Divider().background(Color.appSecondaryText.opacity(0.14))
                Text("Your checklist (\(restricted.entries.count))")
                    .font(.system(size: 10, weight: .semibold))
                    .tracking(1.2)
                    .foregroundStyle(Color.appSecondaryText)
                VStack(spacing: 2) {
                    ForEach(restricted.entries) { entry in
                        customEntryRow(entry)
                    }
                }
            }
        }
        .padding(14)
        .card()
    }

    /// Row for a checklist entry (suggested or custom). Always has an X
    /// remove button since the sheet-based browse flow handles add/toggle.
    private func customEntryRow(_ entry: RestrictedAppEntry) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 16))
                .foregroundStyle(Color.appAccent)
            Text(entry.displayName)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color.appPrimaryText)
            Spacer()
            if entry.micRestricted {
                Image(systemName: "mic.slash.fill").font(.system(size: 11)).foregroundStyle(.green)
            }
            if entry.cameraRestricted {
                Image(systemName: "video.slash.fill").font(.system(size: 11)).foregroundStyle(.green)
            }
            Button { restricted.remove(id: entry.id) } label: {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.appSecondaryText.opacity(0.6))
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 6)
        .padding(.horizontal, 8)
        .background(Color.appAccent.opacity(0.1))
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

// MARK: - Post-return prompt

/// Shown as a sheet after the user comes back to OnlyTrusted from an
/// iOS Settings deep link (within 5 min). Tap-to-add common apps that
/// were likely the subject of the Settings visit. Pre-filters out
/// anything already in the checklist. Honest: we can't verify what the
/// user did in iOS Settings — this is prompted-recall, not detection.
private struct PostReturnPromptSheet: View {
    let sensor: SensorPrivacyScreen.ReturnSensor
    let existingLower: Set<String>
    let onPick: (String) -> Void

    @Environment(\.dismiss) private var dismiss

    private var sensorWord: String { sensor == .mic ? "mic" : "camera" }
    private var sensorIcon: String { sensor == .mic ? "mic.fill" : "video.fill" }

    private var candidates: [String] {
        SuggestedApps.commonMicCamApps.filter { !existingLower.contains($0.lowercased()) }
    }

    var body: some View {
        NavigationStack {
            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 16) {
                    header
                    chipGrid
                    footerNote
                }
                .padding(18)
            }
            .background(Color.appScreenBackground.ignoresSafeArea())
            .navigationTitle("Welcome back")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: sensorIcon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.appAccent)
                Text("Just revoked \(sensorWord) access?")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(Color.appPrimaryText)
            }
            Text("Tap any apps you turned off in iOS Settings — they'll be added to your Always Restricted checklist so you can audit later. iOS doesn't tell us what you changed, so this is self-reported.")
                .font(.system(size: 13))
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var chipGrid: some View {
        let columns = Array(repeating: GridItem(.flexible(), spacing: 8), count: 2)
        return LazyVGrid(columns: columns, spacing: 8) {
            if candidates.isEmpty {
                Text("Everything in the suggested list is already in your checklist. Add custom apps via the main screen.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appSecondaryText)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, 10)
            } else {
                ForEach(candidates, id: \.self) { name in
                    Button {
                        onPick(name)
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "plus.circle.fill")
                                .font(.system(size: 14))
                                .foregroundStyle(Color.appAccent)
                            Text(name)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(Color.appPrimaryText)
                                .lineLimit(1)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 9)
                        .background(Color.appCardBackground)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color.appAccent.opacity(0.3), lineWidth: 0.5)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var footerNote: some View {
        Text("The chips you tap are added with \(sensorWord) marked as revoked. Edit the mic/cam flags later from the Always Restricted card.")
            .font(.system(size: 11))
            .foregroundStyle(Color.appSecondaryText.opacity(0.8))
            .fixedSize(horizontal: false, vertical: true)
    }
}

// MARK: - Browse suggestions sheet

/// Searchable, scrollable sheet with the full curated suggestion list.
/// Checkbox rows sync bidirectionally with the restricted store — tap
/// adds, tap again removes. Select All / Deselect All work on the
/// visible (filtered) subset so searching + bulk-toggle work together.
private struct SuggestedAppsBrowseSheet: View {
    let allApps: [String]
    @ObservedObject var restricted: AlwaysRestrictedAppsStore
    let defaultMic: Bool
    let defaultCam: Bool

    @Environment(\.dismiss) private var dismiss
    @State private var query: String = ""

    private var filtered: [String] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        guard !q.isEmpty else { return allApps }
        return allApps.filter { $0.lowercased().contains(q) }
    }

    private var addedLower: Set<String> {
        Set(restricted.entries.map { $0.displayName.lowercased() })
    }

    private var allVisibleSelected: Bool {
        filtered.allSatisfy { addedLower.contains($0.lowercased()) }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                headerBar
                List {
                    ForEach(filtered, id: \.self) { name in
                        row(name)
                            .listRowBackground(Color.appCardBackground)
                            .listRowSeparatorTint(Color.appSecondaryText.opacity(0.14))
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .background(Color.appScreenBackground.ignoresSafeArea())
            }
            .background(Color.appScreenBackground.ignoresSafeArea())
            .navigationTitle("Suggested apps")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $query, prompt: "Search \(allApps.count) suggested apps")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button(allVisibleSelected ? "Deselect all" : "Select all") {
                        toggleVisible()
                    }
                    .font(.system(size: 13))
                }
            }
        }
    }

    private var headerBar: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Tap any app to add it to your Always Restricted checklist. Tap again to remove.")
                .font(.system(size: 12))
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
            Text("iOS does not reveal what's actually installed. This is an editorial list, not a scan.")
                .font(.system(size: 11))
                .foregroundStyle(Color.appSecondaryText.opacity(0.7))
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 18)
        .padding(.vertical, 12)
        .background(Color.appScreenBackground)
    }

    private func row(_ name: String) -> some View {
        let entryID = restricted.entries.first {
            $0.displayName.caseInsensitiveCompare(name) == .orderedSame
        }?.id
        let selected = entryID != nil
        return Button {
            if let id = entryID {
                restricted.remove(id: id)
            } else {
                restricted.add(name: name,
                               micRestricted: defaultMic,
                               cameraRestricted: defaultCam)
            }
        } label: {
            HStack(spacing: 12) {
                Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 18))
                    .foregroundStyle(selected ? Color.appAccent : Color.appSecondaryText)
                Text(name)
                    .font(.system(size: 15, weight: selected ? .semibold : .regular))
                    .foregroundStyle(Color.appPrimaryText)
                Spacer()
                if selected, let e = restricted.entries.first(where: { $0.id == entryID }) {
                    if e.micRestricted {
                        Image(systemName: "mic.slash.fill").font(.system(size: 12)).foregroundStyle(.green)
                    }
                    if e.cameraRestricted {
                        Image(systemName: "video.slash.fill").font(.system(size: 12)).foregroundStyle(.green)
                    }
                }
            }
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func toggleVisible() {
        if allVisibleSelected {
            // Remove every currently-visible-selected entry from the store.
            let visibleLower = Set(filtered.map { $0.lowercased() })
            restricted.entries
                .filter { visibleLower.contains($0.displayName.lowercased()) }
                .forEach { restricted.remove(id: $0.id) }
        } else {
            // Add any visible app that isn't already in the store.
            filtered
                .filter { !addedLower.contains($0.lowercased()) }
                .forEach {
                    restricted.add(name: $0,
                                   micRestricted: defaultMic,
                                   cameraRestricted: defaultCam)
                }
        }
    }
}

// MARK: - Suggested apps catalog

/// Curated list of apps that commonly request microphone or camera access.
///
/// NOT a list of what the user has installed — iOS does not expose that to
/// third-party apps. This list is editorial. Users browse + check-off from
/// here as a reminder of what they've revoked in iOS Settings.
private enum SuggestedApps {
    /// Curated list of apps that commonly request microphone or camera access.
    /// Grouped roughly by category then alphabetical within. Not a scan of
    /// what's installed — iOS forbids app enumeration for third-party apps.
    static let commonMicCamApps: [String] = [
        // Messaging + calls
        "WhatsApp", "Signal", "Telegram", "Messenger", "iMessage",
        "Discord", "Slack", "Skype", "WeChat", "Line", "KakaoTalk", "Viber",
        // Video conferencing
        "Zoom", "Google Meet", "Microsoft Teams", "Webex", "GoTo Meeting",
        "BlueJeans", "FaceTime",
        // Social + photo
        "Instagram", "Facebook", "TikTok", "Snapchat", "YouTube",
        "Twitter / X", "Threads", "BeReal", "Pinterest", "Reddit",
        // Dating
        "Tinder", "Bumble", "Hinge", "OkCupid", "Grindr",
        // Streaming + voice
        "Clubhouse", "Twitch", "Spotify", "YouTube Music", "SoundCloud",
        // Dictation + voice assistants
        "Google", "Google Assistant", "Alexa", "ChatGPT", "Perplexity",
        // Ride-sharing + delivery (camera for QR/profile)
        "Uber", "Lyft", "DoorDash", "Instacart",
        // Banking + ID (camera for check deposit / KYC)
        "Chase", "Bank of America", "Venmo", "PayPal", "Cash App",
    ]
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
    func miniTierChip() -> some View {
        self.font(.system(size: 9, weight: .heavy))
            .foregroundStyle(Color.subGold)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Capsule().stroke(Color.subGold, lineWidth: 1))
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
