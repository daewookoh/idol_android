package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.databinding.ActivityFragmentContainerBinding
import net.ib.mn.fragment.MyheartInfoFragment
import net.ib.mn.utils.ext.applySystemBarInsets

@AndroidEntryPoint
class MyHeartInfoActivity:BaseActivity() {

    private lateinit var binding : ActivityFragmentContainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionbar = supportActionBar
        actionbar?.setTitle(R.string.my_info_title)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_fragment_container)
        binding.clContainer.applySystemBarInsets()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(
            binding.fragmentContainer.id,
            MyheartInfoFragment(),
            "my_heart_container"
        )
        transaction.commit()
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, MyHeartInfoActivity::class.java)
        }
    }
}