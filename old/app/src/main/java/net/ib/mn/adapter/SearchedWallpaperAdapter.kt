package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.ItemSearchWallpaperBinding
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.model.WallpaperModel
import net.ib.mn.viewholder.SearchedWallpaperViewHolder

class SearchedWallpaperAdapter(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val getIdolByIdUseCase: GetIdolByIdUseCase,
    private val wallpaperModel: WallpaperModel,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: ItemSearchWallpaperBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_search_wallpaper,
            parent,
            false,
        )

        return SearchedWallpaperViewHolder(binding, wallpaperModel, lifecycleScope, getIdolByIdUseCase)
    }

    override fun getItemCount(): Int {
        return wallpaperModel.imageUrls?.size ?: 0
    }

    override fun getItemId(position: Int): Long {
        return wallpaperModel.imageUrls?.get(position).hashCode().toLong()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as SearchedWallpaperViewHolder).apply {
            bind(position)
        }
    }
}