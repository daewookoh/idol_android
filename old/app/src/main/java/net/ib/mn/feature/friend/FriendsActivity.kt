package net.ib.mn.feature.friend

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.IdolApplication
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.FriendDeleteActivity
import net.ib.mn.activity.FriendSearchActivity
import net.ib.mn.activity.FriendsRequestActivity
import net.ib.mn.activity.MainActivity
import net.ib.mn.activity.NewFriendsActivity
import net.ib.mn.adapter.FriendsAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.friends.FriendsRepositoryImpl
import net.ib.mn.core.domain.usecase.GetConfigSelfUseCase
import net.ib.mn.databinding.ActivityFriendsBinding
import net.ib.mn.domain.usecase.GetIdolsByTypeAndCategoryUseCase
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.CelebTutorialBits
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ExtendedDataHolder
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.livedata.SingleEventObserver
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

@AndroidEntryPoint
class FriendsActivity : BaseActivity(),
    FriendsAdapter.OnClickListener {

    @Inject
    lateinit var friendsRepository: FriendsRepositoryImpl

    @Inject
    lateinit var getConfigSelfUseCase: GetConfigSelfUseCase

    @Inject
    lateinit var usersRepository: UsersRepository

    @Inject
    lateinit var getIdolsByTypeAndCategoryUseCase: GetIdolsByTypeAndCategoryUseCase

    private val viewModel: FriendsViewModel by viewModels()

    private var friendsResponse: JSONObject? = null    // 친구 삭제/친신 취소시 재사용하기 위함

    private lateinit var imm: InputMethodManager

    private lateinit var mGlideRequestManager: RequestManager

    private var mSendingHeartIds = ArrayList<Int>()
    private var mRefreshTimer: Timer? = null

    private var mFriends = ArrayList<FriendModel>()
    var friendsAdapter: FriendsAdapter? = null

    private var createListener: Boolean = false

    private lateinit var binding: ActivityFriendsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        observeVM()
    }

    override fun onResume() {
        super.onResume()
        restartTimer()

    }

    override fun onPause() {
        super.onPause()
        if (mRefreshTimer != null) {
            mRefreshTimer?.cancel()
            mRefreshTimer?.purge()
            mRefreshTimer = null
        }
    }

    @OptIn(UnstableApi::class)
    override fun onBackPressed() {
        try {
            // 푸시에서 넘어온 경우 백버튼 누르면 앱종이 아닌 메인화면으로 이동. 또한 나의 정보에서 쿠폰 진입시 나의 정보 화면이 파괴되어 메인으로 가는 현상 방지
            if (IdolApplication.Companion.getInstance(this).mainActivity == null && intent.getBooleanExtra(
                    EXTRA_IS_FROM_PUSH,
                    false
                )
            ) {
                startActivity(MainActivity.Companion.createIntent(this, false))
                finish()
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            super.onBackPressed()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.friend_menu, menu)

        val itemView = menu?.findItem(R.id.action_friends_add)?.actionView
        val lottie = itemView?.findViewById<LottieAnimationView>(R.id.lottie_friend_add)

        val isCurrentTutorial = if (BuildConfig.CELEB) {
            TutorialManager.getTutorialIndex() == CelebTutorialBits.FRIEND_NEW_FACE
        } else {
            TutorialManager.getTutorialIndex() == TutorialBits.FRIEND_NEW_FACE
        }
        if (isCurrentTutorial) {
            lottie?.let {
                it.visibility = View.VISIBLE
                it.setAnimation("tutorial_heart.json")
                it.repeatCount = LottieDrawable.INFINITE
                it.playAnimation()
            }
        } else {
            lottie?.visibility = View.GONE
        }

        itemView?.setOnClickListener {
            if (lottie?.visibility == View.VISIBLE) {
                updateTutorial(TutorialManager.getTutorialIndex())
                lottie.setAnimation("tutorial_heart_touch.json")
                lottie.repeatCount = 0
                lottie.playAnimation()

                lottie.removeAllAnimatorListeners()
                lottie.addAnimatorListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(animation: Animator) {
                        lottie.visibility = View.GONE
                        handleFriendAddClick()
                    }

                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                })
            } else {
                handleFriendAddClick()
            }
        }
        return true
    }

    private fun observeVM() = with(viewModel) {
        showBannerTooltip.observe(this@FriendsActivity, SingleEventObserver { isShow ->
            binding.clToolTip.isVisible = isShow
        })
        inviteData.observe(this@FriendsActivity, SingleEventObserver { payload ->
            setUiActionFirebaseGoogleAnalyticsActivity(
                GaAction.FRIEND_GO_INVITE.actionValue,
                GaAction.FRIEND_GO_INVITE.label
            )
            startActivity(Intent(this@FriendsActivity, FriendInviteActivity::class.java).apply {
                putExtra(FriendInviteActivity.INVITE_PAYLOAD, payload)
            })
        })
    }

    private fun setTutorials() {
        if (TutorialManager.getTutorialIndex() == TutorialBits.FRIEND_INVITE) {
            setupLottieTutorial(binding.lottieTutorialInvitation) {
                updateTutorial(TutorialBits.FRIEND_INVITE)
                tryInvite()
            }
        } else {
            binding.lottieTutorialInvitation.visibility = View.GONE
        }
    }

    private fun setCelebTutorials() {
        if (TutorialManager.getTutorialIndex() == CelebTutorialBits.FRIEND_INVITE) {
            setupLottieTutorial(binding.lottieTutorialInvitation) {
                updateTutorial(CelebTutorialBits.FRIEND_INVITE)
                tryInvite()
            }
        } else {
            binding.lottieTutorialInvitation.visibility = View.GONE
        }
    }

    private fun tryInvite() {
        viewModel.invite()
//        lifecycleScope.launch(Dispatchers.IO) {
//            val language = languagePreferenceRepository.getSystemLanguage()
//            withContext(Dispatchers.Main) {
//                startActivity(Intent(this@FriendsActivity, FriendInviteActivity::class.java).apply {
//                    putExtra("test", language)
//                })
//            }
//        }
//        setUiActionFirebaseGoogleAnalyticsActivity(
//            GaAction.INVITE.actionValue,
//            GaAction.INVITE.label
//        )
//        UtilK.tryKakaoInvite(this@FriendsActivity, getIdolsByTypeAndCategoryUseCase)
    }

    private fun openNewFaces() {
        setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "friends_new_faces"
        )

        setResponseData()
        val intent = NewFriendsActivity.Companion.createIntent(this)
        startActivityForResult(
            intent,
            RequestCode.FRIEND_ADD.value
        )
    }

    private fun handleFriendAddClick() {
        if ("F".equals(ConfigModel.Companion.getInstance(this).friendApiBlock, ignoreCase = true)) {
            Util.Companion.showDefaultIdolDialogWithBtn1(
                this,
                null,
                getString(R.string.friend_api_block)
            ) { Util.Companion.closeIdolDialog() }
        } else {
            openNewFaces()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.action_friends_add -> {
                handleFriendAddClick()
                return true
            }

            R.id.action_friends_remove -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "friends_delete"
                )

                setResponseData()
                val intent = FriendDeleteActivity.Companion.createIntent(this)
                startActivityForResult(
                    intent,
                    RequestCode.FRIEND_REMOVE.value
                )
                return true
            }

            R.id.action_friends_request -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "friends_request"
                )

                setResponseData()
                val intent = FriendsRequestActivity.Companion.createIntent(this, 1)
                startActivityForResult(
                    intent,
                    RequestCode.FRIEND_REQUEST.value
                )
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RequestCode.FRIEND_REQUEST.value,
            RequestCode.FRIEND_REMOVE.value -> {
                if (resultCode == ResultCode.FRIEND_REMOVED.value ||
                    resultCode == ResultCode.FRIEND_REQUESTED.value ||
                    resultCode == ResultCode.FRIEND_REQUEST_CANCELED.value
                ) {
                    if (friendsAdapter != null) {
                        getFriends()
                    }
                }
            }

            RequestCode.FRIEND_ADD.value,
            RequestCode.FRIEND_SEARCH.value -> {
                if (resultCode == ResultCode.FRIEND_REQUESTED.value) {
                    if (friendsAdapter != null) {
                        getFriends()
                    }
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onItemClicked(item: UserModel, view: View, position: Int) {
        when (view.id) {
            R.id.picture,
            R.id.userInfo,
            R.id.requesterPicture,
            R.id.requesterInfo -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "friends_feed"
                )
                startActivity(FeedActivity.Companion.createIntent(this, item))
            }

            R.id.btnSendHeart -> {
                giveHeartToFriend(item)
            }

            R.id.sectionBtnSendHeart -> {
                if (mFriends.size == 0) {
                    return
                }
                Util.Companion.showProgress(this, false)
                // 집계시간 체크 안하고 서버 응답으로 처리하기
                giveHeartToFriendAll(mFriends)
                return
            }

            R.id.sectionBtnTakeHeart -> {
                Util.Companion.showProgress(this, false)
                receiveHeartAll()
            }

            R.id.btnAcceptAll,
            R.id.btnDecline -> {
                val acceptListener: (JSONObject) -> Unit = { response ->
                    Util.Companion.closeProgress()
                    Util.Companion.showDefaultIdolDialogWithBtn1(
                        this@FriendsActivity, null,
                        getString(R.string.desc_accepted_friend_request)
                    ) {
                        Util.Companion.closeIdolDialog()
                        getFriends()
                    }
                }
                val declineListener: (JSONObject) -> Unit = { response ->
                    Util.Companion.closeProgress()
                    Util.Companion.showDefaultIdolDialogWithBtn1(
                        this@FriendsActivity, null,
                        getString(R.string.desc_declined_friend_request)
                    ) {
                        Util.Companion.closeIdolDialog()
                        getFriends()
                    }
                }
                val errorListener: (Throwable) -> Unit = { throwable ->
                    Util.Companion.closeProgress()
                    if (!TextUtils.isEmpty(throwable.message)) {
                        Util.Companion.showDefaultIdolDialogWithBtn1(
                            this@FriendsActivity, null,
                            throwable.message
                        ) { Util.Companion.closeIdolDialog() }
                    } else {
                        Util.Companion.showDefaultIdolDialogWithBtn1(
                            this@FriendsActivity, null,
                            getString(R.string.msg_error_ok)
                        ) { Util.Companion.closeIdolDialog() }
                    }
                }

                if (view.id == R.id.btnAcceptAll) {
                    Util.Companion.showDefaultIdolDialogWithBtn2(
                        this,
                        getString(R.string.title_accept_friend_request),
                        getString(R.string.desc_accept_friend_request),
                        R.string.yes, R.string.no,
                        true, false,
                        {
                            Util.Companion.showProgress(this)
                            MainScope().launch {
                                friendsRepository.respondAllFriendRequest(
                                    acceptListener,
                                    errorListener
                                )
                            }
                        },
                        { Util.Companion.closeIdolDialog() })
                } else if (view.id == R.id.btnDecline) {
                    Util.Companion.showDefaultIdolDialogWithBtn2(
                        this,
                        getString(R.string.title_decline_friend_request),
                        getString(R.string.desc_decline_friend_request),
                        {
                            Util.Companion.showProgress(this@FriendsActivity)
                            MainScope().launch {
                                friendsRepository.respondFriendRequest(
                                    item.id.toLong(),
                                    false,
                                    declineListener,
                                    errorListener
                                )
                            }
                        },
                        { Util.Companion.closeIdolDialog() })
                }
            }
        }
    }

    private fun init() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_friends)
        binding.friendsMain.applySystemBarInsets()

        mGlideRequestManager = Glide.with(this)
        createListener = true

        setSupportActionBar(binding.toolbarMenuFriends)
        val actionbar = supportActionBar
        actionbar?.setDisplayHomeAsUpEnabled(true)
        actionbar?.setTitle(R.string.friend)

        setBottomBannerColor()
        setSearch()
        setInvitationRewardHeart()
        setFriends()
        getFriends()

        binding.clToolTip.setOnClickListener {
            viewModel.updateBannerTooltipState()
            binding.clToolTip.visibility = View.GONE
        }

        if (BuildConfig.CELEB) setCelebTutorials() else setTutorials()
    }

    private fun setBottomBannerColor() {
        if (BuildConfig.CELEB) {
            binding.tvBannerText.setTextColor(getColor(R.color.purple_500))
            ImageViewCompat.setImageTintList(
                binding.iconDisclosure,
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_400))
            )
        }
    }

    //아직 친구가 없어요
    private fun showEmptyView() {
        binding.emptyView.visibility = View.VISIBLE
        binding.loadingView.visibility = View.GONE
        binding.rvFriends.visibility = View.GONE
    }

    //친구목록
    private fun hideEmptyView() {
        binding.emptyView.visibility = View.GONE
        binding.loadingView.visibility = View.GONE
        binding.rvFriends.visibility = View.VISIBLE
    }

    //데이터 가져오는 중
    private fun showWaitingView() {
        binding.emptyView.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE
        binding.rvFriends.visibility = View.GONE
    }

    private fun restartTimer() {
        if (mRefreshTimer != null) {
            mRefreshTimer?.cancel()
            mRefreshTimer?.purge()
        }
        friendsAdapter?.notifyDataSetChanged()
        if (friendsAdapter != null && friendsAdapter!!.itemCount > 0) {
            hideEmptyView()   //친구목록
            mRefreshTimer = Timer()
            mRefreshTimer?.schedule(object : TimerTask() {

                override fun run() {
                    Handler(Looper.getMainLooper()).post {
                        friendsAdapter?.notifyDataSetChanged()
                    }
                }
            }, 1000, 1000)
        }
        //친구 없음
        else if (!createListener && friendsAdapter != null && friendsAdapter!!.itemCount == 0) {
            showEmptyView()
        } else {
            createListener = false
            showWaitingView()   //데이터 가져오는 중
        }
    }

    private fun showSoftKeyboard() {
        binding.searchBar.etSearch.requestFocus()
        binding.searchBar.etSearch.isCursorVisible = true
        imm.showSoftInput(binding.searchBar.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideSoftKeyboard() {
        binding.searchBar.etSearch.isCursorVisible = false
        imm.hideSoftInputFromWindow(binding.searchBar.etSearch.windowToken, 0)
    }

    private fun cleanKeyword() {
        binding.searchBar.etSearch.text = null
        binding.searchBar.etSearch.clearFocus()
    }

    private fun setSearch() {
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        binding.searchBar.etSearch.setOnClickListener {
            showSoftKeyboard()
        }
        binding.searchBar.etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchUser()
                true
            } else {
                false
            }
        }
        binding.searchBar.btnSearch.setOnClickListener {
            searchUser()
        }
    }

    private fun searchUser() {

        val searchText = binding.searchBar.etSearch.text.toString().trim()
        if (searchText.length > 1) {
            hideSoftKeyboard()
            cleanKeyword()
            setResponseData()
            startActivityForResult(
                FriendSearchActivity.Companion.createIntent(this, searchText),
                RequestCode.FRIEND_SEARCH.value
            )
        } else {
            Toast.Companion.makeText(
                this,
                String.format(getString(R.string.comment_minimum_characters), 2),
                Toast.Companion.LENGTH_SHORT
            ).show()
        }
    }

    private fun setInvitationRewardHeart() {
        binding.llInvitation.setOnClickListener {
            if (binding.lottieTutorialInvitation.isVisible) return@setOnClickListener
            viewModel.invite()
//            setUiActionFirebaseGoogleAnalyticsActivity(
//                GaAction.INVITE.actionValue,
//                GaAction.INVITE.label
//            )
//            UtilK.tryKakaoInvite(this, getIdolsByTypeAndCategoryUseCase)

//            lifecycleScope.launch(Dispatchers.IO) {
////                val language = languagePreferenceRepository.getSystemLanguage()
//                withContext(Dispatchers.Main) {
//                    startActivity(
//                        Intent(
//                            this@FriendsActivity,
//                            FriendInviteActivity::class.java
//                        ).apply {
//                            putExtra("test", language)
//                        })
//                }
//            }
        }

        lifecycleScope.launch {
            val result = getConfigSelfUseCase().first()
            val response = result.data ?: return@launch
            if (response.optBoolean("success")) {
                ConfigModel.Companion.getInstance(this@FriendsActivity).parse(response)
                val recommendHeart =
                    ConfigModel.Companion.getInstance(this@FriendsActivity).recommendHeart

                if (recommendHeart > 0) {
//                    binding.tvInvitationReward.text =
//                        String.format(
//                            getString(R.string.btn_invite_friends_kakao_reward),
//                            NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this@FriendsActivity))
//                                .format(recommendHeart)
//                        )
//                    binding.tvInvitationReward.visibility = View.VISIBLE
                } else {
//                    binding.tvInvitationReward.visibility = View.GONE
                }
            }
        }
    }

    private fun setFriends() {
        friendsAdapter = FriendsAdapter(
            this,
            mGlideRequestManager,
            mFriends,
            true,
            this
        )
        binding.rvFriends.adapter = friendsAdapter
    }

    private fun getFriends() {
        val listener: (JSONObject) -> Unit = { response ->
            friendsResponse = response
            if (response?.optBoolean("success")!!) {
                mFriends.clear()
                val gson = IdolGson.getInstance()
                val array = response.getJSONArray("objects")
                val friends = ArrayList<FriendModel>()
                val requesters = ArrayList<FriendModel>()
                val waiters = ArrayList<FriendModel>()

                if (array.length() < Const.THE_NUMBER_OF_FRIENDS_LIMIT) {
                    Util.Companion.setPreference(
                        this@FriendsActivity,
                        Const.PREF_FRIENDS_LIMIT,
                        false
                    )
                } else {
                    Util.Companion.setPreference(
                        this@FriendsActivity,
                        Const.PREF_FRIENDS_LIMIT,
                        true
                    )
                }

                try {
                    for (i in 0 until array.length()) {
                        val model = gson.fromJson(
                            array.getJSONObject(i).toString(),
                            FriendModel::class.java
                        )

                        if (model.isFriend == "Y") {
                            friends.add(model)
                        } else {
                            if (model.userType == FriendModel.Companion.SEND_USER) {
                                requesters.add(model)
                            } else if (model.userType == FriendModel.Companion.RECV_USER) {
                                waiters.add(model)
                            }
                        }
                    }

                    // 가나다순 정렬
                    friends.sortWith(Comparator { lhs, rhs -> lhs.user.nickname.compareTo(rhs.user.nickname) })

                    mFriends.addAll(friends)

                    if (friends.size == 0) {
                        showEmptyView()
                        friendsAdapter?.notifyDataSetChanged()
                    } else {
                        hideEmptyView()
                        restartTimer()
                    }

                    if (requesters.size > 0) {
                        binding.requestCount.text = "${requesters.size}"
                        binding.noticeFriendRequest.apply {
                            visibility = View.VISIBLE
                            setOnClickListener {
                                setUiActionFirebaseGoogleAnalyticsActivity(
                                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                                    "friends_request"
                                )

                                setResponseData()
                                val intent = FriendsRequestActivity.Companion.createIntent(
                                    this@FriendsActivity,
                                    0
                                )
                                startActivityForResult(
                                    intent,
                                    RequestCode.FRIEND_REQUEST.value
                                )
                            }
                        }
                    } else {
                        binding.noticeFriendRequest.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    showEmptyView()
                    e.printStackTrace()
                }
            } else {
                showEmptyView()
                val responseMsg = ErrorControl.parseError(this@FriendsActivity, response)
                if (responseMsg != null) {
                    Toast.Companion.makeText(
                        this@FriendsActivity,
                        responseMsg,
                        Toast.Companion.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            showEmptyView()
            Toast.Companion.makeText(
                this@FriendsActivity,
                R.string.error_abnormal_exception,
                Toast.Companion.LENGTH_SHORT
            ).show()
        }

        MainScope().launch {
            friendsRepository.getFriendsSelf(listener, errorListener)
        }
    }

    private fun receiveHeartAll() {
        val prefs = getSharedPreferences(
            "heart",
            MODE_PRIVATE
        )
//        val lastGaveTime = prefs.getLong("take_heart" , -1)
//        if (lastGaveTime > 0) {
//            val expire = lastGaveTime + 1000 * 3
//            val currTime = System.currentTimeMillis()
//            if (currTime < expire) {
//                val format = getString(R.string.already_sent_heart__format)
//                val timeFormat = SimpleDateFormat("m:ss")
//                val time = timeFormat.format(Date(expire - currTime))
//
//                val text = "이미 하트를 받으셨습니다 3초만 기다려주세요...."
//                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
//                return
//            }
//        }

        MainScope().launch {
            friendsRepository.receiveFriendHeart(
                { response ->
                    if (response.optBoolean("success")) {
                        Util.Companion.closeProgress()
                        val currTime = System.currentTimeMillis()
                        prefs.edit().putLong("take_heart", currTime).apply()

                        val receiveHeart = response.optInt("heart")
                        val msg = if (receiveHeart == 0) {
                            getString(R.string.label_friend_heart_empty)
                        } else if (receiveHeart == 1) {
                            getString(R.string.label_friend_heart_one)
                        } else {
                            String.format(
                                getString(R.string.label_friend_heart_format),
                                receiveHeart
                            )
                        }

                        Util.Companion.showDefaultIdolDialogWithBtn1(
                            this@FriendsActivity,
                            null,
                            msg,
                            { Util.Companion.closeIdolDialog() },
                            true
                        )
                    } else {
                        Util.Companion.closeProgress()
                        val msg = response.optString("msg")
                        if (response.optInt("gcode") == ErrorControl.ERROR_88888) {
                            Util.Companion.showDefaultIdolDialogWithBtn1(
                                this@FriendsActivity,
                                null,
                                msg
                            ) {
                                Util.Companion.closeIdolDialog()
                            }
                        }
                    }
                },
                { throwable ->
                    Util.Companion.closeProgress()
                    Toast.Companion.makeText(
                        this@FriendsActivity,
                        R.string.error_abnormal_exception,
                        Toast.Companion.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun giveHeartToFriendAll(items: List<FriendModel>) {
        val prefs = getSharedPreferences("heart", MODE_PRIVATE)
        val lastGaveAllTime = prefs.getLong("send_heart_all", -1)
        if (lastGaveAllTime > 0) {
            val expire = lastGaveAllTime + 60 * 1000 * 10
            val currTime = System.currentTimeMillis()
            if (currTime < expire) {
                Util.Companion.closeProgress()
                return
            }
        }

        val filtered = ArrayList<UserModel>()
        for (item in items) {
            val lastGaveTime = prefs.getLong("send_heart_${item.user.id}", -1)
            if (lastGaveTime > 0) {
                val expire = lastGaveTime + 60 * 1000 * 10 // 10분으로 지정
                val currTime = System.currentTimeMillis()
                if (currTime < expire) {
                    continue
                }
            }
            if (mSendingHeartIds.indexOf(item.user.id) >= 0) {
                continue
            }
            filtered.add(item.user)
            mSendingHeartIds.add(item.user.id)
        }

        if (filtered.size == 0) {
            Util.Companion.closeProgress()
            return
        }

        MainScope().launch {
            friendsRepository.giveAllHeart(
                1,
                { response ->
                    Util.Companion.closeProgress()
                    if (response.optBoolean("success")) {
                        val currTime = System.currentTimeMillis()
                        val editor = prefs.edit()
                        editor.putLong("send_heart_all", currTime)

                        for (item in filtered) {
                            editor.putLong("send_heart_${item.id}", currTime)
                            mSendingHeartIds.remove(item.id)
                        }
                        editor.apply()

                        // 171010 실제로 보낸 갯수 처리
                        val count = response.optInt("count")
                        val format = getString(R.string.sent_heart_all_friend__format)
                        val msg: String
                        msg = if (count > 0) {
                            String.format(format, count)
                        } else {
                            String.format(format, filtered.size)
                        }
                        Util.Companion.showDefaultIdolDialogWithBtn1(
                            this@FriendsActivity,
                            null,
                            msg,
                            { Util.Companion.closeIdolDialog() },
                            true
                        )

                        friendsAdapter?.notifyDataSetChanged()
                    } else {
                        Util.Companion.showDefaultIdolDialogWithBtn1(
                            this@FriendsActivity, null,
                            response.getString("msg"),
                            { Util.Companion.closeIdolDialog() },
                            true
                        )
                        for (item in filtered) {
                            mSendingHeartIds.remove(item.id)
                        }
                    }
                },
                { throwable ->
                    Util.Companion.closeProgress()
                    for (item in filtered) {
                        mSendingHeartIds.remove(item.id)
                    }

                    Toast.Companion.makeText(
                        this@FriendsActivity,
                        R.string.error_abnormal_exception,
                        Toast.Companion.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun giveHeartToFriend(item: UserModel) {
        val prefs = getSharedPreferences(
            "heart",
            MODE_PRIVATE
        )
        val lastGaveTime = prefs.getLong("send_heart_" + item.id, -1)
        if (lastGaveTime > 0) {
            val expire = lastGaveTime + 60 * 1000 * 10
            val currTime = System.currentTimeMillis()
            if (currTime < expire) {
                val format = getString(R.string.already_sent_heart__format)
                val timeFormat = SimpleDateFormat("m:ss", LocaleUtil.getAppLocale(this))
                val time = timeFormat.format(Date(expire - currTime))

                val text = String.format(format, item.nickname, time)
                Toast.Companion.makeText(this, text, Toast.Companion.LENGTH_SHORT).show()
                return
            }
        }
        if (mSendingHeartIds.indexOf(item.id) >= 0) {
            Toast.Companion.makeText(
                this, getString(R.string.sending_heart),
                Toast.Companion.LENGTH_SHORT
            ).show()
            return
        }
        mSendingHeartIds.add(item.id)

        MainScope().launch {
            friendsRepository.giveHeart(
                item.id.toLong(), 1,
                { response ->
                    if (response.optBoolean("success")) {

                        val currTime = System.currentTimeMillis()
                        prefs.edit().putLong("send_heart_${item.id}", currTime).apply()
                        mSendingHeartIds.remove(item.id)
                        val format = getString(R.string.sent_heart_friend__format)
                        val msg = String.format(format, item.nickname)
                        Toast.Companion.makeText(
                            this@FriendsActivity, msg,
                            Toast.Companion.LENGTH_SHORT
                        ).show()
                        friendsAdapter?.notifyDataSetChanged()
                    } else {
                        Util.Companion.showDefaultIdolDialogWithBtn1(
                            this@FriendsActivity, null,
                            response.getString("msg"),
                            { Util.Companion.closeIdolDialog() },
                            true
                        )

                        mSendingHeartIds.remove(item.id)
                    }
                },
                { throwable ->
                    mSendingHeartIds.remove(item.id)
                    Toast.Companion.makeText(
                        this@FriendsActivity,
                        R.string.error_abnormal_exception, Toast.Companion.LENGTH_SHORT
                    )
                        .show()
                }
            )
        }
    }

    private fun updateTutorial(tutorialIndex: Int) = lifecycleScope.launch {
        usersRepository.updateTutorial(
            tutorialIndex = tutorialIndex,
            listener = { response ->
                if (response.optBoolean("success")) {
                    Logger.Companion.d("Tutorial updated successfully: $tutorialIndex")
                    val bitmask = response.optLong("tutorial", 0L)
                    TutorialManager.init(bitmask)
                }
            },
            errorListener = { throwable ->
                // no-op
            }
        )
    }

    private fun setResponseData() {
        // TransactionTooLargeException 방지
        val extras = ExtendedDataHolder.Companion.getInstance()
        extras.clear()
        if (friendsResponse != null) {
            extras.putExtra("friends", friendsResponse.toString())
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, FriendsActivity::class.java)
        }
    }
}