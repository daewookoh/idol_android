package net.ib.mn.bitmapool

import android.graphics.Bitmap

class BitmapPool private constructor() {

    private val pool = HashMap<String, Bitmap?>()

    companion object {
        val instance: BitmapPool by lazy { BitmapPool() }
    }

    fun getViewFromBitmapPool(name: String): Bitmap? {
        return pool[name]
    }

    fun putViewToBitmapPool(name: String?, category: String?, bitmap: Bitmap?) {
        pool["$name$category"] = bitmap
    }

    fun clearBitmapPool() {
        pool.clear()
    }
}
