package net.ib.mn.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R

/**
 * 하단 고정 상태 버튼
 *
 * 키보드가 올라오면 자동으로 위로 이동하며,
 * 키보드가 올라올 때만 NavigationBar 높이만큼 아래로 offset을 추가합니다.
 * enabled 상태에 따라 색상이 변경되고,
 * loading 상태에서는 로딩 인디케이터를 표시합니다.
 *
 * @param text 버튼 텍스트
 * @param onClick 클릭 이벤트
 * @param modifier Modifier (기본: Alignment.BottomCenter 필요)
 * @param enabled 활성화 여부 (기본: true)
 * @param isLoading 로딩 상태 (기본: false)
 */
@Composable
fun ExoStatusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    // 키보드 높이 감지
    val imeHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardVisible = imeHeight > 0.dp

    // NavigationBar 높이 계산
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .imePadding() // 키보드가 올라오면 자동으로 위로 이동
            .offset(y = if (isKeyboardVisible) navigationBarHeight else 0.dp) // 키보드 올라올 때만 NavBar 높이만큼 아래로
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
            .height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) colorResource(id = R.color.main) else colorResource(id = R.color.gray200),
            contentColor = Color.White,
            disabledContainerColor = colorResource(id = R.color.gray200),
            disabledContentColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 14.sp
            )
        }
    }
}
