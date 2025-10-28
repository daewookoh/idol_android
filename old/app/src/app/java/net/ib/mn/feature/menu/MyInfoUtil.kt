package net.ib.mn.feature.menu

import android.content.Context
import net.ib.mn.R
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util

fun getIconMenuBadgeResId(context: Context, menuItem: IconMenu): Int? {
    return when (menuItem) {
        IconMenu.ATTENDANCE -> {
            val isAbleAttendance =
                Util.getPreferenceBool(context, Const.PREF_IS_ABLE_ATTENDANCE, false)
            if (isAbleAttendance) R.drawable.icon_menu_new else null
        }

        IconMenu.EVENT -> {
            getEventBadgeResId(context)
        }

        IconMenu.STORE -> {
            if (ConfigModel.getInstance(context).showStoreEventMarker == "Y") {
                R.drawable.icon_menu_up
            } else null
        }

        IconMenu.NOTICE -> {
            getNoticeBadgeResId(context)
        }

        IconMenu.FREE_CHARGE -> {
            if (ConfigModel.getInstance(context).showFreeChargeMarker == "Y") {
                R.drawable.icon_menu_up
            } else null
        }

        else -> null
    }
}

fun isShowTextMenuBadge(context: Context, item: IconTextMenu): Boolean {
    return when (item) {
        IconTextMenu.NOTICE -> {
            val noticeList = Util.getPreference(context, Const.PREF_NOTICE_LIST).split(",").toTypedArray()
            val readNoticeId = Util.getPreference(context, Const.PREF_NOTICE_READ)
            val readNoticeArray = readNoticeId.split(",".toRegex()).toTypedArray()

            return if (noticeList.isNotEmpty() && readNoticeId == "") {
                true
            } else {
                val alreadyRead = noticeList.count { Util.isFoundString(it, readNoticeArray) }
                noticeList.size != alreadyRead
            }
        }

        IconTextMenu.STORE -> ConfigModel.getInstance(context).showStoreEventMarker == "Y"

        else -> false
    }
}

fun getEventBadgeResId(context: Context): Int? {
    val eventList = Util.getPreference(context, Const.PREF_EVENT_LIST).split(",").toTypedArray()
    val readNoticeId = Util.getPreference(context, Const.PREF_EVENT_READ)
    val readNoticeArray = readNoticeId.split(",".toRegex()).toTypedArray()

    return if (eventList.isNotEmpty() && readNoticeId == "") {
        R.drawable.icon_menu_up
    } else {
        val alreadyRead = eventList.count { Util.isFoundString(it, readNoticeArray) }
        if (eventList.size == alreadyRead) null else R.drawable.icon_menu_up
    }
}

fun getNoticeBadgeResId(context: Context): Int? {
    val noticeList = Util.getPreference(context, Const.PREF_NOTICE_LIST).split(",").toTypedArray()
    val readNoticeId = Util.getPreference(context, Const.PREF_NOTICE_READ)
    val readNoticeArray = readNoticeId.split(",".toRegex()).toTypedArray()

    return if (noticeList.isNotEmpty() && readNoticeId == "") {
        R.drawable.icon_menu_new
    } else {
        val alreadyRead = noticeList.count { Util.isFoundString(it, readNoticeArray) }
        if (noticeList.size == alreadyRead) null else R.drawable.icon_menu_new
    }
}