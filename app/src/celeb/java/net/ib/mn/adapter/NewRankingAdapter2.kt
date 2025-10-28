package net.ib.mn.adapter

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.RankingItemBinding
import net.ib.mn.databinding.TextureRankingItemBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.fragment.NewRankingFragment2
import net.ib.mn.model.IdolModel
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.CelebTutorialBits
import net.ib.mn.utils.Const
import net.ib.mn.utils.RankingBindingProxy
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.setIdolBadgeIcon
import net.ib.mn.view.ExodusImageView
import net.ib.mn.view.ProgressBarLayout
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.round
import kotlin.math.sqrt
import net.ib.mn.utils.ext.getFontColor
import net.ib.mn.utils.ext.getUiColor

@UnstableApi
class NewRankingAdapter2(
        private val context: Context,
        private val fragment: NewRankingFragment2,
        private val glideRequestManager: RequestManager,
        private var mItems: ArrayList<IdolModel>,
        private val mListener: OnClickListener,
        private val typeList: TypeListModel
) : BaseRankingAdapter() {

    interface OnClickListener {
        fun onItemClicked(item: IdolModel?)
        fun onVote(item: IdolModel)
        fun onPhotoClicked(item: IdolModel, position: Int)
        fun updateTutorialIndex(index: Int)
    }

    private var mMaxVoteCount: Long = 0
    protected var mapExpanded = HashMap<Int, Boolean>()
    private val voteMap = HashMap<Int, Long>()
    var hasExpanded : Boolean = false // 움짤프사 한번이라도 펼치면 1위프사 움짤은 화면 재진입 이전까지 재생 안함. 10초 자동갱신때문에 추가.

    var oldTopId: Int = 0 // 1위가 바뀌는지 알기 위함, 10초 자동갱신때문에 추가.
    var needUpdate : Boolean = false // 남여 전환시나 화면 전환시 강제로 다시 갱신하기 위한 용도
    var oldTop3 : String? = "" // top3가 변경되는 경우 1위 프사 이붙을 갱신
    var isFirst = false

	// 투표수 애니메이션 pool
	private val animatorPool = HashMap<Int, ValueAnimator?>()

    fun setIsFirst(isFirst: Boolean) {
        this.isFirst = isFirst
    }

    @UnstableApi
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        mMaxVoteCount = if( mItems.size > 0 ) { mItems[0].heart } else { 0 }

        val bindingProxy: RankingBindingProxy = if (Util.isOSNougat()) {
            val binding = RankingItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            RankingBindingProxy(binding)
        } else {
            val binding = TextureRankingItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
            RankingBindingProxy(binding)
        }

        bindingProxy.voteBtn.setColorFilter(if (typeList.type.isNullOrEmpty()) context.resources.getColor(R.color.main) else Color.parseColor(typeList.getUiColor(context).toString()), android.graphics.PorterDuff.Mode.SRC_IN)
        val progressBar = this.context.getResources().getDrawable(R.drawable.progressbar_ranking)
        progressBar.setColorFilter(if (typeList.type.isNullOrEmpty()) context.resources.getColor(R.color.main) else Color.parseColor(typeList.getUiColor(context).toString()), android.graphics.PorterDuff.Mode.MULTIPLY)
        bindingProxy.progressBarFrame.background = progressBar

        return RankViewHolder(bindingProxy)
    }

    override fun getItemId(position: Int): Long {
        var id = 0L
        if( mItems.isNotEmpty() ) {
            id = mItems[position].getId().toLong()
        }

        return id
    }

    override fun getItemCount(): Int {
        if( mItems.isNullOrEmpty() )
            return 0
        return mItems.size
    }

    @UnstableApi
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (mItems.isNullOrEmpty()) return

        (holder as RankViewHolder).bind(mItems[position], position)
    }

    fun clear() {
        mItems.clear()
    }

	fun clearAnimation() {
		for((k, v) in animatorPool ) {
			v?.removeAllUpdateListeners()
		}
		animatorPool.clear()
	}

	fun setItems(@NonNull items: ArrayList<IdolModel>) {
        // mItems Clear하면 java.lang.IndexOutOfBoundsException: Inconsistency detected 익셉션이 발생.
        mItems = ArrayList()
        mItems.addAll(items)
        // 남녀 전환시 순위막대 이상해져서 여기에도 넣어줌
        mMaxVoteCount = if( mItems.size > 0 ) { mItems[0].heart } else { 0 }
        notifyDataSetChanged()
    }

    @UnstableApi
    inner class RankViewHolder(val binding: RankingBindingProxy) : RecyclerView.ViewHolder(binding.root) {
        private val badgeBirth = binding.badgeBirth
        private val badgeDebut = binding.badgeDebut
        private val badgeComeback = binding.badgeComeback
        private val badgeMemorialDay = binding.badgeMemorialDay
        private val badgeAllInDay = binding.badgeAllInDay
        public val nameView: AppCompatTextView = binding.nameView
        val rankView: AppCompatTextView = binding.rankView
        val voteCountView: AppCompatTextView = binding.voteCountView
        private val imageView: AppCompatImageView = binding.imageView
        private val voteBtn: AppCompatImageView = binding.voteBtn
        private val progressBar: ProgressBarLayout = binding.progressBar
        private val photoBorder: AppCompatImageView = binding.photoBorder
        private val groupView: AppCompatTextView = binding.groupView

        // 이붙
        private val mContainerPhotos: ConstraintLayout = binding.containerPhotos
        private val mPhoto1: ExodusImageView = binding.photo1
        private val mPhoto2: ExodusImageView = binding.photo2
        private val mPhoto3: ExodusImageView = binding.photo3

        // 기부천사/기부요정
        private val iconAngel: AppCompatTextView = binding.iconAngel
        private val iconFairy: AppCompatTextView = binding.iconFairy
        private val iconMiracle: AppCompatTextView = binding.iconMiracle


        fun bind(idol: IdolModel, position: Int) {
            itemView.setOnClickListener {
				mListener.onItemClicked(idol)
            }

            val rank = idol.rank
            if (idol.getName(context).contains("_")) {
                nameView.text = Util.nameSplit(context, idol)[0]

                groupView.visibility = View.VISIBLE

                if (idol.getName(context).contains("_")) {
                    groupView.text =  Util.nameSplit(context, idol)[1]
                } else {
                    groupView.visibility = View.GONE
                }
            } else {
                nameView.text = idol.getName(context)
                groupView.visibility = View.GONE
            }

            when (idol.anniversary) {
                Const.ANNIVERSARY_BIRTH -> {
                    badgeBirth.visibility = View.VISIBLE
                    badgeDebut.visibility = View.GONE
                    badgeComeback.visibility = View.GONE
                    badgeMemorialDay.visibility = View.GONE
                    badgeAllInDay.visibility = View.GONE
                }
                Const.ANNIVERSARY_DEBUT -> {
                    badgeBirth.visibility = View.GONE
                    badgeDebut.visibility = View.VISIBLE
                    badgeComeback.visibility = View.GONE
                    badgeMemorialDay.visibility = View.GONE
                    badgeAllInDay.visibility = View.GONE
                }
                Const.ANNIVERSARY_COMEBACK -> {
                    badgeBirth.visibility = View.GONE
                    badgeDebut.visibility = View.GONE
                    badgeComeback.visibility = View.VISIBLE
                    badgeMemorialDay.visibility = View.GONE
                    badgeAllInDay.visibility = View.GONE
                }
                Const.ANNIVERSARY_MEMORIAL_DAY -> {
                    badgeBirth.visibility = View.GONE
                    badgeDebut.visibility = View.GONE
                    badgeComeback.visibility = View.GONE
                    badgeMemorialDay.visibility = View.VISIBLE
                    badgeAllInDay.visibility = View.GONE
                    val memorialDayCount: String
                    if (Util.isRTL(context)) {
                        memorialDayCount = NumberFormat.getNumberInstance(Locale.getDefault()).format(idol.anniversaryDays)
                    } else {
                        memorialDayCount = idol.anniversaryDays.toString()
                    }
                    badgeMemorialDay.text = memorialDayCount.replace(("[^\\d.]").toRegex(), "").plus(context.getString(R.string.lable_day))
                }
                Const.ANNIVERSARY_ALL_IN_DAY -> {
                    badgeBirth.visibility = View.GONE
                    badgeDebut.visibility = View.GONE
                    badgeComeback.visibility = View.GONE
                    badgeMemorialDay.visibility = View.GONE
                    badgeAllInDay.visibility = View.VISIBLE
                }
                else -> {
                    badgeBirth.visibility = View.GONE
                    badgeDebut.visibility = View.GONE
                    badgeComeback.visibility = View.GONE
                    badgeMemorialDay.visibility = View.GONE
                    badgeAllInDay.visibility = View.GONE
                }
            }

            UtilK.profileRoundBorder(idol.miracleCount, idol.fairyCount, idol.angelCount, photoBorder)

            val rankCountComma = NumberFormat.getNumberInstance(Locale.getDefault()).format(rank + 1)
            rankView.text = String.format(
                    context.getString(R.string.rank_count_format),
                    rankCountComma)

            // 투표수 글자색
            if(typeList.type.isNullOrEmpty()) {
                voteCountView.setTextColor(ContextCompat.getColor(context, R.color.text_white_black))
            } else {
                voteCountView.setTextColor(ContextCompat.getColor(context, R.color.fix_gray1000))
            }
			val voteCount = idol.heart

			val oldVote : Long = voteMap.get(idol.getId()) ?: 0L

			// ViewHolder 멤버로 넣었더니 이전 animation이 안가져 와져서 pool에서 꺼내옴
			var animator = animatorPool[itemView.hashCode()]
			animator?.removeAllUpdateListeners()    // 기존 애니메이션 돌던거를 취소하고
			animator?.cancel()
			if( oldVote != voteCount && Util.getPreferenceBool(context, Const.PREF_ANIMATION_MODE, false)) {
				animator = ValueAnimator.ofFloat(0f, 1f) //ofInt로 되어있다가 21억표 이상 갈 경우 처리가 안되어서, ofFloat을 이용하여 큰 숫자더라도 근사치값 표현
				// 애니메이터 생성 후 풀에 넣기
				animatorPool.set(itemView.hashCode(), animator)
				animator?.addUpdateListener {
                    val value = round(oldVote + (voteCount - oldVote) * (it.animatedValue as Float))	//소수점 아래 1번째에서 반올림
                    val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault()).format(value)
					voteCountView.text = voteCountComma
				}
				animator?.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator) {
                    }

                    override fun onAnimationCancel(animation: Animator) {
                    }

                    override fun onAnimationStart(animation: Animator) {
                    }

                    override fun onAnimationEnd(animation: Animator) {	//애니메이션 끝나면 실제 표수 보여줌. 숫자가 크면 근사치값만 보여지기 때문에 애니메이션 끝나고 처리 필요
                        val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault()).format(voteCount)
                        voteCountView.text = voteCountComma
                    }
                })
				animator?.duration = 1000
				animator?.start()
			} else {
				val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault()).format(voteCount)
				voteCountView.text = voteCountComma
			}
			voteMap.set(idol.getId(), voteCount)

            val profileThumb: String? = UtilK.top1ImageUrl(context, idol, Const.IMAGE_SIZE_LOWEST)

            val idolId = idol.getId()
            glideRequestManager.load(profileThumb)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(idolId))
                    .fallback(Util.noProfileImage(idolId))
                    .placeholder(Util.noProfileImage(idolId))
                    .dontAnimate()
                    .into(imageView)

            (context as FragmentActivity).windowManager
                    .defaultDisplay
                    .getMetrics(DisplayMetrics())


            if (mMaxVoteCount == 0L) {
                progressBar.setWidthRatio(38)
            } else {
                // int 오버플로 방지
                if (voteCount == 0L) {
                    progressBar.setWidthRatio(38)
                } else {
                    progressBar.setWidthRatio(38 + (sqrt(sqrt(voteCount.toDouble())) * 62 / sqrt(sqrt(mMaxVoteCount.toDouble()))).toInt())
                }
            }

//            Util.log("*** mMaxVoteCount="+mMaxVoteCount);

            voteBtn.setOnClickListener {
				mListener.onVote(idol)
            }

            // 프사 눌러 펼치기.
			val key = position
            if (fragment.mapExpanded[key] == true) {
                binding.containerPhotos.visibility = View.VISIBLE
            } else {
                binding.containerPhotos.visibility = View.GONE
            }

            // 프사 펼쳐져있는 상태라면
            if(binding.containerPhotos.isVisible) {
                val imageUrls = UtilK.getTop3ImageUrl(context, idol)
                glideRequestManager.load(imageUrls[0]).into(mPhoto1)
                glideRequestManager.load(imageUrls[1]).into(mPhoto2)
                glideRequestManager.load(imageUrls[2]).into(mPhoto3)
            }

            imageView.isClickable = true
			imageView.setOnClickListener {
				mListener.onPhotoClicked(idol, position)

				hasExpanded = true

                if(binding.containerPhotos.visibility == View.VISIBLE) {
                    collapse(binding.containerPhotos)
                    fragment.mapExpanded[position] = false
                    // 이전에 펼쳐진 것들 처리
                    fragment.stopExoPlayer(fragment.playerView1)
                    fragment.stopExoPlayer(fragment.playerView2)
                    fragment.stopExoPlayer(fragment.playerView3)
                } else {
                    val imageUrls = UtilK.getTop3ImageUrl(context, idol)
                    glideRequestManager.load(imageUrls[0]).into(mPhoto1)
                    glideRequestManager.load(imageUrls[1]).into(mPhoto2)
                    glideRequestManager.load(imageUrls[2]).into(mPhoto3)

                    expand(binding.containerPhotos) {
                        // 펼치기가 끝나면 움짤 재생
                        fragment.playExoPlayer(
                            0,
                            itemView.findViewById(net.ib.mn.R.id.playerview1),
                            itemView.findViewById(net.ib.mn.R.id.photo1),
                            idol.imageUrl
                        )
                        fragment.playExoPlayer(
                            1,
                            itemView.findViewById(net.ib.mn.R.id.playerview2),
                            itemView.findViewById(net.ib.mn.R.id.photo2),
                            idol.imageUrl2
                        )
                        fragment.playExoPlayer(
                            2,
                            itemView.findViewById(net.ib.mn.R.id.playerview3),
                            itemView.findViewById(net.ib.mn.R.id.photo3),
                            idol.imageUrl3
                        )
                    }
                    fragment.mapExpanded[position] = true
                }

                // 이전에 펼쳐진 것들 처리
                fragment.stopExoPlayer(fragment.playerView1)
                fragment.stopExoPlayer(fragment.playerView2)
                fragment.stopExoPlayer(fragment.playerView3)
            }

            setIdolBadgeIcon(iconAngel, iconFairy, iconMiracle, binding.iconRookie, binding.iconSuperRookie, idol);

            if (isFirst && position == 0) {
                if (TutorialManager.getTutorialIndex() == CelebTutorialBits.MAIN_BANNER_GRAM) {
                    setupLottieTutorial(binding.rankLottie ?: return) {
                        mListener.updateTutorialIndex(TutorialManager.getTutorialIndex())
                        imageView.callOnClick()
                    }
                }
            }
        }
    }

    fun clearMapExpanded() {
        mapExpanded.clear()
    }
}
