package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.dto.UploadFileDTO
import net.ib.mn.core.data.repository.InquiryRepositoryImpl
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.model.PresignedModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ImageUtil
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import javax.inject.Inject

@AndroidEntryPoint
class FaqWriteActivity : BaseWriteActivity(), View.OnClickListener {

    private var localGif: Uri? = null
    @Inject
    lateinit var inquiryRepository: InquiryRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()
    }

    private fun init() {

        type = TYPE_INQUIRY
        fileCount = FILE_COUNT

        with(binding) {
            includeFaqCategory.root.visibility = View.VISIBLE
            tvContentCount.visibility = View.VISIBLE
            etContent.hint = getString(R.string.inquiry_notice)
            tvTitle.text = getString(R.string.setting_menu02)
            btnWrite.text = getString(R.string.register)

            btnWrite.setOnClickListener(this@FaqWriteActivity)
            includeFaqCategory.root.setOnClickListener(this@FaqWriteActivity)
            btnPhoto.setOnClickListener(this@FaqWriteActivity)
            btnVideo.setOnClickListener(this@FaqWriteActivity)
            btnClose.setOnClickListener(this@FaqWriteActivity)
        }

        setFileStatus(FILE_COUNT,false)


        val locale = LocaleUtil.getAppLocale(this)
        //처음 뷰 생성될 때 글자수 제한 텍스트 생성
        binding.tvContentCount.text = "${NumberFormat.getNumberInstance(locale).format(binding.etContent.text.toString().length)}/${NumberFormat.getNumberInstance(locale).format(Const.MAX_FAQ_LENGTH)}"

        // 글자수 제한
        val filterArray = arrayOf<InputFilter>(InputFilter.LengthFilter(Const.MAX_FAQ_LENGTH))
        binding.etContent.filters = filterArray

        //텍스트 바뀔 때 글자수 제한 텍스트 변환
        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.tvContentCount.text = "${NumberFormat.getNumberInstance(locale).format(binding.etContent.text.toString().length)}/${NumberFormat.getNumberInstance(locale).format(Const.MAX_FAQ_LENGTH)}"
            }
        })
    }

    override fun onClick(v: View?) {

        when (v) {
            binding.btnWrite -> {  //등록 버튼 눌렀을 경우
                Util.hideSoftKeyboard(this, binding.etContent)

                showSatisfiedConditionSnackBar { isSatisfiedAllCondition ->
                    if (!isSatisfiedAllCondition) {
                        return@showSatisfiedConditionSnackBar
                    }

                    submitInquiry() //등록하는 api 호출
                }
            }
            binding.includeFaqCategory.root -> { //문의 유형 버튼 눌렀을 경우
                showCategoryBottomSheet()
            }
            binding.btnPhoto -> {
                getPhotoOrVideo(true)
            }
            binding.btnVideo -> { //파일 첨부하기 버튼 눌렀을 경우
                getPhotoOrVideo(false)
            }
            binding.btnClose -> onBackPressed()
        }
    }

    //등록버튼 눌렀을 때 예외상황 다 통과했을 경우 호출
    private fun submitInquiry() {
        val title =
            Build.MODEL + "/" + Build.VERSION.RELEASE + "/v" + getString(R.string.app_version)
        val inquiry: String = binding.etContent.text.toString()

        Util.showProgress(this, false) //연타 방지를 위함.
        MainScope().launch {
            inquiryRepository.postInquiry(
                inquiryType,
                title,
                inquiry,
                filesArray,
                { response ->
                    if (response.optBoolean("success")) {
                        binding.etContent.setText("")
                        Util.closeProgress()
                        Util.showDefaultIdolDialogWithBtn1(
                            this@FaqWriteActivity,
                            null,
                            getString(R.string.registered)
                        ) {
                            if (beforeActivity == 1) {
                                beforeActivity = 2
                            }
                            finish()
                        }
                    } else {
                        Util.closeProgress()
                        UtilK.handleCommonError(this@FaqWriteActivity, response)
                    }
                },
                { throwable ->
                    Util.closeProgress()
                    Toast.makeText(
                        this@FaqWriteActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode != RESULT_OK) {
            return
        }

        when(requestCode) {
            PHOTO_SELECT_REQUEST, VIDEO_SELECT_REQUEST -> { // 이미지를 최신/갤러리에서 선택 시 호출 또는 동영상을 선택했을 시 호출
                data?.data?.let { cropArticlePhoto(it, false) }
            }
            PHOTO_CROP_REQUEST -> {
                if(mTempFileForCrop != null) {
                    try {
                        onArticlePhotoSelected(Uri.fromFile(mTempFileForCrop))
                    } catch (e: SecurityException) {
                        Toast.makeText(this, R.string.image_permission_error, Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, R.string.image_permission_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cropArticlePhoto(uri: Uri, isSharedImage: Boolean) {
        ImageUtil.cropArticlePhoto(this, uri, isSharedImage, false, Const.MAX_FAQ_FILE_SIZE * 1024 * 1024,
            { fileData ->
                // GIF!
                mItemsUriArray.add(uri)
                binImage = fileData
                localGif = uri
                writeMultiImgAdapter.setItemsByteArray(mItemsUriArray, false)
                setFileStatus(FILE_COUNT,false)
                getPresignedUrl(
                    Const.NCLOUD_INQUIRY_BUCKET,
                    (UtilK.setUriPath(this, uri)),
                    fileData,
                    "image/gif"
                )
            }, { fileData ->
                mItemsUriArray.add(uri)
                writeMultiImgAdapter.setItemsByteArray(mItemsUriArray, false)
                setFileStatus(FILE_COUNT,false)
                getPresignedUrl(
                    Const.NCLOUD_INQUIRY_BUCKET,
                    (UtilK.setUriPath(this, uri)),
                    fileData,
                    "video/mp4"
                )
            }, { options ->
                originSrcUri = uri
                originSrcWidth = options.outWidth
                originSrcHeight = options.outHeight

            })
        binding.clPhoto.visibility = View.VISIBLE
    }

    //사진 선택했을 경우
    private fun onArticlePhotoSelected(uri: Uri) {
        ImageUtil.onArticlePhotoSelected(this, uri, originSrcWidth, originSrcHeight, originSrcUri,
            {
            }, { stream ->
                mItemsUriArray.add(uri)
                binImage = stream.toByteArray()
                writeMultiImgAdapter.setItemsByteArray(mItemsUriArray, false)
                setFileStatus(fileCount, false)
                //이미지는 확장자 같이 붙어있어서 UtilK.setUriPath안씀.
                val uriPath = uri.lastPathSegment!!.split(":").last()
                getPresignedUrl(Const.NCLOUD_INQUIRY_BUCKET, uriPath, binImage!!, "image/jpeg")
                binding.clPhoto.visibility = View.VISIBLE
            })
    }

    private fun getPresignedUrl(
        bucket: String,
        filename: String,
        binImage: ByteArray,
        mimeType: String
    ) {
        //서버에서 cdn key값들 받아오는 api
        lifecycleScope.launch {
            filesRepository.getPresignedUrl(
                bucket = bucket,
                filename = filename,
                listener = { response ->
                    if (response?.optBoolean("success")!!) {
                        val gson = IdolGson.getInstance()
                        var presignedModel = gson.fromJson(
                            response.getJSONObject("fields").toString(),
                            PresignedModel::class.java
                        )
                        presignedModel.savedFilename = response.getString("saved_filename")
                        presignedModel.url = response.getString("url")

                        //savedFile 저장해서 delete할 때 사용
                        savedFileNameList.add(presignedModel.savedFilename)

                        //cdn key값들 받아왔을 경우, byteArray까지 같이 보내서 올림
                        lifecycleScope.launch {
                            filesRepository.writeCdn(
                                url = presignedModel.url,
                                AWSAccessKeyId = presignedModel.AWSAccessKeyId,
                                acl = presignedModel.acl,
                                key = presignedModel.key,
                                policy = presignedModel.policy,
                                signature = presignedModel.signature,
                                file = binImage,
                                filename = presignedModel.savedFilename,
                                mimeType = mimeType,
                                listener = { response ->
                                    setFileStatus(FILE_COUNT,false)
                                    //파일 첨부 성공했을 경우
                                    writeMultiImgAdapter.closeProgressDialog(savedFileNameList.size - 1)
                                },
                                errorListener = {
                                    //파일 첨부 실패했을 경우 팝업 띄우고
                                    Util.showDefaultIdolDialogWithBtn1(
                                        this@FaqWriteActivity,
                                        null,
                                        getString(R.string.msg_file_upload_failed)
                                    ) {
                                        Util.closeIdolDialog()
                                    }
                                    //adapter에 올린 썸네일 파일 삭제
                                    savedFileNameList.removeAt(savedFileNameList.size-1)
                                    mItemsUriArray.removeAt(savedFileNameList.size)
                                    writeMultiImgAdapter.setItemsByteArray(mItemsUriArray, true)
                                    setFileStatus(FILE_COUNT,false)
                                }
                            )
                        }

                        //presignedUrl값 처리 후 inquiry api에 보낼 FilesModel Add
                        filesArray.add(UploadFileDTO(binImage.size, presignedModel.key, filename))
                    } else {
                        ErrorControl.parseError(this@FaqWriteActivity, response)?.let {
                            Toast.makeText(this@FaqWriteActivity, it, Toast.LENGTH_SHORT).show()
                        }
                        //adapter에 올린 썸네일 파일 삭제
                        mItemsUriArray.removeAt(savedFileNameList.size)
                        writeMultiImgAdapter.setItemsByteArray(mItemsUriArray, true)
                        setFileStatus(FILE_COUNT,false)
                    }
                },
                errorListener = { throwable ->
                }
            )
        }
    }

    //bottomSheet에서 클릭 시 보이는 카테고리 스트링 변경
    fun setCategory(category: String, categoryType: String) {
        binding.includeFaqCategory.tvCategory.text = category
        this.inquiryType = categoryType
    }

    //bottomSheetDialog 실행
    private fun showCategoryBottomSheet() {
        val sheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_FAQ_FILTER)
        val tag = "faq_filter"
        val oldFrag = this.supportFragmentManager.findFragmentByTag(tag)

        if (oldFrag == null) {
            sheet.show((this).supportFragmentManager, tag)
        }
    }

    //이미지인지, 비디오인지 선택했을 경우 gallery에서 알맞게 get
    fun getPhotoOrVideo(isImg: Boolean) {

        if (mItemsUriArray.size >= FILE_COUNT) {  //파일 개수 제한
            return
        }

        if (isImg) {
            UtilK.getPhoto(this, true)
        } else {
            UtilK.getPhoto(this, false)
        }
    }

    //이미지인지, 비디오인지 고르는 카테고리 보여주는 BottomSheet
    private fun showMediaCategoryBottomSheet() {
        val sheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_MEDIA_FILTER)
        val tag = "faq_media_filter"
        val oldFrag = this.supportFragmentManager.findFragmentByTag(tag)

        if (oldFrag == null) {
            sheet.show((this).supportFragmentManager, tag)
        }
    }

    companion object {
        const val FILE_COUNT = 3
        var beforeActivity: Int = -1

        @JvmStatic
        fun createIntent(context: Context?, beforeActivity: Int): Intent {
            this.beforeActivity = beforeActivity    //0인 경우 : FaqAdapter에서 넘어온 값, 1인 경우 : FaqActivity에서 넘어온 값
            return Intent(context, FaqWriteActivity::class.java)
        }
    }

}