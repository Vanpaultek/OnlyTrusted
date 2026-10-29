// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.contactscan

import android.content.Context
import android.provider.ContactsContract

/**
 * Scans the system contacts database against the bundled scam-numbers feed. All work is
 * local — the contact list is read via ContentResolver, normalised to E.164-ish digits,
 * and compared against `assets/threatintel/scam_numbers.v1.txt`. Nothing is transmitted
 * off the device.
 *
 * Load the scam set once per scan (small file today, ~34 entries; sized for the
 * production ~5MB bundled list later). ContentResolver scan is single-pass across all
 * CommonDataKinds.Phone rows.
 */
object ContactsScamScanner {

    data class ScamMatch(
        val contactName: String,
        val phoneNumber: String,
        val normalisedNumber: String
    )

    data class ScamScanReport(
        val contactsScanned: Int,
        val numbersScanned: Int,
        val matches: List<ScamMatch>
    )

    fun scan(ctx: Context): ScamScanReport {
        val scamNumbers = loadScamNumbers(ctx)
        if (scamNumbers.isEmpty()) {
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
                if (scamNumbers.contains(normalised)) {
                    matches.add(ScamMatch(contactName = name, phoneNumber = rawNumber, normalisedNumber = normalised))
                }
            }
        }

        return ScamScanReport(
            contactsScanned = contactIds.size,
            numbersScanned = numbersScanned,
            matches = matches
        )
    }

    /**
     * Minimum entry count below which the bundled feed is treated as
     * "sample / illustrative only" by the UI (Dashboard shows a different
     * tile state so users aren't misled by a "0 scam contacts" count that
     * actually means "we don't have a real feed yet").
     *
     * Current shipping file: 34 entries (placeholder for development).
     * Production target: 500,000+ entries sourced from FTC Consumer
     * Sentinel + FCC Do-Not-Call violator list + EU equivalents.
     */
    const val REAL_FEED_MIN_ENTRIES = 500

    /** How many non-comment entries are in the currently-bundled feed. */
    fun loadedFeedSize(ctx: Context): Int = loadScamNumbers(ctx).size

    /** True iff the loaded feed is large enough to be considered a real
     *  (not placeholder) scam-detection source. */
    fun isRealFeed(ctx: Context): Boolean =
        loadedFeedSize(ctx) >= REAL_FEED_MIN_ENTRIES

    /** Load the bundled scam-numbers list into a set for O(1) lookup during scan. */
    private fun loadScamNumbers(ctx: Context): Set<String> {
        return runCatching {
            ctx.assets.open("threatintel/scam_numbers.v1.txt").bufferedReader().use { reader ->
                reader.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .map { normalise(it) }
                    .toSet()
            }
        }.getOrDefault(emptySet())
    }

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
