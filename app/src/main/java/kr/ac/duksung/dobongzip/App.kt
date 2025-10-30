package kr.ac.duksung.dobongzip

import android.app.Application
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.kakao.vectormap.KakaoMapSdk
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.local.TokenStore

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ Kakao 지도 SDK 초기화
        val appKey = getString(R.string.kakao_native_app_key).trim()
        KakaoMapSdk.init(this, appKey)
        Log.i("Dobongzip", "✅ KakaoMapSdk.init() 실행됨 - key: $appKey")

        try {
            KakaoMapSdk.init(applicationContext, appKey)
            Log.i("Dobongzip", "✅ KakaoMapSdk 임시 우회 초기화도 성공")
        } catch (e: Exception) {
            Log.e("Dobongzip", "❌ KakaoMapSdk 임시 초기화 실패", e)
        }

        // ✅ TokenStore + Retrofit 초기화
        val tokenStore = TokenStore(applicationContext)

        // lifecycleScope 대신 전역 CoroutineScope 사용 (Application은 lifecycleOwner 아님)
        CoroutineScope(Dispatchers.IO).launch {
            tokenStore.warmUpCache() // DataStore → AuthSession으로 캐시 로드
        }

        // OkHttp / Retrofit 초기화
        ApiClient.rebuild()
    }
}
