package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isEmpty
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.CommunityActivity.Companion.createIntent
import net.ib.mn.adapter.FavoriteSettingAdapter
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.chatting.chatDb.ChatRoomList
import net.ib.mn.core.data.repository.FavoritesRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.ActivityFavoriteSettingBinding
import net.ib.mn.domain.usecase.GetAllIdolsUseCase
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.FavoriteModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.ApiCacheManager.Companion.getInstance
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.TempUtil
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.handleCommonError
import net.ib.mn.utils.UtilK.Companion.showChangeMostDialog
import net.ib.mn.utils.setFirebaseScreenViewEvent
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets

@AndroidEntryPoint
class FavoriteSettingActivity : BaseActivity(), View.OnClickListener,
    FavoriteSettingAdapter.OnAdapterCheckedChangeListener, AdapterView.OnItemClickListener {
    private lateinit var binding: ActivityFavoriteSettingBinding

    private var mAdapter: FavoriteSettingAdapter? = null
    private var mAccount: IdolAccount? = null
    private val mIdols: MutableMap<String, IdolModel> = HashMap()
    private val mFavorites: MutableMap<Int, Int?> =
        HashMap() // 실제 idol id, 사용자의 즐겨찾기 id -> 사용자에게 보여주는 데이타로 사용
    private val mtempFavorites: MutableMap<Int, Int?> =
        HashMap() // 사용자가 즐겨찾기를 변경한 데이타를 갖고 있는다. 검색하면 mtempFavorites 를 mFavorites로 옮긴다.

    private var mGlideRequestManager: RequestManager? = null
    private var mGlideOptions: RequestOptions? = null
    
    @Inject
    lateinit var getAllIdolsUseCase: GetAllIdolsUseCase
    @Inject
    lateinit var sharedAppState: SharedAppState
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var favoritesRepository: FavoritesRepository
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    private var idolSoloFinalId = -1
    private var idolGroupFinalId = -1 // 애돌

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFavoriteSettingBinding.inflate(layoutInflater)
        binding.clContainer.applySystemBarInsets()

        setContentView(binding.root)
        mGlideRequestManager = Glide.with(this)

        val actionbar = supportActionBar
        actionbar!!.setTitle(UtilK.getMyIdolTitle(this))
        actionbar.setDisplayHomeAsUpEnabled(true)
        actionbar.setHomeButtonEnabled(true)

        if (BuildConfig.CELEB) {
            binding.tvFavoriteTag.setText(R.string.actor_most_favorite)
            binding.helpTakeHeart.setText(R.string.tooltip_select_my_favorite_actor)
        }

        binding.searchBar.etSearch.setHint(if (BuildConfig.CELEB) R.string.actor_hint_search_idol else R.string.hint_search_idol)
        binding.searchBar.btnSearch.setOnClickListener(this)

        mGlideOptions = RequestOptions().error(R.drawable.favorite_idol_empty_states)
            .fallback(R.drawable.favorite_idol_empty_states)
            .placeholder(R.drawable.favorite_idol_empty_states)
            .circleCrop()


        mAccount = getAccount(this)
        val most = mAccount!!.most
        if (most != null) {
            binding.favorite.setText(most.getName(this))
            binding.favoritePhoto.post(Runnable {
                mGlideRequestManager!!
                    .load(most.imageUrl)
                    .apply(mGlideOptions!!)
                    .into(binding.favoritePhoto)
            })
        }
        binding.searchBar.etSearch.setOnEditorActionListener(TextView.OnEditorActionListener { textView: TextView?, i: Int, keyEvent: KeyEvent? ->
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                doSearch()
            } else { // 기본 엔터키 동작
                return@OnEditorActionListener false
            }
            true
        })
        binding.searchBar.btnSearch.setEnabled(false)
        mAdapter = FavoriteSettingAdapter(this, mGlideRequestManager!!, this)
        binding.list.apply {
            setAdapter(mAdapter)
            setVisibility(View.GONE)
            setOnItemClickListener(this@FavoriteSettingActivity)
        }

        // 과도하게 불리고 서버에서 캐시 비우기 해주므로 불필요
//        deleteFavoritesCache();
        loadResources()

        //      // 아랍어일때 패딩
        if (Util.isRTL(this)) {
            val left = Util.convertDpToPixel(this, 10f).toInt()
            val top = Util.convertDpToPixel(this, 16f).toInt()
            val right = Util.convertDpToPixel(this, 25f).toInt()
            val bottom = Util.convertDpToPixel(this, 8f).toInt()
            binding.helpTakeHeart.setPadding(left, top, right, bottom)
            binding.helpTakeHeart1.setPadding(left, top, right, bottom)
            binding.helpTakeHeart2.setPadding(left, top, right, bottom)
        }
    }

    override fun onResume() {
        super.onResume()
        setFirebaseScreenViewEvent(GaAction.CHOEAE_SETTING, javaClass.simpleName)
        binding.helpTakeHeart.visibility = View.GONE
        binding.helpTakeHeart1.visibility = View.GONE
        binding.helpTakeHeart2.visibility = View.GONE
    }

    override fun onRestart() {
        super.onRestart()
        //   loadFavorites(); 이걸 부르면 뉴프렌즈 갔다가 뒤로 왔을 때 이전 최애로 보임
    }

    private fun showMost() {
        val most = getAccount(this)!!.userModel!!.most // mAccount를 쓰면 안됨
        if (most != null) {
            Util.log("showMost most=" + most.getName(this))
            binding.favorite.text = most.getName(this)
            mGlideRequestManager
                ?.load(most.imageUrl)
                ?.apply(mGlideOptions!!)
                ?.into(binding.favoritePhoto)
        } else {
            binding.favorite.text = getString(R.string.none)
            binding.favoritePhoto.setImageResource(R.drawable.favorite_idol_empty_states)
        }
    }

    private fun doSearch() {
        if (TextUtils.isEmpty(binding.searchBar.etSearch.text)) {
            return
        }
        mAdapter!!.clear()
        mFavorites.clear()
        mFavorites.putAll(mtempFavorites)
        val keyword = binding.searchBar.etSearch.text.toString()
            .lowercase(Locale.getDefault()).trim { it <= ' ' }

        // 투표순 정렬
        val idols = ArrayList<IdolModel>()

        for (idol in mIdols.values) {
            //다국어로 검색했을때 Local언어로 나오게 하기.
            val name = idol.getName(this) + "/" + idol.description
            //검색시 언어마다 맞춰서 들어갈 수 있기 때문에 여기서는 context를 안넣어준다.
            val nameKo = idol.getName()
            val nameJp = idol.nameJp
            val nameZh = idol.nameZh
            val nameZh_Tw = idol.nameZhTw
            val nameEn = idol.nameEn
            if (name.lowercase(Locale.getDefault())
                    .contains(keyword) || nameKo.lowercase(Locale.getDefault())
                    .contains(keyword) || nameJp.lowercase(
                    Locale.getDefault()
                ).contains(keyword) ||
                nameZh.lowercase(Locale.getDefault()).contains(keyword) || nameZh_Tw.lowercase(
                    Locale.getDefault()
                ).contains(keyword) || nameEn.lowercase(
                    Locale.getDefault()
                ).contains(keyword)
            ) {
                //mAdapter.add(idol);
                if (BuildConfig.CELEB && idol.getOriginalId() == 0) {
                    continue
                }
                idols.add(idol)
            }
        }

        // favorite 표시
        for (id in mFavorites.keys) {
            for (idol in mIdols.values) {
                if (idol.getId() == id) {
                    idol.isFavorite = true
                    break
                }
            }
        }


        sort(idols)
        mAdapter!!.addAll(idols)

        if (mAdapter!!.count > 1) { //아이돌이 두명 이상 검색 되었을때

            binding.empty.visibility = View.GONE
            binding.list.visibility = View.VISIBLE

            if (!checkToolTipFirstShown(binding.helpTakeHeart, Const.PREF_SHOW_SELECT_MY_IDOL)) {
                checkToolTipFirstShown(binding.helpTakeHeart2, Const.PREF_SHOW_SELECT_MY_FAVORITE)
                binding.helpTakeHeart1.visibility = View.GONE
            } else {
                checkToolTipFirstShown(
                    binding.helpTakeHeart1,
                    Const.PREF_SHOW_SELECT_MY_FAVORITE
                )
            }

            //리스트뷰  스크롤에  위치에 따라  아직 삭제안된 툴팁들 보여준다.
            binding.list.setOnScrollListener(object : AbsListView.OnScrollListener {
                var firstVisible: Boolean = false
                override fun onScroll(
                    view: AbsListView,
                    firstVisibleItem: Int,
                    visibleItemCount: Int,
                    totalItemCount: Int
                ) {
                    firstVisible = firstVisibleItem == 0
                    if (!(firstVisible && listIsAtTop())) {
                        binding.helpTakeHeart.visibility = View.GONE
                        binding.helpTakeHeart1.visibility = View.GONE
                        binding.helpTakeHeart2.visibility = View.GONE
                    }
                }

                override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                    if (firstVisible && listIsAtTop()) {
                        if (!checkToolTipFirstShown(
                                binding.helpTakeHeart,
                                Const.PREF_SHOW_SELECT_MY_IDOL
                            )
                        ) {
                            checkToolTipFirstShown(
                                binding.helpTakeHeart2,
                                Const.PREF_SHOW_SELECT_MY_FAVORITE
                            )
                            binding.helpTakeHeart1.visibility = View.GONE
                        } else {
                            checkToolTipFirstShown(
                                binding.helpTakeHeart1,
                                Const.PREF_SHOW_SELECT_MY_FAVORITE
                            )
                        }
                    } else { //리스트뷰  스크롤이  top 상태가 아닐때
                        binding.helpTakeHeart.visibility = View.GONE
                        binding.helpTakeHeart1.visibility = View.GONE
                        binding.helpTakeHeart2.visibility = View.GONE
                    }
                }
            })
        } else if (mAdapter!!.count == 1) { // 아이돌이 1명만   검색 되었을때

            //위  아이돌 2명이상 조건이 먼저 불리면  스크롤리스너가 작동되고 있으므로,
            //여기서  해제 시켜준다.

            binding.list.setOnScrollListener(null)


            binding.empty.visibility = View.GONE
            binding.list.visibility = View.VISIBLE
            binding.helpTakeHeart1.visibility = View.GONE


            if (checkToolTipFirstShown(binding.helpTakeHeart, Const.PREF_SHOW_SELECT_MY_IDOL)) {
                binding.helpTakeHeart2.visibility = View.GONE
            } else {
                checkToolTipFirstShown(binding.helpTakeHeart2, Const.PREF_SHOW_SELECT_MY_FAVORITE)
            }
        } else { //이무것도 검색 안되었을때

            binding.helpTakeHeart.visibility = View.GONE
            binding.helpTakeHeart1.visibility = View.GONE
            binding.helpTakeHeart2.visibility = View.GONE

            binding.empty.visibility = View.VISIBLE
            binding.list.visibility = View.GONE
        }


        mAdapter!!.notifyDataSetChanged()
        binding.searchBar.etSearch.setText("")
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchBar.etSearch.windowToken, 0)
    }


    //리스트뷰 현재 상태가  가장  top으로 스크롤 되었는지 판단.
    private fun listIsAtTop(): Boolean {
        //맨처음  0이상 리스트 호출시 -> getchild 0으로 잡히는데 이때  null 필하기 위해  return 값 true

        if (binding.list.isEmpty()) return true

        //getchild 0 이상시는  index 0 child의 top 위치가  0픽셀일때  true 리턴
        //0픽셀의 의미는  리스트뷰 가장 맨위
        return binding.list.getChildAt(0).top == 0
    }


    //유저의  tooltip 삭제 이벤트 받는 리스너
    var checkToolTipRemoveListener: CheckToolTipRemoveListener =
        object : CheckToolTipRemoveListener {
            override fun toolTipRemoved(removedToolTip: TextView?, searchedIdolCount: Int) {
                //최애 툴팁  지워지고, 검색된  아이돌이 1명일때 ->최애툴팁과  같은 라인의  즐겨찾기 툴팁 보여줌.

                if (removedToolTip === binding.helpTakeHeart && searchedIdolCount == 1) {
                    checkToolTipFirstShown(
                        binding.helpTakeHeart2,
                        Const.PREF_SHOW_SELECT_MY_FAVORITE
                    )
                }
            }
        }


    //유저의  tooltip  없앰  이벤트 받기위한 인터페이스.
    interface CheckToolTipRemoveListener {
        fun toolTipRemoved(tooltip: TextView?, adapterCount: Int)
    }


    //유저가  툴팁을 삭제했는지 여부 판단한다.
    //삭제했으면  false 리턴  한번도 삭제 안했으면 true 리턴
    private fun checkToolTipFirstShown(helpTooltip: TextView, tooltipStatus: String): Boolean {
        //유저가  툴팁 삭제 안했을

        if (Util.getPreferenceBool(this, tooltipStatus, true)) {
            helpTooltip.visibility = View.VISIBLE

            //툴팁 삭제 이벤트.
            helpTooltip.setOnClickListener { v: View? ->
                helpTooltip.visibility = View.GONE
                checkToolTipRemoveListener.toolTipRemoved(binding.helpTakeHeart, mAdapter!!.count)
                Util.setPreference(this, tooltipStatus, false)
            }
            return true
        } else { //유저가 이전에 툴팁 삭제 했을
            helpTooltip.visibility = View.GONE
            return false
        }
    }


    private fun loadResources() {
        val account = getAccount(this@FavoriteSettingActivity)
        mIdols.clear()

        // TODO LoadResource 제거시 TempUtil과 함께 제거
        TempUtil.doGetAllIdols(this, getAllIdolsUseCase) { idols ->
            // 동점자 처리
            val equalize = ArrayList<IdolModel>()

            for (i in idols.indices) {
                val model = idols[i]
                if (model != null) {
                    // 동점자 처리
                    model.rank = i
                    if (i > 0) {
                        if (equalize[i - 1].heart == model.heart) {
                            model.rank = equalize[i - 1].rank
                        }
                    }
                    equalize.add(model)
                    mIdols[model.resourceUri] = model
                }
            }
            val mostModel = account!!.most
            if (mostModel != null) {
                val exist = mIdols[mostModel.resourceUri]
                if (exist != null) {
                    exist.isMost = true
                }
            }
            runOnUiThread {
                loadFavorites()
            }
        }
    }

    private fun setFavorites(response: JSONObject) {
        try {
            var excluded = false
            val gson = instance
            mFavorites.clear()
            mtempFavorites.clear()
            mAdapter!!.clear()
            val array = response.getJSONArray("objects")

            // 투표순 정렬
            val idols = ArrayList<IdolModel>()

            for (i in 0..<array.length()) {
                val model = gson.fromJson(
                    array
                        .getJSONObject(i).toString(),
                    FavoriteModel::class.java
                )
                val idol = model.idol ?: continue
                mFavorites[idol.getId()] = model.id
                if (idol.isViewable == "N") {
                    if (mAccount != null && mAccount!!.most != null) {
                        if (mAccount!!.most!!.resourceUri == idol.resourceUri) idol.isMost = true
                    }
                    mIdols[idol.resourceUri] = idol
                }
                val existModel = mIdols[idol.resourceUri]
                // 제외된 아이돌도 보이게
                if (existModel != null) {
                    idol.isMost = existModel.isMost // 검색화면에서 최애 설정상태가 안보여서
                    // favorites/self를 캐시하고 있어서 투표수를 idols 응답으로 갱신
                    idol.heart = existModel.heart
                }
                if (idol.isViewable == "N") excluded = true
                idol.isFavorite = true
                idols.add(idol)
            }

            if (excluded) {
                lifecycleScope.launch {
                    idolsRepository.getExcludedIdols(
                        { response ->
                            if (response.optBoolean("success")) {
                                val gson1 = instance
                                val listType = object : TypeToken<List<IdolModel?>?>() {}.type
                                val excludedIdols =
                                    gson1.fromJson<List<IdolModel>>(
                                        response.optJSONArray("objects")?.toString() ?: return@getExcludedIdols, listType
                                    )

                                for (excludedIdol in excludedIdols) {
                                    for (idol in idols) {
                                        if (idol.getId() == excludedIdol.getId()) {
                                            idol.heart = excludedIdol.heart
                                        }
                                    }
                                }

                                sort(idols)

                                // 검색결과 유지를 위해 아래 부분 수정
//            mAdapter.addAll(idols);
                                val oldIdols = ArrayList(
                                    mAdapter!!.items
                                )
                                mAdapter!!.clear()
                                if (oldIdols.size > 0) {
                                    for (i in oldIdols.indices) {
                                        val oldIdol = oldIdols[i]
                                        oldIdol?.isFavorite = false
                                        oldIdol?.isMost = false
                                        for (k in idols.indices) {
                                            val idol = idols[k]
                                            if (idol.getId() == oldIdol?.getId()) {
                                                // 어댑터에 있던 아이돌 정보를 최신것으로 갱신
                                                oldIdols[i] = idol
                                                break
                                            }
                                        }
                                    }
                                    mAdapter!!.addAll(oldIdols)
                                } else {
                                    mAdapter!!.addAll(idols)
                                }

                                // 최애 변경된 경우를 다시 적용
                                val items = ArrayList(
                                    mAdapter!!.items
                                )
                                for (i in items.indices) {
                                    val idol = items[i]
                                    if (mAccount!!.most != null) {
                                        idol?.isMost = idol?.getId() == mAccount!!.most!!.getId()
                                    }
                                }

                                mtempFavorites.putAll(mFavorites)
                                if (mAdapter!!.count > 0) {
                                    binding.list.visibility = View.VISIBLE
                                } else {
                                    binding.list.visibility = View.GONE
                                }
                                mAdapter!!.notifyDataSetChanged()
                                binding.searchBar.btnSearch.isEnabled = true
                            }
                        }, {})
                }
            } else {
                sort(idols)
                // 검색결과 유지를 위해 아래 부분 수정
//            mAdapter.addAll(idols);
                val oldIdols = ArrayList(
                    mAdapter!!.items
                )
                mAdapter!!.clear()
                if (oldIdols.size > 0) {
                    for (i in oldIdols.indices) {
                        val oldIdol = oldIdols[i]
                        oldIdol?.isFavorite = false
                        oldIdol?.isMost = false
                        for (k in idols.indices) {
                            val idol = idols[k]
                            if (idol.getId() == oldIdol?.getId()) {
                                // 어댑터에 있던 아이돌 정보를 최신것으로 갱신
                                oldIdols[i] = idol
                                break
                            }
                        }
                    }
                    mAdapter!!.addAll(oldIdols)
                } else {
                    mAdapter!!.addAll(idols)
                }

                // 최애 변경된 경우를 다시 적용
                val items = ArrayList(mAdapter!!.items)
                for (i in items.indices) {
                    val idol = items[i]
                    if (mAccount!!.most != null) {
                        idol?.isMost = idol?.getId() == mAccount!!.most!!.getId()
                    }
                }

                mtempFavorites.putAll(mFavorites)
                if (mAdapter!!.count > 0) {
                    binding.list.visibility = View.VISIBLE
                } else {
                    binding.list.visibility = View.GONE
                }
                mAdapter!!.notifyDataSetChanged()
                binding.searchBar.btnSearch.isEnabled = true
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun loadFavorites() {
        val response = getInstance().getCache(Const.KEY_FAVORITE)
        if (response == null) {
            lifecycleScope.launch {
                favoritesRepository.getFavoritesSelf(
                    { response ->
                        if (response.optBoolean("success")) {
                            setFavorites(response)
                            // api caching
                            getInstance()
                                .putCache(Const.KEY_FAVORITE, response, (60000 * 60).toLong())
                        } else {
                            handleCommonError(this@FavoriteSettingActivity, response)
                        }
                        binding.searchBar.btnSearch.isEnabled = true
                    }, {
                        makeText(
                            this@FavoriteSettingActivity,
                            R.string.error_abnormal_exception,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        } else {
            setFavorites(response)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_search) {
            doSearch()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_OPEN_COMMUNITY) {
            if (resultCode == RESULT_NEED_UPDATE) {
                showMost()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCheckedChanged(
        button: CompoundButton,
        isChecked: Boolean, item: IdolModel, context: Context
    ) {
        if (mAccount!!.heart == Const.LEVEL_MANAGER && button.id == R.id.btn_most) {
            val color = "#" + (Integer.toHexString(
                ContextCompat.getColor(
                    this, R.color.main
                )
            ).substring(2))
            val managerWarnMsg = HtmlCompat.fromHtml(
                (getString(if (BuildConfig.CELEB) R.string.actor_msg_manager_warning else R.string.msg_manager_warning)
                        + "<br>" + "<FONT color=" + color + ">" + "<br><b>" + getString(R.string.msg_continue) + "</b></FONT>"),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            Util.showDefaultIdolDialogWithBtn2(
                this,
                getString(R.string.lable_manager_warning),
                managerWarnMsg.toString(),
                { v: View? ->
                    Util.closeIdolDialog()
                    onCheckedChanged(button, isChecked, item, false, context)
                },
                { v: View? ->
                    Util.closeIdolDialog()
                    button.isChecked = false
                })
        } else {
            onCheckedChanged(button, isChecked, item, false, context)
        }

        // 즐찾에 변경 있으면
        getInstance().clearCache(Const.KEY_FAVORITE)
    }

    fun onCheckedChanged(
        button: CompoundButton,
        isChecked: Boolean, item: IdolModel, forceChange: Boolean, context: Context
    ) {
        when (button.id) {
            R.id.btn_favorite -> {
                if (!button.isEnabled) {
                    return
                }
                button.isEnabled = false
                if (isChecked) {
                    lifecycleScope.launch {
                        favoritesRepository.addFavorite(
                            item.getId(),
                            { response ->
                                if (response.optBoolean("success", true)) {
                                    val gcode = response.optInt("gcode")
                                    try {
                                        //								mFavorites.put(response.getInt("idol_id"),
                                        //										response.getInt("id"));
                                        // 여기 좀 이상함.. 응답은 {"gcode":0,"success":true} => 서버 수정하자
                                        mtempFavorites[response.getInt("idol_id")] =
                                            response.getInt("id")
                                    } catch (e: JSONException) {
                                        e.printStackTrace()
                                    }
                                    //							mIdols.get(item.getName()).setFavorite(isChecked);
                                    item.isFavorite = isChecked
                                    mAdapter!!.notifyDataSetChanged()
                                } else {
                                    button.isChecked = !isChecked
                                    val responseMsg =
                                        ErrorControl.parseError(this@FavoriteSettingActivity, response)
                                    if (responseMsg != null) {
                                        makeText(
                                            this@FavoriteSettingActivity,
                                            responseMsg,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                button.isEnabled = true
                            }, {
                                button.isChecked = !isChecked
                                button.isEnabled = true
                                makeText(
                                    this@FavoriteSettingActivity,
                                    R.string.error_abnormal_exception,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                } else {
                    // 여기도 좀 이상함... mtempFavorites에 넣고 빼는데 mFavorites에서 찾으면 어떡하지?? 검색 후 즐겨찾기에 추가->서버에 반영->바로 즐겨찾기 삭제->아래에 걸려서 아무 동작도 안함-> 서버에 남아있음
                    //				if (mFavorites.get(item.getId()) == null) {
                    //					button.setEnabled(true);
                    //					return;
                    //				}
                    if (mtempFavorites[item.getId()] == null) {
                        button.isEnabled = true
                        return
                    }
                    //int favoriteId = mFavorites.get(item.getId());
                    val favoriteId = mtempFavorites[item.getId()]!!
                    lifecycleScope.launch {
                        favoritesRepository.removeFavorite(
                            favoriteId,
                            { response ->
                                if (response.optBoolean("success")) {
                                    mtempFavorites.remove(item.getId())
                                    item.isFavorite = false
                                    mAdapter!!.notifyDataSetChanged()
                                    button.isEnabled = true
                                } else {
                                    button.isEnabled = true
                                    button.isChecked = !isChecked
                                    handleCommonError(this@FavoriteSettingActivity, response)
                                }
                            }, {
                                button.isEnabled = true
                                button.isChecked = !isChecked
                                makeText(
                                    this@FavoriteSettingActivity,
                                    R.string.error_abnormal_exception,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }

            R.id.btn_most -> showChangeMostDialog(
                context,
                if (isChecked) item else null,
                sharedAppState,
                { pair: Pair<Int?, Int?> ->
                    idolSoloFinalId = if (pair.first == null) -1 else pair.first!!
                    idolGroupFinalId = if (pair.second == null) -1 else pair.second!!
                    updateMost(if (isChecked) item else null, button)
                    null
                },
                {
                    button.isChecked = item.isMost
                    null
                })
        }
    }

    private fun updateMost(item: IdolModel?, btn: CompoundButton) {
        // Oreo+에서 최애 설정 후 2일이내에 변경 시도시 토스트가 안보여서

        binding.searchBar.etSearch.clearFocus()
        btn.isFocusable = true
        btn.requestFocus()

        val account = getAccount(this@FavoriteSettingActivity)

        lifecycleScope.launch {
            usersRepository.updateMost(
                userResourceUri = account?.userResourceUri!!,
                idolResourceUri = item?.resourceUri,
                listener = { response ->
                    if (response.optBoolean("success")) {
                        var preGroupId = 0
                        var isFirst = false

                        if (account.most == null) {
                            isFirst = true
                        } else {
                            val preMost = mIdols[account.most!!.resourceUri]
                            if (preMost != null) {
                                preGroupId = preMost.groupId
                                preMost.isMost = false
                            }
                        }

                        if (item != null) {
                            binding.favorite.text = item.getName(this@FavoriteSettingActivity)
                            mGlideRequestManager
                                ?.load(item.imageUrl)
                                ?.apply(mGlideOptions!!)
                                ?.into(binding.favoritePhoto)
                            mIdols[item.resourceUri]!!.isMost = true
                            mIdols[item.resourceUri]!!.isFavorite = true
                            account.userModel!!.most = item
                            Handler().postDelayed(
                                {
                                    accountManager.fetchUserInfo(this@FavoriteSettingActivity)
                                },1000
                            )
                            // 최애 변경 후 하트 표시가 제대로 안나와서
                            val items = mAdapter!!.items
                            for (i in items) {
                                i?.isMost = (i?.getId() == item.getId())
                            }

                            // 카테고리
                            Util.setPreference(
                                this@FavoriteSettingActivity,
                                Const.PREF_DEFAULT_CATEGORY, item.category
                            )
                        } else {
                            val items = mAdapter!!.items
                            for (i in items) {
                                i?.isMost = false
                            }

                            lifecycleScope.launch {
                                delay(1000)
                                accountManager.fetchUserInfo(this@FavoriteSettingActivity, success = {
                                    val most = mAccount?.userModel?.most
                                    if (most != null) {
                                        binding.favorite.text =
                                            most.getName(this@FavoriteSettingActivity)
                                        mGlideRequestManager
                                            ?.load(most.imageUrl)
                                            ?.apply(mGlideOptions!!)
                                            ?.into(binding.favoritePhoto)
                                    } else {
                                        binding.favorite.text = getString(R.string.none)
                                        binding.favoritePhoto.setImageResource(R.drawable.favorite_idol_empty_states)
                                    }
                                })
                            }
                        }
                        mAdapter!!.notifyDataSetChanged()

                        if (Util.getPreferenceBool(
                                this@FavoriteSettingActivity,
                                Const.PREF_SHOW_SET_NEW_FRIENDS, true
                            ) && !"F".equals(
                                ConfigModel.getInstance(
                                    this@FavoriteSettingActivity
                                ).friendApiBlock, ignoreCase = true
                            )
                        ) {
                            if (isFirst || (item != null && (!BuildConfig.CELEB && preGroupId != item.groupId))) {
                                Util.setPreference(
                                    this@FavoriteSettingActivity,
                                    Const.PREF_SHOW_SET_NEW_FRIENDS,
                                    false
                                )

                                Util.showDefaultIdolDialogWithBtn2(
                                    this@FavoriteSettingActivity,
                                    getString(R.string.new_friends),
                                    getString(R.string.apply_new_friends_desp),
                                    R.string.yes,
                                    R.string.no,
                                    true, false,
                                    { v1: View? ->
                                        Util.closeIdolDialog()
                                        startActivity(NewFriendsActivity.createIntent(this@FavoriteSettingActivity))
                                    },
                                    { v2: View? -> Util.closeIdolDialog() })
                            }
                        }
                        if (idolSoloFinalId != -1) {
                            ChatRoomList.getInstance(this@FavoriteSettingActivity)
                                .deleteRoomWithIdolId(idolSoloFinalId) { null }
                        }
                        if (!BuildConfig.CELEB && idolGroupFinalId != -1) {
                            ChatRoomList.getInstance(this@FavoriteSettingActivity)
                                .deleteRoomWithIdolId(idolGroupFinalId) { null }
                        }
                    } else {
                        // 뭔가 최애설정 실패하면 최애설정 버튼을 다시 빈 하트로
                        btn.isChecked = false
                        handleCommonError(this@FavoriteSettingActivity, response)
                    }
                    Util.closeProgress(1000)
                },
                errorListener = { throwable ->
                    val msg = throwable.message
                    Util.log("FavoriteSettingActivity $msg")
                    try {
                        btn.isChecked = false
                        Util.closeProgress(1000)
                        val json = JSONObject(msg)
                        val result = json.optBoolean("success")
                        if (!result) {
                            val responseMsg =
                                ErrorControl.parseError(this@FavoriteSettingActivity, json)
                            if (responseMsg != null) {
                                makeText(
                                    this@FavoriteSettingActivity,
                                    responseMsg,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Util.closeProgress(1000)
                        makeText(
                            this@FavoriteSettingActivity,
                            R.string.error_abnormal_exception,
                            Toast.LENGTH_SHORT
                        ).show()
                        if (Util.is_log()) {
                            showMessage(msg)
                        }
                    }
                }
            )
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        val item = mAdapter!!.getItem(position) ?: return
        openCommunity(item)
    }

    private fun openCommunity(model: IdolModel) {
        if (Util.mayShowLoginPopup(this)) {
            return
        }

        // 커뮤 진입 후 최애/즐찾 변경되는 경우 대비
        startActivityForResult(createIntent(this, model), REQUEST_OPEN_COMMUNITY)
    }

    //하트 개수 정렬
    private fun sort(idolModel: ArrayList<IdolModel>) {
        try {
            //OS 7 이상부터 정렬
            if (!BuildConfig.CELEB) {
                val heart = Comparator.comparing(IdolModel::heart).reversed()
                idolModel.sortWith(
                    Comparator.comparing(IdolModel::isViewable)
                        .reversed()
                        .thenComparing(heart)
                )
            } else {
                idolModel.sortByDescending { it.heart }
            }
        } catch (e: UnsupportedOperationException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val REQUEST_OPEN_COMMUNITY = 100
        const val RESULT_NEED_UPDATE: Int = 1

        fun createIntent(context: Context?): Intent {
            return Intent(context, FavoriteSettingActivity::class.java)
        }
    }
}
