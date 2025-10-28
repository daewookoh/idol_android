package com.example.idol_android.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 모든 Activity의 Base가 되는 추상 클래스.
 * ActionBar 제어 및 공통 기능을 제공.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 기본적으로 ActionBar 숨김
        supportActionBar?.hide()
    }

    /**
     * ActionBar를 표시.
     * 필요한 화면에서 호출.
     */
    protected fun showActionBar() {
        supportActionBar?.show()
    }

    /**
     * ActionBar를 숨김.
     */
    protected fun hideActionBar() {
        supportActionBar?.hide()
    }

    /**
     * ActionBar 타이틀 설정.
     */
    protected fun setActionBarTitle(title: String) {
        supportActionBar?.title = title
    }

    /**
     * ActionBar 타이틀 설정 및 표시.
     */
    protected fun setActionBarTitleAndShow(title: String) {
        supportActionBar?.apply {
            this.title = title
            show()
        }
    }

    /**
     * ActionBar의 뒤로가기 버튼 활성화.
     */
    protected fun enableActionBarBackButton(enable: Boolean = true) {
        supportActionBar?.setDisplayHomeAsUpEnabled(enable)
    }

    /**
     * ActionBar 완전히 커스터마이징.
     */
    protected fun customizeActionBar(block: androidx.appcompat.app.ActionBar.() -> Unit) {
        supportActionBar?.apply(block)
    }

    /**
     * 뒤로가기 버튼 클릭 시 호출.
     * 각 Activity에서 오버라이드하여 사용.
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
