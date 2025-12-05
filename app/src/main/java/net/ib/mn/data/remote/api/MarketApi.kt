package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.ItemShopListResponse
import net.ib.mn.data.remote.dto.PurchaseBurningDayRequest
import net.ib.mn.data.remote.dto.PurchaseBurningDayResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Market API (아이템 상점)
 *
 * old 프로젝트의 MarketApi와 동일
 */
interface MarketApi {

    /**
     * 아이템 상점 목록 조회
     * market_no == 4 가 burning day 아이템 (diamond_value가 필요 다이아 수)
     */
    @GET("market_lists/")
    suspend fun getItemShopList(): Response<ItemShopListResponse>

    /**
     * 올인데이(BurningDay) 구매
     * @param body burning_day (yyyy-MM-dd 형식)
     */
    @POST("market_lists/set_burning_day/")
    suspend fun purchaseBurningDay(
        @Body body: PurchaseBurningDayRequest
    ): Response<PurchaseBurningDayResponse>
}
