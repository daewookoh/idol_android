/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 잡담게시판 헤더 뷰.
 *
 * */

package net.ib.mn.smalltalk.viewholder

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.ItemSmallTalkHeaderBinding
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.smalltalk.SmallTalkFragment
import net.ib.mn.smalltalk.SmallTalkFragment.Companion.FILTER_DATE_ORDER
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.util.Locale

class SmallTalkHeaderVH(
    val binding: ItemSmallTalkHeaderBinding,
    var orderBy: String,
    var locale: String?,
    val filterSetCallback: (BottomSheetFragment) -> Unit,
    val filterLocaleCallback: (BottomSheetFragment) -> Unit,
    val filterClickCallBack: (String, String?, String?, String) -> Unit,
    val searchCallBack: (String, String?, String?) -> Unit,
    val localeClickCallBack: (String, String?, String?, String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private var keyWord: String? = null

    fun bind(articleModel: ArticleModel) {
        setLanguageFilter()
        setFilter()
        setSearch()
    }

    // 모든언어, 최신순, All 태그 클릭한 상태로 변경
    fun allClickChange() {
        binding.tvFilter.text = itemView.context.getString(R.string.order_by_time)
        binding.tvLocaleFilter.text = itemView.context.getString(R.string.filter_all_language)
    }

    fun filterByDate() {
        Util.showProgress(itemView.context)
        orderBy = FILTER_DATE_ORDER
        binding.tvFilter.text = itemView.context.getString(R.string.order_by_time)
        filterClickCallBack(
            orderBy,
            keyWord,
            locale,
            UtilK.getLocaleStringResource(Locale.KOREA, R.string.order_by_time, itemView.context),
        )
    }

    fun filterByLike() {
        Util.showProgress(itemView.context)
        orderBy = SmallTalkFragment.FILTER_LIKE_ORDER
        binding.tvFilter.text = itemView.context.getString(R.string.order_by_like)
        filterClickCallBack(
            orderBy,
            keyWord,
            locale,
            UtilK.getLocaleStringResource(Locale.KOREA, R.string.order_by_like, itemView.context),
        )
    }

    fun filterByHits() {
        Util.showProgress(itemView.context)
        orderBy = SmallTalkFragment.FILTER_HITS_ORDER
        binding.tvFilter.text = itemView.context.getString(R.string.order_hit)
        filterClickCallBack(
            orderBy,
            keyWord,
            locale,
            UtilK.getLocaleStringResource(Locale.KOREA, R.string.order_hit, itemView.context),
        )
    }

    fun filterByComments() {
        Util.showProgress(itemView.context)
        orderBy = SmallTalkFragment.FILTER_COMMENT_ORDER
        binding.tvFilter.text = itemView.context.getString(R.string.freeboard_order_comments)
        filterClickCallBack(
            orderBy,
            keyWord,
            locale,
            UtilK.getLocaleStringResource(
                Locale.KOREA,
                R.string.freeboard_order_comments,
                itemView.context,
            ),
        )
    }

    //언어 스트링 변경
    fun filterByLanguage(locale: String?, langText: String) {
        Util.showProgress(itemView.context)
        binding.tvLocaleFilter.text = langText
        this.locale = locale
        localeClickCallBack(orderBy, keyWord, locale, langText)
    }

    private fun setFilter() {
        val sheet = BottomSheetFragment.newInstance(BottomSheetFragment.FALG_SMALL_TALK_FILTER)
        binding.clFilter.setOnClickListener {
            filterSetCallback(sheet)
        }
    }

    //바텀시트 미리 만들어놓음
    private fun setLanguageFilter() {
        val sheet =
            BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_SMALL_TALK_LANGUAGE_FILTER)
        binding.clLanguageFilter.setOnClickListener {
            filterLocaleCallback(sheet)
        }
    }

    private fun setSearch() {
        binding.searchBar.etSearch.hint = itemView.context.getString(R.string.freeboard_search)
        binding.searchBar.btnSearch.visibility = View.GONE

        // 검색창에 포커스가 가면 검색 버튼 표시
        binding.searchBar.etSearch.setOnFocusChangeListener { _, hasFocus ->
            binding.searchBar.btnSearch.visibility = if (hasFocus) View.VISIBLE else View.GONE
        }

        binding.searchBar.etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search()
            } else {
                return@setOnEditorActionListener false
            }
            return@setOnEditorActionListener true
        }

        binding.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                keyWord = if (binding.searchBar.etSearch.text.isNullOrEmpty()) {
                    null
                } else {
                    binding.searchBar.etSearch.text.toString()
                }
            }
        })

        binding.searchBar.btnSearch.setOnClickListener {
            search()
        }
    }

    private fun search() {
        val imm =
            itemView.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchBar.etSearch.windowToken, 0)

        val searchText = binding.searchBar.etSearch.text.toString().trim()
        keyWord = searchText.ifEmpty {
            null
        }

        binding.searchBar.etSearch.clearFocus()

        searchCallBack(orderBy, keyWord, locale)
    }
}