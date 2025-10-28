package net.ib.mn.feature.mission.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.core.designsystem.util.noRippleClickable
import net.ib.mn.feature.mission.MissionItemInfo
import net.ib.mn.feature.mission.MissionStatus
import net.ib.mn.model.WelcomeMissionModel

@Composable
fun MissionItem(
    item: WelcomeMissionModel,
    click: (WelcomeMissionModel) -> Unit,
) {
    if (item.key == MissionItemInfo.WELCOME_ALL_CLEAR.key) return

    val title = if (BuildConfig.CELEB && item.desc.celebTitle != 0) {
        item.desc.celebTitle
    } else {
        item.desc.title
    }

    val subTitle = if (BuildConfig.CELEB && item.desc.celebSubTitle != 0) {
        item.desc.celebSubTitle
    } else {
        item.desc.subTitle
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .noRippleClickable {
                if (item.status == MissionStatus.DONE.status) return@noRippleClickable
                click(item)
            }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                modifier = Modifier,
                text = stringResource(id = title),
                color = colorResource(id = R.color.text_default),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(
                modifier = Modifier
                    .height(4.dp)
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                maxLines = 2,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                overflow = TextOverflow.Ellipsis,
                text = stringResource(id = subTitle),
                color = colorResource(id = R.color.text_dimmed),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(
            modifier = Modifier
                .width(11.dp)
        )
        Column(
            modifier = Modifier
        ) {
            Spacer(
                modifier = Modifier
                    .height(5.dp)
            )

            val backgroundColor = when (item.status) {
                "G" -> {
                    R.color.gray900
                }

                "D" -> {
                    R.color.gray200
                }

                "R" -> {
                    if (item.item == "heart") {
                        R.color.main
                    } else {
                        R.color.text_light_blue
                    }
                }

                else -> {
                    R.color.gray200
                }
            }

            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .defaultMinSize(
                        minHeight = 32.dp
                    )
                    .background(
                        color = colorResource(id = backgroundColor),
                        shape = RoundedCornerShape(25.dp)
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.status == "R") {
                    val iconResource = when (item.item) {
                        "heart" -> {
                            R.drawable.ic_mission_heart
                        }

                        "diamond" -> {
                            R.drawable.ic_mission_diamond
                        }

                        else -> {
                            R.drawable.ic_mission_heart
                        }
                    }

                    Icon(
                        modifier = Modifier
                            .size(14.dp),
                        painter = painterResource(id = iconResource),
                        contentDescription = "missionIcon",
                        tint = Color.Unspecified
                    )
                    Spacer(
                        modifier = Modifier
                            .width(4.dp)
                    )
                }

                val buttonText = when (item.status) {
                    "D" -> {
                        stringResource(id = R.string.complete)
                    }

                    "R" -> {
                        "${item.amount}"
                    }

                    "G" -> {
                        stringResource(id = R.string.go)
                    }

                    else -> {
                        ""
                    }
                }

                Text(
                    text = buttonText,
                    color = colorResource(id = R.color.text_white_black),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewMissionItem() {
    MissionItem(
        item = WelcomeMissionModel(100, "item", "key", "D", MissionItemInfo.WELCOME_JOIN)
    ) {

    }
}