package net.ib.mn.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.activity.WriteArticleActivity
import net.ib.mn.adapter.FeedArticleAdapter
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.addon.InternetConnectivityManager
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.article.ArticlesRepositoryImpl
import net.ib.mn.databinding.FragmentFeedCommentBinding
import net.ib.mn.dialog.ArticleRemoveDialogFragment
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.ReportDialogFragment
import net.ib.mn.dialog.VoteDialogFragment
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.safeSetImageBitmap
import net.ib.mn.view.ControllableAppBarLayout
import net.ib.mn.view.EndlessRecyclerViewScrollListener
import net.ib.mn.view.ExodusImageView
import javax.inject.Inject


@AndroidEntryPoint
@UnstableApi
class FeedCommentFragment : BaseFragment(),
        FeedArticleAdapter.OnArticleClickListener,
        FeedArticleAdapter.SmallTalkListener,
        BaseDialogFragment.DialogResultHandler {

    private var mUser: UserModel? = null
    private var userId : Int? = 0
    private var mAccount: IdolAccount? = null
    private lateinit var mActivity: FeedActivity
    var mContext: Context? = null
    var mFeedArticleAdapter: FeedArticleAdapter? = null

    // 움짤 검은화면 방지
    private var activeThumbnailView: View? = null
    private var activeExoPlayerView: View? = null

    // lazy image loading
    internal var lazyImageLoadHandler = Handler()
    internal var lazyImageLoadRunnable: Runnable? = null

    var binding: FragmentFeedCommentBinding? = null // FeedActivity에서 먼저 참조하는 경우가 있어 nullable 처리

    @Inject
    lateinit var articlesRepository: ArticlesRepositoryImpl
    @Inject
    lateinit var usersRepository: UsersRepository

    private val mBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (activeThumbnailView != null) {
                activeThumbnailView!!.visibility = View.GONE
                Util.log("*** hide thumbnail")
            }
            if (activeExoPlayerView != null) {
                activeExoPlayerView!!.visibility = View.VISIBLE

                val shutterId = resources.getIdentifier("exo_shutter", "id", requireContext().packageName)
                val shutter = activeExoPlayerView!!.findViewById<View>(shutterId)
                Util.log(">>>>> COMMU: shutter visibility: " + shutter.visibility + " alpha:" + shutter.alpha)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        mGlideRequestManager = Glide.with(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFeedCommentBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mUser = arguments?.getSerializable(FeedActivity.PARAM_USER) as UserModel
        mActivity = activity as FeedActivity
        mContext = activity as Context
        mAccount = IdolAccount.getAccount(mContext)
        mContext?.let {
            mFeedArticleAdapter = FeedArticleAdapter(mContext!!,
                mActivity.mFeedCommentArticleList,
                this,
                lifecycleScope = lifecycleScope,
            ) { model: ArticleModel, v: View, position: Int ->
                onArticleButtonClick(model, v, position)
            }
        }
        binding?.rvFeedComment?.adapter = mFeedArticleAdapter

        val llm = LinearLayoutManager(activity)
        binding?.rvFeedComment?.layoutManager = llm
        val scrollListener = object : EndlessRecyclerViewScrollListener(llm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                loadMoreActivities()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    // 움짤
                    for (listItemIndex in 0..llm.findLastVisibleItemPosition() - llm.findFirstVisibleItemPosition()) {
                        if (newState == -1) {
                            checkVisibility(recyclerView, listItemIndex)
                        }
                    }

                    if (lazyImageLoadRunnable != null) {
                        lazyImageLoadHandler.removeCallbacks(lazyImageLoadRunnable!!)
                    }

                    val isDataSavingMode = Util.getPreferenceBool(mContext, Const.PREF_DATA_SAVING, false)
                            && !InternetConnectivityManager.getInstance(mContext).isWifiConnected

                    if (!isDataSavingMode) {
                        lazyImageLoadRunnable = object : Runnable {
                            override fun run() {
                                for (listItemIndex in 0..llm.findLastVisibleItemPosition() - llm.findFirstVisibleItemPosition()) {
                                    val listItem = recyclerView.getChildAt(listItemIndex)
                                    val photo = listItem.findViewById<ExodusImageView>(R.id.attach_photo)
                                    if (photo?.loadInfo != null) {
                                        val url = photo.loadInfo

                                        Util.log(">>>>>>>>>>>>>>> loading original size image $url")
                                        // thumbnail이 다 로드되어야 원본 이미지 부른다
                                        if (photo.getLoadInfo(R.id.TAG_LOAD_LARGE_IMAGE) == true
                                                && photo.getLoadInfo(R.id.TAG_IS_UMJJAL) == false) {
                                            photo.post {
                                                mGlideRequestManager
                                                        .asBitmap()
                                                        .load(url)
                                                        .placeholder(photo.drawable)
                                                         .listener(object : RequestListener<Bitmap> {
                                                            override fun onLoadFailed(e: GlideException?,
                                                                                      model: Any?,
                                                                                      target: Target<Bitmap>,
                                                                                      isFirstResource: Boolean): Boolean {
                                                                return false
                                                            }

                                                            override fun onResourceReady(resource: Bitmap,
                                                                                         model: Any,
                                                                                         target: Target<Bitmap>,
                                                                                         dataSource: DataSource,
                                                                                         isFirstResource: Boolean): Boolean {

                                                                mActivity.runOnUiThread {
                                                                    // 썸네일 로딩 후 멈춘 상태에서 1초 후에 로딩 됨
                                                                    val loadInfo = photo.loadInfo as String?
                                                                    if (loadInfo != null && loadInfo == url) {
                                                                        photo.safeSetImageBitmap(activity, resource)
                                                                        Util.log(">>>>>>>>>>>>>>>:: image displayed $url")
                                                                    }
                                                                }

                                                                return false
                                                            }
                                                        })
                                                        .submit()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (lazyImageLoadRunnable != null) {
                            lazyImageLoadHandler.postDelayed(lazyImageLoadRunnable!!, 1000)
                        }

                        // 이미 고해상도 이미지를 보여준 경우라면 바로 보여주기
                        for (listItemIndex in 0..llm.findLastVisibleItemPosition() - llm.findFirstVisibleItemPosition()) {
                            val listItem = recyclerView.getChildAt(listItemIndex)
                            val photo = listItem.findViewById<ExodusImageView>(R.id.attach_photo)
                            if (photo?.loadInfo != null
                                    && photo.getLoadInfo(R.id.TAG_IS_UMJJAL) == false) {
                                val url = photo.loadInfo as String

                                mGlideRequestManager
                                        .asBitmap()
                                        .load(url)
                                        .placeholder(photo.drawable)
                                        .into(photo)
                            }
                        }
                    }
                }

                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (lazyImageLoadRunnable != null) {
                    lazyImageLoadHandler.removeCallbacks(lazyImageLoadRunnable!!)
                }

                // 저사양 단말에서 이미지 보였다 안보였다 하는 현상 수정.
                activeThumbnailView = null
                activeExoPlayerView = null

                for (listItemIndex in 0..llm.findLastVisibleItemPosition() - llm.findFirstVisibleItemPosition()) {
                    checkVisibility(recyclerView, listItemIndex)
                }
            }
        }
        binding?.rvFeedComment?.addOnScrollListener(scrollListener)
        setRecyclerViewListener()
    }

    override fun onResume() {
        // 움짤 검은화면 방지
        mContext?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                mBroadcastReceiver, IntentFilter(Const.VIDEO_READY_EVENT))
        }
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        try {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mBroadcastReceiver)
        } catch (e: Exception) {
        }
    }

    private fun setRecyclerViewListener() {
        mFeedArticleAdapter?.setPhotoClickListener(object : ArticlePhotoListener {
            override fun widePhotoClick(model: ArticleModel, position: Int?) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.SEARCH_WIDEPHOTO.actionValue,
                    GaAction.SEARCH_WIDEPHOTO.label
                )

                if (activity == null || !isAdded) return

                if(model.files.isNullOrEmpty() || model.files.size < 2) {
                    WidePhotoFragment.getInstance(model).apply {
                        setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                    }.show(requireActivity().supportFragmentManager, "wide_photo")
                } else {
                    MultiWidePhotoFragment.getInstance(model, position?:0).apply {
                        setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                    }.show(requireActivity().supportFragmentManager, "wide_photo")
                }
            }

            override fun linkClick(link: String) {
                try {
                    val mIntent = Intent(activity, AppLinkActivity::class.java).apply {
                        data = Uri.parse(link)
                    }
                    startActivity(mIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        })
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.ARTICLE_COMMENT.value -> {
                if (data != null) {
                    val article = data.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel
                    val position = getArticlePosition(article.id)

                    when (resultCode) {
                        ResultCode.REMOVED.value -> {
                            if (position >= 0) {
                                mActivity.mFeedCommentArticleList.removeAt(position)
                                mFeedArticleAdapter?.notifyItemRemoved(position)

                                if (mActivity.mFeedCommentArticleList.size == 0) {
                                    showEmpty()
                                }
                            }
                        }
                        ResultCode.VOTED.value,
                        ResultCode.COMMENT_REMOVED.value,
                        ResultCode.EDITED.value -> {
                            if (position >= 0 && mUser != null) {
                                compareCertainFeedComment(mUser!!, position, article.id)
                            } else {
                                // 댓글이 존재하지 않았던 게시물의 경우 추가해줌
                                if (mActivity.mFeedCommentArticleList.size > 0) {
                                    for (i in 0 until mActivity.mFeedCommentArticleList.size) {
                                        if (mActivity.mFeedCommentArticleList[i].createdAt <= article.createdAt) {
                                            mActivity.mFeedCommentArticleList.add(i, article)
                                            mFeedArticleAdapter?.notifyItemInserted(i)
                                            break
                                        }
                                    }
                                } else {
                                    mActivity.mFeedCommentArticleList.add(article)
                                    mFeedArticleAdapter?.notifyItemInserted(mActivity.mFeedCommentArticleList.size - 1)
                                }
                                hideEmpty()
                            }
                        }
                        ResultCode.ARTICLE_LIKE_EXCEPTION.value -> {
                            mActivity.postArticleLike(article)
                        }
                        ResultCode.UPDATE_LIKE_COUNT.value, ResultCode.COMMENTED.value -> {
                            mActivity.getArticleResource(article.resourceUri)
                        }
                    }
                }
            }
            RequestCode.ARTICLE_EDIT.value -> {
                if (data != null) {
                    val article = data.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel
                    val position = getArticlePosition(article.id)

                    if (resultCode == ResultCode.EDITED.value) {
                        if (position >= 0) {
                            mActivity.mFeedCommentArticleList[position] = article
                            mFeedArticleAdapter?.notifyItemChanged(position)
                        }
                    }
                }
            }
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.ARTICLE_REPORT.value -> {
                if (resultCode == ResultCode.REPORTED.value) {
                    val article = data?.getSerializableExtra(FeedActivity.PARAM_ARTICLE) as ArticleModel
                    val position = getArticlePosition(article.id)

                    if (position >= 0) {
                        val model = mActivity.mFeedCommentArticleList[position]
                        model.reportCount = model.reportCount + 1
                        mFeedArticleAdapter?.notifyItemChanged(position)

                        val account = IdolAccount.getAccount(mContext)
                        if (account != null) {
                            val prefs = PreferenceManager
                                    .getDefaultSharedPreferences(mContext)
                            val editor = prefs.edit()
                            val reportedArticles = prefs.getStringSet(
                                    account.email + "_did_report",
                                    HashSet())
                            reportedArticles!!.add(model.resourceUri)
                            editor.putStringSet(account.email + "_did_report",
                                    reportedArticles).apply()
                        }
                        mActivity.mFeedCommentArticleList.removeAt(position)
                        mFeedArticleAdapter?.notifyItemRemoved(position)

                        if (mActivity.mFeedCommentArticleList.size == 0) {
                            showEmpty()
                        }
                        //article 차단 목록 추가
                        context?.let { UtilK.addArticleReport(it, article.id) }
                    }
                }
            }
            RequestCode.ARTICLE_REMOVE.value -> {
                Util.closeProgress()

                if (resultCode == ResultCode.REMOVED.value) {
                    val position = getArticlePosition(data!!.getStringExtra(FeedActivity.PARAM_ARTICLE_ID))

                    if (position >= 0) {
                        mActivity.mFeedCommentArticleList.removeAt(position)
                        mFeedArticleAdapter?.notifyItemRemoved(position)

                        if (mActivity.mFeedCommentArticleList.size == 0) {
                            showEmpty()
                        }
                    }
                }
            }
        }
    }

    override fun smallTalkItemClicked(articleModel: ArticleModel, position: Int) {
        activity?.startActivityForResult(NewCommentActivity.createIntent(requireActivity(), articleModel,
            position, false, NewCommentAdapter.TYPE_SMALL_TALK),
            RequestCode.ARTICLE_COMMENT.value)
    }

    override fun onArticleButtonClick(model: ArticleModel, v: View?, position: Int) {
        when (v?.id) {
            R.id.footer_comment,
            R.id.ll_comment_count,
            R.id.comment_count_icon,
            R.id.comment_count -> {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.SEARCH_COMMENT.actionValue,
                    GaAction.SEARCH_COMMENT.label
                )
                activity?.startActivityForResult(
                    NewCommentActivity.createIntent(mContext, model, position, false),
                        RequestCode.ARTICLE_COMMENT.value)
            }
            R.id.footer_heart -> {
                if (Util.mayShowLoginPopup(activity)) return

                Util.showProgress(mContext)
                lifecycleScope.launch {
                    usersRepository.isActiveTime(
                        { response ->
                            Util.closeProgress()
                            if (response.optBoolean("success")) {
                                val gcode = response.optInt("gcode")
                                if (response.optString("active") == Const.RESPONSE_Y) {

                                    if (response.optInt("total_heart") == 0) {
                                        Util.showChargeHeartDialog(mContext)
                                    } else {
                                        if (response.optString("vote_able").equals(
                                                Const.RESPONSE_Y, ignoreCase = true)) {
                                            voteHeart(model, position,
                                                response.optLong("total_heart"),
                                                response.optLong("free_heart"))
                                        } else {
                                            if (gcode == Const.RESPONSE_IS_ACTIVE_TIME_1) {
                                                Toast.makeText(mContext, getString(
                                                    R.string.response_users_is_active_time_over),
                                                    Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(mContext,
                                                    getString(R.string.msg_not_able_vote),
                                                    Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    val start = Util.convertTimeAsTimezone(
                                        response.optString("begin"))
                                    val end = Util.convertTimeAsTimezone(
                                        response.optString("end"))
                                    val unableUseTime = String.format(
                                        getString(R.string.msg_unable_use_vote), start, end)

                                    Util.showIdolDialogWithBtn1(mContext, null,
                                        unableUseTime
                                    ) { Util.closeIdolDialog() }
                                }
                            } else { // success is false!
                                UtilK.handleCommonError(mContext, response)
                            }
                        }, {
                            Util.closeProgress()
                            Toast.makeText(mContext,
                                R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            R.id.iv_view_more -> {
                UtilK.clickMore(context, model) { showEdit, showRemove, showReport, showShare ->
                    val sheet = ArticleViewMoreBottomSheetFragment.newInstance(
                        showEdit,
                        showRemove,
                        showReport,
                        false,
                        onClickEdit = {
                            clickEdit(model)
                        },
                        onClickDelete = {
                            clickRemove(model, position)
                        }, onClickReport =  {
                            clickReport(model, position)
                        }) {
                        setUiActionFirebaseGoogleAnalyticsFragment(
                            GaAction.SEARCH_SHARE.actionValue,
                            GaAction.SEARCH_SHARE.label
                        )

                        val url = LinkUtil.getAppLinkUrl(
                            context = context ?: return@newInstance,
                            params = listOf(LinkStatus.ARTICLES.status, model.id.toString())
                        )
                        UtilK.linkStart(context = context, url = url)
                    }

                    val oldFrag = childFragmentManager.findFragmentByTag(ARTICLE_BOTTOM_SHEET_TAG)
                    if (oldFrag == null) {
                        sheet.show(childFragmentManager, ARTICLE_BOTTOM_SHEET_TAG)
                    }
                }
            }
            R.id.footer_report -> {
                val account = IdolAccount.getAccount(mContext)
                if (account == null && Util.mayShowLoginPopup(activity)) return

                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.SEARCH_REPORT.actionValue,
                    GaAction.SEARCH_REPORT.label
                )
                val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
                val reportedArticles = prefs.getStringSet((account?.email ?: "") + "_did_report", HashSet())
                if (reportedArticles!!.contains(model.resourceUri)) {
                    Toast.makeText(mContext,
                            R.string.failed_to_report__already_reported,
                            Toast.LENGTH_SHORT).show()
                    return
                }

                // config/self 제거하고 미리 받아놓은 값 사용
                val reportHeart = ConfigModel.getInstance(mContext).reportHeart
                val report = ReportDialogFragment.getInstance(model, position)
                val articleIdol = model.idol

                // 하트 차감 수가 0일 때
                if (reportHeart == 0
                        // 지식돌, 자유게시판에서는 무료로 신고 가능
                        || articleIdol?.getId() == Const.IDOL_ID_KIN
                        || articleIdol?.getId() == Const.IDOL_ID_FREEBOARD
                        // 내 커뮤이면서
                        || (account?.userModel?.most?.getId() == articleIdol?.getId())
                        // 최애가 없는 사람 글과
                        && (model.user != null
                                && (model.user.most == null
                                // 커뮤니티가 최애가 아닌 사람의 글도 무료로 신고 가능
                                || (model.user.most != null
                                && model.user.most?.getId() != articleIdol?.getId())))) {
                    report.setMessage(HtmlCompat.fromHtml(getString(R.string.warning_report_hide_article),
                            HtmlCompat.FROM_HTML_MODE_LEGACY))
                } else {

                    if (reportHeart > 0) {
                        val color = "#" + Integer.toHexString(ContextCompat.getColor(mContext!!, R.color.main)).substring(2)
                        val msg = String.format(resources.getString(R.string.warning_report_lose_heart), "<FONT color=$color>$reportHeart</FONT>")
                        val spanned = HtmlCompat.fromHtml(msg, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        report.setMessage(spanned)
                    }
                }

                if (activity != null) {
                    report.setActivityRequestCode(RequestCode.ARTICLE_REPORT.value)
                    report.show(requireActivity().supportFragmentManager, "report")
                }
            }
            R.id.btn_remove -> {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.SEARCH_DELETE.actionValue,
                    GaAction.SEARCH_DELETE.label
                )
                if (activity != null) {
                    Util.showProgress(mContext)
                    val removeDlg = ArticleRemoveDialogFragment
                            .getInstance(model, position)
                    removeDlg.setActivityRequestCode(RequestCode.ARTICLE_REMOVE.value)
                    removeDlg.show(requireActivity().supportFragmentManager, "remove")
                }
            }
            R.id.btn_edit -> {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.SEARCH_EDIT.actionValue,
                    GaAction.SEARCH_EDIT.label
                )

                if (Const.FEATURE_WRITE_RESTRICTION) {
                    // 집계시간에는 수정도 불가
                    lifecycleScope.launch {
                        usersRepository.isActiveTime(
                            { response ->
                                if (response.optBoolean("success")) {
                                    if (response.optString("active") == Const.RESPONSE_Y) {
                                        val intent = WriteArticleActivity.createIntent(requireContext(), model.idol)
                                        intent.putExtra(Const.EXTRA_ARTICLE, model)
                                        activity?.startActivityForResult(intent, RequestCode.ARTICLE_EDIT.value)
                                    } else {
                                        val start = Util.convertTimeAsTimezone(response.optString("begin"))
                                        val end = Util.convertTimeAsTimezone(response.optString("end"))
                                        val unableUseTime = String.format(getString(R.string.msg_unable_use_write), start, end)

                                        Util.showIdolDialogWithBtn1(mContext, null, unableUseTime) { Util.closeIdolDialog() }
                                    }
                                } else { // success is false!
                                    UtilK.handleCommonError(mContext, response)
                                }
                            }, {
                                Toast.makeText(mContext, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } else {
                    val intent = WriteArticleActivity.createIntent(requireContext(), model.idol)
                    intent.putExtra(Const.EXTRA_ARTICLE, model)
                    activity?.startActivityForResult(intent, RequestCode.ARTICLE_EDIT.value)
                }
            }
            R.id.photo, R.id.name -> {
                // 본인이 아닌 경우에만 새로운 feed를 염
                if (model.user?.id != mAccount?.userModel?.id) {
                    setUiActionFirebaseGoogleAnalyticsFragment(
                        GaAction.SEARCH_FEED.actionValue,
                        GaAction.SEARCH_FEED.label
                    )

                    activity?.startActivity(FeedActivity.createIntent(requireActivity(), model.user))
                }
            }
            R.id.icon_secret -> {
                Util.showDefaultIdolDialogWithBtn1(mContext,
                        null,
                        getString(if(BuildConfig.CELEB) R.string.actor_lable_show_private else R.string.lable_show_private)) { Util.closeIdolDialog() }
            }
            R.id.footer_like -> {
                mActivity.clickSubject.onNext(model)
            }
        }
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val resolveInfo = activity?.packageManager!!.queryIntentActivities(intent, 0)
        return !resolveInfo.isNullOrEmpty()
    }

    private fun checkVisibility(view: RecyclerView, listItemIndex: Int) {
        if (Build.VERSION.SDK_INT < Const.EXOPLAYER_MIN_SDK) {
            return
        }

        val listItem = view.getChildAt(listItemIndex) ?: return

        val thumbnailView = listItem.findViewById<ImageView>(R.id.attach_photo)
        val exoPlayerView = listItem.findViewById<PlayerView>(R.id.attach_exoplayer_view)

        //        Util.log("checkVisibility "+listItemIndex );
        if (exoPlayerView != null && exoPlayerView.visibility == View.VISIBLE) {
            val loopingSource = exoPlayerView.tag as ProgressiveMediaSource
            // videoview 가운데 부분의 화면상 위치 구하고
            val videoHeight = exoPlayerView.height
            val location = IntArray(2)
            exoPlayerView.getLocationInWindow(location)
            val y = location[1]
            val videoCenterY = y + videoHeight / 2

            // 리스트뷰의 화면상 위치 구해서
            if (binding?.rvFeedComment != null) {
                binding?.rvFeedComment!!.getLocationInWindow(location)
//            mListView.getLocationInWindow(location)
                val listviewTop = location[1]
                val listviewBottom = listviewTop + binding?.rvFeedComment!!.height

                // 화면에 조금이라도 걸쳐져 있으면
                if (y < listviewTop || y + videoHeight > listviewBottom) {
                    var player: ExoPlayer? = null
                    if (exoPlayerView.player != null) {
                        player = exoPlayerView.player as ExoPlayer
                    }
                    player?.stop()

                    exoPlayerView.player = null
                    if (thumbnailView.visibility == View.GONE) {
                        thumbnailView.visibility = View.VISIBLE
                        //                    Util.log(">>>>>>>>>>>>>> show THUMBNAIL "+listItemIndex);
                    }
                } else {
                    activeThumbnailView = thumbnailView
                    activeExoPlayerView = exoPlayerView

                    exoPlayerView.post {
                        var player: ExoPlayer? = null
                        if (exoPlayerView.player != null) {
                            player = exoPlayerView.player as ExoPlayer
                        }

                        if (player == null) {
                            player = mFeedArticleAdapter?.getPlayer()
                            player!!.playWhenReady = false
                            exoPlayerView.player = player
                            player!!.prepare(loopingSource)
                            player!!.playWhenReady = true
                        }

                        if (player!!.playWhenReady /* isPlaying */) {
                            if (thumbnailView.visibility == View.VISIBLE) {
                            }
                        } else {
                            player!!.prepare(loopingSource)
                            player!!.playWhenReady = true
                        }
                    }

                }
            }
        }
    }

    private fun voteHeart(model: ArticleModel, position: Int, totalHeart: Long, freeHeart: Long) {
        if (activity != null) {
            val fragment = VoteDialogFragment
                    .getArticleVoteInstance(model, position, totalHeart, freeHeart)
            fragment.setActivityRequestCode(RequestCode.ARTICLE_VOTE.value)
            fragment.show(requireActivity().supportFragmentManager, "community_vote")
        }
    }

    private fun loadMoreActivities() {
        val size = mActivity.mFeedCommentArticleList.size
        if (size % SIZE_OF_LIMIT == 0) {
            mActivity.getFeedComment(mUser!!, mActivity.mFeedCommentArticleList.size, SIZE_OF_LIMIT)
        }
    }

    private fun compareCertainFeedComment(user: UserModel, offset: Int, articleId: String) {
        // comment는 본인만 보이기 때문에 true로 넘김
        MainScope().launch {
            articlesRepository.getFeedActivity(
                user.id.toLong(),
                FeedActivity.PARAM_COMMENT,
                offset,
                1,
                true,
                { response ->
                    if (response?.optBoolean("success")!!) {
                        try {
                            val array = response.getJSONArray("objects")

                            if (array.length() > 0) {
                                val obj = array.getJSONObject(0)
                                val model = IdolGson.getInstance(true)
                                    .fromJson(obj.toString(), ArticleModel::class.java)

                                if (model.id == mActivity.mFeedCommentArticleList[offset].id) {
                                    mActivity.mFeedCommentArticleList[offset] = model
                                    mFeedArticleAdapter?.notifyItemChanged(offset)
                                } else {
                                    mActivity.mFeedCommentArticleList.removeAt(offset)
                                    mFeedArticleAdapter?.notifyItemRemoved(offset)
                                }
                            } else if (array.length() == 0 && mActivity.mFeedCommentArticleList.size == 1) {
                                // 하나만 있던 댓글이 지워지는 경우
                                if (mActivity.mFeedCommentArticleList[0].id == articleId) {
                                    mActivity.mFeedCommentArticleList.clear()
                                    mFeedArticleAdapter?.notifyItemRemoved(0)
                                    showEmpty()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                { throwable ->
                })
        }
    }



    private fun getArticlePosition(articleId: String?): Int {
        val position = mActivity.mFeedCommentArticleList.withIndex().find { it.value.id == articleId }?.index
        if (position != null) return position
        return -1
    }

    fun hideEmpty() {
        binding?.rvFeedComment?.visibility = View.VISIBLE
        binding?.tvEmpty?.visibility = View.GONE
    }

    fun showEmpty() {
        binding?.rvFeedComment?.visibility = View.GONE
        showExpandedEmpty()
        binding?.tvEmpty?.visibility = View.VISIBLE
    }

    fun showCollpasedEmpty() {
        binding?.tvEmpty?.layoutParams =
                LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT)
    }

    fun showExpandedEmpty() {
        val appbar = activity?.findViewById<ControllableAppBarLayout>(R.id.appbar)
        if (appbar != null) {
            if (appbar.state == ControllableAppBarLayout.State.EXPANDED) {
                val lp = binding?.tvEmpty?.layoutParams
                val metrics = this.resources.displayMetrics
                lp?.height = metrics.heightPixels - appbar.height
                binding?.tvEmpty?.layoutParams = lp
            }
        }
    }

    private fun clickRemove(model: ArticleModel, position: Int){
        setUiActionFirebaseGoogleAnalyticsFragment(
            GaAction.SEARCH_DELETE.actionValue,
            GaAction.SEARCH_DELETE.label
        )
        if (activity != null) {
            Util.showProgress(mContext)
            val removeDlg = ArticleRemoveDialogFragment
                .getInstance(model, position)
            removeDlg.setActivityRequestCode(RequestCode.ARTICLE_REMOVE.value)
            removeDlg.show(requireActivity().supportFragmentManager, "remove")
        }
    }

    private fun clickEdit(model: ArticleModel){
        setUiActionFirebaseGoogleAnalyticsFragment(
            GaAction.SEARCH_EDIT.actionValue,
            GaAction.SEARCH_EDIT.label
        )

        if (Const.FEATURE_WRITE_RESTRICTION) {
            // 집계시간에는 수정도 불가
            lifecycleScope.launch {
                usersRepository.isActiveTime(
                    { response ->
                        if (response.optBoolean("success")) {
                            if (response.optString("active") == Const.RESPONSE_Y) {
                                val intent = WriteArticleActivity.createIntent(requireContext(), model.idol)
                                intent.putExtra(Const.EXTRA_ARTICLE, model)
                                activity?.startActivityForResult(intent, RequestCode.ARTICLE_EDIT.value)
                            } else {
                                val start = Util.convertTimeAsTimezone(response.optString("begin"))
                                val end = Util.convertTimeAsTimezone(response.optString("end"))
                                val unableUseTime = String.format(getString(R.string.msg_unable_use_write), start, end)

                                Util.showIdolDialogWithBtn1(mContext, null, unableUseTime) { Util.closeIdolDialog() }
                            }
                        } else { // success is false!
                            UtilK.handleCommonError(mContext, response)
                        }
                    }, {
                        Toast.makeText(mContext, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else {
            val intent = WriteArticleActivity.createIntent(mActivity, model.idol)
            intent.putExtra(Const.EXTRA_ARTICLE, model)
            mActivity.startActivityForResult(intent, RequestCode.ARTICLE_EDIT.value)
        }
    }

    private fun clickReport(model: ArticleModel, position: Int){
        val account = IdolAccount.getAccount(mContext)
        if (account == null && Util.mayShowLoginPopup(activity)) return

        setUiActionFirebaseGoogleAnalyticsFragment(
            GaAction.SEARCH_REPORT.actionValue,
            GaAction.SEARCH_REPORT.label
        )
        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
        val reportedArticles = prefs.getStringSet((account?.email ?: "") + "_did_report", HashSet())
        if (reportedArticles!!.contains(model.resourceUri)) {
            Toast.makeText(mContext,
                R.string.failed_to_report__already_reported,
                Toast.LENGTH_SHORT).show()
            return
        }

        // config/self 제거하고 미리 받아놓은 값 사용
        val reportHeart = ConfigModel.getInstance(mContext).reportHeart
        val report = ReportDialogFragment.getInstance(model, position)
        val articleIdol = model.idol

        // 하트 차감 수가 0일 때
        if (reportHeart == 0
            // 지식돌, 자유게시판에서는 무료로 신고 가능
            || articleIdol?.getId() == Const.IDOL_ID_KIN
            || articleIdol?.getId() == Const.IDOL_ID_FREEBOARD
            // 내 커뮤이면서
            || (account?.userModel?.most?.getId() == articleIdol?.getId())
            // 최애가 없는 사람 글과
            && (model.user != null
                    && (model.user.most == null
                    // 커뮤니티가 최애가 아닌 사람의 글도 무료로 신고 가능
                    || (model.user.most != null
                    && model.user.most?.getId() != articleIdol?.getId())))) {
            report.setMessage(HtmlCompat.fromHtml(getString(R.string.warning_report_hide_article),
                HtmlCompat.FROM_HTML_MODE_LEGACY))
        } else {

            if (reportHeart > 0) {
                val color = "#" + Integer.toHexString(ContextCompat.getColor(mContext!!, R.color.main)).substring(2)
                val msg = String.format(resources.getString(R.string.warning_report_lose_heart), "<FONT color=$color>$reportHeart</FONT>")
                val spanned = HtmlCompat.fromHtml(msg, HtmlCompat.FROM_HTML_MODE_LEGACY)
                report.setMessage(spanned)
            }
        }

        if (activity != null) {
            report.setActivityRequestCode(RequestCode.ARTICLE_REPORT.value)
            report.show(requireActivity().supportFragmentManager, "report")
        }
    }

    companion object {

        const val PARAM_ARTICLE_POSITION = "article_position"
        const val SIZE_OF_LIMIT = 30
        const val REQ_RESOLVE_SERVICE_MISSING = 2

        fun getInstance(user: UserModel): FeedCommentFragment {
            val fragment = FeedCommentFragment()
            val args = Bundle()
            args.putSerializable(FeedActivity.PARAM_USER, user)
            fragment.arguments = args

            return fragment
        }
    }
}
