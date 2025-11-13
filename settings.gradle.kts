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
        // ✅ Kakao 공용 저장소 (로그인 SDK 등)
        maven(url = "https://devrepo.kakao.com/nexus/content/groups/public/")

        // (선택) 지도 전용 저장소도 계속 쓰고 싶으면 유지
        maven(url = "https://devrepo.kakao.com/nexus/repository/kakaomap-releases/")
    }
}
rootProject.name = "dobongzip"
include(":app")
