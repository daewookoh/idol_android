package net.ib.mn.billing.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.FeatureType
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import net.ib.mn.billing.util.Security.verifyPurchase
import net.ib.mn.utils.Util
import org.json.JSONException
import org.json.JSONObject

// 참고! https://github.com/googlesamples/android-play-billing/blob/master/TrivialDrive_v2/shared-module/src/main/java/com/example/billingmodule/billing/BillingManager.java
/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
class BillingManager(
    activity: Activity,
    base64PublicKey: String?,
    updatesListener: BillingUpdatesListener
) : PurchasesUpdatedListener {
    /** A reference to BillingClient  */
    private var mBillingClient: BillingClient?

    /**
     * True if billing service is connected now.
     */
    private var mIsServiceConnected = false
    private val mBillingUpdatesListener: BillingUpdatesListener
    private val mActivity: Activity
    private val mPurchases: MutableList<Purchase> = ArrayList()
    private var mTokensToBeConsumed: MutableSet<String>? = null

    //    private int mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;
    // Public key for verifying signature, in base64 encoding
    private val mSignatureBase64: String?
    @JvmField
    var isRestoring = false // 구매복원중인지

    /**
     * Listener to the updates that happen when purchases list was updated or consumption of the
     * item was finished
     */
    interface BillingUpdatesListener {
        fun onBillingClientSetupFinished()
        fun onBillingClientSetupFailed()
        fun onConsumeFinished(purchase: Purchase?, result: Int)
        fun onAcknowledgeFinished(purchase: Purchase?, result: Int)
        fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?)
    }

    /**
     * Listener for the Billing client state to become connected
     */
    interface ServiceConnectedListener { //        void onServiceConnected(int resultCode);
    }

    init {
        Log.d(TAG, "Creating Billing client.")
        mActivity = activity
        mSignatureBase64 = base64PublicKey
        mBillingUpdatesListener = updatesListener
        mBillingClient = BillingClient.newBuilder(mActivity)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
        Log.d(TAG, "Starting setup.")

        // Start setup. This is asynchronous and the specified listener will be called
        // once setup completes.
        // It also starts to report all the new purchases through onPurchasesUpdated() callback.
        startServiceConnection({

            // Notifying the listener that billing client is ready
            mBillingUpdatesListener.onBillingClientSetupFinished()
            // IAB is fully set up. Now, let's get an inventory of stuff we own.
            Log.d(TAG, "Setup successful. Querying inventory.")
        }) { mBillingUpdatesListener.onBillingClientSetupFailed() }
    }

    private fun startServiceConnection(executeOnSuccess: Runnable?, executeOnFail: Runnable?) {
        mBillingClient!!.startConnection(object : BillingClientStateListener {
            @SuppressLint("WrongConstant")
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "Setup finished. Response code: " + billingResult.responseCode)
                //                mBillingClientResponseCode = billingResult.getResponseCode();
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    mIsServiceConnected = true
                    executeOnSuccess?.run()
                } else {
                    // 구글 결제 서버 접속 실패
                    mIsServiceConnected = false
                    executeOnFail?.run()
                }
            }

            override fun onBillingServiceDisconnected() {
                mIsServiceConnected = false
                executeOnFail?.run()
            }
        })
    }

    private fun executeServiceRequest(runnable: Runnable) {
        if (mIsServiceConnected) {
            runnable.run()
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable, null)
        }
    }

    /**
     * Handle a callback that purchases were updated from the Billing library
     */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "onPurchasesUpdated() - user cancelled the purchase flow - skipping")
        } else {
            Log.w(TAG, "onPurchasesUpdated() got unknown resultCode: " + billingResult.responseCode)
        }
        mBillingUpdatesListener.onPurchasesUpdated(billingResult, mPurchases)
    }

    /**
     * Start a purchase flow
     */
    fun initiatePurchaseFlow(productDetails: List<ProductDetails>, productIndex: Int) {
        initiatePurchaseFlow(null, null, productDetails, productIndex)
    }

    /**
     * Start a purchase or subscription replace flow
     *
     * example) oldSku는 1개월 짜리를 1년짜리로 업데이트 한다거나 했을 때 쓰임
     */
    fun initiatePurchaseFlow(
        oldSku: String?,
        purchaseToken: String?,
        productDetails: List<ProductDetails>,
        productIndex: Int
    ) {
        val purchaseFlowRequest = Runnable {
            Log.d(TAG, "Launching in-app purchase flow. Replace old SKU? " + (oldSku != null))

            val productDetailsParamsList = ArrayList<ProductDetailsParams>()
            var offerToken : String?  = null
            if (productDetails[productIndex].subscriptionOfferDetails != null) {
                offerToken = productDetails[productIndex].subscriptionOfferDetails!![0].offerToken
            }

            val productDetailBuilder =
                ProductDetailsParams.newBuilder().setProductDetails(productDetails[productIndex])

            if (!offerToken.isNullOrEmpty()) {
                productDetailBuilder.setOfferToken(offerToken)
            }

            productDetailsParamsList.add(
                productDetailBuilder.build()
            )
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .setObfuscatedAccountId(Util.getSecureId(mActivity))
                .build()
            mBillingClient!!.launchBillingFlow(mActivity, billingFlowParams)
        }
        executeServiceRequest(purchaseFlowRequest)
    }

    val context: Context
        get() = mActivity

    /**
     * Clear the resources
     */
    fun destroy() {
        Log.d(TAG, "Destroying the manager.")
        if (mBillingClient != null && mBillingClient!!.isReady) {
            mBillingClient!!.endConnection()
            mBillingClient = null
        }
    }

    fun queryProductDetailsAsync(
        @BillingClient.ProductType productType: String,
        productIds: List<String>,
        listener: ProductDetailsResponseListener
    ) {
        val productList = productIds.map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(productType)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        executeServiceRequest {
            mBillingClient?.queryProductDetailsAsync(params, listener)
        }
    }

    fun consumeAsync(
        purchaseToken: String,
        purchaseDeveloperPayload: String?,
        purchase: Purchase?
    ) {
        // If we've already scheduled to consume this token - no action is needed (this could happen
        // if you received the token when querying purchases inside onReceive() and later from
        // onActivityResult()
        if (mTokensToBeConsumed == null) {
            mTokensToBeConsumed = HashSet()
        } else if (mTokensToBeConsumed!!.contains(purchaseToken)) {
            Log.i(TAG, "Token was already scheduled to be consumed - skipping...")
            return
        }
        mTokensToBeConsumed!!.add(purchaseToken)

        // Generating Consume Response listener
        val onConsumeListener =
            ConsumeResponseListener { billingResult: BillingResult, purchaseToken1: String? ->
                // If billing service was disconnected, we try to reconnect 1 time
                // (feel free to introduce your retry policy here).
                mBillingUpdatesListener.onConsumeFinished(purchase, billingResult.responseCode)
            }

        // Creating a runnable from the request to use it inside our connection retry policy below
        val consumeRequest = Runnable {

            // Consume the purchase async
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
            mBillingClient!!.consumeAsync(consumeParams, onConsumeListener)
        }
        executeServiceRequest(consumeRequest)
    }

    fun acknowledgePurchase(
        purchaseToken: String?,
        purchaseDeveloperPayload: String?,
        purchase: Purchase
    ) {
        if (!purchase.isAcknowledged) {
            val onAcknowledgeListener =
                AcknowledgePurchaseResponseListener { billingResult: BillingResult ->
                    mBillingUpdatesListener.onAcknowledgeFinished(
                        purchase,
                        billingResult.responseCode
                    )
                }
            val acknowledgeRequest = Runnable {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchaseToken!!)
                    .build()
                mBillingClient!!.acknowledgePurchase(params, onAcknowledgeListener)
            }
            executeServiceRequest(acknowledgeRequest)
        }
    }
    //    public int getBillingClientResponseCode() {
    //        return mBillingClientResponseCode;
    //    }
    /**
     * Handles the purchase
     *
     * Note: Notice that for each purchase, we check if signature is valid on the client.
     * It's recommended to move this check into your backend.
     *
     * @param purchase Purchase to be handled
     */
    private fun handlePurchase(purchase: Purchase) {
        if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
            Log.i(TAG, "Got a purchase: $purchase; but signature is bad. Skipping...")
            return
        }
        Log.d(TAG, "Got a verified purchase: $purchase")
        mPurchases.add(purchase)
    }

    /**
     * Handle a result from querying of purchases and report an updated list to the listener
     */
    private fun onQueryPurchasesFinished(result: BillingResult, list: List<Purchase>) {
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (mBillingClient == null || result.responseCode != BillingResponseCode.OK) {
            Log.w(
                TAG, "Billing client was null or result code (" + result.responseCode
                        + ") was bad - quitting"
            )
            return
        }
        Log.d(TAG, "Query inventory was successful.")

        // Update the UI and purchases inventory with new list of purchases
        mPurchases.clear()
        onPurchasesUpdated(result, list)
    }

    /**
     * Checks if subscriptions are supported for current client
     *
     * Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
     * It is only used in unit tests and after queryPurchases execution, which already has
     * a retry-mechanism implemented.
     *
     */
    private fun areSubscriptionsSupported(): Boolean {
        val responseCode =
            mBillingClient!!.isFeatureSupported(FeatureType.SUBSCRIPTIONS).responseCode
        if (responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "areSubscriptionsSupported() got an error response: $responseCode")
        }
        return responseCode == BillingResponseCode.OK
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    fun queryPurchases() {
        val queryToExecute = Runnable {
            val time = System.currentTimeMillis()
            Log.i(
                TAG, "Querying purchases elapsed time: " + (System.currentTimeMillis() - time)
                        + "ms"
            )

            val purchases = ArrayList<Purchase>()
            mBillingClient!!.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
            ) { billingResult, list ->
                purchases.addAll(list)

                // 구독항목도 조회
                mBillingClient!!.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
                ) { billingResult, list ->
                    purchases.addAll(list)
                    onQueryPurchasesFinished(billingResult, purchases)
                }
            }
        }
        executeServiceRequest(queryToExecute)
    }

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     *
     * Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     *
     */
    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        // Some sanity checks to see if the developer (that's you!) really followed the
        // instructions to run this sample (don't put these checks on your app!)
//        if (BASE_64_ENCODED_PUBLIC_KEY.contains("CONSTRUCT_YOUR")) {
//            throw new RuntimeException("Please update your app's public key at: "
//                    + "BASE_64_ENCODED_PUBLIC_KEY");
//        }
        return try {
            verifyPurchase(mSignatureBase64, signedData, signature)
        } catch (e: Exception) {
            Log.e(TAG, "Got an exception trying to validate a purchase: $e")
            false
        }
    }

    fun isSubscription(purchase: Purchase): Boolean {
        try {
            val json = JSONObject(purchase.originalJson)
            // autoRenewing이 없으면 소비형 상품
            if (json.has("autoRenewing")) {
                return true
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return false
    }

    companion object {
        // Default value of mBillingClientResponseCode until BillingManager was not yeat initialized
        //    private static final int BILLING_MANAGER_NOT_INITIALIZED  = -1;
        private const val TAG = "BillingManager"

        //    public static final String ITEM_TYPE_INAPP = "inapp";
        const val ITEM_TYPE_SUBS = "subs"
    }
}
