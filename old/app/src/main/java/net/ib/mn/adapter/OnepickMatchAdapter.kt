package net.ib.mn.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import net.ib.mn.R
import net.ib.mn.databinding.ItemOnepickMatchBinding
import net.ib.mn.onepick.OnepickMatchActivity
import net.ib.mn.model.OnepickIdolModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.view.ExodusImageView


class OnepickMatchAdapter(
        private val context: Context,
        private val glideRequestManager: RequestManager,
        private val imageSize: Int,
        private val roundIdolList: ArrayList<OnepickIdolModel>
) : RecyclerView.Adapter<OnepickMatchAdapter.ViewHolder>() {
    var countLoadRequest = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOnepickMatchBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return roundIdolList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        // 새로운 아이템들로 바인딩할 때, loadRequestCount를 0으로 초기화 함
        if (position == 0 && countLoadRequest == OnepickMatchActivity.SIZE_OF_A_MATCH) {
            countLoadRequest = 0
        }

        val onepickModel: OnepickIdolModel = roundIdolList[position]

        val reqImageSize = Util.getOnDemandImageSize(context)
        val date = (context as OnepickMatchActivity).date
        val imageUrl = UtilK.onePickImageUrl(context, onepickModel.id, date, reqImageSize)

        glideRequestManager.load(imageUrl)
                .override(imageSize)
                .error(R.drawable.bg_loading)
                .fallback(R.drawable.bg_loading)
                .placeholder(R.drawable.bg_loading)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?,
                                              model: Any?,
                                              target: Target<Drawable>,
                                              isFirstResource: Boolean): Boolean {
                        countLoadRequest += 1
                        // error로 들어간 경우에 대해서 click이 가능하게끔 해주려고 추가
                        if (onepickModel.idol != null && imageUrl != onepickModel.imageUrl) {
                            binding.eivPhoto.visibility = View.VISIBLE
                            binding.eivPhoto.setOnClickListener {
                                if (roundIdolList.size >= holder.bindingAdapterPosition) {
                                    (context as OnepickMatchActivity).goNextRound(binding.eivPhoto, onepickModel)
                                }
                            }
                        }
                        return false
                    }

                    override fun onResourceReady(resource: Drawable,
                                                 model: Any,
                                                 target: Target<Drawable>,
                                                 dataSource: DataSource,
                                                 isFirstResource: Boolean): Boolean {
                        countLoadRequest += 1
                        if (onepickModel.idol != null) {
                            binding.eivPhoto.visibility = View.VISIBLE
                            binding.eivPhoto.setOnClickListener {
                                if (roundIdolList.size >= holder.bindingAdapterPosition) {
                                    (context as OnepickMatchActivity).goNextRound(binding.eivPhoto, onepickModel)
                                }
                            }
                        }
                        return false
                    }

                })
                .into(binding.eivPhoto)
    }

    inner class ViewHolder(val binding: ItemOnepickMatchBinding) : RecyclerView.ViewHolder(binding.root) {
    }
}