// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Minimal append-only log for sensor-privacy events. Backed by
 * EncryptedSharedPreferences so entries survive process death but stay
 * on-device. See docs/SENSOR_PRIVACY_ARCHITECTURE.md for the event schema.
 *
 * Capped at the latest 500 entries to bound storage.
 */
object SensorPrivacyLog {
    private const val PREFS = "ot_sensor_privacy_log"
    private const val KEY = "events_json"
    private const val MAX = 500

    object Kind {
        const val SCREEN_LOCK = "SCREEN_LOCK"
        const val SCREEN_UNLOCK = "SCREEN_UNLOCK"
        const val SENSOR_PRIVACY_ENFORCED = "SENSOR_PRIVACY_ENFORCED"
        const val SENSOR_PRIVACY_RESTORED = "SENSOR_PRIVACY_RESTORED"
        const val SENSOR_PRIVACY_REFUSED = "SENSOR_PRIVACY_REFUSED"
    }

    data class Entry(val timestamp: Long, val kind: String, val detail: String?)

    fun append(context: Context, kind: String, detail: String? = null) {
        val sp = prefs(context)
        val raw = sp.getString(KEY, "[]")
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        val obj = JSONObject()
            .put("t", System.currentTimeMillis())
            .put("k", kind)
            .put("d", detail ?: JSONObject.NULL)
        arr.put(obj)
        // Trim to MAX most recent.
        while (arr.length() > MAX) arr.remove(0)
        sp.edit().putString(KEY, arr.toString()).apply()
    }

    fun recent(context: Context, limit: Int = 50): List<Entry> {
        val raw = prefs(context).getString(KEY, "[]")
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        val out = ArrayList<Entry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                Entry(
                    timestamp = o.optLong("t"),
                    kind = o.optString("k"),
                    detail = o.optString("d", null) ?: null,
                )
            )
        }
        return out.reversed().take(limit)
    }

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
