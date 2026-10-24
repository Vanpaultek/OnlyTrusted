// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.distress

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri

/**
 * Opens WhatsApp with the distress message pre-filled, so the owner can broadcast to
 * their emergency contact GROUP in a few extra taps after the SMS fires.
 *
 * Why we can't auto-send to a group:
 *   WhatsApp has no third-party API that can create a group, add members, or
 *   programmatically send to a specific group chat — those actions are reserved for
 *   WhatsApp's own UI (Terms of Service). The ONLY integrations WhatsApp exposes to
 *   third-party apps are:
 *     • `whatsapp://send?phone=<num>&text=<text>` — opens a 1-on-1 chat with the number,
 *       text pre-filled, user taps Send.
 *     • `whatsapp://send?text=<text>` — opens WhatsApp with text pre-filled; user picks
 *       which chat to send to (contact OR group).
 *     • `https://chat.whatsapp.com/<invite-id>` — opens the group preview if the user
 *       isn't yet a member, OR opens the group chat if they are.
 *
 * The honest flow we ship:
 *   1. Owner creates a WhatsApp group with their emergency contacts, manually, the
 *      first time (OnlyTrusted guides them with a setup card). Pastes the group's
 *      invite link into [Prefs.setGroupInviteUrl].
 *   2. On distress trigger, after the SMS composer opens, OnlyTrusted ALSO opens
 *      WhatsApp via `whatsapp://send?text=<distress-msg>` with the full message
 *      pre-filled + copies the group invite link to the clipboard so the owner can
 *      paste it to open the group directly (or picks the group from WhatsApp's chat
 *      selector).
 *   3. From the group chat, the owner can one-tap the voice-call icon to start a
 *      group call to every member — using WhatsApp's own native group-call feature.
 *
 * This keeps the honest-claim intact (OnlyTrusted sends zero traffic itself; everything
 * flows through the user's own WhatsApp account via standard URL schemes) and respects
 * WhatsApp's Terms of Service.
 */
object WhatsAppDistressSender {

    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

    /** Returns true if a WhatsApp (consumer or Business) is installed. */
    fun isInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(WHATSAPP_PACKAGE, 0); true
        } catch (_: Exception) {
            try {
                pm.getPackageInfo(WHATSAPP_BUSINESS_PACKAGE, 0); true
            } catch (_: Exception) { false }
        }
    }

    /**
     * Opens WhatsApp with [body] pre-filled. If the user has saved their emergency
     * group's invite link in [Prefs], we also copy it to the clipboard so the owner
     * can paste it into WhatsApp's search bar to open the group directly.
     *
     * Returns true if WhatsApp was launched successfully.
     */
    fun fire(context: Context, body: String): Boolean {
        if (!isInstalled(context)) return false

        // Copy the group invite link to the clipboard if saved — one-tap paste to
        // open the group chat.
        Prefs.groupInviteUrl(context)?.takeIf { it.isNotBlank() }?.let { link ->
            runCatching {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("OnlyTrusted emergency group", link))
            }
        }

        val encoded = Uri.encode(body)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=$encoded")
            setPackage(WHATSAPP_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            // Consumer not installed, try Business
            runCatching {
                intent.setPackage(WHATSAPP_BUSINESS_PACKAGE)
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
    }

    /** Opens the saved group invite link directly (if the user has saved one). */
    fun openGroupInviteIfSaved(context: Context): Boolean {
        val link = Prefs.groupInviteUrl(context)?.takeIf { it.isNotBlank() } ?: return false
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    /** Per-user storage for the WhatsApp emergency group's invite URL. Not sensitive. */
    object Prefs {
        private const val PREFS = "distress_whatsapp_prefs"
        private const val KEY_GROUP_URL = "group_invite_url"

        private fun prefs(ctx: Context): SharedPreferences =
            ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /** Returns the saved WhatsApp group invite URL, or null/blank if unset. */
        fun groupInviteUrl(ctx: Context): String? = prefs(ctx).getString(KEY_GROUP_URL, null)

        /**
         * Validates + saves the group invite URL. Accepts the two valid WhatsApp
         * invite URL formats: `https://chat.whatsapp.com/<id>` and the alternative
         * `https://wa.me/<id>` sometimes shown in WhatsApp's share sheet. Everything
         * else is rejected so we don't send users to a broken link under distress.
         */
        fun setGroupInviteUrl(ctx: Context, url: String): Boolean {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) {
                prefs(ctx).edit().remove(KEY_GROUP_URL).apply()
                return true
            }
            if (!trimmed.startsWith("https://chat.whatsapp.com/") &&
                !trimmed.startsWith("https://wa.me/")
            ) return false
            prefs(ctx).edit().putString(KEY_GROUP_URL, trimmed).apply()
            return true
        }
    }
}
