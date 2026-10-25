// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
import XCTest
@testable import OnlyTrusted

/// Regression tests for the 4 SmartLinkGuardRules patches landed in the
/// 2026-10-25 security audit:
///   1. Embedded user:password → Danger
///   2. Non-Punycode Unicode homograph → Caution (escalates on combination)
///   3. Brand-as-subdomain spoofing → Danger
///   4. Weighted escalation: 2+ Caution flags → Danger
///
/// Mirror of `android/.../SmartLinkGuardRulesTest.kt`. Both files share the
/// same test vectors; divergence means a bug.
final class SmartLinkGuardRulesTests: XCTestCase {

    /// The main app wires `SmartLinkGuardRules.phishingHostLookup` to the
    /// ThreatIntelDatabase at startup. When the test bundle runs in-process with
    /// the host app, that wiring is active and will intrude on tests that rely
    /// on pure built-in rules. Null it out for every test so we measure the
    /// rules we actually ship in this file, not the state of the bundled feed.
    override func setUp() {
        super.setUp()
        SmartLinkGuardRules.phishingHostLookup = nil
    }

    // MARK: - Patch 1: embedded user:password

    func testEmbeddedCredentialsInUrlAreDanger() {
        let r = SmartLinkGuardRules.inspect("https://admin:secret@legit-looking.com")
        XCTAssertEqual(r.verdict, .danger)
        XCTAssertTrue(r.reasons.contains { $0.contains("embedded username/password") })
    }

    func testOnlyPasswordInUrlIsDanger() {
        let r = SmartLinkGuardRules.inspect("https://:pass@site.com")
        XCTAssertEqual(r.verdict, .danger)
    }

    func testPlainUrlWithoutCredentialsIsNotFlaggedForCreds() {
        let r = SmartLinkGuardRules.inspect("https://google.com")
        XCTAssertEqual(r.verdict, .safe)
    }

    // MARK: - Patch 2: Unicode homograph (non-Punycode)

    func testCyrillicLookalikeInHostFlagsCaution() {
        // "pаypal" — the 'а' is U+0430 Cyrillic small a
        let r = SmartLinkGuardRules.inspect("https://pаypal.com")
        XCTAssertTrue(r.reasons.contains { $0.contains("lookalike") },
                      "reasons were: \(r.reasons)")
        // Caution OR Danger (if escalation fires) — must not be Safe
        XCTAssertNotEqual(r.verdict, .safe)
    }

    func testPlainAsciiHostDoesNotFlagHomograph() {
        let r = SmartLinkGuardRules.inspect("https://paypal.com")
        XCTAssertFalse(r.reasons.contains { $0.contains("lookalike") })
    }

    // MARK: - Patch 3: brand-as-subdomain

    func testBrandNameAsSubdomainOfStrangerTldIsDanger() {
        let r = SmartLinkGuardRules.inspect("https://paypal.com.scam.ru")
        XCTAssertEqual(r.verdict, .danger)
        XCTAssertTrue(r.reasons.contains { $0.contains("brand") && $0.contains("subdomain") })
    }

    func testLegitimateSubdomainOfRealBrandIsNotFlagged() {
        let r = SmartLinkGuardRules.inspect("https://support.paypal.com")
        XCTAssertFalse(r.reasons.contains { $0.contains("subdomain") && $0.contains("phishing") })
    }

    func testRandomSubdomainUnrelatedToAnyBrandIsNotFlaggedForBrandMisuse() {
        let r = SmartLinkGuardRules.inspect("https://foo.bar.example.org")
        XCTAssertFalse(r.reasons.contains { $0.contains("brand") && $0.contains("subdomain") })
    }

    // MARK: - Patch 4: weighted escalation

    func testSingleCautionFlagStaysCaution() {
        // http on an allowlisted domain — 1 caution flag only
        let r = SmartLinkGuardRules.inspect("http://google.com")
        XCTAssertEqual(r.verdict, .caution, "reasons: \(r.reasons)")
    }

    func testTwoCautionFlagsEscalateToDanger() {
        // http on a known shortener → 2 caution flags
        let r = SmartLinkGuardRules.inspect("http://bit.ly/x")
        XCTAssertEqual(r.verdict, .danger)
        XCTAssertTrue(r.reasons.contains { $0.contains("escalated to Danger") })
    }

    func testThreeCautionFlagsStillDanger() {
        // http + shortener + suspicious TLD
        let r = SmartLinkGuardRules.inspect("http://cutt.ly/bad.zip")
        XCTAssertEqual(r.verdict, .danger)
    }

    // MARK: - No-regression pins for existing behavior

    func testPunycodeIdnRemainsDanger() {
        let r = SmartLinkGuardRules.inspect("https://xn--paypal-test.com")
        XCTAssertEqual(r.verdict, .danger)
    }

    func testIpLiteralRemainsDanger() {
        let r = SmartLinkGuardRules.inspect("http://192.168.1.1/login")
        XCTAssertEqual(r.verdict, .danger)
    }

    func testAllowlistedHttpsSiteIsSafe() {
        let r = SmartLinkGuardRules.inspect("https://github.com")
        XCTAssertEqual(r.verdict, .safe)
    }

    func testTyposquatRemainsDanger() {
        let r = SmartLinkGuardRules.inspect("https://gooogle.com")  // 3 o's
        XCTAssertEqual(r.verdict, .danger)
    }

    // MARK: - Direct helper tests

    func testContainsHomoglyphDetectsCyrillic() {
        XCTAssertTrue(SmartLinkGuardRules.containsHomoglyph("pаypal.com"))
        XCTAssertTrue(SmartLinkGuardRules.containsHomoglyph("gооgle.com"))
    }

    func testContainsHomoglyphPassesPlainAscii() {
        XCTAssertFalse(SmartLinkGuardRules.containsHomoglyph("paypal.com"))
        XCTAssertFalse(SmartLinkGuardRules.containsHomoglyph("github.com"))
    }

    func testBrandMisusedAsSubdomainDetectsPaypalShape() {
        let brand = SmartLinkGuardRules.brandMisusedAsSubdomain(
            host: "paypal.com.scam.ru", etldPlusOne: "scam.ru"
        )
        XCTAssertEqual(brand, "paypal")
    }

    func testBrandMisusedAsSubdomainReturnsNilForSelfBrand() {
        XCTAssertNil(SmartLinkGuardRules.brandMisusedAsSubdomain(
            host: "support.paypal.com", etldPlusOne: "paypal.com"
        ))
    }

    func testBrandMisusedAsSubdomainReturnsNilForUnrelatedSubdomain() {
        XCTAssertNil(SmartLinkGuardRules.brandMisusedAsSubdomain(
            host: "foo.bar.example.org", etldPlusOne: "example.org"
        ))
    }

    func testExtractRawHostStripsSchemeAndPath() {
        XCTAssertEqual(SmartLinkGuardRules.extractRawHost("https://example.com/foo"), "example.com")
        XCTAssertEqual(SmartLinkGuardRules.extractRawHost("http://a:b@host.com:8080/x"), "host.com")
        XCTAssertEqual(SmartLinkGuardRules.extractRawHost("site.com"), "site.com")
    }
}
