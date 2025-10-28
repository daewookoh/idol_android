package net.ib.mn.dialog

import android.os.Bundle
import android.text.Spanned
import android.view.*
import android.widget.Button
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import net.ib.mn.R
import net.ib.mn.utils.Util

class CreateChatRoomDialogFragment : BaseDialogFragment() {
    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button
    private lateinit var tvCreateChatRoomMsg: AppCompatTextView
    private lateinit var spannedMsg: Spanned
    override fun onStart() {
        super.onStart()

        if (dialog == null) return

        val lpWindow = WindowManager.LayoutParams()
        val window = dialog!!.window

        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER

        window?.attributes = lpWindow
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setLayout(
            Util.convertDpToPixel(context, 280f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog!!.setCanceledOnTouchOutside(true)
        dialog!!.setCancelable(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_create_chat_room, container, false)
    }
    fun setMessage(spanned: Spanned) {
        spannedMsg = spanned
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnOk = view.findViewById(R.id.btn_confirm)
        btnCancel = view.findViewById(R.id.btn_cancel)
        tvCreateChatRoomMsg= view.findViewById(R.id.text_create_chat_room_msg)


        tvCreateChatRoomMsg.text = spannedMsg

        btnOk.setOnClickListener {
            setResultCode(RESULT_OK)
            dismiss()
        }
        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        fun getInstance(): CreateChatRoomDialogFragment {
            val fragment = CreateChatRoomDialogFragment()
            fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0)

            return fragment
        }
    }
}