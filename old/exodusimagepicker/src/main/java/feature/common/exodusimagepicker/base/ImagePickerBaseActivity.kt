package feature.common.exodusimagepicker.base

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.disposables.CompositeDisposable

/**
 * Create Date: 2023/12/04
 * @author jungSangMin
 *
 * Description: BaseActivity =>  상속 받는  ACtivity들은 onCreate() 를 적용하여, 데이터 바인딩 적용.
 * 다른  lifecycle의 경우  base에서  처리할시  onCreate 와 같이 추가하여,  child classs에  적용해준다.
 *
 * @see compositeDisposable -> 각 베이스프래그먼트들을 받는 프래그먼트 안  disposable들 모아서 한번에 dispose 처리 용
 * */
open class ImagePickerBaseActivity<VDB : ViewDataBinding>(@LayoutRes val layoutRes: Int) :
    AppCompatActivity() {
    lateinit var binding: VDB

    // 현재 화면  disposable들 한번에 clear하기 위한 conatiner
    private val compositeDisposable = CompositeDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, layoutRes)
        binding.onCreate()
    }

    // 토스트로 보여주던거  스낵바로 보여주기로해서 이렇게 사용.
    protected fun showSnackBar(msg: String) {
        val snackbar = Snackbar.make(this.findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT)
        val layoutParams = snackbar.view
        val snackText: TextView =
            layoutParams.findViewById(com.google.android.material.R.id.snackbar_text)
        snackText.maxLines = 10
        // layoutParams.translationY = -(convertDpToPixel(requireActivity(),28f))
        snackbar.show()
    }

    protected fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    open fun VDB.onCreate() = Unit

    override fun onDestroy() {
        super.onDestroy()

        // 화면 destroy될때  disposable들 모두 clear -> 메모리 누수 방지
        compositeDisposable.dispose()
    }
}