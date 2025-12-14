package net.ib.mn.presentation.community.chat.create

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.R
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoBottomSheetItem
import net.ib.mn.ui.components.ExoBottomSheetList
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomCreateScreen(
    idolId: Int,
    onNavigateBack: () -> Unit,
    onRoomCreated: (roomId: Int, nickname: String?, userId: Int?, isAnonymity: Boolean, title: String) -> Unit,
    viewModel: ChatRoomCreateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(idolId) {
        viewModel.sendIntent(ChatRoomCreateContract.Intent.Initialize(idolId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ChatRoomCreateContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ChatRoomCreateContract.Effect.RoomCreated -> {
                    Toast.makeText(context, context.getString(R.string.chat_room_create_success), Toast.LENGTH_SHORT).show()
                    onRoomCreated(effect.roomId, effect.nickname, effect.userId, effect.isAnonymity, effect.title)
                }
                is ChatRoomCreateContract.Effect.NavigateBack -> {
                    onNavigateBack()
                }
                is ChatRoomCreateContract.Effect.ShowTitleTooShort -> {
                    Toast.makeText(context, context.getString(R.string.chat_title_short), Toast.LENGTH_SHORT).show()
                }
                is ChatRoomCreateContract.Effect.ShowBadWordsInTitle -> {
                    Toast.makeText(context, context.getString(R.string.chat_title_bad_words), Toast.LENGTH_SHORT).show()
                }
                is ChatRoomCreateContract.Effect.ShowBadWordsInDescription -> {
                    Toast.makeText(context, context.getString(R.string.chat_description_bad_words), Toast.LENGTH_SHORT).show()
                }
                is ChatRoomCreateContract.Effect.ShowLevelTooHigh -> {
                    Toast.makeText(context, context.getString(R.string.chat_level_select_limit), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Anonymous 선택 시트
    if (state.showAnonymousSheet) {
        ExoBottomSheetList(
            items = listOf(
                ExoBottomSheetItem(value = false, label = stringResource(R.string.chat_room_nickname)),
                ExoBottomSheetItem(value = true, label = stringResource(R.string.chat_room_anonymous))
            ),
            selectedValue = state.isAnonymous,
            onItemSelected = { isAnonymous ->
                viewModel.sendIntent(ChatRoomCreateContract.Intent.SetAnonymous(isAnonymous))
            },
            onDismissRequest = {
                viewModel.sendIntent(ChatRoomCreateContract.Intent.HideAnonymousSheet)
            }
        )
    }

    // Level Limit 선택 시트 (5, 10, 15, 20, 25, 30)
    if (state.showLevelLimitSheet) {
        val levelItems = listOf(5, 10, 15, 20, 25, 30).map { level ->
            ExoBottomSheetItem(
                value = level,
                label = String.format(stringResource(R.string.chat_room_level_limit), level)
            )
        }
        ExoBottomSheetList(
            items = levelItems,
            selectedValue = state.levelLimit,
            onItemSelected = { level ->
                viewModel.sendIntent(ChatRoomCreateContract.Intent.SetLevelLimit(level))
            },
            onDismissRequest = {
                viewModel.sendIntent(ChatRoomCreateContract.Intent.HideLevelLimitSheet)
            }
        )
    }

    // 생성 확인 다이얼로그
    if (state.showConfirmDialog) {
        ExoConfirmDialog(
            title = stringResource(R.string.chat_room_make_popup1_title),
            message = String.format(stringResource(R.string.chat_room_make_popup1_desc), state.diamondCost),
            onConfirm = {
                viewModel.sendIntent(ChatRoomCreateContract.Intent.CreateRoom)
            },
            onDismiss = {
                viewModel.sendIntent(ChatRoomCreateContract.Intent.HideConfirmDialog)
            }
        )
    }

    // 채팅방 개설 안내 다이얼로그
    if (state.showInstructionDialog) {
        ChatRoomCreateInstructionDialog(
            diamondCost = state.diamondCost,
            onConfirm = {
                viewModel.sendIntent(ChatRoomCreateContract.Intent.HideInstructionDialog)
            },
            onDontShowAgain = {
                viewModel.sendIntent(ChatRoomCreateContract.Intent.DontShowInstructionAgain)
            }
        )
    }

    // Scaffold 사용 - contentWindowInsets를 0으로 설정하여 직접 제어
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.chat_make),
                onNavigationClick = { viewModel.sendIntent(ChatRoomCreateContract.Intent.GoBack) }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorPalette.background100)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.sendIntent(ChatRoomCreateContract.Intent.ShowConfirmDialog)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    enabled = state.isFormValid && !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPalette.main,
                        disabledContainerColor = ColorPalette.gray300
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = String.format(stringResource(R.string.chat_room_make_button), state.diamondCost),
                            style = ExoTypo.typo14.copy(
                                color = ColorPalette.textWhiteBlack,
                                lineHeight = 18.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        containerColor = ColorPalette.background100,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // 채팅방 이름 (필수, 최소 5자)
                InputFieldWithTitle(
                    title = stringResource(R.string.chat_room_title),
                    isRequired = true,
                    value = state.title,
                    hint = stringResource(R.string.chat_room_title_placeholder),
                    counterText = state.titleLengthText,
                    onValueChange = { viewModel.sendIntent(ChatRoomCreateContract.Intent.UpdateTitle(it)) },
                    singleLine = true,
                    maxLength = 50,
                    minLength = 5,
                    onFocusLost = {
                        Toast.makeText(context, context.getString(R.string.chat_title_short), Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 채팅방 소개
                InputFieldWithTitle(
                    title = stringResource(R.string.chat_room_desc),
                    isRequired = false,
                    value = state.description,
                    hint = stringResource(R.string.chat_room_desc_placeholder),
                    counterText = state.descriptionLengthText,
                    onValueChange = { viewModel.sendIntent(ChatRoomCreateContract.Intent.UpdateDescription(it)) },
                    singleLine = false,
                    minHeight = 60,
                    maxLength = 100
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 레벨 제한 (필수)
                SelectFieldWithTitle(
                    title = stringResource(R.string.chat_room_limit_level),
                    isRequired = true,
                    value = if (state.levelLimit >= 0) String.format(stringResource(R.string.chat_room_level_limit), state.levelLimit) else "",
                    onClick = { viewModel.sendIntent(ChatRoomCreateContract.Intent.ShowLevelLimitSheet) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 닉네임 공개 여부 (필수)
                SelectFieldWithTitle(
                    title = stringResource(R.string.chat_room_disclose_nickname),
                    isRequired = true,
                    value = if (state.isAnonymous) stringResource(R.string.chat_room_anonymous) else if (state.levelLimit >= 0 || state.title.isNotEmpty()) stringResource(R.string.chat_room_nickname) else "",
                    onClick = { viewModel.sendIntent(ChatRoomCreateContract.Intent.ShowAnonymousSheet) }
                )

                Spacer(modifier = Modifier.height(33.dp))

                // 필수항목 안내
                Text(
                    text = stringResource(R.string.chat_room_require),
                    style = ExoTypo.typo11.copy(fontWeight = FontWeight.Bold),
                    color = ColorPalette.main,
                    modifier = Modifier.padding(start = 10.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))
            }

            // 로딩 오버레이
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ColorPalette.gray1000Opacity60),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPalette.main)
                }
            }
        }
    }
}

@Composable
private fun InputFieldWithTitle(
    title: String,
    isRequired: Boolean,
    value: String,
    hint: String,
    counterText: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    minHeight: Int = 38,
    maxLength: Int = 50,
    minLength: Int = 0,
    onFocusLost: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        // 타이틀 행: * + 타이틀 + 카운터
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Top) {
                if (isRequired) {
                    Text(
                        text = "*",
                        style = ExoTypo.typo14.copy(color = ColorPalette.main),
                        modifier = Modifier.width(8.5.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(8.5.dp))
                }
                Text(
                    text = title,
                    style = ExoTypo.typo14Bold.copy(color = ColorPalette.gray1000)
                )
            }
            Text(
                text = counterText,
                style = ExoTypo.typo12.copy(color = ColorPalette.gray200)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 입력 필드
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight.dp)
                .background(ColorPalette.background100, RoundedCornerShape(8.dp))
                .border(1.dp, ColorPalette.gray200, RoundedCornerShape(8.dp))
                .padding(horizontal = 15.dp, vertical = 10.dp)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && value.trim().length < minLength && value.isNotEmpty()) {
                        onFocusLost?.invoke()
                    }
                },
            textStyle = ExoTypo.typo14.copy(color = ColorPalette.gray1000),
            cursorBrush = SolidColor(ColorPalette.main),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            style = ExoTypo.typo14.copy(color = ColorPalette.gray200)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun SelectFieldWithTitle(
    title: String?,
    isRequired: Boolean,
    value: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        // 타이틀 행 (타이틀이 있는 경우만)
        if (title != null) {
            Row(verticalAlignment = Alignment.Top) {
                if (isRequired) {
                    Text(
                        text = "*",
                        style = ExoTypo.typo14.copy(color = ColorPalette.main),
                        modifier = Modifier.width(8.5.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(8.5.dp))
                }
                Text(
                    text = title,
                    style = ExoTypo.typo14Bold.copy(color = ColorPalette.gray1000)
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
        }

        // 선택 필드
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(ColorPalette.background100, RoundedCornerShape(8.dp))
                .border(1.dp, ColorPalette.gray200, RoundedCornerShape(8.dp))
                .then(
                    if (enabled && onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 15.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = ExoTypo.typo14.copy(
                        color = if (enabled) ColorPalette.gray1000 else ColorPalette.textDimmed
                    )
                )
                if (enabled && onClick != null) {
                    Icon(
                        painter = painterResource(R.drawable.icon_arrow_drop_down),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * 채팅방 개설 안내 다이얼로그
 * old 프로젝트의 dialog_create_chat_room_instruction.xml과 동일한 UI
 */
@Composable
private fun ChatRoomCreateInstructionDialog(
    diamondCost: Int,
    onConfirm: () -> Unit,
    onDontShowAgain: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onConfirm
    ) {
        Column(
            modifier = Modifier
                .width(290.dp)
                .background(ColorPalette.background100, RoundedCornerShape(10.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 타이틀
            Text(
                text = stringResource(R.string.chat_make),
                style = ExoTypo.typo17Bold.copy(color = ColorPalette.main),
                modifier = Modifier.padding(vertical = 20.dp)
            )

            // Important 라벨
            Text(
                text = stringResource(R.string.important),
                style = ExoTypo.typo14.copy(color = ColorPalette.gray1000),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 5.dp, end = 20.dp)
            )

            // 안내 항목들
            InstructionItem(
                text = stringResource(R.string.chat_make_help_popup1),
                topPadding = 10.dp,
                bottomPadding = 15.dp
            )
            InstructionItem(
                text = stringResource(R.string.chat_make_help_popup2),
                topPadding = 0.dp,
                bottomPadding = 15.dp
            )
            InstructionItem(
                text = stringResource(R.string.chat_make_help_popup3),
                topPadding = 0.dp,
                bottomPadding = 15.dp
            )
            InstructionItem(
                text = String.format(stringResource(R.string.chat_make_help_popup4), diamondCost),
                topPadding = 0.dp,
                bottomPadding = 20.dp
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 구분선
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorPalette.gray100)
            )

            // 버튼 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                // 다시 보지 않기 버튼
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onDontShowAgain() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.lable_do_not_read),
                        style = ExoTypo.typo14.copy(color = ColorPalette.gray300)
                    )
                }

                // 세로 구분선
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(ColorPalette.gray100)
                )

                // 확인 버튼
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.btn_confirm),
                        style = ExoTypo.typo14.copy(color = ColorPalette.gray300)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionItem(
    text: String,
    topPadding: androidx.compose.ui.unit.Dp = 10.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 15.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = bottomPadding),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(R.drawable.quiz_check),
            contentDescription = null,
            modifier = Modifier
                .size(15.dp)
                .padding(top = 3.dp),
            tint = ColorPalette.main
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = ExoTypo.typo12.copy(
                color = ColorPalette.gray1000,
                lineHeight = 16.sp
            )
        )
    }
}
