package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.common.util.appendVersion
import net.ib.mn.databinding.TextureGalleryItemBinding
import net.ib.mn.model.GalleryModel
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Util
import java.text.Format
import java.text.NumberFormat
import java.text.SimpleDateFormat

class GalleryAdapter(context: Context, private val mGlideRequestManager: RequestManager?) :
    ArrayAdapter<GalleryModel?>(context, R.layout.texture_gallery_item) {

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: TextureGalleryItemBinding

        if(convertView == null) {
            binding = TextureGalleryItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as TextureGalleryItemBinding
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    // view가 convertview이므로 binding 꺼내기
    override fun update(view: View?, item: GalleryModel?, position: Int) = with(view?.tag as TextureGalleryItemBinding) {
        val view = view ?: return
        val item = item ?: return

        mPhoto1 = photo1
        mPhoto2 = photo2
        mPhoto3 = photo3

        val rank = if (item.rank == 999)
            "-"
        else String.format(context.getString(R.string.rank_count_format), item.rank.toString())
        textRanking.setText(rank)
        if (item.status.equals(RANKING_NEW, ignoreCase = true)) {
            ranking.setVisibility(View.GONE)
            newRanking.setVisibility(View.VISIBLE)
        } else {
            ranking.setVisibility(View.VISIBLE)
            newRanking.setVisibility(View.GONE)
            if (item.status.equals(RANKING_INCREASE, ignoreCase = true)) {
                iconRanking.setImageResource(R.drawable.icon_change_ranking_up)
            } else if (item.status.equals(RANKING_DECREASE, ignoreCase = true)) {
                iconRanking.setImageResource(R.drawable.icon_change_ranking_down)
            } else {
                iconRanking.setImageResource(R.drawable.icon_change_ranking_no_change)
            }

            changeRanking.setText(item.difference.toString())
        }
        val formatter: Format = SimpleDateFormat("yyyy. MM. dd.", getAppLocale(context))
        val dateString = formatter.format(item.createdAt)

        date.setText(dateString)
        val voteCount = item.heart
        val voteCountComma = NumberFormat.getNumberInstance(getAppLocale(context))
            .format(voteCount)
        val voteCountText = String.format(
            context.getString(R.string.vote_count_format),
            voteCountComma
        )
        count.setText(voteCountText)

        if (item.expanded) rankView.setVisibility(View.VISIBLE)
        else rankView.setVisibility(View.GONE)

        Util.loadGif(mGlideRequestManager, item.imageUrl?.appendVersion(item.imageVer), photo1)
        Util.loadGif(mGlideRequestManager, item.imageUrl2?.appendVersion(item.imageVer), photo2)
        Util.loadGif(mGlideRequestManager, item.imageUrl3?.appendVersion(item.imageVer), photo3)
    }

    companion object {
        private const val RANKING_INCREASE = "increase"
        private const val RANKING_DECREASE = "decrease"
        private const val RANKING_SAME = "same"
        private const val RANKING_NEW = "new"
    }
}

