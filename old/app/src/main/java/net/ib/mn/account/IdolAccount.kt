package net.ib.mn.account

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.friends.FriendsRepository
import net.ib.mn.core.model.BadWordModel
import net.ib.mn.model.FriendModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.UserModel
import net.ib.mn.model.UserStamp
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Util
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class IdolAccount constructor(email: String?, token: String?, domain: String?) {
    enum class FriendType {
        NOT_FRIEND,
        WAITING,
        FRIEND
    }

    fun clearAccount(context: Context) {
        removeAccount(context)
        sAccount = null
    }

    // account에 변경이 있을 때 (예:최애 변경) 저장
    fun saveAccount(context: Context) {
        val prefs = context
            .getSharedPreferences(PREFS_ACCOUNT, 0)
        val editor = prefs.edit()
        val gson = IdolGson.getInstance(false)
        val json = gson.toJson(sAccount)
        //        Util.log("IdolAccount saveAccount="+json);
        editor.putString(PREF_KEY__GSON, json)
        editor.commit()
    }

    interface FetchUserInfoListener {
        fun onSuccess()
        fun onFailure(msg: String?)
    }

    interface FetchFriendsInfoListener {
        fun onSuccess()
        fun onFailure(msg: String?)
    }

    var email: String?
    var token: String?
    var domain: String?

    var userModel: UserModel? = null
        private set
    var heart = 0
        private set
    var userId = 0
        private set
    var chartCodes:ArrayList<String>? = null
        private set
    private var mEventList: String? = null
    private var mUserAct: String? = null // 내가쓴글과 댓글
    private var mNeedChangeInfo: String? = null // 회원정보 변경필요 유무 ( 기존 카카오톡 가입자 v3.2.8 에게 닉네임

    // 변경 가능하도록 한다.)
    private var mGoogleReview: String? = null
    var createdAt: Date? = null
        private set
    @JvmField
    var mDailyPackHeart = 0 // 데일리팩 하트 지급받은 경우 설정
    private val mFriends: MutableMap<Int, Friend> = HashMap()
    private var userStamp: UserStamp? = null

    class Friend(var user: UserModel, @JvmField var type: FriendType)

    init {
        this.email = email
        this.token = token
        this.domain = domain
    }

    fun setToken(context: Context, token: String?) {
        val prefs = context
            .getSharedPreferences(PREFS_ACCOUNT, 0)
        val editor = prefs.edit()
        editor.putString(PREF_KEY__TOKEN, token)
        editor.commit()
        this.token = token
    }

    val userResourceUri: String?
        get() = userModel?.resourceUri
    val userName: String
        get() = userModel?.nickname ?: ""
    val profileUrl: String
        get() = userModel?.imageUrl ?: ""

    val diaCount: Int
        get() = userModel?.diamond ?: 0
    val heartCount: Long
        get() = (userModel?.strongHeart ?: 0) +
            (userModel?.weakHeart ?: 0)
    val level: Int
        get() = userModel?.level ?: 0
    val levelHeart: Long
        get() = userModel?.levelHeart ?: 0
    var most: IdolModel?
        get() = userModel?.most
        set(most) {
            userModel?.most = most
        }

    fun hasUserInfo(): Boolean {
        return userModel != null
    }

    fun fetchFriendsInfo(
        context: Context?,
        friendsRepository: FriendsRepository,
        listener: FetchFriendsInfoListener?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            friendsRepository.getFriendsSelf(
                { response ->
                    if (response.optBoolean("success")) {
                        val array = response.optJSONArray("objects") ?: JSONArray()
                        val gson = IdolGson.getInstance(false)
                        mFriends.clear()
                        for (i in 0 until array.length()) {
                            var model: FriendModel
                            model = gson.fromJson(
                                array.optJSONObject(i).toString(),
                                FriendModel::class.java
                            )
                            if (model.isFriend == "Y") {
                                mFriends[model.user.id] = Friend(
                                    model.user,
                                    FriendType.FRIEND
                                )
                            } else {
                                mFriends[model.user.id] = Friend(
                                    model.user,
                                    FriendType.WAITING
                                )
                            }
                        }
                        listener?.onSuccess()
                    } else {
                        val responseMsg = ErrorControl.parseError(
                            context,
                            response
                        )
                        if (responseMsg != null) {
                            listener?.onFailure(responseMsg)
                        }
                    }
                },
                { throwable ->
                    listener?.onFailure(throwable.message)
                }
            )
        }
    }

    // 친구관리 들어갈 때 synchronous하게 처리하기 위해 별도로 분리
    @Throws(JSONException::class)
    fun setUserInfo(context: Context?, response: JSONObject) {
        try {
            val gson = IdolGson.getInstance(false)
            val array = response.getJSONArray("objects")
            val userInfo = array[0] as JSONObject
            userModel = gson.fromJson(
                userInfo.toString(),
                UserModel::class.java
            ) ?: return

            if (array.length() > 0) {
                val firstObject = array.optJSONObject(0)
                val mostObject = firstObject?.optJSONObject("most")
                val chartCodesArray = mostObject?.optJSONArray("chart_codes")
                if (chartCodesArray != null) {
                    val codeList = arrayListOf<String>()
                    for (i in 0 until chartCodesArray.length()) {
                        val chartCode = chartCodesArray.optString(i)
                        chartCode?.let {
                            codeList.add(chartCode)
                        }
                    }
                    Util.setPreferenceArray(context, Const.PREF_MOST_CHART_CODE, codeList)
                }
            }

            userId = userModel!!.id
            heart = userModel!!.heart
            mEventList = response.optString("event_list")
            val total_act = (response.optInt("article_count")
                    + response.optInt("comment_count"))
            mUserAct = Integer.toString(total_act)
            mNeedChangeInfo = response.optString("need_change_info")
            mGoogleReview = response.optString("google_review")
            userStamp =
                gson.fromJson(userInfo.getJSONObject("stamp").toString(), UserStamp::class.java)
            Util.setPreference(context, Const.PREF_IS_ABLE_ATTENDANCE, userStamp?.able ?: false)
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
            // 가입일 오류 수정
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            try {
                createdAt = formatter.parse(userInfo.optString("created_at"))
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            // 가입일 날아가는 현상 등 수정
            val prefs = context?.getSharedPreferences(PREFS_ACCOUNT, 0)
            val editor = prefs?.edit()
            val json = gson.toJson(this)
            Util.log("IdolAccount toJson:$json")
            editor?.putString(PREF_KEY__GSON, json)
            editor?.commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 사용자 하트수 별도 설정용
    fun setUserHearts(response: JSONObject) {
        try {
            if (response.has("weak_heart")) {
                userModel?.weakHeart = response.optLong("weak_heart")
            }
            if (response.has("strong_heart")) {
                userModel?.strongHeart = response.optLong("strong_heart")
            }
            if (response.has("level")) {
                userModel?.level = response.optInt("level")
            }
            if (response.has("level_heart")) {
                userModel?.levelHeart = response.optLong("level_heart")
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    fun setMostImageUrl(url: String?, url2: String?, url3: String?) {
        userModel?.most?.imageUrl = url
        userModel?.most?.imageUrl2 = url2
        userModel?.most?.imageUrl3 = url3
    }

    companion object {
        const val PREFS_ACCOUNT = "account"
        const val PREF_KEY__EMAIL = "email"
        const val PREF_KEY__TOKEN = "token"
        const val PREF_KEY__DOMAIN = "domain" // email, kakao, line
        const val PREF_KEY__GSON = "account_gson"
        @JvmStatic
        var sAccount: IdolAccount? = null
            private set
        @JvmField
        var badWords: ArrayList<BadWordModel>? = null
        @JvmStatic
        fun getAccount(context: Context?): IdolAccount? {
            if( context == null ) {
                return null
            }
            try {
                if (sAccount == null) {
                    val prefs = context.getSharedPreferences(
                        PREFS_ACCOUNT, 0
                    )
                    val email = prefs.getString(PREF_KEY__EMAIL, null)
                    val token = prefs.getString(PREF_KEY__TOKEN, null)
                    val domain = prefs.getString(PREF_KEY__DOMAIN, null)
                    if (email != null && token != null) {
                        sAccount = IdolAccount(email, token, domain)
                    }
                    val gson = IdolGson.getInstance(false)
                    val json = prefs.getString(PREF_KEY__GSON, null)
                    //                Util.log("IdolAccount getAccount="+json);
                    if (json != null) {
//            Util.log("IdolAccount from json:"+json);
                        sAccount = gson.fromJson(json, IdolAccount::class.java)
                        sAccount?.email = email
                        sAccount?.token = token
                        sAccount?.domain = domain
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return sAccount
        }

        @JvmStatic
        fun createAccount(
            context: Context, email: String?,
            token: String?, domain: String?
        ): IdolAccount? {
            val prefs = context
                .getSharedPreferences(PREFS_ACCOUNT, 0)
            val editor = prefs.edit()
            editor.putString(PREF_KEY__EMAIL, email)
            editor.putString(PREF_KEY__TOKEN, token)
            editor.putString(PREF_KEY__DOMAIN, domain)
            editor.commit()

            // 라인 또는 카카오로 로그인 후 다른 폰에서 계정탈퇴를 하고 다시 현재 폰에서 구글 로그인을 하는 경우 기존 계정 정보가 안지워져서 로그인이 안됨.
            // 로그인 후 기존 로그인 정보 날리고 새로 저장해야 함.
            sAccount = IdolAccount(email, token, domain)

            // 가입일 날아가는 현상 등 수정
            val gson = IdolGson.getInstance(false)
            val json = gson.toJson(sAccount)
            editor.putString(PREF_KEY__GSON, json)
            editor.commit()
            return sAccount
        }

        @JvmStatic
        fun removeAccount(context: Context) {
            val prefs = context
                .getSharedPreferences(PREFS_ACCOUNT, 0)
            val editor = prefs.edit()
            editor.clear()
            editor.commit()
        }
    }
}
