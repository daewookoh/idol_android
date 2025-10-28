package net.ib.mn.feature.mission

import net.ib.mn.model.WelcomeMissionResultModel

interface MissionUiState {
    object Loading: MissionUiState
    data class Success(
        val data: WelcomeMissionResultModel
    ): MissionUiState
    data class ERROR(
        val message: String
    ): MissionUiState
}