package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
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
 */
@Composable
fun ExoTitleDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmButtonText: String = stringResource(R.string.confirm)
) {
    ExoDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmButtonText = confirmButtonText
    )
}
