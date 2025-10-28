package net.ib.mn.feature.votingcertificate

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.core.model.VoteCertificateModel
import net.ib.mn.utils.NetworkImage
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.getNameFromIdolLiteModel

@Composable
fun VotingCertificateItem(
    context: Context,
    certificate: VoteCertificateModel,
    onClickItem: (voteCertificateModel: VoteCertificateModel) -> Unit = {}
) {
    val medal = enumValues<VotingCertificateGrade>()
        .firstOrNull { it.grade == certificate.grade }?.medal
        ?: R.drawable.filled

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                color = colorResource(id = R.color.background_100)
            )
            .clickable {
                onClickItem(certificate)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        NetworkImage(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape),
            context = context,
            imageUrl = certificate.idol.imageUrl
        )
        Spacer(modifier = Modifier
            .width(10.dp)
        )
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                val name = UtilK.getName(getNameFromIdolLiteModel(context, certificate.idol))
                Text(
                    modifier = Modifier,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    text = name.first,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.text_default)
                )
                Spacer(
                    modifier = Modifier
                        .width(6.dp)
                )
                Text(
                    modifier = Modifier,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    overflow = TextOverflow.Ellipsis,
                    text = name.second,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fontSize = 10.sp,
                    color = colorResource(id = R.color.text_dimmed)
                )
            }
            Spacer(
                modifier = Modifier
                    .height(3.dp)
            )
            Row {
                Text(
                    modifier = Modifier,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    text = certificate.vote.toString(),
                    maxLines = 1,
                    fontSize = 11.sp,
                    color = colorResource(id = R.color.text_gray)
                )
                Spacer(
                    modifier = Modifier
                        .width(2.dp)
                )
                Icon(
                    modifier = Modifier.size(12.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.filled),
                    contentDescription = "heart",
                    tint = Color.Unspecified
                )

            }
        }
        Spacer(modifier = Modifier
            .width(10.dp)
        )
        Icon(
            modifier = Modifier.size(64.dp),
            imageVector = ImageVector.vectorResource(medal),
            contentDescription = "medal",
            tint = Color.Unspecified
        )
    }
}

@Preview
@Composable
fun PreviewVotingCertificateItem() {
    VotingCertificateItem(
        LocalContext.current,
        VoteCertificateModel()
    )
}