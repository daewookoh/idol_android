package net.ib.mn.dialog

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.activity.VotingCertificateListActivity
import net.ib.mn.core.data.model.GiveHeartModel
import net.ib.mn.core.domain.usecase.GiveHeartToArticleUseCase
import net.ib.mn.core.domain.usecase.idols.GiveHeartToIdolUseCase
import net.ib.mn.dialog.RewardDialogFragment.Companion.getInstance
import net.ib.mn.feature.votingcertificate.VotingCertificateActivity
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.fragment.RewardBottomSheetDialogFragment.Companion.newInstance
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.view.ClearableEditText
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class VoteDialogFragment : BaseDialogFragment(), View.OnClickListener {
    interface OnDismissWithResultListener {
        fun onDismiss(voteHeart: Long)
    }

    @Inject
    lateinit var giveHeartToArticleUseCase: GiveHeartToArticleUseCase
    @Inject
    lateinit var giveHeartToIdolUseCase: GiveHeartToIdolUseCase

    private var onDismissWithResultListener: OnDismissWithResultListener? = null

    private var mSubmitBtn: AppCompatButton? = null
    private var mCancelBtn: AppCompatButton? = null
    private var mHeartCountInput: ClearableEditText? = null
    private var mEverHeartCount: AppCompatTextView? = null
    private var mMyHeartCount: AppCompatTextView? = null
    private var mWeakHeartCount: AppCompatTextView? = null


    private var mArticle: ArticleModel? = null
    private var mIdol: IdolModel? = null
    private var mPosition = 0
    private var total_heart: Long = 0
    private var free_heart: Long = 0
    private var strong_heart: Long = 0
    private var heart: Long = 0

    // 투표 2초 후에 창닫기
    private var voteTime: Long = 0

    private var isIdolVoteSuccess = false

    private var mBottomSheetFragment: RewardBottomSheetDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        mArticle = args!!.getSerializable(PARAM_ARTICLE) as ArticleModel?
        mIdol = args.getSerializable(PARAM_IDOL) as IdolModel?
        mPosition = args.getInt(PARAM_POSITION, -1)
        total_heart = args.getLong(PARAM_TOTAL)
        free_heart = args.getLong(PARAM_FREE)
        strong_heart = total_heart - free_heart
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_vote, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSubmitBtn = view.findViewById(R.id.btn_confirm)
        mCancelBtn = view.findViewById(R.id.btn_cancel)
        val heart1Btn = view.findViewById<View>(R.id.cl_heart1)
        val heart10Btn = view.findViewById<View>(R.id.cl_heart10)
        val heart50Btn = view.findViewById<View>(R.id.cl_heart50)
        val heart100Btn = view.findViewById<View>(R.id.cl_heart100)
        val heartAllBtn = view.findViewById<View>(R.id.cl_heart_all)
        val heartFreeAllBtn = view.findViewById<View>(R.id.cl_heart_free_all)
        val tvSolo = view.findViewById<AppCompatTextView>(R.id.tv_solo)
        val tvGroup = view.findViewById<AppCompatTextView>(R.id.tv_group)
        mHeartCountInput = view.findViewById(R.id.heart_count)
        mEverHeartCount = view.findViewById(R.id.heart_count_everheart)
        mMyHeartCount = view.findViewById(R.id.heart_count_myheart)
        mWeakHeartCount = view.findViewById(R.id.heart_count_weak_heart)

        if (mIdol == null) {
            tvSolo.text = Util.nameSplit(activity, mArticle!!.idol)[0]
            tvGroup.text = Util.nameSplit(activity, mArticle!!.idol)[1]
        } else {
            tvSolo.text = Util.nameSplit(activity, mIdol)[0]
            tvGroup.text = Util.nameSplit(activity, mIdol)[1]
        }

        heart1Btn.setOnClickListener(this)
        heart10Btn.setOnClickListener(this)
        heart50Btn.setOnClickListener(this)
        heart100Btn.setOnClickListener(this)
        heartAllBtn.setOnClickListener(this)
        heartFreeAllBtn.setOnClickListener(this)


        mSubmitBtn?.setOnClickListener(this)
        mCancelBtn?.setOnClickListener(this)
    }

    override fun onActivityCreated(arg0: Bundle?) {
        super.onActivityCreated(arg0)

        mWeakHeartCount!!.text = getCommaNumber(free_heart)
        mEverHeartCount!!.text = getCommaNumber(strong_heart)
        mMyHeartCount!!.text = getCommaNumber(total_heart)
    }

    fun setOnDismissWithResultListener(listener: OnDismissWithResultListener?) {
        this.onDismissWithResultListener = listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (onDismissWithResultListener != null && isIdolVoteSuccess) {
            onDismissWithResultListener!!.onDismiss(heart)
        }
    }

    private fun getCommaNumber(count: Long): String {
        val format = NumberFormat.getNumberInstance(Locale.US)
        return format.format(count)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_confirm -> if (mHeartCountInput!!.text.toString() != "") doVote()
            R.id.btn_cancel -> dismiss()
            R.id.cl_heart1 -> addHeart(1L)
            R.id.cl_heart10 -> addHeart(10L)
            R.id.cl_heart50 -> addHeart(50L)
            R.id.cl_heart100 -> addHeart(100L)
            R.id.cl_heart_all -> Util.showDefaultIdolDialogWithBtn2(
                activity,
                null,
                resources.getString(R.string.are_you_sure),
                { v1: View? ->
                    setHeart(total_heart)
                    Util.closeIdolDialog()
                },
                { v12: View? -> Util.closeIdolDialog() })

            R.id.cl_heart_free_all -> setHeart(free_heart)
        }
    }

    // 보상하트 팝업 후 투표창 닫아서 레벨업 팝업이 다른 팝업 닫힌 후에 나오게
    private fun dismissVoteDialog(response: GiveHeartModel) {
        val result = Intent()
        if (response.eventHeart) {
            val event_heart = response.eventHeartCount.toString()
            result.putExtra(PARAM_EVENT_HEART, event_heart)
        }

        setResultCode(RESULT_OK)

        result.putExtra(PARAM_POSITION, mPosition)
        result.putExtra(PARAM_ARTICLE, mArticle)
        val a: CharSequence? = mHeartCountInput!!.text
        val heart: Long = if (!TextUtils.isEmpty(a)) {
            a.toString().toLong()
        } else {
            0
        }
        result.putExtra(PARAM_HEART, heart)
        // 어느 아이돌에 투표했는지 검사를 위해 추가
        if (mArticle != null) {
            result.putExtra(PARAM_IDOL_MODEL, mArticle!!.idol as Parcelable?)
        } else {
            result.putExtra(PARAM_IDOL_MODEL, mIdol as Parcelable?)
        }

        setResult(result)

        try {
            dismiss(true)
        } catch (e: Exception) {
            e.printStackTrace()
            // Cannot remove Fragment attached to a different FragmentManager. Fragment VoteDialogFragment{5ffed96} (e30bb417-1e2a-485c-a38e-074be2e420a8 tag=vote) is already attached to a FragmentManager.
            try {
                val ft = baseActivity?.supportFragmentManager?.beginTransaction()
                ft?.remove(this)
                ft?.commitAllowingStateLoss()
            } catch (e: Exception) {
                Log.e("VoteDialogFragment", "dismiss error")
            }
        }
    }

    private fun doVote() {
        if (!(mSubmitBtn?.isEnabled ?: false)) {
            return
        }

        // 키패드 열려있는 상태로 투표하면 팝업이 비정상적으로 길어짐
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(mHeartCountInput!!.windowToken, 0)

        Util.showProgress(baseActivity, false)

        mSubmitBtn!!.isEnabled = false
        val listener: (GiveHeartModel) -> Unit = { response ->
            // 투표 2초 후 닫기
            val now = Date().time
            var delay: Long = 0
            if (now - voteTime < 2000) {
                delay = 2000 - (now - voteTime)
            }

            Handler().postDelayed({
                Util.closeProgress()
                if (activity != null && isAdded) {
                    if (response.success) {
                        val bonusHeart = response.bonusHeart ?: 0

                        // 게시글 투표
                        if (requireActivity().supportFragmentManager.findFragmentByTag("community_vote") != null || requireActivity().javaClass == NewCommentActivity::class.java) {
                            val dialogBoardTitle = getString(R.string.vote_posting)
                            val msg = response.msg

                            if (mIdol == null && IdolAccount.getAccount(context)?.most?.getId() == Const.NON_FAVORITE_IDOL_ID) {
                                val newMost = IdolAccount.getAccount(context)?.most
                                newMost?.let {
                                    it.heart = it.heart + heart
                                    IdolAccount.getAccount(context)?.most = it
                                }
                            }

                            // 게시물 투표 보상
                            if (heart >= 100) {
                                // 투표창을 먼저 닫고 레벨업 체크
                                val activity = requireActivity()
                                dismissVoteDialog(response)

                                mArticle?.idol?.let { idol ->
                                    val voteSuccessDialog = VoteBottomSheetFragment.newInstance(
                                        idol = idol,
                                        voteCount = heart,
                                        bonusHeart = bonusHeart.toString(),
                                        onClickConfirm = {
                                            val intent = Intent(activity, VotingCertificateActivity::class.java)
                                            intent.putExtra(VotingCertificateActivity.IDOL_ID, idol.getId().toLong())
                                            activity.startActivity(intent)
                                        },
                                        onClickDismiss = {
                                            dismiss()
                                        }
                                    )
                                    // Can not perform this action after onSaveInstanceState exception 예방
                                    if(!activity.supportFragmentManager.isStateSaved) {
                                        voteSuccessDialog.show(activity.supportFragmentManager, VOTE_DIALOG_TAG)
                                    }
                                }

                            } else {
                                getInstance(
                                    dialogBoardTitle,
                                    msg,
                                    null,
                                    R.drawable.img_popup_post_vote
                                ) {
                                    dismissVoteDialog(response)
                                    Unit
                                }.show(requireActivity().supportFragmentManager, "reward_dialog")
                            }
                        } else {
                            // 순위 투표 보상
                            isIdolVoteSuccess = true

                            // 투표창을 먼저 닫고 레벨업 체크
                            val activity = requireActivity()
                            dismissVoteDialog(response)
                            if (heart >= 100) {
                                mIdol?.let { idol ->
                                    val voteSuccessDialog = VoteBottomSheetFragment.newInstance(
                                        idol = idol,
                                        voteCount = heart,
                                        bonusHeart = bonusHeart.toString(),
                                        onClickConfirm = {
                                            val intent = Intent(activity, VotingCertificateActivity::class.java)
                                            intent.putExtra(VotingCertificateActivity.IDOL_ID, idol.getId().toLong())
                                            activity.startActivity(intent)
                                        },
                                        onClickDismiss = {
                                            dismiss()
                                        }
                                    )
                                    // Can not perform this action after onSaveInstanceState exception 예방
                                    if(!activity.supportFragmentManager.isStateSaved) {
                                        voteSuccessDialog.show(activity.supportFragmentManager, VOTE_DIALOG_TAG)
                                    }
                                }
                            }
                        }
                    } else {
                        val responseMsg = response.msg ?: ""
                        makeText(activity, responseMsg, Toast.LENGTH_SHORT).show()
                        mSubmitBtn!!.isEnabled = true
                    }
                }
            }, delay)
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            Util.closeProgress()
            val msg = throwable.message ?: ""
            makeText(activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
            if (Util.is_log()) {
                makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }
            mSubmitBtn!!.isEnabled = true
            dismiss()
        }

        val a: CharSequence? = mHeartCountInput!!.text
        heart = 0
        if (!TextUtils.isEmpty(a)) {
            // . 이나 - 를 찍고 들어는 경우 대비
            try {
                heart = a.toString().toLong()
            } catch (e: Exception) {
            }
        } else {
            heart = 0
        }
        if (heart <= 0) {
            Util.closeProgress()

            val result = Intent()
            setResultCode(RESULT_OK)
            result.putExtra(PARAM_HEART, heart)

            setResult(result)
            dismiss()
            return
        }

        //자기가 가지고있는 하트보다 더많이 투표할경우 api호출 안하도록 변경.
        if (heart > total_heart) {
            Util.closeProgress()
            makeText(activity, getString(R.string.not_enough_heart), Toast.LENGTH_SHORT).show()
            mSubmitBtn!!.isEnabled = true
            return
        }

        voteTime = Date().time

        if (mIdol != null) {
            MainScope().launch {
                giveHeartToIdolUseCase(
                    mIdol!!.getId(),
                    heart,
                    listener,
                    errorListener)
            }
        } else if (mArticle != null) {
            MainScope().launch {
                giveHeartToArticleUseCase(
                    mArticle!!.id,
                    heart,
                    listener,
                    errorListener)
            }
        }
    }

    private fun addHeart(count: Long) {
        val a: CharSequence? = mHeartCountInput!!.text
        val heart = if (!TextUtils.isEmpty(a)) {
            a.toString().toInt()
        } else {
            0
        }
        if (total_heart >= count + heart) {
            mHeartCountInput!!.setText((count + heart).toString())
        }
    }

    fun setHeart(count: Long) {
        mHeartCountInput!!.setText(count.toString())
    }


    private fun setRewardBottomSheetFragment(resId: Int, bonusHeart: Int, response: GiveHeartModel) {
        mBottomSheetFragment = newInstance(resId, bonusHeart, response.msg ?: "") {
            dismissVoteDialog(response)
            Unit
        }

        val tag = "reward_article"
        if (activity == null) {
            return
        }
        val oldFrag = childFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            try {
                mBottomSheetFragment!!.show(childFragmentManager, tag)
            } catch (e: IllegalStateException) {
                dismissVoteDialog(response) // 바텀시트 보여주기 실패하면 투표창 닫기
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun getArticleVoteInstance(
            model: ArticleModel?,
            position: Int,
            totalHeart: Long,
            freeHeart: Long
        ): VoteDialogFragment {
            val fragment = VoteDialogFragment()
            val args = Bundle()
            args.putSerializable(PARAM_ARTICLE, model)
            args.putLong(PARAM_TOTAL, totalHeart)
            args.putLong(PARAM_FREE, freeHeart)
            args.putInt(PARAM_POSITION, position)
            fragment.arguments = args
            fragment.setStyle(STYLE_NO_TITLE, 0)
            return fragment
        }

        @JvmStatic
        fun getIdolVoteInstance(
            model: IdolModel?,
            totalHeart: Long,
            freeHeart: Long
        ): VoteDialogFragment {
            val fragment = VoteDialogFragment()
            val args = Bundle()
            args.putSerializable(PARAM_IDOL, model)
            args.putLong(PARAM_TOTAL, totalHeart)
            args.putLong(PARAM_FREE, freeHeart)
            fragment.arguments = args
            fragment.setStyle(STYLE_NO_TITLE, 0)
            return fragment
        }

        private const val PARAM_ARTICLE = "article"
        private const val PARAM_IDOL = "idol"
        const val PARAM_POSITION: String = CommunityActivity.PARAM_ARTICLE_POSITION
        const val PARAM_HEART: String = "heart"
        const val PARAM_TOTAL: String = "total_heart"
        const val PARAM_FREE: String = "free_heart"
        private const val PARAM_EVENT_HEART = "event_heart"
        const val PARAM_IDOL_MODEL: String = "idol_model"
        const val VOTE_DIALOG_TAG = "vote_dialog"
    }
}
