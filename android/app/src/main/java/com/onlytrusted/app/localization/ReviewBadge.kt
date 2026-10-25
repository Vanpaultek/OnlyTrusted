// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.localization

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import com.onlytrusted.app.R

private const val REVIEW_PREFIX = "[REVIEW] "

/** Returns the localised string with any `[REVIEW] ` sentinel stripped. */
@Composable
fun stringResourceReviewed(@StringRes id: Int): String {
    val raw = stringResource(id)
    return if (raw.startsWith(REVIEW_PREFIX)) raw.removePrefix(REVIEW_PREFIX) else raw
}

@Composable
fun stringResourceReviewed(@StringRes id: Int, vararg args: Any): String {
    val raw = stringResource(id, *args)
    return if (raw.startsWith(REVIEW_PREFIX)) raw.removePrefix(REVIEW_PREFIX) else raw
}

/**
 * Renders a localised string and, if the resolved value carries the `[REVIEW] `
 * sentinel (machine-translated safety-critical string pending native review),
 * appends a small amber warning line below. Use at the point where safety-critical
 * strings surface — SOS text, duress banners, legal disclosures, system-alert bodies.
 *
 * Mirrors the SwiftUI `ReviewBadge` on iOS so both platforms behave identically.
 */
@Composable
fun ReviewAwareText(
    @StringRes id: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
) {
    val raw = stringResource(id)
    val isReview = raw.startsWith(REVIEW_PREFIX)
    val visible = if (isReview) raw.removePrefix(REVIEW_PREFIX) else raw
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = visible, color = color, style = style, fontWeight = fontWeight)
        if (isReview) {
            Text(
                text = stringResource(R.string.language_pending_review),
                fontSize = 10.sp,
                color = Color(0xFFF2B347),
                fontStyle = FontStyle.Italic
            )
        }
    }
}
