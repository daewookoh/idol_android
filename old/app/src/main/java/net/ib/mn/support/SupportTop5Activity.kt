package net.ib.mn.support

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.adapter.SupportTop5Adapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.databinding.ActivitySupportTop5Binding
import net.ib.mn.model.SupportTop5Model
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONException
import java.text.NumberFormat
import javax.inject.Inject

/**
 * ProjectName: idol_app_renew
 *
 * Description:  서포트 인증샷 화면에서 top5버튼을 눌렀을떄  나오는 화면
 * */

@AndroidEntryPoint
class SupportTop5Activity:BaseActivity() {

    private lateinit var mGlideRequestManager: RequestManager
    private lateinit var supportTop5Adapter: SupportTop5Adapter
    private var supportTop5List: ArrayList<SupportTop5Model> = ArrayList()
    var myVoteDia:Int=0//내가 투표한 다이아 개수

    private lateinit var binding: ActivitySupportTop5Binding

    @Inject
    lateinit var supportRepository: SupportRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_support_top5)
        binding.nsvSupportTop5.applySystemBarInsets()

        initSet()
        setActionBar()
        setRecyclerView()
    }

    private fun setActionBar() {
        val actionBar = supportActionBar
        actionBar?.title = this.getString(R.string.btn_support_top5)
    }


    private fun setRecyclerView(){
        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.line_divider)!!)
        supportTop5Adapter = SupportTop5Adapter(this, mGlideRequestManager, supportTop5List)
        supportTop5Adapter.setHasStableIds(true)
        binding.rvTop5.adapter = supportTop5Adapter
        binding.rvTop5.addItemDecoration(divider)
        binding.rvTop5.setHasFixedSize(true)
    }

    //초기 세팅
    private fun initSet(){
        mGlideRequestManager = Glide.with(this)

        //처음에는  데이터가저오는 중 불러주기
        binding.tvEmpty.visibility = View.VISIBLE

        //받아온 값 보여줌.
        getTop5(intent.getIntExtra(PARAM_SUPPORT_ID,-1))
    }

    private fun getTop5(supportId:Int) {

        if (supportId != -1) {//-1 default value값이  아닌경우  정삭적으로 처리
            MainScope().launch {
                supportRepository.getTop5(
                    supportId = supportId,
                    { response ->
                        if (response!!.optBoolean("success")) {
                            supportTop5List.clear()
                            myVoteDia = response.getInt("my_support")
                            val array = response.getJSONArray("objects")
                            val gson = IdolGson.getInstance()

                            try {
                                for (i in 0 until array.length()) {
                                    supportTop5List.add(
                                        gson.fromJson(
                                            array.getJSONObject(i).toString(),
                                            SupportTop5Model::class.java
                                        )
                                    )
                                }

                                val countDiaFormat  =  NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this@SupportTop5Activity)).format(myVoteDia)
                                binding.myDiamond.text = String.format(getString(R.string.heart_count_format),countDiaFormat)
                                for (i in supportTop5List.indices) {
                                    val item: SupportTop5Model = supportTop5List[i]
                                    // 동점자 처리
                                    if (i > 0 && supportTop5List[i - 1].diamond == item.diamond) item.rank =
                                        supportTop5List[i - 1].rank else item.rank = i
                                }

                                //adapter 업데이트
                                supportTop5Adapter.setItems(supportTop5List)
                                supportTop5Adapter.notifyDataSetChanged()

                                //모든게  성공적으로 끝나면 데이터 가져오는중 화면 없애줌.
                                binding.tvEmpty.visibility = View.GONE

                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }, { throwable ->
                        Toast.makeText(this@SupportTop5Activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                        if (Util.is_log()) {
                            showMessage(throwable.message)
                        }
                    }
                )
            }
        } else {//support id 가 default값인 -1이 왔을때
            Toast.makeText(this@SupportTop5Activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
        }
    }

    companion object{
        const val PARAM_SUPPORT_ID ="param_support_id"
        @JvmStatic
        fun createIntent(context: Context?,supportId: Int): Intent {
            val intent = Intent(context, SupportTop5Activity::class.java)
            intent.putExtra(PARAM_SUPPORT_ID, supportId)
            return intent
        }
    }
}
