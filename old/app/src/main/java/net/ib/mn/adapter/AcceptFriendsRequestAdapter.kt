package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.databinding.ItemReceivedRequestFriendBinding
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Util


class AcceptFriendsRequestAdapter(
        private val context: Context,
        private val glideRequestManager: RequestManager,
        private val mItems: MutableList<FriendModel>,
        private val onClickListener: OnClickListener
) : RecyclerView.Adapter<AcceptFriendsRequestAdapter.ViewHolder>() {

    interface OnClickListener {
        fun onItemClicked(user: UserModel, view: View, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                ItemReceivedRequestFriendBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            )

    override fun getItemCount(): Int = mItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mItems[position], position)
    }

    inner class ViewHolder(val binding: ItemReceivedRequestFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendModel, position: Int) { with(binding) {
            val user = item.user
            val listener = View.OnClickListener { view ->
                onClickListener.onItemClicked(user, view, position)
            }

            if (position == 0) {
                requesterSection.visibility = View.VISIBLE
            } else {
                requesterSection.visibility = View.GONE
            }

            val url = requesterPicture.loadInfo
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
                        .into(requesterPicture)
            }

            requesterLevel.setImageBitmap(Util.getLevelImage(context, user))
            requesterName.text = "\u200E${user.nickname}"

            requesterPicture.setOnClickListener(listener)
            requesterInfo.setOnClickListener(listener)
            btnAcceptAll.setOnClickListener(listener)
            btnDecline.setOnClickListener(listener)
        }}
    }

}