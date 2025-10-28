package net.ib.mn.liveStreaming.viewholder

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.ContextMenu
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.databinding.ItemLiveChatLandscapeBinding
import net.ib.mn.liveStreaming.LiveStreamingActivity
import net.ib.mn.model.LiveChatMessageModel
import net.ib.mn.utils.Util
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * ProjectName: idol_app_renew
 *
 * Description: 라이브 스트리밍 가로모드 채팅 아이템 용  뷰홀더이다.
 * */
class LiveStreamLandscapeChatVH(val binding: ItemLiveChatLandscapeBinding, private val account: IdolAccount?) : RecyclerView.ViewHolder(binding.root),View.OnCreateContextMenuListener {
    private lateinit var liveChatModel: LiveChatMessageModel
    init {
        itemView.setOnCreateContextMenuListener(this)
    }
    private var isMineChatOrNot = false
    fun bind(mGlideRequestManager: RequestManager?, liveChatMessageModel: LiveChatMessageModel) {
        liveChatModel = liveChatMessageModel
        isMineChatOrNot = liveChatMessageModel.isMineChat

        //내 채팅인 경우에는 textcolor 붉은색으로 변경
        if(liveChatMessageModel.isMineChat){
            binding.tvNicknameLiveChatLandscape.setTextColor(itemView.context.resources.getColor(R.color.main_light,itemView.context.theme))
        }else{
            binding.tvNicknameLiveChatLandscape.setTextColor(itemView.context.resources.getColor(R.color.text_dimmed,itemView.context.theme))
        }

        binding.tvNicknameLiveChatLandscape.text = liveChatMessageModel.senderNickName


        //자신의 메세지 삭제하면 삭제된 메세지라고 변경
        if(liveChatMessageModel.deleted == true) {
            liveChatMessageModel.content = itemView.context.getString(R.string.chat_deleted_message)
        }else if(liveChatMessageModel.isReported){ //남의 메세지 신고하면 신고된 메세지라고 변경
            liveChatMessageModel.content = itemView.context.getString(R.string.already_reported)
        }

        val liveCHatMessageWithTs = if(liveChatMessageModel.isMineChat){//내가 보낸 채팅은 client tsㄹ
            liveChatMessageModel.content + "  "+ SimpleDateFormat.getTimeInstance(
                DateFormat.SHORT
            ).format(Date(liveChatMessageModel.clientTs))
        }else{
            liveChatMessageModel.content + "  "+ SimpleDateFormat.getTimeInstance(
                DateFormat.SHORT
            ).format(Date(liveChatMessageModel.serverTs))
        }

        binding.tvLiveChatMessageLandscape.text = SpannableString(liveCHatMessageWithTs).apply {
            this.setSpan(ForegroundColorSpan(ContextCompat.getColor(itemView.context, R.color.text_dimmed)), liveChatMessageModel.content.length, liveCHatMessageWithTs.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            this.setSpan(RelativeSizeSpan(0.75f), liveChatMessageModel.content.length, liveCHatMessageWithTs.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val options = RequestOptions()
                .circleCrop()
                .error(Util.noProfileImage(liveChatMessageModel.userId))
                .fallback(Util.noProfileImage(liveChatMessageModel.userId))
                .placeholder(Util.noProfileImage(liveChatMessageModel.userId))

        mGlideRequestManager?.load(liveChatMessageModel.senderImage)?.apply(options)?.into(binding.ivUserPhotoLiveChatLandscape)

    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        val menuIds: MutableList<Int>
        val menuItems: MutableList<String>
        if(isMineChatOrNot){//내가보내는  채팅일때는  삭제  복사
            menuIds=mutableListOf(
                LiveStreamingActivity.MENU_COPY,
                LiveStreamingActivity.MENU_DELETE)
            menuItems = mutableListOf(itemView.context.getString(android.R.string.copy),
                itemView.context.getString(R.string.remove))
        }else{//내가보내는 채팅 아닐때는  복사 신고만,
            menuIds=mutableListOf(
                LiveStreamingActivity.MENU_COPY,
                LiveStreamingActivity.MENU_REPORT)
            menuItems = mutableListOf(itemView.context.getString(android.R.string.copy),
                itemView.context.getString(R.string.report))
        }

        //신고또는 삭제가 되었을 경우에는  신고 삭제 메뉴를 모두 안나오게 지우준다.
        if(liveChatModel.deleted== true || liveChatModel.isReported){
            menuItems.remove(itemView.context.getString(R.string.report))
            menuItems.remove(itemView.context.getString(R.string.remove))
        }

        for (i in 0 until menuItems.size) {
            menu?.add(bindingAdapterPosition, menuIds[i], i, menuItems[i])
        }

    }
}