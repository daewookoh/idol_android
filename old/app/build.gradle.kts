import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.FileWriter

plugins {
    id("com.android.application")
    id("com.google.firebase.firebase-perf")
    id("org.sonarqube")
    id("kotlin-android")
    alias(libs.plugins.ksp)
    id("com.google.firebase.appdistribution")
    id("com.google.android.gms.oss-licenses-plugin")
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("org.jlleitschuh.gradle.ktlint-idea") version "11.0.0"
    id("kotlin-parcelize")
    id("kotlinx-serialization")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.kotlin.compose)
}

//apply(plugin = "com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
val projectDir = project.projectDir.absolutePath
val now: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
val buildNumber = System.getenv("BITBUCKET_BUILD_NUMBER") ?: "Unknown"

// mezzo 매체 아이디 검사
val MEZZO_SECTION_IDOL = "300884"
val MEZZO_SECTION_ACTOR = "801178"

val APP_ID_ORIGINAL = "net.ib.mn"
// 파생앱 applicationId =
val APP_ID_ONESTORE = "com.exodus.myloveidol.twostore"
val APP_ID_CHINA = "com.exodus.myloveidol.china"
val APP_ID_CELEB = "com.exodus.myloveactor"

fun getArchiveName(applicationId: String): String {
    var archiveName = "idol_original"
    if( applicationId == APP_ID_ORIGINAL ) {
        archiveName = "idol_original"
    }
    if( applicationId == APP_ID_ONESTORE ) {
        archiveName = "idol_onestore"
    }
    if( applicationId == APP_ID_CHINA ) {
        archiveName = "idol_china"
    }
    if( applicationId == APP_ID_CELEB ) {
        archiveName = "celeb"
    }

    println("archiveName: ${archiveName}")

    return archiveName
}

fun getAppdistributionId(applicationId: String): String {
    var appDistId = "1:444896554540:android:12a0be743c254073"
    if( applicationId == APP_ID_ORIGINAL ) {
        appDistId = "1:444896554540:android:12a0be743c254073"
    }
    if( applicationId == APP_ID_ONESTORE ) {
        appDistId = "1:444896554540:android:8c6d5bc2b11bbdb3"
    }
    if( applicationId == APP_ID_CHINA ) {
        appDistId = "1:444896554540:android:69512595327f39b7e66b89"
    }
    if( applicationId == APP_ID_CELEB ) {
        appDistId = "1:445540446080:android:f1790ef919f8e7bc"
    }
    return appDistId
}

fun getAppNameForSlack(applicationId: String): String {
    var appName = ":aedol: 하얀하트♡ 앱번들이"
    if( applicationId == APP_ID_ORIGINAL ) {
        appName = ":aedol: 하얀하트♡ 앱번들이"
    }
    if( applicationId == APP_ID_ONESTORE ) {
        appName = "1️⃣원스토어 APK가"
    }
    if( applicationId == APP_ID_CHINA ) {
        appName = "🇨🇳중국앱 APK가"
    }
    if( applicationId == APP_ID_CELEB ) {
        appName = ":celeb: 셀럽 앱번들이"
    }

    return appName
}

allprojects {
    repositories {
        maven("https://maven.google.com")
        maven("https://sdk.tapjoy.com/")
        maven(
            "https://imobile-maio.github.io/maven"
        )
        maven(
            "https://fan-adn.github.io/nendSDK-Android-lib/library"
        )
        mavenCentral()
        maven("https://android-sdk.is.com/")
        maven("https://artifact.bytedance.com/repository/pangle")
    }
}

fun getBuildNumber(): Int {
    try{
        return System.getenv("BITBUCKET_BUILD_NUMBER")?.toIntOrNull() ?: 1
    }catch(e: Exception){
        return 1
    }
}

fun getRevisionCount(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "--first-parent", "HEAD")
            .redirectErrorStream(true)
            .start()

        val output = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
        output.toIntOrNull() ?: throw RuntimeException("Failed to parse git revision count")
    } catch (e: Exception) {
        println("Error fetching revision count: ${e.message}")
        0
    }
}

fun getXmlValue(xmlFile: File, elementTag: String, attributeName: String, targetValue: String): String? {
    if (!xmlFile.exists()) {
        println("🚨 XML 파일을 찾을 수 없음: ${xmlFile.absolutePath}")
        return null
    }
    return try {
        val document: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        document.documentElement.normalize()

        val nodeList = document.getElementsByTagName(elementTag)
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            val attributes = node.attributes
            val nameAttr = attributes.getNamedItem(attributeName)
            if (nameAttr != null && nameAttr.nodeValue == targetValue) {
                return node.textContent.trim()
            }
        }
        null
    } catch (e: Exception) {
        println("Error parsing XML: ${e.message}")
        null
    }
}

fun getVersionName(): String {
    try {
        val stringsFile = file("$projectDir/src/main/res/values/version.xml")
        return getXmlValue(stringsFile, "string", "name", "app_version") ?: "1.0.0"
    }catch(e: Exception){
        println(e)
        return "1.0.0"
    }
}

fun getAppName(applicationId: String): String {
    try {
        val stringsFile = file("$projectDir/../string/src/main/res/values-ko/strings.xml")
        if( applicationId == APP_ID_ONESTORE ) {
            return getXmlValue(stringsFile, "string", "name", "app_name_onestore_upper") ?: "최애돌"
        } else if( applicationId == APP_ID_CELEB ) {
            return getXmlValue(stringsFile, "string", "name", "actor_app_name_upper") ?: "최애돌셀럽"
        } else {
            return getXmlValue(stringsFile, "string", "name", "app_name_upper") ?: "최애돌"
        }
    }catch(e: Exception){
        println(e)
        return "최애돌"
    }
}

fun removeEmojis(input: String): String {
    // 문자열에서 이모티콘 패턴 찾아 제거
    val result = input.replace(Regex("[\\p{So}\u200d]"), "")
    return result
}

@Suppress("UnstableApiUsage")
android {
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    val currentBuildNumber = getBuildNumber()
    val versionString = getVersionName()
    defaultConfig {
        applicationId = "net.ib.mn"
        minSdk = rootProject.extra["minSdkVersion"] as Int
        targetSdk = rootProject.extra["targetSdkVersion"] as Int
        versionCode = currentBuildNumber
        versionName = versionString
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        buildTypes {
            debug {
            }

            release {
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    namespace = "net.ib.mn"
    flavorDimensions += "default"

    signingConfigs {
        register("release") {
            storeFile = file("$projectDir/../mntalk.keystore")
            storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
        register("chinaRelease") {
            storeFile = file("$projectDir/../china.jks")
            storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
            keyAlias = "key0"
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
        register("celebRelease") {
            storeFile = file("$projectDir/../celeb.keystore")
            storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD_CELEB")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS_CELEB")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD_CELEB")
        }
    }

    productFlavors {
        create("app") {
            dimension = "default"
            signingConfig = signingConfigs.getByName("release")
            applicationId = APP_ID_ORIGINAL
            firebaseAppDistribution {
                appId = "1:444896554540:android:12a0be743c254073"
                artifactType = "AAB"
                serviceCredentialsFile = File("$projectDir/../firebase_app_distribution.json").absolutePath
            }
            buildConfigField("boolean", "CELEB", "false")
            buildConfigField("boolean", "ONESTORE", "false")
            buildConfigField("boolean", "CHINA", "false")
            buildConfigField("String", "KAKAO_APP_KEY", "\"0dd43f929e357f51e61c2d82a683b29a\"")
            manifestPlaceholders["KAKAO_APP_KEY_FOR_MANIFEST"] = "0dd43f929e357f51e61c2d82a683b29a"
            manifestPlaceholders["FACEBOOK_CLIENT_ID"] = "d8c7bdf0d17c7e774d4f637d29d6db9a"
            manifestPlaceholders["host"] = "www.myloveidol.com"
            manifestPlaceholders["host_wildcard"] = "*.myloveidol.com"
            manifestPlaceholders["scheme"] = "choeaedol"
            manifestPlaceholders["devscheme"] = "devloveidol"

            externalNativeBuild {
                ndkBuild {
                    arguments("PRODUCT_FLAVOR=app")
                }
            }

            System.setProperty("GRADLE_DAEMON", "true")
        }
        create("onestore") {
            dimension = "default"
            signingConfig = signingConfigs.getByName("release")
            applicationId = APP_ID_ONESTORE
            firebaseAppDistribution {
                appId = "1:444896554540:android:8c6d5bc2b11bbdb3"
                artifactType = "APK"
                serviceCredentialsFile = File("$projectDir/../firebase_app_distribution.json").absolutePath
            }

            buildConfigField("boolean", "CELEB", "false")
            buildConfigField("boolean", "ONESTORE", "true")
            buildConfigField("boolean", "CHINA", "false")
            buildConfigField("String", "KAKAO_APP_KEY", "\"8af2706fda8ad5ecc7b1b5c03bb0c457\"")
            manifestPlaceholders["KAKAO_APP_KEY_FOR_MANIFEST"] = "8af2706fda8ad5ecc7b1b5c03bb0c457"
            manifestPlaceholders["FACEBOOK_CLIENT_ID"] = "bb9ff280eeb8a9e3a1d839d276a643fe"
            manifestPlaceholders["host"] = "www.myloveidol.com"
            manifestPlaceholders["host_wildcard"] = "*.myloveidol.com"
            manifestPlaceholders["scheme"] = "choeaedol"
            manifestPlaceholders["devscheme"] = "devloveidol"

            externalNativeBuild {
                ndkBuild {
                    arguments("PRODUCT_FLAVOR=onestore")
                }
            }

            proguardFile("proguard-rules-onestore.pro")
            System.setProperty("GRADLE_DAEMON", "false")
        }
        create("china") {
            dimension = "default"
            signingConfig = signingConfigs.getByName("chinaRelease")
            applicationId = APP_ID_CHINA
            firebaseAppDistribution {
                appId = "1:444896554540:android:69512595327f39b7e66b89"
                artifactType = "APK"
                serviceCredentialsFile = File("$projectDir/../firebase_app_distribution.json").absolutePath
            }
            buildConfigField("boolean", "CELEB", "false")
            buildConfigField("boolean", "ONESTORE", "false")
            buildConfigField("boolean", "CHINA", "true")
            buildConfigField("String", "KAKAO_APP_KEY", "\"0dd43f929e357f51e61c2d82a683b29a\"")
            manifestPlaceholders["KAKAO_APP_KEY_FOR_MANIFEST"] = "0dd43f929e357f51e61c2d82a683b29a"
            manifestPlaceholders["FACEBOOK_CLIENT_ID"] = "d8c7bdf0d17c7e774d4f637d29d6db9a"
            manifestPlaceholders["host"] = "www.myloveidol.com"
            manifestPlaceholders["host_wildcard"] = "*.myloveidol.com"
            manifestPlaceholders["scheme"] = "choeaedol"
            manifestPlaceholders["devscheme"] = "devloveidol"

            externalNativeBuild {
                ndkBuild {
                    arguments("PRODUCT_FLAVOR=china")
                }
            }
            System.setProperty("GRADLE_DAEMON", "true")
        }
        create("celeb") {
            dimension = "default"
            signingConfig = signingConfigs.getByName("celebRelease")
            applicationId = APP_ID_CELEB
            firebaseAppDistribution {
                appId = "1:445540446080:android:f1790ef919f8e7bc"
                artifactType = "AAB"
                serviceCredentialsFile = File("$projectDir/../firebase_app_distribution_celeb.json").absolutePath
            }
            buildConfigField("boolean", "CELEB", "true")
            buildConfigField("boolean", "ONESTORE", "false")
            buildConfigField("boolean", "CHINA", "false")
            buildConfigField("String", "KAKAO_APP_KEY", "\"6715432cd074c4d0dd029b3e8995add2\"")
            manifestPlaceholders["KAKAO_APP_KEY_FOR_MANIFEST"] = "6715432cd074c4d0dd029b3e8995add2"
            manifestPlaceholders["FACEBOOK_CLIENT_ID"] = "a59d87d83c736f501cb6d7223010344d"
            manifestPlaceholders["host"] = "www.myloveactor.com"
            manifestPlaceholders["host_wildcard"] = "*.myloveactor.com"
            manifestPlaceholders["scheme"] = "choeaedolceleb"
            manifestPlaceholders["devscheme"] = "myloveactor"

            externalNativeBuild {
                ndkBuild {
                    arguments("PRODUCT_FLAVOR=celeb")
                }
            }
            System.setProperty("GRADLE_DAEMON", "true")
        }
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
//    flavorDimensions = mutableListOf("default")

    buildTypes {
        debug {
            try {
                isMinifyEnabled = false
                setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"))
                extra["enableCrashlytics"] = false
                buildConfigField("int", "VERSION_CODE", getBuildNumber().toString())
            }catch(e: Exception){
                println("Exception debug: ${e}")
            }
        }
        release {
            try{
                isMinifyEnabled = true
                setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"))

                lint.disable += "MissingTranslation"
                lint.checkReleaseBuilds = false
                // Or, if you prefer, you can continue to check(for errors in release builds,)
                // but continue the build even when errors are found:
                lint.abortOnError = false

                firebaseAppDistribution {
                    groups = "qatesters"
                    serviceCredentialsFile = File("$projectDir/../firebase_app_distribution.json").absolutePath
                    releaseNotesFile="$projectDir/../release-notes.txt"
                }

                buildConfigField("int", "VERSION_CODE", getBuildNumber().toString())
            }catch(e: Exception){
                println("Exception buildTypes.release: $e")
            }
        }
    }
    packagingOptions {
        resources.excludes.add("META-INF/maven/com.google.guava/guava/pom.properties")
        resources.excludes.add("META-INF/maven/com.google.guava/guava/pom.xml")
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/NOTICE")
        resources.excludes.add("META-INF/LICENSE")
        resources.excludes.add("META-INF/LICENSE.txt")
        resources.excludes.add("META-INF/NOTICE.txt")
    }

    repositories {
        google()
        mavenCentral()
        flatDir {
            dirs("libs")
        }
        maven("https://jitpack.io")
    }

    bundle{
        language{
            enableSplit = false
        }
        density {
            // This property is set to true by default.
            enableSplit = false
        }
        abi {
            // This property is set to true by default.
            enableSplit = false
        }

    }

    val mezzoFile = File("$projectDir/src/main/res/values/mezzo.xml")
    val mezzoSection = getXmlValue(mezzoFile, "integer", "name", "mezzo_section") ?: throw GradleException("mezzo_section not found in mezzo.xml")
    val applicationId = android.defaultConfig.applicationId

    when {
        applicationId == APP_ID_ORIGINAL && mezzoSection != MEZZO_SECTION_IDOL -> {
            throw GradleException("mezzo_section should be $MEZZO_SECTION_IDOL")
        }
        applicationId == APP_ID_CELEB && mezzoSection != MEZZO_SECTION_ACTOR -> {
            throw GradleException("mezzo_section should be $MEZZO_SECTION_ACTOR")
        }
    }

    dataBinding {
        enable = true
    }

    externalNativeBuild {
        ndkBuild {
            path("src/onestore/jni/Android.mk")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    // Compose 컴파일러 버전 설정
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

firebaseAppDistribution {
    val flavorName = gradle.startParameter.taskRequests
        .flatMap { it.args }
        .firstOrNull { it.contains("app") || it.contains("china") || it.contains("onestore") || it.contains("celeb") }
        ?.replace("appDistributionUpload", "")
        ?.replace("Release", "")
        ?.replace("Debug", "")
        ?.toLowerCase()

    val credentialsFile = when (flavorName) {
        "celeb" -> "$projectDir/../firebase_app_distribution_celeb.json"
        else -> "$projectDir/../firebase_app_distribution.json"
    }
    val firebaseAppId = when(flavorName) {
        "app" -> "1:444896554540:android:12a0be743c254073"
        "onestore" -> "1:444896554540:android:8c6d5bc2b11bbdb3"
        "china" -> "1:444896554540:android:69512595327f39b7e66b89"
        "celeb" -> "1:445540446080:android:f1790ef919f8e7bc"
        else -> "1:444896554540:android:12a0be743c254073"
    }

    appId = firebaseAppId
    groups = "qatesters"
    serviceCredentialsFile = File(credentialsFile).absolutePath
    releaseNotesFile="$projectDir/../release-notes.txt"
}

tasks.register("setArchiveName") {
    val applicationId = project.findProperty("applicationId") as String?
    if (applicationId != null) {
        val archiveName = getArchiveName(applicationId)
        project.setProperty("archivesBaseName", "${archiveName}_${getDate()}_${getVersionName()}_${getBuildNumber()}")
        println("archiveName: $archiveName")
        println("firebase: $firebaseAppDistribution")
    }
}

//git commit message가져오는 테스크이다.
tasks.register("getCommitMessage") {
    var appDistributionId = ""
    var appName = ""
    if( project.hasProperty("applicationId")) {
        val applicationId = project.findProperty("applicationId") as String? ?: ""
        appDistributionId = getAppdistributionId(applicationId)
        appName = getAppNameForSlack(applicationId)
    }

    var prevHash: String = ""
    var commitHash: String = ""

    doFirst({
        //repository
        val repository: String = System.getenv("BITBUCKET_REPO_FULL_NAME")

        //유저 auth값 가져오기 username:password값.
        val authValue: String = System.getenv("BB_AUTH_STRING")

        //현재 커밋 해시를 가져옵니다.
        val commitFullHash: String = System.getenv("BITBUCKET_COMMIT")

        //이전 빌드한 가장 마지막 커밋 해시를 가져옵니다.
        val previousBuildNumber: Int = System.getenv("BITBUCKET_BUILD_NUMBER").toInt() - 1
        val preCommitHashURL: String = "https://api.bitbucket.org/2.0/repositories/$repository/pipelines/$previousBuildNumber"
        val procFullHashCommand = listOf("sh", "-c", "curl -s -X GET --user '$authValue' '$preCommitHashURL' | jq '.target.commit.hash'")
        val procFullHashExe = ProcessBuilder(procFullHashCommand)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()

        val procFullHash = procFullHashExe.inputStream.bufferedReader().use { it.readText() }.replace("\"", "").trim()

        // procFullHash가 7자리보다 짧으면 전체 문자열을 사용하고, 7자리 이상이면 앞의 7자리만 가져옵니다.
        prevHash = if(procFullHash.length >= 7) procFullHash.substring(0, 7) else procFullHash
        commitHash = if (commitFullHash.length >= 7) commitFullHash.substring(0, 7) else commitFullHash
    })

    doLast({
        println("Generating release notes (release-notes.txt)")
        val releaseNotes = File("$projectDir/../release-notes.txt")
        releaseNotes.delete()
        println("*** ${appName}")

        //가장 마지막 태그부터 현재까지 commit mesage를 가져옵니다.
        val cmdLine = listOf("sh" , "-c", "git log --pretty=\"%s\"" + " '"+ prevHash + ".." + commitHash+ "'" + " | sort | awk '/IDOL-/ {issues[$1] = issues[$1] ? issues[$1] \" / \" $0 : $0; } END { for(i in issues) print issues[i]}'")
        val procCommit = ProcessBuilder(cmdLine)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()

        val sb: StringBuilder = StringBuilder()
        sb.append("\n\n✨ <https://appdistribution.firebase.google.com/testerapps/${appDistributionId} | *${android.defaultConfig.versionName} (${getBuildNumber()})*> 빌드 성공! ${appName} 만들어졌습니다." + "\n\n")
        sb.append("🗓Build Date  = $now\n\n")
        sb.append("🔔Build Number  = $buildNumber\n\n")
        sb.append("💡Version  = ${android.defaultConfig.versionName}\n\n")

        //남은 자파 파일 개수 카운트.
        val cmdLeftJava = listOf("sh", "-c", "find \"$projectDir/src\" -name \"*.java\" | wc -l | tr -d \" \"")
        val procLeftJava = ProcessBuilder(cmdLeftJava)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        val countLeftjava = procLeftJava.inputStream.bufferedReader().use { it.readText().trim() }

        //남은 코틀린 파일 개수 카운트.
        val cmdLeftKotlin = listOf("sh", "-c", "find \"$projectDir/src\" -name \"*.kt\" | wc -l | tr -d \" \"")
        val procLeftKotlin = ProcessBuilder(cmdLeftKotlin)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        val countLeftKotlin = procLeftKotlin.inputStream.bufferedReader().use { it.readText().trim() }

        val migraionPercentage: Double = Math.floor((countLeftjava.toDouble() * 1000 / (countLeftjava.toDouble() + countLeftKotlin.toDouble())) / 10)

        sb.append("남은 자바 파일 : $countLeftjava\n")
        sb.append("코틀린 파일 : $countLeftKotlin\n")
        sb.append("전체 자바 파일 비율 : $migraionPercentage% 입니다. \n\n")

        sb.append("🛠Changes\n")

        procCommit.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                //커밋 메시지 공백제거.
                val newLine: String = line.trim()
                val splitedIndex: Int = newLine.indexOf(" ")
                val issue: String = newLine.substring(0, splitedIndex)
                val message: String = newLine.substring(splitedIndex)
                sb.append("<https://exodusent.atlassian.net/browse/$issue| *$issue*> $message" + "\n")
            }
        }

        releaseNotes.bufferedWriter().use { writer ->
            writer.write(sb.toString())
        }

        releaseNotes.appendText("\n\n\n")
    });
}

tasks.register("getJiraJsonFile") {
    val applicationId = project.findProperty("applicationId") as? String ?: ""
    val appDistributionId = getAppdistributionId(applicationId)

    var prevHash = ""
    var commitHash = ""

    doFirst({
        //repository
        val repository: String = System.getenv("BITBUCKET_REPO_FULL_NAME")

        //유저 auth값 가져오기 username:password값.
        val authValue: String = System.getenv("BB_AUTH_STRING")

        //현재 커밋 해시를 가져옵니다.
        val commitFullHash: String = System.getenv("BITBUCKET_COMMIT").trim()

        // 이전 빌드의 가장 마지막 커밋 해시를 가져옵니다.
        val previousBuildNumber: Int = System.getenv("BITBUCKET_BUILD_NUMBER").toInt() - 1
        val preCommitHashURL: String = "https://api.bitbucket.org/2.0/repositories/$repository/pipelines/$previousBuildNumber"
        val procFullHashCommand = listOf("sh", "-c", "curl -s -X GET --user '$authValue' '$preCommitHashURL' | jq '.target.commit.hash'")
        val procFullHashExe = ProcessBuilder(procFullHashCommand)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()

        val procFullHash: String = procFullHashExe.inputStream.bufferedReader().use {
            it.readText().replace("\"", "").trim()
        }

        // prevHash와 commitHash에서 길이가 7자리 이상인지 확인하여 안전하게 값을 추출합니다.
        prevHash = if(procFullHash.length >= 7) procFullHash.substring(0, 7) else procFullHash
        commitHash = if(commitFullHash.length >= 7) commitFullHash.substring(0, 7) else commitFullHash
    })

    doLast({
        val cmdLine = listOf("sh" , "-c",
            "git log --pretty=\"%s\" '$prevHash..$commitHash' | sort | awk \'/IDOL-/ {issues[$1) = issueslistOf($1) ? issueslistOf($1) \" / \" $0 : $0; } END { for(i in issues) print issueslistOf(i)}\'"
        )
        val procCommit = ProcessBuilder(cmdLine)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()

        var writer: FileWriter? = null
        val jsonObject = JsonObject()
        val jsonObject2 = JsonObject()
        jsonObject2.addProperty("app_name", getAppName(applicationId))
        jsonObject2.addProperty("version", android.defaultConfig.versionName)
        jsonObject2.addProperty("build_number", getBuildNumber())
        //testerapps/앱아이디.
        jsonObject2.addProperty("install_url", "https://appdistribution.firebase.google.com/testerapps/${appDistributionId}")
        val issueArray: JsonArray = JsonArray()

        procCommit.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val newLine: String = removeEmojis(line.trim()).trim()
                val splitedIndex: Int = newLine.indexOf(" ")
                val issue: String = newLine.substring(0, splitedIndex)
                issueArray.add(issue)
            }
        }


        jsonObject.add("data", jsonObject2)
        jsonObject.add("issues", issueArray)

        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val json: String = gson.toJson(jsonObject)

        try{
            writer = FileWriter("$projectDir/../jira_issue.json");
            writer.write(json)
        } catch(e: Exception){
            e.printStackTrace()
        } finally {
            try {
                writer?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    })
}

tasks.register<Exec>("unzipAppDistributionFiles") {
    val password = System.getenv("APPDIST_ZIP_PASSWORD") ?: ""
    executable = "sh"
    args("-c", "unzip -P $password $projectDir/../firebase_app_distribution.zip -d ..")
}

fun getDate(): String {
    val formattedDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    return formattedDate
}

val appImplementation by configurations
val onestoreImplementation by configurations
val chinaImplementation by configurations
val celebImplementation by configurations

@Suppress("UnstableApiUsage")
dependencies {
    //TNK
    implementation(project(":tnk_rwd"))
    implementation(project(":exodusimagepicker"))
    implementation(project(":admob"))
    implementation(project(":core:data"))
    implementation(project(":core:designSystem"))
    implementation(project(":core:utils"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))

    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":local"))
    implementation(project(":data"))
    implementation(project(":data-resource"))
    implementation(project(":component"))

    implementation(libs.support.annotations)
    implementation(libs.androidx.legacy.support.v4)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.rules)

    appImplementation(files("libs/NASWall_20221122.jar"))
    onestoreImplementation(files("libs/NASWall_20221122.jar"))
    chinaImplementation(files("libs/NASWall_20221122.jar"))
    // 배터리소모 이슈로 이전버전으로 돌려둠 (애돌이는 괜찮다고 함)
    celebImplementation(files("libs/NASWall_20211112.jar"))

    implementation(files("libs/adMan.jar"))
    implementation(files("libs/jericho-android.3.3.jar"))
//    implementation(files("libs/tapjoyconnectlibrary.jar"))
    implementation(libs.androidx.recyclerview)
//    implementation("androidx.appcompat:appcompat-resources:$appcompat_version")
    implementation(libs.androidx.annotation)
    implementation(libs.material)
    implementation(libs.gms.play.services.base)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.plus)
    implementation(libs.play.services.analytics)
    implementation(platform(libs.firebase.bom))  //firebase BOM 추가(라이브러리 버전 직접 관리)
    implementation(libs.firebase.analytics)
//    implementation("com.google.firebase:firebase-ads:20.3.0")
    implementation(libs.firebase.ads)    //WidePhotoFragment 에서 사용하는 mediaView nullable 가능하게 변경됨
//    implementation("com.google.firebase:firebase-core:19.0.1")  //이 SDK에는 Google 애널리틱스용 Firebase SDK가 포함되어 있습니다.
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.places)
    implementation(libs.androidx.cardview)
    implementation(libs.regacy.androidx.constraint)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference)

    implementation(libs.billing)

    // Crashlytics
    implementation(libs.google.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    //DynamicLink
    implementation(libs.firebase.dynamic.links)
    implementation(libs.firebase.dynamic.links.ktx)

    implementation(libs.firebase.inappmessaging)    //인앱 메시지
    implementation(libs.firebase.inappmessaging.ktx)
    implementation(libs.firebase.inappmessaging.display)    //인앱 메시지 표시
    implementation(libs.firebase.inappmessaging.display.ktx)

    // stetho
    implementation(libs.stetho)
    implementation(libs.stetho.okhttp3)
    implementation(libs.stetho.js.rhino)

    // retrofit
    implementation(libs.retrofit.core)
    implementation(libs.converter.gson)
    implementation(libs.adapter.rxjava2)
    implementation(libs.logging.interceptor.v492)
    implementation(libs.okio.parent)


    implementation(libs.androidx.core.ktx) // 1.12는 target sdk 34이상이어야 함
//    implementation(name = "VungleAdapter", "ext" = "aar")
//
//    // Vungle Adapter for AdMob
//    implementation(files("libs/dagger-2.7.jar"))
////    implementation(files("libs/javax.inject-1.jar"))
//    implementation(files("libs/vungle-publisher-adaptive-id-4.0.3.jar"))

    implementation(libs.vungle) // sdk 버전에 맞춰야함

    // premiumads
    implementation(libs.premiumads.admob)

    implementation(libs.v2.user.rx) // 카카오 로그인

    implementation(libs.okhttp)
    implementation(libs.androidx.multidex)
    implementation(libs.linesdk)

    implementation(libs.gson)

    //    implementation(files("libs/aplus_sdk2.0.jar"))

    //    implementation(files("libs/k1-floating_v1.0.1.jar"))

    implementation(libs.photoview)

    implementation(libs.play.services.ads.identifier)

    // unity ads mediation
    implementation(libs.unity.ads)
    implementation(libs.unity)

    // VM detector
    implementation(libs.rootbeer.lib)
    implementation(libs.android.emulator.detector)

    // custom videoview
    implementation(libs.texturevideoview)

    // exoplayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)

    implementation(libs.kotlin.stdlib)

    // facebook login
    implementation(libs.facebook.login)
    // facebook ad
    implementation(libs.facebook)

    // new image cropper
//    implementation("com.theartofdev.edmodo:android-image-cropper:2.8.0")
    //implementation("com.vanniktech:android-image-cropper:4.6.0")

    // superrewards
    //    implementation(files("libs/SuperRewards-3.1b.jar"))

    implementation(libs.socket.io.client) {
        // excluding org.json which is provided by Android
        exclude(group = "org.json", module = "json")
    }

    implementation(libs.mpandroidchart)

    // glide
    implementation(libs.glide.ksp)
    ksp(libs.compiler)

    // 중국버전은 아래 안씀
    appImplementation(libs.okhttp3.integration)
    onestoreImplementation(libs.okhttp3.integration)
    celebImplementation(libs.okhttp3.integration)

//    implementation("jp.wasabeef:glide-transformations:4.3.0")
    implementation(libs.androidsvg)

    //    implementation(files("libs/offerwallsdk-release.aar"))
    implementation(files("libs/cropper-release.aar"))

    // china ------------------------------------------------------------
    chinaImplementation(libs.acra.mail)
    // wechat
    chinaImplementation(libs.wechat.sdk.android.without.mta)

        // QQ
    chinaImplementation(files("libs/open_sdk_lite.jar"))

    // pushy
    chinaImplementation(libs.pushy.sdk)

    chinaImplementation(name = "paymentwall-android-sdk", ext = "aar", group = "", version = "")

    // Pangle
    chinaImplementation(libs.ads.sdk.pro)

    // nativex
    chinaImplementation(files("libs/getchannel.jar"))
    chinaImplementation(files("libs/oaid_sdk_1.0.25.aar"))
    // ------------------------------------------------------------------

    // celeb ------------------------------------------------------------
    // ------------------------------------------------------------------

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.debug.db)
    // 필요할 때만 주석해제하여 사용
//    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.0")

    // tapjoy
    implementation(libs.tapjoy.android.sdk)

    // Socket
    // remove and add forked repository
//    implementation("com.koushikdutta.async:androidasync:3.0.9")
    implementation(libs.androidasync)

    // AppLovin
    implementation(libs.applovin.sdk)
    appImplementation(libs.bytedance.adapter)    // Pangleを利用しない場合は削除
    onestoreImplementation(libs.bytedance.adapter)    // Pangleを利用しない場合は削除
    celebImplementation(libs.bytedance.adapter)   // Pangleを利用しない場合は削除
    implementation(libs.google.adapter)       // AdMobを利用しない場合は削除
    implementation(libs.maio.adapter)         // maioを利用しない場合は削除
    implementation(libs.unityads.adapter)     // Unity Adsを利用しない場合は削除
//    implementation("com.applovin.mediation:nend-adapter:6.0.1.1")         // nendを利用しない場合は削除
//    implementation("net.nend.android:nend-sdk:6.0.1" => api level 19가 요구사항이라 일단 막음)
    appImplementation(libs.google.ad.manager.adapter)
    onestoreImplementation(libs.google.ad.manager.adapter)
    celebImplementation(libs.google.ad.manager.adapter)

    implementation(libs.lottie)
    implementation(libs.lottie.compose)

    implementation(libs.play.services.basement)

    implementation(libs.play.services.appset)
    implementation(libs.play.services.ads.identifier)
    implementation(libs.play.services.basement)

    //기획상 facebook,pangle 추가 이지만, ironsource  log 리스너 찍힌  adapter 오류 난 mediation들도 추가해줌.
    implementation(libs.audience.network.sdk)

    //아이언소스 mediation  -> Pangle
    appImplementation(libs.ads.sdk)
    onestoreImplementation(libs.ads.sdk)
    celebImplementation(libs.ads.sdk)

    implementation(libs.play.service.ads)

    implementation(libs.ironsource)
    implementation(libs.applovin)


    //RxKotlin
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxkotlin)
    //안드로이드 앱용 라이프사이클 처리.
    implementation(libs.rxlifecycle)
    implementation(libs.rxbinding)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.segmentedbutton)

    // qrcode : android 24 이하에서는 아래처럼 3.3.0으로 고정해야 함
    implementation(libs.zxing.android.embedded) { isTransitive = false}
    implementation(libs.zxing.core)

    implementation(libs.play.services.oss.licenses)

    implementation(libs.user.messaging.platform)

    onestoreImplementation(files("libs/iap_plugin_v17.01.00_20180206.jar"))
    implementation(libs.transcoder.android)

    implementation(libs.af.android.sdk)

    implementation(libs.androidx.compose.ui)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)

    implementation(libs.landscapist.bom)
    implementation(libs.landscapist.coil)
    implementation(libs.landscapist.placeholder)
    implementation(libs.androidx.runtime.livedata)

    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewModelCompose)

    implementation(libs.androidyoutubeplayer.core)

    // Hilt 의존성 추가
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.kotlinx.serialization.json)

    implementation(project(":bridge"))

    debugImplementation(libs.junit)
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.premiumads.net/artifactory/mobile-ads-sdk/")
    }
}
