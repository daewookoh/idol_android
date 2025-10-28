package net.ib.mn.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import net.ib.mn.R
import net.ib.mn.databinding.MultiFileItemBinding

/**
 * Copyright 2022-12-5,월,14:53. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 문의하기, 커뮤니티, 자유게시판, 지식돌, 잡담 Multi Upload Adapter
 *
 **/

class WriteMultiImgAdapter(
    private val context: Context,
    private val isEdit: Boolean,
    private val mGlideRequestManager: RequestManager,
    private var mItemsUriArray : ArrayList<Uri>,
    private val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //이미지 보내는데 성공했는지 체크. writeCdnMultipart 응답오는 부분에서 성공했을 때 true로 변경됨
    private var isSuccessed = false
    private var sendingPosition = 0
    private var mimeType: Int = MIME_TYPE_IMAGE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val faqFileItem : MultiFileItemBinding= DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.multi_file_item,
            parent,
            false,
        )

        return WriteMultiImgViewHolder(faqFileItem)
    }

    interface OnItemClickListener {
        fun onDeletedClickListener(position: Int)   //올린 데이터 삭제하는 버튼 클릭 리스너
    }

    //Add, remove 둘다 처리 가능한 함수
    fun setItemsByteArray(
        uriList: ArrayList<Uri>,
        isSuccessed: Boolean
    ) { //isSuccessed : 이미지 보냈을 때는 false로 만들고, 이미지 삭제할 때는 보내고있는 것이 아니므로 true
        this.isSuccessed = isSuccessed
        mItemsUriArray = uriList
        notifyDataSetChanged()
    }

    fun setMimeType(mimeType: Int = MIME_TYPE_IMAGE) {
        this.mimeType = mimeType
    }

    //해당 아이템 프로그래스바 없애주는 함수. writeCdnMultipart 응답왔을 때 처리. 문의하기에서만 사용. 문의하기는 백그라운드에서 동작처리 하지 않음
    fun closeProgressDialog(position: Int){
        isSuccessed = true
        sendingPosition = position
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as WriteMultiImgViewHolder).bind(position)
    }

    override fun getItemCount(): Int {
        return mItemsUriArray.size
    }


    inner class WriteMultiImgViewHolder(val binding: MultiFileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) = with(binding) {
            // 수정으로 들어왔을 경우 삭제 버튼 GONE 처리
            if(isEdit) {
                ivDelete.visibility = View.GONE
            }

            // 아이템이 1개 초과일 경우 맨 첫번째 아이템은 대표 이미지라는 것을 알려주기 위함
            if(mItemsUriArray.size > 1 && position == 0) {
                clImgTitle.visibility = View.VISIBLE
            }

            if (!isSuccessed && mItemsUriArray.size - 1 == position) { //이미지가 보내는 것이 완료되지 않았는데, 마지막 포지션 아이템이라면 프로그래스바 보이도록
                progressBar.visibility = View.VISIBLE
            }
            if (isSuccessed) {    //이미지 보내는 것이 완료됐다면
                progressBar.visibility = View.GONE
            }
            //글라이드 캐싱처리하면 여러번 올릴 때 앱 죽어서 캐싱 안하도록 처리
            mGlideRequestManager
                .load(mItemsUriArray[position])
                .thumbnail(0.1f)    //이미지나 동영상 보낼 때 원본파일 크기 그대로 글라이드에 올리면 OOM나는 문제 해결
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(eivFile)

            ivVideoThumbnail.visibility = if (mimeType == MIME_TYPE_VIDEO) {
                View.VISIBLE
            } else {
                View.GONE
            }

            ivDelete.setOnClickListener {
                onItemClickListener.onDeletedClickListener(position)
            }
        }
    }

    companion object {
        const val MIME_TYPE_IMAGE = 0
        const val MIME_TYPE_VIDEO = 1
    }
}