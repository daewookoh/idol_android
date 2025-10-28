/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 바텀 다이얼로그 아이템 RecyclerView 전용.
 *
 * */

package net.ib.mn.dialog

import android.content.DialogInterface
import net.ib.mn.R
import net.ib.mn.awards.adapter.BottomSheetAwardsFilterAdapter
import net.ib.mn.base.BaseBottomSheetDialogFragment
import net.ib.mn.databinding.BottomSheetRcyFilterBinding
import net.ib.mn.core.model.AwardChartsModel

class BottomSheetRcyDialogFragment :
    BaseBottomSheetDialogFragment<BottomSheetRcyFilterBinding>(R.layout.bottom_sheet_rcy_filter) {

    private var chartCodes: List<AwardChartsModel> = listOf()

    private var onClickConfirm: (AwardChartsModel) -> Unit = {}
    private var onClickDismiss: () -> Unit = {}

    override fun BottomSheetRcyFilterBinding.onCreateView() {
        initSet()
    }

    private fun initSet() {

        if (chartCodes.isEmpty()) {
            dismiss()
            return
        }

        binding.rvFilter.adapter = BottomSheetAwardsFilterAdapter(chartCodes).apply {
            this.setOnClickListener(object : BottomSheetAwardsFilterAdapter.OnClickListener {
                override fun onItemClicked(chartModel: AwardChartsModel) {
                    onClickConfirm(chartModel)
                    this@BottomSheetRcyDialogFragment.dismiss()
                }
            })
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onClickDismiss()
    }

    companion object {

        fun getInstance(
            chartCodes: List<AwardChartsModel>,
            onClickConfirm: (AwardChartsModel) -> Unit = {},
            onClickDismiss: () -> Unit = {}
        ): BottomSheetRcyDialogFragment {
            return BottomSheetRcyDialogFragment().apply {
                this.onClickConfirm = onClickConfirm
                this.chartCodes = chartCodes
                this.onClickDismiss = onClickDismiss
            }
        }
    }

}