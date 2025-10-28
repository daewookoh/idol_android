package net.ib.mn.feature.votingcertificate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ib.mn.R
import net.ib.mn.core.model.VoteCertificateModel
import net.ib.mn.viewmodel.VotingCertificateListViewModel

@Composable
fun VotingCertificateListScreen(
    viewModel: VotingCertificateListViewModel = hiltViewModel(),
    moveFavorite: () -> Unit,
    moveCertificate: (VoteCertificateModel) -> Unit,
    error: () -> Unit,
) {

    var isInit = remember { true }

    LaunchedEffect(isInit) {
        isInit = false
        viewModel.getVoteCertificate()
    }

    val voteCertificateListUiState = viewModel.voteCertificate.collectAsStateWithLifecycle()

    when(voteCertificateListUiState.value) {
        is VoteCertificateListUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center),
                    color = colorResource(id = R.color.main)
                )
            }
        }
        is VoteCertificateListUiState.Success -> {
            val certificateList = (voteCertificateListUiState.value as VoteCertificateListUiState.Success).list
            if (certificateList.isNotEmpty()) {
                VotingCertificateList(
                    certificateList = certificateList
                ) {
                    moveCertificate(it)
                }
            } else {
                VotingCertificateEmpty {
                    moveFavorite()
                }
            }
        }
        is VoteCertificateListUiState.Error -> {
            error()
        }
    }
}