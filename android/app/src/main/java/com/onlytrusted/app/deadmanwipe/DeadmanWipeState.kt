// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.deadmanwipe

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.Date
import kotlin.math.abs
import kotlin.math.max

/**
 * Dead-Man Wipe (Elite). Mirrors iOS `DeadmanWipeState`.
 *
 * If the user doesn't unlock the app for `intervalDays`, the entire app state is wiped by
 * [DeadmanWipeWorker]. The countdown is reset on every real unlock through
 * [markUnlockNow], which [com.onlytrusted.app.ui.lock.AppLockGate] calls after biometric
 * success.
 *
 * Backing store is `EncryptedSharedPreferences` (AES-256-GCM), reusing the pattern from
 * HP1 Duress / HP3 Watchdog. Key: `deadman_wipe_prefs_v1`.
 */
object DeadmanWipeState {

    /** Valid intervals (days). 60 is the max per the brief. */
    val allowedIntervals: List<Int> = listOf(7, 14, 30, 60)

    private const val PREFS = "deadman_wipe_prefs_v1"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL = "interval_days"
    private const val KEY_LAST_UNLOCK = "last_unlock_millis"
    // Clock-skew defense (added 2026-10-25). Store SystemClock.elapsedRealtime at
    // the moment of unlock + the estimated boot time (wall clock minus elapsed
    // realtime). shouldWipeNow() uses max(wallClockElapsed, monotonicElapsed) so
    // a user setting the clock backward can't buy themselves extra time.
    private const val KEY_LAST_UNLOCK_ELAPSED = "last_unlock_elapsed_realtime"
    private const val KEY_LAST_UNLOCK_BOOT_MS = "last_unlock_boot_time_millis"

    private const val DEFAULT_INTERVAL = 30

    private fun prefs(ctx: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            ctx.applicationContext,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, false)

    fun intervalDays(ctx: Context): Int {
        val stored = prefs(ctx).getInt(KEY_INTERVAL, DEFAULT_INTERVAL)
        return if (allowedIntervals.contains(stored)) stored else DEFAULT_INTERVAL
    }

    fun lastUnlockMillis(ctx: Context): Long =
        prefs(ctx).getLong(KEY_LAST_UNLOCK, System.currentTimeMillis())

    fun lastUnlock(ctx: Context): Date = Date(lastUnlockMillis(ctx))

    /**
     * Compute the deadline. `null` is never returned — if nothing is set we default to
     * "deadline is intervalDays from now", which keeps callers simpler.
     */
    fun wipeDeadlineMillis(ctx: Context): Long {
        val last = lastUnlockMillis(ctx)
        val days = intervalDays(ctx)
        return last + days * 86_400_000L
    }

    fun wipeDeadline(ctx: Context): Date = Date(wipeDeadlineMillis(ctx))

    fun shouldWipeNow(
        ctx: Context,
        now: Long = System.currentTimeMillis(),
        nowElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        if (!isEnabled(ctx)) return false
        val p = prefs(ctx)
        val lastWall = lastUnlockMillis(ctx)
        val intervalMs = intervalDays(ctx) * 86_400_000L

        val wallElapsed = now - lastWall
        val storedElapsed = p.getLong(KEY_LAST_UNLOCK_ELAPSED, 0L)
        val storedBoot = p.getLong(KEY_LAST_UNLOCK_BOOT_MS, 0L)

        // Detect reboot: if the current estimated boot time differs from the
        // stored one by more than 60 s, elapsedRealtime has restarted and the
        // monotonic reference is stale. Fall back to wall clock only.
        val currentBoot = now - nowElapsedRealtime
        val sameBoot = storedBoot > 0 && abs(currentBoot - storedBoot) < 60_000L
        val monoElapsed = if (sameBoot) (nowElapsedRealtime - storedElapsed) else 0L

        // Clock-skew defense: take the larger of the two measurements.
        val effectiveElapsed = max(wallElapsed, monoElapsed)
        return effectiveElapsed >= intervalMs
    }

    fun setIntervalDays(ctx: Context, days: Int) {
        val safe = if (allowedIntervals.contains(days)) days else DEFAULT_INTERVAL
        prefs(ctx).edit().putInt(KEY_INTERVAL, safe).apply()
    }

    /** Called by AppLockGate after a successful biometric unlock. Also by "Reset timer". */
    fun markUnlockNow(ctx: Context) {
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        val bootTime = now - elapsed
        prefs(ctx).edit()
            .putLong(KEY_LAST_UNLOCK, now)
            .putLong(KEY_LAST_UNLOCK_ELAPSED, elapsed)
            .putLong(KEY_LAST_UNLOCK_BOOT_MS, bootTime)
            .apply()
        if (isEnabled(ctx)) {
            DeadmanWipeScheduler.rescheduleAll(ctx)
        }
    }

    /** Enable with the current (or newly-passed) interval; resets the timer. */
    fun enable(ctx: Context, intervalDays: Int) {
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        prefs(ctx).edit()
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_INTERVAL, if (allowedIntervals.contains(intervalDays)) intervalDays else DEFAULT_INTERVAL)
            .putLong(KEY_LAST_UNLOCK, now)
            .putLong(KEY_LAST_UNLOCK_ELAPSED, elapsed)
            .putLong(KEY_LAST_UNLOCK_BOOT_MS, now - elapsed)
            .apply()
        DeadmanWipeScheduler.rescheduleAll(ctx)
    }

    fun disable(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, false).apply()
        DeadmanWipeScheduler.cancelAll(ctx)
    }

    /** Clears every pref under this feature. Used by the wipe path itself. */
    fun clearAll(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }
}
