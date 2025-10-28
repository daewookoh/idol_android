package net.ib.mn.activity

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType.SUBS
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.billing.util.BillingManager
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ActivitySubscriptionDetailDailyPackBinding
import net.ib.mn.databinding.SubscriptionDetailDescriptionBinding
import net.ib.mn.model.StoreItemModel
import net.ib.mn.model.SubscriptionModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class SubscriptionDetailActivity : BaseActivity() {

    private lateinit var mItem: StoreItemModel
    private lateinit var mSkuDetails: SkuDetails
    //    private lateinit var mHelper: IabHelper
    private var mAccount: IdolAccount? = null
    private var orderTime: Date? = null     // 다음 결제 가능 시간
    private var restricted: Boolean = false // 결제가능 시간이 아님
    private var restrictionMsg: String = "" // 결제 불가 이유
    private var isPurchased: Boolean = false
    private var mBillingManager: BillingManager? = null

    private var descriptionBinding: SubscriptionDetailDescriptionBinding? = null
    private var dailypackBinding: ActivitySubscriptionDetailDailyPackBinding? = null

    private val mutex = Mutex()

    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    // 결제 영수증 검증 후 상품목록 업뎃
    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            finish()
        }
    }

    private fun paymentGoogleSubscription(purchase: Purchase) {
        lifecycleScope.launch {
            usersRepository.paymentsGoogleSubscription(
                receipt = purchase.originalJson,
                signature = purchase.signature,
                itemType = SUBS,
                state = Const.IAB_STATE_NORMAL,
                listener = { response ->
                    when (response.optInt("gcode")) {
                        0, ErrorControl.ERROR_4000, ErrorControl.ERROR_6000 -> {
                            val msg = if (mBillingManager?.isRestoring == true)
                                getString(R.string.restore_purchase_completed)
                            else
                                getString(R.string.purchased)

                            // 첫 한달 무료 팝업 숨기기
                            for( sku in purchase.skus ) {
                                if (sku == Const.STORE_ITEM_DAILY_PACK) {
                                    Util.setPreference(this@SubscriptionDetailActivity,
                                        Const.PREF_SHOW_FREE_TRIAL_DAILY_PACK,
                                        false)
                                }
                            }

                            accountManager.fetchUserInfo(this@SubscriptionDetailActivity)

                            Util.closeProgress()
                            Util.showDefaultIdolDialogWithBtn1(
                                this@SubscriptionDetailActivity, null,
                                msg,
                                {
                                    Util.closeIdolDialog()
                                    finish()
                                },
                                true)
                            LocalBroadcastManager.getInstance(this@SubscriptionDetailActivity!!).sendBroadcast(Intent(Const.PURCHASE_FINISHED_EVENT))
                        }
                        ErrorControl.ERROR_5000 -> {
                            Util.closeProgress()
                            Util.showDefaultIdolDialogWithBtn1(
                                this@SubscriptionDetailActivity, null,
                                getString(R.string.no_purchases),
                                { v -> Util.closeIdolDialog() },
                                true)
                        }
                        else -> {
                            val msg2 = if (mBillingManager?.isRestoring == true)
                                getString(R.string.restore_purchase_failed)
                            else
                                getString(R.string.purchase_incompleted)
                            Util.closeProgress()
                            Util.showDefaultIdolDialogWithBtn1(
                                this@SubscriptionDetailActivity, null,
                                msg2,
                                { v -> Util.closeIdolDialog() },
                                true)
                        }
                    }
                    mBillingManager?.isRestoring = false
                },
                errorListener = { throwable ->
                    val msg = throwable.message
                    var errorMsg = if (mBillingManager?.isRestoring == true)
                        getString(R.string.restore_purchase_failed)
                    else
                        getString(R.string.purchase_incompleted)
                    if( !TextUtils.isEmpty(msg) ) {
                        errorMsg += "\n" + msg
                    }
                    Util.closeProgress()
                    Util.showDefaultIdolDialogWithBtn1(this@SubscriptionDetailActivity, null,
                        errorMsg,
                        { v -> Util.closeIdolDialog() },
                        true)
                    mBillingManager?.isRestoring = false
                }
            )
        }
    }

    private fun paymentGoogleRestore(purchase: Purchase) {
        lifecycleScope.launch {
            usersRepository.paymentsGoogleRestore(
                receipt = purchase.originalJson,
                signature = purchase.signature,
                itemType = SUBS,
                state = Const.IAB_STATE_NORMAL,
                listener = { response ->
                    when (response.optInt("gcode")) {
                        0, ErrorControl.ERROR_4000, ErrorControl.ERROR_6000 -> {
                            val msg = if (mBillingManager?.isRestoring == true)
                                getString(R.string.restore_purchase_completed)
                            else
                                getString(R.string.purchased)

                            // 첫 한달 무료 팝업 숨기기
                            for( sku in purchase.skus ) {
                                if (sku == Const.STORE_ITEM_DAILY_PACK) {
                                    Util.setPreference(this@SubscriptionDetailActivity,
                                        Const.PREF_SHOW_FREE_TRIAL_DAILY_PACK,
                                        false)
                                }
                            }

                            accountManager.fetchUserInfo(this@SubscriptionDetailActivity)

                            Util.closeProgress()
                            Util.showDefaultIdolDialogWithBtn1(
                                this@SubscriptionDetailActivity, null,
                                msg,
                                {
                                    Util.closeIdolDialog()
                                    finish()
                                },
                                true)
                            LocalBroadcastManager.getInstance(this@SubscriptionDetailActivity!!).sendBroadcast(Intent(Const.PURCHASE_FINISHED_EVENT))
                        }
                        ErrorControl.ERROR_5000 -> {
                            Util.closeProgress()
                            Util.showDefaultIdolDialogWithBtn1(
                                this@SubscriptionDetailActivity, null,
                                getString(R.string.no_purchases),
                                { v -> Util.closeIdolDialog() },
                                true)
                        }
                        ErrorControl.ERROR_5010 -> {
                            Util.closeProgress()
                            Util.showDefaultIdolDialogWithBtn1(
                                this@SubscriptionDetailActivity, null,
                                getString(R.string.another_account_daily_purchase),
                                { v -> Util.closeIdolDialog() },
                                true)
                        }
                        ErrorControl.ERROR_5011 -> {
                            Util.closeProgress()
                            Util.showDefaultIdolDialogWithBtn1(
                                this@SubscriptionDetailActivity, null,
                                getString(R.string.already_daily_purchase),
                                { v -> Util.closeIdolDialog() },
                                true)
                        }
                        else -> {
                            val msg2 = if (mBillingManager?.isRestoring == true)
                                getString(R.string.restore_purchase_failed)
                            else
                                getString(R.string.purchase_incompleted)
                            Util.closeProgress()
                            Util.showDefaultIdolDialogWithBtn1(
                                this@SubscriptionDetailActivity, null,
                                msg2,
                                { v -> Util.closeIdolDialog() },
                                true)
                        }
                    }
                    mBillingManager?.isRestoring = false
                },
                errorListener = { throwable ->
                    val msg = throwable.message
                    var errorMsg = if (mBillingManager?.isRestoring == true)
                        getString(R.string.restore_purchase_failed)
                    else
                        getString(R.string.purchase_incompleted)
                    if( !TextUtils.isEmpty(msg) ) {
                        errorMsg += "\n" + msg
                    }
                    Util.closeProgress()
                    Util.showDefaultIdolDialogWithBtn1(this@SubscriptionDetailActivity, null,
                        errorMsg,
                        { v -> Util.closeIdolDialog() },
                        true)
                    mBillingManager?.isRestoring = false
                }
            )
        }
    }

    internal var mBillingUpdatesListener: BillingManager.BillingUpdatesListener = object : BillingManager.BillingUpdatesListener {
        override fun onConsumeFinished(purchase: Purchase?, result: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onAcknowledgeFinished(purchase: Purchase?, result: Int) {
            if (purchase != null) {
                paymentGoogleSubscription(purchase)
            }
        }

        override fun onBillingClientSetupFinished() {
            Util.log("Billing Client connection finished")
        }

        override fun onBillingClientSetupFailed() {
            Util.log("Billing Client connection FAILED")
        }

        override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
            Util.log("Billing Client onPurchasesUpdated")
            CoroutineScope(Dispatchers.Main).launch {
                mutex.withLock {
                    if (mBillingManager == null) {
                        Util.log("onPurchasesUpdated mBillingManager is null")

                        Util.showDefaultIdolDialogWithBtn1(this@SubscriptionDetailActivity, null,
                            getString(R.string.restore_purchase_failed),
                            { Util.closeIdolDialog() },
                            true)
                        return@launch
                    }

                    // We know this is the "gas" sku because it's the only one we
                    // consume,
                    // so we don't check which sku was consumed. If you have more than
                    // one
                    // sku, you probably should check...
                    Util.log("Billing Client onPurchasesUpdated runOnUiThread")

                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                        if (purchases.isEmpty()) {
                            Util.closeProgress()
                            Util.showDefaultIdolDialogWithBtn1(
                                this@SubscriptionDetailActivity,
                                null,
                                getString(R.string.no_purchases),
                                { v: View? -> Util.closeIdolDialog() },
                                true,
                            )
                            mBillingManager?.isRestoring = false
                            return@launch
                        }

                        // 로딩 창이 닫히는 현상이 있어서
//                Util.showProgress(this@SubscriptionDetailActivity)
                        for (purchase in purchases) {
                            if (mBillingManager?.isRestoring == true) {
                                // 구매복원을 하려는 경우는 consume할 필요없고 iap_verify만 호출
                                paymentGoogleRestore(purchase)
                            } else {
                                // 구독상품의 경우는 새로 생긴 acknowledgePurchase()를 사용
                                mBillingManager?.acknowledgePurchase(purchase.purchaseToken, purchase.developerPayload, purchase)
                            }
                        }
                    } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) run {
                        Util.closeProgress()
                        Util.showDefaultIdolDialogWithBtn1(this@SubscriptionDetailActivity,
                            null,
                            getString(R.string.purchase_incompleted),
                            { Util.closeIdolDialog() },
                            true)
                        mBillingManager?.isRestoring = false
                    } else {
                        Util.closeProgress()
                        Util.showDefaultIdolDialogWithBtn1(this@SubscriptionDetailActivity,
                            null,
                            getString(R.string.msg_iab_purchase_error) + "\npurchase code: " + billingResult.responseCode,
                            { Util.closeIdolDialog() },
                            true)
                        mBillingManager?.isRestoring = false
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, IntentFilter(Const.PURCHASE_FINISHED_EVENT))
        mAccount = IdolAccount.getAccount(this)
        mItem = intent?.extras?.getSerializable(PARAM_STORE_ITEM) as StoreItemModel
        setIabHelper()
        setContentView()
        setContent()
        setActionBar()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
        } catch (e: Exception) {
        }

        mBillingManager?.destroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Util.log("onActivityResult($requestCode,$resultCode,$data")

//        if (mHelper == null) return
//        // Pass on the activity result to the helper for handling
//        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
//            // not handled, so handle it ourselves (here's where you'd
//            // perform any handling of activity results not related to in-app
//            // billing...
//            super.onActivityResult(requestCode, resultCode, data)
//
//            Toast.makeText(this, getString(R.string.msg_error_ok) + " Error requestCode : " + requestCode + "  resultCode:" + resultCode, Toast.LENGTH_SHORT).show()
//            finish()
//        } else {
//            Util.log("onActivityResult handled by IABUtil.")
//        }
    }

    private fun restorePurchase() {
        Util.showProgress(this@SubscriptionDetailActivity)
        try {
            mBillingManager?.isRestoring = true
            mBillingManager?.queryPurchases()
//            mHelper.queryInventoryAsync(mGotInventoryListener)
        } catch (e: IllegalStateException) {
            Util.closeProgress()
            Util.showDefaultIdolDialogWithBtn1(this@SubscriptionDetailActivity,
                    null,
                    getString(R.string.restore_purchase_failed),
                    { Util.closeIdolDialog() },
                    true)
        }
    }

    private fun openPlayStoreAccount() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/account/subscriptions?sku=${mItem.skuCode}&package=$packageName")))
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun setIabHelper() {
        mBillingManager = BillingManager(this@SubscriptionDetailActivity,
                intent.extras?.getString(PARAM_IAB_KEY), mBillingUpdatesListener)

    }

    private fun setContentView() {
        val layoutResId = when (mItem.skuCode) {
            Const.STORE_ITEM_DAILY_PACK -> run {
                for (subscription in mAccount?.userModel?.subscriptions ?: arrayListOf<SubscriptionModel>() ) {
                    if (subscription.skuCode == mItem.skuCode) {
                        isPurchased = true
                        return@run R.layout.subscription_detail_description
                    }
                }
                return@run R.layout.activity_subscription_detail_daily_pack
            }
            else -> R.layout.subscription_detail_description
        }

        if (layoutResId == R.layout.subscription_detail_description) {
            // 단독 화면으로 description만 사용할 때만 인셋 적용
            descriptionBinding = DataBindingUtil.setContentView(this, layoutResId)
            descriptionBinding?.scrollviewSubscriptionDetailDescription?.applySystemBarInsets()
        } else {
            // DailyPack 화면: 바깥 스크롤뷰에만 적용, 안쪽은 적용 X
            dailypackBinding = DataBindingUtil.setContentView(this, layoutResId)
            descriptionBinding = dailypackBinding?.subscriptionDetailDescription

            dailypackBinding?.scrollviewSubscriptionDetailDailyPack?.applySystemBarInsets()
        }
    }

    private fun setContent() {
        if (!isPurchased) {
            when (mItem.skuCode) {
                Const.STORE_ITEM_DAILY_PACK -> {
                    dailypackBinding?.apply {
                        tvTitle.text = mItem.name
                        tvPurchaseInfo.text =
                            "${mItem.price}/${getString(R.string.month)}(${getString(R.string.regular_payment)})"
                        tvRewards.apply {
                            text = mItem.description
                            gravity = Gravity.CENTER
                        }

                        tvPromotion.text = String.format(getString(R.string.month_free_trial), 1)
                        tvDescribeMonthlyPayment.text = String.format(getString(R.string.describe_monthly_payment), 1, mItem.price)
                        llPurchase.setOnClickListener {
                            purchaseSubscription(mItem)
                        }
                    }
                }
            }
        }

        // 언어 추가되면 추가해주기
        //한국어일때, 중국어일때 빼고 나머지 언어는  언어코드 앞부분에 _를  붙여 html 을 가지고옴.
        var lang = Util.getSystemLanguage(baseContext)
        lang = when(lang.split("_")[0]){

            "ko" -> ""
            "zh" -> {
                if(lang == "zh_CN"){
                    "_zh_CN"
                }else {
                    "_zh_TW"
                }
            }

//            //아직 번역이 안된 언어들은  모두 en으로 처리 해무.
//            "ar","fa","de","it" -> "_en"

            else -> "_"+lang.split("_")[0]
        }

        descriptionBinding?.apply {
            tvRestorePurchase.setOnClickListener {
                restorePurchase()
            }
            tvManagePurchase.setOnClickListener {
                openPlayStoreAccount()
            }
            wvAgreement.settings.defaultFontSize = 11
            wvAgreement.setBackgroundColor(ContextCompat.getColor(this@SubscriptionDetailActivity, R.color.gray50))
            wvAgreement.loadUrl(ServerUrl.HOST + "/static/subscription_android" + lang + ".html")

            tvTermsAndConditions.setOnClickListener {
                startActivity(AgreementActivity
                    .createIntent(this@SubscriptionDetailActivity, AgreementActivity.TYPE_TERMS_OF_SERVICE))
            }
            tvPrivacyPolicy.setOnClickListener {
                startActivity(AgreementActivity
                    .createIntent(this@SubscriptionDetailActivity, AgreementActivity.TYPE_PRIVACY_POLICY))
            }
        }
    }

    private fun setActionBar() {
        val actionBar = supportActionBar
        actionBar?.title = mItem.name
    }

    private fun purchaseSubscription(item: StoreItemModel) {
        // 중복구매 못하게
        Util.showProgress(baseContext, false)

        mBillingManager?.queryProductDetailsAsync(
            SUBS,
            listOf(item.skuCode)
        ) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = queryResult.productDetailsList
                productDetailsList.forEachIndexed { index, productDetails ->
                    if (productDetails.productId.equals(item.skuCode, ignoreCase = true)) {
                        mBillingManager?.initiatePurchaseFlow(productDetailsList, index)
                    }
                }
            } else {
                Log.e(this.localClassName, "queryProductDetailsAsync failed: ${billingResult.debugMessage}")
            }
        }
    }

    companion object {
        const val PARAM_STORE_ITEM = "paramStoreItem"
        const val PARAM_IAB_KEY = "paramIabKey"
        const val RC_REQUEST = 10001

        @JvmStatic
        fun createIntent(context: Context, item: StoreItemModel, key: String): Intent {
            val args = Bundle()
            args.putSerializable(PARAM_STORE_ITEM, item)
            args.putString(PARAM_IAB_KEY, key)
            return Intent(context, SubscriptionDetailActivity::class.java).putExtras(args)
        }
    }
}
