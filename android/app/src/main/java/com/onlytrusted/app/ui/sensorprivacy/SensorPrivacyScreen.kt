// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark
import com.onlytrusted.app.ui.theme.TertiaryTextDark
import com.onlytrusted.app.ui.theme.appBackdropBackground
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Settings → "Mic & Camera Lock". Honest UX that mirrors the iOS screen.
 * Picks the active controller tier (Device Owner > Privileged > Consumer)
 * and tells the user exactly what this install can + cannot enforce.
 */
@Composable
fun SensorPrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val controller = remember { pickController(context) }
    var state by remember { mutableStateOf(SensorState.UNKNOWN) }
    var log by remember { mutableStateOf<List<SensorPrivacyLog.Entry>>(emptyList()) }
    val scope = rememberCoroutineScope()

    var serviceEnabled by remember { mutableStateOf(isServiceRunning(context)) }
    var batteryExempt by remember { mutableStateOf(BatteryOptimizationHelper.isExempted(context)) }
    var deviceAdminActive by remember { mutableStateOf(DeviceAdminSensorPrivacyController.isActive(context)) }
    var pauseRemaining by remember { mutableStateOf(SafetyOverride.remainingMinutes(context)) }
    var micWatcherEnabled by remember { mutableStateOf(AudioRecordingWatcherState.isEnabled(context)) }

    LaunchedEffect(Unit) {
        state = controller.readState()
        log = SensorPrivacyLog.recent(context, limit = 20)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackdropBackground()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PrimaryTextDark)
            }
            Text("Mic & Camera Lock",
                 color = PrimaryTextDark, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(state, controller.tierLabel, controller.canEnforceGlobally)
            SafetyOverrideCard(
                remainingMinutes = pauseRemaining,
                onPause = { window ->
                    SafetyOverride.pause(context, window)
                    pauseRemaining = SafetyOverride.remainingMinutes(context)
                },
                onResume = {
                    SafetyOverride.resume(context)
                    pauseRemaining = 0
                },
            )
            DeviceAdminCard(
                active = deviceAdminActive,
                onRequest = {
                    val intent = DeviceAdminSensorPrivacyController.buildActivationIntent(
                        context,
                        "OnlyTrusted needs Device Admin to globally disable the camera when the screen locks. You can revoke this anytime in Settings → Security → Device admin apps.",
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                },
                onRevoke = {
                    DeviceAdminSensorPrivacyController.removeActiveAdmin(context)
                    deviceAdminActive = false
                },
            )
            MicWatcherCard(
                enabled = micWatcherEnabled,
                serviceRunning = serviceEnabled,
                onToggle = { want ->
                    AudioRecordingWatcherState.setEnabled(context, want)
                    micWatcherEnabled = want
                },
            )
            AutoLockCard(
                serviceEnabled = serviceEnabled,
                batteryExempt = batteryExempt,
                onToggleService = { want ->
                    if (want) SensorPrivacyForegroundService.start(context)
                    else SensorPrivacyForegroundService.stop(context)
                    serviceEnabled = want
                },
                onRequestBatteryExempt = {
                    BatteryOptimizationHelper.requestExemption(context)
                },
                onEnableAccessibility = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
            )
            HonestLimitsCard(controller.canEnforceGlobally)
            DeepLinksCard(context, controller)
            if (log.isNotEmpty()) RecentCard(log)
            else EmptyRecentCard()
        }
    }
}

@Composable
private fun StatusCard(state: SensorState, tier: String, canEnforce: Boolean) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow("CURRENT STATE — $tier")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SensorChip("Microphone", Icons.Filled.MicNone, state.microphoneBlocked, Modifier.weight(1f))
                SensorChip("Camera", Icons.Filled.Videocam, state.cameraBlocked, Modifier.weight(1f))
            }
            val note = if (canEnforce)
                "This install can enforce mic + camera restrictions globally on lock."
            else
                "These show whether YOU have revoked mic/camera access for OnlyTrusted. Android does not let a consumer app read what other apps are doing."
            Text(note, color = SecondaryTextDark, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SensorChip(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    blocked: Boolean?,
    modifier: Modifier = Modifier,
) {
    val stateLabel = when (blocked) {
        true -> "Blocked"
        false -> "Allowed"
        null -> "Unknown"
    }
    val tint = when (blocked) {
        true -> Color(0xFF22C55E)
        false -> Color(0xFFF59E0B)
        null -> SecondaryTextDark
    }
    Column(
        modifier = modifier
            .background(Color(0xFF152636), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, name, tint = tint)
        Text(name, color = PrimaryTextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(stateLabel, color = tint, fontSize = 12.sp)
    }
}

@Composable
private fun HonestLimitsCard(canEnforce: Boolean) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow("HONEST LIMITS")
            Text("What this screen does", color = PrimaryTextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Bullet("Reads SensorPrivacyManager state (Android 12+) for your reference.")
            Bullet("Logs every screen-lock + unlock event to your local timeline.")
            Bullet("Links you to Settings → Privacy where you can revoke per-app permissions.")
            if (canEnforce) {
                Bullet("Enforces camera-off + mic-mute when the screen locks (Device Owner / Privileged tier).")
            } else {
                Text("What this screen does NOT do", color = PrimaryTextDark, fontSize = 15.sp,
                     fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                Bullet("Stop Instagram / WhatsApp / Zoom / any other app from using the mic or camera.")
                Bullet("Globally disable sensors when the screen locks — Android reserves that for system apps + Device Owner.")
                Bullet("Change what the mic / camera indicator in your status bar means. Those are Android's indicators, not ours.")
            }
        }
    }
}

@Composable
private fun DeepLinksCard(context: android.content.Context, controller: SensorPrivacyController) {
    val consumer = remember { ConsumerSensorPrivacyController(context) }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow("DO IT YOURSELF")
            DeepLinkRow("Privacy settings", "Android Settings → Privacy") {
                consumer.openPrivacySettings()
            }
            DeepLinkRow("Per-app permission manager", "Revoke mic / camera / location from any app") {
                consumer.openAppPermissionManager()
            }
            DeepLinkRow("OnlyTrusted — this app's settings", "Review what OnlyTrusted itself can access") {
                consumer.openThisAppSettings()
            }
        }
    }
}

@Composable
private fun DeepLinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = PrimaryTextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SecondaryTextDark, fontSize = 12.sp)
        }
        Icon(Icons.Filled.OpenInNew, null, tint = BrandCyanDark, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RecentCard(entries: List<SensorPrivacyLog.Entry>) {
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM) }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Eyebrow("RECENT")
            entries.take(10).forEach { e ->
                Row {
                    Text(fmt.format(Date(e.timestamp)),
                         color = TertiaryTextDark, fontSize = 11.sp,
                         modifier = Modifier.widthIn(min = 150.dp))
                    Text(e.kind, color = PrimaryTextDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                e.detail?.let {
                    Text(it, color = SecondaryTextDark, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyRecentCard() {
    Card {
        Column(Modifier.padding(16.dp)) {
            Eyebrow("RECENT")
            Text(
                "Lock + unlock events will appear here once the screen locks for the first time while OnlyTrusted is running.",
                color = SecondaryTextDark, fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MidnightElevated.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
            .border(1.dp, SecondaryTextDark.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
    ) { content() }
}

@Composable
private fun Eyebrow(text: String) {
    Text(text,
         color = BrandCyanDark, fontSize = 11.sp,
         fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("• ", color = BrandCyanDark, fontSize = 13.sp)
        Text(text, color = SecondaryTextDark, fontSize = 13.sp)
    }
}

/** Picks the strongest controller available on this install.
 *  Priority: Privileged > Device Owner > Device Admin > Consumer. */
private fun pickController(context: android.content.Context): SensorPrivacyController {
    if (PrivilegedSensorPrivacyController.isActive(context)) {
        return PrivilegedSensorPrivacyController(context)
    }
    if (DeviceOwnerSensorPrivacyController.isActive(context)) {
        return DeviceOwnerSensorPrivacyController(context)
    }
    if (DeviceAdminSensorPrivacyController.isActive(context)) {
        return DeviceAdminSensorPrivacyController(context)
    }
    return ConsumerSensorPrivacyController(context)
}

/** Heuristic: inspect running services. Not 100% reliable on Android 8+ but
 *  good enough for the UI to pre-select the switch state on load. */
private fun isServiceRunning(context: android.content.Context): Boolean {
    val am = context.getSystemService(android.app.ActivityManager::class.java) ?: return false
    @Suppress("DEPRECATION")
    val running = runCatching { am.getRunningServices(Int.MAX_VALUE) }.getOrDefault(emptyList())
    return running.any { it.service.className == SensorPrivacyForegroundService::class.java.name }
}

@Composable
private fun SafetyOverrideCard(
    remainingMinutes: Int,
    onPause: (SafetyOverride.Window) -> Unit,
    onResume: () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow("SAFETY OVERRIDE")
            Text(
                "Need the camera urgently? Pause auto-enforcement for a bounded window. " +
                    "Enforcement resumes automatically — no permanent off switch (that would silently defeat the feature).",
                color = SecondaryTextDark, fontSize = 13.sp,
            )
            if (remainingMinutes > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Pause, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Paused — $remainingMinutes min remaining",
                             color = PrimaryTextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Enforcement will resume automatically.",
                             color = SecondaryTextDark, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.clickable(onClick = onResume).padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = BrandCyanDark, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Resume now", color = BrandCyanDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SafetyOverride.Window.values().forEach { w ->
                        PauseChip(label = w.label, onClick = { onPause(w) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PauseChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xFF152636), RoundedCornerShape(20.dp))
            .border(1.dp, SecondaryTextDark.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Pause, null, tint = BrandCyanDark, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = PrimaryTextDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DeviceAdminCard(active: Boolean, onRequest: () -> Unit, onRevoke: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow("UPGRADE: DEVICE ADMIN (OPTIONAL)")
            Text(
                "Device Admin lets OnlyTrusted actually CUT the camera at the hardware level when the screen locks — not just mute the mic. The user grants it via a one-tap system dialog; revoke anytime from Android Settings → Security.",
                color = SecondaryTextDark, fontSize = 13.sp,
            )
            if (active) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Security, null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Device Admin active", color = PrimaryTextDark, fontSize = 14.sp,
                             fontWeight = FontWeight.SemiBold)
                        Text("Camera will be disabled via setCameraDisabled() on every screen lock.",
                             color = SecondaryTextDark, fontSize = 12.sp)
                    }
                    Text("Revoke", color = Color(0xFFF59E0B), fontSize = 12.sp,
                         fontWeight = FontWeight.SemiBold,
                         modifier = Modifier.clickable(onClick = onRevoke).padding(8.dp))
                }
            } else {
                Row(
                    modifier = Modifier.clickable(onClick = onRequest).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Security, null, tint = BrandCyanDark, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Enable Device Admin", color = PrimaryTextDark, fontSize = 14.sp,
                             fontWeight = FontWeight.SemiBold)
                        Text("Opens the system dialog where you grant OnlyTrusted camera-disable permission.",
                             color = SecondaryTextDark, fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.OpenInNew, null, tint = BrandCyanDark, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MicWatcherCard(enabled: Boolean, serviceRunning: Boolean, onToggle: (Boolean) -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow("REACTIVE MIC ALERTS")
            Text(
                "Posts a notification the moment any other app starts using the microphone — " +
                    "even when the screen is unlocked. Different from the lock-based auto-mute: " +
                    "this one fires on actual recording activity.",
                color = SecondaryTextDark, fontSize = 13.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MicNone, null, tint = if (enabled) BrandCyanDark else SecondaryTextDark, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mic activity watcher", color = PrimaryTextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            enabled && serviceRunning -> "Running — you'll be notified when apps record audio."
                            enabled && !serviceRunning -> "Enabled but idle — turn on the lock-watcher below so this can run."
                            else -> "Off — no mic activity notifications."
                        },
                        color = SecondaryTextDark, fontSize = 12.sp,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = BrandCyanDark),
                )
            }
            Text(
                "Notification debounces per app (max 1 per 60s). Dialer, Assistant, and " +
                    "system packages are whitelisted so legitimate mic use doesn't spam you.",
                color = TertiaryTextDark, fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun AutoLockCard(
    serviceEnabled: Boolean,
    batteryExempt: Boolean,
    onToggleService: (Boolean) -> Unit,
    onRequestBatteryExempt: () -> Unit,
    onEnableAccessibility: () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow("AUTO-LOCK WHEN SCREEN LOCKS")
            Text(
                "Watches for screen-off / unlock and automatically mutes the mic + camera. " +
                    "No audio or video is recorded — the service only toggles the sensor restriction.",
                color = SecondaryTextDark, fontSize = 13.sp,
            )
            Text(
                "Skips the lock when you're on a real call (cellular, or VoIP " +
                    "like WhatsApp, Signal, Zoom, Meet, Teams, Discord), a call " +
                    "is ringing, or media is playing — your call and audio won't " +
                    "be cut. If an app silently activates the mic without your " +
                    "input (e.g. in the background), the lock still fires. The " +
                    "signal: recording must have started while the screen was on " +
                    "and been active a few seconds — a silent flip into VoIP " +
                    "mode doesn't qualify. QS tile, launcher shortcut, and " +
                    "Assistant always lock immediately, no heuristic.",
                color = TertiaryTextDark, fontSize = 12.sp,
            )
            // Enable toggle.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PowerSettingsNew, null,
                     tint = if (serviceEnabled) BrandCyanDark else SecondaryTextDark,
                     modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lock-watcher service", color = PrimaryTextDark, fontSize = 14.sp,
                         fontWeight = FontWeight.SemiBold)
                    Text(if (serviceEnabled) "Running — a persistent notification is shown by Android law."
                         else "Off — screen-lock events will not be observed.",
                         color = SecondaryTextDark, fontSize = 12.sp)
                }
                Switch(checked = serviceEnabled, onCheckedChange = onToggleService,
                       colors = SwitchDefaults.colors(checkedThumbColor = BrandCyanDark))
            }
            // Battery exemption prompt.
            if (serviceEnabled && !batteryExempt) {
                Row(
                    modifier = Modifier.clickable(onClick = onRequestBatteryExempt).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Tune, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Battery optimisation — needs attention",
                             color = PrimaryTextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Android may kill the service to save battery. Tap to exempt OnlyTrusted.",
                             color = SecondaryTextDark, fontSize = 11.sp)
                    }
                    Icon(Icons.Filled.OpenInNew, null, tint = BrandCyanDark, modifier = Modifier.size(16.dp))
                }
            }
            // Volume-key trigger prompt.
            Row(
                modifier = Modifier.clickable(onClick = onEnableAccessibility).padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.VolumeDown, null, tint = BrandCyanDark, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Volume-down double-press trigger",
                         color = PrimaryTextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Optional. Enable OnlyTrusted in Settings → Accessibility. The service only observes volume keys — never reads screen content.",
                         color = SecondaryTextDark, fontSize = 11.sp)
                }
                Icon(Icons.Filled.OpenInNew, null, tint = BrandCyanDark, modifier = Modifier.size(16.dp))
            }
            Text(
                "Quick Settings tile: swipe down twice → Edit → add \"OnlyTrusted mic/cam\" for one-tap control from the pulldown panel.",
                color = TertiaryTextDark, fontSize = 11.sp,
            )
        }
    }
}
