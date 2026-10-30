// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.contactscan

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.onlytrusted.app.ui.theme.AppBackdropScaffold
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.DangerDark
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.WarningDark
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Scan every system contact's phone numbers against the bundled scam-numbers feed.
 * Local-only, zero network. The user triggers the scan explicitly — we never read the
 * address book until they tap the button.
 *
 * MVP: shows "Scan my contacts" button → requests READ_CONTACTS permission → runs scan
 * → displays total scanned + list of scam matches. Phase 3 adds Dashboard counter
 * updates, weekly WorkManager re-scan, and in-notification alerts on new detections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var scanning by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<ScamScanReport?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            scope.launch {
                scanning = true
                val r = withContext(Dispatchers.IO) { ContactsScamScanner.scan(context) }
                ContactScanState.save(context, r)
                report = r
                scanning = false
            }
        }
    }

    AppBackdropScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Safety Scan", color = PrimaryTextDark) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = BrandCyanDark) }
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
            // Hero + honest-claim
            CardBox {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderRow(Icons.Filled.Contacts, "Scan contacts for scams", BrandAccentDark)
                    Text(
                        "We'll check every phone number in your contacts against our bundled " +
                            "scam-numbers list. Runs on this device only — your contacts are " +
                            "never uploaded or transmitted anywhere.",
                        fontSize = 12.sp, color = SecondaryTextDark
                    )
                    Text(
                        "Catches scam numbers you may have saved from forgotten calls — a " +
                            "common attack path for 'bank fraud alert' and 'tax arrears' scams.",
                        fontSize = 11.sp, color = SecondaryTextDark
                    )
                }
            }

            // Honest disclosure of the current feed size. Hidden once the
            // real feed loads (REAL_FEED_MIN_ENTRIES = 500).
            val feedSize = remember { ContactsScamScanner.loadedFeedSize(context) }
            if (!ContactsScamScanner.isRealFeed(context)) {
                CardBox {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderRow(Icons.Filled.Info, "Sample feed only", DangerDark)
                        Text(
                            "The threat-intel feed shipping in this build is a sample for " +
                                "development: $feedSize entries. The production feed " +
                                "(500,000+ numbers sourced from FTC Consumer Sentinel + FCC + EU " +
                                "regulators) is scheduled for a later release. A \"0 matches\" " +
                                "result right now means your contacts don't match this small " +
                                "sample — NOT that no number in your phone is a scammer.",
                            fontSize = 12.sp, color = SecondaryTextDark
                        )
                    }
                }
            }

            if (!permissionGranted) {
                CardBox {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeaderRow(Icons.Filled.Info, "Permission needed", SecondaryTextDark)
                        Text(
                            "Reading your contacts requires one-time permission. We only use " +
                                "it to compare numbers against the local scam list — nothing is " +
                                "transmitted, stored, or shared.",
                            fontSize = 11.sp, color = SecondaryTextDark
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccentDark)
                        ) {
                            Text("Grant Contacts access", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else if (scanning) {
                CardBox {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                            color = BrandAccentDark
                        )
                        Text("Scanning your contacts…", fontSize = 13.sp, color = PrimaryTextDark)
                    }
                }
            } else {
                val existingReport = report
                if (existingReport == null) {
                    CardBox {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Ready to scan.", fontSize = 13.sp, color = SecondaryTextDark)
                            Button(
                                onClick = {
                                    scope.launch {
                                        scanning = true
                                        val r = withContext(Dispatchers.IO) {
                                            ContactsScamScanner.scan(context)
                                        }
                                        ContactScanState.save(context, r)
                                        report = r
                                        scanning = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandAccentDark)
                            ) {
                                Text("Scan my contacts", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    // Summary card
                    CardBox {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val hitColor = if (existingReport.matches.isEmpty()) BrandAccentDark else DangerDark
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    if (existingReport.matches.isEmpty()) Icons.Filled.Info else Icons.Filled.Warning,
                                    null, tint = hitColor
                                )
                                Text(
                                    if (existingReport.matches.isEmpty())
                                        "No scam numbers found in your contacts"
                                    else
                                        "${existingReport.matches.size} potential scam number(s) found",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = hitColor
                                )
                            }
                            Text(
                                "Scanned ${existingReport.contactsScanned} contact(s), " +
                                    "${existingReport.numbersScanned} phone number(s).",
                                fontSize = 11.sp, color = SecondaryTextDark
                            )
                            // Honest transparency: tell the user why we didn't
                            // flag their 1,400 home-country contacts as scams.
                            if (existingReport.homeCountryCode != null && existingReport.homeCountrySkipped > 0) {
                                Text(
                                    "Skipped ${existingReport.homeCountrySkipped} match(es) that only fired because of your home country code ${existingReport.homeCountryCode}. Specific scam ranges inside your country still show.",
                                    fontSize = 11.sp, color = SecondaryTextDark
                                )
                            }
                            if (existingReport.trustedSkipped > 0) {
                                Text(
                                    "Skipped ${existingReport.trustedSkipped} contact(s) already in your Trusted vault.",
                                    fontSize = 11.sp, color = SecondaryTextDark
                                )
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    scanning = true
                                    val r = withContext(Dispatchers.IO) {
                                        ContactsScamScanner.scan(context)
                                    }
                                    ContactScanState.save(context, r)
                                    report = r
                                    scanning = false
                                }
                            }) { Text("Re-scan", color = BrandCyanDark) }
                        }
                    }

                    // Matches list
                    var reportTarget by remember { mutableStateOf<ContactsScamScanner.ScamMatch?>(null) }
                    existingReport.matches.forEach { match ->
                        val tint = when (match.severity) {
                            ContactsScamScanner.Severity.CRITICAL -> DangerDark
                            ContactsScamScanner.Severity.HIGH -> WarningDark
                            ContactsScamScanner.Severity.MEDIUM -> SecondaryTextDark
                        }
                        val heading = when (match.matchType) {
                            ContactsScamScanner.MatchType.KNOWN_SCAM -> "Documented scam"
                            ContactsScamScanner.MatchType.SUSPICIOUS_PATTERN -> "Suspicious pattern"
                        }
                        CardBox {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Warning, null, tint = tint, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(match.contactName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PrimaryTextDark)
                                    Spacer(Modifier.weight(1f))
                                    Text(heading, fontSize = 10.sp, color = tint, fontWeight = FontWeight.Bold)
                                }
                                Text(match.phoneNumber, fontSize = 12.sp, color = SecondaryTextDark)
                                Text(match.label, fontSize = 10.sp, color = SecondaryTextDark)
                                TextButton(
                                    onClick = { reportTarget = match },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        "+ Report as scam",
                                        color = BrandAccentDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                    reportTarget?.let { target ->
                        com.onlytrusted.app.scamreport.ReportScamDialog(
                            phoneNumber = target.phoneNumber,
                            normalisedNumber = target.normalisedNumber,
                            onDismiss = { reportTarget = null },
                            onReported = { category ->
                                com.onlytrusted.app.scamreport.ReportedScamStore.add(
                                    context, target.normalisedNumber, category,
                                )
                                reportTarget = null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(icon: ImageVector, title: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(10.dp), color = tint) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.padding(8.dp))
        }
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PrimaryTextDark)
    }
}

@Composable
private fun CardBox(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidnightElevated,
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}
