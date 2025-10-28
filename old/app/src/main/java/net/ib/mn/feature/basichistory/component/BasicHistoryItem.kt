package net.ib.mn.feature.basichistory.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.feature.basichistory.HistoryCalcType
import net.ib.mn.model.HallModel
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.NetworkImage
import net.ib.mn.utils.UtilK
import java.text.DateFormat
import java.text.NumberFormat

@Composable
fun BasicHistoryItem(
    historyCalcType: HistoryCalcType,
    data: HallModel,
    date: DateFormat,
) {

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()
            .background(colorResource(id = R.color.background_100)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(46.dp)
                .fillMaxHeight()
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = String.format(
                    stringResource(id = R.string.rank_format),
                    (data.rank + 1).toString()
                ),
                color = colorResource(id = R.color.text_default),
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                fontSize = 11.sp
            )
        }

        val imgUrl = if (historyCalcType == HistoryCalcType.TICKET) {
            UtilK.trendImageUrl(context, data.getResourceId())
        } else {
            data.imageUrl
        }

        Surface(
            modifier = Modifier
                .size(41.dp),
            shape = CircleShape
        ) {
            NetworkImage(
                context = context,
                imageUrl = imgUrl
            )
        }
        Spacer(
            modifier = Modifier
                .width(10.dp)
        )
        Row {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                val name = UtilK.getName(data.getName(context) ?: data.idol?.getName() ?: "")
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = name.first,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.text_default),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                    Spacer(
                        modifier = Modifier
                            .width(5.dp)
                    )
                    Text(
                        text = name.second,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = colorResource(id = R.color.text_dimmed),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                }
                Spacer(
                    modifier = Modifier
                        .height(2.dp)
                )
                Row {
                    val text = if (historyCalcType == HistoryCalcType.TICKET) {
                        val voteCountText: String =
                            NumberFormat.getNumberInstance(getAppLocale(context))
                                .format(data.heart)
                        String.format(
                            stringResource(id = R.string.vote_count_format), voteCountText
                        )
                    } else {
                        val voteCountText: String =
                            NumberFormat.getNumberInstance(getAppLocale(context))
                                .format(data.count)
                        String.format(
                            stringResource(id = R.string.number_of_times), voteCountText
                        )
                    }

                    Text(
                        modifier = Modifier
                            .weight(1f),
                        text = text,
                        fontSize = 11.sp,
                        color = colorResource(id = R.color.text_gray),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                    data.createdAt?.let {
                        Text(
                            text = date.format(data.createdAt),
                            fontSize = 11.sp,
                            color = colorResource(id = R.color.text_gray),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )
                    }
                    Spacer(
                        modifier = Modifier
                            .width(15.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewBasicHistoryItem() {
    BasicHistoryItem(
        HistoryCalcType.COUNT,
        HallModel(),
        DateFormat.getDateInstance(DateFormat.MEDIUM),
    )
}