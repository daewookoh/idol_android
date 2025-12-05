package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.ib.mn.R

/**
 * 앱 전체에서 사용하는 공통 Dialog 컴포넌트
 *
 * old 프로젝트의 dialog_default_idol_one_btn.xml을 Compose로 구현
 *
 * 특징:
 * - 전체 화면 Dimmed 처리
 * - 타이틀 (옵션)
 * - 스크롤 가능한 메시지
 * - 하단 확인 버튼
 * - Light/Dark 모드 자동 지원
 *
 * @param title 다이얼로그 타이틀 (null이면 표시 안 함)
 * @param message 다이얼로그 메시지
 * @param confirmButtonText 확인 버튼 텍스트 (기본값: "확인")
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onConfirm 확인 버튼 클릭 콜백 (기본값: onDismiss와 동일)
 * @param dismissOnBackPress 백버튼으로 다이얼로그 닫기 가능 여부 (기본값: true, old 프로젝트는 false)
 * @param dismissOnClickOutside 외부 클릭으로 다이얼로그 닫기 가능 여부 (기본값: true, old 프로젝트는 false)
 */
@Composable
fun ExoDialog(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    confirmButtonText: String = stringResource(R.string.confirm),
    onConfirm: () -> Unit = onDismiss,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true
) {
    Dialog(
        onDismissRequest = {
            if (dismissOnBackPress || dismissOnClickOutside) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = modifier
                    .width(290.dp)
                    .background(
                        color = colorResource(id = R.color.text_white_black),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = colorResource(id = R.color.gray150),
                        shape = RoundedCornerShape(6.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 타이틀 (옵션)
                if (title != null) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = colorResource(id = R.color.main),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    )
                }

                // 메시지 (스크롤 가능)
                // old 프로젝트: TextView의 기본 줄간격 사용 (lineSpacingExtra 없음)
                // Compose Text의 기본 lineHeight는 fontSize * 1.2 정도이므로,
                // old 프로젝트와 동일하게 맞추기 위해 lineHeight를 명시적으로 설정
                val scrollState = rememberScrollState()
                Text(
                    text = message,
                    fontSize = 14.sp,
                    lineHeight = 20.sp, // old 프로젝트 TextView 기본 줄간격과 유사하게 설정 (14sp * 1.43 ≈ 20sp)
                    fontWeight = FontWeight.Normal,
                    color = colorResource(id = R.color.gray580),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(
                            top = if (title == null) 20.dp else 0.dp,
                            bottom = 0.dp
                        )
                        .heightIn(max = 400.dp) // 최대 높이 제한
                        .verticalScroll(scrollState)
                )

                // 구분선
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.dp),
                    thickness = 1.dp,
                    color = colorResource(id = R.color.gray100)
                )

                // 확인 버튼
                TextButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(id = R.color.gray580)
                    )
                ) {
                    Text(
                        text = confirmButtonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 타이틀 없이 메시지만 표시하는 간단한 Dialog
 *
 * @param message 다이얼로그 메시지
 * @param onDismiss 다이얼로그 닫기 콜백
 */
@Composable
fun ExoSimpleDialog(
    message: String,
    onDismiss: () -> Unit,
    confirmButtonText: String = stringResource(R.string.confirm)
) {
    ExoDialog(
        message = message,
        onDismiss = onDismiss,
        confirmButtonText = confirmButtonText
    )
}

/**
 * 타이틀과 메시지를 모두 표시하는 Dialog
 *
 * @param title 다이얼로그 타이틀
 * @param message 다이얼로그 메시지
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param confirmButtonText 확인 버튼 텍스트 (기본값: "확인")
 * @param dismissOnBackPress 백버튼으로 다이얼로그 닫기 가능 여부 (기본값: true)
 * @param dismissOnClickOutside 외부 클릭으로 다이얼로그 닫기 가능 여부 (기본값: true)
 */
@Composable
fun ExoTitleDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmButtonText: String = stringResource(R.string.confirm),
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true
) {
    ExoDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmButtonText = confirmButtonText,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside
    )
}

/**
 * 확인/취소 두 버튼을 가진 Dialog
 *
 * old 프로젝트의 dialog_default_idol_two_btn.xml을 Compose로 구현
 *
 * @param title 다이얼로그 타이틀
 * @param message 다이얼로그 메시지
 * @param onConfirm 확인 버튼 클릭 콜백
 * @param onDismiss 다이얼로그 닫기/취소 콜백
 * @param confirmButtonText 확인 버튼 텍스트 (기본값: "확인")
 * @param dismissButtonText 취소 버튼 텍스트 (기본값: "취소")
 * @param dismissOnBackPress 백버튼으로 다이얼로그 닫기 가능 여부 (기본값: true)
 * @param dismissOnClickOutside 외부 클릭으로 다이얼로그 닫기 가능 여부 (기본값: true)
 */
@Composable
fun ExoConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = stringResource(R.string.confirm),
    dismissButtonText: String = stringResource(R.string.btn_cancel),
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true
) {
    Dialog(
        onDismissRequest = {
            if (dismissOnBackPress || dismissOnClickOutside) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    ) {
        // Old와 동일: dimmed 처리는 Dialog 영역만 (전체 화면 아님)
        Column(
            modifier = Modifier
                .width(290.dp)
                .background(
                    color = colorResource(id = R.color.text_white_black),
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.gray150),
                    shape = RoundedCornerShape(6.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 타이틀
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = colorResource(id = R.color.main),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 10.dp)
            )

            // 메시지
            val scrollState = rememberScrollState()
            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = colorResource(id = R.color.gray580),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState)
            )

            // 구분선
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                thickness = 1.dp,
                color = colorResource(id = R.color.gray100)
            )

            // 버튼 Row (Old와 동일: 확인(왼쪽) | 취소(오른쪽))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                // 확인 버튼 (왼쪽) - Old: btn_ok
                TextButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(bottomStart = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(id = R.color.gray580)
                    )
                ) {
                    Text(
                        text = confirmButtonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // 버튼 사이 구분선
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = colorResource(id = R.color.gray100)
                )

                // 취소 버튼 (오른쪽) - Old: btn_cancel
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(id = R.color.gray580)
                    )
                ) {
                    Text(
                        text = dismissButtonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 에러 메시지를 표시하는 단일 버튼 Dialog
 *
 * Old 프로젝트의 Util.showDefaultIdolDialogWithBtn1을 Compose로 구현
 * 신고 실패 등 에러 상황에서 사용
 *
 * @param message 에러 메시지
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param confirmButtonText 확인 버튼 텍스트 (기본값: "확인")
 */
@Composable
fun ExoErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    confirmButtonText: String = stringResource(R.string.confirm)
) {
    ExoDialog(
        message = message,
        onDismiss = onDismiss,
        confirmButtonText = confirmButtonText
    )
}

/**
 * 확인/취소 두 버튼을 가진 Dialog (AnnotatedString 지원)
 *
 * 메시지에 스타일(색상, 굵기 등)을 적용할 수 있는 버전
 *
 * @param title 다이얼로그 타이틀
 * @param message AnnotatedString 형식의 다이얼로그 메시지 (스타일 적용 가능)
 * @param onConfirm 확인 버튼 클릭 콜백
 * @param onDismiss 다이얼로그 닫기/취소 콜백
 * @param confirmButtonText 확인 버튼 텍스트 (기본값: "확인")
 * @param dismissButtonText 취소 버튼 텍스트 (기본값: "취소")
 * @param dismissOnBackPress 백버튼으로 다이얼로그 닫기 가능 여부 (기본값: true)
 * @param dismissOnClickOutside 외부 클릭으로 다이얼로그 닫기 가능 여부 (기본값: true)
 */
@Composable
fun ExoConfirmDialog(
    title: String,
    message: AnnotatedString,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = stringResource(R.string.confirm),
    dismissButtonText: String = stringResource(R.string.btn_cancel),
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true
) {
    Dialog(
        onDismissRequest = {
            if (dismissOnBackPress || dismissOnClickOutside) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    ) {
        Column(
            modifier = Modifier
                .width(290.dp)
                .background(
                    color = colorResource(id = R.color.text_white_black),
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.gray150),
                    shape = RoundedCornerShape(6.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 타이틀
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = colorResource(id = R.color.main),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 10.dp)
            )

            // 메시지 (AnnotatedString)
            val scrollState = rememberScrollState()
            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = colorResource(id = R.color.gray580),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState)
            )

            // 구분선
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                thickness = 1.dp,
                color = colorResource(id = R.color.gray100)
            )

            // 버튼 Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                // 확인 버튼 (왼쪽)
                TextButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(bottomStart = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(id = R.color.gray580)
                    )
                ) {
                    Text(
                        text = confirmButtonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // 버튼 사이 구분선
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = colorResource(id = R.color.gray100)
                )

                // 취소 버튼 (오른쪽)
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(id = R.color.gray580)
                    )
                ) {
                    Text(
                        text = dismissButtonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * NumberPicker를 사용한 날짜 선택 컴포넌트
 * API 29+ 에서 selectionDividerHeight = 0 으로 구분선 제거
 * ContextThemeWrapper로 테마 적용하여 텍스트 크기/색상 설정
 *
 * @param items 선택 가능한 항목 목록
 * @param selectedIndex 현재 선택된 인덱스
 * @param onIndexChange 선택 변경 콜백
 */
@Composable
private fun ExoNumberPicker(
    items: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            // ContextThemeWrapper로 테마 적용
            val themedContext = android.view.ContextThemeWrapper(context, R.style.Theme_Idol_NumberPicker)
            android.widget.NumberPicker(themedContext).apply {
                minValue = 0
                maxValue = (items.size - 1).coerceAtLeast(0)
                displayedValues = items.toTypedArray()
                value = selectedIndex
                wrapSelectorWheel = false

                // API 29+ 에서 구분선 높이를 0으로 설정
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    selectionDividerHeight = 0
                }

                setOnValueChangedListener { _, _, newVal ->
                    onIndexChange(newVal)
                }
            }
        },
        update = { picker ->
            if (picker.maxValue != (items.size - 1).coerceAtLeast(0)) {
                picker.displayedValues = null
                picker.maxValue = (items.size - 1).coerceAtLeast(0)
                picker.displayedValues = items.toTypedArray()
            }
            if (picker.value != selectedIndex) {
                picker.value = selectedIndex
            }
        },
        modifier = modifier
    )
}

/**
 * 올인데이 설정 다이얼로그
 *
 * old 프로젝트의 BurningDayPurchaseDialogFragment를 Compose로 구현
 * NumberPicker가 포함된 커스텀 다이얼로그
 *
 * @param title 다이얼로그 타이틀
 * @param guideText1 첫 번째 안내 텍스트 (레벨 조건)
 * @param guideText2 두 번째 안내 텍스트 (다이아 비용)
 * @param displayDays 표시할 날짜 목록
 * @param selectedIndex 현재 선택된 날짜 인덱스
 * @param onIndexChange 날짜 선택 변경 콜백
 * @param onConfirm 등록 버튼 클릭 콜백
 * @param onDismiss 취소 버튼 클릭 콜백
 * @param confirmButtonText 등록 버튼 텍스트
 * @param dismissButtonText 취소 버튼 텍스트
 * @param isLoading 로딩 상태
 */
@Composable
fun ExoBurningDayDialog(
    title: String,
    guideText1: String,
    guideText2: String,
    displayDays: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = stringResource(R.string.register),
    dismissButtonText: String = stringResource(R.string.btn_cancel),
    isLoading: Boolean = false,
    isEnabled: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Column(
            modifier = Modifier
                .width(290.dp)
                .background(
                    color = colorResource(id = R.color.text_white_black),
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.gray150),
                    shape = RoundedCornerShape(6.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 타이틀
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = colorResource(id = R.color.main),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            )

            if (isLoading) {
                // 로딩 중
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = colorResource(id = R.color.main)
                    )
                }
            } else {
                // 가이드 텍스트 1 (레벨 조건)
                Text(
                    text = guideText1,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = colorResource(id = R.color.text_gray),
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 25.dp, vertical = 10.dp)
                )

                // 날짜 선택 (NumberPicker)
                if (displayDays.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clipToBounds()
                    ) {
                        ExoNumberPicker(
                            items = displayDays,
                            selectedIndex = selectedIndex,
                            onIndexChange = onIndexChange,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 가이드 텍스트 2 (다이아 비용)
                Text(
                    text = guideText2,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = colorResource(id = R.color.gray900),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
            }

            // 버튼 Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                // 등록 버튼 (왼쪽)
                TextButton(
                    onClick = onConfirm,
                    enabled = isEnabled && !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(bottomStart = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(id = R.color.gray580),
                        disabledContentColor = colorResource(id = R.color.gray300)
                    )
                ) {
                    Text(
                        text = confirmButtonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // 취소 버튼 (오른쪽)
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(id = R.color.gray580)
                    )
                ) {
                    Text(
                        text = dismissButtonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
