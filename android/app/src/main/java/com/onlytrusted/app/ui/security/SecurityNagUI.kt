// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.MidnightCard
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark

/**
 * Inline Dashboard card prompting the user to enable App Lock.
 * Fires on dashboard visits [SecurityNagPolicy.CARD_VISIT_THRESHOLD]+5.
 *
 * Soft nudge: easy to tap away via the [onSnooze] close button.
 */
@Composable
fun SecurityNagCard(onEnable: () -> Unit, onSnooze: () -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightElevated)
            .border(1.dp, BrandAccentDark.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(BrandAccentDark.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        ) {
            Icon(
                Icons.Filled.LockOpen,
                contentDescription = null,
                tint = BrandAccentDark,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Lock OnlyTrusted when you close it",
                color = PrimaryTextDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Face / fingerprint unlock — anyone picking up your phone can see your trusted contacts and audit log otherwise.",
                color = SecondaryTextDark,
                fontSize = 12.sp,
            )
            TextButton(
                onClick = onEnable,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                Text(
                    "Enable App Lock",
                    color = BrandCyanDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        IconButton(onClick = onSnooze, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Not now",
                tint = SecondaryTextDark,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * One-time modal nag that fires on dashboard visit
 * [SecurityNagPolicy.MODAL_VISIT_THRESHOLD]. Interrupts the flow briefly
 * so the user has to make a decision: enable now, snooze, or opt out.
 *
 * No dark patterns: all three options are first-class, dismiss button
 * reachable in one tap, "Never ask again" is explicit text (not hidden).
 */
@Composable
fun SecurityNagModal(
    onEnable: () -> Unit,
    onLater: () -> Unit,
    onNeverAgain: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        containerColor = MidnightCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = BrandAccentDark,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Enable App Lock?",
                    color = PrimaryTextDark,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "You've been using OnlyTrusted without a lock. If someone picks up your phone right now, they can see:",
                    color = PrimaryTextDark,
                    fontSize = 14.sp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BulletLine("Your trusted-contact whitelist")
                    BulletLine("Blocked numbers and the audit log")
                    BulletLine("Recent SOS / Distress Signal activity")
                }
                Text(
                    "App Lock uses your face or fingerprint — nothing to remember, nothing to type. On-device, phishing-proof.",
                    color = SecondaryTextDark,
                    fontSize = 13.sp,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onEnable,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandCyanDark,
                    contentColor = MidnightBackground,
                ),
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text("Enable App Lock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
                    Text("Not now", color = SecondaryTextDark)
                }
                TextButton(onClick = onNeverAgain, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Never ask again",
                        color = SecondaryTextDark,
                        fontSize = 12.sp,
                    )
                }
            }
        },
    )
}

@Composable
private fun BulletLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            "•",
            color = BrandAccentDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(text, color = PrimaryTextDark, fontSize = 13.sp)
    }
}
