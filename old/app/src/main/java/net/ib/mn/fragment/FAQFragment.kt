package net.ib.mn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.adapter.FAQAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.databinding.FragmentFaqBinding
import net.ib.mn.model.FAQModel
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.UtilK
import org.json.JSONArray
import org.json.JSONException
import javax.inject.Inject

/**
 * Copyright 2022-12-7,수,16:5. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 자주 묻는 질문 리스트 보여주는 화면
 *
 **/

@AndroidEntryPoint
class FAQFragment : BaseFragment() {
    private lateinit var mFaqAdapter : FAQAdapter
    private var models = ArrayList<FAQModel>()

    @Inject
    lateinit var miscRepository: MiscRepository

    private var _binding: FragmentFaqBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentFaqBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFaqAdapter = FAQAdapter(requireContext(), models)
        binding.rvFaq.layoutManager = LinearLayoutManager(context)
        binding.rvFaq.adapter = mFaqAdapter
        loadResources()
    }

    private fun loadResources() {
        lifecycleScope.launch {
            miscRepository.getFAQs(
                { response ->
                    if (response.optBoolean("success")) {
                        val array: JSONArray
                        try {
                            array = response.getJSONArray("objects")
                            val gson = IdolGson.getInstance()
                            models.clear()
                            for (i in 0 until array.length()) {
                                models.add(gson.fromJson(array.getJSONObject(i).toString(), FAQModel::class.java))
                            }
                            mFaqAdapter.addAll(models)
                            if (models.size == 0) {
                                binding.empty.visibility = View.VISIBLE
                            }else{
                                binding.empty.visibility = View.GONE
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        UtilK.handleCommonError(activity, response)
                    }
                }, {
                    makeText(activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
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
        fun getInstance(): FAQFragment {
            return FAQFragment()
        }
    }
}