package net.ib.mn.ui.theme

import androidx.compose.ui.graphics.Color

// Default Material colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// App custom colors from old project - Light Mode
val MainLight = Color(0xFFFF4444)
val MainLightVariant = Color(0xFFFF6666)
val RedClickBorderLight = Color(0xFFE53D3D)

val Background100Light = Color(0xFFFFFFFF)
val Background200Light = Color(0xFFFFFFFF)
val Background300Light = Color(0xFFF6F6F6)
val Background400Light = Color(0xFFF1F1F1)

val TextDefaultLight = Color(0xFF333333)
val TextGrayLight = Color(0xFF666666)
val TextDimmedLight = Color(0xFFAAAAAA)
val TextLightLight = Color(0xFFFFFFFF)
val TextWhiteBlackLight = Color(0xFFFFFFFF)

val Gray50Light = Color(0xFFF6F6F6)
val Gray80Light = Color(0xFFF1F1F1)
val Gray100Light = Color(0xFFEEEEEE)
val Gray150Light = Color(0xFFDDDDDD)
val Gray200Light = Color(0xFFCCCCCC)
val Gray300Light = Color(0xFFAAAAAA)
val Gray580Light = Color(0xFF757575)
val Gray900Light = Color(0xFF222222)

// App custom colors from old project - Dark Mode
val MainDark = Color(0xFFE24848)
val MainDarkVariant = Color(0xFFec716c)
val RedClickBorderDark = Color(0xFF9C2929)

val Background100Dark = Color(0xFF151515)
val Background200Dark = Color(0xFF1f1f1f)
val Background300Dark = Color(0xFF1f1f1f)
val Background400Dark = Color(0xFF151515)

val TextDefaultDark = Color(0xFFdbdbdb)
val TextGrayDark = Color(0xFF999999)
val TextDimmedDark = Color(0xFF666666)
val TextLightDark = Color(0xFFcccccc)
val TextWhiteBlackDark = Color(0xFF121212)

val Gray50Dark = Color(0xFF101010)
val Gray80Dark = Color(0xFF1a1a1a)
val Gray100Dark = Color(0xFF303030)
val Gray150Dark = Color(0xFF404040)
val Gray200Dark = Color(0xFF606060)
val Gray300Dark = Color(0xFF808080)
val Gray580Dark = Color(0xFF8a8a8a)
val Gray900Dark = Color(0xFFdddddd)

/**
 * Color Palette for easy access in Composables
 * 현재는 Light 모드만 지원, 추후 다크모드 추가 가능
 */
object ColorPalette {
    val main = MainLight
    val mainVariant = MainLightVariant
    val redClickBorder = RedClickBorderLight

    val background100 = Background100Light
    val background200 = Background200Light
    val background300 = Background300Light
    val background400 = Background400Light

    val textDefault = TextDefaultLight
    val textGray = TextGrayLight
    val textDimmed = TextDimmedLight
    val textLight = TextLightLight
    val textWhiteBlack = TextWhiteBlackLight

    val gray50 = Gray50Light
    val gray80 = Gray80Light
    val gray100 = Gray100Light
    val gray110 = Color(0xFFE8E8E8)
    val gray150 = Gray150Light
    val gray200 = Gray200Light
    val gray300 = Gray300Light
    val gray400 = Color(0xFF999999)
    val gray500 = Color(0xFF888888)
    val gray580 = Gray580Light
    val gray900 = Gray900Light
}