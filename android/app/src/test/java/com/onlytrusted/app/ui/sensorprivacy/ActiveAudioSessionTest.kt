// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the Snapshot classification logic that decides whether an
 * active audio session should defer the auto-lock. The honest-call vs
 * silent-VoIP-flip distinction lives here — if this test regresses, a
 * mic-abusing app can bypass the auto-lock.
 */
class ActiveAudioSessionTest {

    private fun snap(
        cell: Boolean = false, ring: Boolean = false,
        legitVoip: Boolean = false, suspiciousVoip: Boolean = false,
        media: Boolean = false,
    ) = ActiveAudioSession.Snapshot(
        onCellularCall = cell,
        ringing = ring,
        userAcceptedVoipCall = legitVoip,
        suspiciousVoipMode = suspiciousVoip,
        mediaPlaying = media,
    )

    @Test fun `idle snapshot does NOT defer lock`() {
        assertFalse(snap().shouldDeferLock)
    }

    @Test fun `cellular call defers lock`() {
        assertTrue(snap(cell = true).shouldDeferLock)
    }

    @Test fun `incoming ringing defers lock`() {
        assertTrue(snap(ring = true).shouldDeferLock)
    }

    @Test fun `user-accepted VoIP defers lock`() {
        assertTrue(snap(legitVoip = true).shouldDeferLock)
    }

    @Test fun `media playing defers lock`() {
        assertTrue(snap(media = true).shouldDeferLock)
    }

    @Test fun `SUSPICIOUS VoIP mode alone does NOT defer lock`() {
        // The whole point: an app flipping into VoIP mode without a real
        // call session must NOT be able to prevent the auto-lock.
        assertFalse(snap(suspiciousVoip = true).shouldDeferLock)
    }

    @Test fun `reason string is idle when nothing is happening`() {
        assertEquals("idle", snap().reason())
    }

    @Test fun `reason string calls out silent VoIP flip explicitly`() {
        val reason = snap(suspiciousVoip = true).reason()
        assertTrue(reason.contains("VoIP mode set WITHOUT user call"))
    }

    @Test fun `reason string chains multiple active signals`() {
        val reason = snap(cell = true, media = true).reason()
        assertTrue(reason.contains("cellular call"))
        assertTrue(reason.contains("media playing"))
    }
}
