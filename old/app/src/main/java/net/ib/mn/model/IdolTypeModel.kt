package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class IdolTypeModel(
    @SerializedName("idx") var idx:Int=0,
    @SerializedName("type") var type: String? = "",
    @SerializedName("is_divided") var is_divided: String? = "D",    //8.4.0에서 업데이트시 실제 사용하는 Y/N이 아닌 D로 들어오게 하기 위함
    @SerializedName("isFemale") var isFemale : Boolean = false,
    @SerializedName("most") var most: Boolean= false,
    @SerializedName("checkFilter") var checkFilter : Boolean = false,
    @SerializedName("is_viewable") var is_viewable: String = ""
)