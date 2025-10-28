package net.ib.mn.dialog

import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import net.ib.mn.R
import net.ib.mn.databinding.DialogSurpriseDiaBinding
import net.ib.mn.utils.Util
import java.text.NumberFormat
import java.util.*

class SurpriseDiaDialogFragment : BaseDialogFragment() {
    private lateinit var binding: DialogSurpriseDiaBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogSurpriseDiaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSet()
        clickEvent()
    }

    //초기 세팅
    private fun initSet() {
        var serverMsg : String = arguments?.getString(SURPRISE_DIA_TEXT) ?: ""    //서버에서 온 팝업 스트링
        val diaCount : Int? = arguments?.getInt(SURPRISE_DIA_COUNT)  //서버에서 온 다이아 개수
        val localeDiaCount : String = NumberFormat.getNumberInstance(Locale.getDefault()).format(diaCount) //서버에서 온 다이아 개수 언어에 맞게 변경

        if(serverMsg.contains(diaCount.toString())){
            serverMsg = serverMsg.replace(diaCount.toString(), localeDiaCount)
        }
        val count: String = String.format(serverMsg, localeDiaCount)
        var spannable: SpannableString? = SpannableString(count)
        spannable = Util.getColorText(spannable, localeDiaCount, ContextCompat.getColor(requireContext(), R.color.main))
        binding.tvSurpriseDia.text = spannable ?: ""
    }


    //클릭 이벤트 모음
    private fun clickEvent() {

        //확인 버튼 클릭
        binding.btnConfirm.setOnClickListener {
            this.dismiss()
        }
    }

    companion object {
        private const val SURPRISE_DIA_TEXT = "surprise_dia_text"
        private const val SURPRISE_DIA_COUNT = "surprise_dia_count"

        fun getInstance(responseMsg : String, diaCount : Int): SurpriseDiaDialogFragment {
            val args = Bundle()
            val fragment = SurpriseDiaDialogFragment()
            args.putString(SURPRISE_DIA_TEXT, responseMsg)
            args.putInt(SURPRISE_DIA_COUNT, diaCount)
            fragment.arguments = args
            return fragment
        }
    }

}