package net.ib.mn.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.databinding.HeartVoteRankingItemBinding
import net.ib.mn.databinding.TextureGalleryItemBinding
import net.ib.mn.model.UserModel
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Util
import java.text.NumberFormat

class VoteRankingAdater(
    context: Context,
    private val mGlideRequestManager: RequestManager
) : ArrayAdapter<UserModel>(
    context, R.layout.heart_vote_ranking_item
) {
    private var mMyRank: String? = null
    private var mAccount: IdolAccount? = null

    fun addMyRank(rank: String, account: IdolAccount) {
        mMyRank = rank
        mAccount = account
        account.userModel?.let {
            add(it)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: HeartVoteRankingItemBinding

        if(convertView == null) {
            binding = HeartVoteRankingItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as HeartVoteRankingItemBinding
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    override fun update(view: View?, item: UserModel, position: Int): Unit = with(view?.tag as HeartVoteRankingItemBinding) {
        if (position < 3) {
            iconRanking.setVisibility(View.VISIBLE)
            when (position) {
                0 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_1st)
                1 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                2 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
            }
            rank.setTextColor(ContextCompat.getColor(context, R.color.main))
        } else {
            iconRanking.setVisibility(View.GONE)
            rank.setTextColor(ContextCompat.getColor(context, R.color.text_default))
        }
        //		levelIconView.setImageResource(Util.getLevelResId(item.getLevel()));
        level.setImageBitmap(Util.getLevelImage(context, item))

        val format = context.getString(R.string.rank_format)
        val rankCount =
            NumberFormat.getNumberInstance(getAppLocale(context)).format((position + 1).toLong())
        rank.text = String.format(format, rankCount)
        if (mAccount != null && item.email == mAccount!!.email
            && position == count - 1
        ) {
            dividerView.setBackgroundColor(ContextCompat.getColor(context, R.color.main_light))
            if (!TextUtils.isEmpty(mMyRank)) {
                rank.text = String.format(format, mMyRank)
            } else {
                rank.text = "-"
            }
            iconRanking.setVisibility(View.GONE)
            rank.setTextColor(ContextCompat.getColor(context, R.color.text_default))
        } else {
            dividerView.setBackgroundColor(ContextCompat.getColor(context, R.color.gray100))
        }
        name.text = item.nickname
        val voteCountText = NumberFormat.getNumberInstance(getAppLocale(context))
            .format(item.levelHeart)
        val formatText = context.getString(R.string.vote_count_format)
        voteCount.text = String.format(formatText, voteCountText)

        val userId = item.id
        if (item.imageUrl != null) {
            mGlideRequestManager
                .load(item.imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(userId))
                .fallback(Util.noProfileImage(userId))
                .placeholder(Util.noProfileImage(userId))
                .into(photo)
        } else {
            mGlideRequestManager.clear(photo)
            photo.setImageResource(Util.noProfileImage(userId))
        }

        // 이모티콘
        if (item.emoticon != null && item.emoticon?.emojiUrl != null) {
            emoticon.setVisibility(View.VISIBLE)
            mGlideRequestManager
                .load(item.emoticon?.emojiUrl)
                .into(emoticon)
        } else {
            emoticon.setVisibility(View.GONE)
            mGlideRequestManager.clear(emoticon)
        }
    }
}
