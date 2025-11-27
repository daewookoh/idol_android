package net.ib.mn.presentation.main.community

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.data.repository.WikiRepository
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.presentation.webview.WebViewScreen
import net.ib.mn.ui.components.ExoNameWithGroup
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ExoTop3
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.NumberFormatUtil

/**
 * CommunityScreen - 커뮤니티 화면
 *
 * @param rankingItem 선택된 랭킹 아이템 데이터
 * @param wikiRepository WikiRepository 인스턴스
 * @param onBackClick 뒤로가기 클릭 이벤트
 */
@Composable
fun CommunityScreen(
    rankingItem: RankingItem,
    wikiRepository: WikiRepository? = null,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showWikiWebView by remember { mutableStateOf(false) }
    var wikiUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingWiki by remember { mutableStateOf(false) }

    BackHandler {
        if (showWikiWebView) {
            showWikiWebView = false
            wikiUrl = null
        } else {
            onBackClick()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ExoScaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100)
            ) {
                // 상단 ExoTop3 + 뒤로가기 버튼
                Box {
                    ExoTop3(
                        rankingItemData = rankingItem,
                        isVisible = true,
                        onItemClick = { /* TODO */ }
                    )

                    // 뒤로가기 버튼
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 10.dp, top = 11.dp)
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sharp_arrow_back_white_24),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 프로필 영역
                IdolProfile(
                    rankingItem = rankingItem,
                    onProfileImageClick = {
                        wikiRepository?.let { repo ->
                            val idolId = rankingItem.id.toIntOrNull() ?: return@let
                            if (idolId <= 0) return@let

                            coroutineScope.launch {
                                isLoadingWiki = true
                                val locale = LocaleUtil.getWikiLocale(context)

                                when (val result = repo.getWikiUrl(idolId, locale)) {
                                    is ApiResult.Success -> {
                                        wikiUrl = result.data
                                        showWikiWebView = true
                                    }
                                    is ApiResult.Error -> { /* 에러 무시 */ }
                                    is ApiResult.Loading -> { /* no-op */ }
                                }
                                isLoadingWiki = false
                            }
                        }
                    },
                    onMoreClick = { /* TODO */ }
                )
            }
        }

        // 위키 웹뷰 (아래에서 올라오는 애니메이션)
        AnimatedVisibility(
            visible = showWikiWebView && wikiUrl != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            wikiUrl?.let { url ->
                WebViewScreen(
                    url = url,
                    title = "Wiki",
                    onNavigateBack = {
                        showWikiWebView = false
                        wikiUrl = null
                    }
                )
            }
        }

        // 로딩 인디케이터
        if (isLoadingWiki) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorPalette.main)
            }
        }
    }
}

/**
 * IdolProfile - 아이돌 프로필 컴포넌트
 */
@Composable
private fun IdolProfile(
    rankingItem: RankingItem,
    onProfileImageClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(10.dp))

        // 프로필 이미지
        Box(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onProfileImageClick() }
        ) {
            ExoProfileImage(
                imageUrl = rankingItem.photoUrl ?: "",
                type = ProfileImageType.MEDIUM_CIRCLE,
                rank = 0,
                anniversary = rankingItem.anniversary ?: "N",
                anniversaryDays = rankingItem.anniversaryDays
            )
        }

        Spacer(modifier = Modifier.width(5.dp))

        // 이름 + 그룹명 + 팔로워
        Column(modifier = Modifier.weight(1f)) {
            ExoNameWithGroup(
                fullName = rankingItem.name,
                nameFontSize = 16.sp,
                groupFontSize = 11.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.icon_community_person),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = NumberFormatUtil.formatFollowerCount(rankingItem.mostCount),
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    color = ColorPalette.textDimmed
                )
            }
        }

        // 더보기 버튼
        Icon(
            painter = painterResource(R.drawable.btn_navigation_view_more),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(end = 16.dp)
                .clickable { onMoreClick() }
        )
    }
}
