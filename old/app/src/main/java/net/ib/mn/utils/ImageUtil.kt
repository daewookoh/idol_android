/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.theartofdev.edmodo.cropper.CropImage
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.model.ConfigModel
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Arrays

class ImageUtil {
    companion object {
        fun cropArticlePhoto(
            activity: BaseActivity,
            uri: Uri,
            isSharedImage: Boolean,
            useSquareImage: Boolean,
            maxSize: Int,
            gifCallback: ((ByteArray) -> Unit)?,
            videoCallback: ((ByteArray) -> Unit)?,
            imgEditorCallback: ((BitmapFactory.Options) -> Unit)?,
        ) {
            if (Const.FEATURE_VIDEO) {
                try {
                    // 헤더가 GIF87a 또는 GIF89a 인지 검사
                    val inputStream = activity.contentResolver.openInputStream(uri)
                    val length = inputStream!!.available()
                    if (length > maxSize) {

                        IdolSnackBar.make(
                            activity.findViewById(android.R.id.content), String.format(
                                activity.resources.getString(R.string.file_size_exceeded),
                                ConfigModel.getInstance(activity).articleMaxSize
                            )
                        ).show()
                        return
                    }
                    val fileData = ByteArray(length)
                    val dataInputStream = DataInputStream(inputStream)
                    dataInputStream.readFully(fileData)
                    dataInputStream.close()
                    val header = Arrays.copyOf(fileData, Math.min(6, length))
                    val gif87a = "GIF87a".toByteArray()
                    val gif89a = "GIF89a".toByteArray()
                    if (Arrays.equals(header, gif87a) || Arrays.equals(
                            header,
                            gif89a,
                        )
                    ) { // gif일 경우
                        gifCallback?.invoke(fileData)
                        return
                    }

                    if (activity.contentResolver.getType(uri)?.contains("video/") == true) {
                        videoCallback?.invoke(fileData)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            retrieveImageSizeOptions(activity, uri, isSharedImage)?.let {
                imgEditorCallback?.invoke(
                    it,
                )
            }
            chooseInternalEditor(activity, uri, useSquareImage){}
        }

        fun openImageEditor(
            activity: BaseActivity,
            receivedUri: Uri,
            isSharedImage: Boolean,
            useSquareImage: Boolean,
            callback: ((BitmapFactory.Options) -> Unit)?,
        ) {
            callback?.invoke(retrieveImageSizeOptions(activity, receivedUri, isSharedImage))

            chooseInternalEditor(activity, receivedUri, useSquareImage){}
        }

        private fun retrieveImageSizeOptions(
            activity: BaseActivity,
            receivedUri: Uri,
            isSharedImage: Boolean,
        ): BitmapFactory.Options {
            // 원본 가로 세로 사이즈 저장
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            if (isSharedImage) {
                try {
                    BitmapFactory.decodeStream(
                        activity.contentResolver.openInputStream(receivedUri),
                        null,
                        options,
                    )
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    Toast.makeText(activity, "Error Launching Cropper", Toast.LENGTH_SHORT).show()
                }
            } else {
                val path = Util.uriToFilePath(activity, receivedUri)
                // content:// 로 시작하는 주소는 path가 null이 된다. 원본 이미지를 올리기 위해서는 originSrcWidth, originSrcHeight가 꼭 있어야 함.
                if (path == null) {
                    try {
                        val ims = activity.contentResolver.openInputStream(receivedUri)
                        // just display image in imageview
                        BitmapFactory.decodeStream(ims, null, options)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                } else {
                    BitmapFactory.decodeFile(path, options)
                }
            }
            return options
        }

        fun chooseInternalEditor(
            activity: BaseActivity,
            receivedUri: Uri,
            useSquareImage: Boolean = true,
            tempFileCallback: ((File) -> Unit)?,
            ) {
            val useInternalEditor =
                Util.getPreferenceBool(activity, Const.PREF_USE_INTERNAL_PHOTO_EDITOR, true)

            if (!useInternalEditor) {
                activity.openLegacyImageEditor(receivedUri, useSquareImage)
                return
            }

            try {
                val mTempFileCrop = try {
                    File.createTempFile("crop", ".png", activity.externalCacheDir)
                } catch (e: IOException) {
                    File.createTempFile("crop", ".png", activity.cacheDir)
                }
                tempFileCallback?.invoke(mTempFileCrop)

                val builder = CropImage.activity(receivedUri)
                    .setAllowFlipping(false)
                    .setAllowRotation(false)
                    .setAllowCounterRotation(false)
                    .setInitialCropWindowPaddingRatio(0f)

                if (useSquareImage) {
                    builder.setAspectRatio(1, 1)
                }
                builder.start(activity)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(activity, "Error Launching Cropper", Toast.LENGTH_SHORT).show()
            }
        }

        fun onArticlePhotoSelected(activity: BaseActivity, uri: Uri, originSrcWidth: Int = 0, originSrcHeight: Int = 0, originSrcUri: Uri? = null, photoSetCallback: ((Bitmap) -> Unit)?, byteArrayCallback: ((ByteArrayOutputStream) -> Unit)?) {
            // Get the source image's dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(uri.path, options)
            var srcWidth = options.outWidth
            var srcHeight = options.outHeight
            val stream = ByteArrayOutputStream()

            if (originSrcWidth == srcWidth && originSrcHeight == srcHeight && srcWidth <= Const.MAX_IMAGE_WIDTH) { // 이게 없어서 엄청 큰 이미지가 올라가고 있었음...
                try {
                    val inputStream = activity.contentResolver.openInputStream(originSrcUri!!)
                    val bufferSize = 1024
                    val buffer = ByteArray(bufferSize)
                    var len = 0
                    while (inputStream!!.read(buffer).also { len = it } != -1) {
                        stream.write(buffer, 0, len)
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                var desiredWidth = Const.MAX_IMAGE_WIDTH
                // Only scale if the source is big enough. This code is just trying to fit a image into a certain width.
                if (desiredWidth > srcWidth) desiredWidth = srcWidth
                // 무한루프 방지용
                if (desiredWidth <= 1) {
                    return
                }
                var inSampleSize = 1
                while (srcWidth / 2 > desiredWidth) { // 여기서 무한루프에 빠지는 경우가 있음
                    srcWidth /= 2
                    srcHeight /= 2
                    inSampleSize *= 2
                }
                val desiredScale = desiredWidth.toFloat() / srcWidth
                // Decode with inSampleSize
                options.inJustDecodeBounds = false
                options.inDither = false
                options.inSampleSize = inSampleSize
                options.inScaled = false
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                val sampledSrcBitmap = BitmapFactory.decodeFile(uri.path, options)
                // Resize
                val matrix = Matrix()
                matrix.postScale(desiredScale, desiredScale)
                val scaledBitmap = Bitmap.createBitmap(sampledSrcBitmap!!, 0, 0, sampledSrcBitmap.width, sampledSrcBitmap.height, matrix, true)
                photoSetCallback?.invoke(scaledBitmap)
                // setImageUri가 안되는 폰이 있음.
                // Save
                scaledBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            }

            byteArrayCallback?.invoke(stream)
        }

        // BlurTransformation이 Glide 먹통현상을 발생시켜 이를 대체하기 위한 메서드
        // Android 12 미만에서만 사용. 12 이상은 RenderEffect를 사용
        fun blur(context: Context, srcBitmap: Bitmap, targetWidth: Int = 0): Bitmap? {
            // RenderScript는 반경이 25 이하로 제한되어 있어 다운스케일 후 블러 처리
            var bitmap: Bitmap? = null
            val rs: RenderScript? = RenderScript.create(context)
            var input: Allocation? = null
            var output: Allocation? = null
            var script: ScriptIntrinsicBlur? = null
            try {
                var width = (srcBitmap.width * 0.2).toInt()
                var height = (srcBitmap.height * 0.2).toInt()
                if(targetWidth > 0) {
                    width = targetWidth
                    height = (srcBitmap.height * width / srcBitmap.width)
                }

                val downscaledBitmap = Bitmap.createScaledBitmap(srcBitmap, width, height, false)
                val config = downscaledBitmap.config ?: Bitmap.Config.ARGB_8888
                bitmap = downscaledBitmap.copy(config, true)

                input = Allocation.createFromBitmap(rs, downscaledBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)
                output = Allocation.createTyped(rs, input.type)

                script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
                script.setRadius(25f)
                script.setInput(input)
                script.forEach(output)
                output.copyTo(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                bitmap = null // exception 발생시 빈 이미지 보이게
            } finally {
                RenderScript.releaseAllContexts()
                input?.destroy()
                output?.destroy()
                script?.destroy()
            }
            return bitmap
        }
    }
}