package net.ib.mn.addon

import android.view.View
import android.widget.BaseAdapter
import android.widget.HeaderViewListAdapter
import android.widget.ListView

class HeaderFooterListAdapter(
    private val mHeaderViewInfos: ArrayList<ListView.FixedViewInfo?>,
    private val mFooterViewInfos: ArrayList<ListView.FixedViewInfo?>,
    private val mListView: ListView,
    private val mWrappedAdapter: BaseAdapter
) : HeaderViewListAdapter(mHeaderViewInfos, mFooterViewInfos, mWrappedAdapter) {
    constructor(
        listView: ListView,
        adapter: BaseAdapter
    ) : this(
        ArrayList<ListView.FixedViewInfo?>(),
        ArrayList<ListView.FixedViewInfo?>(),
        listView,
        adapter
    )

    @JvmOverloads
    fun addHeader(
        v: View,
        data: Any? = null,
        isSelectable: Boolean = false
    ): HeaderFooterListAdapter {
        addViewInfo(mHeaderViewInfos, v, data, isSelectable)
        return this
    }

    @JvmOverloads
    fun addFooter(
        v: View,
        data: Any? = null,
        isSelectable: Boolean = false
    ): HeaderFooterListAdapter {
        addViewInfo(mFooterViewInfos, v, data, isSelectable)
        return this
    }

    override fun removeFooter(v: View): Boolean {
        val removed = super.removeFooter(v)
        if (removed) {
            mWrappedAdapter.notifyDataSetChanged()
        }
        return removed
    }

    override fun removeHeader(v: View): Boolean {
        val removed = super.removeHeader(v)
        if (removed) {
            mWrappedAdapter.notifyDataSetChanged()
        }
        return removed
    }

    override fun getWrappedAdapter(): BaseAdapter {
        return mWrappedAdapter
    }

    private fun addViewInfo(
        infos: ArrayList<ListView.FixedViewInfo?>, view: View,
        data: Any?, isSelectable: Boolean
    ) {
        val info = mListView.FixedViewInfo()
        info.view = view
        info.data = data
        info.isSelectable = isSelectable
        infos.add(info)
        mWrappedAdapter.notifyDataSetChanged()
    }
}
