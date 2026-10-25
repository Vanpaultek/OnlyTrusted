// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Rate limits PIN unlock attempts to defeat trivial brute-force.
 *
 * The vault is protected by SQLCipher + Android Keystore (hardware-backed AES-256
 * via `MasterKey.AES256_GCM`), which means a locked device won't release the key.
 * But once past the OS lock, the OnlyTrusted PIN sheet was previously unlimited —
 * an attacker could hammer PIN guesses with no slowdown.
 *
 * Policy (kept in sync with iOS `PINRateLimiter.swift`):
 *   - 5 consecutive failures → 60 s lockout (first offense)
 *   - 10 consecutive failures → 5 min lockout
 *   - 15 consecutive failures → 15 min lockout
 *   - 20+ consecutive failures → 60 min lockout, repeats
 *   - Any successful unlock resets the counter to 0
 *
 * Storage: EncryptedSharedPreferences so a force-stop + relaunch can't reset
 * the counter.
 */
object PINRateLimiter {

    private const val PREFS_NAME = "only_trusted_prefs"
    private const val KEY_FAILURES = "pin_rate_failures_v1"
    private const val KEY_LOCKED_UNTIL = "pin_rate_locked_until_v1"

    /** Returns the remaining lockout window in seconds, or 0 if the user may try now. */
    fun remainingLockoutSeconds(ctx: Context, nowMs: Long = System.currentTimeMillis()): Long {
        val lockedUntil = prefs(ctx).getLong(KEY_LOCKED_UNTIL, 0L)
        if (lockedUntil <= 0L) return 0L
        val remainingMs = lockedUntil - nowMs
        return if (remainingMs > 0) (remainingMs / 1000L) else 0L
    }

    /** Record a wrong PIN. Returns the lockout penalty in seconds (0 if under threshold). */
    fun recordFailure(ctx: Context, nowMs: Long = System.currentTimeMillis()): Long {
        val prefs = prefs(ctx)
        val failures = prefs.getInt(KEY_FAILURES, 0) + 1
        val penaltySeconds = penaltySecondsFor(failures)
        prefs.edit().apply {
            putInt(KEY_FAILURES, failures)
            if (penaltySeconds > 0L) {
                putLong(KEY_LOCKED_UNTIL, nowMs + penaltySeconds * 1000L)
            }
            apply()
        }
        return penaltySeconds
    }

    /** Clear the counter + lockout after a successful PIN entry. */
    fun recordSuccess(ctx: Context) {
        prefs(ctx).edit().remove(KEY_FAILURES).remove(KEY_LOCKED_UNTIL).apply()
    }

    /** Visible for tests. Lockout duration triggered by the Nth consecutive failure. */
    fun penaltySecondsFor(failures: Int): Long = when {
        failures < 5 -> 0L
        failures < 10 -> 60L
        failures < 15 -> 5L * 60L
        failures < 20 -> 15L * 60L
        else -> 60L * 60L
    }

    /** Visible for tests. Wipes everything. */
    fun reset(ctx: Context) {
        prefs(ctx).edit().remove(KEY_FAILURES).remove(KEY_LOCKED_UNTIL).apply()
    }

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx,
        PREFS_NAME,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
