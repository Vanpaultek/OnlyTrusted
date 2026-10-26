// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.onlytrusted.app.OnlyTrustedApp
import com.onlytrusted.app.export.CSVExporter
import com.onlytrusted.app.export.DataExporter
import com.onlytrusted.app.export.EvidenceExporter
import com.onlytrusted.app.security.AppLockManager
import com.onlytrusted.app.ui.duress.DuressState
import com.onlytrusted.app.ui.hardwarekey.HardwareKeyGate
import com.onlytrusted.app.ui.hardwarekey.HardwareKeyState
import com.onlytrusted.app.ui.lock.BiometricPrompter
import com.onlytrusted.app.ui.safety.AutoLockPrefs
import com.onlytrusted.app.ui.safety.CarrierSpamReport
import com.onlytrusted.app.ui.subscription.SubscriptionState
import com.onlytrusted.app.ui.subscription.subGold
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.DangerDark
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.MidnightCard
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark
import com.onlytrusted.app.ui.theme.TertiaryTextDark
import com.onlytrusted.app.ui.theme.WarningDark
import kotlinx.coroutines.launch

// Blue + Purple used by the Settings icon chips (match iOS neon mockup).
private val settingsBlue   = Color(0xFF2266F0)
private val settingsPurple = Color(0xFF8B5CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPermissions: () -> Unit = {},
    onOpenPosture: () -> Unit = {},
    onOpenShieldLog: () -> Unit = {},
    onOpenLegal: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenLanguageRegion: () -> Unit = {},
    onOpenSensorPrivacy: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    onOpenBilling: () -> Unit = {},
    onOpenUpgrade: () -> Unit = {},
    onOpenDuress: () -> Unit = {},
    onOpenPermWatchdog: () -> Unit = {},
    onOpenSimSwap: () -> Unit = {},
    onOpenWangiri: () -> Unit = {},
    onOpenCheckIn: () -> Unit = {},
    onOpenSmartLinkGuard: () -> Unit = {},
    onOpenTrackerFinder: () -> Unit = {},
    onOpenHardwareKey: () -> Unit = {},
    onOpenDeadmanWipe: () -> Unit = {},
    onOpenEncryptedBackup: () -> Unit = {},
    onOpenBackupRestore: () -> Unit = {},
    onOpenBorderMode: () -> Unit = {},
    onOpenPanicLockdown: () -> Unit = {},
    onOpenFaceLock: () -> Unit = {},
    onOpenGeofence: () -> Unit = {},
    onOpenShamirSplit: () -> Unit = {},
    onOpenTrustedWitness: () -> Unit = {},
    onOpenStegoVault: () -> Unit = {},
    onOpenSignedLog: () -> Unit = {},
    onOpenVoiceBio: () -> Unit = {},
    onOpenRogueNetwork: () -> Unit = {},
    onOpenThreatIntel: () -> Unit = {},
    onOpenPrivacyShield: () -> Unit = {},
    onOpenAppShield: () -> Unit = {},
    onOpenBreachWatch: () -> Unit = {},
    onOpenDataRemoval: () -> Unit = {},
    onOpenAliases: () -> Unit = {},
    onOpenIdentityProtection: () -> Unit = {},
    onOpenWeeklyDigest: () -> Unit = {},
    onOpenAdvancedBlocking: () -> Unit = {},
    onOpenContactScan: () -> Unit = {},
    onOpenSmsScan: () -> Unit = {},
    onOpenDistressSignal: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as OnlyTrustedApp
    val vm: SettingsViewModel = viewModel(factory = viewModelFactory {
        initializer { SettingsViewModel(app) }
    })
    val state by vm.state.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmWipe by remember { mutableStateOf(false) }
    var confirmPanic by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var lastExport by remember { mutableStateOf<(() -> Unit)?>(null) }
    var themeSheetOpen by remember { mutableStateOf(false) }
    val themeSheetState = rememberModalBottomSheetState()
    var duressUpsellOpen by remember { mutableStateOf(false) }
    var watchdogUpsellOpen by remember { mutableStateOf(false) }
    var simSwapUpsellOpen by remember { mutableStateOf(false) }
    var wangiriUpsellOpen by remember { mutableStateOf(false) }
    var checkInUpsellOpen by remember { mutableStateOf(false) }
    var smartLinkGuardUpsellOpen by remember { mutableStateOf(false) }
    var trackerFinderUpsellOpen by remember { mutableStateOf(false) }
    var hardwareKeyUpsellOpen by remember { mutableStateOf(false) }
    var hardwareKeyGateError by remember { mutableStateOf<String?>(null) }
    var deadmanUpsellOpen by remember { mutableStateOf(false) }
    var encryptedBackupUpsellOpen by remember { mutableStateOf(false) }
    var backupRestoreUpsellOpen by remember { mutableStateOf(false) }
    var shamirUpsellOpen by remember { mutableStateOf(false) }
    var trustedWitnessUpsellOpen by remember { mutableStateOf(false) }
    var trustedWitnessGateError by remember { mutableStateOf<String?>(null) }
    var stegoVaultUpsellOpen by remember { mutableStateOf(false) }
    var signedLogUpsellOpen by remember { mutableStateOf(false) }
    var voiceBioUpsellOpen by remember { mutableStateOf(false) }
    var rogueNetworkUpsellOpen by remember { mutableStateOf(false) }
    var threatIntelUpsellOpen by remember { mutableStateOf(false) }
    // App Shield (Elite) — Phase 2 Selected-App Restriction. Non-Elite users see the upsell;
    // Elite users navigate to the guided Digital Wellbeing walkthrough.
    var appShieldUpsellOpen by remember { mutableStateOf(false) }
    val appShieldWatched = remember {
        com.onlytrusted.app.ui.appshield.AppShieldState.watchedPackages(context)
    }
    val appShieldCount = appShieldWatched.size
    val appShieldConfigured = remember {
        com.onlytrusted.app.ui.appshield.AppShieldState.isConfigured(context)
    }
    val voiceBioEnrolledCount = remember {
        com.onlytrusted.app.ui.voicebio.VoiceBioState.enrolledContactIds(context).size
    }
    val signEveryEnabled = remember {
        com.onlytrusted.app.ui.signedlog.SignedLogState.isSignEveryEntryEnabled(context)
    }
    val shamirSplitStatus = remember {
        com.onlytrusted.app.ui.p2p.ShamirState.mySplit(context)
    }
    val trustedWitnessEnabled = remember {
        com.onlytrusted.app.ui.p2p.TrustedWitnessState.isFeatureEnabled(context)
    }
    val trustedWitnessCount = remember {
        com.onlytrusted.app.ui.p2p.TrustedWitnessState.witnesses(context).size
    }
    var borderModeUpsellOpen by remember { mutableStateOf(false) }
    var panicLockdownUpsellOpen by remember { mutableStateOf(false) }
    val panicLockdownArmed = remember {
        com.onlytrusted.app.ui.paniclockdown.PanicLockdownState.isArmed(context)
    }
    var faceLockUpsellOpen by remember { mutableStateOf(false) }
    var geofenceUpsellOpen by remember { mutableStateOf(false) }
    val geofenceEnabled = remember { com.onlytrusted.app.geofence.GeofenceState.isEnabled(context) }
    val geofenceCount = remember { com.onlytrusted.app.geofence.GeofenceState.geofences(context).size }
    val faceLockEnrolled = remember { com.onlytrusted.app.ui.facelock.FaceLockState.isEnrolled(context) }
    val borderModeArmed = remember { com.onlytrusted.app.ui.bordermode.BorderModeState.isArmed(context) }
    val deadmanEnabled = remember { com.onlytrusted.app.deadmanwipe.DeadmanWipeState.isEnabled(context) }
    val deadmanInterval = remember { com.onlytrusted.app.deadmanwipe.DeadmanWipeState.intervalDays(context) }
    val duressEnabled = remember { DuressState.isEnabled(context) }
    val hardwareKeyActive = remember { HardwareKeyState.hasEnrolledKey(context) }
    val simSwapEnabled = remember {
        com.onlytrusted.app.ui.simswap.SimSwapState.isEnabled(context)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { DataExporter.exportTo(context, uri) }
                .onSuccess { snackbarHostState.showSnackbar("Exported $it rows.") }
                .onFailure { exportError = it.message ?: "unknown error" }
        }
    }
    val evidenceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { EvidenceExporter.exportTo(context, uri) }
                .onSuccess { snackbarHostState.showSnackbar("Evidence pack ready.") }
                .onFailure { exportError = it.message ?: "unknown error" }
        }
    }
    // F6 — CSV export launcher (SAF CreateDocument with text/csv).
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { CSVExporter.exportTo(context, uri) }
                .onSuccess { snackbarHostState.showSnackbar("Exported $it rows to CSV.") }
                .onFailure { exportError = it.message ?: "unknown error" }
        }
    }
    // F2 — Auto-lock state + picker visibility.
    var autoLockEnabled by remember { mutableStateOf(AutoLockPrefs.isEnabled(context)) }
    var autoLockSeconds by remember { mutableStateOf(AutoLockPrefs.currentSeconds(context)) }
    var showAutoLockPicker by remember { mutableStateOf(false) }
    // F8 — Carrier spam report chooser.
    var showCarrierPicker by remember { mutableStateOf(false) }

    if (exportError != null) {
        com.onlytrusted.app.ui.common.ErrorStateView(
            message = "We couldn't complete the export. ${exportError ?: ""}",
            onTryAgain = lastExport?.let { work -> { exportError = null; work() } },
            onGoBack = { exportError = null }
        )
        return
    }

    com.onlytrusted.app.ui.theme.AppBackdropScaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pinned top header — opaque, extends to status bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                MidnightBackground,
                                MidnightBackground.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    "Settings",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTextDark
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            // --- Security & data (Android-preserved functionality) ---
            SectionHeader("Device & data")
            SettingsCard {
                // Region row — read-only summary of what the app detected from the OS.
                // Used by Identity Protection to pick the right action catalog.
                val regionSnapshot = remember { com.onlytrusted.app.ui.region.RegionState.snapshot(context) }
                SettingsNavRow(
                    icon = Icons.Filled.Public,
                    tint = BrandAccentDark,
                    title = "Region",
                    value = regionSnapshot.country,
                    showChevron = false,
                    onClick = {}
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Filled.Lock,
                    tint = BrandAccentDark,
                    title = "App Lock",
                    detail = "Require biometric or device PIN to open.",
                    checked = state.appLockEnabled,
                    onChange = { enable ->
                        if (enable && !BiometricPrompter.canAuthenticate(context)) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Set up a device PIN, pattern, or biometric first."
                                )
                            }
                            return@ToggleRow
                        }
                        vm.setAppLock(enable)
                    }
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Filled.Shield,
                    tint = BrandAccentDark,
                    title = "Sensor monitoring",
                    detail = "Alerts when another app uses mic or camera.",
                    checked = state.sensorMonitorEnabled,
                    onChange = vm::setSensorMonitor
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Filled.Phone,
                    tint = BrandAccentDark,
                    title = "Pocket-dial protection",
                    detail = "Uses proximity and motion to detect pocket.",
                    checked = state.pocketDetectionEnabled,
                    onChange = vm::setPocketDetection
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Filled.Shield,
                    tint = BrandAccentDark,
                    title = "Screenshot protection",
                    detail = "Blocks screenshots of this app's screens.",
                    checked = state.screenshotProtectionEnabled,
                    onChange = vm::setScreenshotProtection
                )
                // F2 — Auto-lock after inactivity (toggle + submenu).
                RowDivider()
                ToggleRow(
                    icon = Icons.Filled.Timer,
                    tint = BrandAccentDark,
                    title = "Auto-lock after inactivity",
                    detail = if (autoLockEnabled)
                        "Re-lock after ${AutoLockPrefs.label(autoLockSeconds)} in the background."
                    else
                        "Only lock when you choose Panic lock.",
                    checked = autoLockEnabled,
                    onChange = { enable ->
                        autoLockEnabled = enable
                        AutoLockPrefs.setEnabled(context, enable)
                        if (enable) {
                            if (autoLockSeconds == 0) autoLockSeconds = 60
                            AutoLockPrefs.setSeconds(context, autoLockSeconds)
                            AppLockManager.setGraceSeconds(context, autoLockSeconds)
                        } else {
                            // Set grace to effectively "never prompt on resume" so the
                            // lock gate stays open until the user chooses Panic lock.
                            AppLockManager.setGraceSeconds(context, 600)
                        }
                    }
                )
                if (autoLockEnabled) {
                    RowDivider()
                    SettingsNavRow(
                        icon = Icons.Filled.Timer,
                        tint = BrandAccentDark,
                        title = "Inactivity window",
                        value = AutoLockPrefs.label(autoLockSeconds),
                        onClick = { showAutoLockPicker = true }
                    )
                }

                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.FileUpload,
                    tint = BrandAccentDark,
                    title = "Export my data (JSON)",
                    showChevron = false,
                    onClick = {
                        val action = {
                            exportLauncher.launch("onlytrusted-${System.currentTimeMillis()}.json")
                        }
                        lastExport = action
                        action()
                    }
                )
                // F6 — CSV export row.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.TableChart,
                    tint = BrandAccentDark,
                    title = "Export as CSV",
                    showChevron = false,
                    onClick = {
                        val action = {
                            csvLauncher.launch("onlytrusted-${System.currentTimeMillis()}.csv")
                        }
                        lastExport = action
                        action()
                    }
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.FileUpload,
                    tint = BrandAccentDark,
                    title = "Export evidence pack",
                    showChevron = false,
                    onClick = {
                        val action = {
                            evidenceLauncher.launch("evidence-${System.currentTimeMillis()}.json")
                        }
                        lastExport = action
                        action()
                    }
                )
                // F8 — Carrier spam report shortcut. US-only (7726 + carrier short-codes
                // are North American conventions); hidden on non-US devices.
                if (com.onlytrusted.app.ui.region.RegionState.snapshot(context).country == "US") {
                    RowDivider()
                    SettingsNavRow(
                        icon = Icons.Filled.Campaign,
                        tint = BrandAccentDark,
                        title = "Carrier spam report",
                        onClick = { showCarrierPicker = true }
                    )
                }
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Lock,
                    tint = WarningDark,
                    title = "Panic lock now",
                    titleTint = WarningDark,
                    showChevron = false,
                    onClick = { confirmPanic = true }
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Delete,
                    tint = DangerDark,
                    title = "Wipe everything",
                    titleTint = DangerDark,
                    showChevron = false,
                    onClick = { confirmWipe = true }
                )
            }

            // --- Account & reports (not tier-exclusive — plan management) ---
            SectionHeader("Account")
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    tint = settingsBlue,
                    title = "Reports",
                    onClick = onOpenReports
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.CreditCard,
                    tint = settingsBlue,
                    title = "Billing & Subscription",
                    onClick = onOpenBilling
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.EmojiEvents,
                    tint = subGold,
                    title = "Upgrade",
                    onClick = onOpenUpgrade
                )
            }

            // --- Appearance (iOS-aligned: PURPLE gradient chips) ---
            SectionHeader("Appearance")
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Filled.Palette,
                    tint = settingsPurple,
                    title = "App Theme",
                    value = "Dark (System)",
                    onClick = { themeSheetOpen = true }
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Language,
                    tint = settingsPurple,
                    title = "Language",
                    value = "English",
                    onClick = {
                        val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    }
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.TextFields,
                    tint = settingsPurple,
                    title = "Text Size",
                    value = "System",
                    onClick = {
                        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    }
                )
            }

                // --- FREE Features (always available — core comms + system settings) ---
            TierSectionHeader("Free Features", chipText = "FREE", chipColor = settingsBlue)
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Filled.PersonSearch,
                    tint = settingsBlue,
                    title = "Data Removal Scan",
                    onClick = onOpenDataRemoval
                )
                RowDivider()
                // Contact Safety Scan — on-device scan of system contacts against the
                // bundled scam-numbers feed. Opt-in READ_CONTACTS; nothing leaves the
                // device. See ui/contactscan/ContactsScamScanner.kt.
                SettingsNavRow(
                    icon = Icons.Filled.Contacts,
                    tint = settingsBlue,
                    title = "Contact Safety Scan",
                    onClick = onOpenContactScan
                )
                RowDivider()
                // Scam SMS Scan — on-device scan of SMS inbox against bundled scam-numbers
                // + phishing-URLs + classic scam phrases. Opt-in READ_SMS; nothing leaves
                // the device. See ui/smsscan/SmsScamScanner.kt.
                SettingsNavRow(
                    icon = Icons.AutoMirrored.Filled.Message,
                    tint = settingsBlue,
                    title = "Scam SMS Scan",
                    onClick = onOpenSmsScan
                )
                RowDivider()
                // Distress Signal — SOS button + shake + volume-down 4× triggers.
                // Multi-recipient SMS with current location + phone dialer + WhatsApp
                // group fan-out. See ui/distress/.
                SettingsNavRow(
                    icon = Icons.Filled.Warning,
                    tint = DangerDark,
                    title = "Distress Signal",
                    onClick = onOpenDistressSignal
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Shield,
                    tint = settingsBlue,
                    title = "Blocking & Unknown",
                    onClick = onOpenPermissions
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Notifications,
                    tint = settingsBlue,
                    title = "Notifications",
                    onClick = onOpenNotifications
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Phone,
                    tint = settingsBlue,
                    title = "Call Handling",
                    onClick = onOpenPermissions
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.AutoMirrored.Filled.Message,
                    tint = settingsBlue,
                    title = "Text Handling",
                    onClick = onOpenPermissions
                )
            }

            // --- PREMIUM Features ($4.99/yr — privacy & identity monitoring) ---
            // Privacy Shield, Breach Watch, Private Aliases, Identity Protection were in
            // Free Features through v0.2.0; moved to Premium per product repositioning
            // so Free covers calls/SMS/system settings only and Premium covers active
            // privacy monitoring. Each screen also enforces the gate internally so
            // Free users deep-linking in see the upsell card.
            TierSectionHeader("Premium Features", chipText = "PREMIUM", chipColor = BrandAccentDark)
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Filled.Security,
                    tint = BrandAccentDark,
                    title = "Privacy Shield",
                    onClick = onOpenPrivacyShield
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.MarkEmailUnread,
                    tint = BrandAccentDark,
                    title = "Breach Watch",
                    onClick = onOpenBreachWatch
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.AlternateEmail,
                    tint = BrandAccentDark,
                    title = "Private Aliases",
                    onClick = onOpenAliases
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.VerifiedUser,
                    tint = BrandAccentDark,
                    title = "Identity Protection",
                    onClick = onOpenIdentityProtection
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Description,
                    tint = BrandAccentDark,
                    title = "Weekly Digest",
                    onClick = onOpenWeeklyDigest
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.FilterList,
                    tint = BrandAccentDark,
                    title = "Advanced Blocking",
                    onClick = onOpenAdvancedBlocking
                )
            }

            // --- PRO Features ($19.99/yr) ---
            TierSectionHeader("Pro Features", chipText = "PRO", chipColor = subGold)
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Filled.Warning,
                    tint = if (duressEnabled) BrandAccentDark else subGold,
                    title = "Duress Mode",
                    value = if (duressEnabled) "On" else null,
                    onClick = {
                        if (SubscriptionState.hasHighPremium(context)) {
                            onOpenDuress()
                        } else {
                            duressUpsellOpen = true
                        }
                    }
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.VerifiedUser,
                    tint = if (SubscriptionState.hasHighPremium(context)) BrandAccentDark else subGold,
                    title = "App Permission Watchdog",
                    onClick = {
                        if (SubscriptionState.hasHighPremium(context)) {
                            onOpenPermWatchdog()
                        } else {
                            watchdogUpsellOpen = true
                        }
                    }
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.SimCard,
                    tint = if (simSwapEnabled) BrandAccentDark else subGold,
                    title = "SIM-Swap Alarm",
                    value = if (simSwapEnabled) "On" else null,
                    onClick = {
                        if (SubscriptionState.hasHighPremium(context)) {
                            onOpenSimSwap()
                        } else {
                            simSwapUpsellOpen = true
                        }
                    }
                )
                // Wangiri Protection (Pro)
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.PhoneDisabled,
                    tint = if (SubscriptionState.hasHighPremium(context)) BrandAccentDark else subGold,
                    title = "Wangiri Protection",
                    onClick = {
                        if (SubscriptionState.hasHighPremium(context)) {
                            onOpenWangiri()
                        } else {
                            wangiriUpsellOpen = true
                        }
                    }
                )
                // Safety Check-In (Pro)
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.LocationSearching,
                    tint = if (SubscriptionState.hasHighPremium(context)) BrandAccentDark else subGold,
                    title = "Safety Check-In",
                    onClick = {
                        if (SubscriptionState.hasHighPremium(context)) {
                            onOpenCheckIn()
                        } else {
                            checkInUpsellOpen = true
                        }
                    }
                )
                // Smart Link Guard (Pro) — URL inspection for SMS + QR codes.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.LinkOff,
                    tint = if (SubscriptionState.hasHighPremium(context)) BrandAccentDark else subGold,
                    title = "Smart Link Guard",
                    onClick = {
                        if (SubscriptionState.hasHighPremium(context)) {
                            onOpenSmartLinkGuard()
                        } else {
                            smartLinkGuardUpsellOpen = true
                        }
                    }
                )
                // Tracker Finder (Pro) — BLE scan for AirTag / Tile / Samsung SmartTag.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.BluetoothSearching,
                    tint = if (SubscriptionState.hasHighPremium(context)) BrandAccentDark else subGold,
                    title = "Tracker Finder",
                    onClick = {
                        if (SubscriptionState.hasHighPremium(context)) {
                            onOpenTrackerFinder()
                        } else {
                            trackerFinderUpsellOpen = true
                        }
                    }
                )
            }

            // --- ELITE Features ($29.99/yr) ---
            TierSectionHeader("Elite Features", chipText = "ELITE", chipColor = subGold)
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Filled.Shield,
                    tint = when {
                        SubscriptionState.hasElite(context) && appShieldConfigured -> BrandAccentDark
                        SubscriptionState.hasElite(context) -> settingsBlue
                        else -> subGold
                    },
                    title = "App Shield",
                    value = if (SubscriptionState.hasElite(context) && appShieldConfigured)
                        "On · $appShieldCount" else null,
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenAppShield()
                        } else {
                            appShieldUpsellOpen = true
                        }
                    }
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.FlightTakeoff,
                    tint = when {
                        borderModeArmed -> DangerDark
                        SubscriptionState.hasElite(context) -> BrandAccentDark
                        else -> subGold
                    },
                    title = "Border Mode",
                    value = if (borderModeArmed) "Armed" else null,
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenBorderMode()
                        } else {
                            borderModeUpsellOpen = true
                        }
                    }
                )

                // Panic Lockdown (Elite) — the "I'm in trouble RIGHT NOW" companion to Border
                // Mode. Single tap locks the app, arms Duress for the next unlock, forces
                // aggressive sensor monitoring, disables the Smart Link Guard SMS watcher +
                // QR scanner, and switches to PIN-only. Placed directly below Border Mode so
                // the two Elite emergency presets live together.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Warning,
                    tint = when {
                        panicLockdownArmed -> DangerDark
                        SubscriptionState.hasElite(context) -> DangerDark
                        else -> subGold
                    },
                    title = "Panic Lockdown",
                    value = if (panicLockdownArmed) "ARMED" else null,
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenPanicLockdown()
                        } else {
                            panicLockdownUpsellOpen = true
                        }
                    }
                )

                // Hardware Security Key (Elite) — FIDO2 YubiKey / Titan / SoloKey. Elite tier
                // gates the most dangerous actions (Wipe everything, Duress disarm, encrypted
                // backup export, Dead-Man Wipe disable) behind a physical key tap.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.VpnKey,
                    tint = if (SubscriptionState.hasElite(context)) BrandAccentDark else subGold,
                    title = "Hardware Security Key",
                    value = if (SubscriptionState.hasElite(context) && hardwareKeyActive) "On" else null,
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenHardwareKey()
                        } else {
                            hardwareKeyUpsellOpen = true
                        }
                    }
                )

                // Voice Biometric (Elite) — enroll trusted contacts' voice fingerprints to
                // catch AI-cloned deepfake callers. 10s enrollment per contact; cosine
                // similarity threshold 0.6 vs stored 20-dim MFCC descriptor.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.MicNone,
                    tint = if (SubscriptionState.hasElite(context)) BrandCyanDark else subGold,
                    title = "Voice Biometric",
                    value = if (SubscriptionState.hasElite(context) && voiceBioEnrolledCount > 0)
                        "$voiceBioEnrolledCount enrolled" else null,
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenVoiceBio()
                        } else {
                            voiceBioUpsellOpen = true
                        }
                    }
                )

                // Mic & Camera Lock — honest sensor-privacy screen. The badge auto-adapts:
                // "Android limits apply" on Consumer tier, "Device Owner active" when
                // provisioned. See docs/SENSOR_PRIVACY_ARCHITECTURE.md.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.MicNone,
                    tint = BrandCyanDark,
                    title = "Mic & Camera Lock",
                    value = if (com.onlytrusted.app.ui.sensorprivacy.DeviceOwnerSensorPrivacyController.isActive(context))
                        "Device Owner active" else "Android limits apply",
                    onClick = onOpenSensorPrivacy,
                )

                // Face-Change Shoulder-Surfing Lock (Elite) — front camera watches for faces
                // while sensitive screens are open. Blurs when the owner walks away; locks
                // instantly if a stranger / extra face appears.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.FaceRetouchingNatural,
                    tint = when {
                        SubscriptionState.hasElite(context) && faceLockEnrolled -> BrandAccentDark
                        SubscriptionState.hasElite(context) -> BrandAccentDark
                        else -> subGold
                    },
                    title = "Face-Change Lock",
                    value = if (SubscriptionState.hasElite(context) && faceLockEnrolled) "On" else null,
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenFaceLock()
                        } else {
                            faceLockUpsellOpen = true
                        }
                    }
                )

                // Geo-fence Auto-Lock (Elite) — up to 5 trusted locations. The app
                // auto-locks + sends a silent SMS alert when the device exits every
                // trusted zone. Requires ACCESS_BACKGROUND_LOCATION for exit events
                // to fire while the app is in the background.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.LocationOn,
                    tint = if (geofenceEnabled) BrandAccentDark else subGold,
                    title = "Geo-fence Auto-Lock",
                    value = if (geofenceEnabled) "On · $geofenceCount" else null,
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenGeofence()
                        } else {
                            geofenceUpsellOpen = true
                        }
                    }
                )

                // Dead-Man Wipe (Elite) — auto-wipe after N days of no unlock.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Timelapse,
                    tint = if (deadmanEnabled) DangerDark else subGold,
                    title = "Dead-Man Wipe",
                    value = if (deadmanEnabled) "On · ${deadmanInterval}d" else null,
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenDeadmanWipe()
                        } else {
                            deadmanUpsellOpen = true
                        }
                    }
                )

                // Encrypted vault backup (Elite) — AES-256-GCM `.ot2bak` saved via SAF to
                // Google Drive / Dropbox / anywhere.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.CloudUpload,
                    tint = if (SubscriptionState.hasElite(context)) BrandAccentDark else subGold,
                    title = "Encrypted vault backup",
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenEncryptedBackup()
                        } else {
                            encryptedBackupUpsellOpen = true
                        }
                    }
                )

                // Restore from backup (Elite) — pick a `.ot2bak`, decrypt, overwrite.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.CloudDownload,
                    tint = if (SubscriptionState.hasElite(context)) BrandAccentDark else subGold,
                    title = "Restore from backup",
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenBackupRestore()
                        } else {
                            backupRestoreUpsellOpen = true
                        }
                    }
                )

                // Shamir Split (Elite) — vault master-key K-of-N split via BLE.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Share,
                    tint = when {
                        shamirSplitStatus != null -> BrandAccentDark
                        SubscriptionState.hasElite(context) -> BrandAccentDark
                        else -> subGold
                    },
                    title = "Shamir Split",
                    value = shamirSplitStatus?.let { "${it.threshold}-of-${it.total}" },
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenShamirSplit()
                        } else {
                            shamirUpsellOpen = true
                        }
                    }
                )

                // Steganographic Vault (Elite) — hide the vault inside a decoy PNG or PDF.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.PhotoLibrary,
                    tint = if (SubscriptionState.hasElite(context)) BrandAccentDark else subGold,
                    title = "Steganographic Vault",
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenStegoVault()
                        } else {
                            stegoVaultUpsellOpen = true
                        }
                    }
                )

                // Signed Audit Log (Elite) — hardware-signed events + Merkle commitments +
                // court-ready evidence pack export + verifier.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.VerifiedUser,
                    tint = when {
                        signEveryEnabled && hardwareKeyActive -> BrandAccentDark
                        SubscriptionState.hasElite(context) -> BrandAccentDark
                        else -> subGold
                    },
                    title = "Signed Audit Log",
                    value = if (signEveryEnabled && hardwareKeyActive) "On" else null,
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenSignedLog()
                        } else {
                            signedLogUpsellOpen = true
                        }
                    }
                )

                // Trusted Witness (Elite) — proximity-approved sensitive actions.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.PeopleAlt,
                    tint = when {
                        trustedWitnessEnabled -> BrandAccentDark
                        SubscriptionState.hasElite(context) -> BrandAccentDark
                        else -> subGold
                    },
                    title = "Trusted Witness",
                    value = if (trustedWitnessEnabled) "On · $trustedWitnessCount" else null,
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenTrustedWitness()
                        } else {
                            trustedWitnessUpsellOpen = true
                        }
                    }
                )

                // Rogue Network Scanner (Elite) — IMSI-catcher + 2G downgrade + lookalike
                // Wi-Fi detector. 100% on-device.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.NetworkCheck,
                    tint = if (SubscriptionState.hasElite(context)) BrandAccentDark else subGold,
                    title = "Rogue Network Scanner",
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenRogueNetwork()
                        } else {
                            rogueNetworkUpsellOpen = true
                        }
                    }
                )

                // Offline Threat Intel (Elite) — bundled scam / phishing / breach / IoC
                // feeds consulted on-device.
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Security,
                    tint = if (SubscriptionState.hasElite(context)) BrandAccentDark else subGold,
                    title = "Offline Threat Intel",
                    onClick = {
                        if (SubscriptionState.hasElite(context)) {
                            onOpenThreatIntel()
                        } else {
                            threatIntelUpsellOpen = true
                        }
                    }
                )
            }

            // --- Shield watermark (Android-only feature) ---
            SectionHeader("Shield watermark")
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Owner tag printed in the on-screen watermark. Short labels work best.",
                        fontSize = 12.sp,
                        color = SecondaryTextDark
                    )
                    OutlinedTextField(
                        value = state.ownerTag,
                        onValueChange = vm::setOwnerTag,
                        singleLine = true,
                        label = { Text("Owner tag") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SettingsNavRow(
                        icon = Icons.Filled.Shield,
                        tint = BrandAccentDark,
                        title = "Shield session log",
                        onClick = onOpenShieldLog
                    )
                }
            }

            // --- More ---
            SectionHeader("More")
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Filled.Shield,
                    tint = BrandAccentDark,
                    title = "Permission audit",
                    onClick = onOpenPermissions
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Shield,
                    tint = BrandAccentDark,
                    title = "Device posture check",
                    onClick = onOpenPosture
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Policy,
                    tint = BrandAccentDark,
                    title = "Privacy, terms & honest claims",
                    onClick = onOpenLegal
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Language,
                    tint = BrandAccentDark,
                    title = androidx.compose.ui.res.stringResource(com.onlytrusted.app.R.string.settings_language_region),
                    onClick = onOpenLanguageRegion
                )
                RowDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Info,
                    tint = BrandAccentDark,
                    title = "About OnlyTrusted",
                    onClick = onOpenAbout
                )
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("OnlyTrusted", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SecondaryTextDark)
                Text("© 2026 VanPaulTek Inc · GNU AGPL-3.0", fontSize = 10.sp, color = TertiaryTextDark)
                Text("admin@vanpaultek.com", fontSize = 10.sp, color = TertiaryTextDark)
            }
            }
        }

        if (confirmPanic) {
            AlertDialog(
                onDismissRequest = { confirmPanic = false },
                title = { Text("Panic lock now?") },
                text = {
                    Text("The app will lock immediately and require your biometric or device PIN to reopen.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmPanic = false
                        AppLockManager.resetUnlock(context)
                        scope.launch { snackbarHostState.showSnackbar("App locked.") }
                    }) { Text("Lock app") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmPanic = false }) { Text("Cancel") }
                }
            )
        }

        if (confirmWipe) {
            AlertDialog(
                onDismissRequest = { confirmWipe = false },
                title = { Text("Wipe everything?") },
                text = {
                    Text(
                        "This will delete every event, every blocked number, every trusted contact, " +
                            "and every setting stored by this app. It cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmWipe = false
                        // Elite gate stack: Trusted Witness (if armed) → Hardware Security Key
                        // (if enrolled). Both are opt-in; if neither is configured this
                        // collapses to the pre-Elite behaviour.
                        com.onlytrusted.app.ui.p2p.TrustedWitnessGate.require(
                            context = context,
                            action = com.onlytrusted.app.ui.p2p.TrustedWitnessState.GatedAction.WIPE_EVERYTHING,
                            reason = "Have your witness approve the wipe.",
                            scope = scope,
                            onSuccess = {
                                HardwareKeyGate.requireAssertion(
                                    context = context,
                                    reason = "Confirm wipe with your security key.",
                                    scope = scope,
                                    onSuccess = {
                                        vm.wipeEverything()
                                        HardwareKeyState.clearAll(context)
                                        scope.launch { snackbarHostState.showSnackbar("Everything wiped.") }
                                    },
                                    onCancel = { msg ->
                                        hardwareKeyGateError = msg
                                            ?: "Wipe cancelled — security key tap missing."
                                    }
                                )
                            },
                            onCancel = { msg ->
                                trustedWitnessGateError = msg
                                    ?: "Wipe cancelled — witness did not approve."
                            }
                        )
                    }) { Text("Wipe") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmWipe = false }) { Text("Cancel") }
                }
            )
        }

        if (hardwareKeyGateError != null) {
            AlertDialog(
                onDismissRequest = { hardwareKeyGateError = null },
                title = {
                    Text(
                        "Hardware key required",
                        color = PrimaryTextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(hardwareKeyGateError.orEmpty(), color = SecondaryTextDark)
                },
                confirmButton = {
                    TextButton(onClick = { hardwareKeyGateError = null }) {
                        Text("OK", color = BrandAccentDark)
                    }
                },
                containerColor = MidnightCard
            )
        }

        if (hardwareKeyUpsellOpen) {
            AlertDialog(
                onDismissRequest = { hardwareKeyUpsellOpen = false },
                title = {
                    Text(
                        "Hardware Security Key is an Elite feature",
                        color = PrimaryTextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Use a YubiKey, Google Titan Key, or any FIDO2 security key to gate " +
                            "the most dangerous actions: disarming Duress Mode, wiping the vault, " +
                            "exporting encrypted backups, and disabling the Dead-Man Wipe. " +
                            "Included in Elite.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        hardwareKeyUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Elite", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { hardwareKeyUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (faceLockUpsellOpen) {
            AlertDialog(
                onDismissRequest = { faceLockUpsellOpen = false },
                title = {
                    Text(
                        "Face-Change Lock is an Elite feature",
                        color = PrimaryTextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "The front camera watches for faces while you're on sensitive screens. " +
                            "It blurs when you walk away and locks the moment someone else looks " +
                            "at your phone. All processing happens on-device; no images are stored " +
                            "or uploaded. Elite tier.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        faceLockUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Elite", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { faceLockUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (panicLockdownUpsellOpen) {
            AlertDialog(
                onDismissRequest = { panicLockdownUpsellOpen = false },
                title = {
                    Text(
                        "Panic Lockdown is an Elite feature",
                        color = PrimaryTextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "The RIGHT-NOW emergency button. One tap locks the app, arms Duress, " +
                            "forces aggressive sensor monitoring, disables the Smart Link " +
                            "Guard watcher, switches to PIN-only, and timestamps the moment " +
                            "in the audit log. Elite tier.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        panicLockdownUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Elite", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { panicLockdownUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (borderModeUpsellOpen) {
            AlertDialog(
                onDismissRequest = { borderModeUpsellOpen = false },
                title = { Text("Border Mode is an Elite feature", color = PrimaryTextDark, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "One tap. Every defensive layer at maximum. Dead-Man Wipe to 7 days, " +
                            "Duress active, biometric off, screenshot & shield on, pocket-dial on, " +
                            "cover identity on your Dashboard. Elite tier.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        borderModeUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Elite", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { borderModeUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (deadmanUpsellOpen) {
            AlertDialog(
                onDismissRequest = { deadmanUpsellOpen = false },
                title = { Text("Dead-Man Wipe is an Elite feature", color = PrimaryTextDark, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "A trip-wire for lost or seized devices. If OnlyTrusted hasn't been " +
                            "unlocked for a long time, it wipes every contact, blocked number, " +
                            "event, and setting on its own. Elite tier.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        deadmanUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Elite", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { deadmanUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (geofenceUpsellOpen) {
            com.onlytrusted.app.ui.geofence.GeofenceUpsellDialog(
                onDismiss = { geofenceUpsellOpen = false },
                onUpgrade = {
                    geofenceUpsellOpen = false
                    onOpenUpgrade()
                }
            )
        }

        if (encryptedBackupUpsellOpen) {
            AlertDialog(
                onDismissRequest = { encryptedBackupUpsellOpen = false },
                title = { Text("Encrypted vault backup is an Elite feature", color = PrimaryTextDark, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Export every trusted contact, blocked number, event, and setting as a " +
                            "single AES-256-GCM encrypted file. You pick where it goes — Google " +
                            "Drive, Dropbox, anywhere. Elite tier.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        encryptedBackupUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Elite", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { encryptedBackupUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (shamirUpsellOpen) {
            com.onlytrusted.app.ui.p2p.ShamirUpsellDialog(
                onUpgrade = {
                    shamirUpsellOpen = false
                    onOpenUpgrade()
                },
                onDismiss = { shamirUpsellOpen = false }
            )
        }

        if (trustedWitnessUpsellOpen) {
            com.onlytrusted.app.ui.p2p.TrustedWitnessUpsellDialog(
                onUpgrade = {
                    trustedWitnessUpsellOpen = false
                    onOpenUpgrade()
                },
                onDismiss = { trustedWitnessUpsellOpen = false }
            )
        }

        if (stegoVaultUpsellOpen) {
            com.onlytrusted.app.ui.stego.StegoVaultUpsellDialog(
                onUpgrade = {
                    stegoVaultUpsellOpen = false
                    onOpenUpgrade()
                },
                onDismiss = { stegoVaultUpsellOpen = false }
            )
        }

        if (signedLogUpsellOpen) {
            com.onlytrusted.app.ui.signedlog.SignedLogUpsellDialog(
                onUpgrade = {
                    signedLogUpsellOpen = false
                    onOpenUpgrade()
                },
                onDismiss = { signedLogUpsellOpen = false }
            )
        }

        if (voiceBioUpsellOpen) {
            com.onlytrusted.app.ui.voicebio.VoiceBioUpsellDialog(
                onUpgrade = {
                    voiceBioUpsellOpen = false
                    onOpenUpgrade()
                },
                onDismiss = { voiceBioUpsellOpen = false }
            )
        }

        if (rogueNetworkUpsellOpen) {
            com.onlytrusted.app.ui.roguenetwork.RogueNetworkUpsellDialog(
                onUpgrade = {
                    rogueNetworkUpsellOpen = false
                    onOpenUpgrade()
                },
                onDismiss = { rogueNetworkUpsellOpen = false }
            )
        }

        if (threatIntelUpsellOpen) {
            com.onlytrusted.app.ui.threatintel.ThreatIntelUpsellDialog(
                onUpgrade = {
                    threatIntelUpsellOpen = false
                    onOpenUpgrade()
                },
                onDismiss = { threatIntelUpsellOpen = false }
            )
        }

        if (appShieldUpsellOpen) {
            com.onlytrusted.app.ui.appshield.AppShieldUpsellDialog(
                onUpgrade = {
                    appShieldUpsellOpen = false
                    onOpenUpgrade()
                },
                onDismiss = { appShieldUpsellOpen = false }
            )
        }

        if (trustedWitnessGateError != null) {
            AlertDialog(
                onDismissRequest = { trustedWitnessGateError = null },
                title = { Text("Trusted Witness required", color = PrimaryTextDark, fontWeight = FontWeight.Bold) },
                text = { Text(trustedWitnessGateError.orEmpty(), color = SecondaryTextDark) },
                confirmButton = {
                    TextButton(onClick = { trustedWitnessGateError = null }) { Text("OK", color = BrandAccentDark) }
                },
                containerColor = MidnightCard
            )
        }

        // Trusted Witness approval overlay — kicks in while a gated action is waiting on
        // the witness's phone.
        com.onlytrusted.app.ui.p2p.TrustedWitnessApprovalOverlay()

        if (backupRestoreUpsellOpen) {
            AlertDialog(
                onDismissRequest = { backupRestoreUpsellOpen = false },
                title = { Text("Restore from backup is an Elite feature", color = PrimaryTextDark, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Pick a .ot2bak file from anywhere on your device, enter your " +
                            "passphrase, and OnlyTrusted restores every contact, blocked number, " +
                            "event, and setting. Elite tier.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        backupRestoreUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Elite", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { backupRestoreUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (duressUpsellOpen) {
            AlertDialog(
                onDismissRequest = { duressUpsellOpen = false },
                title = { Text("Duress Mode is a Pro feature", color = PrimaryTextDark, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "A secondary PIN that unlocks a decoy vault and silently alerts a " +
                            "trusted contact. Available on Pro.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        duressUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Pro", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { duressUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (watchdogUpsellOpen) {
            AlertDialog(
                onDismissRequest = { watchdogUpsellOpen = false },
                title = {
                    Text(
                        "App Permission Watchdog is a Pro feature",
                        color = PrimaryTextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "A daily on-device audit of every installed app's sensitive permissions. " +
                            "Get alerted when something new asks for your camera, mic, location, " +
                            "contacts, SMS, or call log. Available on Pro.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        watchdogUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Pro", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { watchdogUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (simSwapUpsellOpen) {
            AlertDialog(
                onDismissRequest = { simSwapUpsellOpen = false },
                title = {
                    Text(
                        "SIM-Swap Alarm is a Pro feature",
                        color = PrimaryTextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Detect when an attacker ports your number to their SIM. OnlyTrusted " +
                            "fingerprints your SIM serial + carrier, alerts you on next app " +
                            "open if it changes, auto-locks the app, and logs the event. " +
                            "Available on Pro.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        simSwapUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Pro", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { simSwapUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (wangiriUpsellOpen) {
            AlertDialog(
                onDismissRequest = { wangiriUpsellOpen = false },
                title = {
                    Text(
                        "Wangiri Protection is a Pro feature",
                        color = PrimaryTextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Detects the classic one-ring hangup scam the moment it happens and " +
                            "auto-adds the caller to your blocked list. Included in Pro.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        wangiriUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Pro", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { wangiriUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (checkInUpsellOpen) {
            AlertDialog(
                onDismissRequest = { checkInUpsellOpen = false },
                title = {
                    Text(
                        "Safety Check-In is a Pro feature",
                        color = PrimaryTextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "One tap sends 'I'm safe' to every trusted contact. Optional daily " +
                            "reminder if you forget. Included in Pro.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        checkInUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Pro", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { checkInUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (smartLinkGuardUpsellOpen) {
            AlertDialog(
                onDismissRequest = { smartLinkGuardUpsellOpen = false },
                title = {
                    Text(
                        "Smart Link Guard is a Pro feature",
                        color = PrimaryTextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Inspects every URL from SMS and QR codes before you tap it. " +
                            "Flags shorteners, lookalike domains, suspicious TLDs, and " +
                            "phishing paths — all 100% on-device. Included in Pro.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        smartLinkGuardUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Pro", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { smartLinkGuardUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (trackerFinderUpsellOpen) {
            AlertDialog(
                onDismissRequest = { trackerFinderUpsellOpen = false },
                title = {
                    Text(
                        "Tracker Finder is a Pro feature",
                        color = PrimaryTextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Scans nearby Bluetooth for AirTag, Tile, Samsung SmartTag, and " +
                            "unknown trackers. Devices that keep reappearing across your " +
                            "scans get flagged as possible followers. Included in Pro.",
                        color = SecondaryTextDark
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        trackerFinderUpsellOpen = false
                        onOpenUpgrade()
                    }) { Text("Upgrade to Pro", color = subGold) }
                },
                dismissButton = {
                    TextButton(onClick = { trackerFinderUpsellOpen = false }) { Text("Not now") }
                },
                containerColor = MidnightCard
            )
        }

        if (themeSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { themeSheetOpen = false },
                sheetState = themeSheetState,
                containerColor = MidnightCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "App theme",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryTextDark
                    )
                    Text(
                        "OnlyTrusted is designed dark-first. 'System' follows the device setting.",
                        fontSize = 12.sp,
                        color = SecondaryTextDark
                    )
                    Spacer(Modifier.height(4.dp))
                    RadioOption("System", true)
                    RadioOption("Dark", false)
                    RadioOption("Light", false)
                }
            }
        }

        // F2 — Auto-lock window picker.
        if (showAutoLockPicker) {
            AlertDialog(
                onDismissRequest = { showAutoLockPicker = false },
                title = { Text("Auto-lock after", color = PrimaryTextDark) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AutoLockPrefs.options.forEach { opt ->
                            TextButton(onClick = {
                                autoLockSeconds = opt
                                AutoLockPrefs.setSeconds(context, opt)
                                // Keep AppLockManager's grace in sync so the biometric gate
                                // actually re-arms. 0 ("Never") maps to a long grace so the
                                // auto-lock never fires on resume.
                                AppLockManager.setGraceSeconds(
                                    context,
                                    if (opt == 0) 600 else opt
                                )
                                showAutoLockPicker = false
                            }) {
                                val isSelected = autoLockSeconds == opt
                                Text(
                                    text = (if (isSelected) "• " else "   ") + AutoLockPrefs.label(opt),
                                    color = if (isSelected) BrandAccentDark else PrimaryTextDark
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAutoLockPicker = false }) { Text("Close") }
                },
                containerColor = MidnightCard
            )
        }

        // F8 — Carrier spam report chooser.
        if (showCarrierPicker) {
            AlertDialog(
                onDismissRequest = { showCarrierPicker = false },
                title = { Text("Carrier spam report", color = PrimaryTextDark) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Open your carrier's spam-report app to flag the number. If the app isn't installed we'll take you to the Play Store.",
                            fontSize = 12.sp,
                            color = SecondaryTextDark
                        )
                        Spacer(Modifier.height(6.dp))
                        CarrierSpamReport.Carrier.entries.forEach { carrier ->
                            TextButton(onClick = {
                                showCarrierPicker = false
                                CarrierSpamReport.open(context, carrier)
                            }) {
                                Text(carrier.label, color = PrimaryTextDark)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCarrierPicker = false }) { Text("Cancel") }
                },
                containerColor = MidnightCard
            )
        }
    }
}

// ========== Row primitives ==========

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = SecondaryTextDark,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

/**
 * Header for a tier-grouped Settings section. Renders a colored tier badge next to the
 * subtitle so the user can see at a glance which features their current plan unlocks.
 */
@Composable
private fun TierSectionHeader(title: String, chipText: String, chipColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = chipColor.copy(alpha = 0.18f)
        ) {
            Text(
                chipText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = chipColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = SecondaryTextDark
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidnightElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

/**
 * Gradient icon-chip row. Matches iOS SettingsNavRow (chip style).
 * - 44x44 chip, RoundedCornerShape(11.dp)
 * - LinearGradient from tint → tint.alpha=0.75
 * - Drop shadow in the tint color
 * - White icon on top
 * - Title (16sp semibold), optional value in BrandCyanDark (14sp), optional chevron
 */
@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    value: String? = null,
    titleTint: Color = PrimaryTextDark,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconChip(icon = icon, tint = tint)
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleTint,
                modifier = Modifier.weight(1f)
            )
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandCyanDark
                )
            }
            if (showChevron) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = TertiaryTextDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun IconChip(icon: ImageVector, tint: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(11.dp),
                ambientColor = tint,
                spotColor = tint
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(tint, tint.copy(alpha = 0.75f)),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                ),
                shape = RoundedCornerShape(11.dp)
            )
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconChip(icon = icon, tint = tint)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = PrimaryTextDark)
            Text(detail, fontSize = 12.sp, color = SecondaryTextDark)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandCyanDark
            )
        )
    }
}

@Composable
private fun RowDivider() {
    Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 72.dp))
}

@Composable
private fun RadioOption(label: String, selected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (selected) BrandAccentDark else Color.Transparent,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Text(label, fontSize = 14.sp, color = PrimaryTextDark)
    }
}
