// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import android.os.Build

/**
 * Opt-in gate + live-instance holder for [AudioRecordingWatcher]. Keeps a
 * static reference to the active watcher so UI toggles + the foreground
 * service share the same lifecycle, avoiding duplicate callback registration.
 */
object AudioRecordingWatcherState {
    private const val PREFS = "ot_mic_watcher_prefs"
    private const val KEY_ENABLED = "enabled"

    @Volatile private var active: AudioRecordingWatcher? = null

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) startIfSupported(context) else stop()
    }

    /** Called by [SensorPrivacyForegroundService.onCreate] so the watcher
     *  comes back automatically on service restart. */
    fun startIfEnabled(context: Context) {
        if (isEnabled(context)) startIfSupported(context)
    }

    fun stop() {
        active?.stop()
        active = null
    }

    private fun startIfSupported(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        if (active != null) return
        val watcher = AudioRecordingWatcher(context.applicationContext)
        watcher.start()
        active = watcher
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
