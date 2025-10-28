package net.ib.mn.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.core.designsystem.util.noRippleClickable
import net.ib.mn.core.model.HelpInfosModel
import net.ib.mn.databinding.ActionBarTitleAndImageBinding
import net.ib.mn.databinding.ActivityComposeBinding
import net.ib.mn.dialog.DefaultDialog
import net.ib.mn.dialog.NotificationSettingDialog
import net.ib.mn.dialog.VoteNotifyToastFragment
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.model.HeartPickModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.NetworkImage
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.formatDateForDisplay
import net.ib.mn.utils.getModelFromPref
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.parseDateStringToMillis
import net.ib.mn.utils.setFirebaseUIAction
import net.ib.mn.utils.setUiActionFirebaseGoogleAnalyticsActivity
import net.ib.mn.utils.trimNewlineWhiteSpace
import net.ib.mn.viewmodel.HeartPickPrelaunchViewModel
import java.text.NumberFormat

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class HeartPickPrelaunchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComposeBinding
    private val viewModel: HeartPickPrelaunchViewModel by viewModels()
    private lateinit var startActivityResultLauncher: ActivityResultLauncher<Intent>

    private var heartPickId: Int = 0
    var comebackFromSetting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        binding = ActivityComposeBinding.inflate(layoutInflater)
        binding.clContainer.applySystemBarInsets()

        if (intent.hasExtra(EXTRA_HEART_PICK_MODEL)) {
            val heartPickModel: HeartPickModel? = intent.getSerializableExtra(EXTRA_HEART_PICK_MODEL) as? HeartPickModel
            heartPickModel?.let {
                heartPickId = it.id
                viewModel.setHeartPick(it)
            }
        }

        if (intent.hasExtra(HEART_PICK_ID)) {
            heartPickId = savedInstanceState?.getInt(HEART_PICK_ID)
                ?: intent.getIntExtra(HEART_PICK_ID, 0)
        }

        setContentView(binding.root)
        setActionbar()
        setActivityResultLauncher()
        observedVM()

        binding.cvContainer.setContent {
            HeartPickPrelaunchScreen(
                heartPickId = heartPickId,
                viewModel = viewModel
            ) {
                launchCommentActivity(it)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(HEART_PICK_ID, heartPickId)
    }

    override fun onResume() {
        super.onResume()

        viewModel.updateMostId(this)

        if (comebackFromSetting) {
            comebackFromSetting = false

            if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                viewModel.postHeartPickSettingNotification(heartPickId)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }

            R.id.btn_share -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    context = this,
                    GaAction.HEART_PICK_PRELAUNCH_SHARE.actionValue,
                    GaAction.HEART_PICK_PRELAUNCH_SHARE.label
                )
                shareHeartPick(this, viewModel.heartPick.value ?: return false, viewModel.mostId.value ?: -1)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observedVM() = with(viewModel) {
        isNotifyToast.observe(this@HeartPickPrelaunchActivity, SingleEventObserver {
            VoteNotifyToastFragment().show(supportFragmentManager, "VoteNotifyToast")
        })
    }

    @SuppressLint("StringFormatMatches")
    private fun shareHeartPick(context:Context, heartPickModel: HeartPickModel, mostId: Int = -1){

        if(heartPickModel.heartPickIdols.isNullOrEmpty()) return

        // mostId 에 해당하는 아이돌이 있으면 title/subtitle, 없으면 기본값
        val existMost = heartPickModel.heartPickIdols.firstOrNull { it.idol_id == mostId }
        val mostText = existMost?.title
            ?: context.getString(R.string.share_onepick_upcoming_nobias)
        val groupText = existMost?.subtitle.orEmpty()

        // prize 이름이 null 이면 빈 문자열로
        val prizeName = heartPickModel.prize?.name.orEmpty()

        // 딥링크 URL 생성
        val params = listOf(LinkStatus.HEARTPICK.status, heartPickModel.id.toString())
        val url = LinkUtil.getAppLinkUrl(context = context, params = params)

        val msg = context.getString(
            R.string.share_heartpick_upcoming,
            heartPickModel.title,
            prizeName,
            mostText,
            groupText,
            url
        ).trimNewlineWhiteSpace()

        UtilK.linkStart(context, url = "", msg = msg.trimNewlineWhiteSpace())
    }

    private fun setActivityResultLauncher() {
        startActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            try {
                if (it.resultCode == ResultCode.COMMENTED.value || it.resultCode == ResultCode.COMMENT_REMOVED.value) {
                    val heartPickId = it.data?.getIntExtra("heartPickId", 0)
                    val commentCount = it.data?.getIntExtra(Const.COMMENT_COUNT, 0) ?: 0
                    heartPickId?.let { id ->
                        viewModel.getHeartPick(id)
                        viewModel.updateCommentCount(commentCount)
                        viewModel.getHeartPickSettingNotification(id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setActionbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val customActionView: ActionBarTitleAndImageBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.action_bar_title_and_image,
            null,
            false
        )

        customActionView.tvActionBarTitle.text = getString(R.string.vote_preview)

        val helpInfoModel =
            Util.getPreference(this, Const.PREF_HELP_INFO).getModelFromPref<HelpInfosModel>()
        customActionView.ivActionBarInfo.setOnClickListener {
            DefaultDialog(
                title = getString(R.string.popup_title_heartpick),
                subTitle = helpInfoModel?.heartPick,
                context = this,
                theme = android.R.style.Theme_Translucent_NoTitleBar
            ).show()
        }
        supportActionBar?.customView = customActionView.root
    }

    private fun launchCommentActivity(id: Int) {
        val intent = CommentOnlyActivity.createIntent(this@HeartPickPrelaunchActivity, id)
        startActivityResultLauncher.launch(intent)
    }

    companion object {
        const val HEART_PICK_ID = "heartPickId"
        const val EXTRA_HEART_PICK_MODEL = "extra_heart_pick_model"
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun HeartPickPrelaunchScreen(
    viewModel: HeartPickPrelaunchViewModel = hiltViewModel(),
    heartPickId: Int,
    openCommentScreen: (Int) -> Unit = { _ -> },
) {
    val context = LocalContext.current

    val heartPickModel by viewModel.heartPick.collectAsState()
    val mostId by viewModel.mostId.collectAsState()
    val idolEvent by viewModel.idol.collectAsState()
    val isNotify by viewModel.isNotify.collectAsState()

    idolEvent?.getContentIfNotHandled()?.let { idol ->
        startActivity(
            context, CommunityActivity.createIntent(
                context,
                idol
            ), null
        )
    }

    LaunchedEffect(Unit) {
        viewModel.getHeartPick(heartPickId)
        viewModel.getHeartPickSettingNotification(heartPickId)
    }

    if (heartPickModel == null) {
        // Show loading or empty state
        return
    } else {
        Column(
            modifier = Modifier
                .background(Color(ContextCompat.getColor(context, R.color.background_200)))
        ) {
            HeartPickPrelaunchBanner(
                context = context,
                bannerUrl = heartPickModel?.bannerUrl ?: "",
                title = heartPickModel?.title ?: "",
                subtitle = heartPickModel?.subtitle ?: ""
            )
            HeartPickPrelaunchContent(
                context = context,
                heartPickModel = heartPickModel ?: HeartPickModel(),
                mostId = mostId,
                isNotify = isNotify,
                idolClick = {
                    viewModel.getIdolById(it)
                },
                openNotificationSetting = {
                    val isNotificationOn =
                        NotificationManagerCompat.from(context).areNotificationsEnabled()

                    if (!isNotificationOn) {
                        val activity = context as? FragmentActivity
                        activity?.let {
                            val activity = context as? HeartPickPrelaunchActivity
                            val dialog = NotificationSettingDialog() {
                                activity?.comebackFromSetting = true
                            }
                            dialog.show(it.supportFragmentManager, "notification_setting_dialog")
                        }
                    } else {
                        viewModel.postHeartPickSettingNotification(heartPickId)
                    }
                },
                openCommentScreen = { heartPickId ->
                    openCommentScreen(heartPickId)
                }
            )
        }
    }
}

// Color(ContextCompat.getColor(context, R.color.subscribe_label))
@Composable
fun HeartPickPrelaunchBanner(context: Context, bannerUrl: String, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .defaultMinSize(minHeight = 126.dp)
    ) {
        NetworkImage(
            context = context,
            imageUrl = bannerUrl,
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize(),
        )
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 25.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            shape = CircleShape,
                            color = Color(ContextCompat.getColor(context, R.color.gray900))
                        )
                        .defaultMinSize(minHeight = 20.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.icon_heartpick_timer),
                        contentDescription = "timer",
                        tint = Color(ContextCompat.getColor(context, R.color.navigation_bar)),
                        modifier = Modifier
                            .size(11.dp),
                    )
                    Spacer(
                        modifier = Modifier
                            .size(3.dp)
                    )
                    Text(
                        text = context.getString(R.string.upcoming),
                        color = Color(ContextCompat.getColor(context, R.color.navigation_bar)),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(
                    modifier = Modifier
                        .height(25.dp)
                )
                Text(
                    text = title,
                    color = Color(ContextCompat.getColor(context, android.R.color.white)),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.W500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color(ContextCompat.getColor(context, android.R.color.white)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HeartPickPrelaunchContent(
    context: Context,
    heartPickModel: HeartPickModel,
    mostId: Int?,
    isNotify: Boolean = false,
    idolClick: (Int) -> Unit = {},
    openNotificationSetting: () -> Unit = {},
    openCommentScreen: (Int) -> Unit = {},
) {
    var isRewardOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color(ContextCompat.getColor(context, R.color.background_200))
            )
    ) {
        item {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                Row {
                    Text(
                        text = context.getString(R.string.kdf_period_title),
                        color = Color(ContextCompat.getColor(context, R.color.text_gray)),

                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(
                        modifier = Modifier
                            .width(11.dp)
                    )

                    val startMillis = parseDateStringToMillis(heartPickModel.beginAtUtc)
                    val endMillis = parseDateStringToMillis(heartPickModel.endAtUtc)
                    Text(
                        text = "${formatDateForDisplay(startMillis)}~${
                            formatDateForDisplay(
                                endMillis
                            )
                        }",
                        color = Color(ContextCompat.getColor(context, R.color.text_gray)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(
                    modifier = Modifier
                        .height(6.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = context.getString(R.string.cheering_comments),
                        color = Color(ContextCompat.getColor(context, R.color.text_gray)),

                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(
                        modifier = Modifier
                            .width(6.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = Color(ContextCompat.getColor(context, R.color.main200)),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                            .noRippleClickable { openCommentScreen(heartPickModel.id) },
                    ) {
                        Text(
                            text = context.getString(
                                R.string.view_number_of_comments,
                                heartPickModel.numComments.toString()
                            ),
                            color = Color(ContextCompat.getColor(context, R.color.main_light)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(
                            modifier = Modifier
                                .width(3.dp)
                        )
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.arrow_left_to_right),
                            contentDescription = "arrow",
                            tint = Color(ContextCompat.getColor(context, R.color.main_light)),
                            modifier = Modifier
                                .size(8.dp),
                        )
                    }
                }
                Spacer(
                    modifier = Modifier
                        .height(17.dp)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .defaultMinSize(minHeight = 54.dp)
                        .background(
                            color = Color(ContextCompat.getColor(context, R.color.gray80)),
                            shape = RoundedCornerShape(15.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .noRippleClickable { isRewardOpen = !isRewardOpen }
                    ) {
                        Row {
                            Image(
                                imageVector = ImageVector.vectorResource(id = R.drawable.icon_heartpick_reward),
                                contentDescription = "coin",
                                modifier = Modifier
                                    .size(22.dp),
                            )
                            Spacer(
                                modifier = Modifier
                                    .width(9.dp)
                            )
                            Text(
                                text = context.getString(R.string.first_rank_reward),
                                color = Color(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.text_default
                                    )
                                ),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                ),
                                modifier = Modifier
                                    .weight(1f),
                            )
                            Spacer(
                                modifier = Modifier
                                    .width(9.dp)
                            )
                            Image(
                                imageVector = if (isRewardOpen) {
                                    ImageVector.vectorResource(id = R.drawable.btn_arrow_up_gray)
                                } else {
                                    ImageVector.vectorResource(id = R.drawable.btn_arrow_down_gray)
                                },
                                contentDescription = "arrow_up",
                                modifier = Modifier
                                    .size(22.dp),
                            )
                        }
                        if (isRewardOpen) {
                            Spacer(
                                modifier = Modifier
                                    .height(16.dp)
                            )
                            Text(
                                text = heartPickModel.prize?.name ?: "",
                                color = Color(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.text_default
                                    )
                                ),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = heartPickModel.prize?.location ?: "",
                                color = Color(ContextCompat.getColor(context, R.color.text_gray)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(
                                modifier = Modifier
                                    .height(8.dp)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(133.dp)
                                    .background(
                                        color = Color(
                                            ContextCompat.getColor(
                                                context,
                                                R.color.background_100
                                            )
                                        ),
                                        shape = RoundedCornerShape(15.dp)
                                    )
                            ) {
                                NetworkImage(
                                    context = context,
                                    imageUrl = heartPickModel.prize?.image_url ?: "",
                                )
                            }
                        }
                    }
                }
                Spacer(
                    modifier = Modifier
                        .height(20.dp)
                )

                val btnColor = if (!isNotify) {
                    Color(ContextCompat.getColor(context, R.color.main_light))
                } else {
                    Color(ContextCompat.getColor(context, R.color.gray110))
                }
                val btnText = if (!isNotify) {
                    context.getString(R.string.vote_alert_before)
                } else {
                    context.getString(R.string.vote_alert_after)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .background(
                            color = btnColor,
                            shape = CircleShape
                        )
                        .noRippleClickable {
                            if (isNotify) return@noRippleClickable
                            setFirebaseUIAction(GaAction.HEART_PICK_PRELAUNCH)
                            openNotificationSetting()
                        },
                ) {
                    Text(
                        text = btnText,
                        color = Color(ContextCompat.getColor(context, R.color.text_white_black)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        if (!heartPickModel.heartPickIdols.isNullOrEmpty()) {
            val (matched, others) = heartPickModel.heartPickIdols.partition { it.idol_id == mostId }
            val reorderedList = matched + others

            items(reorderedList.size) {
                HeartPickPrelaunchRanking(
                    context = context,
                    idol = reorderedList[it],
                    isMost = mostId == reorderedList[it].idol_id,
                    onItemClick = idolClick
                )
            }
        }
    }
}

@Composable
fun HeartPickPrelaunchRanking(
    context: Context,
    idol: HeartPickIdol,
    isMost: Boolean = false,
    onItemClick: (Int) -> Unit = {},
) {
    val bgColor = if (isMost) {
        Color(ContextCompat.getColor(context, R.color.main100))
    } else {
        Color(ContextCompat.getColor(context, R.color.background_200))
    }
    Box(
        modifier = Modifier
            .background(
                color = bgColor,
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(73.dp)
                .padding(horizontal = 26.dp, vertical = 9.dp)
                .noRippleClickable { onItemClick(idol.idol_id) }
        ) {
            Card(
                modifier = Modifier
                    .size(55.dp)
                    .background(
                        color = Color(ContextCompat.getColor(context, R.color.background_200)),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
            ) {
                NetworkImage(
                    context = context,
                    imageUrl = idol.image_url,
                )
            }
            Spacer(
                modifier = Modifier
                    .width(9.dp)
            )
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = idol.title,
                    color = Color(ContextCompat.getColor(context, R.color.text_default)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(
                    modifier = Modifier
                        .width(5.dp)
                )
                Text(
                    text = idol.subtitle,
                    color = Color(ContextCompat.getColor(context, R.color.text_dimmed)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview
@Composable
fun HeartPickPrelaunchScreenPreview() {
    HeartPickPrelaunchBanner(
        LocalContext.current,
        "url",
        "하트픽 미리보기",
        "하트픽은 투표를 통해 인기 있는 콘텐츠를 선정하는 서비스입니다."
    )
}

@Preview
@Composable
fun HeartPickPrelaunchContentPreview() {
    HeartPickPrelaunchContent(
        context = LocalContext.current,
        mostId = 0,
        heartPickModel = HeartPickModel(
            id = 1,
            title = "하트픽 미리보기",
            subtitle = "하트픽은 투표를 통해 인기 있는 콘텐츠를 선정하는 서비스입니다.",
            beginAt = "2023-10-01T00:00:00Z",
            endAt = "2023-10-31T23:59:59Z",
            bannerUrl = "https://example.com/banner.png",
            numComments = 123,
            heartPickIdols = arrayListOf(
                HeartPickIdol(
                    id = 1,
                    idol_id = 101,
                    groupId = 1,
                    image_url = "https://example.com/idol1.png",
                    subtitle = "아이돌 서브타이틀",
                    title = "아이돌 이름 1",
                    vote = 1000
                ),
                HeartPickIdol(
                    id = 2,
                    idol_id = 102,
                    groupId = 1,
                    image_url = "https://example.com/idol2.png",
                    subtitle = "아이돌 서브타이틀",
                    title = "아이돌 이름 2",
                    vote = 800
                )
            ),
            prize = null
        )
    )
}

@Preview
@Composable
fun HeartPickPrelaunchRankingPreview() {
    HeartPickPrelaunchRanking(
        context = LocalContext.current,
        idol = HeartPickIdol(
            id = 1,
            idol_id = 101,
            groupId = 1,
            image_url = "https://example.com/image.png",
            subtitle = "서브타이틀",
            title = "아이돌 이름",
            vote = 1000
        )
    )
}