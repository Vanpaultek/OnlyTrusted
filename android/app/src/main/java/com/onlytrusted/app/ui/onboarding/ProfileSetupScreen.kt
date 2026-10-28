// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.onboarding

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.onlytrusted.app.ui.lock.BiometricPrompter
import com.onlytrusted.app.ui.theme.AppBackdropScaffold
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.DangerDark
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark

/**
 * Mandatory first-launch profile setup — Android counterpart of iOS `ProfileSetupScreen`.
 * Two sequential sub-steps: Email → Biometric. The PIN step was removed in v0.1.7
 * because testers reported it as unexpected friction; a PIN is still available via
 * Settings for users who want App Lock or Duress Mode.
 * Writes to [ProfileStore]; calls [onComplete] when the user finishes the biometric step.
 */
@Composable
fun ProfileSetupScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(Step.Email) }
    var emailDraft by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AppBackdropScaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Header(step = step)
            when (step) {
                Step.Email -> EmailStep(
                    draft = emailDraft,
                    onDraft = { emailDraft = it },
                    onNext = {
                        val normalised = emailDraft.trim().lowercase()
                        if (!normalised.contains("@") || !normalised.contains(".")) {
                            errorMessage = "That doesn't look like an email address."
                        } else {
                            ProfileStore.setEmail(context, normalised)
                            errorMessage = null
                            // PIN step removed in v0.1.7 — jump straight to
                            // the (optional, Skippable) biometric step.
                            step = Step.Biometric
                        }
                    }
                )
                Step.Biometric -> BiometricStep(
                    onEnable = {
                        val activity = context as? FragmentActivity
                        if (activity != null && BiometricPrompter.canAuthenticate(activity)) {
                            BiometricPrompter.prompt(
                                activity,
                                title = "Set up your Passkey",
                                subtitle = "Face / fingerprint becomes your primary unlock — on-device.",
                                onSuccess = {
                                    ProfileStore.setBiometricEnabled(context, true)
                                    finish(context, onComplete)
                                },
                                onFailure = {
                                    ProfileStore.setBiometricEnabled(context, false)
                                    finish(context, onComplete)
                                }
                            )
                        } else {
                            ProfileStore.setBiometricEnabled(context, false)
                            errorMessage = "Face / fingerprint isn't available on this device — continuing with PIN only."
                            finish(context, onComplete)
                        }
                    },
                    onSkip = {
                        ProfileStore.setBiometricEnabled(context, false)
                        finish(context, onComplete)
                    }
                )
            }
            errorMessage?.let {
                Text(it, fontSize = 12.sp, color = DangerDark)
            }
        }
    }
}

private enum class Step { Email, Biometric }

@Composable
private fun Header(step: Step) {
    val (icon, title, subtitle) = when (step) {
        Step.Email -> Triple(
            Icons.Filled.Email,
            "Your email",
            "Stored on this device only. OnlyTrusted does not have a server, so your email is never transmitted anywhere."
        )
        Step.Biometric -> Triple(
            Icons.Filled.Face,
            "Set up your Passkey",
            "Face / fingerprint becomes your primary unlock — phishing-proof, nothing to type. Uses hardware-backed Keystore / StrongBox; biometric never leaves this device."
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = RoundedCornerShape(50), color = BrandAccentDark.copy(alpha = 0.15f)) {
            Icon(icon, null, tint = BrandAccentDark, modifier = Modifier.padding(20.dp).size(32.dp))
        }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryTextDark)
        Text(subtitle, fontSize = 13.sp, color = SecondaryTextDark)
    }
}

@Composable
private fun EmailStep(draft: String, onDraft: (String) -> Unit, onNext: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraft,
            singleLine = true,
            placeholder = { Text("you@example.com") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onNext,
            enabled = draft.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = BrandAccentDark),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun BiometricStep(onEnable: () -> Unit, onSkip: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Primary — Passkey (biometric) becomes the default unlock. Phishing-proof,
        // nothing to remember, uses Secure Enclave / StrongBox so credential never
        // leaves the device. Modern vocabulary matches 2026 user expectations.
        Button(
            onClick = onEnable,
            colors = ButtonDefaults.buttonColors(containerColor = BrandAccentDark),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Set up Passkey", color = Color.White, fontWeight = FontWeight.SemiBold) }
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Use PIN only", color = BrandAccentDark, fontWeight = FontWeight.SemiBold) }
    }
}

private fun finish(ctx: android.content.Context, onComplete: () -> Unit) {
    ProfileStore.markSetupComplete(ctx)
    onComplete()
}
