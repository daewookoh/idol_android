package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.dto.ItemShopModel
import net.ib.mn.domain.model.ApiResult

/**
 * 아이템 상점 Repository 인터페이스
 */
interface ItemShopRepository {

    /**
     * 아이템 상점 목록 조회
     * @return 아이템 목록 (market_no == 4 가 올인데이 아이템)
     */
    fun getItemShopList(): Flow<ApiResult<List<ItemShopModel>>>

    /**
     * 올인데이 다이아몬드 비용 조회
     * 캐시된 아이템 상점 목록에서 market_no == 4인 아이템의 diamond_value 반환
     * @return 다이아몬드 비용 (캐시가 없으면 0)
     */
    fun getBurningDayDiamondCost(): Int

    /**
     * 올인데이 구매
     * @param burningDay 날짜 (yyyy-MM-dd 형식)
     * @return 성공 시 burningDay 문자열 반환
     */
    fun purchaseBurningDay(burningDay: String): Flow<ApiResult<String>>
}
