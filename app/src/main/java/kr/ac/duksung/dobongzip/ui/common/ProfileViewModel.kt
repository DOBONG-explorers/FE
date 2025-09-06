package kr.ac.duksung.dobongzip.ui.common

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.data.ProfileRepository

data class ProfileState(
    val uri: Uri? = null,
    val nickname: String? = null,
    val birthday: String? = null,
    val email: String? = null
)

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProfileRepository(app)

    /** 전역 상태 합치기 */
    val profileState: StateFlow<ProfileState> =
        combine(
            repo.profileUriFlow,
            repo.nicknameFlow,
            repo.birthdayFlow,
            repo.emailFlow
        ) { uri, name, birth, mail ->
            ProfileState(uri = uri, nickname = name, birthday = birth, email = mail)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileState()
        )

    /** (이미지 전용 Flow가 따로 필요하면 사용 가능) */
    val profileUri: StateFlow<Uri?> =
        profileState.map { it.uri }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 업데이트 메서드들 — Fragment에서 호출 */
    fun updateProfileUri(uri: Uri?) = viewModelScope.launch {
        repo.setProfileUri(uri)
    }

    fun updateNickname(value: String?) = viewModelScope.launch {
        repo.setNickname(value?.trim())
    }

    fun updateBirthday(value: String?) = viewModelScope.launch {
        repo.setBirthday(value?.trim())
    }

    fun updateEmail(value: String?) = viewModelScope.launch {
        repo.setEmail(value?.trim())
    }
}
