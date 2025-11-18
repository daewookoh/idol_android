package net.ib.mn.presentation.main.myinfo.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import java.text.NumberFormat
import java.util.Locale

/**
 * MyInfo 레벨 프로그레스 바 컴포넌트
 * fragment_my_heart_info.xml의 layout_level과 동일
 */
@Composable
fun MyInfoLevelProgressBar(
    level: Int,
    progress: Int,
    levelUpText: String,
    totalExp: String,
    modifier: Modifier = Modifier,
    onInfoClick: () -> Unit = {}
) {
    val mainLightColor = colorResource(id = R.color.main_light)
    val main100Color = colorResource(id = R.color.main100)
    val textDefaultColor = colorResource(id = R.color.text_default)

    // Animate progress
    var animatedProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(progress) {
        animate(
            initialValue = animatedProgress,
            targetValue = progress.toFloat() / 100f,
            animationSpec = tween(
                durationMillis = 300,
                easing = LinearOutSlowInEasing
            )
        ) { value, _ ->
            animatedProgress = value
        }
    }

    // layout_level: bg_radius_12_main100, minHeight 70dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(main100Color)
            .heightIn(min = 70.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Row: tv_level + tv_all_exp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // tv_level: "Lv. 30" (12sp, bold, main_light)
                val formattedLevel = NumberFormat.getNumberInstance(Locale.getDefault()).format(level)
                Text(
                    text = "Lv. $formattedLevel",
                    color = mainLightColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 12.sp
                )

                // tv_all_exp: "총 615,163,485" (10sp, main_light)
                if (totalExp.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(id = R.string.total_amount, totalExp),
                        color = mainLightColor,
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // progress_level: ProgressBar (15dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(15.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorResource(id = R.color.background_100))
            ) {
                Box(
                    modifier = Modifier
                        .height(15.dp)
                        .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(color = colorResource(id = R.color.main))
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // tv_next_level: "다음 레벨까지 13,541" (12sp, 우측 정렬, text_default)
            if (levelUpText.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.next_level_progress, levelUpText),
                    color = textDefaultColor,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        // btn_level_info: 정보 아이콘 (ZStack 형식으로 최상단에 배치)
        Image(
            painter = painterResource(id = R.drawable.btn_info_black),
            contentDescription = "Level Info",
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.TopEnd).offset(x=12.dp, y=(-5).dp)
                .clickable { onInfoClick() }
        )
    }
}
