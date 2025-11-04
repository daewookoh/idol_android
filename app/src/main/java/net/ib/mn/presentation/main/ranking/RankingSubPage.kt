package net.ib.mn.presentation.main.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.data.model.TypeListModel

/**
 * 공통 Ranking SubPage
 */
@Composable
fun RankingSubPage(
    type: TypeListModel,
    modifier: Modifier = Modifier
) {
    // code가 있으면 그대로 사용, 없으면 type_gender 형식 사용
    // 예: code="SOLO_M" 또는 code="PR_G_M" -> 그대로 표시
    // code가 없는 경우(HEARTPICK 등) -> "HEARTPICK_M" 형식으로 표시
    val displayText = if (!type.code.isNullOrEmpty()) {
        type.code!!
    } else {
        val genderSuffix = if (type.isFemale) "F" else "M"
        "${type.type}_$genderSuffix"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_100))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.text_default)
        )
    }
}
