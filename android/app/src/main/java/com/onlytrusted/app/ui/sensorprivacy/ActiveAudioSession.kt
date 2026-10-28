// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import android.media.AudioManager

/**
 * Lightweight guard that answers "is the user currently using the phone's
 * audio pipeline in a way that cutting the mic / camera would disrupt?"
 *
 * Used by [ScreenStateReceiver] to skip `enforceLock()` on screen-off when
 * the user is clearly mid-call or mid-playback. The screen-off trigger is
 * a convenience (lock sensors when you pocket the phone); it should NOT
 * sabotage an active cellular call, a WhatsApp / Signal / Meet video call,
 * or music playing out of your headphones.
 *
 * Signals used (no new permissions required):
 *
 *  - `AudioManager.getMode()` →
 *     - `MODE_IN_CALL`           — cellular call in progress
 *     - `MODE_IN_COMMUNICATION`  — VoIP call (WhatsApp, Signal, Zoom, Meet,
 *                                  Teams, FaceTime Audio, Discord)
 *     - `MODE_RINGTONE`          — incoming call is ringing
 *  - `AudioManager.isMusicActive()` → something is actively playing on the
 *    media stream (Spotify, YouTube, podcast app, video).
 *
 * Returning `true` tells the receiver "defer this lock; the user is doing
 * something that needs the audio stack." The user can still lock
 * explicitly via the Quick Settings tile, launcher shortcut, Siri /
 * Assistant intent, or in-app button — those paths intentionally bypass
 * this guard because the user is making an explicit choice.
 */
internal object ActiveAudioSession {

    data class Snapshot(
        val onCall: Boolean,
        val onVoipCall: Boolean,
        val ringing: Boolean,
        val mediaPlaying: Boolean,
    ) {
        val busy: Boolean get() = onCall || onVoipCall || ringing || mediaPlaying
        fun reason(): String = buildList {
            if (onCall) add("cellular call")
            if (onVoipCall) add("VoIP call")
            if (ringing) add("incoming call ringing")
            if (mediaPlaying) add("media playing")
        }.joinToString(", ").ifBlank { "idle" }
    }

    fun snapshot(context: Context): Snapshot {
        val am = context.applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return Snapshot(false, false, false, false)
        val mode = am.mode
        return Snapshot(
            onCall = mode == AudioManager.MODE_IN_CALL,
            onVoipCall = mode == AudioManager.MODE_IN_COMMUNICATION,
            ringing = mode == AudioManager.MODE_RINGTONE,
            mediaPlaying = am.isMusicActive,
        )
    }
}
