package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.ItemImagePick1stBinding
import net.ib.mn.databinding.ItemImagePickHeaderBinding
import net.ib.mn.databinding.ItemImagePickRankBinding
import net.ib.mn.model.OnepickIdolModel
import net.ib.mn.model.OnepickTopicModel
import net.ib.mn.onepick.viewholder.imagepick.ImagePick1stViewHolder
import net.ib.mn.onepick.viewholder.imagepick.ImagePickHeaderViewHolder
import net.ib.mn.onepick.viewholder.imagepick.ImagePickRankingViewHolder
import kotlin.collections.ArrayList

class OnepickResultRankingAdapter(
    private val rankingList: ArrayList<OnepickIdolModel>,
    private val topPickModel: OnepickTopicModel?,
    private var date: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    init {
        setHasStableIds(true)
    }

    private var listener: OnePickResultClickListener? = null

    interface OnePickResultClickListener {
        fun goCommunity(onePickIdolModel: OnepickIdolModel)
    }

    fun setOnePickResultClickListener(listener: OnePickResultClickListener) {
        this.listener = listener
    }

    override fun getItemCount(): Int {
        return rankingList.size
    }

    override fun getItemId(position: Int): Long {
        return rankingList[position].id.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_HEADER
            1 -> TYPE_1ST
            else -> TYPE_RANK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val itemHeaderImagePick: ItemImagePickHeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_image_pick_header,
                    parent,
                    false
                )

                ImagePickHeaderViewHolder(itemHeaderImagePick, topPickModel)
            }

            TYPE_1ST -> {
                val itemImagePick1st: ItemImagePick1stBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_image_pick_1st,
                    parent,
                    false,
                )
                ImagePick1stViewHolder(itemImagePick1st, date).apply {
                    itemView.setOnClickListener {
                        listener?.goCommunity(rankingList[bindingAdapterPosition])
                    }
                }
            }

            else -> {
                val itemImagePickRanking: ItemImagePickRankBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_image_pick_rank,
                    parent,
                    false,
                )
                ImagePickRankingViewHolder(itemImagePickRanking, date).apply {
                    itemView.setOnClickListener {
                        listener?.goCommunity(rankingList[bindingAdapterPosition])
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_HEADER -> {
                (holder as ImagePickHeaderViewHolder).apply {
                    bind()
                }
            }

            TYPE_1ST -> {
                (holder as ImagePick1stViewHolder).apply {
                    bind(rankingList[position])
                }
            }

            else -> {
                (holder as ImagePickRankingViewHolder).apply {
                    bind(rankingList[position], position)
                }
            }
        }

    }

    fun setDate(date: String) {
        this.date = date
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_1ST = 1
        const val TYPE_RANK = 2
    }
}