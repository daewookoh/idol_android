package net.ib.mn.awards.viewHolder

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.core.model.AwardModel
import net.ib.mn.databinding.AwardsResultHeaderBinding
import net.ib.mn.model.HallModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import java.util.Locale

class AwardFinalTopViewHolder(
    val binding: AwardsResultHeaderBinding,
    val visibleBackground: Boolean = true,
    val glideRequestManager: RequestManager,
    private val awardData: AwardModel?,
    val onItemClicked: (IdolModel?) -> Unit,
    val onFinalResultBottomSheetClick: () -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: HallModel) {
        with(binding) {
            val context = itemView.context

            if (visibleBackground) {
                llAwardBackground.background =
                    ContextCompat.getDrawable(context, R.drawable.img_awards_1st)
            }

            rank.text = String.format(
                context.getString(R.string.rank_format),
                NumberFormat.getNumberInstance(
                    LocaleUtil.getAppLocale(itemView.context),
                ).format(1),
            )

            // 소바, aaa View 다 돌려써서 하트드림 최종결과 색 변경
            name.setTextColor(ContextCompat.getColor(context, R.color.fix_white))
            group.setTextColor(ContextCompat.getColor(context, R.color.fix_white))
            score.setTextColor(ContextCompat.getColor(context, R.color.fix_white))
            rank.setTextColor(ContextCompat.getColor(context, R.color.fix_white))

            val title = awardData?.awardTitle
            val subTitle = itemView.context.getString(R.string.aw_yend2023_result)
            binding.tvAwardTitle.text = String.format("%1s\n%2s", title, subTitle)

            UtilK.setName(context, item.idol, name, group)

            val scoreCount: String =
                NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(item.score)
                    .replace(("[^\\d.]").toRegex(), "")
            val scoreText: String = String.format(
                context.getString(R.string.score_format),
                scoreCount,
            )
            score.text = scoreText

            val idolId = item.idol?.getId() ?: 0

            if (item.id != null) {
                val imageUrl = UtilK.trendImageUrl(context, item.id, item.idol?.sourceApp)
                Util.log("AwardsAggregated:: $imageUrl")
                glideRequestManager
                    .load(imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(idolId))
                    .fallback(Util.noProfileImage(idolId))
                    .placeholder(Util.noProfileImage(idolId))
                    .dontAnimate()
                    .into(photo)
            } else {
                glideRequestManager.clear(photo)
                photo.setImageResource(Util.noProfileImage(idolId))
            }
            setMargins(
                photo,
                0,
                0,
                0,
                Util.convertDpToPixel(context, 23f).toInt(),
            ) // layout 새로 안만들고 기존에서 marginBottom 추가
            if (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE) { // 매우 큰 화면 사이즈(10인치 이상 테블릿). 테블릿에서 유저 이미지가 배경 이미지를 덮는 현상 때문에 추가
                photo.setPadding(
                    Util.convertDpToPixel(context, 50f).toInt(),
                    Util.convertDpToPixel(context, 50f).toInt(),
                    Util.convertDpToPixel(context, 50f).toInt(),
                    Util.convertDpToPixel(context, 50f).toInt(),
                )
                setMargins(photo, 0, 0, 0, Util.convertDpToPixel(context, 40f).toInt())
            }
            itemView.setOnClickListener { onItemClicked(item.idol) }

            inTopAwards.tvTopTitle.text = item.chartName
            inTopAwards.root.visibility = View.GONE

//            val arrayOfView = arrayOf(
//                inTopAwards.clTopAwards,
//                inTopAwards.tvTopTitle,
//                inTopAwards.ivTopArrowDown,
//            )
//            arrayOfView.forEach { view ->
//                view.setOnClickListener {
//                    onFinalResultBottomSheetClick()
//                }
//            }
        }
    }

    // 뷰의 마진을 코드로 설정하기 위한  기능
    private fun setMargins(
        view: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = view.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }
}