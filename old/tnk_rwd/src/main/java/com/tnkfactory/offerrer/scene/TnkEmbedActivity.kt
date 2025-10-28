package com.tnkfactory.offerrer.scene

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tnkfactory.ad.TnkError
import com.tnkfactory.ad.TnkOfferwall
import com.tnkfactory.ad.TnkResultListener
import com.tnkfactory.offerrer.R

/**
 * @author hanago
 * @email hans@tnkfactory.com
 * @since 2023/04/24
 **/
class TnkEmbedActivity : AppCompatActivity() {

    val offerwall: TnkOfferwall by lazy { TnkOfferwall(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_tnk_embed)
        val root = findViewById<View>(R.id.com_tnk_ad_list_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.com_tnk_btn_back).setOnClickListener {
            finish()
        }
        val progress = ProgressDialog(this, 0)
        progress.show()

        offerwall.load(object : TnkResultListener {
            override fun onFail(error: TnkError) {
                progress.dismiss()
                Toast.makeText(this@TnkEmbedActivity, "error : ${error.message}", Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess() {
                progress.dismiss()
                val contents = findViewById<ViewGroup>(R.id.contents)

                offerwall.getAdListView(appId).let {
                    contents.addView(it)
                }
            }
        })
    }


    val appId: Long
        get() = intent.getLongExtra("appId", 0)

    companion object {
        @JvmStatic
        fun start(context: Context, appId: Long? = 0) {
            Intent(context, TnkEmbedActivity::class.java).apply {
                putExtra("appId", appId)
            }.run {
                context.startActivity(this)
            }
        }
    }
}