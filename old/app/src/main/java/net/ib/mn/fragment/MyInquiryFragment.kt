package net.ib.mn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.adapter.InquiryAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.InquiryRepositoryImpl
import net.ib.mn.databinding.FragmentMyinquirylistBinding
import net.ib.mn.model.InquiryModel
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import org.json.JSONException
import javax.inject.Inject

/**
 * Copyright 2022-12-7,수,16:2. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 나의 문의내역 리스트 보여주는 화면
 *
 **/

@AndroidEntryPoint
class MyInquiryFragment : BaseFragment() {
    @Inject
    lateinit var inquiryRepository: InquiryRepositoryImpl

    private lateinit var mInquiryAdapter : InquiryAdapter
    private var models = ArrayList<InquiryModel>()

    private var _binding: FragmentMyinquirylistBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentMyinquirylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mInquiryAdapter = InquiryAdapter(requireContext(), models)
        binding.rvInquiry.layoutManager = LinearLayoutManager(context)
        binding.rvInquiry.adapter = mInquiryAdapter
        loadResources()
    }

    private fun loadResources() {
        MainScope().launch {
            inquiryRepository.getInquiries(
                { response ->
                    if (response.optBoolean("success")) {
                        try {
                            val gson = IdolGson.getInstance(true)
                            val array = response.getJSONArray("objects")
                            models.clear()
                            for (i in 0 until array.length()) {
                                models.add(gson.fromJson(array.getJSONObject(i).toString(), InquiryModel::class.java))
                            }
                            models.reverse()
                            mInquiryAdapter.addAll(models)
                            if (models.size > 0) {
                                binding.empty.visibility = View.GONE
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        UtilK.handleCommonError(activity, response)
                    }                },
                { throwable ->
                    makeText(
                        activity, R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                }
            )
        }
    }
    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)

        if (isVisible) {
            loadResources()
        }
    }

    companion object {

        fun getInstance(): MyInquiryFragment {
            return MyInquiryFragment()
        }
    }
}