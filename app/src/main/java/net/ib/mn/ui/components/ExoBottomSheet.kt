package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

/**
 * ExoBottomSheet 선택 아이템 데이터
 */
data class ExoBottomSheetItem<T>(
    val value: T,
    val label: String
)

/**
 * ExoBottomSheet 타입
 */
enum class ExoBottomSheetType {
    /** 텍스트 리스트 바텀시트 - 배경색 gray100, 드래그 핸들 표시 */
    LIST,
    /** 액션 바텀시트 - 배경색 gray100, 드래그 핸들 표시, 텍스트 가운데 정렬 */
    ACTION,
    /** 커스텀 디자인 바텀시트 - 투명 배경, 드래그 핸들 없음 */
    DESIGN
}

/**
 * ExoBottomSheet - 공용 바텀시트 컴포넌트
 *
 * 프로젝트 전체에서 사용하는 통일된 바텀시트 UI
 *
 * @param onDismissRequest 바텀시트 닫기 요청 콜백
 * @param type 바텀시트 타입 (LIST: 텍스트 리스트, DESIGN: 커스텀 디자인)
 * @param modifier Modifier
 * @param sheetState 바텀시트 상태
 * @param containerColor 바텀시트 배경색 (LIST: gray100, DESIGN: Transparent)
 * @param shape 바텀시트 모양
 * @param title LIST/ACTION 타입에서 핸들바 아래에 표시할 타이틀 (옵셔널)
 * @param hasCloseButton DESIGN 타입에서 우측 상단에 닫기 버튼 표시 여부 (기본값: false)
 * @param content 바텀시트 콘텐츠
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExoBottomSheet(
    onDismissRequest: () -> Unit,
    type: ExoBottomSheetType = ExoBottomSheetType.LIST,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    ),
    containerColor: Color = when (type) {
        ExoBottomSheetType.LIST, ExoBottomSheetType.ACTION -> ColorPalette.gray100
        ExoBottomSheetType.DESIGN -> Color.Transparent
    },
    shape: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    title: String? = null,
    hasCloseButton: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = containerColor,
        shape = shape,
        dragHandle = when (type) {
            ExoBottomSheetType.LIST, ExoBottomSheetType.ACTION -> {
                {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ExoBottomSheetDragHandle()
                        // LIST/ACTION 타입에서 타이틀이 있으면 핸들바 아래에 표시
                        if (title != null) {
                            Text(
                                text = title,
                                style = ExoTypo.body15.copy(fontWeight = FontWeight.Bold),
                                color = ColorPalette.textGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .heightIn(min = 42.dp)
                            )
                        }
                    }
                }
            }
            ExoBottomSheetType.DESIGN -> null
        },
        sheetMaxWidth = BottomSheetDefaults.SheetMaxWidth
    ) {
        if (type == ExoBottomSheetType.DESIGN && hasCloseButton) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content
                )
                Icon(
                    painter = painterResource(R.drawable.btn_popup_close),
                    contentDescription = "Close",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp, end = 24.dp)
                        .size(12.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDismissRequest() }
                )
            }
        } else {
            content()
        }
    }
}

/**
 * ExoBottomSheetDragHandle - 기본 드래그 핸들
 */
@Composable
fun ExoBottomSheetDragHandle(
    modifier: Modifier = Modifier,
    width: Dp = 36.dp,
    height: Dp = 4.dp,
    color: Color = ColorPalette.gray150
) {
    Box(
        modifier = modifier
            .padding(top = 12.dp)
            .size(width = width, height = height)
            .background(
                color = color,
                shape = RoundedCornerShape(height / 2)
            )
    )
}

/**
 * ExoBottomSheetList - 선택 리스트용 바텀시트
 *
 * DEFAULT 타입 바텀시트에서 아이템 선택을 위한 공용 컴포넌트
 * horizontal 패딩 24dp, 스크롤 가능, navigation bar 패딩 적용
 *
 * @param items 선택 아이템 리스트
 * @param selectedValue 현재 선택된 값
 * @param onItemSelected 아이템 선택 콜백
 * @param onDismissRequest 바텀시트 닫기 요청 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ExoBottomSheetList(
    items: List<ExoBottomSheetItem<T>>,
    selectedValue: T,
    onItemSelected: (T) -> Unit,
    onDismissRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp / 2).dp

    ExoBottomSheet(
        onDismissRequest = onDismissRequest,
        type = ExoBottomSheetType.LIST
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(items = items, key = { it.value.hashCode() }) { item ->
                    val isSelected = item.value == selectedValue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onItemSelected(item.value)
                                onDismissRequest()
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.label,
                            style = ExoTypo.body14.copy(fontWeight = FontWeight.Medium),
                            color = ColorPalette.textDefault
                        )
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.icon_check),
                                contentDescription = null,
                                tint = ColorPalette.textDefault,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(16.dp)
            )
        }
    }
}

/**
 * ExoBottomSheetAction 아이템 데이터
 *
 * @param labelResId 다국어 문자열 리소스 ID
 * @param onClick 클릭 콜백
 */
data class ExoBottomSheetActionItem(
    @StringRes val labelResId: Int,
    val onClick: () -> Unit
)

/**
 * ExoBottomSheetAction - 액션 리스트용 바텀시트
 *
 * 텍스트가 가운데 정렬되고, 아이템 사이에 구분선이 있는 액션 바텀시트
 *
 * @param items 액션 아이템 리스트
 * @param onDismissRequest 바텀시트 닫기 요청 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExoBottomSheetAction(
    items: List<ExoBottomSheetActionItem>,
    onDismissRequest: () -> Unit
) {
    ExoBottomSheet(
        onDismissRequest = onDismissRequest,
        type = ExoBottomSheetType.ACTION
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            items.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable {
                            item.onClick()
                            onDismissRequest()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(item.labelResId),
                        style = ExoTypo.body14.copy(fontWeight = FontWeight.Medium),
                        color = ColorPalette.textDefault,
                        textAlign = TextAlign.Center
                    )
                }

                if (index < items.lastIndex) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = ColorPalette.gray100
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
