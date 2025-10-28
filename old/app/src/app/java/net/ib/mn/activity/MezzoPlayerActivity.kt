package net.ib.mn.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.widget.RelativeLayout
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionListener
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadListener
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest
import com.mmc.man.AdConfig
import com.mmc.man.AdEvent
import com.mmc.man.AdListener
import com.mmc.man.AdResponseCode
import com.mmc.man.data.AdData
import com.mmc.man.view.AdManView
import net.ib.mn.R
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.databinding.ActivityMezzoVideoBinding
import net.ib.mn.model.ConfigModel.Companion.getInstance
import net.ib.mn.model.OnepickTopicModel
import net.ib.mn.model.VideoAdOrderModel
import net.ib.mn.onepick.OnepickMatchActivity
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.closeProgress
import net.ib.mn.utils.UtilK.Companion.getAppName
import net.ib.mn.utils.UtilK.Companion.showLottie
import net.ib.mn.utils.VideoAdManager
import net.ib.mn.utils.VideoAdManager.Companion.getInstance
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONArray
import org.json.JSONException
import java.util.Calendar
import java.util.Collections
import java.util.Random

/**
 * Created by vulpes on 16. 1. 17..
 *
 *
 * 2021 4/14 일 광고 순서
 * 한국어 ->  mezzo-> admob -> applovin
 * 일본어 -> applovin -> admob
 * 그외 외국어 ->  admob -> applovin
 */
class MezzoPlayerActivity : BaseActivity() {
    private var bActivate = true

    private var adData: AdData? = null
    private val playAfterLoading = false
    private var videoAdManager: VideoAdManager? = null

    private var isFront = false // check is activity is front

    private val handler = Handler()

    // AppLovin MAX
    private var rewardedAd: MaxRewardedAd? = null

    //mezzo
    private var movieView: AdManView? = null
    private var videoArea: RelativeLayout? = null

    private var adOrderModels: java.util.ArrayList<VideoAdOrderModel>? = null

    var pagRequest: PAGRewardedRequest = PAGRewardedRequest()
    val pagPlacementId: String = "946017150"

    //광고 로드 되었나 확인여부.
    private var mIsLoaded = false

    private val data = Intent()
    private var unitId: String? = Const.ADMOB_REWARDED_VIDEO_QUIZ_UNIT_ID // 광고 단위 ID

    private lateinit var binding: ActivityMezzoVideoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMezzoVideoBinding.inflate(layoutInflater)
        binding.layoutPlayer.applySystemBarInsets()
        binding.videoArea.applySystemBarInsets()
        binding.videoView.applySystemBarInsets()
        setContentView(binding.root)

        adOrderModels = this.vdOrder

        AdManView.init(this, handler)
        rewardedAd = MaxRewardedAd.getInstance(Const.APPLOVIN_MAX_UNIT_ID, this)

        videoArea = binding.videoArea

        // admob
        videoAdManager = getInstance(this, videoAdListener)

        // intent로 원픽 정보 받아온게 있으면 넘겨준다
        val intent = getIntent()
        if (intent != null) {
            val mTopic =
                intent.getSerializableExtra(OnepickMatchActivity.Companion.PARAM_TOPIC) as OnepickTopicModel?
            if (mTopic != null) {
                val bundle = Bundle()
                bundle.putSerializable(OnepickMatchActivity.Companion.PARAM_TOPIC, mTopic)
                data.putExtras(bundle)
            }

            unitId = intent.getStringExtra(EXTRA_UNIT_ID)
            if (TextUtils.isEmpty(unitId)) {
                unitId = Const.ADMOB_REWARDED_VIDEO_QUIZ_UNIT_ID // EXTRA_UNIT_ID를 빼먹은 경우 기본값 설정
            }
        }

        showLottie(this@MezzoPlayerActivity, true) {
            if (!mIsLoaded) {
                finish()
            }
            null
        }

        playVideoAd(adOrderModels!!, "")
    }

    private val vdOrder: ArrayList<VideoAdOrderModel>
        //configs self 로부터 받아온  광고 순서를
        get() {
            //configself 로부터 받아온  광고 순서

            val adOrder = getInstance(this).videoAd
            val models =
                java.util.ArrayList<VideoAdOrderModel>()

            var array: JSONArray? = null
            try {
                array = JSONArray(adOrder)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            val gson = instance
            if (array != null) {
                for (i in 0..<array.length()) {
                    val videoAdOrderModel = gson.fromJson<VideoAdOrderModel>(
                        array.optJSONObject(i).toString(),
                        VideoAdOrderModel::class.java
                    )

                    //혹시나  사용하지 않는  광고 타입이  섞여 들어올수 잇으므로,
                    //여기서 한번  필터링해서  사용하지 않는 광고는  available  false,  사용 하는 광고는  true 처리를 해준다.
                    when (videoAdOrderModel.type) {
                        Const.PANGLE, Const.MEZZO, Const.ADMOB, Const.APPLOVIN -> videoAdOrderModel.available =
                            true

                        else -> videoAdOrderModel.available = false
                    }
                    models.add(videoAdOrderModel)
                }

                //광고 우선순위에 맞춰   정렬을  넣어준다.
                //우선순위가 같은경우에는 type을  이용해  랜덤으로  5:5 랜덤으로  철자 정렬을 진행한다.
                Collections.sort<VideoAdOrderModel>(models, SortingVideoOrder())
            }
            return models
        }


    //서버에서 받아온  광고 순서대로 광고를 진행시킨다.
    //skipAdType을 이용해 실패한 광고는  available을 false처리해준다.
    //전체 광고 데이터의  available이  false인경우에  광고소진 팝업을 띄어준다.
    @SuppressLint("SourceLockedOrientationActivity")
    private fun playVideoAd(
        videoAdOrderModels: java.util.ArrayList<VideoAdOrderModel>,
        skipAdType: String
    ) {
        //광고 실패  카운트이다.
        //해당 카운트가 햔재 광고 list size와 일치하면,
        //전체 광고가 실패 한것이므로,  광고 소진 팝업을 띄어주고  현재 엑티비티 종료.

        var checkAdFailCount = 0

        a@ for (videoAdOrderModel in videoAdOrderModels) {
            //비광이  Available  true 이고, Type 이   null 값이 아닐때 -> 광고 실행이 가능할

            if (videoAdOrderModel.available && videoAdOrderModel.type != null) {
                when (videoAdOrderModel.type) {
                    Const.MEZZO -> if (skipAdType == "mezzo") {
                        movieView?.setData(adData, null)
                        videoAdOrderModel.available = false
                        checkAdFailCount++
                    } else {
                        adType = 1
                        createAdPlayer()
                        movieView?.request(handler) //메조 광고 실행

                        break@a  //for문 break
                    }

                    Const.ADMOB -> if (skipAdType == "admob") {
                        videoAdManager!!.setListener(null)
                        videoAdOrderModel.available = false
                        checkAdFailCount++
                    } else {
                        tryShowAdMob()
                        break@a
                    }

                    Const.APPLOVIN -> if (skipAdType == "applovin") {
                        rewardedAd!!.setListener(null)
                        videoAdOrderModel.available = false
                        checkAdFailCount++
                    } else {
                        rewardedAd!!.setListener(maxListener)
                        if (rewardedAd!!.isReady()) {
                            rewardedAd!!.showAd()
                        } else {
                            rewardedAd!!.loadAd()
                        }
                        break@a
                    }

                    Const.PANGLE -> if (skipAdType == "pangle") {
                        videoAdOrderModel.available = false
                        checkAdFailCount++
                    } else {
                        //광고 로드.
                        PAGRewardedAd.loadAd(
                            pagPlacementId,
                            pagRequest,
                            object : PAGRewardedAdLoadListener {
                                override fun onError(i: Int, s: String?) {
                                    Util.log(">>> Pangle failed to load ad: " + s)
                                    playVideoAd(adOrderModels!!, Const.PANGLE)
                                }

                                override fun onAdLoaded(pagRewardedAd: PAGRewardedAd?) {
                                    if (pagRewardedAd != null) {
                                        pagRewardedAd.setAdInteractionListener(pagListener)
                                        pagRewardedAd.show(this@MezzoPlayerActivity)
                                    }
                                }
                            })
                        break@a
                    }
                }
            } else {
                checkAdFailCount++
                if (checkAdFailCount == videoAdOrderModels.size) { //전체 광고가  실패일경우 광고 소진 팝업 실행
                    isAdLoaded()
                    data.putExtra(RESULT_CODE, RESULT_CODE_OTHER_ERROR)
                    data.putExtra(AD_TYPE, adType)
                    setResult(RESULT_CANCELED, data)
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    finish()
                }
            }
        }
        if (checkAdFailCount == videoAdOrderModels.size) { //처음 시작부터  전체 광고가  실패일경우 광고 소진 팝업 실행
            isAdLoaded()
            data.putExtra(RESULT_CODE, RESULT_CODE_OTHER_ERROR)
            data.putExtra(AD_TYPE, adType)
            setResult(RESULT_CANCELED, data)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            finish()
        }
    }

    private fun tryShowAdMob() {
        v("tryShowAdmob 실행 ")
        if (videoAdManager!!.isAdReady) {
            videoAdManager!!.showAd(this)
        } else {
            videoAdManager!!.requestAd(this, unitId!!)
        }
    }

    private fun createAdPlayer() {
        val c: Context = this

        adData = AdData()
        var appName = getAppName(this)
        try {
            appName = c.packageManager.getApplicationLabel(
                c.packageManager
                    .getApplicationInfo(c.packageName, PackageManager.GET_UNINSTALLED_PACKAGES)
            ) as String
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val width = Util.convertPixelsToDp(this, Util.getDeviceWidth(this).toFloat())
            .toInt() // MZDisplayUtil.convertPixelsToDp(c, w);
        val height = Util.convertPixelsToDp(this, Util.getDeviceHeight(this).toFloat()).toInt()
        adData!!.major(
            "movie",
            AdConfig.API_MOVIE,
            getResources().getInteger(R.integer.mezzo_publisher),
            getResources().getInteger(R.integer.mezzo_media),
            getResources().getInteger(R.integer.mezzo_section),
            "https://www.myloveidol.com",
            c.packageName,
            appName,
            width,
            height
        )

        //        adData.minor("0", "40", "mezzo", IdolAccount.getAccount(this).getEmail()); //이건 옵션(선택).

        //- 어린이(만 13세 미만) = 0
        //- 청소년 및 성인 (만 13세 이상) = 1
        //- 연령을 알 수 없음 = -1
        adData!!.userAgeLevel = 1
        adData!!.movie(
            AdConfig.USED,
            AdConfig.NOT_USED,
            AdConfig.NOT_USED,
            AdConfig.USED,
            AdConfig.USED,
            AdConfig.USED,
            AdConfig.USED
        )
        adData!!.setIsCloseShow(AdConfig.USED)
        adData!!.isPermission(AdConfig.NOT_USED, AdConfig.NOT_USED)
        movieView = AdManView(c)
        movieView?.setData(adData, object : AdListener {
            override fun onAdSuccessCode(
                v: Any?,
                id: String?,
                type: String?,
                status: String?,
                jsonDataString: String?
            ) {
                Util.log("onAdSuccessCode type=" + type + " status=" + status + " json=" + jsonDataString)
                v("mezzo 광고 ad success code ")

                this@MezzoPlayerActivity.runOnUiThread {
                    videoArea?.let {
                        movieView?.addBannerView(it)
                    } ?: run {
                        Util.log("videoArea is null. Cannot add banner view.")
                    }
                }
                isAdLoaded()
            }

            override fun onAdFailCode(
                v: Any?,
                id: String?,
                type: String?,
                status: String?,
                jsonDataString: String?
            ) {
                v("mezzo 광고 ad fail code ")

                movieView?.onDestroy()
                playVideoAd(adOrderModels!!, Const.MEZZO)
                Util.log("onAdFailCode type=" + type + " status=" + status + " json=" + jsonDataString)
            }

            override fun onAdErrorCode(
                v: Any?,
                id: String?,
                type: String?,
                status: String?,
                failingUrl: String?
            ) {
                movieView?.onDestroy()
                v("mezzo 광고 ad error code ")

                playVideoAd(adOrderModels!!, Const.MEZZO)
                Util.log("onAdErrorCode type=" + type + " status=" + status + " failingUrl=" + failingUrl)
            }

            @SuppressLint("SourceLockedOrientationActivity")
            override fun onAdEvent(
                v: Any?,
                id: String?,
                type: String?,
                status: String?,
                jsonDataString: String?
            ) {
                v("mezzo 광고 실행 ")
                if (AdEvent.Type.CLICK == type) {
                    // 광고링크 클릭
                    movieView?.onDestroy()
                    binding.videoArea.removeAllViews()

                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    setResult(RESULT_CODE_MEZZO, data)
                    finish()
                } else if (AdEvent.Type.CLOSE == type || AdEvent.Type.SKIP == type) {
                    movieView?.onDestroy()
                    binding.videoArea.removeAllViews()

                    data.putExtra(RESULT_CODE, RESULT_CODE_CANCELLED)
                    setResult(RESULT_CANCELED, data)

                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    finish()
                } else if (AdEvent.Type.ENDED == type) {
                    // 끝까지 시청
                    movieView?.onDestroy()
                    binding.videoArea.removeAllViews()

                    setResult(RESULT_CODE_MEZZO, data)
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    finish()
                }
            }

            override fun onPermissionSetting(v: Any?, id: String?) {
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        movieView?.onDestroy()
        rewardedAd?.destroy()

        // 비디오광고 화면에서 복귀시 로케일이 시스템 로케일로 바뀌는 경우가 있어서
        Util.log("MezzoPlayerAcivity onDestroy")
    }

    override fun onResume() {
        super.onResume()
        FLAG_CLOSE_DIALOG = false
        if (!bActivate && adType == 1) {   // vungle 광고 시청 후 여기에 걸려서 RESULT_CANCELED 가 불려 광고 적립이 안됨
            movieView?.onResume()
        }
        isFront = true

        // 비디오광고 화면에서 복귀시 로케일이 시스템 로케일로 바뀌는 경우가 있어서
        Util.log("MezzoPlayerAcivity onResume")
    }

    override fun onPause() {
        FLAG_CLOSE_DIALOG = false
        super.onPause()
        Util.log("MezzoPlayerAcivity onPause")

        isFront = false
        bActivate = false
        movieView?.onPause()
    }

    override fun onStop() {
        FLAG_CLOSE_DIALOG = false
        super.onStop()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (movieView != null) {
            movieView?.onDestroy()
            data.putExtra(RESULT_CODE, RESULT_CODE_CANCELLED)
            setResult(RESULT_CANCELED, data)
            finish()
        }
    }

    private fun setMsg(msg: String?) {
        data.putExtra(RESULT_CODE, RESULT_CODE_NETWORK_ERROR)
        setResult(RESULT_CANCELED, data)
        finish() // 이게 왜 없었을까?
    }

    //애드몹
    private val videoAdListener: VideoAdManager.OnAdManagerListener =
        object : VideoAdManager.OnAdManagerListener {
            override fun onAdPreparing() {
            }

            override fun onAdReady() {
                if (!Const.FEATURE_VIDEO_AD_PRELOAD) {
                    isAdLoaded()
                    // 홈으로 나가거나 다른 앱으로 가면 실행하면 안됨.
                    if (isFront) {
                        videoAdManager!!.showAd(this@MezzoPlayerActivity)
                    }
                }
            }

            @SuppressLint("SourceLockedOrientationActivity")
            override fun onAdRewared() {
                isAdLoaded()
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                setResult(RESULT_CODE_ADMOB, data)
                finish()
            }

            override fun onAdFailedToLoad() { //애드몹 로드  실패시

                v("admob ad fail to load")
                playVideoAd(adOrderModels!!, Const.ADMOB)
            }

            override fun onAdClosed() {
                finish()
            }
        }


    override fun onPointerCaptureChanged(hasCapture: Boolean) {
    }


    // AppLovin MAX
    var maxListener: MaxRewardedAdListener = object : MaxRewardedAdListener {
        override fun onAdLoaded(ad: MaxAd) {
            isAdLoaded()
            v("앱러빈 실행 ")
            rewardedAd!!.showAd()
        }

        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
            v("앱러빈  load fail")
            playVideoAd(adOrderModels!!, Const.APPLOVIN)
        }

        override fun onAdDisplayed(ad: MaxAd) {
        }

        override fun onAdHidden(ad: MaxAd) {
        }

        override fun onAdClicked(ad: MaxAd) {
        }

        override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
            v("앱러빈  display fail")
            playVideoAd(adOrderModels!!, Const.APPLOVIN)
        }

        override fun onUserRewarded(ad: MaxAd, reward: MaxReward) {
            isAdLoaded()
            setResult(RESULT_CODE_APPLOVIN_MAX, data)
            finish()
        }
    }


    //팽글
    private val pagListener: PAGRewardedAdInteractionListener =
        object : PAGRewardedAdInteractionListener {
            override fun onUserEarnedReward(pagRewardItem: PAGRewardItem?) {
            }

            override fun onUserEarnedRewardFail(i: Int, s: String?) {
                v("pangle onUserEarnedRewardFail")
                adFail = true
                data.putExtra(RESULT_CODE, RESULT_CODE_CANCELLED)
                data.putExtra(AD_TYPE, adType)
                setResult(RESULT_CANCELED, data)
                finish()
            }

            override fun onAdShowed() {
            }

            override fun onAdClicked() {
            }

            @SuppressLint("SourceLockedOrientationActivity")
            override fun onAdDismissed() {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                if (adFail) {
                    v("pangle onAdDismissed -> adfail")
                    data.putExtra(RESULT_CODE, RESULT_CODE_CANCELLED)
                    data.putExtra(AD_TYPE, adType)
                    setResult(RESULT_CANCELED, data)
                    finish()
                } else {
                    v("pangle onAdDismissed -> adfail  아님 ")
                    setResult(RESULT_CODE_PANGLE, data)
                    finish()
                }
                finish()
            }
        }

    //광고 타입 우선순위별로 sorting 실행한다.
    internal inner class SortingVideoOrder : Comparator<VideoAdOrderModel> {
        override fun compare(o1: VideoAdOrderModel, o2: VideoAdOrderModel): Int {
            if (o1.order == o2.order) { //우선순위가 같을때는 랜덤으로 type을  사전순 or 사전 반대순으로 적용한다.  5:5 랜덤으로 지정한다.
                val randomSortValue = Random().nextInt(2)
                if (randomSortValue == 0) {
                    return o1.type!!.compareTo(o2.type!!)
                } else {
                    return o2.type!!.compareTo(o1.type!!)
                }
            } else {
                return o1.order!!.compareTo(o2.order!!)
            }
        }
    }

    fun isAdLoaded() {
        mIsLoaded = true
        closeProgress()
    }

    companion object {
        const val RESULT_CODE: String = "result_code"
        const val AD_TYPE: String = "adType"
        const val AD_ERROR_TYPE: String = "adErrorType"
        const val RESULT_CODE_OTHER_ERROR: Int = 100
        const val RESULT_CODE_NETWORK_ERROR: Int = 200
        const val RESULT_CODE_CANCELLED: Int = 300
        const val RESULT_CODE_SHOW_FAIL: Int = 1000

        // admob video ad
        const val RESULT_CODE_ADMOB: Int = 400

        private var adType = 0

        // maio
        const val RESULT_CODE_MAIO: Int = 500

        const val RESULT_CODE_PANGLE: Int = 501
        const val RESULT_CODE_APPLOVIN_MAX: Int = 600
        const val RESULT_CODE_MEZZO: Int = 601

        //아이언소스
        const val RESULT_CODE_IRONSOURCE: Int = 700

        const val RESULT_CODE_MOBVISTA: Int = 800
        const val RESULT_CODE_TAPJOY: Int = 900

        private var adFail = false //민티그럴 적립 실패시 메조 출력

        @JvmStatic
        fun createIntent(context: Context?, unitId: String?): Intent {
            val i = Intent(context, MezzoPlayerActivity::class.java)
            i.putExtra(EXTRA_UNIT_ID, unitId)
            return i
        }

        var EXTRA_UNIT_ID: String = "unit_id"
    }
}
