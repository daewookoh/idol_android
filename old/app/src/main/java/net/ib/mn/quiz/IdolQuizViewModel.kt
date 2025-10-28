/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.quiz

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseAndroidViewModel
import net.ib.mn.core.data.model.QuizTodayModel
import net.ib.mn.core.data.repository.QuizRepositoryImpl
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.core.domain.usecase.GetQuizTodayUseCase
import net.ib.mn.model.IdolModel
import net.ib.mn.model.QuizCategoryModel
import net.ib.mn.model.QuizReviewModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Util
import net.ib.mn.utils.livedata.Event
import org.json.JSONException
import javax.inject.Inject

/**
 * @see
 * */

@HiltViewModel
class IdolQuizViewModel @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val quizTodayUseCase: GetQuizTodayUseCase
    ) : BaseAndroidViewModel(application) {
    private val _showAdoptedMyNewQuiz = MutableLiveData<Event<Boolean>>()
    val showAdoptedMyNewQuiz: LiveData<Event<Boolean>> = _showAdoptedMyNewQuiz

    private var quizTodayModel: QuizTodayModel? = null
    val getQuizTodayModel: LiveData<QuizTodayModel?> =
        savedStateHandle.getLiveData(STATE_KEY_OF_QUIZ_TODAY)

    private val _finishActivity = MutableLiveData<Event<Boolean>>()
    val finishActivity: LiveData<Event<Boolean>> = _finishActivity

    private val _goQuizReviewActivity =
        MutableLiveData<Event<Map<Int, ArrayList<QuizReviewModel>>>>()
    val goQuizReviewActivity: LiveData<Event<Map<Int, ArrayList<QuizReviewModel>>>> =
        _goQuizReviewActivity

    private val _getQuizTypeList = MutableLiveData<Event<ArrayList<QuizCategoryModel>>>()
    val getQuizTypeList: LiveData<Event<ArrayList<QuizCategoryModel>>> = _getQuizTypeList

    private val _getIdolModelList = MutableLiveData<Event<ArrayList<IdolModel>>>()
    val getIdolModelList: LiveData<Event<ArrayList<IdolModel>>> = _getIdolModelList

    private val _successOfQuizPlus = MutableLiveData<Event<Boolean>>()
    val successOfQuizPlus: LiveData<Event<Boolean>> = _successOfQuizPlus

    @Inject
    lateinit var quizRepository: QuizRepositoryImpl
    @Inject
    lateinit var idolsRepository: IdolsRepository

    fun getQuizMyOwn(context: Context) {
        viewModelScope.launch {
            quizRepository.getAcceptedQuiz(
                { response ->
                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response)
                        _errorToast.value = Event(responseMsg ?: "")
                        return@getAcceptedQuiz
                    }
                    try {
                        if ((response.optJSONObject("meta")?.getInt("total_count") ?: 0) > 0) {
                            _showAdoptedMyNewQuiz.value = Event(true)
                            return@getAcceptedQuiz
                        }
                        _showAdoptedMyNewQuiz.value = Event(false)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                { throwable ->
                    if (Util.is_log()) {
                        _errorToast.value = Event(throwable.message ?: "")
                    }
                }
            )
        }
    }

    fun getQuizToday(context: Context) {
        viewModelScope.launch {
            try {
                quizTodayUseCase().collect { response ->
                    if( !response.success) {
                        response.msg?.let {
                            _errorToast.value = Event(it)
                        }
                        return@collect
                    }

                    quizTodayModel = response
                    savedStateHandle[STATE_KEY_OF_QUIZ_TODAY] = quizTodayModel
                }
            } catch (e: Exception) {
                _errorToast.value = Event(e.message ?: "")
            }
        }
    }

    fun getQuizReview(mostId: Int, context: Context) {
        val debug: String? = if (BuildConfig.DEBUG) {
            when (Util.getSystemLanguage(context)) {
                "ko_KR" -> "ko"
                "zh_CN" -> "zh-cn"
                "zh_TW" -> "zh-tw"
                "ja_JP" -> "ja"
                else -> "en"
            } + ",$mostId"
        } else {
            null
        }

        viewModelScope.launch {
            quizRepository.getQuizReviewList(
                0,
                debug,
                { response ->
                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response)
                        _errorToast.value = Event(responseMsg ?: "")
                        _finishActivity.value = Event(true)
                        return@getQuizReviewList
                    }
                    try {
                        val quizzes = response.getInt("quizzes")

                        val gson = IdolGson.getInstance()

                        // 심사 리스트 저장.
                        val quizReviewArray = response.getJSONArray("objects").toString()
                        val listType = object : TypeToken<ArrayList<QuizReviewModel>>() {}.type
                        val reviewModels =
                            gson.fromJson<ArrayList<QuizReviewModel>>(quizReviewArray, listType)

                        _goQuizReviewActivity.value = Event(mapOf(quizzes to reviewModels))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }                },
                { throwable ->
                    if (Util.is_log()) {
                        _errorToast.value = Event(throwable.message ?: "")
                    }
                    _finishActivity.value = Event(true)
                }
            )
        }
    }

    fun getQuizTypeList(context: Context) {
        viewModelScope.launch {
            quizRepository.getQuizTypeList(
                lambda@ { response ->
                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response)
                        _errorToast.value = Event(responseMsg ?: "")
                        _finishActivity.value = Event(true)
                        return@lambda
                    }

                    val quizTypeArray = (response.optJSONArray("objects") ?: "").toString()

                    val listType = object :
                        com.google.common.reflect.TypeToken<ArrayList<QuizCategoryModel>>() {}.type
                    val quizTypes: ArrayList<QuizCategoryModel> =
                        IdolGson.getInstance().fromJson(quizTypeArray, listType)

                    _getQuizTypeList.value = Event(quizTypes)
                    Util.setPreference(
                        context,
                        Const.PREF_QUIZ_TYPE_LIST,
                        IdolGson.getInstance().toJson(quizTypes),
                    )
                },
                { throwable ->
                    if (Util.is_log()) {
                        _errorToast.value = Event(throwable.message ?: "")
                    }
                    _finishActivity.value = Event(true)
                }
            )
        }
    }

    fun getIdolGroupList(context: Context, idolAccount: IdolAccount?) {
        viewModelScope.launch {
            idolsRepository.getGroupsForQuiz(
                { response ->
                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response)
                        _errorToast.value = Event(responseMsg ?: "")
                        return@getGroupsForQuiz
                    }
                    try {
                        val gson = IdolGson.getInstance()
                        val array = response.getJSONArray("objects")
                        var most: IdolModel? = null
                        val idolModelList: ArrayList<IdolModel> = ArrayList()

                        for (i in 0 until array.length()) {
                            val model = gson.fromJson(
                                array.getJSONObject(i).toString(),
                                IdolModel::class.java,
                            )
                            if (idolAccount?.most == null || model.groupId != idolAccount?.most?.groupId) {
                                idolModelList.add(model)
                            } else {
                                most = model
                            }
                        }

                        idolModelList.sortBy { it.getName(context) }

                        if (idolAccount?.most != null && idolAccount?.most?.type?.equals(
                                "B",
                                ignoreCase = true,
                            ) != true
                        ) {
                            if (most != null) idolModelList.add(0, most)
                        }

                        _getIdolModelList.value = Event(idolModelList)

                        Util.setPreference(
                            context,
                            Const.PREF_QUIZ_IDOL_LIST,
                            IdolGson.getInstance().toJson(idolModelList),
                        )
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }, {}
            )
        }
    }

    fun requestQuizPlus(context: Context) {
        viewModelScope.launch {
            quizRepository.plusQuizChallenge(
                { response ->
                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response)
                        _errorToast.value = Event(responseMsg ?: "")
                        return@plusQuizChallenge
                    }

                    _successOfQuizPlus.value = Event(true)
                },
                { throwable ->
                    if (Util.is_log()) {
                        _errorToast.value = Event(throwable.message ?: "")
                    }
                }
            )
        }
    }

    companion object {
        const val STATE_KEY_OF_QUIZ_TODAY = "quiz_today"
    }
}