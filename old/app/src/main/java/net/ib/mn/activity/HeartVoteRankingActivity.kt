package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
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
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.activity.FeedActivity.Companion.createIntent
import net.ib.mn.adapter.VoteRankingAdater
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ActivityHeartVoteRankingBinding
import net.ib.mn.model.IdolModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Toast
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject

/**
 * 커뮤 - 투표 탑 100
 */

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class HeartVoteRankingActivity : BaseActivity() {
    private var mIdolModel: IdolModel? = null
    private var mAdapter: VoteRankingAdater? = null

    private lateinit var binding: ActivityHeartVoteRankingBinding
    @Inject
    lateinit var usersRepository: UsersRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHeartVoteRankingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.llContainer.applySystemBarInsets()

        mIdolModel = intent.getSerializableExtra("idol") as IdolModel?

        mIdolModel?.let {
            setCommunityTitle(
                it,
                getString(R.string.title_heart_vote_ranking__format).split("%s".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            )
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(false)

        mAdapter = VoteRankingAdater(this, Glide.with(this))

        binding.list.onItemClickListener =
            AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION, "community_feed_top30"
                )
                startActivity(createIntent(this, mAdapter!!.getItem(position)))
            }

        binding.list.setAdapter(mAdapter)
        binding.loadingView.setVisibility(View.VISIBLE)
        loadList()
    }

    private fun loadList() {
        lifecycleScope.launch {
            usersRepository.getRankedUser(
                idolId = mIdolModel!!.getId(),
                listener = { response ->
                    mAdapter!!.clear()
                    val ranks = response.optJSONObject("ranks")
                    if (ranks != null) {
                        val gson = IdolGson.instance
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
                        getAccount(this@HeartVoteRankingActivity)?.let { account ->
                            val myRank = response.optString("my_rank")
                            if (myRank != null && (myRank != "null") && !TextUtils.isEmpty(myRank)) {
                                mAdapter!!.addMyRank(myRank, account)
                            } else if (account.most != null
                                && account.most!!.getId() == mIdolModel!!.getId()
                            ) {
                                mAdapter!!.addMyRank("", account)
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
                },
                errorListener = { throwable ->
                    val msg = throwable.message
                    var errorText = getString(R.string.failed_to_get_vote_ranking)
                    if (!TextUtils.isEmpty(msg)) {
                        errorText += msg
                    }
                    Toast.makeText(
                        this@HeartVoteRankingActivity, errorText,
                        Toast.LENGTH_SHORT
                    ).show()
                    if (mAdapter!!.getCount() > 0) {
                        binding.list.setVisibility(View.VISIBLE)
                        binding.emptyView.setVisibility(View.GONE)
                        mAdapter!!.notifyDataSetChanged()
                    } else {
                        binding.list.setVisibility(View.GONE)
                        binding.emptyView.setVisibility(View.VISIBLE)
                    }
                    binding.loadingView.setVisibility(View.GONE)

                }
            )
        }
    }

    companion object {
        fun createIntent(context: Context?, idol: IdolModel?): Intent {
            val intent = Intent(context, HeartVoteRankingActivity::class.java)
            intent.putExtra("idol", idol as Parcelable?)
            return intent
        }
    }
}
