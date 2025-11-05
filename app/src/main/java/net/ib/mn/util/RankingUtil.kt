package net.ib.mn.util

import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.ui.components.RankingItemData
import java.text.Collator
import java.util.Locale

/**
 * 랭킹 데이터 처리 유틸리티
 *
 * 랭킹 계산 로직:
 * 1. 하트 수(heart) 내림차순 정렬
 * 2. 동점일 경우 이름(name) 오름차순 정렬 (Collator 사용)
 * 3. 동점자는 동일한 순위 부여
 */

/**
 * processRanksData 결과 데이터
 */
data class ProcessedRankData(
    val rankItems: List<RankingItemData>,
    val topIdol: IdolEntity?
)

object RankingUtil {

    /**
     * RankingItemData 정렬 및 순위 계산
     *
     * @param items 정렬할 RankingItemData 리스트
     * @return 정렬되고 순위가 계산된 RankingItemData 리스트
     */
    fun sortAndRank(items: List<RankingItemData>): List<RankingItemData> {
        if (items.isEmpty()) return emptyList()

        // 1. 정렬: 하트 수 내림차순 -> 이름 오름차순
        val collator = Collator.getInstance(Locale.ROOT).apply {
            strength = Collator.PRIMARY
        }

        val sorted = items.sortedWith(
            compareByDescending<RankingItemData> { it.heartCount }
                .thenComparator { a, b -> collator.compare(a.name, b.name) }
        )

        // 2. 랭킹 계산 (동점자 처리)
        // 동점자는 이전 결과 아이템과 같은 순위를 받음
        val result = mutableListOf<RankingItemData>()
        sorted.forEachIndexed { index, item ->
            val rank = if (index > 0 && sorted[index - 1].heartCount == item.heartCount) {
                // 동점: 이전 결과 아이템의 순위 사용
                result[index - 1].rank
            } else {
                // 순위 변경: 현재 위치 기반 (1부터 시작)
                index + 1
            }
            result.add(item.copy(rank = rank))
        }

        return result
    }

    /**
     * 1위 RankingItemData 가져오기
     *
     * @param items 정렬할 RankingItemData 리스트
     * @return 1위 RankingItemData (없으면 null)
     */
    fun getTopRank(items: List<RankingItemData>): RankingItemData? {
        if (items.isEmpty()) return null

        // 정렬: 하트 수 내림차순 -> 이름 오름차순
        val collator = Collator.getInstance(Locale.ROOT).apply {
            strength = Collator.PRIMARY
        }

        val topItem = items.maxWithOrNull(
            compareBy<RankingItemData> { it.heartCount }
                .thenComparator { a, b -> -collator.compare(a.name, b.name) }
        )

        return topItem
    }

    /**
     * AggregateRankModel을 RankingItemData로 변환하고 1위 아이돌 정보 가져오기
     *
     * @param ranks AggregateRankModel 리스트
     * @param idolDao IdolDao 인스턴스
     * @param formatScore 점수 포맷팅 함수
     * @return ProcessedRankData (rankItems, topIdol)
     */
    suspend fun processRanksData(
        ranks: List<net.ib.mn.data.remote.dto.AggregateRankModel>,
        idolDao: IdolDao,
        formatScore: (Int) -> String
    ): ProcessedRankData {
        // 모든 idol ID 추출
        val idolIds = ranks.map { it.idolId }

        // DB에서 idol 정보 가져오기
        val idols = idolDao.getIdolsByIds(idolIds)
        val idolMap = idols.associateBy { it.id }

        // AggregateRankModel을 RankingItemData로 변환
        val maxScore = ranks.maxOfOrNull { it.score.toLong() } ?: 0L
        val minScore = ranks.minOfOrNull { it.score.toLong() } ?: 0L

        val rankItems = ranks.map { rank ->
            val idol = idolMap[rank.idolId]

            RankingItemData(
                rank = rank.scoreRank,
                name = rank.name,  // "이름_그룹명" 형식 그대로 사용
                voteCount = formatScore(rank.score),
                photoUrl = idol?.imageUrl,
                id = rank.idolId.toString(),
                miracleCount = idol?.miracleCount ?: 0,
                fairyCount = idol?.fairyCount ?: 0,
                angelCount = idol?.angelCount ?: 0,
                rookieCount = idol?.rookieCount ?: 0,
                heartCount = rank.score.toLong(),
                maxHeartCount = maxScore,
                minHeartCount = minScore,
                top3ImageUrls = idol?.let { IdolImageUtil.getTop3ImageUrls(it) } ?: listOf(null, null, null),
                top3VideoUrls = idol?.let { IdolImageUtil.getTop3VideoUrls(it) } ?: listOf(null, null, null)
            )
        }

        // 1위 아이돌 정보 가져오기 (ExoTop3용)
        val topIdol = getTopRank(rankItems)?.let { topRankItem ->
            idolMap[topRankItem.id.toInt()]
        }

        return ProcessedRankData(
            rankItems = rankItems,
            topIdol = topIdol
        )
    }

    /**
     * IdolEntity를 RankingItemData로 변환하고 1위 아이돌 정보 가져오기
     * (Group, Solo 랭킹용 - 정렬은 UI에서 수행)
     *
     * @param idols IdolEntity 리스트
     * @param formatHeartCount 하트 수 포맷팅 함수
     * @return ProcessedRankData (rankItems, topIdol)
     */
    fun processIdolsData(
        idols: List<IdolEntity>,
        formatHeartCount: (Int) -> String
    ): ProcessedRankData {
        val idolMap = idols.associateBy { it.id }

        // IdolEntity를 RankingItemData로 변환 (정렬은 MainRankingList에서 수행)
        // rank는 임시값 0, max/min도 임시값 0 (MainRankingList에서 재계산됨)
        val rankItems = idols.map { idol ->
            RankingItemData(
                rank = 0,  // MainRankingList에서 계산
                name = idol.name,  // "이름_그룹명" 형식 그대로 사용
                voteCount = formatHeartCount(idol.heart.toInt()),
                photoUrl = idol.imageUrl,
                id = idol.id.toString(),
                miracleCount = idol.miracleCount,
                fairyCount = idol.fairyCount,
                angelCount = idol.angelCount,
                rookieCount = idol.rookieCount,
                heartCount = idol.heart,
                maxHeartCount = 0L,  // MainRankingList에서 계산
                minHeartCount = 0L,  // MainRankingList에서 계산
                top3ImageUrls = IdolImageUtil.getTop3ImageUrls(idol),
                top3VideoUrls = IdolImageUtil.getTop3VideoUrls(idol)
            )
        }

        // 1위 아이돌 정보 가져오기 (ExoTop3용)
        val topIdol = getTopRank(rankItems)?.let { topRankItem ->
            idolMap[topRankItem.id.toInt()]
        }

        return ProcessedRankData(
            rankItems = rankItems,
            topIdol = topIdol
        )
    }
}
