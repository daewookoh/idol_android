package net.ib.mn.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.domain.usecase.GetVoteCertificateUseCase
import net.ib.mn.feature.votingcertificate.VoteCertificateListUiState
import javax.inject.Inject

@HiltViewModel
class VotingCertificateListViewModel @Inject constructor(
    private val getVoteCertificateUseCase: GetVoteCertificateUseCase
)  : BaseViewModel() {

    private val _voteCertificate: MutableStateFlow<VoteCertificateListUiState> = MutableStateFlow(VoteCertificateListUiState.Loading)
    val voteCertificate: StateFlow<VoteCertificateListUiState> get() = _voteCertificate

    fun getVoteCertificate() = viewModelScope.launch(Dispatchers.IO){
        try {
            val result = getVoteCertificateUseCase(null).first()
            _voteCertificate.emit(VoteCertificateListUiState.Success(result.data ?: listOf()))
        } catch (e: Exception) {
            _voteCertificate.emit(VoteCertificateListUiState.Error(e.message ?: "Unknown Error"))
        }
    }
}