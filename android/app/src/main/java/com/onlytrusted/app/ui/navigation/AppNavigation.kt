// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.onlytrusted.app.ui.about.AboutScreen
import com.onlytrusted.app.ui.blocklist.BlockListScreen
import com.onlytrusted.app.ui.dashboard.DashboardScreen
import com.onlytrusted.app.ui.duress.DuressSetupScreen
import com.onlytrusted.app.ui.events.EventsScreen
import com.onlytrusted.app.ui.legal.LegalScreen
import com.onlytrusted.app.ui.notifications.NotificationsScreen
import com.onlytrusted.app.ui.permissions.PermissionAuditScreen
import com.onlytrusted.app.ui.permwatchdog.PermissionWatchdogScreen
import com.onlytrusted.app.ui.posture.PostureScreen
import com.onlytrusted.app.ui.reports.ReportsScreen
import com.onlytrusted.app.ui.settings.SettingsScreen
import com.onlytrusted.app.ui.shield.ShieldLogScreen
import com.onlytrusted.app.ui.simswap.SimSwapSetupScreen
import com.onlytrusted.app.ui.subscription.BillingSubscriptionScreen
import com.onlytrusted.app.ui.subscription.UpgradeToPremiumScreen
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark
import com.onlytrusted.app.ui.vault.VaultEditScreen
import com.onlytrusted.app.ui.vault.VaultScreen

sealed class Destination(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Destination("dashboard", "Home", Icons.Filled.Shield)
    data object Vault : Destination("vault", "Contacts", Icons.Filled.Groups)
    data object Events : Destination("events", "Log", Icons.Filled.History)
    data object BlockList : Destination("blocklist", "Blocked", Icons.Filled.Block)
    data object Settings : Destination("settings", "Settings", Icons.Filled.Settings)
    data object Permissions : Destination("permissions", "Permissions", Icons.Filled.Security)
    data object Posture : Destination("posture", "Posture", Icons.Filled.Security)
    data object ShieldLog : Destination("shieldlog", "Shield log", Icons.Filled.Shield)
    data object Legal : Destination("legal", "Legal", Icons.Filled.Security)
    data object About : Destination("about", "About", Icons.Filled.Security)
    data object Notifications : Destination("notifications", "Notifications", Icons.Filled.Security)
    data object Reports : Destination("reports", "Reports", Icons.AutoMirrored.Filled.ShowChart)
    data object BillingSubscription : Destination("billing_subscription", "Billing", Icons.Filled.CreditCard)
    data object UpgradePremium : Destination("upgrade_premium", "Premium", Icons.Filled.WorkspacePremium)
    data object DuressSetup : Destination("duress_setup", "Duress", Icons.Filled.Security)
    data object PermissionWatchdog : Destination("permission_watchdog", "Watchdog", Icons.Filled.Security)
    data object SimSwapSetup : Destination("simswap_setup", "SIM-Swap", Icons.Filled.Security)
    data object Wangiri : Destination("wangiri", "Wangiri", Icons.Filled.Security)
    data object SafetyCheckIn : Destination("safety_checkin", "Check-In", Icons.Filled.Security)
    data object SmartLinkGuard : Destination("smart_link_guard", "Links", Icons.Filled.Security)
    data object TrackerFinder : Destination("tracker_finder", "Tracker", Icons.Filled.Security)
    data object HardwareKey : Destination("hardware_key", "Security key", Icons.Filled.Security)
    data object DeadmanWipe : Destination("deadman_wipe", "Dead-Man", Icons.Filled.Security)
    data object Geofence : Destination("geofence_setup", "Geo-fence", Icons.Filled.Security)
    data object BorderMode : Destination("border_mode", "Border", Icons.Filled.Security)
    // Elite — Panic Lockdown: "I'm in trouble RIGHT NOW" one-tap emergency preset.
    data object PanicLockdown : Destination("panic_lockdown", "Panic", Icons.Filled.Security)
    data object FaceLock : Destination("face_lock", "Face lock", Icons.Filled.Security)
    data object EncryptedBackup : Destination("encrypted_backup", "Backup", Icons.Filled.Security)
    data object BackupRestore : Destination("backup_restore", "Restore", Icons.Filled.Security)
    data object VaultEdit : Destination("vault/edit?id={id}", "Vault edit", Icons.Filled.Security) {
        fun route(id: Long?) = "vault/edit?id=${id ?: -1L}"
    }
    // Shamir Split + Trusted Witness (Elite) — P2P-driven setup screens.
    data object ShamirSplit : Destination("shamir_split", "Shamir", Icons.Filled.Security)
    data object TrustedWitness : Destination("trusted_witness", "Witness", Icons.Filled.Security)
    // Elite — Steganographic Vault + Hardware-Signed Audit Log (forensic-proof features).
    data object StegoVault : Destination("stego_vault", "Stego", Icons.Filled.Security)
    data object SignedLog : Destination("signed_log", "Signed", Icons.Filled.Security)
    // Elite — Voice Biometric Trusted-Contact Verification.
    data object VoiceBio : Destination("voice_bio", "VoiceBio", Icons.Filled.Security)
    // Elite — Fortress R4 recovery: Rogue Wi-Fi + IMSI Catcher Scanner.
    data object RogueNetwork : Destination("rogue_network", "Rogue net", Icons.Filled.NetworkCheck)
    // Elite — Fortress R4 recovery: Offline Threat Intel database.
    data object ThreatIntel : Destination("threat_intel", "Threat intel", Icons.Filled.Security)
    // Auto Privacy Shield (free tier). Dashboard + hardening sub-screen.
    data object PrivacyShield : Destination("privacy_shield", "Privacy shield", Icons.Filled.Shield)
    data object PermissionHardening : Destination("permission_hardening", "Hardening", Icons.Filled.Security)
    // Auto Privacy Shield — Phase 2 (Elite). Guided Digital Wellbeing walkthrough.
    data object AppShield : Destination("app_shield", "App shield", Icons.Filled.Shield)
    // FREE — Breach Watch: monitors one email against the bundled breach-hash prefix list.
    data object BreachWatch : Destination("breach_watch", "Breach Watch", Icons.Filled.Shield)
    // FREE — Data Removal Scan (visibility only). Deep-link search URLs for people-search sites.
    data object DataRemoval : Destination("data_removal", "Data Removal", Icons.Filled.Security)
    // FREE — Private Aliases. On-device alias generator; forwarding is done by the user's chosen relay.
    data object Aliases : Destination("aliases", "Aliases", Icons.Filled.Shield)
    // FREE — Identity Protection catalog. Deep-link checklist for credit freezes, fraud alerts, etc.
    data object IdentityProtection : Destination("identity_protection", "Identity Protection", Icons.Filled.Shield)
    // Premium — Weekly Digest PDF (on-device generation + share-sheet export).
    data object WeeklyDigest : Destination("weekly_digest", "Weekly Digest", Icons.Filled.Shield)
    // Premium — Advanced Blocking (quiet hours + area-code allowlist).
    data object AdvancedBlocking : Destination("advanced_blocking", "Advanced Blocking", Icons.Filled.Shield)
    // FREE — Contact Safety Scan: cross-checks the system contact list against the
    // bundled scam-numbers feed on-device.
    data object ContactScan : Destination("contact_scan", "Contact Safety Scan", Icons.Filled.Shield)
    // FREE — Scam SMS Scan: cross-checks the SMS inbox against bundled scam-numbers +
    // phishing-URLs + classic scam phrases on-device.
    data object SmsScan : Destination("sms_scan", "Scam SMS Scan", Icons.Filled.Shield)
    // FREE — Distress Signal: SOS button + shake + volume-down → multi-recipient SMS +
    // dialer + WhatsApp group fan-out. Settings for triggers + WhatsApp group URL.
    data object DistressSignal : Destination("distress_signal", "Distress Signal", Icons.Filled.Security)
}

private val bottomBarDestinations = listOf(
    Destination.Dashboard,
    Destination.Vault,
    Destination.BlockList,
    Destination.Events,
    Destination.Settings
)

@Composable
fun AppNavigation(onPanic: () -> Unit = {}) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomBarDestinations.map { it.route }

    // F — App Permission Watchdog (Pro): if the user is on Pro and daily audit is enabled,
    // the composable self-gates and shows a non-dismissable alert when ≥1 app has gained
    // sensitive permissions since the stored baseline.
    com.onlytrusted.app.ui.permwatchdog.PermissionWatchdogLaunchAlert(
        onOpenWatchdog = { navController.navigate(Destination.PermissionWatchdog.route) }
    )

    // SIM-Swap Alarm (Pro): checks the carrier identity against the stored baseline on
    // every launch; a mismatch surfaces a non-dismissable full-screen alert and the user
    // can lock the app, call their carrier, or acknowledge the swap as intentional.
    com.onlytrusted.app.ui.simswap.SimSwapLaunchAlert(onLock = onPanic)

    Scaffold(
        containerColor = MidnightBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MidnightBackground,
                    contentColor = PrimaryTextDark
                ) {
                    bottomBarDestinations.forEach { dest ->
                        // Strict compare — hierarchy.any lit up the Home tab for any
                        // descendant route (Vault, BlockList, Events, Settings all push
                        // on top of Dashboard), so taps on Home couldn't visually
                        // confirm the nav. Compare just the current route.
                        val selected = currentRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != dest.route) {
                                    navController.navigate(dest.route) {
                                        launchSingleTop = true
                                        popUpTo(Destination.Dashboard.route) {
                                            saveState = true
                                            inclusive = false
                                        }
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandCyanDark,
                                selectedTextColor = BrandCyanDark,
                                unselectedIconColor = SecondaryTextDark,
                                unselectedTextColor = SecondaryTextDark,
                                indicatorColor = MidnightBackground
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Dashboard.route) {
                DashboardScreen(
                    onPanic = onPanic,
                    onGoToVault = { navController.navigate(Destination.Vault.route) },
                    onGoToSettings = { navController.navigate(Destination.Settings.route) },
                    onGoToBlockList = { navController.navigate(Destination.BlockList.route) },
                    onGoToEvents = { navController.navigate(Destination.Events.route) },
                    onGoToReports = { navController.navigate(Destination.Reports.route) }
                )
            }
            composable(Destination.Vault.route) {
                VaultScreen(
                    onAdd = { navController.navigate(Destination.VaultEdit.route(null)) },
                    onEdit = { id -> navController.navigate(Destination.VaultEdit.route(id)) }
                )
            }
            composable(
                route = Destination.VaultEdit.route,
                arguments = listOf(navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backEntry ->
                val id = backEntry.arguments?.getLong("id")?.takeIf { it > 0 }
                VaultEditScreen(contactId = id, onDone = { navController.popBackStack() })
            }
            composable(Destination.Events.route) { EventsScreen() }
            composable(Destination.BlockList.route) { BlockListScreen() }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    onOpenPermissions = { navController.navigate(Destination.Permissions.route) },
                    onOpenPosture = { navController.navigate(Destination.Posture.route) },
                    onOpenShieldLog = { navController.navigate(Destination.ShieldLog.route) },
                    onOpenLegal = { navController.navigate(Destination.Legal.route) },
                    onOpenAbout = { navController.navigate(Destination.About.route) },
                    onOpenNotifications = { navController.navigate(Destination.Notifications.route) },
                    onOpenReports = { navController.navigate(Destination.Reports.route) },
                    onOpenBilling = { navController.navigate(Destination.BillingSubscription.route) },
                    onOpenUpgrade = { navController.navigate(Destination.UpgradePremium.route) },
                    onOpenDuress = { navController.navigate(Destination.DuressSetup.route) },
                    onOpenPermWatchdog = { navController.navigate(Destination.PermissionWatchdog.route) },
                    onOpenSimSwap = { navController.navigate(Destination.SimSwapSetup.route) },
                    onOpenWangiri = { navController.navigate(Destination.Wangiri.route) },
                    onOpenCheckIn = { navController.navigate(Destination.SafetyCheckIn.route) },
                    onOpenSmartLinkGuard = { navController.navigate(Destination.SmartLinkGuard.route) },
                    onOpenTrackerFinder = { navController.navigate(Destination.TrackerFinder.route) },
                    onOpenHardwareKey = { navController.navigate(Destination.HardwareKey.route) },
                    onOpenDeadmanWipe = { navController.navigate(Destination.DeadmanWipe.route) },
                    onOpenEncryptedBackup = { navController.navigate(Destination.EncryptedBackup.route) },
                    onOpenBackupRestore = { navController.navigate(Destination.BackupRestore.route) },
                    onOpenBorderMode = { navController.navigate(Destination.BorderMode.route) },
                    onOpenPanicLockdown = { navController.navigate(Destination.PanicLockdown.route) },
                    onOpenFaceLock = { navController.navigate(Destination.FaceLock.route) },
                    onOpenGeofence = { navController.navigate(Destination.Geofence.route) },
                    onOpenShamirSplit = { navController.navigate(Destination.ShamirSplit.route) },
                    onOpenTrustedWitness = { navController.navigate(Destination.TrustedWitness.route) },
                    onOpenStegoVault = { navController.navigate(Destination.StegoVault.route) },
                    onOpenSignedLog = { navController.navigate(Destination.SignedLog.route) },
                    onOpenVoiceBio = { navController.navigate(Destination.VoiceBio.route) },
                    onOpenRogueNetwork = { navController.navigate(Destination.RogueNetwork.route) },
                    onOpenThreatIntel = { navController.navigate(Destination.ThreatIntel.route) },
                    onOpenPrivacyShield = { navController.navigate(Destination.PrivacyShield.route) },
                    onOpenAppShield = { navController.navigate(Destination.AppShield.route) },
                    onOpenBreachWatch = { navController.navigate(Destination.BreachWatch.route) },
                    onOpenDataRemoval = { navController.navigate(Destination.DataRemoval.route) },
                    onOpenAliases = { navController.navigate(Destination.Aliases.route) },
                    onOpenIdentityProtection = { navController.navigate(Destination.IdentityProtection.route) },
                    onOpenWeeklyDigest = { navController.navigate(Destination.WeeklyDigest.route) },
                    onOpenAdvancedBlocking = { navController.navigate(Destination.AdvancedBlocking.route) },
                    onOpenContactScan = { navController.navigate(Destination.ContactScan.route) },
                    onOpenSmsScan = { navController.navigate(Destination.SmsScan.route) },
                    onOpenDistressSignal = { navController.navigate(Destination.DistressSignal.route) }
                )
            }
            composable(Destination.PrivacyShield.route) {
                com.onlytrusted.app.ui.privacyshield.PrivacyShieldDashboard(
                    onBack = { navController.popBackStack() },
                    onOpenHardening = {
                        navController.navigate(Destination.PermissionHardening.route)
                    },
                    onOpenUpgrade = {
                        navController.navigate(Destination.UpgradePremium.route)
                    },
                    onOpenAppShield = {
                        navController.navigate(Destination.AppShield.route)
                    }
                )
            }
            composable(Destination.PermissionHardening.route) {
                com.onlytrusted.app.ui.privacyshield.PermissionHardeningScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.AppShield.route) {
                com.onlytrusted.app.ui.appshield.AppShieldSetupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.BreachWatch.route) {
                com.onlytrusted.app.ui.breachwatch.BreachWatchScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.DataRemoval.route) {
                com.onlytrusted.app.ui.dataremoval.DataRemovalScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.Aliases.route) {
                com.onlytrusted.app.ui.aliases.AliasesScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.IdentityProtection.route) {
                com.onlytrusted.app.ui.identity.IdentityProtectionScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.WeeklyDigest.route) {
                com.onlytrusted.app.ui.weeklydigest.WeeklyDigestScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.AdvancedBlocking.route) {
                com.onlytrusted.app.ui.advancedblocking.AdvancedBlockingScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.ContactScan.route) {
                com.onlytrusted.app.ui.contactscan.ContactScanScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.SmsScan.route) {
                com.onlytrusted.app.ui.smsscan.SmsScanScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.DistressSignal.route) {
                com.onlytrusted.app.ui.distress.DistressSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.DuressSetup.route) {
                DuressSetupScreen(onDone = { navController.popBackStack() })
            }
            composable(Destination.PermissionWatchdog.route) {
                PermissionWatchdogScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.SimSwapSetup.route) {
                SimSwapSetupScreen(onDone = { navController.popBackStack() })
            }
            composable(Destination.Wangiri.route) {
                com.onlytrusted.app.ui.wangiri.WangiriSettingsScreen()
            }
            composable(Destination.SafetyCheckIn.route) {
                com.onlytrusted.app.ui.checkin.SafetyCheckInScreen()
            }
            composable(Destination.SmartLinkGuard.route) {
                com.onlytrusted.app.ui.smartlinkguard.SmartLinkGuardScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.TrackerFinder.route) {
                com.onlytrusted.app.ui.trackerfinder.TrackerFinderScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.HardwareKey.route) {
                com.onlytrusted.app.ui.hardwarekey.HardwareKeySetupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.DeadmanWipe.route) {
                com.onlytrusted.app.ui.deadmanwipe.DeadmanWipeSetupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.EncryptedBackup.route) {
                com.onlytrusted.app.ui.backup.EncryptedBackupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.BackupRestore.route) {
                com.onlytrusted.app.ui.backup.BackupRestoreScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.BorderMode.route) {
                com.onlytrusted.app.ui.bordermode.BorderModeSetupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.PanicLockdown.route) {
                com.onlytrusted.app.ui.paniclockdown.PanicLockdownScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.FaceLock.route) {
                com.onlytrusted.app.ui.facelock.FaceLockSetupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.Geofence.route) {
                com.onlytrusted.app.ui.geofence.GeofenceSetupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.ShamirSplit.route) {
                com.onlytrusted.app.ui.p2p.ShamirSetupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.TrustedWitness.route) {
                com.onlytrusted.app.ui.p2p.TrustedWitnessSetupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.StegoVault.route) {
                com.onlytrusted.app.ui.stego.StegoVaultScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.SignedLog.route) {
                com.onlytrusted.app.ui.signedlog.SignedLogScreen(
                    onBack = { navController.popBackStack() },
                    onOpenHardwareKey = {
                        navController.navigate(Destination.HardwareKey.route)
                    }
                )
            }
            composable(Destination.VoiceBio.route) {
                com.onlytrusted.app.ui.voicebio.VoiceBioSetupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.RogueNetwork.route) {
                com.onlytrusted.app.ui.roguenetwork.RogueNetworkScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.ThreatIntel.route) {
                com.onlytrusted.app.ui.threatintel.ThreatIntelScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destination.Reports.route) {
                ReportsScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.BillingSubscription.route) {
                BillingSubscriptionScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.UpgradePremium.route) {
                UpgradeToPremiumScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.Permissions.route) { PermissionAuditScreen() }
            composable(Destination.Posture.route) { PostureScreen() }
            composable(Destination.ShieldLog.route) { ShieldLogScreen() }
            composable(Destination.Legal.route) {
                LegalScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.About.route) { AboutScreen() }
            composable(Destination.Notifications.route) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
