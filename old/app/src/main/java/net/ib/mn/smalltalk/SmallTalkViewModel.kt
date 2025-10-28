/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 잡담 게시판 뷰모델.
 *
 * */

package net.ib.mn.smalltalk

import android.content.Context
import android.content.Intent
import android.os.Parcelable
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.article.ArticlesRepositoryImpl
import net.ib.mn.core.domain.usecase.LikeArticleUseCase
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.NoticeModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.getSerializableData
import net.ib.mn.utils.livedata.Event
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SmallTalkViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private var articlesRepository: ArticlesRepositoryImpl,
) : ViewModel() {
    @Inject
    lateinit var likeArticleUseCase: LikeArticleUseCase

    val idolModel: LiveData<IdolModel> = savedStateHandle.getLiveData(CommunityActivity.PARAM_IDOL)

    // 리사이클러뷰 스크롤 위치 바뀜 방지용
    var recyclerviewScrollState: Parcelable? = null

    // 다음 페이지가 없으면 null로 오게됨.
    private var nextResourceUrl: String? = null
    lateinit var startActivityResultLauncher: ActivityResultLauncher<Intent>
    private var offset = 0
    private var limit = 30
    private var totalCount = 0
    private var noticeCount = 0

    var presentSmallTalkArticleList = mutableListOf<ArticleModel>()

    private val _errorToast = MutableLiveData<Event<String>>()
    val errorToast: LiveData<Event<String>> = _errorToast

    private val _smallTalkArticleList = MutableLiveData<List<ArticleModel>>()
    val smallTalkArticleList: LiveData<List<ArticleModel>> = _smallTalkArticleList

    private val _articleAdd = MutableLiveData<Event<Boolean>>()
    val articleAdd: LiveData<Event<Boolean>> = _articleAdd

    fun getSmallTalkInventory(
        context: Context,
        orderby: String,
        type: String,
        keyWord: String?,
        locale: String?,
        isLoadMore: Boolean,
    ) {
        viewModelScope.launch {
            delay(1000L)
        }
        var isMost = false
        val account = IdolAccount.getAccount(context)
        if (account != null && account.userModel != null &&
            account?.userModel?.most != null &&
            account?.userModel?.most!!.getId() == idolModel.value?.getId()
        ) {
            isMost = true
        }

        val listener : (JSONObject) -> Unit = listener@ { response ->
            if (!response.optBoolean("success")) {
                val responseMsg = ErrorControl.parseError(context, response)
                responseMsg?.let {
                    _errorToast.value = Event(responseMsg)
                }
                return@listener
            }

            val gson = IdolGson.getInstance(true)

            totalCount = response.getJSONObject("meta")
                .optInt("total_count")
            nextResourceUrl = response.getJSONObject("meta")
                .optString("next")

            if (nextResourceUrl != null &&
                nextResourceUrl.equals("null")
            ) {
                nextResourceUrl = null
            }

            val listType = object : TypeToken<List<ArticleModel>>() {}.type
            val smallTalkArticleList =
                gson.fromJson<List<ArticleModel>>(
                    response.getJSONArray("objects").toString(),
                    listType,
                )

            presentSmallTalkArticleList.addAll(smallTalkArticleList.toMutableList())

            var newList = presentSmallTalkArticleList.distinctBy { it.id }
            presentSmallTalkArticleList.clear()

            if (newList.isEmpty() || newList[0].id != "-1") {
                val headerModel = ArticleModel()
                headerModel.id = "-1"
                presentSmallTalkArticleList.add(0, headerModel)
            }

            // 게시글 신고된 것 또는 차단된 사용자 게시글 안보이도록
            newList = newList.filter { it.user != null && UtilK.isArticleNotReported(context, it.id) && UtilK.isUserNotBlocked(context, it.user.id) }

            val noticeList = arrayListOf<ArticleModel>()
            if (!isLoadMore) {
                val jsonArray = response.optJSONArray("top_notices")
                val originNoticeList: List<NoticeModel> = if (jsonArray != null) {
                    val noticeListType = object : TypeToken<List<NoticeModel>>() {}.type
                    gson.fromJson(jsonArray.toString(), noticeListType)
                } else {
                    emptyList()
                }

                noticeCount = originNoticeList.size

                for (i in 0 until noticeCount) {
                    val noticeModel = ArticleModel()
                    noticeModel.id = originNoticeList[i].id.toString()
                    noticeModel.title = originNoticeList[i].title
                    noticeList.add(noticeModel)
                }
            }

            presentSmallTalkArticleList.addAll(noticeList + newList)

            _smallTalkArticleList.value = presentSmallTalkArticleList
        }

        val errorListener : (Throwable) -> Unit = { throwable ->
            _errorToast.value = Event(throwable.message ?: "")
        }

        if (isLoadMore) {
            nextResourceUrl?.let {
                viewModelScope.launch {
                    articlesRepository.getArticles(
                        it,
                        isMost ?: false,
                        keyWord,
                        null,
                        null,
                        listener = listener,
                        errorListener = errorListener
                    )
                }
            }
        } else {
            // 다음 페이징 소스가 없으므로 클리어 시켜줍니다.
            presentSmallTalkArticleList.clear()
            viewModelScope.launch {
                articlesRepository.getSmallTalkInventory(
                    idolModel.value?.getId() ?: 0,
                    isMost,
                    type,
                    orderby,
                    limit,
                    keyWord,
                    locale,
                    listener = listener,
                    errorListener = errorListener
                )
            }
        }
    }

    private fun getSmallTalkArticle(context: Context, articleModel: ArticleModel?, isEdit: Boolean) {
        viewModelScope.launch {
            articlesRepository.getArticle(
                (articleModel ?: return@launch).resourceUri,
                { response ->
                    try {
                        val gson = IdolGson.getInstance(true)
                        // 모델 업데이트.

                        val model = gson.fromJson(response.toString(), ArticleModel::class.java)

                        for (i in 0 until presentSmallTalkArticleList.size) {
                            if (presentSmallTalkArticleList[i].id == model.id) {
                                model.isEdit = !presentSmallTalkArticleList[i].isEdit
                                presentSmallTalkArticleList[i] = model
                                break
                            }
                        }
                        _smallTalkArticleList.value = presentSmallTalkArticleList
                        if (isEdit) {
                            _articleAdd.value = Event(true)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                { throwable ->
                    _errorToast.value = Event(throwable.message ?: "")
                }
            )
        }
    }

    private fun postSmallTalkLike(
        context: Context,
        articleModel: ArticleModel?,
    ) {
        if (articleModel != null) {
            viewModelScope.launch {
                likeArticleUseCase(articleModel.id, !articleModel.isUserLike).collect { response ->
                    if( !response.success ) {
                        response.message?.let {
                            _errorToast.value = Event(it)
                        }
                        return@collect
                    }

                    getSmallTalkArticle(context, articleModel, false)
                }
            }
        }
    }
    fun registerActivityResult(componentActivity: ComponentActivity, fragment: Fragment) {
        startActivityResultLauncher = fragment.registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult(),
        ) {
            try {
                val articleModel: ArticleModel? =
                    it.data?.getSerializableData<ArticleModel>(Const.EXTRA_ARTICLE)

                when (it.resultCode) {
                    ResultCode.EDITED.value -> {
                        getSmallTalkArticle(componentActivity, articleModel, true)
                    }
                    ResultCode.COMMENT_REMOVED.value,
                    -> { // 게시글 수정 시
                        getSmallTalkArticle(componentActivity, articleModel, false)
                    }

                    ResultCode.REMOVED.value -> { // 게시글 삭제 시
                        for (i in 0 until presentSmallTalkArticleList.size) {
                            if (presentSmallTalkArticleList[i].id == articleModel?.id) {
                                presentSmallTalkArticleList.removeAt(i)
                                break
                            }
                        }
                        _smallTalkArticleList.value = presentSmallTalkArticleList
                    }

                    ResultCode.BLOCKED.value,
                    ResultCode.SMALL_TALK_ADDED.value,
                    -> { // 게시글 업로드 시
                        _articleAdd.value = Event(true)
                    }
                    ResultCode.REPORTED.value -> { // 신고 됐을 경우 바로 안 보이도록 처리

                        for (i in 0 until presentSmallTalkArticleList.size) {
                            if (presentSmallTalkArticleList[i].id == articleModel?.id) {
                                presentSmallTalkArticleList.removeAt(i)
                                break
                            }
                        }

                        // article 차단 목록 추가
                        articleModel?.id?.let { it1 ->
                            UtilK.addArticleReport(componentActivity, it1)
                        }
                        _smallTalkArticleList.value = presentSmallTalkArticleList
                    }
                    ResultCode.ARTICLE_LIKE_EXCEPTION.value -> {
                        if (articleModel?.isUserLikeCache != articleModel?.isUserLike) { // 들어갈 때 내 UserLike와, setResult에서 보내온 Article.userLike가 다르다면 post
                            postSmallTalkLike(componentActivity, articleModel)
                        }
                    }
                    ResultCode.UPDATE_VIEW_COUNT.value, ResultCode.COMMENTED.value -> {
                        getSmallTalkArticle(componentActivity, articleModel, false)
                    }
                    ResultCode.UPDATE_LIKE_COUNT.value -> {
                        updateLikeCount(articleModel)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getNextResouceUrl(): String? {
        return nextResourceUrl
    }

    fun getNoticeCount(): Int {
        return noticeCount
    }

    private fun updateLikeCount(articleModel: ArticleModel?) {
        presentSmallTalkArticleList.find { it.id == articleModel?.id }?.apply {
            likeCount = articleModel?.likeCount ?: 0
            isEdit = articleModel?.isEdit ?: false
        }

        _smallTalkArticleList.value = presentSmallTalkArticleList
    }
}