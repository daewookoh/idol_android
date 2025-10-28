package net.ib.mn.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.adapter.CharityCountAdapter
import net.ib.mn.adapter.CharityHistoryAdapter
import net.ib.mn.adapter.CharityHistoryAdapter.CharityListEmptyListener
import net.ib.mn.adapter.CharityHistoryAdapter.OnSearchListener
import net.ib.mn.addon.HeaderFooterListAdapter
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.FragmentCharityCountBinding
import net.ib.mn.fragment.BottomSheetFragment.Companion.newInstance
import net.ib.mn.model.CharityModel
import net.ib.mn.model.HallHistoryModel
import net.ib.mn.model.HallModel
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONArray
import org.json.JSONException
import java.util.Collections
import java.util.Locale
import java.util.Objects
import java.util.concurrent.ExecutionException
import javax.inject.Inject

@AndroidEntryPoint
class CharityCountFragment : BaseFragment(), View.OnClickListener,
    AdapterView.OnItemClickListener,
    CharityHistoryAdapter.OnClickListener, OnSearchListener {
    private var mHistoryAdapter: CharityHistoryAdapter? = null
    private var mCurrentTabIdx = 0
    private var mCurrentTabBtn: Button? = null

    protected var displayErrorHandler: Handler? = null

    private val historyArray: ArrayList<HallHistoryModel>? = null
    private var charityModels = ArrayList<CharityModel>()


    private var charityActivityIdx = 0
    private var mSheet: BottomSheetFragment? = null

    private var _binding: FragmentCharityCountBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var idolsRepository: IdolsRepository

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayErrorHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                val responseMsg = msg.obj as String
                makeText(activity, responseMsg, Toast.LENGTH_SHORT)
                    .show()
            }
        }
        //기부 천사, 요정, 이달의 기적 구별해서 보여주기 위함
        charityActivityIdx = requireArguments().getInt("charity")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCharityCountBinding.inflate(inflater, container, false)
        binding.flContainer.applySystemBarInsets()
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Util.log("CharityCountFragment created")
        showEmptyView()

        if (charityActivityIdx == 0) {
            binding.btnAngelCount.setText(R.string.charity_angel_count)
            charityType("A")
            loadAggResources(LOADER_ANGEL_COUNT)
        } else if (charityActivityIdx == 1) {
            binding.btnAngelCount.visibility = View.GONE
            binding.btnFairyCount.visibility = View.VISIBLE
            binding.btnFairyCount.setText(R.string.charity_fairy_count)
            charityType("F")
            loadAggResources(LOADER_FAIRY_COUNT)
        } else {
            binding.btnAngelCount.visibility = View.GONE
            binding.btnFairyCount.visibility = View.GONE
            binding.btnMiracleCount.visibility = View.VISIBLE
            binding.btnMiracleCount.setText(R.string.miracle_month_count)
            charityType("M")
            loadAggResources(LOADER_MIRACLE_COUNT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mCurrentTabIdx = 0
        binding.btnAngelCount.setOnClickListener(this)
        binding.btnFairyCount.setOnClickListener(this)
        binding.btnMiracleCount.setOnClickListener(this)
        binding.btnRecordHistory.setOnClickListener(this)
        binding.lvAngel.adapter = buildAdapter(LOADER_ANGEL_COUNT)
        binding.lvFairy.adapter = buildAdapter(LOADER_FAIRY_COUNT)
        binding.lvMiracle.adapter = buildAdapter(LOADER_MIRACLE_COUNT)

        binding.lvAngel.onItemClickListener = this
        binding.lvFairy.onItemClickListener = this
        binding.lvMiracle.onItemClickListener = this


        //        mHistoryAdapter = new CharityHistoryAdapter(requireActivity() , charityArray);
//        mHistoryRecyclerView.setAdapter(mHistoryAdapter);
        mCurrentTabBtn = binding.btnRecordHistory
        mCurrentTabBtn!!.isSelected = true
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_record_history -> if (mCurrentTabIdx != 0) {
                mCurrentTabBtn!!.isSelected = false
                mCurrentTabBtn = v as Button
                v.setSelected(true)
                binding.clAngel.visibility = View.GONE
                binding.clFairy.visibility = View.GONE
                binding.clMiracle.visibility = View.GONE
                binding.rlHistory.visibility = View.VISIBLE
                binding.btnAngelCount.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.text_dimmed
                    )
                )
                binding.btnFairyCount.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.text_dimmed
                    )
                )
                binding.btnMiracleCount.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(), R.color.text_dimmed
                    )
                )
                binding.btnRecordHistory.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(), R.color.main
                    )
                )
                mCurrentTabIdx = 0
            }

            R.id.btn_angel_count -> if (mCurrentTabIdx != 1) {
                mCurrentTabBtn!!.isSelected = false
                mCurrentTabBtn = v as Button
                v.setSelected(true)
                binding.clAngel.visibility = View.VISIBLE
                binding.clFairy.visibility = View.GONE
                binding.clMiracle.visibility = View.GONE
                binding.rlHistory.visibility = View.GONE
                binding.btnAngelCount.setTextColor(ContextCompat.getColor(requireActivity(), R.color.main))
                binding.btnFairyCount.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.text_dimmed
                    )
                )
                binding.btnMiracleCount.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(), R.color.text_dimmed
                    )
                )
                binding.btnRecordHistory.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(), R.color.text_dimmed
                    )
                )
                mCurrentTabIdx = 1
            }

            R.id.btn_fairy_count -> if (mCurrentTabIdx != 2) {
                mCurrentTabBtn!!.isSelected = false
                mCurrentTabBtn = v as Button
                v.setSelected(true)
                binding.clAngel.visibility = View.GONE
                binding.clFairy.visibility = View.VISIBLE
                binding.clMiracle.visibility = View.GONE
                binding.rlHistory.visibility = View.GONE
                binding.btnAngelCount.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.text_dimmed
                    )
                )
                binding.btnFairyCount.setTextColor(ContextCompat.getColor(requireActivity(), R.color.main))
                binding.btnMiracleCount.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(), R.color.text_dimmed
                    )
                )
                binding.btnRecordHistory.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(), R.color.text_dimmed
                    )
                )
                mCurrentTabIdx = 2
            }

            R.id.btn_miracle_count -> if (mCurrentTabIdx != 3) {
                mCurrentTabBtn!!.isSelected = false
                mCurrentTabBtn = v as Button
                v.setSelected(true)
                binding.clAngel.visibility = View.GONE
                binding.clFairy.visibility = View.GONE
                binding.clMiracle.visibility = View.VISIBLE
                binding.rlHistory.visibility = View.GONE
                binding.btnAngelCount.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.text_dimmed
                    )
                )
                binding.btnFairyCount.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.text_dimmed
                    )
                )
                binding.btnMiracleCount.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(), R.color.main
                    )
                )
                binding.btnRecordHistory.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(), R.color.text_dimmed
                    )
                )
                mCurrentTabIdx = 3
            }
        }
    }

    protected fun loadAggResources(id: Int) {
        val items: MutableList<HallModel> = ArrayList()

        lifecycleScope.launch {
            idolsRepository.getCharityCount().collect {
                val obj = it.data ?: return@collect

                try {
                    val array: JSONArray = if (id == LOADER_ANGEL_COUNT) {
                        obj["angel"] as JSONArray
                    } else if (id == LOADER_FAIRY_COUNT) {
                        obj["fairy"] as JSONArray
                    } else if (id == LOADER_MIRACLE_COUNT) {
                        obj["miracle"] as JSONArray
                    } else {
                        return@collect
                    }
                    val gson = instance
                    for (i in 0 until array.length()) {
                        items.add(
                            gson.fromJson(
                                array.getJSONObject(i).toString(),
                                HallModel::class.java
                            )
                        )
                    }
                } catch (_: JSONException) {
                } catch (_: InterruptedException) {
                } catch (_: ExecutionException) {
                }
                val temp: List<HallModel?> = ArrayList(items)
                if (BuildConfig.CELEB) {
                    Collections.sort(temp, Comparator.comparingLong { o: HallModel -> o.heart }
                        .reversed())
                }
                for (i in temp.indices) {
                    val item = temp[i]
                    // 동점자 처리
                    if (i > 0 && temp[i - 1]!!
                            .count == item!!.count
                    ) item?.rank = temp[i - 1]!!.rank
                    else item!!.rank = i
                }

                Util.closeProgress()
                hideEmptyView()

                val adapter = getAdapter(id)!!.wrappedAdapter as CharityCountAdapter
                if (adapter.items.isEmpty()) {
                    applyAggItems(id, items)
                } else {
                    showError(getString(R.string.failed_to_load), it.message)
                }
            }
        }
    }

    private fun applyAggItems(id: Int, items: List<HallModel>) {
        binding.llHistoryList.visibility = View.GONE
        binding.llHistoryList.removeAllViews()


        val adapter = getAdapter(id)
            ?.getWrappedAdapter() as? CharityCountAdapter ?: return
        adapter.clear()
        adapter.addAll(items)

        adapter.notifyDataSetChanged()
        if (items.size == 0) {
            showEmptyView(id)
        } else {
            hideEmptyView(id)
            if (id == LOADER_ANGEL_COUNT) {
                binding.lvAngel.setSelection(0)
            } else if (id == LOADER_FAIRY_COUNT) {
                binding.lvFairy.setSelection(0)
            } else if (id == LOADER_MIRACLE_COUNT) {
                binding.lvMiracle.setSelection(0)
            }
        }
    }

    protected fun showEmptyView(id: Int) {
        if (id == LOADER_ANGEL_COUNT) {
            binding.tvAngelEmpty.visibility = View.VISIBLE
        } else if (id == LOADER_FAIRY_COUNT) {
            binding.tvFairyEmpty.visibility = View.VISIBLE
        } else if (id == LOADER_MIRACLE_COUNT) {
            binding.tvMiracleEmpty.visibility = View.VISIBLE
        }
    }

    protected fun hideEmptyView(id: Int) {
        if (id == LOADER_ANGEL_COUNT) {
            binding.tvAngelEmpty.visibility = View.GONE
        } else if (id == LOADER_FAIRY_COUNT) {
            binding.tvFairyEmpty.visibility = View.GONE
        } else if (id == LOADER_MIRACLE_COUNT) {
            binding.tvMiracleEmpty.visibility = View.GONE
        }
    }

    private fun showError(title: String, text: String?) {
        Util.showDefaultIdolDialogWithBtn1(
            activity,
            title,
            text
        ) { v: View? -> Util.closeIdolDialog() }
    }

    protected fun buildAdapter(id: Int): HeaderFooterListAdapter? {
        var adapter: HeaderFooterListAdapter? = null
        val wrappedAdapter = CharityCountAdapter(requireActivity(), mGlideRequestManager)
        if (id == LOADER_ANGEL_COUNT) {
            adapter = HeaderFooterListAdapter(
                binding.lvAngel,
                CharityCountAdapter(requireActivity(), mGlideRequestManager)
            )
        } else if (id == LOADER_FAIRY_COUNT) {
            adapter = HeaderFooterListAdapter(
                binding.lvFairy,
                CharityCountAdapter(requireActivity(), mGlideRequestManager)
            )
        } else if (id == LOADER_MIRACLE_COUNT) {
            adapter = HeaderFooterListAdapter(
                binding.lvMiracle,
                CharityCountAdapter(requireActivity(), mGlideRequestManager)
            )
        }
        return adapter
    }

    protected fun getAdapter(id: Int): HeaderFooterListAdapter? {
        if (id == LOADER_ANGEL_COUNT) {
            return binding.lvAngel
                .adapter as HeaderFooterListAdapter
        } else if (id == LOADER_FAIRY_COUNT) {
            return binding.lvFairy
                .adapter as HeaderFooterListAdapter
        } else if (id == LOADER_MIRACLE_COUNT) {
            return binding.lvMiracle
                .adapter as HeaderFooterListAdapter
        }
        return null
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)
        if (isVisible) {
            binding.llHistoryList.visibility = View.GONE
            binding.llHistoryList.removeAllViews()
        }
    }

    override fun onItemClick(
        parent: AdapterView<*>?, view: View, position: Int,
        id: Long
    ) {
    }

    fun handleOnBackPressed(): Boolean {
        if (binding.llHistoryList.isVisible) {
//            mBtnHistory.setVisibility(View.VISIBLE);
            binding.llHistoryList.visibility = View.GONE
            binding.llHistoryList.removeAllViews()
            return true
        } else {
            return false
        }
    }

    private fun showEmptyView() {
        binding.emptyView.visibility = View.VISIBLE
        binding.rlHalloffame.visibility = View.GONE
    }

    private fun hideEmptyView() {
        binding.emptyView.visibility = View.GONE
        binding.rlHalloffame.visibility = View.VISIBLE
    }

    //아이템을 클릭헀을 때 바텀시트 띄우줄 때 사용
    override fun onItemClicked(charityModel: CharityModel?) {
        checkNotNull(charityModel)
        mSheet = newInstance(
            BottomSheetFragment.FLAG_HISTORY,
            charityModel.imageUrl,
            charityModel.linkUrl
        )
        val tag = "filter"
        val oldFrag =
            Objects.requireNonNull(requireActivity()).supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            mSheet!!.show(requireActivity().supportFragmentManager, tag)
        }
    }

    //Search bar 검색버튼 눌렀을 경우.
    override fun btnSearchClicked(view: View, etSearch: AppCompatEditText, clickCheck: Boolean) {
        if (!clickCheck) {  //돋보기 클릭했을 때(돋보기 그림 or 키보드 돋보기)
            etSearch.isCursorVisible = false
            Util.hideSoftKeyboard(context, etSearch)
            mHistoryAdapter!!.getSearchedKeyword(
                Objects.requireNonNull(etSearch.text).toString().trim { it <= ' ' }
                    .uppercase(Locale.getDefault()))
        } else {  //검색하기 위해 클릭했을 때
            etSearch.requestFocus()
            etSearch.isCursorVisible = true
            Util.showSoftKeyboard(context, etSearch)
        }
    }

    private fun charityType(type: String) {
        lifecycleScope.launch {
            idolsRepository.getCharityHistory(type, systemLanguage(),
                { response ->
                    if (response.optBoolean("success")) {
                        try {
                            val gson = getInstance(false)
                            val charityType =
                                object : TypeToken<ArrayList<CharityModel?>?>() {}.type
                            charityModels = gson.fromJson(
                                response.getJSONArray("objects").toString(),
                                charityType
                            )

                            if (charityModels.size > 0) {
                                mHistoryAdapter = CharityHistoryAdapter(
                                    requireActivity(),
                                    this@CharityCountFragment,
                                    this@CharityCountFragment,
                                    mGlideRequestManager,
                                    charityModels
                                )
                                binding.rcyHistory.adapter = mHistoryAdapter

                                //어댑터 set 한 후 부르는 것으로, 리스트가 비어있는지 없는지에 따라 처리
                                mHistoryAdapter!!.setCharityEmptyListener(object :
                                    CharityListEmptyListener {
                                    override fun getEmptyOrNot(charityEmpty: Boolean) {
                                        if (charityEmpty) {
                                            binding.tvNoSearch.visibility = View.VISIBLE
                                        } else {
                                            binding.tvNoSearch.visibility = View.GONE
                                        }
                                    }
                                })
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                },
                {})
        }
    }

    private fun systemLanguage(): String {
        var locale = Util.getSystemLanguage(activity)

        locale = if (locale.equals("ko_KR", ignoreCase = true)) {
            "ko"
        } else if (locale.equals("zh_CN", ignoreCase = true)) {
            "zh_cn"
        } else if (locale.equals("zh_TW", ignoreCase = true)) {
            "zh_tw"
        } else if (locale.equals("ja_JP", ignoreCase = true)) {
            "ja"
        } else {
            "en"
        }
        return locale
    }

    companion object {
        protected const val LOADER_ANGEL_COUNT: Int = 0
        protected const val LOADER_FAIRY_COUNT: Int = 1
        protected const val LOADER_MIRACLE_COUNT: Int = 2
        @JvmStatic
        fun newInstance(charity: Int): CharityCountFragment {
            val f = CharityCountFragment()
            val args = Bundle()
            args.putInt("charity", charity)
            f.arguments = args
            return f
        }
    }
}
