// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation
import SwiftUI

/// App-wide language picker state. Reads the 195-country directory from
/// `countries.json` bundled under `Resources/Localization/`, exposes the current
/// language choice, and injects it into SwiftUI via `.environment(\.locale, ...)`.
///
/// Persistence: the chosen BCP-47 language tag lives in UserDefaults
/// (`onlytrusted.language`). We do NOT use `UserDefaults.standard.set` on the
/// `AppleLanguages` key — that key triggers a full OS-level restart that
/// surprises users and bypasses our root-view locale environment.
///
/// English is always a selectable language regardless of the country chosen.
/// Shipped-translated locales are declared in `shippedLanguages`; picking an
/// unshipped language still sets the locale (so native number/date formatting
/// is correct) but renders a `ReviewBadge` warning that strings may fall back
/// to English.
@MainActor
final class LocaleStore: ObservableObject {

    static let shared = LocaleStore()

    private static let defaultsKey = "onlytrusted.language"

    /// Locales we actually ship translated strings for. English always first.
    let shippedLanguages: [String] = ["en", "es", "fr", "pt", "hi", "ar", "de", "id", "te"]

    struct Country: Decodable, Identifiable {
        let code: String
        let name: String
        let languages: [String]
        let suggested: String
        var id: String { code }
    }

    @Published private(set) var regions: [String: [Country]] = [:]

    @Published var region: String = "Asia" {
        didSet { if region != oldValue, let first = regions[region]?.first { countryCode = first.code } }
    }
    @Published var countryCode: String = "IN"

    @Published var language: String = UserDefaults.standard.string(forKey: defaultsKey) ?? "en" {
        didSet { UserDefaults.standard.set(language, forKey: Self.defaultsKey) }
    }

    var currentLocale: Locale { Locale(identifier: language) }

    var selectedCountry: Country? { regions[region]?.first { $0.code == countryCode } }

    /// Choices for the Language dropdown: country-advertised languages + English always.
    var languageChoices: [String] {
        var base = selectedCountry?.languages ?? []
        if !base.contains("en") { base.insert("en", at: 0) }
        var seen = Set<String>()
        return base.filter { seen.insert($0).inserted }
    }

    func hasTranslations(for code: String) -> Bool {
        shippedLanguages.contains(code)
    }

    /// Pretty name for a language code in that language (e.g. "es" → "Español").
    func displayName(for code: String) -> String {
        let locale = Locale(identifier: code)
        return locale.localizedString(forLanguageCode: code) ?? code
    }

    /// Returns true when the current locale uses right-to-left script.
    var isRTL: Bool { ["ar", "he", "fa", "ur"].contains(language) }

    /// Locale-aware NSLocalizedString replacement. SwiftUI's `Text(LocalizedStringKey)`
    /// honors the `.environment(\.locale, ...)` override automatically; raw
    /// `NSLocalizedString(_:comment:)` doesn't — it falls back to the device's
    /// system language. Call sites that need a `String` (not a `Text`) go through
    /// here so they follow the user's in-app language choice.
    ///
    /// Looks up the key in the <language>.lproj inside the main bundle. Falls
    /// back to the device's standard resolution path if the lproj isn't present
    /// (e.g. a locale we don't ship — then `Bundle.main.localizedString` picks
    /// English per the CFBundleDevelopmentRegion key).
    func localized(_ key: String) -> String {
        if let path = Bundle.main.path(forResource: language, ofType: "lproj"),
           let bundle = Bundle(path: path) {
            return bundle.localizedString(forKey: key, value: nil, table: nil)
        }
        return NSLocalizedString(key, comment: "")
    }

    private init() {
        loadDirectory()
    }

    private func loadDirectory() {
        guard
            let url = Bundle.main.url(forResource: "countries", withExtension: "json"),
            let data = try? Data(contentsOf: url),
            let parsed = try? JSONDecoder().decode([String: [Country]].self, from: data)
        else { return }
        regions = parsed
        if let first = regions[region]?.first {
            countryCode = first.code
        }
    }
}
