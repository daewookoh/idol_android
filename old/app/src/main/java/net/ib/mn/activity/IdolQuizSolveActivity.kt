/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.admob.GntBannerWithRadius
import net.ib.mn.core.data.model.QuizTodayModel
import net.ib.mn.core.data.repository.QuizRepositoryImpl
import net.ib.mn.databinding.ActivityQuizSolveBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.ReportReasonDialogFragment
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.model.QuizModel
import net.ib.mn.model.QuizReviewModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * @see
 * */

@AndroidEntryPoint
class IdolQuizSolveActivity :
    BaseActivity(),
    View.OnClickListener,
    BaseDialogFragment.DialogResultHandler {

    private var quizCur: Int = 0
    private var answer: Int = 0
    private var r1: Int = 0
    private var r2: Int = 0
    private var userId: Int = 0
    private var cAnswer: Long = 0
    private var mIdolId: Int = 0
    private var mType: String? = null
    private var description: String? = null
    private var modelList: ArrayList<QuizModel> = ArrayList()
    private var reviewModelList: ArrayList<QuizReviewModel> = ArrayList()

    // 추가적인 변수들
    private var quizTodayModel: QuizTodayModel? = null
    private var quizzes: Int = 0
    private var totalCount: Int = 0
    private var todayQuizMax: Int = Const.TODAY_QUIZ_MAX
    private var todayQuizMin: Int = Const.TODAY_QUIZ_MIN
    private var corrects: String? = null
    private var incorrects: String? = null
    private var quizId: Int = 0
    private var sessId: Int = 0
    private var quizFin: Boolean = false
    private var frameAnimation: AnimationDrawable? = null
    private var continueLimit: Int = 0

    private var reportReasonDialogFragment: ReportReasonDialogFragment? = null
    private var mQuizReviewObject: JSONObject? = null
    private var idolName: String? = null
    private var thread: TimeCheck? = null
    private var hasChooseWrongAnswer = false
    private var countCorrectAnswer = 0

    private lateinit var binding: ActivityQuizSolveBinding
    private var isCompleteAdShow = false

    @Inject
    lateinit var quizRepository: QuizRepositoryImpl

    interface OnSubmitAnswersListener {
        fun onSubmitAnswers()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_quiz_solve)
        binding.clContainer.applySystemBarInsets()
        binding.composeAdBanner.setContent {
            GntBannerWithRadius(
                adUnitId = if (BuildConfig.DEBUG) {
                    Const.ADMOB_NATIVE_AD_TEST_UNIT_ID
                } else {
                    if (BuildConfig.CELEB) {
                        Const.ADMOB_NATIVE_AD_ACTOR_UNIT_ID
                    } else {
                        Const.ADMOB_NATIVE_AD_UNIT_ID
                    }
                }
                ,
                loadSuccess = { isSuccess ->
                    if (isSuccess) {
                        return@GntBannerWithRadius
                    }

                    binding.composeAdBanner.visibility = View.GONE
                }
            )
        }

        // 앵커 뷰에 하단 inset 반영 (시스템바/IME 모두)
        ViewCompat.setOnApplyWindowInsetsListener(binding.snackbarAnchor) { v, insets ->
            val bottom = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            ).bottom
            v.layoutParams = v.layoutParams.apply { height = bottom }
            insets
        }

        initData()
        initUI()

        if (savedInstanceState != null) {
            // 액티비티가 재생성된 경우, 저장된 퀴즈 상태를 복원합니다.
            quizCur = savedInstanceState.getInt("quizCur")
            answer = savedInstanceState.getInt("answer")
            r1 = savedInstanceState.getInt("r1")
            r2 = savedInstanceState.getInt("r2")
            cAnswer = savedInstanceState.getLong("cAnswer")
            description = savedInstanceState.getString("description")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                modelList = savedInstanceState.getParcelableArrayList("modelList", QuizModel::class.java) ?: ArrayList()
                reviewModelList = savedInstanceState.getParcelableArrayList("reviewModelList", QuizReviewModel::class.java) ?: ArrayList()
            } else {
                @Suppress("DEPRECATION")
                modelList = savedInstanceState.getParcelableArrayList("modelList") ?: ArrayList()
                @Suppress("DEPRECATION")
                reviewModelList = savedInstanceState.getParcelableArrayList("reviewModelList") ?: ArrayList()
            }
            quizzes = savedInstanceState.getInt("quizzes")
            totalCount = savedInstanceState.getInt("totalCount")
            corrects = savedInstanceState.getString("corrects")
            incorrects = savedInstanceState.getString("incorrects")
            quizId = savedInstanceState.getInt("quizId")
            sessId = savedInstanceState.getInt("sessId")
            quizFin = savedInstanceState.getBoolean("quizFin")
            continueLimit = savedInstanceState.getInt("continueLimit")
            hasChooseWrongAnswer = savedInstanceState.getBoolean("hasChooseWrongAnswer")
            countCorrectAnswer = savedInstanceState.getInt("countCorrectAnswer")

            // UI 상태 복원
            if (hasChooseWrongAnswer) {
                // 오답 화면(이어하기)을 다시 표시
                showQuizContinueFrame(isVisible = true)
            } else {
                if (!quizFin) {
                    settingQuiz()
                } else {
                    // 퀴즈가 끝난 상태였다면 결과 화면 표시
                    showQuizContinueFrame(isVisible = true)
                }
            }
        } else {
            // 액티비티가 처음 생성된 경우, 퀴즈 목록을 가져옵니다.
            getQuizList()
        }

        checkIsCompleteAdShow()
    }

    private fun initData() {
        mIdolId = intent.getIntExtra(EXTRA_IDOL, 0)
        quizTodayModel = intent.getParcelableExtra(EXTRA_QUIZ_TODAY_MODEL)
        idolName = intent.getStringExtra(EXTRA_IDOL_NAME)
        isCompleteAdShow = intent.getBooleanExtra(EXTRA_IS_COMPLETE_AD_SHOW, false)
        quizTodayModel?.let {
            todayQuizMax = it.quizMaxItems
            todayQuizMin = it.quizMinItems
        }
        mType = intent.getStringExtra(EXTRA_TYPE)
        quizCur = 0
        quizFin = false

        corrects = ""
        incorrects = ""
        sessId = -1
        userId = IdolAccount.getAccount(this)?.userModel?.id ?: 0

        thread = TimeCheck()
        thread?.init()
    }

    private fun initUI() = with(binding) {
        val actionbar = supportActionBar
        actionbar?.setTitle(if (BuildConfig.CELEB) R.string.actor_menu_quiz else R.string.menu_quiz)

        val timerDrawable = if (BuildConfig.CELEB) {
            ContextCompat.getDrawable(this@IdolQuizSolveActivity, R.drawable.icon_quiz_timer_celeb)
        } else {
            ContextCompat.getDrawable(this@IdolQuizSolveActivity, R.drawable.icon_quiz_timer)
        }

        ivTimer.setImageDrawable(timerDrawable)

        quizWriterLabel.text = if (Util.isRTL(this@IdolQuizSolveActivity)) {
            " : ${getString(R.string.quiz_writer)}"
        } else {
            "${getString(R.string.quiz_writer)} : "
        }

        val draw = ContextCompat.getDrawable(this@IdolQuizSolveActivity, R.drawable.progressbar_quiz)
        pbTimeLimit.progressDrawable = draw

        try {
            quizSolve.setBackgroundResource(R.drawable.animation_quiz_solve)
            frameAnimation = quizSolve.background as AnimationDrawable
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }

        tvQuizNumber.background = if (BuildConfig.CELEB) {
            ContextCompat.getDrawable(this@IdolQuizSolveActivity, R.drawable.icon_quiz_number_celeb)
        } else {
            ContextCompat.getDrawable(this@IdolQuizSolveActivity, R.drawable.icon_quiz_number)
        }

        quizChoice1.setOnClickListener(this@IdolQuizSolveActivity)
        quizChoice2.setOnClickListener(this@IdolQuizSolveActivity)
        quizChoice3.setOnClickListener(this@IdolQuizSolveActivity)
        quizChoice4.setOnClickListener(this@IdolQuizSolveActivity)
        liQuizReport.setOnClickListener(this@IdolQuizSolveActivity)
        clContinue.setOnClickListener(this@IdolQuizSolveActivity)
        tvQuit.setOnClickListener(this@IdolQuizSolveActivity)

    }

    private fun checkIsCompleteAdShow() {
        if (isCompleteAdShow) {
            thread?.stopTimer()

            Util.showDefaultIdolDialogWithBtn1(
                this,
                null,
                getString(R.string.quiz_retry_ad_done)
            ) {
                //광고 타고 들어왔다면 확인 누른후 타이머 시작
                Util.closeIdolDialog()

                isCompleteAdShow = false
                thread?.restartTimer()
            }
            return
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.quizChoice1.id -> if (thread?.isRunning() == true) quizSolve(1)
            binding.quizChoice2.id -> if (thread?.isRunning() == true) quizSolve(2)
            binding.quizChoice3.id -> if (thread?.isRunning() == true) quizSolve(3)
            binding.quizChoice4.id -> if (thread?.isRunning() == true) quizSolve(4)
            binding.liQuizReport.id -> {
                reportReasonDialogFragment = ReportReasonDialogFragment.getInstance(
                    ReportReasonDialogFragment.QUIZ_REPORT,
                    quizId,
                )
                reportReasonDialogFragment?.show(supportFragmentManager, "report_reason")
            }
            binding.clContinue.id -> {
                // 이어하기 할 수 없거나, 게임이 완전 종료되면 액티비티 종료.
                if (continueLimit <= 0 || quizFin) {
                    if (countCorrectAnswer > 10 && reviewModelList.size > 0) {
                        showReviewIdolQuizDialog()
                    } else {
                        sendQuizRewardHeart()
                    }
                    return
                }

                startActivityForResult(
                    MezzoPlayerActivity.createIntent(this, Const.ADMOB_REWARDED_VIDEO_QUIZ_UNIT_ID),
                    MEZZO_PLAYER_REQ_CODE
                )
            }
            binding.tvQuit.id -> {
                showQuitRewardBottomSheet()
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        //퀴즈가 다끝나지 않고 , 잘못된 정답을 골랐을경우 이어하기 팝업 보여줌.
        if (!quizFin && hasChooseWrongAnswer && continueLimit > 0) {
            showQuitRewardBottomSheet()
            return
        }

        //퀴즈가 다끝나지 않고, 잘못된 정답을 고르지 않았을때 (퀴즈를 계속 풀수 있을때)
        if (!quizFin && !hasChooseWrongAnswer) {
            var msg =
                SpannableString("${getString(R.string.quiz_confirm_exit2)}\n${getString(R.string.quiz_confirm_exit)}")
            msg = Util.getColorText(
                msg,
                getString(R.string.quiz_confirm_exit_highlight1),
                ContextCompat.getColor(this, R.color.main),
            )
            msg = Util.getColorText(
                msg,
                getString(R.string.quiz_confirm_exit_highlight2),
                ContextCompat.getColor(this, R.color.main),
            )

            Util.showDefaultIdolDialogWithBtn2(
                this,
                null,
                msg.toString(),
                R.string.yes,
                R.string.no,
                false, true,
                { ok ->
                    Util.closeIdolDialog()
                    thread?.stopTimer()
                    // 퀴즈 도중 뒤로가기 눌렀을 때, 현재 문제를 틀리게 한다.
                    quizSolve(-2)
                },
                { no ->
                    Util.closeIdolDialog()
                },
            )

            return
        }

        thread?.stopTimer()
        sendQuizRewardHeart()
    }

    private fun showReviewIdolQuizDialog() {
        val reviewDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar).apply {
            window?.let { window ->
                val lpWindow = WindowManager.LayoutParams().apply {
                    flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
                    dimAmount = 0.7f
                    gravity = Gravity.CENTER
                }
                window.attributes = lpWindow
                window.setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }

            setContentView(R.layout.dialog_quiz_review)

            if (BuildConfig.CELEB) {
                findViewById<AppCompatTextView>(R.id.message).text =
                    getString(R.string.actor_menu_quiz)
                findViewById<AppCompatTextView>(R.id.message3).text =
                    getString(R.string.actor_quiz_review_desc)
            }

            findViewById<AppCompatImageView>(R.id.img_review).bringToFront()
            findViewById<AppCompatButton>(R.id.btn_close).setOnClickListener {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.QUIZ_EVALUTAION_REJECT.actionValue,
                    GaAction.QUIZ_EVALUTAION_REJECT.label,
                )
                dismiss()
                thread?.stopTimer()
                sendQuizRewardHeart()
            }
            findViewById<AppCompatButton>(R.id.btn_review).apply {
                text = String.format("${getString(R.string.quiz_review_enter)} %s", ">")
                setOnClickListener {
                    setUiActionFirebaseGoogleAnalyticsActivity(
                        Const.ANALYTICS_BUTTON_PRESS_ACTION,
                        "quiz_evaluation_enter",
                    )
                    dismiss()
                    thread?.stopTimer()
                    startActivity(
                        IdolQuizReviewActivity.createIntent(
                            this@IdolQuizSolveActivity,
                            reviewModelList,
                            sessId,
                            mIdolId,
                            1,
                            quizzes,
                            0,
                            countCorrectAnswer,
                        ).addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT),
                    )
                    finish()
                }
            }

            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }

        try {
            reviewDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun quizSolve(choice: Int) = with(binding) {
        quizDescription.text = description
        quizDescription.visibility = View.VISIBLE
        quizChoiceWrapper.visibility = View.GONE
        ivQuizContentImage.visibility = View.GONE
        ivQuizContentImageXo.visibility = View.VISIBLE
        liQuizReport.visibility = View.GONE

        thread?.stopTimer()

        if (quizCur == 8) {
            getQuizReview(this@IdolQuizSolveActivity, sessId)
        }

        quizCur++
        quizFin = quizCur == todayQuizMax || quizCur == modelList.size
        if (choice == answer) {
            hasChooseWrongAnswer = false
            quizSolve.bringToFront()
            quizSolve.visibility = View.VISIBLE
            frameAnimation?.start()

            corrects += if (corrects.isNullOrEmpty()) quizId.toString() else ",$quizId"
            countCorrectAnswer++
            ivQuizContentImageXo.setImageResource(R.drawable.icon_quiz_o)
            if (BuildConfig.CELEB) {
                ivQuizContentImageXo.setColorFilter(
                    ContextCompat.getColor(this@IdolQuizSolveActivity, R.color.main),
                    android.graphics.PorterDuff.Mode.SRC_IN,
                )
            }
            quizContent.text = getString(R.string.quiz_correct)

            quizAnswerSubmit(this@IdolQuizSolveActivity, null)

            // 만약 퀴즈가 전부 끝났다면.
            if (!quizFin) {
                Handler().postDelayed({
                    frameAnimation?.stop()
                    settingQuiz()
                }, 2000)
                showQuizContinueFrame(isVisible = false)
            } else {
                showQuizContinueFrame(isVisible = true)
            }
        } else {
            hasChooseWrongAnswer = true
            incorrects += quizId.toString()
            quizAnswerSubmit(this@IdolQuizSolveActivity, null)
            ivQuizContentImageXo.setImageResource(R.drawable.icon_quiz_x)
            if (BuildConfig.CELEB) {
                ivQuizContentImageXo.setColorFilter(
                    ContextCompat.getColor(this@IdolQuizSolveActivity, R.color.main),
                    android.graphics.PorterDuff.Mode.SRC_IN,
                )
            }
            quizContent.text =
                if (choice == -1) getString(R.string.quiz_time_over) else getString(R.string.quiz_incorrect)
            liQuizReport.visibility = View.VISIBLE
            showQuizContinueFrame(isVisible = true)
        }
    }

    inner class TimeCheck {

        private var expire: Long = 0
        private var job: Job? = null
        private var run: Boolean = false

        fun init() {
            expire = System.currentTimeMillis() + 15 * 1000
            binding.pbTimeLimit.progress = 15000
        }

        private fun startTimer() {
            run = true
            job = CoroutineScope(Dispatchers.Main).launch {
                while (run) {
                    val time = (expire - System.currentTimeMillis()).toInt()
                    binding.pbTimeLimit.progress = if (time < 0) 0 else time
                    if (time < 0) {
                        stopTimer()
                        quizSolve(-1)
                    }
                    delay(50)
                }
            }
        }

        fun stopTimer() {
            run = false
            job?.cancel()
        }

        fun isRunning(): Boolean = run

        fun restartTimer() {
            stopTimer()
            init()
            startTimer()
        }
    }

    fun settingQuiz(isCompleteAdShow: Boolean = false) = with(binding) {
        // View visibility setup
        ivQuizContentImage.visibility = View.GONE
        ivQuizContentImageXo.visibility = View.GONE
        quizDescription.visibility = View.GONE
        quizChoiceWrapper.visibility = View.GONE
        liQuizReport.visibility = View.GONE
        quizSolve.visibility = View.GONE

        quizId = modelList[quizCur].id
        // Calculate quiz answer
        cAnswer = modelList[quizCur].c_answer
        val q = quizId % 90 + 10
        val u = userId % 990 + 10
        val s = (r1 + q) % 8
        val t = r2 % 8
        answer = (((r1 * t).toLong() xor cAnswer shr s) - q * u * r2).toInt() / r1

        description = modelList[quizCur].description

        tvQuizNumber.text = String.format(LocaleUtil.getAppLocale(this@IdolQuizSolveActivity), "Q %d", quizCur + 1)
        tvQuizNumberTitle.text = idolName
        // For debugging purposes - to easily see the correct answer (exists in iOS too)
        if (false) { // The condition here is always false; adjust as needed for debugging
            binding.tvQuizNumber.text = String.format(Locale.US, "Q %d 정답: %d", quizCur + 1, answer)
        }

        quizContent.text = modelList[quizCur].content

        quizChoice1.text = modelList[quizCur].choice1
        quizChoice2.text = modelList[quizCur].choice2
        quizChoice3.text = modelList[quizCur].choice3
        quizChoice4.text = modelList[quizCur].choice4

        if (modelList[quizCur].user == null) {
            quizWriterLabel.visibility = View.GONE
            quizWriterIcon.visibility = View.GONE
            quizWriter.visibility = View.GONE
        } else {
            quizWriterLabel.visibility = View.VISIBLE
            quizWriterIcon.visibility = View.VISIBLE
            quizWriter.visibility = View.VISIBLE
            quizWriterIcon.setImageBitmap(
                Util.getLevelImage(
                    this@IdolQuizSolveActivity,
                    modelList[quizCur].user,
                ),
            )
            quizWriter.text = modelList[quizCur].user?.nickname
        }

        if (!modelList[quizCur].imageUrl.isNullOrEmpty()) {
            try {
                Glide.with(this@IdolQuizSolveActivity)
                    .asBitmap()
                    .load(modelList[quizCur].imageUrl)
                    .transform(CenterCrop(), RoundedCorners(26))
                    .listener(object : RequestListener<Bitmap> {

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>,
                            isFirstResource: Boolean,
                        ): Boolean {
                            showReloadDialog()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            model: Any,
                            target: Target<Bitmap>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean,
                        ): Boolean {
                            runOnUiThread {
                                ivQuizContentImage.visibility = View.VISIBLE
                                quizChoiceWrapper.visibility = View.VISIBLE
                                //광고 보고 왔으면 타이머 시작 대기.
                                if (isCompleteAdShow) {
                                    return@runOnUiThread
                                }
                                thread?.restartTimer()
                            }
                            return false
                        }
                    })
                    .into(ivQuizContentImage)
            } catch (e: Exception) {
                showReloadDialog()
            }
        } else {
            quizChoiceWrapper.visibility = View.VISIBLE
            //광고 보고 왔으면 타이머 시작 대기.
            if (isCompleteAdShow) {
                thread?.stopTimer()
                return@with
            }
            thread?.restartTimer()
        }
    }

    private fun setQuizReviewObject(response: JSONObject) {
        try {
            mQuizReviewObject = JSONObject()
            mQuizReviewObject!!
                .put(
                    QuizReviewModel.QUESTION,
                    response.getString(QuizReviewModel.QUESTION),
                )
                .put(
                    QuizReviewModel.QUESTION_NUMBER,
                    response.getInt(QuizReviewModel.QUESTION_NUMBER),
                )
                .put(
                    QuizReviewModel.QUIZ,
                    response.getJSONObject(QuizReviewModel.QUIZ),
                )
        } catch (e: java.lang.Exception) {
            mQuizReviewObject = null
            e.printStackTrace()
        }
    }

    private fun getQuizList() {
        Util.showLottie(this, true)
        lifecycleScope.launch {
            quizRepository.getQuizList(if(mIdolId == 0) null else mIdolId,
                mType,
                getQuiz@ { response ->
                    val gson = IdolGson.getInstance()
                    modelList.clear()
                    Util.closeProgress()
                    if (!response.optBoolean("success")) {
                        if (isCompleteAdShow) {
                            Util.closeIdolDialog()
                        }
                        val msg = response.optString("msg")
                        Util.showDefaultIdolDialogWithBtn1(
                            this@IdolQuizSolveActivity,
                            null,
                            msg,
                        ) {
                            Util.closeIdolDialog()
                            thread?.stopTimer()
                            finish()
                        }
                        return@getQuiz
                    }

                    try {
                        totalCount = response.getJSONArray("objects").length()
                        sessId = if (sessId == -1) response.optInt("session") else sessId
                        continueLimit = response.optInt("continue_limit", 0)

                        r1 = response.getInt("r1")
                        r2 = response.getInt("r2")
                        if (totalCount < todayQuizMin) {
                            if (isCompleteAdShow) {
                                Util.closeIdolDialog()
                            }
                            Util.showDefaultIdolDialogWithBtn1(
                                this@IdolQuizSolveActivity,
                                null,
                                if (BuildConfig.CELEB) {
                                    getString(R.string.quiz_celeb_unavailable_not_enough)
                                } else {
                                    getString(
                                        R.string.quiz_unavailable_not_enough,
                                    )
                                },
                            ) {
                                Util.closeIdolDialog()
                                thread?.stopTimer()
                                finish()
                            }
                            return@getQuiz
                        }

                        val array = response.getJSONArray("objects")
                        for (i in 0 until array.length()) {
                            val model = gson.fromJson(
                                array.getJSONObject(i).toString(),
                                QuizModel::class.java,
                            )
                            modelList.add(model)
                        }
                        settingQuiz(isCompleteAdShow)
                        setQuizReviewObject(response)

//                        thread?.restartTimer(692)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Util.closeProgress()
                    }
                }, {
                    Util.closeProgress()
                }
            )
        }
    }

    private fun getQuizReview(context: Context, sessId: Int) {
        var debug: String? = null

        if (BuildConfig.DEBUG) {
            val locale = Util.getSystemLanguage(this).also {
                when (it) {
                    "ko_KR" -> "ko"
                    "zh_CN" -> "zh-cn"
                    "zh_TW" -> "zh-tw"
                    "ja_JP" -> "ja"
                    else -> "en"
                }
            }

            debug = "$locale,$mIdolId"
        }

        MainScope().launch {
            quizRepository.getQuizReviewList(
                sessId,
                debug,
                { response ->
                    if (!response.optBoolean("success")) {
                        UtilK.handleCommonError(context, response)

                        finish()
                    } else {
                        try {
                            quizzes = response.getInt("quizzes")
                            val gson = IdolGson.getInstance()

                            val array = response.getJSONArray("objects")
                            for (i in 0 until array.length()) {
                                val model = gson.fromJson(
                                    array.getJSONObject(i).toString(),
                                    QuizReviewModel::class.java,
                                )
                                reviewModelList.add(model)
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
                },
            )
        }
    }

    private fun quizAnswerSubmit(context: Context, listener: OnSubmitAnswersListener?) {
        Util.showProgress(this, false)
        MainScope().launch {
            quizRepository.postQuizAnswer(
                corrects,
                incorrects,
                sessId,
                if(mIdolId > 0) mIdolId else null,
                { response ->
                    if (!response.optBoolean("success")) {
                        UtilK.handleCommonError(context, response)
                    } else {
                        listener?.onSubmitAnswers()
                    }
                    Util.closeProgress()
                },
                { throwable ->
                    Util.is_log().takeIf { it }?.let {
                        throwable.message?.let { it1 -> showMessage(it1) }
                    }
                    Util.closeProgress()
                }
            )
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == ResultCode.REPORT_REASON_UPLOADED.value) {
            // When the report reason writing is completed
            if (data?.getIntExtra(
                    ReportReasonDialogFragment.QUIZ_REPORT_VALUE_KEY,
                    1,
                ) == ReportReasonDialogFragment.QUIZ_REPORT_SUCCESS
            ) {
                Util.showDefaultIdolDialogWithBtn1(
                    this,
                    null,
                    getString(R.string.report_done),
                ) {
                    Util.closeIdolDialog()
                    sendQuizRewardHeart()
                }
            } else {
                data?.getStringExtra(ReportReasonDialogFragment.QUIZ_REPORT_FAIL_MSG_VALUE_KEY)
                    ?.let {
                        Util.showDefaultIdolDialogWithBtn1(
                            this,
                            null,
                            it,
                        ) {
                            Util.closeIdolDialog()
                            finish()
                        }
                    }
            }
        }
    }

    private fun showReloadDialog() {
        Util.showDefaultIdolDialogWithBtn2(
            this,
            null,
            getString(R.string.error_abnormal_default),
            R.string.btn_retry,
            R.string.btn_cancel,
            true, false,
            {
                Util.closeIdolDialog()
                settingQuiz()
            },
            {
                Util.closeIdolDialog()
                finish()
            },
        )
    }

    private fun sendQuizRewardHeart() {

        thread?.stopTimer()

        val intent = Intent().apply {
            putExtra(IdolQuizMainActivity.QUIZ_REWARD_HEART, countCorrectAnswer)
        }
        setResult(IdolQuizMainActivity.QUIZ_SOLVE_RESULT_CODE, intent)
        finish()
    }

    private fun requestQuizContinue() {
        MainScope().launch {
            quizRepository.continueQuiz(
                sessId,
                { response ->
                    if (!response.optBoolean("success")) {
                        return@continueQuiz
                    }

                    // 이어하기 카운트 차감.
                    continueLimit--
                    showQuizContinueFrame(isVisible = false)
                    settingQuiz()
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

    private fun showQuizContinueFrame(isVisible: Boolean = false) = with(binding) {
        if (!isVisible) {
            liQuizFailed.visibility = View.GONE
            return
        }

        liQuizFailed.visibility = View.VISIBLE
        tvQuizSuccess.text =
            if (quizFin && !hasChooseWrongAnswer) {//퀴즈 모두 종료하고, 정답을 맞췄을경우 완료 메시지 띄워줌.
                Util.getColorText(
                    String.format(getString(R.string.quiz_complete), countCorrectAnswer),
                    quizCur.toString() + "",
                    ContextCompat.getColor(this@IdolQuizSolveActivity, R.color.main)
                )
            } else {// 이외 상황은 정답 개수만 보여줍닌다.
                if (Util.getSystemLanguage(this@IdolQuizSolveActivity) == "ko_KR") getQuizSuccessTitleSpan() else String.format(
                    getString(R.string.quiz_fail),
                    countCorrectAnswer
                )
            }


        //이어하기가 할 수 없거나, 게임이 완전히 종료됬으면 확인버튼 보여줍니다.
        if (continueLimit <= 0 || quizFin) {
            tvQuizSuccessSubTitle.visibility = View.GONE
            tvQuit.visibility = View.GONE
            tvContinue.text = getString(R.string.confirm)
            ivContinue.visibility = View.GONE
            return
        }

        tvQuizSuccessSubTitle.visibility = View.VISIBLE
        tvQuit.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            MEZZO_PLAYER_REQ_CODE -> {

                if (resultCode == RESULT_CANCELED) {
                    Util.handleVideoAdResult(
                        this, true, true, requestCode, resultCode, data, "", binding.snackbarAnchor
                    ) {}
                    return
                }

                Util.showDefaultIdolDialogWithBtn1(
                    this,
                    null,
                    getString(R.string.quiz_continue_ad_done)
                ) {
                    Util.closeIdolDialog()
                    requestQuizContinue()
                }
            }
        }
    }

    private fun showQuitRewardBottomSheet() {
        val rewardBottomSheet = RewardBottomSheetDialogFragment.newInstance(
            RewardBottomSheetDialogFragment.FLAG_QUIZ_CONTINUE,
            quizContinue = {
                if (continueLimit <= 0 || quizFin) {
                    sendQuizRewardHeart()
                    return@newInstance
                }

                startActivityForResult(
                    MezzoPlayerActivity.createIntent(this, Const.ADMOB_REWARDED_VIDEO_QUIZ_UNIT_ID),
                    MEZZO_PLAYER_REQ_CODE
                )
            }, dismiss = {
                if (countCorrectAnswer > 10 && reviewModelList.size > 0) {
                    showReviewIdolQuizDialog()
                    return@newInstance
                }

                sendQuizRewardHeart()
            })

        val tag = "quiz_quit_dialog"
        val oldFrag = supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            rewardBottomSheet.show(supportFragmentManager, tag)
        }
    }

    override fun onResume() {
        super.onResume()
        FLAG_CLOSE_DIALOG = false
    }

    override fun onStop() {
        FLAG_CLOSE_DIALOG = false
        super.onStop()
    }

    private fun getQuizSuccessTitleSpan(): SpannableString {
        val spannableString = SpannableString(String.format(getString(R.string.quiz_fail), countCorrectAnswer))

        val pattern = Pattern.compile("\\d+")
        val matcher = pattern.matcher(spannableString)

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end() + 1 // "개 까지만 적용되게."
            spannableString.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        this@IdolQuizSolveActivity,
                        R.color.main
                    )
                ),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannableString
    }

    override fun onDestroy() {
        super.onDestroy()
        thread?.stopTimer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("quizCur", quizCur)
        outState.putInt("answer", answer)
        outState.putInt("r1", r1)
        outState.putInt("r2", r2)
        outState.putLong("cAnswer", cAnswer)
        outState.putString("description", description)
        outState.putParcelableArrayList("modelList", modelList)
        outState.putParcelableArrayList("reviewModelList", reviewModelList)
        outState.putInt("quizzes", quizzes)
        outState.putInt("totalCount", totalCount)
        outState.putString("corrects", corrects)
        outState.putString("incorrects", incorrects)
        outState.putInt("quizId", quizId)
        outState.putInt("sessId", sessId)
        outState.putBoolean("quizFin", quizFin)
        outState.putInt("continueLimit", continueLimit)
        outState.putBoolean("hasChooseWrongAnswer", hasChooseWrongAnswer)
        outState.putInt("countCorrectAnswer", countCorrectAnswer)
    }

    companion object {

        const val EXTRA_IS_COMPLETE_AD_SHOW = "is_complete_ad_show"
        const val EXTRA_QUIZ_TODAY_MODEL = "quiz_today_model"
        const val EXTRA_TYPE = "type"
        const val EXTRA_IDOL_NAME = "idol_name"

        fun createIntent(
            context: Context,
            idolId: Int?,
            quizTodayModel: QuizTodayModel?,
            type: String?,
            idolName: String,
            isCompleteAdShow: Boolean = false
        ): Intent {
            val intent = Intent(context, IdolQuizSolveActivity::class.java)
            intent.putExtra(EXTRA_IDOL, idolId)
            intent.putExtra(EXTRA_QUIZ_TODAY_MODEL, quizTodayModel)
            intent.putExtra(EXTRA_TYPE, type)
            intent.putExtra(EXTRA_IDOL_NAME, idolName)
            intent.putExtra(EXTRA_IS_COMPLETE_AD_SHOW, isCompleteAdShow)
            return intent
        }
    }
}