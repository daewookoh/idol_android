package net.ib.mn.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.R
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.core.domain.usecase.DeleteArticleUseCase
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import javax.inject.Inject

// TODO: viewModel 작업
@AndroidEntryPoint
class ArticleRemoveDialogFragment : BaseDialogFragment(), View.OnClickListener {
    private var mArticle: ArticleModel? = null
    private var mPosition = 0

    @Inject
    lateinit var deleteArticleUseCase: DeleteArticleUseCase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_remove, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val submitBtn = view.findViewById<AppCompatButton>(R.id.btn_confirm)
        val cancelBtn = view.findViewById<AppCompatButton>(R.id.btn_cancel)

        submitBtn.setOnClickListener(this)
        cancelBtn.setOnClickListener(this)
    }

    override fun onActivityCreated(arg0: Bundle?) {
        super.onActivityCreated(arg0)
        mArticle = requireArguments().getSerializable(PARAM_ARTICLE) as ArticleModel?
        mPosition = requireArguments().getInt(PARAM_POSITION, -1)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_cancel) {
            dismiss()
        } else if (v.id == R.id.btn_confirm) {
            val id = mArticle?.id?.toLong() ?: return
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                deleteArticleUseCase(id).collect { response ->
                    withContext(Dispatchers.Main) {
                        if (response.success) {
                            Util.closeProgress()
                            val result = Intent()
                            if (mPosition != -1) {
                                result.putExtra(PARAM_POSITION, mPosition)
                            }
                            result.putExtra(PARAM_ARTICLE_ID, mArticle!!.id)
                            result.putExtra(PARAM_ARTICLE, mArticle)
                            setResult(result)
                            setResultCode(ResultCode.REMOVED.value)
                            dismiss()
                        } else {
                            if (isAdded) {
                                makeText(requireActivity(), response.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun getInstance(article: ArticleModel?): ArticleRemoveDialogFragment {
            val fragment = ArticleRemoveDialogFragment()
            fragment.setStyle(STYLE_NO_TITLE, 0)
            val args = Bundle()
            args.putSerializable(PARAM_ARTICLE, article)
            fragment.arguments = args
            return fragment
        }

        fun getInstance(
            article: ArticleModel?,
            position: Int
        ): ArticleRemoveDialogFragment {
            val fragment = ArticleRemoveDialogFragment()
            fragment.setStyle(STYLE_NO_TITLE, 0)
            val args = Bundle()
            args.putSerializable(PARAM_ARTICLE, article)
            args.putInt(PARAM_POSITION, position)
            fragment.arguments = args
            return fragment
        }

        const val PARAM_POSITION: String = CommunityActivity.PARAM_ARTICLE_POSITION
        const val PARAM_ARTICLE_ID: String = "articleId"
        private const val PARAM_ARTICLE = "article"
    }
}
