package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.ItemSupportTop5Binding
import net.ib.mn.databinding.SupportTop5HeaderBinding
import net.ib.mn.model.SupportTop5Model
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList


class SupportTop5Adapter(
        private val mContext: Context,
        private val mGlideRequestManager: RequestManager,
        private var mItems: ArrayList<SupportTop5Model>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var isOffset = 0

    companion object {
        const val TYPE_TOP = 0
        const val TYPE_RANK = 1
    }

    interface OnClickListener {
        fun onItemClicked(item: SupportTop5Model, view: View, position: Int)
    }

    fun setItems(@NonNull items: ArrayList<SupportTop5Model>){
        mItems = items
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_TOP
        }
        return TYPE_RANK
    }

    override fun getItemId(position: Int): Long {
        var id = 0L
        if( !mItems.isEmpty() ) {
            if( position == 0 ) {
                id = mItems[position].user.id.toLong() + 10000000L
            } else if(mItems.size >= position){
                id = mItems[position-isOffset].user.id.toLong()
            }
        }

        return id
    }

    override fun getItemCount(): Int {
        if(mItems.size == 0){
            return 0
        }
        return mItems.size + isOffset
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when(viewType){
            TYPE_TOP -> {
                val itemSupportTop5Header: SupportTop5HeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.support_top5_header,
                    parent,
                    false
                )
                isOffset = 0

                TopViewHolder(itemSupportTop5Header)
            }
            else -> {
                val itemSupportTop5: ItemSupportTop5Binding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_support_top5,
                    parent,
                    false
                )
                SupportTop5ViewHolder(itemSupportTop5)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
       if(mItems.isNullOrEmpty()) return

        when (holder.itemViewType) {
            TYPE_TOP -> {
                (holder as TopViewHolder).apply {
                    bind(mItems[position])
                }
            }
            else -> {
                (holder as SupportTop5ViewHolder).bind(mItems[position - isOffset], position)
            }

        }
    }

    inner class TopViewHolder(val binding: SupportTop5HeaderBinding) : RecyclerView.ViewHolder(binding.root)  {

        fun bind(item: SupportTop5Model) {

            val userId =item.user.id
            mGlideRequestManager
                    .load(item.user.imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(userId))
                    .fallback(Util.noProfileImage(userId))
                    .placeholder(Util.noProfileImage(userId))
                    .dontAnimate()
                    .into(binding.photo)
            //1위
            val countDiaFormat  =  NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(item.diamond)
            binding.diamonCount.text = String.format(mContext.getString(R.string.heart_count_format),countDiaFormat)
            binding.name.text = item.user.nickname
            binding.level.setImageBitmap(Util.getLevelImage(mContext,item.user))
        }

    }
    inner class SupportTop5ViewHolder(val binding: ItemSupportTop5Binding) : RecyclerView.ViewHolder(binding.root)  {

        fun bind(item: SupportTop5Model, position: Int) {

            // 동점자 처리
            val rank: Int = item.rank

            if (rank < 3) {
                when (rank) {
                    1 -> binding.iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                    2 -> binding.iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
                }
            } else {
                binding.iconRanking.setImageDrawable(null)
            }

            val userId =item.user.id
            mGlideRequestManager
                    .load(item.user.imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(userId))
                    .fallback(Util.noProfileImage(userId))
                    .placeholder(Util.noProfileImage(userId))
                    .dontAnimate()
                    .into(binding.photo)
            //2~4위
            val countDiaFormat  =  NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(item.diamond)
            binding.diamonCount.text = String.format(mContext.getString(R.string.heart_count_format),countDiaFormat)
            binding.name.text = item.user.nickname
            binding.rank.text = String.format(mContext.getString(R.string.rank_format), (rank + 1).toString())
            binding.level.setImageBitmap(Util.getLevelImage(mContext,item.user))


        }

    }

//    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        internal abstract fun bind(item: SupportTop5Model, position: Int)
//    }

}