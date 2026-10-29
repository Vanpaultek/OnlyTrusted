// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.legal

object LegalContent {

    const val LAST_UPDATED = "2026-10-28"

    val audienceStatement = """
# Who this app is for

OnlyTrusted is a serious privacy tool for the phone owner and their immediate family. It is not an entertainment app, a novelty "spy" tool, or a product you install on someone else's device without their consent.

By using it you are confirming:

- You are at least 18 (or your local legal age of digital consent, whichever is higher).
- You own or administer this phone.
- You will only block calls and intercept SMS on devices you have the right to manage.
- You understand that installing this app on someone else's phone without their knowledge may be illegal in your country (stalkerware laws, wiretap laws, GDPR and similar regulations around the world).

If any of those is not true, uninstall now.

## Family data

Any family phone number, address, email or note you add to the Trusted vault is:

- Stored only on this device.
- Protected by the SQLCipher-encrypted database AND a second layer of AES-256-GCM encryption that uses a key held in Android Keystore, so even a copy of the database file cannot be read on another device.
- Visible in the UI only after a biometric or device-credential unlock.
- Deletable individually or wiped entirely from Settings, under Data, Wipe everything.

We never upload family data. We cannot. The app has no internet permission.
    """.trimIndent()

    val privacyPolicy = """
# Privacy Policy

Last updated: $LAST_UPDATED

OnlyTrusted is designed around a single principle: your data stays on your phone.

## What we collect

Nothing is collected by us. The app has no server. It does not use the internet. No account is required.

Inside your device, the app records the following, stored encrypted on-device only:

- Your in-app settings (which features are turned on, your email, your selected locale).
- Timestamps and metadata of microphone or camera use detected by Android — including which package name was recording, when it started, and whether the screen was on at the time (used by the Mic & Camera Lock feature to decide when an auto-lock would disrupt a legitimate call).
- Timestamps of calls your block list rejected and SMS senders your block list flagged.
- The phone numbers you choose to add to your trusted-contacts list OR your block list.
- A copy of your device's contact names + numbers if you grant Contacts access AND confirm the "Import N contacts" prompt on first Dashboard mount. You can un-trust any individual contact later from the Vault screen, or clear the whole set from Settings.
- A log of lock-and-unlock events, mic activity alerts, and the sensor-privacy foreground service's restart history. Used by the in-app Device Compatibility screen so you can see how aggressively your phone's OS is killing the background lock service.

All of the above is stored encrypted on this device only (SQLCipher-encrypted SQLite for structured data + EncryptedSharedPreferences / AES-256 for small settings, with keys protected by the Android Keystore). It is never transmitted off the phone by this app.

## Third parties

We do not share, sell, rent, trade, or disclose your information to any third party. We cannot. The app has no network access. We do not use analytics SDKs, advertising SDKs, crash reporters that transmit data, or tag managers. The only on-device machine-learning models (Google ML Kit face detection + barcode scanning) run entirely offline — their model files ship inside the APK and never contact Google.

If you tap the "Share" button on the Device Compatibility screen, you can send the diagnostic text yourself via email, messaging apps, or similar. That is a user-initiated action; nothing is uploaded automatically.

## Permissions and how they are used

- **Contacts**: to populate your trusted-contacts list and cross-check against an offline scam-numbers list. If you grant this permission, we will ask for your explicit confirmation (with a count of your contacts) before actually importing anything. You can decline the import and still use the app.
- **Microphone**: ONLY to detect when another app starts using the microphone (via Android's AudioRecordingCallback) so we can alert you and avoid disrupting legitimate calls when the auto-lock fires. The app itself does not record audio.
- **Camera**: ONLY for the in-app QR scanner (Smart Link Guard) and the optional Face-Change Shoulder-Surfing Lock. The app does not capture, store, or transmit images.
- **Phone + call log + call screening**: to let your block list reject unwanted incoming calls and show you an audit log of recent call activity.
- **SMS**: to filter and flag messages from numbers on your block list, and to send the SOS alert text when you trigger the Distress Signal. The app does not read, store, or forward unrelated message content elsewhere.
- **Notifications**: to post the Mic & Camera Lock status notification (required by Android for foreground services), mic-activity alerts (if you opted in), and SOS confirmations.
- **Battery optimization exemption**: to keep the Mic & Camera Lock service running when the screen is off; without it, your phone's OS will kill the service and the lock will not fire.
- **Location**: not requested by default. (The optional Geo-fence Auto-Lock Elite feature requests location only if you enable it.)
- **Internet**: not requested.

## Your rights

You have the right to access, correct, export, restrict, and delete your data at any time. Because the data lives on this device, you exercise these rights directly:

- Access: see every event in the Events screen, every blocked number in the Blocked screen, every imported contact in the Vault screen, and your sensor-privacy history in the Mic & Camera Lock screen.
- Export: Settings → Export sends an encrypted JSON copy to a location you choose.
- Delete: Settings → Clear event history, un-trust any contact from Vault, or uninstall the app to remove everything.

These rights apply regardless of where you live. We honour, at minimum, protections required by the EU GDPR, UK GDPR, California CCPA/CPRA, Virginia VCDPA, Colorado CPA, Connecticut CTDPA, Utah UCPA, Brazil LGPD, Canada PIPEDA, Australia Privacy Act, Japan APPI, Singapore PDPA, South Korea PIPA, India DPDP Act 2023, and South Africa POPIA.

## Open source

OnlyTrusted is licensed under GNU AGPL-3.0. Source code is available at https://github.com/Vanpaultek/OnlyTrusted — the "View source" link at the bottom of Settings opens this directly. You can verify every claim in this policy by reading the code yourself.

## Children

OnlyTrusted is not directed at children under 13 (or the relevant minimum age in your jurisdiction, such as 16 under GDPR). We do not knowingly collect information from children. Because we collect nothing, this is already enforced.

## Changes to this policy

If the policy changes, the updated date at the top will change and the full text will be shipped inside a new version of the app. There is no hidden policy, no linked external page that could change without notice, and no "we may update this at any time" clause that lets us modify terms behind your back.

## Contact

Questions, concerns, or data-rights requests: admin@vanpaultek.com (VanPaulTek Inc).
    """.trimIndent()

    val termsOfUse = """
# Terms of Use

Last updated: $LAST_UPDATED

By installing OnlyTrusted you agree to the following plain-language terms.

## What this app is

A privacy and anti-nuisance tool for Android. It alerts you when apps use your microphone or camera, blocks calls and SMS from numbers you add to your block list, warns on likely pocket-dials, and protects its own screens from screenshots.

## What this app is not

- It is not a replacement for the system security features built into Android.
- It cannot stop a determined attacker, a system app, or a privileged app from using your microphone or camera.
- It cannot stop the person on the other end of a video call from recording their own screen or photographing it.
- It cannot block screenshots of other apps.
- It cannot make your phone untrackable by your carrier, your Internet provider, or any OS vendor telemetry.

If any marketing you see for this app contradicts these points, the marketing is wrong. These terms govern.

## Your responsibilities

- You will only block numbers and intercept calls on devices you own or are authorised to administer.
- You will keep your device operating system up to date; the app relies on Android's own security model.
- You will not resell, redistribute, or sublicense the app without written permission.

## Our responsibilities

- The app will not transmit your data off the device.
- We will clearly disclose every data-handling practice in the Privacy Policy inside the app.
- We will not ship silent updates that change the data-handling practices without a bumped version and an updated "last updated" date visible to you.

## Pricing

If a price is shown in the store listing, that is the full price. There are no hidden recurring charges, no auto-upgrading tiers, and no features that start free and later become locked unless the store listing says so clearly. A one-time "lifetime" purchase means for the lifetime of the app's active maintenance; if we discontinue the app we will say so and refund where required by law.

## Warranty and limits

The app is provided "as is." We make no promise that it will catch every microphone or camera use (Android does not expose everything), that call screening will reject every unwanted call (that depends on your carrier and Android), or that the app will run uninterrupted on every manufacturer's custom Android build.

To the maximum extent allowed by applicable law, our liability is limited to the amount you paid for the app. Nothing in these terms limits liability we cannot limit by law in your country, such as liability for death, personal injury caused by negligence, fraud, or statutory consumer-protection rights.

## Governing law and disputes

These terms are governed by the laws of the country where you are resident, to the extent your local consumer-protection law applies. Nothing here waives mandatory rights you have under local law.

## Contact

admin@vanpaultek.com
    """.trimIndent()

    val callEncryptionExplainer = """
# About call encryption

A lot of apps claim to "encrypt your calls." Most of the time that claim is sloppy or misleading. Here is the honest version.

## Cellular voice/video (your carrier)

When you place a normal call from your phone's dialer, the audio runs through your mobile carrier (GSM / VoLTE / VoNR / 5G). That channel is encrypted *by the carrier*, between your phone and the cell tower, with keys the carrier owns. **No third-party app can add end-to-end encryption to that path.** Any app that says it can is lying. The moment the audio leaves your phone, it is in the carrier's hands.

OnlyTrusted does not touch cellular calls. We screen them, we block spam, we silence untrusted callers, we watermark the screen. We do not and cannot encrypt them.

## Voice / video calls over the internet

For *end-to-end encrypted* calls you need an app whose job it is to carry the audio itself:

- **Signal**: the strongest option. E2E on every call, metadata-minimising, open-source.
- **WhatsApp**: E2E on every call by default (uses the Signal protocol).
- **FaceTime**: E2E, Apple-only, so both sides need an Apple device.
- **iMessage audio/video**: E2E, Apple-only.

Use those for the call. Use OnlyTrusted on the same phone for everything around the call: watermark on the screen, Trusted-only filter blocking everyone outside your family while you talk, Shield log recording the session code.

## What OnlyTrusted does protect

- **Your family vault**: SQLCipher-encrypted database plus a second AES-256-GCM layer with a key held in Android Keystore.
- **Your audit log**: SHA-256 hash chain plus the same SQLCipher encryption.
- **Your evidence pack**: signed with a SHA-256 of the pack contents.
- **Your screen**: FLAG_SECURE on every screen we own, plus the Shield watermark on top of other apps if you ask for it.
    """.trimIndent()

    val honestClaims = """
# Honest claims

A short list of what this app can and cannot do, so you set realistic expectations.

## It can

- Notify you (notification + vibration) when another app begins using the microphone or camera, and log the event locally.
- Block incoming calls from numbers in your block list, provided you set it as the default call screening app in system settings.
- Flag incoming SMS from numbers in your block list.
- Detect likely pocket positions and warn before placing a call.
- Protect its own on-screen content from screenshots and screen recording.
- Keep every piece of data on your device only, encrypted at rest.

## It cannot

- Prevent any app from using the microphone or camera at a hardware level without root or a hardened OS.
- Stop a person on the other side of a video call from recording their own device.
- Block screenshots of your home screen, other apps, or system UI.
- Hide your phone from the cellular network, your carrier, or your ISP.
- Guarantee that every spam call will be caught. Some carrier-level call paths bypass Android call screening.
- Replace security updates, hardened operating systems, or careful app-permission hygiene.
- Detect in-call voice-cloning / deepfake audio in real time. Platforms do not expose a third-party app's access to the live call audio stream. If a cloned-voice scam is a concern for you, pre-share a secret word with family and ask for it on any emergency call.

## Threat Intel feed status (reality, as of this release)

- **Phishing hosts** (`phishing_urls.v1.txt`): ~50 000 real entries, sampled from the public Mitchell Krogza Phishing.Database aggregator + PhishTank, refreshed via `tools/refresh_threatintel.sh` in the repo. Shipped bundle is a build-time snapshot — no live refresh.
- **Scam phone numbers** (`scam_numbers.v1.txt`): currently a 41-entry hand-curated list of publicly-documented scam ranges. There is no free bulk source for global scam-number feeds — the comprehensive ones (Nomorobo, Hiya, Truecaller) are commercial. We'd rather admit this than ship an empty-coverage feed labeled "comprehensive".
- **Breach hashes** (`breach_hashes.v1.txt`): a 37-entry placeholder today; a top-N shipment from Have-I-Been-Pwned is the honest next step and not yet wired.

The honest claim ("no cloud, no server, no account, zero-network") is preserved — all feeds are bundled at build time; the runtime never phones home.

If anything inside this app or in its listing suggests otherwise, treat this page as the source of truth.
    """.trimIndent()

    val nonInterferencePledge = """
# What this app will never do

We built OnlyTrusted to protect the owner's phone. That means we deliberately refuse to do things that would turn it into the thing it is defending against.

1. **We never change system settings for you.** We only read them (USB-debugging state, lock-screen state, developer options, unknown-sources permission) and show you a page in Android's settings where you flip the switch yourself.
2. **We never modify, kill, or interfere with other apps.** The services watch sensors and callbacks; they do not stop other apps from running, do not revoke their permissions, do not deny them network, do not inject into them. We refuse Device Admin and Device Owner escalation.
3. **We never scan or probe anyone else's device, network, or account.** No port scans, no WHOIS, no reverse lookups against third-party reputation services, no OSINT crawls, and not even against people who called you.
4. **We never "hack back."** If an attacker tries to compromise your phone, we observe, record, and alert. We do not attempt to identify them beyond what the phone number or installed-app list already shows locally. If you want someone identified, use the Evidence pack export in Settings and give it to your carrier, your bank, or law enforcement. They have the legal authority to investigate; we do not.
5. **We never phone home.** The manifest does not request the internet permission. There is no server, no cloud, no analytics, no crash reporter that transmits data.
6. **We never run on someone else's phone without their knowledge.** The onboarding gate asks you to confirm you own or administer this device; the Audience page restates this. Installing OnlyTrusted on another adult's phone without their consent is stalkerware regardless of your relationship to them and is illegal in most countries.
    """.trimIndent()

    val dataPrinciples = """
# Our data principles

These are not aspirations. They are the only ways this app is written to behave.

1. Collect nothing we don't need. We only record events needed to show you alerts and keep your block list working.
2. Keep it on your device. No cloud, no server, no analytics, no ads. The app does not request the internet permission.
3. Encrypt at rest. The event database and the key passphrase live inside Android Keystore-protected encrypted storage.
4. Make deletion one tap. "Clear event history" removes the whole log immediately; uninstalling removes everything else.
5. Make export one tap. You can export your data as JSON to a folder you choose at any time.
6. Keep policies in the binary. The Privacy Policy and these principles ship inside the app, with a visible "last updated" date, so you do not have to trust a webpage that could be edited silently.
7. Open about limits. The "Honest claims" page lists every protection the app cannot provide, in plain language.
8. Minimum age. The app is not for children under 13 (or the local minimum, whichever is higher).
9. No tracking. We don't fingerprint, don't attribute, don't do any kind of cross-app or cross-device tracking.
10. No dark patterns. No fake urgency, no hidden auto-renewals, no misleading permission prompts.
    """.trimIndent()

    val accessibilityStatement = """
# Accessibility

OnlyTrusted targets WCAG 2.1 AA (which is also what US Section 508 adopts). Specifically, this app:

## What we commit to

- Color contrast ≥ 4.5:1 for normal text and ≥ 3:1 for large text, icons, and dividers. Every Material color role (primary, onSurface, onSurfaceVariant, outline) has been overridden to a tested AA-compliant value instead of relying on Material You's dynamic palette.
- Text scaling: every text style uses sp units so Settings, under Display, Font size and Display size scale them. The app does not pin text to fixed dp.
- TalkBack: every IconButton has a contentDescription. Decorative icons use contentDescription = null so they are not announced.
- Hit targets ≥ 48×48 dp on every tappable control, per Material 3 defaults.
- Honors system settings: Reduce Motion, Bold Text, High Contrast Text, and Dark Mode all work without any in-app override needed.

## What we do not claim

- Formal Section 508 VPAT certification requires an independent accessibility consultant to audit the shipping build. We have not engaged one. The commitments above are self-measured.
- Android versions below our minSdk of 29 are not supported.

## Report an issue

If anything on any screen is hard to use with TalkBack, Switch Access, a large font size, or in High Contrast mode, email admin@vanpaultek.com and we will treat it as a priority fix. Include the screen name and your Font size + Display size setting if possible.
    """.trimIndent()

    data class LegalSection(val title: String, val body: String)

    val duressAndLaw = """
# Duress Mode & law

Duress Mode is a Pro feature designed for the real-world situation where you cannot refuse to unlock your phone — a mugger, an abusive partner, an unlawful demand at a border. The secondary PIN unlocks a decoy vault that looks like a tidy but boring list of trusted contacts, and sends a silent SOS text to one trusted recipient so they know what just happened.

## What it is for

- Protecting the location of your real family roster from someone who physically has your phone and your attention.
- Buying you time: the coercer sees a believable, dull vault; your emergency contact gets a signal.
- Giving you a way out of a situation without having to explain anything on screen.

## What it is NOT for

- Hiding information from a lawful search where you have a legal obligation to disclose. Depending on your country, lying to law enforcement, obstructing a border inspection, or destroying evidence may be a crime. OnlyTrusted does not advise you on when any of those apply to you.
- Operating on someone else's phone. The owner must set up Duress Mode themselves.

## Legal reality

Laws about compelled unlock, decoy data, and whether you must disclose a second PIN differ widely:

- US — the current body of case law is mixed. Several federal and state courts treat biometric compulsion differently from passcode compulsion. There is no universal answer.
- UK — RIPA s.49 can require you to disclose a key or passphrase; refusal is itself an offence.
- EU — member states vary. Border searches and compelled-disclosure powers differ by country.
- Many other jurisdictions — unknown or evolving.

Before you enable Duress Mode, check what your local law actually permits. If you are being coerced by an abusive partner or trafficker, your safest step is usually to contact a domestic-violence or anti-trafficking hotline that knows your country's legal landscape, not to rely on any app alone.

## What this app will NOT do

- We will not help an attacker, a stalker, or a trafficker use Duress Mode to conceal activity from a victim.
- We will not add network callbacks that would phone home when duress is triggered. Alerting runs through `SmsManager` on this device, with the owner's chosen recipient; nothing goes to us.
- We will not change the behaviour of Duress Mode in a silent update. If the mechanism changes, the "last updated" date above moves and the full text ships inside the new version of the app.
    """.trimIndent()

    val borderModeAndLaw = """
# Border Mode & law

Border Mode is an Elite preset designed for high-risk crossings — international borders, airports where you may be pulled aside, protests where a device could be seized, hostile territory. One tap stacks every defensive layer OnlyTrusted ships: Dead-Man Wipe forced to 7 days, Duress Mode active, biometric unlock disabled (PIN-only), screenshot blocking and shield watermark on, pocket-dial protection on, auto-lock at 30 seconds, and — if you set one up — a plausible "cover identity" shown on the Dashboard and Vault.

## Why PIN-only matters (US courts)

In most United States federal and state cases, courts have treated unlocking a device by passcode as "testimonial" — potentially protected by the Fifth Amendment right against self-incrimination — while unlocking by face, fingerprint or similar biometric has been treated as a physical act and compelled routinely. The case law is not uniform (Minnesota, Vermont, Virginia and the Ninth Circuit have gone different ways over the past decade), and it keeps evolving. Border Mode's PIN-only switch exists so you have the stronger argument available at the moment it matters. It is not legal advice and it does not guarantee any specific outcome.

## What Border Mode is NOT for

- Evading a lawful search, border inspection, or court order. In many jurisdictions, lying to a border officer, destroying evidence after a subpoena, or obstructing an investigation is itself a crime. OnlyTrusted does not advise you on when any of those apply.
- Hiding activity from a victim or a court-ordered monitor.
- Running on a device you don't own, or on behalf of another person.

## Jurisdictional reality

- US — border officers have broad authority to inspect electronic devices at the border. Refusing to unlock can lead to device seizure for forensic examination. Fifth Amendment protection for passcodes is case-law-dependent and not universal.
- UK — Schedule 7 of the Terrorism Act and RIPA s.49 can require you to disclose a passphrase; refusal is itself an offence.
- EU — member states vary. Some require disclosure, some don't.
- Many other jurisdictions — unknown or hostile. Research your destination before you travel.

## Practical reality

Border Mode will not stop a determined forensic lab from imaging your device if your hardware is unlocked or if the authorities have the time and tools. What it does is raise the per-incident cost for a casual search: biometric compulsion is off the table, screenshots and shoulder-surfing are harder, and if the device sits in evidence lockup for more than a week it wipes itself.

## What this app will NOT do

- We will not help someone cross a border fraudulently or evade a lawful warrant.
- We will not add network callbacks that would phone home when Border Mode is armed. Everything stays on device.
- We will not change the behaviour of Border Mode in a silent update. If the mechanism changes, the "last updated" date above moves and the full text ships inside the new version of the app.
    """.trimIndent()

    val panicLockdownAndLaw = """
# Panic Lockdown & law

Panic Lockdown is an Elite feature designed for the moment when you are already in trouble — a mugging, a kidnapping attempt, an abusive partner demanding your phone, an unlawful seizure in progress. Where Border Mode is a planned, triple-confirmation preset for a crossing you know is coming, Panic Lockdown is a single-tap red button for the emergency happening right now. One tap locks the app, arms Duress Mode so the next unlock drops into the decoy vault and sends the silent SOS, forces aggressive sensor monitoring, disables the Smart Link Guard watcher so nothing leaks out of the app, disables biometric unlock (PIN-only), and timestamps the moment in the audit log.

## What it is for

- A self-defence tool when you cannot safely explain what you are doing on your phone.
- Buying you a hand-off: the attacker sees the decoy vault; your emergency contact gets a signal.
- Preserving the Fifth-Amendment advantage in US courts by removing biometric unlock from the picture at the exact moment it matters.

## What it is NOT for

- Obstructing a lawful search, border inspection, warrant, subpoena, or court order. In many jurisdictions, destroying evidence after service of legal process, lying to officers, or interfering with an investigation is itself a crime. OnlyTrusted does not advise you on when any of those apply to your situation.
- Hiding activity from a victim, a court-ordered monitor, or anyone with a lawful right to inspect the device.
- Running on a device you do not own or administer.

## Legal reality

- US — passcode compulsion case law is mixed and jurisdiction-dependent. Biometric compulsion has generally been treated as a physical act and compelled routinely; passcodes have more often been treated as testimonial and protected. The Fifth Amendment advantage of switching to PIN-only is a defensive posture, not a guarantee.
- UK — RIPA s.49 and Schedule 7 of the Terrorism Act can require you to disclose a passphrase; refusal is itself an offence.
- EU — member states vary. Some require disclosure, some don't.
- Many other jurisdictions — unknown or hostile. Research your destination and the authority you may encounter before you rely on this feature.

## Practical reality

Panic Lockdown raises the per-incident cost of a casual coercive search: biometric compulsion is off the table, the share-side components cannot leak data, and the next unlock looks like a plausible-but-empty vault. It will not stop a determined forensic lab from imaging unlocked hardware, and it will not help you if you are legally obligated to disclose a passphrase in your jurisdiction.

## What this app will NOT do

- We will not help someone evade a lawful warrant, destroy evidence after legal process has been served, or obstruct an investigation.
- We will not add network callbacks that would phone home when Panic Lockdown is armed. Everything stays on device. The silent SOS runs through `SmsManager` with the owner's chosen recipient; nothing goes to us.
- We will not change the behaviour of Panic Lockdown in a silent update. If the mechanism changes, the "last updated" date above moves and the full text ships inside the new version of the app.
    """.trimIndent()

    val sections = listOf(
        LegalSection("Audience", audienceStatement),
        LegalSection("Non-interference", nonInterferencePledge),
        LegalSection("Call encryption", callEncryptionExplainer),
        LegalSection("Privacy policy", privacyPolicy),
        LegalSection("Terms of use", termsOfUse),
        LegalSection("Honest claims", honestClaims),
        LegalSection("Data principles", dataPrinciples),
        LegalSection("Accessibility", accessibilityStatement),
        LegalSection("Duress Mode & law", duressAndLaw),
        LegalSection("Border Mode & law", borderModeAndLaw),
        LegalSection("Panic Lockdown & law", panicLockdownAndLaw)
    )
}
