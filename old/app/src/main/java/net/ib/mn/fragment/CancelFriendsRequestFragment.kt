package net.ib.mn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.FriendsRequestActivity
import net.ib.mn.adapter.CancelFriendsRequestAdapter
import net.ib.mn.core.data.repository.friends.FriendsRepositoryImpl
import net.ib.mn.databinding.FragmentCancelFriendsRequestBinding
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import javax.inject.Inject

@AndroidEntryPoint
class CancelFriendsRequestFragment : BaseFragment(),
        CancelFriendsRequestAdapter.OnClickListener {

    private lateinit var mActivity: FriendsRequestActivity
    private var mFriends = ArrayList<FriendModel>()
    private lateinit var mAdapter: CancelFriendsRequestAdapter
    private lateinit var binding: FragmentCancelFriendsRequestBinding
    @Inject
    lateinit var friendsRepository: FriendsRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mGlideRequestManager = Glide.with(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCancelFriendsRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity = activity as FriendsRequestActivity
        mFriends.addAll(mActivity.friends.filter {
            it.userType == FriendModel.RECV_USER
        })
        mAdapter = CancelFriendsRequestAdapter(mActivity,
                mGlideRequestManager,
                mFriends,
                this@CancelFriendsRequestFragment
        )
        binding.cancelFriendsRequestList.adapter = mAdapter
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
            R.id.photo,
            R.id.info -> {
                setUiActionFirebaseGoogleAnalyticsFragment(Const.ANALYTICS_BUTTON_PRESS_ACTION,
                        "friends_feed")
                startActivity(FeedActivity.createIntent(mActivity, user))
            }
            R.id.btnCancel -> {
                Util.showProgress(mActivity)
                MainScope().launch {
                    friendsRepository.cancelFriendRequest(
                        user.id,
                        {
                            try {
                                mFriends.removeAt(position)
                            }catch (e : IndexOutOfBoundsException){
                                e.printStackTrace()
                            }
                            mAdapter.notifyDataSetChanged()
                            mActivity.setResult(ResultCode.FRIEND_REQUEST_CANCELED.value)
                            Util.closeProgress()
                        },
                        { throwable ->
                            Util.closeProgress()
                            if (isAdded) {
                                Toast.makeText(mActivity,
                                    throwable.message,
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun hideEmpty() {
        binding.emptyFriendRequest.visibility = View.GONE
        binding.cancelFriendsRequestList.visibility = View.VISIBLE
    }

    private fun showEmpty() {
        binding.emptyFriendRequest.visibility = View.VISIBLE
        binding.cancelFriendsRequestList.visibility = View.GONE
    }
}