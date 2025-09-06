package kr.ac.duksung.dobongzip.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore by preferencesDataStore(name = "profile_prefs")

class ProfileRepository(private val context: Context) {

    companion object {
        private val KEY_PROFILE_URI = stringPreferencesKey("profile_image_uri")
        private val KEY_NICKNAME    = stringPreferencesKey("profile_nickname")
        private val KEY_BIRTHDAY    = stringPreferencesKey("profile_birthday")
        private val KEY_EMAIL       = stringPreferencesKey("profile_email")

        // 빈 문자열을 “없음”으로 간주하기 위한 헬퍼
        private fun String?.orNullIfBlank(): String? =
            if (this.isNullOrBlank()) null else this
    }

    /** 읽기: 빈 문자열이면 null 처리 */
    val profileUriFlow: Flow<Uri?> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_PROFILE_URI].orNullIfBlank()?.let { Uri.parse(it) }
        }

    val nicknameFlow: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[KEY_NICKNAME].orNullIfBlank() }

    val birthdayFlow: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[KEY_BIRTHDAY].orNullIfBlank() }

    val emailFlow: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[KEY_EMAIL].orNullIfBlank() }

    /** 쓰기: null/blank → 빈 문자열("") 저장 (삭제 대체) */
    suspend fun setProfileUri(uri: Uri?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROFILE_URI] = uri?.toString().orEmpty()
        }
    }

    suspend fun setNickname(value: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NICKNAME] = value?.trim().orEmpty()
        }
    }

    suspend fun setBirthday(value: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIRTHDAY] = value?.trim().orEmpty()
        }
    }

    suspend fun setEmail(value: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EMAIL] = value?.trim().orEmpty()
        }
    }
}
