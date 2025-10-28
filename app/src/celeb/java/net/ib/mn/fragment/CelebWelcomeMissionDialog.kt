package net.ib.mn.fragment

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.ContainerActivity
import net.ib.mn.activity.FavoriteSettingActivity
import net.ib.mn.activity.HeartPlusFreeActivity
import net.ib.mn.activity.NewFriendsActivity.Companion.createIntent
import net.ib.mn.core.data.repository.MissionsRepository
import net.ib.mn.databinding.FragmentWelcomeMissionBinding
import net.ib.mn.feature.mission.MissionItemInfo
import net.ib.mn.feature.mission.MissionScreen
import net.ib.mn.feature.mission.MissionUiState
import net.ib.mn.feature.mission.MissionViewModel
import net.ib.mn.feature.mission.MissionViewModelFactory
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Toast
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.setFirebaseUIAction
import javax.inject.Inject

@AndroidEntryPoint
class CelebWelcomeMissionDialog : DialogFragment() {
    private lateinit var binding: FragmentWelcomeMissionBinding
    private lateinit var viewModel: MissionViewModel
    @Inject
    lateinit var missionsRepository: MissionsRepository

    interface WelcomeMissionDialogListener {
        fun onClose(isAllClear: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen
        ) // 풀스크린 스타일 설정
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomeMissionBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        val factory = MissionViewModelFactory(
            missionsRepository
        )
        viewModel = ViewModelProvider(requireActivity(), factory)[MissionViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.let {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it, false)
        }

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            binding.cvMission.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initUI()
        observedVM()
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as WelcomeMissionDialogListener).onClose(viewModel.getIsAllClear())
    }

    private fun initUI() {
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
                                startActivity(
                                    Intent(
                                        requireContext(),
                                        ContainerActivity::class.java
                                    )
                                )
                            }

                            MissionItemInfo.WELCOME_ADD_FRIEND -> {
                                startActivity(createIntent(requireContext()))
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
                                } ?: Toast.makeText(
                                    requireContext(),
                                    requireContext().getString(R.string.impossible_write_message),
                                    Toast.LENGTH_SHORT
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
                        dismiss()
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