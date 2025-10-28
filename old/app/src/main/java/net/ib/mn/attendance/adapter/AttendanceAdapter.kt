package net.ib.mn.attendance.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.addon.IdolGson
import net.ib.mn.model.DailyRewardModel
import net.ib.mn.attendance.viewholder.AttendanceHeaderVH
import net.ib.mn.attendance.viewholder.AttendanceHeartMoreVH
import net.ib.mn.attendance.viewholder.WarningVH
import net.ib.mn.databinding.ItemAttendanceHeaderBinding
import net.ib.mn.databinding.ItemAttendanceHeartMoreBinding
import net.ib.mn.databinding.ItemWarningBinding
import net.ib.mn.model.StampModel
import net.ib.mn.model.StampRewardModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.UtilK
import net.ib.mn.viewmodel.MainViewModel
import org.json.JSONObject
import javax.inject.Inject

class AttendanceAdapter @Inject constructor(
    private var user: UserModel,
    private var stamp: JSONObject,
    private var rewards: List<Map<String, StampRewardModel>>,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val sharedAppState: SharedAppState,
) : ListAdapter<DailyRewardModel, RecyclerView.ViewHolder>(diffUtil) {

    private var dailyRewards: List<DailyRewardModel> = listOf()
    private var attendanceHeaderListener: AttendanceHeaderListener? = null
    private var attendanceHeartMoreListener: AttendanceHeartMoreListener? = null
    private var isSuccessOfStamp : Boolean = false

    interface AttendanceHeaderListener {
        fun btnAttendanceCheckClick(showAvailableAccount: Boolean, user: UserModel)
    }

    interface AttendanceHeartMoreListener {
        fun btnLevelMoreClick(dailyRewardModel: DailyRewardModel)
        fun btnAdsClick(dailyRewardModel: DailyRewardModel)
        fun btnLinkClick(dailyRewardModel: DailyRewardModel)
        fun btnVideoAdClick(dailyRewardModel: DailyRewardModel)
    }

    fun setAttendanceHeaderListener(listener: AttendanceHeaderListener) {
        this.attendanceHeaderListener = listener
    }

    fun setAttendanceHeartMoreListener(listener: AttendanceHeartMoreListener) {
        this.attendanceHeartMoreListener = listener
    }

    fun refreshGraphData(
        stamp: JSONObject,
        rewards: List<Map<String, StampRewardModel>>,
        isSuccessOfStamp: Boolean,
    ) {
        this.stamp = stamp
        this.rewards = rewards
        this.isSuccessOfStamp = isSuccessOfStamp
        notifyItemChanged(0)
    }

    fun setDailyRewards(dailyRewards: List<DailyRewardModel>) {
        this.dailyRewards = dailyRewards
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemAttendanceHeader: ItemAttendanceHeaderBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_attendance_header,
                parent,
                false,
            )

        val itemAttendanceHeartMore: ItemAttendanceHeartMoreBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_attendance_heart_more,
                parent,
                false,
            )

        val itemWarning: ItemWarningBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_warning,
                parent,
                false,
            )

        return when (viewType) {
            ATTENDANCE_HEADER -> {
                AttendanceHeaderVH(
                    itemAttendanceHeader,
                    user,
                    stamp,
                    rewards,
                    sharedAppState,
                ).apply {
                    binding.attendanceGraph.btnAttendanceCheck.setOnClickListener {

                        //비어있으면 첫 째날이다.
                        val showAvailableAccount = stamp.length() == 0
                        attendanceHeaderListener?.btnAttendanceCheckClick(
                            showAvailableAccount,
                            user
                        )

                        if (showAvailableAccount) {
                            binding.attendanceGraph.btnAttendanceCheck.isEnabled = true
                        }
                    }
                    // 푸시 설정 유도
                    // OS에서 푸시설정 안되어있으면
                    val isNotificationEnabled = NotificationManagerCompat.from(itemView.context).areNotificationsEnabled()
                    var show = false
                    if(!isNotificationEnabled) {
                        // 20% 확률로 노출
                        show = (0..100).random() < 20
                        if(BuildConfig.DEBUG) {
                            show = true
                        }
                        // x 눌러 닫은 경우
                        if(sharedAppState.isIncitePushHidden.value == true) {
                            show = false
                        }
                    }
                    binding.attendancePush.clIncitePush.visibility = if(show) View.VISIBLE else View.GONE

                    binding.attendancePush.clIncitePush.setOnClickListener {
                        UtilK.openNotificationSettings(itemView.context)
                    }
                    binding.attendancePush.ivClose.setOnClickListener {
                        // 한 번 닫으면 앱 재실행까지 다시 노출 안함
                        sharedAppState.setIncitePushHidden(true)
                    }
                }
            }

            ATTENDANCE_MORE_HEART -> {
                AttendanceHeartMoreVH(itemAttendanceHeartMore, lifecycleScope).apply {

                    heartMoreViewClickArray.forEach { view ->
                        view.setOnClickListener {

                            val dailyReward = dailyRewards[bindingAdapterPosition]

                            if (!dailyReward.alert.isNullOrEmpty()) {
                                IdolSnackBar.make(
                                    (itemView.context as AppCompatActivity).findViewById(android.R.id.content),
                                    dailyReward.alert
                                ).show()
                            }

                            // 해당 아이템이 완료 되거나, 오픈전일땐 클릭 안되게.
                            if (dailyReward.status == AttendanceHeartMoreVH.COMPLETE_STATUS || dailyReward.status == AttendanceHeartMoreVH.BEFORE_OPENING_STATUS || dailyReward.status == AttendanceHeartMoreVH.CLOSE_STATUS) {
                                return@setOnClickListener
                            }

                            when {
                                dailyRewards[bindingAdapterPosition].banner != null -> {
                                    //광고 배너 클릭 세팅.
                                    attendanceHeartMoreListener?.btnAdsClick(
                                        dailyRewards[bindingAdapterPosition]
                                    )
                                }

                                dailyRewards[bindingAdapterPosition].linkUrl != null -> {
                                    //링크 클릭 세팅.
                                    attendanceHeartMoreListener?.btnLinkClick(
                                        dailyRewards[bindingAdapterPosition]
                                    )
                                }

                                dailyRewards[bindingAdapterPosition].key.equals(DELIVERY_VIDEO) -> {
                                    attendanceHeartMoreListener?.btnVideoAdClick(
                                        dailyRewards[bindingAdapterPosition]
                                    )
                                }

                                else -> {
                                    //하트 보상 바텀 다이얼로그 보여주기.
                                    attendanceHeartMoreListener?.btnLevelMoreClick(
                                        dailyRewards[bindingAdapterPosition]
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                WarningVH(itemWarning)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            ATTENDANCE_HEADER -> {
                (holder as AttendanceHeaderVH).apply {
                    bind(isSuccessOfStamp).apply {
                        if (isSuccessOfStamp) {

                            val gson = IdolGson.getInstance()
                            val stampModel = gson.fromJson(stamp.toString(), StampModel::class.java)

                            val stampDays = (stampModel.days ?: 0) - 1
                            playStampLottie(
                                days = stampDays,
                                finishStampLottie = {
                                    setLastCompleteImageVisibilty(stampDays)
                                }
                            )
                        }
                    }
                    binding.executePendingBindings()
                }
            }

            ATTENDANCE_MORE_HEART -> {
                (holder as AttendanceHeartMoreVH).apply {
                    bind(dailyRewards[position], position)
                    binding.executePendingBindings()
                }
            }

            else -> {
                (holder as WarningVH).apply {
                    bind(dailyRewards[position])
                    binding.executePendingBindings()
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> {
                ATTENDANCE_HEADER
            }
            in 1 until dailyRewards.size - 1 -> {
                ATTENDANCE_MORE_HEART
            }
            else -> {
                ATTENDANCE_WARNING
            }
        }
    }

    companion object {

        const val ATTENDANCE_HEADER = 0
        const val ATTENDANCE_MORE_HEART = 1
        const val ATTENDANCE_WARNING = 2

        private const val DELIVERY_VIDEO = "daily_videoad"

        val diffUtil = object : DiffUtil.ItemCallback<DailyRewardModel>() {
            override fun areItemsTheSame(
                oldItem: DailyRewardModel,
                newItem: DailyRewardModel,
            ): Boolean {
                return oldItem.key == newItem.key
            }

            override fun areContentsTheSame(
                oldItem: DailyRewardModel,
                newItem: DailyRewardModel,
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}