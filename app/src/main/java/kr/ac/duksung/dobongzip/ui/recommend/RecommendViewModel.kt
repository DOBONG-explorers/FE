package kr.ac.duksung.dobongzip.ui.recommend

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.data.local.SavedLocationStore
import kr.ac.duksung.dobongzip.data.models.PlaceDto
import kr.ac.duksung.dobongzip.data.repository.PlacesRepository

class RecommendViewModel(application: Application) : AndroidViewModel(application) {

    private val _recommendedPlace = MutableLiveData<PlaceDto?>()
    val recommendedPlace: LiveData<PlaceDto?> = _recommendedPlace

    private val _uiState = MutableLiveData<UiState>(UiState.Initial)
    val uiState: LiveData<UiState> = _uiState

    private val placesRepository = PlacesRepository()
    private val locationStore = SavedLocationStore(application)

    fun onRecommendButtonClicked() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val lat = locationStore.lastLatitude.firstOrNull()
            val lng = locationStore.lastLongitude.firstOrNull()

            if (lat == null || lng == null) {
                Log.e("RecommendViewModel", "Location not found in DataStore")
                _uiState.value = UiState.Error("위치 정보를 찾을 수 없습니다.")
                return@launch
            }

            Log.d("RecommendViewModel", "Fetching recommended place for lat=$lat, lng=$lng")
            val result = placesRepository.getRecommendedPlace(lat, lng)

            if (result != null) {
                Log.d("RecommendViewModel", "Recommended place received: ${result.name}")
                _recommendedPlace.value = result
                _uiState.value = UiState.Success
            } else {
                Log.e("RecommendViewModel", "Failed to get recommended place - result is null")

                val errorMessage = placesRepository.getLastErrorMessage() 
                    ?: "추천 장소를 불러오지 못했습니다."
                _uiState.value = UiState.Error(errorMessage)
            }
        }
    }
    
    suspend fun saveLocation(latitude: Double, longitude: Double) {
        locationStore.saveLatitude(latitude)
        locationStore.saveLongitude(longitude)
        Log.d("RecommendViewModel", "Location saved: lat=$latitude, lng=$longitude")
    }
    
    suspend fun getLastLatitude(): Double? {
        return locationStore.lastLatitude.firstOrNull()
    }
    
    suspend fun getLastLongitude(): Double? {
        return locationStore.lastLongitude.firstOrNull()
    }
    
    fun setUiStateLoading() {
        _uiState.value = UiState.Loading
    }
    
    fun handleLocationError(message: String) {
        _uiState.value = UiState.Error(message)
    }
    
    fun resetToInitial() {
        _uiState.value = UiState.Initial
    }
}

sealed class UiState {
    object Initial : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}