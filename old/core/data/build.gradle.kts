plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
    alias(libs.plugins.ksp)
}

fun getBuildNumber(): Int {
    return System.getenv("BITBUCKET_BUILD_NUMBER")?.toIntOrNull() ?: 1
}

android {
    namespace = "net.ib.mn.core.data"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int
        buildConfigField("int", "VERSION_CODE", getBuildNumber().toString())
    }

    flavorDimensions.add("default")

    buildTypes {
        debug {
            consumerProguardFiles("proguard-rules.pro")
        }
        release {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }
    }

    productFlavors {
        create("app") {
            dimension = "default"
            buildConfigField("boolean", "CELEB", "false")
            buildConfigField(
                "boolean",
                "ONESTORE",
                "false"
            )
            buildConfigField("boolean", "CHINA", "false")
        }

        create("onestore") {
            dimension = "default"
            buildConfigField("boolean", "CELEB", "false")
            buildConfigField(
                "boolean",
                "ONESTORE",
                "true"
            )
            buildConfigField("boolean", "CHINA", "false")
        }

        create("china") {
            dimension = "default"
            buildConfigField("boolean", "CELEB", "false")
            buildConfigField(
                "boolean",
                "ONESTORE",
                "false"
            )
            buildConfigField("boolean", "CHINA", "true")
        }

        create("celeb") {
            dimension = "default"
            buildConfigField("boolean", "CELEB", "true")
            buildConfigField(
                "boolean",
                "ONESTORE",
                "false"
            )
            buildConfigField("boolean", "CHINA", "false")
        }

        create("appDev") {
            dimension = "default"
            buildConfigField("boolean", "CELEB", "false")
            buildConfigField(
                "boolean",
                "ONESTORE",
                "false"
            )
            buildConfigField("boolean", "CHINA", "false")
        }

        create("celebDev") {
            dimension = "default"
            buildConfigField("boolean", "CELEB", "true")
            buildConfigField(
                "boolean",
                "ONESTORE",
                "false"
            )
            buildConfigField("boolean", "CHINA", "false")
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
    implementation(project(":core:utils"))
    implementation(project(":core:model"))

    implementation(libs.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    implementation(project(":bridge"))
}