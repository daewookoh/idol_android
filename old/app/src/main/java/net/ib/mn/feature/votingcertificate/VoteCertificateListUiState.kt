package net.ib.mn.feature.votingcertificate

import androidx.compose.runtime.Stable
import net.ib.mn.core.model.VoteCertificateModel

@Stable
sealed interface VoteCertificateListUiState {
    object Loading: VoteCertificateListUiState
    data class Success(val list: List<VoteCertificateModel>): VoteCertificateListUiState
    data class Error(val message: String): VoteCertificateListUiState
}