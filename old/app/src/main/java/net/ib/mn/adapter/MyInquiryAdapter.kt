package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.ItemMyInquiryFilesBinding
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.InquiryModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import java.text.NumberFormat

/**
 * Copyright 2022-12-7,수,16:1. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 나의 문의내역 파일 나열한 adapter
 *
 **/

class MyInquiryAdapter(
    private val context : Context,
    private val inquiryModel: InquiryModel,
    private val onClickListener : OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemMyInquiryFilesBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyInquiryViewHolder(binding)
    }

    interface OnClickListener {
        fun fileClickListener(articleModel: ArticleModel)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MyInquiryViewHolder).bind(position)
    }

    override fun getItemCount(): Int {
        return inquiryModel.file_count
    }

    inner class MyInquiryViewHolder(val binding: ItemMyInquiryFilesBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) = with(binding) {
            if(inquiryModel.file_count != 0 && inquiryModel.files.isNullOrEmpty()){ //파일은 있는데, 파일 uri 없는 경우(30일 지난거라 판단)
                ibFile.setImageResource(R.drawable.btn_faq_file_download_deleted)
            }
            if(!inquiryModel.files.isNullOrEmpty()){
                ibFile.setImageResource(R.drawable.btn_faq_file_download)
                ibFile.setOnClickListener {
                    var model = ArticleModel()
                    val url = inquiryModel.files[position]
                    val ext = Util.getExtensionFromImagePath(url)
                    if( arrayOf("mp4", "mov").contains(ext) ) {
                        model.umjjalUrl = url
                    } else {
                        model.imageUrl = url
                    }
                    onClickListener.fileClickListener(model)
//                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(inquiryModel.files[position]))
//                    context.startActivity(intent)
                }
            }
            tvFileIdx.text = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format((position+1))
        }
    }
}