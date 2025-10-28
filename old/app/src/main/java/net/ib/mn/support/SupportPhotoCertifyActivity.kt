package net.ib.mn.support

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseCommentActivity
import net.ib.mn.activity.FeedActivity
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.adapter.NewCommentAdapter.Companion.MOVE_AD_TYPE_LIST_BUTTON
import net.ib.mn.adapter.NewCommentAdapter.Companion.PHOTO_TYPE
import net.ib.mn.adapter.NewCommentAdapter.Companion.SHARE_BUTTON
import net.ib.mn.adapter.NewCommentAdapter.Companion.VIDEO_TYPE
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.dialog.BaseDialogFragment.DialogResultHandler
import net.ib.mn.fragment.WidePhotoFragment
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.CommentModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.SupportListModel
import net.ib.mn.utils.CommentTranslation
import net.ib.mn.utils.CommentTranslationHelper
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.view.ExodusImageView
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class SupportPhotoCertifyActivity : BaseCommentActivity(), DialogResultHandler, CommentTranslation {

    private var supportInfo: String? = ""
    private lateinit var supportInfoJSONObject: JSONObject

    private var like = false

    private var supportInfoList = SupportListModel()

    @Inject
    lateinit var supportRepository: SupportRepositoryImpl
    @Inject
    lateinit var videoAdUtil: VideoAdUtil
    @Inject
    lateinit var commentTranslationHelper: CommentTranslationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_support_photo_certify)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.clContainer) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())

            // 툴바가 상태바와 겹치지 않도록 패딩 설정
            binding.toolbar.setPadding(binding.toolbar.paddingLeft, systemBars.top, binding.toolbar.paddingRight, binding.toolbar.paddingBottom)

            // 키보드나 네비게이션 바 높이에 따라 cl_root의 패딩을 조절하여 밀어올림
            val bottomInset = maxOf(systemBars.bottom, ime.bottom)
            binding.clRoot.setPadding(binding.clRoot.paddingLeft, binding.clRoot.paddingTop, binding.clRoot.paddingRight, bottomInset)

            insets
        }

        initUI()
        setAdManager()
        setSupportRecyclerView()
        clickEvent()
        getSupportInfo()
        setRecyclerViewListener()
        setFriendsRecyclerView(binding.friendsRcyView)
        inputCommentChangeListen(binding.viewComment.inputComment)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MEZZO_PLAYER_REQ_CODE) {
            Util.handleVideoAdResult(
                this, false, true, requestCode, resultCode, data, "support_Certify_videoad"
            ) { adType: String? ->
                videoAdUtil.onVideoSawCommon(
                    this,
                    true,
                    adType,
                    null
                )
            }
        }

        //피드에서 유저 차단한 경우
        if (requestCode == RequestCode.USER_BLOCK_CHANGE.value && resultCode == ResultCode.BLOCKED.value) {
            val userId = data?.getIntExtra(FeedActivity.PARAM_USER_ID, 0)
            recyclerviewAdapter.userBlockChanged(userId)
        }
    }

    //초기 ui 세팅
    private fun initUI() {
        supportInfo = intent.getStringExtra("support_info")
        supportInfoJSONObject = JSONObject(supportInfo)

        setToolbar()
        initBaseUI()

        binding.friendsRcyView.visibility = View.GONE
        binding.rcySupportPhoto.visibility = View.VISIBLE
    }

    override fun setToolbar() {
        super.setToolbar()
        supportActionBar?.apply {
            title = supportInfoJSONObject.getString("name")
        }
    }

    //클릭 이벤트 모음.
    private fun clickEvent() {
        recyclerviewAdapter.setOnItemClickListener(object :
            NewCommentAdapter.OnSupportItemClickListener {

            //top5눌렀을때 top5 화면으로 이동
            override fun onTop5BtnClicked(supportId: Int) {
                startActivity(
                    SupportTop5Activity.createIntent(
                        this@SupportPhotoCertifyActivity,
                        supportId
                    )
                )
            }

            override fun onItemClick(checkContainer: Int) {
                when (checkContainer) {
                    //쉐어 버튼 눌림.
                    SHARE_BUTTON -> {
                        shareSuccessSupport(supportInfoList.type.toSupportAdTypeListModel())
                    }

                    //광고 종류 리스트 보기 화면 이동
                    MOVE_AD_TYPE_LIST_BUTTON -> {
                        startActivity(
                            SupportAdPickActivity.createIntent(
                                this@SupportPhotoCertifyActivity,
                                false
                            )
                        )
                    }

                }
            }

            //인증샷 ->  사진 또는 영상 클릭됬을때 widePhotoFragment 실행
            override fun onCertifyPhotoClicked(supportListModel: SupportListModel, mediaType: Int) {
                if (mediaType == PHOTO_TYPE) {
                    if (supportListModel.article.imageUrl != null) {
                        setUiActionFirebaseGoogleAnalyticsActivity(
                            Const.ANALYTICS_BUTTON_PRESS_ACTION,
                            "success_support_photo_click"
                        )
                        WidePhotoFragment.getInstance(supportListModel.article).apply {
                            setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                        }.show(supportFragmentManager, "wide_photo")
                    }
                } else if (mediaType == VIDEO_TYPE) {
                    if (supportListModel.article.umjjalUrl != null) {
                        setUiActionFirebaseGoogleAnalyticsActivity(
                            Const.ANALYTICS_BUTTON_PRESS_ACTION,
                            "success_support_video_click"
                        )
                        WidePhotoFragment.getInstance(supportListModel.article).apply {
                            setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                        }.show(supportFragmentManager, "wide_photo")
                    }
                }
            }

            //좋아요 바뀜 여부.
            override fun onLikeChange(imgLikeIcon: ImageView, tvLikeCount: TextView) {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.SUPPORT_SUCCESS_LIKE.actionValue,
                    GaAction.SUPPORT_SUCCESS_LIKE.label
                )
                updateLike(imgLikeIcon, tvLikeCount)
            }

        })

        //댓글 버튼 클릭
        binding.viewComment.btnSubmit.setOnClickListener {
            writeComments(
                supportInfoList.article,
                idol = supportInfoList.idol,
                emoticonId = selectedEmoticonId,
                comment = Util.BadWordsFilterToHeart(
                    this,
                    binding.viewComment.inputComment.text.toString()
                ),
                binImage
            )
        }

    }

    override fun onBackPressed() {
        if (binding.rlEmoticon.isVisible) { //뒤로가기 눌렀을때 이모티콘창 있을경우엔 뒤로가지말고 이모티콘창 닫아줌.
            binding.rlEmoticon.visibility = View.GONE
        } else { //화면 닫아줌.
            super.onBackPressed()
        }
    }


    //서포트 인증샷 화면 공유
    private fun shareSuccessSupport(typeAd: SupportAdTypeListModel?) {
        val idolName = Util.nameSplit(this, supportInfoList.idol)
        if (idolName[1] != "") idolName[1] = "#${UtilK.removeWhiteSpace(idolName[1])}"

        val params = listOf(LinkStatus.SUPPORTS.status, supportInfoList.id.toString())
        val url =
            LinkUtil.getAppLinkUrl(context = this@SupportPhotoCertifyActivity, params = params)
        val msg = String.format(
            getString(if (BuildConfig.CELEB) R.string.share_success_support_celeb else R.string.share_success_support),
            UtilK.removeWhiteSpace(idolName[0]), idolName[1], idolName[0], typeAd?.name, ""
        )

        setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "success_support_share"
        )

        UtilK.linkStart(this, url = url, msg = msg)
    }


    //댓글 리스트를 가지고 온다.
    private fun loadComments(mArticle: ArticleModel) {
        loadComments(mArticle) { response ->
            if (response.optBoolean("success")) {
                val numComments = response.getJSONObject("article").optInt("num_comments", 0)
                supportInfoList.like = like
            }
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onDialogResult(requestCode, resultCode, data)

        if (requestCode == RequestCode.ARTICLE_COMMENT_REMOVE.value) {
            if (resultCode == ResultCode.COMMENT_REMOVED.value) {
                //지우기전에  좋아요를 했을때는 해당 값을 다시 보내줘야됨.(서버 호출 x하기 위해서)
                supportInfoList.like = like
            }
        }
    }


    override fun onPause() {
        super.onPause()

        simpleExoPlayerView?.player?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    //업데이트 값 success 면  일단  유저입장에서는  좋아요 올라간것 처럼 보이게  뷰만 처리해줌.
    private fun updateLike(imgLikeIcon: ImageView, tvLikeCount: TextView) {
        MainScope().launch {
            supportRepository.like(
                supportId = supportInfoJSONObject.getString("support_id").toInt(),
                { response ->
                    val isLikeUpdated = response?.get("success").toString().toBoolean()

                    if (isLikeUpdated) {

                        if (like) {//like true일때는  -1  취소 시켜줌,
                            imgLikeIcon.setImageResource(R.drawable.icon_board_like)
                            val presentLikeCount = tvLikeCount.text.toString().toInt()
                            supportInfoList.article.heart = supportInfoList.article.heart - 1
                            if (presentLikeCount > 0) {
                                tvLikeCount.text = (presentLikeCount - 1).toString()
                                like = false
                            } else {
                                //가끔  좋아요 표시가  -1단계로 내려갈때가 있어서 이렇때는  그냥  0 처리 해준다.
                                like = false
                                tvLikeCount.text = 0.toString()
                            }
                        } else {//like false 일때는 +1 시켜줌.
                            supportInfoList.article.heart = supportInfoList.article.heart + 1
                            imgLikeIcon.setImageResource(R.drawable.icon_board_like_active)
                            val presentLikeCount = tvLikeCount.text.toString().toInt()
                            tvLikeCount.text = (presentLikeCount + 1).toString()
                            like = true
                        }
                        val intent = Intent()
                        intent.putExtra(Const.EXTRA_ARTICLE, supportInfoList.article)
                        setResult(ResultCode.UPDATE_LIKE_COUNT.value, intent)
                    } else {
                        Util.log("realize12나옴 ->  실 ->$response")
                    } //성공 리스폰스 되었을때 -> 이부분에서  뷰를 업데이트 해주도록 바꾸자.
                }, { throwable ->
                }
            )
        }
    }

    //서포트 관련  정보를 받아온다.
    private fun getSupportInfo() {
        MainScope().launch {
            supportRepository.getSupportDetail(supportInfoJSONObject.getString("support_id").toInt(),
                { response ->
                    Util.log("realize12나옴 ->  응답성공 ->$response")
                    val response = IdolGson.getInstance(false).fromJson( // 서포트는 UTC
                        response.toString(),
                        SupportListModel::class.java
                    )
                    recyclerviewAdapter.getSupportInfo(response)

                    //article필드에  support 장소 및  인증샷 이미지가 들어있음.
                    mArticle = response.article
                    like = response.like
                    supportInfoList = response
                    loadComments(response.article)
                },
                { throwable ->
                    Toast.makeText(this@SupportPhotoCertifyActivity, throwable.message, Toast.LENGTH_SHORT).show()
                })
        }
    }


    //리사이클러뷰  setting
    private fun setSupportRecyclerView() {
        recyclerviewAdapter =
            NewCommentAdapter(
                this,
                useTranslation = ConfigModel.getInstance(this).showTranslation,
                mGlideRequestManager,
                NewCommentAdapter.TYPE_SUPPORT,
                null,
                lifecycleScope,
                this.supportFragmentManager
            )
        binding.rcySupportPhoto.apply {
            adapter = recyclerviewAdapter
            itemAnimator = null
        }

        //비디오뷰
        recyclerviewAdapter.setVideoPlayerView(object :
            NewCommentAdapter.GetVideoPlayView {
            override fun getExoVideoPlayView(
                playerView: PlayerView?,
                imageView: ExodusImageView?,
                ivGif: AppCompatImageView?,
                videoUrl: String?
            ) {
                if (playerView != null) {
                    playExoPlayer(playerView, videoUrl)
                }
            }

        })

        recyclerviewAdapter.setSupportJson(supportInfoJSONObject)
    }

    override fun translateComment(item: CommentModel, position: Int) {
        commentTranslationHelper.clickTranslate(
            item = item,
            adapter = recyclerviewAdapter,
            position = position
        )
    }
    
    companion object {
        @JvmStatic
        fun createIntent(context: Context, supportInfoJson: String): Intent {
            val intent = Intent(context, SupportPhotoCertifyActivity::class.java)
            intent.putExtra("support_info", supportInfoJson)
            return intent
        }

        //푸쉬 받는곳에서 받을때  댓글 푸시 여부 체크가 추가됨으로
        //createIntent 메소드를 오버로딩 해서 씀.
        @JvmStatic
        fun createIntent(
            context: Context,
            supportInfoJson: String,
            isPushComment: Boolean
        ): Intent {
            val intent = Intent(context, SupportPhotoCertifyActivity::class.java)
            intent.putExtra("support_info", supportInfoJson)
            intent.putExtra("isCommentPush", isPushComment)
            return intent
        }
    }
}
