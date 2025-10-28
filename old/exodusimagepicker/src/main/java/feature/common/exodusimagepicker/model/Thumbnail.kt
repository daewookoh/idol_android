package feature.common.exodusimagepicker.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Thumbnail(
    val positionUs: Long,
    val bitmap: Bitmap? = null,
) : Parcelable