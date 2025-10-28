package net.ib.mn.activity

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.repository.NoticeEventRepository
import net.ib.mn.link.BaseWebViewActivity
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.NoticeModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.link.LinkUtil
import org.json.JSONException
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class WebViewActivity : BaseWebViewActivity() {
    @Inject
    lateinit var noticeEventRepository: NoticeEventRepository

    private var type: String? = null
    private var id: Int? = null
    private var subTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        type = intent.extras?.getString(PARAM_TYPE)
        id = intent.extras?.getInt(PARAM_ID)
        subTitle = intent.extras?.getString(PARAM_SUB_TITLE)

        if( type == null || id == null ) {
            return
        }

        FLAG_CLOSE_DIALOG = false // AppLinkActivity가 destroy 되면서 팝업을 닫아버리는 것을 방지
        loadResource()

        binding.webview.settings.apply {
            // 기본 WebView 설정
            domStorageEnabled = true

            // 줌 설정 (핀치 줌 허용)
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // 텍스트 렌더링 보정
            textZoom = 103
            defaultFontSize = 18
        }


        binding.webview.setOnLongClickListener {
            val result = binding.webview.hitTestResult
            if (result.type == WebView.HitTestResult.IMAGE_TYPE ||
                result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

                val imageUrl = result.extra
                imageUrl?.let {
                    showDownloadDialog(it)
                }
                true
            } else {
                false
            }
        }
    }

    private fun showDownloadDialog(imageUrl: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.aos_web_image_save_title))
            .setMessage(getString(R.string.aos_web_image_save_body))
            .setPositiveButton(getString(R.string.aos_web_image_save_yes)) { _, _ ->
                downloadImage(imageUrl)
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun saveBase64Image(dataUrl: String, fileName: String = "image_from_webview.jpg") {
        try {
            val base64Data = dataUrl.substringAfter(",")
            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)

            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )

            FileOutputStream(file).use { it.write(imageBytes) }

            MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun downloadImage(url: String) {
        if (url.startsWith("data:image/")) {
            // base64 이미지 처리
            saveBase64Image(url)
        } else {
            // 일반 이미지 다운로드 처리
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, null, null))
                setMimeType("image/*")
            }
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        }
    }

    private fun loadResource() {
        lifecycleScope.launch {
            noticeEventRepository.get(type!!, id!!, Util.isDarkTheme(this@WebViewActivity),
                { response ->
                    binding.emptyView.visibility = View.GONE
                    Util.log(response.toString())
                    if (response.optBoolean("success")) {
                        try {
                            val obj = response.getJSONObject("object")
                            val gson = IdolGson.getInstance()
                            val em = gson.fromJson(obj.toString(), NoticeModel::class.java)
                            binding.text.text = em.title
                            if( !em.contentHtml.isNullOrEmpty() ) {
                                runOnUiThread {
                                    binding.webview.settings.apply {
                                        textZoom = 103
                                    }
                                    binding.webview.loadDataWithBaseURL(
                                        ServerUrl.HOST, em.contentHtml, "text/html; charset=utf-8",
                                        "UTF-8", null)
                                }
                            } else if( !em.content.isNullOrEmpty() ) {
                                // html 공지를 안올린 경우 대비
                                runOnUiThread {
                                    binding.webview.loadDataWithBaseURL(
                                        ServerUrl.HOST, em.content, "text/plain; charset=utf-8",
                                        "UTF-8", null)
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        Util.showIdolDialogWithBtn1(
                            this@WebViewActivity,
                            null,
                            response.optString("msg")
                        ) {
                            Util.closeIdolDialog()
                            finish()
                        }
                    }
                },
                { throwable ->
                    Toast.makeText(
                        this@WebViewActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    override fun shareLink() {
        if(type == Const.TYPE_NOTICE){
            setUiActionFirebaseGoogleAnalyticsActivity(
                GaAction.NOTICE_SHARE.actionValue,
                GaAction.NOTICE_SHARE.label
            )
            val url = LinkUtil.getAppLinkUrl(context = this, params = listOf(LinkStatus.NOTICES.status, id.toString()))
            UtilK.linkStart(context = this, url = url, msg = subTitle)
        }else {
            setUiActionFirebaseGoogleAnalyticsActivity(
                GaAction.EVENT_SHARE.actionValue,
                GaAction.EVENT_SHARE.label
            )
            val url = LinkUtil.getAppLinkUrl(context = this, params = listOf(LinkStatus.EVENTS.status, id.toString()))
            UtilK.linkStart(context = this, url = url, msg = subTitle)
        }
    }

    companion object {
        const val PARAM_TYPE = "type"
        const val PARAM_ID = "id"
        const val PARAM_SUB_TITLE = "sub_title"

        @JvmStatic
        fun createIntent(context: Context, type: String, id: Int, title: String?, subTitle : String? = "", isShowShare: Boolean = true): Intent {
            return Intent(context, WebViewActivity::class.java)
                .putExtra(PARAM_TYPE, type)
                .putExtra(PARAM_ID, id)
                .putExtra(PARAM_TITLE, title)
                .putExtra(PARAM_SUB_TITLE, subTitle)
                .putExtra(PARAM_IS_SHOW_SHARE , isShowShare)
        }
    }
}