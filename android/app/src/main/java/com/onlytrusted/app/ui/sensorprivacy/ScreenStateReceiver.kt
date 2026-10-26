// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Listens for `ACTION_SCREEN_OFF` + `ACTION_SCREEN_ON` and relays to the
 * active [SensorPrivacyController]. Must be registered at runtime
 * (manifest registration doesn't receive screen-off broadcasts on modern
 * Android — these are "protected" intents).
 *
 * Usage: register once from `MainActivity.onCreate` (or an Application
 * subclass) via [register], unregister on destroy.
 */
class ScreenStateReceiver private constructor(
    private val controller: SensorPrivacyController,
) : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
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
                scope.launch { controller.enforceLock() }
            }
            Intent.ACTION_SCREEN_ON -> scope.launch { controller.restoreAfterUnlock() }
        }
    }

    companion object {
        private var active: ScreenStateReceiver? = null

        fun register(context: Context, controller: SensorPrivacyController) {
            if (active != null) return
            val receiver = ScreenStateReceiver(controller)
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            context.applicationContext.registerReceiver(receiver, filter)
            active = receiver
        }

        fun unregister(context: Context) {
            val r = active ?: return
            runCatching { context.applicationContext.unregisterReceiver(r) }
            active = null
        }
    }
}
