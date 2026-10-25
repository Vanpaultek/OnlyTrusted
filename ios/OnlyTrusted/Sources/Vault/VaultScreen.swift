// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI
import UIKit

/// Trusted Contacts list — matches mockup #5.
/// Header "+" opens add flow. Search bar filters by name or phone. Three tab chips
/// (All / Family / Work) show live counts. Each row has a tappable call icon,
/// message icon, and an overflow menu (Edit / Delete). Empty state prompts the
/// user to add their first contact.
struct VaultScreen: View {
    @StateObject private var store = VaultStore.shared
    @State private var query: String = ""
    @State private var selectedTab: ContactTab = .all
    @State private var showAdd = false
    @State private var editing: UUID?
    @State private var pendingDelete: MaskedContact?

    var body: some View {
        ZStack {
            if store.contacts.isEmpty && query.isEmpty {
                emptyState
            } else {
                content
            }
        }
        .appBackdrop()
        .faceLockShield()
        .navigationTitle("Trusted Contacts")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.appScreenBackground, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showAdd = true
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(Color.appScreenBackground)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(Color.appBrightCyan))
                        .shadow(color: Color.appBrightCyan.opacity(0.45), radius: 10, x: 0, y: 0)
                        .decorativeAccessibility()
                }
                .accessibilityLabel("Add trusted contact")
                .minimumHitTarget()
            }
        }
        .task { await store.reload() }
        .sheet(isPresented: $showAdd) {
            NavigationStack { VaultEditScreen(contactId: nil, store: store) }
        }
        .sheet(item: Binding(
            get: { editing.map(IdentifiableUUID.init) },
            set: { editing = $0?.value }
        )) { wrap in
            NavigationStack { VaultEditScreen(contactId: wrap.value, store: store) }
        }
        .confirmationDialog(
            pendingDelete.map { "Delete \($0.displayName)?" } ?? "Delete contact?",
            isPresented: Binding(
                get: { pendingDelete != nil },
                set: { if !$0 { pendingDelete = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                if let c = pendingDelete {
                    try? store.delete(id: c.id)
                }
                pendingDelete = nil
            }
            Button("Cancel", role: .cancel) { pendingDelete = nil }
        }
    }

    // MARK: - Sections

    private var content: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 16) {
                SearchField(text: $query)
                TabChipRow(
                    selected: $selectedTab,
                    all: store.contacts.count,
                    family: tabCount(.family),
                    work: tabCount(.work)
                )

                let visible = filtered
                if visible.isEmpty {
                    PremiumCard {
                        VStack(spacing: 10) {
                            Image(systemName: "magnifyingglass")
                                .font(.title2)
                                .foregroundStyle(Color.appTertiaryText)
                            Text("vault_no_matches")
                                .font(.headline)
                                .foregroundStyle(Color.appPrimaryText)
                            Text(query.isEmpty
                                 ? "No contacts in this tab yet."
                                 : "No contacts match \u{201C}\(query)\u{201D}.")
                                .font(.subheadline)
                                .foregroundStyle(Color.appSecondaryText)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                    }
                } else {
                    PremiumCard {
                        VStack(spacing: 0) {
                            ForEach(Array(visible.enumerated()), id: \.element.id) { idx, contact in
                                ContactRow(
                                    contact: contact,
                                    onCall: { placeCall(contact) },
                                    onMessage: { sendSMS(contact) },
                                    onEdit: { editing = contact.id },
                                    onDelete: { pendingDelete = contact }
                                )
                                if idx < visible.count - 1 {
                                    Divider()
                                        .background(Color.appElevatedSurface)
                                        .padding(.leading, 60)
                                }
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 32)
        }
    }

    private var emptyState: some View {
        VStack(spacing: 18) {
            Spacer(minLength: 24)
            PremiumCard {
                VStack(spacing: 16) {
                    ZStack {
                        Circle()
                            .fill(Color.appAccent.opacity(0.14))
                            .frame(width: 88, height: 88)
                        Image(systemName: "person.2.circle.fill")
                            .font(.system(size: 42, weight: .light))
                            .foregroundStyle(Color.appAccent)
                    }
                    .decorativeAccessibility()
                    Text("vault_no_contacts_yet")
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(Color.appPrimaryText)
                    Text("vault_tap_plus")
                        .font(.subheadline)
                        .foregroundStyle(Color.appSecondaryText)
                        .multilineTextAlignment(.center)
                    Button {
                        showAdd = true
                    } label: {
                        Label("vault_add_contact", systemImage: "plus")
                            .font(.body.weight(.semibold))
                            .frame(maxWidth: .infinity, minHeight: 44)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Color.appAccent)
                    .controlSize(.large)
                }
                .frame(maxWidth: .infinity)
            }
            Spacer()
        }
        .padding(.horizontal, 20)
    }

    // MARK: - Filtering

    private var filtered: [MaskedContact] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        return store.contacts.filter { contact in
            selectedTab.includes(relation: contact.relation) &&
                matchesQuery(contact, query: trimmed)
        }
    }

    private func matchesQuery(_ contact: MaskedContact, query: String) -> Bool {
        guard !query.isEmpty else { return true }
        if contact.displayName.range(of: query, options: .caseInsensitive) != nil { return true }
        let digits = query.filter { $0.isNumber || $0 == "+" }
        if !digits.isEmpty,
           contact.normalisedNumbers.contains(where: { $0.contains(digits) }) {
            return true
        }
        return contact.normalisedNumbers.contains(where: { $0.range(of: query, options: .caseInsensitive) != nil }) ||
            contact.maskedNumbers.contains(where: { $0.range(of: query, options: .caseInsensitive) != nil })
    }

    private func tabCount(_ tab: ContactTab) -> Int {
        store.contacts.filter { tab.includes(relation: $0.relation) }.count
    }

    // MARK: - Actions

    private func placeCall(_ contact: MaskedContact) {
        guard let raw = contact.normalisedNumbers.first,
              let url = URL(string: "tel://\(raw)") else { return }
        UIApplication.shared.open(url)
    }

    private func sendSMS(_ contact: MaskedContact) {
        guard let raw = contact.normalisedNumbers.first,
              let url = URL(string: "sms:\(raw)") else { return }
        UIApplication.shared.open(url)
    }
}

private enum ContactTab: Hashable {
    case all, family, work

    var title: String {
        switch self {
        case .all: return "All"
        case .family: return "Family"
        case .work: return "Work"
        }
    }

    func includes(relation: String?) -> Bool {
        switch self {
        case .all: return true
        case .family: return (relation ?? "").caseInsensitiveCompare("Family") == .orderedSame
        case .work:
            let r = relation ?? ""
            return r.caseInsensitiveCompare("Work") == .orderedSame ||
                r.caseInsensitiveCompare("Colleague") == .orderedSame
        }
    }
}

private struct IdentifiableUUID: Identifiable {
    let value: UUID
    var id: UUID { value }
    init(_ value: UUID) { self.value = value }
}

// MARK: - Search field

private struct SearchField: View {
    @Binding var text: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color.appTertiaryText)
                .decorativeAccessibility()
            TextField("", text: $text, prompt:
                Text("vault_search_placeholder")
                    .foregroundColor(Color.appTertiaryText)
            )
            .textFieldStyle(.plain)
            .foregroundStyle(Color.appPrimaryText)
            .autocorrectionDisabled(true)
            .textInputAutocapitalization(.never)
            .submitLabel(.search)
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Color.appTertiaryText)
                        .decorativeAccessibility()
                }
                .accessibilityLabel("Clear search")
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(
            Capsule(style: .continuous)
                .fill(Color.appElevatedSurface)
        )
    }
}

// MARK: - Tab chips

private struct TabChipRow: View {
    @Binding var selected: ContactTab
    let all: Int
    let family: Int
    let work: Int

    var body: some View {
        HStack(spacing: 10) {
            chip(.all, count: all)
            chip(.family, count: family)
            chip(.work, count: work)
            Spacer(minLength: 0)
        }
    }

    private func chip(_ tab: ContactTab, count: Int) -> some View {
        let isSelected = selected == tab
        return Button {
            selected = tab
        } label: {
            Text("\(tab.title) (\(count))")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(isSelected ? Color.white : Color.appSecondaryText)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(
                    Capsule(style: .continuous)
                        .fill(isSelected ? Color.appAccent : Color.appElevatedSurface)
                )
                .overlay(
                    Capsule(style: .continuous)
                        .strokeBorder(
                            isSelected ? Color.clear : Color.appTertiaryText.opacity(0.3),
                            lineWidth: 1
                        )
                )
                .shadow(
                    color: isSelected ? Color.appAccent.opacity(0.45) : .clear,
                    radius: isSelected ? 10 : 0,
                    x: 0,
                    y: 0
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(tab.title), \(count) contacts")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

// MARK: - Contact row

private struct ContactRow: View {
    let contact: MaskedContact
    let onCall: () -> Void
    let onMessage: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            AvatarCircle(initial: initial)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(contact.displayName)
                        .font(.body.weight(.semibold))
                        .foregroundStyle(Color.appPrimaryText)
                        .lineLimit(1)
                    if contact.isOwner {
                        Text("vault_owner_tag")
                            .font(.caption2.weight(.bold))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Capsule().fill(Color.appAccent.opacity(0.18)))
                            .foregroundStyle(Color.appAccent)
                    }
                }
                Text(subtitle)
                    .font(.footnote)
                    .foregroundStyle(subtitleIsCategory ? Color.appBrightCyan : Color.appSecondaryText)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)

            Button(action: onCall) {
                Image(systemName: "phone.fill")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(Color.appBrightCyan)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
                    .decorativeAccessibility()
            }
            .accessibilityLabel("Call \(contact.displayName)")
            .disabled(contact.normalisedNumbers.isEmpty)
            .opacity(contact.normalisedNumbers.isEmpty ? 0.4 : 1)

            Button(action: onMessage) {
                Image(systemName: "ellipsis.message.fill")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(Color.appBrightCyan)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
                    .decorativeAccessibility()
            }
            .accessibilityLabel("Message \(contact.displayName)")
            .disabled(contact.normalisedNumbers.isEmpty)
            .opacity(contact.normalisedNumbers.isEmpty ? 0.4 : 1)

            Menu {
                Button { onEdit() } label: { Label("Edit", systemImage: "pencil") }
                Button(role: .destructive) { onDelete() } label: { Label("Delete", systemImage: "trash") }
            } label: {
                Image(systemName: "ellipsis.vertical")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(Color.appBrightCyan)
                    .frame(width: 36, height: 44)
                    .contentShape(Rectangle())
                    .decorativeAccessibility()
            }
            .accessibilityLabel("More options for \(contact.displayName)")
        }
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }

    private var subtitle: String {
        if let r = contact.relation, !r.isEmpty { return r }
        return contact.maskedNumbers.first ?? "No phone"
    }

    private var subtitleIsCategory: Bool {
        guard let r = contact.relation?.lowercased() else { return false }
        return ["family", "work", "medical", "other", "colleague"].contains(r)
    }

    private var initial: String {
        contact.displayName.first.map { String($0).uppercased() } ?? "?"
    }
}

private struct AvatarCircle: View {
    let initial: String

    var body: some View {
        ZStack {
            Circle()
                .fill(Color.appAccent.opacity(0.18))
            Text(initial)
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Color.appAccent)
        }
        .frame(width: 46, height: 46)
        .decorativeAccessibility()
    }
}
