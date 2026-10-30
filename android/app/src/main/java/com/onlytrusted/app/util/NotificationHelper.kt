// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.util

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.onlytrusted.app.MainActivity
import com.onlytrusted.app.R

object NotificationHelper {

    const val CHANNEL_MONITOR = "monitor_channel"
    const val CHANNEL_ALERTS = "alerts_channel"
    /**
     * Silent channel for secondary foreground services (pocket detection,
     * sensor-privacy lock, etc.). Posted with IMPORTANCE_MIN so Android
     * hides their status-bar icon — only the shade entry remains, as the
     * FGS spec requires. The primary user-facing status-bar icon lives on
     * SensorMonitorService's [CHANNEL_MONITOR] (LOW importance, visible)
     * so there is exactly ONE OnlyTrusted icon in the status bar that
     * reflects live mic/camera activity.
     */
    const val CHANNEL_SILENT_BG = "ot_background_services_silent"

    /**
     * Shared group key for every OnlyTrusted foreground-service notification.
     * Android collapses notifications sharing a group into a single
     * status-bar icon when a summary notification is also posted. The
     * summary belongs to SensorMonitorService — that's the one with the
     * state-driven shield / mic / camera icon.
     */
    const val GROUP_FGS = "ot_foreground_services"

    const val NOTIF_FG_MONITOR = 1001
    const val NOTIF_FG_POCKET = 1002
    const val NOTIF_FG_SHIELD = 1003
    private const val NOTIF_ALERT_BASE = 2000

    fun buildMonitorNotification(context: Context, text: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setContentTitle(context.getString(R.string.monitor_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setContentIntent(mainActivityIntent(context))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    /**
     * Build a silent FGS notification on the IMPORTANCE_MIN channel. Android
     * requires every foreground service to have a visible notification, but
     * MIN importance hides the status-bar icon so secondary services don't
     * clutter the bar. Only SensorMonitorService's notification stays
     * status-bar-visible so the user sees exactly one OnlyTrusted icon that
     * tracks live mic/camera state.
     */
    fun buildSilentBackgroundNotification(context: Context, text: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_SILENT_BG)
            .setContentTitle(context.getString(R.string.monitor_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setContentIntent(mainActivityIntent(context))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            // Group with the SensorMonitorService summary so Android shows
            // one combined status-bar icon rather than one per service.
            .setGroup(GROUP_FGS)
            .build()
    }

    /**
     * Create the silent channel on first use. Idempotent — safe to call
     * from any FGS onCreate. Only has effect on API 26+ where channels
     * exist at all.
     */
    fun ensureSilentChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_SILENT_BG) != null) return
        val channel = android.app.NotificationChannel(
            CHANNEL_SILENT_BG,
            context.getString(R.string.channel_silent_background_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = context.getString(R.string.channel_silent_background_desc)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        nm.createNotificationChannel(channel)
    }

    fun postAlert(context: Context, title: String, body: String, tag: String) {
        // On the lock screen we show only a generic headline — the real body (which may contain
        // a caller / sender phone number) is replaced by a redacted public version. The full
        // body is still visible to the owner once the device is unlocked and they tap through.
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setContentTitle(context.getString(R.string.alert_lockscreen_title))
            .setContentText(context.getString(R.string.alert_lockscreen_body))
            .setSmallIcon(R.drawable.ic_shield)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_shield)
            .setAutoCancel(true)
            .setContentIntent(mainActivityIntent(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(tag, NOTIF_ALERT_BASE, notification)
        vibrate(context)
    }

    private fun mainActivityIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun vibrate(context: Context) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator ?: return
        if (!vibrator.hasVibrator()) return
        val pattern = longArrayOf(0, 180, 90, 180)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
