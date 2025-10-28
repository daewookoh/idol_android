package net.ib.mn.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.text.Selection
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.CommunityActivity.Companion.CATEGORY_COMMUNITY
import net.ib.mn.activity.CommunityActivity.Companion.CATEGORY_IDOLTALK
import net.ib.mn.activity.CommunityActivity.Companion.CATEGORY_SCHEDULE
import net.ib.mn.activity.CommunityActivity.Companion.PARAM_ARTICLE_POSITION
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.adapter.SearchedAdapter
import net.ib.mn.adapter.SearchedAdapter.Companion.SUPPORT_ING
import net.ib.mn.adapter.SearchedAdapter.Companion.SUPPORT_SUCCESS
import net.ib.mn.addon.IdolGson
import net.ib.mn.addon.InternetConnectivityManager
import net.ib.mn.chatting.chatDb.ChatRoomList
import net.ib.mn.core.data.repository.FavoritesRepository
import net.ib.mn.core.data.repository.SearchRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.core.domain.usecase.LikeArticleUseCase
import net.ib.mn.databinding.ActivitySearchResultBinding
import net.ib.mn.dialog.ArticleRemoveDialogFragment
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.ReportDialogFragment
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.fragment.ArticleViewMoreBottomSheetFragment
import net.ib.mn.fragment.MultiWidePhotoFragment
import net.ib.mn.fragment.WidePhotoFragment
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.FavoriteModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.SearchHistoryModel
import net.ib.mn.model.SupportListModel
import net.ib.mn.model.WallpaperModel
import net.ib.mn.support.SupportDetailActivity
import net.ib.mn.support.SupportPhotoCertifyActivity
import net.ib.mn.utils.ApiCacheManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Translation
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.ext.getSerializableData
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.safeSetImageBitmap
import net.ib.mn.view.EndlessRecyclerViewScrollListener
import net.ib.mn.view.ExodusImageView
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class SearchResultActivity : BaseActivity(),
        BaseDialogFragment.DialogResultHandler,
        SearchedAdapter.OnIdolClickListener,
        SearchedAdapter.OnArticleClickListener,
        SearchedAdapter.OnSupportClickListener,
        SearchedAdapter.OnAdapterCheckedChangeListener,
        SearchedAdapter.SmallTalkListener,
        Translation {
    @Inject
    lateinit var likeArticleUseCase: LikeArticleUseCase
    @Inject
    lateinit var articlesRepository: ArticlesRepository
    @Inject
    lateinit var sharedAppState: SharedAppState
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var favoritesRepository: FavoritesRepository
    @Inject
    lateinit var searchRepository: SearchRepository
    @Inject
    lateinit var accountManager: IdolAccountManager
    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase

    private var mAccount: IdolAccount? = null
    private lateinit var mContext: Context
    private lateinit var mKeyword: String
    private lateinit var imm: InputMethodManager

    private lateinit var scrollListener: EndlessRecyclerViewScrollListener
    private lateinit var mSearchedRecyclerView: RecyclerView
    private lateinit var mSearchedAdapter: SearchedAdapter
    private lateinit var mNoSearchResultView: AppCompatTextView

    private var isFirstSearching = true
    private var isLoading = false
    private var keywordList = ArrayList<String>()

    private lateinit var mGlideRequestManager: RequestManager

    private var mStorageSearchedIdolList = ArrayList<IdolModel>()
    private var mSearchedIdolList = ArrayList<IdolModel>()
    private var mSearchedArticleList = ArrayList<ArticleModel>()
    private var mSearchedSmallTalkList = ArrayList<ArticleModel>()
    private var mStorageSearchedSupportList = ArrayList<SupportListModel>()
    private var mSearchedSupportList = ArrayList<SupportListModel>()
    private var mSearchedWallpapersList = ArrayList<WallpaperModel>()


    private val mIdols = HashMap<String, IdolModel>()
    private val mFavorites = HashMap<Int, Int>() // 실제 idol id, 사용자의 즐겨찾기 id -> 사용자에게 보여주는 데이타로 사용
    private val mtempFavorites = HashMap<Int, Int>() // 사용자가 즐겨찾기를 변경한 데이타를 갖고 있는다. 검색하면 mtempFavorites 를 mFavorites로 옮긴다.

    // 움짤 검은화면 방지
    private var activeThumbnailView: View? = null
    private var activeExoPlayerView: View? = null

    // lazy image loading
    internal var lazyImageLoadHandler = Handler()
    internal var lazyImageLoadRunnable: Runnable? = null

    private var idolSoloFinalId :Int? = null
    private var idolGroupFinalId : Int? = null

    private var isIdolListViewMoreClicked = false

    private var userFavoriteCange = false
    private var searchList = ArrayList<SearchHistoryModel>()

    private var backPressClick = 1

    private var smallTalkListTotal = 0
    private var smallTalkListLimit = 10
    private var smallTalkListOffset = 0
    private var isSmallTalkViewMore = false

    private lateinit var binding: ActivitySearchResultBinding

    private val mBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (activeThumbnailView != null) {
                activeThumbnailView!!.visibility = View.GONE
                Util.log("*** hide thumbnail")
            }
            if (activeExoPlayerView != null) {
                activeExoPlayerView!!.visibility = View.VISIBLE

                val shutterId = resources.getIdentifier("exo_shutter", "id", packageName)
                val shutter = activeExoPlayerView!!.findViewById<View>(shutterId)
                Util.log(">>>>> COMMU: shutter visibility: " + shutter.visibility + " alpha:" + shutter.alpha)
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_search_result)
        binding.llContainer.applySystemBarInsets()

        mContext = this
        mAccount = IdolAccount.getAccount(mContext)
        mKeyword = intent.getStringExtra(PARAM_SEARCH_KEYWORD).toString()
        mSearchedRecyclerView = binding.searchedRecyclerView
        mNoSearchResultView = binding.noSearchResultView
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mGlideRequestManager = Glide.with(this)

        keywordList.add(mKeyword)
        // cursor 위치를 text 끝에 놓기 위함
        setToolbar()

        mSearchedRecyclerView.isNestedScrollingEnabled = false
        mSearchedAdapter = SearchedAdapter(mContext,
                Glide.with(this),
                mAccount,
                keywordList,
                mSearchedSmallTalkList,
                mSearchedIdolList,
                mStorageSearchedIdolList,
                mSearchedArticleList,
                mSearchedSupportList,
                mStorageSearchedSupportList,
                mSearchedWallpapersList,
                {   model: IdolModel, v: View, position: Int ->
                    onIdolButtonClick(model, v, position)
                },{
            view , supportStatus:Int , supportModel:SupportListModel ->
            onSupportButtonClick(view,supportStatus,supportModel)
        },{
            button: CompoundButton, isChecked: Boolean, item: IdolModel ->
            onCheckedChanged(button, isChecked, item)
        },{
            model: ArticleModel, v: View, position: Int ->
            onArticleButtonClick(model, v, position)
        }, this, lifecycleScope, getIdolByIdUseCase)

        mSearchedRecyclerView.adapter = mSearchedAdapter
        val llm = LinearLayoutManager(this)
        mSearchedRecyclerView.layoutManager = llm
        scrollListener = object : EndlessRecyclerViewScrollListener(llm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                //1개 만 검색되었을때,  스크롤 마지막 임으로 감지되어 loadMore이  불리는 현상이 있음.
                //그래서  loadMore  작동하기 위해서는  1보다 초과 해야하는  조건을 달아줌.
                if(totalItemsCount>1){
                  loadMoreArticles()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    // 움짤
                    for (listItemIndex in 0..llm.findLastVisibleItemPosition() - llm.findFirstVisibleItemPosition()) {
                        // onResume에서 넘어오면 비디오 로딩 표시 -> 로딩은 썸네일로 바뀌어서 이제 없고 자동재생 시작
                        if (newState == -1) {
                            checkVisibility(recyclerView, listItemIndex)
                        }
                    }

                    if (lazyImageLoadRunnable != null) {
                        lazyImageLoadHandler.removeCallbacks(lazyImageLoadRunnable!!)
                    }

                    val isDataSavingMode = Util.getPreferenceBool(this@SearchResultActivity, Const.PREF_DATA_SAVING, false)
                            && !InternetConnectivityManager.getInstance(this@SearchResultActivity).isWifiConnected

                    if (!isDataSavingMode) {
                        lazyImageLoadRunnable = object : Runnable {
                            override fun run() {
                                for (listItemIndex in 0..llm.findLastVisibleItemPosition() - llm.findFirstVisibleItemPosition()) {
                                    val listItem = recyclerView.getChildAt(listItemIndex)
                                    val photo = listItem.findViewById<ExodusImageView>(R.id.attach_photo)
                                    if (photo?.loadInfo != null) {
                                        val url = photo.loadInfo

                                        Util.log(">>>>>>>>>>>>>>> loading original size image $url")
                                        // thumbnail이 다 로드되어야 원본 이미지 부른다
                                        if (photo.getLoadInfo(R.id.TAG_LOAD_LARGE_IMAGE) == true
                                                && photo.getLoadInfo(R.id.TAG_IS_UMJJAL) == false) {
                                            photo.post {
                                                mGlideRequestManager
                                                        .asBitmap()
                                                        .load(url)
//                                                        .placeholder(photo.drawable)
                                                        .listener(object : RequestListener<Bitmap> {
                                                            override fun onLoadFailed(e: GlideException?,
                                                                                      model: Any?,
                                                                                      target: Target<Bitmap>,
                                                                                      isFirstResource: Boolean): Boolean {
                                                                return false
                                                            }

                                                            override fun onResourceReady(resource: Bitmap,
                                                                                         model: Any,
                                                                                         target: Target<Bitmap>,
                                                                                         dataSource: DataSource,
                                                                                         isFirstResource: Boolean): Boolean {

                                                                runOnUiThread {
                                                                    // 썸네일 로딩 후 멈춘 상태에서 1초 후에 로딩 됨
                                                                    val loadInfo = photo.loadInfo as String?
                                                                    if (loadInfo != null && loadInfo == url) {
                                                                        photo.safeSetImageBitmap(this@SearchResultActivity, resource)
                                                                        Util.log(">>>>>>>>>>>>>>>:: image displayed $url")
                                                                    }
                                                                }

                                                                return false
                                                            }
                                                        })
                                                        .submit()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (lazyImageLoadRunnable != null) {
                            lazyImageLoadHandler.postDelayed(lazyImageLoadRunnable!!, 1000)
                        }

                        // 이미 고해상도 이미지를 보여준 경우라면 바로 보여주기
                        for (listItemIndex in 0..llm.findLastVisibleItemPosition() - llm.findFirstVisibleItemPosition()) {
                            val listItem = recyclerView.getChildAt(listItemIndex)
                            val photo = listItem?.findViewById<ExodusImageView>(R.id.attach_photo)
                            if (photo?.loadInfo != null
                                    && photo.getLoadInfo(R.id.TAG_IS_UMJJAL) == false) {
                                val url = photo.loadInfo as String

                                mGlideRequestManager
                                        .asBitmap()
                                        .load(url)
//                                        .placeholder(photo.drawable)
                                        .into(photo)
                            }
                        }
                    }
                }

                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (lazyImageLoadRunnable != null) {
                    lazyImageLoadHandler.removeCallbacks(lazyImageLoadRunnable!!)
                }

                // 저사양 단말에서 이미지 보였다 안보였다 하는 현상 수정.
                activeThumbnailView = null
                activeExoPlayerView = null

                for (listItemIndex in 0..llm.findLastVisibleItemPosition() - llm.findFirstVisibleItemPosition()) {
                    checkVisibility(recyclerView, listItemIndex)
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        }
        mSearchedRecyclerView.addOnScrollListener(scrollListener)
        loadFavorites()
        setRecyclerViewListener()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mKeyword = intent!!.getStringExtra(PARAM_SEARCH_KEYWORD).toString()
        binding.searchToolbar.searchInput.setText(mKeyword)
        isFirstSearching = true

        // 새 검색 시작 전 데이터 목록을 비우고 어댑터에 알려 UI 불일치를 방지합니다.
        mSearchedIdolList.clear()
        mSearchedArticleList.clear()
        mSearchedSupportList.clear()
        mSearchedSmallTalkList.clear()
        mSearchedWallpapersList.clear()
        mSearchedAdapter.notifyDataSetChanged()

        loadFavorites()
    }

    override fun onResume() {
        // 움짤 검은화면 방지
        try{
            LocalBroadcastManager.getInstance(this).registerReceiver(
                mBroadcastReceiver, IntentFilter(Const.VIDEO_READY_EVENT))
        } catch (e:SecurityException){
            e.printStackTrace()
        } catch (e:IllegalStateException){
            e.printStackTrace()
        }

        super.onResume()
    }

    override fun onPause() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
        } catch (e: Exception) {
        }

        searchResultFinish = backPressClick
        if(userFavoriteCange) {
            accountManager.fetchUserInfo(this)
        }
        userFavoriteCange = false
        super.onPause()
    }

    private fun setRecyclerViewListener() {
        mSearchedAdapter.setPhotoClickListener(object : ArticlePhotoListener{
            override fun widePhotoClick(model: ArticleModel, position: Int?) {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.SEARCH_WIDEPHOTO.actionValue,
                    GaAction.SEARCH_WIDEPHOTO.label
                )

                if (this@SearchResultActivity.isFinishing) return
                if(model.files.isNullOrEmpty() || model.files.size < 2) {
                    WidePhotoFragment.getInstance(model)
                        .show(supportFragmentManager, "wide_photo")
                }else {
                    MultiWidePhotoFragment.getInstance(model, position?:0)
                        .show(supportFragmentManager, "wide_photo")
                }
            }

            override fun linkClick(link: String) {
                try {
                    val mIntent = Intent(this@SearchResultActivity, AppLinkActivity::class.java).apply {
                        data = Uri.parse(link)
                    }
                    startActivity(mIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        })
    }
    
    private fun showSoftKeyboard() {
        binding.searchToolbar.searchInput.requestFocus()
        binding.searchToolbar.searchInput.isCursorVisible = true
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchToolbar.searchInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideSoftKeyboard() {
        binding.searchToolbar.searchInput.isCursorVisible = false
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchToolbar.searchInput.windowToken, 0)
    }

    @OptIn(UnstableApi::class)
    private fun checkVisibility(view: RecyclerView, listItemIndex: Int) {
        if (Build.VERSION.SDK_INT < Const.EXOPLAYER_MIN_SDK) {
            return
        }

        val listItem = view.getChildAt(listItemIndex) ?: return

        val thumbnailView = listItem.findViewById<ExodusImageView>(R.id.attach_photo)
        val exoPlayerView = listItem.findViewById<PlayerView>(R.id.attach_exoplayer_view)

        //        Util.log("checkVisibility "+listItemIndex );
        if (exoPlayerView != null && exoPlayerView.visibility == View.VISIBLE) {
            val loopingSource = exoPlayerView.tag as ProgressiveMediaSource
            // videoview 가운데 부분의 화면상 위치 구하고
            val videoHeight = exoPlayerView.height
            val location = IntArray(2)
            exoPlayerView.getLocationInWindow(location)
            val y = location[1]
            val videoCenterY = y + videoHeight / 2

            // 리스트뷰의 화면상 위치 구해서
            mSearchedRecyclerView.getLocationInWindow(location)
//            mListView.getLocationInWindow(location)
            val listviewTop = location[1]
            val listviewBottom = listviewTop + mSearchedRecyclerView.height

            // 화면에 조금이라도 걸쳐져 있으면
            if (y < listviewTop || y + videoHeight > listviewBottom) {
                var player: ExoPlayer? = null
                if (exoPlayerView.player != null) {
                    player = exoPlayerView.player as ExoPlayer
                }
                player?.stop()

                exoPlayerView.player = null
                if (thumbnailView.visibility == View.GONE) {
                    thumbnailView.visibility = View.VISIBLE
                    //                    Util.log(">>>>>>>>>>>>>> show THUMBNAIL "+listItemIndex);
                }
            } else {
                activeThumbnailView = thumbnailView
                activeExoPlayerView = exoPlayerView

                exoPlayerView.post {
                    var player: ExoPlayer? = null
                    if (exoPlayerView.player != null) {
                        player = exoPlayerView.player as ExoPlayer
                    }

                    if (player == null) {
                        player = mSearchedAdapter.getPlayer()
                        player!!.playWhenReady = false
                        exoPlayerView.player = player
                        player!!.prepare(loopingSource)
                        player!!.playWhenReady = true
                    }

                    if (player!!.playWhenReady /* isPlaying */) {
                        if (thumbnailView.visibility == View.VISIBLE) {
                        }
                    } else {
                        player!!.prepare(loopingSource)
                        player!!.playWhenReady = true
                    }
                }

            }
        }
    }

    private fun setToolbar() = with(binding.searchToolbar) {
        root.visibility = View.VISIBLE

        searchClose.setOnClickListener{
            this@SearchResultActivity.onBackPressed()
        }

        searchInput.setText(mKeyword)

        Selection.setSelection(searchInput.text, searchInput.length())
        searchInput.setOnClickListener {
            showSoftKeyboard()
        }
        searchInput.setOnEditorActionListener(TextView.OnEditorActionListener { textView, actionId, keyEvent ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    doSearch()
                }
                else ->
                    // 기본 엔터키 동작
                    return@OnEditorActionListener false
            }
            true
        })
        searchBtn.setOnClickListener { doSearch() }

        binding.searchToolbar.flBack.setOnClickListener {
            finish()
        }
    }

    private fun doSearch() {
        hideSoftKeyboard()
        isIdolListViewMoreClicked=false
        val searchText = binding.searchToolbar.searchInput.text.toString().trim()

        if (searchText.isEmpty()) {
            mKeyword = ""
            binding.searchToolbar.searchInput.clearFocus()
        } else {
            isFirstSearching = mKeyword != searchText
            keywordList.clear()
            keywordList.add(searchText)
            mKeyword = searchText

            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<List<SearchHistoryModel>>() {}.type
            searchList = gson.fromJson(Util.getPreference(this, Const.SEARCH_HISTORY).toString(), listType)
            if(searchList.contains(SearchHistoryModel(mKeyword))){
                searchList.remove(SearchHistoryModel(binding.searchToolbar.searchInput.text.toString()))
            }
            searchList.add(SearchHistoryModel(mKeyword))
            if(searchList.size>10){
                searchList.removeAt(0)
            }
            Util.setPreferenceArray(this, Const.SEARCH_HISTORY, searchList)

            // 새 검색 시작 전 데이터 목록을 비우고 어댑터에 알려 UI 불일치를 방지합니다.
            mSearchedIdolList.clear()
            mSearchedArticleList.clear()
            mSearchedSupportList.clear()
            mSearchedSmallTalkList.clear()
            mSearchedWallpapersList.clear()
            mSearchedAdapter.notifyDataSetChanged()

            loadFavorites()
        }
    }

    private fun loadMoreArticles() {
        val response = ApiCacheManager.getInstance().getCache(Const.KEY_FAVORITE)
        loadSearchResult(mKeyword, "article", mSearchedArticleList.size, 50, response,false)
    }

    private fun loadSearchResult(keyword: String?,
                                 category: String?,
                                 offset: Int,
                                 limit: Int,
                                 favoritesResponse: JSONObject? = null,
                                 needToUpdateSupport :Boolean
    ) {

        if (isFirstSearching) Util.showProgress(this, true)

        lifecycleScope.launch {
            searchRepository.search(
                keyword,
                category,
                offset,
                limit,
                { response ->
                    Util.closeProgress()

                    val gson = IdolGson.getInstance(true)
                    val idolsJsonArray: JSONArray
                    val articlesJsonArray: JSONArray
                    val supportJsonArray : JSONArray
                    val smallTalkJsonArray: JSONArray
                    val wallpapersJsonArray: JSONArray?

                    try {
                        idolsJsonArray = response.getJSONArray("idols")
                        articlesJsonArray = response.getJSONArray("articles")
                        supportJsonArray = response.getJSONArray("supports")
                        smallTalkJsonArray = response.getJSONArray("articlev2s")
                        wallpapersJsonArray = response.optJSONArray("wallpapers")

                        val idolsSize = idolsJsonArray?.length()
                        val articlesSize = articlesJsonArray?.length()
                        val supportSize = supportJsonArray?.length()
                        val smallTalkSize = smallTalkJsonArray?.length()
                        val wallpapersSize = wallpapersJsonArray?.length()

                        if (isFirstSearching && (idolsSize == 0 && articlesSize == 0 && smallTalkSize == 0)) {
                            mNoSearchResultView.visibility = View.VISIBLE
                            mSearchedRecyclerView.visibility = View.GONE
                        } else {
                            if ((mSearchedArticleList.isEmpty() && articlesSize == 0)
                                && (mSearchedIdolList.isEmpty() && idolsSize == 0)
                                && (mSearchedSmallTalkList.isEmpty() && smallTalkSize == 0)) {
                                mNoSearchResultView.visibility = View.VISIBLE
                                mSearchedRecyclerView.visibility = View.GONE
                            } else {
                                mNoSearchResultView.visibility = View.GONE
                                mSearchedRecyclerView.visibility = View.VISIBLE
                            }

                            if (idolsSize != 0 && idolsSize != null) {
                                // article만 가져오는 경우에는 idol 리스트 업데이트 X
                                mIdols.clear()
                                mStorageSearchedIdolList.clear()
                                mSearchedIdolList.clear()

                                for (i in 0 until idolsSize) {
                                    val obj = idolsJsonArray.getJSONObject(i)
                                    val model = gson.fromJson(obj.toString(), IdolModel::class.java)

                                    mIdols[model.getName(this@SearchResultActivity)] = model
                                    mStorageSearchedIdolList.add(model)
                                }

                                // 즐겨찾기 여부
                                favoritesResponse?.let { nonNullResponse ->
                                    setFavorites(nonNullResponse)
                                }

                                // 최애 여부
                                val mostIdolModel = mAccount?.most
                                if (mostIdolModel != null) {
                                    val exist = mStorageSearchedIdolList.find { it.getId() == mostIdolModel.getId() }
                                    exist?.isMost = true
                                }

                                // 처음엔 3명만 보여줌
                                if (idolsSize < 3) {
                                    mSearchedIdolList.addAll(mStorageSearchedIdolList)
                                    mStorageSearchedIdolList.clear()
//                                        mSearchedRecyclerView.adapter.notifyItemRangeChanged(0, idolsSize)
                                } else {

                                    //서포트 리스트가  업데이트가 되어야 하는 상황이라면 서포트 전체 리스트를  뿌려준다.
                                    if(isIdolListViewMoreClicked){
                                        for (i in 0 until mStorageSearchedIdolList.size) {
                                            mSearchedIdolList.add(mStorageSearchedIdolList[i])
                                        }
                                    }else{
                                        for (i in 0 until 3) {
                                            mSearchedIdolList.add(mStorageSearchedIdolList[i])
                                        }
                                        for (i in 0 until 3) {
                                            mStorageSearchedIdolList.removeAt(0)
                                        }
                                    }
//                                        mSearchedRecyclerView.adapter.notifyItemRangeChanged(0, 3)
                                }

                            }

                            if (isFirstSearching) {
                                mSearchedWallpapersList.clear()
                            }
                            if(wallpapersSize  != null && wallpapersSize != 0) {
                                for (i in 0 until wallpapersSize) {
                                    val obj = wallpapersJsonArray.getJSONObject(i)
                                    val model = gson.fromJson(obj.toString(), WallpaperModel::class.java)
                                    mSearchedWallpapersList.add(model)
                                }
                            }

                            //서포트 리스트  gson
                            if (supportSize != 0 && supportSize != null) {
                                mStorageSearchedSupportList.clear()
                                mSearchedSupportList.clear()
                                val gson = IdolGson.getInstance(false) // 서포트는 UTC

                                for (i in 0 until supportSize) {
                                    val obj = supportJsonArray.getJSONObject(i)
                                    val model = gson.fromJson(
                                        obj.toString(),
                                        SupportListModel::class.java
                                    )
                                    mStorageSearchedSupportList.add(model)
                                }

                                // 검색된  서포트 사이즈  3미만이면  3개만 보여지므로,
                                // storage리스트에 있는 모든 값을  supportlist에  넣어준다.
                                //그외  3개 이상은
                                if (supportSize < 3) {
                                    mSearchedSupportList.addAll(mStorageSearchedSupportList)
                                    mStorageSearchedSupportList.clear()
                                } else {

                                    //서포트 리스트가  업데이트가 되어야 하는 상황이라면 서포트 전체 리스트를  뿌려준다.
                                    if(needToUpdateSupport){
                                        for (i in 0 until mStorageSearchedSupportList.size) {
                                            mSearchedSupportList.add(mStorageSearchedSupportList[i])
                                        }
                                    }else{
                                        for (i in 0 until 3) {
                                            mSearchedSupportList.add(mStorageSearchedSupportList[i])
                                        }
                                        //위에서 앞에 3개 넣어줬으므로,  앞에 3개를  storage에서 지워준다.
                                        //앞에 3개 이외 데이터들은 나중에  더보기 사용시  보여주어야 함으로,  납둠.
                                        for (i in 0 until 3) {
                                            mStorageSearchedSupportList.removeAt(0)
                                        }
                                    }

                                }

                            }

                            //아이돌 게시판 게시글 리스트.
                            if (smallTalkSize != 0 && smallTalkSize != null) {

                                smallTalkListOffset = response.optInt("articlev2_offset")
                                smallTalkListTotal = response.optInt("articlev2_total")
                                smallTalkListLimit = response.optInt("articlev2_limit")

                                val nextSmallTalkTotal =
                                    smallTalkListOffset + smallTalkListLimit

                                if (nextSmallTalkTotal < smallTalkListTotal) {
                                    isSmallTalkViewMore = true
                                }

                                smallTalkListOffset += smallTalkSize


                                mSearchedSmallTalkList.clear()

                                for (i in 0 until smallTalkSize) {
                                    val obj = smallTalkJsonArray.getJSONObject(i)
                                    val model =
                                        gson.fromJson(obj.toString(), ArticleModel::class.java)

                                    if (UtilK.isArticleNotReported(
                                            this@SearchResultActivity,
                                            model.id
                                        )
                                    ) {
                                        mSearchedSmallTalkList.add(model)
                                    }
                                }
                            }

                            //아티클 리스ㅡ gson
                            if (articlesSize != 0 && articlesSize != null) {
                                for (i in 0 until articlesSize) {
                                    val obj = articlesJsonArray.getJSONObject(i)
                                    val model = gson.fromJson(obj.toString(), ArticleModel::class.java)
                                    if(UtilK.isArticleNotReported(this@SearchResultActivity, model.id)) {
                                        mSearchedArticleList.add(model)
                                    }
                                }
                            }
                        }

                        mSearchedAdapter.updateSearchedData(
                            searchedSmallTalkList = mSearchedSmallTalkList,
                            searchedArticleList = mSearchedArticleList,
                            searchedIdolList = mSearchedIdolList,
                            searchedSupportList = mSearchedSupportList,
                            storageSearchedIdolList = mStorageSearchedIdolList,
                            storageSearchedSupportList = mStorageSearchedSupportList,
                            searchedWallpaperList = mSearchedWallpapersList,
                            mKeyword = keywordList,
                            isSmallTalkViewMore = isSmallTalkViewMore
                        )

                        if (isFirstSearching) {
                            mSearchedRecyclerView.scrollToPosition(0)
                            Util.closeProgress()
                        }

                        isFirstSearching = false
                        isLoading = false

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, {
                    Util.closeProgress()
                    Toast.makeText(this@SearchResultActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun loadMoreSmallTalk(
        keyword: String?,
        offset: Int
    ) {

        Util.showProgress(this, true)
        lifecycleScope.launch {
            searchRepository.search(
                keyword,
                "articlev2",
                offset,
                10,
                { response ->
                    Util.closeProgress()

                    try {

                        val gson = IdolGson.getInstance(true)
                        val smallTalkJsonArray = response.optJSONArray("articlev2s")

                        val smallTalkSize = smallTalkJsonArray?.length()

                        if (smallTalkSize != 0 && smallTalkSize != null) {

                            smallTalkListOffset = response.optInt("articlev2_offset")
                            smallTalkListTotal = response.optInt("articlev2_total")
                            smallTalkListLimit = response.optInt("articlev2_limit")

                            val nextSmallTalkTotal = smallTalkListOffset + smallTalkListLimit

                            isSmallTalkViewMore = nextSmallTalkTotal < smallTalkListTotal

                            smallTalkListOffset += smallTalkSize


                            for (i in 0 until smallTalkSize) {
                                val obj = smallTalkJsonArray.getJSONObject(i)
                                val model = gson.fromJson(obj.toString(), ArticleModel::class.java)

                                if (UtilK.isArticleNotReported(this@SearchResultActivity, model.id)) {
                                    mSearchedSmallTalkList.add(model)
                                }
                            }
                        }

                        mSearchedAdapter.updateSearchedData(
                            searchedSmallTalkList = mSearchedSmallTalkList,
                            searchedArticleList = mSearchedArticleList,
                            searchedIdolList = mSearchedIdolList,
                            searchedSupportList = mSearchedSupportList,
                            storageSearchedIdolList = mStorageSearchedIdolList,
                            storageSearchedSupportList = mStorageSearchedSupportList,
                            searchedWallpaperList = mSearchedWallpapersList,
                            mKeyword = keywordList,
                            isSmallTalkViewMore = isSmallTalkViewMore
                        )


                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                {
                    Util.closeProgress()
                    Toast.makeText(
                        this@SearchResultActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()

                }
            )
        }
    }

    private fun updateMost(item: IdolModel?, button: CompoundButton) {

        val account = IdolAccount
                .getAccount(this@SearchResultActivity)

        lifecycleScope.launch {
            usersRepository.updateMost(
                userResourceUri = account?.userResourceUri!!,
                idolResourceUri = item?.resourceUri,
                listener = { response ->
                    if (response.optBoolean("success")
                        // 160414 account.getUserModel()이 null인 경우가 있음
                        && account != null && account.hasUserInfo()) {
                        var isFirst = false
                        var preGroupId = 0

                        if (account.most == null) {
                            isFirst = true
                        } else {
                            val preMost = mIdols[account.most?.getName(this@SearchResultActivity)]
                            if (preMost != null) {
                                preGroupId = preMost.groupId
                                preMost.isMost = false
                            }
                        }

                        if (item != null) {
                            mIdols[item.getName(this@SearchResultActivity)]?.apply {
                                isMost = true
                                isFavorite = true
                            }
                            account.userModel?.most = item
                            mAccount?.userModel?.most = item
                            // 최애 변경 후 하트 표시가 제대로 안나와서
                            for (i in mSearchedIdolList) {
                                i.isMost = i.getId() == item.getId()
                            }

                            val favoriteId = response.optInt("favorite_id", -1)
                            if (favoriteId != -1) {
                                mtempFavorites[item.getId()] = favoriteId
                            }
                        } else {
                            for (i in mSearchedIdolList) {
                                i.isMost = false
                            }

                            account.userModel?.most = null
                            mAccount?.userModel?.most = null
                        }
                        mSearchedRecyclerView.adapter?.notifyDataSetChanged()

                        //뉴프렌즈를 한번이라도 봤고, 친구추가 API가 안막혔을경우이다( 어워즈 기간 친구추가 API가 심하게 호출되서 해당 friendApiBlock로 구분해줌).
                        if (Util.getPreferenceBool(this@SearchResultActivity, Const.PREF_SHOW_SET_NEW_FRIENDS, true)
                            && !"F".equals(ConfigModel.getInstance(this@SearchResultActivity).friendApiBlock, ignoreCase = true)) {
                            if (isFirst || (item != null && (if( BuildConfig.CELEB ) true else preGroupId != item.groupId) )) {
                                Util.setPreference(this@SearchResultActivity,
                                    Const.PREF_SHOW_SET_NEW_FRIENDS,
                                    false)

                                Util.showDefaultIdolDialogWithBtn2(this@SearchResultActivity,
                                    getString(R.string.new_friends),
                                    getString(R.string.apply_new_friends_desp),
                                    R.string.yes,
                                    R.string.no,
                                    true,false,
                                    {
                                        Util.closeIdolDialog()
                                        startActivity(NewFriendsActivity.createIntent(this@SearchResultActivity))
                                    },
                                    { Util.closeIdolDialog() })
                            }
                        }
                    } else {
                        button.isChecked = false
                        UtilK.handleCommonError(this@SearchResultActivity, response)
                    }
                    Util.closeProgress(1000)
                    Handler().postDelayed({ accountManager.fetchUserInfo(this@SearchResultActivity) }, 1000)
                    userFavoriteCange = true

                    idolSoloFinalId?.let {
                        ChatRoomList.getInstance(this@SearchResultActivity).deleteRoomWithIdolId(it) {
                            idolSoloFinalId = null
                        }
                    }
                    if(!BuildConfig.CELEB) {
                        idolGroupFinalId?.let {
                            ChatRoomList.getInstance(this@SearchResultActivity).deleteRoomWithIdolId(it) {
                                idolGroupFinalId = null
                            }
                        }
                    }
                },
                errorListener = { throwable ->
                    button.isChecked = false
                    Util.closeProgress(1000)
                    Toast.makeText(this@SearchResultActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                })
        }
    }

    private fun setFavorites(response: JSONObject) {
        try {
            val gson = IdolGson.getInstance()
            mFavorites.clear()
            mtempFavorites.clear()

            val array = response.getJSONArray("objects")

            // 투표순 정렬
            val idols = ArrayList<IdolModel>()

            for (i in 0 until array.length()) {
                val model = gson.fromJson(array.getJSONObject(i).toString(),
                        FavoriteModel::class.java)
                val idol = model.idol ?: continue
                mFavorites[idol.getId()] = model.id
//                model.idol.setLocalizedName(this@SearchResultActivity)
                if (idol.isViewable == "N") {
                    val tmp = idol
//                    tmp.setLocalizedName(this@SearchResultActivity)
                    if (mAccount?.most != null) {
//                        mAccount.most!!.setLocalizedName(this@SearchResultActivity)
                        if (mAccount?.most!!.getName(this@SearchResultActivity) == tmp.getName(this@SearchResultActivity))
                            tmp.isMost = true
                    }
                    mIdols[tmp.getName(this@SearchResultActivity)] = tmp
                }
                val existModel = mIdols[idol.getName(this@SearchResultActivity)]
                // 제외된 아이돌도 보이게
                if (existModel != null) {
                    idol.isMost = existModel.isMost // 검색화면에서 최애 설정상태가 안보여서
                    // favorites/self를 캐시하고 있어서 투표수를 idols 응답으로 갱신
                    idol.heart = existModel.heart
                }
                idol.isFavorite = true
                idols.add(idol)
            }

            mStorageSearchedIdolList.forEach { searchedIdol ->
                val matchedIdol = idols.find { it.getId() == searchedIdol.getId() }
                if (matchedIdol != null) searchedIdol.isFavorite = true
            }

            mtempFavorites.putAll(mFavorites)
        } catch (e: JSONException) {
        }

    }

    private fun loadFavorites() {
        val response = ApiCacheManager.getInstance().getCache(Const.KEY_FAVORITE)
        if (response == null) {
            lifecycleScope.launch {
                favoritesRepository.getFavoritesSelf(
                    { response ->
                        Util.closeProgress()
                        if (response.optBoolean("success")) {
                            loadSearchResult(mKeyword, null, 0, 50, response,false)
                            // api caching
                            ApiCacheManager.getInstance()
                                .putCache(Const.KEY_FAVORITE, response, (60000 * 60).toLong())
                        } else {
                            UtilK.handleCommonError(this@SearchResultActivity, response)
                        }
                    }, {
                        Util.closeProgress()
                        Toast.makeText(this@SearchResultActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else {
            loadSearchResult(mKeyword, null, 0, 50, response,false)
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.ARTICLE_REPORT.value -> {
                if (resultCode == ResultCode.REPORTED.value) {
                    val position = data!!.getIntExtra(PARAM_ARTICLE_POSITION, -1)

                    if (position >= 0) {
                        val model = mSearchedArticleList[position]
                        model.reportCount = model.reportCount + 1

                        val account = IdolAccount.getAccount(this)
                        if (account != null) {
                            val prefs = PreferenceManager
                                    .getDefaultSharedPreferences(this)
                            val editor = prefs.edit()
                            val reportedArticles = prefs.getStringSet(
                                    account.email + "_did_report",
                                    HashSet())
                            reportedArticles!!.add(model.resourceUri)
                            editor.putStringSet(account.email + "_did_report",
                                    reportedArticles).apply()
                        }
                        mSearchedArticleList.removeAt(position)
                        mSearchedRecyclerView.adapter?.notifyDataSetChanged()

                        //article 차단 목록 추가
                        UtilK.addArticleReport(this, model.id)
                    }
                }
            }
            RequestCode.ARTICLE_REMOVE.value -> {
                Util.closeProgress()
                if (resultCode == ResultCode.REMOVED.value) {
                    //loadResource();
                    val position = data!!.getIntExtra(PARAM_ARTICLE_POSITION, -1)
                    val model = mSearchedArticleList[position]
                    // 글 삭제하면 게시물 안보이게 다시 복원
                    //            model.setDeleted();
                    mSearchedArticleList.removeAt(position)
                    mSearchedRecyclerView.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_SUPPORT_UPDATE->{// 서포트 리스트 업데이트
                val response = ApiCacheManager.getInstance().getCache(Const.KEY_FAVORITE)

                //아이돌 리스트  더보기 눌렀을때 여부 체크  -> recyclerview update할때 업데이트 될 리스트 체크용
                isIdolListViewMoreClicked = mSearchedIdolList.size>3

                if(mSearchedSupportList.size>3){// 더보기가 눌린 상황이라면,
                    loadSearchResult(mKeyword, null, 0, 50, response,true)
                }else{
                    loadSearchResult(mKeyword, null, 0, 50, response,false)
                }
            }

            RequestCode.ARTICLE_COMMENT.value -> {
                if (data != null) {
                    val article = data.getSerializableData<ArticleModel>(Const.EXTRA_ARTICLE)
                    val position = getArticlePosition(article?.id ?: return)

                    when (resultCode) {
                        ResultCode.REMOVED.value -> {
                            if (position >= 0) {
                                mSearchedArticleList.removeAt(position)
                                mSearchedRecyclerView.adapter?.notifyItemRemoved(position)
                            }
                        }
                        ResultCode.COMMENT_REMOVED.value,
                        ResultCode.EDITED.value, ResultCode.COMMENTED.value -> {
                            if (position >= 0) {
                                mSearchedArticleList[position] = article
                                mSearchedRecyclerView.adapter?.notifyItemChanged(mSearchedAdapter.getLastPosition(SearchedAdapter.TYPE_ARTICLE) + position)
                            }
                        }
                        ResultCode.ARTICLE_LIKE_EXCEPTION.value -> {
                            if (article.isUserLike != article.isUserLikeCache) { // 들어갈 때 내 UserLike와, setResult에서 보내온 Article.userLike가 다르다면 post
                                postSmallTalkLike(article)
                            }
                        }
                        ResultCode.UPDATE_LIKE_COUNT.value -> {
                            article.resourceUri?.let { loadArticleResource(it) } ?: doSearch()
                        }
                    }
                }
            }
            RequestCode.ARTICLE_EDIT.value -> {
                if (resultCode == ResultCode.EDITED.value) {
                    val resourceUri = data?.getStringExtra("resource_uri")
                    resourceUri?.let { loadArticleResource(it) } ?: doSearch()
                    return
                }
            }

            //검색화면에서  커뮤니티로 들어가서  투표후  top3가 바뀐경우  콜백받아  새롭게  데이터 서버에 요청해 update해준다.
            Const.REQUEST_TOP3_UPDATED  ->{
                if(resultCode == Const.RESULT_TOP3_UPDATED){
                    doSearch()
                }
            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // 특정 아티클 업데이트용
    private fun loadArticleResource(resourceUri: String) {
        lifecycleScope.launch {
            articlesRepository.getArticle(
                resourceUri,
                { response ->
                    try {
                        val gson = IdolGson.getInstance(true)

                        val model = gson.fromJson(response.toString(), ArticleModel::class.java)


                        val searchedArticle = mSearchedArticleList.find { it.resourceUri == resourceUri }
                        if(searchedArticle != null) {
                            val articlePosition = mSearchedArticleList.indexOfFirst { it.resourceUri == searchedArticle.resourceUri }
                            mSearchedArticleList.remove(searchedArticle)
                            mSearchedArticleList.add(articlePosition, model)

                            mSearchedRecyclerView.adapter?.notifyItemChanged(mSearchedAdapter.getLastPosition(SearchedAdapter.TYPE_ARTICLE) + articlePosition, "like")
                        }

                        val searchedSmallTalk = mSearchedSmallTalkList.find { it.resourceUri == resourceUri }
                        if(searchedSmallTalk != null) {
                            val searchedPosition = mSearchedSmallTalkList.indexOfFirst { it.resourceUri == searchedSmallTalk.resourceUri }
                            mSearchedSmallTalkList.remove(searchedSmallTalk)
                            mSearchedSmallTalkList.add(searchedPosition, model)
                            mSearchedRecyclerView.adapter?.notifyItemChanged(mSearchedIdolList.size + mSearchedSupportList.size + searchedPosition)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, {
                    Toast.makeText(this@SearchResultActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    //서포트 리스트 관련 버튼 이벤트
    override fun onSupportButtonClick(view: View, supportStatus: Int, model: SupportListModel) {

        when(view.id){
            R.id.view_more_support_list ->{
                setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,
                        "search_more_supportList")
                mSearchedSupportList.addAll(mStorageSearchedSupportList)
                mStorageSearchedSupportList.clear()
                mSearchedRecyclerView.adapter?.notifyDataSetChanged()

            }
            else -> {
                if(supportStatus == SUPPORT_ING){//진행중일때 -> 서포트 상세화면으로
                    startActivity(SupportDetailActivity.createIntent(this.applicationContext,model.id))
                }else if(supportStatus == SUPPORT_SUCCESS){
                    // TODO: 2020/11/24 서포트 인증샷 화면으로 들어가는 로직  추가될 예정
                    startActivityForResult(SupportPhotoCertifyActivity.createIntent(this.applicationContext,getSupportInfo(model)),REQUEST_SUPPORT_UPDATE)
                }
            }
        }
    }

    private fun getSupportInfo(supportListModel: SupportListModel):String{
        //서포트  관련 필요 정보를  서포트 인증샷 화면에 json 화 시켜서 넘겨준다.

        val supportInfo = JSONObject()

        if(supportListModel.idol.getName(this).contains("_")){
            supportInfo.put("name", Util.nameSplit(this, supportListModel.idol)[0])
            supportInfo.put("group",Util.nameSplit(this, supportListModel.idol)[1])
        }else{
            supportInfo.put("name", supportListModel.idol.getName(this))
        }
        supportInfo.put("support_id",supportListModel.id)
        supportInfo.put("title",supportListModel.title)
        supportInfo.put("profile_img_url",supportListModel.image_url)

        return supportInfo.toString()
    }


    override fun onIdolButtonClick(model: IdolModel, v: View?, position: Int) {
        when (v?.id) {
            R.id.cl_searched_idol,
            R.id.communityButton -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.SEARCH_COMMUNITY.actionValue,
                    GaAction.SEARCH_COMMUNITY.label
                )
                startActivityForResult(CommunityActivity.createIntent(this, model, CATEGORY_COMMUNITY),Const.REQUEST_TOP3_UPDATED)
            }
            R.id.idolTalkButton -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.SEARCH_TALK.actionValue,
                    GaAction.SEARCH_TALK.label
                )
                startActivityForResult(CommunityActivity.createIntent(this, model, CATEGORY_IDOLTALK),Const.REQUEST_TOP3_UPDATED)
            }
            R.id.scheduleButton -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.SEARCH_SCHEDULE.actionValue,
                    GaAction.SEARCH_SCHEDULE.label
                )
                startActivityForResult(CommunityActivity.createIntent(this, model, CATEGORY_SCHEDULE),Const.REQUEST_TOP3_UPDATED)
            }
            R.id.viewMore -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.SEARCH_MORE.actionValue,
                    GaAction.SEARCH_MORE.label
                )
                isIdolListViewMoreClicked =true
                mSearchedIdolList.addAll(mStorageSearchedIdolList)
                mStorageSearchedIdolList.clear()
                mSearchedRecyclerView.adapter?.notifyDataSetChanged()
            }
            R.id.tv_help_set_most -> {
                Util.setPreference(this, Const.PREF_SHOW_SET_MOST_IN_SEARCH, false)
                val textView=v.findViewById<TextView>(R.id.tv_help_set_most)
                textView.visibility=View.GONE
                mSearchedRecyclerView.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun smallTalkItemClicked(
        articleModel: ArticleModel,
        buttonStatus: Int,
        position: Int
    ) {
        if (buttonStatus == SearchedAdapter.VIEW_MORE_BUTTON_STATUS) {
            loadMoreSmallTalk(
                keyword = mKeyword, offset = smallTalkListOffset
            )
        } else {
            startActivityForResult(
                NewCommentActivity.createIntent(
                    this, articleModel,
                    position, false, NewCommentAdapter.TYPE_ARTICLE,
                    tagName = articleModel.idol?.getName(this@SearchResultActivity) ?: ""
                ),
                RequestCode.ARTICLE_COMMENT.value
            )
        }
    }

    override fun onArticleButtonClick(model: ArticleModel, v: View?, position: Int) {
        when (v?.id) {
            R.id.footer_comment,
            R.id.ll_comment_count,
            R.id.comment_count_icon,
            R.id.comment_count -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.SEARCH_COMMENT.actionValue,
                    GaAction.SEARCH_COMMENT.label
                )
                startActivityForResult(NewCommentActivity.createIntent(this, model, position, false),
                        RequestCode.ARTICLE_COMMENT.value)
            }
            R.id.btn_share -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.SEARCH_SHARE.actionValue,
                    GaAction.SEARCH_SHARE.label
                )
                val params = listOf(LinkStatus.ARTICLES.status, model.id.toString())
                val url = LinkUtil.getAppLinkUrl(this@SearchResultActivity, params = params)
                UtilK.linkStart(context = this, url = url)
            }
            R.id.iv_view_more -> {
                UtilK.clickMore(this, model) { showEdit, showRemove, showReport, showShare ->
                    val tag = "article"
                    val sheet = ArticleViewMoreBottomSheetFragment.newInstance(
                        showEdit,
                        showRemove,
                        showReport,
                        model.isMostOnly != "Y",
                        onClickEdit = {
                            clickEdit(model)
                        },
                        onClickDelete = {
                            clickRemove(model, position)
                        },
                        onClickReport = {
                            clickReport(model, position)
                        }) {
                        setUiActionFirebaseGoogleAnalyticsActivity(
                            GaAction.SEARCH_SHARE.actionValue,
                            GaAction.SEARCH_SHARE.label
                        )
                        val params = listOf(LinkStatus.ARTICLES.status, model.id.toString())
                        val url = LinkUtil.getAppLinkUrl(this@SearchResultActivity, params = params)
                        UtilK.linkStart(context = this, url = url)
                    }

                    val oldFrag = supportFragmentManager.findFragmentByTag(tag)
                    if (oldFrag == null) {
                        sheet.show(supportFragmentManager, tag)
                    }
                }
            }
            R.id.photo, R.id.name -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.SEARCH_FEED.actionValue,
                    GaAction.SEARCH_FEED.label
                )
                startActivity(FeedActivity.createIntent(this, model.user))
            }
            R.id.icon_secret -> {
                Util.showDefaultIdolDialogWithBtn1(this, null,
                    getString(if(BuildConfig.CELEB) R.string.actor_lable_show_private else R.string.lable_show_private)) {
                    Util.closeIdolDialog()
                }
            }
            R.id.footer_like -> {
                postSmallTalkLike(model)
            }
            R.id.view_translate -> {
                // 번역하기
                clickTranslate(model, position)
            }
        }
    }

    private fun clickTranslate(item: ArticleModel, position: Int) {
        setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "community_translate") // 임시값 (확정 후 변경 예정)

        translateArticle(
            item,
            position,
            mSearchedAdapter,
            articlesRepository
        )
    }

    override fun onCheckedChanged(button: CompoundButton, isChecked: Boolean, item: IdolModel) {
        if (mAccount?.heart == Const.LEVEL_MANAGER && button.id == R.id.btn_most) {
            val color = "#" + Integer.toHexString(ContextCompat.getColor(this, R.color.main)).substring(2)

            val managerWarnmsg = HtmlCompat.fromHtml(getString(if(BuildConfig.CELEB) R.string.actor_msg_manager_warning else R.string.msg_manager_warning)
                    + "<br>"
                    + "<FONT color="
                    + color
                    + ">"
                    + "<br><b>"
                    + getString(R.string.msg_continue)
                    + "</b></FONT>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
            Util.showDefaultIdolDialogWithBtn2(this, getString(R.string.lable_manager_warning), managerWarnmsg.toString(),
                    {
                        Util.closeIdolDialog()
                        onCheckedChanged(button, isChecked, item, false)
                    }, {
                Util.closeIdolDialog()
                button.isChecked = false
            })
        } else {
            onCheckedChanged(button, isChecked, item, false)
        }

        // 즐찾에 변경 있으면
        ApiCacheManager.getInstance().clearCache(Const.KEY_FAVORITE)
    }

    private fun onCheckedChanged(button: CompoundButton,
                                 isChecked: Boolean,
                                 item: IdolModel,
                                 forceChange: Boolean) {

        when (button.id) {
            R.id.btn_favorite -> {
                if (!button.isEnabled) {
                    return
                }
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.SEARCH_FAVORITE.actionValue,
                    GaAction.SEARCH_FAVORITE.label
                )
                button.isEnabled = false
                if (isChecked) {
                    lifecycleScope.launch {
                        favoritesRepository.addFavorite(
                            item.getId(),
                            { response ->
                                if (response.optBoolean("success", true)) {
                                    try {
                                        // 여기 좀 이상함.. 응답은 {"gcode":0,"success":true} => 서버 수정하자
                                        mtempFavorites[response.getInt("idol_id")] = response.getInt("id")
                                    } catch (e: JSONException) {
                                        e.printStackTrace()
                                    }

                                    item.isFavorite = isChecked
                                    mSearchedRecyclerView.adapter?.notifyDataSetChanged()
                                } else {
                                    button.isChecked = !isChecked
                                    val responseMsg = ErrorControl.parseError(this@SearchResultActivity, response)
                                    if (responseMsg != null) {
                                        Toast.makeText(this@SearchResultActivity, responseMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                button.isEnabled = true
                            }, {
                                button.isChecked = !isChecked
                                button.isEnabled = true
                                Toast.makeText(this@SearchResultActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } else {
                    // 여기도 좀 이상함... mtempFavorites에 넣고 빼는데 mFavorites에서 찾으면 어떡하지?? 검색 후 즐겨찾기에 추가->서버에 반영->바로 즐겨찾기 삭제->아래에 걸려서 아무 동작도 안함-> 서버에 남아있음
                    //				if (mFavorites.get(item.getId()) == null) {
                    //					button.setEnabled(true);
                    //					return;
                    //				}
                    if (mtempFavorites[item.getId()] == null) {
                        button.isEnabled = true
                        return
                    }
                    //int favoriteId = mFavorites.get(item.getId());
                    val favoriteId = mtempFavorites[item.getId()]
                    lifecycleScope.launch {
                        favoritesRepository.removeFavorite(
                            favoriteId!!,
                            { response ->
                                if (response.optBoolean("success")) {
                                    mtempFavorites.remove(item.getId())
                                    item.isFavorite = false
                                    mSearchedRecyclerView.adapter?.notifyDataSetChanged()
                                } else {
                                    button.isChecked = !isChecked
                                    UtilK.handleCommonError(this@SearchResultActivity, response)
                                }
                                button.isEnabled = true
                            }, {
                                button.isEnabled = true
                                button.isChecked = !isChecked
                                Toast.makeText(this@SearchResultActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            R.id.btn_most -> {
                hideSoftKeyboard()

                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.SEARCH_MOST.actionValue,
                    GaAction.SEARCH_MOST.label
                )

                val newMost = if (!button.isChecked) null else item
                UtilK.showChangeMostDialog(this,
                    newMost,
                    sharedAppState,
                    {
                        idolSoloFinalId = it.first
                        idolGroupFinalId = it.second
                        updateMost(newMost, button)
                    })
                {
                    button.isChecked = item.isMost
                }
            }
        }
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val resolveInfo = packageManager.queryIntentActivities(intent, 0)
        return !resolveInfo.isNullOrEmpty()
    }

    private fun getArticlePosition(articleId: String): Int {
        val position = mSearchedArticleList.withIndex().find { it.value.id == articleId }?.index
        if (position != null) return position
        return -1
    }

    fun clickRemove(model: ArticleModel, position: Int){
        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.SEARCH_DELETE.actionValue,
            GaAction.SEARCH_DELETE.label
        )
        Util.showProgress(this)
        val removeDlg = ArticleRemoveDialogFragment
            .getInstance(model, position)
        removeDlg.setActivityRequestCode(RequestCode.ARTICLE_REMOVE.value)
        removeDlg.show(supportFragmentManager, "remove")
    }

    fun clickEdit(model: ArticleModel){
        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.SEARCH_EDIT.actionValue,
            GaAction.SEARCH_EDIT.label
        )

        try {
            if (Const.FEATURE_WRITE_RESTRICTION) {
                // 집계시간에는 수정도 불가
                lifecycleScope.launch {
                    usersRepository.isActiveTime(
                        { response ->
                            if (response.optBoolean("success")) {
                                if (response.optString("active") == Const.RESPONSE_Y) {
                                    val intent = WriteArticleActivity.createIntent(this@SearchResultActivity, model.idol)
                                    intent.putExtra(Const.EXTRA_ARTICLE, model)
                                    startActivityForResult(intent, RequestCode.ARTICLE_EDIT.value)
                                } else {
                                    val start = Util.convertTimeAsTimezone(response.optString("begin"))
                                    val end = Util.convertTimeAsTimezone(response.optString("end"))
                                    val unableUseTime = String.format(getString(R.string.msg_unable_use_write), start, end)

                                    Util.showIdolDialogWithBtn1(this@SearchResultActivity, null, unableUseTime) { Util.closeIdolDialog() }
                                }
                            } else { // success is false!
                                UtilK.handleCommonError(this@SearchResultActivity, response)
                            }
                        }, {
                            Toast.makeText(this@SearchResultActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                val intent = WriteArticleActivity.createIntent(this@SearchResultActivity, model.idol)
                intent.putExtra(Const.EXTRA_ARTICLE, model)
                startActivityForResult(intent, RequestCode.ARTICLE_EDIT.value)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun clickReport(model: ArticleModel, position: Int){
        val account = IdolAccount.getAccount(this)
        if (account == null && Util.mayShowLoginPopup(this)) return

        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.SEARCH_REPORT.actionValue,
            GaAction.SEARCH_REPORT.label
        )
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val reportedArticles = prefs.getStringSet(account?.email ?: "" + "_did_report", HashSet())
        if (reportedArticles!!.contains(model.resourceUri)) {
            Toast.makeText(this,
                R.string.failed_to_report__already_reported,
                Toast.LENGTH_SHORT).show()
            return
        }

        // config/self 제거하고 미리 받아놓은 값 사용
        val reportHeart = ConfigModel.getInstance(this).reportHeart
        val report = ReportDialogFragment.getInstance(model, position)
        val articleIdol = model.idol

        // 하트 차감 수가 0일 때
        if (reportHeart == 0
            // 지식돌, 자유게시판에서는 무료로 신고 가능
            || articleIdol?.getId() == Const.IDOL_ID_KIN
            || articleIdol?.getId() == Const.IDOL_ID_FREEBOARD
            // 내 커뮤이면서
            || (account?.userModel?.most != null && account?.userModel?.most?.getId() == articleIdol?.getId())
            // 최애가 없는 사람 글과
            && (model.user != null
                    && (model.user.most == null
                    // 커뮤니티가 최애가 아닌 사람의 글도 무료로 신고 가능
                    || (model.user.most != null
                    && model.user.most?.getId() != articleIdol?.getId())))) {
            report.setMessage(HtmlCompat.fromHtml(getString(R.string.warning_report_hide_article),
                HtmlCompat.FROM_HTML_MODE_LEGACY))
        } else {

            if (reportHeart > 0) {
                val color = "#" + Integer.toHexString(ContextCompat.getColor(this@SearchResultActivity, R.color.main)).substring(2)
                val msg = String.format(resources.getString(R.string.warning_report_lose_heart), "<FONT color=$color>$reportHeart</FONT>")
                val spanned = HtmlCompat.fromHtml(msg, HtmlCompat.FROM_HTML_MODE_LEGACY)
                report.setMessage(spanned)
            }
        }

        report.setActivityRequestCode(RequestCode.ARTICLE_REPORT.value)
        report.show(supportFragmentManager, "report")
    }

    private fun postSmallTalkLike(
        articleModel: ArticleModel?,
    ) {
        if (articleModel != null) {
            MainScope().launch {
                likeArticleUseCase(articleModel.id, articleModel.isUserLikeCache).collect { response ->
                    if( !response.success ) {
                        response.message?.let {
                            Toast.makeText(this@SearchResultActivity, it, Toast.LENGTH_SHORT).show()
                        }
                        return@collect
                    }

                    loadArticleResource(articleModel.resourceUri)
                }
            }
        }
    }

    companion object {
        const val PARAM_SEARCH_KEYWORD = "searchKeyword"
        const val REQUEST_SUPPORT_UPDATE =1101//서포트 리스트 업데이트
        var searchResultFinish = -1


        @JvmStatic
        fun createIntent(context: Context, keyword: String): Intent {
            val intent = Intent(context, SearchResultActivity::class.java)
            intent.putExtra(PARAM_SEARCH_KEYWORD, keyword)
            return intent
        }
    }
}
