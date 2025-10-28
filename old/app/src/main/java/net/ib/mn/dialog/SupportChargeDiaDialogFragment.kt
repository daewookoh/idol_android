package net.ib.mn.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.LinearLayoutCompat
import net.ib.mn.R
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.activity.NewHeartPlusActivity.Companion.createIntent


class SupportChargeDiaDialogFragment : BaseDialogFragment(), View.OnClickListener {
    private var mCloseBtn: AppCompatButton? = null
    private var mHeartShopli: LinearLayoutCompat? = null

    override fun onResume() {
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_charge_diamond, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mCloseBtn = view.findViewById(R.id.btn_close)
        mHeartShopli = view.findViewById(R.id.heartshop_container)

        mCloseBtn?.setOnClickListener(this)
        mHeartShopli?.setOnClickListener(this)
    }

    override fun onActivityCreated(arg0: Bundle?) {
        super.onActivityCreated(arg0)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_close -> dismiss()
            R.id.heartshop_container -> {
                startActivity(createIntent(context, NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP))
                dismiss()
            }
        }
    }

    companion object {
        val diaChargeInstance: SupportChargeDiaDialogFragment
            get() {
                val fragment = SupportChargeDiaDialogFragment()
                fragment.setStyle(STYLE_NO_TITLE, 0)
                return fragment
            }
    }
}
