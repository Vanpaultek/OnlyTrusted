// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.security

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import com.onlytrusted.app.services.SpamLabelNotificationListener

/**
 * Reads the two system grants that spam-call protection depends on:
 *
 *  1. **Default Call Screener role** ([RoleManager.ROLE_CALL_SCREENING])
 *     Without this, [com.onlytrusted.app.services.CallBlockerService]
 *     never receives `onScreenCall` — Google Phone or AT&T Active Armor
 *     handles the call before we can touch it. Nothing we do matters.
 *
 *  2. **Notification Access** (user grants in Settings → Apps → Special
 *     app access → Notification access)
 *     Without this, [SpamLabelNotificationListener] can't read the Phone
 *     app's "Spam Risk" / "Scam Likely" labels and can't auto-learn new
 *     spam numbers into our blocklist.
 *
 * Dashboard uses this to decide whether to show the nudge card.
 */
object SpamProtectionStatus {

    /** True if OnlyTrusted holds [RoleManager.ROLE_CALL_SCREENING] (API 29+)
     *  or is the default dialer (API < 29). Only the first case lets us
     *  silently reject calls without becoming the dialer. */
    fun isDefaultCallScreener(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val role = context.getSystemService(RoleManager::class.java) ?: return false
            runCatching { role.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) }.getOrDefault(false)
        } else {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                ?: return false
            runCatching { tm.defaultDialerPackage == context.packageName }.getOrDefault(false)
        }
    }

    /** True if the user has granted OnlyTrusted "Notification access" so
     *  our [SpamLabelNotificationListener] can read system Phone-app
     *  notifications and learn carrier spam labels. */
    fun hasNotificationAccess(context: Context): Boolean {
        val enabled = runCatching {
            Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ).orEmpty()
        }.getOrDefault("")
        val us = ComponentName(context, SpamLabelNotificationListener::class.java).flattenToString()
        // The listener string is a `:`-separated list of ComponentNames.
        return enabled.split(':').any { it.equals(us, ignoreCase = true) }
    }
}
