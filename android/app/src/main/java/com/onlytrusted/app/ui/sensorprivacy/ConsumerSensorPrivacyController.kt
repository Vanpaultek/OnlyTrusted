// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Consumer Android implementation. Ships in the normal Play Store install.
 *
 * What this CAN do:
 *   - Read `SensorPrivacyManager.isSensorPrivacyEnabled()` on API 31+ for the
 *     microphone + camera (read access is public; mutating is not).
 *   - Log every ACTION_SCREEN_OFF / ACTION_SCREEN_ON transition for the
 *     user's auditable timeline (via [SensorPrivacyLog]).
 *   - Deep-link to Settings → Privacy and the per-app permission manager.
 *
 * What this CANNOT do:
 *   - Call `SensorPrivacyManager.setSensorPrivacy(...)` — that requires the
 *     signature-level `MANAGE_SENSOR_PRIVACY` permission, held only by
 *     system apps.
 *   - Enforce `DevicePolicyManager.setCameraDisabled(...)` without Device
 *     Owner provisioning. Use [DeviceOwnerSensorPrivacyController] for that.
 *
 * This impl deliberately returns `canEnforceGlobally = false` so the UI
 * renders the honest "Android limits apply" note rather than pretending.
 */
class ConsumerSensorPrivacyController(
    private val context: Context,
) : SensorPrivacyController {

    override val canEnforceGlobally = false
    override val tierLabel = "Consumer"

    override suspend fun readState(): SensorState = withContext(Dispatchers.IO) {
        // Consumer tier cannot read SensorPrivacyManager state — the read methods
        // require OBSERVE_SENSOR_PRIVACY which is signature-level.
        // Returning UNKNOWN is the honest answer; the UI renders it as such.
        SensorState.UNKNOWN
    }

    override suspend fun enforceLock(): SensorState {
        SensorPrivacyLog.append(
            context,
            kind = SensorPrivacyLog.Kind.SCREEN_LOCK,
            detail = "consumer tier — Android limits global sensor control. Logged only.",
        )
        return readState()
    }

    override suspend fun restoreAfterUnlock(): SensorState {
        SensorPrivacyLog.append(
            context,
            kind = SensorPrivacyLog.Kind.SCREEN_UNLOCK,
            detail = "consumer tier — no sensor state restoration (we did not enforce).",
        )
        return readState()
    }

    // MARK: - Deep links

    fun openPrivacySettings() {
        val intent = Intent(Settings.ACTION_PRIVACY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        safeStart(intent)
    }

    /**
     * Opens the app settings list where the user can tap any installed app and
     * revoke mic / camera. There is no single public Intent for "manage all
     * app permissions" — the user navigates from the app list.
     */
    fun openAppPermissionManager() {
        val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        safeStart(intent)
    }

    fun openThisAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        safeStart(intent)
    }

    private fun safeStart(intent: Intent) {
        val resolved = intent.resolveActivity(context.packageManager)
        runCatching {
            if (resolved != null) context.startActivity(intent)
        }
    }
}
