package net.ib.mn.viewholder

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.adapter.HeartPickAdapter
import net.ib.mn.databinding.ItemHeartPick1stBinding
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.model.HeartPickModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.setOnSingleClickListener
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

class HeartPick1stViewHolder (
    val binding: ItemHeartPick1stBinding,
    val heartPickListener: HeartPickAdapter.HeartPickListener,
    val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.ViewHolder(binding.root) {
    private lateinit var mGlideRequestManager: RequestManager
    private val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))

    fun bind(heartPickModel : HeartPickModel?) = with(binding) {
        if(heartPickModel == null) return@with
        val heartPickIdol = heartPickModel.heartPickIdols?.get(0) ?: return@with

        mGlideRequestManager = Glide.with(itemView.context)

        mGlideRequestManager.load(heartPickIdol.image_url)
            .centerCrop()
            .apply(RequestOptions.circleCropTransform())
            .error(Util.noProfileImage(heartPickIdol.idol_id))
            .fallback(Util.noProfileImage(heartPickIdol.idol_id))
            .placeholder(Util.noProfileImage(heartPickIdol.idol_id))
            .dontAnimate()
            .into(ivPhoto)

        inRankingPickNameAndGroup.setRecompose(heartPickModel.type != "I")
        inRankingPickNameAndGroup.setNameAndGroupForNoIdol(
            idolName = heartPickIdol.title,
            idolGroup = heartPickIdol.subtitle,
            nameMaxLine = 2,
            groupMaxLine = 1
        )
        setBtnHeart(heartPickModel.status)
        setVote(heartPickModel, heartPickIdol)

        itemView.setOnClickListener {
            heartPickListener.goCommunity(heartPickIdol)
        }
    }

    private fun setBtnHeart(status: Int) = with(binding) {
        val params = clRankingHeart.layoutParams as ConstraintLayout.LayoutParams
        if(status == 2) {
            btnHeart.visibility = View.GONE
            params.marginEnd = Util.convertDpToPixel(itemView.context, 26f).toInt()
        } else {
            btnHeart.visibility = View.VISIBLE
            params.marginEnd = Util.convertDpToPixel(itemView.context, 10f).toInt()
        }
    }

    private fun setVote(heartPickModel: HeartPickModel?, heartPickIdol: HeartPickIdol?)= with(binding) {
        heartPickModel ?: return@with
        heartPickIdol ?: return@with

        btnHeart.setOnSingleClickListener {
            heartPickListener.onVote(heartPickIdol, heartPickModel.id)
        }

        tvVote.text = numberFormat.format(heartPickIdol.vote)

        if(heartPickModel.vote == 0) {
            tvVotePercent.text = numberFormat.format(0).plus("%")
            return@with
        }
        val votePercent: Double =
            100.0 * heartPickIdol.vote.toDouble() / heartPickModel.vote.toDouble()  // 분모가 0일 경우 앱이 죽기때문에 위에서 예외 처리
        tvVotePercent.text = numberFormat.format(votePercent.roundToInt()).plus("%")
    }
}