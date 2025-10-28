plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
}

android {
    namespace = "net.ib.mn.core.domain"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions.add("default")

    buildTypes {
        debug {
        }
        release {
            isMinifyEnabled = false
        }
    }

    productFlavors {
        create("app") {
            dimension = "default"
        }

        create("onestore") {
            dimension = "default"
        }

        create("china") {
            dimension = "default"
        }

        create("celeb") {
            dimension = "default"
        }

        create("appDev") {
            dimension = "default"
        }

        create("celebDev") {
            dimension = "default"
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:model"))

    implementation(libs.inject)
    implementation(libs.coroutines.core)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.kotlinx.serialization.json) // 최신 버전을 사용하세요.
}