package net.ib.mn.fragment

import android.content.Context
import android.content.IntentFilter
import net.ib.mn.utils.Const

class NewSoloRankingFragment : NewRankingFragment() {

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(Const.SOLO_LEAGUE_CHANGE)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onAttach(context: Context) {
        type = "S"
        super.onAttach(context)

    }

    override fun getLoaderId(): Int {
        return Const.SOLO_ID
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)
        if(isVisible){
            startLottie()
        }
    }

}
