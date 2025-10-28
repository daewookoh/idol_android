package net.ib.mn.viewholder

import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.R
import net.ib.mn.adapter.HeartPickMainAdapter
import net.ib.mn.adapter.HeartPickMainIdolAdapter
import net.ib.mn.databinding.ItemHeartPickMainBinding
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.model.HeartPickModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.isWithin48Hours
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class HeartPickMainViewHolder(
    val binding: ItemHeartPickMainBinding,
    val currentDateTime: Long,
    val onItemClickListener: HeartPickMainAdapter.OnItemClickListener,
    val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var heartPickMainIdolAdapter: HeartPickMainIdolAdapter
    private lateinit var mGlideRequestManager: RequestManager

    private val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
    private val constraintSet = ConstraintSet()

    private var timerJob: Job? = null

    fun bind(heartPickModel: HeartPickModel?, isNew: Boolean)   {
        timerJob?.cancel()
        clearView()
        mGlideRequestManager = Glide.with(itemView.context)
        setData(heartPickModel, isNew)
        setOnClickListener(heartPickModel)
    }

    private fun setData(heartPickModel: HeartPickModel?, isNew: Boolean) = with(binding) {
        heartPickModel?.heartPickIdols?:return@with
        if (heartPickModel.heartPickIdols.isEmpty()) return@with

        ivNew.visibility = View.GONE

        tvTitle.text = heartPickModel.title
        tvSubTitle.text = heartPickModel.subtitle
        tvHeartVote.text = UtilK.formatNumberShort(heartPickModel.vote)
        tvComment.text = UtilK.formatNumberShort(heartPickModel.numComments)
        tvName.text = heartPickModel.heartPickIdols[0].title
        tvGroup.text = heartPickModel.heartPickIdols[0].subtitle
        tvGroup.visibility  =if(heartPickModel.heartPickIdols[0].subtitle.isEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }

        if (heartPickModel.status == 1) {
            val isNewBadgeVisible = if (isNew && isWithin48Hours(currentDateTime, heartPickModel.beginAt)) View.VISIBLE else View.GONE
            ivNew.visibility = isNewBadgeVisible
        } else {
            ivNew.visibility = View.GONE
        }


        setVote(heartPickModel)
        setEndDate(heartPickModel)
    }

    private fun clearView() = with(binding) {
        tvTitle.text = ""
        tvSubTitle.text = ""
        tvHeartVote.text = ""
        tvComment.text = ""
        tvName.text = ""
        tvGroup.text = ""
        tvGroup.visibility = View.GONE
        rvHeartPick.adapter = null
        rvHeartPick.visibility = View.GONE
        ivTop.setImageDrawable(null)
        ivPhoto.setImageDrawable(null)
        tvDDay.text = ""
        tvVote.text = ""
        tvVotePercent.text = ""
        tvDoVote.text = ""
    }

    private fun setVote(heartPickModel: HeartPickModel) = with(binding) {
        heartPickModel.heartPickIdols?:return@with

        tvVote.text = String.format(itemView.context.getString(R.string.vote_count_format), numberFormat.format(heartPickModel.heartPickIdols[0].vote))

        if(heartPickModel.vote == 0) {
            tvVotePercent.text = numberFormat.format(0).plus("%")
            setVoteStatus(heartPickModel, 0)
            return@with
        }
        val votePercent: Float = 100.0f * heartPickModel.heartPickIdols[0].vote.toFloat() / heartPickModel.vote.toFloat()    // 분모가 0일 경우 앱이 죽기때문에 위에서 예외 처리
        tvVotePercent.text = numberFormat.format(votePercent.roundToInt()).plus("%")

        val firstPercent = 100 * heartPickModel.heartPickIdols[0].vote / heartPickModel.vote    // 1등 퍼센트

        setVoteStatus(heartPickModel, firstPercent)
    }

    private fun setVoteStatus(heartPickModel: HeartPickModel?, firstPercent: Int) = with(binding) {
        lifecycleScope.launch(Dispatchers.Main) {
            when(heartPickModel?.status) {
                1 -> {  // 투표 진행 중
                    setLayoutRatio(1)
                    setAdapter(heartPickModel.heartPickIdols, heartPickModel.vote, firstPercent)
                    rvHeartPick.visibility = View.VISIBLE
                    clShadow.visibility = View.GONE
                    clDoVote.background = ContextCompat.getDrawable(itemView.context, R.drawable.main_light_radius20)
                    tvDoVote.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_white_black))
                    tvDoVote.text = itemView.context.getString(R.string.guide_vote_title)
                }
                else -> {  // 투표 결과
                    setLayoutRatio(2)
                    rvHeartPick.visibility = View.GONE
                    clShadow.visibility = View.VISIBLE
                    clDoVote.background = ContextCompat.getDrawable(itemView.context, R.drawable.gray900_radius20)
                    tvDoVote.setTextColor(ContextCompat.getColor(itemView.context, R.color.fix_white))
                    tvDoVote.text = itemView.context.getString(R.string.see_result)
                }
            }
            if(heartPickModel != null) {
                setDDay(heartPickModel)
                setImage(heartPickModel)
            }
            clDDay.bringToFront()
            clDoVote.bringToFront()
        }
    }

    private fun setLayoutRatio(status: Int) = with(binding) {
        constraintSet.clone(root)

        tvTitle.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                tvTitle.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val ratio = when{
                    tvTitle.lineCount >= 2 || tvSubTitle.lineCount >= 2 -> {
                        when(status) {
                            1 -> "1:1.28"
                            else -> "1:1.05"
                        }
                    }
                    else -> {
                        when(status) {
                            1 -> "1:1.15"
                            else -> "1:0.92"
                        }
                    }
                }
                constraintSet.setDimensionRatio(cvHeartPick.id, ratio)
                constraintSet.applyTo(root)
            }

        })
    }

    private fun setImage(heartPickModel: HeartPickModel?) = with(binding) {
        heartPickModel?.heartPickIdols?:return@with

        val idolId = heartPickModel.heartPickIdols[0].idol_id

        mGlideRequestManager.load(heartPickModel.bannerUrl)
            .transform(CenterCrop())
            .into(ivTop)
        mGlideRequestManager.load(heartPickModel.heartPickIdols[0].image_url)
            .centerCrop()
            .apply(RequestOptions.circleCropTransform())
            .error(Util.noProfileImage(idolId))
            .fallback(Util.noProfileImage(idolId))
            .placeholder(Util.noProfileImage(idolId))
            .dontAnimate()
            .into(ivPhoto)
    }

    private fun setDDay(heartPickModel: HeartPickModel) = with(binding) {
        // 날짜 포맷 정의
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(itemView.context))
        dateTimeFormat.timeZone = Const.TIME_ZONE_KST

        // 두 날짜 파싱
        val now: Date = dateTimeFormat.parse(dateTimeFormat.format(Calendar.getInstance(Const.TIME_ZONE_KST).time)) ?: throw IllegalArgumentException("Invalid start time format")
        val endDate: Date = dateTimeFormat.parse(heartPickModel.endAt) ?: throw IllegalArgumentException("Invalid end time format")

        // 두 날짜의 차이 계산 (밀리초)
        val diff = endDate.time - now.time

        when {
            diff < 0-> {
                tvDDay.text = itemView.context.getString(R.string.vote_finish)
            }
            diff < 86400000 -> {
                countdownTimer(diff)
            }
            else -> {
                val dDay = diff / 86400000
                tvDDay.text = "D-$dDay"
            }
        }
    }

    private fun countdownTimer(diff: Long) {
        timerJob = lifecycleScope.launch(Dispatchers.IO) {
            var remainingTime = diff
            while (remainingTime > 0) {
                val hours = remainingTime / 3600000
                val minutes = (remainingTime % 3600000) / 60000
                val seconds = (remainingTime % 60000) / 1000
                val timeFormat = SimpleDateFormat("HH:mm:ss", LocaleUtil.getAppLocale(itemView.context))
                val diffDate = Date(0, 0, 0, hours.toInt(), minutes.toInt(), seconds.toInt())

                withContext(Dispatchers.Main){
                    binding.tvDDay.text = timeFormat.format(diffDate)
                }

                delay(1000)
                remainingTime -= 1000

                if(remainingTime <= 0) {
                    return@launch
                }
            }
        }
    }

    private fun setEndDate(heartPickModel: HeartPickModel) {
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(itemView.context))
        val endDate: Date = dateTimeFormat.parse(heartPickModel.endAt) ?: throw IllegalArgumentException("Invalid end time format")
        val startDate: Date = dateTimeFormat.parse(heartPickModel.beginAt) ?: throw IllegalArgumentException("Invalid end time format")
        val periodTimeFormat = SimpleDateFormat("yyyy.MM.dd", LocaleUtil.getAppLocale(itemView.context))

        binding.tvPeriodDate.text = if(heartPickModel.status == 2) {
            "${periodTimeFormat.format(startDate)}~${periodTimeFormat.format(endDate)}"
        } else {
            String.format(itemView.context.getString(R.string.finish_at),periodTimeFormat.format(endDate))
        }
    }

    private fun setOnClickListener(heartPickModel: HeartPickModel?) {
        binding.clDoVote.setOnClickListener {
            onItemClickListener.onItemClick(heartPickModel?.id?:0)
        }
    }

    private fun setAdapter(heartPickList: ArrayList<HeartPickIdol>?, totalVote: Int, firstPercent: Int) {
        binding.rvHeartPick.adapter = null
        heartPickMainIdolAdapter = HeartPickMainIdolAdapter(heartPickList, totalVote, firstPercent, lifecycleScope)
        binding.rvHeartPick.adapter = heartPickMainIdolAdapter
    }

}