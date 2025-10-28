/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 게시글 작성 Activity
 *
 * */

package net.ib.mn.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.android.volley.VolleyError
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.PresignedUrlService
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.FetchUserInfoListener
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.adapter.BottomSheetTagItemAdapter.TagItemListener
import net.ib.mn.adapter.WriteMultiImgAdapter
import net.ib.mn.core.model.HelpInfosModel
import net.ib.mn.core.model.TagModel
import net.ib.mn.feature.votingcertificate.VotingCertificateActivity.Companion.REQUEST_WRITE_STORAGE
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.fragment.BottomSheetFragment.Companion.newInstance
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.PresignedRequestModel
import net.ib.mn.model.WriteArticleModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.HashTagUrlHelper
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.ImageUtil
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.MediaExtension
import net.ib.mn.utils.MediaStoreUtils
import net.ib.mn.utils.permission.Permission
import net.ib.mn.utils.permission.PermissionHelper
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.UploadSingleton
import net.ib.mn.utils.Util
import net.ib.mn.utils.Util.Companion.closeProgress
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.toByteArray
import net.ib.mn.utils.getModelFromPref
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.SmallTalkWriteViewModel
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class WriteArticleActivity : BaseWriteActivity(), View.OnClickListener, TagItemListener {

    private lateinit var mBottomSheetFragment: BottomSheetFragment

    private val smallTalkWriteViewModel: SmallTalkWriteViewModel by viewModels()

    private lateinit var nameAdapter: ArrayAdapter<String>

    // 공유시 아이돌 선택
    private lateinit var idolDialog: Dialog

    private var modelList = ArrayList<IdolModel>()

    @Inject
    lateinit var accountManager: IdolAccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            val path = savedInstanceState.getString(PARAM_PATH_TEMP_FILE)
            path?.let { mTempFileForCrop = File(it) }
            useSquareImage = savedInstanceState.getBoolean(PARAM_IS_SQUARE_IMAGE)
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
        outState.putBoolean(PARAM_IS_SQUARE_IMAGE, useSquareImage)
        // activity가 유지되지 않았을 때 원본 이미지를 전송할 수 있게
        outState.putInt(PARAM_ORG_IMAGE_WIDTH, originSrcWidth)
        outState.putInt(PARAM_ORG_IMAGE_HEIGHT, originSrcHeight)
        if (originSrcUri != null) outState.putString(PARAM_ORG_IMAGE_URI, originSrcUri.toString())
    }

    override fun onResume() {
        if (IdolAccount.sAccount == null) {
            accountManager.fetchUserInfo(this)
        }
        super.onResume()
    }

    private fun init() {
        smallTalkWriteViewModel.registerVideoPickerResult(this)

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


        with(binding) {
            btnWrite.setOnClickListener(this@WriteArticleActivity)
            btnClose.setOnClickListener(this@WriteArticleActivity)
            btnPreviewDel.setOnClickListener(this@WriteArticleActivity)
            btnPhoto.setOnClickListener(this@WriteArticleActivity)
            btnSetting.setOnClickListener(this@WriteArticleActivity)
            includeSelectIdol.root.setOnClickListener(this@WriteArticleActivity)
            includeTagOption.root.setOnClickListener(this@WriteArticleActivity)
            btnVideo.setOnClickListener(this@WriteArticleActivity)
        }

        // 게시글 작성 시 해시태그 컬러 적용
        val hashTagHelper =
            HashTagUrlHelper.Creator.create(resources.getColor(R.color.text_light_blue, null))

        hashTagHelper.handle(binding.etContent)

        // 글자수 제한
        val filterArray = arrayOf<InputFilter>(InputFilter.LengthFilter(Const.MAX_ARTICLE_LENGTH))
        binding.etContent.filters = filterArray

        val filterTitleArray = arrayOf<InputFilter>(InputFilter.LengthFilter(Const.MAX_ARTICLE_TITLE_LENGTH))
        binding.includeTagOption.etTitle.apply {
            filters = filterTitleArray
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
                override fun afterTextChanged(p0: Editable?) {
                }
            })
        }

        setCommunityTypeAndView()
        linkOpenViewSetting()
        editOpenViewSetting()
        actionSendOpenViewSetting()
        editTextChangeListener()
        showHelpProfileEnableAboveBtn()
    }

    override fun onClick(v: View) {
        if(!smallTalkWriteViewModel.locale.language.equals(Util.getSystemLanguage(this))) {
            //nothing to do
        }
        when (v) {
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
                setFileStatus(FILE_COUNT,false)
            }
            binding.btnClose -> onBackPressed()
            binding.btnPhoto -> {
                requestPermission(isPhoto = true)

            }
            binding.btnVideo -> {
                requestPermission(isPhoto = false)

            }
            binding.btnSetting -> showBottomSheetDialogSettingOption()
            binding.includeSelectIdol.root -> showDialogSelectIdolOption()
            binding.includeTagOption.root -> showBottomSheetDialogSelectTag()
        }
    }

    private fun tryUpload() {
        if (!checkWriteData()) {
            Util.showDefaultIdolDialogWithBtn1(
                this,
                null,
                getString(R.string.msg_no_data),
            ) {
                Util.closeIdolDialog()
            }
        } else {
            val title = if (binding.includeTagOption.etTitle.text != null) {
                Util.BadWordsFilterToHeart(
                    this,
                    binding.includeTagOption.etTitle.text.toString(),
                )
            } else {
                ""
            }

            val content = if (binding.etContent.text != null) {
                Util.BadWordsFilterToHeart(
                    this,
                    binding.etContent.text.toString(),
                )
            } else {
                ""
            }
            val show = if (isShowPrivate) {
                Const.SHOW_PRIVATE
            } else {
                Const.SHOW_PUBLIC
            }

            // 파이어베이스 이벤트 추가
            when(mIdol?.getId()){
                Const.IDOL_ID_FREEBOARD -> {
                    setUiActionFirebaseGoogleAnalyticsActivity(GaAction.FREEBOARD_POST.actionValue, GaAction.FREEBOARD_POST.label)
                }
                else -> {
                    setUiActionFirebaseGoogleAnalyticsActivity(GaAction.COMMUNITY_POST.actionValue, GaAction.COMMUNITY_POST.label)
                }
            }

            if(!linkData?.uriPath.isNullOrEmpty()) {
                val presignedRequestModel = PresignedRequestModel(Const.NCLOUD_ARTICLES_BUCKET, linkData?.uriPath, linkData?.srcWidth?:0, linkData?.srcHeight?:0, linkData?.hash, "st", binImage, "image/jpeg")
                presignedRequestModelList.add(presignedRequestModel)
            }

            val tagId = if (selectionTag == null) {
                1
            } else if (selectionTag!!.id == Const.MY_FAVORITE_TAG_ID) {
                null
            } else {
                selectionTag!!.id
            }

            val writeArticleModel = if (tagId == null) {
                WriteArticleModel(title, content, account?.most?.getId() ?: return, linkData?.title, linkData?.description, linkData?.url, show, tagId, account?.most ?: return)
            } else {
                WriteArticleModel(title, content, mIdol!!.getId(), linkData?.title, linkData?.description, linkData?.url, show, tagId, mIdol)
            }

            val articleType = if (tagId == null) { 0 } else { type }

            UploadSingleton.getInstance(
                presignedRequestModelList,
                writeArticleModel,
                articleType,
                LocaleUtil.getAppLocale(this).language,
                intent.getIntExtra(Const.EXTRA_RETURN_TO, 0),
                selectionTag?.id
            )   //intent로 데이터넘기면 The Intent extras size limit error 나서 SingleTon으로 저장
            startService(Intent(applicationContext, PresignedUrlService::class.java))
            finish()
        }
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
            }
        } else {
            Util.showLottie(this, true)

            //타이틀 수정
            val titleText = binding.includeSmallTalk.etTitle.text?.toString()
            val tagTitleText = binding.includeTagOption.etTitle.text?.toString()

            val title = if (!titleText.isNullOrBlank()) {
                Util.BadWordsFilterToHeart(this, titleText)
            } else if (!tagTitleText.isNullOrBlank()) {
                Util.BadWordsFilterToHeart(this, tagTitleText)
            } else {
                ""
            }

            articleModel?.title = title

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
            val selectionTagId = if (selectionTag == null) "1" else selectionTag?.id.toString()
            articleModel?.let {
                smallTalkWriteViewModel.updateArticles(
                    this,
                    id,
                    it,
                    show,
                    selectionTagId,
                )
            }
        }
    }

    override fun onItemClick(tag: TagModel) {
        selectionTag = tag
        if (selectionTag?.id == Const.MY_FAVORITE_TAG_ID) {
            binding.btnSetting.visibility = View.VISIBLE
        } else {
            articleModel?.setIsMostOnly("public")
            isShowPrivate = false
            binding.btnSetting.visibility = View.GONE
        }

        binding.includeTagOption.tvTagOption.text = selectionTag?.name
        mBottomSheetFragment.dismiss()
    }

    // ViewModel Data 받아오는 함수
    private fun getDataFromVM() {
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

        // 즐겨찾기 아이돌 리스트 보여줄 때
        smallTalkWriteViewModel.modelList.observe(
            this,
            SingleEventObserver { modelList ->
                this.modelList = modelList
                for (idol in modelList) {
                    nameAdapter.add(idol.getName(this))
                }
            },
        )

        smallTalkWriteViewModel.selectionTag.observe(
            this,
            SingleEventObserver {
                selectionTag = it
                binding.includeTagOption.tvTagOption.text = selectionTag?.name
            },
        )

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
        // 에러 팝업.
        smallTalkWriteViewModel.errorPopup.observe(
            this,
            SingleEventObserver { message ->
                binding.btnWrite.isEnabled = true
                IdolSnackBar.make(findViewById(android.R.id.content), message).show()
            },
        )
    }

    private fun showUploadSuccess(msg: String, resultCode: Int) {
        Util.showDefaultIdolDialogWithBtn1(
            this,
            null,
            msg,
        ) { v: View? ->
            Util.closeIdolDialog()

            val data = Intent()
            if (articleModel != null) {
                data.putExtra("resource_uri", articleModel?.resourceUri)
                if (isEditing) {
                    data.putExtra(Const.EXTRA_ARTICLE, articleModel)
                }
            }
            setResult(resultCode, data)
            finish()
        }
    }

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
                    this@WriteArticleActivity,
                    null,
                    getString(R.string.cropper_not_found),
                ) { view: View? -> Util.closeIdolDialog() }
            }
        }
    }

    @SuppressLint("Recycle")
    @Throws(SecurityException::class)
    private fun onArticlePhotoSelected(uri: Uri) {
        ImageUtil.onArticlePhotoSelected(
            this,
            uri,
            originSrcWidth,
            originSrcHeight,
            originSrcUri,
            { scaledBitmap ->
                // setImageUri가 안되는 폰이 있음.
//                binding.ivPhoto.setImageBitmap(scaledBitmap)
            },
            { stream ->
                binImage = stream.toByteArray()
                setPreviewItems(uri)

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
            },
        )
    }


    // StartUpActivity를 통해 타고 들어왔을 때 처리
    private fun linkOpenViewSetting() {
        binding.includeSelectIdol.tvIdolLabel.text = getString(R.string.stats_idol).plus(" : ")
        // 유튜브, 트위터, 브이앱 등등 텍스트 링크 전달시
        intent?.getStringExtra(PARAM_TEXT)?.let { intentText ->
            binding.etContent.setText(intentText)
            binding.includeSelectIdol.root.visibility = View.VISIBLE
            binding.includeSelectIdol.tvIdolLabel.text =
                if (BuildConfig.CELEB) getString(R.string.actor) else getString(R.string.stats_idol)
            binding.includeSelectIdol.tvIdol.text = mIdol?.getName(this)
            if (binding.flPreviewInfo.visibility == View.GONE && mItemsUriArray.size == 0) {
                val containedUrl = Util.checkUrls(intentText)
                if (containedUrl.isEmpty()) {
                    return
                }
                htmlParsingThread(containedUrl)
            }
        }

        // 이미지 전달시
        if (intent.getStringExtra(PARAM_URI) != null) {
            binding.includeSelectIdol.root.visibility = View.VISIBLE
            binding.includeSelectIdol.tvIdol.text = mIdol!!.getName(this)
            val intentUri = Uri.parse(intent.getStringExtra(PARAM_URI))
            if (intentUri != null) onArticlePhotoClick(intentUri, true)
        }
    }

    // 게시글 수정하기로 열었을 때 세팅
    private fun editOpenViewSetting() {
        if (articleModel != null) {
            isEditing = true
            binding.tvTitle.setText(R.string.title_edit)
            binding.etContent.setText(articleModel?.content)
            binding.btnPhoto.visibility = View.GONE
            binding.btnVideo.visibility = View.GONE

            // 최애공개여부 설정
            if (articleModel?.isMostOnly == "Y") {
                isShowPrivate = true
            }

            if(articleModel?.files.isNullOrEmpty()) {
                if(!articleModel?.thumbnailUrl.isNullOrEmpty()) {
                    articleModel?.thumbnailUrl?.let { mItemsUriArray.add(Uri.parse(it)) }
                }
            } else {
                for(i in 0 until articleModel!!.files.size){
                    articleModel?.files?.get(i)?.thumbnailUrl?.let { mItemsUriArray.add(Uri.parse(it)) }
                }
            }
            if(mItemsUriArray.size != 0) {
                binding.clPhoto.visibility = View.VISIBLE
                val containedUrl = Util.checkUrls(articleModel?.content)
                if(containedUrl.isNotEmpty()) {
                    htmlParsingThread(containedUrl)
                }
            }

            val isVideo = articleModel?.files?.firstOrNull()?.originUrl?.endsWith(MediaExtension.MP4.value) ?: false

            if (!articleModel?.files.isNullOrEmpty() && isVideo) {
                writeMultiImgAdapter.setMimeType(WriteMultiImgAdapter.MIME_TYPE_VIDEO)
            }
        }
    }

    private fun actionSendOpenViewSetting() {
        // ACTION_SEND로 오는 경우 처리
        val receivedIntent = intent
        if (receivedIntent != null) {
            val receivedUri = receivedIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (receivedUri != null) {
                accountManager.fetchUserInfo(
                    this,
                    {
                        closeProgress()
                        if (!account!!.hasUserInfo()) {
                            Toast.makeText(
                                this@WriteArticleActivity,
                                getString(R.string.msg_error_ok),
                                Toast.LENGTH_SHORT,
                            ).show()
                            finish()
                        } else {
                            val i = intent
                            if (i == null || TextUtils.isEmpty(i.action)) {
                                return@fetchUserInfo
                            }
                            val receivedIntent = intent
                            val receivedUri =
                                receivedIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                            if (receivedUri != null) {
                                openImageEditor(receivedUri, false)
                            }
                        }
                    },
                    {
                        closeProgress()
                        Toast.makeText(
                            this@WriteArticleActivity,
                            getString(R.string.msg_error_ok),
                            Toast.LENGTH_SHORT,
                        ).show()
                        finish()
                    }
                )
            }
            return
        }
    }

    private fun setCommunityTypeAndView() {
        when(mIdol?.getId()) {
            Const.IDOL_ID_KIN -> {
                type = TYPE_KIN
            }
            Const.IDOL_ID_FREEBOARD -> {
                type = TYPE_FREE_BOARD
                binding.includeTagOption.root.visibility = View.VISIBLE

                if (articleModel?.title.isNullOrEmpty()) {
                    binding.includeTagOption.etTitle.hint = String.format(getString(R.string.title_char_limit_placeholder), 30)
                } else {
                    binding.includeTagOption.etTitle.setText(articleModel?.title)
                }

                val helpInfoModel = Util.getPreference(this, Const.PREF_HELP_INFO).getModelFromPref<HelpInfosModel>()
                binding.etContent.hint = helpInfoModel?.freeBoardPlaceHolder ?: ""

                smallTalkWriteViewModel.setTagSelection(this, articleModel)
            }
            else -> {
                type = TYPE_COMMUNITY
                binding.btnSetting.visibility =if(mIdol?.getId() == account?.most?.getId()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun editTextChangeListener() {
        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val text = binding.etContent.text.toString()
                if (binding.flPreviewInfo.visibility == View.GONE && mItemsUriArray.size == 0) {
                    val containedUrl = Util.checkUrls(text)
                    if(containedUrl.isNotEmpty()){
                        htmlParsingThread(containedUrl)
                        setFileStatus(FILE_COUNT,true)
                    } else {
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
                            this@WriteArticleActivity,
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
                        this@WriteArticleActivity,
                        R.string.image_permission_error,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
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

    private fun showHelpProfileEnableAboveBtn() {
        if(isEditing || type != TYPE_COMMUNITY) return
        // 아랍어일때 튜토리얼 텍스트 위치 변경
        val left = Util.convertDpToPixel(this, 12f).toInt()
        val top = Util.convertDpToPixel(this, 5f).toInt()
        val right = Util.convertDpToPixel(this, 25f).toInt()
        val bottom = Util.convertDpToPixel(this, 16f).toInt()
        if (Util.isRTL(this)) {
            binding.tvHelpProfileEnableAboveBtn.background = ContextCompat.getDrawable(this, R.drawable.bg_guide_end_bottom)
            binding.tvHelpProfileEnableAboveBtn.setPadding(left, top, right, bottom)
        }
        if (Util.getPreferenceBool(this, Const.PREF_SHOW_PROFILE_ENABLE_ABOVE_BTN, true)) {
            binding.tvHelpProfileEnableAboveBtn.visibility = View.VISIBLE
            binding.tvHelpProfileEnableAboveBtn.setOnClickListener { v: View? ->
                binding.tvHelpProfileEnableAboveBtn.visibility = View.GONE
                Util.setPreference(applicationContext, Const.PREF_SHOW_PROFILE_ENABLE_ABOVE_BTN, false)
            }
        } else {
            binding.tvHelpProfileEnableAboveBtn.visibility = View.GONE
        }
    }

    private fun checkWriteData(): Boolean {
        if (binImage != null) {
            return true
        }

        val isTitleValid = binding.includeTagOption.etTitle.isVisible &&
            !binding.includeTagOption.etTitle.text.isNullOrEmpty()

        val isContentValid = !binding.etContent.text.isNullOrEmpty()

        return isTitleValid || isContentValid
    }

    private fun showBottomSheetDialogSelectTag() {
        mBottomSheetFragment = newInstance(BottomSheetFragment.FLAG_BOARD_TAG)
        val tag = "filter_tag"
        val oldFrag = supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            mBottomSheetFragment.show(supportFragmentManager, tag)
        }
    }

    private fun showDialogImageOption() {
        if (type == TYPE_COMMUNITY || type == TYPE_KIN || type == TYPE_FREE_BOARD) {
            val sheet = newInstance(BottomSheetFragment.FLAG_PHOTO_RATIO)
            val tag = "filter_ratio"
            val oldFrag = supportFragmentManager.findFragmentByTag(tag)
            if (oldFrag == null) {
                sheet.show(supportFragmentManager, tag)
            }
        } else {
            useSquareImage = false
            onArticlePhotoClick(null, false)
        }
    }
    fun setRatioSquare() {
        useSquareImage = true
        if (binding.flPreviewInfo.visibility == View.VISIBLE) {
            Toast.makeText(this, R.string.msg_link_image_guide, Toast.LENGTH_SHORT).show()
        } else {
            onArticlePhotoClick(null, false)
        }
    }

    fun setRatioFree() {
        useSquareImage = false
        if (binding.flPreviewInfo.visibility == View.VISIBLE) {
            Toast.makeText(this, R.string.msg_link_image_guide, Toast.LENGTH_SHORT).show()
        } else {
            onArticlePhotoClick(null, false)
        }
    }

    private fun openImageEditor(uri: Uri, isSharedImage: Boolean) {
        ImageUtil.openImageEditor(this@WriteArticleActivity, uri, isSharedImage, useSquareImage) { options ->
            if (isSharedImage) {
                useSquareImage = false
            }
            originSrcUri = uri
            originSrcWidth = options.outWidth
            originSrcHeight = options.outHeight
        }
    }

    // 아이돌 즐겨찾기 다이얼로그
    private fun showDialogSelectIdolOption() {
        nameAdapter = ArrayAdapter<String>(this, R.layout.language_item)

        idolDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        if (idolDialog.window != null) {
            idolDialog.window?.attributes = lpWindow
            idolDialog.window?.setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        }
        idolDialog.setContentView(R.layout.dialog_idol)
        idolDialog.setCanceledOnTouchOutside(true)
        idolDialog.setCancelable(true)
        val listView = idolDialog.findViewById<ListView>(R.id.listView)
        if (BuildConfig.CELEB) {
            idolDialog.findViewById<TextView>(R.id.title).setText(R.string.schedule_category)
        }

        smallTalkWriteViewModel.getFavorite(this, account)
        listView.adapter = nameAdapter
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                if (!nameAdapter.isEmpty) {
                    binding.includeSelectIdol.tvIdol.text = nameAdapter.getItem(position).toString()
                    mIdol = modelList[position]
                    if (account?.most?.resourceUri != mIdol?.resourceUri) {
                        isShowPrivate = false
                    }
                    if (idolDialog.isShowing) {
                        try {
                            idolDialog.dismiss()
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        try {
            idolDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            idolDialog.show()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun cropArticlePhoto(uri: Uri, isSharedImage: Boolean) {
        ImageUtil.cropArticlePhoto(
            this,
            uri,
            isSharedImage,
            useSquareImage,
            ConfigModel.getInstance(this).articleMaxSize * 1024 * 1024,
            { fileData ->
                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(fileData)
                val hash = Util.bytesToHex(hashBytes)

                val ims = contentResolver.openInputStream(uri)
                val bitmap: Bitmap = BitmapFactory.decodeStream(ims)
                val width: Int = bitmap.width
                val height: Int = bitmap.height

                // GIF!
                binImage = fileData
                setPreviewItems(uri)

                val presignedRequestModel = PresignedRequestModel(Const.NCLOUD_ARTICLES_BUCKET, UtilK.setUriPath(this@WriteArticleActivity, uri), width, height, hash,"mv", fileData, "image/gif")
                presignedRequestModelList.add(presignedRequestModel)
            },
            {
            },
            { options ->
                if (isSharedImage) {
                    useSquareImage = false
                }
                originSrcUri = uri
                originSrcWidth = options.outWidth
                originSrcHeight = options.outHeight
            },
        )
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

                        smallTalkWriteViewModel.exodusImagePickerRegiser.launchMediaPicker(
                            this@WriteArticleActivity,
                            feature.common.exodusimagepicker.util.Const.videoPickerType,
                            smallTalkWriteViewModel.getVideoSpecModel(this@WriteArticleActivity).maxSeconds,
                            LocaleUtil.getAppLocale(this@WriteArticleActivity).language
                        )
                        return
                    }

                    showDialogImageOption()
                }

                override fun onPermissionDenied() {}
                override fun requestPermission(permissions: Array<String>) { }
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_READ_MEDIA_IMAGES) {
                showDialogImageOption()
            } else {
                smallTalkWriteViewModel.exodusImagePickerRegiser.launchMediaPicker(
                    this@WriteArticleActivity,
                    feature.common.exodusimagepicker.util.Const.videoPickerType,
                    smallTalkWriteViewModel.getVideoSpecModel(this@WriteArticleActivity).maxSeconds,
                    LocaleUtil.getAppLocale(this@WriteArticleActivity).language
                )
            }
        }
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

    companion object {
        private const val FILE_COUNT = 5
        private const val ARTICLES_OK = 0
        private const val ARTICLES_OK_HEART_PROVIDE = 1

        private const val RESPONSE_ARTICLES_2000 = 2000

        @JvmStatic
        fun createIntent(context: Context, model: IdolModel?, tagId: Int = -1): Intent {
            val intent = Intent(context, WriteArticleActivity::class.java)
            intent.putExtra(PARAM_IDOL, model as Parcelable?)
            intent.putExtra(PARAM_TAG, tagId)
            return intent
        }

        @JvmStatic
        fun createIntent(context: Context, model: IdolModel, text: String): Intent {
            val intent = Intent(context, WriteArticleActivity::class.java)
            intent.putExtra(PARAM_IDOL, model as Parcelable?)
            intent.putExtra(PARAM_TEXT, text)
            return intent
        }

        @JvmStatic
        fun createIntent(context: Context, model: IdolModel, uri: Uri): Intent {
            val intent = Intent(context, WriteArticleActivity::class.java)
            intent.putExtra(PARAM_IDOL, model as Parcelable?)
            intent.putExtra(PARAM_URI, uri.toString())
            return intent
        }

        const val PARAM_IDOL = "idol"
        const val PARAM_TEXT = "text"
        const val PARAM_URI = "uri"
        const val PARAM_TAG = "tag"
        const val PARAM_PATH_TEMP_FILE = "tempFilePath"
        const val PARAM_IS_SQUARE_IMAGE = "isSquareImage"
        const val PARAM_ORG_IMAGE_WIDTH = "orgImageWidth"
        const val PARAM_ORG_IMAGE_HEIGHT = "orgImageHeight"
        const val PARAM_ORG_IMAGE_URI = "orgImageUri"
    }
}