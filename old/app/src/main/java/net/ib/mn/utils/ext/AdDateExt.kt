/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 광고 기간 관련 extension 함수
 *
 * */

package net.ib.mn.utils.ext

import android.content.Context
import net.ib.mn.R
import net.ib.mn.support.AdDate

/**
 * @see
 * */

fun String.getAdDatePeriod(context: Context): String {
    return when {
        this.contains(AdDate.DAY.value) -> this.formatPeriod(
            AdDate.DAY.value,
            R.string.date_format_day,
            R.string.date_format_days,
            context
        )

        this.contains(AdDate.WEEK.value) -> this.formatPeriod(
            AdDate.WEEK.value,
            R.string.date_format_week,
            R.string.date_format_weeks,
            context
        )

        this.contains(AdDate.MONTH.value) -> this.formatPeriod(
            AdDate.MONTH.value,
            R.string.date_format_month,
            R.string.date_format_months,
            context
        )

        else -> ""
    }
}

fun String.formatPeriod(
    period: String,
    singularResId: Int,
    pluralResId: Int,
    context: Context
): String {
    val periodValue = this.substring(0, this.indexOf(period)).toInt()
    return if (periodValue == 1) {
        context.getString(singularResId)
    } else {
        String.format(context.getString(pluralResId), periodValue)
    }
}
