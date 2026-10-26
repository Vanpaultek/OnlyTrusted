// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Device Owner implementation. Only usable on an Android device that has been
 * provisioned with `dpm set-device-owner com.onlytrusted.app/.ui.sensorprivacy.OTDeviceAdminReceiver`
 * (requires factory reset + USB debug). See docs/SENSOR_PRIVACY_ARCHITECTURE.md
 * for the one-time flow.
 *
 * What this CAN do (that the Consumer tier cannot):
 *   - `DevicePolicyManager.setCameraDisabled(admin, true)` — globally disables
 *     the camera for all apps while active.
 *   - `addUserRestriction(DISALLOW_UNMUTE_MICROPHONE)` — locks the mic muted
 *     at the OS level; user cannot unmute via Quick Settings.
 *
 * What this tier respects:
 *   - Pre-lock state is snapshotted + only our own changes are reversed on
 *     unlock. If the user had the mic muted before lock, it stays muted
 *     after unlock.
 *
 * Fails loudly if the app is not actually Device Owner — the UI should
 * check [isActive] before offering this controller.
 */
class DeviceOwnerSensorPrivacyController(
    private val context: Context,
) : SensorPrivacyController {

    override val canEnforceGlobally: Boolean get() = isActive(context)
    override val tierLabel = "Device Owner"

    private val adminComponent = ComponentName(context, OTDeviceAdminReceiver::class.java)
    private val dpm: DevicePolicyManager
        get() = context.getSystemService(DevicePolicyManager::class.java)
            ?: error("DevicePolicyManager unavailable on this device")

    /** Snapshot of state we observed BEFORE the most recent lock. Used to
     *  decide what to restore on unlock — if the user had the mic muted
     *  pre-lock, we don't un-mute them on unlock. */
    @Volatile private var preLockSnapshot: SensorState? = null
    @Volatile private var weMutedMic = false
    @Volatile private var weDisabledCamera = false

    override suspend fun readState(): SensorState = withContext(Dispatchers.IO) {
        require(isActive(context)) {
            "DeviceOwnerSensorPrivacyController called while OnlyTrusted is NOT Device Owner. " +
                "See docs/SENSOR_PRIVACY_ARCHITECTURE.md for provisioning."
        }
        val cameraDisabled = runCatching { dpm.getCameraDisabled(adminComponent) }.getOrDefault(false)
        val micMuted = runCatching { micBlockedFromRestriction() }.getOrDefault(null)
        SensorState(microphoneBlocked = micMuted, cameraBlocked = cameraDisabled)
    }

    override suspend fun enforceLock(): SensorState = withContext(Dispatchers.IO) {
        require(isActive(context)) {
            "Cannot enforce lock without Device Owner privilege."
        }
        val before = readState()
        preLockSnapshot = before

        // Camera: only disable if currently enabled. Preserve user intent.
        if (before.cameraBlocked == false) {
            runCatching {
                dpm.setCameraDisabled(adminComponent, true)
                weDisabledCamera = true
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_ENFORCED,
                    detail = "camera disabled on lock (DevicePolicyManager).",
                )
            }.onFailure {
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_REFUSED,
                    detail = "camera disable refused: ${it.message}",
                )
            }
        }

        // Microphone: add DISALLOW_UNMUTE_MICROPHONE only if not already set.
        if (before.microphoneBlocked != true) {
            runCatching {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_UNMUTE_MICROPHONE)
                weMutedMic = true
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_ENFORCED,
                    detail = "mic locked muted on lock (DISALLOW_UNMUTE_MICROPHONE).",
                )
            }.onFailure {
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_REFUSED,
                    detail = "mic mute refused: ${it.message}",
                )
            }
        }

        SensorPrivacyLog.append(
            context,
            kind = SensorPrivacyLog.Kind.SCREEN_LOCK,
            detail = "Device Owner tier — mic=${before.microphoneBlocked}, cam=${before.cameraBlocked}",
        )
        readState()
    }

    override suspend fun restoreAfterUnlock(): SensorState = withContext(Dispatchers.IO) {
        require(isActive(context)) { "Cannot restore without Device Owner privilege." }

        // Camera: only re-enable if WE disabled it (don't fight user intent).
        if (weDisabledCamera) {
            runCatching {
                dpm.setCameraDisabled(adminComponent, false)
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_RESTORED,
                    detail = "camera re-enabled on unlock.",
                )
            }
            weDisabledCamera = false
        }

        // Microphone: only clear the restriction if WE added it.
        if (weMutedMic) {
            runCatching {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_UNMUTE_MICROPHONE)
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_RESTORED,
                    detail = "mic restriction cleared on unlock.",
                )
            }
            weMutedMic = false
        }

        SensorPrivacyLog.append(
            context,
            kind = SensorPrivacyLog.Kind.SCREEN_UNLOCK,
            detail = "Device Owner tier — restoration complete.",
        )
        preLockSnapshot = null
        readState()
    }

    /** Reads whether DISALLOW_UNMUTE_MICROPHONE is currently applied to this user.
     *  Public API via UserManager; works for any caller, not just Device Owner. */
    private fun micBlockedFromRestriction(): Boolean? {
        val um = context.getSystemService(UserManager::class.java) ?: return null
        return runCatching {
            um.hasUserRestriction(UserManager.DISALLOW_UNMUTE_MICROPHONE)
        }.getOrNull()
    }

    companion object {
        /** True iff OnlyTrusted is the current Device Owner on this device. */
        fun isActive(context: Context): Boolean {
            val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
            return runCatching { dpm.isDeviceOwnerApp(context.packageName) }.getOrDefault(false)
        }

        /** The exact `dpm set-device-owner` component string. */
        fun provisioningComponentString(packageName: String): String =
            "$packageName/com.onlytrusted.app.ui.sensorprivacy.OTDeviceAdminReceiver"
    }
}
