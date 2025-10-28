package net.ib.mn.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.databinding.FragmentBaseWebviewBinding
import net.ib.mn.link.CustomWebViewClient
import net.ib.mn.utils.Util
import java.net.URISyntaxException


/**
 * 웹뷰 보여주는 기본 베이스 fragment
 */

open class BaseWebViewFragment(
    private var url: String? = null) : Fragment() {
    private var mPrevActionBarTitle: CharSequence? = null
    private lateinit var binding: FragmentBaseWebviewBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBaseWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actionbar = (activity as BaseActivity?)?.supportActionBar
        mPrevActionBarTitle = actionbar?.title

        if(Util.isUsingNightModeResources(context)) binding.webView.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.gray300
            )
        )
    }

    override fun onDetach() {
        super.onDetach()

        val actionbar = (activity as BaseActivity?)?.supportActionBar
        actionbar?.title = mPrevActionBarTitle
    }

    fun setContentsUrl(url: String?) {
        binding.webView.loadUrl(url ?: return)
    }

    companion object {
        fun getInstance(url: String): BaseWebViewFragment {
            return BaseWebViewFragment(url)
        }
    }
}