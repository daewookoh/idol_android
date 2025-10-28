/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: picker 모듈 데이터 매퍼.
 *
 * */

package net.ib.mn.mapper.picker

import feature.common.exodusimagepicker.model.FileModel
import net.ib.mn.model.CommonFileModel

internal fun FileModel.toData(): CommonFileModel = CommonFileModel(
    id = id,
    contentUri = contentUri,
    name = name,
    duration = duration,
    mimeType = mimeType,
    relativePath = relativePath,
    isVideoFile = isVideoFile,
    isSelected = isSelected,
    thumbnailImage = thumbnailImage,
    startTimeMills = startTimeMills,
    endTimeMills = endTimeMills,
    totalVideoDuration = totalVideoDuration,
    content = content,
    mediaType = mediaType,
    trimFilePath = trimFilePath
)