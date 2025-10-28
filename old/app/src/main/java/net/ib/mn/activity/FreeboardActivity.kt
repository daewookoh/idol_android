package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import net.ib.mn.R
import net.ib.mn.fragment.FreeboardFragment
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util


@UnstableApi
open class FreeboardActivity : BoardCelebActivity(R.layout.activity_fragment_container), HasFreeboard {
    override var freeboardFragment : FreeboardFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 글작성 완료 노티 누른 경우
        val tag = intent.getIntExtra(Const.EXTRA_TAG_ID, -1) ?: -1
        if (tag != -1) {
            Util.setPreference(this, Const.SELECTED_TAG_IDS, tag.toString())
        }

        freeboardFragment = FreeboardFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, freeboardFragment!!, "free")
        transaction.commitAllowingStateLoss()

        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.hometab_title_freeboard)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 자게 열려있는 상태 처리
        val tag = intent.getIntExtra(Const.EXTRA_TAG_ID, -1) ?: -1
        if (tag != -1) {
            freeboardFragment?.refresh(tag)
            return
        }

        freeboardFragment?.scrollToHeader()
        freeboardFragment?.filterByLatest()
        freeboardFragment?.getArticles(true)
    }

    companion object {
        const val PARAM_ARTICLE_ID = "articleId"

        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, FreeboardActivity::class.java)
        }
    }

}
