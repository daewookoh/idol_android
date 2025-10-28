package net.ib.mn.activity

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.util.Base64
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.gson.reflect.TypeToken
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.addon.IdolGson
import net.ib.mn.admanager.AdManager
import net.ib.mn.core.data.repository.BlocksRepository
import net.ib.mn.core.data.repository.ReportRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.article.ArticlesRepositoryImpl
import net.ib.mn.core.data.repository.friends.FriendsRepositoryImpl
import net.ib.mn.core.domain.usecase.GetCouponMessage
import net.ib.mn.core.domain.usecase.LikeArticleUseCase
import net.ib.mn.databinding.ActivityFeedBinding
import net.ib.mn.databinding.DialogDefaultChatReportTwoBtnBinding
import net.ib.mn.databinding.DialogSurpriseHeartBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.RenameConfirmDialogFragment
import net.ib.mn.dialog.RenameDialogFragment
import net.ib.mn.dialog.ReportFeedDialogFragment
import net.ib.mn.dialog.ReportReasonDialogFragment
import net.ib.mn.dialog.VoteDialogFragment
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.fragment.FeedActivityFragment
import net.ib.mn.fragment.FeedCommentFragment
import net.ib.mn.fragment.FeedPhotoFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.FriendModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.ApiCacheManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.IVideoAdListener
import net.ib.mn.utils.Logger
import net.ib.mn.utils.MediaStoreUtils
import net.ib.mn.utils.MessageManager
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.view.ControllableAppBarLayout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

@UnstableApi
@AndroidEntryPoint
class FeedActivity : BaseActivity(),
        AppBarLayout.OnOffsetChangedListener,
        BaseDialogFragment.DialogResultHandler,
        View.OnClickListener {

    //리포트 사유적는 다이얼로그 프래그먼트
    private lateinit var reportReasonDialogFragment:ReportReasonDialogFragment

    @Inject
    lateinit var articlesRepository: ArticlesRepositoryImpl

    @Inject
    lateinit var likeArticleUseCase: LikeArticleUseCase

    @Inject
    lateinit var friendsRepository: FriendsRepositoryImpl

    @Inject
    lateinit var reportRepository: ReportRepositoryImpl
    @Inject
    lateinit var videoAdUtil: VideoAdUtil
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var blocksRepository: BlocksRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    private var mAccount: IdolAccount? = null
    lateinit var vpAdapter: ViewPagerAdapter
    private lateinit var mGlideRequestManager: RequestManager
    private val gson = IdolGson.getInstance()

    private lateinit var mUser: UserModel

//    private var mTempFileForCrop: File? = null
    private var isMine = false
    var isPrivacy = false

    private var menu: Menu? = null
    private var mFriend: FriendModel? = null
    private var didSentFriendRequest = false
    private var didRemovedFriend = false

    var mFeedArticleList = ArrayList<ArticleModel>()
    var feedPhotoList = ArrayList<ArticleModel>()
    var mFeedCommentArticleList = ArrayList<ArticleModel>()

    var isPurchasedDailyPack = false

    //유저 신고기능.
    private var mSheet: BottomSheetFragment? = null

    private var userId : Int = 0

    private var userReport:Boolean = false

    //유저가 현 피드에서 차단했는지 안했는지 체크용
    private var userBlock:Boolean = false

    //차단된 유저 ArrayList
    private var userBlockList = ArrayList<Int>()

    //피드 아래 왼쪽 이미지 리스트 개수
    private var feedPhotoCount = 0
    //피드 아래 오른쪽 게시글 리스트 개수
    private var feedArticleCount = 0

    var feedPhotoOffset = 0

    private var iconSizeCustomed = false

    val clickSubject: PublishSubject<ArticleModel> = PublishSubject.create()
    private val disposable = CompositeDisposable()

    private lateinit var binding: ActivityFeedBinding
    private var isAppBarCollapsed = false // onOffsetChanged에서 타이틀 변경시 onOffsetChanged가 무한호출되는 현상 방지용

    @Inject
    lateinit var getCouponMessageUseCase: GetCouponMessage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_feed)
        binding.coorinatorFeed.applySystemBarInsets()

        mGlideRequestManager = Glide.with(this)
        mAccount = IdolAccount.getAccount(this)
        mUser = intent.extras?.getSerializable(PARAM_USER) as UserModel
        isMine = mAccount?.userModel?.id == mUser.id
        userId = mUser.id


        val ssViewMore = if (Util.isRTL(this))
            getString(R.string.view_more) + " ..."
        else
            "... " + getString(R.string.view_more)

        binding.tvViewMore.text = ssViewMore

        //저장된 유저 차단 리스트 가져옴
        if(Util.getPreference(this, Const.USER_BLOCK_LIST).isNotEmpty()) {
            //차단된 유저 array로 저장
            val listType = object : TypeToken<ArrayList<Int>>() {}.type
            userBlockList = gson.fromJson(Util.getPreference(this@FeedActivity, Const.USER_BLOCK_LIST).toString(), listType)
        }

        //이미 차단된 유저인 경우
        if(userBlockList.contains(userId)){
            userBlock = true
            Logger.v("block user banned")
        }
        //차단 안된 유저인 경우
        else{
            userBlock = false
            Logger.v("block user unbanned")
        }

        binding.eivPhoto.setImageResource(Util.noProfileImage(userId!!))

        setPurchasedDailyPackFlag(mAccount)
        if (!isPurchasedDailyPack && !BuildConfig.CHINA) {
            val adManager = AdManager.getInstance()
            with(adManager) {
                setAdManagerSize(this@FeedActivity, binding.coorinatorFeed)
                setAdManager(this@FeedActivity)
                loadAdManager()
            }
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (isMine) {
            supportActionBar?.setTitle(R.string.feed_my_feed)
        } else {
            supportActionBar?.setTitle(R.string.title_profile)
        }
        binding.appbar.addOnOffsetChangedListener(this)
        if (isMine) {
            setupAccountInfo(mAccount)
        } else {
            MainScope().launch {
                friendsRepository.getFriendInfo(
                    mUser.id,
                    { response ->
                        if (response.optBoolean("success")) {
                            val array = response.optJSONArray("objects")
                            val gson = IdolGson.getInstance()
                            mFriend = gson.fromJson(array.optJSONObject(0).toString(),
                                FriendModel::class.java)
                            invalidateOptionsMenu()
                            setupUserInfo(mFriend?.user!!)
                        } else {
                            setupUserInfo(mUser)
                        }
                    },
                    { throwable ->
                    }
                )
            }
        }
        setupModificationButtons(isMine)
        setupUserStatus(isMine)
        setupCoupon(mAccount)
        setupViewPager(binding.vp, isMine)
        setClickSubject()

//        binding.tlTab.setBackgroundResource(R.drawable.ab_stacked_solid_annie)
        binding.tlTab.addOnTabSelectedListener(object : TabLayout.ViewPagerOnTabSelectedListener(binding.vp) {
            override fun onTabSelected(tab: TabLayout.Tab) {

                binding.vp.currentItem = tab.position
                var fragment = vpAdapter.getItem(tab.position)

                when (tab.position) {
                    0 -> {
                        fragment = fragment as FeedPhotoFragment
                        if (feedPhotoList.size == 0) {
                            if (binding.appbar.state == ControllableAppBarLayout.State.EXPANDED) {
                                fragment.showExpandedEmpty()
                            } else {
                                fragment.showCollpasedEmpty()
                            }
                        }
                        tab.setIcon(onIconResId[0])
                        binding.tlTab.getTabAt(1)?.setIcon(offIconResId[1])
                        binding.tlTab.getTabAt(2)?.setIcon(offIconResId[2])
                    }
                    1 -> {
                        fragment = fragment as FeedActivityFragment
                        if (mFeedArticleList.size == 0) {
                            if (binding.appbar.state == ControllableAppBarLayout.State.EXPANDED) {
                                fragment.showExpandedEmpty()
                            } else {
                                fragment.showCollpasedEmpty()
                            }
                        }
                        binding.tlTab.getTabAt(0)?.setIcon(offIconResId[0])
                        tab.setIcon(onIconResId[1])
                        binding.tlTab.getTabAt(2)?.setIcon(offIconResId[2])
                    }
                    2 -> {
                        fragment = fragment as FeedCommentFragment
                        if (mFeedCommentArticleList.size == 0) {
                            if (binding.appbar.state == ControllableAppBarLayout.State.EXPANDED) {
                                fragment.showExpandedEmpty()
                            } else {
                                fragment.showCollpasedEmpty()
                            }
                        }
                        binding.tlTab.getTabAt(0)?.setIcon(offIconResId[0])
                        binding.tlTab.getTabAt(1)?.setIcon(offIconResId[1])
                        tab.setIcon(onIconResId[2])
                    }
                }
                //아이콘 사이즈 조절된 적 없다면
                if(!iconSizeCustomed) {
                    for (i in 0..2) {
                        binding.tlTab.getTabAt(i)?.setCustomView(R.layout.view_feed_tab)
                    }
                    iconSizeCustomed = true
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    val fragment = vpAdapter.getItem(tab.position)

                    when (tab.position) {
                        0 -> {
                            (fragment as FeedPhotoFragment).binding?.rvFeedPhoto?.
                                smoothScrollToPosition(0)
                        }
                        1 -> {
                            (fragment as FeedActivityFragment).binding?.rvFeedActivity?.
                                smoothScrollToPosition(0)
                        }
                        2 -> {
                            (fragment as FeedCommentFragment).binding?.rvFeedComment?.
                                smoothScrollToPosition(0)
                        }
                    }
                }
            }
        })
        binding.tlTab.setupWithViewPager(binding.vp)
    }

    override fun onResume() {
        if (IdolAccount.sAccount == null) {
            val account = IdolAccount.getAccount(this)
            if (account != null) {
                accountManager.fetchUserInfo(this, {
                    account.fetchFriendsInfo(this, friendsRepository, null)
                    setPurchasedDailyPackFlag(account)
                })
            }
        }
        if(!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.resume()
        }
        super.onResume()
    }

    override fun onPause() {
        if(!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if(!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.destroy()
        }
//        disposable.dispose()
        super.onDestroy()
    }

    override fun onStop() {
        FLAG_CLOSE_DIALOG = false
        super.onStop()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.USER_RENAME_CONFIRM.value -> {
                if (resultCode == BaseDialogFragment.RESULT_OK) {
                    FLAG_CLOSE_DIALOG = false
                    val dialog = RenameDialogFragment.getInstance()
                    dialog.setActivityRequestCode(RequestCode.USER_RENAME.value)
                    dialog.show(supportFragmentManager, "rename_dialog")
                }
            }
            RequestCode.USER_RENAME.value -> {
                if (resultCode == BaseDialogFragment.RESULT_OK) {
                    val account = IdolAccount.getAccount(this) ?: return

                    Util.showProgress(this@FeedActivity)

                    accountManager.fetchUserInfo(this, {
                        binding.tvUsername.text = account.userName
                        onUserInfoUpdated(account)
                    })

                    // messages
                    // 쿠폰등 메시지 관리를 위해 모든 메시지를 받아야와야하나 시간관계상 닉네임 변경 쿠폰 여부만 먼저 체크한다
                    // 향후 아이폰처럼 확장 필요
                    lifecycleScope.launch {
                        MessageManager.shared().getCoupons(this@FeedActivity, getCouponMessageUseCase) {
                            Util.closeProgress()
                        }
                    }
                    updateCoupon()
                }
            }
            RequestCode.ARTICLE_VOTE.value -> {
                if (resultCode == BaseDialogFragment.RESULT_OK) {
                    FLAG_CLOSE_DIALOG = false

                    if (data?.getSerializableExtra(PARAM_ARTICLE) != null) {
                        val heart = data.getLongExtra(VoteDialogFragment.PARAM_HEART, 0)

                        if (heart > 0) {
                            val article = data.getSerializableExtra(PARAM_ARTICLE) as ArticleModel
                            val photoPosition = getPhotoPosition(article.id)
                            val articlePosition = getArticlePosition(article.id)
                            val commentArticlePosition = getCommentArticlePosition(article.id)

                            var isUpdated = false

                            if (photoPosition >= 0) {
                                val model = feedPhotoList[photoPosition]
                                model.heart += heart
                                (vpAdapter.getItem(0) as FeedPhotoFragment)
                                        .mFeedPhotoAdapter?.notifyItemChanged(photoPosition)

                                isUpdated = true
                            }
                            if (articlePosition >= 0) {
                                val model = mFeedArticleList[articlePosition]
                                model.heart += heart
                                (vpAdapter.getItem(1) as FeedActivityFragment)
                                        .mFeedArticleAdapter?.notifyItemChanged(articlePosition)

                                isUpdated = true
                            }
                            if (commentArticlePosition >= 0) {
                                val model = mFeedCommentArticleList[commentArticlePosition]
                                model.heart += heart
                                (vpAdapter.getItem(2) as FeedCommentFragment)
                                        .mFeedArticleAdapter?.notifyItemChanged(commentArticlePosition)

                                isUpdated = true
                            }

                            if (isUpdated) {
                                setResult(FeedActivityFragment.FEED_ARTICLE_MODIFY)

                                val eventHeart = data.getStringExtra(PARAM_EVENT_HEART)
                                if (eventHeart != null) {
                                    showEventDialog(eventHeart)
                                }
                                // 최애에게 투표하면 최애화면에서 프사 순위 변경 적용되게
                                try {
                                    val account = IdolAccount.getAccount(this)
                                    if (account?.userModel != null
                                            && account.userModel!!.most != null
                                            && account.userModel!!.most?.getId() == article.idol?.getId()) {
                                        ApiCacheManager.getInstance().clearCache(Const.KEY_FAVORITE)
                                    }

                                    // 레벨업 체크
                                    UtilK.checkLevelUp(this, accountManager, article.idol, heart)
                                } catch (e: Exception) {
                                }
                            }
                        } else {
                            Util.closeProgress()
                        }
                    }
                }
            }
            RequestCode.USER_REPORT.value -> {
                if (resultCode == BaseDialogFragment.RESULT_OK) {
                    //리포트 사유적는 다이얼로그 실행
                    reportReasonDialogFragment = ReportReasonDialogFragment.getInstance(ReportReasonDialogFragment.FEED_REPORT,-1,mUser)
                    reportReasonDialogFragment.show(supportFragmentManager, "report_reason")
                }
            }
            RequestCode.ENTER_WIDE_PHOTO.value -> {
                if (resultCode == RESULT_CANCELED && !isPurchasedDailyPack && !BuildConfig.CHINA) {
                    AdManager.getInstance().loadAdManager()
                }
            }
            else -> {
                var fragment: Fragment?
                for (i in 0 until 3) {
                    fragment = supportFragmentManager.findFragmentByTag("android:switcher:" + binding.vp.id + ":" + i)

                    if (fragment != null) {
                        when (i) {
                            0 -> {
                                (fragment as FeedPhotoFragment).onDialogResult(requestCode, resultCode, data)
                            }
                            1 -> {
                                (fragment as FeedActivityFragment).onDialogResult(requestCode, resultCode, data)
                            }
                            2 -> {
                                (fragment as FeedCommentFragment).onDialogResult(requestCode, resultCode, data)
                            }
                        }
                    }
                }
            }
        }
        if(resultCode == ResultCode.REPORT_REASON_UPLOADED.value){//리포트 사유 적기 완료 했을때
            userReport = true // 신고했음 으로 값 변경
        }
    }

    fun updateCoupon() {
        accountManager.fetchUserInfo(this, {
            if(isMine){
                menu?.findItem(R.id.action_coupon)?.isVisible =
                    Util.messageParse(applicationContext, mAccount?.userModel?.messageInfo, "C") > 0
            }
            else{
                menu?.findItem(R.id.action_coupon)?.isVisible = false
            }
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        Util.handleVideoAdResult(this, false, true, requestCode, resultCode, data, "feed_videoad", null, IVideoAdListener { adType: String? ->
            videoAdUtil.onVideoSawCommon(this, true, adType, null)
        })

        lifecycleScope.launch {
            MessageManager.shared().getCoupons(this@FeedActivity, getCouponMessageUseCase) {
                Util.closeProgress()
            }
        }
        updateCoupon()

        when (Pair(requestCode, resultCode)) {
            Pair(PHOTO_SELECT_REQUEST, Activity.RESULT_OK) -> {
                val useInternalEditor = Util.getPreferenceBool(this,
                        Const.PREF_USE_INTERNAL_PHOTO_EDITOR,
                        true)
                if (useInternalEditor) {
                    try {
                        val receivedUri: Uri? = data?.data
                        val builder = CropImage.activity(receivedUri)
                                .setAllowFlipping(false)
                                .setAllowRotation(false)
                                .setAllowCounterRotation(false)
                                .setInitialCropWindowPaddingRatio(0f)
                                .setAspectRatio(1, 1)
                        builder.start(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Error Launching Cropper", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    openLegacyImageEditor(data?.data, true)
                }
            }
            Pair(PHOTO_CROP_REQUEST, Activity.RESULT_OK) -> {
                if (mTempFileForCrop != null) {
                    onProfilePhotoSelected(Uri.fromFile(mTempFileForCrop))
                }
            }
            Pair(RequestCode.USER_FAVORITE_SETTING.value, Activity.RESULT_CANCELED) -> {
                Handler().postDelayed({
                    val account = IdolAccount.getAccount(this) ?: return@postDelayed
                    setFavoritesName(account.most)
                    binding.ivLevel.setImageDrawable(Util.getLevelImageDrawable(this, account.userModel!!.level))
                    binding.allLevel.setImageBitmap(Util.getBadgeImage(this, account.userModel!!.itemNo ))
                }, 1000)

            }
            Pair(CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE, Activity.RESULT_OK) -> {
                val result = CropImage.getActivityResult(data)
                val resultUri = result.uri
                onProfilePhotoSelected(resultUri)

            }
            Pair(RequestCode.USER_STATUS_EDIT.value, StatusMessageModificationActivity.RESPONSE_MODIFIED) ->
                 with(binding) {
                    var modifiedStatus = data?.getStringExtra(
                            StatusMessageModificationActivity.PARAM_STATUS_MESSAGE)!!

                    modifiedStatus = Util.BadWordsFilterToHeart(this@FeedActivity, modifiedStatus)


                    if (TextUtils.isEmpty(modifiedStatus)) {
                        if (isMine) {
                            tvStatusMessage.visibility = View.VISIBLE
                            tvStatusMessage.text = ""
                            tvStatusMessage.hint = setStatusModificationIcon(resources.getString(R.string.feed_status_message_hint))
                        } else {
                            tvStatusMessage.visibility = View.GONE
                        }
                    } else {
                        tvStatusMessage.hint = null
                        tvStatusMessage.text = modifiedStatus
                        tvStatusMessage.post {
                            if (tvStatusMessage.layout != null) {
                                //textView layout에서 라인이 1줄이 넘어가면 더보기 보여주기.
                                if(tvStatusMessage.layout.lineCount > 1)
                                {
                                    showViewMore(modifiedStatus)
                                }else{
                                    if (isMine) {
                                        tvStatusMessage.text = setStatusModificationIcon(tvStatusMessage.text.toString())
                                    }
                                }
                            }
                            tvStatusMessage.visibility = View.VISIBLE
                            tvStatusMessage.setLinkURL()
                        }
                    }
                }
            Pair(RequestCode.COUPON_USE.value, ResultCode.COUPON_USED.value) -> {
                accountManager.fetchUserInfo(this, {
                    val account = IdolAccount.getAccount(this@FeedActivity) ?: return@fetchUserInfo
                    binding.tvUsername.text = account.userName
                    onUserInfoUpdated(account)
                })
            }
            else -> {
                var fragment: Fragment?
                for (i in 0 until 3) {
                    fragment = supportFragmentManager.findFragmentByTag("android:switcher:" + binding.vp.id + ":" + i)

                    if (fragment != null) {
                        when (i) {
                            0 -> {
                                (fragment as FeedPhotoFragment).onActivityResult(requestCode, resultCode, data)
                            }
                            1 -> {
                                (fragment as FeedActivityFragment).onActivityResult(requestCode, resultCode, data)
                            }
                            2 -> {
                                (fragment as FeedCommentFragment).onActivityResult(requestCode, resultCode, data)
                            }
                        }
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.feed_friend_menu, menu)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        setOptionsMenu()

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item?.itemId) {
            R.id.action_coupon -> {
                startActivityForResult(MyCouponActivity.createIntent(this), RequestCode.COUPON_USE.value)
                return true
            }
            R.id.action_myheart -> {
                if (Util.mayShowLoginPopup(this)) return true

                setUiActionFirebaseGoogleAnalyticsActivity(
                        Const.ANALYTICS_BUTTON_PRESS_ACTION, "feed_myheart")
                startActivity(MyHeartInfoActivity.createIntent(this))
                return true
            }
            R.id.action_friend -> {
                Util.showDefaultIdolDialogWithBtn1(this@FeedActivity,
                        null,
                        getString(R.string.error_8003),
                        { Util.closeIdolDialog() },
                        true)
                return true
            }
            R.id.action_friend_add -> {
                requestFriend(mUser)
                return true
            }
            R.id.action_friend_wait -> {
                FLAG_CLOSE_DIALOG = false
                Util.showDefaultIdolDialogWithBtn1(this@FeedActivity,
                        null,
                        getString(R.string.error_8002),
                        { Util.closeIdolDialog() },
                        true)
                return true
            }
            R.id.action_report -> {
                mSheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_FEED_REPORT, userBlock)
                val tag = "filter"
                val oldFrag = supportFragmentManager.findFragmentByTag(tag)
                if (oldFrag == null) {
                    mSheet!!.show(supportFragmentManager, tag)
                }
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, offset: Int) {
        if (isMine) {
            return
        }

        val percentage = abs(offset).toFloat() / appBarLayout.totalScrollRange.toFloat()
        val isNowCollapsed = percentage >= 1f

        if (isAppBarCollapsed != isNowCollapsed) {
            isAppBarCollapsed = isNowCollapsed
            if (isAppBarCollapsed) {
                supportActionBar?.title = mUser.nickname
            } else {
                supportActionBar?.setTitle(R.string.title_profile)
            }
        }
    }

    override fun onClick(v: View?) { with(binding) {
        when (v?.id) {
            binding.eivPhoto.id,
            ivPhotoModification.id -> {
                if (Util.mayShowLoginPopup(this@FeedActivity)) return
                onProfilePhotoClick()
            }
            tvUsername.id,
            ivUsernameModification.id -> {
                if (Util.mayShowLoginPopup(this@FeedActivity)) return

                // 닉네임 변경 쿠폰 처리
                // 쿠폰이 사용자가 접속한 후에 발급이 되므로 쿠폰을 다시 조회함
                Util.showProgress(this@FeedActivity)
                lifecycleScope.launch {
                    MessageManager.shared().getCoupons(this@FeedActivity, getCouponMessageUseCase) {
                        Util.closeProgress()
                        if( MessageManager.shared().hasNicknameCoupon() ) { //닉네임 쿠폰이 있을 경우
                            Util.showDefaultIdolDialogWithBtn2(this@FeedActivity, null, getString(R.string.msg_nickname_coupon),
                                R.string.yes, R.string.no, true, false,
                                {
                                    Util.closeIdolDialog()
                                    // 쿠폰 사용
                                    startActivityForResult(MyCouponActivity.createIntent(this@FeedActivity), RequestCode.COUPON_USE.value)
                                },
                                {
                                    Util.closeIdolDialog()
                                })
                        } else {    //닉네임 쿠폰이 없을 경우
                            val dialog = RenameConfirmDialogFragment.instance
                            dialog.setActivityRequestCode(RequestCode.USER_RENAME_CONFIRM.value)
                            dialog.show(supportFragmentManager, "rename_confirm")
                        }
                    }
                }
            }
            tvIdolName.id,
            ivIdolModification.id -> {
                if (Util.mayShowLoginPopup(this@FeedActivity)) return
                startActivityForResult(
                        FavoriteSettingActivity.createIntent(this@FeedActivity),
                        RequestCode.USER_FAVORITE_SETTING.value)
            }
            tvStatusMessage.id,
            clStatusMessage.id -> {
                startActivityForResult(StatusMessageModificationActivity.createIntent(this@FeedActivity,
                        tvStatusMessage.text.toString()), RequestCode.USER_STATUS_EDIT.value)
            }

            else -> {}
        }}
    }

    private fun setupAccountInfo(account: IdolAccount?) {
        val context: Context = this

        accountManager.fetchUserInfo(this, {
            setupUserInfo(IdolAccount.getAccount(context)?.userModel)
        })
    }

    private fun setupUserInfo(user: UserModel?) { with(binding) {
        val user = user ?: return

        tvUsername.text = mUser.nickname

        if (user.heart == Const.LEVEL_MANAGER) {
            tvIdolName.visibility = View.GONE
        } else {
            tvIdolName.visibility = View.VISIBLE
            setFavoritesName(user.most)
        }
        val userId = user.id
        mGlideRequestManager.load(user.imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(userId))
                .fallback(Util.noProfileImage(userId))
                .placeholder(Util.noProfileImage(userId))
                .into(binding.eivPhoto)


        //관리자 계정일때는  ->  레벨 말고 레벨위치에 관리자 뱃지만 보여야 되므로, 기존 getLevelImage사용
        //그리고 관리자가 친구일때  friends model user에  heart가 0으로 와서 문제가 있어  mUser를 사용함.
        if(mUser.heart == Const.LEVEL_MANAGER){
            ivLevel.setImageBitmap(Util.getLevelImage(this@FeedActivity, mUser))
        }else{
            ivLevel.setImageDrawable(Util.getLevelImageDrawable(this@FeedActivity, mUser.level))
            allLevel.setImageBitmap(Util.getBadgeImage(this@FeedActivity, mUser.itemNo))
        }
    }}

    private fun setOptionsMenu() {

        if (isMine) {
            menu?.findItem(R.id.action_coupon)?.isVisible =
                    Util.messageParse(this, mAccount?.userModel?.messageInfo, "C") > 0
            menu?.findItem(R.id.action_myheart)?.isVisible = true
            menu?.findItem(R.id.action_friend)?.isVisible = false
            menu?.findItem(R.id.action_friend_add)?.isVisible = false
            menu?.findItem(R.id.action_friend_wait)?.isVisible = false
            menu?.findItem(R.id.action_report)?.isVisible = false
        } else {
            menu?.findItem(R.id.action_coupon)?.isVisible = false
            menu?.findItem(R.id.action_myheart)?.isVisible = false
            menu?.findItem(R.id.action_report)?.isVisible = true
            if (mFriend != null) {
                when {
                    didSentFriendRequest -> {
                        menu?.findItem(R.id.action_friend)?.isVisible = false
                        menu?.findItem(R.id.action_friend_add)?.isVisible = false
                        menu?.findItem(R.id.action_friend_wait)?.isVisible = true
                    }
                    didRemovedFriend -> {
                        menu?.findItem(R.id.action_friend)?.isVisible = false
                        menu?.findItem(R.id.action_friend_add)?.isVisible = true
                        menu?.findItem(R.id.action_friend_wait)?.isVisible = false
                    }
                    mFriend!!.isFriend.equals("Y", true) -> {
                        menu?.findItem(R.id.action_friend)?.isVisible = true
                        menu?.findItem(R.id.action_friend_add)?.isVisible = false
                        menu?.findItem(R.id.action_friend_wait)?.isVisible = false
                    }
                    mFriend!!.userType.equals(FriendModel.RECV_USER, true) -> {
                        menu?.findItem(R.id.action_friend)?.isVisible = false
                        menu?.findItem(R.id.action_friend_add)?.isVisible = false
                        menu?.findItem(R.id.action_friend_wait)?.isVisible = true
                    }
                    else -> {
                        menu?.findItem(R.id.action_friend)?.isVisible = false
                        menu?.findItem(R.id.action_friend_add)?.isVisible = true
                        menu?.findItem(R.id.action_friend_wait)?.isVisible = false
                    }
                }
            } else {
                if (didSentFriendRequest) {
                    menu?.findItem(R.id.action_friend)?.isVisible = false
                    menu?.findItem(R.id.action_friend_add)?.isVisible = false
                    menu?.findItem(R.id.action_friend_wait)?.isVisible = true
                } else {
                    menu?.findItem(R.id.action_friend)?.isVisible = false
                    menu?.findItem(R.id.action_friend_add)?.isVisible = true
                    menu?.findItem(R.id.action_friend_wait)?.isVisible = false
                }
            }
        }

        //해당 값이 true이면 -> 나의 정보 화면에서 온것이므로,  나의 정보화면 가는 버튼을 invisible 처리 한다.
        if(intent.getBooleanExtra(Const.MY_INFO_TO_FEED_PREFERENCE_KEY,false)){
            menu?.findItem(R.id.action_myheart)?.isVisible = false
        }
    }

    private fun setStatusModificationIcon(statusMessage: String): SpannableString {
        val icon = resources.getDrawable(R.drawable.icon_community_setting)
        icon.setBounds(0,
                0,
                Util.convertDpToPixel(this, 10f).toInt(),
                Util.convertDpToPixel(this, 12f).toInt())
        val imageSpan = ImageSpan(icon, ImageSpan.ALIGN_BASELINE)

        return SpannableString("$statusMessage  ").apply {
            setSpan(imageSpan, statusMessage.length + 1, statusMessage.length + 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    private fun setupUserStatus(isMine: Boolean) {
        lifecycleScope.launch {
            usersRepository.getStatus(
                userId = mUser.id,
                listener = { response ->
                    with(binding) {
                        if (response.optBoolean("success")) {
                            //getFriendInfo api false로 올떄가 있어서 여기서 item_no 셋해줌(몰빵일,보안관,메신저...)
                            mUser.itemNo = response.getInt("item_no")
                            if(!isMine)
                                setupUserInfo(mUser)

                            var statusMessage = response.optString("status_message")
                            statusMessage = Util.BadWordsFilterToHeart(this@FeedActivity, statusMessage)

                            if (response.isNull("status_message") || TextUtils.isEmpty(statusMessage)) {
                                if (isMine) {
                                    tvStatusMessage.visibility = View.VISIBLE
                                    tvStatusMessage.text = ""
                                    tvStatusMessage.hint = setStatusModificationIcon(resources.getString(R.string.feed_status_message_hint))
                                } else {
                                    tvStatusMessage.visibility = View.GONE
                                }
                            } else {
                                tvStatusMessage.text = statusMessage
                                tvStatusMessage.post {
                                    if (tvStatusMessage.layout != null) {
                                        if (tvStatusMessage.layout.lineCount > 1) {
                                            showViewMore(statusMessage)
                                        } else {
                                            if (isMine) {
                                                tvStatusMessage.text = setStatusModificationIcon(tvStatusMessage.text.toString())
                                            }
                                        }
                                    }
                                    tvStatusMessage.visibility = View.VISIBLE
                                    tvStatusMessage.setLinkURL()
                                }
                            }

                            isPrivacy = response.optString("feed_is_viewable", "Y") == "N"

                            onIconResId[0] = R.drawable.icon_feed_photo_on
                            offIconResId[0] = R.drawable.icon_feed_photo_off

                            if (isPrivacy) {
                                if (isMine) {
                                    getFeedPhoto(mUser, 0, SIZE_OF_PHOTO_LIMIT)
                                    getFeedArticle(mUser, 0, SIZE_OF_ARTICLE_LIMIT)
                                    getFeedComment(mUser, 0, SIZE_OF_ARTICLE_LIMIT)
                                    onIconResId[0] = R.drawable.icon_my_feed_photolock_on
                                    offIconResId[0] = R.drawable.icon_my_feed_photolock_off
                                } else {
                                    setPrivacy(userId)
                                }
                            } else {
                                if (isMine) {
                                    getFeedComment(mUser, 0, SIZE_OF_ARTICLE_LIMIT)
                                }
                                getFeedPhoto(mUser, 0, SIZE_OF_PHOTO_LIMIT)
                                getFeedArticle(mUser, 0, SIZE_OF_ARTICLE_LIMIT)
                            }
                            binding.tlTab.getTabAt(0)?.setIcon(onIconResId[0])
                        } else {
                            if (isMine) {
                                tvStatusMessage.visibility = View.VISIBLE
                                tvStatusMessage.text = ""
                            } else {
                                tvStatusMessage.visibility = View.GONE
                            }
                        }
                    }
                },
                errorListener = {
                    binding.tvStatusMessage.visibility = View.GONE
                }
            )
        }
    }

    private fun setupCoupon(account: IdolAccount?) {
        val account = account ?: return
        val couponCount = Util.messageParse(this, account.userModel?.messageInfo, "C")
        if (couponCount > Util.getPreferenceInt(this, "message_coupon_count", -1)) {
            Util.setPreference(this, "message_new", true)
        }
    }

    private fun showViewMore(statusMessage: String) { with(binding) {
        val layout = tvStatusMessage.layout
        if (layout.lineCount > 0 && layout.getEllipsisCount(layout.lineCount - 1) > 0) {
            tvStatusMessage.ellipsize = null
        }

        try{
            val textOfFirstLine = statusMessage.substring(0, layout.getLineEnd(0)).replace("\n", "")
            tvStatusMessage.text = textOfFirstLine
            //다른화면으로 갔다오면 maxLines가 고정이 안될때가 있음. 코드상에서도 1개로 고정해주기.
            tvStatusMessage.maxLines = 1
        }catch (e:IndexOutOfBoundsException){
            e.printStackTrace()
        }

        setViewMore(statusMessage)
        tvStatusMessage.setOnClickListener {
            tvViewMore.visibility = View.INVISIBLE
            tvStatusMessage.text = statusMessage
            tvStatusMessage.maxLines = 1000

            val animation = ObjectAnimator.ofInt(tvStatusMessage, "maxLines", 1000)
            animation.setDuration(500).start()
//            setScrimVisibleHeightTrigger()

            if (isMine) {
                tvStatusMessage.setOnClickListener(this@FeedActivity)
                tvStatusMessage.text = setStatusModificationIcon(tvStatusMessage.text.toString())
            } else {
                tvStatusMessage.setLinkURL()
            }
        }
    }}

    private fun setViewMore(statusMessage: String) { with(binding) {
        val ssViewMore = if (Util.isRTL(this@FeedActivity))
            getString(R.string.view_more) + " ..."
        else
            "... " + getString(R.string.view_more)

        tvViewMore.text = ssViewMore
        tvViewMore.visibility = View.VISIBLE
        tvViewMore.setOnClickListener {
            tvViewMore.visibility = View.INVISIBLE
            tvStatusMessage.text = statusMessage
            tvStatusMessage.maxLines = 1000

            val animation = ObjectAnimator.ofInt(tvStatusMessage, "maxLines", 1000)
            animation.setDuration(500).start()

            if (isMine) {
                tvStatusMessage.text = setStatusModificationIcon(tvStatusMessage.text.toString())
                tvStatusMessage.setOnClickListener(this@FeedActivity)
                clStatusMessage.setOnClickListener(this@FeedActivity)
            } else {
                tvStatusMessage.setLinkURL()
            }
        }
    }}

    private fun setupViewPager(viewPager: ViewPager, isMine: Boolean) {
        vpAdapter = ViewPagerAdapter(supportFragmentManager)
        // fragment들이 아직 살아있으면
        for ( i in 0..2 ) {
            val f = supportFragmentManager.findFragmentByTag("android:switcher:" + binding.vp.id + ":" + i)
                ?: continue
            when (i) {
                0 -> { vpAdapter.addFrag(f, CATEGORY_PHOTO) }
                1 -> { vpAdapter.addFrag(f, CATEGORY_ACTIVITY) }
                2 -> { vpAdapter.addFrag(f, CATEGORY_COMMENT) }
            }
        }

        if( vpAdapter.count == 0 ) {
            vpAdapter.addFrag(FeedPhotoFragment.getInstance(), CATEGORY_PHOTO)
            vpAdapter.addFrag(FeedActivityFragment.getInstance(), CATEGORY_ACTIVITY)
            if (isMine) {
                vpAdapter.addFrag(FeedCommentFragment.getInstance(mUser), CATEGORY_COMMENT)
            }
        }

        viewPager.adapter = vpAdapter
        viewPager.offscreenPageLimit = 2
    }

    private fun setupModificationButtons(isMine: Boolean) { with(binding) {
        if (isMine) {
            ivPhotoModification.visibility = View.VISIBLE
            ivUsernameModification.visibility = View.VISIBLE
            ivIdolModification.visibility = View.VISIBLE

            binding.eivPhoto.setOnClickListener(this@FeedActivity)
            ivPhotoModification.setOnClickListener(this@FeedActivity)
            tvUsername.setOnClickListener(this@FeedActivity)
            ivUsernameModification.setOnClickListener(this@FeedActivity)
            tvIdolName.setOnClickListener(this@FeedActivity)
            ivIdolModification.setOnClickListener(this@FeedActivity)
            tvStatusMessage.setOnClickListener(this@FeedActivity)
        } else {
            ivPhotoModification.visibility = View.GONE
            ivUsernameModification.visibility = View.GONE
            ivIdolModification.visibility = View.GONE

            binding.eivPhoto.setOnClickListener(null)
            ivPhotoModification.setOnClickListener(null)
            tvUsername.setOnClickListener(null)
            ivUsernameModification.setOnClickListener(null)
            tvIdolName.setOnClickListener(null)
            ivIdolModification.setOnClickListener(null)
            tvStatusMessage.setOnClickListener(null)
        }
    }}

    private fun onProfilePhotoClick() {
        val context = this

        val photoPickIntent = MediaStoreUtils.getPickImageIntent(this)

        val packageManager = packageManager
        if (photoPickIntent.resolveActivity(packageManager) !=
                null) {
            startActivityForResult(photoPickIntent, PHOTO_SELECT_REQUEST)
        } else {
            Util.showDefaultIdolDialogWithBtn1(this,
                    null,
                    getString(R.string.cropper_not_found),
                    { Util.closeIdolDialog() },
                    true)
        }

        // 내부편집기 검사를 여기서 하면 안되지...

//        val useInternalEditor = Util.getPreferenceBool(context,
//                Const.PREF_USE_INTERNAL_PHOTO_EDITOR,
//                true)
//        if (useInternalEditor) {
//            try {
//                CropImage.activity().setAspectRatio(1, 1).start(this)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Toast.makeText(context, "Error Launching Cropper", Toast.LENGTH_SHORT).show()
//            }
//
//        } else {
//            // 일부 삼성 폰에서 기본앱 설정이 안되는 현상으로 아래와 같이 바꿔봄...
//            //            Intent photoPickIntent = new Intent(Intent.ACTION_PICK,
//            //                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//            //            photoPickIntent.setType("image/*");
//            val photoPickIntent = MediaStoreUtils.getPickImageIntent(this)
//
//            val packageManager = packageManager
//            if (photoPickIntent.resolveActivity(packageManager) !=
//                    null) {
//                startActivityForResult(photoPickIntent, REQ_PHOTO_SELECT)
//            } else {
//                Util.showDefaultIdolDialogWithBtn1(this,
//                        null,
//                        getString(R.string.cropper_not_found),
//                        { Util.closeIdolDialog() },
//                        true)
//            }
//        }
    }

    private fun showEventDialog(event_heart: String) {
        val binding = DialogSurpriseHeartBinding.inflate(layoutInflater)
        val eventHeartDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        eventHeartDialog.window!!.attributes = lpWindow
        eventHeartDialog.window!!.setLayout(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT)

        eventHeartDialog.setContentView(binding.root)
        eventHeartDialog.setCanceledOnTouchOutside(false)
        eventHeartDialog.setCancelable(true)
        binding.btnOk.setOnClickListener { eventHeartDialog.cancel() }
        val surpriseMsg = String.format(getString(R.string.msg_surprise_heart), event_heart)
        binding.message.text = surpriseMsg
        eventHeartDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        eventHeartDialog.show()
    }

//    private fun cropProfilePhoto(uri: Uri?) {
//        val cropIntent = Intent("com.android.camera.action.CROP")
//        cropIntent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//        cropIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//        cropIntent.setDataAndType(uri, "image/*")
//        cropIntent.putExtra("crop", "true")
//        cropIntent.putExtra("aspectX", 1)
//        cropIntent.putExtra("aspectY", 1)
//        cropIntent.putExtra("scale", "true")
//        cropIntent.putExtra("output", createTempFile())
//        cropIntent.putExtra("outputFormat", "PNG")
//        val packageManager = packageManager
//        if (cropIntent.resolveActivity(packageManager) !=
//                null) {
//            try {
//                startActivityForResult(cropIntent, REQ_PHOTO_CROP)
//            } catch (e: Exception) {
//                Util.showDefaultIdolDialogWithBtn1(this,
//                        null,
//                        getString(R.string.msg_use_internal_editor),
//                        { Util.closeIdolDialog() },
//                        true)
//            }
//
//        } else {
//            Util.showDefaultIdolDialogWithBtn1(this, null,
//                    getString(R.string.cropper_not_found),
//                    { Util.closeIdolDialog() },
//                    true)
//        }
//    }

    private fun onProfilePhotoSelected(uri: Uri) {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        var bitmap: Bitmap? = BitmapFactory.decodeFile(uri.path, options)

        // 일부 단말에서 decode에 실패하는 경우가 있음...
        if (bitmap == null) {
            try {
                Util.log("decodeFile " + uri.path + " failed. try another method...")
                val location = File(uri.path!!)
                val fis: FileInputStream
                fis = FileInputStream(location)
                bitmap = BitmapFactory.decodeStream(fis)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

        }

        if (bitmap == null) {
            Toast.makeText(this, R.string.error_abnormal_default, Toast.LENGTH_SHORT).show()
            return
        }

        val resized = Bitmap.createScaledBitmap(bitmap, PHOTO_RESIZE_WIDTH,
                PHOTO_RESIZE_HEIGHT, true)

        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val data = Base64.encodeToString(stream.toByteArray(),
                Base64.DEFAULT)

        Util.log("onProfilePhotoSelected size=" + data.length)
        val context = this

        lifecycleScope.launch {
            usersRepository.setProfileImage(
                data,
                listener = { response ->
                    if (response.optBoolean("success")) {
                        val account = IdolAccount
                            .getAccount(context)

                        Glide.get(context).clearMemory()
                        CoroutineScope(Dispatchers.IO).launch {
                            Glide.get(context).clearDiskCache()
                        }

                        accountManager.fetchUserInfo(context, {
                                // 프로필 이미지 파일명이 매번 바뀌도록 했으므로 캐시 삭제는 제거
                                // 그래도 이전것이 나온다고 해서 다시 캐시 삭제.
                                clearProfileImageCache(context)
                                mGlideRequestManager
                                    .load(account?.profileUrl)
                                    .apply(RequestOptions.circleCropTransform())
                                    .error(Util.noProfileImage(userId))
                                    .fallback(Util.noProfileImage(userId))
                                    .placeholder(Util.noProfileImage(userId))
                                    .into(binding.eivPhoto)
                            }
                        )
                    } else {
                        UtilK.handleCommonError(context, response)
                    }
                },
                errorListener = { throwable ->
                    Toast.makeText(context,
                        R.string.error_abnormal_exception, Toast.LENGTH_SHORT)
                        .show()
                }
            )
        }
    }

    private fun clearProfileImageCache(context: Context) {
        mGlideRequestManager.clear(binding.eivPhoto)
        Glide.get(context).clearMemory()
        CoroutineScope(Dispatchers.IO).launch {
            Glide.get(context).clearDiskCache()
        }
    }

    private fun requestFriend(user: UserModel) {
        // 친구 제한 꽉차면 꽉 찼다는 알림 주기
        if (Util.getPreferenceBool(this, Const.PREF_FRIENDS_LIMIT, false)) {
            Util.closeProgress()
            FLAG_CLOSE_DIALOG = false
            Util.showDefaultIdolDialogWithBtn1(
                    this,
                    null,
                    getString(R.string.error_8000),
                    { Util.closeIdolDialog() },
                    true)
        } else {
            MainScope().launch {
                friendsRepository.sendFriendRequest(
                    user.id.toLong(),
                    { response ->
                        if (response?.optBoolean("success")!!) {
                            Util.closeProgress()
                            didSentFriendRequest = true
                            invalidateOptionsMenu()

                            FLAG_CLOSE_DIALOG = false
                            Util.showDefaultIdolDialogWithBtn1(this@FeedActivity,
                                null,
                                getString(R.string.friend_request_sent),
                                { Util.closeIdolDialog() },
                                true)
                        } else {
                            Util.closeProgress()

                            FLAG_CLOSE_DIALOG = false
                            val errMsg = ErrorControl.parseError(this@FeedActivity, response)
                            if (errMsg != null) {
                                Util.showDefaultIdolDialogWithBtn1(this@FeedActivity,
                                    null,
                                    errMsg,
                                    { Util.closeIdolDialog() },
                                    true)
                            }
                        }
                    },
                    { throwable ->
                        Util.closeProgress()

                        FLAG_CLOSE_DIALOG = false
                        Util.showDefaultIdolDialogWithBtn1(this@FeedActivity,
                            null,
                            throwable.message,
                            { Util.closeIdolDialog() },
                            true)
                    }
                )
            }
        }
    }

    private fun setPurchasedDailyPackFlag(account: IdolAccount?) {
        if (account?.userModel != null
                && account.userModel!!.subscriptions != null
                && account.userModel!!.subscriptions.isNotEmpty()) {
            for (mySubscription in account.userModel!!.subscriptions) {
                if (mySubscription.familyappId == 1 || mySubscription.familyappId == 2) {
                    if (mySubscription.skuCode == Const.STORE_ITEM_DAILY_PACK) {
                        isPurchasedDailyPack = true
                        break
                    }
                }
            }
        } else {
            isPurchasedDailyPack = false
        }
    }

    private fun setFavoritesName(most: IdolModel?) {

        if (most != null && most.type != null) {
//            most.setLocalizedName(this)

            val text: SpannableString
            if (most.type.contains("G")) {
                text = SpannableString(most.getName(this))
            } else {
                if (most.getName(this).contains("_")) {
                    val mostSoloName = Util.nameSplit(this, most)[0]
                    val mostGroupName = Util.nameSplit(this, most)[1]
                    if(Util.isRTL(this)){
                        text = SpannableString(mostGroupName
                                + " "
                                + mostSoloName)
                        text.setSpan(AbsoluteSizeSpan(Util.convertDpToPixel(this, 10f).toInt()),
                                0,
                                mostGroupName.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        text.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.gray300)),
                                0,
                                mostGroupName.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        binding.tvIdolName.visibility = View.VISIBLE
                    }
                    else {
                        text = SpannableString(mostSoloName
                                + " "
                                + mostGroupName)
                        text.setSpan(AbsoluteSizeSpan(Util.convertDpToPixel(this, 10f).toInt()),
                                mostSoloName.length + 1,
                                text.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        text.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.gray300)),
                                mostSoloName.length + 1,
                                text.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        binding.tvIdolName.visibility = View.VISIBLE
                    }
                } else {
                    text = SpannableString(most.getName(this))
                }
            }
            binding.tvIdolName.text = text
        } else {
            binding.tvIdolName.text = if( BuildConfig.CELEB ) getString(R.string.actor_empty_most) else getString(R.string.empty_most)
        }
    }

    private fun getPhotoPosition(articleId: String): Int {
        val position = feedPhotoList.withIndex().find { it.value.id == articleId }?.index
        if (position != null) return position
        return -1
    }

    private fun getArticlePosition(articleId: String): Int {
        val position = mFeedArticleList.withIndex().find { it.value.id == articleId }?.index
        if (position != null) return position
        return -1
    }

    private fun getCommentArticlePosition(articleId: String): Int {
        val position = mFeedCommentArticleList.withIndex().find { it.value.id == articleId }?.index
        if (position != null) return position
        return -1
    }

    fun loadMoreArticles() {
        val size = mFeedArticleList.size
        if (size % SIZE_OF_ARTICLE_LIMIT == 0) {
            getFeedArticle(mUser, size, SIZE_OF_ARTICLE_LIMIT)
        }
    }

    fun loadMorePhotos() {
        getFeedPhoto(mUser, feedPhotoOffset, SIZE_OF_PHOTO_LIMIT)
    }

    fun getFeedArticle(user: UserModel, offset: Int, limit: Int) {
        MainScope().launch {
            articlesRepository.getFeedActivity(
                user.id.toLong(),
                PARAM_ARTICLE,
                offset,
                limit,
                isMine,
                { response ->
                    if (response.optBoolean("success")) {
                        try {
                            val array = response.getJSONArray("objects")
                            val articleCount = array.length()
                            feedArticleCount = articleCount
                            if (articleCount > 0) {
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    val model = IdolGson.getInstance(true)
                                        .fromJson(obj.toString(), ArticleModel::class.java)

                                    if(UtilK.isArticleNotReported(this@FeedActivity, model.id)){
                                        mFeedArticleList.add(model)
                                    }
                                }

                                setActivityContents(articleCount)
                            } else {
                                if (mFeedArticleList.size == 0) {
                                    val fragment = vpAdapter.getItem(1) as FeedActivityFragment
                                    fragment.showEmpty(userId)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                { throwable ->
                    Toast.makeText(this@FeedActivity, R.string.msg_error_ok, Toast.LENGTH_SHORT).show()
                })
        }
    }

    fun getFeedComment(user: UserModel, offset: Int, limit: Int) {
        // comment는 본인만 보이기 때문에 true로 넘김
        MainScope().launch {
            articlesRepository.getFeedActivity(
                user.id.toLong(),
                PARAM_COMMENT,
                offset,
                limit,
                true,
                { response ->
                    val fragment = vpAdapter.getItem(2) as FeedCommentFragment

                    if (response?.optBoolean("success")!!) {
                        try {
                            val array = response.getJSONArray("objects")

                            if (array.length() > 0) {
                                fragment.hideEmpty()
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    val model = IdolGson.getInstance(true)
                                            .fromJson(obj.toString(), ArticleModel::class.java)

                                    if(UtilK.isArticleNotReported(this@FeedActivity, model.id)){
                                        mFeedCommentArticleList.add(model)
                                    }
                                }

                                fragment.mFeedArticleAdapter?.notifyItemRangeInserted(offset, limit)
                            } else if (mFeedCommentArticleList.size == 0) {
                                fragment.showEmpty()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            fragment.showEmpty()
                        }
                    } else {
                        fragment.showEmpty()
                    }
                },
                { throwable ->
                    val fragment = vpAdapter.getItem(2) as FeedCommentFragment
                    fragment.showEmpty()
                })
        }
    }

    fun getFeedPhoto(user: UserModel, offset: Int, limit: Int) {
        MainScope().launch {
            articlesRepository.getFeedActivity(
                user.id.toLong(),
                PARAM_PHOTO,
                offset,
                limit,
                isMine,
                { response ->
                    if (response?.optBoolean("success")!!) {
                        try {
                            val array = response.getJSONArray("objects")
                            val articleCount = array.length()
                            feedPhotoCount = articleCount
                            if (articleCount > 0) {
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    val model = IdolGson.getInstance(true)
                                        .fromJson(obj.toString(), ArticleModel::class.java)

                                    if (UtilK.isArticleNotReported(
                                            this@FeedActivity,
                                            model.id
                                        ) && (model.imageUrl != null || !model.files.isNullOrEmpty())
                                    ) {
                                        feedPhotoList.add(model)
                                        setPhotoContents(articleCount)
                                    }
                                }

                                feedPhotoOffset += SIZE_OF_PHOTO_LIMIT
                            } else {
                                if (feedPhotoList.size == 0) {
                                    val fragment = vpAdapter.getItem(0) as FeedPhotoFragment
                                    fragment.showEmpty(userId)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                { throwable ->
                    Toast.makeText(this@FeedActivity, R.string.msg_error_ok, Toast.LENGTH_SHORT).show()
                })
        }
    }

    //비공개일 때
    fun setPrivacy(userId : Int) {
        val photoFragment = vpAdapter.getItem(0) as FeedPhotoFragment
        val activityFragment = vpAdapter.getItem(1) as FeedActivityFragment

        photoFragment.showPrivacy(userId)
        activityFragment.showPrivacy(userId)
    }

    //게시물 왼쪽
    fun setPhotoContents(count: Int) {
        val photoFragment = vpAdapter.getItem(0) as FeedPhotoFragment
        val activityFragment = vpAdapter.getItem(1) as FeedActivityFragment
        if (feedPhotoList.size == 0) {
            photoFragment.showEmpty(userId)
            return
        }
        if (count > 0) {
            val adapter = photoFragment.mFeedPhotoAdapter
            adapter?.notifyItemRangeInserted(feedPhotoList.size, count)
            activityFragment.mFeedScrollListener?.setLoading()
        }

        // 페이징 된 아티클 리스트중에서 사진 게시글이 없고 , 일반 글 게시물만 있다면 다음 페이징을 가져와 준다.
        if (count == 0 && feedArticleCount == SIZE_OF_ARTICLE_LIMIT) {
            loadMoreArticles()
        }
        photoFragment.hideEmpty(userId)
    }

    //게시물 오른쪽
    fun setActivityContents(count: Int) {
        val photoFragment = vpAdapter.getItem(0) as FeedPhotoFragment
        val activityFragment = vpAdapter.getItem(1) as FeedActivityFragment
        if (count > 0) {
            val adapter = activityFragment.mFeedArticleAdapter
            adapter?.notifyItemRangeInserted(mFeedArticleList.size, count)
            photoFragment.mFeedScrollListener?.setLoading()
            activityFragment.hideEmpty(userId)
        } else if (mFeedArticleList.size == 0) {
            activityFragment.showEmpty(userId)
        }
    }

    inner class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        val mFragmentList = ArrayList<Fragment>()
        val mFragmentTitleList = ArrayList<String>()

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFrag(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }
    }

     fun reportUser(){
         val account = IdolAccount.getAccount(this)
         if (account == null && Util.mayShowLoginPopup(this)) {
             return
         }
        setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "feed_report")

//         val prefs = PreferenceManager.getDefaultSharedPreferences(this)
//         val reportedUser = prefs.getStringSet(
//                 account.email + "_did_user_report", HashSet())
//         if (reportedUser!!.contains(mUser.resourceUri)) {
//             Toast.makeText(this,
//                     R.string.failed_to_report_user__already_reported,
//                     Toast.LENGTH_SHORT).show()
//             return
//         }

         val reportHeart = ConfigModel.getInstance(this).reportHeart
         val report = ReportFeedDialogFragment.getInstance(mUser)

         //하트차감 수가 0일떄.
         if (reportHeart == 0) {
             report.setMessage(HtmlCompat.fromHtml(resources.getString(R.string.report_user_desc),
                     HtmlCompat.FROM_HTML_MODE_LEGACY))

         }else{
             if(reportHeart > 0){
                 val color = "#" + Integer.toHexString(
                         ContextCompat.getColor(this@FeedActivity,
                                 R.color.main)).substring(2)
                 val msg = String.format(
                         resources.getString(R.string.msg_report_user_confirm),
                         "<FONT color=$color>$reportHeart</FONT>")
                 val spanned = HtmlCompat.fromHtml(msg,
                         HtmlCompat.FROM_HTML_MODE_LEGACY)
                 report.setMessage(spanned)
             }
         }
         report.setActivityRequestCode(RequestCode.USER_REPORT.value)
         report.show(supportFragmentManager, "feed_report")
    }

    private fun userReport() {
        val binding = DialogDefaultChatReportTwoBtnBinding.inflate(layoutInflater)
        val reportDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        reportDialog.window!!.attributes = lpWindow
        reportDialog.window!!.setLayout(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT)

        reportDialog.setContentView(binding.root)
        reportDialog.setCanceledOnTouchOutside(false)
        reportDialog.setCancelable(true)

        reportDialog.show()

        binding.btnConfirm.setOnClickListener {
            if (binding.chatReportContent.length() < 10) {
                Toast.makeText(this,
                        String.format(getString(R.string.comment_minimum_characters), 10), Toast.LENGTH_SHORT).show()
            } else {
                reportDialog.dismiss()
                val content = binding.chatReportContent.text.toString()

                lifecycleScope.launch {
                    reportRepository.doReportFeed(
                        mUser.id.toLong(),
                        content,
                        { response ->
                            Logger.v("신고 : " + response)
                            if (response.getBoolean("success")) {
                                //유저신고는 데이터를 넣어줄거 없으니까 RESULT_OK만 set해준다.
                                Toast.makeText(
                                    this@FeedActivity,
                                    R.string.report_done,
                                    Toast.LENGTH_SHORT
                                ).show()
                                reportDialog.dismiss()

                                val account = IdolAccount.getAccount(this@FeedActivity)
                                if (account != null) {
                                    val prefs = PreferenceManager
                                        .getDefaultSharedPreferences(this@FeedActivity)
                                    val editor = prefs.edit()
                                    val reportedUser = prefs.getStringSet(
                                        account.email + "_did_user_report",
                                        HashSet()
                                    )
                                    reportedUser?.add(mUser.resourceUri)
                                    editor.putStringSet(
                                        account.email + "_did_user_report",
                                        reportedUser
                                    ).apply()
                                }
                            } else {
                                val responseMsg =
                                    ErrorControl.parseError(this@FeedActivity, response)
                                if (responseMsg != null) {
                                    Util.showDefaultIdolDialogWithBtn1(this@FeedActivity,
                                        null,
                                        responseMsg,
                                        { view: View? -> Util.closeIdolDialog() }
                                    )
                                }

                            }
                            reportDialog.dismiss()
                        },
                        { throwable ->
                            Toast.makeText(this@FeedActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
        binding.btnCancel.setOnClickListener {
            reportDialog.dismiss()
        }
    }

    //차단 버튼 눌렀을 경우
    fun block() {
        val block : String = if(userBlock) "N" else "Y"
        lifecycleScope.launch {
            blocksRepository.addBlock(
                userId,
                1,
                block,
                { response ->
                    if (response.optBoolean("success")) {
                        userBlockChange()
                        //설정에서 피드 들어갔다가 차단 상태를 바꿨을 때, 값 전송(설정에서 차단 버튼 상태 바꾸기 위함)
                        USER_BLOCK_CHANGE = userBlock
                        val intent = Intent()
                        intent.putExtra(PARAM_USER_ID, userId)
                        intent.putExtra(PARAM_USER_BLOCK_STATUS, block)
                        setResult(ResultCode.BLOCKED.value, intent)
                    }

                    else{
                        Logger.v("block", "block false")
                        val responseMsg = ErrorControl.parseError(this@FeedActivity, response)
                        if (responseMsg != null) {
                            Util.showDefaultIdolDialogWithBtn1(this@FeedActivity,
                                null,
                                responseMsg,
                                {Util.closeIdolDialog()},true)
                        }

                    }
                },
                { throwable ->
                    Util.showDefaultIdolDialogWithBtn1(this@FeedActivity,
                        null,
                        throwable.message,
                        { Util.closeIdolDialog() },true)
                }
            )
        }
        userBlock = !userBlock
    }

    fun report(){
        //피드 나갔다 들어왔을 경우
        if(!userReport) {
            //신고 했는지 체크
            MainScope().launch {
                reportRepository.getReportPossible(
                    mUser.id.toLong(),
                    { response ->
                        if (response.optBoolean("success")) {
                            reportUser()
                        }
                        else{
                            val responseMsg = ErrorControl.parseError(this@FeedActivity, response);
                            if (responseMsg != null) {
                                Util.showDefaultIdolDialogWithBtn1(this@FeedActivity,
                                    null,
                                    responseMsg,
                                    {Util.closeIdolDialog()},true)
                            }
                        }
                    },
                    { throwable ->
                        Toast.makeText(this@FeedActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        //피드 내에서 신고하고 피드 안나가고 바로 신고했을 경우 전역변수로 체크
        else {
            Util.showDefaultIdolDialogWithBtn1(
                    this, null, resources.getString(
                    R.string.failed_to_report_user__already_reported
            )
            ) {
                Util.closeIdolDialog()
            }
        }
    }

    //바텀시트 차단 눌렀을 경우 나오는 다이얼로그
    fun blockCheckDialog(){
        //차단 되어있으면
       if(userBlock){
           setUiActionFirebaseGoogleAnalyticsActivity(
               GaAction.FEED_UNBLOCK.actionValue,
               GaAction.FEED_UNBLOCK.label
           )
            block()
        }
        //차단 안되어있으면
        else {
            Util.showDefaultIdolDialogWithBtn2(this, null, getString(R.string.block_question),
                    {
                        setUiActionFirebaseGoogleAnalyticsActivity(
                            GaAction.FEED_BLOCK.actionValue,
                            GaAction.FEED_BLOCK.label
                        )
                        //차단
                        block()
                        Util.closeIdolDialog()
                    },
                    {
                        Util.closeIdolDialog()
                    })
        }
    }
    //유저 차단/해제 시 preference 재정의 및 피드 아래 게시글 처리
    fun userBlockChange() {
        var setUserBlockList = ArrayList<Int>() //기존 차단 유저 리스트에서 현 피드 사용자 유저만 add, remove 하는 값
        if(userBlock){
            userBlockList.add(userId)
        }
        else{
            userBlockList.remove(userId)
        }
        setUserBlockList = userBlockList
        Util.setPreferenceArray(this, Const.USER_BLOCK_LIST, setUserBlockList)

        if(isPrivacy) {
            setPrivacy(userId)
        }
        else{
            setPhotoContents(feedPhotoCount)
            setActivityContents(feedArticleCount)
        }
        if (!isPrivacy && feedPhotoList.size == 0) {
            val fragment = vpAdapter.getItem(0) as FeedPhotoFragment
            fragment.showEmpty(userId)
        }

    }

    // 좋아요 Api 중복호출을 예방하기 위해 debounce 처리한 함수
    private fun setClickSubject() {
        clickSubject.debounce(500, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
            .subscribe { model ->
                if(model.isUserLikeCache == model.isUserLike) {
                    return@subscribe
                }
                postArticleLike(model)
            }.addTo(disposable)
    }

    fun postArticleLike(model: ArticleModel) {
        MainScope().launch {
            likeArticleUseCase(model.id, !model.isUserLike).collect { response ->
                if( !response.success ) {
                    response.message?.let {
                        Toast.makeText(this@FeedActivity, it, Toast.LENGTH_SHORT).show()
                    }
                    return@collect
                }

                model.isUserLike = response.liked
                getArticleResource(model.resourceUri)
            }
        }
    }

    fun getArticleResource(resourceUri: String) {
        lifecycleScope.launch {
            articlesRepository.getArticle(
                resourceUri,
                { response ->
                    try {
                        val gson = IdolGson.getInstance(true)

                        val model = gson.fromJson(response.toString(), ArticleModel::class.java)
                        // 현재 게시글들 중 업데이트할거 찾기
                        val updateFeedArticleIdx = mFeedArticleList.indexOfFirst { it.resourceUri == resourceUri }
                        if(updateFeedArticleIdx >= 0) {
                            mFeedArticleList[updateFeedArticleIdx] = model
                            (vpAdapter.getItem(1) as FeedActivityFragment)
                                .mFeedArticleAdapter?.notifyItemChanged(updateFeedArticleIdx)
                        }

                        val updateFeedCommentIdx = mFeedCommentArticleList.indexOfFirst { it.resourceUri == resourceUri }
                        if(updateFeedCommentIdx >= 0) {
                            mFeedCommentArticleList[updateFeedCommentIdx] = model
                            (vpAdapter.getItem(2) as FeedCommentFragment)
                                .mFeedArticleAdapter?.notifyItemChanged(updateFeedCommentIdx)
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                { }
            )
        }
    }

    companion object {
        const val PARAM_USER = "paramUser"
        const val PARAM_USER_ID = "paramUserId"
        const val PARAM_USER_BLOCK_STATUS = "params_user_block_status"
        const val PARAM_COMMENT = "comment"
        const val PARAM_ARTICLE = "article"
        const val PARAM_PHOTO = "image"
        const val PARAM_ARTICLE_ID = "articleId"
        const val PARAM_EVENT_HEART = "event_heart"
        const val CATEGORY_PHOTO = "categoryPhoto"
        const val CATEGORY_ACTIVITY = "categoryActivity"
        const val CATEGORY_COMMENT = "categoryComment"
        const val REPORT_USER = "user"

        const val SIZE_OF_ARTICLE_LIMIT = 50
        const val SIZE_OF_PHOTO_LIMIT = 18

        const val PHOTO_RESIZE_WIDTH = 400
        const val PHOTO_RESIZE_HEIGHT = 400

        @JvmField
        var USER_BLOCK_CHANGE = false   //유저 차단 리스트가 변했는지 체크용

        var onIconResId = mutableListOf(
            R.drawable.icon_feed_photo_on,
            R.drawable.icon_feed_activity_on,
            R.drawable.icon_feed_comment_on
        )

        var offIconResId = mutableListOf(
            R.drawable.icon_feed_photo_off,
            R.drawable.icon_feed_activity_off,
            R.drawable.icon_feed_comment_off
        )

        @JvmStatic
        fun createIntent(context: Context, user: UserModel?): Intent {
            val intent = Intent(context, FeedActivity::class.java)
            val args = Bundle()
            args.putSerializable(PARAM_USER, user)
            intent.putExtras(args)

            return intent
        }
    }
}
