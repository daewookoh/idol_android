package net.ib.mn.feature.votingcertificate

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.ib.mn.R

@Composable
fun VotingCertificateDescriptionFold(
    modifier: Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                color = colorResource(id = R.color.gray100),
                shape = RoundedCornerShape(16.dp)
            )
            .then(modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 15.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight(),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    fontSize = 15.sp,
                    color = colorResource(id = R.color.text_default),
                    text = stringResource(id = R.string.certificate_info_title),
                )
                IconButton(
                    modifier = Modifier.size(15.dp),
                    onClick = {
                        onClick()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(15.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.btn_arrow_down_gray),
                        contentDescription = "arrow",
                        tint = colorResource(id = R.color.text_default)
                    )
                }
            }
        }
    }
}

@Composable
fun VotingCertificateDescriptionExpand(
    modifier: Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = colorResource(id = R.color.gray100),
                shape = RoundedCornerShape(16.dp)
            )
            .then(modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 20.dp, end = 15.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    text = stringResource(id = R.string.certificate_info_title),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    fontSize = 15.sp,
                    color = colorResource(id = R.color.text_default)

                )
                IconButton(
                    modifier = Modifier.size(15.dp),
                    onClick = {
                        onClick()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(15.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.btn_arrow_up_gray),
                        contentDescription = "arrow",
                        tint = colorResource(id = R.color.text_default)
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .height(18.dp)
            )
            Text(
                modifier = Modifier
                    .wrapContentHeight(),
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                text = stringResource(id = R.string.certificate_info_desc),
                fontWeight = FontWeight.Normal,
                color = colorResource(id = R.color.text_gray),
                fontSize = 13.sp
            )
        }
    }
}

@Preview
@Composable
private fun PreviewVotingCertificateDescriptionFold(
) {
    VotingCertificateDescriptionFold(Modifier)
}

@Preview
@Composable
private fun PreviewVotingCertificateDescriptionExpand(
) {
    VotingCertificateDescriptionExpand(Modifier)
}