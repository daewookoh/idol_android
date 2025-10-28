package net.ib.mn.chatting

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ChattingRoomListItemDecoration : RecyclerView.ItemDecoration() {


    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
    }

    interface SectionCallback {
        fun isSection(position: Int): Boolean
        fun getHeaderLayoutView(list: RecyclerView, position: Int): View?
    }


}