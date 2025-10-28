package net.ib.mn.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.ActivityNewHeartPlusBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.fragment.HeartPlusFragment2
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.StoreItemModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets

/**  setContentView(R.layout.activity_new_heartplus)
 * 하트/다이아 상점
 */

@AndroidEntryPoint
class NewHeartPlusActivity : BaseActivity(){

    private var mFragList:ArrayList<HeartPlusFragment2> = ArrayList()
    private lateinit var binding: ActivityNewHeartPlusBinding

    private var currency: String = ""
    private var priceAmountMicros: Long = 0L

    private var progressDialog: ProgressDialog? = null

    private var loadingCompletedFragmentCount = 0
    private var isAllFragmentLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 어떤 이유로 인해 로그인이 안된 상태로 진입하는 것 방지
        if(Util.mayShowLoginPopup(this)) {
            return
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_heart_plus)
        binding.llContainer.applySystemBarInsets()

        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.label_store)

        showLottie(this, false)
        initSet()
    }

    private fun initSet() {
        heartplusValue = intent.getStringExtra("goods")

        //프래그먼트 변수는 무조건 bundle로 넘겨줘서 초기화 시켜야됨. 
        val bundleHeart = Bundle()
        val bundlePackage = Bundle()
        val bundleDia = Bundle()

        val heartFragment = HeartPlusFragment2()
        bundleHeart.putString("goods", "H")
        heartFragment.arguments = bundleHeart

        val packageFragment = HeartPlusFragment2()
        bundlePackage.putString("goods", "P")
        packageFragment.arguments = bundlePackage

        val diaFragment = HeartPlusFragment2()
        bundleDia.putString("goods", "D")
        diaFragment.arguments = bundleDia

        mFragList.add(heartFragment)
        mFragList.add(packageFragment)
        mFragList.add(diaFragment)

        //뷰페이저 세팅
        binding.vpHeartCharge.apply {
            adapter = PagerAdapter(this@NewHeartPlusActivity, mFragList)
            offscreenPageLimit = 3
        }

        val pageCallBack = object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //웰컴패키지 할인율이 업데이트 안되었을수도 있으니까 체크해준다.
                if(position == 1){
                    val frag = mFragList[position]
                    if (frag.isAdded && frag.viewLifecycleOwner.lifecycle.currentState.isAtLeast(
                            Lifecycle.State.STARTED)) {
                        frag.getWelcomePrice()
                    }

                }
            }
        }

        binding.vpHeartCharge.registerOnPageChangeCallback(pageCallBack)

        //상단 tablayout 설정
        binding.tabHeartCharge.setBackgroundResource(
            if(BuildConfig.CELEB) R.drawable.btn_down_two_tab
                else R.drawable.btn_down_tab)
        binding.tabHeartCharge.setSelectedTabIndicatorColor(resources.getColor(R.color.main))
        binding.tabHeartCharge.setTabTextColors(
            ContextCompat.getColor(this, R.color.text_dimmed),
            ContextCompat.getColor(this, R.color.main)
        )
        TabLayoutMediator(binding.tabHeartCharge, binding.vpHeartCharge) { tab, position ->
            when (position) {
                0 -> {//하트상점.
                    tab.text = this.getString(R.string.new_shop_heart)
                }
                1 -> {//패키지 상점.
                    tab.text = this.getString(R.string.new_shop_package)
                }
                2 -> {//다이아 상점.
                    tab.text = this.getString(R.string.diamond)
                }
            }
        }.attach()

        with(binding) {
            //처음 들어왔을떄 위치 지정.
            when (heartplusValue) {
                FRAGMENT_DIAMOND_SHOP -> {
                    tabHeartCharge.getTabAt(2)?.select()
                }
                FRAGMENT_PACKAGE_SHOP -> {
                    tabHeartCharge.getTabAt(1)?.select()
                }
                else -> {
                    tabHeartCharge.getTabAt(0)?.select()
                }
            }
        }
    }

    private class PagerAdapter(
        fm: FragmentActivity,
        val fragList: ArrayList<HeartPlusFragment2>
    )
        : FragmentStateAdapter(fm)
    {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> fragList[0]
                1 -> fragList[1]
                else -> fragList[2]
            }
        }
        override fun getItemCount(): Int = 3
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putString(SELECTED_FRAGMENT, heartplusValue)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState != null) {
            if (savedInstanceState.getInt(SELECTED_FRAGMENT) != 0) {
                heartplusValue = savedInstanceState.getString(SELECTED_FRAGMENT)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Util.log("requestCode:$requestCode   resultCode:$resultCode")
        val fragment = supportFragmentManager.findFragmentByTag("heart_shop") as BaseFragment?
        if (fragment != null) {
            Util.log("fragment is not null!")
            fragment.onActivityResult(requestCode, resultCode, data)
        }
        if (requestCode == REQUEST_CODE_SHOP) {
            if (resultCode == Const.RESULT_CODE_FROM_SHOP) {

                Logger.v("onActivityResult:: this is in")
                //MyHeartInfoActivity에서  result로 받도록 setresult진행
                setResult(Const.RESULT_CODE_FROM_SHOP)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun setCurrency(currency: String) {
        this.currency = currency
    }

    fun getCurrency() = currency

    fun setPriceAmountMicros(priceAmountMicros: Long) {
        this.priceAmountMicros = priceAmountMicros
    }

    fun getPriceAmountMicros() = priceAmountMicros

    fun onFragmentLoadingComplete() {
        if (isAllFragmentLoading) return

        loadingCompletedFragmentCount++
        if (loadingCompletedFragmentCount >= 3) {
            isAllFragmentLoading = true
            lifecycleScope.launch {
                delay(500)
                closeProgress()
            }
        }
    }

    fun showLottie(context: Context?, cancelable: Boolean) {
        try {
            progressDialog = ProgressDialog.show(context, null, null, true, cancelable)
            progressDialog?.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            progressDialog?.setContentView(R.layout.lottie_layout)
            progressDialog?.setCanceledOnTouchOutside(false)
        } catch (e: Exception) {
        }
    }

    fun closeProgress() {
        if (progressDialog != null) {
            try {
                progressDialog!!.dismiss()
                progressDialog = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val SELECTED_FRAGMENT = "selected_fragment"
        const val FRAGMENT_HEART_SHOP = "H"
        const val FRAGMENT_DIAMOND_SHOP = "D"
        const val FRAGMENT_PACKAGE_SHOP = "P"
        @JvmField
        var heartplusValue: String? = null
        private const val REQUEST_CODE_SHOP = 100

        //하트상점 첫번째 아이템 모델.
        @JvmField
        var firstItemHeartModel = StoreItemModel()

        //다이아몬드 상점 첫번째 아이템 모델.
        @JvmField
        var firstItemDiaModel = StoreItemModel()

        @JvmStatic
        @JvmOverloads
        fun createIntent(context: Context?,
                         type: String? = if (ConfigModel.getInstance(context).showHeartShop) FRAGMENT_HEART_SHOP else FRAGMENT_DIAMOND_SHOP): Intent {
            val intent = Intent(context, NewHeartPlusActivity::class.java)
            intent.putExtra("goods", type)
            return intent
        }
    }
}