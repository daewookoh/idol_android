package net.ib.mn.feature.basichistory

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import net.ib.mn.model.CharityModel
import net.ib.mn.model.HallModel
import net.ib.mn.model.SuperRookie

@Stable
interface BasicHistoryUiState {
    @Immutable
    object Loading : BasicHistoryUiState
    @Immutable
    data class Success(val dataMap: Map<String, ArrayList<HallModel>>) : BasicHistoryUiState
    @Immutable
    data class Error(val message: String) : BasicHistoryUiState
}