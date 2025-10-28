package com.example.idol_android.util

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/**
 * ActionBar 제어를 위한 Extension Functions.
 * Activity나 Fragment에서 간편하게 ActionBar를 제어할 수 있도록 제공.
 */

/**
 * Activity에서 ActionBar를 표시.
 */
fun AppCompatActivity.showActionBar() {
    supportActionBar?.show()
}

/**
 * Activity에서 ActionBar를 숨김.
 */
fun AppCompatActivity.hideActionBar() {
    supportActionBar?.hide()
}

/**
 * Activity에서 ActionBar 타이틀 설정.
 */
fun AppCompatActivity.setActionBarTitle(title: String) {
    supportActionBar?.title = title
}

/**
 * Activity에서 ActionBar 타이틀 설정 및 표시.
 */
fun AppCompatActivity.setActionBarTitleAndShow(title: String) {
    supportActionBar?.apply {
        this.title = title
        show()
    }
}

/**
 * Activity에서 ActionBar 뒤로가기 버튼 활성화.
 */
fun AppCompatActivity.enableActionBarBackButton(enable: Boolean = true) {
    supportActionBar?.setDisplayHomeAsUpEnabled(enable)
}

/**
 * Fragment에서 ActionBar를 표시.
 */
fun Fragment.showActionBar() {
    (activity as? AppCompatActivity)?.supportActionBar?.show()
}

/**
 * Fragment에서 ActionBar를 숨김.
 */
fun Fragment.hideActionBar() {
    (activity as? AppCompatActivity)?.supportActionBar?.hide()
}

/**
 * Fragment에서 ActionBar 타이틀 설정.
 */
fun Fragment.setActionBarTitle(title: String) {
    (activity as? AppCompatActivity)?.supportActionBar?.title = title
}

/**
 * Fragment에서 ActionBar 타이틀 설정 및 표시.
 */
fun Fragment.setActionBarTitleAndShow(title: String) {
    (activity as? AppCompatActivity)?.supportActionBar?.apply {
        this.title = title
        show()
    }
}

/**
 * Fragment에서 ActionBar 뒤로가기 버튼 활성화.
 */
fun Fragment.enableActionBarBackButton(enable: Boolean = true) {
    (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(enable)
}
