package net.ib.mn.presentation.community

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.ConfigRepository
import net.ib.mn.domain.repository.ItemShopRepository
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * BurningDayViewModel - 올인데이 설정 ViewModel
 *
 * old 프로젝트의 BurningDayPurchaseDialogViewModel과 동일한 기능
 */
@HiltViewModel
class BurningDayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val itemShopRepository: ItemShopRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    companion object {
        private const val TAG = "BurningDayVM"
    }

    private val _uiState = MutableStateFlow<BurningDayUiState>(BurningDayUiState.Loading)
    val uiState: StateFlow<BurningDayUiState> = _uiState.asStateFlow()

    /**
     * 데이터 로드 (아이템 상점 목록 + configSelf)
     */
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = BurningDayUiState.Loading

            try {
                // 1. 아이템 상점 목록 조회 (다이아몬드 비용 확인)
                val itemShopResult = itemShopRepository.getItemShopList().first { it !is ApiResult.Loading }

                if (itemShopResult is ApiResult.Error) {
                    logE(TAG, "❌ Failed to load item shop list: ${itemShopResult.error}")
                    _uiState.value = BurningDayUiState.Error(
                        context.getString(R.string.error_abnormal_default)
                    )
                    return@launch
                }

                // 2. configSelf에서 itemLevel 가져오기
                val configResult = configRepository.getConfigSelf().first { it !is ApiResult.Loading }
                val itemLevel = when (configResult) {
                    is ApiResult.Success -> configResult.data.itemLevel
                    else -> 0
                }

                // 3. 다이아몬드 비용 가져오기
                val diamondCost = itemShopRepository.getBurningDayDiamondCost()

                // 4. 날짜 목록 생성 (오늘부터 10일)
                val (displayDays, daysYmd) = generateDaysList()

                logD(TAG, "✅ Data loaded: itemLevel=$itemLevel, diamondCost=$diamondCost, days=${displayDays.size}")

                _uiState.value = BurningDayUiState.Ready(
                    itemLevel = itemLevel,
                    diamondCost = diamondCost,
                    displayDays = displayDays,
                    daysYmd = daysYmd
                )
            } catch (e: Exception) {
                logE(TAG, "❌ Exception: ${e.message}", e)
                _uiState.value = BurningDayUiState.Error(
                    context.getString(R.string.error_abnormal_default)
                )
            }
        }
    }

    /**
     * 올인데이 구매
     */
    fun purchaseBurningDay(burningDay: String) {
        viewModelScope.launch {
            _uiState.value = BurningDayUiState.Purchasing

            try {
                val result = itemShopRepository.purchaseBurningDay(burningDay).first { it !is ApiResult.Loading }

                when (result) {
                    is ApiResult.Success -> {
                        logD(TAG, "✅ BurningDay purchased: ${result.data}")
                        _uiState.value = BurningDayUiState.Success(result.data)
                    }
                    is ApiResult.Error -> {
                        logE(TAG, "❌ Purchase failed: ${result.error}")
                        _uiState.value = BurningDayUiState.Error(
                            result.error.message ?: context.getString(R.string.error_abnormal_default)
                        )
                    }
                    is ApiResult.Loading -> {
                        // 이미 필터링됨
                    }
                }
            } catch (e: Exception) {
                logE(TAG, "❌ Exception: ${e.message}", e)
                _uiState.value = BurningDayUiState.Error(
                    context.getString(R.string.error_abnormal_default)
                )
            }
        }
    }

    /**
     * 날짜 목록 생성 (오늘부터 10일)
     * @return Pair(표시용 날짜 목록, API용 yyyy-MM-dd 목록)
     */
    private fun generateDaysList(): Pair<List<String>, List<String>> {
        val displayDays = mutableListOf<String>()
        val daysYmd = mutableListOf<String>()

        val calendar = Calendar.getInstance()

        // 표시용 포맷 (월 일 (요일))
        val dfMd = SimpleDateFormat(
            context.getString(R.string.burning_day_format),
            Locale.getDefault()
        )
        dfMd.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        // API용 포맷 (yyyy-MM-dd)
        val dfYmd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dfYmd.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        // 오늘부터 10일 생성
        for (i in 0..9) {
            displayDays.add(dfMd.format(calendar.time))
            daysYmd.add(dfYmd.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return Pair(displayDays, daysYmd)
    }
}

/**
 * BurningDayUiState - 올인데이 설정 UI 상태
 */
sealed class BurningDayUiState {
    /** 로딩 중 */
    data object Loading : BurningDayUiState()

    /** 데이터 준비 완료 */
    data class Ready(
        val itemLevel: Int,
        val diamondCost: Int,
        val displayDays: List<String>,  // 표시용 날짜 목록
        val daysYmd: List<String>  // API용 날짜 목록 (yyyy-MM-dd)
    ) : BurningDayUiState()

    /** 구매 중 */
    data object Purchasing : BurningDayUiState()

    /** 성공 */
    data class Success(val burningDay: String) : BurningDayUiState()

    /** 에러 */
    data class Error(val message: String) : BurningDayUiState()
}
