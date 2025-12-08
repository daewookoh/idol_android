package net.ib.mn.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.ib.mn.R

/**
 * 스케줄 카테고리
 * Old 프로젝트의 ScheduleWriteCategoryActivity 카테고리 목록과 동일
 */
enum class ScheduleCategory(
    val code: String,
    @StringRes val labelResId: Int,
    @DrawableRes val iconResId: Int,
    val isAllDayOnly: Boolean = false
) {
    ANNIVERSARY("anniversary", R.string.schedule_desp_anniversary, R.drawable.schedule_category_01, true),
    ALBUM("albumday", R.string.schedule_desp_album, R.drawable.schedule_category_02),
    CONCERT("concert", R.string.schedule_desp_concert, R.drawable.schedule_category_03),
    EVENT("event", R.string.schedule_desp_event, R.drawable.schedule_category_04),
    SIGN("sign", R.string.schedule_desp_sign, R.drawable.schedule_category_05),
    TV("tv", R.string.schedule_desp_tv, R.drawable.schedule_category_06),
    RADIO("radio", R.string.schedule_desp_radio, R.drawable.schedule_category_07),
    LIVE("live", R.string.schedule_desp_video, R.drawable.schedule_category_08),
    AWARD("award", R.string.schedule_desp_awards, R.drawable.schedule_category_09),
    TICKETING("ticketing", R.string.schedule_desp_ticketing, R.drawable.schedule_category_11),
    ETC("etc", R.string.schedule_desp_etc, R.drawable.schedule_category_10);

    companion object {
        fun fromCode(code: String?): ScheduleCategory? {
            return entries.find { it.code == code }
        }
    }
}
