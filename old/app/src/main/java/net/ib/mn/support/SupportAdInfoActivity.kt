package net.ib.mn.support

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import net.ib.mn.utils.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.databinding.ActivitySupportAdInfoBinding
import net.ib.mn.utils.*
import net.ib.mn.utils.ext.applySystemBarInsets

class SupportAdInfoActivity : BaseActivity() {

    private lateinit var supportAdTypeListModel: SupportAdTypeListModel
    private var id: Int? = null
    private var adName: String? = null
    private var adLocation: String? = null
    private var adDescription: String? = null
    private var adImageUrl: String? = null
    private var adImageUrl2: String? = null
    private var adIconUrl: String? = null
    private var adPeriod: String? = null
    private var adGuide: String? = null
    private lateinit var mGlideRequestManager: RequestManager

    private lateinit var binding : ActivitySupportAdInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_support_ad_info
        )
        binding.nsvSupportAdInfo.applySystemBarInsets()

        initSet()
    }


    //초기 세팅
    private fun initSet() {
        mGlideRequestManager = Glide.with(this)

        supportAdTypeListModel =
            intent.getSerializableExtra(PARAM_SUPPORT_AD_TYPE) as SupportAdTypeListModel

        //model 에 들어있던 각각 정보 들  set
        adName = supportAdTypeListModel.name.replace("&#39;", " \' ")
        adLocation = supportAdTypeListModel.location.replace("&#39;", " \' ")
        adDescription = supportAdTypeListModel.description.replace("&#39;", " \' ")
        adImageUrl = supportAdTypeListModel.imageUrl
        adImageUrl2 = supportAdTypeListModel.imageUrl2
        adIconUrl = supportAdTypeListModel.iconUrl
        adGuide = supportAdTypeListModel.guide.replace("&#39;", " \' ")

        //위 정보들 뷰에 연결
        binding.adName.text = adName
        binding.adLocaltion.text = adLocation
        binding.adDescription.text = adDescription

        val guideContentSplitList: List<String?>? = adGuide?.split("\n")

        if (guideContentSplitList != null) {
            try {
                binding.adMakingGuideTitle.text = guideContentSplitList[0]
                UtilK.addSupportGuideView(this, guideContentSplitList, binding.llDetailGuide)
            }catch (e:Exception){
                Toast.makeText(this,R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show();
                e.printStackTrace()
            }
        }


        //광고 디자인 공모전 안내.
        binding.adGuidance.text = Util.getPreference(this, Const.AD_GUIDANCE)

        //광고 예시 이미지
        mGlideRequestManager
            .load(adImageUrl)
            .dontAnimate()
            .into(binding.adExampleIv)

        //광고 제작 가이드 이미지
        mGlideRequestManager
            .load(adImageUrl2)
            .dontAnimate()
            .into(binding.adGuideIv)


        // TODO: 2021/08/04 일단 이거는 기획쪽에 없던 내용인데  toolbar 너무 허전해서 넣음
        supportActionBar?.title = getString(R.string.support_ad_type_detail)


    }

    companion object {

        const val PARAM_SUPPORT_AD_TYPE = "support_ad_type"

        @JvmStatic
        fun createIntent(context: Context, supportAdTypeListModel: SupportAdTypeListModel): Intent {
            val intent = Intent(context, SupportAdInfoActivity::class.java)
            intent.putExtra(PARAM_SUPPORT_AD_TYPE, supportAdTypeListModel);
            return intent
        }
    }
}