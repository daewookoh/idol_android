package net.ib.mn.dialog

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.utils.AppConst
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.handleCommonError
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class RenameDialogFragment : BaseDialogFragment(), View.OnClickListener {
    private var mNicknameInput: AppCompatEditText? = null
    private var mDrawableInputOk: Drawable? = null
    private var mDrawableInputError: Drawable? = null
    var useCoupon: Boolean = false
    @Inject
    lateinit var usersRepository: UsersRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_rename, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val submitBtn = view.findViewById<AppCompatButton>(R.id.btn_confirm)
        val cancelBtn = view.findViewById<AppCompatButton>(R.id.btn_cancel)
        mNicknameInput = view.findViewById(R.id.input_nickname)

        mDrawableInputOk = ResourcesCompat.getDrawable(resources, R.drawable.join_approval, null)?.apply {
            setBounds(
                0,
                0,
                intrinsicWidth,
                intrinsicHeight
            )
        }
        mDrawableInputError = ResourcesCompat.getDrawable(resources, R.drawable.join_disapproval, null)?.apply {
            setBounds(
                0,
                0,
                intrinsicWidth,
                intrinsicHeight
            )
        }
        submitBtn.setOnClickListener(this)
        cancelBtn.setOnClickListener(this)

        if (BuildConfig.CELEB) {
            mNicknameInput?.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    mNicknameInput?.background
                        ?.setTint(ResourcesCompat.getColor(resources, R.color.main, null))
                    return@OnFocusChangeListener
                }
                mNicknameInput?.background
                    ?.setTint(ResourcesCompat.getColor(resources, R.color.text_dimmed, null))
            }
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_cancel) {
            dismiss()
        } else if (v.id == R.id.btn_confirm) {
            if (TextUtils.isEmpty(mNicknameInput!!.text)) {
                mNicknameInput!!.error = getString(R.string.required_field)
                return
            }
            if (Util.hasBadWords(baseActivity, mNicknameInput!!.text.toString())) {
                makeText(baseActivity, R.string.bad_words, Toast.LENGTH_SHORT).show()
                return
            }

            val listener: (JSONObject) -> Unit = { response ->
                if (activity != null && isAdded) {
                    Util.log("SignupFragment $response")
                    if (response.optBoolean("success")) {
                        mNicknameInput!!.error = null
                        mNicknameInput!!.setCompoundDrawables(
                            null,
                            null,
                            mDrawableInputOk,
                            null
                        )
                        Util.hideSoftKeyboard(context, mNicknameInput
                        )
                        val handler = Handler(Looper.getMainLooper())
                        Util.showProgress(context, false)
                        handler.postDelayed({
                            Util.closeProgress()
                            updateNickname()
                        }, 500)
                    } else {
                        val responseMsg = ErrorControl.parseError(
                            activity, response
                        )
                        if (responseMsg != null) {
                            mNicknameInput!!.setError(responseMsg, mDrawableInputError)
                        }
                    }
                }
            }
            val errorListener: (Throwable) -> Unit = { throwable ->
                if (activity != null && isAdded) {
                    mNicknameInput!!.setError(
                        getString(R.string.error_abnormal_exception),
                        mDrawableInputError
                    )
                }
            }

            lifecycleScope.launch {
                usersRepository.validate(
                    type = "nickname",
                    value = mNicknameInput!!.text.toString(),
                    appId = AppConst.APP_ID,
                    listener = listener,
                    errorListener = errorListener
                )
            }
        }
    }

    private fun updateNickname() {
        val listener: (JSONObject) -> Unit = { response ->
            if (activity != null && isAdded) {
                if (!response.optBoolean("success")) {
                    handleCommonError(baseActivity, response)
                } else {
                    // 닉네임 변경 완료 팝업
                    Util.showDefaultIdolDialogWithBtn1(
                        baseActivity, null, getString(R.string.msg_nickname_changed)
                    ) { v: View? -> Util.closeIdolDialog() }
                }
                setResultCode(RESULT_OK)
                dismiss()
            }
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            if (activity != null && isAdded) {
                makeText(baseActivity, throwable.message, Toast.LENGTH_SHORT).show()
            }
        }

        val nickname = mNicknameInput?.getText().toString() ?: return

        lifecycleScope.launch {
            usersRepository.alterNickname(
                nickname = nickname,
                useCoupon = useCoupon,
                listener = listener,
                errorListener = errorListener
            )
        }
    }

    companion object {
        fun getInstance(): RenameDialogFragment {
            val fragment = RenameDialogFragment()
            fragment.setStyle(STYLE_NO_TITLE, 0)
            return fragment
        }
    }
}
