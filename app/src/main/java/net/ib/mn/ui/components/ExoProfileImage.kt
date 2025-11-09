package net.ib.mn.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ib.mn.R
import net.ib.mn.util.BirthdayUtil
import net.ib.mn.util.DateTimeManager
import net.ib.mn.util.IdolImageUtil.toSecureUrl

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
 * - birthday 파라미터가 있고 오늘이 생일이면 좌측 상단에 생일 꼬깔 아이콘 표시
 *
 * @param imageUrl 이미지 URL (null 가능)
 * @param modifier Modifier (size, clip 등 추가 가능)
 * @param rank 순위 (디폴트 이미지 선택에 사용, 기본값 0)
 * @param birthday 생일 (형식: "YYYY-MM-DD", 예: "1990-12-25", null 가능)
 * @param contentDescription 이미지 설명
 * @param contentScale ContentScale (기본값: Crop)
 * @param useCircleClip 원형 클립 사용 여부 (기본값: true)
 * @param useGrayBackground 회색 배경 사용 여부 (기본값: true)
 * @param showBirthdayBadge 생일 꼬깔 표시 여부 (기본값: true, false로 설정하면 꼬깔 기능 비활성화)
 */
@Composable
fun ExoProfileImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    rank: Int = 0,
    birthday: String? = null,
    contentDescription: String = "프로필 이미지",
    contentScale: ContentScale = ContentScale.Crop,
    useCircleClip: Boolean = true,
    useGrayBackground: Boolean = true,
    showBirthdayBadge: Boolean = true
) {
    val context = LocalContext.current

    // 전역 KST 날짜 구독
    val currentKstDate by DateTimeManager.currentKstDate.collectAsState()

    // 생일 체크 (derivedStateOf로 캐싱하여 불필요한 재계산 방지)
    val isBirthday by remember(birthday) {
        derivedStateOf {
            if (showBirthdayBadge && !birthday.isNullOrBlank()) {
                BirthdayUtil.isBirthday(birthday, currentKstDate)
            } else {
                false
            }
        }
    }

    // ImageRequest 생성
    val imageModel = remember(imageUrl.toSecureUrl()) {
        ImageRequest.Builder(context)
            .data(imageUrl.toSecureUrl())
            .crossfade(true)
            .build()
    }

    // 디폴트 이미지 선택 (ExoTop3 방식)
    val defaultImageRes = if (rank % 2 == 0) {
        R.drawable.menu_profile_2
    } else {
        R.drawable.menu_profile_1
    }

    // Modifier 구성
    var finalModifier = modifier
    if (useCircleClip) {
        finalModifier = finalModifier.clip(CircleShape)
    }
    if (useGrayBackground) {
        finalModifier = finalModifier.background(ColorPalette.gray100)
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = imageModel,
            contentDescription = contentDescription,
            modifier = finalModifier,
            contentScale = contentScale,
            error = painterResource(defaultImageRes),
            placeholder = painterResource(defaultImageRes)
        )

        // 생일 꼬깔 아이콘 (좌측 상단 오버레이)
        if (isBirthday) {
            Image(
                painter = painterResource(R.drawable.icon_anniversary_birth),
                contentDescription = "생일",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 5.dp, top = 3.dp)
                    .size(36.dp)
            )
        }
    }
}
