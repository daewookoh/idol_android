package net.ib.mn.fragment

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerLib
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount.Companion.createAccount
import net.ib.mn.activity.AuthActivity
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.StartupActivity.Companion.createIntent
import net.ib.mn.core.data.repository.TimestampRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepositoryImpl
import net.ib.mn.databinding.FragmentKakaoMoreBinding
import net.ib.mn.dialog.ProgressDialogFragment
import net.ib.mn.fragment.SignupFragment.OnRegistered
import net.ib.mn.gcm.GcmUtils.registerDevice
import net.ib.mn.model.ConfigModel
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
import java.util.Objects
import javax.inject.Inject

@AndroidEntryPoint
class GoogleMoreFragment : BaseFragment(), View.OnClickListener {
    private var mEmail: String? = null
    private var mPasswd: String? = null
    private var mName: String? = null
    private var mProfileUrl: String? = null

    private var mDrawableInputOk: Drawable? = null
    private var mDrawableInputError: Drawable? = null
    private var mPrevActionBarTitle: CharSequence? = null

    private var isNickName = false

    private var isBadWordsNickName = false
    @Inject
    lateinit var usersRepository: UsersRepositoryImpl
    @Inject
    lateinit var timestampRepository: TimestampRepositoryImpl

    var wrap: OnRegistered = OnRegistered { deviceKey: String? -> this.processSignup(deviceKey) }

    private val agrumnetsData: Unit
        get() {
            mEmail = requireArguments().getString("email")
            mName = requireArguments().getString("name")
            mPasswd = requireArguments().getString("passwd")
            mProfileUrl = requireArguments().getString("profileUrl")
            //googleClient = (GoogleApiClient)getArguments().getSerializable("googleClient");
        }

    private var _binding: FragmentKakaoMoreBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKakaoMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mDrawableInputOk = ContextCompat.getDrawable(requireContext(), R.drawable.join_approval)
        if (mDrawableInputOk != null) {
            mDrawableInputOk!!.setBounds(
                0, 0, mDrawableInputOk!!.intrinsicWidth,
                mDrawableInputOk!!.intrinsicHeight
            )
        }
        mDrawableInputError = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.join_disapproval
        )
        if (mDrawableInputError != null) {
            mDrawableInputError!!.setBounds(
                0, 0,
                mDrawableInputError!!.intrinsicWidth,
                mDrawableInputError!!.intrinsicHeight
            )
        }

        focusChangeListener()

        agrumnetsData

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

        binding.btnSignup.setOnClickListener(this)
    }

    private fun focusChangeListener() {
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

    private fun changeSingupBtnStatus() {
        binding.btnSignup.isEnabled = isNickName
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val actionbar = (activity as BaseActivity?)
            ?.getSupportActionBar()
        mPrevActionBarTitle = actionbar!!.title
        actionbar.setTitle(R.string.title_info_more)
    }

    override fun onResume() {
        super.onResume()
        setFirebaseScreenViewEvent(GaAction.JOIN_FORM_SOCIAL, javaClass.simpleName)
    }

    override fun onDetach() {
        super.onDetach()
        val actionbar = (activity as BaseActivity?)
            ?.getSupportActionBar()
        actionbar!!.setTitle(mPrevActionBarTitle)

        (activity as AuthActivity?)!!.googleApiclientClose()
        //Session.getCurrentSession().close(null);
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_signup) {
            if (localValidate()) {
                // 버튼 연타 방지
                v.isEnabled = false
                v.postDelayed({
                    try {
                        v.isEnabled = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 3000)
                registerDevice(baseActivity, wrap)
            }
        }
    }

    private fun serverValidate(fieldName: String, value: String, editText: AppCompatEditText?) {
        lifecycleScope.launch {
            usersRepository.validate(
                type = fieldName,
                value = value,
                appId = AppConst.APP_ID,
                listener = { response ->
                    Util.log("KakaoMoreFragment $response")
                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(activity, response) ?: return@validate

                        editText?.setError(responseMsg, mDrawableInputError)

                        if (response.optInt("gcode") == ErrorControl.ERROR_88888 && "nickname" == fieldName) {
                            isBadWordsNickName = true
                        } else if (response.optInt("gcode") == ErrorControl.ERROR_1011 && "nickname" == fieldName) {
                            isBadWordsNickName = false
                        }
                        editText?.setCompoundDrawables(null, null, mDrawableInputError, null)
                        binding.btnSignup.isEnabled = false
                        return@validate
                    }

                    if (response.optInt("gcode") == 0 && "nickname" == fieldName) {
                        isBadWordsNickName = false
                    }
                    editText?.apply {
                        error = null
                        setCompoundDrawables(null, null, null, null)
                        setCompoundDrawables(null, null, mDrawableInputOk, null)
                    }
                    binding.btnSignup.isEnabled = true
                },
                errorListener = {
                    if (activity != null && isAdded) editText!!.setError(
                        getString(R.string.error_abnormal_exception),
                        mDrawableInputError
                    )
                }
            )
        }
    }

    private fun processSignup(deviceKey: String?) {
        val recommender = binding.etRecommender.text?.toString() ?: ""

        var domain: String? = null
        if (Const.FEATURE_AUTH2) {
            domain = Const.DOMAIN_GOOGLE
        }

        if (activity != null && isAdded) {
            val listener: (JSONObject) -> Unit = listener@ { response ->
                if (response.optBoolean("success")) {
                    // msg가 있으면 보여주고 로그인 하지 않음
                    if (!response.optString("msg").isEmpty()) {
                        Util.showDefaultIdolDialogWithBtn1(
                            activity,
                            null,
                            response.optString("msg")
                        ) { view: View? ->
                            val startIntent = createIntent(
                                requireActivity()
                            )
                            startActivity(startIntent)
                            requireActivity().finish()
                            Util.closeIdolDialog()
                        }

                        return@listener
                    }
                    setUiActionFirebaseGoogleAnalyticsFragment(GaAction.SIGN_UP.actionValue, GaAction.SIGN_UP.label)

                    Util.setPreference(requireContext(), Const.PREF_FIRST_OPEN, true)

                    val resourceUri = response.optString("resource_uri")
                    val callback = Runnable {
                        registerGoogleProfile(
                            resourceUri
                        ) {
                            // 로그인 후 메인으로 보내야 한다
                            val startIntent = createIntent(
                                requireActivity()
                            )
                            startActivity(startIntent)
                            requireActivity().setResult(Activity.RESULT_OK)
                            requireActivity().finish()
                        }
                    }
                    (activity as AuthActivity?)!!.googleApiclientCloseLogout()
                    trySignin(mEmail, mPasswd, deviceKey, callback)
                } else {
                    val gcode = response.optInt("gcode")
                    val responseMsg = ErrorControl.parseError(activity, response)
                    when (gcode) {
                        RESPONSE_USERS_1011 -> binding.etName.setError(
                            responseMsg,
                            mDrawableInputError
                        )

                        RESPONSE_USERS_1012 -> {
                            binding.etRecommender
                                ?.setError(
                                    responseMsg,
                                    mDrawableInputError
                                )
                            if (activity != null && responseMsg != null) {
                                makeText(activity, responseMsg, Toast.LENGTH_SHORT).show()
                            }
                            if (Util.is_log()) {
                                showMessage(response.optString("msg"))
                            }
                        }

                        else -> {
                            if (activity != null && responseMsg != null) {
                                makeText(activity, responseMsg, Toast.LENGTH_SHORT).show()
                            }
                            if (Util.is_log()) {
                                showMessage(response.optString("msg"))
                            }
                        }
                    }
                }
            }

            val errorListener: (Throwable) -> Unit = { throwable ->
                Util.closeProgress()

                makeText(activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
            }

            var gmail = Util.getGmail(requireContext())
            if(gmail.isEmpty()) {
                gmail = Util.getDeviceUUID(requireContext())
            }

            lifecycleScope.launch {
                timestampRepository.get { date ->
                    launch {
                        if (!isBadWordsNickName) {
                            usersRepository.signUp(
                                domain = domain,
                                email = mEmail!!,
                                passwd = mPasswd!!,
                                name = binding.etName.text.toString(),
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
                        } else {
                            Util.closeProgress()
                        }
                    }
                }
            }
        }
    }


    //	public void trySignin(String email, String passwd, String deviceKey) {
    //		trySignin(email, passwd, deviceKey, null);
    //	}
    fun trySignin(
        email: String?, passwd: String?,
        deviceKey: String?, callback: Runnable?
    ) {
        ProgressDialogFragment.show(baseActivity, "signin", R.string.wait_signin)
        var domain: String? = null
        if (Const.FEATURE_AUTH2) {
            domain = Const.DOMAIN_GOOGLE
        }
        var gmail = Util.getGmail(requireContext())
        if(gmail.isEmpty()) {
            gmail = Util.getDeviceUUID(requireContext())
        }

        lifecycleScope.launch {
            usersRepository.signIn(
                domain = domain,
                email = email!!,
                passwd = passwd!!,
                deviceKey = deviceKey ?: "",
                gmail = gmail,
                deviceId = Util.getDeviceUUID(requireContext()),
                appId = AppConst.APP_ID,
                listener = { response ->
                    if (response.optBoolean("success")) {
                        afterSignin(email, passwd, callback)
                    } else {
                        ProgressDialogFragment.hideAll(baseActivity)
                        UtilK.handleCommonError(activity, response)
                    }
                },
                errorListener = { throwable ->
                    ProgressDialogFragment.hideAll(baseActivity)
                    makeText(activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    fun afterSignin(email: String?, token: String?, callback: Runnable?) {
        ProgressDialogFragment.show(baseActivity, "userinfo", R.string.wait_userinfo)
        var hashToken = Util.md5salt(token ?: "")
        var domain: String? = null
        if (Const.FEATURE_AUTH2) {
            domain = Const.DOMAIN_GOOGLE
            hashToken = token
        }

        val appsFlyerLib = AppsFlyerLib.getInstance()
        val eventValues: MutableMap<String, Any?> = HashMap()
        eventValues["user_id"] = appsFlyerLib.getAppsFlyerUID(requireContext())
        eventValues[AFInAppEventParameterName.REGISTRATION_METHOD] = domain
        appsFlyerLib.logEvent(
            context,
            AFInAppEventType.COMPLETE_REGISTRATION, eventValues
        )

        createAccount(requireContext(), email, hashToken, domain)
        if (callback == null) {
            requireActivity().setResult(Activity.RESULT_OK)
            requireActivity().finish()
        } else {
            callback.run()
        }
    }

    fun showError(msg: String) {
        Util.showDefaultIdolDialogWithBtn1(
            activity,
            null,
            msg
        ) { v: View? -> Util.closeIdolDialog() }

        Util.log("error $msg")
    }


    private fun registerGoogleProfile(
        resourceUri: String,
        callback: Runnable
    ) {
        lifecycleScope.launch {
            usersRepository.updateProfile(
                resourceUri,
                mProfileUrl,
                { response ->
                    if (response.optBoolean("success")) {
                        callback.run()
                    } else {
                        ProgressDialogFragment.hideAll(baseActivity)
                        UtilK.handleCommonError(baseActivity, response)
                    }
                },
                {
                    makeText(
                        activity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun localValidate(): Boolean {
        var isValid = true

        val name = binding.etName.text.toString()
        if (TextUtils.isEmpty(name)) {
            isValid = false
            binding.etRecommender.setError(
                getString(R.string.required_field),
                mDrawableInputError
            )
            return isValid
        } else if (isBadWordsNickName) {
            binding.etName.setError(
                getString(R.string.bad_words),
                mDrawableInputError
            )
        }

        val recommender = binding.etRecommender.text.toString()

        if (!TextUtils.isEmpty(recommender)) {
            if (recommender.trim { it <= ' ' } == name.trim { it <= ' ' }) {
                binding.etRecommender.setError(
                    getString(R.string.msg_no_enter_self),
                    mDrawableInputError
                )
                isValid = false
            }
        }

        return isValid
    }

    companion object {
        private const val RESPONSE_USERS_1011 = 1011
        private const val RESPONSE_USERS_1012 = 1012


        fun newInstance(
            email: String?,
            name: String?,
            passwd: String?,
            profileUrl: String?
        ): GoogleMoreFragment {
            val mFragment = GoogleMoreFragment()

            val bundle = Bundle()
            bundle.putString("email", email)
            bundle.putString("name", name)
            bundle.putString("passwd", passwd)
            bundle.putString("profileUrl", profileUrl)
            //bundle.putSerializable("googleClient", (Serializable) mGoogleApiClient);
            mFragment.arguments = bundle

            return mFragment
        }
    }
}
