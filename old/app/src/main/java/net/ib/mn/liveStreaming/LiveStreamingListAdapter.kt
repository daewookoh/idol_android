package net.ib.mn.liveStreaming

import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.databinding.ItemLiveStreamListComingSoonBinding
import net.ib.mn.databinding.ItemLiveStreamListFinishedBinding
import net.ib.mn.databinding.ItemLiveStreamListLiveBinding
import net.ib.mn.databinding.ItemLiveStreamListTopBannerBinding
import net.ib.mn.model.LiveStreamListModel
import net.ib.mn.liveStreaming.viewholder.LiveStreamListComingSoonVH
import net.ib.mn.liveStreaming.viewholder.LiveStreamListFinishedVH
import net.ib.mn.liveStreaming.viewholder.LiveStreamListLiveVH
import net.ib.mn.liveStreaming.viewholder.LiveStreamTopBannerVH
import net.ib.mn.utils.Util


/**
 * ProjectName: idol_app_renew
 *
 * Description:
 * 메인 라이브 스트리밍 탭 화면에
 * 라이브 스트리밍 관련  데이터를  리사이클러뷰에 뿌려준다.
 *
 * */
class LiveStreamingListAdapter(paramLiveStreamList:ArrayList<LiveStreamListModel>,
                               paramLiveTrailerBannerList:ArrayList<LiveTrailerSlideFragment>,
                               private var mGlideRequestManager: RequestManager?
): RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private var liveStreamList:ArrayList<LiveStreamListModel> = paramLiveStreamList
    private var liveTrailerBannerList:ArrayList<LiveTrailerSlideFragment> = paramLiveTrailerBannerList

    lateinit var slideHandler: Handler
    lateinit var sliderRunnable:Runnable
    private var onItemClickListener: OnItemClickListener? = null

    //아이템 클릭 동작 interface
    interface OnItemClickListener {
        fun onComingSoonItemClicked(listType:Int)
        fun onFinishedItemClicked(listType:Int)
        fun onLiveItemClicked(listType:Int,liveStreamListModel: LiveStreamListModel)
    }

    //외부에서 서포 아이템 클릭 처리할 리스너
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {

            //탑 배너일떄
            TYPE_TOP_BANNER -> {
                val binding: ItemLiveStreamListTopBannerBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_live_stream_list_top_banner,
                    parent,
                    false
                )
                LiveStreamTopBannerVH(binding)
            }

            //라이브 아이템일떄
            TYPE_LIVE -> {
                val binding: ItemLiveStreamListLiveBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_live_stream_list_live,
                    parent,
                    false
                )
                LiveStreamListLiveVH(binding)
            }

            //라이브 종료 아이템일떄
            TYPE_LIVE_FINISH ->{
                val binding: ItemLiveStreamListFinishedBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_live_stream_list_finished,
                    parent,
                    false
                )
                LiveStreamListFinishedVH(binding)
            }

            //위 경우 제외하곤 모두 준비중 아이템
            else -> {
                val binding: ItemLiveStreamListComingSoonBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_live_stream_list_coming_soon,
                    parent,
                    false
                )
                LiveStreamListComingSoonVH(binding)
            }
        }
    }

    //라이브 관련 데이터 리스트 받아옴.
    fun getLiveListData(liveStreamList:ArrayList<LiveStreamListModel>){
        this.liveStreamList = liveStreamList
        notifyDataSetChanged()
    }

    //라이브 리스트 탑배너 데이터 받아옴.
    fun getLiveListTopBannerData(liveTrailerBannerList:ArrayList<LiveTrailerSlideFragment>){
        this.liveTrailerBannerList = liveTrailerBannerList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> {//포지션 0  가장  위쪽에는  탑배너 아이템이 들어감 .
                TYPE_TOP_BANNER
            }

            //라이브 스트리밍 status 0(준비중)  커밍순
            liveStreamList[position-1].status == TYPE_COMING_SOON  -> {
                TYPE_COMING_SOON
            }

            //라이브 스트리밍 status  2(종료됨) 일때는 종료 아이템
            liveStreamList[position-1].status == TYPE_LIVE_FINISH  -> {
                TYPE_LIVE_FINISH
            }

            //라이브 스트리미 status 1일때는  라이브
            else -> {
                TYPE_LIVE
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder.itemViewType) {
            TYPE_TOP_BANNER -> {
                (holder as LiveStreamTopBannerVH).apply {
                    bind(liveTrailerBannerList)
                    this@LiveStreamingListAdapter.slideHandler = slideHandler
                    this@LiveStreamingListAdapter.sliderRunnable = sliderRunnable
                }
            }

            TYPE_LIVE -> {//라이브용 아이템
                (holder as LiveStreamListLiveVH).apply {
                    bind(mGlideRequestManager,liveStreamList[position-1])
                    itemView.setOnClickListener {
                        //유저의 레벨이  스트리밍 level제한 레벨과 같거나 클때만  스트리밍 아이템 클릭 이벤트 실행
                        //그외에는 레벨부족 팝업을 띄어준다.
                        if((IdolAccount.getAccount(holder.itemView.context)?.level ?: 0) >= liveStreamList[position-1].levelLimit){
                            onItemClickListener?.onLiveItemClicked(TYPE_LIVE,liveStreamList[position-1])
                        }else{
                            Util.showDefaultIdolDialogWithBtn1(itemView.context, null,
                                String.format(itemView.context.getString(R.string.live_level_limit), liveStreamList[position-1].levelLimit)

                            ) {
                                Util.closeIdolDialog()
                            }
                        }
                    }

//                    removeLastPositionBottomBar(position, this.binding.liveBottomBar)
                }
            }

            TYPE_LIVE_FINISH ->{//라이브 종료 아이템
                (holder as LiveStreamListFinishedVH).apply {
                    bind(mGlideRequestManager,liveStreamList[position-1])
                    itemView.setOnClickListener {
                        onItemClickListener?.onFinishedItemClicked(TYPE_LIVE_FINISH)
                    }
//                    removeLastPositionBottomBar(position,this.binding.finishBottomBar)
                }
            }


            TYPE_COMING_SOON -> {//커밍순용 아이템일때
                (holder as LiveStreamListComingSoonVH).apply {
                    bind(mGlideRequestManager,liveStreamList[position-1])
                    itemView.setOnClickListener {
                        onItemClickListener?.onComingSoonItemClicked(TYPE_COMING_SOON)
                    }
//                    removeLastPositionBottomBar(position,this.binding.comingSoonBottomBar)
                }
            }
        }
    }


    //마지막 아이템일때, 밑에 bar 삭제 용 코드
    private fun removeLastPositionBottomBar(position: Int,bottomBar:View){
        if(position == itemCount-1){
            bottomBar.visibility = View.GONE
        }else{
            bottomBar.visibility = View.VISIBLE
        }
    }

    override fun getItemCount()= liveStreamList.size+1

    companion object {
        const val  TYPE_COMING_SOON = 20//방송 준비중
        const val  TYPE_LIVE = 10//방송중
        const val  TYPE_LIVE_FINISH =30//방송 종료
        const val  TYPE_TOP_BANNER = 3

    }
}