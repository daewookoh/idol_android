package net.ib.mn.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest

/**
 * 이미지 처리 유틸리티
 * old 프로젝트의 ImageUtil.kt를 Compose/Clean Architecture에 맞게 재구현
 */
object ImageUtil {

    /**
     * 이미지 최적화 결과 데이터
     */
    data class OptimizedImage(
        val byteArray: ByteArray,
        val width: Int,
        val height: Int,
        val hash: String,
        val mimeType: String = "image/jpeg"
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as OptimizedImage

            if (!byteArray.contentEquals(other.byteArray)) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (hash != other.hash) return false
            if (mimeType != other.mimeType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = byteArray.contentHashCode()
            result = 31 * result + width
            result = 31 * result + height
            result = 31 * result + hash.hashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }

    /**
     * Uri에서 이미지를 로드하여 최적화 (리사이즈 + 압축)
     *
     * @param context Context
     * @param uri 이미지 Uri
     * @param maxWidth 최대 너비 (기본값: Constants.MAX_IMAGE_WIDTH)
     * @param quality JPEG 압축 품질 (기본값: Constants.IMAGE_QUALITY)
     * @return OptimizedImage 또는 null (실패 시)
     */
    fun optimizeImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = Constants.MAX_IMAGE_WIDTH,
        quality: Int = Constants.IMAGE_QUALITY
    ): OptimizedImage? {
        return try {
            // 1. 원본 이미지 크기 확인
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            var srcWidth = options.outWidth
            var srcHeight = options.outHeight

            if (srcWidth <= 0 || srcHeight <= 0) {
                logE("ImageUtil: Invalid image dimensions: ${srcWidth}x${srcHeight}")
                return null
            }

            // 2. GIF 체크 - GIF는 압축하지 않고 원본 사용
            if (isGif(context, uri)) {
                return processGifImage(context, uri)
            }

            // 3. 리사이즈가 필요한지 확인
            val stream = ByteArrayOutputStream()

            if (srcWidth <= maxWidth) {
                // 원본 크기가 maxWidth 이하면 압축만 수행
                val bitmap = loadBitmapWithRotation(context, uri, options)
                bitmap?.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                bitmap?.recycle()
            } else {
                // 다운샘플링 + 리사이즈
                var desiredWidth = maxWidth
                if (desiredWidth > srcWidth) desiredWidth = srcWidth
                if (desiredWidth <= 1) return null

                var inSampleSize = 1
                while (srcWidth / 2 > desiredWidth) {
                    srcWidth /= 2
                    srcHeight /= 2
                    inSampleSize *= 2
                }

                val desiredScale = desiredWidth.toFloat() / srcWidth

                // 다운샘플링된 비트맵 로드
                val decodeOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inDither = false
                    this.inSampleSize = inSampleSize
                    inScaled = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                } ?: return null

                // EXIF 회전 적용
                val rotatedBitmap = applyExifRotation(context, uri, sampledBitmap)

                // 최종 리사이즈
                val matrix = Matrix()
                matrix.postScale(desiredScale, desiredScale)
                val scaledBitmap = Bitmap.createBitmap(
                    rotatedBitmap,
                    0, 0,
                    rotatedBitmap.width, rotatedBitmap.height,
                    matrix,
                    true
                )

                // 압축
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

                // 메모리 해제
                if (rotatedBitmap != sampledBitmap) {
                    sampledBitmap.recycle()
                }
                if (scaledBitmap != rotatedBitmap) {
                    rotatedBitmap.recycle()
                }
                scaledBitmap.recycle()
            }

            // 4. 결과 생성
            val byteArray = stream.toByteArray()
            val hash = calculateSha256(byteArray)

            // 최종 크기 확인
            val finalOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, finalOptions)

            OptimizedImage(
                byteArray = byteArray,
                width = finalOptions.outWidth,
                height = finalOptions.outHeight,
                hash = hash,
                mimeType = "image/jpeg"
            )
        } catch (e: FileNotFoundException) {
            logE("ImageUtil: File not found: ${uri}")
            null
        } catch (e: IOException) {
            logE("ImageUtil: IO error processing image")
            null
        } catch (e: OutOfMemoryError) {
            logE("ImageUtil: Out of memory processing image")
            null
        } catch (e: Exception) {
            logE("ImageUtil: Error processing image")
            null
        }
    }

    /**
     * GIF 이미지인지 확인
     */
    private fun isGif(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val header = ByteArray(6)
                inputStream.read(header)
                val gif87a = "GIF87a".toByteArray()
                val gif89a = "GIF89a".toByteArray()
                header.contentEquals(gif87a) || header.contentEquals(gif89a)
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * GIF 이미지 처리 (압축 없이 원본 사용)
     */
    private fun processGifImage(context: Context, uri: Uri): OptimizedImage? {
        return try {
            val byteArray = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: return null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)

            val hash = calculateSha256(byteArray)

            OptimizedImage(
                byteArray = byteArray,
                width = options.outWidth,
                height = options.outHeight,
                hash = hash,
                mimeType = "image/gif"
            )
        } catch (e: Exception) {
            logE("ImageUtil: Error processing GIF")
            null
        }
    }

    /**
     * EXIF 회전 정보를 적용하여 비트맵 로드
     */
    private fun loadBitmapWithRotation(
        context: Context,
        uri: Uri,
        options: BitmapFactory.Options
    ): Bitmap? {
        val decodeOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } ?: return null

        return applyExifRotation(context, uri, bitmap)
    }

    /**
     * EXIF 회전 정보 적용
     */
    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val exif = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ExifInterface(inputStream)
            } ?: return bitmap

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            logE("ImageUtil: Error applying EXIF rotation")
            bitmap
        }
    }

    /**
     * SHA-256 해시 계산
     */
    private fun calculateSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return bytesToHex(hashBytes)
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexString = StringBuilder()
        for (b in bytes) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        return hexString.toString()
    }

    /**
     * Uri에서 파일 경로 추출 (업로드용)
     */
    fun getUriPath(context: Context, uri: Uri): String {
        return uri.lastPathSegment?.split(":")?.last()
            ?: System.currentTimeMillis().toString()
    }
}
