package net.ib.mn.liveStreaming.viewholder

import android.os.Handler
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import net.ib.mn.databinding.ItemLiveStreamListTopBannerBinding
import net.ib.mn.liveStreaming.LiveTrailerSlideFragment
import net.ib.mn.utils.ext.safeActivity

/**
 * ProjectName: idol_app_renew
 *
 * Description: 라이브 탭  라이브 리스트에서  탑 배너 아이템  뷰홀더
 * */
class LiveStreamTopBannerVH (val binding: ItemLiveStreamListTopBannerBinding
) : RecyclerView.ViewHolder(binding.root) {

    lateinit var slideHandler: Handler
    lateinit var sliderRunnable:Runnable

    fun bind(liveTrailerBannerList:ArrayList<LiveTrailerSlideFragment>){

        //탑배너 데이터가  없을때는  탑배너  view를  gone처리해주어서 리스트에서 안보이게 한다.
        if(liveTrailerBannerList.size<=0){
            binding.tabLiveTrailerIndicator.visibility = View.GONE
            binding.vpLiveTrailer.visibility = View.GONE
            binding.conatiner.visibility = View.GONE
        }else{
            binding.tabLiveTrailerIndicator.visibility = View.VISIBLE
            binding.vpLiveTrailer.visibility = View.VISIBLE
            binding.conatiner.visibility = View.VISIBLE
        }

        val pagerAdapter = ScreenSlidePagerAdapter((itemView.context.safeActivity as FragmentActivity),liveTrailerBannerList)
        binding.vpLiveTrailer.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLiveTrailerIndicator, binding.vpLiveTrailer)
        { tab, position ->

        }.attach()


        sliderRunnable = Runnable {
            //뷰페이저 다음으로 넘김
            binding.vpLiveTrailer.apply {
                if(currentItem == liveTrailerBannerList.size-1){
                    currentItem = 0//마지막 왔을떄는  다시 처음으로 돌아감
                }else{
                    currentItem += 1
                }
            }
        }

        slideHandler = Handler()
        binding.vpLiveTrailer.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                slideHandler.removeCallbacks(sliderRunnable)
                slideHandler.postDelayed(sliderRunnable,2000)
            }
        })


    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity, val liveTrailerBannerList: ArrayList<LiveTrailerSlideFragment>) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = liveTrailerBannerList.size

        override fun createFragment(position: Int): Fragment {
            return  liveTrailerBannerList[position]
        }
    }
}