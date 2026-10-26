// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.onlytrusted.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Quick Settings tile — one-tap "block mic + camera now" button that lives
 * in the Android pulldown panel alongside Wi-Fi, Bluetooth, etc.
 *
 * Tapping the tile flips the active controller's state:
 *   - Consumer tier → toggles AudioManager.isMicrophoneMute
 *   - Device Owner tier → also toggles DevicePolicyManager.setCameraDisabled
 *   - Privileged tier → toggles via SensorPrivacyManager
 *
 * The tile ships hidden by default; the user adds it manually from
 * Settings → Edit Quick Settings (same flow as Android's built-in
 * Camera Access + Mic Access tiles added in Android 12).
 *
 * Permission: `android.permission.BIND_QUICK_SETTINGS_TILE` is declared
 * in AndroidManifest.xml. The system binds the service only when the
 * tile is interacted with — nothing runs in the background from this.
 */
@RequiresApi(Build.VERSION_CODES.N)
class SensorPrivacyTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val controller = SensorPrivacyForegroundService.pickController(this)
        val isActive = qsTile?.state == Tile.STATE_ACTIVE
        scope.launch {
            if (isActive) {
                controller.restoreAfterUnlock()
            } else {
                controller.enforceLock()
            }
            // Refresh after a tiny delay so the state change propagates.
            refreshTile(isActive = !isActive)
        }
    }

    private fun refreshTile(isActive: Boolean? = null) {
        val tile = qsTile ?: return
        val active = isActive ?: isCurrentlyActive()
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.sensor_privacy_tile_label)
        tile.contentDescription = tile.label
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                if (active) R.string.sensor_privacy_tile_subtitle_on
                else R.string.sensor_privacy_tile_subtitle_off,
            )
        }
        tile.icon = Icon.createWithResource(
            this,
            if (active) android.R.drawable.ic_lock_lock else android.R.drawable.ic_lock_silent_mode,
        )
        tile.updateTile()
    }

    private fun isCurrentlyActive(): Boolean {
        // Fast heuristic: did WE mute the mic? Reading SensorPrivacyManager
        // state is signature-gated so we fall back to our own log. The tile
        // state is purely a UI hint — the real enforcement lives in the
        // controller.
        return SensorPrivacyLog.recent(this, limit = 1)
            .firstOrNull { it.kind == SensorPrivacyLog.Kind.SENSOR_PRIVACY_ENFORCED } != null
    }
}
