package net.ib.mn.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.domain.usecase.GetVoteCertificateUseCase
import net.ib.mn.core.model.VoteCertificateModel
import net.ib.mn.feature.votingcertificate.VoteCertificateListUiState
import net.ib.mn.utils.livedata.Event
import javax.inject.Inject

@HiltViewModel
class VotingCertificateViewModel @Inject constructor(
    private val getVoteCertificateUseCase: GetVoteCertificateUseCase
)  : BaseViewModel() {

    private val _voteCertificate = MutableLiveData<Event<VoteCertificateModel>>()
    val voteCertificate: LiveData<Event<VoteCertificateModel>> get() = _voteCertificate

    fun getVoteCertificate(idolId: Long) = viewModelScope.launch(Dispatchers.IO){
        try {
            val result = getVoteCertificateUseCase(idolId).first().data
            result?.let {
                _voteCertificate.postValue(Event(it.first()))
            }

        } catch (e: Exception) {
            _errorToast.postValue(Event(e.message ?: "Unknown Error"))
        }
    }
}