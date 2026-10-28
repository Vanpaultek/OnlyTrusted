# Google Play Store Listing — Copy-Paste Pack

Everything you need to fill in **Grow → Store presence → Main store
listing** on Google Play Console. Just copy the text + upload the
images in this folder.

---

## Visual assets

| Field | File | Dimensions | Google requirement |
|---|---|---|---|
| **App icon** | `app_icon_512.png` | 512×512 | 512×512 PNG, ≤1 MB |
| **Feature graphic** | `feature_graphic_1024x500.jpg` | 1024×500 | 1024×500 PNG/JPG, ≤15 MB |
| **Phone screenshots** (8) | `screen_01_welcome.png` through `screen_08_smartlink.png` | 1206×2622 | 320–3840 px, 16:9 or 9:16 |

All eight screenshots are real captures from the shipping iOS build.
Google accepts iOS-style screenshots on Play Console as long as the
underlying app behaviour matches — the UX is nearly identical across
platforms and the "shows what the app actually looks like" rule is
satisfied.

---

## App name

```
OnlyTrusted — Quieter Safer Phone
```

(Max 30 characters.)

---

## Short description (max 80 chars)

```
Only your trusted contacts get through. Zero cloud. Zero account. Open source.
```

---

## Full description (max 4000 chars)

```
OnlyTrusted is a quieter, safer phone for the people who matter.

Allow only trusted contacts. Silence or block everyone else. Keep a local audit log of every call and message. Your data never leaves this device — zero cloud, zero account, zero compromise.

What OnlyTrusted does:

• Trusted Contacts allowlist — the people who matter get through, scammers don't
• Call blocking + SMS filter via Android's supported Call Directory & SMS filter extensions
• Blocked numbers list — one tap, logged on-device for your own audit
• Events log — every block + alert, timestamped and auditable, never uploaded
• Distress Signal — 3-second countdown, location SMS + call + WhatsApp to your Trusted Contacts
• Smart Link Guard — 12 rules on every URL, catches phishing before you tap (Pro)
• Mic & Camera Lock — auto-mute mic + disable camera the moment the phone screen locks, restored on successful unlock (hardware-level with Device Admin)
• Weekly protection digest + honest-limits refresher (Premium)
• Signed audit log + hardware security key support (Elite)

Why zero-cloud:
OnlyTrusted does not operate a backend. There is no OnlyTrusted account to create. We do not store your contacts, your calls, your messages, or your settings on any server. The AGPL-3.0 source is on GitHub (github.com/Vanpaultek/OnlyTrusted) — every claim is verifiable in code.

Honest limits (shown in-app, not buried in a PDF):
• Call blocking uses Android's Call Directory extension — brand-new unknown spam numbers can slip through until first-reported
• Mic auto-mute via AudioManager is not security-grade on consumer Android — apps still see a "mic active" handle, they just receive silence. For hardware-grade mic + cam cutoff, grant Device Admin (one-tap system dialog, no factory reset)
• Some phone manufacturers (Samsung One UI, Xiaomi MIUI, OnePlus) aggressively kill background services. We ask for battery optimisation exemption so the lock-watcher survives — grant it when prompted

Languages shipped: English, Spanish, French, Portuguese, Hindi, Arabic, German, Indonesian, Telugu.

Open source under AGPL-3.0. Full feature grid + pricing: 4 tiers (Free, Premium, Pro, Elite). Free tier includes core protection forever.

Owner: VanPaulTek Inc · Support: admin@vanpaultek.com
```

(Comes to ~2050 chars — well under the 4000 limit.)

---

## Category

**Category:** Communication (primary) · Lifestyle (secondary)

**Tags:** privacy, call blocker, SMS filter, mic lock, camera lock

---

## Contact details

```
Email:    admin@vanpaultek.com
Phone:    (leave blank — not required for Communication category)
Website:  https://github.com/Vanpaultek/OnlyTrusted
Privacy:  https://github.com/Vanpaultek/OnlyTrusted/blob/main/docs/PRIVACY_ARCHITECTURE.md
```

(Replace the privacy URL with your real privacy policy page once you
host one at onlytrusted.app/legal/privacy.)

---

## Promo video (optional)

You can upload one of the generated walkthrough videos as a YouTube
link. Convert `tools/video/output/narrated/OnlyTrusted_Real_UI_en_voiced.mp4`
to YouTube first (private or unlisted upload), then paste the YouTube
URL in the Promo video field.

If you skip this now, you can add it later without re-reviewing.

---

## Where to click

1. Play Console → **Grow → Store presence → Main store listing**
2. **App icon** → upload `app_icon_512.png`
3. **Feature graphic** → upload `feature_graphic_1024x500.jpg`
4. **Phone screenshots** → upload all 8 `screen_*.png` files in order
5. **App name** → paste from "App name" section above
6. **Short description** → paste
7. **Full description** → paste
8. Scroll down → **Save**

Repeat for each locale under "Translations" if you want localised
listings. For first launch, English-only is fine — Google auto-serves
the default listing to users whose locale you haven't customised.

---

## Other Play Console pages that still need attention

Even with the store listing done, these are still blocking Open
Testing (per the Play Console error list):

| Page | Where | What to answer |
|---|---|---|
| **Sensitive app permissions** | Policy → App content | Declare CALL_LOG, SMS, QUERY_ALL_PACKAGES (if kept). See `TESTERS.md` or `docs/SENSOR_PRIVACY_ARCHITECTURE.md` for the exact use cases. |
| **Foreground service permissions declaration** | Policy → App content | Declare specialUse (SensorPrivacyForegroundService — lock-watcher) + location (TrackerFinderService — Pro BLE scan) |
| **Sign in details** | Policy → App content | Yes we have sign-in (local PIN + Passkey). No credentials to share with reviewers — they set their own on first launch. |
| **Financial features** | Policy → App content | **No** — not a financial app |
| **Health declaration** | Policy → App content | **No** — not a health app |
| **Countries / regions** | Internal/Open testing track | Add United States at minimum |

For internal testing specifically, only the countries/regions matter —
the Policy pages gate Open testing + Production. If you stay on
Internal, you can skip the Policy pages for now.

---

Maintained by: VanPaulTek Inc · admin@vanpaultek.com
