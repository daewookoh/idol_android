package net.ib.mn.activity

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.IdolApplication
import net.ib.mn.R
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.chatting.ChattingRoomActivity
import net.ib.mn.chatting.chatDb.ChatRoomList
import net.ib.mn.chatting.model.ChatRoomInfoModel
import net.ib.mn.chatting.model.ChatRoomListModel
import net.ib.mn.core.data.repository.ChatRepositoryImpl
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.feature.friend.FriendsActivity
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.ScheduleModel
import net.ib.mn.model.SupportListModel
import net.ib.mn.model.toPresentation
import net.ib.mn.support.SupportDetailActivity
import net.ib.mn.support.SupportPhotoCertifyActivity
import net.ib.mn.utils.Const
import net.ib.mn.utils.GlobalVariable
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class PushStartActivity : BaseActivity() {
    var idolModel: IdolModel? = null

    @Inject
    lateinit var chatRepository: ChatRepositoryImpl
    @Inject
    lateinit var supportRepository: SupportRepositoryImpl

    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var miscRepository: MiscRepository
    @Inject
    lateinit var articlesRepository: ArticlesRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_link)
        Util.showProgress(this)

        if (!IdolApplication.STRATUP_CALLED) {
            val intent = StartupActivity.createIntent(this@PushStartActivity)
            intent.setFlags(0) // 이렇게 해야 onActivityResult가 바로 RESULT_CANCELED로 호출되는게 방지됨
            intent.putExtra("go_push_start", true)

            startActivityForResult(intent, REQUEST_STARTUP_ACTIVITY)
            return
        }

        processPush()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val i = intent
        // StartupActivity에서 다시 넘어오면
        if (requestCode == REQUEST_STARTUP_ACTIVITY && resultCode == RESULT_OK) {
            processPush()
        }
    }

    override fun onStop() {
        FLAG_CLOSE_DIALOG = false
        super.onStop()
    }

    @OptIn(UnstableApi::class)
    private fun processPush() {
        val account = getAccount(this)

        if (account == null) {
            Util.log("PushStartActivity error1")
            makeText(
                this, getString(R.string.msg_error_ok),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        } else {
            val onSuccess = onSuccess@ {
                Util.closeProgress()
                if (!account.hasUserInfo()) {
                    Util.log("PushStartActivity error2")
                    makeText(
                        this@PushStartActivity,
                        getString(R.string.msg_error_ok),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val i = intent
                    if (i == null || TextUtils.isEmpty(i.action)) {
                        return@onSuccess
                    }
                    val action = i.action
                    v("check_log_push_message -> action 값  ->$action")

                    if (ACTION_COMMENTS == action) {
                        setUiActionFirebaseGoogleAnalyticsActivity(
                            Const.ANALYTICS_BUTTON_PRESS_ACTION,
                            "push_comment"
                        )
                        val model = i.getSerializableExtra(PARAM_ARTICLE) as ArticleModel?
                        if (model != null) {
                            lifecycleScope.launch {
                                articlesRepository.getArticle(
                                    "/api/v1/articles/" + model.id + "/",
                                    { response ->
                                        val article = getInstance(true).fromJson(
                                            response.toString(),
                                            ArticleModel::class.java
                                        )
                                        val adapterType = if (TextUtils.equals(
                                                article.type,
                                                "M"
                                            )
                                        ) NewCommentAdapter.TYPE_SMALL_TALK else NewCommentAdapter.TYPE_ARTICLE
                                        val intent = NewCommentActivity.createIntent(
                                            this@PushStartActivity,
                                            article,
                                            -1,
                                            false,
                                            adapterType
                                        )
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        startActivity(intent)
                                        finish()
                                    }, {
                                        makeText(
                                            this@PushStartActivity,
                                            R.string.error_abnormal_default,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        finish()
                                    }
                                )
                            }
                        } else {
                            makeText(
                                this@PushStartActivity,
                                R.string.error_abnormal_default,
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    } else if (ACTION_FRIEND == action) {
                        setUiActionFirebaseGoogleAnalyticsActivity(
                            Const.ANALYTICS_BUTTON_PRESS_ACTION,
                            "push_friend"
                        )
                        val intent = Intent(this@PushStartActivity, FriendsActivity::class.java)
                        intent.putExtra(EXTRA_IS_FROM_PUSH, true)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                        finish()
                    } else if (ACTION_COUPON == action) {
                        setUiActionFirebaseGoogleAnalyticsActivity(
                            Const.ANALYTICS_BUTTON_PRESS_ACTION,
                            "push_coupon"
                        )
                        val push = i.getIntExtra(EXTRA_COUPON, 0)
                        intent.putExtra(EXTRA_IS_FROM_PUSH, true)
                        val intent = MyCouponActivity.createIntent(this@PushStartActivity, push)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                        finish()
                    } else if (ACTION_SCHEDULE == action) {
                        setUiActionFirebaseGoogleAnalyticsActivity(
                            Const.ANALYTICS_BUTTON_PRESS_ACTION,
                            "push_schedule"
                        )
                        val idol_id = i.getIntExtra(EXTRA_IDOL_ID, -1)
                        if (idol_id == -1) {
                            makeText(
                                this@PushStartActivity,
                                R.string.error_abnormal_default,
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else {
                            lifecycleScope.launch {
                                idolsRepository.getIdolsForSearch(
                                    id = idol_id,
                                    listener = { response ->
                                        try {
                                            Util.log("check 로그-> " + response.getJSONArray("objects"))
                                            val idol = instance
                                                .fromJson(
                                                    response.getJSONArray("objects")
                                                        .getJSONObject(0)
                                                        .toString(), IdolModel::class.java
                                                )
                                            val intent = CommunityActivity.createIntent(
                                                this@PushStartActivity,
                                                idol,
                                                true,
                                                Const.PUSH_CHANNEL_ID_SCHEDULE
                                            )
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                            startActivity(intent)
                                        } catch (e: JSONException) {
                                            e.printStackTrace()
                                            makeText(
                                                this@PushStartActivity,
                                                R.string.error_abnormal_default,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        finish()
                                    },
                                    errorListener = {
                                        makeText(
                                            this@PushStartActivity,
                                            R.string.error_abnormal_default,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        finish()
                                    }
                                )
                            }
                        }
                    } else if (ACTION_SUPPORT == action) { //서포트  액션

                        // TODO: 2020/12/07  서포트 부분  여기까지  진행 .

                        setUiActionFirebaseGoogleAnalyticsActivity(
                            Const.ANALYTICS_BUTTON_PRESS_ACTION,
                            "push_support"
                        )

                        //서포트 아이디 받아옴.
                        val support_id = i.getIntExtra(EXTRA_SUPPORT_ID, -1)

                        val isCommentPush = i.getBooleanExtra(EXTRA_IS_COMMENT_PUSH, false)

                        //서포트 상태 분류 -> 0일때 상세 페이지,  1일때 인증샷으로
                        val status = i.getIntExtra(EXTRA_STATUS, -1)

                        if (support_id != -1 && status == 0) { //상세화면으로 가야됨.

                            val intent = SupportDetailActivity.createIntent(
                                this@PushStartActivity,
                                support_id
                            )
                            val intent1 =
                                Intent(this@PushStartActivity, MainActivity::class.java)
                            startActivities(arrayOf(intent1, intent))
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            startActivity(intent)
                            finish()
                        } else if (support_id != -1 && status == 1) { //status 1 이므로 인증샷 화면으로 가야됨.

                            //서포트 정보중  필요한 부분 받아오기
                            //기존  서포트 시작시  넘겨 받는 정보들이 있는데,  해당  정보들 같이 맞춰서
                            //한 createIntent로  통일하기 위해 여기서  한번  api call 진행.
                            lifecycleScope.launch {
                                supportRepository.getSupportDetail(
                                    support_id,
                                    { response ->
                                        try {
                                            val supportListModel = getInstance(true).fromJson(
                                                response.toString(),
                                                SupportListModel::class.java
                                            )
                                            val supportInfo = JSONObject()

                                            if (supportListModel.idol.getName(this@PushStartActivity)
                                                    .contains("_")
                                            ) {
                                                supportInfo.put(
                                                    "name", Util.nameSplit(
                                                        this@PushStartActivity,
                                                        supportListModel.idol
                                                    )[0]
                                                )
                                                supportInfo.put(
                                                    "group", Util.nameSplit(
                                                        this@PushStartActivity,
                                                        supportListModel.idol
                                                    )[1]
                                                )
                                            } else {
                                                supportInfo.put(
                                                    "name", supportListModel.idol.getName(
                                                        this@PushStartActivity
                                                    )
                                                )
                                            }
                                            supportInfo.put("support_id", supportListModel.id)
                                            supportInfo.put("title", supportListModel.title)
                                            supportInfo.put(
                                                "profile_img_url",
                                                supportListModel.image_url
                                            )
                                            val intent = if (isCommentPush) { //댓글 푸시일때
                                                SupportPhotoCertifyActivity.createIntent(
                                                    this@PushStartActivity,
                                                    supportInfo.toString(),
                                                    true
                                                )
                                            } else { //성공/개설 푸시일때
                                                SupportPhotoCertifyActivity.createIntent(
                                                    this@PushStartActivity,
                                                    supportInfo.toString(),
                                                    false
                                                )
                                            }

                                            val intent1 = Intent(
                                                this@PushStartActivity,
                                                MainActivity::class.java
                                            )
                                            startActivities(arrayOf(intent1, intent))
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                            startActivity(intent)
                                        } catch (e: Exception) {
                                            makeText(
                                                this@PushStartActivity,
                                                R.string.error_abnormal_default,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        finish()
                                    },
                                    { throwable ->
                                        makeText(
                                            this@PushStartActivity,
                                            R.string.error_abnormal_default,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        finish()
                                    }
                                )
                            }
                        } else {
                            //suppport_id나, status 중 한개라도 -1(default value가 나오면  에러 메세지  송출 한다.
                            makeText(
                                this@PushStartActivity,
                                R.string.error_abnormal_default,
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    } else if (ACTION_NOTICE == action) {
                        setUiActionFirebaseGoogleAnalyticsActivity(
                            Const.ANALYTICS_BUTTON_PRESS_ACTION,
                            "push_notice"
                        )
                        Log.d("@@@@", "fkwelfjlwkefj")
                        val intent = MainActivity.createIntent(this@PushStartActivity, false)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                        finish()
                    } else if (ACTION_SCHEDULE_COMMENT == action) { //스케쥴  댓글 푸시

                        //서포트 아이디 받아옴.

                        val schedule_id = i.getIntExtra(EXTRA_SCHEDULE_ID, -1)

                        lifecycleScope.launch {
                            miscRepository.getResource(
                                "schedules/$schedule_id/",
                                { response ->
                                    val scheduleModel = instance.fromJson(
                                        response.toString(),
                                        ScheduleModel::class.java
                                    )
                                    lifecycleScope.launch {
                                        miscRepository.getResource(
                                            "articles/" + scheduleModel.article_id + "/",
                                            { response1 ->
                                                Util.log("아이돌 이름 " + scheduleModel.idol_ids)
                                                val articleModel = instance.fromJson(
                                                    response1.toString(),
                                                    ArticleModel::class.java
                                                )
                                                val intent = NewCommentActivity.createIntent(
                                                    this@PushStartActivity,
                                                    articleModel,
                                                    -1,
                                                    scheduleModel,
                                                    true,
                                                    HashMap<Int, String>(),
                                                    true
                                                )
                                                val intent1 = Intent(
                                                    this@PushStartActivity,
                                                    MainActivity::class.java
                                                )
                                                startActivities(arrayOf(intent1, intent))
                                                intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)


                                                startActivity(intent)
                                                finish()
                                            }, {
                                                makeText(
                                                    this@PushStartActivity,
                                                    R.string.error_abnormal_default,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                finish()
                                            }
                                        )
                                    }
                                },
                                {
                                    makeText(
                                        this@PushStartActivity,
                                        R.string.error_abnormal_default,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()

                                }
                            )
                        }
                    } else if (ACTION_CHATTING == action) { //채팅 클릭시 이동 관련 로직 처리


                        Thread {
                            val chatRoomListModel =
                                ChatRoomList.getInstance(this@PushStartActivity)
                                    .getChatRoomPush(
                                        i.getIntExtra(
                                            EXTRA_ROOM_ID, 0
                                        )
                                    )
                            //채팅룸 데이터가  없는경우에는  api를 통해 불러온다.
                            if (chatRoomListModel != null) {
                                v("로컬 db에 룸 정보가 있을떄 실행 ")
                                var userId: Int
                                var idolModel: IdolModel?

                                lifecycleScope.launch(Dispatchers.IO) {
                                    userId = (chatRoomListModel.userId)!!
                                    idolModel = getIdolByIdUseCase(chatRoomListModel.idolId!!)
                                        .mapDataResource { it?.toPresentation() }
                                        .awaitOrThrow() ?: getAccount(this@PushStartActivity)!!.most

                                    withContext(Dispatchers.Main) {
                                        val intent = ChattingRoomActivity.createIntent(
                                            this@PushStartActivity,
                                            chatRoomListModel.roomId,
                                            chatRoomListModel.nickName,
                                            userId,
                                            chatRoomListModel.role,
                                            chatRoomListModel.isAnonymity,
                                            chatRoomListModel.title
                                        )

                                        val intent1 = CommunityActivity.createIntent(
                                            this@PushStartActivity,
                                            (idolModel)!!,
                                            true,
                                            Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW
                                        )
                                        val intent2 =
                                            Intent(this@PushStartActivity, MainActivity::class.java)


                                        val mngr =
                                            getSystemService(ACTIVITY_SERVICE) as ActivityManager
                                        var currentComponentName: ComponentName? = null

                                        //RunningTaskInfo 가 deprecate되었으므로 AppTask에서 찾도록 한다.
                                        //RunningTaskInfo 에서 launcherActivity가 API level 30이상에서는 Deprecate 되었으므로 해당 액티비티를 리턴을 못해서 앱이죽음(Android 12).
                                        val appTaskList = mngr.appTasks
                                        if (appTaskList != null) {
                                            currentComponentName =
                                                appTaskList[0].taskInfo.topActivity
                                        }

                                        //채팅방 화면이 테스크 상에서 맨위에 잇을때 바로 onresume만 실행되도록
                                        //pushstartactivity만  finish 처리 -> 이렇게하면,  그냥  기존 스택대로 진행한다.
                                        //채팅방에서 설정한 TOP_STACK_ROOM_ID가   roomid 가 현재 가려는  room id인 경우  실행한다.
                                        if (currentComponentName != null) {
                                            if ((currentComponentName!!.className == ChattingRoomActivity::class.java.name) && GlobalVariable.TOP_STACK_ROOM_ID == chatRoomListModel.roomId) {
                                                finish()
                                            } else {
                                                if ((currentComponentName!!.className == ChattingRoomActivity::class.java.name)) {
                                                    Const.IS_CHAT_ACTIVITY_FIRST_RUNNING = true
                                                    Const.OG_REQUEST_COUNT_CHECK = 0
                                                    GlobalVariable.TOP_STACK_ROOM_ID =
                                                        chatRoomListModel.roomId
                                                }

                                                //메인화면  이  항상 task 에 마지막 스택에 있으므로,  cleart top을 해주고  다시 커뮤랑  intent를 쌓아준다
                                                intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                startActivities(arrayOf(intent2, intent1, intent))
                                                finish()
                                            }
                                        } else {
                                            //메인화면  이  항상 task 에 마지막 스택에 있으므로,  cleart top을 해주고  다시 커뮤랑  intent를 쌓아준다
                                            intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            startActivities(arrayOf(intent2, intent1, intent))
                                            finish()
                                        }
                                    }
                                }
                            } else { //로컬 db에  해당 값이 없으므로, api 를 이용해서 값을 받아온다.

                                val pushLocale = i.getStringExtra(EXTRA_LOCALE)
                                val roomId = i.getIntExtra(EXTRA_ROOM_ID, -1)
                                val gson = instance
                                val mHandler = Handler(Looper.getMainLooper())


                                if (roomId != -1) { //아이돌 id,room id -1  (default 값 아닐때)

                                    //일단  채팅방 정보로   바당와서 idolid 를   받아온다.
                                    //푸시에도  idol id 값이 있지만,  로컬 푸시로 만들어지는 경우 idol id가 없으므로,
                                    //현재  로컬에  room id 만으로  조회할수 있는  채팅방 정보들이 없는 경우는 다음과 같이 진행한다.
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        chatRepository.getChatRoomInfo(
                                            roomId,
                                            { response ->
                                                var chatRoomInfoModel: ChatRoomInfoModel? = null
                                                try {
                                                    chatRoomInfoModel = gson.fromJson(
                                                        response.getJSONObject("room")
                                                            .toString(),
                                                        ChatRoomInfoModel::class.java
                                                    )
                                                } catch (e: JSONException) {
                                                    e.printStackTrace()
                                                }
                                                var idolId = i.getIntExtra(EXTRA_IDOL_ID, -1)
                                                if (idolId == -1) {
                                                    if (chatRoomInfoModel != null && chatRoomInfoModel.idolId != null) {
                                                        idolId = chatRoomInfoModel.idolId!!
                                                    }
                                                }

                                                val finalIdolId = idolId
                                                val finalChatRoomInfoModel = chatRoomInfoModel

                                                lifecycleScope.launch(Dispatchers.IO) {
                                                    var idol = getIdolByIdUseCase(idolId)
                                                        .mapDataResource { it?.toPresentation() }
                                                        .awaitOrThrow()

                                                    if (idol == null) {
                                                        idol =
                                                            getAccount(this@PushStartActivity)!!.most
                                                    }

                                                    var locale: String? = ""

                                                    if (pushLocale != null && !pushLocale.isEmpty()) {
                                                        locale = pushLocale
                                                    } else {
                                                        locale =
                                                            finalChatRoomInfoModel?.locale
                                                    }
                                                    val finalIdol = idol
                                                    MainScope().launch {
                                                        chatRepository.getChatRoomJoinList(
                                                            finalIdolId,
                                                            locale,
                                                            0,
                                                            300,
                                                            0,
                                                            { response ->
                                                                if (response.optBoolean("success")) {
                                                                    val chatRoomJoinedArray =
                                                                        response.optJSONArray(
                                                                            "objects"
                                                                        )
                                                                    var myJoinedRoom =
                                                                        ChatRoomListModel()

                                                                    val chatRoomJoinedLength =
                                                                        if (chatRoomJoinedArray != null) chatRoomJoinedArray.length() - 1 else 0
                                                                    for (j in 0..chatRoomJoinedLength) {
                                                                        try {
                                                                            val myJoinRoomObject =
                                                                                chatRoomJoinedArray!!.getJSONObject(
                                                                                    j
                                                                                )
                                                                            if (myJoinRoomObject.getInt(
                                                                                    "id"
                                                                                ) == roomId
                                                                            ) { // room id가 푸시에서 가려는 roomd id가 같을때  myjoinroom 정보를 쓰기위해  for 문을 break 한다.
                                                                                myJoinedRoom =
                                                                                    gson.fromJson(
                                                                                        myJoinRoomObject.toString(),
                                                                                        ChatRoomListModel::class.java
                                                                                    )
                                                                                myJoinedRoom.accountId =
                                                                                    account.userId
                                                                                break
                                                                            }
                                                                        } catch (e: Exception) {
                                                                            e.printStackTrace()
                                                                        }
                                                                    }

                                                                    val intent =
                                                                        ChattingRoomActivity.createIntent(
                                                                            this@PushStartActivity,
                                                                            roomId,
                                                                            myJoinedRoom.nickName,
                                                                            myJoinedRoom.userId,
                                                                            myJoinedRoom.role,
                                                                            myJoinedRoom.isAnonymity,
                                                                            myJoinedRoom.title
                                                                        )

                                                                    val intent1 =
                                                                        CommunityActivity.createIntent(
                                                                            this@PushStartActivity,
                                                                            (finalIdol)!!,
                                                                            true,
                                                                            Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW
                                                                        )
                                                                    val intent2 = Intent(
                                                                        this@PushStartActivity,
                                                                        MainActivity::class.java
                                                                    )
                                                                    //
                                                                    val mngr =
                                                                        getSystemService(
                                                                            ACTIVITY_SERVICE
                                                                        ) as ActivityManager
                                                                    var currentComponentName: ComponentName? =
                                                                        null

                                                                    //RunningTaskInfo 가 deprecate되었으므로 AppTask에서 찾도록 한다.
                                                                    //RunningTaskInfo 에서 launcherActivity가 API level 30이상에서는 Deprecate 되었으므로 해당 액티비티를 리턴을 못해서 앱이죽음(Android 12).
                                                                    val appTaskList =
                                                                        mngr.appTasks
                                                                    if (appTaskList != null) {
                                                                        currentComponentName =
                                                                            appTaskList[0].taskInfo.topActivity
                                                                    }

                                                                    //채팅방 화면이 테스크 상에서 맨위에 잇을때 바로 onresume만 실행되도록
                                                                    //pushstartactivity만  finish 처리 -> 이렇게하면,  그냥  기존 스택대로 진행한다.
                                                                    //채팅방에서 설정한 TOP_STACK_ROOM_ID가   roomid 가 현재 가려는  room id인 경우  실행한다.
                                                                    if (currentComponentName != null) {
                                                                        if ((currentComponentName!!.className == ChattingRoomActivity::class.java.name) && GlobalVariable.TOP_STACK_ROOM_ID == chatRoomListModel?.roomId) {
                                                                            finish()
                                                                        } else {
                                                                            if ((currentComponentName!!.className == ChattingRoomActivity::class.java.name)) {
                                                                                Const.IS_CHAT_ACTIVITY_FIRST_RUNNING =
                                                                                    true
                                                                                Const.OG_REQUEST_COUNT_CHECK =
                                                                                    0
//                                                                                        GlobalVariable.TOP_STACK_ROOM_ID = chatRoomListModel!!.roomId // 여기 코드가 이상해서 나중에 확인 필요. 위쪽 GlobalVariable.TOP_STACK_ROOM_ID == chatRoomListModel?.roomId 도 문제있음
                                                                            }

                                                                            //메인화면  이  항상 task 에 마지막 스택에 있으므로,  cleart top을 해주고  다시 커뮤랑  intent를 쌓아준다
                                                                            intent2.addFlags(
                                                                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                                            )
                                                                            intent2.addFlags(
                                                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                                                            )
                                                                            startActivities(
                                                                                arrayOf(
                                                                                    intent2,
                                                                                    intent1,
                                                                                    intent
                                                                                )
                                                                            )
                                                                        }
                                                                    } else {
                                                                        //메인화면  이  항상 task 에 마지막 스택에 있으므로,  cleart top을 해주고  다시 커뮤랑  intent를 쌓아준다
                                                                        intent2.addFlags(
                                                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                                        )
                                                                        intent2.addFlags(
                                                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                                                        )
                                                                        startActivities(
                                                                            arrayOf(
                                                                                intent2,
                                                                                intent1,
                                                                                intent
                                                                            )
                                                                        )
                                                                    }
                                                                } else {
                                                                    UtilK.handleCommonError(
                                                                        this@PushStartActivity,
                                                                        response
                                                                    )
                                                                }
                                                                finish()
                                                            }, { throwable ->
                                                                mHandler.postDelayed(
                                                                    Runnable {
                                                                        makeText(
                                                                            this@PushStartActivity,
                                                                            R.string.error_abnormal_default,
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    },
                                                                    0
                                                                )
                                                                finish()

                                                            })
                                                    }
                                                }
                                            },
                                            { throwable ->
                                                mHandler.postDelayed(object : Runnable {
                                                    override fun run() {
                                                        makeText(
                                                            this@PushStartActivity,
                                                            R.string.error_abnormal_default,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }, 0)
                                                finish()

                                            }
                                        )
                                    }
                                } else { //idol id 가져온값이  -1 defult값이면  error toast 띄움.
                                    mHandler.postDelayed(object : Runnable {
                                        override fun run() {
                                            makeText(
                                                this@PushStartActivity,
                                                R.string.error_abnormal_default,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }, 0)
                                    finish()
                                }
                            } //로컬 db 채팅방 값  null 일 경우
                        }.start()
                    }
                }
            }

            val onFailure = { msg: String? ->
                    Util.closeProgress()
                    // 점검중 팝업 유지
                    if(!msg.equals(Const.MSG_UNDER_MAINTENANCE)) {
                        Util.log("PushStartActivity error3")
                        makeText(
                            this@PushStartActivity,
                            getString(R.string.msg_error_ok),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }

            accountManager.fetchUserInfo(this, onSuccess, onFailure)
        }
    }


    companion object {
        private const val ACTION_COMMENTS = "action.comments"
        private const val PARAM_ARTICLE = "article"
        private const val ACTION_FRIEND = "action.friend"
        private const val ACTION_COUPON = "action.coupon"
        private const val ACTION_SCHEDULE = "action.schedule"
        private const val ACTION_SCHEDULE_COMMENT = "action.schedule_comment"
        private const val ACTION_SUPPORT = "action.support"
        private const val ACTION_CHATTING = "action.chatting"
        private const val ACTION_NOTICE = "action.notice"
        const val EXTRA_IDOL_ID: String = "idol_id"
        const val EXTRA_SCHEDULE_ID: String = "schedule_id"
        const val EXTRA_SUPPORT_ID: String = "support_id"
        const val EXTRA_STATUS: String = "status"
        const val EXTRA_IS_COMMENT_PUSH: String = "isCommentPush"
        const val EXTRA_ROOM_ID: String = "room_id"
        const val EXTRA_CHATTING_MSG: String = "chatting_msg"
        const val EXTRA_LOCALE: String = "locale"
        const val EXTRA_COUPON: String = "coupon"

        private const val REQUEST_STARTUP_ACTIVITY = 100

        fun createCommentsIntent(context: Context?, model: ArticleModel?): Intent {
            val intent = Intent(context, PushStartActivity::class.java)
            intent.setAction(ACTION_COMMENTS)
            intent.putExtra(PARAM_ARTICLE, model)
            return intent
        }

        fun createFriendIntent(context: Context?): Intent {
            val intent = Intent(context, PushStartActivity::class.java)
            intent.setAction(ACTION_FRIEND)
            return intent
        }

        // 스케쥴 댓글 처리
        fun createScheduleCommentIntent(context: Context?, schedule_id: Int, idol_id: Int): Intent {
            val intent = Intent(context, PushStartActivity::class.java)
            intent.setAction(ACTION_SCHEDULE_COMMENT)
            intent.putExtra(EXTRA_IDOL_ID, idol_id)
            intent.putExtra(EXTRA_SCHEDULE_ID, schedule_id)
            return intent
        }

        //서포트는  개설 , 성공  및  isCommentPush로   댓글 푸시 관련 처리를 진행한다.
        fun createSupportIntent(
            context: Context?,
            support_id: Int,
            status: Int,
            isCommentPush: Boolean
        ): Intent {
            val intent = Intent(context, PushStartActivity::class.java)
            intent.setAction(ACTION_SUPPORT)
            intent.putExtra(EXTRA_SUPPORT_ID, support_id)
            intent.putExtra(EXTRA_STATUS, status)
            intent.putExtra(
                EXTRA_IS_COMMENT_PUSH,
                isCommentPush
            ) //댓글 언급 푸시(true) 인지  성공/개설 푸시(false)인지를  구별한다.
            return intent
        }

        //해당 채팅방 노티 클릭시  채팅방으로 이동하기 위한 intent 설정
        fun createChattingIntent(
            context: Context?,
            chatMsgData: JSONObject,
            roomId: Int,
            idolId: Int,
            pushLocale: String?
        ): Intent {
            val intent = Intent(context, PushStartActivity::class.java)
            intent.setAction(ACTION_CHATTING)
            intent.putExtra(EXTRA_ROOM_ID, roomId)
            intent.putExtra(EXTRA_CHATTING_MSG, chatMsgData.toString())
            intent.putExtra(EXTRA_IDOL_ID, idolId)
            intent.putExtra(EXTRA_LOCALE, pushLocale)

            return intent
        }

        fun createMainIntent(context: Context?): Intent {
            val intent = Intent(context, PushStartActivity::class.java)
            intent.setAction(ACTION_NOTICE)
            return intent
        }

        fun createCouponIntent(context: Context?): Intent {
            val intent = Intent(context, PushStartActivity::class.java)
            intent.setAction(ACTION_COUPON)
            intent.putExtra(EXTRA_COUPON, 1)
            return intent
        }

        fun createScheduleIntent(context: Context?, idol_id: Int): Intent {
            val intent = Intent(context, PushStartActivity::class.java)
            intent.setAction(ACTION_SCHEDULE)
            intent.putExtra(EXTRA_IDOL_ID, idol_id)
            return intent
        }

        fun createPushStartIntent(context: Context?): Intent {
            val intent = Intent(context, PushStartActivity::class.java)
            return intent
        }
    }
}
