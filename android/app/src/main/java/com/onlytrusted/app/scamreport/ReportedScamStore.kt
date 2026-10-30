// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.scamreport

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local-only store for user-submitted "report as scam" entries. The whole
 * point of this feature is **honest crowd-sourcing without a server**:
 *
 *  - Reports live in [EncryptedSharedPreferences] on this device only.
 *  - Nothing is uploaded automatically. Nothing ever.
 *  - User explicitly chooses when (and whether) to export reports via the
 *    share sheet to a destination they pick (email the maintainer, submit a
 *    GitHub issue, post to a forum, or just keep them local).
 *  - Zero-cloud brand promise intact.
 *
 * Dedupe + ring-buffer:
 *  - Reports keyed by [ReportedScam.normalisedNumber]; re-reporting the same
 *    number just updates the latest timestamp + category instead of adding
 *    a duplicate.
 *  - Max 1000 entries. When the cap is hit, oldest entry is evicted.
 *
 * Threat / data model notes:
 *  - Normalised number is PII — storing encrypted at rest.
 *  - Category is low-dimension (6 values) — not sensitive but encrypted
 *    anyway because the whole blob rides together.
 *  - No reporter identity, no device fingerprint, no timestamp of the
 *    original call — just the number + the user's classification + when
 *    they reported it.
 */
object ReportedScamStore {

    private const val PREFS = "ot_reported_scam_store_v1"
    private const val KEY_REPORTS_JSON = "reports_json"
    private const val MAX_REPORTS = 1000

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    enum class Category(val label: String) {
        ROBOCALL("Robocall"),
        PHISHING("Phishing — asked for credentials"),
        IMPERSONATION("Impersonation — fake bank / tax office / IRS"),
        SPAM("Spam / sales"),
        HARASSMENT("Harassment or abuse"),
        OTHER("Other");

        companion object {
            fun fromName(name: String?): Category =
                entries.firstOrNull { it.name == name } ?: OTHER
        }
    }

    @Serializable
    data class ReportedScam(
        val normalisedNumber: String,
        val categoryName: String,
        val reportedAtMs: Long,
    ) {
        val category: Category get() = Category.fromName(categoryName)
    }

    /** Append-or-update a report for [normalisedNumber]. Returns the final
     *  count of reports after dedupe + ring-buffer eviction. */
    @Synchronized
    fun add(
        context: Context,
        normalisedNumber: String,
        category: Category,
        nowMs: Long = System.currentTimeMillis(),
    ): Int {
        if (normalisedNumber.isBlank()) return list(context).size
        val reports = list(context).toMutableList()
        // Dedupe — update existing entry if present
        val existingIndex = reports.indexOfFirst { it.normalisedNumber == normalisedNumber }
        val entry = ReportedScam(normalisedNumber, category.name, nowMs)
        if (existingIndex >= 0) {
            reports[existingIndex] = entry
        } else {
            reports.add(entry)
            // Ring-buffer cap: drop oldest entries until under limit
            while (reports.size > MAX_REPORTS) {
                reports.removeAt(0)
            }
        }
        write(context, reports)
        return reports.size
    }

    /** All reports sorted most-recent-first. */
    fun list(context: Context): List<ReportedScam> {
        val raw = prefs(context)?.getString(KEY_REPORTS_JSON, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ReportedScam>>(raw)
                .sortedByDescending { it.reportedAtMs }
        }.getOrDefault(emptyList())
    }

    fun count(context: Context): Int = list(context).size

    /** Remove a single report by normalised number. */
    @Synchronized
    fun remove(context: Context, normalisedNumber: String) {
        val reports = list(context).filterNot { it.normalisedNumber == normalisedNumber }
        write(context, reports)
    }

    /** Clear every report (user may want to purge after exporting). */
    @Synchronized
    fun clearAll(context: Context) {
        prefs(context)?.edit()?.remove(KEY_REPORTS_JSON)?.apply()
    }

    /** Format every stored report as a GitHub-issue-ready markdown block.
     *  The maintainer imports accepted entries into the bundled feed on the
     *  next release. */
    fun formatForGitHub(context: Context): String {
        val reports = list(context)
        if (reports.isEmpty()) return "_No scam reports yet._"
        return buildString {
            appendLine("# OnlyTrusted — Scam report submission")
            appendLine()
            appendLine("- Reports: ${reports.size}")
            appendLine("- Generated: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}")
            appendLine()
            appendLine("| Number | Category | Reported |")
            appendLine("|---|---|---|")
            reports.forEach { r ->
                val dateString = java.text.DateFormat.getDateInstance().format(java.util.Date(r.reportedAtMs))
                appendLine("| `${r.normalisedNumber}` | ${r.category.label} | $dateString |")
            }
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("Submitted via OnlyTrusted → Settings → \"Share scam reports\". No identifying metadata attached.")
        }
    }

    /** Format as plain CSV — alternative export format for spreadsheet / data-import users. */
    fun formatAsCsv(context: Context): String {
        val reports = list(context)
        return buildString {
            appendLine("normalised_number,category,reported_at_iso8601")
            reports.forEach { r ->
                val iso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                    .format(java.util.Date(r.reportedAtMs))
                appendLine("${r.normalisedNumber},${r.category.name},$iso")
            }
        }
    }

    // --- Internals -----------------------------------------------------------

    private fun write(context: Context, reports: List<ReportedScam>) {
        prefs(context)?.edit()?.putString(KEY_REPORTS_JSON, json.encodeToString(reports))?.apply()
    }

    private fun prefs(context: Context): SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext, PREFS, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (t: Throwable) {
        android.util.Log.w("OTReportedScamStore", "EncryptedSharedPreferences init failed", t)
        null
    }
}
