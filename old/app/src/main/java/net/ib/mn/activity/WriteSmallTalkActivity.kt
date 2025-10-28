/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import com.google.gson.reflect.TypeToken
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.PresignedUrlService
import net.ib.mn.R
import net.ib.mn.adapter.WriteMultiImgAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.model.TagModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.PresignedRequestModel
import net.ib.mn.model.WriteArticleModel
import net.ib.mn.utils.Const.IDOL_ID_FREEBOARD
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.HashTagUrlHelper
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.ImageUtil
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.MediaExtension
import net.ib.mn.utils.MediaStoreUtils
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.UploadSingleton
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.toByteArray
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.permission.Permission
import net.ib.mn.utils.permission.PermissionHelper
import net.ib.mn.viewmodel.SmallTalkWriteViewModel
import java.io.File
import java.security.MessageDigest
import java.util.*

@AndroidEntryPoint
class WriteSmallTalkActivity : BaseWriteActivity(), View.OnClickListener, WriteMultiImgAdapter.OnItemClickListener {

    private val smallTalkWriteViewModel: SmallTalkWriteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            val path = savedInstanceState.getString(PARAM_PATH_TEMP_FILE)
            path?.let { mTempFileForCrop = File(it) }
            originSrcWidth = savedInstanceState.getInt(PARAM_ORG_IMAGE_WIDTH)
            originSrcHeight = savedInstanceState.getInt(PARAM_ORG_IMAGE_HEIGHT)
            val uri = savedInstanceState.getString(PARAM_ORG_IMAGE_URI)
            uri?.let { originSrcUri = Uri.parse(it) }
        }

        init()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mTempFileForCrop != null) {
            outState.putString(
                PARAM_PATH_TEMP_FILE,
                mTempFileForCrop?.path,
            )
        }
        // activity가 유지되지 않았을 때 원본 이미지를 전송할 수 있게
        outState.putInt(PARAM_ORG_IMAGE_WIDTH, originSrcWidth)
        outState.putInt(PARAM_ORG_IMAGE_HEIGHT, originSrcHeight)
        if (originSrcUri != null) outState.putString(PARAM_ORG_IMAGE_URI, originSrcUri.toString())
    }

    private fun init() {
        smallTalkWriteViewModel.registerVideoPickerResult(this)

        // ViewModel
        getDataFromVM()

        fileCount = FILE_COUNT

        mIdol = (intent.getSerializableExtra(PARAM_IDOL) as? IdolModel)

        if (mIdol == null) {
            Util.showDefaultIdolDialogWithBtn1(
                this,
                null,
                getString(R.string.msg_error_ok),
            ) {
                Util.closeIdolDialog()
                setResult(ResultCode.ERROR.value)
                finish()
            }
        }
//
        with(binding) {
            includeSmallTalk.etTitle.hint = String.format(getString(R.string.title_char_limit_placeholder), TITLE_MAX_LENGTH)
            binding.btnSetting.visibility =if (mIdol?.getId() == account?.most?.getId()) View.VISIBLE else View.GONE
            includeSmallTalk.root.visibility = View.VISIBLE
            if (mIdol?.getId() == IDOL_ID_FREEBOARD) {
                val gson = IdolGson.getInstance()
                val listType = object : TypeToken<List<TagModel>>() {}.type
                val tags: MutableList<TagModel> = gson.fromJson(Util.getPreference(this@WriteSmallTalkActivity, Const.BOARD_TAGS), listType)
                val tt = intent.getIntExtra(PARAM_TAG_ID, -1)
                val tagName = tags.firstOrNull { it.id == intent.getIntExtra(PARAM_TAG_ID, -1) }
                if (tagName == null) {
                    includeSmallTalk.tvTag.text = mIdol?.getName(this@WriteSmallTalkActivity)
                } else {
                    includeSmallTalk.tvTag.text = tagName.name
                }
            } else {
                includeSmallTalk.tvTag.text = mIdol?.getName(this@WriteSmallTalkActivity)
            }
            btnWrite.setOnClickListener(this@WriteSmallTalkActivity)
            flPreviewInfo.setOnClickListener(this@WriteSmallTalkActivity)
            btnPreviewDel.setOnClickListener(this@WriteSmallTalkActivity)
            btnPhoto.setOnClickListener(this@WriteSmallTalkActivity)
            btnClose.setOnClickListener(this@WriteSmallTalkActivity)
            btnSetting.setOnClickListener(this@WriteSmallTalkActivity)
            btnVideo.setOnClickListener(this@WriteSmallTalkActivity)
        }

        // 작성/수정 시 보여야 할 라벨 정리
        showLabel()
        // 컨텐트 쓸 때
        writeContent()

        // 처음 파일 개수 set
        setFileStatus(FILE_COUNT, false)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btnVideo -> requestPermission(false)
            binding.btnWrite -> {
                Util.hideSoftKeyboard(this, binding.etContent)
                showSatisfiedConditionSnackBar { isSatisfiedAllCondition ->

                    if (!isSatisfiedAllCondition) {
                        return@showSatisfiedConditionSnackBar
                    }

                    if (isEditing) {
                        tryEdit()
                        return@showSatisfiedConditionSnackBar
                    }
                    tryUpload()
                }
            }
            binding.btnPreviewDel -> {
                rawLinkImage = ""
                linkData = null
                binding.flPreviewInfo.visibility = View.GONE
                setFileStatus(FILE_COUNT, false)
            }
            binding.btnClose -> onBackPressed()
            binding.btnPhoto -> requestPermission(true)
            binding.btnSetting -> showBottomSheetDialogSettingOption()
        }
    }

    private fun requestPermission(isPhoto: Boolean = true) {
        val permissions =
            Permission.getStoragePermissions(this)

        val msgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(getString(R.string.permission_storage), "")
        } else {
            arrayOf(getString(R.string.permission_storage))
        }

        PermissionHelper.requestPermissionIfNeeded(
            this,
            null,
            permissions,
            msgs,
            if(isPhoto) REQUEST_READ_MEDIA_IMAGES else REQUEST_READ_MEDIA_VIDEO,
            object : PermissionHelper.PermissionListener {
                override fun onPermissionAllowed() {
                    if (!isPhoto) {
                        Util.hideSoftKeyboard(this@WriteSmallTalkActivity, binding.etContent)
                        smallTalkWriteViewModel.exodusImagePickerRegiser.launchMediaPicker(
                            this@WriteSmallTalkActivity,
                            feature.common.exodusimagepicker.util.Const.videoPickerType,
                            smallTalkWriteViewModel.getVideoSpecModel(this@WriteSmallTalkActivity).maxSeconds,
                            LocaleUtil.getAppLocale(this@WriteSmallTalkActivity).language
                        )
                        return
                    }

                    onArticlePhotoClick(null, false)
                }

                override fun onPermissionDenied() {}
                override fun requestPermission(permissions: Array<String>) { }
            }
        )
    }

    private fun tryEdit() {
        synchronized(this) {
            if (!binding.btnWrite.isEnabled) {
                return
            }
            binding.btnWrite.isEnabled = false
        }

        // 올린 이미지가 없고 글도 없으면
        if (binding.clPhoto.visibility != View.VISIBLE && binding.etContent.text.toString().isEmpty()) {
            Util.showDefaultIdolDialogWithBtn1(
                this,
                null,
                getString(R.string.msg_no_data),
            ) { v: View? ->
                Util.closeIdolDialog()
                binding.btnWrite.isEnabled = true
            }
        } else {
            Util.showLottie(this, true)

            //타이틀 수정
            val title = if (binding.includeSmallTalk.etTitle.text != null) {
                Util.BadWordsFilterToHeart(
                    this,
                    binding.includeSmallTalk.etTitle.text.toString(),
                )
            } else {
                ""
            }
            articleModel?.title = title

            //내용 수정
            val content = if (binding.etContent.text != null) {
                Util.BadWordsFilterToHeart(
                    this,
                    binding.etContent.text.toString(),
                )
            } else {
                ""
            }
            articleModel?.content = content

            val elem = articleModel?.resourceUri?.split("/")
            var id = ""
            if (elem != null && elem.size > 2) {
                id = elem[elem.size - 2]
            }

            if (linkData != null) {
                articleModel?.linkTitle = linkData?.title
                articleModel?.linkDesc = linkData?.description
                articleModel?.linkUrl = linkData?.url
            }
            val show: String = if (isShowPrivate) {
                Const.SHOW_PRIVATE
            } else {
                Const.SHOW_PUBLIC
            }
            articleModel?.let {
                smallTalkWriteViewModel.updateArticles(
                    this,
                    id,
                    it,
                    show,
                    ""
                )
            }
        }
    }

    // ViewModel Data 받아오는 함수
    private fun getDataFromVM() {
        smallTalkWriteViewModel.getVideoFiles.observe(
            this,
            SingleEventObserver { videoFiles ->

                if (videoFiles.isEmpty()) {
                    return@SingleEventObserver
                }

                val videoImageFile = File(
                    cacheDir,
                    videoFiles[0].thumbnailImage?.lastPathSegment ?: return@SingleEventObserver
                )

                val videoImageByte = videoImageFile.toByteArray() ?: return@SingleEventObserver

                binImage = videoImageByte

                mItemsUriArray.clear()
                setPreviewItems(
                    Uri.fromFile(videoImageFile),
                    WriteMultiImgAdapter.MIME_TYPE_VIDEO
                )

                val presignedRequestModel = PresignedRequestModel(
                    bucket = Const.NCLOUD_ARTICLES_BUCKET,
                    uriPath = Uri.parse(videoFiles[0].relativePath).lastPathSegment,
                    fileType = "mv",
                    byteArray = videoImageByte,
                    mimeType = MediaExtension.MP4.value,
                    videoFile = videoFiles[0]
                )
                presignedRequestModelList.add(presignedRequestModel)
            }
        )
        // 게시글 작성버튼 클릭했을 때(수정 시)
        smallTalkWriteViewModel.updateResponse.observe(
            this,
            SingleEventObserver { response ->
                val gcode = response["gcode"] as Int?
                var msg = ""
                if (gcode == ARTICLES_OK) {
                    msg = getString(R.string.response_v1_articles_ok)
                } else if (gcode == ARTICLES_OK_HEART_PROVIDE) {
                    val heart = response["heart"] as Int?
                    msg = String.format(
                        getString(R.string.response_v1_articles_ok_heart),
                        heart.toString(),
                    )
                }
                showUploadSuccess(msg, ResultCode.EDITED.value)

                if (gcode == RESPONSE_ARTICLES_2000) {
                    binding.btnWrite.isEnabled = true
                    setResult(ResultCode.EDITED.value)
                    finish()
                    return@SingleEventObserver
                }
            },
        )

        //에러 팝업.
        smallTalkWriteViewModel.errorPopup.observe(
            this,
            SingleEventObserver { message ->
                binding.btnWrite.isEnabled = true
                IdolSnackBar.make(findViewById(android.R.id.content), message).show()
            },
        )
    }

    // 업로드 성공했을 때 나오는 팝업
    private fun showUploadSuccess(msg: String, resultCode: Int) {
        Util.showDefaultIdolDialogWithBtn1(
            this,
            null,
            msg,
        ) { v: View? ->
            Util.closeIdolDialog()
            val data = Intent()
            if (articleModel != null) { // 게시한 경우 커뮤 업데이트 하기 위함
                data.putExtra("resource_uri", articleModel?.resourceUri)
                if (isEditing) { // 수정중일 경우
                    data.putExtra(Const.EXTRA_ARTICLE, articleModel)
                }
                else {
                    setUiActionFirebaseGoogleAnalyticsActivity(GaAction.SMALL_TALK_POST.actionValue, GaAction.SMALL_TALK_POST.label)
                }
            }
            setResult(resultCode, data)
            finish()
        }
    }

    // 파일 첨부 클릭 시
    private fun onArticlePhotoClick(uri: Uri?, isSharedImage: Boolean) {
        if (uri != null) {
            cropArticlePhoto(uri, isSharedImage)
        } else {
            val photoPickIntent = MediaStoreUtils.getPickImageIntent(this)
            val packageManager = packageManager
            if (photoPickIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(
                    photoPickIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    PHOTO_SELECT_REQUEST,
                )
            } else {
                Util.showDefaultIdolDialogWithBtn1(
                    this,
                    null,
                    getString(R.string.cropper_not_found),
                ) { view: View? -> Util.closeIdolDialog() }
            }
        }
    }

    @SuppressLint("Recycle")
    @Throws(SecurityException::class)
    private fun onArticlePhotoSelected(uri: Uri) {

        ImageUtil.onArticlePhotoSelected(this, uri, originSrcWidth, originSrcHeight, originSrcUri,
            { scaledBitmap ->
            // setImageUri가 안되는 폰이 있음.
//            binding.ivPhoto.setImageBitmap(scaledBitmap)
            },
            { stream ->
                mItemsUriArray.add(uri)
                binding.clPhoto.visibility = View.VISIBLE
                binImage = stream.toByteArray()
                writeMultiImgAdapter.setItemsByteArray(mItemsUriArray, false)
                enableGalleryBtn(WriteMultiImgAdapter.MIME_TYPE_IMAGE, isPreviewEmpty = false)

                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = binImage?.let { digest.digest(it) }
                val hash = Util.bytesToHex(hashBytes)
                val uriPath = uri.lastPathSegment?.split(":")?.last()

                if (uriPath != null && binImage != null) {
                    // Get the source image's dimensions
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(uri.path, options)
                    val srcWidth = options.outWidth
                    val srcHeight = options.outHeight

                    val presignedRequestModel = PresignedRequestModel(Const.NCLOUD_ARTICLES_BUCKET, uriPath, srcWidth, srcHeight, hash, "st", binImage, "image/jpeg")
                    presignedRequestModelList.add(presignedRequestModel)
                }
            }
        )
    }

    private fun setPreviewItems(
        uri: Uri?,
        mimeType: Int = WriteMultiImgAdapter.MIME_TYPE_IMAGE
    ) {
        binding.clPhoto.visibility = View.VISIBLE
        mItemsUriArray.add(uri ?: return)
        writeMultiImgAdapter.setMimeType(mimeType)
        writeMultiImgAdapter.setItemsByteArray(mItemsUriArray, false)

        enableGalleryBtn(mimeType, isPreviewEmpty = false)

        // 이미지를 올렸을 때만 도움말풍선 보이도록
        if(mimeType == WriteMultiImgAdapter.MIME_TYPE_IMAGE && type == TYPE_COMMUNITY){
            showHelpProfileEnableUnderImg()
        } else {
            binding.tvHelpProfileEnableUnderImg.visibility = View.GONE
        }
    }

    private fun showHelpProfileEnableUnderImg() {
        // 아랍어일때 튜토리얼 텍스트 위치 변경
        val left = Util.convertDpToPixel(this, 12f).toInt()
        val top = Util.convertDpToPixel(this, 16f).toInt()
        val right = Util.convertDpToPixel(this, 25f).toInt()
        val bottom = Util.convertDpToPixel(this, 8f).toInt()
        if (Util.isRTL(this)) {
            binding.tvHelpProfileEnableUnderImg.setPadding(left, top, right, bottom)
        }
        if (Util.getPreferenceBool(this, Const.PREF_SHOW_PROFILE_ENABLE_UNDER_IMG, true)) {
            binding.tvHelpProfileEnableUnderImg.visibility = View.VISIBLE
            binding.tvHelpProfileEnableUnderImg.setOnClickListener { v: View? ->
                binding.tvHelpProfileEnableUnderImg.visibility = View.GONE
                Util.setPreference(applicationContext, Const.PREF_SHOW_PROFILE_ENABLE_UNDER_IMG, false)
            }
        } else {
            binding.tvHelpProfileEnableUnderImg.visibility = View.GONE
        }
    }

    private fun showLabel() {
        // 수정하는 경우
        if (articleModel != null) {
            isEditing = true
            binding.tvTitle.setText(R.string.title_edit)
            binding.includeSmallTalk.etTitle.setText(articleModel?.title)
            binding.etContent.setText(articleModel?.content)
            binding.btnPhoto.visibility = View.GONE
            binding.btnVideo.visibility = View.GONE

            if (articleModel?.isMostOnly == "Y") {
                isShowPrivate = true
            }

            if(articleModel?.files.isNullOrEmpty()) {
                if(!articleModel?.thumbnailUrl.isNullOrEmpty()) {
                    mItemsUriArray.add(Uri.parse(articleModel?.thumbnailUrl))
                }
            } else {
                for(i in 0 until articleModel!!.files.size){
                    mItemsUriArray.add(Uri.parse(articleModel!!.files[i].thumbnailUrl))
                }
            }
            if(mItemsUriArray.size != 0) {
                binding.clPhoto.visibility = View.VISIBLE
            }
        }
    }

    // 게시글 내용 작성
    private fun writeContent() {
        //게시글 작성 시 해시태그 컬러 적용
        val hashTagHelper = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            HashTagUrlHelper.Creator.create(resources.getColor(R.color.text_light_blue, null))
        } else{
            HashTagUrlHelper.Creator.create(resources.getColor(R.color.text_light_blue))
        }
        hashTagHelper.handle(binding.etContent)

        // 글자수 제한
        val filterContentArray = arrayOf<InputFilter>(InputFilter.LengthFilter(Const.MAX_ARTICLE_LENGTH))
        binding.etContent.filters = filterContentArray
        val filterTitleArray = arrayOf<InputFilter>(InputFilter.LengthFilter(Const.MAX_ARTICLE_TITLE_LENGTH))
        binding.includeSmallTalk.etTitle.filters = filterTitleArray

        binding.includeSmallTalk.etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun afterTextChanged(p0: Editable?) {
            }
        })

        // 유튜브, 트위터, 브이앱 등등 텍스트 링크 전달시
        intent?.getStringExtra(PARAM_TEXT)?.let { intentText ->
            binding.etContent.setText(intentText)

            if (!(binding.flPreviewInfo.visibility == View.GONE && mItemsUriArray.size == 0)) {
                return
            }
            val containedUrl = Util.checkUrls(intentText)
            if (containedUrl.isEmpty()) {
                return
            }
            Util.log("afterTextChanged containedUrl $containedUrl")
            htmlParsingThread(containedUrl)
        }

        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {

                val text = binding.etContent.text.toString()
                if (binding.flPreviewInfo.visibility == View.GONE && mItemsUriArray.size == 0) {
                    val containedUrl = Util.checkUrls(text)
                    if(containedUrl.isNotEmpty()){
                        Util.log("afterTextChanged containedUrl $containedUrl")
                        htmlParsingThread(containedUrl)
                        setFileStatus(FILE_COUNT, true)
                    }else{
                        setFileStatus(FILE_COUNT,false)
                    }
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) {
            return
        }

        when (requestCode) {
            PHOTO_SELECT_REQUEST -> {
                data?.data?.let { cropArticlePhoto(it, false) }
            }
            PHOTO_CROP_REQUEST -> {
                if (mTempFileForCrop != null) {
                    try {
                        onArticlePhotoSelected(Uri.fromFile(mTempFileForCrop))
                    } catch (e: SecurityException) {
                        Toast.makeText(
                            this,
                            R.string.image_permission_error,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    mTempFileForCrop?.deleteOnExit()
                }
            }
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                val resultUri = result.uri
                try {
                    onArticlePhotoSelected(resultUri)
                } catch (e: SecurityException) {
                    Toast.makeText(
                        this,
                        R.string.image_permission_error,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private fun cropArticlePhoto(uri: Uri, isSharedImage: Boolean) {
        ImageUtil.cropArticlePhoto(this, uri, isSharedImage, false, ConfigModel.getInstance(this).articleMaxSize * 1024 * 1024,
            { fileData ->
                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(fileData)
                val hash = Util.bytesToHex(hashBytes)

                val ims = contentResolver.openInputStream(uri)
                val bitmap: Bitmap = BitmapFactory.decodeStream(ims)
                val width: Int = bitmap.width
                val height: Int = bitmap.height

                mItemsUriArray.add(uri)
                binding.clPhoto.visibility = View.VISIBLE

                // GIF!
                binImage = fileData
                writeMultiImgAdapter.setItemsByteArray(mItemsUriArray, false)
                enableGalleryBtn(WriteMultiImgAdapter.MIME_TYPE_IMAGE, isPreviewEmpty = false)

                val presignedRequestModel = PresignedRequestModel(Const.NCLOUD_ARTICLES_BUCKET, UtilK.setUriPath(this@WriteSmallTalkActivity, uri), width, height, hash, "mv", fileData, "image/gif")
                presignedRequestModelList.add(presignedRequestModel)
            }, {

            }, { options ->
                originSrcUri = uri
                originSrcWidth = options.outWidth
                originSrcHeight = options.outHeight
            })
    }

    private fun tryUpload() {
        binding.btnWrite.isEnabled = false
        mIdol?.let {
            val filteredTitle =
                if (binding.includeSmallTalk.etTitle.text != null) {
                    Util.BadWordsFilterToHeart(
                        this,
                        binding.includeSmallTalk.etTitle.text.toString(),
                    )
                } else {
                    ""
                }

            val filteredContent =
                if (binding.etContent.text != null) {
                    Util.BadWordsFilterToHeart(
                        this,
                        binding.etContent.text.toString(),
                    )
                } else {
                    ""
                }

            if (!linkData?.uriPath.isNullOrEmpty()) {
                val presignedRequestModel = PresignedRequestModel(
                    Const.NCLOUD_ARTICLES_BUCKET,
                    linkData?.uriPath,
                    linkData?.srcWidth ?: 0,
                    linkData?.srcHeight ?: 0,
                    linkData?.hash,
                    "st",
                    binImage,
                    "image/jpeg"
                )
                presignedRequestModelList.add(presignedRequestModel)
            }

            val writeArticleModel = WriteArticleModel(
                filteredTitle,
                filteredContent,
                it.getId(),
                linkData?.title,
                linkData?.description,
                linkData?.url,
                if (isShowPrivate) Const.SHOW_PRIVATE else Const.SHOW_PUBLIC,
                tagId = null,
                mIdol
            )

            UploadSingleton.getInstance(
                presignedRequestModelList,
                writeArticleModel,
                type,
                LocaleUtil.getAppLocale(this@WriteSmallTalkActivity).language
            )   //intent로 데이터넘기면 The Intent extras size limit error 나서 SingleTon으로 저장
            startService(Intent(applicationContext, PresignedUrlService::class.java))
            finish()
        }
    }

    companion object {
        private const val ARTICLES_OK = 0
        private const val ARTICLES_OK_HEART_PROVIDE = 1
        private const val RESPONSE_ARTICLES_2000 = 2000
        private const val TITLE_MAX_LENGTH = "30"

        const val PARAM_IDOL = "idol"
        const val PARAM_TEXT = "text"
        const val PARAM_TAG_ID = "paramTagId"
        const val PARAM_PATH_TEMP_FILE = "tempFilePath"
        const val PARAM_ORG_IMAGE_WIDTH = "orgImageWidth"
        const val PARAM_ORG_IMAGE_HEIGHT = "orgImageHeight"
        const val PARAM_ORG_IMAGE_URI = "orgImageUri"
        const val FILE_COUNT = 5

        @JvmStatic
        fun createIntent(context: Context, model: IdolModel?, tagId: Int): Intent {
            val intent = Intent(context, WriteSmallTalkActivity::class.java)
            intent.putExtra(PARAM_IDOL, model as Parcelable?)
            intent.putExtra(PARAM_TAG_ID, tagId)
            return intent
        }

        @JvmStatic
        fun createIntent(context: Context, model: IdolModel?): Intent {
            val intent = Intent(context, WriteSmallTalkActivity::class.java)
            intent.putExtra(PARAM_IDOL, model as Parcelable?)
            return intent
        }
    }
}