package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 로딩 오버레이 컴포넌트
 * 화면 전체를 덮는 반투명 배경과 중앙 로딩 인디케이터를 표시합니다.
 * 
 * @param isLoading 로딩 상태
 * @param backgroundColor 배경색 (기본: 반투명 검정)
 * @param indicatorColor 로딩 인디케이터 색상 (기본: MaterialTheme.primary)
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
    indicatorColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = indicatorColor
            )
        }
    }
}

