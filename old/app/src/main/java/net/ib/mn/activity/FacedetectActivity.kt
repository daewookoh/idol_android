package net.ib.mn.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.ImagesRepository
import net.ib.mn.databinding.ActivityFacedetectBinding
import net.ib.mn.dialog.ProgressDialogFragment
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.ImageUtil
import net.ib.mn.utils.MediaStoreUtils
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.link.LinkUtil
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class FacedetectActivity : BaseActivity(), View.OnClickListener {
    @Inject
    lateinit var imagesRepository: ImagesRepository

	// 사진원본
	private var originSrcUri: Uri? = null
	private var originSrcWidth = 0
	private var originSrcHeight = 0

	private var binImage: ByteArray? = null

	private var mGlideRequestManager: RequestManager? = null

	private var category : String? = null

    private var showBtnShare: Boolean = true

    private lateinit var binding: ActivityFacedetectBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_facedetect)
        binding.clContainer.applySystemBarInsets()

        supportActionBar?.setTitle(R.string.face_title)

		mGlideRequestManager = Glide.with(this)
        with(binding) {
            photo.setOnClickListener(this@FacedetectActivity)
            btnPhotoUpload.setOnClickListener(this@FacedetectActivity)
            btnWrite.setOnClickListener(this@FacedetectActivity)
            llManButton.setOnClickListener(this@FacedetectActivity)
            llWomanButton.setOnClickListener(this@FacedetectActivity)
//            btnShare.setOnClickListener(this@FacedetectActivity)
        }
	}

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)
        val btnShare = menu.findItem(R.id.btn_share)
        btnShare.isVisible = showBtnShare
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.btn_share -> {
                val msg = getString(R.string.face_share_msg)
                val url = LinkUtil.getAppLinkUrl(
                    this@FacedetectActivity,
                    params = listOf(LinkStatus.FACEDETECT.status)
                )
                UtilK.linkStart(this@FacedetectActivity, url = url, msg = msg)
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.COMMENT_ARTICLE_SHARE.actionValue,
                    GaAction.COMMENT_ARTICLE_SHARE.label
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }


	override fun onClick(v: View?) { with(binding) {
		when (v?.id){
			photo.id -> {
				onArticlePhotoClick(null, false)
			}
			btnPhotoUpload.id -> {
				if (photo.visibility == View.VISIBLE) {
					Toast.makeText(this@FacedetectActivity, R.string.msg_link_image_guide, Toast.LENGTH_SHORT).show();
				} else {
					onArticlePhotoClick(null, false)
				}
			}
			btnWrite.id -> {
				selectWriteBtn(binImage, llManButton.isSelected, llWomanButton.isSelected)
				if(!btnWrite.isSelected) return

				if(llPhotoUploadWrapper.visibility == View.VISIBLE) { // 최종결과를 보기 위해 닮은꼴 찾기 버튼을 눌렀을 경우
					ProgressDialogFragment.show(this@FacedetectActivity, "upload", R.string.wait_upload_article)
					uploadFaceImage(binImage, category)
				}
				else { // 최종결과 본 후 닮은꼴 찾기 버튼 눌렀을 경우
					initializeSelect()
					selectWriteBtn(binImage, llManButton.isSelected, llWomanButton.isSelected)
					setManOrWoman(isMan = false, isWoman = false)
					llPhotoUploadResult.visibility = View.GONE
					photo.visibility = View.GONE
					btnPhotoUpload.visibility = View.VISIBLE
					llPhotoUploadWrapper.visibility = View.VISIBLE
                    isFaceDetectResult(false)
				}
			}
			llManButton.id -> {
				llManButton.isSelected = true
				llWomanButton.isSelected = false
				selectWriteBtn(binImage, llManButton.isSelected, llWomanButton.isSelected)
				setManOrWoman(isMan = true, isWoman = false)
			}
			llWomanButton.id -> {
				llManButton.isSelected = false
				llWomanButton.isSelected = true
				selectWriteBtn(binImage, llManButton.isSelected, llWomanButton.isSelected)
				setManOrWoman(isMan = false, isWoman = true)
			}
//            btnShare.id -> {
//                UtilK.linkStart(this@FacedetectActivity, LinkStatus.FACEDETECT.status)
//                setUiActionFirebaseGoogleAnalyticsActivity(
//                    GaAction.COMMENT_ARTICLE_SHARE.actionValue,
//                    GaAction.COMMENT_ARTICLE_SHARE.label
//                )
//            }

            else -> {
                return
            }
        }
	}}

    private fun initializeSelect() {
        binImage = null
        binding.llManButton.isSelected = false
        binding.llWomanButton.isSelected = false
    }

	private fun uploadFaceImage(image: ByteArray?,
							 category: String?) { with(binding) {

        lifecycleScope.launch {
            imagesRepository.facedetect(
                image ?: return@launch,
                category ?: return@launch,
                { response ->
                    ProgressDialogFragment.hide(this@FacedetectActivity, "upload")
                    if (response.optBoolean("success")) {
                        val gson = IdolGson.getInstance()
                        val idol = gson.fromJson(response.getJSONObject("idol").toString(), IdolModel::class.java)
                        Util.log("response::$response")
//						idol.setLocalizedName(this@FacedetectActivity)

                        tvName.text = Util.nameSplit(this@FacedetectActivity, idol)[0]
                        tvGroup.text = Util.nameSplit(this@FacedetectActivity, idol)[1]

                        val numberFormat = NumberFormat.getInstance(Locale.ENGLISH) as DecimalFormat
                        tvSimilarity.text = "${getString(R.string.face_similarity)} ${(1.minus(numberFormat.format(response.optDouble("similarity")).toDouble())*100).toInt()}%"
                        message.text = response.optString("message")
                        mGlideRequestManager!!.asBitmap()
                            .load(response.optString("image_url"))
                            .into(idolImage)

                        llPhotoUploadWrapper.visibility = View.GONE
                        llPhotoUploadResult.visibility = View.VISIBLE
                        isFaceDetectResult(true)
                    } else {
                        val responseMsg = ErrorControl.parseError(this@FacedetectActivity, response)
                        if(responseMsg != null) {
                            Toast.makeText(this@FacedetectActivity, responseMsg, Toast.LENGTH_SHORT).show()
                        }
                        photo.visibility = View.GONE

                        initializeSelect()
                        selectWriteBtn(binImage, llManButton.isSelected, llWomanButton.isSelected)
                        setManOrWoman(llManButton.isSelected, llWomanButton.isSelected)

                        btnPhotoUpload.visibility = View.VISIBLE
                    }
                },
                {
                    ProgressDialogFragment.hide(this@FacedetectActivity,
                        "upload")
                    Toast.makeText(this@FacedetectActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                    photo.visibility = View.GONE
                    btnPhotoUpload.visibility = View.VISIBLE
                }
            )
        }
	}}

	private fun onArticlePhotoClick(uri: Uri?, isSharedImage: Boolean) {
		if (uri != null) openImageEditor(uri, isSharedImage)
		else { // 일부 삼성 폰에서 기본앱 설정이 안되는 현상으로 아래와 같이 바꿔봄...
			val photoPickIntent = MediaStoreUtils.getPickImageIntent(this)
			val packageManager = packageManager
			if (photoPickIntent.resolveActivity(packageManager) != null) {
				startActivityForResult(photoPickIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), PHOTO_SELECT_REQUEST)
			} else {
				Util.showDefaultIdolDialogWithBtn1(this@FacedetectActivity,
					null,
					getString(R.string.cropper_not_found)
				) { Util.closeIdolDialog() }
			}
		}
	}

	private fun openImageEditor(uri: Uri, isSharedImage:Boolean) {
        ImageUtil.openImageEditor(this, uri, isSharedImage, true){ options ->
			originSrcUri = uri
			originSrcWidth = options.outWidth
			originSrcHeight = options.outHeight
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		if (requestCode == PHOTO_SELECT_REQUEST
			&& resultCode == Activity.RESULT_OK) {
			openImageEditor(data!!.data!!, false)
		} else if (requestCode == PHOTO_CROP_REQUEST && resultCode == Activity.RESULT_OK) {
			if (mTempFileForCrop != null) {
				onArticlePhotoSelected(Uri.fromFile(mTempFileForCrop))
				mTempFileForCrop?.deleteOnExit()
			}
		} else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
			val result = CropImage.getActivityResult(data)
			if (resultCode == Activity.RESULT_OK) {
				val resultUri = result.uri
				onArticlePhotoSelected(resultUri)
			} else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
				val error = result.error
			}
		}
	}

	private fun onArticlePhotoSelected(uri: Uri) {
		if (binding.photo.visibility == View.GONE) {
            binding.photo.visibility =View.VISIBLE
		}
		mGlideRequestManager!!.load(uri).into(binding.photo)
		mGlideRequestManager!!.load(uri).into(binding.userImage)

		ImageUtil.onArticlePhotoSelected(this, uri, originSrcWidth, originSrcHeight, originSrcUri, { scaledBitmap ->
			binding.photo.setImageBitmap(scaledBitmap)
		}, { stream ->
			binImage = stream.toByteArray()
			selectWriteBtn(binImage, binding.llManButton.isSelected, binding.llWomanButton.isSelected)

			binding.btnPhotoUpload.visibility = View.GONE
		})
	}

	//이미지가 있고, 여자나 남자를 선택했는지 체크해서 btn_write 버튼 on/off
	private fun selectWriteBtn(binImage : ByteArray?, manSelected : Boolean, womanSelected : Boolean){
		if(binImage!= null && (manSelected || womanSelected)){
			binding.btnWrite.isSelected = true
			return
		}
		binding.btnWrite.isSelected = false
	}

	private fun setManOrWoman(isMan: Boolean, isWoman: Boolean) {
		if(isMan && !isWoman) {
			with(binding){
				ivMan.setImageResource(R.drawable.btn_man_on)
				ivWoman.setImageResource(R.drawable.btn_woman_off)
				tvMan.setTextColor(ContextCompat.getColor(this@FacedetectActivity, R.color.main))
				tvWoman.setTextColor(ContextCompat.getColor(this@FacedetectActivity, R.color.gray1000))
			}
			category="M"
		}else if(!isMan && isWoman) {
			with(binding) {
				ivMan.setImageResource(R.drawable.btn_man_off)
				ivWoman.setImageResource(R.drawable.btn_woman_on)
				tvMan.setTextColor(ContextCompat.getColor(this@FacedetectActivity, R.color.gray1000))
				tvWoman.setTextColor(ContextCompat.getColor(this@FacedetectActivity, R.color.main))
			}
			category="F"
		}else {
			with(binding) {
				ivMan.setImageResource(R.drawable.btn_man_off)
				ivWoman.setImageResource(R.drawable.btn_woman_off)
				tvMan.setTextColor(ContextCompat.getColor(this@FacedetectActivity, R.color.gray1000))
				tvWoman.setTextColor(ContextCompat.getColor(this@FacedetectActivity, R.color.gray1000))
			}
			category= null
		}
	}

    private fun isFaceDetectResult(isFaceDetectResult: Boolean) = with(binding) {
        showBtnShare = !isFaceDetectResult
        if(isFaceDetectResult) {
            supportActionBar?.setTitle(R.string.title_result_face_detect)
//            btnShare.visibility = View.VISIBLE
            btnWrite.apply {
//                background = null
//                setTextColor(ContextCompat.getColor(this@FacedetectActivity, R.color.main))
                setText(R.string.title_retry)
            }
        } else {
            supportActionBar?.setTitle(R.string.face_title)
//            btnShare.visibility = View.GONE
            btnWrite.apply {
//                background = ContextCompat.getDrawable(this@FacedetectActivity, R.drawable.facedetect_radius)
//                setTextColor(ContextCompat.getColor(this@FacedetectActivity, R.color.text_white_black))
                setText(R.string.face_submit)
            }
        }
        invalidateOptionsMenu()
    }

	companion object {
		@JvmStatic
		fun createIntent(context: Context): Intent {
			return Intent(context, FacedetectActivity::class.java)
		}
	}
}
