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
import androidx.annotation.OptIn
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
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.activity.WriteArticleActivity
import net.ib.mn.adapter.FeedArticleAdapter
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.addon.InternetConnectivityManager
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.FragmentFeedActivityBinding
import net.ib.mn.dialog.ArticleRemoveDialogFragment
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.ReportDialogFragment
import net.ib.mn.dialog.VoteDialogFragment
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
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
class FeedActivityFragment : BaseFragment(),
        FeedArticleAdapter.OnArticleClickListener,
        FeedArticleAdapter.SmallTalkListener,
        BaseDialogFragment.DialogResultHandler {

    private var mAccount: IdolAccount? = null
    private lateinit var mActivity: FeedActivity
    var mContext: Context? = null
    private var userId : Int = 0
    var mFeedArticleAdapter: FeedArticleAdapter? = null
    var mFeedScrollListener: EndlessRecyclerViewScrollListener? = null

    // 움짤 검은화면 방지
    private var activeThumbnailView: View? = null
    private var activeExoPlayerView: View? = null

    // lazy image loading
    internal var lazyImageLoadHandler = Handler()
    internal var lazyImageLoadRunnable: Runnable? = null

    @Inject
    lateinit var usersRepository: UsersRepository

    var binding: FragmentFeedActivityBinding? = null // FeedActivity에서 먼저 참조하는 경우가 있어 nullable 처리

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
        binding = FragmentFeedActivityBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActivity = activity as FeedActivity
        mContext = activity as Context
        mAccount = IdolAccount.getAccount(mContext)
        val llm = LinearLayoutManager(mContext)
        mContext?.let {
            mFeedArticleAdapter = FeedArticleAdapter(
                mContext!!,
                mActivity.mFeedArticleList,
                this,
                lifecycleScope = lifecycleScope,
                false
            ) { model: ArticleModel, v: View, position: Int ->
                onArticleButtonClick(model, v, position)
            }
        }

        mFeedScrollListener = object : EndlessRecyclerViewScrollListener(llm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                mActivity.loadMoreArticles()
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

                super.onScrolled(recyclerView, dx, dy)
            }
        }
        binding?.rvFeedActivity?.layoutManager = llm
        binding?.rvFeedActivity?.adapter = mFeedArticleAdapter
        binding?.rvFeedActivity?.addOnScrollListener(mFeedScrollListener as EndlessRecyclerViewScrollListener)

        setRecyclerViewListener()
    }

    override fun onResume() {
        // 움짤 검은화면 방지
        mContext?.let {
            LocalBroadcastManager.getInstance(mContext!!).registerReceiver(
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

    @OptIn(UnstableApi::class)
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
                }else {
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
            RequestCode.ARTICLE_COMMENT.value -> {//피드에서 comment화면으로 갔다가 돌아올때
                if (data != null) {
                    val article = data.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel
                    val position = getArticlePosition(article.id)

                    when (resultCode) {
                        ResultCode.REMOVED.value -> {//피드에서 comment화면으로 갔다가 돌아올때 ->  게시글 삭제한 경우
                            if (position >= 0) {
                                mActivity.mFeedArticleList.removeAt(position)
                                mFeedArticleAdapter?.notifyItemRemoved(position)
                            }
                            val intent = Intent()
                            //삭제한  article position 값을 넣어서 피드에서 바로 커뮤로 돌아갈때 용.
                            intent.putExtra(PARAM_ARTICLE_POSITION,position)
                            activity?.setResult(FEED_ARTICLE_REMOVE,intent)
                        }
                        ResultCode.VOTED.value,
                        ResultCode.COMMENT_REMOVED.value,
                        ResultCode.EDITED.value -> {//피드에서 comment화면으로 갔다가 돌아올때 ->  게시글 수정한 경우
                            if (position >= 0) {
                                mActivity.mFeedArticleList[position] = article
                                mFeedArticleAdapter?.notifyItemChanged(position)
                            }
                            val intent = Intent()
                            intent.putExtra(PARAM_ARTICLE_POSITION,position)//바뀐 article positoon 값을 넣어서 피드에서 바로 커뮤로 돌아갈때 용.
                            intent.putExtra(Const.EXTRA_ARTICLE,article)//바뀐 article 값을 넣어서 전달해준다.
                            activity?.setResult(FEED_ARTICLE_MODIFY, intent)
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
                            mActivity.mFeedArticleList[position] = article
                            mFeedArticleAdapter?.notifyItemChanged(position)
                        }
                        val intent = Intent()
                        intent.putExtra(PARAM_ARTICLE_POSITION,position)//바뀐 article positoon 값을 넣어서 피드에서 바로 커뮤로 돌아갈때 용.
                        intent.putExtra(Const.EXTRA_ARTICLE,article)//바뀐 article 값을 넣어서 전달해준다.
                        activity?.setResult(FEED_ARTICLE_MODIFY,intent)
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
                        val model = mActivity.mFeedArticleList[position]
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
                        mActivity.mFeedArticleList.removeAt(position)
                        mFeedArticleAdapter?.notifyItemRemoved(position)

                        val intent = Intent()
                        //삭제한  article position 값을 넣어서 피드에서 바로 커뮤로 돌아갈때 용.
                        intent.putExtra(PARAM_ARTICLE_POSITION, article.id)
                        intent.putExtra(CommunityActivity.PARAM_ARTICLE_REPORT_POSITION, position)
                        activity?.setResult(ResultCode.REPORTED.value,intent)

                        if (mActivity.mFeedArticleList.size == 0) {
                            showEmpty(userId)
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
                    val article = data.getSerializableExtra(PARAM_ARTICLE) as ArticleModel

                    if (position >= 0) {
                        mActivity.mFeedArticleList.removeAt(position)
                        mFeedArticleAdapter?.notifyItemRemoved(position)

                        val intent = Intent()
                        //삭제한  article position 값을 넣어서 피드에서 바로 커뮤로 돌아갈때 용.
                        intent.putExtra(PARAM_ARTICLE_POSITION,position)
                        intent.putExtra(PARAM_ARTICLE, article)
                        activity?.setResult(FEED_ARTICLE_REMOVE,intent)

                        if (mActivity.mFeedArticleList.size == 0) {
                            showEmpty(userId)
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
                setUiActionFirebaseGoogleAnalyticsFragment(Const.ANALYTICS_BUTTON_PRESS_ACTION,
                        "comment_feed")
                activity?.startActivityForResult(NewCommentActivity.createIntent(mContext, model, position, false),
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
//                                        Util.showDefaultIdolDialogWithBtn2(mContext,
//                                                null,
//                                                getString(R.string.msg_go_to_add_heart),
//                                                {
//                                                    Util.closeIdolDialog()
//                                                    activity?.startActivity(HeartPlusActivity.createIntent(mContext))
//                                                },
//                                                { Util.closeIdolDialog() })
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
            R.id.btn_share -> {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.SEARCH_SHARE.actionValue,
                    GaAction.SEARCH_SHARE.label
                )

                val url = LinkUtil.getAppLinkUrl(
                    context = context ?: return,
                    listOf(LinkStatus.ARTICLES.status, model.id.toString())
                )
                UtilK.linkStart(context = context, url = url)
            }
            R.id.iv_view_more -> {
                UtilK.clickMore(context, model) { showEdit, showRemove, showReport, showShare ->
                    val sheet = ArticleViewMoreBottomSheetFragment.newInstance(
                        showEdit, showRemove, showReport, model.isMostOnly != "Y",
                        onClickEdit = {
                            clickEdit(model)
                        },
                        onClickDelete = {
                            clickRemove(model, position)
                        }, onClickReport = {
                            clickReport(model, position)
                        }) {
                        setUiActionFirebaseGoogleAnalyticsFragment(
                            GaAction.SEARCH_SHARE.actionValue,
                            GaAction.SEARCH_SHARE.label
                        )

                        val url = LinkUtil.getAppLinkUrl(
                            context = context ?: return@newInstance,
                            listOf(LinkStatus.ARTICLES.status, model.id.toString())
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

                setUiActionFirebaseGoogleAnalyticsFragment(Const.ANALYTICS_BUTTON_PRESS_ACTION,
                        "search_report")
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
                        val color = "#" + Integer.toHexString(ContextCompat.getColor(requireContext(), R.color.main)).substring(2)
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

                    activity?.startActivity(mContext?.let { FeedActivity.createIntent(it, model.user) })
                }
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

            if (binding?.rvFeedActivity != null) {
                // 리스트뷰의 화면상 위치 구해서
                binding?.rvFeedActivity!!.getLocationInWindow(location)
//            mListView.getLocationInWindow(location)
                val listviewTop = location[1]
                val listviewBottom = listviewTop + binding?.rvFeedActivity!!.height

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

    private fun getArticlePosition(articleId: String?): Int {
        val position = mActivity.mFeedArticleList.withIndex().find { it.value.id == articleId }?.index
        if (position != null) return position
        return -1
    }

    fun hideEmpty(userId: Int) {
        val binding = binding ?: return
        mContext?.let {
            if (!UtilK.isUserNotBlocked(mContext!!, userId)) {
                appBarState(binding.llUserBlock)
                binding.llPrivacy?.visibility = View.GONE
                binding.tvEmpty?.visibility = View.GONE
                binding.rvFeedActivity?.visibility = View.GONE
                binding.llUserBlock?.visibility = View.VISIBLE
            } else {
                binding.llPrivacy?.visibility = View.GONE
                binding.tvEmpty?.visibility = View.GONE
                binding.rvFeedActivity?.visibility = View.VISIBLE
                binding.llUserBlock?.visibility = View.GONE
            }
        }
    }

    //작성한 글이 없음
    fun showEmpty(userId: Int) {
        val binding = binding ?: return
        this.userId = userId
        mContext?.let {
            if (!UtilK.isUserNotBlocked(mContext!!, userId)) {
                appBarState(binding.llUserBlock)
                binding.llPrivacy?.visibility = View.GONE
                binding.tvEmpty?.visibility = View.GONE
                binding.rvFeedActivity?.visibility = View.GONE
                binding.llUserBlock?.visibility = View.VISIBLE
            } else {
                binding.llPrivacy?.visibility = View.GONE
                binding.tvEmpty?.visibility = View.VISIBLE
                binding.rvFeedActivity?.visibility = View.GONE
                binding.llUserBlock?.visibility = View.GONE
                showExpandedEmpty()
            }
        }
    }

    fun showPrivacy(userId : Int) {
        val binding = binding ?: return
        mContext?.let {
            if (!UtilK.isUserNotBlocked(mContext!!, userId)) {
                appBarState(binding.llUserBlock)
                binding.llPrivacy?.visibility = View.GONE
                binding.tvEmpty?.visibility = View.GONE
                binding.rvFeedActivity?.visibility = View.GONE
                binding.llUserBlock?.visibility = View.VISIBLE
            } else {
                appBarState(binding.llPrivacy)
                binding.llPrivacy?.visibility = View.VISIBLE
                binding.tvEmpty?.visibility = View.GONE
                binding.rvFeedActivity?.visibility = View.GONE
                binding.llUserBlock?.visibility = View.GONE
            }
        }
    }

    fun appBarState(linearLayoutCompat: LinearLayoutCompat?){
        val appbar = activity?.findViewById<ControllableAppBarLayout>(R.id.appbar)
        if (appbar != null) {
            if (appbar.state == ControllableAppBarLayout.State.EXPANDED) {
                val lp = linearLayoutCompat?.layoutParams
                val metrics = this.resources.displayMetrics
                lp?.height = metrics.heightPixels - appbar.height
                linearLayoutCompat?.layoutParams = lp
            }
        }
    }

    fun showCollpasedEmpty() {
        binding?.tvEmpty?.layoutParams =
                LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT)
    }

    fun showExpandedEmpty() {
        val binding = binding ?: return
        val appbar = activity?.findViewById<ControllableAppBarLayout>(R.id.appbar)
        if (appbar != null) {
            if (appbar.state == ControllableAppBarLayout.State.EXPANDED) {
                val lp = binding.tvEmpty?.layoutParams
                val metrics = this.resources.displayMetrics
                lp?.height = metrics.heightPixels - appbar.height
                binding.tvEmpty?.layoutParams = lp
            }
        }
    }

    fun clickRemove(model: ArticleModel, position: Int){
        setUiActionFirebaseGoogleAnalyticsFragment(Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "search_delete")
        if (activity != null) {
            Util.showProgress(mContext)
            val removeDlg = ArticleRemoveDialogFragment
                .getInstance(model, position)
            removeDlg.setActivityRequestCode(RequestCode.ARTICLE_REMOVE.value)
            removeDlg.show(requireActivity().supportFragmentManager, "remove")
        }
    }

    fun clickEdit(model: ArticleModel){
        setUiActionFirebaseGoogleAnalyticsFragment(Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "search_edit")

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

    fun clickReport(model: ArticleModel, position: Int){
            val account = IdolAccount.getAccount(mContext)
            if (account == null && Util.mayShowLoginPopup(activity)) return

            setUiActionFirebaseGoogleAnalyticsFragment(Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "search_report")
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
                    val color = "#" + Integer.toHexString(ContextCompat.getColor(requireContext(), R.color.main)).substring(2)
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
        const val PARAM_ARTICLE = "article"
        const val PARAM_ARTICLE_POSITION = "article_position"
        const val REQ_RESOLVE_SERVICE_MISSING = 2
        const val FEED_ARTICLE_MODIFY = 100
        const val FEED_ARTICLE_REMOVE = 200


        fun getInstance(): FeedActivityFragment {
            return FeedActivityFragment()
        }
    }
}
