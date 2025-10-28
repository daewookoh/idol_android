package net.ib.mn.feature.menu.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.core.designsystem.util.noRippleClickable
import net.ib.mn.feature.menu.IconTextMenu
import net.ib.mn.feature.menu.TextMenu
import net.ib.mn.feature.menu.isShowTextMenuBadge
import net.ib.mn.viewmodel.MainViewModel

/**
 * 아이콘 + 텍스트로 구성된 메뉴 아이템 항목
 */

@Composable
fun MenuIconTextItem(
    context: Context,
    item: IconTextMenu,
    onClick: () -> Unit
) {
    val isPreview = LocalInspectionMode.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                color = Color(
                    ContextCompat.getColor(
                        context,
                        R.color.background_100
                    )
                )
            )
            .noRippleClickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                ) {
                    Icon(
                        modifier = Modifier
                            .size(30.dp),
                        painter = painterResource(id = item.icon),
                        contentDescription = "menuIcon",
                        tint = Color.Unspecified
                    )
                    if (isShowTextMenuBadge(context, item) || isPreview) {
                        Icon(
                            modifier = Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd),
                            painter = painterResource(id = R.drawable.icon_menu_new),
                            contentDescription = "menuBadgeIcon",
                            tint = Color.Unspecified,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    modifier = Modifier,
                    text = context.getString(item.title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(
                        ContextCompat.getColor(
                            context,
                            R.color.text_default
                        )
                    )
                )

            }
        }
//        Text(
//            modifier = Modifier
//                .weight(1f)
//                .padding(end = 24.dp),
//            text = context.getString(item.subTitle),
//            fontSize = 12.sp,
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis,
//            color = Color(
//                ContextCompat.getColor(
//                    context,
//                    R.color.text_gray
//                )
//            ),
//            textAlign = TextAlign.End
//        )
    }
}

@Preview
@Composable
fun PreviewMenuIconTextItem() {
    MenuIconTextItem(LocalContext.current, IconTextMenu.FACE, {})
}