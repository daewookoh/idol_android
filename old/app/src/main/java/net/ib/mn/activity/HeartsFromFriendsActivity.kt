package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.adapter.HeartsFromFriendsAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ActivityHeartsFromFriendsBinding
import net.ib.mn.feature.friend.FriendsActivity
import net.ib.mn.model.ConfigModel.Companion.getInstance
import net.ib.mn.model.HeartsFriendsModel
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import java.text.NumberFormat
import javax.inject.Inject

@AndroidEntryPoint
class HeartsFromFriendsActivity : BaseActivity() {
    private var mAdapter: HeartsFromFriendsAdapter? = null
    private lateinit var binding: ActivityHeartsFromFriendsBinding
    @Inject
    lateinit var usersRepository: UsersRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = getIntent()
        val bundle = intent.extras

        val title = bundle!!.getString("title")

        binding = ActivityHeartsFromFriendsBinding.inflate(layoutInflater)
        binding.llContainer.applySystemBarInsets()
        setContentView(binding.root)

        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(false)

        binding.friend.text = title
        mAdapter = HeartsFromFriendsAdapter(this)

        binding.list.setAdapter(mAdapter)
        binding.loadingView.visibility = View.VISIBLE
        loadList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_get_heart_from_friend, menu)
        val menuItem = menu.findItem(R.id.action_add_friend)
        val actionView = menuItem.actionView
        actionView!!.setOnClickListener(View.OnClickListener { v: View? ->
            if ("Y".equals(
                    getInstance(baseContext).friendApiBlock, ignoreCase = true
                )
            ) {
                Util.showDefaultIdolDialogWithBtn1(
                    baseContext, null, getString(R.string.friend_api_block),
                    View.OnClickListener { v1: View? -> Util.closeIdolDialog() })
            } else if ("L".equals(
                    getInstance(baseContext).friendApiBlock,
                    ignoreCase = true
                )
            ) {
                Util.showDefaultIdolDialogWithBtn1(
                    baseContext, null, getString(R.string.friend_api_limit),
                    View.OnClickListener { v12: View? -> Util.closeIdolDialog() })
            } else {
                startActivity(FriendsActivity.createIntent(baseContext))
            }
        })
        return true
    }

    private fun loadList() {
        lifecycleScope.launch {
            usersRepository.getFriendHeartLog(
                { response ->
                    mAdapter!!.clear()

                    if (!response.optBoolean("success")) {
                        val responseMsg =
                            ErrorControl.parseError(this@HeartsFromFriendsActivity, response)
                        if (responseMsg != null) {
                            Toast.makeText(
                                this@HeartsFromFriendsActivity,
                                responseMsg,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@getFriendHeartLog
                    }

                    val friend = response.optJSONArray("earn")
                    val totalEarn = response.optInt("total_earn", 0)
                    val totalEarnComma =
                        NumberFormat.getNumberInstance(getAppLocale(this@HeartsFromFriendsActivity))
                            .format(totalEarn.toLong())
                    binding.hearts.text = totalEarnComma

                    if (friend != null) {
                        val gson = IdolGson.instance
                        val listType =
                            object : TypeToken<MutableList<HeartsFriendsModel?>?>() {}.type
                        val friends =
                            gson.fromJson<MutableList<HeartsFriendsModel?>>(friend.toString(), listType)
                        for (user in friends) {
                            mAdapter!!.add(user)
                        }
                    }
                    if (mAdapter!!.count > 0) {
                        binding.list.visibility = View.VISIBLE
                        mAdapter!!.notifyDataSetChanged()
                    } else {
                        binding.list.visibility = View.GONE
                    }
                    binding.loadingView.visibility = View.GONE
                }, { throwable ->
                    val msg = throwable.message
                    var errorText = getString(R.string.failed_to_load)
                    if (!TextUtils.isEmpty(msg)) {
                        errorText += msg
                    }
                    Toast.makeText(
                        this@HeartsFromFriendsActivity, errorText,
                        Toast.LENGTH_SHORT
                    ).show()
                    if (mAdapter!!.count > 0) {
                        binding.list.visibility = View.VISIBLE
                        mAdapter!!.notifyDataSetChanged()
                    } else {
                        binding.list.visibility = View.GONE
                    }
                    binding.loadingView.visibility = View.GONE
                }
            )
        }
    }

    companion object {
        fun createIntent(context: Context?): Intent {
            return Intent(context, HeartsFromFriendsActivity::class.java)
        }
    }
}
