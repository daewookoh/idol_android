package net.ib.mn

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.activity.BaseActivity.Companion.EXTRA_IS_FREE_BOARD_REFRESH
import net.ib.mn.activity.BaseActivity.Companion.EXTRA_NEXT_ACTIVITY
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.FreeboardActivity
import net.ib.mn.activity.MainActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.dto.FileData
import net.ib.mn.core.data.repository.FilesRepository
import net.ib.mn.core.domain.usecase.CheckReadyUseCase
import net.ib.mn.core.domain.usecase.CreateArticleUseCase
import net.ib.mn.core.domain.usecase.InsertArticleUseCase
import net.ib.mn.core.model.UploadVideoSpecModel
import net.ib.mn.model.PresignedModel
import net.ib.mn.model.PresignedRequestModel
import net.ib.mn.model.WriteArticleModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Logger
import net.ib.mn.utils.MediaExtension
import net.ib.mn.utils.Toast
import net.ib.mn.utils.UploadSingleton
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.encode.TransCoder
import net.ib.mn.utils.ext.getDimensions
import net.ib.mn.utils.ext.getHash
import net.ib.mn.utils.ext.getSizeMB
import net.ib.mn.utils.ext.toByteArray
import org.json.JSONArray
import java.io.File
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.roundToInt

@AndroidEntryPoint
open class PresignedUrlService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var builder: NotificationCompat.Builder
    private var pendingIntent: PendingIntent? = null
    private var timer: Timer? = null


    private var jsonArray: JSONArray = JSONArray()
    private var filesArray = ArrayList<FileData>()
    private var presignedSuccessCount: Int = 0

    private val progressMax = 100 // progress 퍼센트 Max
    private var progressCurrent = 0 // 현재 progress 값
    private var oneItemProgressCount = 0 // 하나의 아이템 당 progress 값
    private var videoZipCount = 80

    private var type: Int? = 0
    private var locale: Locale? = null
    private var localeString: String = ""
    private var presignedRequestModelList: ArrayList<PresignedRequestModel>? = ArrayList()
    private var writeArticleModel: WriteArticleModel? = WriteArticleModel()
    private var finishMsg: String? = null // 서비스 종료시 표시할 메시지 (용량 초과로 업로드 실패 메시지가 덮어써지지 않게)

    private var isRetry: Boolean = false

    private var tempVideoFile: File? = null

    private var isSuccessDestroyed: Boolean = false

    private var notificationManagerCompat: NotificationManagerCompat? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    @Inject
    lateinit var insertArticleUseCase: InsertArticleUseCase
    @Inject
    lateinit var createArticleUseCase: CreateArticleUseCase
    @Inject
    lateinit var checkReadyUseCase: CheckReadyUseCase
    @Inject
    lateinit var filesRepository: FilesRepository

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    // manifest에서 stopWithTask false로 선언하여 백그라운드에서 종료되었을 때 호출
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        Logger.v("mingue", "onTaskRemoved")
        setProgressBar(false)
    }

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Toast.makeText(applicationContext, "onStartCommand called with null Intent", Toast.LENGTH_SHORT).show()
            return START_NOT_STICKY
        }

        type = UploadSingleton.getType()
        presignedRequestModelList = UploadSingleton.getPresignedRequestModelList()
        writeArticleModel = UploadSingleton.getWriteArticleModel()
        localeString = UploadSingleton.getLocale().toString()
        locale = UtilK.getLocale(localeString)

        val isVideo =
            if (presignedRequestModelList.isNullOrEmpty()) false
            else presignedRequestModelList?.get(0)?.mimeType == MediaExtension.MP4.value

        setOneItemProgressCount(presignedRequestModelList, isVideo)

        var idolId = writeArticleModel?.idolModel?.getId()
        // 돌아갈 곳이 지정되어 있으면
        if(UploadSingleton.getReturnTo() != 0) {
            idolId = UploadSingleton.getReturnTo()
        }
        pendingIntent = when (idolId) {

            Const.IDOL_ID_FREEBOARD -> {
                val boardIntent = if (BuildConfig.CELEB) {
                    FreeboardActivity.createIntent(applicationContext).apply {
                        putExtra(Const.EXTRA_TAG_ID, UploadSingleton.getTag())
                    }
                } else {
//                    BoardActivity.createIntent(
//                        applicationContext, writeArticleModel?.idolModel?.getId()!!
//                    )
                    FreeboardActivity.createIntent(applicationContext).apply {
                        putExtra(EXTRA_IS_FREE_BOARD_REFRESH, true) // 자게로 이동 후 리프레시 하도록 설정
//                        putExtra(EXTRA_NEXT_ACTIVITY, MainActivity::class.java)
                        putExtra(Const.EXTRA_TAG_ID, UploadSingleton.getTag())
                    }
                }

                PendingIntent.getActivity(
                    this,
                    0,
                    boardIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            else -> {
                if (!writeArticleModel?.title.isNullOrEmpty()) {
                    PendingIntent.getActivity(
                        this,
                        0,
                        writeArticleModel?.idolModel?.let {
                            CommunityActivity.createIntent(
                                applicationContext,
                                it,
                                CommunityActivity.CATEGORY_SMALL_TALK,
                                true
                            )
                        },
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                } else {
                    PendingIntent.getActivity(
                        this,
                        0,
                        writeArticleModel?.idolModel?.let {
                            CommunityActivity.createIntent(
                                applicationContext,
                                it,
                                CommunityActivity.CATEGORY_COMMUNITY,
                                true
                            )
                        },
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Const.PUSH_CHANNEL_ID_ARTICLE_POSTING,
                UtilK.getLocaleStringResource(locale, R.string.article_post, applicationContext),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                vibrationPattern = longArrayOf(0)
                enableVibration(true)
                setSound(null, null)
            }
            notificationManager =
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
                    createNotificationChannel(channel)
                }
            builder = NotificationCompat.Builder(
                applicationContext,
                Const.PUSH_CHANNEL_ID_ARTICLE_POSTING
            )
        } else {
            builder = NotificationCompat.Builder(applicationContext)
        }

        val appName = if (BuildConfig.CELEB) {
            R.string.actor_app_name
        } else if (BuildConfig.ONESTORE) {
            R.string.app_name_onestore
        } else if (BuildConfig.CHINA) {
            R.string.app_name_china
        } else {
            R.string.app_name
        }

        builder.apply {
            setAutoCancel(true)
            setSmallIcon(R.mipmap.ic_launcher)
            setContentTitle(UtilK.getLocaleStringResource(locale, appName , applicationContext))
            setContentText(UtilK.getLocaleStringResource(locale, R.string.article_uploading, applicationContext))
        }

        notificationManagerCompat = NotificationManagerCompat.from(this).apply {
            if (ActivityCompat.checkSelfPermission(
                    this@PresignedUrlService,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@apply
            }
            notify(PRESIGNED_NOTIFICATION_ID, builder.build())
        }

        if (isVideo) {
            startVideoEncode()
        } else {
            startPresignedAndCreate()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        val stringId = if(isSuccessDestroyed) R.string.complete_article_upload else R.string.fail_article_upload
        uploadingFinished(UtilK.getLocaleStringResource(locale, stringId, applicationContext), isSuccessDestroyed = isSuccessDestroyed)
        serviceScope.cancel()
        timer?.cancel()
    }

    private fun startPresignedAndCreate() {
        if (presignedRequestModelList.isNullOrEmpty()) { // 이미지, 동영상 첨부하지 않았을 경우
            Logger.v("mingue", "이미지, 동영상 첨부하지 않음")
            when (type) {
                TYPE_SMALL_TALK -> insertArticle()
                else -> createArticle()
            }
            return
        }

        Logger.v("mingue", "이미지, 동영상 첨부함")
        for (i in 0 until presignedRequestModelList!!.size) {
            getPresignedUrl(presignedRequestModelList!![i], i) { presignedSuccessCount ->
                Logger.v("mingue", "presignedSuccessCount : " + presignedSuccessCount + "   presignedRequestModeListSize : " + presignedRequestModelList!!.size)
                progressCurrent += oneItemProgressCount
                setProgressBar(true)
                if (presignedSuccessCount == presignedRequestModelList!!.size) {
                    when (type) {
                        TYPE_SMALL_TALK -> insertArticle()
                        else -> createArticle()
                    }
                }
            }
        }
    }

    private fun setProgressBar(
        isSuccess: Boolean,
        msg: String? = UtilK.getLocaleStringResource(locale, R.string.fail_article_upload, applicationContext)
    ) {
        if (!isSuccess) {
            uploadingFinished(msg, false)
            return
        }
        if (progressCurrent == progressMax) {
            isSuccessDestroyed = true
            builder.setContentIntent(pendingIntent)
            uploadingFinished(UtilK.getLocaleStringResource(locale, R.string.complete_article_upload, applicationContext))
            return
        }

        notificationManagerCompat?.apply {
            if (ActivityCompat.checkSelfPermission(
                    this@PresignedUrlService,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@apply
            }
            builder.setProgress(progressMax, progressCurrent, false)
            notify(PRESIGNED_NOTIFICATION_ID, builder.build())
        }
    }

    // 이미지, 동영상이 있을 경우 하나의 아이템 당 채워지는 progress 개수 계산
    private fun setOneItemProgressCount(
        presignedRequestModelList: ArrayList<PresignedRequestModel>?,
        isVideo: Boolean
    ) {
        if (isVideo) {
            oneItemProgressCount = 10
            return
        }

        if (!presignedRequestModelList.isNullOrEmpty()) {
            oneItemProgressCount = ((progressMax - 10) / presignedRequestModelList.size)
        }
    }

    private fun getPresignedUrl(
        presignedRequestModel: PresignedRequestModel,
        position: Int,
        callback: ((Int) -> Unit)?
    ) {
        // TODO 왜인지 모르게 mimeType 다 하드코딩으로 넣고 있어서 완전한 수정 현재 불가능 업로드 쪽 코드 수정하면 자연스레 mimeType에 따라 확장자 붙이는걸로 수정될듯함
        if (presignedRequestModel.mimeType == "image/jpeg") {
            val validExtensions = listOf(".jpg", ".png", ".jpeg", ".webp")
            val hasValidExtension = validExtensions.any { presignedRequestModel.uriPath?.endsWith(it, ignoreCase = true) ?: true }

            if (!hasValidExtension) {
                presignedRequestModel.uriPath += ".jpg"
            }
        }

        serviceScope.launch {
            // 서버에서 cdn key값들 받아오는 api
            filesRepository.getPresignedUrl(
                presignedRequestModel.bucket,
                presignedRequestModel.uriPath,
                presignedRequestModel.srcWidth,
                presignedRequestModel.srcHeight,
                presignedRequestModel.hash,
                presignedRequestModel.fileType,
                lambda@ { response ->
                    Logger.v("mingue", " presignedUrl response : " + response)
                    if (response == null) {
                        uploadingFinished()
                        return@lambda
                    }
                    // 이미 있는 이미지인 경우 (success false, gcode 3900)
                    if (!response.optBoolean("success") && response.optInt("gcode") == ErrorControl.ERROR_3900) {
                        presignedSuccessCountCallback(
                            presignedRequestModel,
                            response.getString("saved_filename"),
                            position
                        ) {
                            callback?.invoke(presignedSuccessCount)
                        }
                        return@lambda
                    }

                    val gson = IdolGson.getInstance()
                    val presignedModel = gson.fromJson(
                        response.getJSONObject("fields").toString(),
                        PresignedModel::class.java
                    )
                    presignedModel.savedFilename = response.getString("saved_filename")
                    presignedModel.url = response.getString("url")

                    // cdn key값들 받아왔을 경우, byteArray까지 같이 보내서 올림
                    serviceScope.launch {
                        filesRepository.writeCdn(
                            url = presignedModel.url,
                            AWSAccessKeyId = presignedModel.AWSAccessKeyId,
                            acl = presignedModel.acl,
                            key = presignedModel.key,
                            policy = presignedModel.policy,
                            signature = presignedModel.signature,
                            file = presignedRequestModel.byteArray ?: ByteArray(0),
                            filename = presignedModel.savedFilename,
                            mimeType = presignedRequestModel.mimeType ?: "",
                            listener = { response ->
                                presignedSuccessCountCallback(
                                    presignedRequestModel,
                                    presignedModel.savedFilename,
                                    position
                                ) {
                                    callback?.invoke(presignedSuccessCount)
                                }
                            },
                            errorListener = {
                                uploadingFinished(UtilK.getLocaleStringResource(locale, R.string.msg_file_upload_failed, applicationContext))
                            }
                        )
                    }
                },
                { throwable ->
                    uploadingFinished(throwable.message)
                }
            )
        }
    }

    private fun presignedSuccessCountCallback(
        presignedRequestModel: PresignedRequestModel,
        savedFileName: String,
        position: Int,
        callback: (() -> Unit)
    ) {
        filesArray.add(
            FileData(
                seq = position + 1,
                size = presignedRequestModel.byteArray?.size?.toLong() ?: 0,
                savedFilename = savedFileName,
                originName = presignedRequestModel.uriPath ?: ""
            )
        )
        writeArticleModel?.files = filesArray
        presignedSuccessCount++
        callback.invoke()
    }

    // 커뮤니티, 지식돌, 자게 게시글 등록 시
    private fun createArticle() {
        serviceScope.launch {
            try {
                createArticleUseCase(writeArticleModel!!.toWriteArticleDTO()).collect { response ->
                    if (!response.success) {
                        response.msg?.let { //@@@
                            uploadingFinished(it)
                        }
                        setProgressBar(false)
                        return@collect
                    }
                    serviceScope.launch {
                        delay(CALL_CHECK_READY_DELAY)
                        articlesCheckReady(applicationContext, response.articleId)
                    }
                }
            } catch (e: Exception) {
                uploadingFinished(e.message)
            }
        }
    }

    // 잡담 게시판 게시글 등록 시
    private fun insertArticle() {
        serviceScope.launch {
            try {
                insertArticleUseCase(writeArticleModel!!.toInsertArticleDTO()).collect { response ->
                    if (!response.success) {
                        response.msg?.let { //@@@
                            Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                        }
                        setProgressBar(false)
                        return@collect
                    }
                    articlesCheckReady(applicationContext, response.articleId)
                }
            } catch (e: Exception) {
                uploadingFinished(e.message)
            }
        }
    }

    // 게시글 작성 서버에서 완료됐을 때 처리
    private fun articlesCheckReady(context: Context, articleId: Long) {
        val leftProgress = (progressMax - progressCurrent).toFloat()
        var maxCount = 30 // 2초간 최대 30번 호출. 즉 1분
        val leftValue = leftProgress / maxCount.toFloat()

        timer = Timer()
        timer?.schedule(
            object : TimerTask() {
                override fun run() {
                    Handler(Looper.getMainLooper()).post {
                        if (maxCount <= 0) {
                            if (!isRetry) {
                                isRetry = true
                                progressCurrent = 0
                                presignedSuccessCount = 0 // 재업로드 시도해야하므로 SuccessCount 세던 것 초기화
                                setProgressBar(false, UtilK.getLocaleStringResource(locale, R.string.retry_article_upload, applicationContext))
                                startPresignedAndCreate()
                                timer?.cancel()
                            } else {
                                setProgressBar(false, UtilK.getLocaleStringResource(locale, R.string.fail_article_upload, applicationContext))
                                timer?.cancel()
                            }
                        }
                        maxCount--
                        if (progressCurrent < 99) {
                            // 소수점 세팅이 안되서 올림 처리.
                            progressCurrent += ceil(leftValue).toInt()
                            setProgressBar(true)
                        }

                        serviceScope.launch {
                            checkReadyUseCase(articleId).collect { response ->
                                if (!response.success) {
                                    if (response.gcode == ErrorControl.ERROR_3902) { // success false이고, gcode = 3902일 경우 업로드 실패
                                        setProgressBar(false, response.msg)
                                        timer?.cancel()
                                    }
                                    return@collect
                                }

                                val rewardHeart = response.reward
                                val intent = Intent(Const.ARTICLE_SERVICE_UPLOAD)
                                    .apply {
                                        putExtra("reward_heart", rewardHeart)
                                    }
                                LocalBroadcastManager.getInstance(applicationContext)
                                    .sendBroadcast(intent)

                                progressCurrent = progressMax
                                setProgressBar(true)
                                timer?.cancel()
                                return@collect
                            }
                        }
                    }
                }
            },
            0,
            2000,
        )
    }

    private fun startVideoEncode() {

        if (presignedRequestModelList.isNullOrEmpty()) {
            return
        }

        if (presignedRequestModelList?.first()?.videoFile == null) {
            return
        }

        var videoZipCountQuotient = 1  // videoZipCount 80에서 5씩 쌓일때마다 +하며 사용할 몫
        val videoSpecPref = Util.getPreference(applicationContext, Const.PERF_UPLOAD_VIDEO_SPEC)
        val gson = IdolGson.getInstance()
        val videoSpecModel =
            gson.fromJson(videoSpecPref, UploadVideoSpecModel::class.java) ?: UploadVideoSpecModel()

        val originFile = File(presignedRequestModelList?.first()?.videoFile?.relativePath ?: return)
        //인코딩한  비디오 파일을  담을  temp 파일을  만들어냄.
        tempVideoFile = File.createTempFile("idol_", ".mp4", applicationContext.cacheDir)

        val transCoder = TransCoder(
            application = application,
            tempVideoFile = tempVideoFile ?: return,
            originFile = originFile,
            videoSpecModel = videoSpecModel,
            startTimeUs = presignedRequestModelList?.first()?.videoFile?.startTimeMills,
            endTimeUs = presignedRequestModelList?.first()?.videoFile?.endTimeMills,
            onProgress = { progress ->
                Logger.v(
                    "VideoProcessing:: ${(progress * 100)}"
                )
                progressCurrent = (progress * videoZipCount).roundToInt()
                // setProgressBar 계속 호출할 경우 Progress가 안오르는 문제가 있어서 5퍼이상 오를때마다 호출하도록 처리
                if(progressCurrent / 5 >= videoZipCountQuotient){
                    videoZipCountQuotient++
                    setProgressBar(true)
                }
            }, onComplete = {
                //TODO:: 비디오 서버 업로딩.
                val isExceededFileSize = (tempVideoFile?.getSizeMB() ?: 0.0) > videoSpecModel.maxSizeMB

                if (isExceededFileSize) {
                    val fileSizeMax = UtilK.getLocaleStringResource(
                        locale,
                        R.string.file_size_exceeded,
                        applicationContext
                    )
                    finishMsg = String.format(fileSizeMax, videoSpecModel.maxSizeMB)
                    uploadingFinished()
                    return@TransCoder
                }

                presignedRequestModelList?.first()?.apply {
                    byteArray = tempVideoFile?.toByteArray()

                    val fileDimension = tempVideoFile?.getDimensions()
                    srcWidth = fileDimension?.first?.toInt() ?: videoSpecModel.maxWidth
                    srcHeight = fileDimension?.second?.toInt() ?: videoSpecModel.maxHeight

                    hash = tempVideoFile?.getHash()
                }

                startPresignedAndCreate()
                tempVideoFile?.delete()
            }, onFail = {
                uploadingFinished()
                tempVideoFile?.delete()
            }, onCancel = {
                uploadingFinished()
                tempVideoFile?.delete()
            })

        transCoder.startTransCode()
    }

    private fun uploadingFinished(_msg: String? = UtilK.getLocaleStringResource(locale, R.string.fail_article_upload, applicationContext), isSuccessDestroyed: Boolean? = true) {
        notificationManagerCompat?.apply{
            if (ActivityCompat.checkSelfPermission(
                    this@PresignedUrlService,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@apply
            }

            var msg = _msg
            // 업로드 실패시 onDestroy에서 uploadingFinished를 호출하면서 100MB 초과 메시지가 덮어써지는것 방지
            if(finishMsg?.isNotEmpty() == true){
                msg = finishMsg
            }
            builder.setContentText(msg).setProgress(0, 0, false)
            notify(PRESIGNED_NOTIFICATION_ID, builder.build())
        }
        UploadSingleton.clear()
        if(isSuccessDestroyed == true) {
            stopSelf()
        }
    }


    companion object {
        const val TYPE_SMALL_TALK = 0
        const val TYPE_COMMUNITY = 1
        const val TYPE_KIN = 2
        const val TYPE_FREE_BOARD = 3
        const val TYPE_INQUIRY = 4

        const val PRESIGNED_NOTIFICATION_ID = 1

        const val CALL_CHECK_READY_DELAY = 1000L
    }
}