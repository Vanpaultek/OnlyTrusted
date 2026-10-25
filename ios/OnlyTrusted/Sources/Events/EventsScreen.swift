// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI

/// Activity Log — matches the new neon mockup.
/// Large bold title, 4 filter chips (All/Calls/Texts/Blocked), large day-group headings
/// (Today/Yesterday/This week/Earlier), each event rendered in its own standalone card
/// with a solid-filled colored icon chip, title+subtitle, and cyan timestamp.
struct EventsScreen: View {
    @State private var rows: [SecurityEventRecord] = []
    @State private var chainOk: Bool? = nil
    @State private var filter: EventFilter = .all

    private var filteredRows: [SecurityEventRecord] {
        filter.filter(rows)
    }

    private var grouped: [(ActivityGroup, [SecurityEventRecord])] {
        let bucketed = Dictionary(grouping: filteredRows) { ActivityGroup.bucket(for: $0.timestampMillis) }
        return ActivityGroup.allCases.compactMap { group in
            guard let items = bucketed[group], !items.isEmpty else { return nil }
            return (group, items.sorted { $0.timestampMillis > $1.timestampMillis })
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 12) {
                headerTitle
                filterChips
            }
            .padding(.horizontal, 18)
            .padding(.top, 6)
            .padding(.bottom, 10)
            .background(
                ZStack {
                    Color.appScreenBackground
                    Color.black.opacity(0.25)
                }
                .ignoresSafeArea(edges: .top)
            )

            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    if filteredRows.isEmpty {
                        chainIntegrityCardIfTampered
                        emptyState
                    } else {
                        chainIntegrityCardIfTampered
                        ForEach(grouped, id: \.0) { group, dayRows in
                            EventDaySection(title: group.title, rows: dayRows)
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
        }
        .appBackdrop()
        .navigationBarHidden(true)
        .task { reload() }
    }

    // MARK: - Header title row

    private var headerTitle: some View {
        HStack(spacing: 10) {
            Text("events_title")
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundStyle(Color.appPrimaryText)
            Spacer()
            Button(action: reload) {
                Image(systemName: "arrow.clockwise")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Color.appBrightCyan)
                    .frame(width: 36, height: 36)
                    .background(
                        Circle().fill(Color.appElevatedSurface)
                    )
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Reload event log")
        }
    }

    // MARK: - Filter chips

    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(EventFilter.allCases, id: \.self) { f in
                    FilterChip(
                        label: f.title,
                        isSelected: filter == f,
                        onTap: { filter = f }
                    )
                }
            }
            .padding(.horizontal, 2)
            .padding(.vertical, 4)
        }
    }

    // MARK: - Chain integrity (only shown when tampered)

    @ViewBuilder
    private var chainIntegrityCardIfTampered: some View {
        if chainOk == false {
            HStack(spacing: 14) {
                ZStack {
                    Circle().fill(Color.appDanger.opacity(0.18))
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(Color.appDanger)
                }
                .frame(width: 44, height: 44)
                .decorativeAccessibility()

                VStack(alignment: .leading, spacing: 2) {
                    Text("events_audit_tampered")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.appPrimaryText)
                    Text("events_audit_tampered_detail")
                        .font(.footnote)
                        .foregroundStyle(Color.appSecondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 0)
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color.appElevatedSurface)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(Color.appDanger.opacity(0.5), lineWidth: 1)
                    )
            )
        }
    }

    // MARK: - Empty state

    @ViewBuilder
    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer(minLength: 32)
            ZStack {
                Circle().fill(Color.appAccent.opacity(0.14))
                    .frame(width: 88, height: 88)
                Image(systemName: "clock.badge.checkmark.fill")
                    .font(.system(size: 36, weight: .regular))
                    .foregroundStyle(Color.appAccent)
            }
            .decorativeAccessibility()
            Text("events_no_events")
                .font(.title3.weight(.semibold))
                .foregroundStyle(Color.appPrimaryText)
            Text(filter == .all
                 ? "Blocked calls, flagged SMS, panic locks, and vault edits will appear here as they happen."
                 : "No events match the \"\(filter.title)\" filter yet.")
                .font(.subheadline)
                .foregroundStyle(Color.appSecondaryText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 20)
        }
        .frame(maxWidth: .infinity)
    }

    private func reload() {
        let recent = EventStore.shared.allRecent()
        rows = recent
        Task.detached(priority: .utility) {
            let ok = EventStore.shared.verifyChain()
            await MainActor.run { chainOk = ok }
        }
    }
}

// MARK: - Filter chip (bigger rounded-square per mockup)

enum EventFilter: CaseIterable, Hashable {
    case all, calls, texts, blocked

    var title: String {
        switch self {
        case .all: "All"
        case .calls: "Calls"
        case .texts: "Texts"
        case .blocked: "Blocked"
        }
    }

    func filter(_ rows: [SecurityEventRecord]) -> [SecurityEventRecord] {
        switch self {
        case .all:
            return rows
        case .calls:
            return rows.filter { $0.type.lowercased().contains("call") }
        case .texts:
            return rows.filter {
                let t = $0.type.lowercased()
                return t.contains("sms") || t.contains("text")
            }
        case .blocked:
            return rows.filter { EventCategory.of($0.type) == .blocked }
        }
    }
}

private struct FilterChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(isSelected ? Color.appScreenBackground : Color.appPrimaryText)
                .padding(.horizontal, 22)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(isSelected ? Color.appBrightCyan : Color.clear)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(
                                    isSelected ? Color.clear : Color.appBrightCyan.opacity(0.35),
                                    lineWidth: 1
                                )
                        )
                )
                .shadow(color: isSelected ? Color.appBrightCyan.opacity(0.45) : .clear,
                        radius: isSelected ? 10 : 0, x: 0, y: 0)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

// MARK: - Day grouping

enum ActivityGroup: CaseIterable, Hashable {
    case today, yesterday, thisWeek, earlier

    var title: String {
        switch self {
        case .today: "Today"
        case .yesterday: "Yesterday"
        case .thisWeek: "This week"
        case .earlier: "Earlier"
        }
    }

    static func bucket(for timestampMillis: Int64) -> ActivityGroup {
        let date = Date(timeIntervalSince1970: Double(timestampMillis) / 1000.0)
        let cal = Calendar.current
        let startOfToday = cal.startOfDay(for: Date())
        let startOfRow = cal.startOfDay(for: date)
        let dayDiff = cal.dateComponents([.day], from: startOfRow, to: startOfToday).day ?? 0
        if dayDiff <= 0 { return .today }
        if dayDiff == 1 { return .yesterday }
        if dayDiff < 7 { return .thisWeek }
        return .earlier
    }
}

// MARK: - Semantic category for icon + tint

enum EventCategory {
    case blocked, allowed, silenced, system

    static func of(_ type: String) -> EventCategory {
        let t = type.lowercased()
        if t.contains("silenced") { return .silenced }
        if t.contains("block") || t.contains("flag") || t == "wipe" || t.contains("panic") {
            return .blocked
        }
        if t.contains("allow") { return .allowed }
        return .system
    }
}

// MARK: - Day section (large header + list of standalone rows)

private struct EventDaySection: View {
    let title: String
    let rows: [SecurityEventRecord]

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.system(size: 22, weight: .bold, design: .rounded))
                .foregroundStyle(Color.appPrimaryText)

            VStack(spacing: 10) {
                ForEach(rows, id: \.id) { row in
                    EventRow(record: row)
                }
            }
        }
    }
}

// MARK: - F7 Report-scam share wrapper

private struct ActivityShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Event row (solid-filled icon chip + title + subtitle + cyan timestamp)

private struct EventRow: View {
    let record: SecurityEventRecord

    @State private var showScamShare = false
    @State private var scamShareItems: [Any] = []

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return f
    }()

    var body: some View {
        HStack(spacing: 14) {
            // Solid-filled colored icon chip (rounded square with glow)
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(typeTint)
                    .shadow(color: typeTint.opacity(0.5), radius: 10, x: 0, y: 2)
                Image(systemName: typeIcon)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(.white)
            }
            .frame(width: 48, height: 48)
            .decorativeAccessibility()

            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(humanLabel(record.type))
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Color.appPrimaryText)
                    if trustedName != nil {
                        // F3 — small "Trusted" chip when the number resolves to a vault entry.
                        Text("Trusted")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(Color.appScreenBackground)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.appBrightCyan, in: Capsule())
                    }
                }
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appSecondaryText)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
            Spacer(minLength: 8)

            Text(Self.timeFormatter.string(from: Date(timeIntervalSince1970: Double(record.timestampMillis) / 1000.0)))
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.appBrightCyan)
                .monospacedDigit()
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color.appElevatedSurface)
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color.appBrightCyan.opacity(0.1), lineWidth: 1)
                )
        )
        // F7 — Long-press on a blocked log entry triggers the Report-Scam share sheet.
        .contextMenu {
            if category == .blocked {
                Button {
                    presentScamShare()
                } label: {
                    Label("Report scam", systemImage: "exclamationmark.bubble.fill")
                }
            }
        }
        .sheet(isPresented: $showScamShare) {
            ActivityShareSheet(items: scamShareItems)
        }
    }

    private func presentScamShare() {
        let text = ReportScam.buildMessage(
            number: extractedNumber ?? "",
            timestamp: record.timestampMillis
        )
        var items: [Any] = [text]
        // Pre-fill the carrier spam shortcode as a sms: URL so iOS surfaces Messages at
        // the top of the share sheet. The user still picks the destination.
        if let url = URL(string: "sms:\(ReportScam.usSmsShortcode)&body=\(text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")") {
            items.append(url)
        }
        scamShareItems = items
        showScamShare = true
    }

    private var subtitle: String {
        if let name = trustedName { return name }
        if let d = record.detail, !d.isEmpty { return d }
        return "Unknown number"
    }

    /// Vault lookup — nil if the detail doesn't match any trusted contact.
    private var trustedName: String? {
        CallerLookup.contactNameForNumber(record.detail)
    }

    private var extractedNumber: String? {
        guard let d = record.detail, !d.isEmpty else { return nil }
        let ns = d as NSString
        let r = NSRange(location: 0, length: ns.length)
        if let regex = try? NSRegularExpression(pattern: "\\+?\\d{7,}"),
           let m = regex.firstMatch(in: d, range: r) {
            return ns.substring(with: m.range)
        }
        return nil
    }

    private var category: EventCategory { EventCategory.of(record.type) }

    private var typeIcon: String {
        switch record.type {
        case SecurityEventRecord.Kind.callBlocked: return "nosign"
        case SecurityEventRecord.Kind.callAllowed: return "phone.badge.checkmark"
        case SecurityEventRecord.Kind.smsFlagged:  return "message.badge.filled.fill"
        case SecurityEventRecord.Kind.callDirectoryReloaded: return "arrow.triangle.2.circlepath"
        case SecurityEventRecord.Kind.panicLock:   return "lock.fill"
        case SecurityEventRecord.Kind.vaultEdit:   return "person.text.rectangle.fill"
        case SecurityEventRecord.Kind.wipe:        return "trash.fill"
        default:
            switch category {
            case .blocked:  return "nosign"
            case .allowed:  return "phone.badge.checkmark"
            case .silenced: return "phone.badge.minus"
            case .system:   return "info.circle.fill"
            }
        }
    }

    /// Solid tint for the icon chip. Per mockup: blocked = red, silenced = amber,
    /// allowed-call = teal, allowed-text = blue, system = brand accent.
    private var typeTint: Color {
        let t = record.type.lowercased()
        if t.contains("silenced") { return Color.blockingAmber }
        if category == .blocked   { return Color.appDanger }
        if category == .allowed {
            if t.contains("sms") || t.contains("text") {
                return Color(red: 0.22, green: 0.56, blue: 0.95) // blue
            }
            return Color.appAccent // teal for allowed call
        }
        return Color.appAccent
    }

    private func humanLabel(_ type: String) -> String {
        switch type {
        case SecurityEventRecord.Kind.callBlocked: return "Blocked call"
        case SecurityEventRecord.Kind.callAllowed: return "Allowed call"
        case SecurityEventRecord.Kind.smsFlagged:  return "Blocked text"
        case SecurityEventRecord.Kind.callDirectoryReloaded: return "Call directory refreshed"
        case SecurityEventRecord.Kind.panicLock:   return "Panic lock engaged"
        case SecurityEventRecord.Kind.vaultEdit:   return "Vault edited"
        case SecurityEventRecord.Kind.wipe:        return "Data wiped"
        default: return type.replacingOccurrences(of: "_", with: " ").capitalized
        }
    }
}
