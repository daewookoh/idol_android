package feature.common.exodusimagepicker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import feature.common.exodusimagepicker.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description:  file 관련 확장함수 기능들 모음
 *
 * @see pathToBitmap ->  path를   bitmap으로 반환한다.
 * @see BitmapToByteArray ->  bitmap을 byte array로 반환한다.
 * @see getResizedBitmap ->  현재 이미지뷰의  이미지를  bitmap으로  받아서 반환한다.
 * @see byteArrayToBitmap ->  byte array를 다시 비트맵으로 변경해준다.
 * */

// 비트맵  바이트 어레이로 변경
fun Bitmap.BitmapToByteArray(): ByteArray? {
    val baos = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    return baos.toByteArray()
}

// 임시 파일  나온것  지우기 위한 로직
fun File.delete(context: Context): Boolean {
    try {
        var selectionArgs = arrayOf(this.absolutePath)
        val contentResolver = context.contentResolver
        val where: String?
        val filesUri: Uri?
        if (DeviceVersionUtil.isAndroid10Later()) {
            filesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            where = MediaStore.Images.Media._ID + "=?"
            selectionArgs = arrayOf(this.name)
        } else {
            where = MediaStore.MediaColumns.DATA + "=?"
            filesUri = MediaStore.Files.getContentUri("external")
        }
        contentResolver.delete(filesUri!!, where, selectionArgs)
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }

    return !this.exists()
}

// 비트맵 테두리 처리
fun Bitmap.addWhiteBorderOnBitmap(borderSize: Int, context: Context): Bitmap? {
    val config = this.config ?: Bitmap.Config.ARGB_8888
    val bmpWithBorder =
        Bitmap.createBitmap(this.width + borderSize * 2, this.height + borderSize * 2, config)
    val canvas = Canvas(bmpWithBorder)
    canvas.drawColor(ContextCompat.getColor(context, R.color.gray110))
    canvas.drawBitmap(this, borderSize.toFloat(), borderSize.toFloat(), null)
    return bmpWithBorder.getRoundedCornerBitmap(15)
}

// 비트맵 테두리 둥근 처리
fun Bitmap.getRoundedCornerBitmap(px: Int): Bitmap? {
    val output = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val color = -0xbdbdbe
    val paint = Paint()
    val rect = Rect(0, 0, this.width, this.height)
    val rectF = RectF(rect)
    val roundPx = px.toFloat()
    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = color
    canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, rect, rect, paint)
    return output
}

fun Bitmap.saveImageCacheDirectory(context: Context): Uri? {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "FavoriteIdol_$timeStamp.jpg"

        val cacheDir = context.cacheDir
        val imageFile = File(cacheDir, fileName)

        val outputStream: OutputStream = FileOutputStream(imageFile)
        this.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        return Uri.parse(imageFile.absolutePath)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

// byte array를 다시 비트맵으로 변경해준다.
fun ByteArray.byteArrayToBitmap(): Bitmap {
    return BitmapFactory.decodeByteArray(this, 0, this.size)
}

// 현재 이미지뷰의  이미지를  bitmap으로  받아서 반환한다.
fun View.getBitmap(): Bitmap? {
    return try {
        this.drawToBitmap()
    } catch (e: Exception) { // 혹시  bitmap 변경중  문제가 생겼으면,  null return 해줌.
        null
    }
}