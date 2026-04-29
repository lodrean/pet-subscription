package com.example.subscription.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription.data.billing.BillingRepository
import com.example.subscription.domain.model.SubscriptionTier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val billingRepository: BillingRepository
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = billingRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val products: StateFlow<List<SubscriptionTier>> = billingRepository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading: StateFlow<Boolean> = billingRepository.isLoading
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val errorMessage: StateFlow<String?> = billingRepository.errorMessage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        billingRepository.startConnection()
    }

    fun purchase(activity: android.app.Activity, tier: SubscriptionTier) {
        billingRepository.launchBillingFlow(activity, tier)
    }

    fun refresh() {
        viewModelScope.launch {
            billingRepository.checkExistingPurchases()
            billingRepository.queryProducts()
        }
    }

    fun enableDemoPremium() {
        billingRepository.enableDemoPremium()
    }

    override fun onCleared() {
        super.onCleared()
        billingRepository.endConnection()
    }
}
