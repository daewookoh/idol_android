package net.ib.mn.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.databinding.IdolSearchItemBinding
import net.ib.mn.databinding.ScheduleWriteIdolItemBinding
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.UtilK.Companion.getTypeList
import net.ib.mn.utils.UtilK.Companion.setName
import net.ib.mn.utils.setIdolBadgeIcon
import net.ib.mn.view.ProgressBarLayout
import java.text.NumberFormat
import kotlin.math.log10
import androidx.core.graphics.toColorInt
import net.ib.mn.utils.Const

class FavoriteSettingAdapter(
    private val mContext: Context,
    private val mGlideRequestManager: RequestManager,
    private val mListener: OnAdapterCheckedChangeListener
) : ArrayAdapter<IdolModel?>(
    mContext, R.layout.idol_search_item
) {
    private var mMaxVoteCount: Long = 0

    interface OnAdapterCheckedChangeListener {
        fun onCheckedChanged(
            button: CompoundButton,
            isChecked: Boolean,
            item: IdolModel,
            context: Context
        )
    }

    override fun notifyDataSetChanged() {
        if (getCount() > 0) {
            mMaxVoteCount = getItem(0)!!.heart
            if (!BuildConfig.CELEB) {
                for (i in items.indices) {
                    if (getItem(i)!!.heart > mMaxVoteCount) {
                        mMaxVoteCount = getItem(i)!!.heart
                    }
                }
            }
        }
        super.notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: IdolSearchItemBinding

        if(convertView == null) {
            binding = IdolSearchItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as IdolSearchItemBinding
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    override fun update(view: View?, item: IdolModel?, position: Int): Unit = with(view?.tag as IdolSearchItemBinding) {
        val item = item ?: return

        containerRanking.setBackgroundResource(0)

        name.text = item.getName(mContext)
        btnFavorite.setChecked(item.isFavorite)

        if (BuildConfig.CELEB) {
            if (item.isViewable == "Y") {
                val typeList = getTypeList(context, item.type + item.category)
                val progress = ResourcesCompat.getDrawable(context.resources, R.drawable.progressbar_ranking, null)
                progress?.let {
                    val tintColor =
                        if (typeList.type == null) ContextCompat.getColor(context, R.color.main)
                        else Util.getUiColor(context, typeList).toColorInt()

                    DrawableCompat.setTint(it, tintColor)
                    DrawableCompat.setTintMode(it, PorterDuff.Mode.MULTIPLY)
                }
                progressBar.background = progress
            } else progressBar.setBackgroundResource(R.drawable.progressbar_ranking_gray)
        } else {
            if (item.isViewable == "Y") {
                progressBar.setBackgroundResource(R.drawable.progressbar_ranking_s_league)
            } else {
                progressBar.setBackgroundResource(R.drawable.progressbar_ranking_gray)
            }
        }

        setName(mContext, item, name, group)

        when (item.anniversary) {
            Const.ANNIVERSARY_BIRTH -> {
                if (item.type == "S" || BuildConfig.CELEB) {
                    badgeBirth.setVisibility(View.VISIBLE)
                    badgeDebut.setVisibility(View.GONE)
                } else {
                    // 그룹은 데뷔뱃지로 보여주기
                    badgeBirth.setVisibility(View.GONE)
                    badgeDebut.setVisibility(View.VISIBLE)
                }
                badgeComeback.setVisibility(View.GONE)
                badgeMemorialDay.setVisibility(View.GONE)
                badgeAllInDay.setVisibility(View.GONE)
            }

            Const.ANNIVERSARY_DEBUT -> {
                badgeBirth.setVisibility(View.GONE)
                badgeDebut.setVisibility(View.VISIBLE)
                badgeComeback.setVisibility(View.GONE)
                badgeMemorialDay.setVisibility(View.GONE)
                badgeAllInDay.setVisibility(View.GONE)
            }

            Const.ANNIVERSARY_COMEBACK -> {
                badgeBirth.setVisibility(View.GONE)
                badgeDebut.setVisibility(View.GONE)
                badgeComeback.setVisibility(View.VISIBLE)
                badgeMemorialDay.setVisibility(View.GONE)
                badgeAllInDay.setVisibility(View.GONE)
            }

            Const.ANNIVERSARY_MEMORIAL_DAY -> {
                badgeBirth.setVisibility(View.GONE)
                badgeDebut.setVisibility(View.GONE)
                badgeComeback.setVisibility(View.GONE)
                badgeMemorialDay.setVisibility(View.VISIBLE)
                badgeAllInDay.setVisibility(View.GONE)
                val badgeCount = NumberFormat.getNumberInstance(getAppLocale(mContext))
                    .format(item.anniversaryDays).replace(",", "")
                val memorialDayText = badgeCount + mContext.getString(R.string.lable_day)
                badgeMemorialDay.setText(memorialDayText)
            }

            Const.ANNIVERSARY_ALL_IN_DAY -> {
                badgeBirth.setVisibility(View.GONE)
                badgeDebut.setVisibility(View.GONE)
                badgeComeback.setVisibility(View.GONE)
                badgeMemorialDay.setVisibility(View.GONE)
                badgeAllInDay.setVisibility(View.VISIBLE)
            }

            else -> {
                badgeBirth.setVisibility(View.GONE)
                badgeDebut.setVisibility(View.GONE)
                badgeComeback.setVisibility(View.GONE)
                badgeMemorialDay.setVisibility(View.GONE)
                badgeAllInDay.setVisibility(View.GONE)
            }
        }

        UtilK.profileRoundBorder(
            item.miracleCount,
            item.fairyCount,
            item.angelCount,
            (photoBorder as AppCompatImageView?)!!
        )

        // 기적, 루키 등 배지 세팅
        setIdolBadgeIcon(imageAngel, imageFairy, imageMiracle, imageRookie, imageSuperRookie, item)

        val idolId = item.getId()

        mGlideRequestManager
            .load(item.imageUrl)
            .apply(RequestOptions.circleCropTransform())
            .error(Util.noProfileImage(idolId))
            .fallback(Util.noProfileImage(idolId))
            .placeholder(Util.noProfileImage(idolId))
            .into(photo)
        val voteCountComma =
            NumberFormat.getNumberInstance(getAppLocale(mContext)).format(item.heart)
        count.setText(voteCountComma)
        if (BuildConfig.CELEB) {
            if (item.isViewable == "N") {
                count.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.text_white_black
                    )
                )
            } else {
                count.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.card_fix_text_black
                    )
                )
            }
        }

        if (mMaxVoteCount == 0L) {
            progress.setWidthRatio(20)
        } else {
            // int 오버플로 방지
            if (item.heart == 0L) progress.setWidthRatio(20)
            else progress.setWidthRatio(
                20 + ((log10(item.heart.toDouble()) * 80) / log10(
                    mMaxVoteCount.toDouble()
                )).toInt()
            )
        }

        btnMost.setChecked(item.isMost)
        val listener = View.OnClickListener { v: View? ->
            val buttonView = (v as CompoundButton)
            val isChecked = buttonView.isChecked()
            mListener.onCheckedChanged(buttonView, isChecked, item, context)
        }
        btnMost.setOnClickListener(listener)
        btnFavorite.setOnClickListener(listener)
    }
}
