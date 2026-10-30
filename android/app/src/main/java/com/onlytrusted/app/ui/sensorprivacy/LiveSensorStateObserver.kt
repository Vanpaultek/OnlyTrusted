// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.sensorprivacy

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration

/**
 * Live tracker of "is any app currently using the microphone / camera?" that
 * drives the dynamic foreground-service notification icon + color.
 *
 * Watches two unprotected Android APIs:
 *  - [AudioManager.AudioRecordingCallback] — fires any time an app opens or
 *    closes the mic.
 *  - [CameraManager.AvailabilityCallback.onCameraUnavailable] — fires when
 *    any app opens a camera (becomes unavailable to others).
 *
 * Neither requires any permission. Reports state via [onStateChange] on the
 * main thread (callbacks come on the thread we registered them with).
 */
internal class LiveSensorStateObserver(
    private val context: Context,
    private val onStateChange: (State) -> Unit,
) {
    enum class State { IDLE, MIC_ONLY, CAMERA_ONLY, BOTH }

    private val audioManager by lazy {
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    private val cameraManager by lazy {
        context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    @Volatile private var micActive = false
    private val activeCameraIds = HashSet<String>()
    @Volatile private var current = State.IDLE

    private val audioCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
            // Any un-silenced config → mic is in use. Our own package counts
            // because the mic/camera lock service doesn't open the mic; any
            // config we see is some OTHER app recording.
            val anyActive = configs?.any { !it.isClientSilenced } == true
            if (micActive != anyActive) {
                micActive = anyActive
                publish()
            }
        }
    }

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            if (activeCameraIds.remove(cameraId)) publish()
        }
        override fun onCameraUnavailable(cameraId: String) {
            // "Unavailable" = another app opened it. Add to our live set.
            if (activeCameraIds.add(cameraId)) publish()
        }
    }

    fun start() {
        runCatching { audioManager?.registerAudioRecordingCallback(audioCallback, null) }
        runCatching { cameraManager?.registerAvailabilityCallback(cameraCallback, null) }
        // Seed initial state from active recording configs.
        val initialMic = audioManager?.activeRecordingConfigurations?.any { !it.isClientSilenced } == true
        if (initialMic != micActive) {
            micActive = initialMic
            publish()
        }
    }

    fun stop() {
        runCatching { audioManager?.unregisterAudioRecordingCallback(audioCallback) }
        runCatching { cameraManager?.unregisterAvailabilityCallback(cameraCallback) }
        activeCameraIds.clear()
        micActive = false
    }

    private fun publish() {
        val cam = activeCameraIds.isNotEmpty()
        val next = when {
            micActive && cam -> State.BOTH
            micActive -> State.MIC_ONLY
            cam -> State.CAMERA_ONLY
            else -> State.IDLE
        }
        if (next != current) {
            current = next
            onStateChange(next)
        }
    }
}
