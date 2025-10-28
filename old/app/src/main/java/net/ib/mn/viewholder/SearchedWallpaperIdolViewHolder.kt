package net.ib.mn.viewholder

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.adapter.SearchedWallpaperAdapter
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.ItemSearchWallpaperIdolBinding
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.model.WallpaperModel
import net.ib.mn.model.toPresentation

class SearchedWallpaperIdolViewHolder(
    val binding: ItemSearchWallpaperIdolBinding,
    private val wallpaperIdolSize: Int,
    val lifecycleScope: LifecycleCoroutineScope,
    val getIdolByIdUseCase: GetIdolByIdUseCase
    ) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var searchedWallpaperAdapter: SearchedWallpaperAdapter

    fun bind(wallpaperModel: WallpaperModel, position: Int) {
        setAdapter(wallpaperModel)
        setCategory(wallpaperModel)
        setMargins(itemView, 0, 0, 0, 30)
    }

    private fun setCategory(wallpaperModel: WallpaperModel) = with(binding) {
        lifecycleScope.launch(Dispatchers.IO) {
            val idol = getIdolByIdUseCase(wallpaperModel.idolId)
                .mapDataResource { it?.toPresentation() }
                .awaitOrThrow()

            withContext(Dispatchers.Main) {
                idol?.let {
                    tvCategory.text = it.getName(itemView.context)
                }
            }
        }
    }

    private fun setAdapter(wallpaperModel: WallpaperModel) = with(binding) {
        searchedWallpaperAdapter = SearchedWallpaperAdapter(lifecycleScope, getIdolByIdUseCase, wallpaperModel)
        searchedWallpaperAdapter.setHasStableIds(true)
        rvWallpaper.adapter = searchedWallpaperAdapter
    }

    // 뷰의 마진을 코드로 설정하기 위한  기능
    private fun setMargins(
        view: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = view.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }
}