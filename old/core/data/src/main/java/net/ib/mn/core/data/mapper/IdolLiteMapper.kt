package net.ib.mn.core.data.mapper

import net.ib.mn.core.data.model.IdolLiteResponse
import net.ib.mn.core.model.IdolLiteModel

internal fun IdolLiteResponse.toData() : IdolLiteModel =
    IdolLiteModel(
        groupId = this.groupId,
        id = this.id,
        imageUrl = this.imageUrl ?: "",
        name = this.name,
        nameEN = this.nameEN,
        nameJP = this.nameJP,
        nameZH = this.nameZH,
        nameZHTW = this.nameZHTW
    )