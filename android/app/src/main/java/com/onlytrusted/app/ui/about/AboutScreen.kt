// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.about

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image as FoundationImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytrusted.app.BuildConfig
import com.onlytrusted.app.R
import com.onlytrusted.app.ui.docs.BundledDocScreen
import com.onlytrusted.app.ui.subscription.SubscriptionState
import com.onlytrusted.app.ui.theme.BrandCyanDark
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.appBackdropBackground
import com.onlytrusted.app.ui.theme.MidnightElevated
import com.onlytrusted.app.ui.theme.PrimaryTextDark
import com.onlytrusted.app.ui.theme.SecondaryTextDark
import com.onlytrusted.app.ui.theme.TertiaryTextDark

/**
 * Public source repository URL. Confirmed from `git remote get-url origin` as
 * `https://github.com/Vanpaultek/OnlyTrusted.git`. Update this constant if the
 * repo is renamed or moved under the VanPaulTek org.
 */
private const val SOURCE_URL = "https://github.com/Vanpaultek/OnlyTrusted"
private const val SUPPORT_EMAIL = "admin@vanpaultek.com"

// Icon-chip tints used by the About rows
private val SettingsBlue = Color(0xFF2266F0)
private val SettingsPurple = Color(0xFF8B5CF6)
private val AboutGreen = Color(0xFF22C55E)
private val AboutOrange = Color(0xFFF59E0B)
private val AboutPink = Color(0xFFEC4899)

private enum class AboutSubScreen { None, Acknowledgments, ThirdParty, AppIcons, UserGuide, PricingChart }

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    var subScreen by remember { mutableStateOf(AboutSubScreen.None) }

    when (subScreen) {
        AboutSubScreen.Acknowledgments ->
            AcknowledgmentsScreen(onBack = { subScreen = AboutSubScreen.None })
        AboutSubScreen.ThirdParty ->
            ThirdPartyLibrariesScreen(onBack = { subScreen = AboutSubScreen.None })
        AboutSubScreen.AppIcons ->
            AppIconsAttributionScreen(onBack = { subScreen = AboutSubScreen.None })
        AboutSubScreen.UserGuide ->
            BundledDocScreen(folder = "guide", title = "User Guide",
                             onBack = { subScreen = AboutSubScreen.None })
        AboutSubScreen.PricingChart ->
            BundledDocScreen(folder = "pricing", title = "Pricing Chart",
                             onBack = { subScreen = AboutSubScreen.None })
        AboutSubScreen.None -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .appBackdropBackground()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    CinematicHero(
                        onBack = { backDispatcherOwner?.onBackPressedDispatcher?.onBackPressed() }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AboutRow(
                            icon = Icons.Filled.Person,
                            tint = SettingsBlue,
                            title = "App Owner",
                            subtitle = "Company or individual information.",
                            value = "VanPaulTek Inc",
                            onClick = null
                        )
                        AboutRow(
                            icon = Icons.Filled.Email,
                            tint = SettingsPurple,
                            title = "Support Email",
                            subtitle = "Get help or contact support.",
                            value = SUPPORT_EMAIL,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$SUPPORT_EMAIL")
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        "OnlyTrusted ${BuildConfig.VERSION_NAME} support"
                                    )
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            }
                        )
                        // AGPL-3.0 §4-5 require the license + source link to stay visible to
                        // every user, not just subscribers.
                        AboutRow(
                            icon = Icons.AutoMirrored.Filled.Article,
                            tint = AboutGreen,
                            title = "License",
                            subtitle = "Open source and usage license.",
                            value = "GNU AGPL-3.0",
                            onClick = null
                        )
                        AboutRow(
                            icon = Icons.Filled.Code,
                            tint = SettingsBlue,
                            title = "Open Source",
                            subtitle = "View source code on GitHub.",
                            value = "View on GitHub",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { context.startActivity(intent) }
                            }
                        )
                        AboutRow(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            tint = AboutGreen,
                            title = "User Guide",
                            subtitle = "Every feature, how to find it, what it does.",
                            value = null,
                            onClick = { subScreen = AboutSubScreen.UserGuide }
                        )
                        AboutRow(
                            icon = Icons.Filled.LocalOffer,
                            tint = AboutOrange,
                            title = "Pricing Chart",
                            subtitle = "All plans and features, side by side.",
                            value = null,
                            onClick = { subScreen = AboutSubScreen.PricingChart }
                        )
                        if (SubscriptionState.isActive(context)) {
                            AboutRow(
                                icon = Icons.Filled.EmojiEvents,
                                tint = AboutOrange,
                                title = "Acknowledgements",
                                subtitle = "Libraries, tools and contributors.",
                                value = null,
                                onClick = { subScreen = AboutSubScreen.Acknowledgments }
                            )
                            AboutRow(
                                icon = Icons.Filled.ViewInAr,
                                tint = SettingsPurple,
                                title = "Third-Party Libraries",
                                subtitle = "View third-party components.",
                                value = null,
                                onClick = { subScreen = AboutSubScreen.ThirdParty }
                            )
                            AboutRow(
                                icon = Icons.Filled.GridView,
                                tint = AboutPink,
                                title = "App Icons",
                                subtitle = "App icons and brand assets.",
                                value = null,
                                onClick = { subScreen = AboutSubScreen.AppIcons }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Cinematic hero (lakescape header) ----------

@Composable
private fun CinematicHero(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Lakescape backdrop
        FoundationImage(
            painter = painterResource(id = R.drawable.splash_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.TopCenter
        )
        // Vertical gradient overlay fading to MidnightBackground at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MidnightBackground.copy(alpha = 0.1f),
                            MidnightBackground.copy(alpha = 0.5f),
                            MidnightBackground
                        )
                    )
                )
        )

        // "< Settings" back link top-left — respect status bar inset on edge-to-edge devices.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 18.dp, top = 14.dp)
                .clickable(onClick = onBack)
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "Back to Settings",
                tint = BrandCyanDark,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = "Settings",
                color = BrandCyanDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bottom-left: About label + wordmark + version
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 18.dp)
        ) {
            Text(
                text = "About",
                color = PrimaryTextDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("Only") }
                    withStyle(SpanStyle(color = BrandCyanDark)) { append("Trusted") }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                color = SecondaryTextDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ---------- About row (icon chip + title + subtitle + optional value + chevron) ----------

@Composable
private fun AboutRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    value: String?,
    onClick: (() -> Unit)?
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidnightElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyanDark.copy(alpha = 0.08f)),
        modifier = rowModifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GradientIconChip(icon = icon, tint = tint)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    title,
                    color = PrimaryTextDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = SecondaryTextDark,
                    fontSize = 13.sp
                )
            }
            if (value != null) {
                Text(
                    value,
                    color = BrandCyanDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = BrandCyanDark,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun GradientIconChip(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(11.dp),
                ambientColor = tint.copy(alpha = 0.5f),
                spotColor = tint.copy(alpha = 0.5f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(tint, tint.copy(alpha = 0.75f))
                ),
                shape = RoundedCornerShape(11.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ---------- Sub-screens ----------

@Composable
private fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .appBackdropBackground()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryTextDark
                    )
                }
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTextDark
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SubScreenCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidnightElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

private data class CreditEntry(val title: String, val body: String)

@Composable
private fun AcknowledgmentsScreen(onBack: () -> Unit) {
    val entries = listOf(
        CreditEntry(
            "Our testers",
            "Everyone who ran the early builds on their own devices and reported bugs."
        ),
        CreditEntry(
            "Open-source maintainers",
            "The authors of every project in Third-Party Libraries. Their work made this app possible."
        ),
        CreditEntry(
            "Users who filed bug reports",
            "Especially the people whose block lists and SMS filter logs helped us find the rough edges."
        )
    )
    SubScreenScaffold(title = "Acknowledgments", onBack = onBack) {
        SubScreenCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                entries.forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            entry.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryTextDark
                        )
                        Text(entry.body, fontSize = 12.sp, color = SecondaryTextDark)
                    }
                }
            }
        }
    }
}

private data class LibraryEntry(val name: String, val license: String, val url: String)

@Composable
private fun ThirdPartyLibrariesScreen(onBack: () -> Unit) {
    val libraries = listOf(
        LibraryEntry("AndroidX libraries", "Apache License 2.0", "developer.android.com/jetpack/androidx"),
        LibraryEntry("Jetpack Compose", "Apache License 2.0", "developer.android.com/jetpack/compose"),
        LibraryEntry("Material 3 for Compose", "Apache License 2.0", "m3.material.io"),
        LibraryEntry("Room", "Apache License 2.0", "developer.android.com/training/data-storage/room"),
        LibraryEntry("SQLCipher for Android", "BSD-style", "github.com/sqlcipher/android-database-sqlcipher"),
        LibraryEntry("Kotlin + Coroutines", "Apache License 2.0", "kotlinlang.org"),
        LibraryEntry("Accompanist Permissions", "Apache License 2.0", "google.github.io/accompanist")
    )
    SubScreenScaffold(title = "Third-Party Libraries", onBack = onBack) {
        SubScreenCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                libraries.forEach { lib ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            lib.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryTextDark
                        )
                        Text(
                            "${lib.license}  ·  ${lib.url}",
                            fontSize = 11.sp,
                            color = SecondaryTextDark
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Attribution links are provided for reference. OnlyTrusted does not make any network requests on your behalf.",
                    fontSize = 11.sp,
                    color = TertiaryTextDark
                )
            }
        }
    }
}

@Composable
private fun AppIconsAttributionScreen(onBack: () -> Unit) {
    SubScreenScaffold(title = "App Icons", onBack = onBack) {
        SubScreenCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Material Icons",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryTextDark
                    )
                    Text(
                        "All in-app icons are drawn from Google's Material Icons set, used under the Apache License 2.0.",
                        fontSize = 12.sp,
                        color = SecondaryTextDark
                    )
                    Text(
                        "fonts.google.com/icons",
                        fontSize = 11.sp,
                        color = TertiaryTextDark
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Launcher icon",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryTextDark
                    )
                    Text(
                        "The OnlyTrusted shield mark is an original work by VanPaulTek Inc, licensed under GNU AGPL-3.0 along with the rest of the app.",
                        fontSize = 12.sp,
                        color = SecondaryTextDark
                    )
                }
            }
        }
    }
}
