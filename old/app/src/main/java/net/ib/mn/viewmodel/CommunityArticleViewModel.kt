/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: CommunityFragment 에서 사용하는 ViewModel.
 * 다 옮기지 못했고 CommunityFragment 값들 여기에 옮길 예정
 *
 * */

package net.ib.mn.viewmodel

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.FeedActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.article.ArticlesRepositoryImpl
import net.ib.mn.core.domain.usecase.LikeArticleUseCase
import net.ib.mn.fragment.FeedActivityFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.NoticeModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.getSerializableData
import net.ib.mn.utils.ext.safeActivity
import net.ib.mn.utils.livedata.Event
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class CommunityArticleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private var articlesRepository: ArticlesRepositoryImpl
) : ViewModel() {
    @Inject
    lateinit var likeArticleUseCase: LikeArticleUseCase
    @Inject
    lateinit var usersRepository: UsersRepository

    lateinit var startActivityResultLauncher: ActivityResultLauncher<Intent>
    var articleList: MutableList<ArticleModel> = mutableListOf()
    var noticeList = emptyList<NoticeModel>()
    var resourceUri : String? = null

    var nextResourceUrl: String? = null
    var mDisableLoadNextResource = AtomicBoolean(false)
    var prevScrollY = 0

    private var mOrderBy: String = "-heart"

    private var imageOnly: String? = null

    private var wallpaperOnly: Boolean = false

    private val primaryFileType = "wp"

    private val _articleAdd = MutableLiveData<Event<Boolean>>()
    val articleAdd: LiveData<Event<Boolean>> = _articleAdd

    private val _editArticle = MutableLiveData<Event<ArticleModel>>()
    val editArticle: LiveData<Event<ArticleModel>> = _editArticle

    private val _articleRemove = MutableLiveData<Event<ArticleModel>>()
    val articleRemove: LiveData<Event<ArticleModel>> = _articleRemove

    private val _feedArticleModify = MutableLiveData<Event<Boolean>>()
    val feedArticleModify: LiveData<Event<Boolean>> = _feedArticleModify

    private val _getArticleOnlyOne = MutableLiveData<Event<String>>()
    val getArticleOnlyOne: LiveData<Event<String>> = _getArticleOnlyOne

    private val _articleBlocked = MutableLiveData<Event<Int>>()
    val articleBlocked: LiveData<Event<Int>> = _articleBlocked

    private val _voteHeart = MutableLiveData<Event<Map<String, Any>>> ()
    val voteHeart: LiveData<Event<Map<String, Any>>>  =_voteHeart

    private val _updateItem = MutableLiveData<Event<ArticleModel>>()
    val updateItem: LiveData<Event<ArticleModel>> = _updateItem

    private val _getCommunityArticles = MutableLiveData<Event<Boolean>>()
    val getCommunityArticles: LiveData<Event<Boolean>> = _getCommunityArticles

    fun registerActivityResult(componentActivity: ComponentActivity, fragment: Fragment) {
        startActivityResultLauncher = fragment.registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult(),
        ) {
            try {
                val articleModel: ArticleModel? = it.data?.getSerializableData(Const.EXTRA_ARTICLE)
                when (it.resultCode) {
                    RESULT_OK -> {
                        _articleAdd.value = Event(true)
                    }
                    ResultCode.VOTED.value,
                    ResultCode.COMMENT_REMOVED.value,
                    FeedActivityFragment.FEED_ARTICLE_MODIFY,
                    ResultCode.EDITED.value,
                    -> {
                        val resourceUri = it.data?.getStringExtra("resource_uri")
                        if (resourceUri != null) {
                            _getArticleOnlyOne.value = Event(resourceUri)
                            return@registerForActivityResult
                        }

                        _editArticle.value = Event(articleModel ?: return@registerForActivityResult)
                    }
                    ResultCode.REMOVED.value,
                    FeedActivityFragment.FEED_ARTICLE_REMOVE,
                    ResultCode.REPORTED.value,
                    -> {
                        if (articleModel != null) {
                            _articleRemove.value = Event(articleModel)
                        }
                    }
                    FeedActivityFragment.FEED_ARTICLE_MODIFY -> {
                        _feedArticleModify.value = Event(true)
                    }
                    ResultCode.BLOCKED.value -> {
                        val blockedUserId = it.data?.getIntExtra(FeedActivity.PARAM_USER_ID, 0) ?: 0

                        if (blockedUserId <= 0) {
                            return@registerForActivityResult
                        }

                        _articleBlocked.value = Event(blockedUserId)
                    }
                    ResultCode.UPDATE_LIKE_COUNT.value -> {
                        if (articleModel != null) {
                            _getArticleOnlyOne.value = Event(articleModel.resourceUri)
                        }
                    }
                    ResultCode.ARTICLE_LIKE_EXCEPTION.value -> {
                        postArticleLike(fragment.requireContext(), articleModel, true)
                    }
                    ResultCode.COMMENTED.value, ResultCode.UPDATE_VIEW_COUNT.value -> {
                        _updateItem.value = Event(articleModel ?: return@registerForActivityResult)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun postArticleLike(
        context: Context?,
        articleModel: ArticleModel?,
        isException: Boolean
    ) {
        if (articleModel != null) {
            viewModelScope.launch {
                likeArticleUseCase(articleModel.id, !articleModel.isUserLike).collect { response ->
                    articleModel.isUserLike = response.liked
                    if(isException) {
                        _getArticleOnlyOne.value = Event(articleModel.resourceUri)
                    }
                }
            }
        }
    }

    fun confirmUseApi(context: Context?, model: ArticleModel, position: Int) {
        viewModelScope.launch {
            usersRepository.isActiveTime(
                { response ->
                    Util.closeProgress()
                    if (!response.optBoolean("success")) {
                        UtilK.handleCommonError(context, response)
                        return@isActiveTime
                    }
                    val gcode = response.optInt("gcode")
                    if (response.optString("active") == Const.RESPONSE_Y) {
                        if (response.optInt("total_heart") == 0) {
                            Util.showChargeHeartDialog(context)
                        } else {
                            if (response.optString("vote_able")
                                    .equals(Const.RESPONSE_Y, ignoreCase = true)
                            ) {
                                _voteHeart.postValue(
                                    Event(
                                        mapOf(
                                            "model" to model,
                                            "position" to position,
                                            "total_heart" to response.optLong("total_heart"),
                                            "free_heart" to response.optLong("free_heart")
                                        )
                                    ))
                            } else {
                                if (gcode == Const.RESPONSE_IS_ACTIVE_TIME_1) {
                                    Toast.makeText(
                                        context,
                                        context?.getString(
                                            R.string.response_users_is_active_time_over,
                                        ),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context?.getString(R.string.msg_not_able_vote),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        }
                        return@isActiveTime
                    }
                    val start = Util.convertTimeAsTimezone(
                        response.optString("begin"),
                    )
                    val end = Util.convertTimeAsTimezone(
                        response.optString("end"),
                    )
                    val unableUseTime = context?.getString(R.string.msg_unable_use_vote)?.let {
                        String.format(
                            it,
                            start,
                            end,
                        )
                    }

                    Util.showIdolDialogWithBtn1(
                        context,
                        null,
                        unableUseTime,
                    ) { Util.closeIdolDialog() }
                }, {
                    Util.closeProgress()
                    Toast.makeText(
                        context,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            )
        }
    }

    fun postArticleLike(context: Context?, model: ArticleModel) {
        if(model.isUserLikeCache == model.isUserLike) {
            return
        }

        (context?.safeActivity as BaseActivity).setUiActionFirebaseGoogleAnalyticsActivity(GaAction.COMMUNITY_LIKE.actionValue, GaAction.COMMUNITY_LIKE.label)

        viewModelScope.launch {
            val result = likeArticleUseCase(model.id, !model.isUserLike).first()
            model.isUserLike = result.liked
        }
    }

    // 특정 아티클 업데이트용
    fun loadArticleOnlyOne(context: Context?, resourceUri: String) {
        Util.showProgress(context)

        viewModelScope.launch {
            articlesRepository.getArticle(
                resourceUri,
                { response ->
                    Util.closeProgress()

                    try {
                        val gson = IdolGson.getInstance(true)
                        val model = gson.fromJson(response.toString(), ArticleModel::class.java)

                        val show: String = if (model.isMostOnly == "Y") {
                            Const.SHOW_PRIVATE
                        } else {
                            Const.SHOW_PUBLIC
                        }


                        // 현재 게시글들 중 업데이트할거 찾기
                        val articleModel = articleList.find { it.id == model.id }?.apply {
                            heart = model.heart
                            commentCount = model.commentCount
                            content = model.content
                            linkDesc = model.linkDesc
                            linkTitle = model.linkTitle
                            linkUrl = model.linkUrl
                            likeCount = model.likeCount
                            isUserLike = model.isUserLike
                            isUserLikeCache = model.isUserLikeCache
                            setIsMostOnly(show)
                        }

                        if (articleModel != null) {
                            _updateItem.value = Event(articleModel)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, {
                    Util.closeProgress()
                    Toast.makeText(
                        context,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            )
        }
    }

    // 커뮤니티 게시글 가져오는 함수
    fun getCommunityArticles(context: Context?, idolModel: IdolModel?, isMost: Boolean? = false, loadNextResource: Boolean = false) {
        Util.showProgress(context)
        val listener : (JSONObject) -> Unit = { response ->
            viewModelScope.launch(Dispatchers.IO) {
                if (!response.optBoolean("success")) {
                    UtilK.handleCommonError(context, response)
                    return@launch
                }

                val gson = IdolGson.getInstance(true)
                try {
                    nextResourceUrl = response.getJSONObject("meta").optString(
                        "next", null,
                    )
                    if (nextResourceUrl != null) {
                        if (nextResourceUrl.equals("null")) {
                            nextResourceUrl = null
                        }
                    }
                    val array = response.getJSONArray("objects")

                    val listType: Type? = object : TypeToken<ArrayList<ArticleModel>>() {}.type
                    val articles: ArrayList<ArticleModel> =
                        gson.fromJson(array.toString(), listType)

                    if (!loadNextResource) {
                        articleList.clear()
                    }

                    if (!loadNextResource) {
                        val jsonArray = response.optJSONArray("top_notices")
                        noticeList = if (jsonArray != null) {
                            val noticeListType = object : TypeToken<List<NoticeModel>>() {}.type
                            gson.fromJson(jsonArray.toString(), noticeListType)
                        } else {
                            emptyList()
                        }
                    }

                    if(context != null) {
                        for (model in articles) {
                            if(UtilK.isUserNotBlocked(context, model.user?.id) && UtilK.isArticleNotReported(context, model.id)) {
                                articleList.add(model)
                            }
                        }
                    }

                    // 커뮤 첫 진입시 또는 갱신시 움짤 바로 재생되게
                    withContext(Dispatchers.Main) {
                        _getCommunityArticles.value = Event(loadNextResource)
                    }

                    mDisableLoadNextResource.set(false)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }

        val errorListener : (Throwable) -> Unit = {
            viewModelScope.launch(Dispatchers.Main) {
                Util.closeProgress()
                Toast.makeText(
                    context,
                    R.string.error_abnormal_exception,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        if (!loadNextResource) {
            nextResourceUrl = null

            viewModelScope.launch {
                articlesRepository.getArticles(
                    idolModel?.getId() ?: 0,
                    isMost ?: false,
                    mOrderBy,
                    null,
                    null,
                    if(getWallpaperOnly()) primaryFileType else null,
                    getImageOnly(),
                    listener = listener,
                    errorListener = errorListener
                )
            }
            return
        }

        if (nextResourceUrl == null) {
            mDisableLoadNextResource.set(false)
            return
        }

        viewModelScope.launch {
            articlesRepository.getArticles(
                nextResourceUrl!!,
                isMost ?: false,
                null,
                null,
                if(getWallpaperOnly()) primaryFileType else null,
                getImageOnly(),
                listener = listener,
                errorListener = errorListener
            )
        }

        FeedActivity.USER_BLOCK_CHANGE = false
    }

    fun getOrderBy() = mOrderBy
    fun setOrderBy(orderBy: String?) {
        mOrderBy = if(orderBy.isNullOrEmpty()) {
            "-heart"
        } else {
            orderBy
        }
    }

    private fun getImageOnly() = imageOnly

    private fun setImageOnly(imageOnly: Boolean) {
        if(imageOnly) {
            this.imageOnly = IMAGE_ONLY
        } else {
            this.imageOnly = NOT_IMAGE_ONLY
        }
    }

    private fun getWallpaperOnly() = wallpaperOnly

    private fun setWallpaperOnly(wallpaperOnly: Boolean) {
        this.wallpaperOnly = wallpaperOnly
    }

    fun setArticleStatus(context: Context?, idolModel: IdolModel?, isMost: Boolean?, imageOnly: Boolean, wallpaperOnly: Boolean) {
        setImageOnly(imageOnly)
        setWallpaperOnly(wallpaperOnly)
        getCommunityArticles(context, idolModel, isMost)
    }

    companion object {
        const val IMAGE_ONLY = "Y"
        const val NOT_IMAGE_ONLY = "N"
    }
}