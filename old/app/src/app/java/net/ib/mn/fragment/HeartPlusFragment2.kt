package net.ib.mn.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsResult
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerLib
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.activity.SubscriptionDetailActivity.Companion.createIntent
import net.ib.mn.adapter.StoreItemAdapter
import net.ib.mn.adapter.StoreItemAdapter.OnBtnBuyClickListener
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.billing.util.BillingManager
import net.ib.mn.billing.util.BillingManager.BillingUpdatesListener
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.model.AwardModel
import net.ib.mn.databinding.FragmentHeartplus2Binding
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.StoreItemModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import org.json.JSONArray
import java.text.DateFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

@AndroidEntryPoint
class HeartPlusFragment2 : BaseFragment(), OnBtnBuyClickListener {
    private var mStoreItemAdapter: StoreItemAdapter? = null
    private val mStoreItems = ArrayList<StoreItemModel>()
    private var bannerUrl: String? = null

    //    private static IabHelper mHelper;
    private var mBillingManager: BillingManager? = null
    private var mIabHelperKey: String? = null

    var goods: String? = "H" // 다이아 상점은 "D"

    // 할인되지 않은 하트, 다이아 가격.
    var originHeartPrice = 0
    var originDiaPrice = 0
    private var subscriptionCheck = false
    var productDetails = mutableListOf<ProductDetails>()
    var a = mutableListOf<ProductDetails>()

    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var miscRepository: MiscRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    // 결제 영수증 검증 후 상품목록 업뎃
    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent,
        ) { // 구매했을 경우 상품목록  업데이트를 위해 H,P,D 다 부름.
            getStoreList() // 비동기 처리 되어 3개 무작위로 완료 처리 됨. 그래서 restrictionMsg를 분리.
        }
    }
    private var mBillingUpdatesListener: BillingUpdatesListener? = null

    private var _binding: FragmentHeartplus2Binding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        goods = arguments?.getString("goods") ?: "H"
        val controller =
            AnimationUtils.loadLayoutAnimation(activity, R.anim.layout_animation_fall_down)
        binding.rvStoreItems.layoutAnimation = controller
        binding.purchaseRestore.root.setOnClickListener { restorePurchase() }
        binding.purchaseRestoreAwards.root.setOnClickListener { restorePurchase() }

        // 유의사항안에 다이아 개수 세팅.
        getTypeList()

        if ( !ConfigModel.getInstance(context).showHeartShop ) {
            val awardData = Json{ignoreUnknownKeys = true}.decodeFromString<AwardModel>(Util.getPreference(requireContext(), Const.AWARD_MODEL))


            val awardsFormat = SimpleDateFormat.getDateInstance(
                DateFormat.MEDIUM,
                LocaleUtil.getAppLocale(requireContext()),
            )
            // 미국 등에서 시작날이 하루 전으로 나와서 한국시간으로 설정
            awardsFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

            val awardBegin = ConfigModel.getInstance(context).awardBegin
            val awardEnd = ConfigModel.getInstance(context).awardEnd

            binding.tvHeartStoreLimit.text = String.format(getString(R.string.award_shop_closed),
                awardData.awardTitle,
                awardsFormat.format(awardBegin),
                awardsFormat.format(awardEnd)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //        mFragment = this;
        mGlideRequestManager = Glide.with(this)
        activity?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                mBroadcastReceiver,
                IntentFilter(Const.PURCHASE_FINISHED_EVENT),
            )
        }

        createBillingUpdatesListener()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentHeartplus2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // cheersiwon@naver.com 문의. onAttach에서 넘어옴.. getActivity()가 null인 경우가 발생하는거 같음.
        val activity = baseActivity
        setIabHelper(activity)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            activity?.let {
                LocalBroadcastManager.getInstance(it).unregisterReceiver(mBroadcastReceiver)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (mBillingManager != null) mBillingManager?.destroy()
    }

    private fun restorePurchase() {
        if (activity != null && isAdded) {
            activity?.runOnUiThread {
                Util.showProgressWithText(context, getString(R.string.purchase_loading_title), getString(R.string.purchase_loading_msg), false)
                try {
                    mBillingManager?.isRestoring = true
                    mBillingManager?.queryPurchases()
                    //                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (e: Exception) {
                    if (mBillingManager != null) {
                        mBillingManager?.isRestoring = false
                    }
                    Util.closeProgress()
                    Util.showDefaultIdolDialogWithBtn1(
                        activity,
                        null,
                        getString(R.string.restore_purchase_failed),
                        { v: View? -> Util.closeIdolDialog() },
                        true,
                    )
                }
            }
        }
    }

    private fun paymentGoogleItem(purchase: Purchase?) {
        lifecycleScope.launch {
            usersRepository.paymentsGoogleItem(
                receipt = purchase!!.originalJson,
                signature = purchase.signature,
                itemType = getSkuType(purchase.products),
                state = Const.IAB_STATE_NORMAL,
                listener = { response ->
                    if (activity != null && isAdded) {
                        when (response.optInt("gcode")) {
                            0, ErrorControl.ERROR_4000, ErrorControl.ERROR_6000 -> {
                                val msg = afterRestoring(mBillingManager!!.isRestoring)

                                // 첫 한달 무료 팝업 숨기기
                                for (sku in purchase.skus) {
                                    if (sku == Const.STORE_ITEM_DAILY_PACK) {
                                        Util.setPreference(
                                            activity,
                                            Const.PREF_SHOW_FREE_TRIAL_DAILY_PACK,
                                            false,
                                        )
                                    }
                                }
                                accountManager.fetchUserInfo(baseActivity)
                                Util.closeProgress()
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    msg,
                                    { v: View? -> Util.closeIdolDialog() },
                                    true,
                                )
                                LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(
                                    Intent(Const.PURCHASE_FINISHED_EVENT),
                                )
                                requireActivity().setResult(Const.RESULT_CODE_FROM_SHOP)
                            }

                            ErrorControl.ERROR_5000 -> {
                                Util.closeProgress()
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    getString(R.string.no_purchases),
                                    { _: View? -> Util.closeIdolDialog() },
                                    true,
                                )
                            }

                            else -> {
                                val msg2 =
                                    if (mBillingManager!!.isRestoring) {
                                        getString(R.string.restore_purchase_failed)
                                    } else {
                                        getString(
                                            R.string.purchase_incompleted,
                                        )
                                    }
                                Util.closeProgress()
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    msg2,
                                    { v: View? -> Util.closeIdolDialog() },
                                    true,
                                )
                            }
                        }
                    }
                    mBillingManager!!.isRestoring = false
                },
                errorListener = { throwable ->
                    val msg = throwable.message
                    if (activity != null && isAdded) {
                        var errorMsg =
                            if (mBillingManager!!.isRestoring) {
                                getString(R.string.restore_purchase_failed)
                            } else {
                                getString(
                                    R.string.purchase_incompleted,
                                )
                            }
                        errorMsg += """
                        
                        $msg
                        """.trimIndent()
                        Util.closeProgress()
                        Util.showDefaultIdolDialogWithBtn1(
                            activity,
                            null,
                            errorMsg,
                            { v: View? -> Util.closeIdolDialog() },
                            true,
                        )
                        mBillingManager!!.isRestoring = false
                    }
                }
            )
        }
    }

    private fun paymentGoogleSubscription(purchase: Purchase?) {
        lifecycleScope.launch {
            usersRepository.paymentsGoogleSubscription(
                receipt = purchase!!.originalJson,
                signature = purchase.signature,
                itemType = getSkuType(purchase.products),
                state = Const.IAB_STATE_NORMAL,
                listener = { response ->
                    if (activity != null && isAdded) {
                        when (response.optInt("gcode")) {
                            0, ErrorControl.ERROR_4000, ErrorControl.ERROR_6000 -> {
                                val msg = afterRestoring(mBillingManager?.isRestoring ?: false)

                                // 첫 한달 무료 팝업 숨기기
                                for (sku in purchase.products) {
                                    if (sku == Const.STORE_ITEM_DAILY_PACK) {
                                        Util.setPreference(
                                            activity,
                                            Const.PREF_SHOW_FREE_TRIAL_DAILY_PACK,
                                            false,
                                        )
                                    }
                                }

                                accountManager.fetchUserInfo(baseActivity)
                                Util.closeProgress()
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    msg,
                                    { v: View? -> Util.closeIdolDialog() },
                                    true,
                                )
                                LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(
                                    Intent(Const.PURCHASE_FINISHED_EVENT),
                                )
                                requireActivity().setResult(Const.RESULT_CODE_FROM_SHOP)
                            }

                            ErrorControl.ERROR_5000 -> {
                                Util.closeProgress()
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    getString(R.string.no_purchases),
                                    { v: View? -> Util.closeIdolDialog() },
                                    true,
                                )
                            }

                            else -> {
                                val msg2 =
                                    if (mBillingManager!!.isRestoring) {
                                        getString(R.string.restore_purchase_failed)
                                    } else {
                                        getString(
                                            R.string.purchase_incompleted,
                                        )
                                    }
                                Util.closeProgress()
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    msg2,
                                    { Util.closeIdolDialog() },
                                    true,
                                )
                            }
                        }
                    }
                    mBillingManager!!.isRestoring = false
                },
                errorListener = { throwable ->
                    val msg = throwable.message
                    if (activity != null && isAdded) {
                        var errorMsg =
                            if (mBillingManager!!.isRestoring) {
                                getString(R.string.restore_purchase_failed)
                            } else {
                                getString(
                                    R.string.purchase_incompleted,
                                )
                            }
                        errorMsg += """
                        
                        $msg
                        """.trimIndent()
                        Util.closeProgress()
                        Util.showDefaultIdolDialogWithBtn1(
                            activity,
                            null,
                            errorMsg,
                            { Util.closeIdolDialog() },
                            true,
                        )
                        mBillingManager!!.isRestoring = false
                    }                }
            )
        }
    }

    private fun paymentGoogleRestore(purchase: Purchase) {
        lifecycleScope.launch {
            usersRepository.paymentsGoogleRestore(
                receipt = purchase.originalJson,
                signature = purchase.signature,
                itemType = getSkuType(purchase.products),
                state = Const.IAB_STATE_NORMAL,
                listener = { response ->
                    if (activity != null && isAdded) {
                        when (response.optInt("gcode")) {
                            0, ErrorControl.ERROR_4000, ErrorControl.ERROR_6000 -> {
                                val msg = afterRestoring(mBillingManager!!.isRestoring)

                                // 첫 한달 무료 팝업 숨기기
                                for (sku in purchase.skus) {
                                    if (sku == Const.STORE_ITEM_DAILY_PACK) {
                                        Util.setPreference(
                                            activity,
                                            Const.PREF_SHOW_FREE_TRIAL_DAILY_PACK,
                                            false,
                                        )
                                    }
                                }
                                accountManager.fetchUserInfo(baseActivity)
                                Util.closeProgress()
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    msg,
                                    { v: View? -> Util.closeIdolDialog() },
                                    true,
                                )
                                LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(
                                    Intent(Const.PURCHASE_FINISHED_EVENT),
                                )
                                requireActivity().setResult(Const.RESULT_CODE_FROM_SHOP)
                            }

                            ErrorControl.ERROR_5000 -> {
                                Util.closeProgress()
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    getString(R.string.no_purchases),
                                    { v: View? -> Util.closeIdolDialog() },
                                    true,
                                )
                            }

                            ErrorControl.ERROR_5010 -> {
                                Util.closeProgress()
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    getString(R.string.another_account_daily_purchase),
                                    { v: View? -> Util.closeIdolDialog() },
                                    true,
                                )
                            }

                            ErrorControl.ERROR_5011 -> {
                                Util.closeProgress()
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    getString(R.string.already_daily_purchase),
                                    { v: View? -> Util.closeIdolDialog() },
                                    true,
                                )
                            }

                            else -> {
                                val msg2 =
                                    if (mBillingManager!!.isRestoring) {
                                        getString(R.string.restore_purchase_failed)
                                    } else {
                                        getString(
                                            R.string.purchase_incompleted,
                                        )
                                    }
                                Util.closeProgress()
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    msg2,
                                    { Util.closeIdolDialog() },
                                    true,
                                )
                            }
                        }
                    }
                    mBillingManager!!.isRestoring = false
                },
                errorListener = { throwable ->
                    val msg = throwable.message
                    if (activity != null && isAdded) {
                        var errorMsg =
                            if (mBillingManager!!.isRestoring) {
                                getString(R.string.restore_purchase_failed)
                            } else {
                                getString(
                                    R.string.purchase_incompleted,
                                )
                            }
                        errorMsg += """
                        
                        $msg
                        """.trimIndent()
                        Util.closeProgress()
                        Util.showDefaultIdolDialogWithBtn1(
                            activity,
                            null,
                            errorMsg,
                            { Util.closeIdolDialog() },
                            true,
                        )
                        mBillingManager!!.isRestoring = false
                    }
                }
            )
        }
    }

    private fun createBillingUpdatesListener() {
        mBillingUpdatesListener = object : BillingUpdatesListener {
            override fun onBillingClientSetupFinished() {
                baseActivity?.runOnUiThread {
                    Util.log("Billing Client connection finished")
                    setStoreList(baseActivity)
                }
            }

            override fun onBillingClientSetupFailed() {
                baseActivity?.runOnUiThread {
                    Util.log("Billing Client connection FAILED")
                    if (activity != null && isAdded) {
                        requireActivity().runOnUiThread {
                            binding.heartStoreLimit.visibility = View.VISIBLE
                            binding.tvHeartStoreLimit.setText(R.string.google_store_error)
                        }
                    }
                }
            }

            override fun onConsumeFinished(purchase: Purchase?, result: Int) {
                baseActivity?.runOnUiThread {
                    Util.log(
                        "Consumption finished. Purchase token: " + purchase!!.purchaseToken +
                            ", result: " + result,
                    )

                    // 일반구매 제한을 위해 시간 기록
                    if (activity != null) {
                        Util.setPreference(
                            activity, Const.PREF_PURCHASE_DATE,
                            Date().time,
                        )
                    }
                    // 3번 불려서 result success(0)일 경우에만 호출
                    if (result == 0) {

                        if (!BuildConfig.CHINA) {
                            val appsFlyerInstance = AppsFlyerLib.getInstance()

                            val eventValues = mutableMapOf<String, Any>()
                            eventValues[AFInAppEventParameterName.CUSTOMER_USER_ID] = appsFlyerInstance.getAppsFlyerUID(requireContext()) ?: ""
                            eventValues[AFInAppEventParameterName.ORDER_ID] = purchase.orderId ?: ""
                            val quantity = purchase.quantity
                            val priceAmountMicros = (activity as NewHeartPlusActivity).getPriceAmountMicros()
                            if (priceAmountMicros > 0) {
                                val revenue = priceAmountMicros / 1000000.0
                                eventValues[AFInAppEventParameterName.REVENUE] = revenue * quantity
                            }
                            eventValues[AFInAppEventParameterName.CURRENCY] = (activity as NewHeartPlusActivity).getCurrency()

                            appsFlyerInstance.logEvent(
                                requireContext(),
                                AFInAppEventType.PURCHASE,
                                eventValues
                            )
                        }
                        paymentGoogleItem(purchase)
                    }
                }
            }

            override fun onAcknowledgeFinished(purchase: Purchase?, result: Int) {
                baseActivity?.runOnUiThread {
                    Util.log(
                        "Acknowledge finished. Purchase token: " + purchase!!.purchaseToken +
                            ", result: " + result,
                    )
                    // 패키지를 구매 후 다른 아이템을 구매하면 기존 돌아가는 로직 + 해당 로직을 타서 다른 아이템이 구매가 됐어도 구매에 실패했다는 팝업이 뜨는 현상이 있어, 프레그먼트가 Package이며 구독을 한번 했을 경우 페이지 나갔다 들어오기 전까지 안타게 수정
                    if (result == 0 && goods == "P" && !subscriptionCheck) {
                        paymentGoogleSubscription(purchase)
                        subscriptionCheck = true
                    }
                }
            }

            override fun onPurchasesUpdated(
                billingResult: BillingResult,
                purchases: List<Purchase>?,
            ) {
                if (baseActivity == null) {
                    return
                }
                baseActivity?.runOnUiThread {
                    Util.log("onPurchasesUpdated billingResult=" + billingResult.responseCode)
                    Util.log("billingResult.getDebugMessage()=" + billingResult.debugMessage)
                    if (!isAdded || activity == null) {
                        return@runOnUiThread
                    }
                    if (mBillingManager == null) {
                        Util.log("onPurchasesUpdated mBillingManager is null")
                        Util.closeProgress()
                        Util.showDefaultIdolDialogWithBtn1(
                            activity,
                            null,
                            getString(R.string.restore_purchase_failed) + " [01]",
                            { v: View? -> Util.closeIdolDialog() },
                            true,
                        )
                        return@runOnUiThread
                    }
                    if (billingResult.responseCode == BillingResponseCode.OK &&
                        purchases != null
                    ) {
                        if (purchases.isEmpty()) {
                            showErrorPopup(R.string.no_purchases)
                            return@runOnUiThread
                        }
                        for (purchase in purchases) {
                            val isSubscription = mBillingManager!!.isSubscription(purchase)
                            if (isSubscription && goods == "P") {
                                if (mBillingManager!!.isRestoring) {
                                    // 구매복원을 하려는 경우는 consume할 필요없고 iap_verify만 호출
                                    paymentGoogleRestore(purchase)
                                } else {
                                    // 구독상품의 경우는 새로 생긴 acknowledgePurchase()를 사용
                                    mBillingManager!!.acknowledgePurchase(
                                        purchase.purchaseToken,
                                        purchase.developerPayload, purchase,
                                    )
                                }
                            } else {
                                // purchaseState가 1이 아닌 경우를 처리.
                                // purchase json에는 purchaseState가 0~4까지 있으나 Purchase 클래스 내에서 1 또는 2만 돌려줌
                                // json 내부 purchaseState 0: 결제완료  1: 결제 취소 2: 환불, 4: 결제 대기중("느린 테스트 카드"로 테스트 가능)
                                // Purchase.PurchaseState 1: 결제완료 2: 결제대기
                                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                    // consume. Item already owned를 테스트 해보려면 여기를 막고 하면 된다.
                                    mBillingManager!!.consumeAsync(
                                        purchase.purchaseToken,
                                        purchase.developerPayload, purchase,
                                    )
                                } else {
                                    showErrorPopup(R.string.purchase_pending)
                                }
                            }
                        }
                    } else if (billingResult.responseCode
                        == BillingResponseCode.USER_CANCELED
                    ) {
                        // 구매를 취소한 경우 1
                        showErrorPopup(R.string.purchase_incompleted)
                    } else if (billingResult.responseCode == BillingResponseCode.SERVICE_UNAVAILABLE) {
                        // 네트워크 연결이 끊긴 경우 2
                        showErrorPopup(R.string.purchase_service_unavailable)
                    } else if (billingResult.responseCode == BillingResponseCode.BILLING_UNAVAILABLE) {
                        // 요청한 유형에 Google play 결제 AIDL 버전이 지원되지 않습니다.. 3
                        showErrorPopup(R.string.purchase_billing_unavailable)
                    } else if (billingResult.responseCode == BillingResponseCode.ITEM_UNAVAILABLE) {
                        // 요청한 제품을 구매할 수 없는경우. 4
                        showErrorPopup(R.string.purchase_item_unavailable)
                    } else if (billingResult.responseCode == BillingResponseCode.ERROR) {
                        // API 작업 중 치명적인 오류가 발생했습니다. 6
                        showErrorPopup(R.string.purchase_error)
                    } else if (billingResult.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) {
                        // 항목을 이미 소유하고 있기 때문에 구매할 수 없는경우. 7
                        showErrorPopup(R.string.purchase_item_already_owned)
                    } else if (billingResult.responseCode == BillingResponseCode.ITEM_NOT_OWNED) {
                        // 항목을 소유하고 있지 않기 때문에 사용할 수 없는경우. 8
                        showErrorPopup(R.string.purchase_item_not_owned)
                    } else {
                        showErrorPopup(R.string.msg_iab_purchase_error)
                    }
                }
            }
        }
    }

    private fun setIabHelper(activity: BaseActivity?) {
        lifecycleScope.launch {
            usersRepository.getIabKey(
                { response ->
                    if (response.optBoolean("success")) {
                        val key = response.optString("key")
                        mIabHelperKey = checkKey(key)
                        // compute your public key and store it in base64EncodedPublicKey
                        mBillingManager = BillingManager(
                            requireActivity(), mIabHelperKey,
                            mBillingUpdatesListener!!,
                        )
                    } else {
                        val responseMsg = ErrorControl.parseError(activity, response)
                        makeText(getActivity(), "[1] $responseMsg", Toast.LENGTH_SHORT).show()
                        //                    activity.finish();
                    }
                }, {
                    makeText(
                        activity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            )
        }
    }

    private fun setStoreList(activity: BaseActivity?) {
        if (activity == null) {
            return
        }
        mStoreItemAdapter = StoreItemAdapter(
            activity,
            mGlideRequestManager,
            this@HeartPlusFragment2,
            mStoreItems,
            bannerUrl
        )
        binding.rvStoreItems.setHasFixedSize(true)
        binding.rvStoreItems.adapter = mStoreItemAdapter
        getStoreList()
    }

    fun getWelcomePrice() {
        // 웰컴패키지 할인가격 초기화 안되어있을때 불러주는 함수(패키지 탭 클릭시에만 불림).
        if (ConfigModel.getInstance(context).showHeartShop &&
            (originHeartPrice == 0 || originDiaPrice == 0)
        ) { // 어워즈일 때 getStoreList Api 계속 불러서 막음.
            getStoreList()
        }
    }

    private fun getStoreList() {
        Util.log("================== getStoreList")
        val welcomePackPrefix = if (BuildConfig.CELEB) "actor_welcome" else "welcome"

        // gaon
        // 결제 후 consume이 완료되는 시점(여기)에 다시 api 응답 갱신
        lifecycleScope.launch {
            miscRepository.getStore(goods,
                { response ->
                    if (response.optBoolean("success")) {
                        Util.closeProgress()
                        mStoreItems.clear()
                        val temp = ArrayList<StoreItemModel?>()
                        val models = ArrayList<StoreItemModel?>()
                        val products = ArrayList<String>()
                        val array: JSONArray
                        val gson = instance
                        try {
                            array = response.getJSONArray("objects")
                            bannerUrl = response.optString("banner_url")
                            mStoreItemAdapter?.setBannerUrl(bannerUrl)
                            if (!ConfigModel.getInstance(context).showHeartShop && array.length() == 0) {
                                binding.heartStoreList.visibility = View.GONE
                                binding.heartStoreLimit.visibility = View.VISIBLE
                                if (goods == NewHeartPlusActivity.FRAGMENT_PACKAGE_SHOP) {
                                    binding.purchaseRestoreAwards.root.visibility = View.VISIBLE
                                } else {
                                    binding.purchaseRestoreAwards.root.visibility = View.GONE
                                }
                            } else {
                                binding.heartStoreList.visibility = View.VISIBLE
                                binding.heartStoreLimit.visibility = View.GONE
                                binding.purchaseRestoreAwards.root.visibility = View.GONE
                                productDetails = mutableListOf()

                                // 먼저 웰컴과 관련된 아이템들을 넣어줍니다.
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    val model = gson.fromJson(
                                        obj.toString(),
                                        StoreItemModel::class.java,
                                    )
                                    if (model.isViewable.equals("Y", ignoreCase = true)) {
                                        if (model.type.equals("Y", ignoreCase = true) ||
                                            model.type.equals("A", ignoreCase = true)
                                        ) {
                                            if (model.skuCode.startsWith(welcomePackPrefix)) {
                                                products.add(model.skuCode)
                                                models.add(model)
                                            }
                                        }
                                    }
                                }

                                // 웰컴 아이템들 정렬해줍니다.
                                models.sortWith { a: StoreItemModel?, b: StoreItemModel? -> a!!.amount - b!!.amount }

                                // 웰컴과 관련되지 않을 아이템들 추가.
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    val model = gson.fromJson(
                                        obj.toString(),
                                        StoreItemModel::class.java,
                                    )
                                    if (model.isViewable.equals("Y", ignoreCase = true)) {
                                        if (model.type.equals("Y", ignoreCase = true) ||
                                            model.type.equals("A", ignoreCase = true)
                                        ) {
                                            if (!model.skuCode.startsWith(welcomePackPrefix)) {
                                                products.add(model.skuCode)
                                                models.add(model)
                                            }
                                        }
                                    }
                                }

                                // playstore에서 상품을 가져와서 일치하는 것들만 추가
                                mBillingManager!!.queryProductDetailsAsync(
                                    ProductType.INAPP,
                                    products,
                                ) { _: BillingResult, queryResult: QueryProductDetailsResult ->
                                    val productDetailsList = queryResult.productDetailsList

                                    a.addAll(productDetailsList)
                                    productDetails.addAll(productDetailsList)
                                    if (goods == NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP) {
                                        for (model in models) {
                                            if (model != null &&
                                                model.subscription.equals("N", ignoreCase = true)
                                            ) {
                                                for (productDetails in productDetailsList) {
                                                    if (productDetails.productId.equals(
                                                            model.skuCode,
                                                            ignoreCase = true,
                                                        )
                                                    ) {
                                                        model.currency = productDetails.oneTimePurchaseOfferDetails!!.priceCurrencyCode
                                                        model.price =
                                                            productDetails.oneTimePurchaseOfferDetails!!.formattedPrice // SkuDetails에
                                                        model.priceAmountMicros =
                                                            productDetails.oneTimePurchaseOfferDetails!!
                                                                .priceAmountMicros
                                                        // 통화표시도 포함되어 있음
                                                        model.skuDetailsJson =
                                                            productDetails.description
                                                        temp.add(model)
                                                        NewHeartPlusActivity.firstItemDiaModel =
                                                            model
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        for (i in models.indices) {
                                            if (models[i] != null &&
                                                models[i]!!.subscription.equals(
                                                    "N",
                                                    ignoreCase = true,
                                                )
                                            ) {
                                                for (j in productDetailsList.indices) {
                                                    if (productDetailsList[j].productId.equals(
                                                            models[i]!!.skuCode,
                                                            ignoreCase = true,
                                                        )
                                                    ) {
                                                        models[i]!!.priceAmountMicros =
                                                            productDetailsList[j]
                                                                .oneTimePurchaseOfferDetails!!.priceAmountMicros
                                                        if (goods == "H") {
                                                            NewHeartPlusActivity.firstItemHeartModel =
                                                                models[0]!!
                                                        }

                                                        // setFirstPriceCheck은 첫번째 아이템인지 확인하기위해(첫번째 아이템은 세일을 안함X)
                                                        if (i == 0) {
                                                            if (!models[i]!!.skuCode.startsWith(
                                                                    welcomePackPrefix,
                                                                )
                                                            ) {
                                                                // 웰컴패키지가 아닐때는 첫번째 아이템의 세일가격이 안보여야되니까 true로 바꿔줍니다.
                                                                models[i]!!.isFirstPriceCheck = true
                                                            } else {
                                                                val currentHeartCount = models[i]!!
                                                                    .amount
                                                                val currentExtraBonus = models[i]!!
                                                                    .bonusExtraAmount
                                                                try {
                                                                    val symbol =
                                                                        Currency.getInstance(
                                                                            productDetailsList[j]
                                                                                .oneTimePurchaseOfferDetails!!
                                                                                .priceCurrencyCode,
                                                                        ).symbol
                                                                    originHeartPrice =
                                                                        calculateProportionalPrice(
                                                                            itemAmount = currentHeartCount,
                                                                            basePrice = NewHeartPlusActivity.firstItemHeartModel.priceAmountMicros / 1000000.0,
                                                                            baseAmount = NewHeartPlusActivity.firstItemHeartModel.amount
                                                                        ).toInt()
                                                                    originDiaPrice =
                                                                        calculateProportionalPrice(
                                                                            itemAmount = currentExtraBonus,
                                                                            basePrice = NewHeartPlusActivity.firstItemDiaModel.priceAmountMicros / 1000000.0,
                                                                            baseAmount = NewHeartPlusActivity.firstItemDiaModel.amount
                                                                        ).toInt()
                                                                    if (goods == "P") {
                                                                        models[i]!!.priorPrice =
                                                                            symbol + NumberFormat.getNumberInstance(
                                                                                LocaleUtil.getAppLocale(context ?: return@queryProductDetailsAsync),
                                                                            )
                                                                                .format((originHeartPrice + originDiaPrice).toLong())
                                                                        models[i]!!.welcomePriorPrice =
                                                                            originHeartPrice + originDiaPrice
                                                                    } else {
                                                                        models[i]!!.priorPrice =
                                                                            symbol + NumberFormat.getNumberInstance(
                                                                                LocaleUtil.getAppLocale(context ?: return@queryProductDetailsAsync),
                                                                            ).format(
                                                                                originHeartPrice.toLong(),
                                                                            )
                                                                    }
                                                                } catch (e: NumberFormatException) {
                                                                    e.printStackTrace()
                                                                }
                                                                // 웰컴패키지 이외 것들은 보여야되니까 false.
                                                                models[i]!!.isFirstPriceCheck =
                                                                    false
                                                            }
                                                        } else {
                                                            val currentHeartCount =
                                                                models[i]!!.amount
                                                            val currentExtraBonus =
                                                                models[i]!!.bonusExtraAmount
                                                            try {
                                                                val symbol = Currency.getInstance(
                                                                    productDetailsList[j]
                                                                        .oneTimePurchaseOfferDetails!!
                                                                        .priceCurrencyCode,
                                                                ).symbol
                                                                originHeartPrice =
                                                                    calculateProportionalPrice(
                                                                        itemAmount = currentHeartCount,
                                                                        basePrice = NewHeartPlusActivity.firstItemHeartModel.priceAmountMicros / 1000000.0,
                                                                        baseAmount = NewHeartPlusActivity.firstItemHeartModel.amount
                                                                    ).toInt()
                                                                originDiaPrice =
                                                                    calculateProportionalPrice(
                                                                        itemAmount = currentExtraBonus,
                                                                        basePrice = NewHeartPlusActivity.firstItemDiaModel.priceAmountMicros / 1000000.0,
                                                                        baseAmount = NewHeartPlusActivity.firstItemDiaModel.amount
                                                                    ).toInt()
                                                                if (goods == "P") {
                                                                    models[i]!!.priorPrice =
                                                                        symbol + NumberFormat.getNumberInstance(
                                                                            LocaleUtil.getAppLocale(context ?: return@queryProductDetailsAsync),
                                                                        )
                                                                            .format((originHeartPrice + originDiaPrice).toLong())
                                                                    models[i]!!.welcomePriorPrice =
                                                                        originHeartPrice + originDiaPrice
                                                                } else {
                                                                    models[i]!!.priorPrice =
                                                                        symbol + NumberFormat.getNumberInstance(
                                                                            LocaleUtil.getAppLocale(context ?: return@queryProductDetailsAsync),
                                                                        )
                                                                            .format(originHeartPrice.toLong())
                                                                }
                                                            } catch (e: NumberFormatException) {
                                                                e.printStackTrace()
                                                            }
                                                            models[i]!!.isFirstPriceCheck = false
                                                        }
                                                        models[i]!!.price = productDetailsList[j]
                                                            .oneTimePurchaseOfferDetails!!.formattedPrice // SkuDetails에

                                                        models[i]!!.currency = productDetailsList[j].oneTimePurchaseOfferDetails!!.priceCurrencyCode

                                                        // 통화표시도 포함되어 있음
                                                        models[i]!!.skuDetailsJson =
                                                            productDetailsList[j]!!
                                                                .description
                                                        Util.log("HeartPlusFrag::" + models[i]!!.skuDetailsJson)
                                                        temp.add(models[i])
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    mBillingManager!!.queryProductDetailsAsync(
                                        ProductType.SUBS,
                                        products,
                                    ) { _: BillingResult, queryResult: QueryProductDetailsResult ->
                                        val productDetailsListSubs = queryResult.productDetailsList
                                        a.addAll(productDetailsList)
                                        productDetails.addAll(productDetailsListSubs)
                                        for (model in models) {
                                            if (model != null &&
                                                model.subscription.equals("Y", ignoreCase = true)
                                            ) {
                                                for (productDetails in productDetailsListSubs) {
                                                    // 구독 모델 중, skuCode가 일치하는 것만 리스트에 넣어줌
                                                    if (productDetails.productId.equals(
                                                            model.skuCode,
                                                            ignoreCase = true,
                                                        )
                                                    ) {
                                                        // 2023.9.8 플레이스토어 정기결제 변경으로 무료체험 항목이 같이 와서 이를 구분해서 처리한다
                                                        // 가격이 0인 것을 제외
                                                        val pricingPhases =
                                                            productDetails.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList
                                                        for (pricing in pricingPhases) {
                                                            if (pricing.priceAmountMicros == 0L) {
                                                                continue
                                                            }
                                                            model.price = pricing.formattedPrice
                                                            model.currency = pricing.priceCurrencyCode
                                                            model.priceAmountMicros =
                                                                pricing.priceAmountMicros
                                                            model.skuDetailsJson = productDetails.description
                                                            temp.add(model)
                                                            break
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 중복 제거
                                        val skuCode: MutableSet<String> = HashSet()
                                        for (model in temp) {
                                            if (skuCode.add(model!!.skuCode)) {
                                                mStoreItems.add(model)
                                            }
                                        }
                                        baseActivity?.runOnUiThread {
                                            mStoreItemAdapter!!.notifyDataSetChanged()
                                            when (goods) {
                                                NewHeartPlusActivity.FRAGMENT_PACKAGE_SHOP -> {
                                                    binding.purchaseRestore.root.visibility = View.VISIBLE
                                                    binding.diamondStoreDisclaimerLi.visibility = View.GONE
                                                }
                                                NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP -> {
                                                    binding.purchaseRestore.root.visibility = View.GONE
                                                    binding.diamondStoreDisclaimerLi.visibility = View.VISIBLE
                                                }
                                                else -> {
                                                    binding.purchaseRestore.root.visibility = View.GONE
                                                    binding.diamondStoreDisclaimerLi.visibility = View.GONE
                                                }
                                            }
                                            (activity as? NewHeartPlusActivity)?.onFragmentLoadingComplete()
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (activity != null && isAdded) {
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    getString(R.string.msg_error_ok)
                                ) { v: View? ->
                                    Util.closeIdolDialog()
                                    requireActivity().finish()
                                }
                            }
                            (activity as? NewHeartPlusActivity)?.onFragmentLoadingComplete()
                        }

                        // gaon
                        when (goods) {
                            "H" -> {
                                restrictedHeart = response.optBoolean("restriction")
                                restrictionMsgH = response.optString("message")
                            }
                            "P" -> {
                                restrictedPack = response.optBoolean("restriction")
                                restrictionMsgP = response.optString("message")
                            }
                            else -> {
                                restrictedDia = response.optBoolean("restriction")
                                restrictionMsgD = response.optString("message")
                            }
                        }
                        val formatter =
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(context ?: return@getStore))
                        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                        orderTime = try {
                            formatter.parse(response.optString("order_time"))
                        } catch (e: ParseException) {
                            e.printStackTrace()
                            Date(Date().time)
                        }
                    } else {
                        Util.closeProgress()
                        UtilK.handleCommonError(activity, response)
                        (activity as? NewHeartPlusActivity)?.onFragmentLoadingComplete()
                    }
                },
                {
                    Util.closeProgress()
                    makeText(
                        activity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT,
                    ).show()
                    (activity as? NewHeartPlusActivity)?.onFragmentLoadingComplete()
                })
        }
    }
    // https://developer.android.com/google/play/billing/billing_library_overview?hl=ko
    // 구매에 임의의 문자열 또는 개발자 페이로드를 첨부할 수 있습니다. 하지만 개발자 페이로드는 구매가 확인되거나 소비된 경우에만 첨부할 수 있습니다.
    // 이는 구매 절차를 시작할 때 페이로드를 지정할 수 있는 AIDL의 개발자 페이로드와 다른 요소입니다.
    // -> 그래서 developer payload 검사하는 부분은 삭제...
    /** Verifies the developer payload of a purchase.  */
    //    boolean verifyDeveloperPayload(Purchase p) {
    //        String payload = p.getDeveloperPayload();
    //        byte[] data = Base64.decode(payload, 0);
    //        final IdolAccount account = IdolAccount
    //                .getAccount(getBaseActivity());
    //        String newPayload = new String(data);
    //        return newPayload.equals(account.getEmail());
    //    }
    var currentItem = StoreItemModel()
    override fun onBtnBuyClick(item: StoreItemModel) {
        if (!BuildConfig.CHINA) {
            try {
                (activity as NewHeartPlusActivity).setCurrency(item.currency)
                (activity as NewHeartPlusActivity).setPriceAmountMicros(item.priceAmountMicros)
            } catch (e: Exception) {

            }
        }
        currentItem = item
        // gaon
        // 앱 자체에서 하루 제한을 걸자
        if (Const.FEATURE_IAB_RESTRICTED) {
            val purchaseDate = Date(
                Util.getPreferenceLong(context, Const.PREF_PURCHASE_DATE, 0),
            )
            val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
            if (fmt.format(purchaseDate) == fmt.format(Date())) {
                // 같은 날이면 구매 안되게
                restrictedHeart = true
                restrictedPack = true
                restrictedDia = true
            }
        }
        if (restrictedHeart && restrictedPack && restrictedDia && !item.skuCode.startsWith("daily_pack")) {
            when (item.goods_type) {
                "H" -> restrictionMsg = restrictionMsgH
                "D" -> restrictionMsg = restrictionMsgD
                "P" -> restrictionMsg = restrictionMsgP
            }
            // 서버 메시지가 있으면 서버 메시지 출력
            if (restrictionMsg != null && restrictionMsg!!.length > 0) {
                Util.showGaonIabRestrictedDialog(context, restrictionMsg)
            } else {
                if (context != null) {
                    val msg = context?.resources?.getString(
                        R.string.iab_restricted,
                    )
                    Util.showGaonIabRestrictedDialog(context, msg)
                }
            }
            return
        }

        // 중복구매 못하게
        Util.showProgressWithText(context, getString(R.string.purchase_loading_title), getString(R.string.purchase_loading_msg), false)

        // developer payload는 더이상 결제 시도시 전달이 불가능해짐.
        // consume 시점에 developer payload 전달이 가능하지만 onConsumeFinished 에서 이 payload를 가져올 방법이 현재 없어서 무의미함... -_-
//        final IdolAccount account = IdolAccount
//                .getAccount(getBaseActivity());
//        String payload = Base64.encodeToString(account.getEmail().getBytes(), 0);
        var productIndex = 0
        for (i in productDetails.indices) {
            if (item.skuCode == productDetails[i].productId) {
                productIndex = i
            }
        }
        if (item.goods_type == NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP) {
            if (!ConfigModel.getInstance(context).showHeartShop) {
                val finalProductIndex = productIndex
                Util.showDefaultIdolDialogWithRedBtn2(
                    context,
                    null,
                    getString(R.string.popup_award_diamond_alert),
                    getString(R.string.popup_award_diamond_alert),
                    R.string.confirm,
                    R.string.btn_cancel,
                    false,
                    true,
                    false, false,
                    { v: View? ->
                        Util.closeIdolDialog()
                        mBillingManager!!.initiatePurchaseFlow(
                            productDetails ?: return@showDefaultIdolDialogWithRedBtn2,
                            finalProductIndex,
                        )
                    },
                ) { v: View? ->
                    Util.closeProgress()
                    Util.closeIdolDialog()
                }
            } else {
                mBillingManager!!.initiatePurchaseFlow(productDetails ?: return, productIndex)
            }
        } else {
            Logger.v("jadslfjaldsf :: dflakdsflkdas")
            mBillingManager!!.initiatePurchaseFlow(productDetails ?: return, productIndex)
        }
    }

    fun onBtnDetailClick(item: StoreItemModel?) {
        startActivity(
            createIntent(baseActivity!!, item!!, mIabHelperKey!!),
        )
    }

    private fun checkKey(key: String): String {
        val key1 = key.substring(key.length - 7, key.length)
        val data = key.substring(0, key.length - 7)
        val pKey = Util.xor(data.toByteArray(), key1.toByteArray())
        return String(pKey)
    }

    // sku code로부터 소비성 제품인지 구독상품인지 알기
    @ProductType
    private fun getSkuType(skus: List<String>): String? {
        // billing library 4.0부터 ArrayList로 오는데.. 실제 구매는 한 아이템만 여러개 구매 가능해서 항상 1개짜리 배열?
        for (sku in skus) {
            for (model in mStoreItems) {
                if (model.skuCode.equals(sku, ignoreCase = true)) {
                    return model.type
                }
            }

            // 데일리팩 Null 방지용.
            if (sku == Const.STORE_ITEM_DAILY_PACK) {
                return BillingManager.ITEM_TYPE_SUBS
            }
        }
        return null
    }

    private fun showErrorPopup(errorPhrase: Int) {
        Util.closeProgress()
        Util.showDefaultIdolDialogWithBtn1(
            activity,
            null,
            getString(errorPhrase),
            { v: View? -> Util.closeIdolDialog() },
            true,
        )
        mBillingManager!!.isRestoring = false
    }

    private fun getTypeList() {
        try {
            if (BuildConfig.CELEB) {
                binding.diamondStoreDisclaimerTv.text =
                    getString(R.string.actor_diamond_store_disclaimer).replace("\n", "\n\n")
            } else {
                binding.diamondStoreDisclaimerTv.text =
                    getString(R.string.diamond_store_disclaimer).replace("\n", "\n\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun afterRestoring(isRestored: Boolean): String {
        val msg: String
        if (!isRestored) {
            msg = getString(R.string.purchased)
            setUiActionFirebaseGoogleAnalyticsFragment(
                GaAction.STORE_IN_APP_PURCHASE_SUCCESS.actionValue,
                GaAction.STORE_IN_APP_PURCHASE_SUCCESS.label,
            )
        } else {
            msg = getString(R.string.restore_purchase_completed)
        }
        return msg
    }

    // 원가 계산시 너저분한 가격을 깔끔하게 정리해주는 함수
    // 가격이 3자리 이상이면 세번째자리 이하를 0으로 절삭
    private fun calculateProportionalPrice(
        itemAmount: Int,
        basePrice: Double,
        baseAmount: Int
    ): Double {
        var originalPrice = Math.round(basePrice * itemAmount.toDouble() / baseAmount.toDouble()).toDouble()

        if (originalPrice >= 100) {
            val digits = floor(log10(originalPrice)) + 1
            val divisor = 10.0.pow(digits - 2)
            originalPrice = floor(originalPrice / divisor) * divisor
        }

        return originalPrice
    }

    companion object {
        // gaon
        private var orderTime: Date? = null // 다음 결제 가능 시간

        //
        private var restrictedHeart = false // 하트 결제가능 시간이 아님
        private var restrictedPack = false // 패키지 결제가능 시간이 아님
        private var restrictedDia = false // 다이아 결제가능 시간이 아님
        private var restrictionMsgH: String? = null // 하트 결제 불가 이유
        private var restrictionMsgP: String? = null // 패키지 결제 불가 이유
        private var restrictionMsgD: String? = null // 다이아 결제 불가 이유
        private var restrictionMsg: String? = null // 실제 구매하려고 버튼 누른 화면의 메세지
    }
}