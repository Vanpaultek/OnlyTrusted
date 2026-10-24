// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.distress

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.ui.theme.AppBackdropScaffold
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.DangerDark
import com.onlytrusted.app.ui.theme.MidnightCard
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark

/**
 * Settings screen for the Distress Signal feature — three triggers + WhatsApp group link
 * configuration + the on-screen walkthrough for creating the group.
 *
 * No new concepts introduced here — just a UI surface over the SharedPreferences stores
 * ShakeDetector.Prefs, VolumeDownTrigger.Prefs, and WhatsAppDistressSender.Prefs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistressSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var shakeEnabled by remember {
        mutableStateOf(ShakeDetector.Prefs.isEnabled(context))
    }
    var volumeDownEnabled by remember {
        mutableStateOf(VolumeDownTrigger.Prefs.isEnabled(context))
    }
    var waGroupUrl by remember {
        mutableStateOf(WhatsAppDistressSender.Prefs.groupInviteUrl(context) ?: "")
    }
    var waSaveError by remember { mutableStateOf<String?>(null) }
    var waSaveOk by remember { mutableStateOf(false) }

    AppBackdropScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Distress Signal", color = PrimaryTextDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = BrandCyanDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Intro / honest-claim card ---
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, null, tint = BrandAccentDark)
                        Spacer(Modifier.width(10.dp))
                        Text("How it works", color = PrimaryTextDark,
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Fires an SMS with your current location to EVERY trusted contact at once, " +
                            "opens the phone dialer with your first contact pre-dialed, and (if " +
                            "WhatsApp is installed) opens WhatsApp with the same message pre-filled " +
                            "so you can one-tap-send to your emergency group.",
                        color = SecondaryTextDark, fontSize = 12.sp
                    )
                    Text(
                        "OnlyTrusted sends zero traffic itself — everything flows through your own " +
                            "carrier (SMS over GSM, voice over PSTN) and your own WhatsApp account.",
                        color = SecondaryTextDark, fontSize = 11.sp
                    )
                }
            }

            // --- Triggers ---
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Triggers", color = PrimaryTextDark,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    SwitchRow(
                        title = "Shake phone to trigger",
                        subtitle = "5 shakes in 3 seconds. Foreground only — no battery cost when the app is closed.",
                        checked = shakeEnabled,
                        onCheckedChange = {
                            shakeEnabled = it
                            ShakeDetector.Prefs.setEnabled(context, it)
                        }
                    )
                    SwitchRow(
                        title = "Volume-down 4× to trigger",
                        subtitle = "4 presses in 2 seconds. The 4th press is swallowed and does not change volume.",
                        checked = volumeDownEnabled,
                        onCheckedChange = {
                            volumeDownEnabled = it
                            VolumeDownTrigger.Prefs.setEnabled(context, it)
                        }
                    )
                }
            }

            // --- WhatsApp emergency group ---
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("WhatsApp emergency group", color = PrimaryTextDark,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "WhatsApp blocks third-party apps from creating groups or sending to a specific " +
                            "group chat. One-time manual setup instead:\n\n" +
                            "1. Open WhatsApp. Tap New Chat → New Group.\n" +
                            "2. Add every emergency contact; name the group (e.g. \"OnlyTrusted SOS\").\n" +
                            "3. Open the group → tap group name → Invite via link → Share link → copy.\n" +
                            "4. Paste the link below.\n\n" +
                            "When you fire SOS, the link is copied to your clipboard automatically and " +
                            "WhatsApp opens with the distress text pre-filled — pick the group, tap " +
                            "Send, then tap the voice-call icon for a group call.",
                        color = SecondaryTextDark, fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = waGroupUrl,
                        onValueChange = { waGroupUrl = it; waSaveError = null; waSaveOk = false },
                        placeholder = { Text("https://chat.whatsapp.com/…", color = SecondaryTextDark) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    waSaveError?.let { err ->
                        Text(err, color = DangerDark, fontSize = 12.sp)
                    }
                    if (waSaveOk) {
                        Text("Saved.", color = BrandAccentDark, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val ok = WhatsAppDistressSender.Prefs
                                    .setGroupInviteUrl(context, waGroupUrl)
                                if (ok) {
                                    waSaveOk = true
                                    waSaveError = null
                                } else {
                                    waSaveError = "Must start with https://chat.whatsapp.com/ or https://wa.me/."
                                    waSaveOk = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccentDark),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = {
                            // Deep-link into WhatsApp so the user can create the group in situ.
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("whatsapp://")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        }) {
                            Text("Open WhatsApp", color = BrandCyanDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidnightCard,
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = PrimaryTextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SecondaryTextDark, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = BrandCyanDark)
        )
    }
}

