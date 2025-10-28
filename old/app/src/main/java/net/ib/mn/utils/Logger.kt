package net.ib.mn.utils

import android.util.Log
import net.ib.mn.base.BaseApplication

class Logger private constructor(){
    companion object{

        private val DEBUG = BaseApplication.DEBUG_AVAILABLE
        private const val TAG = "idol_debugging"

        fun v(msg: String) = logger(Log.VERBOSE, msg)
        fun d(msg: String) = logger(Log.DEBUG, msg)
        fun i(msg: String) = logger(Log.INFO, msg)
        fun w(msg: String) = logger(Log.WARN, msg)
        fun e(msg: String) = logger(Log.ERROR, msg)

        //태그 커스텀 용 -> 기존 idol_debugging 태그가 아닌  일반  지정한 태그로  확인 가능
        fun v(tag: String, msg: String) = logger(Log.VERBOSE, tag, msg)
        fun d(tag: String, msg: String) = logger(Log.DEBUG, tag, msg)
        fun i(tag: String, msg: String) = logger(Log.INFO, tag, msg)
        fun w(tag: String, msg: String) = logger(Log.WARN, tag, msg)
        fun e(tag: String, msg: String) = logger(Log.ERROR, tag, msg)

        private fun logger(priority: Int, msg: String) {

            //debug 가능한 상태일때는  log를 출력한다.
            //해당 클래스 이름  메소드 이름 파일이름 및  라인 넘버  출력하게 함.
            if (DEBUG) {
                val message =
                    "[${Thread.currentThread().stackTrace[4].fileName} => "+
                            "${Thread.currentThread().stackTrace[4].methodName}()] :: $msg" +
                            "(${Thread.currentThread().stackTrace[4].fileName}:${Thread.currentThread().stackTrace[4].lineNumber})"
                Log.println(priority, TAG, message)
            }
        }

        private fun logger(priority: Int, tag: String ,msg: String) {

            //debug 가능한 상태일때는  log를 출력한다.
            //해당 클래스 이름  메소드 이름 파일이름 및  라인 넘버  출력하게 함.
            if (DEBUG) {
                val message =
                    "[${Thread.currentThread().stackTrace[4].fileName} => "+
                            "${Thread.currentThread().stackTrace[4].methodName}()] :: $msg" +
                            "(${Thread.currentThread().stackTrace[4].fileName}:${Thread.currentThread().stackTrace[4].lineNumber})"
                Log.println(priority, tag, message)
            }
        }
    }

}