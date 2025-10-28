package net.ib.mn.feature.votingcertificate

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.model.VoteCertificateModel
import net.ib.mn.databinding.ActivityVotingCertificateBinding
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.getNameFromIdolLiteModel
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.setFirebaseUIAction
import net.ib.mn.viewmodel.VotingCertificateViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class VotingCertificateActivity : BaseActivity() {

    private lateinit var binding: ActivityVotingCertificateBinding
    private lateinit var shareName: String
    private var idolId: Long = 0

    private val viewModel: VotingCertificateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_voting_certificate)
        binding.lifecycleOwner = this
        binding.clContainer.applySystemBarInsets()

        setEventListener()
        setUI()
        observedVM()

        if (intent.hasExtra(CERTIFICATE_DATA)) {
            val certificate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(CERTIFICATE_DATA, VoteCertificateModel::class.java)!!
            } else {
                intent.getParcelableExtra(CERTIFICATE_DATA)!!
            }
            idolId = certificate.idol.id
            setCertificateData(certificate)
        } else if (intent.hasExtra(IDOL_ID)) {
            idolId = intent.getLongExtra(IDOL_ID, 0L)
            if (idolId == 0L) {
                finish()
            }
            viewModel.getVoteCertificate(idolId)
        } else {
            // 여기 올 일은 없는데 어떤 상황이 발생할지 몰라 추가만 해놓음
            Toast.makeText(this, "data error", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setUI() {
        val isKorea = LocaleUtil.getAppLocale(this) == Locale.KOREA
        val logoImage = if (isKorea) {
            if (BuildConfig.CELEB) R.drawable.img_logo_vote_celeb_kr else R.drawable.img_logo_vote_kr
        } else {
            if (BuildConfig.CELEB) R.drawable.img_logo_vote_celeb_en else R.drawable.img_logo_vote_en
        }
        binding.ivLogo.setImageResource(logoImage)
    }

    private fun setEventListener() {
        binding.tvSave.setOnClickListener {
            saveImageWithPermissionCheck()
        }

        binding.tvShare.setOnClickListener {
            setFirebaseUIAction(GaAction.CERTIFICATE_SHARE)
            val sharedFile =
                saveImageToCacheWithBackground(binding.layoutCertificate, this, "certificate_image")
            if (sharedFile != null) {
                shareImage(sharedFile, this)
            } else {
                Toast.makeText(this, R.string.certificate_detail_save_fail, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun observedVM() {
        viewModel.voteCertificate.observe(this, SingleEventObserver {
            setCertificateData(it)
        })

        viewModel.errorToast.observe(this, SingleEventObserver {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            finish()
        })
    }

    private fun setCertificateData(voteCertificateModel: VoteCertificateModel) {
        val idolName = getNameFromIdolLiteModel(this, voteCertificateModel.idol)
        shareName = idolName
        val splitName = UtilK.getName(idolName)

        val myName = IdolAccount.getAccount(this)?.userName
        UtilK.setNameWithInvisible(idolName, binding.tvName, binding.tvGroupName)
        binding.tvVoterName.text = myName

        supportActionBar?.let {
            it.title = getString(R.string.certificate_detail_title)
        }

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", LocaleUtil.getAppLocale(this))
        val outputFormat =
            SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, LocaleUtil.getAppLocale(this))
        val date: Date = inputFormat.parse(voteCertificateModel.refDate)!!
        val formattedDate = outputFormat.format(date) + " (KST)"

        binding.tvVoteDate.text = formattedDate
        binding.tvHeart.text = voteCertificateModel.vote.toString()

        if (voteCertificateModel.idol.imageUrl.isNotEmpty()) {
            val requestOptions = RequestOptions()
                .circleCrop()
            Glide.with(this)
                .load(voteCertificateModel.idol.imageUrl)
                .apply(requestOptions)
                .placeholder(R.drawable.menu_profile_default2)
                .error(R.drawable.menu_profile_default2)
                .into(binding.ivProfile)
        }

        val medal = enumValues<VotingCertificateGrade>()
            .firstOrNull { it.grade == voteCertificateModel.grade }?.medal
            ?: R.drawable.filled
        binding.ivMedal.setImageResource(medal)
    }

    private fun saveImageWithPermissionCheck() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // 권한 요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_STORAGE
                )
            } else {
                // 권한이 이미 허용된 경우 이미지 저장 실행
                saveImageToGalleryAndNotify(binding.layoutCertificate)
            }
        } else {
            // Android 10 이상은 권한 필요 없이 이미지 저장 실행
            saveImageToGalleryAndNotify(binding.layoutCertificate)
        }
    }

    private fun saveTransparentImageToGallery(
        view: View,
        context: Context,
        fileName: String
    ): Uri? {
        // 기존 배경 저장
        val originalBackground = view.background

        // 배경을 투명하게 설정
        view.setBackgroundColor(Color.TRANSPARENT)

        // 비트맵 생성 (ARGB_8888로 투명도 지원)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        // 배경 복원
        view.background = originalBackground

        // MediaStore에 저장
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png") // 파일 이름
            put(MediaStore.Images.Media.MIME_TYPE, "image/png") // PNG 형식
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Idol") // 저장 경로
            put(MediaStore.Images.Media.IS_PENDING, 1) // 저장 중 상태
        }

        val uri: Uri? =
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream) // 비트맵을 PNG로 저장
                }

                // 저장 완료 플래그 설정
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }

        return uri
    }

    private fun saveImageToDownloadsCompat(view: View, context: Context, fileName: String): Uri? {
        // 1. 뷰를 비트맵으로 변환
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val outputUri: Uri?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29 이상: MediaStore 사용
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val uri: Uri? = context.contentResolver.insert(collection, values)

            uri?.let {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    context.contentResolver.update(it, values, null, null)
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null
                }
            }
            outputUri = uri
        } else {
            // API 28 이하: 파일 시스템에 직접 저장
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "$fileName.png")

            try {
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                outputUri = Uri.fromFile(file)
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }
        return outputUri
    }

    private fun saveImageToGalleryAndNotify(view: View) {
        setFirebaseUIAction(GaAction.CERTIFICATE_SAVE) // Firebase 이벤트 호출
        val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", LocaleUtil.getAppLocale(this))
        val currentTime = Date()
        val formattedTime = dateFormat.format(currentTime)
        val fileName = if (BuildConfig.CELEB) {
            "Choeaedol_Celeb_$formattedTime"
        } else {
            "Choeaedol_$formattedTime"
        }

        val uri = saveImageToDownloadsCompat(view, this, fileName)
        if (uri == null) {
            Toast.makeText(this, R.string.certificate_detail_save_fail, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.certificate_detail_save_succ, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToCacheWithBackground(
        view: View,
        context: Context,
        fileName: String
    ): File? {
        // 배경이 포함된 비트맵 생성
        val bitmap = captureViewWithBackground(view)

        // 캐시 폴더 경로 설정
        val cachePath = File(context.cacheDir, "images")
        if (!cachePath.exists()) {
            cachePath.mkdirs() // 폴더 생성
        }

        val file = File(cachePath, "$fileName.png")
        try {
            // 파일 저장
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return file
    }

    private fun saveImageToCacheWithoutBackground(
        view: View,
        context: Context,
        fileName: String
    ): File? {
        // 투명한 비트맵 생성
        val bitmap = captureViewWithoutBackground(view)

        // 캐시 폴더 경로 설정
        val cachePath = File(context.cacheDir, "images")
        if (!cachePath.exists()) {
            cachePath.mkdirs() // 폴더 생성
        }

        val file = File(cachePath, "$fileName.png")
        try {
            // 파일 저장
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return file
    }

    private fun shareImage(file: File, context: Context) {
        val uri = FileProvider.getUriForFile(
            context,
            "${packageName}.provider",
            file
        )

        val appName = getString(R.string.app_name_upper)

        val params = listOf(LinkStatus.COMMUNITY.status)
        val querys = listOf(
            "idol" to idolId.toString()
        )

        val url = LinkUtil.getAppLinkUrl(context = this, params = params, querys = querys)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                getString(
                    R.string.certificate_detail_share_msg,
                    appName,
                    shareName
                ) + "\n\n${url}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }

    private fun captureViewWithBackground(view: View): Bitmap {
        // 비트맵 생성
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 배경이 있으면 배경 먼저 그리기
        val background = view.background
        if (background != null) {
            background.draw(canvas)
        }

        // 뷰 그리기
        view.draw(canvas)

        return bitmap
    }

    private fun captureViewWithoutBackground(view: View): Bitmap {
        // 기존 배경 저장
        val originalBackground = view.background

        // 배경 제거 (투명으로 설정)
        view.setBackgroundColor(Color.TRANSPARENT)

        // 비트맵 생성
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        // 배경 복원
        view.background = originalBackground

        return bitmap
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 허용된 경우 다시 이미지 저장 실행
                val layoutCertificate = binding.layoutCertificate
                saveImageToGalleryAndNotify(layoutCertificate)
            } else {
                // 권한 거부된 경우 사용자에게 알림
                Toast.makeText(this, "권한이 필요합니다. 저장을 진행할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val REQUEST_WRITE_STORAGE = 1001
        const val CERTIFICATE_DATA = "certificateData"
        const val IDOL_ID = "idolId"
    }
}