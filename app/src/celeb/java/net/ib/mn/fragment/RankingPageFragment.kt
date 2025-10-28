package net.ib.mn.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.activity.BaseActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseApplication
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.CelebTutorialBits
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.SharedAppState
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class RankingPageFragment : NewRankingFragment2() {

	private var typeId: Int  = 0
    @Inject
    lateinit var sharedAppState: SharedAppState

	override fun onAttach(context: Context) {
		super.onAttach(context)
		var extra: Bundle? = this.arguments

		val typeList = IdolGson.getInstance().fromJson(extra?.getSerializable("type").toString(), TypeListModel::class.java)
		//bundle2로 여자라는 F값을 받는 값
		val femaleCategory = extra?.getString(Const.KEY_CATEGORY)
		this.typeList = typeList
		if (femaleCategory != null) {
			this.femaleCategory = femaleCategory
		}

		type = if(typeList.type.isNullOrEmpty()) null else typeList.type
		isDevided = if(typeList.isDivided.isNullOrEmpty()) null else typeList.isDivided
		isFemale = typeList.isFemale
        // 아래는 실제 type이 두자리인 경우가 없었고 새로 "AG"가 추가되면서 부작용이 생겨 제거함
//		if(type?.length == 2) {
//			val type_ = type
//			category = type_!![1].toString()
//			type = type_[0].toString()
//		}
		typeName = typeList.typeName
		typeId = typeList.id
		Logger.v("PagerTypeCheck::type${type} typeName ${typeName} typeId${typeId} category${category}")
	}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeState()
    }

    fun observeState() {
        lifecycleScope.launch {
            sharedAppState.scrollToTop.collect {
                if (it) {
                    binding.rvRanking.scrollToPosition(0)
                }
            }
        }
    }

	override fun getVoteRequestCode(): Int {
		return 200
	}

	override fun onFragmentSelected() {
	}

	fun checkVisibility(rankingViewIsOn : Boolean){
		if(rankingViewIsOn) {
//			if(!fragIsVisible) {
				//프래그먼트 6개동시에 뷰업데이트해주면 리소스 많이소모 되므로 뷰업데이트는 무조건 타이머에서 해준다.
//				updateUI(false)
				mClRankingView.visibility = View.VISIBLE
				mClSummaryView.visibility = View.GONE
//			}
		} else {
//			if(!fragIsVisible) {
//				updateUI(false)
				mClRankingView.visibility = View.INVISIBLE
				mClSummaryView.visibility = View.VISIBLE
//			}
		}
	}

	fun firstStart(){
		fragIsVisible = true
		setFakeRankingView()
		startPlayer()
//		startTopBannerPlayer()
		updateDataWithUI(true)
		startTimer()
        setTutorial()

        mRankingAdapter?.setIsFirst(true)
	}

    private fun setTutorial() {
        when(val currentIndex = TutorialManager.getTutorialIndex()) {
            CelebTutorialBits.MAIN_TOP_BANNER -> {
                setupLottieTutorial(binding.lottieTutorialTopBanner) {
                    updateTutorialIndex(currentIndex)
                    binding.top1CardView.callOnClick()
                }
            }
            CelebTutorialBits.RANKING_MORE -> {
                setupLottieTutorial(binding.lottieTutorialMore) {
                    updateTutorialIndex(currentIndex)
                    binding.btnMoreSummary.callOnClick()
                }
            }
        }
    }

	fun closeRankingView(){
		if(fragIsVisible) binding.btnMoreSummary.callOnClick()
	}

	override fun onDestroy() {
		super.onDestroy()
		stopTimer()
	}
}
