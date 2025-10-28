package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import net.ib.mn.utils.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.databinding.ActivityFriendsRequestBinding
import net.ib.mn.fragment.AcceptFriendsRequestFragment
import net.ib.mn.fragment.CancelFriendsRequestFragment
import net.ib.mn.model.FriendModel
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ExtendedDataHolder.Companion.getInstance
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

@AndroidEntryPoint
class FriendsRequestActivity : BaseActivity() {

    companion object {
        private const val PARAM_TAB_NUM = "paramTabNum"

        fun createIntent(context: Context, tabNum: Int): Intent {
            val intent = Intent(context, FriendsRequestActivity::class.java)
            intent.putExtra(PARAM_TAB_NUM, tabNum)
            return intent
        }
    }

    private val pagerAdapter: FriendsRequestPagerAdapter by lazy {
        FriendsRequestPagerAdapter(2, supportFragmentManager)
    }
    private var mAccount: IdolAccount? = null
    val friends: ArrayList<FriendModel> = ArrayList()
    private lateinit var binding: ActivityFriendsRequestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_friends_request)
        binding.llContainer.applySystemBarInsets()

        supportActionBar?.setTitle(R.string.friends_request)
        mAccount = IdolAccount.getAccount(this)

        val extras = getInstance()
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

        binding.pager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 2
            currentItem = intent.getIntExtra(PARAM_TAB_NUM, 0)
        }
        binding.tabs.apply {
            setBackgroundResource(R.drawable.btn_down_tab)
            setSelectedTabIndicatorColor(resources.getColor(R.color.main))
            setSelectedTabIndicatorHeight(Util.convertDpToPixel(context, 3f).toInt())
            setTabTextColors(
                ContextCompat.getColor(context, R.color.gray150),
                ContextCompat.getColor(context, R.color.main)
            )
        }
        binding.tabs.setupWithViewPager(binding.pager)
    }

    private fun parseResponse(response: JSONObject) {
        if (response.optBoolean("success")) {
            val array: JSONArray
            try {
                friends.clear()
                array = response.getJSONArray("objects")
                val gson = IdolGson.getInstance()
                for (i in 0 until array.length()) {
                    val model: FriendModel = gson.fromJson(array.getJSONObject(i).toString(),
                            FriendModel::class.java)
                    if (model.isFriend == "N") {
                        friends.add(model)
                    }
                }
                if (friends.size > 0) { // 가나다순 정렬 -> 접속일 오름차순
                    friends.sortWith(Comparator { lhs: FriendModel, rhs: FriendModel ->
                        lhs.user.lastAct?.compareTo(rhs.user.lastAct) ?: 0 })
                    Util.closeProgress()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            Util.closeProgress()
            if (!isFinishing) {
                UtilK.handleCommonError(this@FriendsRequestActivity, response)
            }
        }
    }

    inner class FriendsRequestPagerAdapter(private var fragNum: Int, fm: FragmentManager) : FragmentPagerAdapter(fm) {

        private val tabTitles = listOf(
                R.string.title_accept_friend_request,
                R.string.cancel_friend_request)
        private val acceptFriendRequestFrag: Fragment = AcceptFriendsRequestFragment()
        private val cancelFriendRequestFrag: Fragment = CancelFriendsRequestFragment()

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> acceptFriendRequestFrag
                1 -> cancelFriendRequestFrag
                else -> acceptFriendRequestFrag
            }
        }

        override fun getCount(): Int = fragNum

        override fun getPageTitle(position: Int): CharSequence? {
            return resources.getString(tabTitles[position])
        }

    }
}