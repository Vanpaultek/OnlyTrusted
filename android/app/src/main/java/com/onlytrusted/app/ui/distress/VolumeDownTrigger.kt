// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.distress

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent

/**
 * Volume-down 4× distress trigger (Android-only — iOS sandboxes volume keys outside
 * the app's own frame and third-party apps cannot intercept them).
 *
 * Pattern: 4 KEYCODE_VOLUME_DOWN presses within [windowMs] ms fires [DistressTriggerBus].
 * Called from [com.onlytrusted.app.MainActivity.onKeyDown] which receives raw volume key
 * events as long as OnlyTrusted holds keyboard focus (i.e. app is foreground + not
 * covered by a system IME / notification shade).
 *
 * Scope: foreground-only — the Activity.onKeyDown hook only fires while the app is
 * in RESUMED state. Backgrounded apps don't see volume keys; running an Accessibility
 * Service to grab them globally would be a huge trust ask + a Play-Store red flag,
 * so we keep this feature narrowly scoped. Users in distress most commonly already
 * have OnlyTrusted open (or can be trained to open it + press-press-press-press).
 *
 * Returns true from [consume] when the pattern matched and the trigger fired, so
 * the Activity can swallow the event (`return true` from onKeyDown) and skip the
 * final volume adjustment. False means the event should bubble normally.
 */
class VolumeDownTrigger(
    private val requiredPresses: Int = 4,
    private val windowMs: Long = 2_000L
) {

    private val timestamps = ArrayDeque<Long>()

    /** Call from Activity.onKeyDown. Returns true when the 4-in-2s pattern just fired. */
    fun consume(keyCode: Int): Boolean {
        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        val now = System.currentTimeMillis()
        timestamps.addLast(now)
        while (timestamps.isNotEmpty() && (now - timestamps.first()) > windowMs) {
            timestamps.removeFirst()
        }
        if (timestamps.size >= requiredPresses) {
            timestamps.clear()
            DistressTriggerBus.fire()
            return true
        }
        return false
    }

    /** Per-user toggle persisted in plain SharedPreferences (not sensitive). */
    object Prefs {
        private const val PREFS = "distress_volume_prefs"
        private const val KEY_ENABLED = "volume_down_trigger_enabled"

        private fun prefs(ctx: Context): SharedPreferences =
            ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /**
         * Default OFF — this trigger swallows a volume-down press (returns true from
         * onKeyDown when the 4th press lands) which some users will find surprising.
         * Opt-in via Settings → Distress Signal keeps the volume rocker fully normal
         * for anyone who doesn't want this behaviour.
         */
        fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, false)

        fun setEnabled(ctx: Context, enabled: Boolean) {
            prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply()
        }
    }
}
