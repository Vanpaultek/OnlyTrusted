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

## Phase 2 — Secure Enclave / StrongBox direct crypto (shipped)

The original Phase 2 plan called for WebAuthn (`ASAuthorizationController`
on iOS, `androidx.credentials.CredentialManager` on Android), but that
approach would have required:
- An Apple Developer account ($99/yr) + `webcredentials` associated
  domains entitlement
- Serving `apple-app-site-association` + `assetlinks.json` from a domain
  we own (`vanpaultek.com`)
- iCloud Keychain / Google Password Manager visibility, which is cosmetic
  for an app with no server-side RP

We rejected that and shipped the honest alternative: a hardware-bound
keypair with biometric ACL that the OS kernel refuses to release without
a fresh BIOMETRIC_STRONG assertion. No infrastructure dependency, no new
accounts, no third-party crypto validation path.

**iOS (`BiometricPasskey.swift`)**
- `SecKeyCreateRandomKey` with `kSecAttrTokenID = kSecAttrTokenIDSecureEnclave`
- EC256 keypair, biometry-current-set + private-key-usage ACL
- Wraps the FieldCrypto AES-256-GCM key via ephemeral ECDH + HKDF-SHA256 +
  AES-GCM (`[65-byte ephemeral pubkey || sealed box]`)
- Unwrap uses `SecKeyCopyKeyExchangeResult` which fires the Face ID /
  Touch ID prompt via the user-supplied `LAContext` (we reuse the one
  from `AppLockGate.attemptAuth` so no second prompt appears)

**Android (`BiometricPasskey.kt`)**
- AES-256-GCM Keystore key, `setIsStrongBoxBacked(true)` with runtime
  fallback to TEE-backed Keystore on devices without StrongBox (Pixel 2
  / emulator / most pre-2018 flagships)
- `setUserAuthenticationRequired(true)` + `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)` —
  no cached auth window, every Cipher init requires a fresh biometric
- Wrapped session nonce via `BiometricPrompt.CryptoObject(Cipher)`,
  stored in `EncryptedSharedPreferences` (`passkey_session_wrapped_v2`)
- `AppLockGate.primePasskeySession` posts the enroll/unlock prompt with
  a 400ms delay so the lock-gate `BiometricPrompt` fragment has time to
  tear down (back-to-back prompts leave the second in a state where its
  success callback never fires)

### What ships in this commit vs. what's deferred

**Shipped**: enroll + unwrap plumbing, kernel-level biometric assertion
at every unlock, hardware binding via Secure Enclave / StrongBox,
session nonce held in memory until app background.

**Deferred (Phase 3)**: migrating the actual vault encryption key to
the biometric-gated wrapped path. Currently the vault still uses
`field-key.v1` (iOS Keychain, no biometric ACL) / `onlytrusted-field-key`
(Android Keystore, no biometric ACL) directly. The Phase 2 passkey
proves biometric at the kernel level but doesn't yet gate the vault key.

Why defer: wrapping the vault key is a 3-release migration (dual-write
→ confirm → delete legacy) because a single-release migration risks
users losing data if the biometric gate becomes permanently
unavailable (sensor failure, device repair). Phase 3 lands once we have
telemetry showing the dual path survives real-world usage.

### Fallback behavior

- **No biometric enrolled on device**: enroll + unlock paths are no-ops;
  the app uses the legacy Keychain / Keystore path. No UX change.
- **Biometric sensor failure mid-session**: user falls back to PIN; next
  successful biometric re-primes the session.
- **User disables biometric from Settings → Account → Biometric unlock**:
  `BiometricPasskey.unenroll()` deletes both the Keystore key and the
  wrapped blob. App continues on legacy path.
- **StrongBox unavailable** (emulators, pre-2018 flagships): generation
  falls back to TEE-backed Keystore automatically. Still hardware-backed,
  just in the main secure environment instead of the dedicated chip.

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
