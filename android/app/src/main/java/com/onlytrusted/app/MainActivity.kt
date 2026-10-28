// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.onlytrusted.app.privacy.OnboardingState
import com.onlytrusted.app.security.AppLockManager
import com.onlytrusted.app.services.PocketDetectionService
import com.onlytrusted.app.services.SensorMonitorService
import com.onlytrusted.app.ui.lock.AppLockGate
import com.onlytrusted.app.ui.navigation.AppNavigation
import com.onlytrusted.app.ui.onboarding.ConsentScreen
import com.onlytrusted.app.ui.onboarding.OnboardingScreen
import com.onlytrusted.app.ui.onboarding.ProfileSetupScreen
import com.onlytrusted.app.ui.onboarding.ProfileStore
import com.onlytrusted.app.ui.settings.SettingsViewModel
import com.onlytrusted.app.ui.splash.SplashScreen
import com.onlytrusted.app.ui.theme.OnlyTrustedTheme
import com.onlytrusted.app.util.PermissionHelper

class MainActivity : FragmentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onPermissionsResult() }

    // App-wide distress triggers — detectors live here so they fire from any screen,
    // not just Dashboard. Pattern-match → DistressTriggerBus.fire() → the top-level
    // overlay composable in setContent catches it, shows the countdown, and runs
    // DistressDispatcher.dispatch() on fire.
    private val shakeDetector by lazy {
        com.onlytrusted.app.ui.distress.ShakeDetector(
            onShakePatternDetected = { com.onlytrusted.app.ui.distress.DistressTriggerBus.fire() }
        )
    }
    private val volumeDownTrigger = com.onlytrusted.app.ui.distress.VolumeDownTrigger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyScreenshotProtection()

        // Dead-Man Wipe (Elite): if the inactivity deadline has elapsed, wipe before the UI
        // mounts. Runs synchronously on the IO dispatcher — the Composable will see a
        // freshly-emptied database when it reads from the repository.
        if (com.onlytrusted.app.deadmanwipe.DeadmanWipeState.shouldWipeNow(this)) {
            kotlinx.coroutines.runBlocking {
                com.onlytrusted.app.deadmanwipe.DeadmanWipePerformer.performWipe(this@MainActivity)
            }
        } else if (com.onlytrusted.app.deadmanwipe.DeadmanWipeState.isEnabled(this)) {
            // Opportunistic re-schedule in case the user upgraded the app and the workers
            // were cleared by Play's install step.
            com.onlytrusted.app.deadmanwipe.DeadmanWipeScheduler.rescheduleAll(this)
        }

        setContent {
            OnlyTrustedTheme {
                var splashDone by remember { mutableStateOf(false) }
                // Initial state reads below hit EncryptedSharedPreferences (ProfileStore)
                // which triggers Tink master-key resolution on first launch — can be
                // 100–500 ms. Hoisted off the composition thread into a LaunchedEffect
                // so the splash renders immediately; the gate below holds the splash in
                // place until the IO read completes (practically always finishes within
                // the splash's 1400 ms window).
                var initialStateLoaded by remember { mutableStateOf(false) }
                var onboarded by remember { mutableStateOf(false) }
                var consented by remember { mutableStateOf(false) }
                var profileSetup by remember { mutableStateOf(false) }
                val lifecycleOwner = LocalLifecycleOwner.current
                var unlocked by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val ob: Boolean; val cn: Boolean
                    val pr: Boolean; val un: Boolean
                    withContext(Dispatchers.IO) {
                        ob = OnboardingState.hasOnboarded(this@MainActivity)
                        cn = OnboardingState.hasAccepted(this@MainActivity)
                        pr = ProfileStore.isSetupComplete(this@MainActivity)
                        un = !AppLockManager.shouldPrompt(this@MainActivity)
                    }
                    onboarded = ob
                    consented = cn
                    profileSetup = pr
                    unlocked = un
                    initialStateLoaded = true
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME &&
                            AppLockManager.shouldPrompt(this@MainActivity)
                        ) {
                            unlocked = false
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // App-wide Distress Signal overlay — any trigger (SOS FAB, shake,
                // volume-down 4×) publishes to DistressTriggerBus; here we collect it
                // and show the countdown + result dialog ABOVE whatever screen is
                // currently rendered. Lives at Activity level so the overlay works
                // from any route, not just Dashboard.
                var showDistressCountdown by remember { mutableStateOf(false) }
                var distressResult by remember { mutableStateOf<String?>(null) }
                val activityScope = rememberCoroutineScope()
                val distressCtx = this@MainActivity
                LaunchedEffect(Unit) {
                    com.onlytrusted.app.ui.distress.DistressTriggerBus.events.collect {
                        showDistressCountdown = true
                    }
                }

                when {
                    !splashDone || !initialStateLoaded ->
                        SplashScreen(onDone = { splashDone = true })
                    !onboarded -> OnboardingScreen(onContinue = { onboarded = true })
                    !consented -> ConsentScreen(onConfirmed = {
                        consented = true
                        // Fire the batched Android permission dialogs right
                        // here — user has just explicitly agreed to use the
                        // app, so this is the honest moment to ask. If any
                        // are already granted, Android stays silent for
                        // those; the user only sees prompts for the ones
                        // still missing.
                        requestCorePermissionsIfNeeded()
                    })
                    !profileSetup -> ProfileSetupScreen(onComplete = { profileSetup = true })
                    !unlocked -> AppLockGate(onUnlocked = {
                        AppLockManager.markUnlocked(this)
                        unlocked = true
                    })
                    else -> AppNavigation(onPanic = {
                        AppLockManager.resetUnlock(this)
                        unlocked = false
                    })
                }

                if (showDistressCountdown) {
                    com.onlytrusted.app.ui.distress.DistressCountdownOverlay(
                        onCancel = { showDistressCountdown = false },
                        onFire = {
                            showDistressCountdown = false
                            activityScope.launch {
                                distressResult = com.onlytrusted.app.ui.distress
                                    .DistressDispatcher.dispatch(distressCtx)
                            }
                        }
                    )
                }
                distressResult?.let { msg ->
                    AlertDialog(
                        onDismissRequest = { distressResult = null },
                        title = { Text("EMERGENCY alert") },
                        text = { Text(msg) },
                        confirmButton = {
                            TextButton(onClick = { distressResult = null }) { Text("OK") }
                        }
                    )
                }
            }
        }

        // v0.1.9 removed the in-app PermissionsOverviewScreen entirely —
        // the only "ask" the user sees is Android's own system dialogs,
        // matching WhatsApp / Signal UX. The batched request is kicked off
        // from the Consent screen's onConfirmed path (see setContent below)
        // so it fires after the user has agreed to use the app, not before
        // Onboarding renders.
    }

    override fun onResume() {
        super.onResume()
        applyScreenshotProtection()
        maybeStartServices()
        // Start shake detector on resume if the user hasn't disabled it. Stopped in
        // onPause so we don't sample the accelerometer while the app is backgrounded.
        if (com.onlytrusted.app.ui.distress.ShakeDetector.Prefs.isEnabled(this)) {
            shakeDetector.start(this)
        }
    }

    override fun onPause() {
        super.onPause()
        shakeDetector.stop()
    }

    /**
     * Volume-down 4× distress trigger. Fires only when the user has enabled it in
     * Settings → Distress Signal (default OFF — swallowing a volume press is surprising).
     * When the pattern matches we return true to consume the event; the final press
     * does not actually change the volume. This mirrors what the user asked for:
     * "press volume-down 4 times → SOS fires".
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN &&
            com.onlytrusted.app.ui.distress.VolumeDownTrigger.Prefs.isEnabled(this)
        ) {
            if (volumeDownTrigger.consume(keyCode)) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun applyScreenshotProtection() {
        if (SettingsViewModel.isScreenshotProtectionEnabled(application)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun requestCorePermissionsIfNeeded() {
        val missing = PermissionHelper.missing(this)
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            onPermissionsResult()
        }
    }

    private fun onPermissionsResult() {
        maybeStartServices()
    }

    private fun maybeStartServices() {
        if (!canPostNotifications()) return

        if (SettingsViewModel.isSensorMonitorEnabled(application) && hasSensorPrerequisites()) {
            SensorMonitorService.start(this)
        }
        if (SettingsViewModel.isPocketDetectionEnabled(application)) {
            PocketDetectionService.start(this)
        }
        // Wangiri Protection (Pro): the service is a plain started Service — it no-ops if
        // READ_PHONE_STATE isn't granted, so this is safe to call unconditionally.
        com.onlytrusted.app.ui.wangiri.WangiriWatcherService.startIfEligible(this)
        // Tracker Finder (Pro) continuous mode. The service self-gates on Pro + the
        // continuous toggle so this is safe to call unconditionally; it exits immediately
        // if either is off.
        com.onlytrusted.app.ui.trackerfinder.TrackerFinderService.startIfEligible(this)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasSensorPrerequisites(): Boolean {
        val micOk = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val camOk = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        return micOk || camOk
    }

}
