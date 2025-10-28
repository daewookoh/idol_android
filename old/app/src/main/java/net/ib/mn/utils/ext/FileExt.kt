/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.utils.ext

import android.media.MediaMetadataRetriever
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * @see toByteArray 해당 파일을 byte로 바꾼다.
 * */

fun File.toByteArray(): ByteArray? {
    try {
        FileInputStream(this).use { fileInputStream ->
            ByteArrayOutputStream().use { byteArrayOutputStream ->
                var bytesRead: Int
                val buffer = ByteArray(1024)

                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead)
                }

                return byteArrayOutputStream.toByteArray()
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}

fun File.getBitRate(): Int? {
    val retriever = MediaMetadataRetriever()

    try {
        retriever.setDataSource(this.absolutePath)

        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

        return bitrate?.toInt()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        retriever.release()
    }

    return null
}

fun File.getDimensions(): Pair<Float, Float>? {
    val retriever = MediaMetadataRetriever()

    try {
        retriever.setDataSource(this.absolutePath)

        val width =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloat()
        val height =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloat()

        if (width != null && height != null) {
            return Pair(width, height)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        retriever.release()
    }

    return null
}

fun String.getDuration(): Long? {
    val retriever = MediaMetadataRetriever()

    try {
        retriever.setDataSource(this, hashMapOf<String, String>())
        return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            ?: 0
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        retriever.release()
    }

    return null
}

fun File.getFrameRate(): Float? {
    val retriever = MediaMetadataRetriever()

    try {
        retriever.setDataSource(this.absolutePath)
        return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            ?.toFloat() ?: 30.0f
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        retriever.release()
    }

    return null
}

fun File.getSizeMB() : Double {
    val fileSizeInBytes = this.length()
    val fileSizeInKB = fileSizeInBytes / 1024.0
    return fileSizeInKB / 1024.0
}

// 파일의 해시값을 chunk 단위로 구하는 함수 (전체 파일을 읽으면 OOM 발생)
fun File.getHash(): String {
    val buffer = ByteArray(8192) // 8KB 청크 크기
    val digest = MessageDigest.getInstance("SHA-256")

    FileInputStream(this).use { fis ->
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }

    // 해시값을 Hex 문자열로 변환
    return digest.digest().joinToString("") { "%02x".format(it) }
}

