package net.ib.mn.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.ib.mn.R

@Composable
fun BottomTab(
    modifier: Modifier = Modifier,
    tabTitle: String = "",
    isSelect: Boolean
) {
    val backgroundColor =
        Color(ContextCompat.getColor(LocalContext.current, R.color.background_100))
    val topDividerColor = Color(ContextCompat.getColor(LocalContext.current, R.color.gray150))
    val selectDividerColor = Color(ContextCompat.getColor(LocalContext.current, R.color.main))

    val textColor = if (isSelect) {
        Color(ContextCompat.getColor(LocalContext.current, R.color.main))
    } else {
        Color(ContextCompat.getColor(LocalContext.current, R.color.text_dimmed))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(backgroundColor)
            .then(modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(topDividerColor)
        )
        if (isSelect) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp)
                    .height(3.dp)
                    .background(selectDividerColor)
            )
        }

        Text(
            modifier = Modifier
                .align(Alignment.Center),
            text = tabTitle,
            fontSize = 13.sp,
            color = textColor
        )
    }
}