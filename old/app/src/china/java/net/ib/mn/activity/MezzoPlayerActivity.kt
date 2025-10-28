package net.ib.mn.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdNative.RewardVideoAdListener
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTRewardVideoAd
import com.bytedance.sdk.openadsdk.TTRewardVideoAd.RewardAdInteractionListener
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.databinding.ActivityMezzoVideoBinding
import net.ib.mn.model.ConfigModel.Companion.getInstance
import net.ib.mn.model.VideoAdOrderModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.closeProgress
import net.ib.mn.utils.UtilK.Companion.showLottie
import net.ib.mn.utils.VideoAdManager
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONArray
import org.json.JSONException
import java.util.Collections
import java.util.Random

/**
 * Created by vulpes on 16. 1. 17..
 * 하트충전소 > 비디오 광고보기 30하트.
 */
@SuppressLint("SourceLockedOrientationActivity")
class MezzoPlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityMezzoVideoBinding

    private var bActivate = true

    private val videoAdManager: VideoAdManager? = null

    private var isFront = false // check is activity is front
    var adSlot: AdSlot? = null

    //    private AdVideoPlayer adPlayer;
    private val handler = Handler()

    //    private AdManView movieView;
    //    private RelativeLayout videoArea;
    //    private final int publisher = 1075;
    //    private final int media = 30394;
    //    private final int section = 300884;
    var mTTAdNative: TTAdNative? = null

    //pangle.
    val mttRewardVideoAd: Array<TTRewardVideoAd?> = arrayOfNulls<TTRewardVideoAd>(1)

    //광고 로드 되었나 확인여부.
    private var mIsLoaded = false
    private var adOrderModels: java.util.ArrayList<VideoAdOrderModel>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMezzoVideoBinding.inflate(layoutInflater)
        binding.layoutPlayer.applySystemBarInsets()
        binding.videoArea.applySystemBarInsets()
        binding.videoView.applySystemBarInsets()
        setContentView(binding.root)

        initSetPangle()
        adOrderModels = java.util.ArrayList<VideoAdOrderModel>()
        adOrderModels = this.vdOrder

        //        getWindow().getDecorView().post(() -> {
        val locale = Util.getSystemLanguage(this)

        showLottie(this@MezzoPlayerActivity, true) {
            if (!mIsLoaded) {
                finish()
            }
            null
        }

        playVideoAd(adOrderModels!!, "")
    }

    private fun initSetPangle() {
        val ttAdManager = TTAdSdk.getAdManager()
        mTTAdNative = ttAdManager.createAdNative(this)

        adSlot = AdSlot.Builder()
            .setCodeId("945758366")
            .setRewardName("Reward Ad") //奖励的名称 选填(보상이름,선택사항)
            .setExpressViewAcceptedSize(500f, 500f)
            .setUserID(getAccount(this)!!.email) //유저이메일
            .setMediaExtra("media_extra") //附加参数(전송정보, 선택사항)
            //                .setOrientation(TTAdConstant.VERTICAL) //必填参数，期望视频的播放方向：TTAdConstant.HORIZONTAL 或 TTAdConstant.VERTICAL(재생방향,필수)
            .build()
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
                        Const.MINT, Const.PANGLE, Const.TAP_JOY -> videoAdOrderModel.available =
                            true

                        else -> videoAdOrderModel.available = false
                    }
                    models.add(videoAdOrderModel)
                }

                //광고 우선순위에 맞춰   정렬을  넣어준다.
                //우선순위가 같은경우에는 type을  이용해  랜덤으로  5:5 랜덤으로  철자 정렬을 진행한다.
                Collections.sort<VideoAdOrderModel?>(models, SortingVideoOrder())
            }
            return models
        }

    //서버에서 받아온  광고 순서대로 광고를 진행시킨다.
    //skipAdType을 이용해 실패한 광고는  available을 false처리해준다.
    //전체 광고 데이터의  available이  false인경우에  광고소진 팝업을 띄어준다.
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
                    Const.MINT -> {
                        // mitegral 제거됨
                        videoAdOrderModel.available = false
                        checkAdFailCount++
                    }

                    Const.PANGLE -> if (skipAdType == "pangle") {
                        videoAdOrderModel.available = false
                        checkAdFailCount++
                    } else {
                        //광고 로드.
                        mTTAdNative!!.loadRewardVideoAd(adSlot, pangleListener)
                        break@a
                    }
                }
            } else {
                checkAdFailCount++
                if (checkAdFailCount == videoAdOrderModels.size) { //전체 광고가  실패일경우 광고 소진 팝업 실행
                    isAdLoaded()

                    val data = Intent()
                    data.putExtra(RESULT_CODE, RESULT_CODE_OTHER_ERROR)
                    data.putExtra(AD_TYPE, adType)
                    setResult(RESULT_CANCELED, data)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    finish()
                }
            }
        }
        if (checkAdFailCount == videoAdOrderModels.size) { //처음 시작부터  전체 광고가  실패일경우 광고 소진 팝업 실행
            isAdLoaded()

            val data = Intent()
            data.putExtra(RESULT_CODE, RESULT_CODE_OTHER_ERROR)
            data.putExtra(AD_TYPE, adType)
            setResult(RESULT_CANCELED, data)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mttRewardVideoAd[0] != null) mttRewardVideoAd[0] = null

        // 비디오광고 화면에서 복귀시 로케일이 시스템 로케일로 바뀌는 경우가 있어서
        Util.log("MezzoPlayerAcivity onDestroy")
    }

    override fun onResume() {
        super.onResume()


        isFront = true

        //        videoAdManager.resumeAd();

        // 비디오광고 화면에서 복귀시 로케일이 시스템 로케일로 바뀌는 경우가 있어서
        Util.log("MezzoPlayerAcivity onResume")
    }

    override fun onPause() {
        super.onPause()
        Util.log("MezzoPlayerAcivity onPause")

        isFront = false
        bActivate = false
    }

    override fun onBackPressed() {
        super.onBackPressed()

        //        if (adPlayer != null) {
//            adPlayer.onBackPressed();
//            //Toast.makeText(this, getString(R.string.video_ad_cancelled), Toast.LENGTH_SHORT).show();
//            Intent data = new Intent();
//            data.putExtra(RESULT_CODE, RESULT_CODE_CANCELLED);
//            setResult(RESULT_CANCELED, data);
//            finish();
//        }
//        if (movieView != null) {
//            adPlayer.onBackPressed();
        //Toast.makeText(this, getString(R.string.video_ad_cancelled), Toast.LENGTH_SHORT).show();

//            Intent data = new Intent();
//            data.putExtra(RESULT_CODE, RESULT_CODE_CANCELLED);
//            setResult(RESULT_CANCELED, data);
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//            finish();
//        }
    }

    //    @Override
    //    public void onError(MediaPlayer mp, int what, int extra) {
    //        Util.log("MezzoPlayerActivity onError:"+what + " "+extra);
    //        final int nwhat = what;
    //        handler.post(new Runnable() {
    //            public void run() {
    //                // TODO Auto-generated method stub
    //                setMsg("VideoError=" + Integer.toString(nwhat));
    //            }
    //
    //        });
    //    }
    private fun setMsg(msg: String?) {
        val data = Intent()
        data.putExtra(RESULT_CODE, RESULT_CODE_NETWORK_ERROR)
        setResult(RESULT_CANCELED, data)
        finish() // 이게 왜 없었을까?
    }

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

            override fun onAdRewared() {
                isAdLoaded()

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                setResult(RESULT_CODE_ADMOB)
                //            finish();
            }

            override fun onAdFailedToLoad() {
                Util.closeProgress()
                val data = Intent()
                data.putExtra(RESULT_CODE, RESULT_CODE_OTHER_ERROR)
                data.putExtra(AD_TYPE, adType)
                setResult(RESULT_CANCELED, data)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                finish()
            }

            override fun onAdClosed() {
                finish()
                //            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                // vungle인 경우 화면 방향이 왔다갔다하는 현상이 생겨서 부득이 딜레이 추가
//            videoArea.postDelayed(()-> finish(), 100);
            }
        }

    private fun onVideoAdFailed() {
        Util.closeProgress()
        val data = Intent()
        data.putExtra(RESULT_CODE, RESULT_CODE_OTHER_ERROR)
        data.putExtra(AD_TYPE, adType)
        setResult(RESULT_CANCELED, data)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        finish()
    }

    override fun onPointerCaptureChanged(hasCapture: Boolean) {
    }

    //팽글
    //불리는순서 onRewardViedAdLoad -> onRewardVideoCached.
    private val pangleListener: RewardVideoAdListener = object : RewardVideoAdListener {
        override fun onError(i: Int, message: String?) {
//
            v("pangle onError   -> " + message)

            playVideoAd(adOrderModels!!, Const.PANGLE)
        }

        override fun onRewardVideoAdLoad(ttRewardVideoAd: TTRewardVideoAd?) {
//            Toast.makeText(MezzoPlayerActivity.this, "onRewardVideoAdLoad", Toast.LENGTH_SHORT).show();
            mIsLoaded = false
            mttRewardVideoAd[0] = ttRewardVideoAd

            mttRewardVideoAd[0]!!.setRewardAdInteractionListener(object :
                RewardAdInteractionListener {
                override fun onAdShow() { //광고 나오는 시점
//                    Toast.makeText(MezzoPlayerActivity.this, "rewardVideoAd show", Toast.LENGTH_SHORT).show();
                    v("pangle 광고 실행 ")
                }

                override fun onAdVideoBarClick() {
//                    Toast.makeText(MezzoPlayerActivity.this, "rewardVideoAd bar click", Toast.LENGTH_SHORT).show();
                }

                override fun onAdClose() {
//                    Toast.makeText(MezzoPlayerActivity.this, "rewardVideoAd close", Toast.LENGTH_SHORT).show();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    if (adFail) {
                        v("pangle onAdClose -> adfail")

                        //                        Toast.makeText(getApplicationContext(),"onADClose:"+isCompleteView+",rName:"+RewardName +"，RewardAmout:"+RewardAmout,Toast.LENGTH_SHORT).show();
                        val data = Intent()
                        data.putExtra(RESULT_CODE, RESULT_CODE_CANCELLED)
                        data.putExtra(AD_TYPE, adType)
                        setResult(RESULT_CANCELED, data)
                        finish()
                    } else {
                        v("pangle onAdClose -> adfail  아님 ")

                        //                        Toast.makeText(getApplicationContext(),"onADClose:"+isCompleteView+",rName:"+RewardName +"，RewardAmout:"+RewardAmout,Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CODE_PANGLE)
                        finish()
                    }
                    finish()
                }

                override fun onVideoComplete() {
//                    Toast.makeText(MezzoPlayerActivity.this, "rewardVideoAd complete", Toast.LENGTH_SHORT).show();
                    adFail = false
                    v("pangle onVideoComplete")

                    setResult(RESULT_CODE_PANGLE)
                    finish()
                }

                override fun onVideoError() {
//                    Toast.makeText(MezzoPlayerActivity.this, "rewardVideoAd onVideoError", Toast.LENGTH_SHORT).show();
                    v("pangle onVideoError")

                    playVideoAd(adOrderModels!!, Const.PANGLE)
                }

                override fun onRewardVerify(b: Boolean, i: Int, s: String?, i1: Int, s1: String?) {
                    // 已废弃 请使用 onRewardArrived 替代 -> 더이상 사용되지 않음. onRewardArrived를 대신 사용.
                }

                override fun onRewardArrived(b: Boolean, i: Int, bundle: Bundle?) {
                }


                override fun onSkippedVideo() {
                    v("pangle onSkippedVideo")

                    //                    Toast.makeText(MezzoPlayerActivity.this, "onSkippedVideo", Toast.LENGTH_SHORT).show();
                    adFail = true
                    val data = Intent()
                    data.putExtra(RESULT_CODE, RESULT_CODE_CANCELLED)
                    data.putExtra(AD_TYPE, adType)
                    setResult(RESULT_CANCELED, data)
                    finish()
                }
            })
        }

        override fun onRewardVideoCached() {
//            Toast.makeText(MezzoPlayerActivity.this, "onRewardVideoCached", Toast.LENGTH_SHORT).show();
            mIsLoaded = true

            //광고 보여주기.
            // 광고 동영상의 원활한 재생을 위해 onRewardVideoCached 콜백이로드 된 후 광고를 표시하도록 메인 스레드에서 showRewardVideoAd 메소드를 호출하는 것이 좋습니다. 광고를 표시 한 후 광고 개체를 null로 설정
            if (mttRewardVideoAd[0] != null && mIsLoaded) {
                runOnUiThread(Runnable {
                    mttRewardVideoAd[0]!!.showRewardVideoAd(
                        this@MezzoPlayerActivity,
                        TTAdConstant.RitScenes.CUSTOMIZE_SCENES,
                        "scenes_test"
                    )
                    mttRewardVideoAd[0] = null
                })
            } else {
                Toast.makeText(this@MezzoPlayerActivity, "请先加载广告", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onRewardVideoCached(ttRewardVideoAd: TTRewardVideoAd?) {
            mttRewardVideoAd[0] = ttRewardVideoAd
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

    private fun isAdLoaded() {
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
        private const val USE_ALTERNATIVE_ADS = true // 메조 광고 소진시 애드몹 출력

        const val RESULT_CODE_MAIO: Int = 500

        const val RESULT_CODE_APPLOVIN_MAX: Int = 600
        const val RESULT_CODE_MEZZO: Int = 601

        //mobvista
        const val RESULT_CODE_MOBVISTA: Int = 800
        const val RESULT_CODE_PANGLE: Int = 501
        private var adFail = false //민티그럴 적립 실패시 메조 출력

        //아이언소스
        const val RESULT_CODE_IRONSOURCE: Int = 700

        private const val adType = 0
        const val RESULT_CODE_TAPJOY: Int = 900

        // --- Tapjoy listeners
        const val TAG: String = "Tapjoy"


        @JvmStatic
        fun createIntent(context: Context?, unitId: String?): Intent {
            val i = Intent(context, MezzoPlayerActivity::class.java)
            i.putExtra(EXTRA_UNIT_ID, unitId)
            return i
        }

        var EXTRA_UNIT_ID: String = "unit_id"
    }
}
