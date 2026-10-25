# Regression tests — SmartLinkGuard, PIN rate limiter, Dead-Man Wipe

Both platforms ship live regression coverage as of 2026-10-25:

- **Android**: JUnit tests under `android/app/src/test/`. 33 cases, 0 failures.
- **iOS**: XCTest target `OnlyTrustedTests` under `ios/OnlyTrustedTests/`.
  34 cases, 0 failures. The extra case is `testExtractRawHostStripsSchemeAndPath`
  which only exists on iOS because iOS's URLComponents behavior around
  raw hosts was the specific gap the raw-input scan closed.

Both platforms share the same source-of-truth rules; the test cases prove
the shared behavior and should return equivalent results on both. The
vectors are identical by design — if Android passes but iOS fails (or
vice versa), one of them has drifted.

## Running the tests

### Android (33 cases)

```bash
cd android
./gradlew :app:testDebugUnitTest
```

### iOS (34 cases)

```bash
cd ios
xcodebuild test \
  -project OnlyTrusted.xcodeproj \
  -scheme OnlyTrusted \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -only-testing:OnlyTrustedTests
```

Both expected: 0 failures.

### Important: ThreatIntel isolation on iOS

The main app wires `SmartLinkGuardRules.phishingHostLookup` to
`ThreatIntelDatabase.isPhishingHost` at startup. The test bundle runs
in-process with the host app so that wiring leaks into test state and
can flip verdicts (e.g. `google.com` picked up a false-positive hit in
the bundled feed during initial development). `SmartLinkGuardRulesTests.setUp()`
nulls `phishingHostLookup` for every test so cases exercise the built-in
rule set in isolation, not the state of the shipped feed.

## iOS test vectors (reference)

These are the vectors ported from Android. Both platforms' tests should
return identical results — any divergence is a bug in one of the two
implementations.

### SmartLinkGuard

| Input | Expected verdict | Must contain reason |
|-------|-----------------|---------------------|
| `https://admin:secret@legit-looking.com` | Danger | "embedded username/password" |
| `https://:pass@site.com` | Danger | — |
| `https://google.com` | Safe | — |
| `https://pаypal.com` (Cyrillic а) | Caution or Danger | "lookalike" |
| `https://paypal.com` | Safe | — |
| `https://paypal.com.scam.ru` | Danger | "brand ... subdomain" |
| `https://support.paypal.com` | Safe | NOT "subdomain ... phishing" |
| `https://foo.bar.example.org` | Safe/Caution | NOT "brand ... subdomain" |
| `http://google.com` | Caution | 1 caution flag → stays caution |
| `http://bit.ly/x` | Danger | 2 caution flags → "escalated to Danger" |
| `http://cutt.ly/bad.zip` | Danger | 3 caution flags |
| `https://xn--paypal-test.com` | Danger | existing Punycode behavior |
| `http://192.168.1.1/login` | Danger | existing IP literal behavior |
| `https://github.com` | Safe | existing allowlist behavior |
| `https://gooogle.com` (3 o's) | Danger | existing typosquat behavior |

### SmartLinkGuardRules.containsHomoglyph

| Input | Expected |
|-------|----------|
| `"pаypal.com"` (Cyrillic а) | true |
| `"gооgle.com"` (Cyrillic о) | true |
| `"paypal.com"` | false |
| `"github.com"` | false |

### SmartLinkGuardRules.brandMisusedAsSubdomain

| host | etldPlusOne | Expected |
|------|-------------|----------|
| `paypal.com.scam.ru` | `scam.ru` | `"paypal"` |
| `support.paypal.com` | `paypal.com` | `nil` |
| `foo.bar.example.org` | `example.org` | `nil` |

### PINRateLimiter.penaltySeconds

| failures | Expected seconds |
|----------|------------------|
| 1 – 4 | 0 |
| 5 – 9 | 60 |
| 10 – 14 | 300 |
| 15 – 19 | 900 |
| 20+ | 3600 |

### DeadmanWipeState.shouldWipeNow — clock-skew defense

| Scenario | Expected `shouldWipe` |
|----------|----------------------|
| wall-clock 7 days elapsed on 7-day timer | true |
| user rewinds clock 10 days + monotonic 8 days elapsed | true |
| wall-clock 3 days on 7-day timer | false |
| after reboot (monotonic reset) + wall-clock 10 days | true |
| boot-time stored vs computed within 60-s tolerance | treated as same boot |
| missing storedBoot (pre-patch install) | falls back to wall clock |

---

## iOS XCTest target details

Shipped as of 2026-10-25. The pbxproj was hand-edited rather than
regenerated via xcodegen — the project ships 15+ existing targets and a
migration to xcodegen would be its own PR.

- Target: `OnlyTrustedTests` (bundle unit-test, test-host =
  `OnlyTrusted.app`)
- Config list: `OnlyTrustedTests` Debug + Release
- Depends on: main `OnlyTrusted` target
- Tests under `ios/OnlyTrustedTests/`:
    - `SmartLinkGuardRulesTests.swift` — 21 cases
    - `PINRateLimiterTests.swift` — 7 cases
    - `DeadmanWipeClockSkewTests.swift` — 6 cases
- Imports main-app code via `@testable import OnlyTrusted` (standard
  pattern, enables access to `internal` symbols).

Follow-up: wire the `xcodebuild test` command into
`.github/workflows/release.yml` so iOS regression tests run on every
tag push, matching Android.

---

Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
