package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.databinding.CommentItemBinding
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.CommentModel
import net.ib.mn.model.SupportListModel
import net.ib.mn.utils.Logger
import net.ib.mn.utils.modelToString
import net.ib.mn.viewholder.CommentViewHolder

class CommentOnlyAdapter(
    private val mGlideRequestManager: RequestManager,
    private val useTranslation: Boolean = false,
    private val lifecycleScope: LifecycleCoroutineScope,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isLoading = false
    var loadFailed = false // 네트워크 오류로 로드 실패 여부

    //댓글 모음
    private var commentList = ArrayList<CommentModel>()
    private var isNextCommentDataExist: Boolean = false

    //클릭 관련 이벤트
    private var onCommentItemClickListener: OnCommentItemClickListener? = null
    private var articlePhotoListener: ArticlePhotoListener? = null

    interface OnCommentItemClickListener {
        fun onCommentNameClicked(commentItem: CommentModel)
        fun onCommentProfileImageClicked(commentItem: CommentModel)
        fun onViewMoreItemClicked()
        fun onCommentImageClicked(articleModel : ArticleModel)
        fun onRefreshClicked() // 다음 댓글 불러오기 실패해서 갱신하는 경우
    }

    fun setPhotoClickListener(articlePhotoListener: ArticlePhotoListener) {
        this.articlePhotoListener = articlePhotoListener
    }

    //외부에서  댓글  아이템 클릭 처리할 리스너
    fun setOnCommentItemClickListener(onCommentItemClickListener: OnCommentItemClickListener) {
        this.onCommentItemClickListener = onCommentItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: CommentItemBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.comment_item,
                parent,
                false)

        return CommentViewHolder(
            binding = binding,
            context = parent.context,
            useTranslation = useTranslation,
            mArticle = null,
            commentList = commentList,
            isCommentOnly = true,
            mGlideRequestManager = mGlideRequestManager
        )
    }

    override fun getItemCount(): Int {
        return commentList.size
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as CommentViewHolder).apply {
            bind(commentList[position], position, isNextCommentDataExist, loadFailed)

            // 마지막 5개쯤 전을 보여줄 때 다음 목록 로드
            // 네트워크 오류로 자동 불러오기 실패한 경우에는 하지 않음
            if( position >= commentList.size - 5 && isNextCommentDataExist && !isLoading && !loadFailed ) {
                isLoading = true
                onCommentItemClickListener?.onViewMoreItemClicked()
            }

            binding.clRefresh.setOnClickListener {
                onCommentItemClickListener?.onRefreshClicked()
            }

            binding.photo.setOnClickListener {
                onCommentItemClickListener?.onCommentProfileImageClicked(commentItem = commentList[position])
            }

            binding.name.setOnClickListener {
                onCommentItemClickListener?.onCommentNameClicked(commentItem = commentList[position])
            }
            binding.ivImageContent.setOnClickListener {
                //이미지나 움짤만 클릭헀을 때 widePhoto가 나와야함.
                commentList.getOrNull(position)?.let { comment ->

                    val article = comment.article ?: ArticleModel().apply {
                        umjjalUrl = comment.contentAlt?.umjjalUrl
                        imageUrl = comment.contentAlt?.imageUrl
                    }

                    if (article.imageUrl.isNullOrEmpty() && article.umjjalUrl.isNullOrEmpty()) {
                        return@let
                    }

                    onCommentItemClickListener?.onCommentImageClicked(article)
                }
            }
        }
    }


    //커멘트 리스트 받기
    fun getCommentList(
        commentList: ArrayList<CommentModel>,
        isNextDataExist: Boolean,
    ) {
        this.commentList = commentList
        this.isNextCommentDataExist = isNextDataExist
        notifyDataSetChanged()
    }

}