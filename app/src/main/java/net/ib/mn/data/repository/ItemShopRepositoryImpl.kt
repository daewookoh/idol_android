package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ib.mn.data.remote.api.MarketApi
import net.ib.mn.data.remote.dto.ItemShopModel
import net.ib.mn.data.remote.dto.PurchaseBurningDayRequest
import net.ib.mn.domain.model.ApiError
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.ItemShopRepository
import net.ib.mn.util.logD
import javax.inject.Inject

/**
 * 아이템 상점 Repository 구현체
 *
 * old 프로젝트의 ItemShopRepositoryImpl과 동일한 기능
 */
class ItemShopRepositoryImpl @Inject constructor(
    private val marketApi: MarketApi
) : BaseRepository(), ItemShopRepository {

    companion object {
        private const val TAG = "ItemShopRepo"
        private const val BURNING_DAY_MARKET_NO = 4  // 올인데이 아이템 번호
    }

    // 아이템 상점 캐시 (다이아몬드 비용 조회용)
    @Volatile
    private var cachedItemShopList: List<ItemShopModel>? = null

    override fun getItemShopList(): Flow<ApiResult<List<ItemShopModel>>> =
        safeApiCall { marketApi.getItemShopList() }
            .validateSuccess()
            .extractList { it.objects }
            .map { result ->
                if (result is ApiResult.Success) {
                    // 캐시에 저장
                    cachedItemShopList = result.data
                    logD(TAG, "✅ ItemShopList cached: ${result.data.size} items")
                }
                result
            }

    override fun getBurningDayDiamondCost(): Int {
        return cachedItemShopList
            ?.find { it.marketNo == BURNING_DAY_MARKET_NO }
            ?.diamondValue
            ?: 0
    }

    override fun purchaseBurningDay(burningDay: String): Flow<ApiResult<String>> =
        safeApiCall { marketApi.purchaseBurningDay(PurchaseBurningDayRequest(burningDay)) }
            .map { result ->
                when (result) {
                    is ApiResult.Success -> {
                        if (result.data.success) {
                            logD(TAG, "✅ BurningDay purchased: $burningDay")
                            ApiResult.Success(burningDay)
                        } else {
                            logD(TAG, "❌ BurningDay purchase failed: ${result.data.msg}")
                            ApiResult.Error(
                                ApiError.Business(
                                    gcode = result.data.gcode,
                                    message = result.data.msg ?: "Purchase failed"
                                )
                            )
                        }
                    }
                    is ApiResult.Error -> result
                    is ApiResult.Loading -> ApiResult.Loading
                }
            }
}
