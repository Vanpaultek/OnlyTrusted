# iOS integration — Swift

The TypeScript layer is not used on iOS; you consume the JSON directly.

## 1. Vendor the JSON files

Easiest path: copy the two directories into your iOS app's bundle.

```
YourApp/
└── Resources/
    └── Locale/
        ├── countries.json
        └── strings/
            ├── en.json
            ├── es.json
            └── …
```

Add `Resources/Locale` to the target as a **folder reference** (blue folder
icon in Xcode) so new language files picked up without re-adding.

Alternatively, add this repo as a git submodule and point the Xcode target
at `node_modules/@vanpaultek/locale/data` (or the standalone repo's `data/`).

## 2. Models

```swift
import Foundation

public struct Country: Decodable, Sendable {
    public let code: String
    public let name: String
    public let languages: [String]
    public let suggested: String
}

public enum LocaleDirectory {
    public static func load(from bundle: Bundle = .main) throws -> [String: [Country]] {
        guard let url = bundle.url(forResource: "countries", withExtension: "json",
                                   subdirectory: "Locale") else {
            throw NSError(domain: "LocaleDirectory", code: 1,
                          userInfo: [NSLocalizedDescriptionKey: "countries.json missing"])
        }
        let data = try Data(contentsOf: url)
        return try JSONDecoder().decode([String: [Country]].self, from: data)
    }

    public static func strings(for lang: String, in bundle: Bundle = .main) -> [String: String] {
        guard let url = bundle.url(forResource: lang, withExtension: "json",
                                   subdirectory: "Locale/strings"),
              let data = try? Data(contentsOf: url),
              let dict = try? JSONDecoder().decode([String: String].self, from: data)
        else { return [:] }
        return dict
    }
}
```

## 3. Translation helper (Android-compatible tokens)

```swift
import Foundation

public enum L10n {
    public static let shipped = ["en","es","fr","pt","hi","ar","de","id","te"]

    public static func t(_ lang: String, _ key: String, _ vars: [CustomStringConvertible] = []) -> String {
        let bundle = LocaleDirectory.strings(for: lang)
        let template = bundle[key] ?? LocaleDirectory.strings(for: "en")[key] ?? key
        if vars.isEmpty { return template }
        // Replace %1$s, %2$s … with positional args.
        var out = template
        for (i, v) in vars.enumerated() {
            out = out.replacingOccurrences(of: "%\(i + 1)$s", with: String(describing: v))
            out = out.replacingOccurrences(of: "%\(i + 1)$d", with: String(describing: v))
        }
        return out
    }
}
```

## 4. Region detection

```swift
import Foundation

public struct RegionSnapshot: Sendable {
    public let country: String      // ISO-3166 alpha-2, uppercased
    public let countryName: String
    public let languageTag: String  // BCP-47
    public let suggestedLanguage: String
    public let timeZoneId: String
}

public enum Region {
    public static func detect() -> RegionSnapshot {
        let country = (Locale.current.region?.identifier ?? "US").uppercased()
        let languageTag = Locale.current.identifier.replacingOccurrences(of: "_", with: "-")

        let directory = (try? LocaleDirectory.load()) ?? [:]
        let countries = directory.values.flatMap { $0 }
        let match = countries.first(where: { $0.code == country })

        return RegionSnapshot(
            country: country,
            countryName: match?.name ?? country,
            languageTag: languageTag,
            suggestedLanguage: match?.suggested ?? "en",
            timeZoneId: TimeZone.current.identifier
        )
    }
}
```

## 5. SwiftUI picker

```swift
import SwiftUI

struct LocalePicker: View {
    @State private var region: String = "Europe"
    @State private var country: String = "DE"
    @State private var language: String = "de"

    private let directory: [String: [Country]] = (try? LocaleDirectory.load()) ?? [:]

    var body: some View {
        Form {
            Picker("Region", selection: $region) {
                ForEach(directory.keys.sorted(), id: \.self) { Text($0) }
            }
            Picker("Country", selection: $country) {
                ForEach(directory[region] ?? [], id: \.code) {
                    Text($0.name).tag($0.code)
                }
            }
            .onChange(of: country) { _, new in
                let c = (directory.values.flatMap { $0 }).first(where: { $0.code == new })
                language = c?.suggested ?? "en"
            }
            Picker("Language", selection: $language) {
                ForEach(["en"] + (directory[region]?.first(where: { $0.code == country })?.languages ?? []),
                        id: \.self) { Text($0) }
            }
        }
    }
}
```

## 6. Swift Package Manager (optional)

If you prefer a proper SPM package over hand-vendored JSON, create a tiny
wrapper package that lists `data/` as a bundle resource and re-exports the
Swift helpers above. The OnlyTrusted team keeps a template at
`templates/VanPaulTekLocale.swift` — copy it into a new SPM package and
point `.binaryTarget` or `.target(resources: ...)` at this package's
`data/` directory.
