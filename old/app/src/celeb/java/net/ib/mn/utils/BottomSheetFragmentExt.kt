package net.ib.mn.utils

import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.HighestVotesActivity
import net.ib.mn.activity.MainActivity
import net.ib.mn.activity.Top1CountActivity
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.fragment.HallOfFameFragment
import net.ib.mn.fragment.HighestVotesFragment
import net.ib.mn.fragment.Top1CountFragment
import net.ib.mn.onepick.AlternateLinkFragmentActivity

// CELEB ---
fun BottomSheetFragment.sethighestvoteTypeFilter(v: View) {
    val activity = context as HighestVotesActivity
    val top1Fragment =
        activity.supportFragmentManager
            .findFragmentByTag("highestVoteFragment") as HighestVotesFragment

    val container = v.findViewById<LinearLayoutCompat>(R.id.sheet_container)
    var typeList = UtilK.getTypeListArray(activity)
    typeList = typeList.filter { it.isViewable == "Y" || it.isViewable == "H" }
    typeList.forEach {
        val item = AppCompatTextView(activity)
        item.width = ViewGroup.LayoutParams.MATCH_PARENT
        item.height = Util.convertDpToPixel(activity, 48f).toInt()
        val outValue = TypedValue()
        context!!.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        item.setBackgroundResource(outValue.resourceId)
        item.gravity = Gravity.CENTER
        item.isClickable = true
        item.isFocusable = true
        item.setTextColor(resources.getColor(R.color.gray900))
        item.textSize = 15f
        item.setTypeface(item.typeface, Typeface.BOLD)
        item.text = it.name
        val backupName = it.name
        val backupTypeName = it.typeName
        item.setOnClickListener {
            top1Fragment.filterByType(backupName, backupTypeName)
            this.dismiss()
        }
        container.addView(item)
    }
}

fun BottomSheetFragment.setTop1TypeFilter(v: View) {
    val activity = context as Top1CountActivity
    val top1Fragment =
        activity.supportFragmentManager
            .findFragmentByTag(Top1CountActivity.TAG_TOP1_FRAGMENT) as Top1CountFragment

    val container = v.findViewById<LinearLayoutCompat>(R.id.sheet_container)
    var typeList = UtilK.getTypeListArray(activity)
    typeList = typeList.filter { it.isViewable == "Y" || it.isViewable == "H" }
    typeList.forEach {
        val item = AppCompatTextView(activity)
        item.width = ViewGroup.LayoutParams.MATCH_PARENT
        item.height = Util.convertDpToPixel(activity, 48f).toInt()
        val outValue = TypedValue()
        context!!.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        item.setBackgroundResource(outValue.resourceId)
        item.gravity = Gravity.CENTER
        item.isClickable = true
        item.isFocusable = true
        item.setTextColor(resources.getColor(R.color.gray900))
        item.textSize = 15f
        item.setTypeface(item.typeface, Typeface.BOLD)
        if (it.type == null) { //null이면 종합이란뜻이므로 top100 스트링 넣어줌.
            it.name = getString(R.string.top_100)
        }
        item.text = it.name
        val backupName = it.name
        val backupTypeName = it.typeName
        item.setOnClickListener { _ ->
            top1Fragment.filterByType(backupName, backupTypeName, it)
            this.dismiss()
        }
        container.addView(item)
    }
}

fun BottomSheetFragment.setTypeFilter(v: View) {

    val activity = context as BaseActivity
    val tag = if (activity is MainActivity) {
        TAG_HOF
    } else {
        AlternateLinkFragmentActivity.ALTERNATE_LINK_FRAGMENT_ACTIVITY
    }
    val hallOfFameFragment =
        activity.supportFragmentManager
            .findFragmentByTag(tag) as HallOfFameFragment

    val container = v.findViewById<LinearLayoutCompat>(R.id.sheet_container)
    var typeList = Util.setGenderTypeLIst(UtilK.getTypeListArray(activity), requireActivity())
    typeList = typeList.filter { it.isViewable == "Y" || it.isViewable == "H" }
    typeList.forEach {

        val item = AppCompatTextView(activity)
        item.width = ViewGroup.LayoutParams.MATCH_PARENT
        item.minHeight = Util.convertDpToPixel(activity, 48f).toInt()
        val outValue = TypedValue()
        context!!.theme.resolveAttribute(
            android.R.attr.selectableItemBackground,
            outValue,
            true
        )
        item.setBackgroundResource(outValue.resourceId)
        item.gravity = Gravity.CENTER
        item.isClickable = true
        item.isFocusable = true
        item.setTextColor(resources.getColor(R.color.gray900))
        item.textSize = 15f
        item.setTypeface(item.typeface, Typeface.BOLD)
        item.text = it.name
        val backupName = it.name
        val backupType = it.type
        item.setOnClickListener {
            hallOfFameFragment.filterByType(loaderId, backupType, backupName)
            this.dismiss()
        }
        container.addView(item)
    }
}
// end CELEB