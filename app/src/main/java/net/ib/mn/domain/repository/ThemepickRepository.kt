package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ThemePickModel

interface ThemepickRepository {
    /**
     * 테마픽 목록 조회
     * @param offset 페이지 offset
     * @param limit 페이지 limit
     */
    fun getThemePickList(offset: Int, limit: Int): Flow<ApiResult<List<ThemePickModel>>>

    /**
     * 특정 테마픽 상세 조회
     * @param id 테마픽 ID
     */
    fun getThemePick(id: Int): Flow<ApiResult<ThemePickModel>>
}
