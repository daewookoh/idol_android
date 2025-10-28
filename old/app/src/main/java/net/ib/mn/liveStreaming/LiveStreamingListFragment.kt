package net.ib.mn.liveStreaming

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.databinding.DataBindingUtil
import net.ib.mn.utils.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.PlayRepositoryImpl
import net.ib.mn.databinding.FragmentLiveStreamingBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.model.LiveStreamListModel
import net.ib.mn.model.LiveStreamTopBannerModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.ext.preventParentHorizontalScrollInViewPager2
import org.json.JSONException
import javax.inject.Inject


/**
 * ProjectName: idol_app_renew
 *
 * Description:
 * 메인에 라이브 탭이 생겼을때,  보이는  화면
 * 라이브 스트리밍  및  리플레이 영상  및 라이브 예고 롤링 배너가 보여지게 된다.
 * */

@AndroidEntryPoint
class LiveStreamingListFragment : BaseFragment(),
    SwipeRefreshLayout.OnRefreshListener,
    OnScrollToTopListener{


    private var liveTrailerBannerList=ArrayList<LiveTrailerSlideFragment>()

    private lateinit var liveStreamingListAdapter:LiveStreamingListAdapter

    private lateinit var liveStreamList: ArrayList<LiveStreamListModel>
    private var gson =  IdolGson.getInstance()

    //라이브 리스트 페이징 처리  위한  offset limit
    private var listOffset = 0
    private var listLimit = 30//2021/11/07 limit은  페이징 없다고 하여,  10-> 30으로 늘림

    //다음 페이지 요청용  url
    private var nextLiveListUrl:String? = ""

    private lateinit var binding: FragmentLiveStreamingBinding
    @Inject
    lateinit var playRepository: PlayRepositoryImpl

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_live_streaming, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSet()
        setLiveListRcyView()
        getTopBannerList()
        getLiveList(listOffset,listLimit)
        setEventListener()
    }


    //초기 세팅
    private fun initSet(){

        binding.tvLiveEmpty.visibility = View.VISIBLE
        binding.rcyLiveList.visibility = View.INVISIBLE

        liveStreamList= ArrayList()//일반  라이브 리스트 -( live,coming soon, finish)
        liveTrailerBannerList = ArrayList()//탑배너 관련 데이터 담을 arraylist
    }

    private fun setEventListener(){

        binding.liveSwipeRefresh.setOnRefreshListener(this)


        //리사이클러뷰 아이템 클릭
        liveStreamingListAdapter.setOnItemClickListener(object:LiveStreamingListAdapter.OnItemClickListener{

            //종료된 라이브 클릭시
            override fun onFinishedItemClicked(listType: Int) {
            }

            //준비중 라이브 아이템 클릭 실행됨
            override fun onComingSoonItemClicked(listType: Int) {
            }


            //라이브 아이템 클릭 실행됨
            override fun onLiveItemClicked(
                listType: Int,
                liveStreamListModel: LiveStreamListModel
            ) {

                setUiActionFirebaseGoogleAnalyticsFragment(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "live_enter"
                )
                startActivityForResult(LiveStreamingActivity.createIntent(requireActivity(),liveStreamListModel),REQUEST_CODE_LIVE_LIST)
            }
        })

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //라이브 화면  들어갔다 다시  돌아오면 리스트 업데이트 해준다.
        if(requestCode == REQUEST_CODE_LIVE_LIST){
            liveStreamList= ArrayList()//일반  라이브 리스트 -( live,coming soon, finish)
            liveTrailerBannerList = ArrayList()//탑배너 관련 데이터 담을 arraylist

            getTopBannerList()
            getLiveList(listOffset,listLimit)
        }
    }


    //라이브 스트림 리스트 용 리아시클러뷰 set
    private fun setLiveListRcyView() {
        liveStreamingListAdapter = LiveStreamingListAdapter(liveStreamList,liveTrailerBannerList,mGlideRequestManager)
        binding.rcyLiveList.apply {
            adapter = liveStreamingListAdapter
            // 상하 스크롤시 좌우 다른 프래그먼트로 넘어가지 않게
            preventParentHorizontalScrollInViewPager2(binding.liveSwipeRefresh)
        }

        //리사이클러뷰 하단을 감지해서 페이징 처리를  진행한다.
        binding.rcyLiveList.addOnScrollListener(object : RecyclerView.OnScrollListener(){

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (!recyclerView.canScrollVertically(1) && newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {

                    // TODO: 2021/11/07 일단 당분간은 페이징 없이  limit 10 -> 30으로 늘린다고 하여,  페이징 코드  주석처리함.
//                    //null값으로 오면 이거는  다음 페이지가 없다는 뜻이므로  null이 아닌경우만  offset을  limit수만큼  올려줘서  다음  페이지를 요청한다.
//                    if(!nextLiveListUrl.equals("null")){
//                        listOffset =+ listLimit
//                        getLiveList(listOffset,listLimit)
//                    }
                }
            }

        })

    }


    //menuvisible true이면 현재 프래그먼트  show됨.
    override fun setMenuVisibility(menuVisible: Boolean) {

        // 2021/11/07 일단 라이브 리스트 화면  show될떄마다 업데이트된 데이터 보여주기로하여, menuvisible true일때 새롭게 데이터 불러서 넣어줌.
        if(menuVisible){
            liveTrailerBannerList = ArrayList()
            liveStreamList = ArrayList()

            listOffset = 0//라이브리스트 처듬부터 다시 가져옴으로 offset 0으로 리셋해줌.
            getTopBannerList()
            getLiveList(listOffset,listLimit)
        }
        super.setMenuVisibility(menuVisible)
    }


    //refresh리스너  동작하면 여기로 타짐.
    override fun onRefresh() {
        binding.rcyLiveList.postDelayed({

            liveTrailerBannerList = ArrayList()
            liveStreamList = ArrayList()

            binding.tvLiveEmpty.visibility = View.VISIBLE
            binding.rcyLiveList.visibility = View.INVISIBLE

            listOffset = 0//라이브리스트 처듬부터 다시 가져옴으로 offset 0으로 리셋해줌.
            getTopBannerList()
            getLiveList(listOffset,listLimit)

            binding.liveSwipeRefresh.isRefreshing = false
            binding.rcyLiveList.smoothScrollToPosition(0)
        }, 500)
    }


    //서버로부터 라이브 리스트 탭  탑쪽에 위치하는 배너  데이터를 받아온다.
    private fun getTopBannerList(){
        MainScope().launch {
            playRepository.getTopBanners(
                { response ->
                    if(response!= null){
                        try{
                            val bannersJSONArray  = response.getJSONArray("banners")
                            for (i in 0 until bannersJSONArray.length()) {
                                val model = gson.fromJson(
                                    bannersJSONArray.getJSONObject(i).toString(),
                                    LiveStreamTopBannerModel::class.java
                                )

                                liveTrailerBannerList.add(LiveTrailerSlideFragment.newInstance(model))

                            }

                            if(liveTrailerBannerList.size>0){//배너값 하나라도 있다면 empty view gone 처리
                                binding.tvLiveEmpty.visibility = View.GONE
                                binding.rcyLiveList.visibility = View.VISIBLE
                            }else{//배너 기준으로  라이브 리스트 보여줌 ->  배너가 없다면,  라이브 목록도 안나와야됨.
                                binding.tvLiveEmpty.visibility = View.VISIBLE
                                binding.rcyLiveList.visibility = View.GONE
                            }

                            liveStreamingListAdapter.getLiveListTopBannerData(liveTrailerBannerList)
                        }catch (e:JSONException){
                            e.printStackTrace()
                        }
                    }else{
                        Toast.makeText(
                            requireActivity(),
                            R.string.error_abnormal_exception,
                            Toast.LENGTH_SHORT).show()
                    }
                },
                { throwable ->
                    Toast.makeText(
                        requireActivity(),
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    //서버로부터 라이브 리스트 목록을  받아온다.
    private fun getLiveList(lisOffset:Int,listLimit:Int) {
        MainScope().launch {
            playRepository.getList(
                lisOffset,
                listLimit,
                { response ->
                    if (response != null) {

                        //response 성공적으로 가져옴. ->  리스트 뿌리기 로직 진행
                        if (response.optBoolean("success")) {

                            //페이징 처리용으로 다음 가져올  데이터가 있는지 체크한다. null 이면  현재 페이지가  마지막
                            nextLiveListUrl = response.optJSONObject("meta")?.optString("next")

                            //라이브 리스트 목록
                            val liveListJSONArray = response.getJSONArray("objects")
                            for (i in 0 until liveListJSONArray.length()) {
                                val model = gson.fromJson(
                                    liveListJSONArray.getJSONObject(i).toString(),
                                    LiveStreamListModel::class.java
                                )

                                liveStreamList.add(model)
                            }


                            // TODO: 2021/12/06 배너기준으로  라이브 목록  empty 여부 체크하기로 해서 요거는  주석처리
                            // if(liveStreamList.size>0){//라이브 리스트 값 하나라도 있다면 empty view gone처리
                            //      binding.tvLiveEmpty.visibility = View.GONE
                            //      binding.rcyLiveList.visibility = View.VISIBLE
                            // }

                            liveStreamingListAdapter.getLiveListData(liveStreamList)
                        } else {
                            Toast.makeText(
                                requireActivity(),
                                R.string.error_abnormal_exception,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {//response null일때
                        Toast.makeText(
                            requireActivity(),
                            R.string.error_abnormal_exception,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }, { throwable ->
                    Toast.makeText(
                        requireActivity(),
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    override fun onScrollToTop() {
        if(!::binding.isInitialized) return
        binding.rcyLiveList.scrollToPosition(0)
    }

    companion object{
        const val REQUEST_CODE_LIVE_LIST = 1002

    }
}