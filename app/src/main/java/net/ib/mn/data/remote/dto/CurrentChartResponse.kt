package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * charts/current/ API 응답
 *
 * old 프로젝트와 동일
 * CELEB이 아닌 앱에서 현재 진행중인 차트 정보를 반환
 */
data class CurrentChartResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("gcode") val gcode: Int? = null,
    @SerializedName("main") val main: MainChartModel? = null,
    @SerializedName("objects") val objects: List<ChartModel>? = null,
    @SerializedName("record_room") val recordRoom: RecordRoomModel? = null,
    var message: String? = null // 오류 발생시 처리용. 서버에서 주지는 않음
)

/**
 * 메인 차트 모델 (개인/그룹 차트 정보)
 *
 * old 프로젝트와 동일
 */
data class MainChartModel(
    @SerializedName("males") val males: List<ChartCodeInfo>? = null,
    @SerializedName("females") val females: List<ChartCodeInfo>? = null
)

/**
 * 차트 코드 정보
 *
 * old 프로젝트와 동일
 * code: 차트 코드 (예: "PR_S_M", "PR_G_F")
 * name: 차트 이름 (예: "개인", "그룹")
 * fullName: 전체 이름 (예: "남자 개인", "여자 그룹")
 */
data class ChartCodeInfo(
    @SerializedName("code") val code: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("full_name") val fullName: String? = null
)

/**
 * 차트 모델
 *
 * old 프로젝트와 동일
 */
data class ChartModel(
    @SerializedName("begin_date") val beginDate: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_rank_url") val imageRankUrl: String? = null,
    @SerializedName("target_month") val targetMonth: Int? = null,
    @SerializedName("aggregate_type") val aggregateType: List<String>? = null
)

/**
 * 기록실 모델
 *
 * old 프로젝트와 동일
 */
data class RecordRoomModel(
    @SerializedName("code") val code: String? = null,
    @SerializedName("name") val name: String? = null
)

/**
 * charts/idol_ids/ API 응답
 *
 * old 프로젝트와 동일
 * 특정 차트 코드의 아이돌 ID 리스트 반환
 *
 * 실제 응답:
 * {
 *   "gcode": 0,
 *   "objects": [4, 5, 6, 18, ...],
 *   "success": true
 * }
 */
data class ChartIdolIdsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("gcode") val gcode: Int? = null,
    @SerializedName("objects") val data: List<Int>? = null,  // objects 필드를 data로 매핑
    @SerializedName("msg") val msg: String? = null
)
