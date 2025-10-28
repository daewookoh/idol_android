package net.ib.mn.liveStreaming.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.databinding.ItemLiveStreamListComingSoonBinding
import net.ib.mn.model.LiveStreamListModel
import net.ib.mn.utils.LocaleUtil
import java.text.DateFormat


/**
 * ProjectName: idol_app_renew
 *
 * Description:
 *
 *메인 LiveStreamingListFragment에  뿌려질 리사이클러뷰 아이템중
 *commingSoon 아이템에 해당하는 viewholder이다.
 *
 * 준비중은 어차피  조회수, 하트수, 동접자수 , 동시동접자수 0이니까  0으로 xml에  넣어놓음.
 * */
class LiveStreamListComingSoonVH(
    val binding: ItemLiveStreamListComingSoonBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(mGlideRequestManager: RequestManager?, liveStreamListModel: LiveStreamListModel){

        binding.tvLimitLevel.text = "Lv."+liveStreamListModel.levelLimit
        binding.tvComingSoonTitle.text = liveStreamListModel.title

        mGlideRequestManager?.load(liveStreamListModel.imageUrl)?.into(binding.ivComingSoonThumbnail)


        //라이브 startdate기준으로 날짜 보여줌. -> 날짜 표시는  각 로컬 타임으로.
        if(liveStreamListModel.startAt != null){

            //날짜 포맷
            val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context))
            //시간 포맷
            val hourFormat = DateFormat.getTimeInstance(DateFormat.SHORT, LocaleUtil.getAppLocale(itemView.context))

            val liveStartDate = dateFormat.format(liveStreamListModel.startAt!!) +" "+hourFormat.format(liveStreamListModel.startAt!!)
            binding.tvComingSoonDate.text = String.format(itemView.context.getString(R.string.live_open),liveStartDate)
        }
    }


}