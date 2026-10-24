// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI

/// Pro-only screen reached from Settings → Security & data → "Wangiri Protection (Pro)".
///
/// Shows:
///  - A master toggle (default ON).
///  - This month's detected count.
///  - The 20 most recent wangiri hangups observed by `CXCallObserver`.
///  - An iOS limitation note explaining why the specific caller number is not auto-blocked.
///
/// Important: on iOS this is a DETECTOR, not a blocker. iOS CallKit does not expose
/// the caller number to third-party apps, so we can only count the pattern and alert
/// the owner — we cannot programmatically add the number to the block list the way
/// the Android equivalent (via `PhoneStateListener`) can. UI copy throughout this
/// screen says "detected" / "flagged", never "blocked", for honest-claim reasons.
struct WangiriSettingsScreen: View {
    @StateObject private var watcher = WangiriWatcher.shared
    @State private var enabledToggle: Bool = WangiriWatcher.shared.isEnabled

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 20) {
                statusCard
                historyCard
                explainerCard
                limitationCard
            }
            .padding(.horizontal, 18)
            .padding(.top, 10)
            .padding(.bottom, 32)
        }
        .appBackdrop()
        .navigationTitle("Wangiri Protection")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            enabledToggle = watcher.isEnabled
            if watcher.isEnabled { watcher.startIfNeeded() }
        }
    }

    // MARK: - Status

    private var statusCard: some View {
        PremiumCard {
            VStack(alignment: .leading, spacing: 14) {
                CardHeader(icon: "phone.down.waveform.fill",
                           title: "One-ring hangup detector",
                           tint: Color.appAccent)

                Toggle(isOn: $enabledToggle) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Enabled")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Color.appPrimaryText)
                        Text("Watches CallKit for ring-and-drop scam calls.")
                            .font(.caption)
                            .foregroundStyle(Color.appSecondaryText)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .tint(Color.appAccent)
                .onChange(of: enabledToggle) { _, now in
                    watcher.isEnabled = now
                }

                Divider().background(Color.appDivider.opacity(0.3))

                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Detected this month")
                            .font(.caption)
                            .foregroundStyle(Color.appSecondaryText)
                        Text("\(watcher.detectedThisMonth)")
                            .font(.system(size: 32, weight: .bold, design: .rounded))
                            .foregroundStyle(Color.appAccent)
                    }
                    Spacer()
                    Image(systemName: "phone.down.waveform.fill")
                        .font(.system(size: 36, weight: .semibold))
                        .foregroundStyle(Color.appAccent.opacity(0.4))
                }
            }
        }
    }

    // MARK: - History

    private var historyCard: some View {
        PremiumCard {
            VStack(alignment: .leading, spacing: 12) {
                CardHeader(icon: "clock.arrow.circlepath",
                           title: "Recent wangiri hangups",
                           tint: Color.appWarning)
                if watcher.recentBlocks.isEmpty {
                    Text("No one-ring hangups detected yet. If a wangiri call comes in while Pro is active it will appear here.")
                        .font(.footnote)
                        .foregroundStyle(Color.appSecondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                } else {
                    ForEach(watcher.recentBlocks) { row in
                        HStack(spacing: 12) {
                            Image(systemName: "phone.down.fill")
                                .foregroundStyle(Color.appDanger)
                                .frame(width: 28, height: 28)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(formatted(row.at))
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(Color.appPrimaryText)
                                Text(String(format: "Rang for %.1fs before hangup", row.elapsedSeconds))
                                    .font(.caption)
                                    .foregroundStyle(Color.appSecondaryText)
                            }
                            Spacer()
                        }
                        .padding(.vertical, 4)
                        if row.id != watcher.recentBlocks.last?.id {
                            Divider().background(Color.appDivider.opacity(0.3))
                        }
                    }
                }
            }
        }
    }

    // MARK: - Explainer

    private var explainerCard: some View {
        PremiumCard {
            VStack(alignment: .leading, spacing: 10) {
                CardHeader(icon: "questionmark.circle.fill",
                           title: "What is wangiri?",
                           tint: Color.appBrightCyan)
                Text("Wangiri (Japanese for 'one ring and cut') is a scam where a premium-rate international number dials you, rings for under two seconds, and hangs up. The scammer hopes you call back out of curiosity. The callback routes through a premium number and charges you per minute until you notice.")
                    .font(.footnote)
                    .foregroundStyle(Color.appSecondaryText)
                    .fixedSize(horizontal: false, vertical: true)
                Text("OnlyTrusted watches CallKit for this ring-and-drop pattern and logs each one. Don't call back unknown international numbers.")
                    .font(.footnote)
                    .foregroundStyle(Color.appSecondaryText)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    // MARK: - iOS limitation

    private var limitationCard: some View {
        PremiumCard {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 8) {
                    Image(systemName: "info.circle.fill")
                        .foregroundStyle(Color.appWarning)
                    Text("iOS limitation")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.appPrimaryText)
                }
                Text("Apple's CallKit framework does not expose the caller's phone number for privacy reasons, so OnlyTrusted can detect and count wangiri hangups but cannot auto-add the specific caller to your block list on iOS.")
                    .font(.caption)
                    .foregroundStyle(Color.appSecondaryText)
                    .fixedSize(horizontal: false, vertical: true)
                Text("Open your Phone app's Recents, long-press the number, and tap 'Block this Caller' to add it manually. The number also stays logged in Recents so your carrier can file a report.")
                    .font(.caption)
                    .foregroundStyle(Color.appSecondaryText)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private func formatted(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateStyle = .medium
        f.timeStyle = .short
        return f.string(from: date)
    }
}

/// Upsell sheet shown when a free user taps "Wangiri Protection (Pro)".
struct WangiriUpsellSheet: View {
    let onUpgrade: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "phone.down.waveform.fill")
                .font(.system(size: 46, weight: .semibold))
                .foregroundStyle(Color.subGold)
                .padding(.top, 28)
            Text("Wangiri Protection")
                .font(.title2.weight(.bold))
                .foregroundStyle(Color.appPrimaryText)
            Text("Detects the classic one-ring hangup scam the moment it happens, logs every attempt, and warns you not to call back. Included in Pro.")
                .font(.subheadline)
                .foregroundStyle(Color.appSecondaryText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Spacer(minLength: 0)
            Button(action: onUpgrade) {
                Text("Upgrade to Pro")
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 48)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.subGold)
            .padding(.horizontal, 24)
            Button("Not now", action: onDismiss)
                .foregroundStyle(Color.appSecondaryText)
                .padding(.bottom, 20)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .appBackdrop()
    }
}
