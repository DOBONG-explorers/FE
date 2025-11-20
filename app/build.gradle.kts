plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}


android {
    namespace = "kr.ac.duksung.dobongzip"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.ac.duksung.dobongzip"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // 🔐 1) 릴리즈 keystore 설정
    signingConfigs {
        create("release") {
            storeFile = file("/Users/jiyeong/Downloads/my_new_release_key.jks")     // 예: "C:/Users/너/keystore/my-release-key.jks"
            storePassword = "123456"
            keyAlias = "my_new_release_key"
            keyPassword = "123456"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // ✅ JDK 17 (AGP 8.x 호환)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ✅ Android 기본
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ✅ Lifecycle & Navigation
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.3")

    // ✅ Activity / Fragment
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.4")

    // ✅ DataStore (토큰 저장)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ✅ 네트워킹 (Retrofit + OkHttp + Gson)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")  // Retrofit 버전
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")  // Gson converter 버전
    implementation("com.squareup.okhttp3:okhttp:4.12.0")  // OkHttp 버전
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")  // OkHttp 로그 인터셉터

    // ✅ 코루틴 (백그라운드 API 호출)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ✅ 이미지 로딩 (Glide / Coil)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation("io.coil-kt:coil:2.6.0")

    // 이미지 처리
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // ✅ 카카오맵 (벡터맵)
    implementation("com.kakao.maps.open:android:2.12.18")
    //implementation("com.kakao.sdk:v2-map:2.11.0")
    // ✅ 위치
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ✅ 테스트

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    //카카오 로그인
    implementation("com.kakao.sdk:v2-user:2.20.6")
    //구글 로그인
    implementation("com.google.android.gms:play-services-auth:21.2.0")
}
