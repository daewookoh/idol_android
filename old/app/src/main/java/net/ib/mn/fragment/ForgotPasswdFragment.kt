/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.core.data.repository.UsersRepositoryImpl
import net.ib.mn.databinding.FragmentForgotPasswdBinding
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONException
import javax.inject.Inject

@AndroidEntryPoint
class ForgotPasswdFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentForgotPasswdBinding
    private lateinit var mDrawableInputOk: Drawable
    private lateinit var mDrawableInputError: Drawable
    @Inject
    lateinit var usersRepository: UsersRepositoryImpl

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_forgot_passwd, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actionbar = (activity as AppCompatActivity).supportActionBar
        actionbar?.setTitle(R.string.title_change_passwd)

        mDrawableInputOk = ContextCompat.getDrawable(requireContext(), R.drawable.join_approval)!!
        mDrawableInputOk.setBounds(0, 0, mDrawableInputOk.intrinsicWidth, mDrawableInputOk.intrinsicHeight)

        mDrawableInputError = ContextCompat.getDrawable(requireContext(), R.drawable.join_disapproval)!!
        mDrawableInputError.setBounds(0, 0, mDrawableInputError.intrinsicWidth, mDrawableInputError.intrinsicHeight)

        binding.btnConfirm.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        trySubmit()
    }

    private fun trySubmit() {
        if (localValidate()) {
            val email: String = binding.etEmail.text.toString()
            lifecycleScope.launch {
                usersRepository.findPassword(
                    email = email,
                    listener = { response ->
                        try {
                            if (!response.getBoolean("success")) {
                                val responseMsg = ErrorControl.parseError(activity, response)
                                Util.showDefaultIdolDialogWithBtn1(
                                    activity,
                                    null,
                                    responseMsg
                                ) { Util.closeIdolDialog() }
                                return@findPassword
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                        Util.showDefaultIdolDialogWithBtn1(
                            activity,
                            null,
                            getString(R.string.sent_find_pw_mail)
                        ) {
                            Util.closeIdolDialog()
                            requireActivity().onBackPressed()
                        }                    },
                    errorListener = {
                        Toast.makeText(
                            activity,
                            R.string.error_abnormal_exception,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    private fun localValidate(): Boolean {
        var isValid = true
        val email: String = binding.etEmail.text.toString()
        when {
            TextUtils.isEmpty(email) -> {
                isValid = false
                binding.etEmail.setError(getString(R.string.required_field), mDrawableInputError)
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                isValid = false
                binding.etEmail.setError(getString(R.string.invalid_format_email), mDrawableInputError)
            }
            else -> {
                binding.etEmail.error = null
                binding.etEmail.setCompoundDrawablesWithIntrinsicBounds(null, null, mDrawableInputOk, null)
            }
        }
        return isValid
    }


}