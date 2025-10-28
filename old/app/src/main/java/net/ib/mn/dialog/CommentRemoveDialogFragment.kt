package net.ib.mn.dialog

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.core.data.repository.comments.CommentsRepository
import net.ib.mn.databinding.DialogRemoveBinding
import net.ib.mn.model.CommentModel
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class CommentRemoveDialogFragment : BaseDialogFragment(), View.OnClickListener {
    private var mComment: CommentModel? = null
    private var mPosition = 0

    @Inject
    lateinit var commentsRepository: CommentsRepository

    // viewbinding
    private var _binding: DialogRemoveBinding? = null
    private val binding get() = _binding!!

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val r = resources
            val width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 290f, r.displayMetrics
            ).toInt()
            dialog.window!!.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRemoveBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfirm.setOnClickListener(this)
        binding.btnCancel.setOnClickListener(this)
        binding.removeDesc.setText(R.string.delete_comment)
    }

    override fun onActivityCreated(arg0: Bundle?) {
        super.onActivityCreated(arg0)
        mComment = requireArguments().getSerializable(PARAM_COMMENT) as CommentModel?
        mPosition = requireArguments().getInt(PARAM_POSITION, 1)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_cancel) {
            dismiss()
        } else if (v.id == R.id.btn_confirm) {
            lifecycleScope.launch {
                commentsRepository.deleteComment(
                    mComment?.resourceUri ?: return@launch,
                    { response ->
                        val result = Intent()
                        result.putExtra(PARAM_POSITION, mPosition)
                        // UI 상에서만 지우게 처리
                        result.putExtra(
                            NewCommentActivity.PARAM_RESOURCE_URI,
                            mComment!!.resourceUri
                        )
                        setResult(result)
                        setResultCode(ResultCode.COMMENT_REMOVED.value)
                        dismiss()
                    }, { throwable ->
                        if (activity != null) {
                            makeText(activity, throwable.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    companion object {
        fun getInstance(
            comment: CommentModel?,
            position: Int
        ): CommentRemoveDialogFragment {
            val fragment = CommentRemoveDialogFragment()
            fragment.setStyle(STYLE_NO_TITLE, 0)
            val args = Bundle()
            args.putSerializable(PARAM_COMMENT, comment)
            args.putInt(PARAM_POSITION, position)
            fragment.arguments = args
            return fragment
        }

        const val PARAM_POSITION: String = CommunityActivity.PARAM_ARTICLE_POSITION
        private const val PARAM_COMMENT = "comment"
    }
}
