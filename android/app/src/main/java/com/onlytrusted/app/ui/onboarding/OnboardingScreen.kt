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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.R
import com.onlytrusted.app.privacy.OnboardingState
import com.onlytrusted.app.ui.theme.BrandAccentDark
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBackground)
            // Respect status bar + navigation bar so the hero image + sticky
            // "Continue" button don't render under the system chrome.
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 0.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CinematicHero()
            Headline()
            BenefitList()
            PageIndicator(activeIndex = 1, count = 3)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MidnightBackground)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Button(
                onClick = {
                    OnboardingState.markOnboarded(context)
                    onContinue()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandCyanDark,
                    contentColor = MidnightBackground
                )
            ) {
                Text(
                    "Get Started",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MidnightBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// MARK: - Hero (full-width image with wordmark overlay)

@Composable
private fun CinematicHero() {
    // Hero must show the full illustration (shield + family + mountains),
    // not crop the family silhouette. Content below scrolls, so there's no
    // layout cost to keeping the hero tall. 8 dp top margin keeps it clear
    // of the camera cutout.
    val heroHeight = 420.dp
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(heroHeight)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, BrandCyanDark.copy(alpha = 0.36f), RoundedCornerShape(22.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.onboarding_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )
        // "OnlyTrusted" wordmark overlaid on top of hero (white Only + teal Trusted)
        val wordmark = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White)) { append("Only") }
            withStyle(SpanStyle(color = BrandCyanDark)) { append("Trusted") }
        }
        Text(
            text = wordmark,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun Headline() {
    Text(
        text = "A safer, quieter phone\nstarts here.",
        color = PrimaryTextDark,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun BenefitList() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BenefitTile(
            icon = Icons.Filled.Groups,
            title = "Allow only trusted contacts",
            subtitle = "Keep your conversations with people you trust."
        )
        BenefitTile(
            icon = Icons.Filled.NotificationsOff,
            title = "Silence or block everyone else",
            subtitle = "Reduce unwanted calls and messages."
        )
        BenefitTile(
            icon = Icons.Filled.History,
            title = "Keep a local audit log",
            subtitle = "Review recent calls and messages on your device."
        )
        BenefitTile(
            icon = Icons.Filled.Shield,
            title = "Your data never leaves this device",
            subtitle = "Everything stays private and on your phone."
        )
        BenefitTile(
            icon = Icons.Filled.Info,
            title = "Important to know",
            subtitle = "Android has system limitations. OnlyTrusted cannot monitor other apps, your microphone, or your camera. Call blocking and message filtering use Android's supported features."
        )
    }
}

@Composable
private fun BenefitTile(icon: ImageVector, title: String, subtitle: String) {
    // Decorative tile only — no onClick, no chevron. The chevron used to
    // suggest navigability but there's no deeper content; removing it so
    // the tiles clearly read as info, not tappable rows.
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightElevated)
            .border(1.dp, BrandCyanDark.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .background(BrandCyanDark.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
        ) {
            Icon(icon, contentDescription = null, tint = BrandCyanDark, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                color = PrimaryTextDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                color = SecondaryTextDark,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PageIndicator(activeIndex: Int, count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        for (i in 0 until count) {
            val active = i == activeIndex
            Box(
                modifier = Modifier
                    .size(if (active) 9.dp else 7.dp)
                    .background(
                        color = if (active) BrandAccentDark else BrandAccentDark.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    }
}
