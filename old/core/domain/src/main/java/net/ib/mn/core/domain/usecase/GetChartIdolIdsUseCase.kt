/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author parkboo parkboo@myloveidol.com
 * Description:
 *
 * */

package net.ib.mn.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.model.ChartModel
import net.ib.mn.core.data.repository.charts.ChartsRepository
import net.ib.mn.core.data.repository.config.ConfigRepository
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.model.SupportAdTypeListModel
import org.json.JSONObject
import javax.inject.Inject


/**
 * @see
 * */

class GetChartIdolIdsUseCase @Inject constructor(
    private val repository: ChartsRepository
) {
    suspend operator fun invoke(code: String): Flow<BaseModel<List<Int>>> =
        repository.getChartIdolIds(code)

}