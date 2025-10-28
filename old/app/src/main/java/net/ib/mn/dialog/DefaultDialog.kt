/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.dialog

import android.content.Context
import net.ib.mn.R
import net.ib.mn.databinding.DialogDefaultBinding
import net.ib.mn.dialog.base.BaseDialog

/**
 * @see
 * */

class DefaultDialog(
    private val title: String? = null,
    private val subTitle: String? = null,
    private val context: Context,
    theme: Int,
) : BaseDialog<DialogDefaultBinding>(
    context,
    theme,
    R.layout.dialog_default,
) {

    override fun DialogDefaultBinding.onCreate() {
        initSet()
    }

    private fun initSet() = with(binding) {
        tvTitle.text = title
        tvSubTitle.text = subTitle
        ivClose.setOnClickListener {
            dismiss()
        }
    }
}