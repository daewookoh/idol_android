package net.ib.mn.adapter

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.RankingItemBinding
import net.ib.mn.databinding.TextureRankingItemBinding
import net.ib.mn.model.IdolModel
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.RankingBindingProxy
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.view.ProgressBarLayout
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.sqrt
import net.ib.mn.utils.ext.getFontColor
import net.ib.mn.utils.ext.getUiColor

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: 순위 더보기 눌렀을 떄 나오는 랭킹 어댑터
 *
 * */

class NewRankingFakeAdapter(
	private val context: Context,
	private val glideRequestManager: RequestManager,
	private var mItems: ArrayList<IdolModel>,
	private var typeList: TypeListModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	private var mMaxVoteCount: Long = 0

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		mMaxVoteCount = if( mItems.size > 0 ) { mItems[0].heart } else { 0 }

        val bindingProxy: RankingBindingProxy = if (Util.isOSNougat()) {
            val binding = RankingItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            RankingBindingProxy(binding)
        } else {
            val binding = TextureRankingItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            RankingBindingProxy(binding)
        }

        bindingProxy.voteBtn.setColorFilter(if (typeList?.type.isNullOrEmpty()) context.resources.getColor(R.color.main) else Color.parseColor(typeList.getUiColor(context).toString()), android.graphics.PorterDuff.Mode.SRC_IN)
        val progressBar = this.context.getResources().getDrawable(R.drawable.progressbar_ranking)
        progressBar.setColorFilter(if (typeList?.type.isNullOrEmpty()) context.resources.getColor(R.color.main) else Color.parseColor(typeList.getUiColor(context).toString()), android.graphics.PorterDuff.Mode.MULTIPLY)
        bindingProxy.progressBarFrame.background = progressBar

        return RankViewHolder(bindingProxy)
	}

	override fun getItemId(position: Int): Long {
		var id = 0L
		if( !mItems.isEmpty() ) {
			id = mItems[position].getId().toLong()
		}

		return id
	}

	override fun getItemCount(): Int {
		if( mItems.size == 0 )
			return 0
		return mItems.size
	}

	fun setTypeList(typeList: TypeListModel){
		this.typeList = typeList
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (mItems.isNullOrEmpty()) return
        (holder as RankViewHolder).bind(mItems[position], position)
	}

	fun clear() {
		mItems.clear()
	}

	fun setItems(@NonNull items: ArrayList<IdolModel>) {
		// 이렇게하면 java.lang.IndexOutOfBoundsException: Inconsistency detected 익셉션이 발생.
        mItems = items
		// 남녀 전환시 순위막대 이상해져서 여기에도 넣어줌
		mMaxVoteCount = if( mItems.size > 0 ) { mItems[0].heart } else { 0 }
	}

	inner class RankViewHolder(val binding: RankingBindingProxy) : RecyclerView.ViewHolder(binding.root) {
        private val badgeBirth = binding.badgeBirth
        private val badgeDebut = binding.badgeDebut
        private val badgeComeback = binding.badgeComeback
        private val badgeMemorialDay = binding.badgeMemorialDay
        private val badgeAllInDay = binding.badgeAllInDay
        public val nameView: AppCompatTextView = binding.nameView
        val rankView: AppCompatTextView = binding.rankView
        val voteCountView: AppCompatTextView = binding.voteCountView
        private val imageView: AppCompatImageView = binding.imageView
        private val voteBtn: AppCompatImageView = binding.voteBtn
        private val progressBar: ProgressBarLayout = binding.progressBar
        private val photoBorder: AppCompatImageView = binding.photoBorder
        private val groupView: AppCompatTextView = binding.groupView

        // 이붙
        private val mContainerPhotos: ConstraintLayout = binding.containerPhotos

        // 기부천사/기부요정
        private val iconAngel: AppCompatTextView = binding.iconAngel
        private val iconFairy: AppCompatTextView = binding.iconFairy
        private val iconMiracle: AppCompatTextView = binding.iconMiracle

		fun bind(idol: IdolModel, position: Int) {

			if(typeList!!.type.isNullOrEmpty()){
				voteCountView.setTextColor(context.resources.getColor(R.color.text_white_black))
			}
			mContainerPhotos.visibility = View.GONE

			val rank = idol.rank

			if (idol.getName(context).contains("_")) {
				nameView.text = Util.nameSplit(context, idol)[0]
				groupView.visibility = View.VISIBLE

				if (idol.getName(context).contains("_")) {
					groupView.text = Util.nameSplit(context, idol)[1]
				} else {
					groupView.visibility = View.GONE
				}
			} else {
				nameView.text = idol.getName(context)
				groupView.visibility = View.GONE
			}

			when (idol.anniversary) {
				Const.ANNIVERSARY_BIRTH -> {
					badgeBirth.visibility = View.VISIBLE
					badgeDebut.visibility = View.GONE
					badgeComeback.visibility = View.GONE
					badgeMemorialDay.visibility = View.GONE
					badgeAllInDay.visibility = View.GONE
				}
				Const.ANNIVERSARY_DEBUT -> {
					badgeBirth.visibility = View.GONE
					badgeDebut.visibility = View.VISIBLE
					badgeComeback.visibility = View.GONE
					badgeMemorialDay.visibility = View.GONE
					badgeAllInDay.visibility = View.GONE
				}
				Const.ANNIVERSARY_COMEBACK -> {
					badgeBirth.visibility = View.GONE
					badgeDebut.visibility = View.GONE
					badgeComeback.visibility = View.VISIBLE
					badgeMemorialDay.visibility = View.GONE
					badgeAllInDay.visibility = View.GONE
				}
				Const.ANNIVERSARY_MEMORIAL_DAY -> {
					badgeBirth.visibility = View.GONE
					badgeDebut.visibility = View.GONE
					badgeComeback.visibility = View.GONE
					badgeMemorialDay.visibility = View.VISIBLE
					badgeAllInDay.visibility = View.GONE

					val memorialDayText = "${idol.anniversaryDays}${context.getString(R.string.lable_day)}"
					badgeMemorialDay.text = memorialDayText
				}
				Const.ANNIVERSARY_ALL_IN_DAY -> {
					badgeBirth.visibility = View.GONE
					badgeDebut.visibility = View.GONE
					badgeComeback.visibility = View.GONE
					badgeMemorialDay.visibility = View.GONE
					badgeAllInDay.visibility = View.VISIBLE
				}
				else -> {
					badgeBirth.visibility = View.GONE
					badgeDebut.visibility = View.GONE
					badgeComeback.visibility = View.GONE
					badgeMemorialDay.visibility = View.GONE
					badgeAllInDay.visibility = View.GONE
				}
			}

			if (idol.fairyCount >= 1 && idol.angelCount >= 1) {
				photoBorder.setImageResource(R.drawable.profile_round_angel_fairy)
			} else if (idol.angelCount >= 1) {
				photoBorder.setImageResource(R.drawable.profile_round_angel)
			} else if (idol.fairyCount >= 1) {
				photoBorder.setImageResource(R.drawable.profile_round_fairy)
			} else {
				photoBorder.setImageResource(R.drawable.profile_round_off)
			}

			rankView.text = String.format(
				context.getString(R.string.rank_count_format),
				(rank + 1).toString())

			val voteCount = idol.heart

			val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault()).format(voteCount)
			voteCountView.text = voteCountComma

			val profileThumb: String? = UtilK.top1ImageUrl(context, idol, Const.IMAGE_SIZE_LOWEST)

			val idolId = idol.getId()
			glideRequestManager.load(profileThumb)
				.apply(RequestOptions.circleCropTransform())
				.error(Util.noProfileImage(idolId))
				.fallback(Util.noProfileImage(idolId))
				.placeholder(Util.noProfileImage(idolId))
				.dontAnimate()
				.into(imageView)

			(context as Activity).windowManager
				.defaultDisplay
				.getMetrics(DisplayMetrics())

			if (mMaxVoteCount == 0L) {
				progressBar.setWidthRatio(38)
			} else {
				// int 오버플로 방지
				if (voteCount == 0L) {
					progressBar.setWidthRatio(38)
				} else {
					progressBar.setWidthRatio(38 + (sqrt(sqrt(voteCount.toDouble())) * 62 / sqrt(sqrt(mMaxVoteCount.toDouble()))).toInt())
				}
			}

			Util.log("*** mMaxVoteCount="+mMaxVoteCount);

			Util.setAngelFairyIcon(iconAngel, iconFairy, iconMiracle,idol)
		}
	}
}
