package net.ib.mn.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R

/**
 * 검색 바 컴포넌트
 *
 * 검색 화면(SearchScreen)과 검색 결과 화면(SearchResultScreen)에서 공통으로 사용
 * old 프로젝트의 search_toolbar.xml UI를 Compose로 구현
 *
 * @param searchQuery 현재 검색어
 * @param onSearchQueryChange 검색어 변경 콜백
 * @param onSearch 검색 실행 콜백
 * @param onNavigateBack 뒤로가기 콜백
 * @param onCancel 취소 버튼 콜백 (null이면 검색어 리셋만 수행)
 * @param focusRequester 포커스 요청자 (선택)
 * @param onFocusChange 포커스 상태 변경 콜백 (Old 프로젝트의 KeyboardVisibilityUtil 대체)
 * @param readOnly 읽기 전용 모드 (검색 결과 화면에서 사용)
 * @param onClick 클릭 콜백 (읽기 전용 모드에서 사용)
 * @param modifier Modifier
 */
@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.navigation_bar))
            .statusBarsPadding()
            .height(56.dp)
            .padding(start = 5.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 뒤로가기 버튼
        Box(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onNavigateBack() }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.btn_navigation_back),
                contentDescription = "Back",
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(5.dp))

        // 검색 입력창 (검색 버튼 포함)
        Row(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colorResource(id = R.color.background_300))
                .then(
                    if (readOnly && onClick != null) {
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onClick() }
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 텍스트 입력 영역
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (focusRequester != null) {
                                Modifier.focusRequester(focusRequester)
                            } else {
                                Modifier
                            }
                        )
                        .onFocusChanged { focusState ->
                            onFocusChange?.invoke(focusState.isFocused)
                        },
                    textStyle = TextStyle(
                        color = colorResource(id = R.color.text_default),
                        fontSize = 12.sp
                    ),
                    singleLine = true,
                    readOnly = readOnly,
                    enabled = !readOnly,
                    cursorBrush = SolidColor(colorResource(id = R.color.text_default)),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearch() }
                    )
                )
            }

            // 검색 버튼 (입력창 안쪽 오른쪽)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .padding(end = 5.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSearch() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.btn_navigation_search),
                    contentDescription = "Search",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 취소 버튼
        Text(
            text = stringResource(id = R.string.btn_cancel),
            color = colorResource(id = R.color.toolbar_default),
            fontSize = 14.sp,
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (onCancel != null) {
                        onCancel()
                    } else {
                        // 기본 동작: 검색어 리셋
                        onSearchQueryChange("")
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
