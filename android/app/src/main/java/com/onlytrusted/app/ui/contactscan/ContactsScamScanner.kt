// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.contactscan

import android.content.Context
import android.provider.ContactsContract

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
    )

    data class Pattern(val prefix: String, val severity: Severity, val label: String)

    fun scan(ctx: Context): ScamScanReport {
        val scamNumbers = loadScamNumbers(ctx)
        val scamPatterns = loadScamPatterns(ctx)
        if (scamNumbers.isEmpty() && scamPatterns.isEmpty()) {
            return ScamScanReport(0, 0, emptyList())
        }

        val matches = mutableListOf<ScamMatch>()
        val contactIds = mutableSetOf<Long>()
        var numbersScanned = 0

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
                val normalised = normalise(rawNumber)

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
                .map { normalise(it) }
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

    /** Normalise to E.164-ish digits-only form. Strips +, spaces, dashes, parens. */
    private fun normalise(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        // Assume US-centric default: if 10 digits, prepend "1" (NANP country code).
        return when {
            digits.length == 10 -> "+1$digits"
            digits.startsWith("+") -> raw.replace(" ", "").replace("-", "")
            else -> "+$digits"
        }
    }
}

/** Convenience alias for callers that want to reference the result type without the enclosing class. */
typealias ScamScanReport = ContactsScamScanner.ScamScanReport
