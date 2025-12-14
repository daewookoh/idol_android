package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

/**
 * Notice 박스 공용 컴포넌트
 *
 * @param text 표시할 텍스트
 * @param style 텍스트 스타일 (기본값: ExoTypo.typo13Bold)
 * @param color 텍스트 색상 (기본값: mainLight)
 * @param textAlign 텍스트 정렬 (기본값: Center)
 * @param modifier Modifier
 */
@Composable
fun ExoNoticeBox(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = ExoTypo.typo13Bold,
    color: Color = ColorPalette.mainLight,
    textAlign: TextAlign = TextAlign.Center
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ColorPalette.noticeBackground)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            textAlign = textAlign
        )
    }
}
