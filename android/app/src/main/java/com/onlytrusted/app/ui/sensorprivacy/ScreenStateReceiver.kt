// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Listens for `ACTION_SCREEN_OFF` + `ACTION_USER_PRESENT` and relays to the
 * active [SensorPrivacyController]. Must be registered at runtime
 * (manifest registration doesn't receive screen-off broadcasts on modern
 * Android — these are "protected" intents).
 *
 * Why `ACTION_USER_PRESENT` and NOT `ACTION_SCREEN_ON`:
 *   - `ACTION_SCREEN_ON` fires when the screen wakes to show the lock
 *     screen itself — if we restored sensors on that event, someone who
 *     picked up the phone would re-enable the mic + camera BEFORE
 *     authenticating. That's a security hole.
 *   - `ACTION_USER_PRESENT` fires only after the user successfully
 *     unlocks (PIN, pattern, fingerprint, face). This is the honest
 *     signal for "the owner is back; it's safe to restore sensors."
 *
 * Usage: register once from the foreground service via [register],
 * unregister on destroy.
 */
class ScreenStateReceiver private constructor(
    private val controller: SensorPrivacyController,
) : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("OTSensorPrivacy", "ScreenStateReceiver.onReceive action=${intent.action}")
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                if (SafetyOverride.isActive(context)) {
                    SensorPrivacyLog.append(
                        context,
                        kind = SensorPrivacyLog.Kind.SCREEN_LOCK,
                        detail = "screen locked, but safety override active " +
                            "(${SafetyOverride.remainingMinutes(context)} min remaining). " +
                            "enforceLock() skipped.",
                    )
                    return
                }
                // If the user is mid-call or playing media, cutting the mic
                // would drop the call or kill audio. Defer the lock — the
                // next screen-off after the audio session ends will fire
                // enforceLock() cleanly. The user can still force a lock
                // right now via the QS tile, launcher shortcut, Siri, or
                // the in-app "Lock Now" button; those paths intentionally
                // bypass this guard because they are explicit user intent.
                //
                // Honest-call check: VoIP mode (MODE_IN_COMMUNICATION) is
                // forgeable by any app, so we trust it only when
                // [MicActivityTracker] confirms the recording started while
                // the screen was on and has been running long enough to
                // look like a user-accepted call. Silent auto-activation
                // (background mic flip) is explicitly NOT a deferral.
                val audio = ActiveAudioSession.snapshot(context)
                if (audio.suspiciousVoipMode && !audio.shouldDeferLock) {
                    SensorPrivacyLog.append(
                        context,
                        kind = SensorPrivacyLog.Kind.SCREEN_LOCK,
                        detail = "VoIP audio mode is set but no user-accepted " +
                            "call session was observed. Treating as silent " +
                            "auto-activation — locking anyway.",
                    )
                }
                if (audio.shouldDeferLock) {
                    SensorPrivacyLog.append(
                        context,
                        kind = SensorPrivacyLog.Kind.SCREEN_LOCK,
                        detail = "screen locked during active audio session " +
                            "(${audio.reason()}). enforceLock() deferred so " +
                            "mic/camera stay available for the call or media.",
                    )
                    return
                }
                scope.launch { controller.enforceLock() }
            }
            // Intentional: restore ONLY on USER_PRESENT, not on SCREEN_ON.
            // SCREEN_ON fires when the lock screen appears — before the user
            // has authenticated. Restoring there would re-enable mic + camera
            // for anyone who picks up the phone. USER_PRESENT fires only
            // after a successful PIN / pattern / biometric unlock.
            Intent.ACTION_USER_PRESENT -> scope.launch { controller.restoreAfterUnlock() }
        }
    }

    companion object {
        private var active: ScreenStateReceiver? = null

        fun register(context: Context, controller: SensorPrivacyController) {
            if (active != null) return
            val receiver = ScreenStateReceiver(controller)
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            // Android 14+ requires an explicit export flag on dynamic receivers.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(
                    receiver, filter, Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                context.applicationContext.registerReceiver(receiver, filter)
            }
            Log.i("OTSensorPrivacy", "ScreenStateReceiver registered for " +
                "ACTION_SCREEN_OFF + ACTION_USER_PRESENT (tier=${controller.tierLabel})")
            active = receiver
        }

        fun unregister(context: Context) {
            val r = active ?: return
            runCatching { context.applicationContext.unregisterReceiver(r) }
            active = null
        }
    }
}
