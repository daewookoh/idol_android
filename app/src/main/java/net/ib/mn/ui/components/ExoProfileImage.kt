package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.IdolImageUtil.toSecureUrl

/**
 * 프로필 이미지 타입별 사이즈
 *
 * CIRCLE 타입: 테두리 이미지 포함 (miracleCount, fairyCount, angelCount 기반)
 * 일반 타입: 테두리 없이 이미지만
 */
object ProfileImageType {
    const val XLARGE = "XLARGE"     // 아이돌 다이얼로그, 하트픽 상세 1위: 전체 90dp(테두리 없음)
    const val XLARGE_SQUARE = "XLARGE_SQUARE"   // 테마픽 상세 1위: 90dp, radius 10 (사각형)
    const val LARGE_CIRCLE = "LARGE_CIRCLE"     // 메인 랭킹: 전체 77dp, 테두리 60dp, 이미지 50dp
    const val LARGE = "LARGE"     // 하트픽1위: 전체 70dp(테두리 없음)
    const val MEDIUM_CIRCLE = "MEDIUM_CIRCLE"   // 기적/루키: 전체 62dp, 테두리 52dp, 이미지 42dp
    const val MEDIUM = "MEDIUM"                 // 하트픽: 55dp (테두리 없음)
    const val MEDIUM_SQUARE = "MEDIUM_SQUARE"   // 테마픽 상세 2위 이하: 55dp, radius 10 (사각형)
    const val SMALL_CIRCLE = "SMALL_CIRCLE"     // 명예의 전당: 45dp (테두리 없음, 원형)
    const val SMALL = "SMALL"                   // 기본: 40dp (테두리 없음)
    const val XSMALL = "XSMALL"                 // VoterTop100: 35dp (테두리 없음)
}

/**
 * 기념일 배지 설정 데이터 클래스
 *
 * old 프로젝트 아이콘 크기:
 * - birth/debut (medium): 126px = 42dp
 * - allinday (medium): 72px = 24dp (모든 프로필에서 동일)
 */
private data class AnniversaryBadgeConfig(
    val birthDebutSize: Dp,
    val badgeOffsetX: Dp,
    val badgeOffsetY: Dp,
    val badgePaddingTop: Dp,
    val memorialDayPadding: Modifier,
    val memorialDayOffsetX: Dp,
    val memorialDayOffsetY: Dp,
    // 몰빵일 전용 offset (old 프로젝트 위치와 동일하게)
    val allInDayOffsetX: Dp,
    val allInDayOffsetY: Dp
)

/**
 * ExoProfileImage - 공용 프로필 이미지 컴포넌트
 *
 * ExoTop3 방식의 디폴트 이미지 처리를 적용한 AsyncImage wrapper
 *
 * 특징:
 * - 이미지 로드 실패 시 순위에 따라 menu_profile_1 또는 menu_profile_2 표시
 * - 짝수 순위: menu_profile_2
 * - 홀수 순위: menu_profile_1
 * - Circular 클립과 회색 배경 기본 적용
 * - 기념일 배지 지원 (타입별 자동 조정)
 * - MAIN 타입: 테두리 이미지 포함 (miracleCount, fairyCount, angelCount 기반)
 *
 * @param imageUrl 이미지 URL (null 가능)
 * @param modifier Modifier (추가 스타일 적용)
 * @param type 프로필 이미지 타입 (LARGE_CIRCLE/MEDIUM_CIRCLE/MEDIUM/SMALL_CIRCLE/SMALL)
 * @param rank 순위 (디폴트 이미지 선택에 사용, 기본값 0)
 * @param contentDescription 이미지 설명
 * @param contentScale ContentScale (기본값: Crop)
 * @param useCircleClip 원형 클립 사용 여부 (기본값: true)
 * @param useGrayBackground 회색 배경 사용 여부 (기본값: true)
 * @param anniversary 기념일 코드 (Y=생일, E=데뷔, C=컴백, D=기념일, B=몰빵일, N=없음)
 * @param anniversaryDays 기념일 일수 (D 코드일 때 사용, 기본값: 0)
 * @param idolType 아이돌 타입 ("S"=솔로, "G"=그룹, 기본값: "S")
 * @param miracleCount 미라클 카운트 (MAIN 타입 테두리용, 기본값: 0)
 * @param fairyCount 페어리 카운트 (MAIN 타입 테두리용, 기본값: 0)
 * @param angelCount 엔젤 카운트 (MAIN 타입 테두리용, 기본값: 0)
 */
@Composable
fun ExoProfileImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    type: String = ProfileImageType.LARGE_CIRCLE,
    rank: Int = 0,
    contentDescription: String = "프로필 이미지",
    contentScale: ContentScale = ContentScale.Crop,
    useCircleClip: Boolean = true,
    useGrayBackground: Boolean = true,
    anniversary: String = "N",
    anniversaryDays: Int = 0,
    idolType: String = "S",
    miracleCount: Int = 0,
    fairyCount: Int = 0,
    angelCount: Int = 0
) {
    // 타입별 사이즈
    // CIRCLE 타입: (전체 Box, 테두리, 이미지) - 테두리 포함
    // 일반 타입: (전체 Box, 0, 이미지) - 테두리 없음
    val (boxSize, borderSize, imageSize) = remember(type) {
        when (type) {
            ProfileImageType.XLARGE -> Triple(90.dp, 0.dp, 90.dp)          // 아이돌 다이얼로그, 하트픽 상세 1위
            ProfileImageType.XLARGE_SQUARE -> Triple(90.dp, 0.dp, 90.dp)   // 테마픽 상세 (radius 10)
            ProfileImageType.LARGE_CIRCLE -> Triple(77.dp, 60.dp, 50.dp)   // 메인 랭킹
            ProfileImageType.MEDIUM_CIRCLE -> Triple(62.dp, 52.dp, 42.dp)  // 기적/루키
            ProfileImageType.LARGE -> Triple(77.dp, 0.dp, 77.dp)          // 하트픽
            ProfileImageType.MEDIUM -> Triple(55.dp, 0.dp, 55.dp)          // 하트픽
            ProfileImageType.MEDIUM_SQUARE -> Triple(55.dp, 0.dp, 55.dp)   // 테마픽 2위 이하 (radius 10)
            ProfileImageType.SMALL_CIRCLE -> Triple(45.dp, 0.dp, 45.dp)    // 명예의 전당
            ProfileImageType.SMALL -> Triple(40.dp, 0.dp, 40.dp)           // 기본
            ProfileImageType.XSMALL -> Triple(35.dp, 0.dp, 35.dp)          // VoterTop100
            else -> Triple(50.dp, 0.dp, 50.dp)
        }
    }

    val context = LocalContext.current

    // ImageRequest 생성
    val imageModel = remember(imageUrl.toSecureUrl()) {
        ImageRequest.Builder(context)
            .data(imageUrl.toSecureUrl())
            .crossfade(true)
            .build()
    }

    // 디폴트 이미지 선택 (ExoTop3 방식)
    val defaultImageRes = if (rank % 2 == 0) {
        R.drawable.menu_profile_1
    } else {
        R.drawable.menu_profile_2
    }

    // CIRCLE 타입: 테두리 포함
    if (type == ProfileImageType.LARGE_CIRCLE || type == ProfileImageType.MEDIUM_CIRCLE) {
        // 테두리 이미지 결정 (old 프로젝트의 flag 기반 로직)
        val borderDrawable = remember(miracleCount, fairyCount, angelCount) {
            var flag = 0
            if (miracleCount >= 1) flag += 1
            if (fairyCount >= 1) flag += 2
            if (angelCount >= 1) flag += 4

            when (flag) {
                0 -> R.drawable.profile_round_off
                1 -> R.drawable.profile_round_miracle
                2 -> R.drawable.profile_round_fairy
                3 -> R.drawable.profile_round_fairy_miracle
                4 -> R.drawable.profile_round_angel
                5 -> R.drawable.profile_round_angel_miracle
                6 -> R.drawable.profile_round_angel_fairy
                7 -> R.drawable.profile_round_angel_fairy_miracle
                else -> R.drawable.profile_round_off
            }
        }

        Box(
            modifier = modifier
                .size(boxSize)
                .then(
                    if (type == ProfileImageType.MEDIUM_CIRCLE) Modifier.padding(horizontal = 5.dp)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // 테두리 PNG 이미지
            Icon(
                painter = painterResource(borderDrawable),
                contentDescription = "Profile border",
                modifier = Modifier.size(borderSize),
                tint = Color.Unspecified
            )

            // 프로필 이미지 (테두리 안에 배치)
            Box(
                modifier = Modifier.size(imageSize),
                contentAlignment = Alignment.Center
            ) {
                var imageModifier = Modifier.fillMaxSize()
                if (useCircleClip) {
                    imageModifier = imageModifier.clip(CircleShape)
                }
                if (useGrayBackground) {
                    imageModifier = imageModifier.background(ColorPalette.gray100)
                }

                AsyncImage(
                    model = imageModel,
                    contentDescription = contentDescription,
                    modifier = imageModifier,
                    contentScale = contentScale,
                    error = painterResource(defaultImageRes),
                )
            }

            // 기념일 배지 (N이 아닐 때만 표시)
            if (anniversary != "N") {
                AnniversaryBadge(
                    anniversary = anniversary,
                    anniversaryDays = anniversaryDays,
                    idolType = idolType,
                    type = type
                )
            }
        }
    } else {
        // 기타 타입: 테두리 없이 이미지만
        var imageModifier = Modifier.fillMaxSize()
        // SQUARE 타입들: radius 10의 사각형
        if (type == ProfileImageType.XLARGE_SQUARE || type == ProfileImageType.MEDIUM_SQUARE) {
            imageModifier = imageModifier.clip(RoundedCornerShape(10.dp))
        } else if (useCircleClip) {
            imageModifier = imageModifier.clip(CircleShape)
        }
        if (useGrayBackground) {
            imageModifier = imageModifier.background(ColorPalette.gray100)
        }

        Box(modifier = modifier.size(boxSize)) {
            AsyncImage(
                model = imageModel,
                contentDescription = contentDescription,
                modifier = imageModifier,
                contentScale = contentScale,
                error = painterResource(defaultImageRes),
            )

            // 기념일 배지 (N이 아닐 때만 표시)
            if (anniversary != "N") {
                AnniversaryBadge(
                    anniversary = anniversary,
                    anniversaryDays = anniversaryDays,
                    idolType = idolType,
                    type = type
                )
            }
        }
    }
}

/**
 * AnniversaryBadge - 기념일 배지 내부 컴포넌트
 *
 * API 코드:
 * - Y: 생일 (Birthday) - 빨간/노란 꼬깔 (그룹은 데뷔 아이콘)
 * - E: 데뷔 (dEbut) - 파란/보라 꼬깔
 * - C: 컴백 (Comeback) - 마이크
 * - D: 기념일 (memorial Day) - N일 텍스트 박스
 *
 * 타입별 스펙:
 * - LARGE_CIRCLE: 48dp 배지, offset(-7, 0), paddingTop=7dp
 * - MEDIUM_CIRCLE: 48dp 배지, offset(-15, -10), paddingTop=0dp
 * - SMALL_CIRCLE/SMALL: 36dp 배지
 */
@Composable
private fun BoxScope.AnniversaryBadge(
    anniversary: String,
    anniversaryDays: Int,
    idolType: String,
    type: String
) {
    // 타입별 배지 크기 및 위치
    // old 프로젝트 몰빵 위치:
    // - s_ranking_item (LARGE): marginStart=14dp, marginTop=10dp
    // - ranking_item (MEDIUM): marginStart=15dp, marginTop=5dp
    // - community_header (SMALL): marginStart=15dp, marginTop=7dp
    val badgeConfig = remember(type) {
        when (type) {
            ProfileImageType.LARGE_CIRCLE -> AnniversaryBadgeConfig(
                birthDebutSize = 48.dp,
                badgeOffsetX = (-7).dp,
                badgeOffsetY = 0.dp,
                badgePaddingTop = 7.dp,
                memorialDayPadding = Modifier,
                memorialDayOffsetX = (-10).dp,
                memorialDayOffsetY = (-10).dp,
                allInDayOffsetX = 2.dp,
                allInDayOffsetY = 5.dp
            )
            ProfileImageType.MEDIUM_CIRCLE -> AnniversaryBadgeConfig(
                birthDebutSize = 40.dp,
                badgeOffsetX = (-14).dp,
                badgeOffsetY = 0.dp,
                badgePaddingTop = 0.dp,
                memorialDayPadding = Modifier,
                memorialDayOffsetX = 15.dp,
                memorialDayOffsetY = 5.dp,
                allInDayOffsetX = (-6).dp,
                allInDayOffsetY = 3.dp
            )
            ProfileImageType.SMALL_CIRCLE, ProfileImageType.SMALL -> AnniversaryBadgeConfig(
                birthDebutSize = 36.dp,
                badgeOffsetX = (-7).dp,
                badgeOffsetY = 0.dp,
                badgePaddingTop = 7.dp,
                memorialDayPadding = Modifier.padding(end = 7.dp, bottom = 11.dp),
                memorialDayOffsetX = (-10).dp,
                memorialDayOffsetY = (-10).dp,
                allInDayOffsetX = 0.dp,
                allInDayOffsetY = 2.dp
            )
            else -> AnniversaryBadgeConfig(
                birthDebutSize = 48.dp,
                badgeOffsetX = (-7).dp,
                badgeOffsetY = 0.dp,
                badgePaddingTop = 7.dp,
                memorialDayPadding = Modifier,
                memorialDayOffsetX = (-10).dp,
                memorialDayOffsetY = (-10).dp,
                allInDayOffsetX = 0.dp,
                allInDayOffsetY = 5.dp
            )
        }
    }

    val birthDebutSize = badgeConfig.birthDebutSize
    val badgeOffsetX = badgeConfig.badgeOffsetX
    val badgeOffsetY = badgeConfig.badgeOffsetY
    val badgePaddingTop = badgeConfig.badgePaddingTop
    val memorialDayPadding = badgeConfig.memorialDayPadding
    val memorialDayOffsetX = badgeConfig.memorialDayOffsetX
    val memorialDayOffsetY = badgeConfig.memorialDayOffsetY
    val allInDayOffsetX = badgeConfig.allInDayOffsetX
    val allInDayOffsetY = badgeConfig.allInDayOffsetY

    val comebackWidth = 66.dp
    val comebackHeight = 56.dp

    when (anniversary) {
        "Y" -> {  // 생일 (Birthday) - 솔로는 생일, 그룹은 데뷔 아이콘
            val iconRes = if (idolType == "S") {
                R.drawable.icon_anniversary_birth_medium
            } else {
                R.drawable.icon_anniversary_debut_medium
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = if (idolType == "S") "생일" else "데뷔일",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(birthDebutSize)
                    .align(Alignment.TopStart)
                    .offset(x = badgeOffsetX, y = badgeOffsetY)
                    .padding(top = badgePaddingTop)
            )
        }
        "E" -> {  // 데뷔 (dEbut)
            Icon(
                painter = painterResource(R.drawable.icon_anniversary_debut_medium),
                contentDescription = "데뷔일",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(birthDebutSize)
                    .align(Alignment.TopStart)
                    .offset(x = badgeOffsetX, y = badgeOffsetY)
                    .padding(top = badgePaddingTop)
            )
        }
        "C" -> {  // 컴백 (Comeback)
            Icon(
                painter = painterResource(R.drawable.icon_anniversary_comeback_medium),
                contentDescription = "컴백일",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(width = comebackWidth, height = comebackHeight)
                    .align(Alignment.TopStart)
                    .padding(top = 12.dp)
            )
        }
        "D" -> {  // 기념일 (memorial Day)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = memorialDayOffsetX, y = memorialDayOffsetY)
                    .then(memorialDayPadding)
                    .background(
                        color = ColorPalette.main,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${anniversaryDays}${stringResource(R.string.lable_day)}",
                    style = ExoTypo.typo7Bold.copy(
                        color = ColorPalette.textWhiteBlack
                    )
                )
            }
        }
        "B" -> {  // 몰빵일 (All-In Day / Burning Day)
            // 모든 프로필 이미지 사이즈에서 24dp로 통일
            Icon(
                painter = painterResource(R.drawable.icon_anniversary_allinday_medium),
                contentDescription = "몰빵일",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopStart)
                    .offset(x = allInDayOffsetX, y = allInDayOffsetY)
            )
        }
    }
}
