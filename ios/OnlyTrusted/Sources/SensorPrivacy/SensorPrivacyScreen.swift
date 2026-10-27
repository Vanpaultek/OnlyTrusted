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
