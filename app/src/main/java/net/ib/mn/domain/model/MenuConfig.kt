package net.ib.mn.domain.model

/**
 * 메뉴 설정 통합 모델
 * ConfigSelfResponse, SharedAppState 등의 설정을 통합하여 관리
 */
data class MenuConfig(
    // ConfigSelfResponse에서 가져오는 값들
    val menuNoticeMain: String = "N",  // "Y" = 아이콘 메뉴, "N" = 텍스트 메뉴
    val menuStoreMain: String = "N",
    val menuFreeBoardMain: String = "N",
    val showStoreEventMarker: String = "N",  // "Y" = 뱃지 표시
    val showFreeChargeMarker: String = "N",
    val showLiveStreamingTab: Boolean = false,  // 자유게시판 표시 여부

    // SharedAppState에서 가져오는 값들 (뱃지 상태)
    val hasUnreadNotice: Boolean = false,
    val hasUnreadEvent: Boolean = false,
    val isAttendanceAvailable: Boolean = false,

    // RemoteConfig에서 가져오는 값들 (선택사항, 나중에 추가)
    val showGameMenu: Boolean = false
) {
    companion object {
        /**
         * 기본값 생성
         */
        fun default() = MenuConfig()
    }
}
