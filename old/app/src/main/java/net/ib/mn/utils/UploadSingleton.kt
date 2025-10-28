/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: presignedRequestModelList, writeArticleModel, type 저장하는 SingleTon. intent로 해당 값들 넘기면 앱이 OOM나기 때문에 SingleTon으로 사용함.
 *
 * */

package net.ib.mn.utils

import net.ib.mn.model.PresignedRequestModel
import net.ib.mn.model.WriteArticleModel
import java.util.Locale

class UploadSingleton private constructor() {

    private var type: Int? = 0
    private var locale: String? = ""
    private var returnTo: Int = 0 // 글작성 완료 클릭시 돌아갈 곳
    private var tag: Int? = null // 프리톡으로 돌아갈 경우 선택할 태그
    private var presignedRequestModelList: ArrayList<PresignedRequestModel> = ArrayList()
    private var writeArticleModel: WriteArticleModel = WriteArticleModel()

    companion object {
        private var instance: UploadSingleton? = null

        fun getInstance(presignedRequestModel: ArrayList<PresignedRequestModel>, writeArticleModel: WriteArticleModel, type: Int, locale: String, returnTo: Int = 0, tag: Int? = null): UploadSingleton? {
            if (instance == null) {
                synchronized(this) {
                    instance = UploadSingleton()
                    instance!!.presignedRequestModelList = presignedRequestModel
                    instance!!.writeArticleModel = writeArticleModel
                    instance!!.type = type
                    instance!!.locale = locale
                    instance!!.returnTo = returnTo
                    instance!!.tag = tag
                }
            }
            return instance
        }

        fun getPresignedRequestModelList(): ArrayList<PresignedRequestModel>? {
            return instance?.presignedRequestModelList
        }

        fun getWriteArticleModel(): WriteArticleModel? {
            return instance?.writeArticleModel
        }

        fun getType(): Int? {
            return instance?.type
        }

        fun getReturnTo(): Int {
            return instance?.returnTo ?: 0
        }

        fun getTag(): Int? {
            return instance?.tag
        }

        // 게시글 업로드 성공하면 Service단에서 SingleTon clear 해줌으로서 다음 게시글 업로드 때 SingleTon instance 없게하기 위함
        fun clear() {
            instance = null
        }
        fun isLive(): Boolean {
            return instance != null
        }

        fun getLocale(): String? {
            return instance?.locale
        }

    }
}