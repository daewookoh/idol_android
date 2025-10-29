plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
}

// ============================================================
// Multi-Flavor Configuration (old 프로젝트와 동일)
// ============================================================

// 앱 ID 상수
val APP_ID_ORIGINAL = "net.ib.mn"
val APP_ID_ONESTORE = "com.exodus.myloveidol.twostore"
val APP_ID_CHINA = "com.exodus.myloveidol.china"
val APP_ID_CELEB = "com.exodus.myloveactor"

android {
    namespace = "net.ib.mn"
    compileSdk = 36

    defaultConfig {
        applicationId = APP_ID_ORIGINAL
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("int", "VERSION_CODE", "${versionCode}")
    }

    // ============================================================
    // Product Flavors (4개 앱: app, onestore, china, celeb)
    // ============================================================

    flavorDimensions += "default"

    productFlavors {
        create("app") {
            dimension = "default"
            applicationId = APP_ID_ORIGINAL

            // BuildConfig Fields
            buildConfigField("boolean", "CELEB", "false")
            buildConfigField("boolean", "ONESTORE", "false")
            buildConfigField("boolean", "CHINA", "false")
            buildConfigField("String", "APP_ID_VALUE", "\"\"")
            buildConfigField("String", "BASE_URL", "\"https://www.myloveidol.com/api/v1/\"")
            buildConfigField("String", "HOST", "\"https://www.myloveidol.com\"")
            buildConfigField("String", "KAKAO_APP_KEY", "\"0dd43f929e357f51e61c2d82a683b29a\"")

            // Manifest Placeholders
            manifestPlaceholders["KAKAO_APP_KEY_FOR_MANIFEST"] = "0dd43f929e357f51e61c2d82a683b29a"
            manifestPlaceholders["FACEBOOK_CLIENT_ID"] = "d8c7bdf0d17c7e774d4f637d29d6db9a"
            manifestPlaceholders["host"] = "www.myloveidol.com"
            manifestPlaceholders["host_wildcard"] = "*.myloveidol.com"
            manifestPlaceholders["scheme"] = "choeaedol"
            manifestPlaceholders["devscheme"] = "devloveidol"
        }

        create("onestore") {
            dimension = "default"
            applicationId = APP_ID_ONESTORE

            // BuildConfig Fields
            buildConfigField("boolean", "CELEB", "false")
            buildConfigField("boolean", "ONESTORE", "true")
            buildConfigField("boolean", "CHINA", "false")
            buildConfigField("String", "APP_ID_VALUE", "\"twostore\"")
            buildConfigField("String", "BASE_URL", "\"https://www.myloveidol.com/api/v1/\"")
            buildConfigField("String", "HOST", "\"https://www.myloveidol.com\"")
            buildConfigField("String", "KAKAO_APP_KEY", "\"8af2706fda8ad5ecc7b1b5c03bb0c457\"")

            // Manifest Placeholders
            manifestPlaceholders["KAKAO_APP_KEY_FOR_MANIFEST"] = "8af2706fda8ad5ecc7b1b5c03bb0c457"
            manifestPlaceholders["FACEBOOK_CLIENT_ID"] = "bb9ff280eeb8a9e3a1d839d276a643fe"
            manifestPlaceholders["host"] = "www.myloveidol.com"
            manifestPlaceholders["host_wildcard"] = "*.myloveidol.com"
            manifestPlaceholders["scheme"] = "choeaedol"
            manifestPlaceholders["devscheme"] = "devloveidol"
        }

        create("china") {
            dimension = "default"
            applicationId = APP_ID_CHINA

            // BuildConfig Fields
            buildConfigField("boolean", "CELEB", "false")
            buildConfigField("boolean", "ONESTORE", "false")
            buildConfigField("boolean", "CHINA", "true")
            buildConfigField("String", "APP_ID_VALUE", "\"china\"")
            buildConfigField("String", "BASE_URL", "\"https://www.myloveidol.com/api/v1/\"")
            buildConfigField("String", "HOST", "\"https://www.myloveidol.com\"")
            buildConfigField("String", "KAKAO_APP_KEY", "\"0dd43f929e357f51e61c2d82a683b29a\"")

            // Manifest Placeholders
            manifestPlaceholders["KAKAO_APP_KEY_FOR_MANIFEST"] = "0dd43f929e357f51e61c2d82a683b29a"
            manifestPlaceholders["FACEBOOK_CLIENT_ID"] = "d8c7bdf0d17c7e774d4f637d29d6db9a"
            manifestPlaceholders["host"] = "www.myloveidol.com"
            manifestPlaceholders["host_wildcard"] = "*.myloveidol.com"
            manifestPlaceholders["scheme"] = "choeaedol"
            manifestPlaceholders["devscheme"] = "devloveidol"
        }

        create("celeb") {
            dimension = "default"
            applicationId = APP_ID_CELEB

            // BuildConfig Fields
            buildConfigField("boolean", "CELEB", "true")
            buildConfigField("boolean", "ONESTORE", "false")
            buildConfigField("boolean", "CHINA", "false")
            buildConfigField("String", "APP_ID_VALUE", "\"4B418BC059C536ECE2DE206C3DC7C4D7\"")
            buildConfigField("String", "BASE_URL", "\"https://www.myloveactor.com/api/v1/\"")
            buildConfigField("String", "HOST", "\"https://www.myloveactor.com\"")
            buildConfigField("String", "KAKAO_APP_KEY", "\"6715432cd074c4d0dd029b3e8995add2\"")

            // Manifest Placeholders
            manifestPlaceholders["KAKAO_APP_KEY_FOR_MANIFEST"] = "6715432cd074c4d0dd029b3e8995add2"
            manifestPlaceholders["FACEBOOK_CLIENT_ID"] = "a59d87d83c736f501cb6d7223010344d"
            manifestPlaceholders["host"] = "www.myloveactor.com"
            manifestPlaceholders["host_wildcard"] = "*.myloveactor.com"
            manifestPlaceholders["scheme"] = "choeaedolceleb"
            manifestPlaceholders["devscheme"] = "myloveactor"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true  // BuildConfig 활성화
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // 리소스 충돌 방지
    androidResources {
        noCompress += "txt"
    }

    // AAPT 옵션 - 리소스 오류를 경고로 변경
    @Suppress("UnstableApiUsage")
    androidComponents {
        onVariants { variant ->
            variant.androidResources.ignoreAssetsPatterns.add("**/.*")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Coil
    implementation(libs.coil.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)

    // Billing
    implementation(libs.billing)

    // Gson
    implementation(libs.gson)
    implementation(libs.retrofit.converter.gson)

    // Material
    implementation(libs.material)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
