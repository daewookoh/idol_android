/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 셀럽 카테고리 관련 확장함수
 *
 * */

package net.ib.mn.utils.ext

import android.content.Context
import net.ib.mn.R
import net.ib.mn.core.data.model.TypeListModel


/**
 * @see
 * */

fun TypeListModel.getTypeCheck(): String? {
    return if (isDivided == "N" && !isFemale) {
        null
    } else {
        if (!isFemale) {
            "M"
        } else {
            "F"
        }
    }
}

fun TypeListModel.getShareLinkCategoryText(context: Context?): String? {
    return when (this.type) {
        "S" -> {
            if (this.getTypeCheck() == "F") {
                context?.getString(R.string.actor_female_singer)
            } else {
                context?.getString(R.string.actor_male_singer)
            }
        }

        "A" -> {
            if (this.getTypeCheck() == "F") {
                context?.getString(R.string.lable_actresses)
            } else {
                context?.getString(R.string.lable_actors)
            }
        }

        else -> {
            this.name.ifEmpty {
                context?.getString(R.string.celeb)
            }
        }
    }
}