/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 무료충전소 상단 offerwall 버튼 보여주는 어댑터
 *
 * */

package net.ib.mn.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.activity.HeartPlusFreeActivity
import net.ib.mn.databinding.FreeHeartBtnItemBinding
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import org.json.JSONArray

class FreeHeartBtnAdapter(
    private val context: Context,
    private val showOfferwallTabs: JSONArray,
    private var showIndex: Int,
    private val onItemClickListener: OnItemClickListener,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val freeHeartBtn: FreeHeartBtnItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.free_heart_btn_item,
            parent,
            false,
        )
        return FreeHeartBtnViewHolder(freeHeartBtn)
    }

    interface OnItemClickListener {
        fun onItemClick(offerwall: String, button: AppCompatButton)
    }

    override fun getItemCount(): Int {
        return showOfferwallTabs.length()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as FreeHeartBtnViewHolder).apply {
            bind(showOfferwallTabs, position)
        }
    }

    fun showIndex(showIndex: Int) {
        this.showIndex = showIndex
        notifyDataSetChanged()
    }

    inner class FreeHeartBtnViewHolder(val binding: FreeHeartBtnItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(showOfferwallTabs: JSONArray, position: Int) {
            binding.btnOfferwall.layoutParams.width =
                Util.convertDpToPixel(context, UtilK.getScreenWidth(context as Activity).toFloat()).toInt() / (showOfferwallTabs.length())

            when (showOfferwallTabs[position].toString()) {
                "naswall" -> {
                    binding.btnOfferwall.text = context.getString(R.string.choeaedol)
                    binding.btnOfferwall.isSelected = showIndex == HeartPlusFreeActivity.FRAGMENT_NAS_WALL
                }
                "appdriver" -> {
                    binding.btnOfferwall.text = context.getString(R.string.offerwall_appdriver)
                    binding.btnOfferwall.isSelected = showIndex == HeartPlusFreeActivity.FRAGMENT_APP_DRIVER
                }
                "metabs" -> {
                    binding.btnOfferwall.text = context.getString(R.string.metaps)
                    binding.btnOfferwall.isSelected = showIndex == HeartPlusFreeActivity.FRAGMENT_METABS
                }
                "tapjoy" -> binding.btnOfferwall.text = context.getString(R.string.tapjoy)
                "ironsource" -> binding.btnOfferwall.text = context.getString(R.string.offerwall_ironsource)
                "tnk" -> binding.btnOfferwall.text = context.getString(R.string.offerwall_tnk)
            }

            if (binding.btnOfferwall.isSelected) {
                binding.btnOfferwall.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.main,
                    ),
                )
            } else {
                binding.btnOfferwall.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.gray200,
                    ),
                )
            }

            binding.btnOfferwall.setOnClickListener {
                onItemClickListener.onItemClick(showOfferwallTabs[position].toString(), binding.btnOfferwall)
            }
        }
    }
}