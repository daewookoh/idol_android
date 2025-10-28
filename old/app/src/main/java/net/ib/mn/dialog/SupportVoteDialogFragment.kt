package net.ib.mn.dialog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
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
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.fragment.SupportSuccessDialogFragment
import net.ib.mn.model.SupportListModel
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.view.ClearableEditText
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SupportVoteDialogFragment : BaseDialogFragment(), View.OnClickListener {
    private var mSubmitBtn: AppCompatButton? = null
    private var mCancelBtn: AppCompatButton? = null
    private var mDiamondCountInput: ClearableEditText? = null
    private var mDiamondCount: AppCompatTextView? = null


    private var total_diamond = 0
    private var diamond = 0
    private var mSupport: SupportListModel? = null

    @Inject
    lateinit var supportRepository: SupportRepositoryImpl
    @Inject
    lateinit var accountManager: IdolAccountManager

    // 투표 2초 후에 창닫기
    private var voteTime: Long = 0

    override fun onResume() {
        super.onResume()
        val account = getAccount(baseActivity)
        if (account != null) {
            accountManager.fetchUserInfo(activity, {
                total_diamond = account.userModel!!.diamond
                mDiamondCount!!.text = getCommaNumber(total_diamond)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        mSupport = args!!.getSerializable(PARAM_SUPPORT) as SupportListModel?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_support_vote, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSubmitBtn = view.findViewById(R.id.btn_confirm)
        mCancelBtn = view.findViewById(R.id.btn_cancel)
        mDiamondCount = view.findViewById(R.id.my_diamond)
        mDiamondCountInput = view.findViewById(R.id.diamon_count_input)
        val diamond1Btn = view.findViewById<View>(R.id.cl_diamond1)
        val diamond5Btn = view.findViewById<View>(R.id.cl_diamond5)
        val diamond10Btn = view.findViewById<View>(R.id.cl_diamond10)
        val diamond50Btn = view.findViewById<View>(R.id.cl_diamond50)
        val diamond100Btn = view.findViewById<View>(R.id.cl_diamond100)
        val diamondAllBtn = view.findViewById<View>(R.id.cl_diamond_all)

        diamond1Btn.setOnClickListener(this)
        diamond5Btn.setOnClickListener(this)
        diamond10Btn.setOnClickListener(this)
        diamond50Btn.setOnClickListener(this)
        diamond100Btn.setOnClickListener(this)
        diamondAllBtn.setOnClickListener(this)

        mSubmitBtn?.setOnClickListener(this)
        mCancelBtn?.setOnClickListener(this)
    }

    override fun onActivityCreated(arg0: Bundle?) {
        super.onActivityCreated(arg0)
    }

    private fun getCommaNumber(count: Int): String {
        val format = NumberFormat.getNumberInstance(Locale.US)
        return format.format(count.toLong())
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_confirm -> if (mDiamondCountInput!!.text.toString() != "") doVote()
            R.id.btn_cancel -> dismiss()
            R.id.cl_diamond1 -> addHeart(1)
            R.id.cl_diamond5 -> addHeart(5)
            R.id.cl_diamond10 -> addHeart(10)
            R.id.cl_diamond50 -> addHeart(50)
            R.id.cl_diamond100 -> addHeart(100)
            R.id.cl_diamond_all -> Util.showDefaultIdolDialogWithBtn2(
                activity,
                null,
                resources.getString(R.string.are_you_sure_dia),
                { v1: View? ->
                    setDiamond(total_diamond)
                    Util.closeIdolDialog()
                },
                { v12: View? -> Util.closeIdolDialog() })
        }
    }

    // 보상하트 팝업 후 투표창 닫아서 레벨업 팝업이 다른 팝업 닫힌 후에 나오게
    private fun dismissVoteDialog(response: JSONObject) {
        val result = Intent()

        val myVoteDiamond = response.optInt("number")
        result.putExtra(PARAM_MYVOTE_DIAMOND, myVoteDiamond)

        setResultCode(RESULT_OK)
        setResult(result)

        try {
            dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
            dismiss()
        }
    }

    private fun doVote() {
        if (!mSubmitBtn!!.isEnabled) {
            return
        }

        // 키패드 열려있는 상태로 투표하면 팝업이 비정상적으로 길어짐
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(mDiamondCountInput!!.windowToken, 0)

        Util.showProgress(baseActivity, false)

        mSubmitBtn!!.isEnabled = false
        val listener: (JSONObject) -> Unit = { response ->
            // 투표 2초 후 닫기
            val now = Date().time
            var delay: Long = 0
            if (now - voteTime < 2000) {
                delay = 2000 - (now - voteTime)
            }

            Handler().postDelayed({
                Util.closeProgress()
                if (activity != null && isAdded) {
                    if (response.optBoolean("success")) {
                        val bonusDiamond = response.optInt("bonus_diamond")
                        Util.log("SupportVoteDialog::$response")

                        var usedDiaAmount = response.optInt("number", -1)
                        if (usedDiaAmount == -1) { //만약에  서버값없으면  사용한 diamond 값으로
                            usedDiaAmount = diamond
                        }
                        val msg = if (diamond == 1) {
                            "1"
                        } else {
                            NumberFormat.getNumberInstance(Locale.getDefault())
                                .format(usedDiaAmount.toLong())
                        }

                        checkNotNull(arguments)
                        val dialogFragment = SupportSuccessDialogFragment(
                            msg, arguments!!.getInt(
                                PARAM_SUPPORT_ID
                            ), mSupport!!, arguments!!.getString(PARAM_AD_NAME)!!
                        )
                        dialogFragment.show(
                            activity!!.supportFragmentManager,
                            "supportSuccessDialog"
                        )

                        val account = getAccount(context)
                        if (account != null) {
                            Util.log("SupportDetail::fetchUserInfo was called")
                            accountManager.fetchUserInfo(context)
                        }

                        dismissVoteDialog(response)
                    } else {
                        UtilK.handleCommonError(context, response)

                        mSubmitBtn!!.isEnabled = true
                    }
                }
            }, delay)
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            Util.closeProgress()
            makeText(activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
            if (Util.is_log()) {
                makeText(activity, throwable.message, Toast.LENGTH_SHORT).show()
            }
            mSubmitBtn!!.isEnabled = true
            dismiss()
        }

        val a: CharSequence? = mDiamondCountInput!!.text
        diamond = 0
        if (!TextUtils.isEmpty(a)) {
            // . 이나 - 를 찍고 들어는 경우 대비
            try {
                diamond = a.toString().toInt()
            } catch (e: Exception) {
            }
        } else {
            diamond = 0
        }
        if (diamond <= 0) {
            val result = Intent()
            setResultCode(RESULT_OK)
            result.putExtra(PARAM_MYVOTE_DIAMOND, diamond)

            setResult(result)
            dismiss()
            return
        }

        voteTime = Date().time

        MainScope().launch {
            supportRepository.giveDiamond(
                supportId = mSupport!!.id,
                diamond = diamond,
                listener = listener,
                errorListener = errorListener)
        }
    }

    private fun addHeart(count: Int) {
        val a: CharSequence? = mDiamondCountInput!!.text
        val diamond = if (!TextUtils.isEmpty(a)) {
            a.toString().toInt()
        } else {
            0
        }
        if (total_diamond >= count + diamond) {
            mDiamondCountInput!!.setText((count + diamond).toString())
        }
    }

    fun setDiamond(count: Int) {
        mDiamondCountInput!!.setText(count.toString())
    }

    companion object {
        fun getDiaMondVoteInstance(
            model: SupportListModel?,
            id: Int,
            adName: String?
        ): SupportVoteDialogFragment {
            val fragment = SupportVoteDialogFragment()
            val args = Bundle()
            args.putSerializable(PARAM_SUPPORT, model)
            args.putInt(PARAM_SUPPORT_ID, id)
            args.putString(PARAM_AD_NAME, adName)
            fragment.arguments = args
            fragment.setStyle(STYLE_NO_TITLE, 0)
            return fragment
        }

        const val PARAM_SUPPORT: String = "support"
        const val PARAM_SUPPORT_ID: String = "supportId"
        const val PARAM_AD_NAME: String = "adName"
        const val PARAM_MYVOTE_DIAMOND: String = "my_vote_diamond"
    }
}
