// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.distress

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * App-wide publish/subscribe channel for distress-signal triggers.
 *
 * Any trigger surface (in-app SOS button tap, shake-pattern detected, volume-down
 * 4× detected, future lock-screen widget) calls [fire] and the top-level countdown
 * overlay in MainActivity picks it up. Decouples trigger detection (lives wherever
 * is most appropriate — Dashboard FAB, MainActivity key listener, SensorManager
 * callback) from the countdown-and-dispatch UI (owned by MainActivity so it can
 * overlay any screen in the app, not just the Dashboard).
 *
 * Buffer config: `extraBufferCapacity = 1`, `onBufferOverflow = DROP_OLDEST`. Two
 * triggers fired in rapid succession collapse to one — a single SOS flow is what
 * the user wants; firing the countdown twice back-to-back would be confusing.
 */
object DistressTriggerBus {

    private val _events = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<Unit> = _events

    /** Non-suspending — safe to call from any thread + any UI callback. */
    fun fire() {
        _events.tryEmit(Unit)
    }
}
