# @vanpaultek/locale

Portable locale directory and 9-language string bundles extracted from
[OnlyTrusted](https://github.com/vanpaultek) by VanPaulTek Inc.

- **195 countries** across 5 regions (Africa, Asia, Europe, North & South
  America, Oceania) with per-country advertised languages and a suggested
  default.
- **9 shipped translation bundles** (en, es, fr, pt, hi, ar, de, id, te)
  derived from the Android `strings.xml` resources — 150+ keys each.
- **Platform-neutral JSON** at the core, with a tiny TypeScript helper layer
  for Node, browsers, and bundlers.

## Install

```bash
npm install @vanpaultek/locale
```

Node ≥ 20.10 required (uses standard JSON import attributes).

## Quickstart

```ts
import {
  directory,
  getCountry,
  detectRegion,
  loadStrings,
  t,
} from "@vanpaultek/locale";

// 1. Browse the directory.
console.log(Object.keys(directory));           // Africa, Asia, Europe, …
console.log(directory.Europe.length);          // 45

// 2. Look up a country.
getCountry("IN"); // { code: "IN", name: "India", languages: [...], suggested: "hi" }

// 3. Detect current region (browser) — hints override detection.
const snap = detectRegion();                   // reads navigator.language
const snapForced = detectRegion({ countryHint: "DE" });

// 4. Translate a string with Android-compatible positional args.
t("es", "alert_mic_body_with_pkg", ["Zoom", "10:14"]);
// => "Zoom abrió el micrófono a las 10:14."

// 5. Pull a whole bundle (useful for i18n libraries).
const es = loadStrings("es");
```

## What's in the package

```
@vanpaultek/locale
├── dist/                      Compiled JS + .d.ts (ESM, Node 20+ / modern browsers)
├── data/
│   ├── countries.json         The 195-country directory, region-bucketed
│   └── strings/
│       ├── en.json … te.json  One flat {key: string} map per shipped language
│       └── _review.json       Keys still carrying machine translations per locale
└── docs/
    ├── INTEGRATION_WEB.md     React/Vue/plain-JS recipes
    ├── INTEGRATION_IOS.md     SwiftUI / Swift Package consumption
    └── INTEGRATION_ANDROID.md Kotlin / Jetpack Compose consumption
```

### Raw-JSON subpath exports

If you don't want the TypeScript helpers (e.g. in Python, Swift, Go) just
read the JSON directly — the package exposes it as a subpath export:

```ts
import countries from "@vanpaultek/locale/countries.json" with { type: "json" };
import en from "@vanpaultek/locale/strings/en.json" with { type: "json" };
```

For non-Node environments, fetch the files from `node_modules` or vendor
them into your repo.

## API

| Function | Purpose |
|---|---|
| `directory` | `Record<Region, Country[]>`, loaded from JSON |
| `countries` | Flat array of all 195 countries |
| `getCountry(code)` | Case-insensitive lookup by ISO-3166 alpha-2 |
| `regionOf(code)` | Which of the 5 regions contains this country |
| `languagesFor(code)` | All languages the country advertises |
| `suggestedLanguageFor(code)` | Picker default (fallback `"en"`) |
| `detectRegion(opts?)` | Best-effort browser/Node region detection |
| `loadStrings(lang)` | Full `{key: string}` bundle; falls back to English |
| `t(lang, key, vars?)` | Translate one key; supports `%1$s`/`%2$s` tokens |
| `hasTranslations(lang)` | True for the 9 shipped locales |
| `reviewPending(lang)` | Keys still carrying `[REVIEW]` machine translations |
| `SHIPPED_LANGUAGES` | `["en","es","fr","pt","hi","ar","de","id","te"]` |

## Integration guides

- [Web (React / Vue / vanilla)](./docs/INTEGRATION_WEB.md)
- [iOS (Swift Package)](./docs/INTEGRATION_IOS.md)
- [Android (Kotlin)](./docs/INTEGRATION_ANDROID.md)

## Updating translations

The source of truth for strings is the Android `res/values-*/strings.xml`
tree in the OnlyTrusted repo. To regenerate the JSON bundles after
translation updates:

```bash
npm run extract:android
```

## License

AGPL-3.0-or-later. For commercial / closed-source redistribution reach out
to admin@vanpaultek.com.
