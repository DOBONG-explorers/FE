package kr.ac.duksung.dobongzip

import android.app.Application
import android.util.Log
import com.kakao.vectormap.KakaoMapSdk

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // ✅ 임시 우회 + 로그 추가
        val appKey = getString(R.string.kakao_native_app_key).trim()
        KakaoMapSdk.init(this, appKey)

        Log.i("Dobongzip", "✅ KakaoMapSdk.init() 실행됨 - key: $appKey")

        // SDK가 초기화되었는지 바로 테스트 (우회 확인용)
        try {
            KakaoMapSdk.init(applicationContext, appKey)
            Log.i("Dobongzip", "✅ KakaoMapSdk 임시 우회 초기화도 성공")
        } catch (e: Exception) {
            Log.e("Dobongzip", "❌ KakaoMapSdk 임시 초기화 실패", e)
        }
    }
}
