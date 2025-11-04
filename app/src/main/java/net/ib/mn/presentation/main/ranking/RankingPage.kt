package net.ib.mn.presentation.main.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.presentation.main.MainViewModel
import net.ib.mn.util.ServerUrl

/**
 * Ranking 페이지
 *
 * Best Practice:
 * 1. BuildConfig.CELEB에 따라 UI 구조 분기
 *    - CELEB: Swiper(HorizontalPager)만 사용, 탭바 없음
 *    - 기타: Swiper + PrimaryScrollableTabRow 사용
 * 2. old 프로젝트의 SummaryMainFragment와 RankingPageFragment 구조 참고
 * 3. TypeListModel을 통한 동적 타입 처리
 */
@Composable
fun RankingPage(
    viewModel: RankingViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    // CELEB: typeList 사용
    // 일반: MainChartModel 사용 (old 프로젝트와 동일)
    val typeList by viewModel.typeList.collectAsState()
    val mainChartModel by viewModel.mainChartModel.collectAsState()

    // MainScreen에서 관리하는 성별 카테고리 (old 프로젝트와 동일)
    val defaultCategory by mainViewModel.preferencesManager.defaultCategory.collectAsState(initial = net.ib.mn.util.Constants.TYPE_MALE)
    val isMale = defaultCategory == net.ib.mn.util.Constants.TYPE_MALE

    // CELEB: typeList 확인
    // 일반: mainChartModel 확인
    if (BuildConfig.CELEB) {
        if (typeList.isEmpty()) {
            // 로딩 중
            return
        }
    } else {
        if (mainChartModel == null) {
            // 로딩 중
            return
        }
    }

    // CELEB: typeList 사용
    // 일반: old 프로젝트와 동일한 순서로 탭 생성
    //   1. 개인/그룹 차트 (MainChartModel.males 또는 females)
    //   2. MIRACLE, ROOKIE (objects에서)
    //   3. HEARTPICK, ONEPICK, HOF (하드코딩)
    val tabDataList = if (BuildConfig.CELEB) {
        typeList
    } else {
        android.util.Log.d("RankingPage", "========================================")
        android.util.Log.d("RankingPage", "[RankingPage] Building tab list")
        android.util.Log.d("RankingPage", "  - defaultCategory: $defaultCategory")
        android.util.Log.d("RankingPage", "  - isMale: $isMale")
        android.util.Log.d("RankingPage", "========================================")
        buildIdolAppTabList(mainChartModel, viewModel, isMale)
    }

    val subPagerState = rememberPagerState(pageCount = { tabDataList.size })
    val coroutineScope = rememberCoroutineScope()

    // TabRow의 스크롤 상태
    val tabScrollState = rememberScrollState()

    // 스크롤 상태에 따라 그라데이션 표시/숨김 제어
    val showLeftGradient by remember {
        derivedStateOf {
            tabScrollState.value > 0 // 스크롤이 0보다 크면 왼쪽 그라데이션 표시
        }
    }

    val showRightGradient by remember {
        derivedStateOf {
            tabScrollState.value < tabScrollState.maxValue // 스크롤이 끝까지 안 갔으면 오른쪽 그라데이션 표시
        }
    }

    // old 프로젝트 색상 (R.color 사용)
    val mainColor = colorResource(R.color.main)
    val textDimmedColor = colorResource(R.color.text_dimmed)
    val borderColor = colorResource(R.color.gray100)
    val backgroundColor = colorResource(R.color.background_100)
    val backgroundTransparent = colorResource(R.color.background_100_transparent)

    // 탭 이름 리스트 생성 (old 프로젝트 로직과 동일)
    val tabs = tabDataList.map { type ->
        // old 프로젝트 로직: 먼저 API name을 기본값으로 설정하고, S/A 타입인 경우에만 덮어씀
        var baseName = type.name // API에서 받은 다국어 이름 (기본값)

        android.util.Log.d("TAB_NAME", "type=${type.type}, name=${type.name}, isDivided=${type.isDivided}, isFemale=${type.isFemale}")

        if (BuildConfig.CELEB) {
            // CELEB 앱: 배우/가수 탭 처리 (old celeb flavor 로직)
            val typeCheck = if (type.isDivided == "N" && !type.isFemale) {
                null
            } else {
                if (type.isFemale) "F" else "M"
            }

            // S(Singer) 또는 A(Actor) 타입인 경우에만 성별에 따라 덮어씀
            when (type.type) {
                "S" -> { // Singer
                    baseName = when (typeCheck) {
                        "M" -> stringResource(R.string.actor_male_singer) // 남자 가수
                        "F" -> stringResource(R.string.actor_female_singer) // 여자 가수
                        else -> type.name // 서버에서 받은 이름
                    }
                }
                "A" -> { // Actor
                    baseName = when (typeCheck) {
                        "M" -> stringResource(R.string.lable_actors) // 배우
                        "F" -> stringResource(R.string.lable_actresses) // 여배우
                        else -> type.name // 서버에서 받은 이름
                    }
                }
                // 그 외(GLOBAL 등)는 이미 설정된 type.name 그대로 사용
            }
        } else {
            // 일반 IDOL 앱: 개인/그룹 등 고정 탭 처리 (old app flavor 로직)
            baseName = when (type.type) {
                "SOLO" -> stringResource(R.string.lable_individual) // 개인
                "GROUP" -> stringResource(R.string.lable_group) // 그룹
                "MIRACLE" -> stringResource(R.string.miracle) // 기적
                "ROOKIE" -> stringResource(R.string.rookie) // 루키
                "HEARTPICK" -> stringResource(R.string.heartpick) // 하트픽
                "ONEPICK" -> stringResource(R.string.onepick) // 원픽
                "HOF" -> stringResource(R.string.title_tab_hof) // 명예전당
                else -> type.name // API에서 받은 다국어 이름 (글로벌 등)
            }
        }

        android.util.Log.d("TAB_NAME", "Final baseName=$baseName")

        // baseurl에 www가 포함되지 않으면 개인, 그룹, 글로벌에 (테섭) 추가
        val finalName = if (ServerUrl.isTestServer() &&
            (type.type == "SOLO" || type.type == "GROUP" || type.type == "GLOBAL")) {
            "(테섭)$baseName(테섭)"
        } else {
            baseName
        }

        android.util.Log.d("TAB_NAME", "Final tab name=$finalName\n")
        finalName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // CELEB이 아닌 경우에만 탭바 표시
        if (!BuildConfig.CELEB) {
            // TabRow를 Box로 감싸서 하단 보더 및 양쪽 그라데이션과 함께 배치
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
            PrimaryScrollableTabRow (
                minTabWidth = 0.dp,
                scrollState = tabScrollState,
                selectedTabIndex = subPagerState.currentPage,
                containerColor = backgroundColor,
                contentColor = mainColor,
                edgePadding = 0.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, tabName ->
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                coroutineScope.launch {
                                    subPagerState.animateScrollToPage(index)
                                }
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (subPagerState.currentPage == index) mainColor else textDimmedColor,
                        )
                    }
                }
            }

            // 왼쪽 그라데이션 (스크롤이 왼쪽 끝이 아닐 때 표시)
            if (showLeftGradient) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(48.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(backgroundColor, backgroundTransparent)
                            )
                        )
                )
            }

            // 오른쪽 그라데이션 (스크롤이 오른쪽 끝이 아닐 때 표시)
            if (showRightGradient) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(48.dp)
                        .align(Alignment.CenterEnd)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(backgroundTransparent, backgroundColor)
                            )
                        )
                )
            }
            }
        }

        // 탭별 컨텐츠 (type에 따라 동적으로 SubPage 표시)
        // CELEB: 순수 Swiper, 기타: Swiper (탭과 연동)
        HorizontalPager(
            state = subPagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            key = { pageIndex ->
                // tabDataList가 변경되면 페이지를 재생성하도록 key 사용
                tabDataList.getOrNull(pageIndex)?.code ?: "page_$pageIndex"
            }
        ) { pageIndex ->
            // pageIndex에 해당하는 type 정보 가져오기
            val currentType = tabDataList.getOrNull(pageIndex)
            if (currentType != null) {
                RankingSubPage(
                    type = currentType,
                    isVisible = subPagerState.currentPage == pageIndex  // 현재 페이지만 visible
                )
            }
        }
    }
}

/**
 * 일반 앱의 탭 리스트 생성 (old 프로젝트와 동일)
 *
 * 순서:
 * 1. 개인/그룹 차트 (MainChartModel.males 또는 females)
 * 2. MIRACLE, ROOKIE (objects에서 type으로 찾기)
 * 3. HEARTPICK, ONEPICK, HOF (하드코딩)
 */
@Composable
private fun buildIdolAppTabList(
    mainChartModel: net.ib.mn.data.remote.dto.MainChartModel?,
    viewModel: RankingViewModel,
    isMale: Boolean
): List<net.ib.mn.data.model.TypeListModel> {
    val tabList = mutableListOf<net.ib.mn.data.model.TypeListModel>()

    // 1. 개인/그룹 차트 (성별에 따라 males/females 선택)
    val chartList = if (isMale) mainChartModel?.males else mainChartModel?.females
    chartList?.forEach { chartInfo ->
        val typeListModel = net.ib.mn.data.model.TypeListModel(
            id = 0,
            name = chartInfo.name ?: "",
            type = extractTypeFromCode(chartInfo.code ?: ""),
            code = chartInfo.code, // 원본 code 저장 (예: "SOLO_M", "PR_G_M")
            isDivided = "N",
            isFemale = !isMale,
            showDivider = false
        )
        tabList.add(typeListModel)
    }

    // 2. objects에서 MIRACLE, ROOKIE 등 추가 (old 프로젝트와 동일)
    val chartObjects = viewModel.configRepository.getChartObjects()
    chartObjects?.forEach { chart ->
        when (chart.type) {
            "M" -> { // MIRACLE (기적)
                // code에서 성별 추출 (예: "PR_G_M" -> M, "PR_S_F" -> F)
                val isFemaleFromCode = chart.code?.endsWith("_F") == true

                tabList.add(
                    net.ib.mn.data.model.TypeListModel(
                        id = 0,
                        name = "MIRACLE",
                        type = "MIRACLE",
                        code = chart.code, // API의 원본 code 사용 (예: "PR_G_M", "PR_S_F")
                        isDivided = "N",
                        isFemale = isFemaleFromCode,
                        showDivider = false
                    )
                )
            }
            "R" -> { // ROOKIE (루키)
                // code에서 성별 추출 (예: "PR_G_M" -> M, "PR_S_F" -> F)
                val isFemaleFromCode = chart.code?.endsWith("_F") == true

                tabList.add(
                    net.ib.mn.data.model.TypeListModel(
                        id = 0,
                        name = "ROOKIE",
                        type = "ROOKIE",
                        code = chart.code, // API의 원본 code 사용
                        isDivided = "N",
                        isFemale = isFemaleFromCode,
                        showDivider = false
                    )
                )
            }
        }
    }

    // 3. 하드코딩 메뉴 (old 프로젝트와 동일)
    // HEARTPICK
    tabList.add(
        net.ib.mn.data.model.TypeListModel(
            id = 0,
            name = "HEARTPICK",
            type = "HEARTPICK",
            isDivided = "N",
            isFemale = false,
            showDivider = false
        )
    )

    // ONEPICK
    tabList.add(
        net.ib.mn.data.model.TypeListModel(
            id = 0,
            name = "ONEPICK",
            type = "ONEPICK",
            isDivided = "N",
            isFemale = false,
            showDivider = false
        )
    )

    // HOF (명예의 전당)
    tabList.add(
        net.ib.mn.data.model.TypeListModel(
            id = 0,
            name = "HOF",
            type = "HOF",
            isDivided = "N",
            isFemale = false,
            showDivider = false
        )
    )

    return tabList
}

/**
 * 차트 코드에서 타입 추출
 *
 * 예: "SOLO_M" -> "SOLO", "GROUP_F" -> "GROUP"
 */
private fun extractTypeFromCode(code: String): String {
    return when {
        code.startsWith("SOLO") -> "SOLO"
        code.startsWith("GROUP") -> "GROUP"
        else -> code
    }
}
