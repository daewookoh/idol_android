package feature.common.exodusimagepicker.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import feature.common.exodusimagepicker.fragment.TrimVideoFragment
import feature.common.exodusimagepicker.fragment.VideoThumbNailPickFragment
import feature.common.exodusimagepicker.viewmodel.VideoPickerViewModel.Companion.VIDEO_EDIT

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 비디오 편집 화면용  뷰페이져이다.
 *
 * @see
 * */
class VideoEditVpAdapter(
    private val fragmentActivity: FragmentActivity,
) : FragmentStateAdapter(fragmentActivity) {

    private val trimVideoFragment = TrimVideoFragment()
    private val thumbNailPickFragment = VideoThumbNailPickFragment()

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            VIDEO_EDIT -> {
                trimVideoFragment
            }
            else -> {
                thumbNailPickFragment
            }
        }
    }

    // 현재 뷰페이져 프래그먼트 모두 제거
    fun removeFragment() {
        fragmentActivity.supportFragmentManager.beginTransaction().remove(trimVideoFragment)
            .commitAllowingStateLoss()
        fragmentActivity.supportFragmentManager.beginTransaction().remove(thumbNailPickFragment)
            .commitAllowingStateLoss()
    }

    // 다듬기 썸네일 화면 총 2개
    override fun getItemCount(): Int = 2
}