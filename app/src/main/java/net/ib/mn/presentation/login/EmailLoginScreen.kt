package net.ib.mn.presentation.login

import android.content.res.Configuration
import net.ib.mn.util.ToastUtil
import net.ib.mn.util.KeyboardUtil
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.LoadingOverlay
import net.ib.mn.ui.components.ExoTitleDialog
import net.ib.mn.ui.theme.ExodusTheme

/**
 * Email 로그인 화면 (old 프로젝트의 EmailSigninFragment).
 * old XML 레이아웃과 완전히 동일하게 구현.
 */
@Composable
fun EmailLoginScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotId: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EmailLoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showFindIdDialog by remember { mutableStateOf(false) }
    var findIdEmail by remember { mutableStateOf<String?>(null) }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EmailLoginContract.Effect.NavigateToMain -> onNavigateToMain()
                is EmailLoginContract.Effect.NavigateToSignUp -> onNavigateToSignUp()
                is EmailLoginContract.Effect.NavigateToForgotId -> onNavigateToForgotId()
                is EmailLoginContract.Effect.NavigateToForgotPassword -> onNavigateToForgotPassword()
                is EmailLoginContract.Effect.NavigateBack -> onNavigateBack()
                is EmailLoginContract.Effect.ShowError -> {
                    ToastUtil.show(context, effect.message)
                }
                is EmailLoginContract.Effect.ShowToast -> {
                    ToastUtil.show(context, effect.message)
                }
                is EmailLoginContract.Effect.ShowFindIdDialog -> {
                    findIdEmail = effect.email
                    showFindIdDialog = true
                }
            }
        }
    }

    EmailLoginContent(
        state = state,
        onIntent = viewModel::sendIntent,
        showFindIdDialog = showFindIdDialog,
        findIdEmail = findIdEmail,
        onDismissFindIdDialog = { showFindIdDialog = false }
    )
}

/**
 * Email 로그인 화면의 UI 컨텐츠 (Stateless).
 * 프리뷰 및 테스트를 위한 stateless composable.
 */
@Composable
private fun EmailLoginContent(
    state: EmailLoginContract.State,
    onIntent: (EmailLoginContract.Intent) -> Unit,
    showFindIdDialog: Boolean = false,
    findIdEmail: String? = null,
    onDismissFindIdDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val hideKeyboard = {
        KeyboardUtil.hideKeyboard(context)
    }

    ExoScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null, // 클릭 시 ripple 효과 제거
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    hideKeyboard()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
            // Close Button (대신 TopAppBar 역할)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(onClick = {
                    hideKeyboard()
                    onIntent(EmailLoginContract.Intent.NavigateBack)
                }) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Back",
                        tint = colorResource(id = R.color.text_default)
                    )
                }

                // Title
                Text(
                    text = stringResource(id = R.string.login_email),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    color = colorResource(id = R.color.text_default)
                )
            }

            // Content (layout_marginTop="50dp" from XML)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 38.dp)
                    .padding(top = 50.dp, bottom = 17.dp)
            ) {
                // Email Input (background="@drawable/edit_underline")
                InputFieldWithIcon(
                    value = state.email,
                    onValueChange = { onIntent(EmailLoginContract.Intent.EmailChanged(it)) },
                    hint = stringResource(id = R.string.hint_email),
                    iconRes = R.drawable.img_login_id,
                    isFocused = state.isEmailFocused,
                    onFocusChanged = { onIntent(EmailLoginContract.Intent.EmailFocusChanged(it)) },
                    keyboardType = KeyboardType.Email,
                    isPassword = false,
                    onPasswordToggle = null,
                    isPasswordVisible = false,
                    topMargin = 0.dp
                )

                Spacer(modifier = Modifier.height(0.dp)) // Password field가 marginTop=15dp를 가지므로 간격 조정

                // Password Input (EditText has layout_marginTop="15dp")
                InputFieldWithIcon(
                    value = state.password,
                    onValueChange = { onIntent(EmailLoginContract.Intent.PasswordChanged(it)) },
                    hint = stringResource(id = R.string.hint_passwd),
                    iconRes = R.drawable.img_login_password,
                    isFocused = state.isPasswordFocused,
                    onFocusChanged = { onIntent(EmailLoginContract.Intent.PasswordFocusChanged(it)) },
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    onPasswordToggle = { onIntent(EmailLoginContract.Intent.TogglePasswordVisibility) },
                    isPasswordVisible = state.isPasswordVisible,
                    topMargin = 15.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Sign In Button
                PressableButton(
                    onClick = {
                        hideKeyboard()
                        onIntent(EmailLoginContract.Intent.SignIn)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    normalColor = colorResource(id = R.color.main),
                    pressedColor = colorResource(id = R.color.red_click_border)
                ) {
                    Text(
                        text = stringResource(id = R.string.btn_signin),
                        color = colorResource(id = R.color.text_white_black)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Sign Up Button
                OutlinedPressableButton(
                    onClick = {
                        hideKeyboard()
                        onIntent(EmailLoginContract.Intent.SignUp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    borderColor = colorResource(id = R.color.main),
                    textColor = colorResource(id = R.color.main)
                ) {
                    Text(text = stringResource(id = R.string.btn_signup))
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Forgot ID / Password Links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.text_forgot_id),
                        fontSize = 12.sp,
                        color = colorResource(id = R.color.text_default),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                hideKeyboard()
                                onIntent(EmailLoginContract.Intent.ForgotId)
                            }
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(10.dp)
                            .background(colorResource(id = R.color.gray200))
                    )

                    Text(
                        text = stringResource(id = R.string.text_forgot_passwd),
                        fontSize = 12.sp,
                        color = colorResource(id = R.color.text_default),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                hideKeyboard()
                                onIntent(EmailLoginContract.Intent.ForgotPassword)
                            }
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 로딩 인디케이터
        LoadingOverlay(isLoading = state.isLoading)
        } // Box

        // 아이디 찾기 다이얼로그
        if (showFindIdDialog) {
            ExoTitleDialog(
                title = stringResource(id = R.string.title_find_id),
                message = if (findIdEmail.isNullOrEmpty()) {
                    stringResource(id = R.string.failed_to_find_id)
                } else {
                    "${stringResource(id = R.string.your_id_is1)}\n${findIdEmail}"
                },
                onDismiss = onDismissFindIdDialog,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        }
    } // ExoScaffold
}

/**
 * Input field with icon (이메일/비밀번호 입력 필드).
 * old XML의 LinearLayout + ImageView + EditText 구조를 Compose로 구현.
 * Material3 TextField 사용 (자동 autofill 지원).
 */
@Composable
private fun InputFieldWithIcon(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    iconRes: Int,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    keyboardType: KeyboardType,
    isPassword: Boolean,
    onPasswordToggle: (() -> Unit)?,
    isPasswordVisible: Boolean,
    topMargin: androidx.compose.ui.unit.Dp = 0.dp
) {
    // edit_underline 또는 edit_underline_click 배경
    val backgroundColor = colorResource(id = R.color.background_100)
    val borderColor = if (isFocused) {
        colorResource(id = R.color.main)
    } else {
        colorResource(id = R.color.text_dimmed)
    }

    Row(
        modifier = Modifier
            .padding(top = topMargin)
            .fillMaxWidth()
            .background(backgroundColor)
            // bottom border 효과 (edit_underline.xml과 동일)
            .drawBottomBorder(1.dp, borderColor),
        verticalAlignment = Alignment.Bottom
    ) {
        // Icon
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .padding(start = 5.dp, bottom = 7.dp),
            contentScale = ContentScale.Fit
        )

        // Text Input (AndroidView + EditText for reliable Autofill support)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, bottom = 5.dp)
                .then(if (isPassword) Modifier.padding(top = 15.dp) else Modifier)
        ) {
            val textColor = colorResource(id = R.color.text_default).toArgb()
            val hintColor = colorResource(id = R.color.text_dimmed).toArgb()

            AndroidView(
                factory = { context ->
                    EditText(context).apply {
                        // Layout params
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        )

                        // Text style (old XML과 동일)
                        textSize = 14f
                        setTextColor(textColor)
                        setHintTextColor(hintColor)
                        this.hint = hint
                        gravity = android.view.Gravity.BOTTOM
                        setPadding(0, 0, 0, (5 * resources.displayMetrics.density).toInt())

                        // InputType 설정 (Autofill을 위해 중요!)
                        inputType = when {
                            isPassword && !isPasswordVisible -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            isPassword && isPasswordVisible -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            keyboardType == KeyboardType.Email -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                            else -> InputType.TYPE_CLASS_TEXT
                        }

                        // Autofill hints (Android 8.0+)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            setAutofillHints(
                                if (isPassword) android.view.View.AUTOFILL_HINT_PASSWORD
                                else android.view.View.AUTOFILL_HINT_EMAIL_ADDRESS
                            )
                            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_YES
                        }

                        // 배경 투명 (Compose에서 처리)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setSingleLine(true)

                        // IME action
                        imeOptions = if (isPassword) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT

                        // Focus listener
                        setOnFocusChangeListener { _, hasFocus ->
                            onFocusChanged(hasFocus)
                        }

                        // Text change listener
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                if (s.toString() != value) {
                                    onValueChange(s.toString())
                                }
                            }
                            override fun afterTextChanged(s: android.text.Editable?) {}
                        })
                    }
                },
                update = { editText ->
                    // Update text if changed
                    if (editText.text.toString() != value) {
                        editText.setText(value)
                        editText.setSelection(value.length)
                    }

                    // Update password visibility
                    if (isPassword) {
                        editText.inputType = if (isPasswordVisible) {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        } else {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        }
                        editText.setSelection(value.length)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Password Toggle Button
        if (isPassword && onPasswordToggle != null) {
            IconButton(
                onClick = onPasswordToggle,
                modifier = Modifier
                    .padding(end = 10.dp, bottom = 5.dp)
                    .size(24.dp)
            ) {
                Image(
                    painter = painterResource(
                        id = if (isPasswordVisible) R.drawable.btn_hide_on
                        else R.drawable.btn_hide_off
                    ),
                    contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/**
 * Bottom border를 그리는 Modifier extension.
 * edit_underline.xml의 효과를 재현.
 */
private fun Modifier.drawBottomBorder(width: androidx.compose.ui.unit.Dp, color: Color): Modifier {
    return this.then(
        Modifier.drawWithContent {
            drawContent()
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(0f, size.height),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                strokeWidth = width.toPx()
            )
        }
    )
}

/**
 * Pressable Button (login_border.xml 재현).
 */
@Composable
private fun PressableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    normalColor: Color,
    pressedColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPressed) pressedColor else normalColor
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(0.dp)
    ) {
        content()
    }
}

/**
 * Outlined Pressable Button (join_border.xml 재현).
 */
@Composable
private fun OutlinedPressableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color,
    textColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = textColor
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        content()
    }
}

@Preview(
    name = "Light Mode",
    showSystemUi = true,
    showBackground = true,
    locale = "ko"
)
@Composable
fun EmailLoginScreenPreviewLight() {
    ExodusTheme(darkTheme = false) {
        EmailLoginContent(
            state = EmailLoginContract.State(),
            onIntent = {}
        )
    }
}

@Preview(
    name = "Dark Mode",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ko"
)
@Composable
fun EmailLoginScreenPreviewDark() {
    ExodusTheme(darkTheme = true) {
        EmailLoginContent(
            state = EmailLoginContract.State(),
            onIntent = {}
        )
    }
}
