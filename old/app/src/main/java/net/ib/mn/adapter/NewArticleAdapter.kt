package net.ib.mn.adapter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
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
import net.ib.mn.R
import net.ib.mn.databinding.CommunityItemBinding
import net.ib.mn.databinding.ItemBoardBinding
import net.ib.mn.databinding.ItemBoardNoticeBinding
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.NoticeModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.viewholder.ArticleViewHolder
import net.ib.mn.viewholder.BoardNoticeViewHolder
import net.ib.mn.viewholder.BoardViewHolder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 *  자유게시판/지식돌 전용. 나중에 커뮤니티 전용 ArticleAdapter를 합칠 것.
 */
@UnstableApi
class NewArticleAdapter(
    private var context: Context,
    private var useTranslation: Boolean = false,
    private var idol: IdolModel,
    private var items: ArrayList<ArticleModel>,
    private var noticeItem: ArrayList<NoticeModel>,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onArticleClick: (ArticleModel, Int) -> Unit,
    private val onNoticeClick: (NoticeModel) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val calendar = Calendar.getInstance()

    interface OnClickListener {
        fun onArticleItemClicked(item: ArticleModel, view: View, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemArticle: ItemBoardBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_board,
            parent,
            false
        )

        val itemNotice: ItemBoardNoticeBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_board_notice,
            parent,
            false
        )
        return if (viewType == TYPE_NOTICE) {
            BoardNoticeViewHolder(itemNotice, onNoticeClick)
        } else {
            BoardViewHolder(itemArticle, calendar, onArticleClick)
        }
    }

    override fun getItemCount(): Int {
        return noticeItem.size + items.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < noticeItem.size) {
            TYPE_NOTICE
        } else {
            TYPE_KIN_BOARD
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == TYPE_KIN_BOARD) {
            val articleHolder = holder as BoardViewHolder
            val item = items[position - noticeItem.size]
            articleHolder.bind(item)
        } else {
            val noticeHolder = holder as BoardNoticeViewHolder
            val item = noticeItem[position]
            noticeHolder.bind(item, position != noticeItem.size - 1)
        }
    }

    companion object {
        const val TYPE_KIN_BOARD = 0    //자게, 지식돌 들어갔을 때
        const val TYPE_NOTICE = 1
    }
}