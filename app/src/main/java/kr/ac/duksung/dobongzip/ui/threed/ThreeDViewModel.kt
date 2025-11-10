package kr.ac.duksung.dobongzip.ui.threed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.data.PlaceDetailDto
import kr.ac.duksung.dobongzip.data.models.ApiResponse
import kr.ac.duksung.dobongzip.data.network.LikeResponse
import kr.ac.duksung.dobongzip.data.repository.PlacesRepository


class ThreeDViewModel : ViewModel() {

    private val repository = PlacesRepository()

    private val _detail = MutableLiveData<PlaceDetailDto?>()
    val detail: LiveData<PlaceDetailDto?> = _detail

    private val _liked = MutableLiveData<Boolean>(false)
    val liked: LiveData<Boolean> = _liked

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadPlaceDetail(placeId: String) {
        viewModelScope.launch {
            try {
                val detail = repository.fetchPlaceDetail(placeId)
                _detail.value = detail
                _liked.value = detail.liked == true
            } catch (e: Exception) {
                _error.value = e.message ?: "장소 정보를 불러올 수 없습니다."
            }
        }
    }

    fun toggleLike(placeId: String) {
        val isCurrentlyLiked = _liked.value == true
        viewModelScope.launch {
            try {
                // ApiResponse<LikeResponse> 반환
                val response: ApiResponse<LikeResponse> = if (isCurrentlyLiked) {
                    repository.unlike(placeId)
                } else {
                    repository.like(placeId)
                }

                if (response.success) {
                    _liked.value = !isCurrentlyLiked
                } else {
                    _error.value = response.message ?: "좋아요 처리에 실패했습니다."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "좋아요 처리 중 오류가 발생했습니다."
            }
        }
    }

    fun consumeError() {
        _error.value = null
    }
}


