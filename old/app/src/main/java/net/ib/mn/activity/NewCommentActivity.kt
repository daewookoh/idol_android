package net.ib.mn.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.annotation.OptIn
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.android.volley.VolleyError
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.IdolApplication
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.FeedActivity.Companion.PARAM_USER_BLOCK_STATUS
import net.ib.mn.activity.FeedActivity.Companion.PARAM_USER_ID
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.addon.InternetConnectivityManager
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.core.domain.usecase.LikeArticleUseCase
import net.ib.mn.core.model.TagModel
import net.ib.mn.databinding.DialogSurpriseHeartBinding
import net.ib.mn.dialog.ArticleRemoveDialogFragment
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.BaseDialogFragment.DialogResultHandler
import net.ib.mn.dialog.ReportDialogFragment
import net.ib.mn.dialog.VoteDialogFragment
import net.ib.mn.domain.usecase.GetIdolsByIdsUseCase
import net.ib.mn.fragment.ArticleViewMoreBottomSheetFragment
import net.ib.mn.fragment.FeedActivityFragment
import net.ib.mn.fragment.WidePhotoFragment
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.CommentModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.ScheduleModel
import net.ib.mn.model.UserModel
import net.ib.mn.schedule.IdolSchedule
import net.ib.mn.utils.ApiCacheManager.Companion.getInstance
import net.ib.mn.utils.CommentTranslation
import net.ib.mn.utils.CommentTranslationHelper
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.Translation
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.ext.updatePadding
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.trimNewlineWhiteSpace
import net.ib.mn.view.ExodusImageView
import org.json.JSONObject
import javax.inject.Inject


/**
 * ProjectName: idol_app_renew
 *
 * Description:
 * CommentActivity 에서
 * kotlin 및  recyclerview 변환을 한  새로운 comment 화면 용 엑티비티이다.
 *
 * */
@AndroidEntryPoint
class NewCommentActivity : BaseCommentActivity(), DialogResultHandler, Translation, CommentTranslation {
    @Inject
    lateinit var likeArticleUseCase: LikeArticleUseCase
    @Inject
    lateinit var articlesRepository: ArticlesRepository
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var getIdolsByIdsUseCase: GetIdolsByIdsUseCase
    @Inject
    lateinit var accountManager: IdolAccountManager

    private var mArticlePosition: Int? = null
    private var mIsScrollToTop = false
    private var mAdapterType = NewCommentAdapter.TYPE_ARTICLE
    private lateinit var menu: Menu

    private lateinit var gifImage: AppCompatImageView
    private lateinit var photo: ExodusImageView

    private var scheduleFlag = false

    private var mSchedule: ScheduleModel? = null
    private var mIds: HashMap<Int, String>? = null

    private var mAccount: IdolAccount? = null

    private var isScheduleCommentPush = false
    private var isDataSavingMode = false

    private var lastSafeBottom = 0

    @Inject
    lateinit var videoAdUtil: VideoAdUtil
    @Inject
    lateinit var commentTranslationHelper: CommentTranslationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_support_photo_certify)

        setupEdgeToEdgeForComments()
        initUI()
        setAdManager()

        if (mArticle!!.user == null) {
            FLAG_CLOSE_DIALOG = false
            showArticleRemovedError()
        } else if (
            (mArticle!!.isMostOnly == "Y"
                    && mAccount!!.most != null
                    && mAccount!!.most?.getId() != mArticle!!.idol?.getId())
            && mArticle!!.user?.id != mAccount!!.userModel?.id) {
            // 최애 공개 게시물은 최애만 볼 수 있게
            FLAG_CLOSE_DIALOG = false
            showArticleMostOnly()
        }else{
            setRecyclerView()
            setRecyclerViewListener()
            setClickEvent()
        }
    }

    private fun setupEdgeToEdgeForComments() {
        val root = binding.clContainer
        val toolbar = binding.toolbar
        val footer = binding.footer
        val emoji = binding.rlEmoticon
        val recycler = binding.rcySupportPhoto
        val friends = binding.friendsRcyView

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 기존 로직 유지 …
        // emoji.addOnLayoutChangeListener { … } 는 아래로 교체

        var wasVisible = emoji.isShown
        emoji.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val nowVisible = v.isShown
            if (wasVisible && !nowVisible) {
                // 이모지 패널이 막 GONE 되는 순간
                // EditText에 포커스가 있을 때만 (즉, 키보드로 전환될 가능성이 높을 때만) 모드를 변경하여
                // 전송/백버튼으로 판넬을 닫는 동작과 충돌하지 않도록 합니다.
                if (binding.viewComment.inputComment.isFocused) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                }
                // 레이아웃 적용 이후 프레임에 인셋 재요청 2번 정도 보강
                root.post { ViewCompat.requestApplyInsets(root) }
                root.postOnAnimation { ViewCompat.requestApplyInsets(root) }
            }
            wasVisible = nowVisible

            // 기존처럼 항상 재요청
            ViewCompat.requestApplyInsets(root)
        }

        // IME 애니 끝난뒤 한 프레임 후 재요청 (기존 유지)
        ViewCompat.setWindowInsetsAnimationCallback(
            root,
            object : WindowInsetsAnimationCompat.Callback(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
            ) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    running: List<WindowInsetsAnimationCompat?>
                ) = insets

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    root.post { ViewCompat.requestApplyInsets(root) }
                }
            }
        )

        var cachedSafeBottom = 0
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val nav = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom
            val cutout = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.displayCutout()).bottom
            val gest = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemGestures()).bottom
            val tapp = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.tappableElement()).bottom
            val safeBottomNow = maxOf(nav, cutout, gest, tapp)

            if (cachedSafeBottom == 0) {
                cachedSafeBottom = safeBottomNow.coerceAtLeast((root.resources.displayMetrics.density * 16).toInt())
            } else if (safeBottomNow > 0) {
                cachedSafeBottom = safeBottomNow
            }
            val safeBottom = cachedSafeBottom
            lastSafeBottom = safeBottom

            toolbar.updatePadding(top = status.top)

            val emojiShowing = emoji.visibility == View.VISIBLE && emoji.height > 0 && emoji.isShown
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            val footerBottomMargin = when {
                imeVisible   -> maxOf(ime.bottom, safeBottom)
                emojiShowing -> 0
                else         -> safeBottom
            }
            footer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = footerBottomMargin
            }

            emoji.updatePadding(bottom = safeBottom)
            recycler.updatePadding(bottom = 0)
            friends.updatePadding(bottom = 0)

            insets
        }
    }

    // 움짤프사 및 카테고리
    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (!Const.USE_ANIMATED_PROFILE) return

            // 움짤 주소가 있을 때에만 처리
            try {
                if (simpleExoPlayerView != null && hasVideo(simpleExoPlayerView)) {

                    photo.post {
                        mGlideRequestManager
                            .asBitmap()
                            .load(recyclerviewAdapter.articleModel.thumbnailUrl)
                            .onlyRetrieveFromCache(true)
                            .listener(object : RequestListener<Bitmap> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Bitmap>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    gifImage.visibility = View.GONE
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Bitmap,
                                    model: Any,
                                    target: Target<Bitmap>,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    runOnUiThread {

                                        // 네트워크가 느린 경우 뒤늦게 썸네일이 로드되는 경우 처리
                                        photo.setImageBitmap(resource)
                                        photo.post {

                                            // gif icon 표시
                                            var width = resource.width
                                            var height = resource.height
                                            val ratio = height.toDouble() / width.toDouble()
                                            val dispWidth = photo.width
                                            val dispHeight = photo.height
                                            if (ratio < 1.0) { // 가로로 긴 움짤
                                                width = dispWidth
                                                height = (dispHeight * ratio).toInt()
                                            } else {
                                                width = (dispWidth / ratio).toInt()
                                                height = dispHeight
                                            }
                                            val lp =
                                                gifImage.layoutParams as RelativeLayout.LayoutParams
                                            lp.rightMargin =
                                                (Util.convertDpToPixel(context, 10f).toInt()
                                                    + (dispWidth - width) / 2)
                                            lp.bottomMargin =
                                                (Util.convertDpToPixel(context, 10f).toInt()
                                                    + (dispHeight - height) / 2)
                                            Util.log(
                                                "lp rightMargin=" + lp.rightMargin
                                                    + " bottomMargin=" + lp.bottomMargin
                                            )
                                            gifImage.layoutParams = lp
                                            gifImage.visibility = View.VISIBLE
                                        }
//                                        photo.setOnClickListener(this)
//                                        attachButton.setOnClickListener(this@CommentActivity)
                                    }
                                    return false
                                }
                            }).submit()
                    }

                    (simpleExoPlayerView!!.parent as ViewGroup).findViewById<View>(
                        R.id.eiv_attach_photo
                    ).visibility = View.INVISIBLE
                    simpleExoPlayerView!!.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mArticle = getIntent().getSerializableExtra(PARAM_ARTICLE) as ArticleModel?
        mAdapterType = getIntent().getIntExtra(PARAM_ADAPTER_TYPE, NewCommentAdapter.TYPE_ARTICLE)
        mArticlePosition = getIntent().getSerializableExtra(PARAM_POSITION) as Int?
        mIsScrollToTop = intent!!.getBooleanExtra(PARAM_SCROLL_TO_TOP, false)

        if(!mIsScrollToTop){//아티클 위쪽부터 보여주는 경우가 아니라면,  맨밑부터 보여주므로, isScrollEnd도 true값 적용
            isScrollEnd = true
        }

        scheduleFlag = getIntent().getBooleanExtra(PARAM_SCHEDULE_FLAG, false)
        if (scheduleFlag) {
            mSchedule = getIntent().getSerializableExtra(PARAM_SCHEDULE) as ScheduleModel?
            mIds = getIntent().getSerializableExtra(PARAM_SCHEDULE_IDOLS) as HashMap<Int, String>?
            isScheduleCommentPush =
                getIntent().getBooleanExtra(PARAM_SCHEDULE_IS_COMMENT_PUSH, false)
        }
        loadComments(mArticle)
    }

    fun hasVideo(view: PlayerView?): Boolean {
        if (view?.tag == null) return false
        val url = view.tag.toString()
        return url.endsWith("_s_mv.jpg") || url.endsWith("mp4")
    }

    @OptIn(UnstableApi::class)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Util.handleVideoAdResult(
            this,
            false,
            true,
            requestCode,
            resultCode,
            data,
            "community_videoad"
        ) { adType: String? ->
            videoAdUtil.onVideoSawCommon(
                this,
                true,
                adType,
                null
            )
        }

        if (requestCode == RequestCode.ARTICLE_EDIT.value && resultCode == ResultCode.EDITED.value) {
            if (data != null) {
                val updatedArticle = data.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel?
                if (updatedArticle != null) {
                    mArticle = updatedArticle
                    recyclerviewAdapter.setArticle(mArticle)
                    val intent = Intent()
                    intent.putExtra(Const.EXTRA_ARTICLE, mArticle)
                    setResult(ResultCode.EDITED.value, intent)
                }
            }
        }

        if(requestCode == REQUEST_FEED_MODIFY){//댓글 화면에서 피드 가서  수정 하는 경우
            if (resultCode == FeedActivityFragment.FEED_ARTICLE_REMOVE) {//피드가서 아티클 삭제하고 돌아왔을때
                setResultData(ResultCode.REMOVED.value)
            }else if(resultCode == FeedActivityFragment.FEED_ARTICLE_MODIFY){//피드가서  아티클 업데이트 한 경우
                if (data != null) {
                    val updatedArticle = data.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel?
                    if (updatedArticle != null) {
                        mArticle = updatedArticle
                        recyclerviewAdapter.setArticle(mArticle)
                        setResultData(ResultCode.EDITED.value)
                    }
                }
            }else if(resultCode == ResultCode.BLOCKED.value){
                val userId = data?.getIntExtra(PARAM_USER_ID, 0) ?: 0
                val isBlock = data?.getStringExtra(PARAM_USER_BLOCK_STATUS) ?: ""

                setResultData(ResultCode.BLOCKED.value, userId, isBlock)
            }
        }
        if(resultCode == ResultCode.ARTICLE_LIKE_EXCEPTION.value) {
            if(data != null) {
                val updatedArticle = data.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel?
                if (updatedArticle != null) {
                    mArticle = updatedArticle
                    postArticleLike(mArticle!!)
                    recyclerviewAdapter.setArticle(mArticle)
                }
            }
        }


    }

    override fun onContextMenuClosed(menu: Menu) {
        super.onContextMenuClosed(menu)
        // 댓글 링크 다시 동작하도록 설정
        recyclerviewAdapter.notifyDataSetChanged()
    }

    private fun setClickEvent() {

        recyclerviewAdapter.setOnArticleItemClickListener(object :
            NewCommentAdapter.OnArticleItemClickListener {

            override fun photoClicked() {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "comment_widephoto"
                )
                if (isFinishing) return
                WidePhotoFragment.getInstance(
                    mArticle!!,
                    mAdapterType == NewCommentAdapter.TYPE_SMALL_TALK
                ).apply {
                    setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                }.show(supportFragmentManager, "wide_photo")
            }

            override fun onEditArticle() {
                startWriteActivity()
            }

            override fun onRemoveArticle() {
                removeArticle()
            }

            @SuppressLint("StringFormatInvalid")
            override fun onShareArticle() {
                // no-op
            }


            //프로필 이동
            override fun onClickWriterProfile(userModel: UserModel) {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "comment_feed"
                )
                startActivityForResult(FeedActivity.createIntent(this@NewCommentActivity, userModel),REQUEST_FEED_MODIFY)
            }

            override fun onReportBtnClicked() {
                reportArticle()
            }

            override fun onVoteBtnClicked(articleModel: ArticleModel) {
                if (Util.mayShowLoginPopup(this@NewCommentActivity)) {
                    return
                }
                Util.showProgress(this@NewCommentActivity)
                lifecycleScope.launch {
                    usersRepository.isActiveTime(
                        { response ->
                            Util.closeProgress()
                            if (response.optBoolean("success")) {
                                val gcode = response.optInt("gcode")
                                if (response.optString("active") == Const.RESPONSE_Y) {
                                    if (response.optInt("total_heart") == 0) {
                                        Util.showChargeHeartDialog(this@NewCommentActivity)
                                    } else {
                                        if (response.optString("vote_able").equals(
                                                Const.RESPONSE_Y, ignoreCase = true
                                            )
                                        ) {
                                            voteHeart(
                                                articleModel, -1,
                                                response.optLong("total_heart"),
                                                response.optLong("free_heart")
                                            )
                                        } else {
                                            if (gcode == Const.RESPONSE_IS_ACTIVE_TIME_1) {
                                                Toast.makeText(
                                                    this@NewCommentActivity, getString(
                                                        R.string.response_users_is_active_time_over
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    this@NewCommentActivity,
                                                    getString(R.string.msg_not_able_vote),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                } else {
                                    val start = Util.convertTimeAsTimezone(
                                        response.optString("begin")
                                    )
                                    val end = Util.convertTimeAsTimezone(
                                        response.optString("end")
                                    )
                                    val unableUseTime: String = String.format(
                                        getString(R.string.msg_unable_use_vote), start, end
                                    )
                                    Util.showIdolDialogWithBtn1(
                                        this@NewCommentActivity,
                                        null,
                                        unableUseTime
                                    ) { Util.closeIdolDialog() }
                                }
                            } else { // success is false!
                                UtilK.handleCommonError(this@NewCommentActivity, response)
                            }
                        }, {
                            Util.closeProgress()
                            Toast.makeText(
                                this@NewCommentActivity,
                                R.string.error_abnormal_exception, Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            override fun onArticleLikeClicked(articleModel: ArticleModel) {
                //clickSubject 내 api호출 전 화면 나갔을 경우 setResult 날려주기 위함
                val intent = Intent()
                intent.putExtra(Const.EXTRA_ARTICLE, articleModel)
                setResult(ResultCode.ARTICLE_LIKE_EXCEPTION.value, intent)
                postArticleLike(articleModel)
            }

            override fun onCommentShowClicked() {
                Util.showSoftKeyboard(this@NewCommentActivity, binding.viewComment.inputComment)

                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.WRITE_COMMENT_SMALL_TALK.actionValue,
                    GaAction.WRITE_COMMENT_SMALL_TALK.label
                )
            }
        })

        binding.viewComment.btnSubmit.setOnClickListener {
            mArticle?.let { it1 ->
                    writeComments(it1,
                        idol = it1.idol ?: return@let,
                        emoticonId = selectedEmoticonId,
                        comment = Util.BadWordsFilterToHeart(this, binding.viewComment.inputComment.text.toString()),
                        binImage,
                        scheduleFlag)
            }

            // Directly set the bottom margin to restore footer position
            (binding.footer.layoutParams as? ConstraintLayout.LayoutParams)?.let {
                it.bottomMargin = lastSafeBottom
                binding.footer.layoutParams = it
            }
            // Restore soft input mode for next interaction
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    private fun postArticleLike(model: ArticleModel) {
        MainScope().launch {
            likeArticleUseCase(model.id, !model.isUserLike).collect { response ->
                if( !response.success ) {
                    response.message?.let {
                        Toast.makeText(this@NewCommentActivity, it, Toast.LENGTH_SHORT).show()
                    }
                    return@collect
                }

                model.isUserLike = response.liked
                model.isEdit = true
                val intent = Intent()
                intent.putExtra(Const.EXTRA_ARTICLE, model)
                setResult(ResultCode.UPDATE_LIKE_COUNT.value, intent)
            }
        }
    }


    //댓글 리스트를 가지고 온다.
    private fun loadComments(mArticle: ArticleModel?) {
        loadComments(mArticle) { response ->
            if (response.optBoolean("success")) {
                val numComments = response.getJSONObject("article").optInt("num_comments", 0)
                if (scheduleFlag) {
                    mSchedule?.let {
                        it.num_comments = mArticle!!.commentCount
                        IdolSchedule.getInstance().setScheduleItem(it)
                    }
                }

                this@NewCommentActivity.mArticle?.commentCount = numComments //community업데이트 용으로  전격 article모델 값도 변형해줌.
            } else {
                setResultData(ResultCode.REMOVED.value)
            }
        }
    }


    private fun voteHeart(model: ArticleModel, position: Int, totalHeart: Long, freeHeart: Long) {
        val fragment = VoteDialogFragment
            .getArticleVoteInstance(model, position, totalHeart, freeHeart)
        fragment.setActivityRequestCode(RequestCode.ARTICLE_VOTE.value)
        fragment.show(supportFragmentManager, "comment_vote")
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onDialogResult(requestCode, resultCode, data)

        // super에서 처리한 후 마저 처리
        if (requestCode == RequestCode.ARTICLE_COMMENT_REMOVE.value) {
            if (resultCode == ResultCode.COMMENT_REMOVED.value) {
                if (scheduleFlag) {
                    mSchedule?.let {
                        it.num_comments = mArticle!!.commentCount
                        IdolSchedule.getInstance().setScheduleItem(it)
                    }
                }

                setResultData(ResultCode.COMMENT_REMOVED.value)
            }
        } else if (requestCode == RequestCode.ARTICLE_REMOVE.value) {
            Util.closeProgress()
            if (resultCode == ResultCode.REMOVED.value) {
                val intent = Intent()
                intent.putExtra(Const.EXTRA_ARTICLE, mArticle)
                setResult(ResultCode.REMOVED.value, intent)
                finish()
            }
        } else if (requestCode == RequestCode.ARTICLE_VOTE.value) {
            if (resultCode == BaseDialogFragment.RESULT_OK) {

                if (data != null) {
                    val heart = data.getLongExtra(VoteDialogFragment.PARAM_HEART, 0)
                    if (heart > 0) {
                        val article =
                            (data.getSerializableExtra(PARAM_ARTICLE) as ArticleModel?)!!
                        val updatedHeart = article.heart + heart
                        article.heart = updatedHeart
                        mArticle = article

                        //새로운 article model을 보내서  업데이트 실행
                        recyclerviewAdapter.setArticle(mArticle)


                        setResultData(ResultCode.VOTED.value)
                        val eventHeart = data.getStringExtra(PARAM_EVENT_HEART)
                        eventHeart?.let { showEventDialog(it) }
                        // 최애에게 투표하면 최애화면에서 프사 순위 변경 적용되게
                        try {
                            val account = IdolAccount.getAccount(this)
                            if (account?.userModel?.most != null && account?.userModel?.most?.getId() == article.idol?.getId()) {
                                getInstance().clearCache(Const.KEY_FAVORITE)
                            }

                            // 레벨업 체크
                            UtilK.checkLevelUp(this, accountManager, article.idol, heart)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        Util.closeProgress()
                    }
                }
            }
        }
    }

    private fun showEventDialog(eventHeart: String) {
        val eventHeartDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        eventHeartDialog.window!!.attributes = lpWindow
        eventHeartDialog.window!!
            .setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

        val dialogBinding = DialogSurpriseHeartBinding.inflate(layoutInflater)
        eventHeartDialog.setContentView(dialogBinding.root)
        eventHeartDialog.setCanceledOnTouchOutside(false)
        eventHeartDialog.setCancelable(false)
        dialogBinding.btnOk.setOnClickListener { v: View? -> eventHeartDialog.cancel() }
        val surpriseMsg = String.format(getString(R.string.msg_surprise_heart), eventHeart)
        dialogBinding.message.text = surpriseMsg
        eventHeartDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        eventHeartDialog.show()
    }

    private fun setResultData(resultCode: Int, userId : Int = 0, isBlock: String = "") {
        val resultIntent = Intent()
        resultIntent.putExtra(Const.EXTRA_ARTICLE, mArticle)
        resultIntent.putExtra(PARAM_POSITION, mArticlePosition)
        resultIntent.putExtra(PARAM_SCROLL_TO_TOP, false)
        resultIntent.putExtra(PARAM_USER_ID, userId)
        resultIntent.putExtra(PARAM_USER_BLOCK_STATUS, isBlock)
        setResult(resultCode, resultIntent)
    }

    private fun setRecyclerView() {
        tagName = getIntent().getStringExtra(PARAM_TAG_NAME) ?: ""

        recyclerviewAdapter = if(mSchedule != null){
            NewCommentAdapter(
                this,
                useTranslation = ConfigModel.getInstance(this).showTranslation,
                mGlideRequestManager,
                NewCommentAdapter.TYPE_SCHEDULE,
                recyclerView = binding.rcySupportPhoto,
                mIsScrollToTop,
                tagName = tagName,
                getIdolsByIdsUseCase = getIdolsByIdsUseCase,
                lifecycleScope = lifecycleScope
            )
        }else{
            // 자게라면 번역 가능하게 -> 모든 커뮤/댓글로
            var useTranslation = ConfigModel.getInstance(this).showTranslation

            NewCommentAdapter(
                this,
                useTranslation,
                mGlideRequestManager,
                if(mAdapterType == NewCommentAdapter.TYPE_SMALL_TALK) NewCommentAdapter.TYPE_SMALL_TALK else NewCommentAdapter.TYPE_ARTICLE,
                recyclerView = binding.rcySupportPhoto,
                mIsScrollToTop,
                getIdolsByIdsUseCase = getIdolsByIdsUseCase,
                lifecycleScope = lifecycleScope,
                tagName = tagName,
            )
        }

        binding.rcySupportPhoto.apply {
            adapter = recyclerviewAdapter
            itemAnimator = null
        }



        //헤더 포지션이  스케쥴 타입일때
        if(recyclerviewAdapter.getItemViewType(0) == NewCommentAdapter.TYPE_SCHEDULE) {
            recyclerviewAdapter.setSchedule(intent.getSerializableExtra(PARAM_SCHEDULE) as ScheduleModel,mIds = mIds, isScheduleCommentPush)
            //댓글 로드
            loadComments(intent.getSerializableExtra(PARAM_ARTICLE) as ArticleModel)

        } else {//헤더 포지션이  아티클 타입일때
            recyclerviewAdapter.setArticle((intent.getSerializableExtra(PARAM_ARTICLE) as ArticleModel))
            //번들로 넘어온 아티클로 먼저 업데이트 해준후 상세 API호출하여 상세정보 가져온다.
            getArticleDetail(mArticle)
        }


        //비디오뷰
        recyclerviewAdapter.setVideoPlayerView(object : NewCommentAdapter.GetVideoPlayView {
            override fun getExoVideoPlayView(
                playerView: PlayerView?,
                imageView: ExodusImageView?,
                ivGif: AppCompatImageView?,
                videoUrl: String?
            ) {
                if (ivGif != null) {
                    gifImage = ivGif
                }
                if (imageView != null) {
                    photo = imageView
                }
                Handler().postDelayed({
                    if (playerView != null && imageView != null) {
                        playExoPlayer(playerView, videoUrl, imageView)
                    }
                }, 400)
            }

        })

        binding.friendsRcyView.visibility = View.GONE
        setFriendsRecyclerView(binding.friendsRcyView)
        inputCommentChangeListen(binding.viewComment.inputComment)
    }

    //초기세팅
    private fun initUI() {
        setToolbar()
        initBaseUI()

        if (mAccount == null) mAccount = IdolAccount.getAccount(this)

        isDataSavingMode = (Util.getPreferenceBool(this, Const.PREF_DATA_SAVING, false)
                && !InternetConnectivityManager.getInstance(this).isWifiConnected)

        mArticle = intent.getSerializableExtra(PARAM_ARTICLE) as ArticleModel?
        mAdapterType = intent.getIntExtra(PARAM_ADAPTER_TYPE, NewCommentAdapter.TYPE_ARTICLE)
        mArticlePosition = intent.getSerializableExtra(PARAM_POSITION) as Int?
        mIsScrollToTop = intent!!.getBooleanExtra(PARAM_SCROLL_TO_TOP, false)

        if(!mIsScrollToTop){//아티클 위쪽부터 보여주는 경우가 아니라면,  맨밑부터 보여주므로, isScrollEnd도 true값 적용
            isScrollEnd = true
        }

        scheduleFlag = intent.getBooleanExtra(PARAM_SCHEDULE_FLAG, false)
        if (scheduleFlag) {
            mSchedule = intent.getSerializableExtra(PARAM_SCHEDULE) as ScheduleModel?
            mIds = intent.getSerializableExtra(PARAM_SCHEDULE_IDOLS) as HashMap<Int, String>?
            isScheduleCommentPush =
                intent.getBooleanExtra(PARAM_SCHEDULE_IS_COMMENT_PUSH, false)
        }

    }

    override fun setToolbar(){
        super.setToolbar()
        supportActionBar?.apply {
            title = if (mAdapterType == NewCommentAdapter.TYPE_SMALL_TALK) {
                String.format(
                    "%s %s",
                    mArticle?.idol?.getName(this@NewCommentActivity),
                    this@NewCommentActivity.getString(R.string.menu_board)
                )
            } else {
                this@NewCommentActivity.getString(R.string.post_detail)
            }
        }
    }

    override fun onBackPressed() {
        if (binding.rlEmoticon.isVisible) { //뒤로가기 눌렀을때 이모티콘창 있을경우엔 뒤로가지말고 이모티콘창 닫아줌.
            binding.rlEmoticon.visibility = View.GONE
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) // 원복
            binding.clContainer.post { ViewCompat.requestApplyInsets(binding.clContainer) }
        } else { //화면 닫아줌.
            try {
                if (IdolApplication.getInstance(this).mainActivity == null
                    && !intent.getBooleanExtra(MainActivity.IS_DEEP_LINK_CLICK_FROM_IDOL,false)) {

                    //딥링크 이동할떄 main 없어지는데  이때는  Main 을 다시 살릴 필요없으므로,  removeExtra시킨다.
                    intent.removeExtra(MainActivity.IS_DEEP_LINK_CLICK_FROM_IDOL);
                    startActivity(MainActivity.createIntent(this, false));
                    finish();
                } else {
                    super.onBackPressed();
                }
            } catch (e: Exception) {
                super.onBackPressed();
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (FeedActivity.USER_BLOCK_CHANGE) {
            loadComments(mArticle)
        }
        if (mAccount?.userName != null && mAccount?.userName!!.isEmpty()) {
            accountManager.fetchUserInfo(this)
        }
        if (!scheduleFlag) {
            // TODO: broadcast 없애야 댓글화면에서 버벅이는거 없어짐
            val filter = IntentFilter()
            filter.addAction(Const.REFRESH)
            if (Const.USE_ANIMATED_PROFILE) {
                filter.addAction(Const.PLAYER_START_RENDERING)
            }
            LocalBroadcastManager.getInstance(this).registerReceiver(
                mBroadcastReceiver,
                filter
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (!scheduleFlag) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
            } catch (e: Exception) {
            }
            // 움짤 멈추기
            if (Const.USE_ANIMATED_PROFILE) {
                stopExoPlayer(simpleExoPlayerView)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!scheduleFlag) {
            try {
                player1?.release()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStop() {
        FLAG_CLOSE_DIALOG = false
        super.onStop()
    }

    private fun showArticleRemovedError() {
        Util.showDefaultIdolDialogWithBtn1(
            this,
            null,
            getString(R.string.deleted_by_unknown)
        ) { view: View? ->
            Util.closeIdolDialog()
            startActivity(MainActivity.createIntent(this, false))
            finish()
        }
    }
    private fun showArticleMostOnly(){
        Util.showDefaultIdolDialogWithBtn1(this ,
        null,
        getString(R.string.show_most_only)
        ) { view: View? ->
             Util.closeIdolDialog()
             startActivity(StartupActivity.createIntent(this))
             finish()
          }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        return if((mAdapterType == NewCommentAdapter.TYPE_SMALL_TALK || mAdapterType == NewCommentAdapter.TYPE_ARTICLE) && !scheduleFlag ) {
            this.menu = menu
            menuInflater.inflate(R.menu.community_menu, menu)
            true
        } else {
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.more) {
            if (mArticle?.files.isNullOrEmpty() || mArticle?.files?.first()?.originUrl == null) {
                mArticle?.files = mutableListOf()
            }
            UtilK.clickMore(this, mArticle) { showEdit, showRemove, showReport, showShare ->
                val tag = "article"
                val sheet = ArticleViewMoreBottomSheetFragment.newInstance(
                    showEdit,
                    showRemove,
                    showReport,
                    showShare,
                    onClickEdit = {
                        filterEditArticle()
                    },
                    onClickDelete = {
                        filterRemoveArticle()
                    },
                    {
                        filterReportArticle()
                    }
                ) {
                    shareArticle()
                }

                val oldFrag = supportFragmentManager.findFragmentByTag(tag)
                if (oldFrag == null) {
                    sheet.show(supportFragmentManager, tag)
                }
            }

            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun filterReportArticle() {
        reportArticle()
    }

    fun filterEditArticle() {
        startWriteActivity()
    }

    fun filterRemoveArticle() {
        removeArticle()
    }

    private fun reportArticle() {
        val account = IdolAccount.getAccount(this@NewCommentActivity)
        if (account == null && Util.mayShowLoginPopup(this@NewCommentActivity)) {
            return
        }

        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.COMMENT_REPORT.actionValue,
            GaAction.COMMENT_REPORT.label
        )

        if (!UtilK.isArticleNotReported(this@NewCommentActivity, mArticle?.id ?: return)) {
            // 신고하지 않은 글이라면
            Toast.makeText(
                this@NewCommentActivity,
                R.string.failed_to_report__already_reported,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        UtilK.addArticleReport(this@NewCommentActivity, mArticle?.id ?: return)

        // config/self 제거하고 미리 받아놓은 값 사용
        val reportHeart = ConfigModel.getInstance(this@NewCommentActivity).reportHeart
        val report: ReportDialogFragment = ReportDialogFragment.getInstance(mArticle)
        val articleIdol: IdolModel? = mArticle?.idol

        // 하트 차감 수가 0일 때
        if (reportHeart == 0 // 지식돌, 자유게시판에서는 무료로 신고 가능
            || articleIdol?.getId() == Const.IDOL_ID_KIN || articleIdol?.getId() == Const.IDOL_ID_FREEBOARD // 내 커뮤이면서
            || (account?.userModel?.most?.getId() == articleIdol?.getId() // 최애가 없는 사람 글과
                && (mArticle?.user != null
                && (mArticle?.user!!.most == null // 커뮤니티가 최애가 아닌 사람의 글도 무료로 신고 가능
                || (mArticle!!.user?.most != null
                && mArticle!!.user?.most?.getId() != articleIdol?.getId()))))
        ) {
            report.setMessage(
                HtmlCompat.fromHtml(
                    getString(R.string.warning_report_hide_article),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            )
        } else {
            if (reportHeart > 0) {
                val color = "#" + Integer.toHexString(
                    ContextCompat.getColor(
                        this@NewCommentActivity,
                        R.color.main
                    )
                ).substring(2)
                val msg: String = String.format(
                    resources.getString(R.string.warning_report_lose_heart),
                    "<FONT color=$color>$reportHeart</FONT>"
                )
                val spanned = HtmlCompat.fromHtml(
                    msg,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
                report.setMessage(spanned)
            }
        }
        report.setActivityRequestCode(RequestCode.ARTICLE_REPORT.value)
        report.show(supportFragmentManager, "report")
    }

    private fun removeArticle() {
        Util.showProgress(this@NewCommentActivity)

        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.COMMENT_DELETE.actionValue,
            GaAction.COMMENT_DELETE.label
        )
        val removeDlg = ArticleRemoveDialogFragment
            .getInstance(mArticle)
        removeDlg.setActivityRequestCode(RequestCode.ARTICLE_REMOVE.value)
        removeDlg.show(supportFragmentManager, "remove")
    }

    private fun startWriteActivity() {
        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.COMMENT_EDIT.actionValue,
            GaAction.COMMENT_EDIT.label
        )
        if (Const.FEATURE_WRITE_RESTRICTION) {
            // 집계시간에는 수정도 불가
            lifecycleScope.launch {
                usersRepository.isActiveTime(
                    { response ->
                        if (response.optBoolean("success")) {
                            if (response.optString("active") == Const.RESPONSE_Y) {
                                val intent =
                                    if (mAdapterType == NewCommentAdapter.TYPE_SMALL_TALK) {
                                        WriteSmallTalkActivity.createIntent(
                                            this@NewCommentActivity, mArticle!!.idol, mArticle!!.tagId
                                        )
                                    } else {
                                        WriteArticleActivity.createIntent(
                                            this@NewCommentActivity, mArticle!!.idol
                                        )
                                    }
                                intent.putExtra(Const.EXTRA_ARTICLE, mArticle)
                                startActivityForResult(
                                    intent,
                                    RequestCode.ARTICLE_EDIT.value
                                )
                            } else {
                                val start = Util.convertTimeAsTimezone(
                                    response.optString("begin")
                                )
                                val end = Util.convertTimeAsTimezone(
                                    response.optString("end")
                                )
                                val unableUseTime = String.format(
                                    getString(R.string.msg_unable_use_write), start,
                                    end
                                )
                                Util.showIdolDialogWithBtn1(
                                    this@NewCommentActivity,
                                    null,
                                    unableUseTime
                                ) { Util.closeIdolDialog() }
                            }
                        } else { // success is false!
                            val responseMsg = ErrorControl.parseError(
                                this@NewCommentActivity, response
                            )
                            IdolSnackBar.make(findViewById(android.R.id.content), responseMsg).show()
                        }
                    }, { throwable ->
                        IdolSnackBar.make(findViewById(android.R.id.content), throwable.message).show()
                    }
                )
            }
        } else {
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<List<TagModel>>() {}.type

            val tags: MutableList<TagModel> = gson.fromJson(Util.getPreference(this@NewCommentActivity, Const.BOARD_TAGS), listType)
            tags.add(TagModel(id = 8))

            val isHasTitle = tags.firstOrNull { it.id == mArticle?.tagId }

            val intent =
                if (isHasTitle != null) {
                    WriteSmallTalkActivity.createIntent(
                        this@NewCommentActivity, mArticle!!.idol, mArticle!!.tagId
                    )
                } else {
                    WriteArticleActivity.createIntent(
                        this@NewCommentActivity, mArticle!!.idol, mArticle!!.tagId
                    )
                }
            intent.putExtra(Const.EXTRA_ARTICLE, mArticle)
            startActivityForResult(intent, RequestCode.ARTICLE_EDIT.value)
        }
    }

    private fun getArticleDetail(articleModel: ArticleModel?) {
        lifecycleScope.launch {
            articlesRepository.getArticle(
                articleModel?.resourceUri ?: return@launch,
                { response ->
                    try {
                        if(!response.optBoolean("success")){
                            IdolSnackBar.make(findViewById(android.R.id.content), response.optString("msg") ?: "").show()
                            return@getArticle
                        }

                        val gson = IdolGson.getInstance(true)

                        mArticle = gson.fromJson(response.toString(), ArticleModel::class.java)

                        // 내가 가지고있는 articleModel.viewcount와 서버에서 보내준 articleModel.viewCount가 다를 경우, setResult 날려줌
                        if(articleModel.viewCount != mArticle?.viewCount){
                            val intent = Intent()
                            intent.putExtra(Const.EXTRA_ARTICLE, mArticle)
                            setResult(ResultCode.UPDATE_VIEW_COUNT.value, intent)
                        }
                        //모델 업데이트.
                        loadComments(mArticle)
                        recyclerviewAdapter.setArticle(mArticle)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, {
                    Util.closeProgress()
                }
            )
        }
    }

    fun clickTranslate(item: ArticleModel) {
        setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "community_translate") // 임시값 (확정 후 변경 예정)

        translateArticle(
            item,
            0,
            recyclerviewAdapter,
            articlesRepository)
    }

    override fun translateComment(item: CommentModel, position: Int) {
        setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "comment_translate") // 임시값 (확정 후 변경 예정)

        commentTranslationHelper.clickTranslate(
            item = item,
            adapter = recyclerviewAdapter,
            position = position
        )
    }

    private fun shareArticle() {
        var msg: String? = mArticle!!.title

        if (mAdapterType == NewCommentAdapter.TYPE_SMALL_TALK) {
            setUiActionFirebaseGoogleAnalyticsActivity(
                GaAction.SHARE_SMALL_TALK.actionValue,
                GaAction.SHARE_SMALL_TALK.label
            )
        } else {
            setUiActionFirebaseGoogleAnalyticsActivity(
                GaAction.COMMENT_ARTICLE_SHARE.actionValue,
                GaAction.COMMENT_ARTICLE_SHARE.label
            )
        }
        val params = listOf(LinkStatus.ARTICLES.status, mArticle!!.id.toString())
        val url = LinkUtil.getAppLinkUrl(
            context = this@NewCommentActivity,
            params = params,
            querys = null
        )
        UtilK.linkStart(this@NewCommentActivity, url = url, msg = msg?.trimNewlineWhiteSpace())
    }

    companion object {
        private const val PARAM_ARTICLE = "article";
        private const val PARAM_POSITION = "article_position"
        private const val PARAM_SCROLL_TO_TOP = "paramScrollToTop"
        private const val PARAM_EVENT_HEART = "event_heart"
        private const val PARAM_TAG_NAME = "tag_name"

        private const val PARAM_SCHEDULE_FLAG = "scheduleFlag"
        private const val PARAM_SCHEDULE = "schedule"
        private const val PARAM_SCHEDULE_IDOLS = "idols"
        private const val PARAM_SCHEDULE_IS_COMMENT_PUSH = "comment_push"

        private const val PARAM_ADAPTER_TYPE = "small_talk"

        const val PARAM_RESOURCE_URI = "resource_uri" // 1분간 댓 제한
        const val REQUEST_FEED_MODIFY = 10001

        fun createIntent(
            context: Context?,
            article: ArticleModel?,
            position: Int,
            isScrollToTop: Boolean,
            adapterType : Int = NewCommentAdapter.TYPE_ARTICLE,
            tagName: String = ""
        ): Intent {
            val intent = Intent(context, NewCommentActivity::class.java)
            intent.putExtra(PARAM_ARTICLE, article)
            intent.putExtra(PARAM_POSITION, position)
            intent.putExtra(PARAM_SCROLL_TO_TOP, isScrollToTop)
            intent.putExtra(PARAM_ADAPTER_TYPE, adapterType)
            intent.putExtra(PARAM_TAG_NAME, tagName)
            return intent
        }

        fun createIntent(
            context: Context?,
            article: ArticleModel?,
            position: Int,
            schedule: ScheduleModel?,
            flag: Boolean,
            mIds: HashMap<Int, String>?,
            isCommentPush: Boolean
        ): Intent? {
            val intent = Intent(context, NewCommentActivity::class.java)
            intent.putExtra(PARAM_ARTICLE, article)
            intent.putExtra(PARAM_POSITION, position)
            intent.putExtra(PARAM_SCHEDULE, schedule)
            intent.putExtra(PARAM_SCHEDULE_FLAG, flag)
            intent.putExtra(PARAM_SCHEDULE_IDOLS, mIds)
            intent.putExtra(PARAM_SCHEDULE_IS_COMMENT_PUSH, isCommentPush)
            return intent
        }

    }

}
