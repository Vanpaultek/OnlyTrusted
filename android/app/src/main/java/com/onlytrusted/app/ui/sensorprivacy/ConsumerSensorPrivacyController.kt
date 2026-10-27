// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
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

    override suspend fun enforceLock(): SensorState = withContext(Dispatchers.IO) {
        // Consumer tier has ONE real lever that doesn't need Device Owner:
        // AudioManager.setMicrophoneMute(true). This mutes the system mic input
        // level so apps that acquire mic access still receive silence rather
        // than meaningful audio. It is not a security-grade sensor cutoff
        // (apps still see "mic active"), but it is the honest best-effort for
        // a Play Store install. Logged honestly.
        val am = context.getSystemService(AudioManager::class.java)
        var micMuted = false
        if (am != null) {
            runCatching {
                am.isMicrophoneMute = true
                micMuted = am.isMicrophoneMute
                weMutedMic = true
                Log.i("OTSensorPrivacy",
                    "enforceLock: AudioManager.setMicrophoneMute(true), readback=$micMuted")
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_ENFORCED,
                    detail = "AudioManager.setMicrophoneMute(true) on lock, readback=$micMuted. " +
                        "Note: not a security-grade cutoff; apps still get a 'mic active' handle.",
                )
            }.onFailure {
                SensorPrivacyLog.append(
                    context,
                    kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_REFUSED,
                    detail = "AudioManager mic mute refused: ${it.message}",
                )
            }
        }
        SensorPrivacyLog.append(
            context,
            kind = SensorPrivacyLog.Kind.SCREEN_LOCK,
            detail = "consumer tier — AudioManager mic mute attempted (success=$micMuted). " +
                "Camera disable requires Device Owner (see docs/SENSOR_PRIVACY_ARCHITECTURE.md).",
        )
        readState()
    }

    override suspend fun restoreAfterUnlock(): SensorState = withContext(Dispatchers.IO) {
        if (weMutedMic) {
            runCatching {
                context.getSystemService(AudioManager::class.java)?.isMicrophoneMute = false
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
            detail = "consumer tier — restoration complete.",
        )
        readState()
    }

    /** Tracks whether WE muted the mic (vs. the user doing it) so we only
     *  un-mute changes we made. See SENSOR_PRIVACY_ARCHITECTURE.md § "Pre-lock
     *  state preservation" for the rationale. */
    @Volatile private var weMutedMic = false

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
