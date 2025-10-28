package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.adapter.IdolQuizStatusAdapter
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.core.data.repository.QuizRepositoryImpl
import net.ib.mn.databinding.ActivityQuizInfoBinding
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.fragment.BottomSheetFragment.Companion.newInstance
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.fragment.RewardBottomSheetDialogFragment.Companion.newInstance
import net.ib.mn.model.QuizModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONException
import javax.inject.Inject

@AndroidEntryPoint
class IdolQuizInfoActivity : BaseActivity(), View.OnClickListener {
    private var mBottomSheetFragment: BottomSheetFragment? = null
    private var mIdolQuizStatusAdapter: IdolQuizStatusAdapter? = null
    private var myQuizArray: ArrayList<QuizModel> = arrayListOf()
    private var isViewable = "" // 페이징 처리 시 현재 보이는 퀴즈 상태를 알기 위함.
    private var limit = 30 //페이지 사이즈
    private val offset = 0 //시작 인덱스

    private var rewardBottomSheetDialogFragment: RewardBottomSheetDialogFragment? = null

    @Inject
    lateinit var quizRepository: QuizRepositoryImpl

    private lateinit var binding: ActivityQuizInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.quiz_button_my_quiz)

        binding = ActivityQuizInfoBinding.inflate(layoutInflater)
        binding.llQuizInfo.applySystemBarInsets()
        setContentView(binding.root)

        mIdolQuizStatusAdapter = IdolQuizStatusAdapter()
        binding.rvQuizList.setAdapter(mIdolQuizStatusAdapter)
        binding.quizHeart.setOnClickListener(this)
        binding.quizTmp.setOnClickListener(this)
        binding.btnQuestion.setOnClickListener(this)

        getMyQuizList("Y", limit, offset)
        setRecyclerViewScrollListener()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.quizHeart -> getQuiz(this)
            R.id.quizTmp -> showBottomSheetDialogSelectShowOption()
            R.id.btn_question -> startActivity(IdolQuizDenyActivity.createIntent(this))
        }
    }

    //퀴즈 승인 대기,거절시 부르는 API
    fun getMyQuizList(isViewable: String, limit: Int, offset: Int) {
        binding.quizHeart.visibility =
            View.GONE //api부를 때 일단 보상하트 없다고 생각. 보상하트 존재한다면 밑에 for문에서 visible처리 해줌
        this.isViewable = isViewable //현재 퀴즈 상태 저장

        MainScope().launch {
            quizRepository.getMyQuizList(
                limit,
                offset,
                isViewable,
                { response ->
                    val gson = instance
                    if (response.optBoolean("success")) {
                        try {
                            myQuizArray = ArrayList<QuizModel>()
                            val array = response.getJSONArray("objects")
                            if (isViewable == "Y") { //공개(채택된 퀴즈일 때)
                                binding.quizTmp.setText(R.string.quiz_accepted)
                            } else if (isViewable == "N") {    //퀴즈 거절,신고 삭제 대상일 때
                                binding.quizTmp.setText(R.string.quiz_denied)
                            } else {   //심사중일 때(5번 승인 시 Y로 승격)
                                binding.quizTmp.setText(R.string.quiz_in_screening)
                            }
                            if (array.length() != 0) {
                                binding.emptyView.visibility = View.GONE
                                for (i in 0 until array.length()) {
                                    val model = gson.fromJson(
                                        array.getJSONObject(i).toString(),
                                        QuizModel::class.java
                                    )
                                    if (model.isViewable == isViewable) {
                                        myQuizArray.add(model)
                                        if (model.isViewable == "Y" && model.rewarded == null) {  //퀴즈 채택됐고, 보상을 안받은게 있다면
                                            binding.quizHeart.visibility =
                                                View.VISIBLE //하트 보상 팝업 띄우는 이미지 visible
                                        }
                                    }
                                }
                            } else {
                                if (isViewable == "Y") { //공개(채택된 퀴즈일 때)
                                    binding.emptyView.setText(R.string.quiz_no_approved)
                                } else if (isViewable == "N") {    //퀴즈 거절,신고 삭제 대상일 때
                                    binding.emptyView.setText(R.string.quiz_no_denied)
                                } else {   //심사중일 때(5번 승인 시 Y로 승격)
                                    binding.emptyView.setText(R.string.quiz_no_in_screening)
                                }
                                binding.emptyView.visibility = View.VISIBLE //채택된 거 없다는 스트링
                            }
                            mIdolQuizStatusAdapter!!.quizInfoArray(myQuizArray)

                            binding.loadingView.visibility = View.GONE
                            binding.clMainView.visibility = View.VISIBLE

                            //거절된 퀴즈 안내 이미지 처리
                            if (isViewable == "N") binding.btnQuestion.visibility = View.VISIBLE
                            else binding.btnQuestion.visibility = View.GONE
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                },
                { throwable ->
                    showMessage(throwable.message)
                }
            )
        }
    }

    //채택된 퀴즈에서 하트 보상 클릭했을 때 부르는 API
    fun getQuiz(context: Context?) {
        MainScope().launch {
            quizRepository.claimQuizReward(
                { response ->
                    if (response.optBoolean("success")) {
                        try {
                            showEventDialog(response.getInt("rewards"))
                        } catch (e: JSONException) {
                        }
                    } else {
                        UtilK.handleCommonError(context, response)
                    }
                },
                { throwable ->
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                }
            )
        }
    }

    //퀴즈 채택 보상 버튼 눌렀을 시 나오는 팝업
    private fun showEventDialog(event_heart: Int) {
        rewardBottomSheetDialogFragment =
            newInstance(RewardBottomSheetDialogFragment.FLAG_QUIZ_CHOOSE_REWARD, event_heart) {
                getMyQuizList(isViewable, limit, offset) //보상하트 받은 경우, 최신화하기 위해 다시 한번 부름.
                Unit
            }

        val tag = "reward_quiz_choose"

        val oldFrag = supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            rewardBottomSheetDialogFragment!!.show(supportFragmentManager, tag)
        }
    }

    //승인, 대기, 거절 바텀시트
    fun showBottomSheetDialogSelectShowOption() {
        mBottomSheetFragment = newInstance(BottomSheetFragment.FLAG_QUIZ_INFO)
        val tag = "filter_privacy"
        val oldFrag = supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            mBottomSheetFragment!!.show(supportFragmentManager, tag)
        }
    }

    fun setRecyclerViewScrollListener() {
        binding.rvQuizList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastViewItem =
                    (binding.rvQuizList.layoutManager as LinearLayoutManager?)!!.findLastCompletelyVisibleItemPosition() + 1 //마지막에 보이는 아이템 값
                if (lastViewItem == limit) {  //마지막 아이템이 현재 보여주는 limit 개수와 같다면
                    limit += 30 //limit 개수 늘리고
                    getMyQuizList(isViewable, limit, offset)
                }
            }
        })
    }

    companion object {
        fun createIntent(context: Context?): Intent {
            return Intent(context, IdolQuizInfoActivity::class.java)
        }
    }
}
