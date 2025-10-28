package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.adapter.SearchedUsersAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.friends.FriendsRepositoryImpl
import net.ib.mn.databinding.ActivityFriendSearchBinding
import net.ib.mn.fragment.DataFragment
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ExtendedDataHolder
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.view.EndlessRecyclerViewScrollListener
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Type
import javax.inject.Inject

@AndroidEntryPoint
class FriendSearchActivity : BaseActivity(),
        SearchedUsersAdapter.OnClickListener {

    companion object {
        const val FRIENDS_INFO = "FRIENDS_INFO"
        const val PARAM_KEYWORD = "PARAM_KEYWORD"
        const val SIZE_OF_SEARCHED_USER = 100

        @JvmStatic
        fun createIntent(context: Context, keyword: String): Intent {
            val intent = Intent(context, FriendSearchActivity::class.java)
            intent.putExtra(PARAM_KEYWORD, keyword)
            return intent
        }
    }

    @Inject
    lateinit var friendsRepository: FriendsRepositoryImpl
    @Inject
    lateinit var usersRepository: UsersRepository

    lateinit var mKeyword: String
    private var isFinished: Boolean = false
    private lateinit var imm: InputMethodManager

    private lateinit var mScrollListener: EndlessRecyclerViewScrollListener
    private lateinit var mAdapter: SearchedUsersAdapter
    private var mHeldFriends = ArrayList<FriendModel>()
    private val mAlreadyFriends = ArrayList<FriendModel>()
    private val mSearchedUsers = ArrayList<FriendModel>()

    private var dataFragment : DataFragment? = null

    private lateinit var binding: ActivityFriendSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var fm = supportFragmentManager
        dataFragment = fm.findFragmentByTag("data") as? DataFragment
        if( dataFragment == null ) {
            dataFragment = DataFragment()
            fm.beginTransaction().add(dataFragment!!, "data").commit()

            dataFragment!!.setFriendsData(getSerializedFriends())
        }

        setInit()
    }

    fun getSerializedFriends() : String {
        val jsonArray = JSONArray()
        val gson = IdolGson.getInstance()
        for (i in 0 until mHeldFriends.size) {
            jsonArray.put(JSONObject(gson.toJson(mHeldFriends[i])))
        }

        return jsonArray.toString()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        dataFragment?.setFriendsData(getSerializedFriends())
//        outState.clear()
//
//        val jsonArray = JSONArray()
//        val gson = IdolGson.getInstance()
//        for (i in 0 until mHeldFriends.size) {
//            jsonArray.put(JSONObject(gson.toJson(mHeldFriends[i])))
//        }
//
//        outState.putString(FRIENDS_INFO, jsonArray.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val friendsInfo = dataFragment?.getFriendsData()
        if( friendsInfo != null ) {
            val gson = IdolGson.getInstance()
            val listType: Type? = object : TypeToken<List<FriendModel>>() {}.type
            mHeldFriends =
                    gson.fromJson(friendsInfo, listType)
        }
//        if (savedInstanceState != null) {
//            val friendsInfo = savedInstanceState.getString(FRIENDS_INFO)
//            if (friendsInfo != null) {
//                val gson = IdolGson.getInstance()
//                val listType: Type? = object : TypeToken<List<FriendModel>>() {}.type
//                mHeldFriends =
//                        gson.fromJson(friendsInfo, listType)
//            }
//        }
    }

    override fun onItemClicked(item: UserModel, view: View, position: Int) {
        when (view.id) {
            R.id.picture,
            R.id.userInfo -> {
                setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,
                        "friends_feed")
                startActivity(FeedActivity.createIntent(this, item))
            }
            R.id.btnReqFriend -> {
                requestFriend(item, position)
            }
        }
    }

    private fun setInit() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_friend_search)
        binding.clContainer.applySystemBarInsets()
        setTitle(R.string.search_friend)

        mKeyword = intent.getStringExtra(PARAM_KEYWORD).toString()

        val extras = ExtendedDataHolder.getInstance()
        if (extras.hasExtra("friends")) {
            val response = extras.getExtra("friends") as String?
            if (response != null) {
                try {
                    parseResponse(JSONObject(response))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }

        binding.searchBar.etSearch.setText(mKeyword)

        mAdapter = SearchedUsersAdapter(this,
                Glide.with(this),
                mAlreadyFriends,
                mSearchedUsers,
                this)
        val llm = LinearLayoutManagerWrapper(this, LinearLayoutManager.VERTICAL, false)
        mScrollListener = object : EndlessRecyclerViewScrollListener(llm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                if (!isFinished) {
                    val size = mAlreadyFriends.size + mSearchedUsers.size
                    getSearchResult(size)
                }
            }
        }
        binding.rvFriends.layoutManager = llm
        binding.rvFriends.adapter = mAdapter
        binding.rvFriends.addOnScrollListener(mScrollListener)

        setSearch()
        searchFriends()
        getSearchResult(0)
    }

    private fun showEmpty() {
        binding.tvEmpty.visibility = View.VISIBLE
        binding.rvFriends.visibility = View.GONE
    }

    private fun hideEmpty() {
        binding.tvEmpty.visibility = View.GONE
        binding.rvFriends.visibility = View.VISIBLE
    }

    private fun showSoftKeyboard() {
        binding.searchBar.etSearch.requestFocus()
        binding.searchBar.etSearch.isCursorVisible = true
        imm.showSoftInput(binding.searchBar.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideSoftKeyboard() {
        binding.searchBar.etSearch.isCursorVisible = false
        imm.hideSoftInputFromWindow(binding.searchBar.etSearch.windowToken, 0)
    }

    private fun setSearch() {
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        binding.searchBar.etSearch.setOnClickListener {
            showSoftKeyboard()
        }
        binding.searchBar.etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchUser()
                true
            } else {
                false
            }
        }
        binding.searchBar.btnSearch.setOnClickListener {
            searchUser()
        }
    }

    private fun searchUser() {

        mKeyword = binding.searchBar.etSearch.text.toString().trim()
        if (mKeyword!!.length > 1) {
            hideSoftKeyboard()

            Util.showProgress(this)
            Handler().postDelayed({
                mAlreadyFriends.clear()
                mSearchedUsers.clear()
                searchFriends()
                getSearchResult(0)
            }, 500)
        } else {
            Toast.makeText(this,
                    String.format(getString(R.string.comment_minimum_characters), 2),
                    Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseResponse(response: JSONObject) {

        if (response.optBoolean("success")) {
            val array: JSONArray
            try {
                mHeldFriends.clear()
                array = response.getJSONArray("objects")
                val gson = IdolGson.getInstance()
                for (i in 0 until array.length()) {
                    val model: FriendModel = gson.fromJson(array.getJSONObject(i).toString(),
                            FriendModel::class.java)

                    mHeldFriends.add(model)
                }

                //접속일 오름차순 -> 가나다순 정렬
                mHeldFriends.sortWith(Comparator { lhs, rhs -> lhs.user.nickname.compareTo(rhs.user.nickname) })

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

    }

    private fun searchFriends() {
        mAlreadyFriends.addAll(mHeldFriends.filter {
            it.user.nickname.contains(mKeyword.toString(), ignoreCase = true)
        }.filter {
            it.isFriend == "Y"
        })
    }

    private fun getSearchResult(offset: Int) {
        Util.closeProgress()
        lifecycleScope.launch {
            usersRepository.searchNickname(
                q = mKeyword,
                offset = offset,
                limit = SIZE_OF_SEARCHED_USER,
                listener = { response ->
                    if (response.optBoolean("success")) {
                        val gson = IdolGson.getInstance()
                        val array = response.getJSONArray("objects")

                        if (array.length() == 0) {
                            isFinished = true

                            if (offset == 0) {
                                showEmpty()
                            }
                        } else {
                            hideEmpty()
                            for (i in 0 until array.length()) {
                                val model = gson.fromJson(array.getJSONObject(i).toString(), UserModel::class.java)
                                val heldFriend = mHeldFriends.find {
                                    it.user.id == model.id
                                }

                                if (heldFriend == null) {
                                    mSearchedUsers.add(FriendModel(model, "N", ""))
                                } else {
                                    if (heldFriend.isFriend == "N") {
                                        mSearchedUsers.add(heldFriend)
                                    }
                                }
                            }
                            mAdapter.notifyDataSetChanged()
                        }
                    } else {
                        Util.showDefaultIdolDialogWithBtn1(this@FriendSearchActivity,
                            null,
                            getString(R.string.msg_error_ok)) {
                            finish()
                        }
                    }
                },
                errorListener = {
                    Util.showDefaultIdolDialogWithBtn1(this@FriendSearchActivity,
                        null,
                        getString(R.string.msg_error_ok)) {
                        finish()
                    }
                }
            )
        }
    }

    private fun requestFriend(user: UserModel, index: Int) {
        // 친구 제한 꽉차면 꽉 찼다는 알림 주기
        if (Util.getPreferenceBool(this, Const.PREF_FRIENDS_LIMIT, false)
                || mHeldFriends.size == Const.THE_NUMBER_OF_FRIENDS_LIMIT) {
            Util.closeProgress()
            Util.showDefaultIdolDialogWithBtn1(
                    this@FriendSearchActivity,
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

                            mHeldFriends.add(FriendModel(user, "N", FriendModel.RECV_USER))
                            mSearchedUsers.find { it.user.id == user.id }
                                ?.let {
                                    it.userType = FriendModel.RECV_USER
                                    mAdapter.notifyItemChanged(mAlreadyFriends.size + index)
                                }

                            if (mAdapter.itemCount > 0) {
                                hideEmpty()
                            } else {
                                showEmpty()
                            }

                            setResult(ResultCode.FRIEND_REQUESTED.value)

                            Toast.makeText(this@FriendSearchActivity, getString(R.string.friend_request_sent),
                                Toast.LENGTH_SHORT).show()
                        } else {
                            Util.closeProgress()
                            val errMsg = ErrorControl.parseError(this@FriendSearchActivity, response)
                            if (errMsg != null) {
                                Util.showDefaultIdolDialogWithBtn1(this@FriendSearchActivity,
                                    null,
                                    errMsg) { Util.closeIdolDialog() }
                            }
                        }
                    },
                    { throwable ->
                        Util.closeProgress()
                        Util.showDefaultIdolDialogWithBtn1(this@FriendSearchActivity,
                            null,
                            throwable.message) { Util.closeIdolDialog() }
                    }
                )
            }
        }
    }

}