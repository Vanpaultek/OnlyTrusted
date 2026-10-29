// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * EncryptedSharedPreferences-backed profile store. Mirrors iOS `ProfileStore`.
 *
 *   * email            — stored on-device only; prefills Breach Watch, personalises copy.
 *   * biometricEnabled — opt-in flag for BiometricPrompter fast-path.
 *
 * No field ever leaves the device. The honest claim stays intact.
 *
 * Note: the general-app PIN originally stored here was removed in v0.1.10.
 * The only live PIN in the app is the Duress Mode PIN (see DuressState) — a
 * separate purpose-built secret, not an app-wide lock credential. App Lock
 * now relies on biometric via AppLockManager + BiometricPrompter.
 */
object ProfileStore {

    private const val PREFS = "profile_prefs_v1"
    private const val KEY_EMAIL = "email"
    private const val KEY_BIOMETRIC = "biometric_enabled"
    private const val KEY_COMPLETED_AT = "setup_completed_at"

    @Volatile private var cachedMasterKey: MasterKey? = null

    private fun prefs(ctx: Context): SharedPreferences {
        val master = cachedMasterKey ?: synchronized(this) {
            cachedMasterKey ?: MasterKey.Builder(ctx.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                .also { cachedMasterKey = it }
        }
        return EncryptedSharedPreferences.create(
            ctx.applicationContext, PREFS, master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // --- Getters -----------------------------------------------------------------

    fun email(ctx: Context): String = prefs(ctx).getString(KEY_EMAIL, "") ?: ""
    fun biometricEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_BIOMETRIC, false)
    fun isSetupComplete(ctx: Context): Boolean = prefs(ctx).contains(KEY_COMPLETED_AT)

    // --- Setters -----------------------------------------------------------------

    fun setEmail(ctx: Context, raw: String) {
        prefs(ctx).edit().putString(KEY_EMAIL, raw.trim().lowercase()).apply()
    }

    fun setBiometricEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun markSetupComplete(ctx: Context) {
        prefs(ctx).edit().putLong(KEY_COMPLETED_AT, System.currentTimeMillis()).apply()
    }

    fun clearAll(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }
}
