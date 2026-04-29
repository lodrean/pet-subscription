package com.example.subscription.di

import android.content.Context
import com.example.subscription.data.billing.BillingRepository
import com.example.subscription.data.billing.BillingRepositoryImpl
import com.example.subscription.data.firebase.FirebaseSubscriptionDataSource
import com.example.subscription.presentation.SubscriptionViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.UUID

val appModule = module {
    viewModel { SubscriptionViewModel(get()) }
}

val billingModule = module {
    single { Firebase.analytics }
    single { Firebase.firestore }
    single {
        val context: Context = get()
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }
    }
    single {
        FirebaseSubscriptionDataSource(
            firestore = get(),
            deviceId = get()
        )
    }
    single<BillingRepository> { BillingRepositoryImpl(get(), get(), get()) }
}
