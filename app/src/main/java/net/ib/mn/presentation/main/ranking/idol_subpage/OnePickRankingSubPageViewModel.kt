package net.ib.mn.presentation.main.ranking.idol_subpage

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ImagePickModel
import net.ib.mn.domain.model.ThemePickModel
import net.ib.mn.domain.repository.OnepickRepository
import net.ib.mn.domain.repository.ThemepickRepository
import net.ib.mn.ui.components.ThemePickState
import net.ib.mn.ui.components.ImagePickState
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.NumberFormatUtil
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * OnePick (테마픽/이미지픽) ViewModel
 *
 * 테마픽과 이미지픽을 탭으로 전환하며 표시
 *
 * SavedStateHandle을 사용하여 탭 선택을 저장:
 * - 앱을 내렸다 올려도 유지 (프로세스가 살아있을 때)
 * - 앱을 재시작하면 리셋 (프로세스 종료 후)
 */
@HiltViewModel(assistedFactory = OnePickRankingSubPageViewModel.Factory::class)
class OnePickRankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @ApplicationContext private val context: Context,
    private val themepickRepository: ThemepickRepository,
    private val onepickRepository: OnepickRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * 탭 타입
     */
    enum class TabType {
        THEME_PICK,  // 테마픽
        IMAGE_PICK   // 이미지픽
    }

    sealed interface UiState {
        data object Loading : UiState
        data class ThemePickSuccess(val items: List<ThemePickCardData>) : UiState
        data class ImagePickSuccess(val items: List<ImagePickCardData>) : UiState
        data class Error(val message: String) : UiState
    }

    companion object {
        private const val KEY_CURRENT_TAB = "currentTab"
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var cachedThemePickData: List<ThemePickCardData>? = null
    private var cachedImagePickData: List<ImagePickCardData>? = null

    // SavedStateHandle을 사용하여 탭 선택 저장/복원
    private var currentTab: TabType
        get() = TabType.valueOf(savedStateHandle.get<String>(KEY_CURRENT_TAB) ?: TabType.THEME_PICK.name)
        set(value) {
            savedStateHandle[KEY_CURRENT_TAB] = value.name
        }

    init {
        android.util.Log.d("OnePickRankingVM", "🆕 ViewModel created for chartCode: $chartCode")
        android.util.Log.d("OnePickRankingVM", "📌 Restored tab: $currentTab")
        // 저장된 탭 상태에 따라 로드
        when (currentTab) {
            TabType.THEME_PICK -> loadThemePickList()
            TabType.IMAGE_PICK -> loadImagePickList()
        }
    }

    fun reloadIfNeeded() {
        when (currentTab) {
            TabType.THEME_PICK -> {
                if (cachedThemePickData != null) {
                    android.util.Log.d("OnePickRankingVM", "✓ Using cached ThemePick data")
                    _uiState.value = UiState.ThemePickSuccess(cachedThemePickData!!)
                } else {
                    loadThemePickList()
                }
            }
            TabType.IMAGE_PICK -> {
                if (cachedImagePickData != null) {
                    android.util.Log.d("OnePickRankingVM", "✓ Using cached ImagePick data")
                    _uiState.value = UiState.ImagePickSuccess(cachedImagePickData!!)
                } else {
                    loadImagePickList()
                }
            }
        }
    }

    /**
     * 탭 전환
     */
    fun switchTab(tabType: TabType) {
        if (currentTab == tabType) return

        currentTab = tabType
        android.util.Log.d("OnePickRankingVM", "🔄 Switching tab to: $tabType")

        when (tabType) {
            TabType.THEME_PICK -> {
                if (cachedThemePickData != null) {
                    _uiState.value = UiState.ThemePickSuccess(cachedThemePickData!!)
                } else {
                    loadThemePickList()
                }
            }
            TabType.IMAGE_PICK -> {
                if (cachedImagePickData != null) {
                    _uiState.value = UiState.ImagePickSuccess(cachedImagePickData!!)
                } else {
                    loadImagePickList()
                }
            }
        }
    }

    /**
     * 테마픽 목록 로드
     */
    private fun loadThemePickList() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading

            android.util.Log.d("OnePickRankingVM", "========================================")
            android.util.Log.d("OnePickRankingVM", "[ThemePick] Loading theme pick list")
            android.util.Log.d("OnePickRankingVM", "  - API: themepick/")

            themepickRepository.getThemePickList(offset = 0, limit = 30).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        android.util.Log.d("OnePickRankingVM", "⏳ Loading...")
                    }
                    is ApiResult.Success -> {
                        android.util.Log.d("OnePickRankingVM", "✅ SUCCESS - ThemePicks count: ${result.data.size}")
                        processThemePickData(result.data)
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e("OnePickRankingVM", "❌ ERROR: ${result.message}")
                        _uiState.value = UiState.Error(result.message ?: result.exception.message ?: "Error loading data")
                    }
                }
            }
        }
    }

    /**
     * 이미지픽 목록 로드
     */
    private fun loadImagePickList() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading

            android.util.Log.d("OnePickRankingVM", "========================================")
            android.util.Log.d("OnePickRankingVM", "[ImagePick] Loading image pick list")
            android.util.Log.d("OnePickRankingVM", "  - API: onepick/")

            onepickRepository.getImagePickList(offset = 0, limit = 30).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        android.util.Log.d("OnePickRankingVM", "⏳ Loading...")
                    }
                    is ApiResult.Success -> {
                        android.util.Log.d("OnePickRankingVM", "✅ SUCCESS - ImagePicks count: ${result.data.size}")
                        processImagePickData(result.data)
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e("OnePickRankingVM", "❌ ERROR: ${result.message}")
                        _uiState.value = UiState.Error(result.message ?: result.exception.message ?: "Error loading data")
                    }
                }
            }
        }
    }

    private fun processThemePickData(themePickList: List<ThemePickModel>) {
        try {
            val cardDataList = themePickList.map { themePick ->
                val state = when (themePick.status) {
                    ThemePickModel.STATUS_PREPARING -> ThemePickState.UPCOMING
                    ThemePickModel.STATUS_PROGRESS -> ThemePickState.ACTIVE
                    else -> ThemePickState.ENDED
                }

                val periodDate = formatPeriodDate(themePick.beginAt, themePick.expiredAt)
                val voteCount = NumberFormatUtil.formatNumberShort(themePick.count)

                // UPCOMING 상태일 때 D-Day 계산
                val subTitle = if (state == ThemePickState.UPCOMING) {
                    calculateDDay(themePick.beginAt)
                } else {
                    themePick.subtitle
                }

                ThemePickCardData(
                    id = themePick.id,
                    state = state,
                    title = themePick.title,
                    subTitle = subTitle,
                    imageUrl = themePick.imageUrl.toSecureUrl(),
                    voteCount = voteCount,
                    periodDate = periodDate
                )
            }

            android.util.Log.d("OnePickRankingVM", "✅ Processed ${cardDataList.size} theme picks")

            cachedThemePickData = cardDataList
            _uiState.value = UiState.ThemePickSuccess(cardDataList)
        } catch (e: Exception) {
            android.util.Log.e("OnePickRankingVM", "❌ Exception: ${e.message}", e)
            _uiState.value = UiState.Error(e.message ?: "Error")
        }
    }

    private fun processImagePickData(imagePickList: List<ImagePickModel>) {
        try {
            val cardDataList = imagePickList.map { imagePick ->
                val state = when (imagePick.status) {
                    ImagePickModel.STATUS_PREPARING -> ImagePickState.UPCOMING
                    ImagePickModel.STATUS_PROGRESS -> ImagePickState.ACTIVE
                    else -> ImagePickState.ENDED
                }

                val periodDate = formatPeriodDate(imagePick.createdAt, imagePick.expiredAt)
                val voteCount = NumberFormatUtil.formatNumberShort(imagePick.count)

                // UPCOMING 상태일 때 D-Day 계산
                val subTitle = if (state == ImagePickState.UPCOMING) {
                    calculateDDay(imagePick.createdAt)
                } else {
                    imagePick.subtitle
                }

                ImagePickCardData(
                    id = imagePick.id,
                    state = state,
                    title = imagePick.title,
                    subTitle = subTitle,
                    imageUrl = "", // 이미지픽은 별도 이미지 URL이 없음
                    voteCount = voteCount,
                    periodDate = periodDate
                )
            }

            android.util.Log.d("OnePickRankingVM", "✅ Processed ${cardDataList.size} image picks")

            cachedImagePickData = cardDataList
            _uiState.value = UiState.ImagePickSuccess(cardDataList)
        } catch (e: Exception) {
            android.util.Log.e("OnePickRankingVM", "❌ Exception: ${e.message}", e)
            _uiState.value = UiState.Error(e.message ?: "Error")
        }
    }

    private fun formatPeriodDate(beginAt: String, expiredAt: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy. M. d.", Locale.getDefault())

            val beginDate = inputFormat.parse(beginAt)
            val endDate = inputFormat.parse(expiredAt)

            if (beginDate != null && endDate != null) {
                "${outputFormat.format(beginDate)} ~ ${outputFormat.format(endDate)}"
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun calculateDDay(beginAt: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val beginDate = inputFormat.parse(beginAt)

            if (beginDate != null) {
                val currentTime = System.currentTimeMillis()
                val beginTime = beginDate.time
                val diffInMillis = beginTime - currentTime
                val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

                "투표시작 D-$diffInDays"
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String): OnePickRankingSubPageViewModel
    }
}

/**
 * 테마픽 카드 데이터
 */
data class ThemePickCardData(
    val id: Int,
    val state: ThemePickState,
    val title: String,
    val subTitle: String,
    val imageUrl: String,
    val voteCount: String,
    val periodDate: String
)

/**
 * 이미지픽 카드 데이터
 */
data class ImagePickCardData(
    val id: Int,
    val state: ImagePickState,
    val title: String,
    val subTitle: String,
    val imageUrl: String,
    val voteCount: String,
    val periodDate: String
)
