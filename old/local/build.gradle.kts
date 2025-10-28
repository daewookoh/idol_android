plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("kotlinx-serialization")
    id("kotlin-android")
    alias(libs.plugins.ksp)
    id("dagger.hilt.android.plugin")
    id("jacoco")
}

android {
    namespace = "net.ib.mn.local"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int

//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        testInstrumentationRunner = "net.ib.mn.local.CustomTestRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true // Jacoco 적용
            enableAndroidTestCoverage = true // Jacoco 적용
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE.md,LICENSE-notice.md}"
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
    implementation(project(":data"))
    implementation(project(":common"))

    implementation(libs.gson)

    implementation(libs.coroutines.core)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    implementation(libs.core.ktx)
    ksp(libs.hilt.android.compiler)

    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.coroutine.test)
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.mockk.core)
    androidTestImplementation(libs.room.test)
    androidTestImplementation(libs.coroutine.test)
    androidTestImplementation(libs.hilt.android.test)
    androidTestImplementation(libs.androidx.datastore)
    kaptAndroidTest(libs.hilt.compiler)
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("connectedDebugAndroidTest")

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }

    val fileFilter = listOf(
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "**/R.class",
        "**/R$*.class",
        "**/*\$DefaultImpls.class",
        "**/*\$Companion.class",
        "**/*\$WhenMappings.class",
        "**/*DefaultConstructorMarker*",
        "**/*\$inlined\$*.*",                   // inline 함수
        "**/*\$result\$*.*",                    // suspend 함수의 결과 처리기
        "**/*\$externalSyntheticLambda*.*",     // 람다 클래스
    )

    val debugClasses = fileTree("${buildDir}/intermediates/classes/debug") {
        exclude(fileFilter)
    }

    val kotlinDebugTree = fileTree("${buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val kotlinAltDebugTree = fileTree("${buildDir}/classes/kotlin/debug") {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(debugClasses, kotlinDebugTree, kotlinAltDebugTree))


    sourceDirectories.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin"
        )
    )

    executionData.setFrom(
        fileTree(buildDir).include(
            "outputs/code_coverage/**/*.ec",
            "jacoco/*.exec"
        )
    )

    doFirst {
        executionData.files.forEach {
            if (!it.exists()) {
                logger.warn("Execution data file not found: ${it.absolutePath}")
            }
        }
    }
}