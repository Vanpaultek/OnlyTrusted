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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBackground)
            // Respect status bar + nav bar so the hero + sticky toggles + CTA
            // don't render under the system chrome.
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 0.dp, bottom = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CinematicHeader()
            TitleBlock()
            ToggleCards(
                isAdult = isAdult, onAdultChange = { isAdult = it },
                ownsDevice = ownsDevice, onOwnerChange = { ownsDevice = it },
                understandsLimits = understandsLimits, onLimitsChange = { understandsLimits = it }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MidnightBackground)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                Text(
                    "I confirm and continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            LegalFootnote(onSelect = { legalSheetSection = it })
        }

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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Fade bottom edge into screen bg
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(90.dp)
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 10.dp)
    ) {
        Text(
            "Please confirm",
            color = PrimaryTextDark,
            fontSize = 28.sp,
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
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightElevated)
            .border(1.dp, BrandCyanDark.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(BrandCyanDark.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
        ) {
            Icon(icon, contentDescription = null, tint = BrandCyanDark, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = PrimaryTextDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = SecondaryTextDark, fontSize = 13.sp)
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
