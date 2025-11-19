package net.ib.mn.presentation.main.menu.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.domain.model.IconMenuItem
import net.ib.mn.domain.model.IconMenuType
import net.ib.mn.ui.theme.ColorPalette

/**
 * 메뉴 아이콘 아이템 컴포넌트
 * 4열 그리드로 표시되는 아이콘 메뉴
 */
@Composable
fun MenuIconItem(
    item: IconMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val itemWidth = (screenWidth.value * 0.2).dp  // 화면 너비의 20%

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 아이콘 이미지 (60dp x 60dp - Old 프로젝트와 동일)
            Box(
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = item.iconResId),
                    contentDescription = stringResource(id = item.labelResId)
                )

                // 뱃지 오버레이
                if (item.hasBadge && item.badgeIconResId != null) {
                    Image(
                        painter = painterResource(id = item.badgeIconResId),
                        contentDescription = "badge",
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 라벨 텍스트 (40dp 높이 - Old 프로젝트와 동일)
            Text(
                text = stringResource(id = item.labelResId),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = ColorPalette.textDefault,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuIconItemPreview() {
    MenuIconItem(
        item = IconMenuItem(
            id = "support",
            labelResId = R.string.support,
            iconResId = R.drawable.icon_menu_support_1,
            type = IconMenuType.SUPPORT
        ),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun MenuIconItemWithBadgePreview() {
    MenuIconItem(
        item = IconMenuItem(
            id = "free_charge",
            labelResId = R.string.btn_free_heart_charge,
            iconResId = R.drawable.icon_menu_freeshop,
            type = IconMenuType.FREE_CHARGE,
            hasBadge = true,
            badgeIconResId = R.drawable.icon_menu_up
        ),
        onClick = {}
    )
}
