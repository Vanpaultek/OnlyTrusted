// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OEM-only skeleton. This class is defined with the intended API surface
 * so a future OnlyTrusted build that ships as a pre-installed system app
 * (signed with the platform key or placed in /system/priv-app) can slot
 * into the same controller contract.
 *
 * On a normal install, [isActive] returns false and all mutating methods
 * log a SENSOR_PRIVACY_REFUSED event rather than throwing — safer for the
 * UI, which can show a "not available on this build" banner.
 *
 * Why not use this on Device Owner? Device Owner can enforce the camera
 * via DevicePolicyManager + mic via DISALLOW_UNMUTE_MICROPHONE, but the
 * privileged tier gives direct access to SensorPrivacyManager which:
 *   - works on secondary users (DO is primary-user only)
 *   - is more granular (per-sensor without full device policy footprint)
 *   - fires the official SensorPrivacyManager listeners other apps watch
 *
 * See docs/SENSOR_PRIVACY_ARCHITECTURE.md § "Tier 3 — Privileged".
 */
class PrivilegedSensorPrivacyController(
    private val context: Context,
) : SensorPrivacyController {

    override val canEnforceGlobally: Boolean get() = isActive(context)
    override val tierLabel = "Privileged (OEM)"

    @Volatile private var preLockSnapshot: SensorState? = null

    override suspend fun readState(): SensorState = withContext(Dispatchers.IO) {
        // The real impl would reflect against SensorPrivacyManager.isSensorPrivacyEnabled()
        // once the signature-level OBSERVE_SENSOR_PRIVACY permission is granted (OEM build).
        // Returning UNKNOWN keeps the consumer build safe; the UI renders "Unknown" instead
        // of guessing.
        SensorState.UNKNOWN
    }

    override suspend fun enforceLock(): SensorState = withContext(Dispatchers.IO) {
        if (!isActive(context)) {
            SensorPrivacyLog.append(
                context,
                kind = SensorPrivacyLog.Kind.SENSOR_PRIVACY_REFUSED,
                detail = "privileged tier called without MANAGE_SENSOR_PRIVACY. Build-time setup required.",
            )
            return@withContext readState()
        }
        val before = readState()
        preLockSnapshot = before
        // TODO: on a system build, call setSensorPrivacy(..., true) for mic + cam here.
        // The hidden method lives at SensorPrivacyManager via reflection — intentionally
        // not wired on the consumer build to avoid tripping Play Protect's non-SDK API
        // detection.
        SensorPrivacyLog.append(
            context,
            kind = SensorPrivacyLog.Kind.SCREEN_LOCK,
            detail = "privileged tier — would enforce via SensorPrivacyManager if signature-permission granted.",
        )
        readState()
    }

    override suspend fun restoreAfterUnlock(): SensorState = withContext(Dispatchers.IO) {
        // Mirror of enforceLock(): defensive stub for the consumer build.
        SensorPrivacyLog.append(
            context,
            kind = SensorPrivacyLog.Kind.SCREEN_UNLOCK,
            detail = "privileged tier — would restore pre-lock snapshot if active.",
        )
        preLockSnapshot = null
        readState()
    }

    companion object {
        /** True iff this build holds the signature-level MANAGE_SENSOR_PRIVACY
         *  permission (only possible when OnlyTrusted is pre-installed as a
         *  system app and signed with the platform key). */
        fun isActive(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
            val granted = context.checkCallingOrSelfPermission(
                "android.permission.MANAGE_SENSOR_PRIVACY"
            )
            return granted == PackageManager.PERMISSION_GRANTED
        }
    }
}
