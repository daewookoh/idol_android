package net.ib.mn.presentation.friend

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.repository.FriendsRepository
import net.ib.mn.data.repository.UsersRepository
import net.ib.mn.data.repository.WebTokenResult
import net.ib.mn.domain.model.FriendModel
import net.ib.mn.util.LocaleUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Friend 화면 ViewModel
 *
 * old 프로젝트의 FriendsActivity + FriendsViewModel 로직을 MVI 패턴으로 구현.
 * - 친구 목록 조회
 * - 친구 요청자 목록 조회
 * - 하트 보내기/받기
 * - 친구 요청 수락/거절
 *
 * Navigation 3 활용:
 * - 네비게이션은 Screen에서 LocalAppNavigator로 직접 처리
 * - ViewModel은 비즈니스 로직(상태 관리, API 호출)에만 집중
 */
@HiltViewModel
class FriendViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val usersRepository: UsersRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : BaseViewModel<FriendContract.State, FriendContract.Intent, FriendContract.Effect>() {

    companion object {
        private const val HEART_PREF_NAME = "heart"
        private const val HEART_COOLDOWN_MS = 10 * 60 * 1000L  // 10분
    }

    private val heartPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(HEART_PREF_NAME, Context.MODE_PRIVATE)
    }

    override fun createInitialState(): FriendContract.State = FriendContract.State()

    init {
        loadFriends()
    }

    override fun handleIntent(intent: FriendContract.Intent) {
        when (intent) {
            is FriendContract.Intent.LoadFriends -> loadFriends()
            is FriendContract.Intent.Refresh -> refresh()
            is FriendContract.Intent.SendHeart -> sendHeart(intent.friendId, intent.nickname)
            is FriendContract.Intent.SendHeartToAll -> sendHeartToAll()
            is FriendContract.Intent.ReceiveHeart -> receiveHeart()
            is FriendContract.Intent.AcceptFriendRequest -> acceptFriendRequest(intent.userId)
            is FriendContract.Intent.DeclineFriendRequest -> declineFriendRequest(intent.userId)
            is FriendContract.Intent.AcceptAllFriendRequests -> acceptAllFriendRequests()
            is FriendContract.Intent.Invite -> invite()
        }
    }

    /**
     * 친구 목록 로드
     */
    private fun loadFriends() {
        if (currentState.isLoading) return

        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val response = friendsRepository.getFriendsSelf()
            if (response.success) {
                setState {
                    copy(
                        isLoading = false,
                        friends = response.friends,
                        requesters = response.requesters,
                        error = null
                    )
                }
            } else {
                setState {
                    copy(
                        isLoading = false,
                        error = response.errorMessage ?: context.getString(R.string.error_abnormal_exception)
                    )
                }
            }
        }
    }

    /**
     * 새로고침
     */
    private fun refresh() {
        setState { copy(isRefreshing = true) }

        viewModelScope.launch {
            val response = friendsRepository.getFriendsSelf()
            if (response.success) {
                setState {
                    copy(
                        isRefreshing = false,
                        friends = response.friends,
                        requesters = response.requesters,
                        error = null
                    )
                }
            } else {
                setState {
                    copy(
                        isRefreshing = false,
                        error = response.errorMessage
                    )
                }
                setEffect { FriendContract.Effect.ShowToast(response.errorMessage ?: context.getString(R.string.error_abnormal_exception)) }
            }
        }
    }

    /**
     * 친구에게 하트 보내기
     */
    private fun sendHeart(friendId: Int, nickname: String) {
        // 쿨타임 체크
        val lastSentTime = heartPrefs.getLong("send_heart_$friendId", -1)
        if (lastSentTime > 0) {
            val expire = lastSentTime + HEART_COOLDOWN_MS
            val currentTime = System.currentTimeMillis()
            if (currentTime < expire) {
                val timeFormat = SimpleDateFormat("m:ss", Locale.getDefault())
                val remainingTime = timeFormat.format(Date(expire - currentTime))
                val message = context.getString(R.string.already_sent_heart__format, nickname, remainingTime)
                setEffect { FriendContract.Effect.ShowToast(message) }
                return
            }
        }

        // 이미 보내는 중인지 체크
        if (currentState.sendingHeartIds.contains(friendId)) {
            setEffect { FriendContract.Effect.ShowToast(context.getString(R.string.sending_heart)) }
            return
        }

        setState { copy(sendingHeartIds = sendingHeartIds + friendId) }

        viewModelScope.launch {
            val response = friendsRepository.giveHeart(friendId)
            setState { copy(sendingHeartIds = sendingHeartIds - friendId) }

            if (response.success) {
                heartPrefs.edit().putLong("send_heart_$friendId", System.currentTimeMillis()).apply()
                val message = context.getString(R.string.sent_heart_friend__format, nickname)
                setEffect { FriendContract.Effect.ShowToast(message) }
            } else {
                setEffect { FriendContract.Effect.ShowDialog(null, response.message ?: context.getString(R.string.error_abnormal_exception)) }
            }
        }
    }

    /**
     * 모든 친구에게 하트 보내기
     */
    private fun sendHeartToAll() {
        if (currentState.friends.isEmpty()) return

        // 전체 쿨타임 체크
        val lastSentAllTime = heartPrefs.getLong("send_heart_all", -1)
        if (lastSentAllTime > 0) {
            val expire = lastSentAllTime + HEART_COOLDOWN_MS
            val currentTime = System.currentTimeMillis()
            if (currentTime < expire) {
                return
            }
        }

        // 쿨타임 지난 친구들만 필터링
        val filteredFriends = currentState.friends.filter { friend ->
            val lastSentTime = heartPrefs.getLong("send_heart_${friend.user.id}", -1)
            if (lastSentTime > 0) {
                val expire = lastSentTime + HEART_COOLDOWN_MS
                val currentTime = System.currentTimeMillis()
                currentTime >= expire && !currentState.sendingHeartIds.contains(friend.user.id)
            } else {
                !currentState.sendingHeartIds.contains(friend.user.id)
            }
        }

        if (filteredFriends.isEmpty()) return

        val sendingIds = filteredFriends.map { it.user.id }.toSet()
        setState { copy(sendingHeartIds = sendingHeartIds + sendingIds) }

        viewModelScope.launch {
            val response = friendsRepository.giveAllHeart()
            setState { copy(sendingHeartIds = sendingHeartIds - sendingIds) }

            if (response.success) {
                val currentTime = System.currentTimeMillis()
                val editor = heartPrefs.edit()
                editor.putLong("send_heart_all", currentTime)
                filteredFriends.forEach { friend ->
                    editor.putLong("send_heart_${friend.user.id}", currentTime)
                }
                editor.apply()

                val count = if (response.count > 0) response.count else filteredFriends.size
                val message = context.getString(R.string.sent_heart_all_friend__format, count)
                setEffect { FriendContract.Effect.ShowDialog(null, message) }
            } else {
                setEffect { FriendContract.Effect.ShowDialog(null, response.message ?: context.getString(R.string.error_abnormal_exception)) }
            }
        }
    }

    /**
     * 하트 받기
     */
    private fun receiveHeart() {
        viewModelScope.launch {
            val response = friendsRepository.receiveFriendHeart()

            if (response.success) {
                val message = when {
                    response.heart == 0 -> context.getString(R.string.label_friend_heart_empty)
                    response.heart == 1 -> context.getString(R.string.label_friend_heart_one)
                    else -> context.getString(R.string.label_friend_heart_format, response.heart)
                }
                setEffect { FriendContract.Effect.ShowDialog(null, message) }
            } else {
                if (response.gcode == 88888) {
                    setEffect { FriendContract.Effect.ShowDialog(null, response.message ?: "") }
                } else {
                    setEffect { FriendContract.Effect.ShowToast(context.getString(R.string.error_abnormal_exception)) }
                }
            }
        }
    }

    /**
     * 친구 요청 수락
     */
    private fun acceptFriendRequest(userId: Int) {
        viewModelScope.launch {
            val response = friendsRepository.respondFriendRequest(userId, true)

            if (response.success) {
                setEffect { FriendContract.Effect.ShowDialog(null, context.getString(R.string.desc_accepted_friend_request)) }
                loadFriends()  // 목록 새로고침
            } else {
                setEffect { FriendContract.Effect.ShowDialog(null, response.message ?: context.getString(R.string.msg_error_ok)) }
            }
        }
    }

    /**
     * 친구 요청 거절
     */
    private fun declineFriendRequest(userId: Int) {
        viewModelScope.launch {
            val response = friendsRepository.respondFriendRequest(userId, false)

            if (response.success) {
                setEffect { FriendContract.Effect.ShowDialog(null, context.getString(R.string.desc_declined_friend_request)) }
                loadFriends()  // 목록 새로고침
            } else {
                setEffect { FriendContract.Effect.ShowDialog(null, response.message ?: context.getString(R.string.msg_error_ok)) }
            }
        }
    }

    /**
     * 모든 친구 요청 수락
     */
    private fun acceptAllFriendRequests() {
        viewModelScope.launch {
            val response = friendsRepository.respondAllFriendRequest()

            if (response.success) {
                setEffect { FriendContract.Effect.ShowDialog(null, context.getString(R.string.desc_accepted_friend_request)) }
                loadFriends()  // 목록 새로고침
            } else {
                setEffect { FriendContract.Effect.ShowDialog(null, response.message ?: context.getString(R.string.msg_error_ok)) }
            }
        }
    }

    /**
     * 친구 초대
     * old 프로젝트: FriendsViewModel.invite()
     * - 서버에서 웹 토큰을 별도로 가져옴 (accessToken이 아닌 web_token)
     * - 저장된 언어 설정을 우선 사용
     */
    private fun invite() {
        viewModelScope.launch {
            // 웹 토큰 가져오기 (old: getWebTokenSuspend())
            when (val tokenResult = usersRepository.getWebToken()) {
                is WebTokenResult.Success -> {
                    // 저장된 언어 설정 사용 (old: languagePreferenceRepository.getSystemLanguage())
                    val savedLanguage = preferencesManager.language.first()
                    val language = LocaleUtil.getWebViewLocale(context, savedLanguage)
                    setEffect { FriendContract.Effect.NavigateToInvite(language, tokenResult.token) }
                }
                is WebTokenResult.ApiError -> {
                    setEffect { FriendContract.Effect.ShowToast(tokenResult.message ?: context.getString(R.string.error_abnormal_exception)) }
                }
                is WebTokenResult.NetworkError -> {
                    setEffect { FriendContract.Effect.ShowToast(context.getString(R.string.error_abnormal_exception)) }
                }
            }
        }
    }
}
