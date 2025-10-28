package net.ib.mn.activity

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.ActivityVotingGuideBinding
import net.ib.mn.databinding.ViewLevelGuideBinding
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.formatNumberShort
import net.ib.mn.utils.ext.applySystemBarInsets
import java.text.NumberFormat

/**
 * 나의 하트 > 나의 누적 투표 > ? 누르기
 */
class VotingGuideActivity : BaseActivity() {
    private lateinit var binding: ActivityVotingGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVotingGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.svVotingGuide.applySystemBarInsets()

        supportActionBar?.setTitle(R.string.title_voting_guide)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)

        if (BuildConfig.CELEB) {
            binding.tvGuideDesc.setText(
                R.string.actor_voting_guide_desc1
            )
        }

        val y = 0
        val rows = (Const.LEVEL_HEARTS.size + 1) / 2
        val level2 = rows
        for (i in 0..<rows) {
            val guideBinding = ViewLevelGuideBinding.inflate(layoutInflater)
            val levelGuide = guideBinding.root

            guideBinding.iconLevel1.setImageResource(Util.getLevelResId(this, i))

            var inputStringReq1 = NumberFormat.getNumberInstance(getAppLocale(this))
                .format(Const.LEVEL_HEARTS[i].toLong())
            if (!Util.getSystemLanguage(this).equals("ko_KR", ignoreCase = true)) {
                inputStringReq1 = formatNumberShort(Const.LEVEL_HEARTS[i])
            }

            guideBinding.textLevelRequirement1.text = String.format(
                getResources().getString(R.string.level_requirement_format)
                    .replace("\$d", "\$s"),
                inputStringReq1
            )
            guideBinding.textLevelBenefit1.text = String.format(
                getResources().getString(R.string.level_benefit_format),
                getLevelBonus(i)
            )

            if (i + level2 < Const.LEVEL_HEARTS.size) {
                var inputStringReq2 = NumberFormat.getNumberInstance(getAppLocale(this))
                    .format(Const.LEVEL_HEARTS[i + level2].toLong())
                if (!Util.getSystemLanguage(this).equals("ko_KR", ignoreCase = true)) {
                    inputStringReq2 = formatNumberShort(Const.LEVEL_HEARTS[i + level2])
                }

                guideBinding.iconLevel2.setImageResource(Util.getLevelResId(this, i + level2))
                guideBinding.textLevelRequirement2.text = String.format(
                    getResources().getString(R.string.level_requirement_format)
                        .replace("\$d", "\$s"),
                    inputStringReq2
                )
                guideBinding.textLevelBenefit2.text = String.format(
                    getResources().getString(R.string.level_benefit_format),
                    getLevelBonus(i + level2)
                )
            } else {
                guideBinding.iconLevel2.setImageDrawable(null)
                guideBinding.textLevelRequirement2.text = ""
                guideBinding.textLevelBenefit2.text = ""
            }

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, Util.convertDpToPixel(this, -1f).toInt(), 0, 0)
            levelGuide.setLayoutParams(lp)
            binding.levelContainer.addView(levelGuide)
        }
    }

    private fun getLevelBonus(level: Int): String {
        return (level * 10).toString()
    }
}
