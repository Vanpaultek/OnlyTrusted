// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Intermediate tier between Consumer and Device Owner. Uses the user-granted
 * DeviceAdmin privilege (prompted via `ACTION_ADD_DEVICE_ADMIN` system dialog
 * — no factory reset needed) to call `DevicePolicyManager.setCameraDisabled()`.
 *
 * What this tier CAN do (that Consumer cannot):
 *   - `setCameraDisabled(admin, true/false)` — globally disables the camera
 *     for all apps while active. Does NOT require Device Owner provisioning.
 *   - Combined with AudioManager mic mute from Consumer tier, delivers a
 *     credible "mic muted + camera cut when locked" experience on any
 *     Android phone the user is willing to grant Device Admin to.
 *
 * What this tier CANNOT do (that Device Owner can):
 *   - `addUserRestriction(DISALLOW_UNMUTE_MICROPHONE)` — Device-Owner-only.
 *     We fall back to AudioManager.setMicrophoneMute() which is weaker but
 *     works for a Play Store install.
 *
 * Fails loudly if DeviceAdmin isn't granted — the UI should check [isActive]
 * before offering this controller. The user grants the privilege via the
 * `ACTION_ADD_DEVICE_ADMIN` prompt (see [requestActivation]).
 */
class DeviceAdminSensorPrivacyController(
    private val context: Context,
) : SensorPrivacyController {

    override val canEnforceGlobally: Boolean get() = isActive(context)
    override val tierLabel = "Device Admin"

    private val adminComponent = ComponentName(context, OTDeviceAdminReceiver::class.java)
    private val dpm: DevicePolicyManager
        get() = context.getSystemService(DevicePolicyManager::class.java)
            ?: error("DevicePolicyManager unavailable on this device")

    @Volatile private var weDisabledCamera = false
    @Volatile private var weMutedMic = false

    override suspend fun readState(): SensorState = withContext(Dispatchers.IO) {
        if (!isActive(context)) return@withContext SensorState.UNKNOWN
        val cameraDisabled = runCatching { dpm.getCameraDisabled(adminComponent) }.getOrDefault(false)
        val micMuted = runCatching { micBlockedFromAudioManager() }.getOrDefault(null)
        SensorState(microphoneBlocked = micMuted, cameraBlocked = cameraDisabled)
    }

    override suspend fun enforceLock(): SensorState = withContext(Dispatchers.IO) {
        require(isActive(context)) {
            "DeviceAdminSensorPrivacyController called without Device Admin privilege. " +
                "Use requestActivation() first."
        }

        // Camera via DPM (the win over Consumer tier).
        val before = readState()
        if (before.cameraBlocked == false) {
            runCatching {
                dpm.setCameraDisabled(adminComponent, true)
                weDisabledCamera = true
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_ENFORCED,
                    detail = "camera disabled on lock (DeviceAdmin DPM).",
                )
            }.onFailure {
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_REFUSED,
                    detail = "camera disable refused: ${it.message}",
                )
            }
        }

        // Microphone: AudioManager fallback (Device Owner path uses
        // DISALLOW_UNMUTE_MICROPHONE instead; we don't have that privilege here).
        val am = context.getSystemService(android.media.AudioManager::class.java)
        if (am != null && before.microphoneBlocked != true) {
            runCatching {
                am.isMicrophoneMute = true
                weMutedMic = true
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_ENFORCED,
                    detail = "AudioManager mic mute on lock (DeviceAdmin tier).",
                )
            }
        }

        SensorPrivacyLog.append(
            context,
            kind = SensorPrivacyLog.Kind.SCREEN_LOCK,
            detail = "Device Admin tier — camera cut via DPM, mic via AudioManager.",
        )
        readState()
    }

    override suspend fun restoreAfterUnlock(): SensorState = withContext(Dispatchers.IO) {
        require(isActive(context)) { "Cannot restore without Device Admin privilege." }

        if (weDisabledCamera) {
            runCatching {
                dpm.setCameraDisabled(adminComponent, false)
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_RESTORED,
                    detail = "camera re-enabled on unlock (DeviceAdmin DPM).",
                )
            }
            weDisabledCamera = false
        }
        if (weMutedMic) {
            runCatching {
                context.getSystemService(android.media.AudioManager::class.java)?.isMicrophoneMute = false
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_RESTORED,
                    detail = "AudioManager mic un-muted on unlock.",
                )
            }
            weMutedMic = false
        }

        SensorPrivacyLog.append(
            context,
            kind = SensorPrivacyLog.Kind.SCREEN_UNLOCK,
            detail = "Device Admin tier — restoration complete.",
        )
        readState()
    }

    private fun micBlockedFromAudioManager(): Boolean? {
        val am = context.getSystemService(android.media.AudioManager::class.java) ?: return null
        return runCatching { am.isMicrophoneMute }.getOrNull()
    }

    companion object {
        /** True iff this app currently holds DeviceAdmin on this device. Note:
         *  DeviceAdmin is a WEAKER privilege than Device Owner. The user grants
         *  it via a system dialog (no factory reset required). */
        fun isActive(context: Context): Boolean {
            val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
            val component = ComponentName(context, OTDeviceAdminReceiver::class.java)
            return runCatching { dpm.isAdminActive(component) }.getOrDefault(false)
        }

        /** Builds the Intent that opens the DeviceAdmin activation dialog.
         *  Caller must start it via `startActivity(...)` from an Activity (not
         *  a Service context, which would need FLAG_ACTIVITY_NEW_TASK). */
        fun buildActivationIntent(context: Context, explanation: String): Intent {
            val component = ComponentName(context, OTDeviceAdminReceiver::class.java)
            return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)
            }
        }

        /** Builds the Intent that opens the DeviceAdmin removal flow — user
         *  revokes the privilege via Settings → Security → Device admin apps. */
        fun removeActiveAdmin(context: Context) {
            val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return
            val component = ComponentName(context, OTDeviceAdminReceiver::class.java)
            runCatching { dpm.removeActiveAdmin(component) }
        }
    }
}
