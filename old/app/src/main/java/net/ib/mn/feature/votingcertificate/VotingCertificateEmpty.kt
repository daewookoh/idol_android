package net.ib.mn.feature.votingcertificate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R

@Composable
fun VotingCertificateEmpty(
    onClickVote: () -> Unit = {}
) {

    var isExpand by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background_100))
            .padding(horizontal = 20.dp)
    ) {
        if (isExpand) {
            VotingCertificateDescriptionExpand(
                modifier = Modifier
                    .align(Alignment.TopCenter)
            ) {
                isExpand = false
            }
        } else {
            VotingCertificateDescriptionFold(
                modifier = Modifier
                    .align(Alignment.TopCenter)
            ) {
                isExpand = true
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.certificate_none),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = colorResource(id = R.color.text_dimmed)
            )
            Spacer(
                modifier = Modifier
                    .height(22.dp)
            )
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 36.dp)
                .background(
                    color = colorResource(id = R.color.main_light),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable {
                    onClickVote()
                }
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center),
                    text = stringResource(id = R.string.certificate_none_button),
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.text_white_black)
                )
            }
        }
    }
}

@Composable
@Preview
fun PreviewVotingCertificateEmpty() {
    VotingCertificateEmpty()
}