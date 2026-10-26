# OnlyTrusted — Pricing kit (branded)

Rebranded version of the stock pricing-page kit, themed to match OnlyTrusted's
shipping Midnight palette + lakescape backdrop + brand-logo tile. Hosts as a
static site, or embeds inside the app via WKWebView / Android WebView.

## What's in here

| File | Purpose |
|---|---|
| `index.html` | Landing page: hero, pricing cards (7 variants), feature comparison, FAQ, footer. Pulls the OnlyTrusted brand name + logo + lakescape backdrop. |
| `styles.css` | OnlyTrusted "Midnight Cybersecurity" theme — color tokens pulled 1:1 from `android/.../theme/Color.kt` and `ios/.../Theme/Theme.swift`. Backdrop image set via `body::before`. |
| `app.js` | Pricing state machine (Individual / Family × Monthly / Yearly). Fires native purchase bridges on tap (iOS: `webkit.messageHandlers.onlyTrustedPurchase`, Android: `OnlyTrustedAndroid.purchase`). Falls back to a toast in plain browser previews. |
| `plans.json` | Canonical pricing data + Apple StoreKit product IDs + proposed Google Play SKU mapping. **This is the shared source of truth for both platforms + MONETIZATION.md** — if prices change, change here first. |
| `assets/brand_logo.png` | Real OnlyTrusted brand-logo tile (cyan shield + wordmark on midnight). Same asset used by the Android launcher and the Dashboard Protection card. |
| `assets/app_backdrop.webp` | Real OnlyTrusted lakescape backdrop (305 KB WebP). Same asset used by `AppBackdrop` on Android. |
| `integrations/OnlyTrustedPaywallView.swift` | Illustrative StoreKit 2 + WKWebView bridge for embedding inside the iOS app. |
| `integrations/AndroidPurchaseBridge.kt` | Illustrative Google Play Billing JavaScript bridge for embedding inside the Android app. |

## Preview locally

```bash
cd docs/pricing
python3 -m http.server 8000
# then open http://localhost:8000 in a browser
```

The purchase buttons will show an informational toast — real checkout only runs
inside the native app via StoreKit / Google Play Billing.

## Three ways to use this

### 1. Static landing page (vanpaultek.com/pricing)

Rsync the entire folder to any static host (Vercel, Netlify, Cloudflare Pages,
GitHub Pages). All paths are relative; no build step. The purchase buttons
redirect to the respective app-store listing deep link via the fallback toast
if visited outside the app.

### 2. Embedded in-app paywall (iOS WKWebView)

1. Drag `docs/pricing/` into the Xcode project as a **folder reference** (not
   group) named e.g. `PricingPages`.
2. Add the Swift wrapper from `integrations/OnlyTrustedPaywallView.swift`.
3. Load `Bundle.main.url(forResource: "PricingPages/index", withExtension: "html")`
   into a WKWebView with the `onlyTrustedPurchase` message handler attached.
4. On message receive → unlock the entitlement, verify the StoreKit receipt
   server-side (if applicable), then dismiss.

### 3. Embedded in-app paywall (Android WebView)

1. Copy everything in `docs/pricing/` into `android/app/src/main/assets/pricing/`.
2. Load `file:///android_asset/pricing/index.html` into a WebView.
3. Register the `OnlyTrustedAndroid` JavaScript interface from
   `integrations/AndroidPurchaseBridge.kt`.
4. Allow-list navigation strictly to local assets so the WebView can never load
   an attacker-controlled page that calls the purchase bridge.
5. On interface call → launch Google Play Billing with the queried
   `ProductDetails` + offer token.

## Native-UI alternative

The in-app Upgrade screens (`UpgradeToPremiumScreen.kt` + `.swift`) already
render the same data natively in Compose / SwiftUI. If App Store Review flags
the WebView-based paywall (increasingly common in 2026), ship the native
screens instead — the data model + visual language are identical.

## Keeping prices in sync

If you update a tier price, update **all three** of these in the same commit:

- `docs/pricing/plans.json` — this file (web paywall + the authoritative data source)
- `android/.../UpgradeToPremiumScreen.kt` — `Plan.priceLabel(cadence)` function
- `ios/.../UpgradeToPremiumScreen.swift` — `Plan.priceLabel(for:)` method
- `MONETIZATION.md` (repo root) — the public-facing pricing matrix
- `app.js` here — the inline `PLANS` constant (currently hand-mirrors `plans.json`)

## Store SKU truth

The `plans.json` → `googlePlayMapping` is **proposed** — the actual SKU IDs
must be configured in Google Play Console → Monetization → Subscriptions and
confirmed there. The `appleProductIds` are the StoreKit product identifiers
to create in App Store Connect → Features → In-App Purchases → Subscriptions.
See `MONETIZATION.md` at the repo root for the full launch playbook.

## Honest-claim preservation

Nothing in this kit reaches a VanPaulTek server. The purchase bridge calls into
Apple StoreKit / Google Play Billing — the user's own store accounts, not ours.
The pages themselves can be served from any static host, or bundled entirely
offline inside the app. The zero-cloud / zero-network OnlyTrusted charter
holds.

---

Last updated: 2026-10-24 (release `v0.3.4`, commit `6c750a6`).

Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
