// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import Foundation

enum LegalContent {
    static let lastUpdated = "2026-10-20"

    struct Section: Identifiable {
        let title: String
        let body: String
        var id: String { title }
    }

    static let sections: [Section] = [
        Section(title: "Audience", body: audience),
        Section(title: "Non-interference", body: nonInterference),
        Section(title: "Call encryption", body: callEncryption),
        Section(title: "Privacy policy", body: privacyPolicy),
        Section(title: "Terms of use", body: termsOfUse),
        Section(title: "Honest claims", body: honestClaims),
        Section(title: "Data principles", body: dataPrinciples),
        Section(title: "Accessibility", body: accessibility),
        Section(title: "Duress Mode & law", body: duressAndLaw),
        Section(title: "Border Mode & law", body: borderModeAndLaw),
        Section(title: "Panic Lockdown & law", body: panicLockdownAndLaw)
    ]

    static let panicLockdownAndLaw = """
    Panic Lockdown is an Elite feature designed for the moment when you are already in trouble — a mugging, a kidnapping attempt, an abusive partner demanding your phone, an unlawful seizure in progress. Where Border Mode is a planned, triple-confirmation preset for a crossing you know is coming, Panic Lockdown is a single-tap red button for the emergency happening right now. One tap locks the app, arms Duress Mode so the next unlock drops into the decoy vault and sends the silent SOS, forces aggressive sensor monitoring, disables the share extension so nothing leaks out of the app, disables biometric unlock (PIN-only), and timestamps the moment in the audit log.

    What it is for

    - A self-defence tool when you cannot safely explain what you are doing on your phone.
    - Buying you a hand-off: the attacker sees the decoy vault; your emergency contact gets a signal.
    - Preserving the Fifth-Amendment advantage in US courts by removing biometric unlock from the picture at the exact moment it matters.

    What it is NOT for

    - Obstructing a lawful search, border inspection, warrant, subpoena, or court order. In many jurisdictions, destroying evidence after service of legal process, lying to officers, or interfering with an investigation is itself a crime. OnlyTrusted does not advise you on when any of those apply to your situation.
    - Hiding activity from a victim, a court-ordered monitor, or anyone with a lawful right to inspect the device.
    - Running on a device you do not own or administer.

    Legal reality

    - US — passcode compulsion case law is mixed and jurisdiction-dependent. Biometric compulsion has generally been treated as a physical act and compelled routinely; passcodes have more often been treated as testimonial and protected. The Fifth Amendment advantage of switching to PIN-only is a defensive posture, not a guarantee.
    - UK — RIPA s.49 and Schedule 7 of the Terrorism Act can require you to disclose a passphrase; refusal is itself an offence.
    - EU — member states vary. Some require disclosure, some don't.
    - Many other jurisdictions — unknown or hostile. Research your destination and the authority you may encounter before you rely on this feature.

    Practical reality

    Panic Lockdown raises the per-incident cost of a casual coercive search: biometric compulsion is off the table, the share extension cannot leak data, and the next unlock looks like a plausible-but-empty vault. It will not stop a determined forensic lab from imaging unlocked hardware, and it will not help you if you are legally obligated to disclose a passphrase in your jurisdiction.

    What this app will NOT do

    - We will not help someone evade a lawful warrant, destroy evidence after legal process has been served, or obstruct an investigation.
    - We will not add network callbacks that would phone home when Panic Lockdown is armed. Everything stays on device. The silent SOS runs through the local SMS composer with the owner's chosen recipient; nothing goes to us.
    - We will not change the behaviour of Panic Lockdown in a silent update. If the mechanism changes, the "last updated" date above moves and the full text ships inside the new version of the app.
    """

    static let borderModeAndLaw = """
    Border Mode is an Elite preset designed for high-risk crossings — international borders, airports where you may be pulled aside, protests where a device could be seized, hostile territory. One tap stacks every defensive layer OnlyTrusted ships: Dead-Man Wipe forced to 7 days, Duress Mode active, biometric unlock disabled (PIN-only), screenshot blocking and shield watermark on, auto-lock at 30 seconds, and — if you set one up — a plausible "cover identity" shown on the Dashboard and Vault.

    Why PIN-only matters (US courts)

    In most United States federal and state cases, courts have treated unlocking a device by passcode as "testimonial" — potentially protected by the Fifth Amendment right against self-incrimination — while unlocking by face, fingerprint or similar biometric has been treated as a physical act and compelled routinely. The case law is not uniform (Minnesota, Vermont, Virginia and the Ninth Circuit have gone different ways over the past decade), and it keeps evolving. Border Mode's PIN-only switch exists so you have the stronger argument available at the moment it matters. It is not legal advice and it does not guarantee any specific outcome.

    What Border Mode is NOT for

    - Evading a lawful search, border inspection, or court order. In many jurisdictions, lying to a border officer, destroying evidence after a subpoena, or obstructing an investigation is itself a crime. OnlyTrusted does not advise you on when any of those apply.
    - Hiding activity from a victim or a court-ordered monitor.
    - Running on a device you don't own, or on behalf of another person.

    Jurisdictional reality

    - US — border officers have broad authority to inspect electronic devices at the border. Refusing to unlock can lead to device seizure for forensic examination. Fifth Amendment protection for passcodes is case-law-dependent and not universal.
    - UK — Schedule 7 of the Terrorism Act and RIPA s.49 can require you to disclose a passphrase; refusal is itself an offence.
    - EU — member states vary. Some require disclosure, some don't.
    - Many other jurisdictions — unknown or hostile. Research your destination before you travel.

    Practical reality

    Border Mode will not stop a determined forensic lab from imaging your device if your hardware is unlocked or if the authorities have the time and tools. What it does is raise the per-incident cost for a casual search: biometric compulsion is off the table, screenshots and shoulder-surfing are harder, and if the device sits in evidence lockup for more than a week it wipes itself.

    What this app will NOT do

    - We will not help someone cross a border fraudulently or evade a lawful warrant.
    - We will not add network callbacks that would phone home when Border Mode is armed. Everything stays on device.
    - We will not change the behaviour of Border Mode in a silent update. If the mechanism changes, the "last updated" date above moves and the full text ships inside the new version of the app.
    """

    static let duressAndLaw = """
    Duress Mode is a Pro feature designed for the real-world situation where you cannot refuse to unlock your phone — a mugger, an abusive partner, an unlawful demand at a border. The secondary PIN unlocks a decoy vault that looks like a tidy but boring list of trusted contacts, and sends a pre-written SOS text to one trusted recipient so they know what just happened.

    What it is for

    • Protecting the location of your real family roster from someone who physically has your phone and your attention.
    • Buying you time: the coercer sees a believable, dull vault; your emergency contact gets a signal.
    • Giving you a way out of a situation without having to explain anything on screen.

    What it is NOT for

    • Hiding information from a lawful search where you have a legal obligation to disclose. Depending on your country, lying to law enforcement, obstructing a border inspection, or destroying evidence may be a crime. OnlyTrusted does not advise you on when any of those apply to you.
    • Operating on someone else's phone. The owner must set up Duress Mode themselves.

    Legal reality

    Laws about compelled unlock, decoy data, and whether you must disclose a second PIN differ widely:

    • US — the current body of case law is mixed. Several federal and state courts treat biometric compulsion differently from passcode compulsion. There is no universal answer.
    • UK — RIPA s.49 can require you to disclose a key or passphrase; refusal is itself an offence.
    • EU — member states vary. Border searches and compelled-disclosure powers differ by country.
    • Many other jurisdictions — unknown or evolving.

    Before you enable Duress Mode, check what your local law actually permits. If you are being coerced by an abusive partner or trafficker, your safest step is usually to contact a domestic-violence or anti-trafficking hotline that knows your country's legal landscape, not to rely on any app alone.

    What this app will NOT do

    • We will not help an attacker, a stalker, or a trafficker use Duress Mode to conceal activity from a victim.
    • We will not add network callbacks that would phone home when duress is triggered. Alerting runs through the SMS composer on this device, with the owner's chosen recipient; nothing goes to us.
    • We will not change the behaviour of Duress Mode in a silent update. If the mechanism changes, the "last updated" date above moves and the full text ships inside the new version of the app.
    """

    static let audience = """
    OnlyTrusted is a serious privacy tool for the phone owner and their immediate family. It is not an entertainment app, a novelty "spy" tool, or a product you install on someone else's device without their consent.

    By using it you are confirming:
    • You are at least 18 (or your local legal age of digital consent, whichever is higher).
    • You own or administer this phone.
    • You will only block calls and intercept SMS on devices you have the right to manage.
    • You understand that installing this app on someone else's phone without their knowledge may be illegal in your country (stalkerware laws, wiretap laws, GDPR and similar regulations around the world).

    If any of those is not true, uninstall now.

    Family data

    Any family phone number, address, email or note you add to the Trusted vault is:
    • Stored only on this device.
    • Protected by Keychain-backed AES-256-GCM encryption via Apple CryptoKit.
    • Visible in the UI only after a Face ID / Touch ID / passcode unlock.
    • Deletable individually or wiped entirely from Settings, Data.

    We never upload family data. The app has no networking code and no entitlements that would permit it.
    """

    static let nonInterference = """
    We built OnlyTrusted to protect the owner's phone. That means we deliberately refuse to do things that would turn it into the thing it is defending against.

    1. We never change system settings for you. We only observe, and we deep-link into Settings where the owner flips the switch.
    2. We never scan or probe anyone else's device, network, or account.
    3. We never "hack back." If an attacker tries to compromise your phone, we observe, record, and alert. We do not attempt to identify them beyond what the phone number already shows locally.
    4. We never phone home. There is no server, no cloud, no analytics, no crash reporter that transmits data. The app has no App Transport Security exception entries for any domain because it never contacts any.
    5. We never run on someone else's phone without their knowledge. Installing OnlyTrusted on another adult's phone without their consent is stalkerware regardless of your relationship to them and is illegal in most countries.
    """

    static let callEncryption = """
    A lot of apps claim to "encrypt your calls." Most of the time that claim is sloppy or misleading. Here is the honest version.

    Cellular voice/video (your carrier)
    When you place a normal call from your phone's dialer, the audio runs through your mobile carrier (GSM / VoLTE / VoNR / 5G). That channel is encrypted by the carrier, between your phone and the cell tower, with keys the carrier owns. No third-party app can add end-to-end encryption to that path. Any app that says it can is lying.

    OnlyTrusted does not touch cellular calls. We screen them via a Call Directory Extension and flag spam, but we do not and cannot encrypt them.

    Voice / video calls over the internet
    For end-to-end encrypted calls use Signal, WhatsApp, FaceTime, or iMessage audio/video. Use those for the call. Use OnlyTrusted on the same phone for everything around the call.

    What OnlyTrusted does protect
    • Your family vault: Keychain + AES-256-GCM via CryptoKit.
    • Your audit log: SHA-256 hash chain.
    """

    static let privacyPolicy = """
    Last updated: \(lastUpdated)

    OnlyTrusted is designed around a single principle: your data stays on your phone.

    What we collect
    Nothing. The app has no server. It does not use the internet. No account is required.

    Inside your device, the app records your trusted-contact vault and your in-app settings. All of this is stored encrypted on this device only (Keychain-protected AES-256-GCM keys via Apple CryptoKit). It is never transmitted off the phone by this app.

    Third parties
    We do not share, sell, rent, trade, or disclose your information to any third party. We cannot. The app has no network capability.

    Permissions and how they are used
    • Face ID / Touch ID / passcode: to unlock the vault and (optionally) the app itself.
    • Contacts, Location, Microphone, and Camera: not requested.

    Your rights
    You have the right to access, correct, export, restrict, and delete your data at any time. These rights apply regardless of where you live. We honour, at minimum, protections required by the EU GDPR, UK GDPR, California CCPA/CPRA, Virginia VCDPA, Colorado CPA, Connecticut CTDPA, Utah UCPA, Brazil LGPD, Canada PIPEDA, Australia Privacy Act, Japan APPI, Singapore PDPA, South Korea PIPA, India DPDP Act 2023, and South Africa POPIA.

    Children
    OnlyTrusted is not directed at children under 13.

    Contact
    admin@vanpaultek.com (VanPaulTek Inc).
    """

    static let termsOfUse = """
    Last updated: \(lastUpdated)

    What this app is
    A privacy and anti-nuisance tool for iOS. It provides an encrypted family contact vault, a biometric app lock, a Call Directory Extension that blocks numbers you add to your block list, and an SMS filter for unknown senders.

    What this app is not
    • It is not a replacement for the system security features built into iOS.
    • It cannot monitor microphone or camera use by other apps on iOS.
    • It cannot stop the person on the other end of a video call from recording their own screen.
    • It cannot draw an overlay watermark on top of other apps (iOS does not allow this).
    • It cannot make your phone untrackable by your carrier.

    If any marketing you see for this app contradicts these points, the marketing is wrong. These terms govern.

    Our responsibilities
    The app will not transmit your data off the device. We will clearly disclose every data-handling practice in the Privacy Policy inside the app.

    Warranty and limits
    The app is provided "as is." To the maximum extent allowed by applicable law, our liability is limited to the amount you paid for the app.

    Contact
    admin@vanpaultek.com (VanPaulTek Inc).
    """

    static let honestClaims = """
    A short list of what this app can and cannot do on iOS.

    It can
    • Store family contacts, numbers, addresses, and notes in an encrypted vault.
    • Protect the vault behind Face ID / Touch ID / passcode.
    • Keep every piece of data on your device only.
    • Panic-lock itself at any time.

    It cannot
    • Monitor other apps' microphone or camera use. iOS reserves this for its own status bar indicator.
    • Draw a watermark overlay on top of other apps. iOS does not allow it.
    • Enumerate other apps' permissions. iOS does not expose this.
    • Encrypt cellular voice/video calls. Nobody but your carrier can.
    • Programmatically add a Wangiri (one-ring) scammer's number to iOS's block list. iOS CallKit does NOT expose incoming caller numbers to third-party apps. OnlyTrusted on iOS DETECTS the one-ring pattern and alerts you, but you must add the number to the block list manually by long-pressing it in the Recents list → Block Contact.
    • Detect in-call voice-cloning / deepfake audio in real time. Apple does not expose a third-party app's access to the live call audio stream. If a cloned-voice scam is a concern for you, pre-share a secret word with family and ask for it on any emergency call.

    Threat Intel feed status (reality, as of this release)
    • Phishing hosts (phishing_urls.v1.txt): ~50 000 real entries, sampled from the public Mitchell Krogza Phishing.Database + PhishTank aggregators, refreshed via tools/refresh_threatintel.sh in the repo. Build-time snapshot; the runtime never phones home.
    • Scam phone numbers (scam_numbers.v1.txt): currently a 41-entry hand-curated list of publicly documented scam ranges. The free bulk sources for global scam-number feeds do not exist — the comprehensive ones (Nomorobo, Hiya, Truecaller) are commercial. We'd rather admit this than ship an empty-coverage feed labeled "comprehensive".
    • Breach hashes (breach_hashes.v1.txt): a 37-entry placeholder today; a top-N shipment from Have-I-Been-Pwned is the honest next step and not yet wired.

    If anything inside this app or in its listing suggests otherwise, treat this page as the source of truth.
    """

    static let dataPrinciples = """
    1. Collect nothing we don't need.
    2. Keep it on your device. No cloud, no server, no analytics, no ads. The app has no networking entitlements.
    3. Encrypt at rest with Keychain-held keys.
    4. Make deletion one tap.
    5. Make export one tap.
    6. Keep policies in the binary. The Privacy Policy and these principles ship inside the app with a visible "last updated" date.
    7. Open about limits.
    8. Minimum age 13 (or local minimum, whichever is higher).
    9. No tracking.
    10. No dark patterns.
    """

    static let accessibility = """
    OnlyTrusted targets WCAG 2.1 AA (which is also what US Section 508 adopts). Specifically, this app:

    What we commit to

    • Color contrast ≥ 4.5:1 for normal text and ≥ 3:1 for large text, icons, and dividers. All in-app text tokens have been measured against both light and dark surfaces and against Increase Contrast mode.
    • Dynamic Type: every text style scales when you change Settings, Display & Brightness, Text Size, up to and including the Accessibility sizes (XXXL and larger). We do not pin text to fixed point sizes.
    • VoiceOver: every button has a human-readable label. Decorative icons are hidden from VoiceOver so it doesn't read "shield shield shield" at you.
    • Hit targets ≥ 44×44 points on every tappable control, per Apple HIG.
    • Honors system settings: Reduce Motion, Bold Text, Increase Contrast, and Dark Mode all work without any in-app override needed.

    What we do not claim

    • Formal Section 508 VPAT certification requires an independent accessibility consultant to audit the shipping build. We have not engaged one. The commitments above are self-measured.
    • Older iOS versions (below our minimum deployment target of iOS 17) are not supported.
    • Third-party extensions (Call Directory, SMS filter) rely on iOS's own system UI for enablement. those pages are Apple's, not ours.

    Report an issue

    If anything on any screen is hard to use with VoiceOver, Switch Control, a large text size, or in Increase Contrast mode, email admin@vanpaultek.com and we will treat it as a priority fix. Include the screen name and your iOS text-size setting if possible.
    """
}
