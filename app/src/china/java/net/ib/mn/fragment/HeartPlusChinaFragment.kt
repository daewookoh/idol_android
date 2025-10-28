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
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.SkuDetails
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import com.paymentwall.pwunifiedsdk.core.PaymentSelectionActivity
import com.paymentwall.pwunifiedsdk.util.ResponseCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.adapter.StoreItemAdapter
import net.ib.mn.adapter.StoreItemAdapter.OnBtnBuyClickListener
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.model.ConfigModel.Companion.getInstance
import net.ib.mn.model.StoreItemModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.decodeAward
import org.json.JSONArray
import org.json.JSONException
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
class HeartPlusChinaFragment : BaseFragment(), OnBtnBuyClickListener {
    private var mStoreItemAdapter: StoreItemAdapter? = null
    private var rvStoreItems: RecyclerView? = null
    private val mStoreItems = ArrayList<StoreItemModel>()

    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var miscRepository: MiscRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    var goods: String? = "H" // 다이아 상점은 "D"

    //할인되지 않은 하트, 다이아 가격.
    var originHeartPrice: Int = 0
    var originDiaPrice: Int = 0

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
    var disClaimerLi: LinearLayoutCompat? = null
    private var disClaimerTv: TextView? = null
    private var bannerUrl: String? = null
    private var tvHeartStoreLimit: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        goods = if (arguments != null) requireArguments().getString("goods") else "H"
        val controller =
            AnimationUtils.loadLayoutAnimation(activity, R.anim.layout_animation_fall_down)

        rvStoreItems = view.findViewById(R.id.rv_store_items)
        rvStoreItems?.setLayoutAnimation(controller)
        //        mRestoreBtn = view.findViewById(R.id.purchase_restore);
//        mRestoreBtn.setOnClickListener(v -> restorePurchase());
        disClaimerLi = view.findViewById(R.id.diamond_store_disclaimer_li)
        disClaimerTv = view.findViewById(R.id.diamond_store_disclaimer_tv)

        stroeView = view.findViewById(R.id.heart_store_list)
        storeLimitView = view.findViewById(R.id.heart_store_limit)
        tvHeartStoreLimit = view.findViewById(R.id.tv_heart_store_limit)

        Util.log("votable is" + getInstance(activity).votable)

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
        setStoreList(baseActivity)
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // cheersiwon@naver.com 문의. onAttach에서 넘어옴.. getActivity()가 null인 경우가 발생하는거 같음.
        val activity = baseActivity
        //        setIabHelper(activity);
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mBroadcastReceiver)
        } catch (e: Exception) {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Util.log("onActivityResult $requestCode,$resultCode,$data")
        
        if (requestCode == PaymentSelectionActivity.REQUEST_CODE) {
            Util.closeProgress()
            when (resultCode) {
                ResponseCode.SUCCESSFUL ->                     // The payment is successful
                    showPurchaseSuccessful()

                ResponseCode.ERROR, ResponseCode.CANCEL, ResponseCode.FAILED ->                     // The payment was failed
                    showPurchaseFailed("" + resultCode)

                else -> {}
            }

            return
        }
    }


    private fun showPurchaseSuccessful() {
        // The payment is successful
        LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(
            Intent(Const.PURCHASE_FINISHED_EVENT)
        )

        accountManager.fetchUserInfo(activity)
        Util.log("onActivityResult::" + PaymentSelectionActivity.REQUEST_CODE)
        Util.closeProgress()

        val msg = getString(R.string.purchased)
        Util.showDefaultIdolDialogWithBtn1(
            activity,
            null,
            msg,
            { v: View? -> Util.closeIdolDialog() },
            true
        )
    }

    private fun showPurchaseFailed(reason: String) {
        BaseActivity.FLAG_CLOSE_DIALOG = false
        val errorMsg = getString(R.string.purchase_incompleted) + " [" + reason + "]"
        Util.closeProgress()
        Util.showDefaultIdolDialogWithBtn1(
            activity,
            null,
            errorMsg,
            { v: View? -> Util.closeIdolDialog() },
            true
        )
    }

    private fun setStoreList(activity: BaseActivity?) {
        if (activity == null) {
            return
        }

        mStoreItemAdapter = StoreItemAdapter(
            activity,
            mGlideRequestManager!!,
            this@HeartPlusChinaFragment,
            mStoreItems,
            bannerUrl
        )

        rvStoreItems!!.setHasFixedSize(true)
        rvStoreItems!!.adapter = mStoreItemAdapter
        getStoreList()
    }

    fun getWelcomePrice() {
        if (getInstance(context).showHeartShop
            && (originHeartPrice == 0 || originDiaPrice == 0)
        ) {  //어워즈일 때 getStoreList Api 계속 불러서 막음.
            getStoreList()
        }
    }

    fun getStoreList() {
        Util.log("================== getStoreList")

        // gaon
        // 결제 후 consume이 완료되는 시점(여기)에 다시 api 응답 갱신
        lifecycleScope.launch {
            miscRepository.getStore(goods,
                { response ->
                    if (response.optBoolean("success")) {
                        Util.closeProgress()

                        mStoreItems.clear()
                        val models = ArrayList<StoreItemModel>()
                        val array: JSONArray
                        val gson = instance

                        try {
                            array = response.getJSONArray("objects")
                            bannerUrl = response.optString("banner_url")
                            mStoreItemAdapter!!.setBannerUrl(bannerUrl)

                            if (!getInstance(context).showHeartShop && array.length() == 0) {
                                stroeView!!.visibility = View.GONE
                                storeLimitView!!.visibility = View.VISIBLE
                                //                            HeartPlusActivity activity=(HeartPlusActivity)getActivity();
                                //                            activity.showTapjoy();
                            } else {
                                stroeView!!.visibility = View.VISIBLE
                                storeLimitView!!.visibility = View.GONE

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
                                            if (model.skuCode.startsWith("welcome")) {
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
                                ) { a: StoreItemModel, b: StoreItemModel -> a.amount - b.amount }

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
                                            if (!model.skuCode.startsWith("welcome")) {
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

                                if (goods == NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP) {
                                    for (model in models) {
                                        if (model.isViewable.equals("Y", ignoreCase = true)) {
                                            if (model.type.equals("Y", ignoreCase = true)) {
                                                var price = model.priceCNY
                                                if (price == 0.0) {
                                                    price = model.priceEn
                                                }
                                                // .0 표시되는거 제거
                                                val x = price - price.toInt()
                                                if (x == 0.0) {
                                                    model.price = "￥" + price.toInt()
                                                } else {
                                                    model.price = "￥$price"
                                                }

                                                model.priceAmountMicros =
                                                    (price * 1000000.0).toLong()
                                                mStoreItems.add(model)
                                                NewHeartPlusActivity.firstItemDiaModel = model
                                            }
                                        }
                                    }
                                } else {
                                    for (i in models.indices) {
                                        if (models[i].isViewable.equals("Y", ignoreCase = true)) {
                                            if (models[i].type.equals("Y", ignoreCase = true)) {
                                                if (goods == "H") {
                                                    NewHeartPlusActivity.firstItemHeartModel =
                                                        models[0]
                                                }

                                                var price = models[i].priceCNY
                                                if (price == 0.0) {
                                                    price = models[i].priceEn
                                                }
                                                // .0 표시되는거 제거
                                                val x = price - price.toInt()
                                                if (x == 0.0) {
                                                    models[i].price = "￥" + price.toInt()
                                                } else {
                                                    models[i].price = "￥$price"
                                                }

                                                models[i].priceAmountMicros =
                                                    (price * 1000000.0).toLong()

                                                if (i == 0) {
                                                    if (!models[i].skuCode.startsWith("welcome")) {
                                                        //웰컴패키지가 아닐때는 첫번째 아이템의 세일가격이 안보여야되니까 true로 바꿔줍니다.
                                                        models[i].isFirstPriceCheck = true
                                                    } else {
                                                        val currentHeartCount = models[i].amount
                                                        val currentExtraBonus =
                                                            models[i].bonusExtraAmount
                                                        try {
                                                            originHeartPrice =
                                                                ((NewHeartPlusActivity.firstItemHeartModel.priceAmountMicros / 1000000.0) * currentHeartCount / NewHeartPlusActivity.firstItemHeartModel.amount).toInt()
                                                            originDiaPrice =
                                                                ((NewHeartPlusActivity.firstItemDiaModel.priceAmountMicros / 1000000.0) * currentExtraBonus / NewHeartPlusActivity.firstItemDiaModel.amount).toInt()

                                                            if (goods == "P") {
                                                                models[i].priorPrice =
                                                                    "￥" + NumberFormat.getNumberInstance(
                                                                        Locale.getDefault()
                                                                    )
                                                                        .format((originHeartPrice + originDiaPrice).toLong())
                                                                models[i].welcomePriorPrice =
                                                                    originHeartPrice + originDiaPrice
                                                            } else {
                                                                models[i].priorPrice =
                                                                    "￥" + NumberFormat.getNumberInstance(
                                                                        Locale.getDefault()
                                                                    )
                                                                        .format(originHeartPrice.toLong())
                                                            }
                                                        } catch (e: NumberFormatException) {
                                                            e.printStackTrace()
                                                        }
                                                        //웰컴패키지 이외 것들은 보여야되니까 false.
                                                        models[i].isFirstPriceCheck = false
                                                    }
                                                } else {
                                                    val currentHeartCount = models[i].amount
                                                    val currentExtraBonus =
                                                        models[i].bonusExtraAmount

                                                    try {
                                                        originHeartPrice =
                                                            ((NewHeartPlusActivity.firstItemHeartModel.priceAmountMicros / 1000000.0) * currentHeartCount / NewHeartPlusActivity.firstItemHeartModel.amount).toInt()
                                                        originDiaPrice =
                                                            ((NewHeartPlusActivity.firstItemDiaModel.priceAmountMicros / 1000000.0) * currentExtraBonus / NewHeartPlusActivity.firstItemDiaModel.amount).toInt()
                                                        if (goods == "P") {
                                                            models[i].priorPrice =
                                                                "￥" + NumberFormat.getNumberInstance(
                                                                    Locale.getDefault()
                                                                )
                                                                    .format((originHeartPrice + originDiaPrice).toLong())
                                                            models[i].welcomePriorPrice =
                                                                originHeartPrice + originDiaPrice
                                                        } else {
                                                            models[i].priorPrice =
                                                                "￥" + NumberFormat.getNumberInstance(
                                                                    Locale.getDefault()
                                                                ).format(originHeartPrice.toLong())
                                                        }
                                                    } catch (e: NumberFormatException) {
                                                        e.printStackTrace()
                                                    }

                                                    //                                                int firstHeartCount = models.get(0).getAmount();
                                                    //                                                int currentHeartCount = models.get(i).getAmount();
                                                    //                                                models.get(i).setPriorPrice("￥"+NumberFormat.getNumberInstance(Locale.getDefault()).format(( (models.get(0).getPriceAmountMicros()/1000000.0) * currentHeartCount) / firstHeartCount));
                                                    models[i].isFirstPriceCheck = false
                                                }

                                                mStoreItems.add(models[i])
                                            }
                                        }
                                    }
                                }


                                mStoreItemAdapter!!.notifyDataSetChanged()
                                if (goods == NewHeartPlusActivity.FRAGMENT_PACKAGE_SHOP) {
                                    //                                mRestoreBtn.setVisibility(View.VISIBLE);
                                    disClaimerLi!!.visibility = View.GONE
                                } else if (goods == NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP) {
                                    //                                mRestoreBtn.setVisibility(View.GONE);
                                    disClaimerLi!!.visibility = View.VISIBLE
                                } else {
                                    //                                mRestoreBtn.setVisibility(View.GONE);
                                    disClaimerLi!!.visibility = View.GONE
                                }
                            }
                        } catch (e: Exception) {
                            v("e messgage ->" + e.message)
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
                })
        }
    }

    // https://developer.android.com/google/play/billing/billing_library_overview?hl=ko
    // 구매에 임의의 문자열 또는 개발자 페이로드를 첨부할 수 있습니다. 하지만 개발자 페이로드는 구매가 확인되거나 소비된 경우에만 첨부할 수 있습니다.
    // 이는 구매 절차를 시작할 때 페이로드를 지정할 수 있는 AIDL의 개발자 페이로드와 다른 요소입니다.
    // -> 그래서 developer payload 검사하는 부분은 삭제...
    /**
     * Verifies the developer payload of a purchase.
     */
    //    boolean verifyDeveloperPayload(Purchase p) {
    //        String payload = p.getDeveloperPayload();
    //        byte[] data = Base64.decode(payload, 0);
    //        final IdolAccount account = IdolAccount
    //                .getAccount(getBaseActivity());
    //        String newPayload = new String(data);
    //        return newPayload.equals(account.getEmail());
    //    }
    override fun onBtnBuyClick(item: StoreItemModel) {
        // gaon
        // 앱 자체에서 하루 제한을 걸자
        if (Const.FEATURE_IAB_RESTRICTED) {
            val purchaseDate = Date(
                Util.getPreferenceLong(context, Const.PREF_PURCHASE_DATE, 0)
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
                val f = SimpleDateFormat("H:mm", Locale.getDefault())
                // order time은 최종 주문시간이니 여기에 3시간 더하자
                val availableDate = Date(orderTime!!.time + 3 * 60 * 60 * 1000)
                val tz = TimeZone.getDefault()
                f.timeZone = tz
                //                String available = f.format(availableDate);
                if (context != null) {
                    val msg = requireContext().resources.getString(
                        R.string.iab_restricted
                    ) // + available;
                    Util.showGaonIabRestrictedDialog(context, msg)
                }
            }
            return
        }

        // 중복구매 못하게
        Util.showProgress(context, false)
        Util.closeProgress(5000)

        // developer payload는 더이상 결제 시도시 전달이 불가능해짐.
        // consume 시점에 developer payload 전달이 가능하지만 onConsumeFinished 에서 이 payload를 가져올 방법이 현재 없어서 무의미함... -_-
//        final IdolAccount account = IdolAccount
//                .getAccount(getBaseActivity());
//        String payload = Base64.encodeToString(account.getEmail().getBytes(), 0);
        var price = item.priceCNY
        var currency = "CNY"
        if (price == 0.0) {
            price = item.priceEn
            currency = "USD"
        }


        if (item.goods_type == NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP) {
            if (!getInstance(context).showHeartShop) {
                v("asdasdasdasd")
                val price02 = price
                val curreny02 = currency
                Util.showDefaultIdolDialogWithRedBtn2(
                    context,
                    null,
                    getString(R.string.popup_award_diamond_alert),
                    getString(R.string.popup_award_diamond_alert),
                    R.string.confirm,
                    R.string.btn_cancel,
                    false, false, false, false,
                    { v: View? ->
                        Util.closeProgress()
                        Util.closeIdolDialog()
                        (requireActivity() as NewHeartPlusActivity).selectPaymentMethod(item)
                    },
                    { v: View? ->
                        Util.closeProgress()
                        Util.closeIdolDialog()
                    })
            } else {
                (requireActivity() as NewHeartPlusActivity).selectPaymentMethod(item)
                Util.closeProgress()

                //                PWManager.getInstance().purchase(getActivity(),
//                        IdolAccount.getAccount(getActivity()).getUserModel().getId(),
//                        IdolAccount.getAccount(getActivity()).getUserModel().getEmail(),
//                        item.getSkuCode(),
//                        item.getName(),
//                        price,
//                        currency,
//                        item.getImageUrl());
            }
        } else {
            (requireActivity() as NewHeartPlusActivity).selectPaymentMethod(item)
            Util.closeProgress()
            //            PWManager.getInstance().purchase(getActivity(),
//                    IdolAccount.getAccount(getActivity()).getUserModel().getId(),
//                    IdolAccount.getAccount(getActivity()).getUserModel().getEmail(),
//                    item.getSkuCode(),
//                    item.getName(),
//                    price,
//                    currency,
//                    item.getImageUrl());
        }
    }


    fun onBtnDetailClick(item: StoreItemModel?) {
//        startActivity(
//                SubscriptionDetailActivity.createIntent(getBaseActivity(), item, mIabHelperKey));
    }

    private fun checkKey(key: String): String {
        val key1 = key.substring(key.length - 7, key.length)
        val data = key.substring(0, key.length - 7)
        val pKey = Util.xor(data.toByteArray(), key1.toByteArray())
        return String(pKey)
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
