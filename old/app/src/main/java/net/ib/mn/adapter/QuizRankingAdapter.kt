package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.databinding.QuizRankingItemBinding
import net.ib.mn.databinding.TextureGalleryItemBinding
import net.ib.mn.model.QuizRankModel
import net.ib.mn.utils.Util

class QuizRankingAdapter(
    context: Context,
    private val mGlideRequestManager: RequestManager
) : ArrayAdapter<QuizRankModel>(
    context, R.layout.quiz_ranking_item
) {
    private var userId = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: QuizRankingItemBinding

        if(convertView == null) {
            binding = QuizRankingItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as QuizRankingItemBinding
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    override fun update(view: View?, item: QuizRankModel, position: Int): Unit = with(view?.tag as QuizRankingItemBinding) {
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

        val user = item.user
        level.setImageBitmap(Util.getLevelImage(context, user))

        val format = context.getString(R.string.rank_format)
        rank.text = String.format(format, (rankValue + 1).toString())
        name.text = user?.nickname
        val formatText = context.getString(R.string.score_format)
        val power: String?
        if (item.power == 0) power = user?.power.toString() + ""
        else power = item.power.toString() + ""
        quizScore.text = String.format(formatText, power)

        userId = user?.id ?: 0

        if (user?.imageUrl != null) {
            mGlideRequestManager
                .load(user.imageUrl)
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
        if (user?.emoticon != null && user.emoticon?.emojiUrl != null) {
            emoticon.setVisibility(View.VISIBLE)
            mGlideRequestManager
                .load(user.emoticon?.emojiUrl)
                .into(emoticon)
        } else {
            emoticon.setVisibility(View.GONE)
            mGlideRequestManager.clear(emoticon)
        }
    }
}
