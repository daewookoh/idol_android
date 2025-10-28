package net.ib.mn.awards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.awards.viewmodel.AwardsMainViewModel
import net.ib.mn.core.model.AwardModel
import net.ib.mn.databinding.FragmentAwardsGuideBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import java.lang.Exception
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AwardsGuideFragment(
    private val checkGuideTab: CheckGuideTabPositionListener? = null,
) : BaseFragment(), View.OnClickListener {

    private var checkGuideTabPosition: CheckGuideTabPositionListener? = null // celeb

    private lateinit var awardBegin: Date
    private lateinit var awardEnd: Date

    private lateinit var binding: FragmentAwardsGuideBinding

    private val awardsMainViewModel: AwardsMainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGlideRequestManager = Glide.with(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_awards_guide, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isVisibleExample =
            arguments?.getBoolean(AwardsMainFragment.VISIBLE_EXAMPLE, true) ?: true

        awardsMainViewModel.setAwardData(context)

        with(binding) {
            btnGroup.visibility = if (isVisibleExample) View.VISIBLE else View.GONE
            tabbtnGuide.setOnClickListener(this@AwardsGuideFragment)
            tabbtnExample.setOnClickListener(this@AwardsGuideFragment)

            tabbtnGuide.isSelected = true
        }
        this.checkGuideTabPosition = checkGuideTab

        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        if (fragmentManager.findFragmentByTag("AwardsRanking")?.isAdded != true && isVisibleExample) {
            val awardsRankingFrag = AwardsRankingFragment()
            fragmentTransaction.add(R.id.awards_ranking, awardsRankingFrag, "AwardsRanking")
            fragmentTransaction.commitAllowingStateLoss()
        }

        Logger.v("체크 ->" + awardsMainViewModel.getAwardData()?.awardTitle)
        awardBegin = ConfigModel.getInstance(context).awardBegin ?: Date()
        awardEnd = ConfigModel.getInstance(context).awardEnd ?: Date()

        uiSet()
    }

    // ui  관련 세팅
    private fun uiSet() {
        val awardData: AwardModel? = awardsMainViewModel.getAwardData()
        val data: List<String>? = if (awardData?.desc?.contains("|") == false) {
            null
        } else {
            awardData?.desc?.split("|")
        }

        Logger.v("체크 ->${awardData?.desc}")
        mGlideRequestManager
            .load(if (Util.isDarkTheme(requireActivity())) awardData?.logoDarkImgUrl else awardData?.logoLightImgUrl)
            .into(binding.awardsGuide.imgAwardMain)

        if (data != null) {
            // dark mode
            val darkmode = Util.getPreferenceInt(
                requireActivity(),
                Const.KEY_DARKMODE,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            )

            Logger.v("다크모드 $darkmode")

            val awardsFormat = SimpleDateFormat.getDateInstance(
                DateFormat.MEDIUM,
                LocaleUtil.getAppLocale(requireContext()),
            )
            // 미국 등에서 시작날이 하루 전으로 나와서 한국시간으로 설정
            awardsFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            binding.awardsGuide.tvAwardsDate.text = String.format(
                "%1s ~ %2s (KST)",
                awardsFormat.format(awardBegin),
                awardsFormat.format(
                    awardEnd,
                ),
            )

            try {
                with(binding.awardsGuide) {
                    tvAwardTitle.text = data[0]
                    tvAwardsInstruction.text = data[1]

                    tvTitleAwardVotePeriod.text = data[2]

                    tvTitleAwardVoteWork.text = data[3]
                    tvAwardVoteWork.text = data[4]

                    tvTitleAwardVotingWay.text = data[5]
                    tvAwardVotingWay.text = data[6]

                    tvTitleAwardNotice.text = data[7]
                    tvAwardNotice.text = data[8]

                    tvTitleAwardRules.text = data[9]
                    tvAwardRules.text = data[10]
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)

        if (isVisible) {
            val fragment = childFragmentManager.findFragmentByTag("AwardsRanking") as AwardsRankingFragment?
            fragment?.updateDataWithUI()
        }
    }

    interface CheckGuideTabPositionListener {
        fun checkCurrentTab(currentTab: Int)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.tabbtnGuide.id -> {
                binding.tabbtnGuide.isSelected = true
                binding.tabbtnExample.isSelected = false
                binding.tabbtnGuide.setTextColor(ContextCompat.getColor(requireContext(), R.color.main))
                binding.tabbtnExample.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray200))
                binding.awardsGuide.guideMain.visibility = View.VISIBLE
                binding.exampleMain.visibility = View.GONE
                if (BuildConfig.CELEB) {
                    checkGuideTabPosition?.checkCurrentTab(GUIDE_INSTRUCTION)
                    val awardsRankingFrag =
                        childFragmentManager.findFragmentByTag("AwardsRanking") as? AwardsRankingFragment
                    awardsRankingFrag?.stopTimer()
                    AwardsMainFragment.isRealTimeClicked = false
                }
            }
            binding.tabbtnExample.id -> {
                binding.tabbtnGuide.isSelected = false
                binding.tabbtnExample.isSelected = true
                binding.tabbtnGuide.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray200))
                binding.tabbtnExample.setTextColor(ContextCompat.getColor(requireContext(), R.color.main))
                binding.awardsGuide.guideMain.visibility = View.GONE
                binding.exampleMain.visibility = View.VISIBLE
                if (BuildConfig.CELEB) {
                    checkGuideTabPosition?.checkCurrentTab(GUIDE_EXAMPLE)
                    if (Util.getPreferenceLong(
                            activity,
                            Const.AWARD_RANKING,
                            0L,
                        ) < System.currentTimeMillis()
                    ) {
                        Util.setPreference(
                            activity,
                            Const.AWARD_RANKING,
                            System.currentTimeMillis() + Const.AWARD_COOLDOWN_TIME,
                        )
                        val awardsRankingFrag =
                            childFragmentManager.findFragmentByTag("AwardsRanking") as? AwardsRankingFragment
                        awardsRankingFrag?.updateDataWithUI()
                        awardsRankingFrag?.startTimer()
                    }
                    AwardsMainFragment.isRealTimeClicked = true
                }
            }
        }
    }

    companion object {
        const val GUIDE_INSTRUCTION = 0
        const val GUIDE_EXAMPLE = 1
    }
}