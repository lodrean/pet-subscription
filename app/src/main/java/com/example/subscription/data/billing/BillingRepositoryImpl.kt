package com.example.subscription.data.billing

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult as GoogleBillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.example.subscription.data.firebase.FirebaseSubscriptionDataSource
import com.example.subscription.domain.model.SubscriptionTier
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingRepositoryImpl(
    private val context: Context,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val firestoreDataSource: FirebaseSubscriptionDataSource
) : BillingRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _products = MutableStateFlow<List<SubscriptionTier>>(emptyList())
    override val products: StateFlow<List<SubscriptionTier>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val productDetailsCache = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED) {
                        scope.launch {
                            acknowledgePurchase(purchase.purchaseToken)
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _errorMessage.value = "Покупка отменена пользователем"
            }
            else -> {
                _errorMessage.value = "Ошибка покупки: ${result.debugMessage}"
            }
        }
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    override fun startConnection(onReady: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: GoogleBillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        checkExistingPurchases()
                        queryProducts()
                        onReady()
                    }
                } else {
                    _errorMessage.value = "Billing setup failed: ${billingResult.debugMessage}"
                }
            }

            override fun onBillingServiceDisconnected() {
                // BillingClient в PBL 8+ сам переподключается
            }
        })
    }

    override fun endConnection() {
        billingClient.endConnection()
    }

    override suspend fun queryProducts() {
        _isLoading.value = true
        firebaseAnalytics.logEvent("billing_query_products_start", Bundle())
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_yearly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = suspendCancellableCoroutine<com.android.billingclient.api.QueryProductDetailsResult> { continuation ->
            billingClient.queryProductDetailsAsync(params, ProductDetailsResponseListener { _, queryResult ->
                continuation.resume(queryResult)
            })
        }
        val detailsList = result.productDetailsList ?: emptyList()

        val tiers = detailsList.mapNotNull { details ->
            productDetailsCache[details.productId] = details
            val offer = details.subscriptionOfferDetails?.firstOrNull()
            val pricingPhase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
            if (offer != null && pricingPhase != null) {
                SubscriptionTier(
                    productId = details.productId,
                    name = details.name,
                    price = pricingPhase.formattedPrice,
                    billingPeriod = pricingPhase.billingPeriod,
                    offerToken = offer.offerToken
                )
            } else null
        }

        _products.value = tiers
        _isLoading.value = false
        val bundle = Bundle().apply { putInt("count", tiers.size) }
        firebaseAnalytics.logEvent("billing_products_loaded", bundle)
    }

    override fun launchBillingFlow(activity: Activity, tier: SubscriptionTier) {
        val params = Bundle().apply {
            putString("product_id", tier.productId)
            putString("price", tier.price)
        }
        firebaseAnalytics.logEvent("billing_purchase_start", params)

        val productDetails = productDetailsCache[tier.productId] ?: run {
            _errorMessage.value = "Product details not found. Call queryProducts() first."
            firebaseAnalytics.logEvent("billing_purchase_error", Bundle().apply {
                putString("reason", "cache_miss")
            })
            return
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    override suspend fun checkExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = suspendCancellableCoroutine<List<com.android.billingclient.api.Purchase>> { continuation ->
            billingClient.queryPurchasesAsync(params, PurchasesResponseListener { _, purchases ->
                continuation.resume(purchases ?: emptyList())
            })
        }

        val hasActive = result.any { purchase ->
            purchase.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
                    && purchase.isAcknowledged
        }
        _isPremium.value = hasActive
        runCatching {
            firestoreDataSource.saveSubscriptionStatus(
                productId = result.firstOrNull()?.products?.firstOrNull() ?: "unknown",
                isPremium = hasActive
            )
        }
    }

    private suspend fun acknowledgePurchase(token: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build()

        val result = suspendCancellableCoroutine<GoogleBillingResult> { continuation ->
            billingClient.acknowledgePurchase(params, AcknowledgePurchaseResponseListener {
                continuation.resume(it)
            })
        }

        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            _isPremium.value = true
            _errorMessage.value = null
            val bundle = Bundle().apply {
                putString("purchase_token", token.take(8) + "...")
                putBoolean("active", true)
            }
            firebaseAnalytics.logEvent("billing_purchase_success", bundle)
            runCatching {
                firestoreDataSource.saveSubscriptionStatus(
                    productId = "premium_subscription",
                    isPremium = true,
                    purchaseToken = token
                )
            }
        } else {
            _errorMessage.value = "Acknowledge failed: ${result.debugMessage}"
            firebaseAnalytics.logEvent("billing_purchase_error", Bundle().apply {
                putString("reason", "acknowledge_failed")
                putString("message", result.debugMessage)
            })
        }
    }
}
