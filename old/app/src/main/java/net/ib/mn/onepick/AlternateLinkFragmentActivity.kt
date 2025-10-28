package net.ib.mn.onepick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.R
import net.ib.mn.BuildConfig
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.data.model.MainChartModel
import net.ib.mn.databinding.ActivityLinkOnePickBinding
import net.ib.mn.feature.rookie.RookieContainerFragment
import net.ib.mn.fragment.HallOfFameFragment
import net.ib.mn.fragment.HeartPickFragment
import net.ib.mn.fragment.MiracleMainFragment
import net.ib.mn.fragment.MiracleMainFragment.Companion.ARG_CHART_MODEL
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.liveStreaming.LiveStreamingListFragment
import java.util.ArrayList
import net.ib.mn.core.domain.usecase.GetChartsUseCase
import net.ib.mn.core.model.ChartModel
import net.ib.mn.utils.Toast
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject

@AndroidEntryPoint
class AlternateLinkFragmentActivity : BaseActivity() {
    @Inject
    lateinit var getChartsUseCase: GetChartsUseCase

    private lateinit var binding: ActivityLinkOnePickBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_link_one_pick)
        binding.clContainer.applySystemBarInsets()

        initSet()
        transactionFragment()
    }

    private fun initSet() {
        val title = intent.getStringExtra(EXTRA_TITLE)

        val actionbar = supportActionBar
        actionbar?.title = title
    }

    private fun transactionFragment() {
        val linkStatus = intent.getStringExtra(EXTRA_LINK_STATUS)

        lifecycleScope.launch {
            val transactionFragment = getFragment(linkStatus ?: return@launch)

            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.fl_fragment_container,
                transactionFragment ?: return@launch,
                ALTERNATE_LINK_FRAGMENT_ACTIVITY,
            )
            transaction.commit()
        }

    }

    private suspend fun getFragment(statue: String): Fragment? = withContext(Dispatchers.Main) {
        return@withContext when (statue) {
            LinkStatus.ONE_PICK.status -> {
                if (BuildConfig.CELEB) {
                    ThemePickMainFragment()
                }

                val isImagePick = intent.getBooleanExtra(EXTRA_IS_IMAGEPICK, false)

                OnePickMainFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean(EXTRA_IS_IMAGEPICK, isImagePick)
                        putBoolean(EXTRA_IS_FROM_ALTERNATE_LINK_FRAGMENT_ACTIVITY, true)
                    }
                }
            }
            LinkStatus.HOF.status -> {
                val chartModelList = getMainChart()

                if (BuildConfig.CELEB) {
                    HallOfFameFragment()
                } else {
                    val hallOfFameBundle =
                        Bundle().apply {
                            putParcelableArrayList(HallOfFameFragment.ARG_MALE_CHART_CODE, ArrayList(chartModelList?.males ?: arrayListOf()))
                            putParcelableArrayList(HallOfFameFragment.ARG_FEMALE_CHART_CODE, ArrayList(chartModelList?.females ?: arrayListOf()))
                        }

                    HallOfFameFragment().apply {
                        arguments = hallOfFameBundle
                    }
                }
            }

            LinkStatus.MIRACLE.status -> {
                val chartModelList = getLiveChart("M")

                MiracleMainFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList(ARG_CHART_MODEL, ArrayList(chartModelList))
                        putBoolean(EXTRA_IS_FROM_ALTERNATE_LINK_FRAGMENT_ACTIVITY, true)
                    }
                }

            }

            LinkStatus.LIVE.status -> {
                LiveStreamingListFragment()
            }

            LinkStatus.HEARTPICK_MAIN.status -> {
                HeartPickFragment()
            }

            LinkStatus.ROOKIE.status -> {
                val chartModelList = getLiveChart("R")
                RookieContainerFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList(ARG_CHART_MODEL, ArrayList(chartModelList))
                        putBoolean(EXTRA_IS_FROM_ALTERNATE_LINK_FRAGMENT_ACTIVITY, true)
                    }
                }
            }

            else -> {
                null
            }
        }
    }

    private suspend fun getLiveChart(type: String): List<ChartModel> = coroutineScope {
        getChartsUseCase().mapNotNull { response ->
            response.message?.let {
                Toast.makeText(
                    context = this@AlternateLinkFragmentActivity,
                    it,
                    Toast.LENGTH_SHORT
                ).show()
                return@mapNotNull null
            }
            response.objects?.filter { it.type == type }
        }.first()
    }

    private suspend fun getMainChart(): MainChartModel? = coroutineScope {
        getChartsUseCase().mapNotNull { response ->
            response.message?.let {
                Toast.makeText(
                    context = this@AlternateLinkFragmentActivity,
                    it,
                    Toast.LENGTH_SHORT
                ).show()
                return@mapNotNull null
            }
            response.main
        }.firstOrNull()
    }

    companion object {
        const val ALTERNATE_LINK_FRAGMENT_ACTIVITY = "alternate_link_fragment_activity"

        fun createIntent(context: Context): Intent {
            return Intent(context, AlternateLinkFragmentActivity::class.java)
        }
    }
}