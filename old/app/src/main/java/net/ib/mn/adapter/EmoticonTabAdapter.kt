package net.ib.mn.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import net.ib.mn.R
import net.ib.mn.databinding.EmoticonTabItemBinding
import net.ib.mn.model.EmoticonDetailModel
import net.ib.mn.model.EmoticonsetModel


class EmoticonTabAdapter(
    private val glideRequestManager: RequestManager,
    private val items: ArrayList<EmoticonsetModel>,
    private val emoAllInfoList: ArrayList<EmoticonDetailModel>,
    private val onItemClick: (ArrayList<EmoticonsetModel>,ArrayList<EmoticonDetailModel> ,View, Int) -> Unit
) : RecyclerView.Adapter<EmoticonTabAdapter.ViewHolder>() {

    interface onItemClickListener{
        fun onItemClick(setItems: ArrayList<EmoticonsetModel>, emoAllInfoList: ArrayList<EmoticonDetailModel>, view: View?, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val emoticonTapItem: EmoticonTabItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.emoticon_tab_item,
            parent,
            false
        )
        return ViewHolder(emoticonTapItem)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(items,emoAllInfoList,position)

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val binding: EmoticonTabItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(setItems: ArrayList<EmoticonsetModel>, emoAllInfoList:ArrayList<EmoticonDetailModel>,position: Int) {
            val listener = View.OnClickListener { view ->
                onItemClick(setItems, emoAllInfoList , view, position)
            }

            binding.liTabEmoticon.setOnClickListener(listener)
        }
    }
}