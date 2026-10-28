// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.annotation.RequiresApi

/**
 * Tracks when each app starts/stops using the microphone, and whether the
 * screen was interactive at the moment recording began. Used by
 * [ScreenStateReceiver] to tell apart "user accepted a VoIP call" (safe to
 * skip the lock) from "a background app silently activated the mic"
 * (never skip the lock).
 *
 * ### Threat model
 *
 * An honest VoIP call starts because the user tapped Accept (or dialed out).
 * That means:
 *   1. The screen was ON when recording began, and
 *   2. The recording has been running for more than a couple of seconds by
 *      the time the user puts the phone down.
 *
 * A malicious app that opens the microphone without any user interaction
 * fails both tests: either the screen was already off, or recording started
 * so recently that the user had no chance to accept anything. In both cases
 * the auto-lock must still fire.
 *
 * We intentionally do NOT whitelist specific VoIP packages (WhatsApp, Signal,
 * Zoom, Meet, Teams, Discord…). A whitelist assumes the whitelisted app
 * cannot be compromised; the behavioural signal above is stronger.
 *
 * ### Permissions
 *
 * None. [AudioManager.AudioRecordingCallback] + [AudioRecordingConfiguration]
 * are unprotected APIs on API 29+. `getClientPackageName` is accessed via
 * reflection for forward-compatibility with API 29 → 36.
 *
 * ### Lifecycle
 *
 * Registered once from [SensorPrivacyForegroundService.onCreate] regardless
 * of the "Reactive mic alerts" opt-in — the alerts feature uses the same
 * underlying callback but we need the tracker running whether the user wants
 * notifications or not, because the lock decision depends on it.
 */
@RequiresApi(Build.VERSION_CODES.N)
internal object MicActivityTracker {

    /**
     * A single mic recording session we have observed. Keyed by package name
     * in the active-sessions map.
     *
     * @param startedWhileScreenOn `true` iff [PowerManager.isInteractive]
     *   returned true at the moment this session first appeared in the
     *   recording configs. False implies the app opened the mic while the
     *   phone was already locked / asleep — a strong "silent activation"
     *   signal.
     */
    data class Session(
        val packageName: String,
        val startedAtMs: Long,
        val startedWhileScreenOn: Boolean,
    ) {
        fun looksLikeUserCall(now: Long): Boolean =
            startedWhileScreenOn && (now - startedAtMs) >= MIN_LEGIT_DURATION_MS
    }

    /** Minimum duration a recording must have been running to count as
     *  "user accepted a call". Three seconds is long enough to filter out
     *  flash-and-close mic probes and short enough that any real call
     *  trivially clears it. */
    private const val MIN_LEGIT_DURATION_MS = 3_000L

    private val sessions = HashMap<String, Session>()
    private var callback: AudioManager.AudioRecordingCallback? = null

    @Synchronized
    fun start(context: Context) {
        if (callback != null) return
        val am = context.applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val pm = context.applicationContext.getSystemService(PowerManager::class.java)
        val cb = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
                val now = System.currentTimeMillis()
                val activePkgs = HashSet<String>()
                configs?.forEach { config ->
                    val pkg = recordingPackage(config) ?: return@forEach
                    activePkgs += pkg
                    synchronized(sessions) {
                        if (sessions[pkg] == null) {
                            sessions[pkg] = Session(
                                packageName = pkg,
                                startedAtMs = now,
                                startedWhileScreenOn = pm?.isInteractive == true,
                            )
                        }
                    }
                }
                // Drop sessions that no longer appear in the configs list —
                // the app stopped recording.
                synchronized(sessions) {
                    val stale = sessions.keys - activePkgs
                    stale.forEach { sessions.remove(it) }
                }
            }
        }
        am.registerAudioRecordingCallback(cb, Handler(Looper.getMainLooper()))
        callback = cb
    }

    @Synchronized
    fun stop(context: Context) {
        val am = context.applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        callback?.let { am.unregisterAudioRecordingCallback(it) }
        callback = null
        synchronized(sessions) { sessions.clear() }
    }

    /** Returns the first observed recording session that looks like a user
     *  call, or `null` if none qualify. See [Session.looksLikeUserCall]. */
    fun legitimateCallSession(now: Long = System.currentTimeMillis()): Session? {
        synchronized(sessions) {
            return sessions.values.firstOrNull { it.looksLikeUserCall(now) }
        }
    }

    /** Returns every active mic session, regardless of whether it looks
     *  legitimate. Caller is responsible for not leaking package names
     *  through logs (they are PII-adjacent — reveal what apps are
     *  installed). */
    fun activeSessions(): List<Session> {
        synchronized(sessions) { return sessions.values.toList() }
    }

    private fun recordingPackage(config: AudioRecordingConfiguration): String? =
        runCatching {
            val m = AudioRecordingConfiguration::class.java.getMethod("getClientPackageName")
            m.invoke(config) as? String
        }.getOrNull()
}
