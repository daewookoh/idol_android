package net.ib.mn.liveStreaming

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.databinding.ItemLiveChatLandscapeBinding
import net.ib.mn.databinding.ItemLiveChatPortraitBinding
import net.ib.mn.model.LiveChatMessageModel
import net.ib.mn.liveStreaming.viewholder.LiveStreamLandscapeChatVH
import net.ib.mn.liveStreaming.viewholder.LiveStreamPortraitChatVH

/**
 * ProjectName: idol_app_renew
 *
 * Description: 라이브 채팅  관련 데이터를  각각 세로, 가로 모드 에 맞춰 리사이클러뷰에 뿌려준다.
 * */
class LiveStreamingChattingAdapter(
    private val orientationMode: Int,
    private val mGlideRequestManager:RequestManager?,
    private val account: IdolAccount?
)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //라이브 채팅 리스트
    private var liveChattingMessageList = ArrayList<LiveChatMessageModel>()

    //라이브 채팅 아이템 클릭 리스너
    private var onItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener{
        fun getItemClick()
    }
    //외부에서 아이템 클릭 처리할 리스너
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }
    override fun getItemCount(): Int = liveChattingMessageList.size

    //라이브 메세지 받아오고 리사이클러뷰에 추가함.
    fun getLiveStreamMessageList(liveChattingMessage: LiveChatMessageModel) {
        this.liveChattingMessageList.add(liveChattingMessage)
        notifyItemInserted(this.liveChattingMessageList.size - 1)
    }
    //라이브 메세지 받아오고 리사이클러뷰에 추가함.
    fun getLiveStreamAllMessageList(liveChattingMessageList: ArrayList<LiveChatMessageModel>) {
        this.liveChattingMessageList.clear()
        this.liveChattingMessageList.addAll(liveChattingMessageList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return orientationMode
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder.itemViewType) {
            LANDSCAPE_MODE -> {
                (holder as LiveStreamLandscapeChatVH).apply {
                    bind(
                            mGlideRequestManager,
                            liveChattingMessageList[position]
                    )
                    itemView.setOnClickListener {
                        onItemClickListener?.getItemClick()
                    }
                }

            }

            PORTRAIT_MODE -> {
                (holder as LiveStreamPortraitChatVH).apply {
                    bind(
                            mGlideRequestManager,
                            liveChattingMessageList[position]
                    )
                    itemView.setOnClickListener {
                        onItemClickListener?.getItemClick()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemLivePortraitChatMessage: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_live_chat_portrait, parent, false)
        val itemLiveLandScapeChatMessage: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_live_chat_landscape, parent, false)

        return when (viewType) {

            LANDSCAPE_MODE -> {
                val binding: ItemLiveChatLandscapeBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_live_chat_landscape,
                    parent,
                    false,
                )
                LiveStreamLandscapeChatVH(binding, account = account)
            }

            else -> {
                val binding: ItemLiveChatPortraitBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_live_chat_portrait,
                    parent,
                    false,
                )
                LiveStreamPortraitChatVH(binding, account = account)
            }
        }
    }


    companion object {

        const val LANDSCAPE_MODE = 0//가로 모드
        const val PORTRAIT_MODE = 1//세로 모드

    }

}