# 마이그레이션 가이드 02: 아키텍처 패턴 변환

## 📋 목차
1. [아키텍처 패턴 비교](#아키텍처-패턴-비교)
2. [ViewModel 변환 가이드](#viewmodel-변환-가이드)
3. [상태 관리 변환](#상태-관리-변환)
4. [Effect 처리 변환](#effect-처리-변환)
5. [실제 변환 예시](#실제-변환-예시)

---

## 아키텍처 패턴 비교

### Old 프로젝트: MVVM 패턴

```
┌─────────────┐
│   Activity  │ ← View
│  /Fragment  │
└──────┬──────┘
       │ observe
       ▼
┌─────────────┐
│  ViewModel  │ ← ViewModel
│  (LiveData) │
└──────┬──────┘
       │ uses
       ▼
┌─────────────┐
│ Repository  │ ← Model
│  /UseCase   │
└─────────────┘
```

**특징**:
- LiveData 기반 상태 관리
- Observer 패턴 사용
- 일회성 이벤트는 Event Wrapper로 처리

### 현재 프로젝트: MVI 패턴

```
┌─────────────┐
│   Screen    │ ← View (Compose)
│ (Composable)│
└──────┬──────┘
       │ collect StateFlow / Effect
       ▼
┌─────────────┐
│  ViewModel  │ ← ViewModel
│ (StateFlow) │    - State: UiState
│  + Channel  │    - Intent: UiIntent
└──────┬──────┘    - Effect: UiEffect
       │ handleIntent
       ▼
┌─────────────┐
│ Repository  │ ← Model
│  /UseCase   │
└─────────────┘
```

**특징**:
- StateFlow 기반 상태 관리
- Intent 기반 사용자 액션 처리
- Effect로 Side Effect 분리

---

## ViewModel 변환 가이드

### 1. Base ViewModel 구조 이해

#### Old 프로젝트 Base ViewModel
```kotlin
// old/app/src/main/java/net/ib/mn/base/BaseViewModel.kt
open class BaseViewModel : ViewModel() {
    protected val _errorToast = MutableLiveData<Event<String>>()
    val errorToast: LiveData<Event<String>> = _errorToast
    
    protected val _errorToastWithJson = MutableLiveData<Event<JSONObject>>()
    val errorToastWithJson: LiveData<Event<JSONObject>> = _errorToastWithJson
    
    protected val _errorToastWithCode = MutableLiveData<Event<Int>>()
    val errorToastWithCode: LiveData<Event<Int>> = _errorToastWithCode
}
```

#### 현재 프로젝트 Base ViewModel
```kotlin
// app/src/main/java/net/ib/mn/base/BaseViewModel.kt
abstract class BaseViewModel<STATE : UiState, INTENT : UiIntent, EFFECT : UiEffect> : ViewModel() {
    
    abstract fun createInitialState(): STATE
    abstract fun handleIntent(intent: INTENT)
    
    private val _uiState: MutableStateFlow<STATE> = MutableStateFlow(initialState)
    val uiState: StateFlow<STATE> = _uiState.asStateFlow()
    
    private val _effect: Channel<EFFECT> = Channel()
    val effect = _effect.receiveAsFlow()
    
    protected val currentState: STATE
        get() = uiState.value
    
    protected fun setState(reduce: STATE.() -> STATE) {
        val newState = currentState.reduce()
        _uiState.value = newState
    }
    
    protected fun setEffect(builder: () -> EFFECT) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
    }
    
    fun sendIntent(intent: INTENT) {
        handleIntent(intent)
    }
}
```

### 2. ViewModel 변환 단계

#### Step 1: Contract 정의 (State, Intent, Effect)

**Old 방식**:
```kotlin
// Old ViewModel에 직접 포함
class MainViewModel : ViewModel() {
    private val _state = MutableLiveData<State>()
    val state: LiveData<State> = _state
    
    fun loadData() { ... }
    fun onButtonClick() { ... }
}
```

**변환 후**:
```kotlin
// Contract 클래스 생성
class MainContract {
    data class State(
        val isLoading: Boolean = false,
        val data: List<Item> = emptyList(),
        val error: String? = null
    ) : UiState
    
    sealed class Intent : UiIntent {
        data object LoadData : Intent()
        data object OnButtonClick : Intent()
        data class OnItemClick(val item: Item) : Intent()
    }
    
    sealed class Effect : UiEffect {
        data object NavigateToDetail : Effect()
        data class ShowError(val message: String) : Effect()
    }
}
```

#### Step 2: ViewModel 구현

**Old 방식**:
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: SomeRepository
) : ViewModel() {
    
    private val _state = MutableLiveData<MainState>()
    val state: LiveData<MainState> = _state
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _state.value = MainState(isLoading = true)
            try {
                val data = repository.getData()
                _state.value = MainState(data = data)
            } catch (e: Exception) {
                _state.value = MainState(error = e.message)
            }
        }
    }
    
    fun onButtonClick() {
        // 처리 로직
    }
}
```

**변환 후**:
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: SomeRepository
) : BaseViewModel<MainContract.State, MainContract.Intent, MainContract.Effect>() {
    
    override fun createInitialState() = MainContract.State()
    
    init {
        sendIntent(MainContract.Intent.LoadData)
    }
    
    override fun handleIntent(intent: MainContract.Intent) {
        when (intent) {
            is MainContract.Intent.LoadData -> loadData()
            is MainContract.Intent.OnButtonClick -> onButtonClick()
            is MainContract.Intent.OnItemClick -> onItemClick(intent.item)
        }
    }
    
    private fun loadData() {
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val data = repository.getData()
                setState { copy(isLoading = false, data = data) }
            } catch (e: Exception) {
                setState { copy(isLoading = false, error = e.message) }
                setEffect { MainContract.Effect.ShowError(e.message ?: "Unknown error") }
            }
        }
    }
    
    private fun onButtonClick() {
        // 처리 로직
        setEffect { MainContract.Effect.NavigateToDetail }
    }
    
    private fun onItemClick(item: Item) {
        // 처리 로직
    }
}
```

---

## 상태 관리 변환

### 1. LiveData → StateFlow 변환

#### Old: LiveData 사용
```kotlin
// ViewModel
private val _state = MutableLiveData<State>()
val state: LiveData<State> = _state

// Activity/Fragment
viewModel.state.observe(this) { state ->
    // UI 업데이트
}
```

#### 변환 후: StateFlow 사용
```kotlin
// ViewModel
private val _uiState = MutableStateFlow(State())
val uiState: StateFlow<State> = _uiState.asStateFlow()

// Compose Screen
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

### 2. 일회성 이벤트 처리

#### Old: Event Wrapper 패턴
```kotlin
// ViewModel
private val _errorToast = MutableLiveData<Event<String>>()
val errorToast: LiveData<Event<String>> = _errorToast

_errorToast.value = Event("에러 메시지")

// Activity/Fragment
viewModel.errorToast.observe(this) { event ->
    event.getContentIfNotHandled()?.let { message ->
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
```

#### 변환 후: Effect (Channel) 사용
```kotlin
// ViewModel
setEffect { MainContract.Effect.ShowError("에러 메시지") }

// Compose Screen
LaunchedEffect(Unit) {
    viewModel.effect.collectLatest { effect ->
        when (effect) {
            is MainContract.Effect.ShowError -> {
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

---

## Effect 처리 변환

### 1. 네비게이션

#### Old: Intent 기반
```kotlin
// ViewModel
fun navigateToDetail(itemId: String) {
    // Activity에서 직접 처리하거나 LiveData로 전달
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
// ViewModel
private fun onItemClick(item: Item) {
    setEffect { MainContract.Effect.NavigateToDetail(item.id) }
}

// Compose Screen
LaunchedEffect(Unit) {
    viewModel.effect.collectLatest { effect ->
        when (effect) {
            is MainContract.Effect.NavigateToDetail -> {
                navController.navigate("detail/${effect.itemId}")
            }
        }
    }
}
```

### 2. 다이얼로그 표시

#### Old: LiveData 이벤트
```kotlin
// ViewModel
private val _showDialog = MutableLiveData<Event<DialogData>>()
val showDialog: LiveData<Event<DialogData>> = _showDialog

fun showConfirmDialog() {
    _showDialog.value = Event(DialogData(...))
}

// Activity
viewModel.showDialog.observe(this) { event ->
    event.getContentIfNotHandled()?.let { data ->
        showDialog(data)
    }
}
```

#### 변환 후: Effect 사용
```kotlin
// ViewModel
fun showConfirmDialog() {
    setEffect { MainContract.Effect.ShowDialog(DialogData(...)) }
}

// Compose Screen
var dialogState by remember { mutableStateOf<DialogData?>(null) }

LaunchedEffect(Unit) {
    viewModel.effect.collectLatest { effect ->
        when (effect) {
            is MainContract.Effect.ShowDialog -> {
                dialogState = effect.data
            }
        }
    }
}

if (dialogState != null) {
    Dialog(...) { /* Dialog UI */ }
}
```

---

## 실제 변환 예시

### 예시 1: StartupActivity → StartUpScreen

#### Old: StartupActivity + StartupViewModel
```kotlin
// StartupViewModel (Old)
@HiltViewModel
class StartupViewModel @Inject constructor(
    private val getConfigUseCase: GetConfigStartupUseCase
) : ViewModel() {
    
    private val _state = MutableLiveData<StartupState>()
    val state: LiveData<StartupState> = _state
    
    private val _navigateToMain = MutableLiveData<Event<Unit>>()
    val navigateToMain: LiveData<Event<Unit>> = _navigateToMain
    
    fun initialize() {
        viewModelScope.launch {
            _state.value = StartupState(isLoading = true, progress = 0f)
            
            try {
                val config = getConfigUseCase()
                _state.value = StartupState(progress = 1f)
                _navigateToMain.value = Event(Unit)
            } catch (e: Exception) {
                _state.value = StartupState(error = e.message)
            }
        }
    }
}

// StartupActivity (Old)
class StartupActivity : BaseActivity() {
    private val viewModel: StartupViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)
        
        viewModel.state.observe(this) { state ->
            progressBar.progress = (state.progress * 100).toInt()
            if (state.error != null) {
                Toast.makeText(this, state.error, Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.navigateToMain.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        
        viewModel.initialize()
    }
}
```

#### 변환 후: StartUpScreen + StartUpViewModel
```kotlin
// StartUpContract
class StartUpContract {
    data class State(
        val progress: Float = 0f,
        val isLoading: Boolean = true,
        val error: String? = null
    ) : UiState
    
    sealed class Intent : UiIntent {
        data object Initialize : Intent()
        data object Retry : Intent()
    }
    
    sealed class Effect : UiEffect {
        data object NavigateToMain : Effect()
        data class ShowError(val message: String) : Effect()
    }
}

// StartUpViewModel
@HiltViewModel
class StartUpViewModel @Inject constructor(
    private val getConfigUseCase: GetConfigStartupUseCase
) : BaseViewModel<StartUpContract.State, StartUpContract.Intent, StartUpContract.Effect>() {
    
    override fun createInitialState() = StartUpContract.State()
    
    override fun handleIntent(intent: StartUpContract.Intent) {
        when (intent) {
            is StartUpContract.Intent.Initialize -> initialize()
            is StartUpContract.Intent.Retry -> retry()
        }
    }
    
    private fun initialize() {
        setState { copy(isLoading = true, progress = 0f) }
        viewModelScope.launch {
            try {
                val config = getConfigUseCase()
                setState { copy(progress = 1f, isLoading = false) }
                setEffect { StartUpContract.Effect.NavigateToMain }
            } catch (e: Exception) {
                setState { copy(isLoading = false, error = e.message) }
                setEffect { StartUpContract.Effect.ShowError(e.message ?: "Unknown error") }
            }
        }
    }
    
    private fun retry() {
        sendIntent(StartUpContract.Intent.Initialize)
    }
}

// StartUpScreen
@Composable
fun StartUpScreen(
    onNavigateToMain: () -> Unit,
    viewModel: StartUpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.sendIntent(StartUpContract.Intent.Initialize)
    }
    
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is StartUpContract.Effect.NavigateToMain -> onNavigateToMain()
                is StartUpContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    StartUpContent(state = state)
}
```

---

## 변환 체크리스트

### ViewModel 변환 시 확인사항
- [ ] Contract 클래스 생성 (State, Intent, Effect)
- [ ] BaseViewModel 상속 및 제네릭 타입 지정
- [ ] createInitialState() 구현
- [ ] handleIntent() 구현
- [ ] 모든 public 메서드를 Intent로 변환
- [ ] LiveData를 StateFlow로 변환
- [ ] Event Wrapper를 Effect로 변환
- [ ] 에러 처리를 Effect로 변환

### Compose Screen 변환 시 확인사항
- [ ] Contract 정의 확인
- [ ] ViewModel 초기화 (hiltViewModel())
- [ ] State 수집 (collectAsStateWithLifecycle())
- [ ] Effect 처리 (LaunchedEffect + collectLatest)
- [ ] Intent 전송 (sendIntent())
- [ ] 네비게이션 콜백 처리

---

## 다음 문서
- [마이그레이션 가이드 03: Activity → Compose Screen](./MIGRATION_GUIDE_03_ACTIVITY_TO_COMPOSE.md)
- [마이그레이션 가이드 04: UI 컴포넌트 변환](./MIGRATION_GUIDE_04_UI_COMPONENTS.md)

