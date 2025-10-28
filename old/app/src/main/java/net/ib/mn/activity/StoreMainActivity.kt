package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import net.ib.mn.R
import net.ib.mn.databinding.ActivityStoreMainBinding
import net.ib.mn.utils.Const
import net.ib.mn.utils.ext.applySystemBarInsets

class StoreMainActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityStoreMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_store_main)
        binding.clStoreMain.applySystemBarInsets()

        setInit()
    }

    private fun setInit() {

        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.label_store)

        with(binding) {
            shopHeartGoIb.setOnClickListener(this@StoreMainActivity)
            shopPackageGoIb.setOnClickListener(this@StoreMainActivity)
            shopDiamondGoIb.setOnClickListener(this@StoreMainActivity)
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_CODE_SHOP) {
           if(resultCode == Const.RESULT_CODE_FROM_SHOP){

                 //MyHeartInfoActivity에서  result로 받도록 setresult진행
                setResult(Const.RESULT_CODE_FROM_SHOP)
           }
        }else{
           super.onActivityResult(requestCode, resultCode, data)
        }

    }

    override fun onClick(v: View?) {
        when(v?.id){
            binding.shopHeartGoIb.id -> {
                setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION, "heart_shop")
                startActivityForResult(NewHeartPlusActivity.createIntent(this, "H"),REQUEST_CODE_SHOP)
            }
            binding.shopPackageGoIb.id -> {
                setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION, "package_shop")
                startActivityForResult(NewHeartPlusActivity.createIntent(this, "P"),REQUEST_CODE_SHOP)
            }
            binding.shopDiamondGoIb.id -> {
                setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION, "diamond_shop")
                startActivityForResult(NewHeartPlusActivity.createIntent(this, "D"),REQUEST_CODE_SHOP)
            }
        }

    }

    companion object {

        const val REQUEST_CODE_SHOP =100

        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, StoreMainActivity::class.java)
        }
    }

}