/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.viewholder

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import net.ib.mn.databinding.WidePhotoItemBinding
import net.ib.mn.model.RemoteFileModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util

class PhotoGifViewHolder (
    val binding: WidePhotoItemBinding,
    val context: Context
): RecyclerView.ViewHolder(binding.root) {

    var glideRequestManager: RequestManager = Glide.with(context)

    fun bind(remoteFileModel: RemoteFileModel) = with(binding){
        // 이미지
        if(!Const.FEATURE_VIDEO || remoteFileModel.umjjalUrl == null) {
            videoView.visibility = View.GONE
            photoView.visibility = View.VISIBLE

            glideRequestManager
                .asBitmap()
                .load(remoteFileModel.originUrl)
                .into(photoView)

            return@with
        }

        // 움짤
        videoView.visibility = View.VISIBLE
        photoView.visibility = View.GONE

        videoView.setShouldRequestAudioFocus(false)

        // video cache
        videoView.setVideoURI(Uri.parse(remoteFileModel.umjjalUrl))

        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            mediaPlayer.start()
            Util.closeProgress()
        }

        // 일부 삼성폰에서 setLooping이 안먹어서
        videoView.setOnCompletionListener {
            videoView.setVideoURI(Uri.parse(remoteFileModel.umjjalUrl))
            videoView.start()
        }

        videoView.setOnErrorListener { mediaPlayer, what, extra ->
            // ApiResources.postUserLog(WidePhotoActivity.this, "user.video", "ERROR what="+what+" extra="+extra)
            Util.closeProgress()
            true // true: Don't show "Sorry, this video cannot be played"
        }
    }
}