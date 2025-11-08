package net.ib.mn.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.ib.mn.R
import net.ib.mn.ui.theme.ExoTypo

/**
 * 앱 전체에서 사용하는 공통 CheckBox 컴포넌트
 *
 * old 프로젝트의 checkbox 스타일을 따름:
 * - 체크박스 아이콘: checkbox_on/checkbox_off
 * - 체크박스와 텍스트 간격: 6dp
 * - 기본 padding: 상하 8dp
 * - 전체 동의일 때 padding: 상하 10dp
 *
 * @param checked 체크 상태
 * @param onCheckedChange 체크 상태 변경 콜백
 * @param text 텍스트 (String 또는 AnnotatedString)
 * @param modifier Modifier
 * @param isMain 전체 동의 여부 (true일 경우 볼드, 17sp)
 * @param trailingIcon 우측 아이콘 (옵션)
 * @param onTrailingIconClick 우측 아이콘 클릭 콜백 (옵션)
 * @param height 높이 (옵션, 기본값 wrap_content)
 */
@Composable
fun ExoCheckBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: Any, // String 또는 AnnotatedString
    modifier: Modifier = Modifier,
    isMain: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    height: Dp? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (height != null) it.height(height) else it }
            .padding(vertical = if (isMain) 10.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 체크박스와 텍스트
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(
                    id = if (checked) R.drawable.checkbox_on else R.drawable.checkbox_off
                ),
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(6.dp))

            when (text) {
                is String -> {
                    Text(
                        text = text,
                        style = if (isMain) ExoTypo.checkboxMain else ExoTypo.checkboxSub
                    )
                }
                is AnnotatedString -> {
                    Text(
                        text = text,
                        style = if (isMain) ExoTypo.checkboxMain else ExoTypo.checkboxSub
                    )
                }
            }
        }

        // 우측 아이콘 (있는 경우만)
        if (trailingIcon != null && onTrailingIconClick != null) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onTrailingIconClick() },
                contentAlignment = Alignment.Center
            ) {
                trailingIcon()
            }
        }
    }
}

/**
 * 우측에 화살표 아이콘이 있는 체크박스
 * (약관 상세보기용)
 */
@Composable
fun ExoCheckBoxWithDetail(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: Any, // String 또는 AnnotatedString
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp? = null
) {
    ExoCheckBox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        text = text,
        modifier = modifier,
        isMain = false,
        trailingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.btn_go),
                contentDescription = "상세보기"
            )
        },
        onTrailingIconClick = onDetailClick,
        height = height
    )
}
