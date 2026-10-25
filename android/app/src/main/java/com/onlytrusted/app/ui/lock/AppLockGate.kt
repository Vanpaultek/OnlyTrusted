// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.lock

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.onlytrusted.app.OnlyTrustedApp
import com.onlytrusted.app.security.AppLockManager
import com.onlytrusted.app.ui.duress.DuressSmsSender
import com.onlytrusted.app.ui.duress.DuressState
import com.onlytrusted.app.ui.duress.DuressVaultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope

@Composable
fun AppLockGate(
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var attempted by remember { mutableStateOf(false) }
    val canAuth = remember { BiometricPrompter.canAuthenticate(context) }
    val lockEnabled = remember { AppLockManager.isEnabled(context) }
    var pinSheet by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Permission launcher used by the duress-SMS path. If the user denies, the SMS send is
    // skipped silently; the decoy vault still opens.
    val scope = rememberCoroutineScopeCompat()
    var pendingSOSNumber by remember { mutableStateOf<String?>(null) }
    var pendingSOSBody by remember { mutableStateOf<String?>(null) }
    val smsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val n = pendingSOSNumber
        val b = pendingSOSBody
        if (granted && n != null && b != null) {
            DuressSmsSender.send(context, n, b)
        }
        pendingSOSNumber = null
        pendingSOSBody = null
        // Enter the decoy vault either way so coercion is not detectable.
        DuressState.setDuressSession(true)
        AppLockManager.markUnlocked(context)
        onUnlocked()
    }

    LaunchedEffect(Unit) {
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        if (!canAuth) {
            error = if (lockEnabled) {
                "This device no longer has a biometric or PIN set up. Disable App Lock below " +
                    "to recover, then re-enable it after setting one up in system settings."
            } else {
                "Panic lock engaged. Tap below to return to the app."
            }
            return@LaunchedEffect
        }
        attempted = true
        BiometricPrompter.prompt(
            activity = activity,
            title = "Unlock with your Passkey",
            subtitle = "Face / fingerprint — on-device, phishing-proof",
            onSuccess = {
                // Real unlock: always clear any duress session that was active.
                DuressState.setDuressSession(false)
                // Dead-Man Wipe (Elite): reset the inactivity countdown.
                com.onlytrusted.app.deadmanwipe.DeadmanWipeState.markUnlockNow(context)
                onUnlocked()
            },
            onFailure = { error = it }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text("Locked", style = MaterialTheme.typography.headlineMedium)
        error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (canAuth) {
            Button(
                onClick = {
                    val activity = context as? FragmentActivity ?: return@Button
                    error = null
                    attempted = true
                    BiometricPrompter.prompt(
                        activity = activity,
                        title = "Unlock with your Passkey",
                        subtitle = "Face / fingerprint — on-device, phishing-proof",
                        onSuccess = {
                            DuressState.setDuressSession(false)
                            onUnlocked()
                        },
                        onFailure = { error = it }
                    )
                },
                modifier = Modifier.padding(top = 24.dp)
            ) { Text(if (attempted) "Try again" else "Unlock") }

            TextButton(
                onClick = { pinSheet = true },
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("Enter PIN instead") }
        } else {
            Button(
                onClick = {
                    if (lockEnabled) {
                        AppLockManager.setEnabled(context, false)
                    }
                    AppLockManager.resetUnlock(context)
                    onUnlocked()
                },
                modifier = Modifier.padding(top = 24.dp)
            ) { Text(if (lockEnabled) "Disable App Lock and continue" else "Return to app") }
        }
    }

    if (pinSheet) {
        AlertDialog(
            onDismissRequest = { pinSheet = false; pin = ""; pinError = null },
            title = { Text("Enter PIN") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text("PIN") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    pinError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (DuressState.matches(context, pin)) {
                        pinSheet = false
                        pin = ""
                        pinError = null
                        triggerDuressUnlock(
                            context = context,
                            scope = scope,
                            onReady = { number, body ->
                                if (number == null || body == null) {
                                    DuressState.setDuressSession(true)
                                    AppLockManager.markUnlocked(context)
                                    onUnlocked()
                                } else if (DuressSmsSender.hasSendSmsPermission(context)) {
                                    DuressSmsSender.send(context, number, body)
                                    DuressState.setDuressSession(true)
                                    AppLockManager.markUnlocked(context)
                                    onUnlocked()
                                } else {
                                    pendingSOSNumber = number
                                    pendingSOSBody = body
                                    smsPermLauncher.launch(Manifest.permission.SEND_SMS)
                                }
                            }
                        )
                    } else {
                        pinError = "Incorrect PIN."
                    }
                }) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = { pinSheet = false; pin = ""; pinError = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Resolves the alert contact phone number and SOS body, then calls `onReady` with them. Runs
 * the DB lookup off the main thread so the lock-screen tap doesn't block.
 */
private fun triggerDuressUnlock(
    context: android.content.Context,
    scope: CoroutineScope,
    onReady: (phoneNumber: String?, body: String?) -> Unit
) {
    val alertId = DuressState.alertContactId(context)
    if (alertId == null) {
        onReady(null, null); return
    }
    val app = context.applicationContext as OnlyTrustedApp
    // Hydrate the decoy vault's in-memory flow so the Contacts tab shows something immediately.
    DuressVaultStore.refresh(context)
    scope.launch {
        val record = withContext(Dispatchers.IO) {
            app.trustedContacts.reveal(alertId)
        }
        val number = record?.numbers?.firstOrNull()?.normalisedNumber
        val body = DuressSmsSender.buildBody(DuressState.ownerName(context))
        onReady(number, body)
    }
}

@Composable
private fun rememberCoroutineScopeCompat(): CoroutineScope =
    androidx.compose.runtime.rememberCoroutineScope()
