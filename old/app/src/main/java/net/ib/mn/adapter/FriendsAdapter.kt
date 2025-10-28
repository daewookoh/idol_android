package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.CommentOnlyActivity
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.databinding.ItemFriendBinding
import net.ib.mn.databinding.ItemNewFriendBinding
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import net.ib.mn.support.SupportPhotoCertifyActivity
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.view.ExodusImageView
import java.text.NumberFormat
import java.util.*


class FriendsAdapter(
        private val mContext: Context,
        private val mGlideRequestManager: RequestManager,
        private val mItems: MutableList<FriendModel>,
        private val isFriendActivity: Boolean,
        private var onClickListener: OnClickListener
) : RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {

    companion object {
        const val TYPE_FRIEND = 0
        const val TYPE_SEARCH_RESULT = 1
    }

    interface OnClickListener {
        fun onItemClicked(item: UserModel, view: View, position: Int)
    }

    override fun getItemCount(): Int = mItems.size

    override fun getItemViewType(position: Int): Int =
            when (mItems[position].isFriend) {
                "Y" -> TYPE_FRIEND
                else -> TYPE_SEARCH_RESULT
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        lateinit var viewHolder: ViewHolder
        val view: View

        when (viewType) {
            TYPE_FRIEND -> {
                val binding: ItemFriendBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_friend,
                    parent,
                    false
                )
                viewHolder = FriendViewHolder(binding)
            }
            TYPE_SEARCH_RESULT -> {
                val binding: ItemNewFriendBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_new_friend,
                    parent,
                    false
                )
                viewHolder = NewFriendsViewHolder(binding)
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = mItems[position]

        holder.bind(friend, position)
    }

    inner class FriendViewHolder(val binding: ItemFriendBinding) : ViewHolder(binding.root) {
        override fun bind(item: FriendModel, position: Int) {
            val user = item.user

            val listener = View.OnClickListener { view ->
                onClickListener.onItemClicked(user, view, position)
            }

            if (position == 0 ||
                    (position != 0 && mItems[position - 1].isFriend == "N")) {
                val sectionTitleText =
                        "${mContext.getString(R.string.friend_section_title)} (${mItems.size - position})"

                binding.sectionTitle.text = sectionTitleText
                binding.section.visibility = View.VISIBLE


                //comment activity 에서도  해당 아이템을 같이 쓰고 있으므로
                //친구 태그 리스트에서 띄어주는 용도로 쓰고 있음
                //그래서 처음  왔을때  툴팁의 경우 CommentActivity, SupportPhotoCertifyActivity 에서는  띄어주지 않는다.
                if(Util.getPreferenceBool(mContext, Const.PREF_SHOW_TAKE_HEART_FRIENDS,true)
                    && mContext.javaClass != SupportPhotoCertifyActivity::class.java
                    && mContext.javaClass != NewCommentActivity::class.java
                    && mContext.javaClass != CommentOnlyActivity::class.java
                ){
                    binding.helpTakeHeart.visibility = View.VISIBLE
                    binding.helpTakeHeart.bringToFront()
                    binding.helpTakeHeart.apply {
                        setOnClickListener {
                            binding.helpTakeHeart.visibility = View.GONE
                            Util.setPreference(mContext, Const.PREF_SHOW_TAKE_HEART_FRIENDS, false)
                        }
                    }
                }else{
                    binding.helpTakeHeart.visibility = View.GONE
                }

                var showHeart = true
                val prefs = mContext.getSharedPreferences("heart",
                        Context.MODE_PRIVATE)
                val lastGaveTime = prefs.getLong("send_heart_all", -1)
                if (lastGaveTime > 0) {
                    val expire = lastGaveTime + 60 * 1000 * 10
                    val currTime = System.currentTimeMillis()
                    if (currTime < expire) {
                        val left = (expire - currTime) / 1000
                        val text = String.format("%d:%02d", left / 60, left % 60)
                        binding.sectionWaitTimer.text = text
                        showHeart = false
                    }
                }
                if (showHeart) {
                    binding.sectionBtnSendHeart.visibility = View.VISIBLE
                    binding.sectionWaitTimer.visibility = View.GONE
                    binding.sectionBtnSendHeart.setOnClickListener(listener)
                } else {
                    binding.sectionBtnSendHeart.visibility = View.GONE
                    binding.sectionWaitTimer.visibility = View.VISIBLE
                    binding.sectionBtnSendHeart.setOnClickListener(null)
                }

                var takeHeart = true
                val lastTakeTime = prefs.getLong("take_heart", -1)
                if(lastTakeTime > 0){
                    val expire = lastTakeTime + 1000 * 3
                    val currTime = System.currentTimeMillis()
                    if (currTime < expire) {
//                        val left = (expire - currTime) / 1000
//                        val text = String.format("%d:%02d", left / 60, left % 60)
//                        sectionTakeWaitTimer.text = text
                        takeHeart = false
                    }
                }

                if(takeHeart){
//                    sectionBtnTakeHeart.visibility = View.VISIBLE
//                    sectionTakeWaitTimer.visibility = View.GONE
                    binding.sectionBtnTakeHeart.isEnabled = true
                    binding.sectionBtnTakeHeart.setOnClickListener(listener)
                }else{
//                    sectionBtnTakeHeart.visibility = View.GONE
//                    sectionTakeWaitTimer.visibility = View.VISIBLE
                    binding.sectionBtnTakeHeart.isEnabled = false
                    binding.sectionBtnTakeHeart.setOnClickListener(null)
                }
            } else {
                binding.section.visibility = View.GONE
                binding.helpTakeHeart.visibility = View.GONE
            }


            val url = binding.picture.loadInfo
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
                        .into(binding.picture)
            }

            binding.level.setImageBitmap(Util.getLevelImage(mContext, user))
            binding.name.text = "\u200E${user.nickname}"

            if (user.heart == Const.LEVEL_MANAGER) {
                binding.voteCount.visibility = View.GONE
                binding.mostIdol.visibility = View.GONE
            } else {
                binding.voteCount.visibility = View.VISIBLE
                binding.mostIdol.visibility = View.VISIBLE

                if (user.most == null) {
                    binding.mostIdol.text =
                            "${mContext.getString(R.string.most_favorite)} : ${mContext.getString(R.string.none)}"
                } else {
                    val most = user.most

//                    most.setLocalizedName(mContext)

                    val mostIdolText = mContext.getString(R.string.favorite_format)
                    binding.mostIdol.text = String.format(mostIdolText, most?.getName(mContext))

                }

                val heartCountComma = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                        .format(user.levelHeart)
                val heartCount =
                        String.format(mContext.getString(R.string.level_heart_format), heartCountComma)
                binding.voteCount.text = heartCount

                val userStatusMsg = user.statusMessage
                if (userStatusMsg.isNullOrEmpty() || user.heart == Const.LEVEL_MANAGER) {
                    binding.statusMsg.visibility = View.GONE
                } else {
                    binding.statusMsg.visibility = View.VISIBLE
                    binding.statusMsg.text = userStatusMsg
                }
            }

            val prefs = mContext.getSharedPreferences("heart",
                    Context.MODE_PRIVATE)
            val lastGaveTime = prefs.getLong("send_heart_${user.id}", -1)
            var showHeart = true
            if (lastGaveTime > 0) {
                val expire = lastGaveTime + 60 * 1000 * 10
                val currTime = System.currentTimeMillis()
                if (currTime < expire) {
                    val left = (expire - currTime) / 1000
                    val text = String.format("%d:%02d", left / 60, left % 60)
                    binding.waitTimer.text = text
                    showHeart = false
                }
            }
            if (showHeart) {
                binding.btnSendHeart.visibility = View.VISIBLE
                binding.waitTimer.visibility = View.GONE
                binding.btnSendHeart.setOnClickListener(listener)
            } else {
                binding.btnSendHeart.visibility = View.GONE
                binding.waitTimer.visibility = View.VISIBLE
            }

            //만약 해당 액티비티가 프렌즈 액티비티가 아닐경우(단순 친구목록 보여줄경우) 사진,이름,최애만 보여준다.
            if(!isFriendActivity){
                binding.level.visibility=View.GONE
                binding.voteCount.visibility=View.GONE
                binding.statusMsg.visibility=View.GONE
                binding.section.visibility=View.GONE
                binding.sendHeartWrapper.visibility=View.GONE
            }

            binding.picture.setOnClickListener(listener)
            binding.userInfo.setOnClickListener(listener)
            binding.btnSendHeart.setOnClickListener(listener)
            binding.section2.setOnClickListener(listener)
        }

    }

    inner class NewFriendsViewHolder(val binding: ItemNewFriendBinding) : ViewHolder(binding.root) {
        override fun bind(item: FriendModel, position: Int) {
            binding.section.visibility = View.GONE

            val user = item.user

            val listener = View.OnClickListener { view ->
                onClickListener.onItemClicked(user, view, position)
            }

            val url = binding.picture.loadInfo
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
                        .into(binding.picture)
            }

            binding.level.setImageBitmap(Util.getLevelImage(mContext, user))
            binding.name.text = "\u200E${user.nickname}"

            if (user.heart == Const.LEVEL_MANAGER) {
                binding.voteCount.visibility = View.GONE
                binding.mostIdol.visibility = View.GONE
            } else {
                binding.voteCount.visibility = View.VISIBLE
                binding.mostIdol.visibility = View.VISIBLE

                if (user.most == null) {
                    binding.mostIdol.text =
                            "${mContext.getString(if(BuildConfig.CELEB) R.string.actor_most_favorite else R.string.most_favorite)} : ${mContext.getString(R.string.none)}"
                } else {
                    val most = user.most

//                    most.setLocalizedName(mContext)

                    val mostIdolText = mContext.getString(if(BuildConfig.CELEB) R.string.actor_favorite_format else R.string.favorite_format)
                    binding.mostIdol.text = String.format(mostIdolText, most?.getName(mContext))
                }

                val heartCountComma = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                        .format(user.levelHeart)
                val heartCount =
                        String.format(mContext.getString(R.string.level_heart_format), heartCountComma)
                binding.voteCount.text = heartCount

                val userStatusMsg = user.statusMessage
                if (userStatusMsg.isNullOrEmpty() || user.heart == Const.LEVEL_MANAGER) {
                    binding.statusMsg.visibility = View.GONE
                } else {
                    binding.statusMsg.visibility = View.VISIBLE
                    binding.statusMsg.text = userStatusMsg
                }
            }

            // 목록에 본인이 있으면 친구 신청 버튼 안보여줌
            val account = IdolAccount.getAccount(mContext)
            if (user.id == account?.userModel?.id) {
                binding.btnReqFriend.visibility = View.GONE
            } else {
                binding.btnReqFriend.visibility = View.VISIBLE
                binding.btnReqFriend.setOnClickListener(listener)
            }

            binding.picture.setOnClickListener(listener)
            binding.userInfo.setOnClickListener(listener)
            binding.section2.setOnClickListener(listener)

        }
    }

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal abstract fun bind(item: FriendModel, position: Int)
    }

}