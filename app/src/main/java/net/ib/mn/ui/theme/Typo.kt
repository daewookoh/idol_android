package net.ib.mn.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 18.sp
    )
)

/**
 * 앱 전용 Typography - fontSize와 lineHeight만 지정
 * lineHeight = fontSize + 2
 *
 * typo{size} - 기본 (Normal weight)
 * typo{size}Bold - Bold weight
 * typo{size}Medium - Medium weight
 * typo{size}Main - Bold + Main 컬러
 * typo{size}Gray - Normal + textGray 컬러
 * typo{size}FixWhite - Normal + fixWhite 컬러
 */
object ExoTypo {
    private val defaultLetterSpacing = 0.1.sp

    // 기본 스타일 (Normal weight) - 7sp ~ 24sp
    val typo7 = TextStyle(fontSize = 7.sp, lineHeight = 9.sp, letterSpacing = defaultLetterSpacing)
    val typo8 = TextStyle(fontSize = 8.sp, lineHeight = 10.sp, letterSpacing = defaultLetterSpacing)
    val typo9 = TextStyle(fontSize = 9.sp, lineHeight = 11.sp, letterSpacing = defaultLetterSpacing)
    val typo10 = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = defaultLetterSpacing)
    val typo11 = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = defaultLetterSpacing)
    val typo12 = TextStyle(fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = defaultLetterSpacing)
    val typo13 = TextStyle(fontSize = 13.sp, lineHeight = 15.sp, letterSpacing = defaultLetterSpacing)
    val typo14 = TextStyle(fontSize = 14.sp, lineHeight = 16.sp, letterSpacing = defaultLetterSpacing)
    val typo15 = TextStyle(fontSize = 15.sp, lineHeight = 17.sp, letterSpacing = defaultLetterSpacing)
    val typo16 = TextStyle(fontSize = 16.sp, lineHeight = 18.sp, letterSpacing = defaultLetterSpacing)
    val typo17 = TextStyle(fontSize = 17.sp, lineHeight = 19.sp, letterSpacing = defaultLetterSpacing)
    val typo18 = TextStyle(fontSize = 18.sp, lineHeight = 20.sp, letterSpacing = defaultLetterSpacing)
    val typo19 = TextStyle(fontSize = 19.sp, lineHeight = 21.sp, letterSpacing = defaultLetterSpacing)
    val typo20 = TextStyle(fontSize = 20.sp, lineHeight = 22.sp, letterSpacing = defaultLetterSpacing)
    val typo21 = TextStyle(fontSize = 21.sp, lineHeight = 23.sp, letterSpacing = defaultLetterSpacing)
    val typo22 = TextStyle(fontSize = 22.sp, lineHeight = 24.sp, letterSpacing = defaultLetterSpacing)
    val typo24 = TextStyle(fontSize = 24.sp, lineHeight = 26.sp, letterSpacing = defaultLetterSpacing)

    // Bold 스타일 - 기본 스타일에서 fontWeight만 변경
    val typo7Bold = typo7.copy(fontWeight = FontWeight.Bold)
    val typo8Bold = typo8.copy(fontWeight = FontWeight.Bold)
    val typo9Bold = typo9.copy(fontWeight = FontWeight.Bold)
    val typo10Bold = typo10.copy(fontWeight = FontWeight.Bold)
    val typo11Bold = typo11.copy(fontWeight = FontWeight.Bold)
    val typo12Bold = typo12.copy(fontWeight = FontWeight.Bold)
    val typo13Bold = typo13.copy(fontWeight = FontWeight.Bold)
    val typo14Bold = typo14.copy(fontWeight = FontWeight.Bold)
    val typo15Bold = typo15.copy(fontWeight = FontWeight.Bold)
    val typo16Bold = typo16.copy(fontWeight = FontWeight.Bold)
    val typo17Bold = typo17.copy(fontWeight = FontWeight.Bold)
    val typo18Bold = typo18.copy(fontWeight = FontWeight.Bold)
    val typo19Bold = typo19.copy(fontWeight = FontWeight.Bold)
    val typo20Bold = typo20.copy(fontWeight = FontWeight.Bold)
    val typo21Bold = typo21.copy(fontWeight = FontWeight.Bold)
    val typo22Bold = typo22.copy(fontWeight = FontWeight.Bold)
    val typo24Bold = typo24.copy(fontWeight = FontWeight.Bold)

    // Medium 스타일 - 기본 스타일에서 fontWeight만 변경
    val typo7Medium = typo7.copy(fontWeight = FontWeight.Medium)
    val typo8Medium = typo8.copy(fontWeight = FontWeight.Medium)
    val typo9Medium = typo9.copy(fontWeight = FontWeight.Medium)
    val typo10Medium = typo10.copy(fontWeight = FontWeight.Medium)
    val typo11Medium = typo11.copy(fontWeight = FontWeight.Medium)
    val typo12Medium = typo12.copy(fontWeight = FontWeight.Medium)
    val typo13Medium = typo13.copy(fontWeight = FontWeight.Medium)
    val typo14Medium = typo14.copy(fontWeight = FontWeight.Medium)
    val typo15Medium = typo15.copy(fontWeight = FontWeight.Medium)
    val typo16Medium = typo16.copy(fontWeight = FontWeight.Medium)
    val typo17Medium = typo17.copy(fontWeight = FontWeight.Medium)
    val typo18Medium = typo18.copy(fontWeight = FontWeight.Medium)
    val typo19Medium = typo19.copy(fontWeight = FontWeight.Medium)
    val typo20Medium = typo20.copy(fontWeight = FontWeight.Medium)
    val typo21Medium = typo21.copy(fontWeight = FontWeight.Medium)
    val typo22Medium = typo22.copy(fontWeight = FontWeight.Medium)
    val typo24Medium = typo24.copy(fontWeight = FontWeight.Medium)

    // Main 스타일 - Bold 스타일에서 color만 추가
    val typo7Main @Composable get() = typo7Bold.copy(color = ColorPalette.main)
    val typo8Main @Composable get() = typo8Bold.copy(color = ColorPalette.main)
    val typo9Main @Composable get() = typo9Bold.copy(color = ColorPalette.main)
    val typo10Main @Composable get() = typo10Bold.copy(color = ColorPalette.main)
    val typo11Main @Composable get() = typo11Bold.copy(color = ColorPalette.main)
    val typo12Main @Composable get() = typo12Bold.copy(color = ColorPalette.main)
    val typo13Main @Composable get() = typo13Bold.copy(color = ColorPalette.main)
    val typo14Main @Composable get() = typo14Bold.copy(color = ColorPalette.main)
    val typo15Main @Composable get() = typo15Bold.copy(color = ColorPalette.main)
    val typo16Main @Composable get() = typo16Bold.copy(color = ColorPalette.main)
    val typo17Main @Composable get() = typo17Bold.copy(color = ColorPalette.main)
    val typo18Main @Composable get() = typo18Bold.copy(color = ColorPalette.main)
    val typo19Main @Composable get() = typo19Bold.copy(color = ColorPalette.main)
    val typo20Main @Composable get() = typo20Bold.copy(color = ColorPalette.main)
    val typo21Main @Composable get() = typo21Bold.copy(color = ColorPalette.main)
    val typo22Main @Composable get() = typo22Bold.copy(color = ColorPalette.main)
    val typo24Main @Composable get() = typo24Bold.copy(color = ColorPalette.main)

    // Gray 스타일 - 기본 스타일에서 color만 추가
    val typo7Gray @Composable get() = typo7.copy(color = ColorPalette.textGray)
    val typo8Gray @Composable get() = typo8.copy(color = ColorPalette.textGray)
    val typo9Gray @Composable get() = typo9.copy(color = ColorPalette.textGray)
    val typo10Gray @Composable get() = typo10.copy(color = ColorPalette.textGray)
    val typo11Gray @Composable get() = typo11.copy(color = ColorPalette.textGray)
    val typo12Gray @Composable get() = typo12.copy(color = ColorPalette.textGray)
    val typo13Gray @Composable get() = typo13.copy(color = ColorPalette.textGray)
    val typo14Gray @Composable get() = typo14.copy(color = ColorPalette.textGray)
    val typo15Gray @Composable get() = typo15.copy(color = ColorPalette.textGray)
    val typo16Gray @Composable get() = typo16.copy(color = ColorPalette.textGray)
    val typo17Gray @Composable get() = typo17.copy(color = ColorPalette.textGray)
    val typo18Gray @Composable get() = typo18.copy(color = ColorPalette.textGray)
    val typo19Gray @Composable get() = typo19.copy(color = ColorPalette.textGray)
    val typo20Gray @Composable get() = typo20.copy(color = ColorPalette.textGray)
    val typo21Gray @Composable get() = typo21.copy(color = ColorPalette.textGray)
    val typo22Gray @Composable get() = typo22.copy(color = ColorPalette.textGray)
    val typo24Gray @Composable get() = typo24.copy(color = ColorPalette.textGray)

    // FixWhite 스타일 - 기본 스타일에서 color만 추가
    val typo7FixWhite @Composable get() = typo7.copy(color = ColorPalette.fixWhite)
    val typo8FixWhite @Composable get() = typo8.copy(color = ColorPalette.fixWhite)
    val typo9FixWhite @Composable get() = typo9.copy(color = ColorPalette.fixWhite)
    val typo10FixWhite @Composable get() = typo10.copy(color = ColorPalette.fixWhite)
    val typo11FixWhite @Composable get() = typo11.copy(color = ColorPalette.fixWhite)
    val typo12FixWhite @Composable get() = typo12.copy(color = ColorPalette.fixWhite)
    val typo13FixWhite @Composable get() = typo13.copy(color = ColorPalette.fixWhite)
    val typo14FixWhite @Composable get() = typo14.copy(color = ColorPalette.fixWhite)
    val typo15FixWhite @Composable get() = typo15.copy(color = ColorPalette.fixWhite)
    val typo16FixWhite @Composable get() = typo16.copy(color = ColorPalette.fixWhite)
    val typo17FixWhite @Composable get() = typo17.copy(color = ColorPalette.fixWhite)
    val typo18FixWhite @Composable get() = typo18.copy(color = ColorPalette.fixWhite)
    val typo19FixWhite @Composable get() = typo19.copy(color = ColorPalette.fixWhite)
    val typo20FixWhite @Composable get() = typo20.copy(color = ColorPalette.fixWhite)
    val typo21FixWhite @Composable get() = typo21.copy(color = ColorPalette.fixWhite)
    val typo22FixWhite @Composable get() = typo22.copy(color = ColorPalette.fixWhite)
    val typo24FixWhite @Composable get() = typo24.copy(color = ColorPalette.fixWhite)
}
