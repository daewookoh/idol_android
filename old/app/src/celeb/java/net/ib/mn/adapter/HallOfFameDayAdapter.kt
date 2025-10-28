package net.ib.mn.adapter

import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.*
import android.widget.BaseAdapter
import android.widget.RelativeLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.databinding.DataBindingUtil
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.HallItemBinding
import net.ib.mn.fragment.HallOfFameDayFragment
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.HallModel
import net.ib.mn.utils.Const
import net.ib.mn.BuildConfig
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.setMargins
import java.text.DateFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.exp

/**
 * Copyright 2023-01-12,목,15:26. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 개인/그룹 일일 Adapter
 *
 **/
class HallOfFameDayAdapter(
    private val context: Context,
    private val mListener: OnClickListener,
    private val fragment : HallOfFameDayFragment,
    private var mItems: MutableList<HallModel>,
    private val mGlideRequestManager: RequestManager
) : BaseRankingAdapter() {

    private var mapExpanded = HashMap<Int, Boolean>()

    interface OnClickListener {
        fun onPhotoClicked(item: HallModel?, position: Int)
        fun onItemClicked(item : HallModel)
    }

    @UnstableApi
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return RankViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.hall_item, parent, false))

    }

    override fun getItemId(position: Int): Long {
        var id = 0L
        if(mItems.isNotEmpty()) {
            if( position == 0 ) {
                id = mItems[position].id.toLong() + 100000L
            } else if(mItems.size >= position){
                id = mItems[position-1].id.toLong()
            }
        }

        return id
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int = mItems.size

    @UnstableApi
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (mItems.isNullOrEmpty()) return
        else {
            (holder as RankViewHolder).apply {
                bind(mItems[position], position)

                if (!BuildConfig.CELEB) {
                    val margin = if (position == mItems.size - 1) {
                        9f
                    } else {
                        0f
                    }

                    holder.itemView.setMargins(bottom = margin)
                }
            }
        }
    }

    fun setItems(items: MutableList<HallModel>) {
        mItems = mutableListOf()
        mItems.addAll(items)
        bannerClear()
        notifyDataSetChanged()
    }

    //불러온 데이터 없을 경우 사용
    fun clear(){
        mItems = mutableListOf()
        notifyDataSetChanged()
    }
    //펼쳐져있는 배너 닫기
    fun bannerClear() {
        fragment.mapExpanded.clear()
    }

    @UnstableApi
    inner class RankViewHolder(val binding: HallItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hallModel: HallModel, position: Int) {

            // 명예전당은 몰빵일이 안보여줌
            when (hallModel.idol?.anniversary) {
                Const.ANNIVERSARY_BIRTH -> {
                    if (BuildConfig.CELEB) {
                        binding.ivBadgeBirth.visibility = View.VISIBLE
                        binding.ivBadgeDebut.visibility = View.GONE
                    } else {
                        if (hallModel.idol?.type == "S") {
                            binding.ivBadgeBirth.visibility = View.VISIBLE
                            binding.ivBadgeDebut.visibility = View.GONE
                        } else {
                            // 그룹은 데뷔뱃지로 보여주기
                            binding.ivBadgeBirth.visibility = View.GONE
                            binding.ivBadgeDebut.visibility = View.VISIBLE
                        }
                    }
                    binding.ivBadgeComeback.visibility = View.GONE
                    binding.tvBadgeMemorialDay.visibility = View.GONE
                }
                Const.ANNIVERSARY_DEBUT -> {
                    binding.ivBadgeBirth.visibility = View.GONE
                    binding.ivBadgeDebut.visibility = View.VISIBLE
                    binding.ivBadgeComeback.visibility = View.GONE
                    binding.tvBadgeMemorialDay.visibility = View.GONE
                }
                Const.ANNIVERSARY_COMEBACK -> {
                    binding.ivBadgeBirth.visibility = View.GONE
                    binding.ivBadgeDebut.visibility = View.GONE
                    binding.ivBadgeComeback.visibility = View.VISIBLE
                    binding.tvBadgeMemorialDay.visibility = View.GONE
                }
                Const.ANNIVERSARY_MEMORIAL_DAY -> {
                    binding.ivBadgeBirth.visibility = View.GONE
                    binding.ivBadgeDebut.visibility = View.GONE
                    binding.ivBadgeComeback.visibility = View.GONE
                    binding.tvBadgeMemorialDay.visibility = View.VISIBLE
                    if (Util.isRTL(context)) {
                        val badgeCount: String = NumberFormat.getNumberInstance(Locale.getDefault())
                            .format(hallModel.idol?.anniversaryDays)
                        val memorialDayText = badgeCount.replace(",", "")
                        binding.tvBadgeMemorialDay.text = memorialDayText + context.getString(R.string.lable_day)
                    } else {
                        var memorialDayText: String = hallModel.idol?.anniversaryDays.toString() + context.getString(R.string.lable_day)
                        memorialDayText = memorialDayText.replace(",", "")
                        binding.tvBadgeMemorialDay.text = memorialDayText
                    }
                }
                else -> {
                    binding.ivBadgeBirth.visibility = View.GONE
                    binding.ivBadgeDebut.visibility = View.GONE
                    binding.ivBadgeComeback.visibility = View.GONE
                    binding.tvBadgeMemorialDay.visibility = View.GONE
                }
            }

            val imageUrl = ConfigModel.getInstance(context).cdnUrl + "/h/" + hallModel.getResourceId() + ".1_" + Const.IMAGE_SIZE_LOWEST + ".webp"
            val idolId: Int = hallModel.idol?.getId() ?: 0
            mGlideRequestManager
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .dontAnimate()
                .into(binding.ivPhoto)

            UtilK.setName(context, hallModel.idol, binding.tvName, binding.tvGroup)

            val f = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            // 명예전당은 항상 KST로 보여야 하므로
            f.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            val date = f.format(hallModel.createdAt)
            binding.tvDate.text = date
            val voteCount: Long = hallModel.heart.toLong()
            val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault())
                .format(voteCount)
            val voteCountText: String = kotlin.String.format(
                context.getString(R.string.vote_count_format),
                voteCountComma
            )
            binding.tvCount.text = voteCountText

            // 동점자 처리
            val rank: Int = hallModel.rank // rank는 0부터

            if (rank < 3) {
                binding.ivRankIcon.visibility = View.VISIBLE
                when (hallModel.rank) {
                    0 -> {
                        binding.ivRankIcon.setImageResource(R.drawable.icon_rating_heart_voting_1st)
                    }
                    1 -> {
                        binding.ivRankIcon.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                    }
                    else -> {
                        binding.ivRankIcon.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
                    }
                }
            } else {
                binding.ivRankIcon.visibility = View.GONE
            }

            binding.llContainer.setOnClickListener {
                mListener.onItemClicked(hallModel)
            }

            if (fragment.mapExpanded[position] == true) {
                binding.clPhotos.visibility = View.VISIBLE
            } else {
                binding.clPhotos.visibility = View.GONE
            }

            binding.ivPhoto.isClickable = true
            binding.ivPhoto.setOnClickListener {
                mListener.onPhotoClicked(hallModel, position)

                if(binding.clPhotos.visibility == View.VISIBLE) {
                    collapse(binding.clPhotos)
                    fragment.mapExpanded[position] = false
                    // 이전에 펼쳐진 것들 처리
                    fragment.stopExoPlayer(fragment.playerView1)
                    fragment.stopExoPlayer(fragment.playerView2)
                    fragment.stopExoPlayer(fragment.playerView3)
                } else {
                    mGlideRequestManager.load(hallModel.imageUrl).into(binding.photo1)
                    mGlideRequestManager.load(hallModel.imageUrl2).into(binding.photo2)
                    mGlideRequestManager.load(hallModel.imageUrl3).into(binding.photo3)

                    expand(binding.clPhotos) {
                        fragment.playExoPlayer(0,
                            itemView.findViewById(R.id.playerview1),
                            itemView.findViewById(R.id.photo1),
                            hallModel.imageUrl)
                        fragment.playExoPlayer(1,
                            itemView.findViewById(R.id.playerview2),
                            itemView.findViewById(R.id.photo2),
                            hallModel.imageUrl2)
                        fragment.playExoPlayer(2,
                            itemView.findViewById(R.id.playerview3),
                            itemView.findViewById(R.id.photo3),
                            hallModel.imageUrl3)
                    }
                    fragment.mapExpanded[position] = true
                }

                // 이전에 펼쳐진 것들 처리
                fragment.stopExoPlayer(fragment.playerView1)
                fragment.stopExoPlayer(fragment.playerView2)
                fragment.stopExoPlayer(fragment.playerView3)
            }

            val expanded = if (fragment.mapExpanded[position] == null) false else fragment.mapExpanded[position]!!

            showExpanded(position, expanded, hallModel)
        }

        private fun showExpanded(position: Int, expanded: Boolean, item: HallModel) {
            val ll = binding.clPhotos.layoutParams as RelativeLayout.LayoutParams
            val toHeight = binding.clPhotos.width / 3
            if (expanded) {
                //            ll.height = mContainerPhotos.getWidth() / 3;
                val photos = arrayOf<View>(binding.photo1, binding.photo2, binding.photo3)
                for (p in photos) {
                    val lp = p.layoutParams as RelativeLayout.LayoutParams
                    lp.height = toHeight
                    p.layoutParams = lp
                }

                Util.loadGif(mGlideRequestManager, item.imageUrl, binding.photo1)
                Util.loadGif(mGlideRequestManager, item.imageUrl2, binding.photo2)
                Util.loadGif(mGlideRequestManager, item.imageUrl3, binding.photo3)

                // 펼쳐진 적이 없으면 펼치기 애니메이션
                if (mapExpanded[position] == null || mapExpanded[position] == false) {

                    val v = binding.clPhotos
                    v.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            v.viewTreeObserver.removeOnGlobalLayoutListener(this)

                            mapExpanded[position] = true
                            val anim = ValueAnimator.ofInt(0, toHeight)
                            anim.addUpdateListener { valueAnimator ->
                                val `val` = valueAnimator.animatedValue as Int
                                val layoutParams = v.layoutParams
                                layoutParams.height = `val`
                                v.layoutParams = layoutParams
                            }
                            anim.duration = 250
                            anim.start()
                        }
                    })
                } else {
                    ll.height = toHeight
                    binding.clPhotos.layoutParams = ll
                }

            } else {
                // 펼쳐진 적이 있으면 접기 애니메이션
                if (mapExpanded[position] != null && mapExpanded[position] == true) {
                    Util.log("shrink animation row $position")
                    val v = binding.clPhotos
                    v.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            v.viewTreeObserver.removeOnGlobalLayoutListener(this)

                            mapExpanded[position] = false
                            val anim = ValueAnimator.ofInt(toHeight, 0)
                            anim.addUpdateListener { valueAnimator ->
                                val `val` = valueAnimator.animatedValue as Int
                                val layoutParams = v.layoutParams
                                layoutParams.height = `val`
                                v.layoutParams = layoutParams
                            }
                            anim.duration = 250
                            anim.start()
                        }
                    })
                } else {
                    //                Util.log("shrink row "+position);
                    ll.height = 0
                    binding.clPhotos.layoutParams = ll
                }
            }
        }
    }
}
