// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

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
        /** TelecomManager says a call is active (cellular OR
         *  ConnectionService-backed VoIP, e.g. WhatsApp / WhatsApp Business
         *  / Signal on API 23+). Unforgeable by third-party apps. */
        val telecomInCall: Boolean,
        /** TelephonyManager.callState reports off-hook (cellular call). */
        val telephonyOffHook: Boolean,
        /** Any active mic recording is tagged as a call-grade audio source
         *  (`VOICE_COMMUNICATION` or `VOICE_CALL`). Catches VoIP apps that
         *  don't flip `AudioManager.mode` into communication mode but DO
         *  tag their mic session correctly. */
        val callGradeRecording: Boolean,
    ) {
        /** True iff we should defer the lock. */
        val shouldDeferLock: Boolean
            get() = onCellularCall || ringing || userAcceptedVoipCall ||
                mediaPlaying || telecomInCall || telephonyOffHook ||
                callGradeRecording

        fun reason(): String = buildList {
            if (onCellularCall) add("cellular call (AudioManager)")
            if (telephonyOffHook) add("cellular call (TelephonyManager)")
            if (telecomInCall) add("Telecom in-call (cellular or ConnectionService VoIP)")
            if (ringing) add("incoming call ringing")
            if (userAcceptedVoipCall) add("VoIP call (user-accepted)")
            if (suspiciousVoipMode) add("VoIP mode set WITHOUT user call — not trusted")
            if (callGradeRecording) add("call-grade mic recording")
            if (mediaPlaying) add("media playing")
        }.joinToString(", ").ifBlank { "idle" }
    }

    fun snapshot(context: Context): Snapshot {
        val am = context.applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return Snapshot(false, false, false, false, false, false, false, false)
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
            telecomInCall = telecomInCall(context),
            telephonyOffHook = telephonyOffHook(context),
            callGradeRecording = callGradeRecording(am),
        )
    }

    /**
     * TelecomManager.isInCall() returns true for cellular calls AND any
     * ConnectionService-backed VoIP call. Modern WhatsApp, WhatsApp
     * Business, Signal, Google Meet, Teams all plug into ConnectionService
     * on API 23+, so this one signal catches the apps that silently fail
     * the AudioManager.mode heuristic. Requires READ_PHONE_STATE — we
     * already hold that for SIM-Swap Alarm, so it's a free win here.
     */
    private fun telecomInCall(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) return false
        val tm = context.applicationContext
            .getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        return runCatching { tm.isInCall }.getOrDefault(false)
    }

    /**
     * Cellular-only backstop. TelephonyManager.callState is unaffected by
     * any user app — it reflects the modem state. Belt-and-suspenders with
     * [telecomInCall] in case the Telecom service is temporarily
     * unavailable (very rare but observed on some low-end OEM builds).
     */
    private fun telephonyOffHook(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) return false
        val tm = context.applicationContext
            .getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return false
        return runCatching { tm.callState != TelephonyManager.CALL_STATE_IDLE }
            .getOrDefault(false)
    }

    /**
     * Inspect every active mic recording configuration. If any is tagged
     * with [MediaRecorder.AudioSource.VOICE_COMMUNICATION] or
     * [MediaRecorder.AudioSource.VOICE_CALL], it is a legitimate call-grade
     * recording — the mic is being used for voice communication regardless
     * of what AudioManager.mode says. Permissionless; API 24+.
     *
     * This catches apps that record for a call without setting the system
     * audio mode (seen in some WhatsApp Business builds and third-party
     * SIP clients that don't go through ConnectionService).
     */
    private fun callGradeRecording(am: AudioManager): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return runCatching {
            am.activeRecordingConfigurations.any { cfg ->
                if (cfg.isClientSilenced) return@any false
                val src = cfg.clientAudioSource
                src == MediaRecorder.AudioSource.VOICE_COMMUNICATION ||
                    src == MediaRecorder.AudioSource.VOICE_CALL
            }
        }.getOrDefault(false)
    }
}
