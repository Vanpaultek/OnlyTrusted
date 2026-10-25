# OnlyTrusted Release Security Gates

Formalized from the 2026-10-25 security directive. Every release of
OnlyTrusted — including patch releases — must pass all 9 gates below
before the Play Store or App Store build is cut.

Status format: `[ ]` pending · `[x]` passed · `[?]` manual verification
needed · `[—]` gate skipped with documented rationale (requires reviewer
sign-off).

## Gates

### Gate 1 — Existing iOS SMS and call filters preserved

- Verify `ios/MessageFilterExtension/MessageFilter.swift` principal
  class still subclasses `ILMessageFilterExtension` and no behavior was
  silently stubbed to always-allow.
- Verify `ios/CallDirectoryExtension/CallDirectoryHandler.swift` populates
  blocking entries from the shared app-group container.
- Diff against the previous release — if the blocking count went down
  significantly, flag for manual review.

**Automation**: `scripts/release/verify-ios-extensions.sh` (follow-up)
compares extension `strings` output + plist against previous release.

### Gate 2 — Existing Android call screening preserved

- Verify `android/app/src/main/java/com/onlytrusted/app/callscreening/`
  still ships a working `CallScreeningService` + manifest `<service>`
  entry with `BIND_SCREENING_SERVICE` permission.
- Verify the local blocklist contains the expected row count (threat intel
  feed + user block list) — regression if the number drops ≥50% unexpectedly.

### Gate 3 — iOS and Android capture shutdown verified

- Manual: launch app, start a camera or voice session, press home → verify
  session is torn down (not just paused). Return to app → verify session is
  NOT auto-resumed (user must explicitly tap to re-enable).
- Code: `CaptureRegistry.stopAll(reason: .background)` still called from
  `OnlyTrustedApp.scenePhase` on iOS; `PrivacyCaptureGate.onStop` still
  attached to `ProcessLifecycleOwner` on Android.

### Gate 4 — Zero-cloud architecture preserved

- Grep both source trees for any outbound network client:
  `grep -rE 'URLSession|OkHttpClient|HttpURLConnection|Retrofit' --include='*.swift' --include='*.kt'`
- Expected matches: ThreatIntel feed refresh (bundled, not live);
  App Store / Play Store receipt validation (OS-mediated); nothing else.
- Any new match requires written sign-off that it doesn't violate the
  zero-cloud promise in Settings → Legal → Honest Limits.

### Gate 5 — Vault encryption and recovery tested

- Pass: all `android/app/src/test/java/.../security/SmartLinkGuardRulesTest.kt`
  tests + the Shamir vector tests in `.../p2p/ShamirSSSTest.kt`.
- Pass: `PINRateLimiterTest.penaltySecondsFor` curve matches the specified
  5 / 10 / 15 / 20-threshold ladder.
- Manual: wrong-PIN 5 times at the lock gate → verify 60-second lockout
  message appears + second wrong-PIN inside the window shows the same lockout
  rather than restarting the counter.
- Manual: Enable biometric → disable biometric → confirm vault still
  decrypts via PIN (fallback path intact).

### Gate 6 — Dead-Man Wipe false-trigger scenarios tested

- Pass: `DeadmanWipeClockSkewTest` (all 6 cases).
- Manual on real device:
  1. Enable Dead-Man Wipe with 7-day interval.
  2. Set device clock back 30 days.
  3. Confirm the wipe still fires at the real 7-day monotonic deadline, not
     at the manipulated wall-clock deadline.
  4. Confirm 24-hour warning notification arrived.
- Manual: force-stop the app during the interval → confirm timer is NOT
  reset.

### Gate 7 — Smart Link Guard false positives tested

- Pass: `SmartLinkGuardRulesTest` (all 20+ cases).
- Manual: run the full `allowlist` through `inspect()` — every entry must
  return `.safe`. Any regression here means a legit domain just became a
  false positive.
- Manual: paste 10 known-benign newsletter URLs with tracking parameters →
  verify no false positives.

### Gate 8 — App Store and Play Store permissions audited

- Diff `AndroidManifest.xml` + `Info.plist` + extension entitlements
  against the previous release.
- Any new `<uses-permission>` requires written justification tied to a
  specific user-visible feature.
- Verify `READ_SMS` / `READ_CALL_LOG` are NOT requested (we use
  `CallScreeningService` role + user-opt-in SMS filter extension instead,
  per Google Play Sensitive Permissions Policy + Apple IdentityLookup
  guidance).
- Verify Apple's App Store Connect "Data Collected" matches reality (we
  collect nothing by default; family-share Vault export is opt-in).

### Gate 9 — Honest Limits disclosures updated

- Review Settings → Legal → Honest Limits sheet copy against the actual
  feature capabilities. Specifically audit (one line each):
  - Call Shield: "uses supported caller-blocking interfaces and cannot
    block every scam call" — matches.
  - SMS Defense: "processes supported messages from unknown senders;
    cannot inspect your entire inbox or iMessages" — matches.
  - Auto Privacy Shield: "stops OnlyTrusted-owned recording sessions;
    does not disable other apps' microphones or cameras" — matches.
  - Dead-Man Wipe: "wipes your on-device vault after the chosen interval
    of inactivity; cannot wipe backups you created outside OnlyTrusted;
    cannot run while the device is powered off" — matches.
  - Voice Biometric (Elite): "cannot prove a caller is genuine; verify
    sensitive requests via a trusted callback" — matches.
- Any marketing copy that overclaims (e.g., says "wipes device" when it
  only wipes the vault) → block release.

---

## How to run the gates

**Automated (both platforms + CI):**

```bash
# Android unit tests
cd android && ./gradlew :app:testDebugUnitTest

# iOS test vectors (manual, until XCTest target lands — see TESTS.md)
```

CI should fail the release workflow if any automated gate fails.
Manual gates are tracked in a release checklist appended to the GitHub
Release notes.

**Follow-up work:**

1. Set up iOS XCTest target so the SmartLinkGuard rule changes get the
   same regression coverage Android has today.
2. Automate the "diff permissions since last release" gate as a GitHub
   Action that runs on every tag push.
3. Automate the Honest Limits copy audit by extracting the strings into
   `docs/HONEST_LIMITS.md` and diffing on every release.

---

Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
