# OnlyTrusted — Marketing Showcase

A creative brief for ad video production. Organized as:
1. **The one-sentence pitch** — the single line everything else supports.
2. **44+ features**, each with: what the camera shows, voiceover copy,
   claim bounds (what you can/can't say legally + ethically), and the
   closest in-app screen for b-roll.
3. **Suggested cuts** — 15s, 30s, 60s, 2-min hero reel scaffolds.
4. **Brand assets inventory** — the images, fonts, palette, and
   screenshot folder.
5. **Honest-claim guardrails** — the five lines that must appear in
   any paid ad that mentions a feature we don't fully control.
6. **Localization notes** — the 9 shipped languages + which to prioritize
   for which markets.

Last updated: 2026-10-25 · covers release `v0.3.6+`

---

## 1 — The one-sentence pitch

**"OnlyTrusted is the privacy phone-shield that works entirely on your
device — no cloud, no account, no server to hack."**

Variants for A/B testing:

- "The phone-security app that doesn't need your data to protect you."
- "Scam calls blocked. Phishing links flagged. Your contacts locked. All
  on your phone — not in a cloud we could lose."
- "Zero cloud. Zero account. Zero server. All the security, none of the
  data trade."

The deliberate contrast is against competitors (Truecaller, Hiya,
Robokiller) who all require an account + upload your contacts / call
history. "On your device, not in a cloud" is OUR category.

---

## 2 — Feature inventory for ad shots

### Opening act — the hooks (first 3 seconds)

**SOS / Distress Signal** — the single most ad-shot-worthy feature.
- **Camera**: close-up of the red SOS button on Dashboard, finger
  slamming it, 3-second countdown overlay, phone dialing + Messages
  popping up with location-filled text.
- **Voiceover**: "One tap. Your location. Your trusted contacts. Phone,
  SMS, and WhatsApp — all at once."
- **Honest bound**: "Opens Messages / Phone with pre-filled recipients.
  You tap Send." We cannot silent-send on iOS by Apple policy.
- **Screen**: Dashboard → bottom-right red circle. B-roll in
  `docs/screenshots/distress_countdown.png`.

**Scam Call Blocked, Live** — hero visual.
- **Camera**: incoming call animation ("Unknown Caller"), OnlyTrusted
  silences it mid-ring, Events Log entry fades in: "Blocked spam call ·
  12:04pm." Toast: "That one's on us."
- **Voiceover**: "Catches 50 000+ known scammer numbers. Updated with
  every app release."
- **Honest bound**: "Blocks most known scam numbers. Carriers can
  bypass; iOS CallKit misses some." (Must appear as a legible fine-
  print line on screen for 2+ seconds in any paid ad.)
- **Screen**: Dashboard shows "Blocked today: N" tile ticking up.

**Phishing Link Flagged** — relatable moment.
- **Camera**: SMS arrives: "Your bank account is suspended, verify now:
  http://chasebank.security-login.com". OnlyTrusted overlays an amber
  "⚠ Danger — phishing pattern detected" badge on the message. Finger
  hovers but doesn't tap.
- **Voiceover**: "12 on-device checks against every link before you
  tap. Catches lookalike domains, hidden shortener destinations,
  Unicode spoofs."
- **Honest bound**: "On-device rules — no URL is sent to any server."
- **Screen**: SmartLinkGuard share-sheet verdict screen.

### Core free-tier features (big ad surface — same audience)

**Trusted Contacts (Vault)**
- Camera: finger adds a contact to the Vault, phone icon turns cyan.
- Voiceover: "The people who matter get through. The rest don't."
- Bound: "Trusted-only mode is an opt-in toggle, not the default."
- Screen: Contacts tab → "+ Add Contact"

**Silence calls / SMS not in Vault**
- Camera: a non-Vault number rings, phone stays silent, Events Log
  logs it with a timestamp.
- Voiceover: "Nobody but your people gets to interrupt you."
- Bound: "Silences — doesn't block. Calls go to voicemail by default."
- Screen: Dashboard → "Trusted-only mode" row with switches.

**Events Log**
- Camera: scroll through a dense Events list with icons: blocked call,
  blocked SMS, SIM-swap detected, geo-fence exited.
- Voiceover: "Every scam. Every block. Every alert. Logged."
- Bound: "100% on-device. Nothing is uploaded. Elite tier adds
  tamper-proof hash chain + Evidence Pack export."
- Screen: Log tab (bottom nav).

**Contact Safety Scan**
- Camera: Settings → Contact Safety Scan → tap "Scan contacts". A
  progress bar sweeps through 500 contacts. 3 flagged red with scam
  tags.
- Voiceover: "Finds the scammers already in your phonebook."
- Bound: "Scan runs 100% on-device. Your contact list is never
  uploaded. Scam feed is bundled."
- Screen: Settings → Contact Safety Scan.

**Breach Watch**
- Camera: email `you@example.com` resolves to "Found in 3 past
  breaches" with LinkedIn / Dropbox / Adobe shown as tiles.
- Voiceover: "Checks if your email leaked — against a database
  shipped inside the app."
- Bound: "Hash-prefix lookup on-device. Your email never leaves the
  phone."
- Screen: Settings → Breach Watch.

**Private Aliases**
- Camera: a form on a shopping site. User taps the OnlyTrusted
  share icon → "Generate alias" → a random `k8f3@dmarc.app` fills in.
  Form submits.
- Voiceover: "A fresh email every time. The retailer sees the alias,
  not your inbox."
- Bound: "You own the alias domain, OnlyTrusted doesn't operate a
  mail server. We generate; you forward via your own provider."
- Screen: Settings → Private Aliases.

### Premium tier (upsell shots — same audience that cares about
scam calls now wants more defense in depth)

**Weekly PDF Digest** — visual gold.
- Camera: a bound PDF animates open on the phone. Shows a bar chart
  "This week: 23 scams blocked, 5 phishing SMS flagged, 2 breaches
  rechecked". Header "Your week in protection".
- Voiceover: "A clean weekly report of what we stopped."
- Bound: "Generated locally. Nothing in the PDF touches our servers
  because we don't have any."
- Screen: Settings → Weekly Digest.

### Pro tier (narrative pivot — now you're protecting yourself from
sophisticated threats, not just annoying calls)

**Duress Mode** — most dramatic shot.
- Camera: Someone demands "unlock your phone". User enters a decoy
  PIN. The decoy Vault opens — empty-looking, 2 fake contacts.
  Simultaneously (not shown on screen) an SMS fires to the real
  alert contact with "I'm in trouble" + location.
- Voiceover: "Give the attacker a fake Vault. Alert your real contact
  silently."
- Bound: **This is safety-critical copy. In any ad showing this
  feature, the fine-print line "Does not substitute professional
  security help. If you're in physical danger, call emergency
  services" must appear on screen for the full duration.**
- Screen: Settings → Duress Setup.

**SIM-Swap Alarm**
- Camera: Carrier change notification: "Verizon → T-Mobile detected".
  Red alert: "Possible SIM swap. Rotate your passwords now."
- Voiceover: "Catches the attack before the attacker catches you."
- Bound: "Detects carrier change via subscription identifier. Does
  not prevent the swap — alerts you so you can react."
- Screen: Settings → SIM-Swap Alarm.

**Wangiri Protection** (one-ring scam)
- Camera: phone rings for 1.5 seconds then stops. Phone screen
  shows: "+234 ... hung up after 1s. Likely Wangiri scam — don't
  call back." Android: auto-blocked. iOS: detect-only.
- Voiceover: "One ring. Zero premium-rate charges."
- Bound: "Android: auto-blocks. iOS: detects + warns; CallKit
  doesn't let third-party apps auto-block."
- Screen: Settings → Wangiri Protection.

**Smart Link Guard** — see the hero shot above.

**Safety Check-In**
- Camera: 2am on a map. "Check in by 11pm or I'll alert my
  contacts." Timer runs out. Silent SMS fires: "Missed check-in.
  Last location: 42nd & Broadway."
- Voiceover: "You disappeared. We noticed."
- Bound: "Sends SMS via your carrier only after a missed check-in.
  Nothing happens if you check in on time."
- Screen: Settings → Safety Check-In.

**Tracker Finder** (visual gold — competitors can't do this honestly)
- Camera: user walks past their coat on a bench. OnlyTrusted
  surfaces: "AirTag ID 0xB4FA has been near you for 15 min across 3
  locations. Not from your paired devices."
- Voiceover: "Catches the AirTag that isn't yours."
- Bound: "Detects AirTag + Tile + Samsung tags + unknown BLE
  trackers. Does not reveal who placed it — that's law enforcement."
- Screen: Settings → Tracker Finder.

### Elite tier (hard-core / investigative / high-threat audience —
different ad campaign)

**Shamir Split** (recovery, hero reel)
- Camera: user picks 5 trusted friends, phone splits a key into 5
  pieces using Bluetooth. Friends' phones vibrate: "You hold 1 of 5
  shards for Alex's OnlyTrusted vault."
- Voiceover: "Lose your phone. Your friends unlock it."
- Bound: "3 of 5 friends must physically meet to recover. Not a
  cloud recovery — a peer recovery."
- Screen: Settings → Shamir Split.

**Trusted Witness** — same aesthetic as Shamir Split.
- Camera: Red dot: "Border crossing detected. Trusted Witness Alex
  must approve to re-enable Face ID."
- Voiceover: "Even if they force you to unlock — your witness
  controls the door back in."
- Bound: "Requires a Bluetooth-paired witness device within range at
  decision time."
- Screen: Settings → Trusted Witness.

**Border Mode**
- Camera: airplane icon lights up as plane lands. Phone screen
  darkens, "Border Mode active. Face ID disabled until home Wi-Fi
  re-detected or Trusted Witness approves."
- Voiceover: "Face ID off when it matters most."
- Bound: "User-triggered — opens based on your rules, not ours."
- Screen: Settings → Border Mode.

**Panic Lockdown**
- Camera: finger holds the volume-down button + power button for 2
  seconds (iOS SOS gesture). Phone reboots. Face ID is dead on
  arrival. Only PIN works.
- Voiceover: "Make Face ID die on command."
- Bound: "Coercion-defense. Won't recover without your PIN. Not a
  factory reset."
- Screen: Settings → Panic Lockdown.

**Hardware Security Key (FIDO2)**
- Camera: user taps a YubiKey against the phone. Red "Confirming
  identity" turns cyan "Verified".
- Voiceover: "The physical key that unlocks the phone that unlocks
  your life."
- Bound: "iOS: NFC only. Android: USB-C + NFC. Required for
  destructive actions in Elite."
- Screen: Settings → Hardware Key.

**Voice Biometric**
- Camera: user says a phrase. Waveform matches a baseline. "Voice
  match: 94%". Below: "Does not prove the caller is genuine. Verify
  via a trusted callback."
- Voiceover: "A match improves confidence. Nothing proves identity."
- Bound: **"Voice checks cannot prove a caller is genuinely who they
  claim. Verify sensitive requests via a trusted callback" — this
  line is required in-ad per Honest Limits.**
- Screen: Settings → Voice Biometric.

**Dead-Man Wipe**
- Camera: calendar countdown "6 days until auto-wipe". User fails
  to unlock. Day 7, phone wipes itself.
- Voiceover: "If you can't come back for it — nothing is there to
  find."
- Bound: **"Destroys your on-device vault only. Does NOT wipe
  iCloud / Google Drive / screenshots you took. Does not wipe if
  the device is off during the deadline."** This precise phrasing
  must appear.
- Screen: Settings → Dead-Man Wipe.

**Encrypted Backup + Restore from Backup**
- Camera: user exports a `.ot2bak` file to AirDrop / Nearby Share
  to their laptop. Later: new phone, restore flow, old Vault
  reappears.
- Voiceover: "You own your backup. Not us."
- Bound: "You choose where it lives. We never store it."
- Screen: Settings → Encrypted Backup.

### Honest limit shots (**must appear in long-form ad**)

**Honest Limits sheet** on Dashboard.
- Camera: slow scroll down the Honest Limits sheet. Each bullet
  fades in: "Call Shield uses supported interfaces... Cannot block
  every scam call. Auto Privacy Shield stops OnlyTrusted-owned
  sessions only. Dead-Man Wipe is on-device, not cloud. Voice
  checks are a signal, not proof."
- Voiceover: "We tell you what we can't do. Up front."
- This shot is a trust-builder. It's the differentiator vs
  competitors who overclaim.

---

## 3 — Suggested cuts

### 15-second cut (platform ads)

```
0.0 – 0.5s   Logo pop + "OnlyTrusted" wordmark
0.5 – 3.0s   Hero shot: scam call auto-blocked, Events Log entry
3.0 – 6.0s   Dashboard with blocked count ticking up ("Blocked today: 23")
6.0 – 9.0s   Smart Link Guard — amber "Danger" badge on a phishing SMS
9.0 – 12.0s  Settings → Language & Region → cascading dropdowns (shows
             internationalization — big in India / LATAM)
12.0 – 15.0s Lock screen Passkey unlock + tagline:
             "On your device. Not in our cloud. OnlyTrusted."
             + Download CTA
```

### 30-second cut (YouTube pre-roll / Facebook feed)

```
0.0 – 2.0s   Problem hook: scam call ringing, user eye-roll
2.0 – 6.0s   Hero: OnlyTrusted silences it, Events log entry
6.0 – 10.0s  Dashboard tour: Trusted Contacts, Blocked count, Scam
             Contacts tile ticking up
10.0 – 14.0s Smart Link Guard flags a phishing SMS → amber Danger
             banner
14.0 – 18.0s SOS button → 3-second countdown → Messages +
             Phone opens with location
18.0 – 22.0s Honest Limits sheet scrolling: "We tell you what we
             can't do."
22.0 – 26.0s Pricing teaser: "Free forever. Premium $4.99/mo. Family
             plans." (price in local currency)
26.0 – 30.0s Lockup: OnlyTrusted wordmark + "Zero cloud. Zero
             account. Zero compromise." + CTA
```

### 60-second cut (connected TV / high-intent audiences)

Add between the 18s and 22s mark in the 30s cut:

```
18.0 – 24.0s Pro+Elite teasers — Tracker Finder (AirTag beeping),
             SIM-Swap Alarm, Shamir Split share animation
24.0 – 30.0s Testimonial-style: "I know what my app knows about me —
             nothing. It's all on my phone."
30.0 – 36.0s Dead-Man Wipe countdown animation
36.0 – 42.0s Border Mode activation on a plane-landing moment
42.0 – 48.0s Interactive: Language picker flips English → Hindi →
             Spanish → Telugu (shows global ambition)
48.0 – 54.0s Honest Limits scroll (same as 30s cut)
54.0 – 60.0s Lockup + CTA
```

### 2-minute hero reel (website hero / investor pitch)

Full feature tour with a narrative arc:

1. **Threat setup (0–20s)** — scam call montage, phishing SMS, lost
   phone, border agent scene
2. **OnlyTrusted response (20–60s)** — one hero shot per response:
   call blocked, link flagged, SOS fired, Border Mode activated,
   Dead-Man Wipe set, Shamir Split configured
3. **Why we're different (60–90s)** — Honest Limits scroll +
   "Zero cloud" architecture explainer (diagrammatic animation of a
   phone with everything happening inside its outline)
4. **Who it's for (90–105s)** — three faces: a parent, a solo
   traveler, a journalist/dissident
5. **CTA + language flip + brand lockup (105–120s)**

---

## 4 — Brand assets inventory

### Logo + wordmark

- Primary: `ios/OnlyTrusted/Assets.xcassets/BrandLogo.imageset/`
  (iOS) + `android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`
  (Android)
- Wordmark: "**Only**Trusted" with "Only" in white + "Trusted" in
  the brand cyan `#55D9D4`
- Shield motif: cyan ring over a dark-navy shield silhouette, a
  checkmark cutaway in the shield body
- Full-bleed hero: `ios/OnlyTrusted/Assets.xcassets/SplashHero.imageset/`
  — the Midnight-palette lakescape with the shield centered + the
  wordmark baked into the mid-band

### Color palette (Midnight)

```
Background    #0B111A   (Midnight)
Elevated      #11192A   (Midnight-elevated / cards)
Accent        #55D9D4   (brand cyan)
Bright-cyan   #4FE3DF   (CTA + stat tiles)
Primary text  #FFFFFF   (ink)
Secondary     #B8C3D1   (body copy)
Tertiary      #7C8795   (caption)
Danger        #EF4444   (SOS + blocked)
Warning       #F2B347   (Honest Limits + REVIEW badge)
Success       #4FE3DF   (uses brand cyan)
```

Full reference in `DESIGN_BRIEF.md` + `ios/OnlyTrusted/Sources/Theme/`.

### Fonts

- UI font: Inter (bundled at `android/app/src/main/assets/fonts/`
  and shipped on iOS as the system SF Pro + Inter fallback)
- Numerals: SF Pro Rounded (iOS) / Inter Rounded (Android) for stat
  tiles

### Screenshot library

Pre-built screenshots suitable for ad shots are at
`docs/screenshots/`. Fresh screenshots can be regenerated via:

- iOS: `xcrun simctl io <UDID> screenshot <path>.png`
- Android: `adb -s <device> exec-out screencap -p > <path>.png`

### App Store / Play Store assets

- Icon: 1024×1024 @ `ios/OnlyTrusted/Assets.xcassets/AppIcon.appiconset/`
- Feature graphic (Play Store): 1024×500 — need to produce
- Hero screenshots (App Store): 1290×2796 (iPhone 15 Pro Max) —
  need to produce in all 9 languages for localized store listings

### Taglines (ready to use)

- "Zero cloud. Zero account. Zero compromise." (primary)
- "The phone-security app that doesn't need your data."
- "On your device. Not in our cloud."
- "Private by design. Not by promise."
- "We tell you what we can't do."

### Pricing kit

Already renders at `docs/pricing/` as a sticky-bar price-flash HTML
page. Lives at `vanpaultek.github.io/OnlyTrusted/pricing/` once
GitHub Pages serves the custom domain.

---

## 5 — Honest-claim guardrails (lawyer + ethics)

Any paid ad that mentions these features must include the exact
accompanying fine-print line on screen for 2+ seconds. Lifted from
the in-app Honest Limits sheet so there's no divergence between what
the ad promises and what the app ships.

| Ad topic | Required fine-print line |
|---|---|
| Scam call blocking | "Blocks most known scam numbers. Cannot block every scam call." |
| SMS defense | "Processes supported messages from unknown senders only. Cannot inspect your entire inbox or iMessages." |
| Privacy Shield / microphone monitoring | "Stops OnlyTrusted-owned recording sessions. Does not disable other apps' microphones or cameras." |
| Dead-Man Wipe | "Wipes your on-device vault only. Cannot wipe backups outside OnlyTrusted. Cannot run while the device is powered off." |
| Voice Biometric | "Cannot prove a caller is genuine. Verify sensitive requests via a trusted callback." |
| "No cloud" / "No server" | "OnlyTrusted operates no backend. Core functions use zero network. SMS / WhatsApp / Phone calls use your own carrier when YOU tap send." |

Also:

- **Never show OnlyTrusted blocking a call that then goes through
  anyway** — competitors have been sued for this exact
  misrepresentation. Our blocking is real but platform-constrained;
  the ad has to show the real behavior.
- **Never dub English voiceover over a Spanish/Hindi/Telugu UI
  screenshot without a title card** marking it as a localization
  demo — regulatory risk under the FTC's "deceptive translation
  practices" guidance.
- **Never use a photo of a real emergency** — Duress Mode + SOS ads
  must be clearly staged. Hire actors; put a "dramatized" label.

---

## 6 — Localization notes (which market gets which language)

| Market | Language code | Priority | Hero cuts to localize first |
|---|---|---|---|
| USA, UK, Canada, Australia, EU English | en | P0 | all 4 cuts |
| India (south) | te | P0 | 15s + 30s |
| India (north) | hi | P0 | 15s + 30s |
| Spain, Mexico, Argentina, Colombia | es | P1 | 15s + 30s |
| France, Belgium, Canada FR, Switzerland FR | fr | P1 | 30s |
| Brazil, Portugal | pt | P1 | 30s |
| MENA (Saudi, UAE, Egypt, Morocco) | ar | P1 | 30s (RTL layout matters) |
| Germany, Austria, Switzerland DE | de | P2 | 30s |
| Indonesia, Malaysia | id | P2 | 30s |

**All translations currently machine-generated by Claude Opus** and
flagged for native review per `docs/NEEDS_REVIEW.md`. Before any
PAID ad spend in a non-English market, a native speaker must sign
off on at least the Honest Limits bullets + any CTA / tagline the
ad uses. India (hi + te) is launch-market priority; a Hindi + Telugu
native-review pass is the next ad-blocking item on the roadmap.

---

## 7 — Pre-launch content pipeline (suggestion)

1. **Film the 30-second cut in English** against the current app.
   Use the exact phrasing from this doc so the voiceover ↔ fine-
   print alignment is automatic.
2. **Localize the voiceover + fine-print** into hi + te + es + fr
   + pt for the P0 + P1 markets. UI b-roll gets rerecorded on a
   simulator with `onlytrusted.language` set to each locale (see
   screenshot command above).
3. **Review each localized cut with a native speaker** before paid
   placement. Keep the uncertified cuts as organic-only (YouTube,
   TikTok uploads) with a visible "beta translation" disclaimer.
4. **Shoot the Duress Mode / SOS cuts last** — these need actors +
   a safety-disclaimer card + the professional-help fine-print. Not
   something to improvise.

---

## Appendix — in-app screens for b-roll by number

Easy reference for a video producer. The number matches the Dashboard
+ Settings order; each screen's live Compose / SwiftUI source is in
the file column.

| # | Screen | iOS source | Android source |
|---|---|---|---|
| 01 | Splash + welcome | SplashScreen.swift | OnlyTrustedApp.kt |
| 02 | Email onboarding | ProfileSetupScreen.swift | ProfileSetupScreen.kt |
| 03 | PIN setup | ProfileSetupScreen.swift | ProfileSetupScreen.kt |
| 04 | Passkey setup | ProfileSetupScreen.swift | ProfileSetupScreen.kt |
| 05 | Consent | ConsentScreen.swift | ConsentScreen.kt |
| 06 | Lock gate | AppLockGate.swift | AppLockGate.kt |
| 07 | Dashboard hero | MainTabs.swift | DashboardScreen.kt |
| 08 | Trusted Contacts | VaultScreen.swift | VaultScreen.kt |
| 09 | Blocked list | BlockListScreen.swift | BlockListScreen.kt |
| 10 | Events log | EventsScreen.swift | EventsScreen.kt |
| 11 | Settings top | SettingsScreen.swift | SettingsScreen.kt |
| 12 | Language & Region | LanguageRegionScreen.swift | LanguageRegionScreen.kt |
| 13 | Smart Link Guard verdict | SmartLinkGuardExtension/ShareViewController | SmartLinkGuardScreen.kt |
| 14 | Breach Watch result | BreachWatchScreen.swift | BreachWatchScreen.kt |
| 15 | Private Aliases | AliasesScreen.swift | AliasesScreen.kt |
| 16 | SOS countdown | DistressCountdownOverlay (MainTabs) | DistressSettingsScreen.kt |
| 17 | Shamir Split setup | ShamirSetupScreen.swift | ShamirSetupScreen.kt |
| 18 | Dead-Man Wipe countdown | (Settings → Dead-Man Wipe row) | DeadmanWipeSettingsScreen.kt |
| 19 | Honest Limits sheet | HonestLimitsSheet (MainTabs) | HonestLimitsSheet (DashboardScreen) |
| 20 | About / version | AboutScreen.swift | AboutScreen.kt |

For each screen, the live app on an iPhone 17 Pro simulator (iOS) or
Pixel 7 Pro emulator (Android) can be captured via the standard
screenshot commands at any time — the UI is deterministic.

---

Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
