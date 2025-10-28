package net.ib.mn.onepick

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.addisonelliott.segmentedbutton.SegmentedButtonGroup
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.adapter.OnePickTopicAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.databinding.FragmentOnepickMainBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.model.OnepickTopicModel
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.activity.BaseActivity.Companion.MEZZO_PLAYER_REQ_CODE
import net.ib.mn.activity.MezzoPlayerActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.core.data.repository.OnepickRepositoryImpl
import net.ib.mn.core.data.repository.ThemepickRepositoryImpl
import net.ib.mn.core.model.NewPicksModel
import net.ib.mn.dialog.NotificationSettingDialog
import net.ib.mn.dialog.VoteNotifyToastFragment
import net.ib.mn.fragment.HeartPickFragment
import net.ib.mn.model.ThemepickModel
import net.ib.mn.onepick.OnepickMatchActivity.Companion.PARAM_TOPIC
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Logger
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.preventUnwantedHorizontalScroll
import net.ib.mn.utils.setFirebaseUIAction
import org.json.JSONArray
import java.lang.Exception
import javax.inject.Inject

@AndroidEntryPoint
class OnePickMainFragment : BaseFragment(), OnScrollToTopListener {

    //imgPick
    private var mAccount: IdolAccount? = null
    private lateinit var topicAdapter: OnePickTopicAdapter

    //themePick
    private lateinit var glideRequestManager: RequestManager
    private lateinit var themePickAdapter: ThemePickAdapter

    private lateinit var binding: FragmentOnepickMainBinding

    private var offsetThemepick = 0
    private var totalCountThemepick = 0
    private val limit = 30
    private var offsetImagepick = 0
    private var totalCountImagepick = 0
    private var isLoading = false
    private var comebackFromSetting = false
    private var updateHeartPickId = -1

    private val themePickList = ArrayList<ThemepickModel>()
    private val imagePickList = ArrayList<OnepickTopicModel>()

    @Inject
    lateinit var onepickRepository: OnepickRepositoryImpl
    @Inject
    lateinit var themepickRepository: ThemepickRepositoryImpl

    private var startActivityForResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                IMAGE_PICK_LIST_UPDATE_RESULT_CODE -> {
                    if (!BuildConfig.CELEB) {
                        offsetImagepick = 0
                        loadTopics()
                    }
                }

                THEME_PICK_LIST_UPDATE_RESULT_CODE -> {
                    offsetThemepick = 0
                    loadThemePick()
                }
            }
        }

    val videoAdLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Util.closeProgress()
        if (result.resultCode == Activity.RESULT_CANCELED) {
            Util.handleVideoAdResult(
                requireActivity() as BaseActivity, false, false, MEZZO_PLAYER_REQ_CODE, result.resultCode, null, ""
            ) {
            }
            return@registerForActivityResult
        }
        // 비광 액티비티에 넘겨준 원픽 데이터 받아오기
        val item = result.data?.getSerializableExtra(PARAM_TOPIC) as? OnepickTopicModel
        item?.let {
            it.voteType = it.vote // 이미지픽 api 호출시에는 vote_type에 넣어서 호출해야 한다
            startActivityForResultLauncher.launch(
                OnepickMatchActivity.createIntent(
                    context ?: return@registerForActivityResult,
                    it
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glideRequestManager = Glide.with(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_onepick_main, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tapDefaultValue = BuildConfig.CELEB

        with(binding) {
            //이미지픽을 보여줄지 , 테마픽 보여줄지 결졍.
            val isImagePick = arguments?.getBoolean(BaseActivity.EXTRA_IS_IMAGEPICK, tapDefaultValue)

            //imgPick
            mAccount = IdolAccount.getAccount(activity)
            topicAdapter = OnePickTopicAdapter(getNewPick(IMAGE_PICK))
            imagePickRv.adapter = topicAdapter
            imagePickRv.addOnScrollListener(imagepickOnScrollListener)

            imagePickRv.preventUnwantedHorizontalScroll()

            //themePick
            themePickAdapter =
                ThemePickAdapter(glideRequestManager, getNewPick(THEME_PICK)) {
                    setFirebaseUIAction(GaAction.THEME_PICK_PRELAUNCH)
                    startActivityForResultLauncher.launch(
                        ThemePickRankActivity.createIntent(
                            requireContext(),
                            it,
                            true
                        ),
                    )
                }
            themePickRv.adapter = themePickAdapter
            themePickRv.addOnScrollListener(themepickOnScrollListener)
            themePickRv.preventUnwantedHorizontalScroll()

            setLinkStatus()

            setClickListener()
            initTabStatus(isImagePick ?: tapDefaultValue)
            setAdapterClickListener()
        }
    }

    override fun onResume() {
        super.onResume()

        if (comebackFromSetting) {
            comebackFromSetting = false

            if (NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                postImagePickSettingNotification()
            }
        }
    }

    private val imagepickOnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager
            val lastVisibleItemPosition = (layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
            val totalItemCount = layoutManager.itemCount

            if (!isLoading &&
                !recyclerView.canScrollVertically(1) &&
                lastVisibleItemPosition >= totalItemCount - 1 &&
                totalItemCount < totalCountImagepick) {
                isLoading = true
                offsetImagepick += limit
                loadTopics()
            }
        }
    }

    private val themepickOnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager
            val lastVisibleItemPosition = (layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
            val totalItemCount = layoutManager.itemCount

            if (!recyclerView.canScrollVertically(1) &&
                lastVisibleItemPosition >= totalItemCount - 1 &&
                totalItemCount < totalCountThemepick) {
                offsetThemepick += limit
                loadThemePick()
            }
        }
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)
        if (!isVisible) return

        val shouldLoadData = themePickList.isEmpty() || imagePickList.isEmpty()
        if (shouldLoadData) {
            loadData()
        }
    }

    fun loadData() {
        // 전체 리스트를 새로 갱신
        offsetThemepick = 0
        offsetImagepick = 0

        if(!BuildConfig.CELEB) loadTopics()
        loadThemePick()
    }

    private fun getNewPick(category: Int): Boolean {
        val listType = object : TypeToken<NewPicksModel>() {}.type
        val newPicks: NewPicksModel? =
            IdolGson
                .getInstance()
                .fromJson(Util.getPreference(requireContext(), Const.PREF_NEW_PICKS), listType)

        return newPicks?.let {
            when (category) {
                IMAGE_PICK -> {
                    newPicks.onepick
                }
                THEME_PICK -> {
                    newPicks.themepick
                }
                else -> {
                    false
                }
            }
        } ?: false
    }

    private fun setAdapterClickListener() {
        topicAdapter.setOnClickListener(object : OnePickTopicAdapter.ClickListener {
            override fun goOnePickMatch(item: OnepickTopicModel) {
                startActivityForResultLauncher.launch(
                    OnepickMatchActivity.createIntent(
                        context ?: return,
                        item
                    )
                )
            }

            override fun goOnePickResult(item: OnepickTopicModel) {
                startActivityForResultLauncher.launch(
                    OnepickResultActivity.createIntent(
                        context ?: return,
                        item
                    )
                )
            }

            override fun goStore() {
                startActivityForResultLauncher.launch(
                    NewHeartPlusActivity.createIntent(
                        context ?: return,
                        NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP
                    )
                )
            }

            override fun showVideoAd(item: OnepickTopicModel) {
                Util.showProgress(activity)
                val intent = MezzoPlayerActivity.createIntent(requireActivity(), Const.ADMOB_REWARDED_VIDEO_ONEPICK_UNIT_ID)
                // 비광 액티비티에 이미지픽 데이터 넘겨주고 시청 완료 후 다시 받아온다
                val bundle = Bundle()
                bundle.putSerializable(PARAM_TOPIC, item)
                intent.putExtras(bundle)
                videoAdLauncher.launch(intent)
            }

            override fun setNotification(item: OnepickTopicModel) {
                updateHeartPickId = item.id
                openNotificationSetting()
            }
        })

        themePickAdapter.setOnClickListener(object : ThemePickAdapter.ClickListener {
            override fun goThemePickRank(item: ThemepickModel) {
                startActivityForResultLauncher.launch(
                    ThemePickRankActivity.createIntent(
                        context ?: return,
                        item,
                    ),
                )
            }

            override fun goThemePickResult(item: ThemepickModel) {
                startActivityForResultLauncher.launch(
                    ThemePickResultActivity.createIntent(
                        context ?: return,
                        item,
                        false
                    )
                )
            }

            override fun goStore() {
                startActivityForResultLauncher.launch(
                    NewHeartPlusActivity.createIntent(
                        context ?: return,
                        NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP
                    )
                )
            }

        })
    }

    private fun loadTopics() {
        lifecycleScope.launch(Dispatchers.IO) {
            onepickRepository.get(
                offset = offsetImagepick,
                limit = limit,
                { response ->
                    if (response.optBoolean("success")) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                totalCountImagepick = response.optJSONObject("meta")?.optInt("total_count") ?:0
                                val topicJsonArray: JSONArray = response.getJSONArray("objects")
                                val topicSize = topicJsonArray.length()
                                binding.tvImagePickDataLoad.visibility = View.GONE

                                if(topicSize <=0) {
                                    binding.llEmptyWrapperImagePick.visibility = View.VISIBLE
                                    binding.imagePickRv.visibility = View.GONE
                                    return@launch
                                }
                                binding.llEmptyWrapperImagePick.visibility = View.GONE
                                binding.imagePickRv.visibility = View.VISIBLE

                                val listType = object : TypeToken<List<OnepickTopicModel>>() {}.type
                                val topicList = IdolGson.getInstance(false)
                                    .fromJson<List<OnepickTopicModel>>(
                                        topicJsonArray.toString(),
                                        listType
                                    )

                                if(offsetImagepick == 0) {
                                    imagePickList.clear()
                                }
                                imagePickList.addAll(topicList)
                                topicAdapter.submitList(imagePickList)
                                topicAdapter.notifyDataSetChanged()
                            } catch (e: Exception) {
                                UtilK.showExceptionDialog(
                                    requireContext(),
                                    errorMsg = e.stackTraceToString(),
                                )

                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            UtilK.showExceptionDialog(
                                requireContext()
                            )
                        }
                        isLoading = false
                    }
                }, { throwable ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        UtilK.showExceptionDialog(
                            requireContext(),
                        )
                    }
                    isLoading = false
                }
            )
        }
    }

    //테마픽 불러오는 함수
    private fun loadThemePick() {
        lifecycleScope.launch(Dispatchers.IO) {
            themepickRepository.get(
                offset = offsetThemepick,
                limit = limit,
                listener = { response ->
                    if(response.optBoolean("success")){
                        try{
                            lifecycleScope.launch(Dispatchers.Main) {
                                totalCountThemepick = response.optJSONObject("meta")?.optInt("total_count") ?:0
                                binding.emptyViewDataLoadTheme.visibility = View.GONE

                                binding.llEmptyWrapperImagePick.visibility = View.GONE
                                binding.themePickRv.visibility = View.VISIBLE

                                val themePickJsonArray = response.getJSONArray("objects")
                                val listType = object : TypeToken<List<ThemepickModel>>() {}.type
                                val list = IdolGson.getInstance(false)
                                    .fromJson<List<ThemepickModel>>(themePickJsonArray.toString(), listType)
                                if(offsetThemepick == 0) {
                                    themePickList.clear()
                                }
                                themePickList.addAll(list)

                                val themePickSize = themePickList.size
                                if(themePickSize <= 0) {
                                    binding.llEmptyWrapperImagePick.visibility = View.VISIBLE
                                    binding.themePickRv.visibility = View.GONE
                                    return@launch
                                }

                                themePickAdapter.submitList(themePickList)
                                themePickAdapter.notifyDataSetChanged()
                            }
                        }catch (e:Exception){
                            e.printStackTrace()
                            lifecycleScope.launch(Dispatchers.Main) {
                                UtilK.showExceptionDialog(requireContext(),
                                    null)
                            }
                        }
                    }
                },
                errorListener = { throwable ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        Util.log(throwable.message)
                        UtilK.showExceptionDialog(requireContext(),
                            throwable = throwable)
                    }
                }
            )
        }
    }

    // 링크 화면에서 왔을땐 menuVisible true체크를 해준다.
    // 기존 메인 화면 뷰페이저에 달려있어서 visible처리를 해주지 않으면 데이터를 안불러옴.
    private fun setLinkStatus() {
        val isFromAlternateLinkFragmentActivity = arguments?.getBoolean(
            BaseActivity.EXTRA_IS_FROM_ALTERNATE_LINK_FRAGMENT_ACTIVITY,
            false
        )

        if (isFromAlternateLinkFragmentActivity == false) {
            return
        }

        setMenuVisibility(true)
    }

    private fun addHeartPickFragment() {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.fc_heart_pick_fragment, HeartPickFragment::class.java, null)
        }
    }

    private fun setClickListener() = with(binding) {
        setupClickListener(
            inSegmentGroupOnepickForCeleb.btnThemePick,
            View.VISIBLE,
            View.GONE,
            clThemePick,
            fcHeartPickFragment
        )
        setupClickListener(
            inSegmentGroupOnepickForCeleb.btnHeartPick,
            View.GONE,
            View.VISIBLE,
            clThemePick,
            fcHeartPickFragment
        )
        setupClickListener(
            inSegmentGroupOnepick.btnThemePick,
            View.GONE,
            View.VISIBLE,
            clImgPick,
            clThemePick
        )
        setupClickListener(
            inSegmentGroupOnepick.btnOnePick,
            View.VISIBLE,
            View.GONE,
            clImgPick,
            clThemePick
        )

        setupPositionChangeListener(
            inSegmentGroupOnepickForCeleb.sbgOnePickGroup,
            fcHeartPickFragment,
            clThemePick
        )
        setupPositionChangeListener(inSegmentGroupOnepick.sbgOnePickGroup, clThemePick, clImgPick)
    }

    private fun setupPositionChangeListener(
        segmentGroup: SegmentedButtonGroup,
        firstView: View,
        secondView: View
    ) {
        segmentGroup.setOnPositionChangedListener { position ->
            if (position == 0) {
                toggleVisibility(firstView, secondView, View.VISIBLE, View.GONE)
            } else {
                toggleVisibility(firstView, secondView, View.GONE, View.VISIBLE)
            }
        }
    }

    private fun setupClickListener(
        button: View,
        visibility1: Int,
        visibility2: Int,
        view1: View,
        view2: View
    ) {
        button.setOnClickListener {
            view1.visibility = visibility1
            view2.visibility = visibility2
        }
    }

    private fun initTabStatus(isImagePick: Boolean) = with(binding) {
        val isCeleb = BuildConfig.CELEB

        if (isCeleb) {
            Logger.v("isCeleb : $isCeleb $isImagePick")
            addHeartPickFragment()
            toggleVisibility(
                view1 = inSegmentGroupOnepickForCeleb.root,
                view2 = inSegmentGroupOnepick.root,
                visibility1 = View.VISIBLE,
                visibility2 = View.GONE
            )

            inSegmentGroupOnepickForCeleb.sbgOnePickGroup.setPosition(
                if (isImagePick) 0 else 1,
                true
            )
            return@with
        }

        toggleVisibility(
            inSegmentGroupOnepickForCeleb.root,
            inSegmentGroupOnepick.root,
            View.GONE,
            View.VISIBLE
        )

        inSegmentGroupOnepick.sbgOnePickGroup.setPosition(if(isImagePick) 1 else 0, true)
    }

    private fun toggleVisibility(view1: View, view2: View, visibility1: Int, visibility2: Int) {
        view1.visibility = visibility1
        view2.visibility = visibility2
    }

    override fun onScrollToTop() {
        if(!::binding.isInitialized) return
        binding.imagePickRv.scrollToPosition(0)
        binding.themePickRv.scrollToPosition(0)
        if(BuildConfig.CELEB) {
            // 하트픽 프래그먼트 가져오기
            val heartPickFragment = childFragmentManager.findFragmentById(R.id.fc_heart_pick_fragment) as? HeartPickFragment
            heartPickFragment?.onScrollToTop()
        }
    }

    private fun openNotificationSetting() {
        val isNotificationOn =
            NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()

        if (!isNotificationOn) {
            val dialog = NotificationSettingDialog() {
                comebackFromSetting = true
            }
            dialog.show(requireActivity().supportFragmentManager, "notification_setting_dialog")
        } else {
            postImagePickSettingNotification()
        }
    }

    private fun postImagePickSettingNotification() {
        lifecycleScope.launch {
            onepickRepository.postOpenImagePickNotification(
                id = updateHeartPickId,
                listener = { response ->
                    if(response.optBoolean("success")) {
                        val idx = imagePickList.indexOfFirst { it.id == updateHeartPickId }
                        if (idx != -1) {
                            imagePickList[idx].alarm = true
                            topicAdapter.notifyItemChanged(idx)

                            VoteNotifyToastFragment().show(requireActivity().supportFragmentManager, "VoteNotifyToast")
                        }
                    }
                },
                errorListener = { throwable ->
                    val msg = throwable.message
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    companion object {
        const val IMAGE_PICK_LIST_UPDATE_RESULT_CODE = 100
        const val THEME_PICK_LIST_UPDATE_RESULT_CODE = 101

        private const val THEME_PICK = 1
        private const val IMAGE_PICK = 2
    }

}