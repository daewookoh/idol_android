package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.adapter.MyInquiryAdapter
import net.ib.mn.databinding.ActivityMyInquiryBinding
import net.ib.mn.fragment.WidePhotoFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.InquiryModel
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject


/**
 * Copyright 2022-12-7,수,15:59. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 나의 문의 내역 view
 *
 **/
@AndroidEntryPoint
class MyInquiryActivity : BaseActivity(), MyInquiryAdapter.OnClickListener {

    private lateinit var inquiryModel: InquiryModel
    private lateinit var myInquiryAdapter: MyInquiryAdapter
    private lateinit var binding: ActivityMyInquiryBinding
    @Inject
    lateinit var videoAdUtil: VideoAdUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_inquiry)
        binding.clContainer.applySystemBarInsets()

        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.title_myinquirylist)

        binding.titleDate.text = intent.extras?.getString(ITEM_TITLE)

        inquiryModel = intent.extras?.getSerializable(PARAM_INQUIRY) as InquiryModel

        binding.content.text = inquiryModel.content //내가 쓴 문의
        binding.answer.text = inquiryModel.answer   //답변
        setCategory(inquiryModel.category)

        if(inquiryModel.answer.isNullOrEmpty()){ //문의 답변 없을 경우
            binding.answerWrapper.visibility = View.GONE
        }
        if(inquiryModel.file_count == 0){ //보여줄 파일 없을 경우
            binding.clFiles.visibility = View.GONE
        }

        myInquiryAdapter = MyInquiryAdapter(this, inquiryModel, this)
        binding.rvSavedFile.layoutManager = LinearLayoutManagerWrapper(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSavedFile.adapter = myInquiryAdapter
    }

    //카테고리에 따른 텍스트 처리
    private fun setCategory(category : String?){
        with(binding) {
            when (category) {
                CATEGORY_USE -> {
                    tvCategory.text = getString(R.string.inquiry_how_to_use)
                }
                CATEGORY_BUG -> {
                    tvCategory.text = getString(R.string.inquiry_report_bugs)
                }
                CATEGORY_PURCHASE -> {
                    tvCategory.text = getString(R.string.inquiry_purchase_subs)
                }
                CATEGORY_IDEA_PROPOSAL -> {
                    tvCategory.text = getString(R.string.inquiry_proposal)
                }
                CATEGORY_ETC -> {
                    tvCategory.text = getString(R.string.inquiry_etc)
                }else -> {
                    clCategory.visibility = View.GONE
                }
            }
        }
    }

    override fun fileClickListener(articleModel: ArticleModel) {
        WidePhotoFragment.getInstance(true, articleModel).show(supportFragmentManager, "wide_photo")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            MEZZO_PLAYER_REQ_CODE -> {
                Util.handleVideoAdResult(this, false, true, requestCode, resultCode, data, "inquiry_videoad") { adType: String? ->
                    videoAdUtil.onVideoSawCommon(this, true, adType, null) }
            }
        }
    }

    companion object {
        private const val ITEM_TITLE = "item"
        private const val PARAM_INQUIRY = "param_inquiry"
        private const val CATEGORY_USE = "U"
        private const val CATEGORY_BUG = "B"
        private const val CATEGORY_PURCHASE = "P"
        private const val CATEGORY_IDEA_PROPOSAL = "I"
        private const val CATEGORY_ETC = "E"

        @JvmStatic
        fun createIntent(context: Context, item: InquiryModel, title: String): Intent {
            val intent = Intent(context, MyInquiryActivity::class.java)
            val args = Bundle()
            args.putSerializable(PARAM_INQUIRY, item)
            args.putString(ITEM_TITLE, title)
            intent.putExtras(args)
            return  intent
        }
    }

}