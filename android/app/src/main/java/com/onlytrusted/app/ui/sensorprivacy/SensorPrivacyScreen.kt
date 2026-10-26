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

/** Picks the strongest controller available on this install. */
private fun pickController(context: android.content.Context): SensorPrivacyController {
    if (PrivilegedSensorPrivacyController.isActive(context)) {
        return PrivilegedSensorPrivacyController(context)
    }
    if (DeviceOwnerSensorPrivacyController.isActive(context)) {
        return DeviceOwnerSensorPrivacyController(context)
    }
    return ConsumerSensorPrivacyController(context)
}
