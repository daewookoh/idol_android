package net.ib.mn.utils

import android.content.Context
import android.widget.Toast

// TODO: 2022/10/12

class Toast  {

    fun show() {
        if(toast != null){
            toast?.show()
        }
    }

    companion object{
        var toast: Toast?= null
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1

        @JvmStatic
        fun makeText(context: Context?, message: String?, duration: Int): net.ib.mn.utils.Toast{    //makeText를 쓰면 해당 Toast 클래스를 return해서 .show()를 쓰게 한다. 그래서 nonNull타입
            if(message.isNullOrEmpty()){
                toast = null
                return Toast()
            }

            toast = setToast(context, message, duration)
            return Toast()
        }

        @JvmStatic
        fun makeText(context: Context?, message: Int, duration: Int):Toast {    //해당 Toast는 안드로이드에서 제공하는 Toast 클래스를 리턴한다. message 값이 int 값이라 null일 수 없음
            return setToast(context, message, duration)
        }

        private fun setToast(
            context: Context?,
            message: String?,
            duration: Int
        ) :Toast{
            return Toast.makeText(context,message,duration)
        }

        private fun setToast(
            context: Context?,
            message: Int,
            duration: Int
        ) :Toast{
            return Toast.makeText(context,message,duration)
        }

    }

}