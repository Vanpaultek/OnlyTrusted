// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.security.AppLockManager
import com.onlytrusted.app.ui.safety.AutoLockPrefs
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark
import com.onlytrusted.app.ui.theme.TertiaryTextDark

/**
 * ModalBottomSheet grouping the three "lock-related" controls — same component
 * is referenced from both the Dashboard entry card and the Settings inline
 * section so iOS + Android + the user get identical UX.
 *
 * The three controls:
 *   1. App Lock                       — re-lock OnlyTrusted on open
 *   2. Auto-lock after inactivity     — re-lock after N minutes in background
 *   3. Mic & Camera Lock              — opens the full sensor-privacy screen
 *
 * All state persists to the same stores that SettingsScreen writes to
 * (`AppLockManager`, `AutoLockPrefs`), so a change in the sheet is visible
 * in Settings on the next render + vice versa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityLockGroupSheet(
    appLockInitiallyEnabled: Boolean,
    onAppLockChanged: (Boolean) -> Unit,
    onOpenMicCameraLock: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var appLockEnabled by remember { mutableStateOf(appLockInitiallyEnabled) }
    var autoLockSeconds by remember { mutableStateOf(AutoLockPrefs.currentSeconds(context)) }
    val autoLockEnabled = autoLockSeconds > 0
    var showWindowPicker by remember { mutableStateOf(false) }
    // Mic & Camera auto-lock service: starting it registers ScreenStateReceiver,
    // so mic mutes on ACTION_SCREEN_OFF and un-mutes on ACTION_USER_PRESENT.
    var micCamAutoLock by remember { mutableStateOf(isServiceActive(context)) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MidnightElevated) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Security Lock",
                color = PrimaryTextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                "App Lock, auto-lock timer, and Mic & Camera Lock in one place. Edit any — the Settings page stays in sync.",
                color = SecondaryTextDark, fontSize = 13.sp,
            )

            Divider(color = SecondaryTextDark.copy(alpha = 0.14f))

            // App Lock lives in Settings only — not shown here. This sheet is
            // scoped to mic + camera lock on device screen state; app-open PIN
            // / biometric belongs with the general app settings.

            // Auto-lock after inactivity
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(Icons.Filled.Timer)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Auto-lock after inactivity",
                         color = PrimaryTextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (autoLockEnabled)
                            "Re-lock after ${AutoLockPrefs.label(autoLockSeconds)} in the background."
                        else "Only lock when you choose Panic lock.",
                        color = SecondaryTextDark, fontSize = 12.sp,
                    )
                }
                Switch(
                    checked = autoLockEnabled,
                    onCheckedChange = { want ->
                        if (want) {
                            if (autoLockSeconds == 0) autoLockSeconds = 60
                            AutoLockPrefs.setSeconds(context, autoLockSeconds)
                            AppLockManager.setGraceSeconds(context, autoLockSeconds)
                        } else {
                            autoLockSeconds = 0
                            AutoLockPrefs.setSeconds(context, 0)
                            AppLockManager.setGraceSeconds(context, 600)
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = BrandCyanDark),
                )
            }

            // Inactivity window — always visible; tappable only when auto-lock
            // is ON. Read-only (dimmed, not clickable) when auto-lock is OFF so
            // the user sees the stored value but can't change it without first
            // enabling the parent toggle.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (autoLockEnabled) {
                    Modifier.clickable { showWindowPicker = true }
                } else {
                    Modifier.alpha(0.45f)
                },
            ) {
                Spacer(Modifier.width(44.dp))
                Column(Modifier.weight(1f)) {
                    Text("Inactivity window", color = PrimaryTextDark, fontSize = 14.sp)
                }
                Text(AutoLockPrefs.label(autoLockSeconds),
                     color = BrandCyanDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.ChevronRight, null, tint = SecondaryTextDark, modifier = Modifier.size(16.dp))
            }

            Divider(color = SecondaryTextDark.copy(alpha = 0.08f))

            // Mic & Camera Lock — actual ON/OFF switch for the lock-watcher
            // service. When ON, mic mutes on ACTION_SCREEN_OFF and un-mutes
            // on ACTION_USER_PRESENT. Device Admin adds camera-disable to
            // the same cycle (configure in the detail screen).
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(Icons.Filled.MicNone)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Auto-lock mic & camera",
                         color = PrimaryTextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (micCamAutoLock)
                            "On — mic mutes on screen-off, restores on unlock."
                        else "Off — mic + camera unchanged when the screen locks.",
                        color = SecondaryTextDark, fontSize = 12.sp,
                    )
                }
                Switch(
                    checked = micCamAutoLock,
                    onCheckedChange = { want ->
                        if (want) SensorPrivacyForegroundService.start(context)
                        else SensorPrivacyForegroundService.stop(context)
                        micCamAutoLock = want
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = BrandCyanDark),
                )
            }

            // Detail-screen link (Device Admin, battery exemption, Quick Settings tile).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onOpenMicCameraLock(); onDismiss() },
            ) {
                Spacer(Modifier.width(44.dp))
                Text("More options (Device Admin, battery, tile)",
                     color = BrandCyanDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = SecondaryTextDark, modifier = Modifier.size(16.dp))
            }

            Text(
                "iOS has no equivalent — Apple forbids this to consumer apps. The Mic & Camera Lock behaviour applies to Android only.",
                color = TertiaryTextDark, fontSize = 11.sp,
            )
        }
    }

    if (showWindowPicker) {
        WindowPickerDialog(
            current = autoLockSeconds,
            onPick = {
                autoLockSeconds = it
                AutoLockPrefs.setSeconds(context, it)
                AppLockManager.setGraceSeconds(context, it)
                showWindowPicker = false
            },
            onDismiss = { showWindowPicker = false },
        )
    }
}

/** Heuristic — true if the lock-watcher foreground service is currently running. */
private fun isServiceActive(context: android.content.Context): Boolean {
    val am = context.getSystemService(android.app.ActivityManager::class.java) ?: return false
    @Suppress("DEPRECATION")
    val running = runCatching { am.getRunningServices(Int.MAX_VALUE) }.getOrDefault(emptyList())
    return running.any { it.service.className == SensorPrivacyForegroundService::class.java.name }
}

@Composable
private fun IconChip(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(BrandCyanDark.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = BrandCyanDark, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun WindowPickerDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(30 to "30 seconds", 60 to "1 minute",
                         300 to "5 minutes", 900 to "15 minutes")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightElevated,
        title = { Text("Inactivity window", color = PrimaryTextDark) },
        text = {
            Column {
                options.forEach { (s, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(s) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = s == current,
                            onClick = { onPick(s) },
                            colors = RadioButtonDefaults.colors(selectedColor = BrandCyanDark),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(label, color = PrimaryTextDark, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = BrandCyanDark)
            }
        },
    )
}
