package net.ib.mn.fragment

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.app.ActivityCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import com.onestore.iap.api.IapEnum
import com.onestore.iap.api.IapResult
import com.onestore.iap.api.PurchaseClient
import com.onestore.iap.api.PurchaseClient.ConsumeListener
import com.onestore.iap.api.PurchaseClient.PurchaseFlowListener
import com.onestore.iap.api.PurchaseClient.QueryPurchaseListener
import com.onestore.iap.api.PurchaseData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.adapter.StoreItemAdapter
import net.ib.mn.adapter.StoreItemAdapter.OnBtnBuyClickListener
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.billing.util.onestore.AppSecurity
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.model.ConfigModel.Companion.getInstance
import net.ib.mn.model.StoreItemModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.decodeAward
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class HeartPlusFragment2 : BaseFragment(), OnBtnBuyClickListener {
    protected var mStoreItemAdapter: StoreItemAdapter? = null
    private var rvStoreItems: RecyclerView? = null
    private val mStoreItems = ArrayList<StoreItemModel>()
    private var mRestoreBtn: View? = null
    private var mRestoreBtnForAwards: View? = null

    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var miscRepository: MiscRepository

    // (arbitrary) request code for the purchase flow
    //    static final int RC_REQUEST = 10001;
    //    private Fragment mFragment;
    private val askPasswordDialog: Dialog? = null

    private var bannerUrl: String? = null

    var goods: String? = "H" // 다이아 상점은 "D"

    //할인되지 않은 하트, 다이아 가격.
    var originHeartPrice: Int = 0
    var originDiaPrice: Int = 0

    private val subscriptionCheck = false

    private var consume = false

    // 결제 영수증 검증 후 상품목록 업뎃
    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) { //구매했을 경우 상품목록  업데이트를 위해 H,P,D 다 부름.
            getStoreList() //비동기 처리 되어 3개 무작위로 완료 처리 됨. 그래서 restrictionMsg를 분리.
        }
    }

    private var stroeView: NestedScrollView? = null
    private var storeLimitView: LinearLayoutCompat? = null
    var disclaimerLi: LinearLayoutCompat? = null
    private var disClaimerTv: TextView? = null
    private var tvHeartStoreLimit: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        goods = if (arguments != null) requireArguments().getString("goods") else "H"
        val controller =
            AnimationUtils.loadLayoutAnimation(activity, R.anim.layout_animation_fall_down)

        rvStoreItems = view.findViewById(R.id.rv_store_items)
        rvStoreItems?.setLayoutAnimation(controller)
        mRestoreBtn = view.findViewById(R.id.purchase_restore)
        mRestoreBtn?.setOnClickListener(View.OnClickListener { v: View? ->
            consume = true
            loadUnconsumedPurchase()
        })
        mRestoreBtnForAwards = view.findViewById(R.id.purchase_restore_awards)
        mRestoreBtnForAwards?.setOnClickListener(View.OnClickListener { v: View? ->
            consume = true
            loadUnconsumedPurchase()
        })
        disclaimerLi = view.findViewById(R.id.diamond_store_disclaimer_li)
        disClaimerTv = view.findViewById(R.id.diamond_store_disclaimer_tv)

        stroeView = view.findViewById(R.id.heart_store_list)
        storeLimitView = view.findViewById(R.id.heart_store_limit)
        tvHeartStoreLimit = view.findViewById(R.id.tv_heart_store_limit)

        //유의사항안에 다이아 개수 세팅.
        typeList

        if (!getInstance(requireContext()).showHeartShop) {
            val awardJson = Util.getPreference(requireActivity(), Const.AWARD_MODEL)
            val awardData = decodeAward(requireContext(), awardJson)

            val awardsFormat = DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                Locale.KOREA
            ) as SimpleDateFormat

            // 미국 등에서 시작날이 하루 전으로 나와서 한국시간으로 설정
            awardsFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

            val awardBegin = getInstance(requireContext()).awardBegin
            val awardEnd = getInstance(requireContext()).awardEnd

            if (tvHeartStoreLimit != null) {
                tvHeartStoreLimit!!.text = String.format(
                    getString(R.string.award_shop_closed),
                    awardData.awardTitle,
                    awardsFormat.format(awardBegin),
                    awardsFormat.format(awardEnd)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //        mFragment = this;
        mGlideRequestManager = Glide.with(this)

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            mBroadcastReceiver, IntentFilter(Const.PURCHASE_FINISHED_EVENT)
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_heartplus2, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = baseActivity
        setStoreList(activity)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mBroadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Util.log("onActivityResult($requestCode,$resultCode,$data")

        // onestore
        if (requestCode == PURCHASE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if ((activity as NewHeartPlusActivity).mPurchaseClient!!.handlePurchaseData(data) == false) {
                    v("asdasdasd onActivityResult handlePurchaseData false ")
                    // listener is null
                }
            } else {
                v("asdasdasd onActivityResult user canceled")

                // user canceled , do nothing..
            }
        }
    }

    private fun checkPermission(): Boolean {
        // android 6.0 처리
        val context: Context? = baseActivity

        if (ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                NewHeartPlusActivity.REQUEST_PERMISSION_AND_PAY
            )
            Util.closeIdolDialog()
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Util.closeProgress()
        if (requestCode == NewHeartPlusActivity.REQUEST_PERMISSION_AND_PAY) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    if (activity != null && isAdded) {
                        Util.showDefaultIdolDialogWithBtn1(
                            activity,
                            null,
                            resources.getString(R.string.deny_phone_state_permission)
                        ) { v: View? -> Util.closeIdolDialog() }
                    }

                    return
                }
            }

            // 권한 모두 설정했다면 구매 시도
            if (productId != null) {
                // 소비되지 않은 구매내역 확인
                loadUnconsumedPurchase()
                //                sendPaymentRequest(productId);
            }
        }
    }

    private val IAP_API_VERSION = 5
    private val PURCHASE_REQUEST_CODE = 1000 // onActivityResult 로 전달받을 request code

    private val mPurchaseFlowListener: PurchaseFlowListener = object : PurchaseFlowListener {
        override fun onSuccess(purchaseData: PurchaseData) {
            v("asdasdasd launchPurchaseFlowAsync onSuccess, $purchaseData")
            // 구매완료 후 developer payload 검증을 수해한다.
            if (!isValidPayload(purchaseData.developerPayload)) {
                v("asdasdasd launchPurchaseFlowAsync onSuccess, Payload is not valid.")
                return
            }

            // 구매완료 후 signature 검증을 수행한다.
            val validPurchase =
                AppSecurity.verifyPurchase(purchaseData.purchaseData, purchaseData.signature)
            if (validPurchase) {
                if (productId == purchaseData.productId) {
                    run {
                        // 관리형상품(inapp)은 구매 완료 후 소비를 수행한다.
                        consumeItem(purchaseData)
                    }
                } else {
                    Util.log("launchPurchaseFlowAsync onSuccess, Signature is not valid.")
                    return
                }
            }
        }

        override fun onError(result: IapResult) {
            Util.log("launchPurchaseFlowAsync onError, $result")
            Util.closeProgress()
            alert(
                """
                    ${getString(R.string.msg_iab_purchase_error)}
                    ${result.description}
                    Code:${result.code}
                    """.trimIndent()
            )
        }

        override fun onErrorRemoteException() {
            Util.log("launchPurchaseFlowAsync onError, 원스토어 서비스와 연결을 할 수 없습니다")
            Util.closeProgress()
            alert(getString(R.string.onestore_remote_exception))
        }

        override fun onErrorSecurityException() {
            Util.log("launchPurchaseFlowAsync onError, 비정상 앱에서 결제가 요청되었습니다")
            Util.closeProgress()
            alert(getString(R.string.msg_iab_purchase_error))
        }

        override fun onErrorNeedUpdateException() {
            Util.log("launchPurchaseFlowAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다")
            Util.closeProgress()
            updateOrInstallOneStoreService()
        }
    }

    // 관리형상품(inapp)의 구매완료 이후 또는 구매내역조회 이후 소비되지 않는 관리형상품에 대해서 소비를 진행합니다.
    private fun consumeItem(purchaseData: PurchaseData) {
        Util.log("consumeItem() :: getProductId - " + purchaseData.productId + " getPurchaseId -" + purchaseData.purchaseId)

        if (activity == null || ((activity as NewHeartPlusActivity).mPurchaseClient == null)) {
            Util.log("PurchaseClient is not initialized")
            return
        }

        setUiActionFirebaseGoogleAnalyticsFragment(
            GaAction.STORE_IN_APP_PURCHASE_SUCCESS.actionValue,
            GaAction.STORE_IN_APP_PURCHASE_SUCCESS.label
        )

        (activity as? NewHeartPlusActivity)?.mPurchaseClient
            ?.consumeAsync(IAP_API_VERSION, purchaseData, mConsumeListener)
    }

    private fun sendPaymentRequest(productId: String?) {
        if (activity == null) {
            return
        }
        Util.showProgress(activity)


        val productName = "" // "" 일때는 개발자센터에 등록된 상품명 노출
        val productType = IapEnum.ProductType.IN_APP.type // "inapp"
        val devPayload = AppSecurity.generatePayload()
        val gameUserId = getAccount(activity)!!.email // 디폴트 ""
        val promotionApplicable = false


        // 구매 후 dev payload를 검증하기 위하여 프리퍼런스에 임시로 저장합니다.
        savePayloadString(devPayload)

        (activity as? NewHeartPlusActivity)?.mPurchaseClient
            ?.launchPurchaseFlowAsync(
                IAP_API_VERSION,
                activity,
                PURCHASE_REQUEST_CODE,
                productId,
                productName,
                productType,
                devPayload,
                gameUserId,
                promotionApplicable,
                mPurchaseFlowListener
            )
    }

    private fun isValidPayload(payload: String): Boolean {
        val sp = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val savedPayload = sp.getString("payload", "")!!

        Util.log("isValidPayload saved payload ::$savedPayload")
        Util.log("isValidPayload server payload ::$payload")

        return savedPayload == payload
    }

    // 소비되지 않은 구매내역 조회
    private fun loadUnconsumedPurchase() {
        if (activity == null || (activity as NewHeartPlusActivity).mPurchaseClient == null) {
            return
        }

        Util.showProgress(activity)
        (activity as? NewHeartPlusActivity)?.mPurchaseClient
            ?.queryPurchasesAsync(
                IAP_API_VERSION,
                IapEnum.ProductType.IN_APP.type,
                mQueryPurchaseListener
            )
    }

    // 구매내역조회에서 받아온 관리형상품(inapp)의 경우 Signature 검증을 진행하고, 성공할 경우 상품소비를 진행합니다.
    private fun onLoadPurchaseInApp(purchaseDataList: List<PurchaseData>) {
        Util.log("onLoadPurchaseInApp() :: purchaseDataList - $purchaseDataList")
        if (consume) {
            consume = false
            if (purchaseDataList != null && purchaseDataList.size > 0) {
                for (purchase in purchaseDataList) {
                    val result =
                        AppSecurity.verifyPurchase(purchase.purchaseData, purchase.signature)
                    Log.e("test", purchase.toString())

                    if (result) {
                        consumeItem(purchase)
                    }
                }
            } else {
                Util.showDefaultIdolDialogWithBtn1(
                    activity,
                    null,
                    "구매 복원할 항목이 없습니다."
                ) { v: View? -> Util.closeIdolDialog() }
            }
        } else {
            if (purchaseDataList != null && purchaseDataList.size > 0) {
                Util.closeProgress()
                Util.showDefaultIdolDialogWithBtn2(
                    activity,
                    null,
                    getString(R.string.onestore_confirm_unconsumed),
                    { v: View? ->
                        Util.closeIdolDialog()
                        Util.showProgress(activity)
                        for (purchase in purchaseDataList) {
                            val result = AppSecurity.verifyPurchase(
                                purchase.purchaseData,
                                purchase.signature
                            )
                            if (result) {
                                consumeItem(purchase)
                            }
                        }
                    },
                    { v: View? -> Util.closeIdolDialog() })
            } else {
                sendPaymentRequest(productId)
            }
        }
    }

    /*
     * PurchaseClient의 queryPurchasesAsync API (구매내역조회) 콜백 리스너
     */
    var mQueryPurchaseListener: QueryPurchaseListener = object : QueryPurchaseListener {
        override fun onSuccess(purchaseDataList: List<PurchaseData>, productType: String) {
            Util.log("queryPurchasesAsync onSuccess, $purchaseDataList")
            Util.closeProgress()

            if (IapEnum.ProductType.IN_APP.type.equals(productType, ignoreCase = true)) {
                onLoadPurchaseInApp(purchaseDataList)
            }
        }

        override fun onErrorRemoteException() {
            Util.log("queryPurchasesAsync onError, 원스토어 서비스와 연결을 할 수 없습니다")
            Util.closeProgress()
            alert(getString(R.string.onestore_remote_exception))
        }

        override fun onErrorSecurityException() {
            Util.log("queryPurchasesAsync onError, 비정상 앱에서 결제가 요청되었습니다")
            Util.closeProgress()
            alert(getString(R.string.msg_iab_purchase_error))
        }

        override fun onErrorNeedUpdateException() {
            Util.log("queryPurchasesAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다")
            Util.closeProgress()
            updateOrInstallOneStoreService()
        }

        override fun onError(result: IapResult) {
            Util.log("queryPurchasesAsync onError, $result")
            Util.closeProgress()
            alert(
                """
                    ${getString(R.string.msg_iab_purchase_error)}
                    ${result.description}
                    Code:${result.code}
                    """.trimIndent()
            )
        }
    }

    /*
     * PurchaseClient의 consumeAsync API (상품소비) 콜백 리스너
     */
    var mConsumeListener: ConsumeListener = object : ConsumeListener {
        override fun onSuccess(purchaseData: PurchaseData) {
            Util.log("consumeAsync onSuccess, $purchaseData")

            iabVerify(purchaseData)
        }

        override fun onErrorRemoteException() {
            Util.log("consumeAsync onError, 원스토어 서비스와 연결을 할 수 없습니다")
            Util.closeProgress()
            alert(getString(R.string.onestore_remote_exception))
        }

        override fun onErrorSecurityException() {
            Util.log("consumeAsync onError, 비정상 앱에서 결제가 요청되었습니다")
            Util.closeProgress()
            alert(getString(R.string.msg_iab_purchase_error))
        }

        override fun onErrorNeedUpdateException() {
            Util.log("consumeAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다")
            Util.closeProgress()
            updateOrInstallOneStoreService()
        }

        override fun onError(result: IapResult) {
            Util.log("consumeAsync onError, $result")
            Util.closeProgress()
            alert(
                """
                    ${getString(R.string.msg_iab_purchase_error)}
                    ${result.description}
                    Code:${result.code}
                    """.trimIndent()
            )
        }
    }

    // 소비가 완료된 상품을 서버에서 지급 처리
    private fun iabVerify(purchase: PurchaseData) {
        val context: Context? = activity

        val json = JSONObject()
        var receipt = ""
        try {
            json.put("orderId", purchase.orderId)
            json.put("purchaseId", purchase.purchaseId)
            json.put("productId", purchase.productId)
            receipt = json.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val signature = purchase.signature
        // gaon
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        orderTime = Date(Date().time)

        lifecycleScope.launch {
            usersRepository.iabVerify(
                receipt,
                signature,
                "",
                Const.IAB_STATE_NORMAL,
                { response ->
                    Util.closeProgress()
                    if (response.optBoolean("success")) {
                        // 구매 완료
                        requireActivity().setResult(Const.RESULT_CODE_FROM_SHOP)
                        alert(getString(R.string.purchased))
                        getStoreList()
                    } else {
                        val response_msg = ErrorControl.parseError(context, response)
                        alert("$response_msg [5]")
                    }
                }, { throwable ->
                    Util.closeProgress()
                    makeText(context, throwable.message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun updateOrInstallOneStoreService() {
        PurchaseClient.launchUpdateOrInstallFlow(activity)
    }

    private fun savePayloadString(payload: String) {
        val spe = requireActivity().getPreferences(Context.MODE_PRIVATE).edit()
        spe.putString("payload", payload)
        spe.commit()
    }

    private fun alert(str: String) {
        if (activity != null && isAdded) {
            Util.showDefaultIdolDialogWithBtn1(
                activity,
                null,
                str
            ) { v: View? -> Util.closeIdolDialog() }
        }
    }

    private fun setStoreList(activity: BaseActivity?) {
        if (activity == null) {
            return
        }

        mStoreItemAdapter = StoreItemAdapter(
            activity,
            mGlideRequestManager!!,
            this@HeartPlusFragment2,
            mStoreItems,
            bannerUrl
        )

        rvStoreItems!!.setHasFixedSize(true)
        rvStoreItems!!.adapter = mStoreItemAdapter
        getStoreList()
    }

    fun getWelcomePrice() {
        //웰컴패키지 할인가격 초기화 안되어있을때 불러주는 함수(패키지 탭 클릭시에만 불림).
        if (getInstance(context).showHeartShop
            && (originHeartPrice == 0 || originDiaPrice == 0)
        ) {  //어워즈일 때 getStoreList Api 계속 불러서 막음.
            getStoreList()
        }
    }

    private fun getStoreList() {
        Util.log("================== getStoreList")
        val welcomePackPrefix = if (BuildConfig.CELEB) "actor_welcome" else "welcome"

        // gaon
        // 결제 후 consume이 완료되는 시점(여기)에 다시 api 응답 갱신
        lifecycleScope.launch {
            miscRepository.getStore(
                goods ?: "H",
                { response ->
                    if (response.optBoolean("success")) {
                        Util.closeProgress()

                        mStoreItems.clear()
                        val temp = ArrayList<StoreItemModel>()
                        val models = ArrayList<StoreItemModel>()
                        val skus = ArrayList<String>()
                        val array: JSONArray
                        val gson = instance

                        try {
                            array = response.getJSONArray("objects")
                            bannerUrl = response.optString("banner_url")
                            mStoreItemAdapter!!.setBannerUrl(bannerUrl)

                            if (!getInstance(context).showHeartShop && array.length() == 0) {
                                stroeView!!.visibility = View.GONE
                                storeLimitView!!.visibility = View.VISIBLE

                                if (goods == NewHeartPlusActivity.FRAGMENT_PACKAGE_SHOP) {
                                    mRestoreBtnForAwards!!.visibility = View.VISIBLE
                                } else {
                                    mRestoreBtnForAwards!!.visibility = View.GONE
                                }
                            } else {
                                stroeView!!.visibility = View.VISIBLE
                                storeLimitView!!.visibility = View.GONE
                                mRestoreBtnForAwards!!.visibility = View.GONE

                                for (i in 0..<array.length()) {
                                    val obj = array.getJSONObject(i)
                                    val model = gson.fromJson(
                                        obj.toString(),
                                        StoreItemModel::class.java
                                    )
                                    if (model.isViewable.equals("Y", ignoreCase = true)) {
                                        if (model.type.equals("Y", ignoreCase = true)
                                            || model.type.equals("A", ignoreCase = true)
                                        ) {
                                            if (model.skuCode.startsWith(welcomePackPrefix)) {
                                                skus.add(model.skuCode)

                                                if (!model.subscription.equals(
                                                        "Y",
                                                        ignoreCase = true
                                                    )
                                                ) {
                                                    models.add(model)
                                                }
                                            }
                                        }
                                    }
                                }

                                //웰컴 아이템들 정렬해줍니다.
                                Collections.sort(
                                    models
                                ) { a: StoreItemModel?, b: StoreItemModel? -> a!!.amount - b!!.amount }

                                for (i in 0..<array.length()) {
                                    val obj = array.getJSONObject(i)
                                    val model = gson.fromJson(
                                        obj.toString(),
                                        StoreItemModel::class.java
                                    )
                                    if (model.isViewable.equals("Y", ignoreCase = true)) {
                                        if (model.type.equals("Y", ignoreCase = true)
                                            || model.type.equals("A", ignoreCase = true)
                                        ) {
                                            if (!model.skuCode.startsWith(welcomePackPrefix)) {
                                                skus.add(model.skuCode)

                                                if (!model.subscription.equals(
                                                        "Y",
                                                        ignoreCase = true
                                                    )
                                                ) {
                                                    models.add(model)
                                                }
                                            }
                                        }
                                    }
                                }

                                for (i in models.indices) {
                                    if (models[i] != null) {
                                        if (goods == "H") {
                                            NewHeartPlusActivity.firstItemHeartModel = models[0]!!
                                        }

                                        if (goods == "D") {
                                            NewHeartPlusActivity.firstItemDiaModel = models[i]!!
                                        }

                                        //setFirstPriceCheck은 첫번째 아이템인지 확인하기위해(첫번째 아이템은 세일을 안함X)
                                        if (i == 0) {
                                            if (!models[i]!!.skuCode.startsWith(welcomePackPrefix)) {
                                                models[i]!!.isFirstPriceCheck = true
                                            } else {
                                                val currentHeartCount = models[i]!!.amount
                                                val currentExtraBonus = models[i]!!.bonusExtraAmount

                                                try {
                                                    val symbol = "￦" // onestore는 원화 고정
                                                    originHeartPrice =
                                                        ((NewHeartPlusActivity.firstItemHeartModel.priceAmountMicros / 1000000.0) * currentHeartCount / NewHeartPlusActivity.firstItemHeartModel.amount).toInt()
                                                    originDiaPrice =
                                                        ((NewHeartPlusActivity.firstItemDiaModel.priceAmountMicros / 1000000.0) * currentExtraBonus / NewHeartPlusActivity.firstItemDiaModel.amount).toInt()

                                                    if (goods == "P") {
                                                        models[i]!!.priorPrice =
                                                            symbol + NumberFormat.getNumberInstance(
                                                                Locale.getDefault()
                                                            )
                                                                .format((originHeartPrice + originDiaPrice).toLong())
                                                        models[i]!!.welcomePriorPrice =
                                                            originHeartPrice + originDiaPrice
                                                    } else {
                                                        models[i]!!.priorPrice =
                                                            symbol + NumberFormat.getNumberInstance(
                                                                Locale.getDefault()
                                                            ).format(originHeartPrice.toLong())
                                                    }
                                                } catch (e: NumberFormatException) {
                                                    e.printStackTrace()
                                                }

                                                models[i]!!.isFirstPriceCheck = false
                                            }
                                        } else {
                                            val currentHeartCount = models[i]!!.amount
                                            val currentExtraBonus = models[i]!!.bonusExtraAmount

                                            try {
                                                val symbol = "￦" // onestore는 원화 고정
                                                originHeartPrice =
                                                    ((NewHeartPlusActivity.firstItemHeartModel.priceAmountMicros / 1000000.0) * currentHeartCount / NewHeartPlusActivity.firstItemHeartModel.amount).toInt()
                                                originDiaPrice =
                                                    ((NewHeartPlusActivity.firstItemDiaModel.priceAmountMicros / 1000000.0) * currentExtraBonus / NewHeartPlusActivity.firstItemDiaModel.amount).toInt()

                                                if (goods == "P") {
                                                    models[i]!!.priorPrice =
                                                        symbol + NumberFormat.getNumberInstance(
                                                            Locale.getDefault()
                                                        )
                                                            .format((originHeartPrice + originDiaPrice).toLong())
                                                    models[i]!!.welcomePriorPrice =
                                                        originHeartPrice + originDiaPrice
                                                } else {
                                                    models[i]!!.priorPrice =
                                                        symbol + NumberFormat.getNumberInstance(
                                                            Locale.getDefault()
                                                        ).format(originHeartPrice.toLong())
                                                }
                                            } catch (e: NumberFormatException) {
                                                e.printStackTrace()
                                            }

                                            models[i]!!.isFirstPriceCheck = false
                                        }


                                        models[i]!!.priceAmountMicros =
                                            models[i]!!.price.toLong() * 1000000
                                        models[i]!!.price = "￦" + NumberFormat.getNumberInstance(
                                            Locale.getDefault()
                                        ).format(
                                            models[i]!!.price.toDouble()
                                        )
                                        Util.log("HeartPlusFrag::" + models[i]!!.skuDetailsJson)

                                        //                                            temp.add(models.get(i));
                                    }
                                }


                                mStoreItems.addAll(models)
                                mStoreItemAdapter!!.notifyDataSetChanged()
                                if (goods == NewHeartPlusActivity.FRAGMENT_PACKAGE_SHOP) {
                                    mRestoreBtn!!.visibility = View.VISIBLE
                                    disclaimerLi!!.visibility = View.GONE
                                } else if (goods == NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP) {
                                    mRestoreBtn!!.visibility = View.GONE
                                    disclaimerLi!!.visibility = View.VISIBLE
                                } else {
                                    mRestoreBtn!!.visibility = View.GONE
                                    disclaimerLi!!.visibility = View.GONE
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
                                    activity!!.finish()
                                }
                            }
                        }

                        // gaon
                        if (goods == "H") {
                            restrictedHeart = response.optBoolean("restriction")
                            restrictionMsgH = response.optString("message")
                        } else if (goods == "P") {
                            restrictedPack = response.optBoolean("restriction")
                            restrictionMsgP = response.optString("message")
                        } else {
                            restrictedDia = response.optBoolean("restriction")
                            restrictionMsgD = response.optString("message")
                        }

                        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                        try {
                            orderTime = formatter.parse(response.optString("order_time"))
                        } catch (e: ParseException) {
                            e.printStackTrace()
                            orderTime = Date(Date().time)
                        }
                    } else {
                        Util.closeProgress()
                        val responseMsg = ErrorControl.parseError(activity, response)
                        makeText(activity, responseMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                {
                    Util.closeProgress()
                    makeText(
                        activity, R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    override fun onBtnBuyClick(item: StoreItemModel) {
        // gaon
        // 앱 자체에서 하루 제한을 걸자
        val purchaeDate = Date(
            Util.getPreferenceLong(context, Const.PREF_PURCHASE_DATE, 0)
        )
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        if (fmt.format(purchaeDate) == fmt.format(Date())) {
            // 같은 날이면 구매 안되게
            restrictedHeart = true
            restrictedPack = true
            restrictedDia = true
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
                    val msg = requireContext().resources.getString(
                        R.string.iab_restricted
                    )
                    Util.showGaonIabRestrictedDialog(context, msg)
                }
            }
            return
        }

        // 중복구매 못하게
        Util.showProgress(context, false)
        productId = item.skuCode
        // 퍼미션 확인
        if (checkPermission()) {
            if (NewHeartPlusActivity.heartplusValue == NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP) {
                if (!getInstance(context).showHeartShop) {
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
                            loadUnconsumedPurchase()
                            Util.closeIdolDialog()
                        },
                        { v: View? ->
                            Util.closeProgress()
                            Util.closeIdolDialog()
                        })
                } else {
                    loadUnconsumedPurchase()
                }
            } else {
                // 소비되지 않은 구매내역 확인
                loadUnconsumedPurchase()
                //                        sendPaymentRequest(item.getSkuCode());
            }
        }
    }

    fun onBtnDetailClick(item: StoreItemModel?) {
//        startActivity(
//                SubscriptionDetailActivity.createIntent(getBaseActivity(), item, mIabHelperKey));
    }

    private val typeList: Unit
        get() {
            try {
                val gson = instance
                val listType = object :
                    TypeToken<List<SupportAdTypeListModel?>?>() {}.type
                val adList =
                    gson.fromJson<ArrayList<SupportAdTypeListModel>>(
                        Util.getPreference(
                            requireActivity(),
                            Const.AD_TYPE_LIST
                        ), listType
                    )
                val a = adList[0].require.toString()
                disClaimerTv!!.text = getString(R.string.diamond_store_disclaimer).replace("\n", "\n\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    companion object {
        private const val RESPONSE_LINK_COPL_ACCOUNT_1990 = 1990
        private const val RESPONSE_LINK_COPL_ACCOUNT_9000 = 9000

        private var productId: String? = null

        // gaon
        private var orderTime: Date? = null // 다음 결제 가능 시간
        private var restrictedHeart = false // 하트 결제가능 시간이 아님
        private var restrictedPack = false // 패키지 결제가능 시간이 아님
        private var restrictedDia = false // 다이아 결제가능 시간이 아님
        private var restrictionMsgH: String? = null // 하트 결제 불가 이유
        private var restrictionMsgP: String? = null // 패키지 결제 불가 이유
        private var restrictionMsgD: String? = null // 다이아 결제 불가 이유
        private var restrictionMsg: String? = null // 실제 구매하려고 버튼 누른 화면의 메세지
    }
}
