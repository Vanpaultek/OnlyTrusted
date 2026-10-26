// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Safety fallback: lets the user pause the lock-watcher's auto-enforcement
 * for a bounded window when they urgently need the camera / mic.
 *
 * The pause state is stored on-device (never anywhere else) and auto-expires
 * after the chosen window. During the pause, enforceLock() is a no-op — the
 * mic stays unmuted and the camera stays enabled regardless of the screen
 * state. On expiry (checked at every screen-off event), normal enforcement
 * resumes without any further user action.
 *
 * Why a bounded pause instead of a toggle: a permanent "off" switch would
 * silently defeat the feature. A time-boxed pause is honest + self-correcting.
 */
object SafetyOverride {
    private const val PREFS = "ot_sensor_privacy_override"
    private const val KEY_PAUSE_UNTIL = "pause_until_epoch_ms"

    /** Common pause windows the UI offers as quick buttons. */
    enum class Window(val minutes: Int, val label: String) {
        FIVE_MINUTES(5, "5 min"),
        FIFTEEN_MINUTES(15, "15 min"),
        ONE_HOUR(60, "1 hour"),
    }

    /** True if auto-enforcement is currently paused by the user. */
    fun isActive(context: Context): Boolean = pauseUntil(context) > System.currentTimeMillis()

    /** How many minutes remain in the active pause, or 0 if not paused. */
    fun remainingMinutes(context: Context): Int {
        val remaining = pauseUntil(context) - System.currentTimeMillis()
        return if (remaining > 0) ((remaining + 59_999) / 60_000).toInt() else 0
    }

    /** Starts a pause for [window]. The lock-watcher skips enforcement until
     *  the window elapses. */
    fun pause(context: Context, window: Window) {
        val until = System.currentTimeMillis() + window.minutes * 60_000L
        prefs(context).edit().putLong(KEY_PAUSE_UNTIL, until).apply()
        SensorPrivacyLog.append(
            context,
            kind = "SAFETY_PAUSE_STARTED",
            detail = "auto-enforcement paused for ${window.label}.",
        )
    }

    /** Clears any active pause immediately — enforcement resumes on next lock. */
    fun resume(context: Context) {
        prefs(context).edit().remove(KEY_PAUSE_UNTIL).apply()
        SensorPrivacyLog.append(
            context,
            kind = "SAFETY_PAUSE_CLEARED",
            detail = "user resumed auto-enforcement before window expiry.",
        )
    }

    private fun pauseUntil(context: Context): Long =
        prefs(context).getLong(KEY_PAUSE_UNTIL, 0L)

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
