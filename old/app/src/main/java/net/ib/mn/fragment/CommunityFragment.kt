/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.BaseActivity.Companion.MEZZO_PLAYER_REQ_CODE
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.CommunityActivity.Companion.PARAM_ARTICLE
import net.ib.mn.activity.CommunityActivity.Companion.mType
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.activity.WebViewActivity
import net.ib.mn.activity.WriteArticleActivity
import net.ib.mn.adapter.CommunityAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.ReportRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.FragmentCommunityBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.VoteDialogFragment
import net.ib.mn.domain.usecase.UpdateIdolUseCase
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.listener.CommunityArticleListener
import net.ib.mn.listener.ImgTypeClickListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.UserModel
import net.ib.mn.model.toDomain
import net.ib.mn.utils.ApiCacheManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.EventBus
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.OrderByType
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Translation
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.safeSetImageBitmap
import net.ib.mn.utils.trimNewlineWhiteSpace
import net.ib.mn.utils.trimSubStringLimit
import net.ib.mn.view.ArticleRecyclerView
import net.ib.mn.view.ExodusImageView
import net.ib.mn.viewholder.CommunityArticleViewHolder
import net.ib.mn.viewmodel.CommunityActivityViewModel
import net.ib.mn.viewmodel.CommunityArticleViewModel
import org.json.JSONException
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class CommunityFragment : BaseFragment(), BaseDialogFragment.DialogResultHandler, Translation {

    lateinit var binding: FragmentCommunityBinding
    private var mContext: Context? = null

    @Inject
    lateinit var updateIdolUseCase: UpdateIdolUseCase
    @Inject
    lateinit var articlesRepository: ArticlesRepository
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    private var communityAdapter: CommunityAdapter? = null

    private val communityArticleViewModel: CommunityArticleViewModel by viewModels()
    private val communityActivityViewModel: CommunityActivityViewModel by activityViewModels()

    // 움짤 검은화면 방지
    private var activeThumbnailView: View? = null
    private var activeExoPlayerView: View? = null

    val lazyImageLoadHandler = Handler(Looper.getMainLooper())
    var lazyImageLoadRunnable: Runnable? = null

    private lateinit var mActivity: CommunityActivity
    @Inject
    lateinit var reportRepository: ReportRepositoryImpl
    @Inject
    lateinit var videoAdUtil: VideoAdUtil

    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (!intent?.action.equals(Const.VIDEO_READY_EVENT)) {
                return
            }

            if (activeThumbnailView != null) {
                activeThumbnailView!!.visibility = View.GONE
                Util.log("*** hide thumbnail")
            }
            if (activeExoPlayerView != null) {
                activeExoPlayerView!!.visibility = View.VISIBLE

                val shutterId = resources.getIdentifier("exo_shutter", "id", requireContext().packageName)
                val shutter = activeExoPlayerView!!.findViewById<View>(shutterId)
                Util.log(">>>>> COMMU: shutter visibility: ${shutter.visibility} alpha: ${shutter.alpha}")
            }
        }
    }

    override fun onResume() {
        // Prevent black screen when playing GIFs
        LocalBroadcastManager.getInstance(requireActivity()).apply {
            registerReceiver(mBroadcastReceiver, IntentFilter(Const.VIDEO_READY_EVENT))
            registerReceiver(mBroadcastReceiver, IntentFilter(Const.ARTICLE_SERVICE_UPLOAD))
        }
        onScrollStateChanged(binding.rvCommunity, -1)

        super.onResume()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mBroadcastReceiver)
        super.onPause()
    }

    override fun onDestroy() {
        binding.rvCommunity.clearOnScrollListeners()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_community, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
    }

    private fun init() {
        mActivity = activity as CommunityActivity

        getDataFromVM()
        setAdapter()

        if (!communityActivityViewModel.getIsWallpaperInit()) {
            if (communityActivityViewModel.getIsFromUpload()) return
            communityArticleViewModel.getCommunityArticles(context, communityActivityViewModel.idolModel.value, communityActivityViewModel.getIsMost())
        }
    }

    private fun getDataFromVM() {
        communityArticleViewModel.registerActivityResult(requireActivity(), this)

        //CommunityActivity를 isRecent true로 intent값을 넘겨줬을 경우, mOrderBy 최신순으로 변경해줌
        communityActivityViewModel.isRecent.observe(
            viewLifecycleOwner,
            SingleEventObserver {isRecent ->
                if(isRecent == true) {
                    communityArticleViewModel.setOrderBy(OrderByType.TIME.orderBy)
                    communityArticleViewModel.getCommunityArticles(context, communityActivityViewModel.idolModel.value, communityActivityViewModel.getIsMost())
                }
            }
        )

        communityActivityViewModel.isWallpaper.observe(
            viewLifecycleOwner,
            SingleEventObserver { imageOnly ->

                if (!imageOnly) {
                    return@SingleEventObserver
                }

                communityAdapter?.setImageOnly(true)
                communityAdapter?.setWallpaperOnly(true)
                communityArticleViewModel.setArticleStatus(
                    context,
                    communityActivityViewModel.idolModel.value,
                    communityActivityViewModel.getIsMost(),
                    imageOnly = true,
                    wallpaperOnly = true
                )
            }
        )

        // 게시글 작성 화면 갈 때
        communityActivityViewModel.articleWrite.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                if (it) {
                    communityArticleViewModel.startActivityResultLauncher.launch(
                        WriteArticleActivity.createIntent(requireActivity(), communityActivityViewModel.idolModel.value),
                    )
                }
            },
        )

        // 게시글 작성 후 왔을 때. 최신순으로 변경
        communityArticleViewModel.articleAdd.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                if (it) {
                    //게시글 작성 시 포지션 0번째로 이동
                    binding.rvCommunity.scrollToPosition(0)

                    communityArticleViewModel.setOrderBy(OrderByType.TIME.orderBy)
                    communityArticleViewModel.getCommunityArticles(context, communityActivityViewModel.idolModel.value, communityActivityViewModel.getIsMost())
                }
            },
        )

        // 게시글 수정, 투표, 댓글 추가, 댓그 삭제 시
        communityArticleViewModel.editArticle.observe(
            viewLifecycleOwner,
            SingleEventObserver { model ->
                val article = communityArticleViewModel.articleList.find { it.id == model.id }
                if (article != null) {
                    val show: String = if (model.isMostOnly == "Y") {
                        Const.SHOW_PRIVATE
                    } else {
                        Const.SHOW_PUBLIC
                    }
                    article.apply {
                        heart = model.heart
                        commentCount = model.commentCount
                        content = model.content
                        linkDesc = model.linkDesc
                        linkTitle = model.linkTitle
                        linkUrl = model.linkUrl
                        setIsMostOnly(show)
                    }
                    communityAdapter?.updateItem(article)
                }
            },
        )

        communityArticleViewModel.getArticleOnlyOne.observe(
            viewLifecycleOwner,
            SingleEventObserver { resourceUri ->
                communityArticleViewModel.loadArticleOnlyOne(context, resourceUri)
            },
        )

        // 게시글 삭제 시
        communityArticleViewModel.articleRemove.observe(
            viewLifecycleOwner,
            SingleEventObserver { model ->
                communityAdapter?.removeItem(model)
            },
        )

        // 피드화면에서 변경된 값이 있을 경우
        communityArticleViewModel.feedArticleModify.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                if (it) {
                    communityArticleViewModel.getCommunityArticles(context, communityActivityViewModel.idolModel.value, communityActivityViewModel.getIsMost())
                }
            },
        )

        communityActivityViewModel.clickCommunityTab.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                binding.rvCommunity.smoothScrollToPosition(0)
            }
        )

        communityActivityViewModel.newCommunityIntent.observe(
            viewLifecycleOwner,
            SingleEventObserver {isRecent ->
                if(isRecent == true){
                    binding.rvCommunity.scrollToPosition(0)
                    communityAdapter?.setImageOnly(false)
                    communityAdapter?.setWallpaperOnly(false)
                    communityArticleViewModel.setOrderBy(OrderByType.TIME.orderBy)
                    communityArticleViewModel.setArticleStatus(context, communityActivityViewModel.idolModel.value, isMost = communityActivityViewModel.getIsMost(), imageOnly = false, wallpaperOnly = false)
                }
            }
        )

        communityActivityViewModel.successBurningDay.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                communityAdapter?.updateIdolModel(it)

                Util.setPreference(
                    requireContext(),
                    Const.PREF_DAILY_IDOL_UPDATE,
                    ""
                )
            }
        )

        communityArticleViewModel.articleBlocked.observe(
            viewLifecycleOwner,
            SingleEventObserver { userId ->
                communityAdapter?.removeBlockItem(userId)
            })

        communityArticleViewModel.voteHeart.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                voteHeart(it["model"] as ArticleModel, it["position"] as Int, it["total_heart"] as Long, it["free_heart"] as Long)
            }
        )

        communityArticleViewModel.updateItem.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                communityAdapter?.updateItem(it)
            }
        )

        communityArticleViewModel.getCommunityArticles.observe(
            viewLifecycleOwner,
            SingleEventObserver { loadNextResource ->
                communityAdapter?.setOrderBy(communityArticleViewModel.getOrderBy())
                communityAdapter?.setItems(communityArticleViewModel.noticeList, communityArticleViewModel.articleList)
                with(binding) {
                    tvLoading.visibility = View.GONE
                    rvCommunity.visibility = View.VISIBLE
                }

                if (!loadNextResource) {
                    binding.rvCommunity.addOnLayoutChangeListener(object :
                        View.OnLayoutChangeListener {

                        override fun onLayoutChange(
                            v: View?,
                            left: Int,
                            top: Int,
                            right: Int,
                            bottom: Int,
                            oldLeft: Int,
                            oldTop: Int,
                            oldRight: Int,
                            oldBottom: Int,
                        ) {
                            binding.rvCommunity.removeOnLayoutChangeListener(this)
                            onScrollStateChanged(binding.rvCommunity, 0)
                        }
                    })
                }
                Util.closeProgress()
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MEZZO_PLAYER_REQ_CODE) {
            Util.handleVideoAdResult(
                activity as BaseActivity,
                false,
                true,
                requestCode,
                resultCode,
                data,
                "community_videoad",
            ) { adType ->
                videoAdUtil.onVideoSawCommon(activity as BaseActivity, true, adType, null)
            }
        }
    }

    // 어댑터 세팅
    private fun setAdapter() {
        binding.rvCommunity.setType(ArticleRecyclerView.TYPE_ARTICLE_RCY)
        communityAdapter = CommunityAdapter(requireContext(),
            this as BaseFragment,
            communityActivityViewModel.idolModel.value,
            communityArticleViewModel.getOrderBy(),
            mGlideRequestManager,
            communityArticleViewModel.noticeList,
            communityArticleViewModel.articleList,
            reportRepository,
            lifecycleScope,
            usersRepository, {
                startActivity(
                    WebViewActivity.createIntent(requireContext(), "notices", it.id.toInt(),
                    it.title, it.title, false))
            })

        communityAdapter?.setHasStableIds(true)

        with(binding) {
            rvCommunity.adapter = communityAdapter
            rvCommunity.setHasFixedSize(true)
        }
        setListenerEvent()
    }

    // Adapter 내에 있는 ClickListener 모음
    private fun setListenerEvent() {
        communityAdapter?.setPhotoClickListener(object : ArticlePhotoListener {
            override fun widePhotoClick(model: ArticleModel, position: Int?) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "community_widephoto",
                )


                val sortedFiles = model.files?.sortedBy { it.seq }

                if (sortedFiles.isNullOrEmpty()) {
                    return
                }

                if (sortedFiles.size < 2) {
                    WidePhotoFragment.getInstance(model).apply {
                        setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                    }.show(requireActivity().supportFragmentManager, "wide_photo")
                } else {
                    MultiWidePhotoFragment.getInstance(model, position ?: 0).apply {
                        setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                    }.show(requireActivity().supportFragmentManager, "wide_photo")
                }

            }

            override fun linkClick(link: String) {
                try {
                    val intent = Intent(activity, AppLinkActivity::class.java).apply {
                        data = Uri.parse(link)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })

        communityAdapter?.setItemEventListener(object : CommunityArticleListener {
            override fun filterSetCallBack(bottomSheetFragment: BottomSheetFragment) {
                setFilter(bottomSheetFragment)
            }

            override fun filterClickCallBack(label: String, orderBy: String) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    label,
                )
                communityArticleViewModel.setOrderBy(orderBy)
                communityArticleViewModel.getCommunityArticles(context, communityActivityViewModel.idolModel.value, communityActivityViewModel.getIsMost())
            }

            override fun heartClick(model: ArticleModel, position: Int) {
                if (Util.mayShowLoginPopup(requireActivity())) {
                    return
                }
                try {
                    Util.showProgress(context)
                    communityArticleViewModel.confirmUseApi(context, model, position)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun commentClick(model: ArticleModel, position: Int) {
                if (Util.mayShowLoginPopup(requireActivity())) {
                    return
                }
                setUiActionFirebaseGoogleAnalyticsFragment(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "community_comment",
                )
                communityArticleViewModel.startActivityResultLauncher.launch(
                    NewCommentActivity.createIntent(context, model, position, false),
                )
            }

            override fun likeClick(model: ArticleModel) {
                communityArticleViewModel.postArticleLike(requireContext(), model)
            }

            @dagger.hilt.android.UnstableApi
            override fun viewMoreClick(model: ArticleModel, position: Int) {
                UtilK.clickMore(context, model) { showEdit, showRemove, showReport, showShare->

                    val viewHolder = binding.rvCommunity.findViewHolderForAdapterPosition(position)
                        ?: return@clickMore

                    val communityViewHolder =
                        viewHolder as CommunityArticleViewHolder

                    val sheet = ArticleViewMoreBottomSheetFragment.newInstance(
                        showEdit,
                        showRemove,
                        showReport,
                        model.isMostOnly != "Y",
                        onClickEdit = {
                            communityViewHolder.clickEdit(
                                mActivity,
                                model
                            )
                        },
                        onClickDelete = {
                            communityViewHolder.clickRemove(
                                mActivity,
                                model,
                                position
                            )
                        }, onClickReport = {
                            communityViewHolder.clickReport(
                                mActivity,
                                model,
                                position
                            )
                        }) {
                        setUiActionFirebaseGoogleAnalyticsFragment(
                            GaAction.COMMENT_ARTICLE_SHARE.actionValue,
                            GaAction.COMMENT_ARTICLE_SHARE.label
                        )

                        val msg  = if(BuildConfig.CELEB) {
                            String.format(
                                LocaleUtil.getAppLocale(context ?: return@newInstance),
                                getString(R.string.celeb_community_share_msg),
                                model.content?.trimSubStringLimit(limit = 30),
                                model.idol?.getName(context),
                                if (model.idol?.category.equals("B", ignoreCase = true)) "" else mType?.name
                            )
                        } else {
                            String.format(
                                LocaleUtil.getAppLocale(context ?: return@newInstance),
                                getString(R.string.community_share_msg),
                                model.content?.trimSubStringLimit(limit = 30),
                                model.idol?.getName(context)
                            )
                        }
                        val url = LinkUtil.getAppLinkUrl(
                            context = context ?: return@newInstance,
                            params = listOf(LinkStatus.ARTICLES.status, model.id.toString())
                        )
                        UtilK.linkStart(context = context, url = url, msg = msg.trimNewlineWhiteSpace())
                    }

                    val oldFrag = childFragmentManager.findFragmentByTag(ARTICLE_BOTTOM_SHEET_TAG)
                    if (oldFrag == null) {
                        sheet.show(childFragmentManager, ARTICLE_BOTTOM_SHEET_TAG)
                    }

                }
            }

            override fun feedClick(user: UserModel) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "community_feed",
                )
                communityArticleViewModel.startActivityResultLauncher.launch(
                    FeedActivity.createIntent(requireContext(), user),
                )
            }

            override fun editClick(intent: Intent) {
                if(!isAdded) {
                    return
                }
                communityArticleViewModel.startActivityResultLauncher.launch(intent)
            }

            @SuppressLint("StringFormatInvalid")
            override fun shareClick(model: ArticleModel) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.COMMENT_ARTICLE_SHARE.actionValue,
                    GaAction.COMMENT_ARTICLE_SHARE.label
                )

                val msg  = if(BuildConfig.CELEB) {
                    String.format(
                        LocaleUtil.getAppLocale(context ?: return),
                        getString(R.string.celeb_community_share_msg),
                        model.content?.trimSubStringLimit(limit = 30),
                        model.idol?.getName(context),
                        if (model.idol?.category.equals("B", ignoreCase = true)) "" else mType?.name
                    )
                } else {
                    String.format(
                        LocaleUtil.getAppLocale(context ?: return),
                        getString(R.string.community_share_msg),
                        model.content?.trimSubStringLimit(limit = 30),
                        model.idol?.getName(context)
                    )
                }
                val url = LinkUtil.getAppLinkUrl(
                    context = context ?: return,
                    params = listOf(LinkStatus.ARTICLES.status, model.id.toString())
                )
                UtilK.linkStart(context = context, url = url, msg = msg.trimNewlineWhiteSpace())
            }

            // 번역하기 클릭
            override fun translationClick(
                model: ArticleModel,
                position: Int
            ) {
                setUiActionFirebaseGoogleAnalyticsFragment(Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "community_translate") // 임시값 (확정 후 변경 예정)

                translateArticle(
                    model,
                    position,
                    communityAdapter,
                    articlesRepository
                )
            }
        })

        communityAdapter?.setImgTypeClickListener(object : ImgTypeClickListener {
            override fun verticalImgClickListener() {
                communityAdapter?.setImageOnly(false)
                communityAdapter?.setWallpaperOnly(false)
                communityArticleViewModel.setArticleStatus(context, communityActivityViewModel.idolModel.value, isMost = communityActivityViewModel.getIsMost(), imageOnly = false, wallpaperOnly = false)
            }
            override fun gridImgClickListener() {
                communityAdapter?.setImageOnly(true)
                communityArticleViewModel.setArticleStatus(context, communityActivityViewModel.idolModel.value, isMost = communityActivityViewModel.getIsMost(), imageOnly = true, wallpaperOnly = false)
            }

            override fun wallpaperClickListener(wallpaperOnly: Boolean) {
                communityAdapter?.setWallpaperOnly(wallpaperOnly)
                communityArticleViewModel.setArticleStatus(context, communityActivityViewModel.idolModel.value, isMost = communityActivityViewModel.getIsMost(), imageOnly = true, wallpaperOnly = wallpaperOnly)
            }
        })
        binding.rvCommunity.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                this@CommunityFragment.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val itemTotalCount = recyclerView.adapter?.itemCount?.minus(1)

                if (lazyImageLoadRunnable != null && firstVisibleItemPosition > 0) {
                    lazyImageLoadHandler.removeCallbacks(lazyImageLoadRunnable!!)
                }
                // 움짤

                // 저사양 단말에서 이미지 보였다 안보였다 하는 현상 수정.
                activeThumbnailView = null
                activeExoPlayerView = null

                if (!recyclerView.canScrollVertically(1) &&
                    lastVisibleItemPosition == itemTotalCount &&
                    !communityArticleViewModel.mDisableLoadNextResource.get() &&
                    communityArticleViewModel.nextResourceUrl != null
                ) {
                    communityArticleViewModel.mDisableLoadNextResource.set(true)
                    communityArticleViewModel.getCommunityArticles(context, communityActivityViewModel.idolModel.value, communityActivityViewModel.getIsMost(), true)
                }
            }
        })

        binding.rvCommunity.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {
                binding.rvCommunity.setNewPlayerWhenViewAttached()
            }

            override fun onViewDetachedFromWindow(p0: View) {
                binding.rvCommunity.removeExistPlayer()
            }
        })
    }

    private fun voteHeart(model: ArticleModel, position: Int, totalHeart: Long, freeHeart: Long) {
        val fragment = VoteDialogFragment.getArticleVoteInstance(model, position, totalHeart, freeHeart)
        fragment.setTargetFragment(this, RequestCode.ARTICLE_VOTE.value)
        fragment.show(requireActivity().supportFragmentManager, "community_vote")
    }

    private fun setFilter(bottomSheetFragment: BottomSheetFragment) {
        val tag = "filter_community"
        val oldFrag = requireActivity().supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            bottomSheetFragment.show(requireActivity().supportFragmentManager, tag)
        }
    }

    private fun showEventDialog(event_heart: String) {
        val eventHeartDialog = Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            dimAmount = 0.7f
            gravity = Gravity.CENTER
        }
        eventHeartDialog.window?.attributes = lpWindow
        eventHeartDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        eventHeartDialog.setContentView(R.layout.dialog_surprise_heart)
        eventHeartDialog.setCanceledOnTouchOutside(false)
        eventHeartDialog.setCancelable(true)
        val btnOk: AppCompatButton = eventHeartDialog.findViewById(R.id.btn_ok)
        btnOk.setOnClickListener { eventHeartDialog.cancel() }
        val msg: AppCompatTextView = eventHeartDialog.findViewById(R.id.message)
        val surprise_msg = getString(R.string.msg_surprise_heart, event_heart)
        msg.text = surprise_msg
        eventHeartDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        eventHeartDialog.show()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCode.ARTICLE_VOTE.value && resultCode == BaseDialogFragment.RESULT_OK) {
            val heart = data?.getLongExtra(VoteDialogFragment.PARAM_HEART, 0) ?: 0
            if (heart > 0) {
                // top3가 갱신되는 것은 서버에서 별도 쓰레드로 돌아간다고 하여 일정시간 후 요청하는 것으로 변경 -> 이미 투표 후 3초 딜레이가 있어 즉시 요청으로 변경
                Handler(Looper.getMainLooper()).postDelayed({
                    if (communityActivityViewModel.idolModel.value == null) {
                        return@postDelayed
                    }

                    lifecycleScope.launch {
                        idolsRepository.getIdolsByIds(
                            ids = communityActivityViewModel.idolModel.value!!.getId().toString(),
                            fields = "top3,top3_type,image_url,image_url2,image_url3",
                            listener = { response ->
                                try {
                                    // 글 삭제 후 즐찾 최애 표시 사라짐 수정
                                    val idol = IdolGson.getInstance().fromJson(
                                        response.getJSONArray("objects")
                                            .getJSONObject(0)
                                            .toString(),
                                        IdolModel::class.java,
                                    )

                                    // top 3순서가  ->  기존 순서랑 다를때   top 배너가 업데이트 된것으로 간주하고,
                                    // setResult를 설정하여,  searchResultAcivity의 경우 업데이트 되게 해준다.
                                    if (idol.top3 != communityActivityViewModel.idolModel.value!!.top3) {
                                        activity?.setResult(Const.RESULT_TOP3_UPDATED)
                                    }

                                    // 변경된 top3 반영하여 DB에 밀어넣기
                                    activity?.let {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            updateIdolUseCase(idol.toDomain()).first()
                                        }
                                    }
                                    communityActivityViewModel.idolModel.value!!.apply {
                                        imageUrl = idol.imageUrl
                                        imageUrl2 = idol.imageUrl2
                                        imageUrl3 = idol.imageUrl3
                                        top3 = idol.top3
                                        top3Type = idol.top3Type
                                    }
                                    communityActivityViewModel.changeTop3()
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                            },
                            errorListener = {}
                        )
                    }
                }, 0)
                val model = data?.getSerializableExtra(PARAM_ARTICLE) as ArticleModel?

                val article = communityArticleViewModel.articleList.find { it.id == model?.id }
                article?.let {
                    it.heart = it.heart + heart
                    communityAdapter?.updateItem(it)
                }

                val eventHeart = data?.getStringExtra(CommunityActivity.PARAM_EVENT_HEART)
                eventHeart?.let { showEventDialog(it) }

                // 최애에게 투표하면 최애화면에서 프사 순위 변경 적용되게
                try {
                    val account = IdolAccount.getAccount(mContext)
                    if (account?.userModel?.most != null && account.userModel?.most?.getId() == communityActivityViewModel.idolModel.value!!.getId()) {
                        ApiCacheManager.getInstance().clearCache(Const.KEY_FAVORITE)
                        // 최애 탑3 갱신.
                        EventBus.sendEvent(true, Const.BROADCAST_REFRESH_TOP3)
                        //                        setResult(RESULT_OK);
                    }

                    // 즐찾에서 아무 커뮤 진입 후 투표 후에 즐찾 갱신되게
                    activity?.setResult(Activity.RESULT_OK)

                    // 레벨업 체크
                    UtilK.checkLevelUp(baseActivity, accountManager, communityActivityViewModel.idolModel.value, heart)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

                // 비밀의 방에 투표시 처리
                if (communityActivityViewModel.idolModel.value != null && communityActivityViewModel.idolModel.value!!.category == "B") {
                    EventBus.sendEvent(true, Const.BROADCAST_REFRESH_TOP3)
                }
            } else {
                Util.closeProgress()
            }
        }
        if (requestCode == RequestCode.ARTICLE_REPORT.value && resultCode == ResultCode.REPORTED.value) {
            val article = data?.getSerializableExtra(PARAM_ARTICLE) as ArticleModel
            val account = IdolAccount.getAccount(mContext)
            if (account != null) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
                val editor = prefs.edit()
                val reportedArticles = prefs.getStringSet(account.email + "_did_report", HashSet())
                reportedArticles!!.add(article.resourceUri)
                editor.putStringSet(account.email + "_did_report", reportedArticles).apply()
            }
            // 글 삭제하면 게시물 안보이게 다시 복원
            communityAdapter?.removeItem(article)

            // article 차단 목록 추가
            UtilK.addArticleReport(requireContext(), article.id)
        }
        if (requestCode == RequestCode.ARTICLE_REMOVE.value) {
            Util.closeProgress()
            if (resultCode == ResultCode.REMOVED.value) {
                // top3가 갱신되는 것은 서버에서 별도 쓰레드로 돌아간다고 하여 일정시간 후 요청하는 것으로 변경
                Handler(Looper.getMainLooper()).postDelayed({
                    if (communityActivityViewModel.idolModel.value == null) {
                        return@postDelayed
                    }

                    lifecycleScope.launch {
                        idolsRepository.getIdolsByIds(
                            ids = communityActivityViewModel.idolModel.value!!.getId().toString(),
                            fields = "top3,top3_type,image_url,image_url2,image_url3",
                            listener = { response ->
                                try {
                                    // 글 삭제 후 즐찾 최애 표시 사라짐 수정
                                    val idol: IdolModel = IdolGson.getInstance().fromJson(
                                        response.getJSONArray("objects")
                                            .getJSONObject(0)
                                            .toString(),
                                        IdolModel::class.java,
                                    )
                                    communityActivityViewModel.idolModel.value?.apply {
                                        imageUrl = idol.imageUrl
                                        imageUrl2 = idol.imageUrl2
                                        imageUrl3 = idol.imageUrl3
                                        top3 = idol.top3
                                        top3Type = idol.top3Type
                                    }
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                            },
                            errorListener = {}
                        )
                    }
                }, 1000)
                val article = data?.getSerializableExtra(PARAM_ARTICLE) as ArticleModel?
                // 글 삭제하면 게시물 안보이게 다시 복원
                if (article != null) {
                    communityAdapter?.removeItem(article)
                }
            }
        }
    }

    fun onScrollStateChanged(recyclerView: RecyclerView, scrollState: Int) {
        // 움짤
        val layoutManager = binding.rvCommunity.layoutManager as LinearLayoutManager
        for (listItemIndex in 0..layoutManager.findLastVisibleItemPosition() - layoutManager.findFirstVisibleItemPosition()) {
            if (scrollState != -1) {
                continue
            }
        }

        if (scrollState != RecyclerView.SCROLL_STATE_IDLE) {
            return
        }

        lazyImageLoadRunnable?.let { lazyImageLoadHandler.removeCallbacks(it) }

        val isDataSavingMode = UtilK.dataSavingMode(requireActivity())
        if (isDataSavingMode) {
            return
        }

        lazyImageLoadRunnable = Runnable {
            try {
                val rvLayoutManager =
                    binding.rvCommunity.layoutManager as LinearLayoutManager
                val lastVisible = rvLayoutManager.findLastVisibleItemPosition()
                val firstVisible = rvLayoutManager.findFirstVisibleItemPosition()
                for (listItemIndex in 0..rvLayoutManager.findLastVisibleItemPosition() - rvLayoutManager.findFirstVisibleItemPosition()) {
                    val listItem = binding.rvCommunity.getChildAt(listItemIndex)
                    val photo: ExodusImageView? =
                        listItem.findViewById(R.id.attach_photo)

                    if (photo?.loadInfo == null) {
                        continue
                    }

                    val url = photo.loadInfo as String

                    Util.log(">>>>>>>>>>>>>>> loading original size image $url")
                    if (photo.getLoadInfo(R.id.TAG_LOAD_LARGE_IMAGE) != java.lang.Boolean.TRUE &&
                        photo.getLoadInfo(R.id.TAG_IS_UMJJAL) != java.lang.Boolean.FALSE
                    ) {
                        continue
                    }

                    // thumbnail이 다 로드되어야 원본 이미지 부른다
                    photo.post {
                        mGlideRequestManager.asBitmap()
                            .load(url)
                            .listener(object : RequestListener<Bitmap> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: com.bumptech.glide.request.target.Target<Bitmap>,
                                    isFirstResource: Boolean,
                                ): Boolean {
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Bitmap,
                                    model: Any,
                                    target: com.bumptech.glide.request.target.Target<Bitmap>,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean,
                                ): Boolean {
                                    requireActivity().runOnUiThread {
                                        val loadInfo =
                                            photo.loadInfo as String?
                                        if (loadInfo != null && loadInfo == url) {
                                            Util.log(">>>>>>>>>>>>>>>:: image displayed $url")
                                            photo.safeSetImageBitmap(activity, resource)
                                        }
                                    }

                                    return false
                                }
                            })
                            .submit()
                    }
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }

        // 1초 후
        // Util.log("********* timer set");
        lazyImageLoadHandler.postDelayed(lazyImageLoadRunnable!!, 1000)
    }
}