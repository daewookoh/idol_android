package net.ib.mn.fragment

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.AuthActivity
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.FragmentEmailSigninBinding
import net.ib.mn.dialog.FindIdDialogFragment
import net.ib.mn.fragment.SignupFragment.OnRegistered
import net.ib.mn.gcm.GcmUtils.registerDevice
import net.ib.mn.pushy.PushyUtil
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Util
import net.ib.mn.utils.setFirebaseUIAction
import org.json.JSONArray
import org.json.JSONException
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class EmailSigninFragment : BaseFragment() {

    private var _binding: FragmentEmailSigninBinding? = null
    val binding get() = _binding!!

    private var isClickedCheck: Boolean = false
    private lateinit var domain: String

    @Inject
    lateinit var usersRepository: UsersRepository

    var wrap: OnRegistered = OnRegistered { id ->
        (activity as AuthActivity?)!!.trySignin(
            binding.inputEmail.getText().toString(),
            binding.inputPasswd.getText().toString(),
            id,
            domain
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmailSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actionbar = (activity as BaseActivity?)?.supportActionBar
        actionbar?.setTitle(R.string.login_email)
        actionbar?.setDisplayHomeAsUpEnabled(true)

        setEventListener()
    }

    override fun onResume() {
        super.onResume()
        ViewCompat.requestApplyInsets(requireView())
    }

    private fun setEventListener() {
        binding.inputEmail.setOnFocusChangeListener { _, hasFocus ->
            binding.inputEmailLayout.setBackgroundResource(
                if(hasFocus) R.drawable.edit_underline_click else R.drawable.edit_underline)
        }

        binding.inputPasswd.setOnFocusChangeListener { _, hasFocus ->
            binding.inputPasswdLayout.setBackgroundResource(
                if(hasFocus) R.drawable.edit_underline_click else R.drawable.edit_underline)
        }

        binding.btnHide.setOnClickListener {
            if (isClickedCheck) {
                binding.btnHide.setImageResource(R.drawable.btn_hide_off)
                binding.inputPasswd.apply {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setSelection(binding.inputPasswd.text.length)
                }
                isClickedCheck = false
            } else {
                binding.btnHide.setImageResource(R.drawable.btn_hide_on)
                binding.inputPasswd.apply {
                    inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    setSelection(binding.inputPasswd.text.length)
                }
                isClickedCheck = true
            }
        }

        binding.btnSignup.setOnClickListener {
            setFirebaseUIAction(GaAction.SIGNUP_EMAIL)

            val destinationFragment = if (BuildConfig.CHINA) {
                SignupFragment()
            } else {
                AgreementFragment()
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, destinationFragment)
                .addToBackStack(null).commit()
        }

        binding.btnSignin.setOnClickListener {
            val drawableInputError =
                ContextCompat.getDrawable(requireContext(), R.drawable.join_disapproval)

            setFirebaseUIAction(GaAction.LOGIN_EMAIL)

            //이메일과 패스워드 중  이메일  정규식 체크를 먼저 체크하고, 이메일이 완벽하면,
            //패스워드 를  체크한다. 두개다 완료 되었을때, 로그인  실행
            if (checkEmailRegex(
                    binding.inputEmail.getText().toString(),
                    drawableInputError!!
                )
            ) { //이메일 정규식 체크
                Util.hideSoftKeyboard(context, binding.btnSignin)
                if (Const.FEATURE_AUTH2) {
                    domain = Const.DOMAIN_EMAIL
                }

                if (BuildConfig.CHINA) {
                    PushyUtil.registerDevice(requireActivity(), wrap)
                } else {
                    registerDevice(activity, wrap)
                }
            }
        }

        binding.btnForgotId.setOnClickListener {
            findId()
        }

        binding.btnForgotPasswd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ForgotPasswdFragment())
                .addToBackStack(null).commit()
        }
    }

    private fun findId() {
        var gmail: String? = Util.getGmail(requireContext())

        if (gmail.isNullOrEmpty()) {
            gmail = Util.getDeviceUUID(requireContext())
        }

        if (!TextUtils.isEmpty(gmail)) {
            lifecycleScope.launch {
                usersRepository.findId(
                    Util.getDeviceUUID(requireContext()),
                    { response ->
                        var ids = ""

                        /*
                        * <domain>: K|F|L|G|E
                        * <domain_desc>: "카카오 계정"|"Facebook 계정"|"라인계정"|"구글계정"|"이메일 계정" (다국어 처리됨)
                        * */

                        try {
                            val array = response?.getJSONArray("ids") ?: JSONArray()
                            if (array.length() > 0) {
                                for (i in 0 until array.length()) {
                                    val idInfo = array.optJSONObject(i)
                                    ids += if (idInfo != null && idInfo.optString(
                                            "domain"
                                        ).equals("E", ignoreCase = true)
                                    ) {
                                        idInfo.optString("email")
                                    } else {
                                        idInfo!!.optString("domain_desc")
                                    }
                                    ids += "\n"
                                }
                                ids = ids.trim { it <= ' ' }

                                showFindIdDialog(ids)
                            } else {
                                showFindIdDialog(null)
                            }
                        } catch (e: JSONException) {
                            showFindIdDialog(null)
                        }
                    }, { throwable ->
                        if( throwable is UnknownHostException) {
                            showMessage(throwable.message)
                        } else {
                            showFindIdDialog(null)
                        }
                    }
                )
            }
        }
    }

    private fun showFindIdDialog(email: String?) {
        FindIdDialogFragment.getInstance(email).show(
            baseActivity!!
                .supportFragmentManager,
            "findid"
        )
    }

    //이메일 정규식을  체크하여,  맞으먄  true 틀리면  false를 반환한다.
    private fun checkEmailRegex(writtenEmail: String, drawableInputError: Drawable): Boolean {
        drawableInputError.setBounds(
            0,
            0,
            drawableInputError.intrinsicWidth,
            drawableInputError.intrinsicHeight
        )
        if (TextUtils.isEmpty(writtenEmail)) { //아무것도 안써져 있을때
            binding.inputEmail.requestFocus()
            binding.inputEmail.setError(getString(R.string.required_field), drawableInputError)
            binding.inputPasswd.error = null
            binding.inputPasswd.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(writtenEmail).matches()) { //이메일 정규식이 안맞을때
            binding.inputEmail.requestFocus()
            binding.inputEmail.setError(getString(R.string.invalid_email_form), drawableInputError)
            binding.inputPasswd.error = null
            binding.inputPasswd.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            return false
        } else {
            binding.inputEmail.error = null
            binding.inputEmail.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            return true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}