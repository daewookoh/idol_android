package net.ib.mn.utils

import android.content.Context
import net.ib.mn.model.IdolModel

fun sort(context: Context, idols: ArrayList<IdolModel>): ArrayList<IdolModel> {
    // 순위 계산
    idols.sortWith(Comparator { lhs, rhs ->
        when {
            lhs.heart > rhs.heart -> -1
            lhs.heart < rhs.heart -> 1
            else -> {
                lhs.getName(context).compareTo(rhs.getName(context))
            }
        }
    })

    for ((rank, i) in idols.indices.withIndex()) {
        val model: IdolModel = idols[i]
        model.rank = rank
        if (i > 0) {
            if (idols[i - 1].heart == model.heart) {
                model.rank = idols[i - 1].rank
            }
        }
    }

    return idols
}