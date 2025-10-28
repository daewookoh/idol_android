package net.ib.mn.onepick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.MainActivity
import net.ib.mn.activity.MezzoPlayerActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.adapter.OnepickResultRankingAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseApplication
import net.ib.mn.core.data.repository.OnepickRepositoryImpl
import net.ib.mn.core.model.HelpInfosModel
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.ActionBarTitleAndImageBinding
import net.ib.mn.databinding.ActivityOnepickResultBinding
import net.ib.mn.dialog.DefaultDialog
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.OnepickIdolModel
import net.ib.mn.model.OnepickTopicModel
import net.ib.mn.model.toPresentation
import net.ib.mn.onepick.OnepickMatchActivity.Companion.PARAM_TOPIC
import net.ib.mn.onepick.viewholder.themepick.OnePickStatus
import net.ib.mn.onepick.viewholder.themepick.OnePickVoteStatus
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.ext.getSerializableData
import net.ib.mn.utils.getModelFromPref
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.trimNewlineWhiteSpace
import org.json.JSONArray
import java.text.NumberFormat
import javax.inject.Inject

@AndroidEntryPoint
class OnepickResultActivity : BaseActivity() {

    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase
    @Inject
    lateinit var sharedAppState: SharedAppState

    private var mTopic: OnepickTopicModel? = null
    private lateinit var mRankingAdapter: OnepickResultRankingAdapter
    private var rankingList = ArrayList<OnepickIdolModel>()
    var date: String = ""

    private var startActivityForResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ -> }

    private lateinit var binding: ActivityOnepickResultBinding
    @Inject
    lateinit var onepickRepository: OnepickRepositoryImpl

    val videoAdLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Util.closeProgress()
        if (result.resultCode == RESULT_CANCELED) {
            Util.handleVideoAdResult(this, false, false, MEZZO_PLAYER_REQ_CODE, result.resultCode, null, ""
            ) {
            }
            return@registerForActivityResult
        }
        // 비광 액티비티에 넘겨준 원픽 데이터 받아오기
        val item = result.data?.getSerializableExtra(PARAM_TOPIC) as? OnepickTopicModel
        item?.let {
            it.voteType = it.vote // 이미지픽 api 호출시에는 vote_type에 넣어서 호출해야 한다
            startActivityForResultLauncher.launch(
                OnepickMatchActivity.createIntent(this, it)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_onepick_result)
        binding.clOnepickResult.applySystemBarInsets(false)

        mTopic = intent.getSerializableData<OnepickTopicModel>(PARAM_ONEPICK_ID)

        setActionBar()
        loadRanking(mTopic?.id ?: -1)
        setBackPressed(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                //앱 내부로 들어왔을때.
                if (intent.getBooleanExtra(MainActivity.IS_DEEP_LINK_CLICK_FROM_IDOL, false)) {
                    return
                }
                setResult(OnePickMainFragment.IMAGE_PICK_LIST_UPDATE_RESULT_CODE)
                finish()
            }
        })
        setVoteViewVisibility()

        lifecycleScope.launch {
            sharedAppState.refreshImagePickResult.collect {
                updateStatus()
            }
        }

        // 처음 들어올 때 원픽 상태를 가져온다
        updateStatus()
    }

    // 원픽 상태 갱신
    private fun updateStatus() {
        // 이미지픽 상태를 다시 가져온다
        lifecycleScope.launch {
            onepickRepository.getResult(
                mTopic?.id ?: -1,
                false,
                { response ->
                    if (!response.optBoolean("success")) {
                        return@getResult
                    }

                    try {
                        val vote = response.optString("vote")
                        mTopic?.vote = vote
                        setVoteButtonStatus()
                    } catch (e: Exception) {
                    }
                },
                { error ->
                    showError()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        setVoteButtonStatus()
    }

    private fun setVoteViewVisibility() = with(binding) {
        cgVote.visibility = when (mTopic?.status) {
            OnePickStatus.DOING.code -> {
                View.VISIBLE
            }

            else -> {
                View.GONE
            }
        }
    }

    private fun setVoteButtonStatus() = with(binding) {
        when (mTopic?.vote) {
            OnePickVoteStatus.ABLE.code -> {
                clThemePickDoingVoteParticipation.setBackgroundResource(R.drawable.bg_square_active_btn)
                tvThemePickDoingVoteParticipation.text =
                    (this@OnepickResultActivity).getString(R.string.guide_vote_title)
            }

            OnePickVoteStatus.SEE_VIDEOAD.code -> {
                // 광고 시청 후 추가 투표로 변경
                clThemePickDoingVoteParticipation.setBackgroundResource(R.drawable.bg_square_active_btn)
                tvThemePickDoingVoteParticipation.text =
                    getString(R.string.imagepick_vote_with_ad)
            }

            OnePickVoteStatus.IMPOSSIBLE.code -> {
                clThemePickDoingVoteParticipation.setBackgroundResource(R.drawable.bg_square_result_btn)
                tvThemePickDoingVoteParticipation.setTextColor(
                    ContextCompat.getColor(
                        (this@OnepickResultActivity),
                        R.color.fix_white
                    )
                )
                tvThemePickDoingVoteParticipation.text =
                    (this@OnepickResultActivity).getString(R.string.onepick_already_voted)
            }
        }

        clThemePickDoingVoteParticipation.setOnClickListener {
            when (mTopic?.vote) {
                OnePickVoteStatus.ABLE.code -> {
                    mTopic?.let {
                        it.vote = OnePickVoteStatus.SEE_VIDEOAD.code
                        it.count += 1
                    }
                    goToMatch()
                }

                OnePickVoteStatus.SEE_VIDEOAD.code -> {
                    val intent = MezzoPlayerActivity.createIntent(this@OnepickResultActivity, Const.ADMOB_REWARDED_VIDEO_ONEPICK_UNIT_ID)
                    // 비광 액티비티에 이미지픽 데이터 넘겨주고 시청 완료 후 다시 받아온다
                    val bundle = Bundle()
                    bundle.putSerializable(PARAM_TOPIC, mTopic)
                    intent.putExtras(bundle)
                    videoAdLauncher.launch(intent)
                }

                OnePickVoteStatus.IMPOSSIBLE.code -> {
                    return@setOnClickListener
                }
            }
        }
    }

    private fun goToMatch() {
        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.IMAGE_PICK_RESULT_VOTE.actionValue,
            GaAction.IMAGE_PICK_RESULT_VOTE.label
        )

        startActivityForResultLauncher.launch(
            OnepickMatchActivity.createIntent(
                this,
                mTopic!!
            )
        )
    }

    private fun goToStore() {
        startActivityForResultLauncher.launch(
            NewHeartPlusActivity.createIntent(
                this,
                NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP
            )
        )
    }

    override fun onStop() {
        FLAG_CLOSE_DIALOG = false
        super.onStop()
    }

    private fun setActionBar() {

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val customActionView: ActionBarTitleAndImageBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.action_bar_title_and_image,
            null,
            false
        )
        customActionView.tvActionBarTitle.text = getString(R.string.imagepick)

        val helpInfoModel =
            Util.getPreference(this, Const.PREF_HELP_INFO).getModelFromPref<HelpInfosModel>()
        customActionView.ivActionBarInfo.setOnClickListener {
            DefaultDialog(
                title = getString(R.string.popup_title_imagepick),
                subTitle = helpInfoModel?.onePick,
                context = this,
                theme = android.R.style.Theme_Translucent_NoTitleBar
            ).show()
        }
        supportActionBar?.customView = customActionView.root
    }

    private fun showError() {
        Util.showIdolDialogWithBtn1(
            this@OnepickResultActivity,
            null,
            getString(R.string.error_abnormal_exception)
        ) {
            Util.closeIdolDialog()
            finish()
        }
    }

    private fun loadRanking(topicId: Int) {
        binding.llEmptyWrapper.visibility = View.VISIBLE

        if (topicId == -1) {
            showError()
        } else {
            Util.showProgress(this)
            MainScope().launch {
                onepickRepository.getResult(
                    topicId,
                    false,
                    { response ->
                        Util.closeProgress()

                        if (!response.optBoolean("success")) {
                            return@getResult
                        }

                        try {
                            val rankingJsonArray = response.getJSONArray("objects")
                            val rankingSize = rankingJsonArray.length()

                            date = response.optString("date")
                            rankingList.clear()

                            if (rankingSize <= 0) {
                                showError()
                                return@getResult
                            }

                            parseRankingList(rankingJsonArray, rankingSize)
                            sortRankingList()
                            assignRanks()

                            addHeaderDummyValue()

                            setAdapter()
                            setAdapterClickListener()

                            setLastPlace(rankingList.last()) { lastPlaceVotePercent ->
                                rankingList.forEach {
                                    it.minPercent = lastPlaceVotePercent
                                }
                                updateUI()
                            }
                        } catch (e: Exception) {
                            showError()
                        }
                    }, { throwable ->
                        Util.closeProgress()
                        showError()
                    }
                )
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.btn_share -> {
                if (rankingList.size > 0) {
                    shareImagePickRank()
                } else {
                    return false
                }
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun shareImagePickRank() {

        if (rankingList.isEmpty()) {
            return
        }

        val startIndex = rankingList.indexOfFirst { it.rank > 0 }

        //공유시 저장할 이름하고, 등수.
        val topIdolNames = arrayListOf<String>()
        val topIdolRanks = arrayListOf<String>()

        for (i in startIndex until rankingList.size) {
            val idolName = UtilK.removeWhiteSpace(
                Util.nameSplit(
                    this,
                    rankingList[i].idol
                )[0]
            )

            topIdolNames.add(
                idolName
            )

            val idolRank =
                String.format(getString(R.string.rank_format), rankingList[i].rank.toString())

            topIdolRanks.add(
                idolRank
            )
        }

        val params = listOf(LinkStatus.ONE_PICK.status, mTopic?.id.toString())
        val url = LinkUtil.getAppLinkUrl(context = this@OnepickResultActivity, params = params)

        val msg: String = String.format(
            if (BuildConfig.CELEB) getString(R.string.share_image_pick_celeb) else getString(R.string.share_image_pick),
            mTopic?.title,
            topIdolRanks[0],
            "#${topIdolNames[0]}",
            topIdolRanks[1],
            "#${topIdolNames[1]}",
            topIdolRanks[2],
            "#${topIdolNames[2]}",
            url
        )
        UtilK.linkStart(this, url = "", msg = msg.trimNewlineWhiteSpace())

        setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "share_imagepick"
        )
    }

    private fun setAdapter() {
        mRankingAdapter = OnepickResultRankingAdapter(
            rankingList,
            mTopic,
            date
        )

        binding.rvResultRanking.adapter = mRankingAdapter
    }

    private fun parseRankingList(rankingJsonArray: JSONArray, rankingSize: Int) {
        for (i in 0 until rankingSize) {
            val obj = rankingJsonArray.getJSONObject(i)
            val ranking = IdolGson.getInstance(false)
                .fromJson(obj.toString(), OnepickIdolModel::class.java)

            if (ranking.vote > 0) {
                rankingList.add(ranking)
            }
        }
    }

    private fun sortRankingList() {
        rankingList.sortWith(Comparator { o1, o2 ->
            when {
                (o1.vote > o2.vote) -> -1
                (o1.vote < o2.vote) -> 1
                else -> o1.idol!!.getName(this@OnepickResultActivity)
                    .compareTo(o2.idol!!.getName(this@OnepickResultActivity))
            }
        })
    }

    private fun assignRanks() {
        var rank = 0
        for (i in 0 until rankingList.size) {
            when {
                i == 0 -> rank++
                rankingList[i - 1].vote == rankingList[i].vote -> rank = rankingList[i - 1].rank
                else -> rank = i + 1
            }
            rankingList[i].rank = rank
            rankingList[i].lastPlaceVoteCount = rankingList.last().vote
            rankingList[i].firstPlaceVoteCount = rankingList.first().vote
        }
    }

    private fun addHeaderDummyValue() {
        rankingList.add(
            0,
            OnepickIdolModel(
                id = -1,
                vote = -1,
                idol = null,
                imageUrl = null,
                rank = 0
            )
        )
    }

    private fun updateUI() {
        binding.llEmptyWrapper.visibility = View.GONE
        mRankingAdapter.setDate(date)
        mRankingAdapter.notifyDataSetChanged()
    }

    private fun setAdapterClickListener() {
        mRankingAdapter.setOnePickResultClickListener(object :
            OnepickResultRankingAdapter.OnePickResultClickListener {
            override fun goCommunity(onePickIdolModel: OnepickIdolModel) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val idolModel = onePickIdolModel.idol?.getId()?.let { id ->
                        getIdolByIdUseCase(id)
                            .mapDataResource { it?.toPresentation() }
                            .awaitOrThrow()
                    }

                    withContext(Dispatchers.Main) {
                        startActivity(
                            CommunityActivity.createIntent(
                                this@OnepickResultActivity,
                                idolModel ?: return@withContext
                            )
                        )
                    }
                }
            }
        })
    }

    private fun setLastPlace(
        onePickIdol: OnepickIdolModel,
        successViewLayout: (Float) -> Unit
    ) =
        with(binding.inHeartPickLastPlaceRank.inGradientProgressBar) {
            progressBar.updateIsLastPlace(true)

            // 마지막 등수의 텍스트를 넣고.
            val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this@OnepickResultActivity))
            tvVote.text = numberFormat.format(onePickIdol.lastPlaceVoteCount)

            // 화면에 그려지면 가로 비율을 가져와줍니다.
            progressBar.setOnLayoutCompleteListener { percentWidth ->
                successViewLayout(percentWidth)
            }
        }

    companion object {
        private const val PARAM_ONEPICK_ID = "paramOnepickId"

        @JvmStatic
        fun createIntent(context: Context, topic: OnepickTopicModel): Intent {
            val intent = Intent(context, OnepickResultActivity::class.java)
            val args = Bundle()

            args.putSerializable(PARAM_ONEPICK_ID, topic)
            intent.putExtras(args)

            return intent
        }
    }
}