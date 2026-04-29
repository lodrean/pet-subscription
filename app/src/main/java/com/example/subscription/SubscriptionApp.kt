package com.example.subscription

import android.app.Application
import com.example.subscription.di.appModule
import com.example.subscription.di.billingModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SubscriptionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SubscriptionApp)
            modules(appModule, billingModule)
        }
    }
}
