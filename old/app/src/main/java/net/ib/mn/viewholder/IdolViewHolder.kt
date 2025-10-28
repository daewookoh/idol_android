package net.ib.mn.viewholder

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.databinding.ItemSearchedIdolBinding
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.setColorFilter
import net.ib.mn.utils.setIdolBadgeIcon
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.sqrt
import net.ib.mn.utils.ext.getFontColor
import net.ib.mn.utils.ext.getUiColor

class IdolViewHolder(
    val binding: ItemSearchedIdolBinding,
    val context: Context,
    private val mAccount: IdolAccount?,
    private val mGlideRequestManager: RequestManager,
    private val searchedIdolList: ArrayList<IdolModel>,
    private val storageSearchedIdolList: ArrayList<IdolModel>,
    private val onIdolButtonClick: (IdolModel, View, Int) -> Unit,
    private val onCheckedChanged: (CompoundButton, Boolean, IdolModel) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var mMaxVoteCount: Long = 0

    fun bind(idol: IdolModel, isShowViewMore: Boolean, position: Int) {
        val listener = View.OnClickListener { v -> onIdolButtonClick(idol, v, position) }
        val checkedListener =
            View.OnClickListener { v -> onCheckedChanged(v as CompoundButton, v.isChecked, idol) }

        for (i in 0 until searchedIdolList.size) {
            if (searchedIdolList[i].heart > mMaxVoteCount)
                mMaxVoteCount = searchedIdolList[i].heart
        }
        for (i in 0 until storageSearchedIdolList.size) {
            if (storageSearchedIdolList[i].heart > mMaxVoteCount)
                mMaxVoteCount = storageSearchedIdolList[i].heart
        }

        if (!ConfigModel.getInstance(context).showLeagueTab) {
            binding.tvLeague.visibility = View.GONE
        }

        binding.idolName.text = idol.getName(context)
        binding.btnMost.isChecked = idol.isMost
        binding.btnFavorite.isChecked = idol.isFavorite

        if (idol.isViewable.equals("Y", ignoreCase = true)){
            if(BuildConfig.CELEB) {
                val typeList = UtilK.getTypeList(context, idol.type+idol.category)
                val progressBar =
                    ContextCompat.getDrawable(context, R.drawable.progressbar_ranking)
                progressBar?.setColorFilter(
                    if (typeList.type.isNullOrEmpty()) ContextCompat.getColor(context,
                        R.color.main
                    ) else Color.parseColor(typeList.getUiColor(context))
                )
                binding.progressBar.background = progressBar
            } else {
                binding.progressBar.setBackgroundResource(R.drawable.progressbar_ranking_s_league)
            }
        }
        else
            binding.progressBar.setBackgroundResource(R.drawable.progressbar_ranking_gray)

        val nameGroup = Util.nameSplit(context, idol)
        if (BuildConfig.CELEB || idol.type.equals("S", ignoreCase = true)) {
            binding.idolName.text = nameGroup[0]
            binding.group.visibility = View.VISIBLE
            if (!nameGroup[1].isEmpty())
                binding.group.text = nameGroup[1]
            else
                binding.group.visibility = View.GONE
        } else {
            binding.idolName.text = idol.getName(context)
            binding.group.visibility = View.GONE
        }

        // 기념일
        when (idol.anniversary) {
            Const.ANNIVERSARY_BIRTH -> {
                if (BuildConfig.CELEB || idol.type == "S") {
                    binding.badgeBirth.visibility = View.VISIBLE
                    binding.badgeDebut.visibility = View.GONE
                } else {
                    // 그룹은 데뷔뱃지로 보여주기
                    binding.badgeBirth.visibility = View.GONE
                    binding. badgeDebut.visibility = View.VISIBLE
                }
                binding.badgeComeback.visibility = View.GONE
                binding.badgeMemorialDay.visibility = View.GONE
                binding.badgeAllInDay.visibility = View.GONE
            }
            Const.ANNIVERSARY_DEBUT -> {
                binding.badgeBirth.visibility = View.GONE
                binding.badgeDebut.visibility = View.VISIBLE
                binding.badgeComeback.visibility = View.GONE
                binding.badgeMemorialDay.visibility = View.GONE
                binding.badgeAllInDay.visibility = View.GONE
            }
            Const.ANNIVERSARY_COMEBACK -> {
                binding.badgeBirth.visibility = View.GONE
                binding.badgeDebut.visibility = View.GONE
                binding.badgeComeback.visibility = View.VISIBLE
                binding.badgeMemorialDay.visibility = View.GONE
                binding.badgeAllInDay.visibility = View.GONE
            }
            Const.ANNIVERSARY_MEMORIAL_DAY -> {
                binding.badgeBirth.visibility = View.GONE
                binding.badgeDebut.visibility = View.GONE
                binding.badgeComeback.visibility = View.GONE
                binding.badgeMemorialDay.visibility = View.VISIBLE
                binding. badgeAllInDay.visibility = View.GONE

                val memorialDayCount : String
                if(Util.isRTL(context)) {
                    memorialDayCount = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(idol.anniversaryDays)
                }
                else{
                    memorialDayCount = idol.anniversaryDays.toString()
                }
                binding.badgeMemorialDay.text = memorialDayCount.replace(("[^\\d.]").toRegex(), "").plus(context.getString(
                    R.string.lable_day))
            }
            Const.ANNIVERSARY_ALL_IN_DAY -> {
                binding.badgeBirth.visibility = View.GONE
                binding.badgeDebut.visibility = View.GONE
                binding.badgeComeback.visibility = View.GONE
                binding.badgeMemorialDay.visibility = View.GONE
                binding.badgeAllInDay.visibility = View.VISIBLE
            }
            else -> {
                binding.badgeBirth.visibility = View.GONE
                binding.badgeDebut.visibility = View.GONE
                binding.badgeComeback.visibility = View.GONE
                binding.badgeMemorialDay.visibility = View.GONE
                binding.badgeAllInDay.visibility = View.GONE
            }
        }

        UtilK.profileRoundBorder(idol.miracleCount, idol.fairyCount, idol.angelCount, binding.photoBorder)

        setIdolBadgeIcon(binding.imageAngel, binding.imageFairy,binding.imageMiracle, binding.imageRookie, binding.imageSuperRookie ,idol)

        binding.rankIndex.visibility = View.GONE

        val idolId = idol.getId()
        mGlideRequestManager
            .load(idol.imageUrl)
            .apply(RequestOptions.circleCropTransform())
            .error(Util.noProfileImage(idolId))
            .fallback(Util.noProfileImage(idolId))
            .placeholder(Util.noProfileImage(idolId))
            .into(binding.idolPhoto)
        val voteCountComma = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(idol.heart)
        binding.count.text = voteCountComma
        if( BuildConfig.CELEB ) {
            if(idol.isViewable == "N"){
                binding.count.setTextColor(ContextCompat.getColor(context, R.color.text_white_black))
            }
            else{
                binding.count.setTextColor(ContextCompat.getColor(context, R.color.card_fix_text_black))
            }
        }

        if (mMaxVoteCount == 0L) {
            binding.progress.setWidthRatio(DEFAULT_PROGRESS_BAR_PERCENT)
        } else {
            // int 오버플로 방지
            if (idol.heart == 0L)
                binding.progress.setWidthRatio(DEFAULT_PROGRESS_BAR_PERCENT)
            else {
                var percent =
                    DEFAULT_PROGRESS_BAR_PERCENT + (sqrt(sqrt(idol.heart.toDouble())) * 48 / sqrt(
                        sqrt(mMaxVoteCount.toDouble())
                    )).toInt()

                // 만약 100퍼센트를 넘는다면 100으로 세팅.
                if (percent > 100) {
                    percent = 100
                }

                binding.progress.setWidthRatio(percent)
            }
        }


        if (
        // 관리자 계정이 아니면서
            (mAccount != null
                && mAccount.heart != Const.LEVEL_ADMIN)
            // 최애가 아닐 때
            && (mAccount.most != null
                && idol.getId() != mAccount.most!!.getId()
                && idol.groupId != mAccount.most!!.groupId
                || (mAccount.most != null && idol.getId() != idol.groupId && idol.getId() != mAccount?.most?.getId()))
            // 최애가 없을 때
            || (mAccount != null
                && mAccount.most == null)) {
            binding.communityButton.setBackgroundResource(R.drawable.search_border_center)
            binding.idolTalkButton.visibility = View.GONE
        } else {
            binding.communityButton.setBackgroundResource(R.drawable.search_border_left)
            binding.idolTalkButton.visibility = View.VISIBLE
            binding.idolTalkButton.setOnClickListener(listener)
        }

        binding.tvHelpSetMost.visibility = if (position == 0
            && (mAccount?.most == null || mAccount.most?.category.equals("B"))
            && Util.getPreferenceBool(context, Const.PREF_SHOW_SET_MOST_IN_SEARCH, true)) {
            View.VISIBLE
        } else {
            View.GONE
        }
        if(BuildConfig.CELEB) {
            binding.tvHelpSetMost.text = context.getString(R.string.tooltip_set_most_actor)
        }

        binding.viewMore.visibility = if (isShowViewMore) View.VISIBLE else View.GONE

        binding.btnMost.setOnClickListener(checkedListener)
        binding.btnFavorite.setOnClickListener(checkedListener)
        binding.clSearchedIdol.setOnClickListener(listener)
        binding.communityButton.setOnClickListener(listener)
        binding.scheduleButton.setOnClickListener(listener)
        binding.viewMore.setOnClickListener(listener)
        binding.tvHelpSetMost.setOnClickListener(listener)

    }

    companion object {
        const val DEFAULT_PROGRESS_BAR_PERCENT = 50
    }
}