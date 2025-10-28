package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import net.ib.mn.R
import net.ib.mn.databinding.ActivityNewHeartPlusBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.paymentwall.pwunifiedsdk.core.PaymentSelectionActivity
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.account.IdolAccount
import net.ib.mn.billing.util.PWManager
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.fragment.HeartPlusChinaFragment
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.StoreItemModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsetsAndRequest
import javax.inject.Inject

/**  setContentView(R.layout.activity_new_heartplus)
 * 하트/다이아 상점
 */

@AndroidEntryPoint
class NewHeartPlusActivity : BaseActivity(){

    private var mFragList:ArrayList<HeartPlusChinaFragment> = ArrayList()
    private lateinit var binding: ActivityNewHeartPlusBinding

    @Inject
    lateinit var usersRepository: UsersRepository

    private var currency: String = ""
    private var price: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_heart_plus)
        binding.llContainer.applySystemBarInsetsAndRequest()
        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.label_store)

        initSet()
    }

    fun getPriceAmountMicros() = 0L

    fun setPriceAmountMicros(priceAmountMicros: Long) {}

    private fun initSet() {

        heartplusValue = intent.getStringExtra("goods")

        //프래그먼트 변수는 무조건 bundle로 넘겨줘서 초기화 시켜야됨.
        val bundleHeart = Bundle()
        val bundlePackage = Bundle()
        val bundleDia = Bundle()

        val heartFragment = HeartPlusChinaFragment()
        bundleHeart.putString("goods", "H")
        heartFragment.arguments = bundleHeart

        val packageFragment = HeartPlusChinaFragment()
        bundlePackage.putString("goods", "P")
        packageFragment.arguments = bundlePackage

        val diaFragment = HeartPlusChinaFragment()
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
                    mFragList[position].getWelcomePrice()
                }
            }
        }

        binding.vpHeartCharge.registerOnPageChangeCallback(pageCallBack)

        //상단 tablayout 설정
        binding.tabHeartCharge.setBackgroundResource(R.drawable.btn_down_two_tab)
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

        //처음 들어왔을떄 위치 지정.
        when (heartplusValue) {
            FRAGMENT_DIAMOND_SHOP -> {
                //맨처음 위치가  다이아몬드일때는 ->  slide 애니메이션이 보임으로  viewpager의  currentitem으로  체크해서
                //먼저 보여질  item을 지정해준다.
                //tab_heart_charge.getTabAt(2)?.select()
                binding.vpHeartCharge.setCurrentItem(2,false)
            }
            FRAGMENT_PACKAGE_SHOP -> {
                binding.tabHeartCharge.getTabAt(1)?.select()
            }
            else -> {
                binding.tabHeartCharge.getTabAt(0)?.select()
            }
        }

    }



    private class PagerAdapter(
        fm: FragmentActivity,
        val fragList: ArrayList<HeartPlusChinaFragment>
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

    // TODO: 2021/05/12 각 페이  event에  맞춰  알맞은  페이  기능을 넣어준다.
    //결제 수단  체크
    fun selectPaymentMethod(storeItemModel: StoreItemModel) {
        var price = storeItemModel.priceCNY
        var currency = "CNY"
        if (price == 0.0) {
            price = storeItemModel.priceEn
            currency = "USD"
        }
        val price02 = price
        val curreny02 = currency
//        if (paymentMethod == Const.PAYMENT_ALIPAY || paymentMethod == Const.PAYMENT_UNIONPAY || paymentMethod == Const.PAYMENT_WECHATPAY) { // 알리페이, 유니온페이
//            v(storeItemModel.name.toString())
//            v("스토어 아이템 체크 " + storeItemModel.name.toString())
            PWManager.getInstance().purchase(
                this,
                usersRepository,
                lifecycleScope,
                IdolAccount.getAccount(this)?.userModel?.id ?: 0,
                IdolAccount.getAccount(this)?.userModel?.email ?: "",
                storeItemModel.skuCode,
                storeItemModel.name,
                price02,
                curreny02,
                storeItemModel.imageUrl
            )
//        }
//        else if (paymentMethod == Const.PAYMENT_WECHATPAY) { //위쳇 페이
//            v(storeItemModel.name.toString())
//            v("스토어 아이템 체크 " + storeItemModel.name.toString())
//            if (ConfigModel.getInstance(this).showAwardTab) {
//                Util.showDefaultIdolDialogWithRedBtn2(this,
//                    null,
//                    getString(R.string.popup_award_diamond_alert),
//                    getString(R.string.popup_award_diamond_alert),
//                    R.string.confirm,
//                    R.string.btn_cancel,
//                    false,
//                    { v: View? ->
//                        NativeXManager.getInstance().purchase(
//                            this,
//                            IdolAccount.getAccount(this).userModel.id,
//                            IdolAccount.getAccount(this).userModel.email,
//                            storeItemModel.skuCode,
//                            storeItemModel.name,
//                            price02,
//                            curreny02
//                        )
//                    }
//                ) { v: View? ->
//                    Util.closeProgress()
//                    Util.closeIdolDialog()
//                }
//            } else {
//                NativeXManager.getInstance().purchase(
//                    this,
//                    IdolAccount.getAccount(this).userModel.id,
//                    IdolAccount.getAccount(this).userModel.email,
//                    storeItemModel.skuCode,
//                    storeItemModel.name,
//                    price,
//                    currency
//                )
//            }
//        }
//        else { //그외 사항  만약에 오면 처리
//            v("스토어 아이템 체크 " + storeItemModel.name.toString())
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Util.log("requestCode:$requestCode   resultCode:$resultCode")

        //페이먼트월 패키지 상점 업데이트 때문에 추가.
        if(requestCode == PaymentSelectionActivity.REQUEST_CODE) {
            for(i in 0 until mFragList.size){
                mFragList[i].onActivityResult(requestCode, resultCode, data)
            }
        }

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

    fun onFragmentLoadingComplete() {
        // 차이나 용 빈 함수
    }

    fun setPrice(price: String) {
        this.price = price
    }

    fun getPrice() = price

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