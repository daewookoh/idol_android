/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.databinding.BottomSheetWriteArticleBinding
import net.ib.mn.model.IdolModel

class WriteArticleBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetWriteArticleBinding
    private lateinit var mContext: Context

    private var resId: Int = 0
    private var idolModel: IdolModel? = null
    private var isShowPrivate: Boolean = false
    private var isPrivateCallback: ((Boolean) -> Unit)? = null

    override fun getTheme(): Int = R.style.BottomSheetDialogRewardTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(
        requireContext(),
        theme,
    ).apply {
        // landscape 모드에서  bottomsheet 메뉴모두  expand되지 않아서  아래 설정값 넣어줌.
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.isDraggable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_write_article, container, false)

        when (resId) {
            FLAG_COMMUNITY -> setCommunityDialog(idolModel, isShowPrivate, isPrivateCallback)
        }

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    private fun setCommunityDialog(idolModel: IdolModel?, isShowPrivate: Boolean, isPrivateCallback: ((Boolean) -> Unit)?) {
        if (isPrivateCallback == null) {
            return
        }

        binding.cbShowPrivate.isChecked = isShowPrivate

        val idolAccount = IdolAccount.getAccount(mContext)

        if (idolAccount?.most?.resourceUri == idolModel?.resourceUri) {
            binding.clShow.visibility = View.VISIBLE
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.cbShowPrivate.setOnClickListener {
            isPrivateCallback(binding.cbShowPrivate.isChecked)
        }
    }

    companion object {

        const val FLAG_COMMUNITY = 0

        fun newInstance(resId: Int, idolModel: IdolModel?, isShowPrivate: Boolean, isPrivateCallback: ((Boolean) -> Unit)?): WriteArticleBottomSheetFragment {
            val f = WriteArticleBottomSheetFragment()
            f.resId = resId
            f.idolModel = idolModel
            f.isShowPrivate = isShowPrivate
            f.isPrivateCallback = isPrivateCallback
            return f
        }
    }
}