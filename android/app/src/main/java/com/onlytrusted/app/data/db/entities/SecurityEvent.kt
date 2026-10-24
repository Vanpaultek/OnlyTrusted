// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_events")
data class SecurityEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
    val packageName: String? = null,
    val detail: String? = null,
    val prevHash: String? = null,
    val rowHash: String? = null
) {
    companion object {
        const val TYPE_MIC = "MIC_IN_USE"
        const val TYPE_CAMERA = "CAMERA_IN_USE"
        const val TYPE_CALL_BLOCKED = "CALL_BLOCKED"
        const val TYPE_CALL_ALLOWED = "CALL_ALLOWED"
        const val TYPE_SMS_FLAGGED = "SMS_FLAGGED"
        const val TYPE_POCKET_LOCK = "POCKET_LOCK"
        const val TYPE_SCREEN_LOCKED = "SCREEN_LOCKED"
        const val TYPE_SCREEN_UNLOCKED = "SCREEN_UNLOCKED"
        const val TYPE_UNTRUSTED_CALL = "UNTRUSTED_CALL"
        const val TYPE_UNTRUSTED_SMS = "UNTRUSTED_SMS"
        const val TYPE_PANIC_LOCK = "PANIC_LOCK"
        const val TYPE_SENSOR_PRIVACY_ON = "SENSOR_PRIVACY_ON"
        const val TYPE_SENSOR_PRIVACY_OFF = "SENSOR_PRIVACY_OFF"
        /** A one-ring wangiri scam call detected by [com.onlytrusted.app.ui.wangiri.WangiriWatcherService]. */
        const val TYPE_WANGIRI_BLOCKED = "WANGIRI_BLOCKED"
        /** Owner tapped "I'm safe" in the Safety Check-In Pro screen. */
        const val TYPE_SAFETY_CHECKIN_SENT = "SAFETY_CHECKIN_SENT"
        /** SIM-Swap Alarm detected a different carrier identity than the stored baseline. */
        const val TYPE_SIM_SWAP = "SIM_SWAP"
        /** Fired by the Dead-Man Wipe (Elite) just before the full-state wipe runs. */
        const val TYPE_WIPE = "WIPE"
        /** Encrypted vault backup (Elite) was restored from a `.ot2bak` file. */
        const val TYPE_BACKUP_RESTORED = "BACKUP_RESTORED"
        /** Geo-fence Auto-Lock (Elite): device exited every trusted region. */
        const val TYPE_GEOFENCE_EXIT = "GEOFENCE_EXIT"
        /** Panic Lockdown (Elite): owner tapped the red "activate" button. */
        const val TYPE_PANIC_LOCKDOWN_ACTIVATED = "PANIC_LOCKDOWN_ACTIVATED"
        /** Panic Lockdown (Elite): owner disarmed with biometric + PIN (+ hardware key). */
        const val TYPE_PANIC_LOCKDOWN_DISARMED = "PANIC_LOCKDOWN_DISARMED"
        /** Distress Signal fired from the Dashboard SOS button (or future shake/volume trigger). */
        const val TYPE_DISTRESS_FIRED = "DISTRESS_FIRED"
    }
}
