package net.ib.mn.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import net.ib.mn.R
import net.ib.mn.databinding.ActivityScheduleWriteCategoryBinding
import net.ib.mn.utils.ext.applySystemBarInsets

class ScheduleWriteCategoryActivity : BaseActivity(), View.OnClickListener {
    private var resultIntent: Intent? = null

    private lateinit var binding: ActivityScheduleWriteCategoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScheduleWriteCategoryBinding.inflate(layoutInflater)
        binding.svScheduleWriteCategory.applySystemBarInsets()
        setContentView(binding.root)

        with(binding) {
            listOf(
                categoryAnniversary,
                categoryAlbum,
                categoryConcert,
                categoryEvent,
                categorySign,
                categoryTvIcon,
                categoryRadio,
                categoryVideo,
                categoryAwards,
                categoryEtc,
                categoryTicketing,
            ).forEach { it.setOnClickListener(this@ScheduleWriteCategoryActivity) }
        }

        resultIntent = Intent()

        val actionbar = supportActionBar
        actionbar!!.title = getString(R.string.schedule_category)
        actionbar.setDisplayHomeAsUpEnabled(true)
        actionbar.setHomeButtonEnabled(false)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.category_anniversary -> {
                resultIntent!!.putExtra("category", "anniversary")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_album -> {
                resultIntent!!.putExtra("category", "albumday")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_concert -> {
                resultIntent!!.putExtra("category", "concert")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_event -> {
                resultIntent!!.putExtra("category", "event")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_sign -> {
                resultIntent!!.putExtra("category", "sign")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_tv_icon -> {
                resultIntent!!.putExtra("category", "tv")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_radio -> {
                resultIntent!!.putExtra("category", "radio")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_video -> {
                resultIntent!!.putExtra("category", "live")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_awards -> {
                resultIntent!!.putExtra("category", "award")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_etc -> {
                resultIntent!!.putExtra("category", "etc")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_ticketing -> {
                resultIntent@ resultIntent!!.putExtra("category", "ticketing")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    companion object {
        fun createIntent(context: Context?): Intent {
            val intent = Intent(
                context,
                ScheduleWriteCategoryActivity::class.java
            )
            return intent
        }
    }
}
