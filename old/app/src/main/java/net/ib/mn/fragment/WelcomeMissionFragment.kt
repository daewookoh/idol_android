package net.ib.mn.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.FavoriteSettingActivity
import net.ib.mn.activity.HeartPlusFreeActivity
import net.ib.mn.activity.NewFriendsActivity
import net.ib.mn.core.data.repository.MissionsRepository
import net.ib.mn.databinding.FragmentWelcomeMissionBinding
import net.ib.mn.feature.mission.MissionItemInfo
import net.ib.mn.feature.mission.MissionScreen
import net.ib.mn.feature.mission.MissionUiState
import net.ib.mn.feature.mission.MissionViewModel
import net.ib.mn.feature.mission.MissionViewModelFactory
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.setFirebaseUIAction
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeMissionFragment : Fragment() {
    private lateinit var binding: FragmentWelcomeMissionBinding
    private lateinit var viewModel: MissionViewModel
    @Inject
    lateinit var missionsRepository: MissionsRepository

    interface WelcomeMissionDialogListener {
        fun onClose(isAllClear: Boolean)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomeMissionBinding.inflate(inflater, container, false)
        binding.clContainer.applySystemBarInsets()
        binding.lifecycleOwner = this

        val factory = MissionViewModelFactory(
            missionsRepository
        )
        viewModel = ViewModelProvider(requireActivity(), factory)[MissionViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        observedVM()
    }

    override fun onResume() {
        super.onResume()
        if (!::viewModel.isInitialized) return

        if (viewModel.missionUiState.value != MissionUiState.Loading) {
            viewModel.getWelcomeMission()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    private fun initUI() {
        binding.clContainer.setOnClickListener{}
        binding.cvMission.setContent {
            MaterialTheme {
                MissionScreen(
                    viewModel = viewModel,
                    moveScreen = { key ->
                        when (key) {
                            MissionItemInfo.WELCOME_JOIN -> {
                                // no-op 기본으로 완료되어 있음
                            }

                            MissionItemInfo.WELCOME_MOST -> {
                                startActivity(
                                    Intent(
                                        requireContext(),
                                        FavoriteSettingActivity::class.java
                                    )
                                )
                            }

                            MissionItemInfo.WELCOME_VOTE_MOST -> {
                                val fragmentManager = parentFragmentManager
                                val existingFragment =
                                    fragmentManager.findFragmentByTag("favoritIdolFragmentTag") as? FavoritIdolFragment

                                val fragment = existingFragment ?: FavoritIdolFragment().apply {
                                    arguments = Bundle().apply {
                                        putBoolean(FavoritIdolFragment.PARAM_IS_MISSION, true)
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
                            }

                            MissionItemInfo.WELCOME_ADD_FRIEND -> {
                                startActivity(NewFriendsActivity.createIntent(requireContext()))
                            }

                            MissionItemInfo.WELCOME_POSTING -> {
                                IdolAccount.getAccount(context)?.most?.let {
                                    startActivity(
                                        CommunityActivity.createIntent(
                                            requireContext(),
                                            IdolAccount.getAccount(requireContext())?.most
                                                ?: return@MissionScreen
                                        )
                                    )
                                } ?: IdolSnackBar.make(
                                    requireActivity().findViewById(android.R.id.content),
                                    requireContext().getString(R.string.impossible_write_message)
                                ).show()
                            }

                            MissionItemInfo.WELCOME_VIDEO_AD -> {
                                startActivity(HeartPlusFreeActivity.createIntent(requireContext()))
                            }

                            MissionItemInfo.WELCOME_ALL_CLEAR -> {
                                setFirebaseUIAction(GaAction.MISSION_ALL_CLEAR)
                            }
                        }
                    },
                    close = {
                        (activity as WelcomeMissionDialogListener).onClose(viewModel.getIsAllClear())
                        parentFragmentManager.popBackStack()
                    }
                )
            }
        }
    }

    private fun observedVM() {
        viewModel.rewardDialog.observe(viewLifecycleOwner, SingleEventObserver { heart ->
            val mBottomSheetDialogFragment = RewardBottomSheetDialogFragment.newInstance(
                RewardBottomSheetDialogFragment.FLAG_MISSION_COMPLETE,
                heart
            )
            mBottomSheetDialogFragment.show(childFragmentManager, "reward_mission_complete")
        })
    }
}