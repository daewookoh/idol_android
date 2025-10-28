package net.ib.mn.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.*
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.FeedActivity
import net.ib.mn.adapter.FeedPhotoAdapter
import net.ib.mn.databinding.FragmentFeedPhotoBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.*
import net.ib.mn.view.ControllableAppBarLayout
import net.ib.mn.view.EndlessRecyclerViewScrollListener
import java.util.HashSet


class FeedPhotoFragment : BaseFragment(),
        BaseDialogFragment.DialogResultHandler {

    private var mAccount: IdolAccount? = null
    private lateinit var mActivity: FeedActivity
    var mContext: Context? = null
    private var userId : Int = 0
    var mFeedPhotoAdapter: FeedPhotoAdapter? = null
    var mFeedScrollListener: EndlessRecyclerViewScrollListener? = null
    private var mImageSize: Int = 0
    var binding: FragmentFeedPhotoBinding? = null // FeedActivity에서 먼저 참조하는 경우가 있어 nullable 처리
    var memoryOffset = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFeedPhotoBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActivity = activity as FeedActivity
        mContext = activity as Context
        mAccount = IdolAccount.getAccount(mContext)
        mImageSize = getImageSize()
        val glm = GridLayoutManager(activity, 3)
        mContext?.let {
            mFeedPhotoAdapter = FeedPhotoAdapter(mContext!!,
                mGlideRequestManager,
                mImageSize,
                mActivity.feedPhotoList)
        }

        mFeedScrollListener = object : EndlessRecyclerViewScrollListener(glm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                if (memoryOffset == mActivity.feedPhotoOffset) {
                    return
                }

                memoryOffset = mActivity.feedPhotoOffset
                mActivity.loadMorePhotos()
            }
        }
        binding?.rvFeedPhoto?.layoutManager = glm
        binding?.rvFeedPhoto?.adapter = mFeedPhotoAdapter
        binding?.rvFeedPhoto?.addOnScrollListener(mFeedScrollListener as EndlessRecyclerViewScrollListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.ARTICLE_COMMENT.value -> {
                if (data != null) {
                    val article = data.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel
                    val position = getPhotoPosition(article.id)

                    when (resultCode) {
                        ResultCode.REMOVED.value -> {
                            if (position >= 0) {
                                mActivity.feedPhotoList.removeAt(position)
                                mFeedPhotoAdapter?.notifyItemRemoved(position)
                            }
                        }
                        ResultCode.VOTED.value,
                        ResultCode.COMMENT_REMOVED.value,
                        ResultCode.EDITED.value -> {
                            if (position >= 0) {
                                mActivity.feedPhotoList[position] = article
                                mFeedPhotoAdapter?.notifyItemChanged(position)
                            }
                        }
                        ResultCode.REPORTED.value -> {  //피드 -> 게시글 -> 댓글 -> 신고 -> 돌아왔을 때
                            mActivity.feedPhotoList.removeAt(position)
                            mFeedPhotoAdapter?.notifyItemRemoved(position)

                            if (mActivity.feedPhotoList.size == 0) {
                                showEmpty(userId)
                            }
                        }
                        ResultCode.ARTICLE_LIKE_EXCEPTION.value -> {
                            mActivity.postArticleLike(article)
                        }
                    }
                }
                activity?.setResult(FeedActivityFragment.FEED_ARTICLE_MODIFY)
            }
            RequestCode.ARTICLE_EDIT.value -> {
                if (data != null) {
                    val article = data.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel
                    val position = getPhotoPosition(article.id)

                    if (resultCode == ResultCode.EDITED.value) {
                        if (position >= 0) {
                            mActivity.feedPhotoList[position] = article
                            mFeedPhotoAdapter?.notifyItemChanged(position)
                        }
                    }
                }
            }

        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.ARTICLE_REPORT.value -> {
                if (resultCode == ResultCode.REPORTED.value) {
                    val article = data?.getSerializableExtra(FeedActivity.PARAM_ARTICLE) as ArticleModel
                    val position = getPhotoPosition(article.id)

                    if (position >= 0) {
                        val model = mActivity.feedPhotoList[position]
                        model.reportCount = model.reportCount + 1
                        mFeedPhotoAdapter?.notifyItemChanged(position)

                        val account = IdolAccount.getAccount(mContext)
                        if (account != null) {
                            val prefs = PreferenceManager
                                    .getDefaultSharedPreferences(mContext)
                            val editor = prefs.edit()
                            val reportedArticles = prefs.getStringSet(
                                    account.email + "_did_report",
                                    HashSet())
                            reportedArticles!!.add(model.resourceUri)
                            editor.putStringSet(account.email + "_did_report",
                                    reportedArticles).apply()
                        }
                        mActivity.feedPhotoList.removeAt(position)
                        mFeedPhotoAdapter?.notifyItemRemoved(position)

                        if (mActivity.feedPhotoList.size == 0) {
                            showEmpty(userId)
                        }
                        //article 차단 목록 추가
                        context?.let { UtilK.addArticleReport(it, article.id) }
                    }
                }
            }
            RequestCode.ARTICLE_REMOVE.value -> {
                Util.closeProgress()

                if (resultCode == ResultCode.REMOVED.value) {
                    val position = getPhotoPosition(data!!.getStringExtra(FeedActivity.PARAM_ARTICLE_ID))

                    if (position >= 0) {
                        mActivity.feedPhotoList.removeAt(position)
                        mFeedPhotoAdapter?.notifyItemRemoved(position)

                        if (mActivity.feedPhotoList.size == 0) {
                            showEmpty(userId)
                        }
                    }
                }
            }
        }
    }

    private fun getImageSize(): Int {
        val wm = activity?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.x / 3
    }

    private fun getPhotoPosition(articleId: String?): Int {
        val position = mActivity.feedPhotoList.withIndex().find { it.value.id == articleId }?.index
        if (position != null) return position
        return -1
    }

    fun hideEmpty(userId: Int) {
        val binding = binding ?: return
        mContext?.let {
            if (!UtilK.isUserNotBlocked(mContext!!, userId)) {
                appBarState(binding.llUserBlock)
                binding.llPrivacy.visibility = View.GONE
                binding.tvEmpty.visibility = View.GONE
                binding.rvFeedPhoto.visibility = View.GONE
                binding.llUserBlock.visibility = View.VISIBLE
            } else {
                binding.llPrivacy.visibility = View.GONE
                binding.tvEmpty.visibility = View.GONE
                binding.rvFeedPhoto.visibility = View.VISIBLE
                binding.llUserBlock.visibility = View.GONE
            }
        }
    }

    //작성한 글이 없음
    fun showEmpty(userId: Int) {
        val binding = binding ?: return
        this.userId = userId
        mContext?.let {
            if (!UtilK.isUserNotBlocked(mContext!!, userId)) {
                appBarState(binding.llUserBlock)
                binding.llPrivacy.visibility = View.GONE
                binding.tvEmpty.visibility = View.GONE
                binding.rvFeedPhoto.visibility = View.GONE
                binding.llUserBlock.visibility = View.VISIBLE
            } else {
                binding.llPrivacy.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvFeedPhoto.visibility = View.GONE
                binding.llUserBlock.visibility = View.GONE
                showExpandedEmpty()
            }
        }
    }

    fun showPrivacy(userId : Int) {
        val binding = binding ?: return
        mContext?.let {
            if (!UtilK.isUserNotBlocked(mContext!!, userId)) {
                appBarState(binding.llUserBlock)
                binding.llPrivacy.visibility = View.GONE
                binding.tvEmpty.visibility = View.GONE
                binding.rvFeedPhoto.visibility = View.GONE
                binding.llUserBlock.visibility = View.VISIBLE
            } else {
                appBarState(binding.llPrivacy)
                binding.llPrivacy.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE
                binding.rvFeedPhoto.visibility = View.GONE
                binding.llUserBlock.visibility = View.GONE
            }
        }
    }

    fun appBarState(linearLayoutCompat: LinearLayoutCompat?){
        val appbar = activity?.findViewById<ControllableAppBarLayout>(R.id.appbar)
        if (appbar != null) {
            if (appbar.state == ControllableAppBarLayout.State.EXPANDED) {
                val lp = linearLayoutCompat?.layoutParams
                val metrics = this.resources.displayMetrics
                lp?.height = metrics.heightPixels - appbar.height
                linearLayoutCompat?.layoutParams = lp
            }
        }
    }

    fun showCollpasedEmpty() {
        binding?.tvEmpty?.layoutParams =
                LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT)
    }

    fun showExpandedEmpty() {
        val appbar = activity?.findViewById<ControllableAppBarLayout>(R.id.appbar)
        if (appbar != null) {
            if (appbar.state == ControllableAppBarLayout.State.EXPANDED) {
                val lp = binding?.tvEmpty?.layoutParams
                val metrics = this.resources.displayMetrics
                lp?.height = metrics.heightPixels - appbar.height
                binding?.tvEmpty?.layoutParams = lp
            }
        }
    }

    companion object {
        fun getInstance(): FeedPhotoFragment {
            return FeedPhotoFragment()
        }
    }
}