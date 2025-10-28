/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.myheart

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.MyHeartInfoViewModel
import net.ib.mn.viewmodel.MyHeartInfoViewModelFactory


/**
 * @see
 * */

open class MyHeartInfoBaseFragment<VDB : ViewDataBinding>(
    @LayoutRes val layoutRes: Int,
) :
    BaseFragment(layoutRes) {

    //MyHeartInfoActivity 와 돌려쓰는 뷰모델
//    protected lateinit var myHeartInfoViewModel: MyHeartInfoViewModel

    protected lateinit var binding: VDB

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        myHeartInfoViewModel = ViewModelProvider(
//            requireParentFragment(), MyHeartInfoViewModelFactory(context, SavedStateHandle())
//        )[MyHeartInfoViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, layoutRes, container, false)
        binding.onCreateView()
        return binding.root
    }

    open fun VDB.onCreateView() = Unit

}