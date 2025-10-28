package net.ib.mn.activity


import android.content.Context
import android.content.Intent
import android.database.CursorIndexOutOfBoundsException
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.adapter.FriendsAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.friends.FriendsRepositoryImpl
import net.ib.mn.databinding.ActivityNewFriendsBinding
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ExtendedDataHolder
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class NewFriendsActivity : BaseActivity(),
        FriendsAdapter.OnClickListener {


    private var myStatusMessage = ""

    private var mLastClickTime = 0L
    companion object {
        const val FRIENDS_INFO = "FRIENDS_INFO"
        var didApplyNewFriends: String? = null

        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, NewFriendsActivity::class.java)
        }
    }

    private lateinit var mContext: Context
    private var mAccount: IdolAccount? = null

    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var mFriends = ArrayList<FriendModel>()
    private var mFriendsSize: Int = 0
    private var mRecommendedFriends = ArrayList<FriendModel>()
    private var mFriendsAdapter: FriendsAdapter? = null
    private var isAllowFriendReq: Boolean = true
    private lateinit var binding: ActivityNewFriendsBinding
    @Inject
    lateinit var friendsRepository: FriendsRepositoryImpl
    @Inject
    lateinit var usersRepository: UsersRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_friends)
        binding.clContainer.applySystemBarInsets()

        mContext = this
        mAccount = IdolAccount.getAccount(mContext)


        // 뉴프렌즈를 한 번이라도 봤다면 flag를 설정
        if (Util.getPreferenceBool(this@NewFriendsActivity,
                        Const.PREF_SHOW_SET_NEW_FRIENDS, true)) {

            Util.setPreference(this@NewFriendsActivity,
                    Const.PREF_SHOW_SET_NEW_FRIENDS,
                    false)
        }

        setActionbar()
        setNewFriends()
        setNewFriendsBtn()
        setFriends()
        getFriends()
    }

    override fun onResume() {
        super.onResume()
        getNewFriends()
    }

// 기존  ExtendedDataHolder에서 값을 가지고 있으므로, 엑티비티가  다시 시작해도  프렌즈 리스트 값은 유지됨으로
// 아래 코드는  불필요한  코드로 간주하여,  주석 처리
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//
//        val jsonArray = JSONArray()
//        val gson = IdolGson.getInstance()
//        for (i in 0 until mFriends.size) {
//            jsonArray.put(JSONObject(gson.toJson(mFriends[i])))
//        }
//
//        outState.putString(FRIENDS_INFO, jsonArray.toString())
//    }
//
//    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
//        super.onRestoreInstanceState(savedInstanceState)
//
//        if (savedInstanceState != null) {
//            val friendsInfo = savedInstanceState.getString(FRIENDS_INFO)
//            if (friendsInfo != null) {
//                val gson = IdolGson.getInstance()
//                val listType: Type? = object : TypeToken<List<FriendModel>>() {}.type
//
//                mFriends =
//                    gson.fromJson(friendsInfo, listType)
//                mFriendsSize = mFriends.size
//            }
//        }
//    }

    override fun onItemClicked(item: UserModel, view: View, position: Int) {
        when (view.id) {
            R.id.picture,
            R.id.userInfo -> {
                setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,
                        "friends_feed")
                startActivity(FeedActivity.createIntent(this, item))
            }
            R.id.btnReqFriend -> {
                if(SystemClock.elapsedRealtime() - mLastClickTime < 300){
                    return@onItemClicked
                }
                else {
                    requestFriend(item, position)
                }
                mLastClickTime = SystemClock.elapsedRealtime()
            }
        }
    }

    private fun setActionbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setTitle(R.string.new_friends)
    }

    private fun setFriends() {
        mFriendsAdapter = FriendsAdapter(this, Glide.with(this), mRecommendedFriends,true, this)

        mLayoutManager = LinearLayoutManager(this)
        binding.rvFriends.adapter = mFriendsAdapter
        binding.rvFriends.layoutManager = mLayoutManager
    }

    private fun setNewFriends() {
        val userModel = mAccount?.userModel ?: return
        lifecycleScope.launch {
            usersRepository.getStatus(
                userId = userModel.id,
                listener = { response ->
                    if (response.optBoolean("success")) {
                        if (!response.isNull("status_message")) {
                            userModel.statusMessage = response.optString("status_message")
                            myStatusMessage = userModel.statusMessage ?: ""
                        }
                        // 뉴프렌즈 신청 여부
                        didApplyNewFriends = response.optString("new_friends", "N")
                        // 친구 요청 여부
                        isAllowFriendReq = response.optString("friend_allow", "Y") == "Y"

                        if (didApplyNewFriends == "Y") {
                            setOffNewFriendsBtn()
                        } else {
                            setOnNewFriendsBtn()
                        }
                    }
                },
                errorListener = {
                    Util.showDefaultIdolDialogWithBtn1(this@NewFriendsActivity, null,
                        getString(R.string.desc_failed_to_connect_internet)
                    ) {
                        Util.closeIdolDialog()
                        finish()
                    }
                }
            )
        }
    }

    private fun setNewFriendsBtn() {
        binding.btnNewFriends.setOnClickListener {

            when {
                didApplyNewFriends == "Y" -> {
                    didApplyNewFriends = "N"
                    setStatus()
                }
                isAllowFriendReq -> {
                    didApplyNewFriends = "Y"
                    setStatus()
                }
                else -> Util.showDefaultIdolDialogWithBtn1(this@NewFriendsActivity,
                        null,
                        getString(R.string.apply_new_friends_blocked)
                ) {
                    Util.closeIdolDialog()
                }
            }
        }
    }

    private fun setStatus() {
        lifecycleScope.launch {
            usersRepository.setStatus(
                newFriends = didApplyNewFriends,
                listener = { response ->
                    Util.closeProgress()
                    if (response.optBoolean("success", false)) {
                        if (didApplyNewFriends == "Y") {
                            setOffNewFriendsBtn()
                        } else {
                            setOnNewFriendsBtn()
                        }
                    } else {
                        // 실패하면 다시 원래대로 바꾸기
                        if (didApplyNewFriends == "Y") {
                            didApplyNewFriends = "N"
                            setOnNewFriendsBtn()
                        } else {
                            didApplyNewFriends = "Y"
                            setOffNewFriendsBtn()
                        }
                        // msg 출력
                        if( response.optString("msg").length > 0 ) {
                            Util.showDefaultIdolDialogWithBtn1(this@NewFriendsActivity,
                                null,
                                response?.optString("msg")
                            ) {
                                Util.closeIdolDialog()
                            }
                        }

                    }
                },
                errorListener = {
                    // 실패하면 다시 원래대로 바꾸기
                    if (didApplyNewFriends == "Y") {
                        didApplyNewFriends = "N"
                        setOnNewFriendsBtn()
                    } else {
                        didApplyNewFriends = "Y"
                        setOffNewFriendsBtn()
                    }

                    Util.showDefaultIdolDialogWithBtn1(this@NewFriendsActivity, null,
                        getString(R.string.desc_failed_to_connect_internet)
                    ) {
                        Util.closeIdolDialog()
                    }
                }
            )
        }
    }

    private fun setOnNewFriendsBtn() {
        binding.btnNewFriends.setText(R.string.apply_new_friends)
        binding.btnNewFriends.setBackgroundResource(if( BuildConfig.CELEB ) R.drawable.bg_radius_main_celeb else R.drawable.btn_bg_radius_brand450)
        binding.btnNewFriends.setTextColor(ContextCompat.getColor(this, R.color.text_white_black))

        mRecommendedFriends
                .withIndex()
                .find {
                    it.value.user.id == mAccount?.userModel?.id
                }?.let {
                    mRecommendedFriends.removeAt(it.index)
                    mFriendsAdapter?.notifyItemRemoved(it.index)

                    if (mRecommendedFriends.size > 0) {
                        hideEmpty()
                    } else {
                        showEmpty()
                    }
                }
    }

    private fun setOffNewFriendsBtn() {
        //내 최애 업데이트 안되는 경우가 있어서 한번더  불러옴.
        mAccount = IdolAccount.getAccount(mContext) ?: return
        binding.btnNewFriends.setText(R.string.cancel_new_friends)
        binding.btnNewFriends.setBackgroundResource(R.drawable.btn_bg_radius_gray100)
        binding.btnNewFriends.setTextColor(ContextCompat.getColor(this, R.color.gray580))

        if (mRecommendedFriends.size == 0) {
            hideEmpty()
        }
        mAccount!!.userModel?.statusMessage = myStatusMessage
        mRecommendedFriends.add(0, FriendModel(mAccount!!.userModel ?: UserModel(), "N",""))
        mFriendsAdapter?.notifyItemInserted(0)
        binding.rvFriends.scrollToPosition(0)
    }


    private fun getFriends() {
        val extras = ExtendedDataHolder.getInstance()
        if (extras.hasExtra("friends")) {
            val response = JSONObject(extras.getExtra("friends") as String)

            if (response.optBoolean("success")) {
                try {
                    val array = response.getJSONArray("objects")
                    mFriends.clear()
                    val gson = IdolGson.getInstance()
                    for (i in 0..array.length()) {
                        val model = gson.fromJson(array.getJSONObject(i).toString(), FriendModel::class.java)

                        mFriends.add(model)
                    }
                    mFriendsSize = mFriends.size
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

        }
    }

    private fun getNewFriends() {
        Util.showProgress(this)
        lifecycleScope.launch {
            usersRepository.newFriendsRecommend(
                { response ->
                    val array = response?.getJSONArray("objects")
                    val gson = IdolGson.getInstance()

                    mRecommendedFriends.clear()

                    for (i in 0 until array?.length()!!) {
                        val model = gson.fromJson(array.getJSONObject(i).toString(), UserModel::class.java)

                        if(mAccount?.userModel?.id == model.id)
                            myStatusMessage = model.statusMessage ?: ""

                        if (!mFriends.any { it.user.id == model.id }) {
                            mRecommendedFriends.add(FriendModel(model, "N", ""))
                        }
                    }
                    mFriendsAdapter?.notifyDataSetChanged()

                    if (mRecommendedFriends.size > 0) {
                        hideEmpty()
                    } else {
                        showEmpty()
                    }
                    Util.closeProgress()
                }, {
                    Util.closeProgress()
                }
            )
        }
    }

    private fun requestFriend(user: UserModel, index: Int) {
        // 친구 제한 꽉차면 꽉 찼다는 알림 주기
        if (Util.getPreferenceBool(this, Const.PREF_FRIENDS_LIMIT, false)
                || mFriendsSize == Const.THE_NUMBER_OF_FRIENDS_LIMIT) {
            Util.closeProgress()
            Util.showDefaultIdolDialogWithBtn1(
                    this@NewFriendsActivity,
                    null,
                    getString(R.string.error_8000)
            ) {
                Util.closeIdolDialog()
            }
        } else {
            MainScope().launch {
                friendsRepository.sendFriendRequest(
                    user.id.toLong(),
                    { response ->
                        if (response?.optBoolean("success")!!) {
                            Util.closeProgress()

                            mRecommendedFriends
                                .withIndex()
                                .find {
                                    it.value.user.id == user.id
                                }?.let {
//                                            mFriendsSize += 1
                                    mFriends.add(mRecommendedFriends[it.index])     //치누 요청을 Send했을 때 추가하는 로직. 뉴프렌즈 화면에서 사용자 피드를 들어갔다가 뒤로가기하면 신청한 유저가 다시 보여서 추
                                    mRecommendedFriends.removeAt(it.index)
                                    mFriendsAdapter?.notifyItemRemoved(it.index)

                                    if (mRecommendedFriends.size > 0) {
                                        hideEmpty()
                                    } else {
                                        showEmpty()
                                    }
                                }

                            try {
                                Toast.makeText(mContext, getString(R.string.friend_request_sent),
                                    Toast.LENGTH_SHORT).show()
                            } catch (e: CursorIndexOutOfBoundsException) {
                                e.printStackTrace()
                            }

                            setResult(ResultCode.FRIEND_REQUESTED.value)

                        } else {
                            Util.closeProgress()
                            val errMsg = ErrorControl.parseError(mContext, response)
                            Util.showDefaultIdolDialogWithBtn1(mContext,
                                null,
                                errMsg) { Util.closeIdolDialog() }
                        }
                    },
                    { throwable ->
                        Util.closeProgress()
                        Util.showDefaultIdolDialogWithBtn1(this@NewFriendsActivity,
                            null,
                            throwable.message) { Util.closeIdolDialog() }
                    }
                )
            }
        }
    }

    private fun hideEmpty() {
        binding.rvFriends.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
    }

    private fun showEmpty() {
        binding.rvFriends.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
    }
}