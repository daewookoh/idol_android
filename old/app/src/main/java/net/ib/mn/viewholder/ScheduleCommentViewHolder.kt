package net.ib.mn.viewholder

import android.annotation.SuppressLint
import android.content.Context
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.databinding.ScheduleCommentHeaderBinding
import net.ib.mn.domain.usecase.GetIdolsByIdsUseCase
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ScheduleModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import java.text.SimpleDateFormat

class ScheduleCommentViewHolder(
    val binding: ScheduleCommentHeaderBinding,
    val getIdolsByIdsUseCase: GetIdolsByIdsUseCase?
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        articleModel: ArticleModel,
        mSchedule: ScheduleModel,
        mIds: HashMap<Int, String>?,
        isScheduleCommentPush: Boolean
    ) { with(binding) {
        scheduleTitle.text = mSchedule.title

        if(BuildConfig.CELEB) {
            scheduleIdolWrapper.visibility = View.GONE
        }

        if (mSchedule.allday == 1) {
            scheduleDate.visibility = View.GONE
        } else {
            @SuppressLint("SimpleDateFormat")
            val sdf = SimpleDateFormat("a h:mm", LocaleUtil.getAppLocale(itemView.context))
            val time = sdf.format(mSchedule.dtstart)
            scheduleDate.text = time
            scheduleDate.visibility = View.VISIBLE
        }
//
        getIdolList(itemView.context, scheduleIdol, mSchedule, isScheduleCommentPush)
        scheduleIdolWrapper.visibility = View.VISIBLE
        if (mSchedule.url.isNullOrEmpty()) {
            scheduleUrlWrapper.visibility = View.GONE
        } else {
            scheduleUrl.text = mSchedule.url
            scheduleUrlWrapper.visibility = View.VISIBLE
            Linkify.addLinks(scheduleUrl, Linkify.WEB_URLS)
        }
        if (mSchedule.extra.isNullOrEmpty()) {
            scheduleInfoWrapper.visibility = View.GONE
        } else {
            scheduleInfo.text = mSchedule.extra
            scheduleInfoWrapper.visibility = View.VISIBLE
        }
        if (mSchedule.location.isNullOrEmpty()) {
            scheduleLocation.visibility = View.GONE
        } else {
            scheduleLocation.visibility = View.VISIBLE
            mapTv.text = mSchedule.location
        }
        if (mSchedule.user == null) {
            scheduleUserLevel.visibility = View.GONE
            scheduleUserName.visibility = View.GONE
        } else {
            scheduleUserLevel.visibility = View.VISIBLE
            scheduleUserName.visibility = View.VISIBLE
            scheduleUserLevel.setImageResource(
                Util.getLevelResId(itemView.context, mSchedule.user!!.level)
            )
            scheduleUserName.text = mSchedule.user!!.nickname
        }
        if (mSchedule.category != null) {
            scheduleIcon.setImageResource(Util.getScheduleIcon(mSchedule.category))
        }
        commentCount.text = articleModel.commentCount.toString()
    }}

    //스케쥴 아이돌 목록 가져오기
    private fun getIdolList(
        mContext: Context,
        textView: TextView,
        mSchedule: ScheduleModel,
        isScheduleCommentPush: Boolean
    ) {
        if (getIdolsByIdsUseCase == null) return
        CoroutineScope(Dispatchers.IO).launch {
            val idolNames = ArrayList<String>()

            val idols = getIdolsByIdsUseCase!!(mSchedule.idol_ids)
                .mapListDataResource { it.toPresentation() }
                .awaitOrThrow()

            idols?.let {
                for (idol in idols) {
                    idolNames.add(Util.nameSplit(mContext, idol)[0])
                }
                withContext(Dispatchers.Main) {
                    textView.text = idolNames.sorted().joinToString(", ")
                }
            }
        }.start()
    }

}