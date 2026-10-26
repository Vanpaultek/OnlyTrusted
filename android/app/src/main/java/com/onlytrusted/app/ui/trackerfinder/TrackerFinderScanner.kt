// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.trackerfinder

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.util.UUID

/**
 * Low-level BLE scan helper for Tracker Finder (Pro).
 *
 * Independent of the Compose UI — both the on-screen "Scan now" button and the
 * [TrackerFinderService] foreground service use this class.
 *
 * Classification rules (brief verbatim):
 *   • Apple AirTag  — mfr ID 0x004C, payload length 25..31 bytes (Offline Finding).
 *   • Tile          — service UUID starting FEED or mfr ID 0x00B9.
 *   • Samsung tag   — mfr ID 0x0075.
 *   • Unknown BLE   — otherwise, no device name, becomes a candidate for the
 *                     "3+ sessions over >15 min" persistent-follower rule.
 */
class TrackerFinderScanner(
    private val context: Context,
    private val onClassified: (TrackerFinderStore.Sighting) -> Unit = {},
    private val onFinished: (detections: Int) -> Unit = {},
    private val onError: (message: String) -> Unit = {}
) {

    @Volatile private var isScanning: Boolean = false
    @Volatile private var sessionId: String = UUID.randomUUID().toString()
    @Volatile private var detections: Int = 0

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handle(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handle)
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            onError("BLE scan failed (code $errorCode).")
            onFinished(detections)
        }
    }

    /**
     * Start a scan. Caller is responsible for stopping via [stop].
     * Returns false if the required runtime permissions aren't granted or BLE isn't available.
     */
    fun start(): Boolean {
        if (!hasRequiredPermissions(context)) {
            onError("Grant Nearby devices + Location to scan.")
            return false
        }
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            onError("Turn on Bluetooth to scan for trackers.")
            return false
        }
        val scanner = adapter.bluetoothLeScanner ?: run {
            onError("This device doesn't expose a BLE scanner.")
            return false
        }

        sessionId = UUID.randomUUID().toString()
        detections = 0

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0)
            .build()

        // We don't restrict by filter — payload-level classification needs to see every
        // advertisement so we can identify mfr-data-only trackers. For the Tile service
        // UUID filter we could narrow but then unknown-BLE wouldn't surface.
        // Suppress: hasRequiredPermissions(context) above gates every call site; the
        // try/catch below handles the lint-contract SecurityException escape anyway.
        try {
            @SuppressLint("MissingPermission")
            val _ignored = scanner.startScan(null, settings, callback)
            isScanning = true
            return true
        } catch (t: Throwable) {
            onError("BLE scan start failed: ${t.message ?: "unknown"}")
            return false
        }
    }

    fun stop() {
        if (!isScanning) return
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        // Suppress: stop() is only reachable after start() returned true, which can
        // only happen if hasRequiredPermissions(context) was true. The permission can
        // only have been revoked by the user mid-session; try/catch handles that race.
        try {
            @SuppressLint("MissingPermission")
            val _ignored = adapter?.bluetoothLeScanner?.stopScan(callback)
        } catch (_: Throwable) { /* ignore */ }
        isScanning = false
        onFinished(detections)
    }

    private fun handle(result: ScanResult) {
        val record = result.scanRecord ?: return
        val localName = record.deviceName
            ?: runCatching {
                // Suppress: hasConnectPermission(context) IS the runtime check lint
                // wants — it just can't see through the helper to the inlined
                // ContextCompat.checkSelfPermission call.
                @SuppressLint("MissingPermission")
                val name = if (hasConnectPermission(context)) result.device?.name else null
                name
            }.getOrNull()
        val serviceUUIDs: List<ParcelUuid> = record.serviceUuids ?: emptyList()

        // Scan the manufacturer-specific-data map for the SIG company IDs we care about.
        val appleData = record.getManufacturerSpecificData(0x004C)
        val tileData = record.getManufacturerSpecificData(0x00B9)
        val samsungData = record.getManufacturerSpecificData(0x0075)

        val classified: TrackerFinderStore.TrackerType? = when {
            // Apple AirTag / Offline Finding — payload length 25..31 bytes.
            appleData != null && appleData.size in 25..31 -> TrackerFinderStore.TrackerType.APPLE_AIRTAG
            // Tile: service UUID 0xFEED or mfr 0x00B9.
            serviceUUIDs.any { it.uuid.toString().uppercase().startsWith("0000FEED") } ||
                tileData != null -> TrackerFinderStore.TrackerType.TILE
            // Samsung: mfr 0x0075.
            samsungData != null -> TrackerFinderStore.TrackerType.SAMSUNG_SMARTTAG
            // Unknown BLE device (no name) — classifier for the "persistent follower" rule.
            localName.isNullOrBlank() -> TrackerFinderStore.TrackerType.UNKNOWN_BLE
            else -> null
        }
        val type: TrackerFinderStore.TrackerType = classified ?: return

        val macHash = TrackerFinderStore.hashAddress(context, result.device?.address ?: "unknown")
        val sighting = TrackerFinderStore.Sighting(
            id = UUID.randomUUID().toString(),
            macHash = macHash,
            type = type,
            rssi = result.rssi,
            timestamp = System.currentTimeMillis(),
            sessionId = sessionId
        )
        TrackerFinderStore.record(context, sighting)
        detections += 1
        onClassified(sighting)
    }

    companion object {

        /** The runtime permissions `BluetoothLeScanner.startScan` demands on this OS version. */
        fun requiredRuntimePermissions(): List<String> {
            val perms = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // BLUETOOTH_SCAN replaces the pre-S location requirement for scans that
                // DECLARE neverForLocation (we don't need the user's position). We request
                // CONNECT too so device names are available when a tracker is nearby.
                perms += Manifest.permission.BLUETOOTH_SCAN
                perms += Manifest.permission.BLUETOOTH_CONNECT
            } else {
                // Pre-S: scan returns nothing unless we have fine location.
                perms += Manifest.permission.ACCESS_FINE_LOCATION
            }
            return perms
        }

        fun hasRequiredPermissions(ctx: Context): Boolean {
            return requiredRuntimePermissions().all {
                ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
            }
        }

        private fun hasConnectPermission(ctx: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
            return ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
