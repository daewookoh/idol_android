package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import net.ib.mn.utils.Toast
import net.ib.mn.addon.IdolGson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.model.NoticeModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.UtilK
import org.json.JSONArray
import org.json.JSONException
import javax.inject.Inject

@AndroidEntryPoint
class EventActivity : NoticeBaseActivity() {
    @Inject
    lateinit var miscRepository: MiscRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        keyRead = Const.PREF_EVENT_READ
        noticeTitle = getString(R.string.title_event)
        type = Const.TYPE_EVENT

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            miscRepository.getEvents(
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
                        UtilK.handleCommonError(this@EventActivity, response)
                    }
                }, {
                    Toast.makeText(
                        this@EventActivity,
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
            return Intent(context, EventActivity::class.java)
        }

        @JvmStatic
        fun createIntent(context: Context?, id: Int): Intent {
            val intent = Intent(context, EventActivity::class.java)
            intent.putExtra("id", id)
            return intent
        }
    }
}