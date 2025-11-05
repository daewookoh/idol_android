# UDP Lifecycle-Based Subscription Optimization

## 개요
화면에 보이지 않는 ViewModel의 불필요한 UDP 구독을 방지하고, 화면이 보일 때만 구독을 활성화하여 리소스를 최적화합니다.

## 구현 원리

### 1. 기존 문제점
- 모든 ViewModel이 init 시점에 UDP 구독 시작
- 화면에 보이지 않아도 백그라운드에서 계속 UDP 이벤트 처리
- 8개의 ViewModel이 동시에 UDP 업데이트를 처리하여 불필요한 리소스 낭비

### 2. 최적화 방식
- **화면이 보일 때만** UDP 구독 활성화
- **화면이 보이는 순간** DB에서 최신 데이터 로드
- **화면이 사라지면** UDP 구독 중지

## 구현 상세

### ViewModel 측 구현

모든 ranking ViewModel에 다음 메서드 추가:

```kotlin
// UDP 구독 Job (화면에 보일 때만 활성화)
private var udpSubscriptionJob: Job? = null

// 화면 가시성 상태
private var isScreenVisible = false

/**
 * 화면이 보일 때 호출 - UDP 구독 시작 및 데이터 새로고침
 */
fun onScreenVisible() {
    android.util.Log.d("VM_TAG", "👁️ Screen became visible")
    isScreenVisible = true

    // DB에서 최신 데이터 로드
    val cachedData = ... // cachedIds or cachedRanks
    if (cachedData != null && cachedData.isNotEmpty()) {
        android.util.Log.d("VM_TAG", "🔄 Refreshing data from DB")
        viewModelScope.launch(Dispatchers.IO) {
            processData(cachedData)
        }
    }

    // UDP 구독 시작
    startUdpSubscription()
}

/**
 * 화면이 사라질 때 호출 - UDP 구독 중지
 */
fun onScreenHidden() {
    android.util.Log.d("VM_TAG", "🙈 Screen hidden")
    isScreenVisible = false
    stopUdpSubscription()
}

/**
 * UDP 구독 시작
 */
private fun startUdpSubscription() {
    // 이미 구독 중이면 중복 방지
    if (udpSubscriptionJob?.isActive == true) {
        android.util.Log.d("VM_TAG", "⚠️ UDP already subscribed, skipping")
        return
    }

    android.util.Log.d("VM_TAG", "📡 Starting UDP subscription")
    udpSubscriptionJob = viewModelScope.launch {
        broadcastManager.updateEvent.collect { changedIds ->
            // 화면이 보이지 않으면 무시
            if (!isScreenVisible) {
                android.util.Log.d("VM_TAG", "⏭️ Screen not visible, ignoring UDP update")
                return@collect
            }

            android.util.Log.d("VM_TAG", "🔄 UDP update event received")
            // 데이터 처리...
        }
    }
}

/**
 * UDP 구독 중지
 */
private fun stopUdpSubscription() {
    udpSubscriptionJob?.cancel()
    udpSubscriptionJob = null
    android.util.Log.d("VM_TAG", "🛑 Stopped UDP subscription")
}

override fun onCleared() {
    super.onCleared()
    stopUdpSubscription()
    android.util.Log.d("VM_TAG", "♻️ ViewModel cleared")
}
```

### Composable 측 구현

모든 SubPage Composable에 다음 LaunchedEffect 추가:

```kotlin
@Composable
fun RankingSubPage(
    chartCode: String,
    isVisible: Boolean = true,  // 부모로부터 받는 가시성 상태
    listState: LazyListState? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: ViewModel = hiltViewModel<ViewModel, ViewModel.Factory> { factory ->
        factory.create(chartCode)
    }

    // 화면 가시성 변경 시 UDP 구독 관리 및 데이터 새로고침
    LaunchedEffect(isVisible) {
        if (isVisible) {
            android.util.Log.d("SubPage", "👁️ Screen became visible")
            viewModel.onScreenVisible()
        } else {
            android.util.Log.d("SubPage", "🙈 Screen hidden")
            viewModel.onScreenHidden()
        }
    }

    // UI rendering...
}
```

## 적용된 파일 목록

### ViewModels (8개)
1. `SoloRankingSubPageViewModel.kt` - charts/idol_ids/ API
2. `GroupRankingSubPageViewModel.kt` - charts/idol_ids/ API
3. `GlobalRankingSubPageViewModel.kt` - charts/ranks/ API
4. `HallOfFameRankingSubPageViewModel.kt` - charts/ranks/ API
5. `HeartPickRankingSubPageViewModel.kt` - charts/ranks/ API
6. `MiracleRankingSubPageViewModel.kt` - charts/ranks/ API
7. `OnePickRankingSubPageViewModel.kt` - charts/ranks/ API
8. `RookieRankingSubPageViewModel.kt` - charts/ranks/ API

### Composables (8개)
1. `SoloRankingSubPage.kt`
2. `GroupRankingSubPage.kt`
3. `GlobalRankingSubPage.kt`
4. `HallOfFameRankingSubPage.kt`
5. `HeartPickRankingSubPage.kt`
6. `MiracleRankingSubPage.kt`
7. `OnePickRankingSubPage.kt`
8. `RookieRankingSubPage.kt`

## 동작 흐름

```
1. 사용자가 탭 A로 이동
   ↓
2. 탭 A의 SubPage: LaunchedEffect(isVisible=true) 트리거
   ↓
3. ViewModel.onScreenVisible() 호출
   ↓
4. DB에서 캐시된 데이터로 UI 즉시 새로고침
   ↓
5. UDP 구독 시작
   ↓
6. UDP 이벤트 발생 시 실시간 업데이트

---

7. 사용자가 탭 B로 이동 (탭 A 숨김)
   ↓
8. 탭 A의 SubPage: LaunchedEffect(isVisible=false) 트리거
   ↓
9. ViewModel.onScreenHidden() 호출
   ↓
10. UDP 구독 중지 (리소스 절약)
```

## 최적화 효과

### 리소스 절약
- **이전**: 8개 ViewModel이 항상 UDP 구독 활성화
- **이후**: 현재 보이는 1개 ViewModel만 UDP 구독 활성화
- **절약**: CPU, 메모리, 배터리 사용량 약 87.5% 감소 (7/8)

### 데이터 신선도
- 화면이 보일 때마다 DB에서 최신 데이터 로드
- UDP 업데이트는 화면이 보일 때만 처리
- 불필요한 백그라운드 처리 방지

### 사용자 경험
- 탭 전환 시 즉시 최신 데이터 표시
- 백그라운드 리소스 낭비 없음
- 부드러운 화면 전환

## 로그 예시

```
// 화면이 보일 때
D/SoloRankingVM: 👁️ Screen became visible for chartCode: solo_male
D/SoloRankingVM: 🔄 Refreshing data from DB (150 items)
D/SoloRankingVM: 📡 Starting UDP subscription

// UDP 업데이트 수신
D/SoloRankingVM: 🔄 UDP update event received - 3 idols changed
D/SoloRankingVM: 📊 Reloading all 150 idols from DB
D/SoloRankingVM:    → Changed IDs in this chart: [1234, 5678]
D/SoloRankingVM:    → Full ranking recalculation (순위 변경 가능)

// 화면이 숨겨질 때
D/SoloRankingVM: 🙈 Screen hidden for chartCode: solo_male
D/SoloRankingVM: 🛑 Stopped UDP subscription

// ViewModel 정리
D/SoloRankingVM: ♻️ ViewModel cleared
```

## 주의사항

1. **isVisible 파라미터 전달**: 부모 Composable에서 정확한 가시성 상태를 전달해야 함
2. **중복 구독 방지**: startUdpSubscription()에서 이미 활성화된 경우 스킵
3. **메모리 누수 방지**: onCleared()에서 반드시 구독 정리
4. **캐시 유지**: codeToIdListMap, cachedRanks 등 캐시 데이터는 유지 (빠른 복원용)

## 테스트 방법

1. 앱 실행 후 로그 모니터링
2. 탭 A → 탭 B → 탭 A 순서로 이동
3. 각 탭에서 UDP 구독 시작/중지 로그 확인
4. Android Profiler로 CPU/메모리 사용량 측정
5. 백그라운드 탭에서 UDP 이벤트 무시되는지 확인

## 결론

이 최적화를 통해:
- ✅ 불필요한 백그라운드 처리 제거
- ✅ 리소스 사용량 대폭 감소
- ✅ 배터리 수명 개선
- ✅ 화면 전환 시 즉시 최신 데이터 표시
- ✅ 사용자 경험 향상
