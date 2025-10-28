package net.ib.mn.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.AuthActivity
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.StartupActivity.Companion.createIntent
import net.ib.mn.core.data.repository.TimestampRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepositoryImpl
import net.ib.mn.databinding.FragmentSignupBinding
import net.ib.mn.dialog.ProgressDialogFragment
import net.ib.mn.gcm.GcmUtils.registerDevice
import net.ib.mn.model.ConfigModel
import net.ib.mn.pushy.PushyUtil
import net.ib.mn.utils.AppConst
import net.ib.mn.utils.Const
import net.ib.mn.utils.DelayedTextWatcher
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.UtilK.Companion.checkPwdRegex
import net.ib.mn.utils.UtilK.Companion.hasEmoticons
import net.ib.mn.utils.setFirebaseScreenViewEvent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects
import javax.inject.Inject

@AndroidEntryPoint
class SignupFragment : BaseFragment(), View.OnClickListener {
    private var mDrawableInputOk: Drawable? = null
    private var mDrawableInputError: Drawable? = null
    private var mPrevActionBarTitle: CharSequence? = null
    @Inject
    lateinit var usersRepository: UsersRepositoryImpl
    @Inject
    lateinit var timestampRepository: TimestampRepositoryImpl

    var wrap: OnRegistered = OnRegistered { id: String? -> processSignup(id) }


    private var isBadWords = false

    //가능 유무에 따라 가입하기 버튼 색 변경하기 위한 변수
    private var isEmail = false
    private var isPasswd = false
    private var isPasswdConfirm = false
    private var isNickName = false

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val actionbar = (activity as BaseActivity?)
            ?.getSupportActionBar()
        mPrevActionBarTitle = actionbar!!.title
        actionbar.setTitle(getString(R.string.title_signup))
    }

    override fun onDetach() {
        super.onDetach()
        val actionbar = (activity as BaseActivity?)
            ?.getSupportActionBar()
        actionbar!!.setTitle(mPrevActionBarTitle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mDrawableInputOk = resources.getDrawable(R.drawable.join_approval)
        mDrawableInputOk?.let {
            it.setBounds(
                0,
                0,
                it.getIntrinsicWidth(),
                it.getIntrinsicHeight()
            )
        }
        mDrawableInputError = resources.getDrawable(R.drawable.join_disapproval)
        mDrawableInputError?.let {
            it.setBounds(
                0,
                0,
                it.getIntrinsicWidth(),
                it.getIntrinsicHeight()
            )
        }

        binding.btnSignup.setOnClickListener(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.scvSignupRoot) { v, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, maxOf(nav.bottom, ime.bottom))
            insets
        }

        val inviteeHeart = ConfigModel.getInstance(requireContext()).inviteeHeart
        binding.tvRecommenderHint.text = if (inviteeHeart > 0) {
            getString(R.string.signup_desc2, inviteeHeart)
        } else {
            getString(R.string.signup_desc2_noreward)
        }

        val inviteCode: String? = UtilK.getInviteCodeFromClipboard(requireContext())
        if (!inviteCode.isNullOrEmpty()) {
            makeText(requireContext(), R.string.signup_toast_auto_fill_recommender, Toast.LENGTH_SHORT).show()
            binding.etRecommender.setText(inviteCode)
        }

        focusChangeListener()
    }

    override fun onResume() {
        super.onResume()
        setFirebaseScreenViewEvent(GaAction.JOIN_FORM_EMAIL, javaClass.simpleName)
    }

    private fun focusChangeListener() {
        binding.etEmail.addTextChangedListener(
            DelayedTextWatcher(
                1000
            ) { s: CharSequence? ->
                changeSingupBtnStatus()
                binding.etEmail.setCompoundDrawables(null, null, null, null)

                if (binding.etEmail.text == null) return@DelayedTextWatcher

                val email = binding.etEmail.text.toString()
                if (isEmailValid(binding.etEmail.text.toString())) {
                    serverValidate("email", email, binding.etEmail)
                }
            }
        )

        binding.etPasswd.onFocusChangeListener = OnFocusChangeListener { v: View?, hasFocus: Boolean ->
            changeSingupBtnStatus()
            if (hasFocus) {
                return@OnFocusChangeListener
            }
            binding.etPasswd.setCompoundDrawables(null, null, null, null)
            val passwd = binding.etPasswd.text.toString()
            if (passwd.isEmpty()) {
                binding.etPasswd.setError(
                    getString(R.string.required_field),
                    mDrawableInputError
                )
                isPasswd = false
            } else if (!checkPwdRegex(passwd)) {
                binding.etPasswd.setError(
                    getString(R.string.check_pwd_requirement),
                    mDrawableInputError
                )
                isPasswd = false
            } else {
                binding.etPasswd.error = null
                binding.etPasswd.setCompoundDrawables(
                    null, null,
                    mDrawableInputOk, null
                )
                isPasswd = true
            }
        }

        binding.etPasswdConfirm.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            changeSingupBtnStatus()
            if (hasFocus) {
                return@OnFocusChangeListener
            }
            binding.etPasswdConfirm.setCompoundDrawables(null, null, null, null)
            val passwdConfirm = binding.etPasswdConfirm.text
                .toString()
            if (passwdConfirm.isEmpty()) {
                binding.etPasswdConfirm.setError(
                    getString(R.string.required_field),
                    mDrawableInputError
                )
                isPasswdConfirm = false
            } else if (passwdConfirm != binding.etPasswd.text
                    .toString()
            ) {
                binding.etPasswdConfirm
                    ?.setError(
                        getString(R.string.passwd_confirm_not_match),
                        mDrawableInputError
                    )
                isPasswdConfirm = false
            } else {
                binding.etPasswdConfirm.error = null
                binding.etPasswdConfirm.setCompoundDrawables(
                    null,
                    null, mDrawableInputOk, null
                )
                isPasswdConfirm = true
            }
        }

        binding.etName.onFocusChangeListener = OnFocusChangeListener { v: View?, hasFocus: Boolean ->
            changeSingupBtnStatus()
            if (hasFocus) {
                return@OnFocusChangeListener
            }
            binding.etName.setCompoundDrawables(null, null, null, null)
            v("checkcheck ->  포커스 달라짐 ")
            var isValid = true
            val name = binding.etName.text.toString()
            if (name.isEmpty()) {
                isValid = false
                binding.etName.setError(
                    getString(R.string.required_field),
                    mDrawableInputError
                )
                isNickName = false
            }
            if (isValid) {
                serverValidate("nickname", name, binding.etName)
            }
        }

        var nicknameDebounceJob: Job? = null
        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.etName.setCompoundDrawables(null, null, null, null)

                if (hasEmoticons(Objects.requireNonNull(binding.etName.text).toString())) {
                    binding.etName.error = getString(R.string.error_2090)
                    isNickName = false
                    changeSingupBtnStatus()
                    return
                }

                nicknameDebounceJob?.cancel()
                val nickname = binding.etName.text?.toString() ?: ""
                if (nickname.isNotEmpty()) {
                    nicknameDebounceJob = lifecycleScope.launch {
                        delay(1500)
                        if (nickname == binding.etName.text?.toString()) {
                            serverValidate("nickname", nickname, binding.etName)
                        }
                    }
                }
            }
        })

        var recommenderDebounceJob: Job? = null
        binding.etRecommender.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etRecommender.setCompoundDrawables(null, null, null, null)
                recommenderDebounceJob?.cancel()
                val recommender = binding.etRecommender.text?.toString() ?: ""
                if (recommender.isNotEmpty()) {
                    recommenderDebounceJob = lifecycleScope.launch {
                        delay(1500)
                        if (recommender == binding.etRecommender.text?.toString()) {
                            serverValidate("referral_code", recommender, binding.etRecommender)
                        }
                    }
                }
            }
        })

        binding.etRecommender.onFocusChangeListener =
            OnFocusChangeListener { v: View?, hasFocus: Boolean ->
                changeSingupBtnStatus()
                if (hasFocus) {
                    return@OnFocusChangeListener
                }
                binding.etRecommender.setCompoundDrawables(null, null, null, null)
                val recommender = binding.etRecommender.text.toString()
                if (recommender.isNotEmpty()) {
                    serverValidate(
                        "referral_code", recommender,
                        binding.etRecommender
                    )
                }
            }
    }

    private fun isEmailValid(email: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.setError(getString(R.string.required_field), mDrawableInputError)
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError(getString(R.string.invalid_format_email), mDrawableInputError)
            return false
        }
        return true
    }

    private fun serverValidate(fieldName: String, value: String, editText: EditText?) {
        lifecycleScope.launch {
            usersRepository.validate(
                type = fieldName,
                value = value,
                appId = AppConst.APP_ID,
                listener = { response ->
                    if (!response.optBoolean("success")) {
                        val responseMsg = response.optString("msg")
                        editText!!.setError(responseMsg, mDrawableInputError)

                        if (response.optInt("gcode") == ErrorControl.ERROR_88888 && "nickname" == fieldName) {
                            isBadWords = true
                        } else if (response.optInt("gcode") == ErrorControl.ERROR_1011 && "nickname" == fieldName) {    //중복되는 닉네임
                            isBadWords = false
                        }
                        isNickName = false
                        changeSingupBtnStatus()
                        return@validate
                    }

                    if (response.optInt("gcode") == 0) {
                        if ("nickname" == fieldName) {
                            isNickName = true
                            isBadWords = false
                        } else if ("email" == fieldName) {
                            isEmail = true
                        }
                    }
                    editText!!.error = null
                    editText.setCompoundDrawables(null, null, mDrawableInputOk, null)
                    changeSingupBtnStatus()
                },
                errorListener = {
                    editText!!.setError(
                        getString(R.string.error_abnormal_exception),
                        mDrawableInputError
                    )

                }
            )
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_signup) {    //가입하기 버튼 눌렀을 경우
            if (localValidate()) {    //형식 맞는지 체크
                // 버튼 연타 방지
                v.isEnabled = false
                v.postDelayed({
                    try {
                        v.isEnabled = true
                    } catch (e: Exception) {
                    }
                }, 3000)

                trySignup()
            }
        }
    }

    private fun changeSingupBtnStatus() {
        binding.btnSignup.isEnabled = isEmail && isPasswd && isPasswdConfirm && isNickName
    }

    private fun localValidate(): Boolean {    //가입하기 누를 시 체크 후 상태에 따라 isValid return
        var isValid = true
        val email = binding.etEmail.text.toString()
        if (email.isEmpty()) {
            isValid = false
            binding.etEmail.setError(
                getString(R.string.required_field),
                mDrawableInputError
            )
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {    //이메일 형식인지 체크
            isValid = false
            binding.etEmail.setError(
                getString(R.string.invalid_format_email),
                mDrawableInputError
            )
        }

        val name = binding.etName.text.toString()
        if (name.isEmpty()) {
            isValid = false
            binding.etName.setError(
                getString(R.string.required_field),
                mDrawableInputError
            )
        } else if (isBadWords) {
            binding.etName.setError(
                getString(R.string.bad_words),
                mDrawableInputError
            )
        }

        val passwd = binding.etPasswd.text.toString()
        if (passwd.isEmpty()) {
            isValid = false
            binding.etPasswd.setError(
                getString(R.string.required_field),
                mDrawableInputError
            )
        } else if (!checkPwdRegex(passwd)) {
            isValid = false
            binding.etPasswd.setError(
                getString(R.string.check_pwd_requirement),
                mDrawableInputError
            )
        } else {
            binding.etPasswd.error = null
            binding.etPasswd.setCompoundDrawablesWithIntrinsicBounds(
                null, null,
                mDrawableInputOk, null
            )
        }

        val passwdConfirm = binding.etPasswdConfirm.text.toString()
        if (passwdConfirm.isEmpty()) {
            isValid = false
            binding.etPasswdConfirm.setError(
                getString(R.string.required_field),
                mDrawableInputError
            )
        } else if (passwdConfirm != passwd) {
            isValid = false
            binding.etPasswdConfirm.setError(
                getString(R.string.passwd_confirm_not_match),
                mDrawableInputError
            )
        } else {
            binding.etPasswdConfirm.error = null
            binding.etPasswdConfirm.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null, mDrawableInputOk, null
            )
        }

        val recommender = binding.etRecommender.text.toString()
        if (recommender.isNotEmpty() && recommender.trim { it <= ' ' } == name.trim { it <= ' ' }) {
            binding.etRecommender.setError(
                getString(R.string.msg_no_enter_self),
                mDrawableInputError
            )
            isValid = false
        } else {
            binding.etRecommender.error = null
            binding.etRecommender.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null, mDrawableInputOk, null
            )
        }

        return isValid
    }

    private fun saveSignupTime() {
        val format = SimpleDateFormat("yyyy-MM-dd:HH:mm:ss", Locale.US)
        Util.putLocalCache("signup_time", format.format(Date()))
    }

    private fun trySignup() {
        registerDevice(activity, wrap)
        PushyUtil.registerDevice(activity, wrap)
    }

    fun processSignup(deviceKey: String?) {
        ProgressDialogFragment.show(
            baseActivity, "signup",
            R.string.wait_signup
        )
        val email = binding.etEmail.text.toString()
        val name = binding.etName.text.toString()
        val passwd = binding.etPasswd.text.toString()
        val recommender = binding.etRecommender.text.toString()
        val domain = if (Const.FEATURE_AUTH2) {
            Const.DOMAIN_EMAIL
        } else {
            null
        }

        // 회원가입 다 하고나서 여기가 불릴 때가 있음
        if (activity == null) return

        val listener: (JSONObject) -> Unit =  { response ->
            ProgressDialogFragment
                .hide(baseActivity, "signup")
            v("isbadwords ->$isBadWords response ->$response")
            if (response.optBoolean("success") && !isBadWords) {
                saveSignupTime()
                val parent = activity as AuthActivity?

                // msg가 있으면 보여주고 로그인 하지 않음
                if (!response.optString("msg").isEmpty()) {
                    Util.setPreference(requireContext(), Const.PREF_FIRST_OPEN, true)

                    Util.showDefaultIdolDialogWithBtn1(
                        activity,
                        null,
                        response.optString("msg")
                    ) { view: View? ->
                        val startIntent = createIntent(
                            requireActivity()
                        )
                        startIntent.putExtra(Const.IS_EMAIL_SIGNUP, "true")
                        startActivity(startIntent)
                        requireActivity().finish()
                        Util.closeIdolDialog()
                    }
                } else {
                    setUiActionFirebaseGoogleAnalyticsFragment(GaAction.SIGN_UP.actionValue, GaAction.SIGN_UP.label)
                    parent!!.trySignin(email, passwd, deviceKey!!, domain!!)
                }
            } else {
                if (isBadWords) {
                    binding.etName.setError(
                        getString(R.string.bad_words),
                        mDrawableInputError
                    )
                }

                val gcode = response.optInt("gcode")
                val responseMsg = ErrorControl.parseError(
                    activity, response
                )
                when (gcode) {
                    RESPONSE_USERS_1001 -> binding.etEmail.setError(
                        responseMsg,
                        mDrawableInputError
                    )

                    RESPONSE_USERS_1011 -> binding.etName.setError(
                        responseMsg,
                        mDrawableInputError
                    )

                    RESPONSE_USERS_1012 -> binding.etRecommender.setError(
                        responseMsg,
                        mDrawableInputError
                    )

                    RESPONSE_USERS_1013 -> Util.showDefaultIdolDialogWithBtn1(
                        activity,
                        null,
                        responseMsg
                    ) { v: View? -> Util.closeIdolDialog() }

                    else -> {
                        makeText(
                            activity, responseMsg,
                            Toast.LENGTH_SHORT
                        ).show()
                        if (Util.is_log()) {
                            showMessage(response.optString("msg"))
                        }
                    }
                }
            }
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            ProgressDialogFragment
                .hide(baseActivity, "signup")
            makeText(
                activity,
                R.string.error_abnormal_exception,
                Toast.LENGTH_SHORT
            ).show()
        }

        var gmail = Util.getGmail(requireContext())
        if(gmail.isEmpty()) {
            gmail = Util.getDeviceUUID(requireContext())
        }
        MainScope().launch {
            timestampRepository.get { date ->
                if(!isBadWords) {
                    launch {
                        usersRepository.signUp(
                            domain = domain,
                            email = email,
                            passwd = passwd,
                            name = name,
                            referralCode = recommender,
                            deviceKey = deviceKey!!,
                            version = getString(R.string.app_version),
                            googleAccount = Const.PARMA_N,
                            time = date?.time ?: 0,
                            appId = AppConst.APP_ID,
                            deviceId = Util.getDeviceUUID(context),
                            gmail = gmail,
                            listener = listener,
                            errorListener = errorListener
                        )
                    }
                } else {
                    ProgressDialogFragment
                        .hide(baseActivity, "signup")
                }
            }
        }
    }

    fun interface OnRegistered {
        fun callback(id: String?)
    }

    companion object {
        private const val RESPONSE_USERS_1001 = 1001
        private const val RESPONSE_USERS_1011 = 1011
        private const val RESPONSE_USERS_1012 = 1012
        private const val RESPONSE_USERS_1013 = 1013
        private const val RESPONSE_USERS_88888 = 88888
    }
}
