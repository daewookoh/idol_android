package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.ib.mn.R

/**
 * old 프로젝트 SignupFragment 스타일의 TextField
 *
 * 주요 기능:
 * - 우측 아이콘 표시 (성공: join_approval, 실패: join_disapproval)
 * - 에러 메시지 표시 (supportingText로 표시)
 * - 실시간 검증을 위한 디바운싱 지원
 * - 포커스 변경 감지
 *
 * @param value 입력 값
 * @param onValueChange 값 변경 콜백
 * @param modifier Modifier
 * @param placeholder 플레이스홀더
 * @param enabled 활성화 여부
 * @param isValid 검증 성공 여부 (true면 체크 아이콘, null이면 아이콘 없음)
 * @param errorMessage 에러 메시지
 * @param visualTransformation 비주얼 변환
 * @param keyboardType 키보드 타입
 * @param imeAction IME 액션
 * @param onFocusChanged 포커스 변경 콜백
 * @param onValueChangeDebounced 디바운싱된 값 변경 콜백 (지연 시간 후 호출)
 * @param debounceMillis 디바운싱 지연 시간 (밀리초)
 */
@Composable
fun ExoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String? = null,
    enabled: Boolean = true,
    isValid: Boolean? = null,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    onValueChangeDebounced: ((String) -> Unit)? = null,
    debounceMillis: Long = 0
) {
    // 디바운싱 처리
    LaunchedEffect(value) {
        if (debounceMillis > 0 && onValueChangeDebounced != null) {
            delay(debounceMillis)
            onValueChangeDebounced(value)
        }
    }

    Column {
        // TextField 박스
        Row(
            modifier = modifier
                .background(
                    color = colorResource(id = R.color.background_300),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp)
                .then(
                    if (onFocusChanged != null) {
                        Modifier.onFocusChanged(onFocusChanged)
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BasicTextField
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        color = colorResource(id = R.color.text_default)
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = imeAction
                    ),
                    keyboardActions = keyboardActions,
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(colorResource(id = R.color.main))
                )

                // 플레이스홀더
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.text_dimmed)
                        )
                    )
                }
            }

            // 우측 아이콘
            when {
                isValid == true -> {
                    Icon(
                        painter = painterResource(id = R.drawable.join_approval),
                        contentDescription = "Valid",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Unspecified
                    )
                }
                isValid == false && errorMessage != null -> {
                    Icon(
                        painter = painterResource(id = R.drawable.join_disapproval),
                        contentDescription = "Invalid",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        }

        // 에러 메시지
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}
