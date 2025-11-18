package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ib.mn.R
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
 *
 * @param imageUrl 이미지 URL (null 가능)
 * @param modifier Modifier (size, clip 등 추가 가능)
 * @param rank 순위 (디폴트 이미지 선택에 사용, 기본값 0)
 * @param contentDescription 이미지 설명
 * @param contentScale ContentScale (기본값: Crop)
 * @param useCircleClip 원형 클립 사용 여부 (기본값: true)
 * @param useGrayBackground 회색 배경 사용 여부 (기본값: true)
 */
@Composable
fun ExoProfileImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    rank: Int = 0,
    contentDescription: String = "프로필 이미지",
    contentScale: ContentScale = ContentScale.Crop,
    useCircleClip: Boolean = true,
    useGrayBackground: Boolean = true
) {
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

    // Modifier 구성
    var finalModifier = modifier
        .aspectRatio(1f)  // 항상 정사각형 비율 유지 (가로:세로 = 1:1)
    if (useCircleClip) {
        finalModifier = finalModifier.clip(CircleShape)
    }
    if (useGrayBackground) {
        finalModifier = finalModifier.background(ColorPalette.gray100)
    }

    AsyncImage(
        model = imageModel,
        contentDescription = contentDescription,
        modifier = finalModifier,
        contentScale = contentScale,
        error = painterResource(defaultImageRes),
//        placeholder = painterResource(defaultImageRes)
    )
}
