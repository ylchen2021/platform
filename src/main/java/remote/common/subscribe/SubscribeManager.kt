package remote.common.subscribe

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingFlowParams

object SubscribeManager {
    private const val TAG = "SubscribeManager"
    private lateinit var billingClient: BillingClient
    private val skuDetailsMap = HashMap<String, SkuDetails>()
    private var eventListeners: MutableList<SubscribeEventListener> = arrayListOf()
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // To be implemented in a later section.
            Log.d(TAG, "PurchasesUpdatedListener result=${billingResult.responseCode}")
        }
    private var connected = false
    private val billingClientStateListener = object : BillingClientStateListener {
        override fun onBillingServiceDisconnected() {
            connected = false
        }

        override fun onBillingSetupFinished(billingResult: BillingResult) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                connected = true
            } else {
                notifyEvent(SubscribeEventType.ERROR, billingResult.responseCode)
            }
        }
    }
    private var isVIP = false
    fun isVIP(): Boolean {
        return isVIP
    }

    fun enableVIP() {
        isVIP = true
    }

    fun addEventListener(listener: SubscribeEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }

    fun removeEventListener(listener: SubscribeEventListener) {
        if (eventListeners.contains(listener)) {
            eventListeners.remove(listener)
        }
    }

    fun init(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
    }

    fun isConnected(): Boolean {
        return connected
    }

    fun connectIfNeed() {
        if (connected) {
            return
        }
        billingClient.startConnection(billingClientStateListener)
    }

    fun buyProducts(sku: String, activity: Activity) {
        if (!connected) {
            return
        }
        val skuList = ArrayList<String>()
        skuList.add(sku)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)
        billingClient.querySkuDetailsAsync(params.build(), object : SkuDetailsResponseListener {
            override fun onSkuDetailsResponse(billingResult: BillingResult, skuDetailsList: MutableList<SkuDetails>?) {
                Log.d(TAG, "onSkuDetailsResponse result=${billingResult.responseCode}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val skuDetails = skuDetailsList?.get(0)
                    if (skuDetails != null) {
                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .build()
                        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
                        Log.d(TAG, "launchBillingFlow responseCode=${responseCode}")
                    }
                } else {
                    notifyEvent(SubscribeEventType.ERROR, billingResult.responseCode)
                }
            }
        })
    }

    private fun notifyEvent(type: SubscribeEventType, param: Any) {
        eventListeners.forEach {
            it.onEvent(SubscribeEvent(type, param))
        }
    }

    interface SubscribeEventListener {
        fun onEvent(subscribeEvent: SubscribeEvent)
    }
}