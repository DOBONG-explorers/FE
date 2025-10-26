pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // ✅ 카카오맵 SDK 저장소
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ✅ 카카오맵 SDK 저장소
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
    }
}
rootProject.name = "dobongzip"
include(":app")
