package net.ib.mn.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.FragmentManager
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.reflect.TypeToken
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BoardActivity
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.FaqWriteActivity
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.FreeboardActivity
import net.ib.mn.activity.FriendDeleteActivity
import net.ib.mn.activity.IdolQuizInfoActivity
import net.ib.mn.activity.IdolQuizMainActivity
import net.ib.mn.activity.IdolQuizRankingActivity
import net.ib.mn.activity.IdolQuizWriteActivity
import net.ib.mn.activity.MainActivity
import net.ib.mn.activity.StatusSettingActivity
import net.ib.mn.activity.WriteArticleActivity
import net.ib.mn.adapter.BottomSheetAdItemAdapter
import net.ib.mn.adapter.BottomSheetSmallTalkLocaleAdapter
import net.ib.mn.adapter.BottomSheetTagItemAdapter
import net.ib.mn.adapter.CommunityPagerAdapter
import net.ib.mn.adapter.BottomSheetQuizRankingFilterAdapter
import net.ib.mn.adapter.quiz.BottomSheetQuizAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.chatting.ChattingCreateActivity
import net.ib.mn.chatting.ChattingCreateActivity.Companion.CHAT_ANONYMOUS
import net.ib.mn.chatting.ChattingCreateActivity.Companion.CHAT_OPEN_COMMUNITY
import net.ib.mn.chatting.ChattingCreateActivity.Companion.CHAT_OPEN_MOST
import net.ib.mn.chatting.ChattingCreateActivity.Companion.CHAT_OPEN_NICKNAME
import net.ib.mn.chatting.ChattingRoomListFragment
import net.ib.mn.chatting.ChattingRoomListFragment.Companion.TYPE_CHAT_ROOM_LIST_FILTER_MANY_TALK
import net.ib.mn.chatting.ChattingRoomListFragment.Companion.TYPE_CHAT_ROOM_LIST_FILTER_RECENT
import net.ib.mn.liveStreaming.LiveStreamingActivity
import net.ib.mn.model.LiveResolutionModel
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.StoreItemModel
import net.ib.mn.smalltalk.SmallTalkFragment
import net.ib.mn.smalltalk.viewholder.SmallTalkHeaderVH
import net.ib.mn.support.SupportMainActivity
import net.ib.mn.support.SupportWriteActivity
import net.ib.mn.utils.Const
import com.bumptech.glide.Glide
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.core.model.TagModel
import net.ib.mn.databinding.ItemLiveResolutionBinding
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.onepick.AlternateLinkFragmentActivity
import net.ib.mn.utils.BoardLanguage
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.setTop1TypeFilter
import net.ib.mn.utils.setTypeFilter
import net.ib.mn.utils.sethighestvoteTypeFilter
import net.ib.mn.viewholder.CommunityHeaderViewHolder


class BottomSheetFragment : BottomSheetDialogFragment() {

    lateinit var storeItemModel:StoreItemModel
    private var mGlideRequestManager: RequestManager? = null
    companion object {
        val FLAG_COMMUNITY_FILTER = R.layout.bottom_sheet_community_filter
        val FLAG_LANGUAGE_SETTING = R.layout.bottom_sheet_langauge_setting
        val FLAG_SCHEDULE_LANGUAGE_SETTING = "schedule"
        val FLAG_BOARD_FILTER = R.layout.bottom_sheet_board_filter
        val FLAG_BOARD_TAG = R.layout.bottom_sheet_board_tag
        val FLAG_PHOTO_RATIO = R.layout.bottom_sheet_photo_ratio
        val FLAG_HALL_OF_FAME_FILTER = R.layout.bottom_sheet_hall_of_fame
        val FLAG_FRIEND_DELETE_FILTER = R.layout.bottom_sheet_friend_delete_filter
        val FLAG_DARKMODE_SETTING = R.layout.bottom_sheet_darkmode_setting
        val FLAG_SUPPORT_ADCHOICE_SETTING = R.layout.bottom_sheet_support_ad_choice
        val FLAG_SUPPORT_MAIN_FILTER_FIRST = R.layout.bottom_sheet_support_main_first_filter
        val FLAG_SUPPORT_MAIN_FILTER_SECOND = R.layout.bottom_sheet_support_main_second_filter
        val FLAG_SUPPORT_MAIN_FILTER_SECOND_CERTIFY = R.layout.bottom_sheet_support_main_second_filter_certify
        val FLAG_FEED_REPORT = R.layout.bottom_sheet_feed_report
        val FLAG_CHAT_OPEN_STATUS = R.layout.bottom_sheet_chat_open_status
        val FLAG_CHAT_ANONYMOUS_STATUS = R.layout.bottom_sheet_chat_anonymous_status
        val FLAG_CHAT_LEVEL_LIMIT = R.layout.bottom_sheet_chat_level
        val FLAG_CHAT_ROOM_LIST_FILTER = R.layout.bottom_sheet_chat_room_list_filter
        val FLAG_HISTORY = R.layout.bottom_sheet_history
        val FLAG_LIVE_STREAMING = R.layout.bottom_sheet_live_streaming
        val FLAG_LIVE_STREAMING_RESOLUTION = R.layout.bottom_sheet_live_streaming_resolution
        val FLAG_QUIZ_INFO = R.layout.bottom_sheet_quiz_info
        val FLAG_FAQ_FILTER = R.layout.bottom_sheet_faq_filter
        val FLAG_MEDIA_FILTER = R.layout.bottom_sheet_media_filter
        val FLAG_QUIZ_ANSWER = R.layout.bottom_sheet_quiz_answer
        val FLAG_QUIZ_DIFFICULTY = R.layout.bottom_sheet_quiz_difficulty
        val FLAG_QUIZ_RANKING_FILTER = R.layout.bottom_sheet_quiz_ranking_filter
        val FLAG_QUIZ_MAIN_FILTER = R.layout.bottom_sheet_quiz_main_filter

        val FLAG_ARTICLE_WRITE_CATEGORY = R.layout.bottom_sheet_article_write_category
        val FALG_SMALL_TALK_FILTER = R.layout.bottom_sheet_small_talk_filter
        val FLAG_SMALL_TALK_LANGUAGE_FILTER = R.layout.bottom_sheet_small_talk_locale
        // CELEB ---
        val FLAG_HALL_OF_FAME_TYPE_FILTER = R.layout.bottom_sheet_type
        val FLAG_TOP1_TYPE_FILTER = R.layout.bottom_sheet_type_top1
        val FLAG_HIGHEST_VOTE_TYPE_FILTER = R.layout.bottom_sheet_type_highestvote

        // default constructor를 사용하도록 하고 newInstance(...) 형식으로 인스턴스 생성하도록 해야
        // "could not find fragment constructor" 에러가 발생하지 않는다.
        @JvmStatic
        fun newInstance(resId: Int) : BottomSheetFragment {
            val f = BottomSheetFragment()
            f.resId = resId
            return f
        }

        // china version
        //결제 방법  고를떄   storeitem  model 전달하기 위해
        //newInstance storemodel  받는걸로  오버로딩
        @JvmStatic
        fun newInstance(resId: Int,storeItemModel: StoreItemModel) : BottomSheetFragment {
            var f = BottomSheetFragment()
            f.resId = resId
            f.storeItemModel = storeItemModel
            return f
        }

        @JvmStatic
        fun newInstance(resId: Int,resolutionList:ArrayList<LiveResolutionModel>) : BottomSheetFragment {
            val f = BottomSheetFragment()
            f.resId = resId
            f.resolutionList = resolutionList
            return f
        }


        @JvmStatic
        fun newInstance(resId: Int, loaderId: Int): BottomSheetFragment {
            val f = BottomSheetFragment()
            f.resId = resId
            f.loaderId = loaderId
            return f
        }

        @JvmStatic
        fun newInstance(resId: Int, isJoinedRoom: Boolean): BottomSheetFragment {
            val f = BottomSheetFragment()
            f.resId = resId
            f.isJoinedRoom=isJoinedRoom
            return f
        }


        @JvmStatic
        fun newInstance(resId: Int, flag: String) : BottomSheetFragment {
            val f = BottomSheetFragment()
            f.resId = resId
            f.flag = flag
            return f
        }

        @JvmStatic
        fun newInstance(resId: Int, charityImage: String, charityUrl: String) : BottomSheetFragment{
            val f = BottomSheetFragment()
            f.resId = resId
            f.charityImage = charityImage
            f.charityUrl = charityUrl
            return f
        }

        @JvmStatic
        fun newInstance(
            resId: Int,
            showEdit: Boolean,
            showRemove: Boolean,
            showReport: Boolean
        ): BottomSheetFragment {
            val f = BottomSheetFragment()
            f.resId = resId
            f.showEdit = showEdit
            f.showRemove = showRemove
            f.showReport = showReport
            return f
        }
    }
    val KEY_RESID = "resid"
    val KEY_FLAG = "flag"

    var resId : Int = 0
    var loaderId: Int = 0
    var userLevel :Int =0
    var flag: String = ""
    var isJoinedRoom:Boolean = false
    var charityImage : String = ""
    var charityUrl : String = ""
    var resolutionList= ArrayList<LiveResolutionModel>()
    var articleModel: ArticleModel = ArticleModel()
    var position: Int = 0
    var showEdit: Boolean = false
    var showRemove: Boolean = false
    var showReport: Boolean = false
    val TAG_HOF = "hof" // CELEB MainActivity.TAG_HOF

//    constructor(resId: Int, flag: String) : this(resId) {
//        this.flag = flag
//    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_RESID, resId)
        outState.putString(KEY_FLAG, flag)
    }

    override fun onStart() {
        super.onStart()
        if (resId == FLAG_SMALL_TALK_LANGUAGE_FILTER || ((resId == FLAG_QUIZ_RANKING_FILTER || resId == FLAG_QUIZ_MAIN_FILTER) && !BuildConfig.CELEB)) {
            dialog?.let { d ->
                val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.let { sheet ->
                    val layoutParams = sheet.layoutParams
                    layoutParams.height = (resources.displayMetrics.heightPixels * 0.5).toInt() // 최대 높이 설정
                    sheet.layoutParams = layoutParams
                    BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(
        requireContext(),
        theme
    ).apply {
        //landscape 모드에서  bottomsheet 메뉴모두  expand되지 않아서  아래 설정값 넣어줌.
        this.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mGlideRequestManager = Glide.with(this)
        if( savedInstanceState != null ) {
            resId = savedInstanceState.getInt(KEY_RESID)
            flag = savedInstanceState.getString(KEY_FLAG, "")
        }
        val view = inflater.inflate(resId, container, false)

        when (resId) {
            FLAG_COMMUNITY_FILTER -> setCommunityFilter(view)
            FLAG_LANGUAGE_SETTING -> setScheduleLanguageSetting(view)
            FLAG_BOARD_FILTER -> setBoardFilterButtons(view)
            FLAG_BOARD_TAG -> setBoardTag(view)
            FLAG_PHOTO_RATIO -> setPhotoRatio(view)
            FLAG_HALL_OF_FAME_FILTER -> setHallOfFameFilter(view)
            FLAG_FRIEND_DELETE_FILTER -> setFriendDeleteFilter(view)
            FLAG_DARKMODE_SETTING -> setDarkmodeSetting(view)
            FLAG_HALL_OF_FAME_TYPE_FILTER -> setTypeFilter(view)
            FLAG_TOP1_TYPE_FILTER -> setTop1TypeFilter(view)
            FLAG_HIGHEST_VOTE_TYPE_FILTER -> sethighestvoteTypeFilter(view)
            FLAG_SUPPORT_ADCHOICE_SETTING -> setAdChoiceSetting(view)
            FLAG_SUPPORT_MAIN_FILTER_FIRST -> setMainFirstFilter(view)
            FLAG_SUPPORT_MAIN_FILTER_SECOND -> setMainSecondFilter(view)
            FLAG_SUPPORT_MAIN_FILTER_SECOND_CERTIFY -> setMainSecondCertify(view)
            FLAG_FEED_REPORT -> setFeedReport(view)
            FLAG_CHAT_OPEN_STATUS -> setChattingOpenStatusFilter(view)
            FLAG_CHAT_ANONYMOUS_STATUS -> setChattingAnonymoustStatusFilter(view)
            FLAG_CHAT_LEVEL_LIMIT -> setChatLevelLimit(view)
            FLAG_CHAT_ROOM_LIST_FILTER -> setChattingRoomListFilter(view)
            FLAG_HISTORY -> setHistory(view)
            FLAG_LIVE_STREAMING ->setLiveStreamingBottomMenu(view)
            FLAG_LIVE_STREAMING_RESOLUTION ->setLiveResolution(view)
            FLAG_QUIZ_INFO -> setQuizInfo(view)
            FLAG_FAQ_FILTER -> setFaqFilter(view)
            FLAG_MEDIA_FILTER -> setMediaFilter(view)
            FLAG_QUIZ_ANSWER -> setQuizAnswer(view)
            FLAG_QUIZ_DIFFICULTY -> setQuizDifficulty(view)
            FLAG_QUIZ_RANKING_FILTER -> setQuizRankingFilter(view)
            FLAG_QUIZ_MAIN_FILTER -> setQuizMainFilter(view)
            FALG_SMALL_TALK_FILTER -> setSmallTalkFilterButtons(view)
            FLAG_SMALL_TALK_LANGUAGE_FILTER -> setSmallTalkFilterLanguage(view)
            else -> return super.onCreateView(inflater, container, savedInstanceState)
        }
        return view
    }

    private fun setQuizAnswer(v: View) {
        val idolQuizWriteActivity = context as IdolQuizWriteActivity
        val rvAnswer = v.findViewById<RecyclerView>(R.id.rv_answer)

        val answerList = listOf(1, 2, 3, 4)

        rvAnswer.adapter = BottomSheetQuizAdapter(
            answerList = answerList,
            type = BottomSheetQuizAdapter.BOTTOM_SHEET_ANSWER
        ).apply {
            this.setAnswerItemClickListener(object :
                BottomSheetQuizAdapter.OnAnswerItemClickListener {
                override fun onAnswerItemClick(position: Int) {
                    idolQuizWriteActivity.setAnswer(answerList[position])
                    this@BottomSheetFragment.dismiss()
                }
            })
        }
    }

    private fun setQuizDifficulty(v: View) {
        val idolQuizWriteActivity = context as IdolQuizWriteActivity
        val rvDifficulty = v.findViewById<RecyclerView>(R.id.rv_difficulty)

        val difficultyMap = mapOf(
            getString(R.string.quiz_write_difficulty_high) to 3,
            getString(R.string.quiz_write_difficulty_mid) to 2,
            getString(R.string.quiz_write_difficulty_low) to 1
        )

        rvDifficulty.adapter = BottomSheetQuizAdapter(
            difficultyList = difficultyMap.keys.toList(),
            type = BottomSheetQuizAdapter.BOTTOM_SHEET_DIFFICULTY
        ).apply {
            this.setDifficultyItemClickListener(object :
                BottomSheetQuizAdapter.OnDifficultyClickListener {
                override fun onDifficultyItemClick(difficulty: String, position: Int) {
                    idolQuizWriteActivity.setDifficulty(difficulty, difficultyMap[difficulty] ?: 1)
                    this@BottomSheetFragment.dismiss()
                }

            })
        }
    }

    private fun setQuizRankingFilter(v: View) {
        val idolQuizRankingActivity = context as? IdolQuizRankingActivity ?: return
        val rv = v.findViewById<RecyclerView>(R.id.rv_quiz_ranking_filter)
        val items = arguments?.getStringArrayList("items") ?: arrayListOf()

        rv.layoutManager = LinearLayoutManager(context)
        val adapter = BottomSheetQuizRankingFilterAdapter(items) { position ->
            idolQuizRankingActivity.onCategorySelected(position)
            dismiss()
        }
        rv.adapter = adapter
    }

    private fun setQuizMainFilter(v: View) {
        val idolQuizMainActivity = context as? IdolQuizMainActivity ?: return
        val rv = v.findViewById<RecyclerView>(R.id.rv_quiz_ranking_filter)
        val items = arguments?.getStringArrayList("items") ?: arrayListOf()

        rv.layoutManager = LinearLayoutManager(context)
        val adapter = BottomSheetQuizRankingFilterAdapter(items) { position ->
            idolQuizMainActivity.onCategorySelected(position)
            dismiss()
        }
        rv.adapter = adapter
    }

    private fun setMainSecondCertify(v: View) {
        val supportMainActivity = context as SupportMainActivity

        val latest = v.findViewById<TextView>(R.id.tv_order_latest)
        val dday = v.findViewById<TextView>(R.id.tv_order_dday)
        val name = v.findViewById<TextView>(R.id.tv_order_by_name)

        latest.setOnClickListener {
            supportMainActivity.filterByLatest()
            this.dismiss()
        }

        dday.setOnClickListener {
            supportMainActivity.filterByDeadLine()
            this.dismiss()
        }

        name.setOnClickListener {
            supportMainActivity.filterByName()
            this.dismiss()
        }
    }

    private fun setMainSecondFilter(v: View) {
        val supportMainActivity = context as SupportMainActivity

        val latest = v.findViewById<TextView>(R.id.tv_order_latest)
        val deadline = v.findViewById<TextView>(R.id.tv_order_deadline)
        val name = v.findViewById<TextView>(R.id.tv_order_by_name)
        val achievement = v.findViewById<TextView>(R.id.tv_order_achievement)

        latest.setOnClickListener {
            supportMainActivity.filterByLatest()
            this.dismiss()
        }

        achievement.setOnClickListener {
            supportMainActivity.filterByAchievement()
            this.dismiss()
        }

        deadline.setOnClickListener {
            supportMainActivity.filterByDeadLine()
            this.dismiss()
        }

        name.setOnClickListener {
            supportMainActivity.filterByName()
            this.dismiss()
        }


    }

    private fun setMainFirstFilter(v: View) {
        val supportMainActivity = context as SupportMainActivity
        val all = v.findViewById<TextView>(R.id.tv_order_all)
        val myFav = v.findViewById<TextView>(R.id.tv_order_myfav)

        if(BuildConfig.CELEB) {
            myFav.text = getString(R.string.actor_support_filter_favorites)
        }

        all.setOnClickListener {
            supportMainActivity.filterByAll()
            this.dismiss()
        }

        myFav.setOnClickListener {
            supportMainActivity.filterByMyFav()
            this.dismiss()
        }

    }

    private fun setAdChoiceSetting(v: View) {
        val activity = context as SupportWriteActivity

        val recyclerView = v.findViewById<RecyclerView>(R.id.rv_ad_choice)

        try {
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<List<SupportAdTypeListModel>>() {}.type
            val adList: List<SupportAdTypeListModel> =
                    gson.fromJson(Util.getPreference(context, Const.AD_TYPE_LIST), listType)

            recyclerView.adapter = BottomSheetAdItemAdapter(adList, activity)
            recyclerView.setHasFixedSize(true)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            super.show(manager, tag)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    private fun setCommunityFilter(v: View) {
        val tvOrderHeart = v.findViewById<AppCompatTextView>(R.id.tv_order_heart)
        val tvOrderLatest = v.findViewById<AppCompatTextView>(R.id.tv_order_latest)
        val tvOrderComments = v.findViewById<AppCompatTextView>(R.id.tv_order_comments)
        val tvOrderLike = v.findViewById<AppCompatTextView>(R.id.tv_order_likes)

        val activity = context as? CommunityActivity
        val fragment = activity?.communityFragment

        val viewPagerAdapter = activity?.binding?.viewPager2?.adapter as? CommunityPagerAdapter
        val indexCommunity = viewPagerAdapter?.getIndexOfFragments(fragment ?: return)

        val communityFragment =
            activity?.supportFragmentManager?.findFragmentByTag(
                "f$indexCommunity"
            ) as? CommunityFragment ?: return

        val communityHeaderViewHolder =
            communityFragment.binding.rvCommunity.findViewHolderForAdapterPosition(loaderId) as? CommunityHeaderViewHolder
                ?: return

        tvOrderHeart.setOnClickListener {
            communityHeaderViewHolder.filterByHeart()
            this.dismiss()
        }
        tvOrderLatest.setOnClickListener {
            communityHeaderViewHolder.filterByLatest()
            this.dismiss()
        }
        tvOrderComments.setOnClickListener {
            communityHeaderViewHolder.filterByComments()
            this.dismiss()
        }
        tvOrderLike.setOnClickListener {
            communityHeaderViewHolder.filterByLikes()
            this.dismiss()
        }
    }

    //해상도 설정  bottomsheet ,  메뉴 resolution 해상도
    @OptIn(UnstableApi::class)
    private fun setLiveResolution(view: View){
        val activity = context as LiveStreamingActivity
        val parentLayout = view.findViewById<LinearLayoutCompat>(R.id.ll_bottom_sheet_live_resolution_filter)
        val tvLiveAuto = view.findViewById<AppCompatTextView>(R.id.tv_live_resolution_auto)
        val cbLiveAuto = view.findViewById<AppCompatImageView>(R.id.cb_live_resolution_auto)


        try {

            //auto 해상도가 선택되어있는 경우에는 체크 박스 visible 처리 나머지는
            //gone 처리해서 해상도가 선택안된것 처럼 보이게 해줌.
            if(resolutionList[0].isSelected){
                cbLiveAuto.visibility = View.VISIBLE
            }else{
                cbLiveAuto.visibility = View.GONE
            }


            //auto 의 경우는  기본으로  제공한다.
            tvLiveAuto.setOnClickListener {
                try {
                    changeResolution(0)
                    activity.setPlayerResolution(changeResolution(0))
                }catch (e:Exception){
                    //지원 resolution이 하나도 없으면,  resoulution index가 0이므로, auto(기본 제공버튼) 클릭시  죽는다. 혹시나 해서 예외처리 넣음.
                    e.printStackTrace()
                }
                this.dismiss()
            }


            //받아온  해상도 사이즈 만큼  반복문을 돌려 메뉴에 동적뷰 추가를 한다. - auto는 기본으로 제공함으로 그 이후 index부터 적용
            for(i in 1 until resolutionList.size){
                parentLayout.addView(addResolutionMenu(activity,resolutionList[i],i))
            }

        }catch (e:Exception){
            e.printStackTrace()
        }

    }


    //해상도 메뉴 동적 추가
    @OptIn(UnstableApi::class)
    private fun addResolutionMenu(activity: Activity, resolution: LiveResolutionModel, index:Int) : View{
        val binding = ItemLiveResolutionBinding.inflate(LayoutInflater.from(activity), null, false)

        //선택된  해상도이면  체크박스 visible 아니면 gone 처리
        if(resolution.isSelected){
            binding.cbLiveResolution.visibility = View.VISIBLE
        }else{
            binding.cbLiveResolution.visibility = View.GONE
        }

        binding.tvLiveResolution.text = "${resolution.height} P"

        //해상도 메뉴 클릭시  동작
        binding.tvLiveResolution.setOnClickListener {
            //해당 resolution값을 넘겨준다.
            binding.cbLiveResolution.visibility = View.VISIBLE
            (activity as LiveStreamingActivity).setPlayerResolution(changeResolution(index))
            this.dismiss()
        }

        return binding.root
    }


    //선택한 해상도의  index를 받아서  해당
    //index에 해당 하는 해상도의 selected값을  true로 바꿔주고 나머지는 false처리
    private fun changeResolution(index: Int):ArrayList<LiveResolutionModel>{
        for(i in 0 until resolutionList.size){
            resolutionList[i].isSelected = i == index
        }
        return resolutionList
    }


    //라이브 스트리밍 해상도,공유하기
    @OptIn(UnstableApi::class)
    private fun setLiveStreamingBottomMenu(v: View){
        val activity = context as LiveStreamingActivity
        val tvLiveShare = v.findViewById<AppCompatTextView>(R.id.tv_live_share)
        val tvLiveResolution = v.findViewById<AppCompatTextView>(R.id.tv_live_resolution)

        //공유 버튼 클릭시
        tvLiveShare.setOnClickListener {
            activity.shareLive()
            this.dismiss()
        }

        //해상도 클릭시
        tvLiveResolution.setOnClickListener {
            activity.showResolutionChangeDialog()
            this.dismiss()
        }
    }

    private fun setHistory(v: View){
        val charityImage = v.findViewById<ImageView>(R.id.img_history)
        val charityUrl = v.findViewById<AppCompatTextView>(R.id.tv_history_url)
        val imgHistoryDown = v.findViewById<ImageView>(R.id.img_history_arrow_down)
        val articleModel = ArticleModel()
        articleModel.imageUrl = this.charityImage
        mGlideRequestManager?.load(this.charityImage)?.into(charityImage)
        charityUrl.text = this.charityUrl
        charityUrl.setOnClickListener {
            val intent = Intent(activity, AppLinkActivity::class.java).apply {
                data = Uri.parse(charityUrl.text.toString())
            }
            startActivity(intent)
        }
        imgHistoryDown.setOnClickListener{
            this.dismiss()
        }
        charityImage.setOnClickListener{
            WidePhotoFragment.getInstance(articleModel)
                    .show(requireActivity().supportFragmentManager, "wide_photo")
        }

    }

    private fun setScheduleLanguageSetting(v: View) {
        val activity = context as? CommunityActivity
        val fragment = activity?.scheduleFragment

        val viewPagerAdapter = activity?.binding?.viewPager2?.adapter as? CommunityPagerAdapter
        val indexSchedule = viewPagerAdapter?.getIndexOfFragments(fragment ?: return)

        val scheduleFragment =
            activity?.supportFragmentManager?.findFragmentByTag(
                "f$indexSchedule"
            ) as? ScheduleFragment ?: return

        val talkLanguages = ScheduleFragment.scheduleLanguages
        val talkLocales = ScheduleFragment.scheduleLocales

        val tvLanguageKorean = v.findViewById<AppCompatTextView>(R.id.tv_language_korean)
        val tvLanguageEnglish = v.findViewById<AppCompatTextView>(R.id.tv_language_english)
        val tvLanguageChinese = v.findViewById<AppCompatTextView>(R.id.tv_language_chinese)
        val tvLanguageJapanese = v.findViewById<AppCompatTextView>(R.id.tv_language_japanese)
        val tvLanguageIn = v.findViewById<AppCompatTextView>(R.id.tv_language_indonesia)
        val tvLanguageEs = v.findViewById<AppCompatTextView>(R.id.tv_language_es)
        val tvLanguagePt = v.findViewById<AppCompatTextView>(R.id.tv_language_pt)
        val tvLanguageVi = v.findViewById<AppCompatTextView>(R.id.tv_language_vi)
        val tvLanguageTh = v.findViewById<AppCompatTextView>(R.id.tv_language_th)

        tvLanguageKorean.setOnClickListener {
            scheduleFragment.setLanguage(talkLanguages[0], talkLocales[0])
            this.dismiss()
        }
        tvLanguageEnglish.setOnClickListener {
            scheduleFragment.setLanguage(talkLanguages[1], talkLocales[1])
            this.dismiss()
        }
        tvLanguageChinese.setOnClickListener {
            scheduleFragment.setLanguage(talkLanguages[2], talkLocales[2])
            this.dismiss()
        }
        tvLanguageJapanese.setOnClickListener {
            scheduleFragment.setLanguage(talkLanguages[3], talkLocales[3])
            this.dismiss()
        }
        tvLanguageIn.setOnClickListener {
            scheduleFragment.setLanguage(talkLanguages[4], talkLocales[4])
            this.dismiss()
        }
        tvLanguageEs.setOnClickListener {
            scheduleFragment.setLanguage(talkLanguages[6], talkLocales[6])
            this.dismiss()
        }
        if(!BuildConfig.CELEB) {
            // TODO: 2021/04/28 셀럽 스케쥴에서 아래 관련 언어들  빼기로하여
            tvLanguagePt.setOnClickListener {
                scheduleFragment.setLanguage(talkLanguages[5], talkLocales[5])
                this.dismiss()
            }
            tvLanguageVi.setOnClickListener {
                scheduleFragment.setLanguage(talkLanguages[7], talkLocales[7])
                this.dismiss()
            }
            tvLanguageTh.setOnClickListener {
                scheduleFragment.setLanguage(talkLanguages[8], talkLocales[8])
                this.dismiss()
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun setBoardFilterButtons(v: View) {
        val tvOrderLatest = v.findViewById<AppCompatTextView>(R.id.tv_order_latest)
        val tvOrderComments = v.findViewById<AppCompatTextView>(R.id.tv_order_comments)
        val tvOrderLike = v.findViewById<AppCompatTextView>(R.id.tv_order_likes)
        val tvOrderViewCount = v.findViewById<AppCompatTextView>(R.id.tv_order_hits)

        //셀럽은 나눠져있지 않으므로 Freeboard로
        if(!BuildConfig.CELEB) {
            val localeStart = LocaleUtil.getAppLocale(context ?: return).toString().split("_")

            when(localeStart[0]){
                //한,중,영,일일때
                "ko", "en", "ja", "zh" -> {
                    val activity = if (context is MainActivity) {
                        context as MainActivity
                    } else {
                        context as BoardActivity
                    }

                    tvOrderLatest.setOnClickListener {
                        activity.freeboardFragment?.filterByLatest()
                        this.dismiss()
                    }

                    tvOrderComments.setOnClickListener {
                        activity.freeboardFragment?.filterByComments()
                        this.dismiss()
                    }

                    tvOrderLike.setOnClickListener {
                        activity.freeboardFragment?.filterByLike()
                        this.dismiss()
                    }

                    tvOrderViewCount.setOnClickListener {
                        activity.freeboardFragment?.filterByViewCount()
                        this.dismiss()
                    }
                    return
                }
            }
        }

        val freeboardFragment = if( context is FreeboardActivity) {
            (context as FreeboardActivity).freeboardFragment
        } else if( context is BoardActivity) {
            (context as BoardActivity).freeboardFragment
        } else {
            (context as MainActivity).freeboardFragment
        }
        tvOrderLatest.setOnClickListener {
            freeboardFragment?.filterByLatest()
            this.dismiss()
        }

        tvOrderComments.setOnClickListener {
            freeboardFragment?.filterByComments()
            this.dismiss()
        }

        tvOrderLike.setOnClickListener {
            freeboardFragment?.filterByLike()
            this.dismiss()
        }

        tvOrderViewCount.setOnClickListener {
            freeboardFragment?.filterByViewCount()
            this.dismiss()
        }
    }

    private fun setSmallTalkFilterButtons(v: View) {
        val tvOrderLatest = v.findViewById<AppCompatTextView>(R.id.tv_order_latest)
        val tvOrderPopular = v.findViewById<AppCompatTextView>(R.id.tv_order_popular)
        val tvOrderHits = v.findViewById<AppCompatTextView>(R.id.tv_order_hits)
        val tvOrderComments = v.findViewById<AppCompatTextView>(R.id.tv_order_comments)

        val activity = context as? CommunityActivity
        val fragment = activity?.smallTalkFragment

        val viewPagerAdapter = activity?.binding?.viewPager2?.adapter as? CommunityPagerAdapter
        val indexSmallTalk = viewPagerAdapter?.getIndexOfFragments(fragment ?: return)

        val smallTalkFragment =
            activity?.supportFragmentManager?.findFragmentByTag(
                "f$indexSmallTalk"
            ) as? SmallTalkFragment ?: return

        val smallTalkHeaderVH =
            smallTalkFragment.binding.rvSmallTalk.findViewHolderForAdapterPosition(0) as? SmallTalkHeaderVH ?: return

        tvOrderLatest.setOnClickListener {
            smallTalkHeaderVH.filterByDate()
            this.dismiss()
        }

        tvOrderPopular.setOnClickListener {
            smallTalkHeaderVH.filterByLike()
            this.dismiss()
        }

        tvOrderHits.setOnClickListener {
            smallTalkHeaderVH.filterByHits()
            this.dismiss()
        }

        tvOrderComments.setOnClickListener {
            smallTalkHeaderVH.filterByComments()
            this.dismiss()
        }
    }

    private fun setBoardTag(v: View) {
        val activity = context as WriteArticleActivity
        val rvTag = v.findViewById<RecyclerView>(R.id.rv_tag)

        try {
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<List<TagModel>>() {}.type
            val tags: ArrayList<TagModel> =
                    gson.fromJson(Util.getPreference(context, Const.BOARD_TAGS), listType)

            //관리자계정아아니면 관리자태그는 없애준다.
            //removeIf는 24이상부터 지원됨.
            val account = IdolAccount.getAccount(requireActivity())
            tags.removeIf { it.adminOnly == "Y" && account?.heart != 30 }
            tags.removeIf { it.id == 999 && (account?.most == null || account.most?.getId() == Const.NON_FAVORITE_IDOL_ID) }

            rvTag.adapter = BottomSheetTagItemAdapter(tags, activity)
            rvTag.setHasFixedSize(true)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    private fun setPhotoRatio(v: View) {
        val activity = context as WriteArticleActivity

        val tvOptionSquare = v.findViewById<AppCompatTextView>(R.id.tv_option_square)
        val tvOptionFree = v.findViewById<AppCompatTextView>(R.id.tv_option_free)

        tvOptionSquare.setOnClickListener {
            activity.setRatioSquare()
            this.dismiss()
        }
        tvOptionFree.setOnClickListener {
            activity.setRatioFree()
            this.dismiss()
        }
    }

    private fun setHallOfFameFilter(v: View) {
        val activity = context as BaseActivity
        // "hof": CELEB MainActivity의 TAG_HOF
        val tag = if (BuildConfig.CELEB) {
            if (activity is MainActivity) {
                TAG_HOF
            } else {
                AlternateLinkFragmentActivity.ALTERNATE_LINK_FRAGMENT_ACTIVITY
            }
        } else {
            AlternateLinkFragmentActivity.ALTERNATE_LINK_FRAGMENT_ACTIVITY
        }

        val hallOfFameFragment = if (BuildConfig.CELEB) {
            activity.supportFragmentManager
                .findFragmentByTag(tag) as HallOfFameFragment
        } else {
            if (activity is MainActivity) {
                val parentFragment = parentFragmentManager.findFragmentByTag("net.ib.mn.ranking.MainRankingFragment")

                parentFragment?.childFragmentManager?.fragments?.firstOrNull {
                    it::class.simpleName == HallOfFameFragment::class.simpleName
                } as HallOfFameFragment
            } else {
                activity.supportFragmentManager
                    .findFragmentByTag(tag) as HallOfFameFragment
            }
        }


        val tvOrderLatest = v.findViewById<AppCompatTextView>(R.id.tv_order_latest)
        val tvOrderHeart = v.findViewById<AppCompatTextView>(R.id.tv_order_heart)

        tvOrderLatest.setOnClickListener {
            hallOfFameFragment.filterClick(false)
            this.dismiss()
        }

        tvOrderHeart.setOnClickListener {
            hallOfFameFragment.filterClick(true)
            this.dismiss()
        }
    }

    private fun setFriendDeleteFilter(v: View) {
        val activity = context as FriendDeleteActivity
        val tvOrderByHeart = v.findViewById<AppCompatTextView>(R.id.tv_order_by_heart)
        val tvOrderByName = v.findViewById<AppCompatTextView>(R.id.tv_order_by_name)
        val tvOrderByLoginTime = v.findViewById<AppCompatTextView>(R.id.tv_order_by_login_time)

        tvOrderByName.setOnClickListener {
            activity.filterByName()
            this.dismiss()
        }

        tvOrderByLoginTime.setOnClickListener {
            activity.filterByLoginTime()
            this.dismiss()
        }

        tvOrderByHeart.setOnClickListener {
            activity.filterByHeart()
            this.dismiss()
        }
    }

    //채팅방  리스트  필터링용
    private fun setChattingRoomListFilter(v: View){
        val activity = context as? CommunityActivity
        val fragment = activity?.chattingRoomListFragment

        val viewPagerAdapter = activity?.binding?.viewPager2?.adapter as? CommunityPagerAdapter
        val indexChatting = viewPagerAdapter?.getIndexOfFragments(fragment ?: return)

        val chattingFragment =
            activity?.supportFragmentManager?.findFragmentByTag(
                "f$indexChatting"
            ) as? ChattingRoomListFragment ?: return

        val recentChatRoomFilter = v.findViewById<AppCompatTextView>(R.id.tv_recent_chat_room_list_filter)
        val manyTalkChatRoomFilter =v.findViewById<AppCompatTextView>(R.id.tv_many_talk_chat_room_list_filter)


        //최신순
        recentChatRoomFilter.setOnClickListener {
            chattingFragment.chatRoomListRcyAdapter.setChatRoomFilterSelected(
                    TYPE_CHAT_ROOM_LIST_FILTER_RECENT,
                    isJoinedRoom
            )
            chattingFragment.setChatRoomFilterSelected(TYPE_CHAT_ROOM_LIST_FILTER_RECENT, isJoinedRoom)
            this.dismiss()
        }

        //대화 많은
        manyTalkChatRoomFilter.setOnClickListener {
            chattingFragment.chatRoomListRcyAdapter.setChatRoomFilterSelected(
                    TYPE_CHAT_ROOM_LIST_FILTER_MANY_TALK,
                    isJoinedRoom
            )
            chattingFragment.setChatRoomFilterSelected(TYPE_CHAT_ROOM_LIST_FILTER_MANY_TALK, isJoinedRoom)
            this.dismiss()
        }

    }


    //채팅방 공개여부 설정
    private fun setChattingOpenStatusFilter(v: View){
        val activity = context as ChattingCreateActivity
        val tvChatMostOpen = v.findViewById<AppCompatTextView>(R.id.tv_chat_most_open)
        val tvChatCommunityOpen = v.findViewById<AppCompatTextView>(R.id.tv_chat_community_open)

        tvChatCommunityOpen.setOnClickListener {
            activity.setChatOpenStatus(CHAT_OPEN_COMMUNITY)
            this.dismiss()
        }

        tvChatMostOpen.setOnClickListener {
            activity.setChatOpenStatus(CHAT_OPEN_MOST)
            this.dismiss()
        }
    }

    //채팅방 닉네임 공개여부 설정
    private fun setChattingAnonymoustStatusFilter(v: View){
        val activity = context as ChattingCreateActivity
        val tvChatNickNameOpen = v.findViewById<AppCompatTextView>(R.id.tv_chat_nickname_open)
        val tvChatAnonymousOpen = v.findViewById<AppCompatTextView>(R.id.tv_chat_anonymous)

        tvChatNickNameOpen.setOnClickListener {
            activity.setChatAnonymousStatus(CHAT_OPEN_NICKNAME)
            this.dismiss()
        }

        tvChatAnonymousOpen.setOnClickListener {
            activity.setChatAnonymousStatus(CHAT_ANONYMOUS)
            this.dismiss()
        }
    }


    //채팅방  레벨 제한
    private fun setChatLevelLimit(v: View) {
        val activity = context as ChattingCreateActivity
        val tvLevel5 = v.findViewById<AppCompatTextView>(R.id.tv_level_5)
        val tvLevel10 = v.findViewById<AppCompatTextView>(R.id.tv_level_10)
        val tvLevel15 = v.findViewById<AppCompatTextView>(R.id.tv_level_15)
        val tvLevel20 = v.findViewById<AppCompatTextView>(R.id.tv_level_20)
        val tvLevel25 = v.findViewById<AppCompatTextView>(R.id.tv_level_25)
        val tvLevel30 = v.findViewById<AppCompatTextView>(R.id.tv_level_30)
        tvLevel5.text = String.format(this.getString(R.string.chat_room_level_limit), 5)
        tvLevel10.text = String.format(this.getString(R.string.chat_room_level_limit), 10)
        tvLevel15.text = String.format(this.getString(R.string.chat_room_level_limit), 15)
        tvLevel20.text = String.format(this.getString(R.string.chat_room_level_limit), 20)
        tvLevel25.text = String.format(this.getString(R.string.chat_room_level_limit), 25)
        tvLevel30.text = String.format(this.getString(R.string.chat_room_level_limit), 30)

        tvLevel5.setOnClickListener { activity.setChatLevelLimit(5);    this.dismiss()}
        tvLevel10.setOnClickListener { activity.setChatLevelLimit(10);  this.dismiss()}
        tvLevel15.setOnClickListener { activity.setChatLevelLimit(15);  this.dismiss()}
        tvLevel20.setOnClickListener { activity.setChatLevelLimit(20);  this.dismiss()}
        tvLevel25.setOnClickListener { activity.setChatLevelLimit(25);  this.dismiss()}
        tvLevel30.setOnClickListener { activity.setChatLevelLimit(30);  this.dismiss()}
    }

    // 다크모드 설정
    private fun setDarkmodeSetting(v: View) {
        val activity = context as StatusSettingActivity

        val tvSystem = v.findViewById<AppCompatTextView>(R.id.tv_darkmode_system)
        val tvAlways = v.findViewById<AppCompatTextView>(R.id.tv_darkmode_always)
        val tvNever = v.findViewById<AppCompatTextView>(R.id.tv_darkmode_never)

        tvSystem.setOnClickListener {
            activity.setDarkmode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            this.dismiss()
        }
        tvAlways.setOnClickListener {
            activity.setDarkmode(AppCompatDelegate.MODE_NIGHT_YES)
            this.dismiss()
        }
        tvNever.setOnClickListener {
            activity.setDarkmode(AppCompatDelegate.MODE_NIGHT_NO)
            this.dismiss()
        }
    }

    private fun setFeedReport(v: View) {
        val activity = context as FeedActivity
        val report = v.findViewById<AppCompatTextView>(R.id.tv_option_report)
        val block = v.findViewById<AppCompatTextView>(R.id.tv_option_block)
        if(!isJoinedRoom){
            block.text = String.format(getString(R.string.block))
        }
        else{
            block.text = String.format(getString(R.string.unblock))
        }

        report.setOnClickListener {
            activity.report()
            this.dismiss()
        }

        block.setOnClickListener {
            activity.blockCheckDialog()
            this.dismiss()
        }
    }

    //심사된 퀴즈 목록
    private fun setQuizInfo(v: View) {
        val activity = context as IdolQuizInfoActivity
        val confirmQuiz = v.findViewById<AppCompatTextView>(R.id.tv_confirm_quiz)
        val waitingQuiz = v.findViewById<AppCompatTextView>(R.id.tv_waiting_quiz)
        val rejectedQuiz = v.findViewById<AppCompatTextView>(R.id.tv_rejected_quiz)

        //채택된 퀴즈 클릭 시
        confirmQuiz.setOnClickListener {
            activity.getMyQuizList("Y",30,0)
            this.dismiss()
        }
        //대기중인 퀴즈 클릭 시
        waitingQuiz.setOnClickListener {
            activity.getMyQuizList("P",30,0)
            this.dismiss()
        }
        //거절된 퀴즈 클릭 시
        rejectedQuiz.setOnClickListener {
            activity.getMyQuizList("N",30,0)
            this.dismiss()
        }

    }

    private fun setFaqFilter(v: View) {
        val activity = context as FaqWriteActivity
        val tvUse = v.findViewById<AppCompatTextView>(R.id.tv_use)
        val tvReport = v.findViewById<AppCompatTextView>(R.id.tv_report)
        val tvPurchase = v.findViewById<AppCompatTextView>(R.id.tv_purchase)
        val tvProposal = v.findViewById<AppCompatTextView>(R.id.tv_proposal)
        val tvEtc = v.findViewById<AppCompatTextView>(R.id.tv_etc)

        tvUse.setOnClickListener {
            activity.setCategory(tvUse.text.toString(), "U")
            this.dismiss()
        }
        tvReport.setOnClickListener {
            activity.setCategory(tvReport.text.toString(), "B")
            this.dismiss()
        }
        tvPurchase.setOnClickListener {
            activity.setCategory(tvPurchase.text.toString(), "P")
            this.dismiss()
        }
        tvProposal.setOnClickListener {
            activity.setCategory(tvProposal.text.toString(), "I")
            this.dismiss()
        }
        tvEtc.setOnClickListener {
            activity.setCategory(tvEtc.text.toString(), "E")
            this.dismiss()
        }

    }

    private fun setMediaFilter(v : View) {
        val activity = context as FaqWriteActivity
        val tvImg = v.findViewById<AppCompatTextView>(R.id.tv_img)
        val tvVideo = v.findViewById<AppCompatTextView>(R.id.tv_video)

        tvImg.setOnClickListener {
            activity.getPhotoOrVideo(true)
            this.dismiss()
        }
        tvVideo.setOnClickListener {
            activity.getPhotoOrVideo(false)
            this.dismiss()
        }
    }

    private fun setSmallTalkFilterLanguage(v : View) {
        val activity = context as? CommunityActivity
        val fragment = activity?.smallTalkFragment

        val viewPagerAdapter = activity?.binding?.viewPager2?.adapter as? CommunityPagerAdapter
        val indexSmallTalk = viewPagerAdapter?.getIndexOfFragments(fragment ?: return)

        val smallTalkFragment =
            activity?.supportFragmentManager?.findFragmentByTag(
                "f$indexSmallTalk"
            ) as? SmallTalkFragment ?: return

        val smallTalkHeaderVH =
            smallTalkFragment.binding.rvSmallTalk.findViewHolderForAdapterPosition(0) as? SmallTalkHeaderVH
                ?: return

        val rvSmallTalkLanguage = v.findViewById<RecyclerView>(R.id.rv_small_talk_locale)

        rvSmallTalkLanguage.adapter = BottomSheetSmallTalkLocaleAdapter(activity, BoardLanguage.all()).apply {
            this.languageClickListener(object : BottomSheetSmallTalkLocaleAdapter.OnClickListener{
                override fun onItemClicked(locale: String?, langText: String) {
                    smallTalkHeaderVH.filterByLanguage(locale, langText)
                    this@BottomSheetFragment.dismiss()
                }
            })
        }
    }
}