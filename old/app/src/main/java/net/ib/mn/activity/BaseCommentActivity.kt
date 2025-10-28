package net.ib.mn.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.adapter.FriendsAdapter
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.admanager.AdManager
import net.ib.mn.core.data.repository.comments.CommentsRepositoryImpl
import net.ib.mn.core.data.repository.friends.FriendsRepositoryImpl
import net.ib.mn.databinding.ActivitySupportPhotoCertifyBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.fragment.EmoticonFragment
import net.ib.mn.fragment.MultiWidePhotoFragment
import net.ib.mn.fragment.WidePhotoFragment
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.CommentModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.FriendModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.UserModel
import net.ib.mn.schedule.IdolSchedule.Companion.getInstance
import net.ib.mn.utils.CacheUtil.getCacheDataSourceFactory
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ImageUtil
import net.ib.mn.utils.KeyboardVisibilityUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.OnEmoticonClickListener
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.bindInputListener
import net.ib.mn.utils.getEmoticon
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

// TODO: 2020/12/14 현재까지  댓글 기능 중 친구  태그 기능들이  base로 빠짐. 앞으로 commentactitivity recyclerview 변환 작업 후,댓글  공통 기능 더 들어가야됨.
@AndroidEntryPoint
open class BaseCommentActivity : BaseActivity(),
    FriendsAdapter.OnClickListener,
    BaseDialogFragment.DialogResultHandler {
    protected val limit: Int = 50
    protected var cursor: String? = null
    protected var removeOffset = 0

    protected var mArticle: ArticleModel? = null
    protected var tagName: String = ""

    //스크롤  맨밑 체크를 위한  value들
    protected var pastVisibleItems: Int = 0
    protected var visibleItemCount: Int = 0
    protected var totalItemCount: Int = 0

    protected val commentModelList = arrayListOf<CommentModel>()

    protected lateinit var recyclerviewAdapter: NewCommentAdapter

    protected var isCommentPush = false;

    //보이는 아이템중 가장 마지막 포지션
    protected var lastComplete: Int = 0

    //현재 리사이클러뷰 스크롤이  맨아래 있는지 여부 체크
    protected var isScrollEnd = false

    //키보드 높이.
    protected var keyboardHeight = -1

    //이미지 버튼 연타방지.
    protected var mLastClickTime = 0L

    // 사진원본
    protected var originSrcUri: Uri? = null
    protected var originSrcWidth = 0
    protected var originSrcHeight = 0

    protected var rawImage = ""
    protected var binImage: ByteArray? = null
    protected var isExistImage: Boolean = false
    protected var isExistUmjjal: Boolean = false

    protected var simpleExoPlayerView: PlayerView? = null

    //친구 태그 기능
    val friendList = ArrayList<FriendModel>()
    val switchList = ArrayList<FriendModel>()
    val removeList = ArrayList<String>()

    //친구 태그 중복 안불리게 check
    var isPasting = false

    //친구 리사이클러뷰 adapter
    var friendsAdapter: FriendsAdapter? = null
    lateinit var mGlideRequestManager: RequestManager

    var isPurchasedDailyPack = false

    protected var localImageUri: Uri? = null //자신이 작성한 댓글의 이미지,움짤은 자신이 가지고 있으므로 자신의 uri 사용하기 위한 변수
    protected var localGif: Uri? = null

    private var videoUrl: String? = null

    protected lateinit var binding: ActivitySupportPhotoCertifyBinding

    protected var selectedEmoticonId: Int = CommentModel.NO_EMOTICON_ID

    @Inject
    lateinit var commentsRepository: CommentsRepositoryImpl
    @Inject
    lateinit var friendsRepository: FriendsRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadFriendsResources()

        setPurchasedDailyPackFlag(IdolAccount.getAccount(this))
    }

    protected fun setAdManager() {
        if (!isPurchasedDailyPack && !BuildConfig.CHINA) {
            val adManager = AdManager.getInstance()
            with(adManager) {
                setAdManagerSize(this@BaseCommentActivity, binding.clRoot)
                setAdManager(this@BaseCommentActivity)
                loadAdManager()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.resume()
        }

        //동영상으로 올땐  oncreate에서  simpleExoPlayerView가 initialized 됨으로,  exoplayer resume이 가능하다.
        if (this::simpleExoPlayerView != null && videoUrl != null) {
            playExoPlayer(simpleExoPlayerView, videoUrl)
        }

        //댓글이 없으면 키보드 show 감지후, scroll이 맨 아래라면 스크롤 올려줌.
        KeyboardVisibilityUtil(window, onShowKeyboard = {
            (binding.rcySupportPhoto.layoutManager as LinearLayoutManager).stackFromEnd =
                commentModelList.size < 0
            if (isScrollEnd) {
                binding.rcySupportPhoto.scrollToPosition(recyclerviewAdapter.itemCount - 1)
            }
        }, onHideKeyboard = {
            if (binding.viewComment.inputComment.text.toString().isEmpty()) {
                binding.viewComment.inputComment.clearFocus()
            }
            if (commentModelList.size <= 0) {
                (binding.rcySupportPhoto.layoutManager as LinearLayoutManager).stackFromEnd = false
            }

        })

        if (binding.viewComment.inputComment.hasFocus()) {
            binding.viewComment.inputComment.clearFocus()
        }
    }

    override fun onPause() {
        if (!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.pause()
        }
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PHOTO_SELECT_REQUEST && resultCode == RESULT_OK) {
            data!!.data?.let { cropArticlePhoto(it, false) }
        } else if (requestCode == PHOTO_CROP_REQUEST && resultCode == RESULT_OK) {
            if (mTempFileForCrop != null) {
                try {
                    onArticlePhotoSelected(Uri.fromFile(mTempFileForCrop))
                } catch (e: SecurityException) {
                    Toast.makeText(this, R.string.image_permission_error, Toast.LENGTH_SHORT).show()
                }
                mTempFileForCrop?.deleteOnExit()
            }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val resultUri = result.uri
                try {
                    onArticlePhotoSelected(resultUri)
                } catch (e: SecurityException) {
                    Toast.makeText(this, R.string.image_permission_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //기초 ui setting
    protected fun initBaseUI() {
        mGlideRequestManager = Glide.with(this)

        binding.viewComment.inputComment.requestFocus()
        Util.showSoftKeyboard(this, binding.viewComment.inputComment)
        //키보드 높이 리사이즈
        UtilK.resizeEmoticonKeyBoardHeight(this, binding.clRoot, binding.rlEmoticon)
        //엑티비티  처음 진입시 ->  댓글 푸시로  실행되었는지 여부를 알기 위한 값
        isCommentPush = intent.getBooleanExtra("isCommentPush", false)

        //이모티콘 가져오기.
        getEmoticon(
            this,
            rootView = binding.root,
            object: OnEmoticonClickListener {
                override fun onClickEmoticon(emoticonId: Int) {
                    selectedEmoticonId = emoticonId
                }
            },
        )

        //키보드 높이를 가져옵니다.
        keyboardHeight = Util.getPreferenceInt(this, Const.KEYBOARD_HEIGHT, -1)

        if (keyboardHeight == -1) { //초기화 안되었다는 뜻이므로 키보드 올라오게해줌.
            binding.viewComment.inputComment.callOnClick()
        }

        //만약 키보드 높이가 없다면.
        if (keyboardHeight != -1) {
            //아니면 키보드 높이 가져와서 이모티콘창 높이계산.
            val params = binding.rlEmoticon.layoutParams
            params.height = keyboardHeight
            binding.rlEmoticon.layoutParams = params
        }

        // 이모티콘 버튼 클릭
        binding.viewComment.btnEmoticon.setOnClickListener {
            lifecycleScope.launch {
                if (binding.rlEmoticon.visibility == View.GONE) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                    Util.hideSoftKeyboard(
                        this@BaseCommentActivity,
                        binding.viewComment.inputComment
                    )
                    binding.rlEmoticon.visibility = View.VISIBLE
//                    delay(100)
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

                    if (isScrollEnd) {//이모티콘 올라온경우도 scroll 마지막일때  리사이클러뷰 스크롤 bottom으로 내려줌.
                        binding.rcySupportPhoto.scrollToPosition(recyclerviewAdapter.itemCount - 1)
                    }
                    if (binding.viewComment.inputComment.text.toString().isEmpty()) {
                        binding.viewComment.inputComment.clearFocus()
                    }
                } else {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                    Util.showSoftKeyboard(
                        this@BaseCommentActivity,
                        binding.viewComment.inputComment
                    )
//                    delay(100)
                    binding.rlEmoticon.visibility = View.GONE
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                }
            }
        }

        //채팅 아이콘 클릭시 이모티콘 나오게.
        binding.viewComment.inputComment.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent): Boolean {

                val DRAWABLE_LEFT = 0
                val DRAWABLE_TOP = 1
                val DRAWABLE_RIGHT = 2
                val DRAWABLE_BOTTOM = 3

                var directionValue = binding.viewComment.inputComment.right
                var drawablesValue = DRAWABLE_RIGHT

                if (Util.isRTL(this@BaseCommentActivity)) {//아랍어일땐 반대로 방향지정해주기.
                    directionValue = binding.viewComment.inputComment.left
                    drawablesValue = DRAWABLE_LEFT
                }

                //아이콘 오른쪽에 넣었음. 만약왼쪽이면 부등호 반대로 해주세요.
                if (event.action == MotionEvent.ACTION_UP) {
                    lifecycleScope.launch {
                        if (binding.rlEmoticon.isVisible) { //이모티콘창 올라와있고 키보드 눌렀을때.
                            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                            Util.showSoftKeyboard(
                                this@BaseCommentActivity,
                                binding.viewComment.inputComment
                            )
                            delay(100)
                            binding.rlEmoticon.visibility = View.GONE
                            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                        } else {
                            binding.viewComment.inputComment.requestFocus()
                            Util.showSoftKeyboard(
                                this@BaseCommentActivity,
                                binding.viewComment.inputComment
                            )
                        }
                    }
                }
                return false
            }
        })

        //이모티콘,이미지,움짤 미리보기 닫기.
        binding.ivPreviewClose.setOnClickListener {
            binImage = null
            isExistImage = false
            isExistUmjjal = false

            selectedEmoticonId = CommentModel.NO_EMOTICON_ID
            binding.clPreview.visibility = View.GONE
        }

        binding.viewComment.btnGallery.setOnClickListener {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return@setOnClickListener
            }
            mLastClickTime = SystemClock.elapsedRealtime()
            UtilK.getPhoto(this, true)
        }

        bindInputListener(binding.root)

        binding.rcySupportPhoto.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                visibleItemCount =
                    (binding.rcySupportPhoto.layoutManager as LinearLayoutManager).childCount
                totalItemCount =
                    (binding.rcySupportPhoto.layoutManager as LinearLayoutManager).itemCount
                pastVisibleItems =
                    (binding.rcySupportPhoto.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

                lastComplete =
                    (binding.rcySupportPhoto.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()


                isScrollEnd = visibleItemCount + pastVisibleItems >= totalItemCount
            }
        })
    }

    protected fun setRecyclerViewListener() {
        recyclerviewAdapter.setOnCommentItemClickListener(object :
            NewCommentAdapter.OnCommentItemClickListener {
            override fun onCommentNameClicked(commentItem: CommentModel) {
                if (UtilK.isUserNotBlocked(
                        this@BaseCommentActivity,
                        commentItem.user?.id
                    )
                ) {  //사용자 이름 클릭 시 차단 사용자면 댓글에 태그 안되게 처리
                    val name = commentItem.user?.nickname ?: ""
                    val mentionText = "@{" + (commentItem.user?.id ?: 0) + ":" + name + "}"

                    val comment =
                        if (binding.viewComment.inputComment.text != null) binding.viewComment.inputComment.text.toString() else ""
                    if (comment.contains(mentionText)) {
                        return
                    }

                    isPasting = true // 붙여넣기 처리 루틴 안불리게


                    val e: Editable = binding.viewComment.inputComment.editableText

                    val sb: SpannableStringBuilder? = getMentionSpannableString(
                        commentItem.user?.id, name
                    )

                    val start = max(binding.viewComment.inputComment.selectionStart, 0)
                    val end = max(binding.viewComment.inputComment.selectionEnd, 0)
                    sb?.length?.let { e.replace(min(start, end), max(start, end), sb, 0, it) }
                }
            }

            override fun onCommentProfileImageClicked(commentItem: CommentModel) {
                val user = commentItem.user
                if (UtilK.isUserNotBlocked(
                        this@BaseCommentActivity,
                        user?.id
                    )
                ) {  //사용자 이미지 클릭 시 차단 사용자면 피드 안가지게 처리
                    startActivity(FeedActivity.createIntent(this@BaseCommentActivity, user))
                }
            }

            override fun onViewMoreItemClicked() {
                loadComments(mArticle)
            }

            override fun onRefreshClicked() {
                loadComments(mArticle)
            }

            override fun onCommentImageClicked(articleModel: ArticleModel) {
                WidePhotoFragment.getInstance(articleModel).apply {
                    setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                }.show(supportFragmentManager, "wide_photo")
            }
        })

        recyclerviewAdapter.setPhotoClickListener(object : ArticlePhotoListener {
            override fun widePhotoClick(model: ArticleModel, position: Int?) {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "community_widephoto",
                )

                if (model.files.isNullOrEmpty() || model.files.size < 2) {
                    WidePhotoFragment.getInstance(model).apply {
                        setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                    }.show(supportFragmentManager, "wide_photo")
                } else {
                    MultiWidePhotoFragment.getInstance(model, position ?: 0).apply {
                        setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                    }.show(supportFragmentManager, "wide_photo")
                }
            }

            override fun linkClick(link: String) {
                try {
                    val intent =
                        Intent(this@BaseCommentActivity, AppLinkActivity::class.java).apply {
                            data = Uri.parse(link)
                        }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    open fun setToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
        val padding = Util.convertDpToPixel(this, 10f).toInt()
        if (Util.isRTL(this)) {
            binding.toolbar.setPadding(padding, 0, 0, 0)
        } else {
            binding.toolbar.setPadding(0, 0, padding, 0)
        }        //키보드 높이 리사이즈
    }

    protected fun updateSubmitButton() {
        // 이미지/이모티콘 프리뷰가 없고 텍스트 입력한 것도 없으면 버튼 비활성화
        binding.viewComment.btnSubmit.isEnabled =
            !(binding.clPreview.visibility != View.VISIBLE && (binding.viewComment.inputComment.text?.isEmpty()
                ?: true))
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCode.ARTICLE_COMMENT_REMOVE.value) {
            if (resultCode == ResultCode.COMMENT_REMOVED.value) {
                for (i in 0 until commentModelList.size) {

                    //respurce uri 같은걸로 지워진걸 찾아서  댓글 리스트에서 지워준다.
                    if (commentModelList[i].resourceUri == data?.getStringExtra("resource_uri")) {

                        commentModelList.remove(commentModelList[i])
                        removeOffset--
                        break
                    }

                }


                //댓글은 삭제 했으니까 1씩 줄여주는데   기존 댓글 카운트가 0이거나 -가 나오면 0으로  처리해준다.(혹시몰라서 적용함)
                mArticle?.commentCount = if (mArticle?.commentCount!! <= 0) {
                    0
                } else {
                    mArticle?.commentCount!! - 1
                }

                //댓글 리스트  업데이트
                recyclerviewAdapter.setCommentList(
                    articleModel = mArticle!!,
                    commentModelList,
                    cursor != null
                )

                (binding.rcySupportPhoto.layoutManager as LinearLayoutManager).stackFromEnd = false
                isScrollEnd = true
            }
        }
        if (requestCode == RequestCode.ENTER_WIDE_PHOTO.value && resultCode == RESULT_CANCELED) {
            if (!isPurchasedDailyPack && !BuildConfig.CHINA) {
                AdManager.getInstance().loadAdManager()
            }
        }

        if (requestCode == RequestCode.ARTICLE_REPORT.value && resultCode == ResultCode.REPORTED.value) {
            val intent = Intent().apply {
                putExtra(Const.EXTRA_ARTICLE, mArticle)
            }

            setResult(ResultCode.REPORTED.value, intent)
            finish()
        }
    }

    protected fun loadComments(
        mArticle: ArticleModel?,
        completion: ((JSONObject) -> Unit)? = null
    ) {
        val elem = mArticle?.resourceUri?.split("/")
        if (elem?.get(elem.size - 2).isNullOrEmpty() || mArticle == null) {
            try {
                //article resourceUri에서 id가 널로 오나 체크 해봄.
                throw Exception("class : $this \n Article Model in loadComments Method::${mArticle?.resourceUri}")
            } catch (e1: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e1)
            }
        }

        val articleId = mArticle?.id?.toLong() ?: return
        MainScope().launch {
            commentsRepository.getCommentsCursor(
                articleId,
                cursor,
                limit,
                { response ->
                    recyclerviewAdapter.isLoading = false
                    recyclerviewAdapter.loadFailed = false

                    if (response.optBoolean("success")) {//response 성공
                        //다음 로드할 데이터가 있는지 여부
                        var nextCursor =
                            response.getJSONObject("meta").optString("next_cursor", null)
                        nextCursor = if (nextCursor == "null") null else nextCursor
                        val isNextDataExist: Boolean = nextCursor != null

                        val positionViewMore = commentModelList.size - 1

                        //댓글 전체  카운트
                        val numComments =
                            response.getJSONObject("article").optInt("num_comments", 0)

                        //댓글 리스트
                        val commentsList = response.getJSONArray("objects")

                        // paging 처리
                        if (cursor == null) {//맨처음 로드 일때는  전체 리스트 clear 한번
                            commentModelList.clear()
                        }
                        cursor = nextCursor

                        if (keyboardHeight == -1) //키보드값이 없다면 올려준다(올려주면 위에 addOnGlobalLayoutListener에서 감지함).
                        {
                            Util.showSoftKeyboard(
                                this@BaseCommentActivity,
                                binding.viewComment.inputComment
                            )
                        }

                        val gson = IdolGson.getInstance(true)
                        for (i in 0 until commentsList.length()) {
                            val obj: JSONObject = commentsList.getJSONObject(i)
                            val model: CommentModel = gson.fromJson(
                                obj.toString(),
                                CommentModel::class.java
                            )

                            commentModelList.add(model)
                        }

//                        //다음에 로드될 데이터가 있다면, 맨앞에  더보기 용으로  mockdata를 넣어주낟.
//                        if (isNextDataExist) {
//                            commentModelList.add(0, CommentModel())
//                        }

                        mArticle?.commentCount = numComments

                        if (mArticle != null) {
                            recyclerviewAdapter.setCommentList(
                                mArticle,
                                commentModelList,
                                isNextDataExist
                            )
                        }

                        if (isCommentPush) {//댓글 푸시로 실행했을때
                            isScrollEnd = true
                            binding.rcySupportPhoto.scrollToPosition(1)
                        }

                        // 더보기 표시하던 뷰 갱신
                        if (commentModelList.size > 0) {
                            recyclerviewAdapter.notifyItemChanged(positionViewMore)
                        }

                        completion?.invoke(response)
                    } else {//response 실패
                        UtilK.handleCommonError(this@BaseCommentActivity, response)
                        completion?.invoke(response)
                    }

                },
                { throwable ->
                    Toast.makeText(
                        this@BaseCommentActivity,
                        R.string.error_abnormal_exception, Toast.LENGTH_SHORT
                    ).show()
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                    recyclerviewAdapter.isLoading = false
                    recyclerviewAdapter.loadFailed = true
                    recyclerviewAdapter.notifyItemChanged(recyclerviewAdapter.itemCount - 1)
                })
        }

    }

    //댓글 입력시  호출됨.
    protected fun writeComments(
        articleModel: ArticleModel,
        idol: IdolModel,
        emoticonId: Int,
        comment: String,
        binImage: ByteArray?,
        isSchedule: Boolean = false
    ) {

        val tmp = if (TextUtils.isEmpty(comment)) "" else comment.trim { it <= ' ' }
        var excludeMention = tmp.replace("@\\{\\d+\\:([^\\}]+)\\}".toRegex(), "").trim { it <= ' ' }
        excludeMention = excludeMention.replace("\\s".toRegex(), "")

        if (TextUtils.isEmpty(tmp) && emoticonId == CommentModel.NO_EMOTICON_ID && binImage == null) {
            return
        }

        //멘션을 제외한  글자가  미니멈 미만이고  이모티콘이 있는 경우 comment가  일단  안비어있으면 -> 미니멈 length 체크 실행
        if ((binImage == null && excludeMention.length < Const.MINIMUM_COMMENT_LENGTH
                && emoticonId == CommentModel.NO_EMOTICON_ID)
            || (!comment.isNullOrEmpty()
                && excludeMention.length < Const.MINIMUM_COMMENT_LENGTH
                && emoticonId != CommentModel.NO_EMOTICON_ID)
            || (binImage != null
                && !TextUtils.isEmpty(tmp)
                && excludeMention.length < Const.MINIMUM_COMMENT_LENGTH)
        ) {
            Util.hideSoftKeyboard(this@BaseCommentActivity, binding.viewComment.inputComment)
            Util.showDefaultIdolDialogWithBtn1(
                this, null, String.format(
                    getString(R.string.comment_minimum_characters),
                    Const.MINIMUM_COMMENT_LENGTH
                )
            ) {
                Util.closeIdolDialog()
            }
            return
        }

        val commentFiltered = Util.BadWordsFilterToHeart(this, tmp)

        Util.showProgress(this)

        //댓글 write api 부름
        UtilK.commentCheckHash(this, idol.getId(), binImage, lifecycleScope, commentsRepository) { cdnImageUrl ->
            callWriteCommentApi(
                articleModel,
                emoticonId,
                commentFiltered,
                comment,
                cdnImageUrl,
                isSchedule
            )
        }
    }

    private fun callWriteCommentApi(
        article: ArticleModel,
        emoticonId: Int,
        commentFiltered: String,
        comment: String,
        cdnImageUrl: String?,
        isSchedule: Boolean
    ) {
        MainScope().launch {
            commentsRepository.writeCommentMultipart(
                articleId = article.id.toLong(),
                emoticonId = emoticonId,
                content = commentFiltered,
                image = if (!cdnImageUrl.isNullOrEmpty()) null else binImage,
                imageUrl = cdnImageUrl,
                listener = { response ->
                    if (response.optBoolean("success")) {

                        //업로드 되었으니,  text뷰 비어줌.
                        binding.viewComment.inputComment.setText("")

                        //키보드 숨기기
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(binding.viewComment.inputComment.windowToken, 0)

                        binding.rlEmoticon.visibility = View.GONE

                        //댓글 카운트 1 올려줌.
                        article.commentCount += 1
                        // 댓글 쓰고 UI에  바로 추가  혹시  exception 나오면  서버에서  받아옴.
                        try {

                            if (isSchedule) {
                                getInstance().lastSchedule?.num_comments = article.commentCount
                            }

                            //새로 추가될  댓글
                            val obj = response.getJSONObject("object")
                            val newComment = IdolGson.getInstance(true).fromJson(
                                obj.toString(),
                                CommentModel::class.java
                            )
                            Util.log(response.toString())
                            commentModelList.add(0, newComment)

                            // supportListModel.like = like // 서포트에서 이게 왜 여기 있어야하는지 의문임..?

                            //리사이클러뷰 업데이트
                            recyclerviewAdapter.setCommentList(
                                article,
                                commentModelList,
                                cursor != null
                            )

                            //이모티콘 댓글을 보낸후에는  이모티콘 미리보기  화면 gone처리 해주고,
                            //이모티콘 id도 리셋시켜준다.
                            if (binding.clPreview.visibility == View.VISIBLE) {
                                binding.clPreview.visibility = View.GONE
                                selectedEmoticonId = CommentModel.NO_EMOTICON_ID
                            }

                            //프로그래스바  close
                            Util.closeProgress()

                            // 입력한 댓글로 이동
                            binding.rcySupportPhoto.scrollToPosition(1)
                            val intent = Intent()
                            intent.putExtra(Const.EXTRA_ARTICLE, article)
                            setResult(ResultCode.COMMENTED.value, intent)
                        } catch (e: Exception) {
                            // 댓글 쓰고 미러링 반영되게 1초 후 로딩
                            Handler().postDelayed({
                                loadComments(article) {}
                                Util.closeProgress()
                            }, 1000)
                        }
                        (binding.rcySupportPhoto.layoutManager as LinearLayoutManager).stackFromEnd =
                            false
                        isScrollEnd = true

                    } else {//댓글 업로드 실패시

                        Util.closeProgress()
                        val gcode = response.optInt("gcode")
                        if (gcode == 2000 || gcode == ErrorControl.ERROR_1111
                        ) {
                            val responseMsg = response.optString("msg")
                            Toast.makeText(
                                this@BaseCommentActivity, responseMsg,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@writeCommentMultipart
                        }
                        val responseMsg = ErrorControl.parseError(
                            this@BaseCommentActivity, response
                        )
                        Toast.makeText(
                            this@BaseCommentActivity, responseMsg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binImage = null //전역변수라 write한 후에는 초기화
                    isExistImage = false  //write 한 후 이미지 존재 여부 false로 변경
                    isExistUmjjal = false //write 한 후 움짤 존재 여부 false로 변경
                },
                { throwable ->
                    Toast.makeText(
                        this@BaseCommentActivity,
                        R.string.error_abnormal_exception, Toast.LENGTH_SHORT
                    ).show()
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                    Util.closeProgress()
                }
            )
        }
    }

    @OptIn(UnstableApi::class)
    fun playExoPlayer(
        view: PlayerView?,
        url: String?,
        thumbnailView: ImageView? = null,
    ) {
        val view = view ?: return
        if (!Const.USE_ANIMATED_PROFILE) {
            return
        }

        if (this.player1 == null) {
            player1 = createExoPlayer(0)
        }
        val player: ExoPlayer = this.player1 ?: return
        // 이전 재생되던거 멈추고
        player.stop()
        // 이전 플레이어 제거
        if (simpleExoPlayerView != null) {
            PlayerView.switchTargetView(player, simpleExoPlayerView, view)
        } else {
            view.player = player
        }
        simpleExoPlayerView = view
        // 움짤 없으면
        if (url == null
            || !url.endsWith("mp4") && !url.endsWith("_s_mv.jpg")
        ) {
            view.visibility = View.INVISIBLE
            thumbnailView?.visibility = View.VISIBLE
            return
        }

        // tag를 설정해서 동영상 있는지 여부 확인
        view.tag = url
        val urlVideo: String =
            if (url.endsWith("mp4")) url else url.replace("_s_mv.jpg", "_m_mv.mp4")

        // 스크롤 속도 문제로 GONE 해놓은걸 다시 VISIBLE로 바꿔서 재생 시작 이벤트 받게 처리
        view.visibility = View.VISIBLE
        thumbnailView?.visibility = View.VISIBLE

        // 검은색 셔터 화면 안나오게 처리
        val imageShutter: ImageView
//        val shutter = view.findViewById<View>(R.id.exo_shutter)
        val shutterId = resources.getIdentifier("exo_shutter", "id", packageName)
        val shutter = view.findViewById<View>(shutterId)
        if (shutter is ImageView) {
            imageShutter = shutter
        } else {
            val shutterParent = shutter.parent as FrameLayout
            shutterParent.removeView(shutter)
            imageShutter = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                id = shutterId //R.id.exo_shutter
                setBackgroundColor(-0x1)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            shutterParent.addView(imageShutter, 0)
        }
        mGlideRequestManager
            .load(url)
            .disallowHardwareConfig()
            .dontAnimate()
            .dontTransform()
            .placeholder(R.drawable.bg_loading)
            .into(imageShutter)

        Logger.v("보이냐 ->" + simpleExoPlayerView!!.isVisible)
        view.post {
            videoUrl = urlVideo
            // ....._s_mv.jpg 가 있으면 이를  _m_mv.mp4로 변환
            // ....._s_mv.jpg 가 있으면 이를  _m_mv.mp4로 변환
            val cacheDataSourceFactory = getCacheDataSourceFactory(this)
            val extractorsFactory: ExtractorsFactory = DefaultExtractorsFactory()

            val videoSource: MediaSource =
                ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(videoUrl)))

            player.addMediaSource(videoSource)
            player.repeatMode = Player.REPEAT_MODE_ONE
            view.useController = false
            player.prepare()
            player.playWhenReady = true
            Util.log("playing $videoUrl")
        }
    }


    //친구 리스트 보여주는  recyclerview setting
    fun setFriendsRecyclerView(recyclerView: RecyclerView) {
        friendsAdapter = FriendsAdapter(
            this, mGlideRequestManager, switchList, false,
            this
        )

        recyclerView.adapter = friendsAdapter
        removeList.clear()
        switchList.addAll(friendList)
    }


    //사용자가  커멘트에  @를 사용햇는지  판별하는  메소드
    //true 리턴시 -> 친구리스트 보여줌  false는  무
    fun checkTagExist(comment: String): Boolean {

        // 가장 마지막 @를  기준으로  그 앞뒤  char 값 index 정의
        val afterTagCharIndex = comment.lastIndexOf("@") + 1
        val beforeTagCharIndex = comment.lastIndexOf("@") - 1

        //태그를 진행 하려는 comment 중간에  -> space 가 발견되면  태그 동작을  중지한다.
        val isWhiteSpaceExist = comment.substring(afterTagCharIndex).contains(" ")

        //@태그  앞에 빈칸이 있을때 랑  맨처음 @를 쳤을때
        return if (beforeTagCharIndex > -1 && comment[beforeTagCharIndex] == ' ' && !isWhiteSpaceExist || afterTagCharIndex == 1 && !isWhiteSpaceExist) {

            //골뱅이 시작부터 친구리스트 보여주기위해 적용
            if (comment.endsWith("@")) {
                switchList.clear()
                switchList.addAll(friendList)
                friendsAdapter?.notifyDataSetChanged()
                true
            } else {
                // TODO: 2020/11/12  추후에  @{로  시작하는  유저 닉네임도 같이 사라지므로,  정규식 리팩토링 또는  닉네임  제한이 필요함.
                //spnnable 처리된 string 의 경우 @{로 시작해서,  리스트가 띄어질수 있므로,
                //아래와 같이 막음 처리.
                if (comment[afterTagCharIndex] == '{') {
                    false
                } else {
                    switchList.clear()
                    for (i in friendList.indices) {
                        if (friendList.get(i).user.nickname
                                .contains(comment.substring(afterTagCharIndex))
                        ) {
                            switchList.add(friendList.get(i))
                        }
                        friendsAdapter?.notifyDataSetChanged()
                    }
                    true
                }
            }
        } else {
            false
        }
    }


    //친구리스트 불러오기
    private fun loadFriendsResources() {
        MainScope().launch {
            friendsRepository.getFriendsSelf(
                { response ->
                    if (response.optBoolean("success")) {
                        try {
                            val gson = IdolGson.getInstance()
                            val array = response.getJSONArray("objects")
                            val friends = java.util.ArrayList<FriendModel>()
                            Util.setPreference(
                                this@BaseCommentActivity,
                                Const.PREF_FRIENDS_LIMIT,
                                array.length() >= Const.THE_NUMBER_OF_FRIENDS_LIMIT
                            )
                            try {
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    Util.log("Friends nicknameis: $obj")
                                    val model = gson.fromJson(
                                        obj.toString(),
                                        FriendModel::class.java
                                    )
                                    Util.log("isFriendis :" + model.isFriend)
                                    //친구인 유저만 보여주기
                                    if (model.isFriend == "Y") {
                                        friends.add(model)
                                        Util.log("NickName Is: " + model.user.nickname)
                                    }
                                }
                                friendList.addAll(friends)

                                friendsAdapter!!.notifyDataSetChanged()
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        UtilK.handleCommonError(this@BaseCommentActivity, response)
                    }
                }, { throwable ->
                    Toast.makeText(
                        this@BaseCommentActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            )
        }
    }

    //태그 처리
    fun getMentionSpannableString(id: Int?, name: String): SpannableStringBuilder? {
        if (id == null || name.isEmpty()) {
            return null
        }

        val mentionText = "@{$id:$name}"
        val sb = SpannableStringBuilder()
        var tv = Util.createMentionTextView(this, name)
        var bd: BitmapDrawable? = Util.convertViewToDrawable(tv) as BitmapDrawable?

        // 닉네임이 너무 길어서 댓글 작성시 입력하는 글이 안보이는 경우가 있음
        if (bd != null) {
            val w = bd.intrinsicWidth
            if (w > Util.getDeviceWidth(this) - 200) {
                bd = null
            }
        }

        // 닉네임이 아주 길면 익셉션이 발생하거나 비트맵이 비거나 하는 경우가 발생함
        if (bd == null) {
            // 글자 줄여서 재시도
            var len = name.length
            len = if (len > 20) {
                20
            } else {
                Math.min(10, len)
            }
            tv = Util.createMentionTextView(this, name.substring(0, len) + "...")
            bd = Util.convertViewToDrawable(tv) as BitmapDrawable?
            if (bd == null) {
                if (len > 10) {
                    len = 10
                }
                tv = Util.createMentionTextView(
                    this,
                    name.substring(0, len) + "..."
                )
                bd = Util.convertViewToDrawable(tv) as BitmapDrawable
            }
        }

        // 그래도 bd==null인 경우가 있다.
        val fontSize = 30 / Util.convertDpToPixel(this, 1f)


        val scale = this.resources.configuration.fontScale
        val ratio = if (scale >= 1.5f) {//어르신모드 적용시
            8 / fontSize
        } else {
            // 15sp에 맞게 크기 조절
            15 / fontSize
        }

        bd.setBounds(
            0,
            0,
            (bd.intrinsicWidth * ratio).toInt(),
            (bd.intrinsicHeight * ratio).toInt()
        )
        sb.append(mentionText)
        sb.setSpan(
            ImageSpan(bd, mentionText), 0, mentionText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        sb.append(" ")
        return sb
    }

    //
    fun inputCommentChangeListen(editText: EditText) {

        editText.addTextChangedListener(object : TextWatcher {
            private var spanLength = -1
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                try {
                    Util.log("Start : $start before :$before count :$count")
                    if (spanLength > -1) {
                        val length = spanLength
                        spanLength = -1
                        editText.editableText.replace(start - length, start, "")
                    }
                    if (count > 2 && !isPasting) {
                        // text pasted
                        isPasting = true // beforeTextChanged 가 중복 호출되지 않게
                        val e: Editable = editText.editableText

                        // 댓글 내용중 멘션(@{...:...})이 있으면 처리. 닉네임에 {}를 쓰는 경우가 있어 정규표현식이 아닌 그냥 무식하게 찾기
                        // s는 붙여넣은 후의 전체 텍스트
                        val pasted = s.toString().substring(start, start + count)
                        var i = 0
                        while (i < pasted.length - 1) {
                            if (pasted[i] == '@' && pasted[i + 1] == '{') {
                                var countBrace = 1
                                for (k in i + 2 until start + pasted.length) {
                                    if (pasted[k] == '{') {
                                        countBrace++
                                    } else if (pasted[k] == '}') {
                                        countBrace--
                                    }
                                    if (countBrace == 0) {
                                        // i+2부터 k-1까지가 id:닉네임 부분
                                        val mention = pasted.subSequence(i + 2, k).toString()
                                        val id = Integer.valueOf(
                                            mention.substring(0, mention.indexOf(':'))
                                        )
                                        val name = mention.substring(mention.indexOf(':') + 1)
                                        val sb_m = getMentionSpannableString(
                                            id,
                                            name
                                        )
                                        e.replace(start + i, start + k + 1, sb_m)
                                        i = k + 1
                                        break
                                    }
                                }
                            }
                            i++
                        }
                    }

                    //유저가  @태그를 사용했는지 판별
                    setFriendListVisible(editText.text.toString())
                } catch (e: IndexOutOfBoundsException) {
                    e.printStackTrace()

                }

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                if (start == 0) return
                if (count > after) {
                    val spans: Array<ImageSpan> = editText.editableText.getSpans(
                        start + count,
                        start + count, ImageSpan::class.java
                    )
                    if (spans == null || spans.isEmpty()) return
                    for (span in spans) {
                        val end: Int = editText.editableText.getSpanEnd(span)
                        if (end != start + count) continue
                        val text = span.source
                        if (text != null && text.isNotEmpty()) {
                            spanLength = text.length - 1
                        }
                        editText.editableText.removeSpan(span)
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {
                isPasting = false
            }
        })
    }

    // 태그 존재  여부를 판단해서  태그할  친구리스트 목록을  보여줄지 결정한다.
    fun setFriendListVisible(comment: String) {
        if (checkTagExist(comment) && friendList.isNotEmpty() && switchList.isNotEmpty()) { //친구가 0명이상일때
            showFriendsList(true)
        } else {
            showFriendsList(false)
        }
    }

    private fun showFriendsList(show: Boolean) {
        binding.friendsRcyView.visibility = if (show) View.VISIBLE else View.GONE
        binding.rcySupportPhoto.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.destroy()
        }

        player1?.release()

        //화면 없어질때 리스트 클리어.
        if (friendList.isNotEmpty()) {
            friendList.clear()
        }
    }

    //태그 아이템 클릭시  진행
    override fun onItemClicked(item: UserModel, view: View, position: Int) {
        when (view.id) {
            R.id.section2, R.id.userInfo, R.id.btnSendHeart, R.id.picture -> {
                val name = item.nickname
                val mentionText = "@{" + item.id + ":" + name + "}"

                // 시스템 문제로 닉네임이 빈 애들이 드물게 있음
                if (name != null && name.trim { it <= ' ' }.isEmpty()) {

                }
                val comment =
                    if (binding.viewComment.inputComment.text != null) binding.viewComment.inputComment.text.toString() else ""
                if (comment.contains(mentionText)) {
                    return
                }
                isPasting = true // 붙여넣기 처리 루틴 안불리게
                val e: Editable = binding.viewComment.inputComment.editableText
                val sb = getMentionSpannableString(
                    item.id, name!!
                )


                //태그를  한 시점부터  교체가 되어야 함으로
                //가장  최근  @를  start로 잡는다.
                val start =
                    max(binding.viewComment.inputComment.text.toString().lastIndexOf("@"), 0)
                val end = max(binding.viewComment.inputComment.selectionEnd, 0)
                e.replace(min(start, end), max(start, end), sb, 0, sb!!.length)
                binding.rcySupportPhoto.visibility = View.VISIBLE
                binding.friendsRcyView.visibility = View.INVISIBLE
            }
        }
    }

    protected fun stopExoPlayer(view: PlayerView?) {
        if (view == null) return
        val player = view.player as ExoPlayer?
        if (player == null) {
            Util.log("         stopExoPlayer player is NULL")
            return
        }

        // 썸네일 다시 보이기 및 리스너 제거해서 까만 화면 나오는 현상 제거
        val parent = view.parent as ViewGroup
        try {
            parent.findViewById<View>(R.id.eiv_attach_photo).visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 검은색으로 깜빡이는 현상 방지
        parent.post {
            player.playWhenReady = false
            // 스크롤할 때 느리지 않게
            view.visibility = View.GONE
        }
    }

    private fun cropArticlePhoto(uri: Uri, isSharedImage: Boolean) {
        ImageUtil.cropArticlePhoto(this,
            uri,
            isSharedImage,
            false,
            ConfigModel.getInstance(this).articleMaxSize * 1024 * 1024,
            { fileData ->
                if (binding.clPreview.visibility == View.GONE) {
                    binding.clPreview.visibility = View.VISIBLE
                }
                // GIF!
                Util.log("FILE is GIF")
                if (Const.USE_MULTIPART_FORM_DATA) {
                    binImage = fileData
                } else {
                    rawImage = Base64.encodeToString(fileData, Base64.NO_WRAP)
                }
                isExistUmjjal = true
                mGlideRequestManager.asGif().load(fileData).transform(CenterInside())
                    .into(binding.ivPreview)

            },
            {
            },
            { options ->
                originSrcUri = uri
                originSrcWidth = options.outWidth
                originSrcHeight = options.outHeight

            })
    }

    protected fun onArticlePhotoSelected(uri: Uri) {
        if (binding.clPreview.visibility == View.GONE) {
            binding.clPreview.visibility = View.VISIBLE
        }
        localImageUri = uri
        mGlideRequestManager.load(uri).transform(CenterInside()).into(binding.ivPreview)


        // Get the source image's dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(uri.path, options)
        var srcWidth = options.outWidth
        var srcHeight = options.outHeight
        val stream = ByteArrayOutputStream()
        if (originSrcWidth == srcWidth && originSrcHeight == srcHeight && srcWidth <= Const.MAX_IMAGE_WIDTH) { // 이게 없어서 엄청 큰 이미지가 올라가고 있었음...
            try {
                val `is` = originSrcUri?.let { contentResolver.openInputStream(it) }
                val bufferSize = 1024
                val buffer = ByteArray(bufferSize)
                var len = 0
                while (`is`!!.read(buffer).also { len = it } != -1) {
                    stream.write(buffer, 0, len)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            var desiredWidth = Const.MAX_IMAGE_WIDTH
            // Only scale if the source is big enough. This code is just trying to fit a image into a certain width.
            if (desiredWidth > srcWidth) desiredWidth = srcWidth

            // 무한루프 방지용
            if (desiredWidth <= 1) {
                return
            }
            var inSampleSize = 1
            while (srcWidth / 2 > desiredWidth) { // 여기서 무한루프에 빠지는 경우가 있음
                srcWidth /= 2
                srcHeight /= 2
                inSampleSize *= 2
            }
            val desiredScale = desiredWidth.toFloat() / srcWidth

            // Decode with inSampleSize
            options.inJustDecodeBounds = false
            options.inDither = false
            options.inSampleSize = inSampleSize
            options.inScaled = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val sampledSrcBitmap = BitmapFactory.decodeFile(uri.path, options)

            // Resize
            val matrix = Matrix()
            matrix.postScale(desiredScale, desiredScale)
            val scaledBitmap = Bitmap.createBitmap(
                sampledSrcBitmap!!,
                0,
                0,
                sampledSrcBitmap.width,
                sampledSrcBitmap.height,
                matrix,
                true
            )

            // setImageUri가 안되는 폰이 있음.
            binding.ivPreview.setImageBitmap(scaledBitmap)

            // Save
            scaledBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        }
        isExistImage = true
        if (Const.USE_MULTIPART_FORM_DATA) {
            binImage = stream.toByteArray()
        } else {
            rawImage = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
        }
    }

    protected fun setPurchasedDailyPackFlag(account: IdolAccount?) {
        // TODO: IdolAccount kotlin 전환 후 단축
        if (account?.userModel != null
            && account?.userModel?.subscriptions != null
            && account?.userModel?.subscriptions!!.isNotEmpty()
        ) {
            for (mySubscription in account?.userModel?.subscriptions!!) {
                if (mySubscription.familyappId == 1 || mySubscription.familyappId == 2) {
                    if (mySubscription.skuCode == Const.STORE_ITEM_DAILY_PACK) {
                        isPurchasedDailyPack = true
                        break
                    }
                }
            }
        } else {
            isPurchasedDailyPack = false
        }
    }

    open class PagerAdapter(
        fm: FragmentActivity,
        emoFragList: java.util.ArrayList<EmoticonFragment>
    ) : FragmentStateAdapter(fm) {

        val fragList = emoFragList

        override fun getItemCount(): Int {
            return fragList.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragList[position]
        }
    }
}