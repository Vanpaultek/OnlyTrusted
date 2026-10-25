// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.security

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-logic regression tests for the PIN lockout curve. The context-backed
 * recordFailure / recordSuccess paths need Android instrumentation to exercise
 * EncryptedSharedPreferences, so those live under androidTest/. These tests
 * cover the deterministic policy function that doesn't need a Context.
 */
class PINRateLimiterTest {

    @Test
    fun `no lockout before 5 failures`() {
        for (i in 1..4) {
            assertEquals(0L, PINRateLimiter.penaltySecondsFor(i))
        }
    }

    @Test
    fun `first lockout at 5 failures is 60 seconds`() {
        assertEquals(60L, PINRateLimiter.penaltySecondsFor(5))
    }

    @Test
    fun `5 to 9 failures all map to 60 second lockout`() {
        for (i in 5..9) {
            assertEquals(60L, PINRateLimiter.penaltySecondsFor(i))
        }
    }

    @Test
    fun `10 to 14 failures escalate to 5 minutes`() {
        for (i in 10..14) {
            assertEquals(300L, PINRateLimiter.penaltySecondsFor(i))
        }
    }

    @Test
    fun `15 to 19 failures escalate to 15 minutes`() {
        for (i in 15..19) {
            assertEquals(900L, PINRateLimiter.penaltySecondsFor(i))
        }
    }

    @Test
    fun `20 and above all map to 60 minutes`() {
        for (i in listOf(20, 25, 50, 100, 1000)) {
            assertEquals(3600L, PINRateLimiter.penaltySecondsFor(i))
        }
    }

    @Test
    fun `curve is monotonic non-decreasing`() {
        var previous = 0L
        for (i in 1..30) {
            val current = PINRateLimiter.penaltySecondsFor(i)
            assertEquals(
                "penalty should never shrink: at $i it is $current, was $previous",
                true,
                current >= previous
            )
            previous = current
        }
    }
}
