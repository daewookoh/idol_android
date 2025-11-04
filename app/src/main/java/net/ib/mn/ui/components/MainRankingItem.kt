package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import net.ib.mn.R

/**
 * MainRankingItem - 랭킹 리스트 아이템
 *
 * old 프로젝트의 heart_vote_ranking_item.xml 참고
 *
 * @param rank 순위 (예: 1, 2, 3)
 * @param name 이름
 * @param voteCount 투표수 또는 득표수
 * @param photoUrl 프로필 이미지 URL
 * @param showDivider 상단 구분선 표시 여부
 * @param onClick 클릭 이벤트
 */
@Composable
fun MainRankingItem(
    rank: Int,
    name: String,
    voteCount: String,
    photoUrl: String? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_100))
            .clickable { onClick() }
    ) {
        // 상단 구분선
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = colorResource(R.color.gray100)
            )
        }

        // 아이템 컨텐츠
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 순위 표시 (45dp 고정 너비)
            Text(
                text = "${rank}위",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.text_default),
                modifier = Modifier.width(45.dp)
            )

            // 프로필 이미지
            AsyncImage(
                model = photoUrl,
                contentDescription = "프로필 이미지",
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.gray100)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 이름 및 득표수
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 이름
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.main)
                )

                // 득표수
                Text(
                    text = voteCount,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colorResource(R.color.text_dimmed)
                )
            }
        }
    }
}
