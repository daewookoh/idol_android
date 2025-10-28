package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import net.ib.mn.utils.Toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.model.NoticeModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.UtilK
import org.json.JSONArray
import org.json.JSONException
import javax.inject.Inject

@AndroidEntryPoint
class NoticeActivity : NoticeBaseActivity() {
    @Inject
    lateinit var miscRepository: MiscRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        keyRead = Const.PREF_NOTICE_READ
        noticeTitle = getString(R.string.title_notice)
        type = Const.TYPE_NOTICE

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            miscRepository.getNotices(
                { response ->
                    if (response.optBoolean("success")) {
                        val array: JSONArray
                        try {
                            array = response.getJSONArray("objects")
                            val gson = IdolGson.getInstance()
                            for (i in 0 until array.length()) {
                                val em = gson.fromJson(
                                    array.getJSONObject(i)
                                        .toString(), NoticeModel::class.java
                                )
                                if (em.id.toInt() == id) {
                                    // 해당 공지/이벤트 상세를 바로 연다
                                    runOnUiThread {
                                        openItem(em)
                                    }
                                }
                                adapter?.add(em)
                            }
                            adapter?.notifyDataSetChanged()
                            if ((adapter?.count ?: 0) > 0) {
                                binding.list.visibility = View.VISIBLE
                                binding.empty.visibility = View.GONE
                            } else {
                                binding.list.visibility = View.GONE
                                binding.empty.visibility = View.VISIBLE
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        UtilK.handleCommonError(this@NoticeActivity, response)
                    }
                }, {
                    Toast.makeText(
                        this@NoticeActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context?): Intent {
            return Intent(context, NoticeActivity::class.java)
        }

        @JvmStatic
        fun createIntent(context: Context?, id: Int): Intent {
            val intent = Intent(context, NoticeActivity::class.java)
            intent.putExtra("id", id)
            return intent
        }
    }
}