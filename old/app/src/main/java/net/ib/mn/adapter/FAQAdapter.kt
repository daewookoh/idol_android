package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.activity.FaqActivity.Companion.createIntent
import net.ib.mn.activity.FaqWriteActivity.Companion.beforeActivity
import net.ib.mn.activity.FaqWriteActivity.Companion.createIntent
import net.ib.mn.databinding.FaqItemBinding
import net.ib.mn.model.FAQModel
import java.util.*

/**
 * Copyright 2022-12-7,수,16:4. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 자주 묻는 질문 리스트 처리한 adapter
 *
 **/

class FAQAdapter (
    private val context : Context,
    private var mItems: ArrayList<FAQModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = FaqItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return FaqViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as FaqViewHolder).bind(mItems[position])
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun addAll(mItems: ArrayList<FAQModel>){
        this.mItems = mItems
        notifyDataSetChanged()
    }

    inner class FaqViewHolder(val binding: FaqItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(faqModel : FAQModel) = with(binding) {

            title.text = faqModel.title
            header.setOnClickListener {
                context.startActivity(createIntent(context, faqModel)
                )
            }

            //마지막 포지션일 경우
            if (mItems[mItems.size-1] == faqModel) {
                faqMore.visibility = View.VISIBLE
                btnFaq.visibility = View.VISIBLE
                btnFaq.setOnClickListener {
                    beforeActivity = 0
                    context.startActivity(createIntent(context, beforeActivity))
                }
            } else {
                faqMore.visibility = View.GONE
                btnFaq.visibility = View.GONE
            }
        }
    }
}