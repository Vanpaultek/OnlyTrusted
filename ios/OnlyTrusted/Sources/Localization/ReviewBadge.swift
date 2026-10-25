// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI

/// Strips the `[REVIEW]` sentinel from a translated string and, when found,
/// renders a small amber warning caption beneath. Use this wrapper around any
/// `Text(LocalizedStringKey)` that might resolve to a safety-critical string
/// (SOS / duress / legal / pricing / system alerts) in a non-English locale.
///
/// English source strings are never tagged, so this is a no-op for English.
struct ReviewBadge: View {

    let key: LocalizedStringKey
    var style: Font = .body
    var weight: Font.Weight = .regular

    private static let sentinel = "[REVIEW] "

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(stripped)
                .font(style)
                .fontWeight(weight)
            if isReview {
                Text(LocalizedStringKey("language_pending_review"))
                    .font(.caption2)
                    .foregroundStyle(Color(red: 0.95, green: 0.70, blue: 0.28))
            }
        }
    }

    private var resolved: String {
        // SwiftUI's LocalizedStringKey → localised String resolution path. We
        // rebuild it manually because String(localized:) is iOS 15+ and gives us
        // the actual resolved string so we can strip the sentinel.
        let mirror = Mirror(reflecting: key)
        let keyName = mirror.descendant("key") as? String ?? "\(key)"
        return NSLocalizedString(keyName, comment: "")
    }

    private var isReview: Bool { resolved.hasPrefix(Self.sentinel) }
    private var stripped: String {
        isReview ? String(resolved.dropFirst(Self.sentinel.count)) : resolved
    }
}
