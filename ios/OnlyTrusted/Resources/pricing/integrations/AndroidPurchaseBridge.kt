package com.onlytrusted.app.billing

import android.app.Activity
import android.webkit.JavascriptInterface
import com.android.billingclient.api.*
import org.json.JSONObject

/**
 * Integration outline for Google Play Billing. Build against a current Billing Library version.
 * In your Activity create the BillingClient, establish a connection, query ProductDetails,
 * then launch the billing flow. A product ID is NOT the base plan ID.
 * Never mark a purchase active until Play Developer API/server-side verification succeeds.
 */
class AndroidPurchaseBridge(
    private val activity: Activity,
    private val launchPurchase: (productId: String, basePlanId: String) -> Unit
) {
    @JavascriptInterface
    fun purchase(json: String) {
        val plan = JSONObject(json)
        val productId = plan.getString("googlePlayProductId")
        val basePlan = plan.getString("googlePlayBasePlanId")
        // Reject requests that do not match configured allowlisted subscriptions/base plans.
        val allowed = mapOf(
            "premium" to setOf("premium_monthly", "premium_yearly"),
            "premium_family" to setOf("family_monthly", "family_yearly"),
            "pro" to setOf("pro_monthly", "pro_yearly"),
            "pro_family" to setOf("pro_family_monthly", "pro_family_yearly"),
            "elite" to setOf("elite_monthly", "elite_yearly"),
            "elite_family" to setOf("elite_family_monthly", "elite_family_yearly")
        )
        if (basePlan !in allowed[productId].orEmpty()) return
        activity.runOnUiThread { launchPurchase(productId, basePlan) }
    }
}

/* Example Activity integration:
val webView: WebView = ...
webView.settings.javaScriptEnabled = true
// ONLY load trusted bundled content, never navigate this WebView to arbitrary websites.
webView.addJavascriptInterface(AndroidPurchaseBridge(this) { productId, basePlanId ->
    // queryProductDetailsAsync(ProductType.SUBS) and find corresponding offerDetails
    // by basePlanId, then launchBillingFlow(activity, params).
}, "OnlyTrustedAndroid")
webView.loadUrl("file:///android_asset/pricing/index.html")

Google Play purchase requirements:
1) Map product IDs to exact subscriptions configured in Play Console.
2) Query ProductDetails and select the eligible offer/basePlan token.
3) Launch BillingClient.launchBillingFlow on user selection.
4) Handle PurchasesUpdatedListener, process pending status, verify on the server.
5) Acknowledge purchases and manage renewals/cancellations/revocations.
*/
