// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.onlytrusted.app.ui.onboarding.ConsentScreen
import com.onlytrusted.app.ui.onboarding.OnboardingScreen
import com.onlytrusted.app.ui.onboarding.ProfileSetupScreen
import com.onlytrusted.app.ui.security.SecurityNagCard
import com.onlytrusted.app.ui.security.SecurityNagModal
import com.onlytrusted.app.ui.theme.OnlyTrustedTheme

/**
 * Preview matrix that renders key onboarding screens at a range of device
 * sizes in the Android Studio preview pane. Catches weighted-layout breaks
 * (toggle cards getting squished on short screens, hero cropping on
 * foldables) BEFORE a tester has to flag it on a real device.
 *
 * How to use:
 *   1. Open this file in Android Studio
 *   2. Click "Split" view
 *   3. See each preview rendered side by side at the declared size
 *
 * Adding a new screen? Add a @Preview that opens it, pick two or three
 * device presets from [Devices], done. Mix of named device presets +
 * explicit width/height so we cover both the common and the extreme.
 */

// --- Reusable device parameter sets --------------------------------------

/** Narrow phone — Samsung Galaxy A14 class, 360 dp width. */
private const val PHONE_SMALL = "spec:width=360dp,height=740dp,dpi=411"
/** Standard phone — Pixel 7 class, 411 dp width. */
private const val PHONE_STD = Devices.PIXEL_7
/** Pixel 7 Pro, taller + wider than standard. */
private const val PHONE_LARGE = Devices.PIXEL_7_PRO
/** Foldable inner display approximation. */
private const val FOLD_UNFOLDED = Devices.FOLDABLE
/** Tablet, 7-inch class. */
private const val TABLET = Devices.NEXUS_7_2013
/** Very short screen — catches weighted-layout breaks. */
private const val SHORT_PHONE = "spec:width=360dp,height=640dp,dpi=320"

// --- OnboardingScreen ----------------------------------------------------

@Preview(name = "Onboarding — small phone", device = PHONE_SMALL, showBackground = true)
@Composable
private fun OnboardingSmallPreview() { OnlyTrustedTheme { OnboardingScreen(onContinue = {}) } }

@Preview(name = "Onboarding — Pixel 7", device = PHONE_STD, showBackground = true)
@Composable
private fun OnboardingStdPreview() { OnlyTrustedTheme { OnboardingScreen(onContinue = {}) } }

@Preview(name = "Onboarding — foldable", device = FOLD_UNFOLDED, showBackground = true)
@Composable
private fun OnboardingFoldPreview() { OnlyTrustedTheme { OnboardingScreen(onContinue = {}) } }

@Preview(name = "Onboarding — tablet", device = TABLET, showBackground = true)
@Composable
private fun OnboardingTabletPreview() { OnlyTrustedTheme { OnboardingScreen(onContinue = {}) } }

// --- ConsentScreen -------------------------------------------------------

@Preview(name = "Consent — short screen (critical)", device = SHORT_PHONE, showBackground = true)
@Composable
private fun ConsentShortPreview() { OnlyTrustedTheme { ConsentScreen(onConfirmed = {}) } }

@Preview(name = "Consent — Pixel 7", device = PHONE_STD, showBackground = true)
@Composable
private fun ConsentStdPreview() { OnlyTrustedTheme { ConsentScreen(onConfirmed = {}) } }

@Preview(name = "Consent — Pixel 7 Pro", device = PHONE_LARGE, showBackground = true)
@Composable
private fun ConsentLargePreview() { OnlyTrustedTheme { ConsentScreen(onConfirmed = {}) } }

@Preview(name = "Consent — foldable", device = FOLD_UNFOLDED, showBackground = true)
@Composable
private fun ConsentFoldPreview() { OnlyTrustedTheme { ConsentScreen(onConfirmed = {}) } }

// --- ProfileSetupScreen --------------------------------------------------

@Preview(name = "Profile Setup — Pixel 7", device = PHONE_STD, showBackground = true)
@Composable
private fun ProfileSetupStdPreview() { OnlyTrustedTheme { ProfileSetupScreen(onComplete = {}) } }

@Preview(name = "Profile Setup — small phone", device = PHONE_SMALL, showBackground = true)
@Composable
private fun ProfileSetupSmallPreview() { OnlyTrustedTheme { ProfileSetupScreen(onComplete = {}) } }

// --- Security nag components --------------------------------------------

@Preview(name = "Nag card — Pixel 7", device = PHONE_STD, showBackground = true)
@Composable
private fun NagCardPreview() {
    OnlyTrustedTheme {
        SecurityNagCard(onEnable = {}, onSnooze = {})
    }
}

@Preview(name = "Nag modal — Pixel 7", device = PHONE_STD, showBackground = true)
@Composable
private fun NagModalPreview() {
    OnlyTrustedTheme {
        SecurityNagModal(onEnable = {}, onLater = {}, onNeverAgain = {})
    }
}
