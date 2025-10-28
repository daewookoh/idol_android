package net.ib.mn.utils

import android.graphics.drawable.PictureDrawable
import android.net.Uri
import androidx.appcompat.widget.AppCompatImageView
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import androidx.core.net.toUri

object SvgLoaderForJava {
    // 메인 스레드에서 작업을 수행하는 헬퍼 메서드
    private fun setImageOnMainThread(imageView: AppCompatImageView, drawable: PictureDrawable) {
        imageView.post { imageView.setImageDrawable(drawable) }
    }

    fun loadSvgImage(imageView: AppCompatImageView, imageUrl: String?) {
        // 백그라운드 작업을 위한 스레드 풀 사용
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            try {
                val uri = imageUrl?.toUri()
                val url = URL(uri.toString())
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val inputStream = connection.inputStream

                // SVG를 파싱하고 PictureDrawable로 변환
                var svg: SVG? = null
                try {
                    svg = SVG.getFromInputStream(inputStream)
                } catch (e: SVGParseException) {
                    throw RuntimeException(e)
                }
                val pictureDrawable = PictureDrawable(svg.renderToPicture())

                // 메인 스레드에서 이미지 설정
                setImageOnMainThread(imageView, pictureDrawable)

                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
