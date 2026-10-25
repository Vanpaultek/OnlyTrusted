// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.deadmanwipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

/**
 * Pure-logic verification of the clock-skew defense formula. The real
 * DeadmanWipeState.shouldWipeNow() needs an Android Context + EncryptedSharedPreferences,
 * so we extract the policy here and verify it in isolation. The code shape here
 * MUST stay in sync with DeadmanWipeState.shouldWipeNow() — any divergence is a bug.
 */
class DeadmanWipeClockSkewTest {

    /** Mirror of DeadmanWipeState.shouldWipeNow() clock-skew logic. */
    private fun shouldWipe(
        now: Long,
        lastUnlockWall: Long,
        nowElapsed: Long,
        lastUnlockElapsed: Long,
        storedBoot: Long,
        intervalMs: Long
    ): Boolean {
        val wallElapsed = now - lastUnlockWall
        val currentBoot = now - nowElapsed
        val sameBoot = storedBoot > 0 && abs(currentBoot - storedBoot) < 60_000L
        val monoElapsed = if (sameBoot) (nowElapsed - lastUnlockElapsed) else 0L
        val effectiveElapsed = max(wallElapsed, monoElapsed)
        return effectiveElapsed >= intervalMs
    }

    @Test
    fun `fires when wall-clock crosses interval normally`() {
        val day = 86_400_000L
        val now = 100L * day
        val lastWall = 93L * day
        val fired = shouldWipe(
            now = now,
            lastUnlockWall = lastWall,
            nowElapsed = 10_000L,          // doesn't matter when wall is authoritative
            lastUnlockElapsed = 5_000L,
            storedBoot = now - 10_000L,
            intervalMs = 7L * day
        )
        assertTrue("wall-clock elapsed 7 days should fire 7-day timer", fired)
    }

    @Test
    fun `fires when user rewinds clock but monotonic marches on`() {
        val day = 86_400_000L
        val intervalMs = 7L * day
        val realNow = 100L * day
        // Attacker set the device clock back by 10 days to delay the wipe
        val attackedWall = realNow - 10L * day
        // Monotonic elapsed is honest — 8 days of uptime since last unlock
        val fired = shouldWipe(
            now = attackedWall,                       // the fake "now" the clock shows
            lastUnlockWall = attackedWall - day,      // wall-clock says only 1 day elapsed
            nowElapsed = 8L * day,                    // 8 days of real uptime
            lastUnlockElapsed = 0L,
            storedBoot = attackedWall - 8L * day,     // boot time stays consistent on same boot
            intervalMs = intervalMs
        )
        assertTrue("monotonic 8 days should fire 7-day timer even if wall clock rewound", fired)
    }

    @Test
    fun `does not fire within interval on normal wall clock`() {
        val day = 86_400_000L
        val now = 100L * day
        val fired = shouldWipe(
            now = now,
            lastUnlockWall = now - 3L * day,
            nowElapsed = 3L * day,
            lastUnlockElapsed = 0L,
            storedBoot = now - 3L * day,
            intervalMs = 7L * day
        )
        assertFalse("3 days of elapsed time should not fire a 7-day timer", fired)
    }

    @Test
    fun `after reboot falls back to wall clock only`() {
        val day = 86_400_000L
        val now = 100L * day
        // Last unlock was 10 days ago wall-clock, but device rebooted since
        // (so storedBoot is far off from currentBoot)
        val fired = shouldWipe(
            now = now,
            lastUnlockWall = now - 10L * day,
            nowElapsed = 1_000L,                      // just booted, 1s uptime
            lastUnlockElapsed = 5L * day,             // irrelevant now
            storedBoot = now - 15L * day,             // old boot time, way out of 60s tolerance
            intervalMs = 7L * day
        )
        assertTrue("after reboot, wall clock 10 days should still fire 7-day timer", fired)
    }

    @Test
    fun `boot time within 60 second tolerance is considered same boot`() {
        val day = 86_400_000L
        val now = 100L * day
        val fired = shouldWipe(
            now = now,
            lastUnlockWall = now - 3L * day,
            nowElapsed = 8L * day,                    // monotonic says 8 days
            lastUnlockElapsed = 0L,
            // Boot time stored vs. computed differs by only 30 seconds — same boot
            storedBoot = (now - 8L * day) + 30_000L,
            intervalMs = 7L * day
        )
        assertTrue("tolerance window should allow monotonic to win", fired)
    }

    @Test
    fun `missing storedBoot means wall-clock only mode`() {
        val day = 86_400_000L
        val now = 100L * day
        val fired = shouldWipe(
            now = now,
            lastUnlockWall = now - 10L * day,
            nowElapsed = 1_000L,
            lastUnlockElapsed = 0L,
            storedBoot = 0L,                          // never stored — pre-patch install
            intervalMs = 7L * day
        )
        assertTrue("pre-patch installs still fire via wall clock", fired)
    }
}
