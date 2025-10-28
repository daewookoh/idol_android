package net.ib.mn.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.FriendsRequestActivity
import net.ib.mn.adapter.AcceptFriendsRequestAdapter
import net.ib.mn.core.data.repository.friends.FriendsRepositoryImpl
import net.ib.mn.databinding.FragmentAcceptFriendsRequestBinding
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Util
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class AcceptFriendsRequestFragment: BaseFragment(),
        AcceptFriendsRequestAdapter.OnClickListener {
    private lateinit var mActivity: FriendsRequestActivity
    private var mFriends = ArrayList<FriendModel>()
    private lateinit var mAdapter: AcceptFriendsRequestAdapter
    private lateinit var binding: FragmentAcceptFriendsRequestBinding
    @Inject
    lateinit var friendsRepository: FriendsRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAcceptFriendsRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity = activity as FriendsRequestActivity
        mFriends.addAll(mActivity.friends.filter {
            it.userType == FriendModel.SEND_USER
        })
        mAdapter = AcceptFriendsRequestAdapter(mActivity,
                mGlideRequestManager,
                mFriends,
                this@AcceptFriendsRequestFragment
        )
        binding.acceptFriendsRequestList.adapter = mAdapter
        if (mFriends.size > 0) {
            hideEmpty()
        } else {
            showEmpty()
        }
    }

    override fun onItemClicked(user: UserModel, view: View, position: Int) {
        when (view.id) {
            R.id.picture,
            R.id.userInfo,
            R.id.requesterPicture,
            R.id.requesterInfo -> {
                setUiActionFirebaseGoogleAnalyticsFragment(Const.ANALYTICS_BUTTON_PRESS_ACTION,
                        "friends_feed")
                startActivity(FeedActivity.createIntent(mActivity, user))
            }
            R.id.btnAcceptAll,
            R.id.btnDecline -> {
                val acceptListener: (JSONObject) -> Unit = acceptListener@ { response ->
                    val msg = if(response.optInt("gcode") == ErrorControl.ERROR_88888){
                        response.optString("msg")
                    }else{
                        getString(R.string.desc_accepted_friend_request)
                    }
                    Util.closeProgress()
                    Util.showDefaultIdolDialogWithBtn1(
                        mActivity,
                        null,
                        msg
                    ) {
                        Util.closeIdolDialog()
                    }

                    if(response.optInt("gcode") == ErrorControl.ERROR_88888){
                        return@acceptListener
                    }
                    mActivity.setResult(ResultCode.FRIEND_REQUESTED.value)
                    mFriends.clear()
                    mAdapter.notifyDataSetChanged()
                    showEmpty()
                }
                val declineListener: (JSONObject) -> Unit = { response ->
                    mFriends.removeAt(position)
                    mAdapter.notifyDataSetChanged()
                    mActivity.setResult(ResultCode.FRIEND_REQUEST_CANCELED.value)
                    if (mFriends.size == 0) {
                        showEmpty()
                    }
                    Util.closeProgress()
                    Util.showDefaultIdolDialogWithBtn1(mActivity,
                            null,
                            getString(R.string.desc_declined_friend_request)
                    ) {
                        Util.closeIdolDialog()
                    }
                }
                val errorListener: (Throwable) -> Unit = { throwable ->
                    Util.closeProgress()
                    if (!TextUtils.isEmpty(throwable.message)) {
                        Util.showDefaultIdolDialogWithBtn1(mActivity,
                                null,
                            throwable.message
                        ) { Util.closeIdolDialog() }
                    } else {
                        Util.showDefaultIdolDialogWithBtn1(mActivity,
                                null,
                                "${getString(R.string.msg_error_ok)}"
                        ) { Util.closeIdolDialog() }
                    }
                }

                if (view.id == R.id.btnAcceptAll) {
                    Util.showDefaultIdolDialogWithBtn2(mActivity,
                        getString(R.string.title_accept_friend_request),
                        getString(R.string.desc_accept_friend_request),
                        R.string.yes, R.string.no,
                        true,false,
                            {
                                Util.showProgress(mActivity)
                                MainScope().launch {
                                    friendsRepository.respondAllFriendRequest(
                                        acceptListener,
                                        errorListener
                                    )
                                }
                                Util.closeIdolDialog()
                            },
                            { Util.closeIdolDialog() })
                } else if (view.id == R.id.btnDecline) {
                    Util.showDefaultIdolDialogWithBtn2(mActivity,
                            getString(R.string.title_decline_friend_request),
                            getString(R.string.desc_decline_friend_request),
                            {
                                Util.showProgress(mActivity)
                                MainScope().launch {
                                    friendsRepository.respondFriendRequest(
                                        user.id.toLong(),
                                        false,
                                        declineListener,
                                        errorListener
                                    )
                                }
                                Util.closeIdolDialog()
                            },
                            { Util.closeIdolDialog() })
                }
            }
        }
    }

    private fun hideEmpty() {
        binding.emptyFriendRequest.visibility = View.GONE
        binding.acceptFriendsRequestList.visibility = View.VISIBLE
    }

    private fun showEmpty() {
        binding.emptyFriendRequest.visibility = View.VISIBLE
        binding.acceptFriendsRequestList.visibility = View.GONE
    }
}