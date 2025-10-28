package net.ib.mn.dialog

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.data.repository.ChatRepositoryImpl
import net.ib.mn.core.data.repository.QuizRepositoryImpl
import net.ib.mn.core.data.repository.ReportRepositoryImpl
import net.ib.mn.databinding.DialogDefaultChatReportTwoBtnBinding
import net.ib.mn.model.UserModel
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import javax.inject.Inject

@AndroidEntryPoint
class ReportReasonDialogFragment : BaseDialogFragment() {
    private lateinit var binding: DialogDefaultChatReportTwoBtnBinding
    @Inject
    lateinit var quizRepository: QuizRepositoryImpl
    @Inject
    lateinit var reportRepository: ReportRepositoryImpl
    @Inject
    lateinit var chatRepository: ChatRepositoryImpl

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogDefaultChatReportTwoBtnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //글자수 2천자 제한.
        binding.chatReportContent.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(REPORT_REASON_MAX_LENGTH))

        initSet()
        clickEvent()
    }

    //초기 세팅
    private fun initSet() {
        this.isCancelable = false
    }


    //클릭 이벤트 모음
    private fun clickEvent() {

        //취소 버튼 클릭
        binding.btnCancel.setOnClickListener {
            this.dismiss()
        }


        //확인 버튼 클릭
        binding.btnConfirm.setOnClickListener {
            if (binding.chatReportContent.length() < 10) {
                Toast.makeText(
                    activity,
                    String.format(resources.getString(R.string.comment_minimum_characters), 10),
                    Toast.LENGTH_SHORT
                ).show()
            } else {

                if(arguments?.getInt(PARAM_REPORT_TYPE) == CHATTING_REPORT){//채팅 신고일때 -> api가 달라서 feed랑 채팅을 나눔.
                    val content = binding.chatReportContent.text.toString()
                    val chatRoomId:Int = requireArguments().getInt(PARAM_CHATTING_ROOM_ID)
                    MainScope().launch {
                        chatRepository.reportChatRoom(
                            chatRoomId,
                            content,
                            listener = { response ->
                                if (response.optBoolean("success")) {
                                    setResultCode(ResultCode.REPORT_REASON_UPLOADED.value)
                                    dismiss()
                                    Util.showDefaultIdolDialogWithBtn1(
                                        activity, null, resources.getString(
                                            R.string.report_done
                                        )
                                    ) {

                                        Util.closeIdolDialog()
                                    }

                                } else {
                                    dismiss()
                                    Util.showDefaultIdolDialogWithBtn1(
                                        activity, null, ErrorControl.parseError(
                                            activity,
                                            response
                                        )
                                    ) { Util.closeIdolDialog() }
                                }
                            },
                            errorListener = { throwable ->
                                Toast.makeText(
                                    activity,
                                    R.string.error_abnormal_exception,
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (Util.is_log()) {
                                    (activity as BaseActivity).showMessage(throwable.message)
                                }
                            }
                        )
                    }
                }else if(arguments?.getInt(PARAM_REPORT_TYPE) == FEED_REPORT){
                    val content = binding.chatReportContent.text.toString()
                    val mUser = arguments?.getSerializable(PARAM_FEED_USERMODEL) as UserModel

                    MainScope().launch {
                        reportRepository.doReportFeed(
                            mUser.id.toLong(),
                            content,
                            listener = { response ->
                                if (response?.getBoolean("success")!!) {
                                    Util.showDefaultIdolDialogWithBtn1(
                                        activity, null, resources.getString(
                                            R.string.report_done
                                        )
                                    ) {

                                        Util.closeIdolDialog()
                                    }
                                    val account = IdolAccount.getAccount(activity)
                                    if (account != null) {
                                        val prefs = PreferenceManager
                                            .getDefaultSharedPreferences(activity)
                                        val editor = prefs.edit()
                                        val reportedUser = prefs.getStringSet(
                                            account.email + "_did_user_report",
                                            HashSet()
                                        )
                                        reportedUser!!.add(mUser.resourceUri)
                                        editor.putStringSet(account.email + "_did_user_report", reportedUser).apply()
                                    }
                                    setResultCode(ResultCode.REPORT_REASON_UPLOADED.value)
                                    dismiss()
                                } else {

                                    dismiss()
                                    val responseMsg = ErrorControl.parseError(activity, response)
                                    if (responseMsg != null) {
                                        Util.showDefaultIdolDialogWithBtn1(activity,
                                            null,
                                            responseMsg
                                        ) { Util.closeIdolDialog() }
                                    }
                                }
                           },
                            errorListener = { throwable ->
                                Toast.makeText(
                                    activity,
                                    R.string.error_abnormal_exception,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }else if(arguments?.getInt(PARAM_REPORT_TYPE) == QUIZ_REPORT) {//채팅 신고일때 -> api가 달라서 feed랑 채팅을 나눔.
                    val content = binding.chatReportContent.text.toString()
                    val quizId =  arguments?.getInt(PARAM_QUIZ_ID)
                    if (quizId != null) {
                        MainScope().launch {
                            quizRepository.reportQuiz(
                                quizId = quizId,
                                content = content,
                                listener = { response ->
                                    if (response.optBoolean("success")) {
                                        setResult(Intent().putExtra(QUIZ_REPORT_VALUE_KEY,QUIZ_REPORT_SUCCESS))
                                        setResultCode(ResultCode.REPORT_REASON_UPLOADED.value)
                                        dismiss()
                                    } else {
                                        val responseMsg = ErrorControl.parseError(context, response)
                                        responseMsg?.let {
                                            setResult(Intent().putExtra(QUIZ_REPORT_VALUE_KEY, QUIZ_REPORT_FAIL))
                                            setResult(Intent().putExtra(QUIZ_REPORT_FAIL_MSG_VALUE_KEY, responseMsg))
                                            setResultCode(ResultCode.REPORT_REASON_UPLOADED.value)
                                            dismiss()
                                        }
                                    }
                                },
                                errorListener = { throwable ->
                                    Toast.makeText(
                                        context, R.string.error_abnormal_exception,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }
            }
        }

    }

    companion object {

        private const val PARAM_REPORT_TYPE = "report_type"
        private const val PARAM_FEED_USERMODEL = "feed_user_model"
        private const val PARAM_CHATTING_ROOM_ID = "chatting_room_id"
        private const val PARAM_QUIZ_ID = "quiz_id"
        const val CHATTING_REPORT =0
        const val FEED_REPORT =1
        const val QUIZ_REPORT =2
        const val REPORT_REASON_MAX_LENGTH = 2000
        const val QUIZ_REPORT_VALUE_KEY = "quiz_report_result"
        const val QUIZ_REPORT_FAIL_MSG_VALUE_KEY = "quiz_report_fail_msg"
        const val QUIZ_REPORT_SUCCESS = 0
        const val QUIZ_REPORT_FAIL = 1

        fun getInstance(reportType:Int,quizId:Int): ReportReasonDialogFragment {

            val args = Bundle()
            val fragment = ReportReasonDialogFragment()


            args.putInt(PARAM_REPORT_TYPE, reportType)

            //리포트 타입에따라 맞는 필요값 넣어줌.
            if(reportType == QUIZ_REPORT){
                args.putInt(PARAM_QUIZ_ID,quizId)
            }

            fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
            fragment.arguments = args

            return fragment
        }

        fun getInstance(reportType:Int,chatRoomId:Int,userModel: UserModel?): ReportReasonDialogFragment {

            val args = Bundle()
            val fragment = ReportReasonDialogFragment()


            args.putInt(PARAM_REPORT_TYPE, reportType)

            //리포트 타입에따라 맞는 필요값 넣어줌.
            //채팅은 ->  채팅방 id
            //피드는 해당 피드 유저모델
            if(reportType == CHATTING_REPORT){
                 args.putInt(PARAM_CHATTING_ROOM_ID,chatRoomId)
            }else if(reportType == FEED_REPORT && userModel != null){
                args.putSerializable(PARAM_FEED_USERMODEL,userModel)
            }

            fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
            fragment.arguments = args

            return fragment
        }



    }

}