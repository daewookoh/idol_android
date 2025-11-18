package net.ib.mn.presentation.main.myinfo.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.ui.components.ExoProfileImage

/**
 * MyInfo 페이지 상단 계정 정보 섹션
 * fragment_myinfo.xml의 layout_account 영역과 동일
 */
@Composable
fun MyInfoAccountSection(
    modifier: Modifier = Modifier,
    userName: String = "",
    profileImageUrl: String = "",
    level: Int = 0,
    favoriteIdolName: String = "",
    favoriteIdolSubName: String = "",
    subscriptionName: String? = null,
    hasNewFeed: Boolean = false,
    onProfileClick: () -> Unit = {},
    onSubscriptionBadgeClick: () -> Unit = {}
) {
    // layout_account (padding 10dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        // cl_subscription_badge (layout_alignParentLeft)
        if (subscriptionName != null) {
            Text(
                text = subscriptionName,
                modifier = Modifier.clickable { onSubscriptionBadgeClick() },
                color = colorResource(id = R.color.text_white_black),
                fontSize = 10.sp,
                lineHeight = 10.sp
            )
        }

        // RelativeLayout with cl_myinfo
        // layout_centerInParent + layout_marginStart works as: start from left, not center
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (subscriptionName != null) 10.dp else 0.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // btn_photo_upload (60dp x 60dp - adjusted for visual balance)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clickable { onProfileClick() }
            ) {
                ExoProfileImage(
                    imageUrl = profileImageUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "Profile Image"
                )

                // iv_feed_new (13dp)
                if (hasNewFeed) {
                    Image(
                        painter = painterResource(id = R.drawable.icon_menu_new_default),
                        contentDescription = "New Feed",
                        modifier = Modifier
                            .size(13.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // cl_account_info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onProfileClick() },
                verticalArrangement = Arrangement.Center
            ) {
                // level + name Row
                Row(
                    Modifier.padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // level icon (15dp height)
                    Image(
                        painter = painterResource(id = getLevelIconResource(level)),
                        contentDescription = "Level Icon",
                        modifier = Modifier
                            .height(15.dp)
                            .wrapContentWidth()
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // name (18sp, bold)
                    Text(
                        text = userName,
                        color = colorResource(id = R.color.text_default),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp
                    )
                }

                // favorite (14sp, bold)
                Text(
                    text = buildAnnotatedString {
                            append(favoriteIdolName)
                            if (favoriteIdolSubName.isNotEmpty()) {
                                append(" ")
                                withStyle(
                                    style = SpanStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = colorResource(id = R.color.text_dimmed)
                                    )
                                ) {
                                    append(favoriteIdolSubName)
                                }
                            }
                        },
                    color = colorResource(id = R.color.text_default),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/**
 * 레벨별 아이콘 리소스 반환
 */
private fun getLevelIconResource(level: Int): Int {
    return when (level) {
        0 -> R.drawable.icon_level_0
        1 -> R.drawable.icon_level_1
        2 -> R.drawable.icon_level_2
        3 -> R.drawable.icon_level_3
        4 -> R.drawable.icon_level_4
        5 -> R.drawable.icon_level_5
        6 -> R.drawable.icon_level_6
        7 -> R.drawable.icon_level_7
        8 -> R.drawable.icon_level_8
        9 -> R.drawable.icon_level_9
        else -> R.drawable.icon_level_0
    }
}
