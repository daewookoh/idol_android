/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import com.bumptech.glide.RequestManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.ib.mn.R
import com.bumptech.glide.Glide
import net.ib.mn.databinding.BottomSheetArticleBinding
import net.ib.mn.utils.Util

class ArticleViewMoreBottomSheetFragment : BottomSheetDialogFragment() {

    private var mGlideRequestManager: RequestManager? = null

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    private var _binding: BottomSheetArticleBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(
        requireContext(),
        theme,
    ).apply {
        // landscape 모드에서  bottomsheet 메뉴모두  expand되지 않아서  아래 설정값 넣어줌.
        this.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        mGlideRequestManager = Glide.with(this)

        if (savedInstanceState != null) {
            showEdit = savedInstanceState.getBoolean(KEY_STATE_SHOW_EDIT)
            showRemove = savedInstanceState.getBoolean(KEY_STATE_SHOW_REMOVE)
            showReport = savedInstanceState.getBoolean(KEY_STATE_SHOW_REPORT)
            showShare = savedInstanceState.getBoolean(KEY_STATE_SHOW_SHARE)
        }

        _binding = BottomSheetArticleBinding.inflate(inflater, container, false)
        setArticleBoardBase(showEdit, showRemove, showReport, showShare)

        return binding.root
    }

    private fun setArticleBoardBase(showEdit: Boolean, showRemove: Boolean, showReport: Boolean, showShare: Boolean): Unit = with(binding){
        val textViewList =
            arrayListOf<AppCompatTextView>(tvArticleEdit, tvArticleDelete, tvArticleReport)

        if (showEdit) {
            tvArticleEdit.visibility = View.VISIBLE
        } else {
            textViewList.remove(tvArticleEdit)
            tvArticleEdit.visibility = View.GONE
        }

        if (showRemove) {
            tvArticleDelete.visibility = View.VISIBLE
        } else {
            textViewList.remove(tvArticleDelete)
            tvArticleDelete.visibility = View.GONE
        }

        if (showReport) {
            tvArticleReport.visibility = View.VISIBLE
        } else {
            textViewList.remove(tvArticleReport)
            tvArticleReport.visibility = View.GONE
        }

        if (showShare) {
            tvArticleShare.visibility = View.VISIBLE
        } else {
            textViewList.remove(tvArticleShare)
            tvArticleShare.visibility = View.GONE
        }

        // 텍스트뷰 1개만 남아있을땐 minheight를 110으로 할경우 아래 빈공간이 많이 남으므로 조정해준다.
        if (textViewList.size == 1) {
            llBottomSheetBoardFilter.minimumHeight = Util.convertDpToPixel(requireContext(), 70f).toInt()
        }

        tvArticleEdit.setOnClickListener {
            onClickEdit()
            dismiss()
        }

        tvArticleDelete.setOnClickListener {
            onClickDelete()
            dismiss()
        }

        tvArticleReport.setOnClickListener {
            onClickReport()
            dismiss()
        }

        tvArticleShare.setOnClickListener {
            onClickShare()
            dismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_STATE_SHOW_EDIT, showEdit)
        outState.putBoolean(KEY_STATE_SHOW_REMOVE, showRemove)
        outState.putBoolean(KEY_STATE_SHOW_REPORT, showReport)
        outState.putBoolean(KEY_STATE_SHOW_SHARE, showShare)
    }

    companion object {

        const val KEY_STATE_SHOW_EDIT = "show_edit"
        const val KEY_STATE_SHOW_REMOVE = "show_remove"
        const val KEY_STATE_SHOW_REPORT = "show_report"
        const val KEY_STATE_SHOW_SHARE = "show_share"
        lateinit var onClickEdit: () -> Unit
        lateinit var onClickDelete: () -> Unit
        lateinit var onClickReport: () -> Unit
        lateinit var onClickShare: () -> Unit
        var showEdit: Boolean = false
        var showRemove: Boolean = false
        var showReport: Boolean = false
        var showShare: Boolean = false
        @JvmStatic
        fun newInstance(
            showEdit: Boolean,
            showRemove: Boolean,
            showReport: Boolean,
            showShare: Boolean,
            onClickEdit: () -> Unit,
            onClickDelete: () -> Unit,
            onClickReport: () -> Unit,
            onClickShare: () -> Unit
        ): ArticleViewMoreBottomSheetFragment {
            this.showEdit = showEdit
            this.showRemove = showRemove
            this.showReport = showReport
            this.showShare = showShare
            this.onClickEdit = onClickEdit
            this.onClickDelete = onClickDelete
            this.onClickReport = onClickReport
            this.onClickShare = onClickShare
            return ArticleViewMoreBottomSheetFragment()
        }
    }
}