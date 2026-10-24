// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.distress

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Core data types + message builder for the Distress Signal feature.
 *
 * The honest claim OnlyTrusted makes — "no cloud, no server, no account, zero-network" —
 * holds through this feature: we don't send any traffic from OnlyTrusted ourselves. On
 * trigger we open the OS SMS composer (user taps Send) with a message pre-filled, and
 * open the phone dialer with a number pre-dialed. Both are user-visible, user-confirmed
 * OS flows that use the device's own carrier channels. OnlyTrusted never phones home.
 *
 * Trigger surfaces (Phase 1 ships only in-app button; shake + volume triggers follow):
 *   • In-app Dashboard SOS FAB (immediate, with 3 s cancel countdown)
 *   • Shake-to-trigger (Phase 2 — 5 shakes in 3 s via accelerometer)
 *   • Volume-down 4× within 2 s (Phase 2 — Android only; iOS blocks volume keys outside app)
 *
 * On trigger the flow is:
 *   1. 3-second cancel countdown overlay (prevents accidental fires)
 *   2. Best-effort one-shot location fetch (3 s timeout → fall back without coords)
 *   3. Open OS SMS composer with ALL emergency contacts pre-selected + message pre-filled
 *      containing distress text + Google Maps link + timestamp
 *   4. After the SMS composer dismisses, auto-dial the first emergency contact
 */

/** A single emergency-group recipient — a phone number + display label. */
data class DistressRecipient(
    val phoneNumber: String,
    val displayName: String
)

/** Snapshot of the device's location at the moment the distress signal fired. */
data class DistressLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val capturedAtMillis: Long
) {
    /** Google Maps web URL that opens the pin in any browser or the Maps app. */
    fun mapsUrl(): String = "https://maps.google.com/?q=$latitude,$longitude"
}

/** Builds the pre-filled distress message body. */
object DistressMessageBuilder {

    private val TIMESTAMP_FORMAT = SimpleDateFormat("MMM d, h:mm a", Locale.US)

    /**
     * Pre-filled message the user sees in the SMS composer before tapping Send. The
     * emoji + "EMERGENCY" prefix is deliberate — recipients should recognise it as
     * urgent at a glance even in notification previews.
     *
     * If `location` is null (fetch timed out, permission denied, or no GPS fix), we
     * append "(location unavailable)" instead of omitting silently — the recipient
     * knows the owner is in distress but their current coordinates can't be shared.
     */
    fun build(
        ownerName: String,
        location: DistressLocation?,
        now: Date = Date()
    ): String {
        val name = ownerName.ifBlank { "an OnlyTrusted user" }
        val timestamp = TIMESTAMP_FORMAT.format(now)
        val locationLine = location?.let {
            "Location: ${it.mapsUrl()}" +
                (it.accuracyMeters?.let { acc -> " (±${acc.toInt()} m)" } ?: "")
        } ?: "Location: unavailable"
        return "🚨 OnlyTrusted EMERGENCY from $name — I need help. " +
            "$locationLine. Sent $timestamp."
    }
}
