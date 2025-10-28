package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import net.ib.mn.R
import net.ib.mn.databinding.ActivityIdolQuizDenyBinding
import net.ib.mn.utils.ext.applySystemBarInsets

class IdolQuizDenyActivity : BaseActivity() {
    private lateinit var binding: ActivityIdolQuizDenyBinding
    private var actionbar: ActionBar? = null

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, IdolQuizDenyActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdolQuizDenyBinding.inflate(layoutInflater)
        binding.clContainer.applySystemBarInsets()
        setContentView(binding.root)

        actionbar = supportActionBar
        actionbar?.title = getString(R.string.quiz_deny_reason_title)
    }


}