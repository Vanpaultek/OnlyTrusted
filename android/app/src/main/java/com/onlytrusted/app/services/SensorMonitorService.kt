// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.IBinder
import com.onlytrusted.app.OnlyTrustedApp
import com.onlytrusted.app.data.db.entities.SecurityEvent
import com.onlytrusted.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class SensorMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollJob: Job? = null

    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private val cameraManager by lazy { getSystemService(CameraManager::class.java) }

    // Live sensor state — drives the state-colored FGS notification icon.
    // mic/cam booleans are updated by the callbacks below; the derived
    // State is passed into [buildMonitorNotificationForState] via
    // [refreshNotificationIcon] whenever either flag flips.
    private enum class LiveState { IDLE, MIC_ONLY, CAMERA_ONLY, BOTH }
    @Volatile private var micLive = false
    private val activeCameras = HashSet<String>()
    @Volatile private var publishedState: LiveState = LiveState.IDLE

    private val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
            super.onRecordingConfigChanged(configs)
            handleAudioConfigs(configs)
            val anyActive = configs.any { !it.isClientSilenced }
            if (micLive != anyActive) {
                micLive = anyActive
                refreshNotificationIcon()
            }
        }
    }

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            super.onCameraAvailable(cameraId)
            if (activeCameras.remove(cameraId)) refreshNotificationIcon()
        }
        override fun onCameraUnavailable(cameraId: String) {
            super.onCameraUnavailable(cameraId)
            logCameraInUse(cameraId)
            if (activeCameras.add(cameraId)) refreshNotificationIcon()
        }
    }

    private val micAlertedSessions = mutableSetOf<Int>()
    private val cameraAlertCooldown = mutableMapOf<String, Long>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundInternal()
        audioManager?.registerAudioRecordingCallback(recordingCallback, null)
        cameraManager?.registerAvailabilityCallback(cameraCallback, null)
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundInternal()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        pollJob?.cancel()
        scope.cancel()
        runCatching { audioManager?.unregisterAudioRecordingCallback(recordingCallback) }
        runCatching { cameraManager?.unregisterAvailabilityCallback(cameraCallback) }
    }

    private fun startForegroundInternal() {
        val notification = buildStateNotification(publishedState)
        // The manifest already declares the foreground-service types; the system picks them up.
        startForeground(NotificationHelper.NOTIF_FG_MONITOR, notification)
    }

    /**
     * Build the single visible status-bar notification with icon + color
     * driven by live mic/camera state:
     *   IDLE         → shield + brand teal
     *   MIC_ONLY     → mic shape + green
     *   CAMERA_ONLY  → camera shape + blue
     *   BOTH         → mic shape + purple
     *
     * Status-bar icons are forced monochrome by Android, so the SHAPE
     * conveys which sensor is active at a glance; the pulled-down
     * notification body carries the real tint via setColor + setColorized.
     */
    private fun buildStateNotification(state: LiveState): Notification {
        val iconRes = when (state) {
            LiveState.MIC_ONLY -> com.onlytrusted.app.R.drawable.ic_notif_mic_active
            LiveState.CAMERA_ONLY -> com.onlytrusted.app.R.drawable.ic_notif_cam_active
            LiveState.BOTH -> com.onlytrusted.app.R.drawable.ic_notif_mic_active
            LiveState.IDLE -> com.onlytrusted.app.R.drawable.ic_notif_shield
        }
        val tintRes = when (state) {
            LiveState.MIC_ONLY -> com.onlytrusted.app.R.color.notif_tint_mic
            LiveState.CAMERA_ONLY -> com.onlytrusted.app.R.color.notif_tint_camera
            LiveState.BOTH -> com.onlytrusted.app.R.color.notif_tint_both
            LiveState.IDLE -> com.onlytrusted.app.R.color.notif_tint_idle
        }
        val bodyRes = when (state) {
            LiveState.MIC_ONLY -> com.onlytrusted.app.R.string.monitor_text_mic
            LiveState.CAMERA_ONLY -> com.onlytrusted.app.R.string.monitor_text_camera
            LiveState.BOTH -> com.onlytrusted.app.R.string.monitor_text_both
            LiveState.IDLE -> com.onlytrusted.app.R.string.monitor_text_idle
        }
        val tap = android.app.PendingIntent.getActivity(
            this, 0,
            Intent(this, com.onlytrusted.app.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return androidx.core.app.NotificationCompat.Builder(this, NotificationHelper.CHANNEL_MONITOR)
            .setSmallIcon(iconRes)
            .setColor(androidx.core.content.ContextCompat.getColor(this, tintRes))
            .setColorized(true)
            .setContentTitle(getString(com.onlytrusted.app.R.string.monitor_title))
            .setContentText(getString(bodyRes))
            .setOngoing(true)
            .setContentIntent(tap)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_STATUS)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_SECRET)
            // Owns the group summary — Android collapses sibling FGS
            // notifications into this icon in the status bar.
            .setGroup(NotificationHelper.GROUP_FGS)
            .setGroupSummary(true)
            .build()
    }

    /** Called whenever mic/camera activity changes. Repost the ongoing
     *  notification with the new icon + tint. Same ID preserves the FGS pin. */
    private fun refreshNotificationIcon() {
        val cam = activeCameras.isNotEmpty()
        val next = when {
            micLive && cam -> LiveState.BOTH
            micLive -> LiveState.MIC_ONLY
            cam -> LiveState.CAMERA_ONLY
            else -> LiveState.IDLE
        }
        if (next == publishedState) return
        publishedState = next
        val nm = getSystemService(android.app.NotificationManager::class.java) ?: return
        nm.notify(NotificationHelper.NOTIF_FG_MONITOR, buildStateNotification(next))
    }

    private fun startPolling() {
        val am = audioManager ?: return
        // The AudioRecordingCallback fires on every change; this loop is only a safety net
        // against missed callbacks. We adapt the interval to activity: tight 30 s poll
        // while a recording is live (catch any missed end-events quickly) and relaxed
        // 2 min poll while idle (don't burn CPU wake-ups when nothing is recording).
        pollJob = scope.launch {
            var lastWasActive = false
            while (true) {
                val interval = if (lastWasActive) ACTIVE_POLL_INTERVAL_MS else IDLE_POLL_INTERVAL_MS
                delay(interval)
                val configs = am.activeRecordingConfigurations
                lastWasActive = configs.isNotEmpty()
                handleAudioConfigs(configs)
            }
        }
    }

    private fun handleAudioConfigs(configs: List<AudioRecordingConfiguration>) {
        if (configs.isEmpty()) {
            micAlertedSessions.clear()
            return
        }
        val now = System.currentTimeMillis()
        val timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now))
        for (config in configs) {
            if (config.isClientSilenced) continue
            val id = config.clientAudioSessionId
            if (!micAlertedSessions.add(id)) continue

            val packageName = resolveOwnerForAudioSession(config)
            val appLabel = packageName?.let { labelForPackage(it) }
            val repo = (application as? OnlyTrustedApp)?.repository
            scope.launch {
                repo?.log(
                    type = SecurityEvent.TYPE_MIC,
                    packageName = packageName,
                    detail = "session=$id, time=$timeString"
                )
            }
            NotificationHelper.postAlert(
                context = this,
                title = getString(com.onlytrusted.app.R.string.alert_mic_title),
                body = if (appLabel != null) {
                    getString(com.onlytrusted.app.R.string.alert_mic_body_with_pkg, appLabel, timeString)
                } else {
                    getString(com.onlytrusted.app.R.string.alert_mic_body_generic, timeString)
                },
                tag = "mic-$id"
            )
        }
    }

    /**
     * [AudioRecordingConfiguration] exposes `getClientPackageName()` on API 29+
     * but through a hidden method accessible only via reflection. Resolve it
     * once per lookup; return null on failure (any API, any OEM-stripped build)
     * so the notification falls back to the honest generic message.
     */
    private fun resolveOwnerForAudioSession(config: AudioRecordingConfiguration): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val m = AudioRecordingConfiguration::class.java.getMethod("getClientPackageName")
            m.invoke(config) as? String
        }.getOrNull()
    }

    /**
     * Resolve a package name to its user-visible app label (e.g.
     * "com.whatsapp" → "WhatsApp"). Falls back to the package name itself if
     * the label can't be read (uninstalled, blocked, etc.).
     */
    private fun labelForPackage(packageName: String): String {
        val pm = packageManager
        return runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }

    private fun logCameraInUse(cameraId: String) {
        val now = System.currentTimeMillis()
        val last = cameraAlertCooldown[cameraId] ?: 0L
        if (now - last < CAMERA_ALERT_COOLDOWN_MS) return
        cameraAlertCooldown[cameraId] = now

        val timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now))
        val cameraDescription = describeCamera(cameraId)
        val repo = (application as? OnlyTrustedApp)?.repository
        scope.launch {
            repo?.log(
                type = SecurityEvent.TYPE_CAMERA,
                packageName = null,
                detail = "cameraId=$cameraId, which=$cameraDescription, time=$timeString"
            )
        }
        NotificationHelper.postAlert(
            context = this,
            title = getString(com.onlytrusted.app.R.string.alert_camera_title),
            body = getString(com.onlytrusted.app.R.string.alert_camera_body, cameraDescription, timeString),
            tag = "cam-$cameraId"
        )
    }

    /**
     * Translate the opaque camera ID into "Front" / "Back" / "External" by
     * reading its [CameraCharacteristics] LENS_FACING. More useful than
     * "Camera 2" in a user-facing notification. Falls back to the raw ID.
     *
     * Android does NOT expose which app opened the camera to third-party
     * apps — [AppOpsManager.OnOpActiveChangedListener] can do it but needs
     * GET_APP_OPS_STATS which is signature|privileged. Documenting that
     * limit honestly in the notification body.
     */
    private fun describeCamera(cameraId: String): String {
        return runCatching {
            val chars = cameraManager?.getCameraCharacteristics(cameraId) ?: return@runCatching "Camera $cameraId"
            when (chars.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)) {
                android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK -> "Back"
                android.hardware.camera2.CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Camera $cameraId"
            }
        }.getOrDefault("Camera $cameraId")
    }

    companion object {
        private const val ACTIVE_POLL_INTERVAL_MS = 30_000L
        private const val IDLE_POLL_INTERVAL_MS = 120_000L
        private const val CAMERA_ALERT_COOLDOWN_MS = 10_000L

        fun start(context: Context) {
            val intent = Intent(context, SensorMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SensorMonitorService::class.java))
        }
    }
}
