/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 비밀번호 변경 화면
 *
 * */

package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnAttach
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ActivityChangePasswordBinding
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject

@AndroidEntryPoint
class ChangePasswdActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var mDrawableInputOk: Drawable
    private lateinit var mDrawableInputError: Drawable

    //가능 유무에 따라 가입하기 버튼 색 변경하기 위한 변수
    private var  isPresentPasswd = false
    private var isPasswd = false
    private var isPasswdConfirm = false

    // 비밀 번호 확인 텍스트를 한 번이라도 변경했나 여부 확인.
    var hashChangedConfirmPasswdText = false

    @Inject
    lateinit var usersRepository: UsersRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionbar = supportActionBar!!
        actionbar.setTitle(R.string.title_change_passwd)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_change_password)

        binding.btnSubmit.setOnClickListener(this)

        mDrawableInputOk = ContextCompat.getDrawable(this, R.drawable.join_approval)!!
        mDrawableInputOk.setBounds(0, 0, mDrawableInputOk.intrinsicWidth, mDrawableInputOk.intrinsicHeight)

        mDrawableInputError = ContextCompat.getDrawable(this, R.drawable.join_disapproval)!!
        mDrawableInputError.setBounds(0, 0, mDrawableInputError.intrinsicWidth, mDrawableInputError.intrinsicHeight)

        setConfirmBtnStatus()
        setupEdgeToEdgeForChangePassword()
        pwdTextChangeListener()
    }

    override fun onClick(v: View?) {
        when(v){
            binding.btnSubmit -> {
                trySubmit()
            }
        }
    }

    private fun setupEdgeToEdgeForChangePassword() {
        val root = binding.clContainer

        // 1) E2E
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // (배경이 밝으면) 라이트 아이콘
        WindowInsetsControllerCompat(window, root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 현재 XML에 있는 기본 패딩값(예: top=18dp, bottom=0) 보존
        val baseTop = root.paddingTop
        val baseBottom = root.paddingBottom

        // 2) IME 애니메이션 후 재적용(일부 단말 0 inset/깜빡임 방지)
        ViewCompat.setWindowInsetsAnimationCallback(
            root,
            object : WindowInsetsAnimationCompat.Callback(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
            ) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: List<WindowInsetsAnimationCompat?>
                ): WindowInsetsCompat = insets

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    root.post { ViewCompat.requestApplyInsets(root) }
                }
            }
        )

        // 3) 인셋을 루트 패딩으로만 처리 (하위 뷰는 그대로)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val nav    = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime    = insets.getInsets(WindowInsetsCompat.Type.ime())

            // 상단: 상태바 + 기존 top padding(18dp)
            val topPad = baseTop + status.top
            // 하단: 키보드가 보이면 IME, 아니면 내비
            val bottomInset = maxOf(nav.bottom, ime.bottom)
            val bottomPad = baseBottom + bottomInset

            v.setPadding(v.paddingLeft, topPad, v.paddingRight, bottomPad)

            // 버튼은 parent bottom에 붙어있으니, parent의 bottom padding만큼 자동으로 위로 뜸
            insets
        }

        if (root.isAttachedToWindow) ViewCompat.requestApplyInsets(root)
        else root.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    //비밀번호 상태에 따라 버튼 클릭 가능 여부 및 배경 색 변경
    private fun setConfirmBtnStatus() {
        if (isPresentPasswd && isPasswd && isPasswdConfirm) {
            binding.btnSubmit.apply {
                isEnabled = true
                setBackgroundResource(R.drawable.bg_round_boarder_main)
            }

            return
        }
        binding.btnSubmit.apply {
            isEnabled = false
            setBackgroundResource(R.drawable.bg_round_boarder_gray200)
        }
    }

    //비밀번호 text 변경할 때마다 작성한 비밀번호 상태에 따른 Listener 처리
    private fun pwdTextChangeListener() {

        binding.etPresentPasswd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val currentPasswd: String = binding.etPresentPasswd.text.toString()
                val hashCurrentPasswd = Util.md5salt(currentPasswd)
                when {
                    TextUtils.isEmpty(currentPasswd) -> {
                        setError(binding.etPresentPasswd, R.string.required_field)
                        isPresentPasswd = false
                    }
                    hashCurrentPasswd != IdolAccount.getAccount(this@ChangePasswdActivity)?.token -> {
                        setError(binding.etPresentPasswd, R.string.passwd_not_match)
                        isPresentPasswd = false
                    }
                    else -> {
                        setDrawable(binding.etPresentPasswd, mDrawableInputOk)
                        isPresentPasswd = true
                    }
                }
                setConfirmBtnStatus()
            }
        })

        binding.etPasswd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val passwd: String = binding.etPasswd.text.toString()
                when {
                    TextUtils.isEmpty(passwd) -> {
                        setError(binding.etPasswd, R.string.required_field)
                        isPasswd = false
                    }
                    passwd.length < 8 -> {
                        setError(binding.etPasswd, R.string.too_short_passwd)
                        isPasswd = false
                    }
                    // 비밀 번호 텍스트가 바뀌지 않았더라면 무조건 빈 공백과 비교하므로 false로 세팅되버림.그래서 hashChangedConfirmPasswdText 변수를 추가하여 비밀번호 확인 텍스트를 한 번이라도 변경했는지 확인.
                    binding.etPasswdConfirm.text.toString() != passwd && hashChangedConfirmPasswdText -> {
                        setError(binding.etPasswd, R.string.passwd_confirm_not_match)
                        isPasswd = false
                    }
                    else -> {
                        if (UtilK.checkPwdRegex(passwd)) {
                            setStatusBothPassword(
                                binding.etPasswd,
                                binding.etPasswdConfirm
                            ) { isSatisfiedAnotherCondition ->
                                isPasswdConfirm = isSatisfiedAnotherCondition
                            }
                            setDrawable(binding.etPasswd, mDrawableInputOk)
                            isPasswd = true
                        } else {
                            setError(binding.etPasswd, R.string.check_pwd_requirement)
                            isPasswd = false
                        }
                    }
                }
                setConfirmBtnStatus()
            }
        })

        binding.etPasswdConfirm.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                hashChangedConfirmPasswdText = true
                val passwdConfirm: String = binding.etPasswdConfirm.text.toString()
                when {
                    TextUtils.isEmpty(passwdConfirm) -> {
                        setError(binding.etPasswdConfirm, R.string.required_field)
                        isPasswdConfirm = false
                    }
                    binding.etPasswd.text.toString() != passwdConfirm -> {
                        setError(binding.etPasswdConfirm, R.string.passwd_confirm_not_match)
                        isPasswdConfirm = false
                    }
                    else -> {
                        if (UtilK.checkPwdRegex(passwdConfirm)) {
                            setStatusBothPassword(
                                binding.etPasswd,
                                binding.etPasswdConfirm
                            ) { isSatisfiedAnotherCondition ->
                                isPasswd = isSatisfiedAnotherCondition
                            }
                            setDrawable(binding.etPasswdConfirm, mDrawableInputOk)
                            isPasswdConfirm = true
                        } else {
                            setError(binding.etPasswdConfirm, R.string.check_pwd_requirement)
                            isPasswdConfirm = false
                        }
                    }
                }
                setConfirmBtnStatus()
            }
        })
    }

    private fun trySubmit() {
        val passwd: String = binding.etPasswd.text.toString()
        val hashNewPasswd = Util.md5salt(passwd) ?: return

        lifecycleScope.launch {
            usersRepository.changePassword(hashNewPasswd,
                listener = { response ->
                    if (response.optBoolean("success")) {
                        val account = IdolAccount
                            .getAccount(this@ChangePasswdActivity)
                        //String hashPasswd = Util.md5salt(hashNewPasswd);
                        account?.setToken(this@ChangePasswdActivity, hashNewPasswd)
                        Util.showDefaultIdolDialogWithBtn1(
                            this@ChangePasswdActivity,
                            null,
                            getString(R.string.applied)
                        ) { v: View? ->
                            Util.closeIdolDialog()
                            binding.btnSubmit.isEnabled = true
                            finish()
                        }
                    } else {
                        UtilK.handleCommonError(this@ChangePasswdActivity, response)
                    }

                },
                errorListener = { throwable ->
                    makeText(
                        this@ChangePasswdActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.btnSubmit.isEnabled = true
                }
            )
        }
    }

    private fun setError(editText: EditText, errorMessageResId: Int) {
        editText.setError(getString(errorMessageResId), mDrawableInputError)
    }

    private fun setDrawable(editText: EditText, drawable: Drawable) {
        editText.setCompoundDrawables(null, null, null, null)
        editText.setCompoundDrawables(null, null, drawable, null)
    }

    //한개 EditText 수정 하고 있을때 다른 EditText 수정 되는지 확인이 필요.
    private fun setStatusBothPassword(
        passwordView: EditText,
        passwordConfirmView: EditText,
        isSatisfiedCondition: (Boolean) -> Unit
    ) {
        val password = passwordView.text.toString()
        val passwordConfirm = passwordConfirmView.text.toString()

        val isEmptyPasswd = TextUtils.isEmpty(password) || TextUtils.isEmpty(passwordConfirm)
        val isSamePasswd = password == passwordConfirm

        if (!isSamePasswd || isEmptyPasswd) {
            return
        }

        setDrawable(passwordView, mDrawableInputOk)
        setDrawable(passwordConfirmView, mDrawableInputOk)
        isSatisfiedCondition(true)
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, ChangePasswdActivity::class.java)
        }
    }
}
