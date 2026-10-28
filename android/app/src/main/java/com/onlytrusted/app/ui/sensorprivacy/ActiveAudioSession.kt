// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import android.media.AudioManager
import android.os.Build

/**
 * Lightweight guard that answers "is the user currently using the phone's
 * audio pipeline in a way that cutting the mic / camera would disrupt a
 * legitimate activity?"
 *
 * Used by [ScreenStateReceiver] to skip `enforceLock()` on screen-off when
 * the user is clearly mid-call or mid-playback **by their own action**.
 *
 * ### Honest-call vs. silent-activation
 *
 * The old (v0.1.4) implementation trusted `AudioManager.MODE_IN_COMMUNICATION`
 * unconditionally. A malicious app could flip the system into that mode
 * silently to bypass our auto-lock. Now we only trust VoIP mode when
 * [MicActivityTracker] confirms the recording started while the screen was
 * on and has been active long enough to look like a user-accepted call.
 *
 * The signals and how they are classified:
 *
 *  - `MODE_IN_CALL` → cellular call. The modem can't be forged by a
 *    third-party app, so this is always trusted → **skip lock**.
 *  - `MODE_RINGTONE` → incoming call ringing (cellular or ConnectionService
 *    VoIP). Also radio / Telecom-anchored and unforgeable → **skip lock**.
 *  - `MODE_IN_COMMUNICATION` → VoIP mode. **Only trusted when
 *    [MicActivityTracker.legitimateCallSession] returns non-null**.
 *    Otherwise classified as *silent-activation* and the lock proceeds.
 *  - `AudioManager.isMusicActive()` → media is playing on a music stream.
 *    No mic implications; **skip lock**.
 *
 * Explicit user-initiated lock paths (Quick Settings tile, launcher
 * shortcut, Siri / Assistant intent, in-app "Lock Now" button)
 * **intentionally bypass this guard** because they represent an explicit
 * intent that overrides any heuristic.
 */
internal object ActiveAudioSession {

    /**
     * @param userAcceptedVoipCall VoIP mode is set AND a legitimate call
     *   session was observed. Safe to skip the lock.
     * @param suspiciousVoipMode VoIP mode is set but no legitimate session
     *   is active. An app flipped the system into communication mode
     *   without the user doing anything — do NOT skip the lock.
     * @param reason Human-readable short description for the audit log.
     *   Does not include package names.
     */
    data class Snapshot(
        val onCellularCall: Boolean,
        val ringing: Boolean,
        val userAcceptedVoipCall: Boolean,
        val suspiciousVoipMode: Boolean,
        val mediaPlaying: Boolean,
    ) {
        /** True iff we should defer the lock. Suspicious VoIP is NOT busy. */
        val shouldDeferLock: Boolean
            get() = onCellularCall || ringing || userAcceptedVoipCall || mediaPlaying

        fun reason(): String = buildList {
            if (onCellularCall) add("cellular call")
            if (ringing) add("incoming call ringing")
            if (userAcceptedVoipCall) add("VoIP call (user-accepted)")
            if (suspiciousVoipMode) add("VoIP mode set WITHOUT user call — not trusted")
            if (mediaPlaying) add("media playing")
        }.joinToString(", ").ifBlank { "idle" }
    }

    fun snapshot(context: Context): Snapshot {
        val am = context.applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return Snapshot(false, false, false, false, false)
        val mode = am.mode
        val inComm = mode == AudioManager.MODE_IN_COMMUNICATION
        val legit = if (inComm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MicActivityTracker.legitimateCallSession() != null
        } else {
            // Below API 29 we can't resolve the recording package name, so
            // we have no way to run the behavioural check. Fall back to the
            // old behaviour (trust the mode) on legacy devices to avoid
            // breaking calls there. API 29+ gets the stronger check.
            inComm && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        }
        return Snapshot(
            onCellularCall = mode == AudioManager.MODE_IN_CALL,
            ringing = mode == AudioManager.MODE_RINGTONE,
            userAcceptedVoipCall = inComm && legit,
            suspiciousVoipMode = inComm && !legit,
            mediaPlaying = am.isMusicActive,
        )
    }
}
