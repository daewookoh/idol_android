package net.ib.mn.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import net.ib.mn.R
import net.ib.mn.adapter.EmoticonAdapter
import net.ib.mn.databinding.FragmentEmoticonBinding
import net.ib.mn.model.EmoticonDetailModel
import com.bumptech.glide.Glide
import net.ib.mn.utils.Logger


class EmoticonFragment() : Fragment(), EmoticonAdapter.onItemClickListener {

    private lateinit var emoList: ArrayList<EmoticonDetailModel>
    private lateinit var binding: FragmentEmoticonBinding

    interface EmotiConClickListener{
        fun emoticonClick(model: EmoticonDetailModel, view: View?, position: Int)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentEmoticonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emoList = arguments?.getSerializable(PARAM_EMOTICON_LIST) as ArrayList<EmoticonDetailModel>

        Logger.v("list:: $emoList")

        binding.rvEmoticon.apply {
            //이모티콘 orderby로 정렬해줍니다.
            adapter = EmoticonAdapter(requireActivity() ,Glide.with(requireActivity()), emoList.sortedBy { it.order }){ model, view, position ->
                onItemClick(model, view, position)
            }
            layoutManager = GridLayoutManager(requireActivity(), 4, LinearLayoutManager.VERTICAL, false)
        }

    }

    override fun onItemClick(model: EmoticonDetailModel, view: View, position: Int) {
        emoticonClick(model, view, position)
    }

    companion object {

        const val PARAM_EMOTICON_LIST = "emoticon_list"
        const val PARAM_EMOTICON_CLICK = "emoticon_click"

        private lateinit var emoticonClick: (EmoticonDetailModel, View, Int) -> Unit

        @JvmStatic
        fun newInstance(emoList: ArrayList<EmoticonDetailModel>, emoticonClick: (EmoticonDetailModel, View, Int) -> Unit): EmoticonFragment {
            val args = Bundle().apply {
                putSerializable(PARAM_EMOTICON_LIST, emoList)
            }
            this@Companion.emoticonClick = emoticonClick

            val fragment = EmoticonFragment()
            fragment.arguments = args
            return fragment
        }
    }

}