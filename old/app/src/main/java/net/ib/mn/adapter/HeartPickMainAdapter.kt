package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.ItemHeartPickMainBinding
import net.ib.mn.databinding.ItemHeartPickPrelaunchBinding
import net.ib.mn.databinding.ItemLoadingBinding
import net.ib.mn.model.HeartPickModel
import net.ib.mn.smalltalk.viewholder.LoadingVH
import net.ib.mn.utils.getCurrentDateTime
import net.ib.mn.utils.setMargins
import net.ib.mn.viewholder.HeartPickMainViewHolder
import net.ib.mn.viewholder.HeartPickPrelaunchViewHolder

class HeartPickMainAdapter(
    private val isNew: Boolean,
    private val onItemClickListener: OnItemClickListener,
    private val lifecycleScope: LifecycleCoroutineScope
) : ListAdapter<HeartPickModel, RecyclerView.ViewHolder>(diffUtil) {

    interface OnItemClickListener {
        fun onItemClick(id: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemHeartPickMain: ItemHeartPickMainBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_heart_pick_main,
            parent,
            false,
        )

        val itemLoading: ItemLoadingBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_loading,
                parent,
                false
            )

        val itemHeartPickPrelaunch: ItemHeartPickPrelaunchBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_heart_pick_prelaunch,
            parent,
            false
        )

        return when (viewType) {
            HEART_PICK_ITEM -> {
                HeartPickMainViewHolder(itemHeartPickMain, getCurrentDateTime(), onItemClickListener, lifecycleScope)
            }
            HEART_PICK_PRELAUNCH_ITEM -> {
                HeartPickPrelaunchViewHolder(itemHeartPickPrelaunch, getCurrentDateTime(), {})
            }
            else -> {
                LoadingVH(itemLoading)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (currentList[position]?.isLoading == true) {
            LOADING_ITEM
        } else if (currentList[position]?.status == 0) {
            HEART_PICK_PRELAUNCH_ITEM
        } else {
            HEART_PICK_ITEM
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    fun setHeartPickModel(heartPickModel: HeartPickModel) {
        val newList = currentList.toMutableList()
        val position = currentList.indexOfFirst { it.id == heartPickModel.id }
        if (position >= 0 && newList.isNotEmpty()) {
            newList[position] = heartPickModel
            submitList(newList)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType) {
            HEART_PICK_ITEM -> {
                (holder as HeartPickMainViewHolder).apply {
                    bind(currentList[position], isNew)

                    if (!BuildConfig.CELEB) {
                        val margin = if (position == currentList.size - 1) {
                            9f
                        } else {
                            0f
                        }
                        holder.itemView.setMargins(bottom = margin)
                    }
                }
            }
            HEART_PICK_PRELAUNCH_ITEM -> {
                (holder as HeartPickPrelaunchViewHolder).apply {
                    bind(currentList[position])
                }
            }
            else -> {
                (holder as LoadingVH)
            }
        }
    }

    //로딩 지워줌.
    fun deleteLoading() {
        try {
            if (currentList[currentList.lastIndex]?.isLoading == true) {
                val lastIndex = currentList.lastIndex
                val newList = currentList.toMutableList()
                newList.removeAt(lastIndex)// 로딩이 완료되면 프로그레스바를 지움
                submitList(newList.map { it.copy() })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val HEART_PICK_ITEM = 0
        const val LOADING_ITEM = -1
        const val HEART_PICK_PRELAUNCH_ITEM = 1

        val diffUtil = object : DiffUtil.ItemCallback<HeartPickModel>() {
            override fun areItemsTheSame(
                oldItem: HeartPickModel,
                newItem: HeartPickModel
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: HeartPickModel,
                newItem: HeartPickModel
            ): Boolean {
                return oldItem == newItem
            }

        }
    }
}