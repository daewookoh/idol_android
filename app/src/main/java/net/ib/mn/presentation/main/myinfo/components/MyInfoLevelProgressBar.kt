package net.ib.mn.presentation.main.myinfo.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R

/**
 * MyInfo 레벨 프로그레스 바 컴포넌트
 * InvertedTextProgressbar를 Compose로 구현
 * fragment_myinfo.xml의 rl_progress_wrapper와 동일
 */
@Composable
fun MyInfoLevelProgressBar(
    level: Int,
    progress: Int,
    levelUpText: String,
    modifier: Modifier = Modifier
) {
    val mainColor = colorResource(id = R.color.main)
    val backgroundColor = colorResource(id = R.color.background_100)

    // Animate progress
    var animatedProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(progress) {
        animatedProgress = 0f
        // Animate to target progress
        animate(
            initialValue = 0f,
            targetValue = progress.toFloat(),
            animationSpec = tween(
                durationMillis = 300,
                easing = LinearOutSlowInEasing
            )
        ) { value, _ ->
            animatedProgress = value
        }
    }

    Column(
        modifier = modifier
    ) {
        // Level and Level Up Text Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Level Text (level_text)
            androidx.compose.material3.Text(
                text = "LV.$level",
                modifier = Modifier.padding(bottom = 1.dp),
                color = mainColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 10.sp
            )

            // Level Up Text (level_up_text)
            if (level < 9 && levelUpText.isNotEmpty()) {
                androidx.compose.material3.Text(
                    text = levelUpText,
                    modifier = Modifier.padding(bottom = 1.dp),
                    color = mainColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 8.sp
                )
            }
        }

        // Progress Bar with inner padding (1px margin simulation)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(mainColor),
            contentAlignment = Alignment.Center
        ) {
            // Inner background (simulating 1px margin)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                // Progress fill (main color)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animatedProgress / 100f)
                        .background(mainColor)
                        .align(Alignment.CenterStart)
                )

                // Progress Text (inverted text effect)
                androidx.compose.material3.Text(
                    text = "${animatedProgress.toInt()}%",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 8.sp,
                    color = if (animatedProgress > 50) {
                        backgroundColor
                    } else {
                        mainColor
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
