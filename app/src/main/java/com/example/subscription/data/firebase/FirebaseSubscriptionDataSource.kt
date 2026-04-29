package com.example.subscription.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseSubscriptionDataSource(
    private val firestore: FirebaseFirestore,
    private val deviceId: String
) {
    companion object {
        private const val COLLECTION = "subscriptions"
    }

    suspend fun saveSubscriptionStatus(
        productId: String,
        isPremium: Boolean,
        purchaseToken: String? = null
    ) {
        val data = hashMapOf(
            "deviceId" to deviceId,
            "productId" to productId,
            "isPremium" to isPremium,
            "purchaseToken" to (purchaseToken ?: ""),
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection(COLLECTION)
            .document(deviceId)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun getSubscriptionStatus(): Boolean {
        val snapshot = firestore.collection(COLLECTION)
            .document(deviceId)
            .get()
            .await()
        return snapshot.getBoolean("isPremium") ?: false
    }
}
