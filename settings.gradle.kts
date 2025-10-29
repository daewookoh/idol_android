pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Kakao SDK Maven Repository (old 프로젝트와 동일)
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }

        // JitPack (추가 라이브러리용)
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "idol_android"
include(":app")
