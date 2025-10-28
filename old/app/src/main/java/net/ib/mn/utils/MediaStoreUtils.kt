package net.ib.mn.utils

import android.content.Context
import android.content.Intent

object MediaStoreUtils {
    //이미지,움짤 피커
    fun getPickImageIntent(context: Context?): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE) // 파일 탐색 가능한 앱만 표시
        }
        return Intent.createChooser(intent, "Select picture")
    }
    
    //비디오 피커
    fun getPickVideoIntent(context: Context?): Intent {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        return Intent.createChooser(intent, "Select picture")
    }
}
