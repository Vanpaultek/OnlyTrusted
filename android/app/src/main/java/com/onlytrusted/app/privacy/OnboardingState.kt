// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.privacy

import android.content.Context

object OnboardingState {
    private const val PREFS = "onboarding_prefs"
    private const val KEY_ONBOARDED = "onboarded_v1"
    private const val KEY_CONSENT_ACCEPTED = "consent_accepted_v1"
    private const val KEY_CONSENT_AT = "consent_accepted_at"
    private const val KEY_IS_ADULT = "is_adult"
    private const val KEY_OWNS_DEVICE = "owns_device"
    private const val KEY_UNDERSTOOD_LIMITS = "understood_limits"
    private const val KEY_PERMISSIONS_REVIEWED = "permissions_reviewed_v1"

    fun hasOnboarded(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ONBOARDED, false)

    fun markOnboarded(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_ONBOARDED, true).apply()
    }

    fun hasAccepted(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_CONSENT_ACCEPTED, false)

    fun accept(
        ctx: Context,
        isAdult: Boolean,
        ownsDevice: Boolean,
        understandsLimits: Boolean
    ) {
        require(isAdult && ownsDevice && understandsLimits) {
            "All three confirmations are required to enter the app."
        }
        prefs(ctx).edit()
            .putBoolean(KEY_CONSENT_ACCEPTED, true)
            .putLong(KEY_CONSENT_AT, System.currentTimeMillis())
            .putBoolean(KEY_IS_ADULT, isAdult)
            .putBoolean(KEY_OWNS_DEVICE, ownsDevice)
            .putBoolean(KEY_UNDERSTOOD_LIMITS, understandsLimits)
            .apply()
    }

    fun hasReviewedPermissions(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_PERMISSIONS_REVIEWED, false)

    /** Mark the permissions overview as reviewed, whether the user granted
     *  everything, some, or nothing. The flag only tracks that we showed
     *  the review screen; feature-level permission prompts still appear
     *  when a specific permission is required. */
    fun markPermissionsReviewed(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_PERMISSIONS_REVIEWED, true).apply()
    }

    fun reset(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
