// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import SwiftUI

/// Settings → Language & Region. Region → Country → Language cascading pickers.
/// English is always offered regardless of country. Shows a review-badge caption
/// when the chosen language falls outside our shipped-translated allow-list.
struct LanguageRegionScreen: View {

    @StateObject private var store = LocaleStore.shared

    var body: some View {
        Form {
            Section {
                Text(LocalizedStringKey("settings_language_region_subtitle"))
                    .font(.body)
                Text(LocalizedStringKey("language_english_always"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Section(header: Text(LocalizedStringKey("language_region_header"))) {
                Picker("", selection: $store.region) {
                    ForEach(store.regions.keys.sorted(), id: \.self) { region in
                        Text(region).tag(region)
                    }
                }
                .pickerStyle(.menu)
            }

            Section(header: Text(LocalizedStringKey("language_country_header"))) {
                Picker("", selection: $store.countryCode) {
                    ForEach(store.regions[store.region] ?? []) { country in
                        Text(country.name).tag(country.code)
                    }
                }
                .pickerStyle(.menu)
            }

            Section(header: Text(LocalizedStringKey("language_language_header"))) {
                Picker("", selection: $store.language) {
                    ForEach(store.languageChoices, id: \.self) { code in
                        Text(store.displayName(for: code)).tag(code)
                    }
                }
                .pickerStyle(.menu)

                Text(String(format: NSLocalizedString("language_selected_locale", comment: ""), store.language))
                    .font(.caption)
                    .foregroundStyle(.secondary)

                if !store.hasTranslations(for: store.language) {
                    HStack(spacing: 6) {
                        Image(systemName: "exclamationmark.triangle.fill")
                        Text(LocalizedStringKey("language_pending_review"))
                            .fontWeight(.semibold)
                    }
                    .font(.caption)
                    .foregroundStyle(Color(red: 0.95, green: 0.70, blue: 0.28))
                }

                Text(LocalizedStringKey("language_restart_note"))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle(LocalizedStringKey("settings_language_region"))
        .environment(\.locale, store.currentLocale)
        .environment(\.layoutDirection, store.isRTL ? .rightToLeft : .leftToRight)
    }
}
