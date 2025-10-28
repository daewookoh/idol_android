plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "net.ib.mn.core.utils"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int
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
}