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
import net.ib.mn.presentation.main.myinfo.components.MyInfoAccountSection
import net.ib.mn.presentation.main.myinfo.components.MyInfoLevelProgressBar

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
    val subscriptionName by viewModel.subscriptionName.collectAsState()
    val hasNewFeed by viewModel.hasNewFeed.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background_100))
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .padding(bottom = 9.dp)
    ) {
        // MyInfo Account Section
        MyInfoAccountSection(
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

        // Level Progress Bar
        MyInfoLevelProgressBar(
            level = level,
            progress = levelProgress,
            levelUpText = levelUpText,
            modifier = Modifier
                .widthIn(max = 160.dp)
                .padding(start = 90.dp) // 10dp(outer) + 60dp(profile) + 20dp(spacer)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Menu Section will be added here
    }
}

