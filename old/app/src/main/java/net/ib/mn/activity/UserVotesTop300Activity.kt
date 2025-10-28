package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.FeedActivity.Companion.createIntent
import net.ib.mn.adapter.UserVotesTop300Adapter
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ActivityHeartVoteRankingBinding
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Toast
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class UserVotesTop300Activity : BaseActivity() {
    private var mAdapter: UserVotesTop300Adapter? = null

    private lateinit var binding: ActivityHeartVoteRankingBinding
    @Inject
    lateinit var usersRepository: UsersRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHeartVoteRankingBinding.inflate(layoutInflater)
        binding.llContainer.applySystemBarInsets()
        setContentView(binding.root)

        supportActionBar?.setTitle(R.string.stats_votes_top300)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(false)

        mAdapter = UserVotesTop300Adapter(this, Glide.with(this))

        binding.list.onItemClickListener =
            AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "accumulated_vote_top_300_feed"
                )
                startActivity(createIntent(this, mAdapter!!.getItem(position)))
            }

        binding.list.setAdapter(mAdapter)
        binding.loadingView.visibility = View.VISIBLE
        loadList()
    }

    private fun loadList() {
        lifecycleScope.launch {
            usersRepository.getTopRanker(
                { response ->
                    mAdapter!!.clear()
                    val ranks = response.optJSONObject("ranks")
                    if (ranks != null) {
                        val gson = instance
                        val listType = object : TypeToken<MutableList<UserModel?>?>() {
                        }.type
                        val rankers = gson.fromJson<MutableList<UserModel>>(
                            ranks.optJSONArray("objects")?.toString(), listType
                        )
                        for (user in rankers) {
                            if (user.levelHeart > 0 /* && mAdapter.getCount() < 10 */) {
                                mAdapter!!.add(user)
                            }
                        }
                    }
                    if (mAdapter!!.count > 0) {
                        binding.list.visibility = View.VISIBLE
                        binding.emptyView.visibility = View.GONE
                        mAdapter!!.notifyDataSetChanged()
                    } else {
                        binding.list.visibility = View.GONE
                        binding.emptyView.visibility = View.VISIBLE
                    }
                    binding.loadingView.visibility = View.GONE
                }, { throwable ->
                    val msg = throwable.message
                    var errorText = getString(R.string.failed_to_get_vote_ranking)
                    if (!TextUtils.isEmpty(msg)) {
                        errorText += msg
                    }
                    Toast.makeText(
                        this@UserVotesTop300Activity, errorText,
                        Toast.LENGTH_SHORT
                    ).show()
                    if (mAdapter!!.count > 0) {
                        binding.list.visibility = View.VISIBLE
                        binding.emptyView.visibility = View.GONE
                        mAdapter!!.notifyDataSetChanged()
                    } else {
                        binding.list.visibility = View.GONE
                        binding.emptyView.visibility = View.VISIBLE
                    }
                    binding.loadingView.visibility = View.GONE
                }
            )
        }
    }

    companion object {
        fun createIntent(context: Context?): Intent {
            return Intent(context, UserVotesTop300Activity::class.java)
        }
    }
}
