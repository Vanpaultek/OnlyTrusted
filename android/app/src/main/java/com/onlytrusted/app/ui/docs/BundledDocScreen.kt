// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.docs

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.onlytrusted.app.ui.theme.MidnightBackground
import com.onlytrusted.app.ui.theme.PrimaryTextDark

/**
 * Loads a bundled static HTML site inside an Android WebView from file:///android_asset/.
 * Used by the in-app User Guide and Pricing Chart — both ship as asset folders so they
 * work 100% offline (matches the zero-cloud ethos).
 *
 * @param folder asset subdirectory name (e.g. "guide" or "pricing").
 */
@Composable
fun BundledDocScreen(folder: String, title: String, onBack: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Simple top bar matching the AboutScreen's sub-screen chrome.
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryTextDark
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(title, color = PrimaryTextDark, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(MidnightBackground.toArgb())
                        settings.javaScriptEnabled = true
                        settings.allowFileAccess = true
                        // allowFileAccessFromFileURLs lets fetch() reach content.json /
                        // plans.json / assets/* from the WebView's file:// origin — needed
                        // for the guide's dynamic content load.
                        @Suppress("DEPRECATION")
                        settings.allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        settings.allowUniversalAccessFromFileURLs = true
                        webViewClient = WebViewClient()
                        loadUrl("file:///android_asset/$folder/index.html")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

