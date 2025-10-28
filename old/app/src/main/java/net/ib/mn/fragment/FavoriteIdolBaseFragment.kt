package net.ib.mn.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.CommunityActivity.Companion.createIntent
import net.ib.mn.activity.CommunityActivity.Companion.isFavoriteChanged
import net.ib.mn.activity.FavoriteSettingActivity
import net.ib.mn.adapter.FavoriteRankingAdapter
import net.ib.mn.adapter.NewRankingAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.repository.FavoritesRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.databinding.FragmentFavoritBinding
import net.ib.mn.dialog.BaseDialogFragment.DialogResultHandler
import net.ib.mn.dialog.VoteDialogFragment.Companion.getIdolVoteInstance
import net.ib.mn.dialog.VoteDialogFragment.OnDismissWithResultListener
import net.ib.mn.domain.usecase.GetAllIdolsUseCase
import net.ib.mn.domain.usecase.GetIdolsByIdsUseCase
import net.ib.mn.idols.IdolApiManager
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.model.IdolModel
import net.ib.mn.model.RankingModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Const
import net.ib.mn.utils.EventBus
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.UtilK.Companion.getShareLocale
import net.ib.mn.utils.throttleFirst
import net.ib.mn.viewmodel.MainViewModel
import org.json.JSONArray
import org.json.JSONException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
abstract class FavoriteIdolBaseFragment : BaseFragment(),
    AdapterView.OnItemClickListener,
    AbsListView.OnScrollListener,
    View.OnClickListener,
    NewRankingAdapter.OnClickListener,
    DialogResultHandler {
    // 앱 시작하자마자 하단 탭 연타시 lateinit 초기화 안된 상태로 접근하는 문제 해결
    protected var _binding: FragmentFavoritBinding? = null
    protected val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()

    @Inject
    lateinit var idolApiManager: IdolApiManager

    @Inject
    lateinit var getIdolsByIdsUseCase: GetIdolsByIdsUseCase

    @Inject
    lateinit var getAllIdolsUseCase: GetAllIdolsUseCase
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var favoritesRepository: FavoritesRepository
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    var mContext: Context? = null

    protected var mEmptyView: TextView? = null
    protected var mNonEmptyView: View? = null

    var favoriteAdapter: FavoriteRankingAdapter? = null

    protected var mAccount: IdolAccount? = null
    protected var eventHeartDialog: Dialog? = null
    protected var mMostRank: Int = -1

    protected var displayErrorHandler: Handler? = null

    protected val mRankings: ArrayList<RankingModel> = ArrayList()

    // 움짤프사
    protected val mHeaderImageUrls: Array<String?> = arrayOfNulls(3)

    // 임시방편
    protected var useLegacyOnce: Boolean = false

    protected val interval: Long = if (BuildConfig.DEBUG) 5L else 10L

    var weakConext: WeakReference<Context>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayErrorHandler =
            Handler(Looper.getMainLooper()) { msg: Message ->
                if (activity != null && isAdded) {
                    val responseMsg = msg.obj as String
                    makeText(activity, responseMsg, Toast.LENGTH_SHORT).show()
                }
                true
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_favorit,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mEmptyView = binding.emptyView
        mNonEmptyView = binding.nonemptyView

        mContext = activity
        weakConext = WeakReference(mContext)
        mAccount = getAccount(mContext)
        showEmptyView()

        favoriteAdapter = FavoriteRankingAdapter(
            requireContext(),
            mGlideRequestManager,
            this@FavoriteIdolBaseFragment,
            this@FavoriteIdolBaseFragment,
            lifecycleScope,
            idolsRepository
        )
        favoriteAdapter?.setHasStableIds(true)

        val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)

        binding.rvFavorite.apply {
            adapter = favoriteAdapter
            addItemDecoration(divider)
            itemAnimator = null
            layoutManager = LinearLayoutManager(context)
        }

        // 최애화면에서 투표 후 갱신 처리
        lifecycleScope.launch {
            EventBus.receiveEvent<Boolean>(Const.BROADCAST_MANAGER_MESSAGE)
                .throttleFirst(1000) // 1초 동안 첫 번째 이벤트만 처리
                .collect { result ->
                    if (result) {
                        val copyOfRankings = ArrayList(mRankings) // mRankings를 동시에 참조하지 않도록 복제

                        lifecycleScope.launch(Dispatchers.IO) {
                            val favIds = copyOfRankings.map { it.idol!!.getId() }.toSet()
                            val allIdols = getAllIdolsUseCase()
                                .mapListDataResource { it.toPresentation() }
                                .awaitOrThrow() ?: listOf()
                            if(BuildConfig.CELEB) {
                                mainViewModel.findFavoriteIdolListCeleb(requireContext(), mAccount?.most, favIds, allIdols)
                            } else {
                                mainViewModel.findFavoriteIdolList(mAccount?.most, favIds, allIdols)
                            }
                        }
                    }
                }
        }

        lifecycleScope.launch {
            EventBus.receiveEvent<Boolean>(Const.BROADCAST_REFRESH_TOP3).collect { result ->
                if (result) {
                    refreshTop3()
                }
            }
        }
    }

    val communityActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (isAdded) {
                    useLegacyOnce = true
                    loadResource()
                }
            }
        }

    protected abstract fun loadResource()

    fun clickMyMostBanner(id: Int, kind: String) {
        var url = ""
        when (kind) {
            FavoriteRankingAdapter.themepick -> url =
                ServerUrl.HOST + "/" + "themepick" + "/" + id + "?locale=" + getShareLocale(
                    mContext!!
                )

            FavoriteRankingAdapter.heartpick -> url =
                ServerUrl.HOST + "/" + "heartpick" + "/" + id + "?locale=" + getShareLocale(
                    mContext!!
                )

            FavoriteRankingAdapter.miracle -> url = ServerUrl.HOST + "/" + "miracle" + "?locale=" + getShareLocale(
                mContext!!
            )

            FavoriteRankingAdapter.onepick -> url =
                ServerUrl.HOST + "/" + "onepick" + "/" + id + "?locale=" + getShareLocale(
                    mContext!!
                )
        }
        openWebPage(url)
    }


    private fun openWebPage(url: String) {
        val browserIntent = Intent(mContext, AppLinkActivity::class.java)
        browserIntent.setData(Uri.parse(url))
        startActivity(browserIntent)
    }

    private fun voteHeart(model: IdolModel, total_heart: Long, freeHeart: Long) {
        val fragment =
            getIdolVoteInstance(model, total_heart, freeHeart)

        fragment.setOnDismissWithResultListener(object: OnDismissWithResultListener {
            override fun onDismiss(voteHeart: Long) {
                val most1 = mAccount!!.most
                if (most1 != null) {
                    most1.heart = most1.heart + voteHeart
                    mAccount!!.most = most1

                    if (voteHeart > 0) {
                        // event_heart는 이제 더이상 없음
                        // 레벨업 체크
                        UtilK.checkLevelUp(baseActivity, accountManager, model, voteHeart)

                        // 비밀의방은 udp로 오지 않으므로 여기서 직접 업데이트 시켜준다
                        favoriteAdapter?.notifyDataSetChanged()
                        refreshTop3()
                    }
                }
            }
        })

        fragment.show(parentFragmentManager, "favorite_vote")
    }

    override fun onVote(item: IdolModel) {
        Util.showProgress(mContext)
        lifecycleScope.launch {
            usersRepository.isActiveTime(
                { response ->
                    Util.closeProgress()
                    if (response.optBoolean("success")) {
                        val gcode = response.optInt("gcode")
                        if (response.optString("active") == Const.RESPONSE_Y) {
                            if (response.optInt("total_heart") == 0) {
                                Util.showChargeHeartDialog(mContext)
                            } else {
                                if (response.optString("vote_able").equals(
                                        Const.RESPONSE_Y, ignoreCase = true
                                    )
                                ) {
                                    voteHeart(
                                        item,
                                        response.optLong("total_heart"),
                                        response.optLong("free_heart")
                                    )
                                } else {
                                    if (gcode == Const.RESPONSE_IS_ACTIVE_TIME_1) {
                                        makeText(
                                            mContext,
                                            getString(
                                                R.string.response_users_is_active_time_over
                                            ),
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    } else {
                                        makeText(
                                            mContext,
                                            getString(R.string.msg_not_able_vote),
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }
                                }
                            }
                        } else {
                            val start = Util.convertTimeAsTimezone(
                                response.optString("begin")
                            )
                            val end = Util.convertTimeAsTimezone(
                                response.optString("end")
                            )
                            val unableUseTime = String.format(
                                getString(R.string.msg_unable_use_vote),
                                start, end
                            )

                            Util.showIdolDialogWithBtn1(
                                mContext,
                                null,
                                unableUseTime
                            ) { Util.closeIdolDialog() }
                        }
                    } else { // success is false!
                        UtilK.handleCommonError(mContext, response)
                    }
                }, {
                    Util.closeProgress()
                    makeText(
                        mContext, R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            )
        }
    }

    private fun refreshTop3() {
        val context = mContext ?: return

        // 비밀의방은 udp로 오지 않으므로 여기서 직접 업데이트 시켜준다. 또한 투표 후 탑3 갱신을 좀 더 빠르게 한다
        val most = mAccount?.most
        most?.let {
            val mostIdolUpdateList = ArrayList<Int>()
            mostIdolUpdateList.add(most.getId())

            idolApiManager.updateIdols(mostIdolUpdateList) { responseArray: JSONArray ->
                try {
                    if (responseArray.length() <= 0) {
                        return@updateIdols
                    }

                    val idolMost = IdolGson.instance.fromJson(
                        responseArray
                            .getJSONObject(0)
                            .toString(), IdolModel::class.java
                    )
                    // 비밀의방 같은 경우는 DB에 없으니 일단 최애 정보에 반영해주고
                    val most1 = mAccount?.most
                    if (most1 != null) {
                        most1.heart = idolMost.heart
                        most1.top3 = idolMost.top3
                        most1.top3Type = idolMost.top3Type
                        most1.imageUrl = idolMost.imageUrl
                        most1.imageUrl2 = idolMost.imageUrl2
                        most1.imageUrl3 = idolMost.imageUrl3
                        mAccount?.most = most1
                        favoriteAdapter?.notifyItemChanged(0)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    protected fun applyItems(adapter: FavoriteRankingAdapter, items: List<RankingModel>) {
//        if (items.size > 0) {
//            mListView!!.dividerHeight = 1
//        } else {
//            mListView!!.divider = null
//        }
//        adapter.clear()
        adapter.setItems(items.toCollection(ArrayList()))
        adapter.notifyDataSetChanged()
    }

    override fun onItemClicked(item: IdolModel) {
        communityActivityResultLauncher.launch(createIntent(mContext!!, item))
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        val model = mRankings[position]
        communityActivityResultLauncher.launch(createIntent(mContext!!, model.idol))
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_bookmark_setting, R.id.btn_favorite_setting -> {
                if (Util.mayShowLoginPopup(baseActivity)) {
                    return
                }
                startActivityForResult(FavoriteSettingActivity.createIntent(mContext), 10)
            }

            R.id.iv_agg_description -> Util.showIdolDialogWithBtn1(
                mContext,
                null,
                getString(if (BuildConfig.CELEB) R.string.actor_msg_favorite_guide else R.string.msg_favorite_guide)
            ) { v1: View? -> Util.closeIdolDialog() }
        }
    }

    protected fun showEmptyView() {
        mEmptyView?.visibility = View.VISIBLE
        mNonEmptyView?.visibility = View.GONE
    }

    protected fun hideEmptyView() {
        mEmptyView?.visibility = View.GONE
        mNonEmptyView?.visibility = View.VISIBLE
    }

//    override fun onClicked(item: IdolModel) {
//        onVoteBtnClicked(item)
//    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // 더미로 남겨둠
    }

    private fun showEventDialog(event_heart: String) {
        eventHeartDialog = Dialog(mContext!!, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        eventHeartDialog!!.window!!.attributes = lpWindow
        eventHeartDialog!!.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        eventHeartDialog!!.setContentView(R.layout.dialog_surprise_heart)
        eventHeartDialog!!.setCanceledOnTouchOutside(false)
        eventHeartDialog!!.setCancelable(true)
        val btnOk = eventHeartDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
        btnOk.setOnClickListener { v: View? -> eventHeartDialog!!.cancel() }
        val msg = eventHeartDialog!!.findViewById<AppCompatTextView>(R.id.message)
        val surprise_msg = String.format(getString(R.string.msg_surprise_heart), event_heart)
        msg.text = surprise_msg
        eventHeartDialog!!
            .window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        eventHeartDialog!!.show()
    }

    override fun onPause() {
        super.onPause()

        // 움짤 멈추기
        if (Const.USE_ANIMATED_PROFILE) {
            stopExoPlayer(playerView1)
            stopExoPlayer(playerView2)
            stopExoPlayer(playerView3)
        }
    }

    override fun onStop() {
        super.onStop()

        Util.log("RankingFragment onStop")
    }

    override fun onResume() {
        super.onResume()

        //최애 변경사항이 있을때만 체크해서 -> user/self를 불러준다.
        if (isFavoriteChanged) {
            //최애변경 사항 다시 false로 reset

            isFavoriteChanged = false
            accountManager.fetchUserInfo(mContext)
        }
        if (mContext == null) mContext = activity

        Util.log("RankingFragment onResume")

        if (Const.USE_ANIMATED_PROFILE) {
            // 움짤 다시 재생
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    startExoPlayer(playerView1)
                    startExoPlayer(playerView2)
                    startExoPlayer(playerView3)
                }, 500
            )
        }
    }

    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
    }

    override fun onScroll(
        view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int
    ) {
        // playerView들이 화면을 벗어나면 재생 멈추기
        if (playerView1 == null) return

        val wrapper = playerView1?.parent?.parent as View

        if (wrapper.tag == null) return

        val index = wrapper.tag.toString().toInt()


        if (index + 1 < firstVisibleItem || index + 1 >= firstVisibleItem + visibleItemCount) {
            stopExoPlayer(playerView1)
            stopExoPlayer(playerView2)
            stopExoPlayer(playerView3)
        }
    }

    protected fun deleteFavoritesCache() {
        // KST 기준으로 마지막으로 즐찾을 가져온 날짜가 다르면, 캐시를 비우고 가져옴
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val favoriteDate = Util.getPreference(requireActivity(), Const.PREF_FAVORITE_DATE)
        val currentDate = sdf.format(Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).time)

        if (favoriteDate.isEmpty()) {
            Util.setPreference(requireActivity(), Const.PREF_FAVORITE_DATE, currentDate)
            if (activity != null && isAdded) {
                loadResource()
            }
        } else {
            try {
                val parsedFavoriteDate = sdf.parse(favoriteDate)
                val parsedCurrentDate = sdf.parse(currentDate)

                checkNotNull(parsedFavoriteDate)
                if (parsedFavoriteDate.compareTo(parsedCurrentDate) < 0) {
                    Util.setPreference(requireActivity(), Const.PREF_FAVORITE_DATE, currentDate)

                    lifecycleScope.launch {
                        favoritesRepository.deleteCache(
                            {
                                if (activity != null && isAdded) {
                                    loadResource()
                                }
                            }, {
                                if (activity != null && isAdded) {
                                    loadResource()
                                }
                            }
                        )
                    }
                } else {
                    loadResource()
                }
            } catch (e: Exception) {
                // 에러나면 그냥 가져오는 걸로...
                e.printStackTrace()
                Util.setPreference(requireActivity(), Const.PREF_FAVORITE_DATE, currentDate)

                lifecycleScope.launch {
                    favoritesRepository.deleteCache(
                        {
                            if (activity != null && isAdded) {
                                loadResource()
                            }
                        }, {
                            if (activity != null && isAdded) {
                                loadResource()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onPhotoClicked(item: IdolModel, position: Int) {
        binding.rvFavorite.postDelayed({
            // get clicked item view
            val targetView = binding.rvFavorite.findViewHolderForAdapterPosition(position)?.itemView
            // get window location
            val location = IntArray(2)

            if (targetView != null) {
                targetView.getLocationInWindow(location)
                // expanded height
                val viewHeight = targetView.height
                val viewWidth = targetView.width
                val targetY = location[1] - (viewWidth / 3)
                val targetHeight = viewHeight + viewWidth / 3
                val targetBottom = targetY + targetHeight
                binding.rvFavorite.getLocationInWindow(location)
                val listviewTop = location[1]
                val listviewBottom = listviewTop + binding.rvFavorite.height
                // check if target bottom is under listview's bottom
                if (targetBottom > listviewBottom) {
                    binding.rvFavorite.smoothScrollBy(targetBottom - listviewBottom + viewHeight, 200)
                }
            }

            // 펼친거 모두 닫으면 움짤프사 다시 재생
            if (mapExpanded.values.all { !it }) {
                favoriteAdapter?.notifyItemChanged(0)
            }
        }, 300)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        protected const val REQUEST_CODE_COMMUNITY: Int = 1000 // 커뮤 투표 반영
    }
}
