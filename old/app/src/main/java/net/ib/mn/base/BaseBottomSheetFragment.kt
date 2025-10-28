/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: 기본 버튼 있는 dialog base fragment
 *
 * */

package net.ib.mn.base

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.ib.mn.R

abstract class BaseBottomSheetFragment<B : ViewDataBinding> : BottomSheetDialogFragment() {

    private var _binding: B? = null
    protected val binding get() = _binding!!

    abstract val layoutResId: Int
    abstract fun onConfirmClick()

    open fun onDismissClick() {
        dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.BottomSheetDialogRewardTheme).apply {
            // 확장 상태 유지하고 싶을 경우
            // behavior.state = BottomSheetBehavior.STATE_EXPANDED
            // behavior.isDraggable = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<B>(inflater, layoutResId, container, false)
        _binding = binding

        binding.root.findViewById<View?>(R.id.btn_close)?.setOnClickListener {
            onDismissClick()
        }

        binding.root.findViewById<View?>(R.id.tv_confirm)?.setOnClickListener {
            onConfirmClick()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}