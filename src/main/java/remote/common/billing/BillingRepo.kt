package remote.common.billing

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData

object BillingRepo {
    private lateinit var skuArray: Array<String>
    private lateinit var billingDataSource: BillingDataSource
    private val isPurchasedMap = hashMapOf<String, LiveData<Boolean>>()
    private val canPurchaseMap = hashMapOf<String, LiveData<Boolean>>()

    fun init(context: Context, skus: Array<String>) {
        skuArray = skus
        billingDataSource = BillingDataSource.getInstance(context, null, skuArray, null)
        skuArray.forEach {
            isPurchasedMap[it] = billingDataSource.isPurchased(it)
            canPurchaseMap[it] = billingDataSource.canPurchase(it)
        }
    }

    fun getIsPurchasedMap(): HashMap<String, LiveData<Boolean>> {
        return isPurchasedMap
    }

    fun getCanPurchasedMap(): HashMap<String, LiveData<Boolean>> {
        return canPurchaseMap
    }

    fun launchBilling(activity: Activity, sku: String, upgradeSku: String? = null) {
        billingDataSource.launchBillingFlow(activity, sku, upgradeSku)
    }

    fun refreshStatus() {
        billingDataSource.refreshPurchasesAsync()
    }

    fun getLifecycleObserver(): LifecycleObserver {
        return billingDataSource
    }
}