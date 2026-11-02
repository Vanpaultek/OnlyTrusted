// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.onlytrusted.app.OnlyTrustedApp
import com.onlytrusted.app.data.db.entities.BlockedNumber
import com.onlytrusted.app.data.db.entities.SecurityEvent
import com.onlytrusted.app.privacy.PhoneUtils
import com.onlytrusted.app.scamreport.ReportedScamStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Reads system Phone-app notifications that carry spam labels and auto-learns
 * them into our Blocked list + ReportedScamStore so future calls from the
 * same number are silenced by [CallBlockerService].
 *
 * ### Why this exists
 *
 * Google Phone (`com.google.android.dialer`) tags incoming calls as "Spam
 * Risk" / "Scam Likely" / "Suspected spam" based on Google's spam database
 * and the carrier's STIR/SHAKEN attestation level. Android does NOT expose
 * that label to third-party apps during [android.telecom.CallScreeningService.onScreenCall]
 * — only Google Phone sees it. The first time one of those calls reaches
 * the user, our screener can't stop it. But the Phone app posts a
 * notification showing the label; we can parse that notification, extract
 * the number, and remember it. The next call from that number hits our
 * blocklist and we silence it before it rings.
 *
 * ### Permissions
 *
 * NotificationListenerService requires the user to grant Notification
 * Access in Settings → Apps → Special app access → Notification access.
 * We prompt for this grant from the Dashboard spam-detection nudge card.
 * Without the grant this service never runs — no silent fallback, no
 * surprises.
 *
 * ### Privacy
 *
 * Only inspects notifications from the known Phone-app packages (see
 * [DIALER_PACKAGES]). Does NOT read messaging / email / other app
 * notifications. The number we extract is normalised and stored locally
 * only — nothing is transmitted anywhere.
 */
class SpamLabelNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        if (n.packageName !in DIALER_PACKAGES) return
        val extras = n.notification?.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val haystack = (title + " \n " + text + " \n " + bigText + " \n " + subText)

        if (!SPAM_LABEL_REGEX.containsMatchIn(haystack)) return

        val rawNumber = NUMBER_REGEX.find(haystack)?.value ?: run {
            // No parseable number — maybe only a contact display name is
            // shown. We can't block something we can't identify.
            Log.i(TAG, "Spam-labelled notification but no number found in text; ignoring.")
            return
        }
        val normalised = PhoneUtils.normalise(rawNumber)
        if (normalised.isBlank()) return

        learn(normalised, haystack)
    }

    private fun learn(normalisedNumber: String, originalText: String) {
        val app = application as? OnlyTrustedApp ?: return
        scope.launch {
            // 1. Add to the Blocked list so CallBlockerService silences the
            //    next call from this number before it rings.
            val dao = app.database.blockedNumberDao()
            val existing = dao.findByNumber(normalisedNumber)
            if (existing == null) {
                dao.upsert(
                    BlockedNumber(
                        number = normalisedNumber,
                        label = "Carrier-labelled spam",
                        blockCalls = true,
                        blockSms = true,
                    )
                )
            }
            // 2. Append to local scam-report store so it shows up in
            //    Settings → My scam reports and users can share the list.
            runCatching {
                ReportedScamStore.add(
                    context = this@SpamLabelNotificationListener,
                    normalisedNumber = normalisedNumber,
                    category = ReportedScamStore.Category.ROBOCALL,
                )
            }
            // 3. Audit the event so the Log tab shows what we did and why.
            app.repository.log(
                type = SecurityEvent.TYPE_CALL_BLOCKED,
                detail = "from=$normalisedNumber reason=carrier_spam_label " +
                    "label_text=${originalText.replace('\n', ' ').take(120)}",
            )
            Log.i(TAG, "Auto-learned carrier spam number $normalisedNumber")
        }
    }

    companion object {
        private const val TAG = "OTSpamLabel"

        /** Dialer packages whose notifications we read. Adding another
         *  package (Samsung's incall UI etc.) only expands coverage — this
         *  service never reads notifications from any other package. */
        private val DIALER_PACKAGES = setOf(
            "com.google.android.dialer",
            "com.android.dialer",
            "com.samsung.android.incallui",
            "com.samsung.android.app.telephonyui",
        )

        /** Regexes for the spam labels Google Phone / Samsung Phone / AT&T
         *  Call Protect render in the notification. Case-insensitive. */
        private val SPAM_LABEL_REGEX = Regex(
            pattern = "spam risk|scam likely|suspected spam|likely spam|possible spam|\\bspam\\b|robocaller",
            option = RegexOption.IGNORE_CASE,
        )

        /** Minimal phone-number matcher. Matches +XXX..., XXX-XXX-XXXX,
         *  (XXX) XXX-XXXX, or 7+ consecutive digits. Deliberately loose so
         *  we pick up almost any carrier-formatted number; PhoneUtils then
         *  normalises to E.164. */
        private val NUMBER_REGEX = Regex(
            "\\+?[0-9][0-9\\-\\s()]{6,}[0-9]"
        )
    }
}
