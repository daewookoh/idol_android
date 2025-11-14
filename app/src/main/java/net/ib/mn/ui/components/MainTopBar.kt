package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R

/**
 * 메인 화면의 TopBar 컴포넌트
 * old 프로젝트의 main_toolbar.xml과 동일한 구조
 */
@Composable
fun MainTopBar(
    timerText: String,
    showToggleButton: Boolean = false,
    showMainMenu: Boolean = true, // main_toolbar_menu (검색, 친구)
    showMyInfoMenu: Boolean = false, // myinfo_toolbar_menu (출석, 알림, 설정)
    onToggleClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onFriendsClick: (() -> Unit)? = null,
    onAttendanceClick: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
    onSettingClick: (() -> Unit)? = null,
    toggleButton: @Composable () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val fontScale = configuration.fontScale
    
    // OS 텍스트 크기에 따라 폰트 크기 조정 (기본 13sp)
    val timerFontSize = (16 * fontScale).sp
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(56.dp)
            .background(colorResource(id = R.color.navigation_bar))
    ) {
        // 중앙에 타이머 표시 (old 프로젝트의 tv_deadline과 동일 - 화면 중앙)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timerText,
                fontSize = timerFontSize,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.toolbar_default),
                textAlign = TextAlign.Center
            )
        }
        
        // 왼쪽: Toggle 버튼 (항상 렌더링하되 visible만 제어)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 13.dp)
                .width(130.dp)
                .alpha(if (showToggleButton) 1f else 0f) // 보이지 않게
        ) {
            toggleButton()
            
            // 클릭 방지용 투명 오버레이
            if (!showToggleButton) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            // 클릭 이벤트를 소비하여 하위 컴포넌트로 전달되지 않도록 함
                        }
                )
            }
        }
        
        // 오른쪽: 메뉴 버튼들
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showMyInfoMenu) {
                // MyInfo 메뉴 (출석, 알림, 설정)
                MyInfoToolbarMenu(
                    onAttendanceClick = onAttendanceClick,
                    onNotificationClick = onNotificationClick,
                    onSettingClick = onSettingClick
                )
            } else if (showMainMenu) {
                // Main 메뉴 (검색, 친구)
                MainToolbarMenu(
                    onSearchClick = onSearchClick,
                    onFriendsClick = onFriendsClick
                )
            }
        }
    }
}

/**
 * Main Toolbar Menu (검색, 친구)
 * old 프로젝트의 main_toolbar_menu.xml과 동일
 * - 검색 버튼: marginEnd="12dp"
 * - 친구 버튼: marginEnd="16dp"
 */
@Composable
private fun MainToolbarMenu(
    onSearchClick: (() -> Unit)? = null,
    onFriendsClick: (() -> Unit)? = null
) {
    // 검색 버튼 (old: marginEnd="12dp")
    Box(
        modifier = Modifier
            .clickable { onSearchClick?.invoke() }
            .padding(5.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.btn_navigation_search),
            contentDescription = "검색",
            modifier = Modifier.size(22.dp)
        )
    }

    // 친구 버튼 (old: marginEnd="16dp")
    Box(
        modifier = Modifier
            .clickable { onFriendsClick?.invoke() }
            .padding(5.dp)
            .padding(end = 12.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.btn_navigation_friend),
            contentDescription = "친구",
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * MyInfo Toolbar Menu (출석, 알림, 설정)
 * old 프로젝트의 myinfo_toolbar_menu.xml과 동일
 * - 출석 버튼: marginEnd="12dp" (기본적으로 숨김)
 * - 알림 버튼: marginEnd="12dp"
 * - 설정 버튼: marginEnd="16dp"
 */
@Composable
private fun MyInfoToolbarMenu(
    onAttendanceClick: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
    onSettingClick: (() -> Unit)? = null
) {
    // 출석 체크 버튼 (기본적으로 숨김, old: marginEnd="12dp")
//    Box(
//        modifier = Modifier
//            .clickable { onAttendanceClick?.invoke() }
//            .padding(5.dp)
//    ) {
//        Icon(
//            painter = painterResource(id = R.drawable.btn_navigation_attendance),
//            contentDescription = "출석체크",
//            modifier = Modifier.size(22.dp)
//        )
//    }

    // 알림 버튼 (old: marginEnd="12dp")
    Box(
        modifier = Modifier
            .clickable { onNotificationClick?.invoke() }
            .padding(5.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.btn_navigation_bell),
            contentDescription = "알림",
            modifier = Modifier.size(22.dp)
        )
    }

    // 설정 버튼 (old: marginEnd="16dp")
    Box(
        modifier = Modifier
            .clickable { onSettingClick?.invoke() }
            .padding(5.dp)
            .padding(end = 12.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.btn_navigation_setting),
            contentDescription = "설정",
            modifier = Modifier.size(22.dp)
        )
    }
}
