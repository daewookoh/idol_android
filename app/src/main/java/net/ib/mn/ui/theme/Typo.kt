package net.ib.mn.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * 앱 전용 Typography 스타일
 */
object ExoTypo {
    // ============ Title Styles ============
    /**
     * 타이틀 텍스트 스타일 (13.sp, Bold, MainLight, Center)
     */
    val title13
        @Composable get() = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.mainLight,
            textAlign = TextAlign.Center
        )

    /**
     * 타이틀 텍스트 스타일 (14.sp, Bold, TextDefault)
     */
    val title14
        @Composable get() = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.textDefault
        )

    /**
     * 타이틀 텍스트 스타일 (15.sp, Bold, Main)
     */
    val title15
        @Composable get() = TextStyle(
            fontSize = 15.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.main
        )

    /**
     * 타이틀 텍스트 스타일 (15.sp, Bold, TextDefault)
     */
    val title15Default
        @Composable get() = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.textDefault
        )

    /**
     * 타이틀 텍스트 스타일 (16.sp, Bold, MainLight, Center)
     */
    val title16
        @Composable get() = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.mainLight,
            textAlign = TextAlign.Center
        )

    /**
     * 타이틀 텍스트 스타일 (18.sp, Bold, TextDefault)
     */
    val title18
        @Composable get() = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.textDefault
        )

    /**
     * 타이틀 텍스트 스타일 (20.sp, AppBar용)
     */
    val title20
        @Composable get() = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            color = ColorPalette.textDefault
        )

    /**
     * 타이틀 텍스트 스타일 (21.sp, Bold, MainLight, Center)
     */
    val title21
        @Composable get() = TextStyle(
            fontSize = 21.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.mainLight,
            textAlign = TextAlign.Center
        )

    // ============ Body Styles ============
    /**
     * 일반 텍스트 스타일 (7.sp, TextDefault)
     * Memorial day badge용
     */
    val body7
        @Composable get() = TextStyle(
            fontSize = 7.sp,
            lineHeight = 7.sp,
            color = ColorPalette.textDefault
        )

    /**
     * 일반 텍스트 스타일 (11.sp, TextGray)
     */
    val body11
        @Composable get() = TextStyle(
            fontSize = 11.sp,
            lineHeight = 11.sp,
            color = ColorPalette.textGray
        )

    /**
     * 일반 텍스트 스타일 (12.sp, TextDimmed)
     */
    val body12
        @Composable get() = TextStyle(
            fontSize = 12.sp,
            color = ColorPalette.textDimmed
        )

    /**
     * 일반 텍스트 스타일 (13.sp, TextDefault)
     */
    val body13
        @Composable get() = TextStyle(
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = ColorPalette.textDefault
        )

    /**
     * 일반 텍스트 스타일 (14.sp, TextDefault)
     */
    val body14
        @Composable get() = TextStyle(
            fontSize = 14.sp,
            color = ColorPalette.textDefault
        )

    val body15
        @Composable get() = TextStyle(
            fontSize = 14.sp,
            color = ColorPalette.textDefault
        )

    /**
     * 일반 텍스트 스타일 (14.sp, Main 컬러)
     */
    val body14Main
        @Composable get() = TextStyle(
            fontSize = 14.sp,
            color = ColorPalette.main,
            fontWeight = FontWeight.Normal
        )

    // ============ Button Styles ============
    /**
     * 버튼 텍스트 스타일 (14.sp, Normal)
     */
    val button14
        @Composable get() = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )

    /**
     * 버튼 텍스트 스타일 (17.sp, Bold)
     */
    val button17
        @Composable get() = TextStyle(
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

    // ============ Label/Badge Styles ============
    /**
     * 라벨 텍스트 스타일 (7.sp, Bold)
     */
    val label7
        @Composable get() = TextStyle(
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold
        )

    /**
     * 라벨 텍스트 스타일 (8.sp, Bold, White)
     */
    val label8
        @Composable get() = TextStyle(
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.white
        )

    /**
     * 라벨 텍스트 스타일 (9.sp, TextDefault)
     */
    val label9
        @Composable get() = TextStyle(
            fontSize = 9.sp,
            color = ColorPalette.textDefault
        )

    /**
     * 라벨 텍스트 스타일 (10.sp, Bold)
     */
    val label10
        @Composable get() = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

    /**
     * 라벨 텍스트 스타일 (13.sp, Bold, MainLight)
     */
    val label13
        @Composable get() = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.mainLight
        )

    // ============ Statistics/Data Styles ============
    /**
     * 통계 텍스트 스타일 (10.sp, Bold, White)
     */
    val stat10
        @Composable get() = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.fixWhite
        )

    /**
     * 통계 텍스트 스타일 (11.sp, Bold, Background100)
     */
    val stat11
        @Composable get() = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.background100
        )

    /**
     * 통계 텍스트 스타일 (13.sp, TextDefault)
     */
    val stat13
        @Composable get() = TextStyle(
            fontSize = 13.sp,
            color = ColorPalette.textDefault
        )

    // ============ Checkbox Styles ============
    /**
     * 체크박스 메인 라벨 (17.sp, Bold, TextDefault)
     */
    val checkboxMain
        @Composable get() = TextStyle(
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.textDefault
        )

    /**
     * 체크박스 서브 라벨 (16.sp, Medium, TextDefault)
     */
    val checkboxSub
        @Composable get() = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = ColorPalette.textDefault
        )

    // ============ Caption Styles ============
    /**
     * 작은 텍스트 스타일 (10.sp, letterSpacing, TextDimmed, Center)
     */
    val caption10
        @Composable get() = TextStyle(
            fontSize = 10.sp,
            lineHeight = 10.sp,
            letterSpacing = (-0.5).sp,
            color = ColorPalette.textDimmed,
            textAlign = TextAlign.Center
        )
}
