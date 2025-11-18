package net.ib.mn.presentation.main.myinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.presentation.main.myinfo.components.MyInfoAccount
import net.ib.mn.presentation.main.myinfo.components.MyInfoLevel
import net.ib.mn.presentation.main.myinfo.components.MyInfoHeart
import net.ib.mn.presentation.main.myinfo.components.MyInfoLinks

/**
 * MyInfo 페이지
 * MyInfoFragment와 동일한 UI 구성
 */
@Composable
fun MyInfoPage(
    modifier: Modifier = Modifier,
    viewModel: MyInfoPageViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()

    val userName by viewModel.userName.collectAsState()
    val profileImageUrl by viewModel.profileImageUrl.collectAsState()
    val level by viewModel.level.collectAsState()
    val favoriteIdolName by viewModel.favoriteIdolName.collectAsState()
    val favoriteIdolSubName by viewModel.favoriteIdolSubName.collectAsState()
    val levelProgress by viewModel.levelProgress.collectAsState()
    val levelUpText by viewModel.levelUpText.collectAsState()
    val totalExp by viewModel.totalExp.collectAsState()
    val subscriptionName by viewModel.subscriptionName.collectAsState()
    val hasNewFeed by viewModel.hasNewFeed.collectAsState()
    val heartCount by viewModel.heartCount.collectAsState()
    val strongHeart by viewModel.strongHeart.collectAsState()
    val weakHeart by viewModel.weakHeart.collectAsState()
    val diaCount by viewModel.diaCount.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background_100))
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .padding(bottom = 9.dp)
    ) {
        // MyInfo Account Section
        MyInfoAccount(
            userName = userName,
            profileImageUrl = profileImageUrl,
            level = level,
            favoriteIdolName = favoriteIdolName,
            favoriteIdolSubName = favoriteIdolSubName,
            subscriptionName = subscriptionName,
            hasNewFeed = hasNewFeed,
            onProfileClick = { /* TODO: Navigate to profile */ },
            onSubscriptionBadgeClick = { /* TODO: Navigate to subscription */ }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Level Progress Bar
        MyInfoLevel(
            level = level,
            progress = levelProgress,
            levelUpText = levelUpText,
            totalExp = totalExp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Heart & Diamond Info
        MyInfoHeart(
            heartCount = heartCount,
            strongHeart = strongHeart,
            weakHeart = weakHeart,
            diaCount = diaCount,
            onInfoClick = { /* TODO: Show currency info */ },
            onHistoryClick = { /* TODO: Navigate to history */ }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Links Section (비디오광고, 상점, 무료충전소)
        MyInfoLinks(
            videoAdHeartCount = 400, // TODO: Get from ViewModel
            onVideoAdClick = { /* TODO: Navigate to video ad */ },
            onStoreClick = { /* TODO: Navigate to store */ },
            onFreeChargeClick = { /* TODO: Navigate to free charge */ }
        )
    }
}

