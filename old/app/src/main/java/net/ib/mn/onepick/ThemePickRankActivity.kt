package net.ib.mn.onepick

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.*
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.HeartPickPrelaunchActivity
import net.ib.mn.activity.MainActivity
import net.ib.mn.activity.MezzoPlayerActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.ThemepickRepositoryImpl
import net.ib.mn.core.model.HelpInfosModel
import net.ib.mn.databinding.ActionBarTitleAndImageBinding
import net.ib.mn.databinding.ActivityThemePickBinding
import net.ib.mn.dialog.DefaultDialog
import net.ib.mn.dialog.NotificationSettingDialog
import net.ib.mn.dialog.VoteNotifyToastFragment
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.ThemepickModel
import net.ib.mn.model.ThemepickRankModel
import net.ib.mn.onepick.OnePickMainFragment.Companion.THEME_PICK_LIST_UPDATE_RESULT_CODE
import net.ib.mn.onepick.viewholder.themepick.OnePickVoteStatus
import net.ib.mn.utils.*
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.livedata.Event
import org.json.JSONArray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.abs

/**
 * 테마픽 투표 화면
 */

@AndroidEntryPoint
class ThemePickRankActivity : BaseActivity(),
    ThemepickViewPagerAdapter.onItemClickListener {

    //현재 뷰페이저 포지션.
    private var mCurrentRankerPosition: Int = 0

    //현재 선택된 후보.
    private var mSelectedCadidate: ThemepickRankModel? = null

    private lateinit var mThemepickViewpagerAdapter: ThemepickViewPagerAdapter

    //랭킹 리스트.
    private var mRankList = ArrayList<ThemepickRankModel>()

    private lateinit var mGlideRequestManager: RequestManager

    //테마픽 모델.
    private lateinit var mTheme: ThemepickModel
    //날짜 계산.
    private lateinit var dateFormat: SimpleDateFormat

    private var isVote = false
    private lateinit var binding: ActivityThemePickBinding
    private var isPrelaunch = false
    private var comebackFromSetting = false

    @Inject
    lateinit var themepickRepository: ThemepickRepositoryImpl

    //뷰페이저 간격 설정.
    var pageMarginPx: Float = 0.0f
    var pagerWidth: Float = 0.0f
    var screenWidth: Float = 0.0f
    var offsetPx: Float = 0.0f

    val videoAdLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Util.closeProgress()
        if (result.resultCode == RESULT_CANCELED) {
            Util.handleVideoAdResult(
                this, false, false, MEZZO_PLAYER_REQ_CODE, result.resultCode, null, ""
            ) {
            }
            return@registerForActivityResult
        }
        // NPE 방지
        mSelectedCadidate?.let {
            if(::mTheme.isInitialized) {
                voteThemePick(mTheme.id, it.id)
            }
        }
    }

    private var startActivityForResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                THEME_PICK_LIST_UPDATE_RESULT_CODE -> {
                    if (result.resultCode == THEME_PICK_LIST_UPDATE_RESULT_CODE) {
                        setResult(THEME_PICK_LIST_UPDATE_RESULT_CODE)
                        finish()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_theme_pick)
        binding.clContainer.applySystemBarInsets()

        setActionBar()
        FLAG_CLOSE_DIALOG = false//안해주면 다이얼로그가 자동으로 닫힘(BaseActivity에있는거 다시 바꿔줌).

        mTheme = if (savedInstanceState != null) {
            // 복원
            @Suppress("UNCHECKED_CAST")
            savedInstanceState.getSerializable(PARAM_THEME_MODEL) as ThemepickModel
        } else {
            intent.getSerializableExtra(PARAM_THEME_MODEL)?.let { it as ThemepickModel }
                ?: throw IllegalStateException("ThemepickModel is required")
        }

        initSet()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(PARAM_THEME_MODEL, mTheme)
    }

    override fun onResume() {
        super.onResume()

        if (comebackFromSetting) {
            comebackFromSetting = false

            if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                postThemePickSettingNotification()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (isPrelaunch) {
            menuInflater.inflate(R.menu.share_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.btn_share -> {
                shareThemePickRank()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        FLAG_CLOSE_DIALOG = false   //안해주면 투표 후 앱 백그라운드에 둘 경우 팝업 사라짐(재투표 가능 방지)
    }

    @SuppressLint("SetTextI18n")
    private fun initSet() {
        if(BuildConfig.CELEB) with(binding) {
            nsvThemePick.visibility = View.VISIBLE
            clThemePickInnerFrame.visibility = View.VISIBLE
            clThemePickInnerVote.visibility = View.VISIBLE
            view.visibility = View.VISIBLE
            tvThemePickDataLoad.visibility = View.GONE
        }
        dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(this))
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        mGlideRequestManager = Glide.with(this)

        isPrelaunch = intent.getBooleanExtra(PARAM_IS_PRELAUNCH, false)

        if (mTheme.status == 0) isPrelaunch = true

        // 추가 투표라면
        if(mTheme.vote == OnePickVoteStatus.SEE_VIDEOAD.code) {
            binding.btnThemePickInnerVote.text = getString(R.string.themepick_vote_with_ad)
        }
        //투표하기 버튼 눌렀을떄.
        binding.btnThemePickInnerVote.setOnClickListener {
            if(mSelectedCadidate != null){
                if (mTheme.status == ThemepickModel.STATUS_PROGRESS) {//진행중.
                    if (mTheme.vote == OnePickVoteStatus.IMPOSSIBLE.code || isVote) { //투표했을떄.
                        Util.showDefaultIdolDialogWithBtn1(this@ThemePickRankActivity,
                            null,
                            this@ThemePickRankActivity.getString(R.string.themepick_already_vote)) {
                            Util.closeIdolDialog()
                        }
                    } else { //투표안했을떄.
                        Util.showProgress(this@ThemePickRankActivity)
                        Util.closeIdolDialog()

                        // 비광보고 투표하기라면
                        if( mTheme.vote == OnePickVoteStatus.SEE_VIDEOAD.code) {
                            // 비광 시청 후 투표하기
                            showVideoAd()
                        } else {
                            // 투표하기
                            voteThemePick(mTheme.id, mSelectedCadidate!!.id)
                        }
                    }
                } else if (mTheme.status == ThemepickModel.STATUS_FINISHED) { //투표종료(투표 버튼이 안보일텐데 혹시모르니까 넣어둠).
                    Util.showDefaultIdolDialogWithBtn1(this@ThemePickRankActivity,
                        null,
                        this@ThemePickRankActivity.getString(R.string.gaon_final_guide)) {
                        Util.closeIdolDialog()
                    }
                }
            }
        }

        //개별 랭크 아이돌 리스트 가져오기(가장 맨처음 들어갈때는 투표여부는 False).
        loadRank(false,null)
        setBackPressed(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (intent.getBooleanExtra(
                        MainActivity.IS_DEEP_LINK_CLICK_FROM_IDOL,
                        false
                    )
                ) { //앱 내부로 들어왔을때.
                    return
                }
                finish()
            }
        })
    }

    //뷰페이저 전체적인 설정해주는부분.
    private fun setViewPager(){
        //투표하기전 리스트(뷰페이저 세팅해주는 부분).
        mThemepickViewpagerAdapter = ThemepickViewPagerAdapter(
            this,
            mRankList,
            mGlideRequestManager,
            mTheme,
            isPrelaunch) { model: ThemepickRankModel, ivView: View, position: Int, ibView:View ->
            onItemClick(model, ivView, position, ibView)
        }
        binding.vpThemePickInner.adapter = mThemepickViewpagerAdapter

        if(mTheme.image_ratio == "S"){
            binding.vpThemePickInner.layoutParams.height = Util.getDeviceWidth(this) * 1 / 2
        }else{
            binding.vpThemePickInner.layoutParams.height = Util.getDeviceWidth(this) * 3 / 4
        }

        binding.vpThemePickInner.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.vpThemePickInner.offscreenPageLimit = 3
        binding.vpThemePickInner.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        //아이템 위치는 정가운데로 맞춰줍니다.
        val position = Integer.MAX_VALUE / 2
        val index = position % mRankList.size

        binding.vpThemePickInner.setCurrentItem(position, false)
        setViewPagerSideValue(position, index)

        //전체화면 크기 가져옴.
        screenWidth = resources.displayMetrics.widthPixels.toFloat()

        //뷰페이저 슬라이딩해서 선택되었을때 애니메이션 조정.
        var transform = CompositePageTransformer()

        val MIN_SCALE = 0.9f
        val MAX_SCALE = 1f

        //뷰페이저 애니메이메이션 및 페이지간 거리 설정.
        transform.addTransformer { page, position ->

            //offset 설정하려면 현재 뷰페이저에서 보여주고있는 이미지뷰를 가져와서 width를 계산해줌.
            if((binding.vpThemePickInner.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(mCurrentRankerPosition)!=null){
                val imageView = ((binding.vpThemePickInner.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(mCurrentRankerPosition)!!.itemView as ConstraintLayout).getChildAt(0)
                pageMarginPx = -((imageView.width.toFloat() / 1.7.toFloat() ))
                pagerWidth = imageView.width.toFloat()
                offsetPx = screenWidth - pageMarginPx - pagerWidth

                when {
                    position < -1 -> {
                        // [-Infinity,-1)
                        // This page is way off-screen to the left.
                    }
                    position < 0 -> {
                        page.translationX = -(offsetPx) * position
                        ViewCompat.setTranslationZ(page, position)
                        val scaleFactor = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (1 - abs(position))
                        page.scaleX = scaleFactor
                        page.scaleY = scaleFactor
                    }
                    position == 0.toFloat() -> {
                        page.translationX = 1.toFloat()
                        ViewCompat.setTranslationZ(page, 0F)
                        page.scaleX = MAX_SCALE
                        page.scaleY = MAX_SCALE
                    }
                    position <= 1.toFloat() -> {
                        ViewCompat.setTranslationZ(page, -position)
                        page.translationX =  -(offsetPx) * position
                        val scaleFactor = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (1 - abs(position))
                        page.scaleX = scaleFactor
                        page.scaleY = scaleFactor
                    }
                    else -> {
                        /// (1,+Infinity]
                        // This page is way off-screen to the right.
                    }
                }
            }
        }

        binding.vpThemePickInner.setPageTransformer(transform)

        binding.vpThemePickInner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (mRankList.size > 0) {
                    val index = position % mRankList.size
                    setViewPagerSideValue(position, index)
                } else {
                    Logger.v("ThemeViewPager:: position 이건 0번째라 그럼 ${(position + 1)}")
                }
            }
        })

    }

    //뷰페이저 처음 시작시(혹은 페이지 넘길때) 가운데 페이지의 양쪽페이지 value지정.
    private fun setViewPagerSideValue(position: Int, index: Int) {
        mRankList[index].isSelected = true

        //페이지 돌릴때마다 현재 보여지는 페이지 이외에는 선택 안되게 해준다.
        if (index - 1 >= 0) { //현재 보여지는거에서 이전께 0이상이어야됨.
            mRankList[index - 1].isSelected = false
        } else { //그 이외에는 그냥 처리해준다.
            mRankList[mRankList.size - 1].isSelected = false
        }

        if (index + 1 < mRankList.size) { //다음 페이지가 리스트 사이즈를 넘으면 안됨.
            mRankList[index + 1].isSelected = false
        } else { //마지막 인덱스에서 0으로 넘어갈때.
            mRankList[0].isSelected = false
        }

        mCurrentRankerPosition = position

        runOnUiThread {
            binding.tvThemePickInnerRankName.text = mRankList[index].title
            binding.tvThemePickInnerRankNameSub.text = mRankList[index].subtitle
            mThemepickViewpagerAdapter.notifyItemRangeChanged(position - 1, 3)
        }
    }

    //상단 헤더뷰 세팅.
    @SuppressLint("SetTextI18n")
    private fun initHeaderView() = with(binding){

        //상단 이미지.
        mGlideRequestManager
            .load(mTheme.imageUrl)
            .transform(CenterCrop(), RoundedCorners(26))
            .into(ivThemePickInner)

        //빨간색 제목.
        tvThemePickInnerHeadTitle.text = mTheme.title

        accordionMenu.setUI(
            title = mTheme.prize?.name,
            subtitle = mTheme.prize?.location,
            imageUrl = mTheme.prize?.image_url
        )

        //글자 중앙정렬했을때 마지막글자 공백때문에 아이콘과 글자사이에 공백생김.
        UtilK.replaceEmptyToLF(tvThemePickInnerHeadTitle)
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
                            mTheme.image_ratio = response.getString("image_ratio")

                            rankListJsonArray = response.getJSONArray("objects")
                            Logger.v("ThemeRank:: ${response}")

                            val rankListSize = rankListJsonArray.length()

                            mRankList.clear()

                            if (rankListSize > 0) {
                                binding.llThemePickEmptyWrapper.visibility = View.GONE
                                binding.clThemePickInnerFrame.visibility = View.VISIBLE
                                binding.clThemePickAll.visibility = View.VISIBLE
                                binding.clThemePickInnerTop.visibility = View.VISIBLE
                                binding.clThemePickInnerVote.visibility = View.VISIBLE
                                binding.view.visibility = View.VISIBLE

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
                                        String.format(this@ThemePickRankActivity.getString(R.string.vote_themepick_finished_first), name, this@ThemePickRankActivity.getString(R.string.title_share))
                                    } else {
                                        val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this@ThemePickRankActivity))
                                        //1등 까지 남은 수 ->  1등 vote수에서  내가 투표한  유저의 보트 수의 차를  보여줌. 혹시나 내가 투표한 유저의 vote가 null이면 0으로 처리
                                        val remainVoteFromTop = numberFormat.format(mRankList[0].vote - (myModel?.vote ?: 0))
                                        String.format(this@ThemePickRankActivity.getString(R.string.vote_themepick_finished), name, remainVoteFromTop, this@ThemePickRankActivity.getString(R.string.title_share))
                                    }

                                    Util.showImageDialogCancelRedBtn2(this@ThemePickRankActivity, votePopupTitle,
                                        "",
                                        this@ThemePickRankActivity.getString(R.string.title_share),
                                        R.string.title_share,
                                        R.string.button_close,
                                        R.drawable.img_themapick_vote,
                                        true, false,
                                        {
                                            Util.closeIdolDialog()
                                            //투표가 완료후 결과화면으로 이동.
                                            startActivityForResultLauncher.launch(
                                                ThemePickResultActivity.createIntent(this@ThemePickRankActivity, mTheme, true)
                                            )
                                        },
                                        {
                                            Util.closeIdolDialog()
                                            startActivityForResultLauncher.launch(
                                                ThemePickResultActivity.createIntent(this@ThemePickRankActivity, mTheme, false)
                                            )
                                        }
                                    )

                                } else {
                                    //정렬 순서 랜덤으로.
                                    mRankList.shuffle()

                                    //헤더뷰 세팅.
                                    initHeaderView()

                                    //데이터 다가져온다음 뷰페이저 세팅.
                                    setViewPager()

                                    //초기 후보이름 및 서브이름 Orientation설정.
                                    val params = LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.WRAP_CONTENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT)
                                    val marginValue = Util.convertDpToPixel(this@ThemePickRankActivity, 3f).toInt()

                                    if(mTheme.type == "I"){
                                        params.setMargins(marginValue,0,0,0)
                                        binding.tvThemePickInnerRankNameSub.layoutParams = params
                                        binding.liThemePickPickInner.orientation = LinearLayoutCompat.HORIZONTAL
                                    } else {
                                        params.setMargins(0,0,0,0)
                                        binding.tvThemePickInnerRankNameSub.layoutParams = params
                                        binding.liThemePickPickInner.orientation = LinearLayoutCompat.VERTICAL
                                    }
                                }

                                //가이드 화면 보여줌 여부 판단.
                                if(!Util.getPreferenceBool(this@ThemePickRankActivity, Const.THEME_PICK_GUIDE, false)){
                                    if (!isRefreshForVote) {
                                        //어느정도 딜레이를 안주면 position찾을떄 null Exception뜸.
                                        binding.vpThemePickInner.postDelayed({

                                            if ((binding.vpThemePickInner.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(mCurrentRankerPosition) != null) {
                                                //현재보여지고있는 이미지뷰를 가지고옵니다(getChildAt으로 하위의 가장 첫번째 뷰를 가져옴).
                                                val rankerImage = ((binding.vpThemePickInner.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(mCurrentRankerPosition)!!.itemView as ConstraintLayout).getChildAt(0)

                                                Util.showThemeGuide(this@ThemePickRankActivity, rankerImage, binding.nsvThemePick!!.height , binding.clThemePickInnerTop.height, isPrelaunch, {
                                                    Util.setPreference(this@ThemePickRankActivity , Const.THEME_PICK_GUIDE, true)
                                                    Util.closeIdolDialog()
                                                }) {
                                                    Util.setPreference(this@ThemePickRankActivity , Const.THEME_PICK_GUIDE, true)
                                                    Util.closeIdolDialog()
                                                }
                                            }
                                        }, 50)
                                    }
                                }

                            } else {
                                binding.clThemePickAll?.visibility = View.GONE
                                binding.llThemePickEmptyWrapper.visibility = View.VISIBLE
                                binding.clThemePickInnerTop.visibility = View.GONE
                                binding.clThemePickInnerFrame.visibility = View.GONE
                                binding.clThemePickInnerVote.visibility = View.GONE
                                binding.view.visibility = View.GONE
                            }
                            binding.tvThemePickDataLoad.visibility = View.GONE

                            if (isPrelaunch) {

                                binding.tvSetNotification.apply {
                                    if (mTheme.alarm) {
                                        background = ContextCompat.getDrawable(context, R.drawable.bg_gray110_radius27)
                                        text = context.getString(R.string.vote_alert_after)
                                    } else {
                                        background = ContextCompat.getDrawable(context, R.drawable.bg_main_light_radius27)
                                        text = getString(R.string.vote_alert_before)
                                        setOnClickListener {
                                            setFirebaseUIAction(GaAction.THEME_PICK_PRELAUNCH)
                                            openNotificationSetting()
                                        }
                                    }

                                    visibility = View.VISIBLE
                                }
                                binding.clThemePickInnerVote.visibility = View.GONE
                                binding.view.visibility = View.GONE
                            }
                        } catch (e: Exception) {
                            UtilK.showExceptionDialog(
                                this@ThemePickRankActivity,
                                errorMsg = e.stackTraceToString()
                            ) {
                                finish()
                            }
                        }
                    } else {
                        val responseMsg = ErrorControl.parseError(this@ThemePickRankActivity, response)
                        Util.showDefaultIdolDialogWithBtn1(this@ThemePickRankActivity,
                            null,
                            responseMsg) {
                            Util.closeIdolDialog()
                            finish()
                        }
                    }
                },
                errorListener = { throwable ->
                    UtilK.showExceptionDialog(
                        this@ThemePickRankActivity,
                        throwable = throwable,
                    ) {
                        finish()
                    }
                }
            )
        }
    }

    //뷰페이저 안에있은 아이템 클릭시 동작.
    override fun onItemClick(model: ThemepickRankModel, ivView: View, position: Int, ibView:View) {

        if (mRankList.size > 0) {

            //이미지 눌렀을때 선택버튼도 토글 시켜줍니다.
            val imageButton = ibView as AppCompatImageView
            imageButton.isSelected = !imageButton.isSelected

            if (imageButton.isSelected) {//선택되었다면.
                binding.btnThemePickInnerVote.setBackgroundResource(if(BuildConfig.CELEB) R.drawable.bg_radius6_active_btn else R.drawable.bg_radius_brand500)
                binding.btnThemePickInnerVote.isEnabled = true
                mSelectedCadidate = model
            } else {
                binding.btnThemePickInnerVote.setBackgroundResource(R.drawable.bg_radius_gray300)
                binding.btnThemePickInnerVote.isEnabled = false
                mSelectedCadidate = null
            }

            //무한 페이지니까 나눈 나머지로 인데스계산.
            val index = position % mRankList.size

            //선택여부 토글.
            mRankList[index].isClicked = !mRankList[index].isClicked
            if (mRankList[index].isClicked) {
                //선택한거 이외엔 모두 해제해준다.
                for (i in 0 until mRankList.size) {
                    if (mRankList[i].id != mRankList[index].id) {
                        mRankList[i].isClicked = false
                    }
                }
            }

        } else {
            Logger.v("ThemeViewPagerClicked:: position 이건 0번째라 그럼 ${(position + 1)}")
        }
    }

    private fun showVideoAd() {
        val intent = MezzoPlayerActivity.createIntent(this, Const.ADMOB_REWARDED_VIDEO_ONEPICK_UNIT_ID)
        videoAdLauncher.launch(intent)
    }

    //투표 API.
    fun voteThemePick(id:Int, idolId:Int){
        val voteType = mTheme.vote

        MainScope().launch {
            themepickRepository.vote(
                id = id,
                idolId = idolId,
                voteType = voteType,
                listener = { response ->
                    Util.closeProgress()
                    if (response.optBoolean("success")) {
                        Handler().postDelayed({ //미러링 반영되게 0.5초정도 주기.
                            //투표 후 후보 , 버튼 둘다 리셋해줌.
                            loadRank(true, idolId)
                            isVote = true
                        }, 500)
                    } else {
                        val responseMsg = ErrorControl.parseError(this@ThemePickRankActivity, response)

                        Util.showDefaultIdolDialogWithBtn1(this@ThemePickRankActivity,
                            null,
                            responseMsg) {
                            Util.closeIdolDialog()
                        }
                    }
               },
                errorListener = { throwable ->
                    UtilK.showExceptionDialog(
                        this@ThemePickRankActivity,
                        throwable = throwable,
                    ) {
                        finish()
                    }
                }
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val customActionView: ActionBarTitleAndImageBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.action_bar_title_and_image,
            null,
            false
        )

        customActionView.tvActionBarTitle.text =
            if (BuildConfig.CELEB) getString(R.string.themepick_celeb) else getString(R.string.themepick)

        val helpInfoModel =
            Util.getPreference(this, Const.PREF_HELP_INFO).getModelFromPref<HelpInfosModel>()
        customActionView.ivActionBarInfo.setOnClickListener {
            DefaultDialog(
                title = getString(R.string.popup_title_themepick_celeb),
                subTitle = helpInfoModel?.themePick,
                context = this,
                theme = android.R.style.Theme_Translucent_NoTitleBar
            ).show()
        }
        supportActionBar?.customView = customActionView.root
    }

    private fun shareThemePickRank(){
        val most = IdolAccount.getAccount(this)?.most
        val nameSplit = most
            ?.getName()
            ?.split("_", limit = 2)
            ?: emptyList()

        val existMost = mRankList.firstOrNull { it.idolId == most?.getId() }
        val mostText = if (existMost != null && nameSplit.isNotEmpty()) {
            nameSplit.first()
        } else {
            getString(R.string.share_onepick_upcoming_nobias)
        }
        val groupText = if (existMost != null && nameSplit.size > 1) {
            nameSplit[1]
        } else {
            ""
        }

        // prize 이름이 null 이면 빈 문자열로 처리
        val prizeName = mTheme.prize?.name.orEmpty()

        // 딥링크 URL 생성
        val params = listOf(LinkStatus.THEMEPICK.status, mTheme.id.toString())
        val url = LinkUtil.getAppLinkUrl(
            context = this@ThemePickRankActivity,
            params = params,
            querys = null
        )

        // 공유 메시지 포맷팅 (trimNewlineWhiteSpace() 호출은 그대로)
        val msg = getString(
            R.string.share_themepick_upcoming,
            mTheme.title,
            prizeName,
            mostText,
            groupText,
            url
        ).trimNewlineWhiteSpace()
        UtilK.linkStart(this, url= "", msg = msg.trimNewlineWhiteSpace())

        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.THEME_PICK_PRELAUNCH_SHARE.actionValue,
            GaAction.THEME_PICK_PRELAUNCH_SHARE.label
        )
    }

    private fun openNotificationSetting() {
        val isNotificationOn =
            NotificationManagerCompat.from(this).areNotificationsEnabled()

        if (!isNotificationOn) {
            val dialog = NotificationSettingDialog() {
                comebackFromSetting = true
            }
            dialog.show(supportFragmentManager, "notification_setting_dialog")
        } else {
            postThemePickSettingNotification()
        }
    }

    private fun postThemePickSettingNotification() {
        lifecycleScope.launch {
            themepickRepository.postOpenThemePickNotification(
                id = mTheme.id,
                listener = { _ ->
                    VoteNotifyToastFragment().show(supportFragmentManager, "VoteNotifyToast")
                    binding.tvSetNotification.apply {
                        background = ContextCompat.getDrawable(context, R.drawable.bg_gray110_radius27)
                        text = context.getString(R.string.vote_alert_after)
                    }
                },
                errorListener = { throwable ->
                    val msg = throwable.message
                    Toast.makeText(this@ThemePickRankActivity, msg, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    companion object{
        const val PARAM_THEME_MODEL = "theme_model"
        const val PARAM_IS_PRELAUNCH = "is_prelaunch"

        @JvmStatic
        fun createIntent(context: Context, theme: ThemepickModel, isPrelaunch: Boolean = false): Intent{
            val intent = Intent(context, ThemePickRankActivity::class.java)
            intent.putExtra(PARAM_THEME_MODEL, theme)
            intent.putExtra(PARAM_IS_PRELAUNCH, isPrelaunch)

            return intent
        }
    }
}
