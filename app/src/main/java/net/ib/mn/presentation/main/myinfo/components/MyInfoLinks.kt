package net.ib.mn.presentation.main.myinfo.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import net.ib.mn.R

/**
 * MyInfo 페이지 하단 링크 메뉴
 * old의 fragment_my_heart_info.xml 하단 3개 버튼과 동일한 구조
 * 비디오광고, 상점, 무료충전소
 */
@Composable
fun MyInfoLinks(
    modifier: Modifier = Modifier,
    videoAdHeartCount: Int = 0, // 비디오광고 하트 개수 (TODO: ConfigModel에서 가져오기)
    onVideoAdClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onFreeChargeClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 비디오광고 (말풍선 포함)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            // 버튼
            MyInfoLinkButton(
                modifier = Modifier.fillMaxWidth(),
                iconResId = R.drawable.icon_my_ad,
                titleResId = R.string.title_reward_video,
                onClick = onVideoAdClick
            )

            // 하트 말풍선 (버튼 위에 표시)
            if (videoAdHeartCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(y = (-10).dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 말풍선 배경
                    Image(
                        painter = painterResource(id = R.drawable.icon_my_adheart),
                        contentDescription = null,
                        modifier = Modifier.height(18.dp),
                        colorFilter = ColorFilter.tint(colorResource(id = R.color.main_light))
                    )

                    // 하트 개수 텍스트
                    Text(
                        text = "♥\uFE0E$videoAdHeartCount",
                        color = colorResource(id = R.color.text_white_black),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier= Modifier.padding(bottom=2.dp)
                    )
                }
            }
        }

        // 상점
        MyInfoLinkButton(
            modifier = Modifier.weight(1f),
            iconResId = R.drawable.icon_my_shop,
            titleResId = R.string.label_store,
            onClick = onStoreClick
        )

        // 무료충전소
        MyInfoLinkButton(
            modifier = Modifier.weight(1f),
            iconResId = R.drawable.icon_my_free,
            titleResId = R.string.btn_free_heart_charge,
            onClick = onFreeChargeClick
        )
    }
}

@Composable
private fun MyInfoLinkButton(
    modifier: Modifier = Modifier,
    iconResId: Int,
    titleResId: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.background_200)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 아이콘
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(25.dp)
            )

            Spacer(modifier = Modifier.height(7.dp))

            // 텍스트
            Text(
                text = stringResource(id = titleResId),
                color = colorResource(id = R.color.text_default),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
