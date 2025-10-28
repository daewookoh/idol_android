package net.ib.mn.attendance.viewholder

import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Activity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.addon.IdolGson
import net.ib.mn.attendance.AttendanceDays
import net.ib.mn.base.BaseApplication
import net.ib.mn.databinding.ItemAttendanceGraphBinding
import net.ib.mn.databinding.ItemAttendanceHeaderBinding
import net.ib.mn.model.StampModel
import net.ib.mn.model.StampRewardModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.safeActivity
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AttendanceHeaderVH(
    val binding: ItemAttendanceHeaderBinding,
    private val user: UserModel,
    private val stamp: JSONObject,
    private val rewards: List<Map<String, StampRewardModel>>,
    private val sharedAppState: SharedAppState,
) : RecyclerView.ViewHolder(binding.root) {
    private var itemAttendance: HashMap<Int, ItemAttendanceGraphBinding>
    private lateinit var currentDate: String


    init {
        with(binding.attendanceGraph) {
            itemAttendance = hashMapOf(
                0 to itemAttendanceGraph01,
                1 to itemAttendanceGraph02,
                2 to itemAttendanceGraph03,
                3 to itemAttendanceGraph04,
                4 to itemAttendanceGraph05,
                5 to itemAttendanceGraph06,
                6 to itemAttendanceGraph07,
                7 to itemAttendanceGraph08,
                8 to itemAttendanceGraph09,
                9 to itemAttendanceGraph10,
            )
        }
    }

    fun bind(isSuccessOfStamp: Boolean) {
        setUserInfo()
        setDaysWithStart()
        setDaysWith()
        setConsecutiveReward()
        setStampStatus(isSuccessOfStamp)
        setAttendanceCheckBtnStatus()
    }

    private fun setUserInfo() = with(user) {
        // 유저정보.
        Glide.with(itemView.context).load(imageUrl)
            .apply(RequestOptions.circleCropTransform())
            .error(Util.noProfileImage(id))
            .fallback(Util.noProfileImage(id))
            .placeholder(Util.noProfileImage(id))
            .into(binding.eivAttendancePhoto)

        binding.tvAttendanceUsername.text = nickname
        binding.ivAttendanceLevel.setImageBitmap(
            Util.getLevelImage(
                itemView.context,
                this,
            ),
        )
    }

    // 최애돌과 함께한 시작 날짜.
    private fun setDaysWithStart() {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(itemView.context))
        formatter.timeZone = TimeZone.getTimeZone("UTC")

        val f = DateFormat.getDateInstance(
            DateFormat.MEDIUM,
            LocaleUtil.getAppLocale(itemView.context),
        )
        val utcDate = formatter.parse(user.createdAt)
        val createdAt = utcDate?.let { f.format(it) }

        // 최애돌과 함께한 시작 시간.
        binding.tvAttendanceStart.text = String.format(
            itemView.context.getString(R.string.attendance_days_start), createdAt,
        )
    }

    // 최애돌과 함께한 날짜.
    private fun setDaysWith() {
        // 이건 D+day계산 로직.
        val kstFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(itemView.context))
        kstFormatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        val kstDate = kstFormatter.parse(user.createdAt)

        // 서버에서온 Year, Month은 그대로 넣어주고 Day에서 무조건 00:00:00부터 시작할 수 있도록 해준다.
        val convertKstTime = Calendar.getInstance()
        convertKstTime.set(
            Calendar.YEAR,
            android.text.format.DateFormat.format("yyyy", kstDate).toString().toInt(),
        )
        convertKstTime.set(
            Calendar.MONTH,
            android.text.format.DateFormat.format("MM", kstDate).toString().toInt() - 1,
        ) // 주의 0부터 시작하니까 -1을 해줘야됨 ex)5를 넣을경우 6월을 의미함.
        convertKstTime.set(
            Calendar.DATE,
            android.text.format.DateFormat.format("dd", kstDate).toString().toInt(),
        )

        // 00:00:00세팅.
        convertKstTime.set(Calendar.HOUR, 0)
        convertKstTime.set(Calendar.MINUTE, 0)
        convertKstTime.set(Calendar.SECOND, 0)
        convertKstTime.set(Calendar.HOUR_OF_DAY, 0)

        // 최애돌과 함께한 시간.
        val today = Calendar.getInstance()
        val calculatedDate = (today.time.time - convertKstTime.time.time) / (60 * 60 * 24 * 1000)

        val appName = UtilK.getAppName(itemView.context)
        val daysWithText = SpannableString(
            String.format(itemView.context.getString(R.string.attendance_with), appName) +
                " " +
                "D+${calculatedDate + 1L}",
        )

        daysWithText.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.main,
                ),
            ),
            String.format(
                itemView.context.getString(R.string.attendance_with),
                appName,
            ).length + 1,
            daysWithText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        // 최애돌과 함께한 시작 시간.
        binding.tvAttendanceDaysWith.text = daysWithText
    }

    @SuppressLint("SetTextI18n")
    private fun setConsecutiveReward() {
        AttendanceDays.values().forEach { attendanceDay ->
            itemAttendance[attendanceDay.days - 1]?.tvAttendanceDay?.text =
                attendanceDay.days.toString()

            val stampReward = rewards.find {
                it.keys.contains(attendanceDay.day)
            } ?: return@forEach

            with(stampReward[attendanceDay.day] ?: return@forEach) {
                if (heart == 0 && diamond == 0) {
                    return@with
                }

                setVisibleReward(
                    heart,
                    diamond,
                    itemAttendance[attendanceDay.days - 1] ?: return@with,
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setVisibleReward(
        heart: Int,
        diamond: Int,
        itemAttendanceGraphBinding: ItemAttendanceGraphBinding,
    ) = with(itemAttendanceGraphBinding) {
        tvAttendanceReward.visibility = View.VISIBLE
        ivAttendanceReward.visibility = View.VISIBLE
        tvAttendanceDay.visibility = View.GONE

        when {
            heart > 0 && diamond <= 0 -> {
                tvAttendanceReward.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.main),
                )
                tvAttendanceReward.text = heart.toString()
                ivAttendanceReward.setImageResource(R.drawable.img_attendance_heart_renewal)
            }

            heart <= 0 && diamond > 0 -> {
                tvAttendanceReward.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.text_light_blue),
                )
                tvAttendanceReward.text =
                    diamond.toString()
                ivAttendanceReward.setImageResource(R.drawable.img_attendance_dia_renewal)
            }

            heart > 0 && diamond > 0 -> {
                tvAttendanceReward.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.main),
                )
                tvAttendanceReward.text = "$heart,$diamond"
                ivAttendanceReward.setImageResource(R.drawable.img_attendance_heart_renewal)
            }
        }
    }

    private fun setStampStatus(isSuccessOfStamp: Boolean) {
        val gson = IdolGson.getInstance()
        val stampModel = gson.fromJson(stamp.toString(), StampModel::class.java)

        if (!stampModel.is_viewable.equals("Y") || stampModel.days == null) {
            return
        }

        val days = if (isSuccessOfStamp) {
            stampModel.days - 1
        } else {
            stampModel.days
        }

        for (i in 0 until days) {
            itemAttendance[i]?.tvAttendanceReward?.visibility = View.GONE
            itemAttendance[i]?.ivAttendanceReward?.visibility = View.GONE
            itemAttendance[i]?.tvAttendanceDay?.visibility = View.GONE

            itemAttendance[i]?.ivAttendanceCompleteDay?.visibility = View.VISIBLE
        }
    }

    private fun setAttendanceCheckBtnStatus() = with(binding.attendanceGraph) {
        val gson = IdolGson.getInstance()
        val stampModel = gson.fromJson(stamp.toString(), StampModel::class.java)

        if (stamp.length() <= 0) {
            btnAttendanceCheck.background =
                ContextCompat.getDrawable(itemView.context, R.drawable.bg_radius_12_main)
            btnAttendanceCheck.isEnabled = true
            setAbleAttendance(true)
            val rewardMap = rewards.find { it.keys.contains(AttendanceDays.DAY1.day) }
            val obtainableRewardModel = rewardMap?.get(AttendanceDays.DAY1.day) ?: return
            setAttendanceBtnText(obtainableRewardModel)
            return
        }

        // 오늘날짜와 스탬프 찍은 날짜가 같은지 확인해서 다르면 찍을수 있다는 뜻이므로 변경해주기.
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        currentDate = format.format(Date())

        if (currentDate == stampModel.stamped_at) {
            btnAttendanceCheck.background =
                ContextCompat.getDrawable(itemView.context, R.drawable.bg_radius_12_gray200)
            btnAttendanceCheck.text = String.format(
                itemView.context.getString(R.string.daily_stamp_already_done),
                stampModel.days.toString()
            )
            setAbleAttendance(false)
            return
        }

        btnAttendanceCheck.background =
            ContextCompat.getDrawable(itemView.context, R.drawable.bg_radius_12_main)
        btnAttendanceCheck.isEnabled = true
        setAbleAttendance(true)

        val daysModel = AttendanceDays.values().find { it.days == (stampModel.days?.plus(1)) }
        val rewardMap =
            rewards.find { it.keys.any { day -> day == daysModel?.day } }
        val obtainableRewardModel = rewardMap?.get(daysModel?.day) ?: return

        setAttendanceBtnText(obtainableRewardModel)
    }

    private fun setAttendanceBtnText(obtainableRewardModel: StampRewardModel) = with(obtainableRewardModel) {
        when {
            heart > 0 && diamond <= 0 -> {
                binding.attendanceGraph.btnAttendanceCheck.text = String.format(
                    itemView.context.getString(R.string.stamp_and_get_hearts),
                    heart.toString(),
                )
            }

            heart <= 0 && diamond > 0 -> {
                binding.attendanceGraph.btnAttendanceCheck.text = String.format(
                    itemView.context.getString(R.string.stamp_and_get_diamonds),
                    diamond.toString(),
                )
            }

            heart > 0 && diamond > 0 -> {
                binding.attendanceGraph.btnAttendanceCheck.text = String.format(
                    itemView.context.getString(R.string.stamp_and_get_hearts),
                    "$heart,$diamond",
                )
            }
        }
    }

    fun playStampLottie(days: Int, finishStampLottie:() -> Unit)  {

        if (days < 0) {
            return
        }

        with(itemAttendance[days] ?: return) {
            loAttendanceStampOn.visibility = View.VISIBLE

            if (Util.isDarkTheme(itemView.context as Activity)) {
                loAttendanceStampOn.setAnimation("animation_attendance_stamp_dark.json")
            } else {
                loAttendanceStampOn.setAnimation("animation_attendance_stamp.json")
            }

            loAttendanceStampOn.playAnimation()
            loAttendanceStampOn.addAnimatorListener(object :
                Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {

                }

                override fun onAnimationEnd(p0: Animator) {
                    loAttendanceStampOn.visibility = View.INVISIBLE
                    finishStampLottie()
                }

                override fun onAnimationCancel(p0: Animator) {

                }

                override fun onAnimationRepeat(p0: Animator) {

                }
            })
        }
    }

    fun setLastCompleteImageVisibilty(days: Int) {
        if (days < 0) {
            return
        }
        itemAttendance[days]?.ivAttendanceCompleteDay?.visibility = View.VISIBLE
    }


    private fun setAbleAttendance(isAble: Boolean) {
        Util.setPreference(itemView.context, Const.PREF_IS_ABLE_ATTENDANCE, isAble)
        sharedAppState.setAttendance(isAble)

        with(binding.attendanceGraph.loAttendanceCheck) {
            if (!isAble) {
                return@with
            }

            val handAnimation =
                if (BuildConfig.CELEB) "animation_attendance_hand_celeb.json" else "animation_attendance_hand.json"
            setAnimation(handAnimation)
            playAnimation()
            visibility = View.VISIBLE
        }
    }
}