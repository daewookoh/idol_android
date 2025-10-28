package net.ib.mn.utils

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: FavoriteSettingActivity 코틀린 변환 후 제거 예정
 *
 * */

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.domain.usecase.GetAllIdolsUseCase
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toPresentation

object TempUtil {
    fun doGetAllIdols(
        activity: AppCompatActivity,
        getAllIdolsUseCase: GetAllIdolsUseCase,
        callback: (ArrayList<IdolModel>) -> Unit
    ) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val idols = getAllIdolsUseCase()
                .mapListDataResource { it.toPresentation() }
                .awaitOrThrow()

            withContext(Dispatchers.Main) {
                callback(ArrayList(idols ?: listOf()))
            }
        }
    }
}