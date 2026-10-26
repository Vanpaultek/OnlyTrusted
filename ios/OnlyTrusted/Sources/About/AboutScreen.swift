// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI

struct AboutScreen: View {
    // The public source repository URL. Confirmed from `git remote get-url origin`
    // as `https://github.com/Vanpaultek/OnlyTrusted.git`. If the repo is renamed or
    // moved under the VanPaulTek org, update this constant.
    private static let sourceURL = "https://github.com/Vanpaultek/OnlyTrusted"
    private static let supportEmail = "admin@vanpaultek.com"

    private var version: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
    }
    private var build: String {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "?"
    }

    @Environment(\.dismiss) private var dismiss
    @StateObject private var subscription = SubscriptionState.shared

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 20) {
                cinematicHero
                VStack(spacing: 10) {
                    rows
                }
                .padding(.horizontal, 18)
            }
            .padding(.bottom, 32)
        }
        .appBackdrop()
        .navigationBarHidden(true)
    }

    // MARK: - Cinematic header (lakescape backdrop + wordmark + version + Settings back link)

    private var cinematicHero: some View {
        ZStack(alignment: .topLeading) {
            // Lakescape backdrop — scale slightly larger than frame and anchor so the shield
            // + mountain + lake band all stay visible; gentle gradient preserves text legibility
            // without washing out the hero.
            GeometryReader { proxy in
                Image("SplashHero")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: proxy.size.width * 1.15,
                           height: proxy.size.width * 1.15 * (1672.0 / 941.0),
                           alignment: .top)
                    .offset(x: -proxy.size.width * 0.075, y: -proxy.size.height * 0.05)
                    .frame(width: proxy.size.width, height: proxy.size.height, alignment: .top)
                    .clipped()
                    .overlay {
                        LinearGradient(
                            stops: [
                                .init(color: Color.appScreenBackground.opacity(0.08), location: 0.0),
                                .init(color: Color.appScreenBackground.opacity(0.18), location: 0.55),
                                .init(color: Color.appScreenBackground.opacity(0.7), location: 0.88),
                                .init(color: Color.appScreenBackground, location: 1.0)
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    }
            }
            .frame(height: 320)

            // Settings back link
            Button(action: { dismiss() }) {
                HStack(spacing: 4) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Settings")
                        .font(.system(size: 16, weight: .semibold))
                }
                .foregroundStyle(Color.appBrightCyan)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Back to Settings")
            .padding(.leading, 18)
            .padding(.top, 14)

            // About label + wordmark + version anchored bottom-left of hero
            VStack(alignment: .leading, spacing: 2) {
                Spacer()
                Text("About")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(Color.appPrimaryText)
                HStack(spacing: 0) {
                    Text("Only").foregroundStyle(.white)
                    Text("Trusted").foregroundStyle(Color.appBrightCyan)
                }
                .font(.system(size: 32, weight: .bold, design: .rounded))
                Text("Version \(version) (\(build))")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(Color.appSecondaryText)
                    .padding(.top, 4)
            }
            .padding(.leading, 18)
            .padding(.bottom, 18)
            .frame(height: 300, alignment: .bottomLeading)
        }
        .frame(height: 300)
    }

    // MARK: - 7 About rows

    @ViewBuilder
    private var rows: some View {
        AboutRow(
            icon: "person.fill",
            tint: Color.settingsBlue,
            title: "App Owner",
            subtitle: "Company or individual information.",
            value: "VanPaulTek Inc",
            action: nil
        )
        AboutRow(
            icon: "envelope.fill",
            tint: Color.settingsPurple,
            title: "Support Email",
            subtitle: "Get help or contact support.",
            value: Self.supportEmail,
            action: openSupportMail
        )
        // AGPL-3.0 §4-5 require recipients to be told the work is AGPL-licensed and given a
        // way to obtain the source. License + Open Source rows must stay visible to everyone.
        AboutRow(
            icon: "doc.text.fill",
            tint: Color.aboutGreen,
            title: "License",
            subtitle: "Open source and usage license.",
            value: "GNU AGPL-3.0",
            action: nil
        )
        AboutRow(
            icon: "chevron.left.forwardslash.chevron.right",
            tint: Color.settingsBlue,
            title: "Open Source",
            subtitle: "View source code on GitHub.",
            value: "View on GitHub",
            action: openSourceRepo
        )
        NavigationLink { UserGuideScreen() } label: {
            AboutRowContent(
                icon: "book.fill",
                tint: Color.aboutGreen,
                title: "User Guide",
                subtitle: "Every feature, how to find it, what it does.",
                value: nil
            )
        }
        .buttonStyle(.plain)
        NavigationLink { PricingChartScreen() } label: {
            AboutRowContent(
                icon: "tag.fill",
                tint: Color.aboutOrange,
                title: "Pricing Chart",
                subtitle: "All plans and features, side by side.",
                value: nil
            )
        }
        .buttonStyle(.plain)
        if subscription.isActive {
            NavigationLink { AcknowledgmentsScreen() } label: {
                AboutRowContent(
                    icon: "rosette",
                    tint: Color.aboutOrange,
                    title: "Acknowledgements",
                    subtitle: "Libraries, tools and contributors.",
                    value: nil
                )
            }
            .buttonStyle(.plain)
            NavigationLink { ThirdPartyLibrariesScreen() } label: {
                AboutRowContent(
                    icon: "cube.box.fill",
                    tint: Color.settingsPurple,
                    title: "Third-Party Libraries",
                    subtitle: "View third-party components.",
                    value: nil
                )
            }
            .buttonStyle(.plain)
            NavigationLink { AppIconsAttributionScreen() } label: {
                AboutRowContent(
                    icon: "square.grid.2x2.fill",
                    tint: Color.aboutPink,
                    title: "App Icons",
                    subtitle: "App icons and brand assets.",
                    value: nil
                )
            }
            .buttonStyle(.plain)
        }
    }

    private func openSupportMail() {
        let subject = "OnlyTrusted%20\(version)%20support"
        if let url = URL(string: "mailto:\(Self.supportEmail)?subject=\(subject)") {
            UIApplication.shared.open(url)
        }
    }

    private func openSourceRepo() {
        if let url = URL(string: Self.sourceURL) {
            UIApplication.shared.open(url)
        }
    }
}

// MARK: - About row (icon chip + title + subtitle + optional value + chevron)

/// Button-wrapped row for About entries that trigger an action (mailto, URL open, etc.).
/// Pass `action: nil` to render a non-interactive display row.
private struct AboutRow: View {
    let icon: String
    let tint: Color
    let title: String
    let subtitle: String
    let value: String?
    let action: (() -> Void)?

    var body: some View {
        if let action {
            Button(action: action) {
                AboutRowContent(icon: icon, tint: tint, title: title, subtitle: subtitle, value: value)
            }
            .buttonStyle(.plain)
        } else {
            AboutRowContent(icon: icon, tint: tint, title: title, subtitle: subtitle, value: value)
        }
    }
}

/// The visual content of an About row — colored gradient icon chip on left,
/// title + subtitle in the middle, optional value in cyan + chevron on right.
private struct AboutRowContent: View {
    let icon: String
    let tint: Color
    let title: String
    let subtitle: String
    let value: String?

    var body: some View {
        HStack(alignment: .center, spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 11, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [tint, tint.opacity(0.75)],
                            startPoint: .topLeading, endPoint: .bottomTrailing
                        )
                    )
                    .shadow(color: tint.opacity(0.5), radius: 8, x: 0, y: 2)
                Image(systemName: icon)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(.white)
            }
            .frame(width: 46, height: 46)
            .decorativeAccessibility()

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(Color.appPrimaryText)
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appSecondaryText)
            }

            Spacer(minLength: 8)

            if let value {
                Text(value)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color.appBrightCyan)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Color.appBrightCyan)
                .decorativeAccessibility()
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color.appElevatedSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color.appBrightCyan.opacity(0.08), lineWidth: 1)
                )
        )
        .contentShape(Rectangle())
    }
}

// MARK: - Icon-chip tints shared across About + Upgrade (Pro tier highlight)

extension Color {
    static let aboutGreen  = Color(red:  34.0/255.0, green: 197.0/255.0, blue:  94.0/255.0)
    static let aboutOrange = Color(red: 245.0/255.0, green: 158.0/255.0, blue:  11.0/255.0)
    static let aboutPink   = Color(red: 236.0/255.0, green:  72.0/255.0, blue: 153.0/255.0)
}

// MARK: - Sub-screens

struct AcknowledgmentsScreen: View {
    private let entries: [(String, String)] = [
        ("Our testers", "Everyone who ran the early TestFlight builds and reported bugs over email."),
        ("Open-source maintainers", "The authors of every project in Third-Party Libraries. Their work made this app possible."),
        ("Users who filed bug reports", "Especially the people whose block lists and SMS filter logs helped us find the rough edges.")
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                PremiumCard {
                    VStack(alignment: .leading, spacing: 16) {
                        CardHeader(icon: "hands.sparkles.fill", title: "Acknowledgments")
                        VStack(alignment: .leading, spacing: 14) {
                            ForEach(entries, id: \.0) { entry in
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(entry.0)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundStyle(Color.appPrimaryText)
                                    Text(entry.1)
                                        .font(.footnote)
                                        .foregroundStyle(Color.appSecondaryText)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                            }
                        }
                    }
                }
            }
            .padding(20)
        }
        .appBackdrop()
        .navigationTitle("Acknowledgments")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct ThirdPartyLibrariesScreen: View {
    private struct Entry: Identifiable {
        let id = UUID()
        let name: String
        let license: String
        let url: String
    }

    private let entries: [Entry] = [
        Entry(name: "Swift", license: "Apache License 2.0", url: "swift.org"),
        Entry(name: "Foundation", license: "Apache License 2.0", url: "swift.org"),
        Entry(name: "SwiftUI", license: "Apple SDK", url: "developer.apple.com"),
        Entry(name: "CryptoKit", license: "Apple SDK", url: "developer.apple.com"),
        Entry(name: "LocalAuthentication", license: "Apple SDK", url: "developer.apple.com"),
        Entry(name: "CallKit / Call Directory", license: "Apple SDK", url: "developer.apple.com"),
        Entry(name: "IdentityLookup (SMS filter)", license: "Apple SDK", url: "developer.apple.com")
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                PremiumCard {
                    VStack(alignment: .leading, spacing: 16) {
                        CardHeader(icon: "shippingbox.fill", title: "Third-Party Libraries")
                        VStack(alignment: .leading, spacing: 14) {
                            ForEach(entries) { entry in
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(entry.name)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundStyle(Color.appPrimaryText)
                                    Text("\(entry.license) · \(entry.url)")
                                        .font(.caption)
                                        .foregroundStyle(Color.appSecondaryText)
                                }
                            }
                        }
                        Text("Attribution links are provided for reference. OnlyTrusted does not make any network requests on your behalf.")
                            .font(.caption)
                            .foregroundStyle(Color.appTertiaryText)
                            .fixedSize(horizontal: false, vertical: true)
                            .padding(.top, 4)
                    }
                }
            }
            .padding(20)
        }
        .appBackdrop()
        .navigationTitle("Third-Party Libraries")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct AppIconsAttributionScreen: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                PremiumCard {
                    VStack(alignment: .leading, spacing: 16) {
                        CardHeader(icon: "app.badge.fill", title: "App Icons")
                        VStack(alignment: .leading, spacing: 10) {
                            Text("SF Symbols")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(Color.appPrimaryText)
                            Text("All in-app icons are drawn from Apple's SF Symbols library, used under the SF Symbols license.")
                                .font(.footnote)
                                .foregroundStyle(Color.appSecondaryText)
                                .fixedSize(horizontal: false, vertical: true)
                            Text("developer.apple.com/sf-symbols/")
                                .font(.caption)
                                .foregroundStyle(Color.appTertiaryText)
                        }
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Launcher icon")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(Color.appPrimaryText)
                            Text("The OnlyTrusted shield mark is an original work by VanPaulTek Inc, licensed under GNU AGPL-3.0 along with the rest of the app.")
                                .font(.footnote)
                                .foregroundStyle(Color.appSecondaryText)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }
            }
            .padding(20)
        }
        .appBackdrop()
        .navigationTitle("App Icons")
        .navigationBarTitleDisplayMode(.inline)
    }
}
