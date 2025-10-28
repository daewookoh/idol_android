package net.ib.mn.feature.mission

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.core.data.repository.MissionsRepository
import net.ib.mn.core.designsystem.util.noRippleClickable
import net.ib.mn.feature.mission.component.MissionItem
import org.json.JSONObject

@Composable
fun MissionScreen(
    viewModel: MissionViewModel,
    moveScreen: (data: MissionItemInfo) -> Unit,
    close: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.missionUiState.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = true) {
        viewModel.getWelcomeMission()
    }

    BackHandler {
        close()
    }

    when (uiState) {
        is MissionUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.Center),
                    color = Color.Black
                )
            }
        }

        is MissionUiState.Success -> {
            val successState = uiState as MissionUiState.Success

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .padding(horizontal = 13.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { close() },
                            painter = painterResource(id = R.drawable.btn_navigation_back),
                            contentDescription = "backButton"
                        )
                        Spacer(modifier = Modifier
                            .width(18.dp)
                        )
                        val toolbarTextColor = if (BuildConfig.CELEB) {
                            R.color.toolbar_default
                        } else {
                            R.color.text_chat
                        }

                        Text(
                            text = stringResource(id = R.string.welcome_mission_title),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorResource(id = toolbarTextColor),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(13.dp))
                        Text(
                            text = stringResource(id = R.string.welcome_mission_desc),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(id = R.color.text_gray),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )
                        Spacer(
                            modifier = Modifier
                                .height(30.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            successState.data.list.forEach { welcomeMissionModel ->
                                MissionItem(
                                    item = welcomeMissionModel
                                ) { missionModel ->
                                    if (missionModel.status == MissionStatus.REWARD.status) {
                                        viewModel.requestGetReward(missionModel.key)
                                    } else {
                                        moveScreen(missionModel.desc)
                                    }
                                }
                            }
                        }
                        Icon(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(67.dp),
                            painter = painterResource(id = R.drawable.img_welcome_mission_clear),
                            contentDescription = "missionClearIcon",
                            tint = Color.Unspecified
                        )

                        val allClearButtonBg =
                            if ((uiState as MissionUiState.Success).data.allClearReward.status == MissionStatus.REWARD.status) {
                                R.color.main
                            } else {
                                R.color.gray110
                            }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 80.dp)
                                .background(
                                    color = colorResource(id = allClearButtonBg),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(14.dp)
                                .noRippleClickable {
                                    moveScreen(MissionItemInfo.WELCOME_ALL_CLEAR)

                                    val allClearStatus =
                                        (uiState as MissionUiState.Success).data.allClearReward.status

                                    when (allClearStatus) {
                                        MissionStatus.REWARD.status -> {
                                            viewModel.requestGetReward(MissionItemInfo.WELCOME_ALL_CLEAR.key)
                                        }

                                        MissionStatus.GOING.status -> {
                                            Toast
                                                .makeText(
                                                    context,
                                                    context.getString(R.string.impossible_all_clear),
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        }

                                        MissionStatus.DONE.status -> {
                                            Toast
                                                .makeText(
                                                    context,
                                                    context.getString(R.string.retry_all_clear),
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        }
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(30.dp),
                                    painter = painterResource(id = R.drawable.ic_mission_heart),
                                    contentDescription = "missionIcon",
                                    tint = Color.Unspecified
                                )
                                Spacer(
                                    modifier = Modifier
                                        .width(7.dp)
                                )

                                Text(
                                    text = successState.data.allClearReward.amount.toString(),
                                    color = colorResource(id = R.color.text_white_black),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(
                                            includeFontPadding = false
                                        )
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

// preview용 fake repository
class FakeMissionRepository: MissionsRepository {
    override suspend fun getWelcomeMission(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
    }

    override suspend fun claimMissionReward(
        key: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
    }
}

@Preview
@Composable
fun PreviewMissionScreen() {
    MissionScreen(
        viewModel = MissionViewModel(FakeMissionRepository()),
        {}
    ) {

    }
}