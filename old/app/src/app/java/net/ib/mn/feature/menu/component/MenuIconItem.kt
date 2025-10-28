package net.ib.mn.feature.menu.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.core.designsystem.util.noRippleClickable
import net.ib.mn.feature.menu.IconMenu
import net.ib.mn.feature.menu.getIconMenuBadgeResId

@Composable
fun MenuIconItem(
    modifier: Modifier = Modifier,
    menuItem: IconMenu,
    isPreview: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.noRippleClickable {
            onClick()
        }
    ) {
        Box(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                painter = painterResource(id = menuItem.icon),
                contentDescription = "menuIcon",
                tint = Color.Unspecified
            )

            // Extracted badge logic
            val badgeResId = getIconMenuBadgeResId(context, menuItem)

            // If badgeResId is not null, display the badge icon
            badgeResId?.let {
                Icon(
                    modifier = Modifier.align(Alignment.TopEnd),
                    painter = painterResource(id = it),
                    contentDescription = "menuBadgeIcon",
                    tint = Color.Unspecified
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        val text = context.getString(menuItem.label)
        var fontSize by remember(text) { mutableStateOf(12.sp) }

        Text(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth(),
            text = text,
            maxLines = 2,
            lineHeight = 15.sp,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = if (isPreview) Color.Red else Color(context.getColor(R.color.text_default)),
            textAlign = TextAlign.Center,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.didOverflowHeight && fontSize == 12.sp) {
                    fontSize = 10.sp
                }
            }
        )
    }
}

@Preview
@Composable
fun PreviewMenuIconItem() {
    MenuIconItem(Modifier, IconMenu.SUPPORT, true, {})
}