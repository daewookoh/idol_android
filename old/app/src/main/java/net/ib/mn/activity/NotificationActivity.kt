package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.adapter.NotificationAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.repository.MessagesRepositoryImpl
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.databinding.ActivityNotificationBinding
import net.ib.mn.feature.friend.FriendsActivity
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.MessageModel
import net.ib.mn.model.ScheduleModel
import net.ib.mn.model.SupportListModel
import net.ib.mn.support.SupportPhotoCertifyActivity.Companion.createIntent
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.NotificationViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActivity : BaseActivity(), View.OnClickListener, NotificationAdapter.OnClickListener {

	private lateinit var mGlideRequestManager: RequestManager
	private var notificationAdapter: NotificationAdapter? = null
	var logs = ArrayList<MessageModel>()

	//타입별  정리
	private val typePushFriends = "f"//친구 공지
	private val typePushArticleComments ="ac"//일반 댓글
	private val typePushSupportComments ="tc"//서포트 댓글
	private val typePushScheduleComments ="sc"//스케쥴 댓글

    private lateinit var binding: ActivityNotificationBinding

    private val viewModel: NotificationViewModel by viewModels()

    @Inject
    lateinit var supportRepository: SupportRepositoryImpl
    @Inject
    lateinit var messagesRepository: MessagesRepositoryImpl
    @Inject
    lateinit var sharedAppState: SharedAppState
    @Inject
    lateinit var miscRepository: MiscRepository

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_notification)
        binding.clNotification.applySystemBarInsets()

		supportActionBar?.setTitle(R.string.title_notifications)

		mGlideRequestManager = Glide.with(this)
		notificationAdapter = NotificationAdapter(this, mGlideRequestManager,this, logs)
        binding.list.adapter = notificationAdapter
		val llm = LinearLayoutManager(this)
        binding.list.layoutManager = llm

        viewModel.deleteOldNotification()

        observeViewModel()
        observeState()

        with(binding) {
            binding.incitePush.clIncitePush.setOnClickListener {
                UtilK.openNotificationSettings(this@NotificationActivity)
            }
            binding.incitePush.ivClose.setOnClickListener {
                // 한 번 닫으면 앱 재실행까지 다시 노출 안함
                sharedAppState.setIncitePushHidden(true)
            }
        }
        tryShowIncitePush()
    }

	override fun onContextItemSelected(item: MenuItem): Boolean {
		val menuItemId = item?.itemId
		val lastClickedMessagePosition = notificationAdapter?.getClickedPosition()!!
		val log = notificationAdapter?.getItem(lastClickedMessagePosition)!!
		when (menuItemId) {
			MENU_DELETE -> {
				deleteMessage(log.id)
			}
		}
		return super.onContextItemSelected(item)
	}

    private fun observeState() {
        lifecycleScope.launch {
            sharedAppState.isIncitePushHidden.collect {
                tryShowIncitePush()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.completeDeleteOldNotification.observe(this, SingleEventObserver {
            getMessages()
        })

        viewModel.completeDeleteNotification.observe(this, SingleEventObserver {
            notificationAdapter?.notifyDataSetChanged()

            if( logs.size == 0 ) {
                binding.empty.visibility = View.VISIBLE
            }
        })

        viewModel.completeDeleteAllNotifications.observe(this, SingleEventObserver {
            logs.clear()
            notificationAdapter?.notifyDataSetChanged()
            binding.empty.visibility = View.VISIBLE
        })

        viewModel.notificationList.observe(this, SingleEventObserver {
            var utcDate:String? =null
            logs.clear()

            //로컬에 저장된 가장 최신 푸시의 create at 값
            utcDate=Util.getPreference(this,Const.KEY_RECENT_NOTIFICATION_CREATE_DATE)

            if (it != null) {
                logs.addAll(it)
            }
            lifecycleScope.launch(Dispatchers.Main) {
                messagesRepository.get(
                    "P",
                    utcDate,
                    { response ->
                        if (response.optBoolean("success")) {
                            val gson = IdolGson.getInstance()
                            val listType = object : TypeToken<List<MessageModel>>() {}.type
                            val pushlogs = gson.fromJson<List<MessageModel>>(
                                response.optJSONArray("objects").toString(), listType
                            )

                            //푸시 리스트가  1개 이상 있을떄 받아온  데이터중 최신 푸시의  create at을 캐싱 하여,
                            //다음  서버 요청떄  사용한다.
                            if(pushlogs.isNotEmpty()){

                                try {
                                    //아랍어 섞여오는것 현상 있어서  Locale.US적용  -> 아래  링크에서  Be wary of the default locale 확인
                                    //https://developer.android.com/reference/java/util/Locale#available_locales
                                    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US)
                                    formatter.timeZone = TimeZone.getTimeZone("UTC")

                                    //recent 값의  createAt을  캐싱함.
                                    Util.setPreference(this@NotificationActivity,
                                        Const.KEY_RECENT_NOTIFICATION_CREATE_DATE, formatter.format(pushlogs?.get(0)?.createdAt))

                                }catch (e:Exception){
                                    e.printStackTrace()
                                }
                            }


                            //로컬에서 가져온거보다 최근 푸시들이므로,  index 0부터  추가 시켜줌.
                            logs.addAll(0,pushlogs)

                            //로컬에 저장
                            viewModel.saveNotifications(logs)


                            //로컬 및 서버에서 가져온 총 로그들이  비어있으면,  empty view 보여준다.
                            if (logs.isNotEmpty()) {
                                binding.empty.visibility = View.GONE
                            } else {
                                binding.empty.visibility = View.VISIBLE
                            }

                            // 메뉴 상단 안읽은 알림 처리
                            sharedAppState.setUnreadNotification(false)
                        } else {
                            UtilK.handleCommonError(this@NotificationActivity, response)
                        }
                    }, { throwable ->
                        Toast.makeText(
                            this@NotificationActivity,
                            R.string.error_abnormal_default,
                            Toast.LENGTH_SHORT
                        ).show()
                    })
            }
        })

        viewModel.saveNotifications.observe(this, SingleEventObserver {
            notificationAdapter?.notifyDataSetChanged()
        })
    }

    fun tryShowIncitePush() {
        // OS 알림 off시 상시 출력
        var show = !NotificationManagerCompat.from(this@NotificationActivity).areNotificationsEnabled()
        // x 눌러 닫은 경우
        if(sharedAppState.isIncitePushHidden.value == true) {
            show = false
        }

        binding.incitePush.clIncitePush.visibility = if(show) View.VISIBLE else View.GONE
        binding.incitePushContainer.visibility = if(show) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        // 푸시 설정하고 돌아오면 설정 유도 사라지게
        tryShowIncitePush()
    }


    override fun onItemClick() {
		val lastClickedMessagePosition = notificationAdapter?.getClickedPosition()!!
        val log = notificationAdapter?.getItem(lastClickedMessagePosition)?.apply {
            if (link?.contains(ServerUrl.HOST_TEST) == true) {
                link = link?.replace(ServerUrl.HOST_TEST, ServerUrl.HOST_BBB_TEST)
            }
        }!!

		when(log.extraType){
			typePushArticleComments -> {
				setUiActionFirebaseGoogleAnalyticsActivity(
					GaAction.PUSHLOG_COMMENT.actionValue,
					GaAction.PUSHLOG_COMMENT.label
				)
			}
			typePushFriends -> {
				setUiActionFirebaseGoogleAnalyticsActivity(
					GaAction.PUSHLOG_FRIEND.actionValue,
					GaAction.PUSHLOG_FRIEND.label
				)
			}

			typePushScheduleComments -> {
				setUiActionFirebaseGoogleAnalyticsActivity(
					GaAction.PUSHLOG_SCHEDULE.actionValue,
					GaAction.PUSHLOG_SCHEDULE.label
				)
			}

			typePushSupportComments -> {
				setUiActionFirebaseGoogleAnalyticsActivity(
					GaAction.PUSHLOG_SUPPORT.actionValue,
					GaAction.PUSHLOG_SUPPORT.label
				)
			}
		}

        // 링크가 존재 할경우 링크 화면으로 보내줌. 아니면 원래 로직 실행.
        if (!log.link.isNullOrEmpty()) {
            startActivity(
                Intent(this, AppLinkActivity::class.java).apply {
                    data = Uri.parse(log.link)
                }
            )
            return
        }

        getLegacyResource(log)
	}

	override fun onClick(v: View?) {
		when (v?.id){

		}
	}

	//각 메세지 별로 지우기
	fun deleteMessage(index: Long){
        MainScope().launch {
            messagesRepository.delete(
                index.toInt(),
                { response ->
                    if (response.optBoolean("success")) {
                        Toast.makeText(this@NotificationActivity, R.string.tiele_friend_delete_result, Toast.LENGTH_SHORT).show()
                        var removeItem : MessageModel? = null
                        logs.forEach{
                            if(it.id==index) removeItem = it
                        }
                        if(removeItem != null) logs.remove(removeItem!!)

                        //삭제되었으므로 해당 id를 가지고 있는 노티를 로컬db 에서 지워준다. -> 그다음 뷰 업데이트
                        viewModel.deleteNotification(index)
                    }
                    else Toast.makeText(this@NotificationActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()                },
                { throwable ->
                    Toast.makeText(this@NotificationActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                }
            )
        }
	}

	//전체 알림 삭제
	fun deleteMessageByType(type: String){
        MainScope().launch {
            messagesRepository.deleteByType(
                type,
                { response ->
                    if (response.optBoolean("success")) {
                        Toast.makeText(this@NotificationActivity, R.string.tiele_friend_delete_result, Toast.LENGTH_SHORT).show()

                        //로컬에 저장된 푸시 로그들을 전부 삭제함. -> 그다음에 뷰 업데이트
                        viewModel.deleteAllNotifications()
                    }
                    else Toast.makeText(this@NotificationActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                },
                { throwable ->
                    Toast.makeText(this@NotificationActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                }
            )
        }
	}

	fun getMessages(){
		//로겈에 캐싱된 푸시로그들을 먼저 가져오고 그후에 서버로  푸시를  호출 한다.
        viewModel.getAllNotificationList()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.notification_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_trash -> {
				if(logs.isNotEmpty()) {
                    Util.showDefaultIdolDialogWithBtn2(this, null, getString(R.string.label_delete_all_notifications), {
                        deleteMessageByType("P")
                        Util.closeIdolDialog()
                    }, {
                        Util.closeIdolDialog()
                    })
                }
				return true
			}
            R.id.action_setting -> {
                startActivity(SettingPushActivity.createIntent(this))
                return true
            }
			else -> {
				return super.onOptionsItemSelected(item)
			}

		}
	}

    private fun getLegacyResource(log: MessageModel) {
        when (log.extraType) {
            typePushArticleComments -> {
                lifecycleScope.launch {
                    miscRepository.getResource(
                        "articles/" + log.extraId + "/",
                        { response ->
                            if (!response.optBoolean("success")) {
                                return@getResource
                            }

                            // article은 KST로 온다
                            val article = IdolGson.getInstance(true).fromJson(
                                response.toString(), ArticleModel::class.java
                            )
                            val adapterType = NewCommentAdapter.TYPE_ARTICLE
                            startActivity(
                                NewCommentActivity.createIntent(
                                    this@NotificationActivity, article, -1, false, adapterType
                                )
                            )
                        },
                        {
                            Toast.makeText(
                                this@NotificationActivity,
                                R.string.error_abnormal_default,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            typePushFriends -> {
                startActivity(FriendsActivity.createIntent(this))
            }

            typePushScheduleComments -> {
                //스케쥴  댓글 푸시  알림  클릭시  처리
                lifecycleScope.launch {
                    miscRepository.getResource(
                        "schedules/${log.extraId}/",
                        { response ->
                            val scheduleModel = IdolGson.getInstance()
                                .fromJson(response.toString(), ScheduleModel::class.java)
                            lifecycleScope.launch {
                                miscRepository.getResource(
                                    "articles/" + scheduleModel.article_id + "/",
                                    { response1 ->
                                        val articleModel = IdolGson.getInstance()
                                            .fromJson(response1.toString(), ArticleModel::class.java)

                                        val intent = NewCommentActivity.createIntent(
                                            this@NotificationActivity,
                                            articleModel,
                                            -1,
                                            scheduleModel,
                                            true,
                                            HashMap<Int, String>(),
                                            true
                                        )

                                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        startActivity(intent)
                                    },
                                    {
                                        Toast.makeText(
                                            this@NotificationActivity, R.string.error_abnormal_default, Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        },
                        {
                            Toast.makeText(
                                this@NotificationActivity, R.string.error_abnormal_default, Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }


            typePushSupportComments -> {
                //서포트 정보중  필요한 부분 받아오기
                //기존  서포트 시작시  넘겨 받는 정보들이 있는데,  해당  정보들 같이 맞춰서
                //한 createIntent로  통일하기 위해 여기서  한번  api call 진행.
                MainScope().launch {
                    supportRepository.getSupportDetail(log.extraId,
                        { response ->
                            try {
                                val (_, _, _, _, _, _, id, _, idol, image_url, _, _, title) = IdolGson.getInstance(
                                    true
                                ).fromJson(
                                    response.toString(), SupportListModel::class.java
                                )
                                val supportInfo = JSONObject()

                                supportInfo.put(
                                    "name", Util.nameSplit(this@NotificationActivity, idol)[0]
                                )
                                supportInfo.put(
                                    "group", Util.nameSplit(this@NotificationActivity, idol)[1]
                                )

                                supportInfo.put("support_id", id)
                                supportInfo.put("title", title)
                                supportInfo.put("profile_img_url", image_url)
                                val intent: Intent
                                intent = createIntent(
                                    this@NotificationActivity, supportInfo.toString(), true
                                )
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@NotificationActivity, R.string.error_abnormal_default, Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        { throwable ->
                            Toast.makeText(
                                this@NotificationActivity, R.string.error_abnormal_default, Toast.LENGTH_SHORT
                            ).show()

                        }
                    )
                }
            }
        }
    }

	companion object {

		const val MENU_DELETE = 1

		@JvmStatic
		fun createIntent(context: Context): Intent {
			return Intent(context, NotificationActivity::class.java)
		}
	}
}
