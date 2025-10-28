package net.ib.mn.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * onSaveInstanceState에서 TransactionTooLargeException 이 발생하는 것을 방지하기 위해
 * 눈에 보이지 않는 Fragment를 생성하고 여기에 데이터를 저장한다.
 * https://stackoverflow.com/questions/41953195/fragment-save-large-list-of-data-on-onsaveinstancestate-how-to-prevent-transac
 *
 */
class DataFragment : Fragment() {

    // data object we want to retain
    private var friendsData : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 여기가 핵심
        retainInstance = true
    }

    fun setFriendsData(data : String) {
        this.friendsData = data
    }

    fun getFriendsData() : String? {
        return friendsData
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}