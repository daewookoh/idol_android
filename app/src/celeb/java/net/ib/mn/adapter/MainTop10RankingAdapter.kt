package net.ib.mn.adapter

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.fragment.NewRankingFragment2
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import java.util.*
import kotlin.collections.HashMap

/**
 * 셀럽 메인 탑10 adapter
 */

class MainTop10RankingAdapter(
    private val activity: Activity,
    private val context: Context,
    private val fragment: NewRankingFragment2,
    private val mListener: OnClickListener,
    private val glideRequestManager: RequestManager,
    private var mItems: ArrayList<IdolModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnClickListener {
        fun onItemClicked(item: IdolModel?)
    }

    private val voteMap = HashMap<Int, Long>()
    // 투표수 애니메이션 pool
    private val animatorPool = HashMap<Int, ValueAnimator?>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.top10_ranking_item, parent, false)

        return RankViewHolder(view)
    }

    override fun getItemId(position: Int): Long {
        var id = 0L
        if( !mItems.isEmpty() ) {
            if( position == 0 ) {
                // 100000번대 아이돌들이 많이 있어서 안전하게 10000000으로
                id = mItems[position].getId().toLong() + 10000000L
            } else if(mItems.size >= position){
                id = mItems[position].getId().toLong()
            }
        }
        return id
    }


    override fun getItemCount(): Int {
        if( mItems.size == 0 ) {
            Util.log("getItemCount ${mItems.size}")
        }
        return mItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (mItems.isNullOrEmpty()) return

        RankViewHolder(holder.itemView).bind(mItems[position])
    }

    fun clear() {
        mItems.clear()
    }

    fun setItems(@NonNull items: ArrayList<IdolModel>) {
        mItems.clear()
        mItems.addAll(items)
    }



    inner class RankViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val anniversaryView: AppCompatImageView = itemView.findViewById(R.id.iv_anniversary)
        val nameView: AppCompatTextView = itemView.findViewById(R.id.tv_name)
        val rankView: AppCompatTextView = itemView.findViewById(R.id.tv_rank)
        val voteCountView: AppCompatTextView = itemView.findViewById(R.id.tv_heart)
        private val imageView: AppCompatImageView = itemView.findViewById(R.id.photo)
        private val crownView: AppCompatImageView = itemView.findViewById(R.id.iv_crown)

        fun bind(idol: IdolModel) {
            itemView.setOnClickListener {
                mListener.onItemClicked(idol)
            }

            val rank = idol.rank
            nameView.text = Util.nameSplit(context, idol)[0]

            val rankCount = NumberFormat.getNumberInstance(Locale.getDefault()).format(rank+1)
            rankView.text = String.format(
                context.getString(R.string.rank_count_format),
                rankCount)

            val voteCount = idol.heart

            val oldVote : Long = voteMap.get(idol.getId()) ?: 0L

            when (idol.anniversary) {
                Const.ANNIVERSARY_BIRTH -> {
                    anniversaryView.visibility = View.VISIBLE
                    anniversaryView.setImageResource(R.drawable.icon_anniversary_birth)
                }
                Const.ANNIVERSARY_DEBUT -> {
                    anniversaryView.visibility = View.VISIBLE
                    anniversaryView.setImageResource(R.drawable.icon_anniversary_debut)
                }
                Const.ANNIVERSARY_COMEBACK -> {
                    anniversaryView.visibility = View.VISIBLE
                    anniversaryView.setImageResource(R.drawable.icon_anniversary_comeback)
                }
                Const.ANNIVERSARY_ALL_IN_DAY -> {
                    anniversaryView.visibility = View.VISIBLE
                    anniversaryView.setImageResource(R.drawable.icon_anniversary_allinday)
                }
                else -> {
                    anniversaryView.visibility = View.GONE
                }
            }

            // 1등이면 왕관 표시
            if( rank == 0 ) {
                crownView.visibility = View.VISIBLE
            } else {
                crownView.visibility = View.GONE
            }

            // ViewHolder 멤버로 넣었더니 이전 animation이 안가져 와져서 pool에서 꺼내옴
            var animator = animatorPool[itemView.hashCode()]
            animator?.removeAllUpdateListeners()    // 기존 애니메이션 돌던거를 취소하고
            animator?.cancel()
            if( oldVote != voteCount && Util.getPreferenceBool(context, Const.PREF_ANIMATION_MODE, false)) {
                animator = ValueAnimator.ofFloat(0f, 1f)
                // 애니메이터 생성 후 풀에 넣기
                animatorPool.set(itemView.hashCode(), animator)
                animator.addUpdateListener {
                    var value = (oldVote + (voteCount - oldVote) * (it.animatedValue as Float)).toLong()
                    value = if(value > voteCount) voteCount else value
                    val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault()).format(value)
                    voteCountView.text = voteCountComma
                }
                animator?.addListener( object: Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator) {
                    }

                    override fun onAnimationCancel(animation: Animator) {
                    }

                    override fun onAnimationStart(animation: Animator) {
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault()).format(voteCount)
                        voteCountView.text = voteCountComma
                    }
                })
                animator?.duration = 1000
                animator?.start()

            } else {
                val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault()).format(voteCount)
                voteCountView.text = voteCountComma
            }
            voteMap.set(idol.getId(), voteCount)

            val profileThumb: String? = UtilK.top1ImageUrl(context, idol ,Const.IMAGE_SIZE_LOWEST)

            val idolId = idol.getId();
            // 테마픽용 네모난 기본 프로필 적용
            glideRequestManager.load(profileThumb)
                .error(Util.noProfileThemePickImage(idolId))
                .fallback(Util.noProfileThemePickImage(idolId))
                .placeholder(Util.noProfileThemePickImage(idolId))
                .dontAnimate()
                .into(imageView)

        }

    }

}
