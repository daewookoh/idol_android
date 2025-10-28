package net.ib.mn.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.adapter.CharityCountAdapter
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.data.repository.HofsRepository
import net.ib.mn.databinding.FragmentHighestVotesBinding
import net.ib.mn.fragment.BottomSheetFragment.Companion.newInstance
import net.ib.mn.model.HallModel
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class Top1CountFragment : BaseFragment() {
    protected var mSheet: BottomSheetFragment? = null
    protected var displayErrorHandler: Handler? = null

    private var mAdapter: CharityCountAdapter? = null
    private val mModels = HashMap<String, ArrayList<HallModel>>()

    lateinit var binding: FragmentHighestVotesBinding
    @Inject
    lateinit var hofsRepository: HofsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGlideRequestManager = Glide.with(this)
        displayErrorHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                val responseMsg = msg.obj as String
                makeText(activity, responseMsg, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        mAdapter = CharityCountAdapter(requireActivity(), mGlideRequestManager!!)
        loadResources()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHighestVotesBinding.inflate(inflater, container, false)

        mSheet = newInstance(BottomSheetFragment.FLAG_TOP1_TYPE_FILTER)
        binding.typeFilter.setOnClickListener(View.OnClickListener { v: View? ->
            mSheet!!.show(
                requireActivity().supportFragmentManager, mSheet!!.tag
            )
        })
        binding.tvTypeFilter.setText(getString(R.string.filter_actor) + " " + getString(R.string.top_100))

        return binding.root
    }

    fun filterByType(name: String, typeName: String, typeListModel: TypeListModel) {
        mAdapter!!.clear()
        mAdapter!!.addAll(mModels[typeName])
        mAdapter!!.notifyDataSetChanged()
        //null이면 종합이란뜻이므로 top100 스트링 넣어줌.
        val filterText =
            if (typeListModel.type == null) getString(R.string.top_100) else name + " " + getString(
                R.string.top_100
            )
        binding.tvTypeFilter.text = filterText
        if (mModels[typeName]?.isEmpty() != false) {
            binding.list.visibility = View.GONE
        } else {
            binding.list.visibility = View.VISIBLE
        }


        if (mAdapter!!.count == 0) {
            if (BuildConfig.CELEB) {
                binding.empty.setText(R.string.actor_label_stats_1st_place_no_data)
            } else {
                binding.empty.setText(R.string.label_stats_1st_place_no_data)
            }

            binding.empty.visibility = View.VISIBLE
        } else {
            binding.empty.visibility = View.GONE
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    protected fun loadResources() {
        lifecycleScope.launch {
            hofsRepository.getTop1Count().collect {
                val obj = it.data ?: return@collect
                val typeNames = Util.getTypeListTypeNameArray(
                    activity
                )
                for (i in typeNames.indices) {
                    val items = ArrayList<HallModel>()
                    val array = (obj[typeNames[i]] as JSONObject).getJSONArray("objects")
                    val gson = instance
                    for (j in 0..<array.length()) {
                        items.add(
                            gson.fromJson(
                                array.getJSONObject(j).toString(),
                                HallModel::class.java
                            )
                        )
                    }

                    val temp = ArrayList(items)
                    temp.sortWith { lhs: HallModel, rhs: HallModel -> rhs.count - lhs.count }
                    for (j in temp.indices) {
                        val item = temp[j]
                        // 동점자 처리
                        if (j > 0 && temp[j - 1].count == item.count) item.rank =
                            temp[j - 1].rank
                        else item.rank = j
                    }
                    mModels[typeNames[i]] = temp
                }
                mAdapter!!.addAll(mModels["type_actor"])
                binding.list.adapter = mAdapter
                mAdapter!!.notifyDataSetChanged()
                binding.loading.visibility = View.GONE
                if (mAdapter!!.count == 0) showEmptyView()
                else hideEmptyView()
            }
        }
    }

    private fun showEmptyView() {
        if (BuildConfig.CELEB) {
            binding.empty.setText(R.string.actor_label_stats_1st_place_no_data)
        } else {
            binding.empty.setText(R.string.label_stats_1st_place_no_data)
        }
        binding.empty.visibility = View.VISIBLE
        binding.rankingView.visibility = View.GONE
        binding.loading.visibility = View.GONE
        binding.list.visibility = View.GONE
    }

    private fun hideEmptyView() {
        binding.empty.visibility = View.GONE
        binding.loading.visibility = View.GONE
        binding.rankingView.visibility = View.VISIBLE
        binding.list.visibility = View.VISIBLE
    }
}
