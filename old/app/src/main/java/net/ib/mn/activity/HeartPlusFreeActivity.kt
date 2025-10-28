/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 무료충전소 Activity. offerwall 버튼 및 fragment 보여주는 처리 하는 곳
 *
 * */

package net.ib.mn.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.nextapps.naswall.NASWall
import com.tapjoy.Tapjoy
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.adapter.FreeHeartBtnAdapter
import net.ib.mn.databinding.ActivityFreeHeartBinding
import net.ib.mn.fragment.HeartPlusAppDriverFragment
import net.ib.mn.fragment.HeartPlusFragment1
import net.ib.mn.fragment.HeartPlusMeTabsFragment
import net.ib.mn.utils.permission.PermissionHelper
import net.ib.mn.utils.permission.PermissionHelper.PermissionListener
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.HeartPlusFreeViewModel

@AndroidEntryPoint
class HeartPlusFreeActivity : BaseActivity(), FreeHeartBtnAdapter.OnItemClickListener {

    private lateinit var binding: ActivityFreeHeartBinding
    private var freeHeartBtnAdapter: FreeHeartBtnAdapter? = null
    private val heartPlusFreeViewModel: HeartPlusFreeViewModel by viewModels()

    private var startActivityForResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_free_heart)
        binding.clContainer.applySystemBarInsets()

        init()
        getDataFromVM()
    }

    override fun onStart() {
        super.onStart()
        Tapjoy.onActivityStart(this)
    }

    override fun onStop() {
        Tapjoy.onActivityStop(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun init() {
        // 어떤 이유로 인해 로그인이 안된 상태로 진입하는 것 방지
        if(Util.mayShowLoginPopup(this)) {
            return
        }

        NASWall.init(this, false)

        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.btn_free_heart_charge)

        heartPlusFreeViewModel.setOfferWallTabs(this)
        heartPlusFreeViewModel.fragmentPosition()
        heartPlusFreeViewModel.isRequestPermission()

        FLAG_CLOSE_DIALOG = false
    }

    private fun requestPermission() {
        // 방통위 규제사항 처리
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.GET_ACCOUNTS,
        )
        val msgs = arrayOf(
            getString(R.string.permission_phone),
            getString(R.string.permission_contact),
        )
        PermissionHelper.requestPermissionIfNeeded(
            this,
            null,
            permissions,
            msgs,
            REQUEST_READ_PHONE_STATE,
            object : PermissionListener {
                override fun onPermissionAllowed() {
                }

                override fun onPermissionDenied() {
                    Util.showDefaultIdolDialogWithBtn1(
                        this@HeartPlusFreeActivity,
                        resources.getString(R.string.title_reward_charge),
                        resources.getString(R.string.deny_phone_state_permission),
                    ) { Util.closeIdolDialog() }
                }

                override fun requestPermission(permissions: Array<String>) { }
            },
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        if (requestCode == REQUEST_READ_PHONE_STATE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            Util.showDefaultIdolDialogWithBtn1(
                this,
                resources.getString(R.string.title_reward_charge),
                resources.getString(R.string.deny_phone_state_permission),
            ) { Util.closeIdolDialog() }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun heartFragmentReplace(index: Int) {
        val newFragment: Fragment? = getHeartFragment(index)
        val transaction = supportFragmentManager.beginTransaction()
        when (index) {
            FRAGMENT_NAS_WALL -> {
                transaction.replace(R.id.ll_fragment, newFragment!!, "heartplus1")
            }
            FRAGMENT_METABS -> {
                transaction.replace(R.id.ll_fragment, newFragment!!, "heartmetabs")
            }
            FRAGMENT_APP_DRIVER -> {
                transaction.replace(R.id.ll_fragment, newFragment!!, "heartappdriver")
            }
        }
        transaction.commitAllowingStateLoss()
    }

    private fun getHeartFragment(heartPlusIndex: Int): Fragment? {
        var newFragment: Fragment? = null
        when (heartPlusIndex) {
            FRAGMENT_NAS_WALL -> newFragment = HeartPlusFragment1()
            FRAGMENT_METABS -> newFragment = HeartPlusMeTabsFragment()
            FRAGMENT_APP_DRIVER -> newFragment = HeartPlusAppDriverFragment()
        }
        return newFragment
    }

    override fun onItemClick(offerwall: String, button: AppCompatButton) {
        heartPlusFreeViewModel.fragmentItemClick(this, offerwall)
    }

    private fun getDataFromVM() {
        heartPlusFreeViewModel.showOfferWallTabs.observe(this) { showOfferWallTabs ->
            freeHeartBtnAdapter = FreeHeartBtnAdapter(this, showOfferWallTabs, FRAGMENT_NAS_WALL, this)
            freeHeartBtnAdapter?.setHasStableIds(true)
            binding.rvFreeHeartBtn.adapter = freeHeartBtnAdapter
            freeHeartBtnAdapter?.showIndex(FRAGMENT_NAS_WALL)
        }

        heartPlusFreeViewModel.fragmentOfferWallClick.observe(this) { heartPlusIndex ->
            heartFragmentReplace(heartPlusIndex)
            freeHeartBtnAdapter?.showIndex(heartPlusIndex)
        }

        heartPlusFreeViewModel.isRequestPermission.observe(this) {
            if (!it) {
                requestPermission()
            }
        }

        // 에러팝업.
        heartPlusFreeViewModel.errorPopup.observe(
            this,
            SingleEventObserver { message ->
                Util.showDefaultIdolDialogWithBtn1(
                    this,
                    null,
                    message,
                    { Util.closeIdolDialog() },
                    true,
                )
            },
        )
    }

    companion object {
        private const val REQUEST_READ_PHONE_STATE = 1
        const val FRAGMENT_NAS_WALL = 1
        const val FRAGMENT_METABS = 2
        const val FRAGMENT_APP_DRIVER = 3

        @JvmStatic
        fun createIntent(context: Context?): Intent {
            return createIntent(context, FRAGMENT_NAS_WALL)
        }

        @JvmStatic
        fun createIntent(context: Context?, type: Int): Intent {
            val intent = Intent(context, HeartPlusFreeActivity::class.java)
            intent.putExtra("type", type)
            return intent
        }
    }
}