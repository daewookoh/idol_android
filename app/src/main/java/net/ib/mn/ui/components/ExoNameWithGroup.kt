package net.ib.mn.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R

/**
 * ExoNameWithGroup - 이름과 그룹명을 표시하는 공용 컴포넌트
 *
 * "이름_그룹명" 형식의 문자열을 받아서 이름과 그룹명으로 분리하여 표시합니다.
 *
 * 사용 예:
 * ```
 * ExoNameWithGroup(
 *     fullName = "슬기_레드벨벳",
 *     nameFontSize = 14.sp,
 *     groupFontSize = 10.sp
 * )
 * ```
 *
 * @param fullName "이름_그룹명" 형식의 전체 이름. "_"가 없으면 이름만 표시됩니다.
 * @param modifier Modifier
 * @param nameFontSize 이름 폰트 사이즈 (기본값: 14.sp)
 * @param groupFontSize 그룹명 폰트 사이즈 (기본값: 10.sp)
 * @param nameColor 이름 색상 (기본값: R.color.text_default)
 * @param groupColor 그룹명 색상 (기본값: R.color.text_dimmed)
 * @param nameFontWeight 이름 폰트 굵기 (기본값: FontWeight.Bold)
 * @param spacing 이름과 그룹명 사이 간격 (기본값: 5.dp)
 * @param textAlign 텍스트 정렬 (기본값: TextAlign.Start)
 */
@Composable
fun ExoNameWithGroup(
    fullName: String,
    modifier: Modifier = Modifier,
    nameFontSize: TextUnit = 14.sp,
    groupFontSize: TextUnit = 10.sp,
    nameColor: Int = R.color.text_default,
    groupColor: Int = R.color.text_dimmed,
    nameFontWeight: FontWeight = FontWeight.Bold,
    spacing: androidx.compose.ui.unit.Dp = 5.dp,
    textAlign: TextAlign = TextAlign.Start
) {
    // "_"로 분리
    val parts = fullName.split("_", limit = 2)
    val name = parts.getOrNull(0) ?: fullName
    val groupName = parts.getOrNull(1)

    Row(
        modifier = modifier
    ) {
        // 이름
        Text(
            text = name,
            fontSize = nameFontSize,
            fontWeight = nameFontWeight,
            color = colorResource(nameColor),
            textAlign = textAlign,
            modifier = Modifier.alignByBaseline()
        )

        // 그룹명 (있는 경우만 표시)
        if (!groupName.isNullOrEmpty()) {
            Spacer(modifier = Modifier.width(spacing))
            Text(
                text = groupName,
                fontSize = groupFontSize,
                color = colorResource(groupColor),
                textAlign = textAlign,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}
