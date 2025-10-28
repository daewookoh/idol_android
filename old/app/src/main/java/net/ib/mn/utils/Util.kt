package net.ib.mn.utils

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.LocaleList
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.util.DisplayMetrics
import android.util.Patterns
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.FragmentManager
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.gson.reflect.TypeToken
import net.htmlparser.jericho.Source
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.activity.AuthActivity.Companion.createIntent
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.HeartPlusFreeActivity
import net.ib.mn.activity.MezzoPlayerActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.activity.SearchResultActivity.Companion.createIntent
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.chatting.chatDb.ChatRoomList.Companion.getInstance
import net.ib.mn.chatting.model.ChatMembersModel
import net.ib.mn.core.data.api.ServerUrl.HOST
import net.ib.mn.core.data.api.ServerUrl.HOST_TEST
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.model.BadWordModel
import net.ib.mn.dialog.NotificationSettingDialog
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.fragment.RewardBottomSheetDialogFragment.Companion.newInstance
import net.ib.mn.model.AccessModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.LGCodeModel
import net.ib.mn.model.LinkDataModel
import net.ib.mn.model.SearchHistoryModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.FileLog.writeLog
import net.ib.mn.utils.IdolSnackBar.Companion.make
import net.ib.mn.utils.LocaleUtil.setLocale
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.UtilK.Companion.getBitmapFromVectorDrawable
import net.ib.mn.utils.UtilK.Companion.videoDisableTimer
import net.ib.mn.utils.ext.safeActivity
import net.ib.mn.utils.permission.Permission.getStoragePermissions
import net.ib.mn.utils.permission.PermissionHelper.PermissionListener
import net.ib.mn.utils.permission.PermissionHelper.requestPermissionIfNeeded
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.InvocationTargetException
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.math.pow
import kotlin.math.sqrt

class Util {
    //미디어 파일 저장용
    private val MEDIA_TYPE_ALL = 0
    private val MEDIA_TYPE_GIF = 1
    private val MEDIA_TYPE_MP4 = 2

    companion object {
        var DEBUG: Boolean =
            BuildConfig.DEBUG || HOST == HOST_TEST // 테섭이면 로그 다찍게 //BuildConfig.DEBUG ? true : false;

        var DEBUG_OUT: Boolean = false //BuildConfig.DEBUG ? true : false;

        var isSoloExist: Boolean = false
        var isGroupExist: Boolean = false

        const val PREF_NAME: String = "com.exodus.myloveidol"
        const val EXOKEY: String = "dus-"
        const val PROPERTY_DEVICE_ID: String = "device_id"
        const val PROPERTY_AD_ID: String = "ad_id"

        private val iabLog: String? = null
        private val handler: Handler? = null
        private val runnable: Runnable? = null

        // 로그쓰기 메소드
        fun log(str: String?) {
            if (DEBUG)  //Log.i("idol", str);
                printLog("i", "idol", str ?: "")
            if (DEBUG_OUT) writeLog(str ?: "")
        }

        // 4000자 이상 제한없이 로그 출력
        fun printLog(level: String, tag: String?, string: String) {
            val Log: Class<*>
            try {
                Log = Class.forName("android.util.Log")
                val paramString: Array<Class<String>?> = arrayOfNulls(2)
                paramString[0] = String::class.java
                paramString[1] = String::class.java

                val m = Log.getDeclaredMethod(level, *paramString)

                val maxLogSize = 4000
                for (i in 0..string.length / maxLogSize) {
                    val start = i * maxLogSize
                    var end = (i + 1) * maxLogSize
                    end = if (end > string.length) string.length else end
                    m.invoke(null, tag, string.substring(start, end))
                }
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
        }

        fun is_log(): Boolean {
            return DEBUG
        }

        fun isSdPresent(): Boolean {
            return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        }

        fun getToday(format: String?): String {
            return DateFormat.format(format, Date()).toString()
        }

        fun getFileDate(format: String?): String {
            val now = System.currentTimeMillis()
            val sdfNow = SimpleDateFormat(format, Locale.US)
            return sdfNow.format(Date(now))
        }

        // SharedPreference에 키/값을 설정
        @Throws(NullPointerException::class)
        fun setPreference(context: Context?, key: String?, value: String?) {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return
            val editor = settings.edit()

            editor.putString(key, value)
            editor.apply()
        }

        // SharedPreference에 키/값을 설정
        fun setPreference(context: Context?, key: String?, value: Long) {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return
            val editor = settings.edit()

            editor.putLong(key, value)
            editor.apply()
        }

        // SharedPreference에 키/값을 설정
        fun setPreference(context: Context?, key: String?, value: Int) {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return
            val editor = settings.edit()

            editor.putInt(key, value)
            editor.apply()
        }

        // SharedPreference에 키/값을 설정
        fun setPreference(context: Context?, key: String?, value: Boolean) {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return
            val editor = settings.edit()

            editor.putBoolean(key, value)
            editor.apply()
        }

        fun setPreferenceArray(context: Context?, key: String?, value: ArrayList<*>?) {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return
            val editor = settings.edit()
            val gson = instance
            val listValue = gson.toJson(value)
            editor.putString(key, listValue)
            editor.apply()
        }

        // SharedPreference에 키/값을 설정
        @Throws(NullPointerException::class)
        fun getPreference(context: Context?, key: String?): String {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            return settings?.getString(key, "") ?: ""
        }

        // SharedPreference에 키/값을 설정
        fun getPreferenceLong(context: Context?, key: String?, def_value: Long): Long {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return settings?.getLong(key, def_value) ?: 0
        }

        // SharedPreference에 키/값을 설정
        fun getPreferenceInt(context: Context?, key: String?, def_value: Int): Int {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return settings?.getInt(key, def_value) ?: 0
        }

        // SharedPreference에 키/값을 설정
        fun getPreferenceBool(context: Context?, key: String?, def_value: Boolean): Boolean {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            var ret = def_value
            try {
                ret = settings?.getBoolean(key, def_value) ?: def_value
            } catch (e: ClassCastException) {
                settings?.edit()?.putBoolean(key, def_value)
            }
            return ret
        }

        // SharedPreference에 키/값을 설정
        fun removePreference(context: Context?, key: String?) {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return
            try {
                val editor = settings.edit()
                editor.remove(key)
                editor.commit()
            } catch (e: Exception) {
            }
        }

        fun removeAllPreference(context: Context?) {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return
            try {
                val editor = settings.edit()
                editor.clear()
                editor.commit()
            } catch (e: Exception) {
            }
        }

        fun containsPreference(context: Context?, key: String?): Boolean {
            val settings = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return false

            return settings.contains(key)
        }

        fun md5(s: String): String? {
            try {
                val md = MessageDigest.getInstance("MD5")

                md.update(s.toByteArray(StandardCharsets.UTF_8), 0, s.length)

                val messageDigest = md.digest()
                val number = BigInteger(1, messageDigest)
                var md5 = number.toString(16)

                while (md5.length < 32) md5 = "0$md5"

                return md5
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                return null
            }
        }

        fun md5salt(s: String): String? {
            try {
                val md = MessageDigest.getInstance("MD5")

                val key = EXOKEY + s
                log("key :$key")
                md.update(key.toByteArray(StandardCharsets.UTF_8), 0, key.length)

                val messageDigest = md.digest()
                val number = BigInteger(1, messageDigest)
                var md5 = number.toString(16)

                while (md5.length < 32) md5 = "0$md5"

                log("result :$md5")
                return md5
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                return null
            }
        }

        //아이돌 type아 성별 나눔이 필요한지 여부를  true/false로 return해준다.
        //현재는 배우 또는 가수일때  true return
        fun isGenderTypeNeed(idolType: String): Boolean {
            return "A" == idolType || "S" == idolType
        }

        fun setGenderTypeLIst(
            typeListModels: List<TypeListModel>,
            context: Context
        ): List<TypeListModel> {
            val returnTypeList: MutableList<TypeListModel> = ArrayList()
            for (typeListModel in typeListModels) {
                if ("S" == typeListModel.type) {
                    val maleType = TypeListModel()
                    maleType.type = "SM"
                    maleType.name = context.resources.getString(R.string.actor_male_singer)
                    maleType.typeName = "type_singer"
                    maleType.isViewable = typeListModel.isViewable

                    val femaleType = TypeListModel()
                    femaleType.type = "SF"
                    femaleType.name = context.resources.getString(R.string.actor_female_singer)
                    femaleType.typeName = "type_singer"
                    femaleType.isViewable = typeListModel.isViewable

                    returnTypeList.add(maleType)
                    returnTypeList.add(femaleType)
                } else if ("A" == typeListModel.type) {
                    val maleType = TypeListModel()
                    maleType.type = "AM"
                    maleType.name = context.resources.getString(R.string.lable_actors)
                    maleType.typeName = "type_actor"
                    maleType.isViewable = typeListModel.isViewable

                    val femaleType = TypeListModel()
                    femaleType.type = "AF"
                    femaleType.name = context.resources.getString(R.string.lable_actresses)
                    femaleType.typeName = "type_actor"
                    femaleType.isViewable = typeListModel.isViewable

                    returnTypeList.add(maleType)
                    returnTypeList.add(femaleType)
                } else {
                    returnTypeList.add(typeListModel)
                }
            }
            return returnTypeList
        }

        private var progressDialog: ProgressDialog? = null

        @JvmOverloads
        fun showProgress(context: Context?, cancelable: Boolean = true) {
            if (progressDialog != null && progressDialog!!.isShowing) {
                return
            }

            // badtokenexception
            try {
                progressDialog = ProgressDialog.show(context, null, null, true, cancelable)
                // 이걸 해줘야 흰색 빈칸이 생김을 방지
                progressDialog?.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                progressDialog?.setContentView(R.layout.progress_layout)
            } catch (e: Exception) {
            }
        }

        fun showProgressWithText(
            context: Context?,
            title: String?,
            content: String?,
            cancelable: Boolean
        ) {
            if (progressDialog != null && progressDialog!!.isShowing) {
                return
            }

            // badtokenexception
            try {
                progressDialog = ProgressDialog.show(context, null, null, true, cancelable)
                // 이걸 해줘야 흰색 빈칸이 생김을 방지
                progressDialog?.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                progressDialog?.setContentView(R.layout.progress_layout)

                val titleView = progressDialog?.findViewById<AppCompatTextView>(R.id.tv_title)
                val contentView = progressDialog?.findViewById<AppCompatTextView>(R.id.tv_content)

                titleView?.text = title
                contentView?.text = content

                titleView?.visibility = View.VISIBLE
                contentView?.visibility = View.VISIBLE
            } catch (e: Exception) {
            }
        }

        fun showLottie(context: Context?, cancelable: Boolean) {
            try {
                progressDialog = ProgressDialog.show(context, null, null, true, cancelable)
                progressDialog?.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                progressDialog?.setContentView(R.layout.lottie_layout)
                progressDialog?.setCanceledOnTouchOutside(false)
            } catch (e: Exception) {
            }
        }

        fun closeProgress() {
            if (progressDialog != null) {
                try {
                    progressDialog!!.dismiss()
                    progressDialog = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun closeProgress(delay: Int) {
            Handler().postDelayed({ closeProgress() }, delay.toLong())
        }

        private var idolDialog: Dialog? = null

        // 테마픽 가이드 공통 로직
        private fun showThemeGuideInternal(
            context: Context,
            on: View,
            allLength: Int?,
            headerLength: Int?,
            isPrelaunch: Boolean = false,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            if (!areAnimatorsEnabledCompat(context)) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.5f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow

            idolDialog!!.setContentView(R.layout.theme_pick_guide_layout)

            val lottieView = idolDialog!!.findViewById<LottieAnimationView>(R.id.lt_theme_pick_guide)

            val params = lottieView.layoutParams as ConstraintLayout.LayoutParams

            params.topMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                if (isPrelaunch) 295f else 50f,
                lottieView.context.resources.displayMetrics
            ).toInt()

            lottieView.layoutParams = params

            // 단일 Lottie 파일로 변경하고 반복 재생 설정
            lottieView.setAnimation("theme_pick_guide_merged.json")
            lottieView.loop(true)
            lottieView.playAnimation()

            var statusBarHeight = 0
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
            }

            // 파라미터 유무에 따라 Y 위치 분기 처리
            if (allLength != null && headerLength != null) {
                lottieView.y = -allLength / 2f + headerLength + on.height / 2f - statusBarHeight
            } else {
                lottieView.y = on.pivotY - ((on.height / 2f) / 2f) - statusBarHeight
            }
            lottieView.x = on.pivotX - (on.width / 2f)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            val btnClosed =
                idolDialog!!.findViewById<AppCompatImageView>(R.id.theme_pick_guide_closed)
            btnClosed.setOnClickListener(listener1)
            val clRoot =
                idolDialog!!.findViewById<ConstraintLayout>(
                    R.id.theme_pick_guide_cl
                ) //가이드화면 전체 눌러도 닫아지게 리스너 등록.
            clRoot.setOnClickListener(listener2)

            ViewCompat.setOnApplyWindowInsetsListener(clRoot) { view, insets ->
                val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                view.setPadding(0, statusBarInsets.top, 0, 0)
                insets
            }

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        //테마픽 가이드 화면.
        fun showThemeGuide(
            context: Context?,
            on: View,
            allLength: Int,
            headerLength: Int,
            isPrelaunch: Boolean,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            if (context == null) return
            showThemeGuideInternal(context, on, allLength, headerLength, isPrelaunch, listener1, listener2)
        }

        fun areAnimatorsEnabledCompat(ctx: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= 26) {
                // O 이상: 공식 API
                ValueAnimator.areAnimatorsEnabled()
            } else {
                // 하위 버전: 설정값 직접 읽기 (아래 2번)
                readAnimatorScale(ctx) > 0f
            }
        }

        @Suppress("DEPRECATION")
        fun readAnimatorScale(ctx: Context): Float {
            // 1) Global 우선
            val global = runCatching {
                Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE)
            }.getOrNull()
            if (global != null) return global

            // 2) 구형 OS 대비: System 키 시도 (API 16 이하)
            val system = runCatching {
                Settings.System.getFloat(ctx.contentResolver, Settings.System.ANIMATOR_DURATION_SCALE)
            }.getOrNull()

            // 읽기 실패 시 기본값 1f
            return system ?: 1f
        }

        fun showChargeDiamondWithBtn1(
            context: Context?,
            title: String?,
            msg: String?,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?,
            listener3: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.5f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            idolDialog!!.setContentView(R.layout.dialog_charge_diamond)

            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val heartShop =
                idolDialog!!.findViewById<LinearLayoutCompat>(R.id.diashop_container)
            heartShop.setOnClickListener(listener1)
            val diashopArrow =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.diashop_arrow)
            diashopArrow.setOnClickListener(listener2)

            val dialog_close_btn =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_close)
            dialog_close_btn.setOnClickListener(listener3)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        //채팅룸 생성시 보여줘야 되는
        fun showChatRoomCreateDialogWithBtn2(
            context: Context?,
            listener: View.OnClickListener?,
            listener1: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.5f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            idolDialog!!.setContentView(R.layout.dialog_create_chat_room_instruction)
            val btnDialogConfirm =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_confirm)
            btnDialogConfirm.setOnClickListener(listener1)

            val btnDialogNeverShown =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_never_show_again)
            btnDialogNeverShown.setOnClickListener(listener)
            val tvDiaCountInfo =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_dia_count_info)
            tvDiaCountInfo.text = String.format(
                context.resources.getString(R.string.chat_make_help_popup4),
                ConfigModel.getInstance(context).chatRoomDiamond
            )

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun showIdolDialogWithBtn1(
            context: Context?, title: String?,
            msg: String?, listener1: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.5f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val dialog_tv_title: AppCompatTextView
            if (title == null) {
                idolDialog!!.setContentView(R.layout.dialog_idol_notitle_one_btn)
            } else {
                idolDialog!!.setContentView(R.layout.dialog_idol_one_btn)
                dialog_tv_title = idolDialog!!.findViewById(R.id.title)
                dialog_tv_title.text = title
            }
            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            dialog_tv_msg.text = msg

            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun showDefaultIdolDialogWithBtn1(
            context: Context?,
            title: String?,
            msg: String?,
            imageId: Int,
            listener1: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val dialog_tv_title: AppCompatTextView

            if (title == null) {
                idolDialog!!.setContentView(R.layout.dialog_default_idol_notitle_one_btn)
            } else {
                idolDialog!!.setContentView(R.layout.dialog_default_idol_one_btn)
                dialog_tv_title = idolDialog!!.findViewById(R.id.title)
                dialog_tv_title.text = title
            }
            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            dialog_tv_msg.text = msg

            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val character_img =
                idolDialog!!.findViewById<AppCompatImageView>(R.id.character_img)
            if (imageId != 0) {
                character_img.visibility = View.VISIBLE
                character_img.setImageResource(imageId)
            } else {
                character_img.visibility = View.GONE
            }
        }


        fun showDefaultIdolDialogWithBtn1(
            context: Context?,
            title: String?, msg: String?, listener1: View.OnClickListener?
        ) {
            if (context == null || msg == null) return
            cleanIdolDialog()

            idolDialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val dialog_tv_title: AppCompatTextView
            if (title == null) {
                idolDialog!!.setContentView(R.layout.dialog_default_idol_notitle_one_btn)
            } else {
                idolDialog!!.setContentView(R.layout.dialog_default_idol_one_btn)
                dialog_tv_title = idolDialog!!.findViewById(R.id.title)
                dialog_tv_title.text = title
            }
            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            dialog_tv_msg.text = msg

            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        //휴먼체크용으로  간단 한 산수를 문제내는  팝업
        fun showHumanCheckDialog(
            context: Context?,
            humanCheckCallbackListener: (Boolean) -> Unit
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!
                .setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            var tvDescription: AppCompatTextView //팝업 설명 tv


            idolDialog!!.setContentView(R.layout.dialog_human_check)
            val tvQuestion =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_math_question) //문제 tv


            //1~4번보기
            val btnAnswer1 = idolDialog!!.findViewById<AppCompatButton>(R.id.answer1)
            val btnAnswer2 = idolDialog!!.findViewById<AppCompatButton>(R.id.answer2)
            val btnAnswer3 = idolDialog!!.findViewById<AppCompatButton>(R.id.answer3)
            val btnAnswer4 = idolDialog!!.findViewById<AppCompatButton>(R.id.answer4)

            val answerButtons = arrayOf(btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4)

            val random = Random()

            //일의 자리수 1~9 사이 랜덤으로 두개 뽑아서 덧셈 문제 제작
            val frontNumber = random.nextInt(9) + 1 //앞자리
            val backNumber = random.nextInt(9) + 1 //뒷자리

            val answerNumber = frontNumber + backNumber


            //문제 string
            val mathQuizContent = "$frontNumber + $backNumber = ?"
            tvQuestion.text = mathQuizContent


            //랜덤으로 anwer 1~4중  정답이 들어갈 보기 선택
            val realAnswerBtn = random.nextInt(4) + 1
            val nonAnswerNumbers = ArrayList<Int>()
            var i = 1
            while (i <= 4) {
                if (realAnswerBtn == i) {
                    answerButtons[i - 1].text = answerNumber.toString()
                } else {
                    val nonAnswerNumber = random.nextInt(19)
                    v(
                        nonAnswerNumbers.toString() + "nonanswer -:" + nonAnswerNumber + " ->" + nonAnswerNumbers.contains(
                            nonAnswerNumber
                        )
                    )
                    if (nonAnswerNumber != answerNumber && !(nonAnswerNumbers.contains(
                            nonAnswerNumber
                        ))
                    ) {
                        nonAnswerNumbers.add(nonAnswerNumber)
                        answerButtons[i - 1].text = nonAnswerNumber.toString()
                    } else {
                        --i //보기 버튼들에 랜덤 숫자가 set 되지 않았으면 다시 해당 index를 for문  돌린다.
                    }
                }
                i++
            }

            btnAnswer1.setOnClickListener { v: View? ->
                humanCheckCallbackListener(
                    realAnswerBtn == 1
                )
            }
            btnAnswer2.setOnClickListener { v: View? ->
                humanCheckCallbackListener(
                    realAnswerBtn == 2
                )
            }
            btnAnswer3.setOnClickListener { v: View? ->
                humanCheckCallbackListener(
                    realAnswerBtn == 3
                )
            }
            btnAnswer4.setOnClickListener { v: View? ->
                humanCheckCallbackListener(
                    realAnswerBtn == 4
                )
            }

            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        fun showDefaultIdolDialogWithBtn1(
            context: Context?,
            title: String?,
            msg: SpannableString?,
            listener1: View.OnClickListener?,
            outsideCancel: Boolean
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val dialog_tv_title: AppCompatTextView
            if (title == null) {
                idolDialog!!
                    .setContentView(R.layout.dialog_default_idol_notitle_one_btn)
            } else {
                idolDialog!!.setContentView(R.layout.dialog_default_idol_one_btn)
                dialog_tv_title = idolDialog!!.findViewById(R.id.title)
                dialog_tv_title.text = title
            }
            idolDialog!!.setCanceledOnTouchOutside(outsideCancel)
            idolDialog!!.setCancelable(outsideCancel)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            dialog_tv_msg.text = msg

            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        //아이돌id, 유저id
        fun noProfileImage(id: Int): Int {
            return if (id % 2 == 0) {
                R.drawable.menu_profile_2
            } else {
                R.drawable.menu_profile_1
            }
        }

        //테마픽용(이미지 안나올떄 사용).
        fun noProfileThemePickImage(id: Int): Int {
            return if (id % 2 == 0) {
                R.drawable.menu_theme_profile_2
            } else {
                R.drawable.menu_theme_profile_1
            }
        }

        fun showDefaultIdolDialogWithBtn1(
            context: Context?,
            title: String?, msg: String?, listener1: View.OnClickListener?, outsideCancel: Boolean
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val dialog_tv_title: AppCompatTextView
            if (title == null) {
                idolDialog!!
                    .setContentView(R.layout.dialog_default_idol_notitle_one_btn)
            } else {
                idolDialog!!.setContentView(R.layout.dialog_default_idol_one_btn)
                dialog_tv_title = idolDialog!!.findViewById(R.id.title)
                dialog_tv_title.text = title
            }
            idolDialog!!.setCanceledOnTouchOutside(outsideCancel)
            idolDialog!!.setCancelable(outsideCancel)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            dialog_tv_msg.text = msg

            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        fun showVideoAdDisableTimerDialog(
            context: Context?,
            fragmentManager: FragmentManager,
            listener1: View.OnClickListener?,
            isAlreadySetNotification: Boolean,
            outsideCancel: Boolean,
            onClickNotification: () -> Unit = {}
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            idolDialog!!.setContentView(R.layout.dialog_video_ad_timer)

            idolDialog!!.setCanceledOnTouchOutside(outsideCancel)
            idolDialog!!.setCancelable(outsideCancel)
            val dialog_tv_remain_time =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_video_timer_remain_time)

            videoDisableTimer(context, dialog_tv_remain_time, null, null)

            val dialogTvBtn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialogTvBtn1.setOnClickListener(listener1)

            val dialogTooltip =
                idolDialog!!.findViewById<ConstraintLayout>(R.id.cl_tool_tip)
            val dialogTooltipText =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_tooltip_down)

            val bonusHeart = ConfigModel.getInstance(context).videoAdBonusHeart
            if (bonusHeart != 0) {
                dialogTooltip.visibility = View.VISIBLE
                dialogTooltipText.text = context.getString(R.string.video_ad_timer_bonus, bonusHeart.toString())
            }

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            val btnNotification =
                idolDialog!!.findViewById<LinearLayoutCompat>(R.id.ll_notification)

            if (isAlreadySetNotification) {
                val ivNotification =
                    idolDialog!!.findViewById<AppCompatImageView>(R.id.iv_notification_icon)
                val tvNotification =
                    idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_notification)

                ivNotification.apply {
                    setImageResource(R.drawable.icon_check)
                    ImageViewCompat.setImageTintList(this, ContextCompat.getColorStateList(context, R.color.text_dimmed))
                }
                tvNotification.apply {
                    text = context.getString(R.string.video_ad_timer_alert_done)
                    setTextColor(context.getColor(R.color.text_dimmed))
                }
            } else {
                btnNotification.setOnClickListener {
                    val isNotificationOn =
                        NotificationManagerCompat.from(context).areNotificationsEnabled()

                    if (!isNotificationOn) {
                        val dialog = NotificationSettingDialog() {
                        }
                        dialog.show(fragmentManager, "notification_setting_dialog")
                    } else {
                        btnNotification.setOnClickListener(null)

                        val ivNotification =
                            idolDialog!!.findViewById<AppCompatImageView>(R.id.iv_notification_icon)
                        val tvNotification =
                            idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_notification)

                        ivNotification.apply {
                            setImageResource(R.drawable.icon_check)
                            ImageViewCompat.setImageTintList(this, ContextCompat.getColorStateList(context, R.color.text_dimmed))
                        }
                        tvNotification.apply {
                            text = context.getString(R.string.video_ad_timer_alert_done)
                            setTextColor(context.getColor(R.color.text_dimmed))
                        }
                        onClickNotification()
                    }
                }
            }

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun checkVideoAdTimer(context: Context) {
            if (idolDialog?.isShowing == true) {
                val timerView = idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_video_timer_remain_time)
                videoDisableTimer(context, timerView, null, null)
            }
        }

        fun showDefaultIdolDialogWithBtn2(
            context: Context?,
            title: String?, msg: String?,
            btn1ResId: Int, btn2ResId: Int,
            btn1IsRed: Boolean, btn2IsRed: Boolean,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val dialog_tv_title: AppCompatTextView
            if (title == null) {
                idolDialog!!
                    .setContentView(R.layout.dialog_default_idol_notitle_two_btn)
            } else {
                idolDialog!!.setContentView(R.layout.dialog_default_idol_two_btn)
                dialog_tv_title = idolDialog!!.findViewById(R.id.title)
                dialog_tv_title.text = title
            }
            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            dialog_tv_msg.text = msg

            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            val dialog_tv_btn2 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
            dialog_tv_btn2.setOnClickListener(listener2)

            if (btn1IsRed) {
                dialog_tv_btn1.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.btn_text_brand500
                    )
                )
            }
            if (btn2IsRed) {
                dialog_tv_btn2.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.btn_text_brand500
                    )
                )
            }
            dialog_tv_btn1.setText(btn1ResId)
            dialog_tv_btn2.setText(btn2ResId)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        //부분 스트링 spannable 처리
        fun showDefaultIdolDialogWithBtn2(
            context: Context?,
            title: String?, msg: String, spanString: String,
            btn1ResId: Int, btn2ResId: Int,
            btn1IsRed: Boolean, btn2IsRed: Boolean,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val dialog_tv_title: AppCompatTextView
            if (title == null) {
                idolDialog!!
                    .setContentView(R.layout.dialog_default_idol_notitle_two_btn)
            } else {
                idolDialog!!.setContentView(R.layout.dialog_default_idol_two_btn)
                dialog_tv_title = idolDialog!!.findViewById(R.id.title)
                dialog_tv_title.text = title
            }
            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            val count = String.format(msg, spanString)
            var spannable = SpannableString(count)
            spannable =
                getColorText(spannable, spanString, ContextCompat.getColor(context, R.color.main))
            dialog_tv_msg.text = spannable

            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            val dialog_tv_btn2 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
            dialog_tv_btn2.setOnClickListener(listener2)

            if (btn1IsRed) {
                dialog_tv_btn1.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.btn_text_brand500
                    )
                )
            }

            if (btn2IsRed) {
                dialog_tv_btn2.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.btn_text_brand500
                    )
                )
            }
            dialog_tv_btn1.setText(btn1ResId)
            dialog_tv_btn2.setText(btn2ResId)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun showDefaultIdolDialogWithBtn2(
            context: Context?,
            title: String?, msg: String, spanString1: String, spanString2: String,
            btn1ResId: Int, btn2ResId: Int,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val dialog_tv_title: AppCompatTextView
            if (title == null) {
                idolDialog!!
                    .setContentView(R.layout.dialog_default_idol_notitle_two_btn)
            } else {
                idolDialog!!.setContentView(R.layout.dialog_default_idol_two_btn)
                dialog_tv_title = idolDialog!!.findViewById(R.id.title)
                dialog_tv_title.text = title
            }
            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            val count1 = String.format(msg, spanString1, spanString2)
            var spannable1 = SpannableString(count1)
            spannable1 = getColorText(
                spannable1,
                spanString1,
                spanString2,
                ContextCompat.getColor(context, R.color.main)
            )
            dialog_tv_msg.text = spannable1


            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            val dialog_tv_btn2 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
            dialog_tv_btn2.setOnClickListener(listener2)

            dialog_tv_btn1.setText(btn1ResId)
            dialog_tv_btn2.setText(btn2ResId)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        fun showDefaultIdolDialogWithRedBtn2(
            context: Context?,
            title: String?, msg: String, spanString: String,
            btn1ResId: Int, btn2ResId: Int, check: Boolean,
            titleIsRed: Boolean, btn1IsRed: Boolean, btn2IsRed: Boolean,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            if (titleIsRed)  //두 레이아웃 view들은 모두 똑같은데 색깔하고 간격이 달라서 나눠줬음.
                idolDialog!!.setContentView(R.layout.dialog_default_idol_two_red_btn)
            else idolDialog!!.setContentView(R.layout.dialog_attendance_two_red_btn)

            val dialog_tv_title =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.title)
            dialog_tv_title.text = title
            val redLi =
                idolDialog!!.findViewById<LinearLayoutCompat>(R.id.red_li)

            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            val count = String.format(msg, spanString)
            var spannable = SpannableString(count)

            if (title == null) {
                log("RedButton:: is gone")
                dialog_tv_title.visibility = View.GONE
                redLi.setPadding(0, 100, 0, 0)
            }

            spannable = if (check) getColorText(
                spannable,
                spanString,
                ContextCompat.getColor(context, R.color.main)
            )
            else getColorText(
                spannable,
                spanString,
                ContextCompat.getColor(context, R.color.gray580)
            )

            dialog_tv_msg.text = spannable

            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            val dialog_tv_btn2 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
            dialog_tv_btn2.setOnClickListener(listener2)

            if (btn1IsRed) {
                dialog_tv_btn1.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.btn_text_brand500
                    )
                )
            }

            if (btn2IsRed) {
                dialog_tv_btn2.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.btn_text_brand500
                    )
                )
            }

            dialog_tv_btn1.setText(btn1ResId)
            dialog_tv_btn2.setText(btn2ResId)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        //테마픽 다이아 투표용.
        fun showThemePickDiaDialogWithRedBtn2(
            context: Context?,
            msg: String?,
            myDiamond: String,
            btn1ResId: Int, btn2ResId: Int,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            idolDialog!!.setContentView(R.layout.dialog_theme_pick_dia_two_red_btn)
            val dialog_tv_my_diamond =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.my_diamond)
            dialog_tv_my_diamond.text =
                String.format(
                    context.getString(R.string.heart_count_format),
                    myDiamond
                )
            val redLi =
                idolDialog!!.findViewById<LinearLayoutCompat>(R.id.red_li)

            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            dialog_tv_msg.text = msg

            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            val dialog_tv_btn2 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
            dialog_tv_btn2.setOnClickListener(listener2)

            dialog_tv_btn1.setText(btn1ResId)
            dialog_tv_btn2.setText(btn2ResId)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        //취소 버튼 빨간색인 다이얼로
        fun showDialogCancelRedBtn2(
            context: Context?,
            msg: String, spanString1: String, spanString2: String,
            btn1ResId: Int, btn2ResId: Int,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            idolDialog!!.setContentView(R.layout.dialog_cancel_red_two_btn)
            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(true)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            val count1 = String.format(msg, spanString1, spanString2)
            var spannable1 = SpannableString(count1)
            spannable1 = getColorText(
                spannable1,
                spanString1,
                spanString2,
                ContextCompat.getColor(context, R.color.main)
            )
            dialog_tv_msg.text = spannable1


            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            val dialog_tv_btn2 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
            dialog_tv_btn2.setOnClickListener(listener2)

            dialog_tv_btn1.setText(btn1ResId)
            dialog_tv_btn2.setText(btn2ResId)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 확인 버튼 하나이고, 이미지 넣을 수 있는 다이얼로그
        fun showImageDialogBtn1(
            context: Context?,
            title: String?,
            msg: String?,
            imageResId: Int,
            listener1: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            idolDialog!!.setContentView(R.layout.dialog_image_one_btn)
            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)

            val tvMsg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_msg)
            val tvTitle =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_title)
            val dialogImage =
                idolDialog!!.findViewById<AppCompatImageView>(R.id.iv_dialog)

            tvTitle.text = title
            tvMsg.text = msg

            if (imageResId != 0) {
                dialogImage.visibility = View.VISIBLE
                dialogImage.setImageResource(imageResId)
            } else {
                dialogImage.visibility = View.GONE
            }


            val btnConfirm =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_confirm)
            btnConfirm.setOnClickListener(listener1)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 예/아니오 빨간색 지정할 수 있고, 다이얼로그 + 이미지 있는 경우
        fun showImageDialogCancelRedBtn2(
            context: Context?,
            msg: String, spanString1: String, spanString2: String,
            btn1ResId: Int, btn2ResId: Int,
            imageResId: Int,
            btn1IsRed: Boolean, btn2IsRed: Boolean,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            idolDialog!!.setContentView(R.layout.dialog_image_cancel_red_two_btn)
            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            val dialogImage =
                idolDialog!!.findViewById<AppCompatImageView>(R.id.iv_dialog)
            val count1 = String.format(msg, spanString1, spanString2)
            var spannable1 = SpannableString(count1)
            spannable1 = getColorText(
                spannable1,
                spanString1,
                spanString2,
                ContextCompat.getColor(context, R.color.main)
            )
            dialog_tv_msg.text = spannable1

            if (imageResId != 0) {
                dialogImage.visibility = View.VISIBLE
                dialogImage.setImageResource(imageResId)
            } else {
                dialogImage.visibility = View.GONE
            }


            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            dialog_tv_btn1.setOnClickListener(listener1)
            val dialog_tv_btn2 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
            dialog_tv_btn2.setOnClickListener(listener2)

            if (btn1IsRed) {
                dialog_tv_btn1.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.btn_text_brand500
                    )
                )
            }

            if (btn2IsRed) {
                dialog_tv_btn2.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.btn_text_brand500
                    )
                )
            }
            dialog_tv_btn1.setText(btn1ResId)
            dialog_tv_btn2.setText(btn2ResId)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun createIdolDialog(
            context: Context?,
            title: String?, msg: String?,
            btn1ResId: Int, btn2ResId: Int,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val dialog_tv_title: AppCompatTextView
            if (title == null) {
                idolDialog!!
                    .setContentView(R.layout.dialog_default_idol_notitle_two_btn)
            } else {
                idolDialog!!.setContentView(R.layout.dialog_default_idol_two_btn)
                dialog_tv_title = idolDialog!!.findViewById(R.id.title)
                dialog_tv_title.text = title
            }
            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val dialog_tv_msg =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
            dialog_tv_msg.text = msg

            val dialog_tv_btn1 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
            //        dialog_tv_btn1.setBackgroundResource(R.drawable.btn_off_gray);
            dialog_tv_btn1.setOnClickListener(listener1)
            val dialog_tv_btn2 =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
            dialog_tv_btn2.setOnClickListener(listener2)

            dialog_tv_btn1.setText(btn1ResId)
            dialog_tv_btn2.setText(btn2ResId)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun showDefaultIdolDialogWithBtn3(
            context: Context?,
            title: String?, msg: String?,
            btn1ResId: Int, btn2ResId: Int,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            try {
                createIdolDialog(context, title, msg, btn1ResId, btn2ResId, listener1, listener2)
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // SpnnableString를 보여주는 다이얼로그
        fun showDefaultIdolDialogWithBtn3(
            context: Context?,
            title: String?, msg: SpannableString,
            btn1ResId: Int, btn2ResId: Int,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            try {
                createIdolDialog(
                    context,
                    title,
                    msg.toString(),
                    btn1ResId,
                    btn2ResId,
                    listener1,
                    listener2
                )
                val dialog_tv_msg = idolDialog!!.findViewById<TextView>(R.id.message)
                dialog_tv_msg.text = msg

                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun showDefaultIdolDialogWithBtn2(
            context: Context?,
            title: String?, msg: String?, listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            showDefaultIdolDialogWithBtn2(
                context,
                title,
                msg,
                R.string.yes,
                R.string.no,
                false,
                false,
                listener1,
                listener2
            )
        }

        //부분 스트링 spannable 처리
        fun showDefaultIdolDialogWithBtn2(
            context: Context?,
            title: String?,
            msg: String,
            spanString: String,
            btn1ResId: Int,
            btn2ResId: Int,
            listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            showDefaultIdolDialogWithBtn2(
                context,
                title,
                msg,
                spanString,
                btn1ResId,
                btn2ResId,
                false,
                false,
                listener1,
                listener2
            )
        }

        //    public static void showDefaultIdolDialogWithBtn3(Context context,
        //            String title, String msg, View.OnClickListener listener1,
        //            View.OnClickListener listener2) {
        //
        //        showDefaultIdolDialogWithBtn3(context, title, msg, R.string.yes, R.string.no, listener1, listener2);
        //    }
        //
        //    public static void showDefaultIdolDialogWithBtn3(Context context,
        //            String title, CharSequence msg, View.OnClickListener listener1,
        //            View.OnClickListener listener2) {
        //
        //        showDefaultIdolDialogWithBtn3(context, title, msg.toString(), R.string.yes, R.string.no, listener1, listener2);
        //    }
        // SpnnableString를 보여주는 다이얼로그
        fun showDefaultIdolDialogWithBtn3(
            context: Context?,
            title: String?, msg: SpannableString, listener1: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            showDefaultIdolDialogWithBtn3(
                context,
                title,
                msg,
                R.string.yes,
                R.string.no,
                listener1,
                listener2
            )
        }


        fun adjustIdolDialogHeight(context: Context?, idolDialog: Dialog) {
            // 다이얼로그가 매우 길어져서 화면에서 넘치는것 방지
            idolDialog.window?.decorView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
                OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    idolDialog.window
                        ?.getDecorView()
                        ?.viewTreeObserver
                        ?.removeOnGlobalLayoutListener(this)

                    val displayRectangle = Rect()

                    val activity = getActivityFromContext(context)

                    val window = activity!!.window
                    window.decorView.getWindowVisibleDisplayFrame(displayRectangle)

                    if (idolDialog.window!!.decorView.height > displayRectangle.height() * 0.6) {
                        val scrollView =
                            idolDialog.window!!.decorView.findViewById<ScrollView>(R.id.scroll)
                        if (scrollView != null) {
                            val lp = scrollView.layoutParams as ViewGroup.LayoutParams
                            lp.height = (displayRectangle.height() * 0.6).toInt()
                            scrollView.layoutParams = lp
                        }
                    }
                }
            })
        }

        fun closeIdolDialog() {
            if (idolDialog != null && idolDialog!!.isShowing) {
                try {
                    idolDialog!!.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun isDialogShowing(): Boolean {
            //다이얼로그  켜져있는지 여부 체크
            return idolDialog != null && idolDialog!!.isShowing
        }

        //권한 요청 팝업.
        fun showRequestAuthDialog(
            context: Context?,
            glideRequestManager: RequestManager?,
            iconUrl: String?,
            title: String?,
            accessModelList: List<AccessModel>,
            listener: View.OnClickListener?,
            listener2: View.OnClickListener?
        ) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            idolDialog!!.setContentView(R.layout.dialog_popup_request_auth)

            val requestAuthIcon =
                idolDialog!!.findViewById<AppCompatImageView>(R.id.iv_request_auth)
            glideRequestManager?.load(iconUrl)
                ?.into(requestAuthIcon)

            val textAuthTitle =
                idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_request_auth_title)
            textAuthTitle.text = title

            val tvMainCategory = idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_main_category)
            val tvSubCategory = idolDialog!!.findViewById<AppCompatTextView>(R.id.tv_sub_category)

            for (i in accessModelList.indices) {
                if (i == 0) {
                    tvMainCategory.text = accessModelList[i].title

                    val permissionsModelList = accessModelList[i].permissions

                    val subCategory = StringBuilder()

                    for (j in permissionsModelList.indices) {
                        v("AuthRequest::---" + permissionsModelList[j].name)
                        if (j == 0) {
                            subCategory.append(permissionsModelList[j].name)
                        } else {
                            subCategory.append(", ").append(permissionsModelList[j].name)
                        }
                    }
                    tvSubCategory.text = subCategory.toString()
                }
            }

            val btnAgreeAndContinue =
                idolDialog!!.findViewById<AppCompatButton>(R.id.btn_agree_and_continue)
            btnAgreeAndContinue.setOnClickListener(listener)

            val btnCancel = idolDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
            btnCancel.setOnClickListener(listener2)

            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                adjustIdolDialogHeight(
                    context,
                    idolDialog!!
                )
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun isFoundString(wr_id: String?, read_string_array: Array<String>): Boolean {
            for (element in read_string_array) {
                if (wr_id == element) {
                    return true
                }
            }
            return false
        }

        fun gotoMarket(activity: Activity?, packageName: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setData(
                    Uri.parse(
                        "market://details?id="
                                + packageName
                    )
                )
                activity!!.startActivity(intent)
            } catch (anfe: ActivityNotFoundException) {
                if (activity != null) {
                    makeText(activity, R.string.msg_error_ok, Toast.LENGTH_SHORT).show()
                }
                anfe.printStackTrace()
            }
        }


        fun xor(data: ByteArray, key: ByteArray): ByteArray {
            val orgLength = data.size
            val keyLength = key.size
            val converted = ByteArray(orgLength)
            var i = 0
            var j = 0
            while (i < orgLength) {
                converted[i] = (data[i].toInt() xor key[j].toInt()).toByte()
                if (j >= (keyLength - 1)) {
                    j = -1
                }
                i++
                j++
            }
            return converted
        }

        fun getOrdinalNumber(i: Int): String {
            val suffixes = arrayOf(
                "th", "st", "nd", "rd", "th", "th",
                "th", "th", "th", "th"
            )
            return when (i % 100) {
                11, 12, 13 -> i.toString() + "th"
                else -> i.toString() + suffixes[i % 10]

            }
        }

        fun getSystemLanguage(context: Context?): String {
            var defaultLocaleString = ""
            try {
                defaultLocaleString =
                    LocaleList.getDefault()[0].language + "_" + LocaleList.getDefault()[0].country

                val prefLocale = getPreference(context, Const.PREF_LANGUAGE)

                if (prefLocale != null && !prefLocale.isEmpty()) {
                    defaultLocaleString = prefLocale
                }
            } catch (e: Exception) {
                e.printStackTrace()
                defaultLocaleString = Const.locales[1]
            }
            return defaultLocaleString
        }

        fun systemLanguageCheck(context: Context?, list: List<*>): Boolean {
            val locale = getSystemLanguage(context)
            for (i in list.indices) {
                if (locale.startsWith(list[i].toString())) {
                    return true
                }
            }
            return false
        }

        fun getCurrentLanguage(context: Context): String {
            val localeString = getPreference(context, Const.PREF_LANGUAGE)
            if (localeString == null || localeString.length < 2) {
                return context.resources.getString(R.string.language_default)
            }

            for (i in 1..<Const.languages.size) {
                val lang = Const.languages[i]
                // 중국어는 둘 다 zh라서
                if (localeString == Const.locales[i]) {
                    return context.resources.getString(lang)
                }
            }
            // 완전히 일치하는게 없으면 앞 2자리 비교
            for (i in 1..<Const.languages.size) {
                val lang = Const.languages[i]
                // 중국어는 둘 다 zh라서
                if (localeString.substring(0, 2) == Const.locales[i].substring(0, 2)) {
                    return context.resources.getString(lang)
                }
            }

            return context.resources.getString(R.string.language_default)
        }

        fun nameSplit(context: Context?, mIdol: IdolModel?): Array<String> {
            val resultName = arrayOf("", "")
            if (mIdol != null) {
                if (mIdol.getName(context).contains("_")) {
                    resultName[0] =
                        mIdol.getName(context).split("_".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                    resultName[1] = mIdol.getName(context).split((resultName[0] + "_").toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]
                } else {
                    resultName[0] = mIdol.getName(context)
                    resultName[1] = ""
                }
            } else {
                resultName[0] = ""
                resultName[1] = ""
            }

            return resultName
        }

        // 명전 등에서 사용하는 함수
        fun nameSplit(name: String): Array<String> {
            val resultName = arrayOf("", "")
            if (name.contains("_")) {
                resultName[0] = name.split("_".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0]
                resultName[1] =
                    name.split((resultName[0] + "_").toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]
            } else {
                resultName[0] = name
                resultName[1] = ""
            }

            return resultName
        }

        fun getDensity(context: Context): Float {
            val dm = DisplayMetrics()
            (context as Activity).windowManager.defaultDisplay
                .getMetrics(dm)
            return dm.density
        }

        fun getDeviceWidth(context: Context?): Int {
            if (context == null) return 0
            val activity = context.safeActivity
            var dm = DisplayMetrics()
            if (activity != null) {
                activity.windowManager.defaultDisplay.getMetrics(dm)
            } else {
                dm = Resources.getSystem().displayMetrics
            }
            return dm.widthPixels
        }

        fun getDeviceHeight(context: Context): Int {
            val dm = DisplayMetrics()
            (context as Activity).windowManager.defaultDisplay
                .getMetrics(dm)
            return dm.heightPixels
        }

        fun getOnDemandImageSize(context: Context?): String {
            if (context == null) return Const.IMAGE_SIZE_MEDIUM
            // TODO: 데이터 절약 모드일 때, IMAGE_SIZE_LOW도 넣어주기
            return if (getDeviceWidth(context) > Const.IMAGE_SIZE_STANDARD) {
                Const.IMAGE_SIZE_HIGH
            } else {
                Const.IMAGE_SIZE_MEDIUM
            }
        }

        //다크모드 판단용
        fun isDarkTheme(activity: Activity?): Boolean {
            return isUsingNightModeResources(activity) // 중복이라 이거 호출하는거로 변경
        }


        fun convertDpToPixel(context: Context?, dp: Float): Float {
            val resources = context?.resources ?: return 0f
            val metrics = resources.displayMetrics
            val px = dp * (metrics.densityDpi / 160f)
            return px
        }

        fun convertPixelsToDp(context: Context?, px: Float): Float {
            val resources = context?.resources ?: return 0f
            val metrics = resources.displayMetrics
            val dp = px / (metrics.densityDpi / 160f)
            return dp
        }

        fun getLevelResId(context: Context?, level: Int): Int {
            if (context == null) return R.drawable.icon_level_0
            var level = level
            var resId = -1

            // 나중에 최고 레벨이 오르면 여기를 수정
            if (level > Const.MAX_LEVEL) level = Const.MAX_LEVEL

            //아랍 숫자 문자만 다르니까 모두 영어로 통일.
            val resName = String.format(Locale.ENGLISH, "icon_level_%d", level)
            var resContext: Context? = null
            try {
                resContext = context.createPackageContext(context.packageName, 0)
                val res = resContext.resources

                resId = res.getIdentifier(resName, "drawable", context.packageName)
                if (resId == 0) {
                    resId = R.drawable.icon_level_0
                }
            } catch (e: PackageManager.NameNotFoundException) {
                resId = R.drawable.icon_level_0
            }

            return resId
        }


        // 해당 유저의 레벨 아이콘 및 뱃지 아이콘이 포함된 이미지 리턴
        fun getLevelImage(context: Context?, user: UserModel?): Bitmap? {
            if (context == null) return null
            var level = 0
            if (user != null) {
                level = user.level
            }
            val drawable = ContextCompat.getDrawable(context, getLevelResId(context, level))
            val levelBitmap = getBitmapFromVectorDrawable(context, drawable)
            var result = levelBitmap

            if (user != null) {
                val userItem = user.itemNo

                // 보안관
                if ((userItem and 0x01) == 0x01) {
                    val b = BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.policebadge_level_icon
                    )
                    result = combineImages(result!!, b)
                }
                // 메신저
                if ((userItem and 0x02) == 0x02) {
                    val b = BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.messengerbadge_level_icon
                    )
                    result = combineImages(result!!, b)
                }
                // 메신저
                if ((userItem and 0x04) == 0x04) {
                    val b = BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.allinbadge_level_icon
                    )
                    result = combineImages(result!!, b)
                }

                if (user.heart == Const.LEVEL_MANAGER) {
                    result = BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.manager_level
                    )
                }
            }

            return result
        }

        // 해당 유저의 레벨 아이콘 이미지 리턴
        fun getLevelImageDrawable(context: Context?, level: Int): Drawable? {
            if (context == null) return null
            val drawable = ContextCompat.getDrawable(context, getLevelResId(context, level))

            return drawable
        }

        // 해당 유저의 몰빵일 아이콘 이미지 리턴
        fun getBadgeImage(context: Context?, userItemNo: Int): Bitmap? {
            if (context == null) return null
            var levelBitmap: Bitmap? = null

            //        Logger.Companion.v("user다 user "+user.getItemNo());
//        if (user != null) {
//            int userItem = user.getItemNo();

            // 몰빵일뱃지
            if ((userItemNo and 0x04) == 0x04) {
                levelBitmap = BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.allinbadge_level_icon
                )
                //            }
            }
            return levelBitmap
        }

        // 채팅전용 아이콘 가져오기.
        fun getLevelImageDrawable(context: Context?, user: ChatMembersModel?): Drawable? {
            var level = 0
            if (user != null) {
                level = user.level
            }
            val result = getLevelImageDrawable(context, level)

            return result
        }

        fun combineImages(
            c: Bitmap,
            s: Bitmap
        ): Bitmap { // can add a 3rd parameter 'String loc' if you want to save the new image - left some code to do that at the bottom
            var cs: Bitmap? = null
            var height = 0

            val width = c.width + s.width
            height = c.height

            cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val comboImage = Canvas(cs)

            comboImage.drawBitmap(c, 0f, 0f, null)
            comboImage.drawBitmap(s, c.width.toFloat(), 0f, null)

            return cs
        }

        fun combineImages2(c: Bitmap): Bitmap { // can add a 3rd parameter 'String loc' if you want to save the new image - left some code to do that at the bottom
            var cs: Bitmap? = null
            var height = 0

            val width = c.width
            height = c.height

            cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val comboImage = Canvas(cs)

            comboImage.drawBitmap(c, 0f, 0f, null)

            return cs
        }

        fun putLocalCache(key: String, `val`: String) {
            val cachedFile = File(
                Environment.getExternalStorageDirectory(),
                Const.LOCAL_CACHE_FILE
            )
            var map: MutableMap<String, String> = HashMap()
            if (cachedFile.exists()) {
                map = HashMap(localCacheMap)
                cachedFile.delete()
            }
            map[key] = `val`
            try {
                val fos = FileOutputStream(cachedFile)
                val writer = BufferedWriter(
                    OutputStreamWriter(
                        fos
                    )
                )
                for ((key1, value) in map) {
                    writer.write("$key1=$value\n")
                }
                writer.flush()
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        val localCacheMap: Map<String, String>
            get() {
                val map: MutableMap<String, String> =
                    HashMap()
                val cachedFile = File(
                    Environment.getExternalStorageDirectory(),
                    Const.LOCAL_CACHE_FILE
                )
                try {
                    val fis = FileInputStream(cachedFile)
                    val reader = BufferedReader(
                        InputStreamReader(
                            fis
                        )
                    )
                    var entry: String
                    while ((reader.readLine().also { entry = it }) != null) {
                        if (!TextUtils.isEmpty(entry)) {
                            val pair =
                                entry.split("=".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            if (pair.size < 2) {
                                continue
                            }
                            map[pair[0]] = pair[1]
                        }
                    }
                    reader.close()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                return map
            }

        fun getTextSizeToPixels(unit: Int, size: Float): Int {
            val metrics = Resources.getSystem().displayMetrics
            return TypedValue.applyDimension(unit, size, metrics).toInt()
        }

        fun createMentionTextView(context: Context?, text: String?): TextView {
            if (context == null || text == null) return TextView(context)
            //creating textview dynamically
            val tv = TextView(context)
            tv.text = text
            // QHD 단말에서 자꾸 뻗어서...
            val fontSize = 30 / convertDpToPixel(context, 1f)
            tv.textSize = getTextSizeToPixels(
                TypedValue.COMPLEX_UNIT_SP,
                fontSize
            ).toFloat()
            tv.setTextColor(ContextCompat.getColor(context, R.color.text_white_black))
            tv.setBackgroundResource(R.color.main_light)
            tv.setPadding(
                Math.round(convertDpToPixel(context, 4f)),
                Math.round(convertDpToPixel(context, 2f)),
                Math.round(convertDpToPixel(context, 4f)),
                Math.round(convertDpToPixel(context, 2f))
            )
            return tv
        }

        fun convertViewToDrawable(view: View): Any? {
            val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(spec, spec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val b = Bitmap.createBitmap(
                view.measuredWidth, view.measuredHeight,
                Bitmap.Config.RGB_565
            )
            val c = Canvas(b)
            c.translate(-view.scrollX.toFloat(), -view.scrollY.toFloat())
            view.draw(c)
            view.isDrawingCacheEnabled = true
            val cacheBmp = view.drawingCache

            // 비트맵이 너무 큰 경우가 있다 (4096x4096이 max). 대략 3000정도에 맞추자
            if (cacheBmp == null || cacheBmp.width > 3000) {
                view.destroyDrawingCache()
                return null
            }

            val viewBmp = cacheBmp.copy(Bitmap.Config.RGB_565, true)
            view.destroyDrawingCache()
            return BitmapDrawable(viewBmp)
        }

        fun mayShowLoginPopup(activity: Activity?): Boolean {
            if (getAccount(activity) != null) {
                return false
            }
            showDefaultIdolDialogWithBtn2(
                activity,
                null,
                activity?.getString(R.string.desc_login),
                R.string.yes,
                R.string.no,
                true, false,
                { v: View? ->
                    closeIdolDialog()
                    val intent = createIntent(activity)
                    activity?.startActivity(intent)
                },
                { v: View? -> closeIdolDialog() })
            return true
        }

        // 160404 gmail 가져오는 메소드로 따로 뺀다
        fun getGmail(context: Context): String {
            val uuid = getPreference(context, PROPERTY_AD_ID) // 모든 단말에서 고유 ID의 해시값을 가져가게 처리
            val hash = SHA1(if (uuid.isEmpty()) getDeviceUUID(context) else uuid)
            return hash ?: ""
        }

        fun SHA1(str: String): String? {
            var SHA: String? = ""
            try {
                val sh = MessageDigest.getInstance("SHA-1")
                sh.update(str.toByteArray())
                val byteData = sh.digest()
                val sb = StringBuffer()
                for (i in byteData.indices) {
                    sb.append(((byteData[i].toInt() and 0xff) + 0x100).toString(16).substring(1))
                }
                SHA = sb.toString()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                SHA = null
            }
            return SHA
        }

        // 160404 gmail을 못가져오는 경우 별도로 사용할 고유값
        fun getDeviceUUID(context: Context?): String {
            if (context == null) return ""
            val id = getPreference(context, PROPERTY_DEVICE_ID)

            var uuid: UUID? = null
            try {
                if (id != null && id.length > 0) {
                    uuid = UUID.fromString(id)
                } else {
                    val androidId = Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                    try {
                        if ("9774d56d682e549c" != androidId) {
                            uuid =
                                UUID.nameUUIDFromBytes(androidId.toByteArray(StandardCharsets.UTF_8))
                        } else {
                            var deviceId: String? = null
                            deviceId =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).imei
                                } else {
                                    (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).deviceId
                                }
                            val ad = getPreference(context, PROPERTY_AD_ID)
                            uuid = if (deviceId != null)
                                UUID.nameUUIDFromBytes(deviceId.toByteArray(StandardCharsets.UTF_8))
                            else
                                if (ad.isEmpty()) UUID.randomUUID() else UUID.nameUUIDFromBytes(
                                    ad.toByteArray(
                                        StandardCharsets.UTF_8
                                    )
                                )
                        }
                    } catch (e: SecurityException) {
                    }

                    setPreference(context, PROPERTY_DEVICE_ID, uuid.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 혹시모를 null 방지
            if (uuid == null) {
                uuid = UUID.randomUUID()
                setPreference(context, PROPERTY_DEVICE_ID, uuid.toString())
            }

            return uuid.toString()
        }

        fun checkUrls(text: String?): String {
            if (text == null || text.isEmpty()) {
                return ""
            }
            val urlRegex = "((https?):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)"

            val pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
            val urlMatcher = pattern.matcher(text)
            while (urlMatcher.find()) {
                val containedUrl = text.substring(urlMatcher.start(0), urlMatcher.end(0))
                log("checkUrls containedUrl:$containedUrl")
                if (Patterns.WEB_URL.matcher(containedUrl).matches()) {
                    return containedUrl
                }
            }
            return ""
        }

        fun getYoutubeVideoID(url: String): String? {
            val regExp =
                "(?:https?:\\/\\/)?(?:www\\.)?youtu(?:\\.be/|be\\.com/(?:watch\\?v=|v/|embed/|user/(?:[\\w#]+/)+))([^&#?\\n]+)"
            val pattern = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(url)

            if (matcher.find()) return matcher.group(1)
            return ""
        }

        // gaon
        fun showGaonIabRestrictedDialog(context: Context?, msg: String?) {
            if (context == null) return
            cleanIdolDialog()

            idolDialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER
            idolDialog!!.window!!.attributes = lpWindow
            idolDialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            idolDialog!!.setContentView(R.layout.dialog_general_failure)
            idolDialog!!.setCanceledOnTouchOutside(false)
            idolDialog!!.setCancelable(false)
            val dialog_btn_close = idolDialog!!.findViewById<Button>(R.id.btn_ok)
            dialog_btn_close.setOnClickListener { v: View? -> idolDialog!!.cancel() }
            idolDialog!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            val tvTitle = idolDialog!!.findViewById<TextView>(R.id.title)
            tvTitle.setText(R.string.iab_restricted_title)

            val tvMsg = idolDialog!!.findViewById<TextView>(R.id.message)
            tvMsg.text = msg

            try {
                idolDialog!!.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 기존 팝업이 닫혔는데 idolDialog가 null이 아닌 경우 닫아준다.
        fun cleanIdolDialog() {
            try {
                if (idolDialog != null) {
                    idolDialog!!.dismiss()
                    idolDialog = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 기부천사/기부요정 아이콘 설정
        fun setAngelFairyIcon(
            iconAngel: TextView,
            iconFairy: TextView,
            iconMiracle: TextView,
            idol: IdolModel
        ) {
            try {
                if (idol.angelCount > 0) {
                    iconAngel.text = idol.angelCount.toString() + ""
                    iconAngel.visibility = View.VISIBLE
                } else {
                    iconAngel.visibility = View.GONE
                }

                if (idol.fairyCount > 0) {
                    iconFairy.text = idol.fairyCount.toString() + ""
                    iconFairy.visibility = View.VISIBLE
                } else {
                    iconFairy.visibility = View.GONE
                }

                if (idol.miracleCount > 0) {
                    iconMiracle.text = idol.miracleCount.toString() + ""
                    iconMiracle.visibility = View.VISIBLE
                } else {
                    iconMiracle.visibility = View.INVISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 기부천사/기부요정 아이콘 설정 -> 아랍어,페르시아일 경우 숫자 바꾸기 위함
        fun setAngelFairyIcon2(
            iconAngel: TextView,
            iconFairy: TextView,
            iconMiracle: TextView,
            iconRookie: TextView,
            iconSuperRookie: TextView,
            idol: IdolModel
        ) {
            try {
                if (idol.angelCount > 0) {
                    val iconAngelCount = NumberFormat.getNumberInstance(Locale.getDefault())
                        .format(idol.angelCount.toLong())
                    iconAngel.text = iconAngelCount
                    iconAngel.visibility = View.VISIBLE
                } else {
                    iconAngel.visibility = View.GONE
                }

                if (idol.fairyCount > 0) {
                    val iconFairyCount = NumberFormat.getNumberInstance(Locale.getDefault())
                        .format(idol.fairyCount.toLong())
                    iconFairy.text = iconFairyCount
                    iconFairy.visibility = View.VISIBLE
                } else {
                    iconFairy.visibility = View.GONE
                }

                if (idol.miracleCount > 0) {
                    val iconMiracleCount = NumberFormat.getNumberInstance(Locale.getDefault())
                        .format(idol.miracleCount.toLong())
                    iconMiracle.text = iconMiracleCount
                    iconMiracle.visibility = View.VISIBLE
                } else {
                    iconMiracle.visibility = View.GONE
                }

                if (idol.rookieCount > 2) {
                    iconSuperRookie.visibility = View.VISIBLE
                    iconRookie.visibility = View.INVISIBLE
                } else if (idol.rookieCount > 0) {
                    val iconRookieCount = NumberFormat.getNumberInstance(Locale.getDefault())
                        .format(idol.rookieCount.toLong())
                    iconRookie.text = iconRookieCount
                    iconRookie.visibility = View.VISIBLE
                    iconSuperRookie.visibility = View.INVISIBLE
                } else {
                    iconRookie.visibility = View.INVISIBLE
                    iconSuperRookie.visibility = View.INVISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun loadGif(
            glideRequestManager: RequestManager?,
            url: String?,
            view: ImageView?
        ) {
            if (glideRequestManager == null || view == null) return
            var url = url
            val options = RequestOptions()
                .disallowHardwareConfig()
                .placeholder(ColorDrawable(-0x1))
                .error(ColorDrawable(-0x1))
                .fallback(ColorDrawable(-0x1))
                .dontAnimate()
                .dontTransform()

            val gifOptions = options
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .skipMemoryCache(false)

            glideRequestManager.clear(view)

            if (url == null) return

            if (url.endsWith(".gif")) {
                glideRequestManager
                    .asGif()
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .load(url)
                    .apply(gifOptions)
                    .into(view)
            } else {
                url = if (url.endsWith("mp4"))
                    url.replace("mp4", "webp")
                else
                    url

                glideRequestManager
                    .load(url)
                    .apply(options)
                    .into(view)
            }
        }

        //메시지 파싱
        fun messageParse(context: Context?, message: String?, type: String): Int {
            if (message == null || message == "") return 0
            var tmp = 0
            tmp = try {
                message.split(("$type:").toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1].split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].toInt()
            } catch (e: Exception) {
                0
            }
            return tmp
        }

        fun getLevel(heart: Long): Int {
            var level = 0
            for (i in 1..<Const.LEVEL_HEARTS.size) {
                if (heart < Const.LEVEL_HEARTS[i]) {
                    level = i - 1
                    break
                } else level = Const.MAX_LEVEL
            }
            return level
        }

        fun getScheduleIcon(str: String?): Int {
            return when (str) {
                "anniversary" -> R.drawable.schedule_category_01
                "albumday" -> R.drawable.schedule_category_02
                "movie" -> R.drawable.schedule_category_20
                "production" -> R.drawable.schedule_category_21
                "concert" -> R.drawable.schedule_category_03
                "event" -> R.drawable.schedule_category_04
                "sign" -> R.drawable.schedule_category_05
                "tv" -> R.drawable.schedule_category_06
                "radio" -> R.drawable.schedule_category_07
                "live" -> R.drawable.schedule_category_08
                "award" -> R.drawable.schedule_category_09
                "ticketing" -> R.drawable.schedule_category_11
                "preview" -> R.drawable.schedule_category_22
                else -> R.drawable.schedule_category_10
            }
        }

        fun getColorText(string: String, targetString: String, color: Int): SpannableString {
            val spannableString = SpannableString(string)
            val targetStartIndex = string.indexOf(targetString)
            val targetEndIndex = targetStartIndex + targetString.length

            if (targetStartIndex != -1) spannableString.setSpan(
                ForegroundColorSpan(color),
                targetStartIndex,
                targetEndIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            return spannableString
        }

        fun getColorText(
            spannableString: SpannableString?,
            targetString: String,
            color: Int
        ): SpannableString {
            val targetStartIndex = spannableString.toString().indexOf(targetString)
            val targetEndIndex = targetStartIndex + targetString.length

            if (targetStartIndex != -1) {
                spannableString?.setSpan(
                    ForegroundColorSpan(color),
                    targetStartIndex,
                    targetEndIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannableString ?: SpannableString("")
        }

        fun getColorTextLastIdx(
            spannableString: SpannableString,
            targetString: String,
            color: Int
        ): SpannableString {
            val targetStartIndex = spannableString.toString().lastIndexOf(targetString)
            val targetEndIndex = targetStartIndex + targetString.length

            if (targetStartIndex != -1) {
                spannableString.setSpan(
                    ForegroundColorSpan(color),
                    targetStartIndex,
                    targetEndIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannableString
        }

        fun getColorText(
            spannableString: SpannableString,
            targetString: String,
            targetString2: String,
            color: Int
        ): SpannableString {
            val targetStartIndex = spannableString.toString().indexOf(targetString)
            val targetEndIndex = targetStartIndex + targetString.length

            val targetStartIndex2 = spannableString.toString().indexOf(targetString2)
            val targetEndIndex2 = targetStartIndex2 + targetString2.length

            if (targetStartIndex != -1) spannableString.setSpan(
                ForegroundColorSpan(color),
                targetStartIndex,
                targetEndIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (targetStartIndex2 != -1) spannableString.setSpan(
                ForegroundColorSpan(color),
                targetStartIndex2,
                targetEndIndex2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return spannableString
        }

        //Spannable String 2개, 첫번 째, 두번째 넣은 targetString 컬러 선택 가능
        fun getColorText(
            string: String,
            targetString: String,
            targetString2: String,
            target1Color: Int,
            target2Color: Int
        ): SpannableString {
            val spannableString = SpannableString(string)
            val targetStartIndex = string.indexOf(targetString)
            val targetEndIndex = targetStartIndex + targetString.length

            val targetStartIndex2 = string.indexOf(targetString2)
            val targetEndIndex2 = targetStartIndex2 + targetString2.length

            if (targetStartIndex != -1) spannableString.setSpan(
                ForegroundColorSpan(target1Color),
                targetStartIndex,
                targetEndIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (targetStartIndex2 != -1) spannableString.setSpan(
                ForegroundColorSpan(target2Color),
                targetStartIndex2,
                targetEndIndex2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return spannableString
        }

        fun dateFromUTC(date: Date?): Date {
            if (date == null) return Date()
            return Date(date.time + Calendar.getInstance().timeZone.getOffset(date.time))
        }

        fun dateToUTC(date: Date?): Date {
            if (date == null) return Date()
            return Date(date.time - Calendar.getInstance().timeZone.getOffset(date.time))
        }

        fun dateToUTC(date: Date, timeZone: TimeZone): Date {
            val calendar = Calendar.getInstance()
            calendar.timeZone = timeZone
            return Date(date.time - calendar.timeZone.getOffset(date.time))
        }

        fun orderByString(str: String): String {
            val tmp = str.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            Arrays.sort(
                tmp,
                java.util.Comparator { obj: String, anotherString: String? ->
                    obj.compareTo(
                        anotherString!!
                    )
                })
            val targetString = StringBuilder()
            for (i in tmp.indices) {
                targetString.append(tmp[i])
                if (i != tmp.size - 1) targetString.append(", ")
            }
            return targetString.toString()
        }

        fun videoCacheHide(context: Context): Boolean {
            val appCacheDir = File(getCacheDirectory(context, true), "video-cache")
            var fileCreated = false
            log(appCacheDir.path)
            try {
                val output = File(appCacheDir, ".nomedia")
                fileCreated = if (output.exists()) false
                else output.createNewFile()
            } catch (e: IOException) {
                e.stackTrace
            }
            return fileCreated
        }

        private fun getCacheDirectory(context: Context?, preferExternal: Boolean): File? {
            if (context == null) return null

            var appCacheDir: File? = null
            var externalStorageState = try {
                Environment.getExternalStorageState()
            } catch (e: NullPointerException) { // (sh)it happens
                ""
            }
            if (preferExternal && Environment.MEDIA_MOUNTED == externalStorageState) {
                appCacheDir = getExternalCacheDir(context)
            }
            if (appCacheDir == null) {
                appCacheDir = context.cacheDir
            }
            if (appCacheDir == null) {
                val cacheDirPath = "/data/data/" + context.packageName + "/cache/"
                appCacheDir = File(cacheDirPath)
            }
            return appCacheDir
        }

        private fun getExternalCacheDir(context: Context): File? {
            val dataDir = File(File(Environment.getExternalStorageDirectory(), "Android"), "data")
            val appCacheDir = File(File(dataDir, context.packageName), "cache")
            if (!appCacheDir.exists()) {
                if (!appCacheDir.mkdirs()) {
                    log("Unable to create external cache directory")
                    return null
                }
            }
            return appCacheDir
        }

        fun hasBadWords(context: Context?, comment: String): Boolean {
            var target = comment

            if (IdolAccount.badWords == null) {
                val gson = getInstance(false)
                val listType = object : TypeToken<List<BadWordModel?>?>() {}.type
                IdolAccount.badWords =
                    gson.fromJson(getPreference(context, Const.BAD_WORDS), listType)
            }

            try {
                for ((exc, type, word1) in IdolAccount.badWords!!) {
                    for (str in exc) {
                        target = target.replace(str.toRegex(), "")
                    }
                    if (type.equals("C", ignoreCase = true)) {
                        if (target.contains(word1)) {
                            return true
                        }
                    } else if (type.equals("R", ignoreCase = true)) {
                        val p = Pattern.compile(word1)
                        val m = p.matcher(target)

                        if (m.find()) {
                            return true
                        }
                    }
                }
            } catch (e: NullPointerException) {
                return false
            }
            return false
        }

        fun BadWordsFilterToHeart(context: Context?, comment: String): String {
            val mAccount = getAccount(context)
            var origin = StringBuilder(comment)

            if (IdolAccount.badWords == null) {
                val gson = getInstance(false)
                val listType = object : TypeToken<List<BadWordModel?>?>() {
                }.type
                val badWords = gson.fromJson<ArrayList<BadWordModel>>(
                    getPreference(context, Const.BAD_WORDS),
                    listType
                )
                IdolAccount.badWords = badWords
            }

            //원인불명;... -> startup activity에서 latch처리완료 했으나 혹시몰라...
            if (IdolAccount.badWords == null) return comment

            for ((exc, type, word1) in IdolAccount.badWords!!) {
                var target = comment
                var heart = ""
                for (str in exc) {
                    var tmp = ""
                    for (i in 0..<str.length) tmp += "0"
                    target = target.replace(str.toRegex(), tmp)
                }
                if (type.equals("C", ignoreCase = true)) {
                    for (i in 0..<word1.length) heart += "♥"
                    while (target.contains(word1)) {
                        origin = origin.replace(
                            target.indexOf(word1),
                            target.indexOf(word1) + word1.length,
                            heart
                        )
                        target = target.replaceFirst(word1.toRegex(), heart)
                    }
                } else if (type.equals("R", ignoreCase = true)) {
                    val p = Pattern.compile(word1)
                    val m = p.matcher(target)
                    while (m.find()) {
                        for (i in 0..<m.end() - m.start()) heart += "♥"
                        origin = origin.replace(m.start(), m.end(), heart)
                        heart = ""
                    }
                }
            }
            return origin.toString()
        }

        fun convertTimeAsTimezone(time: String): String {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = Date()

            date.hours =
                time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                    .toInt()
            date.minutes =
                time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                    .toInt()

            return formatter.format(
                dateFromUTC(
                    dateToUTC(
                        date,
                        TimeZone.getTimeZone("Asia/Seoul")
                    )
                )
            )
        }

        /**
         * Activity에서 status bar를 숨겨주는 메서드
         *
         * @author Daeho Kim
         * @since 6.6.6
         * @param context - activity
         */
        fun hideStatusBarOnActivity(context: Context) {
            val decorView = (context as Activity).window.decorView
            val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
            decorView.systemUiVisibility = uiOptions
        }

        /**
         * 외부 path의 이미지 확장자를 뽑아낸다.
         *
         * @author Daeho Kim
         * @since 6.7.3
         * @param url - String
         */
        fun getExtensionFromImagePath(url: String): String {
            var extension = ""
            val i = url.lastIndexOf('.')
            if (i > 0) {
                extension = url.substring(i + 1)
            }

            return extension
        }

        /**
         * uri로부터 file path를 가져온다.
         *
         * @author Daeho Kim
         * @since 6.7.3
         * @param context - String
         * @param uri - Uri
         */
        fun uriToFilePath(context: Context?, uri: Uri?): String? {
            if (context == null || uri == null) return null
            var filePath: String? = null

            if ("content" == uri.scheme) {
                val cursor = context.contentResolver
                    .query(
                        uri,
                        arrayOf(MediaStore.Images.ImageColumns.DATA),
                        null, null, null
                    )

                if (cursor != null) {
                    cursor.moveToFirst()
                    // cursor value check for preventing IllegalStateException
                    if (cursor.count != 0 && cursor.getType(0) == Cursor.FIELD_TYPE_STRING) filePath =
                        cursor.getString(0)
                    cursor.close()
                }
            } else {
                if (uri != null) filePath = uri.path
            }

            return filePath
        }

        //redirection한 url이 썸네일이 보여지는지 하는지 체크 해주는  기능이다.
        //redirection한
        private fun isRedirectionNeededUrl(url: URL): URL? {
            if (url.toString().contains("music.youtube.com")) {
                try {
                    return URL(url.toString().replace("music.".toRegex(), ""))
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                    return getFinalURL(url)
                }
            } else {
                return getFinalURL(url)
            }
        }

        fun getURLtoText(context: Context?, strURL: String?): LinkDataModel? {
            try {
                var url = URL(strURL)
                //            String userInfo = System.getProperty("http.agent") + " (" + context.getApplicationInfo().packageName + "/" + context.getString(R.string.app_version)+")";
//
//            String pattern = "Android\\s+[0-9.]+;?\\s*";
//            Pattern regex = Pattern.compile(pattern);
//            Matcher matcher = regex.matcher(userInfo);
//
//            if(matcher.find()){
//                userInfo = userInfo.replace(userInfo.substring(matcher.start(), matcher.end()), "");
//            }
//
//           String userAgent = "Mozilla/5.0 ".concat(userInfo);
                val linkData = LinkDataModel()
                var conn = isRedirectionNeededUrl(
                    url
                )!!
                    .openConnection()
                conn.connectTimeout = 3000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", Const.META_OG_USER_AGENT)
                var source = Source(conn)

                // Location header가 없을 때까지 redirection 시도. 최대 10회
                for (i in 0..9) {
                    val location = conn.getHeaderField("Location")
                    if (location != null) {
                        url = URL(location)
                        conn = isRedirectionNeededUrl(url)!!.openConnection()
                        conn.connectTimeout = 3000
                        conn.readTimeout = 10000
                        conn.setRequestProperty("User-Agent", Const.META_OG_USER_AGENT)
                        source = Source(conn)
                    } else {
                        break
                    }
                }

                val elements = source.getAllElements("meta")

                for (element in elements) {
                    val id = element.getAttributeValue("property")

                    if (id != null && id == "og:title") {
                        linkData.title = element.getAttributeValue("content")
                        log(
                            "getURLtoText title id" + id + "    content:" + element.getAttributeValue(
                                "content"
                            )
                        )
                    } else if (id != null && id == "og:image") {
                        linkData.imageUrl = element.getAttributeValue("content")
                        log(
                            "getURLtoText img id" + id + "    content:" + element.getAttributeValue(
                                "content"
                            )
                        )
                    } else if (id != null && id == "og:description") {
                        linkData.description = element.getAttributeValue("content")
                        log(
                            "getURLtoText description id" + id + "    content:" + element.getAttributeValue(
                                "content"
                            )
                        )
                    } else if (id != null && id == "og:url") {
                        val ogUrlString = element.getAttributeValue("content")
                        linkData.url = strURL //seturl 은  og:url이 아니라 일반 url 이 가지게  수정
                        val ogUrl = URL(ogUrlString)
                        val baseUrl = ogUrl.protocol + "://" + ogUrl.host
                        linkData.host = baseUrl
                    }
                }
                return linkData
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return null
        }

        //기장 마지막에  redirection이 되는 url 을 체크 해서 return 해준다.
        fun getFinalURL(url: URL): URL? {
            try {
                val con = url.openConnection() as HttpURLConnection
                con.instanceFollowRedirects = false
                con.setRequestProperty("User-Agent", Const.META_OG_USER_AGENT)
                con.addRequestProperty("Accept-Language", "en-US,en;q=0.8")
                con.addRequestProperty("Referer", "https://www.google.com/")
                con.connect()
                //con.getInputStream();
                val resCode = con.responseCode
                if (resCode == HttpURLConnection.HTTP_SEE_OTHER || resCode == HttpURLConnection.HTTP_MOVED_PERM || resCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    var Location = con.getHeaderField("Location")
                    if (Location.startsWith("/")) {
                        Location = url.protocol + "://" + url.host + Location
                    }
                    return getFinalURL(URL(Location))
                }
            } catch (e: Exception) {
                println(e.message)
            }
            return url
        }

        fun showSoftKeyboard(context: Context?, view: View) {
            if (context == null) return

            val imm =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (imm != null && view.requestFocus()) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        /**
         * String에서 hashTag의 형태를 바꿔준다
         *
         * @author Daeho Kim
         * @since 6.8.0
         * @param content - String
         */
        fun convertHashTags(context: Context?, content: String?): Editable {
            //https://www.regextester.com/106421 -> 모든 이모지 매칭 regex적용
            //https://ko.wikipedia.org/wiki/%EC%9C%A0%EB%8B%88%EC%BD%94%EB%93%9C_D000~DFFF ->D000~DFFF 중 D000~D7A0사이 문자들은  ㅋ,ㅌ,ㅍ,ㅎ 문자들이 포함되므로, 제외한다.
            //http://titus.uni-frankfurt.de/unicode/unitestx.htm#U2800   u2000-u2E7F| u2FF0-u303F | u3200-u3300 해당 범위들 이모지로 제외
            if (content.isNullOrEmpty()) return Editable.Factory.getInstance().newEditable("")

            val hashTagPattern =
                "#([^\\s#(\\u00a9|\\u00ae|[\\u2000-\\u2E7F]|[\\u2FF0-\\u303F]|[\\u3200-\\u3300]|[\\ud83c\\ud7b0-\\ud83c\\udfff]|[\\ud83d\\ud7b0-\\ud83d\\udfff]|[\\ud83e\\ud7b0-\\ud83e\\udfff])+]*)"
            val pattern = Pattern.compile(hashTagPattern)
            val matcher = pattern.matcher(content)

            val e = Editable.Factory.getInstance().newEditable("")
            var lastHashIdx = 0

            while (matcher.find()) {
                val startIdx = matcher.start()
                val endIdx = matcher.end()

                val prefix = content.substring(lastHashIdx, startIdx)
                e.append(prefix)
                lastHashIdx = endIdx

                val hashTag = matcher.group(0)

                val sb = SpannableStringBuilder()

                //리스트 아이템 속  해쉬태그를 누를때,  clickspan이  두번씩  동작하므로,
                //아래 값으로  값을  체크하여, 한번만 onclick 동작을 진행한다.
                val clickSpanWorkCount = intArrayOf(0)

                val clickSpan: ClickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        ++clickSpanWorkCount[0]
                        if (clickSpanWorkCount[0] == 1) { //값 1일때만  실행

                            //해시태그 클릭시 최근검색어에 추가

                            val gson = instance
                            val listType = object : TypeToken<List<SearchHistoryModel?>?>() {}.type
                            var searchList = gson.fromJson<ArrayList<SearchHistoryModel?>>(
                                getPreference(context, Const.SEARCH_HISTORY).toString(), listType
                            )

                            //혹시나  preference값 없어서 null 인 경우는 빈어레이를 추가해준다.
                            if (searchList == null) {
                                searchList = ArrayList()
                            }

                            searchList.remove(SearchHistoryModel(hashTag))
                            searchList.add(SearchHistoryModel(hashTag))
                            if (searchList.size > 10) {
                                searchList.removeAt(0)
                            }
                            setPreferenceArray(context, Const.SEARCH_HISTORY, searchList)
                            context?.startActivity(
                                createIntent(
                                    context,
                                    hashTag!!
                                )
                            )
                        }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        clickSpanWorkCount[0] = 0 //clickspan onclick 눌리고 한번불리므로,  여기서  reset
                    }
                }

                sb.append(hashTag)
                if(context != null) {
                    sb.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_light_blue)),
                        0,
                        sb.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    sb.setSpan(
                        clickSpan,
                        0,
                        sb.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    e.append(sb)
                }
            }
            e.append(content.substring(lastHashIdx))

            if (e.toString().isEmpty()) return e.append(content)

            return e
        }

        fun hideSoftKeyboard(context: Context?, view: View?) {
            val imm =
                context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view?.windowToken, 0)
        }

        /**
         * TextView의 url link들의 underline을 지운다.
         *
         * @author Daeho Kim
         * @since 6.8.0
         * @param content - Editable
         */
        fun stripUrlUnderlines(content: Spannable): Spannable {
            val spans = content.getSpans(0, content.length, URLSpan::class.java)
            if (spans.size > 0) {
                for (span in spans) {
                    var span = span
                    val start = content.getSpanStart(span)
                    val end = content.getSpanEnd(span)
                    content.removeSpan(span)
                    span = URLSpanNoUnderline(span.url)
                    content.setSpan(span, start, end, 0)
                }
            }
            return content
        }

        fun bytesToHex(hash: ByteArray?): String {
            if (hash == null) return ""

            val hexString = StringBuffer()
            for (i in hash.indices) {
                val hex = Integer.toHexString(0xff and hash[i].toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            return hexString.toString()
        }

        fun sha256(s: String): String {
            try {
                // Create SHA256 Hash
                val digest = MessageDigest.getInstance("SHA-256")
                digest.update(s.toByteArray())
                val messageDigest = digest.digest()

                // Create Hex String
                val hexString = StringBuffer()
                for (i in messageDigest.indices) hexString.append(Integer.toHexString(0xFF and messageDigest[i].toInt()))
                return hexString.toString()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
            return s
        }

        fun sha1(s: String): String {
            try {
                // Create SHA256 Hash
                val digest = MessageDigest.getInstance("SHA-1")
                digest.update(s.toByteArray())
                val messageDigest = digest.digest()

                // Create Hex String
                val hexString = StringBuffer()
                for (i in messageDigest.indices) hexString.append(
                    String.format(
                        "%02x",
                        0xFF and messageDigest[i].toInt()
                    )
                )
                return hexString.toString()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
            return s
        }

        fun sha256modi(str: String): String? {
            var SHA: String? = ""
            try {
                val sh = MessageDigest.getInstance("SHA-256")
                sh.update(str.toByteArray())
                val byteData = sh.digest()
                val sb = StringBuffer()
                for (i in byteData.indices) {
                    sb.append(((byteData[i].toInt() and 0xff) + 0x100).toString(16).substring(1))
                }
                SHA = sb.toString()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                SHA = null
            }
            return SHA
        }

        fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
            return sqrt((x2 - x1).toDouble().pow(2.0) + (y2 - y1).toDouble().pow(2.0))
        }

        val isChinaBrand: Boolean
            get() {
                return if (Build.BRAND == null) {
                    true
                } else {
                    (Build.BRAND.equals("Huawei", ignoreCase = true)
                            || Build.BRAND.equals("XiaoMi", ignoreCase = true)
                            || Build.BRAND.equals("OPPO", ignoreCase = true)
                            || Build.BRAND.equals("Vivo", ignoreCase = true)
                            || Build.BRAND.equals("Honor", ignoreCase = true)
                            || Build.BRAND.equals("Meizu", ignoreCase = true)
                            || Build.BRAND.equals("LeNovo", ignoreCase = true)
                            || Build.BRAND.equals("Qiku", ignoreCase = true)
                            || Build.BRAND.equals("Smartisan", ignoreCase = true))
                }
            }

        fun isOSNougat(): Boolean {
            return (Build.VERSION.SDK_INT == Build.VERSION_CODES.N
                    || Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1)
        }

        fun isRTL(context: Context?): Boolean {
            if (context == null) return false

            return (context.resources.configuration.layoutDirection
                    == ViewCompat.LAYOUT_DIRECTION_RTL)
        }

        fun isRTL(locale: Locale): Boolean {
            val directionality = Character.getDirectionality(locale.displayName[0]).toInt()
            return (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT.toInt()
                    || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC.toInt())
        }

        fun takeScreenShot(activity: Activity, view: View) {
            //현재 application info를 가지고와서 현재  타겟 sdk  값을  알아낸다.


            var packageInfo: PackageInfo? = null
            try {
                packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            checkNotNull(packageInfo) { "warning : packageInfo is null" }
            val applicationInfo = packageInfo.applicationInfo
            val targetSdkVersion = applicationInfo?.targetSdkVersion ?: 35
            // android 6.0 처리
            // 방통위 규제사항 처리
            // TODO: 2021/05/05 targetsdk 30부터  새로 바뀐 scoped storage 정책을  추가해준다.
            val permissions = getStoragePermissions(activity)
            val msgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    activity.getString(R.string.permission_storage),
                    ""
                )
            } else {
                arrayOf(activity.getString(R.string.permission_storage))
            }

            requestPermissionIfNeeded(
                activity,
                null,
                permissions,
                msgs,
                BaseActivity.REQUEST_WRITE_EXTERNAL_STORAGE,
                object : PermissionListener {
                    override fun requestPermission(permissions: Array<String>) {
                    }

                    override fun onPermissionDenied() {
                    }

                    override fun onPermissionAllowed() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= 30) { //버전 29이상일때 && target sdk 30이상일떄
                            val contentValues = ContentValues()

                            contentValues.put(
                                MediaStore.Images.Media.IS_PENDING,
                                1
                            ) // I/O는 오래 걸리니... 일단 PENDING 시켜놓고
                            contentValues.put(
                                MediaStore.Images.Media.RELATIVE_PATH,
                                Environment.DIRECTORY_DOWNLOADS
                            ) // 경로를 결정합니다.
                            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            contentValues.put(
                                MediaStore.Images.Media.DISPLAY_NAME,
                                Const.DOWNLOAD_FILE_PATH_PREFIX + getFileDate("yyyy-MM-dd-HH-mm-ss")
                            ) // 경로를 결정합니다.

                            try {
                                val uri = activity.contentResolver.insert(
                                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                    contentValues
                                )

                                if (uri != null) {
                                    val pdf = activity.contentResolver.openFileDescriptor(
                                        uri,
                                        "w"
                                    ) // 앞서 얻은 uri와 ContentResolver를 이용해서 접근하도록 합시다.
                                    val bytes = ByteArrayOutputStream()
                                    loadBitmapFromView(
                                        activity,
                                        view
                                    )?.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
                                    val bitmapData = bytes.toByteArray()
                                    val strToByte = getBytes(ByteArrayInputStream(bitmapData))
                                    checkNotNull(pdf) { "Descriptor is null" }
                                    val outputStream = FileOutputStream(pdf.fileDescriptor)
                                    outputStream.write(strToByte)
                                    outputStream.close()
                                }

                                contentValues.clear()
                                contentValues.put(
                                    MediaStore.Images.Media.IS_PENDING,
                                    0
                                ) // 작업이 끝나면 PENDING을 해제합니다.

                                if (uri != null) {
                                    activity.contentResolver.update(uri, contentValues, null, null)
                                }
                                //
                                closeProgress()
                                makeText(
                                    activity,
                                    activity.getString(R.string.msg_save_ok),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: NullPointerException) {
                                e.printStackTrace()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        } else { //29  미만일때는
                            var fos: FileOutputStream? = null

                            val filename =
                                Const.DOWNLOAD_FILE_PATH_PREFIX + getFileDate("yyyy-MM-dd-HH-mm-ss") + ".jpg"
                            val captureFile = File(Const.FILE_DIR, filename)

                            try {
                                fos = FileOutputStream(captureFile, false)
                                loadBitmapFromView(activity, view)
                                    ?.compress(Bitmap.CompressFormat.JPEG, 100, fos)

                                if (captureFile.exists()) {
                                    if (captureFile.length() != 0L) {
                                        activity.sendBroadcast(
                                            Intent(
                                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                Uri.fromFile(captureFile)
                                            )
                                        )
                                        makeText(
                                            activity,
                                            activity.getString(R.string.msg_save_ok),
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        notifyMediaStoreScanner(activity, captureFile)
                                    } else {
                                        makeText(
                                            activity,
                                            activity.getString(R.string.msg_unable_use_download_2),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    makeText(
                                        activity,
                                        activity.getString(R.string.msg_unable_use_download_2),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: NullPointerException) {
                                e.printStackTrace()
                            } catch (e: FileNotFoundException) {
                                e.printStackTrace()
                                makeText(
                                    activity,
                                    activity.getString(R.string.msg_unable_use_download_2),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                if (fos != null) {
                                    try {
                                        fos.close()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                }, false
            )
        }

        //image input stream 을  byte로 변환해서 내보낸다. ->  mediastore wirte 용으로.
        private fun getBytes(inputStream: InputStream): ByteArray {
            val buffer = ByteArrayOutputStream()
            var nRead = 0
            val data = ByteArray(1024)
            while (true) {
                try {
                    if ((inputStream.read(data, 0, data.size).also { nRead = it }) == -1) break
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                buffer.write(data, 0, nRead)
            }
            return buffer.toByteArray()
        }

        fun loadBitmapFromView(context: Context?, v: View): Bitmap? {
            var width = v.width
            var height = v.height

            if (width == 0 || height == 0) {
                width = v.measuredWidth
                height = v.measuredHeight

                val dm = context?.resources?.displayMetrics ?: return null
                v.measure(
                    View.MeasureSpec.makeMeasureSpec(dm.widthPixels, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dm.heightPixels, View.MeasureSpec.EXACTLY)
                )
            }
            val returnedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(returnedBitmap)
            v.layout(0, 0, width, height)
            v.draw(c)

            return returnedBitmap
        }

        fun notifyMediaStoreScanner(context: Context?, file: File?) {
            // 이미지 저장 후 갤러리에 안나오는 문제 대응
            try {
                //MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(), file.getName(), null);

                context?.sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // LG Code에 포함되는 아이돌인지 확인
        fun isInLGCode(context: Context?, idolId: Int): Boolean {
            if (getPreference(context, Const.LG_CODE).isEmpty()) {
                return false
            }

            val gson = getInstance(false)
            val listType = object : TypeToken<List<LGCodeModel?>?>() {}.type

            val lgCodes = gson.fromJson<ArrayList<LGCodeModel>>(
                getPreference(context, Const.LG_CODE),
                listType
            )
            for ((idolId1) in lgCodes) {
                if (idolId1 == idolId) return true
            }

            return false
        }

        fun getLGCode(context: Context?, idolId: Int): String {
            if (getPreference(context, Const.LG_CODE).isEmpty()) {
                return ""
            }

            val gson = getInstance(false)
            val listType = object : TypeToken<List<LGCodeModel?>?>() {}.type

            val lgCodes = gson.fromJson<ArrayList<LGCodeModel>>(
                getPreference(context, Const.LG_CODE),
                listType
            )
            for ((idolId1, lgCode) in lgCodes) {
                if (idolId1 == idolId) {
                    return lgCode
                }
            }

            return ""
        }

        fun getToolbarHeight(context: Context?): Int {
            if (context == null) return 0
            var result = 0
            val tv = TypedValue()
            if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                result = TypedValue.complexToDimensionPixelSize(
                    tv.data,
                    context.resources.displayMetrics
                )
            }

            return result
        }

        fun listToCommaSeparatedString(list: List<*>): String {
            val builder = StringBuilder()
            val comma = ","

            if (list.size > 0) {
                for (item in list) {
                    builder.append(item)
                    builder.append(comma)
                }
                var commaSeparatedString = builder.toString()
                commaSeparatedString =
                    commaSeparatedString.substring(
                        0,
                        commaSeparatedString.length - comma.length
                    )

                return commaSeparatedString
            } else {
                return ""
            }
        }

        fun idolListToJSONArray(list: List<IdolModel?>): JSONArray {
            val gson = instance
            val array = JSONArray()
            try {
                for (i in list.indices) {
                    val item = list[i]

                    if (item != null) {
                        val json = JSONObject(gson.toJson(item))
                        array.put(json)
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return array
        }

        fun isUsingNightModeResources(context: Context?): Boolean {
            //혹시 몰라서 방어코드
            if (context == null) return false
            val nightModeFlags =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        }

        fun getTypeListTypeArray(context: Context): Array<String?> {
            val tmp = getPreference(context, Const.PREF_TYPE_LIST)
            val gson = instance
            val listType = object : TypeToken<List<TypeListModel?>?>() {}.type
            val typeList = gson.fromJson<List<TypeListModel>>(tmp, listType)
            val res = arrayOfNulls<String>(typeList.size)

            for (i in res.indices) {
                res[i] = typeList[i].type
            }

            return res
        }

        fun getTypeListTypeNameArray(context: Context?): Array<String> {
            val tmp = getPreference(context, Const.PREF_TYPE_LIST)
            val gson = instance
            val listType = object : TypeToken<List<TypeListModel?>?>() {}.type
            val typeList = gson.fromJson<List<TypeListModel>>(tmp, listType)
            val res = Array(typeList.size) { "" }

            for (i in res.indices) {
                res[i] = typeList[i].typeName
            }

            return res
        }

        // end CELEB
        fun getAgreementLanguage(context: Context?): String {
            if (context == null) return "_en"
            val lang = getSystemLanguage(context)
            return if (lang.startsWith("ko")) ""
            else if (lang.startsWith("zh_CN")) "_zhcn"
            else if (lang.startsWith("zh_TW")) "_zhtw"
            else if (lang.startsWith("ja")) "_ja"
            else "_en"
        }

        fun showChargeHeartDialog(context: Context?) {
            val context = context ?: return
            val dialog = Dialog(
                context,
                android.R.style.Theme_Translucent_NoTitleBar
            )

            val lpWindow = WindowManager.LayoutParams()
            lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lpWindow.dimAmount = 0.7f
            lpWindow.gravity = Gravity.CENTER

            if (dialog.window == null) return

            dialog.window!!.attributes = lpWindow
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            dialog.setContentView(R.layout.dialog_charge_heart)
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(true)

            val btnClose = dialog.findViewById<AppCompatButton>(R.id.btn_ok)
            btnClose.setOnClickListener { v: View? -> dialog.cancel() }

            //
            val activity = getActivityFromContext(context) as BaseActivity?
            val heartShop = dialog.findViewById<View>(R.id.cl_heart_shop)
            val heartBox = dialog.findViewById<View>(R.id.cl_heartbox)
            val freeHeart = dialog.findViewById<View>(R.id.cl_free_heart)
            val videoAd = dialog.findViewById<View>(R.id.cl_video_ad)
            heartShop.setOnClickListener { v: View? ->
                activity!!.setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "vote_heart_shop"
                )
                activity.startActivity(NewHeartPlusActivity.createIntent(activity))
                dialog.cancel()
            }
            heartBox.setOnClickListener { v: View? ->
                dialog.cancel()
            }
            freeHeart.setOnClickListener { v: View? ->
                activity!!.setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "vote_free_heart_charge"
                )
                activity.startActivity(HeartPlusFreeActivity.createIntent(activity))
                dialog.cancel()
            }


            //video disable timer 적용중이면,  비광 보러가기는  gone처리  한다.
            if ((getPreferenceLong(
                    context,
                    Const.VIDEO_TIMER_END_TIME_PREFERENCE_KEY,
                    Const.DEFAULT_VIDEO_DISABLE_TIME
                ) != Const.DEFAULT_VIDEO_DISABLE_TIME)
            ) {
                videoAd.visibility = View.GONE
            }
            videoAd.setOnClickListener { v: View? ->
                activity!!.setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "vote_video_ad"
                )
                dialog.cancel()
                activity.startActivityForResult(
                    MezzoPlayerActivity.createIntent(
                        context,
                        Const.ADMOB_REWARDED_VIDEO_ONEPICK_UNIT_ID
                    ),
                    BaseActivity.MEZZO_PLAYER_REQ_CODE
                )
            }

            dialog.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            try {
                dialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 비광 처리 통합
        fun handleVideoAdResult(
            activity: BaseActivity?,
            isForQuiz: Boolean,
            fragmentAdded: Boolean,
            requestCode: Int,
            resultCode: Int,
            data: Intent?,
            analyticsAction: String,
            anchorView: View? = null,
            listener: IVideoAdListener?
        ) {
            val MEZZO_PLAYER_REQ_CODE = 900

            if (requestCode == MEZZO_PLAYER_REQ_CODE || requestCode == -1) {    //HeartPlusFragment1에서 광고를 볼 경우, onActivityResult를 이제 제거하고 ActivityResultLauncher 사용하기 때문에 request코드가 없어서 -1로 보냈음.  향후 광고 볼 수 있는 화면 전부 onActivityResult를 사용하도록 바꾸면 지울 것.
                if (resultCode == Activity.RESULT_CANCELED) {
                    BaseActivity.FLAG_CLOSE_DIALOG = false // 하트적립/미적립 팝업 자동으로 닫힘 방지

                    if (activity != null && fragmentAdded
                        && data != null
                    ) {
                        activity.setUiActionFirebaseGoogleAnalyticsActivity(
                            Const.ANALYTICS_BUTTON_PRESS_ACTION,
                            analyticsAction
                        )

                        // 비디오광고 화면에서 복귀시 로케일이 시스템 로케일로 바뀌는 경우가 있어서
                        setLocale(activity)
                        val result = data.getIntExtra(MezzoPlayerActivity.RESULT_CODE, -1)
                        val adType = data.getIntExtra(MezzoPlayerActivity.AD_TYPE, -1)
                        val adErrorType = data.getIntExtra(MezzoPlayerActivity.AD_ERROR_TYPE, -10)
                        when (result) {
                            MezzoPlayerActivity.RESULT_CODE_OTHER_ERROR -> Handler().postDelayed(
                                {
                                    showDefaultIdolDialogWithBtn1(
                                        activity,
                                        null,
                                        activity.getString(R.string.video_ad_unable),
                                        { v: View? -> closeIdolDialog() },
                                        true
                                    )
                                }, 300
                            )

                            MezzoPlayerActivity.RESULT_CODE_NETWORK_ERROR -> Handler().postDelayed(
                                {
                                    showDefaultIdolDialogWithBtn1(
                                        activity,
                                        null,
                                        activity.getString(
                                            R.string.video_ad_failed
                                        ),
                                        { v: View? -> closeIdolDialog() },
                                        true
                                    )
                                }, 300
                            )

                            MezzoPlayerActivity.RESULT_CODE_SHOW_FAIL -> {
                                val type = ("ErrorCode="
                                        + (MezzoPlayerActivity.RESULT_CODE_SHOW_FAIL + adType + adErrorType)
                                        + "\n")
                                Handler().postDelayed({
                                    showDefaultIdolDialogWithBtn1(
                                        activity,
                                        null,
                                        type + activity.getString(
                                            R.string.ad_show_fail
                                        ),
                                        { v: View? -> closeIdolDialog() },
                                        true
                                    )
                                }, 300)
                            }

                            MezzoPlayerActivity.RESULT_CODE_CANCELLED -> Handler().postDelayed(
                                {
                                    if (!isForQuiz) {
                                        showDefaultIdolDialogWithBtn1(
                                            activity,
                                            null,
                                            activity.getString(R.string.video_ad_cancelled),
                                            { v: View? -> closeIdolDialog() },
                                            true
                                        )
                                    } else {
                                        make(
                                            activity.findViewById(android.R.id.content),
                                            activity.getString(
                                                R.string.desc_skip_ad_to_retry_quiz
                                            )
                                        ).also { snack ->
                                            if (anchorView != null) {
                                                val v = anchorView.findViewById<View>(R.id.snackbar_anchor)
                                                snack.setAnchorView(v)
                                            }
                                        }.show()
                                    }
                                }, 300
                            )

                            else -> Handler().postDelayed({
                                if (!isForQuiz) {
                                    showDefaultIdolDialogWithBtn1(
                                        activity,
                                        null,
                                        activity.getString(R.string.video_ad_cancelled),
                                        { v: View? -> closeIdolDialog() },
                                        true
                                    )
                                } else {
                                    make(
                                        activity.findViewById(android.R.id.content),
                                        activity.getString(
                                            R.string.desc_skip_ad_to_retry_quiz
                                        )
                                    ).also { snack ->
                                        if (anchorView != null) {
                                            val v = anchorView.findViewById<View>(R.id.snackbar_anchor)
                                            snack.setAnchorView(v)
                                        }
                                    } .show()
                                }
                            }, 300)
                        }
                    } else {
                        try {
                            Handler().postDelayed(
                                {
                                    if (!isForQuiz) {
                                        showDefaultIdolDialogWithBtn1(
                                            activity,
                                            null,
                                            activity!!.getString(R.string.video_ad_cancelled),
                                            { v: View? -> closeIdolDialog() },
                                            true
                                        )
                                    } else {
                                        make(
                                            activity!!.findViewById(android.R.id.content),
                                            activity.getString(
                                                R.string.desc_skip_ad_to_retry_quiz
                                            )
                                        ).also { snack ->
                                            if (anchorView != null) {
                                                val v = anchorView.findViewById<View>(R.id.snackbar_anchor)
                                                snack.setAnchorView(v)
                                            }
                                        }.show()
                                    }
                                },
                                300
                            )
                        } catch (e: Exception) {
                            e.stackTrace
                        }
                    }
                } else if (resultCode == MezzoPlayerActivity.RESULT_CODE_MEZZO && listener != null) {
                    v("resultcode 메조")
                    listener.onVideoSaw("mezzo")
                } else if (resultCode == MezzoPlayerActivity.RESULT_CODE_ADMOB && listener != null) {
                    v("resultcode 애드몹")
                    listener.onVideoSaw("admob")
                } else if (resultCode == MezzoPlayerActivity.RESULT_CODE_MAIO && listener != null) {
                    v("resultcode 마이오")
                    listener.onVideoSaw("maio")
                } else if (resultCode == MezzoPlayerActivity.RESULT_CODE_APPLOVIN_MAX && listener != null) {
                    v("resultcode 앱 러빈")
                    listener.onVideoSaw("applovin")
                } else if (resultCode == MezzoPlayerActivity.RESULT_CODE_IRONSOURCE && listener != null) {
                    v("resultcode 아이언")
                    listener.onVideoSaw("ironsource")
                } else if (resultCode == MezzoPlayerActivity.RESULT_CODE_PANGLE && listener != null) {
                    listener.onVideoSaw("pangle")
                    v("resultcode pangle")
                } else if (resultCode == MezzoPlayerActivity.RESULT_CODE_MOBVISTA && listener != null) {
                    listener.onVideoSaw("mint")
                    v("resultcode mint")
                } else if (resultCode == MezzoPlayerActivity.RESULT_CODE_TAPJOY && listener != null) {
                    listener.onVideoSaw("tapjoy")
                    v("resultcode tapjoy")
                }
            }
        }

        //로컬  videi timer의  end time을  set 해준다.
        //is_Last가  false 이면 -1L값으로  video_disable_end_time을 reset 해준다.
        fun setLocalVideoTimer(response: JSONObject, context: Context) {
            if (response.optBoolean("is_last")) {
                val remain_seconds = response.optInt("remain_seconds")
                setPreference(
                    context,
                    Const.VIDEO_TIMER_END_TIME_PREFERENCE_KEY,
                    System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(remain_seconds.toLong())
                )
            } else {
                setPreference(
                    context,
                    Const.VIDEO_TIMER_END_TIME_PREFERENCE_KEY,
                    Const.DEFAULT_VIDEO_DISABLE_TIME
                )
            }
        }

        fun encodingSpecialChar(str: String): String {
            return str.replace("%".toRegex(), "%25").replace("&".toRegex(), "%26")
                .replace("\\?".toRegex(), "%3F").replace("#".toRegex(), "%23")
                .replace("/".toRegex(), "%2F").replace("=".toRegex(), "%3D")
        }

        // ... x시간 전 처리해주는 함수. 줄넘김이 있는 경우 잘 안됨.
        fun makeTextViewResizable(
            context: Context?,
            tv: TextView,
            maxLine: Int,
            expandText: String
        ) {
            if (context == null) return
            if (tv.tag == null) {
                tv.tag = tv.text
            }
            val vto = tv.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                @Suppress("deprecation")
                override fun onGlobalLayout() {
                    val builder = SpannableStringBuilder()
                    val obs = tv.viewTreeObserver
                    obs.removeGlobalOnLayoutListener(this)

                    // ... 텍스트에 색 입히기
                    builder.append(expandText)
                    builder.setSpan(
                        StyleSpan(Typeface.NORMAL),
                        0,
                        builder.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.setSpan(
                        ForegroundColorSpan(
                            ContextCompat.getColor(
                                context,
                                R.color.text_dimmed
                            )
                        ), 0, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    var lineEndIndex = 0
                    if (maxLine <= 0) {
                        lineEndIndex = tv.layout.getLineEnd(0)
                    } else if (tv.lineCount >= maxLine) {
                        lineEndIndex = tv.layout.getLineEnd(maxLine - 1)
                    }


                    if (lineEndIndex > 0) {
                        val orgText = tv.text.toString()
                        var text: String? = null
                        // RTL에서 lineEndIndex가 잘못 오는 경우 대비 (알림모아보기 레이아웃 수정으로 현재는 발생하지 않음)
                        text = try {
                            orgText.subSequence(0, lineEndIndex - expandText.length + 1).toString()
                        } catch (e: Exception) {
                            orgText.subSequence(0, orgText.length - expandText.length + 1)
                                .toString()
                        }
                        val messageEdit = Editable.Factory.getInstance().newEditable("")
                        messageEdit.append(text)
                        messageEdit.append(builder)
                        tv.text = messageEdit
                    }
                }
            })
        }

        fun isLargeFont(context: Context): Boolean {
            val scale = context.resources.configuration.fontScale
            return scale >= 1.29
        }

        fun chattingExist(context: Context): String {
            val mostIdol = getAccount(context)!!.userModel!!.most
            val idolSoloId = mostIdol!!.getId()
            val idolGroupId = mostIdol.groupId
            v("idolSoloId : $idolSoloId")
            v("idolGroupId : $idolGroupId")
            var idolName = ""
            val idolFullName = mostIdol.getName()
            var idolSoloName = ""
            var idolGroupName = ""

            if (idolFullName.contains("_")) {
                val SoloName =
                    idolFullName.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                idolSoloName = SoloName[0]
                idolGroupName = SoloName[1]
            }

            val r = Runnable {
                val roomList = getInstance(context).getJoinedRoom(1)
                for (i in roomList!!.indices) {
                    if (idolSoloId == roomList[i].idolId) {
                        isSoloExist = true
                    } else if (idolGroupId == roomList[i].idolId) {
                        isGroupExist = true
                    }
                }
            }
            val thread = Thread(r)
            thread.start()


            v("isSoloExist = true")
            v("isGroupExist = true")
            synchronized(thread) {
                if (isSoloExist && isSoloExist) {
                    idolName = "$idolSoloName,$idolGroupName"
                    v("aaaaa")
                } else if (isSoloExist && !isGroupExist) {
                    idolName = idolSoloName
                    v("bbbbb")
                } else {
                    idolName = idolGroupName
                    v("ccccc")
                }
                return idolName
            }
        }

        //해당  유저의 채팅 db를 없애준다.
        fun deleteChatDB(context: Context?, userId: Int) {
            //해당 chat db 지우도록 수정
            context?.deleteDatabase(userId.toString() + "_chat.db")
        }

        //앱 포그라운드인지 확인.
        fun isAppOnForeground(context: Context): Boolean {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses ?: return false

            val packageName = context.packageName
            for (appProcess in appProcesses) {
                if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName) {
                    return true
                }
            }

            return false
        }

        //비광봤을 경우 깜짝 다이아 나왔을 때 처리
        fun surpriseDia(activity: BaseActivity, response: JSONObject) {
            if (response.has("diamond")) {
                val rewardBottomSheetDialogFragment = newInstance(
                    RewardBottomSheetDialogFragment.FLAG_SURPRISE_DIA_REWARD,
                    response.optInt("diamond"),
                    response.optString("msg")
                )

                val tag = "surprise_dia_dialog"

                val oldFrag = activity.supportFragmentManager.findFragmentByTag(tag)
                if (oldFrag == null) {
                    rewardBottomSheetDialogFragment.show(activity.supportFragmentManager, tag)
                }
            }
        }

        //언어별 숫자 표시 다르게 사용
        fun mostCountLocale(context: Context?, mostCount: Int): String {
            var localeMostCount =
                NumberFormat.getNumberInstance(Locale.getDefault()).format(mostCount.toLong())

            localeMostCount = if (mostCount < 100) {
                "<" + NumberFormat.getNumberInstance(Locale.getDefault())
                    .format(100)
            } else if (mostCount < 1000) {
                "$localeMostCount+"
            } else if (mostCount < 1000000) { //1000이상 100만명 미만
                NumberFormat.getNumberInstance(Locale.getDefault())
                    .format((mostCount / 1000).toLong()) + "K+"
            } else if (mostCount < 1000000000) { //100만명 이상 10억 미만
                NumberFormat.getNumberInstance(Locale.getDefault())
                    .format((mostCount / 1000000).toLong()) + "M+"
            } else {   //10억 이상
                NumberFormat.getNumberInstance(Locale.getDefault())
                    .format((mostCount / 1000000000).toLong()) + "B+"
            }

            //중국어일 때
            if (getSystemLanguage(context).equals("zh_CN", ignoreCase = true) || getSystemLanguage(
                    context
                ).equals("zh_TW", ignoreCase = true)
            ) {
                if (mostCount < 100) {
                    localeMostCount = "<1百"
                } else if (mostCount < 1000) {
                    localeMostCount = mostCount.toString().substring(0, 1) + "百+"
                } else if (mostCount < 10000) {
                    localeMostCount = (mostCount / 1000).toString() + "千+"
                } else if (mostCount < 100000000) {  // 1만이상 1억미만
                    localeMostCount = if (getSystemLanguage(context)
                            .equals("zh_TW", ignoreCase = true)
                    ) {
                        (mostCount / 10000).toString() + "萬+" //최소 0은 4개를 가지고 있으니, 4개를 자른다.
                    } else {
                        (mostCount / 10000).toString() + "万+"
                    }
                }
            }
            return localeMostCount
        }

        fun getSecureId(context: Context?): String {
            val idolAccount = getAccount(context)
            val userId = idolAccount!!.userId
            val a = (0x163F2857 xor userId)
            val b = ((a and 0x0F0F0F0F) shl 4 or ((a and -0xf0f0f10) shr 4))
            return Integer.toHexString(b).uppercase(Locale.getDefault())
        }

        // TypeListModel을 사용하는 java 클래스를 kotlin으로 변환하기 전까지 임시로 사용할 함수
        fun getUiColor(context: Context?, typeList: TypeListModel): String {
            return if (isUsingNightModeResources(context)) {
                typeList.uiColorDarkmode
            } else {
                typeList.uiColor
            }
        }

        fun getFontColor(context: Context?, typeList: TypeListModel): String {
            return if (isUsingNightModeResources(context)) {
                typeList.fontColorDarkmode
            } else {
                typeList.fontColor
            }
        }

        // kotlin extension(ContextExt)에 있는 것과 같은 기능을 하는 함수
        fun getActivityFromContext(context: Context?): Activity? {
            var context = context
            while (context is ContextWrapper) {
                if (context is Activity) {
                    return context
                }
                context = context.baseContext
            }
            return null // Activity를 찾을 수 없으면 null 반환
        }
    }
}
