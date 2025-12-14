package net.ib.mn.presentation.awards.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import net.ib.mn.presentation.awards.AwardsViewModel
import net.ib.mn.ui.theme.LocalDarkTheme
import net.ib.mn.ui.theme.ExoTypo

private val FixBlack = Color(0xFF000000)
private val FixWhite = Color(0xFFFFFFFF)
private const val IMAGE_ASPECT_RATIO = 1.5f // 360:240

/**
 * AwardsGuideSubPage - 어워즈 안내 탭
 *
 * old 프로젝트의 AwardsGuideFragment 참고
 * - 어워즈 메인 이미지 표시
 * - 어워즈 타이틀 및 설명
 * - 투표기간, 투표대상, 투표방법, 유의사항, 투표규정 섹션
 */
@Composable
fun AwardsGuideSubPage(
    viewModel: AwardsViewModel = hiltViewModel()
) {
    // collectAsState로 데이터 변경 시 recomposition 트리거
    viewModel.awardModel.collectAsState()

    // 앱 내부 다크모드 설정 우선, 없으면 시스템 설정 사용
    val isDarkTheme = LocalDarkTheme.current ?: isSystemInDarkTheme()
    val guideData = viewModel.getGuideDescriptions()
    val periodText = viewModel.getAwardPeriodText()
    val logoUrl = viewModel.getLogoImageUrl(isDarkTheme)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FixBlack)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 메인 이미지 (360:240 고정 영역, 이미지는 Fit)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT_RATIO),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // 타이틀 및 설명 영역
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 17.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 어워즈 타이틀
            Text(
                text = guideData?.getOrNull(0).orEmpty(),
                style = ExoTypo.typo20Bold.copy(
                    color = FixWhite,
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 어워즈 설명
            Text(
                text = guideData?.getOrNull(1).orEmpty(),
                style = ExoTypo.typo12.copy(
                    color = FixWhite,
                    lineHeight = 16.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        // 투표 기간 섹션
        GuideSection(
            title = guideData?.getOrNull(2).orEmpty(),
            content = periodText
        )

        // 투표 대상 섹션
        GuideSection(
            title = guideData?.getOrNull(3).orEmpty(),
            content = guideData?.getOrNull(4).orEmpty()
        )

        // 투표 방법 섹션
        GuideSection(
            title = guideData?.getOrNull(5).orEmpty(),
            content = guideData?.getOrNull(6).orEmpty()
        )

        // 유의사항 섹션
        GuideSection(
            title = guideData?.getOrNull(7).orEmpty(),
            content = guideData?.getOrNull(8).orEmpty()
        )

        // 투표 규정 섹션
        GuideSection(
            title = guideData?.getOrNull(9).orEmpty(),
            content = guideData?.getOrNull(10).orEmpty()
        )
    }
}

@Composable
private fun GuideSection(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    if (title.isEmpty() && content.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 22.dp, end = 22.dp)
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = ExoTypo.typo14Bold.copy(
                    color = FixWhite,
                    lineHeight = 18.sp
                )
            )
        }

        if (content.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = ExoTypo.typo12.copy(
                    color = FixWhite,
                    lineHeight = 16.sp
                )
            )
        }
    }
}
