package net.ib.mn.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.ItemSentRequestFriendBinding
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Util


class CancelFriendsRequestAdapter(
        private val context: Context,
        private val glideRequestManager: RequestManager,
        private val mItems: MutableList<FriendModel>,
        private val onClickListener: OnClickListener
) : RecyclerView.Adapter<CancelFriendsRequestAdapter.ViewHolder>() {

    interface OnClickListener {
        fun onItemClicked(user: UserModel, view: View, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                ItemSentRequestFriendBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            )

    override fun getItemCount(): Int = mItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mItems[position], position)
    }

    inner class ViewHolder(val binding: ItemSentRequestFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendModel, position: Int) { with(binding) {
            val user = item.user
            val listener = View.OnClickListener { view ->
                onClickListener.onItemClicked(user, view, position)
            }

            val url = photo.loadInfo
            var userImageUrl = user.imageUrl
            if (userImageUrl != null) {
                val idx = userImageUrl.lastIndexOf("v=")
                if (idx >= 0) {
                    userImageUrl = userImageUrl.substring(0, idx)
                }
            }
            val userId = item.user.id
            if (url == null || url != userImageUrl) {
                val options = RequestOptions()
                        .circleCrop()
                        .error(Util.noProfileImage(userId))
                        .fallback(Util.noProfileImage(userId))
                        .placeholder(Util.noProfileImage(userId))

                glideRequestManager.load(userImageUrl)
                        .apply(options)
                        .into(photo)
            }

            level.setImageBitmap(Util.getLevelImage(context, user))
            name.text = "\u200E${user.nickname}"

            var favoriteText: String
            if (user.most != null && !TextUtils.isEmpty(user.most?.getName(context))) {
//                user.most.setLocalizedName(context)
                favoriteText = context.getString(R.string.favorite_format)
                favoriteText = String.format(favoriteText, user.most?.getName(context))
            } else {
                favoriteText =
                        "${context.getString(R.string.most_favorite)} : ${context.getString(R.string.none)}"
            }
            favorite.text = favoriteText

            photo.setOnClickListener(listener)
            info.setOnClickListener(listener)
            btnCancel.setOnClickListener(listener)
        }}
    }

}