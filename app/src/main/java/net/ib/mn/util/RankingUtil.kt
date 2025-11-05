package net.ib.mn.util

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
object RankingUtil {

    /**
     * 랭킹 데이터 정렬 및 순위 계산
     *
     * @param items 정렬할 아이템 리스트
     * @param getHeart 하트 수를 가져오는 함수
     * @param getName 이름을 가져오는 함수
     * @param getRank 순위를 가져오는 함수 (계산된 아이템에서)
     * @param createRankedItem 순위가 포함된 새 아이템을 생성하는 함수
     * @return 정렬되고 순위가 계산된 아이템 리스트
     *
     * 예시:
     * ```
     * val rankedItems = RankingUtil.sortAndRank(
     *     items = idols,
     *     getHeart = { it.heart },
     *     getName = { it.name },
     *     getRank = { it.rank },
     *     createRankedItem = { item, rank -> item.copy(rank = rank) }
     * )
     * ```
     */
    fun <T> sortAndRank(
        items: List<T>,
        getHeart: (T) -> Long,
        getName: (T) -> String,
        getRank: (T) -> Int,
        createRankedItem: (T, Int) -> T
    ): List<T> {
        if (items.isEmpty()) return emptyList()

        // 1. 정렬: 하트 수 내림차순 -> 이름 오름차순
        val collator = Collator.getInstance(Locale.ROOT).apply {
            strength = Collator.PRIMARY
        }

        val sorted = items.sortedWith(
            compareByDescending<T> { getHeart(it) }
                .thenComparator { a, b -> collator.compare(getName(a), getName(b)) }
        )

        // 2. 랭킹 계산 (동점자 처리)
        // 동점자는 이전 결과 아이템과 같은 순위를 받음
        val result = mutableListOf<T>()
        sorted.forEachIndexed { index, item ->
            val rank = if (index > 0 && getHeart(sorted[index - 1]) == getHeart(item)) {
                // 동점: 이전 결과 아이템의 순위 사용
                getRank(result[index - 1])
            } else {
                // 순위 변경: 현재 위치 기반 (1부터 시작)
                index + 1
            }
            result.add(createRankedItem(item, rank))
        }

        return result
    }

    /**
     * 간단한 랭킹 계산 (정렬된 리스트에서 순위만 부여)
     *
     * @param sortedItems 이미 정렬된 아이템 리스트
     * @param getHeart 하트 수를 가져오는 함수
     * @param getRank 순위를 가져오는 함수 (계산된 아이템에서)
     * @param createRankedItem 순위가 포함된 새 아이템을 생성하는 함수
     * @return 순위가 계산된 아이템 리스트
     */
    fun <T> calculateRank(
        sortedItems: List<T>,
        getHeart: (T) -> Long,
        getRank: (T) -> Int,
        createRankedItem: (T, Int) -> T
    ): List<T> {
        val result = mutableListOf<T>()
        sortedItems.forEachIndexed { index, item ->
            val rank = if (index > 0 && getHeart(sortedItems[index - 1]) == getHeart(item)) {
                // 동점: 이전 결과 아이템의 순위 사용
                getRank(result[index - 1])
            } else {
                index + 1
            }
            result.add(createRankedItem(item, rank))
        }
        return result
    }
}
