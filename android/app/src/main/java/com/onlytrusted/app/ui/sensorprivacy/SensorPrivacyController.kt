// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

/**
 * Snapshot of mic + camera privacy state. `null` for a sensor means "unknown
 * on this install tier" — the UI should render that as "Unknown" rather than
 * guessing. See docs/SENSOR_PRIVACY_ARCHITECTURE.md for the full contract.
 */
data class SensorState(
    val microphoneBlocked: Boolean?,
    val cameraBlocked: Boolean?,
) {
    companion object {
        val UNKNOWN = SensorState(microphoneBlocked = null, cameraBlocked = null)
    }
}

/**
 * Shared controller contract. Implementations live under this package:
 *   - [ConsumerSensorPrivacyController]    — Play Store install, read-only + deep-links
 *   - [DeviceOwnerSensorPrivacyController] — Device Owner enrolment, true enforcement
 *   - [PrivilegedSensorPrivacyController]  — OEM / custom ROM only, skeleton
 *
 * The UI picks the controller to display based on which tier is actually
 * active (checked via `DevicePolicyManager.isDeviceOwnerApp()` and whether
 * `MANAGE_SENSOR_PRIVACY` is held).
 */
interface SensorPrivacyController {
    /** Current mic + camera privacy state as this tier can read it. */
    suspend fun readState(): SensorState

    /** Called on screen-lock. See impl notes for what each tier does. */
    suspend fun enforceLock(): SensorState

    /** Called on screen-unlock. Restores pre-lock state (own changes only). */
    suspend fun restoreAfterUnlock(): SensorState

    /**
     * `false` for the Consumer tier. The UI uses this to decide whether to
     * render a "we actually enforce this" badge or an honest "Android limits
     * what any app can do here" note.
     */
    val canEnforceGlobally: Boolean

    /** Tier label shown in the UI ("Consumer" / "Device Owner" / "Privileged"). */
    val tierLabel: String
}
