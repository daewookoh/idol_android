package net.ib.mn.dialog

import android.R
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import net.ib.mn.BuildConfig
import net.ib.mn.model.IdolModel
import net.ib.mn.model.QuizCategoryModel

/**
 * 아이돌 퀴즈에서 아이돌 group 선택 DialogFragment
 *
 * @author Daeho Kim
 * @since 6.6.9
 */
class QuizIdolOptionDialogFragment : BaseDialogFragment() {
    private var modelList: ArrayList<IdolModel>? = ArrayList()
    var quizTypeList: ArrayList<QuizCategoryModel>? = ArrayList()
    private var callbackListener: onItemClickCallbackListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(net.ib.mn.R.layout.dialog_idol, container, false)
        if (BuildConfig.CELEB) {
            (view.findViewById<View>(net.ib.mn.R.id.title) as TextView).setText(net.ib.mn.R.string.schedule_category)
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (activity is onItemClickCallbackListener) callbackListener =
            activity as onItemClickCallbackListener?
        modelList = requireArguments().getSerializable(PARAM_IDOL_OPTION) as ArrayList<IdolModel>?
        quizTypeList =
            requireArguments().getSerializable(PARAM_TYPE_OPTION) as ArrayList<QuizCategoryModel>?
        setScreenSize()
    }

    override fun onDetach() {
        callbackListener = null
        super.onDetach()
    }

    private fun setScreenSize() {
        val dialog = dialog

        val display = requireActivity().windowManager.defaultDisplay
        val screenSize = Point()
        display.getSize(screenSize)
        val width = (screenSize.x * 0.8).toInt()
        val height = (screenSize.y * 0.7).toInt()

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        dialog!!.window!!.attributes = lpWindow

        if (modelList!!.size > 15) {
            dialog.window!!.setLayout(width, height)
        } else {
            dialog.window!!.setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)

        val title = dialog.findViewById<View>(net.ib.mn.R.id.favorite)
        title.visibility = View.GONE

        val listView = dialog.findViewById<ListView>(net.ib.mn.R.id.listView)
        val nameAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            requireActivity(), net.ib.mn.R.layout.language_item
        )
        if (BuildConfig.CELEB) {
            for ((name) in quizTypeList!!) {
                nameAdapter.add(name)
            }
        } else {
            nameAdapter.add(getString(net.ib.mn.R.string.quiz_idol_all))
            for (idol in modelList!!) {
                nameAdapter.add(idol.getName(activity))
            }
        }
        listView.adapter = nameAdapter

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                if (!nameAdapter.isEmpty) {
                    selectedIdolName =
                        nameAdapter.getItem(position).toString()
                    if (position == 0) {
                        selectedType = null
                        selectedIdolId = 0
                    } else {
                        if (BuildConfig.CELEB) {
                            selectedType =
                                quizTypeList!![position].type
                        } else {
                            selectedIdolId =
                                modelList!![position - 1].getId()
                        }
                    }
                    if (callbackListener != null) callbackListener!!.onItemClickCallback()
                    dialog.dismiss()
                }
            }

        dialog.window!!.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
    }

    interface onItemClickCallbackListener {
        fun onItemClickCallback()
    }

    companion object {
        private val TAG: String = QuizIdolOptionDialogFragment::class.java.simpleName
        private const val PARAM_IDOL_OPTION = "idol_option"
        private const val PARAM_TYPE_OPTION = "type_option"

        var selectedIdolName: String? = null
            private set
        var selectedIdolId: Int = 0
            private set
        var selectedType: String? = null
            private set

        fun getInstance(
            modelList: ArrayList<IdolModel>?,
            quizCategoryList: ArrayList<QuizCategoryModel>?
        ): QuizIdolOptionDialogFragment {
            val fragment = QuizIdolOptionDialogFragment()
            val args = Bundle()
            args.putSerializable(PARAM_IDOL_OPTION, modelList)
            args.putSerializable(PARAM_TYPE_OPTION, quizCategoryList)
            fragment.arguments = args
            return fragment
        }
    }
}
