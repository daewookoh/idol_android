# 마이그레이션 가이드 06: 네비게이션 변환

## 📋 목차
1. [네비게이션 방식 비교](#네비게이션-방식-비교)
2. [Intent 기반 → Navigation Compose 변환](#intent-기반--navigation-compose-변환)
3. [Deep Link 처리](#deep-link-처리)
4. [파라미터 전달](#파라미터-전달)
5. [Back Stack 관리](#back-stack-관리)
6. [실제 변환 예시](#실제-변환-예시)

---

## 네비게이션 방식 비교

### Old 프로젝트: Intent 기반 네비게이션

```kotlin
// Activity 간 이동
val intent = Intent(this, DetailActivity::class.java)
intent.putExtra("itemId", itemId)
intent.putExtra("title", title)
startActivity(intent)

// 결과 받기
startActivityForResult(intent, REQUEST_CODE)

// Fragment 전환
supportFragmentManager.beginTransaction()
    .replace(R.id.container, fragment)
    .addToBackStack(null)
    .commit()
```

**특징**:
- Intent 기반 Activity 전환
- FragmentManager로 Fragment 전환
- 파라미터는 Intent.putExtra()로 전달
- Deep Link는 AppLinkActivity에서 처리

### 현재 프로젝트: Navigation Compose

```kotlin
// Screen 간 이동
navController.navigate("detail/$itemId")

// 결과 받기
val resultLauncher = rememberLauncherForActivityResult(...)

// Navigation Graph에서 선언
NavHost(navController, startDestination = "main") {
    composable("main") { MainScreen(...) }
    composable("detail/{itemId}") { backStackEntry ->
        val itemId = backStackEntry.arguments?.getString("itemId")
        DetailScreen(itemId = itemId)
    }
}
```

**특징**:
- 타입 안전한 네비게이션
- 선언적 네비게이션 그래프
- 파라미터는 route에 포함 또는 arguments로 전달
- Deep Link는 NavGraph에서 선언

---

## Intent 기반 → Navigation Compose 변환

### 1. 기본 네비게이션 변환

#### Old: Intent 기반
```kotlin
// MainActivity에서
fun navigateToDetail(itemId: String) {
    val intent = Intent(this, DetailActivity::class.java)
    intent.putExtra("itemId", itemId)
    startActivity(intent)
}
```

#### 변환 후: Navigation Compose
```kotlin
// NavGraph.kt
NavHost(
    navController = navController,
    startDestination = "main"
) {
    composable("main") {
        MainScreen(
            onNavigateToDetail = { itemId ->
                navController.navigate("detail/$itemId")
            }
        )
    }
    
    composable("detail/{itemId}") { backStackEntry ->
        val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
        DetailScreen(itemId = itemId)
    }
}

// MainScreen.kt
@Composable
fun MainScreen(
    onNavigateToDetail: (String) -> Unit
) {
    // ...
    Button(onClick = { onNavigateToDetail("123") }) {
        Text("Go to Detail")
    }
}
```

### 2. ViewModel에서 네비게이션 처리

#### Old: ViewModel에서 LiveData로 전달
```kotlin
// ViewModel
private val _navigateToDetail = MutableLiveData<Event<String>>()
val navigateToDetail: LiveData<Event<String>> = _navigateToDetail

fun onItemClick(itemId: String) {
    _navigateToDetail.value = Event(itemId)
}

// Activity
viewModel.navigateToDetail.observe(this) { event ->
    event.getContentIfNotHandled()?.let { itemId ->
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra("itemId", itemId)
        startActivity(intent)
    }
}
```

#### 변환 후: Effect 사용
```kotlin
// Contract
sealed class Effect : UiEffect {
    data class NavigateToDetail(val itemId: String) : Effect()
}

// ViewModel
fun onItemClick(itemId: String) {
    setEffect { MainContract.Effect.NavigateToDetail(itemId) }
}

// Screen
LaunchedEffect(Unit) {
    viewModel.effect.collectLatest { effect ->
        when (effect) {
            is MainContract.Effect.NavigateToDetail -> {
                onNavigateToDetail(effect.itemId)
            }
        }
    }
}
```

---

## Deep Link 처리

### 1. Old 프로젝트: AppLinkActivity

#### AppLinkActivity.kt
```kotlin
class AppLinkActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val data: Uri? = intent.data
        if (data != null) {
            handleDeepLink(data)
        }
    }
    
    private fun handleDeepLink(uri: Uri) {
        when {
            uri.path?.startsWith("/feed/") == true -> {
                val feedId = uri.lastPathSegment
                val intent = Intent(this, FeedActivity::class.java)
                intent.putExtra("feedId", feedId)
                startActivity(intent)
            }
            uri.path?.startsWith("/user/") == true -> {
                val userId = uri.lastPathSegment
                val intent = Intent(this, UserActivity::class.java)
                intent.putExtra("userId", userId)
                startActivity(intent)
            }
        }
        finish()
    }
}
```

#### AndroidManifest.xml
```xml
<activity
    android:name=".link.AppLinkActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="choeaedol"
            android:host="www.myloveidol.com" />
    </intent-filter>
</activity>
```

### 2. 변환 후: Navigation Compose Deep Link

#### NavGraph.kt
```kotlin
NavHost(
    navController = navController,
    startDestination = "main"
) {
    composable("main") {
        MainScreen(...)
    }
    
    composable(
        route = "feed/{feedId}",
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "choeaedol://www.myloveidol.com/feed/{feedId}"
            },
            navDeepLink {
                uriPattern = "https://www.myloveidol.com/feed/{feedId}"
            }
        )
    ) { backStackEntry ->
        val feedId = backStackEntry.arguments?.getString("feedId") ?: ""
        FeedScreen(feedId = feedId)
    }
    
    composable(
        route = "user/{userId}",
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "choeaedol://www.myloveidol.com/user/{userId}"
            }
        )
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: ""
        UserScreen(userId = userId)
    }
}
```

#### MainActivity.kt
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val navController = rememberNavController()
            
            // Deep Link 처리
            val uri = intent.data
            if (uri != null) {
                LaunchedEffect(uri) {
                    navController.navigate(uri)
                }
            }
            
            NavGraph(navController = navController)
        }
    }
}
```

---

## 파라미터 전달

### 1. 기본 파라미터 전달

#### Old: Intent.putExtra()
```kotlin
val intent = Intent(this, DetailActivity::class.java)
intent.putExtra("itemId", itemId)
intent.putExtra("title", title)
intent.putExtra("isEditable", true)
startActivity(intent)

// 받기
val itemId = intent.getStringExtra("itemId")
val title = intent.getStringExtra("title")
val isEditable = intent.getBooleanExtra("isEditable", false)
```

#### 변환 후: Route 파라미터
```kotlin
// 전달
navController.navigate("detail/$itemId?title=$title&isEditable=true")

// 받기
composable("detail/{itemId}") { backStackEntry ->
    val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
    val title = backStackEntry.arguments?.getString("title") ?: ""
    val isEditable = backStackEntry.arguments?.getBoolean("isEditable") ?: false
    
    DetailScreen(
        itemId = itemId,
        title = title,
        isEditable = isEditable
    )
}
```

### 2. 타입 안전한 파라미터 전달

#### NavType 정의
```kotlin
composable(
    route = "detail/{itemId}",
    arguments = listOf(
        navArgument("itemId") { type = NavType.StringType },
        navArgument("title") { 
            type = NavType.StringType
            defaultValue = ""
        },
        navArgument("isEditable") {
            type = NavType.BoolType
            defaultValue = false
        }
    )
) { backStackEntry ->
    val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
    val title = backStackEntry.arguments?.getString("title") ?: ""
    val isEditable = backStackEntry.arguments?.getBoolean("isEditable") ?: false
    
    DetailScreen(
        itemId = itemId,
        title = title,
        isEditable = isEditable
    )
}
```

### 3. 복잡한 객체 전달

#### Old: Serializable/Parcelable
```kotlin
// 전달
val intent = Intent(this, DetailActivity::class.java)
intent.putExtra("item", item as Serializable)
startActivity(intent)

// 받기
val item = intent.getSerializableExtra("item") as? Item
```

#### 변환 후: Navigation Compose
```kotlin
// 방법 1: JSON으로 변환
val itemJson = Gson().toJson(item)
navController.navigate("detail?itemJson=$itemJson")

// 방법 2: ViewModel에 저장하고 ID만 전달 (권장)
viewModel.setSelectedItem(item)
navController.navigate("detail/${item.id}")

// 방법 3: Type-safe navigation (Navigation Compose Type-Safe Args)
// (build.gradle에 플러그인 추가 필요)
```

---

## Back Stack 관리

### 1. Back 버튼 처리

#### Old: onBackPressed()
```kotlin
override fun onBackPressed() {
    if (supportFragmentManager.backStackEntryCount > 0) {
        supportFragmentManager.popBackStack()
    } else {
        super.onBackPressed()
    }
}
```

#### 변환 후: NavController
```kotlin
@Composable
fun ScreenWithBackButton(
    navController: NavController
) {
    // Back 버튼 자동 처리됨
    // 또는 수동 처리
    IconButton(onClick = { navController.popBackStack() }) {
        Icon(Icons.Default.ArrowBack, "Back")
    }
}
```

### 2. Back Stack 조작

#### Old: Fragment Back Stack
```kotlin
supportFragmentManager.popBackStack("tag", FragmentManager.POP_BACK_STACK_INCLUSIVE)
supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
```

#### 변환 후: NavController
```kotlin
// 특정 destination까지 pop
navController.popBackStack("main", inclusive = false)

// 모든 back stack pop
navController.popBackStack("main", inclusive = true)

// 특정 destination으로 이동 (back stack 초기화)
navController.navigate("main") {
    popUpTo("main") { inclusive = true }
}
```

### 3. 결과 반환

#### Old: Activity Result
```kotlin
// 시작
startActivityForResult(intent, REQUEST_CODE)

// 결과 받기
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
        val result = data?.getStringExtra("result")
        // 처리
    }
}

// 결과 반환
setResult(RESULT_OK, Intent().putExtra("result", "success"))
finish()
```

#### 변환 후: Navigation Compose
```kotlin
// 방법 1: Callback 함수
@Composable
fun EditScreen(
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    // ...
    Button(onClick = { onSave(newText) }) {
        Text("Save")
    }
}

// NavGraph
composable("edit") {
    EditScreen(
        onSave = { result ->
            navController.popBackStack()
            // 결과 처리
        },
        onCancel = { navController.popBackStack() }
    )
}

// 방법 2: SavedStateHandle 사용
@Composable
fun EditScreen(
    viewModel: EditViewModel = hiltViewModel()
) {
    // ...
    Button(onClick = { 
        viewModel.save()
        navController.popBackStack()
    }) {
        Text("Save")
    }
}
```

---

## 실제 변환 예시

### 예시: FeedActivity → FeedScreen 네비게이션

#### Old: FeedActivity에서 다른 화면으로 이동
```kotlin
class FeedActivity : BaseActivity() {
    private fun setupRecyclerView() {
        adapter = FeedAdapter { item ->
            // Detail로 이동
            val intent = Intent(this, FeedDetailActivity::class.java)
            intent.putExtra("feedId", item.id)
            intent.putExtra("title", item.title)
            startActivity(intent)
        }
        
        adapter.setOnUserClick { user ->
            // User Profile로 이동
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("userId", user.id)
            startActivity(intent)
        }
        
        adapter.setOnCommentClick { feedId ->
            // Comment로 이동
            val intent = Intent(this, CommentActivity::class.java)
            intent.putExtra("feedId", feedId)
            startActivityForResult(intent, REQUEST_COMMENT)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_COMMENT && resultCode == RESULT_OK) {
            // Comment 작성 완료 처리
            viewModel.refreshFeed()
        }
    }
}
```

#### 변환 후: FeedScreen 네비게이션

**1. Contract 정의**
```kotlin
class FeedContract {
    // ...
    sealed class Effect : UiEffect {
        data class NavigateToDetail(val feedId: String, val title: String) : Effect()
        data class NavigateToUserProfile(val userId: String) : Effect()
        data class NavigateToComment(val feedId: String) : Effect()
    }
}
```

**2. ViewModel**
```kotlin
@HiltViewModel
class FeedViewModel @Inject constructor(...)
    : BaseViewModel<FeedContract.State, FeedContract.Intent, FeedContract.Effect>() {
    
    fun onFeedClick(feedId: String, title: String) {
        setEffect { FeedContract.Effect.NavigateToDetail(feedId, title) }
    }
    
    fun onUserClick(userId: String) {
        setEffect { FeedContract.Effect.NavigateToUserProfile(userId) }
    }
    
    fun onCommentClick(feedId: String) {
        setEffect { FeedContract.Effect.NavigateToComment(feedId) }
    }
}
```

**3. NavGraph**
```kotlin
NavHost(
    navController = navController,
    startDestination = "feed"
) {
    composable("feed") {
        FeedScreen(
            onNavigateToDetail = { feedId, title ->
                navController.navigate("feed/detail/$feedId?title=${Uri.encode(title)}")
            },
            onNavigateToUserProfile = { userId ->
                navController.navigate("user/$userId")
            },
            onNavigateToComment = { feedId ->
                navController.navigate("comment/$feedId")
            }
        )
    }
    
    composable("feed/detail/{feedId}") { backStackEntry ->
        val feedId = backStackEntry.arguments?.getString("feedId") ?: ""
        val title = backStackEntry.arguments?.getString("title") ?: ""
        FeedDetailScreen(feedId = feedId, title = title)
    }
    
    composable("user/{userId}") { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: ""
        UserProfileScreen(userId = userId)
    }
    
    composable("comment/{feedId}") { backStackEntry ->
        val feedId = backStackEntry.arguments?.getString("feedId") ?: ""
        CommentScreen(
            feedId = feedId,
            onCommentSaved = {
                navController.popBackStack()
                // FeedScreen에서 자동으로 refresh됨
            }
        )
    }
}
```

**4. FeedScreen**
```kotlin
@Composable
fun FeedScreen(
    onNavigateToDetail: (String, String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToComment: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is FeedContract.Effect.NavigateToDetail -> {
                    onNavigateToDetail(effect.feedId, effect.title)
                }
                is FeedContract.Effect.NavigateToUserProfile -> {
                    onNavigateToUserProfile(effect.userId)
                }
                is FeedContract.Effect.NavigateToComment -> {
                    onNavigateToComment(effect.feedId)
                }
            }
        }
    }
    
    FeedContent(
        state = state,
        onFeedClick = { feed -> 
            viewModel.onFeedClick(feed.id, feed.title) 
        },
        onUserClick = { user -> 
            viewModel.onUserClick(user.id) 
        },
        onCommentClick = { feedId -> 
            viewModel.onCommentClick(feedId) 
        }
    )
}
```

---

## 변환 체크리스트

### 네비게이션 변환
- [ ] Intent 기반 네비게이션 → Navigation Compose 변환
- [ ] NavGraph 정의
- [ ] Route 파라미터 정의
- [ ] Deep Link 처리
- [ ] Back Stack 관리

### 파라미터 전달
- [ ] Intent.putExtra() → Route 파라미터 변환
- [ ] Serializable/Parcelable 객체 처리
- [ ] 타입 안전한 파라미터 정의

### 결과 반환
- [ ] Activity Result → Callback 함수 변환
- [ ] 결과 처리 로직 변환

### Deep Link
- [ ] AppLinkActivity → NavGraph Deep Link 변환
- [ ] URI 패턴 정의
- [ ] MainActivity에서 Deep Link 처리

---

## 다음 문서
- [마이그레이션 가이드 07: 테스트 및 검증](./MIGRATION_GUIDE_07_TESTING.md)

