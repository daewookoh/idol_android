package net.ib.mn.onepick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.ActivityNewOnePickMainBinding
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.ThemepickRepositoryImpl
import net.ib.mn.core.model.NewPicksModel
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.model.ThemepickModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.preventUnwantedHorizontalScroll
import javax.inject.Inject

@AndroidEntryPoint
class ThemePickMainFragment : BaseFragment() {

    private lateinit var mThemepickRecyclerView: RecyclerView
    private lateinit var themePickAdapter: ThemePickAdapter

    private lateinit var mEmptyView: LinearLayoutCompat
    private lateinit var binding: ActivityNewOnePickMainBinding

    private var offset = 0
    private val limit = 30

    @Inject
    lateinit var themepickRepository: ThemepickRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionbar = (activity as AppCompatActivity).supportActionBar
        actionbar?.setTitle(R.string.themepick)

    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = ActivityNewOnePickMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mThemepickRecyclerView = binding.themePickRv
        themePickAdapter = ThemePickAdapter(mGlideRequestManager, getNewPick()) {
            /// TODO 개설 예정 작업
        }
        mEmptyView = binding.llEmptyWrapper

        mThemepickRecyclerView.adapter = themePickAdapter
        mThemepickRecyclerView.addOnScrollListener(onScrollListener)

        mThemepickRecyclerView.preventUnwantedHorizontalScroll()

        loadThemePick()
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager
            val lastVisibleItemPosition = (layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
            val totalItemCount = layoutManager.itemCount


            if (lastVisibleItemPosition >= totalItemCount - 1) {
                offset += limit
                loadThemePick()
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun loadThemePick() {
        MainScope().launch {
            themepickRepository.get(
                offset = offset,
                limit = limit,
                listener = { response ->
                    if(response.optBoolean("success")){
                        try{
                            val themePickJsonArray = response.getJSONArray("objects")
                            val themePickSize = themePickJsonArray.length()

                            binding.emptyViewDataLoad.visibility = View.GONE
                            if(themePickSize <= 0) {
                                binding.llEmptyWrapper.visibility = View.VISIBLE
                                binding.themePickRv.visibility = View.GONE
                                return@get
                            }

                            binding.llEmptyWrapper.visibility = View.GONE
                            binding.themePickRv.visibility = View.VISIBLE

                            val listType = object : TypeToken<List<ThemepickModel>>() {}.type
                            val themePickList = IdolGson.getInstance(false)
                                .fromJson<List<ThemepickModel>>(themePickJsonArray.toString(), listType)
                            themePickAdapter.submitList(themePickList.map { it.copy() })
                        }catch (e:Exception){
                            UtilK.showExceptionDialog(
                                requireContext(),
                                errorMsg = e.stackTraceToString(),
                            )
                        }
                    }
                },
                errorListener = { throwable ->
                    Logger.v("Themepick::익셉션입니다.")
                    UtilK.showExceptionDialog(
                        context = requireContext(),
                        throwable = throwable
                    )
                }
            )
        }
    }

    private fun getNewPick(): Boolean {
        val listType = object : TypeToken<NewPicksModel>() {}.type
        val newPicks: NewPicksModel? =
            IdolGson
                .getInstance()
                .fromJson(Util.getPreference(requireContext(), Const.PREF_NEW_PICKS), listType)

        return newPicks?.let {
            newPicks.themepick
        } ?: false
    }

    companion object{

        @JvmStatic
        fun createIntent(context: Context, flag: Int): Intent {
            val intent = Intent(context, ThemePickMainFragment::class.java)
            intent.flags = flag
            return intent
        }
    }
}