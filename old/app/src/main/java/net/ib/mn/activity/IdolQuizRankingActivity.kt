package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import androidx.annotation.OptIn
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.activity.FeedActivity.Companion.createIntent
import net.ib.mn.adapter.QuizRankingAdapter
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.core.data.repository.QuizRepositoryImpl
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.ActivityQuizRankingBinding
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.model.IdolModel
import net.ib.mn.model.QuizCategoryModel
import net.ib.mn.model.QuizRankModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.UtilK.Companion.getPrefIdolList
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections
import java.util.Objects
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class IdolQuizRankingActivity : BaseActivity(), View.OnClickListener {
    private var modelList = ArrayList<IdolModel>()

    private var currentTabIdx = 0
    private var idolId = 0
    private var account: IdolAccount? = null

    private var mAdapter: QuizRankingAdapter? = null
    private var mCurrentTabBtn: Button? = null

    var typeList: ArrayList<QuizCategoryModel> = ArrayList()
    private var mType: String? = null

    private lateinit var binding: ActivityQuizRankingBinding

    @Inject
    lateinit var quizRepository: QuizRepositoryImpl
    @Inject
    lateinit var idolsRepository: IdolsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityQuizRankingBinding.inflate(layoutInflater)
        binding.clContainer.applySystemBarInsets()
        setContentView(binding.root)

        val idolSelectLayout = findViewById<LinearLayoutCompat>(R.id.select_idol)
        
        initializeData()

        val actionbar = supportActionBar
        actionbar?.setTitle(R.string.stats_quiz)

        showEmptyView()

        idolId = 0
        currentTabIdx = 0
        
        // 초기 텍스트 설정
        binding.tvIdol.text = "ALL"

        idolSelectLayout.setOnClickListener(this)
        binding.tabbtnToday.setOnClickListener(this)
        binding.tabbtnYesterday.setOnClickListener(this)
        binding.tabbtnAlltime.setOnClickListener(this)

        mAdapter = QuizRankingAdapter(this, Glide.with(this))

        binding.list.setOnItemClickListener(AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            setUiActionFirebaseGoogleAnalyticsActivity(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "quiz_ranking_top_100_feed"
            )
            startActivity(createIntent(this, mAdapter!!.getItem(position).user))
        })
        binding.list.setAdapter(mAdapter)

        selectTab(binding.tabbtnToday, 0)
    }

    private fun initializeData() {
        if (BuildConfig.CELEB) {
            typeList = UtilK.getQuizTypeList(this)
            if (typeList.isEmpty()) {
                getQuizTypeList(this)
            }
        } else {
            modelList = getPrefIdolList(this, Const.PREF_QUIZ_IDOL_LIST)
            if (modelList.isEmpty()) {
                getIdolGroupList(this)
            }
        }
    }

    private fun showBottomSheet() {
        val items = if (BuildConfig.CELEB) {
            arrayListOf<String>().apply { addAll(typeList.map { it.name }) }
        } else {
            arrayListOf("ALL").apply { addAll(modelList.map { it.getName(this@IdolQuizRankingActivity) }) }
        }

        val bottomSheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_QUIZ_RANKING_FILTER)
        val args = Bundle()
        args.putStringArrayList("items", items)
        bottomSheet.arguments = args
        bottomSheet.show(supportFragmentManager, "quiz_ranking_filter")
    }

    private fun showEmptyView() {
        binding.emptyView.visibility = View.VISIBLE
        binding.quizRank.visibility = View.GONE
    }

    private fun hideEmptyView() {
        binding.emptyView.visibility = View.GONE
        binding.quizRank.visibility = View.VISIBLE
    }

    private fun loadList(index: Int, idolId: Int, type: String?) {
        val listener: (JSONObject) -> Unit = { response ->
            mAdapter!!.clear()
            if (response.optBoolean("success")) {
                val gson = instance
                val listType = object : TypeToken<List<QuizRankModel?>?>() {
                }.type
                val items = gson.fromJson<List<QuizRankModel>>(
                    response.optJSONArray("objects").toString(), listType
                )
                val temp: List<QuizRankModel> = ArrayList(items)
                for (i in temp.indices) {
                    val item = temp[i]
                    // 동점자 처리
                    if (i > 0 && temp[i - 1].power == item.power) {
                        item.rank = temp[i - 1].rank
                    } else {
                        item.rank = i
                    }
                }
                Collections.sort(items) { lhs: QuizRankModel, rhs: QuizRankModel ->
                    if (lhs.rank == rhs.rank) {
                        return@sort lhs.user?.nickname?.compareTo(
                            rhs.user?.nickname ?: ""
                        ) ?: 0
                    } else {
                        return@sort lhs.rank - rhs.rank
                    }
                }
                for (user in items) {
                    mAdapter!!.add(user)
                }
            }

            dataChange()
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            val errorText = getString(R.string.error_abnormal_default)
            makeText(this@IdolQuizRankingActivity, errorText, Toast.LENGTH_SHORT).show()
            dataChange()
        }

        MainScope().launch {
            quizRepository.getQuizRanking(index, idolId, type, listener, errorListener)
        }
    }

    private fun dataChange() {
        if (mAdapter!!.count > 0) {
            binding.preparingConstraint.visibility = View.GONE
            binding.list.visibility = View.VISIBLE
            mAdapter!!.notifyDataSetChanged()
        } else {
            binding.preparingConstraint.visibility = View.VISIBLE
            binding.list.visibility = View.GONE
        }
        hideEmptyView()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.select_idol -> showBottomSheet()

            R.id.tabbtn_today -> if (currentTabIdx != 0) {
                selectTab(v, 0)
            }

            R.id.tabbtn_yesterday -> if (currentTabIdx != 1) {
                selectTab(v, 1)
            }

            R.id.tabbtn_alltime -> if (currentTabIdx != 2) {
                selectTab(v, 2)
            }
        }
    }

    fun onCategorySelected(position: Int) {
        if (position == 0) {
            selectAll()
        } else {
            selectItem(position)
        }
        loadList(currentTabIdx, idolId, mType)
    }

    private fun selectAll() {
        binding.tvIdol.text = "ALL"
        idolId = 0
        mType = null
    }

    private fun selectItem(index: Int) {
        if (BuildConfig.CELEB) {
            if (index < 0 || index >= typeList.size) return
            val selected = typeList[index]
            binding.tvIdol.text = selected.name
            mType = selected.type
        } else {
            if (index - 1 < 0 || index - 1 >= modelList.size) return
            val selected = modelList[index - 1]
            binding.tvIdol.text = selected.getName(this)
            idolId = selected.groupId
            mType = null
        }
    }

    private fun selectTab(v: View?, tabIdx: Int) {
        if (mCurrentTabBtn != null) mCurrentTabBtn!!.isSelected = false
        mCurrentTabBtn = v as Button?
        v!!.isSelected = true

        when (tabIdx) {
            0 -> {
                binding.tabbtnToday.setTextColor(ContextCompat.getColor(this, R.color.main))
                binding.tabbtnYesterday.setTextColor(ContextCompat.getColor(this, R.color.text_dimmed))
                binding.tabbtnAlltime.setTextColor(ContextCompat.getColor(this, R.color.text_dimmed))
            }

            1 -> {
                binding.tabbtnToday.setTextColor(ContextCompat.getColor(this, R.color.text_dimmed))
                binding.tabbtnYesterday.setTextColor(ContextCompat.getColor(this, R.color.main))
                binding.tabbtnAlltime.setTextColor(ContextCompat.getColor(this, R.color.text_dimmed))
            }

            2 -> {
                binding.tabbtnToday.setTextColor(ContextCompat.getColor(this, R.color.text_dimmed))
                binding.tabbtnYesterday.setTextColor(ContextCompat.getColor(this, R.color.text_dimmed))
                binding.tabbtnAlltime.setTextColor(ContextCompat.getColor(this, R.color.main))
            }
        }
        currentTabIdx = tabIdx
        loadList(currentTabIdx, idolId, mType)
    }

    fun getIdolGroupList(context: Context?) {
        val gson = instance
        account = getAccount(context)

        lifecycleScope.launch {
            idolsRepository.getGroupsForQuiz(
                { response ->
                    if (response.optBoolean("success")) {
                        try {
                            val array = response.getJSONArray("objects")
                            var most: IdolModel? = null
                            for (i in 0 until array.length()) {
                                val model = gson.fromJson(
                                    array.getJSONObject(i).toString(),
                                    IdolModel::class.java
                                )
                                //                            model.setLocalizedName(context);
                                if (account!!.most == null
                                    || model.groupId != account!!.most!!.groupId
                                ) {
                                    modelList.add(model)
                                } else {
                                    most = model
                                }
                            }

                            modelList.sortWith { lhs: IdolModel, rhs: IdolModel ->
                                lhs.getName(context).compareTo(rhs.getName(context))
                            }
                            if (account!!.most != null
                                && !account!!.most!!.type.equals("B", ignoreCase = true)
                            ) {
                                //제외된 친구들은 좀 빠져라,,,
                                if (most != null) modelList.add(0, most)
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        UtilK.handleCommonError(context, response)
                    }
                }, { }
            )
        }
    }

    private fun getQuizTypeList(context: Context) {
        MainScope().launch {
            quizRepository.getQuizTypeList(
                lambda@ { response ->
                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response)
                        makeText(context, responseMsg, Toast.LENGTH_SHORT).show()
                        finish()
                        return@lambda
                    }

                    val quizTypeArray =
                        Objects.requireNonNull(response.optJSONArray("objects")).toString()

                    val listType =
                        object : com.google.common.reflect.TypeToken<List<QuizCategoryModel?>?>() {
                        }.type
                    typeList = instance.fromJson(quizTypeArray, listType)

                    Util.setPreference(
                        context,
                        Const.PREF_QUIZ_TYPE_LIST,
                        instance.toJson(typeList)
                    )
                },
                { throwable ->
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Util.removePreference(this, Const.PREF_QUIZ_IDOL_LIST)
    }

    companion object {
        private const val PARAM_IDOL_OPTION = "idol_option"

        fun createIntent(context: Context?): Intent {
            return Intent(context, IdolQuizRankingActivity::class.java)
        }
    }
}
