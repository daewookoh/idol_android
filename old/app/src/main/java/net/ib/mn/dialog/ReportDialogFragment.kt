package net.ib.mn.dialog

import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.core.data.repository.ReportRepositoryImpl
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.CommentModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import org.json.JSONException
import javax.inject.Inject

@AndroidEntryPoint
class ReportDialogFragment : BaseDialogFragment(), View.OnClickListener {
    private var mSubmitBtn: AppCompatButton? = null
    private var mCancelBtn: AppCompatButton? = null
    private var mTextMsg: AppCompatTextView? = null
    private var mTextTitle: AppCompatTextView? = null

    //댓글 신고시 받아오는 댓글 모델
    private var mComment: CommentModel? = null
    private var mArticle: ArticleModel? = null

    private var isHeartPick: Boolean? = null

    //어떤 신고인지  신고할 모델의 type check
    private var typeCheck = 0

    //채팅 리포트일때  내용이  기존 신고와 다르므로,  비교 값을 주어  구별 한다.
    private var isChattingReport = false

    private var mPosition = 0

    private var spannedMsg: Spanned? = null
    @Inject
    lateinit var reportRepository: ReportRepositoryImpl

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mTextTitle = view.findViewById(R.id.text_report_title)
        mSubmitBtn = view.findViewById(R.id.btn_confirm)
        mCancelBtn = view.findViewById(R.id.btn_cancel)
        mTextMsg = view.findViewById(R.id.text_report_msg)

        mSubmitBtn?.setOnClickListener(this)
        mCancelBtn?.setOnClickListener(this)

        if (spannedMsg != null) mTextMsg?.setText(spannedMsg)
    }

    override fun onActivityCreated(arg0: Bundle?) {
        super.onActivityCreated(arg0)
        mArticle = requireArguments().getSerializable(PARAM_ARTICLE) as ArticleModel?
        mComment = requireArguments().getSerializable(PARAM_COMMENT_REPORT) as CommentModel?
        isHeartPick = requireArguments().getBoolean(PARAM_IS_HEART_PICK)
        mPosition = requireArguments().getInt(PARAM_POSITION, 1)
        isChattingReport = requireArguments().getBoolean(PARAM_CHATTING_REPORT, false)
        typeCheck = requireArguments().getInt(PARAM_TYPE_CHECK, -1)
        if (isChattingReport || typeCheck == Const.TYPE_COMMENT_REPORT) {
            mTextTitle!!.setText(R.string.report)
        }
    }

    /**
     * 신고시 하트 소진 경고 문구 설정
     * @param spanned
     */
    fun setMessage(spanned: Spanned?) {
        spannedMsg = spanned
    }

    //typecheck 값에 따라 알맞은 model값을 return 해준다.
    private fun returnRightTypeModel(typeCheck: Int): Any? {
        if (typeCheck == Const.TYPE_ARTICLE_REPORT) {
            return mArticle
        } else if (typeCheck == Const.TYPE_COMMENT_REPORT) {
            return mComment
        }
        return null
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_cancel) {
            dismiss()
        } else if (v.id == R.id.btn_confirm) {
            if (!isChattingReport) {
                try {
                    if (isHeartPick!!) {
                        doHeartPickReport()
                    } else {
                        doReport()
                    }
                } catch (e: Exception) {
                    makeText(
                        requireActivity(),
                        R.string.failed_to_report,
                        Toast.LENGTH_SHORT
                    ).show()
                    e.printStackTrace()
                }
            } else {
                setResultCode(ResultCode.REPORTED.value)
                dismiss()
            }
        }
    }

    private fun doReport() {
        MainScope().launch {
            reportRepository.doReport(
                articleId = mArticle?.id?.toLong(),
                commentId = mComment?.id?.toLong(),
                listener = { response ->
                    try {
                        if (response.getBoolean("success")) {
                            //아티클 신고시 처리

                            if (typeCheck == Const.TYPE_ARTICLE_REPORT) {
                                val result = Intent()
                                result.putExtra(PARAM_POSITION, mPosition)
                                result.putExtra(PARAM_ARTICLE, mArticle)
                                setResult(result)
                                setResultCode(ResultCode.REPORTED.value)
                            }
                            makeText(activity, R.string.report_done, Toast.LENGTH_SHORT).show()

                            dismiss()
                        } else {
                            val responseMsg = ErrorControl.parseError(activity, response)
                            if (responseMsg != null) {
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    responseMsg
                                ) { v: View? ->
                                    Util.closeIdolDialog()
                                    dismiss()
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }                },
                errorListener = { error ->
                    makeText(activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT)
                        .show()
                    if (Util.is_log()) {
                        (activity as BaseActivity?)!!.showMessage(error.message)
                    }
                }
            )
        }
    }

    private fun doHeartPickReport() {
        MainScope().launch {
            val id = mComment?.id?.toLong() ?: return@launch

            reportRepository.doReportHeartPick(
                id,
                listener = { response ->
                    if (response.getBoolean("success")) {
                        val result = Intent()
                        result.putExtra(PARAM_POSITION, mPosition)
                        result.putExtra(PARAM_ARTICLE, mArticle)
                        setResult(result)
                        setResultCode(ResultCode.REPORTED.value)
                        makeText(activity, R.string.report_done, Toast.LENGTH_SHORT).show()

                        dismiss()
                    } else {
                        val responseMsg = ErrorControl.parseError(activity, response)
                        if (responseMsg != null) {
                            Util.showDefaultIdolDialogWithBtn1(
                                activity,
                                null,
                                responseMsg
                            ) { v: View? ->
                                Util.closeIdolDialog()
                                dismiss()
                            }
                        }
                    }
                },
                errorListener = { error ->
                    makeText(activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT)
                        .show()
                    if (Util.is_log()) {
                        (activity as BaseActivity?)!!.showMessage(error.message)
                    }
                }
            )
        }
    }

    companion object {
        fun getInstance(article: ArticleModel?): ReportDialogFragment {
            val fragment = ReportDialogFragment()
            fragment.setStyle(STYLE_NO_TITLE, 0)
            val args = Bundle()
            args.putSerializable(PARAM_ARTICLE, article)
            args.putInt(PARAM_TYPE_CHECK, Const.TYPE_ARTICLE_REPORT)
            fragment.arguments = args
            return fragment
        }

        fun getInstance(
            commentModel: CommentModel?,
            isHeartPickComment: Boolean?
        ): ReportDialogFragment {
            val fragment = ReportDialogFragment()
            fragment.setStyle(STYLE_NO_TITLE, 0)
            val args = Bundle()
            args.putInt(PARAM_TYPE_CHECK, Const.TYPE_COMMENT_REPORT)
            args.putSerializable(PARAM_COMMENT_REPORT, commentModel)
            args.putBoolean(PARAM_IS_HEART_PICK, isHeartPickComment!!)
            fragment.arguments = args
            return fragment
        }

        fun getInstance(
            article: ArticleModel?,
            position: Int
        ): ReportDialogFragment {
            val fragment = ReportDialogFragment()
            fragment.setStyle(STYLE_NO_TITLE, 0)
            val args = Bundle()
            args.putInt(PARAM_TYPE_CHECK, Const.TYPE_ARTICLE_REPORT)
            args.putSerializable(PARAM_ARTICLE, article)
            args.putInt(PARAM_POSITION, position)
            fragment.arguments = args
            return fragment
        }

        //채팅 방 신고일때는 따로  텍스트를 바꿔줘야 되므로, 아래 처럼 적용 한다.
        fun getInstance(isChattingReport: Boolean): ReportDialogFragment {
            val fragment = ReportDialogFragment()
            fragment.setStyle(STYLE_NO_TITLE, 0)
            val args = Bundle()
            args.putBoolean(PARAM_CHATTING_REPORT, isChattingReport)
            fragment.arguments = args
            return fragment
        }


        const val PARAM_POSITION: String = CommunityActivity.PARAM_ARTICLE_POSITION
        private const val PARAM_ARTICLE = "article"
        private const val PARAM_CHATTING_REPORT = "chatting_report"
        private const val PARAM_COMMENT_REPORT = "comment_report"
        private const val PARAM_TYPE_CHECK = "type_check"
        private const val PARAM_IS_HEART_PICK = "is_heart_pick"
    }
}
