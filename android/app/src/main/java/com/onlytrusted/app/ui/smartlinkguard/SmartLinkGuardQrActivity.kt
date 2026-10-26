// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
@file:androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
package com.onlytrusted.app.ui.smartlinkguard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.onlytrusted.app.ui.privacyshield.CaptureRegistry
import java.util.concurrent.Executors

/**
 * Smart Link Guard — QR scanner activity.
 *
 * Launched via [Contract] and returns the detected URL as the activity result (or `null`
 * if the user cancelled or the camera permission was denied). Uses CameraX for the preview
 * pipeline and ML Kit's on-device barcode scanner for QR detection — zero network traffic.
 */
class SmartLinkGuardQrActivity : ComponentActivity() {

    class Contract : ActivityResultContract<Unit, String?>() {
        override fun createIntent(context: Context, input: Unit): Intent =
            Intent(context, SmartLinkGuardQrActivity::class.java)

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            if (resultCode != Activity.RESULT_OK) return null
            return intent?.getStringExtra(EXTRA_URL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QrScannerUI(
                onDetected = { url ->
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_URL, url))
                    finish()
                },
                onCancel = {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            )
        }
    }

    companion object {
        private const val EXTRA_URL = "smart_link_guard.qr_url"
    }
}

@Composable
private fun QrScannerUI(
    onDetected: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCamera = granted
        if (!granted) onCancel()
    }

    LaunchedEffect(Unit) {
        if (!hasCamera) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (hasCamera) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp)
            ) {
                CameraPreview(onDetected = onDetected)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                ) { Text("Cancel") }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Camera permission is required to scan QR codes.",
                    color = Color.White
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant camera access")
                }
                Button(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun CameraPreview(onDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var fired by remember { mutableStateOf(false) }

    val scanner = remember { BarcodeScanning.getClient() }
    // Auto Privacy Shield handle for this scanner's camera. Non-null while the preview
    // is bound; the stop closure finishes the hosting activity so the user sees the
    // UI tear down too (not just the sensor).
    val providerRef = remember { arrayOf<ProcessCameraProvider?>(null) }
    val shieldHandleRef = remember { arrayOf<CaptureRegistry.Handle?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            // Unbind + unregister on composable disposal so the dashboard history logs
            // this teardown explicitly rather than seeing a lingering phantom handle.
            runCatching { providerRef[0]?.unbindAll() }
            providerRef[0] = null
            shieldHandleRef[0]?.let { h ->
                shieldHandleRef[0] = null
                CaptureRegistry.unregister(h)
            }
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        // ImageProxy.image is annotated @ExperimentalGetImage upstream;
                        // we need the raw Image to hand to ML Kit's InputImage factory.
                        // This is Google's documented pattern for ML Kit + CameraX.
                        // File-level @OptIn at the top of the file acknowledges the
                        // experimental API — expression-level @OptIn is a Kotlin
                        // compile error.
                        it.setAnalyzer(executor) { proxy ->
                            val mediaImage = proxy.image
                            if (mediaImage == null || fired) {
                                proxy.close()
                                return@setAnalyzer
                            }
                            val input = InputImage.fromMediaImage(
                                mediaImage,
                                proxy.imageInfo.rotationDegrees
                            )
                            scanner.process(input)
                                .addOnSuccessListener { barcodes ->
                                    val url = barcodes
                                        .firstOrNull {
                                            it.valueType == Barcode.TYPE_URL ||
                                                it.valueType == Barcode.TYPE_TEXT
                                        }
                                        ?.rawValue
                                    if (!url.isNullOrBlank() && !fired) {
                                        fired = true
                                        onDetected(url)
                                    }
                                }
                                .addOnCompleteListener { proxy.close() }
                        }
                    }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                    providerRef[0] = provider
                    // Auto Privacy Shield: register once bind succeeds. The stop
                    // closure unbinds, which cascades through CameraX to close the
                    // sensor; the `DisposableEffect` cleans up the handle on recompose.
                    if (shieldHandleRef[0] == null) {
                        shieldHandleRef[0] = CaptureRegistry.register(
                            feature = "SmartLinkGuard QR",
                            kind = CaptureRegistry.Kind.CAMERA
                        ) {
                            runCatching { providerRef[0]?.unbindAll() }
                        }
                        CaptureRegistry.maybeArmScreenOffReceiver(ctx.applicationContext)
                    }
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
