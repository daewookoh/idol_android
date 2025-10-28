package net.ib.mn.fragment

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.reflect.TypeToken
import com.unity3d.services.core.misc.Utilities.runOnUiThread
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.adapter.NewRankingFakeAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.awards.IdolAwardsActivity
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.model.AwardModel
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.databinding.FragmentSummaryMainBinding
import net.ib.mn.domain.usecase.GetIdolsByTypeAndCategoryUseCase
import net.ib.mn.liveStreaming.LiveStreamingListActivity
import net.ib.mn.model.*
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.*
import net.ib.mn.utils.Const.KEY_CATEGORY
import net.ib.mn.utils.Const.MAIN_CHECK_VIEW
import net.ib.mn.utils.ExtendedDataHolder.Companion.getInstance
import net.ib.mn.utils.ext.getUiColor
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: 셀럽 첫 화면애 랭킹 스와이프 화면 컨테이너
 *
 * */

@AndroidEntryPoint
@UnstableApi
class SummaryMainFragment : BaseFragment(), OnScrollToTopListener {

    @Inject
    lateinit var getIdolsByTypeAndCategoryUseCase: GetIdolsByTypeAndCategoryUseCase

    @Inject
    lateinit var usersRepository: UsersRepository

	private var maxPages : Int = 0
	private var rankingViewIsOn = false
	private var margin = 0
	protected var mRankingAdapter: NewRankingFakeAdapter? = null
//	private lateinit var mGlideRequestManager: RequestManager
	private var rankingViewAnimationIsOn = false
	private var weakActivity : WeakReference<FragmentActivity>? = null
	private var models : ArrayList<IdolModel> = arrayListOf()
	private var femaleCheck : Boolean = false
	private var sharedPosition : Int = 0
    private var realType : String =""
	private var tempType : String = ""

    public lateinit var binding: FragmentSummaryMainBinding

	override fun onAttach(context: Context) {
		super.onAttach(context)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
//		mGlideRequestManager = GlideApp.with(this)
		weakActivity = WeakReference<FragmentActivity>(requireActivity())
        setFirebaseScreenViewEvent(GaAction.RANKING_OVERALL, this::class.simpleName)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
        binding = FragmentSummaryMainBinding.inflate(inflater, container, false)
        return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val gson2 = IdolGson.getInstance()
		val listType2 = object : TypeToken<ArrayList<IdolTypeModel>>() {}.type

		if((!Util.getPreference(context, MAIN_CHECK_VIEW).isNullOrEmpty())) {
			idolTypeList = gson2.fromJson(Util.getPreference(context, MAIN_CHECK_VIEW), listType2)
		}

		try {
            val most = IdolAccount.getAccount(activity)?.most
			realType  = most?.type + most?.category
			tempType = most?.type ?: ""	// 종합, 예능, 댄서는 category값이 다 다르게 와서 type은 남녀로 안나눈 한개뿐이니까 realtype말고 새로 하나 만듬
		}catch (e:Exception){
			e.printStackTrace()
		}


		var mostPosition = 0

		//최애 타입이 뭔지 체크해서 그 값만 true로 변경
		for(i in 0 until idolTypeList.size){

			val category = UtilK.getTypeCheck(idolTypeList[i].is_divided, idolTypeList[i].isFemale)

			if (idolTypeList[i].is_divided == "N" && !idolTypeList[i].isFemale) {	//종합, 예능, 댄서 일 때
				if (tempType == idolTypeList[i].type) {
					idolTypeList[i].most = true
				}
			} else {	//가수, 배우일 떄
				if(realType==idolTypeList[i].type + category){
					idolTypeList[i].most = true
				}
			}
			//카테고리 체크가 되어있는 것만 따로 어레이리스트 생성
			if(idolTypeList[i].checkFilter){
				checkTrueList.add(idolTypeList[i])
			}
		}
		//모스트가 있는지 체크하고, 없으면 맨 앞 페이지, 있으면 모스트 있는 인덱스 위치 구함
		if(checkTrueList.any { it.most }){
			if(checkTrueList.count { it.most } == 0){
				mostPosition = 0
			}
			else {
				mostPosition = checkTrueList.indexOf(idolTypeList.find { it.most })
			}
		}
		checkTrueList.clear()


		// 자신만의 fragmentmanager를 갖자!
		mSummaryAdapter = SummaryPagerAdapter(childFragmentManager)

		binding.rankingPager.clipToPadding = false
		val dpValue = 50 // FHD폰, 갤S9 해상도 양쪽 모두 적절히 만족하는 값. 40은 넥서스5x에서 양쪽 카드가 안보이고 너무 크면 카드가 좁아짐.
		val d = resources.displayMetrics.density
		margin = (dpValue * d).toInt()
		binding.rankingPager.setPadding((margin / 1.5).toInt(), 0, (margin / 1.5).toInt(), 0)
		binding.rankingPager.pageMargin = 0//margin / 3

		val typeList = Util.getPreference(context, Const.PREF_TYPE_LIST)
		val gson = IdolGson.getInstance()
		val listType = object : TypeToken<List<TypeListModel>>() {}.type
		var types = gson.fromJson<ArrayList<TypeListModel>>(typeList, listType)
		types = types.filter { it.isViewable == "Y" } as ArrayList<TypeListModel>?
		if (types.isNotEmpty()) {
			maxPages = types.size * 2

			for (i in 0 until types.size) {
					Logger.v("type::${gson.toJson(types[i])}")
					val bundle = Bundle()
					val bundle2 = Bundle()
					bundle.putString("type", gson.toJson(types[i]))

					var summaryFragment : Fragment
					var idolCheckFragment : Fragment

					//체크박스 트루인 것만 보여줌
					if(idolTypeList[i].checkFilter){
						bundle.putInt(MAIN_INDEX, i)
						bundle.putString("type", gson.toJson(types[i]))

						//타입이 여자일 경우, 키값 넣어줌
						if(types[i].isFemale){
							bundle.putString(KEY_CATEGORY, "F")
						}

						summaryFragment = RankingPageFragment()
						summaryFragment.arguments = bundle
						mSummaryAdapter?.add(summaryFragment)
					}
					//for문 다 돌면 마지막에 카테고리 필터 Fragment 추가
					if(i == types.size-1){
						idolCheckFragment = IdolShowCheckFragment()
						bundle2.putString("type", gson.toJson(types[i]))
						bundle2.putInt(MAIN_INDEX, i + 1) //가장자미막은 RankingPage꺼라서 인덱스는 +1 해줘야됨.
						idolCheckFragment.arguments = bundle2
						mSummaryAdapter?.add(idolCheckFragment)
					}
				}
			binding.rankingPager.offscreenPageLimit = maxPages
			binding.rankingPager.adapter = mSummaryAdapter
			mSummaryAdapter?.notifyDataSetChanged()

			try {
				//랭킹 뷰페이져  시작  나의  최애 타입으로  설정함.
				sharedPosition = mostPosition
			}catch (e: NullPointerException){//맨처음  회원가입하고 들어갈때,  type null 떠서  try catch  적용
                 e.printStackTrace()
			}
			binding.rankingPager.setCurrentItem(sharedPosition, false)

			binding.rankingPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
				private var scrollState = 0
				private var targetPage = 0

				override fun onPageScrollStateChanged(state: Int) {
					if (scrollState == ViewPager.SCROLL_STATE_SETTLING) {
						sharedPosition = targetPage
					}
					scrollState = state
					Logger.v("ranking pager::${sharedPosition}")

				}

				//스크롤 중 값
				override fun onPageScrolled(
					position: Int,
					positionOffset: Float,
					positionOffsetPixels: Int
				) {

					//순위 더보기 가 올라가져있고 , 필터 fragment  바로전 fragment 일때
					//오른쪽  스와프가 되는 것을 막아  필터 fragment로 이동하는것을 막는다.
					if(position == mSummaryAdapter!!.count-2 && rankingViewIsOn){

						//뷰페이저를 손가락 터치를 유지하면서 넘길려고 하면  positionOFFset이  0.8 이상일때 다음  position으로 넘어가는 경우가 있음
						// 다음으로 넘어가면 필터 fragment이므로  다시 현재 fragment로  current item으로 set해준다.
						if(positionOffset >0.8){
							binding.rankingPager.currentItem = position
						}
						binding.rankingPager.setRightSwipeAllow(false)
					}

					mSummaryAdapter?.apply {
						if(position == count -2) { //필터 이전페이지에서 버튼없애줌.
                            val frag = fragments[count - 1]
							if(frag is IdolShowCheckFragment){ //필터페이지 확인.
								val manager = frag.binding.rvShowCheck.layoutManager as LinearLayoutManager
								val totalItemCount = manager.itemCount
								val top = manager.findFirstCompletelyVisibleItemPosition()
								val bottom = manager.findLastCompletelyVisibleItemPosition()
								if ((bottom - top) == totalItemCount - 1) { //리사이클러뷰 맨아래 아이템 인덱스와 위에 인덱스를 빼줘서 총합이 전체아이템이 같을경우엔 전체가보임.
                                    frag.binding.imgArrowState.visibility = View.GONE
								}
							}
						}
					}
				}

				//스크롤된 후 값
				override fun onPageSelected(position: Int) {

					//순위 더보기 가 올라가져있고 , 필터 fragment  바로전 fragment 일때
					//오른쪽  스와프가 되는 것을 막아  필터 fragment로 이동하는것을 막는다.
					if(position == mSummaryAdapter!!.count-2 && rankingViewIsOn){
						binding.rankingPager.setRightSwipeAllow(false)
					}else{
						binding.rankingPager.setRightSwipeAllow(true)//오른쪽 스와이프 다시 가능하게
					}

                    if (position == 0) {
                        setFirebaseScreenViewEvent(GaAction.RANKING_OVERALL, this::class.simpleName)
                    }

					targetPage = position
				}

			})

			binding.rankingPager.setOnTouchListener(object : View.OnTouchListener {
				override fun onTouch(v: View?, event: MotionEvent?): Boolean {
					return rankingViewAnimationIsOn
				}
			})

//			var gestureListener = MyGesture()
//			var gesturedetector = GestureDetector(context, gestureListener)
//
//			prev_page.setOnTouchListener { v, event ->
//				return@setOnTouchListener gesturedetector.onTouchEvent(event)
//			}

			binding.rankingPager.postDelayed({
				Logger.v("Banner:: firstStart")
				// 현재 infiniteViewPager제거로 switcher는 필요없게됨 그러므로 어댑터에있는 fragment를 직접 가져옴.
				val fragment = mSummaryAdapter!!.fragments[sharedPosition]

				if (fragment is RankingPageFragment) {
					fragment.firstStart()
					Logger.v("Banner::firstStart is success")
				}
			}, 300)
		}

		val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
		divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)

		binding.rvRanking.layoutManager = LinearLayoutManagerWrapper(
			requireContext(),
			LinearLayoutManager.VERTICAL,
			false
		)

		mRankingAdapter?.setHasStableIds(true)
		binding.rvRanking.addItemDecoration(divider)

        lifecycleScope.launch(Dispatchers.IO) {
            val dbItems = getIdolsByTypeAndCategoryUseCase(null, null)
                .mapListDataResource { it.toPresentation() }
                .awaitOrThrow()

            dbItems?.let {
                withContext(Dispatchers.Main) {
                    models.clear()
                    models.addAll(sort(requireContext(), ArrayList(it)))

                    mRankingAdapter = NewRankingFakeAdapter(
                        weakActivity!!.get()!!,
                        mGlideRequestManager,
                        models,
                        types[0]
                    )

                    val tmp = Date().time
                    val formatter = SimpleDateFormat("yyyy. MM. dd ", Locale.getDefault())
                    val date = formatter.format(tmp)

                    try {
                        binding.rvRanking?.adapter = mRankingAdapter
                        binding.tvTitle?.text = types[0].name

                        binding.tvDate?.text = date + requireContext().getString(R.string.ranking)

                        val bgShape_ranking = binding.layoutBgRanking?.getBackground() as GradientDrawable?
                        bgShape_ranking?.setColor(
                            Color.parseColor(
                                types[0].getUiColor(requireContext()).toString()
                            )
                        )
                    }	catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            }
        }

		if((ConfigModel.getInstance(requireActivity()).showAwardTab)) {
			try {
                val awardData = Json{ignoreUnknownKeys = true}.decodeFromString<AwardModel>(Util.getPreference(requireContext(), Const.AWARD_MODEL))
				mGlideRequestManager.load(
                    awardData.mainFloatingImgUrl
				).into(object : CustomTarget<Drawable?>() {

					override fun onResourceReady(
						@NonNull resource: Drawable, transition: Transition<in Drawable?>?
					) {
						binding.btnSoba.background = resource
					}

					//이미지  로드를 재대로 못햇을때 기존  Drawable을 이용해서  어워드 타입에 맞춰 보여준다. -> 실패시 안보여주는게 나을듯 (엉뚱한 어워즈 이미지 나옴)
					override fun onLoadCleared(placeholder: Drawable?) {
						try {
							binding.btnSoba.visibility = View.GONE
						}catch (e:Exception){
							e.printStackTrace()
						}
					}
				})
			} catch (e: Exception) {
				e.printStackTrace()
			}

			// soba
			if (ConfigModel.getInstance(context).showAwardTab) {
				binding.btnSoba.visibility = View.VISIBLE
				binding.btnSoba.setOnClickListener {
                    if (binding.btnSoba.isVisible) return@setOnClickListener
					val i = Intent(context, IdolAwardsActivity::class.java)
					activity?.startActivity(i)
				}
				binding.btnSoba.postDelayed({
					animateSoba()
				}, 1000)
			} else {
				binding.btnSoba.visibility = View.GONE
			}
		}

		// live탭 보여주기 true 일때
		if (ConfigModel.getInstance(context).showLiveStreamingTab) {
			binding.btnLive.visibility = View.VISIBLE
			binding.btnLive.setOnClickListener {
                if (binding.lottieTutorialLive.isVisible) return@setOnClickListener
				val i = Intent(context, LiveStreamingListActivity::class.java)
				activity?.startActivity(i)
			}

		} else {
			binding.btnLive.visibility = View.GONE
		}

        binding.ivMission.setOnClickListener {
            val dialog = CelebWelcomeMissionDialog()
            dialog.show(parentFragmentManager, "WelcomeMissionDialog")
        }

        setTutorial()
	}

    private fun setTutorial() {
        when (TutorialManager.getTutorialIndex()) {
            CelebTutorialBits.MAIN_PLAY -> {
                if (binding.btnLive.isVisible) {
                    setupLottieTutorial(binding.lottieTutorialLive) {
                        updateTutorial(TutorialManager.getTutorialIndex())
                        binding.btnLive.callOnClick()
                    }
                }
            }
            CelebTutorialBits.MAIN_AWARDS -> {
                if (binding.btnSoba.isVisible) {
                    setupLottieTutorial(binding.lottieAwardCeleb) {
                        updateTutorial(TutorialManager.getTutorialIndex())
                        binding.btnSoba.callOnClick()
                    }
                }
            }
        }
    }

	//다른 탭 갔다왔을 때 배너가 움직이지 않아 넣음
	override fun setMenuVisibility(menuVisible: Boolean) {
		super.setMenuVisibility(menuVisible)
		//화면이 보였을 때
		if(menuVisible){
			try{
				if(mSummaryAdapter != null && mSummaryAdapter!!.count > 0){
					if(mSummaryAdapter?.fragments!![sharedPosition] is RankingPageFragment){
						Logger.v("menuVisibility:: is adapter startPlayer")
						val frag = (mSummaryAdapter?.fragments!![sharedPosition] as RankingPageFragment)
						frag.startPlayer()
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		//화면이 안보였을 때
		else{
			try{
				if(mSummaryAdapter != null && mSummaryAdapter!!.count > 0){
					if(mSummaryAdapter?.fragments!![sharedPosition] is RankingPageFragment){
						val frag = (mSummaryAdapter?.fragments!![sharedPosition] as RankingPageFragment)
						frag.stopPlayer()
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	override fun onResume() {
		super.onResume()
		val extendedDataHolder = getInstance()
		if (extendedDataHolder.hasExtra("bannerList")) {
			val fronbannerlist =
				extendedDataHolder.getExtra("bannerList") as java.util.ArrayList<FrontBannerModel?>?

			Logger.v(fronbannerlist.toString())
		}
		if(isVisible && isAdded && userVisibleHint){
			try{
				if (mSummaryAdapter != null && mSummaryAdapter!!.count > 0) {
					if (mSummaryAdapter?.fragments!![sharedPosition] is RankingPageFragment) {
						val frag = (mSummaryAdapter?.fragments!![sharedPosition] as RankingPageFragment)

						if (frag.userVisibleHint) {
							frag.startTimer()
						}
					}
				}
			}catch (e: Exception){
				e.printStackTrace()
			}
		}else{
			try{
				if (mSummaryAdapter != null && mSummaryAdapter!!.count > 0) {
					if (mSummaryAdapter?.fragments!![sharedPosition] is RankingPageFragment) {
						val frag = (mSummaryAdapter?.fragments!![sharedPosition] as RankingPageFragment)

						if (frag.userVisibleHint) {
							frag.stopTimer()
						}
					}
				}
			}catch (e: Exception){
				e.printStackTrace()
			}
		}
	}

	fun animateSoba() {
        // android 4.x에서 크래시나서 막음

        var animator = ObjectAnimator.ofFloat(binding.btnSoba, "rotationY", 0f, 180f)
        animator.duration = 300
        animator.start()
        animator.addListener(object : AnimatorListener {
			override fun onAnimationRepeat(animation: Animator) {
			}

			override fun onAnimationEnd(animation: Animator) {
				animator = ObjectAnimator.ofFloat(binding.btnSoba, "rotationY", 180f, 0f)
				animator.duration = 300
				animator.start()
			}

			override fun onAnimationCancel(animation: Animator) {
			}

			override fun onAnimationStart(animation: Animator) {
			}
		})

        binding.btnSoba?.let {
            binding.btnSoba?.postDelayed({
				animateSoba()
			}, 2000)
        }
    }

	fun onNextClick(){
		binding.rankingPager.setCurrentItem(sharedPosition + 1, true)
	}

	fun onPrevClick(){
		binding.rankingPager.setCurrentItem(sharedPosition - 1, true)
	}

	fun setRankingViewAnimationIsOn(flag: Boolean){
		rankingViewAnimationIsOn = flag
	}

	fun getRankingViewIsOn() : Boolean{
		return rankingViewIsOn
	}

	fun setRankingViewIsOn(rankingViewIsOn: Boolean){
		this.rankingViewIsOn = rankingViewIsOn
	}

	fun getRankingViewAnimationIsOn() : Boolean{
		return rankingViewAnimationIsOn
	}

//	inner class MyGesture : GestureDetector.OnGestureListener {
//
//		override fun onShowPress(e: MotionEvent?) {
//			Util.log("****************************** onShowPress")
//			summary_pager.
//		}
//		override fun onSingleTapUp(e: MotionEvent?): Boolean {
//			Util.log("****************************** onSingleTapUp")
//			summary_pager.setCurrentItem(sharedPosition-1, true)
//			return true
//		}
//		override fun onDown(e: MotionEvent?): Boolean {
//			Util.log("****************************** onDown")
//			return true
//		}
//		override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
//			Util.log("****************************** onFling")
//			return true
//		}
//		override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
//			Util.log("****************************** onScroll")
//			return true
//		}
//		override fun onLongPress(e: MotionEvent?) {
//			Util.log("****************************** onLongPress")
//		}
//
//	}

	inner class SummaryPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

		val fragments = ArrayList<Fragment>()

		internal fun add(fragment: Fragment) {
			fragments.add(fragment)
		}

		internal fun addInFirst(fragment: Fragment){
			fragments.add(0,fragment)
			sort()
			notifyDataSetChanged()
		}

		//번들로 넘어온 페이지 넘버값으로 정렬해준다.
		internal fun sort(){
			fragments.sortBy { data -> data.arguments?.getInt(MAIN_INDEX) }
		}

		internal fun remove(fragment :Fragment) {
			fragments.remove(fragment)
		}

		override fun getItemPosition(`object`: Any): Int {
			val idx = fragments.indexOf(`object`)
			return if (idx < 0) PagerAdapter.POSITION_NONE else idx
		}

		override fun getItemId(position: Int): Long {
			return System.identityHashCode(fragments[position]).toLong()
		}

		override fun getItem(position: Int): Fragment {
			return fragments[position]
		}

		override fun getCount(): Int {
			return fragments.size
		}
		fun clear() {
			fragments.clear()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		mRankingAdapter?.clear()
		mSummaryAdapter?.clear()
	}

	fun moreRankingView() {
		rankingViewIsOn = true
		binding.rankingPager.setPadding(0, 0, 0, 0)
		checkVisibility()
		binding.rankingPager.currentItem = sharedPosition + 1
		binding.rankingPager.currentItem = sharedPosition
	}

	fun moreSummaryView(){
		rankingViewIsOn =false
		binding.rankingPager.setPadding((margin / 1.5).toInt(), 0, (margin / 1.5).toInt(), 0)
		//binding.rankingPager.setPadding(margin, 0, margin, 0)
		checkVisibility()
		binding.rankingPager.currentItem = sharedPosition+1
		binding.rankingPager.currentItem = sharedPosition
	}

	fun checkVisibility(){
		mSummaryAdapter?.let {
			for (i in 0 until it.fragments.size - 1) {
				try {
					if (it.fragments[i] is RankingPageFragment) {
						val frag = it.fragments[i] as RankingPageFragment
						frag.checkVisibility(rankingViewIsOn)
					}
				} catch (e: Exception) {
					try {
						//아래 exception으로  던짐
						throw  Exception("class : $this \n exception -> $e");
					} catch (e1: Exception) {
						FirebaseCrashlytics.getInstance().recordException(e1);
					}
					e.printStackTrace()
				}
			}
		}
	}

	fun setFakeRankingView(mItems: ArrayList<IdolModel>, typeList: TypeListModel, femaleCategory : String?){
		binding.rvRanking.adapter = null
		mRankingAdapter?.clear()
		mRankingAdapter?.setTypeList(typeList)
		mRankingAdapter?.setItems(mItems)
		mRankingAdapter?.notifyDataSetChanged()
		binding.rvRanking.adapter = mRankingAdapter

		binding.tvTitle.text = typeList.name

        femaleCheck = femaleCategory!=null

		if(typeList.type == "S"){
			if(femaleCheck){
				binding.tvTitle.text = getString(R.string.actor_female_singer)
			}
			else{
				binding.tvTitle.text = getString(R.string.actor_male_singer)
			}
		}
		else if(typeList.type == "A"){
            if(femaleCheck)
            {
                binding.tvTitle.text = getString(R.string.lable_actresses)
            }
            else{
                binding.tvTitle.text = getString(R.string.lable_actors)
            }
		}

		val tmp = Date().time
		val formatter = SimpleDateFormat("yyyy. MM. dd ", Locale.getDefault())
		val date = formatter.format(tmp)
		binding.tvDate.text = date + requireContext().getString(R.string.ranking)

		val bgShape_ranking = binding.layoutBgRanking.getBackground() as GradientDrawable
		bgShape_ranking.setColor(Color.parseColor(typeList.getUiColor(requireContext()).toString()))
	}

	fun getFakeRankingView() : View{
		binding.clSummaryRankingView.visibility = View.VISIBLE
		binding.clSummaryRankingView.bringToFront()
		return binding.clSummaryRankingView
	}

	fun getAnimationView(bitmap: Bitmap) : View{
        with(binding) {
            animationView.setImageBitmap(bitmap)
            animationView.visibility = View.VISIBLE
            animationView.bringToFront()
            return animationView
        }
	}

	fun releaseView(){
        with(binding) {
            animationView.visibility = View.INVISIBLE
            clSummaryRankingView.visibility = View.GONE
            binding.rankingPager.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.text_white_black))
            binding.rankingPager.bringToFront()

            //라이브 버튼 또는 어워트 버튼 보여야 하는 상황 + 랭킹뷰 내려갔을때
            if((ConfigModel.getInstance(context).showAwardTab || ConfigModel.getInstance(context).showLiveStreamingTab || binding.ivMission.isVisible)
                && !rankingViewIsOn){
                llAwardLiveContainer.bringToFront()
            }
        }
	}

	fun closeRankingView(){
		for(i in 0 until mSummaryAdapter?.fragments!!.size) {
			try {
				Logger.v("MoreCheck:: class name ${mSummaryAdapter?.fragments!![i].javaClass}")
				if(mSummaryAdapter?.fragments!![i] is RankingPageFragment){
					Logger.v("MoreCheck:: inside index ${i}")
					val frag = mSummaryAdapter?.fragments!![i] as RankingPageFragment
					if(frag.fragIsVisible) frag.closeRankingView()
				}
			} catch (e: Exception){
				e.printStackTrace()
			}
		}
	}

	fun getCurrentFragment() : Fragment? {
		try {
			return mSummaryAdapter?.fragments?.get(sharedPosition)
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}

    fun showWelcomeMissionButton(show: Boolean = true) {
        binding.ivMission.visibility = if (show) View.VISIBLE else View.GONE

        if (show) {
            binding.ivMission.post {
                binding.ivMission.visibility = View.VISIBLE
                // 바운싱 애니메이션은 아직 적용하면 안됨
//                startBouncing()
            }
        }
    }

    fun startBouncing() {
        val bounceAnimator = ValueAnimator.ofFloat(0f, -50f, 0f).apply {
            duration = 600 // 전체 애니메이션 지속 시간
            interpolator = LinearInterpolator() // 일정한 속도로 튀기기
            repeatCount = ValueAnimator.INFINITE // 무한 반복
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                binding.ivMission.translationY = value
            }
        }
        bounceAnimator.start()
    }

    private fun updateTutorial(tutorialIndex: Int) = lifecycleScope.launch {
        usersRepository.updateTutorial(
            tutorialIndex = tutorialIndex,
            listener = { response ->
                if (response.optBoolean("success")) {
                    Logger.d("Tutorial updated successfully: $tutorialIndex")
                    val bitmask = response.optLong("tutorial", -1L)
                    TutorialManager.init(bitmask)
                } else {
                    Toast.makeText(requireContext(), response.toString(), Toast.LENGTH_SHORT).show()
                }
            },
            errorListener = { throwable ->
                Toast.makeText(requireContext(), throwable.message ?: "Error updating tutorial", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onScrollToTop() {
        binding.rvRanking.scrollToPosition(0)
    }

	companion object {

		//번들로 전해주는 페이지 순서 값
		const val MAIN_INDEX = "main_index"

		var mSummaryAdapter: SummaryPagerAdapter? = null
		var idolTypeList: ArrayList<IdolTypeModel> = ArrayList<IdolTypeModel>()
		var checkTrueList: ArrayList<IdolTypeModel> = ArrayList<IdolTypeModel>()
	}
}
