package net.ib.mn.activity

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
        binding.svContainer.applySystemBarInsets()
        setContentView(binding.root)

        with(binding) {
            categoryAnniversary.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categoryMovie.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categoryConcert.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categoryEvent.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categorySign.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categoryProduction.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categoryTvIcon.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categoryVideo.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categoryAwards.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categoryEtc.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categoryPreview.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categoryRadio.setOnClickListener(this@ScheduleWriteCategoryActivity)
            categoryAlbum.setOnClickListener(this@ScheduleWriteCategoryActivity)
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
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_movie -> {
                resultIntent!!.putExtra("category", "movie")
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_concert -> {
                resultIntent!!.putExtra("category", "concert")
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_event -> {
                resultIntent!!.putExtra("category", "event")
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_sign -> {
                resultIntent!!.putExtra("category", "sign")
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_production -> {
                resultIntent!!.putExtra("category", "production")
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_tv_icon -> {
                resultIntent!!.putExtra("category", "tv")
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_radio -> {
                resultIntent!!.putExtra("category", "radio")
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_video -> {
                resultIntent!!.putExtra("category", "live")
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_awards -> {
                resultIntent!!.putExtra("category", "award")
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_etc -> {
                resultIntent!!.putExtra("category", "etc")
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_preview -> {
                resultIntent@ resultIntent!!.putExtra("category", "preview")
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            R.id.category_album -> {
                resultIntent!!.putExtra("category", "albumday")
                setResult(RESULT_OK, resultIntent)
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
