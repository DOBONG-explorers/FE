// kr/ac/duksung/dobongzip/data/local/TokenStore.kt
package kr.ac.duksung.dobongzip.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenStore(private val context: Context) {
    companion object {
        private val KEY_ACCESS = stringPreferencesKey("access_token")
        private val KEY_EMAIL = stringPreferencesKey("signup_email") // step2용
    }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { it[KEY_ACCESS] = token }
    }
    suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { it[KEY_ACCESS] }.first()
    }
    suspend fun saveSignupEmail(email: String) {
        context.dataStore.edit { it[KEY_EMAIL] = email }
    }
    suspend fun getSignupEmail(): String? {
        return context.dataStore.data.map { it[KEY_EMAIL] }.first()
    }
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
