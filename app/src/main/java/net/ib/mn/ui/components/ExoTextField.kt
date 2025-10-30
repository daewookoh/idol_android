package net.ib.mn.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.ui.theme.ColorPalette

/**
 * 프로젝트 전역에서 사용하는 공통 TextField 컴포넌트
 *
 * 기본 스타일:
 * - OutlinedTextField 기반
 * - 포커스 시: 메인 컬러 테두리
 * - 에러 시: 빨간색 테두리 및 에러 메시지 표시
 * - 비활성화 시: 회색 테두리
 *
 * @param value 입력 값
 * @param onValueChange 값 변경 콜백
 * @param modifier Modifier (기본: fillMaxWidth)
 * @param label 라벨 텍스트
 * @param placeholder 플레이스홀더 텍스트
 * @param enabled 활성화 여부 (기본: true)
 * @param readOnly 읽기 전용 여부 (기본: false)
 * @param isError 에러 상태 (기본: false)
 * @param errorMessage 에러 메시지 (isError가 true일 때 표시)
 * @param helperText 도움말 텍스트 (에러가 아닐 때 표시)
 * @param leadingIcon 앞쪽 아이콘
 * @param trailingIcon 뒤쪽 아이콘
 * @param visualTransformation 비주얼 변환 (비밀번호 마스킹 등)
 * @param keyboardType 키보드 타입 (기본: Text)
 * @param imeAction IME 액션 (기본: Next)
 * @param keyboardActions 키보드 액션
 * @param singleLine 한 줄 입력 여부 (기본: true)
 * @param maxLines 최대 줄 수 (기본: 1)
 */
@Composable
fun ExoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    helperText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = if (label != null) {
                { Text(label) }
            } else null,
            placeholder = if (placeholder != null) {
                { Text(placeholder) }
            } else null,
            enabled = enabled,
            readOnly = readOnly,
            isError = isError,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorPalette.main,
                unfocusedBorderColor = ColorPalette.gray300,
                errorBorderColor = Color.Red,
                disabledBorderColor = ColorPalette.gray200,
                focusedLabelColor = ColorPalette.main,
                unfocusedLabelColor = ColorPalette.textGray,
                errorLabelColor = Color.Red,
                cursorColor = ColorPalette.main,
                errorCursorColor = Color.Red
            )
        )

        // 에러 메시지 표시
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        // 도움말 텍스트 표시 (에러가 아닐 때만)
        if (!isError && helperText != null) {
            Text(
                text = helperText,
                color = ColorPalette.gray500,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
