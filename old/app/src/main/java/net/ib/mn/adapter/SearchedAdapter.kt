package net.ib.mn.adapter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import kotlinx.coroutines.CoroutineScope
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.databinding.CommunityItemBinding
import net.ib.mn.databinding.ItemSearchWallpaperIdolBinding
import net.ib.mn.databinding.ItemSearchedIdolBinding
import net.ib.mn.databinding.ItemSmallTalkBinding
import net.ib.mn.databinding.ItemSupportMainBinding
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.*
import net.ib.mn.smalltalk.viewholder.SmallTalkVH
import net.ib.mn.utils.*
import net.ib.mn.viewholder.ArticleViewHolder
import net.ib.mn.viewholder.IdolViewHolder
import net.ib.mn.viewholder.SearchedWallpaperIdolViewHolder
import net.ib.mn.viewholder.SupportViewHolder
import java.util.Calendar
import java.util.HashMap
import kotlin.collections.ArrayList


@UnstableApi
class SearchedAdapter(
    private val context: Context,
    private val mGlideRequestManager: RequestManager,
    private val mAccount: IdolAccount?,
    private var mKeyword: ArrayList<String>,
    private var searchedSmallTalkList: ArrayList<ArticleModel>,
    private var searchedIdolList: ArrayList<IdolModel>,
    private var storageSearchedIdolList: ArrayList<IdolModel>,
    private var searchedArticleList: ArrayList<ArticleModel>,
    private var searchedSupportList:ArrayList<SupportListModel>,
    private var storageSearchedSupportList: ArrayList<SupportListModel>,
    private var searchedWallpaperList: ArrayList<WallpaperModel>,
    private val onIdolButtonClick: (IdolModel, View, Int) -> Unit,
    private val onSupportButtonClick: (view:View,supportStatus:Int,model:SupportListModel) -> Unit,
    private val onCheckedChanged: (CompoundButton, Boolean, IdolModel) -> Unit,
    private val onArticleButtonClick: (ArticleModel, View, Int) -> Unit,
    private val smallTalkListener: SmallTalkListener,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val getIdolByIdUseCase: GetIdolByIdUseCase
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var isSmallTalkViewMore = false
    private var articlePhotoListener: ArticlePhotoListener? = null

    private val calendar = Calendar.getInstance()
    private val locale = LocaleUtil.getAppLocale(context)

    private val mapExpanded = HashMap<Int, Boolean>()

    fun setPhotoClickListener(articlePhotoListener: ArticlePhotoListener) {
        this.articlePhotoListener = articlePhotoListener
    }
    interface SmallTalkListener{
        fun smallTalkItemClicked(articleModel: ArticleModel, buttonStatus: Int, position: Int)

    }
    interface OnSupportClickListener {
        fun onSupportButtonClick(view: View,supportStatus :Int,model: SupportListModel)
    }

    interface OnIdolClickListener {
        fun onIdolButtonClick(model: IdolModel, v: View?, position: Int)
    }

    interface OnArticleClickListener {
        fun onArticleButtonClick(model: ArticleModel, v: View?, position: Int)
    }

    interface OnAdapterCheckedChangeListener {
        fun onCheckedChanged(button: CompoundButton, isChecked: Boolean, item: IdolModel)
    }

    // exoplayer
    private var player: ExoPlayer? = null
    lateinit var dataSourceFactory: DefaultDataSourceFactory
    lateinit var extractorsFactory: ExtractorsFactory

    init {
        initExoPlayer()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            //아이돌 리스트 관련  뷰홀더
            TYPE_IDOL ->{
                val itemSearchedIdol : ItemSearchedIdolBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_searched_idol,
                    parent,
                    false
                )
                return IdolViewHolder(itemSearchedIdol, context, mAccount, mGlideRequestManager, searchedIdolList, storageSearchedIdolList, onIdolButtonClick, onCheckedChanged)
            }
            //서포트 리스트 관련 뷰홀더
            TYPE_SUPPORT-> {
                val itemSupportMain : ItemSupportMainBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_support_main,
                    parent,
                    false
                )
                return SupportViewHolder(lifecycleScope, itemSupportMain, context, mGlideRequestManager, getIdolByIdUseCase, onSupportButtonClick)
            }
            TYPE_SMALL_TALK -> {
                val itemSmallTalk : ItemSmallTalkBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_small_talk,
                    parent,
                    false
                )
                return SmallTalkVH(itemSmallTalk, calendar, locale)

            }
            TYPE_WALLPAPER -> {
                val itemWallpaper: ItemSearchWallpaperIdolBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_search_wallpaper_idol,
                    parent,
                    false
                )
                return SearchedWallpaperIdolViewHolder(itemWallpaper, searchedWallpaperList.size, lifecycleScope, getIdolByIdUseCase)
            }
            //그외 아티클 관련 뷰홀더
            else -> {
                val itemSearchedArticle : CommunityItemBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.community_item,
                    parent,
                    false
                )
                return ArticleViewHolder(itemSearchedArticle,
                    context,
                    useTranslation = true,
                    viewType,
                    onArticleButtonClick,
                    articlePhotoListener,
                    searchedIdolList.size,
                    lifecycleScope = lifecycleScope,
                    mapExpandedForSearch = mapExpanded,
                    updateExpanded = { position, isExpanded ->
                        mapExpanded[position] = isExpanded
                        notifyItemChanged(position, "like")
                    })
            }
        }
    }

    override fun getItemCount(): Int {
        return searchedIdolList.size + searchedSupportList.size + searchedSmallTalkList.size + searchedArticleList.size + searchedWallpaperList.size
    }

    override fun getItemId(position: Int): Long {
        return position.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < getLastPosition(TYPE_IDOL) -> TYPE_IDOL
            position < getLastPosition(TYPE_SUPPORT) -> TYPE_SUPPORT
            position < getLastPosition(TYPE_WALLPAPER) -> TYPE_WALLPAPER
            position < getLastPosition(TYPE_SMALL_TALK) -> TYPE_SMALL_TALK
            else -> {
                val articleIndex = position - getLastPosition(TYPE_SMALL_TALK)
                val article = searchedArticleList[articleIndex]
                if (article.idol?.getId() == Const.IDOL_ID_KIN || article.idol?.getId() == Const.IDOL_ID_FREEBOARD) {
                    TYPE_SEARCH_FEED_KIN_BOARD
                } else {
                    TYPE_SEARCH_COMMUNITY_ARTICLE
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder, position, mutableListOf())
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        when (holder.itemViewType) {
            TYPE_IDOL -> {
                val searchedIdol = searchedIdolList[position]

                if(searchedIdolList.size>3){
                    storageSearchedIdolList.clear()
                }

                val isShowViewMore = storageSearchedIdolList.size > 0 && position == 2
                (holder as IdolViewHolder).apply {
                    bind(searchedIdol, isShowViewMore, position)
                }
            }
            TYPE_SUPPORT -> {
                val isShowViewMore = storageSearchedSupportList.size > 0 && position == searchedIdolList.size+2

                val searchedSupportModel = searchedSupportList[position - searchedIdolList.size]

                (holder as SupportViewHolder).apply {
                    bind(searchedSupportModel, isShowViewMore , searchedSupportList.size ,position - searchedIdolList.size)
                }
            }
            TYPE_SMALL_TALK -> {

                val isLastPosition =
                    searchedSmallTalkList.size > 0 && position == getLastPosition(TYPE_SMALL_TALK) - 1

                val smallTalkItemPosition =
                    position - getLastPosition(TYPE_WALLPAPER)

                (holder as SmallTalkVH).apply {
                    this.binding.viewMoreSmallTalkList.setOnClickListener{
                        smallTalkListener.smallTalkItemClicked(searchedSmallTalkList[smallTalkItemPosition], VIEW_MORE_BUTTON_STATUS, position)
                    }

                    bind(searchedSmallTalkList[smallTalkItemPosition], isSmallTalkViewMore, isLastPosition) {
                        smallTalkListener.smallTalkItemClicked(
                            searchedSmallTalkList[smallTalkItemPosition],
                            NORMAL_BUTTON_STATUS,
                            position
                        )
                    }
                }
            }
            TYPE_WALLPAPER -> {
                val wallpaperItemPosition = position - getLastPosition(TYPE_SUPPORT)
                (holder as SearchedWallpaperIdolViewHolder).apply {
                    bind(searchedWallpaperList[wallpaperItemPosition], wallpaperItemPosition)
                }
            }
            else -> {
                val searchedArticle =
                    searchedArticleList[position - getLastPosition(TYPE_SMALL_TALK)]
                if (payloads.contains("like")) {
                    (holder as? ArticleViewHolder)?.updateLike(searchedArticle)
                    return
                }

                (holder as ArticleViewHolder).apply {
                    bind(searchedArticle, IdolModel(), position, 0, false)
                }
            }
        }
    }

    //검색시에 검색데이터들 새롭게  업데이트
    fun updateSearchedData(searchedIdolList: ArrayList<IdolModel>,
                           storageSearchedIdolList: ArrayList<IdolModel>,
                           searchedArticleList: ArrayList<ArticleModel>,
                           searchedSupportList:ArrayList<SupportListModel>,
                           storageSearchedSupportList: ArrayList<SupportListModel>,
                           searchedWallpaperList: ArrayList<WallpaperModel>,
                           mKeyword: ArrayList<String>,
                           searchedSmallTalkList: ArrayList<ArticleModel>,
                           isSmallTalkViewMore :Boolean
    ){
        this.searchedIdolList = searchedIdolList
        this.storageSearchedIdolList = storageSearchedIdolList
        this.searchedArticleList = searchedArticleList
        this.searchedSupportList = searchedSupportList
        this.storageSearchedSupportList = storageSearchedSupportList
        this.mKeyword = mKeyword
        this.searchedSmallTalkList = searchedSmallTalkList
        this.isSmallTalkViewMore = isSmallTalkViewMore
        this.searchedWallpaperList = searchedWallpaperList
        notifyDataSetChanged()
    }

    fun getPlayer(): ExoPlayer {
        return player!!
    }


    private fun initExoPlayer() {
        if (Build.VERSION.SDK_INT < Const.EXOPLAYER_MIN_SDK) return

        if (player == null) {
            // 1. Create a default TrackSelector
            val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
            val trackSelector = DefaultTrackSelector(context)

            // 2. Create a default LoadControl
            val loadControl = DefaultLoadControl()

            // 3. Create the player
            val renderersFactory = DefaultRenderersFactory(context)
            player = ExoPlayer.Builder(context,renderersFactory)
                .setBandwidthMeter(bandwidthMeter)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build()
            player!!.volume = 0f

            //Measures bandwidth during playback. Can be null if not required.
            val bandwidthMeterA = DefaultBandwidthMeter.Builder(context).build()
            //Produces DataSource instances through which media data is loaded.
            dataSourceFactory = DefaultDataSourceFactory(context, "myloveidol/1.0", bandwidthMeterA)
            //Produces Extractor instances for parsing the media data.
            extractorsFactory = DefaultExtractorsFactory()

            player!!.addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    // 여기가 그나마 나중에 불려서 실제 재생 시작시와 가장 일치하는 듯..
                    Util.log("onRenderedFirstFrame ")
                    val intent = Intent(Const.VIDEO_READY_EVENT)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

                }

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                }

            })
        }
    }

    fun pausePlayer() {
        if (player != null) {
            player?.playWhenReady = false
        }
    }

    fun getLastPosition(type: Int): Int {
        return when (type) {
            TYPE_IDOL -> searchedIdolList.size
            TYPE_SUPPORT -> searchedIdolList.size + searchedSupportList.size
            TYPE_WALLPAPER -> searchedIdolList.size + searchedSupportList.size + searchedWallpaperList.size
            TYPE_SMALL_TALK -> searchedIdolList.size + searchedSupportList.size + searchedWallpaperList.size + searchedSmallTalkList.size
            TYPE_ARTICLE -> searchedIdolList.size + searchedSupportList.size + searchedWallpaperList.size + searchedSmallTalkList.size + searchedArticleList.size
            else -> searchedIdolList.size + searchedSupportList.size + searchedWallpaperList.size + searchedSmallTalkList.size + searchedArticleList.size
        }
    }


    companion object {
        const val TYPE_IDOL = 0
        const val TYPE_SEARCH_COMMUNITY_ARTICLE = 3
        const val TYPE_SEARCH_FEED_KIN_BOARD = 4    // FeedArticleAdapter TYPE_FEED_KIN_BOARD와 동일
        const val TYPE_SUPPORT = 6
        const val SUPPORT_SUCCESS = 7
        const val SUPPORT_ING = 8
        //잡담 게시판
        const val TYPE_SMALL_TALK = 9
        const val TYPE_WALLPAPER = 10
        const val TYPE_ARTICLE = 11

        //버튼 관련 STATUS값들.
        const val VIEW_MORE_BUTTON_STATUS = 100
        const val NORMAL_BUTTON_STATUS = 101
    }

}