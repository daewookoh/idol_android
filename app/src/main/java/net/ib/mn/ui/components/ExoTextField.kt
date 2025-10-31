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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
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
 * @param focusRequester 포커스 요청자 (자동 포커스용)
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
    debounceMillis: Long = 0,
    focusRequester: FocusRequester? = null
) {
    val context = LocalContext.current

    // 포커스 요청을 위한 변수 (AndroidView의 EditText에 직접 포커스 설정)
    var editTextRef: EditText? by remember { mutableStateOf(null) }

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
                                // 값이 실제로 변경되었을 때만 onValueChange 호출
                                val newText = s?.toString() ?: ""
                                if (newText != value) {
                                    android.util.Log.d("ExoTextField", "TextWatcher.onTextChanged: '$newText' (value: '$value')")
                                    onValueChange(newText)
                                }
                            }
                            override fun afterTextChanged(s: android.text.Editable?) {}
                        })

                        // Editor action listener는 EditText가 자체적으로 처리하므로 별도 설정 불필요
                        
                        // EditText 참조 저장 (포커스 설정용)
                        editTextRef = this
                    }
                },
                update = { editText ->
                    // Update text - 사용자가 입력 중일 때는 값을 덮어쓰지 않도록 주의
                    // editText.text.toString()이 현재 표시된 값, value가 Compose state 값
                    // 사용자가 입력 중일 때는 editText의 값이 더 최신일 수 있으므로 주의
                    val currentText = editText.text.toString()
                    if (currentText != value) {
                        // 포커스 상태 확인 - 포커스가 있으면 사용자가 입력 중일 수 있음
                        val hasFocus = editText.isFocused
                        
                        if (hasFocus) {
                            // 포커스가 있을 때는 값이 실제로 다를 때만 업데이트
                            // (사용자가 입력 중일 때는 TextWatcher를 통해 이미 업데이트됨)
                            android.util.Log.d("ExoTextField", "Update: hasFocus=true, currentText='$currentText', value='$value' - skipping update")
                        } else {
                            // 포커스가 없을 때만 강제로 업데이트 (외부에서 값 변경 시)
                            android.util.Log.d("ExoTextField", "Update: hasFocus=false, currentText='$currentText', value='$value' - updating")
                            
                            // 커서 위치 저장
                            val selectionStart = editText.selectionStart
                            val selectionEnd = editText.selectionEnd
                            val wasAtEnd = selectionStart == currentText.length && selectionEnd == currentText.length
                            
                            editText.setText(value)
                            
                            // 커서 위치 복원 (끝에 있었으면 끝으로, 아니면 원래 위치로)
                            if (wasAtEnd || selectionStart > value.length) {
                                editText.setSelection(value.length)
                            } else {
                                val newStart = selectionStart.coerceIn(0, value.length)
                                val newEnd = selectionEnd.coerceIn(0, value.length)
                                editText.setSelection(newStart, newEnd)
                            }
                        }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            )
            
            // 포커스 요청 처리 (AndroidView의 EditText에 직접 포커스 설정)
            focusRequester?.let { requester ->
                LaunchedEffect(editTextRef) {
                    // EditText가 생성된 후 포커스 요청
                    if (editTextRef != null) {
                        // 약간의 지연 후 EditText에 직접 포커스를 요청
                        delay(100)
                        editTextRef?.requestFocus()
                        // 키보드 표시
                        editTextRef?.context?.let { ctx ->
                            val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                            imm?.showSoftInput(editTextRef, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                        }
                    }
                }
            }
        }
    }
}
