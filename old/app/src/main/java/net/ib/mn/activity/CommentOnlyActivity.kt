package net.ib.mn.activity

import android.content.Context
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
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.adapter.CommentOnlyAdapter
import net.ib.mn.adapter.FriendsAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.HeartpickRepository
import net.ib.mn.core.data.repository.comments.CommentsRepository
import net.ib.mn.core.data.repository.friends.FriendsRepositoryImpl
import net.ib.mn.databinding.ActivityCommentOnlyBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.fragment.MultiWidePhotoFragment
import net.ib.mn.fragment.WidePhotoFragment
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.CommentModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import net.ib.mn.model.contentAlt
import net.ib.mn.utils.CommentTranslation
import net.ib.mn.utils.CommentTranslationHelper
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ImageUtil
import net.ib.mn.utils.OnEmoticonClickListener
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.bindInputListener
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.getEmoticon
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Date
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * 하트픽 댓글
 */

@AndroidEntryPoint
open class CommentOnlyActivity : BaseActivity(), CommentTranslation,
    BaseDialogFragment.DialogResultHandler, FriendsAdapter.OnClickListener{

    private lateinit var binding: ActivityCommentOnlyBinding
    private lateinit var mGlideRequestManager: RequestManager
    private lateinit var commentOnlyAdapter: CommentOnlyAdapter
    @Inject
    lateinit var friendsRepository: FriendsRepositoryImpl
    @Inject
    lateinit var commentsRepository: CommentsRepository
    @Inject
    lateinit var heartpickRepository: HeartpickRepository

    private var heartPickId: Int = 0

    private var cursor: String? = null
    private val limit: Int = 50
    private val commentModelList = arrayListOf<CommentModel>()
    protected var keyboardHeight = -1


    //이미지 버튼 연타방지.
    protected var mLastClickTime = 0L

    //스크롤  맨밑 체크를 위한  value들
    protected var pastVisibleItems: Int = 0
    protected var visibleItemCount: Int = 0
    protected var totalItemCount: Int = 0

    //현재 리사이클러뷰 스크롤이  맨아래 있는지 여부 체크
    protected var isScrollEnd = false

    // 사진원본
    protected var originSrcUri: Uri? = null
    protected var originSrcWidth = 0
    protected var originSrcHeight = 0

    protected var rawImage = ""
    protected var binImage: ByteArray? = null
    protected var isExistImage : Boolean = false
    protected var isExistUmjjal : Boolean = false
    protected var localGif : Uri? = null

    protected var localImageUri : Uri? = null //자신이 작성한 댓글의 이미지,움짤은 자신이 가지고 있으므로 자신의 uri 사용하기 위한 변수

    //보이는 아이템중 가장 마지막 포지션
    protected var lastComplete: Int = 0

    //친구 태그 기능
    val friendList = ArrayList<FriendModel>()
    val switchList = ArrayList<FriendModel>()
    val removeList = ArrayList<String>()
    var friendsAdapter: FriendsAdapter? = null

    //친구 태그 중복 안불리게 check
    var isPasting = false

    var selectedEmoticonId: Int = CommentModel.NO_EMOTICON_ID

    @Inject
    lateinit var commentTranslationHelper: CommentTranslationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_comment_only)
        binding.clRoot.applySystemBarInsets()

        supportActionBar?.title = getString(R.string.cheering_comments)

        init()
    }

    private fun init() {
        mGlideRequestManager = Glide.with(this)

        heartPickId = intent.getIntExtra(HEART_PICK_ID, 0)

        initBaseUI()
        setAdapter()
        setCommentRecyclerViewListener()
        loadComments()
        loadFriendsResources()
    }

    private fun setAdapter() {
        commentOnlyAdapter = CommentOnlyAdapter(
            mGlideRequestManager = mGlideRequestManager,
            useTranslation = ConfigModel.getInstance(this).showTranslation,
            lifecycleScope = lifecycleScope
        )
        binding.rvCommentOnly.adapter = commentOnlyAdapter
        binding.friendsRcyView.visibility = View.GONE
        setFriendsRecyclerView(binding.friendsRcyView)
        inputCommentChangeListen(binding.viewComment.inputComment)
    }

    private fun setCommentRecyclerViewListener() {
        commentOnlyAdapter.setOnCommentItemClickListener(object :
            CommentOnlyAdapter.OnCommentItemClickListener {
            override fun onCommentNameClicked(commentItem: CommentModel) {
                if(UtilK.isUserNotBlocked(this@CommentOnlyActivity, commentItem.user?.id)) {  //사용자 이름 클릭 시 차단 사용자면 댓글에 태그 안되게 처리
                    val name: String = commentItem.user?.nickname ?: ""
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
                val user: UserModel? = commentItem.user
                if(UtilK.isUserNotBlocked(this@CommentOnlyActivity, user?.id)) {  //사용자 이미지 클릭 시 차단 사용자면 피드 안가지게 처리
                    startActivity(FeedActivity.createIntent(this@CommentOnlyActivity, user))
                }
            }

            override fun onViewMoreItemClicked() {
                loadComments()
            }

            override fun onRefreshClicked() {
                loadComments()
            }

            override fun onCommentImageClicked(articleModel: ArticleModel) {
                WidePhotoFragment.getInstance(articleModel).apply {
                    setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                }.show(supportFragmentManager, "wide_photo")
            }
        })

        commentOnlyAdapter.setPhotoClickListener(object: ArticlePhotoListener {
            override fun widePhotoClick(model: ArticleModel, position: Int?) {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "community_widephoto",
                )

                if(model.files.isNullOrEmpty() || model.files.size < 2) {
                    WidePhotoFragment.getInstance(model).apply {
                        setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                    }.show(supportFragmentManager, "wide_photo")
                } else {
                    MultiWidePhotoFragment.getInstance(model, position?:0).apply {
                        setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value)
                    }.show(supportFragmentManager, "wide_photo")
                }
            }

            override fun linkClick(link: String) {
                try {
                    val intent = Intent(this@CommentOnlyActivity, AppLinkActivity::class.java).apply {
                        data = Uri.parse(link)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun initBaseUI(){
        mGlideRequestManager = Glide.with(this)

        //키보드 높이 리사이즈
        binding.viewComment.inputComment.requestFocus()
        Util.showSoftKeyboard(this, binding.viewComment.inputComment)

        //이모티콘 가져오기.
        getEmoticon(this,
            rootView = binding.root,
            object: OnEmoticonClickListener {
                override fun onClickEmoticon(emoticonId: Int) {
                    selectedEmoticonId = emoticonId
                }
            },
        )

        // 이모티콘 버튼 클릭
        binding.viewComment.btnEmoticon.setOnClickListener {
            if (binding.rlEmoticon.visibility == View.GONE) {
                Util.hideSoftKeyboard(this@CommentOnlyActivity, binding.viewComment.inputComment)
                binding.rlEmoticon.visibility = View.VISIBLE
                if(binding.viewComment.inputComment.text.toString().isEmpty()){
                    binding.viewComment.inputComment.clearFocus()
                }
            } else {
                Util.showSoftKeyboard(this@CommentOnlyActivity, binding.viewComment.inputComment)
                binding.rlEmoticon.visibility = View.GONE
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

                if(Util.isRTL(this@CommentOnlyActivity)) {//아랍어일땐 반대로 방향지정해주기.
                    directionValue = binding.viewComment.inputComment.left
                    drawablesValue = DRAWABLE_LEFT
                }

                //아이콘 오른쪽에 넣었음. 만약왼쪽이면 부등호 반대로 해주세요.
                if (event.action == MotionEvent.ACTION_UP) {
                    lifecycleScope.launch {
                        if (binding.rlEmoticon.isVisible) { //이모티콘창 올라와있고 키보드 눌렀을때.
                            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                            Util.showSoftKeyboard(this@CommentOnlyActivity, binding.viewComment.inputComment)
                            delay(100)
                            binding.rlEmoticon.visibility = View.GONE
                            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                        } else {
                            binding.viewComment.inputComment.requestFocus()
                            Util.showSoftKeyboard(this@CommentOnlyActivity, binding.viewComment.inputComment)
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
            if(SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                return@setOnClickListener
            }
            mLastClickTime = SystemClock.elapsedRealtime()
            UtilK.getPhoto(this, true)
        }

        binding.viewComment.btnSubmit.setOnClickListener {
            writeComments(selectedEmoticonId, Util.BadWordsFilterToHeart(this, binding.viewComment.inputComment.text.toString()), binImage)
        }

        bindInputListener(binding.root)
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

    private fun loadComments(completion: ((JSONObject) -> Unit)? = null ) {
        lifecycleScope.launch {
            heartpickRepository.getReplies(
                id = heartPickId,
                limit = limit,
                cursor = cursor,
                listener = { response ->
                    commentOnlyAdapter.isLoading = false
                    commentOnlyAdapter.loadFailed = false

                    if (response.optBoolean("success")) {//response 성공
                        //다음 로드할 데이터가 있는지 여부
                        var nextCursor = response.getJSONObject("meta").optString("next_cursor", null)
                        nextCursor = if(nextCursor == "null") null else nextCursor
                        val isNextDataExist: Boolean = nextCursor != null

                        val positionViewMore = commentModelList.size - 1

                        //댓글 리스트
                        val commentsList = response.getJSONArray("objects")

                        // paging 처리
                        if (cursor == null) {//맨처음 로드 일때는  전체 리스트 clear 한번
                            commentModelList.clear()
                        }
                        cursor = nextCursor

                        if (keyboardHeight == -1) //키보드값이 없다면 올려준다(올려주면 위에 addOnGlobalLayoutListener에서 감지함).
                        {
                            Util.showSoftKeyboard(this@CommentOnlyActivity, binding.viewComment.inputComment)
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

                        commentOnlyAdapter.getCommentList(
                            commentModelList,
                            isNextDataExist
                        )

                        // 더보기 표시하던 뷰 갱신
                        if( commentModelList.size > 0 ) {
                            commentOnlyAdapter.notifyItemChanged(positionViewMore)
                        }

                        completion?.invoke(response)
                    } else {//response 실패
                        UtilK.handleCommonError(this@CommentOnlyActivity, response)

                        completion?.invoke(response)
                    }
                },
                errorListener = { throwable ->
                    Toast.makeText(
                        this@CommentOnlyActivity,
                        R.string.error_abnormal_exception, Toast.LENGTH_SHORT
                    ).show()
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }

                    commentOnlyAdapter.isLoading = false
                    commentOnlyAdapter.loadFailed = true
                    commentOnlyAdapter.notifyItemChanged(commentOnlyAdapter.itemCount - 1)
                })
        }
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

    private fun cropArticlePhoto(uri: Uri, isSharedImage: Boolean) {
        ImageUtil.cropArticlePhoto(this, uri, isSharedImage, false, ConfigModel.getInstance(this).articleMaxSize * 1024 * 1024,
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

            }, {
            }, { options ->
                originSrcUri = uri
                originSrcWidth = options.outWidth
                originSrcHeight = options.outHeight

            })
    }

    private fun onArticlePhotoSelected(uri: Uri) {
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
            val scaledBitmap = Bitmap.createBitmap(sampledSrcBitmap!!, 0, 0, sampledSrcBitmap.width, sampledSrcBitmap.height, matrix, true)

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

    private fun writeComments(emoticonId:Int, comment: String, binImage: ByteArray?) {

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
            Util.hideSoftKeyboard(this@CommentOnlyActivity, binding.viewComment.inputComment)
            Util.showDefaultIdolDialogWithBtn1(this, null, String.format(
                getString(R.string.comment_minimum_characters),
                Const.MINIMUM_COMMENT_LENGTH
            )) {
                Util.closeIdolDialog()
            }
            return
        }

        val commentFiltered = Util.BadWordsFilterToHeart(this, tmp)

        Util.showProgress(this)

        //댓글 write api 부름
        UtilK.commentCheckHash(this, heartPickId, binImage, lifecycleScope, commentsRepository){ cdnImageUrl ->
            callWriteCommentApi(emoticonId, commentFiltered, comment, cdnImageUrl)
        }
    }

    private fun callWriteCommentApi(emoticonId: Int,
                                    commentFiltered: String,
                                    comment: String,
                                    cdnImageUrl: String?,
    ){
        lifecycleScope.launch {
            heartpickRepository.postReplyMultipart(
                heartPickId = heartPickId,
                emoticonId = emoticonId,
                content = commentFiltered,
                imageUrl = cdnImageUrl,
                image = binImage,
                listener = { response ->
                    if (response.optBoolean("success")) {

                        //업로드 되었으니,  text뷰 비어줌.
                        binding.viewComment.inputComment.setText("")

                        //키보드 숨기기
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(binding.viewComment.inputComment.windowToken, 0)

                        binding.rlEmoticon.visibility = View.GONE

                        // 댓글 쓰고 UI에  바로 추가  혹시  exception 나오면  서버에서  받아옴.
                        try {

                            //새로 추가될  댓글
                            val newComment = CommentModel()
                            newComment.createdAt = Date()
                            newComment.content = comment

                            //이모티콘 댓글을 보냈을때는  새 댓글에 이모티콘 아이디를 적용해서
                            //이모티콘이 나오도록 한다.
                            if(emoticonId != CommentModel.NO_EMOTICON_ID){
                                newComment.emoticonId = emoticonId
                                newComment.contentAlt = contentAlt(emoticonId,true,commentFiltered)
                            }

                            if(isExistImage){
                                newComment.contentAlt = contentAlt(emoticonId, false, commentFiltered, localImageUri.toString(), true)
                            }
                            else if(isExistUmjjal){
                                newComment.contentAlt = contentAlt(emoticonId, false, commentFiltered, "", false, localGif.toString(), true)
                            }

                            newComment.resourceUri = response.optString("resource_uri")
                            newComment.thumbnailUrl = response.optString("thumbnail_url")
                            newComment.imageUrl = response.optString("image_url")
                            newComment.umjjalUrl = response.optString("umjjal_url")
                            newComment.user = IdolAccount.getAccount(this@CommentOnlyActivity)?.userModel

                            newComment.article = ArticleModel().apply {
                                imageUrl = response.optString("image_url")
                                thumbnailUrl = response.optString("thumbnail_url")
                                umjjalUrl = response.optString("umjjal_url")
                            }

                            Util.log(response.toString())
                            commentModelList.add(0, newComment)

                            // supportListModel.like = like // 서포트에서 이게 왜 여기 있어야하는지 의문임..?

                            //리사이클러뷰 업데이트
                            commentOnlyAdapter.getCommentList(
                                commentModelList,
                                cursor != null
                            )

                            //이모티콘 댓글을 보낸후에는  이모티콘 미리보기  화면 gone처리 해주고,
                            //이모티콘 id도 리셋시켜준다.
                            if(binding.clPreview.visibility == View.VISIBLE){
                                binding.clPreview.visibility = View.GONE
                                selectedEmoticonId = CommentModel.NO_EMOTICON_ID
                            }

                            //프로그래스바  close
                            Util.closeProgress()
                            val intent = Intent()
                            intent.putExtra("heartPickId", heartPickId)
                            intent.putExtra(Const.COMMENT_COUNT, commentOnlyAdapter.itemCount)
                            setResult(ResultCode.COMMENTED.value, intent)
                        } catch (e: Exception) {
                            // 댓글 쓰고 미러링 반영되게 1초 후 로딩
                            Handler().postDelayed({
                                loadComments()
                                Util.closeProgress()
                            }, 1000)
                        }
                        isScrollEnd =true

                    } else {//댓글 업로드 실패시

                        Util.closeProgress()
                        val gcode = response.optInt("gcode")
                        if (gcode == 2000 || gcode == ErrorControl.ERROR_1111
                        ) {
                            val responseMsg = response.optString("msg")
                            Toast.makeText(
                                this@CommentOnlyActivity, responseMsg,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@postReplyMultipart
                        }
                        val responseMsg = ErrorControl.parseError(
                            this@CommentOnlyActivity, response
                        )
                        Toast.makeText(
                            this@CommentOnlyActivity, responseMsg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binImage = null //전역변수라 write한 후에는 초기화
                    isExistImage = false  //write 한 후 이미지 존재 여부 false로 변경
                    isExistUmjjal = false //write 한 후 움짤 존재 여부 false로 변경
                },
                errorListener = { throwable ->
                    Toast.makeText(
                        this@CommentOnlyActivity,
                        R.string.error_abnormal_exception, Toast.LENGTH_SHORT
                    ).show()
                    Util.closeProgress()
                }
            )
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCode.ARTICLE_COMMENT_REMOVE.value) {
            if (resultCode == ResultCode.COMMENT_REMOVED.value) {
                val intent = Intent()
                intent.putExtra("heartPickId", heartPickId)
                setResult(ResultCode.COMMENT_REMOVED.value, intent)
                for (i in 0 until commentModelList.size) {

                    //respurce uri 같은걸로 지워진걸 찾아서  댓글 리스트에서 지워준다.
                    if (commentModelList[i].resourceUri == data?.getStringExtra("resource_uri")) {

                        commentModelList.remove(commentModelList[i])

                        intent.putExtra(Const.COMMENT_COUNT, commentModelList.size)

                        //댓글 리스트  업데이트
                        commentOnlyAdapter.getCommentList(
                            commentModelList,
                            cursor != null
                        )
                        break
                    }
                }
            }
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
                                this@CommentOnlyActivity,
                                Const.PREF_FRIENDS_LIMIT,
                                array.length() >= Const.THE_NUMBER_OF_FRIENDS_LIMIT
                            )
                            try {
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    val model = gson.fromJson(
                                        obj.toString(),
                                        FriendModel::class.java
                                    )
                                    //친구인 유저만 보여주기
                                    if (model.isFriend == "Y") {
                                        friends.add(model)
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
                        UtilK.handleCommonError(this@CommentOnlyActivity, response)
                    }
                },
                { throwable ->
                    Toast.makeText(
                        this@CommentOnlyActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()

                }
            )
        }
    }

    //친구 리스트 보여주는  recyclerview setting
    private fun setFriendsRecyclerView(recyclerView: RecyclerView){
        friendsAdapter =  FriendsAdapter(
            this, mGlideRequestManager, switchList, false,
            this
        )

        recyclerView.adapter = friendsAdapter
        removeList.clear()
        switchList.addAll(friendList)
    }

    private fun inputCommentChangeListen(editText: EditText){

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
        binding.rvCommentOnly.visibility = if (show) View.GONE else View.VISIBLE
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

    override fun onItemClicked(item: UserModel, view: View, position: Int) {
        when (view.id) {
            R.id.section2, R.id.userInfo, R.id.btnSendHeart, R.id.picture -> {
                val name = item.nickname
                val mentionText = "@{" + item.id + ":" + name + "}"

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
                binding.rvCommentOnly.visibility = View.VISIBLE
                binding.friendsRcyView.visibility = View.INVISIBLE
            }
        }
    }

    override fun translateComment(item: CommentModel, position: Int) {
        setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "comment_translate") // 임시값 (확정 후 변경 예정)

        commentTranslationHelper.clickTranslate(
            item = item,
            adapter = commentOnlyAdapter,
            position = position
        )
    }

    companion object {
        const val HEART_PICK_ID = "id"

        fun createIntent(context: Context, id: Int): Intent {
            val intent = Intent(context, CommentOnlyActivity::class.java)
            intent.putExtra(HEART_PICK_ID, id)
            return intent
        }
    }
}