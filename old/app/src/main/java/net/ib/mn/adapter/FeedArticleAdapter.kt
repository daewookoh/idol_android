package net.ib.mn.adapter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import net.ib.mn.R
import net.ib.mn.databinding.CommunityItemBinding
import net.ib.mn.databinding.ItemSmallTalkBinding
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.smalltalk.viewholder.SmallTalkVH
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.viewholder.ArticleViewHolder
import java.util.Calendar

@UnstableApi
class FeedArticleAdapter(
    private val context: Context,
    private val feedArticleList: ArrayList<ArticleModel>,
    private val smallTalkListener: SmallTalkListener,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val isVisibleViewCount: Boolean = true,
    private val onArticleButtonClick: (ArticleModel, View, Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val calendar = Calendar.getInstance()
    private val locale = LocaleUtil.getAppLocale(context)

    private var articlePhotoListener: ArticlePhotoListener? = null

    fun setPhotoClickListener(articlePhotoListener: ArticlePhotoListener) {
        this.articlePhotoListener = articlePhotoListener
    }

    interface SmallTalkListener {
        fun smallTalkItemClicked(articleModel: ArticleModel, position: Int)

    }

    interface OnArticleClickListener {
        fun onArticleButtonClick(model: ArticleModel, v: View?, position: Int)
    }

    // exoplayer
    private var player: ExoPlayer? = null
    lateinit var dataSourceFactory: DefaultDataSourceFactory
    lateinit var extractorsFactory: ExtractorsFactory

    init {
        initExoPlayer()
    }

    override fun getItemCount(): Int {
        return feedArticleList.size
    }

    override fun getItemViewType(position: Int): Int {
        if (feedArticleList[position].idol != null
            && (feedArticleList[position].idol?.getId() == Const.IDOL_ID_KIN
                || feedArticleList[position].idol?.getId() == Const.IDOL_ID_FREEBOARD)
        ) {
            return TYPE_SEARCH_FEED_KIN_BOARD
        }

        return if (feedArticleList[position].type == "A") {
            TYPE_FEED_COMMUNITY_ARTICLE
        } else {
            TYPE_SMALL_TALK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SMALL_TALK, TYPE_SEARCH_FEED_KIN_BOARD -> {
                val itemSmallTalk: ItemSmallTalkBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_small_talk,
                    parent,
                    false
                )
                return SmallTalkVH(itemSmallTalk, calendar, locale, true)
            }

            else -> {
                val itemFeedArticle: CommunityItemBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.community_item,
                    parent,
                    false
                )
                return ArticleViewHolder(
                    itemFeedArticle,
                    context,
                    useTranslation = ConfigModel.getInstance(context).showTranslation,
                    viewType,
                    onArticleButtonClick,
                    articlePhotoListener,
                    lifecycleScope = lifecycleScope,
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val article = feedArticleList[position]
        when (holder.itemViewType) {
            TYPE_SMALL_TALK, TYPE_SEARCH_FEED_KIN_BOARD -> {
                (holder as SmallTalkVH).apply {
                    bind(articleModel = article) {
                        smallTalkListener.smallTalkItemClicked(article, position)
                    }
                }
            }

            else -> {
                (holder as ArticleViewHolder).apply {
                    bind(article, IdolModel(), position, 0, isVisibleViewCount)
                }
            }
        }
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
            player = ExoPlayer.Builder(context, renderersFactory)
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
                    Util.log("onPlayerStateChanged $playbackState")
                    if (playbackState == ExoPlayer.STATE_READY) {
                        // 검은화면 방지
                        //holder.mProgressVideo.setVisibility(View.GONE);
                        //Intent intent = new Intent(Const.VIDEO_READY_EVENT);
                        //LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                }

            })

        }
    }

    fun getPlayer(): ExoPlayer {
        return player!!
    }

    companion object {
        const val TYPE_FEED_COMMUNITY_ARTICLE = 2
        const val TYPE_SEARCH_FEED_KIN_BOARD = 4    // SearchAdapter TYPE_FEED_KIN_BOARD와 동일
        const val TYPE_SMALL_TALK = 5
    }
}