package net.ib.mn.ui.components

import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import kotlinx.coroutines.delay
import net.ib.mn.R

/**
 * old 프로젝트 SignupFragment 스타일의 TextField
 *
 * 주요 기능:
 * - 우측 아이콘 표시 (성공: join_approval, 실패: join_disapproval)
 * - 에러 메시지 표시 (EditText.setError()를 사용한 말풍선 형태)
 * - 실시간 검증을 위한 디바운싱 지원
 * - 포커스 변경 감지
 *
 * @param value 입력 값
 * @param onValueChange 값 변경 콜백
 * @param modifier Modifier
 * @param placeholder 플레이스홀더
 * @param enabled 활성화 여부
 * @param isValid 검증 성공 여부 (true면 체크 아이콘, null이면 아이콘 없음)
 * @param errorMessage 에러 메시지 (null이면 에러 표시 안 함)
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
    val context = LocalContext.current

    // 디바운싱 처리
    LaunchedEffect(value) {
        if (debounceMillis > 0 && onValueChangeDebounced != null) {
            delay(debounceMillis)
            onValueChangeDebounced(value)
        }
    }

    // 에러 아이콘 Drawable (old 프로젝트와 동일)
    val errorDrawable = remember {
        ContextCompat.getDrawable(context, R.drawable.join_disapproval)?.let { drawable ->
            val wrappedDrawable = DrawableCompat.wrap(drawable).mutate()
            wrappedDrawable.setBounds(0, 0, wrappedDrawable.intrinsicWidth, wrappedDrawable.intrinsicHeight)
            wrappedDrawable
        }
    }

    // 성공 아이콘 Drawable (old 프로젝트와 동일)
    val successDrawable = remember {
        ContextCompat.getDrawable(context, R.drawable.join_approval)?.let { drawable ->
            val wrappedDrawable = DrawableCompat.wrap(drawable).mutate()
            wrappedDrawable.setBounds(0, 0, wrappedDrawable.intrinsicWidth, wrappedDrawable.intrinsicHeight)
            wrappedDrawable
        }
    }

    Column {
        // TextField 박스
        Box(
            modifier = modifier
                .background(
                    color = colorResource(id = R.color.background_300),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp)
        ) {
            // AndroidView로 EditText 구현 (old 프로젝트와 동일한 setError() 기능 사용)
            AndroidView(
                factory = { ctx ->
                    EditText(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        )

                        // 스타일 설정 (old 프로젝트와 동일)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setPadding(0, 24, 0, 24)
                        textSize = 12f
                        setTextColor(ContextCompat.getColor(ctx, R.color.text_default))
                        setHintTextColor(ContextCompat.getColor(ctx, R.color.text_dimmed))
                        hint = placeholder
                        isSingleLine = true
                        gravity = android.view.Gravity.CENTER_VERTICAL

                        // InputType 설정
                        inputType = when (keyboardType) {
                            KeyboardType.Email -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                            KeyboardType.Password -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            KeyboardType.Number -> InputType.TYPE_CLASS_NUMBER
                            else -> InputType.TYPE_CLASS_TEXT
                        }

                        // Password transformation
                        if (visualTransformation is PasswordVisualTransformation) {
                            transformationMethod = PasswordTransformationMethod.getInstance()
                        }

                        // IME Action
                        imeOptions = when (imeAction) {
                            ImeAction.Done -> android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                            ImeAction.Next -> android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
                            ImeAction.Search -> android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                            else -> android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
                        }

                        // Focus listener
                        setOnFocusChangeListener { _, hasFocus ->
                            onFocusChanged?.invoke(
                                object : FocusState {
                                    override val hasFocus: Boolean = hasFocus
                                    override val isFocused: Boolean = hasFocus
                                    override val isCaptured: Boolean = false
                                }
                            )
                        }

                        // Text change listener
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                if (s?.toString() != value) {
                                    onValueChange(s?.toString() ?: "")
                                }
                            }
                            override fun afterTextChanged(s: android.text.Editable?) {}
                        })

                        // Editor action listener는 EditText가 자체적으로 처리하므로 별도 설정 불필요
                    }
                },
                update = { editText ->
                    // Update text
                    if (editText.text.toString() != value) {
                        editText.setText(value)
                        editText.setSelection(value.length)
                    }

                    // Update password transformation
                    editText.transformationMethod = if (visualTransformation is PasswordVisualTransformation) {
                        PasswordTransformationMethod.getInstance()
                    } else {
                        null
                    }

                    // Update error message (old 프로젝트의 setError()와 동일)
                    // old 프로젝트: editText.setError(responseMsg, mDrawableInputError)
                    if (errorMessage != null && isValid == false && errorDrawable != null) {
                        editText.setError(errorMessage, errorDrawable)
                    } else {
                        editText.error = null
                        // old 프로젝트: 성공 시 체크 아이콘 표시
                        // editText.setCompoundDrawables(null, null, mDrawableInputOk, null)
                        if (isValid == true && successDrawable != null) {
                            editText.setCompoundDrawables(null, null, successDrawable, null)
                        } else {
                            editText.setCompoundDrawables(null, null, null, null)
                        }
                    }

                    // Update enabled state
                    editText.isEnabled = enabled

                    // Update placeholder
                    editText.hint = placeholder
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
