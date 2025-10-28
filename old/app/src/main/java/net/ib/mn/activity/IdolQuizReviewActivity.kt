package net.ib.mn.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.QuizRepositoryImpl
import net.ib.mn.databinding.ActivityIdolQuizReviewBinding
import net.ib.mn.databinding.DialogIdolQuizReviewMoreBinding
import net.ib.mn.databinding.DialogQuizReportBinding
import net.ib.mn.model.QuizReviewModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONException
import javax.inject.Inject
import kotlin.math.min

@AndroidEntryPoint
class IdolQuizReviewActivity : BaseActivity() {

    private lateinit var mQuizReviewObject: QuizReviewModel

    private lateinit var binding: ActivityIdolQuizReviewBinding

    //세션 id
    private var sessId = 0

    //quiz_approve에 review에서 얻어온 ssid를 넣는 변수
    private var approveSessId = 0

    //아이돌 id
    private var mIdolId = 0

    //라운드카운드.
    private var roundCount = 1

    //문제개수.
    private var quizzes = 1

    //심사리스트.
    private var reviewModelList = ArrayList<QuizReviewModel>()

    //현재 심사리스트에서 심사모델가져오는 index.
    private var listCount = 0

    private var quizRewardHeart = 0

    @Inject
    lateinit var quizRepository: QuizRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_idol_quiz_review)
        binding.svQuizReview.applySystemBarInsets()

        supportActionBar?.setTitle(R.string.quiz_review_in_review)
        initQuizReview()
    }

    //처음 퀴즈 리뷰 들어왔을 때 intent로 받은 값 저장
    private fun initQuizReview() {
        reviewModelList = intent.getSerializableExtra(QUIZ_REVIEW_LIST) as ArrayList<QuizReviewModel>
        sessId = intent.getIntExtra(QUIZ_SESS_ID,0)
        mIdolId = intent.getIntExtra(QUIZ_IDOL_ID,0)
        roundCount = intent.getIntExtra(QUIZ_ROUND_COUNT,1)
        quizzes = intent.getIntExtra(QUIZ_QUIZZES,1)
        listCount = intent.getIntExtra(QUIZ_LIST_COUNT, 1)
        quizRewardHeart = intent.getIntExtra(QUIZ_REWARD_HEART, 0)

        //CardView BackGround는 코드상에서 해줘야됨.
        with(binding) {
            cvQuizFirstReview.background = ContextCompat.getDrawable(this@IdolQuizReviewActivity,R.drawable.quiz_shadow_radius)
            cvQuizSecondReview.background = ContextCompat.getDrawable(this@IdolQuizReviewActivity, R.drawable.quiz_shadow_radius)

            //퀴즈리뷰 object에   해당 리뷰리스트의  퀴즈 모델(현재 리뷰할 퀴즈 모델)을 넣어줌.
            mQuizReviewObject = reviewModelList[listCount]

            //여기로 넘어왔다는건 리스트가 빈값이 아니라는뜻이므로 첫번째 문제를 보여줌.
            tvQuizTitle.text = reviewModelList[listCount].quiz.content


            ivQuizContent.visibility = if (reviewModelList[listCount].quiz.imageUrl.isNullOrEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }

            val glideRequestManager: RequestManager = Glide.with(this@IdolQuizReviewActivity)

            if (reviewModelList[listCount].quiz.umjjalUrl == null) {
                ivQuizContent.post {
                    glideRequestManager
                        .load(reviewModelList[listCount].quiz.imageUrl)
                        .into(ivQuizContent)
                }
            } else {
                ivQuizContent.post {
                    glideRequestManager
                        .asGif()
                        .load(reviewModelList[listCount].quiz.imageUrl)
                        .into(ivQuizContent)
                }
            }

            if (Util.isRTL(this@IdolQuizReviewActivity)) {
                tvQuizChoice1.text = reviewModelList[listCount].quiz.choice1 + " (1"
                tvQuizChoice2.text = reviewModelList[listCount].quiz.choice2 + " (2"
                tvQuizChoice3.text = reviewModelList[listCount].quiz.choice3 + " (3"
                tvQuizChoice4.text = reviewModelList[listCount].quiz.choice4 + " (4"
            } else {
                tvQuizChoice1.text = "1) " + reviewModelList[listCount].quiz.choice1
                tvQuizChoice2.text = "2) " + reviewModelList[listCount].quiz.choice2
                tvQuizChoice3.text = "3) " + reviewModelList[listCount].quiz.choice3
                tvQuizChoice4.text = "4) " + reviewModelList[listCount].quiz.choice4
            }


            alignChoices()
            setPreviewAnswer()

            tvQuizCommentary.text = getString(R.string.commentary)+" : " + reviewModelList[listCount].quiz.description
            tvQuizReviewQuestion.text = reviewModelList[listCount].question
            btnYes.setOnClickListener {
                btnYes.isEnabled = false
                btnNo.isEnabled = false
                reviewQuiz(ANSWER_YES)
            }
            btnNo.setOnClickListener {
                btnYes.isEnabled = false
                btnNo.isEnabled = false
                reviewQuiz(ANSWER_NO)
            }
            llQuizReportWrapper.setOnClickListener {
                if(!reviewModelList[listCount].isReported) {
                    reportQuiz()
                }
                else{
                    Util.showDefaultIdolDialogWithBtn1(
                        this@IdolQuizReviewActivity, null, resources.getString(
                            R.string.quiz_already_report_review
                        )
                    ) {
                        Util.closeIdolDialog()
                    }
                }
            }
        }
    }

    //두번째 문제부터 불리는 함수로, intent가 아닌 해당 액티비티에서 변경된 데이터 저장
    private fun quizReviewNext(reviewModelList: ArrayList<QuizReviewModel>, sessId : Int, mIdolId : Int, roundCount: Int, quizzes: Int, listCount: Int){
        this.reviewModelList = reviewModelList
        this.sessId = sessId
        this.mIdolId = mIdolId
        this.roundCount = roundCount
        this.quizzes = quizzes
        this.listCount = listCount
        with(binding) {
            btnYes.isEnabled = true
            btnNo.isEnabled = true

            //CardView BackGround는 코드상에서 해줘야됨.
            cvQuizFirstReview.background =
                ContextCompat.getDrawable(this@IdolQuizReviewActivity, R.drawable.quiz_shadow_radius)
            cvQuizSecondReview.background =
                ContextCompat.getDrawable(this@IdolQuizReviewActivity, R.drawable.quiz_shadow_radius)

            //퀴즈리뷰 object에   해당 리뷰리스트의  퀴즈 모델(현재 리뷰할 퀴즈 모델)을 넣어줌.
            mQuizReviewObject = reviewModelList[listCount]

            //여기로 넘어왔다는건 리스트가 빈값이 아니라는뜻이므로 첫번째 문제를 보여줌.
            tvQuizTitle.text = reviewModelList[listCount].quiz.content


            ivQuizContent.visibility = if (reviewModelList[listCount].quiz.imageUrl.isNullOrEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }

            val glideRequestManager: RequestManager = Glide.with(this@IdolQuizReviewActivity)

            if (reviewModelList[listCount].quiz.umjjalUrl == null) {
                ivQuizContent.post {
                    glideRequestManager
                        .load(reviewModelList[listCount].quiz.imageUrl)
                        .into(ivQuizContent)
                }
            } else {
                ivQuizContent.post {
                    glideRequestManager
                        .asGif()
                        .load(reviewModelList[listCount].quiz.imageUrl)
                        .into(ivQuizContent)
                }
            }

            if (Util.isRTL(this@IdolQuizReviewActivity)) {
                tvQuizChoice1.text = reviewModelList[listCount].quiz.choice1 + " (1"
                tvQuizChoice2.text = reviewModelList[listCount].quiz.choice2 + " (2"
                tvQuizChoice3.text = reviewModelList[listCount].quiz.choice3 + " (3"
                tvQuizChoice4.text = reviewModelList[listCount].quiz.choice4 + " (4"
            } else {
                tvQuizChoice1.text = "1) " + reviewModelList[listCount].quiz.choice1
                tvQuizChoice2.text = "2) " + reviewModelList[listCount].quiz.choice2
                tvQuizChoice3.text = "3) " + reviewModelList[listCount].quiz.choice3
                tvQuizChoice4.text = "4) " + reviewModelList[listCount].quiz.choice4
            }


//        alignChoices()
            setPreviewAnswer()
            tvQuizCommentary.text =
                getString(R.string.commentary) + " : " + reviewModelList[listCount].quiz.description
            tvQuizReviewQuestion.text = reviewModelList[listCount].question
            btnYes.setOnClickListener {
                btnYes.isEnabled = false
                btnNo.isEnabled = false
                reviewQuiz(ANSWER_YES)
            }

            btnNo.setOnClickListener {
                btnYes.isEnabled = false
                btnNo.isEnabled = false
                reviewQuiz(ANSWER_NO)
            }

            llQuizReportWrapper.setOnClickListener {
                if (!reviewModelList[listCount].isReported) {
                    reportQuiz()
                } else {
                    Util.showDefaultIdolDialogWithBtn1(
                        this@IdolQuizReviewActivity, null, resources.getString(
                            R.string.quiz_already_report_review
                        )
                    ) {
                        Util.closeIdolDialog()
                    }
                }
            }
        }
    }

    private fun reportQuiz() {
        val reportDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        reportDialog.window!!.attributes = lpWindow
        reportDialog.window!!.setLayout(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT)

        val dialogBinding = DialogQuizReportBinding.inflate(layoutInflater)
        reportDialog.setContentView(dialogBinding.root)
        reportDialog.setCanceledOnTouchOutside(false)
        reportDialog.setCancelable(true)

        dialogBinding.btnConfirm.setOnClickListener {
            val content = dialogBinding.quizReportContent.text.toString()

            dialogBinding.btnConfirm.isEnabled = false
            MainScope().launch {
                quizRepository.reportQuiz(
                    quizId = mQuizReviewObject.quiz.id,
                    content = content,
                    { response ->
                        if (response.optBoolean("success")) {
                            Util.showDefaultIdolDialogWithBtn1(this@IdolQuizReviewActivity,
                                null,
                                getString(R.string.report_done)
                            ) {
                                reviewModelList[listCount].isReported = true
                                Util.closeIdolDialog()
                                if (reportDialog.isShowing) {
                                    try {
                                        reportDialog.dismiss()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                }
                            }
                        } else {
                            UtilK.handleCommonError(this@IdolQuizReviewActivity, response)
                            dialogBinding.btnConfirm.isEnabled = true
                        }
                    },
                    { throwable ->
                        Toast.makeText(this@IdolQuizReviewActivity,
                                R.string.error_abnormal_exception,
                                Toast.LENGTH_SHORT).show()
                        if (Util.is_log()) {
                            showMessage(throwable.message)
                        }
                        dialogBinding.btnConfirm.isEnabled = true
                    }
                )
            }
        }

        dialogBinding.btnCancel.setOnClickListener { reportDialog.dismiss() }

        try {
            reportDialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            reportDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            reportDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun reviewQuiz(answer: Int) {
        setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION, "quiz_evaluation_submit")
        Util.showProgress(this, false)

        MainScope().launch {
            quizRepository.approveQuiz(
                sessionId = approveSessId,
                quizId = reviewModelList[listCount].quiz.id,
                answerNumber = reviewModelList[listCount].question_number,
                answer = answer,
                { response ->
                    Util.closeProgress()
                    if (response.optBoolean("success")) {
                        if(roundCount > 4 && (listCount+1) == quizzes){ //라운드 5이상이이고 현재문제 인덱스랑 서버로부터 가져온값(quizzes)가 같으면 그냥 종료해준다.(라운드끝)
                            sendQuizRewardHeart()
                            finish()
                            return@approveQuiz
                        }

                        listCount += 1
                        if(listCount + 1 > quizzes) { //quizzes보다 listCount가 더크면 마지막문제니까 다음라운드로.
                            if (quizzes > 2 && sessId != 0) {
                                //3개 이상있다면 다음라운드떄도 퀴즈가 있으므로 팝업을 띄워준다.(팝업은 API호출후 띄워주는걸로 합니다.)
                                getQuizReview(this@IdolQuizReviewActivity)
                            } else {
                                Util.showDefaultIdolDialogWithBtn1(
                                    this@IdolQuizReviewActivity,
                                    null,
                                    if (BuildConfig.CELEB) getString(R.string.actor_quiz_review_thankyou) else getString(
                                        R.string.quiz_review_thankyou
                                    ),
                                ) {
                                    Util.closeIdolDialog()
                                    sendQuizRewardHeart()
                                }
                            }
                        } else { //그렇지 않을경우엔 아직 문제가 남아있으므로 다음문제로.
                            Logger.v("QuizReview::다음문제로갑니다. ssId ${sessId} roundCount ${roundCount} quizzes ${quizzes} listCount ${listCount} idolId ${mIdolId}")
                            quizReviewNext(reviewModelList, sessId, mIdolId, roundCount, quizzes, listCount)
                        }
                    } else {
                        binding.btnYes.isEnabled = true
                        binding.btnNo.isEnabled = true

                        Util.showDefaultIdolDialogWithBtn1(this@IdolQuizReviewActivity,
                            null,
                            getString(R.string.msg_error_ok),
                            { Util.closeIdolDialog() },
                            true)
                    }
                },
                { throwable ->
                    Util.closeProgress()
                    binding.btnYes.isEnabled = true
                    binding.btnNo.isEnabled = true

                    Util.showDefaultIdolDialogWithBtn1(this@IdolQuizReviewActivity,
                        null,
                        getString(R.string.msg_error_ok),
                        { Util.closeIdolDialog() },
                        true)
                }

            )
        }
    }

    private fun showMoreReviewIdolQuizDialog(reviewModelList: ArrayList<QuizReviewModel>) {
        val reviewDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER

        if (reviewDialog.window != null) {
            reviewDialog.window!!.attributes = lpWindow
            val dm = resources.displayMetrics
            val width = (min(dm.widthPixels, dm.heightPixels) * 0.85f).toInt()
            reviewDialog.window!!.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val dialogBinding = DialogIdolQuizReviewMoreBinding.inflate(layoutInflater)
        reviewDialog.setContentView(dialogBinding.root)
        if(BuildConfig.CELEB) {
            reviewDialog.findViewById<AppCompatTextView>(R.id.title).text = getString(R.string.actor_quiz_review_thankyou)
        }

        reviewDialog.setCanceledOnTouchOutside(false)
        reviewDialog.setCancelable(false)

        dialogBinding.quizReviewMore.setOnClickListener {
            listCount = 0 //listCount는 다시 1로 변경.
            roundCount += 1 //round는 하나 올려주기.

            reviewDialog.dismiss()
            quizReviewNext(reviewModelList, sessId, mIdolId, roundCount, quizzes, listCount)
        }
        dialogBinding.goQuizMain.setOnClickListener {
            reviewDialog.dismiss()
            sendQuizRewardHeart()
        }

        try {
            reviewDialog.show()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    //퀴즈 리뷰 리스트 가져와줍니다.
    fun getQuizReview(context: Context?) {
        Util.showProgress(this, false)
        //debug는 서버에서 필요한값이라서 locale + idolId 합쳐서 보내줌.(디버그일때만 사용)
        var debug: String? = null

        if(BuildConfig.DEBUG){
            var locale = Util.getSystemLanguage(this)
            locale = when (locale) {
                "ko_KR" -> "ko"
                "zh_CN" -> "zh-cn"
                "zh_TW" -> "zh-tw"
                "ja_JP" -> "ja"
                else -> "en"
            }

            debug = locale + "," + mIdolId
        }

        MainScope().launch {
            quizRepository.getQuizReviewList(
                session = sessId,
                debug = debug,
                { response ->
                    Util.closeProgress()
                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response)
                        if (responseMsg != null) {
                            Toast.makeText(context, responseMsg, Toast.LENGTH_SHORT).show()
                        }
                        finish() // false떳을땐 그냥 종료시켜줌.
                    } else {
                        try {
                            quizzes = response.getInt("quizzes")
                            approveSessId = response.getInt("session")

                            val reviewModelList = ArrayList<QuizReviewModel>()
                            if (quizzes > 0) { //있으면 다음라운드로.

                                val gson = IdolGson.getInstance()
                                val array = response.getJSONArray("objects")

                                for (i in 0 until array.length()) {
                                    val model = gson.fromJson(
                                        array.getJSONObject(i).toString(),
                                        QuizReviewModel::class.java)
                                    reviewModelList.add(model)
                                }

                                showMoreReviewIdolQuizDialog(reviewModelList)

                            } else { //아니면 그냥 종료해주기.
                                finish()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                },
                { throwable ->
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                    finish()
                    Util.closeProgress()
                }
            )
        }
    }

    private fun getLongestChoice(): Int {
        val choices = mutableListOf<String>(
            reviewModelList[listCount].quiz.choice1,
            reviewModelList[listCount].quiz.choice2,
            reviewModelList[listCount].quiz.choice3,
            reviewModelList[listCount].quiz.choice4
        )
        var longestChoiceIndex = 0

        for (i in 0 until choices.size - 1) {
            longestChoiceIndex = if (choices[longestChoiceIndex].length >= choices[i + 1].length) {
                i
            } else {
                i + 1
            }
        }

        return longestChoiceIndex + 1
    }

    private fun alignChoices() {
        with(binding) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(clQuizChoicesWrapper)

            when (getLongestChoice()) {
                1 -> {
                    constraintSet.connect(tvQuizChoice1.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice1.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)

                    constraintSet.connect(tvQuizChoice2.id, ConstraintSet.START, tvQuizChoice1.id, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice3.id, ConstraintSet.START, tvQuizChoice1.id, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice4.id, ConstraintSet.START, tvQuizChoice1.id, ConstraintSet.START, 0)
                }
                2 -> {
                    constraintSet.connect(tvQuizChoice2.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice2.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)

                    constraintSet.connect(tvQuizChoice1.id, ConstraintSet.START, tvQuizChoice2.id, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice3.id, ConstraintSet.START, tvQuizChoice2.id, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice4.id, ConstraintSet.START, tvQuizChoice2.id, ConstraintSet.START, 0)
                }
                3 -> {
                    constraintSet.connect(tvQuizChoice3.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice3.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)

                    constraintSet.connect(tvQuizChoice1.id, ConstraintSet.START, tvQuizChoice3.id, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice2.id, ConstraintSet.START, tvQuizChoice3.id, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice4.id, ConstraintSet.START, tvQuizChoice3.id, ConstraintSet.START, 0)
                }
                4 -> {
                    constraintSet.connect(tvQuizChoice4.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice4.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)

                    constraintSet.connect(tvQuizChoice1.id, ConstraintSet.START, tvQuizChoice4.id, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice2.id, ConstraintSet.START, tvQuizChoice4.id, ConstraintSet.START, 0)
                    constraintSet.connect(tvQuizChoice3.id, ConstraintSet.START, tvQuizChoice4.id, ConstraintSet.START, 0)

                }
            }
            constraintSet.applyTo(clQuizChoicesWrapper)
        }
    }

    private fun setPreviewAnswer() {
        with(binding) {
            tvQuizChoice1.setTextColor(ContextCompat.getColorStateList(this@IdolQuizReviewActivity,R.color.gray900))
            tvQuizChoice2.setTextColor(ContextCompat.getColorStateList(this@IdolQuizReviewActivity,R.color.gray900))
            tvQuizChoice3.setTextColor(ContextCompat.getColorStateList(this@IdolQuizReviewActivity,R.color.gray900))
            tvQuizChoice4.setTextColor(ContextCompat.getColorStateList(this@IdolQuizReviewActivity,R.color.gray900))

            val constraintSet = ConstraintSet()
            constraintSet.clone(clQuizChoicesWrapper)

            val tvAnswer = TextView(this@IdolQuizReviewActivity)
            tvAnswer.id = R.id.tv_answer
            tvAnswer.text = if (Util.isRTL(this@IdolQuizReviewActivity)) {
                " : "+getString(R.string.quiz_write_answer)
            } else {
                getString(R.string.quiz_write_answer)+" : "
            }
            tvAnswer.setTextColor(
                ContextCompat.getColorStateList(this@IdolQuizReviewActivity, R.color.main))
            tvAnswer.textSize = 11f
            clQuizChoicesWrapper.addView(tvAnswer)
            constraintSet.constrainWidth(tvAnswer.id, ConstraintSet.WRAP_CONTENT)

            when (reviewModelList[listCount].quiz.answer) {
                1 -> {
                    constraintSet.connect(tvAnswer.id, ConstraintSet.TOP, tvQuizChoice1.id, ConstraintSet.TOP, 0)
                    constraintSet.connect(tvAnswer.id, ConstraintSet.BOTTOM, tvQuizChoice1.id, ConstraintSet.BOTTOM, 0)
                    constraintSet.connect(tvAnswer.id, ConstraintSet.END, tvQuizChoice1.id, ConstraintSet.START, 0)
                    constraintSet.constrainHeight(tvAnswer.id, tvQuizChoice1.height)

                    tvQuizChoice1.setTextColor(ContextCompat
                        .getColorStateList(this@IdolQuizReviewActivity, R.color.main))
                }
                2 -> {
                    constraintSet.connect(tvAnswer.id, ConstraintSet.TOP, tvQuizChoice2.id, ConstraintSet.TOP, 0)
                    constraintSet.connect(tvAnswer.id, ConstraintSet.BOTTOM, tvQuizChoice2.id, ConstraintSet.BOTTOM, 0)
                    constraintSet.connect(tvAnswer.id, ConstraintSet.END, tvQuizChoice2.id, ConstraintSet.START, 0)
                    constraintSet.constrainHeight(tvAnswer.id, tvQuizChoice2.height)

                    tvQuizChoice2.setTextColor(ContextCompat
                        .getColorStateList(this@IdolQuizReviewActivity, R.color.main))
                }
                3 -> {
                    constraintSet.connect(tvAnswer.id, ConstraintSet.TOP, tvQuizChoice3.id, ConstraintSet.TOP, 0)
                    constraintSet.connect(tvAnswer.id, ConstraintSet.BOTTOM, tvQuizChoice3.id, ConstraintSet.BOTTOM, 0)
                    constraintSet.connect(tvAnswer.id, ConstraintSet.END, tvQuizChoice3.id, ConstraintSet.START, 0)
                    constraintSet.constrainHeight(tvAnswer.id, tvQuizChoice3.height)

                    tvQuizChoice3.setTextColor(ContextCompat
                        .getColorStateList(this@IdolQuizReviewActivity, R.color.main))
                }
                4 -> {
                    constraintSet.connect(tvAnswer.id, ConstraintSet.TOP, tvQuizChoice4.id, ConstraintSet.TOP, 0)
                    constraintSet.connect(tvAnswer.id, ConstraintSet.BOTTOM, tvQuizChoice4.id, ConstraintSet.BOTTOM, 0)
                    constraintSet.connect(tvAnswer.id, ConstraintSet.END, tvQuizChoice4.id, ConstraintSet.START, 0)
                    constraintSet.constrainHeight(tvAnswer.id, tvQuizChoice4.height)

                    tvQuizChoice4.setTextColor(ContextCompat
                        .getColorStateList(this@IdolQuizReviewActivity, R.color.main))
                }

            }
            constraintSet.applyTo(clQuizChoicesWrapper)
            //정답 안보이게 해달라고해서 gone처리.
            tvAnswer.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        Util.showDialogCancelRedBtn2(this,getString(R.string.quiz_ask_finish_review),"","",R.string.yes, R.string.no,
            {
                Util.closeIdolDialog()
                super.onBackPressed()
            },{
                Util.closeIdolDialog()
            })
    }

    private fun sendQuizRewardHeart() {
        val intent = Intent()
        intent.putExtra(IdolQuizMainActivity.QUIZ_REWARD_HEART, quizRewardHeart)
        setResult(IdolQuizMainActivity.QUIZ_SOLVE_RESULT_CODE, intent)
        finish()
    }

    companion object {
        const val QUIZ_REVIEW_LIST = "quizReviewList"
        const val QUIZ_SESS_ID = "quizSessid"
        const val QUIZ_IDOL_ID = "quizIdolId"
        const val QUIZ_ROUND_COUNT = "quizRoundCount"
        const val QUIZ_QUIZZES = "quizQuizzes"
        const val QUIZ_LIST_COUNT = "quizListCount"
        const val QUIZ_REWARD_HEART = "quizRewardHeart"
        const val ANSWER_YES = 1
        const val ANSWER_NO = 0

        @JvmStatic
        fun createIntent(context: Context,
                         reviewModelList: ArrayList<QuizReviewModel>,
                         sessId: Int,
                         idolId:Int,
                         roundCount: Int,
                         quizzes: Int,
                         listCount: Int,
                         quizRewardHeart: Int): Intent {
            val intent = Intent(context, IdolQuizReviewActivity::class.java)
            val args = Bundle()

            args.putSerializable(QUIZ_REVIEW_LIST, reviewModelList)
            args.putInt(QUIZ_SESS_ID, sessId)
            args.putInt(QUIZ_IDOL_ID, idolId)
            args.putInt(QUIZ_ROUND_COUNT, roundCount)
            args.putInt(QUIZ_QUIZZES, quizzes)
            args.putInt(QUIZ_LIST_COUNT, listCount)
            args.putInt(QUIZ_REWARD_HEART, quizRewardHeart)
            intent.putExtras(args)
            return intent
        }
    }
}