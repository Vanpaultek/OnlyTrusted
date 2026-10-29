// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.security

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-logic tests for the SecurityNagPolicy decision function. No Android
 * Context, no Robolectric — the policy state is passed in as [SecurityNagPolicy.State].
 *
 * Covers the dismissal / snooze / opt-out matrix so a regression in the
 * nag policy surfaces here first instead of annoying real users.
 */
class SecurityNagPolicyTest {

    private val baseline = SecurityNagPolicy.State(
        appLockEnabled = false,
        suggestionsEnabled = true,
        optedOut = false,
        visitsSoFar = 0,
        snoozeCount = 0,
        snoozedUntilMs = 0L,
        modalAlreadyShown = false,
        nowMs = 1_000_000L,
    )

    // --- Silent cases --------------------------------------------------------

    @Test fun `visits 1 and 2 return NONE (silent)`() {
        assertEquals(SecurityNagPolicy.Prompt.NONE, SecurityNagPolicy.decide(baseline.copy(visitsSoFar = 1)))
        assertEquals(SecurityNagPolicy.Prompt.NONE, SecurityNagPolicy.decide(baseline.copy(visitsSoFar = 2)))
    }

    @Test fun `app lock already enabled returns NONE regardless of visit count`() {
        val state = baseline.copy(visitsSoFar = 100, appLockEnabled = true)
        assertEquals(SecurityNagPolicy.Prompt.NONE, SecurityNagPolicy.decide(state))
    }

    @Test fun `opted out returns NONE regardless of visits`() {
        val state = baseline.copy(visitsSoFar = 100, optedOut = true)
        assertEquals(SecurityNagPolicy.Prompt.NONE, SecurityNagPolicy.decide(state))
    }

    @Test fun `suggestions disabled by Settings kill switch returns NONE`() {
        val state = baseline.copy(visitsSoFar = 100, suggestionsEnabled = false)
        assertEquals(SecurityNagPolicy.Prompt.NONE, SecurityNagPolicy.decide(state))
    }

    @Test fun `three snoozes auto-stops forever`() {
        val state = baseline.copy(visitsSoFar = 100, snoozeCount = 3)
        assertEquals(SecurityNagPolicy.Prompt.NONE, SecurityNagPolicy.decide(state))
    }

    @Test fun `inside snooze window returns NONE`() {
        val state = baseline.copy(
            visitsSoFar = 5,
            snoozedUntilMs = 2_000_000L, // future
            nowMs = 1_500_000L,          // before snooze expiry
        )
        assertEquals(SecurityNagPolicy.Prompt.NONE, SecurityNagPolicy.decide(state))
    }

    // --- CARD cases ----------------------------------------------------------

    @Test fun `visit 3 shows CARD`() {
        assertEquals(SecurityNagPolicy.Prompt.CARD, SecurityNagPolicy.decide(baseline.copy(visitsSoFar = 3)))
    }

    @Test fun `visits 4 and 5 keep showing CARD`() {
        assertEquals(SecurityNagPolicy.Prompt.CARD, SecurityNagPolicy.decide(baseline.copy(visitsSoFar = 4)))
        assertEquals(SecurityNagPolicy.Prompt.CARD, SecurityNagPolicy.decide(baseline.copy(visitsSoFar = 5)))
    }

    @Test fun `card still shows after snooze expires`() {
        val state = baseline.copy(
            visitsSoFar = 4,
            snoozedUntilMs = 1_000L, // past
            nowMs = 2_000_000L,
        )
        assertEquals(SecurityNagPolicy.Prompt.CARD, SecurityNagPolicy.decide(state))
    }

    // --- MODAL cases ---------------------------------------------------------

    @Test fun `visit 6 shows MODAL on first time`() {
        val state = baseline.copy(visitsSoFar = 6, modalAlreadyShown = false)
        assertEquals(SecurityNagPolicy.Prompt.MODAL, SecurityNagPolicy.decide(state))
    }

    @Test fun `visit 6+ falls back to CARD after modal shown once`() {
        val state = baseline.copy(visitsSoFar = 10, modalAlreadyShown = true)
        assertEquals(SecurityNagPolicy.Prompt.CARD, SecurityNagPolicy.decide(state))
    }

    @Test fun `modal does NOT fire a second time`() {
        val afterModal = baseline.copy(visitsSoFar = 7, modalAlreadyShown = true)
        assertEquals(SecurityNagPolicy.Prompt.CARD, SecurityNagPolicy.decide(afterModal))
    }

    // --- Priority of guards --------------------------------------------------

    @Test fun `app lock enabled trumps everything including qualifying visit count`() {
        val state = baseline.copy(
            visitsSoFar = 20,
            appLockEnabled = true,
            modalAlreadyShown = false,
        )
        assertEquals(SecurityNagPolicy.Prompt.NONE, SecurityNagPolicy.decide(state))
    }

    @Test fun `opt-out trumps modal threshold`() {
        val state = baseline.copy(visitsSoFar = 6, optedOut = true)
        assertEquals(SecurityNagPolicy.Prompt.NONE, SecurityNagPolicy.decide(state))
    }
}
