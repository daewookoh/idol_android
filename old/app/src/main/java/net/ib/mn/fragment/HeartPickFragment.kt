package net.ib.mn.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.activity.HeartPickActivity
import net.ib.mn.adapter.HeartPickMainAdapter

import net.ib.mn.databinding.FragmentHeartPickBinding
import net.ib.mn.model.HeartPickModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.EventBus
import net.ib.mn.utils.Logger
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.HeartPickViewModel

@AndroidEntryPoint
class HeartPickFragment : BaseFragment(),
    HeartPickMainAdapter.OnItemClickListener,
    OnScrollToTopListener {

    private lateinit var binding: FragmentHeartPickBinding
    private val heartPickViewModel: HeartPickViewModel by viewModels()
    private lateinit var heartPickMainAdapter: HeartPickMainAdapter
    private var isNotify = false
    private var isLoading = false

    private var disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHeartPickBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSet()
        getDataFromVM()
        setAdapter()
        setClickEvent()

        heartPickViewModel.getHeartPick(context)

        lifecycleScope.launch {
            EventBus.receiveEvent<Boolean>(Const.BROADCAST_VOTE).collect { result ->
                if(result){
                    isNotify = true
                }
            }
        }
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)
        Logger.v("asdasdasd -> "+ isVisible)
        if(isVisible && isNotify) {
            lifecycleScope.launch {
                delay(3000)
                heartPickViewModel.getHeartPick(context)
                isNotify = false
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(BuildConfig.CELEB && !hidden && isNotify) {
            lifecycleScope.launch {
                delay(3000)
                heartPickViewModel.getHeartPick(context)
                isNotify = false
            }
        }
    }

    private fun getDataFromVM() {
        heartPickViewModel.registerActivityResult(requireActivity(), this)

        heartPickViewModel.getHeartPickList.observe(
            viewLifecycleOwner,
            SingleEventObserver { heartPickList ->
                binding.tvDataLoad.visibility = View.GONE
                val copyHeartPickList = heartPickList.map { it.copy() }.toMutableList()
                updateHeartPickRanks(copyHeartPickList)
//                addLoadingModelIfNeeded(copyHeartPickList, heartPickViewModel)
                heartPickMainAdapter.submitList(copyHeartPickList)
                isLoading = false
            })

        heartPickViewModel.getHeartPick.observe(
            viewLifecycleOwner,
            SingleEventObserver {heartPickModel ->
                heartPickMainAdapter.setHeartPickModel(heartPickModel)
            }
        )
    }
    private fun setAdapter() {
        heartPickMainAdapter = HeartPickMainAdapter(heartPickViewModel.getNewPick(), this, lifecycleScope)
        binding.rvHeartPick.apply {
            adapter = heartPickMainAdapter
            val layoutManager = LinearLayoutManager(context)
            layoutManager.initialPrefetchItemCount = 3
            this.layoutManager = layoutManager

            setItemViewCacheSize(5)
            setHasFixedSize(true)
        }
    }

    override fun onItemClick(id: Int) {
        context?.let { HeartPickActivity.createIntent(it, id) }?.let {
            heartPickViewModel.startActivityResultLauncher.launch(
                it
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        if(::binding.isInitialized) {
            binding.slRefresh.cancel()
        }
    }

    private fun setClickEvent() {
        binding.slRefresh.setCustomRefreshListener {
            heartPickViewModel.getHeartPick(
                context,
                isRefresh = true,
            )
        }

        binding.rvHeartPick.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val lastVisiblePosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                val itemTotalCount = recyclerView.adapter!!.itemCount - 1

                heartPickViewModel.recyclerviewScrollState =
                    recyclerView.layoutManager?.onSaveInstanceState()

                // 스크롤이 끝에 도달했는지 확인후  api를 요청해서 다음 페이지를 받아온다.
                if (!isLoading &&
                    !recyclerView.canScrollVertically(1) &&
                    lastVisiblePosition == itemTotalCount &&
                    !heartPickViewModel.getNextResourceUrl().isNullOrEmpty()
                ) {
                    isLoading = true
                    heartPickViewModel.getHeartPick(context)
                }

                if (heartPickViewModel.getNextResourceUrl().isNullOrEmpty()) {
//                    heartPickMainAdapter.deleteLoading()
                }
            }
        })
    }

    private fun initSet() {
        binding.rvHeartPick.layoutManager?.onRestoreInstanceState(heartPickViewModel.recyclerviewScrollState)
    }

    private fun updateHeartPickRanks(copyHeartPickList: MutableList<HeartPickModel>) {
        for (i in 0 until copyHeartPickList.size) {
            val heartPickIdols = copyHeartPickList[i].heartPickIdols
            if (heartPickIdols != null) {
                for (j in heartPickIdols.indices) {
                    heartPickIdols[j].rank = j + 1
                    if (j > 0 && heartPickIdols[j - 1].vote == heartPickIdols[j].vote) {
                        heartPickIdols[j].rank = heartPickIdols[j - 1].rank
                    }
                }
            }
        }
    }

    private fun addLoadingModelIfNeeded(
        copyHeartPickList: MutableList<HeartPickModel>,
        heartPickViewModel: HeartPickViewModel
    ) {
        if (heartPickViewModel.getNextResourceUrl().isNullOrEmpty()) {
            return
        }

        val loadingModel = HeartPickModel().apply {
            id = HeartPickMainAdapter.LOADING_ITEM
            isLoading = true
        }
        copyHeartPickList.add(loadingModel)
    }

    override fun onScrollToTop() {
        if(!::binding.isInitialized) return
        binding.rvHeartPick.scrollToPosition(0)
    }
}