package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.databinding.ActivityLaboratoryBinding
import net.ib.mn.utils.Const
import net.ib.mn.utils.Toast
import net.ib.mn.utils.VideoAdManager
import net.ib.mn.utils.ext.applySystemBarInsets


class LaboratoryActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityLaboratoryBinding
    private var rewardedAd: MaxRewardedAd? = null
    private var videoAdManager: VideoAdManager? = null
    private val pagPlacementId = "946017150"

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_laboratory)
        binding.llContainer.applySystemBarInsets()

		supportActionBar?.setTitle("실험실")

		binding.rlFacedetect.setOnClickListener(this)
		binding.rlWiki.setOnClickListener(this)
        binding.rlApplovin.setOnClickListener(this)
        binding.rlAdmob.setOnClickListener(this)
        binding.rlPangle.setOnClickListener(this)

        rewardedAd = MaxRewardedAd.getInstance(Const.APPLOVIN_MAX_UNIT_ID, this)
        videoAdManager = VideoAdManager.getInstance(this, videoAdListener)
    }

	override fun onClick(v: View?) {
		when (v?.id){
			binding.rlFacedetect.id -> startActivity(FacedetectActivity.createIntent(this))
			binding.rlWiki.id -> startActivity(WikiActivity.createIntent(this, IdolAccount.getAccount(this)!!.most!!))
            binding.rlApplovin.id -> showAppLovin()
            binding.rlAdmob.id -> showAdMob()
//            binding.rlPangle.id -> showPangle()
		}
	}

    // AppLovin MAX
    val maxListener = object : MaxRewardedAdListener {

        override fun onAdLoaded(ad: MaxAd) {
            rewardedAd?.showAd()
        }

        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
            Toast.makeText(this@LaboratoryActivity, "광고를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }

        override fun onAdDisplayed(ad: MaxAd) {
            // 필요시 구현
        }

        override fun onAdHidden(ad: MaxAd) {
            // 필요시 구현
        }

        override fun onAdClicked(ad: MaxAd) {
            // 필요시 구현
        }

        override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
            Toast.makeText(this@LaboratoryActivity, "광고를 재생하는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }

        override fun onUserRewarded(ad: MaxAd, reward: MaxReward) {
            Toast.makeText(this@LaboratoryActivity, "광고 보상 완료", Toast.LENGTH_SHORT).show()
        }
    }

    fun showAppLovin() {
        rewardedAd?.setListener(maxListener);
        if (rewardedAd?.isReady() == true) {
            rewardedAd?.showAd()
        } else {
            Toast.makeText(this@LaboratoryActivity, "광고 로드 중...", Toast.LENGTH_SHORT).show()
            rewardedAd?.loadAd()
        }
    }

    // 애드몹
    private val videoAdListener = object: VideoAdManager.OnAdManagerListener {
        override fun onAdPreparing() {
            // 필요시 구현
            Toast.makeText(this@LaboratoryActivity, "광고 로드 중...", Toast.LENGTH_SHORT).show()
        }

        override fun onAdReady() {
            videoAdManager?.showAd(this@LaboratoryActivity)
        }

        override fun onAdRewared() {
            Toast.makeText(this@LaboratoryActivity, "광고 보상 완료", Toast.LENGTH_SHORT).show()
        }

        override fun onAdFailedToLoad() {
            Toast.makeText(this@LaboratoryActivity, "광고를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }

        override fun onAdClosed() {
        }
    }

    fun showAdMob() {
        if (videoAdManager?.isAdReady == true) {
            videoAdManager?.showAd(this)
        } else {
            videoAdManager?.requestAd(this, Const.ADMOB_REWARDED_VIDEO_QUIZ_UNIT_ID)
        }
    }

	companion object {
		@JvmStatic
		fun createIntent(context: Context): Intent {
			return Intent(context, LaboratoryActivity::class.java)
		}
	}
}
