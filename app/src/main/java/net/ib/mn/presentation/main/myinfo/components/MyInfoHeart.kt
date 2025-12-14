package net.ib.mn.presentation.main.myinfo.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.ui.theme.ExoTypo

/**
 * MyInfo 하트/다이아 정보 컴포넌트
 * fragment_my_heart_info.xml의 layout_currency 영역과 동일
 */
@Composable
fun MyInfoHeart(
    modifier: Modifier = Modifier,
    heartCount: String = "0",
    strongHeart: String = "0",
    weakHeart: String = "0",
    diaCount: String = "0",
    onInfoClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {}
) {
    // layout_currency
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false
            )
            .clip(RoundedCornerShape(12.dp))
            .background(colorResource(id = R.color.background_200))
            .padding(horizontal = 20.dp).padding(top=20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Heart Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // iv_heart (41dp)
                Image(
                    painter = painterResource(id = R.drawable.icon_my_own_heart),
                    contentDescription = "Heart Icon",
                    modifier = Modifier.size(41.dp)
                )

                Spacer(modifier = Modifier.width(11.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // tv_my_heart
                    Text(
                        text = stringResource(id = R.string.my_info_own_heart),
                        style = ExoTypo.typo14.copy(color = colorResource(id = R.color.text_gray)),
                        lineHeight = 14.sp
                    )

                    // tv_my_heart_count
                    Text(
                        text = heartCount,
                        style = ExoTypo.typo19Bold.copy(color = colorResource(id = R.color.text_default)),
                        lineHeight = 19.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // layout_heart: Strong Heart & Weak Heart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colorResource(id = R.color.gray50))
                    .padding(all = 19.dp)
            ) {
                Column {
                    // Strong Heart Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.ever_heart_short),
                            style = ExoTypo.typo14.copy(color = colorResource(id = R.color.text_default)),
                            lineHeight = 14.sp
                        )

                        Text(
                            text = strongHeart,
                            style = ExoTypo.typo14Bold.copy(color = colorResource(id = R.color.text_default)),
                            lineHeight = 14.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Weak Heart Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.daily_heart_short),
                            style = ExoTypo.typo14.copy(color = colorResource(id = R.color.text_default)),
                            lineHeight = 14.sp
                        )

                        Text(
                            text = weakHeart,
                            style = ExoTypo.typo14Bold.copy(color = colorResource(id = R.color.text_default)),
                            lineHeight = 14.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Diamond Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // iv_dia (41dp)
                Image(
                    painter = painterResource(id = R.drawable.icon_my_own_dia),
                    contentDescription = "Diamond Icon",
                    modifier = Modifier.size(41.dp)
                )

                Spacer(modifier = Modifier.width(11.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // tv_my_dia
                    Text(
                        text = stringResource(id = R.string.my_info_own_diamond),
                        style = ExoTypo.typo14.copy(color = colorResource(id = R.color.text_gray)),
                        lineHeight = 14.sp
                    )

                    // tv_my_dia_count
                    Text(
                        text = diaCount,
                        style = ExoTypo.typo19Bold.copy(color = colorResource(id = R.color.text_default)),
                        lineHeight = 19.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // v_divider
            HorizontalDivider(
                thickness = 0.3.dp,
                color = colorResource(id = R.color.gray110)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable { onHistoryClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.my_info_heart_dia_detail),
                    style = ExoTypo.typo14.copy(color = colorResource(id = R.color.text_gray))
                )
            }
        }

        // btn_currency_info: 정보 아이콘 (우측 상단)
        Image(
            painter = painterResource(id = R.drawable.btn_info_black),
            contentDescription = "Currency Info",
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.TopEnd)
                .offset(x = 14.dp, y = (-13).dp)
                .clickable { onInfoClick() }
        )
    }
}
