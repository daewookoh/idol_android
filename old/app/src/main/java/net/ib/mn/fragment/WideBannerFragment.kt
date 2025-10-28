package net.ib.mn.fragment

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import net.ib.mn.R
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import java.text.DateFormat


class WideBannerFragment : WidePhotoFragment() {

    private var idolDialog: Dialog? = null

    companion object {
        private const val PARAM_MODEL = "paramModel"
        const val keyDoNotShow = "banner_do_not_show"

        fun getInstance(model: ArticleModel): WideBannerFragment {
            val fragment = WideBannerFragment()
            val args = Bundle()
            args.putSerializable(PARAM_MODEL, model)
            fragment.arguments = args

            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isWideBanner = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnHeartBox.visibility = View.GONE

        val dateString = if (baseWidePhotoViewModel.articleModel.refDate.isNullOrEmpty()) {
            val f = DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(context ?: return))
            f.format(baseWidePhotoViewModel.articleModel.createdAt)
        } else {
            baseWidePhotoViewModel.articleModel.refDate
        }

        binding.tvDate.text = dateString
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (activity != null) {
            val adView : View = requireActivity().findViewById(R.id.ad_container)

            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                adView.visibility = View.GONE
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                adView.visibility = View.VISIBLE
            }
        }
    }

    fun showDialogGuide(context: Context) {
        idolDialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        idolDialog!!.window!!.attributes = lpWindow
        idolDialog!!.window!!.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)

        idolDialog!!.setContentView(R.layout.dialog_banner_guide)
        idolDialog!!.setCanceledOnTouchOutside(true)
        idolDialog!!.setCancelable(true)

        val buttonOk : Button = idolDialog!!.findViewById(R.id.btn_ok)
        buttonOk.setOnClickListener {
            idolDialog!!.cancel()
        }

        val buttonDoNotShow : Button = idolDialog!!.findViewById(R.id.btn_do_not_show_again)
        buttonDoNotShow.setOnClickListener {
            Util.setPreference(context, keyDoNotShow, true)
            idolDialog!!.cancel()
        }

        try {
            idolDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            idolDialog!!.show()
        } catch (e: Exception) {
        }

    }
}