package net.ib.mn.presentation.awards

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.presentation.awards.subpage.AwardsCumulativeSubPage
import net.ib.mn.presentation.awards.subpage.AwardsDailySubPage
import net.ib.mn.presentation.awards.subpage.AwardsGuideSubPage
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.theme.ColorPalette

private const val TAB_HEIGHT = 48
private const val INDICATOR_HEIGHT = 3

@Composable
fun AwardsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AwardsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val awardModel by viewModel.awardModel.collectAsState()

    val tabs = listOf(
        stringResource(R.string.award_guide),
        stringResource(R.string.aggregation),
        stringResource(R.string.award_realtime)
    )

    val pagerState = rememberPagerState(initialPage = 2) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = awardModel?.awardTitle.orEmpty(),
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, viewModel.getShareMessage())
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.btn_navigation_share),
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> AwardsGuideSubPage()
                    1 -> AwardsCumulativeSubPage()
                    2 -> AwardsDailySubPage()
                }
            }

            AwardsBottomTabBar(
                tabs = tabs,
                pagerState = pagerState,
                coroutineScope = coroutineScope
            )
        }
    }
}

@Composable
private fun AwardsBottomTabBar(
    tabs: List<String>,
    pagerState: PagerState,
    coroutineScope: CoroutineScope
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TAB_HEIGHT.dp)
            .background(ColorPalette.background100)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = pagerState.currentPage == index

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(INDICATOR_HEIGHT.dp)
                        .background(if (isSelected) ColorPalette.main else Color.Transparent)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) ColorPalette.main else ColorPalette.textDimmed,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
