package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import net.ib.mn.R

/**
 * ExoTop3 - 랭킹 리스트 상단 배너 (구 MainRankingBanner)
 *
 * 화면을 꽉 채우는 배너 컴포넌트 (padding 없음, radius 없음)
 * 광고, 이벤트, 또는 주요 정보를 표시
 *
 * @param title 배너 제목
 * @param imageUrl 배너 이미지 URL
 * @param onClick 클릭 이벤트
 */
@Composable
fun ExoTop3(
    title: String? = null,
    imageUrl: String? = null,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(colorResource(R.color.gray100))
            .clickable { onClick() }
    ) {
        // 배너 이미지
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "배너 이미지",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }

        // 배너 제목 (이미지가 없거나 오버레이로 표시)
        if (title != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (imageUrl != null) {
                        colorResource(R.color.white)
                    } else {
                        colorResource(R.color.text_default)
                    }
                )
            }
        }
    }
}
