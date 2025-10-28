package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.activity.FriendSearchActivity
import net.ib.mn.databinding.ItemFriendBinding
import net.ib.mn.databinding.ItemNewFriendBinding
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import java.text.NumberFormat
import java.util.Locale


class SearchedUsersAdapter(
        private val mContext: Context,
        private val mGlideRequestManager: RequestManager,
        private val friends: MutableList<FriendModel>,
        private val notFriends: MutableList<FriendModel>,
        private var onClickListener: OnClickListener
) : RecyclerView.Adapter<SearchedUsersAdapter.ViewHolder>() {

    companion object {
        const val TYPE_FRIEND = 0
        const val TYPE_NOT_FRIEND = 1
    }

    interface OnClickListener {
        fun onItemClicked(item: UserModel, view: View, position: Int)
    }

    override fun getItemCount(): Int {
        return friends.size + notFriends.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (friends.size > 0 && position < friends.size) {
            TYPE_FRIEND
        } else {
            TYPE_NOT_FRIEND
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val viewHolder: ViewHolder
        val view: View

        when (viewType) {
            TYPE_FRIEND -> {
                val binding = ItemFriendBinding.inflate(
                    LayoutInflater.from(mContext),
                    parent, false)
                viewHolder = FriendViewHolder(binding)
            }
            else -> {
                val binding = ItemNewFriendBinding.inflate(
                    LayoutInflater.from(mContext),
                    parent, false)
                viewHolder = NotFriendViewHolder(binding)
            }
        }

        return viewHolder
    }

    inner class FriendViewHolder(val binding: ItemFriendBinding) : ViewHolder(binding.root) {
        override fun bind(item: FriendModel, position: Int) { with(binding) {
            val user = item.user

            val listener = View.OnClickListener { view ->
                onClickListener.onItemClicked(user, view, position)
            }

            if (position == 0) {
                val sectionTitleText =
                        "${mContext.getString(R.string.friend_section_title)} (${friends.size})"

                sectionTitle.text = sectionTitleText
                section.visibility = View.VISIBLE
            } else {
                section.visibility = View.GONE
            }

            val url = picture.loadInfo
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

                mGlideRequestManager.load(userImageUrl)
                        .apply(options)
                        .into(picture)
            }

            level.setImageBitmap(Util.getLevelImage(mContext, user))
            name.text = "\u200E${user.nickname}"

            if (user.heart == Const.LEVEL_MANAGER) {
                voteCount.visibility = View.GONE
                mostIdol.visibility = View.GONE
            } else {
                voteCount.visibility = View.VISIBLE
                mostIdol.visibility = View.VISIBLE

                if (user.most == null) {
                    mostIdol.text =
                            "${mContext.getString(R.string.most_favorite)} : ${mContext.getString(R.string.none)}"
                } else {
                    val most = user.most

                    val mostIdolText = mContext.getString(R.string.favorite_format)
                    mostIdol.text = String.format(mostIdolText, most?.getName(mContext))
                }


                val heartCount = String.format(mContext.getString(R.string.level_heart_format),
                                        NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                                                .format(user.levelHeart))
                voteCount.text = heartCount

                val userStatusMsg = user.statusMessage
                if (userStatusMsg.isNullOrEmpty() || user.heart == Const.LEVEL_MANAGER) {
                    statusMsg.visibility = View.GONE
                } else {
                    statusMsg.visibility = View.VISIBLE
                    statusMsg.text = userStatusMsg
                }
            }

            // 친구 검색에서는 안보여줌
            sectionBtnSendHeart.visibility = View.GONE
            sectionWaitTimer.visibility = View.GONE
            sectionBtnTakeHeart.visibility = View.GONE
//            sectionTakeWaitTimer.visibility = View.GONE
            btnSendHeart.visibility = View.GONE
            waitTimer.visibility = View.GONE
            helpTakeHeart.visibility = View.GONE

            picture.setOnClickListener(listener)
            userInfo.setOnClickListener(listener)
            btnSendHeart.setOnClickListener(listener)
        }}
    }

    inner class NotFriendViewHolder(val binding: ItemNewFriendBinding) : ViewHolder(binding.root) {
        override fun bind(item: FriendModel, position: Int) { with(binding) {
            val user = item.user

            val listener = View.OnClickListener { view ->
                onClickListener.onItemClicked(user, view, position)
            }

            if (friends.size == 0) {
                section.setPadding(Util.convertDpToPixel(mContext, 10f).toInt(),
                        Util.convertDpToPixel(mContext, 10f).toInt(),
                        Util.convertDpToPixel(mContext, 10f).toInt(),
                        Util.convertDpToPixel(mContext, 10f).toInt())
            } else {
                section.setPadding(Util.convertDpToPixel(mContext, 10f).toInt(),
                        Util.convertDpToPixel(mContext, 34f).toInt(),
                        Util.convertDpToPixel(mContext, 10f).toInt(),
                        Util.convertDpToPixel(mContext, 10f).toInt())
            }

            if (position == 0 ||
                    (position != 0 && notFriends[position - 1].isFriend == "Y")) {
                val sectionTitleText =
                        "'${(mContext as FriendSearchActivity).mKeyword}' ${mContext.getString(R.string.search_result)}"

                sectionTitle.text = sectionTitleText
                section.visibility = View.VISIBLE
            } else {
                section.visibility = View.GONE
            }

            val url = picture.loadInfo
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

                mGlideRequestManager.load(userImageUrl)
                        .apply(options)
                        .into(picture)
            }

            level.setImageBitmap(Util.getLevelImage(mContext, user))
            name.text = "\u200E${user.nickname}"

            if (user.heart == Const.LEVEL_MANAGER) {
                voteCount.visibility = View.GONE
                mostIdol.visibility = View.GONE
            } else {
                voteCount.visibility = View.VISIBLE
                mostIdol.visibility = View.VISIBLE

                var heartCount =
                        String.format(mContext.getString(R.string.heart_count_format), 0)

                if (user.most == null) {
                    mostIdol.text =
                            "${mContext.getString(R.string.most_favorite)} : ${mContext.getString(R.string.none)}"
                } else {
                    val most = user.most
                    val heartCountComma = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                            .format(user.levelHeart)

                    heartCount =
                            String.format(mContext.getString(R.string.heart_count_format), heartCountComma)

//                    most.setLocalizedName(mContext)

                    val mostIdolText = mContext.getString(R.string.favorite_format)
                    mostIdol.text = String.format(mostIdolText, most?.getName(mContext))

                }

                heartCount = String.format(mContext.getString(R.string.level_heart_format), heartCount)
                voteCount.text = heartCount

                val userStatusMsg = user.statusMessage
                if (userStatusMsg.isNullOrEmpty() || user.heart == Const.LEVEL_MANAGER) {
                    statusMsg.visibility = View.GONE
                } else {
                    statusMsg.visibility = View.VISIBLE
                    statusMsg.text = userStatusMsg
                }
            }

            if (item.userType == "") {
                btnReqFriend.background = ContextCompat.getDrawable(mContext, R.drawable.btn_add_friend_recommended)
                btnReqFriend.setOnClickListener(listener)
            } else {
                btnReqFriend.background = ContextCompat.getDrawable(mContext, R.drawable.btn_friend_friend_standby)
                btnReqFriend.setOnClickListener {
                    Util.showDefaultIdolDialogWithBtn1(
                            mContext,
                            null,
                            mContext.getString(R.string.error_8002)
                    ) {
                        Util.closeIdolDialog()
                    }
                }
            }

            picture.setOnClickListener(listener)
            userInfo.setOnClickListener(listener)
        }}
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (friends.size > 0 && position < friends.size) {
            holder.bind(friends[position], position)
        } else {
            holder.bind(notFriends[position - friends.size], position - friends.size)
        }
    }


    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal abstract fun bind(item: FriendModel, position: Int)
    }
}