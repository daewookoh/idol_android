package net.ib.mn.link

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.BoardActivity
import net.ib.mn.activity.CharityCountActivity
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.EventActivity
import net.ib.mn.activity.FacedetectActivity
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.FreeboardActivity
import net.ib.mn.feature.friend.FriendsActivity
import net.ib.mn.activity.GalleryActivity
import net.ib.mn.activity.HeartPickActivity
import net.ib.mn.activity.HeartPlusFreeActivity
import net.ib.mn.activity.IdolQuizMainActivity
import net.ib.mn.activity.MainActivity
import net.ib.mn.activity.MyCouponActivity
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.activity.NoticeActivity
import net.ib.mn.activity.StartupActivity
import net.ib.mn.activity.StatsActivity
import net.ib.mn.activity.WebViewActivity
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.attendance.AttendanceActivity
import net.ib.mn.awards.IdolAwardsActivity
import net.ib.mn.base.BaseApplication
import net.ib.mn.common.util.logD
import net.ib.mn.common.util.logI
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.repository.AwardsRepositoryImpl
import net.ib.mn.core.data.repository.CouponRepositoryImpl
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.OnepickRepositoryImpl
import net.ib.mn.core.data.repository.PlayRepositoryImpl
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.core.data.repository.ThemepickRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.core.data.repository.language.LanguagePreferenceRepository
import net.ib.mn.core.domain.usecase.GetConfigSelfUseCase
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.ActivityAppLinkBinding
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.feature.friend.FriendInviteActivity
import net.ib.mn.feature.friend.InvitePayload
import net.ib.mn.feature.search.history.SearchHistoryActivity
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.liveStreaming.LiveStreamingActivity
import net.ib.mn.liveStreaming.LiveStreamingListActivity
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.LiveStreamListModel
import net.ib.mn.model.OnepickTopicModel
import net.ib.mn.model.PushMessageModel
import net.ib.mn.model.ScheduleModel
import net.ib.mn.model.SupportListModel
import net.ib.mn.model.ThemepickModel
import net.ib.mn.model.toPresentation
import net.ib.mn.onepick.AlternateLinkFragmentActivity
import net.ib.mn.onepick.OnepickResultActivity
import net.ib.mn.onepick.ThemePickRankActivity
import net.ib.mn.onepick.ThemePickResultActivity
import net.ib.mn.support.SupportDetailActivity
import net.ib.mn.support.SupportMainActivity
import net.ib.mn.support.SupportPhotoCertifyActivity
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.SupportedLanguage
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.getModelFromPref
import net.ib.mn.utils.modelToString
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class AppLinkActivity : BaseActivity() {

    private lateinit var binding: ActivityAppLinkBinding

    private var supportStatus = "inprogress"
    private var boardStatus = "qna"
    private var recordStatus = "records"
    private var storeStatus = "store"

    private var kindOfDeepLink: String? = null

    private var isSolo: Boolean = false
    private var isMale: Boolean = false

    private var couponValue: String? = null
    private var id: Int = 0
    private var scheduleId: Int = 0

    private var mNextIntent: Intent? = null

    private var tabOfCommunity = "community"
    private var isWallPaper = false
    private var feedUserId: Int = 0
    private var uri: String? = null

    @Inject
    lateinit var supportRepository: SupportRepositoryImpl
    @Inject
    lateinit var onepickRepository: OnepickRepositoryImpl
    @Inject
    lateinit var themepickRepository: ThemepickRepositoryImpl
    @Inject
    lateinit var playRepository: PlayRepositoryImpl
    @Inject
    lateinit var awardsRepository: AwardsRepositoryImpl
    @Inject
    lateinit var couponRepository: CouponRepositoryImpl
    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var miscRepository: MiscRepository
    @Inject
    lateinit var articlesRepository: ArticlesRepository
    @Inject
    lateinit var getConfigSelfUseCase: GetConfigSelfUseCase
    @Inject
    lateinit var languagePreferenceRepository: LanguagePreferenceRepository
    @Inject
    lateinit var usersRepository: UsersRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLinkBinding.inflate(layoutInflater)
        binding.clContainer.applySystemBarInsets()

        setContentView(binding.root)

        Util.showProgress(this, true)
        initSet()
    }

    private fun initSet() {
        var host = Util.getPreference(this, Const.PREF_SERVER_URL) ?: ServerUrl.HOST
        if (host.isNullOrEmpty()) {
            host = ServerUrl.HOST
        }

        // 테섭은 무조건 bbb로 보내달라는 요청쓰.
        if (host == ServerUrl.HOST_TEST) {
            host = ServerUrl.HOST_BBB_TEST
        }

        var intentData = intent.data
        var intentDataString = intent.dataString

        if(intentData == null) {
            val message = intent.getStringExtra("message")
            message?.let {
                val pushMessageModel = it.getModelFromPref<PushMessageModel>() ?: PushMessageModel()
                intentDataString = pushMessageModel.link
                intentData = pushMessageModel.link?.toUri()
            }
        }

        if (intentData == null || intentData?.isHierarchical == false) {
            defaultStartNextActivity()
            finish()
            return
        }

        val intentScheme = intentData?.scheme
        val intentHost = intentData?.host

        var intentSchemeHost = "$intentScheme://$intentHost"
        if (intentSchemeHost == ServerUrl.HOST_TEST) {
            intentSchemeHost = ServerUrl.HOST_BBB_TEST
        }

        Logger.v("AppLink::${intentData?.modelToString()}")

        val buildHost = if (BuildConfig.CELEB) ServerUrl.HOST_IDOL else ServerUrl.HOST_ACTOR
        if (intentHost == buildHost) {
            val packageName =
                if (BuildConfig.CELEB) Const.PACKAGE_NAME_IDOL else Const.PACKAGE_NAME_ACTOR
            try {
                mNextIntent = Intent(Intent.ACTION_VIEW, Uri.parse(intentDataString)).apply {
                    setPackage(packageName)
                }
                startNextActivity()
            } catch (e: ActivityNotFoundException) {
                Util.gotoMarket(this, packageName)
                e.printStackTrace()
            }
            Util.closeProgress()
            finish()
            return
        }

        // 딥링크 (웹에서 온 링크 처리).
        if (intentScheme != "http" && intentScheme != "https") {
            calculateWebLink()
            getLinkUriApi()
            return
        }

        // 내부 host 링크 처리. ex) myloveidol 링크만 처리.
        if (intentSchemeHost == host) {
            calculateLinkData(intentDataString ?: return)
            setFirebaseAction()
            getLinkUriApi()
            return
        }

        if (!Const.FEATURE_WEB_VIEW) {
            Util.closeProgress()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intentDataString))
            mNextIntent = intent
            startNextActivity()
            finish()
            return
        }

        // 외부 host 모든 링크 처리.
        Util.closeProgress()
        val intent = Intent(this, WebViewExternalLinkActivity::class.java).apply {
            data = intentData
        }
        mNextIntent = intent
        startNextActivity()
        finish()
    }

    private fun calculateLinkData(
        uri: String,
    ) {
        val parsedUri = Uri.parse(uri)

        if (parsedUri.lastPathSegment == null) {
            return
        }

        val splitedUri = parsedUri.pathSegments

        Log.d("@@@@", "calculateLinkData: $splitedUri")

        try {
            when (splitedUri[0]) {
                LinkStatus.AUTH.status -> {
                    kindOfDeepLink = DEEPLINK_AUTH
                    this.uri = uri
                }

                LinkStatus.ARTICLES.status -> {
                    kindOfDeepLink = DEEPLINK_ARTICLE
                    id = parsedUri.lastPathSegment?.toInt() ?: return
                }

                LinkStatus.COMMUNITY.status -> {
                    kindOfDeepLink = DEEPLINK_COMMUNITY
                    val start: Int = uri.indexOf("idol=") + 5 // 5 is length of ("idol=")

                    var end: Int = uri.indexOf("&", start)
                    end = if (end == -1) uri.length else end
                    id = uri.substring(start, end).toInt()

                    tabOfCommunity = parsedUri.getQueryParameter("tab") ?: "community"
                    isWallPaper = parsedUri.getQueryParameter("wp")?.toBoolean() ?: false
                }

                LinkStatus.COUPON.status -> {
                    kindOfDeepLink = DEEPLINK_COUPON
                    couponValue = splitedUri[splitedUri.size - 1]
                }

                LinkStatus.IDOL.status -> {
                    when (splitedUri[1]) {
                        LinkStatus.HOF.status -> kindOfDeepLink = DEEPLINK_HOF
                        else -> {
                            kindOfDeepLink = DEEPLINK_IDOL
                            if (!BuildConfig.CELEB) {
                                isSolo = splitedUri[1].equals("solo", ignoreCase = true)
                            }
                            isMale = splitedUri[2].equals("male", ignoreCase = true)
                        }
                    }
                }

                LinkStatus.SUPPORTS.status -> {
                    try {
                        kindOfDeepLink = DEEPLINK_SUPPORT
                        id = parsedUri.lastPathSegment?.toInt() ?: return
                    } catch (e: Exception) {
                        kindOfDeepLink = DEEPLINK_SUPPORT_MAIN_STATUS
                        supportStatus = parsedUri.lastPathSegment ?: "inprogress"
                    }
                }

                LinkStatus.THEMEPICK.status -> {
                    id = parsedUri.lastPathSegment?.toIntOrNull() ?: 0
                    kindOfDeepLink = DEEPLINK_THEME_PICK
                }

                LinkStatus.LIVE.status -> {
                    kindOfDeepLink = DEEPLINK_LIVE_STREAMING
                    id = parsedUri.lastPathSegment?.toInt() ?: return
                }

                LinkStatus.ONE_PICK.status -> {
                    id = parsedUri.lastPathSegment?.toIntOrNull() ?: 0
                    kindOfDeepLink = DEEPLINK_IMAGE_PICK
                }

                LinkStatus.BANNERGRAM.status -> {
                    kindOfDeepLink = DEEPLINK_BANNERGRAM
                    id = parsedUri.lastPathSegment?.toInt() ?: return
                }

                LinkStatus.RECORDS.status -> {
                    recordStatus = parsedUri.lastPathSegment ?: return
                    kindOfDeepLink = DEEPLINK_RECORDS
                }

                LinkStatus.QNA.status, LinkStatus.BOARD.status -> {
                    kindOfDeepLink = DEEPLINK_BOARD
                    boardStatus = if (parsedUri.lastPathSegment == "qna") "qna" else "board"
                }

                LinkStatus.OFFERWALL.status -> {
                    kindOfDeepLink = DEEPLINK_OFFERWALL
                }

                LinkStatus.HEARTPICK.status -> {
                    kindOfDeepLink = DEEPLINK_HEARTPICK
                    id = parsedUri.lastPathSegment?.toIntOrNull() ?: return
                }

                LinkStatus.QUIZZES.status -> {
                    kindOfDeepLink = DEEPLINK_QUIZZES
                }

                LinkStatus.STORE.status -> {
                    storeStatus = parsedUri.lastPathSegment ?: return
                    kindOfDeepLink = DEEPLINK_STORE
                }

                LinkStatus.FACEDETECT.status -> {
                    kindOfDeepLink = DEEPLINK_FACEDETECT
                }

                LinkStatus.NOTICES.status -> {
                    id = parsedUri.lastPathSegment?.toIntOrNull() ?: 0
                    kindOfDeepLink = DEEPLINK_NOTICE
                }

                LinkStatus.EVENTS.status -> {
                    id = parsedUri.lastPathSegment?.toIntOrNull() ?: 0
                    kindOfDeepLink = DEEPLINK_EVENT
                }

                LinkStatus.MENU.status -> {
                    kindOfDeepLink = DEEPLINK_MENU
                }

                LinkStatus.AWARD.status -> {
                    kindOfDeepLink = DEEPLINK_AWARD
                }

                LinkStatus.HOTTRENDS.status -> {
                    kindOfDeepLink = DEEPLINK_HOTTRENDS
                }
                LinkStatus.FRIENDS.status -> {
                    kindOfDeepLink = DEEPLINK_FRIENDS
                }
                LinkStatus.ATTENDANCE.status -> {
                    kindOfDeepLink = DEEPLINK_ATTENDANCE
                }

                LinkStatus.SCHEDULES.status -> {
                    scheduleId = parsedUri.getQueryParameter("schedule_id")?.toIntOrNull() ?: 0
                    kindOfDeepLink = DEEPLINK_SCHEDULE
                }
                LinkStatus.MIRACLE.status -> {
                    kindOfDeepLink = DEEPLINK_MIRACLE
                }

                LinkStatus.MY_FEED.status -> {
                    feedUserId = parsedUri.getQueryParameter("user_id")?.toIntOrNull() ?: 0
                    kindOfDeepLink = DEEPLINK_MY_FEED
                }

                LinkStatus.MY_INFO.status -> {
                    kindOfDeepLink = DEEPLINK_MY_INFO
                }

                LinkStatus.ROOKIE.status -> {
                    kindOfDeepLink = DEEPLINK_ROOKIE
                }
                LinkStatus.INVITE.status -> {
                    kindOfDeepLink = DEEPLINK_INVITE
                }
                LinkStatus.HOF.status -> {
                    kindOfDeepLink = DEEPLINK_HOF
                }
                "webview" -> {
                    when(splitedUri[1] ?: "") {
                        LinkStatus.INVITE_SHARE.status -> {
                            kindOfDeepLink = DEEPLINK_INVITE_SHARE
                        }
                        else -> {

                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            defaultStartNextActivity()
            finish()
        }
    }

    private fun calculateWebLink() {
        val scheme = if (BuildConfig.CELEB) "choeaedolceleb" else "choeaedol"

        val uri = intent.data
        Logger.v("AppLink:: $uri")
        if (!scheme.equals(intent.scheme, true)) {
            return
        }

        // 웹링크.
        Logger.v("AppLink:: inininin")

        val deeplink = uri?.getQueryParameter("deeplink") ?: return

        // 웹에서 들어올때사용합니다(외부링크)
        // 앱 -> 앱으로 들어갈떄는 위에 로직을탐(intent data만뽑아서 support있나확인).
        val splitUri = deeplink.split("/".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()

        when {
            splitUri[splitUri.size - 2] == LinkStatus.SUPPORT.status -> {
                kindOfDeepLink = DEEPLINK_SUPPORT
                id = splitUri[splitUri.size - 1].toInt()
            }

            splitUri[splitUri.size - 2] == LinkStatus.THEMEPICK.status -> {
                kindOfDeepLink = DEEPLINK_THEME_PICK
                id = splitUri[splitUri.size - 1].toInt()
            }

            splitUri[splitUri.size - 2] == LinkStatus.IMAGEPICK.status -> {
                kindOfDeepLink = DEEPLINK_IMAGE_PICK
                id = splitUri[splitUri.size - 1].toInt()
            }

            splitUri[splitUri.size - 2] == LinkStatus.ARTICLES.status -> {
                kindOfDeepLink = DEEPLINK_ARTICLE
                id = splitUri[splitUri.size - 1].toInt()
            }
        }
    }

    private fun isActivityAlive(): Boolean {
        return try {
            (application as BaseApplication).getActivityCount() > 1
        } catch (e: java.lang.Exception) {
            false
        }
    }

    @OptIn(UnstableApi::class)
    private fun getLinkUriApi() {
        when (kindOfDeepLink) {
            DEEPLINK_AUTH -> {
                mNextIntent = MainActivity.createIntent(this, false).apply {
                    putExtra("is_auth_request", true)
                    putExtra("uri", uri)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startNextActivity()
            }

            DEEPLINK_ARTICLE -> lifecycleScope.launch {
                articlesRepository.getArticle(
                    "/api/v1/articles/$id/",
                    { response ->
                        // article은 KST
                        val article = IdolGson.getInstance(true).fromJson(
                            response.toString(),
                            ArticleModel::class.java,
                        )

                        val commentIntent = NewCommentActivity.createIntent(
                            this@AppLinkActivity,
                            article,
                            -1,
                            false,
                            NewCommentAdapter.TYPE_ARTICLE,
                        ).apply {
                            putExtra(EXTRA_NEXT_ACTIVITY, NewCommentActivity::class.java)
                        }

                        mNextIntent = if (isActivityAlive()) {
                            commentIntent
                        } else {
                            StartupActivity.createIntentForLink(
                                this@AppLinkActivity,
                                commentIntent,
                                NewCommentActivity::class.java,
                            )
                        }

                        startNextActivity()
                        finish()
                    },
                    {
                        defaultStartNextActivity()
                        finish()
                    }
                )
            }
            DEEPLINK_COMMUNITY -> lifecycleScope.launch {
                logI("Call api")
                idolsRepository.getIdolsForSearch(
                    id = id,
                    listener = { response ->
                        try {
                            val idol = IdolGson.getInstance()
                                .fromJson(
                                    response.getJSONArray("objects")
                                        .getJSONObject(0)
                                        .toString(),
                                    IdolModel::class.java,
                                )

                            val communityIntent = CommunityActivity.createIntent(
                                this@AppLinkActivity,
                                idol,
                                CommunityActivity.CATEGORY_COMMUNITY,
                            ).apply {
                                putExtra(
                                    EXTRA_NEXT_ACTIVITY,
                                    CommunityActivity::class.java,
                                )
                                putExtra(CommunityActivity.PARAM_CATEGORY, tabOfCommunity)
                                putExtra(CommunityActivity.PARAM_IS_WALLPAPER, isWallPaper)
                            }

                            mNextIntent = if (isActivityAlive()) {
                                communityIntent
                            } else {
                                StartupActivity.createIntentForLink(
                                    this@AppLinkActivity,
                                    communityIntent,
                                    CommunityActivity::class.java,
                                )
                            }

                            startNextActivity()
                            finish()
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            defaultStartNextActivity()
                            finish()
                        }
                    }, errorListener = {
                        defaultStartNextActivity()
                        finish()
                    }
                )
            }

            DEEPLINK_COUPON -> MainScope().launch {
                couponRepository.take(
                    couponValue,
                    { response ->
                        if (!response.optBoolean("success")) {
                            val responseMsg = ErrorControl.parseError(
                                this@AppLinkActivity,
                                response,
                            )
                            Toast.makeText(
                                this@AppLinkActivity,
                                responseMsg,
                                Toast.LENGTH_SHORT,
                            ).show()
                            defaultStartNextActivity()
                            finish()
                            return@take
                        }

                        val couponIntent = MyCouponActivity.createIntent(
                            this@AppLinkActivity,
                            1,
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                            )
                            putExtra(EXTRA_NEXT_ACTIVITY, MyCouponActivity::class.java)
                        }

                        mNextIntent = if (isActivityAlive()) {
                            couponIntent
                        } else {
                            StartupActivity.createIntentForLink(
                                this@AppLinkActivity,
                                couponIntent,
                                MyCouponActivity::class.java,
                            )
                        }

                        startNextActivity()
                        finish()
                    }, { throwable ->
                        Toast.makeText(
                            this@AppLinkActivity,
                            R.string.failed_to_load,
                            Toast.LENGTH_SHORT,
                        ).show()
                        defaultStartNextActivity()
                        finish()
                    }
                )
            }

            DEEPLINK_IDOL -> {
                val mainIntent = if (!BuildConfig.CELEB) {
                    MainActivity.createIntent(
                        this,
                        false,
                    ).apply {
                        putExtra(BaseActivity.EXTRA_IDOL_STATUS_CHANGE, true)
                        putExtra(BaseActivity.PARAM_IS_SOLO, isSolo)
                        putExtra(BaseActivity.PARAM_IS_MALE, isMale)
                        putExtra(EXTRA_NEXT_ACTIVITY, MainActivity::class.java)
                    }
                } else {
                    MainActivity.createIntent(this, true).apply {
                        putExtra(EXTRA_NEXT_ACTIVITY, MainActivity::class.java)
                    }
                }

                mNextIntent = if (isActivityAlive()) {
                    mainIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this, mainIntent, MainActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_SUPPORT -> MainScope().launch {
                supportRepository.getSupportDetail(id,
                    { response ->
                        try {
                            val (_, _, _, _, _, _, id1, _, idol, imageUrl, _, _, title) = IdolGson.getInstance(
                                true,
                            ).fromJson(
                                response.toString(),
                                SupportListModel::class.java,
                            )

                            val supportInfo = JSONObject()
                            if (idol.getName(this@AppLinkActivity).contains("_")) {
                                supportInfo.put(
                                    "name",
                                    Util.nameSplit(this@AppLinkActivity, idol)[0],
                                )
                                supportInfo.put(
                                    "group",
                                    Util.nameSplit(this@AppLinkActivity, idol)[1],
                                )
                            } else {
                                supportInfo.put("name", idol.getName(this@AppLinkActivity))
                            }

                            supportInfo.put("support_id", id1)
                            supportInfo.put("title", title)
                            supportInfo.put("profile_img_url", imageUrl)

                            val supportIntent = if (response.optInt("status") == Const.SUPPORT_DETAIL) {
                                SupportDetailActivity.createIntent(this@AppLinkActivity, id1)
                            } else {
                                SupportPhotoCertifyActivity.createIntent(
                                    this@AppLinkActivity,
                                    supportInfo.toString(),
                                )
                            }

                            if (isActivityAlive()) {
                                mNextIntent = supportIntent
                            } else {
                                val nextActivity =
                                    if (response.optInt("status") == Const.SUPPORT_DETAIL) {
                                        SupportDetailActivity::class.java
                                    } else {
                                        SupportPhotoCertifyActivity::class.java
                                    }

                                mNextIntent = StartupActivity.createIntentForLink(
                                    this@AppLinkActivity,
                                    supportIntent,
                                    nextActivity,
                                )
                            }

                            startNextActivity()
                            finish()
                        } catch (e: java.lang.Exception) {
                            Toast.makeText(
                                this@AppLinkActivity,
                                R.string.error_abnormal_default,
                                Toast.LENGTH_SHORT,
                            ).show()
                            defaultStartNextActivity()
                            finish()
                        }
                    },
                    { throwable ->
                        Toast.makeText(
                            this@AppLinkActivity,
                            R.string.error_abnormal_default,
                            Toast.LENGTH_SHORT,
                        ).show()
                        defaultStartNextActivity()
                        finish()
                    }
                )
            }

            DEEPLINK_THEME_PICK -> {
                if (id > 0) {
                    MainScope().launch {
                        themepickRepository.getResult(
                            id,
                            listener = { response ->
                                try {
                                    var themepickModel: ThemepickModel? =
                                        IdolGson.getInstance().fromJson(
                                            response.toString(),
                                            ThemepickModel::class.java)

                                    if (themepickModel == null) {
                                        defaultStartNextActivity()
                                        finish()
                                    }

                                    if (themepickModel?.status == 0) {
                                        val themePickPrelaunchIntent = ThemePickRankActivity.createIntent(
                                            this@AppLinkActivity,
                                            themepickModel,
                                            false,
                                        )

                                        mNextIntent = if (isActivityAlive()) {
                                            themePickPrelaunchIntent
                                        } else {
                                            StartupActivity.createIntentForLink(
                                                this@AppLinkActivity,
                                                themePickPrelaunchIntent,
                                                ThemePickRankActivity::class.java,
                                            )
                                        }
                                    } else {
                                        val themePickIntent = ThemePickResultActivity.createIntent(
                                            this@AppLinkActivity,
                                            themepickModel!!,
                                            false,
                                        ).apply {
                                            putExtra(EXTRA_NEXT_ACTIVITY, ThemePickResultActivity::class.java)
                                        }

                                        mNextIntent = if (isActivityAlive()) {
                                            themePickIntent
                                        } else {
                                            StartupActivity.createIntentForLink(
                                                this@AppLinkActivity,
                                                themePickIntent,
                                                ThemePickResultActivity::class.java,
                                            )
                                        }
                                    }

                                    startNextActivity()
                                    finish()
                                } catch (e: java.lang.Exception) {
                                    Toast.makeText(
                                        this@AppLinkActivity,
                                        R.string.error_abnormal_default,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    defaultStartNextActivity()
                                    finish()
                                }
                            },
                            errorListener = { throwable ->
                                Toast.makeText(
                                    this@AppLinkActivity,
                                    R.string.error_abnormal_default,
                                    Toast.LENGTH_SHORT,
                                ).show()
                                defaultStartNextActivity()
                                finish()
                            }
                        )
                    }
                } else {
                    val themePickIntent = AlternateLinkFragmentActivity.createIntent(this)
                        .apply {
                            putExtra(EXTRA_IS_IMAGEPICK, false)
                            putExtra(EXTRA_TITLE, getString(R.string.onepick))
                            putExtra(EXTRA_LINK_STATUS, LinkStatus.ONE_PICK.status)
                        }

                    mNextIntent = if (isActivityAlive()) {
                        themePickIntent
                    } else {
                        StartupActivity.createIntentForLink(
                            this,
                            themePickIntent,
                            MainActivity::class.java,
                        )
                    }

                    startNextActivity()
                    finish()
                }
            }

            DEEPLINK_IMAGE_PICK -> {
                if (id > 0) {
                    MainScope().launch {
                        onepickRepository.get(
                            offset = 0,
                            limit = 100,
                            { response ->
                                try {
                                    var onepickTopicModel: OnepickTopicModel? = null
                                    val array = response.getJSONArray("objects")
                                    var i = 0
                                    while (i < array.length()) {
                                        val model =
                                            IdolGson.getInstance().fromJson(
                                                array.getJSONObject(i).toString(),
                                                OnepickTopicModel::class.java,
                                            )
                                        if (model.id == id) { // 딥링크로 넘어온 아이디와 비교해줘서 모델에 넣어줌.
                                            onepickTopicModel = model
                                        }
                                        i++
                                    }

                                    if (onepickTopicModel == null) {
                                        defaultStartNextActivity()
                                        finish()
                                    }

                                    val imagePickIntent = OnepickResultActivity.createIntent(
                                        this@AppLinkActivity,
                                        onepickTopicModel ?: return@get,
                                    ).apply {
                                        putExtra(EXTRA_NEXT_ACTIVITY, OnepickResultActivity::class.java)
                                    }

                                    mNextIntent = if (isActivityAlive()) {
                                        imagePickIntent
                                    } else {
                                        StartupActivity.createIntentForLink(
                                            this@AppLinkActivity,
                                            imagePickIntent,
                                            OnepickResultActivity::class.java,
                                        )
                                    }

                                    startNextActivity()
                                    finish()
                                } catch (e: java.lang.Exception) {
                                    Toast.makeText(
                                        this@AppLinkActivity,
                                        R.string.error_abnormal_default,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    defaultStartNextActivity()
                                    finish()
                                }
                            }, { throwable ->
                                Toast.makeText(
                                    this@AppLinkActivity,
                                    R.string.error_abnormal_default,
                                    Toast.LENGTH_SHORT,
                                ).show()
                                defaultStartNextActivity()
                                finish()
                            }
                        )
                    }
                } else {
                    val imagePickIntent = AlternateLinkFragmentActivity.createIntent(this)
                        .apply {
                            putExtra(EXTRA_IS_IMAGEPICK, true)
                            putExtra(EXTRA_TITLE, getString(R.string.onepick))
                            putExtra(EXTRA_LINK_STATUS, LinkStatus.ONE_PICK.status)
                        }

                    mNextIntent = if (isActivityAlive()) {
                        imagePickIntent
                    } else {
                        StartupActivity.createIntentForLink(
                            this,
                            imagePickIntent,
                            MainActivity::class.java,
                        )
                    }

                    startNextActivity()
                    finish()
                }
            }

            DEEPLINK_LIVE_STREAMING -> {
                if (id > 0) {
                    // 해당 live id의  라이브 정보를 받아온다.
                    MainScope().launch {
                        playRepository.getInfo(
                            id,
                            { response ->
                                try {
                                    if (!response.optBoolean("success", false)) {
                                        Toast.makeText(
                                            this@AppLinkActivity,
                                            R.string.error_abnormal_default,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        defaultStartNextActivity()
                                        finish()
                                        return@getInfo
                                    }

                                    // 라이브 모델
                                    val liveStreamModel =
                                        IdolGson.getInstance().fromJson(
                                            response.getJSONObject("live").toString(),
                                            LiveStreamListModel::class.java,
                                        )

                                    if (liveStreamModel == null) {
                                        defaultStartNextActivity()
                                        finish()
                                    }

                                    val liveIntent = LiveStreamingActivity.createIntent(
                                        this@AppLinkActivity,
                                        liveStreamModel ?: return@getInfo,
                                    ).apply {
                                        putExtra(EXTRA_NEXT_ACTIVITY, LiveStreamingActivity::class.java)
                                    }

                                    mNextIntent = if (isActivityAlive()) {
                                        liveIntent
                                    } else {
                                        StartupActivity.createIntentForLink(
                                            this@AppLinkActivity,
                                            liveIntent, LiveStreamingActivity::class.java,
                                        )
                                    }

                                    startNextActivity()
                                    finish()
                                } catch (e: java.lang.Exception) {
                                    Toast.makeText(
                                        this@AppLinkActivity,
                                        R.string.error_abnormal_default,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    defaultStartNextActivity()
                                    finish()
                                }
                            }, { throwable ->
                                Toast.makeText(
                                    this@AppLinkActivity,
                                    R.string.error_abnormal_default,
                                    Toast.LENGTH_SHORT,
                                ).show()
                                defaultStartNextActivity()
                                finish()
                            }
                        )
                    }
                } else {
                    val liveIntent =
                        if (BuildConfig.CELEB) {
                            Intent(this, LiveStreamingListActivity::class.java)
                                .apply {
                                    putExtra(BaseActivity.EXTRA_IS_LIVE, true)
                                    putExtra(
                                        EXTRA_NEXT_ACTIVITY,
                                        LiveStreamingListActivity::class.java,
                                    )
                                }
                        } else {
                            AlternateLinkFragmentActivity.createIntent(this).apply {
                                putExtra(BaseActivity.EXTRA_IS_LIVE, true)
                                putExtra(EXTRA_NEXT_ACTIVITY, MainActivity::class.java)
                                putExtra(EXTRA_TITLE, getString(R.string.menu_live))
                                putExtra(EXTRA_LINK_STATUS, LinkStatus.LIVE.status)
                            }
                        }

                    mNextIntent = if (isActivityAlive()) {
                        liveIntent
                    } else {
                        StartupActivity.createIntentForLink(
                            this,
                            liveIntent,
                            MainActivity::class.java,
                        )
                    }

                    startNextActivity()
                    finish()
                }
            }

            DEEPLINK_QUIZZES -> {
                val quizIntent =
                    if (LocaleUtil.isExistCurrentLocale(this, SupportedLanguage.BOARD_KIN_QUIZZES_TOP100_LOCALES)) {
                        IdolQuizMainActivity.createIntent(this)
                    } else {
                        MainActivity.createIntent(this, false)
                    }

                mNextIntent = if (isActivityAlive()) {
                    quizIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        quizIntent,
                        IdolQuizMainActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_STORE -> {
                val type = when (storeStatus) {
                    "heart" -> {
                        "H"
                    }

                    "package" -> {
                        "P"
                    }

                    "dia" -> {
                        "D"
                    }

                    else -> {
                        "H"
                    }
                }

                val newHeartStoreIntent =
                    NewHeartPlusActivity.createIntent(this, type)
                mNextIntent = if (isActivityAlive()) {
                    newHeartStoreIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        newHeartStoreIntent,
                        NewHeartPlusActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_OFFERWALL -> {
                val newHeartFreeIntent = HeartPlusFreeActivity.createIntent(this)
                mNextIntent = if (isActivityAlive()) {
                    newHeartFreeIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        newHeartFreeIntent,
                        NewHeartPlusActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_HEARTPICK -> {

                if (id == 0)  {
                    val heartPickIntent = AlternateLinkFragmentActivity.createIntent(
                        this
                    ).apply {
                        // 셀럽에서는 이미지픽 위치에 하트픽이 위치하고있음.. 화면을 같이쓰고 있음 혼돈주의.
                        if (BuildConfig.CELEB) {
                            putExtra(EXTRA_IS_IMAGEPICK, true)
                            putExtra(EXTRA_TITLE, getString(R.string.onepick))
                            putExtra(EXTRA_LINK_STATUS, LinkStatus.ONE_PICK.status)
                        } else {
                            putExtra(EXTRA_IS_HEART, true)
                            putExtra(EXTRA_TITLE, getString(R.string.heartpick))
                            putExtra(EXTRA_LINK_STATUS, LinkStatus.HEARTPICK_MAIN.status)
                        }
                    }
                    mNextIntent = if (isActivityAlive()) {
                        heartPickIntent
                    } else {
                        StartupActivity.createIntentForLink(
                            this,
                            heartPickIntent,
                            MainActivity::class.java,
                        )
                    }
                } else {
                    val heartPickIntent = HeartPickActivity.createIntent(this, id)
                    mNextIntent = if (isActivityAlive()) {
                        heartPickIntent
                    } else {
                        StartupActivity.createIntentForLink(
                            this,
                            heartPickIntent,
                            HeartPickActivity::class.java,
                        )
                    }
                }


                startNextActivity()
                finish()
            }

            DEEPLINK_BOARD -> {
                val boardIntent = FreeboardActivity.createIntent(this)

                mNextIntent = if (isActivityAlive()) {
                    boardIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        boardIntent,
                        BoardActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_SUPPORT_MAIN_STATUS -> {
                val supportMainIntent =
                    SupportMainActivity.createIntent(this, supportStatus)
                mNextIntent = if (isActivityAlive()) {
                    supportMainIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        supportMainIntent,
                        SupportMainActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_RECORDS -> {
                val charityIntent: Intent
                val nextActivity: Class<*>

                when (recordStatus) {
                    "angel" -> {
                        charityIntent = CharityCountActivity.createIntent(this, 0)
                        nextActivity = CharityCountActivity::class.java
                    }

                    "fairy" -> {
                        charityIntent = CharityCountActivity.createIntent(this, 1)
                        nextActivity = CharityCountActivity::class.java
                    }

                    "miracle" -> {
                        charityIntent = CharityCountActivity.createIntent(this, 2)
                        nextActivity = CharityCountActivity::class.java
                    }

                    else -> {
                        charityIntent = StatsActivity.createIntent(this)
                        nextActivity = StatsActivity::class.java
                    }
                }

                mNextIntent = if (isActivityAlive()) {
                    charityIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        charityIntent,
                        nextActivity,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_FACEDETECT -> {
                val faceIntent = FacedetectActivity.createIntent(this)
                mNextIntent = if (isActivityAlive()) {
                    faceIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        faceIntent,
                        FacedetectActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_MENU -> {
                val menuIntent =
                    MainActivity.createIntent(this, false).apply {
                        putExtra(EXTRA_IS_MENU, true)
                        putExtra(EXTRA_NEXT_ACTIVITY, MainActivity::class.java)
                    }
                mNextIntent = if (isActivityAlive()) {
                    menuIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        menuIntent,
                        MainActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_AWARD -> {
                val awardIntent = Intent(this, IdolAwardsActivity::class.java)

                // 어워즈 종료상태면 이동 안함
                if(!ConfigModel.getInstance(this).showAwardTab) {
                    defaultStartNextActivity()
                    finish()
                    return
                }

                if (isActivityAlive()) {
                    lifecycleScope.launch {
                        val result = getConfigSelfUseCase().first()
                        if(!result.success || result.data == null) {
                            defaultStartNextActivity()
                            finish()
                            return@launch
                        }
                        val response = result.data ?: return@launch
                        ConfigModel.getInstance(this@AppLinkActivity).parse(response)
                        if (!ConfigModel.getInstance(this@AppLinkActivity).showAwardTab) {
                            defaultStartNextActivity()
                            finish()
                            return@launch
                        }
                        getAwardData(awardIntent)
                    }
                } else {
                    mNextIntent = StartupActivity.createIntentForLink(
                        this,
                        awardIntent,
                        if (BuildConfig.CELEB) IdolAwardsActivity::class.java else MainActivity::class.java,
                    )
                    startNextActivity()
                    finish()
                }
            }

            DEEPLINK_HOF -> {

                val hofIntent =
                    MainActivity.createIntent(this, false).apply {
                        putExtra(EXTRA_IS_HOF, true)
                        putExtra(EXTRA_NEXT_ACTIVITY, MainActivity::class.java)
                    }
                mNextIntent = if (isActivityAlive()) {
                    hofIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        hofIntent,
                        MainActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_HOTTRENDS -> {
                val hotTrendIntent = SearchHistoryActivity.createIntent(this)

                mNextIntent = if (isActivityAlive()) {
                    hotTrendIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        hotTrendIntent,
                        SearchHistoryActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_BANNERGRAM -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val idol = getIdolByIdUseCase(id)
                        .mapDataResource { it?.toPresentation() }
                        .awaitOrThrow()

                    val galleryIntent = GalleryActivity.createIntent(this@AppLinkActivity, idol)
                    mNextIntent = if (isActivityAlive()) {
                        galleryIntent
                    } else {
                        StartupActivity.createIntentForLink(
                            this@AppLinkActivity,
                            galleryIntent,
                            GalleryActivity::class.java,
                        )
                    }

                    withContext(Dispatchers.Main) {
                        startNextActivity()
                        finish()
                    }
                }
            }

            DEEPLINK_NOTICE -> {
                if (id > 0) {
                    val webViewIntent = WebViewActivity.createIntent(
                        this,
                        Const.TYPE_NOTICE,
                        id,
                        getString(R.string.title_notice),
                    )

                    mNextIntent = if (isActivityAlive()) {
                        webViewIntent
                    } else {
                        StartupActivity.createIntentForLink(
                            this,
                            webViewIntent,
                            WebViewActivity::class.java,
                        )
                    }
                } else {
                    val noticeIntent = NoticeActivity.createIntent(
                        this,
                    )
                    mNextIntent = if (isActivityAlive()) {
                        noticeIntent
                    } else {
                        StartupActivity.createIntentForLink(
                            this,
                            noticeIntent,
                            NoticeActivity::class.java,
                        )
                    }
                }

                startNextActivity()
                finish()
            }

            DEEPLINK_EVENT -> {
                if (id > 0) {
                    val webViewIntent = WebViewActivity.createIntent(
                        this,
                        Const.TYPE_EVENT,
                        id,
                        getString(R.string.title_event),
                    )

                    mNextIntent = if (isActivityAlive()) {
                        webViewIntent
                    } else {
                        StartupActivity.createIntentForLink(
                            this,
                            webViewIntent,
                            WebViewActivity::class.java,
                        )
                    }
                } else {
                    val eventIntent = EventActivity.createIntent(
                        this,
                    )
                    mNextIntent = if (isActivityAlive()) {
                        eventIntent
                    } else {
                        StartupActivity.createIntentForLink(
                            this,
                            eventIntent,
                            EventActivity::class.java,
                        )
                    }
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_FRIENDS -> {
                val friendsIntent = getDefaultLinkIntent<FriendsActivity>()
                mNextIntent = if (isActivityAlive()) {
                    friendsIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this, friendsIntent, FriendsActivity::class.java
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_ATTENDANCE -> {
                val attendanceIntent = getDefaultLinkIntent<AttendanceActivity>()
                mNextIntent = if (isActivityAlive()) {
                    attendanceIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        attendanceIntent,
                        AttendanceActivity::class.java
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_SCHEDULE -> lifecycleScope.launch {
                miscRepository.getResource(
                    "schedules/$scheduleId/",
                    { response ->
                        val scheduleModel = IdolGson.getInstance()
                            .fromJson(response.toString(), ScheduleModel::class.java)

                        lifecycleScope.launch {
                            miscRepository.getResource(
                                "articles/${scheduleModel.article_id}/",
                                { response ->
                                    val articleModel = IdolGson.getInstance()
                                        .fromJson(response.toString(), ArticleModel::class.java)

                                    val scheduleCommentIntent = NewCommentActivity.createIntent(
                                        this@AppLinkActivity,
                                        articleModel,
                                        -1,
                                        scheduleModel,
                                        true,
                                        HashMap(),
                                        true,
                                    )

                                    mNextIntent = if (isActivityAlive()) {
                                        scheduleCommentIntent
                                    } else {
                                        StartupActivity.createIntentForLink(
                                            this@AppLinkActivity,
                                            scheduleCommentIntent,
                                            NewCommentActivity::class.java,
                                        )
                                    }

                                    startNextActivity()
                                    finish()
                                },
                                {
                                    defaultStartNextActivity()
                                    finish()
                                }
                            )
                        }
                    },
                    {
                        defaultStartNextActivity()
                        finish()
                    },
                )
            }

            DEEPLINK_MIRACLE -> {
                val miracleIntent = AlternateLinkFragmentActivity.createIntent(
                    this
                ).apply {
                    putExtra(BaseActivity.EXTRA_IS_MIRACLE, true)
                    putExtra(EXTRA_NEXT_ACTIVITY, MainActivity::class.java)
                    putExtra(EXTRA_TITLE, getString(R.string.miracle))
                    putExtra(EXTRA_LINK_STATUS, LinkStatus.MIRACLE.status)
                }
                mNextIntent = if (isActivityAlive()) {
                    miracleIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        miracleIntent,
                        MainActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_MY_FEED -> {

                val account = IdolAccount.getAccount(this)

                if (feedUserId != 0 && feedUserId != (account?.userModel?.id ?: return)) {
                    defaultStartNextActivity()
                    finish()
                    return
                }

                val myFeedIntent =
                    FeedActivity.createIntent(this, account?.userModel)

                mNextIntent = if (isActivityAlive()) {
                    myFeedIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        myFeedIntent,
                        FeedActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_MY_INFO -> {

                val myHeartInfoIntent =
                    MainActivity.createIntent(this, false).apply {
                        putExtra(EXTRA_IS_MY_HEART_INFO, true)
                        putExtra(EXTRA_NEXT_ACTIVITY, MainActivity::class.java)
                    }
                mNextIntent = if (isActivityAlive()) {
                    myHeartInfoIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        myHeartInfoIntent,
                        MainActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_ROOKIE -> {
                val rookieIntent = AlternateLinkFragmentActivity.createIntent(
                    this
                ).apply {
                    putExtra(EXTRA_IS_ROOKIE, true)
                    putExtra(EXTRA_NEXT_ACTIVITY, MainActivity::class.java)
                    putExtra(EXTRA_TITLE, getString(R.string.rookie))
                    putExtra(EXTRA_LINK_STATUS, LinkStatus.ROOKIE.status)
                }
                mNextIntent = if (isActivityAlive()) {
                    rookieIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        rookieIntent,
                        MainActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_INVITE -> {
                val mainIntent = MainActivity.createIntent(this, false).apply {
                    putExtra(EXTRA_NEXT_ACTIVITY, MainActivity::class.java)
                }

                mNextIntent = if (isActivityAlive()) {
                    mainIntent
                } else {
                    StartupActivity.createIntentForLink(
                        this,
                        mainIntent,
                        MainActivity::class.java,
                    )
                }
                startNextActivity()
                finish()
            }

            DEEPLINK_INVITE_SHARE -> {
                val account = IdolAccount.getAccount(this)

                if (account == null) {
                    logD("No account found, cannot proceed to invite share.")
                    defaultStartNextActivity()
                    return
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    val language = languagePreferenceRepository.getSystemLanguage()

                    usersRepository.getWebToken({ response ->
                        val success = response.optBoolean("success", false)
                        if (!success) {
                            defaultStartNextActivity()
                            return@getWebToken
                        }
                        val token =
                            response.optString("token").takeIf { it.isNotBlank() }

                        startActivity(Intent(this@AppLinkActivity, FriendInviteActivity::class.java).apply {
                            putExtra(FriendInviteActivity.INVITE_PAYLOAD,
                                InvitePayload(language, token ?: "")
                            )
                        })
                        finish()
                    }, { throwable ->
                        Toast.makeText(this@AppLinkActivity, throwable.message, Toast.LENGTH_SHORT).show()
                    })
                }
            }

            else -> {
                defaultStartNextActivity()
            }
        }
    }

    private inline fun <reified T : AppCompatActivity> getDefaultLinkIntent(): Intent =
        Intent(this, T::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

    private fun startNextActivity() {
        if (mNextIntent == null) {
            return
        }
        // 실행할 앱을 찾을 수 없는 경우 android.content.ActivityNotFoundException 크래시 방지
        if( mNextIntent?.resolveActivity(packageManager) == null) {
            val msg = getString(R.string.error_abnormal_default) + "[${ErrorControl.ERROR_START_ACTIVITY}]"
            Toast.makeText(
                this,
                msg,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        startActivity(mNextIntent)
        // 앱 종료 후 언어 변경이나 메모리부족으로 앱이 재시작되는 경우가 있음.
        mNextIntent = null // mHelper에서 여기를 또 불러서.. 다음번에 또 불리면 MainActivity가 또 실행되지 않게 처리
        Util.closeProgress()
    }

    private fun defaultStartNextActivity() {
        mNextIntent = StartupActivity.createIntent(this)
        startActivity(mNextIntent)
        mNextIntent = null
        Util.closeProgress()
    }

    private fun getAwardData(awardIntent: Intent) {
        MainScope().launch {
            awardsRepository.current(
                { response ->
                    try {
                        val json = response.optJSONObject("award")?.toString()
                        Util.setPreference(
                            this@AppLinkActivity,
                            Const.AWARD_MODEL,
                            json ?: throw Exception(),
                        )
                        mNextIntent = awardIntent
                        startNextActivity()
                        finish()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        defaultStartNextActivity()
                        finish()
                    }
                }, { throwable ->
                    defaultStartNextActivity()
                    finish()
                }
            )
        }
    }

    private fun setFirebaseAction() {

        val isPush = intent.getBooleanExtra(APP_LINK_PUSH_STATUS, false)

        if (!isPush) {
            return
        }

        val analyticsClickLabel = intent.getStringExtra(APP_LINK_PUSH_ANALYTICS_CLICK_LABEL)

        val label = analyticsClickLabel
            ?: when (kindOfDeepLink) {
                DEEPLINK_ARTICLE -> GaAction.PUSH_COMMENT.label
                DEEPLINK_FRIENDS -> GaAction.PUSH_FRIEND.label
                DEEPLINK_COUPON -> GaAction.PUSH_COUPON.label
                DEEPLINK_SCHEDULE -> GaAction.PUSH_SCHEDULE.label
                DEEPLINK_SUPPORT -> GaAction.PUSH_SUPPORT.label
                DEEPLINK_NOTICE -> GaAction.PUSH_NOTICE.label
                else -> return
            }

        setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION, label)
    }

    companion object {
        const val DEEPLINK_ARTICLE = "deeplink_article"
        const val DEEPLINK_COMMUNITY = "deeplink_community"
        const val DEEPLINK_COUPON = "deeplink_coupon"
        const val DEEPLINK_IDOL = "deeplink_idol"
        const val DEEPLINK_SUPPORT = "deeplink_support"
        const val DEEPLINK_HOF = "deeplink_HOF"
        const val DEEPLINK_MIRACLE = "deeplink_miracle"
        const val DEEPLINK_MY_FEED = "deeplink_my_feed"
        const val DEEPLINK_MY_INFO = "deeplink_my_info"
        const val DEEPLINK_SUPPORT_MAIN_STATUS = "deeplink_support_main_status"
        const val DEEPLINK_QUIZZES = "deeplink_quizzes"
        const val DEEPLINK_STORE = "deeplink_store"
        const val DEEPLINK_OFFERWALL = "deeplink_offerwall"
        const val DEEPLINK_BOARD = "deeplink_board"
        const val DEEPLINK_RECORDS = "deeplink_records"
        const val DEEPLINK_FACEDETECT = "deeplink_face_detect"
        const val DEEPLINK_NOTICE = "deeplink_notice"
        const val DEEPLINK_EVENT = "deeplink_event"
        const val DEEPLINK_MENU = "deeplink_menu"
        const val DEEPLINK_AWARD = "deeplink_award"
        const val DEEPLINK_HOTTRENDS = "deeplink_hottrends"
        const val DEEPLINK_BANNERGRAM = "deeplink_bannergram"
        const val DEEPLINK_THEME_PICK = "deeplink_theme_pick"
        const val DEEPLINK_LIVE_STREAMING = "deeplink_live_streaming"
        const val DEEPLINK_IMAGE_PICK = "deeplink_image_pick"
        const val DEEPLINK_FRIENDS = "deeplink_friends"
        const val DEEPLINK_ATTENDANCE = "deeplink_attendance"
        const val DEEPLINK_SCHEDULE = "deeplink_schedule"
        const val DEEPLINK_HEARTPICK = "deeplink_heartpick"
        const val DEEPLINK_ROOKIE = "deeplink_rookie"
        const val DEEPLINK_INVITE = "deeplink_invite"
        const val DEEPLINK_AUTH = "deeplink_auth"
        const val DEEPLINK_INVITE_SHARE = "deeplink_invite_share"

        const val APP_LINK_PUSH_STATUS = "app_link_push_status"
        const val APP_LINK_PUSH_ANALYTICS_CLICK_LABEL = "app_link_push_analytics_click_label"
    }
}