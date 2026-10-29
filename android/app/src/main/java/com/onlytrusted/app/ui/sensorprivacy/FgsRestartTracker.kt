// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight counter that records every time
 * [SensorPrivacyForegroundService.onCreate] fires, plus the timestamp of
 * the current running instance. The ratio of restarts to elapsed-since-first-start
 * is the honest signal of how aggressive the OEM's battery killer is on this
 * device — testers can read it off the DeviceCompatibilityScreen and tell us
 * "Xiaomi killed the service 47 times in 3 days."
 *
 * Plain SharedPreferences — the counter + timestamp aren't PII and we want
 * zero startup cost on service onCreate.
 */
internal object FgsRestartTracker {

    private const val PREFS = "ot_fgs_restart_tracker"
    private const val KEY_RESTART_COUNT = "restart_count"
    private const val KEY_FIRST_START_AT = "first_start_at"
    private const val KEY_CURRENT_START_AT = "current_start_at"

    fun recordStart(context: Context) {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        if (!p.contains(KEY_FIRST_START_AT)) {
            p.edit().putLong(KEY_FIRST_START_AT, now).apply()
        }
        p.edit()
            .putInt(KEY_RESTART_COUNT, p.getInt(KEY_RESTART_COUNT, 0) + 1)
            .putLong(KEY_CURRENT_START_AT, now)
            .apply()
    }

    fun restartCount(context: Context): Int =
        prefs(context).getInt(KEY_RESTART_COUNT, 0)

    fun firstStartAtMs(context: Context): Long? {
        val v = prefs(context).getLong(KEY_FIRST_START_AT, 0L)
        return v.takeIf { it > 0L }
    }

    /** Milliseconds since this running instance started; null if never started. */
    fun currentUptimeMs(context: Context): Long? {
        val v = prefs(context).getLong(KEY_CURRENT_START_AT, 0L)
        return if (v > 0L) System.currentTimeMillis() - v else null
    }

    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
