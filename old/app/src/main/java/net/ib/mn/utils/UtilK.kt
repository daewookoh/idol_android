package net.ib.mn.utils

import android.app.Activity
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.LocaleList
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.Display
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import net.ib.mn.common.util.updateQueryParameter
import androidx.core.text.HtmlCompat
import com.addisonelliott.segmentedbutton.SegmentedButton
import com.addisonelliott.segmentedbutton.SegmentedButtonGroup
import com.android.volley.VolleyError
import com.applovin.sdk.AppLovinSdkUtils.runOnUiThread
import com.google.android.gms.common.util.Base64Utils
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.reflect.TypeToken
import com.tapjoy.TJConnectListener
import com.tapjoy.Tapjoy
import com.tapjoy.TapjoyConnectFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.BaseActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.addon.InternetConnectivityManager
import net.ib.mn.chatting.ChattingRoomActivity
import net.ib.mn.chatting.chatDb.ChatRoomList
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.data.repository.MessagesRepository
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.comments.CommentsRepository
import net.ib.mn.core.model.AwardModel
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.domain.usecase.GetIdolsByTypeAndCategoryUseCase
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.HallModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.IdolTypeModel
import net.ib.mn.model.QuizCategoryModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.link.LinkUtil
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.Date
import java.util.Hashtable
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern
import kotlin.system.exitProcess

class UtilK {

    companion object {

        //노티 지워주는용.
        fun deleteChatNotification(roomId: Int, context: Context) {

            //현재방의 채팅  notification 삭제
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(ChattingRoomActivity.ID_CHATTING_MSG + roomId)

            //statusbar array는 m 버전 이상부터지만,  isGroup 여부를 체크 하려면  n버전 이상 부터여서 그냥  n버전으로  넣음
            val statusBarNotifications: Array<StatusBarNotification> =
                notificationManager.activeNotifications
            val chatNotiCount: Int =
                statusBarNotifications.count { it.notification.group == Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW }
            if (chatNotiCount == 1 && statusBarNotifications[0].isGroup
                && (Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW == statusBarNotifications[0].notification.group)
            ) {
                notificationManager.cancel(Const.NOTIFICATION_GROUP_ID_CHAT_MSG)
            } else {
                //푸시를 받고  앱을 완전히 종료 하는 경우 ->  해당  그룹 노티가 null 로와서 안지워지는 경우가 있는데 이경우는 그냥 그룹 노티 삭제
                if (chatNotiCount == 1 && (statusBarNotifications[0].notification.group == null)) {
                    notificationManager.cancel(Const.NOTIFICATION_GROUP_ID_CHAT_MSG)
                }
            }
            //n 버전  미만은 애초에 그룹핑을 지원하지 않으므로, 그냥  해당 방  노티만  지워주면 된다.
        }

        //text 공백 제거용  함수
        fun removeWhiteSpace(text: String): String {
            return text.trim().replace(" ", "")
        }


        //뷰의 마진을 코드로 설정하기 위한  기능
        fun setMargins(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int
        ) {
            if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                val p = view.layoutParams as ViewGroup.MarginLayoutParams
                p.setMargins(left, top, right, bottom)
                view.requestLayout()
            }
        }


        //텍스트뷰 텍스트에  imagespan 적용 .
        fun TextView.addImage(
            atText: String,
            @DrawableRes imgSrc: Int,
            imgWidth: Int,
            imgHeight: Int
        ) {
            val ssb = SpannableStringBuilder(this.text)


            val drawable = ContextCompat.getDrawable(this.context, imgSrc) ?: return
            drawable.mutate()
            drawable.setBounds(
                0, 0,
                imgWidth,
                imgHeight
            )
            val start = text.indexOf(atText)
            ssb.setSpan(
                VerticalImageSpan(drawable),
                start,
                start + atText.length,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
            this.setText(ssb, TextView.BufferType.SPANNABLE)
        }

        //editext 키보드 option버튼 눌렀을때  특정뷰  click 실행되게 하는  기능
        fun EditText.doEditorAction(view: View) {
            this.setOnEditorActionListener(object : TextView.OnEditorActionListener {
                override fun onEditorAction(
                    v: TextView?,
                    actionId: Int,
                    event: KeyEvent?
                ): Boolean {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_SEND) {
                        view.performClick()
                        return true
                    }
                    return false
                }
            })
        }

        //기부 천사, 요정, 이달의 기적 체크해서 프로필 테두리 변경
        @JvmStatic
        fun profileRoundBorder(
            miracleCount: Int,
            fairyCount: Int,
            angelCount: Int,
            photoBorder: AppCompatImageView
        ) {
            var flag = 0
            if (miracleCount >= 1) flag += 1
            if (fairyCount >= 1) flag += 2
            if (angelCount >= 1) flag += 4

            val profileRoundList = arrayOf(
                R.drawable.profile_round_off,
                R.drawable.profile_round_miracle,
                R.drawable.profile_round_fairy,
                R.drawable.profile_round_fairy_miracle,
                R.drawable.profile_round_angel,
                R.drawable.profile_round_angel_miracle,
                R.drawable.profile_round_angel_fairy,
                R.drawable.profile_round_angel_fairy_miracle
            )

            when (flag) {
                flag -> {
                    photoBorder.setImageResource(profileRoundList[flag])
                }
            }
        }


        //video 금지 타이머 끝나는 시간
        private var videoDisableEndTime = -1L

        //video 타이머
        private var videoActivateTimer: CountDownTimer? = null
        private var simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())


        @JvmStatic
        fun videoDisableTimer(
            context: Context,
            tvDialogTimer: AppCompatTextView?,
            ivVideoAbleRedDot: ImageView?,
            freePlusHeartContainer: LinearLayoutCompat?
        ) {
            try {
                //shared에 저장된  비디오 disable 끝나는 시간 가져옴.
                videoDisableEndTime = Util.getPreferenceLong(
                    context,
                    Const.VIDEO_TIMER_END_TIME_PREFERENCE_KEY,
                    Const.DEFAULT_VIDEO_DISABLE_TIME
                )

                //타이머가 새롭게 만들어지는거 방지를 위해 초기화 시켜줌.
                videoActivateTimer?.cancel()
                videoActivateTimer = null

                //현재시간과  disable 끝나는 시간의  diff를 계산후,  해당 diff가 0 이되면 onFinish를 부름.
                val currentTime = System.currentTimeMillis()
                val diff: Long = videoDisableEndTime - currentTime

                videoActivateTimer = object : CountDownTimer(diff, 1000) {
                    override fun onFinish() {
                        runOnUiThread {

                            //무료 충전소화면에서는  타이머  사라지게 해준다.
                            //그외 다이얼로그에서는 00:00:00을 유지시켜줌.
                            if (freePlusHeartContainer != null) {
                                tvDialogTimer?.visibility = View.GONE
                            }

                            freePlusHeartContainer?.visibility = View.VISIBLE
                            ivVideoAbleRedDot?.visibility = View.VISIBLE

                            videoDisableEndTime = Const.DEFAULT_VIDEO_DISABLE_TIME
                            Util.setPreference(
                                context,
                                Const.VIDEO_TIMER_END_TIME_PREFERENCE_KEY,
                                videoDisableEndTime
                            )
                        }
                    }

                    override fun onTick(p0: Long) {
                        runOnUiThread {

                            tvDialogTimer?.visibility = View.VISIBLE
                            ivVideoAbleRedDot?.visibility = View.INVISIBLE
                            freePlusHeartContainer?.visibility = View.GONE


                            val millToSecond = p0 / 1000
                            val timerFormat = String.format(
                                "%02d:%02d",
                                (millToSecond % 3600) / 60,
                                millToSecond % 60
                            )

                            simpleDateFormat.format(p0)
                            tvDialogTimer?.text = timerFormat
                        }
                    }
                }.start()

            } catch (e: Exception) {//혹시 몰라서  try catch 로 감싸고  exception시  timer  null 처리
                videoActivateTimer = null
                e.printStackTrace()
            }
        }

        //서포트 액션바에  padding 없앨때 (24v이상부터 들어감)  직접  뷰를 적용하는데
        //이때  들어가는 공통적이 setting  확장 함수로 빼놓음.
        fun Toolbar.setCustomActionBar(context: AppCompatActivity, titleRes: Int) {
            context.setSupportActionBar(this)
            context.supportActionBar?.setTitle(titleRes)
            context.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        //뷰의 padding 을  상대적 관계로 (start end 개념)으로 줌
        fun View.setRelativePadding(
            startPadding: Float,
            topPadding: Float,
            endPadding: Float,
            bottomPadding: Float
        ) {
            this.setPaddingRelative(
                Util.convertDpToPixel(context, startPadding).toInt(),
                Util.convertDpToPixel(context, topPadding).toInt(),
                Util.convertDpToPixel(context, endPadding).toInt(),
                Util.convertDpToPixel(context, bottomPadding).toInt()
            )
        }

        fun getEmoticonId(url: String): String {
            val index = url.lastIndexOf("/")
            val dot = url.lastIndexOf(".")
            return url.substring(index + 1, dot)
        }

        fun getEmoticonSetId(url: String): String {
            val index = url.lastIndexOf("/")
            val dot = url.lastIndexOf("_")
            return url.substring(index + 1, dot)
        }

        fun getEmoticonTransId(url: String): String {
            val index = url.lastIndexOf("_")
            val dot = url.lastIndexOf(".")
            return url.substring(index + 1, dot)
        }

        //자리수 000 인경우에는 k 로
        //자리수 000,000 인 경우에는 m으로
        //자리수 000,000,000 인 경우에는 B로 변환해서 보여준다.
        //소수점 1의 자리까지는 반올림 처리하여 보여주는데 소수점 1의자리가 0이면 안보여주기 위해 DecimalFormat 을 사용함
        fun convertNumberToKMB(value: Long?): String {
            return if (value != null) {
                when {
                    value >= 1E9 -> "${DecimalFormat("#.#").format((value.toFloat() / 1E9))}B"
                    value >= 1E6 -> "${DecimalFormat("#.#").format((value.toFloat() / 1E6))}M"
                    value >= 1E3 -> "${DecimalFormat("#.#").format((value.toFloat() / 1E3))}K"
                    else -> NumberFormat.getInstance().format(value)
                }
            } else {//혹시나 null 이면  0으로 리턴 해줌.
                NumberFormat.getInstance().format(0)
            }
        }

        //파라미터로 받은 mills 형태의 시간을  얼마나 지남의 string으로  변환해서  return 해준다.
        fun convertMillsToPassedString(passedTimeMills: Long): String {
            val millToSecond = passedTimeMills / 1000
            return when {
                (millToSecond / 3600) > 0 -> {
                    String.format("%01d시간 전", millToSecond / 3600)
                }

                ((millToSecond % 3600) / 60) > 0 -> {
                    String.format("%01d분 전", (millToSecond % 3600) / 60)
                }

                else -> {//0~59초일때는 지금으로 보여줌.
                    "지금"
                }
            }
        }

        fun timeBefore(tempDate: Date, context: Context): String {
            val curTime = System.currentTimeMillis()
            val regTime = tempDate.time
            var diffTime = (curTime - regTime) / 1000
            var msg = ""

            val SEC = 60
            val MIN = 60
            val HOUR = 24

            if (diffTime != 0L && diffTime < SEC) {
                return context.getString(R.string.time_just_now)
            }
            diffTime = (diffTime / SEC)
            if (diffTime != 0L && (diffTime / SEC) < MIN) msg =
                diffTime.toString() + context.getString(R.string.time_minute)
            diffTime = (diffTime / MIN)
            if (diffTime != 0L && (diffTime / MIN) < HOUR) msg =
                (diffTime.toString()) + context.getString(R.string.time_hour)
            diffTime = (diffTime / HOUR)
            if (diffTime != 0L) msg = (diffTime.toString()) + context.getString(R.string.time_day)
            return msg
        }

        //url 에서 =같은게  encoded 되어서  %3d 등등으로 보일때 있는데
        //이때 해당 url을  decode 해준다.
        fun urlDecode(url: String): Uri {
            return URLDecoder.decode(url, "UTF-8").toUri()
        }

        //공유기능 사용할떄 언어 가져오는 함수.
        fun getShareLocale(context: Context): String {
            var locale = Util.getSystemLanguage(context)

            //중국어는 하이픈들어가고, 나머지는 앞에껏만 가져옴 ko_KR 일떄 ko만가져옴.
            locale = when (locale) {
                "zh_CN" -> "zh-cn"
                "zh_TW" -> "zh-tw"
                "ko_KR", "ja_JP" -> {
                    val splitedLocale = locale.split("_")
                    splitedLocale[0]
                }
                else -> {
                    "en"
                }
            }

            return locale
        }

        // 이모티콘  키보드 height 리아시즈 시킴
        fun resizeEmoticonKeyBoardHeight(
            context: AppCompatActivity,
            rootView: View,
            emoticonLayout: ConstraintLayout
        ) {

            val visibleFrameSize = Rect()

            val rootHeight = intArrayOf(-1)
            rootView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {

                override fun onGlobalLayout() {
                    if (rootHeight[0] == -1) {
                        if (context is ChattingRoomActivity) {//채팅룸일경우  supportactionbar 를 따로 안쓰고 툴바를 적용해놔서 root height  분기해서 사용함.
                            rootHeight[0] = rootView.height
                        } else {
                            rootHeight[0] = rootView.height + (context.supportActionBar?.height
                                ?: 112)
                        }
                    }

                    rootView.getWindowVisibleDisplayFrame(visibleFrameSize)
                    val heightExpectedBoard = visibleFrameSize.bottom - visibleFrameSize.top

                    val keyboardheight = rootHeight[0] - heightExpectedBoard//현재 키보드 길이
                    if (keyboardheight != 0) {
                        if (keyboardheight > Const.MINIMUM_EMOTICON_KEYBOARD_HEIGHT) {
                            //아니면 키보드 높이 가져와서 이모티콘창 높이계산.
                            val params = emoticonLayout.layoutParams
                            params.height = keyboardheight
                            emoticonLayout.layoutParams = params//키보드  높이 갱신
                        }

                        if (Util.getPreferenceInt(
                                context,
                                Const.KEYBOARD_HEIGHT,
                                -1
                            ) != keyboardheight
                        ) {
                            if (keyboardheight > Const.MINIMUM_EMOTICON_KEYBOARD_HEIGHT) {
                                Util.setPreference(
                                    context,
                                    Const.KEYBOARD_HEIGHT,
                                    keyboardheight
                                )//새로운  키보드 사이즈 캐싱
                            }
                        } else {

                            //새롭게 키보드 캐싱할 필요 없으면,  global layout remove
                            rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }

                }

            })
        }

        //깊은 복사 적용하기 위해 만듬
        fun deepCopy(model: TypeListModel): TypeListModel {
            return model.copy()
        }

        @JvmStatic
        fun initSetMainCheck(context: Context?) {
            val model = ArrayList<IdolTypeModel>()
            val typeListPref = Util.getPreference(context, Const.PREF_TYPE_LIST)
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<List<TypeListModel?>?>() {}.type
            val typeList =
                gson.fromJson<ArrayList<TypeListModel>>(typeListPref, listType) //서버에서 주는 타입리스트

            if (Util.getPreference(context, Const.MAIN_CHECK_VIEW).isNullOrEmpty()) {
                for (i in 0 until typeList.size) {
                    model.add(
                        IdolTypeModel(
                            i,
                            typeList[i].type,
                            typeList[i].isDivided,
                            typeList[i].isFemale,
                            false,
                            true,
                            typeList[i].isViewable
                        )
                    )  // is_devided = Y && isFemale = N -> 남자,  id_devided = N && isFemale = Y -> 여자 , 둘다 N인 것들은 구별안하는 값
                }
                Util.setPreference(context, Const.MAIN_CHECK_VIEW, gson.toJson(model))
            } else {
                val idolListType = object : TypeToken<List<IdolTypeModel?>?>() {}.type
                val idolTypeList = gson.fromJson<ArrayList<IdolTypeModel>>(
                    Util.getPreference(
                        context,
                        Const.MAIN_CHECK_VIEW
                    ), idolListType
                )

                //저장되어있는 idolTypeModel의 is_divided의 값이 TypeListModel의 is_divided가 아닌, IdolTypeModel의 default값이 들어가 있다면(8.4.0 -> 8.4.1 업데이트 할때만 나옴)
                //또는 타입리스트 사이즈와 프레그먼트 on/off 상태 저장해주는 preference 크기가 다르면 on/off 상태 저장해주는 프리퍼런스 재생성
                if (idolTypeList.any { it.is_divided == "D" } || typeList.size != idolTypeList.size || (typeList.count { it.isViewable == "Y" } != idolTypeList.count { it.is_viewable == "Y" })) {
                    for (i in 0 until typeList.size) {
                        model.add(
                            IdolTypeModel(
                                i,
                                typeList[i].type,
                                typeList[i].isDivided,
                                typeList[i].isFemale,
                                false,
                                true,
                                typeList[i].isViewable
                            )
                        )  // is_devided = Y && isFemale = N -> 남자,  id_devided = N && isFemale = Y -> 여자 , 둘다 N인 것들은 구별안하는 값
                    }
                    Util.setPreference(context, Const.MAIN_CHECK_VIEW, gson.toJson(model))
                }
            }
        }

        //type구분용(종합이나 예능같은경우 한개이거나 값이 null이기때문에 체크를 해줘야됨).
        fun getTypeCheck(isDevided: String?, isFemale: Boolean): String? {
            return if (isDevided == "N" && !isFemale) {
                null
            } else if (isFemale) {
                "F"
            } else {
                "M"
            }
        }

        fun getTypeListArray(context: Context?): List<TypeListModel> {
            val tmp = Util.getPreference(context, Const.PREF_TYPE_LIST)
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<List<TypeListModel?>?>() {}.type
            val list = gson.fromJson(tmp, listType) as List<TypeListModel>
            return list.distinctBy { it.type }
        }

        //동적뷰 생성해서 -> 가이드 사이즈 더 추가
        fun addSupportGuideView(
            context: Context,
            guideList: List<String?>,
            linearLayoutCompat: LinearLayoutCompat
        ) {
            for (i in 1 until guideList.size) {
                val v: View = LayoutInflater.from(context)
                    .inflate(R.layout.item_support_guide_size, null) as View
                val containerLayout: ConstraintLayout = v.findViewById(R.id.ll_container)
                val tvGuideSize: AppCompatTextView = v.findViewById(R.id.tv_guide_size)
                val tvGuideSizeTitle: AppCompatTextView = v.findViewById(R.id.tv_guide_size_title)
                val vDivider: View = v.findViewById(R.id.v_divider)
                if (i == guideList.size - 1) {
                    vDivider.visibility = View.GONE
                }

                tvGuideSizeTitle.text =
                    guideList[i]?.split(": ", "：", limit = 2)?.getOrNull(0) ?: ""
                tvGuideSize.text = guideList[i]?.split(": ", "：", limit = 2)?.getOrNull(1) ?: ""

                linearLayoutCompat.addView(containerLayout)
            }
        }

        // API KEY decode.
        fun getDecodedKey(dataKey: String): String {
            val decodedDataKey = Base64Utils.decode(dataKey)
            return String(decodedDataKey)
        }

        //글자 중앙정렬했을때 마지막글자 공백때문에 아이콘과 글자사이에 공백생김.
        fun replaceEmptyToLF(appCompatTextView: AppCompatTextView) {
            appCompatTextView.post(Runnable {
                try {
                    //텍스트 줄 1줄이상만 계산되게 함.
                    if (appCompatTextView.layout.lineCount > 0 && appCompatTextView.layout.getLineStart(
                            1
                        ) > 0
                    ) {
                        val beginIndex = appCompatTextView.layout.getLineStart(1)
                        if (appCompatTextView.text[beginIndex - 1].toString() == " ") {
                            val tvSubtitleRewardChange: StringBuilder =
                                StringBuilder(appCompatTextView.text.toString())
                            tvSubtitleRewardChange.setCharAt(beginIndex - 1, '\n')
                            appCompatTextView.text = tvSubtitleRewardChange.toString()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }

        fun commentCheckHash(
            context: Context,
            idolId: Int,
            binImage: ByteArray?,
            scope: CoroutineScope,
            commentsRepository: CommentsRepository,
            callback: ((cdnImageUrl: String?) -> Unit)
        ) {

            // 이미지 hash값 먼저 검사
            var cdnImageUrl: String? = null
            if (binImage == null) {
                scope.launch {
                    callback(null)
                }
                return
            }

            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(binImage)
                val hash = Util.bytesToHex(hashBytes)
                // 서버에 같은 hash 이미지가 있는지 조회
                scope.launch {
                    commentsRepository.checkHash(
                        hash,
                        idolId,
                        { response ->
                            // exists가 Y 이면 image_url을 올린다
                            if (response.optString("exists").equals("Y", ignoreCase = true)) {
                                cdnImageUrl = response.optString("image_url")
                            }
                            callback(cdnImageUrl)
                        }, {
                            callback(null)
                        }
                    )
                }
            } catch (e: NoSuchAlgorithmException) {
                scope.launch {
                    callback(null)
                }
            }
        }

        //프로필 이미지 top3가 null일 때 레티/리니 안나와서 추가(어워즈에서 사용)
        fun top1ImageUrl(context: Context, idolModel: IdolModel, reqImageSize: String): String? {
            var top3Ids: Array<String?>? =
                if (idolModel.top3.isNullOrEmpty()) {
                    arrayOfNulls(3)
                } else {
                    idolModel.top3!!.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()
                }
            top3Ids = Arrays.copyOf(top3Ids, 3)
            val ver = idolModel.top3ImageVer.split(",").firstOrNull() ?: ""

            return if (top3Ids[0] == null || top3Ids[0]!!.isEmpty() || top3Ids[0].equals("null")) {
                idolModel.imageUrl
            } else {
                "${ConfigModel.getInstance(context).cdnUrl}/a/${top3Ids[0]}.1.${ver}_${reqImageSize}.webp"
            }
        }

        //셀럽 이미지 cdnUrl 맞춰서 호출해야 해서 추가
        fun top1ImageUrl(
            context: Context,
            idolModel: IdolModel,
            reqImageSize: String,
            sourceApp: String?
        ): String? {
            var top3Ids: Array<String?>? =
                if (idolModel.top3.isNullOrEmpty()) {
                    arrayOfNulls(3)
                } else {
                    idolModel.top3!!.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()
                }
            top3Ids = Arrays.copyOf(top3Ids, 3)

            return if (top3Ids[0] == null || top3Ids[0]!!.isEmpty() || top3Ids[0].equals("null")) {
                idolModel.imageUrl
            } else {
                val ver = idolModel.top3ImageVer.split(",").firstOrNull() ?: ""
                if (sourceApp == "actor") {
                    "${ConfigModel.getInstance(context).cdnUrlActor}/a/${top3Ids[0]}.1.${ver}_${reqImageSize}.webp"
                } else if (sourceApp == "idol") {
                    "${ConfigModel.getInstance(context).cdnUrlIdol}/a/${top3Ids[0]}.1.${ver}_${reqImageSize}.webp"
                } else {
                    "${ConfigModel.getInstance(context).cdnUrl}/a/${top3Ids[0]}.1.${ver}_${reqImageSize}.webp"
                }
            }
        }

        fun getTop3Ids(idol: IdolModel): Array<String?> = if (idol.top3 == null) {
            arrayOfNulls(3)
        } else {
            idol.top3!!.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        private fun getType3Types(idol: IdolModel): Array<String?> = if (idol.top3Type == null) {
            arrayOfNulls(3)
        } else {
            idol.top3Type!!.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        // top3 이미지는 image_url 2 3를 그대로 사용한다 (mp4 주소 추출하는 규칙 적용해야해서 on demand resize 적용이 어려움)
        fun getTop3ImageUrl(context: Context, idol: IdolModel): List<String?> {
            var top3Ids: Array<String?> = getTop3Ids(idol)
            val top3ImageVer = idol.top3ImageVer
            top3Ids = top3Ids.copyOf(3)
            val urls = arrayOf(idol.imageUrl, idol.imageUrl2, idol.imageUrl3)

            // 비밀의방처럼 top3ImageVer가 비어있거나 top3Ids가 모두 null인 경우
            if (top3ImageVer.isEmpty() || top3Ids.all { it.isNullOrEmpty() }) {
                return urls.toList()
            }

            return top3Ids.mapIndexed { index, id ->
                id?.takeIf { it.isNotEmpty() && it.toInt() != 0 }?.let {
                    val ver = top3ImageVer.split(",").getOrNull(index) ?: ""
                    val originalUrl = urls[index]
                    // originalUrl에 이미 ver가 포함되어 있다면 ver를 떼고 새로 추가한다
                    originalUrl?.updateQueryParameter("ver", ver)
                }
            }
        }

        fun trendImageUrl(context: Context, id: Int): String {
            return "${ConfigModel.getInstance(context).cdnUrl}/t/${id}.1_${Const.IMAGE_SIZE_LOWEST}.webp"
        }

        fun trendImageUrl(context: Context, id: String): String {
            return "${ConfigModel.getInstance(context).cdnUrl}/t/${id}.1_${Const.IMAGE_SIZE_LOWEST}.webp"
        }

        fun trendImageUrl(context: Context, id: Int, sourceApp: String?): String {
            return trendImageUrl(context, id.toString(), sourceApp)
        }

        fun trendImageUrl(context: Context, id: String, sourceApp: String?): String {
            return if (sourceApp == "actor") {
                "${ConfigModel.getInstance(context).cdnUrlActor}/t/${id}.1_${Const.IMAGE_SIZE_LOWEST}.webp"
            } else if (sourceApp == "idol") {
                "${ConfigModel.getInstance(context).cdnUrlIdol}/t/${id}.1_${Const.IMAGE_SIZE_LOWEST}.webp"
            } else {
                "${ConfigModel.getInstance(context).cdnUrl}/t/${id}.1_${Const.IMAGE_SIZE_LOWEST}.webp"
            }
        }

        fun onePickImageUrl(context: Context, id: Int, date: String, reqImageSize: String): String {
            return "${ConfigModel.getInstance(context).cdnUrl}/o/${id}.1.${date}_${reqImageSize}.webp"
        }

        fun themePickImageUrl(context: Context, id: Int, dummy: String): String {
            return "${ConfigModel.getInstance(context).cdnUrl}/p/${id}.1.${dummy}_${Const.IMAGE_SIZE_MEDIUM}.webp"
        }

        fun supportImageUrl(context: Context, id: Int): String {
            return "${ConfigModel.getInstance(context).cdnUrl}/s/${id}.1_${Const.IMAGE_SIZE_LOWEST_FOR_SUPPORT}.webp"
        }

        fun fileImageUrl(context: Context, emoticonId: String): String {
            return "${ConfigModel.getInstance(context).cdnUrl}/e/${emoticonId}.t.webp"
        }

        fun fileImageUrl(context: Context, emoticonId: Int): String {
            return "${ConfigModel.getInstance(context).cdnUrl}/e/${emoticonId}.t.webp"
        }

        fun hallImageUrl(context: Context, resourceUrl: String): String {
            return "${ConfigModel.getInstance(context).cdnUrl}/h/${resourceUrl}.1_${Const.IMAGE_SIZE_LOWEST}.webp"
        }

        fun charityImageUrl(context: Context, id: Int): String {
            return "${ConfigModel.getInstance(context).cdnUrl}/c/${id}.1_${Const.IMAGE_SIZE_LOWEST}.webp"
        }


        //KST String 리턴하는 함수(DateFormat.MEDIUM을 써서 년/월/도 까지)
        fun dateTimeToKST(day: String?): String? {
            if (day == null) {
                return null
            }
            return try {
                val formatter =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)  //서버에서 준 날짜 포멧
                formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                val f = DateFormat.getDateInstance(
                    DateFormat.MEDIUM,
                    Locale.getDefault()
                )  //우리가 사용할 날짜 포멧
                f.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                f.format(formatter.parse(day))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun dateToKST(day: String?): String? {
            if (day == null) {
                return null
            }
            return try {
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())  //서버에서 준 날짜 포멧
                formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                val f = DateFormat.getDateInstance(
                    DateFormat.MEDIUM,
                    Locale.getDefault()
                )  //우리가 사용할 날짜 포멧
                f.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                f.format(formatter.parse(day))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        //유저 차단했는지 체크
        fun isUserNotBlocked(context: Context, userId: Int?): Boolean {
            if (userId == null) return true //유저 아이디가 null이면 차단 안한거로 간주

            var userBlockList = ArrayList<String>()
            if (!Util.getPreference(context, Const.USER_BLOCK_LIST).isNullOrEmpty()) {
                //차단된 아티클 array로 저장
                val listType = object : TypeToken<ArrayList<String>>() {}.type
                val gson = IdolGson.getInstance()
                userBlockList = gson.fromJson(
                    Util.getPreference(context, Const.USER_BLOCK_LIST).toString(),
                    listType
                )
            }
            return !userBlockList.any { it == userId.toString() }  //차단한 유저가 있으면 false 반환
        }

        //차단한 리스트 체크용(article for문 돌릴 때 포함됐는지 체크)
        fun isArticleNotReported(context: Context, articleId: String): Boolean {
            var articleReportList = ArrayList<String>()
            if (!Util.getPreference(context, Const.ARTICLE_REPORT_LIST).isNullOrEmpty()) {
                //차단된 아티클 array로 저장
                val listType = object : TypeToken<ArrayList<String>>() {}.type
                val gson = IdolGson.getInstance()
                articleReportList = gson.fromJson(
                    Util.getPreference(context, Const.ARTICLE_REPORT_LIST).toString(),
                    listType
                )
            }
            return !articleReportList.any { it == articleId }
        }

        fun addArticleReport(context: Context?, articleId: String) {
            var articleReportList = ArrayList<String>()

            //저장된 아티클 차단 리스트 가져옴
            if (Util.getPreference(context, Const.ARTICLE_REPORT_LIST).isNotEmpty()) {
                //차단된 아티클 array로 저장
                val listType = object : TypeToken<ArrayList<String>>() {}.type
                val gson = IdolGson.getInstance()
                articleReportList = gson.fromJson(
                    Util.getPreference(context, Const.ARTICLE_REPORT_LIST).toString(),
                    listType
                )
            }

            articleReportList.add(articleId)
            Util.setPreferenceArray(context, Const.ARTICLE_REPORT_LIST, articleReportList)
        }

        //비밀번호 정규식 체크.
        //가능한 특수문자 []"/;:'<>,.~!@#$%^&*()_+?!-=/\
        fun checkPwdRegex(passwd: String): Boolean {
            var count = 0

            val wordPattern = """^.*[A-Za-z].*$"""
            val numberPattern = """^.*[0-9].*$"""
            val specialPattern = """^.*[\[\]\"|;:'<>\\,.~@#\$%^&*()_+?!={}\/-].*$"""

            val pwdPattern = """^[A-Za-z\d\[\]\"|;:'<>\\,.~@#\$%^&*()_+?!={}\/-]{8,}$"""
            if (Pattern.matches(wordPattern, passwd)) count++

            if (Pattern.matches(numberPattern, passwd)) count++

            if (Pattern.matches(specialPattern, passwd)) count++

            return Pattern.matches(pwdPattern, passwd) && count >= 2
        }

        // date를 KST에 맞춘 날짜로 현재 로케일에 맞게 표시
        fun getKSTDateString(date: Date, context: Context): String {
            val f = DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(context))
            f.timeZone = Const.TIME_ZONE_KST
            val dateString = f.format(date)
            return dateString
        }

        /** Get Photo **/
        fun getPhoto(context: Context, isPhoto: Boolean) {

            val photoPickIntent = if (isPhoto) {
                MediaStoreUtils.getPickImageIntent(context) //이미지,움짤일 경우
            } else {
                MediaStoreUtils.getPickVideoIntent(context) //비디오일 경우
            }

            val packageManager = context.packageManager

            if (photoPickIntent.resolveActivity(packageManager) != null) {
                if (isPhoto) {
                    (context as Activity).startActivityForResult(
                        photoPickIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                        BaseActivity.PHOTO_SELECT_REQUEST
                    )
                } else {
                    (context as Activity).startActivityForResult(
                        photoPickIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                        BaseActivity.VIDEO_SELECT_REQUEST
                    )
                }

            } else {
                Util.showDefaultIdolDialogWithBtn1(
                    context,
                    null,
                    context.getString(R.string.cropper_not_found)
                ) { view: View? -> Util.closeIdolDialog() }
            }
        }

        //움짤, video 경로 Return 해주는 함수
        fun setUriPath(context: Context, uri: Uri): String {
            val file = File(uri.lastPathSegment!!)
            val uriType =
                context.contentResolver.getType(uri)!!.split("/") // ex) video/mp4 이런식으로 옴.
            if (file.path.contains(":")) {    //최근 파일에서 고른 경우
                val path = file.path.split(":") //video:123123 이런식으로 와서, video: 뒤의 경로만 따기 위함.
                return path[1].plus("." + uriType[1])
            }

            return file.toString()
                .plus("." + uriType[1]) //갤러리로 들어가서 고른 경우 video:123 , image:123 이런식으로 안와서 file path에 확장자만 붙여서 return
        }

        // 앱내 광고 트래킹
        fun sendAnalyticsAdEvent(context: Context, name: String, id: Int) {
            if (BuildConfig.CHINA) {
                return
            }
            val params = Bundle()
            params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "ExodusEntAd")
            params.putString(FirebaseAnalytics.Param.AD_SOURCE, "ExodusEnt")
            params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, "MenuBanner")
            params.putString(FirebaseAnalytics.Param.AD_FORMAT, "Banner")
            params.putInt("line_item_id", id)
            params.putInt(FirebaseAnalytics.Param.VALUE, 0)
            params.putString(FirebaseAnalytics.Param.CURRENCY, "KRW")

            FirebaseAnalytics.getInstance(context).logEvent(name, params)

            Logger.i("sendAnalyticsAdEvent ${name} ${id}")
        }

        //년 반환하는 함수
        fun getYear(date: Date): String {
            val yearDateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
            yearDateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            return yearDateFormat.format(date)
        }

        //월 반환하는 함수
        fun getMonth(date: Date): String {
            val monthDateFormat = SimpleDateFormat("MMM", Locale.getDefault())
            monthDateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            return monthDateFormat.format(date)
        }

        //일 반환하는 함수
        fun getDay(date: Date): String {
            val dayDateFormat = SimpleDateFormat("d", Locale.getDefault())
            dayDateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            return dayDateFormat.format(date)
        }

        fun getYearMonth(calendar: Calendar): Date {
            val yearMonthCalendar = Calendar.getInstance()

            with(yearMonthCalendar) {
                time = calendar.time
                timeZone = TimeZone.getTimeZone("Asia/Seoul")
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            return yearMonthCalendar.time
        }

        suspend fun getFriendInviteMsg(
            context: Context,
            getIdolsByTypeAndCategoryUseCase: GetIdolsByTypeAndCategoryUseCase
        ): String {
            val account = IdolAccount.getAccount(context)

            var type: String
            var category: String
            val idolList: ArrayList<IdolModel> = arrayListOf()
            val most = account?.most

            return withContext(Dispatchers.IO) {
                var inviteMsg = ""

                if (most == null || most.type.equals("B", ignoreCase = true) || most.isViewable == "N") {
                    // 최애가 없거나 비밀의 방이면
                    type = if (BuildConfig.CELEB) "A" else "S"
                    category = "M"

                    val dbIdols = getIdolsByTypeAndCategoryUseCase(type, category)
                        .mapListDataResource { it.toPresentation() }
                        .awaitOrThrow() ?: return@withContext ""

                    val sortedIdols = dbIdols
                        .sortedByDescending { it.heart }
                        .filter { it.isViewable == "Y" }

                    sortedIdols.forEachIndexed { index, idol ->
                        idol.rank = if (index > 0 && sortedIdols[index - 1].heart == idol.heart) {
                            sortedIdols[index - 1].rank
                        } else {
                            index
                        }
                    }

                    idolList.addAll(sortedIdols)

                    if (idolList.size >= 2) {
                        val idolName = Util.nameSplit(context, idolList[1])
                        if (idolName[1] != "") idolName[1] = "(#${idolName[1]})"

                        inviteMsg = if (BuildConfig.CELEB) {
                            String.format(
                                context.getString(R.string.share_friend_invite_nomost_celeb),
                                Util.getSecureId(context),
                                idolName[0],
                                idolName[1],
                                ""
                            )
                        } else {
                            String.format(
                                context.getString(R.string.share_friend_invite_nomost),
                                Util.getSecureId(context),
                                idolName[0],
                                idolName[1],
                                ""
                            )
                        }
                    }
                } else {
                    type = most.type
                    category = most.category

                    val dbIdols = getIdolsByTypeAndCategoryUseCase(type, category)
                        .mapListDataResource { it.toPresentation() }
                        .awaitOrThrow() ?: return@withContext ""

                    val sortedIdols = dbIdols
                        .sortedByDescending { it.heart }
                        .filter { it.isViewable == "Y" }

                    sortedIdols.forEachIndexed { index, idol ->
                        idol.rank = if (index > 0 && sortedIdols[index - 1].heart == idol.heart) {
                            sortedIdols[index - 1].rank
                        } else {
                            index
                        }
                    }

                    idolList.addAll(sortedIdols)

                    val idol = idolList.find { it.getId() == most.getId() } ?: return@withContext ""

                    val idolName = Util.nameSplit(context, idol)
                    if (idolName[1] != "") idolName[1] = "#${idolName[1]}"

                    inviteMsg = if (BuildConfig.CELEB) {
                        String.format(
                            context.getString(R.string.share_friend_invite_celeb),
                            idolName[0],
                            idol.rank + 1,
                            idolName[0],
                            idolName[1],
                            Util.getSecureId(context),
                            ""
                        )
                    } else {
                        String.format(
                            context.getString(R.string.share_friend_invite),
                            idolName[0],
                            idol.rank + 1,
                            idolName[0],
                            idolName[1],
                            Util.getSecureId(context),
                            ""
                        )
                    }
                }

                inviteMsg
            }
        }

        //와이파이 켜져있을 경우, 데이터절약모드 상관없이 false, wifi 꺼져있을 경우 데이터절약모드 상태에 따라 처리
        fun dataSavingMode(context: Context): Boolean {
            return if (InternetConnectivityManager.getInstance(context).isWifiConnected) {
                false
            } else {
                Util.getPreferenceBool(context, Const.PREF_DATA_SAVING, false)
            }
        }

        fun getFirebaseLabel(
            category: String,
            type: String,
            screenType: String
        ): String {
            return when (screenType) {
                "hof_agg" -> {
                    "hof_${category}_${type}_trends"
                }

                "hof_day" -> {
                    "hof_${category}_${type}_daily"
                }

                "hof_trend" -> {
                    "hof_${category}_${type}_trend_chart"
                }

                "hof_history" -> {
                    "hof_${category}_${type}_daily_history"
                }

                else -> {
                    "main_${category}_${type}"
                }
            }
        }

        // 원하는 locale String 넣어서 설정
        fun setLocale(context: Context?, localeString: String) {
            val ls: Array<String> =
                localeString.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val locale = if (ls.size >= 2) {
                Locale(ls[0], ls[1])
            } else {
                Locale(localeString)
            }

            Locale.setDefault(locale)
            val config: Configuration =
                context?.resources?.configuration ?: return //new Configuration();
            Locale.setDefault(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
            } else {
                config.locale = locale
            }
            context.resources?.updateConfiguration(config, context.resources.displayMetrics)
        }

        fun getLocale(localeString: String): Locale {
            val ls: Array<String> =
                localeString.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return if (ls.size >= 2) {
                Locale(ls[0], ls[1])
            } else {
                Locale(localeString)
            }
        }

        //스트링 언어 하나만 가져오고 싶을때 사용.
        fun getLocaleStringResource(
            requestedLocale: Locale?,
            resourceId: Int,
            context: Context
        ): String {
            val result: String
            val config =
                Configuration(context.resources.configuration)
            config.setLocale(requestedLocale)
            result = context.createConfigurationContext(config).getText(resourceId).toString()

            return result
        }

        // 안읽은 공지사항이 있는지
        fun hasUnreadNotice(context: Context): Boolean {
            try {
                val readIds =
                    Util.getPreference(context, Const.PREF_NOTICE_READ).split(",".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                val serverList =
                    Util.getPreference(context, Const.PREF_NOTICE_LIST).split(",".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()

//            if (readNoticeIds.size == noticeList.size) mIvNoticeNew.setVisibility(View.GONE) // ???
                // 서버 리스트 항목들이 읽은 항목들에 포함되어 있는지 확인 (언어 변경시 언어별 공지 id가 각각 저장되어 개별비교 해야함)
                var read = true
                run {
                    serverList.forEach {
                        if (!readIds.contains(it)) {
                            read = false
                            return@run
                        }
                    }
                }
                return !read
            } catch (e: NullPointerException) {
            }
            return true
        }

        // 안읽은 이벤트가 있는지
        fun hasUnreadEvent(context: Context): Boolean {
            try {
                val readIds =
                    Util.getPreference(context, Const.PREF_EVENT_READ).split(",".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                val serverList =
                    Util.getPreference(context, Const.PREF_EVENT_LIST).split(",".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()

                var read = true
                run {
                    serverList.forEach {
                        if (!readIds.contains(it)) {
                            read = false
                            return@run
                        }
                    }
                }
                return !read
            } catch (e: NullPointerException) {
            }
            return true
        }

        fun getBitmapFromVectorDrawable(context: Context?, drawable: Drawable?): Bitmap? {
            val context = context ?: return null
            var drawable = drawable ?: return null
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }

        fun clickMore(
            context: Context?, model: ArticleModel?,
            showArticleBottomSheet: (Boolean, Boolean, Boolean, Boolean) -> Unit
        ) {
            var showEdit = false
            var showRemove = false
            var showReport = false
            var showShare = false

            val mAccount = IdolAccount.getAccount(context)

            if (mAccount?.userModel == null || model?.user == null) {
                return
            }

            if (mAccount.userModel?.id == model.user?.id) {
                showEdit = true
                showRemove = true
            } else {
                showReport = true
                if (mAccount.heart == Const.LEVEL_ADMIN) {
                    showEdit = true
                    showRemove = true
                } else if (mAccount.heart == Const.LEVEL_MANAGER) {
                    if (mAccount.most != null && model.idol != null) {
                        if (mAccount.most?.resourceUri == model.idol?.resourceUri) {
                            showEdit = true
                            showRemove = true
                        }
                    }
                }
            }

            if (model.isMostOnly != "Y") showShare = true

            showArticleBottomSheet(showEdit, showRemove, showReport, showShare)
        }

        fun getScreenWidth(activity: Activity): Int {
            val display: Display = activity.windowManager.defaultDisplay
            val size = Point()
            display.getRealSize(size)
            return Util.convertPixelsToDp(activity, size.x.toFloat()).toInt()
        }

        fun isDayChanged(serverTime: String?, context: Context): Boolean {
            var result = false
            val lastTime = Util.getPreference(context, Const.PREF_SERVER_TIME)
            if (serverTime != null && lastTime.length >= 10 && serverTime.length >= 10) {
                // T 로 잘라서 날짜만 비교
                val dayServer = serverTime.substring(0, 10)
                val dayPrev = lastTime.substring(0, 10)
                if (!dayServer.equals(dayPrev, ignoreCase = true)) {
                    result = true
                }
            }
            Util.log("serverTime $serverTime")
            Util.log("lastTime $lastTime")
            Util.setPreference(context, Const.PREF_SERVER_TIME, serverTime)
            return result
        }

        fun getAwardChartCode(type: String, category: String): String {
            var chartCode = ChartCode.AW_HDA2023.chartCode
            when (type) {
                Const.TYPE_SOLO -> chartCode += "S"//남여 솔로 통합으로 category가뭐든지 type이 솔로로오면 S로 줌.
                Const.TYPE_GROUP -> chartCode += if (category == "M") "GM" else "GF"
                Const.TYPE_GEN_4 -> chartCode += if (category == "M") "4M" else "4F"
            }
            return chartCode
        }

        //  베트남, 스페인, 인도네시아일 경우 버튼 넓이 넓게 수정
        fun setSegmentBtnWidth(
            context: Context,
            segmentedButtonGroup: SegmentedButtonGroup,
            constraintLayout: ConstraintLayout,
            sbSolo: SegmentedButton,
            sbGroup: SegmentedButton,
            sbGeneration4: SegmentedButton
        ) {
            val locale = Locale.getDefault().toString().split("_".toRegex()).toTypedArray()
            when (locale[0]) {
                "vi", "es", "in" -> {
                    sbSolo.textSize = Util.convertDpToPixel(context, 12f)
                    sbGroup.textSize = Util.convertDpToPixel(context, 12f)
                    sbGeneration4.textSize = Util.convertDpToPixel(context, 12f)
                    constraintLayout.layoutParams.width =
                        Util.convertDpToPixel(context, 308f).toInt()
                    segmentedButtonGroup.layoutParams.width =
                        Util.convertDpToPixel(context, 302f).toInt()
                }
            }
        }

        fun extractUrls(text: String): List<String> {
            val urls = mutableListOf<String>()

            // Regex pattern to match URLs
            val urlRegex = "((https?):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)"
            val regex = Regex(urlRegex).findAll(text)

            // Find all occurrences of URLs
            for (match in regex) {
                val url = match.value
                urls.add(url)
            }

            return urls
        }

        fun getQuizTypeList(context: Context?): ArrayList<QuizCategoryModel> {
            val tmp = Util.getPreference(context, Const.PREF_QUIZ_TYPE_LIST)
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<ArrayList<QuizCategoryModel?>?>() {}.type
            return if (!tmp.isNullOrEmpty()) gson.fromJson(
                tmp,
                listType
            ) as ArrayList<QuizCategoryModel> else arrayListOf()
        }

        fun getPrefIdolList(context: Context?, key: String): ArrayList<IdolModel> {
            val tmp = Util.getPreference(context, key)
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<ArrayList<IdolModel?>?>() {}.type
            return if (!tmp.isNullOrEmpty()) gson.fromJson(
                tmp,
                listType
            ) as ArrayList<IdolModel> else arrayListOf()
        }

        fun setTapJoy(activity: Activity, isConnected: (Boolean) -> Unit) {
            val connectFlags = Hashtable<String, Any>()
            if (BuildConfig.DEBUG) {
                connectFlags[TapjoyConnectFlag.ENABLE_LOGGING] =
                    "true" // Disable this in production builds
            }
            connectFlags[TapjoyConnectFlag.USER_ID] =
                IdolAccount.getAccount(activity)?.email ?: ""// Important for self-managed currency

            if (Tapjoy.isConnected()) {
                isConnected(true)
                return
            }

            // tapjoy
            Tapjoy.connect(
                activity,
                Const.TAPJOY_KEY,
                connectFlags,
                object : TJConnectListener() {
                    override fun onConnectSuccess() {
                        isConnected(true)
                    }
                    override fun onConnectFailure(code: Int, message: String) {
                        isConnected(false)
                    }
                },
            )
            if (BuildConfig.DEBUG) {
                Tapjoy.setDebugEnabled(BuildConfig.DEBUG) // Do not set this for apps released to a store!
            }
        }

        // Compose 용 함수
        fun getName(name: String) : Pair<String, String>{
            val artistName = Util.nameSplit(name)[0]
            val groupName = if (name.contains("_")) {
                Util.nameSplit(name)[1]
            } else {
                ""
            }
            return Pair(artistName, groupName)
        }

        fun setName(name: String, nameView: TextView, groupView: TextView) {
            nameView.text = Util.nameSplit(name)[0]
            groupView.visibility = View.VISIBLE
            if (name.contains("_")) {
                groupView.text = Util.nameSplit(name)[1]
            } else {
                groupView.visibility = View.GONE
            }
        }

        fun setNameWithInvisible(name: String, nameView: TextView, groupView: TextView) {
            nameView.text = Util.nameSplit(name)[0]
            groupView.visibility = View.VISIBLE
            if (name.contains("_")) {
                groupView.text = Util.nameSplit(name)[1]
            } else {
                groupView.visibility = View.INVISIBLE
            }
        }

        // 이름/그룹(예명) 출력해주는 부분 공통 처리
        fun setName(context: Context?, idol: IdolModel?, nameView: TextView, groupView: TextView) {
            val context = context ?: return
            val idol = idol ?: return
            // type이 오지 않는 경우 대비 (예: 서포트)
            val isSolo = idol.type.equals("S", ignoreCase = true) || idol.groupId != idol.getId()
            if (BuildConfig.CELEB && idol.getName(context).contains("_")) {
                nameView.text = Util.nameSplit(context, idol)[0]
                groupView.text = Util.nameSplit(context, idol)[1]
                groupView.visibility = View.VISIBLE
            } else if (!BuildConfig.CELEB && isSolo) {
                nameView.text = Util.nameSplit(context, idol)[0]
                groupView.visibility = View.VISIBLE
                if (idol.getName(context).contains("_")) {
                    groupView.text = Util.nameSplit(context, idol)[1]
                } else {
                    groupView.visibility = View.GONE
                }
            } else {
                nameView.text = idol.getName(context)
                groupView.visibility = View.GONE
            }
        }

        fun setName(
            context: Context?,
            hallModel: HallModel,
            nameView: TextView,
            groupView: TextView
        ) {
            val context = context ?: return
            val idol: IdolModel? = hallModel.idol
            val name = hallModel.getName(context) ?: (idol?.getName()
                ?: "") // 기록실 기부 화면처럼 이름 필드값이 있으면 이걸 사용하고 없으면 하위 아이돌 이름 사용
            val type = hallModel.type ?: (idol?.type ?: "")
            // type이 오지 않는 경우 대비 (예: 서포트)
            val isSolo = type.equals("S", ignoreCase = true) || idol?.groupId != idol?.getId()

            if (BuildConfig.CELEB && name.contains("_")) {
                nameView.text = Util.nameSplit(name)[0]
                groupView.text = Util.nameSplit(name)[1]
                groupView.visibility = View.VISIBLE
            } else if (!BuildConfig.CELEB && isSolo) {
                nameView.text = Util.nameSplit(name)[0]
                groupView.visibility = View.VISIBLE
                if (name.contains("_")) {
                    groupView.text = Util.nameSplit(name)[1]
                } else {
                    groupView.visibility = View.GONE
                }
            } else {
                nameView.text = name
                groupView.visibility = View.GONE
            }
        }

        fun hasEmoticons(input: String): Boolean {
            val regex = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+")
            return regex.find(input) != null
        }

        /**
         * 최애 설정/해제를 시도한다
         *
         * @param idol 최애로 설정할 idol. 해제는 null
         * @param onConfirm 최애 설정 확인시 불림. 인자값은 참여중인 채팅방의 아이돌/그룹 id
         * @param onCancel 최애 설정 취소시 불림.
         */
        fun showChangeMostDialog(
            context: Context,
            idol: IdolModel?,
            sharedAppState: SharedAppState,
            onConfirm: (Pair<Int?, Int?>) -> Unit,
            onCancel: () -> Unit
        ) {
            val mostIdol: IdolModel? = IdolAccount.getAccount(context)?.userModel?.most
            var idolFullName = mostIdol?.getName(context) ?: ""
            var idolSoloName: String = Util.nameSplit(context, mostIdol)[0]
            var idolGroupName: String = Util.nameSplit(context, mostIdol)[1]

            val mostId = mostIdol?.getId()
            val mostGroupId = mostIdol?.groupId

            CoroutineScope(Dispatchers.IO).launch {
                val chatroomJoined = async { isJoinedChatroom(context) }.await()
                val isSoloExist = chatroomJoined.first
                val isGroupExist = chatroomJoined.second
                var msg = ""
                var idolName = ""
                var idolName2: String? = null // '~를 해제하고 ~를 최애 설정하시겠습니까' 의 두 번째 텍스트
                var chatroomIdolIdPair = Pair<Int?, Int?>(null, null) // 참여중인 개인/그룹 채팅방 idol id pair

                val unregiGuide1 =
                    context.getString(if (BuildConfig.CELEB) R.string.actor_msg_favorite_unregi_guide1 else R.string.msg_favorite_unregi_guide1)
                val unregiGuide2 =
                    context.getString(if (BuildConfig.CELEB) R.string.actor_msg_favorite_unregi_guide2 else R.string.msg_favorite_unregi_guide2)
                val unregiGuide3 =
                    context.getString(if (BuildConfig.CELEB) R.string.actor_msg_favorite_unregi_guide3 else R.string.msg_favorite_unregi_guide3)

                val regiGuide1 =
                    context.getString(if (BuildConfig.CELEB) R.string.actor_msg_favorite_guide_1 else R.string.msg_favorite_guide_1)
                val regiGuide2 =
                    context.getString(if (BuildConfig.CELEB) R.string.actor_msg_favorite_guide_2__ else R.string.msg_favorite_guide_2__)
                val regiGuide4 =
                    context.getString(if (BuildConfig.CELEB) R.string.actor_msg_favorite_guide_4 else R.string.msg_favorite_guide_4)

                if (idol == null) { // 해제하는 경우
                    // 그룹 멤버라면
                    if (!BuildConfig.CELEB && mostId != mostGroupId) {
                        // 개인 채팅방/그룹 채팅방 모두 들어가 있으면
                        if (isSoloExist && isGroupExist) {
                            idolName = idolSoloName.plus(", ").plus(idolGroupName)
                            msg = String.format(unregiGuide3, idolName)
                            chatroomIdolIdPair = Pair(mostId, mostGroupId)
                        } else if (isSoloExist) {
                            // 개인 채팅방에만 들어가 있으면
                            idolName = idolSoloName
                            msg = String.format(unregiGuide3, idolName)
                            chatroomIdolIdPair = Pair(mostId, null)
                        } else if (isGroupExist) {
                            // 그룹 채팅방에만 들어가 있으면
                            idolName = idolGroupName
                            msg = String.format(unregiGuide3, idolName)
                            chatroomIdolIdPair = Pair(null, mostGroupId)
                        } else {
                            msg = HtmlCompat.fromHtml(
                                unregiGuide2
                                    + "<br>" + unregiGuide1,
                                HtmlCompat.FROM_HTML_MODE_LEGACY
                            ).toString()
                        }
                    } else {
                        // 솔로 혹은 그룹 (+ 셀럽)
                        if (isSoloExist || isGroupExist) {
                            idolName = if (BuildConfig.CELEB) idolSoloName else idolFullName
                            msg = String.format(unregiGuide3, idolName)
                            chatroomIdolIdPair = Pair(
                                if (isSoloExist) mostId else null,
                                if (isGroupExist) mostGroupId else null
                            )
                        } else {
                            msg = HtmlCompat.fromHtml(
                                unregiGuide2
                                    + "<br>" + unregiGuide1,
                                HtmlCompat.FROM_HTML_MODE_LEGACY
                            ).toString()
                        }
                    }
                } else {
                    // 최애 설정
                    //최애가 없으면
                    if (mostIdol?.category == "B" || mostIdol == null) {
                        idolName = idol.getName(context)
                        msg = String.format(regiGuide1 + "\n" + regiGuide2, idolName)
                    } else if (BuildConfig.CELEB) { // 셀럽
                        // 채팅방이 있을 때
                        if (isSoloExist) {
                            idolName = idolSoloName
                            idolName2 = idol.getName(context)
                            msg = String.format(regiGuide4, idolSoloName, idolName2)
                        } else {
                            idolName = idol.getName(context)
                            msg = String.format(regiGuide1 + "\n" + regiGuide2, idolName)
                        }
                    } else { // 최애돌
                        //최애가 있으면
                        //그룹 내에서 최애를 바꿀 때
                        if (mostGroupId == idol.groupId) {
                            //그룹에서 그룹 개인으로 이동할 때
                            if (mostGroupId == mostId) {
                                idolName = idol.getName(context)
                                msg = String.format(regiGuide1 + "\n" + regiGuide2, idolName)
                            }
                            //그룹 개인에서 그룹 개인으로 이동할 때
                            else {
                                //개인 채팅방이 있을 때
                                if (isSoloExist) {
                                    idolName = idolSoloName
                                    idolName2 = idol.getName(context)
                                    msg = String.format(regiGuide4, idolName, idolName2)
                                }
                                //개인 채팅방이 없을 때
                                else {
                                    idolName = idol.getName(context)
                                    msg = String.format(regiGuide1 + "\n" + regiGuide2, idolName)
                                }
                            }
                        } else {
                            //그룹있는 사람일 때
                            if (mostId != mostGroupId) {
                                //개인, 그룹 채팅방 둘 다 있을 때
                                if (isSoloExist && isGroupExist) {
                                    idolName = idolSoloName.plus(", ").plus(idolGroupName)
                                    idolName2 = idol.getName(context)
                                    msg = String.format(regiGuide4, idolName, idolName2)
                                } else if (isSoloExist && !isGroupExist) {
                                    idolName = idolSoloName
                                    idolName2 = idol.getName(context)
                                    msg = String.format(regiGuide4, idolName, idolName2)
                                } else if (!isSoloExist && isGroupExist) {
                                    idolName = idolGroupName
                                    idolName2 = idol.getName(context)
                                    msg = String.format(
                                        regiGuide4,
                                        idolGroupName,
                                        idol.getName(context)
                                    )
                                } else {
                                    idolName = idol.getName(context)
                                    msg = String.format(regiGuide1 + "\n" + regiGuide2, idolName)
                                }
                            }
                            //그룹 없는 사람일 때
                            else {
                                if (isSoloExist || isGroupExist) {
                                    idolName = idolFullName
                                    idolName2 = idol.getName(context)
                                    msg = String.format(regiGuide4, idolName, idol.getName(context))
                                } else {
                                    idolName = idol.getName(context)
                                    msg = String.format(regiGuide1 + "\n" + regiGuide2, idolName)
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (TextUtils.isEmpty(idolName2)) {
                        Util.showDefaultIdolDialogWithBtn2(context,
                            UtilK.getMyIdolTitle(context),
                            msg,
                            idolName,
                            R.string.yes,
                            R.string.no,
                            {
                                Util.closeIdolDialog()
                                onConfirm(chatroomIdolIdPair)
                                // 최애 변경을 알림
                                sharedAppState.setMostChanged(true)
                            }
                        ) {
                            Util.closeIdolDialog()
                            onCancel()
                        }
                    } else {
                        Util.showDefaultIdolDialogWithBtn2(context,
                            UtilK.getMyIdolTitle(context),
                            msg,
                            idolName,
                            idolName2 ?: "",
                            R.string.yes,
                            R.string.no,
                            {
                                Util.closeIdolDialog()
                                onConfirm(chatroomIdolIdPair)
                                // 최애 변경을 알림
                                sharedAppState.setMostChanged(true)
                            }
                        ) {
                            Util.closeIdolDialog()
                            onCancel()
                        }
                    }
                }
            }
        }

        // 아이돌 개인/소속 그룹 채팅방에 참여중인지 여부
        fun isJoinedChatroom(context: Context): Pair<Boolean, Boolean> {
            val mostIdol: IdolModel? = IdolAccount.getAccount(context)?.userModel?.most
            val roomList = ChatRoomList.getInstance(context).getAll12()
            val mostId = mostIdol?.getId()
            val mostGroupId = mostIdol?.groupId

            val isSoloExist = roomList?.any { it.idolId == mostId && it.isJoinedRoom } ?: false
            val isGroupExist =
                roomList?.any { it.idolId == mostGroupId && it.isJoinedRoom } ?: false

            if (BuildConfig.CELEB) {
                return Pair(isSoloExist, false)
            }
            return Pair(isSoloExist, isGroupExist)
        }

        fun getAppName(context: Context): String {
            return when {
                BuildConfig.CELEB -> context.getString(R.string.actor_app_name_upper)
                BuildConfig.CHINA -> context.getString(R.string.app_name_china_upper)
                BuildConfig.ONESTORE -> context.getString(R.string.app_name_onestore_upper)
                else -> context.getString(R.string.app_name_upper)
            }
        }

        fun restartApplication(activity: Activity) {
            val packageManager: PackageManager = activity.packageManager
            val intent = packageManager.getLaunchIntentForPackage(activity.packageName)
            val componentName = intent?.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            activity.startActivity(mainIntent)
            exitProcess(0)
        }

        fun linkStart(context: Context?, appLinkPage: String) {
            if (context == null) return
            val url = ServerUrl.HOST + "/" + appLinkPage + "/"
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, url)

            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    context.resources.getString(R.string.title_share)
                )
            )
        }

        fun linkStart(context: Context?, url: String, msg: String? = null) {
            if (context == null) return
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"

            val textToShare = if (msg.isNullOrEmpty()) {
                url
            } else {
                "$msg\n$url"
            }

            shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare)

            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    context.resources.getString(R.string.title_share)
                )
            )
        }

        fun formatNumberShort(value: Int): String {
            return when {
                value < 1000 -> "$value"
                value < 1000000 -> {
                    val formattedValue = value.toDouble() / 1000.0
                    val decimalFormat = DecimalFormat("#.#")
                    "${decimalFormat.format(formattedValue)}K"
                }

                else -> {
                    val formattedValue = value.toDouble() / 1000000.0
                    val decimalFormat = DecimalFormat("#.#")
                    "${decimalFormat.format(formattedValue)}M"
                }
            }
        }

        private var lottieDialog: AlertDialog? = null

        fun showLottie(
            context: Context,
            cancelable: Boolean,
            onDismissCallback: (() -> Unit)? = null
        ) {
            try {
                val dialogView = LayoutInflater.from(context).inflate(R.layout.lottie_layout, null)

                lottieDialog = AlertDialog.Builder(context).apply {
                    setView(dialogView)
                    setCancelable(cancelable)
                }.create().apply {
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    setCanceledOnTouchOutside(false)
                    setOnDismissListener {
                        onDismissCallback?.invoke()
                    }
                }
                lottieDialog?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun closeProgress() {
            if (lottieDialog != null) {
                try {
                    lottieDialog!!.dismiss()
                    lottieDialog = null
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
            lottieDialog?.let {
                try {
                    lottieDialog!!.dismiss()
                    lottieDialog = null
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun Context.getSystemLanguage(): String {
            val locale = Util.getSystemLanguage(this)

            return if (locale.equals("ko_KR", ignoreCase = true)) {
                "ko"
            } else if (locale.equals("zh_CN", ignoreCase = true)) {
                "zh_cn"
            } else if (locale.equals("zh_TW", ignoreCase = true)) {
                "zh_tw"
            } else if (locale.equals("ja_JP", ignoreCase = true)) {
                "ja"
            } else {
                "en"
            }
        }

        @JvmStatic
        fun getTypeList(context: Context, tc: String?): TypeListModel {
            var res: TypeListModel? = null
            val tmp = Util.getPreference(context, Const.PREF_TYPE_LIST)
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<List<TypeListModel>>() {}.type
            val typeList: List<TypeListModel> = gson.fromJson(tmp, listType)
            val type = if (tc.isNullOrEmpty()) null else tc[0].toString()

            for (i in typeList.indices) {
                if (typeList[i].type == null || typeList[i].type.equals(
                        type,
                        ignoreCase = true
                    ) || typeList[i].type.equals(tc, ignoreCase = true)
                ) {
                    res = typeList[i]
                    if (typeList[i].type != null) break
                }
            }

            if (res == null) {
                res = TypeListModel(
                    uiColor = "#ffffff",
                    uiColorDarkmode = "#cccccc",
                    fontColor = "#000000",
                    fontColorDarkmode = "#000000"
                )
            }
            return res
        }

        fun getAwardTitle(resultTitle: String?, name: String?) : String? {
            val title = resultTitle?.replace("<CHART_NAME>", name ?: "")
            return title
        }

        // java에서 쓰는 함수
        fun decodeAward(context: Context, json: String): AwardModel {
            val awardData = Json{ignoreUnknownKeys = true}.decodeFromString<AwardModel>(Util.getPreference(context, Const.AWARD_MODEL))
            return awardData
        }

        fun extractTranslatable(input: String): String {
            // URL 패턴
            val urlPattern = "(http|https)://(([\\w\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\-\\+\\=\\(\\)\\{\\}\\?\\<\\>])*)+([\\.|/](([\\w\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\-\\+\\=\\(\\)\\{\\}\\?\\<\\>])*))+"

            // 해시태그 패턴
            val hashtagPattern = "#(\\p{L}|_|[0-9])+"

            // 이모지 패턴 (문자/숫자/기호 이외의 문자를 제거)
            val emojiPattern = "[^\\p{L}\\p{N}\\p{P}\\p{Z}]"

            // 언급 패턴
            val mentionPattern = "@\\{\\d+:[^}]+\\}"

            // 패턴을 통합
            val combinedPattern = "$urlPattern|$hashtagPattern|$emojiPattern|$mentionPattern"

            // Regex를 이용해 매칭되는 부분을 제거
            return Regex(combinedPattern).replace(input, "")
                .replace("\\s+".toRegex(), " ")  // 여러 공백을 하나로
                .trim()
        }

        // api 응답 오류 공통 처리
        fun handleCommonError(context: Context?, response: JSONObject) {
            if(context == null) return
            ErrorControl.parseError(context, response)?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }

        fun openNotificationSettings(context: Context) {
            val intent = Intent()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0 (API 26) 이상: 앱의 알림 설정 화면
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                // Android 7.1 이하: 앱 상세 설정 화면으로 이동
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = "package:${context.packageName}".toUri()
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        fun canPlayUmjjal(url: String?) : Boolean {
            return url?.endsWith(".mp4") == true || url?.endsWith("_s_mv.jpg") == true
        }

        // 안읽은 알림 표시
        fun checkNewNotification(context: Context, messagesRepository: MessagesRepository, sharedAppState: SharedAppState) {
            val utcDate = Util.getPreference(context, Const.KEY_RECENT_NOTIFICATION_CREATE_DATE)
            MainScope().launch {
                messagesRepository.get(
                    "P",
                    utcDate,
                    { response ->
                        if (response.optBoolean("success")) {
                            // "meta": {"total_count"}가 0보다 큰지 체크
                            val meta = response.optJSONObject("meta")
                            val totalCount = meta?.optInt("total_count") ?: 0
                            sharedAppState.setUnreadNotification(totalCount > 0)
                        }
                    }, { _ ->
                    })
            }
        }

        fun countWithLocale(context: Context, mostCount: Int): String {
            val locale = LocaleUtil.getAppLocale(context)
            val numberFormat = NumberFormat.getNumberInstance(locale).apply {
                maximumFractionDigits = 0
                minimumFractionDigits = 0
            }

            return when {
                mostCount < 1_000 -> numberFormat.format(mostCount)
                mostCount < 1_000_000 -> numberFormat.format(mostCount / 1_000f) + "K"
                mostCount < 1_000_000_000 -> numberFormat.format(mostCount / 1_000_000f) + "M"
                else -> numberFormat.format(mostCount / 1_000_000_000f) + "B"
            }
        }

        /**
         * 예외 발생시 다이얼로그 띄우기
         * 관리자라면 좀 더 상세한 정보를 표시한다
         */
        fun showExceptionDialog(
            context: Context,
            throwable: Throwable? = null,
            volleyError: VolleyError? = null,
            errorMsg: String? = null,
            callback: (() -> Unit)? = null,
        ) {
            var msg = context.getString(R.string.error_abnormal_exception)

            var additionalMsg = ""
            throwable?.let {
                additionalMsg += "\n\n${throwable.message}\n\n${throwable.stackTraceToString()}"
            }
            volleyError?.let {
                additionalMsg += "\n\n${volleyError.message}\n\n${volleyError.stackTraceToString()}"
            }
            errorMsg?.let {
                additionalMsg += "\n\n$errorMsg"
            }
            // 별다른 메시지가 없으면 콜스택 표시
            if (additionalMsg.isEmpty()) {
                additionalMsg += "\n\n${Thread.currentThread().stackTrace.joinToString("\n")}"
            }
            // firebase crashlytics에 non-fatal 로그 남기기
            val exception = IllegalStateException(msg + additionalMsg)
            FirebaseCrashlytics.getInstance().recordException(exception)

            Util.closeProgress()
            Util.showIdolDialogWithBtn1(
                context,
                null,
                msg
            ) {
                Util.closeIdolDialog()
                callback?.invoke()
            }
        }


        // VM/Rooting/인앱결제 오류 로그용
        fun postVMLog(context: Context?, scope: CoroutineScope, miscRepository: MiscRepository, log: String, key: String) {
            try {
                if (context != null) {
                    v("로그 -> $log")
                    scope.launch {
                        miscRepository.reportLog(key, log, {}, {})
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        // 최애 설정/변경 타이틀 공통처리
        fun getMyIdolTitle(context: Context?): String {
            return if (BuildConfig.CELEB) {
                context?.getString(R.string.actor_title_favorite_setting) ?: ""
            } else {
                context?.getString(R.string.title_favorite_setting) ?: ""
            }
        }

        fun checkLevelUp(context: BaseActivity?, accountManager: IdolAccountManager, idol: IdolModel?, heart: Long) {
            if (idol == null || context == null) return

            Util.log("***** checkLevelUp idol=" + idol.getId() + " groupId=" + idol.groupId + " heart=" + heart)
            try {
                val account = getAccount(context)

                // 내 최애그룹에 투표하거나 최애에 투표한 경우
                var isValidVote = false
                if (account!!.most != null) {
                    // 배우자인 경우 group_id == 0이고 최애에 투표한 경우만 처리
                    if (idol.groupId == 0) {
                        if (idol.getId() == account.most!!.getId()) {
                            isValidVote = true
                        }
                    } // group_id가 있으면 최애그룹/멤버에 투표
                    else if (idol.groupId == account.most!!.groupId) {
                        isValidVote = true
                    }
                }
                if (isValidVote) {
                    val oldLevel = account.level

                    // 투표할 때마다 users/self를 호출할 수는 없어서 투표한 하트수만큼 레벨하트 수동 증가시킴.
                    val user = account.userModel
                    user?.let {
                        it.levelHeart += heart
                    }
                    account.saveAccount(context) // 순위창에서 조금씩 투표하는 경우 누적하트가 저장이 안되서 호출

                    val newLevel = Util.getLevel(account.levelHeart)

                    if (oldLevel != newLevel) {
                        accountManager.fetchUserInfo(context, {
                            user?.level = newLevel
                            context.showLevelUpDialog(newLevel)
                        })
                    }
                }
            } catch (e: java.lang.NullPointerException) {
                e.printStackTrace()
            }
        }

        fun getInviteCodeFromClipboard(context: Context): String? {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData? = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val clipboardText = clip.getItemAt(0).coerceToText(context).toString()

                // 정규식 패턴
                val regex = Regex(":\\s*([A-Fa-f0-9]{8})")
                val match = regex.find(clipboardText)

                return match?.groupValues?.get(1)
            }
            return null
        }
    }
}