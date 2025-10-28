package net.ib.mn.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.adapter.NoticeBaseAdapter
import net.ib.mn.databinding.ActivityNoticeBinding
import net.ib.mn.model.NoticeModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets

open class NoticeBaseActivity : BaseActivity(), AdapterView.OnItemClickListener {
    protected var id = 0
    protected var noticeTitle: String? = null // 액션바 타이틀
    protected var keyRead: String? = null // 읽음 기록 키
    protected var type: String? = null // notices or event_list
    protected var adapter: NoticeBaseAdapter? = null

    protected lateinit var binding: ActivityNoticeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 하위 클래스들 모두 acrivity_notice.xml을 사용하므로ㅁ
        binding = ActivityNoticeBinding.inflate(layoutInflater)
        binding.rlContainer.applySystemBarInsets()

        setContentView(binding.root)

        val actionbar = supportActionBar
        actionbar!!.setTitle(noticeTitle)

        adapter = NoticeBaseAdapter(this, keyRead)
        binding.list.adapter = adapter
        binding.list.onItemClickListener = this
        id = intent.getIntExtra("id", 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if(BuildConfig.DEBUG) {
            menuInflater.inflate(R.menu.notice_menu, menu)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.btn_mark_read -> {
                markReadAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 개발 편의를 위해 모두 읽은 상태로 처리
    fun markReadAll() {
        val items = adapter?.items ?: return
        for (item in items) {
            markRead(item ?: continue)
        }
    }

    private fun markRead( item: NoticeModel ) {
        val keyRead = keyRead ?: return
        val readId = Util.getPreference(this, keyRead)
        val selectedId = item.id
        val readTotal: String
        val readArray = readId.split(",").toTypedArray()
        if (readId == "") {
            readTotal = readId + selectedId
            Util.setPreference(this, keyRead, readTotal)
        } else {
            if (!Util.isFoundString(selectedId, readArray)) {
                readTotal = "$readId,$selectedId"
                Util.setPreference(this, keyRead, readTotal)
            }
        }
        adapter?.notifyDataSetChanged()
    }

    fun openItem( item: NoticeModel ) {
        markRead(item)

        // 열기
        if( type == null ) { return }
        startActivity(WebViewActivity.createIntent(this, type!!, item.id.toInt(),
            noticeTitle, item.title))
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val item = adapter?.getItem(position) ?: return
        openItem(item)
    }

}