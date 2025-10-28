package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.databinding.AggregatedHofItemBinding
import net.ib.mn.databinding.TextureGalleryItemBinding
import net.ib.mn.model.HallModel
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.charityImageUrl
import net.ib.mn.utils.UtilK.Companion.setName
import java.text.NumberFormat

class CharityCountAdapter(
    context: Context,
    private val mGlideRequestManager: RequestManager
) : ArrayAdapter<HallModel?>(
    context, R.layout.aggregated_hof_item
) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: AggregatedHofItemBinding

        if(convertView == null) {
            binding = AggregatedHofItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as AggregatedHofItemBinding
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    override fun update(view: View?, item: HallModel?, position: Int): Unit = with(view?.tag as AggregatedHofItemBinding) {
        val view = view ?: return
        val item = item ?: return

        ivArrowGo.setVisibility(View.GONE)

        // 동점자 처리
        val rankValue = item.rank // rank는 0부터
        if (rankValue < 3) {
            iconRanking.setVisibility(View.VISIBLE)


            when (rankValue) {
                0 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_1st)
                1 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                2 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
            }
            rank.setTextColor(ContextCompat.getColor(context, R.color.main))
        } else {
            iconRanking.setVisibility(View.GONE)
            rank.setTextColor(ContextCompat.getColor(context, R.color.text_default))
        }

        setName(context, item, name, group)

        val voteCountText = NumberFormat.getNumberInstance(getAppLocale(context))
            .format(item.count.toLong())
        val scoreText = String.format(
            context.getString(R.string.number_of_times), voteCountText
        )
        score.setText(scoreText)
        val rankCount =
            NumberFormat.getNumberInstance(getAppLocale(context)).format((rankValue + 1).toLong())
        rank.setText(
            String.format(
                context.getString(R.string.rank_format),
                rankCount
            )
        )
        val idolId = item.id
        if (item.imageUrl != null) {
            val imageUrl = charityImageUrl(context, item.id)
            Util.log("CharityCountAdapter:: " + imageUrl)
            mGlideRequestManager
                .load(item.imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .into(photo)
        } else {
            mGlideRequestManager.clear(photo)
            photo.setImageResource(Util.noProfileImage(idolId))
        }
    }
}
