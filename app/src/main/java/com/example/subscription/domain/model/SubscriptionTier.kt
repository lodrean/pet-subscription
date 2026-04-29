package com.example.subscription.domain.model

data class SubscriptionTier(
    val productId: String,
    val name: String,
    val price: String,
    val billingPeriod: String,
    val offerToken: String? = null
)
