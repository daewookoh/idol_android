package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import net.ib.mn.R
import net.ib.mn.databinding.ActivityFaqBinding
import net.ib.mn.model.FAQModel
import net.ib.mn.utils.ext.applySystemBarInsets

class FaqActivity : BaseActivity(){
    private lateinit var binding: ActivityFaqBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            ITEM_TITLE = it.getString(KEY_TITLE) ?: ""
            ITEM_CONTENT = it.getString(KEY_CONTENT) ?: ""
        }

        init()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TITLE, ITEM_TITLE)
        outState.putString(KEY_CONTENT, ITEM_CONTENT)
    }

    fun init(){
        binding = DataBindingUtil.setContentView(this, R.layout.activity_faq)
        binding.clContainer.applySystemBarInsets()

        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.title_faq)
        binding.inquiryTitle.text = ITEM_TITLE
        binding.inquiryContent.text = ITEM_CONTENT
        binding.faqBtn.setOnClickListener {
            FaqWriteActivity.beforeActivity = 1
            startActivity(FaqWriteActivity.createIntent(this, FaqWriteActivity.beforeActivity ))
        }

    }

    override fun onResume() {
        super.onResume()
        //문의하기를 성공적으로 했을 경우 2로 변경됨. 변경되고 오면 자주묻는 질문 리스트가 보이기 위해 finish
        if(FaqWriteActivity.beforeActivity==2){
            FaqWriteActivity.beforeActivity = 0
            finish()
        }
    }

    companion object {
        var ITEM_TITLE = "item"
        var ITEM_CONTENT = "content"

        val KEY_TITLE = "title"
        val KEY_CONTENT = "content"

        @JvmStatic
        fun createIntent(context: Context, item: FAQModel): Intent {
            ITEM_TITLE = item.title
            ITEM_CONTENT = item.content
            return Intent(context, FaqActivity::class.java)
        }
    }
}


