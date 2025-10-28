package net.ib.mn.awards

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.GaonHistoryActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.awards.adapter.AwardsCategoryAdapter
import net.ib.mn.awards.viewmodel.AwardsAggregatedViewModel
import net.ib.mn.awards.viewmodel.AwardsMainViewModel
import net.ib.mn.core.model.AwardChartsModel
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.databinding.AwardsRankingFragmentBinding
import net.ib.mn.domain.usecase.GetIdolsByIdsUseCase
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.HallModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Const
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import org.json.JSONArray
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject

// 어워즈 중간집계/최종 결과 화면
@AndroidEntryPoint
class AwardsAggregatedFragment : BaseFragment(), AwardsCategory, AwardsAggregatedAdapter.OnClickListener, AwardsCategoryAdapter.OnClickListener {

    private lateinit var binding: AwardsRankingFragmentBinding

    private var mRankingAdapter: AwardsAggregatedAdapter? = null

    private lateinit var models: ArrayList<HallModel>
    private var displayErrorHandler: Handler? = null

    private var isUpdate = false

    private val awardsMainViewModel: AwardsMainViewModel by activityViewModels()
    private val awardsAggregatedViewModel: AwardsAggregatedViewModel by activityViewModels()
    @Inject
    lateinit var trendsRepository: TrendsRepositoryImpl

    @Inject
    lateinit var getIdolsByIdsUseCase: GetIdolsByIdsUseCase

    open class MyHandler(fragment: AwardsAggregatedFragment) : Handler(Looper.getMainLooper()) {
        private val weakReference: WeakReference<AwardsAggregatedFragment> = WeakReference<AwardsAggregatedFragment>(fragment)

        override fun handleMessage(msg: Message) {
            weakReference.get()?.handleMessage(msg)
        }
    }

    private fun handleMessage(msg: Message) {
        val responseMsg = msg.obj as String
        Toast.makeText(requireContext(), responseMsg, Toast.LENGTH_SHORT)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGlideRequestManager = Glide.with(this)
        displayErrorHandler = MyHandler(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.awards_ranking_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        awardsAggregatedViewModel.setAwardData(context)

        models = ArrayList()
        if (ConfigModel.getInstance(context).votable == "Y") {
            binding.inTopAwards.root.visibility =
                View.VISIBLE
            binding.inTopAwards.tvTopTitle.text = awardsMainViewModel.getAwardData()?.charts?.get(0)?.name

            val awardData = awardsMainViewModel.getAwardData()
            mGlideRequestManager.load(
                if (Util.isDarkTheme(activity)) {
                    awardData?.bannerDarkImgUrl
                } else {
                    awardData?.bannerLightImgUrl
                },
            ).into(object : CustomTarget<Drawable?>() {

                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable?>?,
                    ) {
                        binding.inTopAwards.clBanner.background = resource
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        } else {
            binding.inTopAwards.root.visibility = View.GONE
        }

        // 어댑터 초기값 세팅
        setAdapter()

        // DB에 저장된 순위 먼저 보여준다
        updateDataWithUI()

        setCategories(
            requireContext(),
            binding.inTopAwards.rvTag,
            awardsMainViewModel,
            this,
        )
    }

    private fun setAdapter() {
        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)

        binding.rvRanking.layoutManager = LinearLayoutManagerWrapper(requireContext(), LinearLayoutManager.VERTICAL, false)
        mRankingAdapter = AwardsAggregatedAdapter(
            requireActivity(),
            requireContext(),
            mGlideRequestManager,
            { item: IdolModel? -> onItemClicked(item) },
            this,
            awardsMainViewModel.getAwardData(),
        )

        binding.rvRanking.apply {
            itemAnimator = null
        }

        mRankingAdapter?.setHasStableIds(true)
        with(binding) {
            rvRanking.adapter = mRankingAdapter
            rvRanking.addItemDecoration(divider)
            rvRanking.setHasFixedSize(true)
        }
    }

    override fun onIntoAppBtnClicked(view: View) {
        startActivity(
            Intent(activity, AppLinkActivity::class.java).apply {
                data = Uri.parse(awardsMainViewModel.getAwardData()?.bannerUrl)
            }
        )
    }

    override fun onItemClicked(item: IdolModel?) {
        if(BuildConfig.CELEB) {
            val intent = GaonHistoryActivity.createIntent(activity, item)
            intent.putExtra(Const.EXTRA_GAON_EVENT, awardsMainViewModel.getCurrentStatus())
            intent.putExtra(Const.KEY_CHART_CODE, awardsAggregatedViewModel.getRequestChartCodeModel()?.code)
            intent.putExtra(Const.KEY_SOURCE_APP, item?.sourceApp)
            startActivityForResult(intent, 1)
        } else {
            val intent = AwardsHistoryActivity.createIntent(activity, item)
            intent.putExtra(Const.EXTRA_GAON_EVENT, awardsMainViewModel.getCurrentStatus())
            intent.putExtra(Const.KEY_CHART_CODE, awardsAggregatedViewModel.getRequestChartCodeModel()?.code)
            startActivityForResult(intent, 1)
        }
    }

    private fun stopPlayer() {
        if (Const.USE_ANIMATED_PROFILE) {
            stopExoPlayer(playerView1)
            stopExoPlayer(playerView2)
            stopExoPlayer(playerView3)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onPause() {
        super.onPause()
        Logger.v("AwardsRankingFragment onPause")

        // 움짤 멈추기
        stopPlayer()
    }

    fun updateDataWithUI() {
        if (isUpdate) return
        isUpdate = true

        MainScope().launch {
            trendsRepository.awardRank(
                awardsAggregatedViewModel.getRequestChartCodeModel()?.code ?: return@launch,
                awardsMainViewModel.getCurrentStatus(),
                { response ->
                    val objects = response.optJSONArray("data")

                    val errorMsg = if (objects?.length() == 0) {
                        mRankingAdapter?.setVisibleAwardToday(false)
                        getString(R.string.label_award_aggregated_no_data)
                    } else {
                        null
                    }

                    getHallModels(objects, errorMsg, response.optBoolean("success"))
                }, { throwable ->
                    isUpdate = false
                }
            )
        }
    }

    private fun applyItems(items: ArrayList<HallModel>, errorMsg: String?) {
        Util.closeProgress()
        if(items.size <= 1) {
            mRankingAdapter?.setEmptyVHErrorMessage(errorMsg)
            //Empty View dummy값 추가.
            val dummyHallModel = HallModel()
            dummyHallModel.id =
                AwardsAggregatedAdapter.EMPTY_ITEM
            items.add(dummyHallModel)
        }
        mRankingAdapter?.submitList(items)
    }

    private fun hideLoadData() = with(binding){
        tvLoadData.visibility = View.GONE
        rvRanking.visibility = View.VISIBLE
    }

    fun updateHeaderTitle(requestChartCodeModel: AwardChartsModel?) {
        binding.inTopAwards.tvTopTitle.text = requestChartCodeModel?.name
    }

    private fun getHallModels(objects: JSONArray?, errorMsg: String?, isSuccess: Boolean) {
        val hallModels: ArrayList<HallModel> = ArrayList<HallModel>()

        if (ConfigModel.getInstance(context).votable == "Y") {    // 누적순위일 경우, TopViewHolder에는 가짜 dummy값이 필요해서 추가
            val dummyHallModel = HallModel()
            dummyHallModel.id =
                -1    // ListAdapter diffUtil 나누는 기준이 HallModel의 id여서 -1로 세팅
            hallModels.add(dummyHallModel)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val missingIdolIds = mutableSetOf<Int>()
            val length = objects?.length() ?: 0

            for (i in 0 until length) {
                val obj = objects?.getJSONObject(i)
                val hall = IdolGson.getInstance(false)
                    .fromJson(obj.toString(), HallModel::class.java)
                // idol이 이미 채워져 있지 않다면, DB에서 조회할 idolId를 수집
                if (hall.idol == null) {
                    missingIdolIds.add(hall.idol_id)
                }
                hallModels.add(hall)
            }

            val dbIdols = getIdolsByIdsUseCase(missingIdolIds.toList())
                .mapListDataResource { it.toPresentation() }
                .awaitOrThrow()

            val idolMap = dbIdols?.associateBy { it.getId() } ?: emptyMap()

            val finalHallModels = hallModels.mapNotNull { hall ->
                if (hall.idol == null) {
                    hall.idol = idolMap[hall.idol_id]
                }
                if (hall.idol != null) hall else null
            }

            for (i in finalHallModels.indices) {
                val item = finalHallModels[i]
                item.rank = if (i > 0 && finalHallModels[i - 1].score == item.score) {
                    finalHallModels[i - 1].rank
                } else {
                    i
                }
            }

            if (finalHallModels.isNotEmpty() && ConfigModel.getInstance(context).votable == "A") {
                finalHallModels[0].chartName = awardsAggregatedViewModel.getRequestChartCodeModel()?.name
            }

            withContext(Dispatchers.Main) {
                hideLoadData()
                applyItems(hallModels, errorMsg)

                if (BuildConfig.DEBUG && !isSuccess) {
                    IdolSnackBar.make(
                        activity?.findViewById(
                            android.R.id.content
                        ) ?: return@withContext,
                        errorMsg
                    ).show()
                }

                isUpdate = false
            }
        }
    }

    // 태그(카테고리) 클릭
    override fun onItemClicked(position: Int) {
        val chartModel = awardsMainViewModel.getAwardData()?.charts?.get(position) ?: return

        awardsAggregatedViewModel.setSaveState(
            requestChartCodeModel = chartModel,
            currentStatus = awardsMainViewModel.getAwardData()?.name
        )
        updateHeaderTitle(chartModel)
        updateDataWithUI()
    }
}