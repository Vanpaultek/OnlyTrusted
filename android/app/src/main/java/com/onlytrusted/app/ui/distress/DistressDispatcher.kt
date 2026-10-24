// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.distress

import android.content.Context
import com.onlytrusted.app.OnlyTrustedApp
import com.onlytrusted.app.data.db.entities.SecurityEvent
import com.onlytrusted.app.ui.duress.DuressState
import kotlinx.coroutines.flow.first

/**
 * Single entry-point for firing a distress signal after the countdown lands.
 *
 * Any trigger surface (SOS FAB, shake pattern, volume-down 4×) ultimately runs this
 * same function — one place for the whole dispatch so the behaviour never diverges
 * between how the signal was initiated. Steps:
 *
 *   1. Best-effort one-shot location fetch via [LocationFetcher] (3-second timeout —
 *      falls back to null / "Location: unavailable" rather than blocking the SMS).
 *   2. Read the current trusted-contacts list; pair each with their first phone number.
 *   3. Build the pre-filled distress body via [DistressMessageBuilder].
 *   4. Fire the SMS composer (multi-recipient) + auto-open the phone dialer via
 *      [DistressSender].
 *   5. Log a [SecurityEvent.TYPE_DISTRESS_FIRED] row for the audit trail.
 *
 * Returns a human-readable result string the caller can show in a confirm dialog.
 */
object DistressDispatcher {

    suspend fun dispatch(context: Context): String {
        val app = context.applicationContext as OnlyTrustedApp
        val location = LocationFetcher.fetchOnce(context)
        val contacts = app.trustedContacts.allContacts.first()
        val recipients = contacts.mapNotNull { c ->
            c.numbers.firstOrNull()?.let { num ->
                DistressRecipient(phoneNumber = num.normalisedNumber, displayName = c.contact.displayName)
            }
        }
        val ownerName = DuressState.ownerName(context)
        val body = DistressMessageBuilder.build(ownerName = ownerName, location = location)
        val result = DistressSender.fire(
            context = context,
            recipients = recipients,
            body = body,
            alsoAutoDialFirst = true
        )
        app.repository.log(
            type = SecurityEvent.TYPE_DISTRESS_FIRED,
            packageName = null,
            detail = "Distress signal fired to ${recipients.size} recipient(s); " +
                "location=${if (location != null) "yes" else "unavailable"}"
        )
        return when (result) {
            is DistressSender.FireResult.Dispatched ->
                "Distress alert ready — tap Send in Messages to broadcast to ${recipients.size} contact(s)."
            is DistressSender.FireResult.NoRecipients ->
                "No emergency contacts configured — add at least one in Contacts."
            is DistressSender.FireResult.ComposerFailed ->
                "Couldn't open Messages. Dial a trusted contact manually."
        }
    }
}
