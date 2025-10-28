package net.ib.mn.fragment

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.account.IdolAccount.Companion.sAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.model.MainChartModel
import net.ib.mn.core.data.repository.FavoritesRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.domain.usecase.GetAllIdolsUseCase
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.model.FavoriteModel
import net.ib.mn.model.RankingModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Const
import net.ib.mn.utils.Const.NON_FAVORITE_IDOL_ID
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsetsAndRequest
import net.ib.mn.viewmodel.MainViewModel
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import javax.inject.Inject

@AndroidEntryPoint
class FavoritIdolFragment : FavoriteIdolBaseFragment(), OnScrollToTopListener {
    var isMission: Boolean = false
    var isFromVotingCertificate: Boolean = false
    private var mainChartCode: MainChartModel? = null
    var isFirst: Boolean = true

    private var mainViewModel: MainViewModel? = null
    private var job: Job? = null

    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase

    @Inject
    lateinit var sharedAppState: SharedAppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            isMission = savedInstanceState.getBoolean(PARAM_IS_MISSION, false)
            mainChartCode = savedInstanceState.getParcelable(Const.CHART_CODE_FOR_CERTIFICATE)
            isFromVotingCertificate = savedInstanceState.getBoolean(PARAM_IS_FROM_VOTING_CERTIFICATE, false)
        } else {
            arguments?.let { args ->
                isMission = args.getBoolean(PARAM_IS_MISSION, false)
                mainChartCode = args.getParcelable(Const.CHART_CODE_FOR_CERTIFICATE)
                isFromVotingCertificate = args.getBoolean(PARAM_IS_FROM_VOTING_CERTIFICATE, false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(PARAM_IS_MISSION, isMission)
        outState.putParcelable(Const.CHART_CODE_FOR_CERTIFICATE, mainChartCode)
        outState.putBoolean(PARAM_IS_FROM_VOTING_CERTIFICATE, isFromVotingCertificate)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel = ViewModelProvider(requireActivity()).get(
            MainViewModel::class.java
        )
        mAccount = getAccount(requireActivity())

        if (mainChartCode != null) {
            mainViewModel!!.setMainChartModel(mainChartCode!!)
        }

        if (isMission || isFromVotingCertificate) {
            binding.rlContainer.applySystemBarInsetsAndRequest()
        }

        observedVM()
    }

    override fun onResume() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isMission) {
                        parentFragmentManager.popBackStack()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            })
        isFirst = false

        super.onResume()
    }

    override fun onScrollToTop() {
        if(_binding == null) return
        binding.rvFavorite.scrollToPosition(0)
    }

    @Synchronized
    override fun loadResource() {
        // 최애 변경 후 갱신 안되서
        mAccount = getAccount(mContext)

        lifecycleScope.launch(Dispatchers.IO) {
            favoritesRepository.getFavoritesSelf(
                { response ->
                    val items: MutableList<FavoriteModel> = ArrayList()
                    val array = response.getJSONArray("objects")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val model = IdolGson.getInstance().fromJson(obj.toString(), FavoriteModel::class.java)
                        items.add(model)
                    }

                    lifecycleScope.launch(Dispatchers.IO) {
                        val allIdols = getAllIdolsUseCase()
                            .mapListDataResource { it.toPresentation() }
                            .awaitOrThrow() ?: listOf()

                        val favIds = items.mapNotNull { it.idol?.getId() }.toSet()
                        if(BuildConfig.CELEB) {
                            mainViewModel!!.findFavoriteIdolListCeleb(requireContext(), mAccount?.most, favIds, allIdols)
                        } else {
                            mainViewModel!!.findFavoriteIdolList(mAccount?.most, favIds, allIdols)
                        }
                    }
                }, { throwable ->

                }
            )
        }
    }

    @OptIn(UnstableApi::class)
    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)
        if (isVisible) {
            if (mEmptyView!!.visibility == View.VISIBLE) {
                deleteFavoritesCache()
            } else {
                loadResource()
            }

            //최애 화면 보일 때 udp timer start
            idolApiManager.startTimer()

            //화면 보일 때 제외된 아이돌이 즐찾에 포함되어 있는 경우 대비
            job = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) { // 🔹 Coroutine이 취소될 때까지 실행
                    delay(interval * 1000L) // 🔹 interval 초마다 실행

                    if (!idolApiManager.isConnected()) {
                        idolApiManager.updateExcludedFavorites()
                    }
                }
            }

            // 즐찾 변경시 띠배너 나올 수 있게
            favoriteAdapter?.notifyItemChanged(0)
        } else {
            idolApiManager.stopTimer()
            job?.cancel()
        }
    }

    private fun observedVM() {
        mainViewModel!!.favoriteData.observe(
            viewLifecycleOwner
        ) { data: Map<String, List<RankingModel>> ->
            mRankings.clear()

            data.forEach { key: String?, value: List<RankingModel> ->
                val maxVote = AtomicLong(0)
                val count = AtomicInteger(0)

                value.forEach(Consumer { item: RankingModel ->
                    if (item.idol != null) {
                        if (count.get() == 0) {
                            item.isSection = true
                        }
                        item.sectionName = key

                        if (maxVote.get() < item.idol.heart) {
                            maxVote.set(item.idol.heart)
                        }

                        count.incrementAndGet()
                    }
                    if (mAccount!!.most != null
                        && mAccount!!.most!!.getId() == item.idol?.getId()
                    ) {
                        //mAccount의   most의  기적  천사 카운트가  Rankingmodel 과  다른 경우가 있어  한번더 set 해줌.

                        mAccount!!.most!!.miracleCount = (item.idol.miracleCount)
                        mAccount!!.most!!.angelCount = (item.idol.angelCount)
                        mAccount!!.most!!.fairyCount = (item.idol.fairyCount)
                        mAccount!!.most!!.rookieCount = (item.idol.rookieCount)

                        mAccount!!.setMostImageUrl(
                            item.idol.imageUrl,
                            item.idol.imageUrl2,
                            item.idol.imageUrl3
                        )
                        mAccount!!.saveAccount(mContext!!)
                    }
                })
                value.forEach(Consumer<RankingModel> { item: RankingModel ->
                    if (item.idol != null
                        && !item.idol.type.isEmpty()
                    ) {
                        if (count.get() == 0) {
                            item.isSection = true
                        }
                        item.sectionName = key

                        item.sectionMaxVote = maxVote.get()
                        mRankings.add(item)
                    }
                })
            }

            applyItems(favoriteAdapter!!, mRankings)
//                    applyItemsToFooter(adapter, mRankings)

            // 최애 프사
            if (mAccount!!.most != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val most = getIdolByIdUseCase(mAccount!!.most!!.getId())
                        .mapDataResource { it }
                        .awaitOrThrow()

                    if (most != null) {
                        mAccount!!.userModel!!.most = most.toPresentation()
                    }

                    if (activity != null) {
                        requireActivity().runOnUiThread {
                            try {
                                // java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing
                                // a layout or scrolling androidx.recyclerview.widget.RecyclerView 방지
                                favoriteAdapter?.notifyItemChanged(0)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } else {
                if (activity != null) {
                    requireActivity().runOnUiThread {
                        try {
                            favoriteAdapter?.notifyItemChanged(0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            hideEmptyView()
        }

        mainViewModel!!.mostIdolData.observe(viewLifecycleOwner) { data: RankingModel? ->
            if (sAccount == null) {
                mAccount = getAccount(mContext)
            }
            if (data != null) {
                // 타이밍 문제로 실제 최애와 여기로 오는 최애가 다른 경우가 있음(최애를 해제하여 비밀의방이 된 경우 이전 최애 정보가 뒤늦게 오는 경우)
                var idol = data.idol
                if(mAccount?.most?.getId() == NON_FAVORITE_IDOL_ID) {
                   idol = mAccount?.most
                }
                mMostRank = data.ranking
                if (idol != null) {
                    Util.log("===== most: ${mAccount?.most?.getName()} data: ${data.idol?.getName()}")
                    Util.log("===== most.imageUrl: ${mAccount?.most?.imageUrl} data.imageUrl: ${data.idol?.imageUrl}")
                    mAccount!!.setMostImageUrl(
                        idol.imageUrl,
                        idol.imageUrl2,
                        idol.imageUrl3
                    )
                }
                mAccount!!.saveAccount(mContext!!)
            } else {
                mMostRank = -1
            }
        }

        // 최애 변경 감지
        viewLifecycleOwner.lifecycleScope.launch {
            // 다른 화면일 때는 불리자않고 최애화면에서만 불리도록 함
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedAppState.isMostChanged.collect { changed ->
                    if(changed) {
                        mainViewModel?.requestEvent(requireActivity(), ignoreBanner = true)
                        sharedAppState.setMostChanged(false)
                    }
                }
            }
        }
    }

    companion object {
        const val PARAM_IS_MISSION = "isMission"
        const val PARAM_IS_FROM_VOTING_CERTIFICATE = "isFromVotingCertificate"
    }
}
