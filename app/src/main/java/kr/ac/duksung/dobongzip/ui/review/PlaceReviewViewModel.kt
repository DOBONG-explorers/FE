package kr.ac.duksung.dobongzip.ui.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.data.models.PlaceReviewDto
import kr.ac.duksung.dobongzip.data.models.PlaceReviewSummaryDto
import kr.ac.duksung.dobongzip.data.repository.ReviewRepository

class PlaceReviewViewModel(
    private val repository: ReviewRepository = ReviewRepository()
) : ViewModel() {

    private val _summary = MutableLiveData<PlaceReviewSummaryDto>()
    val summary: LiveData<PlaceReviewSummaryDto> = _summary

    private val _reviews = MutableLiveData<List<PlaceReviewDto>>(emptyList())
    val reviews: LiveData<List<PlaceReviewDto>> = _reviews

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private var currentPlaceId: String? = null
    private var limit: Int = 20

    fun initialize(placeId: String, limit: Int = 20) {
        this.limit = limit
        if (currentPlaceId == placeId && _summary.value != null) return
        currentPlaceId = placeId
        loadReviews()
    }

    fun refresh() {
        loadReviews(force = true)
    }

    private fun loadReviews(force: Boolean = false) {
        val placeId = currentPlaceId ?: return
        if (_loading.value == true && !force) return
        viewModelScope.launch {
            try {
                _loading.value = true
                val result = repository.getReviews(placeId, limit)
                _summary.value = result
                _reviews.value = result.reviews
            } catch (e: Exception) {
                _error.value = e.message ?: "리뷰를 불러오지 못했습니다."
            } finally {
                _loading.value = false
            }
        }
    }

    fun createReview(rating: Double, text: String) {
        val placeId = currentPlaceId ?: return
        viewModelScope.launch {
            try {
                repository.createReview(placeId, rating, text)
                _message.value = "리뷰가 등록되었습니다."
                loadReviews(force = true)
            } catch (e: Exception) {
                _error.value = e.message ?: "리뷰 등록에 실패했습니다."
            }
        }
    }

    fun updateReview(reviewId: Long, rating: Double, text: String) {
        val placeId = currentPlaceId ?: return
        viewModelScope.launch {
            try {
                repository.updateReview(placeId, reviewId, rating, text)
                _message.value = "리뷰가 수정되었습니다."
                loadReviews(force = true)
            } catch (e: Exception) {
                _error.value = e.message ?: "리뷰 수정에 실패했습니다."
            }
        }
    }

    fun deleteReview(reviewId: Long) {
        if (reviewId <= 0) {
            _error.value = "리뷰 ID가 유효하지 않습니다."
            return
        }
        val placeId = currentPlaceId ?: return
        viewModelScope.launch {
            try {
                repository.deleteReview(placeId, reviewId)
                _message.value = "리뷰가 삭제되었습니다."
                loadReviews(force = true)
            } catch (e: Exception) {
                _error.value = e.message ?: "리뷰 삭제에 실패했습니다."
            }
        }
    }

    fun consumeError() {
        _error.value = null
    }

    fun consumeMessage() {
        _message.value = null
    }
}

