// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.security

import com.onlytrusted.app.ui.smartlinkguard.SmartLinkGuardRules
import com.onlytrusted.app.ui.smartlinkguard.URLVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Regression tests for the 4 SmartLinkGuardRules patches landed in the
 * 2026-10-25 security audit:
 *   1. Embedded user:password → Danger
 *   2. Non-Punycode Unicode homograph → Caution (escalates on combination)
 *   3. Brand-as-subdomain spoofing → Danger
 *   4. Weighted escalation: 2+ Caution flags → Danger
 *
 * Also pins existing behavior that must not regress: allowlist still wins
 * when no caution flags fire, legitimate IDN sites with just `xn--` prefix
 * remain Danger (unchanged), typosquat still Danger.
 */
class SmartLinkGuardRulesTest {

    // ---- Patch 1: embedded user:password ----

    @Test
    fun `embedded credentials in URL are Danger`() {
        val r = SmartLinkGuardRules.inspect("https://admin:secret@legit-looking.com")
        assertEquals(URLVerdict.Danger, r.verdict)
        assertTrue(r.reasons.any { "embedded username/password" in it })
    }

    @Test
    fun `only password in URL is Danger`() {
        val r = SmartLinkGuardRules.inspect("https://:pass@site.com")
        assertEquals(URLVerdict.Danger, r.verdict)
    }

    @Test
    fun `plain url without credentials is not flagged for creds`() {
        val r = SmartLinkGuardRules.inspect("https://google.com")
        assertEquals(URLVerdict.Safe, r.verdict)
    }

    // ---- Patch 2: Unicode homograph (non-Punycode) ----

    @Test
    fun `Cyrillic lookalike in host flags Caution`() {
        // "pаypal" — the 'а' is U+0430 Cyrillic small a
        val r = SmartLinkGuardRules.inspect("https://pаypal.com")
        assertTrue(r.reasons.any { "lookalike" in it })
        // Caution OR Danger (if escalation fires) — must not be Safe
        assertTrue(r.verdict != URLVerdict.Safe)
    }

    @Test
    fun `plain ASCII host does not flag homograph`() {
        val r = SmartLinkGuardRules.inspect("https://paypal.com")
        assertTrue(r.reasons.none { "lookalike" in it })
    }

    // ---- Patch 3: brand-as-subdomain ----

    @Test
    fun `brand name as subdomain of stranger TLD is Danger`() {
        val r = SmartLinkGuardRules.inspect("https://paypal.com.scam.ru")
        assertEquals(URLVerdict.Danger, r.verdict)
        assertTrue(r.reasons.any { "brand" in it && "subdomain" in it })
    }

    @Test
    fun `legitimate subdomain of real brand is not flagged`() {
        val r = SmartLinkGuardRules.inspect("https://support.paypal.com")
        assertTrue(r.reasons.none { "subdomain" in it && "phishing" in it })
    }

    @Test
    fun `random subdomain unrelated to any brand is not flagged for brand misuse`() {
        val r = SmartLinkGuardRules.inspect("https://foo.bar.example.org")
        assertTrue(r.reasons.none { "brand" in it && "subdomain" in it })
    }

    // ---- Patch 4: weighted escalation ----

    @Test
    fun `single caution flag stays Caution`() {
        // http on an allowlisted domain — 1 caution flag only
        val r = SmartLinkGuardRules.inspect("http://google.com")
        assertEquals(URLVerdict.Caution, r.verdict)
    }

    @Test
    fun `two caution flags escalate to Danger`() {
        // http on a known shortener → 2 caution flags (http + shortener)
        val r = SmartLinkGuardRules.inspect("http://bit.ly/x")
        assertEquals(URLVerdict.Danger, r.verdict)
        assertTrue(r.reasons.any { "escalated to Danger" in it })
    }

    @Test
    fun `three caution flags still Danger`() {
        // http + shortener + suspicious TLD (ensure escalation isn't accidentally capped)
        val r = SmartLinkGuardRules.inspect("http://cutt.ly/bad.zip")
        assertEquals(URLVerdict.Danger, r.verdict)
    }

    // ---- No-regression pins for existing behavior ----

    @Test
    fun `Punycode IDN remains Danger as before`() {
        val r = SmartLinkGuardRules.inspect("https://xn--paypal-test.com")
        assertEquals(URLVerdict.Danger, r.verdict)
    }

    @Test
    fun `IP literal remains Danger`() {
        val r = SmartLinkGuardRules.inspect("http://192.168.1.1/login")
        assertEquals(URLVerdict.Danger, r.verdict)
    }

    @Test
    fun `allowlisted https site is Safe`() {
        val r = SmartLinkGuardRules.inspect("https://github.com")
        assertEquals(URLVerdict.Safe, r.verdict)
    }

    @Test
    fun `typosquat remains Danger`() {
        val r = SmartLinkGuardRules.inspect("https://gooogle.com")  // 3 o's
        assertEquals(URLVerdict.Danger, r.verdict)
    }

    // ---- Direct helper tests ----

    @Test
    fun `containsHomoglyph detects Cyrillic`() {
        assertTrue(SmartLinkGuardRules.containsHomoglyph("pаypal.com"))
        assertTrue(SmartLinkGuardRules.containsHomoglyph("gооgle.com"))
    }

    @Test
    fun `containsHomoglyph passes plain ASCII`() {
        assertEquals(false, SmartLinkGuardRules.containsHomoglyph("paypal.com"))
        assertEquals(false, SmartLinkGuardRules.containsHomoglyph("github.com"))
    }

    @Test
    fun `brandMisusedAsSubdomain detects paypal shape`() {
        val brand = SmartLinkGuardRules.brandMisusedAsSubdomain("paypal.com.scam.ru", "scam.ru")
        assertEquals("paypal", brand)
    }

    @Test
    fun `brandMisusedAsSubdomain returns null for self-brand`() {
        assertNull(SmartLinkGuardRules.brandMisusedAsSubdomain("support.paypal.com", "paypal.com"))
    }

    @Test
    fun `brandMisusedAsSubdomain returns null for unrelated subdomain`() {
        assertNull(SmartLinkGuardRules.brandMisusedAsSubdomain("foo.bar.example.org", "example.org"))
    }
}
