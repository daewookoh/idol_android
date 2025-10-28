package net.ib.mn.view

import android.database.DataSetObserver
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import net.ib.mn.utils.Util

class InfinitePagerAdapter(private val adapter: PagerAdapter) : PagerAdapter() {
    override fun getCount(): Int {
        if (realCount == 0) {
            return 0
        }

        return 7
    }

    val realCount: Int
        /**
         * @return the [.getCount] result of the wrapped adapter
         */
        get() = adapter.count

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val virtualPosition = position % realCount
        debug("instantiateItem: real position: $position")
        debug("instantiateItem: virtual position: $virtualPosition")
        Util.log("*** instantiateItem$virtualPosition")
        return adapter.instantiateItem(container, virtualPosition)
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val virtualPosition = position % realCount
        debug("destroyItem: real position: $position")
        debug("destroyItem: virtual position: $virtualPosition")
        Util.log("*** destroyItem$virtualPosition")
        adapter.destroyItem(container, virtualPosition, `object`)
    }


    override fun finishUpdate(container: ViewGroup) {
        adapter.finishUpdate(container)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return adapter.isViewFromObject(view, `object`)
    }

    override fun restoreState(bundle: Parcelable?, classLoader: ClassLoader?) {
        adapter.restoreState(bundle, classLoader)
    }

    override fun saveState(): Parcelable? {
        return adapter.saveState()
    }

    override fun startUpdate(container: ViewGroup) {
        adapter.startUpdate(container)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        val virtualPosition = position % realCount
        return adapter.getPageTitle(virtualPosition)
    }

    override fun getPageWidth(position: Int): Float {
        return adapter.getPageWidth(position)
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        adapter.setPrimaryItem(container, position, `object`)
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver) {
        adapter.unregisterDataSetObserver(observer)
    }

    override fun registerDataSetObserver(observer: DataSetObserver) {
        adapter.registerDataSetObserver(observer)
    }

    override fun notifyDataSetChanged() {
        adapter.notifyDataSetChanged()
    }

    override fun getItemPosition(`object`: Any): Int {
        return adapter.getItemPosition(`object`)
    }

    private fun debug(message: String) {
        if (DEBUG) {
            Log.d(TAG, message)
        }
    }

    companion object {
        private const val TAG = "InfinitePagerAdapter"
        private const val DEBUG = false
    }
}
