package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.ItemImagePickBinding
import net.ib.mn.databinding.ItemImagePickPrelaunchBinding
import net.ib.mn.model.OnepickTopicModel
import net.ib.mn.onepick.viewholder.imagepick.ImagePickPrelaunchViewHolder
import net.ib.mn.onepick.viewholder.imagepick.ImagePickViewHolder
import net.ib.mn.utils.setMargins


class OnePickTopicAdapter(
    private val isNew: Boolean
): ListAdapter<OnepickTopicModel, RecyclerView.ViewHolder>(diffUtil) {

    private lateinit var clickListener: ClickListener

    interface ClickListener {
        fun goOnePickMatch(item : OnepickTopicModel)
        fun goOnePickResult(item : OnepickTopicModel)
        fun goStore()
        fun showVideoAd(item: OnepickTopicModel)
        fun setNotification(item: OnepickTopicModel)
    }

    fun setOnClickListener(listener: ClickListener) {
        this.clickListener = listener
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (currentList[position].status != 0) {
            IMAGE_PICK_ITEM
        } else {
            IMAGE_PICK_PRELAUNCH_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: ItemImagePickBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_image_pick,
            parent,
            false
        )

        val prelaunchView: ItemImagePickPrelaunchBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_image_pick_prelaunch,
            parent,
            false
        )

        return when(viewType) {
            IMAGE_PICK_ITEM -> {
                ImagePickViewHolder(view, clickListener)
            }
            else -> {
                ImagePickPrelaunchViewHolder(prelaunchView, clickListener)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType) {
            IMAGE_PICK_ITEM -> {
                (holder as ImagePickViewHolder).apply {
                    bind(currentList[position], isNew)

                    if (!BuildConfig.CELEB) {
                        val margin = if (position == currentList.size - 1) {
                            9f
                        } else {
                            0f
                        }
                        holder.itemView.setMargins(bottom = margin)
                    }

                    binding.executePendingBindings()
                }
            }
            else -> {
                (holder as ImagePickPrelaunchViewHolder).apply {
                    bind(currentList[position], position)

                    if (!BuildConfig.CELEB) {
                        val margin = if (position == currentList.size - 1) {
                            9f
                        } else {
                            0f
                        }
                        holder.itemView.setMargins(bottom = margin)
                    }

                    binding.executePendingBindings()
                }
            }
        }
    }

    companion object {
        const val IMAGE_PICK_ITEM = 0
        const val IMAGE_PICK_PRELAUNCH_ITEM = 1

        val diffUtil = object : DiffUtil.ItemCallback<OnepickTopicModel>() {
            override fun areContentsTheSame(
                oldItem: OnepickTopicModel,
                newItem: OnepickTopicModel
            ): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(
                oldItem: OnepickTopicModel,
                newItem: OnepickTopicModel
            ): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}