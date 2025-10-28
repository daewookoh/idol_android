package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.material.search.SearchBar
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.HistoryItemBinding
import net.ib.mn.databinding.SearchBarBinding
import net.ib.mn.fragment.CharityCountFragment
import net.ib.mn.model.CharityModel
import java.util.*
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import kotlin.collections.ArrayList

class CharityHistoryAdapter(
        private val context: Context,
        private val onClickListener: CharityCountFragment,
        private val onSearchListener : CharityCountFragment,
        private val mGlideRequestManager: RequestManager,
        private var charityModel: ArrayList<CharityModel>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    //검색했을 때 필터된 모델값 저장할 때 사용
    private var filterCharityModel = ArrayList<CharityModel>()

    //charityModel이 빈값인지 체크할 때 사용
    lateinit var charityListEmptyListener : CharityListEmptyListener


    //기록실 리스트 아이템 클릭 리스너
    interface OnClickListener{
        fun onItemClicked(charityModel: CharityModel?)
    }

    //기록실 Search bar 돋보기 버튼 클릭 리스너
    interface OnSearchListener{
        fun btnSearchClicked(view : View, editText: AppCompatEditText, clickCheck : Boolean)
    }

    fun setCharityEmptyListener(charityListEmptyListener: CharityListEmptyListener){
        this.charityListEmptyListener = charityListEmptyListener
    }

    //검색 시 아이템 비어있는지 체크하는 리스너
    interface CharityListEmptyListener{
        fun getEmptyOrNot(charityEmpty: Boolean)
    }

    init {
        filterCharityModel = charityModel
    }

    //검색했을 경우
    fun getSearchedKeyword(inputSearch: String) {
        filterCharityModel = ArrayList()

        //charityModel 크기 만큼 검색
        for(i in 0 until charityModel.size) {
            //아이돌 이름,그룹 이름 대문자로 변환 후 검색
            if (charityModel[i].idolName.uppercase().contains(inputSearch)||charityModel[i].groupName.uppercase().contains(inputSearch)) {
                filterCharityModel.add(charityModel[i])
                charityListEmptyListener.getEmptyOrNot(false)//리스트가 비어있지 않을 때 false
            }
        }
        //검색했을 때 검색결과가 없으면
        if(filterCharityModel.size == 0){
            charityListEmptyListener.getEmptyOrNot(true)//리스트가 비어있을 때 true
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when(viewType){
            TYPE_TOP -> {
                val binding = DataBindingUtil.inflate<SearchBarBinding>(
                    LayoutInflater.from(parent.context),
                    R.layout.search_bar,
                    parent,
                    false,
                )
                SearchViewHolder(binding)
            }
            else ->{
                val binding = HistoryItemBinding.inflate(LayoutInflater.from(context), parent, false)
                CharityHistoryViewHolder(binding)
            }
        }
    }

    override fun getItemCount(): Int = filterCharityModel.size+1

    override fun getItemViewType(position: Int): Int {
        return if(position == 0){
            TYPE_TOP
        }
        else{
            TYPE_RANK
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(position==0){    //맨 위일 경우 Search_bar
            (holder as SearchViewHolder).bind()
        }
        else{    //맨 위 제외하고 기록실 역사관 list
            (holder as CharityHistoryViewHolder).bind(filterCharityModel[position-1], position)
        }
    }

    //Search bar ViewHolder
    inner class SearchViewHolder(val binding: SearchBarBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind() = with(binding) {
            etSearch.setHint(if(BuildConfig.CELEB) R.string.actor_hint_search_idol else R.string.hint_search_idol)

            //search bar 돋보기 클릭 리스너
            val listener = View.OnClickListener { view ->
                onSearchListener.btnSearchClicked(view, etSearch, false)
            }

            //키보드 돋보기 클릭 리스너
            etSearch.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onSearchListener.btnSearchClicked(v,etSearch, false)
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            //검색어 쓰기 위해 클릭했을 때 리스너
            etSearch.setOnClickListener { v -> onSearchListener.btnSearchClicked(v,etSearch, true) }

            btnSearch.setOnClickListener(listener)

            //포커스가 잡혀있는 상태에서 스크롤 시 포커스가 해제되면 키보드 내려줌
            etSearch.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    Util.hideSoftKeyboard(context, view)
                }
            }

        }
    }

    //역사관 list ViewHolder
    inner class CharityHistoryViewHolder(val binding: HistoryItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(charityModel: CharityModel?, position: Int) {
            mGlideRequestManager.load(charityModel?.imageUrl).into(binding.imgHistory)
            binding.tvHistoryDate.text = charityModel?.title
            binding.tvHistoryName.text = charityModel?.idolName
            binding.tvHistorySubName.text = charityModel?.groupName

            val listener  = View.OnClickListener {
                onClickListener.onItemClicked(filterCharityModel[position-1])
            }
            binding.clHistory.setOnClickListener(listener)

        }
    }

    companion object{
        const val TYPE_TOP = 0  //search bar
        const val TYPE_RANK = 1 //역사관 list
    }
}