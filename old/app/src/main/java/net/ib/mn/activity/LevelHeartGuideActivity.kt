package net.ib.mn.activity

import android.os.Bundle
import android.view.View
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.ActivityLevelHeartGuideBinding
import net.ib.mn.utils.ext.applySystemBarInsets

class LevelHeartGuideActivity : BaseActivity() {
    private lateinit var binding: ActivityLevelHeartGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLevelHeartGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.svLevelHeartGuide.applySystemBarInsets()

        supportActionBar?.setTitle(if (BuildConfig.CELEB) R.string.actor_title_level_heart else R.string.title_level_heart)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)

        if (BuildConfig.CELEB) {
            binding.tvGuideTitle.setText(
                R.string.actor_level_heart_guide_title1
            )
            binding.tvGuideDesc.setText(
                R.string.actor_level_heart_guide_desc1
            )
        }
    }
}
