package net.ib.mn.onepick

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.ItemThemePickBinding
import net.ib.mn.databinding.ItemThemePickPrelaunchBinding
import net.ib.mn.model.ThemepickModel
import net.ib.mn.onepick.viewholder.themepick.ThemePickPrelaunchViewHolder
import net.ib.mn.onepick.viewholder.themepick.ThemePickViewHolder
import net.ib.mn.utils.setMargins

class ThemePickAdapter(
    private val glideRequestManager: RequestManager,
    private val isNew: Boolean,
    val prelaunchLick: (ThemepickModel) -> Unit,
) : ListAdapter<ThemepickModel, RecyclerView.ViewHolder>(diffUtil) {

    private lateinit var clickListener : ClickListener

    interface ClickListener {
        fun goThemePickRank(item: ThemepickModel)
        fun goThemePickResult(item: ThemepickModel)
        fun goStore()
    }

    fun setOnClickListener(listener: ClickListener) {
        this.clickListener = listener
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (currentList[position].status != 0) {
            THEME_PICK_ITEM
        } else {
            THEME_PICK_PRELAUNCH_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val view : ItemThemePickBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_theme_pick,
            parent,
            false
        )

        val prelaunchView: ItemThemePickPrelaunchBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_theme_pick_prelaunch,
            parent,
            false
        )
        return when(viewType) {
            THEME_PICK_ITEM -> {
                ThemePickViewHolder(view, clickListener)
            }
            else -> {
                ThemePickPrelaunchViewHolder(prelaunchView, prelaunchLick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType) {
            THEME_PICK_ITEM -> {
                (holder as ThemePickViewHolder).apply {
                    bind(currentList[position], glideRequestManager, isNew)
                    binding.executePendingBindings()

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
            else -> {
                (holder as ThemePickPrelaunchViewHolder).apply {
                    bind(currentList[position], glideRequestManager)
                    binding.executePendingBindings()

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
        }
    }

    companion object {
        const val THEME_PICK_ITEM = 0
        const val THEME_PICK_PRELAUNCH_ITEM = 1

        private val diffUtil = object : DiffUtil.ItemCallback<ThemepickModel>() {
            override fun areItemsTheSame(
                oldItem: ThemepickModel,
                newItem: ThemepickModel
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ThemepickModel,
                newItem: ThemepickModel
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

}