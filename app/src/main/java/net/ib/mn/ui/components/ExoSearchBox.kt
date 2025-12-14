package net.ib.mn.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

/**
 * 검색 박스 컴포넌트
 *
 * 주요 기능:
 * - 검색 텍스트 입력
 * - 포커스 상태에 따른 돋보기 아이콘 표시/숨김
 *   - 포커스가 없을 때: 돋보기 아이콘 숨김
 *   - 포커스가 있을 때: 돋보기 아이콘 표시
 * - 키보드의 검색 버튼 지원
 *
 * @param value 입력 값
 * @param onValueChange 값 변경 콜백
 * @param onSearch 검색 실행 콜백
 * @param modifier Modifier
 * @param placeholder 플레이스홀더 텍스트
 * @param enabled 활성화 여부
 */
@Composable
fun ExoSearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .height(38.dp)
            .background(
                color = ColorPalette.background300,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(start = 20.dp, end = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            enabled = enabled,
            textStyle = ExoTypo.typo12.copy(
                color = ColorPalette.textDefault
            ),
            cursorBrush = SolidColor(ColorPalette.main),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = ExoTypo.typo12.copy(
                            color = ColorPalette.textDimmed
                        )
                    )
                }
                innerTextField()
            }
        )

        // 돋보기 아이콘 - 포커스가 있을 때만 표시
        AnimatedVisibility(
            visible = isFocused,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = onSearch,
                modifier = Modifier.size(38.dp),
                enabled = enabled
            ) {
                Icon(
                    painter = painterResource(R.drawable.btn_navigation_search),
                    contentDescription = "Search",
                    tint = ColorPalette.textGray
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExoSearchBoxPreview() {
    var text by remember { mutableStateOf("") }

    ExoSearchBox(
        value = text,
        onValueChange = { text = it },
        onSearch = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = "검색어를 입력하세요"
    )
}

@Preview(showBackground = true)
@Composable
fun ExoSearchBoxWithTextPreview() {
    ExoSearchBox(
        value = "검색어",
        onValueChange = {},
        onSearch = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = "검색어를 입력하세요"
    )
}
