package feature.common.exodusimagepicker.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import feature.common.exodusimagepicker.adapter.FilePagingAdapter.Companion.MULTI_PICKER_TYPE
import feature.common.exodusimagepicker.databinding.ItemPhotoFileBinding
import feature.common.exodusimagepicker.model.FileModel
import feature.common.exodusimagepicker.util.GlobalVariable
import feature.common.exodusimagepicker.util.convertTimeMillsToTimerFormat

class PhotoItemVH(
    val binding: ItemPhotoFileBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: FileModel?, pickerType: Int) {
        // 선택했던 경우에는 체크 아이콘 visible
        if (item?.isSelected == true) {
            binding.filterSelected.visibility = View.VISIBLE // 선택 필터처리
            binding.ivCheckedIcon.visibility = View.VISIBLE
            if (pickerType == MULTI_PICKER_TYPE) { // 멀티피커일때
                binding.cgMultiCheck.visibility = View.VISIBLE
                binding.ivMultiUnCheckIcon.visibility = View.GONE
            }
        } else {
            binding.filterSelected.visibility = View.GONE
            binding.ivCheckedIcon.visibility = View.GONE
            if (pickerType == MULTI_PICKER_TYPE) {
                binding.cgMultiCheck.visibility = View.GONE
                binding.ivMultiUnCheckIcon.visibility = View.VISIBLE
            }
        }

        // 비디오 파일인 경우에는  비디오 총 시간을 보여줌.
        if (item?.isVideoFile == true) {
            binding.tvVideoTime.visibility = View.VISIBLE
            binding.tvVideoTime.convertTimeMillsToTimerFormat(item.duration ?: 0L)
        } else {
            binding.tvVideoTime.visibility = View.GONE
        }

        // 멀티 피커 타입일때 싱글 피커 아이콘 없애주고,  멀티피커 적용 함.
        if (pickerType == MULTI_PICKER_TYPE) {
            binding.ivCheckedIcon.visibility = View.GONE

            binding.tvCounter.text = if (GlobalVariable.selectedFileIds.any { it == item?.id }) {
                (GlobalVariable.selectedFileIds.indexOf(item?.id) + 1).toString()
            } else {
                ""
            }
        } else { // 멀티피커가 아니면  멀티 피커 아이콘 gone 처리
            binding.cgMultiCheck.visibility = View.GONE
            binding.ivMultiUnCheckIcon.visibility = View.GONE
            // 선택했던 경우에는 체크 아이콘 visible
            if (item?.isSelected == true) {
                binding.ivCheckedIcon.visibility = View.VISIBLE
            } else {
                binding.ivCheckedIcon.visibility = View.GONE
            }
        }

        // 상대경로 파일 glide 적용
        Glide.with(itemView.context)
            .load(item?.relativePath)
            .into(binding.ivPhoto)
    }
}