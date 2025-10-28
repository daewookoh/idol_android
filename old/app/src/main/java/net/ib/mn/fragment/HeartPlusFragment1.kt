package net.ib.mn.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.MezzoPlayerActivity.Companion.createIntent
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.domain.usecase.GetConfigSelfUseCase
import net.ib.mn.databinding.ChargeItemBinding
import net.ib.mn.databinding.FragmentHeartplus1Binding
import net.ib.mn.model.ConfigModel.Companion.getInstance
import net.ib.mn.model.InHouseOfferwallModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Const.AUTO_CLICK_COUNT_PREFERENCE_KEY
import net.ib.mn.utils.IEarnHeartsListener
import net.ib.mn.utils.IVideoAdListener
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.handleCommonError
import net.ib.mn.utils.UtilK.Companion.videoDisableTimer
import net.ib.mn.utils.VideoAdManager
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.livedata.Event
import net.ib.mn.viewmodel.HeartPlusViewModel
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.ib.mn.core.domain.usecase.PostVideoAdNotificationUseCase
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.dialog.VideoAdNotifyToastFragment
import net.ib.mn.domain.usecase.datastore.GetAdCountUseCase
import net.ib.mn.domain.usecase.datastore.IsSetAdNotificationPrefsUseCase
import net.ib.mn.domain.usecase.datastore.SetAdNotificationPrefsUseCase
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.UtilK

@AndroidEntryPoint
class HeartPlusFragment1 : BaseFragment(), AdapterView.OnItemClickListener {

    private val viewModel: HeartPlusViewModel by viewModels()

    private var mAdapter: Adapter? = null
    private var heart_count = 0

    // 비디오 광고 프로세스 개선
    private var videoAdManager: VideoAdManager? = null
    private var loadingTimer: Timer? = null
    private var loadingString: String? = null
    private var loadingCount = 0
    private var showLoading = false
    private var mContext: Context? = null

    // 배우자 인하우스 오퍼월
    private val inHouseOfferwalls = ArrayList<InHouseOfferwallModel>()

    private var launcher: ActivityResultLauncher<Intent?>? = null

    @JvmField
    @Inject
    var videoAdUtil: VideoAdUtil? = null
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var getConfigSelfUseCase: GetConfigSelfUseCase
    @Inject
    lateinit var getAdCountUseCase: GetAdCountUseCase
    @Inject
    lateinit var postVideoAdNotificationUseCase: PostVideoAdNotificationUseCase
    @Inject
    lateinit var setAdNotificationPrefsUseCase: SetAdNotificationPrefsUseCase
    @Inject
    lateinit var isSetAdNotificationPrefsUseCase: IsSetAdNotificationPrefsUseCase

    private var _binding: FragmentHeartplus1Binding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGlideRequestManager = Glide.with(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //startActivityForResult deprecated되서 대체
        launcher = registerForActivityResult<Intent?, ActivityResult?>(
            ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? ->
                val activity = getActivity() as BaseActivity?
                Util.handleVideoAdResult(
                    activity,
                    false,
                    isAdded(),
                    -1,
                    result!!.resultCode,
                    result.data,
                    "widephoto_videoad",
                    null,IVideoAdListener { type: String? -> this.onVideoSaw(type) })
            })

        observedVM()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHeartplus1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context

        Util.showProgress(activity)

        lifecycleScope.launch {
            try {
                val result = getConfigSelfUseCase().first()
                val response = result.data
                if(!result.success || result.data == null) {
                    Util.closeProgress()
                    if (activity != null && isAdded && response != null) {
                        handleCommonError(activity, response)
                    }
                    return@launch
                }

                if (response?.optBoolean("success") != true) {
                    Util.closeProgress()
                    return@launch
                }

                getInstance(mContext).parse(response)
                heart_count = getInstance(activity).nasHeart
                if (heart_count != 0) {
                    mAdapter = Adapter(context, heart_count)
                    binding.list.setAdapter(mAdapter)
                    binding.list.setOnItemClickListener(this@HeartPlusFragment1)

                    // crash 방지
                    if (activity == null || !isAdded) {
                        return@launch
                    }

                    //위에 Naswall없어서 추가해줌. 없으면 adapter update하는부분에서 마지막 요소까지 가지 않는다.
                    mAdapter!!.add(Any())
                    val offerwallResult = withContext(Dispatchers.IO) {
                        usersRepository.inhouseOfferwallCheck().first()
                    }

                    if( !offerwallResult.success ) {
                        throw(Exception(offerwallResult.message))
                    }

                    val offerwallResponse = offerwallResult.data ?: JSONObject()
                    inHouseOfferwalls.clear()
                    val gson = IdolGson.instance
                    if (offerwallResponse.optBoolean("success")) {
                        val array = offerwallResponse.optJSONArray("offerwall")
                        if (array != null) {
                            for (i in 0..<array.length()) {
                                try {
                                    val model =
                                        gson.fromJson<InHouseOfferwallModel?>(
                                            array.getJSONObject(i)
                                                .toString(),
                                            InHouseOfferwallModel::class.java
                                        )
                                    inHouseOfferwalls.add(model!!)

                                    mAdapter!!.add(Any())
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                    Util.closeProgress()
                } else {
                    Util.closeProgress()
                }

                mAdapter?.let {
                    binding.list.post {
                        mAdapter?.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                if (activity != null && isAdded) {
                    Toast.makeText(
                        activity,
                        net.ib.mn.R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                Util.closeProgress()
            }
        }

        // 비디오광고 프로세스 개선
        if (Const.FEATURE_VIDEO_AD_PRELOAD) {
            videoAdManager = VideoAdManager.getInstance(requireActivity(), videoAdListener)
        }
    }

    private fun showVideoAdLoading(show: Boolean) {
        if (loadingTimer != null) {
            loadingTimer!!.cancel()
            loadingTimer!!.purge()
            loadingTimer = null
        }

        if (show) {
            loadingCount = 0
            loadingString =
                getString(net.ib.mn.R.string.loading).replace("...", "").replace("…", "")
            loadingTimer = Timer()
            loadingTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    Handler(Looper.getMainLooper()).post(Runnable {
                        loadingCount = (loadingCount + 1) % 3
                        //                            mVideoBtnText.setText(loadingString + dots.substring(2-loadingCount) + spaces.substring(loadingCount));
                        if (mAdapter != null) {
                            synchronized(this) {
                                mAdapter!!.notifyDataSetChanged()
                            }
                        }
                    })
                }
            }, 100, 500)
        } else {
            if (mAdapter != null) {
                synchronized(this) {
                    mAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    private val videoAdListener: VideoAdManager.OnAdManagerListener =
        object : VideoAdManager.OnAdManagerListener {
            override fun onAdPreparing() {
                showLoading = true
                showVideoAdLoading(true)
            }

            override fun onAdReady() {
                showLoading = false
                showVideoAdLoading(false)
            }

            override fun onAdRewared() {
                showLoading = false
            }

            override fun onAdFailedToLoad() {
                showLoading = false
                showVideoAdLoading(false)
            }

            override fun onAdClosed() {
            }
        }

    public override fun onResume() {
        super.onResume()

        synchronized(this) {
            if (mAdapter != null) mAdapter!!.notifyDataSetChanged()
        }

        context?.let {
            Util.checkVideoAdTimer(requireContext())
        }
    }

    private fun onVideoSaw(type: String?) {
        videoAdUtil!!.onVideoSawCommon(baseActivity, isAdded(), type, IEarnHeartsListener {
            //비광보고 response 받은 후에는 다시 list view update 실행위해  notifyDataSetchanged부름
            mAdapter!!.notifyDataSetChanged()
        })
    }

    private inner class Adapter(context: Context, private val heart_count: Int) :
        ArrayAdapter<Any?>(context, net.ib.mn.R.layout.charge_item) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val binding: ChargeItemBinding

            if(convertView == null) {
                binding = ChargeItemBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                binding.root.tag = binding
            } else {
                binding = convertView.tag as ChargeItemBinding
            }

            update(binding.root, getItem(position), position)
            return binding.root
        }

        override fun update(view: View?, item: Any?, position: Int) = with(view?.tag as ChargeItemBinding) {
            val heart = String.format(
                getString(net.ib.mn.R.string.offer_video_ad_desc),
                getInstance(getActivity()).video_heart.toString() + ""
            )

            // 오토클릭커 밴 처리
            chargeItem.onBanAutoClicker = {
                lifecycleScope.launch {
                    usersRepository.banAutoClicker(
                        { response ->
                            if(response.optBoolean("success")){
                                //성공 적으로 ban 처리가 되었을 경우는 풀리기 전까지 휴면체크 팝업을 부르지 않기 위해
                                //autoclick  카운트에 -1을  넣어줌.
                                Util.setPreference(
                                    context,
                                    AUTO_CLICK_COUNT_PREFERENCE_KEY,
                                    Const.AUTO_CLICKER_BAN_CONFIRM_VALUE
                                )
                            }
                        },
                        {
                        }
                    )
                }
            }

            chargeItem.setVisibility(View.VISIBLE)
            if (position == 0) {
                //접근성 유저 체크 때문에  비디오 아이템인지 여부를 체크한다.
                //비디오 아이템이면,  서버로   로그 남기는 로직 실행됨

                chargeItem.checkVideoItem(true, true)

                //어르신 모드때 max ems 줄여서 타이머랑 subtitle 안겹치게 해줌.
                if (isLargeFont) {
                    subTitle.setMaxEms(8)
                } else {
                    subTitle.setMaxEms(11)
                }

                mGlideRequestManager!!
                    .load(net.ib.mn.R.drawable.img_video_ad)
                    .apply(RequestOptions.circleCropTransform())
                    .error(net.ib.mn.R.drawable.menu_profile_default2)
                    .fallback(net.ib.mn.R.drawable.menu_profile_default2)
                    .placeholder(net.ib.mn.R.drawable.menu_profile_default2)
                    .into(icon)
                title.setText(net.ib.mn.R.string.title_reward_video)

                val txt = ConfigModel.getInstance(requireContext()).videoAdDesc
                if (Const.FEATURE_VIDEO_AD_PRELOAD) {
                    if (VideoAdManager.instance!!.isAdReady) {
                        subTitle.setText(txt)
                    } else {
//                    subTitle.setText(R.string.loading);
                        if (showLoading) {
                            val dots = "..."
                            val spaces = "   "
                            subTitle.setText(
                                loadingString + dots.substring(2 - loadingCount) + spaces.substring(
                                    loadingCount
                                )
                            )
                        } else {
                            subTitle.setText(txt)
                        }
                    }
                } else {
                    subTitle.setText(txt)
                }
                chargeHeart.setText(getInstance(getActivity()).video_heart.toString())
            } else if (inHouseOfferwalls.size > 0 && position <= inHouseOfferwalls.size) {
                chargeItem.checkVideoItem(false, true)
                val model = inHouseOfferwalls.get(position - 1)

                //비광 아이템 이외에는  타이머 gone처라
                chargeHeartLl.setVisibility(View.VISIBLE)

                mGlideRequestManager!!
                    .load(model.icon)
                    .apply(RequestOptions.circleCropTransform())
                    .error(net.ib.mn.R.drawable.menu_profile_default2)
                    .fallback(net.ib.mn.R.drawable.menu_profile_default2)
                    .placeholder(net.ib.mn.R.drawable.menu_profile_default2)
                    .into(icon)
                title.setText(model.title)
                subTitle.setText(model.desc)
                chargeHeart.setText(model.heart.toString())
            }
        }
    }

    private val isLargeFont: Boolean
        //어르신모드 여부 체크
        get() {
            //시스템 font scale  받아옴.
            val scale = requireActivity().getResources().getConfiguration().fontScale
            return scale >= 1.3f
        }

    //무층 아이템 launch intent 세팅용
    private fun setLaunchIntent(key: String, launchIntent: Intent): Intent {
        //프럽용일때 필요한 세팅값 추가함.
        //그외에는  따로 설정값 기존에 없어서  lauchintent 그대로  return

        when (key) {
            Const.FLUV_IN_HOUSE_OFFER_WALL_ITEM_KEY -> {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.putExtra(Const.PARAM_INTENT_IN_HOUSE_OFFER_WALL_FLUV, true)
                launchIntent.putExtra(
                    Const.PARAM_INTENT_IN_HOUSE_OFFER_WALL_PACKAGE_FLUV,
                    if (BuildConfig.CELEB) "actor" else "idol"
                )
            }

            Const.CELEB_IN_HOUSE_OFFER_WALL_ITEM_KEY -> launchIntent.putExtra(
                Const.PARAM_INTENT_IN_HOUSE_OFFER_WALL_CELEB,
                true
            )

            Const.IDOL_IN_HOUSE_OFFER_WALL_ITEM_KEY -> launchIntent.putExtra(
                Const.PARAM_INTENT_IN_HOUSE_OFFER_WALL_IDOL,
                true
            )

            Const.FILLIT_IN_HOUSE_OFFER_WALL_ITEM_KEY -> launchIntent.putExtra(
                Const.PARAM_INTENT_IN_HOUSE_OFFER_WALL_FILLIT,
                if (BuildConfig.CELEB) "actor" else "idol"
            )
        }
        return launchIntent
    }

    private fun observedVM() {
        viewModel!!.moveScreenToVideo.observe(
            getViewLifecycleOwner(),
            object : Observer<Event<Boolean?>?> {
                override fun onChanged(event: Event<Boolean?>?) {
                    if (event != null && event.getContentIfNotHandled() != null) {
                        val isEnabled: Boolean = event.peekContent()!!
                        if (isEnabled) {
                            val endTime = Util.getPreferenceLong(
                                context,
                                Const.VIDEO_TIMER_END_TIME_PREFERENCE_KEY,
                                Const.DEFAULT_VIDEO_DISABLE_TIME
                            )

                            val currentTime = System.currentTimeMillis()
                            val isTimerRunning = endTime != Const.DEFAULT_VIDEO_DISABLE_TIME && endTime > currentTime

                            if ((Const.FEATURE_VIDEO_AD_PRELOAD && !videoAdManager!!.isAdReady)
                                || isTimerRunning
                            ) {
                                lifecycleScope.launch {
                                    val isSet: Boolean = isSetAdNotificationPrefsUseCase()
                                        .mapDataResource { it }
                                        .awaitOrThrow() ?: false

                                    withContext(Dispatchers.Main) {
                                        Util.showVideoAdDisableTimerDialog(
                                            context,
                                            requireActivity().supportFragmentManager,
                                            { v1: View? ->

                                                //팝업 확인 버튼을 누른뒤, timer를 다시 실행한다. -> 비디오 버튼 빨간점 업데이트 용
                                                context?.let {
                                                    UtilK.videoDisableTimer(
                                                        it,
                                                        null,
                                                        null,
                                                        null
                                                    )
                                                }
                                                Util.closeIdolDialog()
                                            },
                                            isAlreadySetNotification = isSet,
                                            false
                                        ) {
                                            lifecycleScope.launch {
                                                postVideoAdNotificationUseCase().collectLatest { result ->
                                                    if (result.success) {
                                                        VideoAdNotifyToastFragment().show(requireActivity().supportFragmentManager, "VideoAdNotifyToast")
                                                    } else {
                                                        withContext(Dispatchers.Main) {
                                                            makeText(requireContext(), result.error?.message, LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }

                                                setAdNotificationPrefsUseCase(true)
                                                    .mapDataResource { it }
                                                    .awaitOrThrow()
                                            }
                                        }
                                    }
                                }
                                return
                            }

                            setUiActionFirebaseGoogleAnalyticsFragment(
                                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                                "myheart_freeheartshop_videoad"
                            )

                            lifecycleScope.launch {
                                setAdNotificationPrefsUseCase(false)
                                    .mapDataResource { it }
                                    .awaitOrThrow()
                            }

                            val intent = createIntent(
                                requireActivity(),
                                Const.ADMOB_REWARDED_VIDEO_FREECHARGE_UNIT_ID
                            )

                            launcher!!.launch(intent)
                        } else {
                            val dialogFragment = AdExceedDialogFragment()
                            dialogFragment.show(
                                getParentFragmentManager(),
                                "AdExceedDialogFragment"
                            )
                        }
                    }
                }
            })
    }

    override fun onItemClick(
        parent: AdapterView<*>?, view: View?, position: Int,
        id: Long
    ) {
        if (position == 0) {
            viewModel!!.moveScreenToVideo()
            return
        }

        if (inHouseOfferwalls.size > 0 && position <= inHouseOfferwalls.size) {
//            setUiActionFirebaseGoogleAnalyticsFragment(Const.ANALYTICS_BUTTON_PRESS_ACTION, "inhouse_actor");

            val model = inHouseOfferwalls.get(position - 1)

            if (model.type.equals(InHouseOfferwallModel.TYPE_APP, ignoreCase = true)) {
                val appPackageName = model._package
                lifecycleScope.launch {
                    usersRepository.inhouseOfferwallCreate(
                        model.key,
                        { response ->
                            Util.closeProgress()

                            if (response.optBoolean("success")) {
                                val launchIntent = baseActivity!!.getPackageManager()
                                    .getLaunchIntentForPackage(appPackageName)
                                setUiActionFirebaseGoogleAnalyticsFragmentWithKey(
                                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                                    "myheart_freeheartshop",
                                    "title",
                                    model.tag
                                )
                                if (launchIntent == null) {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            (model.androidLink + appPackageName).toUri()
                                        )
                                    )
                                } else {
                                    // 미오는 플루터 앱이라 AOS/IOS 동작 맞추고 싶다고 이 방식으로 요청함
                                    if (model.key == Const.MIO_IN_HOUSE_OFFER_WALL_ITEM_KEY) {
                                        val uriBuilder = Uri.Builder()
                                        uriBuilder.scheme(model.key)

                                        uriBuilder.appendQueryParameter(
                                            Const.PARAM_INTENT_IN_HOUSE_OFFER_WALL_MIO_PACKAGE,
                                            if (BuildConfig.CELEB) "actor" else "idol"
                                        )

                                        val host =
                                            Util.getPreference(mContext, Const.PREF_SERVER_URL)
                                        val debug = host == ServerUrl.HOST_TEST
                                        uriBuilder.appendQueryParameter(
                                            Const.PARAM_INTENT_IN_HOUSE_OFFER_WALL_MIO_DEBUG,
                                            debug.toString()
                                        )

                                        val uri = uriBuilder.build()

                                        val mioIntent = Intent(Intent.ACTION_VIEW, uri)
                                        mioIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                                        startActivity(mioIntent)
                                    } else {
                                        startActivity(setLaunchIntent(model.key, launchIntent))
                                    }
                                }
                                requireActivity().runOnUiThread {
                                    synchronized(this) {
                                        try {
                                            inHouseOfferwalls.remove(model)
                                            mAdapter!!.remove(position)
                                            //해당아이템 remove해주고나서 notify불러주기.
                                            mAdapter!!.notifyDataSetChanged()
                                        } catch (e: IndexOutOfBoundsException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        },
                        { throwable ->
                            Util.closeProgress()
                            Toast.makeText(activity, throwable.message, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                // TODO: 추후에 type이 link일 때랑 video일 때 구분해줘야 됨
                lifecycleScope.launch {
                    usersRepository.getOfferwallCallback(
                        userId = getAccount(activity)!!.userModel!!.id,
                        adId = model.adId,
                        listener = { response ->
                            Util.closeProgress()

                            if (response.optBoolean("success")) {
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, model.url.toUri()))
                                    setUiActionFirebaseGoogleAnalyticsFragmentWithKey(
                                        Const.ANALYTICS_BUTTON_PRESS_ACTION,
                                        "myheart_freeheartshop",
                                        "title",
                                        model.tag
                                    )
                                    requireActivity().runOnUiThread {
                                        synchronized(this) {
                                            inHouseOfferwalls.remove(model)
                                            mAdapter!!.remove(position)
                                            //해당아이템 remove해주고나서 notify불러주기.
                                            mAdapter!!.notifyDataSetChanged()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        activity,
                                        getString(net.ib.mn.R.string.msg_error_ok),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    activity,
                                    getString(net.ib.mn.R.string.msg_error_ok),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        errorListener = {
                            Util.closeProgress()
                            Toast.makeText(
                                activity,
                                getString(net.ib.mn.R.string.msg_error_ok),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                return
            }
        }
    }
}
