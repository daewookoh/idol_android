package net.ib.mn.fragment

import android.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.nextapps.naswall.NASWall
import com.nextapps.naswall.NASWall.OnAdListListener
import com.nextapps.naswall.NASWall.OnJoinAdListener
import com.nextapps.naswall.NASWallAdInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.core.domain.usecase.GetConfigSelfUseCase
import net.ib.mn.databinding.ChargeItemMetabsBinding
import net.ib.mn.databinding.FragmentHeartplus1Binding
import net.ib.mn.model.ConfigModel.Companion.getInstance
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.handleCommonError
import javax.inject.Inject

@AndroidEntryPoint
class HeartPlusMeTabsFragment : BaseFragment(), AdapterView.OnItemClickListener {
    private var mListView: ListView? = null
    private var mAdapter: Adapter? = null
    private var heartCount = 0

    private var mContext: Context? = null

    private var _binding: FragmentHeartplus1Binding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var getConfigSelfUseCase: GetConfigSelfUseCase

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGlideRequestManager = Glide.with(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mListView = view.findViewById<ListView>(R.id.list)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHeartplus1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context

        Util.showProgress(getActivity())

        val locale = Util.getSystemLanguage(getActivity())
        Util.log("locale::" + locale)

        lifecycleScope.launch {
            val result = getConfigSelfUseCase().first()
            val response = result.data
            if(!result.success || result.data == null) {
                Util.closeProgress()
                if (activity != null && isAdded && response != null) {
                    handleCommonError(activity, response)
                }
                return@launch
            }

            if (response?.optBoolean("success") != true) {
                Util.closeProgress()
                return@launch
            }

            if (response.optBoolean("success")) {
                getInstance(mContext).parse(response)
                heartCount = getInstance(activity).nasHeart
                if (heartCount != 0) {
                    mAdapter = Adapter(context, heartCount)
                    mListView!!.setAdapter(mAdapter)
                    mListView!!.setOnItemClickListener(this@HeartPlusMeTabsFragment)

                    // crash 방지
                    if (activity == null || !isAdded) {
                        return@launch
                    }

                    NASWall.getAdList(
                        activity, getAccount(activity)!!.email,
                        object : OnAdListListener {
                            override fun OnError(code: Int) {
                                Util.closeProgress()
                            }

                            override fun OnSuccess(arg0: ArrayList<NASWallAdInfo?>) {
                                Util.closeProgress()
                                if (!isAdded || _binding == null) return

                                for (item in arg0) {
                                    mAdapter?.add(item)
                                }

                                mListView?.post {
                                    if (!isAdded || _binding == null) return@post
                                    mAdapter?.notifyDataSetChanged()
                                }
                            }
                        })
                } else {
                    Util.closeProgress()
                }
            } else {
                Util.closeProgress()
                if (activity != null && isAdded) {
                    handleCommonError(activity, response)
                    if (Util.is_log()) {
                        showMessage(response.optString("msg"))
                    }
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    public override fun onResume() {
        super.onResume()

        synchronized(this) {
            if (mAdapter != null) mAdapter!!.notifyDataSetChanged()
        }
    }

    private inner class Adapter(context: Context, private val heart_count: Int) :
        ArrayAdapter<Any?>(context, net.ib.mn.R.layout.charge_item_metabs) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val binding: ChargeItemMetabsBinding

            if(convertView == null) {
                binding = ChargeItemMetabsBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                binding.root.tag = binding
            } else {
                binding = convertView.tag as ChargeItemMetabsBinding
            }

            update(binding.root, getItem(position), position)
            return binding.root
        }

        override fun update(view: View?, item: Any?, position: Int) = with(view?.tag as ChargeItemMetabsBinding) {
            chargeItem.setVisibility(View.VISIBLE)

            try {
                val info = item as NASWallAdInfo

                icon.setBackground(
                    ContextCompat.getDrawable(
                        requireActivity(),
                        net.ib.mn.R.drawable.round_outline
                    )
                )

                mGlideRequestManager!!
                    .load(info.getIconUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(item.getAdId()))
                    .fallback(Util.noProfileImage(item.getAdId()))
                    .placeholder(Util.noProfileImage(item.getAdId()))
                    .into(icon)
                title.setText(info.getTitle())
                subTitle.setText(info.getMissionText())
                chargeHeart.setText(heart_count.toString())
            } catch (e: Exception) {
                chargeItem.setVisibility(View.GONE)
            }
        }
    }

    override fun onItemClick(
        parent: AdapterView<*>?, view: View?, position: Int,
        id: Long
    ) {
        val adInfo = mAdapter!!.getItem(position) as NASWallAdInfo?
        NASWall.joinAd(getActivity(), adInfo, object : OnJoinAdListener {
            override fun OnComplete(nasWallAdInfo: NASWallAdInfo?) {
            }

            override fun OnSuccess(adInfo: NASWallAdInfo?, url: String?) {
                var isSuccess = false
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (intent != null) {
                        startActivity(intent)
                        isSuccess = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (!isSuccess) {
                    Toast.makeText(
                        getActivity(),
                        getString(net.ib.mn.R.string.error_nextapps_error2), Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun OnError(adInfo: NASWallAdInfo?, errorCode: Int) {
                // 크래시 방지

                val activity: Activity? = getActivity()
                if (activity != null && isAdded()) {
                    var message = "[" + errorCode + "] "
                    when (errorCode) {
                        -10001 -> message += getString(net.ib.mn.R.string.error_nextapps_error3)
                        -20001 -> message += getString(net.ib.mn.R.string.error_nextapps_error4)
                        else -> message += getString(net.ib.mn.R.string.error_nextapps_error5)
                    }

                    Toast.makeText(
                        getActivity(), message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }
}
