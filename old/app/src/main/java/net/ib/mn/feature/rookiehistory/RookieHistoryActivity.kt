package net.ib.mn.feature.rookiehistory

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.ActivityRookieHistoryBinding
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.model.CharityModel
import net.ib.mn.model.SuperRookie
import net.ib.mn.utils.NetworkImage
import net.ib.mn.utils.ext.applySystemBarInsets
import java.util.Objects
import javax.inject.Inject

@AndroidEntryPoint
class RookieHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityRookieHistoryBinding
    private lateinit var rookieHistoryViewModel: RookieHistoryViewModel
    @Inject
    lateinit var idolsRepository: IdolsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.let {
            it.title = getString(R.string.rookie_month_title)
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_rookie_history)
        binding.lifecycleOwner = this
        binding.flContainer.applySystemBarInsets()

        val factory = RookieHistoryViewModelFactory(this, SavedStateHandle(), idolsRepository)
        rookieHistoryViewModel =
            ViewModelProvider(this, factory)[RookieHistoryViewModel::class.java]

        binding.cvRookieHistory.setContent {
            MaterialTheme {
                RookieHistoryScreen(
                    rookieHistoryViewModel = rookieHistoryViewModel,
                    onClickHistory = {
                        openHistoryDialog(
                            it.imageUrl,
                            it.linkUrl
                        )
                    }
                )
            }
        }
    }

    // TODO 차후 다이얼로그들 리뉴얼 되면 그떄 Compose로 하는게 좋을듯
    private fun openHistoryDialog(imgUrl: String, linkUrl: String) {
        val mSheet =
            BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_HISTORY, imgUrl, linkUrl)
        val tag = "filter"
        val oldFrag: Fragment? =
            Objects.requireNonNull<FragmentActivity>(this).supportFragmentManager.findFragmentByTag(
                tag
            )
        if (oldFrag == null) {
            mSheet.show(this.supportFragmentManager, tag)
        }
    }

    @Composable
    fun RookieHistoryScreen(
        rookieHistoryViewModel: RookieHistoryViewModel,
        onClickHistory: (CharityModel) -> Unit = { },
    ) {
        val context = LocalContext.current

        val uiState by rookieHistoryViewModel.uiState.collectAsStateWithLifecycle()

        var currentIndex by remember { mutableIntStateOf(0) }
        var currentSearchKeyword by remember { mutableStateOf("") }

        val backgroundColor =
            Color(ContextCompat.getColor(LocalContext.current, R.color.background_100))

        when (uiState) {
            is RookieHistoryUiState.Loading -> {
                // Loading
            }

            is RookieHistoryUiState.Success -> {
                val successState = uiState as RookieHistoryUiState.Success

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                ) {
                    if (currentIndex == 0) {
                        RookieHistory(
                            modifier = Modifier
                                .weight(1f),
                            currentSearchKeyword = currentSearchKeyword,
                            historyList = successState.charityModels,
                            onChangeSearchKeyword = {
                                currentSearchKeyword = it

                            },
                            onClickHistory = onClickHistory
                        )
                    } else {
                        SuperRookieItem(
                            modifier = Modifier
                                .weight(1f),
                            superRookieList = successState.superRookieModels
                        )
                    }

                    Row {
                        BottomTabItem(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentIndex = 0 },
                            isSelect = currentIndex == 0,
                            tabTitle = context.getString(R.string.record_room)
                        )
                        BottomTabItem(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentIndex = 1 },
                            isSelect = currentIndex == 1,
                            tabTitle = context.getString(R.string.super_rookie_history_tab)
                        )
                    }
                }
            }

            is RookieHistoryUiState.Error -> {
                // Error
            }
        }
    }

    @Composable
    fun RookieHistory(
        modifier: Modifier = Modifier,
        currentSearchKeyword: String = "",
        historyList: List<CharityModel>,
        onChangeSearchKeyword: (String) -> Unit = {},
        onClickHistory: (CharityModel) -> Unit = { }
    ) {
        val context = LocalContext.current
        val configuration = LocalConfiguration.current

        val backgroundColor =
            Color(ContextCompat.getColor(LocalContext.current, R.color.background_100))
        val emptyTextColor =
            Color(ContextCompat.getColor(LocalContext.current, R.color.text_dimmed))

        var filteredList by remember {
            mutableStateOf(historyList)
        }

        var cachedSearchKeyword by remember {
            mutableStateOf(currentSearchKeyword)
        }

        if (historyList.isEmpty() && currentSearchKeyword.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 40.dp),
                    text = context.getString(R.string.record_rookie_month_default),
                    color = emptyTextColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            filteredList = if (currentSearchKeyword.isEmpty()) {
                historyList
            } else {
                historyList.filter {
                    it.idolName.contains(currentSearchKeyword)
                } as ArrayList<CharityModel>
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .then(modifier),
                userScrollEnabled = !(filteredList.isEmpty() && currentSearchKeyword.isNotEmpty())
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(color = backgroundColor)
                            .padding(horizontal = 16.dp)
                    ) {
                        SearchBar(
                            modifier = Modifier
                                .align(Alignment.Center),
                            currentSearchKeyword = cachedSearchKeyword,
                            doSearch = { searchKeyword ->
                                onChangeSearchKeyword(searchKeyword)
                            },
                            onValueChange = { searchKeyword ->
                                cachedSearchKeyword = searchKeyword
                            }
                        )
                    }
                }

                if (filteredList.isEmpty() && currentSearchKeyword.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(configuration.screenHeightDp.dp - 138.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = context.getString(R.string.no_search_result),
                                color = emptyTextColor,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                items(filteredList.size) {
                    RookieHistoryItem(
                        modifier = Modifier
                            .clickable { onClickHistory(filteredList[it]) },
                        charityModel = filteredList[it]
                    )
                }
            }
        }
    }

    @Composable
    fun SearchBar(
        modifier: Modifier = Modifier,
        currentSearchKeyword: String = "",
        doSearch: (String) -> Unit = {},
        onValueChange: (String) -> Unit = {}
    ) {
        val context = LocalContext.current

        val backgroundColor =
            Color(ContextCompat.getColor(LocalContext.current, R.color.background_300))
        val textFieldTextColor =
            Color(ContextCompat.getColor(LocalContext.current, R.color.text_default))
        val textFieldHintColor =
            Color(ContextCompat.getColor(LocalContext.current, R.color.text_dimmed))

        val keyboardController = LocalSoftwareKeyboardController.current

        Box(
            modifier = Modifier
                .height(38.dp)
                .fillMaxWidth()
                .background(color = backgroundColor, shape = RoundedCornerShape(9.dp))
                .then(modifier)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(start = 14.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .weight(1f),
                    value = currentSearchKeyword,
                    onValueChange = { newText ->
                        onValueChange(newText)

                    },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textFieldTextColor
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        doSearch(currentSearchKeyword)
                    }),
                    enabled = true,
                    decorationBox = { innerTextField ->
                        if (currentSearchKeyword.isEmpty()) {
                            Text(
                                text = context.getString(R.string.hint_search_idol),
                                color = textFieldHintColor,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                )
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = {
                        doSearch(currentSearchKeyword)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.btn_navigation_search),
                        contentDescription = "Search",
                        tint = Color.Unspecified
                    )
                }
            }
        }
    }

    @Composable
    fun RookieHistoryItem(
        modifier: Modifier = Modifier,
        charityModel: CharityModel
    ) {
        val context = LocalContext.current
        val backgroundColor =
            Color(ContextCompat.getColor(LocalContext.current, R.color.background_100))
        val textColor = Color(ContextCompat.getColor(LocalContext.current, R.color.text_default))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .padding(horizontal = 20.dp)
                .background(backgroundColor)
                .then(modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Surface(
                modifier = Modifier
                    .size(62.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                NetworkImage(
                    modifier = Modifier
                        .size(62.dp)
                        .background(
                            backgroundColor, shape = RoundedCornerShape(6.dp)
                        ),
                    context = context,
                    imageUrl = charityModel.imageUrl
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = charityModel.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor,
                )
                Text(
                    text = charityModel.idolName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.btn_go),
                contentDescription = "Arrow",
                tint = Color.Unspecified
            )
        }
    }

    @Composable
    fun BottomTabItem(
        modifier: Modifier = Modifier,
        tabTitle: String = "",
        isSelect: Boolean
    ) {
        val backgroundColor =
            Color(ContextCompat.getColor(LocalContext.current, R.color.background_100))
        val topDividerColor = Color(ContextCompat.getColor(LocalContext.current, R.color.gray150))
        val selectDividerColor = Color(ContextCompat.getColor(LocalContext.current, R.color.main))

        val textColor = if (isSelect) {
            Color(ContextCompat.getColor(LocalContext.current, R.color.main))
        } else {
            Color(ContextCompat.getColor(LocalContext.current, R.color.text_dimmed))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(backgroundColor)
                .then(modifier)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(topDividerColor)
            )
            if (isSelect) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp)
                        .height(3.dp)
                        .background(selectDividerColor)
                )
            }

            Text(
                modifier = Modifier
                    .align(Alignment.Center),
                text = tabTitle,
                fontSize = 13.sp,
                color = textColor
            )
        }
    }

    @Composable
    fun SuperRookieItem(
        modifier: Modifier = Modifier,
        superRookieList: List<SuperRookie> = arrayListOf()
    ) {
        val context = LocalContext.current

        val backgroundColor =
            Color(ContextCompat.getColor(LocalContext.current, R.color.background_100))
        val boxBackgroundColor =
            Color(ContextCompat.getColor(LocalContext.current, R.color.background_300))
        val textColor = Color(ContextCompat.getColor(LocalContext.current, R.color.text_default))
        val textColorDimmed =
            Color(ContextCompat.getColor(LocalContext.current, R.color.text_dimmed))

        if (superRookieList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(bottom = 40.dp)
                    .then(modifier),
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(
                    modifier = Modifier
                        .height(20.dp)
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 37.dp),
                    text = context.getString(R.string.record_super_rookie_default),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = textColorDimmed
                )
                Spacer(
                    modifier = Modifier
                        .height(12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 89.dp)
                        .padding(horizontal = 20.dp)
                        .background(boxBackgroundColor, RoundedCornerShape(15.dp))
                ) {
                    Column(
                        modifier = Modifier,
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = context.getString(R.string.record_super_rookie_info_title),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 34.dp),
                            text = context.getString(R.string.record_super_rookie_info_body),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            ),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
                Spacer(
                    modifier = Modifier
                        .height(12.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .then(modifier),
            ) {
                item {
                    Column {
                        Spacer(
                            modifier = Modifier
                                .height(20.dp)
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 37.dp),
                            text = context.getString(R.string.record_super_rookie),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(
                            modifier = Modifier
                                .height(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 89.dp)
                                .padding(horizontal = 20.dp)
                                .background(boxBackgroundColor, RoundedCornerShape(15.dp))
                        ) {
                            Column(
                                modifier = Modifier,
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = context.getString(R.string.record_super_rookie_info_title),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 34.dp),
                                    text = context.getString(R.string.record_super_rookie_info_body),
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = textColor,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(
                                            includeFontPadding = false
                                        )
                                    ),
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                        Spacer(
                            modifier = Modifier
                                .height(12.dp)
                        )
                    }
                }
                items(superRookieList.size) {
                    SuperRookieItem(
                        superRookie = superRookieList[it]
                    )
                }
            }
        }
    }

    @Composable
    fun SuperRookieItem(
        superRookie: SuperRookie
    ) {
        val context = LocalContext.current

        val boxBackgroundColor =
            Color(ContextCompat.getColor(LocalContext.current, R.color.background_300))
        val textColor = Color(ContextCompat.getColor(LocalContext.current, R.color.text_default))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .width(44.dp),
                text = getString(R.string.record_super_rookie_number, superRookie.ordinal.toString()),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = textColor
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = boxBackgroundColor,
                        shape = CircleShape
                    )
            ) {
                NetworkImage(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                    context = context,
                    imageUrl = superRookie.imageUrl
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                text = superRookie.idolName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }

    @Preview
    @Composable
    fun PreviewRookieHistoryScreen() {
        RookieHistoryScreen(RookieHistoryViewModel(this, SavedStateHandle(), idolsRepository))
    }

    @Preview(
        uiMode = UI_MODE_NIGHT_YES
    )
    @Composable
    fun PreviewRookieHistoryItem() {
        RookieHistoryItem(
            charityModel = CharityModel(
                "",
                "Title",
                "IdolName",
                "GroupName",
                "LinkUrl"
            )
        )
    }

    @Preview
    @Composable
    fun PreviewBottomTabBar() {
        BottomTabItem(isSelect = true)
    }

    @Preview
    @Composable
    fun PreviewSuperRookie() {
        SuperRookieItem()
    }

    @Preview
    @Composable
    fun PreviewSuperRookieItem() {
        SuperRookieItem(SuperRookie())
    }

    @Preview
    @Composable
    fun PreviewRookieHistory() {
        RookieHistory(historyList = ArrayList())
    }

    @Preview
    @Composable
    fun PreviewSearchBar() {
        SearchBar()
    }
}