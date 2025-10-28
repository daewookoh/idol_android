/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: Snack Bar custom
 *
 * */

package net.ib.mn.utils

import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.SnackBarCustomLayoutBinding


class IdolSnackBar(view: View, private val message: String?) {

    private val context = view.context
    private val snackbar = Snackbar.make(view, "", if(BuildConfig.CELEB) 2000 else 1500) // 셀럽은 1.5초가 빠르다고 하여 2초로 설정함
    private val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout

    private val inflater = LayoutInflater.from(context)
    private val snackbarBinding: SnackBarCustomLayoutBinding = DataBindingUtil.inflate(inflater, R.layout.snack_bar_custom_layout, null, false)

    init {
        initView()
        initData()
    }

    private fun initView() {
        with(snackbarLayout) {
            removeAllViews()
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            addView(snackbarBinding.root, 0)
        }
    }

    private fun initData() {
        snackbarBinding.tvContent.text = message
    }

    fun show() {
        snackbar.show()
    }

    fun setAnchorView(anchor: View): IdolSnackBar {
        snackbar.setAnchorView(anchor)
        return this
    }

    companion object {
        fun make(view: View, message: String?) = IdolSnackBar(view, message)
    }
}