package net.ib.mn.feature.generic

import net.ib.mn.model.IdolModel

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: chartId 사용하는 실시간 랭킹 화면 아이템 클릭 리스너
 *
 * */

interface GenericRankingClickListener {
    fun onItemClicked(item: IdolModel?)
    fun onVote(item: IdolModel)
}