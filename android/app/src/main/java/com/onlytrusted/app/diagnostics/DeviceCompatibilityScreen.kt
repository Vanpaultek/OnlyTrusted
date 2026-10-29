// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.DangerDark
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.MidnightCard
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark
import com.onlytrusted.app.ui.theme.TertiaryTextDark

/**
 * Settings → Device compatibility. Builds a one-tap diagnostic report so
 * testers can paste or share their device's state when they report a bug.
 *
 * Honest UX: no uploading, no analytics — the report is built in-memory and
 * the user chooses whether to copy or share. Nothing leaves the device
 * unless they tap one of the two action buttons.
 */
@Composable
fun DeviceCompatibilityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val report = remember { DiagnosticReport.collect(context) }
    val text = remember { DiagnosticReport.format(report) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        TopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IntroCard()
            SectionCard("Device") {
                Line("Make", "${report.device.manufacturer} ${report.device.model}")
                Line("Android", "${report.device.androidRelease} (API ${report.device.apiLevel})")
                Line("Locale", report.device.locale)
            }
            SectionCard("Battery / power") {
                StatusLine(
                    label = "Battery optimization exempt",
                    good = report.battery.ignoringOptimizations,
                    goodHint = "✓ The Mic Lock service won't be killed by the OS power saver.",
                    badHint = "⚠ The OS may kill the Mic Lock service in the background. Grant exemption from the Mic Lock card.",
                )
                Line("Power save mode", if (report.battery.powerSaveMode) "ON" else "off")
                Line("Device Idle (Doze)", if (report.battery.deviceIdleMode) "ON" else "off")
            }
            SectionCard("App Lock + Mic Watcher") {
                Line("App Lock", if (report.lock.appLockEnabled) "ON" else "off")
                Line("Mic Activity Watcher", if (report.lock.micWatcherEnabled) "ON" else "off")
            }
            SectionCard("Foreground service") {
                StatusLine(
                    label = "Service alive now",
                    good = report.fgs.alive,
                    goodHint = "✓ The Mic Lock service is running right now.",
                    badHint = "✗ The service is not running. If you expected it on, your OEM may be killing it.",
                )
                Line("Restarts since install", report.fgs.restartCount.toString())
                report.fgs.currentUptimeMs?.let { Line("Current uptime", formatUptime(it)) }
            }
            SectionCard("Permissions (${report.permissions.count { it.granted }}/${report.permissions.size} granted)") {
                report.permissions.forEach { p ->
                    PermissionLine(p.name, p.granted)
                }
            }
            if (report.channels.isNotEmpty()) {
                SectionCard("Notification channels") {
                    report.channels.forEach { c ->
                        PermissionLine(c.id, c.enabled)
                    }
                }
            }
            if (report.oemWarnings.isNotEmpty()) {
                SectionCard("OEM-specific tips") {
                    report.oemWarnings.forEach { warning ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•", color = BrandAccentDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                            Text(warning, color = PrimaryTextDark, fontSize = 13.sp)
                        }
                    }
                }
            }
            ActionButtons(text = text)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryTextDark)
        }
        Text(
            "Device compatibility",
            color = PrimaryTextDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun IntroCard() {
    Text(
        "Local diagnostic — tap Copy or Share to send this with a bug report. " +
            "Nothing is uploaded automatically.",
        color = SecondaryTextDark,
        fontSize = 13.sp,
    )
}

@Composable
private fun SectionCard(title: String, body: @Composable ColumnScopeMarker.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightCard)
            .border(1.dp, BrandCyanDark.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = PrimaryTextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        ColumnScopeMarker.body()
    }
}

/** Tiny marker object so SectionCard's body lambda can use shared helpers. */
private object ColumnScopeMarker

@Composable
private fun ColumnScopeMarker.Line(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SecondaryTextDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = PrimaryTextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ColumnScopeMarker.StatusLine(label: String, good: Boolean, goodHint: String, badHint: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (good) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (good) BrandCyanDark else DangerDark,
                modifier = Modifier.width(18.dp).height(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(label, color = PrimaryTextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            if (good) goodHint else badHint,
            color = TertiaryTextDark,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 26.dp),
        )
    }
}

@Composable
private fun ColumnScopeMarker.PermissionLine(name: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Filled.Error,
            contentDescription = null,
            tint = if (granted) BrandCyanDark else SecondaryTextDark,
            modifier = Modifier.width(14.dp).height(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            name,
            color = PrimaryTextDark,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun ActionButtons(text: String) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Button(
            onClick = { copyToClipboard(context, text) },
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandCyanDark,
                contentColor = MidnightBackground,
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.height(16.dp).width(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Copy report to clipboard", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = { shareReport(context, text) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, tint = BrandAccentDark, modifier = Modifier.height(16.dp).width(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Share…", color = BrandAccentDark, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("OnlyTrusted diagnostic", text))
    Toast.makeText(context, "Diagnostic copied", Toast.LENGTH_SHORT).show()
}

private fun shareReport(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "OnlyTrusted — Device Compatibility Report")
    }
    context.startActivity(Intent.createChooser(intent, "Share diagnostic"))
}

private fun formatUptime(ms: Long): String {
    val minutes = ms / 60_000L
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        else -> "${minutes}m"
    }
}
