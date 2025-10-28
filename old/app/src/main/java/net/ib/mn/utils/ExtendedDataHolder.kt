package net.ib.mn.utils

/**
 * Created by parkboo on 2018. 1. 12..
 */
class ExtendedDataHolder {
    companion object {
        val ourInstance = ExtendedDataHolder()

        @JvmStatic fun getInstance(): ExtendedDataHolder {
            return ourInstance
        }
    }

    private val extras = HashMap<String, Any>()


    fun putExtra(name: String, `object`: Any) {
        extras.put(name, `object`)
    }

    fun getExtra(name: String): Any? {
        return extras.get(name)
    }

    fun hasExtra(name: String): Boolean {
        return extras.containsKey(name)
    }

    fun removeExtra(name: String) {
        extras.remove(name)
    }

    fun clear() {
        extras.clear()
    }
}