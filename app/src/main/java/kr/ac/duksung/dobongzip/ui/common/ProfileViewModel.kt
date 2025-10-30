// kr/ac/duksung/dobongzip/ui/common/ProfileViewModel.kt
package kr.ac.duksung.dobongzip.ui.common

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    val uri: Uri? = null,         // 로컬 미리보기 (선택 중일 때)
    val loading: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState

    fun loadProfileAll() {
        viewModelScope.launch {
            _profileState.update { it.copy(loading = true, error = null) }
            try {
                // 1) 텍스트 프로필
                val profileRes = ApiClient.myPageService.getProfile()
                if (profileRes.success) {
                    val d = profileRes.data
                    _profileState.update {
                        it.copy(nickname = d?.nickname, birthday = d?.birth, email = d?.email)
                    }
                } else {
                    _profileState.update { it.copy(error = profileRes.message.ifBlank { "프로필 조회 실패" }) }
                }

                // 2) 이미지
                val imgRes = ApiClient.myPageService.getProfileImage()
                if (imgRes.success) {
                    _profileState.update { it.copy(imageUrl = imgRes.data?.imageUrl) }
                }
            } catch (e: Exception) {
                _profileState.update { it.copy(error = e.message) }
            } finally {
                _profileState.update { it.copy(loading = false) }
            }
        }
    }

    fun updateNickname(v: String?) { _profileState.update { it.copy(nickname = v) } }
    fun updateBirthday(v: String?) { _profileState.update { it.copy(birthday = v) } }
    fun updateEmail(v: String?)    { _profileState.update { it.copy(email = v) } }
    fun updateProfileUri(uri: Uri?) { _profileState.update { it.copy(uri = uri) } }

    fun saveProfile(
        nickname: String?, birth: String?, email: String?,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _profileState.update { it.copy(loading = true, error = null) }
            try {
                val res = ApiClient.myPageService.patchProfile(
                    MyPageProfilePatchReq(nickname, birth, email)
                )
                if (res.success) {
                    _profileState.update {
                        it.copy(nickname = nickname, birthday = birth, email = email)
                    }
                    onDone(true, null)
                    // 변경 후 최신값 재로드(서버 진실원본 기준)
                    loadProfileAll()
                } else {
                    _profileState.update { it.copy(error = res.message.ifBlank { "프로필 저장 실패" }) }
                    onDone(false, res.message)
                }
            } catch (e: Exception) {
                _profileState.update { it.copy(error = e.message) }
                onDone(false, e.message)
            } finally {
                _profileState.update { it.copy(loading = false) }
            }
        }
    }

    fun finalizeImage(objectKey: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _profileState.update { it.copy(loading = true, error = null) }
            try {
                val res = ApiClient.myPageService.finalizeProfileImage(ImageObjectKey(objectKey))
                if (res.success) {
                    _profileState.update {
                        it.copy(imageUrl = res.data?.imageUrl, uri = null)
                    }
                    onDone(true, null)
                } else {
                    _profileState.update { it.copy(error = res.message.ifBlank { "이미지 반영 실패" }) }
                    onDone(false, res.message)
                }
            } catch (e: Exception) {
                _profileState.update { it.copy(error = e.message) }
                onDone(false, e.message)
            } finally {
                _profileState.update { it.copy(loading = false) }
            }
        }
    }

    fun changePassword(current: String, newPw: String, confirm: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _profileState.update { it.copy(loading = true, error = null) }
            try {
                val res = ApiClient.myPageService.changePassword(
                    PasswordChangeReq(current, newPw, confirm)
                )
                if (res.success) {
                    onDone(true, null)
                } else {
                    _profileState.update { it.copy(error = res.message.ifBlank { "비밀번호 변경 실패" }) }
                    onDone(false, res.message)
                }
            } catch (e: Exception) {
                _profileState.update { it.copy(error = e.message) }
                onDone(false, e.message)
            } finally {
                _profileState.update { it.copy(loading = false) }
            }
        }
    }

    fun deleteImage(onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _profileState.update { it.copy(loading = true, error = null) }
            try {
                val res = ApiClient.myPageService.deleteProfileImage()
                if (res.success) {
                    _profileState.update { it.copy(imageUrl = null, uri = null) }
                    onDone(true, null)
                } else {
                    _profileState.update { it.copy(error = res.message.ifBlank { "이미지 삭제 실패" }) }
                    onDone(false, res.message)
                }
            } catch (e: Exception) {
                _profileState.update { it.copy(error = e.message) }
                onDone(false, e.message)
            } finally {
                _profileState.update { it.copy(loading = false) }
            }
        }
    }
}
