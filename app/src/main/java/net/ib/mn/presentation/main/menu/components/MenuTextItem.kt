package net.ib.mn.presentation.main.menu.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.domain.model.TextMenuItem
import net.ib.mn.domain.model.TextMenuType
import net.ib.mn.ui.theme.ColorPalette

/**
 * 메뉴 텍스트 아이템 컴포넌트
 * 리스트 형태로 표시되는 텍스트 메뉴
 */
@Composable
fun MenuTextItem(
    item: TextMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)  // Old 프로젝트와 동일
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(start = 20.dp),  // Old 프로젝트와 동일
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘 박스 (30dp - Old 프로젝트와 동일)
        Box(
            modifier = Modifier.size(30.dp)
        ) {
            Image(
                painter = painterResource(id = item.iconResId),
                contentDescription = stringResource(id = item.labelResId),
                modifier = Modifier.size(30.dp)
            )

            // 뱃지 (빨간 점)
            if (item.hasBadge) {
                Box(
                    modifier = Modifier
                        .size(7.dp)  // Old 프로젝트와 동일
                        .align(Alignment.TopEnd)
                        .background(
                            color = ColorPalette.main,  // TODO: ColorPalette.alertColor
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))  // Old 프로젝트와 동일

        // 타이틀
        Text(
            text = stringResource(id = item.labelResId),
            fontSize = 15.sp,  // Old 프로젝트와 동일
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = ColorPalette.textDefault,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MenuTextItemPreview() {
    MenuTextItem(
        item = TextMenuItem(
            id = "vote_certificate",
            labelResId = R.string.certificate_title,
            iconResId = R.drawable.icon_menu_notice,
            type = TextMenuType.VOTE_CERTIFICATE
        ),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun MenuTextItemWithBadgePreview() {
    MenuTextItem(
        item = TextMenuItem(
            id = "notice",
            labelResId = R.string.setting_menu01,
            iconResId = R.drawable.icon_menu_notice,
            type = TextMenuType.NOTICE,
            hasBadge = true
        ),
        onClick = {}
    )
}
