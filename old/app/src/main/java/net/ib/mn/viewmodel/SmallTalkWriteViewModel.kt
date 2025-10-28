/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import feature.common.exodusimagepicker.ExodusImagePickerRegister
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseWriteActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.dto.UpdateArticleDTO
import net.ib.mn.core.data.repository.FavoritesRepository
import net.ib.mn.core.data.repository.article.ArticlesRepositoryImpl
import net.ib.mn.core.model.TagModel
import net.ib.mn.core.model.UploadVideoSpecModel
import net.ib.mn.mapper.picker.toData
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.CommonFileModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.getSizeMB
import net.ib.mn.utils.ext.safeActivity
import net.ib.mn.utils.getModelFromPref
import net.ib.mn.utils.livedata.Event
import org.json.JSONException
import java.io.File
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SmallTalkWriteViewModel @Inject constructor (
    application: Application,
    private var articlesRepository: ArticlesRepositoryImpl,
    private var favoritesRepository: FavoritesRepository,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val _updateResponse = MutableLiveData<Event<Map<String, Any>>>()
    val updateResponse : LiveData<Event<Map<String, Any>>> = _updateResponse

    private val _modelList = MutableLiveData<Event<ArrayList<IdolModel>>>()
    val modelList : LiveData<Event<ArrayList<IdolModel>>> = _modelList

    private val _selectionTag = MutableLiveData<Event<TagModel?>>()
    val selectionTag : LiveData<Event<TagModel?>> = _selectionTag

    private val _getVideoFiles = MutableLiveData<Event<List<CommonFileModel>>>()
    val getVideoFiles: LiveData<Event<List<CommonFileModel>>> = _getVideoFiles

    private val _errorPopup = MutableLiveData<Event<String>>()
    val errorPopup: LiveData<Event<String>> = _errorPopup

    private var presentVideoFileModels: List<CommonFileModel> = listOf()

    lateinit var exodusImagePickerRegiser: ExodusImagePickerRegister

    lateinit var locale: Locale

    init {
        initLocale(application)
    }

    private fun initLocale(context: Context) {
        val systemLanguage = Util.getSystemLanguage(context)
        val ls: Array<String> = systemLanguage.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val locale = if (ls.size >= 2) {
            Locale(ls[0], ls[1])
        } else {
            Locale(systemLanguage)
        }
        this.locale = locale
    }

    fun updateArticles(context : Context, id : String?, articleModel: ArticleModel, show : String, selectionTagId : String){
        val articleId = id ?: return
        val dto = UpdateArticleDTO(
            articleId = articleId,
            title = articleModel.title,
            content = articleModel.content ?: "",
            linkTitle = articleModel.linkTitle,
            linkDesc = articleModel.linkDesc,
            linkUrl = articleModel.linkUrl,
            show = show,
            tagId = selectionTagId
        )
        viewModelScope.launch {
            articlesRepository.updateArticle(dto, { response ->
                Util.closeProgress()
                if (response.success) {
                    val article = ArticleModel()
                    article.content = articleModel.content
                    article.setIsMostOnly(show)

                    _updateResponse.value = Event(mapOf(
                        "gcode" to response.gcode
                        ,"heart" to response.provide))
                } else {
                    _updateResponse.value = Event(mapOf("gcode" to response.gcode))
                    val responseMsg = ErrorControl.parseError(context.safeActivity, response.gcode, response.msg)
                    if (responseMsg != null) {
                        Toast.makeText(context, responseMsg, Toast.LENGTH_SHORT).show()
                        _errorPopup.postValue(Event(responseMsg ?: ""))
                    }
                }
            }, { error ->
                _errorPopup.postValue(Event(error.message ?: ""))
            })

        }
    }

    fun getFavorite(context: Context, account: IdolAccount?){
        val modelList = ArrayList<IdolModel>()
        viewModelScope.launch {
            favoritesRepository.getFavoritesSelf(
                { response ->
                    val gson = IdolGson.getInstance()
                    if (response.optBoolean("success")) {
                        try {
                            val array = response.getJSONArray("objects")
                            account?.most?.let {
                                modelList.add(it)
                            }
                            for (i in 0 until array.length()) {
                                val model = gson.fromJson(
                                    array.getJSONObject(i)["idol"].toString(),
                                    IdolModel::class.java
                                )
                                if (account?.most?.getName(context) == model.getName(context)) continue
                                modelList.add(model)
                            }
                            modelList.sortBy { it.getName(context) }
                            _modelList.value = Event(modelList)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        val responseMsg = ErrorControl.parseError(context, response)
                        if (responseMsg != null) {
                            _errorPopup.postValue(Event(responseMsg ?: ""))
                        }
                    }
                }, { throwable ->
                    _errorPopup.postValue(Event(throwable.message ?: ""))
                }
            )
        }
    }

    fun setTagSelection(context: Context, articleModel: ArticleModel?) {
        var selectionTag: TagModel? = null
        val gson = IdolGson.getInstance()
        val listType = object : TypeToken<List<TagModel?>?>() {}.type
        val tags =
            gson.fromJson<List<TagModel>>(Util.getPreference(context, Const.BOARD_TAGS), listType)
        try {
            // default 값으로 세팅
            if (articleModel == null) {
                // 20240124 기본 카테고리 제거하고 선택하게 변경
//                for (tagModel in tags) {
//                    if (tagModel.id == 5) { // 태그가  피드는  id 가 5로 넘어오므로, id 5가 감지되면 -> 해당 값의 index의  tagmodel을  default로
//                        selectionTag = tags[tags.indexOf(tagModel)]
//                        break
//                    } else { // 5가 없으면 그냥  가장 마지막 index의 값을 기본으로
//                        selectionTag = tags[tags.size - 1]
//                    }
//                }
            } else {
                // 그냥 id에 따른  포지션으로 하니까  tag 바뀌는 현상있어서,  id값으로만 비교해서  넣어줌.
                for (i in tags.indices) {
                    if (tags[i].id == articleModel?.tagId) {
                        selectionTag = tags[i]
                        break
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            selectionTag = tags[0]
            e.printStackTrace()
        }
        _selectionTag.value = Event(selectionTag)
    }

    //비디오 선택화면에서 선택한 아이템을 가지고옴.
    fun registerVideoPickerResult(context: Activity) {
        //exodus 비디오 picker launcher
        exodusImagePickerRegiser = ExodusImagePickerRegister.registerForVideoPicker(
            context as BaseWriteActivity
        ) { fileModelList ->

            if (fileModelList == null) {
                return@registerForVideoPicker
            }
            presentVideoFileModels = fileModelList.map {
                it.toData()
            }

            val videoFile = presentVideoFileModels[0].trimFilePath?.let { File(it) }
            if (videoFile != null && videoFile.getSizeMB() > getVideoSpecModel(context).maxSizeMB) {
                _errorPopup.value = Event(
                    String.format(
                        context.getString(R.string.file_size_exceeded),
                        getVideoSpecModel(context).maxSizeMB.toString()
                    )
                )
                return@registerForVideoPicker
            }
            videoFile?.delete()

            _getVideoFiles.value = Event(presentVideoFileModels)
        }
    }

    fun getVideoSpecModel(context: Activity): UploadVideoSpecModel {
        val videoSpecPref = Util.getPreference(
            context,
            Const.PERF_UPLOAD_VIDEO_SPEC
        )

        return videoSpecPref.getModelFromPref<UploadVideoSpecModel>() ?: UploadVideoSpecModel()
    }
}