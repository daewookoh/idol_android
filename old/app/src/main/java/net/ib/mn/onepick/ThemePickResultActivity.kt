package net.ib.mn.onepick

import android.annotation.SuppressLint
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
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.MainActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.ThemepickRepositoryImpl
import net.ib.mn.core.model.HelpInfosModel
import net.ib.mn.databinding.ActionBarTitleAndImageBinding
import net.ib.mn.databinding.ActivityThemePickResultBinding
import net.ib.mn.dialog.DefaultDialog
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.ThemepickModel
import net.ib.mn.model.ThemepickRankModel
import net.ib.mn.onepick.viewholder.themepick.OnePickStatus
import net.ib.mn.onepick.viewholder.themepick.OnePickVoteStatus
import net.ib.mn.utils.*
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.ext.getSerializableData
import net.ib.mn.utils.link.LinkUtil
import org.json.JSONArray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class ThemePickResultActivity : BaseActivity(){

    //투표하고나서 보여주는 랭킹뷰.
    private lateinit var mThemePickResultAdapter: ThemePickResultAdapter

    //랭킹 리스트.
    private var mRankList = ArrayList<ThemepickRankModel>()

    private lateinit var mGlideRequestManager: RequestManager

    //테마픽 모델.
    private lateinit var mTheme: ThemepickModel

    //날짜 계산.
    private lateinit var dateFormat: SimpleDateFormat

    //처음 들어왔을때 공유창 띄워줌 여부.
    private var isShare = false

    private lateinit var binding: ActivityThemePickResultBinding

    @Inject
    lateinit var themepickRepository: ThemepickRepositoryImpl
    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase

    private var startActivityForResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            // no-op 차후 기획 어떻게 될지 몰라 코드만 남겨둠
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_theme_pick_result)
        binding.clContainer.applySystemBarInsets(false)

        initSet()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.btn_share -> {
                if (mRankList.size > 0) {
                    shareThemePickRank()
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

    private fun shareThemePickRank(){
        //아이돌이 3개미만 방어코드.
        var top1IdolName:String? = null
        var top2IdolName:String? = null
        var top3IdolName:String? = null

        var top1IdolRank:String? = null
        var top2IdolRank:String? = null
        var top3IdolRank:String? = null

        when (mRankList.size) {
            in 3..Int.MAX_VALUE -> {
                top1IdolName = getIdolName(mRankList[0])
                top2IdolName = getIdolName(mRankList[1])
                top3IdolName = getIdolName(mRankList[2])

                top1IdolRank = getIdolRank(mRankList[0])
                top2IdolRank = getIdolRank(mRankList[1])
                top3IdolRank = getIdolRank(mRankList[2])
            }
            2 -> {
                top1IdolName = getIdolName(mRankList[0])
                top2IdolName = getIdolName(mRankList[1])

                top1IdolRank = getIdolRank(mRankList[0])
                top2IdolRank = getIdolRank(mRankList[1])
            }
            1 -> {
                top1IdolName = getIdolName(mRankList[0])
                top1IdolRank = getIdolRank(mRankList[0])
            }
        }

        val params = listOf(LinkStatus.THEMEPICK.status, mTheme.id.toString())
        val url = LinkUtil.getAppLinkUrl(
            context = this@ThemePickResultActivity,
            params = params,
            querys = null
        )

        val msg = String.format(
            getString(if(BuildConfig.CELEB) R.string.share_themepick_celeb else R.string.share_themepick),
            mTheme.title,
            top1IdolRank ?: "",
            "#${top1IdolName ?: ""}",
            top2IdolRank ?: "",
            "#${top2IdolName ?: ""}",
            top3IdolRank ?: "",
            "#${top3IdolName ?: ""}",
            url
        )
        UtilK.linkStart(this, url= "", msg = msg.trimNewlineWhiteSpace())

        setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "share_themepick"
        )
    }

    private fun getIdolName(rank: ThemepickRankModel): String {
        return UtilK.removeWhiteSpace(shareThemePickRankName(rank.title ?: "", rank.subtitle ?: ""))
    }

    private fun getIdolRank(rank: ThemepickRankModel): String {
        return String.format(getString(R.string.rank_format), rank.rank.toString())
    }

    //공유할떄 _(언더바) 존재여부 판단.
    private fun shareThemePickRankName(title: String, subtitle: String): String{

        val name = if (subtitle.isEmpty()) {
            title
        } else {
            "${title}_${subtitle}"
        }

        return name
    }

    @SuppressLint("SetTextI18n")
    private fun initSet() {
        //response json date값 때문에 선언해줌.
        dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(this))
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        mGlideRequestManager = Glide.with(this)

        mTheme = intent.getSerializableExtra(PARAM_THEME_MODEL) as ThemepickModel
        isShare = intent.getBooleanExtra(PARAM_THEME_IS_SHARE, false)

        //투표하고난후 랭킹리스트.
        mThemePickResultAdapter = ThemePickResultAdapter(
            this,
            mRankList,
            mGlideRequestManager,
            lifecycleScope,
            getIdolByIdUseCase,
            mTheme)
        binding.rvThemePickInner.adapter = mThemePickResultAdapter

        //개별 랭크 아이돌 리스트 가져오기(가장 맨처음 들어갈때는 투표여부는 False).
        loadRank(false,null)
        setActionBar()
        setBackPressed(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (intent.getBooleanExtra(MainActivity.IS_DEEP_LINK_CLICK_FROM_IDOL, false)) {
                    return
                }
                setResult(OnePickMainFragment.THEME_PICK_LIST_UPDATE_RESULT_CODE)
                finish()
            }
        })
        setVoteViewVisibility()
        setVoteButtonStatus()
    }

    //랭크 가져오는 API.
    private fun loadRank(isRefreshForVote: Boolean, idolId: Int?) {
        MainScope().launch {
            themepickRepository.getResult(
                id = mTheme.id,
                listener = { response ->
                    if (response.optBoolean("success")) {

                        val rankListJsonArray: JSONArray

                        try {

                            //intent로 받은걸 다시한번 업데이트 시켜줌(투표후하고 공유때문에 한번더 들어가야됨).
                            mTheme.alarm = response.getBoolean("alarm")
                            mTheme.status = response.getInt("status")
                            mTheme.vote = response.getString("vote")
                            mTheme.voteId = response.getInt("vote_id")
                            mTheme.title = response.getString("title")
                            mTheme.subtitle = response.getString("subtitle")
                            mTheme.count = response.getInt("count")
                            mTheme.beginAt = dateFormat.parse(response.getString("begin_at"))
                            mTheme.expiredAt = dateFormat.parse(response.getString("expired_at"))
                            mTheme.dummy = response.getString("dummy") //이미지를 불러줄때마다 dummy값을 넣어줌(dummy값을 계속 변경해줌으로써 캐싱방지).
                            mTheme.type = response.getString("type")

                            // 추가투표 후 바뀐 voteId 적용
                            mThemePickResultAdapter.setTheme(mTheme)

                            rankListJsonArray = response.getJSONArray("objects")
                            Logger.v("ThemeRank:: ${response}")

                            val rankListSize = rankListJsonArray.length()

                            mRankList.clear()

                            if (rankListSize > 0) {
                                binding.llThemePickEmptyWrapper.visibility = View.GONE
                                binding.rvThemePickInner.visibility = View.VISIBLE
                                binding.cgVote.visibility = View.VISIBLE

                                for (i in 0 until rankListSize) {
                                    val obj = rankListJsonArray.getJSONObject(i)
                                    val rank = IdolGson.getInstance(false).fromJson(obj.toString(), ThemepickRankModel::class.java)
                                    mRankList.add(rank)
                                }

                                //정렬 순서 투표많은순 -> 이름순.
                                mRankList.sortWith { o1, o2 ->
                                    when {
                                        (o1.vote > o2.vote) -> -1
                                        (o1.vote < o2.vote) -> 1
                                        else -> (o1.title ?: "").compareTo(o2.title ?: "")
                                    }
                                }

                                //동점자 처리.
                                var rank = 0
                                for (i in 0 until mRankList.size){
                                    when{
                                        i == 0 -> rank++
                                        mRankList[i-1].vote == mRankList[i].vote -> rank = mRankList[i-1].rank
                                        else -> rank = i + 1
                                    }
                                    mRankList[i].rank = rank
                                    mRankList[i].lastPlaceVote = mRankList.last().vote
                                    mRankList[i].firstPlaceVote = mRankList.find { it.rank == 1 }?.vote ?: 1
                                }


                                //투표가 진행되엇을때이다.
                                if(isRefreshForVote){

                                    //내가 투표한 아이돌
                                    val myModel = mRankList.find { idolId == it.id}
                                    //가수일땐 언더바 , 이외(노래)일때 하이픈
                                    var name: String? = if (mTheme.type == "I") {
                                        if (myModel?.subtitle.isNullOrEmpty()) {
                                            myModel?.title
                                        } else {
                                            "${myModel?.title}_${myModel?.subtitle}"
                                        }
                                    } else {
                                        if (myModel?.subtitle.isNullOrEmpty()) {
                                            myModel?.title
                                        } else {
                                            "${myModel?.title} - ${myModel?.subtitle}"
                                        }
                                    }

                                    //투표한 아이돌  다이얼로그  타이틀
                                    val votePopupTitle = if(myModel?.rank == 1) {
                                        String.format(this@ThemePickResultActivity.getString(R.string.vote_themepick_finished_first), name, this@ThemePickResultActivity.getString(R.string.title_share))
                                    } else {
                                        val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this@ThemePickResultActivity))
                                        //1등 까지 남은 수 ->  1등 vote수에서  내가 투표한  유저의 보트 수의 차를  보여줌. 혹시나 내가 투표한 유저의 vote가 null이면 0으로 처리
                                        val remainVoteFromTop = numberFormat.format(mRankList[0].vote - (myModel?.vote ?: 0))
                                        String.format(this@ThemePickResultActivity.getString(R.string.vote_themepick_finished),name, remainVoteFromTop, this@ThemePickResultActivity.getString(R.string.title_share))
                                    }

                                    Util.showImageDialogCancelRedBtn2(this@ThemePickResultActivity, votePopupTitle,
                                        "",
                                        this@ThemePickResultActivity.getString(R.string.title_share),
                                        R.string.title_share,
                                        R.string.button_close,
                                        R.drawable.img_themapick_vote,
                                        true,false,
                                        {
                                            Util.closeIdolDialog()
                                            shareThemePickRank()
                                        },
                                        {
                                            Util.closeIdolDialog()
                                        }
                                    )

                                }

                                setLastPlace(mRankList.last()) { minPercent ->
                                    mRankList.forEach { it.minPercent = minPercent }
                                    mThemePickResultAdapter.notifyDataSetChanged()
                                }

                                if(isShare){ //공유 버튼을 누르고 들어왔다는 뜻이므로 공유창을 띄워줍니다.
                                    shareThemePickRank()
                                }
                            } else {
                                binding.llThemePickEmptyWrapper.visibility = View.VISIBLE
                                binding.rvThemePickInner.visibility = View.GONE
                                binding.cgVote.visibility = View.GONE
                            }
                            binding.tvThemePickDataLoad.visibility = View.GONE

                            if (mTheme.status == OnePickStatus.DONE.code) {
                                binding.cgVote.visibility = View.GONE
                            }
                        } catch (e: Exception) {
                            Util.showIdolDialogWithBtn1(this@ThemePickResultActivity,
                                null,
                                getString(R.string.error_abnormal_exception)
                            ) {
                                Util.closeIdolDialog()
                                finish()
                            }
                        }
                    }
                },
                errorListener = { throwable ->
                    UtilK.showExceptionDialog(this@ThemePickResultActivity, throwable) {
                        finish()
                    }
                }
            )
        }
    }

    @SuppressLint("SetTextI18n")
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

        val title =
            if (BuildConfig.CELEB) getString(R.string.themepick_celeb) else getString(R.string.themepick)
        customActionView.tvActionBarTitle.text =
            if (mTheme.status == ThemepickModel.STATUS_FINISHED) {
                "$title ${getString(R.string.lable_final_result)}"
            } else {
                title
            }


        val helpInfoModel =
            Util.getPreference(this, Const.PREF_HELP_INFO).getModelFromPref<HelpInfosModel>()
        customActionView.ivActionBarInfo.setOnClickListener {
            DefaultDialog(
                title = if (BuildConfig.CELEB) getString(R.string.popup_title_themepick_celeb) else getString(
                    R.string.popup_title_themepick
                ),
                subTitle = helpInfoModel?.themePick,
                context = this,
                theme = android.R.style.Theme_Translucent_NoTitleBar
            ).show()
        }
        supportActionBar?.customView = customActionView.root
    }

    private fun setLastPlace(
        themePickRankModel: ThemepickRankModel,
        successViewLayout: (Float) -> Unit
    ) =
        with(binding.inHeartPickLastPlaceRank.inGradientProgressBar) {
            progressBar.updateIsLastPlace(true)

            // 마지막 등수의 텍스트를 넣고.
            val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this@ThemePickResultActivity))
            tvVote.text = numberFormat.format(themePickRankModel.lastPlaceVote)

            // 화면에 그려지면 가로 비율을 가져와줍니다.
            progressBar.setOnLayoutCompleteListener { percentWidth ->
                successViewLayout(percentWidth)
            }
        }

    private fun setVoteViewVisibility() = with(binding) {
        cgVote.visibility = when(mTheme.status) {
            OnePickStatus.DOING.code -> {
                View.VISIBLE
            }
            else -> {
                View.GONE
            }
        }
    }

    private fun setVoteButtonStatus() = with(binding) {
        when(mTheme.vote) {
            OnePickVoteStatus.ABLE.code -> {
                clThemePickDoingVoteParticipation.setBackgroundResource(R.drawable.bg_square_active_btn)
                tvThemePickDoingVoteParticipation.text =
                    (this@ThemePickResultActivity).getString(R.string.guide_vote_title)
            }
            OnePickVoteStatus.SEE_VIDEOAD.code -> {
                // 광고 시청 후 추가 투표로 변경
                clThemePickDoingVoteParticipation.setBackgroundResource(R.drawable.bg_square_active_btn)
                tvThemePickDoingVoteParticipation.text =
                    getString(R.string.themepick_vote_again)
            }
            OnePickVoteStatus.IMPOSSIBLE.code -> {
                clThemePickDoingVoteParticipation.setBackgroundResource(R.drawable.bg_square_result_btn)
                tvThemePickDoingVoteParticipation.setTextColor(ContextCompat.getColor((this@ThemePickResultActivity), R.color.fix_white))
                tvThemePickDoingVoteParticipation.text =
                    (this@ThemePickResultActivity).getString(R.string.themepick_today_voted)
            }
        }

        clThemePickDoingVoteParticipation.setOnClickListener {
            when(mTheme.vote) {
                OnePickVoteStatus.ABLE.code -> {
                    goToRank()
                }
                OnePickVoteStatus.SEE_VIDEOAD.code -> {
                    goToRank()
                }
                OnePickVoteStatus.IMPOSSIBLE.code -> {
                    return@setOnClickListener
                }
            }
        }
    }

    private fun goToRank() {
        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.THEME_PICK_RESULT_VOTE.actionValue,
            GaAction.THEME_PICK_RESULT_VOTE.label
        )

        startActivityForResultLauncher.launch(
            ThemePickRankActivity.createIntent(
                this@ThemePickResultActivity,
                mTheme
            ),
        )
    }

    private fun goToStore() {
        startActivityForResultLauncher.launch(
            NewHeartPlusActivity.createIntent(
                this@ThemePickResultActivity,
                NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP
            )
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        isShare = intent.getBooleanExtra(PARAM_THEME_IS_SHARE, false)
        val themeModel = intent.getSerializableData<ThemepickModel>(PARAM_THEME_MODEL)
        themeModel?.let { mTheme = it }
        loadRank(false, null)
        setVoteViewVisibility()
        setVoteButtonStatus()
    }

    companion object{

        const val PARAM_THEME_MODEL = "theme_model"
        const val PARAM_THEME_IS_SHARE = "theme_is_share"

        @JvmStatic
        fun createIntent(context: Context, theme: ThemepickModel, isShare: Boolean): Intent{
            val intent = Intent(context, ThemePickResultActivity::class.java)
            intent.putExtra(PARAM_THEME_MODEL, theme)
            intent.putExtra(PARAM_THEME_IS_SHARE, isShare)
            return intent
        }
    }

}