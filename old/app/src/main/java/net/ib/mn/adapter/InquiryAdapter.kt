package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.activity.MyInquiryActivity.Companion.createIntent
import net.ib.mn.databinding.InquiryItemBinding
import net.ib.mn.model.InquiryModel
import net.ib.mn.utils.LocaleUtil
import java.text.DateFormat
import java.util.*

/**
 * Copyright 2022-12-7,수,16:1. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 나의 문의 내역 리스트 보여주는 adapter
 *
 **/

class InquiryAdapter(
    private val context : Context,
    private var mItems: ArrayList<InquiryModel>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = InquiryItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return InquiryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        InquiryViewHolder((holder as InquiryViewHolder).binding).bind(mItems[position])
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun addAll(mItems: ArrayList<InquiryModel>){
        this.mItems = mItems
        notifyDataSetChanged()
    }

    inner class InquiryViewHolder(val binding: InquiryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(inquiryModel : InquiryModel) = with(binding) {
            // 언어로 인한 크래시 수정
            val format: String = context.getString(R.string.inquiry_time_format)
            val f = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, LocaleUtil.getAppLocale(itemView.context))
            val dateString = f.format(inquiryModel.createdAt!!)
            title.text = String.format(format, dateString)
            header.setOnClickListener {
                context.startActivity(createIntent(context, inquiryModel, title.text.toString())
                )
            }
        }
    }
}