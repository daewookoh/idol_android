package net.ib.mn.awards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import net.ib.mn.R
import net.ib.mn.databinding.FragmentAwardsMainBinding
import net.ib.mn.fragment.BaseFragment
import com.bumptech.glide.Glide
import net.ib.mn.awards.viewmodel.AwardsMainViewModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util

class AwardsMainFragment : BaseFragment(), View.OnClickListener{

    private var reloadData = true
    private lateinit var binding: FragmentAwardsMainBinding

    private val awardsMainViewModel: AwardsMainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        userVisibleHint = false // true가 기본값이라 fragment가 보이지 않는 상태에서도 visible한거로 처리되는 문제 방지

        super.onCreate(savedInstanceState)
        mGlideRequestManager = Glide.with(this)

        Util.log("Awards onCreate")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_awards_main, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding){
            tabbtnAggregated.setOnClickListener(this@AwardsMainFragment)
            tabbtnRealtime.setOnClickListener(this@AwardsMainFragment)
            tabbtnGuide.setOnClickListener(this@AwardsMainFragment)
            tabbtnRealtime.isSelected = true
        }

        awardsMainViewModel.setAwardData(context)
        //초기 프래그먼트 추가.
        createFragment()
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)

        //해당 프래그먼트 없을경우 대비.
        if (childFragmentManager.findFragmentByTag("AwardsRanking") == null ||
            childFragmentManager.findFragmentByTag("AwardsAggregated") == null ||
            childFragmentManager.findFragmentByTag("AwardsGuide") == null
        ) {
            Thread {
                activity?.runOnUiThread {
                    createFragment()
                }
            }.start()
            return
        }

        Util.log("AwardsMain onVisibilityChanged ${isRealTimeClicked}")

        val awardsRankingFrag = childFragmentManager.findFragmentByTag("AwardsRanking") as AwardsRankingFragment
        if(isVisible){
            if(reloadData && Util.getPreferenceLong(activity, Const.AWARD_RANKING, 0L) < System.currentTimeMillis()){
                Util.setPreference(activity, Const.AWARD_RANKING, System.currentTimeMillis()+Const.AWARD_COOLDOWN_TIME)
                awardsRankingFrag.updateDataWithUI()
            }
            if(isRealTimeClicked){
                awardsRankingFrag.startTimer()
            }
        }
        else{
            awardsRankingFrag.stopTimer()
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            binding.tabbtnAggregated.id -> {
                if(!binding.tabbtnAggregated.isSelected){
                    with(binding){
                        tabbtnAggregated.isSelected = true
                        tabbtnRealtime.isSelected = false
                        tabbtnGuide.isSelected = false
                        tabbtnAggregated.setTextColor(ContextCompat.getColor(requireContext(), R.color.main))
                        tabbtnRealtime.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray200))
                        tabbtnGuide.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray200))
                        awardsAggregated.visibility = View.VISIBLE
                        awardsRealtime.visibility = View.GONE
                        awardsGuide.visibility = View.GONE
                    }
                    val awardsAggregatedFrag = childFragmentManager.findFragmentByTag("AwardsAggregated") as AwardsAggregatedFragment
                    awardsAggregatedFrag.updateDataWithUI()
                    awardsAggregatedFrag.updateHeaderTitle(awardsMainViewModel.getRequestChartCodeModel())
                    val awardsRankingFrag = childFragmentManager.findFragmentByTag("AwardsRanking") as AwardsRankingFragment
                    awardsRankingFrag.stopTimer()
                    isRealTimeClicked = false
                }
            }
            binding.tabbtnRealtime.id -> {
                if(!binding.tabbtnRealtime.isSelected){
                    with(binding){
                        tabbtnAggregated.isSelected = false
                        tabbtnRealtime.isSelected = true
                        tabbtnGuide.isSelected = false
                        tabbtnAggregated.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray200))
                        tabbtnRealtime.setTextColor(ContextCompat.getColor(requireContext(), R.color.main))
                        tabbtnGuide.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray200))
                        awardsAggregated.visibility = View.GONE
                        awardsRealtime.visibility = View.VISIBLE
                        awardsGuide.visibility = View.GONE
                    }
                    val awardsRankingFrag = childFragmentManager.findFragmentByTag("AwardsRanking") as AwardsRankingFragment
                    awardsRankingFrag.updateDataWithUI()
                    awardsRankingFrag.updateHeaderTitle(awardsMainViewModel.getRequestChartCodeModel())
                    awardsRankingFrag.startTimer()
                    isRealTimeClicked = true
                }
            }
            binding.tabbtnGuide.id -> {
                if(!binding.tabbtnGuide.isSelected) {
                    with(binding) {
                        tabbtnAggregated.isSelected = false
                        tabbtnRealtime.isSelected = false
                        tabbtnGuide.isSelected = true
                        tabbtnAggregated.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray200))
                        tabbtnRealtime.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray200))
                        tabbtnGuide.setTextColor(ContextCompat.getColor(requireContext(), R.color.main))
                        awardsAggregated.visibility = View.GONE
                        awardsRealtime.visibility = View.GONE
                        awardsGuide.visibility = View.VISIBLE
                    }
                    val awardsRankingFrag = childFragmentManager.findFragmentByTag("AwardsRanking") as AwardsRankingFragment
                    awardsRankingFrag.stopTimer()
                    isRealTimeClicked = false
                }
            }
        }
    }


    private fun createFragment() {
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()


        if (fragmentManager.findFragmentByTag("AwardsRanking")?.isAdded != true) {
            val awardsRankingFrag = AwardsRankingFragment()
            fragmentTransaction.add(
                R.id.awards_ranking_realtime,
                awardsRankingFrag,
                "AwardsRanking"
            )
        }
        if (fragmentManager.findFragmentByTag("AwardsAggregated")?.isAdded != true) {
            val awardsAggregatedFrag = AwardsAggregatedFragment()
            fragmentTransaction.add(
                R.id.awards_ranking_aggregated,
                awardsAggregatedFrag,
                "AwardsAggregated"
            )
        }

        if (fragmentManager.findFragmentByTag("AwardsGuide")?.isAdded != true) {
            val awardsGuideFragment = AwardsGuideFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(VISIBLE_EXAMPLE, false)
                }
            }
            fragmentTransaction.add(
                R.id.awards_ranking_guide,
                awardsGuideFragment,
                "AwardsGuide"
            )
        }

        fragmentTransaction.commitAllowingStateLoss() // 크래시 발생하여 commitAllowingStateLoss()로 변경. 저장하는 state가 없으니 이래도 될듯.

        isRealTimeClicked = binding.tabbtnRealtime.isSelected
    }

    companion object{
        //하단 탭 눌렸는지 체크
        var isRealTimeClicked = true

        const val VISIBLE_EXAMPLE = "visible_example"
    }
}
