package net.ib.mn.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import net.ib.mn.R

/**
 * 앱 전체에서 사용하는 공통 AppBar 컴포넌트
 *
 * old 프로젝트의 ActionBar 스타일을 따름:
 * - 높이: 56dp
 * - 타이틀 텍스트 크기: 20sp
 * - 백 버튼 아이콘 크기: 24dp
 *
 * @param title AppBar 타이틀
 * @param navigationIcon 네비게이션 아이콘 (기본값: 백 버튼)
 * @param onNavigationClick 네비게이션 아이콘 클릭 리스너
 * @param actions AppBar 우측 액션 버튼들
 * @param colors AppBar 색상
 */
@Composable
fun ExoAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors? = null
) {
    val defaultColors = TopAppBarDefaults.topAppBarColors(
        containerColor = colorResource(id = R.color.background_100),
        titleContentColor = colorResource(id = R.color.text_default),
        navigationIconContentColor = colorResource(id = R.color.text_default),
        actionIconContentColor = colorResource(id = R.color.text_default)
    )

    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (navigationIcon != null) {
                navigationIcon()
            } else if (onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.btn_navigation_back),
                        contentDescription = "뒤로가기"
                    )
                }
            }
        },
        actions = actions,
        colors = colors ?: defaultColors
    )
}

