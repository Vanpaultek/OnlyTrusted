// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.scamreport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Modal that prompts the user to classify a number as a scam, then persists
 * the entry via [ReportedScamStore]. Keeps a single-select category list —
 * narrow enough that classification is quick, broad enough that most reports
 * fit.
 */
@Composable
fun ReportScamDialog(
    phoneNumber: String,
    normalisedNumber: String,
    onDismiss: () -> Unit,
    onReported: (ReportedScamStore.Category) -> Unit,
) {
    var selected by remember { mutableStateOf(ReportedScamStore.Category.ROBOCALL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightCard,
        title = {
            Text(
                "Report as scam",
                color = PrimaryTextDark,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    phoneNumber,
                    color = PrimaryTextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "The report stays on this device until you choose to share it. Nothing is uploaded. Pick the kind of scam:",
                    color = SecondaryTextDark,
                    fontSize = 12.sp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReportedScamStore.Category.entries.forEach { cat ->
                        CategoryRow(
                            label = cat.label,
                            selected = selected == cat,
                            onSelect = { selected = cat },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onReported(selected) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandCyanDark,
                    contentColor = MidnightBackground,
                ),
            ) {
                Text("Report", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryTextDark)
            }
        },
    )
}

@Composable
private fun CategoryRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) BrandAccentDark.copy(alpha = 0.14f) else MidnightElevated)
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Icon(
            if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) BrandAccentDark else SecondaryTextDark,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = PrimaryTextDark,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
