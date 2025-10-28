package net.ib.mn.awards

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.awards.adapter.AwardsCategoryAdapter
import net.ib.mn.awards.viewmodel.AwardsMainViewModel
import net.ib.mn.core.model.TagModel
import net.ib.mn.view.ArticleRecyclerView
import java.util.ArrayList

interface AwardsCategory {
    fun setCategories(
        context: Context,
        categoryRecyclerView: RecyclerView?,
        awardsMainViewModel: AwardsMainViewModel,
        clickListener:  AwardsCategoryAdapter. OnClickListener) {
        val tags: ArrayList<TagModel> = ArrayList()
        var index = 0
        awardsMainViewModel.getAwardData()?.charts?.forEach { chart ->
            val tag = TagModel(index, chart.name, "N", index == 0)
            tags.add(index, tag)
            index++
        }

        val tagAdapter = AwardsCategoryAdapter(context, tags, clickListener)
        categoryRecyclerView?.adapter = tagAdapter
    }
}