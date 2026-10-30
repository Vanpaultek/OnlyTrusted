// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.R
import com.onlytrusted.app.privacy.OnboardingState
import com.onlytrusted.app.ui.legal.LegalScreen
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.MidnightCard
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark
import com.onlytrusted.app.ui.theme.TertiaryTextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(onConfirmed: () -> Unit) {
    val context = LocalContext.current
    var isAdult by remember { mutableStateOf(false) }
    var ownsDevice by remember { mutableStateOf(false) }
    var understandsLimits by remember { mutableStateOf(false) }
    var legalSheetSection by remember { mutableStateOf<String?>(null) }
    val legalSheetState = rememberModalBottomSheetState()

    val canContinue = isAdult && ownsDevice && understandsLimits

    // Weighted layout: hero takes ~40% of available vertical space, the three
    // toggles + CTA + legal footer fill the remaining ~60% with no scroll.
    // Adapts to any screen size because every dimension is a fraction / weight,
    // not a fixed dp. Removes the sticky-bottom + scroll approach that was
    // hiding the third toggle on Pixel-class phones.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.40f)
                .padding(top = 8.dp),
        ) { CinematicHeader() }

        // Tightened: hero and title block now sit flush, no wasted gap.
        Spacer(Modifier.height(4.dp))
        TitleBlock()

        Spacer(Modifier.height(10.dp))
        // Non-interactive heads-up: tells the user what Android will ask for
        // after Continue and what the Dashboard will do with Contacts, so
        // the auto-import flow doesn't feel surprising.
        PermissionHeadsUpCard()

        Spacer(Modifier.height(8.dp))
        // Three toggle cards share the remaining space between the title
        // and the CTA. weight(1f) keeps them visible without scroll even
        // on short screens — cards get a tighter vertical padding to fit.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ToggleCards(
                isAdult = isAdult, onAdultChange = { isAdult = it },
                ownsDevice = ownsDevice, onOwnerChange = { ownsDevice = it },
                understandsLimits = understandsLimits, onLimitsChange = { understandsLimits = it }
            )
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                OnboardingState.accept(context, isAdult, ownsDevice, understandsLimits)
                onConfirmed()
            },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandCyanDark,
                contentColor = MidnightBackground,
                disabledContainerColor = MidnightElevated.copy(alpha = 0.8f),
                disabledContentColor = TertiaryTextDark
            )
        ) {
            Text("I confirm and continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(8.dp))
        LegalFootnote(onSelect = { legalSheetSection = it })
        Spacer(Modifier.height(8.dp))

        legalSheetSection?.let { section ->
            ModalBottomSheet(
                onDismissRequest = { legalSheetSection = null },
                sheetState = legalSheetState,
                containerColor = MidnightCard
            ) {
                LegalScreen(initialSectionTitle = section)
            }
        }
    }
}

// MARK: - Hero + title block

@Composable
private fun CinematicHeader() {
    // Fills the parent Box (sized to 40% of screen height by caller). This
    // keeps the hero proportional on every device — tall on tablets, short
    // on compact phones — while the toggles + CTA always have the other 60%.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // Bias the crop very close to TopCenter so the shield's top edge
            // is fully preserved, with just enough shift away from exact-top
            // to avoid the pure starry sky above the shield. The mountain /
            // lake at the bottom gets clipped — that's intentional, there's
            // no essential content there.
            alignment = BiasAlignment(horizontalBias = 0f, verticalBias = -1f),
            modifier = Modifier.fillMaxSize()
        )
        // Fade bottom edge into screen bg
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MidnightBackground.copy(alpha = 0.55f), MidnightBackground)
                    )
                )
        )
    }
}

@Composable
private fun TitleBlock() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 10.dp)
    ) {
        Text(
            "Please confirm",
            color = PrimaryTextDark,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "To use OnlyTrusted, you must review and confirm the following.",
            color = SecondaryTextDark,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

// MARK: - Permission heads-up card

@Composable
private fun PermissionHeadsUpCard() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightCard)
            .border(1.dp, BrandAccentDark.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(
            Icons.Filled.Groups,
            contentDescription = null,
            tint = BrandAccentDark,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "After Continue",
                color = PrimaryTextDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Android will ask for Contacts, SMS, Phone, Mic, Camera. If you " +
                    "grant Contacts, we'll show how many we found before importing anything.",
                color = SecondaryTextDark,
                fontSize = 11.sp,
            )
        }
    }
}

// MARK: - Three toggle cards

@Composable
private fun ToggleCards(
    isAdult: Boolean, onAdultChange: (Boolean) -> Unit,
    ownsDevice: Boolean, onOwnerChange: (Boolean) -> Unit,
    understandsLimits: Boolean, onLimitsChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ConsentToggleCard(
            icon = Icons.Filled.Person,
            title = "I am an adult",
            detail = "I am 18 years or older.",
            checked = isAdult,
            onCheckedChange = onAdultChange
        )
        ConsentToggleCard(
            icon = Icons.Filled.PhoneIphone,
            title = "I own this device or am authorized to use it",
            detail = "I have the right to use and configure this device.",
            checked = ownsDevice,
            onCheckedChange = onOwnerChange
        )
        ConsentToggleCard(
            icon = Icons.Filled.Shield,
            title = "I have read and understood the honest limits",
            detail = "I understand what OnlyTrusted can do, what it cannot do, and any limitations on Android.",
            checked = understandsLimits,
            onCheckedChange = onLimitsChange
        )
    }
}

@Composable
private fun ConsentToggleCard(
    icon: ImageVector,
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // Compact variant: tighter paddings so three cards fit in the
    // weighted bottom half of the Consent screen on short devices.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightElevated)
            .border(1.dp, BrandCyanDark.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(BrandCyanDark.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
        ) {
            Icon(icon, contentDescription = null, tint = BrandCyanDark, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = PrimaryTextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = SecondaryTextDark, fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandAccentDark,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF4A5563)
            )
        )
    }
}

// MARK: - Legal footer

@Composable
private fun LegalFootnote(onSelect: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        LegalLink("Privacy Policy") { onSelect("Privacy policy") }
        LegalDivider()
        LegalLink("Terms of Use") { onSelect("Terms of use") }
        LegalDivider()
        LegalLink("Honest Claims") { onSelect("Honest claims") }
    }
}

@Composable
private fun LegalDivider() {
    Text(
        "|",
        color = TertiaryTextDark,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 10.dp)
    )
}

@Composable
private fun LegalLink(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(label) }
            },
            color = BrandAccentDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
