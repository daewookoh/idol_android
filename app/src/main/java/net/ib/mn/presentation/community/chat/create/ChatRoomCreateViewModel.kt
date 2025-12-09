package net.ib.mn.presentation.community.chat.create

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ChatRoomCreateRequest
import net.ib.mn.domain.repository.ChatRepository
import net.ib.mn.util.LocaleUtil
import javax.inject.Inject

@HiltViewModel
class ChatRoomCreateViewModel @Inject constructor(
    private val application: Application,
    private val chatRepository: ChatRepository,
    private val preferencesManager: PreferencesManager,
    private val userCacheRepository: UserCacheRepository
) : BaseViewModel<ChatRoomCreateContract.State, ChatRoomCreateContract.Intent, ChatRoomCreateContract.Effect>() {

    private val locale = LocaleUtil.getSystemLanguage(application).split("_")[0]

    override fun createInitialState() = ChatRoomCreateContract.State()

    override fun handleIntent(intent: ChatRoomCreateContract.Intent) {
        when (intent) {
            is ChatRoomCreateContract.Intent.Initialize -> initialize(intent.idolId)
            is ChatRoomCreateContract.Intent.UpdateTitle -> updateTitle(intent.title)
            is ChatRoomCreateContract.Intent.UpdateDescription -> updateDescription(intent.description)
            is ChatRoomCreateContract.Intent.SetMostOnly -> setMostOnly(intent.isMostOnly)
            is ChatRoomCreateContract.Intent.SetAnonymous -> setAnonymous(intent.isAnonymous)
            is ChatRoomCreateContract.Intent.SetLevelLimit -> setLevelLimit(intent.level)
            is ChatRoomCreateContract.Intent.ShowAnonymousSheet -> setState { copy(showAnonymousSheet = true) }
            is ChatRoomCreateContract.Intent.HideAnonymousSheet -> setState { copy(showAnonymousSheet = false) }
            is ChatRoomCreateContract.Intent.ShowLevelLimitSheet -> setState { copy(showLevelLimitSheet = true) }
            is ChatRoomCreateContract.Intent.HideLevelLimitSheet -> setState { copy(showLevelLimitSheet = false) }
            is ChatRoomCreateContract.Intent.ShowConfirmDialog -> showConfirmDialog()
            is ChatRoomCreateContract.Intent.HideConfirmDialog -> setState { copy(showConfirmDialog = false) }
            is ChatRoomCreateContract.Intent.HideInstructionDialog -> setState { copy(showInstructionDialog = false) }
            is ChatRoomCreateContract.Intent.DontShowInstructionAgain -> dontShowInstructionAgain()
            is ChatRoomCreateContract.Intent.CreateRoom -> createRoom()
            is ChatRoomCreateContract.Intent.GoBack -> setEffect { ChatRoomCreateContract.Effect.NavigateBack }
        }
    }

    private fun initialize(idolId: Int) {
        viewModelScope.launch {
            // UserCacheRepository에서 먼저 시도, 없으면 PreferencesManager에서 가져오기
            val cacheUserData = userCacheRepository.getUserData()
            val prefUserData = preferencesManager.getUserSelfData()
            val userData = cacheUserData ?: prefUserData

            // 개별 필드로 저장된 레벨 (setUserInfo에서 저장된 KEY_USER_LEVEL)
            val prefUserLevel = preferencesManager.getUserLevel()

            // 우선순위: cacheUserData.level > prefUserData.level > prefUserLevel > 0
            val userLevel = cacheUserData?.level
                ?: prefUserData?.level
                ?: prefUserLevel.takeIf { it > 0 }
                ?: 0
            val userMostType = userData?.most?.type ?: preferencesManager.getMostIdolCategory() ?: "S"
            val shouldShowInstruction = preferencesManager.shouldShowChatRoomCreateInstruction()
            val diamondCost = preferencesManager.chatRoomDiamond.first()

            // 유저 최애 타입에 따라 공개 설정 결정
            // S: 솔로 아이돌 -> 최애만 공개
            // G: 그룹 아이돌 -> 그룹/멤버 공개 가능
            val isMostOnly = userMostType == "S"

            setState {
                copy(
                    idolId = idolId,
                    userLevel = userLevel,
                    userMostType = userMostType,
                    isMostOnly = isMostOnly,
                    diamondCost = diamondCost,
                    showInstructionDialog = shouldShowInstruction
                )
            }
        }
    }

    private fun dontShowInstructionAgain() {
        viewModelScope.launch {
            preferencesManager.setShowChatRoomCreateInstruction(false)
            setState { copy(showInstructionDialog = false) }
        }
    }

    private fun updateTitle(title: String) {
        if (title.length <= ChatRoomCreateContract.State.MAX_TITLE_LENGTH) {
            setState { copy(title = title) }
        }
    }

    private fun updateDescription(description: String) {
        if (description.length <= ChatRoomCreateContract.State.MAX_DESCRIPTION_LENGTH) {
            setState { copy(description = description) }
        }
    }

    private fun setMostOnly(isMostOnly: Boolean) {
        setState { copy(isMostOnly = isMostOnly) }
    }

    private fun setAnonymous(isAnonymous: Boolean) {
        setState { copy(isAnonymous = isAnonymous, showAnonymousSheet = false) }
    }

    private fun setLevelLimit(level: Int) {
        val userLevel = uiState.value.userLevel
        if (level <= userLevel) {
            setState { copy(levelLimit = level, showLevelLimitSheet = false) }
        } else {
            setEffect { ChatRoomCreateContract.Effect.ShowLevelTooHigh }
        }
    }

    private fun showConfirmDialog() {
        val state = uiState.value
        val title = state.title.trim()

        when {
            title.length < ChatRoomCreateContract.State.MIN_TITLE_LENGTH -> {
                setEffect { ChatRoomCreateContract.Effect.ShowTitleTooShort }
            }
            state.levelLimit < 0 -> {
                setEffect { ChatRoomCreateContract.Effect.ShowError("레벨 제한을 선택해주세요.") }
            }
            !state.isFormValid -> {
                setEffect { ChatRoomCreateContract.Effect.ShowError("필수 항목을 모두 입력해주세요.") }
            }
            else -> {
                setState { copy(showConfirmDialog = true) }
            }
        }
    }

    private fun createRoom() {
        val state = uiState.value

        viewModelScope.launch {
            setState { copy(isLoading = true, showConfirmDialog = false) }

            val request = ChatRoomCreateRequest(
                idolId = state.idolId,
                title = state.title.trim(),
                desc = state.description.takeIf { it.isNotBlank() },
                locale = locale,
                maxPeople = ChatRoomCreateContract.State.MAX_PEOPLE_COUNT,
                levelLimit = state.levelLimit,
                isAnonymity = if (state.isAnonymous) "Y" else "N",
                isMostOnly = if (state.isMostOnly) "Y" else "N"
            )

            chatRepository.createChatRoom(request).collect { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> {
                        setState { copy(isLoading = false) }
                        val roomInfo = result.data

                        if (roomInfo.success == true) {
                            setEffect {
                                ChatRoomCreateContract.Effect.RoomCreated(
                                    roomId = roomInfo.roomId,
                                    nickname = null, // API 응답에서 nickname 가져오기
                                    userId = roomInfo.userId,
                                    isAnonymity = roomInfo.isAnonymousRoom,
                                    title = state.title.trim()
                                )
                            }
                        } else {
                            setEffect {
                                ChatRoomCreateContract.Effect.ShowError(
                                    "채팅방 생성에 실패했습니다."
                                )
                            }
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false) }
                        setEffect {
                            ChatRoomCreateContract.Effect.ShowError(
                                result.message ?: "채팅방 생성에 실패했습니다."
                            )
                        }
                    }
                }
            }
        }
    }
}
