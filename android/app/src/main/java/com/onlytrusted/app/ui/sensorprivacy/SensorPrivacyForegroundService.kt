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
import androidx.core.content.ContextCompat
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

    @Volatile private var liveState: LiveSensorStateObserver.State = LiveSensorStateObserver.State.IDLE
    private var liveObserver: LiveSensorStateObserver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundCompat()
        // Live sensor-state observer — swaps the notification icon + color
        // between shield (idle), mic (green), camera (blue), both (purple).
        // Independent of SensorMonitorService's user-visible alerts; this
        // only drives the FGS icon so users can glance at the status bar.
        liveObserver = LiveSensorStateObserver(this) { state ->
            if (state != liveState) {
                liveState = state
                refreshNotification()
            }
        }.also { it.start() }
        // Record this start for the Device Compatibility screen — testers
        // use the restart count to tell us how aggressive their OEM's
        // battery killer is.
        FgsRestartTracker.recordStart(this)
        // Register the receiver on the active controller. The controller picks
        // the strongest available tier (Privileged > DeviceOwner > Consumer).
        val controller = pickController(this)
        ScreenStateReceiver.register(this, controller)
        // Always-on mic activity tracker — used by the screen-off guard to
        // tell a user-accepted VoIP call apart from a silent background mic
        // activation. Independent of the opt-in Reactive Mic Alerts: the
        // alerts post user-visible notifications, this one is internal
        // state that drives lock decisions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MicActivityTracker.start(this)
        }
        // Reactive mic-recording watcher — only runs if the user opted in from
        // the sensor-privacy screen. Share one lifecycle with the FGS so it
        // dies cleanly when the user turns the whole feature off.
        AudioRecordingWatcherState.startIfEnabled(this)
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
        liveObserver?.stop()
        liveObserver = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MicActivityTracker.stop(this)
        }
        AudioRecordingWatcherState.stop()
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
        val state = liveState
        val smallIcon = when (state) {
            LiveSensorStateObserver.State.MIC_ONLY -> R.drawable.ic_notif_mic_active
            LiveSensorStateObserver.State.CAMERA_ONLY -> R.drawable.ic_notif_cam_active
            // "Both" doesn't have a composite icon — pick mic (speech is
            // usually the more sensitive capture). The purple tint still
            // signals the combined state in the shade.
            LiveSensorStateObserver.State.BOTH -> R.drawable.ic_notif_mic_active
            LiveSensorStateObserver.State.IDLE -> R.drawable.ic_notif_shield
        }
        val tintRes = when (state) {
            LiveSensorStateObserver.State.MIC_ONLY -> R.color.notif_tint_mic
            LiveSensorStateObserver.State.CAMERA_ONLY -> R.color.notif_tint_camera
            LiveSensorStateObserver.State.BOTH -> R.color.notif_tint_both
            LiveSensorStateObserver.State.IDLE -> R.color.notif_tint_idle
        }
        val bodyRes = when (state) {
            LiveSensorStateObserver.State.MIC_ONLY -> R.string.sensor_privacy_notif_body_mic
            LiveSensorStateObserver.State.CAMERA_ONLY -> R.string.sensor_privacy_notif_body_camera
            LiveSensorStateObserver.State.BOTH -> R.string.sensor_privacy_notif_body_both
            LiveSensorStateObserver.State.IDLE -> R.string.sensor_privacy_notif_body
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setColor(ContextCompat.getColor(this, tintRes))
            .setColorized(true)
            .setContentTitle(getString(R.string.sensor_privacy_notif_title))
            .setContentText(getString(bodyRes))
            .setOngoing(true)
            .setContentIntent(tap)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            // Group with SensorMonitorService's summary — Android merges
            // the status-bar icons into one. Our own state-swap stays
            // internal to this notification but the OS shows a single icon.
            .setGroup(com.onlytrusted.app.util.NotificationHelper.GROUP_FGS)
            .build()
    }

    /** Repost the ongoing FGS notification so Android picks up the new icon
     *  + tint. Same NOTIFICATION_ID preserves the FGS pin. */
    private fun refreshNotification() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        // IMPORTANCE_MIN — Android still requires the FGS notification to
        // exist, but MIN hides the status-bar icon so SensorMonitorService's
        // single state-driven icon is the only one shown. Keeps the status
        // bar from carrying two shields side-by-side.
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.sensor_privacy_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = getString(R.string.sensor_privacy_channel_desc)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
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

        /** Picks the strongest available controller tier for the receiver.
         *  Priority: Privileged (OEM) > Device Owner (reset flow) >
         *  Device Admin (user dialog) > Consumer. */
        internal fun pickController(context: Context): SensorPrivacyController = when {
            PrivilegedSensorPrivacyController.isActive(context) ->
                PrivilegedSensorPrivacyController(context)
            DeviceOwnerSensorPrivacyController.isActive(context) ->
                DeviceOwnerSensorPrivacyController(context)
            DeviceAdminSensorPrivacyController.isActive(context) ->
                DeviceAdminSensorPrivacyController(context)
            else ->
                ConsumerSensorPrivacyController(context)
        }
    }
}
