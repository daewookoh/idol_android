package net.ib.mn.presentation.login

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is EmailLoginContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    EmailLoginContent(
        state = state,
        onIntent = viewModel::sendIntent
    )
}

/**
 * Email 로그인 화면의 UI 컨텐츠 (Stateless).
 * 프리뷰 및 테스트를 위한 stateless composable.
 */
@Composable
private fun EmailLoginContent(
    state: EmailLoginContract.State,
    onIntent: (EmailLoginContract.Intent) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background_100))
            .statusBarsPadding()
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
                IconButton(onClick = { onIntent(EmailLoginContract.Intent.NavigateBack) }) {
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
                        focusManager.clearFocus()
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
                    onClick = { onIntent(EmailLoginContract.Intent.SignUp) },
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
                            .clickable { onIntent(EmailLoginContract.Intent.ForgotId) }
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
                            .clickable { onIntent(EmailLoginContract.Intent.ForgotPassword) }
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 로딩 인디케이터
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Input field with icon (이메일/비밀번호 입력 필드).
 * old XML의 LinearLayout + ImageView + EditText 구조를 Compose로 구현.
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

        // Text Input
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, bottom = 5.dp)
                .then(if (isPassword) Modifier.padding(top = 15.dp) else Modifier)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.text_default)
                ),
                singleLine = true,
                visualTransformation = if (isPassword && !isPasswordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(colorResource(id = R.color.main)),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.text_dimmed)
                        )
                    }
                    innerTextField()
                }
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
    showBackground = true
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
    uiMode = Configuration.UI_MODE_NIGHT_YES
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
