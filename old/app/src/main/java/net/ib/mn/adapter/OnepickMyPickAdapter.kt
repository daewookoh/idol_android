package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.ItemOnepickMyPickBinding
import net.ib.mn.onepick.OnepickMyPickActivity
import net.ib.mn.model.OnepickIdolModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.view.ExodusImageView


class OnepickMyPickAdapter(
        private val context: Context,
        private val glideRequestManager: RequestManager,
        private val pickIdolList: ArrayList<OnepickIdolModel>
) : RecyclerView.Adapter<OnepickMyPickAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOnepickMyPickBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return pickIdolList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val item = pickIdolList[position]
        if (position % 2 == 0) {
            clItemOnepickMyPick.setPadding(Util.convertDpToPixel(context, 28f).toInt(),
                    Util.convertDpToPixel(context, 8f).toInt(),
                    Util.convertDpToPixel(context, 4f).toInt(),
                    Util.convertDpToPixel(context, 8f).toInt())
        } else {
            clItemOnepickMyPick.setPadding(Util.convertDpToPixel(context, 14f).toInt(),
                    Util.convertDpToPixel(context, 8f).toInt(),
                    Util.convertDpToPixel(context, 4f).toInt(),
                    Util.convertDpToPixel(context, 8f).toInt())
        }

        val reqImageSize = Util.getOnDemandImageSize(context)
        val imageUrl = UtilK.onePickImageUrl(context, item.id, (context as OnepickMyPickActivity).date!!, reqImageSize)

        val idolId = item.idol!!.getId()
        glideRequestManager.load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .into(ivPhoto)

        UtilK.setName(context, item.idol!!, tvName, tvGroupName)
    }

    inner class ViewHolder(val binding: ItemOnepickMyPickBinding) : RecyclerView.ViewHolder(binding.root) {
    }
}