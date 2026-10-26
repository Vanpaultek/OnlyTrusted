// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Volume-down double-press → cut mic + camera. Alternative trigger for the
 * physical power button, which Android reserves for system use.
 *
 * Permission footprint — intentionally minimal:
 *   - `canRequestFilterKeyEvents=true` is NOT set; this service observes
 *     key events via `onKeyEvent()` only (dispatched post-system-handling)
 *     so we don't block the normal volume-change behaviour.
 *   - No `canRetrieveWindowContent` / `canPerformGestures` / text reading.
 *   - Service config XML (res/xml/sensor_privacy_accessibility.xml) locks
 *     capabilities down further via `accessibilityFlags`.
 *
 * Why AccessibilityService at all: it is the only Android API that can
 * observe hardware key events from a background service without blocking
 * the normal UX. The user must explicitly enable it in
 * Settings → Accessibility → OnlyTrusted. Google Play's Accessibility
 * Service Policy requires us to justify the use + disclose data handling —
 * see docs/SENSOR_PRIVACY_ARCHITECTURE.md.
 *
 * Data handling (comply with Play Policy + GDPR):
 *   - No AccessibilityEvents are stored, inspected, or sent anywhere.
 *   - `onAccessibilityEvent` is intentionally a no-op.
 *   - `onKeyEvent` only matches `KEYCODE_VOLUME_DOWN` and never records
 *     the key timeline. Only the double-press signal reaches the
 *     controller.
 */
class SensorPrivacyAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var lastVolumeDownAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        SensorPrivacyLog.append(
            this,
            kind = "ACCESSIBILITY_CONNECTED",
            detail = "Volume-key trigger service enabled by user.",
        )
    }

    /** Intentionally empty — we do NOT read window content, text, or any
     *  other AccessibilityEvent payload. Compliance proof for Play Review. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        val now = System.currentTimeMillis()
        val delta = now - lastVolumeDownAt
        lastVolumeDownAt = now
        if (delta in 100..600) {
            // Double-press detected within 100-600ms window. Trigger the cut.
            val controller = SensorPrivacyForegroundService.pickController(this)
            scope.launch {
                controller.enforceLock()
                SensorPrivacyLog.append(
                    this@SensorPrivacyAccessibilityService,
                    kind = "ACCESSIBILITY_TRIGGER",
                    detail = "Volume-down double-press → enforceLock().",
                )
            }
            // Return false so the system still processes the volume-change
            // normally — we're an observer, not an interceptor.
            return false
        }
        return false
    }

    override fun onInterrupt() {
        // Required override; no action needed.
    }
}
