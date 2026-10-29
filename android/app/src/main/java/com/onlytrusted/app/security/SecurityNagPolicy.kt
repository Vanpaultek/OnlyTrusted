// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * State + policy for the "please enable App Lock" nag that fires on the
 * Dashboard when the user hasn't turned on [AppLockManager].
 *
 * ### Approach D — hybrid soft→hard
 *
 *   * Visits 1–2       → silent (new user; let them learn the app first).
 *   * Visits 3–5       → inline Dashboard card (soft nudge, easy to dismiss).
 *   * Visit 6 onwards  → one-time modal (interrupts flow briefly), then
 *                        falls back to the card again if the user picks
 *                        "Later" / "Not now".
 *
 * ### Dismissal policy — honest, no dark patterns
 *
 *   * "Not now" / "Later"            → snoozes for [SNOOZE_DAYS] days
 *   * After [MAX_SNOOZES] snoozes    → auto-stops permanently
 *   * Explicit "Never ask again"     → opt-out flag flipped to true
 *   * Settings toggle "Suggest security setup" (default on) is the master kill switch
 *
 * All state lives in EncryptedSharedPreferences so a forensic dump of the
 * device backup can't reveal the user's security-setup state.
 */
object SecurityNagPolicy {

    private const val PREFS = "ot_security_nag_prefs"
    private const val KEY_VISITS = "dashboard_visits"
    private const val KEY_SNOOZED_UNTIL = "snoozed_until"
    private const val KEY_SNOOZE_COUNT = "snooze_count"
    private const val KEY_OPTED_OUT = "opted_out"
    private const val KEY_MODAL_SHOWN = "modal_shown"
    private const val KEY_SUGGEST_ENABLED = "suggest_enabled"

    /** Visits before the inline card first appears. */
    const val CARD_VISIT_THRESHOLD = 3
    /** Visits before the one-time modal fires. */
    const val MODAL_VISIT_THRESHOLD = 6
    private const val SNOOZE_DAYS = 7L
    private const val SNOOZE_MS = SNOOZE_DAYS * 24L * 60L * 60L * 1000L
    private const val MAX_SNOOZES = 3

    enum class Prompt { NONE, CARD, MODAL }

    /** Snapshot of all inputs to [decide]. Lets the pure decision function
     *  be tested without a real Android Context. */
    data class State(
        val appLockEnabled: Boolean,
        val suggestionsEnabled: Boolean,
        val optedOut: Boolean,
        val visitsSoFar: Int,
        val snoozeCount: Int,
        val snoozedUntilMs: Long,
        val modalAlreadyShown: Boolean,
        val nowMs: Long,
    )

    /**
     * Pure, stateless policy. Returns the prompt to show given the current
     * state. Call site is responsible for incrementing [State.visitsSoFar]
     * BEFORE calling this and for persisting any side-effects (e.g.,
     * marking the modal shown) AFTER.
     *
     * Extracted from [registerDashboardVisit] so the policy itself is
     * trivially testable with plain JUnit — no Context, no Robolectric.
     */
    fun decide(state: State): Prompt {
        if (state.appLockEnabled) return Prompt.NONE
        if (state.optedOut) return Prompt.NONE
        if (!state.suggestionsEnabled) return Prompt.NONE
        if (state.snoozeCount >= MAX_SNOOZES) return Prompt.NONE
        if (state.nowMs < state.snoozedUntilMs) return Prompt.NONE
        return when {
            state.visitsSoFar >= MODAL_VISIT_THRESHOLD && !state.modalAlreadyShown -> Prompt.MODAL
            state.visitsSoFar >= CARD_VISIT_THRESHOLD -> Prompt.CARD
            else -> Prompt.NONE
        }
    }

    /**
     * Called from DashboardScreen's LaunchedEffect on each entry.
     * Increments the visit counter and returns the prompt to show (if any).
     *
     * [appLockEnabled] is passed in so this file never needs to depend on
     * [AppLockManager] directly — keeps the policy pure and testable.
     */
    fun registerDashboardVisit(context: Context, appLockEnabled: Boolean): Prompt {
        val p = prefs(context) ?: return Prompt.NONE
        // Increment counter FIRST so [State.visitsSoFar] already includes
        // this visit when passed to [decide].
        val visits = p.getInt(KEY_VISITS, 0) + 1
        p.edit().putInt(KEY_VISITS, visits).apply()

        val state = State(
            appLockEnabled = appLockEnabled,
            suggestionsEnabled = p.getBoolean(KEY_SUGGEST_ENABLED, true),
            optedOut = p.getBoolean(KEY_OPTED_OUT, false),
            visitsSoFar = visits,
            snoozeCount = p.getInt(KEY_SNOOZE_COUNT, 0),
            snoozedUntilMs = p.getLong(KEY_SNOOZED_UNTIL, 0L),
            modalAlreadyShown = p.getBoolean(KEY_MODAL_SHOWN, false),
            nowMs = System.currentTimeMillis(),
        )
        val decision = decide(state)
        if (decision == Prompt.MODAL) {
            p.edit().putBoolean(KEY_MODAL_SHOWN, true).apply()
        }
        return decision
    }

    /** User tapped "Not now" / "Later" / swiped the card. */
    fun snooze(context: Context) {
        val p = prefs(context) ?: return
        val count = p.getInt(KEY_SNOOZE_COUNT, 0) + 1
        p.edit()
            .putLong(KEY_SNOOZED_UNTIL, System.currentTimeMillis() + SNOOZE_MS)
            .putInt(KEY_SNOOZE_COUNT, count)
            .apply()
    }

    /** User chose "Never ask again" from the modal. */
    fun optOut(context: Context) {
        prefs(context)?.edit()?.putBoolean(KEY_OPTED_OUT, true)?.apply()
    }

    /** User tapped the CTA and successfully enabled App Lock — stop nagging. */
    fun markCompleted(context: Context) {
        // Clearing counters means a future accidental AppLock disable will
        // start the sequence over from scratch, which is what we want.
        prefs(context)?.edit()
            ?.remove(KEY_VISITS)
            ?.remove(KEY_SNOOZED_UNTIL)
            ?.remove(KEY_SNOOZE_COUNT)
            ?.remove(KEY_MODAL_SHOWN)
            ?.apply()
    }

    // --- Settings toggle (master kill switch) --------------------------------

    fun suggestionsEnabled(context: Context): Boolean =
        prefs(context)?.getBoolean(KEY_SUGGEST_ENABLED, true) ?: true

    fun setSuggestionsEnabled(context: Context, enabled: Boolean) {
        prefs(context)?.edit()?.putBoolean(KEY_SUGGEST_ENABLED, enabled)?.apply()
    }

    // --- Internal ------------------------------------------------------------

    @Volatile private var loggedKeystoreFailureOnce: Boolean = false

    private fun prefs(context: Context): SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (t: Throwable) {
        // Keystore unavailable → nag never fires rather than crashing the
        // Dashboard. Log the first occurrence per-process so OEM-specific
        // Keystore quirks (notably seen on some Xiaomi + Honor builds) are
        // visible to testers in the Device Compatibility screen's logcat
        // capture, instead of silently losing nag state.
        if (!loggedKeystoreFailureOnce) {
            loggedKeystoreFailureOnce = true
            android.util.Log.w(
                "OTSecurityNag",
                "EncryptedSharedPreferences init failed; nag disabled this session",
                t,
            )
        }
        null
    }
}
