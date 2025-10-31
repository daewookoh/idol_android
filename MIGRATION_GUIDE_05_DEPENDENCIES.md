# 마이그레이션 가이드 05: 의존성 및 라이브러리 매핑

## 📋 목차
1. [의존성 버전 비교](#의존성-버전-비교)
2. [라이브러리 매핑](#라이브러리-매핑)
3. [의존성 추가 가이드](#의존성-추가-가이드)
4. [주의사항](#주의사항)

---

## 의존성 버전 비교

### 빌드 도구 버전

| 항목 | Old 프로젝트 | 현재 프로젝트 |
|------|-------------|--------------|
| AGP (Android Gradle Plugin) | 8.x | 8.12.1 |
| Kotlin | 2.1.10 | 2.0.21 |
| Compose Compiler | 1.5.x | 2.0.21 (built-in) |
| JVM Target | 17 | 11 |
| Compile SDK | 35 | 36 |
| Target SDK | 35 | 36 |
| Min SDK | 26 | 26 |

### 주요 라이브러리 버전

| 라이브러리 | Old 프로젝트 | 현재 프로젝트 | 비고 |
|-----------|-------------|--------------|------|
| Compose BOM | 2024.09.00 | 2024.09.00 | 동일 |
| Hilt | 2.51.1 | 2.51.1 | 동일 |
| Retrofit | 2.11.0 | 2.11.0 | 동일 |
| OkHttp | 4.12.0 | 4.12.0 | 동일 |
| Room | 2.6.1 | 2.6.1 | 동일 |
| Coroutines | 1.9.0 | 1.9.0 | 동일 |
| Coil | 2.7.0 | 2.7.0 | 동일 |
| Navigation Compose | 2.8.5 | 2.8.5 | 동일 |

---

## 라이브러리 매핑

### 1. UI 라이브러리

#### Image Loading

**Old 프로젝트**:
```kotlin
// Glide 사용
implementation(libs.glide.ksp)
ksp(libs.compiler)

// 사용
Glide.with(context).load(url).into(imageView)
```

**현재 프로젝트**:
```kotlin
// Coil Compose 사용
implementation(libs.coil.compose)

// 사용
AsyncImage(
    model = imageUrl,
    contentDescription = "Image"
)
```

**변환 방법**:
- Glide → Coil Compose로 변경
- `Glide.with(context).load(url).into(imageView)` → `AsyncImage(model = url)`

#### Image Picker

**Old 프로젝트**:
```kotlin
// Custom Image Picker 모듈 사용
implementation(project(":exodusimagepicker"))
```

**현재 프로젝트**:
```kotlin
// 동일하게 사용 가능하거나 Jetpack Activity Result API 사용
// 또는 Compose에서 ActivityResultLauncher 사용
```

### 2. 네트워킹 라이브러리

#### Old 프로젝트
```kotlin
// Retrofit + Gson
implementation(libs.retrofit.core)
implementation(libs.converter.gson)
implementation(libs.adapter.rxjava2)  // RxJava Adapter
```

#### 현재 프로젝트
```kotlin
// Retrofit + Kotlinx Serialization (또는 Gson)
implementation(libs.retrofit)
implementation(libs.retrofit.converter.serialization)  // 또는 Gson
// RxJava Adapter 제거 (Coroutines 사용)
```

**변환 방법**:
- RxJava Adapter 제거
- Coroutines + suspend 함수 사용
- Gson 또는 Kotlinx Serialization 선택 가능

### 3. 비동기 처리

#### Old 프로젝트
```kotlin
// RxJava 사용
implementation(libs.rxandroid)
implementation(libs.rxjava)
implementation(libs.rxkotlin)
implementation(libs.rxlifecycle)
implementation(libs.rxbinding)
```

#### 현재 프로젝트
```kotlin
// Coroutines 사용
implementation(libs.kotlinx.coroutines.android)
implementation(libs.kotlinx.coroutines.core)
// RxJava는 Kakao SDK 의존성으로만 필요
```

**변환 방법**:
- RxJava → Coroutines로 변환
- `Observable` → `Flow` 또는 `suspend` 함수
- `Single` → `suspend` 함수
- `Completable` → `suspend` 함수

### 4. 데이터베이스

#### Old 프로젝트
```kotlin
// Room
implementation(libs.androidx.room.ktx)
implementation(libs.androidx.room.runtime)
ksp(libs.androidx.room.compiler)

// DataStore
implementation(libs.androidx.datastore.preferences)
implementation(libs.androidx.datastore.core)
```

#### 현재 프로젝트
```kotlin
// 동일
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
kapt(libs.androidx.room.compiler)  // 또는 ksp

// DataStore 동일
implementation(libs.androidx.datastore.preferences)
implementation(libs.androidx.datastore.core)
```

**변환 방법**:
- Room 코드는 거의 동일하게 사용 가능
- KSP 대신 KAPT 사용 가능 (현재 프로젝트는 KAPT 사용)

### 5. UI 컴포넌트 라이브러리

#### Old 프로젝트
```kotlin
// Material Components
implementation(libs.material)
implementation(libs.androidx.appcompat)
implementation(libs.androidx.recyclerview)
implementation(libs.androidx.cardview)
implementation(libs.androidx.constraintlayout)
```

#### 현재 프로젝트
```kotlin
// Material3 Compose
implementation(libs.androidx.material3)
// View System 라이브러리는 Compose에서 불필요
```

**변환 방법**:
- Material Components → Material3 Compose
- RecyclerView → LazyColumn
- CardView → Card
- ConstraintLayout → ConstraintLayout (Compose)

### 6. 네비게이션

#### Old 프로젝트
```kotlin
// Fragment Navigation (또는 직접 Intent)
// Navigation Component (View System)
```

#### 현재 프로젝트
```kotlin
// Navigation Compose
implementation(libs.androidx.navigation.compose)
implementation(libs.hilt.navigation.compose)
```

**변환 방법**:
- Intent 기반 네비게이션 → Navigation Compose
- Fragment Navigation → Compose Screen Navigation

### 7. SNS 로그인 SDK

#### Old 프로젝트
```kotlin
// Kakao SDK
implementation(libs.v2.user.rx)  // Kakao SDK 2.13.0

// Line SDK
implementation(libs.linesdk)  // Line SDK 5.8.1

// Facebook Login
implementation(libs.facebook.login)  // Facebook Login 17.0.2

// Google Sign-In
implementation(libs.play.services.auth)  // Google Sign-In 20.7.0

// RxJava (Kakao SDK 의존성)
implementation(libs.rxandroid)
implementation(libs.rxjava)
implementation(libs.rxkotlin)
```

#### 현재 프로젝트
```kotlin
// 동일한 버전 사용
implementation(libs.kakao.sdk.user.rx)
implementation(libs.line.sdk)
implementation(libs.facebook.login)
implementation(libs.play.services.auth)

// RxJava (Kakao SDK 의존성으로 필요)
implementation(libs.rxandroid)
implementation(libs.rxjava)
implementation(libs.rxkotlin)
```

**변환 방법**:
- SNS 로그인 SDK는 동일하게 사용 가능
- Compose에서도 Activity Result API로 사용 가능

### 8. 광고 라이브러리

#### Old 프로젝트
```kotlin
// Firebase Ads
implementation(libs.firebase.ads)

// AdMob
implementation(libs.google.adapter)

// 기타 광고 SDK들
implementation(project(":tnk_rwd"))
implementation(project(":admob"))
```

#### 현재 프로젝트
```kotlin
// 동일하게 사용 가능
// Compose에서는 AndroidView로 WebView처럼 래핑
```

**변환 방법**:
- 광고 라이브러리는 Compose에서 `AndroidView`로 래핑하여 사용
- 예: `AndroidView(factory = { AdView(context) })`

### 9. 기타 라이브러리

#### Media Player

**Old 프로젝트**:
```kotlin
// ExoPlayer
implementation(libs.androidx.media3.exoplayer)
implementation(libs.androidx.media3.ui)
```

**현재 프로젝트**:
```kotlin
// 동일하게 사용 가능
// Compose에서는 AndroidView로 래핑
```

#### WebView

**Old 프로젝트**:
```kotlin
// 직접 WebView 사용
```

**현재 프로젝트**:
```kotlin
// AndroidView로 래핑 또는 ExoWebView 컴포넌트 사용
```

---

## 의존성 추가 가이드

### 1. libs.versions.toml에 추가

```toml
[versions]
newLibrary = "1.0.0"

[libraries]
new-library = { group = "com.example", name = "library", version.ref = "newLibrary" }

[plugins]
new-plugin = { id = "com.example.plugin", version.ref = "newLibrary" }
```

### 2. build.gradle.kts에 추가

```kotlin
dependencies {
    implementation(libs.new.library)
}
```

### 3. 버전 관리

- 모든 버전은 `libs.versions.toml`에서 중앙 관리
- 직접 버전 번호 하드코딩 금지

---

## 주의사항

### 1. RxJava 의존성

**문제**:
- Kakao SDK가 RxJava를 의존성으로 요구
- 하지만 실제 앱 코드에서는 Coroutines 사용

**해결**:
- RxJava는 Kakao SDK 의존성으로만 포함
- 실제 코드는 Coroutines 사용

### 2. Compose와 View System 혼용

**문제**:
- 일부 라이브러리는 아직 Compose를 지원하지 않음
- WebView, AdView 등

**해결**:
- `AndroidView`로 래핑하여 사용
- 예: `AndroidView(factory = { WebView(context) })`

### 3. KAPT vs KSP

**Old 프로젝트**:
- KSP 사용

**현재 프로젝트**:
- KAPT 사용 (Hilt, Room)

**변환 시**:
- 필요시 KSP로 전환 가능
- 하지만 현재는 KAPT 유지 권장

### 4. Flavor별 의존성

**Old 프로젝트**:
```kotlin
appImplementation(files("libs/NASWall_20221122.jar"))
onestoreImplementation(files("libs/NASWall_20221122.jar"))
chinaImplementation(files("libs/NASWall_20221122.jar"))
celebImplementation(files("libs/NASWall_20211112.jar"))
```

**현재 프로젝트**:
- 동일하게 flavor별 의존성 분리 가능

---

## 의존성 변환 체크리스트

### UI 라이브러리
- [ ] Glide → Coil Compose 변환
- [ ] Material Components → Material3 Compose
- [ ] RecyclerView → LazyColumn
- [ ] View System 라이브러리 제거

### 네트워킹
- [ ] RxJava Adapter 제거
- [ ] Coroutines 사용
- [ ] Serialization 라이브러리 선택 (Gson 또는 Kotlinx)

### 비동기 처리
- [ ] RxJava → Coroutines 변환
- [ ] Observable → Flow 변환
- [ ] Single/Completable → suspend 함수 변환

### 네비게이션
- [ ] Intent 기반 → Navigation Compose
- [ ] Fragment Navigation → Compose Screen Navigation

### 기타
- [ ] WebView → AndroidView 래핑
- [ ] AdView → AndroidView 래핑
- [ ] ExoPlayer → AndroidView 래핑

---

## 다음 문서
- [마이그레이션 가이드 06: 네비게이션 변환](./MIGRATION_GUIDE_06_NAVIGATION.md)
- [마이그레이션 가이드 07: 테스트 및 검증](./MIGRATION_GUIDE_07_TESTING.md)

