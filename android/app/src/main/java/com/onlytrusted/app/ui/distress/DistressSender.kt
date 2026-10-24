// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.distress

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

/**
 * Fires the two user-visible OS intents at the heart of a distress signal:
 *   • Multi-recipient SMS composer with distress body pre-filled.
 *   • Phone dialer with the first emergency contact pre-dialed.
 *
 * Both intents are the user's OWN carrier channels (SMS over GSM/CDMA, voice call
 * over PSTN) — OnlyTrusted sends nothing over the internet during a distress fire.
 * The honest claim stays intact.
 *
 * Why composer + dialer instead of silent-send + auto-call:
 *   SmsManager.sendTextMessage can send silently with SEND_SMS permission; ACTION_CALL
 *   can auto-dial with CALL_PHONE permission. We deliberately show the composer and
 *   open the dialer instead for two reasons:
 *     1. User gets one last chance to cancel if the trigger fired accidentally.
 *     2. Silent SMS without any visible UI feels malware-adjacent; a composer with
 *        pre-filled text is unambiguously user-authored.
 *   Duress Mode (a different, specifically-silent flow) uses the silent path — but
 *   that's a coerced-unlock scenario where visible UI would blow the owner's cover.
 */
object DistressSender {

    /**
     * Dispatch the distress intents. Returns true if at least the SMS composer was
     * launched successfully — if there's no default SMS app or no recipients, returns
     * false and the caller can surface an error.
     *
     * Flow:
     *   1. Launch SMS composer with every recipient's number pre-populated and the
     *      distress body pre-filled. User taps Send once → one broadcast to everyone.
     *   2. In parallel (so the dialer is already waiting when the user dismisses the
     *      composer), launch the phone dialer with the first recipient pre-dialed.
     */
    fun fire(
        context: Context,
        recipients: List<DistressRecipient>,
        body: String,
        alsoAutoDialFirst: Boolean = true
    ): FireResult {
        if (recipients.isEmpty()) return FireResult.NoRecipients

        val numbers = recipients.map { it.phoneNumber.trim() }.filter { it.isNotEmpty() }
        if (numbers.isEmpty()) return FireResult.NoRecipients

        val smsLaunched = launchSmsComposer(context, numbers, body)
        if (alsoAutoDialFirst && numbers.isNotEmpty()) {
            // Open the dialer in the background queue — on most launchers it'll stack
            // behind the SMS composer so the user sees it after they tap Send.
            launchDialer(context, numbers.first())
        }
        return if (smsLaunched) FireResult.Dispatched else FireResult.ComposerFailed
    }

    /**
     * Opens the default SMS app with multiple recipients + body pre-filled. We use
     * the `smsto:` URI scheme which every stock SMS app on Android understands;
     * numbers are joined with `;` which is the de-facto separator the AOSP + Google
     * Messages app parse correctly for group-broadcast MMS.
     */
    private fun launchSmsComposer(
        context: Context,
        numbers: List<String>,
        body: String
    ): Boolean {
        val uri = Uri.parse("smsto:" + numbers.joinToString(";"))
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", body)
            // Some OEM SMS apps read "exit_on_sent" to pop back automatically.
            putExtra("exit_on_sent", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    /**
     * Opens the phone dialer with [number] pre-dialed. Uses ACTION_DIAL (no permission
     * needed) rather than ACTION_CALL (requires CALL_PHONE + auto-places the call) so
     * the user keeps a one-tap confirmation. If the user has previously granted
     * CALL_PHONE we still use DIAL — the composer dismissal + one-tap-call sequence
     * mirrors every other emergency-dial pattern users expect (e.g. 911 contacts).
     */
    private fun launchDialer(context: Context, number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /** Returned by [fire] so the UI knows whether to show success / error. */
    sealed class FireResult {
        /** SMS composer launched successfully; dialer intent attempted. */
        data object Dispatched : FireResult()

        /** No valid recipients — tell the user to add emergency contacts first. */
        data object NoRecipients : FireResult()

        /** SMS composer couldn't start (no default SMS app, etc). */
        data object ComposerFailed : FireResult()
    }

    /** Convenience check so UI can gate the SOS button if the OS has no SMS app. */
    fun deviceCanSendSms(context: Context): Boolean {
        val probe = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:+10000000000"))
        return probe.resolveActivity(context.packageManager) != null
    }

    /** Check for CALL_PHONE permission — purely informational, we use ACTION_DIAL anyway. */
    @Suppress("unused")
    fun hasAutoCallPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
}
