// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.contactscan

import android.content.Context
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import com.onlytrusted.app.OnlyTrustedApp
import com.onlytrusted.app.privacy.PhoneUtils
import java.util.Locale

/**
 * Scans the system contacts database against two bundled threat-intel sources,
 * both local — nothing is transmitted off the device:
 *
 *  1. **Exact-number list** — `assets/threatintel/scam_numbers.v1.txt`
 *     Specific numbers documented in FCC enforcement filings / public reports.
 *     Match = known-scam number.
 *
 *  2. **Pattern list** — `assets/threatintel/scam_patterns.v1.txt`
 *     Prefix patterns (country codes, area codes, premium-rate ranges) with
 *     documented scam history. Match = suspicious pattern.
 *
 * Patterns catch orders of magnitude more real scams than a stale exact-number
 * blocklist, because scammers rotate numbers frequently but almost never
 * leave their origin country / area code.
 *
 * ### False-positive guards (v2)
 *
 *  - **Home-country skip** — any pattern whose prefix matches the user's own
 *    country code (detected from `TelephonyManager.simCountryIso` with
 *    `Locale.getDefault().country` as fallback) is skipped. A user in India
 *    doesn't need +91 flagged as a scam origin for every one of their 1,400
 *    local contacts. Specific sub-ranges (longer than the country code, e.g.
 *    +1876 for Jamaica inside the NANP +1 space) still fire — those identify
 *    real scam territory inside the user's home dial plan.
 *
 *  - **Trusted-contact skip** — any number already in the user's Trusted
 *    vault is never scam-flagged. The user explicitly declared them trusted,
 *    which is a stronger signal than any prefix heuristic.
 */
object ContactsScamScanner {

    enum class MatchType {
        /** Exact match in `scam_numbers.v1.txt` — documented scam number. */
        KNOWN_SCAM,
        /** Prefix match in `scam_patterns.v1.txt` — suspicious range. */
        SUSPICIOUS_PATTERN,
    }

    enum class Severity { CRITICAL, HIGH, MEDIUM }

    data class ScamMatch(
        val contactName: String,
        val phoneNumber: String,
        val normalisedNumber: String,
        val matchType: MatchType,
        val severity: Severity,
        val label: String,
    )

    data class ScamScanReport(
        val contactsScanned: Int,
        val numbersScanned: Int,
        val matches: List<ScamMatch>,
        /** User's detected home country code (e.g. "+91") or null when
         *  the device has no SIM and no useful locale country. Shown in the
         *  UI so users understand why their local country isn't being
         *  flagged en-masse. */
        val homeCountryCode: String? = null,
        /** How many contacts the scanner skipped because they are already
         *  in the Trusted vault. Shown in the UI as a transparency aid. */
        val trustedSkipped: Int = 0,
        /** How many pattern matches were suppressed because they matched
         *  the user's home country code. Shown so users can audit the
         *  skip logic. */
        val homeCountrySkipped: Int = 0,
    )

    data class Pattern(val prefix: String, val severity: Severity, val label: String)

    fun scan(ctx: Context): ScamScanReport {
        val scamNumbers = loadScamNumbers(ctx)
        val scamPatterns = loadScamPatterns(ctx)
        val homeCountryCode = detectHomeCountryCode(ctx)
        if (scamNumbers.isEmpty() && scamPatterns.isEmpty()) {
            return ScamScanReport(0, 0, emptyList(), homeCountryCode = homeCountryCode)
        }

        val matches = mutableListOf<ScamMatch>()
        val contactIds = mutableSetOf<Long>()
        var numbersScanned = 0
        var trustedSkipped = 0
        var homeCountrySkipped = 0

        // Resolve the Trusted-vault DAO once — scanning 1,000+ contacts
        // against it needs a tight sync path. isTrustedSync is backed by
        // an indexed SQLCipher query so each lookup is O(log n).
        val dao = (ctx.applicationContext as? OnlyTrustedApp)?.database?.trustedContactDao()

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        ctx.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                numbersScanned++
                val contactId = cursor.getLong(0)
                contactIds.add(contactId)
                val name = cursor.getString(1).orEmpty().ifBlank { "Unknown" }
                val rawNumber = cursor.getString(2).orEmpty()
                val normalised = normaliseForScan(rawNumber)
                // Use the repo's canonical normaliser for the trusted-DB
                // comparison so our key matches what's actually stored.
                val normalisedCanonical = PhoneUtils.normalise(rawNumber)

                // Trusted-vault exemption. User explicitly declared this
                // person trusted — a prefix heuristic cannot override that.
                if (dao != null && normalisedCanonical.isNotBlank() &&
                    dao.isTrustedSync(normalisedCanonical)) {
                    trustedSkipped++
                    continue
                }

                // Exact-match (KNOWN_SCAM) wins over pattern match. A number
                // may be in both; the more specific signal is more useful.
                if (scamNumbers.contains(normalised)) {
                    matches += ScamMatch(
                        contactName = name,
                        phoneNumber = rawNumber,
                        normalisedNumber = normalised,
                        matchType = MatchType.KNOWN_SCAM,
                        severity = Severity.CRITICAL,
                        label = "Documented scam number",
                    )
                    continue
                }
                // Pattern match: find the LONGEST matching prefix (most
                // specific pattern wins — e.g. +44908 beats +44).
                val patternHit = scamPatterns
                    .filter { normalised.startsWith(it.prefix) }
                    .maxByOrNull { it.prefix.length }
                if (patternHit != null) {
                    // Home-country skip: if the user is in India (+91)
                    // and we matched a 3-char "+91" pattern, swallow it.
                    // A longer prefix like "+91812-something" would be a
                    // specific area code and legitimately scam-heavy — in
                    // that case keep the match.
                    if (homeCountryCode != null &&
                        patternHit.prefix.length <= homeCountryCode.length &&
                        patternHit.prefix == homeCountryCode) {
                        homeCountrySkipped++
                        continue
                    }
                    matches += ScamMatch(
                        contactName = name,
                        phoneNumber = rawNumber,
                        normalisedNumber = normalised,
                        matchType = MatchType.SUSPICIOUS_PATTERN,
                        severity = patternHit.severity,
                        label = patternHit.label,
                    )
                }
            }
        }

        return ScamScanReport(
            contactsScanned = contactIds.size,
            numbersScanned = numbersScanned,
            matches = matches,
            homeCountryCode = homeCountryCode,
            trustedSkipped = trustedSkipped,
            homeCountrySkipped = homeCountrySkipped,
        )
    }

    /**
     * Minimum entry count below which the bundled feed is treated as
     * "sample / illustrative only" by the UI. We count exact numbers +
     * patterns combined — pattern-based detection catches far more real
     * scams than any exact-number blocklist of the same size.
     */
    const val REAL_FEED_MIN_ENTRIES = 50

    /** How many non-comment entries are in the currently-bundled feed
     *  (exact numbers + patterns combined). */
    fun loadedFeedSize(ctx: Context): Int =
        loadScamNumbers(ctx).size + loadScamPatterns(ctx).size

    /** True iff the loaded feed is large enough to be considered a real
     *  (not placeholder) scam-detection source. */
    fun isRealFeed(ctx: Context): Boolean =
        loadedFeedSize(ctx) >= REAL_FEED_MIN_ENTRIES

    /** Load the bundled scam-numbers list into a set for O(1) lookup during scan. */
    private fun loadScamNumbers(ctx: Context): Set<String> = runCatching {
        ctx.assets.open("threatintel/scam_numbers.v1.txt").bufferedReader().use { reader ->
            reader.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { normaliseForScan(it) }
                .toSet()
        }
    }.getOrDefault(emptySet())

    /** Load prefix patterns from `scam_patterns.v1.txt`.
     *  Format per line: `<prefix>  <severity>  <label>` */
    private fun loadScamPatterns(ctx: Context): List<Pattern> = runCatching {
        ctx.assets.open("threatintel/scam_patterns.v1.txt").bufferedReader().use { reader ->
            reader.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    // Split on whitespace runs; need at least 3 tokens.
                    val parts = line.split(Regex("\\s+"), limit = 3)
                    if (parts.size < 3) return@mapNotNull null
                    val severity = runCatching {
                        Severity.valueOf(parts[1].uppercase())
                    }.getOrNull() ?: return@mapNotNull null
                    Pattern(
                        prefix = parts[0],
                        severity = severity,
                        label = parts[2].trim(),
                    )
                }
                .toList()
        }
    }.getOrDefault(emptyList())

    /**
     * Normalise to a comparable form for the scam-pattern database.
     * Deliberately simple: strip to digits, prepend `+` (or `+1` for a bare
     * 10-digit NANP number). This is for pattern-prefix comparison only —
     * the trusted-vault lookup uses [PhoneUtils.normalise] because that is
     * the canonical form stored in the SQLCipher DB.
     */
    private fun normaliseForScan(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "+1$digits"
            else -> "+$digits"
        }
    }

    /**
     * Best-effort detection of the user's "home" country code. Order of
     * preference:
     *   1. `TelephonyManager.simCountryIso` — the carrier's registered
     *      country, strongest signal.
     *   2. `TelephonyManager.networkCountryIso` — current roaming network,
     *      only useful when the SIM reports blank.
     *   3. `Locale.getDefault().country` — fallback for Wi-Fi-only tablets.
     *
     * Returns the dial-plan prefix (e.g. "+91" for India, "+1" for the US)
     * or null if none of the above produces a known ISO-3166 country.
     *
     * The mapping table is deliberately short — only the ISO codes we need
     * to actually skip patterns for. Unknown country → no skip, scanner
     * behaves as if there is no home country.
     */
    fun detectHomeCountryCode(ctx: Context): String? {
        val tm = ctx.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val iso = sequenceOf(
            runCatching { tm?.simCountryIso }.getOrNull(),
            runCatching { tm?.networkCountryIso }.getOrNull(),
            Locale.getDefault().country,
        )
            .mapNotNull { it?.trim()?.takeIf { s -> s.isNotEmpty() }?.uppercase() }
            .firstOrNull()
            ?: return null
        return COUNTRY_ISO_TO_DIAL_PREFIX[iso]
    }

    /** ISO-3166 alpha-2 → E.164 country-calling-code map for the countries
     *  whose patterns the scanner is prepared to skip. Extend as needed. */
    private val COUNTRY_ISO_TO_DIAL_PREFIX: Map<String, String> = mapOf(
        "US" to "+1",
        "CA" to "+1",
        "IN" to "+91",
        "PK" to "+92",
        "GB" to "+44",
        "NG" to "+234",
        "GH" to "+233",
        "BJ" to "+229",
        "TG" to "+228",
        "SN" to "+221",
        "AL" to "+355",
        "RS" to "+381",
        "XK" to "+383",
        "AU" to "+61",
        "DE" to "+49",
        "FR" to "+33",
        "ES" to "+34",
        "IT" to "+39",
        "BR" to "+55",
        "MX" to "+52",
        "ZA" to "+27",
        "KE" to "+254",
        "AE" to "+971",
        "SA" to "+966",
        "SG" to "+65",
        "MY" to "+60",
        "PH" to "+63",
        "ID" to "+62",
        "TH" to "+66",
        "VN" to "+84",
        "JP" to "+81",
        "KR" to "+82",
        "CN" to "+86",
        "TW" to "+886",
        "HK" to "+852",
        "NZ" to "+64",
        "IE" to "+353",
        "NL" to "+31",
        "BE" to "+32",
        "CH" to "+41",
        "AT" to "+43",
        "SE" to "+46",
        "NO" to "+47",
        "DK" to "+45",
        "FI" to "+358",
        "PL" to "+48",
        "PT" to "+351",
        "GR" to "+30",
        "TR" to "+90",
        "IL" to "+972",
        "EG" to "+20",
        "RU" to "+7",
        "UA" to "+380",
    )
}

/** Convenience alias for callers that want to reference the result type without the enclosing class. */
typealias ScamScanReport = ContactsScamScanner.ScamScanReport
