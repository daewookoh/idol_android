package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.admanager.AdManager
import net.ib.mn.databinding.ActivityBoardBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.fragment.FreeboardFragment
import net.ib.mn.utils.Const
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.Util
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject

@AndroidEntryPoint
open class BoardActivity : BaseActivity(), BaseDialogFragment.DialogResultHandler, HasFreeboard {
    override var freeboardFragment : FreeboardFragment? = null

    private var category: Int? = null
    private var isPurchasedDailyPack = false
    @Inject
    lateinit var videoAdUtil: VideoAdUtil
    private lateinit var binding: ActivityBoardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBoardBinding.inflate(layoutInflater)
        binding.llContainer.applySystemBarInsets()
        setContentView(binding.root)

        val idolAccount = IdolAccount.getAccount(this)

        val fragmentContainer = binding.fragmentContainer
        // 데일리팩 구독 여부에 따라 광고 세팅
        setPurchasedDailyPackFlag(idolAccount)
        if (!isPurchasedDailyPack && !BuildConfig.CHINA) {
            val adManager = AdManager.getInstance()
            with(adManager) {
                setAdManagerSize(this@BoardActivity, fragmentContainer)
                setAdManager(this@BoardActivity)
                loadAdManager()
            }
        }

        // 지식돌/자게 통합
        freeboardFragment = FreeboardFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, freeboardFragment!!, "free")
        transaction.hide(freeboardFragment!!)
        transaction.commitAllowingStateLoss()

        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.menu_board)

        category = intent.getIntExtra(CommunityActivity.PARAM_CATEGORY, Const.IDOL_ID_FREEBOARD)

        fragmentReplace()
    }


    private fun fragmentReplace() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.show(freeboardFragment!!)
        transaction.commitAllowingStateLoss()
    }

    override fun onResume() {
        if (!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.resume()
        }
        super.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            MEZZO_PLAYER_REQ_CODE -> {
                Util.handleVideoAdResult(this, false, true, requestCode, resultCode, data, "community_videoad") { adType: String? ->
                    videoAdUtil.onVideoSawCommon(this, true, adType, null) }
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        category = intent?.getIntExtra(CommunityActivity.PARAM_CATEGORY, Const.IDOL_ID_FREEBOARD)
        if(category == Const.IDOL_ID_FREEBOARD) {
            fragmentReplace()
            freeboardFragment?.scrollToHeader()
            freeboardFragment?.filterByLatest()
            freeboardFragment?.getArticles(true)
        }
    }

    private fun setPurchasedDailyPackFlag(account: IdolAccount?) {
        isPurchasedDailyPack = false
        account?.userModel?.subscriptions?.forEach { mySubscription ->
            if (mySubscription.familyappId == 1 ||
                mySubscription.familyappId == 2 ||
                mySubscription.skuCode == Const.STORE_ITEM_DAILY_PACK
            ) {
                isPurchasedDailyPack = true
                return@forEach
            }
        }
    }

    override fun onDestroy() {
        if (!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.destroy()
        }
        super.onDestroy()
    }

    override fun onPause() {
        if (!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.pause()
        }
        super.onPause()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.ENTER_WIDE_PHOTO.value -> {
                if (resultCode == RESULT_CANCELED && !isPurchasedDailyPack && !BuildConfig.CHINA) {
                    AdManager.getInstance().loadAdManager()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, BoardActivity::class.java)
        }

        @JvmStatic
        fun createIntent(context: Context, category: Int): Intent {
            val i = Intent(context, BoardActivity::class.java)
            i.putExtra(CommunityActivity.PARAM_CATEGORY, category)
            return i
        }
    }

}
