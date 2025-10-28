/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 친구 삭제 화면
 *
 * */

package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.adapter.FriendDeleteAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.friends.FriendsRepositoryImpl
import net.ib.mn.databinding.ActivityFriendDeleteBinding
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONException
import javax.inject.Inject

@AndroidEntryPoint
class FriendDeleteActivity : BaseActivity(), OnClickListener, FriendDeleteAdapter.OnClickListener {

    private lateinit var binding: ActivityFriendDeleteBinding

    private var mSheet: BottomSheetFragment? = null

    private var friendDeleteAdapter: FriendDeleteAdapter? = null

    private var mItems = ArrayList<UserModel>()

    @Inject
    lateinit var friendsRepository: FriendsRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_friend_delete)
        binding.clContainer.applySystemBarInsets()

        init()
    }

    private fun init() {
        val actionbar = supportActionBar
        actionbar?.setDisplayHomeAsUpEnabled(true)
        actionbar?.setHomeButtonEnabled(true)
        actionbar?.setTitle(R.string.title_friend_delete)

        mSheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_FRIEND_DELETE_FILTER)

        friendDeleteAdapter = FriendDeleteAdapter(this, this, mItems)
        binding.rvFriends.adapter = friendDeleteAdapter

        binding.llFilter.setOnClickListener(this)
        binding.btnDelete.setOnClickListener(this)
        getFriends()
    }

    private fun getFriends() {
        Util.showProgress(this)
        MainScope().launch {
            friendsRepository.getFriendsSelf(
                { response ->
                    if (!response.optBoolean("success")) {
                        Util.closeProgress()
                        val responseMsg = ErrorControl.parseError(
                            this@FriendDeleteActivity,
                            response,
                        )
                        if (!isFinishing) {
                            makeText(
                                this@FriendDeleteActivity,
                                responseMsg,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        return@getFriendsSelf
                    }
                    try {
                        mItems.clear()
                        val array = response.getJSONArray("objects")
                        val gson = IdolGson.getInstance()
                        for (i in 0 until array.length()) {
                            val model: FriendModel = gson.fromJson(
                                array.getJSONObject(i).toString(),
                                FriendModel::class.java,
                            )
                            if (model.isFriend == "Y") {
                                val userModel = model.user
                                userModel.giveHeart = model.giveHeart
                                mItems.add(userModel)
                            }
                        }
                        if (mItems.size > 0) {
                            mItems.sortWith(compareBy { it.lastAct })
                            friendDeleteAdapter?.setItems(mItems)
                            hideEmpty()
                        } else {
                            showEmpty()
                        }
                        Util.closeProgress()
                    } catch (e: JSONException) {
                        Util.closeProgress()
                        e.printStackTrace()
                    }
                },
                { throwable ->

                }
            )
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.llFilter -> {
                val tag = "filter"
                val oldFrag = supportFragmentManager.findFragmentByTag(tag)
                if (oldFrag == null) {
                    mSheet!!.show(supportFragmentManager, tag)
                }
            }
            binding.btnDelete -> {
                deleteBtnClicked()
            }
        }
    }

    override fun onItemClicked(item: UserModel?) {
        if (item != null) {
            mItems.find { it.id == item.id }?.deleteChecked = item.deleteChecked
        }
    }

    fun filterByName() {
        setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION, "friend_order_name")
        binding.tvFilter.setText(R.string.order_by_name)
        mItems.sortWith(compareBy { it.nickname })
        friendDeleteAdapter?.setItems(mItems)
    }

    fun filterByLoginTime() {
        setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION, "friend_order_time")
        binding.tvFilter.setText(R.string.order_by_login_time)
        mItems.sortWith(compareBy { it.lastAct })
        friendDeleteAdapter?.setItems(mItems)
    }

    fun filterByHeart() {
        setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION, "friend_order_heart")
        binding.tvFilter.setText(R.string.order_by_heart)
        mItems.sortWith(compareBy({ it.giveHeart }, { it.lastAct }))
        friendDeleteAdapter?.setItems(mItems)
    }

    private fun showEmpty() {
        with(binding) {
            empty.visibility = View.VISIBLE
            empty.text = getString(R.string.empty_friends)
            rvFriends.visibility = View.GONE
        }
    }

    // 불러올 친구 없을 경우 호출
    private fun hideEmpty() {
        with(binding) {
            empty.visibility = View.GONE
            rvFriends.visibility = View.VISIBLE
        }
    }

    private fun deleteBtnClicked() {
        val ids = mItems.filter { it.deleteChecked }.map { it.id } as ArrayList<Int>
        if (ids.isEmpty()) return

        Util.showDefaultIdolDialogWithBtn2(
            this,
            null,
            getString(R.string.friends_delete_message),
            {
                Util.closeIdolDialog()
                setResult(ResultCode.FRIEND_REMOVED.value)
                Util.showProgress(this@FriendDeleteActivity)
                MainScope().launch {
                    friendsRepository.deleteFriends(
                        ids,
                        { response ->
                            Util.closeProgress()
                            if (!response.optBoolean("success")) {
                                val responseMsg =
                                    ErrorControl.parseError(this@FriendDeleteActivity, response)
                                makeText(
                                    this@FriendDeleteActivity,
                                    responseMsg,
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@deleteFriends
                            }
                            Util.showDefaultIdolDialogWithBtn1(
                                this@FriendDeleteActivity,
                                null,
                                getString(R.string.tiele_friend_delete_result),
                            ) {
                                Util.closeIdolDialog()
                                val removeFriends = mItems.filter { ids.contains(it.id) } as MutableList<UserModel>
                                mItems.removeAll(removeFriends.toSet())
                                friendDeleteAdapter?.setItems(mItems)
                            }
                        },
                        { throwable ->
                            Util.closeProgress()
                            if (!isFinishing) {
                                makeText(
                                    this@FriendDeleteActivity,
                                    throwable.message,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    )
                }
            },
        ) { Util.closeIdolDialog() }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, FriendDeleteActivity::class.java)
        }
    }
}