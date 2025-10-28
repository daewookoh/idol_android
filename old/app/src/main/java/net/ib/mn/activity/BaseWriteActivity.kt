/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 문의하기, 커뮤니티, 자유게시판, 지식돌, 잡담 Parent Activity.
 * link관련하여 문의하기에선 필요없으나 향후 미래를 위해 Base에 넣어둠.
 *
 * */

package net.ib.mn.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Base64
import android.view.View
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.adapter.WriteMultiImgAdapter
import net.ib.mn.core.data.dto.UploadFileDTO
import net.ib.mn.core.data.repository.FilesRepository
import net.ib.mn.core.model.TagModel
import net.ib.mn.databinding.ActivityWriteArticleBinding
import net.ib.mn.fragment.WriteArticleBottomSheetFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.LinkDataModel
import net.ib.mn.model.PresignedRequestModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.Util
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.security.MessageDigest
import javax.inject.Inject
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import net.ib.mn.utils.ext.updatePadding

@AndroidEntryPoint
open class BaseWriteActivity : BaseActivity(), WriteMultiImgAdapter.OnItemClickListener {
    protected lateinit var binding: ActivityWriteArticleBinding

    @Inject
    lateinit var filesRepository: FilesRepository

    protected lateinit var mGlideRequestManager: RequestManager
    protected lateinit var writeMultiImgAdapter: WriteMultiImgAdapter

    protected var mItemsUriArray: ArrayList<Uri> = ArrayList()
    protected var savedFileNameList: ArrayList<String?> = ArrayList() // 미리 보여주는 img/video 삭제 시 사용하는 List
//    protected var jArray: JSONArray = JSONArray() // 등록할 때 파일들의 사이즈, 이름 보내는 List
    protected var filesArray: ArrayList<UploadFileDTO> = ArrayList() // 등록할 때 파일들의 사이즈, 이름 보내는 List
    // 사진 비율
    protected var useSquareImage = true

    // 사진 원본
    protected var originSrcUri: Uri? = null
    protected var originSrcWidth = 0
    protected var originSrcHeight = 0

    protected var binImage: ByteArray? = null

    // 자게, 지식돌, 커뮤, 문의하기, 잡담 type
    protected var type: Int = 0

    // 자유게시판 태그 model
    protected var selectionTag: TagModel? = null
    // 문의하기 문의유형 type
    protected var inquiryType: String? = null

    // html 파싱 찻 시도시 데이터를 제대로 못 가져오는 경우가 있음 그래서 이를 방지하기 위해 사용
    private var isFirstParsingUrl = true

    private val linkHandler: LinkHandler = LinkHandler(this)
    private var htmlParsingThread: Thread? = null
    protected var linkData: LinkDataModel? = null
    protected var rawLinkImage: String? = ""

    protected var fileCount: Int = 0

    protected var account: IdolAccount? = null
    var articleModel: ArticleModel? = null

    var isEditing = false

    // 사진, 동영상 선택 시 PreSignedUrl Api 요청할 data들 모아두는 ArrayList
    protected var presignedRequestModelList : ArrayList<PresignedRequestModel> = ArrayList()

    private lateinit var writeArticleBottomSheetFragment: WriteArticleBottomSheetFragment
    protected var mIdol: IdolModel? = null
    protected var isShowPrivate = false // 전체공개일지 최애공개일지 체크

    private var cachedSafeBottom = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        init()
        setupEdgeToEdgeForWrite()
    }

    private fun setupEdgeToEdgeForWrite() {
        val root   = binding.clContainer
        val title  = binding.clTitle
        val footer = binding.clSetting
        val body   = binding.llContent

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 본문을 푸터 위에 제약으로 고정(혹시 XML에서 안 묶여있다면 강제로 묶음)
        body.updateLayoutParams<ConstraintLayout.LayoutParams> {
            // 상단/좌우 제약은 기존 그대로 쓰고, 하단만 푸터 top에 연결
            bottomToTop = footer.id
        }
        // 본문 하단 패딩은 관리하지 않도록 0으로
        if (body.paddingBottom != 0) body.updatePadding(bottom = 0)

        ViewCompat.setWindowInsetsAnimationCallback(
            root,
            object : WindowInsetsAnimationCompat.Callback(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
            ) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    running: List<WindowInsetsAnimationCompat>
                ) = insets

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    root.post { ViewCompat.requestApplyInsets(root) }
                }
            }
        )

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val ime    = insets.getInsets(WindowInsetsCompat.Type.ime())

            // 하단 "안전 영역"을 안정적으로 캐시(가시성 무관)
            val nav  = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom
            val cut  = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.displayCutout()).bottom
            val gest = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemGestures()).bottom
            val tapp = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.tappableElement()).bottom
            val safeBottomNow = maxOf(nav, cut, gest, tapp)

            if (cachedSafeBottom == 0) {
                cachedSafeBottom = safeBottomNow.coerceAtLeast((root.resources.displayMetrics.density * 16).toInt())
            } else if (safeBottomNow > 0) {
                cachedSafeBottom = safeBottomNow
            }
            val safeBottom = cachedSafeBottom

            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            // 1) 상단 상태바 패딩은 타이틀에만
            title.updatePadding(top = status.top)

            // 2) 푸터는 항상 하단 안전영역/IME 위로 띄운다(마진만 조절)
            val footerBottom = if (imeVisible) maxOf(ime.bottom, safeBottom) else safeBottom
            footer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                if (bottomMargin != footerBottom) bottomMargin = footerBottom
            }

            // 3) 본문은 패딩/마진 건드리지 않음 (제약으로 붙어있으니 ‘흰 띠’가 생길 여지 없음)

            insets
        }

        if (root.isAttachedToWindow) ViewCompat.requestApplyInsets(root)
        else root.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    private fun init() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_write_article)

        account = IdolAccount.getAccount(this)

        articleModel = intent.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel?

        // 글 작성 완료하고 방치시 화면이 꺼지면서 다이얼로그가 닫히기때문에 이를 방지
        FLAG_CLOSE_DIALOG = false

        mGlideRequestManager = Glide.with(this)

        binding.rvPhoto.layoutManager =
            LinearLayoutManagerWrapper(this, LinearLayoutManager.HORIZONTAL, false)
        writeMultiImgAdapter = WriteMultiImgAdapter(this, articleModel != null,  mGlideRequestManager, mItemsUriArray, this)
        writeMultiImgAdapter.setHasStableIds(true)
        binding.rvPhoto.adapter = writeMultiImgAdapter
    }

    override fun onDestroy() {
        // 임시파일이 남아있는 경우 삭제 시도.
        if (mTempFileForCrop != null) {
            mTempFileForCrop?.deleteOnExit()
        }
        htmlParsingThread?.interrupt()
        htmlParsingThread = null
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        try {
            if(isEditing) {
                super.onBackPressed()
                return
            }

            Util.hideSoftKeyboard(this, binding.etContent) // 키보드 미리 안 닫았을 경우 팝업 크게 나오는 현상 방지

            if (!binding.etContent.text.isNullOrEmpty() || binding.clPhoto.visibility == View.VISIBLE) {
                Util.showDefaultIdolDialogWithBtn2(
                    this,
                    getString(R.string.article_cancel_title),
                    getString(
                        R.string.article_cancel_msg,
                    ),
                    {
                        Util.closeIdolDialog()
                        super.onBackPressed()
                    },
                    {
                        Util.closeIdolDialog()
                    },
                )
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDeletedClickListener(position: Int) {
        if(type == TYPE_INQUIRY) {    // 문의하기에서만 사용
            lifecycleScope.launch {
                savedFileNameList[position]?.let {
                    filesRepository.deleteUploaded(Const.NCLOUD_INQUIRY_BUCKET, it, {}, {})
                }
                savedFileNameList.removeAt(position)
            }
        } else {
            presignedRequestModelList.removeAt(position)
        }

        mItemsUriArray.removeAt(position)
        writeMultiImgAdapter.setItemsByteArray(mItemsUriArray, true)
        setFileStatus(fileCount, false)
        // 문의하기에서만 filesArray를 사용
        if(filesArray.size > position) {
            filesArray.removeAt(position) // 등록버튼 눌렀을 때, 삭제된 파일도 보내는 문제 처리
        }

        if (mItemsUriArray.isEmpty()) {
            binding.clPhoto.visibility = View.GONE
        }
    }

    // 글 내용에 url이 포함되어 있다면, 이미지 첨부를 못하게 하기 위해 막음.
    protected fun setFileStatus(fileCount: Int, containedUrl: Boolean) {
        if(containedUrl) {
            binding.btnPhoto.isEnabled = false
            binding.btnVideo.isEnabled = false
            return
        }

        if (type == TYPE_INQUIRY) {
            enableGalleryBtnForInquiry()
            return
        }

        enableGalleryBtn(
            mimeType = WriteMultiImgAdapter.MIME_TYPE_IMAGE,
            isPreviewEmpty = mItemsUriArray.isEmpty()
        )
    }

    // This Handler class should be static or leaks might occur 처리
    private class LinkHandler(activity: BaseWriteActivity) : Handler() {
        private val mActivity: WeakReference<BaseWriteActivity> = WeakReference(activity)
        override fun handleMessage(msg: Message) {
            val activity = mActivity.get()
            activity?.handleLinkMessage(msg)
        }
    }

    private fun handleLinkMessage(msg: Message) {
        setFileStatus(fileCount, containedUrl = true)
        linkData = msg.obj as LinkDataModel
        with(binding) {
            flPreviewInfo.visibility = View.VISIBLE
            tvPreviewDescription.visibility = View.VISIBLE
            tvPreviewTitle.text = linkData?.title
        }
        if (linkData?.description == null || linkData?.description?.trim() == "") {
            binding.tvPreviewDescription.visibility = View.GONE
            binding.tvPreviewTitle.maxLines = 2
        } else {
            binding.tvPreviewDescription.text = linkData?.description
        }
        binding.tvPreviewHost.text = linkData?.host ?: linkData?.url
        val imageUrl = linkData?.imageUrl
        if (!imageUrl.isNullOrEmpty()) {
            binding.progressBar.visibility = View.VISIBLE
            mGlideRequestManager.asBitmap()
                .load(imageUrl)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean,
                    ): Boolean {
                        binding.progressBar.visibility = View.GONE
                        rawLinkImage = null // null로 설정해서 썸네일없이 그냥 올리게 처리
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap>,
                        dataSource: DataSource,
                        isFirstResource: Boolean,
                    ): Boolean {
                        runOnUiThread {
                            resizeLinkImage(resource)
                            binding.progressBar.visibility = View.GONE
                        }
                        return false
                    }
                })
                .into(binding.imgPreviewPhoto)
        }
    }

    private fun resizeLinkImage(originImage: Bitmap) {
        // Get the source image's dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var srcWidth = originImage.width
        var srcHeight = originImage.height
        var desiredWidth = Const.MAX_IMAGE_WIDTH
        // Only scale if the source is big enough. This code is just trying to fit a image into a certain width.
        if (desiredWidth > srcWidth) desiredWidth = srcWidth

        // Calculate the correct inSampleSize/scale value. This helps reduce memory use. It should be a power of 2
        // from: http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue/823966#823966
        var inSampleSize = 1
        while (srcWidth / 2 > desiredWidth) {
            srcWidth /= 2
            srcHeight /= 2
            inSampleSize *= 2
        }
        val desiredScale = desiredWidth.toFloat() / srcWidth

        options.apply {
            inJustDecodeBounds = false
            inDither = false
            this.inSampleSize = inSampleSize
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        // Decode with inSampleSize
        val stream = ByteArrayOutputStream()
        originImage.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bitmapData = stream.toByteArray()
        val sampledSrcBitmap =
            BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.size, options)

        // Resize
        val matrix = Matrix()
        matrix.postScale(desiredScale, desiredScale)
        val scaledBitmap = Bitmap.createBitmap(
            sampledSrcBitmap,
            0,
            0,
            sampledSrcBitmap.width,
            sampledSrcBitmap.height,
            matrix,
            true,
        )
        val resizeStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, resizeStream)
        binImage = resizeStream.toByteArray() // 링크 올릴 때에도 multipart로
        rawLinkImage = Base64.encodeToString(
            resizeStream.toByteArray(),
            Base64.DEFAULT,
        )

        val hashBytes = MessageDigest.getInstance("SHA-256").digest(binImage?: return)
        val hash = Util.bytesToHex(hashBytes)

        linkData?.apply {
            uriPath = Util.uriToFilePath(this@BaseWriteActivity, Uri.parse(this.imageUrl))
            this.srcWidth = srcWidth
            this.srcHeight = srcHeight
            this.hash = hash
        }
    }

    protected fun htmlParsingThread(containedUrl: String) {
        htmlParsingThread = Thread {
            try {
                Util.getURLtoText(this, containedUrl)?.let { linkData ->
                    if (linkData.imageUrl != null) {
                        if (linkData.url == null) {
                            linkData.url = containedUrl
                        }
                        // 메시지 얻어오기
                        val msg = linkHandler.obtainMessage()
                        // 메시지 ID 설정
                        msg.what = 0
                        msg.obj = linkData
                        linkHandler.sendMessage(msg)
                        return@let
                    }

                    if (isFirstParsingUrl) {
                        isFirstParsingUrl = false
                        htmlParsingThread(containedUrl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        htmlParsingThread?.start()

    }

    protected fun enableGalleryBtn(mimeType: Int, isPreviewEmpty: Boolean = false) = with(binding) {

        if (isPreviewEmpty) {
            btnPhoto.isEnabled = true
            btnVideo.isEnabled = true
            return@with
        }

        if(mimeType == WriteMultiImgAdapter.MIME_TYPE_VIDEO) {
            btnPhoto.isEnabled = false
            btnVideo.isEnabled = false
            return@with
        }

        if(mItemsUriArray.size != fileCount) {
            btnPhoto.isEnabled = true
            btnVideo.isEnabled = false
            return
        }
        btnPhoto.isEnabled = false
        btnVideo.isEnabled = false
    }

    private fun enableGalleryBtnForInquiry() = with(binding) {

        if (mItemsUriArray.size >= FaqWriteActivity.FILE_COUNT) {
            btnPhoto.isEnabled = false
            btnVideo.isEnabled = false
            return@with
        }

        btnPhoto.isEnabled = true
        btnVideo.isEnabled = true
    }

    // 설정 버튼 눌렀을 때 최애만 공개 checkBox 나오는 BottomSheet
    protected fun showBottomSheetDialogSettingOption() {
        writeArticleBottomSheetFragment = WriteArticleBottomSheetFragment.newInstance(
            WriteArticleBottomSheetFragment.FLAG_COMMUNITY,
            mIdol,
            isShowPrivate,
        ) { isPrivateCallback ->

            if (isShowPrivate) {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.POST_SETTING_ONLY_CM.actionValue,
                    GaAction.POST_SETTING_ONLY_CM.label
                )
            }
            isShowPrivate = isPrivateCallback
            val isMostOnly = if (isShowPrivate) Const.SHOW_PRIVATE else Const.SHOW_PUBLIC
            articleModel?.setIsMostOnly(isMostOnly)
        }
        val tag = "write_article_setting"
        val oldFrag = supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            writeArticleBottomSheetFragment.show(supportFragmentManager, tag)
        }
    }

    protected fun showSatisfiedConditionSnackBar(satisfiedAllCondition: (Boolean) -> Unit) =
        with(binding) {

            var snackBarString: String? = ""
            var isSatisfiedAllCondition = true

            // 본문 내용, 이미지 첨부 하나라도 없을때.
            when {
                etContent.text.isNullOrEmpty() && clPhoto.visibility != View.VISIBLE -> {
                    snackBarString = getString(R.string.enter_content)
                    isSatisfiedAllCondition = false
                }
                includeTagOption.root.isVisible && includeTagOption.etTitle.text.toString().trim().isEmpty()-> {
                    snackBarString = getString(R.string.enter_title)
                    isSatisfiedAllCondition = false
                }
                includeSmallTalk.root.isVisible && includeSmallTalk.etTitle.text.toString().trim().isEmpty() -> {
                    snackBarString = getString(R.string.enter_title)
                    isSatisfiedAllCondition = false
                }
            }

            when (type) {
                TYPE_INQUIRY -> {
                    if (inquiryType.isNullOrEmpty()) {
                        snackBarString = getString(R.string.choose_inquiry_category)
                        isSatisfiedAllCondition = false
                    }
                }

                TYPE_FREE_BOARD -> {
                    if (selectionTag == null) {
                        snackBarString = getString(R.string.select_category_toast)
                        isSatisfiedAllCondition = false
                    }
                }

                TYPE_SMALL_TALK -> {

                    if (includeSmallTalk.etTitle.text.isNullOrEmpty()) {
                        snackBarString = getString(R.string.enter_title)
                        isSatisfiedAllCondition = false
                    }
                }
            }

            if (!snackBarString.isNullOrEmpty()) {
                IdolSnackBar.make(findViewById(android.R.id.content), snackBarString).show()
            }

            satisfiedAllCondition(isSatisfiedAllCondition)
        }

    companion object {
        const val TYPE_SMALL_TALK = 0
        const val TYPE_COMMUNITY = 1
        const val TYPE_KIN = 2
        const val TYPE_FREE_BOARD = 3
        const val TYPE_INQUIRY = 4
    }
}