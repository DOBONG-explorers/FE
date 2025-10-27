package kr.ac.duksung.dobongzip.ui.common

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.load.model.GlideUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.api.ImageObjectKey
import kr.ac.duksung.dobongzip.data.api.MyPageProfilePatchReq
import kr.ac.duksung.dobongzip.data.api.PasswordChangeReq

data class ProfileState(
    val nickname: String? = null,
    val birthday: String? = null,
    val email: String? = null,
    val imageUrl: String? = null, // 서버 이미지 URL
    val uri: Uri? = null          // 로컬 미리보기 (선택 중일 때)
)

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState

    fun loadProfileAll() {
        viewModelScope.launch {
            // 1) 텍스트 프로필
            runCatching { ApiClient.myPageService.getProfile() }
                .onSuccess { res ->
                    if (res.success) {
                        val d = res.data
                        _profileState.value = _profileState.value.copy(
                            nickname = d?.nickname,
                            birthday = d?.birth,
                            email = d?.email
                        )
                    }
                }
            // 2) 이미지
            runCatching { ApiClient.myPageService.getProfileImage() }
                .onSuccess { res ->
                    if (res.success) {
                        _profileState.value = _profileState.value.copy(
                            imageUrl = res.data?.imageUrl
                        )
                    }
                }
        }
    }

    fun updateNickname(v: String?) { _profileState.value = _profileState.value.copy(nickname = v) }
    fun updateBirthday(v: String?) { _profileState.value = _profileState.value.copy(birthday = v) }
    fun updateEmail(v: String?)    { _profileState.value = _profileState.value.copy(email = v) }
    fun updateProfileUri(uri: Uri?) { _profileState.value = _profileState.value.copy(uri = uri) }

    fun saveProfile(nickname: String?, birth: String?, email: String?, onDone: (Boolean,String?) -> Unit) {
        viewModelScope.launch {
            val res = runCatching { ApiClient.myPageService.patchProfile(MyPageProfilePatchReq(nickname, birth, email)) }.getOrNull()
            onDone(res?.success == true, res?.message)
            if (res?.success == true) loadProfileAll()
        }
    }

    fun finalizeImage(objectKey: String, onDone: (Boolean,String?) -> Unit) {
        viewModelScope.launch {
            val res = runCatching { ApiClient.myPageService.finalizeProfileImage(ImageObjectKey(objectKey)) }.getOrNull()
            if (res?.success == true) {
                _profileState.value = _profileState.value.copy(
                    imageUrl = res.data?.imageUrl,
                    uri = null
                )
                onDone(true, null)
            } else onDone(false, res?.message)
        }
    }

    fun changePassword(current: String, new: String, confirm: String, onDone: (Boolean,String?) -> Unit) {
        viewModelScope.launch {
            val res = runCatching { ApiClient.myPageService.changePassword(PasswordChangeReq(current, new, confirm)) }.getOrNull()
            onDone(res?.success == true, res?.message)
        }
    }

    fun deleteImage(onDone: (Boolean,String?) -> Unit) {
        viewModelScope.launch {
            val res = runCatching { ApiClient.myPageService.deleteProfileImage() }.getOrNull()
            if (res?.success == true) {
                _profileState.value = _profileState.value.copy(imageUrl = null, uri = null)
                onDone(true, null)
            } else onDone(false, res?.message)
        }
    }
}
