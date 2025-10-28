/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 캐시 디렉토리에 있는 파일에 인코딩한 비디오를 넣어줍니다.
 *
 * */

package net.ib.mn.utils.encode

import android.app.Application
import android.net.Uri
import android.util.Size
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.TranscoderOptions
import com.otaliastudios.transcoder.common.TrackType
import com.otaliastudios.transcoder.source.ClipDataSource
import com.otaliastudios.transcoder.source.DefaultDataSource
import com.otaliastudios.transcoder.source.UriDataSource
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import net.ib.mn.core.model.UploadVideoSpecModel
import net.ib.mn.utils.Logger
import net.ib.mn.utils.ext.getBitRate
import net.ib.mn.utils.ext.getDimensions
import java.io.File
import java.util.concurrent.Future

/**
 * @see
 * */

class TransCoder(
    private val application: Application,
    private val tempVideoFile: File,
    private val originFile: File,
    private val videoSpecModel: UploadVideoSpecModel,
    private var startTimeUs: Int?,
    private var endTimeUs: Int?,
    private val onProgress: (Double) -> Unit,
    private val onComplete: () -> Unit = {},
    private val onCancel: () -> Unit = {},
    private val onFail: () -> Unit = {},
) : TranscoderListener {

    private var transcoder: TranscoderOptions.Builder
    private lateinit var transCoderFuture: Future<Void>

    init {

        val limitWidthAndHeight = getLimitWidthAndHeight()
        val limitWidth = limitWidthAndHeight.first
        val limitHeight = limitWidthAndHeight.second
        val limitSize = Size(limitWidth.toInt(), limitHeight.toInt())

        val maxValue = Math.max(limitSize.width, limitSize.height)
        val minValue = Math.min(limitSize.width, limitSize.height)

        val videoStrategy =
            DefaultVideoStrategy
                .atMost(minValue, maxValue)
                .bitRate(getLimitBitRate())
                .build()

        transcoder = Transcoder.into(tempVideoFile.absolutePath)
            .setVideoTrackStrategy(
                videoStrategy,
            )

        setDataSource()
        setListener()
    }

    private fun setDataSource() {
        // 원본 데이터 소스
        val source: DefaultDataSource = UriDataSource(
            application,
            Uri.parse(originFile.absolutePath),
        )
        source.initialize()

        // 시작 trim 시간  마이너스면  0으로  넣어줌.
        if ((startTimeUs ?: -1) < 0) {
            startTimeUs = 0
        }

        // 원본데이터 소스 duration보다 trim된 last 포지션이 높으면
        // 문제가 생기므로,  그경우에는  원본데이터의 duration을  마지막 last 포지션에  적용해준다.
        if ((
            source.durationUs < (
                (
                    endTimeUs
                        ?: 0
                    )
                )
            )
        ) {
            endTimeUs = source.durationUs.toInt()
        }

        val trimmedDataSource = ClipDataSource(
            source,
            (startTimeUs ?: -1).toLong(),
            endTimeUs?.toLong() ?: 0L,
        )

        transcoder.addDataSource(TrackType.VIDEO, trimmedDataSource)
        transcoder.addDataSource(TrackType.AUDIO, trimmedDataSource) // 안넣어주면  오디오 안나옴.
    }

    fun setListener() {
        transcoder.setListener(this)
    }

    fun startTransCode() {
        transCoderFuture = transcoder.transcode()
    }

    private fun getLimitBitRate(): Long =
        if ((originFile.getBitRate() ?: 0) >= videoSpecModel.maxBitrate) {
            videoSpecModel.maxBitrate.toLong()
        } else {
            (originFile.getBitRate() ?: DEFAULT_BITRATE).toLong()
        }

    private fun getLimitWidthAndHeight(): Pair<Float, Float> {
        val originFileDimension = originFile.getDimensions()

        val originWidth = originFileDimension?.first ?: DEFAULT_WIDTH
        val originHeight = originFileDimension?.second ?: DEFAULT_HEIGHT

        val originRatio = (originWidth / originHeight)
        val isWidthDominant = originRatio > 1.0f

        //가로, 세로중 가장 긴 값을 가져 옵니다.
        val limit = if (isWidthDominant) {
            videoSpecModel.maxWidth.toFloat()
        } else {
            videoSpecModel.maxHeight.toFloat()
        }

        val currentDimension = if (isWidthDominant) {
            originWidth
        } else {
            originHeight
        }

        //서버 값보다 작으면 원본 값을 줍니다.
        if (currentDimension < limit) {
            return Pair(originWidth, originHeight)
        }

        //그렇지 않으면 서버값 보다 더 큰 경우 원본 값을 줄여야 되므로 비율 계산 후 줄여 줍니다.
        val decreaseRatio = limit / currentDimension

        return if (isWidthDominant) {
            Pair(limit, originHeight * decreaseRatio)
        } else {
            Pair(originWidth * decreaseRatio, limit)
        }

    }

    override fun onTranscodeProgress(progress: Double) {
        this.onProgress(progress)
    }

    override fun onTranscodeCompleted(successCode: Int) {
        this.onComplete()
    }

    override fun onTranscodeCanceled() {
        this.onCancel
    }

    override fun onTranscodeFailed(exception: Throwable) {
        this.onFail
    }

    companion object {
        const val DEFAULT_BITRATE = 1000000
        const val DEFAULT_WIDTH = 1080f
        const val DEFAULT_HEIGHT = 1080f
    }
}