package net.ib.mn.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.FeedActivity.Companion.PARAM_USER_BLOCK_STATUS
import net.ib.mn.activity.FeedActivity.Companion.PARAM_USER_ID
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.activity.WebViewActivity
import net.ib.mn.activity.WriteArticleActivity
import net.ib.mn.adapter.NewArticleAdapter
import net.ib.mn.adapter.TagAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.common.extension.isNull
import net.ib.mn.common.util.logD
import net.ib.mn.common.util.logV
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.article.ArticlesRepositoryImpl
import net.ib.mn.core.domain.usecase.LikeArticleUseCase
import net.ib.mn.core.model.TagModel
import net.ib.mn.databinding.FragmentFreeboardBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.FreeBoardPrefsModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.NoticeModel
import net.ib.mn.utils.BoardLanguage
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Translation
import net.ib.mn.utils.UploadSingleton
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.FreeboardViewModel
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.find
import kotlin.collections.withIndex
import androidx.core.content.edit
import net.ib.mn.BuildConfig

/**
 * 지식돌/자게 공통
 */
@AndroidEntryPoint
@UnstableApi
open class FreeboardFragment : BaseFragment(),
    BaseDialogFragment.DialogResultHandler,
    SwipeRefreshLayout.OnRefreshListener,
    OnScrollToTopListener,
    TagAdapter.OnClickListener,
    Translation {

    @Inject
    lateinit var articlesRepository: ArticlesRepositoryImpl
    @Inject
    lateinit var likeArticleUseCase: LikeArticleUseCase
    @Inject
    lateinit var usersRepository: UsersRepository

    private var reqArticleHandler = Handler()
    private lateinit var binding: FragmentFreeboardBinding
    private val KEY_SHOW_POPULAR = "show_popular"

    private val viewModel: FreeboardViewModel by viewModels()

    private var account: IdolAccount? = null
    private var currentMostId = -1
    private var isInit = true

    private var isUpdateNow = false

    private lateinit var mIdol: IdolModel
    private var initialTagId = 0

    private var tagAdapter: TagAdapter? = null
    private lateinit var tags: ArrayList<TagModel>
    private var articleAdapter: NewArticleAdapter? = null
    private var articles = ArrayList<ArticleModel>()
    private var notices = ArrayList<NoticeModel>()
    private var orderBy = FILTER_DATE_ORDER
    private var totalCount = 0
    private var nextResourceUrl: String? = null

    private var keyword: String? = null
    private var freeBoardPrefsModel: FreeBoardPrefsModel? = null

    private var isSmallTalk = false

    private var selectedTagIds: String? = null
    private lateinit var etSearch: AppCompatEditText

    private val clickSubject: PublishSubject<ArticleModel> = PublishSubject.create()
    private val disposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFreeboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderBy = FILTER_DATE_ORDER
        account = IdolAccount.getAccount(context)
        currentMostId = account?.most?.getId() ?: -1

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)

                account = IdolAccount.getAccount(context)
                val newMost = account?.most?.getId() ?: -1

                if (newMost != currentMostId) {
                    currentMostId = newMost

                    val beforeTagId = tagAdapter?.getCurrentTag()?.id ?: -1

                    setTags()

                    // 이전 태그가 최애 태그인데 현재 최애가 바뀐 경우
                    if (beforeTagId == Const.MY_FAVORITE_TAG_ID || (newMost == -1 || newMost == Const.NON_FAVORITE_IDOL_ID)) {
                        getSmallTalk()
                    }

                    // 이전 태그가 최애 태그인데 현재 최애가 없는 경우
//                    if (beforeTagId == Const.MY_FAVORITE_TAG_ID && (newMost == -1 || newMost == Const.NON_FAVORITE_IDOL_ID)) {
//                        getHotArticles(false)
//                    }
                }
            }
        })

        observeVM()
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    override fun onRefresh() {
        binding.rvArticle.postDelayed({
            notices.clear()
            search() // 당겨서 새로고침하면 새로 검색하도록 협의 (2025.1.13 with 안태운)
            binding.swipeRefresh?.isRefreshing = false
        }, 500)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activity?.let {
            when (requestCode) {
                BaseWidePhotoFragment.MEZZO_PLAYER_REQ_CODE -> {
                    for (fragment in requireActivity().supportFragmentManager.fragments) {
                        if (fragment is WidePhotoFragment) {
                            fragment.onActivityResult(requestCode, resultCode, data)
                        }
                    }
                }
                RequestCode.ARTICLE_COMMENT.value -> {
                    if (data != null) {

                        val article = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            data.getSerializableExtra(
                                Const.EXTRA_ARTICLE,
                                ArticleModel::class.java,
                            )
                        } else {
                            data.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel?
                        }
                        if(article == null) {
                            return
                        }
                        val position = getArticlePosition(article.id) + notices.size

                        when (resultCode) {
                            ResultCode.REMOVED.value -> {
                                if (position >= 0) {
                                    articles.removeAt(getArticlePosition(article.id))
                                    articleAdapter?.notifyItemRemoved(position)
                                }
                            }
                            ResultCode.COMMENT_REMOVED.value,
                            ResultCode.EDITED.value -> {
                                if (position >= 0) {
                                    articles[getArticlePosition(article.id)] = article
                                    articleAdapter?.notifyItemChanged(position)
                                    articleAdapter?.notifyItemChanged(position + 1)
                                }
                            }
                            ResultCode.ARTICLE_LIKE_EXCEPTION.value -> {
                                postArticleLike(article)
                            }
                            ResultCode.UPDATE_LIKE_COUNT.value, ResultCode.COMMENTED.value, ResultCode.UPDATE_VIEW_COUNT.value -> {
                                getArticleResource(article.resourceUri)
                            }
                            ResultCode.BLOCKED.value -> {
                                val userId = data.getIntExtra(PARAM_USER_ID, -1) ?: -1
                                val isBlock = data.getStringExtra(PARAM_USER_BLOCK_STATUS) ?: ""

                                if (userId != -1 && isBlock.isNotEmpty()) {
                                    articles.removeIf { it.user?.id == userId }
                                    articleAdapter?.notifyDataSetChanged()
                                }
                            }
                            ResultCode.REPORTED.value -> {
                                val reportedArticle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    data.getSerializableExtra(
                                        Const.EXTRA_ARTICLE,
                                        ArticleModel::class.java,
                                    )
                                } else {
                                    data.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel?
                                }
                                articles.removeIf { it.id == reportedArticle?.id }
                                articleAdapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }
                RequestCode.ARTICLE_WRITE.value -> {
                    if (resultCode == Activity.RESULT_OK) {
                        getArticles(false)
                    }
                }
                RequestCode.ARTICLE_EDIT.value -> {
                    if (resultCode == ResultCode.EDITED.value) {
                        val resourceUri = data?.getStringExtra("resource_uri")
                        if (resourceUri == null) {
                            getArticles(false)
                        } else {
                            getArticleResource(resourceUri)
                        }
                    }
                }
                RequestCode.ARTICLE_REPORT.value -> {   //피드에서 신고했을 경우
                    if(resultCode == ResultCode.REPORTED.value) {
                        val articleId = data?.getSerializableExtra("article_position") as String
                        val position = getArticlePosition(articleId)

                        if (position >= 0) {
                            articles.removeAt(position)
//                            articleAdapter?.notifyItemRemoved(position)
                            articleAdapter?.notifyDataSetChanged() // 항목이 삭제되어서 인덱스가 바뀌므로 전체 갱신해줌

                            if (articles.size == 0) {
                                showEmpty()
                            }
                        }
                    } else if (resultCode == ResultCode.BLOCKED.value) {
                        val userId = data?.getIntExtra(PARAM_USER_ID, -1) ?: -1
                        val isBlock = data?.getStringExtra(PARAM_USER_BLOCK_STATUS) ?: ""

                        if (userId != -1 && isBlock.isNotEmpty()) {
                            articles.removeIf { it.user?.id == userId }
                            articleAdapter?.notifyDataSetChanged()
                        }
                    }
                }
                else -> super.onActivityResult(requestCode, resultCode, data)
            }

        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.ARTICLE_REPORT.value -> {
                if (resultCode == ResultCode.REPORTED.value) {
                    val article = data?.getSerializableExtra("article") as ArticleModel
                    val position = getArticlePosition(article.id)

                    if (position >= 0) {
                        val model = articles[position]
                        model.reportCount = model.reportCount + 1
                        articleAdapter?.notifyItemChanged(position)

                        val account = IdolAccount.getAccount(context)
                        if (account != null) {
                            val prefs = PreferenceManager
                                .getDefaultSharedPreferences(context)
                            val editor = prefs.edit()
                            val reportedArticles = prefs.getStringSet(
                                account.email + "_did_report",
                                HashSet())
                            reportedArticles!!.add(model.resourceUri)
                            editor.putStringSet(account.email + "_did_report",
                                reportedArticles).apply()
                        }
                        articles.removeAt(position)
//                        articleAdapter?.notifyItemRemoved(position)
                        articleAdapter?.notifyDataSetChanged() // 항목이 삭제되어서 인덱스가 바뀌므로 전체 갱신해줌

                        if (articles.size == 0) {
                            showEmpty()
                        }
                        //article 차단 목록 추가
                        context?.let { UtilK.addArticleReport(it, article.id) }
                    }
                }
            }
            RequestCode.ARTICLE_REMOVE.value -> {
                Util.closeProgress()

                if (resultCode == ResultCode.REMOVED.value) {
                    if (data != null) {
                        val position = getArticlePosition(data.getStringExtra(PARAM_ARTICLE_ID))

                        if (position >= 0) {
                            articles.removeAt(position)
//                            articleAdapter?.notifyItemRemoved(position)
                            articleAdapter?.notifyDataSetChanged() // 항목이 삭제되어서 인덱스가 바뀌므로 전체 갱신해줌

                            if (articles.size == 0) {
                                showEmpty()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun init() {
        if( context == null || activity == null ) {
            return
        }

        mGlideRequestManager = Glide.with(this)
        mIdol = IdolGson.getInstance()
            .fromJson("{\"resource_uri\": \"/api/v1/idols/${Const.IDOL_ID_FREEBOARD}/\"}",
                IdolModel::class.java)

        setSearchBar()
        setTags()
        setFilter()
        setTagListViewEvent()
        setRvContent()
        setSearch()
        setBtnWrite()
        setRefresh()
        setClickSubject()

        viewModel.initializeCacheData()
    }

    private fun setInitData() {
        if (initialTagId == Const.MY_FAVORITE_TAG_ID) { // 덕게
            if (account?.most == null) {
                showEmpty()
                return
            }
            getSmallTalk()
        } else if (initialTagId == 0) { // 자게
            getHotArticles(false)
        } else if (initialTagId == 9898) {
            getAllArticles(false)
        } else {
            getArticles(true)
        }
    }

    private fun getSmallTalk(isLoadedMore: Boolean = false) {
        viewModel.getSmallTalkInventory(
            context = requireContext(),
            isLoadMore = isLoadedMore,
            orderBy = orderBy,
            keyWord = keyword,
            locale = freeBoardPrefsModel?.selectLanguage,
            idolId = IdolAccount.getAccount(context)?.most?.getId() ?: 0
        )
    }

    private fun setRvContent() {
        articleAdapter = NewArticleAdapter(requireContext(),
            useTranslation = ConfigModel.getInstance(requireContext()).showTranslation,
            mIdol,
            articles,
            notices,
            lifecycleScope = lifecycleScope,
            onNoticeClick = {
                startActivity(WebViewActivity.createIntent(requireContext(), "notices", it.id.toInt(),
                    it.title, it.title, false))
            },
            onArticleClick = { model: ArticleModel, position: Int ->
                logV("item ${model.toString()}")

                if (model.idol == null) {
                    model.idol = account?.most
                }

                setUiActionFirebaseGoogleAnalyticsFragment(Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "communiy_comment")

                val currentTagName = tagAdapter?.getCurrentTag()?.name ?: ""
                
                val tagName = if (model.type == "M") {
                    model.idol?.getName(requireContext()) ?: ""
                } else {
                    tags.firstOrNull { it.id == model.tagId }?.name ?: currentTagName
                }
                startActivityForResult(NewCommentActivity.createIntent(context, model, position, false, tagName = tagName),
                    RequestCode.ARTICLE_COMMENT.value)
            }
        )

        val llm = LinearLayoutManager(requireContext())

        val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // 아래 도달했는지 체크
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                // 바닥 도달 조건
                if (lastVisibleItemPosition >= totalItemCount - 2) {
                    if (isUpdateNow) {
                        return
                    }
                    // 조건이 충족되면 로딩
                    if (articles.size < totalCount) {
                        isUpdateNow = true
                        if (tagAdapter?.getCurrentTag()?.id == Const.MY_FAVORITE_TAG_ID) {
                            getSmallTalk(true)
                        } else if (tagAdapter?.getCurrentTag()?.id == 0) {
                            getHotArticles(true)
                        } else if (tagAdapter?.getCurrentTag()?.id == 9898) {
                            getAllArticles(true)
                        } else {
                            getArticlesMore()
                        }
                    }
                }
            }
        }


        binding.rvArticle.apply {
            layoutManager = llm
            adapter = articleAdapter
            isNestedScrollingEnabled = true
            addOnScrollListener(scrollListener)
            itemAnimator = null // 번역하기 누를 때 깜빡이는 현상 제거
        }
    }

    private fun observeVM() {
        with(viewModel) {
            filterByPopularity.observe(viewLifecycleOwner, SingleEventObserver { isPopular ->
                binding.tvHot.isSelected = isPopular
                Util.setPreference(requireContext(), KEY_SHOW_POPULAR, isPopular)
                if (!isInit) {
                    getArticles(true)
                }
            })

            smallTalkCount.observe(viewLifecycleOwner, SingleEventObserver {
                totalCount = it
            })


            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        smallTalkArticleList.collectLatest { triple ->
                            Util.closeProgress()

                            val (isLoadMore, articleModelList, noticeModelList) = triple
                            val isListEmpty = articleModelList.isEmpty()

                            if (tagAdapter?.getCurrentTag()?.id != Const.MY_FAVORITE_TAG_ID) return@collectLatest

                            isUpdateNow = false

                            if (isListEmpty && noticeModelList.isEmpty()) {
                                // 기존 데이터 → 빈 리스트 상태 변화 처리
                                articles.clear()
                                notices.clear()
                                articleAdapter?.notifyDataSetChanged()
                                showEmpty()
                            } else {
                                hideEmpty()
                                if (isLoadMore) {
                                    articles.addAll(articleModelList)
                                    articleAdapter?.notifyItemRangeInserted(notices.size + articles.size - articleModelList.size, articleModelList.size)
                                } else {
                                    notices.clear()
                                    // 비밀의 방일 때 공지 노출 안되게 수정
                                    if (IdolAccount.getAccount(context)?.most?.getId() != Const.NON_FAVORITE_IDOL_ID) {
                                        notices.addAll(noticeModelList)
                                    }
                                    articles.clear()
                                    articles.addAll(articleModelList)
                                    articleAdapter?.notifyDataSetChanged()
                                    binding.rvArticle.post {
                                        binding.rvArticle.scrollToPosition(0)
                                    }
                                    if (articles.isEmpty() && notices.isEmpty()) {
                                        showEmpty()
                                    }
                                }
                            }
                        }
                    }

                    launch {
                        freeBoardPrefs.collectLatest { prefs ->
                            prefs?.let {
                                if (freeBoardPrefsModel == it) return@collectLatest
                                logD("FreeBoardPrefs", "언어: ${it.selectLanguage}, ID: ${it.selectLanguageId}")
                                freeBoardPrefsModel = it

                                setLanguageFilterInit()
                                if (isInit) {
                                    setInitData()
                                    isInit = false
                                } else {
                                    getArticlesFromTagSelected()
                                }
                            }
                        }
                    }

                }
            }
        }
    }


    open fun getArticleResource(resourceUri: String) {
        if (selectedTagIds.isNullOrEmpty()) {
            showEmpty()
            return
        }

        lifecycleScope.launch {
            articlesRepository.getArticle(
                resourceUri,
                { response ->
                    try {
                        val gson = IdolGson.getInstance(true)

                        val model = gson.fromJson(response.toString(), ArticleModel::class.java)
                        // 현재 게시글들 중 업데이트할거 찾기
                        for (i in 0 until articles.size) {
                            val item = articles[i]
                            if (item.resourceUri == resourceUri) {
                                articles[i] = model
                                articleAdapter?.notifyItemChanged(notices.size + i)
                                break
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, {
                    Util.closeProgress()
                    Toast.makeText(context, R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun search() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)

        val searchText = etSearch.text.toString().trim()
        keyword = if (searchText.isEmpty()) {
            null
        } else {
            searchText
        }

        etSearch.clearFocus()

        getArticlesFromTagSelected()
    }

    private fun setSearch() {
        etSearch = view?.findViewById(R.id.et_search) ?: return
        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search()
            } else {
                return@setOnEditorActionListener false
            }
            return@setOnEditorActionListener true
        }

        view?.findViewById<AppCompatImageButton>(R.id.btn_search)?.setOnClickListener {
            search()
        }
    }

    private fun setBtnWrite() {
        val btnWrite = requireView().findViewById<AppCompatImageButton>(R.id.btn_write)

        btnWrite.setOnClickListener {
            if(UploadSingleton.isLive()) {
                IdolSnackBar.make(activity?.findViewById(android.R.id.content)?:return@setOnClickListener, getString(R.string.upload_in_progress)).show()
                return@setOnClickListener
            }
            // 최애 게시글 작성 후 프리톡으로 돌아올 수 있게 한다
            val intent = WriteArticleActivity.createIntent(requireContext(), mIdol)
            intent.putExtra(Const.EXTRA_RETURN_TO, Const.IDOL_ID_FREEBOARD)
            startActivityForResult(
                intent,
                RequestCode.ARTICLE_WRITE.value)
        }
    }

    fun getArticles(showProgress: Boolean) {
        if (showProgress) {
            Util.showProgress(context)
        }

        if (selectedTagIds.isNullOrEmpty()) {
            Util.closeProgress()
            showEmpty()
            return
        }

        getArticles(false, showProgress)
    }

    private fun getHotArticles(isLoadedMore: Boolean) {
        val successListener: (JSONObject) -> Unit = { response ->
            Util.closeProgress()
            if (response.optBoolean("success")) {
                isUpdateNow = false

                if (!isLoadedMore) {
                    articles.clear()
                }

                val gson = IdolGson.getInstance(true)
                try {
                    totalCount = response.getJSONObject("meta")
                        .optInt("total_count")
                    nextResourceUrl = response.getJSONObject("meta")
                        .optString("next", null)

                    if (nextResourceUrl != null
                        && nextResourceUrl.equals("null")) {
                        nextResourceUrl = null
                    }
                    val array = response.getJSONArray("objects")
                    val models = ArrayList<ArticleModel>()

                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val model = gson.fromJson(obj.toString(), ArticleModel::class.java)

                        if (model.isViewable.equals("Y", true)) {
                            // 다음 게시물들 받아오기 직전에 새 게시물이 등록되는 경우 처리
                            var found = false
                            var k = articles.size - 1
                            while (k > 0 && k > articles.size - 50) {
                                val article = articles[k]
                                if (article.resourceUri == model.resourceUri) {
                                    found = true
                                    break
                                }
                                k--
                            }
                            if (!found) {
                                model.enterTime
                                if(UtilK.isUserNotBlocked(requireContext(), model.user?.id) && UtilK.isArticleNotReported(requireContext(), model.id)) {
                                    models.add(model)
                                }
                            }
                        }
                    }
                    if (models.size > 0) {
                        hideEmpty()
                        if (!isLoadedMore) {
                            articles.clear()
                        }
                        articles.addAll(models)
                        if (isLoadedMore) {
                            articleAdapter?.notifyItemRangeInserted(articles.size - models.size, models.size)
                        } else {
                            articleAdapter?.notifyDataSetChanged()
                            binding.rvArticle.post {
                                binding.rvArticle.scrollToPosition(0)
                            }
                        }
                    } else if (articles.size == 0) {
                        showEmpty()
                    }
                    Util.closeProgress()
                } catch (e: Exception) {
                    Util.closeProgress()
                    e.printStackTrace()
                }
            } else {
                Util.closeProgress()
                UtilK.handleCommonError(context, response)
            }
            if(KinFreeCount >= 2) {
                FeedActivity.USER_BLOCK_CHANGE = false
                KinFreeCount = 0
            }
        }

        val errorListener : (Throwable) -> Unit = { throwable ->
            Util.closeProgress()
            Toast.makeText(context,
                R.string.error_abnormal_exception,
                Toast.LENGTH_SHORT).show()
            if (Util.is_log()) {
                showMessage(throwable.message)
            }
        }

        lifecycleScope.launch {
            if (isLoadedMore) {
                articlesRepository.getFreeBoardHot(
                    nextResourceUrl ?: return@launch,
                    listener = successListener,
                    errorListener = errorListener
                )
            } else {
                articlesRepository.getFreeBoardHot(
                    orderBy = orderBy,
                    keyword = keyword,
                    locale = freeBoardPrefsModel?.selectLanguage,
                    listener = successListener,
                    errorListener = errorListener
                )
            }
        }
    }

    private fun getAllArticles(isLoadedMore: Boolean) {
        val successListener: (JSONObject) -> Unit = { response ->
            Util.closeProgress()
            if (response.optBoolean("success")) {
                isUpdateNow = false

                if (!isLoadedMore) {
                    articles.clear()
                }

                val gson = IdolGson.getInstance(true)
                try {
                    totalCount = response.getJSONObject("meta")
                        .optInt("total_count")
                    nextResourceUrl = response.getJSONObject("meta")
                        .optString("next", null)

                    if (nextResourceUrl != null
                        && nextResourceUrl.equals("null")) {
                        nextResourceUrl = null
                    }
                    val array = response.getJSONArray("objects")
                    val models = ArrayList<ArticleModel>()

                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val model = gson.fromJson(obj.toString(), ArticleModel::class.java)

                        if (model.isViewable.equals("Y", true)) {
                            // 다음 게시물들 받아오기 직전에 새 게시물이 등록되는 경우 처리
                            var found = false
                            var k = articles.size - 1
                            while (k > 0 && k > articles.size - 50) {
                                val article = articles[k]
                                if (article.resourceUri == model.resourceUri) {
                                    found = true
                                    break
                                }
                                k--
                            }
                            if (!found) {
                                model.enterTime
                                if(UtilK.isUserNotBlocked(requireContext(), model.user?.id) && UtilK.isArticleNotReported(requireContext(), model.id)) {
                                    models.add(model)
                                }
                            }
                        }
                    }
                    if (models.size > 0) {
                        hideEmpty()
                        if (!isLoadedMore) {
                            articles.clear()
                        }
                        articles.addAll(models)
                        if (isLoadedMore) {
                            articleAdapter?.notifyItemRangeInserted(articles.size - models.size, models.size)
                        } else {
                            articleAdapter?.notifyDataSetChanged()
                            binding.rvArticle.post {
                                binding.rvArticle.scrollToPosition(0)
                            }
                        }
                    } else if (articles.size == 0) {
                        showEmpty()
                    }
                    Util.closeProgress()
                } catch (e: Exception) {
                    Util.closeProgress()
                    e.printStackTrace()
                }
            } else {
                Util.closeProgress()
                UtilK.handleCommonError(context, response)
            }
            if(KinFreeCount >= 2) {
                FeedActivity.USER_BLOCK_CHANGE = false
                KinFreeCount = 0
            }
        }

        val errorListener : (Throwable) -> Unit = { throwable ->
            Util.closeProgress()
            Toast.makeText(context,
                R.string.error_abnormal_exception,
                Toast.LENGTH_SHORT).show()
            if (Util.is_log()) {
                showMessage(throwable.message)
            }
        }

        lifecycleScope.launch {
            if (isLoadedMore) {
                articlesRepository.getFreeBoardAll(
                    nextResourceUrl ?: return@launch,
                    listener = successListener,
                    errorListener = errorListener
                )
            } else {
                articlesRepository.getFreeBoardAll(
                    orderBy = orderBy,
                    keyword = keyword,
                    locale = freeBoardPrefsModel?.selectLanguage,
                    listener = successListener,
                    errorListener = errorListener
                )
            }
        }
    }

    private fun getArticles(isLoadedMore: Boolean, showProgress: Boolean = false) {
        val listener : (JSONObject) -> Unit = { response ->
            Util.closeProgress()
            if (response.optBoolean("success")) {
                isUpdateNow = false

                if (!isLoadedMore) {
                    articles.clear()
                    notices.clear()
                }

                val gson = IdolGson.getInstance(true)
                try {
                    totalCount = response.getJSONObject("meta")
                        .optInt("total_count")
                    nextResourceUrl = response.getJSONObject("meta")
                        .optString("next", null)

                    if (nextResourceUrl != null
                        && nextResourceUrl.equals("null")) {
                        nextResourceUrl = null
                    }
                    val array = response.getJSONArray("objects")
                    val models = ArrayList<ArticleModel>()

                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val model = gson.fromJson(obj.toString(), ArticleModel::class.java)

                        if (model.isViewable.equals("Y", true)) {
                            // 다음 게시물들 받아오기 직전에 새 게시물이 등록되는 경우 처리
                            var found = false
                            var k = articles.size - 1
                            while (k > 0 && k > articles.size - 50) {
                                val article = articles[k]
                                if (article.resourceUri == model.resourceUri) {
                                    found = true
                                    break
                                }
                                k--
                            }
                            if (!found) {
                                model.enterTime
                                if(UtilK.isUserNotBlocked(requireContext(), model.user?.id) && UtilK.isArticleNotReported(requireContext(), model.id)) {
                                    models.add(model)
                                }
                            }
                        }
                    }

                    val jsonArray = response.optJSONArray("top_notices")
                    val noticeList: List<NoticeModel> = if (jsonArray != null) {
                        val noticeListType = object : TypeToken<List<NoticeModel>>() {}.type
                        gson.fromJson(jsonArray.toString(), noticeListType)
                    } else {
                        emptyList()
                    }
                    if (models.size > 0 || noticeList.isNotEmpty()) {
                        hideEmpty()
                        if (!isLoadedMore) {
                            notices.addAll(noticeList)
                        }
                        articles.addAll(models)
                        if (isLoadedMore) {
                            articleAdapter?.notifyItemRangeInserted(notices.size+ articles.size - models.size, models.size)
                        } else {
                            articleAdapter?.notifyDataSetChanged()
                            binding.rvArticle.post {
                                binding.rvArticle.scrollToPosition(0)
                            }
                        }
                    } else if (articles.size == 0 && noticeList.isEmpty()) {
                        showEmpty()
                    }
                    Util.closeProgress()
                } catch (e: Exception) {
                    Util.closeProgress()
                    e.printStackTrace()
                }
            } else {
                Util.closeProgress()
                UtilK.handleCommonError(context, response)
            }
            if(KinFreeCount >= 2) {
                FeedActivity.USER_BLOCK_CHANGE = false
                KinFreeCount = 0
            }
        }

        val errorListener : (Throwable) -> Unit = { throwable ->
            Util.closeProgress()
            Toast.makeText(context,
                R.string.error_abnormal_exception,
                Toast.LENGTH_SHORT).show()
            if (Util.is_log()) {
                showMessage(throwable.message)
            }
        }

        var isMost = false
        val account = IdolAccount.getAccount(context)
        if (account?.userModel?.most != null &&
            account.userModel?.most?.getId() == mIdol.getId()) {
            isMost = true
        }

        if (isLoadedMore) {

            if(!nextResourceUrl.isNullOrEmpty()) {  //더이상 불러올 게시글이 없을 때 서버에서 path를 빈 값을 주기 때문에 null이란 path를 보내서 error Toast가 나와 막아줌
                MainScope().launch {
                    articlesRepository.getArticles(
                        nextResourceUrl!!,
                        isMost ?: false,
                        keyword,
                        selectedTagIds,
                        null,
                        null,
                        isPopular = false,
                        listener = listener,
                        errorListener = errorListener
                    )
                }
            }
        } else {
            nextResourceUrl = null

            // TODO hot 부르기 뭔지 물어보기
            MainScope().launch {
                articlesRepository.getArticles(
                    mIdol.getId() ?: 0,
                    isMost ?: false,
                    orderBy,
                    keyword,
                    selectedTagIds,
                    null,
                    null,
                    isPopular = false,
                    locale = freeBoardPrefsModel?.selectLanguage,
                    listener = listener,
                    errorListener = errorListener
                )
            }
        }
    }

    private fun getArticlesMore() {
        getArticles(isLoadedMore = true, showProgress = false)
    }

    private fun getArticlePosition(articleId: String?): Int {
        val position = articles.withIndex().find { it.value.id == articleId }?.index
        if (position != null) return position
        return -1
    }

    private fun setRefresh() {
        binding.swipeRefresh.apply {
            setOnRefreshListener(this@FreeboardFragment)
            setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.main))
        }
    }

    private fun showEmpty() {
        if (tagAdapter?.getCurrentTag()?.id == Const.MY_FAVORITE_TAG_ID) {
            binding.appbar.visibility = View.GONE
        } else {
            binding.appbar.visibility = View.VISIBLE
        }

        binding.rvArticle.post {
            binding.rvArticle.visibility = View.GONE
            showExpandedEmpty()
        }

        if (tagAdapter?.getCurrentTag()?.id == Const.MY_FAVORITE_TAG_ID && etSearch.text.toString().trim().isEmpty()
            && (account?.most?.getId() == Const.NON_FAVORITE_IDOL_ID || account?.most == null)) {
            binding.llEmptyMost.visibility = View.VISIBLE
            binding.tvSubtitle.text =  if (BuildConfig.CELEB) {
                getString(R.string.freeboard_nobias_subtitle_celeb)
            } else {
                getString(R.string.freeboard_nobias_subtitle)
            }
            binding.tvEmpty.visibility = View.GONE
            return
        }

        binding.llEmptyMost.visibility = View.GONE
        binding.tvEmpty.apply {
            visibility = View.VISIBLE

            text = if (etSearch.text.toString().trim().isEmpty()) {
                getString(R.string.freeboard_empty)
            } else {
                getString(R.string.no_search_result)
            }
        }
    }

    private fun hideEmpty() {
        binding.appbar.visibility = View.VISIBLE
        binding.rvArticle.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        binding.llEmptyMost.visibility = View.GONE
    }

    private fun showExpandedEmpty() {
        val appbar = view?.findViewById<AppBarLayout>(R.id.appbar)
        val currentTagId = tagAdapter?.getCurrentTag()?.id
        if (appbar != null) {
            val lp = if (currentTagId == Const.MY_FAVORITE_TAG_ID) {
                binding.llEmptyMost.layoutParams
            } else {
                binding.tvEmpty.layoutParams
            }
            val metrics = this.resources.displayMetrics
            lp.height = metrics.heightPixels - appbar.height - Util.getToolbarHeight(context) - Util.convertDpToPixel(requireContext(), 130F).toInt()

            if (currentTagId == Const.MY_FAVORITE_TAG_ID) {
                binding.llEmptyMost.layoutParams = lp
            } else {
                binding.tvEmpty.layoutParams = lp
            }

            appbar.setExpanded(true, true)
        }
    }

    // 좋아요 Api 중복호출을 예방하기 위해 debounce 처리한 함수
    private fun setClickSubject() {
        clickSubject.debounce(500, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
            .subscribe { model ->
                if(model.isUserLikeCache == model.isUserLike) {
                    return@subscribe
                }
                if(model.idol?.getId() == Const.IDOL_ID_FREEBOARD) {
                    setUiActionFirebaseGoogleAnalyticsFragment(GaAction.FREEBOARD_LIKE.actionValue, GaAction.FREEBOARD_LIKE.label)
                } else {
                    setUiActionFirebaseGoogleAnalyticsFragment(GaAction.QNA_LIKE.actionValue, GaAction.QNA_LIKE.label)
                }
                postArticleLike(model)
            }.addTo(disposable)
    }

    private fun postArticleLike(model: ArticleModel) {
        MainScope().launch {
            likeArticleUseCase(model.id, !model.isUserLike).collect { response ->
                if( !response.success ) {
                    response.message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                    return@collect
                }
                getArticleResource(model.resourceUri)
            }
        }
    }

    fun scrollToHeader() {
        binding.rvArticle.scrollToPosition(0)
    }

    override fun onItemClicked(tag: TagModel) {
        Util.showProgress(requireContext())

        Util.setPreference(context, Const.SELECTED_TAG_IDS, tag.id.toString())

        val delayTime: Long = if (tag.id == 0) {
            0
        } else {
            500
        }

        if (account == null) {
            account = IdolAccount.getAccount(context)
        }

        reqArticleHandler.removeCallbacksAndMessages(null)
        reqArticleHandler.postDelayed({
            selectedTagIds = Util.getPreference(context, Const.SELECTED_TAG_IDS)

            notices.clear()
            getArticlesFromTagSelected()
        }, delayTime)
    }

    override fun onResume() {
        super.onResume()
        if(FeedActivity.USER_BLOCK_CHANGE && KinFreeCount <3) {
            KinFreeCount++
        }
    }

    override fun onPause() {
        super.onPause()
        // 검색창 포커스 해제
        binding.searchBar.etSearch.clearFocus()
    }

    override fun onScrollToTop() {
        if(!::binding.isInitialized) return
        binding.rvArticle.scrollToPosition(0)
    }

    private fun setTagListViewEvent() {
        binding.rvTag.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (recyclerView.computeHorizontalScrollOffset() == 0) {
                    binding.vRvBg.setBackgroundResource(0)
                } else {
                    binding.vRvBg.setBackgroundResource(R.drawable.bg_free_board_tag_list)
                }
            }
        })
    }

    private fun setLanguageFilterInit() {
        val lastSelect = freeBoardPrefsModel?.selectLanguageId ?: ""

        binding.tvLanguageFilter.text = if (lastSelect == "") {
            requireContext().getString(R.string.filter_all_language)
        } else {
            requireContext().getString(BoardLanguage.fromCode(lastSelect).labelResId)
        }

        binding.llLanguageFilter.setOnClickListener {
            setLanguageFilter()
        }
    }

    // 검색창 UI 설정
    private fun setSearchBar() = with(binding) {
        searchBar.etSearch.hint = getString(R.string.freeboard_search)
        searchBar.btnSearch.visibility = View.GONE

        // 검색창에 포커스가 가면 검색 버튼 표시
        searchBar.etSearch.setOnFocusChangeListener { _, hasFocus ->
            searchBar.btnSearch.visibility = if (hasFocus) View.VISIBLE else View.GONE
        }

    }

    fun refresh(tag: Int = -1) {
        // 중복호출 방지를 위해 filterByLatest() 호출하지 않음
        orderBy = FILTER_DATE_ORDER
        binding.tvFilter.text = getString(R.string.freeboard_order_newest)

        val lastSelect = freeBoardPrefsModel?.selectLanguageId ?: -1

        binding.tvLanguageFilter.text = if (BoardLanguage.ALL.code == lastSelect) {
            requireContext().getString(R.string.filter_all_language)
        } else {
            requireContext().getString(BoardLanguage.fromCode(lastSelect.toString()).labelResId)
        }

        // tag가 있으면 해당 태그를 선택
        if (tag != -1) {
            Util.showProgress(requireContext())
            selectedTagIds = tag.toString()
            Util.setPreference(context, Const.SELECTED_TAG_IDS, tag.toString())
            tagAdapter?.selectTag(tag)
            // 태그 position 가져오기
            val position = tags.indexOfFirst { it.id == tag }
            binding.rvTag.scrollToPosition(position)

            reqArticleHandler.removeCallbacksAndMessages(null)
            reqArticleHandler.postDelayed({
                notices.clear()
                getArticlesFromTagSelected()
            }, 500)
        }

    }

    private fun setLanguageFilter() {
        val dialogFragment = LanguageFilterDialogFragment {
            freeBoardPrefsModel?.let { model ->
                model.selectLanguage = it.first
                model.selectLanguageId = it.second
            }

            logV("FreeBoardPrefs", "setLanguage ${it.first}, ${it.second}")

            viewModel.setFreeBoardSelectLanguage(it.first, it.second)
            binding.tvLanguageFilter.text = if (BoardLanguage.ALL.code == it.second) {
                requireContext().getString(R.string.filter_all_language)
            } else {
                requireContext().getString(BoardLanguage.fromCode(it.second).labelResId)
            }

            getArticlesFromTagSelected()
        }
        dialogFragment.show(parentFragmentManager, "LanguageFilterDialogFragment")
    }

    private fun setTags() {
        val gson = IdolGson.getInstance()
        val listType = object : TypeToken<List<TagModel>>() {}.type

        try {
            tags =
                gson.fromJson(Util.getPreference(requireContext(), Const.BOARD_TAGS), listType)
            selectedTagIds = Util.getPreference(requireContext(), Const.SELECTED_TAG_IDS)

            var selectedTagIdsList = selectedTagIds!!.split(",")

            if (selectedTagIdsList.isEmpty() || selectedTagIdsList.size > 1) {
                selectedTagIdsList = listOf("0")
            }

            // 인기글 버튼
            val tagHot = TagModel(0, "", "N",false)
            tags.add(0, tagHot)

            // ALL 버튼
            val tagAll = TagModel(9898, "ALL", "N",false)
            tags.add(1, tagAll)

            tags.forEach {
                it.selected = selectedTagIdsList.contains(it.id.toString())
            }

            // 선택된 태그가 없을 때 인기글 선택
            if (tags.firstOrNull { it.selected }.isNull()) {
                Util.setPreference(context, Const.SELECTED_TAG_IDS, selectedTagIds)
                tags[0].selected = true
            }

            if (!tags[0].selected) {
                val firstSelected = tags.firstOrNull { it.selected && it.id != 0 }
                if (firstSelected != null) {
                    tags.remove(firstSelected)
                    tags.add(1, firstSelected)
                    initialTagId = firstSelected.id
                }
            } else {
                initialTagId = tags[0].id
            }

            if (initialTagId == Const.MY_FAVORITE_TAG_ID) {
                isSmallTalk = true
            }

            tagAdapter = TagAdapter(requireContext(), tags, this)
            binding.rvTag.adapter = tagAdapter
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }


    private fun setFilter() {
        val sheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_BOARD_FILTER)

        binding.llFilter.setOnClickListener {
            val tag = "filter"
            val oldFrag = requireActivity().supportFragmentManager.findFragmentByTag(tag)
            if (oldFrag == null) {
                sheet.show(requireActivity().supportFragmentManager, tag)
            }
        }

        val showPopular = Util.getPreferenceBool(requireContext(), KEY_SHOW_POPULAR, false)
        binding.tvHot.isSelected = showPopular // observe에서 처리되면서 깜빡이는 현상이 있어 미리 세팅
        binding.tvHot.setOnClickListener {
            viewModel.toggleFilterByPopularity()
        }
        // 초기값은 인기글 보기부터
        viewModel.filterByPopularity(showPopular)
    }

    fun filterByLatest() {
        Util.showProgress(context)
        orderBy = FILTER_DATE_ORDER
        binding.tvFilter.text = getString(R.string.freeboard_order_newest)
        getArticlesFromTagSelected()
    }

    fun filterByComments() {
        Util.showProgress(context)
        orderBy = FILTER_COMMENT_ORDER
        binding.tvFilter.text = getString(R.string.freeboard_order_comments)
        getArticlesFromTagSelected()
    }

    fun filterByLike() {
        Util.showProgress(context)
        orderBy = FILTER_LIKE_ORDER
        binding.tvFilter.text = getString(R.string.order_by_like)
        getArticlesFromTagSelected()
    }

    fun filterByViewCount() {
        Util.showProgress(context)
        orderBy = FILTER_HITS_ORDER
        binding.tvFilter.text = getString(R.string.order_hit)
        getArticlesFromTagSelected()
    }

    private fun getArticlesFromTagSelected() {
        when (tagAdapter?.getCurrentTag()?.id) {
            Const.MY_FAVORITE_TAG_ID -> {
                if (account?.most == null) {
                    showEmpty()
                    Util.closeProgress()
                    return
                }
                getSmallTalk()
            }
            0 -> {
                getHotArticles(false)
            }
            9898 -> {
                getAllArticles(false)
            }
            else -> {
                getArticles(true)
            }
        }
    }

    companion object {
        const val PARAM_ARTICLE_ID = "articleId"
        const val FILTER_DATE_ORDER = "-created_at"
        const val FILTER_COMMENT_ORDER = "-num_comments"
        const val FILTER_LIKE_ORDER = "-like_count"
        const val FILTER_HITS_ORDER = "-view_count"
        var KinFreeCount = 0

        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, FreeboardFragment::class.java)
        }
    }
}
