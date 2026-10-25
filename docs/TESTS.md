# Regression tests — SmartLinkGuard, PIN rate limiter, Dead-Man Wipe

Android has JUnit coverage under `android/app/src/test/`. iOS does not yet
have an XCTest target wired up — the Swift test vectors below mirror the
Android tests exactly, so any future XCTest target can start from this
list.

Both platforms share the same source-of-truth rules; the test cases prove
the shared behavior and should return equivalent results on both.

## Running Android tests

```bash
cd android
./gradlew :app:testDebugUnitTest
```

Expected: 33 tests pass, 0 failures.

## iOS test vectors (manual, pending XCTest target)

Open `ios/OnlyTrusted/Sources/SmartLinkGuard/SmartLinkGuardRules.swift`
in Xcode, add an XCTest target, port each row as `XCTAssert*(...)`.

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

## Setting up iOS XCTest (follow-up)

1. In Xcode: File → New → Target → iOS → Unit Testing Bundle. Name it
   `OnlyTrustedTests`.
2. Add `OnlyTrusted` as a target dependency.
3. Create `OnlyTrustedTests/SmartLinkGuardRulesTests.swift` and port the
   above vectors as `XCTAssertEqual` / `XCTAssertTrue`.
4. Scheme → Edit Scheme → Test → add `OnlyTrustedTests` to the test
   action.
5. Run via `xcodebuild test -scheme OnlyTrusted -destination 'platform=iOS Simulator,name=iPhone 15'`.
6. Wire into `.github/workflows/release.yml` so iOS tests run on every
   tag push, same as Android.

---

Maintained by: VanPaulTek Inc. · admin@vanpaultek.com
