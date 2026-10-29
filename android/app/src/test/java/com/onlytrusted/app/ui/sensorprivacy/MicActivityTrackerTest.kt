// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic tests for the Session.looksLikeUserCall() classifier.
 * This is the heart of the silent-mic-activation defence — it decides
 * whether an active recording should pause the auto-lock.
 */
class MicActivityTrackerTest {

    private val baseStart = 1_000_000L

    @Test fun `recording that started while screen was on and ran 3s qualifies`() {
        val s = MicActivityTracker.Session("com.whatsapp", baseStart, startedWhileScreenOn = true)
        assertTrue(s.looksLikeUserCall(now = baseStart + 3_000L))
    }

    @Test fun `recording that started while screen was off never qualifies`() {
        val s = MicActivityTracker.Session("com.hacker", baseStart, startedWhileScreenOn = false)
        assertFalse(s.looksLikeUserCall(now = baseStart + 10_000L))
        assertFalse(s.looksLikeUserCall(now = baseStart + 3600_000L))
    }

    @Test fun `recording less than 3s old does not qualify yet`() {
        val s = MicActivityTracker.Session("com.whatsapp", baseStart, startedWhileScreenOn = true)
        assertFalse(s.looksLikeUserCall(now = baseStart + 2_999L))
        assertFalse(s.looksLikeUserCall(now = baseStart + 1_000L))
    }

    @Test fun `recording exactly at 3s threshold qualifies`() {
        val s = MicActivityTracker.Session("com.whatsapp", baseStart, startedWhileScreenOn = true)
        assertTrue(s.looksLikeUserCall(now = baseStart + 3_000L))
    }

    @Test fun `long-running recording started while screen on is clearly legit`() {
        val s = MicActivityTracker.Session("com.zoom", baseStart, startedWhileScreenOn = true)
        assertTrue(s.looksLikeUserCall(now = baseStart + 60_000L))
    }
}
