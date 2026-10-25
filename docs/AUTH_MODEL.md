# Authentication model — Passkey + PIN

Last updated: 2026-10-25 · covers release `v0.3.4+`

OnlyTrusted uses **Passkey** (Face ID / Touch ID / fingerprint, backed by the
device's Secure Enclave on iOS or StrongBox / Keystore on Android) as the
**primary unlock method** on both platforms, with a **PIN as the fallback**.

This document explains the architecture, the deliberate rejection of
email-code login, and the Phase 2 roadmap for full WebAuthn passkey crypto.

---

## Why passkey + PIN (not email code)

**Email-code login would break OnlyTrusted's zero-cloud, zero-server, no-account
promise.** The whole value proposition — the honest-claim text on the landing
page, the Settings → Legal → Honest Limits sheet, the SECURITY.md disclosure
policy, and every onboarding screen — is grounded in the architectural fact
that OnlyTrusted operates no backend.

Sending a 6-digit code to an email address requires, at minimum:

- SMTP relay or transactional email provider (SendGrid / Postmark / SES) —
  ~$10–30/mo baseline + per-message cost
- An HTTP API service to generate codes, store them with expiry, verify
  user-submitted codes
- A database to hold user-account records keyed on email
- Rate limiting + abuse controls (6-digit codes are 1-in-a-million brute-force
  targets)
- Data-processor obligations (GDPR, CCPA — storing an email + an association
  with the app installer identifier)
- Updates to the Privacy Policy explaining the server exists
- Deletion of the honest-claim text on the landing page + Settings → Legal
- A trusted-time source (user's phone clock may differ from server clock —
  must handle out-of-sync code expiry)

Passkey/biometric authentication delivers the same user experience ("I open
the app, Face ID scans, I'm in") **without** any of that infrastructure.
Secure Enclave / StrongBox handles the credential locally; the OS prompts
for biometric; the credential never leaves the device. OnlyTrusted stays
zero-cloud.

---

## Current (Phase 1) implementation

Shipping as of commit `9eb5d9c+` (iOS) and parallel Android commit:

- **Primary unlock**: `LAContext.evaluatePolicy(.deviceOwnerAuthentication)`
  (iOS) / `androidx.biometric.BiometricPrompt` (Android), auto-triggered when
  the lock screen appears. User sees a Face ID / Touch ID / fingerprint prompt
  immediately — no tap required.
- **PIN fallback**: Available via "Use PIN instead" link on the lock screen.
  Stored as PBKDF2-HMAC-SHA256 hash (100 000 iterations, 16-byte random salt)
  in the system Keychain (iOS) / EncryptedSharedPreferences (Android).
- **Encryption key derivation**: The vault's AES-256-GCM master key is
  derived from the PIN via PBKDF2. The biometric unlock gates **access to the
  Keychain entry** that holds the master key — a well-established pattern
  the device OS vendors (Apple, Google) explicitly document as the safe way
  to combine biometric convenience with cryptographic locking.
- **PIN remains required as a fallback** for devices without biometric
  sensors AND as a secondary unlock path if the device's biometric sensors
  fail transiently (dirty finger, poor lighting for Face ID, injured hand).
- **UI copy** uses "Passkey" vocabulary throughout: onboarding step titled
  "Set up your Passkey"; lock screen says "Unlock with your Passkey · Face /
  fingerprint · on-device, phishing-proof"; Settings calls it "Passkey
  (biometric)".

### Terminology clarification

The UI calls this a "Passkey" because in 2026-era user vocabulary that's the
expected term for "biometric that unlocks the thing with no password typed."
Technically, the Phase 1 implementation is **biometric-gated Keychain
access** — the "proper" WebAuthn passkey (ASAuthorizationController +
credential ID + cryptographic signature) lands in Phase 2 below.

For OnlyTrusted's local-only threat model, the two approaches are
functionally equivalent — both bind to Secure Enclave, both can't be
phished (no attack surface), both work offline. Phase 2 adds ecosystem
visibility (iOS Settings → Passwords → Passkeys) and future-proofs for
optional cross-device sync via iCloud Keychain / Google Password Manager.

---

## Phase 2 — true WebAuthn passkey (follow-up commit)

**iOS 17+:**
- `ASAuthorizationController` + `ASAuthorizationPlatformPublicKeyCredentialProvider`
- Register a device-bound credential with `createCredentialRegistrationRequest`
- Authenticate via `createCredentialAssertionRequest`
- RP ID = `onlytrusted.app` (hardcoded; local-RP use requires no
  `.well-known/apple-app-site-association` file)
- Credential ID + public key stored in the app's Keychain alongside the
  current PIN hash
- Signature verification happens locally (we're our own relying party)
- Visible to the user in iOS Settings → Passwords → Passkeys

**Android 14+:**
- `androidx.credentials.CredentialManager` + `CreatePublicKeyCredentialRequest`
- Same local-RP pattern (RP ID = `onlytrusted.app`)
- Credential visible in Google Password Manager
- Android 10–13 (API 29–33) fall back to current `BiometricPrompt` + PIN
  path (runtime API-level check)

**Optional user-opt-in: cross-device sync**
- iCloud Keychain carries the passkey to the user's other Apple devices
  automatically — no OnlyTrusted server involvement
- Google Password Manager does the same across the user's Android devices
- User sees "Also unlock this on my iPad / other Android" without any
  action on our part

**Estimated scope for Phase 2**: ~400 LOC iOS + ~450 LOC Android + ~100 LOC
UI integration + ~150 LOC docs = one dense commit per platform plus a
coordinating commit.

---

## Phase 3 — optional recovery via Shamir Split (Elite tier)

Already shipping, documented here for completeness: Elite subscribers can
split their Vault master key into M-of-N shares distributed to trusted
friends via Bluetooth peer-to-peer. If a user loses BOTH their biometric
(e.g., permanent injury) AND their PIN, they can recover by having M of
the N shareholders bring their OnlyTrusted devices near theirs and
collectively approve the recovery.

See `USER_GUIDE.md` → Elite → "Shamir Split" + "Trusted Witness".

---

## What this does NOT change

- **No new network calls**: everything stays on-device.
- **No new accounts**: there's still nothing to sign up for.
- **No new permissions**: Face ID / Touch ID / fingerprint were already in
  the permission list; the Phase 2 WebAuthn APIs don't require new ones.
- **No data loss on upgrade**: existing PIN-only users keep their PIN
  working. The Passkey setup prompt appears on first launch after upgrade;
  users can accept or skip. Vault contents + settings persist either way.

---

## Threat-model notes

- **Shoulder-surfing a PIN**: still possible with PIN-only mode. Face-Change
  Lock (Elite) mitigates by blurring the screen when the owner looks away.
- **Coerced biometric unlock** (someone physically forces your face/finger
  on the sensor): Panic Lockdown (Elite) disables biometric unlock until
  the device reboots. Border Mode (Elite) does the same when crossing a
  border.
- **Device loss**: there is no account recovery because there is no
  account. Set up Shamir Split (Elite) with trusted shareholders BEFORE
  you lose your device.
- **OS-level forensic imaging**: SQLCipher + Keychain / Keystore encryption
  resists filesystem-level attacks. If the device is unlocked when seized,
  the forensic lab wins — this is a hardware limitation we can't fix.

See `SECURITY.md` for the full vulnerability-disclosure policy and
[Honest Limits](https://vanpaultek.github.io/OnlyTrusted/guide/#limits)
for every documented limitation.

---

Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
