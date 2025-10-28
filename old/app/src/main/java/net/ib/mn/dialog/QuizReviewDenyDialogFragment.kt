package net.ib.mn.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.ib.mn.R
import net.ib.mn.activity.IdolQuizMainActivity
import net.ib.mn.databinding.DialogQuizReviewDenyBinding

class QuizReviewDenyDialogFragment : BaseDialogFragment() {
    private lateinit var binding: DialogQuizReviewDenyBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogQuizReviewDenyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSet()
        clickEvent()
    }

    //초기 세팅
    private fun initSet() {

        val quizDenyType = arguments?.getString(QUIZ_REVIEW_DENY_TYPE)
        //심사 3번 완료한 경우
        if(quizDenyType==IdolQuizMainActivity.QUIZ_REVIEW_MAX_OVER){
            binding.tvReviewDeny.text = getString(R.string.quiz_finish_today_review)
        }
        //최애의 퀴즈, All 퀴즈에 대기 상태가 없는 경우
        else{
            binding.tvReviewDeny.text = getString(R.string.quiz_nothing_review)
        }
    }


    //클릭 이벤트 모음
    private fun clickEvent() {
        //확인 버튼 클릭
        binding.btnConfirm.setOnClickListener {
            this.dismiss()
        }
    }

    companion object{
        private const val QUIZ_REVIEW_DENY_TYPE = "quiz_review_deny_type"
        fun getInstance(status : String) : QuizReviewDenyDialogFragment{
            val args = Bundle()
            val fragment = QuizReviewDenyDialogFragment()
            args.putString(QUIZ_REVIEW_DENY_TYPE, status)
            fragment.arguments = args

            return fragment
        }
    }
}