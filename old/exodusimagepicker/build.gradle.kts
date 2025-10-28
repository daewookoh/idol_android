plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("org.jlleitschuh.gradle.ktlint-idea") version "11.0.0"
    alias(libs.plugins.ksp)
    kotlin("kapt") // data binding때문에 필요
}

@Suppress("UnstableApiUsage")
android {
    namespace = "feature.common.exodusimagepicker"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
        debug {
            consumerProguardFiles("proguard-consumer_rules.pro")
        }
        create("releaseLocal") {
            consumerProguardFiles("proguard-consumer_rules.pro")
        }
    }

    dataBinding {
        enable = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    namespace = "feature.common.exodusimagepicker"
    flavorDimensions += "default"
    productFlavors {
        create("app") {
            dimension = "default"
        }
        create("china") {
            dimension = "default"
        }
        create("onestore") {
            dimension = "default"
        }
        create("celeb") {
            dimension = "default"
        }
    }
}

dependencies {
    implementation(libs.transcoder.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.regacy.androidx.constraint)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.subsampling.scale.image.view.androidx)

    //fragment activity
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.ktx)

    //애널리틱스 추가.
    implementation(libs.firebase.bom)
    implementation(libs.firebase.analytics.ktx)

    //Rxjava 추가
    implementation(libs.rxjava3.rxjava)
    implementation(libs.rxjava3.rxkotlin)
    implementation(libs.rxjava3.rxandroid)
    implementation(libs.rxbinding)

//    implementation("net.ypresto.androidtranscoder:android-transcoder:0.3.0")

    //Glide
    implementation(libs.glide)
    ksp(libs.compiler)

    //Paging3
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.rxjava3)

    //jetpack navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    //사진 핀치줌용 라이브러리
    implementation(libs.photoview)

    // exoplayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    //힐트 적용
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
    annotationProcessor(libs.androidx.hilt.compiler)

    //gson 적용
    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //room 추가
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
}
