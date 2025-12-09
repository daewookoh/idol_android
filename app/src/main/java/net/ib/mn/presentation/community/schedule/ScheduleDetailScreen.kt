package net.ib.mn.presentation.community.schedule

import android.widget.Toast
import net.ib.mn.util.IntentUtil
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.domain.model.ScheduleModel
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ScheduleVoteAndComment
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.LocaleUtil
import java.text.SimpleDateFormat
import java.util.TimeZone

private val KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")

@Composable
fun ScheduleDetailScreen(
    idolId: Int,
    yearMonth: String,
    locale: String,
    initialScheduleId: Int? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToScheduleEdit: (ScheduleModel) -> Unit = {},
    onNavigateToComments: (ScheduleModel) -> Unit = {},
    viewModel: ScheduleDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var showDeleteDialog by remember { mutableStateOf<Int?>(null) }

    // 초기화
    LaunchedEffect(idolId, yearMonth, locale) {
        viewModel.sendIntent(
            ScheduleDetailContract.Intent.Initialize(
                idolId = idolId,
                yearMonth = yearMonth,
                locale = locale,
                initialScheduleId = initialScheduleId
            )
        )
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ScheduleDetailContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ScheduleDetailContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ScheduleDetailContract.Effect.NavigateToScheduleEdit -> {
                    onNavigateToScheduleEdit(effect.schedule)
                }
                is ScheduleDetailContract.Effect.NavigateToComments -> {
                    onNavigateToComments(effect.schedule)
                }
                is ScheduleDetailContract.Effect.OpenMapIntent -> {
                    IntentUtil.openMap(context, effect.location)
                }
                is ScheduleDetailContract.Effect.OpenUrlIntent -> {
                    IntentUtil.openUrl(context, effect.url)
                }
                is ScheduleDetailContract.Effect.ShowDeleteConfirm -> {
                    showDeleteDialog = effect.scheduleId
                }
                is ScheduleDetailContract.Effect.ScrollToSchedule -> {
                    var targetIndex = 0
                    for ((_, schedules) in state.groupedSchedules) {
                        targetIndex++ // date header
                        val found = schedules.indexOfFirst { it.id == effect.scheduleId }
                        if (found >= 0) {
                            targetIndex += found
                            break
                        }
                        targetIndex += schedules.size
                    }
                    listState.scrollToItem(targetIndex)
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    showDeleteDialog?.let { scheduleId ->
        ExoConfirmDialog(
            title = stringResource(R.string.title_remove),
            message = stringResource(R.string.confirm_delete),
            onConfirm = {
                showDeleteDialog = null
                viewModel.sendIntent(ScheduleDetailContract.Intent.DeleteSchedule(scheduleId))
            },
            onDismiss = { showDeleteDialog = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.gray80)
    ) {
        ExoAppBar(
            title = stringResource(R.string.schedule_detail),
            onNavigationClick = onNavigateBack
        )

        when {
            state.isLoading && state.schedules.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPalette.main)
                }
            }
            state.schedules.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.schedule_empty),
                        fontSize = 14.sp,
                        color = ColorPalette.textDimmed
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    state.groupedSchedules.forEach { (dateString, schedules) ->
                        item(key = "date_$dateString") {
                            DateHeader(dateString = dateString)
                        }

                        items(
                            items = schedules,
                            key = { "schedule_${it.id}" }
                        ) { schedule ->
                            ScheduleDetailItem(
                                schedule = schedule,
                                onCommentClick = {
                                    viewModel.sendIntent(ScheduleDetailContract.Intent.ViewComments(schedule))
                                },
                                onVoteYes = {
                                    if (!schedule.isVoted) {
                                        viewModel.sendIntent(ScheduleDetailContract.Intent.VoteSchedule(schedule.id, "Y"))
                                    }
                                },
                                onVoteNo = {
                                    if (!schedule.isVoted) {
                                        viewModel.sendIntent(ScheduleDetailContract.Intent.VoteSchedule(schedule.id, "N"))
                                    }
                                },
                                onEditClick = {
                                    viewModel.sendIntent(ScheduleDetailContract.Intent.EditSchedule(schedule))
                                },
                                onDeleteClick = {
                                    showDeleteDialog = schedule.id
                                },
                                onMapClick = {
                                    viewModel.sendIntent(ScheduleDetailContract.Intent.OpenMap(schedule))
                                },
                                onUrlClick = { url ->
                                    viewModel.sendIntent(ScheduleDetailContract.Intent.OpenUrl(url))
                                }
                            )
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(50.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(dateString: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 날짜 왼쪽 선
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(ColorPalette.gray150)
            )

            // 날짜 텍스트
            Text(
                text = dateString,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ColorPalette.textGray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 날짜 오른쪽 선
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(ColorPalette.gray150)
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(ColorPalette.gray80)
        )
    }
}

@Composable
private fun ScheduleDetailItem(
    schedule: ScheduleModel,
    onCommentClick: () -> Unit,
    onVoteYes: () -> Unit,
    onVoteNo: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMapClick: () -> Unit,
    onUrlClick: (String) -> Unit
) {
    val context = LocalContext.current
    val appLocale = LocaleUtil.getAppLocale(context)
    val timeFormat = remember(appLocale) {
        SimpleDateFormat("a h:mm", appLocale).apply { timeZone = KST }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
    ) {
        // 상단 - 시간, 아이콘, 제목, 수정/삭제 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 60.dp)
                .padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 시간 (하루종일이 아닌 경우에만 표시)
            if (schedule.allday != 1) {
                Text(
                    text = timeFormat.format(schedule.dtstart),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPalette.textDefault,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }

            // 카테고리 아이콘
            Icon(
                painter = painterResource(getScheduleIcon(schedule.category)),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )

            // 제목
            Text(
                text = schedule.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorPalette.textDefault,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            )

            // 수정/삭제 버튼 (권한이 있는 경우만)
            if (!schedule.isReadonly) {
                Icon(
                    painter = painterResource(R.drawable.icon_community_setting),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(25.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onEditClick() }
                        .padding(5.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.icon_community_trash),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(25.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onDeleteClick() }
                        .padding(5.dp)
                )
            }
        }

        // URL (있는 경우)
        schedule.url?.takeIf { it.isNotEmpty() }?.let { url ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onUrlClick(url) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.icon_schedule_link),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.padding(end = 5.dp, top = 8.5.dp)
                )
                Text(
                    text = url,
                    fontSize = 12.sp,
                    color = ColorPalette.textLightBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
        }

        // 추가 정보 (있는 경우)
        schedule.extra?.takeIf { it.isNotEmpty() }?.let { extra ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    painter = painterResource(R.drawable.icon_schedule_information),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.padding(end = 5.dp, top = 8.5.dp)
                )
                Text(
                    text = extra,
                    fontSize = 12.sp,
                    color = ColorPalette.textGray,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
        }

        // 위치 (있는 경우) - 중국 버전에서는 숨김
        if (!BuildConfig.CHINA) {
            schedule.location?.takeIf { it.isNotEmpty() }?.let { location ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .height(100.dp)
                        .clickable { onMapClick() }
                ) {
                    // 지도 배경 이미지
                    Icon(
                        painter = painterResource(R.drawable.bg_map),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.fillMaxSize()
                    )
                    // 지도 아이콘
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.icon_schedule_map),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                        Text(
                            text = location,
                            fontSize = 9.sp,
                            color = ColorPalette.gray580,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 작성자 정보
        schedule.userName?.let { userName ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 레벨 아이콘 (레벨 정보가 있는 경우)
                if (schedule.userLevel > 0) {
                    Icon(
                        painter = painterResource(getLevelIcon(schedule.userLevel)),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.padding(end = 5.dp)
                    )
                }
                Text(
                    text = userName,
                    fontSize = 11.sp,
                    color = ColorPalette.textGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 하단 댓글/투표 버튼 영역
        ScheduleVoteAndComment(
            schedule = schedule,
            onCommentClick = onCommentClick,
            onVoteYes = onVoteYes,
            onVoteNo = onVoteNo
        )

        // 구분선
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(ColorPalette.gray80)
        )
    }
}

private fun getScheduleIcon(category: String?): Int = when (category) {
    "anniversary" -> R.drawable.schedule_category_01
    "albumday" -> R.drawable.schedule_category_02
    "movie" -> R.drawable.schedule_category_20
    "production" -> R.drawable.schedule_category_21
    "concert" -> R.drawable.schedule_category_03
    "event" -> R.drawable.schedule_category_04
    "sign" -> R.drawable.schedule_category_05
    "tv" -> R.drawable.schedule_category_06
    "radio" -> R.drawable.schedule_category_07
    "live" -> R.drawable.schedule_category_08
    "award" -> R.drawable.schedule_category_09
    "ticketing" -> R.drawable.schedule_category_11
    "preview" -> R.drawable.schedule_category_22
    else -> R.drawable.schedule_category_10
}

private fun getLevelIcon(level: Int): Int = when {
    level >= 15 -> R.drawable.icon_level_15
    level >= 14 -> R.drawable.icon_level_14
    level >= 13 -> R.drawable.icon_level_13
    level >= 12 -> R.drawable.icon_level_12
    level >= 11 -> R.drawable.icon_level_11
    level >= 10 -> R.drawable.icon_level_10
    level >= 9 -> R.drawable.icon_level_9
    level >= 8 -> R.drawable.icon_level_8
    level >= 7 -> R.drawable.icon_level_7
    level >= 6 -> R.drawable.icon_level_6
    level >= 5 -> R.drawable.icon_level_5
    level >= 4 -> R.drawable.icon_level_4
    level >= 3 -> R.drawable.icon_level_3
    level >= 2 -> R.drawable.icon_level_2
    else -> R.drawable.icon_level_1
}
