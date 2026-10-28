// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.privacy.OnboardingState
import com.onlytrusted.app.ui.sensorprivacy.BatteryOptimizationHelper
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.MidnightCard
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark
import com.onlytrusted.app.ui.theme.TertiaryTextDark

/**
 * One-screen review of every permission OnlyTrusted uses, with per-row
 * checkboxes and a single "Grant selected" button that batches the system
 * requests via [ActivityResultContracts.RequestMultiplePermissions].
 *
 * ### Honest limit
 *
 * Android does NOT allow a single system dialog that covers all
 * permissions. Each dangerous permission is its own system-shown prompt
 * by design. What we can do — and this screen does — is give the user a
 * complete pre-approval review so the sequence of Android prompts that
 * follows is not a surprise. Special permissions (battery optimisation
 * exemption, device admin, accessibility) are deep-links to Settings,
 * handled in their respective feature screens; this overview focuses on
 * the runtime permission bucket.
 *
 * The user can un-check anything they don't want to grant, or hit
 * "Skip for now" to bypass the batch entirely. Features that need a
 * specific permission still prompt on their own when the user first
 * taps into them — this screen is a convenience, not a gate.
 */
@Composable
fun PermissionsOverviewScreen(onContinue: () -> Unit) {
    val context = LocalContext.current

    val rows = remember { buildPermissionRows() }
    val selected = remember {
        mutableStateMapOf<String, Boolean>().apply {
            rows.forEach { put(it.key, true) }
        }
    }
    var grantedSnapshot by remember {
        mutableStateOf(rows.associate { it.key to it.isGranted(context) })
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        OnboardingState.markPermissionsReviewed(context)
        onContinue()
    }

    // Refresh "already granted" status on each entry — covers the case where
    // the user flipped a permission in Settings and came back.
    LaunchedEffect(Unit) {
        grantedSnapshot = rows.associate { it.key to it.isGranted(context) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 20.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Header()
            rows.forEach { row ->
                val granted = grantedSnapshot[row.key] == true
                PermissionRow(
                    row = row,
                    checked = selected[row.key] == true,
                    alreadyGranted = granted,
                    onCheckedChange = { selected[row.key] = it },
                )
            }
            // Non-runtime "special" permissions. Linked to Settings
            // deep-links; cannot be included in the system batch.
            SpecialPermissionsSection(context)
            HonestLimitFootnote()
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MidnightBackground)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    val toRequest = rows
                        .filter { selected[it.key] == true }
                        .flatMap { it.androidKeys() }
                        .filterNot { PermissionsOverviewScreenInternal.has(context, it) }
                        .toTypedArray()
                    if (toRequest.isEmpty()) {
                        OnboardingState.markPermissionsReviewed(context)
                        onContinue()
                    } else {
                        launcher.launch(toRequest)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandCyanDark,
                    contentColor = MidnightBackground,
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(
                    "Grant selected",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            TextButton(
                onClick = {
                    OnboardingState.markPermissionsReviewed(context)
                    onContinue()
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text(
                    "Skip for now",
                    color = SecondaryTextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// --- Rows + section composables -------------------------------------------

@Composable
private fun Header() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
    ) {
        Text(
            "App permissions",
            color = PrimaryTextDark,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            "Review everything OnlyTrusted uses, then grant them all in one " +
                "step. Nothing leaves your device. You can un-check anything " +
                "you don't want, or skip and grant later.",
            color = SecondaryTextDark,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PermissionRow(
    row: PermissionRowSpec,
    checked: Boolean,
    alreadyGranted: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val effectiveChecked = alreadyGranted || checked
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightElevated)
            .border(1.dp, BrandCyanDark.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(BrandCyanDark.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
        ) {
            Icon(
                row.icon,
                contentDescription = null,
                tint = BrandCyanDark,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                row.title,
                color = PrimaryTextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                row.rationale,
                color = SecondaryTextDark,
                fontSize = 12.sp,
            )
            if (alreadyGranted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = BrandCyanDark,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Already granted",
                        color = BrandCyanDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        TextButton(
            onClick = {
                if (!alreadyGranted) onCheckedChange(!checked)
            },
            enabled = !alreadyGranted,
        ) {
            Icon(
                if (effectiveChecked) Icons.Outlined.CheckBox
                else Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = if (effectiveChecked) "Selected" else "Not selected",
                tint = if (effectiveChecked) BrandAccentDark else SecondaryTextDark,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun SpecialPermissionsSection(context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Special permissions (optional)",
            color = PrimaryTextDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 10.dp, start = 4.dp),
        )
        Text(
            "These live in Settings and can't be requested through the normal " +
                "prompt. Grant them later if you enable the feature that needs them.",
            color = TertiaryTextDark,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        )
        SpecialPermissionRow(
            icon = Icons.Filled.BatteryChargingFull,
            title = "Battery optimisation exemption",
            rationale = "Keeps the Mic & Camera Lock service running after you " +
                "lock the screen. Only needed if you enable that feature.",
            onOpenSettings = { BatteryOptimizationHelper.requestExemption(context) },
        )
        SpecialPermissionRow(
            icon = Icons.Filled.Notifications,
            title = "App Notifications",
            rationale = "If you denied the notifications prompt, you can re-enable " +
                "them here. Required for lock-service and SOS confirmations.",
            onOpenSettings = {
                runCatching {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    context.startActivity(intent)
                }
            },
        )
    }
}

@Composable
private fun SpecialPermissionRow(
    icon: ImageVector,
    title: String,
    rationale: String,
    onOpenSettings: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightCard)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = SecondaryTextDark,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = PrimaryTextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(rationale, color = SecondaryTextDark, fontSize = 11.sp)
        }
        TextButton(onClick = onOpenSettings) {
            Text("Open", color = BrandAccentDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = BrandAccentDark)
        }
    }
}

@Composable
private fun HonestLimitFootnote() {
    Text(
        "Android shows one system dialog per permission — we can't combine " +
            "them into a single \"Allow all\" tap. We can only line them up for " +
            "you in advance. Expect that many Allow / Deny prompts after you " +
            "tap Grant selected.",
        color = TertiaryTextDark,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
    )
}

// --- Row specs ------------------------------------------------------------

private data class PermissionRowSpec(
    val key: String,
    val title: String,
    val rationale: String,
    val icon: ImageVector,
    val required: Boolean = true,
    val sdkMin: Int = 1,
    val androidKey: String,
    val extraAndroidKeys: List<String> = emptyList(),
) {
    fun androidKeys(): List<String> = listOf(androidKey) + extraAndroidKeys
    fun isGranted(context: Context): Boolean =
        androidKeys().all { PermissionsOverviewScreenInternal.has(context, it) }
}

private fun buildPermissionRows(): List<PermissionRowSpec> = buildList {
    add(
        PermissionRowSpec(
            key = "contacts",
            title = "Contacts",
            rationale = "Needed to build your trusted-contacts whitelist (who can " +
                "call and text you) and your Emergency contact for SOS.",
            icon = Icons.Filled.Contacts,
            androidKey = Manifest.permission.READ_CONTACTS,
        )
    )
    add(
        PermissionRowSpec(
            key = "phone",
            title = "Phone and call log",
            rationale = "Needed to see who called, block spam numbers, and show " +
                "the audit log of recent calls.",
            icon = Icons.Filled.Phone,
            androidKey = Manifest.permission.READ_PHONE_STATE,
            extraAndroidKeys = listOf(Manifest.permission.READ_CALL_LOG),
        )
    )
    add(
        PermissionRowSpec(
            key = "sms",
            title = "SMS",
            rationale = "Needed to filter and block spam SMS, and to send the " +
                "SOS alert text when you trigger Distress Signal.",
            icon = Icons.Filled.Sms,
            androidKey = Manifest.permission.RECEIVE_SMS,
            extraAndroidKeys = listOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
            ),
        )
    )
    add(
        PermissionRowSpec(
            key = "mic",
            title = "Microphone",
            rationale = "Needed for calls, SOS voice recording, and the Mic & " +
                "Camera Lock feature.",
            icon = Icons.Filled.Mic,
            androidKey = Manifest.permission.RECORD_AUDIO,
        )
    )
    add(
        PermissionRowSpec(
            key = "camera",
            title = "Camera",
            rationale = "Needed for the QR scanner (Smart Link Guard) and the " +
                "optional Face-Change Shoulder-Surfing Lock.",
            icon = Icons.Filled.CameraAlt,
            androidKey = Manifest.permission.CAMERA,
        )
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(
            PermissionRowSpec(
                key = "notifications",
                title = "Notifications",
                rationale = "Needed to show lock-service, Distress Signal, and " +
                    "mic-activity alerts.",
                icon = Icons.Filled.Notifications,
                sdkMin = Build.VERSION_CODES.TIRAMISU,
                androidKey = Manifest.permission.POST_NOTIFICATIONS,
            )
        )
    }
}

/** Package-private helper so the composable file can call into the Context
 *  without pulling the whole `androidx.core.content` stack into the top of
 *  the composable namespace. */
private object PermissionsOverviewScreenInternal {
    fun has(context: Context, permission: String): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}

