package feature.common.exodusimagepicker.util

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import java.util.concurrent.TimeUnit

// 이미지로드용
@BindingAdapter("bind:loadImage", "bind:loadError")
fun ImageView.loadImage(imageResource: Any?, errorImageResource: Any?) {
    Glide.with(context)
        .load(imageResource)
        .error(errorImageResource)
        .into(this)
}

// 텍스트뷰에  timemills 를  00:00 포맷으로 바꿔줌.
fun TextView.convertTimeMillsToTimerFormat(duration: Long) {
    this.text = String.format(
        "%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(duration),
        TimeUnit.MILLISECONDS.toSeconds(duration) -
            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)),
    )
}

// constraint group 차일드뷰들  클릭 전부 클릭 동작 받음.
fun Group.setAllOnClickListener(listener: View.OnClickListener?) {
    referencedIds.forEach { id ->
        rootView.findViewById<View>(id).setOnClickListener(listener)
    }
}