package kr.ac.duksung.dobongzip.data.repository

import android.util.Log
import kr.ac.duksung.dobongzip.data.PlaceDetailDto
import kr.ac.duksung.dobongzip.data.models.ApiResponse
import kr.ac.duksung.dobongzip.data.models.PlaceDto
import kr.ac.duksung.dobongzip.data.models.RandomPlaceDto
import kr.ac.duksung.dobongzip.data.models.TopPlaceDto
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class PlacesRepository {

    private val placesApi = RetrofitProvider.placesApi
    private val likeApi   = RetrofitProvider.placeLikeApi
    
    private var lastErrorMessage: String? = null

    suspend fun fetchPlaces(lat: Double, lng: Double, limit: Int): List<PlaceDto> =
        withContext(Dispatchers.IO) {
            val res: ApiResponse<List<PlaceDto>> = placesApi.getPlaces(lat, lng, limit)
            res.data.orEmpty()
        }

    suspend fun fetchTopPlaces(lat: Double, lng: Double, limit: Int = 3): List<TopPlaceDto> =
        withContext(Dispatchers.IO) {
            val response = placesApi.getTopPlaces(lat, lng, limit)
            if (response.success) {
                response.data.orEmpty()
            } else {
                throw IllegalStateException(response.message ?: "인기 장소를 불러오지 못했습니다.")
            }
        }

    suspend fun fetchPlaceDetail(placeId: String): PlaceDetailDto =
        withContext(Dispatchers.IO) {
            val response = placesApi.getPlaceDetail(placeId)
            val data = response.data
            if (response.success && data != null) {
                data
            } else {
                throw IllegalStateException(
                    response.message ?: "장소 상세 정보를 불러오지 못했습니다."
                )
            }
        }

    suspend fun getRecommendedPlace(lat: Double, lng: Double): PlaceDto? =
        withContext(Dispatchers.IO) {
            try {
                lastErrorMessage = null
                Log.d("PlacesRepository", "Requesting recommended place from random-place API with lat=$lat, lon=$lng")
                
                val response = placesApi.getRandomPlaces(lat = lat, lon = lng)
                
                Log.d("PlacesRepository", "Response received: success=${response.success}, data=${response.data}, message=${response.message}")
                
                if (response.success && response.data != null) {
                    val randomPlace = response.data
                    Log.d("PlacesRepository", "Random place received: ${randomPlace.name}")
                    
                    val imageUrl = randomPlace.imageUrl?.takeIf { it.isNotBlank() && it != "null" }
                    
                    PlaceDto(
                        placeId = randomPlace.placeId,
                        name = randomPlace.name,
                        address = randomPlace.address,
                        latitude = randomPlace.latitude ?: 0.0,
                        longitude = randomPlace.longitude ?: 0.0,
                        distanceMeters = randomPlace.distanceMeters,
                        distanceText = randomPlace.distanceText,
                        imageUrl = imageUrl,
                        description = null,
                        openingHours = null,
                        priceLevel = null,
                        mapsUrl = null,
                        phone = randomPlace.phone,
                        rating = randomPlace.rating,
                        reviewCount = randomPlace.reviewCount
                    )
                } else if (response.success && response.data == null) {
                    lastErrorMessage = response.message ?: "추천할 장소가 없습니다."
                    Log.e("PlacesRepository", "API returned null data: message=$lastErrorMessage")
                    null
                } else {
                    lastErrorMessage = response.message ?: "서버에서 추천 장소를 반환하지 않았습니다."
                    Log.e("PlacesRepository", "API returned failure: success=${response.success}, message=$lastErrorMessage")
                    null
                }
            } catch (e: HttpException) {
                val errorCode = e.code()
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("PlacesRepository", "HTTP Error $errorCode: $errorBody", e)
                
                lastErrorMessage = when (errorCode) {
                    500 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                    404 -> "추천 장소를 찾을 수 없습니다."
                    400 -> "잘못된 요청입니다. 위치 정보를 확인해주세요."
                    401, 403 -> "인증이 필요합니다. 다시 로그인해주세요."
                    else -> "서버 오류 (HTTP $errorCode). 잠시 후 다시 시도해주세요."
                }
                null
            } catch (e: Exception) {
                lastErrorMessage = "네트워크 연결을 확인해주세요: ${e.message}"
                Log.e("PlacesRepository", "Failed to get recommended place: ${e.message}", e)
                e.printStackTrace()
                null
            }
        }
    
    fun getLastErrorMessage(): String? = lastErrorMessage

    suspend fun like(placeId: String): ApiResponse<Any> = withContext(Dispatchers.IO) { 
        likeApi.like(placeId) 
    }
    suspend fun unlike(placeId: String): ApiResponse<Any> = withContext(Dispatchers.IO) { 
        likeApi.unlike(placeId) 
    }

    suspend fun toggleLike(placeId: String, isCurrentlyLiked: Boolean) =
        withContext(Dispatchers.IO) {
            if (isCurrentlyLiked) likeApi.unlike(placeId) else likeApi.like(placeId)
        }

    suspend fun likeFirstPlaceFromServer(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        val res: ApiResponse<List<PlaceDto>> = placesApi.getPlaces(lat = lat, lng = lng, limit = 1)
        val first = res.data?.firstOrNull() ?: return@withContext null

        val placeId = first.placeId

        likeApi.like(placeId)
        return@withContext placeId
    }

}
