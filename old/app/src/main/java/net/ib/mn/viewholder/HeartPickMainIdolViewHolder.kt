package net.ib.mn.viewholder

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.ItemHeartPickIdolBinding
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.model.HeartPickModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

class HeartPickMainIdolViewHolder(
    val binding: ItemHeartPickIdolBinding,
    private val totalVote: Int,
    val lifecycleScope: LifecycleCoroutineScope,
) : RecyclerView.ViewHolder(binding.root) {
    private lateinit var mGlideRequestManager: RequestManager
    private val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))

    fun bind(heartPickIdol: HeartPickIdol?, position: Int) = with(binding) {
        heartPickIdol?:return@with
        mGlideRequestManager = Glide.with(itemView.context)

        if(position == 0) {
            clRoot.setPadding(Util.convertDpToPixel(itemView.context, 16f).toInt(),0,Util.convertDpToPixel(itemView.context, 16f).toInt(),0)
        } else {
            clRoot.setPadding(0,0,Util.convertDpToPixel(itemView.context, 16f).toInt(), 0)
        }
        tvRank.text = numberFormat.format(heartPickIdol.rank)
        mGlideRequestManager.load(heartPickIdol.image_url)
            .centerCrop()
            .apply(RequestOptions.circleCropTransform())
            .error(Util.noProfileImage(heartPickIdol.idol_id))
            .fallback(Util.noProfileImage(heartPickIdol.idol_id))
            .placeholder(Util.noProfileImage(heartPickIdol.idol_id))
            .dontAnimate()
            .into(ivPhoto)

        tvName.text = heartPickIdol.title
        tvGroup.text = heartPickIdol.subtitle

        setVote(heartPickIdol)
    }

    private fun setVote(heartPickIdol: HeartPickIdol?)= with(binding) {
        heartPickIdol?:return@with

        tvVote.text = numberFormat.format(heartPickIdol.vote)

        if(totalVote == 0) {
            tvVotePercent.text = numberFormat.format(0).plus("%")
            return@with
        }
        val votePercent: Float = 100.0f * heartPickIdol.vote.toFloat() / totalVote.toFloat()    // 분모가 0일 경우 앱이 죽기때문에 위에서 예외 처리
        tvVotePercent.text = numberFormat.format(votePercent.roundToInt()).plus("%")
    }
}