// Top-level build.gradle.kts
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// === Buildscript Block ===
buildscript {
    repositories {
        google()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.android.maven.gradle.plugin)
        classpath(libs.sonarqube.gradle.plugin)
        classpath(libs.perf.plugin)
        classpath(libs.play.publisher)
        classpath(libs.firebase.appdistribution.gradle)
        classpath(libs.oss.licenses.plugin)
        classpath(libs.hilt.android.gradle.plugin)
    }
}

// === Plugins Block ===
plugins {
    alias(libs.plugins.ksp)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10" apply false
}

extra["minSdkVersion"] = 26
extra["targetSdkVersion"] = 35
extra["compileSdkVersion"] = 35

// === Subprojects Repository Configuration ===
subprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://artifact.bytedance.com/repository/pangle")
    }
}

// === Allprojects Configuration ===
allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://devrepo.kakao.com/nexus/content/groups/public/")
        maven(url = "https://maven.google.com/")
        maven(url = "https://repository.tnkad.net:8443/repository/public/")
        maven(url = "https://jitpack.io")
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.android") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension> {
            jvmToolchain(17)
        }
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(17)
        }
    }
}