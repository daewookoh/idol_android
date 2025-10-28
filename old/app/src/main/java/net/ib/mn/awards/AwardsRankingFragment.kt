package net.ib.mn.awards

import android.app.Activity.RESULT_OK
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
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.RequestFuture
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.awards.adapter.AwardsCategoryAdapter
import net.ib.mn.awards.viewmodel.AwardsMainViewModel
import net.ib.mn.awards.viewmodel.AwardsRankingViewModel
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.core.model.AwardChartsModel
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.AwardsRankingFragmentBinding
import net.ib.mn.databinding.ItemAwardsTopBinding
import net.ib.mn.databinding.NewFragmentRankingBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.VoteDialogFragment
import net.ib.mn.domain.usecase.GetAwardsIdolUseCase
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toPresentation
import net.ib.mn.remote.IdolBroadcastManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.EventBus
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.Logger
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.sort
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

// 어워즈 실시간 순위 화면
@AndroidEntryPoint
class AwardsRankingFragment : BaseFragment(), BaseDialogFragment.DialogResultHandler, AwardsRankingAdapter.OnClickListener,
    AwardsCategoryAdapter.OnClickListener, AwardsCategory {

    @Inject
    lateinit var idolBroadcastManager: IdolBroadcastManager

    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase

    @Inject
    lateinit var getAwardsIdolUseCase: GetAwardsIdolUseCase
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    private var mRankingAdapter: AwardsRankingAdapter? = null

    private lateinit var models: ArrayList<IdolModel>

    private var timerHandler: Handler? = null // 10초 자동갱신 타이머
    private var timerRunnable: Runnable? = null
    private val refreshInterval: Long = if (BuildConfig.DEBUG) 5 else 10 // 10초 갱신

    private var displayErrorHandler: Handler? = null

    private var isUpdate = false

    private lateinit var binding: BindingProxy

    private val awardsMainViewModel: AwardsMainViewModel by activityViewModels()
    private val awardsRankingViewModel: AwardsRankingViewModel by viewModels()

    private var disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userVisibleHint = false // true가 기본값이라 fragment가 보이지 않는 상태에서도 visible한거로 처리되는 문제 방지

        mGlideRequestManager = Glide.with(this)
        displayErrorHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                val responseMsg = msg.obj as String
                Toast.makeText(activity, responseMsg, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        lifecycleScope.launch {
            EventBus.receiveEvent<Boolean>(Const.BROADCAST_MANAGER_MESSAGE).collect {
                updateDataWithUI()
            }
        }

        // udp 계속 받기
        try {
            if (ConfigModel.getInstance(requireContext()).udp_stage > 0) {
                idolBroadcastManager.startHeartbeat()
            }
        } catch( e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding: NewFragmentRankingBinding = DataBindingUtil.inflate(inflater, R.layout.new_fragment_ranking, container, false)
        this.binding = BindingProxy(binding)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        awardsRankingViewModel.setAwardData(requireContext())

        models = ArrayList()
        binding.bindingTopAwards.root.visibility = View.VISIBLE
        binding.bindingTopAwards.tvTopTitle.text = awardsMainViewModel.getAwardData()?.charts?.get(0)?.name

        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)

        binding.rvRanking.layoutManager =
            LinearLayoutManagerWrapper(requireContext(), LinearLayoutManager.VERTICAL, false)
        mRankingAdapter = AwardsRankingAdapter(
            requireActivity(),
            requireContext(),
            this,
            mGlideRequestManager,
            this,
            awardsMainViewModel.getAwardData(),
            ConfigModel.getInstance(context).votable,
            awardsRankingViewModel.getRequestChartCodeModel() ?: AwardChartsModel("", "", "")
        )

        binding.rvRanking.apply {
            itemAnimator = null
        }

        mRankingAdapter?.setHasStableIds(true)
        binding.rvRanking.adapter = mRankingAdapter
        binding.rvRanking.addItemDecoration(divider)
        binding.rvRanking.setHasFixedSize(true)

        val awardData = awardsMainViewModel.getAwardData()
        mGlideRequestManager.load(
            if (Util.isDarkTheme(activity)) {
                awardData?.bannerDarkImgUrl
            } else {
                awardData?.bannerLightImgUrl
            },
        )
        .into(object : CustomTarget<Drawable?>() {

            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable?>?,
            ) {
                binding.clBanner.background = resource
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })

        setCategories(
            requireContext(),
            binding.rvTag,
            awardsMainViewModel,
            this,
        )

        // DB에 저장된 순위 먼저 보여준다
        updateDataWithUI()
    }

    fun stopPlayer() {
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

        Util.log("AwardsRankingFragment onPause")

        // 움짤 멈추기
        stopPlayer()

        stopTimer()
    }

    override fun onStop() {
        super.onStop()

        Util.log("AwardsRankingFragment onStop")
    }

    override fun onResume() {
        super.onResume()

        if(BuildConfig.CELEB) {
            if (!AwardsMainFragment.isRealTimeClicked) {
                stopTimer()
            } else {
                startTimer()
            }
        }

        Util.log("AwardsRankingFragment onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    // 셀럽에서 투표 후 처리
    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCode.RANKING_VOTE.value &&
            resultCode == BaseDialogFragment.RESULT_OK
        ) {
            val heart = data?.getLongExtra(VoteDialogFragment.PARAM_HEART, 0)
            if ((heart ?: 0) > 0) {
                Util.setPreference(activity, Const.AWARD_RANKING, System.currentTimeMillis() + Const.AWARD_COOLDOWN_TIME)
                updateDataWithUI()

                // 레벨업 체크
                if (data != null) {
                    val idol: IdolModel = data.getSerializableExtra(VoteDialogFragment.PARAM_IDOL_MODEL) as IdolModel
                    val paramHeart = data.getLongExtra(VoteDialogFragment.PARAM_HEART, 0)
                    UtilK.checkLevelUp(baseActivity, accountManager, idol, paramHeart)
                }
            } else {
                Util.closeProgress()
            }
        }
    }

    fun updateDataWithUI() {
        Util.log("*** Awards updateDataWithUI")
        if (isUpdate) return
        isUpdate = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val future = RequestFuture.newFuture<JSONObject>()

                Logger.v("AwardDataCheck::updateDataWithUI")
                val response = idolsRepository.getAwardIdols(
                    awardsRankingViewModel.getRequestChartCodeModel()?.code ?: return@launch,
                    "heart,top3"
                )

                // 여기서 리턴을 시켜주지 않습니다.(헤더 값은 무조건 보여줘야됨)
                val errorMsg = if (!response.optBoolean("success")) {
                    ErrorControl.parseError(activity, response)
                } else {
                    null
                }

                val objects = response.getJSONArray("objects")
                // adapter가 models를 바라보고 있어서 여기서 clear하는 순간 스크롤을 하면 crash
                val idols: ArrayList<IdolModel> = getIdolModels(objects)

                // updateDataWithUI()가 양쪽 쓰레드에서 불리는 경우 순위가 중복되어 나와서 이를 방지. (셀럽)
                synchronized(this) {
                    isUpdate = false
                }

                activity?.runOnUiThread {
                    hideLoadData()
                    applyItems(idols, errorMsg)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                val idols: ArrayList<IdolModel> = getIdolModels(null)

                activity?.runOnUiThread {
                    hideLoadData()
                    applyItems(idols, null)
                }

                isUpdate = false
            }
        }
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)

        Util.log("Ranking::onVisibilityChanged was called")

        if (!isVisible || !BuildConfig.CELEB) {
            return
        }

        if (Util.getPreferenceLong(activity, Const.AWARD_RANKING, 0L) < System.currentTimeMillis()) {
            Util.setPreference(activity, Const.AWARD_RANKING, System.currentTimeMillis() + Const.AWARD_COOLDOWN_TIME)
            updateDataWithUI()
        }
    }

    fun startTimer() {
        Logger.v("Awards startTimer")
        if (timerRunnable != null) {
            timerHandler?.removeCallbacks(timerRunnable!!)
        }

        timerRunnable = Runnable {
            try {
                Logger.v("Awards startTimer 10 seconds")
                if (Util.getPreferenceLong(activity, Const.AWARD_RANKING, 0L) < System.currentTimeMillis()) {
                    Util.setPreference(activity, Const.AWARD_RANKING, System.currentTimeMillis() + Const.AWARD_COOLDOWN_TIME)
                    updateDataWithUI()
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            } finally {
                timerHandler?.postDelayed(timerRunnable!!, refreshInterval * 1000)
            }
        }

        timerHandler = Handler(Looper.getMainLooper())
        timerHandler?.postDelayed(timerRunnable!!, refreshInterval * 1000)
    }

    fun stopTimer() {
        Util.log("Awards stopTimer")

        if (timerRunnable != null) {
            timerHandler?.removeCallbacks(timerRunnable!!)
        }
    }

    private fun applyItems(items: ArrayList<IdolModel>, errorMsg: String?) {

        //비어있을때 (첫 번째 헤더 값 제외)
        if (items.size <= 1) {
            mRankingAdapter?.setEmptyVHErrorMessage(errorMsg)
            //Empty View dummy값 추가.
            items.add(IdolModel(id = AwardsRankingAdapter.EMPTY_ITEM))
        }
        mRankingAdapter?.submitList(items)
    }

    private fun hideLoadData() = with(binding){
        tvLoadData.visibility = View.GONE
        rvRanking.visibility = View.VISIBLE
    }

    private fun openCommunity(idol: IdolModel) {
        if (Util.mayShowLoginPopup(activity) || activity == null) return

        // awards용 idol model은 필수정보가 빠져 있으므로 Idol DB에서 다시 꺼내온다
        lifecycleScope.launch(Dispatchers.IO) {
            val currentIdol = getIdolByIdUseCase(idol.getId())
                .mapDataResource { it?.toPresentation() }
                .awaitOrThrow()
            currentIdol?.let {
                try {
                    requireActivity().runOnUiThread {
                        startActivityForResult(
                            CommunityActivity.createIntent(requireActivity(), it),
                            RequestCode.COMMUNITY_OPEN.value
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun voteHeart(idol: IdolModel, totalHeart: Long, freeHeart: Long) {
        val dialogFragment = VoteDialogFragment.getIdolVoteInstance(idol, totalHeart, freeHeart)
        dialogFragment.setTargetFragment(this, RequestCode.RANKING_VOTE.value)
        dialogFragment.show(parentFragmentManager, "vote")
    }

    override fun onItemClicked(item: IdolModel?) {
        if (item?.sourceApp == SourceApp.IDOL.value) {
            val url = "https://www.myloveidol.com/community/?idol=${item.getId()}" +
                "&locale=${UtilK.getShareLocale(requireActivity())}" +
                "&from=${awardsMainViewModel.getAwardData()?.name}" +
                "&utm_source=actor" +
                "&utm_medium=install" +
                "&utm_campaign=${awardsMainViewModel.getAwardData()?.name}"

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url),
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }

        if (item != null) {
            openCommunity(item)
        }
    }

    override fun onVote(item: IdolModel) {
        if (Util.mayShowLoginPopup(baseActivity)) {
            return
        }

        if (item.sourceApp == SourceApp.IDOL.value) {
            val url = "https://www.myloveidol.com/community/?idol=${item.getId()}" +
                "&locale=${UtilK.getShareLocale(requireActivity())}" +
                "&from=${awardsMainViewModel.getAwardData()?.name}" +
                "&utm_source=actor" +
                "&utm_medium=install" +
                "&utm_campaign=${awardsMainViewModel.getAwardData()?.name}"

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url),
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }

        Util.showProgress(activity)
        lifecycleScope.launch {
            usersRepository.isActiveTime(
                { response ->
                    Util.closeProgress()

                    if (response.optBoolean("success")) {
                        val gcode = response.optInt("gcode")
                        if (response.optString("active") == Const.RESPONSE_Y) {
                            if (response.optInt("total_heart") == 0) {
                                Util.showChargeHeartDialog(activity)
                            } else {
                                if (response.optString("vote_able").equals(Const.RESPONSE_Y, ignoreCase = true)) {
                                    voteHeart(item, response.optLong("total_heart"), response.optLong("free_heart"))
                                } else {
                                    if (gcode == 1) {
                                        Toast.makeText(activity, getString(R.string.response_users_is_active_time_over), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(activity, getString(R.string.msg_not_able_vote), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            val start = Util.convertTimeAsTimezone(response.optString("begin"))
                            val end = Util.convertTimeAsTimezone(response.optString("end"))
                            val unableUseTime = String.format(
                                getString(R.string.msg_unable_use_vote),
                                start,
                                end,
                            )
                            Util.showIdolDialogWithBtn1(
                                activity,
                                null,
                                unableUseTime,
                            ) { Util.closeIdolDialog() }
                        }
                    } else { // success is false!
                        UtilK.handleCommonError(activity, response)
                    }
                }, {
                    Util.closeProgress()
                    Toast.makeText(activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onIntoAppBtnClicked(view: View) {
        startActivity(
            Intent(activity, AppLinkActivity::class.java).apply {
                data = Uri.parse(awardsMainViewModel.getAwardData()?.bannerUrl)
            },
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCode.COMMUNITY_OPEN.value && resultCode == RESULT_OK) {
            // 커뮤에서 투표했을시..
            Util.log("Awards Community vote updated")
            updateDataWithUI()
        }
    }

    fun updateHeaderTitle(requestChartCodeModel: AwardChartsModel?) {
        binding.bindingTopAwards.tvTopTitle.text = requestChartCodeModel?.name
    }

    private suspend fun getIdolModels(objects: JSONArray?): ArrayList<IdolModel> {
        // adapter가 models를 바라보고 있어서 여기서 clear하는 순간 스크롤을 하면 crash
        var idols: ArrayList<IdolModel> = ArrayList()

        for (i in 0 until (objects?.length() ?: 0)) {
            val id = objects?.getJSONObject(i)?.optInt("id")
            val heart = objects?.getJSONObject(i)?.optLong("heart")
            val top3 = objects?.getJSONObject(i)?.optString("top3")
            val top3Type = objects?.getJSONObject(i)?.optString("top3_type")

            val idolResource = getAwardsIdolUseCase(id ?: continue)
                .mapDataResource { it }
                .awaitOrThrow()
            val idol = idolResource?.toPresentation()

            if( idol != null ) {
                Logger.w("idol: $id, $heart, $top3, $top3Type. idol=${idol.toString()}")
                idol.heart = heart ?: break
                idol.top3 = top3
                idol.top3Type = if (top3Type.isNullOrEmpty()) null else top3Type
                idols.add(idol)
            }
        }

        idols = sort(activity?.applicationContext ?: return idols, idols)

        idols.add(
            0,
            IdolModel(
                id = -1,
                description = awardsRankingViewModel.getRequestChartCodeModel()?.desc
                    ?: "" //dummy값에다가 차트코드 description추가.
            )
        )

        return idols
    }

    // 태그(카테고리) 클릭
    override fun onItemClicked(position: Int) {
        val chartModel = awardsMainViewModel.getAwardData()?.charts?.get(position) ?: return

        awardsRankingViewModel.setSaveState(
            requestChartCodeModel = chartModel,
            currentStatus = awardsMainViewModel.getAwardData()?.name
        )
        updateHeaderTitle(chartModel)
        mRankingAdapter?.requestChartModel = chartModel
        updateDataWithUI()
    }

    inner class BindingProxy {
        private val viewBinding: ViewDataBinding
        val root: View
        val rvRanking: RecyclerView
        val tvLoadData: AppCompatTextView
        val bindingTopAwards: ItemAwardsTopBinding
        val rvTag: RecyclerView?
        val clBanner: View // 배너 이미지 및 타이틀 출력 부분

        constructor(binding: NewFragmentRankingBinding) {
            viewBinding = binding
            root = binding.root
            rvRanking = binding.rvRanking
            tvLoadData = binding.tvLoadData
            bindingTopAwards = binding.inTopAwards
            rvTag = binding.inTopAwards.rvTag
            clBanner = binding.inTopAwards.clBanner
        }

        constructor(binding: AwardsRankingFragmentBinding) {
            viewBinding = binding
            root = binding.root
            rvRanking = binding.rvRanking
            tvLoadData = binding.tvLoadData
            bindingTopAwards = binding.inTopAwards
            rvTag = null
            clBanner = binding.inTopAwards.clBanner
        }
    }

    companion object {
    }
}