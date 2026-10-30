// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import com.onlytrusted.app.OnlyTrustedApp
import com.onlytrusted.app.data.db.entities.SecurityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Low-battery pocket detection: proximity listens continuously (very cheap), accelerometer is
 * only subscribed when proximity reports "near". That avoids the always-on gravity sampling
 * that the earlier implementation incurred.
 */
class PocketDetectionService : Service(), SensorEventListener {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val sensorManager by lazy { getSystemService(SensorManager::class.java) }
    private var proximity: Sensor? = null
    private var accelerometer: Sensor? = null

    private var isNear = false
    private var isFaceDown = false
    private var accelerometerActive = false

    @Volatile private var pocketState = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // No longer a foreground service — removes the duplicate status-bar
        // icon that confused users ("why do I see two shields?"). The
        // process stays alive because [SensorMonitorService] is an FGS in
        // the same process, which holds the whole PID in memory. Pocket
        // detection uses TYPE_PROXIMITY / TYPE_ACCELEROMETER which have no
        // background-execution restrictions on any supported API level.
        val sm = sensorManager ?: return
        proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximity?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
        scope.cancel()
        accelerometerActive = false
        pocketStateHolder = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val max = event.sensor.maximumRange
                val near = event.values.firstOrNull()?.let { it < max && it < 5f } ?: false
                if (near != isNear) {
                    isNear = near
                    toggleAccelerometer(active = near)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val z = event.values.getOrNull(2) ?: 0f
                isFaceDown = z < -7f
            }
        }
        updatePocketState()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun toggleAccelerometer(active: Boolean) {
        val sm = sensorManager ?: return
        val sensor = accelerometer ?: return
        if (active && !accelerometerActive) {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            accelerometerActive = true
        } else if (!active && accelerometerActive) {
            sm.unregisterListener(this, sensor)
            accelerometerActive = false
            isFaceDown = false
        }
    }

    private fun updatePocketState() {
        val newState = isNear || isFaceDown
        if (newState == pocketState) return
        pocketState = newState
        pocketStateHolder = newState

        if (newState) {
            val repo = (application as? OnlyTrustedApp)?.repository ?: return
            scope.launch {
                repo.log(
                    type = SecurityEvent.TYPE_POCKET_LOCK,
                    detail = "near=$isNear faceDown=$isFaceDown"
                )
            }
        }
    }

    companion object {
        @Volatile private var pocketStateHolder = false

        fun isLikelyInPocket(): Boolean = pocketStateHolder

        fun start(context: Context) {
            // Plain startService — this service no longer calls
            // startForeground. The SensorMonitorService FGS in the same
            // process keeps the PID alive so this service's sensor
            // callbacks continue firing.
            runCatching { context.startService(Intent(context, PocketDetectionService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PocketDetectionService::class.java))
        }
    }
}
