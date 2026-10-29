// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI

/// Billing & Subscription screen. Current plan card + 4 nav rows (Manage, Payment Method,
/// Billing History, Restore Purchase). Values are currently local state / placeholders —
/// swap in StoreKit `Transaction.currentEntitlements` and `Product.purchase(options:)` when
/// the IAP work lands.
struct BillingSubscriptionScreen: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var sub = SubscriptionState.shared
    @State private var showManageSheet = false
    @State private var showPaymentMethodSheet = false
    @State private var showBillingHistorySheet = false
    @State private var restoreNotice: String?

    var body: some View {
        VStack(spacing: 0) {
            header
                .padding(.horizontal, 18)
                .padding(.top, 6)
                .padding(.bottom, 12)
                .background(
                    ZStack {
                        Color.appScreenBackground
                        Color.black.opacity(0.25)
                    }
                    .ignoresSafeArea(edges: .top)
                )

            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 20) {
                    currentPlanCard
                    VStack(spacing: 12) {
                        SubscriptionNavRow(
                            icon: "doc.text.magnifyingglass",
                            title: "Manage Subscription",
                            subtitle: "Update plan or cancel",
                            action: { showManageSheet = true }
                        )
                        SubscriptionNavRow(
                            icon: "creditcard.fill",
                            title: "Payment Method",
                            subtitle: "Visa •••• \(sub.lastFour)",
                            action: { showPaymentMethodSheet = true }
                        )
                        SubscriptionNavRow(
                            icon: "doc.text.fill",
                            title: "Billing History",
                            subtitle: "View past invoices",
                            action: { showBillingHistorySheet = true }
                        )
                        SubscriptionNavRow(
                            icon: "bag.badge.plus",
                            title: "Restore Purchase",
                            subtitle: "Restore your premium access",
                            action: { restoreTapped() }
                        )
                    }
                }
                .padding(.horizontal, 18)
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
        }
        .appBackdrop()
        .navigationBarHidden(true)
        .alert(restoreNotice ?? "", isPresented: Binding(
            get: { restoreNotice != nil },
            set: { if !$0 { restoreNotice = nil } }
        )) {
            Button("OK", role: .cancel) {}
        }
        .sheet(isPresented: $showManageSheet) {
            NavigationStack { ManageSubscriptionSheet() }
        }
        .sheet(isPresented: $showPaymentMethodSheet) {
            NavigationStack { PaymentMethodSheet() }
        }
        .sheet(isPresented: $showBillingHistorySheet) {
            NavigationStack { BillingHistorySheet() }
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 10) {
            Button(action: { dismiss() }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(Color.appBrightCyan)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Back")
            Text("Billing & Subscription")
                .font(.system(size: 22, weight: .bold, design: .rounded))
                .foregroundStyle(Color.appPrimaryText)
            Spacer()
        }
    }

    // MARK: - Current plan card

    private var currentPlanCard: some View {
        VStack(spacing: 14) {
            HStack(alignment: .top, spacing: 16) {
                // Glowing gold crown badge
                ZStack {
                    Circle()
                        .stroke(Color.subGold, lineWidth: 3)
                        .frame(width: 76, height: 76)
                        .shadow(color: Color.subGold.opacity(0.7), radius: 14, x: 0, y: 0)
                    Image(systemName: "crown.fill")
                        .font(.system(size: 32, weight: .semibold))
                        .foregroundStyle(Color.subGold)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text("Current Plan")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(Color.appSecondaryText)
                    Text(sub.planName)
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundStyle(Color.appBrightCyan)
                    Text(sub.pricingSummary)
                        .font(.system(size: 15))
                        .foregroundStyle(Color.appPrimaryText)
                }

                Spacer()

                Text(sub.isActive ? "Active" : "Inactive")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 6)
                    .background(
                        Capsule().fill(sub.isActive ? Color.appSuccess : Color.appDanger)
                    )
            }

            Divider().background(Color.appBrightCyan.opacity(0.15))

            VStack(spacing: 4) {
                Text("Next billing date")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color.appSecondaryText)
                Text(dateFmt(sub.nextBillingDate))
                    .font(.system(size: 20, weight: .bold, design: .rounded))
                    .foregroundStyle(Color.appPrimaryText)
            }
            .frame(maxWidth: .infinity)
            .padding(.bottom, 6)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color.appElevatedSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(Color.appBrightCyan.opacity(0.2), lineWidth: 1)
                )
        )
    }

    private func restoreTapped() {
        // Hook: swap for `AppStore.sync()` + replay of `Transaction.currentEntitlements`.
        restoreNotice = sub.isActive
            ? "Your Premium access was already active on this device."
            : "No previous purchases were found on this Apple ID."
    }

    private func dateFmt(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "MMM d, yyyy"
        return f.string(from: date)
    }
}

// MARK: - Row component

private struct SubscriptionNavRow: View {
    let icon: String
    let title: String
    let subtitle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color.settingsBlue.opacity(0.18))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(Color.settingsBlue.opacity(0.45), lineWidth: 1)
                        )
                    Image(systemName: icon)
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(Color.settingsBlue)
                }
                .frame(width: 46, height: 46)

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Color.appPrimaryText)
                    Text(subtitle)
                        .font(.system(size: 13))
                        .foregroundStyle(Color.appSecondaryText)
                }

                Spacer(minLength: 8)

                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.bold))
                    .foregroundStyle(Color.appTertiaryText)
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color.appElevatedSurface)
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Simple sub-sheets

private struct ManageSubscriptionSheet: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var sub = SubscriptionState.shared
    var body: some View {
        VStack(spacing: 16) {
            Text("On iOS, subscription changes are managed by the App Store.")
                .font(.subheadline)
                .foregroundStyle(Color.appSecondaryText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 20)
            Button {
                if let url = URL(string: "https://apps.apple.com/account/subscriptions") {
                    UIApplication.shared.open(url)
                }
            } label: {
                Text("Open App Store Subscriptions")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(Color.appScreenBackground)
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(Capsule().fill(Color.appBrightCyan))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 20)
            Spacer()
        }
        .padding(.top, 20)
        .appBackdrop()
        .navigationTitle("Manage Subscription")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) { Button("Done") { dismiss() }.tint(Color.appBrightCyan) }
        }
    }
}

private struct PaymentMethodSheet: View {
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        VStack(spacing: 12) {
            Text("Payment method is managed by your Apple ID.")
                .font(.subheadline).foregroundStyle(Color.appSecondaryText)
                .multilineTextAlignment(.center).padding(.horizontal, 20)
            Spacer()
        }
        .padding(.top, 20)
        .appBackdrop()
        .navigationTitle("Payment Method")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) { Button("Done") { dismiss() }.tint(Color.appBrightCyan) }
        }
    }
}

private struct BillingHistorySheet: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var sub = SubscriptionState.shared
    var body: some View {
        List {
            ForEach(sub.invoiceHistory) { entry in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(entry.dateString)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Color.appPrimaryText)
                        Text(entry.description)
                            .font(.caption)
                            .foregroundStyle(Color.appSecondaryText)
                    }
                    Spacer()
                    Text(entry.amountString)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.appAccent)
                }
                .listRowBackground(Color.appElevatedSurface)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.appScreenBackground)
        .navigationTitle("Billing History")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) { Button("Done") { dismiss() }.tint(Color.appBrightCyan) }
        }
    }
}

// MARK: - Subscription state (local for now; swap for StoreKit entitlements)

/// Tier ladder. Features gate against this via `rank` so new tiers slot in cleanly.
/// Each paid tier has an Individual (1 seat) + Family (5 seats) variant priced
/// independently; they share the same rank so Family unlocks the SAME features as the
/// matching Individual sibling.
enum SubscriptionTier: String, CaseIterable, Codable {
    case free
    case premium                // Premium Individual (1 seat)
    case family                 // Premium Family (5 seats; alias kept for Codable compat)
    case highPremium            // Pro Individual
    case highPremiumFamily      // Pro Family
    case elite                  // Elite Individual
    case eliteFamily            // Elite Family

    var displayName: String {
        switch self {
        case .free:               return "Free"
        case .premium:            return "Premium"
        case .family:             return "Premium Family"
        case .highPremium:        return "Pro"
        case .highPremiumFamily:  return "Pro Family"
        case .elite:              return "Elite"
        case .eliteFamily:        return "Elite Family"
        }
    }

    var seats: Int {
        switch self {
        case .family, .highPremiumFamily, .eliteFamily: return 5
        default: return 1
        }
    }

    /// Numeric rank for `>=` checks. Variants of the same tier share the same rank.
    var rank: Int {
        switch self {
        case .free:                                  return 0
        case .premium, .family:                      return 1
        case .highPremium, .highPremiumFamily:       return 2
        case .elite, .eliteFamily:                   return 3
        }
    }
}

@MainActor
final class SubscriptionState: ObservableObject {
    static let shared = SubscriptionState()

    @Published var isActive: Bool {
        didSet { d.set(isActive, forKey: Keys.isActive) }
    }
    @Published var tier: SubscriptionTier {
        didSet { d.set(tier.rawValue, forKey: Keys.tier) }
    }
    @Published var planName: String {
        didSet { d.set(planName, forKey: Keys.planName) }
    }
    @Published var pricingSummary: String {
        didSet { d.set(pricingSummary, forKey: Keys.pricingSummary) }
    }
    @Published var nextBillingDate: Date {
        didSet { d.set(nextBillingDate.timeIntervalSince1970, forKey: Keys.nextBillingDate) }
    }
    @Published var lastFour: String {
        didSet { d.set(lastFour, forKey: Keys.lastFour) }
    }

    /// Tester-build override. When `true`, every `has*` gate returns true
    /// regardless of the user's actual subscription state — so Internal
    /// Testing track users can exercise Elite features without paying.
    ///
    /// MUST FLIP TO `false` before promoting past Open Testing to Production,
    /// otherwise the entire monetization model collapses.
    ///
    /// TODO(launch): flip this to `false` + remove the pre-launch note from
    /// the Billing screen that mentions "tester access."
    static let TESTER_BUILD_UNLOCK_EVERYTHING = true

    /// Convenience: does the current tier include Premium-level perks?
    var hasPremium: Bool {
        Self.TESTER_BUILD_UNLOCK_EVERYTHING ||
            (isActive && tier.rank >= SubscriptionTier.premium.rank)
    }
    /// Convenience: does the current tier include every High Premium / Pro feature?
    var hasHighPremium: Bool {
        Self.TESTER_BUILD_UNLOCK_EVERYTHING ||
            (isActive && tier.rank >= SubscriptionTier.highPremium.rank)
    }
    /// Convenience: does the current tier include Elite features (hardware key, dead-man wipe,
    /// encrypted vault backup)? Elite also unlocks everything Pro ships.
    var hasElite: Bool {
        Self.TESTER_BUILD_UNLOCK_EVERYTHING ||
            (isActive && tier.rank >= SubscriptionTier.elite.rank)
    }

    let invoiceHistory: [InvoiceEntry] = [
        .init(date: Date().addingTimeInterval(-365 * 86400), description: "Premium yearly", amountCents: 499),
        .init(date: Date().addingTimeInterval(-730 * 86400), description: "Premium yearly", amountCents: 499)
    ]

    private let d = UserDefaults.standard
    private enum Keys {
        static let isActive = "subscription.v1.active"
        static let tier = "subscription.v1.tier"
        static let planName = "subscription.v1.plan_name"
        static let pricingSummary = "subscription.v1.pricing"
        static let nextBillingDate = "subscription.v1.next_billing"
        static let lastFour = "subscription.v1.last_four"
    }

    private init() {
        isActive = d.object(forKey: Keys.isActive) as? Bool ?? false
        tier = (d.string(forKey: Keys.tier).flatMap(SubscriptionTier.init(rawValue:))) ?? .free
        planName = d.string(forKey: Keys.planName) ?? "Premium"
        pricingSummary = d.string(forKey: Keys.pricingSummary) ?? "$4.99 per year"
        let timestamp = d.object(forKey: Keys.nextBillingDate) as? Double
        nextBillingDate = timestamp.flatMap { Date(timeIntervalSince1970: $0) }
            ?? Calendar.current.date(byAdding: .day, value: 335, to: Date()) ?? Date()
        lastFour = d.string(forKey: Keys.lastFour) ?? "4242"
    }

    /// Called when a successful purchase is confirmed by StoreKit (future wire-up).
    func markPurchased(tier: SubscriptionTier, plan: String, pricing: String, nextDate: Date) {
        isActive = tier != .free
        self.tier = tier
        planName = plan
        pricingSummary = pricing
        nextBillingDate = nextDate
    }

    struct InvoiceEntry: Identifiable {
        let id = UUID()
        let date: Date
        let description: String
        let amountCents: Int

        var dateString: String {
            let f = DateFormatter()
            f.dateFormat = "MMM d, yyyy"
            return f.string(from: date)
        }
        var amountString: String {
            String(format: "$%.2f", Double(amountCents) / 100.0)
        }
    }
}

// MARK: - Palette

extension Color {
    static let subGold = Color(red: 250.0/255.0, green: 189.0/255.0, blue: 61.0/255.0)
}
