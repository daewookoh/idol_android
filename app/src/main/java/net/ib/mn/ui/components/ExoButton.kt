package net.ib.mn.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.ui.theme.ColorPalette

/**
 * 프로젝트 전역에서 사용하는 공통 Button 컴포넌트
 *
 * 기본 스타일:
 * - 활성화 시: 메인 컬러 배경, 흰색 텍스트
 * - 비활성화 시: 회색 배경, 흰색 텍스트
 * - 로딩 중: CircularProgressIndicator 표시
 * - 둥근 모서리 (10dp)
 *
 * @param onClick 클릭 이벤트
 * @param modifier Modifier (기본: fillMaxWidth)
 * @param enabled 활성화 여부 (기본: true)
 * @param isLoading 로딩 상태 (true일 때 로딩 인디케이터 표시)
 * @param text 버튼 텍스트 (content를 사용하지 않을 때)
 * @param fontSize 텍스트 크기 (기본: 17sp)
 * @param fontWeight 텍스트 굵기 (기본: Bold)
 * @param height 버튼 높이 (기본: 56dp)
 * @param shape 버튼 모양 (기본: RoundedCornerShape(10dp))
 * @param containerColor 활성화 시 배경색 (기본: ColorPalette.main)
 * @param disabledContainerColor 비활성화 시 배경색 (기본: ColorPalette.gray400)
 * @param contentColor 활성화 시 텍스트 색상 (기본: Color.White)
 * @param disabledContentColor 비활성화 시 텍스트 색상 (기본: Color.White)
 * @param contentPadding 내부 패딩
 * @param content 커스텀 버튼 내용 (text 대신 사용)
 */
@Composable
fun ExoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    isLoading: Boolean = false,
    text: String? = null,
    fontSize: TextUnit = 17.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    height: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    containerColor: Color = ColorPalette.main,
    disabledContainerColor: Color = ColorPalette.gray400,
    contentColor: Color = ColorPalette.textDefault,
    disabledContentColor: Color = ColorPalette.textDimmed,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        ),
        shape = shape,
        contentPadding = contentPadding
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            }
            content != null -> {
                content()
            }
            text != null -> {
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = fontWeight
                )
            }
        }
    }
}

/**
 * ExoButton의 Outlined 변형
 *
 * 배경색이 투명하고 테두리가 있는 버튼
 *
 * @param onClick 클릭 이벤트
 * @param modifier Modifier (기본: fillMaxWidth)
 * @param enabled 활성화 여부 (기본: true)
 * @param isLoading 로딩 상태
 * @param text 버튼 텍스트
 * @param fontSize 텍스트 크기 (기본: 17sp)
 * @param fontWeight 텍스트 굵기 (기본: Bold)
 * @param height 버튼 높이 (기본: 56dp)
 * @param shape 버튼 모양 (기본: RoundedCornerShape(10dp))
 * @param borderColor 테두리 색상 (기본: ColorPalette.main)
 * @param contentColor 텍스트 색상 (기본: ColorPalette.main)
 * @param disabledBorderColor 비활성화 시 테두리 색상 (기본: ColorPalette.gray400)
 * @param disabledContentColor 비활성화 시 텍스트 색상 (기본: ColorPalette.gray400)
 * @param content 커스텀 버튼 내용
 */
@Composable
fun ExoOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    isLoading: Boolean = false,
    text: String? = null,
    fontSize: TextUnit = 17.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    height: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    borderColor: Color = ColorPalette.main,
    contentColor: Color = ColorPalette.main,
    disabledBorderColor: Color = ColorPalette.gray400,
    disabledContentColor: Color = ColorPalette.gray400,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = disabledContentColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (enabled && !isLoading) borderColor else disabledBorderColor
        ),
        shape = shape
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            }
            content != null -> {
                content()
            }
            text != null -> {
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = fontWeight
                )
            }
        }
    }
}

/**
 * ExoButton의 Text 변형
 *
 * 배경색과 테두리가 없는 텍스트 버튼
 *
 * @param onClick 클릭 이벤트
 * @param modifier Modifier (기본: fillMaxWidth)
 * @param enabled 활성화 여부 (기본: true)
 * @param isLoading 로딩 상태
 * @param text 버튼 텍스트
 * @param fontSize 텍스트 크기 (기본: 17sp)
 * @param fontWeight 텍스트 굵기 (기본: Bold)
 * @param height 버튼 높이 (기본: 56dp)
 * @param contentColor 텍스트 색상 (기본: ColorPalette.main)
 * @param disabledContentColor 비활성화 시 텍스트 색상 (기본: ColorPalette.gray400)
 * @param content 커스텀 버튼 내용
 */
@Composable
fun ExoTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    isLoading: Boolean = false,
    text: String? = null,
    fontSize: TextUnit = 17.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    height: Dp = 56.dp,
    contentColor: Color = ColorPalette.main,
    disabledContentColor: Color = ColorPalette.gray400,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = disabledContentColor
        )
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            }
            content != null -> {
                content()
            }
            text != null -> {
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = fontWeight
                )
            }
        }
    }
}
