package net.ib.mn.adapter

import android.content.Context
import android.text.util.Linkify
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import net.ib.mn.R
import net.ib.mn.activity.WebViewActivity.Companion.createIntent
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.model.NoticeModel
import net.ib.mn.utils.Util

class NoticeBaseAdapter(context: Context,
    private val keyRead: String?) :
    ArrayAdapter<NoticeModel?>(context, R.layout.notice_base_item) {

    override fun update(view: View?, item: NoticeModel?, position: Int) {
        val titleView = view?.findViewById<TextView>(R.id.title)
        val tvNewEvent = view?.findViewById<ImageView>(R.id.new_event)
        titleView?.text = item?.title

        val readNoticeIds = Util.getPreference(context, keyRead)
        val readNoticeArray = readNoticeIds.split(",").toTypedArray()
        val readEvent = Util.isFoundString(item?.id, readNoticeArray)
        if (readEvent) {
            tvNewEvent?.visibility = View.INVISIBLE
        } else {
            tvNewEvent?.visibility = View.VISIBLE
        }
    }
}