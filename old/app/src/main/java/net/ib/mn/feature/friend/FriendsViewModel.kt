package net.ib.mn.feature.friend

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.language.LanguagePreferenceRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.datastore.IsShowFriendInviteBannerTooltipUseCase
import net.ib.mn.domain.usecase.datastore.SetFriendInviteBannerTooltipUseCase
import net.ib.mn.utils.livedata.Event
import org.json.JSONObject
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val languagePreferenceRepository: LanguagePreferenceRepository,
    private val usersRepository: UsersRepository,
    private val isShowFriendInviteBannerTooltipUseCase: IsShowFriendInviteBannerTooltipUseCase,
    private val setFriendInviteBannerTooltipUseCase: SetFriendInviteBannerTooltipUseCase
) : BaseViewModel() {

    private val _inviteData = MutableLiveData<Event<InvitePayload>>()
    val inviteData: LiveData<Event<InvitePayload>> = _inviteData
    private val _isShowBannerTooltip = MutableLiveData<Event<Boolean>>()
    val showBannerTooltip: LiveData<Event<Boolean>> = _isShowBannerTooltip

    init {
        isShowBannerTooltip()
    }

    fun invite() = viewModelScope.launch {
        val languageDeferred = async { languagePreferenceRepository.getSystemLanguage() }
        val tokenDeferred = async { getWebTokenSuspend() }

        val language = languageDeferred.await()
        when (val tokenResult = tokenDeferred.await()) {
            is TokenResult.Success -> {
                val token = tokenResult.token
                _inviteData.value = Event(InvitePayload(language, token))
            }

            is TokenResult.ApiError -> {
                Log.e("@@@@", "${tokenResult.response}")
                _errorToastWithJson.postValue(Event(tokenResult.response))
            }

            is TokenResult.NetworkError -> {
                Log.e("@@@@", tokenResult.throwable.message ?: "Unknown error")
                _errorToast.postValue(Event(tokenResult.throwable.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun getWebTokenSuspend(): TokenResult =
        suspendCancellableCoroutine { continuation ->
            val job =
                CoroutineScope(Dispatchers.Default).launch { // Using Dispatchers.Default for background work
                    try {
                        usersRepository.getWebToken(
                            listener = { response ->
                                if (continuation.isActive) {
                                    val success = response.optBoolean("success", false)
                                    val token =
                                        response.optString("token").takeIf { it.isNotBlank() }
                                    if (success && token != null) {
                                        continuation.resume(TokenResult.Success(token))
                                    } else {
                                        continuation.resume(TokenResult.ApiError(response))
                                    }
                                }
                            },
                            errorListener = { throwable ->
                                if (continuation.isActive) {
                                    continuation.resume(TokenResult.NetworkError(throwable))
                                }
                            }
                        )
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(TokenResult.NetworkError(e))
                        }
                    }
                }

            continuation.invokeOnCancellation {
                job.cancel()
            }
        }

    private fun isShowBannerTooltip() = viewModelScope.launch {
        val isShow: Boolean = isShowFriendInviteBannerTooltipUseCase()
            .mapDataResource { it }
            .awaitOrThrow() ?: true

        _isShowBannerTooltip.postValue(Event(isShow))
    }

    fun updateBannerTooltipState() = viewModelScope.launch {
        setFriendInviteBannerTooltipUseCase()
            .mapDataResource { it }
            .awaitOrThrow()
    }
}

private sealed class TokenResult {
    data class Success(val token: String) : TokenResult()
    data class ApiError(val response: JSONObject) : TokenResult()
    data class NetworkError(val throwable: Throwable) : TokenResult()
}