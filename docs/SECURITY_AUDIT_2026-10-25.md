# Security Audit — 2026-10-25

Scope: targeted audit of 4 security modules against the OnlyTrusted Secure
Implementation Kit's checklist. The kit itself was reviewed and rejected
as a drop-in replacement because every piece would have regressed existing
functionality (empty blocklist stubs, parallel capture coordinator,
Node.js backend conflicting with the zero-cloud architecture). This audit
preserves the shipping implementation and documents what, if anything,
needs targeted patches.

Pre-patch commit: `779dafb`. Patches land in the same release as this doc.

---

## Module 1 — SmartLinkGuard (URL risk scoring)

Files:
- `ios/OnlyTrusted/Sources/SmartLinkGuard/SmartLinkGuardRules.swift`
- `android/app/src/main/java/com/onlytrusted/app/ui/smartlinkguard/SmartLinkGuardRules.kt`

### Present protections (8 rules, both platforms)

- [x] Scheme check — flags http/ftp/telnet/file as insecure (`Caution`)
- [x] Known-shortener list — 100+ domains (`Caution`)
- [x] Suspicious TLD — .zip, .mov, .xyz, .top, .click, etc. (`Caution`)
- [x] IDN / Punycode `xn--` detection (`Danger`)
- [x] IP literal detection (`Danger`)
- [x] ~200-entry allowlist of known-legitimate eTLD+1s
- [x] Typo-squat via Levenshtein ≤ 2 against allowlist (`Danger`)
- [x] Suspicious path keyword on unverified host (`Caution`)
- [x] Threat-intel injection point for 50k-entry phishing feed (`Danger`)

### Missing protections → patched

- [ ] Embedded `user:password@host` in URL authority → **patch applied**: `Danger`
- [ ] Non-Punycode Unicode homograph (Cyrillic 'а' lookalike in a host already
      resolved past the IDN encoder) → **patch applied**: `Caution` (not `Danger`
      because some legitimate internationalized hosts do contain non-ASCII
      characters; combined with other flags it escalates)
- [ ] Brand-as-subdomain spoofing (`paypal.com.scam.ru`) → **patch applied**:
      `Danger`
- [ ] Weighted escalation when 2+ `Caution` flags fire → **patch applied**:
      final verdict escalates to `Danger` when the number of unique `Caution`
      reasons is ≥ 2 (preserves the existing 3-level ladder for the UI)

Severity: Medium → Low after patches. The missing checks are standard
anti-phishing patterns that any security vendor should ship on day one; the
patches close the gap without rewriting the engine.

### Marketing vs reality

Current copy says "Smart Link Guard checks every URL against on-device rules."
Reality matches: no network calls, bundled lists only, verdicts computed from
8 (now 11) local heuristics. No change required.

---

## Module 2 — Shamir Secret Sharing (Elite tier recovery)

Files:
- `ios/OnlyTrusted/Sources/P2P/ShamirSSS.swift`
- `android/app/src/main/java/com/onlytrusted/app/ui/p2p/ShamirSSS.kt`

### Present protections

- [x] Hand-rolled GF(2^8) arithmetic — cross-platform test vectors pass
- [x] HMAC-SHA256 proof per share — detects tampered or swapped shares
- [x] Unique 16-byte random salt per split
- [x] Split ID derived from `SHA-256(secret || salt)` for stable grouping
- [x] Enforced `k ≥ 2`, `k ≤ n`, `n ≤ 255`
- [x] Clear-cache path drops the plaintext master key from memory

### Findings — not patched this release

- Hand-rolled crypto (vs an audited library like SLIP-0039). Correctness is
  verified by test vectors, but the implementation hasn't been independently
  audited. **Decision**: do not swap in a different library in this release.
  Switching would be a larger-scope change that itself needs an audit of the
  migration path. Flagged for a future dedicated Shamir review.
- No share versioning for key rotation. If a share is compromised and the user
  re-splits, the old shares remain usable against the old wrapped vault key.
  **Decision**: not patched here because key rotation itself isn't yet
  exposed as a user flow. When rotation ships, add a `shareVersion: UInt8`
  to the envelope.
- Shares are held as `Data` / `ByteArray` which don't guarantee secure
  zeroing. **Decision**: Swift/Kotlin don't natively guarantee memory
  zeroing — this would need `CryptoKit.SymmetricKey` wrapping on iOS and a
  careful `fill(0)` + GC hint on Android. Minor severity vs. the real threat
  model (physical memory extraction on an unlocked device is already a loss
  per our Honest Limits).

Severity: Medium, no patches shipped this release. Documented so the next
Shamir touch addresses them.

---

## Module 3 — Dead-Man Wipe (Elite tier auto-wipe)

Files:
- `ios/OnlyTrusted/Sources/DeadmanWipe/DeadmanWipeState.swift`
- `android/app/src/main/java/com/onlytrusted/app/deadmanwipe/DeadmanWipeState.kt`

### Present protections

- [x] Multiple intervals (7/14/30/60 days) with a max of 60
- [x] Timer resets on every unlock (`markUnlockNow` from AppLockGate)
- [x] 24h warning notification before wipe
- [x] Full-state wipe: vault, blocklist, events, settings
- [x] EncryptedSharedPreferences / Keychain-backed persistence

### Missing protections → patched

- [ ] Clock-skew bypass — a user who sets the device clock backward can
      indefinitely delay the deadline. **Patch applied**: compare
      wall-clock `Date()` against monotonic uptime (`CLOCK_MONOTONIC` on
      Android, `clock_gettime(CLOCK_UPTIME_RAW)` on iOS). If wall clock has
      moved backward relative to monotonic uptime by more than one hour,
      log tamper suspicion and refuse to defer the deadline.
- [ ] No user-cancellable window before the final wipe — on the day of the
      wipe, the user has no chance to override. **Patch applied**: 60-second
      countdown with a `Cancel` button that defers the wipe by 24 hours (max
      3 deferrals per 30 days, logged to the Events log).

### Marketing vs reality — audited

Current copy: "Wipe the vault after N days of inactivity." Reality: calls
`VaultStore.shared.wipe()` which re-keys the Keychain master key and
atomically deletes the local vault JSON + SQLCipher DB. The old ciphertext
is unrecoverable because the master key is destroyed.

The copy says "wipe" and the behavior destroys the key material — this
matches. **No marketing changes required.** The feature does NOT wipe the
whole phone (never claimed to), does NOT wipe iCloud backups (never
claimed to), and does NOT wipe the device if it's powered off during the
deadline (next boot runs the check — documented in Honest Limits).

Severity: Medium → Low after patches.

---

## Module 4 — Vault encryption keys

Files:
- `ios/OnlyTrusted/Sources/Vault/FieldCrypto.swift`
- `ios/OnlyTrusted/Sources/Vault/BiometricPasskey.swift`
- `ios/OnlyTrusted/Sources/Onboarding/ProfileStore.swift`
- `android/app/src/main/java/com/onlytrusted/app/privacy/FieldCrypto.kt`
- `android/app/src/main/java/com/onlytrusted/app/security/BiometricPasskey.kt`
- `android/app/src/main/java/com/onlytrusted/app/data/db/AppDatabase.kt`

### Present protections

- [x] AES-256-GCM for every encrypt/decrypt (CryptoKit + Android Keystore)
- [x] Randomized IV per operation (GCM default + explicit `setRandomizedEncryptionRequired(true)`)
- [x] Master key in iOS Keychain with
      `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`
- [x] Android master key via `MasterKey.KeyScheme.AES256_GCM` — AndroidX
      Security's keystore wrapper
- [x] PBKDF2-HMAC-SHA256 100 000 iterations for PIN hashing (`ProfileStore`)
- [x] SQLCipher-encrypted Room database on Android with per-install random
      32-byte passphrase, stored in `EncryptedSharedPreferences`
- [x] Phase 2 Secure Enclave / StrongBox biometric-gated passkey wraps the
      session nonce (defense-in-depth over the Keystore-protected key)

### Missing protections → patched

- [ ] No PIN rate limiting — a brute-force attacker with the device unlocked
      past the lock gate can hammer the PIN sheet freely. **Patch applied**:
      new `PINRateLimiter` on both platforms. 5 failed attempts within the
      same unlock session triggers a 60-second lockout; cumulative counter
      extends to 5 min / 15 min / 60 min at 10 / 15 / 20 total failures.
      Backed by Keychain (iOS) / EncryptedSharedPreferences (Android) so a
      force-stop doesn't reset the counter.

### Findings — not patched this release

- Biometric is additive, not mandatory. If `BiometricPasskey.unwrapVaultKey`
  fails, FieldCrypto falls back to the Keychain copy. **Decision**: this is
  intentional architecture per the Phase 2 commit (`8fdb621`). Making
  biometric mandatory would strand users who disable Face ID or whose
  sensor fails. The threat model: Keychain is already protected by
  `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`, meaning a locked
  device can't release the key. Attacker needs an unlocked device to
  attempt extraction — at which point they've already defeated the lock
  gate. Biometric adds defense in depth, not a hard requirement.
- Android master-key iteration count is opaque (AndroidX `MasterKey`
  doesn't expose it). The underlying Keystore spec uses hardware-backed
  key derivation; iteration count isn't the right metric for a hardware
  keystore. **Decision**: documented but not changed.

Severity: HIGH on PIN rate limiting → Low after patch. Other findings:
Low / documented.

---

## Non-findings

Things the kit flagged that we checked and found adequate:

- SMS filter extension: already filters on URL, sender, and body keywords
  with the Threat Intel feed. Kit's version was 2 keywords; ours is 50k+
  entries. No action.
- Call Directory extension: already loaded with the real blocklist via
  app-group. Kit's version has an empty array. No action.
- CaptureRegistry: already wired to `scenePhase` on iOS and
  `ProcessLifecycleOwner` on Android. Stops every registered session on
  background. Kit's version would be a parallel coordinator. No action.
- Backend security utilities: OnlyTrusted has no backend. The kit's
  `security.mjs` / `checkin.mjs` / `URLRisk.mjs` are Node.js server code
  that would require standing up an HTTP service — directly contradicts
  the zero-cloud architecture. No action; architecture is deliberate.

---

## Patch summary

| Module | Patches shipped this release | New tests |
|--------|------------------------------|-----------|
| SmartLinkGuard | 4 (embedded creds, Unicode homograph, brand-subdomain, weighted escalation) | iOS + Android |
| PIN rate limiter | 2 (new class on each platform + lock gate integration) | iOS + Android |
| Dead-Man Wipe | 2 (clock-skew check, cancellable countdown) | iOS + Android |
| Shamir | 0 (documented follow-up) | — |

Total: 8 patches, 6 regression tests, 0 marketing-copy changes required.

Signed off against the 9-item release-gate checklist in
`docs/RELEASE_GATES.md`.

---

Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
