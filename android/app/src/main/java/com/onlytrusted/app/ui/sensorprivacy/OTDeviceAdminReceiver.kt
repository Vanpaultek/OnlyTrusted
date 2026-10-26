// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * The DeviceAdminReceiver required for OnlyTrusted to be registered as
 * Android Device Owner (DO). Receiving the admin lifecycle broadcasts is
 * what makes the component a legitimate target for
 * `dpm set-device-owner com.onlytrusted.app/.ui.sensorprivacy.OTDeviceAdminReceiver`.
 *
 * See docs/SENSOR_PRIVACY_ARCHITECTURE.md § "Provisioning" for the one-time
 * ADB setup. Must also be declared in AndroidManifest.xml with the standard
 * `android.app.device_admin` metadata pointing at
 * res/xml/device_admin_sensor_privacy.xml.
 */
class OTDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        SensorPrivacyLog.append(
            context,
            kind = "DEVICE_ADMIN_ENABLED",
            detail = "OnlyTrusted granted Device Owner privileges.",
        )
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        SensorPrivacyLog.append(
            context,
            kind = "DEVICE_ADMIN_DISABLED",
            detail = "OnlyTrusted Device Owner privileges revoked.",
        )
    }
}
