package net.ib.mn.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import net.ib.mn.databinding.EmoticonItemBinding
import net.ib.mn.model.EmoticonDetailModel
import java.io.File


class EmoticonAdapter(
        private val mContext:Context,
        private val glideRequestManager: RequestManager,
        private val items: List<EmoticonDetailModel>,
        private val onItemClick: (EmoticonDetailModel, View, Int) -> Unit
) : RecyclerView.Adapter<EmoticonAdapter.ViewHolder>() {

    interface onItemClickListener{
        fun onItemClick(model: EmoticonDetailModel, view: View, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                EmoticonItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false)
            )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position],position)

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val binding: EmoticonItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: EmoticonDetailModel, position: Int) { with(binding) {
            //이모티콘 -> filapth uri 로 변환
            val uri = Uri.parse(item.filePath + ".webp")
            val file = File(uri.path)
            if (file.exists()) { //파일있으면 보여주기.
                glideRequestManager
                    .load(uri.path)
                    .transform(CenterCrop(), RoundedCorners(40))
                    .into(ivEmoticon)

                val listener = View.OnClickListener { view ->
                    onItemClick(item, view, position)
                }

                ivEmoticon.setOnClickListener(listener)
            } else { //없으면 null처리.
                ivEmoticon.setImageDrawable(null)
                ivEmoticon.setOnClickListener(null)
            }

        }}
    }
}