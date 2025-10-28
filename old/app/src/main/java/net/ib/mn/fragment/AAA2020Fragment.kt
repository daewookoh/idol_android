package net.ib.mn.fragment
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.GaonHistoryActivity
import net.ib.mn.activity.StatsActivity
import net.ib.mn.adapter.AAA2020Adapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.databinding.Aaa2020FragmentBinding
import net.ib.mn.model.AwardStatsModel
import net.ib.mn.model.HallModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.UtilK
import java.lang.reflect.Type
import javax.inject.Inject

@AndroidEntryPoint
class AAA2020Fragment : BaseFragment(), AAA2020Adapter.OnClickListener {

	private lateinit var binding: Aaa2020FragmentBinding
	private var mRankingAdapter: AAA2020Adapter? = null
	private var awardStatsModel: AwardStatsModel? = null
	private var awardStatsCode: String? = null
    private var awardStatsIndex: Int = 0

	private lateinit var models:ArrayList<HallModel>
    @Inject
    lateinit var trendsRepository: TrendsRepositoryImpl

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		mGlideRequestManager = Glide.with(this)
	}

	override fun onCreateView(inflater: LayoutInflater,
							  container: ViewGroup?,
							  savedInstanceState: Bundle?): View? {
		binding = DataBindingUtil.inflate(inflater, R.layout.aaa_2020_fragment, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		init()

		setAdapter()

		// DB에 저장된 순위 먼저 보여준다
		updateDataWithUI()
	}

	private fun init() {
		if (arguments != null) {
			awardStatsModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				arguments?.getSerializable(StatsActivity.PARAM_AWARD_STATS, AwardStatsModel::class.java)
			} else {
				arguments?.getSerializable(StatsActivity.PARAM_AWARD_STATS) as AwardStatsModel
			}
		}
		awardStatsCode = arguments?.getString(StatsActivity.PARAM_AWARD_STATS_CODE)
        awardStatsIndex = arguments?.getInt(StatsActivity.PARAM_AWARD_STATS_INDEX) ?: 0
		models = ArrayList()
	}

	private fun setAdapter() {
		val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
		divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable	.line_divider)!!)

        val title = UtilK.getAwardTitle(awardStatsModel?.resultTitle,
            awardStatsModel?.charts?.get(awardStatsIndex)?.name)
		binding.rvRanking.layoutManager = LinearLayoutManagerWrapper(requireContext(), LinearLayoutManager.VERTICAL, false)
		mRankingAdapter = AAA2020Adapter(requireContext(),
			mGlideRequestManager,
			this,
			models,
			awardStatsCode,
            title)

		mRankingAdapter?.setHasStableIds(true)
		with(binding) {
			rvRanking.adapter = mRankingAdapter
			rvRanking.addItemDecoration(divider)
			rvRanking.setHasFixedSize(true)
		}
	}


	override fun onItemClickListener(item: HallModel) {
		val intent = GaonHistoryActivity.createIntent(activity, item.idol)
        intent.putExtra(Const.EXTRA_GAON_EVENT, awardStatsModel?.name)
        intent.putExtra(Const.KEY_CHART_CODE, awardStatsCode)
		startActivity(intent)
	}

	fun updateDataWithUI() {
        MainScope().launch {
            trendsRepository.awardRank(
                awardStatsCode,
                awardStatsModel?.name,
                { response ->
                    if (!response.optBoolean("success")) {
                        showEmptyView()
                        return@awardRank
                    }
                    val objects = response.getJSONArray("data")
                    if (objects.length() == 0) {
                        showEmptyView()
                        return@awardRank
                    }

                    var idols: ArrayList<HallModel> = ArrayList<HallModel>()

                    val gson = IdolGson.getInstance()
                    val listType: Type? = object : TypeToken<List<HallModel>>() {}.type
                    idols = gson.fromJson(objects.toString(), listType)

                    for (i in idols.indices) {
                        val item: HallModel = idols[i]
                        if (i > 0 && idols[i - 1].score == item.score) {
                            item.rank =
                                idols[i - 1].rank
                        } else {
                            item.rank = i
                        }
                    }
                    applyItems(idols)
                }, { throwable ->
                }
            )
        }
	}

	private fun applyItems(items: ArrayList<HallModel>) {
		mRankingAdapter?.setItems(items)
		mRankingAdapter?.notifyDataSetChanged()

		if (items.isEmpty()) {
			showEmptyView()
		} else {
			hideEmptyView()
		}
	}

	private fun showEmptyView() {
		with(binding) {
			tvEmpty.visibility = View.VISIBLE
			rvRanking.visibility = View.GONE
		}
	}

	private fun hideEmptyView() {
		with(binding) {
			tvEmpty.visibility = View.GONE
			rvRanking.visibility = View.VISIBLE
		}
	}
}
