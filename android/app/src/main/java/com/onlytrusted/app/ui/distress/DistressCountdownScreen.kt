// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.distress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.ui.theme.DangerDark
import kotlinx.coroutines.delay

/**
 * Full-screen 3-second cancel countdown shown before the distress signal fires.
 *
 * UX rationale: the SOS FAB is a single tap, so accidental fires (pocket touch, child
 * tap, dropped phone bump) are a real risk. The countdown gives the owner a chance to
 * abort without punishing a real emergency with any extra friction — three seconds is
 * fast enough that an owner in distress tolerates it + long enough that an accidental
 * tap can be reversed. The number counts down VERY visibly in red, dominating the
 * screen so there's no doubt about what's happening.
 *
 * The cancel button is deliberately `OutlinedButton` (less prominent) and the Fire-Now
 * button is tinted the brand danger red — if the owner wants to skip the countdown
 * entirely they can tap Fire Now.
 */
@Composable
fun DistressCountdownOverlay(
    totalSeconds: Int = 3,
    onFire: () -> Unit,
    onCancel: () -> Unit
) {
    var remaining by remember { mutableStateOf(totalSeconds) }
    var firedOrCancelled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (remaining > 0 && !firedOrCancelled) {
            delay(1_000)
            remaining -= 1
        }
        if (!firedOrCancelled) {
            firedOrCancelled = true
            onFire()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = DangerDark,
                modifier = Modifier.size(72.dp)
            )
            Text(
                "Sending EMERGENCY alert",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "SMS to your emergency contacts with your current location, then auto-dial the first one.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                remaining.toString(),
                color = DangerDark,
                fontSize = 92.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (!firedOrCancelled) {
                        firedOrCancelled = true
                        onFire()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DangerDark),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Send now",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = {
                    if (!firedOrCancelled) {
                        firedOrCancelled = true
                        onCancel()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Cancel",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
