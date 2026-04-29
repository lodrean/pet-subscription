package com.example.subscription.data.remote

/**
 * TODO: Server-side verification via Google Play Developer API
 *
 * POST /api/verify-subscription
 * Body: { "token": purchaseToken, "productId": "premium_monthly" }
 *
 * Server should call:
 * https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{packageName}/
 * purchases/subscriptions/{subscriptionId}/tokens/{token}
 */
interface SubscriptionVerifyService {
    // Retrofit interface will be implemented here
}
