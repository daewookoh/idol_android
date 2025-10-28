package net.ib.mn.liveStreaming.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.databinding.ItemLiveStreamListFinishedBinding
import net.ib.mn.model.LiveStreamListModel
import net.ib.mn.utils.UtilK


/**
 * ProjectName: idol_app_renew
 *
 * Description: 라이브 리스트 탭에서  종료된  라이브를 보여줄때
 * 사용하는  뷰홀더이다.
 * */
class LiveStreamListFinishedVH (val binding: ItemLiveStreamListFinishedBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(mGlideRequestManager: RequestManager?, liveStreamListModel: LiveStreamListModel) {

        //라이브 타이틀 적용
        binding.tvFinishedTitle.text = liveStreamListModel.title

        //라이브 이미지 적용
        mGlideRequestManager?.load(liveStreamListModel.imageUrl)?.into(binding.ivFinishedThumbnail)

        binding.tvStreamingFinishedHits.text = UtilK.convertNumberToKMB(liveStreamListModel.totalViews)//조회수 적용
        binding.tvStreamingFinishedHeart.text = UtilK.convertNumberToKMB(liveStreamListModel.heart)//하트수 적용
        binding.tvStreamingFinishedUsers.text = UtilK.convertNumberToKMB(liveStreamListModel.views)//동접자수 적용
        binding.tvStreamingFinishedUsers2.text = UtilK.convertNumberToKMB(liveStreamListModel.maxViews)//촤대 동접자수 적용


        if(liveStreamListModel.startAt != null){
            //start at 시간과  현재 시간을 빼서 나온 값으로 라이브 시작하고 얼마나 지났는지 체크
            binding.tvFinishedDate.text = UtilK.timeBefore(liveStreamListModel.startAt!!,itemView.context)
        }

    }
}


