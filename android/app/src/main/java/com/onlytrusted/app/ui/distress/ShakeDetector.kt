// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.distress

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Shake-to-trigger for the Distress Signal.
 *
 * Pattern: detects at least [requiredShakes] discrete shakes within [windowMs] ms.
 *   • A "shake" fires when total acceleration minus gravity exceeds [thresholdGforce].
 *   • Successive shakes must be at least [cooldownMs] ms apart so a single violent
 *     motion isn't counted as several shakes (prevents false positives from drops).
 *   • If the window elapses without reaching [requiredShakes], the counter resets.
 *
 * Scope: foreground-only. Android's SensorManager only delivers accelerometer events
 * while the owning Activity is in RESUMED state (or inside a foreground service).
 * Running a dedicated foreground service just for shake detection would burn battery
 * all day for a feature that only matters in a user-held phone — so Phase 2 ships
 * foreground-only. If demand warrants, Phase 3 can add a background shake service
 * gated behind a Pro-tier opt-in with a persistent "listening for shake" notification.
 *
 * Honest-claim note: accelerometer data NEVER leaves the device, is NEVER logged, and
 * is only sampled while the user has OnlyTrusted open on screen. We don't retain any
 * motion history past the rolling 3-second window.
 */
class ShakeDetector(
    private val onShakePatternDetected: () -> Unit,
    private val requiredShakes: Int = 5,
    private val windowMs: Long = 3_000L,
    private val cooldownMs: Long = 150L,
    private val thresholdGforce: Float = 2.5f
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private val shakeTimestamps = ArrayDeque<Long>()
    private var lastShakeAt: Long = 0L

    /** Start listening. Returns true if the device actually has an accelerometer. */
    fun start(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return false
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
        sensorManager = sm
        accelerometer = sensor
        return sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    /** Stop listening. Idempotent. */
    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        accelerometer = null
        shakeTimestamps.clear()
        lastShakeAt = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        // Total acceleration magnitude includes gravity (~9.81 m/s²). Subtract to
        // get "shake" force. Divide by g to express in units the user intuits.
        val gforce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH - 1f
        val now = System.currentTimeMillis()
        if (gforce < thresholdGforce) return
        if ((now - lastShakeAt) < cooldownMs) return
        lastShakeAt = now
        shakeTimestamps.addLast(now)
        // Drop timestamps that have fallen outside the rolling window.
        while (shakeTimestamps.isNotEmpty() && (now - shakeTimestamps.first()) > windowMs) {
            shakeTimestamps.removeFirst()
        }
        if (shakeTimestamps.size >= requiredShakes) {
            shakeTimestamps.clear()
            lastShakeAt = 0L
            onShakePatternDetected()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }

    /** Per-user toggle persisted in plain SharedPreferences (not sensitive). */
    object Prefs {
        private const val PREFS = "distress_shake_prefs"
        private const val KEY_ENABLED = "shake_trigger_enabled"

        private fun prefs(ctx: Context): SharedPreferences =
            ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /** Default ON — foreground-only, zero battery impact when the app is backgrounded. */
        fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, true)

        fun setEnabled(ctx: Context, enabled: Boolean) {
            prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply()
        }
    }
}
