// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.scamreport

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
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
 * Settings → Device & data → "My scam reports". Lists every entry the user
 * has added via [ReportScamDialog], with per-row delete + full-set share +
 * clear-all. Nothing is uploaded automatically; share-sheet is the only
 * egress and user chooses the destination (email, GitHub issue, files, etc.).
 */
@Composable
fun ScamReportsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var reports by remember { mutableStateOf(ReportedScamStore.list(context)) }
    var confirmClearAll by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        TopBar(onBack = onBack)
        if (reports.isEmpty()) {
            EmptyState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IntroCard(count = reports.size)
                reports.forEach { r ->
                    ReportRow(
                        report = r,
                        onDelete = {
                            ReportedScamStore.remove(context, r.normalisedNumber)
                            reports = ReportedScamStore.list(context)
                        },
                    )
                }
                ActionButtons(
                    context = context,
                    onClearAll = { confirmClearAll = true },
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            containerColor = MidnightCard,
            title = { Text("Delete all scam reports?", color = PrimaryTextDark, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This removes every report from this device. If you haven't shared them yet, they're gone.",
                    color = SecondaryTextDark,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        ReportedScamStore.clearAll(context)
                        reports = ReportedScamStore.list(context)
                        confirmClearAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerDark),
                ) {
                    Text("Delete all", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) {
                    Text("Cancel", color = SecondaryTextDark)
                }
            },
        )
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
            "My scam reports",
            color = PrimaryTextDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun IntroCard(count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightCard)
            .border(1.dp, BrandAccentDark.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "$count report(s) on this device",
            color = PrimaryTextDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Nothing is uploaded automatically. Use \"Share\" below to send a markdown or CSV to our GitHub repo, email the maintainer, or export to Files.",
            color = SecondaryTextDark,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(60.dp))
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = SecondaryTextDark,
            modifier = Modifier.size(44.dp),
        )
        Text(
            "No scam reports yet",
            color = PrimaryTextDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Long-press a contact or tap \"Report as scam\" on any Scam Contacts result to add one. Reports stay local until you choose to share them.",
            color = SecondaryTextDark,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun ReportRow(report: ReportedScamStore.ReportedScam, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightElevated)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(report.normalisedNumber, color = PrimaryTextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(report.category.label, color = SecondaryTextDark, fontSize = 11.sp)
            Text(
                java.text.DateFormat.getDateInstance().format(java.util.Date(report.reportedAtMs)),
                color = TertiaryTextDark,
                fontSize = 10.sp,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DangerDark, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ActionButtons(context: Context, onClearAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = {
                val text = ReportedScamStore.formatForGitHub(context)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "OnlyTrusted — Scam report submission")
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "Share scam reports"))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandCyanDark,
                contentColor = MidnightBackground,
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Share as Markdown (GitHub-ready)", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = {
                val text = ReportedScamStore.formatAsCsv(context)
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("OnlyTrusted scam reports CSV", text))
                Toast.makeText(context, "CSV copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = BrandAccentDark, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Copy as CSV", color = BrandAccentDark, fontWeight = FontWeight.SemiBold)
        }
        TextButton(
            onClick = onClearAll,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = DangerDark, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Delete all reports", color = DangerDark)
        }
    }
}
