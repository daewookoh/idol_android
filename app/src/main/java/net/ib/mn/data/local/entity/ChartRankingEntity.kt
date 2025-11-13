package net.ib.mn.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * 차트 랭킹 데이터를 저장하는 Entity (최적화 버전)
 *
 * RankingItemData의 모든 필드를 포함하여 조인 없이 빠르게 데이터 조회 가능
 *
 * 5개 차트:
 * - PR_S_F: Female Solo
 * - PR_S_M: Male Solo
 * - PR_G_F: Female Group
 * - PR_G_M: Male Group
 * - GLOBALS: Global
 *
 * Primary Key: (chartCode, idolId)
 * Index: chartCode (차트별 조회 최적화)
 */
@Entity(
    tableName = "chart_rankings",
    primaryKeys = ["chartCode", "idolId"],
    indices = [Index(value = ["chartCode"])]
)
data class ChartRankingEntity(
    // 차트 정보
    val chartCode: String,          // 차트 코드
    val idolId: Int,                 // 아이돌 ID
    val rank: Int,                   // 순위

    // 하트 정보
    val heartCount: Long,            // 하트 수
    val maxHeartCount: Long,         // 차트 내 최대 하트 수
    val minHeartCount: Long,         // 차트 내 최소 하트 수
    val voteCount: String,           // 포맷된 하트 수 (예: "1,234,567")

    // 아이돌 기본 정보
    val name: String,                // 다국어 처리된 이름
    val photoUrl: String?,           // 프로필 이미지 URL

    // 배지 정보
    val miracleCount: Int = 0,       // 미라클 배지 수
    val fairyCount: Int = 0,         // 요정 배지 수
    val angelCount: Int = 0,         // 천사 배지 수
    val rookieCount: Int = 0,        // 루키 배지 수
    val superRookieCount: Int = 0,   // 슈퍼 루키 배지 수

    // 기념일 정보
    val anniversary: String? = null, // 기념일 타입
    val anniversaryDays: Int = 0,    // 몰빵일 일수

    // Top3 이미지/비디오 (JSON 배열 또는 구분자로 저장)
    val top3Image1: String? = null,
    val top3Image2: String? = null,
    val top3Image3: String? = null,
    val top3Video1: String? = null,
    val top3Video2: String? = null,
    val top3Video3: String? = null,

    // 업데이트 시간
    val updatedAt: Long = System.currentTimeMillis()  // 업데이트 시간 (타임스탬프)
)
