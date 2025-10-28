package net.ib.mn.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.model.MainChartModel
import net.ib.mn.core.data.repository.charts.ChartsRepository
import javax.inject.Inject

class GetIdolsChartCodesUseCase @Inject constructor(
    private val chartsRepository: ChartsRepository,
){
    suspend operator fun invoke(currentChartCodes: MainChartModel): Flow<Map<String, ArrayList<String>>> = flow{
        val result = chartsRepository.getIdolChartCodes().first()

        val males = currentChartCodes.males
        val females = currentChartCodes.females

        val duplicateCodes = females.map { it.code }.intersect(males.map { it.code }.toSet())

        // 겹치는 항목을 제외한 리스트 생성
        val filteredFemales = females.filter { it.code !in duplicateCodes }
        val filteredMales = males.filter { it.code !in duplicateCodes }

        // 중복 제거 후 남은 항목을 병합
        val uniqueChartCodes = (filteredFemales + filteredMales).toMutableList()

        // 중복되는 아이템을 각 리스트에서 하나씩 찾아 맨 뒤에 추가
        duplicateCodes.forEach { code ->
            // females에서 중복 항목 하나 가져오기
            females.find { it.code == code }?.let { uniqueChartCodes.add(it) }
        }

        // IdolChartCodes의 데이터를 코드 기준으로 반전하여 Map 생성
        val reversedMap = mutableMapOf<String, ArrayList<String>>()

        // idolChartCodesResponse의 각 항목을 돌면서 매칭되는 코드에 대해 반전된 Map에 추가
        result.data?.forEach { (key, codes) ->
            codes.forEach { code ->
                // uniqueChartCodes에 해당 코드가 존재하는 경우에만 추가
                if (uniqueChartCodes.any { it.code == code }) {
                    reversedMap.getOrPut(code) { arrayListOf() }.add(key)
                }
            }
        }

        emit(reversedMap)
//        idolsPreferenceRepository.saveIdolsChartCodes(reversedMap)
    }
}