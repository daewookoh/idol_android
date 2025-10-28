package net.ib.mn.feature.rookiehistory

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import net.ib.mn.model.CharityModel
import net.ib.mn.model.SuperRookie

@Stable
interface RookieHistoryUiState {
    @Immutable
    object Loading : RookieHistoryUiState
    @Immutable
    data class Success(val charityModels: List<CharityModel>, val superRookieModels: List<SuperRookie>) : RookieHistoryUiState
    @Immutable
    data class Error(val message: String) : RookieHistoryUiState
}