// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.distress

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * One-shot best-effort location fetch for the Distress Signal flow.
 *
 * Uses the Android framework `LocationManager` directly — no Google Play Services
 * dependency. Strategy:
 *   1. If `ACCESS_FINE_LOCATION` is granted, try GPS provider first (best accuracy),
 *      falling back to NETWORK provider if GPS has no fresh fix within the timeout.
 *   2. If only `ACCESS_COARSE_LOCATION` is granted, go straight to NETWORK.
 *   3. If no location permission is granted, return null immediately — the sender
 *      will dispatch the SMS with "Location: unavailable" so the signal still fires.
 *
 * Timeout is deliberately short (default 3 s). In a true emergency, waiting 30+ s for
 * a perfect GPS fix defeats the purpose — the SMS and dial need to go out NOW. A
 * stale cached fix is better than nothing; a current rough network fix is better than
 * a precise fix that arrives after the user is incapacitated.
 */
object LocationFetcher {

    /**
     * Returns the best current location within [timeoutMs], or null if nothing came back
     * or permission was denied.
     */
    suspend fun fetchOnce(
        context: Context,
        timeoutMs: Long = 3_000L
    ): DistressLocation? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return null

        // Try last-known from both providers first — if there's a recent cached fix we
        // can short-circuit the fetch and dispatch immediately.
        val cached = runCatching {
            val gpsLast = if (fineGranted) lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            val netLast = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            bestOf(gpsLast, netLast)
        }.getOrNull()
        if (cached != null && (System.currentTimeMillis() - cached.time) < 30_000L) {
            // Fresh enough (< 30 s old) — use it, don't bother firing a new request.
            return cached.toDistressLocation()
        }

        // No fresh cache — request a single fresh update from both providers in
        // parallel, take whichever lands first within the timeout.
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<DistressLocation?> { cont ->
                val executor = Executors.newSingleThreadExecutor()
                val resumedOnce = java.util.concurrent.atomic.AtomicBoolean(false)
                fun resumeOnce(value: DistressLocation?) {
                    if (resumedOnce.compareAndSet(false, true)) {
                        runCatching { executor.shutdownNow() }
                        if (cont.isActive) cont.resume(value)
                    }
                }

                val consumer = { loc: Location? -> resumeOnce(loc?.toDistressLocation()) }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (fineGranted) {
                            lm.getCurrentLocation(LocationManager.GPS_PROVIDER, null, executor, consumer)
                        }
                        lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, executor, consumer)
                    } else {
                        // Pre-R: fall back to last-known even if stale — single-shot
                        // `requestSingleUpdate` was deprecated and unreliable.
                        resumeOnce(cached?.toDistressLocation())
                    }
                } catch (se: SecurityException) {
                    resumeOnce(null)
                }

                cont.invokeOnCancellation {
                    runCatching { executor.shutdownNow() }
                }
            }
        } ?: cached?.toDistressLocation()
    }

    private fun bestOf(a: Location?, b: Location?): Location? = when {
        a == null -> b
        b == null -> a
        a.accuracy < b.accuracy -> a
        else -> b
    }

    private fun Location.toDistressLocation(): DistressLocation = DistressLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        capturedAtMillis = time
    )
}
