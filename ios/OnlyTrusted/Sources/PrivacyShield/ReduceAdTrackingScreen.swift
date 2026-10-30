// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI
import UIKit

/// One-screen helper: deep-links to the places users can actually reduce
/// ad-tracking surface (Google Account controls, YouTube/Maps history, iOS
/// Privacy settings). We do NOT scrape, auto-toggle, or sign in anywhere —
/// Apple forbids managing another app's permissions from a third-party app
/// (there is no equivalent of Android's ACTION_APPLICATION_DETAILS_SETTINGS),
/// and we won't automate anyone's Google account.
///
/// Honest framing stays visible at the top: "We show you the dial — we can't
/// turn it for you."
struct ReduceAdTrackingScreen: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                honestFramingCard
                sectionHeader("Your Google account")
                googleAccountCard
                sectionHeader("System privacy (iOS)")
                systemPrivacyCard
                sectionHeader("Common ad-tracking apps")
                perAppDisclaimer
                perAppCard
                Text(
                    "Honest limit: Google links data across devices on the same " +
                    "Wi-Fi, same contact graph, and same account. A consumer app " +
                    "cannot block that linkage — only Google can. The quick-links " +
                    "above are the fastest way to pull those dials yourself."
                )
                .font(.caption)
                .foregroundStyle(Color.appSecondaryText)
                .padding(.top, 6)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle("Reduce ad tracking")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Sections

    private var honestFramingCard: some View {
        PremiumCard {
            VStack(alignment: .leading, spacing: 6) {
                Text("We show you the dial — we can't turn it for you.")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.appPrimaryText)
                Text(
                    "iOS does not let a third-party app change another app's " +
                    "permissions or sign into your Google account. These are " +
                    "one-tap shortcuts to the real settings where you control it."
                )
                .font(.caption)
                .foregroundStyle(Color.appSecondaryText)
                .fixedSize(horizontal: false, vertical: true)
            }
            .padding(14)
        }
    }

    private var googleAccountCard: some View {
        PremiumCard {
            VStack(spacing: 0) {
                QuickLinkRow(
                    icon: "clock.arrow.circlepath",
                    title: "Review & clear activity",
                    subtitle: "myactivity.google.com — search, YouTube, Maps history. Pause or delete here.",
                    url: "https://myactivity.google.com/"
                )
                Divider().overlay(Color.appSecondaryText.opacity(0.2))
                QuickLinkRow(
                    icon: "slider.horizontal.3",
                    title: "Turn off personalised ads",
                    subtitle: "myadcenter.google.com — switch ad personalisation off across Google surfaces.",
                    url: "https://myadcenter.google.com/"
                )
                Divider().overlay(Color.appSecondaryText.opacity(0.2))
                QuickLinkRow(
                    icon: "play.rectangle.fill",
                    title: "YouTube watch history",
                    subtitle: "Review or clear what shapes your YouTube recommendations.",
                    url: "https://www.youtube.com/feed/history"
                )
                Divider().overlay(Color.appSecondaryText.opacity(0.2))
                QuickLinkRow(
                    icon: "map.fill",
                    title: "Location history (Maps Timeline)",
                    subtitle: "google.com/maps/timeline — pause and delete past locations.",
                    url: "https://www.google.com/maps/timeline"
                )
                Divider().overlay(Color.appSecondaryText.opacity(0.2))
                QuickLinkRow(
                    icon: "waveform.and.mic",
                    title: "\"Hey Google\" voice match",
                    subtitle: "Open the Google app → Settings → Google Assistant → Hey Google & Voice Match.",
                    url: "https://support.google.com/assistant/answer/7394306"
                )
            }
        }
    }

    private var systemPrivacyCard: some View {
        PremiumCard {
            VStack(spacing: 0) {
                QuickLinkRow(
                    icon: "lock.shield.fill",
                    title: "iOS Privacy settings",
                    subtitle: "Settings → Privacy & Security. Mic, Camera, Location, Tracking per app.",
                    openSystemSettings: true
                )
                Divider().overlay(Color.appSecondaryText.opacity(0.2))
                QuickLinkRow(
                    icon: "hand.raised.fill",
                    title: "App Tracking Transparency",
                    subtitle: "Settings → Privacy & Security → Tracking. Turn off \"Allow Apps to Request to Track\".",
                    openSystemSettings: true
                )
                Divider().overlay(Color.appSecondaryText.opacity(0.2))
                QuickLinkRow(
                    icon: "doc.text.magnifyingglass",
                    title: "App Privacy Report",
                    subtitle: "Settings → Privacy & Security → App Privacy Report. See which apps touched mic, camera, location, contacts in the last 7 days.",
                    openSystemSettings: true
                )
            }
        }
    }

    private var perAppDisclaimer: some View {
        Text(
            "iOS keeps each app's permissions inside Settings. Tap any app below " +
            "to open its page, or long-press its icon on the Home Screen → App " +
            "Info → Privacy."
        )
        .font(.caption)
        .foregroundStyle(Color.appSecondaryText)
        .fixedSize(horizontal: false, vertical: true)
    }

    private var perAppCard: some View {
        PremiumCard {
            VStack(spacing: 0) {
                ForEach(ReduceAdTrackingScreen.appTargets.indices, id: \.self) { idx in
                    let target = ReduceAdTrackingScreen.appTargets[idx]
                    if idx > 0 { Divider().overlay(Color.appSecondaryText.opacity(0.2)) }
                    QuickLinkRow(
                        icon: target.sfSymbol,
                        title: target.name,
                        subtitle: "Open \(target.name) if installed. Then tap your profile → Settings → Privacy.",
                        url: target.universalLink
                    )
                }
            }
        }
    }

    // MARK: - Static data

    /// Universal links for common ad-tracking apps. These open the app if
    /// installed, or route to its web/App Store page as fallback. Unlike
    /// Android there is no API to open another app's iOS Settings page
    /// directly — Apple reserves that. The honest best we can do is open the
    /// app so the user can navigate to its in-app privacy menu.
    private struct AppTarget: Hashable {
        let name: String
        let sfSymbol: String
        let universalLink: String
    }

    private static let appTargets: [AppTarget] = [
        .init(name: "YouTube", sfSymbol: "play.rectangle.fill",
              universalLink: "https://www.youtube.com/account_privacy"),
        .init(name: "Google", sfSymbol: "magnifyingglass.circle.fill",
              universalLink: "https://myaccount.google.com/data-and-privacy"),
        .init(name: "Chrome", sfSymbol: "safari.fill",
              universalLink: "https://myactivity.google.com/activitycontrols"),
        .init(name: "Google Maps", sfSymbol: "map.fill",
              universalLink: "https://www.google.com/maps/timeline"),
        .init(name: "Gmail", sfSymbol: "envelope.fill",
              universalLink: "https://myaccount.google.com/data-and-privacy"),
        .init(name: "Facebook", sfSymbol: "person.2.fill",
              universalLink: "https://accountscenter.facebook.com/privacy_and_safety"),
        .init(name: "Instagram", sfSymbol: "camera.fill",
              universalLink: "https://accountscenter.facebook.com/privacy_and_safety"),
        .init(name: "TikTok", sfSymbol: "music.note",
              universalLink: "https://www.tiktok.com/safety/en/privacy-security/"),
        .init(name: "WhatsApp", sfSymbol: "message.fill",
              universalLink: "https://faq.whatsapp.com/privacy-and-security"),
        .init(name: "Zoom", sfSymbol: "video.fill",
              universalLink: "https://explore.zoom.us/en/privacy/"),
        .init(name: "Spotify", sfSymbol: "music.note.list",
              universalLink: "https://www.spotify.com/account/privacy/"),
        .init(name: "Snapchat", sfSymbol: "camera.on.rectangle.fill",
              universalLink: "https://support.snapchat.com/a/privacy-settings"),
        .init(name: "X (Twitter)", sfSymbol: "xmark.circle.fill",
              universalLink: "https://twitter.com/settings/privacy_and_safety"),
        .init(name: "LinkedIn", sfSymbol: "briefcase.fill",
              universalLink: "https://www.linkedin.com/mypreferences/d/categories/privacy"),
    ]

    // MARK: - Helpers

    @ViewBuilder
    private func sectionHeader(_ text: String) -> some View {
        Text(text.uppercased())
            .font(.caption.weight(.semibold))
            .foregroundStyle(Color.appSecondaryText)
            .padding(.top, 4)
            .padding(.bottom, 2)
    }
}

/// One tappable row. Either opens a URL or iOS's own Settings app
/// (via `openSettingsURLString` which is the only system Settings deep-link
/// Apple allows a third-party app to use).
private struct QuickLinkRow: View {
    let icon: String
    let title: String
    let subtitle: String
    var url: String? = nil
    var openSystemSettings: Bool = false

    var body: some View {
        Button(action: open) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color.appAccent.opacity(0.18))
                        .frame(width: 36, height: 36)
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Color.appAccent)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.appPrimaryText)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(Color.appSecondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer()
                Image(systemName: "arrow.up.right.square")
                    .font(.caption)
                    .foregroundStyle(Color.appSecondaryText)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func open() {
        if openSystemSettings {
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url)
            }
            return
        }
        if let urlString = url, let u = URL(string: urlString) {
            UIApplication.shared.open(u)
        }
    }
}
