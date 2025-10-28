package net.ib.mn.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.core.data.model.MainChartModel
import net.ib.mn.databinding.ActivityVotingCertificateListBinding
import net.ib.mn.feature.votingcertificate.VotingCertificateActivity
import net.ib.mn.feature.votingcertificate.VotingCertificateListScreen
import net.ib.mn.fragment.FavoritIdolFragment
import net.ib.mn.utils.Const
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.viewmodel.VotingCertificateListViewModel

@AndroidEntryPoint
class VotingCertificateListActivity : BaseActivity() {

    private lateinit var binding: ActivityVotingCertificateListBinding
    private val viewModel: VotingCertificateListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_voting_certificate_list)
        binding.lifecycleOwner = this
        binding.flContainer.applySystemBarInsets()

        supportActionBar?.let {
            it.title = getString(R.string.certificate_title)
        }

        val mainChartCode: MainChartModel? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    Const.CHART_CODE_FOR_CERTIFICATE,
                    MainChartModel::class.java
                )
            } else {
                intent.getParcelableExtra(Const.CHART_CODE_FOR_CERTIFICATE) as MainChartModel?
            }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                supportActionBar?.show()
            }
        }

        binding.cvVotingCertificate.setContent {
            MaterialTheme {
                VotingCertificateListScreen(
                    moveFavorite = {
                        supportActionBar?.hide()
                        val fragmentManager = supportFragmentManager

                        val fragment = FavoritIdolFragment().apply {
                            arguments = Bundle().apply {
                                putParcelable(Const.CHART_CODE_FOR_CERTIFICATE, mainChartCode)
                                putBoolean(FavoritIdolFragment.PARAM_IS_FROM_VOTING_CERTIFICATE, true)
                            }
                        }

                        val transaction = fragmentManager.beginTransaction()
                        transaction.replace(
                            android.R.id.content,
                            fragment,
                            "favoritIdolFragmentTag"
                        )
                            .addToBackStack(null)
                            .commit()
                    },
                    moveCertificate = {
                        val intent = Intent(this, VotingCertificateActivity::class.java)
                        intent.putExtra(VotingCertificateActivity.CERTIFICATE_DATA, it)
                        startActivity(intent)
                    },
                    error = {
                        finish()
                    }
                )
            }
        }
    }
}