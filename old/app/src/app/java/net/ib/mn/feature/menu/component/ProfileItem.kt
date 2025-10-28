package net.ib.mn.feature.menu.component

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.utils.NetworkImage

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: 나의 정보 프로필 부분 컴포넌트 (적용은 차후 해야할듯, 코드 정리 필요)
 *
 * */

@Composable
fun ProfileComponent(
    context: Context,
) {
    Column {
        Box(
            modifier = Modifier
                .height(31.dp)
                .padding(start = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                Spacer(modifier = Modifier.width(18.dp))
                Text(
                    modifier = Modifier
                        .background(
                            Color(ContextCompat.getColor(context, R.color.subscribe_label)),
                            CircleShape
                        )
                        .defaultMinSize(minWidth = 77.dp)
                        .height(18.dp)
                        .padding(start = 14.dp, end = 8.dp, top = 0.dp, bottom = 3.dp),
                    text = "정기결제중입니다",
                    fontSize = 10.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 2.em
                )
            }
            Image(
                painter = painterResource(id = R.drawable.bg_daily_badge_1),
                contentDescription = "dailyPackBadge"
            )
        }
        Spacer(
            modifier = Modifier
                .height(14.dp)
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(80.dp)  // 이미지의 크기 설정
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    shape = CircleShape
                ) {
                    NetworkImage(
                        context = context,
                        imageUrl = "https://xkxqjlzvieat874751.gcdn.ntruss.com/1/2023/405e/1405ecb6a659f845d416c1fb8261aa31c26e6034164a1285b816d11b6fbf8adb1_s_st.jpg"
                    )
                }
                // Icon을 오른쪽 상단에 배치
                Icon(
                    modifier = Modifier
                        .size(13.dp)
                        .align(Alignment.TopEnd),
                    imageVector = ImageVector.vectorResource(id = R.drawable.icon_menu_new_default),
                    contentDescription = "coupon",
                    tint = Color(ContextCompat.getColor(context, R.color.main))
                )
            }
            Spacer(
                modifier = Modifier
                    .width(20.dp)
            )
            Column(
                verticalArrangement = Arrangement
                    .spacedBy(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier,
                        painter = painterResource(id = R.drawable.icon_level_0),
                        contentDescription = "levelIcon"
                    )
                    Spacer(
                        modifier = Modifier
                            .width(8.dp)
                    )
                    Text(
                        text = "닉네임",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(ContextCompat.getColor(context, R.color.text_default)),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                }
                Spacer(
                    modifier = Modifier
                        .height(2.dp)
                )
                Row(
                ) {
                    Spacer(
                        modifier = Modifier
                            .width(8.dp)
                    )
                    Image(
                        modifier = Modifier
                            .height(15.dp),
                        painter = painterResource(id = R.drawable.allinbadge_level_icon),
                        contentDescription = "levelIcon"
                    )
                    Spacer(
                        modifier = Modifier
                            .width(15.dp)
                    )
                    Text(
                        text = "닉운만",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(ContextCompat.getColor(context, R.color.text_default)),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .align(Alignment.Bottom),
                        text = "부자임",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(ContextCompat.getColor(context, R.color.text_dimmed)),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f),
                        text = "Lv.30",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(ContextCompat.getColor(context, R.color.main)),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                    Text(
                        modifier = Modifier
                            .weight(1f),
                        text = "13,541",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(ContextCompat.getColor(context, R.color.main)),
                        textAlign = TextAlign.End,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                }
                Spacer(
                    modifier = Modifier
                        .height(2.dp)
                )
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .border(
                            1.dp,
                            Color(ContextCompat.getColor(context, R.color.main)),
                            RoundedCornerShape(2.dp)
                        )
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    LinearProgressIndicator(
                        progress = { 0.5f },
                        color = Color(ContextCompat.getColor(context, R.color.main)),
                        trackColor = Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .height(10.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewProfileComponent() {
    ProfileComponent(
        LocalContext.current,
    )
}