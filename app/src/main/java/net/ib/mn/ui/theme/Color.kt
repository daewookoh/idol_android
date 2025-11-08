package net.ib.mn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * CompositionLocal for theme mode.
 *
 * 앱 내부 테마 설정을 전체 Composable 트리에 전파합니다.
 * MainActivity에서 PreferencesManager의 theme을 구독하여 제공합니다.
 */
val LocalDarkTheme = staticCompositionLocalOf<Boolean?> { null }

// ==================== Light Mode Colors ====================
private val MainLight = Color(0xFFFF4444)
private val MainLightVariant = Color(0xFFFF6666)
private val NavigationBarLight = Color(0xFFFFFFFF)
private val BottomSheetTitleLight = Color(0xFF8F8F8F)

// Background Colors - Light
private val Background100Light = Color(0xFFFFFFFF)
private val Background100TransparentLight = Color(0x00FFFFFF)
private val Background200Light = Color(0xFFFFFFFF)
private val Background300Light = Color(0xFFF6F6F6)
private val Background400Light = Color(0xFFF1F1F1)

// Text Colors - Light
private val TextDefaultLight = Color(0xFF333333)
private val TextGrayLight = Color(0xFF666666)
private val TextDimmedLight = Color(0xFFAAAAAA)
private val TextLightLight = Color(0xFFFFFFFF)
private val TextWhiteBlackLight = Color(0xFFFFFFFF)
private val TextLightBlueLight = Color(0xFF68A9E0)
private val ToolbarDefaultLight = Color(0xFF333333)
private val TextDefaultOpacity60Light = Color(0x99333333)
private val TextLightOnlyLight = Color(0xFFFFFFFF)
private val TextHeartVotesLight = Color(0xFFFFFFFF)

// Gray Colors - Light
private val Gray50Light = Color(0xFFF6F6F6)
private val Gray80Light = Color(0xFFF1F1F1)
private val Gray100Light = Color(0xFFEEEEEE)
private val Gray110Light = Color(0xFFE1E1E1)
private val Gray120Light = Color(0xFFE0E1E2)
private val Gray150Light = Color(0xFFDDDDDD)
private val Gray200Light = Color(0xFFCCCCCC)
private val Gray250Light = Color(0xFFBBBBBB)
private val Gray300Light = Color(0xFFAAAAAA)
private val Gray400Light = Color(0xFF999999)
private val Gray500Light = Color(0xFF888888)
private val Gray580Light = Color(0xFF757575)
private val Gray900Light = Color(0xFF222222)
private val Gray1000Light = Color(0xFF000000)
private val Gray1000Opa15Light = Color(0x26000000)
private val Gray1000Opacity60Light = Color(0x99000000)
private val Gray1000Shadow12Light = Color(0x0A000000)
private val GrayToastLight = Color(0xD6222222)

// Chat Colors - Light
private val TextChatLight = Color(0xFF333333)
private val MyChatDeletedLight = Color(0xFFA33E3E)

// Card Colors - Light
private val CardFixTextBlackLight = Color(0xFF121212)
private val CardFixTextVoteLight = Color(0xFF666666)
private val CardLineLight = Color(0xFFEEEEEE)
private val CardSettingLight = Color(0xFFFFFFFF)

// Mention Colors - Light
private val MentionFgLight = Color(0xFF555555)
private val MentionBgLight = Color(0xFFEEEEEE)
private val MentionBgEditingLight = Color(0xFFFFA7A7)

// Brand Colors - Light
private val Brand800Light = Color(0xFFA54949)

// Webview Colors - Light
private val WebviewBackgroundLight = Color(0xFFFFFFFF)

// League Progress Colors - Light
private val SLeagueProgressLight = Color(0xFFFCBB92)
private val ALeagueProgressLight = Color(0xFFFF6666)

// Main Color Variants - Light
private val Main100Light = Color(0xFFFFF4F4)
private val Main200Light = Color(0xFFFFEFEF)
private val Main300Light = Color(0xFFFFBABABA)

// Actionbar - Light
private val ActionbarLight = Color(0xFFFFFFFF)

// Pink Colors - Light
private val Pink200Light = Color(0xFFFFF4F4)
private val Pink400Light = Color(0xFFFF9BC1)
private val Pink500Light = Color(0xFFFF7AAD)

// White - Light
private val WhiteLight = Color(0xFFFFFFFF)

// Notice - Light
private val NoticeBackgroundLight = Color(0xFFFFF4F4)

// ==================== Dark Mode Colors ====================
private val MainDark = Color(0xFFE24848)
private val MainLightVariantDark = Color(0xFFEC716C)
private val NavigationBarDark = Color(0xFF1F1F1F)
private val BottomSheetTitleDark = Color(0xFF8F8F8F)

// Background Colors - Dark
private val Background100Dark = Color(0xFF151515)
private val Background100TransparentDark = Color(0x00151515)
private val Background200Dark = Color(0xFF1f1f1f)
private val Background300Dark = Color(0xFF1f1f1f)
private val Background400Dark = Color(0xFF151515)

// Text Colors - Dark
private val TextDefaultDark = Color(0xFFdbdbdb)
private val TextGrayDark = Color(0xFF999999)
private val TextDimmedDark = Color(0xFF666666)
private val TextLightDark = Color(0xFFFFFFFF)
private val TextWhiteBlackDark = Color(0xFF000000)
private val TextLightBlueDark = Color(0xFF68A9E0)
private val ToolbarDefaultDark = Color(0xFFdddddd)
private val TextDefaultOpacity60Dark = Color(0x99dbdbdb)
private val TextLightOnlyDark = Color(0xFFFFFFFF)
private val TextHeartVotesDark = Color(0xFFFFFFFF)

// Gray Colors - Dark
private val Gray50Dark = Color(0xFF101010)
private val Gray80Dark = Color(0xFF1a1a1a)
private val Gray100Dark = Color(0xFF303030)
private val Gray110Dark = Color(0xFF3a3a3a)
private val Gray120Dark = Color(0xFF262626)
private val Gray150Dark = Color(0xFF404040)
private val Gray200Dark = Color(0xFF606060)
private val Gray250Dark = Color(0xFF707070)
private val Gray300Dark = Color(0xFF808080)
private val Gray400Dark = Color(0xFF999999)
private val Gray500Dark = Color(0xFFaaaaaa)
private val Gray580Dark = Color(0xFF8a8a8a)
private val Gray900Dark = Color(0xFFdddddd)
private val Gray1000Dark = Color(0xFFFFFFFF)
private val Gray1000Opa15Dark = Color(0x26FFFFFF)
private val Gray1000Opacity60Dark = Color(0x99FFFFFF)
private val Gray1000Shadow12Dark = Color(0x0AFFFFFF)
private val GrayToastDark = Color(0xD6dddddd)

// Chat Colors - Dark
private val TextChatDark = Color(0xFFdbdbdb)
private val MyChatDeletedDark = Color(0xFFE24848)

// Card Colors - Dark
private val CardFixTextBlackDark = Color(0xFFdddddd)
private val CardFixTextVoteDark = Color(0xFF999999)
private val CardLineDark = Color(0xFF303030)
private val CardSettingDark = Color(0xFF1F1F1F)

// Mention Colors - Dark
private val MentionFgDark = Color(0xFFdddddd)
private val MentionBgDark = Color(0xFF303030)
private val MentionBgEditingDark = Color(0xFFE24848)

// Brand Colors - Dark
private val Brand800Dark = Color(0xFFE24848)

// Webview Colors - Dark
private val WebviewBackgroundDark = Color(0xFF151515)

// League Progress Colors - Dark
private val SLeagueProgressDark = Color(0xFFFCBB92)
private val ALeagueProgressDark = Color(0xFFFF6666)

// Main Color Variants - Dark
private val Main100Dark = Color(0xFF2a1a1a)
private val Main200Dark = Color(0xFF312626)
private val Main300Dark = Color(0xFF4a2626)

// Actionbar - Dark
private val ActionbarDark = Color(0xFF1F1F1F)

// Pink Colors - Dark
private val Pink200Dark = Color(0xFF2a1a1a)
private val Pink400Dark = Color(0xFFFF9BC1)
private val Pink500Dark = Color(0xFFFF7AAD)

// White - Dark
private val WhiteDark = Color(0xFF414243)

// Notice - Dark
private val NoticeBackgroundDark = Color(0xFF1d1a19)
//private val NoticeBackgroundDark = Color(0xFF2a1a1a)

// ==================== Fixed Colors (테마 변경 없음) ====================
// 고정 색상 (라이트/다크 모드에서 변하지 않음)
private val FixGray1000 = Color(0xFF000000)
private val FixGray900 = Color(0xFF222222)
private val FixWhite = Color(0xFFFFFFFF)
private val FixMain = Color(0xFFFF4444)
private val FixTransparent = Color(0x00000000)

// 기부 뱃지 COLOR (고정)
private val TextAngel = Color(0xFF10CDDD)
private val TextFairy = Color(0xFF9E5FF6)
private val TextMiracle = Color(0xFFFF7D7D)
private val TextRookie = Color(0xFF15CE23)
private val TextSuperRookie = Color(0xFFFFB60E)

// 소리바다 (고정)
private val SoribadaText = Color(0xFF1B0090)
private val SoribadaUnderbar = Color(0xFF52F8FC)

// 퀴즈 (고정)
private val QuizContentColor = Color(0xFFFFFFFF)
private val QuizSolveLabelColor = Color(0xFFEEEEEE)
private val QuizShadow1 = Color(0x00CCCCCC)
private val QuizShadow2 = Color(0x06CCCCCC)
private val QuizShadow3 = Color(0x09CCCCCC)
private val QuizShadow4 = Color(0x0BCCCCCC)
private val QuizShadow5 = Color(0x0DCCCCCC)
private val QuizShadow6 = Color(0x10CCCCCC)
private val IdolQuizCategory = Color(0xFFFF4444)
private val CelebQuizCategory = Color(0xFFA96BD4)

// 쿠폰 (고정)
private val MainYellow = Color(0xFFFFC355)

// 기타 (고정)
private val RedClickBorder = Color(0xFFE53D3D)
private val ButtonOn = Color(0xFF96312C)

// Subscribe (고정)
private val SubscribeLabel = Color(0xFFFF414C)

// Certificate (고정)
private val CertificateBg = Color(0xFF842121)

// Purple (고정)
private val Purple400 = Color(0xFFAA8EFF)
private val Purple500 = Color(0xFF9B6DFF)

// 랭킹 아이템 배지 배경색 (고정)
private val BadgeBirth = Color(0xFFFF9800)
private val BadgeDebut = Color(0xFF4CAF50)
private val BadgeComeback = Color(0xFF9C27B0)
private val BadgeMemorialDay = Color(0xFFF44336)
private val BadgeAllinDay = Color(0xFFE91E63)

// 프로필 테두리 색상 (고정)
private val BorderMiracle = Color(0xFFFF7D7D)
private val BorderFairy = Color(0xFF9E5FF6)
private val BorderAngel = Color(0xFF10CDDD)

// 배지 아이콘 배경색 (고정)
private val BadgeAngelBg = Color(0xFF10CDDD)
private val BadgeFairyBg = Color(0xFF9E5FF6)
private val BadgeMiracleBg = Color(0xFFFF7D7D)
private val BadgeRookieBg = Color(0xFF15CE23)
private val BadgeSuperRookieBg = Color(0xFFFFB60E)

// Awards (고정)
private val AwardsYellow = Color(0xFFFFCC66)

/**
 * Color Palette - 테마 자동 분기 지원
 *
 * 앱 내부 테마 설정 우선, 없으면 시스템 테마 사용.
 * @Composable 함수 내에서만 사용 가능합니다.
 */
object ColorPalette {
    /**
     * 현재 다크 모드 여부를 반환합니다.
     * 1. LocalDarkTheme이 설정되어 있으면 (앱 내부 설정) 그 값 사용
     * 2. null이면 시스템 다크 모드 사용
     */
    private val isDarkTheme: Boolean
        @Composable get() = LocalDarkTheme.current ?: isSystemInDarkTheme()

    // ==================== MAIN COLORS ====================
    val main: Color
        @Composable get() = if (isDarkTheme) MainDark else MainLight

    val mainLight: Color
        @Composable get() = if (isDarkTheme) MainLightVariantDark else MainLightVariant

    val navigationBar: Color
        @Composable get() = if (isDarkTheme) NavigationBarDark else NavigationBarLight

    val bottomSheetTitle: Color
        @Composable get() = if (isDarkTheme) BottomSheetTitleDark else BottomSheetTitleLight

    // ==================== BACKGROUND COLORS ====================
    val background100: Color
        @Composable get() = if (isDarkTheme) Background100Dark else Background100Light

    val background100Transparent: Color
        @Composable get() = if (isDarkTheme) Background100TransparentDark else Background100TransparentLight

    val background200: Color
        @Composable get() = if (isDarkTheme) Background200Dark else Background200Light

    val background300: Color
        @Composable get() = if (isDarkTheme) Background300Dark else Background300Light

    val background400: Color
        @Composable get() = if (isDarkTheme) Background400Dark else Background400Light

    // ==================== TEXT COLORS ====================
    val textDefault: Color
        @Composable get() = if (isDarkTheme) TextDefaultDark else TextDefaultLight

    val textGray: Color
        @Composable get() = if (isDarkTheme) TextGrayDark else TextGrayLight

    val textDimmed: Color
        @Composable get() = if (isDarkTheme) TextDimmedDark else TextDimmedLight

    val textLight: Color
        @Composable get() = if (isDarkTheme) TextLightDark else TextLightLight

    val textWhiteBlack: Color
        @Composable get() = if (isDarkTheme) TextWhiteBlackDark else TextWhiteBlackLight

    val textLightBlue: Color
        @Composable get() = if (isDarkTheme) TextLightBlueDark else TextLightBlueLight

    val toolbarDefault: Color
        @Composable get() = if (isDarkTheme) ToolbarDefaultDark else ToolbarDefaultLight

    val textDefaultOpacity60: Color
        @Composable get() = if (isDarkTheme) TextDefaultOpacity60Dark else TextDefaultOpacity60Light

    val textLightOnly: Color
        @Composable get() = if (isDarkTheme) TextLightOnlyDark else TextLightOnlyLight

    val textHeartVotes: Color
        @Composable get() = if (isDarkTheme) TextHeartVotesDark else TextHeartVotesLight

    // ==================== GRAY COLORS ====================
    val gray50: Color
        @Composable get() = if (isDarkTheme) Gray50Dark else Gray50Light

    val gray80: Color
        @Composable get() = if (isDarkTheme) Gray80Dark else Gray80Light

    val gray100: Color
        @Composable get() = if (isDarkTheme) Gray100Dark else Gray100Light

    val gray110: Color
        @Composable get() = if (isDarkTheme) Gray110Dark else Gray110Light

    val gray120: Color
        @Composable get() = if (isDarkTheme) Gray120Dark else Gray120Light

    val gray150: Color
        @Composable get() = if (isDarkTheme) Gray150Dark else Gray150Light

    val gray200: Color
        @Composable get() = if (isDarkTheme) Gray200Dark else Gray200Light

    val gray250: Color
        @Composable get() = if (isDarkTheme) Gray250Dark else Gray250Light

    val gray300: Color
        @Composable get() = if (isDarkTheme) Gray300Dark else Gray300Light

    val gray400: Color
        @Composable get() = if (isDarkTheme) Gray400Dark else Gray400Light

    val gray500: Color
        @Composable get() = if (isDarkTheme) Gray500Dark else Gray500Light

    val gray580: Color
        @Composable get() = if (isDarkTheme) Gray580Dark else Gray580Light

    val gray900: Color
        @Composable get() = if (isDarkTheme) Gray900Dark else Gray900Light

    val gray1000: Color
        @Composable get() = if (isDarkTheme) Gray1000Dark else Gray1000Light

    val gray1000Opa15: Color
        @Composable get() = if (isDarkTheme) Gray1000Opa15Dark else Gray1000Opa15Light

    val gray1000Opacity60: Color
        @Composable get() = if (isDarkTheme) Gray1000Opacity60Dark else Gray1000Opacity60Light

    val gray1000Shadow12: Color
        @Composable get() = if (isDarkTheme) Gray1000Shadow12Dark else Gray1000Shadow12Light

    val grayToast: Color
        @Composable get() = if (isDarkTheme) GrayToastDark else GrayToastLight

    // ==================== CHAT COLORS ====================
    val textChat: Color
        @Composable get() = if (isDarkTheme) TextChatDark else TextChatLight

    val myChatDeleted: Color
        @Composable get() = if (isDarkTheme) MyChatDeletedDark else MyChatDeletedLight

    // ==================== CELEB CARD COLORS ====================
    val cardFixTextBlack: Color
        @Composable get() = if (isDarkTheme) CardFixTextBlackDark else CardFixTextBlackLight

    val cardFixTextVote: Color
        @Composable get() = if (isDarkTheme) CardFixTextVoteDark else CardFixTextVoteLight

    val cardLine: Color
        @Composable get() = if (isDarkTheme) CardLineDark else CardLineLight

    val cardSetting: Color
        @Composable get() = if (isDarkTheme) CardSettingDark else CardSettingLight

    // ==================== mention ====================
    val mentionFg: Color
        @Composable get() = if (isDarkTheme) MentionFgDark else MentionFgLight

    val mentionBg: Color
        @Composable get() = if (isDarkTheme) MentionBgDark else MentionBgLight

    val mentionBgEditing: Color
        @Composable get() = if (isDarkTheme) MentionBgEditingDark else MentionBgEditingLight

    // ==================== brand ====================
    val brand800: Color
        @Composable get() = if (isDarkTheme) Brand800Dark else Brand800Light

    // ==================== webview ====================
    val webviewBackground: Color
        @Composable get() = if (isDarkTheme) WebviewBackgroundDark else WebviewBackgroundLight

    // ==================== 리그 프로그레스 바 ====================
    val sLeagueProgress: Color
        @Composable get() = if (isDarkTheme) SLeagueProgressDark else SLeagueProgressLight

    val aLeagueProgress: Color
        @Composable get() = if (isDarkTheme) ALeagueProgressDark else ALeagueProgressLight

    // ==================== main 색상 변형 ====================
    val main100: Color
        @Composable get() = if (isDarkTheme) Main100Dark else Main100Light

    val main200: Color
        @Composable get() = if (isDarkTheme) Main200Dark else Main200Light

    val main300: Color
        @Composable get() = if (isDarkTheme) Main300Dark else Main300Light

    // ==================== actionbar ====================
    val actionbar: Color
        @Composable get() = if (isDarkTheme) ActionbarDark else ActionbarLight

    // ==================== pink ====================
    val pink200: Color
        @Composable get() = if (isDarkTheme) Pink200Dark else Pink200Light

    val pink400: Color
        @Composable get() = if (isDarkTheme) Pink400Dark else Pink400Light

    val pink500: Color
        @Composable get() = if (isDarkTheme) Pink500Dark else Pink500Light

    // ==================== white ====================
    val white: Color
        @Composable get() = if (isDarkTheme) WhiteDark else WhiteLight

    // ==================== notice ====================
    val noticeBackground: Color
        @Composable get() = if (isDarkTheme) NoticeBackgroundDark else NoticeBackgroundLight

    // ===================================================================
    // ==================== 고정 색상 (Fixed Colors) ====================
    // ===================================================================
    // 테마 변경 없이 항상 동일한 색상 사용

    // ==================== 기부 뱃지 COLOR ====================
    val textAngel: Color get() = TextAngel
    val textFairy: Color get() = TextFairy
    val textMiracle: Color get() = TextMiracle
    val textRookie: Color get() = TextRookie
    val textSuperRookie: Color get() = TextSuperRookie

    // ==================== 소리바다 ====================
    val soribadaText: Color get() = SoribadaText
    val soribadaUnderbar: Color get() = SoribadaUnderbar

    // ==================== 퀴즈 ====================
    val quizContentColor: Color get() = QuizContentColor
    val quizSolveLabelColor: Color get() = QuizSolveLabelColor
    val quizShadow1: Color get() = QuizShadow1
    val quizShadow2: Color get() = QuizShadow2
    val quizShadow3: Color get() = QuizShadow3
    val quizShadow4: Color get() = QuizShadow4
    val quizShadow5: Color get() = QuizShadow5
    val quizShadow6: Color get() = QuizShadow6
    val idolQuizCategory: Color get() = IdolQuizCategory
    val celebQuizCategory: Color get() = CelebQuizCategory

    // ==================== 쿠폰 ====================
    val mainYellow: Color get() = MainYellow

    // ==================== 기타 ====================
    val redClickBorder: Color get() = RedClickBorder
    val buttonOn: Color get() = ButtonOn

    // ==================== fix colors ====================
    val fixGray1000: Color get() = FixGray1000
    val fixGray900: Color get() = FixGray900
    val fixWhite: Color get() = FixWhite
    val fixMain: Color get() = FixMain
    val fixTransparent: Color get() = FixTransparent

    // ==================== awards ====================
    val awardsYellow: Color get() = AwardsYellow

    // ==================== subscribe ====================
    val subscribeLabel: Color get() = SubscribeLabel

    // ==================== certificate ====================
    val certificateBg: Color get() = CertificateBg

    // ==================== purple ====================
    val purple400: Color get() = Purple400
    val purple500: Color get() = Purple500

    // ==================== 랭킹 아이템 배지 배경색 ====================
    val badgeBirth: Color get() = BadgeBirth
    val badgeDebut: Color get() = BadgeDebut
    val badgeComeback: Color get() = BadgeComeback
    val badgeMemorialDay: Color get() = BadgeMemorialDay
    val badgeAllinDay: Color get() = BadgeAllinDay

    // ==================== 프로필 테두리 색상 ====================
    val borderMiracle: Color get() = BorderMiracle
    val borderFairy: Color get() = BorderFairy
    val borderAngel: Color get() = BorderAngel

    // ==================== 배지 아이콘 배경색 ====================
    val badgeAngelBg: Color get() = BadgeAngelBg
    val badgeFairyBg: Color get() = BadgeFairyBg
    val badgeMiracleBg: Color get() = BadgeMiracleBg
    val badgeRookieBg: Color get() = BadgeRookieBg
    val badgeSuperRookieBg: Color get() = BadgeSuperRookieBg
}
