package net.ib.mn.feature.rookie

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.WebViewActivity
import net.ib.mn.core.model.ChartModel
import net.ib.mn.databinding.FragmentRookieContainerBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.fragment.MiracleMainFragment
import net.ib.mn.idols.IdolApiManager
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.ConfigModel
import net.ib.mn.remote.IdolBroadcastManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.link.LinkUtil
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class RookieContainerFragment() : BaseFragment(), OnScrollToTopListener {

    private lateinit var binding: FragmentRookieContainerBinding

    private var realTimeChartModel: ChartModel? = null
    private var accumulateChartModel: ChartModel? = null

    private val rookieContainerViewModel: RookieContainerViewModel by activityViewModels()

    @Inject
    lateinit var idolBroadcastManager: IdolBroadcastManager

    @Inject
    lateinit var idolApiManager: IdolApiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        userVisibleHint = false // true가 기본값이라 fragment가 보이지 않는 상태에서도 visible한거로 처리되는 문제 방지
        super.onCreate(savedInstanceState)
        arguments?.let {
            val chartModelList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableArrayList(MiracleMainFragment.ARG_CHART_MODEL, ChartModel::class.java) ?: arrayListOf()
            } else {
                it.getParcelableArrayList(MiracleMainFragment.ARG_CHART_MODEL) ?: arrayListOf()
            }

            when (chartModelList.size) {
                1 -> {
                    realTimeChartModel = chartModelList.first()
                }
                else -> {
                    chartModelList.forEach { chartModel ->
                        when (chartModel.aggregateType.firstOrNull()) {
                            TYPE_ACCUMULATE -> accumulateChartModel = chartModel
                            TYPE_DAILY -> realTimeChartModel = chartModel
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRookieContainerBinding.inflate(inflater, container, false)
        binding.apply {
            lifecycleOwner = this@RookieContainerFragment
            view = this@RookieContainerFragment
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
    }

    override fun onResume() {
        super.onResume()
        idolBroadcastManager.startHeartbeat()

        if (!BuildConfig.CELEB) {
            idolApiManager.startTimer()
        }
        if (tabCheck) {   //기적 탭이 눌려있고, 실시간 순위 버튼을 누르고 있다면
            findRookieRankingFragment()?.stopTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        idolApiManager.stopTimer()
        findRookieRankingFragment()?.stopTimer()
    }

    private fun findRookieRankingFragment(): RookieRankingFragment? {
        return fm().findFragmentByTag(RookieRankingFragment.TAG) as? RookieRankingFragment
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)

        if (!BuildConfig.CELEB) {
            handleVisibility(isVisible)
        }
    }

    override fun onScrollToTop() {
        ((fm().findFragmentByTag(RookieRankingFragment.TAG) as? RookieRankingFragment) as? OnScrollToTopListener)?.onScrollToTop()
        ((fm().findFragmentByTag(RookieAggregatedFragment.TAG) as? RookieAggregatedFragment) as? OnScrollToTopListener)?.onScrollToTop()
    }

    private fun initUI() {
        binding.btnRealtime.isSelected = true

        Glide.with(requireContext())
            .load(realTimeChartModel?.imageUrl)
            .into(binding.ivBanner)

        if (ConfigModel.getInstance(binding.root.context).showRookieInfo != 0) {
            binding.btnRookieInfo.visibility = View.VISIBLE
        }
        handleVisibility(true)
    }

    private fun handleVisibility(isVisible: Boolean) {
        val fm = fm()

        val rookieRankingFrag = fm.findFragmentByTag(RookieRankingFragment.TAG) as? RookieRankingFragment
        val rookieAggregatedFrag = fm.findFragmentByTag(RookieAggregatedFragment.TAG) as? RookieAggregatedFragment

        //해당 프래그먼트 없을경우 대비.
        if (rookieRankingFrag == null ||
            rookieAggregatedFrag == null
        ) {
            createFragment()
            return
        }
    }

    private fun createFragment() {
        val fm = fm()
        val fragmentTransaction = fm.beginTransaction()

        if (fm.findFragmentByTag(RookieRankingFragment.TAG)?.isAdded != true) {
            val rookieRankingFrag = RookieRankingFragment.newInstance(realTimeChartModel ?: return)
            fragmentTransaction.add(
                binding.rookieRealtime.id,
                rookieRankingFrag,
                RookieRankingFragment.TAG
            )
        }
        if (fm.findFragmentByTag(RookieAggregatedFragment.TAG)?.isAdded != true) {
            val rookieAggregatedFrag = RookieAggregatedFragment.newInstance(accumulateChartModel ?: realTimeChartModel ?: return)
            fragmentTransaction.add(
                binding.rookieAggregated.id,
                rookieAggregatedFrag,
                RookieAggregatedFragment.TAG
            )
        }
        fragmentTransaction.commitAllowingStateLoss() // 크래시 발생하여 commitAllowingStateLoss()로 변경. 저장하는 state가 없으니 이래도 될듯.

        tabCheck = true
    }

    fun fm(): FragmentManager {
        return childFragmentManager
    }

    fun onClickRealTime() = with(binding) {
        if (btnRealtime.isSelected) return

        setSelectButton(btnRealtime)
        setUnSelectButton(btnAggregated)

        Glide.with(requireContext())
            .load(realTimeChartModel?.imageUrl)
            .into(binding.ivBanner)

        rookieRealtime.visibility = View.VISIBLE
        rookieAggregated.visibility = View.GONE

        val rookieRankingFrag =
            fm().findFragmentByTag(RookieRankingFragment.TAG) as? RookieRankingFragment
        rookieRankingFrag?.startTimer()

        tabCheck = true
    }

    fun onClickAggregated() = with(binding) {
        if (btnAggregated.isSelected) return

        setSelectButton(btnAggregated)
        setUnSelectButton(btnRealtime)

        Glide.with(requireContext())
            .load(accumulateChartModel?.imageRankUrl ?: realTimeChartModel?.imageRankUrl)
            .into(binding.ivBanner)

        rookieRealtime.visibility = View.GONE
        rookieAggregated.visibility = View.VISIBLE

        ////이달의 기적 실시간순위 타이머 돌던 것 제거
        val miracleRankingFrag =
            fm().findFragmentByTag(RookieRankingFragment.TAG) as? RookieRankingFragment
        miracleRankingFrag?.stopTimer()

        tabCheck = false
    }

    private fun setSelectButton(view: Button) {
        view.apply {
            isSelected = true
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_default))
        }
    }

    private fun setUnSelectButton(view: Button) {
        view.apply {
            isSelected = false
            setTextColor(ContextCompat.getColor(requireContext(), R.color.gray200))
        }
    }

    fun share() {
        val name = rookieContainerViewModel.firstPlaceRankerName.value ?: return
        setUiActionFirebaseGoogleAnalyticsFragment(
            GaAction.ROOKIE_SHARE.actionValue,
            GaAction.ROOKIE_SHARE.label
        )

        val url = LinkUtil.getAppLinkUrl(
            context = context ?: return,
            params = listOf(LinkStatus.ROOKIE.status)
        )

        val msg = String.format(
            Locale.getDefault(),
            getString(R.string.rookie_share_msg),
            accumulateChartModel?.targetMonth ?: realTimeChartModel?.targetMonth,
            name
        )
        UtilK.linkStart(context = context, url = url, msg = msg)
    }

    fun openInfoScreen() {
        val id = ConfigModel.getInstance(requireActivity()).showRookieInfo

        setUiActionFirebaseGoogleAnalyticsFragment(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            GaAction.ROOKIE_INFO.label
        )
        startActivity(
            WebViewActivity.createIntent(requireActivity(), Const.TYPE_EVENT,
                id,
                getString(R.string.title_rookie_month), isShowShare = false))
    }

    companion object {
        const val ARG_CHART_MODEL = "chart_model"
        const val TYPE_ACCUMULATE = "A"
        const val TYPE_DAILY = "D"

        var tabCheck = true
    }
}