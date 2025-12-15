package net.ib.mn.presentation.mostset

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.data.remote.dto.FavoriteIdolDto

/**
 * MostSet 화면의 MVI Contract.
 *
 * old 프로젝트의 FavoriteSettingActivity 로직을 MVI 패턴으로 구현.
 * - 현재 최애 아이돌 표시
 * - 아이돌 검색
 * - 즐겨찾기 추가/삭제
 * - 최애 설정/해제
 */
class MostSetContract {

    /**
     * 최애 설정 화면의 아이돌 아이템
     *
     * @property id 아이돌 ID
     * @property name 아이돌 이름
     * @property nameEn 영어 이름
     * @property nameJp 일본어 이름
     * @property nameZh 중국어 간체 이름
     * @property nameZhTw 중국어 번체 이름
     * @property groupId 그룹 ID
     * @property groupName 그룹 이름
     * @property imageUrl 이미지 URL
     * @property imageUrl2 이미지 URL 2
     * @property heart 투표 수
     * @property type 타입 (S: 솔로, G: 그룹)
     * @property category 카테고리 (M: 남, F: 여)
     * @property resourceUri 리소스 URI
     * @property isFavorite 즐겨찾기 여부
     * @property isMost 최애 여부
     * @property favoriteId 즐겨찾기 ID (삭제 시 필요)
     * @property isViewable 노출 여부
     * @property anniversary 기념일 타입
     * @property anniversaryDays 기념일 일수
     * @property miracleCount 기적 카운트
     * @property fairyCount 요정 카운트
     * @property angelCount 천사 카운트
     * @property isRookie 루키 여부
     * @property isSuperRookie 슈퍼루키 여부
     * @property chartCodes 차트 코드 리스트
     */
    data class IdolItem(
        val id: Int,
        val name: String = "",
        val nameEn: String = "",
        val nameJp: String = "",
        val nameZh: String = "",
        val nameZhTw: String = "",
        val groupId: Int = 0,
        val groupName: String? = null,
        val imageUrl: String? = null,
        val imageUrl2: String? = null,
        val heart: Long = 0,
        val type: String = "",
        val category: String = "",
        val resourceUri: String = "",
        val isFavorite: Boolean = false,
        val isMost: Boolean = false,
        val favoriteId: Int? = null,
        val isViewable: String = "Y",
        val anniversary: String? = null,
        val anniversaryDays: Int = 0,
        val miracleCount: Int = 0,
        val fairyCount: Int = 0,
        val angelCount: Int = 0,
        val isRookie: Boolean = false,
        val isSuperRookie: Boolean = false,
        val chartCodes: List<String>? = null
    ) {
        companion object {
            fun fromFavoriteIdolDto(dto: FavoriteIdolDto, favoriteId: Int, isMost: Boolean = false): IdolItem {
                return IdolItem(
                    id = dto.id,
                    name = dto.name ?: "",
                    nameEn = dto.nameEn ?: "",
                    groupId = dto.groupId ?: 0,
                    groupName = dto.groupName,
                    imageUrl = dto.imageUrl,
                    imageUrl2 = dto.imageUrl2,
                    heart = dto.heart ?: 0,
                    type = dto.type ?: "",
                    category = dto.category ?: "",
                    isFavorite = true,
                    isMost = isMost,
                    favoriteId = favoriteId,
                    isViewable = "Y",
                    chartCodes = dto.chartCodes
                )
            }
        }
    }

    /**
     * UI State for MostSet screen.
     *
     * @property searchQuery 현재 검색어
     * @property mostIdolName 현재 최애 아이돌 이름
     * @property mostIdolImageUrl 현재 최애 아이돌 이미지 URL
     * @property mostIdolId 현재 최애 아이돌 ID
     * @property searchResults 검색 결과 리스트
     * @property isLoading 로딩 중 여부
     * @property isSearchMode 검색 모드 여부
     * @property cdnUrl CDN URL
     */
    data class State(
        val searchQuery: String = "",
        val mostIdolName: String? = null,
        val mostIdolImageUrl: String? = null,
        val mostIdolId: Int? = null,
        val searchResults: List<IdolItem> = emptyList(),
        val isLoading: Boolean = false,
        val isSearchMode: Boolean = false,
        val cdnUrl: String = ""
    ) : UiState

    /**
     * User intents.
     */
    sealed class Intent : UiIntent {
        /**
         * 검색어 입력 변경
         */
        data class UpdateSearchQuery(val query: String) : Intent()

        /**
         * 검색 실행
         */
        data object DoSearch : Intent()

        /**
         * 즐겨찾기 토글
         */
        data class ToggleFavorite(val idol: IdolItem) : Intent()

        /**
         * 최애 설정/해제
         */
        data class SetMost(val idol: IdolItem?) : Intent()

        /**
         * 초기 데이터 로드
         */
        data object LoadInitialData : Intent()
    }

    /**
     * Side effects (일회성 이벤트).
     */
    sealed class Effect : UiEffect {
        /**
         * 토스트 메시지 표시
         */
        data class ShowToast(val message: String) : Effect()

        /**
         * 토스트 메시지 표시 (리소스 ID)
         */
        data class ShowToastRes(val messageResId: Int) : Effect()

        /**
         * 키보드 숨기기
         */
        data object HideKeyboard : Effect()
    }
}
