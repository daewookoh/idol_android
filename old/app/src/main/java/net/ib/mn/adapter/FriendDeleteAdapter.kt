/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.FriendDeleteItemBinding
import net.ib.mn.model.UserModel
import com.bumptech.glide.Glide
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class FriendDeleteAdapter(
    private val context: Context,
    private val mListener: OnClickListener,
    private var mItems: ArrayList<UserModel>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mGlideRequestManager: RequestManager = Glide.with(context)

    interface OnClickListener {
        fun onItemClicked(item: UserModel?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val friendDeleteItem: FriendDeleteItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.friend_delete_item,
            parent,
            false,
        )
        return FriendDeleteViewHolder(friendDeleteItem)
    }

    override fun getItemCount(): Int = mItems.size

    fun setItems(mItems: ArrayList<UserModel>) {
        this.mItems = mItems
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as FriendDeleteViewHolder).apply {
            bind(mItems[position], position)
        }
    }

    inner class FriendDeleteViewHolder(val binding: FriendDeleteItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(userModel: UserModel, position: Int) {
            with(binding) {
                val most = userModel.most
                if (most != null && !TextUtils.isEmpty(userModel.most?.getName(context))) {
                    val favoriteText: String = context.getString(if (BuildConfig.CELEB) R.string.actor_favorite_format else R.string.favorite_format)
                    favorite.text = String.format(favoriteText, most.getName(context))
                } else {
                    favorite.text = context.getString(if (BuildConfig.CELEB) R.string.actor_most_favorite else R.string.most_favorite).plus(":".plus(context.getString(R.string.none)))
                }
                name.text = userModel.nickname
                tvHeartCount.text = userModel.giveHeart.toString()
                val heartCountComma: String = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(userModel.levelHeart)
                var heartCountText: String = String.format(context.getString(R.string.heart_count_format), heartCountComma)
                heartCountText = String.format(context.getString(R.string.level_heart_format), heartCountText)
                voteCount.text = heartCountText
                val url = photo.loadInfo as String?
                var profileUrl: String? = userModel.imageUrl
                val idx = profileUrl?.lastIndexOf("v=")
                if (idx != null) {
                    if (idx >= 0) {
                        profileUrl = profileUrl?.substring(0, idx)
                    }
                }

                val userId: Int = userModel.id
                if (url == null || url != profileUrl) {
                    mGlideRequestManager
                        .load(profileUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .error(Util.noProfileImage(userId))
                        .fallback(Util.noProfileImage(userId))
                        .placeholder(Util.noProfileImage(userId))
                        .into(photo)
                }
                level.setImageBitmap(Util.getLevelImage(context, userModel))

                // 최종 로그인 날짜
                val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context))
                val localPattern = (formatter as SimpleDateFormat).toLocalizedPattern()
                val format = SimpleDateFormat(localPattern, LocaleUtil.getAppLocale(itemView.context))
                val lastLogin = format.format(userModel.lastAct)
                binding.lastLogin.text = context.resources.getString(R.string.last_login).plus(":".plus(lastLogin))

                // 일괄삭제 체크박스
                check.tag = position

                check.isChecked = userModel.deleteChecked

                clContainer.setOnClickListener {
                    check.performClick()
                }
                check.setOnClickListener {
                    userModel.deleteChecked = !userModel.deleteChecked
                    mListener.onItemClicked(userModel)
                }

                // 이모티콘
                if (userModel.emoticon != null && userModel.emoticon?.emojiUrl != null) {
                    emoticon.visibility = View.VISIBLE
                    mGlideRequestManager
                        .load(userModel.emoticon?.emojiUrl)
                        .into(emoticon)
                } else {
                    emoticon.visibility = View.GONE
                    mGlideRequestManager.clear(emoticon)
                }

                // 상태메시지
                val statusMessage: String = userModel.statusMessage ?: ""
                if (statusMessage.isEmpty()) {
                    tvStatusMessage.visibility = View.GONE
                } else {
                    tvStatusMessage.visibility = View.VISIBLE
                    tvStatusMessage.text = statusMessage
                }
            }
        }
    }
}