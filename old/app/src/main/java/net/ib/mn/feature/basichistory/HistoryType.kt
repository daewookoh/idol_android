package net.ib.mn.feature.basichistory

import net.ib.mn.R

enum class HistoryType(val titleRes: Int, val historyCalcType: HistoryCalcType) {
    MOST_TOP_100(R.string.stats_highest_votes, HistoryCalcType.TICKET),
    COUNT_RANK_1(R.string.stats_1st_place, HistoryCalcType.COUNT)
}

enum class HistoryCalcType {
    COUNT, TICKET
}