package net.ib.mn.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

@Parcelize
data class EmoticonModel(
    var id: Int,
    var title: String,
    var emojiUrl: String,
    var isViewable: String
) : Serializable, Parcelable {

    // JSONObject로부터 객체를 생성하는 기존 로직을 보조 생성자로 유지합니다.
    constructor(jsonObject: JSONObject?) : this(
        id = jsonObject?.optInt("id") ?: 0,
        title = jsonObject?.optString("title") ?: "",
        emojiUrl = jsonObject?.optString("emoji_url") ?: "",
        isViewable = jsonObject?.optString("is_viewable") ?: ""
    )

    /**
     * Returns all the available property values in the form of JSONObject instance where the key is the approperiate json key and the value is the value of the corresponding field
     */
    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("emoji_url", emojiUrl)
            jsonObject.put("id", id)
            jsonObject.put("is_viewable", isViewable)
            jsonObject.put("title", title)
        } catch (e: JSONException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return jsonObject
    }
}