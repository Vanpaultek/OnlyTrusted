# OnlyTrusted — User Guide

Last updated: 2026-10-25 · covers release `v0.3.4`

Welcome. This is the complete, end-to-end guide to every OnlyTrusted feature,
how to find it, how to use it, and what it does under the hood. Written for
you — the person using the app — not for developers.

---

## Table of contents

- [What OnlyTrusted is](#what-onlytrusted-is)
- [The honest promise](#the-honest-promise)
- [First-time setup](#first-time-setup)
- [Dashboard tour](#dashboard-tour)
- [Free tier — included forever](#free-tier--included-forever)
- [Premium tier](#premium-tier)
- [Pro tier](#pro-tier)
- [Elite tier](#elite-tier)
- [Settings — every toggle explained](#settings)
- [Platform differences (iOS vs Android)](#platform-differences)
- [Honest Limits — what the app can't do](#honest-limits)
- [Subscription management](#subscription-management)
- [Troubleshooting + FAQ](#troubleshooting--faq)
- [Appendix: glossary](#appendix-glossary)

---

## What OnlyTrusted is

OnlyTrusted is a cross-platform (iOS + Android) privacy and personal-security
app with one defining architectural principle: **nothing it does requires an
account, a server, or network traffic to a VanPaulTek-controlled endpoint**.

Every scan, every encryption key, every pattern match, every alert happens on
your own device. The scam-phone-number list that catches a bank-impersonator
call is bundled inside the app — it's not fetched. The breach-check for your
email runs against a hashed database shipped with the app — your email never
leaves your phone. The distress-SMS you fire during an emergency goes out
through your own cellular carrier — not through our servers (we have none).

**Shipped in 8 languages** (English, Spanish, French, Portuguese, Hindi,
Arabic, German, Indonesian) as of v0.3.4+. The language picker lives at
Settings → Language & Region. Non-English translations are machine-generated
and tagged for review; see the Language & Region section below for the
honest-note on translation quality.

If you can't audit this claim from the publicly available source code at
[github.com/Vanpaultek/OnlyTrusted](https://github.com/Vanpaultek/OnlyTrusted),
please open an issue. The whole value proposition hinges on it.

---

## The honest promise

Before you depend on OnlyTrusted for any serious safety situation, read
these three statements carefully. They're the ones we commit to never
breaking:

1. **We send zero network traffic from the app for its core functions.** The
   app's `AndroidManifest.xml` requests no `INTERNET` permission for the
   main process and iOS has no `App Transport Security` exception entries
   for any domain. The only time your device talks to a network because
   of OnlyTrusted is when you yourself tap a share-sheet button to send
   something via SMS / WhatsApp / email — and even then, those are the
   OS's own channels, not ours.

2. **We have no server, no cloud, no account to sign up for.** There is no
   "forgot password" link because there is no account. All your trusted
   contacts, your event log, your encrypted backups — they live on your
   device, encrypted at rest with keys stored in iOS Keychain / Android
   Keystore. Losing your device means losing the data unless you've
   backed up to your own storage.

3. **We document what we can't do.** Any screen that promises something
   OnlyTrusted can't technically deliver on your particular OS version is
   either removed or flagged with an "Honest Limits" note. For example,
   iOS doesn't let third-party apps read your SMS inbox, so the "Scam SMS
   Scan" feature is Android-only, and we say so. We will never market a
   feature we can't ship.

If any of these ever become untrue, the publicly visible git history will
show the exact commit that broke the promise.

---

## First-time setup

The first time you launch OnlyTrusted you'll go through four screens:

### Step 1 — Welcome / Splash

A short animated splash with the OnlyTrusted shield logo over the Midnight
lakescape backdrop. Takes about 1.4 seconds. Shows while the app loads
your (empty, this first time) encrypted stores off the main thread so the
UI never freezes.

### Step 2 — Your email

Enter the email address you'd want contacted if your device sends an
emergency distress signal, and the email you want monitored for data
breaches (Free tier watches 1 email; Premium watches up to 5).

The email is **stored on this device only**. We don't verify it, don't
send a confirmation link, don't log it anywhere outside your phone's
encrypted iOS Keychain / Android EncryptedSharedPreferences. If you
mistype, you can fix it later under Settings → Account.

Tap **Continue** when the field has a sensible-looking email in it.

### Step 3 — Set a 4-digit PIN

Pick a 4-digit number you'll remember. OnlyTrusted uses this PIN as:
- The lock gate when you reopen the app
- The derivation input for the AES-256-GCM key that encrypts your vault
  (via PBKDF2-HMAC-SHA256 at 100 000 iterations — slow enough that a
  brute-force attacker can't try all 10 000 combinations instantly)

Enter the PIN. Enter it again on the confirmation screen. Tap **Continue**.

**Important:** we cannot recover your PIN. There is nowhere to recover
it from. If you forget it, your only paths back in are:
- Your Passkey (Face ID / Touch ID / fingerprint — if you set it up in
  Step 4)
- Shamir Split recovery (Elite tier, if you shared recovery pieces with
  trusted friends in advance)
- Wiping the app and starting over (loses all data)

### Step 4 — Set up your Passkey

Tap **Set up Passkey**. Face ID / Touch ID (iOS) or your fingerprint
sensor (Android) becomes your **primary unlock** — tap the icon, scan,
you're in. No password to type, no email code to wait for, no account to
compromise. The credential lives in the Secure Enclave (iOS) or
StrongBox / Keystore (Android) — biometric never leaves this device.

**Hardware-bound (Phase 2, shipping as of v0.3.5+).** Enrolling creates
an EC-256 keypair inside the Secure Enclave / StrongBox hardware chip
that wraps your vault's master key. The OS kernel refuses to perform
the key-agreement operation needed to unwrap the vault without a fresh
biometric assertion in the same unlock session. Clearing or re-enrolling
your device biometric invalidates the keypair (the `.biometryCurrentSet`
ACL) so an attacker who physically acquires the device and registers
their own face can't use it to open your vault.

Phishing-proof by design: there's no server to phish and no code to
intercept. The PIN set in Step 3 remains as a fallback for the moments
biometric can't be presented (dirty finger, bright light defeating Face
ID, injured hand).

Tap **Use PIN only** if you'd rather skip Passkey setup — you can
enable it later from Settings → Account → Biometric unlock.

See `docs/AUTH_MODEL.md` for the full architecture, the deliberate
rejection of email-code login, and the dual-wrap migration plan for
replacing the legacy Keychain-only fallback path in a future release.

### Step 5 — Consent

A plain-text privacy statement + a checkbox confirming you've read it +
a tap-to-agree button. This is where OnlyTrusted repeats the "no server,
no cloud, no account" promise and gets your explicit acknowledgment.

Tap **I understand**.

You're in. The Dashboard loads.

---

## Dashboard tour

The Dashboard is the home screen you see on every app reopen (after
unlocking). It has eight areas, top to bottom:

### 1. Cinematic header
Branded header with the OnlyTrusted wordmark, Settings gear icon
(top-right), and a time-aware greeting (`Good morning,` / `Good evening,`
based on your clock) + `Welcome 👋`. Behind it is a scaled-down version
of the lakescape backdrop + a glowing cyan brand-shield so the Dashboard
has strong brand recognition even at a glance.

### 2. Monthly reminder banner (orange info card)
Once a month, a small orange card reminds you of the key limitation:
*"This app cannot monitor other apps' microphone or camera use."* with
**Learn more** / **Dismiss** buttons. Tap **Learn more** to open the
Honest Limits sheet (same content as below in this guide).

### 3. Protection card (big cyan card)
A prominent card showing one of three states:

- **Protection is active** (cyan, glowing border): You have at least one
  trusted contact AND the "Silence calls not in vault" toggle is on. The
  subtitle reads: *"Only trusted contacts can reach you."*
- **Almost there** (cyan border, dimmed): You have at least one trusted
  contact but the "Silence calls not in vault" toggle is still off. The
  subtitle tells you exactly which toggle to flip.
- **Setup needed** (grey border): No trusted contacts yet. The subtitle
  reads: *"Add trusted contacts to begin."*

Tap the card to jump to the Contacts (Vault) tab.

### 4. Stats row (three tiles)
Three equally-sized tiles side by side:

- **Trusted Contacts** — count of people in your vault.
- **Blocked** — big number of calls/SMS blocked **today**, with
  `Today · 0 this week` subtitle showing weekly rolling total.
- **Scam Contacts** — count of your system contacts flagged by the last
  Contact Safety Scan. Icon turns red when > 0.

### 5. Quick Actions row (three tiles)
Three tap-targets for the most common actions:

- **Emergency Contact** — opens Vault edit screen to add a new trusted
  contact. Subtitle: *"Manual or pick from Contacts"* (lets you add
  manually or pick from your system address book).
- **Block Number** — opens the Block List screen to add a number you
  want silenced going forward.
- **View Log** — opens the Events Log tab showing every blocked call,
  flagged SMS, and security event the app has recorded.

### 6. Honest Limits callout (orange)
Expandable info card titled **Honest Limits** with a short preamble + a
chevron. Tap to open the sheet listing every real OS-level limitation
in plain English — see the [Honest Limits](#honest-limits) section below.

### 7. Red SOS button (floating, bottom-right)
Big red circular floating-action button in the bottom-right corner.
Tap to fire the Distress Signal: 3-second cancel countdown → SMS to
all trusted contacts with your location → phone dialer opens to the
first contact. See the [Distress Signal](#distress-signal--sos)
section below for the full flow.

### 8. Bottom navigation bar
Five tabs across the bottom edge:

- **Home** — the Dashboard (what you're on now)
- **Contacts** — the Trusted Contacts vault
- **Blocked** — the Block List + Blocking & Unknown preferences
- **Log** — the Events Log (chronological security audit)
- **Settings** — all configuration + tier-gated features

---

## Free tier — included forever

Everything in this section works on the Free tier and continues to work
if you ever downgrade. No paywall, no "trial" wording — this is the
baseline OnlyTrusted ships for everyone.

### Trusted Contacts (Vault)

**Where:** Contacts tab (bottom nav), or Dashboard → Emergency Contact
Quick Action.

**What it does:** A locally-stored allowlist of people whose calls you
want to always get through, and whose SMS you want to always see. Everyone
else can be silenced (via the Blocking & Unknown toggle). The vault is
also where the Distress Signal looks for who to notify when you fire
the SOS button.

**How to use:**
1. Tap **Contacts** on the bottom nav
2. Tap the **+** button (top-right of the list) or **Add Contact** CTA
3. Choose:
   - **Enter manually** — type name + phone number(s). You can add email,
     notes, and a label (Family / Friend / Doctor / Work / etc.).
   - **Pick from Contacts** — grants one-time read access to your system
     address book + opens the native contact picker. **OnlyTrusted stores
     only the one contact you pick** (name + numbers + the system
     `LOOKUP_KEY` so updates to the system contact flow through), never
     your full address book.
4. Tap **Save**.

The contact appears in the list. On the Dashboard, the Trusted Contacts
stat tile increments.

**Behavior once added:**
- Any incoming call from that number rings through (even if "Silence
  calls not in vault" is on).
- SMS from that number arrives normally (no silencing).
- The Distress Signal will include this number as a recipient.
- The contact appears in the Call Handling + Text Handling allowlist.

**Edit / remove:** tap the contact in the list → **Edit** or **Delete**.
Delete requires PIN confirmation on Elite tier if Trusted Witness is set
up (prevents coercion).

### Call & SMS Blocking (base level)

**Where:**
- To view what's been blocked: Log tab (bottom nav) → filter by `Call
  blocked` or `SMS flagged`.
- To add a specific number to the block list: Blocked tab or Dashboard
  → **Block Number** Quick Action.
- To configure blocking rules: Settings → **Blocking & Unknown**.

**What it does:**
- Silence or outright block incoming calls from numbers you add to the
  block list.
- Flag SMS from blocked numbers (iOS auto-sorts into Junk via
  `MessageFilterExtension`; Android silences via a priority
  `SmsReceiver`).
- Optionally silence ALL calls from numbers not in your Trusted Contacts
  vault (the main gating toggle).

**How to add a number to the block list:**
1. Tap **Blocked** on the bottom nav
2. Tap the **+** button
3. Enter the number (format `+15551234567` or `555-123-4567` both OK)
4. Choose: **Block calls**, **Block SMS**, or **both**
5. Optionally add a label (e.g. "Spam caller")
6. Tap **Save**

The number is now in your block list. All future calls/SMS from it get
silenced or dropped. The Log tab will show a record each time it fires.

**Platform-specific detail (Android):** OnlyTrusted uses
`CallScreeningService` which requires being set as the device's default
call-screening app. On first-time use the system will prompt you to
grant this role — tap **Yes, use OnlyTrusted**. Without the role,
blocking still logs the call but can't silence the ring.

**Platform-specific detail (iOS):** OnlyTrusted ships a Call Directory
Extension that iOS loads at install time. iOS calls into it on every
incoming call to decide whether to label it as spam. Your block list is
synced into the extension automatically. The extension never has
network access — it's local-only.

### Events Log (security audit trail)

**Where:** Log tab (bottom nav).

**What it does:** Chronological, searchable, filterable log of every
security event OnlyTrusted has observed on your device:
- `Call blocked / silenced` (number, timestamp, duration until hangup)
- `SMS flagged` (sender, timestamp, trigger pattern)
- `Permission granted / revoked` to any app (Pro tier via App
  Permission Watchdog)
- `SIM-Swap detected` (Pro tier)
- `Wangiri (one-ring) detected` (Pro tier)
- `Distress Signal fired` (recipient count, location-attached y/n)
- `App unlocked / locked`
- `Dead-Man Wipe armed / disarmed / fired` (Elite)
- `Geo-fence exited` (Elite)
- `Panic Lockdown activated / disarmed` (Elite)
- `Vault backup created / restored` (Elite)
- + ~20 other event types

**How to use:**
- Scroll the list to see recent events (newest first).
- Tap any event to see full details (location coordinates, trigger
  context, hash chain for Elite-tier Signed Audit Log).
- Use the filter button (top-right) to filter by event type or time
  range.
- Use the search button to search by phone number, SMS content snippet,
  or event tag.

**Elite tier:** every event is cryptographically signed by your Hardware
Security Key (if enrolled) + Merkle-hashed so any tampering with the
historical log breaks the chain and can be proven in court. See
[Signed Audit Log](#signed-audit-log-elite) below.

### Contact Safety Scan

**Where:** Settings → **Contact Safety Scan**.

**What it does:** Cross-checks every phone number in your system
contacts against the bundled scam-numbers feed (currently 50k+ phishing
hosts + a curated list of government-reported scam phone number ranges).
Flags matches with a red warning triangle. Great for catching "I saved
this number ages ago from a cold call and now I don't remember why" —
and realizing it was a known scammer.

**How to use:**
1. Open **Settings** (bottom nav) → tap **Contact Safety Scan**
2. First-time only: tap **Grant Contacts access** → **Allow** in the OS
   prompt. OnlyTrusted uses this to read contact names + numbers for
   comparison; nothing is uploaded.
3. Tap **Scan contacts for scams**
4. The scanner runs in a few seconds. Results appear below:
   - *"No scam numbers found"* (green banner) — your contacts are clean
   - *"X suspicious contact(s) found"* (red banner) + a list of each
     flagged contact with the number + the match reason

**On the Dashboard:** the **Scam Contacts** stat tile reflects the last
scan's result. Red triangle if > 0.

**Honest claim:** scan runs 100% on-device. Your contact list is never
uploaded. The scam-feed is bundled in the APK/IPA (see `MONETIZATION.md`
→ Threat Intel section for the feed's update cadence).

### Scam SMS Scan (Android only)

**Where:** Settings → **Scam SMS Scan**.

**What it does:** Scans your entire SMS inbox against:
- Known-scam sender numbers (bundled feed)
- Phishing URLs in message bodies (bundled 50k+ host feed)
- Classic scam phrases ("Your bank account is suspended", "URGENT:
  wire transfer required", "You've won a lottery", etc.)

Reports: number of messages scanned, number flagged as suspicious, and
per-message details showing which rule matched.

**Why Android only:** iOS blocks third-party apps from reading the SMS
inbox entirely. There's no API — Apple reserves it for iOS Mail and
Messages themselves. For iOS, OnlyTrusted uses a `MessageFilterExtension`
that pre-screens unknown-sender SMS before delivery (a different
approach — it can't retroactively scan what's already in your inbox).

**How to use (Android):**
1. Settings → **Scam SMS Scan**
2. Tap **Grant SMS access** → **Allow** in the OS prompt
3. Tap **Scan my SMS**
4. Results appear with per-message flag details

### Distress Signal / SOS

**Where:**
- **Dashboard** — red circular floating button, bottom-right corner
- **Volume-down 4×** (Android only, opt-in) — press volume-down 4 times
  in 2 seconds from any screen while OnlyTrusted is foreground
- **Shake 5× in 3 seconds** (both platforms, opt-in)
- Configuration: Settings → **Distress Signal**

**What it does:** Multi-recipient emergency broadcast:
1. 3-second cancel countdown appears (prevents accidental fires)
2. Best-effort one-shot location fetch (3-second timeout — falls back
   to "Location: unavailable" rather than blocking the SMS)
3. OS SMS composer opens with **every emergency contact** pre-selected
   and the body pre-filled: `🚨 OnlyTrusted EMERGENCY from <your name>
   — I need help. Location: https://maps.google.com/?q=<lat>,<lng>.
   Sent <timestamp>.`
4. User taps **Send** once → broadcast fires to all recipients
5. Phone dialer opens with your first emergency contact's number
   pre-dialed — tap the green Call button to connect
6. (Optional) WhatsApp opens with the same distress text pre-filled, and
   your saved emergency-group invite link is copied to the clipboard —
   pick the group, tap Send, then tap WhatsApp's voice-call icon for a
   group voice call

**Setup (first-time use):**
1. Add at least one trusted contact to your Vault with a phone number
2. Settings → **Distress Signal** → toggle shake / volume-down triggers
   on or off per your preference
3. Settings → **Distress Signal** → **WhatsApp emergency group**:
   paste your emergency-group's invite link (see the setup instructions
   on that screen — you create the group manually in WhatsApp, then
   copy-paste the invite link in; WhatsApp blocks third-party apps from
   creating groups programmatically)

**How to use (emergency):**
- Shake your phone 5 times quickly, OR
- Press the volume-down button 4 times fast (Android), OR
- Open OnlyTrusted and tap the big red SOS FAB

Then let the 3-second countdown complete (or tap **Send now** to skip
it). The composer opens, you tap Send once, then the dialer opens.

**Honest claim:** OnlyTrusted sends zero network traffic itself. The
SMS goes out via your carrier's SMS network; the phone call goes
over PSTN / VoLTE; WhatsApp uses your own WhatsApp account. If you've
no cellular signal, SMS will queue until you regain signal and the
dialer won't connect.

### Breach Watch (1 email, Free tier)

**Where:** Settings → **Breach Watch**.

**What it does:** Monitors one email address against a bundled database
of known-breached email prefixes. If your email shows up in a known
breach (LinkedIn 2012, Equifax, Yahoo, Twitter, etc.), you get an alert
with the breach name, date, and what data was exposed.

**How it works without the internet:** OnlyTrusted ships with a
k-anonymity-style hash prefix set — the first 5 bytes of the SHA-1
hash of hundreds of thousands of breached email addresses. When you
enter your email, OnlyTrusted hashes it, takes the same 5-byte prefix,
and looks it up in the bundled set. **Your actual email never leaves
your device.** False-positive rate is < 0.001% for the top-breach set
and acceptable for the alerts to be actionable.

**How to use:**
1. Settings → **Breach Watch**
2. Enter the email you want monitored (same as the one you used in
   onboarding by default)
3. Tap **Check now**
4. Result shows: "No matches found" OR a list of breaches your email
   appeared in with mitigation advice (change password, enable 2FA,
   freeze credit, etc.)

**Premium tier:** monitor up to 5 emails (useful for personal + work +
school + shared family accounts).

### Private Aliases

**Where:** Settings → **Private Aliases**.

**What it does:** Generates random email aliases (e.g.,
`xk7q2m@yourpersonaldomain.com`) you can use for signups you don't
trust. If a merchant leaks the alias, you know which one got breached
and can disable just that alias — without changing your real email.

**Important:** OnlyTrusted generates the alias strings. **The actual
mail-forwarding is handled by your own relay service** of choice
(SimpleLogin, Addy.io, Firefox Relay, Fastmail Masked Email, etc.) —
not by us. The app's job is to give you a predictable alias pattern +
remember which alias you used where.

**How to use:**
1. Settings → **Private Aliases**
2. Tap **Generate new alias** → a random alias string appears
3. Add a label (what you'll use it for, e.g., "DoorDash signup")
4. Save
5. Use the alias on the signup form of the service you're joining
6. Configure your chosen relay service separately to forward that
   alias to your real inbox

Premium tier adds **CSV export** of your alias list for backup/migration.

### Identity Protection

**Where:** Settings → **Identity Protection**.

**What it does:** A guided checklist of identity-hardening actions.
Each row is a deep link to the official self-service portal for that
action:
- **Freeze your credit** (Equifax, Experian, TransUnion deep-links)
- **Place a fraud alert** (same bureaus, 90-day initial alert)
- **File an FTC identity-theft report** at identitytheft.gov
- **Block your Social Security number** at ssa.gov
- **Register with HIBP** (notifies you when new breaches include your
  email)
- **Set up Google / Apple account recovery** methods
- + ~10 more platform-specific recommendations

**Important:** OnlyTrusted doesn't DO any of these for you — it just
guides you through the official steps and provides deep links. You
perform the actions yourself in your browser.

Premium tier adds **yearly renewal reminders** — some actions (fraud
alerts, credit freezes in some states) expire annually; the app
reminds you ahead of expiry.

### Data Removal Scan

**Where:** Settings → **Data Removal Scan**.

**What it does:** Shows you a list of 50+ people-search sites
(Spokeo, Whitepages, BeenVerified, Intelius, etc.) and gives you
deep-links to their individual opt-out pages. Each site has a brief
description of what data they aggregate about you.

**Important:** OnlyTrusted doesn't scrape or remove anything —
removing your data requires filling out each site's opt-out form
yourself (name, email, often a photo ID). We give you the checklist +
the links so you don't have to hunt them down.

Premium tier adds **90-day re-check reminders** since data brokers
notoriously re-add your info after a few months.

### Blocking & Unknown (settings screen)

**Where:** Settings → **Blocking & Unknown**.

**What it does:** The main configuration screen for how OnlyTrusted
handles calls and SMS from numbers that aren't in your Trusted Contacts
vault.

**Toggles:**
- **Silence calls not in vault** — the master "trusted callers only"
  switch. When on, any call from a number not in your Vault is
  silenced (phone doesn't ring). The call still logs; you can see
  missed calls in Log tab.
- **Silence unknown SMS** — same for SMS. Unknown-sender messages
  get silenced + sorted into a separate folder (iOS Junk SMS filter;
  Android custom silent notification).
- **Call handling mode** — what to do with silenced calls:
  - *Silenced + logged* (recommended) — missed-call log entry, no ring
  - *Block completely* — reject the call entirely, no voicemail allowed
- **Text handling mode** — what to do with silenced SMS:
  - *Silence but keep* (recommended) — read if you want to, just no
    notification
  - *Delete automatically* — permanently drop

### Call Handling + Text Handling (separate screens)

**Where:** Settings → **Call Handling** / **Text Handling**.

**What they do:** Finer-grained preferences for how trusted vs
non-trusted calls/texts are announced:
- Ringtone per contact type (trusted / blocked / unknown)
- Vibration pattern per contact type
- Badge counters on the Dashboard (how many unread per type)
- Spam report quick-link (opens your carrier's spam-report app)

### Notifications

**Where:** Settings → **Notifications**.

**What it does:** Enables/disables push notifications for each event
category:
- Call blocked / SMS flagged alerts
- Permission changes (which app got a new sensitive permission)
- SIM-Swap alerts (Pro+)
- Check-In reminders (Pro+)
- Geo-fence exit alerts (Elite)
- Weekly digest reminder
- Honest Limits monthly refresher

### Permission Audit (iOS Permissions / Android Permissions screen)

**Where:** Settings → **Permissions** (or **System → Permissions**).

**What it does:** Shows every OS-level permission the app currently
holds (Contacts, Camera, Microphone, Location, Phone, SMS, etc.) and
explains in plain English what feature each permission powers.

Useful for peace of mind: "The app has Microphone access. Why? Because
I enabled Voice Biometric. If I disable Voice Biometric in Settings,
can I revoke the Microphone permission at the OS level?" Yes — this
screen gives you that visibility.

### Device Posture

**Where:** Settings → **Posture**.

**What it does:** Audits your device for weak security settings and
scores you:
- OS version (current vs latest available)
- Auto-lock timeout (green ≤5 min / yellow ≤15 min / red otherwise)
- Biometric unlock enabled?
- Unknown sources disabled? (Android)
- USB debugging off? (Android)
- Installed from App Store / Play Store? (iOS)
- Overall risk score (color-coded)

Each weak item has a "Fix this" button that deep-links to the relevant
OS Settings screen.

### App Lock, Pocket-dial, Screenshot protection

**Where:** Settings → **App Lock** / **Pocket-dial** / **Screenshot
protection**.

- **App Lock** — require Passkey (biometric) or PIN every time you
  reopen the app. Default on. Timeout configurable (immediate / 1 min
  / 5 min / 15 min / 60 min). On the lock screen, Passkey fires
  automatically; tap **Use PIN instead** if biometric fails.
  **PIN rate limiting (v0.3.5+):** 5 consecutive wrong PINs trigger a
  60-second lockout. 10 → 5 min. 15 → 15 min. 20+ → 60 min lockouts
  that repeat. The counter is stored in the Keychain (iOS) /
  EncryptedSharedPreferences (Android) so a force-stop + relaunch
  doesn't reset it. A successful unlock resets the counter to 0.
- **Pocket-dial** — uses proximity sensor + accelerometer to detect
  when the phone is in your pocket or face-down. If you accidentally
  trigger a tap through fabric, the app refuses to act on it until
  the phone is held normally again.
- **Screenshot protection** — adds `FLAG_SECURE` on Android /
  secure-view treatment on iOS to every OnlyTrusted screen so no app
  (including the OS screenshotter) can capture your Vault or Settings
  as an image. Note: *this only protects the OnlyTrusted app's own
  screens, not the whole phone*.

### Panic Lock + Data Export

**Where:** Settings → **Device & data** → **Panic Lock** / **Export my
data**.

- **Panic Lock** — a destructive-looking red button that **immediately
  locks the app** (next access requires PIN + biometric). Useful if
  you suddenly need to hand your phone over and don't want anyone
  casually peeking at the Vault.
- **Export my data** — one-shot JSON or CSV export of your Vault,
  event log, and settings via the OS share-sheet. Pick a destination
  (email, cloud, local file). Everything stays encrypted at rest; the
  export is plain text so you can read it in a spreadsheet.

### Shield Watermark (Android-only)

**Where:** Settings → **Shield Watermark**.

**What it does:** Draws an on-screen text watermark with your name +
device identifier + timestamp in the corner of your screen whenever
sensitive OnlyTrusted screens are open. Deters shoulder surfing +
serves as physical proof of ownership if the phone shows up on CCTV.

Not available on iOS — iOS doesn't allow overlays over other app
content from third-party apps.

---

## Premium tier

$4.99/yr Individual · $9.99/yr Family (5 seats).

Everything above, plus:

### Privacy Shield

**Where:** Settings → **Privacy Shield** (opens Privacy Shield
Dashboard).

**What it does:** Phase 1 — monitoring of which apps are using the
microphone and camera. Uses:
- iOS 14+ Privacy Dashboard entries (where available)
- Android 12+ `SensorPrivacyManager` observer callbacks

Alerts you with a notification whenever another app starts a mic or
camera capture. Logs every event to the Events Log.

**Important limit:** OnlyTrusted can only *observe* — it can't block
other apps from using mic/camera unless you've installed OnlyTrusted
as a MDM / device-admin profile (which 99% of users won't do). The
Dashboard also hosts a toggle for "Auto-stop OnlyTrusted-owned captures
on screen off" — a narrower guarantee that applies only to the camera/
mic sessions OnlyTrusted itself opens.

### Breach Watch (up to 5 emails)

Same engine as Free but you can enroll up to 5 email addresses
simultaneously. Useful if you want to monitor personal + work +
school + shared family + secondary addresses in one place.

### Private Aliases CSV backup

**Where:** Settings → **Private Aliases** → **Export CSV**.

Exports your alias list (alias string, destination, label, created
date, last-used date) to a CSV file via the OS share-sheet. Useful
for migrating between relay services or auditing which aliases got
leaked.

### Identity Protection renewal reminders

**Where:** Settings → **Identity Protection** → **Reminders**.

Adds annual reminders for actions that expire — fraud alerts (expire
after 90 days or 1 year), state-level credit freezes that require
renewal, etc. The app notifies you ahead of expiry so coverage
doesn't lapse unnoticed.

### Data Removal 90-day re-check reminders

Adds a reminder every 90 days to re-run the opt-out process on each
data broker site, because they re-aggregate your info within weeks of
removal. The reminder links back to the Data Removal screen with the
sites you've previously processed highlighted.

### Weekly PDF Digest

**Where:** Settings → **Weekly Digest**, or Dashboard banner that
appears on Monday morning.

**What it does:** On-demand or auto-generated PDF covering the last
7 days:
- Total calls blocked
- Total SMS flagged
- Top 10 blocked numbers (frequency)
- Trusted-contact changes (adds / edits / deletes)
- Any security events (SIM-swap? Geo-fence exits? Scam contacts
  detected?)
- Honest Limits refresher paragraph

Fully generated on-device (no server round-trip), exported via OS
share-sheet (email, cloud, local save).

### Advanced Blocking

**Where:** Settings → **Advanced Blocking**.

**What it does:** Extra blocking rules on top of the base "silence
calls not in vault":
- **Quiet hours** — silence everyone (including trusted) in a time
  window, e.g., 10pm–8am. Great for Do-Not-Disturb that doesn't
  depend on your OS's finicky DND rules.
- **Area-code allowlist** — explicitly allow calls from one or more
  area codes even when "silence unknowns" is on. Useful if you're
  job-hunting (recruiter area codes), on-call for medical reasons,
  or expecting delivery calls from a known area.
- **Country allowlist** — silence all international calls except
  from the countries you specify.

---

## Pro tier

$19.99/yr Individual · $29.99/yr Family.

Everything above, plus:

### Duress Mode

**Where:** Settings → **Duress Mode**.

**What it does:** Enables a **second PIN** at the unlock screen. If
you type the real PIN, you see the real app (your real Vault, real
events, real backup). If you type the duress PIN, you see a **decoy
app** with:
- A fake Vault populated with the "trusted contacts" you pre-approved
  for the decoy (your dentist, a pizza place, a few other benign
  numbers)
- An event log that looks real but doesn't show the sensitive events
- Settings that look normal but silently prevent destructive actions
  (so a coercer can't wipe your real data from the decoy)

At the same time as showing the decoy, OnlyTrusted **silently fires a
pre-set SMS** to your pre-chosen emergency contact: *"⚠️ OnlyTrusted
DURESS ALERT from <name>: duress unlock triggered at <timestamp>. If
you weren't expecting this, call them or 911."*

**Setup:**
1. Settings → **Duress Mode** → **Enable duress mode**
2. Pick your duress PIN (must be different from your real PIN)
3. Choose which contacts appear in the decoy Vault (hand-picked list)
4. Choose which emergency contact gets the silent SMS when duress fires
5. Confirm with real PIN + biometric

**Important ethics note:** Duress Mode is a *last-resort* feature for
genuinely dangerous situations (domestic abuse, kidnapping, hostile
border crossings). It's not a toy. Fire a false duress alarm and
your emergency contact may call 911 for a wellness check.

### App Permission Watchdog

**Where:** Settings → **App Permission Watchdog**.

**What it does:** Daily, fully automatic audit of every app installed
on your device + the OS permissions each one holds. Compares against
a baseline snapshot from the day you enabled Watchdog. If anything
sensitive changed (an app you've had for months suddenly got
microphone access, for instance), the app:

- Posts a persistent notification
- Shows a full-screen alert on next app launch
- Logs the event in the Events Log

Tap to see which app changed, which permission was added, and
decide:
- **Uninstall** — opens Settings → Apps → that app → Uninstall
- **Allow** — accept the change + update your baseline
- **Investigate later** — snooze the alert

**Why this matters:** Updates to apps you installed years ago can add
new permissions without re-prompting you if you've enabled auto-updates
in Play Store / App Store. Watchdog is your canary.

### SIM-Swap Alarm

**Where:** Settings → **SIM-Swap Alarm**.

**What it does:** On every app launch, reads your current SIM's IMSI +
IMEI + carrier operator name + ICCID. Compares against the baseline
snapshot from when you enabled the alarm. If any identifier differs,
the app:
- Shows a non-dismissable full-screen alert: *"🚨 SIM changed — was
  this you?"*
- Three choices: **Yes, I replaced it** (updates baseline) / **No,
  this is suspicious** (locks the app and shows carrier emergency
  phone numbers) / **Not sure** (requires biometric + PIN to proceed)

**Why this matters:** SIM-swap scams work by convincing your carrier
to port your number to a SIM the attacker controls. From there they
can intercept SMS 2FA codes for your bank, email, crypto accounts, etc.
Carrier notification about the swap is often delayed by hours; the
attacker can empty your accounts in that window. OnlyTrusted detects
the swap the moment your phone fails to see the old SIM — usually
within seconds.

### Wangiri Protection (one-ring scam detection)

**Where:** Settings → **Wangiri Protection**.

**What it does:** Watches for the "one-ring" (Wangiri) scam pattern:
a call that rings for < 2 seconds then hangs up, often from an
international number. The scam goal is to make you call back —
reaching a premium-rate number charging you per minute.

**Android:** full auto-block. The number is added to your personal
block list and future calls from it are silenced. Logged to Events.

**iOS:** detect-only. iOS's `CXCallObserver` doesn't expose the
caller number to third-party apps, so OnlyTrusted can see the
one-ring pattern + count it + warn you, but can't programmatically
block the number. The Wangiri Settings screen clearly labels this
limitation. If you want to block, long-press the call in your
Recents list → Block Contact (iOS's built-in blocker).

### Smart Link Guard

**Where:** Settings → **Smart Link Guard**.

**What it does:** Three sub-features:
1. **SMS URL inspection** — intercepts incoming SMS, extracts URLs,
   checks each against the bundled 50 000-entry phishing-host feed. If
   there's a match, the SMS is tagged as risky in your inbox and you
   get an alert.
2. **QR code scanner** — on-device QR scanner (via ML Kit / iOS
   Vision). Scans a QR code with your camera, extracts the URL, checks
   it against the phishing feed BEFORE opening — gives you a chance to
   decline.
3. **Share-sheet "Scan this URL" extension** — shows up in the system
   share-sheet when you long-press any URL in Safari, Messages, Mail,
   etc. One tap → OnlyTrusted inspects the URL + its full redirect
   chain + reports risk. Nothing is sent to any server.

**Rules engine (12 checks, shared iOS + Android, all run on-device):**

| # | Check | Verdict |
|---|-------|---------|
| 1 | Scheme — http / ftp / telnet / file flagged as insecure | Caution |
| 2 | Known-shortener list (100+ domains) | Caution |
| 3 | Suspicious TLD (.zip, .mov, .xyz, .top, …) | Caution |
| 4 | Punycode `xn--` on hostname | Danger |
| 5 | IP literal (direct IPv4 / IPv6 instead of domain) | Danger |
| 6 | Known-good allowlist (~200 top legit domains) | Safe |
| 7 | Typo-squat (Levenshtein ≤ 2 vs allowlist) | Danger |
| 8 | Suspicious path keyword (/login, /verify, /wallet, …) on an unverified host | Caution |
| 9 | Embedded `user:password@` in URL authority *(v0.3.5+)* | Danger |
| 10 | Unicode homograph — Cyrillic / Greek lookalikes in a host *(v0.3.5+)* | Caution |
| 11 | Brand-as-subdomain spoofing — `paypal.com.scam.ru` *(v0.3.5+)* | Danger |
| 12 | Weighted escalation — 2+ independent Caution flags on the same URL *(v0.3.5+)* | Danger |

Combined with the Offline Threat Intel feed (Elite tier) that bumps any
host matching the APWG phishing list straight to Danger. All 12 checks
are regression-tested on every commit via the JUnit + XCTest suites
documented in `docs/TESTS.md`.

### Safety Check-In

**Where:** Settings → **Safety Check-In**.

**What it does:** Scheduled check-ins. You set up a schedule (e.g.,
"Every 2 hours between 8pm–11pm when I'm out") and OnlyTrusted sends
you a notification: *"Tap to confirm you're OK."* If you don't tap
within a configurable grace period (default 10 min), the app:
- Silently sends your location to pre-chosen emergency contacts via SMS
- Logs the missed check-in to the Events Log

Useful for solo travelers, meeting a Tinder date, walking home late.

### Tracker Finder

**Where:** Settings → **Tracker Finder**.

**What it does:** BLE (Bluetooth Low Energy) scan for nearby trackers
— AirTag, Tile, SmartTag, Chipolo. If OnlyTrusted sees the same
unknown tracker traveling with you across multiple scans (same
device ID appearing in your path 15+ minutes apart), it alerts you.

**Platform limits:**
- iOS: Apple's native "AirTag unknown accessory" detection is more
  thorough + runs system-wide (OnlyTrusted supplements but can't
  replace it)
- Android: Google's "Find My Device" network has similar detection
  for its own tags only; OnlyTrusted is more complete

**Honest caveat:** BLE signal range is 10–30 meters indoors so
tracker detection depends on physical proximity.

---

## Elite tier

$29.99/yr Individual · $49.99/yr Family.

Everything above, plus the hard-security features intended for
high-threat users (journalists, activists, DV survivors, lawyers
handling sensitive matters, security researchers themselves).

### App Shield

**Where:** Settings → **App Shield**.

**What it does:** Phase 2 of Privacy Shield. Walks you through
configuring iOS Screen Time (Family Controls API) or Android Digital
Wellbeing to **lock specific apps behind biometric unlock** every
time they're opened.

Pick which apps need biometric to open — social media, banking,
dating apps, work tools, etc. From then on, every launch requires a
Face ID / fingerprint tap.

**Why not just use OS App Lock?** Both platforms' built-in app lock
is clunky (iOS has it only via Screen Time with lots of edge cases;
Android varies wildly by manufacturer). OnlyTrusted gives you one
consistent interface + Honest Limits callouts for what the OS can
and can't deliver.

### Border Mode

**Where:** Settings → **Border Mode**.

**What it does:** One-tap preset that armors your phone for a hostile
situation — border crossing in an authoritarian country, protest
march, journalist field assignment, etc. Flipping Border Mode on:

1. Enables Duress Mode immediately
2. Puts the phone into Airplane Mode (cuts all radios)
3. Silences all notifications
4. Locks the OnlyTrusted app
5. Arms a Dead-Man Wipe countdown (default 7 days) — if you don't
   unlock the app in that window, your Vault + events + backup auto-
   wipe

Flip it off (requires PIN + biometric + Hardware Key if enrolled)
when you're safely home.

### Panic Lockdown

**Where:** Settings → **Panic Lockdown** (also arm button on the
Dashboard if enabled).

**What it does:** Immediate full-pressure armor:
- Immediate app lock
- Duress Mode arm
- Smart Link Guard SMS watcher **disabled** (so an attacker who
  grabs the phone can't trigger it accidentally)
- Aggressive sensor monitoring (constant Privacy Shield checks)
- PIN-only unlock until next device reboot (biometric disabled —
  prevents a sleeping / coerced user from being forced to unlock
  with their face)

Fire this when you're in acute physical danger. The nuclear option
below Border Mode.

### Hardware Security Key

**Where:** Settings → **Hardware Security Key**.

**What it does:** Enroll a FIDO2 key — YubiKey 5 / Titan / SoloKey /
etc. Once enrolled, the following destructive actions require a
physical tap of your key before they can complete:
- Wipe Everything
- Duress Mode disable
- Encrypted Backup export
- Dead-Man Wipe disable
- Vault restore from backup

iOS: use an NFC-capable FIDO2 key (tap to the back of the phone).
Android: USB-C FIDO2 key or NFC.

**Why this matters:** Even if an attacker has your PIN and biometric,
they can't execute destructive actions without the physical key —
which lives in your pocket / safe / safety deposit box.

### Voice Biometric

**Where:** Settings → **Voice Biometric**.

**What it does:** Deepfake-voice-call defense. For each trusted
contact, record a 10-second clip of them saying a specific phrase.
OnlyTrusted stores a 20-dimensional MFCC descriptor (Mel-Frequency
Cepstral Coefficient — a tiny ~500-byte voice fingerprint). On
incoming calls from that contact, if they repeat the phrase, OnlyTrusted
records 2–3 seconds, extracts the MFCC, and compares via cosine
similarity. Match ≥ 0.6 = authentic. Mismatch = possible AI-cloned
voice; the app warns you.

**Why:** AI voice-cloning from 3 seconds of audio is operational
today. "Mom, I'm in trouble, send money" scams increasingly use
cloned voices. Voice Biometric is a tangible defense without needing
a shared passphrase (though you can combine both).

**Honest limits:** Accuracy ~90-95%. False negatives possible if the
real caller has a cold / is drunk / is calling from a loud room.
Doesn't work for first-contact calls (someone you've never spoken
to by phone before).

### Face-Change Lock

**Where:** Settings → **Face-Change Lock**.

**What it does:** While sensitive OnlyTrusted screens are open
(Vault, Settings, Distress config), the front camera watches
quietly. If a **different face** appears beside / instead of yours,
the app instantly locks itself and requires unlock. If **no face**
is detected for a few seconds (you looked away), the screen blurs
to prevent shoulder-surfing.

Primary use case: you're reading your Vault on the train and
someone sits down next to you. Instead of flipping the phone over
awkwardly, OnlyTrusted hides itself automatically.

### Geo-fence Auto-Lock

**Where:** Settings → **Geo-fence Auto-Lock**.

**What it does:** Define up to 5 "trusted locations" — home, work,
partner's house, etc. When your device **exits all of them**, the
app automatically:
- Locks itself
- Sends a silent SMS alert to designated contacts: *"<Name>'s phone
  left all trusted locations at <timestamp>. Current location:
  <maps-url>."*

Useful for noticing when your phone has been taken (stolen, moved
by someone else) — before you realize it's missing from your pocket.

### Dead-Man Wipe

**Where:** Settings → **Dead-Man Wipe**.

**What it does:** If you don't unlock the app for N days (default 7,
configurable 1–30), it **automatically and irreversibly wipes** your
Vault, event log, and all encrypted data. Useful for:
- Travel to high-risk countries — if your device is confiscated, data
  self-destructs after a week
- Witness protection
- Dead-man's-switch scenarios in general

**How to arm:** Settings → **Dead-Man Wipe** → **Enable** → pick days
→ confirm with PIN + biometric + Hardware Key.

**How to postpone / disable:** unlock the app before the countdown
hits zero (resets to N days) OR disable the feature entirely (needs
PIN + biometric + Hardware Key + Trusted Witness approval if
configured — prevents a coercer from disabling it).

**Clock-skew defense (v0.3.5+):** stores both wall-clock time AND
monotonic uptime at every unlock. The wipe fires on `max(wall-clock
elapsed, monotonic elapsed)`, so an attacker who sets the device
clock backward to delay the wipe doesn't buy themselves extra time —
monotonic uptime can't be rewound by the user. If the device reboots
mid-interval the stored boot time no longer matches the current one
and we fall back to wall-clock only (a reboot is a legitimate signal
we accept, not something we treat as tampering).

**What "wipe" actually destroys:** your Vault contents, blocked
numbers, Events log, Settings prefs, and the master key material.
The feature does NOT wipe the whole phone, iCloud or Google Drive
backups you made outside OnlyTrusted, or data on an offline device
during the deadline window (the next boot runs the check — if the
device stays off past the deadline, it wipes on next boot).

### Encrypted Backup (export)

**Where:** Settings → **Encrypted Backup**.

**What it does:** Exports your entire Vault + settings to a single
`.ot2bak` file, encrypted with AES-256-GCM using a key derived from
your PIN (PBKDF2-HMAC-SHA256, 100k iters). The file is opaque to
cloud providers — safe to store on Google Drive, Dropbox, iCloud,
or email to yourself. Only you can decrypt it (needs your PIN).

### Restore from Backup

**Where:** Settings → **Restore from Backup**.

**What it does:** Pick a `.ot2bak` file from a previous backup +
enter your PIN + decrypt + overwrite the current Vault. Useful on
new devices or after a wipe.

### Shamir Split

**Where:** Settings → **Shamir Split**.

**What it does:** Splits your Vault's master encryption key into
M-of-N shares using Shamir Secret Sharing (e.g., 3-of-5). Distribute
each share to a trusted friend/family member via Bluetooth peer-to-
peer (no network, no cloud).

Later, if you lose your PIN, you can recover the Vault by having M
(e.g., 3) of the N shareholders bring their OnlyTrusted devices
near yours — each one approves the recovery on their own phone, and
your Vault is reconstructed.

**Why:** removes the single-point-of-failure of your PIN. If you die
in an accident, your spouse + sibling + lawyer (any 3 of your 5
shareholders) can collectively recover your Vault for the estate.

### Trusted Witness

**Where:** Settings → **Trusted Witness**.

**What it does:** Designate up to 5 friends as "witnesses." For
destructive actions (Wipe Everything, Duress disable, Dead-Man
disable, Backup export), the app requires at least one witness
device to be physically near yours via BLE **and** that witness to
approve on their device.

Prevents coercion: an attacker who has your PIN + biometric still
can't wipe your Vault unless a trusted witness is standing there +
chooses to approve.

### Steganographic Vault

**Where:** Settings → **Steganographic Vault**.

**What it does:** Hides your encrypted Vault inside a decoy image
(PNG) or document (PDF). The image looks like a normal family photo
or vacation picture; the encrypted Vault is embedded in the least-
significant bits of the pixel data (LSB steganography). To an
observer casually inspecting your phone, it's just a photo.

Combine with Duress Mode for maximum plausible deniability — the
decoy Vault visible on the duress-PIN unlock is a shell; your real
Vault lives inside `beach_vacation_2024.png` in your photos.

### Signed Audit Log

**Where:** Settings → **Signed Audit Log** → **Evidence Pack
export**.

**What it does:** Every security event (every blocked call, every
Distress fire, every permission change, every Dead-Man countdown
tick) is:
- Cryptographically signed by your Hardware Security Key when it
  occurs
- Chained into a Merkle tree so any attempt to tamper with historical
  entries breaks the chain + is detectable

**Export an Evidence Pack:** generates a `.zip` containing the signed
log, the Merkle proofs, and a verifier program. Admissible in court
as forensic proof that specific events happened at specific times
and weren't retroactively edited. Primary use: survivor + prosecutor
evidence in domestic abuse cases.

### Rogue Network Scanner

**Where:** Settings → **Rogue Network Scanner**.

**What it does:** Three continuous checks:
- **IMSI-catcher detection** — notices if your phone silently
  downgrades from LTE/5G to 2G (a classic cell-site simulator
  tell). Alerts you.
- **Rogue Wi-Fi SSID detection** — flags Wi-Fi networks with names
  that mimic legitimate ones (`Starbuks` vs `Starbucks`).
- **Hotel / coffee shop Wi-Fi downgrade** — notices if "Hilton
  Guest" suddenly started presenting an identical SSID but a
  different BSSID (MAC address of the access point).

**Honest limit:** heuristic detection ~70-80% accurate. False
positives possible in areas with legit weak signal.

### Offline Threat Intel

**Where:** Settings → **Threat Intel**.

**What it does:** Browse the bundled threat-intel feeds the app ships
with (50k+ phishing hosts, scam phone numbers, Pegasus / Candiru /
Predator IoCs, breach-hash prefixes). Useful for curiosity +
auditing *what* OnlyTrusted actually knows about threats.

Updates ship via normal app updates — not live-fetched. See the
`README.md` in that section for the sources.

---

## Settings

### Account

- **Email** — change the email registered during onboarding (for
  Breach Watch + Distress Signal personalization)
- **Change PIN** — rotate your 4-6 digit PIN. Requires current PIN.
  Re-derives all encryption keys.
- **Biometric unlock (Passkey)** — toggle Face ID / Touch ID /
  fingerprint. Backed by Secure Enclave (iOS) / StrongBox (Android)
  since v0.3.5. See `docs/AUTH_MODEL.md` for the architecture.
- **Auto-lock timeout** — immediate / 1 min / 5 min / 15 min / 60 min

### Language & Region

**Where:** Settings → **Language & Region**.

**What it does:** Choose your region, country, and preferred language.
English is always available regardless of country. OnlyTrusted ships
with 8 UI languages as of v0.3.4+:

- English (source)
- Spanish — Español
- French — Français
- Portuguese — Português
- Hindi — हिन्दी
- Arabic — العربية (full RTL layout)
- German — Deutsch
- Indonesian — Bahasa Indonesia

The picker has 3 cascading dropdowns: Region (5 regions) → Country
(195 countries from the OnlyTrusted Localization Kit) → Language. The
language list for each country includes the native language(s) + English
as the fallback. Picking any language in the shipped-translated list
flips the UI immediately; picking one we don't ship for yet keeps the
underlying locale set correctly (so dates / numbers / currency format
in the chosen locale) but renders UI text in English.

**Honest note on translation quality:** every translation for the 7
non-English languages shipping today is **machine-generated by Claude
Opus** and awaits native-speaker review. Safety-critical strings
(SOS, duress, legal, pricing, system alerts) are tagged in the
resource files with a `[REVIEW]` sentinel and the UI shows a small
amber **"⚠ Translation pending review"** caption so you can see
exactly which strings a human hasn't signed off on yet.

See `docs/NEEDS_REVIEW.md` for the full per-locale review checklist
and the exact string keys that need professional review before any
locale is listed in App Store / Play Store metadata.

### Appearance

- **Dark mode** — forced on currently (Midnight palette is the
  brand). Light mode exists in code but not exposed; may be opt-in
  in a future release.
- **Accent color** — Cyan (default), Purple, Teal
- **Text size** — Default, Large, Extra Large

### Device & data

- **Panic Lock** — immediate app lock (red button)
- **Export my data** — JSON / CSV export
- **Wipe everything** — irreversible destructive wipe. Needs PIN +
  biometric + Hardware Key (if enrolled) + Trusted Witness approval
  (if enrolled). Confirmation dialog.

### Billing

- **Current tier** — Free / Premium / Pro / Elite + Family variant
- **Renewal date** — when the next charge happens
- **Manage subscription** — deep-link to Google Play / App Store
  subscription management
- **Upgrade** — opens the Upgrade screen (same UI as the pricing
  page at docs/pricing/)

### Legal

- **Privacy Policy** — the full policy, bundled in the app
- **Terms of Service** — the EULA
- **Open Source Licenses** — all third-party library licenses
- **Honest Limits** — the full "what we can't do" document

### About

- **Version** — current app version + build number + commit SHA
- **Support** — admin@vanpaultek.com (email link)
- **GitHub** — github.com/Vanpaultek/OnlyTrusted (opens in browser)

---

## Platform differences

| Feature | iOS | Android | Notes |
|---|---|---|---|
| SMS inbox scan | ❌ | ✓ | iOS blocks third-party SMS inbox read; use the MessageFilter extension (pre-delivery filter only) |
| Wangiri auto-block | detect-only | ✓ | iOS CallKit doesn't expose caller number to third parties |
| Shield watermark overlay | ❌ | ✓ | iOS doesn't allow overlays over other app content from third parties |
| Volume-down 4× distress trigger | ❌ | ✓ | iOS doesn't expose volume keys outside the app |
| Hotword distress trigger | ❌ | ❌ | Apple + Google reserve this for Siri / Hey Google. OnlyTrusted doesn't try to compete. |
| Live-read of system contacts in Vault | partial | partial | Both platforms require a one-time user grant |
| Face-Change Lock | ✓ | partial | iOS Vision framework is more mature; Android support varies by OEM |
| Background BLE scan (Tracker Finder) | partial | ✓ | iOS requires app to be foreground for continuous scan; Android allows foreground service |
| Hardware Security Key (FIDO2) | NFC only | USB-C + NFC | iOS doesn't support USB FIDO keys for third-party apps |
| App Shield (lock other apps) | via Screen Time | via Digital Wellbeing | Both platforms require elevated grants |
| Encrypted Backup / Restore | ✓ | ✓ | Both ship the same `.ot2bak` format |
| Signed Audit Log (Elite) | ✓ | ✓ | Both support FIDO2 attestation chains |

---

## Honest Limits

Everything OnlyTrusted **cannot** do, listed plainly. Also surfaced
in-app at Dashboard → Honest Limits sheet (bullets kept in sync with
this document — audited every release per `docs/RELEASE_GATES.md`
gate 9).

**Call Shield** uses supported caller-blocking interfaces and cannot
block every scam call.

**SMS Defense** processes supported messages from unknown senders
only. It cannot inspect your entire inbox or iMessages. Scam SMS
Scan (bulk-inbox audit) is Android-only — iOS reserves inbox read
to Mail / Messages.

**Auto Privacy Shield** stops OnlyTrusted-owned recording sessions.
It does not disable other apps' microphones or cameras. Monitoring
other apps' mic / camera use at a hardware level needs root (Android)
or MDM (iOS); what Privacy Shield shows is what the OS exposes via
Privacy Dashboard / SensorPrivacyManager — which is OS-decided, not
comprehensive.

**Dead-Man Wipe** destroys your on-device vault after the chosen
interval. It cannot wipe backups you created outside OnlyTrusted
(iCloud, Google Drive, screenshots, printouts). It cannot run while
the device is powered off during the deadline — the check runs on
next boot, so if the device stays off past the deadline the wipe
fires on the subsequent power-on. The v0.3.5+ clock-skew defense
means setting the device clock backward no longer buys an attacker
extra time, but a reboot still falls back to wall-clock timing for
that unlock cycle.

**Voice checks** cannot prove a caller is genuinely who they claim.
Verify sensitive requests via a trusted callback to a saved number,
not a callback to the number that just called you. Voice Biometric
(Elite) + a pre-shared family passphrase is the OnlyTrusted-owned
mitigation; neither replaces a real callback.

**Wangiri auto-block** is Android-only. iOS CallKit doesn't expose
the caller number to third-party apps, so we can detect the one-ring
pattern + warn you but can't programmatically block the number. On
iOS you long-press the call in Recents → Block Contact to use the
system blocker.

**OS-level tracking is out of scope.** We cannot make your phone
untrackable by your carrier, your ISP, your operating-system vendor,
or any system app with network access.

**Screenshots of OTHER apps** cannot be blocked by `FLAG_SECURE`.
That flag only applies to OnlyTrusted's own screens. Someone can
always photograph the screen with a second device.

**Video-call recording** by the other party on their own screen or
by filming your face with a separate device cannot be prevented by
anything running on your phone.

**WhatsApp groups** cannot be created programmatically. WhatsApp ToS
reserves that to its own UI. You create the group manually once +
paste the invite link into OnlyTrusted Settings → Distress Signal.
After that, Distress Signal opens the group chat with your emergency
message pre-filled.

**PIN brute-force limits:** 5 wrong PINs → 60 s lockout, escalating
to 60 min at 20+ failures (v0.3.5+). The counter is Keychain / 
EncryptedSharedPreferences-backed so force-stop + relaunch doesn't
reset it. But: if someone physically acquires an UNLOCKED device,
PIN rate limiting doesn't help — the vault is already accessible to
anyone operating that device.

**PIN + biometric recovery** is impossible if you've forgotten your
PIN AND never set up Shamir Split with trusted shareholders. There
is nowhere to recover from — by design. Set up Shamir Split (Elite)
BEFORE you need it.

---

## Subscription management

### Managing on iOS

Settings → **Billing** → **Manage subscription** opens iOS Settings →
Apple ID → Subscriptions → OnlyTrusted. Everything happens there:
upgrade / downgrade / cancel / change renewal cadence.

### Managing on Android

Settings → **Billing** → **Manage subscription** opens Google Play →
account menu → Subscriptions → OnlyTrusted. Same options.

### What happens if you downgrade

- **Downgrade from paid to Free**: at the end of your current paid
  period, premium features gray out but your Vault + all contact /
  blocking data stay intact. You keep using Free indefinitely.
- **Downgrade Family → Individual**: your family members lose their
  seats at the end of the billing period. Each one keeps their
  Vault + can subscribe independently.
- **Cancel entirely**: at the end of the billing period, you're on
  Free. Your data stays. No data loss.

### What happens if you lose access to the App Store / Play Store

Nothing happens to your data. The app continues to run on the tier
you're on. If you can't renew, you drop to Free at next renewal
date but keep all contacts + settings.

---

## Troubleshooting + FAQ

### My SOS button is grayed out
You need at least one Trusted Contact with a valid phone number in
your Vault. Add one via Contacts tab → **+**.

### Shake-to-trigger isn't firing
Verify under Settings → **Distress Signal** that **Shake phone to
trigger** is toggled on. Shake-detection runs only when the app is
in the foreground (both platforms don't allow background shake
listeners for third-party apps).

### Volume-down 4× isn't firing (Android)
This is opt-in and off by default. Enable it under Settings →
**Distress Signal** → **Volume-down 4× to trigger**. Also note:
the 4-press pattern must land within 2 seconds and the OnlyTrusted
app must be foreground.

### The release AAB on GitHub is "unsigned"
Only true for releases built before the Android signing secrets were
wired to the CI (which happened at `v0.3.1`). Every release from
`v0.3.1` onward is signed with the OnlyTrusted upload key — verify
with `jarsigner -verify OnlyTrusted-v0.3.x.aab`.

### How do I change the app language?
Settings → **Language & Region** → pick Region → Country → Language.
English is always offered regardless of country. The UI flips
immediately for most screens; a handful of system-controlled surfaces
(StoreKit sheets on iOS, the system BiometricPrompt on Android) follow
the device locale rather than OnlyTrusted's own override. 8 languages
ship with v0.3.4+; the 7 non-English ones are machine-translated and
flagged for review — see the Language & Region section + the "⚠
Translation pending review" badge in-app.

### My wrong-PIN lockout isn't lifting
5 wrong PINs = 60-second lockout. 10 = 5 minutes. 15 = 15 minutes.
20+ = 60-minute lockouts that keep repeating until you enter the
correct PIN. The counter is Keychain / EncryptedSharedPreferences-
backed, so quitting the app (even force-stop) doesn't reset it. The
counter resets to zero the moment you successfully enter the right
PIN. If you've genuinely forgotten the PIN, the only paths back in
are Passkey (biometric) if enrolled, or Shamir Split recovery if you
set that up BEFORE forgetting the PIN. Wait out the lockout window
rather than reinstalling — reinstalling wipes the vault.

### I lost my phone — can I log into my OnlyTrusted account from a new one?
No, because there is no account. Your Vault lives in encrypted storage
on the lost device. To restore on a new device: install OnlyTrusted,
go to Settings → **Restore from Backup** (Elite tier) + load your
`.ot2bak` file. If you didn't set up Encrypted Backup OR Shamir Split
before losing the phone, your data is gone.

### How do I know OnlyTrusted actually isn't phoning home?
Three ways:
1. Install [NetGuard](https://netguard.me) (Android) or
   [Lockdown Privacy](https://lockdownprivacy.com) (iOS) and watch —
   OnlyTrusted will show zero outbound connections.
2. Inspect the `AndroidManifest.xml` + `Info.plist` in the public
   source. The main process has no `INTERNET` permission on Android
   + no `App Transport Security` entries for any domain on iOS.
3. Verify the SLSA Build Provenance attestation on any GitHub
   release matches the public source + the shipping artifact.

### I need help / have a feature request / found a bug
Email `admin@vanpaultek.com` or open an issue at
github.com/Vanpaultek/OnlyTrusted/issues.

### Security vulnerability
See `SECURITY.md`. Private disclosure to `security@vanpaultek.com`
with a 90-day embargo before public disclosure.

---

## Appendix: glossary

- **AAB** — Android App Bundle. The format Google Play uses for
  installing apps; smaller than APK because the store generates a
  per-device APK on install.
- **AES-256-GCM** — Advanced Encryption Standard with 256-bit keys
  in Galois/Counter Mode. Industry standard for authenticated
  encryption. OnlyTrusted uses it everywhere data is encrypted at
  rest.
- **BLE** — Bluetooth Low Energy. The radio protocol used by trackers
  (AirTag, Tile, etc.) and the Shamir Split peer-to-peer recovery.
- **CallKit** — Apple's iOS framework for third-party calling apps +
  call directory extensions.
- **FIDO2** — Fast Identity Online 2.0. The standard for hardware
  security keys (YubiKey, Titan, SoloKey).
- **IMSI** — International Mobile Subscriber Identity. A unique
  identifier on your SIM card.
- **IMEI** — International Mobile Equipment Identity. A unique
  identifier for your phone hardware.
- **IoC** — Indicator of Compromise. Known-bad hashes, URLs, process
  names, or filenames used to detect malware.
- **MFCC** — Mel-Frequency Cepstral Coefficient. A compact numerical
  representation of a voice sample, used in Voice Biometric
  fingerprinting.
- **PBKDF2** — Password-Based Key Derivation Function 2. The slow
  hash function OnlyTrusted uses to turn your 4-digit PIN into a
  256-bit encryption key.
- **PSTN** — Public Switched Telephone Network. The classical phone
  network your SOS phone call rides on.
- **SBOM** — Software Bill of Materials. A machine-readable list of
  every dependency in the released artifact. OnlyTrusted releases
  include a CycloneDX SBOM.
- **Shamir Secret Sharing** — a cryptographic scheme for splitting a
  secret into N shares such that any M of them can reconstruct it,
  but M-1 or fewer reveal nothing. Used in Shamir Split (Elite).
- **SLSA** — Supply-chain Levels for Software Artifacts. The standard
  for proving that a shipped binary was built by a specific CI
  workflow from a specific source commit. OnlyTrusted releases
  include SLSA Build Provenance attestations.
- **SQLCipher** — AES-256 encrypted drop-in replacement for SQLite.
  OnlyTrusted's local database is wrapped in SQLCipher so even a
  copy of the database file is unreadable off-device.
- **Wangiri** — Japanese for "one ring and cut." The scam pattern of
  calling, letting the phone ring once, and hanging up — hoping the
  victim calls back a premium-rate number.

---

Maintained by: VanPaulTek Inc. · `admin@vanpaultek.com`
