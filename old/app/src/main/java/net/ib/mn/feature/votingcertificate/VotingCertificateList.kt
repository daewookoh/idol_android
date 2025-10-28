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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.core.model.VoteCertificateModel

@Composable
fun VotingCertificateList(
    certificateList: List<VoteCertificateModel>,
    onClickItem: (voteCertificateModel: VoteCertificateModel) -> Unit = {}
) {
    val context = LocalContext.current
    var isExpand by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background_100))
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            if (isExpand) {
                VotingCertificateDescriptionExpand(
                    modifier = Modifier
                ) {
                    isExpand = false
                }
            } else {
                VotingCertificateDescriptionFold(
                    modifier = Modifier
                ) {
                    isExpand = true
                }
            }
            Spacer(
                modifier = Modifier
                    .height(3.dp)
            )
        }

        items(certificateList.size) { index ->
            VotingCertificateItem(
                context = context,
                certificate = certificateList[index],
                onClickItem = onClickItem
            )
            Spacer(
                modifier = Modifier
                    .height(6.dp)
            )
        }
    }
}

@Composable
@Preview
fun PreviewVotingCertificateList() {
    VotingCertificateList(listOf()) {}
}