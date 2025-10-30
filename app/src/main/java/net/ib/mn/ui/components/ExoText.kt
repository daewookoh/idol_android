package net.ib.mn.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import net.ib.mn.R

/**
 * 앱 전체에서 사용하는 공통 Text 컴포넌트
 *
 * 기본값:
 * - color: text_default (다크모드 자동 대응)
 * - fontSize: 14sp
 * - fontWeight: Normal
 *
 * @param text 텍스트 내용
 * @param modifier Modifier
 * @param color 텍스트 색상 (기본: text_default)
 * @param fontSize 폰트 크기 (기본: 14sp)
 * @param fontStyle 폰트 스타일
 * @param fontWeight 폰트 굵기 (기본: Normal)
 * @param fontFamily 폰트 패밀리
 * @param letterSpacing 글자 간격
 * @param textDecoration 텍스트 데코레이션
 * @param textAlign 텍스트 정렬
 * @param lineHeight 줄 높이
 * @param overflow 오버플로우 처리
 * @param softWrap 소프트 랩
 * @param maxLines 최대 줄 수
 * @param minLines 최소 줄 수
 * @param onTextLayout 텍스트 레이아웃 콜백
 * @param style 텍스트 스타일
 */
@Composable
fun ExoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = colorResource(id = R.color.text_default),
    fontSize: TextUnit = 14.sp,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = FontWeight.Normal,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * AnnotatedString을 지원하는 ExoText
 *
 * @param text AnnotatedString 텍스트 내용
 * @param modifier Modifier
 * @param color 텍스트 색상 (기본: text_default)
 * @param fontSize 폰트 크기 (기본: 14sp)
 * @param fontStyle 폰트 스타일
 * @param fontWeight 폰트 굵기 (기본: Normal)
 * @param fontFamily 폰트 패밀리
 * @param letterSpacing 글자 간격
 * @param textDecoration 텍스트 데코레이션
 * @param textAlign 텍스트 정렬
 * @param lineHeight 줄 높이
 * @param overflow 오버플로우 처리
 * @param softWrap 소프트 랩
 * @param maxLines 최대 줄 수
 * @param minLines 최소 줄 수
 * @param onTextLayout 텍스트 레이아웃 콜백
 * @param style 텍스트 스타일
 */
@Composable
fun ExoText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = colorResource(id = R.color.text_default),
    fontSize: TextUnit = 14.sp,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = FontWeight.Normal,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}
