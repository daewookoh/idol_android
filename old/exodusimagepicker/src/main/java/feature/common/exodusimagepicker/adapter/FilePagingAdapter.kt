package feature.common.exodusimagepicker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import feature.common.exodusimagepicker.R
import feature.common.exodusimagepicker.databinding.ItemPhotoFileBinding
import feature.common.exodusimagepicker.model.FileModel
import feature.common.exodusimagepicker.viewholder.PhotoItemVH

class FilePagingAdapter : PagingDataAdapter<FileModel, PhotoItemVH>(diffCallback = diffCallback) {

    private var onItemClickListener: ItemClickListener? = null

    // 피커 타입 체크
    private var pickerType = SINGLE_PICKER_TYPE

    // 아이템 전체 클릭
    interface ItemClickListener {
        fun onItemClickListener(relativePath: String, fileModel: FileModel, position: Int, isMultiPickerType: Boolean) // 아이템 클릭시 -> 디테일 화면으로?
    }

    // 아이템 전체 클릭
    fun setItemClickListener(itemCliCkListener: ItemClickListener) {
        this.onItemClickListener = itemCliCkListener
    }

    // 피커 타입 체인지
    fun changePickerType(pickerType: Int) {
        this.pickerType = pickerType
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: PhotoItemVH, position: Int) {
        val item = getItem(position)

        // 리스트 파일 하나 클릭시
        holder.binding.ivPhoto.setOnClickListener {
            if (item?.relativePath != null) {
                onItemClickListener?.onItemClickListener(
                    item.relativePath,
                    item,
                    position,
                    pickerType == MULTI_PICKER_TYPE,
                )
            }
        }

        holder.bind(item, pickerType)
        holder.binding.executePendingBindings()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoItemVH {
        val binding: ItemPhotoFileBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_photo_file,
                parent,
                false,
            )
        return PhotoItemVH(binding)
    }

    companion object {

        const val SINGLE_PICKER_TYPE = 0
        const val MULTI_PICKER_TYPE = 1

        val diffCallback = object : DiffUtil.ItemCallback<FileModel>() {
            override fun areItemsTheSame(oldItem: FileModel, newItem: FileModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: FileModel, newItem: FileModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}