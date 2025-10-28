package net.ib.mn.utils

import android.util.Log
import android.view.View
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.data.repository.TimestampRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.domain.usecase.datastore.UpdateAdCountPrefsUseCase
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.fragment.RewardBottomSheetDialogFragment.Companion.newInstance
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.LocaleUtil.setLocale
import org.json.JSONObject
import javax.inject.Inject

class VideoAdUtil @Inject constructor(
    private val timestampRepository: TimestampRepository,
    private val usersRepository: UsersRepository,
    private val updateAdCountPrefsUseCase: UpdateAdCountPrefsUseCase
){
    @OptIn(DelicateCoroutinesApi::class)
    fun onVideoSawCommon(
        activity: BaseActivity?,
        isAdded: Boolean,
        type: String?,
        earnHeartsListener: IEarnHeartsListener?,
    ) {
        if (activity == null) {
            return
        }

        if (isAdded) {
            Util.showProgress(activity)
        }

        val listener: (JSONObject) -> Unit = { response ->
            /**
             * 광고 횟수 초과된 상태로 캐시 지우고 호출시 응답 여기로 옴
             * 실패시 여기로 타는듯?
             * **/
            Log.i("####", "$response")
            val isSuccess = response?.optBoolean("success") ?: true
            if (!isSuccess) {
                //video timer 관련 setting
                Util.setLocalVideoTimer(response, activity)

                // 캐시 지우고 사용하는 유저들 UI 처리 때문에 분기 추가
                GlobalScope.launch(Dispatchers.IO) {
                    val currentCount = response?.optInt("rewarded") ?: 0
                    val maxCount = response?.optInt("reward_limit") ?: 0

                    if (currentCount == maxCount) {
                        updateAdCountPrefsUseCase(
                            currentCount,
                            maxCount
                        ).collect{ }
                    }
                }
            }

            BaseActivity.FLAG_CLOSE_DIALOG = false // 하트적립/미적립 팝업 자동으로 닫힘 방지
            if (!response.optBoolean("success")) {
                //video timer 관련 setting

                Util.setLocalVideoTimer(response, activity)

                if (isAdded) {
                    val msg = ErrorControl.parseError(activity, response)
                    if (msg != null) {
                        Util.showDefaultIdolDialogWithBtn1(
                            activity,
                            null,
                            msg,
                            { v: View? -> Util.closeIdolDialog() },
                            true
                        )
                        Util.closeProgress()
                    }
                }
            } else {
                Util.setLocalVideoTimer(response, activity)

                if (isAdded) {
                    var heart = response.optInt("heart")
                    if (heart == 0) {
                        heart = ConfigModel.getInstance(activity).video_heart
                    }
                    val bonusHeart = response.optInt("videoad_bonus_heart")

                    val nextBonusHeart = response.optInt("next_videoad_max_bonus_heart")
                    ConfigModel.getInstance(activity).videoAdBonusHeart = nextBonusHeart

                    val rewardBottomSheetDialogFragment = newInstance(
                        resId = RewardBottomSheetDialogFragment.FLAG_VIDEO_REWARD,
                        bonusHeart = heart,
                        plusHeart = bonusHeart,
                    ) {
                        Util.surpriseDia(activity, response)
                        Unit
                    }

                    val tag = "reward_video"

                    val oldFrag = activity.supportFragmentManager.findFragmentByTag(tag)
                    if (oldFrag == null) {
                        rewardBottomSheetDialogFragment.show(
                            activity.supportFragmentManager,
                            tag
                        )
                    }

                    Util.closeProgress()

                    GlobalScope.launch(Dispatchers.IO) {
                        //적립된경우는 비광밴이 아니므로, 혹시 비광밴 값(-1) 이 적용되어있다면 0으로 reset 해준다.
                        if (Util.getPreferenceInt(
                                activity,
                                Const.AUTO_CLICK_COUNT_PREFERENCE_KEY,
                                0
                            ) == Const.AUTO_CLICKER_BAN_CONFIRM_VALUE
                        ) {
                            Util.setPreference(
                                activity,
                                Const.AUTO_CLICK_COUNT_PREFERENCE_KEY,
                                0
                            )
                        }

                        Util.setPreference(
                            activity,
                            Const.PREF_HEART_BOX_VIEWABLE,
                            true
                        ) // 비디오광고 시청 후 하트박스 가능하게

                        val currentCount = response.optInt("rewarded")
                        val maxCount = response.optInt("reward_limit")

                        updateAdCountPrefsUseCase(
                            currentCount,
                            maxCount
                        ).collect{ }
                    }
                }

                earnHeartsListener?.onEarnHearts()
            }
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            if (isAdded) {
                BaseActivity.FLAG_CLOSE_DIALOG = false // 하트적립/미적립 팝업 자동으로 닫힘 방지
                Toast.makeText(
                    activity,
                    R.string.desc_failed_to_connect_internet,
                    Toast.LENGTH_SHORT
                ).show()
                Util.closeProgress()
            }
        }

        // 비디오광고 화면에서 복귀시 로케일이 시스템 로케일로 바뀌는 경우가 있어서
        if (isAdded) setLocale(activity)

        //완료되기전에 서버로부터 시간가져오기.
        MainScope().launch {
            timestampRepository.get { date ->
                launch {
                    usersRepository.giveRewardHeart(
                        type,
                        date?.time,
                        listener,
                        errorListener
                    )
                }
            }
        }
    }
}