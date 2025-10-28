package feature.common.exodusimagepicker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import feature.common.exodusimagepicker.R
import feature.common.exodusimagepicker.databinding.ItemFileFolderBinding
import feature.common.exodusimagepicker.model.FolderModel
import feature.common.exodusimagepicker.viewholder.FolderItemVH

class FolderRvAdapter : ListAdapter<FolderModel, FolderItemVH>(diffUtil) {

    private var onItemClickListener: ItemClickListener? = null

    // 폴더 아이템 전체 클릭
    interface ItemClickListener {
        fun onItemClickListener(position: Int) // 아이템 클릭시 -> 디테일 화면으로?
    }

    // 폴더 클릭
    fun setItemClickListener(itemCliCkListener: ItemClickListener) {
        this.onItemClickListener = itemCliCkListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderItemVH {
        val binding: ItemFileFolderBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_file_folder,
            parent,
            false,
        )
        return FolderItemVH(binding)
    }

    override fun onBindViewHolder(holder: FolderItemVH, position: Int) {
        holder.apply {
            bind(currentList[position])

            // 폴더 아이템 클릭시 -> 폴더모델 넘겨줌
            binding.root.setOnClickListener {
                onItemClickListener?.onItemClickListener(position)
            }

            binding.executePendingBindings()
        }
    }

    override fun getItemCount() = currentList.size

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<FolderModel>() {
            override fun areContentsTheSame(
                oldItem: FolderModel,
                newItem: FolderModel,
            ): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(
                oldItem: FolderModel,
                newItem: FolderModel,
            ): Boolean {
                return oldItem.folderId == newItem.folderId
            }
        }
    }
}