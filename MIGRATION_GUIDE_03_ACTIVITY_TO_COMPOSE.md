# 마이그레이션 가이드 03: Activity → Compose Screen 변환

## 📋 목차
1. [변환 프로세스 개요](#변환-프로세스-개요)
2. [Activity 분석 및 구조 파악](#activity-분석-및-구조-파악)
3. [Compose Screen 생성](#compose-screen-생성)
4. [UI 레이아웃 변환](#ui-레이아웃-변환)
   - [리소스 마이그레이션 규칙 (필수)](#4-리소스-마이그레이션-규칙)
5. [이벤트 처리 변환](#이벤트-처리-변환)
6. [실제 변환 예시](#실제-변환-예시)
7. [주의사항 및 트러블슈팅](#주의사항-및-트러블슈팅)

---

## 변환 프로세스 개요

### 변환 단계
```
1. Old Activity 분석
   ├── 레이아웃 파일 (XML) 확인
   ├── ViewModel 확인
   ├── 비즈니스 로직 확인
   └── 네비게이션 흐름 확인

2. Contract 정의
   ├── State 정의
   ├── Intent 정의
   └── Effect 정의

3. ViewModel 변환
   ├── BaseViewModel 상속
   ├── StateFlow로 상태 관리
   └── Effect로 Side Effect 처리

4. Compose Screen 생성
   ├── Screen Composable 생성
   ├── Content Composable 생성 (Stateless)
   └── Preview 추가

5. UI 변환
   ├── XML Layout → Compose UI
   ├── ViewBinding → Composable
   └── 리소스 매핑

6. 통합 및 테스트
   ├── Navigation Graph 업데이트
   ├── 테스트
   └── Flavor별 확인
```

---

## Activity 분석 및 구조 파악

### 1. Old Activity 구조 분석 방법

#### Step 1: Activity 파일 확인
```kotlin
// old/app/src/main/java/net/ib/mn/activity/FeedActivity.kt
class FeedActivity : BaseActivity() {
    private lateinit var binding: ActivityFeedBinding
    private val viewModel: FeedViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        observeViewModel()
    }
    
    private fun setupViews() {
        binding.recyclerView.adapter = adapter
        binding.fab.setOnClickListener { ... }
    }
    
    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            // UI 업데이트
        }
    }
}
```

#### Step 2: 레이아웃 파일 확인
```xml
<!-- activity_feed.xml -->
<LinearLayout>
    <androidx.appcompat.widget.Toolbar />
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView" />
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab" />
</LinearLayout>
```

#### Step 3: ViewModel 확인
```kotlin
// old/app/src/main/java/net/ib/mn/viewmodel/FeedViewModel.kt
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: FeedRepository
) : ViewModel() {
    private val _state = MutableLiveData<FeedState>()
    val state: LiveData<FeedState> = _state
    
    fun loadFeed() { ... }
    fun refreshFeed() { ... }
}
```

### 2. 분석 체크리스트

- [ ] Activity 클래스 위치 및 패키지 확인
- [ ] 사용하는 Layout XML 파일 확인
- [ ] ViewModel 클래스 확인
- [ ] 사용하는 Adapter 확인
- [ ] 사용하는 Dialog/Fragment 확인
- [ ] 네비게이션 대상 확인 (어디로 이동하는지)
- [ ] Intent 파라미터 확인 (어떤 데이터를 받는지)
- [ ] Flavor별 차이 확인 (app, celeb, china, onestore)

---

## Compose Screen 생성

### 1. Screen 구조 패턴

#### 기본 구조
```kotlin
// 1. Contract 정의
class FeedContract {
    data class State(...) : UiState
    sealed class Intent : UiIntent { ... }
    sealed class Effect : UiEffect { ... }
}

// 2. ViewModel
@HiltViewModel
class FeedViewModel @Inject constructor(...)
    : BaseViewModel<FeedContract.State, FeedContract.Intent, FeedContract.Effect>() {
    // ...
}

// 3. Screen Composable (Stateful)
@Composable
fun FeedScreen(
    onNavigateBack: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            // Side Effect 처리
        }
    }
    
    // Stateless Content
    FeedContent(
        state = state,
        onIntent = viewModel::sendIntent
    )
}

// 4. Content Composable (Stateless)
@Composable
private fun FeedContent(
    state: FeedContract.State,
    onIntent: (FeedContract.Intent) -> Unit
) {
    // UI 구현
}

// 5. Preview
@Preview
@Composable
fun FeedScreenPreview() {
    FeedContent(
        state = FeedContract.State(...),
        onIntent = {}
    )
}
```

### 2. 파일 구조 생성

```
app/src/main/java/net/ib/mn/presentation/feed/
├── FeedContract.kt        # State, Intent, Effect 정의
├── FeedScreen.kt          # Screen Composable
└── FeedViewModel.kt        # ViewModel
```

---

## UI 레이아웃 변환

### 1. XML Layout → Compose 변환 매핑

| XML Layout | Compose |
|------------|---------|
| `LinearLayout` (vertical) | `Column` |
| `LinearLayout` (horizontal) | `Row` |
| `FrameLayout` | `Box` |
| `RelativeLayout` | `Box` with `Alignment` |
| `ConstraintLayout` | `ConstraintLayout` (Compose) |
| `RecyclerView` | `LazyColumn` / `LazyRow` |
| `TextView` | `Text` |
| `EditText` | `TextField` / `OutlinedTextField` |
| `Button` | `Button` / `TextButton` / `OutlinedButton` |
| `ImageButton` | `IconButton` |
| `ImageView` | `Image` |
| `ProgressBar` | `CircularProgressIndicator` / `LinearProgressIndicator` |
| `Switch` | `Switch` |
| `CheckBox` | `Checkbox` |
| `RadioButton` | `RadioButton` |
| `Toolbar` | `TopAppBar` / `SmallTopAppBar` |
| `FloatingActionButton` | `FloatingActionButton` |
| `CardView` | `Card` |
| `ScrollView` | `Column` with `verticalScroll` |

### 2. 레이아웃 변환 예시

#### XML 예시
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">
    
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Title"
        android:textSize="20sp"
        android:textStyle="bold" />
    
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
    
    <Button
        android:id="@+id/button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Click Me" />
</LinearLayout>
```

#### Compose 변환
```kotlin
@Composable
fun FeedContent(
    state: FeedContract.State,
    onIntent: (FeedContract.Intent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Title",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(state.items) { item ->
                FeedItem(item = item)
            }
        }
        
        Button(
            onClick = { onIntent(FeedContract.Intent.OnButtonClick) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Click Me")
        }
    }
}
```

### 3. 리소스 참조 변환

#### Old: XML 리소스 참조
```xml
<TextView
    android:text="@string/app_name"
    android:textColor="@color/main"
    android:background="@drawable/bg_button" />
```

#### Compose: 리소스 참조
```kotlin
Text(
    text = stringResource(R.string.app_name),
    color = colorResource(R.color.main),
    modifier = Modifier.background(painterResource(R.drawable.bg_button))
)
```

### 4. 리소스 마이그레이션 규칙

#### ⚠️ 필수 규칙: 기존 리소스 재사용

**Rule 1: XML 레이아웃 및 이미지 파일 복사**
- **MUST**: Old 프로젝트에서 사용하는 XML 레이아웃 파일 및 이미지 리소스는 **모두 복사**하여 사용
- **WHY**: 동일한 디자인을 유지하고, 기존 리소스 재사용으로 작업 효율성 향상
- **HOW**:
  - Old 프로젝트의 `res/layout/`, `res/drawable/` 폴더에서 해당 파일들을 찾아 복사
  - 같은 경로에 동일한 이름으로 배치

```bash
# 예시: XML 레이아웃 복사
old/app/src/main/res/layout/activity_feed.xml
  → app/src/main/res/layout/activity_feed.xml (참고용)

# 예시: 이미지 리소스 복사
old/app/src/main/res/drawable/icon_heart.xml
  → app/src/main/res/drawable/icon_heart.xml
```

**Rule 2: String 리소스는 절대 새로 추가하지 말 것**
- **MUST**: 모든 String 리소스는 **이미 `strings.xml`에 정의되어 있음**
- **DO**: 기존 `strings.xml`에서 적절한 키를 찾아서 사용
- **DON'T**: 새로운 String 리소스를 추가하지 말 것
- **WHY**: 다국어 지원이 이미 완료되어 있으며, 중복 키 방지 및 일관성 유지

```kotlin
// ✅ GOOD: 기존 String 리소스 사용
Text(stringResource(R.string.guide_vote_title))
Text(stringResource(R.string.see_result))

// ❌ BAD: 하드코딩하지 말 것
Text("투표하기")

// ❌ BAD: 새로운 String 리소스 추가하지 말 것
// <string name="new_vote_text">투표하기</string>
```

**String 리소스 검색 방법**:
```kotlin
// 1. 프로젝트 내 검색
// app/src/main/res/values/strings.xml 파일에서 키워드 검색

// 2. Old 프로젝트 참고
// old/app/src/main/res/values/strings.xml에서 사용된 키 확인
// old/app/src/main/res/values-ko/strings.xml (한국어)
// old/app/src/main/res/values-en/strings.xml (영어)
// old/app/src/main/res/values-ja/strings.xml (일본어)
```

**Rule 3: Color 값은 ColorPalette 사용 필수**
- **MUST**: 모든 Color 값은 **`ColorPalette` 객체에서만** 가져와 사용
- **DO NOT**: `colorResource(R.color.xxx)` 사용 금지
- **DO NOT**: `Color(0xFFXXXXXX)` 하드코딩 금지
- **IMPORTANT**: 필요한 색상이 `ColorPalette`에 없을 경우, **작업 시작 전에 반드시 알려줄 것**

```kotlin
// ✅ GOOD: ColorPalette 사용
Text(
    text = "Title",
    color = ColorPalette.textDefault
)

Box(
    modifier = Modifier.background(ColorPalette.main)
)

// ❌ BAD: colorResource 사용 금지
Text(
    color = colorResource(R.color.main)  // ❌ 사용하지 말 것
)

// ❌ BAD: 하드코딩 금지
Text(
    color = Color(0xFF6200EE)  // ❌ 사용하지 말 것
)
```

**ColorPalette에 색상이 없는 경우 대응 방법**:
```kotlin
// ⚠️ 작업 시작 전 체크 필수
// 1. Old 프로젝트에서 사용하는 색상 확인
//    old/app/src/main/res/values/colors.xml

// 2. 해당 색상이 ColorPalette에 있는지 확인
//    app/src/main/java/net/ib/mn/ui/theme/Color.kt

// 3. 없으면 작업 시작 전에 알림
//    "ColorPalette에 'primary_variant' 색상이 없습니다.
//     Old 프로젝트의 @color/primary_variant (#FF03DAC5) 색상을
//     ColorPalette에 추가해야 합니다."
```

**리소스 사용 체크리스트**:
- [ ] XML 레이아웃 파일을 Old 프로젝트에서 복사했는가?
- [ ] 이미지 리소스(drawable)를 Old 프로젝트에서 복사했는가?
- [ ] 모든 텍스트에 `stringResource(R.string.xxx)` 사용했는가?
- [ ] 새로운 String 리소스를 추가하지 않았는가?
- [ ] 모든 색상을 `ColorPalette`에서 가져왔는가?
- [ ] `colorResource()` 또는 하드코딩된 `Color()`를 사용하지 않았는가?
- [ ] 필요한 색상이 ColorPalette에 없는 경우 사전에 알렸는가?

---

## 이벤트 처리 변환

### 1. 클릭 이벤트

#### Old: ViewBinding
```kotlin
binding.button.setOnClickListener {
    viewModel.onButtonClick()
}
```

#### Compose
```kotlin
Button(
    onClick = { onIntent(FeedContract.Intent.OnButtonClick) }
) {
    Text("Click")
}
```

### 2. 텍스트 입력 이벤트

#### Old: ViewBinding
```kotlin
binding.editText.addTextChangedListener(object : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        viewModel.onTextChanged(s.toString())
    }
    // ...
})
```

#### Compose
```kotlin
var text by remember { mutableStateOf("") }

TextField(
    value = text,
    onValueChange = { newText ->
        text = newText
        onIntent(FeedContract.Intent.OnTextChanged(newText))
    }
)
```

### 3. 리스트 아이템 클릭

#### Old: Adapter
```kotlin
class FeedAdapter(private val onItemClick: (Item) -> Unit) : RecyclerView.Adapter<...>() {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            onItemClick(items[position])
        }
    }
}
```

#### Compose
```kotlin
LazyColumn {
    items(state.items) { item ->
        FeedItem(
            item = item,
            onClick = { onIntent(FeedContract.Intent.OnItemClick(item)) }
        )
    }
}
```

---

## 실제 변환 예시

### 예시: FeedActivity → FeedScreen

#### Old: FeedActivity
```kotlin
class FeedActivity : BaseActivity() {
    private lateinit var binding: ActivityFeedBinding
    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: FeedAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        
        viewModel.loadFeed()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupRecyclerView() {
        adapter = FeedAdapter { item ->
            val intent = Intent(this, FeedDetailActivity::class.java)
            intent.putExtra("itemId", item.id)
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when {
                state.isLoading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                }
                state.error != null -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.error, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    adapter.submitList(state.items)
                }
            }
        }
        
        viewModel.refreshComplete.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
```

#### 변환 후: FeedScreen

**1. Contract 정의**
```kotlin
class FeedContract {
    data class State(
        val isLoading: Boolean = false,
        val items: List<FeedItem> = emptyList(),
        val error: String? = null,
        val isRefreshing: Boolean = false
    ) : UiState
    
    sealed class Intent : UiIntent {
        data object LoadFeed : Intent()
        data object RefreshFeed : Intent()
        data class OnItemClick(val item: FeedItem) : Intent()
    }
    
    sealed class Effect : UiEffect {
        data class NavigateToDetail(val itemId: String) : Effect()
        data class ShowError(val message: String) : Effect()
    }
}
```

**2. ViewModel 변환**
```kotlin
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: FeedRepository
) : BaseViewModel<FeedContract.State, FeedContract.Intent, FeedContract.Effect>() {
    
    override fun createInitialState() = FeedContract.State()
    
    override fun handleIntent(intent: FeedContract.Intent) {
        when (intent) {
            is FeedContract.Intent.LoadFeed -> loadFeed()
            is FeedContract.Intent.RefreshFeed -> refreshFeed()
            is FeedContract.Intent.OnItemClick -> onItemClick(intent.item)
        }
    }
    
    private fun loadFeed() {
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val items = repository.getFeed()
                setState { copy(isLoading = false, items = items) }
            } catch (e: Exception) {
                setState { copy(isLoading = false, error = e.message) }
                setEffect { FeedContract.Effect.ShowError(e.message ?: "Unknown error") }
            }
        }
    }
    
    private fun refreshFeed() {
        setState { copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                val items = repository.refreshFeed()
                setState { copy(isRefreshing = false, items = items) }
            } catch (e: Exception) {
                setState { copy(isRefreshing = false, error = e.message) }
                setEffect { FeedContract.Effect.ShowError(e.message ?: "Unknown error") }
            }
        }
    }
    
    private fun onItemClick(item: FeedItem) {
        setEffect { FeedContract.Effect.NavigateToDetail(item.id) }
    }
}
```

**3. Screen Composable**
```kotlin
@Composable
fun FeedScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.sendIntent(FeedContract.Intent.LoadFeed)
    }
    
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is FeedContract.Effect.NavigateToDetail -> {
                    onNavigateToDetail(effect.itemId)
                }
                is FeedContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    FeedContent(
        state = state,
        onIntent = viewModel::sendIntent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun FeedContent(
    state: FeedContract.State,
    onIntent: (FeedContract.Intent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feed") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                // Error UI
            }
            else -> {
                SwipeRefresh(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onIntent(FeedContract.Intent.RefreshFeed) }
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        items(state.items) { item ->
                            FeedItem(
                                item = item,
                                onClick = { onIntent(FeedContract.Intent.OnItemClick(item)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
```

---

## 주의사항 및 트러블슈팅

### 1. BaseActivity 기능 확인

Old 프로젝트의 `BaseActivity`는 많은 공통 기능을 제공합니다:
- ExoPlayer 관리 (움짤 재생)
- Socket 관리
- Firebase Analytics
- 다이얼로그 표시
- 다크모드 설정

**대응 방법**:
- ExoPlayer: Compose에서도 동일하게 사용 가능 (`remember { ExoPlayer(...) }`)
- Socket: Application 레벨에서 관리하거나 ViewModel에서 관리
- Firebase Analytics: ViewModel이나 UseCase에서 처리
- 다이얼로그: Compose `Dialog` 사용
- 다크모드: MaterialTheme에서 자동 처리

### 2. Fragment 사용 확인

Old 프로젝트에서 Activity 내부에 Fragment를 사용하는 경우:
- Fragment를 별도의 Compose Screen으로 변환
- 또는 Compose 내부에서 `HorizontalPager` 등으로 처리

### 3. ViewBinding 데이터 바인딩

Old 프로젝트에서 DataBinding을 사용하는 경우:
- Compose는 선언적이므로 자동으로 양방향 바인딩이 아님
- `remember`와 `mutableStateOf`로 상태 관리

### 4. Flavor별 차이 처리

```kotlin
// Old: BuildConfig 사용
if (BuildConfig.CELEB) {
    // celeb 전용 코드
}

// Compose: 동일하게 사용 가능
if (BuildConfig.CELEB) {
    // celeb 전용 UI
}
```

### 5. 리소스 참조

```kotlin
// Old: XML에서 직접 참조
android:text="@string/app_name"

// Compose: 함수로 참조
Text(stringResource(R.string.app_name))
```

---

## 변환 체크리스트

### Activity 분석
- [ ] Activity 클래스 위치 확인
- [ ] Layout XML 파일 확인
- [ ] ViewModel 확인
- [ ] Adapter 확인
- [ ] 네비게이션 대상 확인
- [ ] Intent 파라미터 확인
- [ ] Flavor별 차이 확인

### Contract 정의
- [ ] State 정의
- [ ] Intent 정의
- [ ] Effect 정의

### ViewModel 변환
- [ ] BaseViewModel 상속
- [ ] createInitialState() 구현
- [ ] handleIntent() 구현
- [ ] StateFlow로 상태 관리
- [ ] Effect로 Side Effect 처리

### Compose Screen 생성
- [ ] Screen Composable 생성
- [ ] Content Composable 생성 (Stateless)
- [ ] Preview 추가
- [ ] Effect 처리

### UI 변환
- [ ] XML Layout → Compose 변환
- [ ] 리소스 참조 변환
- [ ] 이벤트 처리 변환
- [ ] Adapter → LazyColumn 변환

### 통합
- [ ] Navigation Graph 업데이트
- [ ] 테스트
- [ ] Flavor별 확인

---

## 다음 문서
- [마이그레이션 가이드 04: UI 컴포넌트 변환](./MIGRATION_GUIDE_04_UI_COMPONENTS.md)
- [마이그레이션 가이드 05: 의존성 및 라이브러리](./MIGRATION_GUIDE_05_DEPENDENCIES.md)

