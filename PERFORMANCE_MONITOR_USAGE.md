# PerformanceMonitor 사용 가이드

`net.ib.mn.util.PerformanceMonitor`를 사용하여 코드의 성능을 측정할 수 있습니다.

## 📦 위치
```
app/src/main/java/net/ib/mn/util/PerformanceMonitor.kt
```

## 🎯 사용 방법

### 1. 기본 사용 (start - checkpoint - end)

```kotlin
class MyViewModel @Inject constructor() : ViewModel() {

    private val perfMonitor = PerformanceMonitor()

    private suspend fun loadData() {
        perfMonitor.start("DataLoad")

        // DB 쿼리
        val data = database.query()
        perfMonitor.checkpoint("DataLoad", "DB Query Complete")

        // 데이터 가공
        val processed = processData(data)
        perfMonitor.checkpoint("DataLoad", "Processing Complete")

        // 완료
        perfMonitor.end("DataLoad", itemCount = processed.size)
    }
}
```

### 2. 간단한 일회성 측정

```kotlin
// 동기 함수
val result = PerformanceMonitor.measure("QuickTask") {
    // 측정할 코드
    heavyCalculation()
}

// suspend 함수
val result = PerformanceMonitor.measureSuspend("AsyncTask") {
    // 측정할 suspend 코드
    apiCall()
}
```

### 3. 조건부 측정 (UDP 업데이트 vs 초기 로딩)

```kotlin
private suspend fun queryData(isUpdate: Boolean = false) {
    val tag = if (isUpdate) "Query_Update" else "Query_Initial"

    if (!isUpdate) {
        perfMonitor.start(tag)
    }

    // 쿼리 실행
    val data = database.query()

    if (!isUpdate) {
        perfMonitor.checkpoint(tag, "Query Complete")
    }

    // 처리
    val result = process(data)

    if (!isUpdate) {
        perfMonitor.end(tag, result.size)
    }
}
```

## 📊 출력 예시

### start - checkpoint - end 패턴
```
D/Performance: [DataLoad] 📍 Started
D/Performance: [DataLoad] 🔵 Checkpoint: DB Query Complete
               ⏱️  Elapsed: 45ms
               💾 Memory Delta: 128KB
D/Performance: [DataLoad] 🔵 Checkpoint: Processing Complete
               ⏱️  Elapsed: 87ms
               💾 Memory Delta: 256KB
D/Performance: [DataLoad] ✅ Completed
               ⏱️  Duration: 158ms
               💾 Memory Delta: 568KB
               💾 Current Memory: 45MB
               📊 Items: 100 (1.58ms/item)
```

### measure 패턴
```
D/Performance: [QuickTask] ⚡ Quick Measure
               ⏱️  Duration: 23ms
               💾 Memory Delta: 64KB
```

## 🎨 적용 예시

### ViewModel에서 사용

```kotlin
@HiltViewModel
class RankingViewModel @Inject constructor(
    private val repository: RankingRepository,
    private val dao: IdolDao
) : ViewModel() {

    private val perfMonitor = PerformanceMonitor()

    fun loadRanking() {
        viewModelScope.launch(Dispatchers.IO) {
            perfMonitor.start("LoadRanking")

            // 1. API 호출
            val ids = repository.getRankingIds()
            perfMonitor.checkpoint("LoadRanking", "API Complete")

            // 2. DB 조회
            val idols = dao.getIdolsByIds(ids)
            perfMonitor.checkpoint("LoadRanking", "DB Query Complete")

            // 3. 정렬
            val sorted = idols.sortedByDescending { it.heart }
            perfMonitor.checkpoint("LoadRanking", "Sorting Complete")

            // 4. UI 데이터 변환
            val items = sorted.map { it.toRankingItem() }
            perfMonitor.end("LoadRanking", items.size)

            _uiState.value = UiState.Success(items)
        }
    }
}
```

### Repository에서 사용

```kotlin
class RankingRepositoryImpl @Inject constructor(
    private val api: RankingApi,
    private val dao: IdolDao
) : RankingRepository {

    override suspend fun syncRanking() = flow {
        emit(Loading)

        val result = PerformanceMonitor.measureSuspend("SyncRanking") {
            try {
                // API 호출
                val response = api.getRanking()

                // DB 저장
                dao.upsertAll(response.idols)

                Success(response)
            } catch (e: Exception) {
                Error(e.message)
            }
        }

        emit(result)
    }
}
```

### UseCase에서 사용

```kotlin
class GetRankingUseCase @Inject constructor(
    private val repository: RankingRepository
) {
    suspend operator fun invoke(): Result<List<Idol>> {
        return PerformanceMonitor.measureSuspend("GetRanking") {
            repository.getRanking()
                .map { it.sortByRank() }
                .getOrElse { emptyList() }
        }
    }
}
```

## 🔧 프로덕션 빌드 최적화

### BuildConfig로 제어

```kotlin
// app/build.gradle.kts에서
android {
    buildTypes {
        debug {
            buildConfigField("boolean", "ENABLE_PERFORMANCE_LOG", "true")
        }
        release {
            buildConfigField("boolean", "ENABLE_PERFORMANCE_LOG", "false")
        }
    }
}

// Application 클래스에서
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PerformanceMonitor.ENABLED = BuildConfig.ENABLE_PERFORMANCE_LOG
    }
}
```

### 또는 개별 제어

```kotlin
// 특정 ViewModel에서만 비활성화
class MyViewModel @Inject constructor() : ViewModel() {
    private val perfMonitor = PerformanceMonitor().apply {
        // 이 ViewModel에서는 측정 비활성화
        // (PerformanceMonitor는 인스턴스별 제어 불가, 전역 ENABLED 사용)
    }
}
```

## 📋 권장 측정 지점

### ViewModel
- [ ] 초기 데이터 로딩
- [ ] 실시간 업데이트 (UDP/WebSocket)
- [ ] 탭/필터 전환
- [ ] 검색/정렬

### Repository
- [ ] API 호출
- [ ] DB 대량 조회/쓰기
- [ ] 캐시 동기화

### UseCase
- [ ] 복잡한 비즈니스 로직
- [ ] 데이터 변환/가공

## ⚠️ 주의사항

### 1. 메모리 측정의 한계
```kotlin
// ❌ 정확하지 않을 수 있음
perfMonitor.start("Task")
val data = loadHugeData() // 큰 데이터
perfMonitor.end("Task")

// ✅ Android Profiler로 정확한 메모리 측정 권장
```

### 2. 중첩 측정 주의
```kotlin
// ❌ 같은 인스턴스로 중첩 측정 불가
perfMonitor.start("Outer")
perfMonitor.start("Inner") // Outer가 덮어씌워짐
perfMonitor.end("Inner")
perfMonitor.end("Outer")

// ✅ 별도 인스턴스 사용
val outerMonitor = PerformanceMonitor()
val innerMonitor = PerformanceMonitor()

outerMonitor.start("Outer")
innerMonitor.start("Inner")
innerMonitor.end("Inner")
outerMonitor.end("Outer")

// ✅ 또는 measure 사용
perfMonitor.start("Outer")
PerformanceMonitor.measure("Inner") {
    // inner task
}
perfMonitor.end("Outer")
```

### 3. 프로덕션 빌드에서 비활성화
```kotlin
// Application.onCreate()에서
PerformanceMonitor.ENABLED = BuildConfig.DEBUG
```

## 🎯 실전 팁

### 1. 병목 구간 찾기
```kotlin
perfMonitor.start("FullFlow")
perfMonitor.checkpoint("FullFlow", "Step 1") // 45ms
perfMonitor.checkpoint("FullFlow", "Step 2") // 120ms ← 병목!
perfMonitor.checkpoint("FullFlow", "Step 3") // 15ms
perfMonitor.end("FullFlow")
```

### 2. Before/After 비교
```kotlin
// Before 최적화
D/Performance: [LoadRanking] ✅ Completed
               ⏱️  Duration: 458ms
               💾 Memory Delta: 2048KB

// After 최적화
D/Performance: [LoadRanking] ✅ Completed
               ⏱️  Duration: 158ms  ← 65% 개선!
               💾 Memory Delta: 568KB  ← 72% 개선!
```

### 3. 아이템당 평균 시간 활용
```kotlin
perfMonitor.end("ProcessItems", itemCount = 1000)
// 출력: 📊 Items: 1000 (0.15ms/item)

// 기준: 아이템당 1ms 이하면 양호
// 1ms 이상이면 최적화 검토
```

## 📚 관련 문서

- [PERFORMANCE_GUIDE.md](./PERFORMANCE_GUIDE.md) - 전체 성능 측정 가이드
- Android Studio Profiler - 시각적 분석
- Layout Inspector - Compose 리컴포지션 분석

---

**작성일**: 2025-11-05
**작성자**: Claude Code
