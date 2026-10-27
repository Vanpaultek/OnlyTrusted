// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Launcher-shortcut target. Opens as a transparent, no-UI activity, runs
 * enforceLock() on the strongest active sensor-privacy controller, and —
 * if OnlyTrusted holds Device Admin — immediately locks the screen too.
 *
 * Entry points:
 *   - Long-press OnlyTrusted icon → "Lock + Mute Now" static shortcut.
 *   - Drag the static shortcut to the home screen for one-tap access.
 *
 * Zero-UI by design — the activity finishes within ~100ms of launch so the
 * user sees the launcher snap back, with a brief Toast confirming the action.
 *
 * This path is COMPLEMENTARY to the foreground-service auto-enforcement.
 * Users who prefer not to run the FGS can trigger protection manually via
 * this shortcut whenever they lock the phone.
 */
class LockAndMuteActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = SensorPrivacyForegroundService.pickController(this)

        scope.launch {
            val before = controller.readState()
            controller.enforceLock()
            SensorPrivacyLog.append(
                this@LockAndMuteActivity,
                kind = "SHORTCUT_TRIGGERED",
                detail = "Lock + Mute Now shortcut tapped (tier=${controller.tierLabel}).",
            )
            val afterSummary = buildString {
                append("Mic muted")
                if (before.cameraBlocked == false) append(" · camera disabled")
            }
            runOnUiThread {
                Toast.makeText(this@LockAndMuteActivity, afterSummary, Toast.LENGTH_SHORT).show()
            }
            // If Device Admin is active, also lock the screen immediately —
            // completes the "Lock + Mute Now" promise literally.
            if (DeviceAdminSensorPrivacyController.isActive(this@LockAndMuteActivity)) {
                val dpm = getSystemService(DevicePolicyManager::class.java)
                runCatching { dpm?.lockNow() }
            }
            finish()
        }
    }
}
