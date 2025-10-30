// kr/ac/duksung/dobongzip/SplashActivity.kt
package kr.ac.duksung.dobongzip

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.local.TokenStore
import kr.ac.duksung.dobongzip.data.auth.AuthSession

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenStore = TokenStore(applicationContext)

        lifecycleScope.launch {
            // 1) DataStore → 메모리 캐시 적재
            tokenStore.warmUpCache()

            val token = AuthSession.getToken()
            if (token.isNullOrBlank()) {
                goLogin(); return@launch
            }

            // 2) 토큰 유효성 검증: 가벼운 API 호출
            try {
                val res = ApiClient.myPageService.getProfile() // 200이면 유효
                if (res.success) goMain() else goLogin()
            } catch (e: Exception) {
                if (e is HttpException && e.code() == 401) goLogin()
                else goMain() // 네트워크 불안정 시엔 일단 진입하도록 선택
            }
        }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun goLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
