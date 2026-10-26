// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.onlytrusted.app.MainActivity
import com.onlytrusted.app.R

/**
 * Keeps [ScreenStateReceiver] registered while the screen is off.
 *
 * Why a foreground service: Android does NOT deliver `ACTION_SCREEN_OFF` /
 * `ACTION_USER_PRESENT` to manifest-declared receivers (they are "protected"
 * intents). The receiver must be registered at runtime, and the registering
 * process must stay alive. A foreground service with a visible notification
 * is the only user-honest way to keep that process running once the user
 * backgrounds OnlyTrusted.
 *
 * Permission footprint:
 *   - `FOREGROUND_SERVICE` — required to call `startForeground()`.
 *   - `FOREGROUND_SERVICE_SPECIAL_USE` — Android 14+ requires an explicit
 *     type for services that don't fit the standard categories. The
 *     justification string is declared in AndroidManifest.xml so Google
 *     Play review has the rationale on file.
 *
 * Legal/privacy posture (see docs/SENSOR_PRIVACY_ARCHITECTURE.md):
 *   - No audio, video, location, or contacts are read or sent anywhere.
 *   - The service's only job is to call the active [SensorPrivacyController]
 *     when the screen state changes.
 *   - A persistent notification is required by Android law + makes the
 *     background activity visible to the user.
 */
class SensorPrivacyForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundCompat()
        // Register the receiver on the active controller. The controller picks
        // the strongest available tier (Privileged > DeviceOwner > Consumer).
        val controller = pickController(this)
        ScreenStateReceiver.register(this, controller)
        SensorPrivacyLog.append(
            this,
            kind = "SERVICE_STARTED",
            detail = "Lock-when-screen-locked foreground service active (tier=${controller.tierLabel}).",
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY so Android restarts the service if the OS kills it under
        // memory pressure. Combined with the battery optimization exemption
        // (BatteryOptimizationHelper), this gives the receiver the best shot
        // at surviving long screen-off periods.
        return START_STICKY
    }

    override fun onDestroy() {
        ScreenStateReceiver.unregister(this)
        SensorPrivacyLog.append(
            this,
            kind = "SERVICE_STOPPED",
            detail = "Lock-when-screen-locked foreground service stopped.",
        )
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: must declare the service type explicitly.
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val tap = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.sensor_privacy_notif_title))
            .setContentText(getString(R.string.sensor_privacy_notif_body))
            .setOngoing(true)
            .setContentIntent(tap)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.sensor_privacy_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.sensor_privacy_channel_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "ot_sensor_privacy_service"
        private const val NOTIFICATION_ID = 7721

        fun start(context: Context) {
            val intent = Intent(context, SensorPrivacyForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SensorPrivacyForegroundService::class.java))
        }

        /** Picks the strongest available controller tier for the receiver. */
        internal fun pickController(context: Context): SensorPrivacyController = when {
            PrivilegedSensorPrivacyController.isActive(context) ->
                PrivilegedSensorPrivacyController(context)
            DeviceOwnerSensorPrivacyController.isActive(context) ->
                DeviceOwnerSensorPrivacyController(context)
            else ->
                ConsumerSensorPrivacyController(context)
        }
    }
}
