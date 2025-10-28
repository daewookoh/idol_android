/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.viewmodel

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley
import com.google.android.gms.ads.admanager.AdManagerAdView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.admanager.AdManager
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.data.repository.article.ArticlesRepositoryImpl
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.datastore.GetIsEnableVideoAdPrefsUseCase
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.common.util.appendVersion
import net.ib.mn.utils.getKSTMidnightEpochTime
import net.ib.mn.utils.livedata.Event
import net.ib.mn.utils.permission.PermissionHelper

class BaseWidePhotoViewModel (
    private val articlesRepository: ArticlesRepositoryImpl,
    private val getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase,
) : BaseViewModel() {

    var articleModel:ArticleModel = ArticleModel()

    private var downloadQueueId: Long = 0

    private val _adManagerAdView = MutableLiveData<Event<AdManagerAdView>>()
    val adManagerAdView : LiveData<Event<AdManagerAdView>> = _adManagerAdView
    private val _moveScreenToVideo = MutableLiveData<Event<Boolean>>()
    val moveScreenToVideo: LiveData<Event<Boolean>> = _moveScreenToVideo

    fun articleModel(model: ArticleModel){
        articleModel = model
    }

    val downloadCompleteBroadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val dm = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadQueueId == reference) {
                val query = DownloadManager.Query() // 다운로드 항목 조회에 필요한 정보 포함
                query.setFilterById(reference)
                val cursor: Cursor = dm.query(query)
                cursor.moveToFirst()
                val columnIndex: Int = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status: Int = cursor.getInt(columnIndex)
                cursor.close()
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> Toast.makeText(context, context.getString(R.string.msg_save_ok), Toast.LENGTH_SHORT).show()
                    DownloadManager.STATUS_FAILED -> Toast.makeText(context, context.getString(R.string.msg_unable_use_download_2), Toast.LENGTH_SHORT).show()
                }
                Util.closeProgress()
            }
        }
    }

    // 서버에 있는 파일 그대로 다운로드한다 (png->jpg로 변환되는 현상을 방지)
    fun downloadImage(context: Context, url: String ) {
        Util.showProgress(context)

        val uri = Uri.parse(url)
        val orgFilename = uri.lastPathSegment ?: return
        val ext = orgFilename.substring(orgFilename.lastIndexOf(".")) // . 포함
        val filename = "${Const.DOWNLOAD_FILE_PATH_PREFIX}${Util.getFileDate("yyyy-MM-dd-HH-mm-ss")}${ext}"
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(uri)
            .setTitle(filename)
            .setAllowedOverMetered(true)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

        downloadQueueId = dm.enqueue(request)
    }

    fun isGifFromArticleModel(): Boolean {
        return articleModel.imageUrl?.contains(".gif")
            ?: articleModel.thumbnailUrl?.contains("gif") ?: false
    }

    fun getImageUrl(position: Int = 0, isGif: Boolean = false): String? {
        val url = if(articleModel.files.isNullOrEmpty()) { // 이붙그램인 경우
            if(articleModel.imageUrl == null){
                articleModel.umjjalUrl
            }else{
                if(isGif) {
                    articleModel.imageUrl?.appendVersion(articleModel.imageVer)
                } else {
                    articleModel.umjjalUrl ?: articleModel.imageUrl?.appendVersion(articleModel.imageVer) // 기록실-기적-이미지 저장시 imageUrl만 있음
                }
            }
        }else {
            if(articleModel.files[position].originUrl == null) { //옛날에 올린 커뮤 게시글인 경우
                articleModel.imageUrl?.appendVersion(articleModel.imageVer)
            } else {
                articleModel.files[position].originUrl?.appendVersion(articleModel.imageVer)
            }
        }

        return url
    }

    fun requestPermission(fragment: Fragment?, activity: Activity?, targetSdkVersion: Int, position: Int =0, isGifDownload: Boolean = true) {
        if(activity == null || fragment == null) {
            return
        }
        // android 6.0 처리
        // 방통위 규제사항 처리
        // TODO: 2023/06/27 targetsdk 33부터  권한 세분화로 IMAGE,VIDEO 권한 추가해줌.
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= 30) {//버전 29이상일때는 permission read external storage 만  넣어줘도됨
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {//29  미만일때는 WRITE_EXTERNAL_STORAGE 허용  요거는 read external 까지  같이 처리해줌.
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

        val msgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(activity.getString(R.string.permission_storage), "")
        } else {
            arrayOf(activity.getString(R.string.permission_storage))
        }

        PermissionHelper.requestPermissionIfNeeded(activity, null, permissions,
            msgs, BaseActivity.REQUEST_WRITE_EXTERNAL_STORAGE,
            object : PermissionHelper.PermissionListener {
                override fun onPermissionAllowed() {
                    Logger.v("mingue","articleModel files -> "+ articleModel.files)

                    val mUrl = getImageUrl(position, isGifDownload)

                    if (fragment.isAdded) {
                        try {
                            if (mUrl != null) {
                                downloadImage(activity, mUrl)
                            } else {
                                throw Exception("mUrl is null")
                            }
                        } catch (e: Exception) {
                            Util.closeProgress()
                            _errorToast.postValue(Event(activity.getString(R.string.msg_unable_use_download_2)))
                            e.printStackTrace()
                        }
                    }
                }

                override fun onPermissionDenied() {}
                override fun requestPermission(permissions: Array<String>) { }
            })
    }

    // 7.28.2023 AdMob제거후 AdManager로 변경.
    fun addAdManagerView(context: Context?) {
        val account = IdolAccount.getAccount(context) ?: return
        var shouldShowAd = true

        // 구독 여부 판단
        if (account.userModel?.subscriptions != null
            && account.userModel?.subscriptions!!.isNotEmpty()) {
            for (mySubscription in account.userModel?.subscriptions!!) {
                if (mySubscription.familyappId == 1 || mySubscription.familyappId == 2) {
                    if (mySubscription.skuCode == Const.STORE_ITEM_DAILY_PACK) {
                        shouldShowAd = false
                    }
                }
            }
        }

        if (shouldShowAd && !BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.let { adManagerView ->
                if (adManagerView.parent != null) {
                    (adManagerView.parent as ViewGroup).removeView(adManagerView)
                }
                _adManagerAdView.value = Event(adManagerView)
            }
        }
    }

    fun downloadCount(context: Context?, articleId: Long, seq: Int) {
        viewModelScope.launch {
            articlesRepository.downloadArticle(articleId, seq)
        }
    }

    fun viewCountArticle(context: Context?, articleId: Long) {
        viewModelScope.launch {
            articlesRepository.viewCount(articleId)
        }
    }

    fun moveScreenToVideo() = viewModelScope.launch(Dispatchers.IO){
        val isEnabled = getIsEnableVideoAdPrefsUseCase()
            .mapDataResource { it }
            .awaitOrThrow()
        _moveScreenToVideo.postValue(Event(isEnabled ?: true))
    }
}

class BaseWidePhotoViewModelFactory(
    private val context: Context,
    private val articlesRepository: ArticlesRepositoryImpl,
    private val getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BaseWidePhotoViewModel::class.java)) {
            return BaseWidePhotoViewModel(articlesRepository, getIsEnableVideoAdPrefsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}