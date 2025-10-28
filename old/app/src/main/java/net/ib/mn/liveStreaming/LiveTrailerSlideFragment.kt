package net.ib.mn.liveStreaming

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.bumptech.glide.RequestManager
import com.google.firebase.analytics.FirebaseAnalytics
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.databinding.FragmentTrailerImageSlideBinding
import net.ib.mn.liveStreaming.LiveStreamingListAdapter.Companion.TYPE_COMING_SOON
import net.ib.mn.liveStreaming.LiveStreamingListAdapter.Companion.TYPE_LIVE
import net.ib.mn.liveStreaming.LiveStreamingListAdapter.Companion.TYPE_LIVE_FINISH
import net.ib.mn.model.LiveStreamListModel
import net.ib.mn.model.LiveStreamTopBannerModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.core.data.repository.PlayRepositoryImpl
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import java.text.DateFormat
import javax.inject.Inject

/**
 * ProjectName: idol_app_renew
 *
 * Description: 메인 라이브 스트리밍 탭에서
 * 탑 배너 슬라이드가 보여질  framgent
 * 아곳에서  탑배너의 이미지를 넣어준다.
 * */
@AndroidEntryPoint
class LiveTrailerSlideFragment() : Fragment() {

    private lateinit var topBannerModel: LiveStreamTopBannerModel
    var mGlideRequestManager: RequestManager? = null
    private lateinit var binding: FragmentTrailerImageSlideBinding
    @Inject
    lateinit var playRepository: PlayRepositoryImpl

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_trailer_image_slide, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //탑배너 모델
        topBannerModel = arguments?.getSerializable(PARAM_TOP_BANNER_MODEL) as LiveStreamTopBannerModel

        //탑배너 이미지 넣어줌.
        mGlideRequestManager = Glide.with(this)
        mGlideRequestManager?.load(topBannerModel.imageUrl)?.into(binding.ivTrailer)


        //배너 클릭시
        binding.clContainer.setOnClickListener {
            if (topBannerModel.live != null) {//현재 라이브 진행중.

                //레벨 limit 통과일떄
                if((IdolAccount.getAccount(requireActivity())?.level ?: 0) >= topBannerModel.live!!.levelLimit){

                    //해당 live id의  라이브 정보를 받아온다.
                    MainScope().launch {
                        playRepository.getInfo(
                            topBannerModel.live!!.id!!,
                            { response ->
                                try {
                                    if (response.optBoolean("success", false)) { //콜백 성공적
                                        //라이브 모델
                                        val liveStreamModel =
                                            IdolGson.getInstance().fromJson(
                                                response.getJSONObject("live").toString(),
                                                LiveStreamListModel::class.java
                                            )

                                        if (liveStreamModel.status == TYPE_LIVE) {//라이브 중인 경우라면  세로모드 화면으로 이동한다.
                                            requireActivity().startActivityForResult(
                                                LiveStreamingActivity.createIntent(
                                                    requireActivity(),
                                                    liveStreamModel
                                                ),//라이브 화면(세로모드 화면) 이동
                                                REQUEST_PARAM_TO_MAIN)

                                            setUiActionFirebaseGoogleAnalyticsFragment(
                                                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                                                "live_banner_enter"
                                            )


                                        } else if (liveStreamModel.status == TYPE_COMING_SOON) {//준비중일때는 오픈일정(startAt)과 함께 준비중 팝업을 뛰어줌.

                                            //날짜 포맷
                                            val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(context ?: return@getInfo))

                                            //시간 포맷
                                            val hourFormat = DateFormat.getTimeInstance(DateFormat.SHORT,LocaleUtil.getAppLocale(context ?: return@getInfo))

                                            //시간, 데이트 포맷을 나눠서  만들고   \n을  넣어줘서  데이트 밑에  시간이 나오도록 함.
                                            val liveStartDate =
                                                dateFormat.format(liveStreamModel.startAt!!) + "\n" + hourFormat.format(
                                                    liveStreamModel.startAt!!
                                                )

                                            Util.showDefaultIdolDialogWithBtn1(//타이틀에는  오픈 일정, 내용에는  준비중 스트링  적용
                                                requireActivity(),
                                                String.format(
                                                    requireActivity().getString(R.string.live_open),
                                                    liveStartDate
                                                ),
                                                getString(R.string.live_coming_soon)
                                            ) { Util.closeIdolDialog() }

                                        } else if (liveStreamModel.status == TYPE_LIVE_FINISH){ //종료된 라이브가 배너에 있을 경우 팝업
                                            Util.showDefaultIdolDialogWithBtn1(
                                                requireActivity(),
                                                null,
                                                requireActivity().getString(R.string.live_end),
                                            ) { Util.closeIdolDialog() }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, { throwable ->
                                Toast.makeText(
                                    requireActivity(),
                                    R.string.error_abnormal_default,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }else{//레벨이 안될때
                    Util.showDefaultIdolDialogWithBtn1(requireActivity(), null,
                        String.format(requireActivity().getString(R.string.live_level_limit), topBannerModel.live!!.levelLimit)

                    ) {
                        Util.closeIdolDialog()
                    }
                }

            }
        }
    }

    fun setUiActionFirebaseGoogleAnalyticsFragment(action: String?, label: String) {
        try {
            var firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(requireActivity())
            firebaseAnalytics.setCurrentScreen(requireActivity(), this.javaClass.simpleName, null)
            val params = Bundle()
            params.putString("ui_action", action)
            firebaseAnalytics.logEvent(label, params)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    companion object{

        //메인 엑티비티에  보낼 startforactivityresult request 코드
        const val REQUEST_PARAM_TO_MAIN = 1127
        const val PARAM_TOP_BANNER_MODEL = "top_banner_model"

        @JvmStatic
        fun newInstance(
            topBannerModel: LiveStreamTopBannerModel
        ):LiveTrailerSlideFragment {
            val args = Bundle()
            args.putSerializable(PARAM_TOP_BANNER_MODEL, topBannerModel)
            val fragment = LiveTrailerSlideFragment()
            fragment.arguments = args
            return fragment
        }
    }


}