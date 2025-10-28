/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 잡담게시판 목록.
 *
 * */

package net.ib.mn.smalltalk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.activity.WebViewActivity
import net.ib.mn.activity.WriteSmallTalkActivity
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.databinding.FragmentSmallTalkBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.NoticeModel
import net.ib.mn.smalltalk.adapter.SmallTalkAdapter
import net.ib.mn.smalltalk.viewholder.SmallTalkHeaderVH
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.utils.BoardLanguage
import net.ib.mn.utils.CelebTutorialBits
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.Translation
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.CommunityActivityViewModel
import java.util.Locale

@AndroidEntryPoint
class SmallTalkFragment : BaseFragment(), Translation {

    lateinit var binding: FragmentSmallTalkBinding

    internal lateinit var manager: FragmentManager
    private lateinit var trans: FragmentTransaction
    private var actionbar: ActionBar? = null

    private val communityActivityViewModel: CommunityActivityViewModel by activityViewModels()
    private val smallTalkViewModel: SmallTalkViewModel by viewModels()

    private lateinit var smallTalkAdapter: SmallTalkAdapter

    var orderBy: String = FILTER_DATE_ORDER
    var keyWord: String? = null
    var locale: String? = null

    private var isFilterDefault: Boolean? = false // 아이돌 게시판 글 작성 후, 필터 최신화 해야할 때 사용

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_small_talk, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSet()
        getDataFromVM()

        getSmallTalkInventory(FILTER_DATE_ORDER, null, null)

    }

    private fun setListenerEvent() {
        smallTalkAdapter.setItemEventListener(object : SmallTalkAdapter.ItemListener {
            override fun filterSetCallBack(bottomSheetFragment: BottomSheetFragment) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.SORT_FILTER_BUTTON_SMALL_TALK_ARTICLE.actionValue,
                    GaAction.SORT_FILTER_BUTTON_SMALL_TALK_ARTICLE.label,
                )
                setFilter(bottomSheetFragment)
            }

            override fun filterLocaleCallback(bottomSheetFragment: BottomSheetFragment) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.LOCALE_FILTER_BUTTON_SMALL_TALK_ARTICLE.actionValue,
                    GaAction.LOCALE_FILTER_BUTTON_SMALL_TALK_ARTICLE.label,
                )
                setFilter(bottomSheetFragment)
            }

            override fun localeClickCallBack(orderBy: String, keyWord: String?, locale: String?, langText: String) {
                val firebaseLangText = when (langText) {
                    getString(BoardLanguage.all().first().labelResId) -> {
                        UtilK.getLocaleStringResource(
                            Locale.KOREA,
                            BoardLanguage.all().first().labelResId,
                            requireActivity(),
                        )
                    }
                    getString(BoardLanguage.all()[1].labelResId) -> {
                        UtilK.getLocaleStringResource(
                            Locale.KOREA,
                            BoardLanguage.all()[1].labelResId,
                            requireActivity(),
                        )
                    }
                    else -> {
                        langText
                    }
                }

                setUiActionFirebaseGoogleAnalyticsFragmentWithKey(
                    GaAction.LOCALE_FILTER_SMALL_TALK_ARTICLE.actionValue,
                    GaAction.LOCALE_FILTER_SMALL_TALK_ARTICLE.label,
                    GaAction.LOCALE_FILTER_KEY.paramKey,
                    firebaseLangText,
                )
                getSmallTalkInventory(
                    orderBy = orderBy,
                    keyWord = keyWord,
                    locale = locale,
                )
            }

            override fun filterClickCallBack(orderBy: String, keyWord: String?, locale: String?, orderByText: String) {
                setUiActionFirebaseGoogleAnalyticsFragmentWithKey(
                    GaAction.SORT_FILTER_SMALL_TALK_ARTICLE.actionValue,
                    GaAction.SORT_FILTER_SMALL_TALK_ARTICLE.label,
                    GaAction.SORT_FILTER_KEY.paramKey,
                    orderByText,
                )
                getSmallTalkInventory(
                    orderBy = orderBy,
                    keyWord = keyWord,
                    locale = locale,
                )
            }

            override fun searchCallBack(orderBy: String, keyWord: String?, locale: String?) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.SEARCH_SMALL_TALK_ARTICLE.actionValue,
                    GaAction.SEARCH_SMALL_TALK_ARTICLE.label,
                )
                getSmallTalkInventory(
                    orderBy = orderBy,
                    keyWord = keyWord,
                    locale = locale,
                )
            }

            override fun noticeClickCallback(noticeModel: NoticeModel) {
                startActivity(
                    WebViewActivity.createIntent(requireContext(), "notices", noticeModel.id.toInt(),
                        noticeModel.title, noticeModel.title, false))
            }

            override fun smallTalkItemClicked(articleModel: ArticleModel, position: Int, isTutorial: Boolean) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.MOVE_SMALL_TALK_DETAIL_ARTICLE.actionValue,
                    GaAction.MOVE_SMALL_TALK_DETAIL_ARTICLE.label,
                )

                if (isTutorial) {
                    val tutorialIndex = if (BuildConfig.CELEB) {
                        CelebTutorialBits.FAN_TALK_DETAIL
                    } else {
                        TutorialBits.COMMUNITY_FAN_TALK_DETAIL
                    }
                    communityActivityViewModel.updateTutorial(tutorialIndex)
                }

                articleModel.idol = smallTalkViewModel.idolModel.value
                smallTalkViewModel.startActivityResultLauncher?.launch(
                    NewCommentActivity.createIntent(requireActivity(), articleModel, position, false, NewCommentAdapter.TYPE_ARTICLE, tagName = (articleModel.idol?.getName(requireContext())) ?: ""),
                )
            }
        })

        binding.rvSmallTalk.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val lastVisiblePosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                val itemTotalCount = recyclerView.adapter!!.itemCount - 1

                smallTalkViewModel.recyclerviewScrollState =
                    recyclerView.layoutManager?.onSaveInstanceState()

                // 스크롤이 끝에 도달했는지 확인후  api를 요청해서 다음 페이지를 받아온다.
                if (!recyclerView.canScrollVertically(1) &&
                    lastVisiblePosition == itemTotalCount &&
                    !smallTalkViewModel.getNextResouceUrl().isNullOrEmpty()
                ) {
                    smallTalkViewModel.getSmallTalkInventory(
                        context = requireActivity(),
                        orderby = orderBy,
                        type = "M",
                        keyWord = keyWord,
                        locale = locale,
                        isLoadMore = true,
                    )
                }

                if (smallTalkViewModel.getNextResouceUrl().isNullOrEmpty()) {
                    smallTalkAdapter.deleteLoading()
                }
            }
        })
        binding.rvSmallTalk.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && isFilterDefault == true) { // 스크롤이 끝났고, 글을 작성해서 필터를 디폴트로 만들어야하는 경우
                    (binding.rvSmallTalk.findViewHolderForAdapterPosition(0) as SmallTalkHeaderVH?)?.allClickChange()
                    isFilterDefault = false
                }
            }
        })
    }
    private fun getDataFromVM() {
        // 잡담 게시물 받아오기 성공.
        smallTalkViewModel.smallTalkArticleList.observe(viewLifecycleOwner) {
            Util.closeProgress()

            setAdapter()

            val articleList = it.map { model -> model.clone() as ArticleModel }.toMutableList()
            smallTalkAdapter.apply {
                setNoticeCount(smallTalkViewModel.getNoticeCount())
                submitList(
                    articleList.apply {
                        if (this.size < 2) { // 헤더값 포함이라서 2로 세팅을 해줌.
                            val model = ArticleModel()
                            model.id = SmallTalkAdapter.EMPTY_ITEM.toString()
                            this.add(model)
                            return@apply
                        }

                        if (!smallTalkViewModel.getNextResouceUrl().isNullOrEmpty()) {
                            val model = ArticleModel()
                            model.id = SmallTalkAdapter.LOADING_ITEM.toString()
                            model.isLoading = true
                            this.add(model)
                        }
                    },
                )
            }

            smallTalkAdapter.currentList.forEach { model ->
                model.isEdit = false
            }
        }

        smallTalkViewModel.errorToast.observe(
            viewLifecycleOwner,
            SingleEventObserver { errorMessage ->
                IdolSnackBar.make(
                    requireActivity().findViewById(android.R.id.content),
                    errorMessage,
                )
                    .show()
            },
        )

        communityActivityViewModel.smallTalkWrite.observe(viewLifecycleOwner, SingleEventObserver{
            if(it) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.MOVE_SMALL_TALK_WRITE_ARTICLE.actionValue,
                    GaAction.MOVE_SMALL_TALK_WRITE_ARTICLE.label,
                )
                smallTalkViewModel.startActivityResultLauncher.launch(
                    WriteSmallTalkActivity.createIntent(requireActivity(), smallTalkViewModel.idolModel.value),
                )
            }
        })

        // 게시글 작성 시 화면 초기화 되면서 업데이트
        smallTalkViewModel.articleAdd.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                if (it) {
                    getSmallTalkInventory(FILTER_DATE_ORDER, null, null)
                    smallTalkAdapter.notifyItemChanged(0)
                    binding.rvSmallTalk.smoothScrollToPosition(0)
                    isFilterDefault = true
                }
            },
        )

        communityActivityViewModel.clickSmallTalkTab.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                binding.rvSmallTalk.smoothScrollToPosition(0)
            }
        )
        communityActivityViewModel.newSmallTalkIntent.observe(
            viewLifecycleOwner,
            SingleEventObserver {isRecent ->
                if(isRecent == true){
                    getSmallTalkInventory(FILTER_DATE_ORDER, null, null)
                    smallTalkAdapter.notifyItemChanged(0)
                    binding.rvSmallTalk.smoothScrollToPosition(0)
                    isFilterDefault = true
                }
            }
        )
    }

    private fun initSet() {
        actionbar = (activity as AppCompatActivity).supportActionBar
        manager = requireActivity().supportFragmentManager
        trans = manager.beginTransaction()

        smallTalkViewModel.registerActivityResult(requireActivity(), this)

        binding.rvSmallTalk.layoutManager?.onRestoreInstanceState(smallTalkViewModel.recyclerviewScrollState)
    }

    private fun setAdapter() {
        if (!this::smallTalkAdapter.isInitialized) {
            smallTalkAdapter = SmallTalkAdapter(requireContext(), FILTER_DATE_ORDER, null)

            binding.rvSmallTalk.apply {
                adapter = smallTalkAdapter
            }

            setListenerEvent()
        }
    }

    private fun getSmallTalkInventory(
        orderBy: String,
        keyWord: String?,
        locale: String?,
        isLoadMore: Boolean = false,
    ) {
        // 어댑터에서 온값들 저장.
        this.orderBy = orderBy
        this.keyWord = keyWord
        this.locale = locale
        Util.showProgress(requireActivity())
        smallTalkViewModel.getSmallTalkInventory(
            requireActivity(),
            orderBy,
            "M",
            keyWord,
            locale,
            isLoadMore,
        )
    }

    private fun setFilter(bottomSheetFragment: BottomSheetFragment) {
        val tag = "filter"
        val oldFrag = requireActivity().supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            bottomSheetFragment.show(requireActivity().supportFragmentManager, tag)
        }
    }

    companion object {
        const val FILTER_DATE_ORDER = "-created_at"
        const val FILTER_LIKE_ORDER = "-like_count"
        const val FILTER_HITS_ORDER = "-view_count"
        const val FILTER_COMMENT_ORDER = "-num_comments"
    }
}