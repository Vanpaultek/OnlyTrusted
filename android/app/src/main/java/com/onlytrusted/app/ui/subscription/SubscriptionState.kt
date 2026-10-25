// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.ui.subscription

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Local-only subscription state that mirrors the iOS `SubscriptionState`.
 * Backed by SharedPreferences; swap for Google Play Billing entitlements once IAP lands.
 *
 * All reads/writes go through [SubscriptionState.get] so the Billing and Upgrade screens
 * share the same source of truth.
 */
/**
 * Tier ladder — mirrors iOS `SubscriptionTier`. Each paid tier now has an Individual
 * (1 seat) + Family (5 seats) variant priced separately; feature-gating is still
 * rank-based so a Family variant unlocks the SAME features as its Individual sibling
 * (ranks shared across the two variants of the same tier). Elite is the top tier.
 *
 * Rank layout (shared ranks within a tier are intentional):
 *   0  FREE
 *   1  PREMIUM, FAMILY (= Premium Family)
 *   2  HIGH_PREMIUM (Pro), HIGH_PREMIUM_FAMILY (Pro Family)
 *   3  ELITE, ELITE_FAMILY
 */
enum class SubscriptionTier(val displayName: String, val rank: Int, val seats: Int) {
    FREE("Free", 0, 1),
    PREMIUM("Premium", 1, 1),
    FAMILY("Premium Family", 1, 5),
    HIGH_PREMIUM("Pro", 2, 1),
    HIGH_PREMIUM_FAMILY("Pro Family", 2, 5),
    ELITE("Elite", 3, 1),
    ELITE_FAMILY("Elite Family", 3, 5);

    companion object {
        fun fromKey(key: String?): SubscriptionTier =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: FREE
    }
}

object SubscriptionState {
    private const val PREFS = "subscription_prefs_v1"

    // Keys match the iOS UserDefaults keys so the strings stay recognisable.
    private const val KEY_ACTIVE = "subscription.v1.active"
    private const val KEY_TIER = "subscription.v1.tier"
    private const val KEY_PLAN_NAME = "subscription.v1.plan_name"
    private const val KEY_PRICING = "subscription.v1.pricing"
    private const val KEY_NEXT_BILLING = "subscription.v1.next_billing"
    private const val KEY_LAST_FOUR = "subscription.v1.last_four"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun defaultNextBilling(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 335)
        return cal.timeInMillis
    }

    fun isActive(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ACTIVE, false)
    fun setActive(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ACTIVE, value).apply()
    }

    fun tier(ctx: Context): SubscriptionTier =
        SubscriptionTier.fromKey(prefs(ctx).getString(KEY_TIER, null))

    /** True on any paid tier (Premium Individual/Family, Pro Individual/Family, Elite Individual/Family). */
    fun hasPremium(ctx: Context): Boolean =
        isActive(ctx) && tier(ctx).rank >= SubscriptionTier.PREMIUM.rank

    /** True on Pro Individual, Pro Family, Elite Individual, Elite Family. */
    fun hasHighPremium(ctx: Context): Boolean =
        isActive(ctx) && tier(ctx).rank >= SubscriptionTier.HIGH_PREMIUM.rank

    /** True on Elite Individual + Elite Family. */
    fun hasElite(ctx: Context): Boolean =
        isActive(ctx) && tier(ctx).rank >= SubscriptionTier.ELITE.rank

    fun planName(ctx: Context): String =
        prefs(ctx).getString(KEY_PLAN_NAME, null) ?: "Premium"

    fun pricingSummary(ctx: Context): String =
        prefs(ctx).getString(KEY_PRICING, null) ?: "$4.99 per year"

    fun nextBillingDateMillis(ctx: Context): Long =
        prefs(ctx).getLong(KEY_NEXT_BILLING, defaultNextBilling())

    fun nextBillingDate(ctx: Context): Date = Date(nextBillingDateMillis(ctx))

    fun lastFour(ctx: Context): String =
        prefs(ctx).getString(KEY_LAST_FOUR, null) ?: "4242"

    /** Format a date the same way the iOS screen does (`MMM d, yyyy`). */
    fun formatDate(date: Date): String =
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)

    /** Mock invoice history used by the Billing History sheet. */
    data class InvoiceEntry(
        val date: Date,
        val description: String,
        val amountCents: Int
    ) {
        val dateString: String get() = formatDate(date)
        val amountString: String get() = String.format(Locale.US, "$%.2f", amountCents / 100.0)
    }

    fun invoiceHistory(): List<InvoiceEntry> {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        return listOf(
            InvoiceEntry(Date(now - 365 * day), "Premium yearly", 499),
            InvoiceEntry(Date(now - 730 * day), "Premium yearly", 499)
        )
    }

    /**
     * Called after a successful (stubbed) purchase. Flips the active flag and persists the
     * plan details so the Billing screen reflects the new state next time it opens.
     */
    fun markPurchased(
        ctx: Context,
        tier: SubscriptionTier,
        plan: String,
        pricing: String,
        nextDate: Date
    ) {
        prefs(ctx).edit()
            .putBoolean(KEY_ACTIVE, tier != SubscriptionTier.FREE)
            .putString(KEY_TIER, tier.name)
            .putString(KEY_PLAN_NAME, plan)
            .putString(KEY_PRICING, pricing)
            .putLong(KEY_NEXT_BILLING, nextDate.time)
            .apply()
    }
}

/** Shared gold tint used by the crown badges. */
val subGold = Color(0xFFFABD3D)
