/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.listener

import net.ib.mn.model.ArticleModel

interface ArticlePhotoListener {
    fun widePhotoClick(model: ArticleModel, position: Int? = 0)
    fun linkClick(link: String)
}