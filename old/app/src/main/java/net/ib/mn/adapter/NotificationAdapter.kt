package net.ib.mn.adapter

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.activity.NotificationActivity
import net.ib.mn.databinding.ItemNotificationBinding
import net.ib.mn.model.MessageModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK


class NotificationAdapter(
	private val context: Context,
	private val glideRequestManager: RequestManager,
	private val mListener: OnClickListener,
	private val items: List<MessageModel>
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
	private var clickedPosition: Int = 0
	private var todayLastPosition = 0	//오늘 보낸 메세지 마지막 포지션 값
	private var isTodayExist = false


	interface OnClickListener {
		fun onItemClick()
	}

	fun getItem(index: Int): MessageModel {
		return items[index]
	}

	fun getClickedPosition(): Int {
		return clickedPosition
	}

	private fun setClickedPosition(position: Int) {
		this.clickedPosition = position
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
		ViewHolder(
            ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
		)

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		items[position].let { current ->
			holder.binding.notificationWrapper.setOnLongClickListener {
				setClickedPosition(holder.adapterPosition)
				return@setOnLongClickListener false
			}
			holder.binding.notificationWrapper.setOnClickListener {
				setClickedPosition(holder.adapterPosition)
				mListener.onItemClick()
			}
			holder.bind(current, position)
		}
	}

	override fun getItemCount(): Int = items.size

	inner class ViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root), View.OnCreateContextMenuListener {

		init {
            binding.notificationWrapper.setOnCreateContextMenuListener(this)
		}

		override fun onCreateContextMenu(menu: ContextMenu?,
										 v: View?,
										 menuInfo: ContextMenu.ContextMenuInfo?) {

			val menuIds = intArrayOf(NotificationActivity.MENU_DELETE)
			val menuItems = arrayOf(context.getString(R.string.remove))

			for (i in 0 until menuItems.size) {
				menu?.add(Menu.NONE, menuIds[i], i, menuItems[i])
				val item = menu?.getItem(i)
				val spanString = SpannableString(item.toString())
				spanString.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.gray1000)), 0, spanString.length, 0)
				item?.title = spanString
			}
		}

		fun bind(message: MessageModel, position : Int) {
			with(binding) {
				val titleEdit = Editable.Factory.getInstance().newEditable("")
				val messageEdit = Editable.Factory.getInstance().newEditable("")
				val dateSB = SpannableStringBuilder()

				ivNotification.setImageResource(getNotificationIcon(message.extraType))

				if(!message.title.isNullOrEmpty()) tvTitle.visibility = View.VISIBLE
				else tvTitle.visibility = View.GONE

				titleEdit.append(message.title)
                // ... x시간 전 줄임말이 줄넘김이 있으면 잘 안되서 임시방편
                val msg = message.message.replace("\n", " ")
				messageEdit.append(msg)
				dateSB.append("  "+ message.createdAt?.let { UtilK.timeBefore(it,itemView.context) })
				dateSB.setSpan(StyleSpan(Typeface.BOLD), 0, dateSB.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
				dateSB.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_dimmed)), 0, dateSB.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
				messageEdit.append(dateSB)

				tvTitle.text = titleEdit
				tvMessage.text = messageEdit


				//친구 요청이 왔을 경우, 사용자 닉네임 색 변경
				if(message.extraType == "f" && tvMessage.text.contains(message.value)){
					tvMessage.text = Util.getColorText(messageEdit.toString(), message.value, ContextCompat.getColor(context, R.color.main))
				}
				else if((message.extraType == "ac" || message.extraType == "tc" || message.extraType == "sc") && binding.tvTitle.text.contains(message.value)){	//댓글이 달렸을 경우, 사용자 낵네임 색 변경
					tvTitle.text = Util.getColorText(titleEdit.toString(), message.value, ContextCompat.getColor(context, R.color.main))
				}

				val isToday = message.createdAt?.let { DateUtils.isToday(it.time) }    //오늘온 것인지 아닌지 true/false

				if(isToday == true){	//오늘 보낸 알림인 경우
					if(position == 0 ) {
						tvDay.text = context.getString(R.string.recent)
						clDay.visibility = View.VISIBLE
						isTodayExist = true
					}
					else{
						clDay.visibility = View.GONE
					}
					todayLastPosition = position
				}
				else{	//오늘 받은 알림이 아닐 경우
					if(position == 0){//오늘 보낸 알림이 없을 경우
							tvDay.text = context.getString(R.string.earlier)
							clDay.visibility = View.VISIBLE

					}
					else if(isTodayExist && position == todayLastPosition+1){	//오늘 보낸 알림이 있고, 오늘 보낸 알림의 다음 알림인 경우
						tvDay.text = context.getString(R.string.earlier)
						clDay.visibility = View.VISIBLE
						todayLastPosition = -1	// 오늘 받은 푸시 삭제 했을 경우 로직 다시 타는 경우가 있을 수 있어 -1로 초기화.
					}
					else{
						clDay.visibility = View.GONE

					}
				}
				//3줄 이상 시간 색상 처리
                Util.makeTextViewResizable(context, tvMessage, 3, "...  "+ message.createdAt?.let { UtilK.timeBefore(it,itemView.context) })

				//3줄 이상이 아니면 시간 색상 처리가 안되어서 따로 처리
				if(tvMessage.text.contains(UtilK.timeBefore(message.createdAt!!, itemView.context))){
					if(message.extraType == "f"){
						tvMessage.text = Util.getColorText(messageEdit.toString(), message.value, UtilK.timeBefore(message.createdAt!!, itemView.context),ContextCompat.getColor(context, R.color.main), ContextCompat.getColor(context, R.color.text_dimmed))
					}
					else{
						tvMessage.text = Util.getColorText(messageEdit.toString(), UtilK.timeBefore(message.createdAt!!, itemView.context), ContextCompat.getColor(context, R.color.text_dimmed))
					}
				}
			}
		}
	}

	fun getNotificationIcon(type: String): Int {
		when (type) {
			"n" -> return R.drawable.icon_notification_notice
			"ac","tc","sc" -> return R.drawable.icon_notification_comment
			"f" -> return R.drawable.icon_notification_friend
		}
		return R.drawable.icon_notification_notice
	}
}
