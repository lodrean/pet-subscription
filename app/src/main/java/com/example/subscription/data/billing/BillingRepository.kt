package com.example.subscription.data.billing

import android.app.Activity
import com.example.subscription.domain.model.SubscriptionTier
import kotlinx.coroutines.flow.StateFlow

sealed interface BillingResult {
    data object Success : BillingResult
    data class Error(val message: String) : BillingResult
    data object Cancelled : BillingResult
}

interface BillingRepository {
    val isPremium: StateFlow<Boolean>
    val products: StateFlow<List<SubscriptionTier>>
    val isLoading: StateFlow<Boolean>
    val errorMessage: StateFlow<String?>

    fun startConnection(onReady: () -> Unit = {})
    fun endConnection()
    suspend fun queryProducts()
    fun launchBillingFlow(activity: Activity, tier: SubscriptionTier)
    suspend fun checkExistingPurchases()
}
