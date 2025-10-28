package net.ib.mn.liveStreaming.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.databinding.ItemLiveStreamListLiveBinding
import net.ib.mn.model.LiveStreamListModel
import net.ib.mn.utils.UtilK


/**
 * ProjectName: idol_app_renew
 *
 * Description:
 *
 *메인 LiveStreamingListFragment에  뿌려질 리사이클러뷰 아이템중
 *LIVE 에  해당하는 viewholder이다.
 * */
class LiveStreamListLiveVH(
    val binding: ItemLiveStreamListLiveBinding
) :RecyclerView.ViewHolder(binding.root) {
    fun bind(mGlideRequestManager: RequestManager?, liveStreamListModel: LiveStreamListModel){

        binding.tvLiveTitle.text = liveStreamListModel.title
        binding.tvLimitLevel.text = "Lv."+liveStreamListModel.levelLimit

        binding.tvStreamingLiveHits.text = UtilK.convertNumberToKMB(liveStreamListModel.totalViews)//조회수 적용
        binding.tvStreamingLiveHeart.text = UtilK.convertNumberToKMB(liveStreamListModel.heart)//하트수 적용
        binding.tvStreamingLiveUsers.text = UtilK.convertNumberToKMB(liveStreamListModel.views)//동접자수 적용
        binding.tvStreamingLiveUsers2.text = UtilK.convertNumberToKMB(liveStreamListModel.maxViews)//촤대 동접자수 적용

        if(liveStreamListModel.startAt != null){

            //start at 시간과  현재 시간을 빼서 나온 값으로 라이브 시작하고 얼마나 지났는지 체크
            binding.tvLiveDate.text = UtilK.timeBefore(liveStreamListModel.startAt!!,itemView.context)
        }

        // TODO: 2021/11/26 일단  라이브 일때도 썸네일  imageurl을 사용하기로 하여, 일단  thumnail 에 null 값 줘서 imageurl 쓰도록 함.  
        liveStreamListModel.thumbnailUrl = null
        
        //라이브는 thumbnail url을 보여주는데  thumbnail url은  referer를 요청해야  보여지므로
        if(!liveStreamListModel.thumbnailUrl.isNullOrEmpty()){
            val url = GlideUrl(
                liveStreamListModel.thumbnailUrl, LazyHeaders.Builder()
                    .addHeader("Referer", ServerUrl.HOST)
                    .build()
            )

            //thumbnail 캐싱 없애서  업데이트된 썸네일 바로바로 보여지게  적용
            mGlideRequestManager
                ?.load(url)
                ?.skipMemoryCache(true)
                ?.diskCacheStrategy(DiskCacheStrategy.NONE)
                ?.into(binding.ivLiveThumbnail)
        }else{//thumbnail null일때 예외처리
            mGlideRequestManager?.load(liveStreamListModel.imageUrl)?.into(binding.ivLiveThumbnail)
        }
    }

}