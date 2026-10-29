// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralised spacing / radius / typography constants for OnlyTrusted UI.
 *
 * Why this file exists
 * --------------------
 * The app's layout was built via tester-driven iteration — every screen
 * invented its own paddings, radii, and font sizes as issues came up.
 * That's fine for shipping internal betas but accumulates inconsistency.
 *
 * New code (and refactors of existing screens) should reference values
 * from here instead of hard-coding dp / sp literals. Changes to the
 * design system land once, propagate everywhere.
 *
 * Not a full migration: existing hard-coded values are left in place
 * where they already work. Replace opportunistically, not as a mass
 * search-and-replace that risks regressions.
 */
object Spacing {
    /** 4 dp — hairline / tight stacks (chip paddings, between icon + label). */
    val xs = 4.dp
    /** 8 dp — compact (within a Row that groups related elements). */
    val sm = 8.dp
    /** 12 dp — standard horizontal rhythm inside a card. */
    val md = 12.dp
    /** 16 dp — standard vertical rhythm between cards + screen edge gutter. */
    val lg = 16.dp
    /** 24 dp — section separator; between major content blocks. */
    val xl = 24.dp
    /** 32 dp — top-of-screen breathing room beneath the system bars. */
    val xxl = 32.dp
}

object Radius {
    /** 8 dp — small chips and icon tiles. */
    val sm = 8.dp
    /** 12 dp — standard card radius (status cards, settings rows). */
    val md = 12.dp
    /** 16 dp — primary cards (Dashboard tiles, Consent toggles). */
    val lg = 16.dp
    /** 22 dp — hero containers (Onboarding + Consent heroes). */
    val xl = 22.dp
    /** 28 dp — primary CTAs (pill buttons that span the full width). */
    val button = 28.dp
}

object FontSizes {
    /** 10 sp — footnote / legal micro-copy. */
    val footnote = 10.sp
    /** 11 sp — supporting labels under a tile. */
    val caption = 11.sp
    /** 12 sp — secondary explanatory copy under a title. */
    val body2 = 12.sp
    /** 13 sp — body copy inside a card. */
    val body = 13.sp
    /** 14 sp — primary body + inline action labels. */
    val bodyEmphasis = 14.sp
    /** 16 sp — primary CTA label. */
    val cta = 16.sp
    /** 20 sp — section headers inside a screen. */
    val sectionHeader = 20.sp
    /** 24 sp — screen title (compact variant for weighted layouts). */
    val titleCompact = 24.sp
    /** 28 sp — full screen title (used only when hero + content fit together). */
    val titleFull = 28.sp
}

object IconSizes {
    /** 14 dp — inline hint icon next to a label. */
    val hint = 14.dp
    /** 16 dp — icon beside body text. */
    val sm = 16.dp
    /** 18 dp — compact row icon (settings rows). */
    val md = 18.dp
    /** 22 dp — primary card icon. */
    val lg = 22.dp
    /** 40 dp — rounded icon tile with background on compact cards. */
    val tileCompact = 40.dp
    /** 48 dp — rounded icon tile with background on standard cards. */
    val tile = 48.dp
}

object Elevation {
    /** For status cards sitting on top of the dark backdrop. */
    val card = 0.dp
    /** For raised cards that need to pop (currently unused — flat UI). */
    val raised = 2.dp
}
