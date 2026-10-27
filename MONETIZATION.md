# OnlyTrusted — Monetization & Distribution

Reference for Google Play Console + App Store Connect configuration. Reflects
what's shipping as of `v0.3.4` (commit `6c750a6`). Pricing + feature list are
authoritative here — the in-app `UpgradeToPremiumScreen` (both platforms) and
the `SubscriptionTier` enums are kept in sync with this doc on every release.

**App type:** Free-to-download privacy & security app with freemium subscription
tiers. All download channels (Play Store, App Store, F-Droid, direct APK) are
**free**. Revenue comes from in-app auto-renewing subscriptions.

**Honest-claim preservation:** No subscription unlocks any cloud / server / network
feature — every tier is 100% on-device. The paid tiers unlock additional on-device
features (Pro: duress mode, tracker finder, etc.; Elite: hardware key, dead-man
wipe, signed audit log, etc.). The zero-network charter holds across every tier.

---

## Pricing matrix

| Tier | Monthly | Yearly | Yearly savings vs monthly | Per-member/mo (Family only) |
|---|---:|---:|---:|---:|
| **Free** | $0 | $0 | — | — |
| **Premium Individual** | $0.99 | $4.99 | 58% off | — |
| **Premium Family** (5 seats) | $1.99 | **$9.99** | 58% off | $0.17/mo |
| **Pro Individual** | $2.99 | $19.99 | 44% off | — |
| **Pro Family** (5 seats) | $4.99 | **$29.99** | 50% off | $0.50/mo |
| **Elite Individual** | $3.99 | $29.99 | 37% off | — |
| **Elite Family** (5 seats) | $7.99 | **$49.99** | 48% off | $0.83/mo |

Family variants pay once, cover **up to 5 household members** via Google Family
Library / Apple Family Sharing. Feature-gating is rank-based: Family unlocks
the exact same features as the matching Individual tier, just shared across the
household.

---

## Feature cascade

Features cascade upward — Pro includes everything in Premium; Elite includes
everything in Pro. Family variants of each tier unlock the exact same features
as the Individual variant.

### Free — $0 forever
- Trusted contacts allowlist
- Call blocking + SMS filtering (block list)
- Blocking & Unknown screen (silence unknowns, call/text handling preferences)
- Data Removal Scan (visibility-only — flags you at 50+ people-search sites)
- Private Aliases (unlimited local alias generation)
- Breach Watch (1 email)
- Contact Safety Scan (checks your contacts against bundled scam-number feed)
- Scam SMS Scan (Android; iOS can't read SMS inbox)
- App Lock, Pocket-dial detection, Screenshot protection
- Panic Lock + Data Export (JSON/CSV)
- Honest Limits callout
- Emergency Contact + Distress Signal (shake / volume-down 4× / in-app SOS → auto SMS + dialer + WhatsApp)

### Premium — $4.99/yr Individual · $9.99/yr Family
Everything in Free, plus:
- Privacy Shield (mic/camera use alerts for OnlyTrusted-owned captures)
- Breach Watch (up to 5 emails)
- Private Aliases (CSV backup)
- Identity Protection + yearly renewal reminders (credit freeze, fraud alert, etc.)
- Data Removal 90-day re-check reminders
- Weekly PDF digest
- Quiet hours + area-code allowlist (Advanced Blocking)

### Pro — $19.99/yr Individual · $29.99/yr Family
Everything in Premium, plus:
- Duress Mode + decoy vault
- SIM-Swap Alarm + Wangiri (one-ring) protection
- App Permission Watchdog
- Tracker Finder (AirTag, Tile, SmartTag detection)
- Smart Link Guard (SMS + QR + share-sheet URL scanner)
- Safety Check-In

### Elite — $29.99/yr Individual · $49.99/yr Family
Everything in Pro, plus:
- App Shield (block any app behind biometric; iOS adds Family Controls + Shortcuts "Shield apps on lock" automation)
- Border Mode preset + Panic Lockdown
- Hardware Security Key + Signed Audit Log
- Encrypted vault backup + Shamir Split + Trusted Witness
- Dead-Man Wipe + Geo-fence Auto-Lock
- Face-Change Lock + Voice Biometric
- Steganographic Vault + Rogue Network Scanner + Offline Threat Intel

---

## Google Play distribution

**Package name:** `com.onlytrusted.app`
*(permanent — can never be changed after first upload)*

**App type at creation:** `App` (not `Game`)

**Pricing model at creation:** `Free`
*(this is irreversible — pick wisely; "Free" allows in-app purchases, "Paid" does not and can't switch back)*

**Signing fingerprint for Play App Signing enrollment:**
```
SHA-256: 33:86:D9:84:1C:6C:09:36:0D:D1:81:5C:51:4D:B0:B1:60:E5:72:8E:E6:7A:CF:7C:BD:67:35:0D:EC:FE:AE:4D
SHA-1:   C2:9E:73:09:3B:AA:DE:A7:FD:14:52:2A:A2:B5:15:2A:01:69:33:E6
```
*(This is the upload key. The private key lives at `~/Documents/OnlyTrusted-signing/release.jks` + is backed up at `~/Documents/OnlyTrusted-signing-backup.dmg` + stored in the Mac Passwords.app under "OnlyTrusted Android Upload Keystore".)*

### Subscription SKUs to configure in Play Console → Monetization → Subscriptions

Each row is a **Base Plan** inside a **Subscription Group**. The yearly plan should be the default for each group (better presentation + usually what you want to show first in the StoreKit sheet).

| Subscription Group | Base Plan ID | Price | Family Library eligible |
|---|---|---:|:---:|
| **OnlyTrusted Premium** | `premium_monthly` | $0.99/mo | — |
| | `premium_yearly` *(default)* | $4.99/yr | — |
| **OnlyTrusted Premium Family** | `family_monthly` | $1.99/mo | ✓ |
| | `family_yearly` *(default)* | $9.99/yr | ✓ |
| **OnlyTrusted Pro** | `pro_monthly` | $2.99/mo | — |
| | `pro_yearly` *(default)* | $19.99/yr | — |
| **OnlyTrusted Pro Family** | `pro_family_monthly` | $4.99/mo | ✓ |
| | `pro_family_yearly` *(default)* | $29.99/yr | ✓ |
| **OnlyTrusted Elite** | `elite_monthly` | $3.99/mo | — |
| | `elite_yearly` *(default)* | $29.99/yr | — |
| **OnlyTrusted Elite Family** | `elite_family_monthly` | $7.99/mo | ✓ |
| | `elite_family_yearly` *(default)* | $49.99/yr | ✓ |

**12 SKUs total** (6 tiers × monthly + yearly cadences).

### Deep-link URLs

Shareable — opens the subscribe sheet directly in Play Store on any Android device with the app installed (or the install prompt if not):

```
# Install the app:
https://play.google.com/store/apps/details?id=com.onlytrusted.app

# Open specific subscribe sheets:
https://play.google.com/store/account/subscriptions?sku=premium_yearly&package=com.onlytrusted.app
https://play.google.com/store/account/subscriptions?sku=family_yearly&package=com.onlytrusted.app
https://play.google.com/store/account/subscriptions?sku=pro_yearly&package=com.onlytrusted.app
https://play.google.com/store/account/subscriptions?sku=pro_family_yearly&package=com.onlytrusted.app
https://play.google.com/store/account/subscriptions?sku=elite_yearly&package=com.onlytrusted.app
https://play.google.com/store/account/subscriptions?sku=elite_family_yearly&package=com.onlytrusted.app

# Open all active OnlyTrusted subscriptions (lets user manage/cancel):
https://play.google.com/store/account/subscriptions?package=com.onlytrusted.app
```

### Revenue split

**85% to you / 15% to Google** on the first $1M/year per developer; **70/30** above $1M. *(Google Play Service Fee changes April 2026 — verify current rate at play.google.com/console at time of launch.)*

---

## Apple App Store distribution

**Bundle ID:** `com.onlytrusted.app`
*(matches Android package name for consistency — permanent in App Store Connect)*

**App listing URL** (assigned after first submission):
```
https://apps.apple.com/app/id<APPLE_APP_ID>
```

### StoreKit product IDs to configure in App Store Connect → Features → In-App Purchases → Subscriptions

| Subscription Group | Product ID | Price | Family Sharing |
|---|---|---:|:---:|
| **OnlyTrusted Premium** | `com.onlytrusted.app.premium.monthly` | $0.99/mo | — |
| | `com.onlytrusted.app.premium.yearly` | $4.99/yr | — |
| **OnlyTrusted Premium Family** | `com.onlytrusted.app.family.monthly` | $1.99/mo | ✓ |
| | `com.onlytrusted.app.family.yearly` | $9.99/yr | ✓ |
| **OnlyTrusted Pro** | `com.onlytrusted.app.pro.monthly` | $2.99/mo | — |
| | `com.onlytrusted.app.pro.yearly` | $19.99/yr | — |
| **OnlyTrusted Pro Family** | `com.onlytrusted.app.pro_family.monthly` | $4.99/mo | ✓ |
| | `com.onlytrusted.app.pro_family.yearly` | $29.99/yr | ✓ |
| **OnlyTrusted Elite** | `com.onlytrusted.app.elite.monthly` | $3.99/mo | — |
| | `com.onlytrusted.app.elite.yearly` | $29.99/yr | — |
| **OnlyTrusted Elite Family** | `com.onlytrusted.app.elite_family.monthly` | $7.99/mo | ✓ |
| | `com.onlytrusted.app.elite_family.yearly` | $49.99/yr | ✓ |

### Deep-link URLs

Apple does NOT expose a per-SKU subscribe URL — the subscription sheet opens from inside the app's StoreKit flow only. What you CAN share:

```
# App listing (after approval):
https://apps.apple.com/app/id<APPLE_APP_ID>

# Manage all iOS subscriptions (opens iOS Settings → Apple ID → Subscriptions):
https://apps.apple.com/account/subscriptions
```

### Revenue split

- **Default:** 70/30 for year 1 of each subscriber, then **85/15** after 12 months of continuous subscription.
- **Small Business Program:** flat **85/15 from day one** while annual proceeds are under $1M (strongly recommended — saves ~$0.75 per $5 subscription in year 1).

---

## Alternative distribution channels

### F-Droid (optional, trust-maximizing)

F-Droid builds the APK from source + signs with their own key, so users don't have to trust the maintainer's build. Trade-offs:
- Requires an F-Droid metadata YAML in a separate repo.
- F-Droid refuses apps that include proprietary SDKs — OnlyTrusted is clean here (no analytics, no ads).
- No in-app purchase mechanism — Premium/Pro/Elite would need a separate activation flow (license key purchased externally).

### Direct APK (website download)

The GitHub Release pipeline already produces signed AABs with SLSA attestation. For direct distribution:
1. Convert AAB → APK via `bundletool build-apks`.
2. Serve from `vanpaultek.com/download` with SHA-256 checksum + attestation URL alongside.
3. Users need to enable "Install from unknown sources".

Doesn't support in-app purchases — same caveat as F-Droid.

### TestFlight (iOS beta channel)

Up to 10,000 external testers for 90 days per build. Requires an Apple Developer account.

---

## Playbook — launch sequence

1. **Create Google Play Console app** → set type **App** → set pricing **Free** → declare "contains in-app purchases".
2. **Create App Store Connect app** → same category selection.
3. **Enroll in Google Play App Signing** with the upload-key fingerprint above.
4. **Enroll in Apple Small Business Program** → gets you the 85/15 split from day one (saves ~$0.75 per $5 subscription in year 1).
5. **Configure the 12 subscription SKUs on each store** per the tables above.
6. **Submit AAB** from a `v0.3.x` GitHub Release (signed, attested, SBOM-attached) to Play Console internal track.
7. **Submit IPA** — needs iOS CI signing wired first (see `SECURITY.md` → Reproducible builds + the open installer-sprint TODO list).

### Expected timeline

- Play Store **internal track → closed testing**: ~1–2 days once AAB uploaded
- Play Store **production**: ~3–7 days after you promote from closed testing
- Apple App Store: ~1–7 days review per submission

---

## Where this doc is authoritative vs. not

**Authoritative here** (safe to edit this file, code will NOT auto-sync — update both):
- Price points per tier
- Tier naming + feature inclusion rules
- SKU IDs (Google Play) + Product IDs (App Store)
- Public-facing deep-link URLs
- Revenue-split expectations
- Launch-sequence playbook

**Authoritative in code** (don't rely on this doc for the ground truth):
- Feature gating → `android/.../ui/subscription/SubscriptionState.kt` + `ios/.../Subscription/BillingSubscriptionScreen.swift` (SubscriptionTier enum)
- In-app upgrade copy → `android/.../ui/subscription/UpgradeToPremiumScreen.kt` + `ios/.../Subscription/UpgradeToPremiumScreen.swift`
- Signing certificate → `~/Documents/OnlyTrusted-signing/release.jks` (keystore) + GitHub Actions Secrets (`ANDROID_KEYSTORE_BASE64` + 3 others)

---

Last updated: 2026-10-24 (release `v0.3.4`, commit `6c750a6`).

Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
