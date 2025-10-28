package net.ib.mn.utils

import android.view.View
import android.widget.TextView
import net.ib.mn.BuildConfig
import net.ib.mn.model.IdolModel
import java.text.NumberFormat
import java.util.Locale

// 기부천사/기부요정 아이콘 설정 -> 아랍어,페르시아일 경우 숫자 바꾸기 위함
fun setIdolBadgeIcon(
    iconAngel: TextView,
    iconFairy: TextView,
    iconMiracle: TextView,
    iconRookie: TextView,
    iconSuperRookie: TextView,
    idol: IdolModel
) {
    try {
        if (idol.angelCount > 0) {
            val iconAngelCount =
                NumberFormat.getNumberInstance(Locale.getDefault()).format(idol.angelCount.toLong())
            iconAngel.text = iconAngelCount
            iconAngel.visibility = View.VISIBLE
        } else {
            iconAngel.visibility = View.GONE
        }

        if (idol.fairyCount > 0) {
            val iconFairyCount =
                NumberFormat.getNumberInstance(Locale.getDefault()).format(idol.fairyCount.toLong())
            iconFairy.text = iconFairyCount
            iconFairy.visibility = View.VISIBLE
        } else {
            iconFairy.visibility = View.GONE
        }

        if (idol.miracleCount > 0) {
            val iconMiracleCount = NumberFormat.getNumberInstance(Locale.getDefault())
                .format(idol.miracleCount.toLong())
            iconMiracle.text = iconMiracleCount
            iconMiracle.visibility = View.VISIBLE
        } else {
            iconMiracle.visibility = View.GONE
        }

        if (BuildConfig.CELEB) {
            iconSuperRookie.visibility = View.GONE
            iconRookie.visibility = View.GONE
        } else {
            if (idol.rookieCount > 2) {
                iconSuperRookie.visibility = View.VISIBLE
                iconRookie.visibility = View.GONE
            } else if (idol.rookieCount > 0) {
                val iconRookieCount = NumberFormat.getNumberInstance(Locale.getDefault())
                    .format(idol.rookieCount.toLong())
                iconRookie.text = iconRookieCount
                iconRookie.visibility = View.VISIBLE
                iconSuperRookie.visibility = View.INVISIBLE
            } else {
                iconRookie.visibility = View.GONE
                iconSuperRookie.visibility = View.INVISIBLE
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}