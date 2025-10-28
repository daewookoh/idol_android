package feature.common.exodusimagepicker.viewholder

import androidx.recyclerview.widget.RecyclerView
import feature.common.exodusimagepicker.databinding.ItemFileFolderBinding
import feature.common.exodusimagepicker.model.FolderModel

class FolderItemVH(val binding: ItemFileFolderBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(folder: FolderModel) {
        // folder 모델  데바연결
        binding.folderModel = folder
    }
}