package net.ib.mn.addon

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.BaseAdapter
import android.widget.RelativeLayout
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Util
import net.ib.mn.view.ExodusImageView

abstract class ArrayAdapter<T> : BaseAdapter {
    private var mLayoutId: Int
    private var mItems: MutableList<T>
    protected var context: Context
        private set
    private var mGlideRequestManager: RequestManager? = null
    protected var layoutInflater: LayoutInflater
        private set

    // 프사 눌러 펼치기
    protected var mContainerPhotos: View? = null
    @JvmField
    protected var mPhoto1: ExodusImageView? = null
    @JvmField
    protected var mPhoto2: ExodusImageView? = null
    @JvmField
    protected var mPhoto3: ExodusImageView? = null
    protected var mapExpanded = HashMap<Int, Boolean?>()
    protected var viewContainerRanking: View? = null
    protected abstract fun update(view: View?, item: T, position: Int)

    constructor(context: Context, layoutId: Int) {
        this.context = context
        mLayoutId = layoutId
        layoutInflater = LayoutInflater.from(context)
        mItems = ArrayList()
    }

    constructor(context: Context, glideRequestManager: RequestManager?, layoutId: Int) {
        this.context = context
        mGlideRequestManager = glideRequestManager
        mLayoutId = layoutId
        layoutInflater = LayoutInflater.from(context)
        mItems = ArrayList()
    }

    val items: List<T>
        get() = mItems

    override fun getCount(): Int {
        return mItems.size
    }

    override fun getItem(position: Int): T {
        return mItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun add(item: T): Boolean {
        return mItems.add(item)
    }

    fun add(position: Int, item: T) {
        mItems.add(position, item)
    }

    fun remove(item: T): Boolean {
        return mItems.remove(item)
    }

    @Throws(IndexOutOfBoundsException::class)
    fun remove(position: Int): T {
        return mItems.removeAt(position)
    }

    fun addAll(items: Collection<T>?): Boolean {
        return mItems.addAll(items!!)
    }

    fun isLastPosition(position: Int): Boolean {
        return if (mItems.isEmpty()) false else mItems.size - 1 == position
    }

    fun clear() {
        mItems.clear()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        @Suppress("NAME_SHADOWING")
        var convertView = convertView
        if (convertView == null) {
            convertView = layoutInflater.inflate(mLayoutId, parent, false)
        }
        // 프사 눌러 펼치기
        mContainerPhotos = convertView?.findViewById(R.id.container_photos)
        if (mContainerPhotos != null) mContainerPhotos!!.tag =
            "" + position // set tag to stop exoplayer
        mPhoto1 = convertView?.findViewById(R.id.photo1)
        mPhoto2 = convertView?.findViewById(R.id.photo2)
        mPhoto3 = convertView?.findViewById(R.id.photo3)
        viewContainerRanking = convertView?.findViewById(R.id.container_ranking)
        if (viewContainerRanking != null) {
            viewContainerRanking!!.setBackgroundResource(0)
        }
        update(convertView!!, getItem(position), position)
        return convertView!!
    }

    // 카테고리/탭 이동시 닫히는 애니메이션 제거용
    fun clearMapExpanded() {
        mapExpanded.clear()
    }

    // recyclerview로 전환 후 BaseRankingAdapter 적용 필요 (코드 중복)
    protected fun showExpanded(position: Int, expanded: Boolean, item: IdolModel) {
        val ll = mContainerPhotos!!.layoutParams as RelativeLayout.LayoutParams
        val toHeight = mContainerPhotos!!.width / 3
        val key = position
        if (expanded) {
//            ll.height = mContainerPhotos.getWidth() / 3;
            val photos = arrayOf<View?>(mPhoto1, mPhoto2, mPhoto3)
            for (p in photos) {
                val lp = p!!.layoutParams as RelativeLayout.LayoutParams
                lp.height = toHeight
                p.layoutParams = lp
            }
            if (mGlideRequestManager != null) {
                if (item.imageUrl != null) {
                    Util.loadGif(mGlideRequestManager, item.imageUrl, mPhoto1)
                    Util.loadGif(mGlideRequestManager, item.imageUrl2, mPhoto2)
                    Util.loadGif(mGlideRequestManager, item.imageUrl3, mPhoto3)
                } else {
                    mGlideRequestManager!!.load(Util.noProfileThemePickImage(item.getId())).into(
                        mPhoto1!!
                    )
                    mGlideRequestManager!!.load(Util.noProfileThemePickImage(item.getId())).into(
                        mPhoto2!!
                    )
                    mGlideRequestManager!!.load(Util.noProfileThemePickImage(item.getId())).into(
                        mPhoto3!!
                    )
                }
            }

            // 펼쳐진 적이 없으면 펼치기 애니메이션
            if (mapExpanded[key] == null || !mapExpanded[key]!!) {
                val v = mContainerPhotos
                v!!.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        v.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        mapExpanded[key] = true
                        val anim = ValueAnimator.ofInt(0, toHeight)
                        anim.addUpdateListener { valueAnimator: ValueAnimator ->
                            val `val` = valueAnimator.animatedValue as Int
                            val layoutParams = v.layoutParams
                            layoutParams.height = `val`
                            v.layoutParams = layoutParams
                        }
                        anim.setDuration(250)
                        anim.start()
                    }
                })
            } else {
                ll.height = toHeight
                mContainerPhotos!!.layoutParams = ll
            }
        } else {
            // 펼쳐진 적이 있으면 접기 애니메이션
            if (mapExpanded[key] != null && mapExpanded[key]!!) {
                Util.log("shrink animation row $position")
                val v = mContainerPhotos
                v!!.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        v.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        mapExpanded[key] = false
                        val anim = ValueAnimator.ofInt(toHeight, 0)
                        anim.addUpdateListener { valueAnimator: ValueAnimator ->
                            val `val` = valueAnimator.animatedValue as Int
                            val layoutParams = v.layoutParams
                            layoutParams.height = `val`
                            v.layoutParams = layoutParams
                        }
                        anim.setDuration(300)
                        anim.start()
                    }
                })
            } else {
//                Util.log("shrink row "+position);
                ll.height = 0
                mContainerPhotos!!.layoutParams = ll
            }
        }
    }
}
