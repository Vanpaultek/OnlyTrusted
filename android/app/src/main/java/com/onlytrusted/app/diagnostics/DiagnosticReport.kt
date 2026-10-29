// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.diagnostics

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.onlytrusted.app.BuildConfig
import com.onlytrusted.app.security.AppLockManager
import com.onlytrusted.app.ui.sensorprivacy.AudioRecordingWatcherState
import com.onlytrusted.app.ui.sensorprivacy.FgsRestartTracker
import com.onlytrusted.app.ui.sensorprivacy.SensorPrivacyForegroundService
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Collects a plain-text diagnostic report testers can copy or share from the
 * Device Compatibility screen. Purely local — nothing is uploaded; it's up
 * to the user whether to paste it into feedback.
 *
 * Honest about what it CAN and CAN'T report:
 *  - It can read Android public APIs (permissions, battery exemption,
 *    notification channel states, running services).
 *  - It cannot read OEM-private settings like Samsung "Deep Sleeping Apps,"
 *    Xiaomi "Autostart," or OnePlus "Deep Optimization" — those have no
 *    public API. We flag them in the OEM-specific warnings section so the
 *    user knows to check manually.
 */
object DiagnosticReport {

    private val DANGEROUS_PERMS = listOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    /** Returns a structured in-memory report for the screen to render. */
    fun collect(context: Context): Report {
        val now = System.currentTimeMillis()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?

        // Battery optimization exemption
        val ignoringBatt = pm?.isIgnoringBatteryOptimizations(context.packageName) == true

        // FGS alive status
        val fgsClassName = SensorPrivacyForegroundService::class.java.name
        @Suppress("DEPRECATION")
        val fgsAlive = am?.getRunningServices(Int.MAX_VALUE)
            ?.any { it.service.className == fgsClassName } == true
        val fgsRestarts = FgsRestartTracker.restartCount(context)
        val fgsFirstStart = FgsRestartTracker.firstStartAtMs(context)
        val fgsCurrentUptime = FgsRestartTracker.currentUptimeMs(context)

        // Permissions
        val perms = DANGEROUS_PERMS.mapNotNull { permission ->
            if (permission == Manifest.permission.POST_NOTIFICATIONS &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) return@mapNotNull null
            val granted = ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
            PermissionRow(permission.substringAfterLast('.'), granted)
        }

        // Notification channel states (API 26+)
        val channelsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            val listed = nm.notificationChannels.map { ch ->
                ChannelRow(ch.id, ch.importance != NotificationManager.IMPORTANCE_NONE)
            }
            listed
        } else emptyList()

        return Report(
            generatedAtMs = now,
            app = AppInfo(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
            ),
            device = DeviceInfo(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                product = Build.PRODUCT,
                androidRelease = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                buildFingerprint = Build.FINGERPRINT,
                locale = Locale.getDefault().toString(),
            ),
            battery = BatteryInfo(
                ignoringOptimizations = ignoringBatt,
                powerSaveMode = pm?.isPowerSaveMode == true,
                deviceIdleMode = pm?.isDeviceIdleMode == true,
            ),
            lock = LockInfo(
                appLockEnabled = AppLockManager.isEnabled(context),
                micWatcherEnabled = AudioRecordingWatcherState.isEnabled(context),
            ),
            fgs = FgsInfo(
                alive = fgsAlive,
                restartCount = fgsRestarts,
                firstStartAtMs = fgsFirstStart,
                currentUptimeMs = fgsCurrentUptime,
            ),
            permissions = perms,
            channels = channelsEnabled,
            oemWarnings = oemWarnings(),
        )
    }

    /** Flat text version suitable for copy-to-clipboard / share-sheet. */
    fun format(report: Report): String = buildString {
        appendLine("=== OnlyTrusted — Device Compatibility Report ===")
        appendLine("Generated: ${formatTime(report.generatedAtMs)}")
        appendLine()
        appendLine("## App")
        appendLine("  version:        ${report.app.versionName} (code ${report.app.versionCode})")
        appendLine()
        appendLine("## Device")
        appendLine("  manufacturer:   ${report.device.manufacturer}")
        appendLine("  model:          ${report.device.model}")
        appendLine("  product:        ${report.device.product}")
        appendLine("  android:        ${report.device.androidRelease} (API ${report.device.apiLevel})")
        // Full Build.FINGERPRINT is a near-unique device/ROM identifier (incl.
        // carrier + security-patch date). We expose it in the in-app view but
        // only share an 8-char hash so testers can report platform-specific
        // bugs without accidentally handing a stranger a device fingerprint.
        appendLine("  fingerprint:    ${hashFingerprint(report.device.buildFingerprint)}")
        appendLine("  locale:         ${report.device.locale}")
        appendLine()
        appendLine("## Battery / power")
        appendLine("  battery-exempt: ${report.battery.ignoringOptimizations}")
        appendLine("  power-save:     ${report.battery.powerSaveMode}")
        appendLine("  doze:           ${report.battery.deviceIdleMode}")
        appendLine()
        appendLine("## Locks")
        appendLine("  App Lock:       ${if (report.lock.appLockEnabled) "ON" else "off"}")
        appendLine("  Mic Watcher:    ${if (report.lock.micWatcherEnabled) "ON" else "off"}")
        appendLine()
        appendLine("## Foreground Service (SensorPrivacyForegroundService)")
        appendLine("  alive now:      ${report.fgs.alive}")
        appendLine("  restart count:  ${report.fgs.restartCount}")
        report.fgs.firstStartAtMs?.let { appendLine("  first start:    ${formatTime(it)}") }
        report.fgs.currentUptimeMs?.let { appendLine("  current uptime: ${humanize(it)}") }
        appendLine()
        appendLine("## Permissions")
        report.permissions.forEach { p ->
            appendLine("  ${if (p.granted) "✓" else "✗"} ${p.name}")
        }
        appendLine()
        if (report.channels.isNotEmpty()) {
            appendLine("## Notification channels")
            report.channels.forEach { c ->
                appendLine("  ${if (c.enabled) "✓" else "✗"} ${c.id}")
            }
            appendLine()
        }
        if (report.oemWarnings.isNotEmpty()) {
            appendLine("## OEM compatibility notes")
            report.oemWarnings.forEach { appendLine("  - $it") }
        }
    }

    private fun oemWarnings(): List<String> {
        val mfr = Build.MANUFACTURER.lowercase(Locale.ROOT)
        return when {
            mfr.contains("samsung") -> listOf(
                "Samsung One UI: open Device Care → Battery → Background usage limits → set OnlyTrusted to \"Never sleeping\" so the Mic Lock service survives.",
            )
            mfr.contains("xiaomi") || mfr.contains("redmi") || mfr.contains("poco") -> listOf(
                "MIUI / HyperOS: Settings → Apps → Manage apps → OnlyTrusted → Battery saver → No restrictions.",
                "MIUI: enable Autostart (Permissions → Autostart) or the service will not restart after reboot.",
                "MIUI: enable \"Display pop-up windows while running in background\" so SOS confirmations render.",
            )
            mfr.contains("oneplus") -> listOf(
                "OxygenOS: Settings → Battery → Battery optimization → Don't optimize OnlyTrusted.",
                "OxygenOS: Advanced optimization → ensure OnlyTrusted is NOT marked for Deep sleep.",
            )
            mfr.contains("huawei") || mfr.contains("honor") -> listOf(
                "EMUI / MagicOS: add OnlyTrusted to Protected apps or background services will be killed.",
            )
            mfr.contains("vivo") -> listOf(
                "Funtouch OS: enable High background power consumption, allow Autostart, lock in Recent Apps.",
            )
            mfr.contains("oppo") || mfr.contains("realme") -> listOf(
                "ColorOS / Realme UI: Settings → Battery → OnlyTrusted → disable Deep optimization and enable Allow background activity.",
            )
            else -> listOf("Stock-ish Android — default battery optimization should be fine; enable battery exemption from the Mic Lock card if the service is being killed.")
        }
    }

    /** First 8 chars of SHA-256(fingerprint). Enough to compare between runs
     *  on the same device, nowhere near enough to re-identify a device from
     *  the hash alone. */
    private fun hashFingerprint(fingerprint: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(fingerprint.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(8)
    }

    private fun formatTime(ms: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(ms))

    private fun humanize(ms: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(ms)
        val hours = TimeUnit.MILLISECONDS.toHours(ms) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    // --- Data classes --------------------------------------------------------

    data class Report(
        val generatedAtMs: Long,
        val app: AppInfo,
        val device: DeviceInfo,
        val battery: BatteryInfo,
        val lock: LockInfo,
        val fgs: FgsInfo,
        val permissions: List<PermissionRow>,
        val channels: List<ChannelRow>,
        val oemWarnings: List<String>,
    )

    data class AppInfo(val versionName: String, val versionCode: Int)
    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val product: String,
        val androidRelease: String,
        val apiLevel: Int,
        val buildFingerprint: String,
        val locale: String,
    )
    data class BatteryInfo(
        val ignoringOptimizations: Boolean,
        val powerSaveMode: Boolean,
        val deviceIdleMode: Boolean,
    )
    data class LockInfo(val appLockEnabled: Boolean, val micWatcherEnabled: Boolean)
    data class FgsInfo(
        val alive: Boolean,
        val restartCount: Int,
        val firstStartAtMs: Long?,
        val currentUptimeMs: Long?,
    )
    data class PermissionRow(val name: String, val granted: Boolean)
    data class ChannelRow(val id: String, val enabled: Boolean)
}
