package net.ib.mn.util

import android.media.MediaMetadataRetriever
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * File 확장 함수 모음
 * Old 프로젝트의 FileExt.kt 참고
 */

/**
 * 파일을 ByteArray로 변환
 */
fun File.toByteArray(): ByteArray? {
    return try {
        FileInputStream(this).use { fis ->
            ByteArrayOutputStream().use { baos ->
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    baos.write(buffer, 0, bytesRead)
                }
                baos.toByteArray()
            }
        }
    } catch (e: IOException) {
        logE("FileExtensions", "toByteArray", e)
        null
    }
}

/**
 * 동영상 파일의 비트레이트 조회
 */
fun File.getBitRate(): Int? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(this.absolutePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toInt()
    } catch (e: Exception) {
        logE("FileExtensions", "getBitRate", e)
        null
    } finally {
        retriever.release()
    }
}

/**
 * 동영상 파일의 해상도(width, height) 조회
 */
fun File.getDimensions(): Pair<Float, Float>? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(this.absolutePath)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloat()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloat()
        if (width != null && height != null) Pair(width, height) else null
    } catch (e: Exception) {
        logE("FileExtensions", "getDimensions", e)
        null
    } finally {
        retriever.release()
    }
}

/**
 * 동영상 파일의 재생 시간(ms) 조회
 */
fun File.getDurationMs(): Long? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(this.absolutePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
    } catch (e: Exception) {
        logE("FileExtensions", "getDurationMs", e)
        null
    } finally {
        retriever.release()
    }
}

/**
 * 파일 크기를 MB로 반환
 */
fun File.getSizeMB(): Double {
    val fileSizeInBytes = this.length()
    val fileSizeInKB = fileSizeInBytes / 1024.0
    return fileSizeInKB / 1024.0
}

/**
 * 파일의 SHA-256 해시값 계산
 * 대용량 파일도 청크 단위로 처리하여 OOM 방지
 */
fun File.getHash(): String {
    val buffer = ByteArray(8192) // 8KB 청크
    val digest = MessageDigest.getInstance("SHA-256")

    FileInputStream(this).use { fis ->
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }

    return digest.digest().joinToString("") { "%02x".format(it) }
}
