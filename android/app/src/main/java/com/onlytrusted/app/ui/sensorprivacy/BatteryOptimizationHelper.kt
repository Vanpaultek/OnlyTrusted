// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Battery optimisation exemption for the lock-watcher foreground service.
 *
 * Android aggressively kills background services on OEM builds that layer
 * stricter policies on top of AOSP (Samsung, Xiaomi, Huawei, OnePlus etc.).
 * To keep [SensorPrivacyForegroundService] alive during long screen-off
 * periods, the user must grant OnlyTrusted a battery optimisation exemption.
 *
 * This helper DOES NOT force the exemption — it reads the current state and
 * opens the system settings page where the user can grant it. That is both
 * the Google Play policy requirement (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
 * is a user-granted permission, not auto-granted) and the honest approach.
 */
object BatteryOptimizationHelper {

    /** True if OnlyTrusted is already exempted from battery optimisation. */
    fun isExempted(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
    }

    /**
     * Opens the system "Ignore battery optimizations" settings page targeted
     * at this app. Returns true if the Intent resolved and was dispatched,
     * false otherwise.
     *
     * Note: Google Play policy requires that apps requesting
     * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS have a clear user-benefit
     * justification. See docs/SENSOR_PRIVACY_ARCHITECTURE.md § "Battery
     * optimisation exemption" for the rationale we show in the UI.
     */
    fun requestExemption(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolved = intent.resolveActivity(context.packageManager)
        if (resolved == null) return false
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    /** Opens the general battery optimisation settings list — fallback if the
     *  targeted exemption Intent is unavailable on the OEM build. */
    fun openGeneralSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
