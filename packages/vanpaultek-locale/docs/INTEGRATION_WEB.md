# Web integration — React, Vue, and vanilla JS

```bash
npm install @vanpaultek/locale
```

## 1. Country/region picker (React)

```tsx
import { useState, useMemo } from "react";
import {
  directory,
  type Region,
  type CountryCode,
  suggestedLanguageFor,
  getCountry,
} from "@vanpaultek/locale";

export function LocalePicker() {
  const regions = Object.keys(directory) as Region[];
  const [region, setRegion] = useState<Region>("Europe");
  const [country, setCountry] = useState<CountryCode>("DE");
  const [language, setLanguage] = useState("de");

  const countries = directory[region];
  const languages = useMemo(() => {
    const base = getCountry(country)?.languages ?? [];
    return base.includes("en") ? base : ["en", ...base];
  }, [country]);

  return (
    <form>
      <select value={region} onChange={(e) => setRegion(e.target.value as Region)}>
        {regions.map((r) => <option key={r}>{r}</option>)}
      </select>

      <select
        value={country}
        onChange={(e) => {
          const c = e.target.value;
          setCountry(c);
          setLanguage(suggestedLanguageFor(c));
        }}
      >
        {countries.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
      </select>

      <select value={language} onChange={(e) => setLanguage(e.target.value)}>
        {languages.map((l) => <option key={l}>{l}</option>)}
      </select>
    </form>
  );
}
```

## 2. Auto-detect on first load

```ts
import { detectRegion } from "@vanpaultek/locale";

const snap = detectRegion();
// { country: "US", region: "North & South America", languageTag: "en-US",
//   suggestedLanguage: "en", timeZoneId: "America/Los_Angeles", ... }
```

Pair it with your server-side IP lookup if you have one:

```ts
const snap = detectRegion({ countryHint: userGeo?.country });
```

## 3. Using the translation bundles

The simplest path — plug into `i18next` or `react-i18next`:

```ts
import i18next from "i18next";
import { loadStrings, SHIPPED_LANGUAGES } from "@vanpaultek/locale";

i18next.init({
  lng: "en",
  fallbackLng: "en",
  resources: Object.fromEntries(
    SHIPPED_LANGUAGES.map((l) => [l, { translation: loadStrings(l) }])
  ),
  interpolation: { prefix: "%", suffix: "$s" }, // Android-style tokens
});
```

Or use the bundled `t()` helper directly:

```ts
import { t } from "@vanpaultek/locale";

t("fr", "alert_mic_title");                       // "Le micro est actif"
t("en", "alert_mic_body_with_pkg", ["Zoom", "10:14"]);
```

## 4. Bundler notes

- **Vite / Next 14+ / Webpack 5** — works out of the box, JSON is tree-shaken.
- **Webpack 4 or CRA** — add `"resolveJsonModule": true` to `tsconfig.json`;
  JSON imports work, the `with { type: "json" }` attribute is optional.
- **Edge runtimes** (Vercel, Cloudflare Workers) — ESM-only, fully supported.

## 5. SSR / server use

```ts
// Next.js server component or API route
import { directory, detectRegion } from "@vanpaultek/locale";

export async function GET(req: Request) {
  const countryHint = req.headers.get("x-vercel-ip-country") ?? undefined;
  const snap = detectRegion({ countryHint });
  return Response.json({ snap, availableLanguages: directory });
}
```

## 6. Keep your page size down

If you only need the country directory and not the 9 translation bundles,
import only what you use — tree-shaking drops the unused bundles:

```ts
import { getCountry, suggestedLanguageFor } from "@vanpaultek/locale";
// loadStrings/t are not imported → their JSON bundles never ship
```

Or import the raw JSON directly to skip the helpers entirely:

```ts
import countries from "@vanpaultek/locale/countries.json";
```
