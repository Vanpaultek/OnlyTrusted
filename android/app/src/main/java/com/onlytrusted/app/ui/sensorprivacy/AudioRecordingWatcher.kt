// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.onlytrusted.app.R

/**
 * Reactive sibling of the lock-based auto-mute. Observes
 * [AudioManager.AudioRecordingCallback] and raises a notification the moment
 * ANY third-party app starts using the microphone — even if your phone is
 * unlocked and the lock-watcher isn't firing.
 *
 * Design:
 *   - Single callback registered from [SensorPrivacyForegroundService].
 *   - Debounces per-package: at most one notification per 60s per app.
 *   - Whitelists system packages that are expected to use the mic
 *     (dialer, assistant, system UI, carrier call app, OnlyTrusted itself).
 *   - Notification has a one-tap "Mute mic" action that fires the
 *     [LockAndMuteActivity] shortcut.
 *
 * Opt-in: user enables this watcher from the sensor-privacy settings. We
 * deliberately keep it off by default because any notification stream is
 * noise unless the user actually wants it.
 */
@RequiresApi(Build.VERSION_CODES.N)
class AudioRecordingWatcher(private val context: Context) {

    private val audioManager by lazy {
        context.getSystemService(AudioManager::class.java)
    }

    private var callback: AudioManager.AudioRecordingCallback? = null
    private val lastNotifiedAt = HashMap<String, Long>()

    fun start() {
        if (callback != null) return
        val am = audioManager ?: return
        createChannel()
        callback = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
                configs?.forEach { config ->
                    val pkg = recordingPackage(config) ?: return@forEach
                    if (pkg in WHITELIST || pkg == context.packageName) return@forEach
                    maybeNotify(pkg)
                }
            }
        }
        am.registerAudioRecordingCallback(callback!!, Handler(Looper.getMainLooper()))
        SensorPrivacyLog.append(
            context,
            kind = "MIC_WATCHER_STARTED",
            detail = "AudioRecordingCallback registered.",
        )
    }

    fun stop() {
        val am = audioManager ?: return
        callback?.let {
            am.unregisterAudioRecordingCallback(it)
            SensorPrivacyLog.append(
                context,
                kind = "MIC_WATCHER_STOPPED",
                detail = "AudioRecordingCallback unregistered.",
            )
        }
        callback = null
        lastNotifiedAt.clear()
    }

    private fun maybeNotify(packageName: String) {
        val now = System.currentTimeMillis()
        val last = lastNotifiedAt[packageName] ?: 0L
        if (now - last < DEBOUNCE_MS) return
        lastNotifiedAt[packageName] = now

        val label = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)

        val muteIntent = Intent(context, LockAndMuteActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val mutePi = PendingIntent.getActivity(
            context, 0, muteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.mic_watcher_title, label))
            .setContentText(context.getString(R.string.mic_watcher_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.mic_watcher_action_mute), mutePi)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.notify(NOTIFICATION_ID_BASE + packageName.hashCode(), notification)

        SensorPrivacyLog.append(
            context,
            kind = "MIC_WATCHER_ALERT",
            detail = "mic active in $label ($packageName) — alert shown.",
        )
    }

    private fun recordingPackage(config: AudioRecordingConfiguration): String? {
        // clientPackageName is API 29+ and only accessible via reflection on
        // earlier levels (there's no public getter below Q). Fall back to null
        // on older devices — we skip the notification entirely rather than
        // show an opaque "An app is recording" without naming it.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val m = AudioRecordingConfiguration::class.java.getMethod("getClientPackageName")
            m.invoke(config) as? String
        }.getOrNull()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.mic_watcher_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.mic_watcher_channel_desc)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "ot_mic_watcher"
        private const val NOTIFICATION_ID_BASE = 7730
        private const val DEBOUNCE_MS = 60_000L

        /** Packages we expect to use the mic without needing a user alert.
         *  Keep tight — adding too many silences legitimate warnings. */
        private val WHITELIST = setOf(
            "com.android.phone",             // dialer
            "com.android.systemui",          // system UI (lock screen sounds etc.)
            "com.google.android.googlequicksearchbox", // Google Assistant
            "com.samsung.android.bixby.agent",         // Bixby
            "com.google.android.dialer",     // Google Phone
            "com.android.telecom",           // Telecom framework
            "com.android.incallui",          // In-call UI
        )
    }
}
