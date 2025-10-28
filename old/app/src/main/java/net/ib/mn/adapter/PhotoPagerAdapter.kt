/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.WidePhotoItemBinding
import net.ib.mn.model.RemoteFileModel
import net.ib.mn.viewholder.PhotoGifViewHolder

class PhotoPagerAdapter(
    private val context: Context,
    private val remoteFileModelList: List<RemoteFileModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: WidePhotoItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.wide_photo_item,
            parent,
            false,
        )

        return PhotoGifViewHolder(binding, context)
    }

    override fun getItemCount(): Int = remoteFileModelList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as PhotoGifViewHolder).apply {
            bind(remoteFileModelList[position])
        }
    }
}