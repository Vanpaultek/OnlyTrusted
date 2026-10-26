# OnlyTrusted — User Guide Video Script

A ready-to-film step-by-step video script for the complete OnlyTrusted
onboarding + feature walkthrough. Written as a tutorial, not an ad —
warm, educational, honest.

**Format**: 1 long video (~28 min) broken into 15 YouTube chapters.
Each chapter can also stand alone as a 1-3 minute short.

**Audience**: everyday phone users who've just installed OnlyTrusted
and want to know how to actually use it. Not developers.

**Tone**: calm, factual, respect-the-user. Avoid hype. Treat the viewer
like a smart adult who deserves to know both what the app does AND
what it can't do.

**Shoot setup**:
- Phone: iPhone 17 Pro OR Pixel 7 Pro (shoot both, cut together)
- Screen recording: QuickTime (iOS) + `scrcpy` (Android), 60fps
- Face-cam: optional B-roll of the presenter for intro + outro
- Mic: lavalier, voiceover-safe room
- Length budget: aim ~27 minutes final, chaptered

**Last updated**: 2026-10-25 · covers release `v0.3.6+`

---

## Full chapter list (YouTube chapter stamps go here)

```
00:00  Introduction — what makes OnlyTrusted different
02:00  Install + first launch
03:00  Setup walkthrough — email, PIN, Passkey, consent
06:30  The Dashboard tour
08:30  Trusted Contacts — adding people who matter
10:30  Blocked list — stopping the ones who don't
12:00  Events log — your protection history
13:30  Settings — language, PIN rules, biometrics
15:30  SOS / Distress Signal — when things go wrong
18:30  Smart Link Guard — the phishing shield
20:30  Breach Watch — has your email leaked?
21:30  Upgrading — Premium / Pro / Elite tiers
24:00  Honest Limits — what we don't do
25:30  Changing language — 9 languages shipped
26:30  Where to get help + outro
```

---

## Chapter 1 — Introduction (00:00 – 02:00)

### Scene 1A: cold open (0:00 – 0:15)

**Camera**: fade in on a phone screen showing: "Unknown Caller" ringing.
Finger hesitates. The call cuts to voicemail without the phone ringing
once more — because OnlyTrusted silenced it.

**Voiceover**: *"Every day, your phone is ringing with calls you don't
want. From people you don't know. For reasons that don't help you."*

### Scene 1B: pitch (0:15 – 0:45)

**Camera**: face-cam of the presenter in a well-lit room. OnlyTrusted
app visible on the phone they're holding.

**Voiceover**: *"OnlyTrusted is a privacy phone-shield. Three things
make it different from the dozen other apps that promise this. First
— nothing it does needs a cloud. Second — nothing it does needs an
account. Third — everything happens on your device, where your data
belongs. We'll show you how it works, and we'll be honest about what
it doesn't do."*

### Scene 1C: what we'll cover (0:45 – 1:30)

**Camera**: screen-recording montage of the Dashboard, Trusted Contacts,
Settings, SOS. Slow Ken-Burns pan on each.

**Voiceover**: *"In this guide I'll walk you through installing the
app, setting it up, the Dashboard, blocking scammers, protecting
yourself from phishing, and the premium features that cover you in
more serious situations — like a lost phone or a border crossing. The
chapter list is in the description so you can jump straight to what
you need."*

### Scene 1D: what we won't lie about (1:30 – 2:00)

**Camera**: close-up of the Honest Limits sheet, slowly scrolling.

**Voiceover**: *"Before we start — this app has limits. We tell you
what we can't do inside the app itself. We'll cover them at the end
of this guide. If you ever want to double-check anything you see here,
tap the Honest Limits link from the Dashboard."*

---

## Chapter 2 — Install + first launch (02:00 – 03:00)

### Scene 2A: App Store / Play Store (2:00 – 2:30)

**Camera**: open App Store on iPhone (or Play Store on Pixel). Type
"OnlyTrusted" in the search bar. The listing appears with the shield
icon. Tap **Get** / **Install**.

**Voiceover**: *"Open the App Store on iPhone, or the Play Store on
Android. Search for 'OnlyTrusted' — one word, capital O, capital T.
Tap Get, or Install. It's a free download — the app has a paid Pro
and Elite tier, but every core safety feature is Free forever."*

### Scene 2B: first launch — splash (2:30 – 3:00)

**Camera**: app finishes installing. Tap the icon. Splash screen:
Midnight-palette lakescape with the OnlyTrusted shield + wordmark.
Progress bar fills. Transitions to the Welcome card.

**Voiceover**: *"The first time you open it, there's a short splash —
that's the app loading your preferences and the local scam-number
database it ships with. No download, no sign-up — all of that is
inside the app itself. That's part of the zero-cloud design."*

**B-roll screen**: `SplashScreen.swift` / `SplashScreen.kt`.

---

## Chapter 3 — Setup walkthrough (03:00 – 06:30)

### Scene 3A: Welcome card (3:00 – 3:20)

**Camera**: welcome screen with the OnlyTrusted wordmark + tagline
*"A safer, quieter phone starts here."* Three bullet rows underneath:
"Allow only trusted contacts", "Silence or block everyone else",
"Keep a local audit log". **Get Started** button in cyan at bottom.

**Voiceover**: *"The welcome screen tells you the three things the app
is really about. Trusted people get through. Everyone else gets
filtered. And every security event is logged on your device so you can
see exactly what happened. Tap Get Started."*

### Scene 3B: Consent screen (3:20 – 4:00)

**Camera**: three checkboxes:
- *I am an adult*
- *I own this device or am authorized to use it*
- *I have read and understood the honest limits*

Tap each. **I confirm and continue** button activates. Tap it.

**Voiceover**: *"Three check-boxes before we go further. One: you're
an adult. Two: this is your phone, or you're allowed to use it. Three:
you've read what we can't do. Tap all three, then confirm. This is
also where you can tap through to our privacy policy, terms of use,
and the full Honest Claims document."*

### Scene 3C: Email (4:00 – 4:40)

**Camera**: email input field with placeholder "you@example.com".
Below it: *"Stored on this device only. OnlyTrusted does not have a
server, so your email is never transmitted anywhere."* Type an email.
Tap **Continue**.

**Voiceover**: *"Enter your email. This is only used to personalize
the distress-signal text and to let you check if your email was in
any data breach — using a hash list we ship inside the app, not a
cloud service. The email never leaves your phone. There's no account
being created. Nothing to log into."*

### Scene 3D: PIN (4:40 – 5:30)

**Camera**: two PIN fields. Type a 4-6 digit PIN twice. Shows:
*"Backup unlock for when your Passkey can't be presented. PBKDF2-
hashed with 100k iterations before storing — we can't read it back."*
Tap **Set PIN**.

**Voiceover**: *"Pick a PIN between four and six digits. This is your
fallback — the primary unlock will be your face or fingerprint on the
next screen, but you need a PIN in case your fingerprint sensor fails
or your face is injured. Important: write this PIN down somewhere
safe. We cannot recover it. There's no cloud it lives in, no email
reset, no customer-service workaround. If you forget both your PIN
and your biometric, your vault is gone — by design."*

### Scene 3E: Passkey setup (5:30 – 6:15)

**Camera**: *Set up your Passkey* screen. *Face / fingerprint becomes
your primary unlock — phishing-proof, nothing to type. Uses hardware-
backed Keystore / StrongBox; biometric never leaves this device.*
Tap **Set up Passkey**. System biometric prompt. Face ID / Touch ID
authenticates. Returns to onboarding.

**Voiceover**: *"Now your Passkey. On iPhone this uses the Secure
Enclave chip — a dedicated piece of hardware physically separate from
the main processor. On Android it uses StrongBox or the Keystore. The
key is created inside that chip. It never comes out. Your face scan
unlocks it only when you're the one holding the phone. If someone
registers their own face on your phone later, the key gets wiped
automatically. That's a hardware-level defense, not something the app
is promising to do in software."*

### Scene 3F: Consent → Dashboard (6:15 – 6:30)

**Camera**: final consent card. Tap **I understand**. Dashboard loads.

**Voiceover**: *"One final acknowledgment, and we're in."*

---

## Chapter 4 — The Dashboard tour (06:30 – 08:30)

### Scene 4A: cinematic header + greeting (6:30 – 7:00)

**Camera**: Dashboard header — Midnight lakescape backdrop, "OnlyTrusted"
wordmark top-left, settings gear top-right. Below, in large type:
"Good evening, Welcome 👋".

**Voiceover**: *"This is the Dashboard — your home base. The greeting
changes with time of day. The gear icon in the top right takes you to
Settings. We'll come back to Settings in a bit."*

### Scene 4B: Protection card (7:00 – 7:30)

**Camera**: large cyan card with the OnlyTrusted shield graphic, "Setup
needed" / "Protection is active" as the state changes. Finger taps —
toggles trusted-only mode.

**Voiceover**: *"The big card tells you if protection is on. If it
says 'Setup needed', add at least one trusted contact and flip the
silence-calls-not-in-vault toggle. Once both are done, this turns
cyan and says 'Protection is active'."*

### Scene 4C: Stat tiles (7:30 – 8:00)

**Camera**: three stat tiles — "1 Trusted Contacts", "0 Blocked (Today
· 0 this week)", "0 Scam Contacts". Camera zooms in on each.

**Voiceover**: *"Three tiles. Trusted Contacts — how many people you've
let through. Blocked — how many scam calls and texts we silenced for
you today and this week. Scam Contacts — how many entries in your
regular phone book match our known-scammer list. All of this is counted
on your device."*

### Scene 4D: Quick Actions (8:00 – 8:20)

**Camera**: "Quick Actions" header, three cards — "Emergency Contact",
"Block Number", "View Log". Hover each.

**Voiceover**: *"Quick Actions puts your three most-used controls one
tap away. Add an emergency contact. Block a specific number. Jump
straight to the Events log."*

### Scene 4E: SOS button + Honest Limits callout (8:20 – 8:30)

**Camera**: pull back to show the full Dashboard. Red SOS button
floats in the bottom-right. Below the Quick Actions, an amber card:
"Honest Limits — This app cannot monitor other apps' microphone or
camera use. Tap to see all platform limits."

**Voiceover**: *"The red SOS button is always on the Dashboard. We'll
cover that in detail in chapter 8. And the amber Honest Limits card
is here from day one — a reminder of what we don't do. Tap it anytime
to see the full list."*

**B-roll screens**: `MainTabs.swift` / `DashboardScreen.kt`.

---

## Chapter 5 — Trusted Contacts (08:30 – 10:30)

### Scene 5A: open Contacts tab (8:30 – 8:50)

**Camera**: tap the bottom-nav Contacts tab. Empty state card:
*"No trusted contacts yet. Tap + to add your first."*

**Voiceover**: *"Trusted Contacts are the people whose calls and
messages should always get through. Everyone else can be silenced
when you're in trusted-only mode. Tap the plus button to add your
first one."*

### Scene 5B: add from Contacts (8:50 – 9:30)

**Camera**: tap plus. Two options: *Pick from Contacts* or *Add
manually*. Tap Pick from Contacts. iOS/Android contact picker opens.
Choose a contact. Returns to OnlyTrusted with a confirmation card
showing the person's name + numbers.

**Voiceover**: *"You have two ways to add. Pick from Contacts opens
your system contact book — this is a one-time read, we only save the
person you pick, not the whole book. Or Add manually if you'd rather
type the number without granting contact access at all. Everything
you add is encrypted on your device."*

### Scene 5C: Contact detail + relation (9:30 – 10:00)

**Camera**: contact row shows name, masked phone (partially hidden
until the user taps to reveal), tap-and-hold reveals menu — *Edit*
and *Delete*.

**Voiceover**: *"Each contact shows a masked version of their phone
number for privacy at a glance. Tap to reveal. You can edit relation
— family, work, friend — or delete. Deleting is permanent and
removes the encrypted record."*

### Scene 5D: search + relation tabs (10:00 – 10:30)

**Camera**: top of Contacts screen — search bar. Below it, filter
chips: All / Family / Work. Type into search; list filters live.

**Voiceover**: *"If you've got many contacts, the search bar at the
top filters as you type. The relation chips let you narrow by family,
work, or friend. All of this filtering happens on your device —
nothing you search for is logged anywhere."*

**B-roll screens**: `VaultScreen.swift` / `VaultScreen.kt`.

---

## Chapter 6 — Blocked list (10:30 – 12:00)

### Scene 6A: Blocked tab (10:30 – 10:50)

**Camera**: tap the bottom-nav Blocked tab. Empty state: "No blocked
numbers yet." Below it: a form with Number + Label fields and two
checkboxes — Block calls, Block SMS.

**Voiceover**: *"The Blocked tab is the opposite of Contacts — these
are the specific numbers you want to shut out. You can block a known
spammer, a stalker number, or any number you no longer want to hear
from."*

### Scene 6B: add a blocked number (10:50 – 11:30)

**Camera**: type a phone number. Add label "spam". Check both
boxes. Tap **Add to block list**. The number appears in the list
below with Calls + SMS tags.

**Voiceover**: *"Enter the number, add an optional label so you
remember who it is, and choose whether to block calls, SMS, or both.
Tap Add to block list. The number is now blocked at the OS level —
on Android via the call-screening service, on iPhone via the Call
Directory extension. Note that this works best after you tap Reload
and enable OnlyTrusted inside your iPhone's Settings app, under
Phone."*

### Scene 6C: review + remove (11:30 – 11:50)

**Camera**: scroll the blocked list. Tap a trash icon on a row. Confirm.
The row disappears + a toast confirms removal.

**Voiceover**: *"To un-block someone, tap the trash icon next to
their row. There's no cooldown or appeals process — you can un-block
immediately."*

### Scene 6D: Blocking & Unknown settings (11:50 – 12:00)

**Camera**: top-right gear icon on Blocked. Opens *Blocking & Unknown*
settings. Shows master toggle "Silence calls not in vault".

**Voiceover**: *"The master toggle in the gear menu silences EVERYONE
not in your trusted list — not just the specific numbers you blocked.
This is the single most powerful setting in the app, so we leave it
off by default."*

**B-roll screens**: `BlockListScreen.swift` / `BlockListScreen.kt`.

---

## Chapter 7 — Events log (12:00 – 13:30)

### Scene 7A: open Log tab (12:00 – 12:20)

**Camera**: tap Log tab. Events list: timestamped icons — blocked
call, blocked SMS, SIM-swap event, geo-fence exit.

**Voiceover**: *"The Log tab is your running history. Every call we
blocked, every scam SMS we flagged, every security event we noticed —
all here with timestamps."*

### Scene 7B: filter + search (12:20 – 12:50)

**Camera**: top-right filter icon. Options: All / Blocked / Flagged /
Panic / System. Tap one, list updates. Then type in search bar — list
filters by number or label.

**Voiceover**: *"Filter by type, or search by phone number or label.
This is useful when you want to prove something — like showing a
family member that yes, that scammer did call three times this week."*

### Scene 7C: tamper warning (12:50 – 13:30)

**Camera**: a hypothetical red banner at the top: *"Audit chain
tampered. The event log hash chain is broken. An event may have been
edited."* Then zoom out to show the normal log.

**Voiceover**: *"If anyone — or any malware — tries to edit the log,
the app detects it. Elite tier adds a tamper-proof hash chain so you
have forensic proof your log hasn't been changed. In the Free tier,
basic integrity checks still fire. Your log is yours; we don't send
it anywhere."*

**B-roll screens**: `EventsScreen.swift` / `EventsScreen.kt`.

---

## Chapter 8 — Settings tour (13:30 – 15:30)

### Scene 8A: Settings top (13:30 – 13:50)

**Camera**: tap Settings tab. Scrollable list of categories.

**Voiceover**: *"Settings has every toggle in the app. We'll touch
the three most important ones: Account, Language & Region, and App
Lock."*

### Scene 8B: Account section (13:50 – 14:20)

**Camera**: tap Account. Rows: Email · Change PIN · Biometric unlock
(Passkey) · Auto-lock timeout. Tap Change PIN. Flow: enter current
PIN → enter new PIN twice → confirm.

**Voiceover**: *"Account is where you change your email, rotate your
PIN, toggle Passkey, or change how long before the app locks itself.
When you rotate the PIN, we re-derive all encryption keys from the
new one — the old PIN is immediately useless."*

### Scene 8C: App Lock + PIN rate limiter (14:20 – 14:50)

**Camera**: Settings → App Lock. Toggle on. Timeout dropdown: Immediate
/ 1 min / 5 min / 15 min / 60 min. Then screen a hypothetical: enter
wrong PIN 5 times at the lock gate. Toast: *"Too many wrong PINs.
Locked for 1 min."*

**Voiceover**: *"App Lock requires your Passkey or PIN every time
you reopen the app. If someone tries to guess your PIN, five wrong
guesses triggers a one-minute lockout. Ten gets you five minutes.
Twenty or more locks you out for an hour at a time. The counter
survives if someone force-quits the app, so brute-forcing is
genuinely slow."*

### Scene 8D: Language & Region preview (14:50 – 15:30)

**Camera**: scroll Settings to **Language & Region**. Tap. Three
dropdowns: Region → Country → App language. Change region to Asia,
country to India, language to Telugu. UI flips. Camera does a slow
sweep of the newly-translated screen.

**Voiceover**: *"Language & Region is where you pick your UI language.
Nine languages ship today: English, Spanish, French, Portuguese,
Hindi, Arabic, German, Indonesian, and Telugu. Pick region, country,
and language. If the language we show has a small amber warning
underneath, that means the translation is machine-generated and
awaiting native-speaker review. We'll come back to this in Chapter 13."*

**B-roll screens**: `SettingsScreen.swift` / `SettingsScreen.kt`,
`LanguageRegionScreen.swift` / `LanguageRegionScreen.kt`.

---

## Chapter 9 — SOS / Distress Signal (15:30 – 18:30)

**IMPORTANT**: this chapter must include an on-screen fine-print
line visible for the whole duration: *"Dramatized. If you're in
genuine danger, call your local emergency services."*

### Scene 9A: setup — add emergency contacts (15:30 – 16:00)

**Camera**: Settings → Distress Signal. Choose up to 5 emergency
contacts from your Trusted list. Toggle on *Shake to trigger* and
*Volume-down 4× to trigger* (Android only). Paste a WhatsApp group
invite URL (optional).

**Voiceover**: *"Before you can use the SOS feature, go to Settings,
Distress Signal, and pick up to five people to alert. These are
pulled from your Trusted Contacts. On Android you can also enable
shake-to-trigger or pressing volume down four times quickly, as
alternatives to tapping the button."*

### Scene 9B: the Dashboard SOS button (16:00 – 16:30)

**Camera**: back on Dashboard. The red SOS button bottom-right. Tap
it. Full-screen countdown: "3... 2... 1..." with a cancel option.

**Voiceover**: *"On the Dashboard, the red button floats bottom-right.
One tap gives you a three-second countdown. During that window, tap
Cancel if you tapped by accident. Otherwise, when the countdown ends,
the distress sequence starts."*

### Scene 9C: what happens when it fires (16:30 – 17:30)

**Camera**: countdown ends. Phone app opens — call to the first
contact starts dialing. User taps Call. Then Messages app opens with
all recipients + a pre-filled body: *"EMERGENCY — I need help. My
location: [Google Maps URL with coordinates]."* Then WhatsApp opens
with the group invite + a pre-filled distress message.

**Voiceover**: *"Four things happen in sequence. The phone app opens
and dials your first emergency contact — you tap Call. Then the
Messages app opens with every emergency contact pre-selected and a
message pre-filled with your current location. You tap Send once —
it goes to everyone. Then WhatsApp opens with the group invite you
set up, if you have one. All of this uses your phone's own apps —
nothing goes through our servers because we don't have any."*

### Scene 9D: honest limits of SOS (17:30 – 18:00)

**Camera**: close-up of the fine-print on screen.

**Voiceover**: *"Honest limits on SOS. One: you have to tap Call and
Send yourself — Apple and Google both forbid third-party apps from
silently calling or texting without a user tap. Two: your location
is from the last GPS fix your phone has — if you're in a building
with no signal, the coordinates may be minutes old. Three: this is
not a substitute for calling emergency services. If you're in
genuine physical danger, dial 911 or your local equivalent first."*

### Scene 9E: Duress mode (brief tease) (18:00 – 18:30)

**Camera**: close-up of Settings → Duress Setup. "Decoy PIN" field.
*"When someone forces you to unlock, enter this PIN. The app opens a
decoy vault AND silently sends your alert contact an SOS."*

**Voiceover**: *"Pro tier adds Duress Mode — a second, decoy PIN. If
someone physically forces you to unlock your phone, you enter the
decoy PIN. They see a fake, empty vault. Behind the scenes, a silent
alert fires to your emergency contact. We'll cover more Pro features
in Chapter 11."*

**B-roll screens**: SOS button in `MainTabs.swift`,
`DistressSettingsScreen.swift` / `DistressSettingsScreen.kt`.

---

## Chapter 10 — Smart Link Guard (18:30 – 20:30)

### Scene 10A: what it is (18:30 – 18:50)

**Camera**: a phishing SMS arrives: *"Your bank account is suspended.
Verify now: http://chase.verify-login.com"*. OnlyTrusted overlays
a Danger badge.

**Voiceover**: *"Smart Link Guard checks every URL in incoming SMS,
scanned QR codes, and anything you share from your browser or messages
app. All checks happen on your device — no URL is ever sent anywhere."*

### Scene 10B: the 12 rules (18:50 – 19:30)

**Camera**: Smart Link Guard verdict screen with a dangerous URL.
Shows a list of reasons: *"Suspicious subdomain of unknown host.
Possible homograph attack. Multiple caution flags on the same link
— escalated to Danger."* Three verdict badges: Safe (green), Caution
(amber), Danger (red).

**Voiceover**: *"The app runs twelve different checks on every link.
It flags suspicious top-level domains like dot-zip or dot-xyz. It
catches lookalike domain names — like a Cyrillic 'a' that looks
exactly like a regular 'a'. It detects when a well-known brand name
is used as a subdomain of a stranger — like 'paypal.com.scam.ru'.
It warns about embedded passwords in a URL. And when two or more
yellow caution flags fire on the same link, it escalates the verdict
to full Danger. Each verdict tells you exactly why."*

### Scene 10C: QR scanning (19:30 – 20:00)

**Camera**: Settings → Smart Link Guard → *Scan QR Code*. Camera
opens. Point at a QR code in the real world. A URL resolves. Danger
verdict. User taps *Don't open*.

**Voiceover**: *"The QR scanner uses the same twelve rules. Point your
camera at a QR code — in a restaurant, on a parking meter, in a
promotional poster — and we tell you if the URL behind it is safe
before you open it in your browser. We've all seen the parking-meter
QR-code scams in the news."*

### Scene 10D: Share-sheet integration (20:00 – 20:30)

**Camera**: long-press any URL in Safari / Chrome. System share
sheet appears. Scroll to find *OnlyTrusted — Scan this URL*. Tap.
Verdict appears.

**Voiceover**: *"The share-sheet integration lets you check any URL
from anywhere. In Safari, Chrome, Messages, Mail — long-press any
link, scroll to OnlyTrusted, tap. The verdict appears instantly. No
URL is sent to our servers, because we don't have any."*

**B-roll screens**: `SmartLinkGuardScreen.swift` /
`SmartLinkGuardScreen.kt`, `SmartLinkGuardQrActivity.kt` (Android),
`SmartLinkGuardExtension/ShareViewController.swift` (iOS).

---

## Chapter 11 — Breach Watch (20:30 – 21:30)

### Scene 11A: setup (20:30 – 21:00)

**Camera**: Settings → Breach Watch. Shows one email address
pre-filled from onboarding. Tap *Check now*. Spinner. Result card
appears: *"Found in 3 past breaches."*

**Voiceover**: *"Breach Watch tells you if your email has appeared
in any of the big public data breaches — LinkedIn 2012, Dropbox 2016,
Adobe 2013, and dozens more. The breach database is shipped inside
the app as hashed prefixes — your email is never sent anywhere. You
can check it even in airplane mode."*

### Scene 11B: what the result means (21:00 – 21:30)

**Camera**: tap on one result row. Expanded card: *"LinkedIn — May
2012. Password was in the leak. Rotate your LinkedIn password now."*
Below: *Mark as rotated* button.

**Voiceover**: *"Each result tells you which breach and when, and if
your password was part of the leaked data. Tap Mark as rotated once
you've changed your password on that site. In the Premium tier you
can watch up to five emails for your whole family."*

**B-roll screens**: `BreachWatchScreen.swift` / `BreachWatchScreen.kt`.

---

## Chapter 12 — Upgrading (21:30 – 24:00)

### Scene 12A: tier overview (21:30 – 22:00)

**Camera**: Settings → Billing. Tier matrix on screen: Free / Premium
/ Pro / Elite with Individual + Family pricing.

**Voiceover**: *"OnlyTrusted has four tiers. Free covers everything
we've seen so far — all the core safety features, forever. Premium
adds the weekly PDF digest, extra breach-watch emails, and advanced
blocking rules. Pro adds Duress mode, SIM-Swap Alarm, Smart Link
Guard, and the tracker finder. Elite adds the serious coercion-
defense features: Shamir Split recovery, Trusted Witness, Border Mode,
Panic Lockdown, hardware security keys, and more."*

### Scene 12B: Premium highlights (22:00 – 22:30)

**Camera**: montage of Weekly PDF Digest opening ("This week: 23
scams blocked, 5 phishing SMS flagged"), Breach Watch with 5 emails
listed, Advanced Blocking rules.

**Voiceover**: *"Premium is for anyone who wants a weekly report of
what the app did for them, plus coverage of their whole family's
email addresses in Breach Watch, plus custom blocking rules — like
area-code blocking or time-of-day silencing."*

### Scene 12C: Pro highlights (22:30 – 23:00)

**Camera**: close-up of Duress setup, SIM-Swap alarm, Smart Link
Guard verdict, Tracker Finder catching an AirTag.

**Voiceover**: *"Pro covers scenarios where someone's trying harder.
Duress mode protects you from being coerced to unlock. SIM-Swap
Alarm catches a specific attack where someone transfers your phone
number to their own SIM to intercept your bank's security codes.
Tracker Finder catches AirTags and other BLE trackers that someone
might have slipped into your bag."*

### Scene 12D: Elite highlights (23:00 – 23:30)

**Camera**: Shamir Split peer-recovery animation, Trusted Witness
Bluetooth pairing, Border Mode on an airplane window, hardware YubiKey.

**Voiceover**: *"Elite is for people in high-threat situations —
journalists, dissidents, domestic-abuse survivors, cross-border
travelers. Shamir Split lets you give encrypted pieces of your vault
key to five trusted friends, and three of them have to physically
meet to help you recover if you lose your phone. Border Mode
disables Face ID at an international border. Panic Lockdown kills
biometrics with a hidden gesture when someone's demanding you
unlock. All of this is coercion-aware; none of it requires a cloud."*

### Scene 12E: how to subscribe (23:30 – 24:00)

**Camera**: tap **Upgrade** → App Store / Play Store subscription
sheet. Choose Premium Individual. Confirm purchase with Face ID /
fingerprint.

**Voiceover**: *"Subscribing uses the normal App Store or Play Store
flow — no sign-up, no account, no billing details given to us
directly. You can switch tiers or cancel anytime from App Store →
Subscriptions, or Google Play → Subscriptions. If you cancel, your
vault and all your data stay — you just lose access to the premium
features."*

**B-roll screens**: Settings → Billing / Upgrade,
`UpgradePremiumScreen.swift` / `UpgradePremiumScreen.kt`.

---

## Chapter 13 — Honest Limits (24:00 – 25:30)

### Scene 13A: open the sheet (24:00 – 24:20)

**Camera**: Dashboard → amber "Honest Limits" callout → tap. Full-
screen sheet opens with a scrollable list of bullets.

**Voiceover**: *"We're almost done. Before we finish, this is the
most important screen in the app. Honest Limits — what we don't do.
Every bullet here is us telling you up-front where the app can't
help. We'd rather lose a sale than overclaim."*

### Scene 13B: scroll through the bullets (24:20 – 25:30)

**Camera**: slow scroll through the Honest Limits sheet. Each bullet
fades in:

- Cannot monitor other apps' microphone or camera at hardware level
- Cannot stop someone on a video call from recording their own screen
- Cannot block screenshots of other apps
- Cannot make your phone untrackable by your carrier
- Cannot block every scam call — carrier paths bypass us
- Cannot detect deepfake audio in real time on a live call
- Cannot auto-block Wangiri numbers on iPhone (CallKit limitation)
- Cannot read your SMS inbox on iPhone
- Cannot create a WhatsApp group programmatically
- Cannot wipe iCloud / Google Drive backups you made outside the app
- Cannot recover your PIN if you forget it and never set up Shamir Split

**Voiceover**: *"Read through this once. It's the honest answer to
'what's the catch?'. We can't monitor what other apps do with your
microphone — only the OS can. We can't block every scam call because
some reach your phone through paths our software never sees. We
can't recover your PIN because there's no cloud it's stored in. If
any of these are deal-breakers, we'd rather you know before you
depend on the app."*

---

## Chapter 14 — Changing language (25:30 – 26:30)

### Scene 14A: open Language & Region (25:30 – 25:50)

**Camera**: Settings → Language & Region. Three dropdowns visible.
Current language: English. Caption: *"English is always available."*

**Voiceover**: *"To switch languages, Settings → Language & Region.
Three dropdowns — region, country, language. English is always
available regardless of country."*

### Scene 14B: pick a language — Telugu example (25:50 – 26:30)

**Camera**: tap Region → Asia. Tap Country → India. Tap App language
dropdown — list shows Hindi, English, Bengali, Telugu, Tamil, etc.
Pick Telugu. UI flips. Camera slow sweep of the newly-Telugu screen.
A small amber warning appears below the picker: *"⚠ Translation
pending review."*

**Voiceover**: *"Pick Asia, India, then Telugu. The UI flips
immediately for most screens. If a small amber warning appears, it
means our translation is machine-generated and hasn't been reviewed
by a native speaker yet. We ship nine languages this way — English
is fully reviewed, the other eight are in review. We'd rather ship
them with a visible 'in-review' marker than wait a year to launch."*

### Scene 14C: platform notes (26:30 – 26:30 out)

Already covered — the OS-controlled bits (StoreKit, system biometric
prompt) follow device locale not our override. Noted in-passing in
previous chapter.

**B-roll screens**: `LanguageRegionScreen.swift` /
`LanguageRegionScreen.kt`.

---

## Chapter 15 — Help, support, outro (26:30 – 28:00)

### Scene 15A: Settings → About (26:30 – 27:00)

**Camera**: Settings scrolls to About. Version string, build number,
commit SHA. Support email: `admin@vanpaultek.com`. GitHub link.

**Voiceover**: *"If something's broken, or you have a question we
haven't answered, Settings → About has our support email and a link
to our open-source GitHub. You can read every line of our source code
there — the whole 'no cloud' claim is auditable by anyone."*

### Scene 15B: Where to find written docs (27:00 – 27:30)

**Camera**: open Safari / Chrome. Navigate to
`vanpaultek.github.io/OnlyTrusted/`. Pricing page + USER_GUIDE on
the website.

**Voiceover**: *"The full written user guide lives on our website at
vanpaultek-dot-github-dot-io-slash-OnlyTrusted. Every feature
documented end-to-end. And the pricing kit lives there too."*

### Scene 15C: Outro (27:30 – 28:00)

**Camera**: face-cam of the presenter wrapping up. OnlyTrusted on
the phone behind them.

**Voiceover**: *"That's the full tour. If you got value from this,
subscribe for more. If you have a question, email us. If you find a
bug, we want to know. OnlyTrusted — zero cloud, zero account, zero
compromise. See you in the next one."*

Final frame: logo lockup + tagline + **vanpaultek.com** URL.

---

## Shot-list quick reference

| Chapter | Main screens filmed |
|---|---|
| 1 Intro | Dashboard b-roll + face-cam |
| 2 Install | App Store + splash |
| 3 Setup | Welcome / Consent / Email / PIN / Passkey / Final |
| 4 Dashboard | Full Dashboard with each card highlighted |
| 5 Contacts | Vault empty state + add flow + search |
| 6 Blocked | Blocked list + add form + settings sub-page |
| 7 Events | Log empty + populated + filter + tamper banner |
| 8 Settings | Settings top + Account + App Lock + Lockout toast |
| 9 SOS | Distress Setup + Dashboard button + Countdown + Fire |
| 10 Smart Link Guard | Verdict screen + QR scanner + Share-sheet |
| 11 Breach Watch | Setup + result card + per-breach detail |
| 12 Upgrading | Billing + Premium / Pro / Elite teasers + Subscribe |
| 13 Honest Limits | Honest Limits sheet full scroll |
| 14 Language | Language & Region picker + Telugu demo |
| 15 Outro | About screen + website + face-cam |

---

## Localization note

**Master cut**: English, record this script verbatim.

**Then re-record voiceover only** in the P0 markets:
- Hindi (hi)
- Telugu (te)
- Spanish (es)

Re-record screen captures with the app set to each of those locales,
cut in with the matching voiceover. 3 localized videos total for the
launch, plus the English master. The other 5 shipped languages (fr,
pt, ar, de, id) are P1-P2 and can come post-launch as organic uploads
with "beta translation" captions until native review is done per
`docs/NEEDS_REVIEW.md`.

**Fine-print lines (bottom-of-screen tickers)** must be localized
per market along with the UI. Lift the exact phrasing from the in-
app Honest Limits sheet of the target locale so the ad can't drift.

---

## Production checklist

Before filming:

- [ ] Fresh install OnlyTrusted on both devices (iPhone 17 Pro + Pixel 7 Pro)
- [ ] Create a demo email like `demo@onlytrusted.app` so no real email leaks
- [ ] Create a demo PIN you don't care about (1-2-3-4-5-6)
- [ ] Have 5 fake trusted contacts ready in a staging contact book
- [ ] Have 3 known-benign scam numbers for live block demos (use `1-555` test numbers)
- [ ] Have a known-phishing URL for the Smart Link Guard demo (from the bundled test set — don't use real phishing URLs)
- [ ] If filming Duress / SOS, hire an actor + add dramatized title card
- [ ] If filming on a real device, factory-reset after to clear demo data

Post-production:

- [ ] Add YouTube chapters from the chapter list above
- [ ] Add Honest Limits fine-print as a lower-third ticker in any
      chapter that mentions a bounded capability
- [ ] Localized subtitle track for every shipped language
- [ ] Captions ON by default (accessibility)
- [ ] End screen with 3 related video thumbnails (Smart Link Guard
      deep dive, SOS deep dive, Shamir Split deep dive — produce as
      follow-up videos)

---

## Related docs

- `USER_GUIDE.md` — the written-text user guide (same content, text form)
- `docs/MARKETING_SHOWCASE.md` — short-form ad creative brief (15s / 30s / 60s cuts)
- `docs/guide/` — the interactive HTML guide hosted at
  vanpaultek.github.io/OnlyTrusted/guide/
- `docs/pricing/` — the live pricing page
- `docs/AUTH_MODEL.md` — reference for the Passkey architecture
  mentioned in Chapter 3
- `docs/NEEDS_REVIEW.md` — the per-locale translation review log

---

Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
