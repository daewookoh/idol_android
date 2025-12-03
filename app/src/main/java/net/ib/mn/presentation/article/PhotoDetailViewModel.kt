package net.ib.mn.presentation.article

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.domain.model.ArticleFile
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.ServerUrl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val DOWNLOAD_FILE_PREFIX = "IDOLCHAMP_"

/**
 * PhotoDetailViewModel - 사진 상세 화면 ViewModel
 */
@HiltViewModel
class PhotoDetailViewModel @Inject constructor() : ViewModel() {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<ToastEvent>()
    val toastEvent: SharedFlow<ToastEvent> = _toastEvent.asSharedFlow()

    /**
     * 이미지 다운로드 (DownloadManager 사용)
     *
     * @param context Context
     * @param media 다운로드할 미디어
     * @param articleId 게시글 ID (다운로드 카운트 API용)
     * @param mediaIndex 미디어 인덱스 (다운로드 카운트 API용, 1부터 시작)
     */
    fun downloadMedia(
        context: Context,
        media: ArticleFile,
        articleId: String?,
        mediaIndex: Int
    ) {
        viewModelScope.launch {
            try {
                _downloadState.value = DownloadState.Downloading

                // 다운로드 URL 결정
                // - 비디오/GIF: originUrl 또는 playableUrl (umjjalUrl)
                // - 이미지: originUrl 또는 originalUrl (originUrl ?: fileUrl)
                val downloadUrl = when {
                    media.isVideo -> media.originUrl
                    media.isGif -> media.originUrl ?: media.umjjalUrl
                    else -> media.originUrl ?: media.fileUrl ?: media.thumbnailUrl
                }
                if (downloadUrl.isNullOrEmpty()) {
                    _downloadState.value = DownloadState.Error
                    _toastEvent.emit(ToastEvent.DownloadError)
                    return@launch
                }

                val secureUrl = downloadUrl.toSecureUrl()

                withContext(Dispatchers.IO) {
                    downloadWithManager(context, secureUrl)
                }

                _downloadState.value = DownloadState.Success
                _toastEvent.emit(ToastEvent.DownloadSuccess)

            } catch (e: Exception) {
                e.printStackTrace()
                _downloadState.value = DownloadState.Error
                _toastEvent.emit(ToastEvent.DownloadError)
            }
        }
    }

    /**
     * DownloadManager를 사용한 다운로드
     */
    private fun downloadWithManager(context: Context, url: String) {
        val uri = Uri.parse(url)
        val orgFilename = uri.lastPathSegment ?: "image.jpg"
        val ext = if (orgFilename.contains(".")) {
            orgFilename.substring(orgFilename.lastIndexOf("."))
        } else {
            ".jpg"
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val filename = "$DOWNLOAD_FILE_PREFIX${dateFormat.format(Date())}$ext"

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(uri)
            .setTitle(filename)
            .setDescription("Downloading...")
            .setAllowedOverMetered(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

        dm.enqueue(request)
    }

    /**
     * 게시글 공유
     */
    fun shareArticle(context: Context, article: ArticleModel) {
        val locale = LocaleUtil.getWikiLocale(context)
        val shareUrl = "${ServerUrl.HOST}/articles/${article.id}/?locale=$locale"

        // 공유 메시지: 내용 30자 + 아이돌 이름
        val contentPreview = article.content?.take(30)?.trim() ?: ""
        val idolName = article.idol?.let { LocaleUtil.getLocalizedIdolName(context, it) } ?: ""

        val shareMsg = buildString {
            if (contentPreview.isNotEmpty()) {
                append(contentPreview)
                if ((article.content?.length ?: 0) > 30) append("...")
            }
            if (idolName.isNotEmpty()) {
                if (isNotEmpty()) append(" - ")
                append(idolName)
            }
        }

        val shareText = if (shareMsg.isNotEmpty()) {
            "$shareMsg\n$shareUrl"
        } else {
            shareUrl
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, null)
        )
    }
}

/**
 * 다운로드 상태
 */
sealed interface DownloadState {
    data object Idle : DownloadState
    data object Downloading : DownloadState
    data object Success : DownloadState
    data object Error : DownloadState
}

/**
 * 토스트 이벤트
 */
sealed interface ToastEvent {
    data object DownloadSuccess : ToastEvent
    data object DownloadError : ToastEvent
}
