package net.ib.mn.presentation.welcomemission

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoRewardBottomSheet
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import java.text.NumberFormat
import java.util.Locale

/**
 * 웰컴 미션 화면
 *
 * old 프로젝트의 WelcomeMissionFragment와 동일한 UI/기능
 * Navigation3로 표시되는 일반 화면
 */
@Composable
fun WelcomeMissionScreen(
    viewModel: WelcomeMissionViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToSetMost: () -> Unit,
    onNavigateToVoteMost: () -> Unit,
    onNavigateToAddFriend: () -> Unit,
    onNavigateToPosting: () -> Unit,
    onNavigateToVideoAd: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Navigation 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.navigation.collect { nav ->
            when (nav) {
                MissionNavigation.SetMost -> {
                    onNavigateToSetMost()
                }
                MissionNavigation.VoteMost -> {
                    onNavigateToVoteMost()
                }
                MissionNavigation.AddFriend -> {
                    onNavigateToAddFriend()
                }
                MissionNavigation.Posting -> {
                    onNavigateToPosting()
                }
                MissionNavigation.VideoAd -> {
                    onNavigateToVideoAd()
                }
                MissionNavigation.None -> { /* Do nothing */ }
            }
        }
    }

    // 화면 닫기 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.closeScreen.collect {
            onBackClick()
        }
    }

    // 보상 바텀시트 (old 프로젝트의 RewardBottomSheetDialogFragment.setMissionRewardDialog 참조)
    uiState.claimedReward?.let { reward ->
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        ExoRewardBottomSheet(
            imageRes = R.drawable.img_popup_attendance,
            onDismiss = viewModel::dismissRewardDialog,
            showCloseButton = false
        ) {
            // 타이틀
            Text(
                text = stringResource(R.string.welcome_mission_all_clear_reward_title),
                style = ExoTypo.typo22Bold,
                color = ColorPalette.textChat,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 보상 수량
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "+",
                    style = ExoTypo.typo24Bold,
                    color = ColorPalette.main
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = numberFormat.format(reward.amount),
                    style = ExoTypo.typo24Bold,
                    color = ColorPalette.main
                )
                Spacer(modifier = Modifier.width(2.dp))
                Image(
                    painter = painterResource(R.drawable.img_popup_heart),
                    contentDescription = null,
                    modifier = Modifier.size(21.dp, 17.dp)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 확인 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(55.dp)
                    .background(
                        color = ColorPalette.main,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clickable { viewModel.dismissRewardDialog() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.confirm),
                    style = ExoTypo.typo16Bold,
                    color = ColorPalette.fixWhite
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
            .statusBarsPadding()
    ) {
        // 앱바
        ExoAppBar(
            title = stringResource(R.string.welcome_mission_title),
            onNavigationClick = { viewModel.closeScreen() }
        )

        // 콘텐츠
        WelcomeMissionContent(
            uiState = uiState,
            onMissionClick = viewModel::onMissionClick,
            onAllClearClick = { allClearReward ->
                // old 프로젝트와 동일하게 상태별 처리
                when (allClearReward.status) {
                    MissionStatus.REWARD -> {
                        viewModel.onAllClearClick()
                    }
                    MissionStatus.GOING -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.impossible_all_clear),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    MissionStatus.DONE -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.retry_all_clear),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismissReward = viewModel::dismissRewardDialog
        )
    }
}

@Composable
private fun WelcomeMissionContent(
    uiState: WelcomeMissionUiState,
    onMissionClick: (WelcomeMissionItem) -> Unit,
    onAllClearClick: (WelcomeMissionItem) -> Unit,
    onDismissReward: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // 설명 텍스트
        Text(
            text = stringResource(R.string.welcome_mission_desc),
            style = ExoTypo.typo14,
            color = ColorPalette.textDimmed,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = ColorPalette.main,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // 미션 리스트
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                items(
                    items = uiState.missions,
                    key = { it.key }
                ) { mission ->
                    MissionItem(
                        mission = mission,
                        onClick = { onMissionClick(mission) }
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = ColorPalette.gray100
                    )
                }

                // ALL CLEAR 보상 아이템
                uiState.allClearReward?.let { allClear ->
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        AllClearItem(
                            reward = allClear,
                            isAllMissionsDone = uiState.missions.all { it.status == MissionStatus.DONE },
                            onClick = { onAllClearClick(allClear) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * 개별 미션 아이템
 */
@Composable
private fun MissionItem(
    mission: WelcomeMissionItem,
    onClick: () -> Unit
) {
    // ALL CLEAR 아이템은 별도로 표시
    if (mission.key == MissionItemInfo.WELCOME_ALL_CLEAR.key) return

    val info = mission.info ?: return

    // CELEB 앱일 경우 다른 타이틀/서브타이틀 사용
    val title = if (BuildConfig.CELEB && info.celebTitle != 0) {
        info.celebTitle
    } else {
        info.title
    }

    val subTitle = if (BuildConfig.CELEB && info.celebSubTitle != 0) {
        info.celebSubTitle
    } else {
        info.subTitle
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(enabled = mission.status != MissionStatus.DONE) {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(id = title),
                style = ExoTypo.typo16Bold,
                color = ColorPalette.textDefault,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = subTitle),
                style = ExoTypo.typo14,
                color = ColorPalette.textDimmed,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(11.dp))

        MissionStatusButton(
            status = mission.status,
            item = mission.item,
            amount = mission.amount
        )
    }
}

/**
 * 미션 상태 버튼
 */
@Composable
private fun MissionStatusButton(
    status: MissionStatus,
    item: String,
    amount: Int
) {
    val backgroundColor = when (status) {
        MissionStatus.GOING -> ColorPalette.gray900
        MissionStatus.DONE -> ColorPalette.gray200
        MissionStatus.REWARD -> {
            if (item == "heart") ColorPalette.main else ColorPalette.textLightBlue
        }
    }

    Row(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(25.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (status == MissionStatus.REWARD) {
            val iconResource = if (item == "heart") {
                R.drawable.ic_mission_heart
            } else {
                R.drawable.ic_mission_diamond
            }

            Icon(
                painter = painterResource(id = iconResource),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        val buttonText = when (status) {
            MissionStatus.DONE -> stringResource(R.string.complete)
            MissionStatus.REWARD -> "$amount"
            MissionStatus.GOING -> stringResource(R.string.go)
        }

        Text(
            text = buttonText,
            color = ColorPalette.textWhiteBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * ALL CLEAR 보상 아이템 (old 프로젝트와 동일한 디자인)
 */
@Composable
private fun AllClearItem(
    reward: WelcomeMissionItem,
    isAllMissionsDone: Boolean,
    onClick: () -> Unit
) {
    val canClaim = isAllMissionsDone && reward.status == MissionStatus.REWARD
    val isDone = reward.status == MissionStatus.DONE

    val backgroundColor = when {
        canClaim -> ColorPalette.main
        else -> ColorPalette.gray100
    }

    // ALL CLEAR 이미지
    Icon(
        modifier = Modifier
            .fillMaxWidth()
            .height(67.dp),
        painter = painterResource(id = R.drawable.img_welcome_mission_clear),
        contentDescription = "missionClearIcon",
        tint = Color.Unspecified
    )

    // ALL CLEAR 버튼
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = canClaim) { onClick() }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconResource = if (reward.item == "heart") {
                R.drawable.ic_mission_heart
            } else {
                R.drawable.ic_mission_diamond
            }
            Icon(
                modifier = Modifier.size(30.dp),
                painter = painterResource(id = iconResource),
                contentDescription = "missionIcon",
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "${reward.amount}",
                color = ColorPalette.textWhiteBlack,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
