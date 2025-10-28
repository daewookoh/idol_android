package net.ib.mn.awards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import net.ib.mn.R
import net.ib.mn.awards.adapter.AwardsCategoryAdapter
import net.ib.mn.awards.viewmodel.AwardsAggregatedViewModel
import net.ib.mn.awards.viewmodel.AwardsMainViewModel
import net.ib.mn.databinding.FragmentAwardsResultBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.utils.UtilK

open class AwardsResultFragment : BaseFragment(), AwardsCategory, AwardsCategoryAdapter.OnClickListener {

    private lateinit var binding: FragmentAwardsResultBinding
    private val awardsMainViewModel: AwardsMainViewModel by activityViewModels()
    private val awardsAggregatedViewModel: AwardsAggregatedViewModel by activityViewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		mGlideRequestManager = Glide.with(this)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_awards_result, container, false)
        return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

        awardsMainViewModel.setAwardData(context)
        awardsAggregatedViewModel.setAwardData(context)

		val fragmentManager = childFragmentManager
		val fragmentTransaction = fragmentManager.beginTransaction()

		if(fragmentManager.findFragmentByTag("AwardsResult")?.isAdded != true) {
			val awardsAggFrag = AwardsAggregatedFragment()
			fragmentTransaction.add(R.id.awards_ranking_result, awardsAggFrag, "AwardsResult")
			fragmentTransaction.commit()

            setCategories(
                requireContext(),
                binding.rvTag,
                awardsMainViewModel,
                this
            )
		}

        val chartModel = awardsAggregatedViewModel.getAwardData()?.charts?.get(0)
        val awardData = awardsAggregatedViewModel.getAwardData()
        val resultTitle = awardData?.resultTitle // 2024 SBS 가요대전\n<CHART_NAME> 최종 결과
        val title = UtilK.getAwardTitle(resultTitle, chartModel?.name ?: "")
        binding.tvAwardTitle.text = title
	}

    // 태그(카테고리) 클릭
    override fun onItemClicked(position: Int) {
        val chartModel = awardsAggregatedViewModel.getAwardData()?.charts?.get(position) ?: return

        awardsAggregatedViewModel.setSaveState(
            requestChartCodeModel = chartModel,
            currentStatus = awardsAggregatedViewModel.getAwardData()?.name
        )

        val awardData = awardsAggregatedViewModel.getAwardData()
        val resultTitle = awardData?.resultTitle // 2024 SBS 가요대전\n<CHART_NAME> 최종 결과
        val title = UtilK.getAwardTitle(resultTitle, chartModel.name ?: "")
        binding.tvAwardTitle.text = title

        val awardsAggFrag = childFragmentManager.findFragmentByTag("AwardsResult") as AwardsAggregatedFragment
        awardsAggFrag.updateHeaderTitle(chartModel)
        awardsAggFrag.updateDataWithUI()
    }
}
