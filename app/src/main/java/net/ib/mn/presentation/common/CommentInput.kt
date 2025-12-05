package net.ib.mn.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.IdolImageUtil.toSecureUrl

/**
 * CommentInput - 댓글 입력 컴포넌트 (하단 고정)
 * old 프로젝트의 view_comment.xml과 동일한 레이아웃
 *
 * @param value 입력된 텍스트
 * @param onValueChange 텍스트 변경 콜백
 * @param onSubmit 전송 버튼 클릭 콜백
 * @param onAttachClick 파일 첨부 버튼 클릭 콜백
 * @param onEmoticonClick 이모티콘 버튼 클릭 콜백
 * @param isEmoticonPanelOpen 이모티콘 패널이 열려있는지 여부
 * @param selectedEmoticonUrl 선택된 이모티콘 이미지 URL (프리뷰용)
 * @param onEmoticonClear 이모티콘 선택 해제 콜백 (X 버튼 클릭)
 * @param focusRequester 외부에서 전달받는 FocusRequester (이모티콘 선택 후 포커스용)
 * @param isLoading 로딩 중 여부
 * @param enabled 활성화 여부
 * @param modifier Modifier
 */
@Composable
fun CommentInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onAttachClick: (() -> Unit)? = null,
    onEmoticonClick: (() -> Unit)? = null,
    isEmoticonPanelOpen: Boolean = false,
    selectedEmoticonUrl: String? = null,
    onEmoticonClear: (() -> Unit)? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onInputFocused: (() -> Unit)? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    // 텍스트가 있거나 이모티콘이 선택되어 있으면 전송 가능
    val canSubmit = (value.isNotBlank() || !selectedEmoticonUrl.isNullOrEmpty()) && !isLoading && enabled

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
    ) {
        // 이모티콘 프리뷰 영역 (old: cl_preview)
        if (!selectedEmoticonUrl.isNullOrEmpty()) {
            EmoticonPreview(
                emoticonUrl = selectedEmoticonUrl,
                onClose = { onEmoticonClear?.invoke() }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 1. 파일 첨부 버튼 (btn_chat_add)
            Box(
                modifier = Modifier
                    .padding(bottom = 7.dp)
                    .size(34.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled && !isLoading
                    ) { onAttachClick?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.btn_chat_add),
                    contentDescription = "Attach file",
                    modifier = Modifier.size(34.dp),
                    tint = Color.Unspecified
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            // 2. 입력 필드 영역 (테두리 + 입력창 + 이모티콘 + 전송)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 120.dp)
                    .background(
                        color = ColorPalette.background200,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = ColorPalette.gray150,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(start = 15.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 텍스트 입력 필드
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 32.dp, max = 112.dp)
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onInputFocused?.invoke()
                                }
                            },
                        enabled = enabled && !isLoading,
                        textStyle = ExoTypo.body15.copy(color = ColorPalette.textGray),
                        cursorBrush = SolidColor(ColorPalette.main),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Default
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.hint_comment),
                                        style = ExoTypo.body15.copy(color = ColorPalette.gray200)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 이모티콘 버튼 (btn_chat_emoticon)
                Box(
                    modifier = Modifier
                        .padding(bottom = 3.dp, end = 5.dp)
                        .size(34.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = enabled && !isLoading
                        ) { onEmoticonClick?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isEmoticonPanelOpen) R.drawable.btn_chat_emoticon_on
                            else R.drawable.btn_chat_emoticon_off
                        ),
                        contentDescription = "Emoticon",
                        modifier = Modifier.size(34.dp),
                        tint = Color.Unspecified
                    )
                }

                // 전송 버튼 (btn_chat_enter)
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp, end = 4.dp)
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = canSubmit
                        ) {
                            if (canSubmit) {
                                onSubmit()
                                focusManager.clearFocus()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = ColorPalette.main,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                if (canSubmit) R.drawable.btn_chat_enter_on
                                else R.drawable.btn_chat_enter_off
                            ),
                            contentDescription = "Send",
                            modifier = Modifier.size(32.dp),
                            tint = Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}

/**
 * 이모티콘 프리뷰 컴포넌트 (old: cl_preview)
 * old 프로젝트와 동일한 레이아웃:
 * - 전체 너비 딤드 배경 (#80000000)
 * - X 버튼: 우측 상단 (margin 18dp)
 * - 이모티콘: 가운데, 136dp x 136dp
 */
@Composable
private fun EmoticonPreview(
    emoticonUrl: String,
    onClose: () -> Unit
) {
    // old: cl_preview - match_parent, 더 투명한 배경
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x40000000))
            .padding(vertical = 2.dp)
    ) {
        // X 버튼 (iv_preview_close) - 우측 상단
        Icon(
            painter = painterResource(R.drawable.btn_close_w),
            contentDescription = "Close",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 18.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                ),
            tint = Color.Unspecified
        )

        // 이모티콘 이미지 (iv_preview) - 가운데, 136dp x 136dp
        AsyncImage(
            model = emoticonUrl.toSecureUrl(),
            contentDescription = "Selected emoticon",
            modifier = Modifier
                .size(136.dp)
                .align(Alignment.Center),
            contentScale = ContentScale.Fit
        )
    }
}
