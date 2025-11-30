package net.ib.mn.util

import android.content.Context
import android.text.TextUtils
import androidx.appcompat.app.AppCompatDelegate
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.ui.components.RankingItem
import java.text.Collator
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 랭킹 데이터 처리 유틸리티
 *
 * 랭킹 계산 로직:
 * 1. 하트 수(heart) 내림차순 정렬
 * 2. 동점일 경우 이름(name) 오름차순 정렬 (Collator 사용)
 * 3. 동점자는 동일한 순위 부여
 *
 * 다국어 처리:
 * - old 프로젝트의 IdolModel.getName(context) 로직과 동일
 * - 현재 언어 설정에 따라 적절한 이름 필드 반환
 */

/**
 * processIdolsData 결과 데이터
 */
data class ProcessedRankData(
    val rankItems: List<RankingItem>,
    val topIdol: IdolEntity?
)

object RankingUtil {

    // 공통 Collator (이름 정렬용) - 불변 객체로 재사용
    private val nameCollator = Collator.getInstance(Locale.ROOT).apply {
        strength = Collator.PRIMARY
    }

    /**
     * IdolEntity에서 현재 언어에 맞는 이름을 가져오기
     * old 프로젝트의 IdolModel.getName(context) 로직과 동일
     *
     * @param idol IdolEntity 인스턴스
     * @param context Context (언어 설정 확인용)
     * @return 현재 언어에 맞는 이름
     */
    fun getLocalizedName(idol: IdolEntity, context: Context): String {
        try {
            // AppCompatDelegate에서 설정된 언어 가져오기
            val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
            val lang = locale.language.lowercase()

            return when {
                lang.startsWith("en") && !TextUtils.isEmpty(idol.nameEn) -> idol.nameEn
                lang.startsWith("ko") -> idol.name
                lang == "zh" && locale.country == "TW" && !TextUtils.isEmpty(idol.nameZhTw) -> idol.nameZhTw
                lang.startsWith("zh") && !TextUtils.isEmpty(idol.nameZh) -> idol.nameZh
                lang.startsWith("ja") && !TextUtils.isEmpty(idol.nameJp) -> idol.nameJp
                !TextUtils.isEmpty(idol.nameEn) -> idol.nameEn
                else -> idol.name
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return idol.nameEn.ifEmpty { idol.name }
        }
    }

    /**
     * IdolEntity에서 현재 언어에 맞는 팬덤 이름을 가져오기
     *
     * @param idol IdolEntity 인스턴스
     * @param context Context (언어 설정 확인용)
     * @return 현재 언어에 맞는 팬덤 이름 (없으면 null)
     */
    fun getLocalizedFandomName(idol: IdolEntity, context: Context): String? {
        try {
            val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
            val lang = locale.language.lowercase()

            return when {
                lang.startsWith("ko") && !idol.fdName.isNullOrEmpty() -> idol.fdName
                !idol.fdNameEn.isNullOrEmpty() -> idol.fdNameEn
                !idol.fdName.isNullOrEmpty() -> idol.fdName
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return idol.fdNameEn ?: idol.fdName
        }
    }

    /**
     * 생일 포맷팅 (yyyy-MM-dd -> 로케일에 맞는 날짜 형식)
     * Old 프로젝트의 IdolCommunityDialogFragment 로직과 동일
     *
     * @param birthDay "yyyy-MM-dd" 형식의 생일 문자열
     * @param isLunarBirthday 음력 생일 여부 ("Y" 또는 null)
     * @param context Context (로케일 확인용)
     * @return 포맷된 생일 문자열 (예: "1995년 1월 1일" 또는 "Jan 1, 1995")
     */
    fun formatBirthday(birthDay: String?, isLunarBirthday: String?, context: Context): String? {
        if (birthDay.isNullOrEmpty()) return null
        return try {
            val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", locale)
            val date = inputFormat.parse(birthDay) ?: return null
            val outputFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
            var formattedDate = outputFormat.format(date)
            if (isLunarBirthday == "Y") {
                formattedDate += " (음력)"
            }
            formattedDate
        } catch (e: Exception) {
            e.printStackTrace()
            birthDay  // 파싱 실패 시 원본 반환
        }
    }

    /**
     * 이름을 "_"로 분리 (이름_그룹명 형식)
     * old 프로젝트의 Util.nameSplit(name) 로직과 동일
     *
     * @param fullName "이름_그룹명" 형식의 문자열
     * @return Pair<이름, 그룹명>
     */
    fun splitName(fullName: String): Pair<String, String> {
        return if (fullName.contains("_")) {
            val parts = fullName.split("_", limit = 2)
            Pair(parts[0], parts.getOrNull(1) ?: "")
        } else {
            Pair(fullName, "")
        }
    }

    /**
     * RankingItem 정렬 및 순위 계산
     *
     * @param items 정렬할 RankingItem 리스트
     * @return 정렬되고 순위가 계산된 RankingItem 리스트
     */
    fun sortAndRank(items: List<RankingItem>): List<RankingItem> {
        if (items.isEmpty()) return emptyList()

        // 1. 정렬: 하트 수 내림차순 -> 이름 오름차순
        val sorted = items.sortedWith(
            compareByDescending<RankingItem> { it.heartCount }
                .thenComparator { a, b -> nameCollator.compare(a.name, b.name) }
        )

        // 2. 랭킹 계산 (동점자 처리)
        // 동점자는 이전 결과 아이템과 같은 순위를 받음
        val result = mutableListOf<RankingItem>()
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
     * 1위 RankingItem 가져오기 (하트 수 기준)
     *
     * @param items RankingItem 리스트
     * @return 하트 수가 가장 많은 RankingItem (없으면 null)
     */
    private fun getTopRank(items: List<RankingItem>): RankingItem? {
        return items.maxByOrNull { it.heartCount }
    }

    /**
     * 투표 성공 후 랭킹 데이터 업데이트
     *
     * 1. 로컬 DB의 투표 수 업데이트
     * 2. 투표한 아이돌의 하트 수 증가
     * 3. 재정렬 및 순위 재계산
     * 4. max/min 재계산 및 적용
     *
     * @param items 현재 랭킹 아이템 리스트
     * @param idolId 투표한 아이돌 ID
     * @param voteCount 투표한 하트 수
     * @param idolDao IdolDao 인스턴스 (로컬 DB 업데이트용)
     * @param formatHeartCount 하트 수 포맷팅 함수
     * @return 업데이트되고 정렬된 랭킹 아이템 리스트
     */
    suspend fun updateVoteAndRerank(
        items: List<RankingItem>,
        idolId: Int,
        voteCount: Long,
        idolDao: IdolDao,
        formatHeartCount: (Long) -> String
    ): List<RankingItem> {
        // 1. 로컬 DB의 투표 수 업데이트
        try {
            val idol = idolDao.getIdolById(idolId)
            if (idol != null) {
                val newHeart = idol.heart + voteCount
                idolDao.updateIdolHeart(idolId, newHeart)
                android.util.Log.d("RankingUtil", "✅ DB updated: idol=$idolId, newHeart=$newHeart")
            } else {
                android.util.Log.w("RankingUtil", "⚠️ Idol not found in DB: idol=$idolId")
            }
        } catch (e: Exception) {
            android.util.Log.e("RankingUtil", "❌ Failed to update DB: ${e.message}", e)
        }

        // 2. 투표한 아이돌의 하트 수 업데이트 (메모리)
        val updatedItems = items.map { item ->
            if (item.id == idolId.toString()) {
                val newHeart = item.heartCount + voteCount
                item.copy(
                    voteCount = formatHeartCount(newHeart),
                    heartCount = newHeart
                )
            } else {
                item
            }
        }

        // 3. 재정렬 및 순위 재계산
        val sortedItems = sortAndRank(updatedItems)

        // 4. max/min 재계산
        val maxHeart = sortedItems.maxOfOrNull { it.heartCount } ?: 0L
        val minHeart = sortedItems.minOfOrNull { it.heartCount } ?: 0L

        // 5. 모든 아이템에 새로운 max/min 적용
        return sortedItems.map { item ->
            item.copy(
                maxHeartCount = maxHeart,
                minHeartCount = minHeart
            )
        }
    }


    /**
     * IdolEntity를 RankingItem로 변환하고 1위 아이돌 정보 가져오기
     * (Group, Solo 랭킹용 - 정렬 및 순위 계산 포함)
     *
     * @param idols IdolEntity 리스트
     * @param context Context (다국어 이름 처리용)
     * @param mostIdolId 최애 아이돌 ID (배경색 하이라이트용)
     * @param formatHeartCount 하트 수 포맷팅 함수
     * @return ProcessedRankData (rankItems, topIdol)
     */
    fun processIdolsData(
        idols: List<IdolEntity>,
        context: Context,
        mostIdolId: Int?,
        formatHeartCount: (Int) -> String
    ): ProcessedRankData {
        val idolMap = idols.associateBy { it.id }

        // IdolEntity를 RankingItem로 변환 (임시 rank, max/min 값)
        val tempItems = idols.map { idol ->
            // 다국어 이름 가져오기 (old 프로젝트와 동일)
            val localizedName = getLocalizedName(idol, context)

            // 최애 여부 판단 (old 프로젝트와 동일)
            val isFavorite = mostIdolId != null && idol.id == mostIdolId
            if (isFavorite) {
                android.util.Log.d("RankingUtil", "💗 Found favorite idol: id=${idol.id}, name=${idol.name}")
            }

            RankingItem(
                rank = 0,  // sortAndRank에서 계산
                name = localizedName,  // 다국어 처리된 이름 사용
                voteCount = formatHeartCount(idol.heart.toInt()),
                photoUrl = idol.imageUrl,
                id = idol.id.toString(),
                miracleCount = idol.miracleCount,
                fairyCount = idol.fairyCount,
                angelCount = idol.angelCount,
                rookieCount = idol.rookieCount,
                heartCount = idol.heart,
                maxHeartCount = 0L,  // 아래에서 계산
                minHeartCount = 0L,  // 아래에서 계산
                isFavorite = isFavorite,  // 최애 여부 설정
                anniversary = idol.anniversary.takeIf { it != "N" },
                anniversaryDays = idol.anniversaryDays ?: 0,
                top3ImageUrls = IdolImageUtil.getTop3ImageUrls(idol),
                top3VideoUrls = IdolImageUtil.getTop3VideoUrls(idol),
                fandomName = getLocalizedFandomName(idol, context),
                birthday = formatBirthday(idol.birthDay, idol.isLunarBirthday, context).also {
                    android.util.Log.d("RankingUtil", "🎂 idol=${idol.name}, birthDay=${idol.birthDay}, isLunar=${idol.isLunarBirthday}, formatted=$it")
                }
            )
        }

        // 정렬 및 순위 계산
        val sortedItems = sortAndRank(tempItems)

        // max/min 하트 수 계산
        val maxHeart = sortedItems.maxOfOrNull { it.heartCount } ?: 0L
        val minHeart = sortedItems.minOfOrNull { it.heartCount } ?: 0L

        // 최종 rankItems (max/min 적용)
        val rankItems = sortedItems.map { item ->
            item.copy(
                maxHeartCount = maxHeart,
                minHeartCount = minHeart
            )
        }

        // 1위 아이돌 정보 가져오기 (ExoTop3용)
        val topIdol = rankItems.firstOrNull()?.let { topRankItem ->
            idolMap[topRankItem.id.toInt()]
        }

        return ProcessedRankData(
            rankItems = rankItems,
            topIdol = topIdol
        )
    }
}
