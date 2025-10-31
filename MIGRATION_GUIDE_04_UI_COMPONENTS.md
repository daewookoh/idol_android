# 마이그레이션 가이드 04: UI 컴포넌트 변환

## 📋 목차
1. [UI 컴포넌트 매핑](#ui-컴포넌트-매핑)
2. [Custom View 변환](#custom-view-변환)
3. [RecyclerView → LazyColumn 변환](#recyclerview--lazycolumn-변환)
4. [Dialog 변환](#dialog-변환)
5. [리소스 변환](#리소스-변환)
6. [테마 및 스타일 변환](#테마-및-스타일-변환)

---

## UI 컴포넌트 매핑

### 기본 컴포넌트 매핑

| Old (View System) | Compose | 비고 |
|-------------------|---------|------|
| `TextView` | `Text` | |
| `EditText` | `TextField` / `OutlinedTextField` | |
| `Button` | `Button` / `TextButton` / `OutlinedButton` | |
| `ImageButton` | `IconButton` | |
| `ImageView` | `Image` / `AsyncImage` | Coil 사용 시 `AsyncImage` |
| `ProgressBar` | `CircularProgressIndicator` / `LinearProgressIndicator` | |
| `Switch` | `Switch` | |
| `CheckBox` | `Checkbox` | |
| `RadioButton` | `RadioButton` | |
| `SeekBar` | `Slider` | |
| `Toolbar` | `TopAppBar` / `SmallTopAppBar` / `MediumTopAppBar` | |
| `FloatingActionButton` | `FloatingActionButton` | |
| `CardView` | `Card` | |
| `WebView` | `AndroidView` + `WebView` | |

### 레이아웃 매핑

| Old (View System) | Compose | 비고 |
|-------------------|---------|------|
| `LinearLayout` (vertical) | `Column` | |
| `LinearLayout` (horizontal) | `Row` | |
| `FrameLayout` | `Box` | |
| `RelativeLayout` | `Box` with `Alignment` | |
| `ConstraintLayout` | `ConstraintLayout` (Compose) | |
| `ScrollView` | `Column` with `verticalScroll()` | |
| `HorizontalScrollView` | `Row` with `horizontalScroll()` | |
| `RecyclerView` | `LazyColumn` / `LazyRow` | |
| `ViewPager` | `HorizontalPager` / `VerticalPager` | |

---

## Custom View 변환

### 1. Custom View → Composable 함수

#### Old: Custom View 클래스
```kotlin
class CustomButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {
    
    init {
        setBackgroundColor(Color.RED)
        setTextColor(Color.WHITE)
        textSize = 16f
    }
    
    fun setCustomText(text: String) {
        this.text = text
    }
}
```

#### Compose: Composable 함수
```kotlin
@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.RED,
            contentColor = Color.WHITE
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp
        )
    }
}
```

### 2. 복잡한 Custom View 변환

#### Old: 복잡한 Custom View
```kotlin
class ProfileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    
    private val imageView: ImageView
    private val nameTextView: TextView
    private val descriptionTextView: TextView
    
    init {
        inflate(context, R.layout.view_profile, this)
        imageView = findViewById(R.id.imageView)
        nameTextView = findViewById(R.id.nameTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
    }
    
    fun setProfile(profile: Profile) {
        Glide.with(context).load(profile.imageUrl).into(imageView)
        nameTextView.text = profile.name
        descriptionTextView.text = profile.description
    }
}
```

#### Compose: Composable 함수
```kotlin
@Composable
fun ProfileView(
    profile: Profile,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AsyncImage(
            model = profile.imageUrl,
            contentDescription = profile.name,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = profile.description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
```

---

## RecyclerView → LazyColumn 변환

### 1. Adapter → LazyColumn

#### Old: RecyclerView Adapter
```kotlin
class FeedAdapter(
    private val items: List<FeedItem>,
    private val onItemClick: (FeedItem) -> Unit
) : RecyclerView.Adapter<FeedAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val image: ImageView = itemView.findViewById(R.id.image)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .into(holder.image)
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }
    
    override fun getItemCount() = items.size
}
```

#### Compose: LazyColumn
```kotlin
@Composable
fun FeedList(
    items: List<FeedItem>,
    onItemClick: (FeedItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(items) { item ->
            FeedItem(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun FeedItem(
    item: FeedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
```

### 2. 다양한 ViewType 처리

#### Old: Multiple ViewType Adapter
```kotlin
class MultiTypeAdapter(
    private val items: List<Item>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HeaderItem -> TYPE_HEADER
            is FeedItem -> TYPE_ITEM
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(...)
            TYPE_ITEM -> ItemViewHolder(...)
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(items[position] as HeaderItem)
            is ItemViewHolder -> holder.bind(items[position] as FeedItem)
        }
    }
}
```

#### Compose: Sealed Class 사용
```kotlin
sealed class ListItem {
    data class Header(val title: String) : ListItem()
    data class Feed(val item: FeedItem) : ListItem()
}

@Composable
fun MultiTypeList(
    items: List<ListItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(items) { item ->
            when (item) {
                is ListItem.Header -> HeaderItem(item.title)
                is ListItem.Feed -> FeedItem(item.item)
            }
        }
    }
}
```

### 3. Header, Footer 추가

#### Old: Header/Footer in Adapter
```kotlin
class FeedAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private const val TYPE_FOOTER = 2
    }
    
    override fun getItemCount() = items.size + 2 // header + footer
}
```

#### Compose: item() 사용
```kotlin
LazyColumn {
    // Header
    item {
        HeaderView()
    }
    
    // Items
    items(feedItems) { item ->
        FeedItem(item = item)
    }
    
    // Footer
    item {
        FooterView()
    }
}
```

---

## Dialog 변환

### 1. AlertDialog 변환

#### Old: AlertDialog
```kotlin
AlertDialog.Builder(context)
    .setTitle("Title")
    .setMessage("Message")
    .setPositiveButton("OK") { _, _ ->
        // OK 처리
    }
    .setNegativeButton("Cancel") { _, _ ->
        // Cancel 처리
    }
    .show()
```

#### Compose: AlertDialog
```kotlin
@Composable
fun CustomDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Title") },
        text = { Text("Message") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### 2. BottomSheetDialog 변환

#### Old: BottomSheetDialogFragment
```kotlin
class CustomBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet, container, false)
    }
}
```

#### Compose: ModalBottomSheet
```kotlin
@Composable
fun CustomBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss
        ) {
            // Bottom Sheet Content
        }
    }
}
```

### 3. Dialog State 관리

#### Compose에서 Dialog 표시
```kotlin
@Composable
fun FeedScreen(viewModel: FeedViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is FeedContract.Effect.ShowDialog -> {
                    showDialog = true
                }
            }
        }
    }
    
    if (showDialog) {
        CustomDialog(
            onDismiss = { showDialog = false },
            onConfirm = {
                showDialog = false
                viewModel.sendIntent(FeedContract.Intent.Confirm)
            }
        )
    }
}
```

---

## 리소스 변환

### 1. 문자열 리소스

#### Old: XML
```xml
<TextView android:text="@string/app_name" />
```

#### Compose
```kotlin
Text(stringResource(R.string.app_name))
```

### 2. 색상 리소스

#### Old: XML
```xml
<TextView android:textColor="@color/main" />
```

#### Compose
```kotlin
Text(
    text = "Text",
    color = colorResource(R.color.main)
)
```

### 3. Drawable 리소스

#### Old: XML
```xml
<ImageView android:src="@drawable/ic_heart" />
```

#### Compose
```kotlin
Image(
    painter = painterResource(R.drawable.ic_heart),
    contentDescription = "Heart"
)
```

### 4. Dimension 리소스

#### Old: XML
```xml
<TextView android:padding="@dimen/padding_16" />
```

#### Compose
```kotlin
Text(
    text = "Text",
    modifier = Modifier.padding(dimensionResource(R.dimen.padding_16))
)
```

---

## 테마 및 스타일 변환

### 1. Material Theme 설정

#### Old: themes.xml
```xml
<style name="Theme.App" parent="Theme.Material3.DayNight">
    <item name="colorPrimary">@color/main</item>
    <item name="colorSecondary">@color/secondary</item>
</style>
```

#### Compose: Theme.kt
```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFff4444), // main color
    secondary = Color(0xFF...),  // secondary color
    // ...
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE24848), // main color (dark)
    secondary = Color(0xFF...),  // secondary color (dark)
    // ...
)

@Composable
fun ExodusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### 2. Typography 설정

#### Old: styles.xml
```xml
<style name="TextAppearance.App.Title">
    <item name="android:textSize">20sp</item>
    <item name="android:textStyle">bold</item>
</style>
```

#### Compose: Type.kt
```kotlin
val Typography = Typography(
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    ),
    // ...
)
```

### 3. 컴포넌트 스타일

#### Old: 스타일 적용
```xml
<Button
    style="@style/Widget.App.Button"
    android:text="Click" />
```

#### Compose: Modifier 및 MaterialTheme 사용
```kotlin
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
    ),
    modifier = Modifier
        .height(48.dp)
        .fillMaxWidth()
) {
    Text(
        text = "Click",
        style = MaterialTheme.typography.labelLarge
    )
}
```

---

## 실제 변환 예시

### 예시: Custom Profile Card

#### Old: XML Layout + Custom View
```xml
<!-- view_profile_card.xml -->
<androidx.cardview.widget.CardView>
    <LinearLayout android:orientation="vertical">
        <ImageView android:id="@+id/profileImage" />
        <TextView android:id="@+id/name" />
        <TextView android:id="@+id/description" />
        <Button android:id="@+id/followButton" />
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

```kotlin
class ProfileCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : CardView(context, attrs) {
    
    private val profileImage: ImageView
    private val name: TextView
    private val description: TextView
    private val followButton: Button
    
    init {
        inflate(context, R.layout.view_profile_card, this)
        profileImage = findViewById(R.id.profileImage)
        name = findViewById(R.id.name)
        description = findViewById(R.id.description)
        followButton = findViewById(R.id.followButton)
    }
    
    fun setProfile(profile: Profile) {
        Glide.with(context).load(profile.imageUrl).into(profileImage)
        name.text = profile.name
        description.text = profile.description
        followButton.setOnClickListener { 
            onFollowClick?.invoke(profile)
        }
    }
    
    var onFollowClick: ((Profile) -> Unit)? = null
}
```

#### Compose: Composable 함수
```kotlin
@Composable
fun ProfileCard(
    profile: Profile,
    onFollowClick: (Profile) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            AsyncImage(
                model = profile.imageUrl,
                contentDescription = profile.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onFollowClick(profile) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Follow")
            }
        }
    }
}
```

---

## 변환 체크리스트

### Custom View 변환
- [ ] Custom View 클래스 확인
- [ ] 사용하는 속성 확인
- [ ] 이벤트 리스너 확인
- [ ] Composable 함수로 변환
- [ ] Preview 추가

### RecyclerView 변환
- [ ] Adapter 클래스 확인
- [ ] ViewHolder 확인
- [ ] LazyColumn으로 변환
- [ ] Item Composable 생성
- [ ] 클릭 이벤트 처리

### Dialog 변환
- [ ] Dialog 클래스 확인
- [ ] Compose Dialog로 변환
- [ ] State 관리
- [ ] Effect로 처리

### 리소스 변환
- [ ] 문자열 리소스 변환
- [ ] 색상 리소스 변환
- [ ] Drawable 리소스 변환
- [ ] Dimension 리소스 변환

### 테마 변환
- [ ] Color Scheme 정의
- [ ] Typography 정의
- [ ] Theme Composable 생성

---

## 다음 문서
- [마이그레이션 가이드 05: 의존성 및 라이브러리](./MIGRATION_GUIDE_05_DEPENDENCIES.md)
- [마이그레이션 가이드 06: 네비게이션 변환](./MIGRATION_GUIDE_06_NAVIGATION.md)

