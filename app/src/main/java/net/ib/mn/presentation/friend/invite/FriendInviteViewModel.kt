package net.ib.mn.presentation.friend.invite

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import javax.inject.Inject

/**
 * FriendInviteViewModel - 친구 초대 화면 ViewModel
 *
 * old 프로젝트의 FriendInviteViewModel 로직을 유지
 * - 초대 메시지 생성
 */
@HiltViewModel
class FriendInviteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val idolRepository: IdolRepository,
    private val userCacheRepository: UserCacheRepository
) : ViewModel() {

    companion object {
        private const val TAG = "FriendInviteViewModel"
    }

    private val _inviteMsg = MutableStateFlow<String?>(null)
    val inviteMsg: StateFlow<String?> = _inviteMsg.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

    /**
     * 초대 메시지 가져오기
     * old 프로젝트의 getInviteMsg()와 동일
     */
    fun getInviteMsg() {
        viewModelScope.launch {
            try {
                val msg = getFriendInviteMsg()
                _inviteMsg.value = msg
            } catch (e: Exception) {
                logE(TAG, "Failed to get invite message: ${e.message}")
                _errorMsg.value = context.getString(R.string.error_abnormal_default)
            }
        }
    }

    /**
     * 초대 메시지 소비 (일회성 이벤트 처리)
     */
    fun consumeInviteMsg() {
        _inviteMsg.value = null
    }

    /**
     * 에러 메시지 소비 (일회성 이벤트 처리)
     */
    fun consumeErrorMsg() {
        _errorMsg.value = null
    }

    /**
     * 친구 초대 메시지 생성
     * old 프로젝트의 UtilK.getFriendInviteMsg()와 동일한 방식
     */
    private suspend fun getFriendInviteMsg(): String {
        // 최애 아이돌 가져오기
        val favoriteIdolName = try {
            val favoriteIdolIds = userCacheRepository.favoriteIdolIds.first()
            if (favoriteIdolIds.isNotEmpty()) {
                val idol = idolRepository.getIdolById(favoriteIdolIds.first())
                idol?.let { LocaleUtil.getLocalizedIdolName(context, it) }
            } else {
                null
            }
        } catch (e: Exception) {
            logE(TAG, "Failed to get favorite idol: ${e.message}")
            null
        }

        // 메시지 생성
        return if (favoriteIdolName != null) {
            context.getString(R.string.friend_invite_msg, favoriteIdolName)
        } else {
            context.getString(R.string.friend_invite_msg_default)
        }
    }
}
