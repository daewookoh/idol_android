package net.ib.mn.awards

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.awards.viewmodel.AwardsMainViewModel
import net.ib.mn.databinding.ActivityIdolAwardsBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.link.LinkUtil
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.TimeZone

/**
 * 애돌용 어워즈 화면
 */

@AndroidEntryPoint
class IdolAwardsActivity : BaseActivity() {
    private lateinit var binding: ActivityIdolAwardsBinding
    private val awardsMainViewModel: AwardsMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        awardsMainViewModel.setAwardData(this)
        supportActionBar?.let {
            it.title = awardsMainViewModel.getAwardData()?.awardTitle
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_idol_awards)
        binding.flAwards.applySystemBarInsets()

        val votable = ConfigModel.getInstance(this).votable
        var frag: BaseFragment? = null
        when {
            "B".equals(votable, ignoreCase = true) ->
                frag = AwardsGuideFragment()

            "Y".equals(votable, ignoreCase = true) ->
                frag = AwardsMainFragment()

            else ->
                frag = AwardsResultFragment()
        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fl_awards, frag)
        transaction.commitAllowingStateLoss()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.btn_share -> {
                shareAwards()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * [%s]\n🚀 최애돌에서 곧 투표가 시작돼요! 🏆\n최애가 1위에 오를 수 있도록 응원 준비 완료!\n⏰ 투표 시작 : %s\n\n#최애돌 #choeaedol
     * [%s]\n🔥 최애돌에서 불꽃 튀는 투표 진행 중 🗳️\n지금 하트로 최애를 응원해 주세요! 💖\n\n#최애돌 #choeaedol
     * [%s]\n🏆 영광의 1위 주인공은 누구?! 🫣\n지금 최애돌에서 확인해보세요!\n\n#최애돌 #choeaedol
     *
     */
    private fun shareAwards() {
        val awardsData = awardsMainViewModel.getAwardData()
        var shareMsg = ""
        val keyword = awardsData?.keyword ?: ""
        val params = arrayListOf("awards")
        if(keyword.isNotEmpty()) params.add(keyword)
        val votable = ConfigModel.getInstance(this).votable
        val link = LinkUtil.getAppLinkUrl(this, params.toList(), null)
        when(votable) {
            "B" -> {
                val awardsFormat = SimpleDateFormat.getDateInstance(
                    DateFormat.MEDIUM,
                    LocaleUtil.getAppLocale(this),
                )
                awardsFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                val format = if(BuildConfig.CELEB) getString(R.string.celeb_share_awards_ready)
                    else getString(R.string.share_awards_ready)
                // 투표 시작일
                val awardBegin = ConfigModel.getInstance(this).awardBegin
                val start = awardsFormat.format(awardBegin)
                shareMsg = String.format(format, awardsData?.awardTitle, start)
            }
            "Y" -> {
                val format = if(BuildConfig.CELEB) getString(R.string.celeb_share_awards_run)
                    else getString(R.string.share_awards_run)
                shareMsg = String.format(format, awardsData?.awardTitle)
            }
            else -> {
                val format = if(BuildConfig.CELEB) getString(R.string.celeb_share_awards_result)
                    else getString(R.string.share_awards_result)
                shareMsg = String.format(format, awardsData?.awardTitle)
            }
        }

        UtilK.linkStart(this@IdolAwardsActivity, url= "", msg = shareMsg + "\n" + link)
        setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "share_awards"
        )
    }
}