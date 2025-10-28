package net.ib.mn.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.adapter.HighestVotesAdapter
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.core.domain.usecase.TrendsTop100UseCase
import net.ib.mn.fragment.BottomSheetFragment.Companion.newInstance
import net.ib.mn.model.HallModel
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import org.json.JSONObject
import java.util.Collections
import java.util.Objects
import javax.inject.Inject

@AndroidEntryPoint
class HighestVotesFragment : BaseFragment() {
    private var mListView: ListView? = null
    private var mEmptyView: TextView? = null
    private var mLoadingView: TextView? = null
    private var mRankingView: ConstraintLayout? = null
    private var mTypeFilterView: ConstraintLayout? = null
    private var tvTypeFilter: AppCompatTextView? = null
    protected var mSheet: BottomSheetFragment? = null

    protected var displayErrorHandler: Handler? = null

    private var mAdapter: HighestVotesAdapter? = null
    private val mModels = HashMap<String, ArrayList<HallModel>>()

    @Inject
    lateinit var trendsTop100UseCase: TrendsTop100UseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mGlideRequestManager = Glide.with(this)
        displayErrorHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                val responseMsg = msg.obj as String
                makeText(activity, responseMsg, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        mAdapter = HighestVotesAdapter(requireActivity(), mGlideRequestManager)

        loadResources()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_highest_votes, container, false)

        mListView = view.findViewById(R.id.list)
        mEmptyView = view.findViewById(R.id.empty)
        mLoadingView = view.findViewById(R.id.loading)
        mRankingView = view.findViewById(R.id.ranking_view)
        mTypeFilterView = view.findViewById(R.id.type_filter)
        tvTypeFilter = view.findViewById(R.id.tv_type_filter)

        mSheet = newInstance(BottomSheetFragment.FLAG_HIGHEST_VOTE_TYPE_FILTER)
        mTypeFilterView?.setOnClickListener(View.OnClickListener { v: View? ->
            mSheet!!.show(
                requireActivity().supportFragmentManager, mSheet!!.tag
            )
        })
        tvTypeFilter?.setText(getString(R.string.filter_actor) + " " + getString(R.string.top_100))

        return view
    }

    fun filterByType(name: String, typeName: String) {
        mAdapter!!.clear()
        mAdapter!!.addAll(mModels[typeName])
        mAdapter!!.notifyDataSetChanged()
        tvTypeFilter!!.text = name + " " + getString(R.string.top_100)
        if (Objects.requireNonNull(mModels[typeName])?.isEmpty() ?: false) {
            showEmptyView()
        } else {
            hideEmptyView()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    protected fun loadResources() {
        MainScope().launch {
            trendsTop100UseCase(true).collectLatest { response ->
                try {
                    val obj = response
                    val typeNames = Util.getTypeListTypeNameArray(activity)
                    for (i in typeNames.indices) {
                        val items = ArrayList<HallModel>()
                        val array = (obj[typeNames[i]] as JSONObject).getJSONArray("objects")
                        val gson = getInstance(true)
                        for (j in 0 until array.length()) {
                            items.add(
                                gson.fromJson(
                                    array.getJSONObject(j).toString(),
                                    HallModel::class.java
                                )
                            )
                        }

                        val temp = ArrayList(items)
                        Collections.sort(
                            temp,
                            Comparator.comparingLong { obj: HallModel -> obj.heart }
                                .reversed())
                        for (j in temp.indices) {
                            val item = temp[j]
                            // 동점자 처리
                            if (j > 0 && temp[j - 1].heart == item.heart) item.rank =
                                temp[j - 1].rank
                            else item.rank = j
                        }
                        mModels[typeNames[i]] = temp
                    }
                    mAdapter!!.addAll(mModels["type_actor"])
                    mListView!!.adapter = mAdapter
                    mAdapter!!.notifyDataSetChanged()
                    mLoadingView!!.visibility = View.GONE
                    if (mAdapter!!.count == 0) showEmptyView()
                    else hideEmptyView()
                } catch (e: Exception) {
                    e.stackTrace
                }
            }
        }
    }

    private fun showError(title: String, text: String) {
        Util.showDefaultIdolDialogWithBtn1(
            activity,
            title,
            text
        ) { v: View? -> Util.closeIdolDialog() }
    }

    private fun showEmptyView() {
        if (BuildConfig.CELEB) {
            mEmptyView!!.setText(R.string.actor_label_stats_highest_votes_no_data)
        } else {
            mEmptyView!!.setText(R.string.label_stats_highest_votes_no_data)
        }
        mEmptyView!!.visibility = View.VISIBLE
        mLoadingView!!.visibility = View.GONE
        mListView!!.visibility = View.GONE
    }

    private fun hideEmptyView() {
        mEmptyView!!.visibility = View.GONE
        mLoadingView!!.visibility = View.GONE
        mRankingView!!.visibility = View.VISIBLE
        mListView!!.visibility = View.VISIBLE
    }
}
