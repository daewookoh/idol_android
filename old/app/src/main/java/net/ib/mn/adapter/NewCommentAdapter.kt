package net.ib.mn.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.databinding.CommentItemBinding
import net.ib.mn.databinding.CommunityItemBinding
import net.ib.mn.databinding.ItemSupportPhotoBinding
import net.ib.mn.databinding.ScheduleCommentHeaderBinding
import net.ib.mn.databinding.SmallTalkCommentHeaderBinding
import net.ib.mn.dialog.MapDialog
import net.ib.mn.domain.usecase.GetIdolsByIdsUseCase
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.*
import net.ib.mn.smalltalk.viewholder.SmallTalkCommentHeaderViewHolder
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.ext.setOnSingleClickListener
import net.ib.mn.utils.setFirebaseUIAction
import net.ib.mn.view.ExodusImageView
import net.ib.mn.viewholder.ArticleCommentViewHolder
import net.ib.mn.viewholder.CommentViewHolder
import net.ib.mn.viewholder.ScheduleCommentViewHolder
import net.ib.mn.viewholder.SupportCertifyViewHolder
import org.json.JSONObject
import java.util.Calendar


class NewCommentAdapter(
    private val context: Context,
    private val useTranslation: Boolean = false,
    private val mGlideRequestManager: RequestManager,
    private val HEADER_VIEW_TYPE: Int,
    private var getIdolsByIdsUseCase: GetIdolsByIdsUseCase? = null,
    private var lifecycleScope: LifecycleCoroutineScope,
    private var fragmentManager: FragmentManager? = null,
    private var tagName: String? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var recyclerView: RecyclerView? = null
    private var mIsScrollToTop: Boolean = true
    private var isUserLikeCache = false
    private var supportAdTypeModel: SupportAdTypeListModel? = null

    private val now = Calendar.getInstance()
    private val locale = LocaleUtil.getAppLocale(context)

    var isLoading = false
    var loadFailed = false // 네트워크 오류로 로드 실패 여부

    //일반 댓글일때는  스크롤관련  top 여부 관련 값을  생성자에 보내준다.
    constructor(
        context: Context,
        useTranslation: Boolean,
        mGlideRequestManager: RequestManager,
        HEADER_VIEW_TYPE: Int,
        recyclerView: RecyclerView,
        mIsScrollToTop: Boolean,
        getIdolsByIdsUseCase: GetIdolsByIdsUseCase,
        lifecycleScope: LifecycleCoroutineScope,
        tagName: String?
    ) : this(context, useTranslation, mGlideRequestManager, HEADER_VIEW_TYPE, getIdolsByIdsUseCase, lifecycleScope, null, tagName) {
        this.recyclerView = recyclerView
        this.mIsScrollToTop = mIsScrollToTop
    }

    var articleModel = ArticleModel()
    var scheduleModel = ScheduleModel()

    //댓글 모음
    private var commentList = ArrayList<CommentModel>()
    private var isNextCommentDataExist: Boolean = false
    private var supportListModel = SupportListModel()

    lateinit var typeList: ArrayList<SupportAdTypeListModel>

    lateinit var supportInfo: JSONObject

    //클릭 관련 이벤트
    private var onCommentItemClickListener: OnCommentItemClickListener? = null
    private var onArticleItemClickListener: OnArticleItemClickListener? = null
    private var onSupportItemClickListener: OnSupportItemClickListener? = null
    private var articlePhotoListener: ArticlePhotoListener? = null

    private var mIds: HashMap<Int, String>? = null
    private var isScheduleCommentPush: Boolean = false


    private var getVideoPlayView: GetVideoPlayView? = null

    interface OnCommentItemClickListener {
        fun onCommentNameClicked(commentItem: CommentModel)
        fun onCommentProfileImageClicked(commentItem: CommentModel)
        fun onViewMoreItemClicked()
        fun onCommentImageClicked(articleModel: ArticleModel)
        fun onRefreshClicked() // 다음 댓글 불러오기 실패해서 갱신하는 경우
    }

    //아이템 클릭 이벤트 받을  리스너 인터페이스
    interface OnSupportItemClickListener {
        fun onItemClick(checkView: Int)
        fun onLikeChange(imgLikeIcon: ImageView, tvLikeCount: TextView)
        fun onTop5BtnClicked(supportId: Int)
        fun onCertifyPhotoClicked(supportListModel: SupportListModel, mediaType: Int)
    }

    //아티클 관련 게시글 click 이벤트 모음
    interface OnArticleItemClickListener {
        fun onReportBtnClicked()//게시글 신고 버튼 클릭시
        fun onVoteBtnClicked(articleModel: ArticleModel)//투표 버튼 클릭시
        fun onClickWriterProfile(userModel: UserModel)//게시글 작성자 프로필 또는 이름 클릭시
        fun onShareArticle()//게시글 공유버튼 클릭시
        fun onEditArticle()//게시글 편집 버튼 클릭시
        fun onRemoveArticle()//게시글 삭제 버튼 클릭시
        fun photoClicked()
        fun onArticleLikeClicked(articleModel: ArticleModel)   //좋아요 버튼 클릭 시
        fun onCommentShowClicked()
    }

    interface GetVideoPlayView {
        fun getExoVideoPlayView(
            playerView: PlayerView?,
            imageView: ExodusImageView?,
            ivGif: AppCompatImageView?,
            videoUrl: String?
        )
    }

    fun setSupportJson(supportInfoJson: JSONObject) {
        this.supportInfo = supportInfoJson
    }

    fun getSupportInfo(model: SupportListModel) {
        this.supportListModel = model
        notifyItemChanged(0)
    }

    //외부에서 서포 아이템 클릭 처리할 리스너
    fun setOnItemClickListener(onSupportItemClickListener: OnSupportItemClickListener) {
        this.onSupportItemClickListener = onSupportItemClickListener
    }


    //외부에서  아티클 아이템 클릭 처리할 리스너
    fun setOnArticleItemClickListener(onArticleItemClickListener: OnArticleItemClickListener) {
        this.onArticleItemClickListener = onArticleItemClickListener
    }

    //외부에서  댓글  아이템 클릭 처리할 리스너
    fun setOnCommentItemClickListener(onCommentItemClickListener: OnCommentItemClickListener) {
        this.onCommentItemClickListener = onCommentItemClickListener
    }

    fun setPhotoClickListener(articlePhotoListener: ArticlePhotoListener) {
        this.articlePhotoListener = articlePhotoListener
    }

    fun setVideoPlayerView(getVideoPlayView: GetVideoPlayView) {
        this.getVideoPlayView = getVideoPlayView
    }

    //유저가 차단된 경우 최신화
    fun userBlockChanged(userId: Int?) {
        if (commentList.size < 1) {
            return
        }
        for (i in 0 until commentList.size) {
            if (commentList[i].id != 0 && commentList[i].user?.id == userId) { //더보기 있을 경우, commentList[0] 의 값은 가짜 데이터, 더보기 없을 경우 실제 데이터. (가짜데이터의 경우 id = 0)
                notifyItemChanged(i + 1)  //첫번째 포지션은 header이기 떄문에 +1
            }
        }
    }

    @OptIn(dagger.hilt.android.UnstableApi::class)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            TYPE_ARTICLE -> {
                val binding: CommunityItemBinding =
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.community_item,
                        parent,
                        false
                    )
                ArticleCommentViewHolder(
                    context = context,
                    useTranslation = useTranslation,
                    binding = binding,
                    mGlideRequestManager = mGlideRequestManager,
                    getVideoPlayView = getVideoPlayView,
                    tagName = tagName,
                    now = now,
                    locale = locale,
                    articlePhotoListener = articlePhotoListener,
                    lifecycleScope = lifecycleScope,
                    onArticleItemClickListener = onArticleItemClickListener
                )
            }

            TYPE_SUPPORT -> {
                val binding: ItemSupportPhotoBinding =
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_support_photo,
                        parent,
                        false
                    )

                SupportCertifyViewHolder(
                    binding = binding,
                    mGlideRequestManager = mGlideRequestManager,
                    itemCount = itemCount,
                    getVideoPlayView = getVideoPlayView,
                    supportInfo = supportInfo,
                    supportListModel = supportListModel
                ).apply {
                    // TODO 좋아요 문제 생기면 확인
                    binding.footerSmile.setOnClickListener {
                        onSupportItemClickListener?.onLikeChange(
                            binding.imgSupportLikeIcon,
                            binding.likeCount
                        )

                        supportListModel.apply {
                            like = !like
                            toggleLikeIcon(like)
                        }
                    }

                    binding.layoutCheckLocation.setOnClickListener {
                        setFirebaseUIAction(GaAction.SUPPORT_ADDRESS)
                        if (!supportListModel.type.locationImageUrl.isNullOrEmpty()) {
                            MapDialog.getInstance(
                                supportListModel.type.locationImageUrl!!
                            ).show(fragmentManager!!, "map_dialog")
                        } else {
                            val gmmIntentUri =
                                Uri.parse(supportListModel.type.locationMapUrl)
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                startActivity(context, mapIntent, null)
                            }
                        }
                    }
                }
            }

            TYPE_SCHEDULE -> {
                val binding: ScheduleCommentHeaderBinding =
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.schedule_comment_header,
                        parent,
                        false
                    )
                ScheduleCommentViewHolder(
                    binding = binding,
                    getIdolsByIdsUseCase)
            }

            TYPE_SMALL_TALK -> {
                val binding: SmallTalkCommentHeaderBinding =
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.small_talk_comment_header,
                        parent,
                        false
                    )
                SmallTalkCommentHeaderViewHolder(
                    binding = binding,
                    useTranslation = useTranslation,
                    mGlideRequestManager = mGlideRequestManager,
                    tagName = tagName,
                    now = now,
                    locale = locale,
                    getVideoPlayView = getVideoPlayView,
                    onArticleItemClickListener = onArticleItemClickListener
                )
            }

            else -> {
                val binding: CommentItemBinding =
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.comment_item,
                        parent,
                        false
                    )

                CommentViewHolder(
                    binding = binding,
                    useTranslation = useTranslation,
                    context = parent.context,
                    mArticle = articleModel,
                    commentList = commentList,
                    isCommentOnly = false,
                    mGlideRequestManager = mGlideRequestManager
                )

            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            HEADER_VIEW_TYPE
        } else {
            TYPE_COMMENT
        }
    }

    @OptIn(dagger.hilt.android.UnstableApi::class)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0) {
            //positon0 이면서  아티클 일 경우
            when (holder.itemViewType) {

                TYPE_ARTICLE -> (holder as ArticleCommentViewHolder).apply {//일반 댓글 화면
                    val idolModel = articleModel.idol
                    bind(articleModel = articleModel, idolModel = idolModel ?: return)
                    //투표 버튼 눌렀을때
                    binding.footerHeart.setOnSingleClickListener { v: View? ->
                        onArticleItemClickListener?.onVoteBtnClicked(articleModel)
                    }
                    //유저 프로필 사진  클릭시
                    binding.photo.setOnClickListener { v: View? ->
                        onArticleItemClickListener?.onClickWriterProfile(articleModel.user ?: return@setOnClickListener)
                    }
                    //유저 이름 클릭시
                    binding.name.setOnClickListener { v: View? ->
                        onArticleItemClickListener?.onClickWriterProfile(articleModel.user ?: return@setOnClickListener)
                    }
                }

                TYPE_SUPPORT -> (holder as SupportCertifyViewHolder).apply {//서포트 인증샷 화면

                    bind(supportInfo, supportListModel)

                    //광고 종류 화면 보러 가기
                    binding.ivAdTypeList.moveToSupportTypeList()
                    binding.tvExpandTouch.moveToSupportTypeList()

                    if (supportListModel.type.locationImageUrl == null && supportListModel.type.locationMapUrl == null) {
                        binding.layoutCheckLocation.visibility = View.GONE
                    }

                    binding.layoutCheckLocation.setOnClickListener {
                        setFirebaseUIAction(GaAction.SUPPORT_ADDRESS)
                        if (!supportListModel.type.locationImageUrl.isNullOrEmpty()) {
                            MapDialog.getInstance(
                                supportListModel.type.locationImageUrl!!
                            ).show(fragmentManager!!, "map_dialog")
                        } else {
                            val gmmIntentUri =
                                Uri.parse(supportListModel.type.locationMapUrl)
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                startActivity(context, mapIntent, null)
                            }
                        }
                    }

                    binding.tvCategory.text = when(supportListModel.type.category) {
                        SupportAdType.KOREA.label -> {
                            context.getString(R.string.adtype_korean)
                        }
                        SupportAdType.MOBILE.label -> {
                            context.getString(R.string.adtype_mobile)
                        }
                        SupportAdType.FOREIGN.label -> {
                            context.getString(R.string.adtype_global)
                        }
                        else -> {
                            ""
                        }
                    }

                    //인증샷 이미지 클릭시
                    binding.imgSupportResult.setOnClickListener {
                        onSupportItemClickListener?.onCertifyPhotoClicked(
                            supportListModel,
                            PHOTO_TYPE
                        )
                    }

                    //인증샷 비디오 클릭시
                    binding.videoSupportResult.videoSurfaceView?.setOnClickListener {
                        onSupportItemClickListener?.onCertifyPhotoClicked(
                            supportListModel,
                            VIDEO_TYPE
                        )
                    }

                    binding.llHeartCount.setOnClickListener {
                        onSupportItemClickListener?.onLikeChange(
                            binding.imgSupportLikeIcon,
                            binding.likeCount
                        )
                    }

                    //top5버튼 클릭했을떄 support id를 넘겨줌.
                    binding.footerTop5.setOnClickListener {
                        onSupportItemClickListener?.onTop5BtnClicked(supportListModel.id)
                    }

                    //공유 버튼 눌림.
                    binding.btnShare.setOnClickListener {
                        onSupportItemClickListener?.onItemClick(SHARE_BUTTON)

                    }


                }

                TYPE_SCHEDULE -> (holder as ScheduleCommentViewHolder).apply {//스케쥴 댓글 화면 일때

                    bind(
                        articleModel,
                        mSchedule = scheduleModel,
                        mIds = mIds,
                        isScheduleCommentPush
                    )

                    //지도 클릭시
                    binding.scheduleLocation.setOnClickListener {
                        val i = Intent(
                            Intent.ACTION_VIEW, Uri.parse(
                                "geo:0,0?q=" + scheduleModel.lat + "," + scheduleModel.lng + "("
                                    + scheduleModel.location + ")"
                            )
                        )
                        i.setClassName(
                            "com.google.android.apps.maps",
                            "com.google.android.maps.MapsActivity"
                        )
                        itemView.context.startActivity(i)
                    }


                }

                TYPE_SMALL_TALK -> {
                    (holder as SmallTalkCommentHeaderViewHolder).apply {
                        bind(articleModel)

                        //사진 클릭시
                        binding.eivAttachPhoto.setOnClickListener {
                            onArticleItemClickListener?.photoClicked()
                        }
                        binding.attachButton.setOnClickListener {
                            onArticleItemClickListener?.photoClicked()
                        }

                        //유저 프로필 사진  클릭시
                        binding.eivSmallTalkHeaderPhoto.setOnClickListener { v: View? ->
                            onArticleItemClickListener?.onClickWriterProfile(articleModel.user ?: return@setOnClickListener)
                        }
                        //유저 이름 클릭시
                        binding.tvSmallTalkHeaderUsername.setOnClickListener { v: View? ->
                            onArticleItemClickListener?.onClickWriterProfile(articleModel.user ?: return@setOnClickListener)
                        }
                    }
                }
            }

        } else {//그밖에는 댓글

            (holder as CommentViewHolder).apply {
                if (commentList.isEmpty()) {
                    bindEmpty()
                } else {
                    bind(commentList[position - 1], position, isNextCommentDataExist, loadFailed)
                }

                // 마지막 5개쯤 전을 보여줄 때 다음 목록 로드
                // 네트워크 오류로 자동 불러오기 실패한 경우에는 하지 않음
                if (position >= commentList.size - 5 && isNextCommentDataExist && !isLoading && !loadFailed) {
                    isLoading = true
                    onCommentItemClickListener?.onViewMoreItemClicked()
                }

                binding.clRefresh.setOnClickListener {
                    onCommentItemClickListener?.onRefreshClicked()
                }

                binding.photo.setOnClickListener {
                    onCommentItemClickListener?.onCommentProfileImageClicked(commentItem = commentList[position - 1])
                }

                binding.name.setOnClickListener {
                    onCommentItemClickListener?.onCommentNameClicked(commentItem = commentList[position - 1])
                }
                binding.ivImageContent.setOnClickListener {
                    //이미지나 움짤만 클릭헀을 때 widePhoto가 나와야함.
                    commentList.getOrNull(position - 1)?.let { comment ->

                        val article = comment.article ?: ArticleModel().apply {
                            umjjalUrl = comment.contentAlt?.umjjalUrl
                            imageUrl = comment.contentAlt?.imageUrl
                        }

                        if (article.imageUrl.isNullOrEmpty() && article.umjjalUrl.isNullOrEmpty()) {
                            return@let
                        }

                        onCommentItemClickListener?.onCommentImageClicked(article)
                    }
                }
            }

        }
    }

    //광고종류 화면으로 이동
    private fun View.moveToSupportTypeList() {
        this.bringToFront()
        this.setOnClickListener {
            onSupportItemClickListener?.onItemClick(MOVE_AD_TYPE_LIST_BUTTON)
        }
    }


    override fun getItemCount(): Int = if(commentList.isEmpty()) 2 else commentList.size + 1


    //커멘트 리스트 받기
    fun setCommentList(
        articleModel: ArticleModel,
        commentList: ArrayList<CommentModel>,
        isNextDataExist: Boolean,
    ) {
        this.articleModel = articleModel
        this.commentList = commentList
        this.isNextCommentDataExist = isNextDataExist
        notifyDataSetChanged()
    }

    //article 값 받기
    fun setArticle(articleModel: ArticleModel?) {
        if (articleModel != null) {
            this.articleModel = articleModel
            isUserLikeCache = this.articleModel.isUserLike
            notifyItemChanged(0)
        }
    }

    //스케쥴 에 필요한 값 받기
    fun setSchedule(
        scheduleModel: ScheduleModel?,
        mIds: HashMap<Int, String>?,
        isScheduleCommentPush: Boolean
    ) {
        if (scheduleModel != null) {
            this.scheduleModel = scheduleModel
            this.mIds = mIds
            this.isScheduleCommentPush = isScheduleCommentPush
            notifyItemChanged(0)
        }
    }

    companion object {
        const val TYPE_ARTICLE = 0
        const val TYPE_PHOTO = 1
        const val TYPE_COMMENT = 2
        const val TYPE_SUPPORT = 3
        const val TYPE_SCHEDULE = 4
        const val TYPE_SMALL_TALK = 5
        const val SHARE_BUTTON = 12
        const val MOVE_AD_TYPE_LIST_BUTTON = 13
        const val VIDEO_TYPE = 24
        const val PHOTO_TYPE = 25
        const val COPY_COMMENT = 1
        const val REPORT_COMMENT = 2
        const val DELETE_COMMENT = 3
        const val MENTION_REGEX = "@\\{(\\d+)\\:(([^\\}]+))\\}"
    }
}