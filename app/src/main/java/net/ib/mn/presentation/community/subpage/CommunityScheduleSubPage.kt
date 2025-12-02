package net.ib.mn.presentation.community.subpage

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.R
import net.ib.mn.domain.model.ScheduleModel
import net.ib.mn.ui.components.ExoBottomSheetItem
import net.ib.mn.ui.components.ExoBottomSheetList
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScheduleSubPage(
    rankingItem: RankingItem,
    viewModel: CommunityScheduleViewModel = hiltViewModel(key = "schedule_${rankingItem.id}")
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val idolId = rankingItem.id.toIntOrNull() ?: 0

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(idolId) {
        viewModel.sendIntent(CommunityScheduleContract.Intent.LoadInitialData(idolId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CommunityScheduleContract.Effect.ShowError ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is CommunityScheduleContract.Effect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is CommunityScheduleContract.Effect.ShowLanguageSelector ->
                    showLanguageSheet = true
                is CommunityScheduleContract.Effect.ShowDeleteConfirm ->
                    showDeleteDialog = effect.scheduleId
                else -> Unit
            }
        }
    }

    showDeleteDialog?.let { scheduleId ->
        ExoConfirmDialog(
            title = stringResource(R.string.title_remove),
            message = stringResource(R.string.confirm_delete),
            onConfirm = {
                showDeleteDialog = null
                viewModel.sendIntent(CommunityScheduleContract.Intent.DeleteSchedule(scheduleId, idolId))
            },
            onDismiss = { showDeleteDialog = null }
        )
    }

    if (showLanguageSheet) {
        val locales = CommunityScheduleContract.State.SCHEDULE_LOCALES
        val languages = CommunityScheduleContract.State.SCHEDULE_LANGUAGES
        val languageItems = locales.mapIndexed { index, locale ->
            ExoBottomSheetItem(
                value = Pair(locale, languages[index]),
                label = languages[index]
            )
        }
        val selectedLocale = locales.find { it == state.locale } ?: locales[0]
        val selectedIndex = locales.indexOf(selectedLocale)
        val selectedLabel = languages.getOrElse(selectedIndex) { languages[0] }

        ExoBottomSheetList(
            items = languageItems,
            selectedValue = Pair(selectedLocale, selectedLabel),
            onItemSelected = { (locale, localeText) ->
                viewModel.sendIntent(CommunityScheduleContract.Intent.ChangeLocale(locale, localeText, idolId))
            },
            onDismissRequest = { showLanguageSheet = false }
        )
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.sendIntent(CommunityScheduleContract.Intent.Refresh(idolId)) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.gray80))
        ) {
            item(key = "calendar_header") {
                CalendarHeader(
                    year = state.currentYear,
                    month = state.currentMonth,
                    localeText = state.localeText,
                    onPreviousClick = { viewModel.sendIntent(CommunityScheduleContract.Intent.PreviousMonth(idolId)) },
                    onNextClick = { viewModel.sendIntent(CommunityScheduleContract.Intent.NextMonth(idolId)) },
                    onLanguageClick = { showLanguageSheet = true }
                )
            }

            item(key = "weekday_header") {
                WeekdayHeader()
            }

            item(key = "calendar_grid") {
                CalendarGrid(
                    state = state,
                    onDayClick = { day ->
                        viewModel.sendIntent(CommunityScheduleContract.Intent.SelectDay(day, idolId))
                    }
                )
            }

            when {
                state.isLoading && state.daySchedules.isEmpty() -> {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(colorResource(R.color.background_100)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colorResource(R.color.main))
                        }
                    }
                }
                state.daySchedules.isEmpty() -> {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colorResource(R.color.background_100))
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.schedule_empty),
                                fontSize = 14.sp,
                                color = colorResource(R.color.text_dimmed)
                            )
                        }
                    }
                }
                else -> {
                    items(items = state.daySchedules, key = { "schedule_${it.id}" }) { schedule ->
                        ScheduleItem(
                            schedule = schedule,
                            onDetailClick = { },
                            onCommentClick = { },
                            onVoteYes = {
                                if (!schedule.isVoted) {
                                    viewModel.sendIntent(CommunityScheduleContract.Intent.VoteSchedule(schedule.id, "Y", idolId))
                                }
                            },
                            onVoteNo = {
                                if (!schedule.isVoted) {
                                    viewModel.sendIntent(CommunityScheduleContract.Intent.VoteSchedule(schedule.id, "N", idolId))
                                }
                            },
                            onEditClick = { },
                            onDeleteClick = { showDeleteDialog = schedule.id }
                        )
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    year: Int,
    month: Int,
    localeText: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onLanguageClick: () -> Unit
) {
    val borderColor = colorResource(R.color.gray110)
    val appLocale = getAppLocale()
    val monthText = remember(year, month, appLocale) {
        SimpleDateFormat("MMM", appLocale).format(
            Calendar.getInstance().apply { set(year, month, 1) }.time
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_200))
            .drawBehind {
                drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.button_previous_month),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(45.dp)
                    .clickable { onPreviousClick() }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = year.toString(),
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    color = colorResource(R.color.text_gray)
                )
                Text(
                    text = monthText,
                    fontSize = 15.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.text_gray)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Icon(
                painter = painterResource(R.drawable.button_next_month),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(45.dp)
                    .clickable { onNextClick() }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(45.dp)
                .padding(end = 20.dp, start = 10.dp)
                .clickable { onLanguageClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = localeText,
                fontSize = 12.sp,
                color = colorResource(R.color.text_gray)
            )
            Icon(
                painter = painterResource(R.drawable.icon_arrow_drop_down),
                contentDescription = null,
                tint = colorResource(R.color.text_gray),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val borderColor = colorResource(R.color.gray110)
    val appLocale = getAppLocale()
    val weekdays = remember(appLocale) {
        val namesOfDays = DateFormatSymbols.getInstance(appLocale).shortWeekdays
        listOf(
            namesOfDays[Calendar.SUNDAY],
            namesOfDays[Calendar.MONDAY],
            namesOfDays[Calendar.TUESDAY],
            namesOfDays[Calendar.WEDNESDAY],
            namesOfDays[Calendar.THURSDAY],
            namesOfDays[Calendar.FRIDAY],
            namesOfDays[Calendar.SATURDAY]
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(colorResource(R.color.background_200))
            .drawBehind {
                drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
    ) {
        weekdays.forEachIndexed { index, day ->
            val color = when (index) {
                0 -> colorResource(R.color.main)
                6 -> colorResource(R.color.text_dimmed)
                else -> colorResource(R.color.text_gray)
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = day, fontSize = 10.sp, color = color)
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    state: CommunityScheduleContract.State,
    onDayClick: (Int) -> Unit
) {
    val firstDayOfWeek = state.firstDayOfWeek
    val daysInMonth = state.daysInMonth
    val totalCells = ((firstDayOfWeek - 1) + daysInMonth + 6) / 7 * 7

    val borderColor = colorResource(R.color.gray100)
    val mainColor = colorResource(R.color.main)
    val whiteColor = colorResource(R.color.text_white_black)
    val saturdayColor = colorResource(R.color.gray300)
    val weekdayColor = colorResource(R.color.gray580)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_100))
            .drawBehind {
                drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
    ) {
        for (week in 0 until totalCells / 7) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0 until 7) {
                    val cellIndex = week * 7 + dayOfWeek
                    val day = cellIndex - (firstDayOfWeek - 2)
                    val isValidDay = day in 1..daysInMonth
                    val isSelected = isValidDay && day == state.selectedDay
                    val scheduleCategory = if (isValidDay) state.dayIconMap[day] else null

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .then(
                                if (isSelected) Modifier.background(mainColor)
                                else Modifier.drawBehind {
                                    drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                                }
                            )
                            .clickable(enabled = isValidDay) { if (isValidDay) onDayClick(day) },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (isValidDay) {
                            val weekDay = remember(state.currentYear, state.currentMonth, day) {
                                Calendar.getInstance().apply {
                                    set(state.currentYear, state.currentMonth, day)
                                }.get(Calendar.DAY_OF_WEEK)
                            }

                            val textColor = when {
                                isSelected -> whiteColor
                                weekDay == Calendar.SUNDAY -> mainColor
                                weekDay == Calendar.SATURDAY -> saturdayColor
                                else -> weekdayColor
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 5.dp)
                            ) {
                                Text(
                                    text = day.toString(),
                                    fontSize = 9.sp,
                                    lineHeight = 9.sp,
                                    color = textColor
                                )
                                scheduleCategory?.let { category ->
                                    Icon(
                                        painter = painterResource(getScheduleIcon(category)),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleItem(
    schedule: ScheduleModel,
    onDetailClick: () -> Unit,
    onCommentClick: () -> Unit,
    onVoteYes: () -> Unit,
    onVoteNo: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val appLocale = getAppLocale()
    val timeFormat = remember(appLocale) {
        SimpleDateFormat("a h:mm", appLocale).apply { timeZone = KST }
    }
    val borderColor = colorResource(R.color.gray110)
    val gray300 = colorResource(R.color.gray300)
    val mainColor = colorResource(R.color.main)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .background(colorResource(R.color.background_100))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 60.dp)
                .clickable { onDetailClick() }
                .padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (schedule.allday != 1) {
                Text(
                    text = timeFormat.format(schedule.dtstart),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.text_default),
                    modifier = Modifier.padding(end = 10.dp)
                )
            }

            Icon(
                painter = painterResource(getScheduleIcon(schedule.category)),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = schedule.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.text_default),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            )

            if (!schedule.isReadonly) {
                Icon(
                    painter = painterResource(R.drawable.icon_community_setting),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(25.dp)
                        .clickable { onEditClick() }
                        .padding(5.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.icon_community_trash),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(25.dp)
                        .clickable { onDeleteClick() }
                        .padding(5.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().height(33.dp)) {
            // Comment button
            ScheduleActionButton(
                iconRes = R.drawable.icon_community_comment,
                text = schedule.numComments.toString(),
                isActive = false,
                borderColor = borderColor,
                activeColor = mainColor,
                inactiveColor = gray300,
                showEndBorder = true,
                onClick = onCommentClick,
                modifier = Modifier.weight(1f)
            )

            // Vote Yes button
            ScheduleActionButton(
                iconRes = if (schedule.isVotedYes) R.drawable.btn_schedule_yes_on else R.drawable.btn_schedule_yes_off,
                text = schedule.numYes.toString(),
                isActive = schedule.isVotedYes,
                borderColor = borderColor,
                activeColor = mainColor,
                inactiveColor = gray300,
                showEndBorder = true,
                onClick = onVoteYes,
                modifier = Modifier.weight(1f)
            )

            // Vote No button
            ScheduleActionButton(
                iconRes = if (schedule.isVotedNo) R.drawable.btn_schedule_no_on else R.drawable.btn_schedule_no_off,
                text = schedule.numNo.toString(),
                isActive = schedule.isVotedNo,
                borderColor = borderColor,
                activeColor = mainColor,
                inactiveColor = gray300,
                showEndBorder = false,
                onClick = onVoteNo,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScheduleActionButton(
    iconRes: Int,
    text: String,
    isActive: Boolean,
    borderColor: Color,
    activeColor: Color,
    inactiveColor: Color,
    showEndBorder: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isActive) activeColor else inactiveColor

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_200))
            .drawBehind {
                drawLine(borderColor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                if (showEndBorder) {
                    drawLine(borderColor, Offset(size.width, 0f), Offset(size.width, size.height), 1.dp.toPx())
                }
            }
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = text, fontSize = 11.sp, color = color)
    }
}


@Composable
private fun getAppLocale(): Locale {
    val context = LocalContext.current
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        context.resources.configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        context.resources.configuration.locale
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
