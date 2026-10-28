// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Opt-in gate + live-instance holder for [AudioRecordingWatcher]. Keeps a
 * static reference to the active watcher so UI toggles + the foreground
 * service share the same lifecycle, avoiding duplicate callback registration.
 *
 * The enabled flag lives in EncryptedSharedPreferences so a forensic dump of
 * the device backup cannot read whether the user opted into mic monitoring.
 * On the rare keystore corruption path we fall back to a per-process in-memory
 * default of `false` — the user re-opts in on the next launch.
 */
object AudioRecordingWatcherState {
    private const val PREFS = "ot_mic_watcher_prefs"
    private const val KEY_ENABLED = "enabled"

    @Volatile private var active: AudioRecordingWatcher? = null

    fun isEnabled(context: Context): Boolean =
        prefs(context)?.getBoolean(KEY_ENABLED, false) ?: false

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context)?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
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

    private fun prefs(context: Context): SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Throwable) {
        // Keystore unavailable (very rare — corrupted key material). Fall
        // through to disabled; user will re-opt-in on next launch.
        null
    }
}
