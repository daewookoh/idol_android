/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: Base 다이얼로그 기본형.
 *
 * */

package net.ib.mn.dialog.base

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import feature.common.exodusimagepicker.util.Util

/**
 * @see
 * */

open class BaseDialog<VDB : ViewDataBinding>(
    private val context: Context,
    theme: Int,
    @LayoutRes val layoutRes: Int,
) : Dialog(context, theme) {

    lateinit var binding: VDB

    init {
        reConfigurationDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.inflate(layoutInflater, layoutRes, null, false)
        binding.onCreate()
        setCanceledOnTouchOutside(false)
        setCancelable(false)
        setContentView(binding.root)
    }

    open fun VDB.onCreate() = Unit

    private fun reConfigurationDialog() {
        val layoutParamWindowManager = WindowManager.LayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            dimAmount = 0.7f
            gravity = Gravity.CENTER
        }
        window?.attributes = layoutParamWindowManager
        window?.setLayout(
            Util.convertDpToPixel(context, 310f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}