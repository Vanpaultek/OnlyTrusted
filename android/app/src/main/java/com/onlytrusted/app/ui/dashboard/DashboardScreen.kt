// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.onlytrusted.app.localization.stringResourceReviewed
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.onlytrusted.app.OnlyTrustedApp
import com.onlytrusted.app.R
import com.onlytrusted.app.data.db.entities.SecurityEvent
import com.onlytrusted.app.ui.safety.HomeBannerPrefs
import com.onlytrusted.app.ui.safety.PanicContact
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.BrandPrimaryTeal
import com.onlytrusted.app.ui.theme.DangerDark
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.MidnightCard
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark
import com.onlytrusted.app.ui.theme.TertiaryTextDark
import com.onlytrusted.app.ui.theme.WarningDark
import java.util.Calendar

@Composable
fun DashboardScreen(
    onPanic: () -> Unit = {},
    onGoToVault: () -> Unit = {},
    onGoToSettings: () -> Unit = {},
    onGoToBlockList: () -> Unit = {},
    onGoToEvents: () -> Unit = {},
    onGoToReports: () -> Unit = {},
    onGoToContactScan: () -> Unit = {},
) {
    val duressActive by com.onlytrusted.app.ui.duress.DuressState.isDuressSession
        .collectAsStateWithLifecycle()
    var confirmDuressExit by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }
    val context = LocalContext.current
    val app = context.applicationContext as OnlyTrustedApp
    val vm: DashboardViewModel = viewModel(factory = viewModelFactory {
        initializer { DashboardViewModel(app) }
    })
    val state by vm.state.collectAsStateWithLifecycle()
    val runtime by vm.runtimeState.collectAsStateWithLifecycle()
    var showHonestLimitsSheet by remember { mutableStateOf(false) }
    var showNoContactAlert by remember { mutableStateOf(false) }
    // Security Lock sheet state — opens from the Dashboard card below the quick
    // actions. Edits persist via AppLockManager + AutoLockPrefs so SettingsScreen
    // stays in sync automatically.
    var showSecurityLockSheet by remember { mutableStateOf(false) }
    var showSensorPrivacyScreen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Distress Signal: the countdown overlay + dispatcher + result dialog all live at
    // MainActivity level so they work above any screen (triggered by SOS button, shake
    // from MainActivity, or volume-down 4× from MainActivity.onKeyDown). This Dashboard
    // just publishes to DistressTriggerBus when the user taps the FAB.

    // F4 weekly digest + F5 refresher state.
    var showWeeklyDigest by remember { mutableStateOf(false) }
    var showHonestLimitsRefresher by remember { mutableStateOf(false) }
    var weeklyCallsBlocked by remember { mutableStateOf(0) }
    var weeklyTextsBlocked by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        vm.refreshRuntime()
        vm.verifyAuditChain()
        if (HomeBannerPrefs.shouldShowWeeklyDigest(context)) {
            // Compute from EventStore for the last 7 days.
            val weekAgo = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
            val events = app.database.eventDao().recent(limit = 10_000).first()
                .filter { it.timestamp >= weekAgo }
            weeklyCallsBlocked = events.count { it.type == SecurityEvent.TYPE_CALL_BLOCKED }
            weeklyTextsBlocked = events.count { it.type == SecurityEvent.TYPE_SMS_FLAGGED }
            showWeeklyDigest = true
        }
        if (HomeBannerPrefs.shouldShowHonestLimitsRefresher(context)) {
            showHonestLimitsRefresher = true
        }
    }
    LaunchedEffect(state.trustedContactCount) {
        vm.setHasAnyTrustedContact(state.trustedContactCount > 0)
    }

    val hasTrustedContacts = state.trustedContactCount > 0
    val trustedOnlyEnabled = runtime.trustedOnlyCalls
    val protectionActive = hasTrustedContacts && trustedOnlyEnabled

    Box(modifier = Modifier.fillMaxSize().background(MidnightBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CinematicHeader_Unused(name = null, onSettings = onGoToSettings)

            // F4 — Weekly digest banner (Monday first open).
            if (showWeeklyDigest) {
                WeeklyDigestBanner(
                    callCount = weeklyCallsBlocked,
                    textCount = weeklyTextsBlocked,
                    onDismiss = {
                        showWeeklyDigest = false
                        HomeBannerPrefs.markWeeklyDigestShown(context)
                    },
                    onSeeReport = {
                        showWeeklyDigest = false
                        HomeBannerPrefs.markWeeklyDigestShown(context)
                        onGoToReports()
                    }
                )
            }

            // F5 — Monthly Honest Limits refresher.
            if (showHonestLimitsRefresher) {
                HonestLimitsRefresherBanner(
                    onLearnMore = {
                        showHonestLimitsRefresher = false
                        HomeBannerPrefs.markHonestLimitsRefresherShown(context)
                        showHonestLimitsSheet = true
                    },
                    onDismiss = {
                        showHonestLimitsRefresher = false
                        HomeBannerPrefs.markHonestLimitsRefresherShown(context)
                    }
                )
            }

            Box(
                modifier = Modifier.clickable {
                    // Hidden triple-tap panic exit for Duress Mode. Only reacts while a duress
                    // session is live; otherwise this behaves like a no-op decorative shell.
                    if (!duressActive) return@clickable
                    val now = System.currentTimeMillis()
                    tapCount = if (now - lastTapAt < 600) tapCount + 1 else 1
                    lastTapAt = now
                    if (tapCount >= 3) {
                        tapCount = 0
                        confirmDuressExit = true
                    }
                }
            ) {
                ProtectionActiveCard(
                    isActive = protectionActive,
                    awaitingToggle = hasTrustedContacts && !trustedOnlyEnabled
                )
            }
            // Live match count — recomposes when the auto-import+scan below
            // flips the stored value.
            var scamContactsCount by remember {
                mutableStateOf(com.onlytrusted.app.ui.contactscan.ContactScanState.matchCount(context))
            }

            // First-time auto-import of device contacts + scam scan. Fires
            // exactly once per install (gated by TrustedContactsAutoImporter's
            // own prefs flag), only after READ_CONTACTS has been granted.
            // Mirrors the WhatsApp first-launch "we found your contacts"
            // behaviour. All reads + inserts are local — nothing transmitted.
            LaunchedEffect(Unit) {
                if (com.onlytrusted.app.contactscan.TrustedContactsAutoImporter.shouldRun(context)) {
                    runCatching {
                        com.onlytrusted.app.contactscan.TrustedContactsAutoImporter.importAll(context)
                    }
                    val report = runCatching {
                        com.onlytrusted.app.ui.contactscan.ContactsScamScanner.scan(context)
                    }.getOrNull()
                    if (report != null) {
                        com.onlytrusted.app.ui.contactscan.ContactScanState.save(context, report)
                        scamContactsCount = report.matches.size
                    }
                }
            }

            StatsRow(
                trusted = state.trustedContactCount,
                blockedToday = state.blockedCalls24h,
                blockedWeek = state.flaggedSms24h,
                scamContacts = scamContactsCount,
                onTrustedClick = onGoToVault,
                onBlockedClick = onGoToBlockList,
                onScamClick = onGoToContactScan,
            )
            QuickActionsSection(
                onAddContact = onGoToVault,
                onBlockNumber = onGoToBlockList,
                onViewLog = onGoToEvents
            )
            SecurityLockEntryCard(onClick = { showSecurityLockSheet = true })
            HonestLimitsCallout(onTap = { showHonestLimitsSheet = true })

            // --- Android-only advanced protections (preserved) ---
            androidx.compose.material3.Card { TrustedModeContent(vm, state, runtime, onGoToVault) }
            androidx.compose.material3.Card { SensorOwnershipContent(context, runtime) }
        }

        // F1 — Panic quick-dial floating button, bottom-right above the bottom nav.
        FloatingActionButton(
            onClick = {
                val list = runtime.contactNumbers // may be empty
                if (list.isEmpty()) {
                    showNoContactAlert = true
                } else {
                    com.onlytrusted.app.ui.distress.DistressTriggerBus.fire()
                }
            },
            containerColor = DangerDark,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 24.dp)
                .size(64.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Phone, contentDescription = "Panic — call trusted contact", modifier = Modifier.size(22.dp))
                Text("SOS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showNoContactAlert) {
        AlertDialog(
            onDismissRequest = { showNoContactAlert = false },
            title = { Text(stringResource(R.string.dash_sos_add_contact_title), color = PrimaryTextDark) },
            text = {
                Text(
                    stringResourceReviewed(R.string.dash_sos_add_contact_body),
                    color = SecondaryTextDark
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNoContactAlert = false
                    onGoToVault()
                }) { Text(stringResource(R.string.dash_open_contacts), color = BrandAccentDark) }
            },
            dismissButton = {
                TextButton(onClick = { showNoContactAlert = false }) {
                    Text(stringResource(R.string.lock_cancel))
                }
            },
            containerColor = MidnightCard
        )
    }

    if (confirmDuressExit) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDuressExit = false },
            title = { Text(stringResourceReviewed(R.string.dash_exit_duress_title), color = PrimaryTextDark, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResourceReviewed(R.string.dash_exit_duress_body),
                    color = SecondaryTextDark
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDuressExit = false
                    com.onlytrusted.app.ui.duress.DuressState.setDuressSession(false)
                    onPanic()
                }) { Text(stringResource(R.string.dash_exit), color = DangerDark) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDuressExit = false }) {
                    Text(stringResource(R.string.lock_cancel), color = SecondaryTextDark)
                }
            },
            containerColor = MidnightCard
        )
    }

    if (showSecurityLockSheet) {
        com.onlytrusted.app.ui.sensorprivacy.SecurityLockGroupSheet(
            appLockInitiallyEnabled = com.onlytrusted.app.security.AppLockManager.isEnabled(context),
            onAppLockChanged = {
                com.onlytrusted.app.security.AppLockManager.setEnabled(context, it)
            },
            onOpenMicCameraLock = {
                showSecurityLockSheet = false
                showSensorPrivacyScreen = true
            },
            onDismiss = { showSecurityLockSheet = false },
        )
    }
    if (showSensorPrivacyScreen) {
        com.onlytrusted.app.ui.sensorprivacy.SensorPrivacyScreen(
            onBack = { showSensorPrivacyScreen = false }
        )
    }

    if (showHonestLimitsSheet) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHonestLimitsSheet = false },
            title = { Text(stringResource(R.string.dash_honest_limits), color = PrimaryTextDark, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HonestLimitBullet("Monitor other apps' microphone or camera use.")
                    HonestLimitBullet("Stop recordings made on another device.")
                    HonestLimitBullet("Full screenshot blocking across the whole phone needs a hardened OS or root.")
                    HonestLimitBullet("Prevent carrier or operating-system tracking.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHonestLimitsSheet = false }) {
                    Text(stringResource(R.string.dash_done), color = BrandAccentDark)
                }
            },
            containerColor = MidnightCard
        )
    }
}

// MARK: - Cinematic header (lakescape backdrop + wordmark + settings + greeting)

@Composable
private fun CinematicHeader_Unused(name: String?, onSettings: () -> Unit) {
    val headerHeight = 240.dp
    val greeting: String = run {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val key = when {
            h < 12 -> R.string.dash_good_morning
            h < 18 -> R.string.dash_good_afternoon
            else -> R.string.dash_good_evening
        }
        stringResource(key)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Lakescape backdrop (reuse SplashHero PNG — anchor top so baked wordmark stays
        // off-screen below the clipped frame; dark gradient overlay preserves greeting
        // legibility.)
        Image(
            painter = painterResource(id = R.drawable.splash_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MidnightBackground.copy(alpha = 0.35f),
                            MidnightBackground.copy(alpha = 0.70f),
                            MidnightBackground
                        )
                    )
                )
        )

        // Top row: wordmark + settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val wordmark = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) { append("Only") }
                withStyle(SpanStyle(color = BrandCyanDark)) { append("Trusted") }
            }
            Text(
                text = wordmark,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, BrandCyanDark.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Greeting anchored bottom-left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "$greeting,",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name ?: stringResource(R.string.dash_welcome),
                    color = BrandCyanDark,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text("👋", fontSize = 22.sp)
            }
        }
    }
}

// MARK: - Protection active card (previous version)

@Composable
private fun ProtectionActiveCard(isActive: Boolean, awaitingToggle: Boolean = false) {
    // Three distinct states:
    //   isActive=true  → contacts + "Silence calls not in vault" both on
    //   awaitingToggle → at least one contact added, but the toggle is still off
    //   else           → no contacts yet
    // Previously the subtitle was always "Add trusted contacts to begin.", which was
    // misleading after the user added a contact (the real blocker became the toggle,
    // not the contact).
    val headline = when {
        isActive -> stringResource(R.string.dash_protection_active)
        awaitingToggle -> stringResource(R.string.dash_almost_there)
        else -> stringResource(R.string.dash_setup_needed)
    }
    val subtitle = when {
        isActive -> stringResource(R.string.dash_protection_active_detail)
        awaitingToggle -> stringResource(R.string.dash_awaiting_toggle_detail)
        else -> stringResource(R.string.dash_setup_needed_detail)
    }
    val emphasised = isActive || awaitingToggle

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightElevated)
            .border(
                1.dp,
                BrandCyanDark.copy(alpha = if (emphasised) 0.4f else 0.15f),
                RoundedCornerShape(20.dp)
            )
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Full OnlyTrusted brand-logo tile (shield + wordmark on midnight bg with
        // baked-in cyan ring + sparkles). The asset already carries its own ring
        // treatment, so we drop the extra Circle border/glow that used to wrap the
        // glyph and just add a soft cyan drop shadow when protection is fully active.
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(
                id = com.onlytrusted.app.R.drawable.brand_logo
            ),
            contentDescription = "OnlyTrusted protection shield",
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(18.dp))
                .shadow(
                    elevation = if (isActive) 14.dp else 0.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = BrandCyanDark,
                    spotColor = BrandCyanDark
                ),
            alpha = when {
                isActive -> 1.0f
                awaitingToggle -> 0.85f
                else -> 0.55f
            }
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                headline,
                color = BrandCyanDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                color = SecondaryTextDark,
                fontSize = 14.sp
            )
        }

        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TertiaryTextDark,
            modifier = Modifier.size(18.dp)
        )
    }
}

// MARK: - 3 stat tiles

@Composable
private fun StatsRow(
    trusted: Int,
    blockedToday: Int,
    blockedWeek: Int,
    scamContacts: Int,
    onTrustedClick: () -> Unit,
    onBlockedClick: () -> Unit,
    onScamClick: () -> Unit,
) {
    // 1x3 row: Trusted Contacts · Blocked (today + week combined) · Scam Contacts.
    // Each tile is tappable — routes to the matching list screen.
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        StatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Groups,
            tint = BrandAccentDark,
            value = trusted,
            label = stringResource(R.string.dash_trusted_contacts),
            onClick = onTrustedClick,
        )
        BlockedCombinedTile(
            modifier = Modifier.weight(1f),
            blockedToday = blockedToday,
            blockedWeek = blockedWeek,
            onClick = onBlockedClick,
        )
        // Scam Contacts tile — populated by the last Contact Safety Scan.
        StatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Warning,
            tint = if (scamContacts > 0) DangerDark else BrandAccentDark,
            value = scamContacts,
            label = stringResource(R.string.dash_scam_contacts),
            onClick = onScamClick,
        )
    }
}

/**
 * Combined "Blocked" tile that stacks today's count prominently with the 7-day total
 * underneath. Replaces the former pair of Blocked Today + Blocked This Week tiles so
 * the dashboard reads 1x3 instead of 2x2.
 */
@Composable
private fun BlockedCombinedTile(
    modifier: Modifier,
    blockedToday: Int,
    blockedWeek: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightElevated)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(DangerDark.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Filled.Block, null, tint = DangerDark, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(blockedToday.toString(), color = DangerDark, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.dash_blocked), color = PrimaryTextDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(R.string.dash_today_week_fmt, blockedWeek),
            color = SecondaryTextDark,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier,
    icon: ImageVector,
    tint: Color,
    value: Int,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightElevated)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(tint.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                value.toString(),
                color = tint,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(label, color = SecondaryTextDark, fontSize = 12.sp)
    }
}

// MARK: - Quick actions

@Composable
private fun QuickActionsSection(
    onAddContact: () -> Unit,
    onBlockNumber: () -> Unit,
    onViewLog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.dash_quick_actions),
            color = PrimaryTextDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.PersonAdd,
                title = stringResource(R.string.dash_qa_emergency_title),
                subtitle = stringResource(R.string.dash_qa_emergency_subtitle),
                tint = BrandAccentDark,
                onClick = onAddContact
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CallEnd,
                title = stringResource(R.string.dash_qa_block_title),
                subtitle = stringResource(R.string.dash_qa_block_subtitle),
                tint = DangerDark,
                onClick = onBlockNumber
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Description,
                title = stringResource(R.string.dash_qa_log_title),
                subtitle = stringResource(R.string.dash_qa_log_subtitle),
                tint = BrandCyanDark,
                onClick = onViewLog
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MidnightElevated,
        modifier = modifier.heightIn(min = 150.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .background(tint.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Text(
                title,
                color = PrimaryTextDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                color = SecondaryTextDark,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

// MARK: - Security Lock entry card

@Composable
private fun SecurityLockEntryCard(onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = BrandAccentDark.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandAccentDark.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(BrandAccentDark.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
            ) {
                Icon(Icons.Filled.Lock, null, tint = BrandAccentDark, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Security Lock",
                     color = PrimaryTextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("App Lock, auto-lock timer, and Mic & Camera Lock in one place.",
                     color = SecondaryTextDark, fontSize = 12.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = SecondaryTextDark, modifier = Modifier.size(16.dp))
        }
    }
}

// MARK: - Honest Limits callout (restored)

@Composable
private fun HonestLimitsCallout(onTap: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onTap,
        shape = RoundedCornerShape(18.dp),
        color = WarningDark.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningDark.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(WarningDark.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningDark, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.dash_honest_limits), color = WarningDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResourceReviewed(R.string.dash_tap_see_limits),
                    color = SecondaryTextDark,
                    fontSize = 13.sp
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = WarningDark.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun HonestLimitBullet(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(5.dp)
                .background(WarningDark, CircleShape)
        )
        Text(text, color = PrimaryTextDark, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

// MARK: - F4 Weekly digest banner

@Composable
private fun WeeklyDigestBanner(
    callCount: Int,
    textCount: Int,
    onDismiss: () -> Unit,
    onSeeReport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightElevated)
            .border(1.dp, BrandCyanDark.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .heightIn(min = 92.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(BrandCyanDark.copy(alpha = 0.18f), RoundedCornerShape(9.dp))
        ) {
            Icon(Icons.Filled.BarChart, contentDescription = null, tint = BrandCyanDark, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.dash_week_title),
                color = PrimaryTextDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (callCount == 0 && textCount == 0)
                    stringResource(R.string.dash_week_empty)
                else
                    stringResource(R.string.dash_week_fmt, callCount, textCount),
                color = SecondaryTextDark,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onSeeReport, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text(stringResource(R.string.dash_see_full_report), color = BrandCyanDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text(stringResource(R.string.dash_dismiss), color = SecondaryTextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// MARK: - F5 Honest Limits monthly refresher banner

@Composable
private fun HonestLimitsRefresherBanner(
    onLearnMore: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WarningDark.copy(alpha = 0.08f))
            .border(1.dp, WarningDark.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .heightIn(min = 92.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(WarningDark.copy(alpha = 0.18f), RoundedCornerShape(9.dp))
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = WarningDark, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.dash_monthly_reminder),
                color = PrimaryTextDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResourceReviewed(R.string.dash_cannot_monitor_other_apps),
                color = SecondaryTextDark,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onLearnMore, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text(stringResource(R.string.dash_learn_more), color = WarningDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text(stringResource(R.string.dash_dismiss), color = SecondaryTextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// MARK: - Android-only extras (preserved from previous dashboard)

@Composable
private fun TrustedModeContent(
    vm: DashboardViewModel,
    state: DashboardState,
    runtime: DashboardViewModel.RuntimeState,
    onManageVault: () -> Unit
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = BrandAccentDark)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.dash_trusted_only_mode),
                color = PrimaryTextDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onManageVault) { Text("Vault →", color = BrandAccentDark) }
        }
        Text("${state.trustedContactCount} trusted contact(s).", color = SecondaryTextDark, fontSize = 13.sp)
        SwitchRow(stringResource(R.string.dash_silence_calls_not_vault), runtime.trustedOnlyCalls, vm::setTrustedOnlyCalls)
        SwitchRow(stringResource(R.string.dash_flag_sms_not_vault), runtime.trustedOnlySms, vm::setTrustedOnlySms)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = PrimaryTextDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SensorOwnershipContent(
    context: android.content.Context,
    runtime: DashboardViewModel.RuntimeState
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Verified, contentDescription = null, tint = BrandAccentDark)
            Spacer(Modifier.width(8.dp))
            Text(
                "Mic & camera ownership",
                color = PrimaryTextDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        SensorStatus("Mic", runtime.sensorPrivacy.micMuted)
        SensorStatus("Camera", runtime.sensorPrivacy.cameraMuted)
        TextButton(onClick = {
            val intent = Intent(Settings.ACTION_PRIVACY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }) {
            Text("Open system Privacy settings", color = BrandAccentDark)
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = BrandAccentDark)
        }
    }
}

@Composable
private fun SensorStatus(label: String, muted: Boolean?) {
    val (text, tint) = when (muted) {
        true -> "muted — hardware off" to BrandAccentDark
        false -> "available to granted apps" to SecondaryTextDark
        null -> "state unknown" to SecondaryTextDark
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Shield, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: $text", color = PrimaryTextDark, fontSize = 13.sp)
    }
}
