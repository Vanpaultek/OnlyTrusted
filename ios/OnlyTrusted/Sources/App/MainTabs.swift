// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI

struct MainTabs: View {
    @EnvironmentObject private var gate: AppGate
    @State private var selectedTab: Tab = .home

    enum Tab: Hashable { case home, contacts, blocked, log, settings }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            TabView(selection: $selectedTab) {
                NavigationStack {
                    DashboardScreen(onNavigate: { selectedTab = $0 })
                }
                .tabItem { Label("Home", systemImage: "house.fill") }
                .tag(Tab.home)

                NavigationStack { VaultScreen() }
                    .tabItem { Label("Contacts", systemImage: "person.2.fill") }
                    .tag(Tab.contacts)

                NavigationStack { BlockListScreen() }
                    .tabItem { Label("Blocked", systemImage: "nosign") }
                    .tag(Tab.blocked)

                NavigationStack { EventsScreen() }
                    .tabItem { Label("Log", systemImage: "doc.text.fill") }
                    .tag(Tab.log)

                NavigationStack { SettingsScreen() }
                    .tabItem { Label("Settings", systemImage: "gearshape.fill") }
                    .tag(Tab.settings)
            }
            .tint(Color.appAccent)
            .toolbarBackground(Color.appScreenBackground, for: .tabBar)
            .toolbarBackground(.visible, for: .tabBar)

            // F1 — Panic quick-dial. Floating red circular SOS button, shown only on Home
            // so it doesn't collide with destructive actions deeper in the app. Sits above
            // the tab bar (lift by 100pt to clear the bar + bottom inset).
            if selectedTab == .home {
                PanicSOSButton(onNavigateToContacts: { selectedTab = .contacts })
                    .padding(.trailing, 18)
                    .padding(.bottom, 100)
            }
        }
        .onAppear {
            // Pro feature: Wangiri Protection auto-starts if the subscription is active and
            // the user hasn't disabled it. The watcher is a no-op when disabled.
            if SubscriptionState.shared.hasHighPremium {
                WangiriWatcher.shared.startIfNeeded()
            }
        }
    }
}

// MARK: - F1 Panic SOS button

/// Floating circular red button on Home. One tap triggers a confirmation sheet; the
/// user then taps "Call now" to open `tel:` for the first trusted contact and (if
/// available) a pre-filled Messages composer with the panic text. Pure deep links,
/// no network.
private struct PanicSOSButton: View {
    let onNavigateToContacts: () -> Void

    // Distress Signal state. Replaces the old single-contact confirmation dialog with
    // a 3-second cancel countdown + multi-recipient broadcast; see the Distress/ folder
    // for the full flow doc.
    @State private var showCountdown = false
    @State private var showNoContactAlert = false
    @State private var showSMSComposer = false
    @State private var smsBody: String = ""
    @State private var smsRecipients: [String] = []
    @State private var postFireMessage: String?
    @State private var shakeDetector = ShakeDetector()

    var body: some View {
        Button(action: { tapped() }) {
            ZStack {
                Circle()
                    .fill(Color.appDanger)
                    .frame(width: 64, height: 64)
                    .shadow(color: Color.appDanger.opacity(0.6), radius: 14, x: 0, y: 0)
                VStack(spacing: 0) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.white)
                    Text("SOS")
                        .font(.system(size: 11, weight: .heavy, design: .rounded))
                        .foregroundStyle(.white)
                        .tracking(0.5)
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Emergency distress signal")
        .accessibilityAddTraits(.isButton)
        .fullScreenCover(isPresented: $showCountdown) {
            DistressCountdownOverlay(
                onFire: {
                    showCountdown = false
                    Task { await dispatchDistress() }
                },
                onCancel: { showCountdown = false }
            )
        }
        .alert("dash_sos_add_contact_title", isPresented: $showNoContactAlert) {
            Button("dash_open_contacts") { onNavigateToContacts() }
            Button("lock_cancel", role: .cancel) {}
        } message: {
            Text("dash_sos_add_contact_body")
        }
        .alert("EMERGENCY alert", isPresented: Binding(
            get: { postFireMessage != nil },
            set: { if !$0 { postFireMessage = nil } }
        )) {
            Button("OK") { postFireMessage = nil }
        } message: {
            Text(postFireMessage ?? "")
        }
        .sheet(isPresented: $showSMSComposer) {
            MessageComposerView(
                recipients: smsRecipients,
                body: smsBody,
                onFinish: { _ in showSMSComposer = false }
            )
            .ignoresSafeArea()
        }
        // Shake-to-trigger: start CMMotionManager when the button is onscreen (always
        // visible on the Dashboard), stop on leave. Fires the same countdown overlay
        // as a button tap. Honours the per-user toggle in ShakeTriggerPrefs.
        .onAppear {
            guard ShakeTriggerPrefs.isEnabled else { return }
            shakeDetector.start { showCountdown = true }
        }
        .onDisappear { shakeDetector.stop() }
    }

    private func tapped() {
        guard PanicContact.firstTrustedContactNumber() != nil else {
            showNoContactAlert = true
            return
        }
        showCountdown = true
    }

    /// Executes the distress flow: location fetch → message build → multi-recipient
    /// composer + auto-dial first contact.
    private func dispatchDistress() async {
        let recipients = allTrustedRecipients()
        guard !recipients.isEmpty else {
            postFireMessage = "No emergency contacts configured."
            return
        }

        // 1. Best-effort one-shot location fetch (3-second timeout).
        let location = await DistressLocationFetcher.shared.fetchOnce()

        // 2. Pre-filled distress body (location-enriched, timestamped).
        let ownerName = ProfileStore.shared.email.components(separatedBy: "@").first ?? ""
        let body = DistressMessageBuilder.build(ownerName: ownerName, location: location)

        // 3. Open the Phone app with the first contact's number pre-dialed — happens
        //    first so the Phone app comes forward, then the SMS composer slides on top
        //    when the user dismisses it after sending.
        if let first = recipients.first,
           let url = URL(string: "tel:\(first.phoneNumber)") {
            await UIApplication.shared.open(url)
        }

        // 4. Open the system Messages composer with every recipient + body pre-filled.
        //    iOS has no silent-send API — the user must tap Send once, which broadcasts
        //    to all pre-selected recipients at once.
        smsRecipients = recipients.map(\.phoneNumber)
        smsBody = body
        var baseMessage: String
        if MessageComposerView.canSendText {
            // Brief delay so the Phone app renders first; keeps the sequence legible.
            try? await Task.sleep(nanoseconds: 600_000_000)
            showSMSComposer = true
            baseMessage = "Distress alert ready. Tap Send in Messages to broadcast to \(recipients.count) contact(s)"
        } else {
            baseMessage = "Device cannot send SMS. The Phone app is dialing \(recipients.first?.displayName ?? "your first contact")"
        }

        // 5. WhatsApp fan-out (optional): if WhatsApp is installed, open it with the
        //    distress text pre-filled so the owner can one-tap-send to their saved
        //    emergency group. Group invite link (if saved) is also copied to the
        //    clipboard — paste into WhatsApp's search bar to open the group directly.
        let whatsAppLaunched = await WhatsAppDistressSender.fire(body: body)
        if whatsAppLaunched {
            baseMessage += ", then tap Send in WhatsApp."
        } else {
            baseMessage += "."
        }
        postFireMessage = baseMessage
    }

    private func allTrustedRecipients() -> [DistressRecipient] {
        VaultStore.shared.contacts.compactMap { c in
            c.normalisedNumbers.first.map { num in
                DistressRecipient(phoneNumber: num, displayName: c.displayName)
            }
        }
    }
}

// MARK: - Dashboard (Home tab)

struct DashboardScreen: View {
    let onNavigate: (MainTabs.Tab) -> Void

    @EnvironmentObject private var gate: AppGate
    @StateObject private var vault = VaultStore.shared
    @StateObject private var duress = DuressState.shared
    @State private var blockedToday: Int = 0
    @State private var blockedWeek: Int = 0
    // Cached ContactScanState.matchCount() value — the underlying call does a Keychain
    // read, so we snapshot it once in `.task` instead of re-reading on every body
    // re-evaluation (which happened on every state change — trustedCount, banners, etc.).
    @State private var scamContactsCount: Int = 0
    @State private var showHonestLimitsSheet = false
    @State private var confirmDuressExit = false

    // F4 weekly digest + F5 honest-limits refresher banner state.
    @State private var showWeeklyDigest = false
    @State private var weeklyDigestCounts = WeeklyDigestCounts(callsBlocked: 0, textsBlocked: 0)
    @State private var showHonestLimitsRefresher = false
    @State private var showReports = false

    private var protectionActive: Bool { !vault.contacts.isEmpty }

    // Mockup target shows "12 / 8 / 42" populated numbers. These fall back to zero when
    // there is no live data, so the empty-state still renders cleanly.
    private var trustedCount: Int { vault.contacts.count }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 18) {
                CinematicHeader(
                    name: nil,
                    onSettings: { onNavigate(.settings) }
                )
                // F4 weekly digest banner (Monday first open, once per week).
                if showWeeklyDigest {
                    WeeklyDigestBanner(
                        callCount: weeklyDigestCounts.callsBlocked,
                        textCount: weeklyDigestCounts.textsBlocked,
                        onDismiss: dismissWeeklyDigest,
                        onSeeReport: {
                            dismissWeeklyDigest()
                            showReports = true
                        }
                    )
                }
                // F5 honest-limits monthly refresher.
                if showHonestLimitsRefresher {
                    HonestLimitsRefresherBanner(
                        onLearnMore: {
                            dismissHonestLimitsRefresher()
                            showHonestLimitsSheet = true
                        },
                        onDismiss: dismissHonestLimitsRefresher
                    )
                }
                // Triple-tap the shield card to panic-exit a duress session. Deliberately
                // hidden: no label, no visible affordance — the gesture only matters while a
                // duress session is live, and we show the confirm dialog only then.
                ProtectionActiveCard(isActive: protectionActive)
                    .onTapGesture(count: 3) {
                        if duress.isDuressSession { confirmDuressExit = true }
                    }
                statsRow
                quickActionsSection
                HonestLimitsCallout(onTap: { showHonestLimitsSheet = true })
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 24)
        }
        .background(Color.appScreenBackground.ignoresSafeArea())
        .navigationBarHidden(true)
        .navigationDestination(isPresented: $showReports) { ReportsScreen() }
        .task {
            await vault.reload()
            await refreshStats()
            refreshBanners()
        }
        .sheet(isPresented: $showHonestLimitsSheet) {
            NavigationStack {
                HonestLimitsSheet()
            }
        }
        .confirmationDialog("dash_exit_duress_title", isPresented: $confirmDuressExit, titleVisibility: .visible) {
            Button("dash_exit", role: .destructive) {
                duress.isDuressSession = false
                gate.panic()
            }
            Button("lock_cancel", role: .cancel) {}
        } message: {
            Text("dash_exit_duress_body")
        }
    }

    private var statsRow: some View {
        // 1x3 row: Trusted Contacts · Blocked (today + week combined) · Scam Contacts.
        // scamContactsCount is cached @State refreshed in `.task` — see property above.
        HStack(spacing: 10) {
            StatTile(icon: "person.2.fill",
                     tint: Color.appAccent,
                     value: trustedCount,
                     label: NSLocalizedString("dash_trusted_contacts", comment: ""))
            BlockedCombinedTile(blockedToday: blockedToday, blockedWeek: blockedWeek)
            StatTile(icon: "exclamationmark.triangle.fill",
                     tint: scamContactsCount > 0 ? Color.appDanger : Color.appAccent,
                     value: scamContactsCount,
                     label: NSLocalizedString("dash_scam_contacts", comment: ""))
        }
    }

    private var quickActionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("dash_quick_actions")
                .font(.system(size: 22, weight: .bold, design: .rounded))
                .foregroundStyle(Color.appPrimaryText)

            HStack(spacing: 10) {
                QuickActionCard(
                    icon: "person.badge.plus.fill",
                    title: NSLocalizedString("dash_qa_emergency_title", comment: ""),
                    subtitle: NSLocalizedString("dash_qa_emergency_subtitle", comment: ""),
                    tint: Color.appAccent,
                    action: { onNavigate(.contacts) }
                )
                QuickActionCard(
                    icon: "phone.down.fill",
                    title: NSLocalizedString("dash_qa_block_title", comment: ""),
                    subtitle: NSLocalizedString("dash_qa_block_subtitle", comment: ""),
                    tint: Color.appDanger,
                    action: { onNavigate(.blocked) }
                )
                QuickActionCard(
                    icon: "doc.text.fill",
                    title: NSLocalizedString("dash_qa_log_title", comment: ""),
                    subtitle: NSLocalizedString("dash_qa_log_subtitle", comment: ""),
                    tint: Color.appBrightCyan,
                    action: { onNavigate(.log) }
                )
            }
        }
    }

    private func refreshStats() async {
        // Move the synchronous disk read + JSON decode off the main actor — BlockListStore
        // uses a serial queue.sync internally so any main-thread caller blocks until the
        // read completes. Same for ContactScanState (Keychain read).
        let (todayCount, weekCount, scamCount) = await Task.detached(priority: .userInitiated) {
            let all = BlockListStore.shared.allRecent()
            let cal = Calendar.current
            let startOfToday = cal.startOfDay(for: Date())
            let startOfWeek = cal.date(byAdding: .day, value: -6, to: startOfToday) ?? startOfToday
            let today = all.filter { $0.addedAt >= startOfToday }.count
            let week = all.filter { $0.addedAt >= startOfWeek }.count
            let scam = ContactScanState.matchCount()
            return (today, week, scam)
        }.value
        blockedToday = todayCount
        blockedWeek = weekCount
        scamContactsCount = scamCount
    }

    private func refreshBanners() {
        if HomeBannerPrefs.shouldShowWeeklyDigest() {
            weeklyDigestCounts = WeeklyDigestCounts.compute()
            showWeeklyDigest = true
        }
        if HomeBannerPrefs.shouldShowHonestLimitsRefresher() {
            showHonestLimitsRefresher = true
        }
    }

    private func dismissWeeklyDigest() {
        showWeeklyDigest = false
        HomeBannerPrefs.markWeeklyDigestShown()
    }

    private func dismissHonestLimitsRefresher() {
        showHonestLimitsRefresher = false
        HomeBannerPrefs.markHonestLimitsRefresherShown()
    }
}

// MARK: - F4 Weekly digest banner

private struct WeeklyDigestBanner: View {
    let callCount: Int
    let textCount: Int
    let onDismiss: () -> Void
    let onSeeReport: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "chart.bar.xaxis")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Color.appBrightCyan)
                .frame(width: 44, height: 44)
                .background(
                    Color.appBrightCyan.opacity(0.18),
                    in: RoundedRectangle(cornerRadius: 11, style: .continuous)
                )

            VStack(alignment: .leading, spacing: 6) {
                Text("dash_week_title")
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundStyle(Color.appPrimaryText)
                Text(summary)
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appSecondaryText)
                    .fixedSize(horizontal: false, vertical: true)
                HStack(spacing: 12) {
                    Button(action: onSeeReport) {
                        Text("dash_see_full_report")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color.appBrightCyan)
                    }
                    .buttonStyle(.plain)
                    Button(action: onDismiss) {
                        Text("dash_dismiss")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color.appSecondaryText)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.top, 2)
            }

            Spacer(minLength: 0)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color.appElevatedSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color.appBrightCyan.opacity(0.35), lineWidth: 1)
                )
        )
    }

    private var summary: String {
        if callCount == 0 && textCount == 0 {
            return NSLocalizedString("dash_week_empty", comment: "")
        }
        let fmt = NSLocalizedString("dash_week_fmt", comment: "")
        return String(format: fmt, callCount, textCount)
    }
}

// MARK: - F5 Honest-limits refresher banner

private struct HonestLimitsRefresherBanner: View {
    let onLearnMore: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "info.circle.fill")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Color.appWarning)
                .frame(width: 44, height: 44)
                .background(
                    Color.appWarning.opacity(0.18),
                    in: RoundedRectangle(cornerRadius: 11, style: .continuous)
                )

            VStack(alignment: .leading, spacing: 6) {
                Text("dash_monthly_reminder")
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundStyle(Color.appPrimaryText)
                Text("dash_cannot_monitor_other_apps")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appSecondaryText)
                    .fixedSize(horizontal: false, vertical: true)
                HStack(spacing: 12) {
                    Button(action: onLearnMore) {
                        Text("dash_learn_more")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color.appWarning)
                    }
                    .buttonStyle(.plain)
                    Button(action: onDismiss) {
                        Text("dash_dismiss")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color.appSecondaryText)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.top, 2)
            }

            Spacer(minLength: 0)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color.appWarning.opacity(0.08))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color.appWarning.opacity(0.4), lineWidth: 1)
                )
        )
    }
}

// MARK: - Cinematic header (lakescape backdrop + wordmark + settings + greeting)

private struct CinematicHeader: View {
    let name: String?
    let onSettings: () -> Void

    private var timeOfDay: String {
        let h = Calendar.current.component(.hour, from: Date())
        let key: String
        if h < 12 { key = "dash_good_morning" }
        else if h < 18 { key = "dash_good_afternoon" }
        else { key = "dash_good_evening" }
        return NSLocalizedString(key, comment: "")
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            // Lakescape backdrop (reuse SplashHero asset). The SplashHero PNG has the
            // "OnlyTrusted" wordmark + tagline baked into its middle band, so we scale
            // the image larger than the header frame and anchor to the TOP so only the
            // shield + mountain-ring section shows — the baked wordmark lives off-screen
            // below our clipped frame. A heavier darker gradient on top preserves text
            // legibility for the native greeting block.
            GeometryReader { proxy in
                Image("SplashHero")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: proxy.size.width * 1.4,
                           height: proxy.size.width * 1.4 * (1672.0 / 941.0),
                           alignment: .top)
                    .offset(x: -proxy.size.width * 0.2, y: -proxy.size.height * 0.12)
                    .frame(width: proxy.size.width, height: proxy.size.height, alignment: .top)
                    .clipped()
                    .overlay {
                        LinearGradient(
                            colors: [
                                Color.appScreenBackground.opacity(0.12),
                                Color.appScreenBackground.opacity(0.35),
                                Color.appScreenBackground.opacity(0.95)
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    }
            }
            .frame(height: 240)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))

            // Top row: wordmark + settings gear
            HStack(alignment: .top) {
                HStack(spacing: 0) {
                    Text("Only").foregroundStyle(.white)
                    Text("Trusted").foregroundStyle(Color.appAccent)
                }
                .font(.system(size: 24, weight: .bold, design: .rounded))

                Spacer()

                Button(action: onSettings) {
                    Image(systemName: "gearshape.fill")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Color.white)
                        .frame(width: 40, height: 40)
                        .background(
                            Circle()
                                .fill(Color.white.opacity(0.08))
                                .overlay(Circle().stroke(Color.appBrightCyan.opacity(0.4), lineWidth: 1))
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Settings")
            }
            .padding(.horizontal, 16)
            .padding(.top, 14)

            // Greeting block anchored to bottom-left of the header area
            VStack(alignment: .leading, spacing: 2) {
                Spacer()
                Text("\(timeOfDay),")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)
                HStack(spacing: 8) {
                    Text(name ?? NSLocalizedString("dash_welcome", comment: ""))
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundStyle(Color.appAccent)
                    Text("👋")
                        .font(.system(size: 24))
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .frame(height: 240, alignment: .bottomLeading)
        }
        .frame(height: 240)
    }
}

// MARK: - Protection active card

private struct ProtectionActiveCard: View {
    let isActive: Bool

    var body: some View {
        HStack(spacing: 16) {
            // Full OnlyTrusted brand-logo tile (shield + wordmark on midnight bg with
            // baked-in cyan ring + sparkles). The asset already carries its own ring
            // treatment, so we drop the extra Circle stroke / glow that used to wrap the
            // glyph and just add a soft cyan drop shadow when protection is active.
            Image("BrandLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 86, height: 86)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .opacity(isActive ? 1.0 : 0.55)
                .shadow(color: Color.appBrightCyan.opacity(isActive ? 0.55 : 0),
                        radius: 14, x: 0, y: 0)
                .accessibilityLabel("OnlyTrusted protection shield")

            VStack(alignment: .leading, spacing: 4) {
                Text(isActive ? "dash_protection_active" : "dash_setup_needed")
                    .font(.system(size: 20, weight: .bold, design: .rounded))
                    .foregroundStyle(Color.appAccent)
                Text(isActive ? "dash_protection_active_detail" : "dash_setup_needed_detail")
                    .font(.subheadline)
                    .foregroundStyle(Color.appSecondaryText)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)

            Image(systemName: "chevron.right")
                .font(.footnote.weight(.bold))
                .foregroundStyle(Color.appTertiaryText)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color.appElevatedSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(Color.appAccent.opacity(isActive ? 0.4 : 0.15), lineWidth: 1)
                )
                .shadow(color: Color.appAccent.opacity(isActive ? 0.2 : 0), radius: 18, x: 0, y: 0)
        )
    }
}

// MARK: - Stat tile (icon chip + number + label, previous version)

/// Combined "Blocked" tile: today's count prominent + 7-day total underneath. Replaces
/// the former pair of Blocked Today + Blocked This Week tiles so the dashboard reads
/// 1x3 instead of 2x2.
private struct BlockedCombinedTile: View {
    let blockedToday: Int
    let blockedWeek: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .top) {
                Image(systemName: "nosign")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.appDanger)
                    .frame(width: 32, height: 32)
                    .background(Color.appDanger.opacity(0.18), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                Spacer()
                Text("\(blockedToday)")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundStyle(Color.appDanger)
                    .minimumScaleFactor(0.6)
            }
            Text("dash_blocked")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Color.appPrimaryText)
            Text(String(format: NSLocalizedString("dash_today_week_fmt", comment: ""), blockedWeek))
                .font(.system(size: 10))
                .foregroundStyle(Color.appSecondaryText)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color.appElevatedSurface)
        )
    }
}

private struct StatTile: View {
    let icon: String
    let tint: Color
    let value: Int
    let label: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(tint)
                    .frame(width: 32, height: 32)
                    .background(tint.opacity(0.18), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                Spacer()
                Text("\(value)")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundStyle(tint)
                    .minimumScaleFactor(0.6)
            }
            Text(label)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
                .lineLimit(2)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color.appElevatedSurface)
        )
    }
}

// MARK: - Quick action card (icon chip + title + subtitle, previous version)

private struct QuickActionCard: View {
    let icon: String
    let title: String
    let subtitle: String
    let tint: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 10) {
                Image(systemName: icon)
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(tint)
                    .frame(width: 46, height: 46)
                    .background(tint.opacity(0.18), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .shadow(color: tint.opacity(0.4), radius: 10, x: 0, y: 0)

                Text(title)
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundStyle(Color.appPrimaryText)
                    .multilineTextAlignment(.leading)

                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(Color.appSecondaryText)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                    .lineLimit(2)
            }
            .padding(12)
            .frame(maxWidth: .infinity, minHeight: 150, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color.appElevatedSurface)
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Honest Limits callout + sheet (restored)

private struct HonestLimitsCallout: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(Color.appWarning)
                    .frame(width: 48, height: 48)
                    .background(
                        Color.appWarning.opacity(0.18),
                        in: RoundedRectangle(cornerRadius: 12, style: .continuous)
                    )
                    .shadow(color: Color.appWarning.opacity(0.4), radius: 10)

                VStack(alignment: .leading, spacing: 4) {
                    Text("dash_honest_limits")
                        .font(.system(size: 17, weight: .bold, design: .rounded))
                        .foregroundStyle(Color.appWarning)
                    Text("dash_tap_see_limits")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.appSecondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.bold))
                    .foregroundStyle(Color.appWarning.opacity(0.7))
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color.appWarning.opacity(0.08))
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(Color.appWarning.opacity(0.5), lineWidth: 1)
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

private struct HonestLimitsSheet: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                limitBullet("Monitor other apps' microphone or camera use.")
                limitBullet("Stop recordings made on another device.")
                limitBullet("Place watermarks over other apps on iOS.")
                limitBullet("Prevent carrier or operating-system tracking.")
                limitBullet("Guarantee that every unwanted contact will be blocked — some requires enabling Apple's system extensions in iOS Settings.")
            }
            .padding(20)
        }
        .background(Color.appScreenBackground.ignoresSafeArea())
        .navigationTitle("dash_honest_limits")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("dash_done") { dismiss() }
                    .tint(Color.appAccent)
            }
        }
    }

    private func limitBullet(_ text: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            Circle()
                .fill(Color.appWarning)
                .frame(width: 6, height: 6)
                .offset(y: -3)
            Text(text)
                .font(.subheadline)
                .foregroundStyle(Color.appPrimaryText)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

