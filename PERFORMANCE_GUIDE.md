# SoloRankingSubPage 성능 측정 가이드

성능 측정 코드가 추가되었습니다. 이제 Logcat에서 실시간으로 성능 지표를 확인할 수 있습니다.

## 📊 측정되는 항목

### 1. ViewModel 성능 (자동 측정)

#### 초기 로딩 성능
```
Tag: "QueryDB_Initial"
- DB 쿼리 시간
- 정렬 및 순위 계산 시간
- 데이터 매핑 시간
- 전체 메모리 사용량
- 아이템당 평균 처리 시간
```

#### UDP 업데이트 성능
```
Tag: "UDP_Update"
- 업데이트 전체 소요 시간
- 메모리 증감
- 아이템당 평균 처리 시간
```

## 🔍 Logcat 필터 사용법

### Android Studio에서:
1. **View → Tool Windows → Logcat**
2. 상단 필터 바에서 다음 중 선택:

#### 성능 측정만 보기
```
tag:Performance
```

#### 리컴포지션 경고만 보기
```
tag:Recomposition
```

#### 모든 성능 관련 로그
```
tag:Performance|Recomposition
```

#### ViewModel 로그 포함
```
tag:Performance|Recomposition|SoloRankingVM
```

## 📈 로그 출력 예시

### 초기 로딩
```
D/Performance: [QueryDB_Initial] 📍 Started
D/Performance: [QueryDB_Initial] 🔵 Checkpoint: DB Query Complete
               ⏱️  Elapsed: 45ms
               💾 Memory Delta: 128KB
D/Performance: [QueryDB_Initial] 🔵 Checkpoint: Sorting & Ranking Complete
               ⏱️  Elapsed: 72ms
               💾 Memory Delta: 256KB
D/Performance: [QueryDB_Initial] 🔵 Checkpoint: Data Mapping Complete
               ⏱️  Elapsed: 145ms
               💾 Memory Delta: 512KB
D/Performance: [QueryDB_Initial] ✅ Completed
               ⏱️  Duration: 158ms
               💾 Memory Delta: 568KB
               💾 Current Memory: 45MB
               📊 Items: 100 (1.58ms/item)
```

### UDP 업데이트
```
D/Performance: [UDP_Update] 📍 Started
D/Performance: [UDP_Update] ✅ Completed
               ⏱️  Duration: 89ms
               💾 Memory Delta: 64KB
               💾 Current Memory: 46MB
               📊 Items: 100 (0.89ms/item)
```

### 리컴포지션 경고
```
W/Recomposition: ⚠️ Item 123 (지수) recomposed 5 times
E/Recomposition: 🔴 Item 456 (제니) recomposed 15 times - Optimization needed!
```

## ✅ 좋은 성능 기준

### 초기 로딩
- **전체 시간**: < 200ms
- **DB 쿼리**: < 50ms
- **정렬/순위**: < 30ms
- **데이터 매핑**: < 100ms
- **메모리 증가**: < 1MB

### UDP 업데이트
- **전체 시간**: < 100ms
- **메모리 증가**: < 200KB
- **아이템당**: < 1ms

### 리컴포지션
- **정상**: 1-3회 (초기 렌더 + 데이터 로드)
- **주의**: 5회 (경고 로그 출력)
- **문제**: 10회 이상 (에러 로그 출력)

## ⚠️ 문제 신호

### 성능 저하
```
❌ 초기 로딩 > 500ms
❌ UDP 업데이트 > 200ms
❌ 메모리 증가 > 5MB
❌ 아이템당 > 2ms
```

### 과도한 리컴포지션
```
⚠️ 같은 아이템이 5회 이상 리컴포지션
🔴 같은 아이템이 10회 이상 리컴포지션
→ RankingItem의 equals 로직 검토 필요
→ remember 키 확인 필요
```

## 🎯 측정 시나리오

### 시나리오 1: 초기 로딩 성능
```
1. 앱 재시작
2. 랭킹 탭 클릭
3. Logcat에서 "QueryDB_Initial" 검색
4. Duration 및 Memory Delta 확인
```

### 시나리오 2: UDP 업데이트 성능
```
1. 랭킹 화면 진입 대기
2. UDP 이벤트 발생 대기 (자동 또는 수동 트리거)
3. Logcat에서 "UDP_Update" 검색
4. Duration 확인 (100ms 이하가 목표)
```

### 시나리오 3: 리컴포지션 검증
```
1. 랭킹 화면 진입
2. Logcat에서 "Recomposition" 검색
3. 경고/에러 로그 확인
4. 문제 아이템 식별
```

### 시나리오 4: 스크롤 성능
```
1. 랭킹 화면에서 빠르게 스크롤
2. Logcat에서 "Recomposition" 검색
3. 스크롤 중 과도한 리컴포지션 발생 여부 확인
```

## 🔧 Android Studio Profiler와 함께 사용

### 권장 조합:
1. **Logcat 필터**: `tag:Performance|Recomposition`
2. **CPU Profiler**: Record 시작
3. **Memory Profiler**: 실시간 모니터링
4. 측정 시나리오 실행
5. 결과 비교:
   - Logcat: 정확한 시간/메모리 수치
   - Profiler: 시각화 및 함수별 분석

## 📝 성능 측정 체크리스트

### 초기 로딩
- [ ] Duration < 200ms
- [ ] Memory Delta < 1MB
- [ ] DB Query < 50ms
- [ ] Sorting < 30ms
- [ ] Mapping < 100ms

### UDP 업데이트
- [ ] Duration < 100ms
- [ ] Memory Delta < 200KB
- [ ] 리컴포지션: 변경된 아이템만

### 리컴포지션
- [ ] 초기 렌더: 1-2회
- [ ] 데이터 업데이트: 변경된 아이템만 1회
- [ ] 경고 로그 없음
- [ ] 에러 로그 없음

## 🚀 최적화 팁

### Duration이 너무 긴 경우:
1. Checkpoint 로그로 병목 구간 식별
2. DB Query가 느림 → 인덱스 확인
3. Sorting이 느림 → 데이터 크기 확인
4. Mapping이 느림 → 변환 로직 간소화

### 메모리가 많이 증가하는 경우:
1. RankingItem 크기 확인
2. 이미지 URL 리스트 최적화
3. 불필요한 데이터 제거

### 리컴포지션이 많은 경우:
1. RankingItem.equals() 검토
2. remember 키 확인
3. 불필요한 상태 업데이트 제거

## 📊 성능 리포트 템플릿

```
## 성능 측정 결과

### 테스트 환경
- 디바이스: [Galaxy S23 / Pixel 7 등]
- 안드로이드 버전: [13 / 14 등]
- 랭킹 아이템 수: [100개]

### 초기 로딩
- Duration: ___ms
- Memory Delta: ___KB
- DB Query: ___ms
- Sorting: ___ms
- Mapping: ___ms

### UDP 업데이트
- Duration: ___ms
- Memory Delta: ___KB
- 업데이트된 아이템: ___개
- 리컴포지션된 아이템: ___개

### 리컴포지션
- 경고 로그: ___개
- 에러 로그: ___개
- 문제 아이템: [ID 리스트]

### 결론
- [ ] 성능 기준 충족
- [ ] 최적화 필요
- [ ] 문제 발견
```

## 🔗 추가 리소스

- Android Studio Profiler: View → Tool Windows → Profiler
- Layout Inspector: View → Tool Windows → Layout Inspector
- Compose Compiler Metrics: `app/build/compose_metrics/`

---

**주의**: 프로덕션 빌드에서는 성능 측정 로그를 제거하거나 BuildConfig.DEBUG로 제한하세요.
