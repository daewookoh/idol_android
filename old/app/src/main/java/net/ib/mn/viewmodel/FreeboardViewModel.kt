package net.ib.mn.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.ib.mn.addon.IdolGson
import net.ib.mn.common.util.logE
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.datastore.GetFreeBoardPrefsUseCase
import net.ib.mn.domain.usecase.datastore.SetFreeBoardSelectLangPrefsUseCase
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.FreeBoardPrefsModel
import net.ib.mn.model.NoticeModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.Event
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class FreeboardViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository,
    private val getFreeBoardPrefsUseCase: GetFreeBoardPrefsUseCase,
    private val setFreeBoardSelectLangPrefsUseCase: SetFreeBoardSelectLangPrefsUseCase
) : ViewModel() {

    private val _filterByPopularity = MutableLiveData<Event<Boolean>>()
    val filterByPopularity: LiveData<Event<Boolean>> get() = _filterByPopularity
    private val _smallTalkCount = MutableLiveData<Event<Int>>()
    val smallTalkCount: LiveData<Event<Int>> get() = _smallTalkCount
    private val _smallTalkArticleList =
        MutableSharedFlow<Triple<Boolean, List<ArticleModel>, List<NoticeModel>>>(replay = 0)
    val smallTalkArticleList = _smallTalkArticleList.asSharedFlow()
    private val _freeBoardPrefs = MutableStateFlow<FreeBoardPrefsModel?>(null)
    val freeBoardPrefs: StateFlow<FreeBoardPrefsModel?> = _freeBoardPrefs
    private val _errorToast = MutableLiveData<Event<String>>()
    val errorToast: LiveData<Event<String>> = _errorToast

    private var nextResourceUrlForSmallTalk: String? = null
    var presentSmallTalkArticleList = mutableListOf<ArticleModel>()

    fun filterByPopularity(isPopular: Boolean) {
        _filterByPopularity.postValue(Event(isPopular))
    }

    fun toggleFilterByPopularity() {
        _filterByPopularity.postValue(Event(!_filterByPopularity.value!!.peekContent()))
    }

    fun initializeCacheData() {
        Log.i("FreeBoardPrefs", "initializeCacheData called")
        viewModelScope.launch {
            val prefs = try {
                getFreeBoardPrefsUseCase()
                    .mapDataResource { it?.toPresentation() }
                    .awaitOrThrow()
            } catch (e: Exception) {
                logE("FreeBoardPrefs", "prefs 가져오기 실패", "")
                null
            }
            _freeBoardPrefs.value = prefs
        }
    }

    fun setFreeBoardSelectLanguage(language: String?, languageId: String) {
        viewModelScope.launch {
            viewModelScope.launch {
                runCatching {
                    setFreeBoardSelectLangPrefsUseCase(language, languageId).collect {}
                }.onFailure {
                    logE("if", "저장 실패 $it")
                }
            }
        }
    }

    fun getSmallTalkInventory(
        context: Context,
        idolId: Int,
        orderBy: String,
        keyWord: String?,
        locale: String?,
        isLoadMore: Boolean,
    ) {
        val listener: (JSONObject) -> Unit = listener@{ response ->
            Util.closeProgress()

            if (!response.optBoolean("success")) {
                val responseMsg = ErrorControl.parseError(context, response)
                responseMsg?.let {
                    _errorToast.value = Event(responseMsg)
                }
                return@listener
            }

            val gson = IdolGson.getInstance(true)

            // 총 개수 처리
            val metaObject = response.getJSONObject("meta")
            val totalCount = metaObject.optInt("total_count")
            _smallTalkCount.postValue(Event(totalCount))

            // next URL 처리
            nextResourceUrlForSmallTalk = metaObject.optString("next").takeIf { it != "null" }

            // 게시글 리스트 파싱
            val listType = object : TypeToken<List<ArticleModel>>() {}.type
            val smallTalkArticleList = gson.fromJson<List<ArticleModel>>(
                response.getJSONArray("objects").toString(),
                listType
            )

            // 차단 및 신고 필터
            val filteredArticles = smallTalkArticleList.filter { model ->
                UtilK.isUserNotBlocked(context, model.user?.id) &&
                    UtilK.isArticleNotReported(context, model.id)
            }

            // 공지 리스트 파싱 (isLoadMore가 아닐 때만)
            val noticeList: List<NoticeModel> = if (!isLoadMore) {
                response.optJSONArray("top_notices")?.let { jsonArray ->
                    val noticeListType = object : TypeToken<List<NoticeModel>>() {}.type
                    gson.fromJson(jsonArray.toString(), noticeListType)
                } ?: emptyList()
            } else {
                emptyList()
            }

            // 현재 데이터 업데이트
            if (isLoadMore) {
                presentSmallTalkArticleList.addAll(filteredArticles)
            } else {
                presentSmallTalkArticleList.clear()
                presentSmallTalkArticleList.addAll(filteredArticles)
            }

            // 최종 필터링 (중복 제거 + 신고/차단 필터 재적용)
            val distinctFilteredList = presentSmallTalkArticleList
                .distinctBy { it.id }
                .filter { it.user != null && UtilK.isArticleNotReported(context, it.id) && UtilK.isUserNotBlocked(context, it.user.id) }

            presentSmallTalkArticleList.clear()
            presentSmallTalkArticleList.addAll(distinctFilteredList)

            // 데이터 방출
            viewModelScope.launch {
                _smallTalkArticleList.emit(
                    Triple(isLoadMore, distinctFilteredList.toList(), noticeList)
                )
            }
        }

        val errorListener : (Throwable) -> Unit = { throwable ->
            Util.closeProgress()
            _errorToast.value = Event(throwable.message ?: "")
        }


        if (isLoadMore) {
            nextResourceUrlForSmallTalk?.let {
                viewModelScope.launch {
                    articlesRepository.getArticles(
                        it,
                        true,
                        keyWord,
                        null,
                        null,
                        null,
                        listener = listener,
                        errorListener = errorListener
                    )
                }
            }
        } else {
            presentSmallTalkArticleList.clear()
            viewModelScope.launch {
                articlesRepository.getSmallTalkInventory(
                    idolId,
                    true,
                    "M",
                    orderBy,
                    30,
                    keyWord,
                    locale,
                    listener = listener,
                    errorListener = errorListener
                )
            }
        }
    }
}
