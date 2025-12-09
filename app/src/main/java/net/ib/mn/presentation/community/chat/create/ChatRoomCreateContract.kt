package net.ib.mn.presentation.community.chat.create

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.ChatRoomInfoModel

/**
 * ChatRoomCreateContract - 채팅방 생성 MVI Contract
 */
class ChatRoomCreateContract {

    data class State(
        val isLoading: Boolean = false,
        val idolId: Int = 0,

        // 입력 필드
        val title: String = "",
        val description: String = "",

        // 설정 옵션
        val isMostOnly: Boolean = true,        // 최애 전용 (Y) or 그룹/멤버 공개 (N)
        val isAnonymous: Boolean = false,      // 익명 (Y) or 닉네임 공개 (N)
        val levelLimit: Int = -1,              // -1: 제한 없음, 0~: 레벨 제한

        // 다이얼로그
        val showAnonymousSheet: Boolean = false,
        val showLevelLimitSheet: Boolean = false,
        val showConfirmDialog: Boolean = false,
        val showInstructionDialog: Boolean = false,  // 채팅방 개설 안내 팝업

        // 유저 정보
        val userLevel: Int = 0,
        val userMostType: String = "S",        // S: 솔로, G: 그룹
        val diamondCost: Int = 0,

        // 에러
        val error: String? = null
    ) : UiState {

        /**
         * 필수 항목 입력 완료 여부
         */
        val isFormValid: Boolean
            get() = title.trim().length >= MIN_TITLE_LENGTH && levelLimit >= 0

        /**
         * 제목 글자 수 텍스트
         */
        val titleLengthText: String
            get() = "${title.trim().length}/$MAX_TITLE_LENGTH"

        /**
         * 설명 글자 수 텍스트
         */
        val descriptionLengthText: String
            get() = "${description.length}/$MAX_DESCRIPTION_LENGTH"

        /**
         * 공개 설정 텍스트
         */
        val openStatusText: String
            get() = if (isMostOnly) "최애만 공개" else "그룹/멤버 공개"

        /**
         * 익명 설정 텍스트
         */
        val anonymousStatusText: String
            get() = if (isAnonymous) "익명" else "닉네임 공개"

        /**
         * 레벨 제한 텍스트
         */
        val levelLimitText: String
            get() = if (levelLimit < 0) "" else "Lv.$levelLimit 이상"

        companion object {
            const val MIN_TITLE_LENGTH = 5
            const val MAX_TITLE_LENGTH = 50
            const val MAX_DESCRIPTION_LENGTH = 100
            const val MAX_PEOPLE_COUNT = 300
        }
    }

    sealed class Intent : UiIntent {
        data class Initialize(val idolId: Int) : Intent()
        data class UpdateTitle(val title: String) : Intent()
        data class UpdateDescription(val description: String) : Intent()
        data class SetMostOnly(val isMostOnly: Boolean) : Intent()
        data class SetAnonymous(val isAnonymous: Boolean) : Intent()
        data class SetLevelLimit(val level: Int) : Intent()
        object ShowAnonymousSheet : Intent()
        object HideAnonymousSheet : Intent()
        object ShowLevelLimitSheet : Intent()
        object HideLevelLimitSheet : Intent()
        object ShowConfirmDialog : Intent()
        object HideConfirmDialog : Intent()
        object HideInstructionDialog : Intent()          // 안내 팝업 닫기
        object DontShowInstructionAgain : Intent()       // 다시 보지 않기
        object CreateRoom : Intent()
        object GoBack : Intent()
    }

    sealed class Effect : UiEffect {
        data class ShowError(val message: String) : Effect()
        data class RoomCreated(
            val roomId: Int,
            val nickname: String?,
            val userId: Int?,
            val isAnonymity: Boolean,
            val title: String
        ) : Effect()
        object NavigateBack : Effect()
        object ShowTitleTooShort : Effect()
        object ShowBadWordsInTitle : Effect()
        object ShowBadWordsInDescription : Effect()
        object ShowLevelTooHigh : Effect()
    }
}
