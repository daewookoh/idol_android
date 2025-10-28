package net.ib.mn.dialog

import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.base.BaseBottomSheetFragment
import net.ib.mn.databinding.DialogNotificationSettingBinding
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.setFirebaseUIAction

@AndroidEntryPoint
class NotificationSettingDialog(
    val dismissOnConfirm: () -> Unit = {},
) : BaseBottomSheetFragment<DialogNotificationSettingBinding>() {

    override val layoutResId = R.layout.dialog_notification_setting

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onConfirmClick() {
        setFirebaseUIAction(GaAction.SYS_PUSH)
        UtilK.openNotificationSettings(requireContext())
        dismissOnConfirm()
        dismiss()
    }
}