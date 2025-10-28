/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.listener

import android.content.Intent
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.UserModel

interface CommunityArticleListener {
    fun filterSetCallBack(bottomSheetFragment: BottomSheetFragment)
    fun filterClickCallBack(label: String, orderBy: String)
    fun heartClick(model: ArticleModel, position: Int)
    fun commentClick(model: ArticleModel, position: Int)
    fun likeClick(model: ArticleModel)
    fun viewMoreClick(model: ArticleModel, position: Int)
    fun feedClick(user: UserModel)
    fun editClick(intent: Intent)
    fun shareClick(model: ArticleModel)
    fun translationClick(model: ArticleModel, position: Int)
}