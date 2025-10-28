/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: CommunityActivity 에서 사용하는 ViewModel.
 * CommunityActivity 자식 Fragment들에게 데이터 전달하는 역할 및 CommunityHeader 데이터 처리
 *
 * */

package net.ib.mn.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.data.repository.FavoritesRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.model.FavoriteModel
import net.ib.mn.model.IdolModel
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.utils.ApiCacheManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.Event
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class CommunityActivityViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val favoritesRepository: FavoritesRepository,
    private val usersRepository: UsersRepository,
) : BaseViewModel() {
    var idolModel: LiveData<IdolModel> = savedStateHandle.getLiveData(CommunityActivity.PARAM_IDOL)
    var idolAccount: IdolAccount? = null

    private var showChattingTab: Boolean = false
    private var dialogProfileThumb: String? = null
    private var isPurchasedDailyPack = false

    private var isFromUpload = false
    private var isMost: Boolean? = false
    private var mId: Int = 0
    private var isWallpaperInit: Boolean = false

    private val _articleWrite = MutableLiveData<Event<Boolean>>()
    val articleWrite: LiveData<Event<Boolean>> = _articleWrite

    private val _smallTalkWrite = MutableLiveData<Event<Boolean>>()
    val smallTalkWrite: LiveData<Event<Boolean>> = _smallTalkWrite

    private val _chattingRoomWrite = MutableLiveData<Event<Boolean>>()
    val chattingRoomWrite: LiveData<Event<Boolean>> = _chattingRoomWrite

    private val _scheduleWrite = MutableLiveData<Event<Boolean>>()
    val scheduleWrite: LiveData<Event<Boolean>> = _scheduleWrite

    private val _changeTop3 = MutableLiveData<Event<Boolean>>()
    val changeTop3: LiveData<Event<Boolean>> = _changeTop3

    private val _clickCommunityTab = MutableLiveData<Event<Boolean>>()
    val clickCommunityTab: LiveData<Event<Boolean>> = _clickCommunityTab

    private val _clickSmallTalkTab = MutableLiveData<Event<Boolean>>()
    val clickSmallTalkTab: LiveData<Event<Boolean>> = _clickSmallTalkTab

    private val _newCommunityIntent = MutableLiveData<Event<Boolean?>>()
    val newCommunityIntent: LiveData<Event<Boolean?>> = _newCommunityIntent

    private val _newSmallTalkIntent = MutableLiveData<Event<Boolean?>>()
    val newSmallTalkIntent: LiveData<Event<Boolean?>> = _newSmallTalkIntent

    private val _changeMost = MutableLiveData<Event<Boolean>>()
    val changeMost: LiveData<Event<Boolean>> = _changeMost

    private val _successBurningDay = MutableLiveData<Event<String>>()
    val successBurningDay: LiveData<Event<String>> = _successBurningDay

    private val _intentcategory = MutableLiveData<Event<String?>>()
    val intentcategory: LiveData<Event<String?>> = _intentcategory

    private val _newIntentcategory = MutableLiveData<Event<String?>>()
    val newIntentCategory: LiveData<Event<String?>> = _newIntentcategory

    private val _isRecent = MutableLiveData<Event<Boolean?>>()
    val isRecent: LiveData<Event<Boolean?>> = _isRecent

    private val _push = MutableLiveData<Event<Boolean?>>()
    val push: LiveData<Event<Boolean?>> = _push

    private val _pushType = MutableLiveData<Event<String?>>()
    val pushType: LiveData<Event<String?>> = _pushType

    private val _mostCount = MutableLiveData<Event<String>>()
    val mostCount: LiveData<Event<String>> = _mostCount

    private val _isWallpaper = MutableLiveData<Event<Boolean>>()
    val isWallpaper: LiveData<Event<Boolean>> = _isWallpaper
    fun handleIntent(intent: Intent) {
        setPush(intent.getBooleanExtra(CommunityActivity.PUSH, false))
        setIsRecent(intent.getBooleanExtra(CommunityActivity.PARAM_ARTICLE_RECENT, false))
        setIntentCategory(intent.getStringExtra(CommunityActivity.PARAM_CATEGORY))
        setIsWallpaper(intent.getBooleanExtra(CommunityActivity.PARAM_IS_WALLPAPER, false))

    }
    private fun setPush(push: Boolean?) {
        _push.value = Event(push)
    }

    fun setPushType(pushType: String?) {
        _pushType.value = Event(pushType)
    }

    private fun setIntentCategory(category: String?) {
        _intentcategory.value = Event(category)
    }

    fun setNewIntentCategory(category: String?) {
        _newIntentcategory.value = Event(category)
    }

    fun setIsRecent(isRecent: Boolean?) {
        isFromUpload = isRecent == true
        _isRecent.value = Event(isRecent)
    }

    fun setIdolAccount(context: Context) {
        idolAccount = IdolAccount.getAccount(context)
    }

    fun getIsMost() = isMost
    fun setIsMost() {
        isMost = idolAccount?.most?.getId() == idolModel.value?.getId()
    }

    private fun setIsWallpaper(isWallpaper: Boolean) {
        isWallpaperInit = isWallpaper
        _isWallpaper.value = Event(isWallpaper)
    }

    fun getIsWallpaperInit() = isWallpaperInit

    fun getMId() = mId
    fun setMId(mId: Int) {
        this.mId = mId
    }

    fun getIsFromUpload() = isFromUpload

    fun getDialogProfileThumb() = dialogProfileThumb

    fun setDialogProfileThumb(dialogProfileThumb: String?) {
        this.dialogProfileThumb = dialogProfileThumb
    }

    fun getPurchasedDailyPack() = isPurchasedDailyPack

    fun setPurchasedDailyPack(isPurchasedDailyPack: Boolean) {
        this.isPurchasedDailyPack = isPurchasedDailyPack
    }

    // 커뮤니티 게시글 생성
    fun articleWrite() {
        _articleWrite.value = Event(true)
    }

    // 아이돌게시판 생성
    fun smallTalkWrite() {
        _smallTalkWrite.value = Event(true)
    }

    fun recentCommunityArticle() {
        _newCommunityIntent.value = Event(true)
    }

    fun recentSmallTalkArticle() {
        _newSmallTalkIntent.value = Event(true)
    }

    // 채팅방 생성
    fun chattingRoomWrite() {
        _chattingRoomWrite.value = Event(true)
    }

    // 스케줄 생성
    fun scheduleWrite() {
        _scheduleWrite.value = Event(true)
    }

    fun changeTop3() {
        _changeTop3.value = Event(true)
    }

    fun clickTab(position: Int) {
        when(position) {
            0 -> _clickCommunityTab.value = Event(true)
            1 -> _clickSmallTalkTab.value = Event(true)
        }

    }

    fun loadFavorites(context: Context) {
        val response: JSONObject? = ApiCacheManager.getInstance().getCache(Const.KEY_FAVORITE)
        if (response == null) {
            viewModelScope.launch {
                favoritesRepository.getFavoritesSelf(
                    { response ->
                        if (response.optBoolean("success")) {
                            setFavorites(context, response)
                            // api caching
                            ApiCacheManager.getInstance()
                                .putCache(Const.KEY_FAVORITE, response, 60000 * 60)
                        } else {
                            UtilK.handleCommonError(context, response)
                        }
                    }, {
                        try {
                            Toast.makeText(
                                context,
                                R.string.error_abnormal_exception,
                                Toast.LENGTH_SHORT,
                            ).show()
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        } else {
            setFavorites(context, response)
        }
    }

    fun changeMost() {
        _changeMost.value = Event(true)
    }

    fun successBurningDay(burningDay: String) {
        _successBurningDay.value = Event(burningDay)
    }

    private fun setFavorites(context: Context, response: JSONObject) {
        try {
            val gson = IdolGson.getInstance()
            val array = response.getJSONArray("objects")
            for (i in 0 until array.length()) {
                val model = gson.fromJson(
                    array.getJSONObject(i).toString(),
                    FavoriteModel::class.java,
                )
                if (idolModel.value?.getName(context).equals(
                        model.idol?.getName(context),
                    )
                ) {
                    mId = array.getJSONObject(i).getInt("id")
                    idolModel.value?.isFavorite = true
                }
            }
            if (idolAccount?.most == null) {
                idolModel.value?.isMost = false
            } else {
                // account.getMost().setLocalizedName(CommunityActivity.this);
                if (idolModel.value?.getName(context).equals(
                        idolAccount?.most?.getName(context),
                    )
                ) {
                    idolModel.value?.isMost = true
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun isShowChattingTab() = showChattingTab

    //채팅방 탭 보여줄지 여부 return해주는 함수 (해당 아이돌이 내 최애이거나, 최애의 그룹일 경우, 관리자일 경우)
    fun setIsShowChattingTab() {
        showChattingTab = idolAccount?.userModel?.most?.getId() == idolModel.value?.getId()
            || idolAccount?.userModel?.most?.groupId == idolModel.value?.getId()
            || idolAccount?.heart == Const.LEVEL_ADMIN
    }

    fun setMostCount(context: Context) {
        // 비밀의 방일 때 제외
        if (!idolModel.value?.category.equals("B", ignoreCase = true)) {
            val mostCountLocale = Util.mostCountLocale(context, idolModel.value?.mostCount?:0)
            _mostCount.value = Event(mostCountLocale)
        }
    }

    fun updateTutorial(tutorialIndex: Int) = viewModelScope.launch {
        usersRepository.updateTutorial(
            tutorialIndex = tutorialIndex,
            listener = { response ->
                if (response.optBoolean("success")) {
                    Logger.d("Tutorial updated successfully: $tutorialIndex")
                    val bitmask = response.optLong("tutorial", 0L)
                    TutorialManager.init(bitmask)
                } else {
                    _errorToast.value = Event(response.toString())
                }
            },
            errorListener = { throwable ->
                _errorToast.value = Event(throwable.message ?: "Error updating tutorial")
            }
        )
    }
}