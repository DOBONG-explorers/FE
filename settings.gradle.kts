pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // ✅ 카카오맵 SDK 저장소
        maven(url = "https://devrepo.kakao.com/nexus/repository/kakaomap-releases/")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://devrepo.kakao.com/nexus/repository/kakaomap-releases/")
    }
}
rootProject.name = "dobongzip"
include(":app")
