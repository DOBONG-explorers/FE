package kr.ac.duksung.dobongzip.ui.top

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.data.local.SavedLocationStore
import kr.ac.duksung.dobongzip.data.models.TopPlaceDto
import kr.ac.duksung.dobongzip.data.repository.PlacesRepository

data class TopPlacesUiState(
    val isLoading: Boolean = false,
    val places: List<TopPlaceDto> = emptyList(),
    val sortedPlaces: List<TopPlaceDto> = emptyList(),
    val isDescending: Boolean = true,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean = sortedPlaces.isEmpty()
}

class TopPlacesViewModel(
    private val repository: PlacesRepository,
    private val locationStore: SavedLocationStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopPlacesUiState())
    val uiState: StateFlow<TopPlacesUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (lat, lng) = resolveCoordinates()
                val places = repository.fetchTopPlaces(lat, lng)
                val isDescending = _uiState.value.isDescending
                val sorted = sortPlaces(places, isDescending)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        places = places,
                        sortedPlaces = sorted,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        places = emptyList(),
                        sortedPlaces = emptyList(),
                        errorMessage = e.message ?: DEFAULT_ERROR_MESSAGE
                    )
                }
            }
        }
    }

    fun toggleSortOrder() {
        _uiState.update { current ->
            val newDescending = !current.isDescending
            val sorted = sortPlaces(current.places, newDescending)
            current.copy(
                isDescending = newDescending,
                sortedPlaces = sorted
            )
        }
    }

    private suspend fun resolveCoordinates(): Pair<Double, Double> {
        val lat = locationStore.lastLatitude.firstOrNull()
        val lng = locationStore.lastLongitude.firstOrNull()
        return if (isInDobong(lat, lng)) {
            lat!! to lng!!
        } else {
            DOBONG_CENTER_LAT to DOBONG_CENTER_LNG
        }
    }

    private fun isInDobong(lat: Double?, lng: Double?): Boolean {
        if (lat == null || lng == null) return false
        return lat in DOBONG_LAT_RANGE && lng in DOBONG_LNG_RANGE
    }

    private fun sortPlaces(
        places: List<TopPlaceDto>,
        isDescending: Boolean
    ): List<TopPlaceDto> {
        return if (isDescending) {
            places.sortedByDescending { it.reviewCount ?: 0 }
        } else {
            places.sortedBy { it.reviewCount ?: 0 }
        }
    }

    companion object {
        private const val DOBONG_CENTER_LAT = 37.695
        private const val DOBONG_CENTER_LNG = 127.046944
        private const val DEFAULT_ERROR_MESSAGE = "인기 장소를 불러오지 못했습니다."
        private val DOBONG_LAT_RANGE = 37.63..37.75
        private val DOBONG_LNG_RANGE = 126.99..127.09
    }
}

class TopPlacesViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TopPlacesViewModel::class.java)) {
            val repository = PlacesRepository()
            val locationStore = SavedLocationStore(application)
            @Suppress("UNCHECKED_CAST")
            return TopPlacesViewModel(repository, locationStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

