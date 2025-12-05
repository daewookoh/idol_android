package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName
import net.ib.mn.data.repository.HasSuccess

/**
 * 아이템 상점 목록 응답
 *
 * GET market_lists/
 */
data class ItemShopListResponse(
    @SerializedName("success")
    override val success: Boolean = false,

    @SerializedName("gcode")
    val gcode: Int = 0,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("objects")
    val objects: List<ItemShopModel>? = null
) : HasSuccess

/**
 * 아이템 상점 모델
 *
 * market_no == 4 가 올인데이(BurningDay) 아이템
 */
data class ItemShopModel(
    @SerializedName("market_no")
    val marketNo: Int = 0,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("description2")
    val description2: String? = null,

    @SerializedName("heart_value")
    val heartValue: Int = 0,

    @SerializedName("diamond_value")
    val diamondValue: Int = 0,

    @SerializedName("is_viewable")
    val isViewable: String? = null,

    @SerializedName("resource_uri")
    val resourceUri: String? = null
)

/**
 * 올인데이 구매 요청
 *
 * POST market_lists/set_burning_day/
 */
data class PurchaseBurningDayRequest(
    @SerializedName("burning_day")
    val burningDay: String  // yyyy-MM-dd 형식
)

/**
 * 올인데이 구매 응답
 */
data class PurchaseBurningDayResponse(
    @SerializedName("success")
    override val success: Boolean = false,

    @SerializedName("gcode")
    val gcode: Int = 0,

    @SerializedName("msg")
    val msg: String? = null
) : HasSuccess
