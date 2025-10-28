package net.ib.mn.addon

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import net.ib.mn.utils.Util

class InternetConnectivityManager private constructor() {
    interface Observer {
        fun onInternetConnected()
        fun onInternetDisconnected()
        fun onInternetConnectTypeChanged(isWifi: Boolean)
    }

    private val mObservers: ArrayList<Observer>
    private var mConnectState = 0
    private val connectStateLock: Any

    init {
        mObservers = ArrayList()
        connectStateLock = Any()
    }

    fun addObserver(ob: Observer): Boolean {
        synchronized(mObservers) { return mObservers.add(ob) }
    }

    fun removeObserver(ob: Observer): Boolean {
        synchronized(mObservers) { return mObservers.remove(ob) }
    }

    val isConnected: Boolean
        get() {
            synchronized(connectStateLock) { return mConnectState > 0 }
        }
    var isWifiConnected: Boolean
        get() {
            synchronized(connectStateLock) { return mConnectState and STATE_WIFI > 0 }
        }
        private set(connected) {
            synchronized(connectStateLock) {
                val isWifiPrevConnected = isWifiConnected
                if (isWifiPrevConnected && !connected) {
                    mConnectState -= STATE_WIFI
                } else if (!isWifiPrevConnected && connected) {
                    mConnectState += STATE_WIFI
                }
            }
        }
    var isMobileConnected: Boolean
        get() {
            synchronized(connectStateLock) { return mConnectState and STATE_MOBILE > 0 }
        }
        private set(connected) {
            synchronized(connectStateLock) {
                val isMobilePrevConnected = isMobileConnected
                if (isMobilePrevConnected && !connected) {
                    mConnectState -= STATE_MOBILE
                } else if (!isMobilePrevConnected && connected) {
                    mConnectState += STATE_MOBILE
                }
            }
        }
    private val connectState: Int
        get() {
            synchronized(connectStateLock) { return mConnectState }
        }

    private fun notifyConnected() {
        synchronized(mObservers) {
            Util.log(TAG + "notify internet connected")
            for (ob in mObservers) {
                ob.onInternetConnected()
            }
        }
    }

    private fun notifyDisconnected() {
        synchronized(mObservers) {
            Util.log(TAG + "notify internet disconnected")
            for (ob in mObservers) {
                ob.onInternetDisconnected()
            }
        }
    }

    private fun notifyConnectTypeChanged() {
        synchronized(mObservers) {
            Util.log(TAG + "notify internet connect type changed")
            for (ob in mObservers) {
                ob.onInternetConnectTypeChanged(isWifiConnected)
            }
        }
    }

    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateNetworkState(context)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(context: Context?): InternetConnectivityManager {
            @Suppress("NAME_SHADOWING")
            var context = context
            if (manager == null) {
                if (context is Activity) {
                    context = context.applicationContext
                } else if (context is Service) {
                    context = context.applicationContext
                }
                manager = InternetConnectivityManager()
                updateNetworkState(context)
            }
            return manager!!
        }

        private const val TAG = "InternetConnectivityManager"
        private const val STATE_WIFI = 1
        private const val STATE_MOBILE = 2
        private var manager: InternetConnectivityManager? = null

        // 비행기모드로 실행->회원가입시 네트워크 오류->와이파이 연결 후 다시 회원가입 시도시 네트워크 상태 업데이트 안됨 수정 private->public
        fun updateNetworkState(context: Context?) {
            val manager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfos = manager.allNetworkInfo
            val connManager = getInstance(context)
            synchronized(connManager.connectStateLock) {
                val prevConnectedState = connManager.connectState
                for (netInfo in netInfos) {
                    when (netInfo.type) {
                        ConnectivityManager.TYPE_MOBILE -> connManager.isMobileConnected =
                            netInfo.isConnected

                        ConnectivityManager.TYPE_WIFI -> connManager.isWifiConnected =
                            netInfo.isConnected
                    }
                }
                val currConnectedState = connManager.connectState
                if (prevConnectedState != currConnectedState) {
                    if (currConnectedState == 0) {
                        connManager.notifyDisconnected()
                    } else {
                        if (prevConnectedState == 0) {
                            connManager.notifyConnected()
                        } else {
                            connManager.notifyConnectTypeChanged()
                        }
                    }
                }
            }
        }
    }
}
