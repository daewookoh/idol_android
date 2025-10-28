package net.ib.mn.fragment

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.MainActivity.Companion.TAG_SUMMARY
import net.ib.mn.adapter.MainTop10RankingAdapter
import net.ib.mn.adapter.NewRankingAdapter2
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.bitmapool.BitmapPool
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.databinding.NewFragmentRanking2Binding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.VoteDialogFragment
import net.ib.mn.domain.usecase.GetAllIdolsUseCase
import net.ib.mn.domain.usecase.GetIdolsByTypeAndCategoryUseCase
import net.ib.mn.domain.usecase.SaveIdolsUseCase
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toDomain
import net.ib.mn.model.toPresentation
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.EventBus
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.getUiColor
import net.ib.mn.utils.sort
import net.ib.mn.view.ExodusImageView
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.round
import kotlin.collections.ArrayList

/**
 * 셀럽 메인화면 카테고리별 순위 (탑10)
 */
@UnstableApi
@AndroidEntryPoint
abstract class NewRankingFragment2 : BaseFragment(),
        BaseDialogFragment.DialogResultHandler,
        NewRankingAdapter2.OnClickListener,
        MainTop10RankingAdapter.OnClickListener {

    @Inject
    lateinit var getIdolsByTypeAndCategoryUseCase: GetIdolsByTypeAndCategoryUseCase

    @Inject
    lateinit var saveIdolsUseCase: SaveIdolsUseCase

    @Inject
    lateinit var getIdolsUseCase: GetAllIdolsUseCase
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    private val MAX_ITEMS = 10 // 최대 10위까지 표시
    private lateinit var models: ArrayList<IdolModel>

	lateinit var mClRankingView: ConstraintLayout
	lateinit var mClSummaryView: ConstraintLayout
    lateinit var mEmptyView: AppCompatTextView
    lateinit var rvRanking: RecyclerView
    lateinit var rvTop10: RecyclerView
    protected var mRankingAdapter: NewRankingAdapter2? = null
//    private lateinit var mGlideRequestManager: RequestManager
	private lateinit var weakActivity : WeakReference<FragmentActivity>
    protected var top10RankingAdapter: MainTop10RankingAdapter? = null

    protected abstract fun getVoteRequestCode(): Int
    protected val displayErrorHandler = DisplayErrorHandler(activity)
    protected class DisplayErrorHandler(activity: FragmentActivity?) : Handler() {
			private val mActivity: WeakReference<FragmentActivity?>
			override fun handleMessage(msg: Message) {
				val activity = mActivity.get()
				super.handleMessage(msg)
				try {
					val responseMsg = msg.obj as String
					Toast.makeText(activity, responseMsg, Toast.LENGTH_SHORT).show()
				}catch (e: Exception) {
					e.printStackTrace()
				}
			}
			init {
				mActivity = WeakReference(activity)
			}
	}
    private lateinit var eventHeartDialog: Dialog

	var type: String? = null
	var isDevided : String? = null
	var isFemale : Boolean = false
	var typeName: String? = null
	var category: String? = null

    protected lateinit var typeList: TypeListModel
	var femaleCategory : String? = null

    protected lateinit var animator : SimpleItemAnimator

    protected var timerHandler : Handler? = null // 10초 자동갱신 타이머
    protected var timerRunnable : Runnable? = null
    protected val refreshInterval : Long = if( BuildConfig.DEBUG ) 30 else 10    // 10초 갱신

	//초기화 다시 해야함
	protected lateinit var ivMap:HashMap<Int, ExodusImageView>
	protected lateinit var ivMapForTopBanner:HashMap<Int, ExodusImageView>
	protected lateinit var playerMapForTopBanner:HashMap<Int, PlayerView>
    protected lateinit var wrapperMap:HashMap<Int, RelativeLayout?>
    protected lateinit var voteMap:HashMap<Int, TextView>
    protected lateinit var anniversaryMap:HashMap<Int, AppCompatImageView>
    protected lateinit var duplCheckMap:HashMap<Int, String?>
    protected lateinit var oldCountMap:HashMap<Int, Long?>

	private val ANIMATION_DURATION : Long = 700
	private var rankingViewAnimationIsOn = false

    var fragmentListener : FragmentListener? = null

	//top1 배너  미디어 url
	private var topBannerMediaUrl1:String? = ""
	private var topBannerMediaUrl2:String? = ""
	private var topBannerMediaUrl3:String? = ""

	//
	private var isPlayingVideo : Boolean = false // 움짤 재생 시작했는지 여부
	private var playerHandler : Handler = Handler() // 움짤 재생 딜레이 핸들러
    private var thumbnailLoaded:HashMap<Int, Boolean> = HashMap() // 썸네일 다 불러왔는지 (fail인 경우도 불러온거로 처리)
	private var disposable : CompositeDisposable = CompositeDisposable()

    protected lateinit var binding: NewFragmentRanking2Binding

    interface FragmentListener{
        fun onPrevClick()
		fun onMoreClick()
		fun onPrevPageClick()
		fun onNextPageClick()
		fun setRankingViewAnimationIsOn(flag: Boolean)
    }


    private val mBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
			if (intent.action!!.equals(Const.REFRESH_SUMMARY_RANKING, ignoreCase = true)) {
				applyItems(models)
				return
			}

            // 메뉴에서 카테고리 변경시 처리
            if (intent.action!!.equals(Const.REFRESH, ignoreCase = true)) {

                // 현재 보여지는 상태라면 reload
                if (isVisible) updateDataWithUI(false)

                return
            }

            if (!Const.USE_ANIMATED_PROFILE || !isVisible) return

            val index = intent.getIntExtra("index", 0)
            // 움짤 주소가 있을 때에만 처리
            try {
				if (models.size > 0) {
					if (index == 0
						&& playerView1 != null
						&& hasVideo(playerView1)
					) {
						(playerView1?.parent as ViewGroup)
							.findViewById<View>(R.id.photo1).visibility = View.INVISIBLE
						playerView1?.visibility = View.VISIBLE
					} else if (index == 1
						&& playerView2 != null
						&& hasVideo(playerView2)
					) {
						(playerView2?.parent as ViewGroup)
							.findViewById<View>(R.id.photo2).visibility = View.INVISIBLE
						playerView2?.visibility = View.VISIBLE
					} else if (index == 2
						&& playerView3 != null
						&& hasVideo(playerView3)
					) {
						(playerView3?.parent as ViewGroup)
							.findViewById<View>(R.id.photo3).visibility = View.INVISIBLE
						playerView3?.visibility = View.VISIBLE
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        mGlideRequestManager = GlideApp.with(this)

		weakActivity = WeakReference<FragmentActivity>(requireActivity())
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = NewFragmentRanking2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

		//어르신 모드때 ui 모드
//		setLargeFontUi()

        mClRankingView = binding.clRankingView
		mClSummaryView = binding.clSummaryView

		ivMapForTopBanner = hashMapOf(0 to binding.photo1, 1 to binding.photo2, 2 to binding.photo3)
		playerMapForTopBanner = hashMapOf(
			0 to binding.topPlayerview1,
			1 to binding.topPlayerview2,
			2 to binding.topPlayerview3
		)
        wrapperMap = hashMapOf( 1 to null) // 기존 소스와 호환성을 위해
        duplCheckMap = hashMapOf(0 to "")
        voteMap = hashMapOf(0 to binding.tvTop1Vote)
        anniversaryMap = hashMapOf(0 to binding.ivTop1Anniversary)
        oldCountMap = hashMapOf(0 to 0L)
		models = ArrayList()

        val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)

        rvRanking = binding.rvRanking
        rvRanking.layoutManager = LinearLayoutManagerWrapper(requireContext(), LinearLayoutManager.VERTICAL, false)
        mRankingAdapter = NewRankingAdapter2(requireActivity(),
                this,
                mGlideRequestManager,
                models,
                this, typeList)
        if(Util.getPreferenceBool(context, Const.PREF_ANIMATION_MODE, false)) {
            animator = DefaultItemAnimator()
            animator.supportsChangeAnimations = true
            rvRanking.itemAnimator = animator
        }else{
            rvRanking.itemAnimator = null
        }

		mRankingAdapter?.setHasStableIds(true)
        rvRanking.adapter = mRankingAdapter
        rvRanking.addItemDecoration(divider)
        rvRanking.setHasFixedSize(true)

        // top 10
        rvTop10 = binding.rvMainTop10
        rvTop10.layoutManager = LinearLayoutManagerWrapper(requireContext(), LinearLayoutManager.VERTICAL, false)

        top10RankingAdapter = MainTop10RankingAdapter(
            requireActivity(),
            requireContext(),
            this,
            this,
            mGlideRequestManager,
            ArrayList(),
        )

        if (Util.getPreferenceBool(context, Const.PREF_ANIMATION_MODE, false)) {
            animator = DefaultItemAnimator()
            animator.supportsChangeAnimations = true
            rvTop10.itemAnimator = animator
        } else {
            rvTop10.itemAnimator = null
        }

        top10RankingAdapter?.setHasStableIds(true)
        rvTop10.adapter = top10RankingAdapter
//        rvTop10.addItemDecoration(divider)
        rvTop10.setHasFixedSize(true)

        mEmptyView = binding.tvEmpty
        val tmp = Date().time
        val formatter = SimpleDateFormat("yyyy. MM. dd ", Locale.getDefault())
        val date = formatter.format(tmp)
        binding.tvDate.text = date + requireContext().getString(R.string.ranking)

        val bgShape_ranking = binding.layoutBgRanking.getBackground() as GradientDrawable
        bgShape_ranking.setColor(Color.parseColor(typeList.getUiColor(requireContext()).toString()))

		//폰 작을 때 xml background 다시 묶어서 분기
		val bgShape_summary : GradientDrawable?

			bgShape_summary = binding.clBgSummary.getBackground() as GradientDrawable?
			bgShape_summary?.setColor(Color.parseColor(typeList.getUiColor(requireContext()).toString()))
		if(type == "S"){
            // 남녀 외 다른 카테고리 껴들어가는 경우 미리 대비
            when(typeCheck()) {
                "M" -> {
                    binding.tvTitle.text = getString(R.string.actor_male_singer)
                    binding.tvMore.text = String.format(getString(R.string.see_ranking), getString(R.string.actor_male_singer))
                }
                "F" -> {
                    binding.tvTitle.text = getString(R.string.actor_female_singer)
                    binding.tvMore.text = String.format(getString(R.string.see_ranking), getString(R.string.actor_female_singer))
                }
                else -> {
                    binding.tvTitle.text = typeList.name
                    binding.tvMore.text = String.format(getString(R.string.see_ranking), typeList.name)
                }
            }
		}
		else if(type == "A"){
            // 해외 배우가 껴들어가서
            when(typeCheck()) {
                "M" -> {
                    binding.tvTitle.text = getString(R.string.lable_actors)
                    binding.tvMore.text = String.format(getString(R.string.see_ranking), getString(R.string.lable_actors))
                }
                "F" -> {
                    binding.tvTitle.text = getString(R.string.lable_actresses)
                    binding.tvMore.text = String.format(getString(R.string.see_ranking), getString(R.string.lable_actresses))
                }
                else -> {
                    binding.tvTitle.text = typeList.name
                    binding.tvMore.text = String.format(getString(R.string.see_ranking), typeList.name)
                }
            }
		}
		else{
			binding.tvTitle.text = typeList.name
			binding.tvMore.text = String.format(getString(R.string.see_ranking), typeList.name)
		}

        binding.btnMoreSummary.setOnClickListener {
			if (!rankingViewAnimationIsOn && fragIsVisible && SummaryMainFragment.mSummaryAdapter != null &&
				this::mClRankingView.isInitialized && this::mClSummaryView.isInitialized
			) {
				try {
					stopExoPlayer(playerView1)
					stopExoPlayer(playerView2)
					stopExoPlayer(playerView3)
					BitmapPool.instance.putViewToBitmapPool(
						typeName, femaleCategory, getBitmapFromView(
						mClRankingView
					)
					)
					closeAnimationRankingView()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
		binding.btnMoreRanking.setOnClickListener {
			if (!rankingViewAnimationIsOn && fragIsVisible && SummaryMainFragment.mSummaryAdapter != null &&
				this::mClRankingView.isInitialized && this::mClSummaryView.isInitialized
			) {
				try {
                    startAnimationRankingView()
				} catch (e : Exception){
					e.printStackTrace()
				}
			}
		}
		binding.prevPage.setOnClickListener{
			if(!rankingViewAnimationIsOn() && !fragIsVisible) fragmentListener?.onNextPageClick()
		}
		binding.nextPage.setOnClickListener{
			if(!rankingViewAnimationIsOn() && !fragIsVisible) fragmentListener?.onPrevPageClick()
		}
        // DB에 저장된 순위 먼저 보여준다
		// true로 줘서 맨처음 랭킹뷰리스트만 미리 넣어준다.
        //  => true로 주면 랭킹리스트뷰 만드느라 엄청 버벅여서 false로 변경하고 순위 더보기 누르면 그때 생성하게 변경
        updateUI(false)

		if(this::mClSummaryView.isInitialized) {
			mClSummaryView.post {
				if(activity != null && isAdded){
					val fragment = requireActivity().supportFragmentManager.findFragmentByTag(TAG_SUMMARY) as SummaryMainFragment
					fragment.binding.loadingView.visibility = View.GONE
				}
			}
		}
        lifecycleScope.launch {
            EventBus.receiveEvent<Boolean>(Const.BROADCAST_MANAGER_MESSAGE).collect { result ->
                if (result) {
                    updateDataWithUI(false)
                }
            }
        }
    }

	fun getBitmapFromView(view: View): Bitmap? {
		var bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
		var canvas = Canvas(bitmap)
		view.draw(canvas)
		return bitmap
	}

	fun stopPlayer(){
		playerHandler.removeCallbacksAndMessages(null)
		if (Const.USE_ANIMATED_PROFILE) {
			stopExoPlayer(playerView1)
			stopExoPlayer(playerView2)
			stopExoPlayer(playerView3)
			isPlayingVideo = false
		}
	}

	//7.8.0에서는 largefont에  새  layout을 사용하지 않고, font크기로 조잘
//	fun setLargeFontUi(){
//		if( Util.isLargeFont(activity)) {
//			tv_top1_name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19.5f)
//			tv_top1_group.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f)
//			tv_top1_vote.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
//			tv_top1_rank.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19.5f)
//		}
//	}

	fun setFakeRankingView(){

		try {
			val fakeList : ArrayList<IdolModel> = ArrayList()
			val maxFakeList = 20

			if(!::models.isInitialized) models = ArrayList()
			for (i in 0 until maxFakeList){
				if(i>=models.size || models.size == 0) break
				fakeList.add(models[i])
			}
			val fragment = requireActivity().supportFragmentManager.findFragmentByTag(TAG_SUMMARY) as SummaryMainFragment
			fragment.setFakeRankingView(fakeList, typeList, femaleCategory)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun startAnimationRankingView(){
		val fragment = requireActivity().supportFragmentManager.findFragmentByTag(TAG_SUMMARY) as SummaryMainFragment
		var saveBitmap = BitmapPool.instance.getViewFromBitmapPool(typeName+femaleCategory)
		Logger.v("saveBitmap:"+saveBitmap)
		Logger.v("saveBitmapTypeName:"+typeName)
		setFakeRankingView()
//		var view : View = fragment.getFakeRankingView()
		if (saveBitmap == null) setFakeRankingView()
		var view : View = if(saveBitmap==null) fragment.getFakeRankingView() else fragment.getAnimationView(saveBitmap)
        view.translationY = view.height.toFloat() // 먼저 아래로 보내놓고

        view.post {
            val animator = ValueAnimator.ofFloat(view.height.toFloat(), 0f).setDuration(ANIMATION_DURATION)
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                view.translationY = value
            }
            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    try {
//					setRankingViewIsOn(false)
                        fragment.releaseView()
                        rankingViewAnimationIsOn = false
                        fragmentListener?.setRankingViewAnimationIsOn(rankingViewAnimationIsOn)

                        fragmentListener?.onMoreClick()
                        mClSummaryView.visibility = View.GONE
                        mClRankingView.visibility = View.VISIBLE

                        // 다른 fragment들에게 순위 화면을 그리도록 요청
                        val intent = Intent(Const.REFRESH_SUMMARY_RANKING)
                        LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)

                        Util.log("NewRankingFrag::animation end")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    Util.log("NewRankingFrag::animation cancle")
                }

                override fun onAnimationStart(animation: Animator) {
                    Util.log("NewRankingFrag::animation start")
                    //순위더보기 누르고 바로 백버튼 눌렀을시 앱이 종료되는 상황 방지.
                    setRankingViewIsOn(true)
                    rankingViewAnimationIsOn = true
                    fragmentListener?.setRankingViewAnimationIsOn(rankingViewAnimationIsOn)
                    binding.prevPage.visibility = View.GONE
                    binding.nextPage.visibility = View.GONE
                    updateUI(false) // 버벅임을 유발하지만 이걸 막으면 순위화면 업데이트가 늦게 되어 순위 더보기 누르면 올라온 후에 갱신됨
                    stopPlayer()
                }
            })
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.start()
        }
	}

	fun closeAnimationRankingView(){
		val fragment = requireActivity().supportFragmentManager.findFragmentByTag(TAG_SUMMARY) as SummaryMainFragment
		val view = fragment.getAnimationView(getBitmapFromView(mClRankingView)!!)
		val height = view.height
		val animator = ValueAnimator.ofFloat(0f, view.height.toFloat()).setDuration(ANIMATION_DURATION)
		animator.addUpdateListener {
			val value = it.animatedValue as Float
			try {
				view.translationY = value
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		animator.addListener(object : Animator.AnimatorListener {
			override fun onAnimationRepeat(animation: Animator) {
			}

			override fun onAnimationEnd(animation: Animator) {
				try {
					fragment.releaseView()
                    with(binding) {
                        prevPage.visibility = View.VISIBLE
                        nextPage.visibility = View.VISIBLE
                        prevPage.bringToFront()
                        nextPage.bringToFront()
                    }
					// 다시 펼치기할 때를 위해 원래크기 복구
					view.layoutParams.height = height
					rankingViewAnimationIsOn = false
					fragmentListener?.setRankingViewAnimationIsOn(rankingViewAnimationIsOn)
					//updateTopBanner(idols =models )

					//더보기 애니메이션 끝난후 ExoPlayer재생.
					tryStartPlayer() // startPlayer()를 쓰면 움짤이 퍼덕임
//					startTopBannerPlayer()
//					startPlayer()

				} catch (e: Exception) {
					e.printStackTrace()
				}
			}

			override fun onAnimationCancel(animation: Animator) {
			}

			override fun onAnimationStart(animation: Animator) {
				rankingViewAnimationIsOn = true
				fragmentListener?.setRankingViewAnimationIsOn(rankingViewAnimationIsOn)
				fragmentListener?.onPrevClick()
				mClRankingView.visibility = View.INVISIBLE
				mClSummaryView.visibility = View.VISIBLE
			}

		})
		animator.interpolator = AccelerateDecelerateInterpolator()
		animator.start()
	}

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            fragmentListener = activity as FragmentListener
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + "must implement FragmentListener")
        }
    }

    override fun onPause() {
        super.onPause()

        Util.log("RankingFragment onPause")

        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mBroadcastReceiver)

		mRankingAdapter?.clearAnimation()

		// 움짤 멈추기
		stopPlayer()
    }

    override fun onStop() {
        super.onStop()

        Util.log("NewRankingFragment2 onStop")
    }

    override fun onResume() {
        super.onResume()
        Util.log("NewRankingFragment2 onResume")

		if(rankingViewIsOn()) {
			mClRankingView.visibility = View.VISIBLE
			mClSummaryView.visibility = View.GONE
		} else {
			mClRankingView.visibility = View.INVISIBLE
			mClSummaryView.visibility = View.VISIBLE
		}

        val filter = IntentFilter()

		filter.addAction(Const.REFRESH)
		filter.addAction(Const.REFRESH_SUMMARY_RANKING)
        if (Const.USE_ANIMATED_PROFILE) {
            filter.addAction(Const.PLAYER_START_RENDERING)
        }
		LocalBroadcastManager.getInstance(requireActivity())
			.registerReceiver(mBroadcastReceiver, filter)

        if (Const.USE_ANIMATED_PROFILE) {
            // 움짤 다시 재생
//            Handler().postDelayed({
//				startExoPlayer(playerView1)
//				startExoPlayer(playerView2)
//				startExoPlayer(playerView3)
//				startExoPlayer(playerView4)
//				startExoPlayer(playerView5)
//				startExoPlayer(playerView6)
//				startExoPlayer(playerView7)
//			}, 200)
        }
    }

	override fun onDestroy() {
		super.onDestroy()
		disposable.dispose()
	}

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == getVoteRequestCode()
                && resultCode == BaseDialogFragment.RESULT_OK) {
            val heart = data!!.getLongExtra(VoteDialogFragment.PARAM_HEART, 0)
            if( heart > 0 ) {
                updateDataWithUI(false)

                val eventHeart = data?.getStringExtra(PARAM_EVENT_HEART)
                if (!eventHeart.isNullOrEmpty()) {
                    showEventDialog(eventHeart)
                }

                // 레벨업 체크
                if (data != null) {
                    val idol : IdolModel = data.getSerializableExtra(VoteDialogFragment.PARAM_IDOL_MODEL) as IdolModel
                    val heart = data.getLongExtra(VoteDialogFragment.PARAM_HEART, 0)
                    UtilK.checkLevelUp(baseActivity, accountManager, idol, heart)
                }
            } else {
                Util.closeProgress()
            }
        }
    }

	fun rankingViewIsOn() : Boolean{
		try {
			val fragment = requireActivity().supportFragmentManager.findFragmentByTag(TAG_SUMMARY) as SummaryMainFragment
			return fragment.getRankingViewIsOn()
		}catch (e: Exception){
			e.printStackTrace()
			return false
		}
	}

	fun setRankingViewIsOn(rankingViewIsOn: Boolean){
		val fragment = requireActivity().supportFragmentManager.findFragmentByTag(TAG_SUMMARY) as SummaryMainFragment
		fragment.setRankingViewIsOn(rankingViewIsOn)
	}

	fun rankingViewAnimationIsOn() : Boolean{
		val fragment = requireActivity().supportFragmentManager.findFragmentByTag(TAG_SUMMARY) as SummaryMainFragment
		return fragment.getRankingViewAnimationIsOn()
	}

	//타입에 따른 남녀 구분 할지 안할지 설정하는 함수
	fun typeCheck() : String? {
		if(isDevided == "N" && isFemale==false){
			return null
		}
		else {
			//femaleCategory값이 null로 오는 경우는 남자일 경우라 M값 리턴
			if (femaleCategory == null) {
				return "M"
			}
			//여자일경우 femaleCategory값을 F로 가지고 있어서 그 값 그대로 리턴
			else {
				return femaleCategory as String
			}
		}
	}

	fun updateUI(allUpdate: Boolean) {
		Util.log("*** type=$type")
        lifecycleScope.launch(Dispatchers.IO) {
            val idols = getIdolsByTypeAndCategoryUseCase(type, typeCheck())
                .mapListDataResource { it.toPresentation() }
                .awaitOrThrow()

            idols?.let {
                withContext(Dispatchers.Main) {
                    models.clear()
                    models.addAll(sort(requireContext(), ArrayList(it)))
                    weakActivity?.get()?.runOnUiThread {
                        try {
                            if (allUpdate) {
                                applyItems(models)
                                updateAllSummary(models)
                            } else {
                                if (rankingViewIsOn()) {
                                    applyItems(models)
                                } else {
                                    updateAllSummary(models)
                                }

                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
	}

    fun updateDataWithUI(allUpdate: Boolean) {
        Thread( object: Runnable {
            override fun run() {
                try {
                    var type_: String? = if (typeList.type.isNullOrEmpty()) null else typeList.type
                    getIdols(models, type_ ?: "", typeCheck(), displayErrorHandler)
                    if( models.size == 0) {
                        val a=1
                    }

                    weakActivity?.get()?.runOnUiThread {
                        try {
                            if (allUpdate) {
                                applyItems(models)
                                updateAllSummary(models)
                            } else {
                                if (rankingViewIsOn()) {
                                    applyItems(models)
                                }
                                else{
                                    thumbnailLoaded.clear()
                                    updateAllSummary(models)
                                }

                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }).start()
    }

	private fun updateTopBanner(idols: ArrayList<IdolModel>){

		if(idols.isNullOrEmpty()){
			return
		}
		topBannerMediaUrl1=idols[0].imageUrl
		topBannerMediaUrl2=idols[0].imageUrl2
        topBannerMediaUrl3=idols[0].imageUrl3

        if (idols[0].getName(context).contains("_")) {
            binding.tvTop1Name.text = Util.nameSplit(context, idols[0])[0]
            binding.tvTop1Group.visibility = View.GONE

			if (idols[0].getName(context).contains("_")) {
                binding.tvTop1Group.text = Util.nameSplit(context, idols[0])[1]
			} else {
                binding.tvTop1Group.visibility = View.GONE
			}
		} else {
            binding.tvTop1Name.text = idols[0].getName(context)
            binding.tvTop1Group.visibility = View.GONE
		}


		//투표 카운트 애니메이션 업데이트
		startCountAnimation(binding.tvTop1Vote,oldCountMap[0], idols[0].heart,0)
		binding.top1CardView.setOnClickListener {
			if (!idols.isNullOrEmpty()) weakActivity.get()?.let { it1 ->
				CommunityActivity.createIntent(
					it1,
					idols[0]
				)
			}?.let { it2 ->
				startActivity(
					it2
				)
			}
		}

        val topBannerMediaUrls = arrayOf(topBannerMediaUrl1, topBannerMediaUrl2, topBannerMediaUrl3)

        for(i in 0 until 3){
            val mediaUrl = topBannerMediaUrls[i]
            val thumbnailUrl: String? = mediaUrl?.replace(".mp4", ".webp")

			val target = object : CustomTarget<Drawable>() {
				override fun onLoadCleared(placeholder: Drawable?) {
				}

				override fun onResourceReady(
					resource: Drawable,
					transition: Transition<in Drawable>?
				) {
					ivMapForTopBanner[i]?.setImageDrawable(resource)
                    thumbnailLoaded[i] = true
					tryStartPlayer()
				}

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    thumbnailLoaded[i] = true
                    tryStartPlayer()
                }

			}

            if(mediaUrl.isNullOrEmpty()){
				mGlideRequestManager.load(Util.noProfileThemePickImage(models[0].getId())).into(target)
            } else{
                // 썸네일 뷰에 썸네일 URL을 사용하여 이미지 로드. 기존에는 mp4 url을 잘못 넘기고 있었음.
                mGlideRequestManager.load(thumbnailUrl)
                    .error(R.color.gray150)
                    .fallback(R.color.gray150)
                    .placeholder(R.color.gray150)
                    .dontAnimate()
                    .into(target)
            }
		}
	}
	private fun startCountAnimation(tvVoteCount:TextView?,oldCount: Long?,newUpdateCount: Long?,position: Int) {
		try {
			if( oldCount != newUpdateCount && Util.getPreferenceBool(context, Const.PREF_ANIMATION_MODE, false)) {

				val animator = ValueAnimator.ofFloat(0f, 1f)	//ofInt로 되어있다가 21억표 이상 갈 경우 처리가 안되어서, ofFloat을 이용하여 큰 숫자더라도 근사치값 표현
				oldCountMap[position] = newUpdateCount
				animator.addUpdateListener {
					try {
						val value = round(oldCount!! + (newUpdateCount!! - oldCount) * (it.animatedValue as Float))	//소수점 아래 1번째에서 반올림
						tvVoteCount?.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(value)

					}catch (e:java.lang.Exception){
						e.printStackTrace()
					}
				}
				animator?.addListener(object : Animator.AnimatorListener {
					override fun onAnimationRepeat(animation: Animator) {
					}

					override fun onAnimationCancel(animation: Animator) {
					}

					override fun onAnimationStart(animation: Animator) {
					}

					override fun onAnimationEnd(animation: Animator) {	//애니메이션 끝나면 실제 표수 보여줌. 숫자가 크면 근사치값만 보여지기 때문에 애니메이션 끝나고 처리 필요
						val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault()).format(newUpdateCount)
						tvVoteCount?.text = voteCountComma
					}
				})
				animator.duration = 800 //Duration is in milliseconds
				animator.start()
			}
			else{
				tvVoteCount?.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(newUpdateCount)
			}
		}catch (e:Exception){//만약에 애니메이션 문제가 있는 경우는 기존대로  투표수만 업데이트해줌.
			e.printStackTrace()

			tvVoteCount?.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(newUpdateCount)
		}
	}

	fun tryStartPlayer() {
		// 현재 화면에 보여지는 fragment 일 때에만
		val fragment = requireActivity().supportFragmentManager.findFragmentByTag(TAG_SUMMARY) as SummaryMainFragment
		// summary 화면이 아니면 pass
		if( !fragment.isVisible ) {
			return
		}
		if( fragment.getCurrentFragment() != this ) {
			return
		}
		if( isPlayingVideo )
			return

		for(i in 0 until 3){
			if( thumbnailLoaded[i] != true ) {
				return
			}
		}

        isPlayingVideo = true

		Logger.i("*** tryStartPlayer()")

		startPlayer()
//		startTopBannerPlayer()
	}

    // 1등과 2~5등 모두 업데이트
    fun updateAllSummary(idols: ArrayList<IdolModel>) {
        val prevImageUrls = duplCheckMap.clone()
        updateTopBanner(idols)

        // 순위변화나 1위 탑3에 변동이 생기면 새로 재생
        if( !prevImageUrls.equals(duplCheckMap) ) {
            Util.log("*** 프사/순위 변화가 있음.")
            startPlayer()
        }

		//밑에다 넣어준이유는 만약 try-catch에 걸려버리게되면 아래로직이 실행안되므로 맨밑에다가 넣어줌.
		updateSummaryRanking(idols)
    }

	private fun updateSummaryRanking(idols: ArrayList<IdolModel>) {
        Util.log("idols.size=${idols.size}")
        // 최대 10개까지 보여주기
        val count = min(idols.size, MAX_ITEMS)
        val selected = ArrayList(idols.take(count))
        top10RankingAdapter?.setItems( selected )
        top10RankingAdapter?.notifyDataSetChanged()
	}

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)

        // 최애가 그룹인 경우 뷰 생성 전에 이쪽을 타서 방어
        if( !isAdded() ) {
            // 앱 시작 후 방치시 자동갱신 안되서
            if(isVisible) {
                startTimer();
            }
            return
        }

		val fragment = requireActivity().supportFragmentManager.findFragmentByTag(TAG_SUMMARY) as SummaryMainFragment
		if(!fragment.userVisibleHint){
			return
		}

        // 펼친거 다시 닫기
        mapExpanded.clear()
        mRankingAdapter?.clearMapExpanded()

        if (isVisible) {
        	// 빠른 페이지 이동시 움짤 재생 안됨 방지
			isPlayingVideo = false
//			if(!rankingViewIsOn()) {
//				var saveBitmap = BitmapPool.getInstance().getViewFromBitmapPool(type)
//				if (saveBitmap == null && fragIsVisible && !rankingViewAnimationIsOn)
//					setFakeRankingView()
//			}
			if(!rankingViewIsOn()){ //랭킹뷰 안보일떄.
                // 랭킹 뷰를 그린적이 없으면 1회 그려서 순위 더보기할 때 빈화면을 방지
                if( mRankingAdapter?.itemCount == 0 ) {
                    applyItems(models)
                }
                // 여긴 처음 시작시엔 안불리고 좌우 페이징하거나 다른 화면 갔다오면 불림
                tryStartPlayer() // startPlayer()를 쓰면 움짤이 퍼덕임

//				startPlayer()
//				startTopBannerPlayer()
			}
			updateDataWithUI(false)

            // 다른탭 갔다오면 1위 움짤프사 다시 재생되게
            mRankingAdapter?.hasExpanded = false
			Util.log("visibility changed was called")
            startTimer()
        } else {
            stopTimer()
			stopPlayer()
        }
    }

	fun startPlayer(){
		// 좌우 왔다갔다 할 때 중복재생됨 방지
		playerHandler.removeCallbacksAndMessages(null)
		playerHandler.postDelayed({
			try {
				val urls = arrayOf(topBannerMediaUrl1, topBannerMediaUrl2, topBannerMediaUrl3)
				for (i in 0 until 3) {
					playExoPlayer(
							i,
							playerMapForTopBanner[i],
							ivMapForTopBanner[i]!!,
							urls[i]
					)
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}, 300)
	}

    fun startTimer() {
		Util.log("startTimter was called")
        if( timerRunnable != null ) {
            timerHandler?.removeCallbacks(timerRunnable!!)
        }

        timerRunnable = Runnable {
            try {
				Util.log("UpdateUi was called")
                updateDataWithUI(false)
            } finally {
                timerHandler?.postDelayed(timerRunnable!!, refreshInterval*1000)
            }
        }

        timerHandler = Handler()
        timerHandler?.postDelayed(timerRunnable!!, refreshInterval*1000)
        Util.log("*** startTimer "+this)
    }

    fun stopTimer() {
		Util.log("stopTimer was called")
        if( timerRunnable != null ) {
            timerHandler?.removeCallbacks(timerRunnable!!)
        }
    }

    private fun applyItems(items: ArrayList<IdolModel>) {
        if(!items.isNullOrEmpty()) {
            hideEmptyView()
			mRankingAdapter?.setItems(items)
        }
    }

    private fun showEmptyView() {
        mEmptyView.visibility = View.VISIBLE
        rvRanking.visibility = View.GONE
    }

    private fun hideEmptyView() {
        mEmptyView.visibility = View.GONE
        rvRanking.visibility = View.VISIBLE
    }

    private fun showEventDialog(eventHeart: String) {
        eventHeartDialog = Dialog(requireActivity(), android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        eventHeartDialog.window!!.attributes = lpWindow
        eventHeartDialog.window!!.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)

        val btnOk: AppCompatButton = eventHeartDialog.findViewById(R.id.btn_ok)
        val msg: AppCompatTextView = eventHeartDialog.findViewById(R.id.message)

        eventHeartDialog.setContentView(R.layout.dialog_surprise_heart)
        eventHeartDialog.setCanceledOnTouchOutside(false)
        eventHeartDialog.setCancelable(true)
        btnOk.setOnClickListener { eventHeartDialog.cancel() }
        msg.text = String.format(getString(R.string.msg_surprise_heart), eventHeart)
        eventHeartDialog.window!!
                .setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        eventHeartDialog.show()
    }

    private fun showError(title: String, text: String?) {
        Util.showDefaultIdolDialogWithBtn1(activity,
                title,
                text
        ) { Util.closeIdolDialog() }
    }

    open fun onFragmentSelected() {}

    private fun openCommunity(idol: IdolModel) {
        if (Util.mayShowLoginPopup(activity) || activity == null) return

        startActivity(CommunityActivity.createIntent(requireActivity(), idol))
    }

    private fun voteHeart(idol: IdolModel, totalHeart: Long, freeHeart: Long) {
        val dialogFragment = VoteDialogFragment.getIdolVoteInstance(idol, totalHeart, freeHeart)
        dialogFragment.setTargetFragment(this, getVoteRequestCode())
        dialogFragment.show(requireFragmentManager(), "vote")
    }

    override fun onItemClicked(item: IdolModel?) {
        if(item == null){
            fragmentListener?.onPrevClick()
        }
        else {
            openCommunity(item)
        }
    }

    override fun onVote(item: IdolModel) {
        if (Util.mayShowLoginPopup(baseActivity)) {
            return
        }

		setUiActionFirebaseGoogleAnalyticsFragment(GaAction.VOTE.actionValue, GaAction.VOTE.label)

        Util.showProgress(activity)
        lifecycleScope.launch {
            usersRepository.isActiveTime(
                { response ->
                    Util.closeProgress()

                    if (response.optBoolean("success")) {
                        val gcode = response.optInt("gcode")
                        if (response.optString("active") == Const.RESPONSE_Y) {
                            if (response.optInt("total_heart") == 0) {
                                Util.showChargeHeartDialog(activity)
//                            Util.showDefaultIdolDialogWithBtn2(activity,
//                                    null,
//                                    getString(R.string.msg_go_to_add_heart),
//                                    {
//                                        Util.closeIdolDialog()
//                                        startActivity(HeartPlusActivity.createIntent(activity))
//                                    },
//                                    { Util.closeIdolDialog() })
                            } else {
                                if (response.optString("vote_able").equals(Const.RESPONSE_Y, ignoreCase = true)) {
                                    voteHeart(item, response.optLong("total_heart"), response.optLong("free_heart"))
                                } else {

                                    if (gcode == Const.RESPONSE_IS_ACTIVE_TIME_1) {
                                        Toast.makeText(activity, getString(R.string.response_users_is_active_time_over), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(activity, getString(R.string.msg_not_able_vote), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            val start = Util.convertTimeAsTimezone(response.optString("begin"))
                            val end = Util.convertTimeAsTimezone(response.optString("end"))
                            val unableUseTime = String.format(
                                getString(R.string.msg_unable_use_vote), start, end)
                            Util.showIdolDialogWithBtn1(activity,
                                null,
                                unableUseTime) { Util.closeIdolDialog() }
                        }
                    } else { // success is false!
                        UtilK.handleCommonError(activity, response)
                    }
                }, {
                    Util.closeProgress()
                    Toast.makeText(activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // 프사 눌러 펼치기
    override fun onPhotoClicked(item: IdolModel, position: Int) {
        rvRanking.postDelayed({
            // get clicked item view
            val targetView = rvRanking.findViewHolderForAdapterPosition(position)?.itemView
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
                rvRanking.getLocationInWindow(location)
                val listviewTop = location[1]
                val listviewBottom = listviewTop + rvRanking.height
                // check if target bottom is under listview's bottom
                if (targetBottom > listviewBottom) {
                    rvRanking.smoothScrollBy(targetBottom - listviewBottom + viewHeight, 200)
                }
            }
        }, 300)
    }

    override fun updateTutorialIndex(index: Int) {
        lifecycleScope.launch {
            usersRepository.updateTutorial(
                tutorialIndex = index,
                listener = { response ->
                    if (response.optBoolean("success")) {
                        Logger.d("Tutorial updated successfully: $index")
                        val bitmask = response.optLong("tutorial", 0L)
                        TutorialManager.init(bitmask)
                    } else {
                        android.widget.Toast.makeText(requireContext(), response.toString(), android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                errorListener = { throwable ->
                    android.widget.Toast.makeText(requireContext(), throwable.message ?: "Error updating tutorial", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // FIXME 셀럽 메인에 뷰모델로 이동
    @Throws(ExecutionException::class, InterruptedException::class)
    fun getIdols(
        models: ArrayList<IdolModel>,
        type: String,
        category: String?,
        displayErrorHandler: Handler?,
    ) {
        var category = category
        var response: JSONObject? = null

        if (TextUtils.isEmpty(category)) {
            category = null
        }
        val realCategory = category

        val cacheKey =
            ((if (type.equals("S", ignoreCase = true)) Const.KEY_IDOLS_S else Const.KEY_IDOLS_G)
                + (realCategory ?: ""))

        if (models.isEmpty()) {
            v(response.toString() + "확인 ")
            if (models.isEmpty()) {
                lifecycleScope.launch {
                    idolsRepository.getIdols().collect {
                        it.data?.let { jsonObject ->
                            val success = jsonObject.optBoolean("success")
                            if(success) {
                                handleResponse(
                                    models,
                                    type,
                                    realCategory,
                                    cacheKey,
                                    jsonObject
                                )
                            } else {
                                showError(displayErrorHandler, jsonObject)
                            }
                        }
                    }
                }
            }
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                val idols = getIdolsByTypeAndCategoryUseCase(type, category)
                    .mapListDataResource { it.toPresentation() }
                    .awaitOrThrow()
                idols?.let {
                    withContext(Dispatchers.Main) {
                        models.clear()
                        models.addAll(sort(requireContext(), ArrayList(it)))
                    }
                }
            }
        }
    }

    // FIXME 셀럽 메인에 뷰모델로 이동
    @Synchronized
    private fun handleResponse(
        models: ArrayList<IdolModel>,
        type: String,
        category: String?,
        cacheKey: String,
        response: JSONObject
    ) {
        if (activity == null) return

        if (UtilK.isDayChanged(response.optString("server_time"), requireActivity())) {
            // 미션 초기화
            Util.setPreference(baseActivity, Const.PREF_MISSION_COMPLETED, false)
        }

        try {
            Util.setPreference(
                baseActivity,
                Const.PREF_ALL_IDOL_UPDATE,
                response.optString(Const.PREF_ALL_IDOL_UPDATE)
            )
            Util.setPreference(
                baseActivity,
                Const.PREF_DAILY_IDOL_UPDATE,
                response.optString(Const.PREF_DAILY_IDOL_UPDATE)
            )

            val gson = instance
            val idolList = response.optJSONArray("objects").toString()

            v(idolList + "확인 ")
            Util.setPreference(
                baseActivity,
                cacheKey,
                System.currentTimeMillis() + Const.COOLDOWN_TIME
            )

            val listType = object : TypeToken<List<IdolModel?>?>() {
            }.type
            val idols = gson.fromJson<ArrayList<IdolModel>>(idolList, listType)

            lifecycleScope.launch(Dispatchers.IO) {
                saveIdolsUseCase(idols.map { it.toDomain() }).first()

                val dbItems = getIdolsUseCase()
                    .mapListDataResource { it.toPresentation() }
                    .awaitOrThrow()

                val test = sort(requireContext(), ArrayList(dbItems))

                dbItems?.let {
                    withContext(Dispatchers.Main) {
                        models.clear()
                        models.addAll(sort(requireContext(), ArrayList(dbItems)))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // FIXME 셀럽 메인에 뷰모델로 이동
    private fun showError(displayErrorHandler: Handler?, result: JSONObject?) {
        if (displayErrorHandler != null && result != null) {
            val responseMsg = ErrorControl.parseError(activity, result) ?: return
            val msg = displayErrorHandler.obtainMessage()
            msg.what = 0
            msg.arg1 = 0
            msg.obj = responseMsg
            displayErrorHandler.sendMessage(msg)
        }
    }

    companion object {
        const val PARAM_EVENT_HEART = "paramEventHeart"
    }
}
