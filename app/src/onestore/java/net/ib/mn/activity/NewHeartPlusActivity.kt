package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.onestore.iap.api.PurchaseClient
import com.onestore.iap.api.PurchaseClient.ServiceConnectionListener
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.billing.util.onestore.AppSecurity
import net.ib.mn.databinding.ActivityNewHeartPlusBinding
import net.ib.mn.fragment.HeartPlusFragment2
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.StoreItemModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets

/**  setContentView(R.layout.activity_new_heartplus)
 * 하트/다이아 상점
 */

@AndroidEntryPoint
class NewHeartPlusActivity : BaseActivity(){

    //원스토어 구매화면 갔다오고 onActivirtyResult에서 써야되기 때문에 전역으로 둬야됨.
    private lateinit var heartFragment:HeartPlusFragment2
    private lateinit var packageFragment:HeartPlusFragment2
    private lateinit var diaFragment:HeartPlusFragment2

    private var mFragList:ArrayList<HeartPlusFragment2> = ArrayList()
    private lateinit var binding: ActivityNewHeartPlusBinding

    private var isSmoothScrollAvailable = true
    private var isSwipeAvailable = true

    // onestore -----------------------------------------------------------------------------------------------------------
    // onestore helper handler
    @JvmField
    var mPurchaseClient: PurchaseClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_heart_plus)
        binding.llContainer.applySystemBarInsets()
        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.label_store)

        initSet()

    }

    private val mServiceConnectionListener: ServiceConnectionListener =
        object : ServiceConnectionListener {
            override fun onConnected() {
                Logger.v("Service connected")
            }

            override fun onDisconnected() {
                Logger.v("Service disconnected")
            }

            override fun onErrorNeedUpdateException() {
                PurchaseClient.launchUpdateOrInstallFlow(this@NewHeartPlusActivity)
            }
        }

    private fun initSet() {
        // PurchaseClient 초기화 - context 와 Signature 체크를 위한 public key 를 파라미터로 넘겨줍니다.
        mPurchaseClient = PurchaseClient(this, AppSecurity.getPublicKey())

        // 원스토어 서비스로 인앱결제를 위한 서비스 바인딩을 요청합니다.
        mPurchaseClient?.connect(mServiceConnectionListener)

        heartplusValue = intent.getStringExtra("goods")


        //서포트 화면에서 온경우에는  스와이프 모션 안보여줘야되므로,  해당 값들  false 처리
        if(intent.getBooleanExtra(IS_FROM_SUPPORT_SCREEN,false)){
            isSmoothScrollAvailable = false
            isSwipeAvailable = false
        }

        //프래그먼트 변수는 무조건 bundle로 넘겨줘서 초기화 시켜야됨.
        val bundleHeart = Bundle()
        val bundlePackage = Bundle()
        val bundleDia = Bundle()

        //하트상점.
        heartFragment = HeartPlusFragment2()
        bundleHeart.putString("goods", "H")
        heartFragment.arguments = bundleHeart

        //패키지 상점.
        packageFragment = HeartPlusFragment2()
        bundlePackage.putString("goods", "P")
        packageFragment.arguments = bundlePackage

        //다이아 상점.
        diaFragment = HeartPlusFragment2()
        bundleDia.putString("goods", "D")
        diaFragment.arguments = bundleDia

        mFragList.add(heartFragment)
        mFragList.add(packageFragment)
        mFragList.add(diaFragment)

        //뷰페이저 세팅
        binding.vpHeartCharge.apply {
            adapter = PagerAdapter(this@NewHeartPlusActivity, mFragList)
            offscreenPageLimit = 3
            isUserInputEnabled = isSwipeAvailable
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

        //원스토어 구매화면 갔다오고 난후 여기로 와지는데 해당 heartplusValue로 구분해서 해당 프래그먼트에 onActivityResult를 불러줘야됨.
        when (heartplusValue) {
            FRAGMENT_HEART_SHOP -> {
                if(this::heartFragment.isInitialized){
                    heartFragment.onActivityResult(requestCode, resultCode, data)
                }
            }
            FRAGMENT_DIAMOND_SHOP -> {
                if(this::diaFragment.isInitialized){
                    diaFragment.onActivityResult(requestCode, resultCode, data)
                }
            }
            else -> {
                if(this::packageFragment.isInitialized){
                    packageFragment.onActivityResult(requestCode, resultCode, data)
                }
            }
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

    override fun onDestroy() {
        super.onDestroy()
        if (mPurchaseClient == null) {
            return
        }

        // 앱 종료시 PurchaseClient를 이용하여 서비스를 terminate 시킵니다.
        mPurchaseClient?.terminate()
    }

    companion object {
        private const val SELECTED_FRAGMENT = "selected_fragment"
        const val FRAGMENT_HEART_SHOP = "H"
        const val FRAGMENT_DIAMOND_SHOP = "D"
        const val FRAGMENT_PACKAGE_SHOP = "P"
        const val IS_FROM_SUPPORT_SCREEN = "isFromSupport"

        @JvmField
        var heartplusValue: String? = null
        private const val REQUEST_CODE_SHOP = 100

        //하트상점 첫번째 아이템 모델.
        @JvmField
        var firstItemHeartModel = StoreItemModel()

        //다이아몬드 상점 첫번째 아이템 모델.
        @JvmField
        var firstItemDiaModel = StoreItemModel()

        // onestore : 구매시도시 퍼미션 검사
        const val REQUEST_PERMISSION_AND_PAY = 2

        @JvmStatic
        @JvmOverloads
        fun createIntent(context: Context?, type: String? = if (ConfigModel.getInstance(context).showAwardTab && !"A".equals(ConfigModel.getInstance(context).votable, ignoreCase = true)) FRAGMENT_DIAMOND_SHOP else FRAGMENT_HEART_SHOP, isFromSupportDia:Boolean): Intent {
            heartplusValue = null
            val intent = Intent(context, NewHeartPlusActivity::class.java)
            intent.putExtra(IS_FROM_SUPPORT_SCREEN,true)
            intent.putExtra("goods", type)
            return intent
        }

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