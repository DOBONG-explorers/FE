package kr.ac.duksung.dobongzip.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kr.ac.duksung.dobongzip.data.auth.AuthSession

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenStore(private val context: Context) {

    companion object {
        private val KEY_ACCESS = stringPreferencesKey("access_token")
        private val KEY_EMAIL  = stringPreferencesKey("signup_email")
    }

    /** 앱 시작 시 DataStore → 메모리 캐시에 적재할 때 호출 */
    suspend fun warmUpCache() {
        // DataStore에서 accessToken을 가져와서 AuthSession에 설정
        val token = getAccessToken()
        token?.let {
            AuthSession.setToken(it)
        }
    }

    // ---------------- Access Token ----------------

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { it[KEY_ACCESS] = token }
        AuthSession.setToken(token) // ← 메모리 캐시 동기화
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { it[KEY_ACCESS] }.first()
    }

    suspend fun clearAccessToken() {
        context.dataStore.edit { it.remove(KEY_ACCESS) }
        AuthSession.clear() // ← 메모리 캐시 동기화
    }

    // ---------------- Signup Email ----------------

    suspend fun saveSignupEmail(email: String) {
        context.dataStore.edit { it[KEY_EMAIL] = email }
    }

    suspend fun getSignupEmail(): String? {
        return context.dataStore.data.map { it[KEY_EMAIL] }.first()
    }

    // ---------------- All Clear ----------------

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
        AuthSession.clear()
    }
}
