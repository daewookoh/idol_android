package net.ib.mn.feature.common

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.model.InAppBannerModel
import net.ib.mn.utils.NetworkImageScaleFit
import net.ib.mn.utils.UtilK

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InAppBanner(
    bannerList: List<InAppBannerModel>,
    clickBanner: (InAppBannerModel) -> Unit = {},
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(0, pageCount = { bannerList.size })

    val selectedColor = Color(ContextCompat.getColor(context, R.color.main))
    val unselectedColor = Color(ContextCompat.getColor(context, R.color.gray150))

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = pagerState.currentPage, key2 = bannerList.size > 1) {
        val delayMillis = 5000L // 5초
        while (true) {
            delay(delayMillis)
            val nextPage = if (pagerState.currentPage == bannerList.size - 1) {
                0
            } else {
                pagerState.currentPage + 1
            }
            coroutineScope.launch {
                pagerState.animateScrollToPage(nextPage)
                sendAnalyticsAdImpressionEvent(context, bannerList[pagerState.currentPage])
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        HorizontalPager(
            state = pagerState,
        ) {
            InAppBannerItem(
                item = bannerList[it],
                clickBanner = clickBanner
            )
        }
        Spacer(
            modifier = Modifier
                .height(4.dp)
        )
        if (bannerList.size > 1) {
            Row(
                Modifier
                    .height(6.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(bannerList.size) { iteration ->
                    val color =
                        if (pagerState.currentPage == iteration) selectedColor else unselectedColor
                    Box(
                        modifier = Modifier
                            .background(color, CircleShape)
                            .size(6.dp)
                    )
                    if (iteration != bannerList.size - 1 || iteration == 0) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(0.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InAppBannerItem(
    item: InAppBannerModel,
    clickBanner: (InAppBannerModel) -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(17f / 3f)
            .clickable {
                clickBanner(item)
            }
    ) {
        NetworkImageScaleFit(
            context = LocalContext.current,
            imageUrl = item.imageUrl
        )
    }
}

private fun sendAnalyticsAdImpressionEvent(context: Context, bannerItem: InAppBannerModel) {
        UtilK.sendAnalyticsAdEvent(
            context,
            FirebaseAnalytics.Event.AD_IMPRESSION,
            bannerItem.id,
        )
}

@Preview
@Composable
fun PreviewInAppBanner() {
    InAppBanner(
        bannerList = listOf(
            InAppBannerModel(
                id = 0,
                imageUrl = "",
                link = "",
                section = "M"
            ),
            InAppBannerModel(
                id = 1,
                imageUrl = "",
                link = "",
                section = "M"
            ),
            InAppBannerModel(
                id = 2,
                imageUrl = "",
                link = "",
                section = "M"
            )
        )
    )
}

@Preview
@Composable
private fun PreviewInAppBannerItem() {
    InAppBannerItem(
        item = InAppBannerModel(
            id = 0,
            imageUrl = "",
            link = "",
            section = "M"
        )
    )
}


