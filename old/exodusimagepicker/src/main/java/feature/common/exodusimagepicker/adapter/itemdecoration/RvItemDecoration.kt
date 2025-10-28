package feature.common.exodusimagepicker.adapter.itemdecoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 파일 리스트 아이템에  간격을 주기위해
 * recyclerview itemdecoration 적용
 *
 * @see
 * */
class RvItemDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        outRect.top = 0
        outRect.bottom = 2
        outRect.right = 2
        outRect.left = 2
    }
}