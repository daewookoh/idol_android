package net.ib.mn.util

import android.content.Context
import android.net.Uri
import android.util.Size
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.common.TrackType
import com.otaliastudios.transcoder.source.ClipDataSource
import com.otaliastudios.transcoder.source.UriDataSource
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.ib.mn.domain.model.UploadVideoSpecModel
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

/**
 * 동영상 처리 유틸리티
 * Old 프로젝트의 TransCoder를 기반으로 세련되게 재작성
 *
 * 주요 기능:
 * - 동영상 인코딩 (압축)
 * - 해상도/비트레이트 제한
 * - 트리밍 지원
 */
object VideoProcessor {

    private const val TAG = "VideoProcessor"
    private const val DEFAULT_BITRATE = 1_000_000  // 1 Mbps
    private const val DEFAULT_WIDTH = 1080f
    private const val DEFAULT_HEIGHT = 1080f

    /**
     * 동영상 처리 결과
     */
    data class ProcessedVideo(
        val file: File,
        val byteArray: ByteArray,
        val width: Int,
        val height: Int,
        val hash: String,
        val sizeMB: Double
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ProcessedVideo
            return file == other.file && byteArray.contentEquals(other.byteArray)
        }

        override fun hashCode(): Int {
            var result = file.hashCode()
            result = 31 * result + byteArray.contentHashCode()
            return result
        }
    }

    /**
     * 동영상 인코딩 및 압축
     *
     * @param context Context
     * @param sourceUri 원본 동영상 URI
     * @param spec 동영상 스펙 설정
     * @param startTimeMs 시작 시간 (ms), null이면 처음부터
     * @param endTimeMs 종료 시간 (ms), null이면 끝까지
     * @param onProgress 진행률 콜백 (0.0 ~ 1.0)
     * @return 처리된 동영상 정보
     */
    suspend fun processVideo(
        context: Context,
        sourceUri: Uri,
        spec: UploadVideoSpecModel = UploadVideoSpecModel(),
        startTimeMs: Long? = null,
        endTimeMs: Long? = null,
        onProgress: ((Double) -> Unit)? = null
    ): Result<ProcessedVideo> = withContext(Dispatchers.IO) {
        runCatching {
            // 임시 파일 생성
            val tempFile = File.createTempFile("video_", ".mp4", context.cacheDir)

            try {
                // 인코딩 수행
                encodeVideo(
                    context = context,
                    sourceUri = sourceUri,
                    outputFile = tempFile,
                    spec = spec,
                    startTimeMs = startTimeMs,
                    endTimeMs = endTimeMs,
                    onProgress = onProgress
                )

                // 파일 크기 확인
                val sizeMB = tempFile.getSizeMB()
                if (sizeMB > spec.maxSizeMB) {
                    tempFile.delete()
                    throw VideoProcessingException("파일 크기가 ${spec.maxSizeMB}MB를 초과했습니다.")
                }

                // 결과 생성
                val dimensions = tempFile.getDimensions()
                val byteArray = tempFile.toByteArray()
                    ?: throw VideoProcessingException("동영상 파일을 읽을 수 없습니다.")

                ProcessedVideo(
                    file = tempFile,
                    byteArray = byteArray,
                    width = dimensions?.first?.toInt() ?: spec.maxWidth,
                    height = dimensions?.second?.toInt() ?: spec.maxHeight,
                    hash = tempFile.getHash(),
                    sizeMB = sizeMB
                )
            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        }
    }

    /**
     * 동영상 인코딩 수행 (suspend)
     */
    private suspend fun encodeVideo(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        spec: UploadVideoSpecModel,
        startTimeMs: Long?,
        endTimeMs: Long?,
        onProgress: ((Double) -> Unit)?
    ) = suspendCancellableCoroutine { continuation ->

        // 원본 파일 정보 가져오기
        val sourceFile = sourceUri.path?.let { File(it) }
        val originDimensions = sourceFile?.getDimensions()
        val originBitrate = sourceFile?.getBitRate()

        // 제한 해상도 계산
        val limitSize = calculateLimitSize(
            originWidth = originDimensions?.first ?: DEFAULT_WIDTH,
            originHeight = originDimensions?.second ?: DEFAULT_HEIGHT,
            maxWidth = spec.maxWidth.toFloat(),
            maxHeight = spec.maxHeight.toFloat()
        )

        // 제한 비트레이트 계산
        val limitBitrate = if ((originBitrate ?: 0) >= spec.maxBitrate) {
            spec.maxBitrate.toLong()
        } else {
            (originBitrate ?: DEFAULT_BITRATE).toLong()
        }

        // 비디오 전략 설정
        val videoStrategy = DefaultVideoStrategy
            .atMost(min(limitSize.width, limitSize.height), max(limitSize.width, limitSize.height))
            .bitRate(limitBitrate)
            .build()

        // 데이터 소스 설정
        val source = UriDataSource(context, sourceUri)
        source.initialize()

        // 트리밍 시간 설정
        val startUs = ((startTimeMs ?: 0) * 1000).coerceAtLeast(0)
        val endUs = ((endTimeMs ?: (source.durationUs / 1000)) * 1000).coerceAtMost(source.durationUs)

        val trimmedSource = ClipDataSource(source, startUs, endUs)

        // Transcoder 빌드 및 실행
        val transcoderFuture = Transcoder.into(outputFile.absolutePath)
            .setVideoTrackStrategy(videoStrategy)
            .addDataSource(TrackType.VIDEO, trimmedSource)
            .addDataSource(TrackType.AUDIO, trimmedSource)
            .setListener(object : TranscoderListener {
                override fun onTranscodeProgress(progress: Double) {
                    onProgress?.invoke(progress)
                }

                override fun onTranscodeCompleted(successCode: Int) {
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                override fun onTranscodeCanceled() {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            VideoProcessingException("동영상 인코딩이 취소되었습니다.")
                        )
                    }
                }

                override fun onTranscodeFailed(exception: Throwable) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            VideoProcessingException("동영상 인코딩에 실패했습니다: ${exception.message}")
                        )
                    }
                }
            })
            .transcode()

        // 취소 처리
        continuation.invokeOnCancellation {
            transcoderFuture.cancel(true)
        }
    }

    /**
     * 제한 해상도 계산
     * 원본 비율을 유지하면서 최대 크기를 넘지 않도록 조정
     */
    private fun calculateLimitSize(
        originWidth: Float,
        originHeight: Float,
        maxWidth: Float,
        maxHeight: Float
    ): Size {
        val originRatio = originWidth / originHeight
        val isWidthDominant = originRatio > 1.0f

        val limit = if (isWidthDominant) maxWidth else maxHeight
        val currentDimension = if (isWidthDominant) originWidth else originHeight

        // 원본이 제한보다 작으면 원본 크기 사용
        if (currentDimension < limit) {
            return Size(originWidth.toInt(), originHeight.toInt())
        }

        // 비율 유지하면서 축소
        val decreaseRatio = limit / currentDimension
        return if (isWidthDominant) {
            Size(limit.toInt(), (originHeight * decreaseRatio).toInt())
        } else {
            Size((originWidth * decreaseRatio).toInt(), limit.toInt())
        }
    }

    /**
     * 동영상 처리 예외
     */
    class VideoProcessingException(message: String) : Exception(message)
}
