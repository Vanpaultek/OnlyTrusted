// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.privacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.YoutubeSearchedFor
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background

/**
 * One-screen "lower your ad-tracking surface" helper. We cannot toggle any of
 * these for the user — Android forbids a consumer app from managing another
 * app's permissions, and we can't log into their Google account. But we CAN:
 *   - Deep-link the top ad-tracking URLs (myactivity, myadcenter, YouTube
 *     history, Google Maps Timeline, Google Assistant settings).
 *   - Open the system "App info" page for any app the user wants to review,
 *     so revoking its mic/camera/location is a two-tap trip from here.
 *
 * The honest framing: "we show you the dial — Google is the only one who can
 * turn it." No scraping, no credentials, no automation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReduceAdTrackingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reduce ad tracking") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HonestFramingCard()
            SectionHeader("Your Google account")
            GroupCard {
                QuickLinkRow(
                    icon = Icons.Filled.History,
                    title = "Review & clear activity",
                    subtitle = "myactivity.google.com — search, YouTube, Maps history. Pause or delete here.",
                    onClick = { openUrl(context, "https://myactivity.google.com/") },
                )
                RowSep()
                QuickLinkRow(
                    icon = Icons.Filled.Tune,
                    title = "Turn off personalised ads",
                    subtitle = "myadcenter.google.com — switch ad personalisation off across Google surfaces.",
                    onClick = { openUrl(context, "https://myadcenter.google.com/") },
                )
                RowSep()
                QuickLinkRow(
                    icon = Icons.Filled.YoutubeSearchedFor,
                    title = "YouTube watch history",
                    subtitle = "Review or clear what shapes your YouTube recommendations.",
                    onClick = { openUrl(context, "https://www.youtube.com/feed/history") },
                )
                RowSep()
                QuickLinkRow(
                    icon = Icons.Filled.LocationOn,
                    title = "Location history (Maps Timeline)",
                    subtitle = "google.com/maps/timeline — pause and delete past locations.",
                    onClick = { openUrl(context, "https://www.google.com/maps/timeline") },
                )
                RowSep()
                QuickLinkRow(
                    icon = Icons.Filled.RecordVoiceOver,
                    title = "\"Hey Google\" voice match",
                    subtitle = "Google app → Settings → Google Assistant → Hey Google & Voice Match.",
                    onClick = {
                        // Deep-link into the Google app if it exists; fall back
                        // to the Google Assistant settings help URL.
                        val fallback = "https://support.google.com/assistant/answer/7394306"
                        if (!openGoogleApp(context)) openUrl(context, fallback)
                    },
                )
            }

            SectionHeader("App permissions")
            Text(
                "Tap any app to open its system permissions page — revoke mic, camera, " +
                    "location, or contacts from there. Only apps installed on this phone show up.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GroupCard {
                val installed = remember { installedTargets(context) }
                if (installed.isEmpty()) {
                    Text(
                        "None of the common ad-tracking apps are installed on this device.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    installed.forEachIndexed { index, target ->
                        if (index > 0) RowSep()
                        QuickLinkRow(
                            icon = Icons.Filled.Apps,
                            title = target.label,
                            subtitle = "Open ${target.label}'s permissions in Android Settings.",
                            onClick = { openAppDetails(context, target.pkg) },
                        )
                    }
                }
            }

            SectionHeader("System privacy")
            GroupCard {
                QuickLinkRow(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Android privacy dashboard",
                    subtitle = "See which apps used your mic, camera and location in the last 24 h.",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_PRIVACY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Honest limit: Google links data across devices on the same Wi-Fi, " +
                    "same contact graph, and same account. A consumer app cannot block " +
                    "that linkage — only Google can. The quick-links above are the " +
                    "fastest way to pull those dials yourself.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HonestFramingCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "We show you the dial — we can't turn it for you.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Android does not let a consumer app change another app's permissions " +
                    "or sign into your Google account. These are one-tap shortcuts to " +
                    "the real settings where you control it.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun QuickLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RowSep() {
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

/** Common ad-tracking apps worth reviewing. Non-exhaustive on purpose — this
 *  surface is quick-access, not an app audit. The watchdog screen is the
 *  audit tool. */
private data class AppTarget(val pkg: String, val label: String)

private val COMMON_TARGETS = listOf(
    AppTarget("com.google.android.youtube", "YouTube"),
    AppTarget("com.google.android.googlequicksearchbox", "Google"),
    AppTarget("com.android.chrome", "Chrome"),
    AppTarget("com.google.android.apps.maps", "Google Maps"),
    AppTarget("com.google.android.gm", "Gmail"),
    AppTarget("com.facebook.katana", "Facebook"),
    AppTarget("com.instagram.android", "Instagram"),
    AppTarget("com.zhiliaoapp.musically", "TikTok"),
    AppTarget("com.whatsapp", "WhatsApp"),
    AppTarget("us.zoom.videomeetings", "Zoom"),
    AppTarget("com.google.android.apps.tachyon", "Google Meet"),
    AppTarget("com.spotify.music", "Spotify"),
    AppTarget("com.snapchat.android", "Snapchat"),
    AppTarget("com.twitter.android", "X (Twitter)"),
    AppTarget("com.linkedin.android", "LinkedIn"),
)

private fun installedTargets(context: Context): List<AppTarget> {
    val pm = context.packageManager
    return COMMON_TARGETS.filter { target ->
        runCatching { pm.getApplicationInfo(target.pkg, 0) }.isSuccess
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private fun openAppDetails(context: Context, pkg: String) {
    runCatching {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", pkg, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private fun openGoogleApp(context: Context): Boolean {
    val pm = context.packageManager
    val pkg = "com.google.android.googlequicksearchbox"
    val launch = pm.getLaunchIntentForPackage(pkg) ?: return false
    return runCatching {
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}
