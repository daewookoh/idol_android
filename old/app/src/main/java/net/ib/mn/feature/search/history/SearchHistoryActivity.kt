package net.ib.mn.feature.search.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.SearchResultActivity
import net.ib.mn.adapter.SearchHistoryAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.databinding.ActivitySearchHistoryBinding
import net.ib.mn.feature.common.InAppBanner
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.model.SearchHistoryModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.KeyboardVisibilityUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.setUiActionFirebaseGoogleAnalyticsWithKey
import org.json.JSONArray

@AndroidEntryPoint
class SearchHistoryActivity : BaseActivity(),
    SearchHistoryAdapter.OnClickListener {
    private lateinit var mSearchHistoryAdapter: SearchHistoryAdapter
    private var searchList = ArrayList<SearchHistoryModel>()
    private var trendJson = JSONArray()
    private var suggestJson = JSONArray()
    private var resultBackClick = 1

    private lateinit var binding: ActivitySearchHistoryBinding
    private val viewModel: SearchHistoryViewModel by viewModels()

    //5 = TYPE_TREND, 6 = TYPE_HISTORY, 7 = TYPE_AUTO
    private var searchType = SearchHistoryAdapter.TYPE_TREND

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_search_history)
        binding.clSearchHistory.applySystemBarInsets()

        viewModel.init(this)

        if (BuildConfig.CELEB) {
            binding.tvSearchTitle.visibility = View.GONE
        }
        setToolBar()
        getHistory()
        onClickListener()
        observedVM()

        resizeSearchList(searchList)
    }

    override fun onResume() {
        super.onResume()
        //키보드 show 여부 감지
        KeyboardVisibilityUtil(window, onShowKeyboard = {
            if (searchType != SearchHistoryAdapter.TYPE_HISTORY && binding.searchToolbar.searchInput.text.isNullOrEmpty()) {
                searchHistory()
            } else if (searchType != SearchHistoryAdapter.TYPE_AUTO && !binding.searchToolbar.searchInput.text.isNullOrEmpty()) {
                viewModel.searchSuggest(binding.searchToolbar.searchInput.text.toString())
            }
        }, onHideKeyboard = {})
        //검색 결과 창에서 뒤로가기 버튼을 눌렀을 경우
        if (SearchResultActivity.searchResultFinish == resultBackClick) {
            getHistory()
            binding.searchToolbar.searchInput.text?.clear()

            viewModel.searchTrends()
        }
    }

    override fun onItemClicked(position: Int, checkItem: Int) {
        when (checkItem) {
            //최근 검색 삭제
            SearchHistoryAdapter.SEARCH_X -> {
                searchList.removeAt(position)
                Util.setPreferenceArray(this, Const.SEARCH_HISTORY, searchList)
                mSearchHistoryAdapter.notifyDataSetChanged()
            }
            //최근 검색 클릭
            SearchHistoryAdapter.MY_SEARCH -> {
                hideSoftKeyboard()
                val searchClickTxt = searchList[position].search
                searchList.removeAt(position)
                searchList.add(SearchHistoryModel(searchClickTxt))
                Util.setPreferenceArray(this, Const.SEARCH_HISTORY, searchList)
                startActivity(
                    SearchResultActivity.createIntent(
                        this,
                        searchList.get(searchList.size - 1).search.toString()
                    )
                )

            }
            //핫 트렌드 클릭
            SearchHistoryAdapter.CL_HOT_TREND -> {
                Util.setPreferenceArray(this, Const.SEARCH_HISTORY, searchList)
                startActivity(
                    SearchResultActivity.createIntent(
                        this,
                        trendJson.getJSONObject(position).getString("text")
                    )
                )
            }
            //자동완성 클릭
            SearchHistoryAdapter.CL_AUTO_COMPLETE -> {
                hideSoftKeyboard()
                Util.setPreferenceArray(this, Const.SEARCH_HISTORY, searchList)
                startActivity(
                    SearchResultActivity.createIntent(
                        this,
                        suggestJson.getJSONObject(position).getString("text")
                    )
                )
            }
        }
    }

    private fun setToolBar() {
        binding.searchToolbar.root.visibility = View.VISIBLE
        binding.searchToolbar.flBack.setOnClickListener {
            finish()
        }
    }

    //Shared에 저장된 검색기록 가져오기.
    private fun getHistory() {
        val gson = IdolGson.getInstance()
        val listType = object : TypeToken<List<SearchHistoryModel?>?>() {}.type
        //null인 경우 에러가 남
        if (!Util.getPreference(this, Const.SEARCH_HISTORY).isEmpty()) {
            searchList =
                gson.fromJson(Util.getPreference(this, Const.SEARCH_HISTORY).toString(), listType)
        }
    }

    //클릭 리스너 모음
    private fun onClickListener() = with(binding.searchToolbar) {
        root.setOnClickListener {
            showSoftKeyboard()
        }

        searchClose.setOnClickListener {
            if (searchType != SearchHistoryAdapter.TYPE_TREND) {
                hideSoftKeyboard()
                searchInput.text = null
                viewModel.searchTrends()
            } else {
                finish()
            }
        }
        searchInput.setOnClickListener {
            showSoftKeyboard()
        }
        //키패드 돋보기(확인) 클릭
        searchInput.setOnEditorActionListener(TextView.OnEditorActionListener { textView, actionId, keyEvent ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    if (searchList.contains(SearchHistoryModel(searchInput.text.toString()))) {
                        searchList.remove(SearchHistoryModel(searchInput.text.toString()))
                    }
                    searchList.add(SearchHistoryModel(searchInput.text.toString()))
                    resizeSearchList(searchList)
                    hideSoftKeyboard()
                    startActivity(
                        SearchResultActivity.createIntent(
                            this@SearchHistoryActivity,
                            searchInput.text.toString()
                        )
                    )
                }

                else ->
                    // 기본 엔터키 동작
                    return@OnEditorActionListener false
            }
            true
        })

        //editText 변할 때 불러줌
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (searchInput.text.isNullOrEmpty() && searchInput.isCursorVisible) {
                    searchHistory()
                } else if (!searchInput.text.isNullOrEmpty() && searchInput.isCursorVisible) {
                    viewModel.searchSuggest(searchInput.text.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        //돋보기 버튼 눌렀을 때 Shared 저장 및 이동
        searchBtn.setOnClickListener {
            if (!searchInput.text.isNullOrEmpty()) {
                if (searchList.contains(SearchHistoryModel(searchInput.text.toString()))) {
                    searchList.remove(SearchHistoryModel(searchInput.text.toString()))
                }
                searchList.add(SearchHistoryModel(searchInput.text.toString()))
                resizeSearchList(searchList)
                hideSoftKeyboard()
                startActivity(
                    SearchResultActivity.createIntent(
                        this@SearchHistoryActivity,
                        searchInput.text.toString()
                    )
                )
            }
        }
    }

    //검색 히스토리 사이즈 조정 해줌.
    private fun resizeSearchList(searchList: ArrayList<SearchHistoryModel>) {
        if (searchList.size > Const.MAXIMUM_SEARCH_HISTORY_LIST) {//10 초과일때는 마지막 포지션 삭제해줌.
            searchList.removeAt(0)
            Util.setPreferenceArray(this, Const.SEARCH_HISTORY, searchList)
        } else {//10개 미만의 경우에는  저장해줌.
            Util.setPreferenceArray(this, Const.SEARCH_HISTORY, searchList)
        }
    }

    private fun showSoftKeyboard() = with(binding.searchToolbar) {
        searchInput.requestFocus()
        searchInput.isCursorVisible = true
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideSoftKeyboard() = with(binding.searchToolbar) {
        searchInput.isCursorVisible = false
        val imm = getSystemService(
            INPUT_METHOD_SERVICE
        ) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }

    private fun observedVM() = with(viewModel) {
        bannerList.observe(this@SearchHistoryActivity, SingleEventObserver { bannerList ->
            if (bannerList.isEmpty()) {
                binding.cvInAppBanner.visibility = View.GONE
                return@SingleEventObserver
            }

            binding.cvInAppBanner.apply {
                setContent {
                    MaterialTheme {
                        InAppBanner(
                            bannerList = bannerList,
                            clickBanner = { inAppBanner ->
                                setUiActionFirebaseGoogleAnalyticsWithKey(
                                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                                    "menu_banner",
                                    "id",
                                    inAppBanner.id,
                                )

                                UtilK.sendAnalyticsAdEvent(
                                    context,
                                    "ad_click_exodus",
                                    inAppBanner.id
                                )

                                viewModel.clickBanner(this@SearchHistoryActivity, inAppBanner)
                            }
                        )
                    }
                }
                visibility = View.VISIBLE
            }
        })

        navigateCommunity.observe(this@SearchHistoryActivity, SingleEventObserver { idol ->
            val intent = CommunityActivity.createIntent(
                this@SearchHistoryActivity, idol
            )
            startActivity(intent)
        })

        navigateAppLink.observe(this@SearchHistoryActivity, SingleEventObserver { bannerLink ->
            val browserIntent = Intent(this@SearchHistoryActivity, AppLinkActivity::class.java)
            browserIntent.data = Uri.parse(bannerLink)
            startActivity(browserIntent)
        })

        searchTrends.observe(this@SearchHistoryActivity, SingleEventObserver {
            trendJson = it

            searchType = SearchHistoryAdapter.TYPE_TREND
            binding.tvSearchTitle.visibility = View.GONE
            val currentLayout =
                binding.rvSearch.layoutParams as ConstraintLayout.LayoutParams
            currentLayout.topToBottom = R.id.tv_hot_title
            binding.cvInAppBanner.visibility = View.VISIBLE
            binding.tvHotTitle.apply {
                visibility = View.VISIBLE
                text = getString(R.string.search_hot_trend)
            }
            mSearchHistoryAdapter = SearchHistoryAdapter(
                this@SearchHistoryActivity,
                searchType,
                this@SearchHistoryActivity,
                null,
                trendJson
            )
            binding.rvSearch.adapter = mSearchHistoryAdapter
        })

        searchSuggests.observe(this@SearchHistoryActivity, SingleEventObserver {
            searchType = SearchHistoryAdapter.TYPE_AUTO
            binding.tvSearchTitle.text = getString(R.string.search_recommended)
            suggestJson = it
            mSearchHistoryAdapter = SearchHistoryAdapter(
                this@SearchHistoryActivity,
                searchType,
                this@SearchHistoryActivity,
                null,
                suggestJson
            )
            binding.rvSearch.adapter = mSearchHistoryAdapter
        })

        errorToast.observe(this@SearchHistoryActivity, SingleEventObserver { msg ->
            val toastMsg =
                msg.ifEmpty { this@SearchHistoryActivity.getString(R.string.error_abnormal_default) }

            Toast.makeText(
                this@SearchHistoryActivity,
                toastMsg,
                Toast.LENGTH_SHORT,
            ).show()
        })
    }

    //최근 검색기록
    private fun searchHistory() {
        searchType = SearchHistoryAdapter.TYPE_HISTORY
        binding.tvHotTitle.visibility = View.GONE
        binding.cvInAppBanner.visibility = View.GONE
        val currentLayout = binding.rvSearch.layoutParams as ConstraintLayout.LayoutParams
        currentLayout.topToBottom = R.id.tv_search_title
        binding.tvSearchTitle.apply {
            visibility = View.VISIBLE
            text = getString(R.string.search_history)
        }
        mSearchHistoryAdapter = SearchHistoryAdapter(this, searchType, this, searchList, null)
        binding.rvSearch.adapter = mSearchHistoryAdapter
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context?): Intent {
            return Intent(context, SearchHistoryActivity::class.java)
        }
    }
}